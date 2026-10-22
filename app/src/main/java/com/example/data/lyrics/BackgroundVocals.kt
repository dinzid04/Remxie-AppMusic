package com.example.data.lyrics

private val TRAILING_BG_REGEX = Regex("""^(.*\S)\s+\(([^()]{2,})\)\s*$""")

internal fun List<LyricLine>.withBackgroundVocals(): List<LyricLine> {
    if (isEmpty()) return this
    return map { line ->
        if (line.background != null || line.isGap) return@map line
        val match = TRAILING_BG_REGEX.matchEntire(line.text) ?: return@map line
        val mainText = match.groupValues[1].trim()
        val bgInner = match.groupValues[2].trim()
        if (mainText.isEmpty() || bgInner.isEmpty()) return@map line

        if (line.words.isEmpty()) {
            return@map line.copy(
                text = mainText,
                background = LyricLine(
                    timeMs = line.timeMs,
                    text = "($bgInner)",
                    sungUntilMs = line.sungUntilMs,
                    alignment = line.alignment,
                )
            )
        }

        val openParenWordIdx = line.words.indexOfLast { it.text.startsWith('(') }
        if (openParenWordIdx <= 0) {
            return@map line
        }
        val mainWords = line.words.subList(0, openParenWordIdx)
        val bgWordsRaw = line.words.subList(openParenWordIdx, line.words.size)
        val bgWords = bgWordsRaw.mapNotNull { w ->
            w.withoutChars { it == '(' || it == ')' }
        }
        val bgStart = bgWords.firstOrNull()?.startMs ?: line.timeMs
        val bgEnd = bgWords.lastOrNull()?.endMs ?: line.sungUntilMs
        val cleanMainText = mainWords.joinToString(" ") { it.text }.ifBlank { mainText }
        val cleanBgText = bgWords.joinToString(" ") { it.text }.ifBlank { bgInner }

        line.copy(
            text = cleanMainText,
            words = mainWords,
            sungUntilMs = mainWords.lastOrNull()?.endMs ?: line.sungUntilMs,
            background = LyricLine(
                timeMs = bgStart,
                text = cleanBgText,
                words = bgWords,
                sungUntilMs = bgEnd,
                alignment = line.alignment,
            )
        )
    }
}
