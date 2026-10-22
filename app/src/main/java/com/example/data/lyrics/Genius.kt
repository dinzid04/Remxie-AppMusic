package com.example.data.lyrics

import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

object Genius {
    private const val SEARCH_MULTI_URL = "https://genius.com/api/search/multi"
    private const val SEARCH_SONG_URL = "https://genius.com/api/search/song"

    data class Hit(
        val url: String,
        val title: String,
        val artist: String,
    )

    suspend fun lyrics(
        title: String,
        artist: String,
    ): List<LyricLine>? {
        val cleanTitle = title.forLyricsSearch()
        val cleanArtist = artist.artistForLyricsSearch()
        val query = "$cleanTitle $cleanArtist".trim()
        if (query.isEmpty()) return null

        val encoded = URLEncoder.encode(query, "UTF-8")
        val json = lyricsGet(
            url = "$SEARCH_MULTI_URL?q=$encoded",
            userAgent = "BitChord",
        ) ?: lyricsGet(
            url = "$SEARCH_SONG_URL?q=$encoded",
            userAgent = "BitChord",
        ) ?: return null

        val hits = parseHits(json)
        val chosen = bestMatch(hits, cleanTitle, cleanArtist) ?: return null
        val pageHtml = lyricsGet(
            url = chosen.url,
            userAgent = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36",
        ) ?: return null

        val extracted = extractLyricsFromHtml(pageHtml) ?: return null
        val cleaned = stripArtifacts(extracted)
        return textToLyricLines(cleaned).takeIf { it.isNotEmpty() }
    }

