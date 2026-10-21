package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.ArtistProfileDetail
import com.example.data.model.ArtistReleaseTab
import com.example.data.model.MusicItem
import com.example.ui.theme.CardPlaceholderDark
import com.example.ui.theme.OledBlack
import com.example.ui.theme.PlusJakartaSans
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextPrimaryWhite
import com.example.ui.theme.TextSecondaryGrey
import com.example.ui.theme.rememberDominantAccentColor

@Composable
fun ArtistProfileScreen(
    artistProfile: ArtistProfileDetail?,
    isLoading: Boolean,
    isFollowing: Boolean,
    currentPlayingTrackId: String?,
    favoriteIds: Set<String>,
    hasActiveMiniPlayer: Boolean,
    onBack: () -> Unit,
    onToggleFollow: (String) -> Unit,
    onPlayTrack: (MusicItem, List<MusicItem>, String) -> Unit,
    onOpenReleaseOrPlaylist: (MusicItem) -> Unit,
    onPlayReleaseDirect: (MusicItem) -> Unit,
    onOpenRelatedArtist: (MusicItem) -> Unit,
    onToggleFavoriteTrack: (MusicItem) -> Unit,
    onOpenTrackMenu: (MusicItem) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    var selectedReleaseTab by remember { mutableStateOf(ArtistReleaseTab.ALL) }
    var showAllTopSongs by remember { mutableStateOf(false) }

    if (artistProfile == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(OledBlack),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color(0xFFE94057))
        }
        return
    }

    val extractedAccent = rememberDominantAccentColor(
        imageUrl = artistProfile.avatarUrl.ifBlank { artistProfile.headerImageUrl },
        defaultColor = Color(0xFFE94057)
    )
    val dynamicAccent by animateColorAsState(
        targetValue = extractedAccent,
        animationSpec = tween(durationMillis = 550),
        label = "artist_dynamic_accent"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(OledBlack)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("artist_profile_lazy_column"),
            contentPadding = PaddingValues(
                bottom = if (hasActiveMiniPlayer) 176.dp else 112.dp
            )
        ) {
            // 1. Immersive Dynamic-Color Hero Header
            item(key = "artist_hero_header") {
                ArtistHeroHeaderSection(
                    artist = artistProfile,
                    dynamicAccent = dynamicAccent,
                    isFollowing = isFollowing,
                    isLoading = isLoading,
                    onBack = onBack,
                    onToggleFollow = { onToggleFollow(artistProfile.browseId) },
                    onPlayAllTop = {
                        val first = artistProfile.topSongs.firstOrNull()
                        if (first != null) {
                            onPlayTrack(first, artistProfile.topSongs, "Playing from ${artistProfile.name}")
                        }
                    },
                    onShuffleTop = {
                        val shuffled = artistProfile.topSongs.shuffled()
                        val first = shuffled.firstOrNull()
                        if (first != null) {
                            onPlayTrack(first, shuffled, "Shuffle • ${artistProfile.name}")
                        }
                    },
                    onStartRadio = {
                        val radioSongs = artistProfile.topSongs.shuffled()
                        val first = radioSongs.firstOrNull()
                        if (first != null) {
                            onPlayTrack(first, radioSongs, "Radio • ${artistProfile.name}")
                        }
                    }
                )
            }

            // 2. Highlight Rilis Terbaru
            artistProfile.latestRelease?.let { latest ->
                item(key = "artist_latest_release_spotlight") {
                    LatestReleaseSpotlightCard(
                        release = latest,
                        artistName = artistProfile.name,
                        accentColor = dynamicAccent,
                        onOpenDetail = { onOpenReleaseOrPlaylist(latest) }
                    )
                }
            }

            // 3. Lagu Populer (Top Songs)
            if (artistProfile.topSongs.isNotEmpty()) {
                item(key = "artist_top_songs_header") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 20.dp, top = 26.dp, bottom = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Lagu",
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 22.sp,
                                letterSpacing = (-0.5).sp,
                                color = TextPrimaryWhite
                            )
                            Text(
                                text = "Karya terpopuler dari ${artistProfile.name}",
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.5.sp,
                                color = TextSecondaryGrey
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = dynamicAccent.copy(alpha = 0.16f),
                                modifier = Modifier
                                    .border(1.dp, dynamicAccent.copy(alpha = 0.45f), RoundedCornerShape(50))
                                    .clip(RoundedCornerShape(50))
                                    .clickable {
                                        val first = artistProfile.topSongs.firstOrNull()
                                        if (first != null) {
                                            onPlayTrack(first, artistProfile.topSongs, "Playing from ${artistProfile.name}")
                                        }
                                    }
                            ) {
                                Text(
                                    text = "Putar semua",
                                    fontFamily = PlusJakartaSans,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.5.sp,
                                    color = TextPrimaryWhite,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }
                }

                val visibleSongs = if (showAllTopSongs) {
                    artistProfile.topSongs
                } else {
                    artistProfile.topSongs.take(5)
                }

                itemsIndexed(
                    items = visibleSongs,
                    key = { idx, song -> "top_song_${song.id}_$idx" }
                ) { index, song ->
                    val isPlayingThis = currentPlayingTrackId == song.id ||
                        (song.videoId != null && currentPlayingTrackId == song.videoId)
                    val isFav = favoriteIds.contains(song.id) ||
                        (song.videoId != null && favoriteIds.contains(song.videoId))

                    ArtistTopSongRowItem(
                        rank = index + 1,
                        song = song,
                        isPlaying = isPlayingThis,
                        isFavorite = isFav,
                        accentColor = dynamicAccent,
                        onClick = {
                            onPlayTrack(song, artistProfile.topSongs, "Playing from ${artistProfile.name}")
                        },
                        onToggleFavorite = { onToggleFavoriteTrack(song) },
                        onMoreClick = { onOpenTrackMenu(song) }
                    )
                }

                if (artistProfile.topSongs.size > 5) {
                    item(key = "artist_top_songs_expand") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 8.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color(0xFF1A1A20),
                                modifier = Modifier
                                    .border(1.dp, Color(0xFF2E2E38), RoundedCornerShape(50))
                                    .clip(RoundedCornerShape(50))
                                    .clickable { showAllTopSongs = !showAllTopSongs }
                                    .testTag("artist_toggle_all_songs_button")
                            ) {
                                Text(
                                    text = if (showAllTopSongs) "Tampilkan lebih sedikit" else "Tampilkan semua (${artistProfile.topSongs.size} lagu)",
                                    fontFamily = PlusJakartaSans,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = TextPrimaryWhite,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 4. Album & Single — Full-bleed image cards with seamless blurred opacity overlay & no Play button
            if (artistProfile.albums.isNotEmpty() || artistProfile.singlesAndEps.isNotEmpty()) {
                item(key = "artist_discography_section") {
                    ArtistDiscographySection(
                        albums = artistProfile.albums,
                        singlesAndEps = artistProfile.singlesAndEps,
                        selectedTab = selectedReleaseTab,
                        accentColor = dynamicAccent,
                        onSelectTab = { selectedReleaseTab = it },
                        onOpenRelease = onOpenReleaseOrPlaylist
                    )
                }
            }

            // 5. Video Musik (16:9 Widescreen Cards with guaranteed valid YouTube thumbnails)
            if (artistProfile.videos.isNotEmpty()) {
                item(key = "artist_videos_section") {
                    ArtistVideosSection(
                        videos = artistProfile.videos,
                        fallbackArtistImage = artistProfile.headerImageUrl.ifBlank { artistProfile.avatarUrl },
                        accentColor = dynamicAccent,
                        onVideoClick = { video ->
                            onPlayTrack(video, artistProfile.videos, "Video • ${artistProfile.name}")
                        }
                    )
                }
            }

            // 6. Menampilkan Artis / Playlist Kurasi & Radio Terbaik
            if (artistProfile.featuredPlaylists.isNotEmpty()) {
                item(key = "artist_featured_playlists_section") {
                    ArtistFeaturedPlaylistsSection(
                        artistName = artistProfile.name,
                        playlists = artistProfile.featuredPlaylists,
                        accentColor = dynamicAccent,
                        onPlaylistClick = { playlist ->
                            val enriched = if (playlist.previewTracks.isEmpty() && artistProfile.topSongs.isNotEmpty()) {
                                playlist.copy(previewTracks = artistProfile.topSongs)
                            } else {
                                playlist
                            }
                            onOpenReleaseOrPlaylist(enriched)
                        }
                    )
                }
            }

            // 7. "Penggemar mungkin juga suka" — Strictly Circular Artist Avatars
            if (artistProfile.relatedArtists.isNotEmpty()) {
                item(key = "artist_fans_also_like_section") {
                    FansMightAlsoLikeCircularSection(
                        relatedArtists = artistProfile.relatedArtists,
                        accentColor = dynamicAccent,
                        onArtistClick = onOpenRelatedArtist
                    )
                }
            }

            // 8. Tentang Artis — Handcrafted Full-Stack Editorial Section (No Boxed Overlay Slop)
            if (!artistProfile.description.isNullOrBlank()) {
                item(key = "artist_about_card") {
                    ArtistAboutBioSection(
                        artist = artistProfile,
                        accentColor = dynamicAccent
                    )
                }
            }
        }
    }
}

