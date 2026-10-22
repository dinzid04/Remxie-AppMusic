package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.lyrics.LyricAlignment
import com.example.data.lyrics.LyricLine
import com.example.data.lyrics.LyricsProviderState
import com.example.data.lyrics.LyricsSettingsState
import com.example.data.lyrics.LyricsSource
import com.example.data.lyrics.TRANSLATION_LANGUAGES
import com.example.data.lyrics.translationLanguageName
import com.example.ui.theme.PlusJakartaSans
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextPrimaryWhite
import com.example.ui.theme.TextSecondaryGrey
import kotlin.math.abs
import kotlin.math.roundToInt

private val AccentCoral = Color(0xFFFF4D67)
private val FoundGreen = Color(0xFF38EF7D)

@Composable
fun BitChordSyncedLyricsPanel(
    lines: List<LyricLine>,
    translatedLines: List<LyricLine>?,
    romanizedLines: List<LyricLine>?,
    isTranslationEnabled: Boolean,
    isRomanizationEnabled: Boolean,
    isTranslating: Boolean,
    activeSource: LyricsSource?,
    isEmbeddedLyrics: Boolean,
    isLoadingLyrics: Boolean,
    lyricsSettings: LyricsSettingsState,
    currentTrackOffsetMs: Int,
    clockMsState: State<Long>,
    durationMs: Long,
    listState: LazyListState,
    nestedScrollConnection: NestedScrollConnection,
    frostedTint: Color,
    frostedBorderTint: Color,
    onSeekToMs: (Long) -> Unit,
    onOpenProviderSheet: () -> Unit,
    onOpenSourcesDialog: () -> Unit,
    onOpenOffsetSheet: () -> Unit,
    onToggleTranslation: () -> Unit,
    onToggleRomanization: () -> Unit,
    onSelectTranslationLanguage: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showLanguagePickerDialog by remember { mutableStateOf(false) }

    val hasTimestamps = remember(lines) {
        lines.any { it.timeMs > 0L || it.isWordSynced }
    }
    val isSyncedMode = lyricsSettings.syncedLyrics && hasTimestamps

    val positionMs = clockMsState.value
    val quantizedTick = (positionMs / 35L).toInt()

    val activeIndices = remember(lines, quantizedTick, isSyncedMode) {
        if (!isSyncedMode) emptyList() else activeLyricRows(lines, positionMs)
    }
    val primaryActiveIndex = activeIndices.firstOrNull() ?: -1

    LaunchedEffect(primaryActiveIndex, isSyncedMode, lines.size) {
        if (isSyncedMode && primaryActiveIndex >= 0 && !listState.isScrollInProgress && lines.isNotEmpty()) {
            val targetIdx = (primaryActiveIndex - 1).coerceAtLeast(0)
            try {
                listState.animateScrollToItem(targetIdx)
            } catch (_: Exception) {
            }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Top BitChord Lyrics Toolbar (Source Pill, Translate, Romanize, Offset, Sources Priority)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Active Provider Chip
            val sourceLabel = when {
                isLoadingLyrics && lines.isEmpty() -> "Mencari lirik..."
                isEmbeddedLyrics -> "Embedded LRC"
                activeSource != null -> activeSource.displayName
                lines.isNotEmpty() -> "Lrc.red"
                else -> "Pilih Sumber"
            }
            val syncBadge = remember(lines) {
                when {
                    lines.any { line -> line.words.any { it.syllables.isNotEmpty() } } -> "Syllable"
                    lines.any { it.isWordSynced } -> "Word-Sync"
                    lines.any { it.timeMs > 0L } -> "LRC"
                    lines.isNotEmpty() -> "Plain"
                    else -> null
                }
            }

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(frostedTint)
                    .border(0.8.dp, frostedBorderTint, RoundedCornerShape(50))
                    .clickable(onClick = onOpenProviderSheet)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .testTag("bitchord_lyrics_provider_chip"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isLoadingLyrics) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(13.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    text = sourceLabel,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.5.sp,
                    color = Color.White,
                    maxLines = 1,
                )
                if (syncBadge != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(AccentCoral.copy(alpha = 0.85f))
                            .padding(horizontal = 6.dp, vertical = 1.5.dp),
                    ) {
                        Text(
                            text = syncBadge,
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.5.sp,
                            color = Color.White,
                        )
                    }
                }
            }

            // Right Action Pills: Romanize (A↔あ), Translate, Offset (±0.0s), Sources Priority
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Romanize Toggle Pill
                Box(
                    modifier = Modifier
                        .height(32.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (isRomanizationEnabled) AccentCoral.copy(alpha = 0.88f) else frostedTint)
                        .border(0.8.dp, frostedBorderTint, RoundedCornerShape(50))
                        .clickable(onClick = onToggleRomanization)
                        .padding(horizontal = 10.dp)
                        .testTag("lyrics_romanize_toggle_button"),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Romaji",
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.5.sp,
                        color = Color.White,
                    )
                }

                // Translate Toggle Pill (Long-click / language icon opens language picker)
                Row(
                    modifier = Modifier
                        .height(32.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (isTranslationEnabled) AccentCoral.copy(alpha = 0.88f) else frostedTint)
                        .border(0.8.dp, frostedBorderTint, RoundedCornerShape(50))
                        .clickable(onClick = onToggleTranslation)
                        .padding(horizontal = 10.dp)
                        .testTag("lyrics_translate_toggle_button"),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (isTranslating) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 1.8.dp,
                            modifier = Modifier.size(12.dp),
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Translate,
                            contentDescription = "Terjemahkan Lirik",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = lyricsSettings.translationLanguage.uppercase().take(2),
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp,
                        color = Color.White,
                        modifier = Modifier.clickable { showLanguagePickerDialog = true },
                    )
                }

                // Offset Button
                val offsetSec = currentTrackOffsetMs / 1000f
                val offsetLabel = if (currentTrackOffsetMs == 0) {
                    "0.0s"
                } else {
                    "%+.1fs".format(offsetSec)
                }
                Row(
                    modifier = Modifier
                        .height(32.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (currentTrackOffsetMs != 0) AccentCoral.copy(alpha = 0.88f) else frostedTint)
                        .border(0.8.dp, frostedBorderTint, RoundedCornerShape(50))
                        .clickable(onClick = onOpenOffsetSheet)
                        .padding(horizontal = 9.dp)
                        .testTag("lyrics_offset_button"),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = "Atur Offset Lirik",
                        tint = Color.White,
                        modifier = Modifier.size(13.dp),
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = offsetLabel,
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp,
                        color = Color.White,
                    )
                }

                // Sources Priority Button
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(frostedTint)
                        .border(0.8.dp, frostedBorderTint, CircleShape)
                        .clickable(onClick = onOpenSourcesDialog)
                        .testTag("lyrics_sources_priority_button"),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Urutan Sumber Lirik",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }

        // Main Lyrics Scroll Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .nestedScroll(nestedScrollConnection),
        ) {
            if (lines.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (isLoadingLyrics) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.8.dp,
                                modifier = Modifier.size(34.dp),
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Mencari lirik tersinkronisasi dari 15+ provider...",
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.White.copy(alpha = 0.85f),
                                textAlign = TextAlign.Center,
                            )
                        } else {
                            Text(
                                text = "Lirik tidak ditemukan untuk lagu ini",
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                color = Color.White.copy(alpha = 0.85f),
                                textAlign = TextAlign.Center,
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Ketuk untuk mengganti provider atau mengatur urutan sumber lirik",
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.5.sp,
                                color = Color.White.copy(alpha = 0.62f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.clickable(onClick = onOpenProviderSheet),
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(
                        start = 24.dp,
                        end = 24.dp,
                        top = 12.dp,
                        bottom = 110.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("lyrics_lazy_column"),
                ) {
                    itemsIndexed(
                        items = lines,
                        key = { idx, line -> "${idx}_${line.timeMs}_${line.text.hashCode()}" },
                        contentType = { _, line -> if (line.isGap) "gap_line" else "lyric_line" },
                    ) { index, line ->
                        val isCurrent = isSyncedMode && (index in activeIndices)
                        val isPassed = isSyncedMode && !isCurrent && (primaryActiveIndex > index)
                        val distance = if (primaryActiveIndex >= 0) abs(index - primaryActiveIndex) else 0

                        val translatedText = if (isTranslationEnabled) {
                            translatedLines?.getOrNull(index)?.text?.takeIf { it.isNotBlank() && it != line.text }
                        } else null

                        val romanizedText = if (isRomanizationEnabled) {
                            romanizedLines?.getOrNull(index)?.text?.takeIf { it.isNotBlank() && it != line.text }
                        } else null

                        if (line.isGap) {
                            val gapEnd = line.sungUntilMs ?: lines.getOrNull(index + 1)?.timeMs ?: (line.timeMs + 4_000L)
                            if (isCurrent) {
                                BitChordInstrumentalDots(
                                    startMs = line.timeMs,
                                    endMs = gapEnd,
                                    positionMs = positionMs,
                                    alignment = line.alignment,
                                )
                            }
                        } else {
                            BitChordLyricRow(
                                line = line,
                                translatedText = translatedText,
                                romanizedText = romanizedText,
                                isSyncedMode = isSyncedMode,
                                isCurrent = isCurrent,
                                isPassed = isPassed,
                                distanceFromActive = distance,
                                enableBlur = lyricsSettings.lyricsBlur,
                                reduceAnimation = lyricsSettings.reduceAnimation,
                                clockMsState = clockMsState,
                                onClick = {
                                    if (hasTimestamps) {
                                        onSeekToMs(line.timeMs)
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    if (showLanguagePickerDialog) {
        TranslationLanguageDialog(
            selectedCode = lyricsSettings.translationLanguage,
            onSelectLanguage = { code ->
                onSelectTranslationLanguage(code)
                showLanguagePickerDialog = false
            },
            onDismiss = { showLanguagePickerDialog = false },
        )
    }
}

@Composable
private fun BitChordLyricRow(
    line: LyricLine,
    translatedText: String?,
    romanizedText: String?,
    isSyncedMode: Boolean,
    isCurrent: Boolean,
    isPassed: Boolean,
    distanceFromActive: Int,
    enableBlur: Boolean,
    reduceAnimation: Boolean,
    clockMsState: State<Long>,
    onClick: () -> Unit,
) {
    val isRightAligned = line.alignment == LyricAlignment.End
    val textAlign = if (isRightAligned) TextAlign.End else TextAlign.Start
    val horizontalAlign = if (isRightAligned) Alignment.End else Alignment.Start
    val originX = if (isRightAligned) 1f else 0f

    val targetScale = when {
        !isSyncedMode || reduceAnimation -> 1.0f
        isCurrent -> 1.045f
        isPassed -> 1.0f
        else -> 0.968f
    }
    val rowScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = spring(
            dampingRatio = 0.82f,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "bitchord_row_scale",
    )

    val blurDp = if (enableBlur && isSyncedMode && !isCurrent && distanceFromActive >= 2) {
        (distanceFromActive * 0.9f).coerceAtMost(3.2f).dp
    } else {
        0.dp
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = rowScale
                scaleY = rowScale
                transformOrigin = TransformOrigin(originX, 0.5f)
            }
            .then(
                if (blurDp > 0.dp) {
                    Modifier.blur(blurDp, BlurredEdgeTreatment.Unbounded)
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick)
            .padding(vertical = 3.dp),
        horizontalAlignment = horizontalAlign,
    ) {
        // Optional Romanized line above or below main lyric
        if (!romanizedText.isNullOrBlank()) {
            Text(
                text = romanizedText,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Bold,
                fontSize = if (isCurrent) 15.5.sp else 14.5.sp,
                color = Color.White.copy(alpha = if (isCurrent || !isSyncedMode) 0.78f else 0.34f),
                textAlign = textAlign,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(2.dp))
        }

        if (!isSyncedMode) {
            Text(
                text = line.text,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Black,
                fontSize = 24.5.sp,
                lineHeight = 32.sp,
                letterSpacing = (-0.4).sp,
                color = Color.White.copy(alpha = 0.96f),
                textAlign = textAlign,
                modifier = Modifier.fillMaxWidth(),
            )
        } else if (isCurrent) {
            val posMs = clockMsState.value
            val annotated = buildBitChordWordSweepString(
                line = line,
                positionMs = posMs,
                passedFontSizeSp = 28.5f,
                activeFontSizeSp = 27.5f,
                upcomingFontSizeSp = 24.5f,
                activeAlpha = 1.0f,
                upcomingAlpha = 0.40f,
            )
            Text(
                text = annotated,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Black,
                fontSize = 27.5.sp,
                lineHeight = 35.sp,
                letterSpacing = (-0.5).sp,
                textAlign = textAlign,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            Text(
                text = line.text,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Black,
                fontSize = if (isPassed) 25.5.sp else 23.5.sp,
                lineHeight = if (isPassed) 32.sp else 30.sp,
                letterSpacing = (-0.45).sp,
                color = Color.White.copy(alpha = if (isPassed) 0.52f else 0.38f),
                textAlign = textAlign,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // Background Vocals Sub-line (with independent word sweep if word-synced!)
        val bg = line.background
        if (bg != null && bg.text.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            val displayBgText = if (bg.text.startsWith("(")) bg.text else "(${bg.text})"
            if (isSyncedMode && isCurrent && bg.isWordSynced) {
                val posMs = clockMsState.value
                val bgAnnotated = buildBitChordWordSweepString(
                    line = bg.copy(text = displayBgText),
                    positionMs = posMs,
                    passedFontSizeSp = 19.0f,
                    activeFontSizeSp = 18.5f,
                    upcomingFontSizeSp = 17.0f,
                    activeAlpha = 0.90f,
                    upcomingAlpha = 0.35f,
                )
                Text(
                    text = bgAnnotated,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.5.sp,
                    textAlign = textAlign,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                Text(
                    text = displayBgText,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = if (isCurrent) 18.sp else 16.5.sp,
                    color = when {
                        !isSyncedMode -> Color.White.copy(alpha = 0.78f)
                        isCurrent -> Color.White.copy(alpha = 0.82f)
                        else -> Color.White.copy(alpha = 0.30f)
                    },
                    textAlign = textAlign,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        // Translated Lyric Sub-line
        if (!translatedText.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = translatedText,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Bold,
                fontSize = if (isCurrent) 17.sp else 15.5.sp,
                lineHeight = 22.sp,
                color = when {
                    !isSyncedMode -> Color(0xFFFFD584).copy(alpha = 0.92f)
                    isCurrent -> Color(0xFFFFE09E)
                    else -> Color.White.copy(alpha = 0.36f)
                },
                textAlign = textAlign,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private fun buildBitChordWordSweepString(
    line: LyricLine,
    positionMs: Long,
    passedFontSizeSp: Float,
    activeFontSizeSp: Float,
    upcomingFontSizeSp: Float,
    activeAlpha: Float,
    upcomingAlpha: Float,
) = buildAnnotatedString {
    val text = line.text
    if (text.isEmpty()) return@buildAnnotatedString
    val len = text.length

    val revealed = if (line.isWordSynced) {
        line.revealedChars(positionMs).coerceIn(0f, len.toFloat())
    } else {
        val dur = (line.endMs(null) - line.timeMs).coerceAtLeast(350L).toFloat()
        val frac = ((positionMs - line.timeMs).toFloat() / dur).coerceIn(0f, 1f)
        frac * len
    }

    val fullPassedEnd = revealed.toInt().coerceIn(0, len)
    val featherSpan = 5
    val featherEnd = (revealed + featherSpan).toInt().coerceIn(fullPassedEnd, len)

    // Sample held-word growth & syllable lift so held vowels bloom larger & brighter
    val growths = line.growingAt(positionMs)
    val growthBoost = growths.maxOfOrNull { it.progress } ?: 0f
    val boostedPassedSp = passedFontSizeSp + (growthBoost * 1.6f)

    if (fullPassedEnd > 0) {
        withStyle(
            SpanStyle(
                color = Color.White.copy(alpha = activeAlpha),
                fontWeight = FontWeight.Black,
                fontSize = boostedPassedSp.sp,
                shadow = Shadow(
                    color = Color.White.copy(alpha = 0.34f + growthBoost * 0.25f),
                    offset = Offset.Zero,
                    blurRadius = 10f + growthBoost * 8f,
                ),
            )
        ) {
            append(text.substring(0, fullPassedEnd))
        }
    }

    if (featherEnd > fullPassedEnd) {
        val subFract = (revealed - fullPassedEnd).coerceIn(0f, 1f)
        for (charIdx in fullPassedEnd until featherEnd) {
            val dist = (charIdx - fullPassedEnd).toFloat() - subFract
            val charProgress = (1f - (dist / featherSpan.toFloat())).coerceIn(0f, 1f)
            val alpha = upcomingAlpha + (activeAlpha - upcomingAlpha) * charProgress
            val sizeSp = upcomingFontSizeSp + (activeFontSizeSp - upcomingFontSizeSp) * charProgress
            withStyle(
                SpanStyle(
                    color = Color.White.copy(alpha = alpha),
                    fontWeight = FontWeight.Black,
                    fontSize = sizeSp.sp,
                    shadow = Shadow(
                        color = Color.White.copy(alpha = 0.38f * charProgress),
                        offset = Offset.Zero,
                        blurRadius = 12f * charProgress,
                    ),
                )
            ) {
                append(text[charIdx])
            }
        }
    }

    if (featherEnd < len) {
        withStyle(
            SpanStyle(
                color = Color.White.copy(alpha = upcomingAlpha),
                fontWeight = FontWeight.ExtraBold,
                fontSize = upcomingFontSizeSp.sp,
            )
        ) {
            append(text.substring(featherEnd, len))
        }
    }
}

@Composable
private fun BitChordInstrumentalDots(
    startMs: Long,
    endMs: Long,
    positionMs: Long,
    alignment: LyricAlignment,
) {
    val span = (endMs - startMs).coerceAtLeast(800L).toFloat()
    val overallProgress = ((positionMs - startMs).toFloat() / span).coerceIn(0f, 1f)
    val infiniteTransition = rememberInfiniteTransition(label = "bitchord_dots_pulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 680, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "dots_scale",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = if (alignment == LyricAlignment.End) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Row(
            modifier = Modifier.graphicsLayer {
                scaleX = pulse
                scaleY = pulse
                transformOrigin = TransformOrigin(if (alignment == LyricAlignment.End) 1f else 0f, 0.5f)
            },
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            for (dotIdx in 0..2) {
                val dotStart = dotIdx / 3f
                val dotEnd = (dotIdx + 1) / 3f
                val dotFill = ((overallProgress - dotStart) / (dotEnd - dotStart)).coerceIn(0f, 1f)
                val dotAlpha = 0.28f + (0.72f * dotFill)
                val dotSizeDp = (9f + 3.5f * dotFill).dp
                Box(
                    modifier = Modifier
                        .size(dotSizeDp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = dotAlpha)),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsProviderSheet(
    activeSource: LyricsSource?,
    isEmbeddedLyrics: Boolean,
    providerStates: Map<LyricsSource, LyricsProviderState>,
    providerResults: Map<LyricsSource, List<LyricLine>>,
    orderedSources: List<LyricsSource>,
    enabledSources: Set<LyricsSource>,
    onSelectSource: (LyricsSource) -> Unit,
    onOpenSourcesDialog: () -> Unit,
    onOpenOffsetSheet: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(0xFF14151C),
        contentColor = TextPrimaryWhite,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Sumber Lirik BitChord",
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 19.sp,
                        color = Color.White,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isEmbeddedLyrics) {
                            "Menggunakan lirik offline yang tertanam (.lrc / tag)"
                        } else {
                            "Pilih manual atau atur prioritas otomatis dari 15+ provider"
                        },
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.5.sp,
                        color = TextSecondaryGrey,
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {
                        onDismiss()
                        onOpenOffsetSheet()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = AccentCoral,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Offset",
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = AccentCoral,
                        )
                    }
                    TextButton(onClick = {
                        onDismiss()
                        onOpenSourcesDialog()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = AccentCoral,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Prioritas",
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = AccentCoral,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
            Spacer(modifier = Modifier.height(10.dp))

            val displayedSources = remember(orderedSources, enabledSources) {
                val activeList = orderedSources.filter { it in enabledSources }
                val disabledList = orderedSources.filter { it !in enabledSources }
                activeList + disabledList
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                itemsIndexed(
                    items = displayedSources,
                    key = { _, src -> src.name },
                ) { _, source ->
                    val isSelected = activeSource == source
                    val isEnabled = source in enabledSources
                    val state = providerStates[source] ?: LyricsProviderState.NOT_FETCHED
                    val lines = providerResults[source]
                    val syncTypeLabel = when {
                        lines == null -> null
                        lines.any { l -> l.words.any { it.syllables.isNotEmpty() } } -> "Syllable TTML"
                        lines.any { it.isWordSynced } -> "Word-Sync"
                        lines.any { it.timeMs > 0L } -> "Synced LRC"
                        lines.isNotEmpty() -> "Plain"
                        else -> null
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (isSelected) AccentCoral.copy(alpha = 0.16f)
                                else Color.White.copy(alpha = 0.04f)
                            )
                            .border(
                                width = if (isSelected) 1.2.dp else 0.8.dp,
                                color = if (isSelected) AccentCoral else Color.White.copy(alpha = 0.07f),
                                shape = RoundedCornerShape(14.dp),
                            )
                            .clickable {
                                onSelectSource(source)
                                onDismiss()
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                            .testTag("lyrics_provider_option_${source.name.lowercase()}"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = source.displayName,
                                    fontFamily = PlusJakartaSans,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    color = if (isEnabled) Color.White else Color.White.copy(alpha = 0.55f),
                                )
                                if (syncTypeLabel != null) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .background(FoundGreen.copy(alpha = 0.18f))
                                            .padding(horizontal = 8.dp, vertical = 2.dp),
                                    ) {
                                        Text(
                                            text = syncTypeLabel,
                                            fontFamily = PlusJakartaSans,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            color = FoundGreen,
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = source.subtitle,
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = TextSecondaryGrey,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        when {
                            state == LyricsProviderState.FETCHING -> {
                                CircularProgressIndicator(
                                    color = AccentCoral,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                            isSelected -> {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Aktif",
                                    tint = AccentCoral,
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                            state == LyricsProviderState.FOUND -> {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Tersedia",
                                    tint = FoundGreen,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            state == LyricsProviderState.NOT_FOUND -> {
                                Text(
                                    text = "Kosong",
                                    fontFamily = PlusJakartaSans,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.38f),
                                )
                            }
                            else -> {
                                Text(
                                    text = "Ketuk",
                                    fontFamily = PlusJakartaSans,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.52f),
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
fun LyricsSourcesDialog(
    settings: LyricsSettingsState,
    onToggleSource: (LyricsSource) -> Unit,
    onMoveSource: (Int, Int) -> Unit,
    onTogglePrioritizeSyllable: (Boolean) -> Unit,
    onSavePaxSenixApiKey: (String) -> Unit,
    onResetDefault: () -> Unit,
    onDismiss: () -> Unit,
) {
    var apiKeyDraft by remember(settings.paxSenixApiKey) { mutableStateOf(settings.paxSenixApiKey) }
    var showApiKeyField by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF15161E))
                .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(24.dp))
                .padding(20.dp)
                .testTag("lyrics_sources_dialog"),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Urutan & Sumber Lirik",
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 19.sp,
                        color = Color.White,
                    )
                    Text(
                        text = "Atur prioritas fallback & aktifkan provider lirik",
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.5.sp,
                        color = TextSecondaryGrey,
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Tutup",
                        tint = Color.White,
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Prioritize Syllable Sync Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Utamakan Lirik Per Suku Kata (Syllable Sync)",
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        color = Color.White,
                    )
                    Text(
                        text = "Pilih provider dengan lirik per kata/suku kata terlebih dahulu",
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.5.sp,
                        color = TextSecondaryGrey,
                    )
                }
                Switch(
                    checked = settings.prioritizeSyllableSync,
                    onCheckedChange = onTogglePrioritizeSyllable,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = AccentCoral,
                    ),
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // PaxSenix API Key Expandable Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showApiKeyField = !showApiKeyField },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            tint = AccentCoral,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "PaxSenix API Key (Spotify & Musixmatch)",
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color.White,
                            )
                            Text(
                                text = if (settings.paxSenixApiKey.isNotBlank()) "API Key tersimpan" else "Opsional untuk PaxSenix Spotify/Musixmatch",
                                fontFamily = PlusJakartaSans,
                                fontSize = 11.sp,
                                color = if (settings.paxSenixApiKey.isNotBlank()) FoundGreen else TextSecondaryGrey,
                            )
                        }
                    }
                    Text(
                        text = if (showApiKeyField) "Sembunyikan" else "Atur",
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = AccentCoral,
                    )
                }

                AnimatedVisibility(
                    visible = showApiKeyField,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    Column(modifier = Modifier.padding(top = 10.dp)) {
                        OutlinedTextField(
                            value = apiKeyDraft,
                            onValueChange = { apiKeyDraft = it },
                            placeholder = {
                                Text(
                                    text = "Tempel Bearer / API Key PaxSenix...",
                                    fontSize = 12.5.sp,
                                    color = TextSecondaryGrey,
                                )
                            },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = AccentCoral,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            Button(
                                onClick = {
                                    onSavePaxSenixApiKey(apiKeyDraft)
                                    showApiKeyField = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentCoral),
                                shape = RoundedCornerShape(10.dp),
                            ) {
                                Text("Simpan Key", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Ordered Sources List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 310.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                itemsIndexed(
                    items = settings.sourceOrder,
                    key = { _, src -> src.name },
                ) { index, source ->
                    val isEnabled = source in settings.enabledSources
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = if (isEnabled) 0.06f else 0.02f))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = isEnabled,
                            onCheckedChange = { onToggleSource(source) },
                            colors = CheckboxDefaults.colors(
                                checkedColor = AccentCoral,
                                uncheckedColor = Color.White.copy(alpha = 0.4f),
                            ),
                        )
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 6.dp),
                        ) {
                            Text(
                                text = "${index + 1}. ${source.displayName}",
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (isEnabled) Color.White else Color.White.copy(alpha = 0.45f),
                            )
                            Text(
                                text = source.subtitle,
                                fontFamily = PlusJakartaSans,
                                fontSize = 11.sp,
                                color = TextSecondaryGrey,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        IconButton(
                            onClick = { if (index > 0) onMoveSource(index, index - 1) },
                            enabled = index > 0,
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = "Naikkan",
                                tint = if (index > 0) Color.White else Color.White.copy(alpha = 0.2f),
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        IconButton(
                            onClick = {
                                if (index < settings.sourceOrder.lastIndex) {
                                    onMoveSource(index, index + 1)
                                }
                            },
                            enabled = index < settings.sourceOrder.lastIndex,
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = "Turunkan",
                                tint = if (index < settings.sourceOrder.lastIndex) Color.White else Color.White.copy(alpha = 0.2f),
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onResetDefault) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = TextSecondaryGrey,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Reset Default",
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondaryGrey,
                    )
                }

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentCoral),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(
                        text = "Selesai",
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsOffsetSheet(
    currentOffsetMs: Int,
    onChangeOffsetMs: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val snappedOffset = ((currentOffsetMs / 100f).roundToInt() * 100).coerceIn(-5000, 5000)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(0xFF14151C),
        contentColor = Color.White,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 28.dp)
                .testTag("lyrics_offset_sheet"),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Pengaturan Offset Waktu Lirik",
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.5.sp,
                color = Color.White,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Sesuaikan ketepatan waktu lirik terhadap ketukan lagu (-5.0s s/d +5.0s)",
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.5.sp,
                color = TextSecondaryGrey,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(18.dp))

            val sec = snappedOffset / 1000f
            Text(
                text = if (snappedOffset == 0) "0.0s (Tepat Waktu)" else "%+.1fs (%+d ms)".format(sec, snappedOffset),
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Black,
                fontSize = 24.sp,
                color = if (snappedOffset == 0) Color.White else AccentCoral,
            )

            Spacer(modifier = Modifier.height(14.dp))

            Slider(
                value = snappedOffset.toFloat(),
                onValueChange = { raw ->
                    val stepped = ((raw / 100f).roundToInt() * 100).coerceIn(-5000, 5000)
                    onChangeOffsetMs(stepped)
                },
                valueRange = -5000f..5000f,
                steps = 99,
                colors = SliderDefaults.colors(
                    thumbColor = AccentCoral,
                    activeTrackColor = AccentCoral,
                    inactiveTrackColor = Color.White.copy(alpha = 0.2f),
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OffsetStepButton(
                    label = "-0.5s",
                    onClick = { onChangeOffsetMs((snappedOffset - 500).coerceIn(-5000, 5000)) },
                    modifier = Modifier.weight(1f),
                )
                OffsetStepButton(
                    label = "-0.1s",
                    onClick = { onChangeOffsetMs((snappedOffset - 100).coerceIn(-5000, 5000)) },
                    modifier = Modifier.weight(1f),
                )
                OffsetStepButton(
                    label = "Reset",
                    isPrimary = snappedOffset != 0,
                    onClick = { onChangeOffsetMs(0) },
                    modifier = Modifier.weight(1f),
                )
                OffsetStepButton(
                    label = "+0.1s",
                    onClick = { onChangeOffsetMs((snappedOffset + 100).coerceIn(-5000, 5000)) },
                    modifier = Modifier.weight(1f),
                )
                OffsetStepButton(
                    label = "+0.5s",
                    onClick = { onChangeOffsetMs((snappedOffset + 500).coerceIn(-5000, 5000)) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun OffsetStepButton(
    label: String,
    isPrimary: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(42.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isPrimary) AccentCoral else Color.White.copy(alpha = 0.10f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = Color.White,
        )
    }
}

@Composable
fun TranslationLanguageDialog(
    selectedCode: String,
    onSelectLanguage: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xFF161720))
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(22.dp))
                .padding(20.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = null,
                        tint = AccentCoral,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Bahasa Terjemahan Lirik",
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 17.sp,
                        color = Color.White,
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Tutup",
                        tint = Color.White,
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                itemsIndexed(TRANSLATION_LANGUAGES) { _, lang ->
                    val isSelected = lang.code.equals(selectedCode, ignoreCase = true)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) AccentCoral.copy(alpha = 0.18f) else Color.Transparent)
                            .clickable { onSelectLanguage(lang.code) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = lang.name,
                            fontFamily = PlusJakartaSans,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = Color.White,
                        )
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = AccentCoral,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
