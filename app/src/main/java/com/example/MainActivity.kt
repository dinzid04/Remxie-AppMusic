package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.lyrics.LyricLine as BitChordLyricLine
import com.example.data.lyrics.LyricsProviderState
import com.example.data.lyrics.LyricsSettingsState
import com.example.data.lyrics.LyricsSource
import com.example.data.model.ActiveDownloadProgress
import com.example.data.model.LyricsData
import com.example.data.model.LyricsProvider
import com.example.data.model.MusicItem
import com.example.data.model.PlaylistPrivacy
import com.example.data.model.UserLibraryPlaylist
import com.example.player.PlayerUiState
import com.example.ui.components.BitChordTab
import com.example.ui.components.FloatingMiniPlayer
import com.example.ui.components.FloatingPillBottomBar
import com.example.ui.components.FullMusicPlayerScreen
import com.example.ui.home.ListenNowScreen
import com.example.ui.home.MusicHomeViewModel
import com.example.ui.home.PlayerSubView
import com.example.ui.library.LibraryScreen
import com.example.ui.playlist.PlaylistDetailScreen
import com.example.ui.screens.AccountIntegrationsScreen
import com.example.ui.screens.ArtistProfileScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.search.SearchScreen
import com.example.ui.theme.BitChordTheme
import com.example.ui.theme.PlusJakartaSans
import com.example.ui.theme.SurfaceObsidian
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BitChordTheme {
                BitChordApp()
            }
        }
    }
}

