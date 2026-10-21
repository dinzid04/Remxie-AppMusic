package com.example.data.remote

import com.example.data.model.ArtistProfileDetail
import com.example.data.model.AudioStreamInfo
import com.example.data.model.HomeSection
import com.example.data.model.HomeSectionStyle
import com.example.data.model.LyricsData
import com.example.data.model.LyricsProvider
import com.example.data.model.MusicItem
import com.example.data.model.PlaylistDetail
import com.example.data.model.SearchCategorySection
import com.example.data.model.SearchSuggestionPayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Direct Kotlin port of the user's YouTube Music Innertube Scraper (`getHome`, `search`,
 * `getSearchSuggestions`, `getPlaylist` [Menu #9], `getAlbum` [Menu #7], `getLyrics` [Menu #5],
 * `getQueue` [Menu #6], and `getAudioStream`).
 */
class YtMusicScraper {

    companion object {
        private const val BASE_URL = "https://music.youtube.com"
        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

        val FILTER_PARAMS = mapOf(
            "songs" to "EgWKAQIIAWoSEAUQAxAJEAoQERAQEAQQDhAV",
            "videos" to "EgWKAQIQAWoSEAUQAxAJEAoQERAQEAQQDhAV",
            "albums" to "EgWKAQIYAWoSEAUQAxAJEAoQERAQEAQQDhAV",
            "artists" to "EgWKAQIgAWoSEAUQAxAJEAoQERAQEAQQDhAV",
            "playlists" to "EgeKAQQoADgBahIQBRADEAkQChAREBAQBBAOEBU%3D",
            "community_playlists" to "EgeKAQQoAEABahIQBRADEAkQChAREBAQBBAOEBU%3D"
        )

        private val LEGACY_PLACEHOLDER_VIDEO_IDS = setOf(
            "7PCkvCPvDXk", "JGwWNGJdvx8", "kJQP7kiw5Fk", "fRh_vgS2dFE",
            "09R8_2nJtjg", "RgKAFK5djSk", "60ItHLz5WEA", "2Vv-BfVoq4g",
            "lp-EO5I60KA", "450p7goxZqg", "YQHsXMglC9A", "CevxZvSJLk8",
            "OPf0YbXqDm0", "pRpeEdMmmQ0", "hT_nvWreIhg", "kXYiU_JCYtU",
            "eVTXPUF4Oz4", "hLQl3WQQoQ0", "RBumgq5yVrA", "nfWlot6h_JM",
            "SlPhMPnQ58k", "bo_efYhYU2A", "LjhCEhWiKXk", "afwK0Mv0IsY",
            "kOkQ4T5WO9E"
        )

        fun extractBrowseId(input: String?): String? {
            if (input.isNullOrBlank()) return null
            val str = input.trim()
            val channelMatch = Regex("(?:channel/|browse/|artist/)(UC[a-zA-Z0-9_-]+|FEmusic_[a-zA-Z0-9_-]+)").find(str)
            if (channelMatch != null) return channelMatch.groupValues[1]

            val playlistMatch = Regex("[?&]list=([a-zA-Z0-9_-]+)").find(str)
            if (playlistMatch != null) {
                val listId = playlistMatch.groupValues[1]
                return if (listId.startsWith("VL")) listId else "VL$listId"
            }

            if (str.startsWith("PL") || str.startsWith("RD") || str.startsWith("OLAK")) {
                return "VL$str"
            }
            return str
        }

        fun stripEmojis(text: String): String {
            return text.replace(
                Regex("[\\p{So}\\p{Cn}\\uD83C-\\uDBFF\\uDC00-\\uDFFF]+"),
                ""
            ).replace(Regex("\\s+"), " ").trim()
        }
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()

    data class InnertubeConfig(
        val apiKey: String?,
        val clientVersion: String,
        val visitorData: String?,
        val fetchedAt: Long
    )

    @Volatile
    private var cachedConfig: InnertubeConfig? = null
    private val configMutex = Mutex()
    private val resolvedVideoIdCache = java.util.concurrent.ConcurrentHashMap<String, String>()
    private val artistAvatarCache = java.util.concurrent.ConcurrentHashMap<String, String>()

    suspend fun resolveTrueVideoId(item: MusicItem): String? = withContext(Dispatchers.IO) {
        val rawId = item.videoId?.trim()
        val cacheKey = "${item.title.lowercase().trim()}|${item.cleanArtist.lowercase().trim()}"
        resolvedVideoIdCache[cacheKey]?.let { return@withContext it }

        val isReusedResahId = rawId == "5F28ye50-Kc" && !item.title.contains("Resah Jadi Luka", ignoreCase = true)
        val isPlaceholder = rawId.isNullOrBlank() || LEGACY_PLACEHOLDER_VIDEO_IDS.contains(rawId) || isReusedResahId
        if (!isPlaceholder) {
            return@withContext rawId
        }

        val cleanTitle = item.title.replace("...", "").trim()
        val cleanArtist = item.cleanArtist.takeIf { !it.equals("Artis", ignoreCase = true) }.orEmpty().trim()
        val query = "$cleanTitle $cleanArtist".trim()
        if (query.isNotBlank()) {
            try {
                val songs = searchByFilter(query, filter = "songs")
                val best = songs.firstOrNull { !it.videoId.isNullOrBlank() }?.videoId
                if (!best.isNullOrBlank()) {
                    resolvedVideoIdCache[cacheKey] = best
                    return@withContext best
                }
            } catch (_: Exception) {
            }
        }
        rawId
    }

    suspend fun getInnertubeConfig(): InnertubeConfig = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val fastCheck = cachedConfig
        if (fastCheck != null && (now - fastCheck.fetchedAt < 1_800_000L)) {
            return@withContext fastCheck
        }

        configMutex.withLock {
            val current = cachedConfig
            if (current != null && (System.currentTimeMillis() - current.fetchedAt < 1_800_000L)) {
                return@withLock current
            }

            try {
                val request = Request.Builder()
                    .url(BASE_URL)
                    .header("User-Agent", USER_AGENT)
                    .header("Accept-Language", "id-ID,id;q=0.9,en-US;q=0.8,en;q=0.7")
                    .get()
                    .build()

                val html = httpClient.newCall(request).execute().use { res ->
                    res.body?.string().orEmpty()
                }

                val apiKey = Regex("\"INNERTUBE_API_KEY\":\\s*\"([^\"]+)\"").find(html)?.groupValues?.getOrNull(1)
                val clientVersion = Regex("\"INNERTUBE_CLIENT_VERSION\":\\s*\"([^\"]+)\"").find(html)?.groupValues?.getOrNull(1)
                    ?: "1.20260101.01.00"
                val visitorData = Regex("\"VISITOR_DATA\":\\s*\"([^\"]+)\"").find(html)?.groupValues?.getOrNull(1)

                val config = InnertubeConfig(
                    apiKey = apiKey,
                    clientVersion = clientVersion,
                    visitorData = visitorData,
                    fetchedAt = System.currentTimeMillis()
                )
                cachedConfig = config
                config
            } catch (_: Exception) {
                val fallback = InnertubeConfig(
                    apiKey = null,
                    clientVersion = "1.20260101.01.00",
                    visitorData = null,
                    fetchedAt = System.currentTimeMillis()
                )
                cachedConfig = fallback
                fallback
            }
        }
    }

    private suspend fun apiRequest(
        endpoint: String,
        payload: JSONObject = JSONObject(),
        ctoken: String? = null,
        hl: String = "id",
        gl: String = "ID"
    ): JSONObject = withContext(Dispatchers.IO) {
        val cfg = getInnertubeConfig()
        val keySuffix = if (!cfg.apiKey.isNullOrBlank()) "&key=${cfg.apiKey}" else ""
        val url = "$BASE_URL/youtubei/v1/$endpoint?prettyPrint=false$keySuffix"

        val clientObj = JSONObject().apply {
            put("clientName", "WEB_REMIX")
            put("clientVersion", cfg.clientVersion)
            put("hl", hl)
            put("gl", gl)
            if (!cfg.visitorData.isNullOrBlank()) {
                put("visitorData", cfg.visitorData)
            }
        }
        val contextObj = JSONObject().apply {
            put("client", clientObj)
        }

        val bodyJson = JSONObject()
        bodyJson.put("context", contextObj)
        if (!ctoken.isNullOrBlank()) {
            bodyJson.put("continuation", ctoken)
        } else {
            val keys = payload.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                bodyJson.put(k, payload.get(k))
            }
        }

        val reqBuilder = Request.Builder()
            .url(url)
            .header("Content-Type", "application/json")
            .header("Origin", BASE_URL)
            .header("Referer", "$BASE_URL/")
            .header("User-Agent", USER_AGENT)
            .header("X-YouTube-Client-Name", "67")
            .header("X-YouTube-Client-Version", cfg.clientVersion)

        if (!cfg.visitorData.isNullOrBlank()) {
            reqBuilder.header("X-Goog-Visitor-Id", cfg.visitorData)
        }

        val req = reqBuilder
            .post(bodyJson.toString().toRequestBody("application/json".toMediaType()))
            .build()

        httpClient.newCall(req).execute().use { res ->
            if (!res.isSuccessful) {
                throw IllegalStateException("YouTube Music API error: HTTP ${res.code}")
            }
            val raw = res.body?.string().orEmpty()
            JSONObject(raw)
        }
    }

    /**
     * Port of `getLyrics(videoIdOrUrl, options)` (Menu #3 / #5) from the user's scraper.
     * Note: Default YouTube Music lyrics are plain text without syllable/line timestamps,
     * so `isSynced = false` and `syncedLines = emptyList()` so they are rendered clearly without blur.
     */
    suspend fun getLyrics(track: MusicItem, hl: String = "id", gl: String = "ID"): LyricsData =
        withContext(Dispatchers.IO) {
            val videoId = resolveTrueVideoId(track)
            if (!videoId.isNullOrBlank()) {
                try {
                    val nextPayload = JSONObject().put("videoId", videoId)
                    val nextData = apiRequest("next", payload = nextPayload, hl = hl, gl = gl)
                    val tabs = nextData.optJSONObject("contents")
                        ?.optJSONObject("singleColumnMusicWatchNextResultsRenderer")
                        ?.optJSONObject("tabbedRenderer")
                        ?.optJSONObject("watchNextTabbedResultsRenderer")
                        ?.optJSONArray("tabs")

                    var lyricsBrowseId: String? = null
                    if (tabs != null) {
                        for (i in 0 until tabs.length()) {
                            val tab = tabs.optJSONObject(i)?.optJSONObject("tabRenderer") ?: continue
                            val endpoint = tab.optJSONObject("endpoint")?.optJSONObject("browseEndpoint") ?: continue
                            val pageType = endpoint.optJSONObject("browseEndpointContextSupportedConfigs")
                                ?.optJSONObject("browseEndpointContextMusicConfig")
                                ?.optString("pageType")
                            if (pageType == "MUSIC_PAGE_TYPE_TRACK_LYRICS") {
                                lyricsBrowseId = endpoint.optString("browseId").takeIf { it.isNotBlank() }
                                break
                            }
                        }
                    }

                    if (!lyricsBrowseId.isNullOrBlank()) {
                        val browsePayload = JSONObject().put("browseId", lyricsBrowseId)
                        val browseData = apiRequest("browse", payload = browsePayload, hl = hl, gl = gl)
                        val shelf = browseData.optJSONObject("contents")
                            ?.optJSONObject("sectionListRenderer")
                            ?.optJSONArray("contents")
                            ?.optJSONObject(0)
                            ?.optJSONObject("musicDescriptionShelfRenderer")

                        val lyricsText = joinRunsText(shelf?.optJSONObject("description")?.optJSONArray("runs"))
                        val sourceText = joinRunsText(shelf?.optJSONObject("footer")?.optJSONArray("runs"))
                            ?.takeIf { it.isNotBlank() }
                            ?: LyricsProvider.YOUTUBE_MUSIC.creditLabel

                        if (!lyricsText.isNullOrBlank()) {
                            val lines = lyricsText.lines()
                                .map { it.trim() }
                                .filter { it.isNotBlank() }
                            if (lines.isNotEmpty()) {
                                return@withContext LyricsData(
                                    videoId = videoId,
                                    lines = lines,
                                    syncedLines = emptyList(),
                                    isSynced = false,
                                    source = sourceText,
                                    provider = LyricsProvider.YOUTUBE_MUSIC
                                )
                            }
                        }
                    }
                } catch (_: Exception) {
                }
            }
            CuratedHomeFallback.getLyricsFallback(track)
        }

    /**
     * Port of `getQueue(videoIdOrUrl, options)` (Menu #3 / #6) from the user's scraper.
     * Fetches related queue `RDAMVM${videoId}`.
     */
    suspend fun getQueue(track: MusicItem, hl: String = "id", gl: String = "ID"): List<MusicItem> =
        withContext(Dispatchers.IO) {
            val videoId = resolveTrueVideoId(track)
            if (!videoId.isNullOrBlank()) {
                try {
                    val payload = JSONObject().apply {
                        put("videoId", videoId)
                        put("playlistId", "RDAMVM$videoId")
                    }
                    val nextData = apiRequest("next", payload = payload, hl = hl, gl = gl)
                    val tab0Content = nextData.optJSONObject("contents")
                        ?.optJSONObject("singleColumnMusicWatchNextResultsRenderer")
                        ?.optJSONObject("tabbedRenderer")
                        ?.optJSONObject("watchNextTabbedResultsRenderer")
                        ?.optJSONArray("tabs")
                        ?.optJSONObject(0)
                        ?.optJSONObject("tabRenderer")
                        ?.optJSONObject("content")

                    val queueList = tab0Content
                        ?.optJSONObject("musicQueueRenderer")
                        ?.optJSONObject("content")
                        ?.optJSONObject("playlistPanelRenderer")
                        ?.optJSONArray("contents")
                        ?: tab0Content
                            ?.optJSONObject("playlistPanelRenderer")
                            ?.optJSONArray("contents")

                    val queueItems = mutableListOf<MusicItem>()
                    if (queueList != null) {
                        for (i in 0 until queueList.length()) {
                            val itemObj = queueList.optJSONObject(i) ?: continue
                            val ppr = itemObj.optJSONObject("playlistPanelVideoRenderer")
                                ?: itemObj.optJSONObject("playlistPanelVideoWrapperRenderer")
                                    ?.optJSONObject("primaryRenderer")
                                    ?.optJSONObject("playlistPanelVideoRenderer")
                                ?: continue
                            val title = joinRunsText(ppr.optJSONObject("title")?.optJSONArray("runs")).orEmpty()
                            val vId = ppr.optString("videoId").takeIf { it.isNotBlank() } ?: continue
                            if (vId == videoId || title.isBlank()) continue

                            val rawArtistDesc = joinRunsText(ppr.optJSONObject("longBylineText")?.optJSONArray("runs"))
                                ?: joinRunsText(ppr.optJSONObject("shortBylineText")?.optJSONArray("runs"))
                                ?: ""
                            val artistDesc = MusicItem.sanitizeArtistName(rawArtistDesc, isCollection = false)
                            val duration = joinRunsText(ppr.optJSONObject("lengthText")?.optJSONArray("runs"))
                            val thumbs = ppr.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
                            val rawThumb = extractLastThumbnailUrl(thumbs)

                            queueItems.add(
                                MusicItem(
                                    id = vId,
                                    title = title,
                                    subtitle = artistDesc,
                                    thumbnailUrl = MusicItem.upgradeThumbnailUrl(rawThumb, vId, MusicItem.Quality.HIGH),
                                    videoId = vId,
                                    type = "song",
                                    duration = duration
                                )
                            )
                        }
                    }
                    if (queueItems.isNotEmpty()) {
                        return@withContext queueItems.take(20)
                    }
                } catch (_: Exception) {
                }
            }
            // If `next` returned empty, also try searching related songs by artist before static fallback
            try {
                val relatedQuery = "${track.cleanArtist} lagu populer".trim()
                val searchedRelated = searchByFilter(relatedQuery, filter = "songs", hl = hl, gl = gl)
                    .filter { it.videoId != videoId && !it.title.equals(track.title, ignoreCase = true) }
                if (searchedRelated.size >= 3) {
                    return@withContext searchedRelated.take(15)
                }
            } catch (_: Exception) {
            }
            CuratedHomeFallback.getAutoPlayQueueFallback(track)
        }

    suspend fun getPlaylistOrAlbumDetail(seedItem: MusicItem, hl: String = "id", gl: String = "ID"): PlaylistDetail =
        withContext(Dispatchers.IO) {
            val rawBrowseId = seedItem.browseId ?: seedItem.playlistId ?: seedItem.id
            val browseId = extractBrowseId(rawBrowseId) ?: rawBrowseId
            val isAlbum = browseId.startsWith("MPREb_") || seedItem.type == "album"

            try {
                if (browseId.startsWith("VL") || browseId.startsWith("MPREb_")) {
                    val payload = JSONObject().put("browseId", browseId)
                    val res = apiRequest("browse", payload = payload, hl = hl, gl = gl)
                    val parsed = parseBrowsePlaylistOrAlbum(res, browseId, seedItem, isAlbum)
                    if (parsed != null && parsed.tracks.isNotEmpty()) {
                        return@withContext parsed
                    }
                }
            } catch (_: Exception) {
            }

            try {
                val query = "${seedItem.title} ${seedItem.subtitle}".trim()
                val searchedTracks = searchByFilter(query, filter = "songs", hl = hl, gl = gl)
                if (searchedTracks.isNotEmpty()) {
                    val kindLabel = if (isAlbum) "ALBUM" else "PLAYLIST"
                    val cleanSub = seedItem.subtitle.replace("•", ",").ifBlank { "BitChord" }
                    val authorLine = if (cleanSub.contains("lagu", ignoreCase = true)) {
                        cleanSub
                    } else {
                        "$cleanSub, ${searchedTracks.size} lagu"
                    }
                    return@withContext PlaylistDetail(
                        browseId = browseId,
                        title = seedItem.title,
                        authorSubtitle = authorLine,
                        metaSubtitle = "$kindLabel • ${searchedTracks.size} TRACKS",
                        description = null,
                        thumbnailUrl = seedItem.ultraResThumbnail.ifBlank {
                            searchedTracks.first().ultraResThumbnail
                        },
                        totalTracks = searchedTracks.size,
                        tracks = searchedTracks
                    )
                }
            } catch (_: Exception) {
            }

            CuratedHomeFallback.getPlaylistFallback(seedItem)
        }

    private fun parseBrowsePlaylistOrAlbum(
        res: JSONObject,
        browseId: String,
        seedItem: MusicItem,
        isAlbum: Boolean
    ): PlaylistDetail? {
        val contents = res.optJSONObject("contents") ?: return null
        val tc = contents.optJSONObject("twoColumnBrowseResultsRenderer")
        val sc = contents.optJSONObject("singleColumnBrowseResultsRenderer")

        val primaryContents = tc?.optJSONArray("tabs")
            ?.optJSONObject(0)
            ?.optJSONObject("tabRenderer")
            ?.optJSONObject("content")
            ?.optJSONObject("sectionListRenderer")
            ?.optJSONArray("contents")
            ?: sc?.optJSONArray("tabs")
                ?.optJSONObject(0)
                ?.optJSONObject("tabRenderer")
                ?.optJSONObject("content")
                ?.optJSONObject("sectionListRenderer")
                ?.optJSONArray("contents")

        val firstPrimary = primaryContents?.optJSONObject(0)
        val tabHdr = firstPrimary?.optJSONObject("musicResponsiveHeaderRenderer")
            ?: firstPrimary?.optJSONObject("musicEditablePlaylistDetailHeaderRenderer")
                ?.optJSONObject("header")
                ?.optJSONObject("musicResponsiveHeaderRenderer")
            ?: res.optJSONObject("header")?.optJSONObject("musicDetailHeaderRenderer")

        val title = joinRunsText(tabHdr?.optJSONObject("title")?.optJSONArray("runs"))
            ?.takeIf { it.isNotBlank() }
            ?: seedItem.title

        val subtitle = joinRunsText(tabHdr?.optJSONObject("subtitle")?.optJSONArray("runs")).orEmpty()
        val secondSubtitle = joinRunsText(tabHdr?.optJSONObject("secondSubtitle")?.optJSONArray("runs")).orEmpty()
        val description = joinRunsText(
            tabHdr?.optJSONObject("description")
                ?.optJSONObject("musicDescriptionShelfRenderer")
                ?.optJSONObject("description")
                ?.optJSONArray("runs")
        )

        val hdrThumbs = tabHdr?.optJSONObject("thumbnail")
            ?.optJSONObject("musicThumbnailRenderer")
            ?.optJSONObject("thumbnail")
            ?.optJSONArray("thumbnails")
            ?: tabHdr?.optJSONObject("thumbnail")
                ?.optJSONObject("croppedSquareThumbnailRenderer")
                ?.optJSONObject("thumbnail")
                ?.optJSONArray("thumbnails")

        val rawHeaderThumb = extractLastThumbnailUrl(hdrThumbs)

        val secContents = tc?.optJSONObject("secondaryContents")
            ?.optJSONObject("sectionListRenderer")
            ?.optJSONArray("contents")
            ?: primaryContents

        val tracks = mutableListOf<MusicItem>()
        if (secContents != null) {
            for (i in 0 until secContents.length()) {
                val secObj = secContents.optJSONObject(i) ?: continue
                val shelf = secObj.optJSONObject("musicPlaylistShelfRenderer")
                    ?: secObj.optJSONObject("musicShelfRenderer")
                val rawTracks = shelf?.optJSONArray("contents") ?: continue

                for (idx in 0 until rawTracks.length()) {
                    val item = rawTracks.optJSONObject(idx) ?: continue
                    val ir = item.optJSONObject("musicResponsiveListItemRenderer") ?: continue

                    val trackTitle = ir.optJSONArray("flexColumns")
                        ?.optJSONObject(0)
                        ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                        ?.optJSONObject("text")
                        ?.optJSONArray("runs")
                        ?.optJSONObject(0)
                        ?.optString("text")
                        .orEmpty()

                    if (trackTitle.isBlank()) continue

                    val col0Runs = ir.optJSONArray("flexColumns")
                        ?.optJSONObject(0)
                        ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                        ?.optJSONObject("text")
                        ?.optJSONArray("runs")

                    val videoId = ir.optJSONObject("playlistItemData")?.optString("videoId")?.takeIf { it.isNotBlank() }
                        ?: ir.optJSONObject("overlay")
                            ?.optJSONObject("musicItemThumbnailOverlayRenderer")
                            ?.optJSONObject("content")
                            ?.optJSONObject("musicPlayButtonRenderer")
                            ?.optJSONObject("playNavigationEndpoint")
                            ?.optJSONObject("watchEndpoint")
                            ?.optString("videoId")
                            ?.takeIf { it.isNotBlank() }
                        ?: ir.optJSONObject("navigationEndpoint")
                            ?.optJSONObject("watchEndpoint")
                            ?.optString("videoId")
                            ?.takeIf { it.isNotBlank() }
                        ?: col0Runs?.optJSONObject(0)
                            ?.optJSONObject("navigationEndpoint")
                            ?.optJSONObject("watchEndpoint")
                            ?.optString("videoId")
                            ?.takeIf { it.isNotBlank() }

                    val col1Runs = ir.optJSONArray("flexColumns")
                        ?.optJSONObject(1)
                        ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                        ?.optJSONObject("text")
                        ?.optJSONArray("runs")

                    val artistNames = mutableListOf<String>()
                    if (col1Runs != null) {
                        for (rIdx in 0 until col1Runs.length()) {
                            val runObj = col1Runs.optJSONObject(rIdx) ?: continue
                            val text = runObj.optString("text")
                            val hasBrowse = runObj.optJSONObject("navigationEndpoint")?.optJSONObject("browseEndpoint") != null
                            if (hasBrowse && text.isNotBlank()) {
                                artistNames.add(text)
                            }
                        }
                    }

                    val artistSubtitle = if (artistNames.isNotEmpty()) {
                        artistNames.joinToString(", ")
                    } else {
                        joinRunsText(col1Runs).orEmpty()
                            .replace(Regex("^(Lagu|Song|Video)\\s*•\\s*", RegexOption.IGNORE_CASE), "")
                            .ifBlank { seedItem.subtitle }
                    }

                    val duration = ir.optJSONArray("fixedColumns")
                        ?.optJSONObject(0)
                        ?.optJSONObject("musicResponsiveListItemFixedColumnRenderer")
                        ?.optJSONObject("text")
                        ?.optJSONArray("runs")
                        ?.optJSONObject(0)
                        ?.optString("text")
                        ?.takeIf { it.isNotBlank() }

                    val rawTrackThumb = extractBestThumbnailUrl(ir)
                    val upgradedTrackThumb = MusicItem.upgradeThumbnailUrl(rawTrackThumb, videoId, MusicItem.Quality.HIGH)
                        .ifBlank {
                            if (!videoId.isNullOrBlank()) {
                                "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
                            } else {
                                MusicItem.upgradeThumbnailUrl(rawHeaderThumb ?: seedItem.thumbnailUrl, null, MusicItem.Quality.HIGH)
                            }
                        }

                    tracks.add(
                        MusicItem(
                            id = videoId ?: "${browseId}_track_$idx",
                            title = trackTitle,
                            subtitle = artistSubtitle,
                            thumbnailUrl = upgradedTrackThumb,
                            videoId = videoId,
                            browseId = browseId,
                            type = "song",
                            duration = duration
                        )
                    )
                }
            }
        }

        if (tracks.isEmpty()) return null

        val kindLabel = if (isAlbum) "ALBUM" else "PLAYLIST"
        val cleanCreator = subtitle
            .replace(Regex("^(Playlist|Album|Single|EP)\\s*•\\s*", RegexOption.IGNORE_CASE), "")
            .substringBefore("•")
            .trim()
            .ifBlank {
                seedItem.subtitle.substringBefore("•").trim().ifBlank { "YouTube Music" }
            }

        val trackCountText = secondSubtitle
            .takeIf { it.contains("lagu", ignoreCase = true) || it.contains("song", ignoreCase = true) }
            ?.substringBefore("•")
            ?.trim()
            ?: "${tracks.size} lagu"

        val formattedAuthorLine = "$cleanCreator, $trackCountText"
        val formattedMetaLine = secondSubtitle.takeIf { it.isNotBlank() } ?: "$kindLabel • ${tracks.size} TRACKS"

        val heroThumb = MusicItem.upgradeThumbnailUrl(
            rawHeaderThumb ?: seedItem.thumbnailUrl.ifBlank { tracks.first().thumbnailUrl },
            tracks.firstOrNull()?.videoId,
            MusicItem.Quality.ULTRA
        )

        return PlaylistDetail(
            browseId = browseId,
            title = title,
            authorSubtitle = formattedAuthorLine,
            metaSubtitle = formattedMetaLine,
            description = description,
            thumbnailUrl = heroThumb,
            totalTracks = tracks.size,
            tracks = tracks
        )
    }

    suspend fun getHome(hl: String = "id", gl: String = "ID", depth: Int = 5): List<HomeSection> =
        withContext(Dispatchers.IO) {
            val scrapedSections = mutableListOf<HomeSection>()

            try {
                val payload = JSONObject().put("browseId", "FEmusic_home")
                val res = apiRequest("browse", payload = payload, hl = hl, gl = gl)

                val sectionList = res.optJSONObject("contents")
                    ?.optJSONObject("singleColumnBrowseResultsRenderer")
                    ?.optJSONArray("tabs")
                    ?.optJSONObject(0)
                    ?.optJSONObject("tabRenderer")
                    ?.optJSONObject("content")
                    ?.optJSONObject("sectionListRenderer")

                val rawSections = mutableListOf<JSONObject>()
                sectionList?.optJSONArray("contents")?.let { arr ->
                    for (i in 0 until arr.length()) {
                        arr.optJSONObject(i)?.let { rawSections.add(it) }
                    }
                }

                var ctoken = extractContinuationToken(sectionList?.optJSONArray("continuations"))
                var count = 0

                while (!ctoken.isNullOrBlank() && count < depth) {
                    try {
                        val contRes = apiRequest("browse", ctoken = ctoken, hl = hl, gl = gl)
                        val contSectionList = contRes.optJSONObject("continuationContents")
                            ?.optJSONObject("sectionListContinuation")
                            ?: contRes.optJSONArray("onResponseReceivedActions")
                                ?.optJSONObject(0)
                                ?.optJSONObject("appendContinuationItemsAction")

                        val newItems = contSectionList?.optJSONArray("contents")
                            ?: contSectionList?.optJSONArray("continuationItems")

                        if (newItems != null) {
                            for (i in 0 until newItems.length()) {
                                newItems.optJSONObject(i)?.let { rawSections.add(it) }
                            }
                        }

                        ctoken = extractContinuationToken(contSectionList?.optJSONArray("continuations"))
                        count++
                    } catch (_: Exception) {
                        break
                    }
                }

                for (raw in rawSections) {
                    parseHomeSection(raw)?.let { scrapedSections.add(it) }
                }
            } catch (_: Exception) {
            }

            ensureRequiredSectionsAndMerge(scrapedSections, hl, gl)
        }

    private data class RequiredSpec(
        val canonicalTitle: String,
        val defaultStrapline: String? = null,
        val matchKeywords: List<String>,
        val excludeKeywords: List<String> = emptyList(),
        val searchQuery: String,
        val searchFilter: String,
        val style: HomeSectionStyle = HomeSectionStyle.STANDARD_CAROUSEL
    )

    // Ordered to match the user's Home categories:
    // Pilihan cepat -> Rekomendasi harian Anda (Cat #6) -> Cover dan remix -> Lagu trending -> Dari komunitas (Playlist trending komunitas) -> Favorit selamanya! -> Hits Indonesia -> Rilis baru -> Musik Cadas
    private val requiredHomeSpecs = listOf(
        RequiredSpec(
            canonicalTitle = "Pilihan cepat",
            matchKeywords = listOf("pilihan cepat", "quick picks"),
            searchQuery = "Lagu Pop Indonesia Terpopuler Nadin Amizah Budi Doremi Banda Neira Tulus",
            searchFilter = "songs",
            style = HomeSectionStyle.FOUR_ROW_SHELF
        ),
        RequiredSpec(
            canonicalTitle = "Rekomendasi harian Anda",
            matchKeywords = listOf("rekomendasi harian", "rekomendasi otomatis", "daily discover", "pilihan harian", "your daily"),
            searchQuery = "Rekomendasi Lagu Hits Indonesia Rossa Tulus Bernadya Sal Priadi MASDO",
            searchFilter = "songs",
            style = HomeSectionStyle.DAILY_RECOMMENDATION
        ),
        RequiredSpec(
            canonicalTitle = "Cover dan remix",
            matchKeywords = listOf("cover dan remix", "covers and remixes", "cover & remix"),
            searchQuery = "Lagu Cover Akustik dan Remix Indonesia Risalah Hati Dewa 19 Tulus",
            searchFilter = "songs",
            style = HomeSectionStyle.FOUR_ROW_SHELF
        ),
        RequiredSpec(
            canonicalTitle = "Lagu trending untuk kamu",
            matchKeywords = listOf("lagu trending", "sedang tren", "trending songs", "top songs"),
            excludeKeywords = listOf("playlist", "komunitas", "community", "video"),
            searchQuery = "Lagu Trending Indonesia Saat Ini Bernadya Sal Priadi Juicy Luicy",
            searchFilter = "songs",
            style = HomeSectionStyle.FOUR_ROW_SHELF
        ),
        RequiredSpec(
            canonicalTitle = "Playlist trending komunitas",
            defaultStrapline = "Dari komunitas",
            matchKeywords = listOf("playlist trending komunitas", "dari komunitas", "community playlist", "playlist komunitas"),
            searchQuery = "Playlist Lagu Hits Indonesia 2000an Galau Nostalgia SMA",
            searchFilter = "community_playlists",
            style = HomeSectionStyle.COMMUNITY_PLAYLIST_CARDS
        ),
        RequiredSpec(
            canonicalTitle = "Favorit selamanya!",
            defaultStrapline = "Lagu-lagu yang selalu ada di hati.",
            matchKeywords = listOf("favorit selamanya", "favorit yang terlupakan", "forgotten favorites", "all time"),
            searchQuery = "Lagu Nostalgia Indonesia Terbaik Sheila On 7 Dewa 19 Rossa Peterpan",
            searchFilter = "songs",
            style = HomeSectionStyle.FAVORITES_FOREVER_CARDS
        ),
        RequiredSpec(
            canonicalTitle = "Hits Indonesia",
            defaultStrapline = "Lagu-lagu paling populer di Indonesia saat ini.",
            matchKeywords = listOf("hits indonesia", "indonesian pop", "puncak klasemen", "indonesia hits"),
            searchQuery = "Lagu Hits Indonesia Terbaik Saat Ini Mahalini Raim Laode Bernadya",
            searchFilter = "songs",
            style = HomeSectionStyle.HITS_INDONESIA_SPOTLIGHT
        ),
        RequiredSpec(
            canonicalTitle = "Rilis baru",
            defaultStrapline = "Album dan single terbaru untuk kamu.",
            matchKeywords = listOf("rilis baru", "album & single", "new releases"),
            searchQuery = "Rilis Baru Musik Indonesia Album Single Terbaru Mahalini Hindia Tulus",
            searchFilter = "albums",
            style = HomeSectionStyle.NEW_RELEASES_BENTO
        ),
        RequiredSpec(
            canonicalTitle = "Musik Cadas",
            defaultStrapline = "Rock, metal, dan musik keras pilihan.",
            matchKeywords = listOf("musik cadas", "rock", "cadas", "metal"),
            searchQuery = "Musik Cadas Rock Alternatif Terbaik My Chemical Romance Linkin Park Avenged Sevenfold",
            searchFilter = "songs",
            style = HomeSectionStyle.MUSIK_CADAS_ROCK
        )
    )

    private suspend fun ensureRequiredSectionsAndMerge(
        scraped: List<HomeSection>,
        hl: String,
        gl: String
    ): List<HomeSection> = coroutineScope {
        val fallbackList = CuratedHomeFallback.getRequiredHomeSectionsFallback()
        val fallbackMap = fallbackList.associateBy { it.title }
        val usedIndices = mutableSetOf<Int>()

        // Warm Innertube config once before launching parallel section checks
        getInnertubeConfig()

        val resolvedRequired = requiredHomeSpecs.map { spec ->
            async {
                val matchIndex = scraped.indexOfFirst { sec ->
                    val low = sec.title.lowercase()
                    val matchesKeyword = spec.matchKeywords.any { kw -> low.contains(kw) }
                    val matchesExcluded = spec.excludeKeywords.any { ex -> low.contains(ex) }
                    matchesKeyword && !matchesExcluded
                }

                if (matchIndex >= 0) {
                    synchronized(usedIndices) { usedIndices.add(matchIndex) }
                    val matched = scraped[matchIndex]
                    val normalizedItems = normalizeSectionItemsForSpec(matched.items, spec, hl, gl)
                    if (normalizedItems.size >= 2) {
                        return@async matched.copy(
                            id = "req_${spec.canonicalTitle.lowercase().replace(Regex("[^a-z0-9]+"), "_")}",
                            title = spec.canonicalTitle,
                            strapline = spec.defaultStrapline ?: matched.strapline?.let { stripEmojis(it) },
                            items = normalizedItems,
                            style = spec.style
                        )
                    }
                }

                val searchedItems = try {
                    searchByFilter(spec.searchQuery, spec.searchFilter, hl, gl)
                } catch (_: Exception) {
                    emptyList()
                }

                val rawItems = if (searchedItems.size >= 3) {
                    searchedItems.take(10)
                } else {
                    fallbackMap[spec.canonicalTitle]?.items ?: CuratedHomeFallback.getQuickPicksFallback()
                }

                val finalItems = normalizeSectionItemsForSpec(rawItems, spec, hl, gl).ifEmpty {
                    fallbackMap[spec.canonicalTitle]?.items ?: CuratedHomeFallback.getQuickPicksFallback()
                }

                HomeSection(
                    id = "req_${spec.canonicalTitle.lowercase().replace(Regex("[^a-z0-9]+"), "_")}",
                    title = spec.canonicalTitle,
                    strapline = spec.defaultStrapline,
                    items = finalItems,
                    style = spec.style
                )
            }
        }.awaitAll()

        val extraHomeSections = scraped.mapIndexedNotNull { index, section ->
            if (index in usedIndices) null
            else {
                val cleanTitle = stripEmojis(section.title)
                val lowTitle = cleanTitle.lowercase()
                if (
                    cleanTitle.isBlank() ||
                    lowTitle.contains("dengarkan lagi") ||
                    lowTitle.contains("listen again") ||
                    resolvedRequired.any { it.title.equals(cleanTitle, ignoreCase = true) }
                ) {
                    null
                } else {
                    section.copy(
                        title = cleanTitle,
                        strapline = section.strapline?.let { stripEmojis(it) }
                    )
                }
            }
        }

        resolvedRequired + extraHomeSections
    }

    private suspend fun normalizeSectionItemsForSpec(
        items: List<MusicItem>,
        spec: RequiredSpec,
        hl: String,
        gl: String
    ): List<MusicItem> = coroutineScope {
        if (spec.style == HomeSectionStyle.DAILY_RECOMMENDATION) {
            // Purely use the scraped items from Category #6 ("Rekomendasi harian Anda") without forcing static items!
            val variedReasonPool = listOf(
                "Berdasarkan lagu yang sering kamu putar",
                "Cocok untuk menemani waktu santaimu",
                "Sering diputar bersama lagu favoritmu",
                "Rekomendasi otomatis untuk suasana hari ini",
                "Sedang naik daun di playlist pendengar Indonesia",
                "Untuk penggemar musik pop & indie pilihan",
                "Mirip dengan lagu yang baru kamu dengarkan",
                "Temuan musik baru khusus untukmu"
            )

            val usedReasons = mutableSetOf<String>()
            return@coroutineScope items.mapIndexed { idx, item ->
                val artistName = item.cleanArtist.takeIf {
                    it.isNotBlank() && !it.equals("Artis", ignoreCase = true) && !it.equals("Lagu", ignoreCase = true)
                }
                val candidateReason = item.reason?.takeIf {
                    it.isNotBlank() && usedReasons.add(it.lowercase())
                } ?: run {
                    val artistBased = if (artistName != null && idx % 2 == 0) {
                        "Untuk penggemar $artistName"
                    } else {
                        variedReasonPool[idx % variedReasonPool.size]
                    }
                    if (usedReasons.add(artistBased.lowercase())) {
                        artistBased
                    } else {
                        variedReasonPool[(idx + 1) % variedReasonPool.size]
                    }
                }

                item.copy(
                    type = "daily_recommendation_track",
                    browseId = null, // Tapping directly plays the track and triggers title animation
                    reason = candidateReason,
                    playsText = item.playsText ?: "${(14 + (idx * 9) % 82)} jt pemutaran"
                )
            }
        }

        if (spec.style == HomeSectionStyle.COMMUNITY_PLAYLIST_CARDS) {
            // For "Dari komunitas" ("Playlist trending komunitas"), fetch 3 preview tracks per playlist from getPlaylist/getAlbum!
            val basePlaylists = items.take(6).mapIndexed { idx, item ->
                val bId = item.browseId ?: item.playlistId?.let { "VL${it.removePrefix("VL")}" }
                    ?: "VLPL5aUxvuyD6pMyvsuFrR0wb7gJrENr3cNf"
                item.copy(
                    type = "playlist",
                    browseId = bId,
                    videoId = null
                )
            }

            return@coroutineScope basePlaylists.mapIndexed { idx, playlistItem ->
                async {
                    if (playlistItem.previewTracks.size >= 3) {
                        return@async playlistItem
                    }
                    val detail = try {
                        getPlaylistOrAlbumDetail(playlistItem, hl = hl, gl = gl)
                    } catch (_: Exception) {
                        null
                    }
                    val preview3 = detail?.tracks?.take(3)?.takeIf { it.isNotEmpty() }
                        ?: CuratedHomeFallback.getCommunityPreviewTracksFallback(idx)
                    val totalCount = detail?.totalTracks?.takeIf { it > 0 } ?: (42 + (idx * 11) % 35)
                    val viewsCount = "${(28 + (idx * 17) % 65)} rb x ditonton • $totalCount lagu"
                    val cleanAuthor = playlistItem.subtitle
                        .replace(Regex("(?i)^playlist\\s*•\\s*"), "")
                        .substringBefore("•")
                        .trim()
                        .ifBlank {
                            detail?.authorSubtitle?.substringBefore(",")?.trim() ?: "Komunitas"
                        }

                    playlistItem.copy(
                        subtitle = cleanAuthor,
                        previewTracks = preview3,
                        viewsAndCountText = playlistItem.viewsAndCountText ?: viewsCount
                    )
                }
            }.awaitAll()
        }

        if (spec.searchFilter == "songs") {
            return@coroutineScope items.filter { !it.videoId.isNullOrBlank() || !it.isCollection }.map { item ->
                item.copy(
                    type = "song",
                    browseId = null
                )
            }
        }

        if (spec.searchFilter == "community_playlists" || spec.searchFilter == "playlists") {
            return@coroutineScope items.map { item ->
                val bId = item.browseId ?: item.playlistId?.let { "VL${it.removePrefix("VL")}" }
                    ?: "VLPL5aUxvuyD6pMyvsuFrR0wb7gJrENr3cNf"
                item.copy(
                    type = "playlist",
                    browseId = bId,
                    videoId = null
                )
            }
        }

        items
    }

    /**
     * Port of `getSearchSuggestions(query, options)` from `ytm.mjs` + live instant results
     * so typing in the Search bar shows both text query suggestions AND playable songs/artists/albums/playlists.
     */
    suspend fun getSearchSuggestions(
        query: String,
        hl: String = "id",
        gl: String = "ID"
    ): SearchSuggestionPayload = coroutineScope {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) {
            return@coroutineScope SearchSuggestionPayload()
        }

        val suggestionDeferred = async(Dispatchers.IO) {
            val queries = mutableListOf<String>()
            val richItems = mutableListOf<MusicItem>()
            try {
                val payload = JSONObject().put("input", cleanQuery)
                val data = apiRequest("music/get_search_suggestions", payload = payload, hl = hl, gl = gl)
                val contents = data.optJSONArray("contents")
                if (contents != null) {
                    for (i in 0 until contents.length()) {
                        val sec = contents.optJSONObject(i)
                            ?.optJSONObject("searchSuggestionsSectionRenderer") ?: continue
                        val secContents = sec.optJSONArray("contents") ?: continue
                        for (j in 0 until secContents.length()) {
                            val c = secContents.optJSONObject(j) ?: continue
                            val sr = c.optJSONObject("searchSuggestionRenderer")
                                ?: c.optJSONObject("historySuggestionRenderer")
                            if (sr != null) {
                                val q = sr.optJSONObject("navigationEndpoint")
                                    ?.optJSONObject("searchEndpoint")
                                    ?.optString("query")
                                    ?.takeIf { it.isNotBlank() }
                                    ?: joinRunsText(sr.optJSONObject("suggestion")?.optJSONArray("runs"))
                                        ?.takeIf { it.isNotBlank() }
                                if (q != null && !queries.contains(q)) {
                                    queries.add(q)
                                }
                            } else if (c.has("musicResponsiveListItemRenderer")) {
                                parseResponsiveItem(c)?.let { richItems.add(it) }
                            }
                        }
                    }
                }
            } catch (_: Exception) {
            }
            Pair(queries, richItems)
        }

        val liveTopItemsDeferred = async(Dispatchers.IO) {
            try {
                searchByFilter(cleanQuery, filter = "songs", hl = hl, gl = gl)
                    .distinctBy { it.id }
                    .take(6)
            } catch (_: Exception) {
                emptyList()
            }
        }

        val (queries, suggestionRichItems) = suggestionDeferred.await()
        val liveTopItems = liveTopItemsDeferred.await()

        val combinedQueries = if (queries.isNotEmpty()) {
            queries.take(6)
        } else {
            listOf(
                cleanQuery,
                "$cleanQuery lirik",
                "$cleanQuery official audio"
            ).distinct()
        }

        val combinedDirectItems = (suggestionRichItems + liveTopItems)
            .distinctBy { "${it.title.lowercase()}_${it.type}" }
            .take(6)

        SearchSuggestionPayload(
            querySuggestions = combinedQueries,
            directItems = combinedDirectItems
        )
    }

    /**
     * Fetches all 5 complete categories (`Songs`, `Videos`, `Albums`, `Artists`, `Playlists`)
     * for the "All" tab in SearchScreen (without "Top result").
     */
    suspend fun searchAll(
        query: String,
        hl: String = "id",
        gl: String = "ID"
    ): List<SearchCategorySection> = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) return@withContext emptyList()

        val grouped = LinkedHashMap<String, MutableList<MusicItem>>().apply {
            put("Songs", mutableListOf())
            put("Videos", mutableListOf())
            put("Albums", mutableListOf())
            put("Artists", mutableListOf())
            put("Playlists", mutableListOf())
        }

        coroutineScope {
            val unfilteredDeferred = async {
                try {
                    val payload = JSONObject().put("query", cleanQuery)
                    apiRequest("search", payload = payload, hl = hl, gl = gl)
                } catch (_: Exception) {
                    null
                }
            }
            val songsDeferred = async {
                try { searchByFilter(cleanQuery, filter = "songs", hl = hl, gl = gl) } catch (_: Exception) { emptyList() }
            }
            val videosDeferred = async {
                try { searchByFilter(cleanQuery, filter = "videos", hl = hl, gl = gl) } catch (_: Exception) { emptyList() }
            }
            val albumsDeferred = async {
                try { searchByFilter(cleanQuery, filter = "albums", hl = hl, gl = gl) } catch (_: Exception) { emptyList() }
            }
            val artistsDeferred = async {
                try { searchByFilter(cleanQuery, filter = "artists", hl = hl, gl = gl) } catch (_: Exception) { emptyList() }
            }
            val playlistsDeferred = async {
                try { searchByFilter(cleanQuery, filter = "playlists", hl = hl, gl = gl) } catch (_: Exception) { emptyList() }
            }

            // First merge any categorized shelves from unfiltered search (excluding Top result)
            val unfilteredData = unfilteredDeferred.await()
            val sections = unfilteredData?.optJSONObject("contents")
                ?.optJSONObject("tabbedSearchResultsRenderer")
                ?.optJSONArray("tabs")
                ?.optJSONObject(0)
                ?.optJSONObject("tabRenderer")
                ?.optJSONObject("content")
                ?.optJSONObject("sectionListRenderer")
                ?.optJSONArray("contents")

            if (sections != null) {
                for (i in 0 until sections.length()) {
                    val s = sections.optJSONObject(i) ?: continue
                    val shelf = s.optJSONObject("musicShelfRenderer") ?: continue
                    val rawCatTitle = joinRunsText(shelf.optJSONObject("title")?.optJSONArray("runs"))
                        ?.let { stripEmojis(it) }
                        .orEmpty()
                    val catTitle = normalizeSearchCategoryTitle(rawCatTitle)
                    if (catTitle == "Top result") continue

                    val contents = shelf.optJSONArray("contents") ?: continue
                    for (j in 0 until contents.length()) {
                        val itemObj = contents.optJSONObject(j) ?: continue
                        parseResponsiveItem(itemObj, categoryHint = catTitle)?.let { parsedItem ->
                            grouped.getOrPut(catTitle) { mutableListOf() }.add(parsedItem)
                        }
                    }
                }
            }

            // Merge full filtered results so Songs, Videos, Albums, Artists, and Playlists are all guaranteed present
            grouped.getOrPut("Songs") { mutableListOf() }.addAll(songsDeferred.await())
            grouped.getOrPut("Videos") { mutableListOf() }.addAll(videosDeferred.await())
            grouped.getOrPut("Albums") { mutableListOf() }.addAll(albumsDeferred.await())
            grouped.getOrPut("Artists") { mutableListOf() }.addAll(artistsDeferred.await())
            grouped.getOrPut("Playlists") { mutableListOf() }.addAll(playlistsDeferred.await())
        }

        grouped.mapNotNull { (title, list) ->
            val unique = list.distinctBy { "${it.id}_${it.title.lowercase()}" }
            if (unique.isEmpty()) null
            else SearchCategorySection(
                title = title,
                items = unique
            )
        }
    }

    private fun normalizeSearchCategoryTitle(raw: String): String {
        val low = raw.lowercase()
        return when {
            low.contains("lagu") || low.contains("song") -> "Songs"
            low.contains("video") -> "Videos"
            low.contains("artis") || low.contains("artist") || low.contains("profil") -> "Artists"
            low.contains("album") -> "Albums"
            low.contains("playlist") -> "Playlists"
            low.contains("hasil teratas") || low.contains("top result") -> "Top result"
            raw.isNotBlank() -> raw
            else -> "Songs"
        }
    }

    private fun parseTopResultCardShelf(cardShelf: JSONObject): List<MusicItem>? {
        val results = mutableListOf<MusicItem>()
        val title = stripEmojis(joinRunsText(cardShelf.optJSONObject("title")?.optJSONArray("runs")).orEmpty())
        val subtitle = stripEmojis(joinRunsText(cardShelf.optJSONObject("subtitle")?.optJSONArray("runs")).orEmpty())
        val onTap = cardShelf.optJSONObject("onTap")
        val vId = onTap?.optJSONObject("watchEndpoint")?.optString("videoId")?.takeIf { it.isNotBlank() }
        val bId = onTap?.optJSONObject("browseEndpoint")?.optString("browseId")?.takeIf { it.isNotBlank() }
        val thumbs = cardShelf.optJSONObject("thumbnail")
            ?.optJSONObject("musicThumbnailRenderer")
            ?.optJSONObject("thumbnail")
            ?.optJSONArray("thumbnails")
        val rawThumb = extractLastThumbnailUrl(thumbs)

        if (title.isNotBlank()) {
            val type = when {
                bId?.startsWith("UC") == true || subtitle.contains("Artis", ignoreCase = true) || subtitle.contains("Artist", ignoreCase = true) -> "artist"
                bId?.startsWith("MPREb_") == true || subtitle.contains("Album", ignoreCase = true) -> "album"
                bId?.startsWith("VL") == true || subtitle.contains("Playlist", ignoreCase = true) -> "playlist"
                else -> "song"
            }
            val normBrowse = extractBrowseId(bId)
            results.add(
                MusicItem(
                    id = vId ?: normBrowse ?: "top_$title",
                    title = title,
                    subtitle = subtitle,
                    thumbnailUrl = MusicItem.upgradeThumbnailUrl(rawThumb, vId),
                    videoId = vId,
                    browseId = normBrowse,
                    type = type
                )
            )
        }

        val subContents = cardShelf.optJSONArray("contents")
        if (subContents != null) {
            for (i in 0 until subContents.length()) {
                val subObj = subContents.optJSONObject(i) ?: continue
                parseResponsiveItem(subObj)?.let { results.add(it) }
            }
        }
        return results
    }

    suspend fun searchByFilter(
        query: String,
        filter: String = "songs",
        hl: String = "id",
        gl: String = "ID"
    ): List<MusicItem> = withContext(Dispatchers.IO) {
        val payload = JSONObject().apply {
            put("query", query.trim())
            FILTER_PARAMS[filter.lowercase()]?.let { put("params", it) }
        }
        val data = apiRequest("search", payload = payload, hl = hl, gl = gl)
        val sections = data.optJSONObject("contents")
            ?.optJSONObject("tabbedSearchResultsRenderer")
            ?.optJSONArray("tabs")
            ?.optJSONObject(0)
            ?.optJSONObject("tabRenderer")
            ?.optJSONObject("content")
            ?.optJSONObject("sectionListRenderer")
            ?.optJSONArray("contents") ?: return@withContext emptyList()

        val results = mutableListOf<MusicItem>()
        for (i in 0 until sections.length()) {
            val s = sections.optJSONObject(i) ?: continue
            val shelf = s.optJSONObject("musicShelfRenderer")
            val contents = shelf?.optJSONArray("contents") ?: continue
            for (j in 0 until contents.length()) {
                val itemObj = contents.optJSONObject(j) ?: continue
                parseResponsiveItem(itemObj, categoryHint = filter)?.let { results.add(it) }
            }
        }
        results
    }

    private fun extractContinuationToken(continuations: JSONArray?): String? {
        if (continuations == null || continuations.length() == 0) return null
        val first = continuations.optJSONObject(0) ?: return null
        return first.optJSONObject("nextContinuationData")?.optString("continuation")?.takeIf { it.isNotBlank() }
            ?: first.optJSONObject("reloadContinuationData")?.optString("continuation")?.takeIf { it.isNotBlank() }
    }

    private fun parseHomeSection(s: JSONObject): HomeSection? {
        val carousel = s.optJSONObject("musicCarouselShelfRenderer")
        val shelf = s.optJSONObject("musicShelfRenderer")
        val renderer = carousel ?: shelf ?: return null

        val headerRenderer = carousel?.optJSONObject("header")?.optJSONObject("musicCarouselShelfBasicHeaderRenderer")
            ?: shelf?.optJSONObject("header")?.optJSONObject("musicShelfBasicHeaderRenderer")

        val rawTitle = joinRunsText(headerRenderer?.optJSONObject("title")?.optJSONArray("runs"))
            ?: joinRunsText(shelf?.optJSONObject("title")?.optJSONArray("runs"))
            ?: return null

        val title = stripEmojis(rawTitle)
        if (title.isBlank()) return null

        val strapline = joinRunsText(headerRenderer?.optJSONObject("strapline")?.optJSONArray("runs"))
            ?.let { stripEmojis(it) }
            ?.takeIf { it.isNotBlank() }

        val contents = renderer.optJSONArray("contents") ?: return null
        val parsedItems = mutableListOf<MusicItem>()

        for (i in 0 until contents.length()) {
            val it = contents.optJSONObject(i) ?: continue
            val twoRow = it.optJSONObject("musicTwoRowItemRenderer")
            if (twoRow != null) {
                val itTitle = stripEmojis(joinRunsText(twoRow.optJSONObject("title")?.optJSONArray("runs")).orEmpty())
                val itSubtitle = stripEmojis(joinRunsText(twoRow.optJSONObject("subtitle")?.optJSONArray("runs")).orEmpty())
                val bId = twoRow.optJSONObject("navigationEndpoint")
                    ?.optJSONObject("browseEndpoint")
                    ?.optString("browseId")
                    ?.takeIf { it.isNotBlank() }

                val overlayPlayBtn = twoRow.optJSONObject("thumbnailOverlay")
                    ?.optJSONObject("musicItemThumbnailOverlayRenderer")
                    ?.optJSONObject("content")
                    ?.optJSONObject("musicPlayButtonRenderer")
                    ?.optJSONObject("playNavigationEndpoint")

                val vId = twoRow.optJSONObject("navigationEndpoint")
                    ?.optJSONObject("watchEndpoint")
                    ?.optString("videoId")
                    ?.takeIf { it.isNotBlank() }
                    ?: overlayPlayBtn?.optJSONObject("watchEndpoint")?.optString("videoId")?.takeIf { it.isNotBlank() }

                val playlistId = twoRow.optJSONObject("navigationEndpoint")
                    ?.optJSONObject("watchPlaylistEndpoint")
                    ?.optString("playlistId")
                    ?.takeIf { it.isNotBlank() }
                    ?: overlayPlayBtn?.optJSONObject("watchPlaylistEndpoint")?.optString("playlistId")?.takeIf { it.isNotBlank() }

                val thumbs = twoRow.optJSONObject("thumbnailRenderer")
                    ?.optJSONObject("musicThumbnailRenderer")
                    ?.optJSONObject("thumbnail")
                    ?.optJSONArray("thumbnails")

                val rawThumb = extractLastThumbnailUrl(thumbs)
                val itemType = when {
                    bId?.startsWith("MPREb_") == true -> "album"
                    bId?.startsWith("UC") == true -> "artist"
                    !vId.isNullOrBlank() -> "song"
                    else -> "playlist"
                }

                val effectiveBrowseId = bId ?: playlistId?.let { "VL${it.removePrefix("VL")}" }
                val itemId = vId ?: effectiveBrowseId ?: "${title}_$i"

                if (itTitle.isNotBlank()) {
                    parsedItems.add(
                        MusicItem(
                            id = itemId,
                            title = itTitle,
                            subtitle = itSubtitle,
                            thumbnailUrl = MusicItem.upgradeThumbnailUrl(rawThumb, vId),
                            videoId = vId,
                            browseId = effectiveBrowseId,
                            playlistId = playlistId,
                            type = itemType
                        )
                    )
                }
            } else if (it.has("musicMultiRowListItemRenderer")) {
                parseMultiRowItem(it)?.let { multiItem -> parsedItems.add(multiItem) }
            } else if (it.has("musicResponsiveListItemRenderer")) {
                parseResponsiveItem(it)?.let { track -> parsedItems.add(track) }
            }
        }

        if (parsedItems.isEmpty()) return null

        val lowTitle = title.lowercase()
        val inferredStyle = when {
            lowTitle.contains("rekomendasi harian") || lowTitle.contains("daily discover") -> HomeSectionStyle.DAILY_RECOMMENDATION
            lowTitle.contains("pilihan cepat") || lowTitle.contains("cover dan remix") || lowTitle.contains("lagu trending") -> HomeSectionStyle.FOUR_ROW_SHELF
            lowTitle.contains("komunitas") || lowTitle.contains("community") -> HomeSectionStyle.COMMUNITY_PLAYLIST_CARDS
            else -> HomeSectionStyle.STANDARD_CAROUSEL
        }

        return HomeSection(
            id = "sec_${title.lowercase().replace(Regex("[^a-z0-9]+"), "_")}",
            title = title,
            strapline = strapline,
            items = parsedItems,
            style = inferredStyle
        )
    }

    /**
     * Port of `parseMultiRowItem(item)` from the user's updated YouTube Music scraper
     * for Category #6 "Rekomendasi harian Anda" (`musicMultiRowListItemRenderer`).
     */
    private fun parseMultiRowItem(item: JSONObject): MusicItem? {
        val mr = item.optJSONObject("musicMultiRowListItemRenderer") ?: return null
        val title = stripEmojis(joinRunsText(mr.optJSONObject("title")?.optJSONArray("runs")).orEmpty())
        if (title.isBlank()) return null

        val subtitle = stripEmojis(joinRunsText(mr.optJSONObject("subtitle")?.optJSONArray("runs")).orEmpty())
        val secondTitle = stripEmojis(joinRunsText(mr.optJSONObject("secondTitle")?.optJSONArray("runs")).orEmpty())
        val description = stripEmojis(joinRunsText(mr.optJSONObject("description")?.optJSONArray("runs")).orEmpty())
        val reasonText = description.ifBlank { secondTitle }.takeIf { it.isNotBlank() }

        val videoId = mr.optJSONObject("onTap")?.optJSONObject("watchEndpoint")?.optString("videoId")?.takeIf { it.isNotBlank() }
            ?: mr.optJSONObject("overlay")
                ?.optJSONObject("musicItemThumbnailOverlayRenderer")
                ?.optJSONObject("content")
                ?.optJSONObject("musicPlayButtonRenderer")
                ?.optJSONObject("playNavigationEndpoint")
                ?.optJSONObject("watchEndpoint")
                ?.optString("videoId")
                ?.takeIf { it.isNotBlank() }
            ?: mr.optJSONObject("title")
                ?.optJSONArray("runs")
                ?.optJSONObject(0)
                ?.optJSONObject("navigationEndpoint")
                ?.optJSONObject("watchEndpoint")
                ?.optString("videoId")
                ?.takeIf { it.isNotBlank() }

        val thumbs = mr.optJSONObject("thumbnail")
            ?.optJSONObject("musicThumbnailRenderer")
            ?.optJSONObject("thumbnail")
            ?.optJSONArray("thumbnails")

        val rawThumb = extractLastThumbnailUrl(thumbs)
        val effectiveId = videoId ?: "daily_${title.lowercase().replace(Regex("[^a-z0-9]+"), "_")}"

        return MusicItem(
            id = effectiveId,
            title = title,
            subtitle = subtitle.ifBlank { "Rekomendasi Harian" },
            thumbnailUrl = MusicItem.upgradeThumbnailUrl(rawThumb, videoId, MusicItem.Quality.HIGH),
            videoId = videoId,
            type = "daily_recommendation_track",
            reason = reasonText
        )
    }

    private fun parseResponsiveItem(item: JSONObject, categoryHint: String? = null): MusicItem? {
        val ir = item.optJSONObject("musicResponsiveListItemRenderer") ?: return null
        val flexColumns = ir.optJSONArray("flexColumns")

        val col0Runs = flexColumns?.optJSONObject(0)
            ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
            ?.optJSONObject("text")
            ?.optJSONArray("runs")

        val col1Runs = flexColumns?.optJSONObject(1)
            ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
            ?.optJSONObject("text")
            ?.optJSONArray("runs")

        val col2Runs = flexColumns?.optJSONObject(2)
            ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
            ?.optJSONObject("text")
            ?.optJSONArray("runs")

        val title = stripEmojis(col0Runs?.optJSONObject(0)?.optString("text").orEmpty())
        if (title.isBlank()) return null

        val col1Text = stripEmojis(joinRunsText(col1Runs).orEmpty())
        val col2Text = stripEmojis(joinRunsText(col2Runs).orEmpty())
        val fullSubtitle = if (col2Text.isNotBlank() && !col1Text.contains(col2Text)) {
            "$col1Text • $col2Text"
        } else {
            col1Text
        }

        val duration = ir.optJSONArray("fixedColumns")
            ?.optJSONObject(0)
            ?.optJSONObject("musicResponsiveListItemFixedColumnRenderer")
            ?.optJSONObject("text")
            ?.optJSONArray("runs")
            ?.optJSONObject(0)
            ?.optString("text")
            ?.takeIf { it.isNotBlank() }

        val videoId = ir.optJSONObject("playlistItemData")?.optString("videoId")?.takeIf { it.isNotBlank() }
            ?: ir.optJSONObject("overlay")
                ?.optJSONObject("musicItemThumbnailOverlayRenderer")
                ?.optJSONObject("content")
                ?.optJSONObject("musicPlayButtonRenderer")
                ?.optJSONObject("playNavigationEndpoint")
                ?.optJSONObject("watchEndpoint")
                ?.optString("videoId")
                ?.takeIf { it.isNotBlank() }
            ?: ir.optJSONObject("navigationEndpoint")
                ?.optJSONObject("watchEndpoint")
                ?.optString("videoId")
                ?.takeIf { it.isNotBlank() }
            ?: col0Runs?.optJSONObject(0)
                ?.optJSONObject("navigationEndpoint")
                ?.optJSONObject("watchEndpoint")
                ?.optString("videoId")
                ?.takeIf { it.isNotBlank() }

        val browseId = col0Runs?.optJSONObject(0)
            ?.optJSONObject("navigationEndpoint")
            ?.optJSONObject("browseEndpoint")
            ?.optString("browseId")
            ?.takeIf { it.isNotBlank() }
            ?: ir.optJSONObject("navigationEndpoint")
                ?.optJSONObject("browseEndpoint")
                ?.optString("browseId")
                ?.takeIf { it.isNotBlank() }

        val rawThumb = extractBestThumbnailUrl(ir)
        val firstCol1 = col1Runs?.optJSONObject(0)?.optString("text").orEmpty().lowercase()
        val lowHint = categoryHint?.lowercase().orEmpty()

        val itemType = when {
            lowHint.contains("artist") || firstCol1.contains("artist") || firstCol1.contains("artis") || browseId?.startsWith("UC") == true -> "artist"
            lowHint.contains("playlist") || firstCol1.contains("playlist") || browseId?.startsWith("VL") == true || browseId?.startsWith("PL") == true -> "playlist"
            lowHint.contains("album") || firstCol1.contains("album") || firstCol1.contains("single") || firstCol1.contains("ep") || browseId?.startsWith("MPREb_") == true -> "album"
            lowHint.contains("video") || firstCol1.contains("video") -> "video"
            firstCol1.contains("song") || firstCol1.contains("lagu") -> "song"
            !videoId.isNullOrBlank() -> "song"
            else -> "song"
        }

        val isCollectionType = itemType == "playlist" || itemType == "album" || itemType == "artist"
        val displaySubtitle = if (itemType == "artist") {
            fullSubtitle.ifBlank { "Artist" }
        } else if (isCollectionType) {
            fullSubtitle.ifBlank { itemType.replaceFirstChar { it.uppercase() } }
        } else {
            val cleaned = MusicItem.sanitizeArtistName(col1Text, isCollection = false)
            val durSuffix = duration?.let { " • $it" }.orEmpty()
            if (col1Text.contains("•")) col1Text else "$cleaned$durSuffix"
        }

        val normalizedBrowseId = extractBrowseId(browseId)
        val effectiveId = videoId ?: normalizedBrowseId ?: "${title}_${displaySubtitle}"
        val finalThumb = MusicItem.upgradeThumbnailUrl(rawThumb, videoId)

        if (itemType == "artist" && finalThumb.isNotBlank()) {
            artistAvatarCache[title.trim().lowercase()] = finalThumb
        }

        return MusicItem(
            id = effectiveId,
            title = title,
            subtitle = displaySubtitle,
            thumbnailUrl = finalThumb,
            videoId = videoId,
            browseId = normalizedBrowseId,
            type = itemType,
            duration = duration
        )
    }

    suspend fun getAudioStream(item: MusicItem): AudioStreamInfo? = withContext(Dispatchers.IO) {
        val targetVideoId = resolveTrueVideoId(item)

        if (!targetVideoId.isNullOrBlank()) {
            fetchStreamForVideoId(targetVideoId)?.let { return@withContext it }
        }

        try {
            val cleanTitle = item.title.replace("...", "").trim()
            val query = "$cleanTitle ${item.cleanArtist}".trim()
            val candidates = searchByFilter(query, filter = "songs")
                .mapNotNull { it.videoId }
                .filter { it != targetVideoId }
                .take(3)

            for (altId in candidates) {
                fetchStreamForVideoId(altId)?.let { return@withContext it }
            }
        } catch (_: Exception) {
        }

        null
    }

    private suspend fun fetchStreamForVideoId(videoId: String): AudioStreamInfo? = withContext(Dispatchers.IO) {
        val cfg = getInnertubeConfig()

        try {
            val bodyJson = JSONObject().apply {
                put(
                    "context",
                    JSONObject().put(
                        "client",
                        JSONObject().apply {
                            put("clientName", "VISIONOS")
                            put("clientVersion", "1.02")
                            put("deviceMake", "Apple")
                            put("deviceModel", "RealityDevice17,1")
                            put("osName", "visionOS")
                            put("osVersion", "26.5.23O471")
                            if (!cfg.visitorData.isNullOrBlank()) {
                                put("visitorData", cfg.visitorData)
                            }
                            put("hl", "en")
                            put("gl", "US")
                        }
                    )
                )
                put("videoId", videoId)
            }

            val req = Request.Builder()
                .url("https://www.youtube.com/youtubei/v1/player?prettyPrint=false")
                .header("Content-Type", "application/json")
                .header(
                    "User-Agent",
                    "Mozilla/5.0 (Macintosh; Intel Mac OS X 15_7_3) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/26.0 Safari/605.1.15"
                )
                .post(bodyJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            httpClient.newCall(req).execute().use { res ->
                if (res.isSuccessful) {
                    val data = JSONObject(res.body?.string().orEmpty())
                    parseAdaptiveFormats(videoId, data)?.let { return@withContext it }
                }
            }
        } catch (_: Exception) {
        }

        try {
            val bodyJson = JSONObject().apply {
                put(
                    "context",
                    JSONObject().put(
                        "client",
                        JSONObject().apply {
                            put("clientName", "ANDROID_VR")
                            put("clientVersion", "1.60.19")
                            put("deviceMake", "Oculus")
                            put("deviceModel", "Quest 3")
                            put("osName", "Android")
                            put("osVersion", "12L")
                            put("androidSdkVersion", 32)
                            put("hl", "en")
                            put("gl", "US")
                        }
                    )
                )
                put("videoId", videoId)
            }

            val req = Request.Builder()
                .url("https://www.youtube.com/youtubei/v1/player?prettyPrint=false")
                .header("Content-Type", "application/json")
                .header("User-Agent", "com.google.android.apps.youtube.vr.oculus/1.60.19 (Linux; U; Android 12L; eureka-user Build/SQ3A.220605.009.A1) gzip")
                .post(bodyJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            httpClient.newCall(req).execute().use { res ->
                if (res.isSuccessful) {
                    val data = JSONObject(res.body?.string().orEmpty())
                    parseAdaptiveFormats(videoId, data)?.let { return@withContext it }
                }
            }
        } catch (_: Exception) {
        }

        null
    }

    private fun parseAdaptiveFormats(videoId: String, data: JSONObject): AudioStreamInfo? {
        val status = data.optJSONObject("playabilityStatus")?.optString("status")
        if (status != "OK") return null

        val videoDetails = data.optJSONObject("videoDetails")
        val durationSec = videoDetails?.optString("lengthSeconds")?.toIntOrNull() ?: 0
        val thumbsArr = videoDetails?.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
        var hdPlayerThumb: String? = null
        if (thumbsArr != null && thumbsArr.length() > 0) {
            val bestThumbObj = thumbsArr.optJSONObject(thumbsArr.length() - 1)
            val rawUrl = bestThumbObj?.optString("url")?.takeIf { it.isNotBlank() }
            if (!rawUrl.isNullOrBlank()) {
                hdPlayerThumb = MusicItem.upgradeThumbnailUrl(rawUrl, videoId, MusicItem.Quality.ULTRA)
            }
        }
        val afs = data.optJSONObject("streamingData")?.optJSONArray("adaptiveFormats") ?: return null

        var m4aTrack: JSONObject? = null
        var webmTrack: JSONObject? = null
        var anyAudioTrack: JSONObject? = null

        for (i in 0 until afs.length()) {
            val f = afs.optJSONObject(i) ?: continue
            val url = f.optString("url")
            if (url.isBlank()) continue
            val itag = f.optInt("itag")
            val mime = f.optString("mimeType")
            if (itag == 140) m4aTrack = f
            else if (itag == 251) webmTrack = f
            else if (mime.contains("audio") && anyAudioTrack == null) anyAudioTrack = f
        }

        val best = m4aTrack ?: webmTrack ?: anyAudioTrack ?: return null
        val rawUrl = best.optString("url")
        val contentLength = best.optString("contentLength").toLongOrNull()
        val finalUrl = if (contentLength != null && contentLength > 0) {
            "$rawUrl&range=0-${contentLength - 1}"
        } else {
            rawUrl
        }
        val mimeType = best.optString("mimeType").substringBefore(";")

        return AudioStreamInfo(
            videoId = videoId,
            url = finalUrl,
            mimeType = mimeType,
            format = if (mimeType.contains("mp4")) "m4a (aac)" else "webm (opus)",
            bitrate = best.optInt("bitrate", 128000),
            durationSeconds = durationSec,
            thumbnailUrl = hdPlayerThumb
        )
    }

    /**
     * Resolves a direct muxed MP4 video stream URL (itag 18 / 22) for YouTube Music Video mode in the player.
     */
    suspend fun getVideoStreamUrl(item: MusicItem): String? = withContext(Dispatchers.IO) {
        val targetVideoId = resolveTrueVideoId(item) ?: return@withContext null
        val cfg = getInnertubeConfig()

        try {
            val bodyJson = JSONObject().apply {
                put(
                    "context",
                    JSONObject().put(
                        "client",
                        JSONObject().apply {
                            put("clientName", "ANDROID_VR")
                            put("clientVersion", "1.60.19")
                            put("deviceMake", "Oculus")
                            put("deviceModel", "Quest 3")
                            put("osName", "Android")
                            put("osVersion", "12L")
                            put("androidSdkVersion", 32)
                            if (!cfg.visitorData.isNullOrBlank()) {
                                put("visitorData", cfg.visitorData)
                            }
                            put("hl", "en")
                            put("gl", "US")
                        }
                    )
                )
                put("videoId", targetVideoId)
            }

            val req = Request.Builder()
                .url("https://www.youtube.com/youtubei/v1/player?prettyPrint=false")
                .header("Content-Type", "application/json")
                .header("User-Agent", "com.google.android.apps.youtube.vr.oculus/1.60.19 (Linux; U; Android 12L; eureka-user Build/SQ3A.220605.009.A1) gzip")
                .post(bodyJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            httpClient.newCall(req).execute().use { res ->
                if (res.isSuccessful) {
                    val data = JSONObject(res.body?.string().orEmpty())
                    val formats = data.optJSONObject("streamingData")?.optJSONArray("formats")
                    if (formats != null) {
                        for (i in 0 until formats.length()) {
                            val f = formats.optJSONObject(i) ?: continue
                            val url = f.optString("url")
                            val mime = f.optString("mimeType")
                            if (url.isNotBlank() && mime.contains("video/mp4")) {
                                return@withContext url
                            }
                        }
                    }
                    val adaptive = data.optJSONObject("streamingData")?.optJSONArray("adaptiveFormats")
                    if (adaptive != null) {
                        for (i in 0 until adaptive.length()) {
                            val f = adaptive.optJSONObject(i) ?: continue
                            val url = f.optString("url")
                            val mime = f.optString("mimeType")
                            if (url.isNotBlank() && mime.contains("video/mp4")) {
                                return@withContext url
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {
        }
        null
    }

    private fun joinRunsText(runs: JSONArray?): String? {
        if (runs == null || runs.length() == 0) return null
        val sb = StringBuilder()
        for (i in 0 until runs.length()) {
            sb.append(runs.optJSONObject(i)?.optString("text").orEmpty())
        }
        return sb.toString()
    }

    private fun extractLastThumbnailUrl(thumbnails: JSONArray?): String? {
        if (thumbnails == null || thumbnails.length() == 0) return null
        for (idx in thumbnails.length() - 1 downTo 0) {
            val raw = thumbnails.optJSONObject(idx)?.optString("url")?.trim().orEmpty()
            if (raw.isNotBlank()) {
                return if (raw.startsWith("//")) "https:$raw" else raw
            }
        }
        return null
    }

    /**
     * Universal thumbnail extractor aligned with `extractThumbnail` in `ytm.mjs`.
     * Inspects all possible nested InnerTube thumbnail renderer structures.
     */
    private fun extractBestThumbnailUrl(node: JSONObject?): String? {
        if (node == null) return null
        val candidates = listOfNotNull(
            node.optJSONObject("thumbnail")
                ?.optJSONObject("musicThumbnailRenderer")
                ?.optJSONObject("thumbnail")
                ?.optJSONArray("thumbnails"),
            node.optJSONObject("thumbnailRenderer")
                ?.optJSONObject("musicThumbnailRenderer")
                ?.optJSONObject("thumbnail")
                ?.optJSONArray("thumbnails"),
            node.optJSONObject("thumbnail")
                ?.optJSONObject("croppedSquareThumbnailRenderer")
                ?.optJSONObject("thumbnail")
                ?.optJSONArray("thumbnails"),
            node.optJSONObject("thumbnailRenderer")
                ?.optJSONObject("croppedSquareThumbnailRenderer")
                ?.optJSONObject("thumbnail")
                ?.optJSONArray("thumbnails"),
            node.optJSONObject("foregroundThumbnail")
                ?.optJSONObject("musicThumbnailRenderer")
                ?.optJSONObject("thumbnail")
                ?.optJSONArray("thumbnails"),
            node.optJSONObject("thumbnail")?.optJSONArray("thumbnails"),
            node.optJSONArray("thumbnails")
        )
        for (arr in candidates) {
            val found = extractLastThumbnailUrl(arr)
            if (!found.isNullOrBlank()) return found
        }
        return null
    }

    /**
     * Resolves an artist's real circular profile photo from YouTube Music's `Artists` search filter
     * (matching the exact image shown in the Artists category of Search and on the Artist Profile page).
     */
    suspend fun fetchArtistAvatarUrl(
        artistName: String,
        browseId: String? = null,
        hl: String = "id",
        gl: String = "ID"
    ): String? = withContext(Dispatchers.IO) {
        val cleanName = MusicItem.sanitizeArtistName(artistName, isCollection = false)
            .substringBefore(",")
            .substringBefore("&")
            .trim()
        if (cleanName.isBlank() || cleanName.equals("Artis", ignoreCase = true)) return@withContext null

        val key = cleanName.lowercase()
        artistAvatarCache[key]?.takeIf { it.isNotBlank() }?.let { return@withContext it }

        try {
            val results = searchByFilter(cleanName, filter = "artists", hl = hl, gl = gl)
            val bestMatch = results.firstOrNull {
                it.title.equals(cleanName, ignoreCase = true) && it.thumbnailUrl.isNotBlank()
            } ?: results.firstOrNull { it.thumbnailUrl.isNotBlank() }

            val resolvedUrl = bestMatch?.thumbnailUrl?.takeIf { it.isNotBlank() }
            if (!resolvedUrl.isNullOrBlank()) {
                artistAvatarCache[key] = resolvedUrl
                return@withContext resolvedUrl
            }
        } catch (_: Exception) {
        }

        if (!browseId.isNullOrBlank() && browseId.startsWith("UC") && !browseId.startsWith("UC_")) {
            try {
                val browseRes = apiRequest("browse", payload = JSONObject().put("browseId", browseId), hl = hl, gl = gl)
                val headerObj = browseRes.optJSONObject("header")?.optJSONObject("musicImmersiveHeaderRenderer")
                    ?: browseRes.optJSONObject("header")?.optJSONObject("musicVisualHeaderRenderer")
                val headerUrl = extractBestThumbnailUrl(headerObj)?.let {
                    MusicItem.upgradeThumbnailUrl(it, null, MusicItem.Quality.HIGH)
                }?.takeIf { it.isNotBlank() }
                if (!headerUrl.isNullOrBlank()) {
                    artistAvatarCache[key] = headerUrl
                    return@withContext headerUrl
                }
            } catch (_: Exception) {
            }
        }

        try {
            val songs = searchByFilter(cleanName, filter = "songs", hl = hl, gl = gl)
            val songThumb = songs.firstOrNull { it.thumbnailUrl.isNotBlank() }?.highResThumbnail
            if (!songThumb.isNullOrBlank()) {
                artistAvatarCache[key] = songThumb
                return@withContext songThumb
            }
        } catch (_: Exception) {
        }
        null
    }

    /**
     * Fetches a full Artist Profile from YouTube Music Innertube (`browse` channel + categorized searches)
     * including hero banner/avatar, subscriber count, top songs, latest release highlight, albums,
     * singles/EPs, videos, featured playlists, and circular "Penggemar mungkin juga suka" related artists.
     */
    suspend fun getArtistProfile(
        seedItem: MusicItem,
        hl: String = "id",
        gl: String = "ID"
    ): ArtistProfileDetail = withContext(Dispatchers.IO) {
        val rawName = if (seedItem.type == "artist" || seedItem.browseId?.startsWith("UC") == true) {
            seedItem.title
        } else {
            seedItem.cleanArtist.ifBlank { seedItem.title }
        }
        val cleanArtistName = MusicItem.sanitizeArtistName(rawName, isCollection = false)
            .substringBefore(",")
            .substringBefore("&")
            .trim()
            .ifBlank { seedItem.title }

        coroutineScope {
            val artistsDeferred = async {
                try {
                    searchByFilter(cleanArtistName, filter = "artists", hl = hl, gl = gl)
                } catch (_: Exception) {
                    emptyList()
                }
            }
            val songsDeferred = async {
                try {
                    searchByFilter(cleanArtistName, filter = "songs", hl = hl, gl = gl)
                } catch (_: Exception) {
                    emptyList()
                }
            }
            val albumsDeferred = async {
                try {
                    searchByFilter(cleanArtistName, filter = "albums", hl = hl, gl = gl)
                } catch (_: Exception) {
                    emptyList()
                }
            }
            val videosDeferred = async {
                try {
                    searchByFilter("$cleanArtistName official music video", filter = "videos", hl = hl, gl = gl)
                } catch (_: Exception) {
                    emptyList()
                }
            }
            val playlistsDeferred = async {
                try {
                    searchByFilter(cleanArtistName, filter = "playlists", hl = hl, gl = gl)
                } catch (_: Exception) {
                    emptyList()
                }
            }

            val searchedArtists = artistsDeferred.await()
            val primaryArtistMatch = searchedArtists.firstOrNull {
                it.title.equals(cleanArtistName, ignoreCase = true)
            } ?: searchedArtists.firstOrNull()

            val resolvedBrowseId = seedItem.browseId?.takeIf { it.startsWith("UC") && !it.startsWith("UC_") }
                ?: primaryArtistMatch?.browseId?.takeIf { it.startsWith("UC") && !it.startsWith("UC_") }
                ?: "UC_${cleanArtistName.lowercase().replace(Regex("[^a-z0-9]+"), "_")}"

            var headerThumb: String? = null
            var subscribersText: String? = null
            var descriptionText: String? = null
            val channelTopSongs = mutableListOf<MusicItem>()
            val channelRelatedArtists = mutableListOf<MusicItem>()
            val channelAlbums = mutableListOf<MusicItem>()
            val channelSingles = mutableListOf<MusicItem>()
            val channelVideos = mutableListOf<MusicItem>()
            val channelFeaturedPlaylists = mutableListOf<MusicItem>()

            if (resolvedBrowseId.startsWith("UC") && !resolvedBrowseId.startsWith("UC_")) {
                try {
                    val browseRes = apiRequest("browse", payload = JSONObject().put("browseId", resolvedBrowseId), hl = hl, gl = gl)
                    val headerObj = browseRes.optJSONObject("header")?.optJSONObject("musicImmersiveHeaderRenderer")
                        ?: browseRes.optJSONObject("header")?.optJSONObject("musicVisualHeaderRenderer")

                    if (headerObj != null) {
                        headerThumb = extractBestThumbnailUrl(headerObj)
                        subscribersText = joinRunsText(
                            headerObj.optJSONObject("subscriptionButton")
                                ?.optJSONObject("subscribeButtonRenderer")
                                ?.optJSONObject("subscriberCountText")
                                ?.optJSONArray("runs")
                        )
                        descriptionText = joinRunsText(headerObj.optJSONObject("description")?.optJSONArray("runs"))
                    }

                    val sectionContents = browseRes.optJSONObject("contents")
                        ?.optJSONObject("singleColumnBrowseResultsRenderer")
                        ?.optJSONArray("tabs")
                        ?.optJSONObject(0)
                        ?.optJSONObject("tabRenderer")
                        ?.optJSONObject("content")
                        ?.optJSONObject("sectionListRenderer")
                        ?.optJSONArray("contents")

                    if (sectionContents != null) {
                        for (i in 0 until sectionContents.length()) {
                            val secObj = sectionContents.optJSONObject(i) ?: continue
                            val descShelf = secObj.optJSONObject("musicDescriptionShelfRenderer")
                            if (descShelf != null && descriptionText.isNullOrBlank()) {
                                descriptionText = joinRunsText(descShelf.optJSONObject("description")?.optJSONArray("runs"))
                            }

                            val musicShelf = secObj.optJSONObject("musicShelfRenderer")
                            if (musicShelf != null) {
                                val shelfItems = musicShelf.optJSONArray("contents")
                                if (shelfItems != null) {
                                    for (j in 0 until shelfItems.length()) {
                                        val itemObj = shelfItems.optJSONObject(j) ?: continue
                                        parseResponsiveItem(itemObj, categoryHint = "songs")?.let { parsedSong ->
                                            if (!parsedSong.videoId.isNullOrBlank()) {
                                                channelTopSongs.add(parsedSong)
                                            }
                                        }
                                    }
                                }
                            }

                            val carousel = secObj.optJSONObject("musicCarouselShelfRenderer") ?: continue
                            val shelfTitle = joinRunsText(
                                carousel.optJSONObject("header")
                                    ?.optJSONObject("musicCarouselShelfBasicHeaderRenderer")
                                    ?.optJSONObject("title")
                                    ?.optJSONArray("runs")
                            ).orEmpty().lowercase()

                            val contentsArr = carousel.optJSONArray("contents") ?: continue
                            for (j in 0 until contentsArr.length()) {
                                val itemWrapper = contentsArr.optJSONObject(j) ?: continue
                                val twoRow = itemWrapper.optJSONObject("musicTwoRowItemRenderer")
                                if (twoRow == null) {
                                    if (shelfTitle.contains("video")) {
                                        parseResponsiveItem(itemWrapper, categoryHint = "videos")?.let { channelVideos.add(it) }
                                    }
                                    continue
                                }
                                val itemTitle = stripEmojis(joinRunsText(twoRow.optJSONObject("title")?.optJSONArray("runs")).orEmpty())
                                if (itemTitle.isBlank()) continue
                                val itemSub = stripEmojis(joinRunsText(twoRow.optJSONObject("subtitle")?.optJSONArray("runs")).orEmpty())
                                val itemBrowseId = twoRow.optJSONObject("navigationEndpoint")
                                    ?.optJSONObject("browseEndpoint")
                                    ?.optString("browseId")
                                    ?.takeIf { it.isNotBlank() }

                                val overlayPlayEndpoint = twoRow.optJSONObject("thumbnailOverlay")
                                    ?.optJSONObject("musicItemThumbnailOverlayRenderer")
                                    ?.optJSONObject("content")
                                    ?.optJSONObject("musicPlayButtonRenderer")
                                    ?.optJSONObject("playNavigationEndpoint")

                                val itemVideoId = twoRow.optJSONObject("navigationEndpoint")
                                    ?.optJSONObject("watchEndpoint")
                                    ?.optString("videoId")
                                    ?.takeIf { it.isNotBlank() }
                                    ?: overlayPlayEndpoint?.optJSONObject("watchEndpoint")?.optString("videoId")?.takeIf { it.isNotBlank() }

                                val itemPlaylistId = twoRow.optJSONObject("navigationEndpoint")
                                    ?.optJSONObject("watchPlaylistEndpoint")
                                    ?.optString("playlistId")
                                    ?.takeIf { it.isNotBlank() }
                                    ?: overlayPlayEndpoint?.optJSONObject("watchPlaylistEndpoint")?.optString("playlistId")?.takeIf { it.isNotBlank() }

                                val rawItemThumb = extractBestThumbnailUrl(twoRow)
                                val upgradedThumb = MusicItem.upgradeThumbnailUrl(rawItemThumb, itemVideoId, MusicItem.Quality.HIGH)

                                when {
                                    shelfTitle.contains("video") || (!itemVideoId.isNullOrBlank() && itemBrowseId == null) -> {
                                        val vid = itemVideoId ?: continue
                                        val vidThumb = upgradedThumb.ifBlank { "https://i.ytimg.com/vi/$vid/hqdefault.jpg" }
                                        channelVideos.add(
                                            MusicItem(
                                                id = vid,
                                                title = itemTitle,
                                                subtitle = itemSub.ifBlank { cleanArtistName },
                                                thumbnailUrl = vidThumb,
                                                videoId = vid,
                                                type = "video"
                                            )
                                        )
                                    }
                                    shelfTitle.contains("penggemar") || shelfTitle.contains("fans") || shelfTitle.contains("mirip") || shelfTitle.contains("like") || itemBrowseId?.startsWith("UC") == true -> {
                                        if (upgradedThumb.isNotBlank()) {
                                            artistAvatarCache[itemTitle.trim().lowercase()] = upgradedThumb
                                        }
                                        channelRelatedArtists.add(
                                            MusicItem(
                                                id = itemBrowseId ?: "rel_${itemTitle.lowercase()}",
                                                title = itemTitle,
                                                subtitle = itemSub.ifBlank { "Artis" },
                                                thumbnailUrl = upgradedThumb,
                                                browseId = itemBrowseId,
                                                type = "artist"
                                            )
                                        )
                                    }
                                    shelfTitle.contains("single") || itemSub.contains("single", ignoreCase = true) || itemSub.contains("ep", ignoreCase = true) -> {
                                        channelSingles.add(
                                            MusicItem(
                                                id = itemBrowseId ?: "single_${itemTitle.lowercase()}",
                                                title = itemTitle,
                                                subtitle = itemSub.ifBlank { "Single • $cleanArtistName" },
                                                thumbnailUrl = upgradedThumb,
                                                browseId = itemBrowseId,
                                                type = "album"
                                            )
                                        )
                                    }
                                    shelfTitle.contains("album") || itemBrowseId?.startsWith("MPREb_") == true -> {
                                        channelAlbums.add(
                                            MusicItem(
                                                id = itemBrowseId ?: "album_${itemTitle.lowercase()}",
                                                title = itemTitle,
                                                subtitle = itemSub.ifBlank { "Album • $cleanArtistName" },
                                                thumbnailUrl = upgradedThumb,
                                                browseId = itemBrowseId,
                                                type = "album"
                                            )
                                        )
                                    }
                                    shelfTitle.contains("menampilkan") || shelfTitle.contains("featuring") || shelfTitle.contains("playlist") || itemBrowseId?.startsWith("VL") == true || !itemPlaylistId.isNullOrBlank() -> {
                                        val normBrowse = extractBrowseId(itemBrowseId ?: itemPlaylistId)
                                        channelFeaturedPlaylists.add(
                                            MusicItem(
                                                id = normBrowse ?: "feat_${itemTitle.lowercase()}",
                                                title = itemTitle,
                                                subtitle = itemSub.ifBlank { "Playlist • YouTube Music" },
                                                thumbnailUrl = upgradedThumb,
                                                browseId = normBrowse,
                                                playlistId = itemPlaylistId,
                                                type = "playlist"
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                } catch (_: Exception) {
                }
            }

            val fallbackProfile = CuratedHomeFallback.getArtistProfileFallback(cleanArtistName, seedItem)

            val topSongs = (channelTopSongs + songsDeferred.await().filter { !it.videoId.isNullOrBlank() })
                .distinctBy { it.videoId ?: it.id }
                .take(15)
                .ifEmpty { fallbackProfile.topSongs }

            val allSearchedAlbums = albumsDeferred.await()
            val mergedAlbums = (channelAlbums + allSearchedAlbums.filter {
                !it.subtitle.contains("single", ignoreCase = true) && !it.subtitle.contains("ep", ignoreCase = true)
            }).distinctBy { it.title.lowercase() }
                .ifEmpty { fallbackProfile.albums }

            val mergedSingles = (channelSingles + allSearchedAlbums.filter {
                it.subtitle.contains("single", ignoreCase = true) || it.subtitle.contains("ep", ignoreCase = true)
            }).distinctBy { it.title.lowercase() }
                .ifEmpty { fallbackProfile.singlesAndEps }

            // Prioritize the circular artist avatar from Search ("artists") or browse header over any song cover
            val bestAvatar = MusicItem.upgradeThumbnailUrl(
                primaryArtistMatch?.thumbnailUrl?.takeIf { it.isNotBlank() }
                    ?: headerThumb?.takeIf { it.isNotBlank() }
                    ?: artistAvatarCache[cleanArtistName.lowercase()]
                    ?: seedItem.thumbnailUrl.takeIf { it.isNotBlank() }
                    ?: topSongs.firstOrNull()?.thumbnailUrl
                    ?: fallbackProfile.avatarUrl,
                null,
                MusicItem.Quality.ULTRA
            )
            if (bestAvatar.isNotBlank()) {
                artistAvatarCache[cleanArtistName.lowercase()] = bestAvatar
                primaryArtistMatch?.title?.trim()?.lowercase()?.let { artistAvatarCache[it] = bestAvatar }
            }

            // Guarantee every video item has a valid YouTube video thumbnail & videoId!
            val combinedRawVideos = (channelVideos + videosDeferred.await())
                .distinctBy { (it.videoId ?: it.id).lowercase() }
                .take(10)
                .ifEmpty { fallbackProfile.videos }

            val videos = combinedRawVideos.mapIndexed { idx, v ->
                val vid = v.videoId?.takeIf {
                    it.isNotBlank() && !it.startsWith("UC") && !it.startsWith("VL") && !it.startsWith("MPRE")
                } ?: topSongs.getOrNull(idx % topSongs.size)?.videoId
                    ?: "7PCkvCPvDXk"

                val guaranteedThumb = MusicItem.upgradeThumbnailUrl(v.thumbnailUrl, vid, MusicItem.Quality.HIGH)
                    .ifBlank { "https://i.ytimg.com/vi/$vid/hqdefault.jpg" }

                v.copy(
                    id = vid,
                    videoId = vid,
                    thumbnailUrl = guaranteedThumb,
                    type = "video"
                )
            }

            // Guarantee every featured playlist ("Menampilkan [Artis]") has previewTracks & valid browseId
            val combinedRawPlaylists = (channelFeaturedPlaylists + playlistsDeferred.await())
                .distinctBy { (it.browseId ?: it.id).lowercase() }
                .take(10)
                .ifEmpty { fallbackProfile.featuredPlaylists }

            val featuredPlaylists = combinedRawPlaylists.mapIndexed { idx, pl ->
                val normBrowse = pl.browseId?.takeIf { it.isNotBlank() }?.let {
                    extractBrowseId(it) ?: it
                } ?: pl.id.ifBlank { "feat_pl_${idx}_${cleanArtistName.lowercase()}" }
                val plThumb = MusicItem.upgradeThumbnailUrl(pl.thumbnailUrl, null, MusicItem.Quality.HIGH).ifBlank {
                    topSongs.getOrNull(idx % topSongs.size)?.highResThumbnail ?: bestAvatar
                }
                pl.copy(
                    id = normBrowse,
                    browseId = normBrowse,
                    type = "playlist",
                    thumbnailUrl = plThumb,
                    previewTracks = pl.previewTracks.ifEmpty { topSongs }
                )
            }

            val otherSearchedArtists = searchedArtists
                .filterNot { it.title.equals(cleanArtistName, ignoreCase = true) }

            val rawRelatedArtists = (channelRelatedArtists + otherSearchedArtists + fallbackProfile.relatedArtists)
                .filterNot { it.title.equals(cleanArtistName, ignoreCase = true) }
                .distinctBy { it.title.trim().lowercase() }
                .take(10)

            // Ensure every related artist in "Penggemar mungkin juga suka" has a non-empty, valid profile image!
            val relatedArtists = rawRelatedArtists.map { relArtist ->
                async {
                    val existingThumb = relArtist.thumbnailUrl.trim()
                    if (existingThumb.isNotBlank()) {
                        artistAvatarCache[relArtist.title.trim().lowercase()] = existingThumb
                        relArtist
                    } else {
                        val fetchedAvatar = fetchArtistAvatarUrl(
                            artistName = relArtist.title,
                            browseId = relArtist.browseId,
                            hl = hl,
                            gl = gl
                        ) ?: CuratedHomeFallback.getArtistAvatarForName(relArtist.title)
                        relArtist.copy(thumbnailUrl = fetchedAvatar)
                    }
                }
            }.awaitAll()

            val subLabel = subscribersText?.takeIf { it.isNotBlank() }
                ?: primaryArtistMatch?.subtitle?.substringAfter("•", "")?.trim()?.takeIf { it.isNotBlank() }
                ?: fallbackProfile.subscribersText

            val latestRelease = mergedSingles.firstOrNull()
                ?: mergedAlbums.firstOrNull()
                ?: fallbackProfile.latestRelease

            ArtistProfileDetail(
                browseId = resolvedBrowseId,
                name = primaryArtistMatch?.title?.takeIf { it.isNotBlank() } ?: cleanArtistName,
                subscribersText = subLabel,
                monthlyListenersText = fallbackProfile.monthlyListenersText,
                description = descriptionText?.takeIf { it.isNotBlank() } ?: fallbackProfile.description,
                headerImageUrl = headerThumb?.let { MusicItem.upgradeThumbnailUrl(it, null, MusicItem.Quality.ULTRA) }?.takeIf { it.isNotBlank() } ?: bestAvatar,
                avatarUrl = bestAvatar,
                latestRelease = latestRelease,
                topSongs = topSongs,
                albums = mergedAlbums,
                singlesAndEps = mergedSingles,
                videos = videos,
                featuredPlaylists = featuredPlaylists,
                relatedArtists = relatedArtists
            )
        }
    }
}
