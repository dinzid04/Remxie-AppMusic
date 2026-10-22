package com.example.data.model

import androidx.compose.runtime.Immutable

@Immutable
data class MusicItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val thumbnailUrl: String,
    val videoId: String? = null,
    val browseId: String? = null,
    val playlistId: String? = null,
    val type: String = "song",
    val duration: String? = null,
    val reason: String? = null,
    val playsText: String? = null,
    val previewTracks: List<MusicItem> = emptyList(),
    val viewsAndCountText: String? = null,
    val releaseYear: String? = null,
    val sourcePlatformBadge: String? = null,
    val localFilePath: String? = null,
    val playCount: Int = 0
) {
    val isCollection: Boolean
        get() = type == "playlist" || type == "album" || type == "artist" ||
            (!browseId.isNullOrBlank() && videoId.isNullOrBlank())

    val isArtist: Boolean
        get() = type == "artist" || browseId?.startsWith("UC") == true

    val cleanArtist: String
        get() = sanitizeArtistName(subtitle, isCollection)

    val artistWithPlays: String
        get() {
            val baseArtist = cleanArtist
            val explicitPlays = playsText?.takeIf { it.isNotBlank() }
            if (explicitPlays != null) {
                return if (baseArtist.contains("rb", ignoreCase = true) || baseArtist.contains("jt", ignoreCase = true)) {
                    baseArtist
                } else {
                    "$baseArtist • $explicitPlays"
                }
            }
            val segments = subtitle.split("•").map { it.trim() }.filter { it.isNotBlank() }
            val playSegment = segments.firstOrNull {
                it.contains("diputar", ignoreCase = true) ||
                    it.contains("ditonton", ignoreCase = true) ||
                    it.contains("views", ignoreCase = true) ||
                    it.contains("plays", ignoreCase = true) ||
                    it.contains("jt ", ignoreCase = true) ||
                    it.contains("rb ", ignoreCase = true)
            }
            return if (playSegment != null && !baseArtist.contains(playSegment, ignoreCase = true)) {
                "$baseArtist • $playSegment"
            } else {
                baseArtist
            }
        }

    val artistWithDuration: String
        get() {
            val base = cleanArtist
            val dur = duration?.takeIf { it.isNotBlank() }
            return if (dur != null) "$base • $dur" else base
        }

    val smallThumbnail: String
        get() = upgradeThumbnailUrl(thumbnailUrl, videoId, Quality.SMALL)

    val highResThumbnail: String
        get() = upgradeThumbnailUrl(thumbnailUrl, videoId, Quality.HIGH)

    val ultraResThumbnail: String
        get() = upgradeThumbnailUrl(thumbnailUrl, videoId, Quality.ULTRA)

    enum class Quality(val googleSize: Int, val ytFile: String) {
        SMALL(226, "mqdefault.jpg"),
        HIGH(420, "hqdefault.jpg"),
        ULTRA(1080, "maxresdefault.jpg")
    }

    companion object {
        private val TYPE_PREFIX_REGEX = Regex(
            "^(Lagu|Song|Video|Album|Single|EP|Artis|Artist|Playlist|Mix)\\s*•\\s*",
            RegexOption.IGNORE_CASE
        )
        private val DURATION_PATTERN = Regex("^\\d{1,2}:\\d{2}(:\\d{2})?$")
        private val GOOGLE_WH_REGEX = Regex("=w\\d+-h\\d+[^&?\\s]*")
        private val GOOGLE_S_REGEX = Regex("=s\\d+[^&?\\s]*")

        fun sanitizeArtistName(rawSubtitle: String, isCollection: Boolean = false): String {
            if (rawSubtitle.isBlank()) return if (isCollection) "Playlist" else "Artis"
            val strippedPrefix = rawSubtitle.replace(TYPE_PREFIX_REGEX, "").trim()
            val parts = strippedPrefix.split("•").map { it.trim() }.filter { it.isNotBlank() }
            if (parts.isEmpty()) return strippedPrefix.ifBlank { "Artis" }

            val candidate = parts.firstOrNull { part ->
                !DURATION_PATTERN.matches(part) &&
                    !part.equals("Lagu", ignoreCase = true) &&
                    !part.equals("Song", ignoreCase = true) &&
                    !part.equals("Video", ignoreCase = true)
            } ?: parts.first()

            return candidate.ifBlank { "Artis" }
        }

        private val YT_VI_ID_REGEX = Regex("ytimg\\.com/(?:vi|vi_webp)/([^/?#&]+)")

        fun upgradeThumbnailUrl(
            url: String?,
            videoId: String? = null,
            quality: Quality = Quality.HIGH
        ): String {
            val raw = url?.trim().orEmpty()
            if (raw.startsWith("file://") || raw.startsWith("content://") || raw.startsWith("/")) {
                return raw
            }
            val clean = if (raw.startsWith("//")) "https:$raw" else raw
            val validVideoId = videoId?.trim()?.takeIf {
                it.isNotBlank() &&
                    !it.startsWith("UC") &&
                    !it.startsWith("VL") &&
                    !it.startsWith("PL") &&
                    !it.startsWith("MPRE") &&
                    !it.startsWith("local_")
            }

            if (clean.isNotBlank()) {
                if (clean.contains("googleusercontent.com") || clean.contains("ggpht.com")) {
                    val targetSize = quality.googleSize
                    return when {
                        GOOGLE_WH_REGEX.containsMatchIn(clean) ->
                            clean.replace(GOOGLE_WH_REGEX, "=w$targetSize-h$targetSize-l90-rj")
                        GOOGLE_S_REGEX.containsMatchIn(clean) ->
                            clean.replace(GOOGLE_S_REGEX, "=s$targetSize")
                        !clean.contains("=") ->
                            "$clean=w$targetSize-h$targetSize-l90-rj"
                        else -> clean
                    }
                }
                if (clean.contains("ytimg.com")) {
                    val matchedVid = YT_VI_ID_REGEX.find(clean)?.groupValues?.getOrNull(1)
                        ?: validVideoId
                    if (!matchedVid.isNullOrBlank()) {
                        // Keep explicit maxresdefault/sddefault/hq720 if already resolved from /player
                        if (quality == Quality.ULTRA &&
                            (clean.contains("maxresdefault") || clean.contains("hq720") || clean.contains("sddefault"))
                        ) {
                            return clean.replace("vi_webp", "vi").replace(".webp", ".jpg").substringBefore("?")
                        }
                        val ytFile = if (quality == Quality.SMALL) "mqdefault.jpg" else "hqdefault.jpg"
                        return "https://i.ytimg.com/vi/$matchedVid/$ytFile"
                    }
                    return clean
                }
                return clean
            }
            if (!validVideoId.isNullOrBlank()) {
                val ytFile = if (quality == Quality.SMALL) "mqdefault.jpg" else "hqdefault.jpg"
                return "https://i.ytimg.com/vi/$validVideoId/$ytFile"
            }
            return ""
        }
    }
}

