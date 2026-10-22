package com.example.data.lyrics

import android.content.Context
import android.util.LruCache
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull

object LyricsRepository {
    private const val IDENTIFY_TIMEOUT_MS = 2_500L

    private val identifyCache = LruCache<String, BiniLyrics.Track>(128)
    private val resultsMemoryCache = LruCache<String, CachedLyricsResult>(96)

    data class CachedLyricsResult(
        val lines: List<LyricLine>,
        val winningSource: LyricsSource?,
        val perSourceResults: Map<LyricsSource, List<LyricLine>>,
        val providerStates: Map<LyricsSource, LyricsProviderState>,
    )

    suspend fun getLyrics(
        context: Context? = null,
        videoId: String,
        title: String,
        artist: String,
        album: String? = null,
        durationMs: Long = 0L,
        localFilePathOrUri: String? = null,
        enabledSources: Set<LyricsSource> = LyricsSource.defaultEnabled,
        sourceOrder: List<LyricsSource> = LyricsSource.offered,
        prioritizeSyllableSync: Boolean = false,
        paxSenixApiKey: String = "",
        forceRefresh: Boolean = false,
        onSourceStarted: (LyricsSource) -> Unit = {},
        onSourceResult: (LyricsSource, List<LyricLine>?) -> Unit = { _, _ -> },
        onSourceCancelled: (LyricsSource) -> Unit = {},
    ): Pair<LyricsSource?, List<LyricLine>> = coroutineScope {
        // 1. Check embedded/sidecar local lyrics first (instant offline word-by-word support)
        if (context != null && (!localFilePathOrUri.isNullOrBlank() || videoId.isNotBlank())) {
            val embedded = EmbeddedLyrics.read(
                context = context,
                localFilePathOrUri = localFilePathOrUri,
                trackIdKey = videoId,
            )
            if (!embedded.isNullOrEmpty()) {
                return@coroutineScope Pair(null, embedded)
            }
        }

        val orderedActive = sourceOrder.filter { it in enabledSources && !it.hidden }
        if (orderedActive.isEmpty()) {
            return@coroutineScope Pair(null, emptyList())
        }

        val cacheKey = buildString {
            append(videoId.trim()).append('|')
            append(title.forLyricsSearch().lowercase()).append('|')
            append(artist.artistForLyricsSearch().lowercase()).append('|')
            append(orderedActive.joinToString(",") { it.name }).append('|')
            append(prioritizeSyllableSync).append('|')
            append(paxSenixApiKey.hashCode())
        }

        if (!forceRefresh) {
            val cached = resultsMemoryCache.get(cacheKey)
            if (cached != null && cached.lines.isNotEmpty()) {
                cached.providerStates.forEach { (src, st) ->
                    when (st) {
                        LyricsProviderState.FOUND -> onSourceResult(src, cached.perSourceResults[src])
                        LyricsProviderState.NOT_FOUND -> onSourceResult(src, null)
                        LyricsProviderState.NOT_FETCHED -> onSourceCancelled(src)
                        LyricsProviderState.FETCHING -> {}
                    }
                }
                return@coroutineScope Pair(cached.winningSource, cached.lines)
            }
        }

        // 2. Shared BiniLyrics.identify deferred so LRC_RED, BINI_LYRICS, PAXSENIX, LYRICS_PLUS, UNISON, MUSIXMATCH share ISRC
        val identifyKey = "${title.forLyricsSearch().lowercase()}|${artist.artistForLyricsSearch().lowercase()}|$durationMs"
        val identifyDeferred: Deferred<BiniLyrics.Track?> = async(Dispatchers.IO) {
            identifyCache.get(identifyKey)?.let { return@async it }
            val identified = withTimeoutOrNull(IDENTIFY_TIMEOUT_MS) {
                BiniLyrics.identify(
                    title = title,
                    artist = artist,
                    album = album,
                    durationMs = durationMs,
                )
            }
            if (identified != null) {
                identifyCache.put(identifyKey, identified)
            }
            identified
        }

        val sharedDocs = SharedDocuments(
            scope = this,
            title = title,
            artist = artist,
            album = album,
            durationMs = durationMs,
            paxSenixApiKey = paxSenixApiKey,
            identifyDeferred = identifyDeferred,
        )

        val primarySources = orderedActive.filter { it != LyricsSource.GENIUS }
        val includeGeniusFallback = LyricsSource.GENIUS in orderedActive

        val jobs = LinkedHashMap<LyricsSource, Deferred<Pair<LyricsSource, List<LyricLine>?>>>()
        for (source in primarySources) {
            onSourceStarted(source)
            jobs[source] = async(Dispatchers.IO) {
                val res = try {
                    fetchFromSource(
                        source = source,
                        videoId = videoId,
                        title = title,
                        artist = artist,
                        album = album,
                        durationMs = durationMs,
                        paxSenixApiKey = paxSenixApiKey,
                        sharedDocs = sharedDocs,
                    )
                } catch (ce: CancellationException) {
                    throw ce
                } catch (_: Exception) {
                    null
                }
                source to res?.takeIf { it.isNotEmpty() }
            }
        }

        val results = LinkedHashMap<LyricsSource, List<LyricLine>?>()
        val states = LinkedHashMap<LyricsSource, LyricsProviderState>()
        orderedActive.forEach { states[it] = LyricsProviderState.FETCHING }

        val pending = jobs.values.toMutableList()
        var winnerSource: LyricsSource? = null
        var winnerLines: List<LyricLine>? = null

        fun checkEarlyWinner(): Boolean {
            // Walk priority order from highest to lowest
            for (src in primarySources) {
                if (!results.containsKey(src)) {
                    // Highest-priority unresolved source is still in flight; wait for it unless prioritizeSyllableSync
                    // already has a higher-priority syllable-synced result (impossible since we walk from top)
                    return false
                }
                val lines = results[src]
                if (!lines.isNullOrEmpty()) {
                    if (!prioritizeSyllableSync || lines.any { it.isWordSynced }) {
                        winnerSource = src
                        winnerLines = lines
                        return true
                    }
                }
            }
            return false
        }

        while (pending.isNotEmpty()) {
            val completed = select<Pair<Deferred<Pair<LyricsSource, List<LyricLine>?>>, Pair<LyricsSource, List<LyricLine>?>>> {
                for (job in pending) {
                    job.onAwait { out -> job to out }
                }
            }
            pending.remove(completed.first)
            val (src, lines) = completed.second
            results[src] = lines
            states[src] = if (!lines.isNullOrEmpty()) LyricsProviderState.FOUND else LyricsProviderState.NOT_FOUND
            onSourceResult(src, lines)

            if (checkEarlyWinner()) {
                break
            }
        }

        // Cancel remaining slower jobs if we already found our highest-priority winner
        if (winnerLines != null) {
            for ((src, job) in jobs) {
                if (!results.containsKey(src)) {
                    job.cancel()
                    states[src] = LyricsProviderState.NOT_FETCHED
                    onSourceCancelled(src)
                }
            }
            if (includeGeniusFallback) {
                states[LyricsSource.GENIUS] = LyricsProviderState.NOT_FETCHED
                onSourceCancelled(LyricsSource.GENIUS)
            }
        } else {
            // Pick best among completed primary sources (prefer word-synced if prioritizeSyllableSync is enabled, then synced, then plain)
            val bestFromPrimary = primarySources
                .mapNotNull { src -> results[src]?.takeIf { it.isNotEmpty() }?.let { src to it } }
                .sortedWith(
                    compareByDescending<Pair<LyricsSource, List<LyricLine>>> { (_, lines) ->
                        when {
                            prioritizeSyllableSync && lines.any { it.isWordSynced } -> 3
                            lines.any { it.timeMs > 0L } -> 2
                            else -> 1
                        }
                    }
                )
                .firstOrNull()

            if (bestFromPrimary != null) {
                winnerSource = bestFromPrimary.first
                winnerLines = bestFromPrimary.second
                if (includeGeniusFallback) {
                    states[LyricsSource.GENIUS] = LyricsProviderState.NOT_FETCHED
                    onSourceCancelled(LyricsSource.GENIUS)
                }
            } else if (includeGeniusFallback) {
                // Lazy start Genius only when all synced providers failed
                onSourceStarted(LyricsSource.GENIUS)
                val geniusLines = try {
                    Genius.lyrics(title = title, artist = artist)?.takeIf { it.isNotEmpty() }
                } catch (_: Exception) {
                    null
                }
                results[LyricsSource.GENIUS] = geniusLines
                states[LyricsSource.GENIUS] = if (geniusLines != null) LyricsProviderState.FOUND else LyricsProviderState.NOT_FOUND
                onSourceResult(LyricsSource.GENIUS, geniusLines)
                if (geniusLines != null) {
                    winnerSource = LyricsSource.GENIUS
                    winnerLines = geniusLines
                }
            }
        }

        val finalLines = winnerLines ?: emptyList()
        if (finalLines.isNotEmpty()) {
            resultsMemoryCache.put(
                cacheKey,
                CachedLyricsResult(
                    lines = finalLines,
                    winningSource = winnerSource,
                    perSourceResults = results.filterValues { !it.isNullOrEmpty() }.mapValues { it.value!! },
                    providerStates = states,
                )
            )
        }

        Pair(winnerSource, finalLines)
    }

