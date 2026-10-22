package com.example.ui.components

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.CornerPathEffect
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Tune
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.data.lyrics.LyricLine as BitChordLyricLine
import com.example.data.lyrics.LyricsProviderState
import com.example.data.lyrics.LyricsSettingsState
import com.example.data.lyrics.LyricsSource
import com.example.data.model.ActiveDownloadProgress
import com.example.data.model.LyricLine
import com.example.data.model.LyricsData
import com.example.data.model.LyricsProvider
import com.example.data.model.MusicItem
import com.example.data.model.PlaylistPrivacy
import com.example.data.model.UserLibraryPlaylist
import com.example.player.PlayerUiState
import com.example.ui.home.PlayerSubView
import com.example.ui.library.AddToPlaylistBottomSheet
import com.example.ui.theme.CardPlaceholderDark
import com.example.ui.theme.PlusJakartaSans
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextPrimaryWhite
import com.example.ui.theme.TextSecondaryGrey
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DownloadDone

private val DefaultPlayerWarmColor = Color(0xFF4A483A)
private val ActiveHeartCoral = Color(0xFFFF4D67)
private val playerAmbientColorCache = ConcurrentHashMap<String, Color>()

private fun lerpFloat(start: Float, stop: Float, fraction: Float): Float =
    start + (stop - start) * fraction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullMusicPlayerScreen(
    isVisible: Boolean,
    playerState: PlayerUiState,
    activeSubView: PlayerSubView,
    lyricsData: LyricsData?,
    selectedLyricsProvider: LyricsProvider,
    favoriteIds: Set<String>,
    customCoverOverrides: Map<String, String>,
    isDownloaded: Boolean = false,
    activeDownloadProgress: ActiveDownloadProgress? = null,
    libraryPlaylists: List<UserLibraryPlaylist> = emptyList(),
    isVideoModeActive: Boolean = false,
    activeVideoStreamUrl: String? = null,
    isResolvingVideoStream: Boolean = false,
    onToggleVideoMode: () -> Unit = {},
    onClosePlayer: () -> Unit,
    onChangeSubView: (PlayerSubView) -> Unit,
    onSelectLyricsProvider: (LyricsProvider) -> Unit,
    onApplyLrcHdCover: (MusicItem, String) -> Unit,
    onRestoreDefaultCover: (MusicItem) -> Unit,
    onToggleFavorite: (MusicItem) -> Unit,
    onDownloadTrack: (MusicItem) -> Unit = {},
    onRemoveDownload: (String) -> Unit = {},
    onAddTrackToPlaylist: (playlistId: String, track: MusicItem) -> Unit = { _, _ -> },
    onCreatePlaylistAndAddTrack: (title: String, privacy: PlaylistPrivacy, track: MusicItem) -> Unit = { _, _, _ -> },
    onTogglePlayPause: () -> Unit,
    onNextTrack: () -> Unit,
    onPreviousTrack: () -> Unit,
    onSeekTo: (Float) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onVolumeStep: (Boolean) -> Unit = {},
    onSyncDeviceVolume: () -> Unit = {},
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleAutoPlay: () -> Unit,
    onSelectQueueTrack: (MusicItem) -> Unit,
    onRemoveQueueTrack: (MusicItem) -> Unit,
    onPlayNext: (MusicItem) -> Unit = {},
    onAddToQueue: (MusicItem) -> Unit = {},
    onSetSleepTimerMinutes: (Int, String) -> Unit = { _, _ -> },
    onSetSleepTimerEndOfTrack: () -> Unit = {},
    onCancelSleepTimer: () -> Unit = {},
    onOpenArtistProfile: (String, String) -> Unit = { _, _ -> },
    bitChordLyricsLines: List<BitChordLyricLine> = emptyList(),
    translatedLyricsLines: List<BitChordLyricLine>? = null,
    romanizedLyricsLines: List<BitChordLyricLine>? = null,
    isTranslationEnabled: Boolean = false,
    isRomanizationEnabled: Boolean = false,
    isTranslatingLyrics: Boolean = false,
    activeLyricsSource: LyricsSource? = null,
    isEmbeddedLyrics: Boolean = false,
    isLoadingLyrics: Boolean = false,
    lyricsSettings: LyricsSettingsState = LyricsSettingsState(),
    lyricsProviderStates: Map<LyricsSource, LyricsProviderState> = emptyMap(),
    lyricsProviderResults: Map<LyricsSource, List<BitChordLyricLine>> = emptyMap(),
    onSelectBitChordLyricsSource: (LyricsSource) -> Unit = {},
    onToggleLyricsSourceEnabled: (LyricsSource) -> Unit = {},
    onMoveLyricsSourceOrder: (Int, Int) -> Unit = { _, _ -> },
    onTogglePrioritizeSyllable: (Boolean) -> Unit = {},
    onSavePaxSenixApiKey: (String) -> Unit = {},
    onResetLyricsSourcesDefault: () -> Unit = {},
    onChangeTrackLyricsOffsetMs: (Int) -> Unit = {},
    onToggleLyricsTranslation: () -> Unit = {},
    onToggleLyricsRomanization: () -> Unit = {},
    onSelectTranslationLanguage: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val track = playerState.currentTrack
    if (!isVisible || track == null) return

    var showLyricsProviderSheet by remember { mutableStateOf(false) }
    var showLyricsSourcesDialog by remember { mutableStateOf(false) }
    var showLyricsOffsetSheet by remember { mutableStateOf(false) }
    var showPlayerMoreOptionsSheet by remember { mutableStateOf(false) }
    var showSleepTimerOverlay by remember { mutableStateOf(false) }
    var showAddToPlaylistSheet by remember { mutableStateOf(false) }

    val currentTrackOffsetMs = remember(track.id, lyricsSettings.trackOffsetsMs, lyricsSettings.globalOffsetMs) {
        lyricsSettings.offsetForTrack(track.id)
    }
    val bitChordClockMsState = rememberBitChordLyricClockMs(
        playerState = playerState,
        offsetMs = currentTrackOffsetMs
    )

    BackHandler {
        when {
            showSleepTimerOverlay -> {
                showSleepTimerOverlay = false
                showPlayerMoreOptionsSheet = false
            }
            showPlayerMoreOptionsSheet -> {
                showPlayerMoreOptionsSheet = false
            }
            activeSubView != PlayerSubView.MAIN -> {
                onChangeSubView(PlayerSubView.MAIN)
            }
            else -> {
                onClosePlayer()
            }
        }
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val currentSyncCallback by rememberUpdatedState(onSyncDeviceVolume)

    // Listen to hardware volume button changes so the volume bar stays 100% synced with the device
    DisposableEffect(context) {
        currentSyncCallback()
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                currentSyncCallback()
            }
        }
        val filter = IntentFilter().apply {
            addAction("android.media.VOLUME_CHANGED_ACTION")
            addAction("android.media.MASTER_MUTE_CHANGED_ACTION")
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                @Suppress("UnspecifiedRegisterReceiverFlag")
                context.registerReceiver(receiver, filter)
            }
        } catch (_: Exception) {
        }
        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: Exception) {
            }
        }
    }

    // Active cover URL (uses lrc.red HD cover override if user enabled it in the 3-dot menu)
    val customCoverUrl = customCoverOverrides[track.id]
    val effectiveCoverUrl = remember(track.id, customCoverUrl, track.ultraResThumbnail) {
        customCoverUrl?.takeIf { it.isNotBlank() } ?: track.ultraResThumbnail
    }
    val hasCustomHdCover = !customCoverUrl.isNullOrBlank()
    val availableLrcHdCover = lyricsData?.hdCoverUrl?.takeIf { it.isNotBlank() }

    // Detect if the thumbnail is a 4:3 YouTube letterboxed image (hqdefault/sddefault) so we zoom out black bars
    val isYoutubeLetterboxThumb = remember(effectiveCoverUrl) {
        effectiveCoverUrl.contains("ytimg.com") &&
            (effectiveCoverUrl.contains("hqdefault") || effectiveCoverUrl.contains("sddefault") || effectiveCoverUrl.contains("mqdefault"))
    }

    // Opening morph animation progress (0f -> 1f)
    val openAnim = remember { Animatable(0f) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(isVisible) {
        dragOffsetY = 0f
        if (openAnim.value < 0.99f) {
            openAnim.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
    }

    // Continuous morph progress between Main Player (0f) and Lyrics/Queue Compact Header (1f)
    val isCompactMode = activeSubView != PlayerSubView.MAIN
    val subViewMorph by animateFloatAsState(
        targetValue = if (isCompactMode) 1f else 0f,
        animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
        label = "subview_artwork_title_morph"
    )

    // Cached dominant ambient color sampled from effective cover
    val initialAmbient = remember(effectiveCoverUrl) {
        playerAmbientColorCache[effectiveCoverUrl] ?: DefaultPlayerWarmColor
    }
    var extractedAmbient by remember(effectiveCoverUrl) { mutableStateOf(initialAmbient) }

    LaunchedEffect(effectiveCoverUrl) {
        if (effectiveCoverUrl.isNotBlank()) {
            val cached = playerAmbientColorCache[effectiveCoverUrl]
            if (cached != null) {
                extractedAmbient = cached
            } else {
                val sampled = withContext(Dispatchers.IO) {
                    try {
                        val req = ImageRequest.Builder(context)
                            .data(effectiveCoverUrl)
                            .allowHardware(false)
                            .size(56, 56)
                            .build()
                        val res = context.imageLoader.execute(req)
                        val bmp = (res as? SuccessResult)?.drawable?.let { it as? BitmapDrawable }?.bitmap
                        if (bmp != null) extractPlayerAmbientColor(bmp) else null
                    } catch (_: Exception) {
                        null
                    }
                }
                if (sampled != null) {
                    playerAmbientColorCache[effectiveCoverUrl] = sampled
                    extractedAmbient = sampled
                }
            }
        }
    }

    val ambientColor by animateColorAsState(
        targetValue = extractedAmbient,
        animationSpec = tween(durationMillis = 480),
        label = "player_ambient_color"
    )

    val topAtmosphereColor = remember(ambientColor) {
        lerp(ambientColor, Color(0xFF22201C), 0.14f)
    }
    val midAtmosphereColor = remember(ambientColor) {
        lerp(ambientColor, Color(0xFF181613), 0.34f)
    }
    val bottomAtmosphereColor = remember(ambientColor) {
        lerp(ambientColor, Color(0xFF0E0D0B), 0.58f)
    }

    // Frosted glass button tint harmonized with thumbnail color
    val frostedTint = remember(ambientColor) {
        lerp(ambientColor, Color.White, 0.28f).copy(alpha = 0.38f)
    }
    val frostedBorderTint = remember(ambientColor) {
        lerp(ambientColor, Color.White, 0.52f).copy(alpha = 0.25f)
    }

    val isFavorite = favoriteIds.contains(track.id)

    val heroImageRequest = remember(context, effectiveCoverUrl) {
        ImageRequest.Builder(context)
            .data(effectiveCoverUrl)
            .size(900, 900)
            .allowHardware(true)
            .crossfade(false)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .build()
    }

    // High-frame-rate continuous playback clock for word-by-word karaoke sweep
    val smoothPositionSecState = rememberSmoothPlaybackSeconds(playerState)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .testTag("full_music_player_root")
    ) {
        val containerWidthDp = maxWidth
        val containerHeightDp = maxHeight
        val screenHeightPx = remember(containerHeightDp, density) {
            with(density) { containerHeightDp.toPx().coerceAtLeast(1f) }
        }
        val screenWidthPx = remember(containerWidthDp, density) {
            with(density) { containerWidthDp.toPx().coerceAtLeast(1f) }
        }

        val dragFraction = (dragOffsetY / (screenHeightPx * 0.65f)).coerceIn(0f, 1f)
        val effectiveOpen = (openAnim.value * (1f - dragFraction * 0.42f)).coerceIn(0f, 1f)
        val morphScale = 0.84f + (0.16f * effectiveOpen)
        val morphTranslateX = (1f - openAnim.value) * (-screenWidthPx * 0.24f)
        val morphTranslateY = (1f - openAnim.value) * (screenHeightPx * 0.36f) + dragOffsetY
        val cornerDp = ((1f - effectiveOpen) * 34f).dp

        var mainControlsHeightPx by remember { mutableIntStateOf(0) }
        var titleRowHeightPx by remember { mutableIntStateOf(0) }

        val mainControlsHeightDp = if (mainControlsHeightPx > 0) {
            with(density) { mainControlsHeightPx.toDp() }
        } else {
            330.dp
        }
        val titleRowHeightDp = if (titleRowHeightPx > 0) {
            with(density) { titleRowHeightPx.toDp() }
        } else {
            54.dp
        }

        // Exact top edge of the Lyric Teaser row in Main Player (anchored to bottom of screen)
        val lyricTeaserTopDp = (containerHeightDp - mainControlsHeightDp).coerceAtLeast(300.dp)
        // Position the Title/Artist + Love + 3-Dots row directly above the Lyric Teaser with a clean 14.dp gap
        val mainTitleRowTop = (lyricTeaserTopDp - titleRowHeightDp - 14.dp).coerceAtLeast(230.dp)
        // Full-bleed cover extends down behind the Title/Artist row so its bottom dissolve blends seamlessly
        val heroArtworkHeight = (mainTitleRowTop + titleRowHeightDp + 18.dp).coerceAtLeast(310.dp)
        val statusBarTopPadding = 38.dp

        val animatedArtWidth: Dp = lerp(containerWidthDp, 52.dp, subViewMorph)
        val animatedArtHeight: Dp = lerp(heroArtworkHeight, 52.dp, subViewMorph)
        val animatedArtOffsetX: Dp = lerp(0.dp, 20.dp, subViewMorph)
        val animatedArtOffsetY: Dp = lerp(0.dp, statusBarTopPadding + 22.dp, subViewMorph)
        val animatedArtCorner: Dp = lerp(0.dp, 10.dp, subViewMorph)

        val compactTitleRowTop = statusBarTopPadding + 24.dp
        val animatedTitleOffsetX: Dp = lerp(24.dp, 84.dp, subViewMorph)
        val animatedTitleOffsetY: Dp = lerp(mainTitleRowTop, compactTitleRowTop, subViewMorph)
        val animatedTitleRowWidth: Dp = lerp(containerWidthDp - 48.dp, containerWidthDp - 104.dp, subViewMorph)
        val animatedTitleFontSize = (23f - (5.5f * subViewMorph)).sp
        val animatedArtistFontSize = (16f - (2f * subViewMorph)).sp

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = (0.82f * (1f - dragFraction)).coerceIn(0f, 0.88f)))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset {
                        IntOffset(
                            x = morphTranslateX.roundToInt(),
                            y = morphTranslateY.roundToInt()
                        )
                    }
                    .graphicsLayer {
                        scaleX = morphScale
                        scaleY = morphScale
                        alpha = (0.35f + 0.65f * openAnim.value).coerceIn(0f, 1f)
                    }
                    .clip(RoundedCornerShape(topStart = cornerDp, topEnd = cornerDp))
                    .background(topAtmosphereColor)
                    .pointerInput(activeSubView) {
                        detectVerticalDragGestures(
                            onDragEnd = {
                                if (dragOffsetY > screenHeightPx * 0.18f) {
                                    onClosePlayer()
                                    dragOffsetY = 0f
                                } else {
                                    scope.launch {
                                        Animatable(dragOffsetY).animateTo(
                                            targetValue = 0f,
                                            animationSpec = spring(stiffness = Spring.StiffnessMedium)
                                        ) {
                                            dragOffsetY = value
                                        }
                                    }
                                }
                            },
                            onDragCancel = { dragOffsetY = 0f },
                            onVerticalDrag = { change, dragAmount ->
                                val next = dragOffsetY + dragAmount
                                if (next >= 0f) {
                                    change.consume()
                                    dragOffsetY = next
                                }
                            }
                        )
                    }
            ) {
                // =========================================================================
                // 0. FULL-SCREEN BLURRED ALBUM ARTWORK ATMOSPHERE (Zero-Seam Matching)
                // =========================================================================
                AsyncImage(
                    model = heroImageRequest,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = 1.45f
                            scaleY = 1.45f
                            alpha = 0.86f
                        }
                        .blur(radius = 65.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                )

                // Soft atmosphere overlay so lower controls & lyrics are crisp and legible while preserving seamless cover tones
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawBehind {
                            drawRect(
                                brush = Brush.verticalGradient(
                                    0.00f to topAtmosphereColor.copy(alpha = 0.18f + 0.52f * subViewMorph),
                                    0.44f to topAtmosphereColor.copy(alpha = 0.32f + 0.45f * subViewMorph),
                                    0.68f to midAtmosphereColor.copy(alpha = 0.68f + 0.20f * subViewMorph),
                                    1.00f to bottomAtmosphereColor.copy(alpha = 0.86f)
                                )
                            )
                        }
                )

                val isGlassOverlayActive = showSleepTimerOverlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (isGlassOverlayActive) {
                                Modifier
                                    .blur(radius = 34.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                                    .graphicsLayer { alpha = 0.26f }
                            } else {
                                Modifier
                            }
                        )
                ) {
                // =========================================================================
                // 1. SHARED MORPHING ARTWORK STAGE (0f = Full-Bleed Seamless -> 1f = 52.dp)
                // =========================================================================
                Box(
                    modifier = Modifier
                        .offset(x = animatedArtOffsetX, y = animatedArtOffsetY)
                        .size(width = animatedArtWidth, height = animatedArtHeight)
                        .clip(RoundedCornerShape(animatedArtCorner))
                        .clickable(enabled = isCompactMode) {
                            onChangeSubView(PlayerSubView.MAIN)
                        }
                        .testTag("morphing_player_artwork")
                ) {
                    val dissolveStrength = (1f - subViewMorph * 1.45f).coerceIn(0f, 1f)
                    val coverScaleFactor = if (isYoutubeLetterboxThumb) {
                        lerpFloat(1.34f, 1.15f, subViewMorph)
                    } else {
                        1.0f
                    }

                    if (isVideoModeActive && subViewMorph < 0.65f) {
                        FullBleedMusicVideoStage(
                            videoId = track.videoId ?: track.id,
                            directVideoStreamUrl = activeVideoStreamUrl,
                            isResolvingStream = isResolvingVideoStream,
                            isPlaying = playerState.isPlaying,
                            currentPositionSec = playerState.currentPositionSec.toFloat(),
                            fallbackCoverReq = heroImageRequest,
                            dissolveStrength = dissolveStrength,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        AsyncImage(
                            model = heroImageRequest,
                            contentDescription = track.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    compositingStrategy = CompositingStrategy.Offscreen
                                    scaleX = coverScaleFactor
                                    scaleY = coverScaleFactor
                                }
                                .drawWithContent {
                                    drawContent()
                                    // Seamless bottom alpha dissolve using BlendMode.DstIn so there is ZERO horizontal seam!
                                    if (dissolveStrength > 0.01f) {
                                        drawRect(
                                            brush = Brush.verticalGradient(
                                                0.00f to Color.Black,
                                                0.54f to Color.Black,
                                                0.72f to Color.Black.copy(alpha = 1f - 0.38f * dissolveStrength),
                                                0.87f to Color.Black.copy(alpha = 1f - 0.78f * dissolveStrength),
                                                1.00f to Color.Black.copy(alpha = 1f - 1.00f * dissolveStrength)
                                            ),
                                            blendMode = BlendMode.DstIn
                                        )
                                    }
                                }
                        )
                    }

                    // Top subtle scrim + Centered "Playing from" header over the Full-Bleed Artwork (only in Main Player)
                    if (dissolveStrength > 0.01f) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                                .graphicsLayer { alpha = dissolveStrength }
                                .drawBehind {
                                    drawRect(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Black.copy(alpha = 0.36f),
                                                Color.Transparent
                                            )
                                        )
                                    )
                                }
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .windowInsetsPadding(WindowInsets.statusBars)
                                .padding(top = 8.dp, start = 24.dp, end = 24.dp)
                                .graphicsLayer { alpha = dissolveStrength },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(38.dp)
                                    .height(4.5.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(Color.White.copy(alpha = 0.62f))
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            val cleanSource = remember(playerState.playingFromSource) {
                                playerState.playingFromSource
                                    .removePrefix("Playing from ")
                                    .ifBlank { "Pilihan cepat" }
                            }
                            Text(
                                text = "Playing from $cleanSource",
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.92f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Top handle bar in Compact Mode (Lyrics / Queue)
                if (subViewMorph > 0.05f) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = statusBarTopPadding)
                            .graphicsLayer { alpha = subViewMorph }
                            .width(38.dp)
                            .height(4.5.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = 0.45f))
                    )
                }

                // =========================================================================
                // 2. BODY CONTENT LAYER (Main Controls / Synced Lyrics / Queue & AutoPlay)
                // =========================================================================
                val compactHeaderBottomDp = statusBarTopPadding + 78.dp

                if (activeSubView == PlayerSubView.MAIN) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .onSizeChanged { size ->
                                    if (size.height > 0) {
                                        mainControlsHeightPx = size.height
                                    }
                                }
                                .windowInsetsPadding(WindowInsets.navigationBars)
                                .padding(bottom = 6.dp)
                        ) {
                            MainPlayerSyncedLyricTeaser(
                                lyricsData = lyricsData,
                                smoothPositionSecState = smoothPositionSecState,
                                durationSec = playerState.durationSec,
                                onClick = { onChangeSubView(PlayerSubView.LYRICS) }
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            IsolatedPlayerSeekBar(
                                playerState = playerState,
                                onSeekTo = onSeekTo
                            )

                            Spacer(modifier = Modifier.height(18.dp))

                            PlayerTransportRow(
                                isPlaying = playerState.isPlaying,
                                isBuffering = playerState.isLoading,
                                onPreviousTrack = onPreviousTrack,
                                onTogglePlayPause = onTogglePlayPause,
                                onNextTrack = onNextTrack
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            SlimVolumeBar(
                                volume = playerState.volumeFraction,
                                onVolumeChange = onVolumeChange,
                                onVolumeStep = onVolumeStep
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            PureBottomActionRow(
                                activeSubView = activeSubView,
                                isShuffleEnabled = playerState.isShuffleEnabled,
                                isRepeatEnabled = playerState.isRepeatEnabled,
                                isAutoPlayEnabled = playerState.isAutoPlayEnabled,
                                frostedTint = frostedTint,
                                frostedBorderTint = frostedBorderTint,
                                onChangeSubView = onChangeSubView,
                                onToggleShuffle = onToggleShuffle,
                                onToggleRepeat = onToggleRepeat,
                                onToggleAutoPlay = onToggleAutoPlay
                            )
                        }
                    }
                } else if (activeSubView == PlayerSubView.LYRICS) {
                    LyricsSubViewContent(
                        playerState = playerState,
                        smoothPositionSecState = smoothPositionSecState,
                        bitChordClockMsState = bitChordClockMsState,
                        lyricsData = lyricsData,
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
                        currentTrackOffsetMs = currentTrackOffsetMs,
                        selectedLyricsProvider = selectedLyricsProvider,
                        frostedTint = frostedTint,
                        frostedBorderTint = frostedBorderTint,
                        topContentPadding = compactHeaderBottomDp,
                        onOpenProviderSelector = { showLyricsProviderSheet = true },
                        onOpenSourcesDialog = { showLyricsSourcesDialog = true },
                        onOpenOffsetSheet = { showLyricsOffsetSheet = true },
                        onToggleTranslation = onToggleLyricsTranslation,
                        onToggleRomanization = onToggleLyricsRomanization,
                        onSelectTranslationLanguage = onSelectTranslationLanguage,
                        onChangeSubView = onChangeSubView,
                        onTogglePlayPause = onTogglePlayPause,
                        onNextTrack = onNextTrack,
                        onPreviousTrack = onPreviousTrack,
                        onSeekTo = onSeekTo,
                        onVolumeChange = onVolumeChange,
                        onVolumeStep = onVolumeStep,
                        onToggleShuffle = onToggleShuffle,
                        onToggleRepeat = onToggleRepeat,
                        onToggleAutoPlay = onToggleAutoPlay
                    )
                } else {
                    QueueSubViewContent(
                        track = track,
                        effectiveCoverUrl = effectiveCoverUrl,
                        playerState = playerState,
                        smoothPositionSecState = smoothPositionSecState,
                        lyricsData = lyricsData,
                        isFavorite = isFavorite,
                        frostedTint = frostedTint,
                        frostedBorderTint = frostedBorderTint,
                        topContentPadding = compactHeaderBottomDp,
                        onToggleFavorite = { onToggleFavorite(track) },
                        onChangeSubView = onChangeSubView,
                        onTogglePlayPause = onTogglePlayPause,
                        onNextTrack = onNextTrack,
                        onPreviousTrack = onPreviousTrack,
                        onSeekTo = onSeekTo,
                        onVolumeChange = onVolumeChange,
                        onVolumeStep = onVolumeStep,
                        onToggleShuffle = onToggleShuffle,
                        onToggleRepeat = onToggleRepeat,
                        onToggleAutoPlay = onToggleAutoPlay,
                        onSelectQueueTrack = onSelectQueueTrack,
                        onRemoveQueueTrack = onRemoveQueueTrack
                    )
                }

                // =========================================================================
                // 3. SHARED MORPHING TITLE + ARTIST + FROSTED LOVE (♡) + MORE (•••) ROW
                // =========================================================================
                Row(
                    modifier = Modifier
                        .offset(x = animatedTitleOffsetX, y = animatedTitleOffsetY)
                        .width(animatedTitleRowWidth)
                        .onSizeChanged { size ->
                            if (subViewMorph < 0.05f && size.height > 0) {
                                titleRowHeightPx = size.height
                            }
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(enabled = isCompactMode) {
                                onChangeSubView(PlayerSubView.MAIN)
                            }
                    ) {
                        Text(
                            text = track.title,
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = animatedTitleFontSize,
                            letterSpacing = (-0.4).sp,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("full_player_track_title")
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = track.cleanArtist,
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = animatedArtistFontSize,
                            color = Color.White.copy(alpha = 0.78f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .clickable {
                                    onClosePlayer()
                                    onOpenArtistProfile(track.cleanArtist, "")
                                }
                                .testTag("full_player_track_artist")
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val buttonSize = lerp(42.dp, 38.dp, subViewMorph)
                        Box(
                            modifier = Modifier
                                .size(buttonSize)
                                .clip(CircleShape)
                                .background(frostedTint)
                                .border(0.8.dp, frostedBorderTint, CircleShape)
                                .clickable { onToggleFavorite(track) }
                                .testTag("full_player_favorite_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Simpan ke Favorit",
                                tint = if (isFavorite) ActiveHeartCoral else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(buttonSize)
                                .clip(CircleShape)
                                .background(frostedTint)
                                .border(0.8.dp, frostedBorderTint, CircleShape)
                                .clickable {
                                    showSleepTimerOverlay = false
                                    showPlayerMoreOptionsSheet = true
                                }
                                .testTag("full_player_more_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreHoriz,
                                contentDescription = "More options",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
                } // End of foreground stage Box

                // =========================================================================
                // 3.5 SLEEP TIMER FROSTED OVERLAY & 3-DOT BOTTOM SHEET (iOS-Style Bottom Sheet)
                // =========================================================================
                if (showSleepTimerOverlay) {
                    val glassCardSurface = remember(midAtmosphereColor, bottomAtmosphereColor) {
                        lerp(midAtmosphereColor, Color(0xFF2B222C), 0.52f).copy(alpha = 0.80f)
                    }
                    val glassCardBorder = Color.White.copy(alpha = 0.14f)

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .windowInsetsPadding(WindowInsets.statusBars)
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                showSleepTimerOverlay = false
                                showPlayerMoreOptionsSheet = false
                            }
                            .padding(horizontal = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(28.dp))
                                .background(glassCardSurface)
                                .border(1.dp, glassCardBorder, RoundedCornerShape(28.dp))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { /* consume click inside card */ }
                                .padding(horizontal = 22.dp, vertical = 24.dp)
                                .testTag("player_sleep_timer_card"),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Sleep timer",
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.5.sp,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            val pillBg = Color.White.copy(alpha = 0.13f)
                            val activeLabel = playerState.sleepTimerLabel

                            // Row 1: 10m | 15m
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                SleepTimerOptionPill(
                                    label = "10m",
                                    isActive = activeLabel == "10m",
                                    backgroundColor = pillBg,
                                    onClick = {
                                        onSetSleepTimerMinutes(10, "10m")
                                        showSleepTimerOverlay = false
                                        showPlayerMoreOptionsSheet = false
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("sleep_timer_10m_button")
                                )
                                SleepTimerOptionPill(
                                    label = "15m",
                                    isActive = activeLabel == "15m",
                                    backgroundColor = pillBg,
                                    onClick = {
                                        onSetSleepTimerMinutes(15, "15m")
                                        showSleepTimerOverlay = false
                                        showPlayerMoreOptionsSheet = false
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("sleep_timer_15m_button")
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Row 2: 30m | 1h
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                SleepTimerOptionPill(
                                    label = "30m",
                                    isActive = activeLabel == "30m",
                                    backgroundColor = pillBg,
                                    onClick = {
                                        onSetSleepTimerMinutes(30, "30m")
                                        showSleepTimerOverlay = false
                                        showPlayerMoreOptionsSheet = false
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("sleep_timer_30m_button")
                                )
                                SleepTimerOptionPill(
                                    label = "1h",
                                    isActive = activeLabel == "1h",
                                    backgroundColor = pillBg,
                                    onClick = {
                                        onSetSleepTimerMinutes(60, "1h")
                                        showSleepTimerOverlay = false
                                        showPlayerMoreOptionsSheet = false
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("sleep_timer_1h_button")
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Row 3: End of track (full width)
                            SleepTimerOptionPill(
                                label = "End of track",
                                isActive = playerState.sleepTimerEndOfTrack,
                                backgroundColor = pillBg,
                                onClick = {
                                    onSetSleepTimerEndOfTrack()
                                    showSleepTimerOverlay = false
                                    showPlayerMoreOptionsSheet = false
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("sleep_timer_end_of_track_button")
                            )

                            Spacer(modifier = Modifier.height(22.dp))

                            Text(
                                text = "Cancel",
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.5.sp,
                                color = Color.White,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        if (playerState.sleepTimerLabel != null || playerState.sleepTimerEndOfTrack) {
                                            onCancelSleepTimer()
                                        }
                                        showSleepTimerOverlay = false
                                        showPlayerMoreOptionsSheet = false
                                    }
                                    .padding(horizontal = 24.dp, vertical = 6.dp)
                                    .testTag("sleep_timer_cancel_button")
                            )
                        }
                    }
                }

                // 3-DOT MENU BOTTOM SHEET (Slides up from bottom like iOS / Reference Image 1)
                AnimatedVisibility(
                    visible = showPlayerMoreOptionsSheet && !showSleepTimerOverlay,
                    enter = fadeIn(tween(220)) + slideInVertically(tween(280, easing = FastOutSlowInEasing)) { it },
                    exit = fadeOut(tween(180)) + slideOutVertically(tween(240, easing = FastOutSlowInEasing)) { it }
                ) {
                    val sheetTopColor = remember(midAtmosphereColor) {
                        lerp(midAtmosphereColor, Color(0xFF3A3A40), 0.62f).copy(alpha = 0.96f)
                    }
                    val sheetBottomColor = remember(bottomAtmosphereColor) {
                        lerp(bottomAtmosphereColor, Color(0xFF141418), 0.78f).copy(alpha = 0.99f)
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.28f))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                showPlayerMoreOptionsSheet = false
                            },
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(sheetTopColor, sheetBottomColor)
                                    )
                                )
                                .border(
                                    width = 0.8.dp,
                                    color = Color.White.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { /* consume click inside bottom sheet */ }
                                .windowInsetsPadding(WindowInsets.navigationBars)
                                .verticalScroll(rememberScrollState())
                                .padding(top = 10.dp, bottom = 18.dp)
                                .testTag("player_more_options_card")
                        ) {
                            // Top Drag Handle Pill
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .width(38.dp)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(Color.White.copy(alpha = 0.42f))
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Top Header: Rounded Square Thumbnail + Song Title + Artist Name
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp)
                            ) {
                                AsyncImage(
                                    model = effectiveCoverUrl,
                                    contentDescription = track.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(CardPlaceholderDark)
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = track.title,
                                        fontFamily = PlusJakartaSans,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = track.cleanArtist,
                                        fontFamily = PlusJakartaSans,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp,
                                        color = Color.White.copy(alpha = 0.72f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            HorizontalDivider(
                                color = Color.White.copy(alpha = 0.09f),
                                thickness = 0.8.dp,
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Convert to audio / Switch to video
                            PlayerGlassMenuRow(
                                icon = if (isVideoModeActive) Icons.Default.MusicNote else Icons.Default.OndemandVideo,
                                iconTint = Color.White,
                                label = if (isVideoModeActive) "Convert to audio" else "Switch to video",
                                onClick = {
                                    onToggleVideoMode()
                                    showPlayerMoreOptionsSheet = false
                                },
                                modifier = Modifier.testTag("player_more_toggle_video_button")
                            )

                            HorizontalDivider(
                                color = Color.White.copy(alpha = 0.08f),
                                thickness = 0.8.dp,
                                modifier = Modifier.fillMaxWidth()
                            )

                            // 1. Like
                            PlayerGlassMenuRow(
                                icon = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                iconTint = if (isFavorite) ActiveHeartCoral else Color.White,
                                label = "Like",
                                trailingBadge = if (isFavorite) "Liked" else null,
                                onClick = {
                                    onToggleFavorite(track)
                                    showPlayerMoreOptionsSheet = false
                                },
                                modifier = Modifier.testTag("player_more_like_button")
                            )

                            // 2. Add to playlist
                            PlayerGlassMenuRow(
                                icon = Icons.AutoMirrored.Filled.PlaylistAdd,
                                iconTint = Color.White,
                                label = "Add to playlist",
                                onClick = {
                                    showPlayerMoreOptionsSheet = false
                                    showAddToPlaylistSheet = true
                                },
                                modifier = Modifier.testTag("player_more_add_to_playlist_button")
                            )

                            HorizontalDivider(
                                color = Color.White.copy(alpha = 0.08f),
                                thickness = 0.8.dp,
                                modifier = Modifier.fillMaxWidth()
                            )

                            // 3. Download
                            PlayerGlassMenuRow(
                                icon = if (isDownloaded) Icons.Rounded.DownloadDone else Icons.Rounded.Download,
                                iconTint = if (isDownloaded) Color(0xFF38EF7D) else Color.White,
                                label = when {
                                    activeDownloadProgress != null -> "Downloading (${activeDownloadProgress.progressPercent}%)"
                                    isDownloaded -> "Downloaded"
                                    else -> "Download"
                                },
                                trailingBadge = if (isDownloaded) "Remove" else null,
                                onClick = {
                                    if (activeDownloadProgress == null) {
                                        if (isDownloaded) onRemoveDownload(track.id)
                                        else onDownloadTrack(track)
                                        showPlayerMoreOptionsSheet = false
                                    }
                                },
                                modifier = Modifier.testTag("player_more_download_button")
                            )

                            // 4. Play next
                            PlayerGlassMenuRow(
                                icon = Icons.AutoMirrored.Filled.PlaylistPlay,
                                iconTint = Color.White,
                                label = "Play next",
                                onClick = {
                                    onPlayNext(track)
                                    showPlayerMoreOptionsSheet = false
                                },
                                modifier = Modifier.testTag("player_more_play_next_button")
                            )

                            // 5. Add to queue
                            PlayerGlassMenuRow(
                                icon = Icons.AutoMirrored.Filled.QueueMusic,
                                iconTint = Color.White,
                                label = "Add to queue",
                                onClick = {
                                    onAddToQueue(track)
                                    showPlayerMoreOptionsSheet = false
                                },
                                modifier = Modifier.testTag("player_more_add_to_queue_button")
                            )

                            // 6. Open artist
                            PlayerGlassMenuRow(
                                icon = Icons.Default.Person,
                                iconTint = Color.White,
                                label = "Open artist",
                                onClick = {
                                    showPlayerMoreOptionsSheet = false
                                    onClosePlayer()
                                    onOpenArtistProfile(track.cleanArtist, "")
                                },
                                modifier = Modifier.testTag("player_more_view_artist_button")
                            )

                            // 7. Sleep timer
                            PlayerGlassMenuRow(
                                icon = Icons.Default.Bedtime,
                                iconTint = Color.White,
                                label = "Sleep timer",
                                trailingBadge = playerState.sleepTimerLabel,
                                onClick = {
                                    showSleepTimerOverlay = true
                                },
                                modifier = Modifier.testTag("player_more_sleep_timer_button")
                            )

                            HorizontalDivider(
                                color = Color.White.copy(alpha = 0.08f),
                                thickness = 0.8.dp,
                                modifier = Modifier.fillMaxWidth()
                            )

                            // 7.1 BitChord Lyrics Provider Selector
                            PlayerGlassMenuRow(
                                icon = Icons.Default.Subtitles,
                                iconTint = Color.White,
                                label = "Sumber Lirik (Provider)",
                                trailingBadge = when {
                                    isEmbeddedLyrics -> "Offline .lrc"
                                    activeLyricsSource != null -> activeLyricsSource.displayName
                                    else -> "Auto"
                                },
                                onClick = {
                                    showPlayerMoreOptionsSheet = false
                                    showLyricsProviderSheet = true
                                },
                                modifier = Modifier.testTag("player_more_lyrics_provider_button")
                            )

                            // 7.2 BitChord Lyrics Priority & Sources Dialog
                            PlayerGlassMenuRow(
                                icon = Icons.Default.Tune,
                                iconTint = Color.White,
                                label = "Urutan & Prioritas Lirik",
                                trailingBadge = "${lyricsSettings.enabledSources.size} Aktif",
                                onClick = {
                                    showPlayerMoreOptionsSheet = false
                                    showLyricsSourcesDialog = true
                                },
                                modifier = Modifier.testTag("player_more_lyrics_sources_button")
                            )

                            // 7.3 BitChord Lyrics Offset Sheet
                            PlayerGlassMenuRow(
                                icon = Icons.Default.Schedule,
                                iconTint = Color.White,
                                label = "Offset Waktu Lirik",
                                trailingBadge = if (currentTrackOffsetMs == 0) "0.0s" else "%+.1fs".format(currentTrackOffsetMs / 1000f),
                                onClick = {
                                    showPlayerMoreOptionsSheet = false
                                    showLyricsOffsetSheet = true
                                },
                                modifier = Modifier.testTag("player_more_lyrics_offset_button")
                            )

                            HorizontalDivider(
                                color = Color.White.copy(alpha = 0.08f),
                                thickness = 0.8.dp,
                                modifier = Modifier.fillMaxWidth()
                            )

                            // 8. Share
                            PlayerGlassMenuRow(
                                icon = Icons.Default.Share,
                                iconTint = Color.White,
                                label = "Share",
                                onClick = {
                                    showPlayerMoreOptionsSheet = false
                                    try {
                                        val vid = track.videoId ?: track.id
                                        val shareUrl = "https://music.youtube.com/watch?v=$vid"
                                        val sendIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(
                                                android.content.Intent.EXTRA_TEXT,
                                                "${track.title} - ${track.cleanArtist}\n$shareUrl"
                                            )
                                        }
                                        context.startActivity(
                                            android.content.Intent.createChooser(sendIntent, "Share ${track.title}")
                                        )
                                    } catch (_: Exception) {
                                    }
                                },
                                modifier = Modifier.testTag("player_more_share_button")
                            )
                        }
                    }
                }
            }
        }
    }

    // =========================================================================
    // 4. BITCHORD LYRICS MODALS: PROVIDER SHEET, SOURCES DIALOG & OFFSET SHEET
    // =========================================================================
    if (showLyricsProviderSheet) {
        LyricsProviderSheet(
            activeSource = activeLyricsSource,
            isEmbeddedLyrics = isEmbeddedLyrics,
            providerStates = lyricsProviderStates,
            providerResults = lyricsProviderResults,
            orderedSources = lyricsSettings.sourceOrder,
            enabledSources = lyricsSettings.enabledSources,
            onSelectSource = { source ->
                onSelectBitChordLyricsSource(source)
            },
            onOpenSourcesDialog = {
                showLyricsProviderSheet = false
                showLyricsSourcesDialog = true
            },
            onOpenOffsetSheet = {
                showLyricsProviderSheet = false
                showLyricsOffsetSheet = true
            },
            onDismiss = { showLyricsProviderSheet = false }
        )
    }

    if (showLyricsSourcesDialog) {
        LyricsSourcesDialog(
            settings = lyricsSettings,
            onToggleSource = onToggleLyricsSourceEnabled,
            onMoveSource = onMoveLyricsSourceOrder,
            onTogglePrioritizeSyllable = onTogglePrioritizeSyllable,
            onSavePaxSenixApiKey = onSavePaxSenixApiKey,
            onResetDefault = onResetLyricsSourcesDefault,
            onDismiss = { showLyricsSourcesDialog = false }
        )
    }

    if (showLyricsOffsetSheet) {
        LyricsOffsetSheet(
            currentOffsetMs = currentTrackOffsetMs,
            onChangeOffsetMs = onChangeTrackLyricsOffsetMs,
            onDismiss = { showLyricsOffsetSheet = false }
        )
    }

    // Floating glass 3-dot menu & sleep timer are rendered inline inside the player stage above.

    if (showAddToPlaylistSheet) {
        AddToPlaylistBottomSheet(
            track = track,
            userPlaylists = libraryPlaylists,
            onDismiss = { showAddToPlaylistSheet = false },
            onSelectPlaylist = { targetPlaylist ->
                onAddTrackToPlaylist(targetPlaylist.id, track)
                showAddToPlaylistSheet = false
            },
            onCreateNewPlaylistAndAdd = { title, privacy ->
                onCreatePlaylistAndAddTrack(title, privacy, track)
                showAddToPlaylistSheet = false
            }
        )
    }
}

@Composable
private fun PlayerGlassMenuRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    label: String,
    trailingBadge: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = iconTint,
            modifier = Modifier.size(23.dp)
        )
        Spacer(modifier = Modifier.width(18.dp))
        Text(
            text = label,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.Bold,
            fontSize = 15.5.sp,
            color = Color.White,
            modifier = Modifier.weight(1f)
        )
        if (!trailingBadge.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.16f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = trailingBadge,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun SleepTimerOptionPill(
    label: String,
    isActive: Boolean,
    backgroundColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (isActive) Color.White.copy(alpha = 0.26f) else backgroundColor)
            .then(
                if (isActive) {
                    Modifier.border(1.dp, Color.White.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = Color.White,
            textAlign = TextAlign.Center
        )
    }
}

// ============================================================================
// SMOOTH 120FPS PLAYBACK CLOCK & KARAOKE WORD-BY-WORD SWEEP HELPERS
// ============================================================================
@Composable
private fun rememberSmoothPlaybackSeconds(playerState: PlayerUiState): State<Double> {
    val initialExact = playerState.exactPositionSec.takeIf { it > 0.0 }
        ?: playerState.currentPositionSec.toDouble()
    val smoothSec = remember { mutableDoubleStateOf(initialExact) }

    // Anchor to 40ms millisecond-precision updates from AudioPlayerController (+0.04s single-frame vocal onset lead)
    LaunchedEffect(playerState.exactPositionSec, playerState.currentPositionMs, playerState.currentTrack?.id) {
        val exactTarget = (playerState.exactPositionSec.takeIf { it > 0.0 }
            ?: (playerState.currentPositionMs / 1000.0)) + if (playerState.isPlaying) 0.04 else 0.0
        val drift = kotlin.math.abs(smoothSec.doubleValue - exactTarget)
        if (drift > 0.16 || playerState.currentPositionMs <= 120L) {
            smoothSec.doubleValue = exactTarget
        } else if (exactTarget > smoothSec.doubleValue) {
            smoothSec.doubleValue = smoothSec.doubleValue + (exactTarget - smoothSec.doubleValue) * 0.75
        }
    }

    // 60-120fps frame interpolation between 40ms hardware ticks
    LaunchedEffect(playerState.isPlaying, playerState.currentTrack?.id) {
        if (!playerState.isPlaying) {
            smoothSec.doubleValue = playerState.exactPositionSec.takeIf { it > 0.0 }
                ?: playerState.currentPositionSec.toDouble()
            return@LaunchedEffect
        }
        var lastNanos = 0L
        while (true) {
            withFrameNanos { now ->
                if (lastNanos != 0L) {
                    val dt = ((now - lastNanos) / 1_000_000_000.0).coerceIn(0.0, 0.05)
                    val maxDur = playerState.durationSec.toDouble().coerceAtLeast(10.0)
                    smoothSec.doubleValue = (smoothSec.doubleValue + dt).coerceAtMost(maxDur)
                }
                lastNanos = now
            }
        }
    }

    return smoothSec
}

@Composable
private fun MainPlayerSyncedLyricTeaser(
    lyricsData: LyricsData?,
    smoothPositionSecState: State<Double>,
    durationSec: Int,
    onClick: () -> Unit
) {
    val isSyncedLrc = lyricsData?.isSynced == true &&
        lyricsData.syncedLines.isNotEmpty()

    val syncedLines = remember(lyricsData) {
        lyricsData?.syncedLines?.takeIf { it.isNotEmpty() }
            ?: lyricsData?.lines?.mapIndexed { idx, text ->
                val start = 3.0 + (idx * 5.2)
                LyricLine(text = text, beginSec = start, endSec = start + 4.8)
            }
            ?: listOf(LyricLine("Nada pertama masuk...", 0.0, 8.0))
    }

    val smoothSec = smoothPositionSecState.value
    val quantizedTick = (smoothSec * 25.0).toInt()
    val activeIdx = remember(syncedLines, quantizedTick) {
        val idx = syncedLines.indexOfLast { line ->
            smoothSec >= ((line.beginSec ?: 0.0) - 0.16)
        }
        if (idx >= 0) idx.coerceIn(0, (syncedLines.size - 1).coerceAtLeast(0)) else 0
    }

    val currentLine = syncedLines.getOrElse(activeIdx) { LyricLine("Nada pertama masuk...", 0.0, 8.0) }
    val annotatedPreview = if (isSyncedLrc) {
        buildSyllableKaraokeAnnotatedString(
            line = currentLine,
            smoothSec = smoothSec,
            defaultIndex = activeIdx,
            durationSec = durationSec.toDouble().coerceAtLeast(10.0),
            passedFontSizeSp = 15.5f,
            activeFontSizeSp = 15.2f,
            upcomingFontSizeSp = 14.5f,
            activeAlpha = 1.0f,
            upcomingAlpha = 0.50f
        )
    } else {
        buildAnnotatedString {
            withStyle(
                SpanStyle(
                    color = Color.White.copy(alpha = 0.95f),
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp
                )
            ) {
                append(currentLine.text)
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 3.dp)
            .testTag("main_player_lyric_preview_row"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.MusicNote,
            contentDescription = "Buka Lirik",
            tint = Color.White.copy(alpha = 0.88f),
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = annotatedPreview,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.Black,
            fontSize = 15.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.65f),
            modifier = Modifier.size(18.dp)
        )
    }
}

private fun computeExactCharCursor(
    line: LyricLine,
    smoothSec: Double,
    defaultIndex: Int,
    durationSec: Double
): Float {
    val text = line.text
    if (text.isEmpty()) return 0f
    val len = text.length

    val syllables = line.words
    if (syllables.isNotEmpty()) {
        var searchFrom = 0
        for (i in syllables.indices) {
            val syl = syllables[i]
            val sylText = syl.text
            if (sylText.isEmpty()) continue
            val matchIdx = text.indexOf(sylText, startIndex = searchFrom).let {
                if (it >= 0) it else searchFrom.coerceAtMost(len)
            }
            val charStart = matchIdx.coerceIn(0, len)
            val charEnd = (charStart + sylText.length).coerceIn(charStart, len)
            searchFrom = charEnd

            if (smoothSec < syl.beginSec) {
                return charStart.toFloat()
            } else if (smoothSec <= syl.endSec) {
                val sylDur = (syl.endSec - syl.beginSec).coerceAtLeast(0.06)
                val frac = ((smoothSec - syl.beginSec) / sylDur).toFloat().coerceIn(0f, 1f)
                return charStart + frac * (charEnd - charStart)
            }
        }
        return len.toFloat()
    }

    val startSec = line.beginSec ?: (defaultIndex * 5.0)
    val endSec = line.endSec ?: (startSec + 4.5).coerceAtMost(durationSec)
    if (smoothSec <= startSec) {
        return (len * 0.08f).coerceAtMost(2.5f)
    }
    val lineDur = (endSec - startSec).coerceAtLeast(0.35)
    val progress = ((smoothSec - startSec) / lineDur).toFloat().coerceIn(0f, 1f)
    return progress * len
}

private fun buildSyllableKaraokeAnnotatedString(
    line: LyricLine,
    smoothSec: Double,
    defaultIndex: Int,
    durationSec: Double = 240.0,
    passedFontSizeSp: Float = 28.5f,
    activeFontSizeSp: Float = 27.5f,
    upcomingFontSizeSp: Float = 24.5f,
    activeAlpha: Float = 1.0f,
    upcomingAlpha: Float = 0.36f
) = buildAnnotatedString {
    val text = line.text
    if (text.isEmpty()) return@buildAnnotatedString
    val len = text.length

    val exactCursor = computeExactCharCursor(
        line = line,
        smoothSec = smoothSec,
        defaultIndex = defaultIndex,
        durationSec = durationSec
    ).coerceIn(0f, len.toFloat())

    val fullPassedEnd = exactCursor.toInt().coerceIn(0, len)
    val featherSpan = 5
    val featherEnd = (exactCursor + featherSpan).toInt().coerceIn(fullPassedEnd, len)

    // 1. Already passed / sung portion -> Larger font size (28.5.sp), Ultra-Bold (Black), pure white 1.0f + subtle glow
    if (fullPassedEnd > 0) {
        withStyle(
            SpanStyle(
                color = Color.White.copy(alpha = activeAlpha),
                fontWeight = FontWeight.Black,
                fontSize = passedFontSizeSp.sp,
                shadow = androidx.compose.ui.graphics.Shadow(
                    color = Color.White.copy(alpha = 0.32f),
                    offset = Offset(0f, 0f),
                    blurRadius = 10f
                )
            )
        ) {
            append(text.substring(0, fullPassedEnd))
        }
    }

    // 2. Currently singing transition feather -> Smooth gradient sweep from white to grey and large to medium
    if (featherEnd > fullPassedEnd) {
        val subFract = (exactCursor - fullPassedEnd).coerceIn(0f, 1f)
        for (charIdx in fullPassedEnd until featherEnd) {
            val dist = (charIdx - fullPassedEnd).toFloat() - subFract
            val charProgress = (1f - (dist / featherSpan.toFloat())).coerceIn(0f, 1f)
            val alpha = lerpFloat(upcomingAlpha, activeAlpha, charProgress)
            val sizeSp = lerpFloat(upcomingFontSizeSp, activeFontSizeSp, charProgress)
            withStyle(
                SpanStyle(
                    color = Color.White.copy(alpha = alpha),
                    fontWeight = FontWeight.Black,
                    fontSize = sizeSp.sp,
                    shadow = androidx.compose.ui.graphics.Shadow(
                        color = Color.White.copy(alpha = 0.38f * charProgress),
                        offset = Offset(0f, 0f),
                        blurRadius = 12f * charProgress
                    )
                )
            ) {
                append(text[charIdx])
            }
        }
    }

    // 3. Upcoming portion not yet sung -> Smaller font size (24.5.sp), ExtraBold, soft grey
    if (featherEnd < len) {
        withStyle(
            SpanStyle(
                color = Color.White.copy(alpha = upcomingAlpha),
                fontWeight = FontWeight.ExtraBold,
                fontSize = upcomingFontSizeSp.sp
            )
        ) {
            append(text.substring(featherEnd, len))
        }
    }
}

@Composable
private fun InterludeThreeDotsIndicator(
    gapStartSec: Double,
    gapEndSec: Double,
    smoothSec: Double,
    modifier: Modifier = Modifier
) {
    val span = (gapEndSec - gapStartSec).coerceAtLeast(0.8)
    val overallProgress = ((smoothSec - gapStartSec) / span).toFloat().coerceIn(0f, 1f)
    val infiniteTransition = rememberInfiniteTransition(label = "interlude_dots_pulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 680, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dots_scale_pulse"
    )

    Row(
        modifier = modifier
            .padding(vertical = 8.dp)
            .graphicsLayer {
                scaleX = pulse
                scaleY = pulse
                transformOrigin = TransformOrigin(0f, 0.5f)
            },
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (dotIdx in 0..2) {
            val dotStart = dotIdx / 3f
            val dotEnd = (dotIdx + 1) / 3f
            val dotFill = ((overallProgress - dotStart) / (dotEnd - dotStart)).coerceIn(0f, 1f)
            val dotAlpha = lerpFloat(0.28f, 1.0f, dotFill)
            val dotSizeDp = lerpFloat(9f, 12.5f, dotFill).dp
            Box(
                modifier = Modifier
                    .size(dotSizeDp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = dotAlpha))
            )
        }
    }
}

// ============================================================================
// LYRICS SUB-VIEW (BitChord Multi-Provider Engine + TTML / Word-by-Word Sync)
// ============================================================================
@Composable
private fun LyricsSubViewContent(
    playerState: PlayerUiState,
    smoothPositionSecState: State<Double>,
    bitChordClockMsState: State<Long>,
    lyricsData: LyricsData?,
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
    currentTrackOffsetMs: Int,
    selectedLyricsProvider: LyricsProvider,
    frostedTint: Color,
    frostedBorderTint: Color,
    topContentPadding: Dp,
    onOpenProviderSelector: () -> Unit,
    onOpenSourcesDialog: () -> Unit,
    onOpenOffsetSheet: () -> Unit,
    onToggleTranslation: () -> Unit,
    onToggleRomanization: () -> Unit,
    onSelectTranslationLanguage: (String) -> Unit,
    onChangeSubView: (PlayerSubView) -> Unit,
    onTogglePlayPause: () -> Unit,
    onNextTrack: () -> Unit,
    onPreviousTrack: () -> Unit,
    onSeekTo: (Float) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onVolumeStep: (Boolean) -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleAutoPlay: () -> Unit
) {
    val effectiveBitChordLines = remember(bitChordLyricsLines, lyricsData) {
        if (bitChordLyricsLines.isNotEmpty()) {
            bitChordLyricsLines
        } else if (lyricsData != null && lyricsData.bitChordLines.isNotEmpty()) {
            lyricsData.bitChordLines
        } else if (lyricsData != null && lyricsData.syncedLines.isNotEmpty()) {
            lyricsData.syncedLines.map { l ->
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
        } else if (lyricsData != null && lyricsData.lines.isNotEmpty()) {
            lyricsData.lines.map { text ->
                BitChordLyricLine(timeMs = 0L, text = text)
            }
        } else {
            emptyList()
        }
    }

    val durationMs = remember(playerState.durationSec) {
        playerState.durationSec.coerceAtLeast(180) * 1000L
    }

    val lyricsListState = rememberLazyListState()
    var controlsVisible by remember { mutableStateOf(true) }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput) {
                    if (available.y < -9f && controlsVisible) {
                        controlsVisible = false
                    } else if (available.y > 9f && !controlsVisible) {
                        controlsVisible = true
                    }
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (available.y < -320f && controlsVisible) {
                    controlsVisible = false
                } else if (available.y > 320f && !controlsVisible) {
                    controlsVisible = true
                }
                return Velocity.Zero
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = topContentPadding)
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        BitChordSyncedLyricsPanel(
            lines = effectiveBitChordLines,
            translatedLines = translatedLyricsLines,
            romanizedLines = romanizedLyricsLines,
            isTranslationEnabled = isTranslationEnabled,
            isRomanizationEnabled = isRomanizationEnabled,
            isTranslating = isTranslatingLyrics,
            activeSource = activeLyricsSource ?: lyricsData?.winningSource,
            isEmbeddedLyrics = isEmbeddedLyrics || (lyricsData?.isEmbedded == true),
            isLoadingLyrics = isLoadingLyrics,
            lyricsSettings = lyricsSettings,
            currentTrackOffsetMs = currentTrackOffsetMs,
            clockMsState = bitChordClockMsState,
            durationMs = durationMs,
            listState = lyricsListState,
            nestedScrollConnection = nestedScrollConnection,
            frostedTint = frostedTint,
            frostedBorderTint = frostedBorderTint,
            onSeekToMs = { targetMs ->
                val ratio = (targetMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                onSeekTo(ratio)
                controlsVisible = true
            },
            onOpenProviderSheet = onOpenProviderSelector,
            onOpenSourcesDialog = onOpenSourcesDialog,
            onOpenOffsetSheet = onOpenOffsetSheet,
            onToggleTranslation = onToggleTranslation,
            onToggleRomanization = onToggleRomanization,
            onSelectTranslationLanguage = onSelectTranslationLanguage,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        )

        // Auto-Hide Bottom Controls + Clickable Underlined Provider Credit above Seek Bar
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(tween(220)) + expandVertically(tween(240)),
            exit = fadeOut(tween(180)) + shrinkVertically(tween(220))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                val creditText = when {
                    isEmbeddedLyrics || lyricsData?.isEmbedded == true -> "Lirik Offline (Embedded .lrc)"
                    activeLyricsSource != null -> "Lirik oleh ${activeLyricsSource.displayName}"
                    !lyricsData?.source.isNullOrBlank() -> lyricsData?.source.orEmpty()
                    else -> selectedLyricsProvider.creditLabel
                }

                Text(
                    text = creditText,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.5.sp,
                    color = Color.White.copy(alpha = 0.78f),
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(onClick = onOpenProviderSelector)
                        .padding(vertical = 6.dp)
                        .testTag("lyrics_provider_credit_button")
                )

                Spacer(modifier = Modifier.height(4.dp))

                IsolatedPlayerSeekBar(
                    playerState = playerState,
                    onSeekTo = onSeekTo
                )

                Spacer(modifier = Modifier.height(18.dp))

                PlayerTransportRow(
                    isPlaying = playerState.isPlaying,
                    isBuffering = playerState.isLoading,
                    onPreviousTrack = onPreviousTrack,
                    onTogglePlayPause = onTogglePlayPause,
                    onNextTrack = onNextTrack
                )

                Spacer(modifier = Modifier.height(18.dp))

                SlimVolumeBar(
                    volume = playerState.volumeFraction,
                    onVolumeChange = onVolumeChange,
                    onVolumeStep = onVolumeStep
                )

                Spacer(modifier = Modifier.height(12.dp))

                PureBottomActionRow(
                    activeSubView = PlayerSubView.LYRICS,
                    isShuffleEnabled = playerState.isShuffleEnabled,
                    isRepeatEnabled = playerState.isRepeatEnabled,
                    isAutoPlayEnabled = playerState.isAutoPlayEnabled,
                    frostedTint = frostedTint,
                    frostedBorderTint = frostedBorderTint,
                    onChangeSubView = onChangeSubView,
                    onToggleShuffle = onToggleShuffle,
                    onToggleRepeat = onToggleRepeat,
                    onToggleAutoPlay = onToggleAutoPlay
                )
            }
        }
    }
}