enum class HomeSectionStyle {
    DAILY_RECOMMENDATION,
    FOUR_ROW_SHELF,
    COMMUNITY_PLAYLIST_CARDS,
    FAVORITES_FOREVER_CARDS,
    HITS_INDONESIA_SPOTLIGHT,
    NEW_RELEASES_BENTO,
    MUSIK_CADAS_ROCK,
    STANDARD_CAROUSEL
}

enum class HomeThemeMode(val title: String, val subtitle: String) {
    DEFAULT_APP(
        title = "Default App (Rekomendasi)",
        subtitle = "Tampilan modern dengan kartu berlapis pada Favorit Selamanya, Hits Indonesia, Rilis Baru & Musik Cadas"
    ),
    YOUTUBE_MUSIC(
        title = "Default YouTube Music",
        subtitle = "Tampilan klasik karosel standar ala YouTube Music"
    )
}

@Immutable
data class HomeSection(
    val id: String,
    val title: String,
    val strapline: String? = null,
    val items: List<MusicItem>,
    val style: HomeSectionStyle = HomeSectionStyle.STANDARD_CAROUSEL
) {
    val isTrackShelf: Boolean
        get() = style == HomeSectionStyle.FOUR_ROW_SHELF
}

@Immutable
data class AudioStreamInfo(
    val videoId: String,
    val url: String,
    val mimeType: String,
    val format: String,
    val bitrate: Int,
    val durationSeconds: Int,
    val thumbnailUrl: String? = null
)

enum class PlayerSubView {
    MAIN,
    LYRICS,
    QUEUE
}

enum class LyricsProvider(val displayName: String, val creditLabel: String) {
    LRC_RED("Lirik Sinkron (lrc.red)", "Lirik oleh lrc.red"),
    YOUTUBE_MUSIC("Default YouTube Music", "Lirik oleh YouTube Music")
}