@Composable
fun BitChordApp(
    homeViewModel: MusicHomeViewModel = viewModel()
) {
    val context = LocalContext.current

    // Runtime permission launcher for Android 13+ Notification progress bar during music downloads
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    // Proactively request POST_NOTIFICATIONS permission on Android 13+ so status bar download % works immediately
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val ensureNotificationPermissionAndRun: (() -> Unit) -> Unit = remember(context) {
        { action ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val granted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
                if (!granted) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
            action()
        }
    }

    val homeState by homeViewModel.uiState.collectAsStateWithLifecycle()
    val playlistScreenState by homeViewModel.playlistScreenState.collectAsStateWithLifecycle()
    val artistScreenState by homeViewModel.artistScreenState.collectAsStateWithLifecycle()
    val isSettingsVisible by homeViewModel.isSettingsVisible.collectAsStateWithLifecycle()
    val isAccountIntegrationsVisible by homeViewModel.isAccountIntegrationsVisible.collectAsStateWithLifecycle()
    val spotifyAccount by homeViewModel.spotifyAccount.collectAsStateWithLifecycle()
    val youtubeMusicAccount by homeViewModel.youtubeMusicAccount.collectAsStateWithLifecycle()
    val isIntegrationSyncing by homeViewModel.isIntegrationSyncing.collectAsStateWithLifecycle()
    val spotifyImportProgress by homeViewModel.spotifyImportProgress.collectAsStateWithLifecycle()
    val integrationStatusMessage by homeViewModel.integrationStatusMessage.collectAsStateWithLifecycle()

    val searchUiState by homeViewModel.searchUiState.collectAsStateWithLifecycle()
    val hasActiveTrack by homeViewModel.hasActiveTrack.collectAsStateWithLifecycle()
    val currentPlayingTrackId by homeViewModel.currentPlayingTrackId.collectAsStateWithLifecycle()
    val isPlayingAudio by homeViewModel.isPlayingAudio.collectAsStateWithLifecycle()
    val favoriteIds by homeViewModel.favoriteIds.collectAsStateWithLifecycle()
    val favoriteTracks by homeViewModel.favoriteTracks.collectAsStateWithLifecycle()
    val recentTracks by homeViewModel.recentTracks.collectAsStateWithLifecycle()
    val artistAvatarMap by homeViewModel.artistAvatarMap.collectAsStateWithLifecycle()
    val libraryPlaylists by homeViewModel.libraryPlaylists.collectAsStateWithLifecycle()
    val subscribedArtists by homeViewModel.subscribedArtists.collectAsStateWithLifecycle()
    val savedPlaylistIds by homeViewModel.savedPlaylistIds.collectAsStateWithLifecycle()
    val downloadedTracks by homeViewModel.downloadedTracks.collectAsStateWithLifecycle()
    val downloadedTrackIds by homeViewModel.downloadedTrackIds.collectAsStateWithLifecycle()
    val activeDownloads by homeViewModel.activeDownloads.collectAsStateWithLifecycle()
    val downloadStorageLocation by homeViewModel.downloadStorageLocation.collectAsStateWithLifecycle()
    val hasAudioPermission by homeViewModel.hasAudioPermission.collectAsStateWithLifecycle()
    val localDeviceTracks by homeViewModel.localDeviceTracks.collectAsStateWithLifecycle()
    val listeningStats by homeViewModel.listeningStats.collectAsStateWithLifecycle()

    val isPlayerExpanded by homeViewModel.isPlayerExpanded.collectAsStateWithLifecycle()
    val activePlayerSubView by homeViewModel.activePlayerSubView.collectAsStateWithLifecycle()
    val currentLyrics by homeViewModel.currentLyrics.collectAsStateWithLifecycle()
    val selectedLyricsProvider by homeViewModel.selectedLyricsProvider.collectAsStateWithLifecycle()
    val lyricsSettings by homeViewModel.lyricsSettings.collectAsStateWithLifecycle()
    val bitChordLyricsLines by homeViewModel.bitChordLyricsLines.collectAsStateWithLifecycle()
    val activeLyricsSource by homeViewModel.activeLyricsSource.collectAsStateWithLifecycle()
    val isEmbeddedLyrics by homeViewModel.isEmbeddedLyrics.collectAsStateWithLifecycle()
    val isLoadingLyrics by homeViewModel.isLoadingLyrics.collectAsStateWithLifecycle()
    val lyricsProviderStates by homeViewModel.lyricsProviderStates.collectAsStateWithLifecycle()
    val lyricsProviderResults by homeViewModel.lyricsProviderResults.collectAsStateWithLifecycle()
    val translatedLyrics by homeViewModel.translatedLyrics.collectAsStateWithLifecycle()
    val romanizedLyrics by homeViewModel.romanizedLyrics.collectAsStateWithLifecycle()
    val isTranslationEnabled by homeViewModel.isTranslationEnabled.collectAsStateWithLifecycle()
    val isRomanizationEnabled by homeViewModel.isRomanizationEnabled.collectAsStateWithLifecycle()
    val isTranslatingLyrics by homeViewModel.isTranslatingLyrics.collectAsStateWithLifecycle()
    val customCoverOverrides by homeViewModel.customCoverOverrides.collectAsStateWithLifecycle()
    val isVideoModeActive by homeViewModel.isVideoModeActive.collectAsStateWithLifecycle()
    val activeVideoStreamUrl by homeViewModel.activeVideoStreamUrl.collectAsStateWithLifecycle()
    val isResolvingVideoStream by homeViewModel.isResolvingVideoStream.collectAsStateWithLifecycle()
    val selectedTabIndex by homeViewModel.selectedTab.collectAsStateWithLifecycle()
    val uiMessage by homeViewModel.uiMessage.collectAsStateWithLifecycle()

    LaunchedEffect(uiMessage) {
        if (uiMessage != null) {
            delay(3000L)
            homeViewModel.clearUiMessage()
        }
    }

    val selectedTab = when (selectedTabIndex) {
        1 -> BitChordTab.LIBRARY
        2 -> BitChordTab.SEARCH
        else -> BitChordTab.HOME
    }

    BackHandler(enabled = selectedTab != BitChordTab.HOME && !isPlayerExpanded && !playlistScreenState.isVisible && !artistScreenState.isVisible && !isSettingsVisible && !isAccountIntegrationsVisible) {
        homeViewModel.selectTab(0)
    }

    val candidateTracksForAdd = remember(homeState.pintasanCepatItems, homeState.topShelfItems, favoriteTracks) {
        (favoriteTracks + homeState.pintasanCepatItems + homeState.topShelfItems).distinctBy { it.id }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceObsidian)
    ) {
        // 1. Primary Tab Content (Home, Library, Search)
        when (selectedTab) {
            BitChordTab.HOME -> {
                ListenNowScreen(
                    uiState = homeState,
                    favoriteIds = favoriteIds,
                    downloadedTrackIds = downloadedTrackIds,
                    activeDownloads = activeDownloads,
                    libraryPlaylists = libraryPlaylists,
                    onTrackClick = homeViewModel::onHomeItemClicked,
                    onCarouselItemClick = homeViewModel::onHomeItemClicked,
                    onPlaySectionAll = homeViewModel::onPlaySectionAll,
                    onPlayCommunityTrack = { track, queue, sourceLabel ->
                        homeViewModel.playSingleOrQueueTrack(
                            item = track,
                            contextQueue = queue,
                            sourceLabel = sourceLabel
                        )
                    },
                    onPlayCommunityFirstTrack = homeViewModel::onPlayCommunityPlaylistFirstTrack,
                    onStartCommunityRadio = homeViewModel::onStartCommunityPlaylistRadio,
                    onToggleSaveCommunityPlaylist = homeViewModel::onToggleSaveCommunityPlaylist,
                    onOpenSettings = homeViewModel::openSettingsScreen,
                    onOpenAccountIntegrations = homeViewModel::openAccountIntegrationsScreen,
                    onOpenTrackMenu = homeViewModel::onOpenTrackMenu,
                    onDismissTrackMenu = homeViewModel::onDismissTrackMenu,
                    onToggleFavoriteTrack = homeViewModel::toggleFavoriteTrack,
                    onAddTrackToPlaylist = homeViewModel::addTrackToLibraryPlaylist,
                    onCreatePlaylistAndAddTrack = homeViewModel::createCustomPlaylistAndAddTrack,
                    onDownloadTrack = { track ->
                        ensureNotificationPermissionAndRun {
                            homeViewModel.downloadSingleTrack(track)
                        }
                    },
                    onRemoveDownload = homeViewModel::deleteDownloadedTrack,
                    onOpenArtistByName = { artistName ->
                        homeViewModel.openArtistProfileByName(artistName)
                    },
                    onRemoveFromRecents = homeViewModel::onRemoveFromRecents,
                    onClearAllRecents = homeViewModel::onClearAllRecents,
                    onRetry = homeViewModel::loadHomeData
                )
            }

            BitChordTab.LIBRARY -> {
                LibraryScreen(
                    listeningStats = listeningStats,
                    favoriteTracks = favoriteTracks,
                    libraryPlaylists = libraryPlaylists,
                    subscribedArtists = subscribedArtists,
                    recentTracks = recentTracks,
                    artistAvatarMap = artistAvatarMap,
                    hasAudioPermission = hasAudioPermission,
                    onAudioPermissionResult = { granted ->
                        homeViewModel.onAudioPermissionResult(granted, openPlaylistAfterGrant = true)
                    },
                    onResolveArtistAvatar = homeViewModel::resolveArtistAvatarIfNeeded,
                    downloadedTracks = downloadedTracks,
                    localDeviceTracks = localDeviceTracks,
                    activeDownloads = activeDownloads,
                    candidateTracksForAdd = candidateTracksForAdd,
                    onOpenLikedSongs = homeViewModel::openLikedSongsPlaylist,
                    onOpenDownloads = homeViewModel::openDownloadsPlaylist,
                    onOpenLocalMusic = homeViewModel::openLocalMusicPlaylist,
                    onOpenPlaylist = homeViewModel::openLibraryPlaylist,
                    onCreatePlaylist = homeViewModel::createCustomPlaylist,
                    onRenamePlaylist = homeViewModel::renameLibraryPlaylist,
                    onDeletePlaylist = homeViewModel::deleteLibraryPlaylist,
                    onTogglePinPlaylist = homeViewModel::togglePinLibraryPlaylist,
                    onAddTrackToPlaylist = homeViewModel::addTrackToLibraryPlaylist,
                    onDownloadPlaylist = { userPlaylist ->
                        ensureNotificationPermissionAndRun {
                            homeViewModel.downloadMusicItemCollection(userPlaylist.toMusicItem())
                        }
                    },
                    onOpenImportIntegrations = homeViewModel::openAccountIntegrationsScreen,
                    onOpenSettings = homeViewModel::openSettingsScreen,
                    onOpenArtistByName = { artistName ->
                        homeViewModel.openArtistProfileByName(artistName)
                    },
                    onUpdateUsername = homeViewModel::updateUsername,
                    onPlayTopTrack = { topTrack ->
                        homeViewModel.playSingleOrQueueTrack(
                            item = topTrack,
                            contextQueue = listOf(topTrack),
                            sourceLabel = "Playing from Top Song"
                        )
                    }
                )
            }

            BitChordTab.SEARCH -> {
                SearchScreen(
                    searchState = searchUiState,
                    hasActiveMiniPlayer = hasActiveTrack,
                    onQueryChange = homeViewModel::onSearchQueryChange,
                    onFillQuery = homeViewModel::onFillQueryFromSuggestion,
                    onClearQuery = homeViewModel::onClearSearchQuery,
                    onSubmitSearch = homeViewModel::onSubmitSearch,
                    onSelectFilter = homeViewModel::onSelectSearchFilter,
                    onItemClick = homeViewModel::onSearchItemClicked,
                    onOpenTrackMenu = homeViewModel::onOpenTrackMenu,
                    onBackToHome = { homeViewModel.selectTab(0) }
                )
            }
        }

        // 2. Dedicated Artist Profile Screen (rendered before PlaylistDetailScreen so playlists clicked inside ArtistProfile open on top!)
        AnimatedVisibility(
            visible = artistScreenState.isVisible && artistScreenState.artistProfile != null,
            enter = fadeIn(tween(220)) + slideInHorizontally(
                animationSpec = tween(260),
                initialOffsetX = { it / 3 }
            ),
            exit = fadeOut(tween(180)) + slideOutHorizontally(
                animationSpec = tween(220),
                targetOffsetX = { it / 3 }
            )
        ) {
            val profile = artistScreenState.artistProfile
            if (profile != null) {
                val isFollowed = artistScreenState.followedArtistIds.contains(profile.id) ||
                    artistScreenState.followedArtistIds.contains(profile.name) ||
                    subscribedArtists.any { it.id == profile.id || it.name.equals(profile.name, ignoreCase = true) }
                ArtistProfileScreen(
                    artistProfile = profile,
                    isLoading = artistScreenState.isLoading,
                    isFollowing = isFollowed,
                    currentPlayingTrackId = currentPlayingTrackId,
                    favoriteIds = favoriteIds,
                    hasActiveMiniPlayer = hasActiveTrack,
                    onBack = homeViewModel::closeArtistProfile,
                    onToggleFollow = { homeViewModel.toggleFollowArtist(profile.id) },
                    onPlayTrack = { track, queue, sourceLabel ->
                        homeViewModel.playSingleOrQueueTrack(
                            item = track,
                            contextQueue = queue,
                            sourceLabel = sourceLabel
                        )
                    },
                    onOpenReleaseOrPlaylist = { item ->
                        homeViewModel.openPlaylistOrAlbumDetail(item)
                    },
                    onPlayReleaseDirect = { item ->
                        homeViewModel.onPlayCommunityPlaylistFirstTrack(item)
                    },
                    onOpenRelatedArtist = { relatedItem ->
                        homeViewModel.openArtistProfile(relatedItem)
                    },
                    onToggleFavoriteTrack = homeViewModel::toggleFavoriteTrack,
                    onOpenTrackMenu = homeViewModel::onOpenTrackMenu
                )
            }
        }

        // 3. Playlist / Album Detail Overlay Screen (rendered above ArtistProfileScreen so clicking Artist Releases / Curated Playlists opens visibly)
        AnimatedVisibility(
            visible = playlistScreenState.isVisible && playlistScreenState.playlist != null,
            enter = fadeIn(tween(220)) + slideInHorizontally(
                animationSpec = tween(260),
                initialOffsetX = { it / 3 }
            ),
            exit = fadeOut(tween(180)) + slideOutHorizontally(
                animationSpec = tween(220),
                targetOffsetX = { it / 3 }
            )
        ) {
            val openPlaylistId = playlistScreenState.playlist?.id
            val isLibraryOwned = openPlaylistId != null && libraryPlaylists.any { it.id == openPlaylistId }

            PlaylistDetailScreen(
                state = playlistScreenState,
                currentPlayingTrackId = currentPlayingTrackId,
                isPlayingAudio = isPlayingAudio,
                favoriteIds = favoriteIds,
                downloadedTrackIds = downloadedTrackIds,
                activeDownloads = activeDownloads,
                libraryPlaylists = libraryPlaylists,
                savedPlaylistIds = savedPlaylistIds,
                username = listeningStats.userHandle,
                onBackClick = homeViewModel::closePlaylistDetail,
                onPlayAllClick = homeViewModel::onPlayAllFromPlaylist,
                onShufflePlayClick = homeViewModel::onShufflePlayPlaylist,
                onPlayPlaylistNext = homeViewModel::playPlaylistNext,
                onAddPlaylistToQueue = homeViewModel::addPlaylistToQueue,
                onToggleSearchClick = homeViewModel::onTogglePlaylistSearch,
                onSearchQueryChange = homeViewModel::onUpdatePlaylistSearchQuery,
                onToggleSortClick = homeViewModel::onTogglePlaylistSort,
                onTrackClick = { clickedTrack, displayedList ->
                    val plTitle = playlistScreenState.playlist?.title ?: "Playlist"
                    homeViewModel.playSingleOrQueueTrack(
                        item = clickedTrack,
                        contextQueue = displayedList,
                        sourceLabel = "Playing from $plTitle"
                    )
                },
                onTrackMenuClick = homeViewModel::onOpenTrackMenu,
                onToggleFavoriteTrack = homeViewModel::toggleFavoriteTrack,
                onAddTrackToPlaylist = homeViewModel::addTrackToLibraryPlaylist,
                onCreatePlaylistAndAddTrack = homeViewModel::createCustomPlaylistAndAddTrack,
                onRemoveTrackFromCurrentPlaylist = if (isLibraryOwned && openPlaylistId != null) {
                    { trackId -> homeViewModel.removeTrackFromLibraryPlaylist(openPlaylistId, trackId) }
                } else null,
                onReorderPlaylistTracks = { orderedTracks ->
                    if (openPlaylistId != null) {
                        homeViewModel.reorderPlaylistTracks(openPlaylistId, orderedTracks)
                    }
                },
                onDownloadSingleTrack = { track ->
                    ensureNotificationPermissionAndRun {
                        homeViewModel.downloadSingleTrack(track)
                    }
                },
                onRemoveDownload = homeViewModel::deleteDownloadedTrack,
                onDownloadWholePlaylist = { detail ->
                    ensureNotificationPermissionAndRun {
                        homeViewModel.downloadPlaylistOrAlbum(detail)
                    }
                },
                onToggleSavePlaylistToLibrary = homeViewModel::toggleSavePlaylistDetailToLibrary,
                onRenamePlaylist = if (isLibraryOwned && openPlaylistId != null) {
                    { newTitle -> homeViewModel.renameLibraryPlaylist(openPlaylistId, newTitle) }
                } else null,
                onTogglePinPlaylist = if (isLibraryOwned && openPlaylistId != null) {
                    { homeViewModel.togglePinLibraryPlaylist(openPlaylistId) }
                } else null,
                onDeletePlaylist = if (isLibraryOwned && openPlaylistId != null) {
                    { homeViewModel.deleteLibraryPlaylist(openPlaylistId) }
                } else null,
                onOpenArtistByName = { artistName ->
                    homeViewModel.openArtistProfileByName(artistName)
                }
            )
        }

        // 4. Settings Screen
        AnimatedVisibility(
            visible = isSettingsVisible,
            enter = fadeIn(tween(200)) + slideInVertically(
                animationSpec = tween(240),
                initialOffsetY = { it / 4 }
            ),
            exit = fadeOut(tween(160)) + slideOutVertically(
                animationSpec = tween(200),
                targetOffsetY = { it / 4 }
            )
        ) {
            SettingsScreen(
                onBack = homeViewModel::closeSettingsScreen,
                onOpenAccountIntegrations = {
                    homeViewModel.closeSettingsScreen()
                    homeViewModel.openAccountIntegrationsScreen()
                },
                spotifyConnected = spotifyAccount.isConnected,
                ytMusicConnected = youtubeMusicAccount.isConnected,
                downloadStorageLocation = downloadStorageLocation,
                onSelectDownloadStorageLocation = homeViewModel::setDownloadStorageLocation,
                downloadedTracksCount = downloadedTracks.size,
                lyricsSettings = lyricsSettings,
                onToggleLyricsSourceEnabled = homeViewModel::toggleLyricsSourceEnabled,
                onMoveLyricsSourceOrder = homeViewModel::moveLyricsSourceOrder,
                onTogglePrioritizeSyllable = homeViewModel::setPrioritizeSyllableSync,
                onSavePaxSenixApiKey = homeViewModel::savePaxSenixApiKey,
                onResetLyricsSourcesDefault = homeViewModel::resetLyricsSourcesToDefault,
                onChangeLyricsOffsetMs = homeViewModel::setCurrentTrackLyricsOffsetMs,
                onSelectTranslationLanguage = homeViewModel::setLyricsTranslationLanguage,
                onSetSyncedLyricsEnabled = homeViewModel::setSyncedLyricsEnabled,
                onSetLyricsBlurEnabled = homeViewModel::setLyricsBlurEnabled,
                onSetReduceLyricsAnimation = homeViewModel::setReduceLyricsAnimation
            )
        }

        // 5. Account Integrations Screen (YouTube Music Google & Spotify)
        AnimatedVisibility(
            visible = isAccountIntegrationsVisible,
            enter = fadeIn(tween(200)) + slideInVertically(
                animationSpec = tween(240),
                initialOffsetY = { it / 4 }
            ),
            exit = fadeOut(tween(160)) + slideOutVertically(
                animationSpec = tween(200),
                targetOffsetY = { it / 4 }
            )
        ) {
            AccountIntegrationsScreen(
                spotifyAccount = spotifyAccount,
                ytMusicAccount = youtubeMusicAccount,
                isSyncing = isIntegrationSyncing,
                spotifyImportProgress = spotifyImportProgress,
                statusMessage = integrationStatusMessage,
                onBack = homeViewModel::closeAccountIntegrationsScreen,
                onConnectSpotify = homeViewModel::connectSpotifyAccount,
                onConnectYtMusic = homeViewModel::connectYouTubeMusicAccount,
                onDisconnectSpotify = { homeViewModel.disconnectIntegration(com.example.data.model.IntegrationPlatform.SPOTIFY) },
                onDisconnectYtMusic = { homeViewModel.disconnectIntegration(com.example.data.model.IntegrationPlatform.YOUTUBE_MUSIC) },
                onClearStatusMessage = homeViewModel::clearIntegrationStatusMessage,
                onNavigateToLibrary = {
                    homeViewModel.closeAccountIntegrationsScreen()
                    homeViewModel.selectTab(1)
                }
            )
        }

        // 6. Bottom Ambient Scrim
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(176.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.72f),
                            SurfaceObsidian.copy(alpha = 0.96f)
                        )
                    )
                )
        )

        // 7. Floating Mini Player + Floating Pill Navigation Dock (3 Tabs: Home, Library, Search)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = 12.dp),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Toast / Feedback Message Pill
            AnimatedVisibility(
                visible = uiMessage != null,
                enter = fadeIn(tween(180)) + slideInVertically { it / 2 },
                exit = fadeOut(tween(150)) + slideOutVertically { it / 2 }
            ) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 24.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFF232734))
                        .border(1.dp, Color(0xFF8AB4F8).copy(alpha = 0.4f), RoundedCornerShape(50))
                        .padding(horizontal = 18.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = uiMessage.orEmpty(),
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color.White
                    )
                }
            }

            AnimatedVisibility(
                visible = hasActiveTrack && !isPlayerExpanded,
                enter = slideInVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    initialOffsetY = { it }
                ) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                IsolatedFloatingMiniPlayerHost(
                    playerStateFlow = homeViewModel.playerState,
                    customCoverOverrides = customCoverOverrides,
                    onExpandPlayer = { homeViewModel.openFullPlayer(PlayerSubView.MAIN) },
                    onTogglePlayPause = homeViewModel::onTogglePlayPause,
                    onNextTrack = homeViewModel::onNextTrack,
                    onPreviousTrack = homeViewModel::onPreviousTrack
                )
            }

            if (hasActiveTrack && !isPlayerExpanded) {
                Spacer(modifier = Modifier.height(10.dp))
            }

            FloatingPillBottomBar(
                selectedTab = selectedTab,
                onSelectTab = { tab ->
                    if (playlistScreenState.isVisible) {
                        homeViewModel.closePlaylistDetail()
                    }
                    if (artistScreenState.isVisible) {
                        homeViewModel.closeArtistProfile()
                    }
                    if (isSettingsVisible) {
                        homeViewModel.closeSettingsScreen()
                    }
                    if (isAccountIntegrationsVisible) {
                        homeViewModel.closeAccountIntegrationsScreen()
                    }
                    when (tab) {
                        BitChordTab.HOME -> homeViewModel.selectTab(0)
                        BitChordTab.LIBRARY -> homeViewModel.selectTab(1)
                        BitChordTab.SEARCH -> homeViewModel.selectTab(2)
                    }
                }
            )
        }

        // 8. Full Screen Expandable Music Player (Isolated State Host so scrolling behind never lags)
        AnimatedVisibility(
            visible = isPlayerExpanded && hasActiveTrack,
            enter = slideInVertically(
                animationSpec = spring(
                    dampingRatio = 0.86f,
                    stiffness = Spring.StiffnessMediumLow
                ),
                initialOffsetY = { it }
            ) + fadeIn(),
            exit = slideOutVertically(
                animationSpec = spring(
                    dampingRatio = 0.9f,
                    stiffness = Spring.StiffnessMedium
                ),
                targetOffsetY = { it }
            ) + fadeOut()
        ) {
            IsolatedFullMusicPlayerHost(
                isVisible = isPlayerExpanded,
                playerStateFlow = homeViewModel.playerState,
                activeSubView = activePlayerSubView,
                lyricsData = currentLyrics,
                selectedLyricsProvider = selectedLyricsProvider,
                favoriteIds = favoriteIds,
                downloadedTrackIds = downloadedTrackIds,
                activeDownloads = activeDownloads,
                libraryPlaylists = libraryPlaylists,
                customCoverOverrides = customCoverOverrides,
                isVideoModeActive = isVideoModeActive,
                activeVideoStreamUrl = activeVideoStreamUrl,
                isResolvingVideoStream = isResolvingVideoStream,
                onToggleVideoMode = homeViewModel::toggleVideoPlayerMode,
                onClosePlayer = homeViewModel::closeFullPlayer,
                onChangeSubView = homeViewModel::setPlayerSubView,
                onSelectLyricsProvider = homeViewModel::selectLyricsProvider,
                onApplyLrcHdCover = homeViewModel::applyLrcHdCover,
                onRestoreDefaultCover = homeViewModel::restoreDefaultCover,
                onToggleFavorite = homeViewModel::toggleFavoriteTrack,
                onDownloadTrack = { track ->
                    ensureNotificationPermissionAndRun {
                        homeViewModel.downloadSingleTrack(track)
                    }
                },
                onRemoveDownload = homeViewModel::deleteDownloadedTrack,
                onAddTrackToPlaylist = homeViewModel::addTrackToLibraryPlaylist,
                onCreatePlaylistAndAddTrack = homeViewModel::createCustomPlaylistAndAddTrack,
                onTogglePlayPause = homeViewModel::onTogglePlayPause,
                onNextTrack = homeViewModel::onNextTrack,
                onPreviousTrack = homeViewModel::onPreviousTrack,
                onSeekTo = homeViewModel::onSeekTo,
                onVolumeChange = homeViewModel::onVolumeChange,
                onVolumeStep = homeViewModel::onVolumeStep,
                onSyncDeviceVolume = homeViewModel::onSyncDeviceVolume,
                onToggleShuffle = homeViewModel::onToggleShuffle,
                onToggleRepeat = homeViewModel::onToggleRepeat,
                onToggleAutoPlay = homeViewModel::onToggleAutoPlay,
                onSelectQueueTrack = homeViewModel::onSelectTrackFromQueue,
                onRemoveQueueTrack = homeViewModel::onRemoveTrackFromQueue,
                onPlayNext = homeViewModel::playTrackNext,
                onAddToQueue = homeViewModel::addTrackToQueue,
                onSetSleepTimerMinutes = homeViewModel::setSleepTimerMinutes,
                onSetSleepTimerEndOfTrack = homeViewModel::setSleepTimerEndOfTrack,
                onCancelSleepTimer = homeViewModel::cancelSleepTimer,
                onOpenArtistProfile = { artistName, thumbUrl ->
                    homeViewModel.openArtistProfileByName(artistName, thumbUrl)
                },
                bitChordLyricsLines = bitChordLyricsLines,
                translatedLyricsLines = translatedLyrics,
                romanizedLyricsLines = romanizedLyrics,
                isTranslationEnabled = isTranslationEnabled,
                isRomanizationEnabled = isRomanizationEnabled,
                isTranslatingLyrics = isTranslatingLyrics,
                activeLyricsSource = activeLyricsSource,
                isEmbeddedLyrics = isEmbeddedLyrics,
                isLoadingLyrics = isLoadingLyrics,
                lyricsSettings = lyricsSettings,
                lyricsProviderStates = lyricsProviderStates,
                lyricsProviderResults = lyricsProviderResults,
                onSelectBitChordLyricsSource = homeViewModel::selectBitChordLyricsSource,
                onToggleLyricsSourceEnabled = homeViewModel::toggleLyricsSourceEnabled,
                onMoveLyricsSourceOrder = homeViewModel::moveLyricsSourceOrder,
                onTogglePrioritizeSyllable = homeViewModel::setPrioritizeSyllableSync,
                onSavePaxSenixApiKey = homeViewModel::savePaxSenixApiKey,
                onResetLyricsSourcesDefault = homeViewModel::resetLyricsSourcesToDefault,
                onChangeTrackLyricsOffsetMs = homeViewModel::setCurrentTrackLyricsOffsetMs,
                onToggleLyricsTranslation = homeViewModel::toggleLyricsTranslation,
                onToggleLyricsRomanization = homeViewModel::toggleLyricsRomanization,
                onSelectTranslationLanguage = homeViewModel::setLyricsTranslationLanguage
            )
        }
    }
}