@Composable
private fun KaraokeLyricLineItem(
    line: LyricLine,
    index: Int,
    isSyncedLrc: Boolean,
    isCurrent: Boolean,
    isPassed: Boolean,
    durationSec: Double,
    smoothPositionSecState: State<Double>,
    onClick: () -> Unit
) {
    // Dynamic scale variation: passed lines stay slightly fuller (1.008f), active line pops (1.045f), upcoming lines start smaller (0.965f)
    val targetScale = when {
        !isSyncedLrc -> 1.0f
        isCurrent -> 1.045f
        isPassed -> 1.008f
        else -> 0.965f
    }
    val lineScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = spring(
            dampingRatio = 0.82f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "lyric_line_scale"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = lineScale
                scaleY = lineScale
                transformOrigin = TransformOrigin(0f, 0.5f)
            }
            .clickable(onClick = onClick)
            .padding(vertical = 3.dp)
    ) {
        if (!isSyncedLrc) {
            // Default YouTube Music lyrics: Crisp, unblurred, ultra-bold, high legibility
            Text(
                text = line.text,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Black,
                fontSize = 24.5.sp,
                lineHeight = 32.sp,
                letterSpacing = (-0.4).sp,
                color = Color.White.copy(alpha = 0.96f)
            )
        } else if (isCurrent) {
            // Active synced LRC line: syllable-accurate white sweep + small-to-large dynamic word size variation
            val smoothSec = smoothPositionSecState.value
            val karaokeText = buildSyllableKaraokeAnnotatedString(
                line = line,
                smoothSec = smoothSec,
                defaultIndex = index,
                durationSec = durationSec,
                passedFontSizeSp = 28.5f,
                activeFontSizeSp = 27.5f,
                upcomingFontSizeSp = 24.5f,
                activeAlpha = 1.0f,
                upcomingAlpha = 0.42f
            )

            Text(
                text = karaokeText,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Black,
                fontSize = 27.5.sp,
                lineHeight = 35.sp,
                letterSpacing = (-0.5).sp
            )
        } else {
            // Inactive synced LRC line: crisp, thick Sans-Serif Black/ExtraBold without RenderNode blur overhead
            Text(
                text = line.text,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Black,
                fontSize = if (isPassed) 25.5.sp else 23.5.sp,
                lineHeight = if (isPassed) 32.sp else 30.sp,
                letterSpacing = (-0.45).sp,
                color = Color.White.copy(alpha = if (isPassed) 0.54f else 0.40f)
            )
        }

        if (!line.bgText.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            if (isSyncedLrc && isCurrent && line.bgWords.isNotEmpty()) {
                val smoothSec = smoothPositionSecState.value
                val bgLineObj = LyricLine(
                    text = "(${line.bgText})",
                    beginSec = line.bgBeginSec,
                    endSec = line.bgEndSec,
                    words = line.bgWords
                )
                val bgKaraoke = buildSyllableKaraokeAnnotatedString(
                    line = bgLineObj,
                    smoothSec = smoothSec,
                    defaultIndex = index,
                    durationSec = durationSec,
                    passedFontSizeSp = 19.0f,
                    activeFontSizeSp = 18.5f,
                    upcomingFontSizeSp = 17.0f,
                    activeAlpha = 0.90f,
                    upcomingAlpha = 0.35f
                )
                Text(
                    text = bgKaraoke,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.5.sp
                )
            } else {
                Text(
                    text = "(${line.bgText})",
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = if (isCurrent) 18.sp else 16.5.sp,
                    color = when {
                        !isSyncedLrc -> Color.White.copy(alpha = 0.80f)
                        isCurrent -> Color.White.copy(alpha = 0.82f)
                        else -> Color.White.copy(alpha = 0.32f)
                    }
                )
            }
        }
    }
}