@Immutable
data class LyricWord(
    val text: String,
    val beginSec: Double,
    val endSec: Double
)

typealias LyricSyllable = LyricWord

@Immutable
data class LyricLine(
    val text: String,
    val beginSec: Double? = null,
    val endSec: Double? = null,
    val words: List<LyricWord> = emptyList(),
    val bgText: String? = null,
    val bgBeginSec: Double? = null,
    val bgEndSec: Double? = null,
    val bgWords: List<LyricWord> = emptyList(),
    val part: String? = null
)

@Immutable
data class LyricsData(
    val videoId: String,
    val lines: List<String>,
    val syncedLines: List<LyricLine> = emptyList(),
    val isSynced: Boolean = false,
    val source: String = "Lirik oleh lrc.red",
    val provider: LyricsProvider = LyricsProvider.LRC_RED,
    val hdCoverUrl: String? = null,
    val lrcSongId: String? = null,
    val accentColorHex: String? = null,
    val songwriters: List<String> = emptyList(),
    val bitChordLines: List<com.example.data.lyrics.LyricLine> = emptyList(),
    val winningSource: com.example.data.lyrics.LyricsSource? = null,
    val isEmbedded: Boolean = false
) {
    companion object {
        fun fromBitChord(
            videoId: String,
            bitChordLines: List<com.example.data.lyrics.LyricLine>,
            winningSource: com.example.data.lyrics.LyricsSource?,
            isEmbedded: Boolean = false,
            fallbackLyricsData: LyricsData? = null
        ): LyricsData {
            val nonGap = bitChordLines.filter { !it.isGap }
            val hasTimestamps = nonGap.any { it.timeMs > 0L || it.isWordSynced }
            val convertedSynced = nonGap.mapIndexed { idx, bLine ->
                val nextStartMs = nonGap.getOrNull(idx + 1)?.timeMs
                val bEndMs = bLine.endMs(nextStartMs)
                val wordsConverted = bLine.words.flatMap { w ->
                    if (w.syllables.isNotEmpty()) {
                        w.syllables.map { syl ->
                            val sIdx = syl.startChar.coerceIn(0, w.text.length)
                            val eIdx = syl.endChar.coerceIn(sIdx, w.text.length)
                            LyricWord(
                                text = w.text.substring(sIdx, eIdx),
                                beginSec = syl.startMs / 1000.0,
                                endSec = syl.endMs / 1000.0
                            )
                        }
                    } else {
                        listOf(
                            LyricWord(
                                text = w.text,
                                beginSec = w.startMs / 1000.0,
                                endSec = w.endMs / 1000.0
                            )
                        )
                    }
                }
                val bg = bLine.background
                val bgWordsConverted = bg?.words?.map { w ->
                    LyricWord(
                        text = w.text,
                        beginSec = w.startMs / 1000.0,
                        endSec = w.endMs / 1000.0
                    )
                } ?: emptyList()

                LyricLine(
                    text = bLine.text,
                    beginSec = if (hasTimestamps) bLine.timeMs / 1000.0 else null,
                    endSec = if (hasTimestamps) bEndMs / 1000.0 else null,
                    words = wordsConverted,
                    bgText = bg?.text,
                    bgBeginSec = bg?.timeMs?.let { it / 1000.0 },
                    bgEndSec = bg?.endMs(null)?.let { it / 1000.0 },
                    bgWords = bgWordsConverted,
                    part = if (bLine.alignment == com.example.data.lyrics.LyricAlignment.End) "v2" else "v1"
                )
            }
            val credit = when {
                isEmbedded -> "Lirik Offline (Embedded .lrc)"
                winningSource != null -> "Lirik oleh ${winningSource.displayName}"
                fallbackLyricsData != null -> fallbackLyricsData.source
                else -> "Lirik oleh BitChord Engine"
            }
            return LyricsData(
                videoId = videoId,
                lines = nonGap.map { it.text }.ifEmpty { fallbackLyricsData?.lines ?: listOf("Lirik belum tersedia untuk lagu ini.") },
                syncedLines = if (hasTimestamps) convertedSynced else emptyList(),
                isSynced = hasTimestamps,
                source = credit,
                provider = if (winningSource == com.example.data.lyrics.LyricsSource.YOUTUBE_MUSIC || winningSource == com.example.data.lyrics.LyricsSource.YOUTUBE_TRANSCRIPT) {
                    LyricsProvider.YOUTUBE_MUSIC
                } else {
                    LyricsProvider.LRC_RED
                },
                hdCoverUrl = fallbackLyricsData?.hdCoverUrl,
                lrcSongId = fallbackLyricsData?.lrcSongId,
                accentColorHex = fallbackLyricsData?.accentColorHex,
                songwriters = fallbackLyricsData?.songwriters ?: emptyList(),
                bitChordLines = bitChordLines,
                winningSource = winningSource,
                isEmbedded = isEmbedded
            )
        }
    }
}

