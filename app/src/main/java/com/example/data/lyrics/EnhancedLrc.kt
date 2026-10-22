package com.example.data.lyrics

object EnhancedLrc {
    private val LINE_TAG_REGEX = Regex("""^\[(\d+):(\d{1,2})(?:[.,](\d{1,3}))?]\s*(.*)$""")
    private val WORD_TAG_REGEX = Regex("""<(\d+):(\d{1,2})(?:[.,](\d{1,3}))?>([^<]*)""")
    private val END_TAG_REGEX = Regex("""<(\d+):(\d{1,2})(?:[.,](\d{1,3}))?>\s*$""")
    private val SINGER_PREFIX_REGEX = Regex("""^(?:v(\d+)|\[v(\d+)]|([A-Za-z0-9_]+)):\s*(.*)$""")
    private val RIGHT_ALIGN_PREFIX_REGEX = Regex("""^<R>\s*""", RegexOption.IGNORE_CASE)
    private val BG_INLINE_REGEX = Regex("""\[bg:\s*(.*?)\s*]\s*$""", RegexOption.IGNORE_CASE)

    fun parse(lrc: String): List<LyricLine> {
        val rawLines = lrc.lines()
        val parsed = ArrayList<RawParsedLine>()
        val singers = ArrayList<String?>()

        for (rawLine in rawLines) {
            val trimmed = rawLine.trim()
            if (trimmed.isEmpty()) continue
            val match = LINE_TAG_REGEX.matchEntire(trimmed) ?: continue
            val lineMs = toMillis(
                match.groupValues[1],
                match.groupValues[2],
                match.groupValues[3],
            )
            var body = decodeEntities(match.groupValues[4].trim())
            if (body.isEmpty()) continue

            var explicitRightAlign = false
            if (RIGHT_ALIGN_PREFIX_REGEX.containsMatchIn(body)) {
                explicitRightAlign = true
                body = body.replaceFirst(RIGHT_ALIGN_PREFIX_REGEX, "").trim()
            }

            var bgRaw: String? = null
            val bgMatch = BG_INLINE_REGEX.find(body)
            if (bgMatch != null) {
                bgRaw = bgMatch.groupValues[1].trim()
                body = body.removeRange(bgMatch.range).trim()
            }

            var singerId: String? = if (explicitRightAlign) "v2" else null
            val singerMatch = SINGER_PREFIX_REGEX.matchEntire(body)
            if (singerMatch != null) {
                val id = singerMatch.groupValues[1]
                    .ifEmpty { singerMatch.groupValues[2] }
                    .ifEmpty { singerMatch.groupValues[3] }
                if (id.lowercase() in setOf("v1", "v2", "v3", "m", "f", "d", "duet", "male", "female")) {
                    singerId = id.lowercase()
                    body = singerMatch.groupValues[4].trim()
                }
            }

            val (text, words, sungUntilMs) = parseWordBody(body, lineMs)
            if (text.isEmpty() || isGeniusSectionHeader(text)) continue

            val bgLine = bgRaw?.takeIf { it.isNotEmpty() }?.let { bgStr ->
                val (bgText, bgWords, bgEnd) = parseWordBody(bgStr, lineMs)
                if (bgText.isNotEmpty()) {
                    LyricLine(
                        timeMs = bgWords.firstOrNull()?.startMs ?: lineMs,
                        text = bgText,
                        words = bgWords,
                        sungUntilMs = bgEnd ?: bgWords.lastOrNull()?.endMs ?: sungUntilMs,
                    )
                } else null
            }

            singers.add(singerId)
            parsed.add(
                RawParsedLine(
                    timeMs = words.firstOrNull()?.startMs ?: lineMs,
                    text = text,
                    words = words,
                    sungUntilMs = sungUntilMs,
                    background = bgLine,
                    explicitRightAlign = explicitRightAlign,
                )
            )
        }

        if (parsed.isEmpty()) return emptyList()
        val alignments = lineAlignments(singers)
        return parsed.mapIndexed { idx, item ->
            LyricLine(
                timeMs = item.timeMs,
                text = item.text,
                words = item.words,
                sungUntilMs = item.sungUntilMs,
                background = item.background,
                alignment = if (item.explicitRightAlign) LyricAlignment.End else alignments.getOrElse(idx) { LyricAlignment.Start },
            )
        }
            .sortedBy { it.timeMs }
            .withBackgroundVocals()
            .withInstrumentalGaps()
    }

    private fun parseWordBody(body: String, defaultStartMs: Long): Triple<String, List<LyricWord>, Long?> {
        val wordMatches = WORD_TAG_REGEX.findAll(body).toList()
        if (wordMatches.isEmpty()) {
            val clean = body.replace(Regex("""<\d+:\d{1,2}(?:[.,]\d{1,3})?>"""), "").trim()
            return Triple(clean, emptyList(), null)
        }

        val trailingEnd = END_TAG_REGEX.find(body)?.let {
            toMillis(it.groupValues[1], it.groupValues[2], it.groupValues[3])
        }

        val runs = ArrayList<TimedRun>(wordMatches.size)
        for (i in wordMatches.indices) {
            val current = wordMatches[i]
            val startMs = toMillis(current.groupValues[1], current.groupValues[2], current.groupValues[3])
            val rawPiece = current.groupValues[4]
            if (rawPiece.isEmpty()) continue
            val nextStart = wordMatches.getOrNull(i + 1)?.let {
                toMillis(it.groupValues[1], it.groupValues[2], it.groupValues[3])
            } ?: trailingEnd ?: (startMs + 420L)
            val endMs = nextStart.coerceAtLeast(startMs + 40L)
            runs.add(TimedRun(startMs = startMs, endMs = endMs, raw = rawPiece))
        }

        val words = wordsFromRuns(runs, spacingIsExplicit = true)
        val joinedText = if (words.isNotEmpty()) {
            words.joinToString(" ") { it.text }
        } else {
            body.replace(Regex("""<\d+:\d{1,2}(?:[.,]\d{1,3})?>"""), "").trim()
        }
        val sungUntil = trailingEnd ?: words.lastOrNull()?.endMs
        return Triple(joinedText, words, sungUntil)
    }

    private fun toMillis(minStr: String, secStr: String, fracStr: String): Long {
        val min = minStr.toLongOrNull() ?: 0L
        val sec = secStr.toLongOrNull() ?: 0L
        val ms = when (fracStr.length) {
            0 -> 0L
            1 -> (fracStr.toLongOrNull() ?: 0L) * 100L
            2 -> (fracStr.toLongOrNull() ?: 0L) * 10L
            else -> fracStr.take(3).toLongOrNull() ?: 0L
        }
        return (min * 60_000L) + (sec * 1_000L) + ms
    }

    fun decodeEntities(text: String): String {
        if (!text.contains('&')) return text
        return text
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&nbsp;", " ")
    }

    private data class RawParsedLine(
        val timeMs: Long,
        val text: String,
        val words: List<LyricWord>,
        val sungUntilMs: Long?,
        val background: LyricLine?,
        val explicitRightAlign: Boolean,
    )
}
