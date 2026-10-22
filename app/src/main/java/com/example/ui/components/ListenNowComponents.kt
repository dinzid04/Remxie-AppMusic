package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.R
import com.example.data.model.HomeSection
import com.example.data.model.HomeSectionStyle
import com.example.data.model.HomeThemeMode
import com.example.data.model.MusicItem
import com.example.ui.theme.CardPlaceholderDark
import com.example.ui.theme.PlusJakartaSans
import com.example.ui.theme.TextPrimaryWhite
import com.example.ui.theme.TextSecondaryGrey
import com.example.ui.theme.rememberDominantAccentColor
import kotlinx.coroutines.delay

private val EMOJI_STRIP_REGEX = Regex("[\\p{So}\\p{Cn}\\uFE0F\\u200D]+")
private val MULTI_SPACE_REGEX = Regex("\\s+")

@Composable
fun BitChordTopHeader(
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Canvas(
                modifier = Modifier
                    .size(width = 40.dp, height = 34.dp)
                    .padding(top = 6.dp)
                    .testTag("bitchord_wave_logo")
            ) {
                val w = size.width
                val h = size.height
                val strokeWidth = h * 0.14f

                val path1 = Path().apply {
                    moveTo(w * 0.10f, h * 0.18f)
                    quadraticTo(w * 0.50f, h * 0.36f, w * 0.90f, h * 0.14f)
                }
                val path2 = Path().apply {
                    moveTo(w * 0.05f, h * 0.50f)
                    quadraticTo(w * 0.50f, h * 0.70f, w * 0.95f, h * 0.46f)
                }
                val path3 = Path().apply {
                    moveTo(w * 0.12f, h * 0.82f)
                    quadraticTo(w * 0.50f, h * 0.98f, w * 0.88f, h * 0.78f)
                }

                drawPath(path1, color = Color.White, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))
                drawPath(path2, color = Color.White, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))
                drawPath(path3, color = Color.White, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))
            }

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF17171A))
                    .border(1.dp, Color(0xFF2C2C30), CircleShape)
                    .clickable(onClick = onProfileClick)
                    .padding(6.dp)
                    .testTag("profile_avatar_button"),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_cat_avatar),
                    contentDescription = "Profil & Pengaturan Eksperimental",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = stringResource(R.string.listen_now),
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.Black,
            fontSize = 34.sp,
            letterSpacing = (-1.0).sp,
            color = TextPrimaryWhite,
            modifier = Modifier.testTag("listen_now_title")
        )
    }
}

/**
 * Refined "Pintasan cepat" (Recents / Quick Shortcuts) 3x3 compact square grid
 * shown ONLY when the user has actual listening history (`items.isNotEmpty()`).
 */
@Composable
fun PintasanCepatGridSection(
    items: List<MusicItem>,
    onItemClick: (MusicItem) -> Unit,
    onExpandAllClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return
    val pages = remember(items) { items.chunked(9).take(3) }
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val firstThumb = items.firstOrNull()?.smallThumbnail.orEmpty()
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 18.dp)
            .testTag("pintasan_cepat_section")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onExpandAllClick)
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val headerIconReq = remember(context, firstThumb) {
                    ImageRequest.Builder(context)
                        .data(firstThumb)
                        .size(72, 72)
                        .allowHardware(true)
                        .crossfade(false)
                        .build()
                }
                AsyncImage(
                    model = headerIconReq,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(26.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(CardPlaceholderDark)
                )

                Text(
                    text = "Pintasan cepat",
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp,
                    letterSpacing = (-0.5).sp,
                    color = TextPrimaryWhite
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Lihat Semua Pintasan Cepat",
                tint = TextPrimaryWhite,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        val screenWidthDp = LocalConfiguration.current.screenWidthDp.dp
        val cellSize = remember(screenWidthDp) {
            ((screenWidthDp - 48.dp) / 3f).coerceIn(96.dp, 134.dp)
        }
        val firstPageRows = pages.firstOrNull()?.chunked(3)?.size ?: 1
        val gridHeight = remember(cellSize, firstPageRows) {
            (cellSize * firstPageRows) + (8.dp * (firstPageRows - 1).coerceAtLeast(0))
        }

        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 16.dp),
            pageSpacing = 12.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(gridHeight)
        ) { pageIndex ->
            val pageItems = pages[pageIndex]
            val rows = remember(pageItems) { pageItems.chunked(3) }

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rows.forEach { rowItems ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(cellSize),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (colIdx in 0 until 3) {
                            val item = rowItems.getOrNull(colIdx)
                            if (item != null) {
                                PintasanCepatCard(
                                    item = item,
                                    onClick = { onItemClick(item) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                )
                            } else {
                                Spacer(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (pages.size > 1) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(pages.size) { idx ->
                    val isSelected = pagerState.currentPage == idx
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .size(if (isSelected) 7.dp else 6.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) Color.White else Color.White.copy(alpha = 0.28f)
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun PintasanCepatCard(
    item: MusicItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val imageRequest = remember(context, item.smallThumbnail) {
        ImageRequest.Builder(context)
            .data(item.smallThumbnail)
            .size(220, 220)
            .allowHardware(true)
            .crossfade(false)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .build()
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(CardPlaceholderDark)
            .clickable(onClick = onClick)
            .testTag("shortcut_card_${item.id}")
    ) {
        AsyncImage(
            model = imageRequest,
            contentDescription = item.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithCache {
                    val gradient = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.35f),
                            Color.Black.copy(alpha = 0.86f)
                        ),
                        startY = size.height * 0.35f,
                        endY = size.height
                    )
                    onDrawBehind {
                        drawRect(brush = gradient)
                    }
                }
        )

        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = item.title,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 12.sp,
                lineHeight = 14.sp,
                letterSpacing = (-0.2).sp,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (item.isCollection) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
fun ListenNowSkeletonLoading(
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "skeleton_shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = -300f,
        targetValue = 1100f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1150, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_x"
    )

    val shimmerColorBase = Color(0xFF16161A)
    val shimmerColorHighlight = Color(0xFF26262E)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .testTag("listen_now_skeleton")
    ) {
        Box(
            modifier = Modifier
                .width(156.dp)
                .height(22.dp)
                .clip(RoundedCornerShape(6.dp))
                .drawBehind {
                    val brush = Brush.linearGradient(
                        colors = listOf(shimmerColorBase, shimmerColorHighlight, shimmerColorBase),
                        start = Offset(translateAnim, 0f),
                        end = Offset(translateAnim + 300f, 120f)
                    )
                    drawRect(brush)
                }
        )

        Spacer(modifier = Modifier.height(14.dp))

        repeat(4) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(shimmerColorBase)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.62f)
                            .height(15.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(shimmerColorHighlight)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.40f)
                            .height(12.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(shimmerColorBase)
                    )
                }
            }
        }
    }
}

