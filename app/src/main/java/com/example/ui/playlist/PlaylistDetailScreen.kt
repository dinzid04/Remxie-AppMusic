package com.example.ui.playlist

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Size
import com.example.data.model.ActiveDownloadProgress
import com.example.data.model.MusicItem
import com.example.data.model.PlaylistDetail
import com.example.data.model.PlaylistPrivacy
import com.example.data.model.UserLibraryPlaylist
import com.example.ui.home.PlaylistScreenState
import com.example.ui.library.AddToPlaylistBottomSheet
import com.example.ui.library.UniversalTrackMenuBottomSheet
import com.example.ui.theme.CardPlaceholderDark
import com.example.ui.theme.PlusJakartaSans
import com.example.ui.theme.TextPrimaryWhite
import com.example.ui.theme.rememberDominantAccentColor

private fun Color.darken(factor: Float): Color {
    val f = factor.coerceIn(0f, 1f)
    return Color(
        red = red * f,
        green = green * f,
        blue = blue * f,
        alpha = 1f
    )
}

private fun Color.lightenTowardWhite(amount: Float): Color {
    val a = amount.coerceIn(0f, 1f)
    return Color(
        red = red + (1f - red) * a,
        green = green + (1f - green) * a,
        blue = blue + (1f - blue) * a,
        alpha = 1f
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    state: PlaylistScreenState,
    currentPlayingTrackId: String?,
    isPlayingAudio: Boolean,
    favoriteIds: Set<String> = emptySet(),
    downloadedTrackIds: Set<String> = emptySet(),
    activeDownloads: Map<String, ActiveDownloadProgress> = emptyMap(),
    libraryPlaylists: List<UserLibraryPlaylist> = emptyList(),
    savedPlaylistIds: Set<String> = emptySet(),
    username: String = "user",
    onBackClick: () -> Unit,
    onPlayAllClick: () -> Unit,
    onShufflePlayClick: () -> Unit,
    onToggleSearchClick: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onToggleSortClick: () -> Unit,
    onTrackClick: (MusicItem, List<MusicItem>) -> Unit,
    onTrackMenuClick: (MusicItem) -> Unit,
    onToggleFavoriteTrack: (MusicItem) -> Unit = {},
    onAddTrackToPlaylist: (String, MusicItem) -> Unit = { _, _ -> },
    onCreatePlaylistAndAddTrack: (String, PlaylistPrivacy, MusicItem) -> Unit = { _, _, _ -> },
    onRemoveTrackFromCurrentPlaylist: ((String) -> Unit)? = null,
    onDownloadSingleTrack: (MusicItem) -> Unit = {},
    onRemoveDownload: (String) -> Unit = {},
    onDownloadWholePlaylist: (PlaylistDetail) -> Unit = {},
    onToggleSavePlaylistToLibrary: (PlaylistDetail) -> Unit = {},
    onPlayPlaylistNext: (PlaylistDetail) -> Unit = {},
    onAddPlaylistToQueue: (PlaylistDetail) -> Unit = {},
    onReorderPlaylistTracks: (List<MusicItem>) -> Unit = {},
    onRenamePlaylist: ((String) -> Unit)? = null,
    onTogglePinPlaylist: (() -> Unit)? = null,
    onDeletePlaylist: (() -> Unit)? = null,
    onOpenArtistByName: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val playlist = state.playlist ?: return
    val context = LocalContext.current

    BackHandler(enabled = state.isVisible) {
        onBackClick()
    }

    var showPlaylistMoreSheet by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var isReorderMode by remember(playlist.id) { mutableStateOf(false) }
    var selectedTrackForMenu by remember { mutableStateOf<MusicItem?>(null) }
    var selectedTrackForAddToPlaylist by remember { mutableStateOf<MusicItem?>(null) }

    val coverUrl = remember(playlist.thumbnailUrl, playlist.tracks) {
        playlist.thumbnailUrl.ifBlank {
            playlist.tracks.firstOrNull()?.ultraResThumbnail.orEmpty()
        }
    }

    // Dynamic palette extraction from playlist cover artwork
    val extractedDominant = rememberDominantAccentColor(
        imageUrl = coverUrl,
        defaultColor = Color(0xFF8B2635)
    )

    val animatedDominant by animateColorAsState(
        targetValue = extractedDominant,
        animationSpec = tween(durationMillis = 550),
        label = "playlistDominantColor"
    )

    // Deep atmospheric background color matching Screenshot_20261020_165435
    val pageBaseTint = remember(animatedDominant) {
        animatedDominant.darken(0.24f)
    }
    val pageBottomTint = remember(animatedDominant) {
        animatedDominant.darken(0.10f)
    }
    val subtitleCoralTint = remember(animatedDominant) {
        animatedDominant.lightenTowardWhite(0.58f)
    }
    val actionCircleColor = remember(animatedDominant) {
        animatedDominant.darken(0.46f)
    }

    val displayedTracks = remember(playlist.tracks, state.searchQuery, state.sortAscending) {
        val filtered = if (state.searchQuery.isBlank()) {
            playlist.tracks
        } else {
            val q = state.searchQuery.trim().lowercase()
            playlist.tracks.filter {
                it.title.lowercase().contains(q) || it.subtitle.lowercase().contains(q)
            }
        }
        if (state.sortAscending) {
            filtered
        } else {
            filtered.sortedBy { it.title.lowercase() }
        }
    }

    val isCurrentPlaylistSaved = savedPlaylistIds.contains(playlist.id)
    val isCurrentPlaylistPinned = libraryPlaylists.firstOrNull { it.id == playlist.id }?.isPinned == true

    // 1. 3-Dots (...) Playlist Options Bottom Sheet (Exact match to Screenshot_20261020_165442)
    if (showPlaylistMoreSheet) {
        PlaylistOptionsReferenceSheet(
            playlist = playlist,
            coverUrl = coverUrl,
            username = username,
            isPinned = isCurrentPlaylistPinned,
            isReorderMode = isReorderMode,
            onDismiss = { showPlaylistMoreSheet = false },
            onPlayNext = {
                showPlaylistMoreSheet = false
                onPlayPlaylistNext(playlist)
            },
            onAddToQueue = {
                showPlaylistMoreSheet = false
                onAddPlaylistToQueue(playlist)
            },
            onDownloadAll = {
                showPlaylistMoreSheet = false
                onDownloadWholePlaylist(playlist)
            },
            onShare = {
                showPlaylistMoreSheet = false
                val trackPreview = playlist.tracks.take(10).joinToString("\n") { "• ${it.title} - ${it.cleanArtist}" }
                val shareBody = buildString {
                    append("Dengarkan playlist \"${playlist.title}\" (${playlist.tracks.size} lagu) di BitChord!\n\n")
                    if (trackPreview.isNotBlank()) {
                        append(trackPreview)
                    }
                }
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, playlist.title)
                    putExtra(Intent.EXTRA_TEXT, shareBody)
                }
                context.startActivity(Intent.createChooser(sendIntent, "Bagikan Playlist"))
            },
            onTogglePin = {
                showPlaylistMoreSheet = false
                onTogglePinPlaylist?.invoke() ?: onToggleSavePlaylistToLibrary(playlist)
            },
            onRename = {
                showPlaylistMoreSheet = false
                showRenameDialog = true
            },
            onReorderSongs = {
                showPlaylistMoreSheet = false
                isReorderMode = !isReorderMode
            },
            onDeletePlaylist = {
                showPlaylistMoreSheet = false
                onDeletePlaylist?.invoke() ?: onToggleSavePlaylistToLibrary(playlist)
            }
        )
    }

    if (showRenameDialog) {
        RenamePlaylistInlineDialog(
            currentTitle = playlist.title,
            onDismiss = { showRenameDialog = false },
            onRename = { newTitle ->
                onRenamePlaylist?.invoke(newTitle)
                showRenameDialog = false
            }
        )
    }

    // 2. Per-Track 3-Dots Bottom Sheet
    selectedTrackForMenu?.let { track ->
        val trackKey = track.videoId ?: track.id
        val isFav = favoriteIds.contains(track.id) || favoriteIds.contains(trackKey)
        val isDownloaded = downloadedTrackIds.contains(track.id) || downloadedTrackIds.contains(trackKey)
        val activeProg = activeDownloads[trackKey]?.progressPercent ?: activeDownloads[track.id]?.progressPercent

        UniversalTrackMenuBottomSheet(
            track = track,
            isFavorite = isFav,
            isDownloaded = isDownloaded,
            downloadProgressPercent = activeProg,
            onDismiss = { selectedTrackForMenu = null },
            onPlayNow = {
                onTrackClick(track, displayedTracks)
            },
            onToggleFavorite = {
                onToggleFavoriteTrack(track)
            },
            onAddToPlaylist = {
                selectedTrackForMenu = null
                selectedTrackForAddToPlaylist = track
            },
            onDownloadTrack = {
                onDownloadSingleTrack(track)
            },
            onRemoveDownload = {
                onRemoveDownload(trackKey)
            },
            onRemoveFromCurrentPlaylist = onRemoveTrackFromCurrentPlaylist?.let { removeFn ->
                { removeFn(track.id) }
            },
            onOpenArtistProfile = {
                selectedTrackForMenu = null
                onOpenArtistByName(track.cleanArtist)
            }
        )
    }

    // 3. Add Track to Playlist Sheet
    selectedTrackForAddToPlaylist?.let { track ->
        AddToPlaylistBottomSheet(
            track = track,
            userPlaylists = libraryPlaylists,
            onDismiss = { selectedTrackForAddToPlaylist = null },
            onSelectPlaylist = { targetPlaylist ->
                onAddTrackToPlaylist(targetPlaylist.id, track)
            },
            onCreateNewPlaylistAndAdd = { newTitle, privacy ->
                onCreatePlaylistAndAddTrack(newTitle, privacy, track)
            }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        pageBaseTint,
                        pageBaseTint.darken(0.82f),
                        pageBottomTint
                    )
                )
            )
            .testTag("playlist_detail_screen")
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("playlist_tracks_list"),
            contentPadding = PaddingValues(bottom = 190.dp)
        ) {
            // HERO HEADER: Full-bleed cover image at top with seamless blur & opacity fade (Screenshot_20261020_165435)
            item(key = "playlist_hero_header") {
                PlaylistHeroHeader(
                    playlist = playlist,
                    coverUrl = coverUrl,
                    username = username,
                    pageBaseTint = pageBaseTint,
                    subtitleCoralTint = subtitleCoralTint,
                    actionCircleColor = actionCircleColor,
                    isSearchVisible = state.isSearchVisible,
                    searchQuery = state.searchQuery,
                    isReorderMode = isReorderMode,
                    onBackClick = onBackClick,
                    onShufflePlayClick = onShufflePlayClick,
                    onPlayAllClick = onPlayAllClick,
                    onToggleSearchClick = onToggleSearchClick,
                    onSearchQueryChange = onSearchQueryChange,
                    onOpenMoreMenu = { showPlaylistMoreSheet = true },
                    onDoneReorder = { isReorderMode = false }
                )
            }

            if (state.isLoading && displayedTracks.isEmpty()) {
                item(key = "playlist_loading_spinner") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }
            }

            itemsIndexed(
                items = displayedTracks,
                key = { idx, item -> "pl_track_${item.id}_$idx" }
            ) { index, track ->
                val isCurrentlyPlaying = currentPlayingTrackId != null &&
                    (track.id == currentPlayingTrackId || (track.videoId != null && track.videoId == currentPlayingTrackId))
                val trackKey = track.videoId ?: track.id
                val isDownloaded = downloadedTrackIds.contains(track.id) || downloadedTrackIds.contains(trackKey)
                val activeProgress = activeDownloads[trackKey]?.progressPercent ?: activeDownloads[track.id]?.progressPercent

                PlaylistTrackRowItem(
                    track = track,
                    fallbackCoverUrl = coverUrl,
                    isCurrentlyPlaying = isCurrentlyPlaying,
                    isPlayingAudio = isPlayingAudio,
                    isDownloaded = isDownloaded,
                    activeDownloadPercent = activeProgress,
                    isReorderMode = isReorderMode,
                    canMoveUp = index > 0,
                    canMoveDown = index < displayedTracks.lastIndex,
                    onMoveUp = {
                        if (index > 0) {
                            val mutable = playlist.tracks.toMutableList()
                            val item = mutable.removeAt(index)
                            mutable.add(index - 1, item)
                            onReorderPlaylistTracks(mutable)
                        }
                    },
                    onMoveDown = {
                        if (index < playlist.tracks.lastIndex) {
                            val mutable = playlist.tracks.toMutableList()
                            val item = mutable.removeAt(index)
                            mutable.add(index + 1, item)
                            onReorderPlaylistTracks(mutable)
                        }
                    },
                    onClick = { onTrackClick(track, displayedTracks) },
                    onMoreClick = { selectedTrackForMenu = track }
                )
            }
        }
    }
}