@Immutable
data class PlaylistDetail(
    val browseId: String = "",
    val title: String = "",
    val authorSubtitle: String = "",
    val metaSubtitle: String = "",
    val description: String? = "Disimpan di Perpustakaan Anda",
    val thumbnailUrl: String = "",
    val totalTracks: Int = 0,
    val tracks: List<MusicItem> = emptyList(),
    val id: String = browseId,
    val subtitle: String = authorSubtitle.ifBlank { metaSubtitle },
    val isAlbum: Boolean = false
) {
    val highResThumbnail: String
        get() = MusicItem.upgradeThumbnailUrl(
            thumbnailUrl.ifBlank { tracks.firstOrNull()?.thumbnailUrl.orEmpty() },
            tracks.firstOrNull()?.videoId,
            MusicItem.Quality.HIGH
        )
}

enum class ArtistReleaseTab(val label: String) {
    ALL("Semua"),
    ALBUMS("Album"),
    SINGLES("Single & EP")
}

@Immutable
data class ArtistProfileDetail(
    val browseId: String,
    val name: String,
    val subscribersText: String = "",
    val monthlyListenersText: String = "",
    val description: String = "",
    val avatarUrl: String = "",
    val bannerUrl: String = "",
    val headerImageUrl: String = bannerUrl.ifBlank { avatarUrl },
    val latestRelease: MusicItem? = null,
    val topSongs: List<MusicItem> = emptyList(),
    val albums: List<MusicItem> = emptyList(),
    val singlesAndEps: List<MusicItem> = emptyList(),
    val videos: List<MusicItem> = emptyList(),
    val featuredPlaylists: List<MusicItem> = emptyList(),
    val relatedArtists: List<MusicItem> = emptyList()
) {
    val id: String
        get() = browseId
}

@Immutable
data class SubscribedArtistItem(
    val id: String = "",
    val name: String,
    val avatarUrl: String,
    val subscribersText: String = "Artis",
    val subscribedAt: Long = System.currentTimeMillis(),
    val browseId: String = id
)

@Immutable
data class SpotifyImportProgressState(
    val isImporting: Boolean = false,
    val playlistTitle: String = "",
    val totalTracks: Int = 0,
    val processedTracks: Int = 0,
    val successCount: Int = 0,
    val failedCount: Int = 0,
    val currentTrackName: String = "",
    val statusText: String = ""
) {
    val progressPercent: Int
        get() = if (totalTracks <= 0) 0 else ((processedTracks * 100) / totalTracks).coerceIn(0, 100)
    val isActive: Boolean
        get() = isImporting
    val succeededTracks: Int
        get() = successCount
    val failedTracks: Int
        get() = failedCount
    val currentTrackTitle: String
        get() = currentTrackName.ifBlank { statusText }
}

enum class IntegrationPlatform(
    val id: String,
    val title: String,
    val subtitle: String,
    val actionLabel: String,
    val isSoon: Boolean
) {
    YOUTUBE_MUSIC(
        id = "ytm_google",
        title = "YouTube Music",
        subtitle = "Login dengan Google OAuth atau impor link playlist YouTube Music",
        actionLabel = "Continue with Google",
        isSoon = false
    ),
    SPOTIFY(
        id = "spotify",
        title = "Spotify",
        subtitle = "Login akun Spotify / API Token atau impor link playlist Spotify",
        actionLabel = "Connect with Spotify",
        isSoon = false
    ),
    DISCORD(
        id = "discord",
        title = "Discord Rich Presence",
        subtitle = "Tampilkan lagu yang sedang diputar di status Discord",
        actionLabel = "Soon",
        isSoon = true
    ),
    LAST_FM(
        id = "lastfm",
        title = "Last.fm Scrobbler",
        subtitle = "Catat riwayat pemutaran lagu secara otomatis ke Last.fm",
        actionLabel = "Soon",
        isSoon = true
    ),
    APPLE_MUSIC(
        id = "apple_music",
        title = "Apple Music",
        subtitle = "Sinkronisasi koleksi lagu dari Apple Music",
        actionLabel = "Soon",
        isSoon = true
    ),
    TIDAL(
        id = "tidal",
        title = "Tidal HiFi",
        subtitle = "Integrasi playlist lossless dari Tidal",
        actionLabel = "Soon",
        isSoon = true
    );

    val displayName: String
        get() = title
}

