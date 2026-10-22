package com.example.data.lyrics

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.net.URLEncoder
import kotlin.math.abs

fun normalizePaxSenixApiKey(raw: String): String {
    return raw.trim()
        .removePrefix("Bearer ")
        .removePrefix("bearer ")
        .trim()
}

object PaxSenix {
    private const val PUBLIC_BASE = "https://lyrics.paxsenix.org"
    private const val API_BASE = "https://api.paxsenix.org"
    private const val APPLE_WEB_URL = "https://music.apple.com/us/new"
    private const val TOKEN_TTL_MS = 30 * 60 * 1000L

    private val tokenMutex = Mutex()
    @Volatile
    private var cachedAppleToken: String? = null
    @Volatile
    private var cachedAppleTokenAtMs: Long = 0L

    data class SpotifyTrack(
        val id: String,
        val title: String,
        val artist: String,
        val album: String = "",
        val isrc: String? = null,
        val durationMs: Long = 0L,
    )

    suspend fun lyrics(
        title: String,
        artist: String,
        album: String? = null,
        durationMs: Long = 0L,
        isrc: String? = null,
        apiKey: String = "",
    ): List<LyricLine>? {
        val cleanTitle = title.forLyricsSearch()
        val cleanArtist = artist.artistForLyricsSearch()
        if (cleanTitle.isEmpty()) return null

        // 1. Public Apple Music proxy on lyrics.paxsenix.org
        val q = buildString {
            append("title=").append(URLEncoder.encode(cleanTitle, "UTF-8"))
            if (cleanArtist.isNotEmpty()) {
                append("&artist=").append(URLEncoder.encode(cleanArtist, "UTF-8"))
            }
            if (!album.isNullOrBlank()) {
                append("&album=").append(URLEncoder.encode(album.trim(), "UTF-8"))
            }
            if (durationMs > 0L) {
                append("&duration=").append(durationMs)
            }
            if (!isrc.isNullOrBlank()) {
                append("&isrc=").append(URLEncoder.encode(isrc.trim(), "UTF-8"))
            }
        }
        lyricsGet("$PUBLIC_BASE/apple-music/lyrics?$q")?.let { raw ->
            parseTimedApple(raw)?.takeIf { it.isNotEmpty() }?.let { return it }
            ProviderLyrics.parse(raw)?.takeIf { it.isNotEmpty() }?.let { return it }
        }

        // 2. If user supplied PaxSenix API key, try authenticated LrcGet / Apple Music fallback
        val normKey = normalizePaxSenixApiKey(apiKey)
        if (normKey.isNotEmpty()) {
            lyricsGetAuthorized("$API_BASE/lyrics/lrcget?$q", normKey)?.let { raw ->
                parseLrcGet(raw)?.takeIf { it.isNotEmpty() }?.let { return it }
                ProviderLyrics.parse(raw)?.takeIf { it.isNotEmpty() }?.let { return it }
            }
        }

        // 3. Direct Apple Music web catalog token scrape + lookup if ISRC or search succeeds
        val token = getAppleDeveloperToken()
        if (!token.isNullOrBlank() && !isrc.isNullOrBlank()) {
            val encodedIsrc = URLEncoder.encode(isrc.trim(), "UTF-8")
            val catalogJson = lyricsGetBearer(
                url = "https://amp-api.music.apple.com/v1/catalog/us/songs?filter[isrc]=$encodedIsrc",
                bearer = token,
                extraHeaders = mapOf("Origin" to "https://music.apple.com", "Referer" to "https://music.apple.com/"),
            )
            if (!catalogJson.isNullOrBlank()) {
                val songId = runCatching {
                    JSONObject(catalogJson).optJSONArray("data")?.optJSONObject(0)?.optString("id")
                }.getOrNull()
                if (!songId.isNullOrBlank()) {
                    lyricsGet("$PUBLIC_BASE/apple-music/lyrics?id=$songId")?.let { raw ->
                        parseTimedApple(raw)?.takeIf { it.isNotEmpty() }?.let { return it }
                        ProviderLyrics.parse(raw)?.takeIf { it.isNotEmpty() }?.let { return it }
                    }
                }
            }
        }
        return null
    }

    suspend fun spotifyLyrics(
        title: String,
        artist: String,
        album: String? = null,
        durationMs: Long = 0L,
        isrc: String? = null,
        apiKey: String = "",
    ): List<LyricLine>? {
        val normKey = normalizePaxSenixApiKey(apiKey)
        if (normKey.isEmpty()) return null
        val track = searchSpotifyTrack(title, artist, durationMs, isrc, normKey)
        val url = if (track != null && track.id.isNotBlank()) {
            "$API_BASE/lyrics/spotify?id=${URLEncoder.encode(track.id, "UTF-8")}"
        } else {
            val q = URLEncoder.encode("${title.forLyricsSearch()} ${artist.artistForLyricsSearch()}".trim(), "UTF-8")
            "$API_BASE/lyrics/spotify?q=$q"
        }
        val raw = lyricsGetAuthorized(url, normKey) ?: return null
        return parseTimedApple(raw) ?: ProviderLyrics.parse(raw)
    }