// ============================================================================
// QUEUE SUB-VIEW (Antrean & AutoPlay)
// ============================================================================
@Composable
private fun QueueSubViewContent(
    track: MusicItem,
    effectiveCoverUrl: String,
    playerState: PlayerUiState,
    smoothPositionSecState: State<Double>,
    lyricsData: LyricsData?,
    isFavorite: Boolean,
    frostedTint: Color,
    frostedBorderTint: Color,
    topContentPadding: Dp,
    onToggleFavorite: () -> Unit,
    onChangeSubView: (PlayerSubView) -> Unit,
    onTogglePlayPause: () -> Unit,
    onNextTrack: () -> Unit,
    onPreviousTrack: () -> Unit,
    onSeekTo: (Float) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onVolumeStep: (Boolean) -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleAutoPlay: () -> Unit,
    onSelectQueueTrack: (MusicItem) -> Unit,
    onRemoveQueueTrack: (MusicItem) -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = topContentPadding)
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("queue_lazy_column"),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item(key = "sedang_diputar_header", contentType = "queue_header") {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Antrean",
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = Color.White
                    )
                    Text(
                        text = "Sedang diputar",
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.5.sp,
                        color = Color.White.copy(alpha = 0.68f)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(alpha = 0.13f))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(CardPlaceholderDark),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = effectiveCoverUrl,
                                contentDescription = track.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.35f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.size(16.dp)) {
                                    val barW = size.width * 0.22f
                                    val gap = (size.width - barW * 3f) / 2f
                                    drawRoundRect(
                                        color = Color.White,
                                        topLeft = Offset(0f, size.height * 0.2f),
                                        size = Size(barW, size.height * 0.6f),
                                        cornerRadius = CornerRadius(barW / 2f, barW / 2f)
                                    )
                                    drawRoundRect(
                                        color = Color.White,
                                        topLeft = Offset(barW + gap, 0f),
                                        size = Size(barW, size.height),
                                        cornerRadius = CornerRadius(barW / 2f, barW / 2f)
                                    )
                                    drawRoundRect(
                                        color = Color.White,
                                        topLeft = Offset((barW + gap) * 2f, size.height * 0.3f),
                                        size = Size(barW, size.height * 0.5f),
                                        cornerRadius = CornerRadius(barW / 2f, barW / 2f)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = track.title,
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.5.sp,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = track.cleanArtist,
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.5.sp,
                                color = Color.White.copy(alpha = 0.72f),
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
                                tint = if (isFavorite) ActiveHeartCoral else Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            item(key = "autoplay_header", contentType = "autoplay_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "AutoPlay",
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Lagu serupa akan otomatis diputar",
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.5.sp,
                            color = Color.White.copy(alpha = 0.68f)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .height(34.dp)
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (playerState.isAutoPlayEnabled) Color.White.copy(alpha = 0.24f)
                                else Color.White.copy(alpha = 0.10f)
                            )
                            .border(0.8.dp, frostedBorderTint, RoundedCornerShape(50))
                            .clickable(onClick = onToggleAutoPlay)
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AllInclusive,
                            contentDescription = "AutoPlay",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            itemsIndexed(
                items = playerState.autoPlayQueue,
                key = { idx, item -> "q_${item.id}_$idx" },
                contentType = { _, _ -> "queue_track_item" }
            ) { _, item ->
                val thumbReq = remember(context, item.smallThumbnail) {
                    ImageRequest.Builder(context)
                        .data(item.smallThumbnail)
                        .size(128, 128)
                        .allowHardware(true)
                        .crossfade(false)
                        .build()
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onSelectQueueTrack(item) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.DragHandle,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.55f),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    AsyncImage(
                        model = thumbReq,
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(CardPlaceholderDark)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.title,
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.5.sp,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = item.cleanArtist,
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.5.sp,
                            color = Color.White.copy(alpha = 0.70f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(
                        onClick = { onRemoveQueueTrack(item) },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Hapus dari Antrean",
                            tint = Color.White.copy(alpha = 0.65f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            MainPlayerSyncedLyricTeaser(
                lyricsData = lyricsData,
                smoothPositionSecState = smoothPositionSecState,
                durationSec = playerState.durationSec,
                onClick = { onChangeSubView(PlayerSubView.LYRICS) }
            )

            Spacer(modifier = Modifier.height(4.dp))

            IsolatedPlayerSeekBar(
                playerState = playerState,
                onSeekTo = onSeekTo
            )

            Spacer(modifier = Modifier.height(14.dp))

            PlayerTransportRow(
                isPlaying = playerState.isPlaying,
                isBuffering = playerState.isLoading,
                onPreviousTrack = onPreviousTrack,
                onTogglePlayPause = onTogglePlayPause,
                onNextTrack = onNextTrack
            )

            Spacer(modifier = Modifier.height(14.dp))

            SlimVolumeBar(
                volume = playerState.volumeFraction,
                onVolumeChange = onVolumeChange,
                onVolumeStep = onVolumeStep
            )

            Spacer(modifier = Modifier.height(10.dp))

            PureBottomActionRow(
                activeSubView = PlayerSubView.QUEUE,
                isShuffleEnabled = playerState.isShuffleEnabled,
                isRepeatEnabled = playerState.isRepeatEnabled,
                isAutoPlayEnabled = playerState.isAutoPlayEnabled,
                frostedTint = frostedTint,
                frostedBorderTint = frostedBorderTint,
                onChangeSubView = onChangeSubView,
                onToggleShuffle = onToggleShuffle,
                onToggleRepeat = onToggleRepeat,
                onToggleAutoPlay = onToggleAutoPlay
            )
        }
    }
}

// ============================================================================
// WAVY ANIMATED DURATION BAR, CUSTOM TRANSPORT ICONS & DEVICE-SYNCED VOLUME BAR
// ============================================================================
@Composable
private fun IsolatedPlayerSeekBar(
    playerState: PlayerUiState,
    onSeekTo: (Float) -> Unit
) {
    var isDraggingSeek by remember { mutableStateOf(false) }
    var dragSeekFraction by remember { mutableFloatStateOf(0f) }

    val durationMs = (playerState.durationSec.coerceAtLeast(1) * 1000L)
    val currentFraction = if (isDraggingSeek) {
        dragSeekFraction
    } else {
        playerState.progressFraction.coerceIn(0f, 1f)
    }

    val displayedPosMs = (currentFraction * durationMs).toLong()
    val remainingMs = (durationMs - displayedPosMs).coerceAtLeast(0L)

    // Smooth wave animation on the elapsed duration portion while music is playing
    val infiniteWave = rememberInfiniteTransition(label = "seekbar_wave")
    val wavePhase by infiniteWave.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1050, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )

    val waveAmplitudeFactor by animateFloatAsState(
        targetValue = if (playerState.isPlaying && !isDraggingSeek) 1f else 0f,
        animationSpec = tween(durationMillis = 320),
        label = "wave_amp_factor"
    )

    val wavePath = remember { Path() }

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(22.dp)
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val frac = (offset.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f)
                        onSeekTo(frac)
                    }
                }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            isDraggingSeek = true
                            dragSeekFraction = (offset.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f)
                        },
                        onDragEnd = {
                            onSeekTo(dragSeekFraction)
                            isDraggingSeek = false
                        },
                        onDragCancel = { isDraggingSeek = false },
                        onHorizontalDrag = { change, _ ->
                            change.consume()
                            dragSeekFraction = (change.position.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f)
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
            ) {
                val centerY = size.height / 2f
                val strokeWidthPx = 4.dp.toPx()
                val activeW = (size.width * currentFraction).coerceIn(0f, size.width)

                // Inactive portion (to the right of current position)
                if (activeW < size.width) {
                    drawLine(
                        color = Color.White.copy(alpha = 0.25f),
                        start = Offset(activeW, centerY),
                        end = Offset(size.width, centerY),
                        strokeWidth = strokeWidthPx,
                        cap = StrokeCap.Round
                    )
                }

                // Active elapsed portion (wavy sine curve when playing, straight when paused)
                if (activeW > 0f) {
                    val maxAmp = 2.6.dp.toPx() * waveAmplitudeFactor
                    if (maxAmp > 0.15f && activeW > 8.dp.toPx()) {
                        val wavelength = 20.dp.toPx().coerceAtLeast(1f)
                        val stepPx = 2.5.dp.toPx().coerceAtLeast(2f)
                        wavePath.reset()
                        var x = 0f
                        val startY = centerY + sin(-wavePhase) * maxAmp
                        wavePath.moveTo(0f, startY)
                        while (x < activeW) {
                            x = (x + stepPx).coerceAtMost(activeW)
                            val angle = ((x / wavelength) * (2f * PI).toFloat()) - wavePhase
                            // Taper wave slightly near the thumb so the thumb joins smoothly
                            val edgeTaper = ((activeW - x) / 10.dp.toPx()).coerceIn(0f, 1f)
                            val y = centerY + sin(angle) * maxAmp * edgeTaper
                            wavePath.lineTo(x, y)
                        }
                        drawPath(
                            path = wavePath,
                            color = Color.White,
                            style = Stroke(
                                width = strokeWidthPx,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    } else {
                        drawLine(
                            color = Color.White,
                            start = Offset(0f, centerY),
                            end = Offset(activeW, centerY),
                            strokeWidth = strokeWidthPx,
                            cap = StrokeCap.Round
                        )
                    }

                    // Sleek thumb indicator at the active position
                    drawCircle(
                        color = Color.White,
                        radius = if (isDraggingSeek) 6.5.dp.toPx() else 4.2.dp.toPx(),
                        center = Offset(activeW.coerceIn(4.dp.toPx(), size.width - 4.dp.toPx()), centerY)
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatPlaybackDuration(displayedPosMs),
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.72f)
            )
            Text(
                text = "-${formatPlaybackDuration(remainingMs)}",
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.72f)
            )
        }
    }
}

