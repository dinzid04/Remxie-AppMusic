package com.example.data.lyrics

import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import kotlin.math.abs

object LrcLib {
    private const val BASE_URL = "https://lrclib.net/api"
    private val LINE_TIMESTAMP_REGEX = Regex("""\[(\d+):(\d{1,2})(?:[.,](\d{1,3}))?]""")
    private val WORD_TAG_CHECK_REGEX = Regex("""<\d+:\d{1,2}(?:[.,]\d{1,3})?>""")
    private val RIGHT_MARKER_REGEX = Regex("""^<R>\s*""", RegexOption.IGNORE_CASE)

    suspend fun lyrics(
        title: String,
        artist: String,
        album: String? = null,
        durationMs: Long = 0L,
    ): List<LyricLine>? {
        val cleanTitle = title.forLyricsSearch()
        val cleanArtist = artist.artistForLyricsSearch()
        if (cleanTitle.isEmpty()) return null

        // 1. Exact /api/get when artist and duration are known
        if (cleanArtist.isNotEmpty() && durationMs > 0L) {
            val getParams = buildString {
                append("track_name=").append(URLEncoder.encode(cleanTitle, "UTF-8"))
                append("&artist_name=").append(URLEncoder.encode(cleanArtist, "UTF-8"))
                if (!album.isNullOrBlank()) {
                    append("&album_name=").append(URLEncoder.encode(album.trim(), "UTF-8"))
                }
                append("&duration=").append(durationMs / 1000L)
            }
            lyricsGet("$BASE_URL/get?$getParams")?.let { raw ->
                parseTrackObject(raw)?.let { return it }
            }
        }

        // 2. Fallback /api/search
        val searchParams = buildString {
            append("track_name=").append(URLEncoder.encode(cleanTitle, "UTF-8"))
            if (cleanArtist.isNotEmpty()) {
                append("&artist_name=").append(URLEncoder.encode(cleanArtist, "UTF-8"))
            }
        }
        val searchRaw = lyricsGet("$BASE_URL/search?$searchParams")
            ?: lyricsGet("$BASE_URL/search?q=${URLEncoder.encode("$cleanTitle $cleanArtist".trim(), "UTF-8")}")
            ?: return null

        return pickBestSearchMatch(searchRaw, durationMs)
    }

    private fun parseTrackObject(rawJson: String): List<LyricLine>? {
        return try {
            val obj = JSONObject(rawJson)
            if (obj.optBoolean("instrumental", false)) return null
            val synced = obj.optString("syncedLyrics", "").trim()
            if (synced.isNotEmpty()) {
                parseLrc(synced).takeIf { it.isNotEmpty() }?.let { return it }
            }
            val plain = obj.optString("plainLyrics", "").trim()
            if (plain.isNotEmpty()) {
                Genius.textToLyricLines(plain).takeIf { it.isNotEmpty() }
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun pickBestSearchMatch(rawJson: String, targetDurationMs: Long): List<LyricLine>? {
        return try {
            val arr = JSONArray(rawJson)
            if (arr.length() == 0) return null

            data class Candidate(val obj: JSONObject, val hasSynced: Boolean, val durationDiffMs: Long)

            val candidates = ArrayList<Candidate>(arr.length())
            for (i in 0 until arr.length()) {
                val item = arr.optJSONObject(i) ?: continue
                if (item.optBoolean("instrumental", false)) continue
                val synced = item.optString("syncedLyrics", "").trim()
                val plain = item.optString("plainLyrics", "").trim()
                if (synced.isEmpty() && plain.isEmpty()) continue
                val durSec = item.optDouble("duration", 0.0)
                val durMs = (durSec * 1000.0).toLong()
                val diff = if (targetDurationMs > 0L && durMs > 0L) abs(durMs - targetDurationMs) else 0L
                if (targetDurationMs > 0L && durMs > 0L && diff > 15_000L) continue
                candidates.add(Candidate(item, synced.isNotEmpty(), diff))
            }

            val sorted = candidates.sortedWith(
                compareByDescending<Candidate> { it.hasSynced }.thenBy { it.durationDiffMs }
            )
            for (cand in sorted) {
                val synced = cand.obj.optString("syncedLyrics", "").trim()
                if (synced.isNotEmpty()) {
                    parseLrc(synced).takeIf { it.isNotEmpty() }?.let { return it }
                }
                val plain = cand.obj.optString("plainLyrics", "").trim()
                if (plain.isNotEmpty()) {
                    Genius.textToLyricLines(plain).takeIf { it.isNotEmpty() }?.let { return it }
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    fun parseLrc(lrc: String): List<LyricLine> {
        val trimmed = lrc.trim()
        if (trimmed.isEmpty()) return emptyList()
        if (KaraokeLrc.looksLike(trimmed)) {
            return KaraokeLrc.parse(trimmed)
        }
        if (WORD_TAG_CHECK_REGEX.containsMatchIn(trimmed)) {
            return EnhancedLrc.parse(trimmed)
        }

        val out = ArrayList<LyricLine>()
        for (rawLine in trimmed.lines()) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue
            val matches = LINE_TIMESTAMP_REGEX.findAll(line).toList()
            if (matches.isEmpty()) continue

            val lastBracketEnd = matches.last().range.last + 1
            var text = EnhancedLrc.decodeEntities(line.substring(lastBracketEnd).trim())
            if (text.isEmpty() || isGeniusSectionHeader(text)) continue

            var alignment = LyricAlignment.Start
            if (RIGHT_MARKER_REGEX.containsMatchIn(text)) {
                alignment = LyricAlignment.End
                text = text.replaceFirst(RIGHT_MARKER_REGEX, "").trim()
            }
            if (text.isEmpty()) continue

            for (m in matches) {
                val min = m.groupValues[1].toLongOrNull() ?: 0L
                val sec = m.groupValues[2].toLongOrNull() ?: 0L
                val fracStr = m.groupValues[3]
                val ms = when (fracStr.length) {
                    0 -> 0L
                    1 -> (fracStr.toLongOrNull() ?: 0L) * 100L
                    2 -> (fracStr.toLongOrNull() ?: 0L) * 10L
                    else -> fracStr.take(3).toLongOrNull() ?: 0L
                }
                val totalMs = (min * 60_000L) + (sec * 1_000L) + ms
                out.add(
                    LyricLine(
                        timeMs = totalMs,
                        text = text,
                        alignment = alignment,
                    )
                )
            }
        }

        if (out.isEmpty()) return emptyList()
        val sorted = out.sortedBy { it.timeMs }
        val withEnds = sorted.mapIndexed { idx, current ->
            val nextMs = sorted.getOrNull(idx + 1)?.timeMs
            val inferredEnd = if (nextMs != null && nextMs > current.timeMs) {
                minOf(nextMs, current.timeMs + 7_500L)
            } else {
                current.timeMs + 4_500L
            }
            current.copy(sungUntilMs = inferredEnd)
        }
        return withEnds
            .withBackgroundVocals()
            .withInstrumentalGaps()
    }
}
