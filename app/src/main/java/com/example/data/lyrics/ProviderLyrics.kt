package com.example.data.lyrics

import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener

object ProviderLyrics {
    fun parse(raw: String): List<LyricLine>? {
        val text = unwrap(raw)?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        return when {
            text.startsWith("<") && (text.contains("<tt") || text.contains("<p")) ->
                TtmlLyrics.parse(text).takeIf { it.isNotEmpty() }
            KaraokeLrc.looksLike(text) ->
                KaraokeLrc.parse(text).takeIf { it.isNotEmpty() }
            text.contains(Regex("""<\d+:\d{1,2}(?:[.,]\d{1,3})?>""")) ->
                EnhancedLrc.parse(text).takeIf { it.isNotEmpty() }
            text.contains(Regex("""\[\d+:\d{1,2}""")) ->
                LrcLib.parseLrc(text).takeIf { it.isNotEmpty() }
            else ->
                Genius.textToLyricLines(text).takeIf { it.isNotEmpty() }
        }
    }

    fun unwrap(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        if (!trimmed.startsWith("{") && !trimmed.startsWith("[") && !trimmed.startsWith("\"")) {
            return trimmed
        }
        return try {
            when (val token = JSONTokener(trimmed).nextValue()) {
                is String -> token.takeIf { it.isNotBlank() }
                is JSONObject -> {
                    for (key in listOf("ttml", "lyrics", "syncedLyrics", "lrc", "subtitles", "plainLyrics", "data", "content", "text")) {
                        val v = token.opt(key)
                        when (v) {
                            is String -> if (v.isNotBlank()) return unwrap(v) ?: v
                            is JSONObject -> {
                                val nested = unwrap(v.toString())
                                if (!nested.isNullOrBlank()) return nested
                            }
                        }
                    }
                    null
                }
                is JSONArray -> {
                    if (token.length() == 0) null
                    else {
                        val lines = ArrayList<String>(token.length())
                        for (i in 0 until token.length()) {
                            when (val item = token.opt(i)) {
                                is String -> lines.add(item)
                                is JSONObject -> {
                                    val t = item.optString("text", item.optString("words", ""))
                                    val ms = item.optLong("startTimeMs", item.optLong("timeMs", item.optLong("time", -1L)))
                                    if (t.isNotBlank()) {
                                        if (ms >= 0L) {
                                            val min = ms / 60_000L
                                            val sec = (ms % 60_000L) / 1_000L
                                            val cs = (ms % 1_000L) / 10L
                                            lines.add("[%02d:%02d.%02d]%s".format(min, sec, cs, t))
                                        } else {
                                            lines.add(t)
                                        }
                                    }
                                }
                            }
                        }
                        lines.joinToString("\n").takeIf { it.isNotBlank() }
                    }
                }
                else -> trimmed
            }
        } catch (_: Exception) {
            trimmed
        }
    }
}

internal object KaraokeLrc {
    private val MULTI_TAG_LINE = Regex("""^((?:\[\d+:\d{1,2}(?:[.,]\d{1,3})?][^\[<\r\n]*){2,})$""")
    private val PIECE_REGEX = Regex("""\[(\d+):(\d{1,2})(?:[.,](\d{1,3}))?]([^\[]*)""")

    fun looksLike(text: String): Boolean {
        return text.lineSequence().any { line ->
            val t = line.trim()
            t.count { it == '[' } >= 2 && MULTI_TAG_LINE.matches(t)
        }
    }

    fun parse(text: String): List<LyricLine> {
        val converted = text.lineSequence().map { rawLine ->
            val trimmed = rawLine.trim()
            val matches = PIECE_REGEX.findAll(trimmed).toList()
            if (matches.size >= 2 && matches.any { it.groupValues[4].isNotEmpty() }) {
                val first = matches.first()
                val prefix = "[${first.groupValues[1]}:${first.groupValues[2]}${
                    if (first.groupValues[3].isNotEmpty()) ".${first.groupValues[3]}" else ""
                }]"
                val body = buildString {
                    for (m in matches) {
                        val stamp = "<${m.groupValues[1]}:${m.groupValues[2]}${
                            if (m.groupValues[3].isNotEmpty()) ".${m.groupValues[3]}" else ""
                        }>"
                        append(stamp)
                        append(m.groupValues[4])
                    }
                }
                "$prefix $body"
            } else {
                rawLine
            }
        }.joinToString("\n")
        return EnhancedLrc.parse(converted)
    }
}
