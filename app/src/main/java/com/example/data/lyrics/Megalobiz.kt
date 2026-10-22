package com.example.data.lyrics

import java.net.URLEncoder

object Megalobiz {
    private const val BASE_URL = "https://www.megalobiz.com"

    suspend fun lyrics(
        title: String,
        artist: String,
    ): List<LyricLine>? {
        val cleanTitle = title.forLyricsSearch()
        val cleanArtist = artist.artistForLyricsSearch()
        val query = "$cleanTitle $cleanArtist".trim()
        if (query.isEmpty()) return null

        val encoded = URLEncoder.encode(query, "UTF-8")
        val searchHtml = lyricsGet("$BASE_URL/search/all?qry=$encoded") ?: return null
        val path = extractBestLrcPath(searchHtml, cleanTitle, cleanArtist) ?: return null
        val detailHtml = lyricsGet("$BASE_URL$path") ?: return null
        val lrcRaw = extractLrcSpan(detailHtml) ?: return null
        return LrcLib.parseLrc(lrcRaw).takeIf { it.isNotEmpty() }
    }

    private fun extractBestLrcPath(html: String, title: String, artist: String): String? {
        val linkRegex = Regex("""<a[^>]+href="(/lrc/maker/[^"]+)"[^>]*>(.*?)</a>""", RegexOption.IGNORE_CASE)
        val normTitle = title.lowercase()
        val normArtist = artist.lowercase()

        var bestHref: String? = null
        var bestScore = Int.MIN_VALUE

        for (match in linkRegex.findAll(html)) {
            val href = match.groupValues[1].trim()
            val label = match.groupValues[2].replace(Regex("<[^>]+>"), " ").lowercase()
            var score = 0
            if (label.contains(normTitle)) score += 60
            if (normArtist.isNotEmpty() && label.contains(normArtist)) score += 40
            if (score > bestScore) {
                bestScore = score
                bestHref = href
            }
        }
        return if (bestScore >= 40) bestHref else null
    }

    private fun extractLrcSpan(html: String): String? {
        val spanRegex = Regex(
            """<span[^>]+id="lrc_\d+_lyrics"[^>]*>(.*?)</span>""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
        )
        val match = spanRegex.find(html) ?: return null
        val rawInner = match.groupValues[1]
            .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("<[^>]+>"), "")
        return EnhancedLrc.decodeEntities(rawInner).trim().takeIf { it.isNotEmpty() }
    }
}