/**
 * Custom Transport Icons matching Reference Design (`Resah Jadi Luka` & `Sakura`):
 * - Previous (`◀◀`): Twin rounded triangles pointing left without vertical bar
 * - Play (`▶`) / Pause (`||`): Rounded solid triangle or twin capsule bars
 * - Next (`▶▶`): Twin rounded triangles pointing right without vertical bar
 */
@Composable
private fun PlayerTransportRow(
    isPlaying: Boolean,
    isBuffering: Boolean,
    onPreviousTrack: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNextTrack: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(CircleShape)
                .clickable(onClick = onPreviousTrack)
                .testTag("full_player_prev_button"),
            contentAlignment = Alignment.Center
        ) {
            CustomDoubleTriangleSeekIcon(
                pointLeft = true,
                tint = Color.White,
                modifier = Modifier.size(38.dp)
            )
        }

        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .clickable(onClick = onTogglePlayPause)
                .testTag("full_player_play_pause_button"),
            contentAlignment = Alignment.Center
        ) {
            if (isBuffering) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(38.dp)
                )
            } else {
                CustomPlayPauseIcon(
                    isPlaying = isPlaying,
                    tint = Color.White,
                    modifier = Modifier.size(44.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(CircleShape)
                .clickable(onClick = onNextTrack)
                .testTag("full_player_next_button"),
            contentAlignment = Alignment.Center
        ) {
            CustomDoubleTriangleSeekIcon(
                pointLeft = false,
                tint = Color.White,
                modifier = Modifier.size(38.dp)
            )
        }
    }
}