    suspend fun fetchSingleSource(
        source: LyricsSource,
        videoId: String,
        title: String,
        artist: String,
        album: String? = null,
        durationMs: Long = 0L,
        paxSenixApiKey: String = "",
    ): List<LyricLine>? = coroutineScope {
        val identifyDeferred = async(Dispatchers.IO) {
            withTimeoutOrNull(IDENTIFY_TIMEOUT_MS) {
                BiniLyrics.identify(title, artist, album, durationMs)
            }
        }
        val sharedDocs = SharedDocuments(
            scope = this,
            title = title,
            artist = artist,
            album = album,
            durationMs = durationMs,
            paxSenixApiKey = paxSenixApiKey,
            identifyDeferred = identifyDeferred,
        )
        fetchFromSource(
            source = source,
            videoId = videoId,
            title = title,
            artist = artist,
            album = album,
            durationMs = durationMs,
            paxSenixApiKey = paxSenixApiKey,
            sharedDocs = sharedDocs,
        )
    }

    private suspend fun fetchFromSource(
        source: LyricsSource,
        videoId: String,
        title: String,
        artist: String,
        album: String?,
        durationMs: Long,
        paxSenixApiKey: String,
        sharedDocs: SharedDocuments,
    ): List<LyricLine>? {
        val isrc = sharedDocs.awaitIsrc()
        return when (source) {
            LyricsSource.LRC_RED -> LrcRed.lyrics(title, artist, album, isrc)
            LyricsSource.BINI_LYRICS -> sharedDocs.awaitBiniLyrics()
            LyricsSource.BETTER_LYRICS -> BetterLyrics.lyrics(title, artist, album, durationMs)
            LyricsSource.BETTER_LYRICS_PORTATO -> BetterLyrics.portato(title, artist, album, durationMs)
            LyricsSource.PAXSENIX -> sharedDocs.awaitPaxSenixApple()
            LyricsSource.PAXSENIX_SPOTIFY -> PaxSenix.spotifyLyrics(title, artist, album, durationMs, isrc, paxSenixApiKey)
            LyricsSource.PAXSENIX_MUSIXMATCH -> PaxSenix.musixmatchLyrics(title, artist, album, durationMs, isrc, paxSenixApiKey)
            LyricsSource.LYRICS_PLUS -> LyricsPlus.lyrics(title, artist, album, durationMs, isrc)
            LyricsSource.SIMP_MUSIC -> SimpMusicLyrics.lyrics(videoId)
            LyricsSource.UNISON -> Unison.lyrics(title, artist, album, durationMs, isrc)
            LyricsSource.YOUTUBE_TRANSCRIPT -> YouTubeTranscriptLyrics.lyrics(videoId)
            LyricsSource.YOUTUBE_MUSIC -> YouTubeMusicLyrics.lyrics(videoId, title, artist)
            LyricsSource.MEGALOBIZ -> Megalobiz.lyrics(title, artist)
            LyricsSource.KUGOU -> KuGou.lyrics(title, artist, durationMs)
            LyricsSource.LRCLIB -> LrcLib.lyrics(title, artist, album, durationMs)
            LyricsSource.MUSIXMATCH -> Musixmatch.lyrics(title, artist, durationMs, isrc)
            LyricsSource.GENIUS -> Genius.lyrics(title, artist)
        }
    }