    suspend fun musixmatchLyrics(
        title: String,
        artist: String,
        album: String? = null,
        durationMs: Long = 0L,
        isrc: String? = null,
        apiKey: String = "",
    ): List<LyricLine>? {
        val normKey = normalizePaxSenixApiKey(apiKey)
        if (normKey.isEmpty()) return null
        val cleanTitle = title.forLyricsSearch()
        val cleanArtist = artist.artistForLyricsSearch()
        val params = buildString {
            append("title=").append(URLEncoder.encode(cleanTitle, "UTF-8"))
            if (cleanArtist.isNotEmpty()) {
                append("&artist=").append(URLEncoder.encode(cleanArtist, "UTF-8"))
            }
            if (!album.isNullOrBlank()) {
                append("&album=").append(URLEncoder.encode(album.trim(), "UTF-8"))
            }
            if (durationMs > 0L) {
                append("&duration=").append(durationMs / 1000L)
            }
            if (!isrc.isNullOrBlank()) {
                append("&isrc=").append(URLEncoder.encode(isrc.trim(), "UTF-8"))
            }
        }
        val raw = lyricsGetAuthorized("$API_BASE/lyrics/musixmatch?$params", normKey) ?: return null
        return parseTimedApple(raw) ?: parseLrcGet(raw) ?: ProviderLyrics.parse(raw)
    }