@Composable
private fun IsolatedFloatingMiniPlayerHost(
    playerStateFlow: StateFlow<PlayerUiState>,
    customCoverOverrides: Map<String, String>,
    onExpandPlayer: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNextTrack: () -> Unit,
    onPreviousTrack: () -> Unit
) {
    val playerState by playerStateFlow.collectAsStateWithLifecycle()
    val customCoverUrl = playerState.currentTrack?.id?.let { customCoverOverrides[it] }
    FloatingMiniPlayer(
        playerState = playerState,
        customCoverUrl = customCoverUrl,
        onExpandPlayer = onExpandPlayer,
        onTogglePlayPause = onTogglePlayPause,
        onNextTrack = onNextTrack
    )
}

@Composable
private fun IsolatedFullMusicPlayerHost(
    isVisible: Boolean,
    playerStateFlow: StateFlow<PlayerUiState>,
    activeSubView: PlayerSubView,
    lyricsData: LyricsData?,
    selectedLyricsProvider: LyricsProvider,
    favoriteIds: Set<String>,
    downloadedTrackIds: Set<String>,
    activeDownloads: Map<String, ActiveDownloadProgress>,
    libraryPlaylists: List<UserLibraryPlaylist>,
    customCoverOverrides: Map<String, String>,
    isVideoModeActive: Boolean,
    activeVideoStreamUrl: String?,
    isResolvingVideoStream: Boolean,
    onToggleVideoMode: () -> Unit,
    onClosePlayer: () -> Unit,
    onChangeSubView: (PlayerSubView) -> Unit,
    onSelectLyricsProvider: (LyricsProvider) -> Unit,
    onApplyLrcHdCover: (MusicItem, String) -> Unit,
    onRestoreDefaultCover: (MusicItem) -> Unit,
    onToggleFavorite: (MusicItem) -> Unit,
    onDownloadTrack: (MusicItem) -> Unit,
    onRemoveDownload: (String) -> Unit,
    onAddTrackToPlaylist: (playlistId: String, track: MusicItem) -> Unit,
    onCreatePlaylistAndAddTrack: (title: String, privacy: PlaylistPrivacy, track: MusicItem) -> Unit,
    onTogglePlayPause: () -> Unit,
    onNextTrack: () -> Unit,
    onPreviousTrack: () -> Unit,
    onSeekTo: (Float) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onVolumeStep: (Boolean) -> Unit,
    onSyncDeviceVolume: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleAutoPlay: () -> Unit,
    onSelectQueueTrack: (MusicItem) -> Unit,
    onRemoveQueueTrack: (MusicItem) -> Unit,
    onPlayNext: (MusicItem) -> Unit,
    onAddToQueue: (MusicItem) -> Unit,
    onSetSleepTimerMinutes: (Int, String) -> Unit,
    onSetSleepTimerEndOfTrack: () -> Unit,
    onCancelSleepTimer: () -> Unit,
    onOpenArtistProfile: (String, String) -> Unit,
    bitChordLyricsLines: List<BitChordLyricLine>,
    translatedLyricsLines: List<BitChordLyricLine>?,
    romanizedLyricsLines: List<BitChordLyricLine>?,
    isTranslationEnabled: Boolean,
    isRomanizationEnabled: Boolean,
    isTranslatingLyrics: Boolean,
    activeLyricsSource: LyricsSource?,
    isEmbeddedLyrics: Boolean,
    isLoadingLyrics: Boolean,
    lyricsSettings: LyricsSettingsState,
    lyricsProviderStates: Map<LyricsSource, LyricsProviderState>,
    lyricsProviderResults: Map<LyricsSource, List<BitChordLyricLine>>,
    onSelectBitChordLyricsSource: (LyricsSource) -> Unit,
    onToggleLyricsSourceEnabled: (LyricsSource) -> Unit,
    onMoveLyricsSourceOrder: (Int, Int) -> Unit,
    onTogglePrioritizeSyllable: (Boolean) -> Unit,
    onSavePaxSenixApiKey: (String) -> Unit,
    onResetLyricsSourcesDefault: () -> Unit,
    onChangeTrackLyricsOffsetMs: (Int) -> Unit,
    onToggleLyricsTranslation: () -> Unit,
    onToggleLyricsRomanization: () -> Unit,
    onSelectTranslationLanguage: (String) -> Unit
) {
    val playerState by playerStateFlow.collectAsStateWithLifecycle()
    val currentId = playerState.currentTrack?.id
    val isDownloaded = currentId != null && downloadedTrackIds.contains(currentId)
    val activeDl = if (currentId != null) activeDownloads[currentId] else null

    FullMusicPlayerScreen(
        isVisible = isVisible,
        playerState = playerState,
        activeSubView = activeSubView,
        lyricsData = lyricsData,
        selectedLyricsProvider = selectedLyricsProvider,
        favoriteIds = favoriteIds,
        customCoverOverrides = customCoverOverrides,
        isDownloaded = isDownloaded,
        activeDownloadProgress = activeDl,
        libraryPlaylists = libraryPlaylists,
        isVideoModeActive = isVideoModeActive,
        activeVideoStreamUrl = activeVideoStreamUrl,
        isResolvingVideoStream = isResolvingVideoStream,
        onToggleVideoMode = onToggleVideoMode,
        onClosePlayer = onClosePlayer,
        onChangeSubView = onChangeSubView,
        onSelectLyricsProvider = onSelectLyricsProvider,
        onApplyLrcHdCover = onApplyLrcHdCover,
        onRestoreDefaultCover = onRestoreDefaultCover,
        onToggleFavorite = onToggleFavorite,
        onDownloadTrack = onDownloadTrack,
        onRemoveDownload = onRemoveDownload,
        onAddTrackToPlaylist = onAddTrackToPlaylist,
        onCreatePlaylistAndAddTrack = onCreatePlaylistAndAddTrack,
        onTogglePlayPause = onTogglePlayPause,
        onNextTrack = onNextTrack,
        onPreviousTrack = onPreviousTrack,
        onSeekTo = onSeekTo,
        onVolumeChange = onVolumeChange,
        onVolumeStep = onVolumeStep,
        onSyncDeviceVolume = onSyncDeviceVolume,
        onToggleShuffle = onToggleShuffle,
        onToggleRepeat = onToggleRepeat,
        onToggleAutoPlay = onToggleAutoPlay,
        onSelectQueueTrack = onSelectQueueTrack,
        onRemoveQueueTrack = onRemoveQueueTrack,
        onPlayNext = onPlayNext,
        onAddToQueue = onAddToQueue,
        onSetSleepTimerMinutes = onSetSleepTimerMinutes,
        onSetSleepTimerEndOfTrack = onSetSleepTimerEndOfTrack,
        onCancelSleepTimer = onCancelSleepTimer,
        onOpenArtistProfile = onOpenArtistProfile,
        bitChordLyricsLines = bitChordLyricsLines,
        translatedLyricsLines = translatedLyricsLines,
        romanizedLyricsLines = romanizedLyricsLines,
        isTranslationEnabled = isTranslationEnabled,
        isRomanizationEnabled = isRomanizationEnabled,
        isTranslatingLyrics = isTranslatingLyrics,
        activeLyricsSource = activeLyricsSource,
        isEmbeddedLyrics = isEmbeddedLyrics,
        isLoadingLyrics = isLoadingLyrics,
        lyricsSettings = lyricsSettings,
        lyricsProviderStates = lyricsProviderStates,
        lyricsProviderResults = lyricsProviderResults,
        onSelectBitChordLyricsSource = onSelectBitChordLyricsSource,
        onToggleLyricsSourceEnabled = onToggleLyricsSourceEnabled,
        onMoveLyricsSourceOrder = onMoveLyricsSourceOrder,
        onTogglePrioritizeSyllable = onTogglePrioritizeSyllable,
        onSavePaxSenixApiKey = onSavePaxSenixApiKey,
        onResetLyricsSourcesDefault = onResetLyricsSourcesDefault,
        onChangeTrackLyricsOffsetMs = onChangeTrackLyricsOffsetMs,
        onToggleLyricsTranslation = onToggleLyricsTranslation,
        onToggleLyricsRomanization = onToggleLyricsRomanization,
        onSelectTranslationLanguage = onSelectTranslationLanguage
    )
}
