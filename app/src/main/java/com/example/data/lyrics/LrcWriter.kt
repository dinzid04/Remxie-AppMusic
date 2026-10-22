package com.example.data.lyrics

const val WORD_LYRICS_FIELD = "BITCHORD_LYRICS"

fun List<LyricLine>.toLrc(): String {
    val lines = filter { !it.isGap && it.text.isNotBlank() }
    if (lines.isEmpty()) return ""
    val hasTimestamps = lines.count { it.timeMs > 0L } >= 1 && lines.size > 1
    if (!hasTimestamps && lines.all { it.timeMs == 0L }) {
        return lines.joinToString("\n") { it.text }
    }
    return lines.joinToString("\n") { line ->
        "${formatLrcBracketTimestamp(line.timeMs)}${line.text}"
    }
}

fun List<LyricLine>.toEnhancedLrc(): String {
    val lines = filter { !it.isGap && it.text.isNotBlank() }
    if (lines.isEmpty()) return ""
    return lines.joinToString("\n") { line ->
        val prefix = formatLrcBracketTimestamp(line.timeMs)
        val alignTag = if (line.alignment == LyricAlignment.End) "<R> " else ""
        val mainBody = formatLineWords(line)
        val bgSuffix = line.background?.takeIf { it.text.isNotBlank() }?.let { bg ->
            " [bg: ${formatLineWords(bg)}]"
        }.orEmpty()
        "$prefix$alignTag$mainBody$bgSuffix"
    }
}

private fun formatLineWords(line: LyricLine): String {
    if (line.words.isEmpty()) return line.text
    return buildString {
        for ((wIdx, word) in line.words.withIndex()) {
            if (wIdx > 0) append(' ')
            if (word.syllables.size > 1) {
                for (syl in word.syllables) {
                    val piece = word.text.substring(
                        syl.startChar.coerceIn(0, word.text.length),
                        syl.endChar.coerceIn(0, word.text.length),
                    )
                    append(formatLrcAngleTimestamp(syl.startMs))
                    append(piece)
                }
            } else {
                append(formatLrcAngleTimestamp(word.startMs))
                append(word.text)
            }
        }
        val endMs = line.sungUntilMs ?: line.words.lastOrNull()?.endMs
        if (endMs != null) {
            append(formatLrcAngleTimestamp(endMs))
        }
    }
}

private fun formatLrcBracketTimestamp(ms: Long): String {
    val clamped = ms.coerceAtLeast(0L)
    val min = clamped / 60_000L
    val sec = (clamped % 60_000L) / 1_000L
    val cs = (clamped % 1_000L) / 10L
    return "[%02d:%02d.%02d]".format(min, sec, cs)
}

private fun formatLrcAngleTimestamp(ms: Long): String {
    val clamped = ms.coerceAtLeast(0L)
    val min = clamped / 60_000L
    val sec = (clamped % 60_000L) / 1_000L
    val cs = (clamped % 1_000L) / 10L
    return "<%02d:%02d.%02d>".format(min, sec, cs)
}