    private fun parseHits(rawJson: String): List<Hit> {
        return try {
            val root = JSONObject(rawJson)
            val response = root.optJSONObject("response") ?: return emptyList()
            val hits = ArrayList<Hit>()

            val sections = response.optJSONArray("sections")
            if (sections != null) {
                for (sIdx in 0 until sections.length()) {
                    val sec = sections.optJSONObject(sIdx) ?: continue
                    val type = sec.optString("type", "")
                    if (type != "song" && type != "top_hit" && type != "lyric") continue
                    val secHits = sec.optJSONArray("hits") ?: continue
                    collectHitsFromArray(secHits, hits)
                }
            }

            val directHits = response.optJSONArray("hits")
            if (directHits != null) {
                collectHitsFromArray(directHits, hits)
            }
            hits.distinctBy { it.url }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun collectHitsFromArray(arr: JSONArray, out: MutableList<Hit>) {
        for (i in 0 until arr.length()) {
            val hitObj = arr.optJSONObject(i) ?: continue
            val hitType = hitObj.optString("type", "song")
            if (hitType != "song") continue
            val result = hitObj.optJSONObject("result") ?: continue
            val url = result.optString("url", "").trim()
            if (!url.startsWith("http")) continue
            val title = result.optString("title", result.optString("title_with_featured", "")).trim()
            val artist = result.optJSONObject("primary_artist")?.optString("name", "")
                ?: result.optString("artist_names", "")
            out.add(Hit(url = url, title = title, artist = artist.trim()))
        }
    }

    internal fun bestMatch(
        hits: List<Hit>,
        targetTitle: String,
        targetArtist: String,
    ): Hit? {
        if (hits.isEmpty()) return null
        val normTitle = normalize(targetTitle)
        val normArtist = normalize(targetArtist)
        if (normTitle.isEmpty()) return null

        var best: Hit? = null
        var bestScore = Int.MIN_VALUE

        for (hit in hits) {
            val hTitle = normalize(hit.title)
            val hArtist = normalize(hit.artist)
            val urlLower = hit.url.lowercase()
            if (
                urlLower.endsWith("-annotated") ||
                urlLower.contains("-translations-") ||
                urlLower.contains("-romanized-") ||
                urlLower.contains("-tracklist-")
            ) {
                continue
            }

            var score = 0
            when {
                hTitle == normTitle -> score += 100
                hTitle.startsWith(normTitle) || normTitle.startsWith(hTitle) -> score += 65
                hTitle.contains(normTitle) || normTitle.contains(hTitle) -> score += 40
                else -> continue
            }

            if (normArtist.isNotEmpty()) {
                when {
                    hArtist == normArtist -> score += 80
                    hArtist.contains(normArtist) || normArtist.contains(hArtist) -> score += 50
                    else -> score -= 30
                }
            }

            if (score > bestScore) {
                bestScore = score
                best = hit
            }
        }
        return if (bestScore >= 40) best else null
    }

    internal fun extractLyricsFromHtml(html: String): String? {
        val blocks = ArrayList<String>()
        val marker = "data-lyrics-container=\"true\""
        var searchIdx = 0
        while (true) {
            val found = html.indexOf(marker, searchIdx, ignoreCase = true)
            if (found < 0) break
            val openDivStart = html.lastIndexOf("<div", found, ignoreCase = true)
            val tagClose = html.indexOf('>', found)
            if (openDivStart < 0 || tagClose < 0) {
                searchIdx = found + marker.length
                continue
            }
            val block = extractBalancedDivInner(html, tagClose + 1)
            if (block != null) {
                blocks.add(htmlFragmentToPlainText(block))
                searchIdx = tagClose + 1 + block.length
            } else {
                searchIdx = tagClose + 1
            }
        }

        if (blocks.isEmpty()) {
            val legacyMatch = Regex(
                """<div[^>]+class="[^"]*\blyrics\b[^"]*"[^>]*>(.*?)</div>""",
                setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
            ).find(html)
            if (legacyMatch != null) {
                blocks.add(htmlFragmentToPlainText(legacyMatch.groupValues[1]))
            }
        }

        return blocks.joinToString("\n").trim().takeIf { it.isNotEmpty() }
    }

    private fun extractBalancedDivInner(html: String, startIndex: Int): String? {
        var depth = 1
        var cursor = startIndex
        val len = html.length
        while (cursor < len && depth > 0) {
            val nextOpen = html.indexOf("<div", cursor, ignoreCase = true)
            val nextClose = html.indexOf("</div", cursor, ignoreCase = true)
            if (nextClose < 0) return null
            if (nextOpen in 0 until nextClose) {
                depth++
                cursor = nextOpen + 4
            } else {
                depth--
                if (depth == 0) {
                    return html.substring(startIndex, nextClose)
                }
                cursor = nextClose + 5
            }
        }
        return null
    }

    private fun htmlFragmentToPlainText(fragment: String): String {
        val withoutExcluded = fragment.replace(
            Regex("""<div[^>]+data-exclude-from-selection="true"[^>]*>.*?</div>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)),
            "",
        )
        val withNewlines = withoutExcluded
            .replace(Regex("""<br\s*/?>""", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("""</p>""", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("""<[^>]+>"""), "")
        return EnhancedLrc.decodeEntities(withNewlines)
    }

    internal fun stripArtifacts(raw: String): String {
        var text = raw.trim()
        text = text.replace(Regex("""^\d+\s*Contributors?.*?Lyrics\s*""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), "")
        text = text.replace(Regex("""^.*?Lyrics\s*(?=\[)""", RegexOption.IGNORE_CASE), "")
        text = text.replace(Regex("""You might also like""", RegexOption.IGNORE_CASE), "")
        text = text.replace(Regex("""\d*\s*Embed\s*$""", RegexOption.IGNORE_CASE), "")
        return text.trim()
    }

    fun textToLyricLines(plain: String): List<LyricLine> {
        val lines = plain.lines()
            .map { EnhancedLrc.decodeEntities(it).trim() }
            .filter { it.isNotEmpty() && !isGeniusSectionHeader(it) }
        if (lines.isEmpty()) return emptyList()
        return lines.map { lineText ->
            LyricLine(
                timeMs = 0L,
                text = lineText,
            )
        }.withBackgroundVocals()
    }

    private fun normalize(text: String): String {
        return text.forLyricsSearch()
            .lowercase()
            .replace(Regex("[^\\p{L}\\p{N}\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