@Composable
private fun CustomDoubleTriangleSeekIcon(
    pointLeft: Boolean,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val topY = h * 0.16f
        val botY = h * 0.84f
        val midY = h * 0.50f
        val cornerRadiusPx = w * 0.065f

        drawIntoCanvas { canvas ->
            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                style = android.graphics.Paint.Style.FILL
                color = tint.toArgb()
                pathEffect = CornerPathEffect(cornerRadiusPx)
            }
            val path = android.graphics.Path()
            if (pointLeft) {
                // First (left) triangle pointing left: tip at 0.04w, base at 0.50w
                path.moveTo(w * 0.04f, midY)
                path.lineTo(w * 0.50f, topY)
                path.lineTo(w * 0.50f, botY)
                path.close()

                // Second (right) triangle pointing left: tip at 0.48w, base at 0.94w
                path.moveTo(w * 0.48f, midY)
                path.lineTo(w * 0.94f, topY)
                path.lineTo(w * 0.94f, botY)
                path.close()
            } else {
                // First (left) triangle pointing right: base at 0.06w, tip at 0.52w
                path.moveTo(w * 0.52f, midY)
                path.lineTo(w * 0.06f, topY)
                path.lineTo(w * 0.06f, botY)
                path.close()

                // Second (right) triangle pointing right: base at 0.50w, tip at 0.96w
                path.moveTo(w * 0.96f, midY)
                path.lineTo(w * 0.50f, topY)
                path.lineTo(w * 0.50f, botY)
                path.close()
            }
            canvas.nativeCanvas.drawPath(path, paint)
        }
    }
}

