package com.example.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import com.example.data.lyrics.LyricLine
import com.example.player.PlayerUiState
import kotlin.math.abs

@Composable
fun rememberBitChordLyricClockMs(
    playerState: PlayerUiState,
    offsetMs: Int = 0,
): State<Long> {
    val initialMs = ((playerState.exactPositionSec.takeIf { it > 0.0 }
        ?: playerState.currentPositionSec.toDouble()) * 1000.0).toLong() + offsetMs
    val clockState = remember { mutableLongStateOf(initialMs.coerceAtLeast(0L)) }

    LaunchedEffect(
        playerState.exactPositionSec,
        playerState.currentPositionMs,
        playerState.currentTrack?.id,
        offsetMs,
    ) {
        val exactMs = ((playerState.exactPositionSec.takeIf { it > 0.0 }
            ?: (playerState.currentPositionMs / 1000.0)) * 1000.0).toLong() +
            offsetMs +
            if (playerState.isPlaying) 40L else 0L
        val target = exactMs.coerceAtLeast(0L)
        val drift = abs(clockState.longValue - target)
        if (drift > 150L || playerState.currentPositionMs <= 120L) {
            clockState.longValue = target
        } else if (target > clockState.longValue) {
            clockState.longValue = clockState.longValue + ((target - clockState.longValue) * 3L) / 4L
        }
    }

    LaunchedEffect(playerState.isPlaying, playerState.currentTrack?.id, offsetMs) {
        if (!playerState.isPlaying) {
            val pausedMs = ((playerState.exactPositionSec.takeIf { it > 0.0 }
                ?: playerState.currentPositionSec.toDouble()) * 1000.0).toLong() + offsetMs
            clockState.longValue = pausedMs.coerceAtLeast(0L)
            return@LaunchedEffect
        }
        var lastNanos = 0L
        while (true) {
            withFrameNanos { now ->
                if (lastNanos != 0L) {
                    val deltaMs = ((now - lastNanos) / 1_000_000L).coerceIn(0L, 50L)
                    val maxDurMs = (playerState.durationSec.coerceAtLeast(10) * 1000L) + 5_000L
                    clockState.longValue = (clockState.longValue + deltaMs).coerceIn(0L, maxDurMs)
                }
                lastNanos = now
            }
        }
    }

    return clockState
}

fun activeLyricRows(
    lines: List<LyricLine>,
    positionMs: Long,
): List<Int> {
    if (lines.isEmpty()) return emptyList()
    val leadAdjusted = positionMs + 80L
    val active = ArrayList<Int>(2)
    for (index in lines.indices) {
        val line = lines[index]
        if (leadAdjusted < line.timeMs) continue
        val nextMs = lines.getOrNull(index + 1)?.timeMs
        val endMs = line.endMs(nextMs)
        if (leadAdjusted <= endMs || (nextMs != null && leadAdjusted < nextMs)) {
            active.add(index)
        }
    }
    if (active.isNotEmpty()) return active
    val fallback = lines.indexOfLast { leadAdjusted >= it.timeMs }
    return if (fallback >= 0) listOf(fallback) else emptyList()
}
