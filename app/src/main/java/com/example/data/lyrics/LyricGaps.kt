package com.example.data.lyrics

internal const val MIN_GAP_MS = 4_000L
private const val INTRO_GAP_MIN_MS = 4_000L

fun List<LyricLine>.withInstrumentalGaps(): List<LyricLine> {
    if (isEmpty()) return this
    if (any { it.isGap }) return this
    val out = ArrayList<LyricLine>(size + 4)
    val first = first()
    if (first.timeMs >= INTRO_GAP_MIN_MS) {
        out.add(
            LyricLine(
                timeMs = 0L,
                text = "",
                sungUntilMs = first.timeMs,
            )
        )
    }
    for (index in indices) {
        val current = this[index]
        out.add(current)
        val next = getOrNull(index + 1) ?: continue
        val currentEnd = current.sungUntilMs ?: current.words.lastOrNull()?.endMs ?: continue
        val silence = next.timeMs - currentEnd
        if (silence >= MIN_GAP_MS) {
            out.add(
                LyricLine(
                    timeMs = currentEnd,
                    text = "",
                    sungUntilMs = next.timeMs,
                )
            )
        }
    }
    return out
}