@Composable
private fun CustomPlayPauseIcon(
    isPlaying: Boolean,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        if (isPlaying) {
            // Pause: Two thick vertical rounded pill bars matching Screenshot 4
            val barW = w * 0.24f
            val barH = h * 0.78f
            val topY = (h - barH) / 2f
            val gap = w * 0.18f
            val leftX = (w - (barW * 2f + gap)) / 2f
            val rightX = leftX + barW + gap
            val r = CornerRadius(barW * 0.38f, barW * 0.38f)

            drawRoundRect(
                color = tint,
                topLeft = Offset(leftX, topY),
                size = Size(barW, barH),
                cornerRadius = r
            )
            drawRoundRect(
                color = tint,
                topLeft = Offset(rightX, topY),
                size = Size(barW, barH),
                cornerRadius = r
            )
        } else {
            // Play: Large rounded solid triangle pointing right matching Screenshot 2
            val cornerRadiusPx = w * 0.09f
            drawIntoCanvas { canvas ->
                val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                    style = android.graphics.Paint.Style.FILL
                    color = tint.toArgb()
                    pathEffect = CornerPathEffect(cornerRadiusPx)
                }
                val path = android.graphics.Path().apply {
                    moveTo(w * 0.88f, h * 0.50f)
                    lineTo(w * 0.18f, h * 0.11f)
                    lineTo(w * 0.18f, h * 0.89f)
                    close()
                }
                canvas.nativeCanvas.drawPath(path, paint)
            }
        }
    }
}

