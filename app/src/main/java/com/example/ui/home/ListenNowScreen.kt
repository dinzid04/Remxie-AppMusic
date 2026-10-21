package com.example.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.ActiveDownloadProgress
import com.example.data.model.MusicItem
import com.example.data.model.PlaylistPrivacy
import com.example.data.model.UserLibraryPlaylist
import com.example.ui.components.BitChordTopHeader
import com.example.ui.components.FourRowHorizontalTrackShelf
import com.example.ui.components.ListenNowSkeletonLoading
import com.example.ui.components.MusicCarouselSection
import com.example.ui.components.PintasanCepatGridSection
import com.example.ui.library.AddToPlaylistBottomSheet
import com.example.ui.library.UniversalTrackMenuBottomSheet
import com.example.ui.theme.SurfaceObsidian

@Composable
fun ListenNowScreen(
    uiState: HomeUiState,
    favoriteIds: Set<String> = emptySet(),
    downloadedTrackIds: Set<String> = emptySet(),
    activeDownloads: Map<String, ActiveDownloadProgress> = emptyMap(),
    libraryPlaylists: List<UserLibraryPlaylist> = emptyList(),
    onTrackClick: (MusicItem) -> Unit,
    onCarouselItemClick: (MusicItem) -> Unit,
    onPlaySectionAll: (String, List<MusicItem>) -> Unit,
    onPlayCommunityTrack: (MusicItem, List<MusicItem>, String) -> Unit = { item, _, _ -> onCarouselItemClick(item) },
    onPlayCommunityFirstTrack: (MusicItem) -> Unit = onCarouselItemClick,
    onStartCommunityRadio: (MusicItem) -> Unit = onCarouselItemClick,
    onToggleSaveCommunityPlaylist: (MusicItem) -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenAccountIntegrations: () -> Unit = {},
    onOpenTrackMenu: (MusicItem) -> Unit,
    onDismissTrackMenu: () -> Unit,
    onToggleFavoriteTrack: (MusicItem) -> Unit = {},
    onAddTrackToPlaylist: (playlistId: String, track: MusicItem) -> Unit = { _, _ -> },
    onCreatePlaylistAndAddTrack: (title: String, privacy: PlaylistPrivacy, track: MusicItem) -> Unit = { _, _, _ -> },
    onDownloadTrack: (MusicItem) -> Unit = {},
    onRemoveDownload: (String) -> Unit = {},
    onOpenArtistByName: (String) -> Unit = {},
    onRemoveFromRecents: (MusicItem) -> Unit,
    onClearAllRecents: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    var trackForAddToPlaylist by remember { mutableStateOf<MusicItem?>(null) }

    val scrollProgress by remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) {
                1f
            } else {
                (listState.firstVisibleItemScrollOffset / 520f).coerceIn(0f, 1f)
            }
        }
    }

    val ambientImageUrl = remember(uiState.pintasanCepatItems, uiState.topShelfItems) {
        uiState.pintasanCepatItems.firstOrNull()?.highResThumbnail
            ?: uiState.topShelfItems.firstOrNull()?.highResThumbnail
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("listen_now_scaffold"),
        containerColor = SurfaceObsidian,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("listen_now_lazy_column"),
                contentPadding = PaddingValues(bottom = 185.dp)
            ) {
                item(
                    key = "top_header",
                    contentType = "header"
                ) {
                    BitChordTopHeader(
                        onProfileClick = onOpenSettings
                    )
                }

                // Conditional "Pintasan cepat" 3x3 Grid: ONLY appears AFTER user plays music
                if (uiState.hasListenedToMusic && uiState.pintasanCepatItems.isNotEmpty()) {
                    item(
                        key = "pintasan_cepat_section",
                        contentType = "pintasan_cepat"
                    ) {
                        PintasanCepatGridSection(
                            items = uiState.pintasanCepatItems,
                            onItemClick = { clicked ->
                                if (clicked.isCollection) {
                                    onCarouselItemClick(clicked)
                                } else {
                                    onPlayCommunityTrack(
                                        clicked,
                                        uiState.pintasanCepatItems,
                                        "Playing from Pintasan cepat"
                                    )
                                }
                            },
                            onExpandAllClick = {
                                onPlaySectionAll("Pintasan cepat", uiState.pintasanCepatItems)
                            }
                        )
                    }
                }

                if (uiState.isLoading && uiState.topShelfItems.isEmpty()) {
                    item(
                        key = "skeleton_loading",
                        contentType = "skeleton"
                    ) {
                        ListenNowSkeletonLoading(
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                } else {
                    // Top Horizontal 4-Row Track Shelf ("Pilihan cepat")
                    if (uiState.topShelfItems.isNotEmpty()) {
                        item(
                            key = "pilihan_cepat_shelf",
                            contentType = "four_row_shelf"
                        ) {
                            FourRowHorizontalTrackShelf(
                                title = uiState.topShelfTitle,
                                items = uiState.topShelfItems,
                                onTrackClick = onTrackClick,
                                onTrackMenuClick = onOpenTrackMenu,
                                onPlayAllClick = {
                                    onPlaySectionAll(uiState.topShelfTitle, uiState.topShelfItems)
                                }
                            )
                        }
                    }

                    // Dynamic YTM Sections
                    items(
                        items = uiState.carouselSections,
                        key = { section -> "section_${section.id}" },
                        contentType = { section -> section.style.name }
                    ) { section ->
                        MusicCarouselSection(
                            section = section,
                            homeThemeMode = uiState.homeThemeMode,
                            savedPlaylistIds = uiState.savedPlaylistIds,
                            onItemClick = onCarouselItemClick,
                            onTrackMenuClick = onOpenTrackMenu,
                            onPlayAllClick = {
                                onPlaySectionAll(section.title, section.items)
                            },
                            onPlayCommunityTrack = onPlayCommunityTrack,
                            onPlayCommunityFirstTrack = onPlayCommunityFirstTrack,
                            onStartCommunityRadio = onStartCommunityRadio,
                            onToggleSaveCommunityPlaylist = onToggleSaveCommunityPlaylist
                        )
                    }
                }

                item(key = "bottom_spacer") {
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp)
                    )
                }
            }

            // Universal 3-Dot Track Menu Sheet (Like, Add to Playlist, Download Offline, Remove Recent, Artist)
            uiState.selectedTrackForMenu?.let { selectedTrack ->
                val isFav = favoriteIds.contains(selectedTrack.id) ||
                    (selectedTrack.videoId != null && favoriteIds.contains(selectedTrack.videoId))
                val isDownloaded = downloadedTrackIds.contains(selectedTrack.id)
                val activeDl = activeDownloads[selectedTrack.id]

                UniversalTrackMenuBottomSheet(
                    track = selectedTrack,
                    isFavorite = isFav,
                    isDownloaded = isDownloaded,
                    activeDownloadProgress = activeDl,
                    onDismiss = onDismissTrackMenu,
                    onToggleFavorite = { onToggleFavoriteTrack(selectedTrack) },
                    onAddToPlaylist = { trackForAddToPlaylist = selectedTrack },
                    onDownloadTrack = { onDownloadTrack(selectedTrack) },
                    onRemoveDownload = { onRemoveDownload(selectedTrack.id) },
                    onOpenArtist = { onOpenArtistByName(selectedTrack.cleanArtist) },
                    onRemoveFromCurrentPlaylist = if (uiState.pintasanCepatItems.any { it.id == selectedTrack.id }) {
                        { onRemoveFromRecents(selectedTrack) }
                    } else null
                )
            }

            trackForAddToPlaylist?.let { targetTrack ->
                AddToPlaylistBottomSheet(
                    track = targetTrack,
                    userPlaylists = libraryPlaylists,
                    onDismiss = { trackForAddToPlaylist = null },
                    onSelectPlaylist = { playlist ->
                        onAddTrackToPlaylist(playlist.id, targetTrack)
                        trackForAddToPlaylist = null
                    },
                    onCreateNewPlaylistAndAdd = { title, privacy ->
                        onCreatePlaylistAndAddTrack(title, privacy, targetTrack)
                        trackForAddToPlaylist = null
                    }
                )
            }
        }
    }
}
