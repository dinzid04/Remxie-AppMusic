package com.example.data.lyrics

import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.URLEncoder

object LyricsTranslation {
    private const val TRANSLATE_URL = "https://translate.googleapis.com/translate_a/single"
    private const val SEPARATOR = "\n ||| \n"

    private val translationCache = LruCache<String, List<LyricLine>>(64)
    private val romanizationCache = LruCache<String, List<LyricLine>>(64)

    suspend fun translate(
        lines: List<LyricLine>,
        targetLang: String,
    ): List<LyricLine>? = withContext(Dispatchers.IO) {
        if (lines.isEmpty()) return@withContext null
        val nonGapIndices = lines.indices.filter { !lines[it].isGap && lines[it].text.isNotBlank() }
        if (nonGapIndices.isEmpty()) return@withContext null

        val cacheKey = buildString {
            append(targetLang).append('|')
            append(lines.size).append('|')
            append(lines.take(5).joinToString { it.text }.hashCode())
        }
        translationCache.get(cacheKey)?.let { return@withContext it }

        val sourceTexts = nonGapIndices.map { lines[it].text }
        val translatedTexts = translateBatch(sourceTexts, targetLang) ?: return@withContext null
        if (translatedTexts.size != nonGapIndices.size) return@withContext null

        val mapByIndex = HashMap<Int, String>(nonGapIndices.size)
        for (i in nonGapIndices.indices) {
            mapByIndex[nonGapIndices[i]] = translatedTexts[i]
        }

        val out = lines.mapIndexed { idx, original ->
            val translated = mapByIndex[idx]?.takeIf { it.isNotBlank() } ?: return@mapIndexed original
            original.copy(
                text = translated,
                words = emptyList(),
                timingSource = original,
            )
        }
        translationCache.put(cacheKey, out)
        out
    }

    suspend fun romanize(
        lines: List<LyricLine>,
    ): List<LyricLine>? = withContext(Dispatchers.IO) {
        if (lines.isEmpty()) return@withContext null
        val nonGapIndices = lines.indices.filter { !lines[it].isGap && lines[it].text.isNotBlank() }
        if (nonGapIndices.isEmpty()) return@withContext null

        // Check if any line contains non-Latin script (CJK, Hangul, Cyrillic, Arabic, Thai, Devanagari, etc.)
        val hasNonLatin = nonGapIndices.any { idx ->
            lines[idx].text.any { ch -> ch.code > 0x024F && ch.isLetter() }
        }
        if (!hasNonLatin) return@withContext null

        val cacheKey = "rom|${lines.size}|${lines.take(5).joinToString { it.text }.hashCode()}"
        romanizationCache.get(cacheKey)?.let { return@withContext it }

        val sourceTexts = nonGapIndices.map { lines[it].text }
        val romanizedTexts = romanizeBatch(sourceTexts) ?: return@withContext null

        val mapByIndex = HashMap<Int, String>(nonGapIndices.size)
        for (i in nonGapIndices.indices) {
            val r = romanizedTexts.getOrNull(i)?.trim().orEmpty()
            if (r.isNotEmpty()) {
                mapByIndex[nonGapIndices[i]] = r
            }
        }

        if (mapByIndex.isEmpty()) return@withContext null

        val out = lines.mapIndexed { idx, original ->
            val rom = mapByIndex[idx] ?: return@mapIndexed original
            original.copy(
                text = rom,
                words = emptyList(),
                timingSource = original,
            )
        }
        romanizationCache.put(cacheKey, out)
        out
    }

    private suspend fun translateBatch(
        texts: List<String>,
        targetLang: String,
    ): List<String>? {
        val result = ArrayList<String>(texts.size)
        val chunkSize = 25
        for (chunk in texts.chunked(chunkSize)) {
            val joined = chunk.joinToString("\n")
            val encoded = URLEncoder.encode(joined, "UTF-8")
            val url = "$TRANSLATE_URL?client=gtx&sl=auto&tl=${URLEncoder.encode(targetLang, "UTF-8")}&dt=t&q=$encoded"
            val raw = lyricsGet(url) ?: return null
            val parsedChunk = parseTranslatedLines(raw, chunk.size) ?: return null
            result.addAll(parsedChunk)
        }
        return result
    }

    private suspend fun romanizeBatch(texts: List<String>): List<String>? {
        val result = ArrayList<String>(texts.size)
        val chunkSize = 20
        for (chunk in texts.chunked(chunkSize)) {
            val joined = chunk.joinToString("\n")
            val encoded = URLEncoder.encode(joined, "UTF-8")
            val url = "$TRANSLATE_URL?client=gtx&sl=auto&tl=en&dt=rm&dt=t&q=$encoded"
            val raw = lyricsGet(url) ?: return null
            val parsed = parseRomanizedLines(raw, chunk) ?: return null
            result.addAll(parsed)
        }
        return result
    }

    private fun parseTranslatedLines(rawJson: String, expectedCount: Int): List<String>? {
        return try {
            val root = JSONArray(rawJson)
            val sentences = root.optJSONArray(0) ?: return null
            val sb = StringBuilder()
            for (i in 0 until sentences.length()) {
                val item = sentences.optJSONArray(i) ?: continue
                val piece = item.optString(0, "")
                sb.append(piece)
            }
            val lines = sb.toString().split("\n").map { it.trim() }
            if (lines.size == expectedCount) {
                lines
            } else if (lines.size > expectedCount) {
                lines.take(expectedCount)
            } else {
                lines + List(expectedCount - lines.size) { "" }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun parseRomanizedLines(rawJson: String, fallbackChunk: List<String>): List<String>? {
        return try {
            val root = JSONArray(rawJson)
            val sentences = root.optJSONArray(0) ?: return null
            val sb = StringBuilder()
            for (i in 0 until sentences.length()) {
                val item = sentences.optJSONArray(i) ?: continue
                val rom = item.optString(3, "").ifBlank { item.optString(2, "") }
                if (rom.isNotBlank()) {
                    if (sb.isNotEmpty() && !sb.endsWith("\n")) sb.append(" ")
                    sb.append(rom)
                }
            }
            val romLines = sb.toString().split("\n").map { it.trim() }.filter { it.isNotEmpty() }
            if (romLines.size == fallbackChunk.size) {
                romLines
            } else if (sb.isNotBlank()) {
                // Fallback: split or align line by line
                val rawSplit = sb.toString().lines().map { it.trim() }
                fallbackChunk.mapIndexed { idx, orig ->
                    rawSplit.getOrNull(idx)?.takeIf { it.isNotEmpty() } ?: orig
                }
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }
}
