package com.example.ui.home

import android.app.Application
import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.lyrics.EmbeddedLyrics
import com.example.data.lyrics.LyricLine as BitChordLyricLine
import com.example.data.lyrics.LyricsProviderState
import com.example.data.lyrics.LyricsRepository
import com.example.data.lyrics.LyricsSettingsState
import com.example.data.lyrics.LyricsSettingsStore
import com.example.data.lyrics.LyricsSource
import com.example.data.lyrics.LyricsTranslation
import com.example.data.model.ActiveDownloadProgress
import com.example.data.model.ArtistProfileDetail
import com.example.data.model.ConnectedAccountState
import com.example.data.model.DownloadedTrackItem
import com.example.data.model.HomeSection
import com.example.data.model.HomeThemeMode
import com.example.data.model.IntegrationPlatform
import com.example.data.model.ListeningStatsState
import com.example.data.model.LyricsData
import com.example.data.model.LyricsProvider
import com.example.data.model.MusicItem
import com.example.data.model.PlaylistDetail
import com.example.data.model.PlaylistPrivacy
import com.example.data.model.PlaylistSourceType
import com.example.data.model.SearchCategorySection
import com.example.data.model.SearchFilter
import com.example.data.model.SpotifyImportProgressState
import com.example.data.model.SubscribedArtistItem
import com.example.data.model.UserLibraryPlaylist
import com.example.data.remote.CuratedHomeFallback
import com.example.data.repository.MusicRepository
import com.example.player.AudioPlayerController
import com.example.player.MusicDownloadManager
import com.example.player.PlayerUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class PlayerSubView {
    MAIN,
    LYRICS,
    QUEUE
}

@Immutable
data class HomeUiState(
    val isLoading: Boolean = true,
    val pintasanCepatItems: List<MusicItem> = emptyList(),
    val topShelfTitle: String = "Pilihan cepat",
    val topShelfItems: List<MusicItem> = emptyList(),
    val carouselSections: List<HomeSection> = emptyList(),
    val importedPlaylists: List<MusicItem> = emptyList(),
    val hasListenedToMusic: Boolean = false,
    val selectedTrackForMenu: MusicItem? = null,
    val homeThemeMode: HomeThemeMode = HomeThemeMode.DEFAULT_APP,
    val savedPlaylistIds: Set<String> = emptySet(),
    val errorMessage: String? = null
)

@Immutable
data class PlaylistScreenState(
    val isVisible: Boolean = false,
    val isLoading: Boolean = false,
    val playlist: PlaylistDetail? = null,
    val isSearchVisible: Boolean = false,
    val searchQuery: String = "",
    val sortAscending: Boolean = true
)

@Immutable
data class ArtistScreenState(
    val isVisible: Boolean = false,
    val isLoading: Boolean = false,
    val artistProfile: ArtistProfileDetail? = null,
    val followedArtistIds: Set<String> = emptySet()
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
    val allCategorySections: List<SearchCategorySection> = emptyList(),
    val filteredItems: List<MusicItem> = emptyList(),
    val recentSearches: List<String> = listOf(
        "Teh Hijau Tulus",
        "Sakura Rossa",
        "Nadin Amizah",
        "Mahalini Sial",
        "Budi Doremi Mesin Waktu",
        "Sheila On 7",
        "Bernadya Satu Bulan"
    )
)

class MusicHomeViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val repository = MusicRepository(database.recentTrackDao())
    private val playerController = AudioPlayerController.getInstance(application)
    private val downloadManager = MusicDownloadManager.getInstance(application)
    private val lyricsSettingsStore = LyricsSettingsStore.getInstance(application)
    private val prefs = application.getSharedPreferences("bitchord_experimental_prefs", Context.MODE_PRIVATE)

    private val installDateFormatted: String by lazy {
        val firstInstallMs = try {
            val pkgInfo = application.packageManager.getPackageInfo(application.packageName, 0)
            pkgInfo.firstInstallTime.takeIf { it > 0L } ?: getOrSaveFirstOpenTimestamp()
        } catch (_: Exception) {
            getOrSaveFirstOpenTimestamp()
        }
        val sdf = SimpleDateFormat("d MMM yyyy", Locale("id", "ID"))
        sdf.format(Date(firstInstallMs))
    }

    private fun getOrSaveFirstOpenTimestamp(): Long {
        val existing = prefs.getLong("first_app_open_timestamp", 0L)
        if (existing > 0L) return existing
        val now = System.currentTimeMillis()
        prefs.edit().putLong("first_app_open_timestamp", now).apply()
        return now
    }

    private val _username = MutableStateFlow(loadSavedUsername())
    val username: StateFlow<String> = _username.asStateFlow()

    private fun loadSavedUsername(): String {
        val raw = prefs.getString("user_display_name", "user")?.trim()
        return if (raw.isNullOrBlank() || raw.equals("QIAA", ignoreCase = true)) {
            "user"
        } else {
            raw
        }
    }

    fun updateUsername(newName: String) {
        val clean = newName.trim().ifBlank { "user" }
        _username.value = clean
        prefs.edit().putString("user_display_name", clean).apply()
        _uiMessage.value = "Username berhasil diubah menjadi \"$clean\""
    }

    private val _scrapedSections = MutableStateFlow<List<HomeSection>>(emptyList())
    private val _isLoading = MutableStateFlow(true)
    private val _selectedTrackForMenu = MutableStateFlow<MusicItem?>(null)
    private val _errorMessage = MutableStateFlow<String?>(null)

    private val _uiMessage = MutableStateFlow<String?>(null)
    val uiMessage: StateFlow<String?> = _uiMessage.asStateFlow()

    fun showUiMessage(message: String) {
        _uiMessage.value = message
    }

    fun clearUiMessage() {
        _uiMessage.value = null
    }

    private val _homeThemeMode = MutableStateFlow(loadSavedHomeThemeMode())
    val homeThemeMode: StateFlow<HomeThemeMode> = _homeThemeMode.asStateFlow()

    val libraryPlaylists: StateFlow<List<UserLibraryPlaylist>> = repository.libraryPlaylistsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val savedPlaylistIds: StateFlow<Set<String>> = libraryPlaylists
        .map { list -> list.map { it.id }.toSet() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptySet()
        )

    val subscribedArtists: StateFlow<List<SubscribedArtistItem>> = repository.subscribedArtistsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val favoriteTracks: StateFlow<List<MusicItem>> = repository.favoriteTracksFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val downloadedTracks: StateFlow<List<DownloadedTrackItem>> = repository.downloadedTracksFlow
        .map { list ->
            list.map { item ->
                val offlineArt = downloadManager.getOfflineArtworkUri(item.videoId ?: item.id)
                    ?: downloadManager.getOfflineArtworkUri(item.id)
                if (!offlineArt.isNullOrBlank()) {
                    item.copy(thumbnailUrl = offlineArt)
                } else {
                    item
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val downloadedTrackIds: StateFlow<Set<String>> = repository.downloadedTrackIdsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptySet()
    )

    val activeDownloads: StateFlow<Map<String, ActiveDownloadProgress>> = downloadManager.activeDownloads

    val downloadStorageLocation: StateFlow<com.example.player.DownloadStorageLocation> = downloadManager.storageLocation

    private val _hasAudioPermission = MutableStateFlow(downloadManager.hasAudioStoragePermission())
    val hasAudioPermission: StateFlow<Boolean> = _hasAudioPermission.asStateFlow()

    private val _localDeviceTracks = MutableStateFlow<List<MusicItem>>(emptyList())
    val localDeviceTracks: StateFlow<List<MusicItem>> = _localDeviceTracks.asStateFlow()

    private val _playlistScreenState = MutableStateFlow(PlaylistScreenState())
    val playlistScreenState: StateFlow<PlaylistScreenState> = _playlistScreenState.asStateFlow()

    private val _artistScreenState = MutableStateFlow(ArtistScreenState())
    val artistScreenState: StateFlow<ArtistScreenState> = _artistScreenState.asStateFlow()
    private val artistBackStack = ArrayDeque<ArtistProfileDetail>()

    private val _isSettingsVisible = MutableStateFlow(false)
    val isSettingsVisible: StateFlow<Boolean> = _isSettingsVisible.asStateFlow()

    private val _isAccountIntegrationsVisible = MutableStateFlow(false)
    val isAccountIntegrationsVisible: StateFlow<Boolean> = _isAccountIntegrationsVisible.asStateFlow()

    private val _spotifyAccount = MutableStateFlow(ConnectedAccountState(platform = IntegrationPlatform.SPOTIFY))
    val spotifyAccount: StateFlow<ConnectedAccountState> = _spotifyAccount.asStateFlow()

    private val _youtubeMusicAccount = MutableStateFlow(ConnectedAccountState(platform = IntegrationPlatform.YOUTUBE_MUSIC))
    val youtubeMusicAccount: StateFlow<ConnectedAccountState> = _youtubeMusicAccount.asStateFlow()

    private val _importedPlaylists = MutableStateFlow<List<MusicItem>>(emptyList())
    val importedPlaylists: StateFlow<List<MusicItem>> = _importedPlaylists.asStateFlow()

    private val _isIntegrationSyncing = MutableStateFlow(false)
    val isIntegrationSyncing: StateFlow<Boolean> = _isIntegrationSyncing.asStateFlow()

    private val _integrationStatusMessage = MutableStateFlow<String?>(null)
    val integrationStatusMessage: StateFlow<String?> = _integrationStatusMessage.asStateFlow()

    private val _spotifyImportProgress = MutableStateFlow(SpotifyImportProgressState())
    val spotifyImportProgress: StateFlow<SpotifyImportProgressState> = _spotifyImportProgress.asStateFlow()

    private val _searchUiState = MutableStateFlow(SearchUiState())
    val searchUiState: StateFlow<SearchUiState> = _searchUiState.asStateFlow()

    private val _isPlayerExpanded = MutableStateFlow(false)
    val isPlayerExpanded: StateFlow<Boolean> = _isPlayerExpanded.asStateFlow()

    private val _activePlayerSubView = MutableStateFlow(PlayerSubView.MAIN)
    val activePlayerSubView: StateFlow<PlayerSubView> = _activePlayerSubView.asStateFlow()

    private val _isVideoModeActive = MutableStateFlow(false)
    val isVideoModeActive: StateFlow<Boolean> = _isVideoModeActive.asStateFlow()

    private val _activeVideoStreamUrl = MutableStateFlow<String?>(null)
    val activeVideoStreamUrl: StateFlow<String?> = _activeVideoStreamUrl.asStateFlow()

    private val _isResolvingVideoStream = MutableStateFlow(false)
    val isResolvingVideoStream: StateFlow<Boolean> = _isResolvingVideoStream.asStateFlow()

    private val _currentLyrics = MutableStateFlow<LyricsData?>(null)
    val currentLyrics: StateFlow<LyricsData?> = _currentLyrics.asStateFlow()

    private val _selectedLyricsProvider = MutableStateFlow(LyricsProvider.LRC_RED)
    val selectedLyricsProvider: StateFlow<LyricsProvider> = _selectedLyricsProvider.asStateFlow()

    // BitChord Lyrics Engine & Multi-Provider State
    val lyricsSettings: StateFlow<LyricsSettingsState> = lyricsSettingsStore.state

    private val _bitChordLyricsLines = MutableStateFlow<List<BitChordLyricLine>>(emptyList())
    val bitChordLyricsLines: StateFlow<List<BitChordLyricLine>> = _bitChordLyricsLines.asStateFlow()

    private val _activeLyricsSource = MutableStateFlow<LyricsSource?>(null)
    val activeLyricsSource: StateFlow<LyricsSource?> = _activeLyricsSource.asStateFlow()

    private val _isEmbeddedLyrics = MutableStateFlow(false)
    val isEmbeddedLyrics: StateFlow<Boolean> = _isEmbeddedLyrics.asStateFlow()

    private val _isLoadingLyrics = MutableStateFlow(false)
    val isLoadingLyrics: StateFlow<Boolean> = _isLoadingLyrics.asStateFlow()

    private val _lyricsProviderStates = MutableStateFlow<Map<LyricsSource, LyricsProviderState>>(emptyMap())
    val lyricsProviderStates: StateFlow<Map<LyricsSource, LyricsProviderState>> = _lyricsProviderStates.asStateFlow()

    private val _lyricsProviderResults = MutableStateFlow<Map<LyricsSource, List<BitChordLyricLine>>>(emptyMap())
    val lyricsProviderResults: StateFlow<Map<LyricsSource, List<BitChordLyricLine>>> = _lyricsProviderResults.asStateFlow()

    private val _translatedLyrics = MutableStateFlow<List<BitChordLyricLine>?>(null)
    val translatedLyrics: StateFlow<List<BitChordLyricLine>?> = _translatedLyrics.asStateFlow()

    private val _romanizedLyrics = MutableStateFlow<List<BitChordLyricLine>?>(null)
    val romanizedLyrics: StateFlow<List<BitChordLyricLine>?> = _romanizedLyrics.asStateFlow()

    private val _isTranslationEnabled = MutableStateFlow(false)
    val isTranslationEnabled: StateFlow<Boolean> = _isTranslationEnabled.asStateFlow()

    private val _isRomanizationEnabled = MutableStateFlow(false)
    val isRomanizationEnabled: StateFlow<Boolean> = _isRomanizationEnabled.asStateFlow()

    private val _isTranslatingLyrics = MutableStateFlow(false)
    val isTranslatingLyrics: StateFlow<Boolean> = _isTranslatingLyrics.asStateFlow()

    private var lyricsFetchJob: Job? = null
    private var translationJob: Job? = null

    private val _customCoverOverrides = MutableStateFlow<Map<String, String>>(emptyMap())
    val customCoverOverrides: StateFlow<Map<String, String>> = _customCoverOverrides.asStateFlow()

    private val _artistAvatarMap = MutableStateFlow<Map<String, String>>(loadSavedArtistAvatarMap())
    val artistAvatarMap: StateFlow<Map<String, String>> = _artistAvatarMap.asStateFlow()
    private val pendingAvatarResolutions = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

    private fun loadSavedArtistAvatarMap(): Map<String, String> {
        val raw = prefs.getString("cached_artist_avatars_v2", null) ?: return emptyMap()
        return try {
            val obj = org.json.JSONObject(raw)
            val result = mutableMapOf<String, String>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val v = obj.optString(k).trim()
                if (k.isNotBlank() && v.isNotBlank() && !v.contains("FRX282637095.webp")) {
                    result[k.lowercase()] = v
                }
            }
            result
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun saveArtistAvatarToCache(artistName: String, avatarUrl: String) {
        val cleanKey = MusicItem.sanitizeArtistName(artistName, isCollection = false)
            .substringBefore(",")
            .substringBefore("&")
            .trim()
            .lowercase()
        val cleanUrl = avatarUrl.trim()
        if (cleanKey.isBlank() || cleanUrl.isBlank() || cleanUrl.contains("FRX282637095.webp")) return
        val existing = _artistAvatarMap.value[cleanKey]
        if (existing == cleanUrl) return

        val updated = _artistAvatarMap.value + (cleanKey to cleanUrl)
        _artistAvatarMap.value = updated
        try {
            val json = org.json.JSONObject()
            updated.forEach { (k, v) -> json.put(k, v) }
            prefs.edit().putString("cached_artist_avatars_v2", json.toString()).apply()
        } catch (_: Exception) {
        }
    }

    fun resolveArtistAvatarIfNeeded(artistName: String, browseId: String? = null) {
        val cleanName = MusicItem.sanitizeArtistName(artistName, isCollection = false)
            .substringBefore(",")
            .substringBefore("&")
            .trim()
        if (cleanName.isBlank() || cleanName.equals("Artis", ignoreCase = true) || cleanName.equals("user", ignoreCase = true)) return
        val key = cleanName.lowercase()
        val existing = _artistAvatarMap.value[key]
        if (!existing.isNullOrBlank() && (existing.contains("googleusercontent.com") || existing.contains("ggpht.com"))) {
            return
        }
        if (!pendingAvatarResolutions.add(key)) return

        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val resolved = repository.resolveArtistAvatar(cleanName, browseId)
                if (!resolved.isNullOrBlank()) {
                    saveArtistAvatarToCache(cleanName, resolved)
                    // Also update subscribed artist in Room if subscribed
                    val matchingSub = subscribedArtists.value.firstOrNull {
                        it.name.equals(cleanName, ignoreCase = true) ||
                            (!browseId.isNullOrBlank() && it.browseId.equals(browseId, ignoreCase = true))
                    }
                    if (matchingSub != null && matchingSub.avatarUrl != resolved) {
                        repository.upsertSubscribedArtist(matchingSub.copy(avatarUrl = resolved))
                    }
                }
            } catch (_: Exception) {
            } finally {
                pendingAvatarResolutions.remove(key)
            }
        }
    }

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    fun selectTab(tabIndex: Int) {
        _selectedTab.value = tabIndex
    }

    fun clearIntegrationStatusMessage() {
        _integrationStatusMessage.value = null
    }

    val playerState: StateFlow<PlayerUiState> = playerController.playerState

    val hasActiveTrack: StateFlow<Boolean> = playerController.playerState
        .map { it.currentTrack != null }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val currentPlayingTrackId: StateFlow<String?> = playerController.playerState
        .map { it.currentTrack?.id }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val isPlayingAudio: StateFlow<Boolean> = playerController.playerState
        .map { it.isPlaying }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val favoriteIds: StateFlow<Set<String>> = repository.favoriteIdsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptySet()
    )

    val recentTracks: StateFlow<List<MusicItem>> = repository.recentTracksFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private fun parseDurationMinutes(durationStr: String?): Int {
        if (durationStr.isNullOrBlank()) return 4
        val parts = durationStr.trim().split(":").mapNotNull { it.trim().toIntOrNull() }
        val totalSec = when (parts.size) {
            2 -> parts[0] * 60 + parts[1]
            3 -> parts[0] * 3600 + parts[1] * 60 + parts[2]
            else -> 210
        }
        return ((totalSec + 30) / 60).coerceAtLeast(1)
    }

    val listeningStats: StateFlow<ListeningStatsState> = combine(
        recentTracks,
        favoriteTracks,
        _username
    ) { recents, favs, currentUsername ->
        val totalPlays = recents.sumOf { it.playCount.coerceAtLeast(1) }
        val minutes = if (recents.isEmpty()) {
            0
        } else {
            recents.sumOf { track ->
                parseDurationMinutes(track.duration) * track.playCount.coerceAtLeast(1)
            }
        }
        val topPlayedTrack = recents.maxByOrNull { it.playCount }
        val topTrack = topPlayedTrack ?: favs.firstOrNull()
        val topSongTitle = topTrack?.title ?: "About You"
        val topSongArtist = topTrack?.cleanArtist ?: "The 1975"
        val topSongPlays = topPlayedTrack?.playCount?.coerceAtLeast(1) ?: 0

        ListeningStatsState(
            minutesListened = minutes,
            totalPlays = totalPlays,
            userHandle = currentUsername,
            memberSince = installDateFormatted,
            topSongTitle = topSongTitle,
            topSongArtist = topSongArtist,
            topSongPlays = topSongPlays,
            topTrack = topTrack,
            totalTracksPlayed = totalPlays
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ListeningStatsState(
            userHandle = _username.value,
            memberSince = installDateFormatted
        )
    )

    private var trackEnrichmentJob: Job? = null
    private var liveSuggestionJob: Job? = null
    private var fullSearchJob: Job? = null
    private var artistLoadJob: Job? = null

    val uiState: StateFlow<HomeUiState> = combine(
        repository.recentTracksFlow,
        _scrapedSections,
        _isLoading,
        _selectedTrackForMenu,
        _homeThemeMode
    ) { recents, sections, loading, menuTrack, themeMode ->
        val quickPicksSection = sections.firstOrNull { sec ->
            val low = sec.title.lowercase()
            low.contains("pilihan cepat") || low.contains("quick picks")
        }

        val topShelfItems = quickPicksSection?.items?.takeIf { it.isNotEmpty() }
            ?: repository.getQuickPicksDefault()

        val remainingSections = sections.filterNot { sec ->
            sec.id == quickPicksSection?.id
        }

        val pintasanItems = if (recents.isNotEmpty()) {
            recents.take(27)
        } else {
            emptyList()
        }

        HomeUiState(
            isLoading = loading,
            pintasanCepatItems = pintasanItems,
            topShelfTitle = "Pilihan cepat",
            topShelfItems = topShelfItems,
            carouselSections = remainingSections,
            importedPlaylists = emptyList(),
            hasListenedToMusic = recents.isNotEmpty(),
            selectedTrackForMenu = menuTrack,
            homeThemeMode = themeMode,
            savedPlaylistIds = savedPlaylistIds.value,
            errorMessage = _errorMessage.value
        )
    }.combine(savedPlaylistIds) { baseState, savedIds ->
        baseState.copy(savedPlaylistIds = savedIds)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState(
            isLoading = true,
            pintasanCepatItems = emptyList(),
            topShelfTitle = "Pilihan cepat",
            topShelfItems = repository.getQuickPicksDefault(),
            carouselSections = CuratedHomeFallback.getRequiredHomeSectionsFallback().drop(1),
            homeThemeMode = _homeThemeMode.value
        )
    )

    init {
        playerController.setFavoriteCallback { track ->
            toggleFavoriteTrack(track)
        }
        viewModelScope.launch {
            combine(
                playerController.playerState.map { it.currentTrack }.distinctUntilChanged(),
                favoriteIds
            ) { track, favs ->
                if (track == null) false
                else favs.contains(track.id) || (track.videoId != null && favs.contains(track.videoId))
            }.collect { isFav ->
                playerController.updateCurrentTrackFavoriteStatus(isFav)
            }
        }
        viewModelScope.launch {
            subscribedArtists.collect { subs ->
                val ids = buildSet {
                    subs.forEach { artist ->
                        add(artist.browseId)
                        add(artist.name)
                        add("UC_${artist.name.lowercase()}")
                    }
                }
                _artistScreenState.update { it.copy(followedArtistIds = ids) }
            }
        }
        viewModelScope.launch {
            favoriteTracks.collect { favs ->
                val current = _playlistScreenState.value
                if (current.isVisible && current.playlist?.id == LIKED_SONGS_PLAYLIST_ID) {
                    _playlistScreenState.update {
                        it.copy(
                            playlist = it.playlist?.copy(
                                subtitle = "${_username.value}, ${favs.size} lagu",
                                authorSubtitle = "${_username.value}, ${favs.size} lagu",
                                metaSubtitle = "PLAYLIST • ${favs.size} TRACKS",
                                thumbnailUrl = favs.firstOrNull()?.thumbnailUrl ?: "",
                                totalTracks = favs.size,
                                tracks = favs
                            )
                        )
                    }
                }
            }
        }
        viewModelScope.launch {
            downloadedTracks.collect { downloads ->
                val current = _playlistScreenState.value
                if (current.isVisible && current.playlist?.id == DOWNLOADS_PLAYLIST_ID) {
                    val items = downloads.map { it.toMusicItem() }
                    _playlistScreenState.update {
                        it.copy(
                            playlist = it.playlist?.copy(
                                subtitle = "${_username.value}, ${items.size} lagu",
                                authorSubtitle = "${_username.value}, ${items.size} lagu",
                                metaSubtitle = "PLAYLIST • ${items.size} TRACKS",
                                thumbnailUrl = items.firstOrNull()?.thumbnailUrl ?: "",
                                totalTracks = items.size,
                                tracks = items
                            )
                        )
                    }
                }
            }
        }
        viewModelScope.launch {
            libraryPlaylists.collect { playlists ->
                val current = _playlistScreenState.value
                val openId = current.playlist?.id
                if (current.isVisible && openId != null) {
                    val match = playlists.firstOrNull { it.id == openId }
                    if (match != null) {
                        val effectiveTracks = if (match.tracks.isNotEmpty() || match.sourceType == PlaylistSourceType.USER_CREATED) {
                            match.tracks
                        } else {
                            current.playlist.tracks
                        }
                        _playlistScreenState.update {
                            it.copy(
                                playlist = it.playlist?.copy(
                                    title = match.title,
                                    subtitle = match.subtitle,
                                    authorSubtitle = "${_username.value}, ${effectiveTracks.size} lagu",
                                    metaSubtitle = "PLAYLIST • ${effectiveTracks.size} TRACKS",
                                    thumbnailUrl = match.thumbnailUrl.ifBlank {
                                        effectiveTracks.firstOrNull()?.thumbnailUrl ?: ""
                                    },
                                    totalTracks = effectiveTracks.size,
                                    tracks = effectiveTracks
                                )
                            )
                        }
                    }
                }
            }
        }
        loadHomeData()
        refreshLocalDeviceMusic()
    }

    private fun loadSavedHomeThemeMode(): HomeThemeMode {
        val raw = prefs.getString("home_theme_mode", HomeThemeMode.DEFAULT_APP.name)
        return try {
            HomeThemeMode.valueOf(raw ?: HomeThemeMode.DEFAULT_APP.name)
        } catch (_: Exception) {
            HomeThemeMode.DEFAULT_APP
        }
    }

    fun setHomeThemeMode(mode: HomeThemeMode) {
        _homeThemeMode.value = mode
        prefs.edit().putString("home_theme_mode", mode.name).apply()
    }

    fun loadHomeData() {
        viewModelScope.launch {
            if (_scrapedSections.value.isEmpty()) {
                _scrapedSections.value = CuratedHomeFallback.getRequiredHomeSectionsFallback()
                _isLoading.value = false
            }
            _errorMessage.value = null
            val sections = repository.fetchHomeSections()
            if (sections.isNotEmpty()) {
                _scrapedSections.value = sections
            }
            _isLoading.value = false
        }
    }

    // ========================================================================
    // LIBRARY & PLAYLIST MANAGEMENT (CREATE, RENAME, DELETE, PIN, REORDER, ADD TRACK, SAVE)
    // ========================================================================
    fun createCustomPlaylist(title: String, privacy: PlaylistPrivacy = PlaylistPrivacy.PUBLIC) {
        val clean = title.trim()
        if (clean.isBlank()) return
        viewModelScope.launch {
            repository.createCustomPlaylist(clean, privacy, ownerUsername = _username.value)
            _uiMessage.value = "Playlist \"$clean\" berhasil dibuat di Library"
        }
    }

    fun createCustomPlaylistAndAddTrack(
        title: String,
        privacy: PlaylistPrivacy = PlaylistPrivacy.PUBLIC,
        track: MusicItem
    ) {
        val clean = title.trim()
        if (clean.isBlank()) return
        viewModelScope.launch {
            val created = repository.createCustomPlaylist(clean, privacy, ownerUsername = _username.value)
            repository.addTrackToLibraryPlaylist(created.id, track)
            _uiMessage.value = "\"${track.title}\" disimpan ke playlist baru \"$clean\""
        }
    }

    fun renameLibraryPlaylist(playlistId: String, newTitle: String) {
        val clean = newTitle.trim()
        if (clean.isBlank()) return
        viewModelScope.launch {
            repository.renameLibraryPlaylist(playlistId, clean)
            if (_playlistScreenState.value.playlist?.id == playlistId) {
                _playlistScreenState.update { st ->
                    st.copy(playlist = st.playlist?.copy(title = clean))
                }
            }
            _uiMessage.value = "Nama playlist diubah menjadi \"$clean\""
        }
    }

    fun deleteLibraryPlaylist(playlistId: String) {
        viewModelScope.launch {
            repository.deleteLibraryPlaylist(playlistId)
            if (_playlistScreenState.value.playlist?.id == playlistId) {
                closePlaylistDetail()
            }
            _uiMessage.value = "Playlist dihapus dari Library"
        }
    }

    fun togglePinLibraryPlaylist(playlistId: String) {
        viewModelScope.launch {
            val target = libraryPlaylists.value.firstOrNull { it.id == playlistId }
            if (target != null) {
                val nextPinned = !target.isPinned
                repository.setPlaylistPinned(playlistId, nextPinned)
                _uiMessage.value = if (nextPinned) {
                    "\"${target.title}\" disematkan (Pin) di atas Library"
                } else {
                    "Pin \"${target.title}\" dilepas"
                }
            } else {
                val openDetail = _playlistScreenState.value.playlist
                if (openDetail != null && openDetail.id == playlistId) {
                    repository.savePlaylistToLibrary(openDetail, PlaylistSourceType.SAVED_COMMUNITY)
                    repository.setPlaylistPinned(playlistId, true)
                    _uiMessage.value = "\"${openDetail.title}\" disimpan & disematkan (Pin) di Library"
                }
            }
        }
    }

    fun reorderPlaylistTracks(playlistId: String, newOrderedTracks: List<MusicItem>) {
        _playlistScreenState.update { st ->
            if (st.playlist?.id == playlistId) {
                st.copy(
                    playlist = st.playlist.copy(
                        tracks = newOrderedTracks,
                        totalTracks = newOrderedTracks.size
                    )
                )
            } else {
                st
            }
        }
        viewModelScope.launch {
            if (savedPlaylistIds.value.contains(playlistId)) {
                repository.reorderPlaylistTracks(playlistId, newOrderedTracks)
            }
            _uiMessage.value = "Urutan lagu di playlist diperbarui"
        }
    }

    fun playPlaylistNext(detail: PlaylistDetail) {
        if (detail.tracks.isEmpty()) {
            _uiMessage.value = "Tidak ada lagu di playlist ini"
            return
        }
        playerController.playNextInQueue(
            tracks = detail.tracks,
            sourceLabel = "Playing from ${detail.title}",
            resolveStream = { track -> repository.resolveAudioStream(track) },
            onTrackStarted = { startedTrack -> enrichTrackPlaybackContext(startedTrack) }
        )
        _uiMessage.value = "${detail.tracks.size} lagu dari \"${detail.title}\" akan diputar berikutnya"
    }

    fun addPlaylistToQueue(detail: PlaylistDetail) {
        if (detail.tracks.isEmpty()) {
            _uiMessage.value = "Tidak ada lagu di playlist ini"
            return
        }
        playerController.addToQueue(
            tracks = detail.tracks,
            sourceLabel = "Playing from ${detail.title}",
            resolveStream = { track -> repository.resolveAudioStream(track) },
            onTrackStarted = { startedTrack -> enrichTrackPlaybackContext(startedTrack) }
        )
        _uiMessage.value = "${detail.tracks.size} lagu dari \"${detail.title}\" ditambahkan ke antrean"
    }

    fun playTrackNext(track: MusicItem) {
        playerController.playNextInQueue(
            tracks = listOf(track),
            sourceLabel = playerController.playerState.value.playingFromSource,
            resolveStream = { t -> repository.resolveAudioStream(t) },
            onTrackStarted = { startedTrack -> enrichTrackPlaybackContext(startedTrack) }
        )
        _uiMessage.value = "\"${track.title}\" will play next"
    }

    fun addTrackToQueue(track: MusicItem) {
        playerController.addToQueue(
            tracks = listOf(track),
            sourceLabel = playerController.playerState.value.playingFromSource,
            resolveStream = { t -> repository.resolveAudioStream(t) },
            onTrackStarted = { startedTrack -> enrichTrackPlaybackContext(startedTrack) }
        )
        _uiMessage.value = "\"${track.title}\" added to queue"
    }

    fun setSleepTimerMinutes(minutes: Int, label: String = "${minutes}m") {
        playerController.setSleepTimerMinutes(minutes, label)
        _uiMessage.value = "Sleep timer set for $label"
    }

    fun setSleepTimerEndOfTrack() {
        playerController.setSleepTimerEndOfTrack()
        _uiMessage.value = "Sleep timer set for end of track"
    }

    fun cancelSleepTimer() {
        val wasActive = playerController.playerState.value.sleepTimerLabel != null ||
            playerController.playerState.value.sleepTimerEndOfTrack
        playerController.cancelSleepTimer()
        if (wasActive) {
            _uiMessage.value = "Sleep timer cancelled"
        }
    }

    fun addTrackToLibraryPlaylist(playlistId: String, track: MusicItem) {
        viewModelScope.launch {
            val target = libraryPlaylists.value.firstOrNull { it.id == playlistId }
            repository.addTrackToLibraryPlaylist(playlistId, track)
            _uiMessage.value = "Ditambahkan ke \"${target?.title ?: "Playlist"}\""
        }
    }

    fun removeTrackFromLibraryPlaylist(playlistId: String, trackId: String) {
        viewModelScope.launch {
            repository.removeTrackFromLibraryPlaylist(playlistId, trackId)
            _uiMessage.value = "Lagu dihapus dari playlist"
        }
    }

    fun toggleSavePlaylistDetailToLibrary(detail: PlaylistDetail) {
        viewModelScope.launch {
            val isAlreadySaved = savedPlaylistIds.value.contains(detail.id)
            if (isAlreadySaved) {
                repository.deleteLibraryPlaylist(detail.id)
                _uiMessage.value = "\"${detail.title}\" dihapus dari Library"
            } else {
                repository.savePlaylistToLibrary(
                    playlistDetail = detail,
                    sourceType = PlaylistSourceType.SAVED_COMMUNITY
                )
                _uiMessage.value = "\"${detail.title}\" disimpan ke Library Anda"
            }
        }
    }

    fun openLikedSongsPlaylist() {
        val favs = favoriteTracks.value
        _playlistScreenState.value = PlaylistScreenState(
            isVisible = true,
            isLoading = false,
            playlist = PlaylistDetail(
                id = LIKED_SONGS_PLAYLIST_ID,
                title = "Musik yang Disukai",
                subtitle = "${_username.value}, ${favs.size} lagu",
                authorSubtitle = "${_username.value}, ${favs.size} lagu",
                metaSubtitle = "PLAYLIST • ${favs.size} TRACKS",
                description = "Semua lagu yang Anda tandai suka tersimpan di sini.",
                thumbnailUrl = favs.firstOrNull()?.thumbnailUrl ?: "",
                isAlbum = false,
                totalTracks = favs.size,
                tracks = favs
            )
        )
    }

    fun openDownloadsPlaylist() {
        val items = downloadedTracks.value.map { it.toMusicItem() }
        _playlistScreenState.value = PlaylistScreenState(
            isVisible = true,
            isLoading = false,
            playlist = PlaylistDetail(
                id = DOWNLOADS_PLAYLIST_ID,
                title = "Downloads",
                subtitle = "${_username.value}, ${items.size} lagu",
                authorSubtitle = "${_username.value}, ${items.size} lagu",
                metaSubtitle = "PLAYLIST • ${items.size} TRACKS",
                description = "Lagu yang telah diunduh ke penyimpanan perangkat & siap diputar offline.",
                thumbnailUrl = items.firstOrNull()?.thumbnailUrl ?: "",
                isAlbum = false,
                totalTracks = items.size,
                tracks = items
            )
        )
    }

    fun refreshLocalDeviceMusic() {
        viewModelScope.launch {
            val granted = downloadManager.hasAudioStoragePermission()
            _hasAudioPermission.value = granted
            val scanned = if (granted) {
                downloadManager.scanLocalDeviceMusic()
            } else {
                emptyList()
            }
            _localDeviceTracks.value = scanned
            val current = _playlistScreenState.value
            if (current.isVisible && current.playlist?.id == LOCAL_MUSIC_PLAYLIST_ID) {
                _playlistScreenState.update {
                    it.copy(
                        isLoading = false,
                        playlist = it.playlist?.copy(
                            subtitle = "${_username.value}, ${scanned.size} lagu",
                            authorSubtitle = "${_username.value}, ${scanned.size} lagu",
                            metaSubtitle = "LOCAL STORAGE • ${scanned.size} TRACKS",
                            thumbnailUrl = scanned.firstOrNull()?.thumbnailUrl ?: "",
                            totalTracks = scanned.size,
                            tracks = scanned
                        )
                    )
                }
            }
        }
    }

    fun onAudioPermissionResult(granted: Boolean, openPlaylistAfterGrant: Boolean = true) {
        _hasAudioPermission.value = granted || downloadManager.hasAudioStoragePermission()
        if (_hasAudioPermission.value) {
            if (openPlaylistAfterGrant) {
                openLocalMusicPlaylist()
            } else {
                refreshLocalDeviceMusic()
            }
            _uiMessage.value = "Izin penyimpanan diberikan • Memindai musik lokal HP..."
        } else {
            _uiMessage.value = "Izin akses musik diperlukan untuk menampilkan lagu dari penyimpanan HP"
        }
    }

    fun setDownloadStorageLocation(location: com.example.player.DownloadStorageLocation) {
        viewModelScope.launch {
            val migratedCount = downloadManager.setStorageLocationAndMigrate(location)
            refreshLocalDeviceMusic()
            _uiMessage.value = if (migratedCount > 0) {
                "Lokasi unduhan diubah ke ${location.title} ($migratedCount lagu dipindahkan otomatis)"
            } else {
                "Lokasi unduhan diatur ke ${location.title}"
            }
        }
    }

    fun openLocalMusicPlaylist() {
        val cachedTracks = _localDeviceTracks.value
        // Open the Local Music screen immediately (0ms delay) with cached tracks
        _playlistScreenState.value = PlaylistScreenState(
            isVisible = true,
            isLoading = cachedTracks.isEmpty(),
            playlist = PlaylistDetail(
                id = LOCAL_MUSIC_PLAYLIST_ID,
                title = "Local Music",
                subtitle = "${_username.value}, ${cachedTracks.size} lagu",
                authorSubtitle = "${_username.value}, ${cachedTracks.size} lagu",
                metaSubtitle = "LOCAL STORAGE • ${cachedTracks.size} TRACKS",
                description = "Semua file audio yang dipindai otomatis dari penyimpanan HP Anda.",
                thumbnailUrl = cachedTracks.firstOrNull()?.thumbnailUrl ?: "",
                isAlbum = false,
                totalTracks = cachedTracks.size,
                tracks = cachedTracks
            )
        )
        // Refresh/scan asynchronously in background without blocking navigation
        refreshLocalDeviceMusic()
    }

    fun openLibraryPlaylist(userPlaylist: UserLibraryPlaylist) {
        val initialDetail = PlaylistDetail(
            id = userPlaylist.id,
            title = userPlaylist.title,
            subtitle = "${_username.value}, ${userPlaylist.tracks.size} lagu",
            authorSubtitle = "${_username.value}, ${userPlaylist.tracks.size} lagu",
            metaSubtitle = "PLAYLIST • ${userPlaylist.tracks.size} TRACKS",
            description = userPlaylist.description,
            thumbnailUrl = userPlaylist.thumbnailUrl.ifBlank {
                userPlaylist.tracks.firstOrNull()?.thumbnailUrl ?: ""
            },
            isAlbum = false,
            totalTracks = userPlaylist.tracks.size,
            tracks = userPlaylist.tracks
        )

        val needsRemoteFetch = userPlaylist.tracks.isEmpty() &&
            userPlaylist.sourceType != PlaylistSourceType.USER_CREATED

        _playlistScreenState.value = PlaylistScreenState(
            isVisible = true,
            isLoading = needsRemoteFetch,
            playlist = initialDetail
        )

        if (needsRemoteFetch) {
            viewModelScope.launch {
                val seed = userPlaylist.toMusicItem()
                val fetched = repository.fetchPlaylistOrAlbumDetail(seed)
                if (fetched.tracks.isNotEmpty()) {
                    val enriched = fetched.copy(
                        id = userPlaylist.id,
                        title = userPlaylist.title,
                        authorSubtitle = "${_username.value}, ${fetched.tracks.size} lagu",
                        metaSubtitle = "PLAYLIST • ${fetched.tracks.size} TRACKS",
                        totalTracks = fetched.tracks.size
                    )
                    repository.savePlaylistToLibrary(
                        playlistDetail = enriched,
                        sourceType = userPlaylist.sourceType,
                        platformBadge = userPlaylist.platformBadge
                    )
                    _playlistScreenState.update {
                        it.copy(
                            isLoading = false,
                            playlist = enriched
                        )
                    }
                } else {
                    _playlistScreenState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    // ========================================================================
    // OFFLINE MUSIC DOWNLOADER (SINGLE TRACK & FULL PLAYLIST/ALBUM)
    // ========================================================================
    fun downloadSingleTrack(track: MusicItem) {
        downloadManager.downloadTrack(
            track = track,
            onCompleted = { item ->
                if (item != null) {
                    _uiMessage.value = "\"${track.title}\" selesai diunduh untuk offline"
                    refreshLocalDeviceMusic()
                } else {
                    _uiMessage.value = "Gagal mengunduh \"${track.title}\""
                }
            }
        )
        _uiMessage.value = "Mengunduh \"${track.title}\" (lihat progres % di Notifikasi Status Bar)..."
    }

    fun downloadPlaylistOrAlbum(detail: PlaylistDetail) {
        if (detail.tracks.isEmpty()) {
            _uiMessage.value = "Tidak ada lagu untuk diunduh di daftar ini"
            return
        }
        downloadManager.downloadTracksBatch(
            collectionTitle = detail.title,
            tracks = detail.tracks,
            onBatchCompleted = { count ->
                _uiMessage.value = "$count lagu dari \"${detail.title}\" selesai diunduh"
                refreshLocalDeviceMusic()
            }
        )
        _uiMessage.value = "Mengunduh ${detail.tracks.size} lagu dari \"${detail.title}\" (lihat progres % di Status Bar)..."
    }

    fun downloadMusicItemCollection(collectionItem: MusicItem) {
        viewModelScope.launch {
            val existingLib = libraryPlaylists.value.firstOrNull { it.id == collectionItem.id }
            if (existingLib != null && existingLib.tracks.isNotEmpty()) {
                downloadManager.downloadTracksBatch(
                    collectionTitle = existingLib.title,
                    tracks = existingLib.tracks,
                    onBatchCompleted = { count ->
                        _uiMessage.value = "$count lagu dari \"${existingLib.title}\" selesai diunduh"
                        refreshLocalDeviceMusic()
                    }
                )
                _uiMessage.value = "Mengunduh ${existingLib.tracks.size} lagu dari \"${existingLib.title}\"..."
                return@launch
            }
            val detail = repository.fetchPlaylistOrAlbumDetail(collectionItem)
            downloadPlaylistOrAlbum(detail)
        }
    }

    fun deleteDownloadedTrack(trackId: String) {
        viewModelScope.launch {
            downloadManager.deleteDownloadedTrack(trackId)
            refreshLocalDeviceMusic()
            _uiMessage.value = "Unduhan offline dihapus"
        }
    }

    // ========================================================================
    // ARTIST PROFILE SCREEN NAVIGATION & SUBSCRIBE TO LIBRARY
    // ========================================================================
    fun openArtistProfile(seedItem: MusicItem) {
        val current = _artistScreenState.value.artistProfile
        if (_artistScreenState.value.isVisible && current != null &&
            !current.name.equals(seedItem.title, ignoreCase = true)
        ) {
            artistBackStack.addLast(current)
        }

        val rawName = if (seedItem.type == "artist" || seedItem.browseId?.startsWith("UC") == true) {
            seedItem.title
        } else {
            seedItem.cleanArtist.ifBlank { seedItem.title }
        }
        val instantFallback = CuratedHomeFallback.getArtistProfileFallback(rawName, seedItem)

        _artistScreenState.update {
            it.copy(
                isVisible = true,
                isLoading = true,
                artistProfile = instantFallback
            )
        }

        artistLoadJob?.cancel()
        artistLoadJob = viewModelScope.launch {
            val detail = repository.fetchArtistProfile(seedItem)
            if (detail.avatarUrl.isNotBlank()) {
                saveArtistAvatarToCache(detail.name, detail.avatarUrl)
                saveArtistAvatarToCache(rawName, detail.avatarUrl)
                val matchingSub = subscribedArtists.value.firstOrNull {
                    it.name.equals(detail.name, ignoreCase = true) ||
                        it.name.equals(rawName, ignoreCase = true) ||
                        it.browseId.equals(detail.browseId, ignoreCase = true)
                }
                if (matchingSub != null && matchingSub.avatarUrl != detail.avatarUrl) {
                    repository.upsertSubscribedArtist(matchingSub.copy(avatarUrl = detail.avatarUrl))
                }
            }
            detail.relatedArtists.forEach { rel ->
                if (rel.thumbnailUrl.isNotBlank()) {
                    saveArtistAvatarToCache(rel.title, rel.thumbnailUrl)
                }
            }
            _artistScreenState.update {
                it.copy(
                    isLoading = false,
                    artistProfile = detail
                )
            }
        }
    }

    fun openArtistProfileByName(artistName: String, thumbnailUrl: String = "") {
        val cleanName = MusicItem.sanitizeArtistName(artistName, isCollection = false)
            .substringBefore(",")
            .substringBefore("&")
            .trim()
            .ifBlank { artistName }
        val key = cleanName.lowercase()
        val cachedAvatar = _artistAvatarMap.value[key]
        val subAvatar = subscribedArtists.value
            .firstOrNull { it.name.equals(cleanName, ignoreCase = true) }
            ?.avatarUrl
            ?.takeIf { it.isNotBlank() && !it.contains("FRX282637095.webp") }
        val effectiveThumb = cachedAvatar
            ?: subAvatar
            ?: thumbnailUrl.takeIf { it.isNotBlank() && !it.contains("FRX282637095.webp") }
            ?: CuratedHomeFallback.getArtistAvatarForName(cleanName)
        val seed = MusicItem(
            id = "artist_${cleanName.lowercase()}",
            title = cleanName,
            subtitle = "Artis",
            thumbnailUrl = effectiveThumb,
            browseId = "UC_${cleanName.lowercase()}",
            type = "artist"
        )
        openArtistProfile(seed)
    }

    fun closeArtistProfile() {
        if (artistBackStack.isNotEmpty()) {
            val previous = artistBackStack.removeLast()
            _artistScreenState.update {
                it.copy(
                    isVisible = true,
                    isLoading = false,
                    artistProfile = previous
                )
            }
        } else {
            _artistScreenState.update {
                it.copy(isVisible = false, isLoading = false)
            }
        }
    }

    fun toggleFollowArtist(artistIdOrName: String) {
        val currentProfile = _artistScreenState.value.artistProfile
        val browseId = currentProfile?.browseId?.takeIf { it.isNotBlank() } ?: artistIdOrName
        val artistName = currentProfile?.name?.takeIf { it.isNotBlank() } ?: artistIdOrName
        val isCurrentlyFollowed = _artistScreenState.value.followedArtistIds.contains(browseId) ||
            _artistScreenState.value.followedArtistIds.contains(artistName) ||
            subscribedArtists.value.any {
                it.browseId.equals(browseId, ignoreCase = true) ||
                    it.name.equals(artistName, ignoreCase = true)
            }

        viewModelScope.launch {
            if (isCurrentlyFollowed) {
                repository.deleteSubscribedArtist(browseId)
                repository.deleteSubscribedArtist(artistName)
                _artistScreenState.update { state ->
                    state.copy(
                        followedArtistIds = state.followedArtistIds - setOf(browseId, artistName, "UC_${artistName.lowercase()}")
                    )
                }
                _uiMessage.value = "Berhenti subscribe $artistName (dihapus dari Library)"
            } else {
                val cachedPortrait = _artistAvatarMap.value[artistName.trim().lowercase()]
                val realAvatar = cachedPortrait
                    ?: currentProfile?.avatarUrl?.takeIf { it.isNotBlank() }
                    ?: currentProfile?.headerImageUrl?.takeIf { it.isNotBlank() }
                    ?: CuratedHomeFallback.getArtistAvatarForName(artistName)
                if (realAvatar.isNotBlank()) {
                    saveArtistAvatarToCache(artistName, realAvatar)
                }
                val item = SubscribedArtistItem(
                    id = browseId,
                    browseId = browseId,
                    name = artistName,
                    avatarUrl = realAvatar,
                    subscribersText = currentProfile?.subscribersText ?: "Artis"
                )
                repository.upsertSubscribedArtist(item)
                resolveArtistAvatarIfNeeded(artistName, browseId)
                _artistScreenState.update { state ->
                    state.copy(
                        followedArtistIds = state.followedArtistIds + setOf(browseId, artistName, "UC_${artistName.lowercase()}")
                    )
                }
                _uiMessage.value = "Berhasil subscribe $artistName! Tersimpan di seksi Artis pada Library"
            }
        }
    }

    // ========================================================================
    // SETTINGS & ACCOUNT INTEGRATIONS (YOUTUBE MUSIC GOOGLE & SPOTIFY)
    // ========================================================================
    fun openSettingsScreen() {
        _isSettingsVisible.value = true
    }

    fun closeSettingsScreen() {
        _isSettingsVisible.value = false
    }

    fun openAccountIntegrationsScreen() {
        _isAccountIntegrationsVisible.value = true
    }

    fun closeAccountIntegrationsScreen() {
        _isAccountIntegrationsVisible.value = false
    }

    private suspend fun persistImportedPlaylistsToLibrary(
        importedItems: List<MusicItem>,
        platformBadge: String
    ) {
        for (item in importedItems) {
            val tracks = if (item.previewTracks.isNotEmpty()) {
                item.previewTracks
            } else {
                repository.fetchPlaylistOrAlbumDetail(item).tracks
            }
            val detail = PlaylistDetail(
                id = item.id,
                title = item.title,
                subtitle = "${_username.value} • ${tracks.size} lagu",
                authorSubtitle = "${_username.value}, ${tracks.size} lagu",
                metaSubtitle = "PLAYLIST • ${tracks.size} TRACKS",
                description = "Diimpor dari $platformBadge ke Library Anda",
                thumbnailUrl = item.thumbnailUrl.ifBlank { tracks.firstOrNull()?.thumbnailUrl ?: "" },
                isAlbum = false,
                totalTracks = tracks.size,
                tracks = tracks
            )
            repository.savePlaylistToLibrary(
                playlistDetail = detail,
                sourceType = PlaylistSourceType.IMPORTED,
                platformBadge = platformBadge
            )
        }
    }

    fun connectSpotifyAccount(accountInput: String, tokenOrUrl: String) {
        viewModelScope.launch {
            _isIntegrationSyncing.value = true
            _integrationStatusMessage.value = null
            _spotifyImportProgress.value = SpotifyImportProgressState(
                isImporting = true,
                playlistTitle = "Menghubungkan Spotify...",
                statusText = "Memindai daftar lagu playlist..."
            )
            val result = repository.connectSpotifyAndImport(
                accountInput = accountInput,
                tokenOrUrl = tokenOrUrl,
                onProgress = { progress ->
                    _spotifyImportProgress.value = progress
                }
            )
            _spotifyAccount.value = result.account
            _importedPlaylists.update { current ->
                (result.importedPlaylists + current).distinctBy { it.id }
            }
            persistImportedPlaylistsToLibrary(result.importedPlaylists, "Spotify")
            _isIntegrationSyncing.value = false
            _integrationStatusMessage.value = "${result.statusMessage} Tersimpan di halaman Library."
        }
    }

    fun connectYouTubeMusicAccount(googleEmail: String, tokenOrUrl: String) {
        viewModelScope.launch {
            _isIntegrationSyncing.value = true
            _integrationStatusMessage.value = null
            _spotifyImportProgress.value = SpotifyImportProgressState(
                isImporting = true,
                playlistTitle = "Menghubungkan YouTube Music...",
                statusText = "Memindai daftar lagu playlist..."
            )
            val result = repository.connectYouTubeMusicAndImport(
                googleEmail = googleEmail,
                tokenOrUrl = tokenOrUrl,
                onProgress = { progress ->
                    _spotifyImportProgress.value = progress
                }
            )
            _youtubeMusicAccount.value = result.account
            _importedPlaylists.update { current ->
                (result.importedPlaylists + current).distinctBy { it.id }
            }
            persistImportedPlaylistsToLibrary(result.importedPlaylists, "YouTube Music")
            _isIntegrationSyncing.value = false
            _integrationStatusMessage.value = "${result.statusMessage} Tersimpan di halaman Library."
        }
    }

    fun importPlaylistByUrl(platform: IntegrationPlatform, urlOrId: String) {
        val clean = urlOrId.trim()
        if (clean.isBlank()) return
        viewModelScope.launch {
            _isIntegrationSyncing.value = true
            _integrationStatusMessage.value = null
            _spotifyImportProgress.value = SpotifyImportProgressState(
                isImporting = true,
                playlistTitle = "Memindai tautan ${platform.displayName}...",
                statusText = "Mengekstrak seluruh lagu dari playlist..."
            )
            val imported = repository.importPlaylistByUrl(
                platform = platform,
                urlOrId = clean,
                onProgress = { progress ->
                    _spotifyImportProgress.value = progress
                }
            )
            if (imported != null) {
                _importedPlaylists.update { current ->
                    (listOf(imported) + current).distinctBy { it.id }
                }
                persistImportedPlaylistsToLibrary(listOf(imported), platform.displayName)
                val finalProg = _spotifyImportProgress.value
                val summary = if (finalProg.totalTracks > 0) {
                    "Playlist \"${imported.title}\" selesai diimpor (${finalProg.successCount} berhasil, ${finalProg.failedCount} gagal)!"
                } else {
                    "Playlist \"${imported.title}\" (${imported.previewTracks.size} lagu) berhasil diimpor ke Library!"
                }
                _integrationStatusMessage.value = summary
                _uiMessage.value = summary
            } else {
                val errMsg = "Gagal mengimpor playlist dari tautan tersebut."
                _integrationStatusMessage.value = errMsg
                _uiMessage.value = errMsg
            }
            _isIntegrationSyncing.value = false
        }
    }

    fun disconnectIntegration(platform: IntegrationPlatform) {
        when (platform) {
            IntegrationPlatform.SPOTIFY -> {
                _spotifyAccount.value = ConnectedAccountState(platform = IntegrationPlatform.SPOTIFY)
                _integrationStatusMessage.value = "Koneksi akun Spotify telah diputuskan."
            }
            IntegrationPlatform.YOUTUBE_MUSIC -> {
                _youtubeMusicAccount.value = ConnectedAccountState(platform = IntegrationPlatform.YOUTUBE_MUSIC)
                _integrationStatusMessage.value = "Koneksi akun Google (YouTube Music) telah diputuskan."
            }
            else -> Unit
        }
    }

    // ========================================================================
    // SEARCH LOGIC (Live Suggestions + Full Categorized & Filtered Search)
    // ========================================================================
    fun onSearchQueryChange(newQuery: String) {
        _searchUiState.update {
            it.copy(
                query = newQuery,
                isSubmitted = false,
                isLoadingSuggestions = newQuery.isNotBlank()
            )
        }
        liveSuggestionJob?.cancel()
        if (newQuery.isBlank()) {
            _searchUiState.update {
                it.copy(
                    isLoadingSuggestions = false,
                    querySuggestions = emptyList(),
                    directSuggestionItems = emptyList()
                )
            }
            return
        }

        liveSuggestionJob = viewModelScope.launch {
            delay(220L)
            val payload = repository.fetchSearchSuggestions(newQuery)
            _searchUiState.update { current ->
                if (current.query == newQuery && !current.isSubmitted) {
                    current.copy(
                        isLoadingSuggestions = false,
                        querySuggestions = payload.querySuggestions,
                        directSuggestionItems = payload.directItems
                    )
                } else {
                    current
                }
            }
        }
    }

    fun onFillQueryFromSuggestion(suggestionText: String) {
        onSearchQueryChange(suggestionText)
    }

    fun onClearSearchQuery() {
        liveSuggestionJob?.cancel()
        fullSearchJob?.cancel()
        _searchUiState.update {
            it.copy(
                query = "",
                isSubmitted = false,
                selectedFilter = SearchFilter.ALL,
                isLoadingSuggestions = false,
                isLoadingResults = false,
                querySuggestions = emptyList(),
                directSuggestionItems = emptyList(),
                allCategorySections = emptyList(),
                filteredItems = emptyList()
            )
        }
    }

    fun onSubmitSearch(submittedQuery: String = _searchUiState.value.query) {
        val clean = submittedQuery.trim()
        if (clean.isBlank()) return

        liveSuggestionJob?.cancel()
        fullSearchJob?.cancel()

        _searchUiState.update { state ->
            val updatedRecents = (listOf(clean) + state.recentSearches.filterNot { it.equals(clean, ignoreCase = true) }).take(8)
            state.copy(
                query = clean,
                isSubmitted = true,
                selectedFilter = SearchFilter.ALL,
                isLoadingSuggestions = false,
                isLoadingResults = true,
                recentSearches = updatedRecents
            )
        }

        fullSearchJob = viewModelScope.launch {
            val sections = repository.searchAllCategories(clean)
            _searchUiState.update { current ->
                current.copy(
                    isLoadingResults = false,
                    allCategorySections = sections
                )
            }
        }
    }

    fun onSelectSearchFilter(filter: SearchFilter) {
        val currentQuery = _searchUiState.value.query.trim()
        if (currentQuery.isBlank()) return

        _searchUiState.update {
            it.copy(
                selectedFilter = filter,
                isSubmitted = true,
                isLoadingResults = true
            )
        }

        fullSearchJob?.cancel()
        fullSearchJob = viewModelScope.launch {
            if (filter == SearchFilter.ALL) {
                val sections = repository.searchAllCategories(currentQuery)
                _searchUiState.update {
                    it.copy(
                        isLoadingResults = false,
                        allCategorySections = sections
                    )
                }
            } else {
                val apiKey = filter.apiFilterKey ?: "songs"
                val items = repository.searchByFilter(currentQuery, filter = apiKey)
                _searchUiState.update {
                    it.copy(
                        isLoadingResults = false,
                        filteredItems = items
                    )
                }
            }
        }
    }

    fun onSearchItemClicked(item: MusicItem, contextItems: List<MusicItem> = emptyList()) {
        if (item.type == "artist" || item.browseId?.startsWith("UC") == true) {
            openArtistProfile(item)
        } else if (item.type == "playlist" || item.type == "album") {
            openPlaylistOrAlbumDetail(item)
        } else {
            val playableQueue = contextItems.filter { !it.isCollection && it.type != "artist" }
                .ifEmpty { listOf(item) }
            playSingleOrQueueTrack(
                item = item,
                contextQueue = playableQueue,
                sourceLabel = "Playing from Search"
            )
        }
    }

    // ========================================================================
    // COMMUNITY PLAYLIST CARD ("Dari komunitas") ACTIONS
    // ========================================================================
    fun onPlayCommunityPlaylistFirstTrack(playlistItem: MusicItem) {
        val preview = playlistItem.previewTracks
        if (preview.isNotEmpty()) {
            playSingleOrQueueTrack(
                item = preview.first(),
                contextQueue = preview,
                sourceLabel = "Playing from ${playlistItem.title}"
            )
        }
        viewModelScope.launch {
            val detail = repository.fetchPlaylistOrAlbumDetail(playlistItem)
            if (detail.tracks.isNotEmpty()) {
                val firstTrack = detail.tracks.first()
                playSingleOrQueueTrack(
                    item = firstTrack,
                    contextQueue = detail.tracks,
                    sourceLabel = "Playing from ${detail.title}"
                )
            }
        }
    }

    fun onStartCommunityPlaylistRadio(playlistItem: MusicItem) {
        viewModelScope.launch {
            val detail = repository.fetchPlaylistOrAlbumDetail(playlistItem)
            val tracks = detail.tracks.ifEmpty { playlistItem.previewTracks }
            if (tracks.isNotEmpty()) {
                val shuffled = tracks.shuffled()
                playSingleOrQueueTrack(
                    item = shuffled.first(),
                    contextQueue = shuffled,
                    sourceLabel = "Radio • ${detail.title}"
                )
            }
        }
    }

    fun onToggleSaveCommunityPlaylist(playlistItem: MusicItem) {
        viewModelScope.launch {
            val isSaved = savedPlaylistIds.value.contains(playlistItem.id)
            if (isSaved) {
                repository.deleteLibraryPlaylist(playlistItem.id)
                _uiMessage.value = "\"${playlistItem.title}\" dihapus dari Library"
            } else {
                val tracks = if (playlistItem.previewTracks.isNotEmpty()) {
                    playlistItem.previewTracks
                } else {
                    repository.fetchPlaylistOrAlbumDetail(playlistItem).tracks
                }
                val detail = PlaylistDetail(
                    id = playlistItem.id,
                    title = playlistItem.title,
                    subtitle = "${_username.value} • ${tracks.size} lagu",
                    authorSubtitle = "${_username.value}, ${tracks.size} lagu",
                    metaSubtitle = "PLAYLIST • ${tracks.size} TRACKS",
                    description = "Disimpan ke Library Anda",
                    thumbnailUrl = playlistItem.thumbnailUrl,
                    isAlbum = playlistItem.type == "album",
                    totalTracks = tracks.size,
                    tracks = tracks
                )
                repository.savePlaylistToLibrary(
                    playlistDetail = detail,
                    sourceType = PlaylistSourceType.SAVED_COMMUNITY
                )
                _uiMessage.value = "\"${playlistItem.title}\" disimpan ke Library"
            }
        }
    }

    // ========================================================================
    // PLAYER, VIDEO SWITCHER & LYRICS PROVIDER LOGIC
    // ========================================================================
    fun openFullPlayer(initialSubView: PlayerSubView = PlayerSubView.MAIN) {
        _activePlayerSubView.value = initialSubView
        _isPlayerExpanded.value = true
        playerController.setFullPlayerExpanded(true)
    }

    fun closeFullPlayer() {
        _isPlayerExpanded.value = false
        _activePlayerSubView.value = PlayerSubView.MAIN
        playerController.setFullPlayerExpanded(false)
    }

    fun setPlayerSubView(subView: PlayerSubView) {
        if (_activePlayerSubView.value == subView && subView != PlayerSubView.MAIN) {
            _activePlayerSubView.value = PlayerSubView.MAIN
        } else {
            _activePlayerSubView.value = subView
        }
    }

    fun toggleVideoPlayerMode() {
        val nextMode = !_isVideoModeActive.value
        _isVideoModeActive.value = nextMode
        if (nextMode) {
            _activePlayerSubView.value = PlayerSubView.MAIN
            val currentTrack = playerController.playerState.value.currentTrack
            if (currentTrack != null) {
                _isResolvingVideoStream.value = true
                viewModelScope.launch {
                    val streamUrl = repository.resolveVideoStreamUrl(currentTrack)
                    _activeVideoStreamUrl.value = streamUrl
                    _isResolvingVideoStream.value = false
                }
                _uiMessage.value = "Beralih ke Mode Video Musik"
            }
        } else {
            _uiMessage.value = "Kembali ke Tampilan Sampul Musik"
        }
    }

    fun disableVideoPlayerMode() {
        if (_isVideoModeActive.value) {
            _isVideoModeActive.value = false
        }
    }

    fun selectLyricsProvider(provider: LyricsProvider) {
        _selectedLyricsProvider.value = provider
        val targetSource = if (provider == LyricsProvider.YOUTUBE_MUSIC) {
            LyricsSource.YOUTUBE_MUSIC
        } else {
            LyricsSource.LRC_RED
        }
        selectBitChordLyricsSource(targetSource)
    }

    fun selectBitChordLyricsSource(source: LyricsSource) {
        val activeTrack = playerController.playerState.value.currentTrack ?: return
        _activeLyricsSource.value = source
        _isEmbeddedLyrics.value = false
        _selectedLyricsProvider.value = if (source == LyricsSource.YOUTUBE_MUSIC || source == LyricsSource.YOUTUBE_TRANSCRIPT) {
            LyricsProvider.YOUTUBE_MUSIC
        } else {
            LyricsProvider.LRC_RED
        }

        // Check if already fetched in memory for this track
        val existingLines = _lyricsProviderResults.value[source]
        if (!existingLines.isNullOrEmpty()) {
            applyResolvedBitChordLyrics(
                track = activeTrack,
                lines = existingLines,
                winningSource = source,
                isEmbedded = false
            )
            _uiMessage.value = "Sumber lirik: ${source.displayName}"
            return
        }

        // Fetch on-demand from the selected provider
        lyricsFetchJob?.cancel()
        lyricsFetchJob = viewModelScope.launch {
            _isLoadingLyrics.value = true
            _lyricsProviderStates.update { it + (source to LyricsProviderState.FETCHING) }
            val settings = lyricsSettingsStore.state.value
            val durationSec = parseTrackDurationSeconds(activeTrack.duration, playerController.playerState.value.durationSec)
            val fetched = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                LyricsRepository.fetchSingleSource(
                    source = source,
                    videoId = activeTrack.videoId ?: activeTrack.id,
                    title = activeTrack.title,
                    artist = activeTrack.cleanArtist,
                    album = null,
                    durationMs = durationSec * 1000L,
                    paxSenixApiKey = settings.paxSenixApiKey
                )
            }
            _isLoadingLyrics.value = false
            if (!fetched.isNullOrEmpty()) {
                _lyricsProviderStates.update { it + (source to LyricsProviderState.FOUND) }
                _lyricsProviderResults.update { it + (source to fetched) }
                applyResolvedBitChordLyrics(
                    track = activeTrack,
                    lines = fetched,
                    winningSource = source,
                    isEmbedded = false
                )
                _uiMessage.value = "Lirik dimuat dari ${source.displayName}"
            } else {
                _lyricsProviderStates.update { it + (source to LyricsProviderState.NOT_FOUND) }
                _uiMessage.value = "${source.displayName} tidak memiliki lirik untuk lagu ini"
            }
        }
    }

    fun toggleLyricsSourceEnabled(source: LyricsSource) {
        lyricsSettingsStore.toggleSourceEnabled(source)
        val activeTrack = playerController.playerState.value.currentTrack
        if (activeTrack != null) {
            fetchBitChordLyricsForTrack(activeTrack)
        }
    }

    fun moveLyricsSourceOrder(fromIndex: Int, toIndex: Int) {
        val currentOrder = lyricsSettingsStore.state.value.sourceOrder.toMutableList()
        if (fromIndex !in currentOrder.indices || toIndex !in currentOrder.indices) return
        val moved = currentOrder.removeAt(fromIndex)
        currentOrder.add(toIndex, moved)
        lyricsSettingsStore.setSourceOrder(currentOrder)
        val activeTrack = playerController.playerState.value.currentTrack
        if (activeTrack != null) {
            fetchBitChordLyricsForTrack(activeTrack)
        }
    }

    fun setPrioritizeSyllableSync(enabled: Boolean) {
        lyricsSettingsStore.setPrioritizeSyllableSync(enabled)
        val activeTrack = playerController.playerState.value.currentTrack
        if (activeTrack != null) {
            fetchBitChordLyricsForTrack(activeTrack)
        }
    }

    fun savePaxSenixApiKey(key: String) {
        lyricsSettingsStore.setPaxSenixApiKey(key)
        _uiMessage.value = if (key.isBlank()) "PaxSenix API Key dihapus" else "PaxSenix API Key disimpan"
        val activeTrack = playerController.playerState.value.currentTrack
        if (activeTrack != null) {
            fetchBitChordLyricsForTrack(activeTrack)
        }
    }

    fun resetLyricsSourcesToDefault() {
        lyricsSettingsStore.resetSourcesToDefault()
        _uiMessage.value = "Urutan sumber lirik dikembalikan ke default"
        val activeTrack = playerController.playerState.value.currentTrack
        if (activeTrack != null) {
            fetchBitChordLyricsForTrack(activeTrack)
        }
    }

    fun setCurrentTrackLyricsOffsetMs(offsetMs: Int) {
        val trackId = playerController.playerState.value.currentTrack?.id
        lyricsSettingsStore.setTrackOffsetMs(trackId, offsetMs)
    }

    fun setSyncedLyricsEnabled(enabled: Boolean) {
        lyricsSettingsStore.setSyncedLyrics(enabled)
    }

    fun setLyricsBlurEnabled(enabled: Boolean) {
        lyricsSettingsStore.setLyricsBlur(enabled)
    }

    fun setReduceLyricsAnimation(enabled: Boolean) {
        lyricsSettingsStore.setReduceAnimation(enabled)
    }

    fun toggleLyricsTranslation() {
        val next = !_isTranslationEnabled.value
        _isTranslationEnabled.value = next
        if (next) {
            refreshTranslationAndRomanization()
        }
    }

    fun toggleLyricsRomanization() {
        val next = !_isRomanizationEnabled.value
        _isRomanizationEnabled.value = next
        if (next) {
            refreshTranslationAndRomanization()
        }
    }

    fun setLyricsTranslationLanguage(langCode: String) {
        lyricsSettingsStore.setTranslationLanguage(langCode)
        _translatedLyrics.value = null
        if (!_isTranslationEnabled.value) {
            _isTranslationEnabled.value = true
        }
        refreshTranslationAndRomanization()
    }

    private fun refreshTranslationAndRomanization() {
        val lines = _bitChordLyricsLines.value
        if (lines.isEmpty()) return
        val wantTrans = _isTranslationEnabled.value
        val wantRom = _isRomanizationEnabled.value
        if (!wantTrans && !wantRom) return

        translationJob?.cancel()
        translationJob = viewModelScope.launch {
            _isTranslatingLyrics.value = true
            val lang = lyricsSettingsStore.state.value.translationLanguage
            if (wantTrans && _translatedLyrics.value == null) {
                val trans = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    LyricsTranslation.translate(lines, lang)
                }
                _translatedLyrics.value = trans
            }
            if (wantRom && _romanizedLyrics.value == null) {
                val rom = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    LyricsTranslation.romanize(lines)
                }
                _romanizedLyrics.value = rom
            }
            _isTranslatingLyrics.value = false
        }
    }

    private fun parseTrackDurationSeconds(durationStr: String?, fallbackSec: Int): Int {
        if (!durationStr.isNullOrBlank()) {
            val parts = durationStr.trim().split(":").mapNotNull { it.trim().toIntOrNull() }
            when (parts.size) {
                2 -> return (parts[0] * 60 + parts[1]).coerceAtLeast(1)
                3 -> return (parts[0] * 3600 + parts[1] * 60 + parts[2]).coerceAtLeast(1)
            }
        }
        return fallbackSec.takeIf { it > 0 } ?: 210
    }

    private fun applyResolvedBitChordLyrics(
        track: MusicItem,
        lines: List<BitChordLyricLine>,
        winningSource: LyricsSource?,
        isEmbedded: Boolean,
        fallbackData: LyricsData? = _currentLyrics.value
    ) {
        _bitChordLyricsLines.value = lines
        _activeLyricsSource.value = winningSource
        _isEmbeddedLyrics.value = isEmbedded
        _translatedLyrics.value = null
        _romanizedLyrics.value = null

        _currentLyrics.value = LyricsData.fromBitChord(
            videoId = track.videoId ?: track.id,
            bitChordLines = lines,
            winningSource = winningSource,
            isEmbedded = isEmbedded,
            fallbackLyricsData = fallbackData
        )

        if (_isTranslationEnabled.value || _isRomanizationEnabled.value) {
            refreshTranslationAndRomanization()
        }
    }

    private fun fetchBitChordLyricsForTrack(item: MusicItem) {
        lyricsFetchJob?.cancel()
        lyricsFetchJob = viewModelScope.launch {
            _isLoadingLyrics.value = true
            _lyricsProviderStates.value = emptyMap()
            _lyricsProviderResults.value = emptyMap()
            _bitChordLyricsLines.value = emptyList()
            _activeLyricsSource.value = null
            _isEmbeddedLyrics.value = false
            _translatedLyrics.value = null
            _romanizedLyrics.value = null

            val settings = lyricsSettingsStore.state.value
            val durationSec = parseTrackDurationSeconds(item.duration, playerController.playerState.value.durationSec)
            val offlineLrcFile = downloadManager.getOfflineLyricsFile(item.videoId ?: item.id)
                ?: downloadManager.getOfflineLyricsFile(item.id)
            val offlineAudioFile = downloadManager.getOfflineFileIfExists(item.videoId ?: item.id)
                ?: downloadManager.getOfflineFileIfExists(item.id)
            val localPath = item.localFilePath?.removePrefix("file://")
                ?: offlineAudioFile?.absolutePath
                ?: offlineLrcFile?.absolutePath

            // 1. Check offline sidecar .lrc in offline_lyrics first if present
            if (offlineLrcFile != null && offlineLrcFile.exists()) {
                val sidecarLines = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        com.example.data.lyrics.ProviderLyrics.parse(offlineLrcFile.readText())
                    } catch (_: Exception) {
                        null
                    }
                }
                if (!sidecarLines.isNullOrEmpty()) {
                    _isLoadingLyrics.value = false
                    applyResolvedBitChordLyrics(
                        track = item,
                        lines = sidecarLines,
                        winningSource = null,
                        isEmbedded = true
                    )
                    return@launch
                }
            }

            // 2. Also fetch legacy LyricsData in parallel for HD cover metadata (hdCoverUrl / accentColorHex)
            val legacyDeferred = async(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    repository.getTrackLyrics(item, preferredProvider = _selectedLyricsProvider.value)
                } catch (_: Exception) {
                    null
                }
            }

            val (winningSrc, lines) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                LyricsRepository.getLyrics(
                    context = getApplication(),
                    videoId = item.videoId ?: item.id,
                    title = item.title,
                    artist = item.cleanArtist,
                    album = null,
                    durationMs = durationSec * 1000L,
                    localFilePathOrUri = localPath,
                    enabledSources = settings.enabledSources,
                    sourceOrder = settings.sourceOrder,
                    prioritizeSyllableSync = settings.prioritizeSyllableSync,
                    paxSenixApiKey = settings.paxSenixApiKey,
                    onSourceStarted = { src ->
                        _lyricsProviderStates.update { it + (src to LyricsProviderState.FETCHING) }
                    },
                    onSourceResult = { src, resLines ->
                        if (!resLines.isNullOrEmpty()) {
                            _lyricsProviderStates.update { it + (src to LyricsProviderState.FOUND) }
                            _lyricsProviderResults.update { it + (src to resLines) }
                        } else {
                            _lyricsProviderStates.update { it + (src to LyricsProviderState.NOT_FOUND) }
                        }
                    },
                    onSourceCancelled = { src ->
                        _lyricsProviderStates.update { it + (src to LyricsProviderState.NOT_FETCHED) }
                    }
                )
            }

            val embeddedFlag = winningSrc == null && lines.isNotEmpty()
            val legacyData = legacyDeferred.await()
            _isLoadingLyrics.value = false

            if (lines.isNotEmpty()) {
                applyResolvedBitChordLyrics(
                    track = item,
                    lines = lines,
                    winningSource = winningSrc,
                    isEmbedded = embeddedFlag,
                    fallbackData = legacyData
                )
            } else if (legacyData != null) {
                // Convert fallback lines into BitChordLyricLine so UI always renders cleanly
                val converted = if (legacyData.syncedLines.isNotEmpty()) {
                    legacyData.syncedLines.map { l ->
                        BitChordLyricLine(
                            timeMs = ((l.beginSec ?: 0.0) * 1000.0).toLong(),
                            text = l.text,
                            words = l.words.map { w ->
                                com.example.data.lyrics.LyricWord(
                                    startMs = (w.beginSec * 1000.0).toLong(),
                                    endMs = (w.endSec * 1000.0).toLong(),
                                    text = w.text
                                )
                            }
                        )
                    }
                } else {
                    legacyData.lines.map { text ->
                        BitChordLyricLine(timeMs = 0L, text = text)
                    }
                }
                applyResolvedBitChordLyrics(
                    track = item,
                    lines = converted,
                    winningSource = LyricsSource.LRC_RED,
                    isEmbedded = false,
                    fallbackData = legacyData
                )
            }
        }
    }

    fun applyLrcHdCover(track: MusicItem, hdCoverUrl: String) {
        if (hdCoverUrl.isBlank()) return
        _customCoverOverrides.update { current ->
            current + (track.id to hdCoverUrl)
        }
    }

    fun restoreDefaultCover(track: MusicItem) {
        _customCoverOverrides.update { current ->
            current - track.id
        }
    }

    fun onHomeItemClicked(item: MusicItem) {
        if (item.type == "artist" || item.browseId?.startsWith("UC") == true) {
            openArtistProfile(item)
            return
        }
        if (item.type == "daily_recommendation_track") {
            val dailySection = _scrapedSections.value.firstOrNull {
                it.title.contains("rekomendasi harian", ignoreCase = true)
            }
            val queue = dailySection?.items?.takeIf { it.isNotEmpty() } ?: listOf(item)
            playSingleOrQueueTrack(
                item = item,
                contextQueue = queue,
                sourceLabel = "Playing from Rekomendasi harian Anda"
            )
            return
        }

        if (item.isCollection) {
            openPlaylistOrAlbumDetail(item)
        } else {
            val parentSection = _scrapedSections.value.firstOrNull { sec ->
                sec.items.any { it.id == item.id }
            }
            val sourceName = parentSection?.title ?: "Pilihan cepat"
            val sectionPlayableQueue = parentSection?.items
                ?.filter { !it.isCollection }
                ?.takeIf { it.isNotEmpty() }
                ?: uiState.value.topShelfItems
            playSingleOrQueueTrack(
                item = item,
                contextQueue = sectionPlayableQueue,
                sourceLabel = "Playing from $sourceName"
            )
        }
    }

    fun onPlaySectionAll(sectionTitle: String, items: List<MusicItem>) {
        val playable = items.filter { !it.isCollection }
        val firstTrack = playable.firstOrNull() ?: items.firstOrNull() ?: return
        if (firstTrack.isCollection && playable.isEmpty()) {
            openPlaylistOrAlbumDetail(firstTrack)
        } else {
            playSingleOrQueueTrack(
                item = firstTrack,
                contextQueue = playable.ifEmpty { listOf(firstTrack) },
                sourceLabel = "Playing from $sectionTitle"
            )
        }
    }

    fun openPlaylistOrAlbumDetail(seedItem: MusicItem) {
        val existingUserPlaylist = libraryPlaylists.value.firstOrNull { it.id == seedItem.id }
        if (existingUserPlaylist != null) {
            openLibraryPlaylist(existingUserPlaylist)
            return
        }

        val fallbackDetail = CuratedHomeFallback.getPlaylistFallback(seedItem)
        _playlistScreenState.value = PlaylistScreenState(
            isVisible = true,
            isLoading = true,
            playlist = fallbackDetail
        )
        viewModelScope.launch {
            val detail = repository.fetchPlaylistOrAlbumDetail(seedItem)
            _playlistScreenState.update { state ->
                state.copy(
                    isLoading = false,
                    playlist = detail
                )
            }
        }
    }

    fun openPlaylistByCustomBrowseId(browseIdInput: String) {
        val cleanId = browseIdInput.trim()
        if (cleanId.isBlank()) return
        val normalizedId = if (cleanId.startsWith("PL")) "VL$cleanId" else cleanId
        val seed = MusicItem(
            id = normalizedId,
            title = "Memuat Playlist...",
            subtitle = "YouTube Music",
            thumbnailUrl = "",
            browseId = normalizedId,
            type = if (normalizedId.startsWith("MPREb_")) "album" else "playlist"
        )
        openPlaylistOrAlbumDetail(seed)
    }

    fun closePlaylistDetail() {
        _playlistScreenState.update { it.copy(isVisible = false, isSearchVisible = false, searchQuery = "") }
    }

    fun onTogglePlaylistSearch() {
        _playlistScreenState.update {
            val nextVisible = !it.isSearchVisible
            it.copy(
                isSearchVisible = nextVisible,
                searchQuery = if (nextVisible) it.searchQuery else ""
            )
        }
    }

    fun onUpdatePlaylistSearchQuery(query: String) {
        _playlistScreenState.update { it.copy(searchQuery = query) }
    }

    fun onTogglePlaylistSort() {
        _playlistScreenState.update { it.copy(sortAscending = !it.sortAscending) }
    }

    fun onPlayAllFromPlaylist() {
        val playlist = _playlistScreenState.value.playlist ?: return
        val firstTrack = playlist.tracks.firstOrNull() ?: return
        playSingleOrQueueTrack(
            item = firstTrack,
            contextQueue = playlist.tracks,
            sourceLabel = "Playing from ${playlist.title}"
        )
    }

    fun onShufflePlayPlaylist() {
        val playlist = _playlistScreenState.value.playlist ?: return
        if (playlist.tracks.isEmpty()) return
        val shuffled = playlist.tracks.shuffled()
        playSingleOrQueueTrack(
            item = shuffled.first(),
            contextQueue = shuffled,
            sourceLabel = "Playing from ${playlist.title}"
        )
    }

    fun playSingleOrQueueTrack(
        item: MusicItem,
        contextQueue: List<MusicItem> = emptyList(),
        sourceLabel: String? = null
    ) {
        val effectiveSource = sourceLabel ?: playerController.playerState.value.playingFromSource
        val effectiveQueue = if (contextQueue.isNotEmpty()) contextQueue else listOf(item)

        playerController.playTrackWithQueue(
            track = item,
            queue = effectiveQueue,
            sourceLabel = effectiveSource,
            resolveStream = { track -> repository.resolveAudioStream(track) },
            onTrackStarted = { startedTrack ->
                enrichTrackPlaybackContext(startedTrack)
            }
        )
    }

    private fun enrichTrackPlaybackContext(item: MusicItem) {
        _currentLyrics.value = null
        // Default to cover image first when a track/video starts; user can switch to video mode via 3-dots menu
        _isVideoModeActive.value = false
        _activeVideoStreamUrl.value = null

        // Launch full BitChord Multi-Provider Lyrics Engine race + Embedded Lyrics reader
        fetchBitChordLyricsForTrack(item)

        trackEnrichmentJob?.cancel()
        trackEnrichmentJob = viewModelScope.launch {
            launch {
                repository.recordPlayedTrack(item)
                resolveArtistAvatarIfNeeded(item.cleanArtist, item.browseId)
            }

            val fetchedQueue = repository.getTrackQueue(item)
            if (fetchedQueue.isNotEmpty()) {
                playerController.setAutoPlayQueue(fetchedQueue)
            }
        }
    }

    fun onSelectTrackFromQueue(item: MusicItem) {
        playerController.playTrackFromQueueOrAutoPlay(item)
    }

    fun onRemoveTrackFromQueue(item: MusicItem) {
        playerController.removeTrackFromQueue(item.id)
    }

    fun toggleFavoriteTrack(item: MusicItem) {
        viewModelScope.launch {
            val wasFav = favoriteIds.value.contains(item.id) ||
                (item.videoId != null && favoriteIds.value.contains(item.videoId))
            repository.toggleFavoriteTrack(item)
            _uiMessage.value = if (wasFav) {
                "Dihapus dari Musik yang Disukai"
            } else {
                "Ditambahkan ke Musik yang Disukai"
            }
        }
    }

    fun onOpenTrackMenu(item: MusicItem) {
        _selectedTrackForMenu.value = item
    }

    fun onDismissTrackMenu() {
        _selectedTrackForMenu.value = null
    }

    fun onRemoveFromRecents(item: MusicItem) {
        viewModelScope.launch {
            repository.removeRecentTrack(item.id)
            _selectedTrackForMenu.value = null
        }
    }

    fun onClearAllRecents() {
        viewModelScope.launch {
            repository.clearAllRecents()
        }
    }

    fun onTogglePlayPause() {
        playerController.togglePlayPause()
    }

    fun onNextTrack() {
        playerController.playNext()
    }

    fun onPreviousTrack() {
        playerController.playPrevious()
    }

    fun onSeekTo(progress: Float) {
        playerController.seekToFraction(progress)
    }

    fun onVolumeChange(volume: Float) {
        playerController.setVolume(volume)
    }

    fun onVolumeStep(raise: Boolean) {
        playerController.adjustDeviceVolumeStep(raise)
    }

    fun onSyncDeviceVolume() {
        playerController.syncVolumeFromDevice()
    }

    fun onToggleShuffle() {
        playerController.toggleShuffle()
    }

    fun onToggleRepeat() {
        playerController.toggleRepeat()
    }

    fun onToggleAutoPlay() {
        playerController.toggleAutoPlay()
    }

    override fun onCleared() {
        super.onCleared()
    }

    companion object {
        const val LIKED_SONGS_PLAYLIST_ID = "liked_songs_playlist"
        const val DOWNLOADS_PLAYLIST_ID = "offline_downloads_playlist"
        const val LOCAL_MUSIC_PLAYLIST_ID = "local_device_music"
    }
}