    private class SharedDocuments(
        private val scope: CoroutineScope,
        private val title: String,
        private val artist: String,
        private val album: String?,
        private val durationMs: Long,
        private val paxSenixApiKey: String,
        private val identifyDeferred: Deferred<BiniLyrics.Track?>,
    ) {
        private val mutex = Mutex()
        private var biniDef: Deferred<List<LyricLine>?>? = null
        private var paxAppleDef: Deferred<List<LyricLine>?>? = null

        suspend fun awaitIsrc(): String? = runCatching { identifyDeferred.await()?.isrc }.getOrNull()

        suspend fun awaitBiniLyrics(): List<LyricLine>? {
            val def = mutex.withLock {
                biniDef ?: scope.async(Dispatchers.IO) {
                    val identified = runCatching { identifyDeferred.await() }.getOrNull()
                    if (identified != null) {
                        BiniLyrics.lyricsFor(identified)?.takeIf { it.isNotEmpty() }?.let { return@async it }
                    }
                    BiniLyrics.lyrics(title, artist, album, durationMs, identified?.isrc)
                }.also { biniDef = it }
            }
            return def.await()
        }

        suspend fun awaitPaxSenixApple(): List<LyricLine>? {
            val def = mutex.withLock {
                paxAppleDef ?: scope.async(Dispatchers.IO) {
                    val isrc = awaitIsrc()
                    PaxSenix.lyrics(title, artist, album, durationMs, isrc, paxSenixApiKey)
                }.also { paxAppleDef = it }
            }
            return def.await()
        }
    }
}