/**
 * 4-row horizontal track shelf used for "Pilihan cepat", "Cover dan remix", and "Lagu trending untuk kamu".
 */
@Composable
fun FourRowHorizontalTrackShelf(
    title: String,
    items: List<MusicItem>,
    onTrackClick: (MusicItem) -> Unit,
    onTrackMenuClick: (MusicItem) -> Unit,
    onPlayAllClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val columns = remember(items) { items.chunked(4) }
    val cleanTitle = remember(title) { stripEmojisFromTitle(title) }
    val screenWidthDp = LocalConfiguration.current.screenWidthDp.dp
    val columnWidth = remember(screenWidthDp) {
        (screenWidthDp * 0.86f).coerceIn(270.dp, 336.dp)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 20.dp)
    ) {
        SectionHeaderWithPlayAll(
            title = cleanTitle,
            strapline = null,
            showPlayAll = true,
            onPlayAllClick = onPlayAllClick,
            titleTestTag = if (cleanTitle.equals("Pilihan cepat", ignoreCase = true)) "top_shelf_title" else null
        )

        Spacer(modifier = Modifier.height(10.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(264.dp)
                .testTag("top_shelf_horizontal_list")
        ) {
            itemsIndexed(
                items = columns,
                key = { idx, col -> "${cleanTitle}_col_${idx}_${col.firstOrNull()?.id.orEmpty()}" },
                contentType = { _, _ -> "four_row_column" }
            ) { _, colItems ->
                Column(
                    modifier = Modifier
                        .width(columnWidth)
                        .height(264.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    colItems.forEach { track ->
                        CompactTrackRowItem(
                            item = track,
                            onClick = { onTrackClick(track) },
                            onMenuClick = { onTrackMenuClick(track) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeaderWithPlayAll(
    title: String,
    strapline: String?,
    showPlayAll: Boolean,
    onPlayAllClick: () -> Unit,
    titleTestTag: String? = null,
    playAllLabel: String = "Putar semua",
    showArrowOnPlayAll: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 22.sp,
                letterSpacing = (-0.5).sp,
                color = TextPrimaryWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = if (titleTestTag != null) Modifier.testTag(titleTestTag) else Modifier
            )
            if (!strapline.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = strapline,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = TextSecondaryGrey,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (showPlayAll) {
            Spacer(modifier = Modifier.width(12.dp))
            if (showArrowOnPlayAll) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable(onClick = onPlayAllClick)
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .testTag("play_all_${title.lowercase().replace(" ", "_")}"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = playAllLabel,
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = TextSecondaryGrey
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = TextSecondaryGrey,
                        modifier = Modifier.size(16.dp)
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .border(1.dp, Color.White.copy(alpha = 0.24f), RoundedCornerShape(50))
                        .clickable(onClick = onPlayAllClick)
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                        .testTag("play_all_${title.lowercase().replace(" ", "_")}"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = playAllLabel,
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.5.sp,
                        color = TextPrimaryWhite
                    )
                }
            }
        }
    }
}

@Composable
fun CompactTrackRowItem(
    item: MusicItem,
    onClick: () -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val imageRequest = remember(context, item.smallThumbnail) {
        ImageRequest.Builder(context)
            .data(item.smallThumbnail)
            .size(144, 144)
            .allowHardware(true)
            .crossfade(false)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .build()
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp)
            .testTag("compact_track_${item.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = imageRequest,
            contentDescription = item.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(CardPlaceholderDark)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = item.title,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                letterSpacing = (-0.3).sp,
                color = TextPrimaryWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = item.artistWithPlays,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.5.sp,
                letterSpacing = (-0.1).sp,
                color = TextSecondaryGrey,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(
            onClick = onMenuClick,
            modifier = Modifier
                .size(48.dp)
                .testTag("track_menu_${item.id}")
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Opsi untuk ${item.title}",
                tint = TextSecondaryGrey,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * "Rekomendasi harian Anda" section with large square hero cards, animated cover zoom,
 * staggered text reveal, and top-right equalizer icon.
 */
@Composable
fun DailyRecommendationSection(
    section: HomeSection,
    onItemClick: (MusicItem) -> Unit,
    onPlayAllClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cleanTitle = remember(section.title) { stripEmojisFromTitle(section.title) }
    val screenWidthDp = LocalConfiguration.current.screenWidthDp.dp
    val cardSizeDp = remember(screenWidthDp) {
        (screenWidthDp - 44.dp).coerceIn(288.dp, 348.dp)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 22.dp)
            .testTag("home_section_${section.id}")
    ) {
        SectionHeaderWithPlayAll(
            title = cleanTitle,
            strapline = null,
            showPlayAll = true,
            onPlayAllClick = onPlayAllClick
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(cardSizeDp)
        ) {
            itemsIndexed(
                items = section.items,
                key = { idx, item -> "daily_${item.id}_$idx" },
                contentType = { _, _ -> "daily_recommendation_card" }
            ) { index, item ->
                DailyRecommendationHeroCard(
                    item = item,
                    index = index,
                    cardSize = cardSizeDp,
                    onClick = { onItemClick(item) }
                )
            }
        }
    }
}

@Composable
private fun DailyRecommendationHeroCard(
    item: MusicItem,
    index: Int,
    cardSize: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val imageRequest = remember(context, item.highResThumbnail) {
        ImageRequest.Builder(context)
            .data(item.highResThumbnail)
            .size(480, 480)
            .allowHardware(true)
            .crossfade(false)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .build()
    }

    Box(
        modifier = Modifier
            .size(cardSize)
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF3E2723))
            .clickable(onClick = onClick)
            .testTag("daily_rec_card_${item.id}")
    ) {
        AsyncImage(
            model = imageRequest,
            contentDescription = item.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithCache {
                    val topOverlay = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xCC2C1A16),
                            Color(0x662C1A16),
                            Color.Transparent
                        ),
                        startY = 0f,
                        endY = size.height * 0.42f
                    )
                    val bottomOverlay = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0x77241512),
                            Color(0xE61E110E)
                        ),
                        startY = size.height * 0.58f,
                        endY = size.height
                    )
                    onDrawBehind {
                        drawRect(brush = topOverlay)
                        drawRect(brush = bottomOverlay)
                    }
                }
        )

        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    letterSpacing = (-0.4).sp,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = item.artistWithPlays,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.5.sp,
                    color = Color.White.copy(alpha = 0.80f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Canvas(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .size(22.dp)
            ) {
                val barW = size.width * 0.16f
                val gap = size.width * 0.24f
                val corner = CornerRadius(barW / 2f, barW / 2f)

                val h1 = size.height * 0.62f
                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(0f, size.height - h1),
                    size = Size(barW, h1),
                    cornerRadius = corner
                )
                val h2 = size.height * 0.92f
                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(barW + gap, size.height - h2),
                    size = Size(barW, h2),
                    cornerRadius = corner
                )
                val h3 = size.height * 0.74f
                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset((barW + gap) * 2f, size.height - h3),
                    size = Size(barW, h3),
                    cornerRadius = corner
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp)
        ) {
            Text(
                text = item.reason ?: "Untuk penggemar ${item.cleanArtist}",
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Bold,
                fontSize = 14.5.sp,
                color = Color.White.copy(alpha = 0.94f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ============================================================================
// 1. "DARI KOMUNITAS" (PLAYLIST TRENDING KOMUNITAS) LARGE PREVIEW CARDS
// ============================================================================
@Composable
fun CommunityPlaylistCardsSection(
    section: HomeSection,
    savedPlaylistIds: Set<String>,
    onOpenPlaylist: (MusicItem) -> Unit,
    onPlayPreviewTrack: (MusicItem, List<MusicItem>, String) -> Unit,
    onPlayPlaylistFirstTrack: (MusicItem) -> Unit,
    onStartPlaylistRadio: (MusicItem) -> Unit,
    onToggleSavePlaylist: (MusicItem) -> Unit,
    onTrackMenuClick: (MusicItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val screenWidthDp = LocalConfiguration.current.screenWidthDp.dp
    val cardWidth = remember(screenWidthDp) {
        (screenWidthDp - 40.dp).coerceIn(298.dp, 344.dp)
    }

    val cardPalettes = remember {
        listOf(
            Color(0xFF1E222B),
            Color(0xFF281E24),
            Color(0xFF1D2625),
            Color(0xFF27211C)
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 22.dp)
            .testTag("home_section_${section.id}")
    ) {
        Text(
            text = "Dari komunitas",
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 22.sp,
            letterSpacing = (-0.5).sp,
            color = TextPrimaryWhite,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            itemsIndexed(
                items = section.items,
                key = { idx, item -> "comm_card_${item.id}_$idx" },
                contentType = { _, _ -> "community_playlist_card" }
            ) { idx, playlistItem ->
                val bgTint = cardPalettes[idx % cardPalettes.size]
                val isSaved = savedPlaylistIds.contains(playlistItem.id)

                CommunityPlaylistPreviewCard(
                    playlistItem = playlistItem,
                    cardWidth = cardWidth,
                    cardBackground = bgTint,
                    isSaved = isSaved,
                    onOpenPlaylist = { onOpenPlaylist(playlistItem) },
                    onPlayPreviewTrack = { track ->
                        onPlayPreviewTrack(
                            track,
                            playlistItem.previewTracks,
                            "Playing from ${playlistItem.title}"
                        )
                    },
                    onPlayFirstTrack = { onPlayPlaylistFirstTrack(playlistItem) },
                    onStartRadio = { onStartPlaylistRadio(playlistItem) },
                    onToggleSave = { onToggleSavePlaylist(playlistItem) },
                    onTrackMenuClick = onTrackMenuClick
                )
            }
        }
    }
}

@Composable
private fun CommunityPlaylistPreviewCard(
    playlistItem: MusicItem,
    cardWidth: androidx.compose.ui.unit.Dp,
    cardBackground: Color,
    isSaved: Boolean,
    onOpenPlaylist: () -> Unit,
    onPlayPreviewTrack: (MusicItem) -> Unit,
    onPlayFirstTrack: () -> Unit,
    onStartRadio: () -> Unit,
    onToggleSave: () -> Unit,
    onTrackMenuClick: (MusicItem) -> Unit
) {
    val context = LocalContext.current
    val coverReq = remember(context, playlistItem.highResThumbnail) {
        ImageRequest.Builder(context)
            .data(playlistItem.highResThumbnail)
            .size(280, 280)
            .allowHardware(true)
            .crossfade(false)
            .build()
    }

    val previewTracks = remember(playlistItem.previewTracks) {
        playlistItem.previewTracks.take(3)
    }

    Column(
        modifier = Modifier
            .width(cardWidth)
            .clip(RoundedCornerShape(24.dp))
            .background(cardBackground)
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
            .padding(18.dp)
            .testTag("community_playlist_card_${playlistItem.id}")
    ) {
        // Top Header: Cover Art on Left + Title, Author, Views & Track Count on Right
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .clickable(onClick = onOpenPlaylist),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = coverReq,
                contentDescription = playlistItem.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(92.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardPlaceholderDark)
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playlistItem.title,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.5.sp,
                    lineHeight = 22.sp,
                    letterSpacing = (-0.3).sp,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = playlistItem.subtitle.ifBlank { "Musik Hits" },
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp,
                    color = Color.White.copy(alpha = 0.78f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = playlistItem.viewsAndCountText ?: "53 rb x ditonton • 60 lagu",
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.56f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Middle: 3 Preview Tracks from the playlist
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            previewTracks.forEach { track ->
                val trackThumbReq = remember(context, track.smallThumbnail) {
                    ImageRequest.Builder(context)
                        .data(track.smallThumbnail)
                        .size(120, 120)
                        .allowHardware(true)
                        .crossfade(false)
                        .build()
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onPlayPreviewTrack(track) }
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = trackThumbReq,
                        contentDescription = track.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(CardPlaceholderDark)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = track.title,
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(1.dp))
                        Text(
                            text = track.artistWithPlays,
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.5.sp,
                            color = Color.White.copy(alpha = 0.65f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = { onTrackMenuClick(track) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Opsi ${track.title}",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Bottom: 3 Circular Action Buttons (Play First Track, Radio, Save Bookmark)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Play Button (White Circle -> Plays Playlist from Track #1)
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable(onClick = onPlayFirstTrack)
                    .testTag("community_play_btn_${playlistItem.id}"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Putar Playlist dari Urutan Pertama",
                    tint = Color.Black,
                    modifier = Modifier.size(28.dp)
                )
            }

            // 2. Radio Button (Dark Translucent Circle)
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.12f))
                    .clickable(onClick = onStartRadio)
                    .testTag("community_radio_btn_${playlistItem.id}"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Sensors,
                    contentDescription = "Mulai Radio Playlist",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            // 3. Save / Bookmark Button (Dark Translucent Circle)
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSaved) Color(0xFFE94057).copy(alpha = 0.26f)
                        else Color.White.copy(alpha = 0.12f)
                    )
                    .clickable(onClick = onToggleSave)
                    .testTag("community_save_btn_${playlistItem.id}"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    contentDescription = "Simpan Playlist",
                    tint = if (isSaved) Color(0xFFFF6B8B) else Color.White,
                    modifier = Modifier.size(23.dp)
                )
            }
        }
    }
}

// ============================================================================
// 2. NEW DEFAULT THEME SECTIONS: FAVORIT SELAMANYA, HITS INDONESIA, RILIS BARU, MUSIK CADAS
// ============================================================================
@Composable
fun FavoritesForeverNewSection(
    section: HomeSection,
    onItemClick: (MusicItem) -> Unit,
    onPlayAllClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 22.dp)
            .testTag("home_section_${section.id}")
    ) {
        SectionHeaderWithPlayAll(
            title = "Favorit selamanya!",
            strapline = section.strapline ?: "Lagu-lagu yang selalu ada di hati.",
            showPlayAll = true,
            onPlayAllClick = onPlayAllClick,
            playAllLabel = "Lihat semua",
            showArrowOnPlayAll = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(
                items = section.items,
                key = { item -> "fav_forever_${item.id}" },
                contentType = { "fav_forever_card" }
            ) { item ->
                val imgReq = remember(context, item.highResThumbnail) {
                    ImageRequest.Builder(context)
                        .data(item.highResThumbnail)
                        .size(240, 240)
                        .allowHardware(true)
                        .crossfade(false)
                        .build()
                }

                Row(
                    modifier = Modifier
                        .width(262.dp)
                        .height(106.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF15151B))
                        .border(1.dp, Color(0xFF252530), RoundedCornerShape(20.dp))
                        .clickable { onItemClick(item) }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = imgReq,
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(82.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(CardPlaceholderDark)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 4.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = item.title,
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = item.cleanArtist,
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = TextSecondaryGrey,
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
fun HitsIndonesiaSpotlightSection(
    section: HomeSection,
    onItemClick: (MusicItem) -> Unit,
    onTrackMenuClick: (MusicItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val heroItem = section.items.firstOrNull() ?: return
    val rankedItems = remember(section.items) { section.items.drop(1).take(3) }

    val extractedHeroAccent = rememberDominantAccentColor(
        imageUrl = heroItem.smallThumbnail,
        defaultColor = Color(0xFF5A2538)
    )
    val heroCardBase = remember(extractedHeroAccent) {
        Color(
            red = (extractedHeroAccent.red * 0.28f).coerceIn(0f, 1f),
            green = (extractedHeroAccent.green * 0.28f).coerceIn(0f, 1f),
            blue = (extractedHeroAccent.blue * 0.28f).coerceIn(0f, 1f),
            alpha = 1f
        )
    }

    val heroImgReq = remember(context, heroItem.highResThumbnail) {
        ImageRequest.Builder(context)
            .data(heroItem.highResThumbnail)
            .size(380, 380)
            .allowHardware(true)
            .crossfade(false)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .build()
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 22.dp)
            .testTag("home_section_${section.id}")
    ) {
        SectionHeaderWithPlayAll(
            title = "Hits Indonesia",
            strapline = "Lagu-lagu paling populer di Indonesia saat ini.",
            showPlayAll = false,
            onPlayAllClick = {}
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Hero Card (#1 Hits Indonesia — no "LAGU TERPOPULER" badge, seamless horizontal & vertical blend)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(194.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(heroCardBase)
                .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(24.dp))
                .clickable { onItemClick(heroItem) }
                .testTag("hits_indonesia_hero_card")
        ) {
            // Right-aligned artwork with offscreen alpha feathering so there is zero hard border
            AsyncImage(
                model = heroImgReq,
                contentDescription = heroItem.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxWidth(0.74f)
                    .fillMaxHeight()
                    .graphicsLayer {
                        compositingStrategy = CompositingStrategy.Offscreen
                    }
                    .drawWithContent {
                        drawContent()
                        drawRect(
                            brush = Brush.horizontalGradient(
                                0.0f to Color.Transparent,
                                0.22f to Color.Black.copy(alpha = 0.28f),
                                0.50f to Color.Black.copy(alpha = 0.78f),
                                1.0f to Color.Black
                            ),
                            blendMode = BlendMode.DstIn
                        )
                    }
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            0.0f to heroCardBase,
                            0.38f to heroCardBase.copy(alpha = 0.92f),
                            0.68f to heroCardBase.copy(alpha = 0.52f),
                            1.0f to heroCardBase.copy(alpha = 0.18f)
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.fillMaxWidth(0.76f)) {
                    Text(
                        text = heroItem.title,
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Black,
                        fontSize = 27.sp,
                        letterSpacing = (-0.6).sp,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = heroItem.reason ?: "Kisah cinta dan melodi populer yang sedang memuncaki tangga lagu Indonesia.",
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.5.sp,
                        lineHeight = 16.sp,
                        color = Color.White.copy(alpha = 0.82f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Artist Pill Chip
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color.Black.copy(alpha = 0.48f))
                            .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(50))
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = heroImgReq,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(CardPlaceholderDark)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = heroItem.cleanArtist,
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // White Circular Play Button
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .clickable { onItemClick(heroItem) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Putar ${heroItem.title}",
                            tint = Color.Black,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }
        }

        if (rankedItems.isNotEmpty()) {
            Spacer(modifier = Modifier.height(14.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rankedItems.forEachIndexed { idx, track ->
                    val rankNum = idx + 1
                    val trackReq = remember(context, track.smallThumbnail) {
                        ImageRequest.Builder(context)
                            .data(track.smallThumbnail)
                            .size(144, 144)
                            .allowHardware(true)
                            .crossfade(false)
                            .build()
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onItemClick(track) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = rankNum.toString(),
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = TextSecondaryGrey,
                            modifier = Modifier.width(24.dp)
                        )

                        AsyncImage(
                            model = trackReq,
                            contentDescription = track.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(CardPlaceholderDark)
                        )

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
                                fontSize = 13.sp,
                                color = TextSecondaryGrey,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        IconButton(
                            onClick = { onTrackMenuClick(track) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Menu ${track.title}",
                                tint = TextSecondaryGrey,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NewReleasesBentoSection(
    section: HomeSection,
    onItemClick: (MusicItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val featuredItem = section.items.firstOrNull() ?: return
    val rightTopItem = section.items.getOrNull(1) ?: featuredItem
    val rightBottomItem = section.items.getOrNull(2) ?: rightTopItem

    val featuredAccent = rememberDominantAccentColor(
        imageUrl = featuredItem.smallThumbnail,
        defaultColor = Color(0xFF4A2C40)
    )
    val featuredCardBg = remember(featuredAccent) {
        Color(
            red = (featuredAccent.red * 0.28f).coerceIn(0f, 1f),
            green = (featuredAccent.green * 0.28f).coerceIn(0f, 1f),
            blue = (featuredAccent.blue * 0.28f).coerceIn(0f, 1f),
            alpha = 1f
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 22.dp)
            .testTag("home_section_${section.id}")
    ) {
        SectionHeaderWithPlayAll(
            title = "Rilis baru",
            strapline = "Album dan single terbaru untuk kamu.",
            showPlayAll = false,
            onPlayAllClick = {}
        )

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(242.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Left Vertical Featured Card with seamless opacity/blur gradient blend into adaptive card color
            val leftReq = remember(context, featuredItem.highResThumbnail) {
                ImageRequest.Builder(context)
                    .data(featuredItem.highResThumbnail)
                    .size(320, 320)
                    .allowHardware(true)
                    .crossfade(false)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .build()
            }

            Box(
                modifier = Modifier
                    .weight(0.47f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(22.dp))
                    .background(featuredCardBg)
                    .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(22.dp))
                    .clickable { onItemClick(featuredItem) }
            ) {
                // Full-width top cover artwork that feathers seamlessly into featuredCardBg (zero hard border!)
                AsyncImage(
                    model = leftReq,
                    contentDescription = featuredItem.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.78f)
                        .align(Alignment.TopCenter)
                        .graphicsLayer {
                            compositingStrategy = CompositingStrategy.Offscreen
                        }
                        .drawWithContent {
                            drawContent()
                            drawRect(
                                brush = Brush.verticalGradient(
                                    0.0f to Color.Black,
                                    0.46f to Color.Black,
                                    0.72f to Color.Black.copy(alpha = 0.52f),
                                    0.90f to Color.Black.copy(alpha = 0.14f),
                                    1.0f to Color.Transparent
                                ),
                                blendMode = BlendMode.DstIn
                            )
                        }
                )

                // Soft atmospheric gradient wash
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0.0f to Color.Black.copy(alpha = 0.14f),
                                0.35f to Color.Transparent,
                                0.62f to featuredCardBg.copy(alpha = 0.72f),
                                0.80f to featuredCardBg.copy(alpha = 0.96f),
                                1.0f to featuredCardBg
                            )
                        )
                )

                // "Baru" badge
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.Black.copy(alpha = 0.58f))
                        .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(50))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Baru",
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 10.5.sp,
                        color = Color.White
                    )
                }

                // Bottom info anchored in the seamless gradient zone
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(horizontal = 13.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = featuredItem.title,
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.5.sp,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = featuredItem.cleanArtist,
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.5.sp,
                        color = Color.White.copy(alpha = 0.80f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Album • Pop",
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.5.sp,
                        color = Color.White.copy(alpha = 0.60f)
                    )
                }
            }

            // Right Column: 2 Stacked Horizontal Cards with seamless horizontal opacity/color blend
            Column(
                modifier = Modifier
                    .weight(0.53f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                listOf(rightTopItem, rightBottomItem).forEach { item ->
                    val itemAccent = rememberDominantAccentColor(
                        imageUrl = item.smallThumbnail,
                        defaultColor = Color(0xFF283044)
                    )
                    val rightCardBg = remember(itemAccent) {
                        Color(
                            red = (itemAccent.red * 0.28f).coerceIn(0f, 1f),
                            green = (itemAccent.green * 0.28f).coerceIn(0f, 1f),
                            blue = (itemAccent.blue * 0.28f).coerceIn(0f, 1f),
                            alpha = 1f
                        )
                    }
                    val rightReq = remember(context, item.smallThumbnail) {
                        ImageRequest.Builder(context)
                            .data(item.smallThumbnail)
                            .size(220, 220)
                            .allowHardware(true)
                            .crossfade(false)
                            .memoryCachePolicy(CachePolicy.ENABLED)
                            .diskCachePolicy(CachePolicy.ENABLED)
                            .build()
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(rightCardBg)
                            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(20.dp))
                            .clickable { onItemClick(item) }
                    ) {
                        // Left artwork seamlessly fading into rightCardBg (no hard box border!)
                        AsyncImage(
                            model = rightReq,
                            contentDescription = item.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .fillMaxWidth(0.60f)
                                .fillMaxHeight()
                                .graphicsLayer {
                                    compositingStrategy = CompositingStrategy.Offscreen
                                }
                                .drawWithContent {
                                    drawContent()
                                    drawRect(
                                        brush = Brush.horizontalGradient(
                                            0.0f to Color.Black,
                                            0.42f to Color.Black.copy(alpha = 0.82f),
                                            0.76f to Color.Black.copy(alpha = 0.28f),
                                            1.0f to Color.Transparent
                                        ),
                                        blendMode = BlendMode.DstIn
                                    )
                                }
                        )

                        // Smooth horizontal gradient wash into rightCardBg
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.horizontalGradient(
                                        0.0f to Color.Black.copy(alpha = 0.15f),
                                        0.28f to rightCardBg.copy(alpha = 0.55f),
                                        0.52f to rightCardBg.copy(alpha = 0.92f),
                                        1.0f to rightCardBg
                                    )
                                )
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .clip(RoundedCornerShape(50))
                                    .background(Color.Black.copy(alpha = 0.45f))
                                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(50))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Baru",
                                    fontFamily = PlusJakartaSans,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.5.sp,
                                    color = Color.White.copy(alpha = 0.90f)
                                )
                            }

                            Column(
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .fillMaxWidth(0.66f)
                                    .padding(top = 6.dp)
                            ) {
                                Text(
                                    text = item.title,
                                    fontFamily = PlusJakartaSans,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp,
                                    lineHeight = 17.sp,
                                    color = Color.White,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = item.cleanArtist,
                                    fontFamily = PlusJakartaSans,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.5.sp,
                                    color = Color.White.copy(alpha = 0.78f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .fillMaxWidth(0.66f),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Album",
                                    fontFamily = PlusJakartaSans,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 10.5.sp,
                                    color = Color.White.copy(alpha = 0.60f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MusikCadasRockSection(
    section: HomeSection,
    onItemClick: (MusicItem) -> Unit,
    onPlayAllClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 24.dp)
            .testTag("home_section_${section.id}")
    ) {
        SectionHeaderWithPlayAll(
            title = "Musik Cadas",
            strapline = "Rock, metal, dan musik keras pilihan.",
            showPlayAll = false,
            onPlayAllClick = {}
        )

        Spacer(modifier = Modifier.height(14.dp))

        // "Rock Never Dies" Concert Banner Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(152.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xFF26120E))
                .border(1.dp, Color(0xFF4A251B), RoundedCornerShape(22.dp))
                .clickable(onClick = onPlayAllClick)
                .testTag("rock_never_dies_banner")
        ) {
            // Atmospheric Concert Stage Spotlights & Silhouette Backdrop
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Warm amber/crimson stage glow
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFE65C2E).copy(alpha = 0.55f),
                            Color(0xFF8C2318).copy(alpha = 0.35f),
                            Color(0xFF140A08)
                        ),
                        center = Offset(w * 0.72f, h * 0.28f),
                        radius = w * 0.75f
                    )
                )

                // Stage light beams
                val beamPaint = Path().apply {
                    moveTo(w * 0.78f, 0f)
                    lineTo(w * 0.42f, h)
                    lineTo(w * 0.96f, h)
                    close()
                }
                drawPath(
                    path = beamPaint,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFFFB74D).copy(alpha = 0.25f),
                            Color.Transparent
                        )
                    )
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Rock Never Dies",
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Black,
                        fontSize = 24.sp,
                        letterSpacing = (-0.5).sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Kumpulan lagu rock terbaik sepanjang masa.",
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.80f)
                    )
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.White)
                        .clickable(onClick = onPlayAllClick)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Dengarkan Sekarang",
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.5.sp,
                        color = Color.Black
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Row of Square Rock Album/Song Cards below the Banner
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(
                items = section.items,
                key = { item -> "rock_card_${item.id}" },
                contentType = { "rock_music_card" }
            ) { item ->
                val imgReq = remember(context, item.highResThumbnail) {
                    ImageRequest.Builder(context)
                        .data(item.highResThumbnail)
                        .size(280, 280)
                        .allowHardware(true)
                        .crossfade(false)
                        .build()
                }

                Column(
                    modifier = Modifier
                        .width(136.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onItemClick(item) }
                ) {
                    AsyncImage(
                        model = imgReq,
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(136.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(CardPlaceholderDark)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = item.title,
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.5.sp,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.cleanArtist,
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
}

@Composable
fun MusicCarouselSection(
    section: HomeSection,
    homeThemeMode: HomeThemeMode = HomeThemeMode.DEFAULT_APP,
    savedPlaylistIds: Set<String> = emptySet(),
    onItemClick: (MusicItem) -> Unit,
    onTrackMenuClick: (MusicItem) -> Unit = {},
    onPlayAllClick: () -> Unit = {},
    onPlayCommunityTrack: (MusicItem, List<MusicItem>, String) -> Unit = { item, _, _ -> onItemClick(item) },
    onPlayCommunityFirstTrack: (MusicItem) -> Unit = onItemClick,
    onStartCommunityRadio: (MusicItem) -> Unit = onItemClick,
    onToggleSaveCommunityPlaylist: (MusicItem) -> Unit = {},
    modifier: Modifier = Modifier
) {
    when (section.style) {
        HomeSectionStyle.DAILY_RECOMMENDATION -> {
            DailyRecommendationSection(
                section = section,
                onItemClick = onItemClick,
                onPlayAllClick = onPlayAllClick,
                modifier = modifier
            )
        }
        HomeSectionStyle.FOUR_ROW_SHELF -> {
            FourRowHorizontalTrackShelf(
                title = section.title,
                items = section.items,
                onTrackClick = onItemClick,
                onTrackMenuClick = onTrackMenuClick,
                onPlayAllClick = onPlayAllClick,
                modifier = modifier
            )
        }
        HomeSectionStyle.COMMUNITY_PLAYLIST_CARDS -> {
            CommunityPlaylistCardsSection(
                section = section,
                savedPlaylistIds = savedPlaylistIds,
                onOpenPlaylist = onItemClick,
                onPlayPreviewTrack = onPlayCommunityTrack,
                onPlayPlaylistFirstTrack = onPlayCommunityFirstTrack,
                onStartPlaylistRadio = onStartCommunityRadio,
                onToggleSavePlaylist = onToggleSaveCommunityPlaylist,
                onTrackMenuClick = onTrackMenuClick,
                modifier = modifier
            )
        }
        HomeSectionStyle.FAVORITES_FOREVER_CARDS -> {
            if (homeThemeMode == HomeThemeMode.DEFAULT_APP) {
                FavoritesForeverNewSection(
                    section = section,
                    onItemClick = onItemClick,
                    onPlayAllClick = onPlayAllClick,
                    modifier = modifier
                )
            } else {
                StandardYtmCarouselSection(
                    section = section,
                    onItemClick = onItemClick,
                    onPlayAllClick = onPlayAllClick,
                    modifier = modifier
                )
            }
        }
        HomeSectionStyle.HITS_INDONESIA_SPOTLIGHT -> {
            if (homeThemeMode == HomeThemeMode.DEFAULT_APP) {
                HitsIndonesiaSpotlightSection(
                    section = section,
                    onItemClick = onItemClick,
                    onTrackMenuClick = onTrackMenuClick,
                    modifier = modifier
                )
            } else {
                StandardYtmCarouselSection(
                    section = section,
                    onItemClick = onItemClick,
                    onPlayAllClick = onPlayAllClick,
                    modifier = modifier
                )
            }
        }
        HomeSectionStyle.NEW_RELEASES_BENTO -> {
            if (homeThemeMode == HomeThemeMode.DEFAULT_APP) {
                NewReleasesBentoSection(
                    section = section,
                    onItemClick = onItemClick,
                    modifier = modifier
                )
            } else {
                StandardYtmCarouselSection(
                    section = section,
                    onItemClick = onItemClick,
                    onPlayAllClick = onPlayAllClick,
                    modifier = modifier
                )
            }
        }
        HomeSectionStyle.MUSIK_CADAS_ROCK -> {
            if (homeThemeMode == HomeThemeMode.DEFAULT_APP) {
                MusikCadasRockSection(
                    section = section,
                    onItemClick = onItemClick,
                    onPlayAllClick = onPlayAllClick,
                    modifier = modifier
                )
            } else {
                StandardYtmCarouselSection(
                    section = section,
                    onItemClick = onItemClick,
                    onPlayAllClick = onPlayAllClick,
                    modifier = modifier
                )
            }
        }
        HomeSectionStyle.STANDARD_CAROUSEL -> {
            StandardYtmCarouselSection(
                section = section,
                onItemClick = onItemClick,
                onPlayAllClick = onPlayAllClick,
                modifier = modifier
            )
        }
    }
}

@Composable
private fun StandardYtmCarouselSection(
    section: HomeSection,
    onItemClick: (MusicItem) -> Unit,
    onPlayAllClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cleanTitle = remember(section.title) { stripEmojisFromTitle(section.title) }
    val cleanStrapline = remember(section.strapline) {
        section.strapline?.let { stripEmojisFromTitle(it) }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 20.dp)
            .testTag("home_section_${section.id}")
    ) {
        SectionHeaderWithPlayAll(
            title = cleanTitle,
            strapline = cleanStrapline,
            showPlayAll = false,
            onPlayAllClick = onPlayAllClick
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(208.dp)
        ) {
            items(
                items = section.items,
                key = { item -> "${section.id}_${item.id}" },
                contentType = { "carousel_music_card" }
            ) { item ->
                CarouselMusicCard(
                    item = item,
                    onClick = { onItemClick(item) }
                )
            }
        }
    }
}

@Composable
fun CarouselMusicCard(
    item: MusicItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val imageRequest = remember(context, item.highResThumbnail) {
        ImageRequest.Builder(context)
            .data(item.highResThumbnail)
            .size(300, 300)
            .allowHardware(true)
            .crossfade(false)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .build()
    }

    Column(
        modifier = modifier
            .width(156.dp)
            .height(208.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag("carousel_card_${item.id}")
    ) {
        AsyncImage(
            model = imageRequest,
            contentDescription = item.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(156.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(CardPlaceholderDark)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = item.title,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 15.5.sp,
            letterSpacing = (-0.3).sp,
            color = TextPrimaryWhite,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = if (item.isCollection) item.subtitle.ifBlank { "Playlist" } else item.cleanArtist,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            letterSpacing = (-0.1).sp,
            color = TextSecondaryGrey,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun stripEmojisFromTitle(input: String): String {
    return input
        .replace(EMOJI_STRIP_REGEX, "")
        .replace(MULTI_SPACE_REGEX, " ")
        .trim()
}
