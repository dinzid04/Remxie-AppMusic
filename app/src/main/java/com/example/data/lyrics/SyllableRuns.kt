package com.example.data.lyrics

internal data class TimedRun(
    val startMs: Long,
    val endMs: Long,
    val raw: String,
)

internal fun wordsFromRuns(
    runs: List<TimedRun>,
    spacingIsExplicit: Boolean = false,
): List<LyricWord> {
    if (runs.isEmpty()) return emptyList()
    val explicitSpaces = spacingIsExplicit || runs.any { it.raw.startsWith(' ') || it.raw.endsWith(' ') }

    val grouped = ArrayList<MutableList<TimedRun>>()
    for (run in runs) {
        val startsNew = grouped.isEmpty() || !explicitSpaces || run.raw.startsWith(' ')
        val trimmed = run.raw.trim()
        if (trimmed.isEmpty()) continue
        val pieces = splitOnInternalWhitespace(trimmed, run.startMs, run.endMs)
        for ((index, piece) in pieces.withIndex()) {
            if (index == 0 && !startsNew && grouped.isNotEmpty()) {
                grouped.last().add(piece)
            } else {
                grouped.add(mutableListOf(piece))
            }
        }
        if (explicitSpaces && run.raw.endsWith(' ') && grouped.isNotEmpty()) {
            grouped.add(mutableListOf())
        }
    }

    return grouped.mapNotNull { pieces ->
        if (pieces.isEmpty()) return@mapNotNull null
        val text = buildString { pieces.forEach { append(it.raw) } }
        if (text.isEmpty()) return@mapNotNull null
        val startMs = pieces.first().startMs
        val endMs = pieces.last().endMs.coerceAtLeast(startMs)
        val syllables = if (pieces.size > 1) {
            var cursor = 0
            val built = pieces.map { p ->
                val s = cursor
                cursor += p.raw.length
                LyricSyllable(
                    startMs = p.startMs,
                    endMs = p.endMs.coerceAtLeast(p.startMs),
                    startChar = s,
                    endChar = cursor,
                )
            }
            LyricSyllable.normalized(built, text.length)
        } else {
            emptyList()
        }
        LyricWord(
            startMs = startMs,
            endMs = endMs,
            text = text,
            syllables = syllables,
        )
    }
}

private fun splitOnInternalWhitespace(
    text: String,
    startMs: Long,
    endMs: Long,
): List<TimedPiece> {
    val parts = text.split(Regex("\\s+")).filter { it.isNotEmpty() }
    if (parts.size <= 1) return listOf(TimedPiece(startMs, endMs, text))
    val totalChars = parts.sumOf { it.length }.coerceAtLeast(1)
    val duration = (endMs - startMs).coerceAtLeast(0L)
    var cursorChars = 0
    return parts.mapIndexed { idx, part ->
        val pStart = startMs + (duration * cursorChars) / totalChars
        cursorChars += part.length
        val pEnd = if (idx == parts.lastIndex) endMs else startMs + (duration * cursorChars) / totalChars
        TimedPiece(pStart, pEnd.coerceAtLeast(pStart), part)
    }
}

private typealias TimedPiece = TimedRun