@Immutable
data class ConnectedAccountState(
    val platform: IntegrationPlatform,
    val isConnected: Boolean = false,
    val accountName: String = "",
    val accountEmailOrHandle: String = "",
    val avatarUrl: String = "",
    val importedPlaylistsCount: Int = 0,
    val lastSyncLabel: String = "",
    val displayName: String = accountName,
    val handleOrEmail: String = accountEmailOrHandle,
    val lastSyncedAt: String = lastSyncLabel,
    val importedPlaylistCount: Int = importedPlaylistsCount
)

enum class SearchFilter(val label: String, val scraperParamKey: String?) {
    ALL("All", null),
    SONGS("Songs", "songs"),
    VIDEOS("Videos", "videos"),
    ALBUMS("Albums", "albums"),
    ARTISTS("Artists", "artists"),
    PLAYLISTS("Playlists", "playlists");

    val apiFilterKey: String?
        get() = scraperParamKey
}

@Immutable
data class SearchCategorySection(
    val title: String,
    val items: List<MusicItem>
)

@Immutable
data class SearchSuggestionPayload(
    val querySuggestions: List<String> = emptyList(),
    val directItems: List<MusicItem> = emptyList()
)

@Immutable
data class SearchUiState(
    val query: String = "",
    val isSubmitted: Boolean = false,
    val selectedFilter: SearchFilter = SearchFilter.ALL,
    val isLoadingSuggestions: Boolean = false,
    val isLoadingResults: Boolean = false,
    val querySuggestions: List<String> = emptyList(),
    val directSuggestionItems: List<MusicItem> = emptyList(),
    val searchHistory: List<String> = listOf(
        "teh hijau tulus",
        "sakura rossa",
        "hati-hati di jalan tulus",
        "untungnya hidup harus tetap berjalan bernadya"
    ),
    val allCategorySections: List<SearchCategorySection> = emptyList(),
    val filteredItems: List<MusicItem> = emptyList()
)

enum class PlaylistPrivacy(val label: String) {
    PRIVATE("Private"),
    UNLISTED("Unlisted"),
    PUBLIC("Public")
}

enum class PlaylistSourceType(val badge: String) {
    CUSTOM("user"),
    USER_CREATED("user"),
    IMPORTED("Impor"),
    SPOTIFY_IMPORT("Spotify"),
    YOUTUBE_IMPORT("YouTube Music"),
    SAVED_COMMUNITY("Tersimpan")
}

@Immutable
data class UserLibraryPlaylist(
    val id: String,
    val title: String,
    val subtitle: String,
    val description: String = "Disimpan di Perpustakaan Anda",
    val thumbnailUrl: String = "",
    val privacy: PlaylistPrivacy = PlaylistPrivacy.PRIVATE,
    val isPinned: Boolean = false,
    val sourceType: PlaylistSourceType = PlaylistSourceType.CUSTOM,
    val platformBadge: String = when (sourceType) {
        PlaylistSourceType.SPOTIFY_IMPORT -> "Spotify"
        PlaylistSourceType.YOUTUBE_IMPORT -> "YouTube Music"
        else -> ""
    },
    val createdAt: Long = System.currentTimeMillis(),
    val tracks: List<MusicItem> = emptyList()
) {
    val effectiveThumbnail: String
        get() = thumbnailUrl.ifBlank {
            tracks.firstOrNull()?.highResThumbnail.orEmpty()
        }

    fun toMusicItem(): MusicItem = MusicItem(
        id = id,
        title = title,
        subtitle = if (tracks.isNotEmpty()) "$subtitle • ${tracks.size} lagu" else subtitle,
        thumbnailUrl = effectiveThumbnail,
        browseId = id,
        playlistId = id,
        type = "playlist",
        previewTracks = tracks,
        viewsAndCountText = "${tracks.size} lagu"
    )
}