    private suspend fun searchSpotifyTrack(
        title: String,
        artist: String,
        durationMs: Long,
        isrc: String?,
        apiKey: String,
    ): SpotifyTrack? {
        val query = "${title.forLyricsSearch()} ${artist.artistForLyricsSearch()}".trim()
        if (query.isEmpty()) return null
        val encoded = URLEncoder.encode(query, "UTF-8")
        val raw = lyricsGetAuthorized("$API_BASE/spotify/search?q=$encoded", apiKey) ?: return null
        return try {
            val root = JSONTokener(raw).nextValue()
            val arr = when (root) {
                is JSONArray -> root
                is JSONObject -> root.optJSONArray("tracks")
                    ?: root.optJSONObject("tracks")?.optJSONArray("items")
                    ?: root.optJSONArray("data")
                    ?: root.optJSONArray("items")
                    ?: JSONArray()
                else -> JSONArray()
            }
            val candidates = ArrayList<SpotifyTrack>(arr.length())
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val id = obj.optString("id", "").trim()
                if (id.isEmpty()) continue
                val t = obj.optString("name", obj.optString("title", "")).trim()
                val a = obj.optString("artist", "").ifBlank {
                    obj.optJSONArray("artists")?.optJSONObject(0)?.optString("name", "").orEmpty()
                }
                val dur = obj.optLong("duration_ms", obj.optLong("durationMs", 0L))
                val itemIsrc = obj.optString("isrc", "").ifBlank {
                    obj.optJSONObject("external_ids")?.optString("isrc", "").orEmpty()
                }.ifBlank { null }
                candidates.add(SpotifyTrack(id = id, title = t, artist = a, isrc = itemIsrc, durationMs = dur))
            }
            if (!isrc.isNullOrBlank()) {
                candidates.firstOrNull { it.isrc?.equals(isrc.trim(), ignoreCase = true) == true }?.let { return it }
            }
            candidates.minByOrNull { c ->
                if (durationMs > 0L && c.durationMs > 0L) abs(c.durationMs - durationMs) else 0L
            }
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun getAppleDeveloperToken(): String? = tokenMutex.withLock {
        val now = System.currentTimeMillis()
        val cached = cachedAppleToken
        if (cached != null && now - cachedAppleTokenAtMs < TOKEN_TTL_MS) {
            return cached
        }
        val html = lyricsGet(
            url = APPLE_WEB_URL,
            userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
        ) ?: return null
        val scriptPath = Regex("""/assets/index[~.][^"'\s]+\.js""").find(html)?.value ?: return null
        val js = lyricsGet(
            url = "https://music.apple.com$scriptPath",
            userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
        ) ?: return null
        val jwt = Regex("""eyJh[A-Za-z0-9_-]{20,}\.[A-Za-z0-9_-]{20,}\.[A-Za-z0-9_-]{20,}""").find(js)?.value
        if (!jwt.isNullOrBlank()) {
            cachedAppleToken = jwt
            cachedAppleTokenAtMs = now
        }
        jwt
    }

    internal fun parseTimedApple(raw: String): List<LyricLine>? {
        val trimmed = raw.trim()
        if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) return null
        return try {
            val token = JSONTokener(trimmed).nextValue()
            val linesArr = when (token) {
                is JSONArray -> token
                is JSONObject -> {
                    token.optString("ttml", "").takeIf { it.isNotBlank() }?.let {
                        return TtmlLyrics.parse(it).takeIf(List<LyricLine>::isNotEmpty)
                    }
                    token.optJSONArray("content")
                        ?: token.optJSONArray("lines")
                        ?: token.optJSONArray("lyrics")
                        ?: token.optJSONObject("data")?.optJSONArray("lines")
                        ?: token.optJSONObject("data")?.optJSONArray("content")
                }
                else -> null
            } ?: return null

            val lines = ArrayList<LyricLine>(linesArr.length())
            val singers = ArrayList<String?>(linesArr.length())

            for (i in 0 until linesArr.length()) {
                val obj = linesArr.optJSONObject(i) ?: continue
                val startMs = obj.optLong("timestamp", obj.optLong("startTimeMs", obj.optLong("time", -1L)))
                val endMs = obj.optLong("endtime", obj.optLong("endTimeMs", -1L)).takeIf { it > 0L }
                val opposite = obj.optBoolean("oppositeTurn", false)
                singers.add(if (opposite) "v2" else "v1")

                val syllablesArr = obj.optJSONArray("text")
                    ?: obj.optJSONArray("syllables")
                    ?: obj.optJSONArray("words")
                if (syllablesArr != null && syllablesArr.length() > 0) {
                    val runs = ArrayList<TimedRun>()
                    val bgRuns = ArrayList<TimedRun>()
                    for (j in 0 until syllablesArr.length()) {
                        val sObj = syllablesArr.optJSONObject(j) ?: continue
                        val partText = sObj.optString("text", sObj.optString("part", ""))
                        if (partText.isEmpty()) continue
                        val sStart = sObj.optLong("timestamp", sObj.optLong("startTimeMs", sObj.optLong("time", startMs.coerceAtLeast(0L))))
                        val sEnd = sObj.optLong("endtime", sObj.optLong("endTimeMs", sStart + 300L)).coerceAtLeast(sStart)
                        val isPart = sObj.optBoolean("part", false)
                        val isBg = sObj.optBoolean("background", false)
                        val formattedRaw = if (isPart) partText else "$partText "
                        val run = TimedRun(sStart, sEnd, formattedRaw)
                        if (isBg) bgRuns.add(run) else runs.add(run)
                    }
                    val words = wordsFromRuns(runs, spacingIsExplicit = true)
                    val lineText = words.joinToString(" ") { it.text }.trim()
                    if (lineText.isEmpty()) continue
                    val bgWords = wordsFromRuns(bgRuns, spacingIsExplicit = true)
                    val bgLine = if (bgWords.isNotEmpty()) {
                        LyricLine(
                            timeMs = bgWords.first().startMs,
                            text = bgWords.joinToString(" ") { it.text },
                            words = bgWords,
                            sungUntilMs = bgWords.last().endMs,
                        )
                    } else null
                    lines.add(
                        LyricLine(
                            timeMs = words.firstOrNull()?.startMs ?: startMs.coerceAtLeast(0L),
                            text = lineText,
                            words = words,
                            sungUntilMs = endMs ?: words.lastOrNull()?.endMs,
                            background = bgLine,
                        )
                    )
                } else {
                    val plain = obj.optString("text", obj.optString("words", "")).trim()
                    if (plain.isNotEmpty() && !isGeniusSectionHeader(plain)) {
                        lines.add(
                            LyricLine(
                                timeMs = startMs.coerceAtLeast(0L),
                                text = plain,
                                sungUntilMs = endMs,
                            )
                        )
                    }
                }
            }

            if (lines.isEmpty()) return null
            val alignments = lineAlignments(singers)
            lines.mapIndexed { idx, l ->
                val align = alignments.getOrElse(idx) { LyricAlignment.Start }
                l.copy(alignment = align, background = l.background?.copy(alignment = align))
            }
                .sortedBy { it.timeMs }
                .withBackgroundVocals()
                .withInstrumentalGaps()
        } catch (_: Exception) {
            null
        }
    }

    internal fun parseLrcGet(raw: String): List<LyricLine>? {
        return try {
            val obj = JSONObject(raw.trim())
            val synced = obj.optString("syncedLyrics", obj.optString("lrc", "")).trim()
            if (synced.isNotEmpty()) return ProviderLyrics.parse(synced)
            val plain = obj.optString("plainLyrics", obj.optString("lyrics", "")).trim()
            if (plain.isNotEmpty()) return ProviderLyrics.parse(plain)
            null
        } catch (_: Exception) {
            null
        }
    }
}