@Composable
private fun SlimVolumeBar(
    volume: Float,
    onVolumeChange: (Float) -> Unit,
    onVolumeStep: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Clickable Volume Down Button -> Directly lowers device media volume
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .clickable { onVolumeStep(false) }
                .testTag("volume_down_button"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.VolumeDown,
                contentDescription = "Kurangi Volume Perangkat",
                tint = Color.White.copy(alpha = 0.72f),
                modifier = Modifier.size(19.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .height(24.dp)
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        onVolumeChange((offset.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f))
                    }
                }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { change, _ ->
                        change.consume()
                        onVolumeChange((change.position.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f))
                    }
                }
                .testTag("volume_slider_bar"),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
            ) {
                val h = size.height
                val r = CornerRadius(h / 2f, h / 2f)
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.24f),
                    size = Size(size.width, h),
                    cornerRadius = r
                )
                val activeW = (size.width * volume.coerceIn(0f, 1f)).coerceIn(0f, size.width)
                if (activeW > 0f) {
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.94f),
                        size = Size(activeW, h),
                        cornerRadius = r
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Clickable Volume Up Button -> Directly raises device media volume
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .clickable { onVolumeStep(true) }
                .testTag("volume_up_button"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = "Tambah Volume Perangkat",
                tint = Color.White.copy(alpha = 0.72f),
                modifier = Modifier.size(19.dp)
            )
        }
    }
}