@Composable
private fun PlaylistHeroHeader(
    playlist: PlaylistDetail,
    coverUrl: String,
    username: String,
    pageBaseTint: Color,
    subtitleCoralTint: Color,
    actionCircleColor: Color,
    isSearchVisible: Boolean,
    searchQuery: String,
    isReorderMode: Boolean,
    onBackClick: () -> Unit,
    onShufflePlayClick: () -> Unit,
    onPlayAllClick: () -> Unit,
    onToggleSearchClick: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onOpenMoreMenu: () -> Unit,
    onDoneReorder: () -> Unit
) {
    val context = LocalContext.current
    val trackCount = playlist.tracks.size.takeIf { it > 0 } ?: playlist.totalTracks

    // Format subtitle like Screenshot_20261020_165435: "user, 22 lagu"
    val authorLine = remember(playlist.authorSubtitle, playlist.subtitle, username, trackCount) {
        val raw = playlist.authorSubtitle.ifBlank { playlist.subtitle }
        if (raw.contains("lagu", ignoreCase = true) && !raw.contains("•")) {
            raw.replace("Qiaa", username, ignoreCase = true)
        } else {
            "$username, $trackCount lagu"
        }
    }

    val metaLine = remember(trackCount, playlist.isAlbum) {
        val kind = if (playlist.isAlbum) "ALBUM" else "PLAYLIST"
        "$kind • $trackCount TRACKS"
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Full-width top cover image with seamless blur & opacity fade (no hard border line!)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(386.dp)
        ) {
            if (coverUrl.isNotBlank()) {
                // 1. Blurred ambient backdrop so edges & bottom transition seamlessly
                AsyncImage(
                    model = remember(coverUrl) {
                        ImageRequest.Builder(context)
                            .data(coverUrl)
                            .size(Size(400, 400))
                            .crossfade(false)
                            .build()
                    },
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(32.dp)
                )

                // 2. Full-width main cover artwork at the top
                AsyncImage(
                    model = remember(coverUrl) {
                        ImageRequest.Builder(context)
                            .data(coverUrl)
                            .size(Size(800, 800))
                            .crossfade(false)
                            .build()
                    },
                    contentDescription = playlist.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(pageBaseTint.lightenTowardWhite(0.2f), pageBaseTint)
                            )
                        )
                )
            }

            // 3. Soft blurred bottom feather strip so there is zero visible seam line
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.0f to Color.Black.copy(alpha = 0.18f),
                            0.35f to Color.Transparent,
                            0.58f to pageBaseTint.copy(alpha = 0.38f),
                            0.76f to pageBaseTint.copy(alpha = 0.78f),
                            0.90f to pageBaseTint.copy(alpha = 0.95f),
                            1.0f to pageBaseTint
                        )
                    )
            )

            // Back Button in Top-Left
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.28f))
                        .testTag("playlist_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        tint = TextPrimaryWhite,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Title + Author Subtitle + PLAYLIST • X TRACKS anchored cleanly in the seamless fade zone
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = playlist.title,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 30.sp,
                    lineHeight = 35.sp,
                    letterSpacing = (-0.6).sp,
                    color = TextPrimaryWhite,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = authorLine,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.5.sp,
                    color = subtitleCoralTint,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = metaLine,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.5.sp,
                    letterSpacing = 0.9.sp,
                    color = TextPrimaryWhite.copy(alpha = 0.62f),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 4 Circular Action Buttons Row: Shuffle, Large White Play, Search, 3-Dots (...)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Shuffle Button
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(actionCircleColor.copy(alpha = 0.85f))
                    .clickable(onClick = onShufflePlayClick)
                    .testTag("playlist_shuffle_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shuffle,
                    contentDescription = "Shuffle",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(18.dp))

            // 2. Large White Play Button
            val playIconDarkTint = remember(pageBaseTint) {
                if (pageBaseTint.luminance() > 0.5f) Color(0xFF18181C) else pageBaseTint.darken(0.65f)
            }
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .shadow(14.dp, CircleShape)
                    .clip(CircleShape)
                    .background(Color(0xFFFDF7F7))
                    .clickable(onClick = onPlayAllClick)
                    .testTag("playlist_play_all_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Putar Playlist",
                    tint = playIconDarkTint,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.width(18.dp))

            // 3. Search Button
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSearchVisible) Color.White.copy(alpha = 0.28f)
                        else actionCircleColor.copy(alpha = 0.85f)
                    )
                    .clickable(onClick = onToggleSearchClick)
                    .testTag("playlist_search_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Cari di playlist",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(18.dp))

            // 4. More (...) Button
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(actionCircleColor.copy(alpha = 0.85f))
                    .clickable(onClick = onOpenMoreMenu)
                    .testTag("playlist_more_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MoreHoriz,
                    contentDescription = "Menu Playlist",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Reorder Mode Banner (when user selects "Reorder songs" from the 3-dot menu)
        AnimatedVisibility(
            visible = isReorderMode,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.Black.copy(alpha = 0.38f))
                    .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.SwapVert,
                        contentDescription = null,
                        tint = subtitleCoralTint,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Mode Urutkan Lagu Aktif",
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.5.sp,
                        color = TextPrimaryWhite
                    )
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.White,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable(onClick = onDoneReorder)
                        .testTag("playlist_reorder_done_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Selesai",
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp,
                            color = Color.Black
                        )
                    }
                }
            }
        }

        // Expandable In-Playlist Search Bar
        AnimatedVisibility(
            visible = isSearchVisible,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
                    .height(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.Black.copy(alpha = 0.32f))
                    .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(24.dp))
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = TextPrimaryWhite.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Cari lagu atau artis di playlist ini...",
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = TextPrimaryWhite.copy(alpha = 0.5f)
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            singleLine = true,
                            textStyle = TextStyle(
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = TextPrimaryWhite
                            ),
                            cursorBrush = SolidColor(TextPrimaryWhite),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("playlist_search_input")
                        )
                    }
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { onSearchQueryChange("") },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Hapus pencarian",
                                tint = TextPrimaryWhite.copy(alpha = 0.75f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
    }
}