@Immutable
data class DownloadedTrackItem(
    val id: String,
    val videoId: String?,
    val title: String,
    val subtitle: String,
    val thumbnailUrl: String,
    val duration: String?,
    val localFilePath: String,
    val mediaStoreUri: String? = null,
    val fileSizeBytes: Long = 0L,
    val downloadedAt: Long = System.currentTimeMillis()
) {
    fun toMusicItem(): MusicItem = MusicItem(
        id = id,
        title = title,
        subtitle = subtitle,
        thumbnailUrl = thumbnailUrl,
        videoId = videoId ?: id,
        type = "song",
        duration = duration,
        localFilePath = localFilePath
    )
}

@Immutable
data class ActiveDownloadProgress(
    val id: String,
    val title: String,
    val subtitle: String,
    val progressPercent: Int,
    val currentTrackIndex: Int = 1,
    val totalTracks: Int = 1,
    val isCompleted: Boolean = false,
    val statusText: String = ""
)

@Immutable
data class ListeningStatsState(
    val minutesListened: Int = 0,
    val totalPlays: Int = 0,
    val totalTracksPlayed: Int = 0,
    val topTrack: MusicItem? = null,
    val topSongTitle: String = topTrack?.title ?: "About You",
    val topSongArtist: String = topTrack?.cleanArtist ?: "The 1975",
    val topSongPlays: Int = 0,
    val memberSince: String = "20/10/2026",
    val userHandle: String = "user"
)

@Immutable
data class PlayerUiState(
    val currentTrack: MusicItem? = null,
    val isPlaying: Boolean = false,
    val isLoading: Boolean = false,
    val statusText: String = "",
    val streamFormat: String = "",
    val currentPositionMs: Long = 0L,
    val exactPositionSec: Double = 0.0,
    val currentPositionSec: Int = 0,
    val durationSec: Int = 215,
    val progressFraction: Float = 0f,
    val volumeFraction: Float = 0.80f,
    val isFullPlayerOpen: Boolean = false,
    val activeSubView: PlayerSubView = PlayerSubView.MAIN,
    val isShuffleEnabled: Boolean = false,
    val isRepeatEnabled: Boolean = false,
    val isAutoPlayEnabled: Boolean = true,
    val playingFromSource: String = "Pilihan cepat",
    val autoPlayQueue: List<MusicItem> = emptyList(),
    val currentLyrics: LyricsData? = null,
    val selectedLyricsProvider: LyricsProvider = LyricsProvider.LRC_RED,
    val isLoadingLyrics: Boolean = false,
    val errorMessage: String? = null
)

@Immutable
data class ListenNowUiState(
    val isLoading: Boolean = true,
    val recentShortcuts: List<MusicItem> = emptyList(),
    val homeSections: List<HomeSection> = emptyList(),
    val selectedBottomTab: Int = 0,
    val homeThemeMode: HomeThemeMode = HomeThemeMode.DEFAULT_APP,
    val playerState: PlayerUiState = PlayerUiState(),
    val activePlaylistDetail: PlaylistDetail? = null,
    val isLoadingPlaylistDetail: Boolean = false,
    val playlistSearchQuery: String = "",
    val isPlaylistSearchVisible: Boolean = false,
    val playlistSortAscending: Boolean = false,
    val activeArtistProfile: ArtistProfileDetail? = null,
    val isLoadingArtistProfile: Boolean = false,
    val selectedArtistReleaseTab: ArtistReleaseTab = ArtistReleaseTab.ALL,
    val followedArtistIds: Set<String> = emptySet(),
    val isShowingSettingsScreen: Boolean = false,
    val isShowingAccountIntegrationsScreen: Boolean = false,
    val connectedAccounts: Map<IntegrationPlatform, ConnectedAccountState> = IntegrationPlatform.entries.associateWith {
        ConnectedAccountState(platform = it)
    },
    val importedPlaylists: List<MusicItem> = emptyList(),
    val isSyncingIntegration: Boolean = false,
    val integrationStatusMessage: String? = null,
    val searchState: SearchUiState = SearchUiState(),
    val favoriteIds: Set<String> = emptySet(),
    val favoriteTracks: List<MusicItem> = emptyList(),
    val savedCommunityIds: Set<String> = emptySet(),
    val savedCommunityPlaylists: List<MusicItem> = emptyList(),
    val customCoverOverrides: Map<String, String> = emptyMap(),
    val availableLrcCovers: Map<String, String> = emptyMap(),
    val selectedTrackForMenu: MusicItem? = null,
    val isShowingAllShortcutsSheet: Boolean = false
)