@Composable
private fun PureBottomActionRow(
    activeSubView: PlayerSubView,
    isShuffleEnabled: Boolean,
    isRepeatEnabled: Boolean,
    isAutoPlayEnabled: Boolean,
    frostedTint: Color,
    frostedBorderTint: Color,
    onChangeSubView: (PlayerSubView) -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleAutoPlay: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val isLyricsActive = activeSubView == PlayerSubView.LYRICS
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .then(
                    if (isLyricsActive) {
                        Modifier
                            .background(frostedTint)
                            .border(0.8.dp, frostedBorderTint, CircleShape)
                    } else {
                        Modifier
                    }
                )
                .clickable { onChangeSubView(PlayerSubView.LYRICS) }
                .testTag("bottom_lyrics_quote_button"),
            contentAlignment = Alignment.Center
        ) {
            LyricsQuoteIcon(
                tint = if (isLyricsActive) Color.White else Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(24.dp)
            )
        }

        // Center Shuffle / Repeat / AutoPlay pill ONLY in Queue view
        if (activeSubView == PlayerSubView.QUEUE) {
            Row(
                modifier = Modifier
                    .height(42.dp)
                    .clip(RoundedCornerShape(50))
                    .background(frostedTint)
                    .border(0.8.dp, frostedBorderTint, RoundedCornerShape(50))
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onToggleShuffle,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Acak",
                        tint = if (isShuffleEnabled) Color.White else Color.White.copy(alpha = 0.50f),
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = onToggleRepeat,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Repeat,
                        contentDescription = "Ulangi",
                        tint = if (isRepeatEnabled) Color.White else Color.White.copy(alpha = 0.50f),
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = onToggleAutoPlay,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AllInclusive,
                        contentDescription = "AutoPlay",
                        tint = if (isAutoPlayEnabled) Color.White else Color.White.copy(alpha = 0.50f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        } else {
            Spacer(modifier = Modifier.width(48.dp))
        }

        val isQueueActive = activeSubView == PlayerSubView.QUEUE
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .then(
                    if (isQueueActive) {
                        Modifier
                            .background(frostedTint)
                            .border(0.8.dp, frostedBorderTint, CircleShape)
                    } else {
                        Modifier
                    }
                )
                .clickable { onChangeSubView(PlayerSubView.QUEUE) }
                .testTag("bottom_queue_button"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                contentDescription = "Antrean",
                tint = if (isQueueActive) Color.White else Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(25.dp)
            )
        }
    }
}

@Composable
private fun LyricsQuoteIcon(
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val bubblePath = Path().apply {
            addRoundRect(
                androidx.compose.ui.geometry.RoundRect(
                    left = w * 0.10f,
                    top = h * 0.12f,
                    right = w * 0.90f,
                    bottom = h * 0.74f,
                    cornerRadius = CornerRadius(w * 0.18f, w * 0.18f)
                )
            )
            moveTo(w * 0.26f, h * 0.74f)
            lineTo(w * 0.18f, h * 0.92f)
            lineTo(w * 0.44f, h * 0.74f)
            close()
        }
        drawPath(path = bubblePath, color = tint)

        val quoteColor = Color(0xFF22201C)
        val qW = w * 0.11f
        val qH = h * 0.20f
        drawRoundRect(
            color = quoteColor,
            topLeft = Offset(w * 0.33f, h * 0.32f),
            size = Size(qW, qH),
            cornerRadius = CornerRadius(qW / 2f, qW / 2f)
        )
        drawRoundRect(
            color = quoteColor,
            topLeft = Offset(w * 0.54f, h * 0.32f),
            size = Size(qW, qH),
            cornerRadius = CornerRadius(qW / 2f, qW / 2f)
        )
    }
}

private fun formatPlaybackDuration(ms: Long): String {
    val totalSeconds = (ms / 1000L).coerceAtLeast(0L)
    val mins = totalSeconds / 60L
    val secs = totalSeconds % 60L
    return "%d:%02d".format(mins, secs)
}

private fun extractPlayerAmbientColor(bitmap: Bitmap): Color {
    var rSum = 0L
    var gSum = 0L
    var bSum = 0L
    var count = 0L

    val stepX = (bitmap.width / 12).coerceAtLeast(1)
    val stepY = (bitmap.height / 12).coerceAtLeast(1)

    for (y in 0 until bitmap.height step stepY) {
        for (x in 0 until bitmap.width step stepX) {
            val pixel = bitmap.getPixel(x, y)
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            val maxC = maxOf(r, g, b)
            val minC = minOf(r, g, b)
            if (maxC in 28..242 && (maxC - minC) > 12) {
                rSum += r
                gSum += g
                bSum += b
                count++
            }
        }
    }

    if (count == 0L) return DefaultPlayerWarmColor

    val avgColor = Color(
        red = ((rSum / count).toInt().coerceIn(32, 165)) / 255f,
        green = ((gSum / count).toInt().coerceIn(32, 165)) / 255f,
        blue = ((bSum / count).toInt().coerceIn(32, 165)) / 255f
    )
    return if (avgColor.luminance() > 0.42f) {
        lerp(avgColor, Color(0xFF28241E), 0.45f)
    } else {
        avgColor
    }
}

@android.annotation.SuppressLint("SetJavaScriptEnabled")
@Composable
private fun FullBleedMusicVideoStage(
    videoId: String,
    directVideoStreamUrl: String?,
    isResolvingStream: Boolean,
    isPlaying: Boolean,
    currentPositionSec: Float,
    fallbackCoverReq: ImageRequest,
    dissolveStrength: Float,
    modifier: Modifier = Modifier
) {
    val cleanVideoId = remember(videoId) {
        videoId
            .removePrefix("yt_track_")
            .removePrefix("yt_vid_")
            .removePrefix("artist_video_")
            .substringBefore("_")
            .trim()
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
            }
            .drawWithContent {
                drawContent()
                if (dissolveStrength > 0.01f) {
                    drawRect(
                        brush = Brush.verticalGradient(
                            0.00f to Color.Black,
                            0.56f to Color.Black,
                            0.75f to Color.Black.copy(alpha = 1f - 0.38f * dissolveStrength),
                            0.89f to Color.Black.copy(alpha = 1f - 0.78f * dissolveStrength),
                            1.00f to Color.Black.copy(alpha = 1f - 1.00f * dissolveStrength)
                        ),
                        blendMode = BlendMode.DstIn
                    )
                }
            }
    ) {
        // Backdrop cover while video frame initializes
        AsyncImage(
            model = fallbackCoverReq,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        if (!directVideoStreamUrl.isNullOrBlank()) {
            var isVideoPrepared by remember(directVideoStreamUrl) { mutableStateOf(false) }
            AndroidView(
                factory = { ctx ->
                    android.widget.VideoView(ctx).apply {
                        layoutParams = android.widget.FrameLayout.LayoutParams(
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                            android.view.Gravity.CENTER
                        )
                        setVideoURI(android.net.Uri.parse(directVideoStreamUrl))
                        setOnPreparedListener { mp ->
                            isVideoPrepared = true
                            mp.isLooping = true
                            // Mute video track because high-bitrate audio is already playing via AudioPlayerController
                            mp.setVolume(0f, 0f)
                            try {
                                mp.setVideoScalingMode(android.media.MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
                            } catch (_: Exception) {
                            }
                            val targetMs = (currentPositionSec * 1000f).toInt().coerceAtLeast(0)
                            if (targetMs > 1000 && targetMs < mp.duration) {
                                mp.seekTo(targetMs)
                            }
                            if (isPlaying) {
                                start()
                            }
                        }
                        setOnErrorListener { _, _, _ ->
                            true
                        }
                    }
                },
                update = { videoView ->
                    if (isVideoPrepared) {
                        if (isPlaying && !videoView.isPlaying) {
                            videoView.start()
                        } else if (!isPlaying && videoView.isPlaying) {
                            videoView.pause()
                        }
                        val currentVideoSec = videoView.currentPosition / 1000f
                        if (kotlin.math.abs(currentVideoSec - currentPositionSec) > 2.8f && currentPositionSec > 0f) {
                            videoView.seekTo((currentPositionSec * 1000f).toInt())
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        // Scale full-bleed to fill vertical thumbnail stage without black bars
                        scaleX = 1.75f
                        scaleY = 1.75f
                    }
            )
        } else if (cleanVideoId.isNotBlank()) {
            // Instant full-bleed YouTube HTML5 autoplay embed while direct stream resolves or as universal fallback
            val startSec = currentPositionSec.toInt().coerceAtLeast(0)
            val embedHtml = remember(cleanVideoId) {
                """
                <!DOCTYPE html>
                <html>
                <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                <style>
                    * { margin: 0; padding: 0; box-sizing: border-box; background: #000; overflow: hidden; }
                    html, body { width: 100%; height: 100%; background: #000; overflow: hidden; }
                    .video-wrap {
                        position: fixed;
                        top: 50%; left: 50%;
                        width: 185vw; height: 185vh;
                        transform: translate(-50%, -50%);
                        pointer-events: none;
                    }
                    iframe {
                        width: 100%; height: 100%;
                        border: 0;
                        object-fit: cover;
                    }
                </style>
                </head>
                <body>
                    <div class="video-wrap">
                        <iframe src="https://www.youtube.com/embed/$cleanVideoId?autoplay=1&mute=1&controls=0&showinfo=0&rel=0&modestbranding=1&playsinline=1&enablejsapi=1&loop=1&playlist=$cleanVideoId&start=$startSec"
                            allow="autoplay; encrypted-media" allowfullscreen></iframe>
                    </div>
                </body>
                </html>
                """.trimIndent()
            }
            AndroidView(
                factory = { ctx ->
                    android.webkit.WebView(ctx).apply {
                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                        settings.javaScriptEnabled = true
                        settings.mediaPlaybackRequiresUserGesture = false
                        settings.domStorageEnabled = true
                        settings.loadWithOverviewMode = true
                        settings.useWideViewPort = true
                        isVerticalScrollBarEnabled = false
                        isHorizontalScrollBarEnabled = false
                        webChromeClient = android.webkit.WebChromeClient()
                        webViewClient = android.webkit.WebViewClient()
                        loadDataWithBaseURL(
                            "https://www.youtube.com",
                            embedHtml,
                            "text/html",
                            "utf-8",
                            null
                        )
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        if (isResolvingStream && directVideoStreamUrl.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 86.dp, end = 24.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Memuat stream video HD...",
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.5.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