@Composable
private fun PlaylistTrackRowItem(
    track: MusicItem,
    fallbackCoverUrl: String = "",
    isCurrentlyPlaying: Boolean,
    isPlayingAudio: Boolean,
    isDownloaded: Boolean,
    activeDownloadPercent: Int?,
    isReorderMode: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    val context = LocalContext.current
    val rowBg = if (isCurrentlyPlaying) {
        Color.White.copy(alpha = 0.12f)
    } else {
        Color.Transparent
    }

    val cleanVid = remember(track.videoId, track.id) {
        (track.videoId ?: track.id).takeIf {
            it.isNotBlank() && !it.startsWith("UC") && !it.startsWith("VL") && !it.startsWith("MPRE")
        }
    }
    val backupUrl = remember(cleanVid, fallbackCoverUrl) {
        if (cleanVid != null) "https://i.ytimg.com/vi/$cleanVid/mqdefault.jpg"
        else fallbackCoverUrl
    }
    val primaryUrl = remember(track.thumbnailUrl, cleanVid, fallbackCoverUrl) {
        track.highResThumbnail.ifBlank {
            if (cleanVid != null) "https://i.ytimg.com/vi/$cleanVid/hqdefault.jpg"
            else fallbackCoverUrl
        }
    }
    var useBackupThumb by remember(primaryUrl) { mutableStateOf(false) }
    val activeThumbUrl = if (useBackupThumb && backupUrl.isNotBlank()) backupUrl else primaryUrl

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(rowBg)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .testTag("playlist_track_row_${track.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 54dp Rounded Square Thumbnail with authentic per-song cover image
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(CardPlaceholderDark),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = remember(activeThumbUrl) {
                    ImageRequest.Builder(context)
                        .data(activeThumbUrl)
                        .size(Size(160, 160))
                        .crossfade(false)
                        .build()
                },
                contentDescription = track.title,
                contentScale = ContentScale.Crop,
                onError = {
                    if (!useBackupThumb && backupUrl.isNotBlank() && backupUrl != primaryUrl) {
                        useBackupThumb = true
                    }
                },
                modifier = Modifier.matchParentSize()
            )

            if (isCurrentlyPlaying) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color.Black.copy(alpha = 0.50f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlayingAudio) Icons.Default.GraphicEq else Icons.Default.Pause,
                        contentDescription = "Sedang Diputar",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Track Title + Artist Subtitle
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = track.title,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                lineHeight = 20.sp,
                color = TextPrimaryWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(3.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (activeDownloadPercent != null) {
                    Text(
                        text = "Mengunduh $activeDownloadPercent% •",
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        color = Color(0xFF38EF7D)
                    )
                } else if (isDownloaded) {
                    Icon(
                        imageVector = Icons.Default.DownloadDone,
                        contentDescription = "Tersimpan Offline",
                        tint = Color(0xFF38EF7D),
                        modifier = Modifier.size(14.dp)
                    )
                }

                Text(
                    text = track.cleanArtist.ifBlank { track.subtitle },
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.5.sp,
                    lineHeight = 17.sp,
                    color = TextPrimaryWhite.copy(alpha = 0.72f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (isReorderMode) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onMoveUp,
                    enabled = canMoveUp,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowUpward,
                        contentDescription = "Geser Naik",
                        tint = if (canMoveUp) Color.White else Color.White.copy(alpha = 0.25f),
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = onMoveDown,
                    enabled = canMoveDown,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowDownward,
                        contentDescription = "Geser Turun",
                        tint = if (canMoveDown) Color.White else Color.White.copy(alpha = 0.25f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        } else {
            IconButton(
                onClick = onMoreClick,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("playlist_track_more_${track.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Opsi lagu ${track.title}",
                    tint = TextPrimaryWhite.copy(alpha = 0.75f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * 3-Dots (...) Playlist Options ModalBottomSheet matching Screenshot_20261020_165442:
 * - Top header with thumbnail, title, and "user, X lagu"
 * - Grouped dark rounded container with:
 *   1. Play next
 *   2. Add to queue
 *   3. Download all
 *   4. Share
 *   5. Pin
 *   6. Rename
 *   7. Reorder songs
 *   8. Delete playlist
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaylistOptionsReferenceSheet(
    playlist: PlaylistDetail,
    coverUrl: String,
    username: String,
    isPinned: Boolean,
    isReorderMode: Boolean,
    onDismiss: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onDownloadAll: () -> Unit,
    onShare: () -> Unit,
    onTogglePin: () -> Unit,
    onRename: () -> Unit,
    onReorderSongs: () -> Unit,
    onDeletePlaylist: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val trackCount = playlist.tracks.size.takeIf { it > 0 } ?: playlist.totalTracks

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF130F10),
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 28.dp)
        ) {
            // Header: Cover Thumbnail + Playlist Title + "user, X lagu"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = coverUrl,
                    contentDescription = playlist.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(CardPlaceholderDark)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = playlist.title,
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 17.sp,
                        color = Color(0xFFF3ECEE),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$username, $trackCount lagu",
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.5.sp,
                        color = Color(0xFFA89FA2),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            HorizontalDivider(
                color = Color.White.copy(alpha = 0.08f),
                thickness = 0.8.dp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Rounded Group Container matching Screenshot_20261020_165442
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0xFF1E181A))
            ) {
                PlaylistReferenceMenuRow(
                    icon = Icons.AutoMirrored.Filled.PlaylistPlay,
                    label = "Play next",
                    onClick = onPlayNext,
                    testTag = "playlist_sheet_play_next"
                )
                MenuDivider()
                PlaylistReferenceMenuRow(
                    icon = Icons.AutoMirrored.Filled.QueueMusic,
                    label = "Add to queue",
                    onClick = onAddToQueue,
                    testTag = "playlist_sheet_add_to_queue"
                )
                MenuDivider()
                PlaylistReferenceMenuRow(
                    icon = Icons.Default.Download,
                    label = "Download all",
                    onClick = onDownloadAll,
                    testTag = "playlist_sheet_download_all"
                )
                MenuDivider()
                PlaylistReferenceMenuRow(
                    icon = Icons.Default.Share,
                    label = "Share",
                    onClick = onShare,
                    testTag = "playlist_sheet_share"
                )
                MenuDivider()
                PlaylistReferenceMenuRow(
                    icon = Icons.Default.PushPin,
                    label = if (isPinned) "Unpin" else "Pin",
                    onClick = onTogglePin,
                    testTag = "playlist_sheet_pin"
                )
                MenuDivider()
                PlaylistReferenceMenuRow(
                    icon = Icons.Default.Edit,
                    label = "Rename",
                    onClick = onRename,
                    testTag = "playlist_sheet_rename"
                )
                MenuDivider()
                PlaylistReferenceMenuRow(
                    icon = Icons.Default.SwapVert,
                    label = if (isReorderMode) "Done reordering" else "Reorder songs",
                    onClick = onReorderSongs,
                    testTag = "playlist_sheet_reorder"
                )
                MenuDivider()
                PlaylistReferenceMenuRow(
                    icon = Icons.Default.Delete,
                    label = "Delete playlist",
                    onClick = onDeletePlaylist,
                    testTag = "playlist_sheet_delete"
                )
            }
        }
    }
}

@Composable
private fun MenuDivider() {
    HorizontalDivider(
        color = Color.White.copy(alpha = 0.06f),
        thickness = 0.7.dp,
        modifier = Modifier.padding(horizontal = 18.dp)
    )
}

@Composable
private fun PlaylistReferenceMenuRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Color(0xFFE6DFE1),
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(18.dp))
        Text(
            text = label,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 15.5.sp,
            color = Color(0xFFF3ECEE)
        )
    }
}

@Composable
private fun RenamePlaylistInlineDialog(
    currentTitle: String,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit
) {
    var titleInput by remember(currentTitle) { mutableStateOf(currentTitle) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1A22),
        titleContentColor = Color.White,
        textContentColor = Color.White,
        title = {
            Text(
                text = "Rename Playlist",
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 19.sp
            )
        },
        text = {
            OutlinedTextField(
                value = titleInput,
                onValueChange = { titleInput = it },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF8AB4F8),
                    unfocusedBorderColor = Color(0xFF3A3A48)
                ),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    val clean = titleInput.trim()
                    if (clean.isNotBlank()) onRename(clean)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF8AB4F8),
                    contentColor = Color(0xFF10131A)
                )
            ) {
                Text(
                    text = "Simpan",
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Batal",
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }
    )
}