@Composable
private fun ArtistHeroHeaderSection(
    artist: ArtistProfileDetail,
    dynamicAccent: Color,
    isFollowing: Boolean,
    isLoading: Boolean,
    onBack: () -> Unit,
    onToggleFollow: () -> Unit,
    onPlayAllTop: () -> Unit,
    onShuffleTop: () -> Unit,
    onStartRadio: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(OledBlack)
    ) {
        AsyncImage(
            model = artist.headerImageUrl.ifBlank { artist.avatarUrl },
            contentDescription = artist.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(390.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(390.dp)
                .background(
                    Brush.verticalGradient(
                        0.0f to OledBlack.copy(alpha = 0.45f),
                        0.38f to dynamicAccent.copy(alpha = 0.36f),
                        0.76f to dynamicAccent.copy(alpha = 0.62f),
                        1.0f to OledBlack
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(OledBlack.copy(alpha = 0.62f))
                        .border(1.dp, Color.White.copy(alpha = 0.16f), CircleShape)
                        .testTag("artist_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        tint = TextPrimaryWhite
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = TextPrimaryWhite,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = OledBlack.copy(alpha = 0.62f),
                        modifier = Modifier.border(1.dp, dynamicAccent.copy(alpha = 0.55f), RoundedCornerShape(50))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = "Artis Resmi",
                                tint = dynamicAccent,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ARTIS RESMI",
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 10.5.sp,
                                letterSpacing = 0.6.sp,
                                color = TextPrimaryWhite
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(110.dp))

            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.fillMaxWidth()
            ) {
                AsyncImage(
                    model = artist.avatarUrl.ifBlank { artist.headerImageUrl },
                    contentDescription = artist.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(88.dp)
                        .shadow(16.dp, CircleShape)
                        .clip(CircleShape)
                        .border(2.5.dp, dynamicAccent, CircleShape)
                        .background(CardPlaceholderDark)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = artist.name,
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 32.sp,
                        lineHeight = 36.sp,
                        letterSpacing = (-0.8).sp,
                        color = TextPrimaryWhite,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${artist.subscribersText} • ${artist.monthlyListenersText}",
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.84f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (isFollowing) dynamicAccent.copy(alpha = 0.24f) else Color.White.copy(alpha = 0.12f),
                        modifier = Modifier
                            .border(
                                width = 1.2.dp,
                                color = if (isFollowing) dynamicAccent else Color.White.copy(alpha = 0.28f),
                                shape = RoundedCornerShape(50)
                            )
                            .clip(RoundedCornerShape(50))
                            .clickable(onClick = onToggleFollow)
                            .testTag("artist_follow_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isFollowing) Icons.Default.Check else Icons.Default.PersonAdd,
                                contentDescription = null,
                                tint = TextPrimaryWhite,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isFollowing) "Disubscribe" else "Subscribe",
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                color = TextPrimaryWhite
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color.White.copy(alpha = 0.10f),
                        modifier = Modifier
                            .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(50))
                            .clip(RoundedCornerShape(50))
                            .clickable(onClick = onStartRadio)
                            .testTag("artist_radio_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Radio,
                                contentDescription = "Radio",
                                tint = TextPrimaryWhite,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Radio",
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                color = TextPrimaryWhite
                            )
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onShuffleTop,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.12f))
                            .border(1.dp, Color.White.copy(alpha = 0.22f), CircleShape)
                            .testTag("artist_shuffle_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = "Acak",
                            tint = TextPrimaryWhite,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onPlayAllTop,
                        modifier = Modifier
                            .size(54.dp)
                            .shadow(12.dp, CircleShape)
                            .clip(CircleShape)
                            .background(TextPrimaryWhite)
                            .testTag("artist_play_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Putar",
                            tint = OledBlack,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LatestReleaseSpotlightCard(
    release: MusicItem,
    artistName: String,
    accentColor: Color,
    onOpenDetail: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(SurfaceDark)
            .clickable(onClick = onOpenDetail)
            .testTag("artist_latest_release_card")
    ) {
        // Soft ambient blurred background from release cover
        AsyncImage(
            model = release.highResThumbnail,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .matchParentSize()
                .blur(28.dp)
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            OledBlack.copy(alpha = 0.72f),
                            OledBlack.copy(alpha = 0.86f),
                            OledBlack.copy(alpha = 0.94f)
                        )
                    )
                )
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(104.dp)
                    .height(86.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Box(
                    modifier = Modifier
                        .size(74.dp)
                        .offset(x = 24.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0E0E12)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(accentColor)
                    )
                }

                AsyncImage(
                    model = release.highResThumbnail,
                    contentDescription = release.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(84.dp)
                        .shadow(10.dp, RoundedCornerShape(14.dp))
                        .clip(RoundedCornerShape(14.dp))
                        .background(CardPlaceholderDark)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "RILIS TERBARU",
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 10.sp,
                    letterSpacing = 0.8.sp,
                    color = accentColor
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = release.title,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp,
                    color = TextPrimaryWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = release.subtitle.ifBlank { "Rilisan Unggulan • $artistName" },
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.5.sp,
                    color = TextSecondaryGrey,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ArtistTopSongRowItem(
    rank: Int,
    song: MusicItem,
    isPlaying: Boolean,
    isFavorite: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onMoreClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 3.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (isPlaying) accentColor.copy(alpha = 0.16f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .testTag("artist_top_song_${song.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$rank",
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 14.sp,
            color = if (isPlaying) accentColor else TextSecondaryGrey,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(26.dp)
        )

        Spacer(modifier = Modifier.width(6.dp))

        AsyncImage(
            model = song.smallThumbnail,
            contentDescription = song.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(CardPlaceholderDark)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.5.sp,
                color = if (isPlaying) accentColor else TextPrimaryWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            val metaText = buildString {
                append(song.cleanArtist)
                song.playsText?.takeIf { it.isNotBlank() }?.let { append(" • $it") }
                    ?: song.duration?.takeIf { it.isNotBlank() }?.let { append(" • $it") }
            }
            Text(
                text = metaText,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.5.sp,
                color = TextSecondaryGrey,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(
            onClick = onToggleFavorite,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = "Favorit",
                tint = if (isFavorite) Color(0xFFFF4D67) else TextSecondaryGrey,
                modifier = Modifier.size(20.dp)
            )
        }

        IconButton(
            onClick = onMoreClick,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Opsi Lagu",
                tint = TextSecondaryGrey,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun ArtistDiscographySection(
    albums: List<MusicItem>,
    singlesAndEps: List<MusicItem>,
    selectedTab: ArtistReleaseTab,
    accentColor: Color,
    onSelectTab: (ArtistReleaseTab) -> Unit,
    onOpenRelease: (MusicItem) -> Unit
) {
    val displayedReleases = remember(albums, singlesAndEps, selectedTab) {
        when (selectedTab) {
            ArtistReleaseTab.ALL -> (albums + singlesAndEps).distinctBy { it.title.lowercase() }
            ArtistReleaseTab.ALBUMS -> albums
            ArtistReleaseTab.SINGLES -> singlesAndEps
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 26.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Album & Single",
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp,
                    letterSpacing = (-0.5).sp,
                    color = TextPrimaryWhite
                )
                Text(
                    text = "Diskografi resmi dengan rilisan pilihan",
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.5.sp,
                    color = TextSecondaryGrey
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ArtistReleaseTab.entries.forEach { tab ->
                val isSelected = selectedTab == tab
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (isSelected) accentColor else Color(0xFF1A1A20),
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable { onSelectTab(tab) }
                        .testTag("artist_release_tab_${tab.name.lowercase()}")
                ) {
                    Text(
                        text = tab.label,
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.5.sp,
                        color = TextPrimaryWhite,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            itemsIndexed(
                items = displayedReleases,
                key = { idx, item -> "discog_${item.id}_$idx" }
            ) { index, release ->
                FullBleedSeamlessDiscographyCard(
                    release = release,
                    isFeaturedWide = index == 0,
                    onClick = { onOpenRelease(release) }
                )
            }
        }
    }
}

/**
 * Full-bleed cover card where the artwork fills the entire card and seamlessly melts
 * into a soft blurred/opacity gradient at the bottom without any hard border lines or play button.
 */
@Composable
private fun FullBleedSeamlessDiscographyCard(
    release: MusicItem,
    isFeaturedWide: Boolean,
    onClick: () -> Unit
) {
    val cardWidth = if (isFeaturedWide) 224.dp else 168.dp
    val cardHeight = 224.dp

    Box(
        modifier = Modifier
            .width(cardWidth)
            .height(cardHeight)
            .clip(RoundedCornerShape(20.dp))
            .background(CardPlaceholderDark)
            .clickable(onClick = onClick)
            .testTag("artist_discography_card_${release.id}")
    ) {
        // 1. Ambient blurred background layer for seamless bottom blend
        AsyncImage(
            model = release.highResThumbnail,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .blur(22.dp)
        )

        // 2. Crisp full-bleed artwork filling the box
        AsyncImage(
            model = release.highResThumbnail,
            contentDescription = release.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // 3. Seamless multi-stop opacity gradient (no border line, blends smoothly into bottom)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Transparent,
                        0.42f to Color.Transparent,
                        0.68f to OledBlack.copy(alpha = 0.52f),
                        0.86f to OledBlack.copy(alpha = 0.84f),
                        1.0f to OledBlack.copy(alpha = 0.96f)
                    )
                )
        )

        // 4. Clean title & subtitle anchored at the bottom (no play button overlay)
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp)
        ) {
            Text(
                text = release.title,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.5.sp,
                color = TextPrimaryWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = release.subtitle.ifBlank { "Album" },
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.76f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ArtistVideosSection(
    videos: List<MusicItem>,
    fallbackArtistImage: String,
    accentColor: Color,
    onVideoClick: (MusicItem) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 26.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Text(
                text = "Video",
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 22.sp,
                letterSpacing = (-0.5).sp,
                color = TextPrimaryWhite
            )
            Text(
                text = "Ketuk untuk memutar • Ubah ke Mode Video di titik 3 (⋮) pemutar",
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.5.sp,
                color = TextSecondaryGrey
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(videos, key = { "vid_${it.id}" }) { video ->
                val rawVidId = (video.videoId ?: video.id).removePrefix("vid_")
                val backupThumb = remember(rawVidId, fallbackArtistImage) {
                    if (rawVidId.length == 11) "https://i.ytimg.com/vi/$rawVidId/mqdefault.jpg"
                    else fallbackArtistImage
                }
                val primaryThumb = when {
                    video.thumbnailUrl.isNotBlank() -> video.highResThumbnail
                    rawVidId.length == 11 -> "https://i.ytimg.com/vi/$rawVidId/hqdefault.jpg"
                    else -> fallbackArtistImage
                }
                var useBackup by remember(primaryThumb) { androidx.compose.runtime.mutableStateOf(false) }
                val effectiveThumb = if (useBackup && backupThumb.isNotBlank()) backupThumb else primaryThumb

                Column(
                    modifier = Modifier
                        .width(236.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            onVideoClick(
                                if (video.thumbnailUrl.isBlank()) video.copy(thumbnailUrl = effectiveThumb)
                                else video
                            )
                        }
                        .testTag("artist_video_card_${video.id}")
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(134.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(CardPlaceholderDark)
                    ) {
                        AsyncImage(
                            model = effectiveThumb,
                            contentDescription = video.title,
                            contentScale = ContentScale.Crop,
                            onError = {
                                if (!useBackup && backupThumb.isNotBlank() && backupThumb != primaryThumb) {
                                    useBackup = true
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            OledBlack.copy(alpha = 0.65f)
                                        )
                                    )
                                )
                        )
                        video.duration?.takeIf { it.isNotBlank() }?.let { dur ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = OledBlack.copy(alpha = 0.78f),
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = dur,
                                    fontFamily = PlusJakartaSans,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = TextPrimaryWhite,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = video.title,
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.5.sp,
                        color = TextPrimaryWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = video.subtitle,
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = TextSecondaryGrey,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun ArtistFeaturedPlaylistsSection(
    artistName: String,
    playlists: List<MusicItem>,
    accentColor: Color,
    onPlaylistClick: (MusicItem) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 26.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Text(
                text = "Menampilkan $artistName",
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 22.sp,
                letterSpacing = (-0.5).sp,
                color = TextPrimaryWhite
            )
            Text(
                text = "Playlist kurasi & radio terbaik",
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.5.sp,
                color = TextSecondaryGrey
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(playlists, key = { "feat_pl_${it.id}" }) { playlist ->
                Box(
                    modifier = Modifier
                        .width(172.dp)
                        .height(216.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(CardPlaceholderDark)
                        .clickable { onPlaylistClick(playlist) }
                        .testTag("artist_featured_playlist_${playlist.id}")
                ) {
                    AsyncImage(
                        model = playlist.highResThumbnail,
                        contentDescription = playlist.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    0.0f to Color.Transparent,
                                    0.45f to Color.Transparent,
                                    0.75f to OledBlack.copy(alpha = 0.68f),
                                    1.0f to OledBlack.copy(alpha = 0.95f)
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Text(
                            text = playlist.title,
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.5.sp,
                            color = TextPrimaryWhite,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = playlist.subtitle.ifBlank { "Playlist Kurasi" },
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.5.sp,
                            color = accentColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FansMightAlsoLikeCircularSection(
    relatedArtists: List<MusicItem>,
    accentColor: Color,
    onArtistClick: (MusicItem) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 28.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Penggemar mungkin juga suka",
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp,
                    letterSpacing = (-0.5).sp,
                    color = TextPrimaryWhite
                )
                Text(
                    text = "Artis serupa yang direkomendasikan untuk Anda",
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.5.sp,
                    color = TextSecondaryGrey
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            items(relatedArtists, key = { "rel_artist_${it.id}_${it.title}" }) { artistItem ->
                val backupAvatar = remember(artistItem.title) {
                    com.example.data.remote.CuratedHomeFallback.getArtistAvatarForName(artistItem.title)
                }
                val primaryAvatar = remember(artistItem.thumbnailUrl, backupAvatar) {
                    artistItem.highResThumbnail.ifBlank { backupAvatar }
                }
                var useBackupAvatar by remember(primaryAvatar) { androidx.compose.runtime.mutableStateOf(false) }
                val activeAvatar = if (useBackupAvatar && backupAvatar.isNotBlank()) backupAvatar else primaryAvatar

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(124.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onArtistClick(artistItem) }
                        .padding(vertical = 6.dp)
                        .testTag("related_artist_circular_${artistItem.title}")
                ) {
                    Box(
                        modifier = Modifier
                            .size(112.dp)
                            .shadow(10.dp, CircleShape)
                            .clip(CircleShape)
                            .background(CardPlaceholderDark),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = activeAvatar,
                            contentDescription = artistItem.title,
                            contentScale = ContentScale.Crop,
                            onError = {
                                if (!useBackupAvatar && backupAvatar.isNotBlank() && backupAvatar != primaryAvatar) {
                                    useBackupAvatar = true
                                }
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = artistItem.title,
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.5.sp,
                        color = TextPrimaryWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = artistItem.subtitle.ifBlank { "Artis" },
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = TextSecondaryGrey,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * Clean, human-crafted editorial biography section directly integrated with the page canvas
 * without artificial boxed borders or heavy overlay cards.
 */
@Composable
private fun ArtistAboutBioSection(
    artist: ArtistProfileDetail,
    accentColor: Color
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 28.dp)
            .clickable { expanded = !expanded }
            .testTag("artist_about_section")
    ) {
        HorizontalDivider(
            color = Color.White.copy(alpha = 0.08f),
            thickness = 1.dp,
            modifier = Modifier.padding(bottom = 22.dp)
        )

        Text(
            text = "Tentang ${artist.name}",
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 22.sp,
            letterSpacing = (-0.5).sp,
            color = TextPrimaryWhite
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(28.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = artist.monthlyListenersText.substringBefore(" ").ifBlank { "2.4M" },
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    color = TextPrimaryWhite
                )
                Text(
                    text = "Pendengar Bulanan",
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = TextSecondaryGrey
                )
            }

            Column {
                Text(
                    text = artist.subscribersText.substringBefore(" ").ifBlank { "1.8M" },
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    color = TextPrimaryWhite
                )
                Text(
                    text = "Pelanggan Resmi",
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = TextSecondaryGrey
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = artist.description.orEmpty(),
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.5.sp,
            lineHeight = 22.sp,
            color = Color(0xFFD0D0D8),
            maxLines = if (expanded) 30 else 4,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (expanded) "Tampilkan lebih sedikit" else "Baca selengkapnya",
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 13.sp,
            color = accentColor
        )
    }
}
