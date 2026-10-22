package com.example.data.lyrics

import android.util.Base64
import org.json.JSONObject
import java.net.URLEncoder
import kotlin.math.abs

object KuGou {
    private const val SEARCH_SONG_URL = "https://mobileservice.kugou.com/api/v3/search/song"
    private const val SEARCH_LYRICS_URL = "https://lyrics.kugou.com/search"
    private const val DOWNLOAD_LYRICS_URL = "https://lyrics.kugou.com/download"

    private val CREDIT_PREFIXES = listOf(
        "作词", "作曲", "编曲", "制作人", "混音", "母带", "录音", "吉他", "贝斯",
        "鼓", "键盘", "弦乐", "和声", "监制", "统筹", "出品", "发行", "演唱",
        "原唱", "翻唱", "词：", "曲：", "编：", "OP", "SP", "lyricist", "composer",
        "arranger", "producer", "mixed by", "mastered by",
    )

    suspend fun lyrics(
        title: String,
        artist: String,
        durationMs: Long = 0L,
    ): List<LyricLine>? {
        val cleanTitle = title.forLyricsSearch()
        val cleanArtist = artist.artistForLyricsSearch()
        if (cleanTitle.isEmpty()) return null

        val keyword = "$cleanArtist - $cleanTitle".trim(' ', '-')
        val encodedKeyword = URLEncoder.encode(keyword, "UTF-8")
        val searchUrl = "$SEARCH_SONG_URL?version=9108&plat=0&pagesize=8&showtype=0&keyword=$encodedKeyword"
        val songJson = lyricsGet(searchUrl) ?: return null

        val candidates = parseSongCandidates(songJson, cleanTitle, cleanArtist, durationMs)
        for (candidate in candidates.take(3)) {
            val lrcText = fetchCandidateLrc(candidate.hash, candidate.durationMs.coerceAtLeast(durationMs))
                ?: continue
            val lines = LrcLib.parseLrc(lrcText).stripCredits()
            if (lines.isNotEmpty() && !lines.isInstrumentalNotice()) {
                return lines
            }
        }

        // Direct lyrics search fallback without hash
        val fallbackLrc = fetchCandidateLrc(hash = "", durationMs = durationMs, keyword = keyword)
        if (!fallbackLrc.isNullOrBlank()) {
            val lines = LrcLib.parseLrc(fallbackLrc).stripCredits()
            if (lines.isNotEmpty() && !lines.isInstrumentalNotice()) {
                return lines
            }
        }
        return null
    }

    private suspend fun fetchCandidateLrc(
        hash: String,
        durationMs: Long,
        keyword: String = "",
    ): String? {
        val durParam = if (durationMs > 0L) durationMs else 200_000L
        val params = buildString {
            append("ver=1&man=yes&client=pc&duration=").append(durParam)
            if (hash.isNotBlank()) {
                append("&hash=").append(URLEncoder.encode(hash, "UTF-8"))
            }
            if (keyword.isNotBlank()) {
                append("&keyword=").append(URLEncoder.encode(keyword, "UTF-8"))
            }
        }
        val searchRes = lyricsGet("$SEARCH_LYRICS_URL?$params") ?: return null
        val root = runCatching { JSONObject(searchRes) }.getOrNull() ?: return null
        val cands = root.optJSONArray("candidates") ?: return null
        if (cands.length() == 0) return null
        val first = cands.optJSONObject(0) ?: return null
        val id = first.optString("id", "").trim()
        val accessKey = first.optString("accesskey", "").trim()
        if (id.isEmpty() || accessKey.isEmpty()) return null

        val dlUrl = "$DOWNLOAD_LYRICS_URL?ver=1&client=pc&id=${URLEncoder.encode(id, "UTF-8")}&accesskey=${URLEncoder.encode(accessKey, "UTF-8")}&fmt=lrc&charset=utf8"
        val dlRes = lyricsGet(dlUrl) ?: return null
        val dlObj = runCatching { JSONObject(dlRes) }.getOrNull() ?: return null
        val contentBase64 = dlObj.optString("content", "").trim()
        if (contentBase64.isEmpty()) return null
        val decodedBytes = runCatching { Base64.decode(contentBase64, Base64.DEFAULT) }.getOrNull() ?: return null
        return String(decodedBytes, Charsets.UTF_8).takeIf { it.isNotBlank() }
    }

    private data class SongCandidate(
        val hash: String,
        val songName: String,
        val singerName: String,
        val durationMs: Long,
        val score: Int,
    )

    private fun parseSongCandidates(
        rawJson: String,
        targetTitle: String,
        targetArtist: String,
        targetDurationMs: Long,
    ): List<SongCandidate> {
        return try {
            val root = JSONObject(rawJson)
            val infoArr = root.optJSONObject("data")?.optJSONArray("info") ?: return emptyList()
            val normTitle = targetTitle.lowercase()
            val normArtist = targetArtist.lowercase()
            val list = ArrayList<SongCandidate>(infoArr.length())

            for (i in 0 until infoArr.length()) {
                val item = infoArr.optJSONObject(i) ?: continue
                val hash = item.optString("hash", "").trim()
                if (hash.isEmpty()) continue
                val songName = item.optString("songname", item.optString("filename", "")).trim()
                val singerName = item.optString("singername", "").trim()
                val durSec = item.optLong("duration", 0L)
                val durMs = durSec * 1000L

                if (targetDurationMs > 0L && durMs > 0L && abs(durMs - targetDurationMs) > 15_000L) {
                    continue
                }

                var score = 0
                val sLower = songName.lowercase()
                val aLower = singerName.lowercase()
                if (sLower == normTitle) score += 80
                else if (sLower.contains(normTitle) || normTitle.contains(sLower)) score += 45

                if (normArtist.isNotEmpty()) {
                    if (aLower == normArtist) score += 60
                    else if (aLower.contains(normArtist) || normArtist.contains(aLower)) score += 35
                }

                if (targetDurationMs > 0L && durMs > 0L) {
                    val diff = abs(durMs - targetDurationMs)
                    if (diff <= 3_000L) score += 30
                    else if (diff <= 7_000L) score += 15
                }

                list.add(SongCandidate(hash, songName, singerName, durMs, score))
            }
            list.sortedByDescending { it.score }
        } catch (_: Exception) {
            emptyList()
        }
    }

    internal fun List<LyricLine>.stripCredits(): List<LyricLine> {
        if (isEmpty()) return this
        return filterIndexed { idx, line ->
            if (line.isGap) return@filterIndexed true
            if (idx > 6 && line.timeMs > 25_000L) return@filterIndexed true
            val t = line.text.trim()
            val hasColon = t.contains(':') || t.contains('：')
            val isCredit = hasColon && CREDIT_PREFIXES.any { prefix ->
                t.startsWith(prefix, ignoreCase = true)
            }
            !isCredit
        }
    }

    internal fun List<LyricLine>.isInstrumentalNotice(): Boolean {
        val nonGap = filter { !it.isGap }
        if (nonGap.isEmpty()) return true
        if (nonGap.size <= 2) {
            val combined = nonGap.joinToString(" ") { it.text.lowercase() }
            if (
                combined.contains("纯音乐") ||
                combined.contains("请欣赏") ||
                combined.contains("instrumental") ||
                combined.contains("no lyrics")
            ) {
                return true
            }
        }
        return false
    }
}
