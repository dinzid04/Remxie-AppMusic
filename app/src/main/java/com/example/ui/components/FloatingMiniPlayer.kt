package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.player.PlayerUiState
import com.example.ui.theme.CardPlaceholderDark
import com.example.ui.theme.PillNavBackground
import com.example.ui.theme.PillNavBorder
import com.example.ui.theme.PlusJakartaSans
import com.example.ui.theme.TextPrimaryWhite
import com.example.ui.theme.TextSecondaryGrey

/**
 * Floating Pill Mini-Player positioned above the bottom navigation bar.
 * - Clicking or dragging upwards expands into the Full Music Player with a corner-to-center morphing animation.
 */
@Composable
fun FloatingMiniPlayer(
    playerState: PlayerUiState,
    customCoverUrl: String? = null,
    onExpandPlayer: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNextTrack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val track = playerState.currentTrack
    var upwardDragAccum by remember { mutableFloatStateOf(0f) }

    AnimatedVisibility(
        visible = track != null,
        enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut(),
        modifier = modifier
    ) {
        if (track != null) {
            val context = LocalContext.current
            val effectiveThumb = customCoverUrl?.takeIf { it.isNotBlank() } ?: track.smallThumbnail
            val thumbRequest = remember(context, effectiveThumb) {
                ImageRequest.Builder(context)
                    .data(effectiveThumb)
                    .size(132, 132)
                    .allowHardware(true)
                    .crossfade(false)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .build()
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier
                        .widthIn(min = 260.dp, max = 356.dp)
                        .fillMaxWidth()
                        .height(64.dp)
                        .shadow(
                            elevation = 22.dp,
                            shape = RoundedCornerShape(50),
                            ambientColor = Color.Black,
                            spotColor = Color.Black
                        )
                        .clip(RoundedCornerShape(50))
                        .background(PillNavBackground.copy(alpha = 0.96f))
                        .border(
                            width = 1.dp,
                            color = PillNavBorder.copy(alpha = 0.9f),
                            shape = RoundedCornerShape(50)
                        )
                        .pointerInput(track.id) {
                            detectVerticalDragGestures(
                                onDragStart = { upwardDragAccum = 0f },
                                onVerticalDrag = { change, dragAmount ->
                                    if (dragAmount < 0f) {
                                        change.consume()
                                        upwardDragAccum += -dragAmount
                                        if (upwardDragAccum > 36f) {
                                            upwardDragAccum = 0f
                                            onExpandPlayer()
                                        }
                                    }
                                }
                            )
                        }
                        .clickable(onClick = onExpandPlayer)
                        .padding(start = 10.dp, end = 8.dp)
                        .testTag("floating_mini_player"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = thumbRequest,
                        contentDescription = track.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(CardPlaceholderDark)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = track.title,
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            letterSpacing = (-0.35).sp,
                            color = TextPrimaryWhite,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(1.dp))
                        Text(
                            text = track.cleanArtist,
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            letterSpacing = (-0.15).sp,
                            color = TextSecondaryGrey,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    if (playerState.isLoading) {
                        Box(
                            modifier = Modifier.size(48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = TextPrimaryWhite,
                                strokeWidth = 2.2.dp,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    } else {
                        IconButton(
                            onClick = onTogglePlayPause,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("mini_player_play_pause")
                        ) {
                            Icon(
                                imageVector = if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (playerState.isPlaying) "Pause" else "Play",
                                tint = TextPrimaryWhite,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onNextTrack,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("mini_player_next")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FastForward,
                            contentDescription = "Lagu Berikutnya",
                            tint = TextPrimaryWhite,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }
}
