package com.example.data.lyrics

import org.json.JSONArray
import org.json.JSONObject
import org.w3c.dom.Element
import java.io.ByteArrayInputStream
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.math.roundToLong

object YouTubeMusicLyrics {
    private const val INNERTUBE_NEXT = "https://music.youtube.com/youtubei/v1/next?prettyPrint=false"
    private const val INNERTUBE_BROWSE = "https://music.youtube.com/youtubei/v1/browse?prettyPrint=false"

    suspend fun lyrics(
        videoId: String,
        title: String = "",
        artist: String = "",
    ): List<LyricLine>? {
        val cleanId = cleanVideoId(videoId)
        if (cleanId.isEmpty()) return null

        val nextBody = """
            {
              "context": {
                "client": {
                  "clientName": "WEB_REMIX",
                  "clientVersion": "1.20241118.01.00",
                  "hl": "en",
                  "gl": "US"
                }
              },
              "videoId": "$cleanId"
            }
        """.trimIndent()

        val nextJson = lyricsPostJson(INNERTUBE_NEXT, nextBody) ?: return null
        val browseId = extractLyricsBrowseId(nextJson) ?: return null

        val browseBody = """
            {
              "context": {
                "client": {
                  "clientName": "WEB_REMIX",
                  "clientVersion": "1.20241118.01.00",
                  "hl": "en",
                  "gl": "US"
                }
              },
              "browseId": "$browseId"
            }
        """.trimIndent()

        val browseJson = lyricsPostJson(INNERTUBE_BROWSE, browseBody) ?: return null
        return parseBrowseLyrics(browseJson)
    }

    private fun extractLyricsBrowseId(rawJson: String): String? {
        return try {
            val regex = Regex(""""browseId"\s*:\s*"(MPLYt[^"]+)"""")
            regex.find(rawJson)?.groupValues?.getOrNull(1)
        } catch (_: Exception) {
            null
        }
    }

    internal fun parseBrowseLyrics(rawJson: String): List<LyricLine>? {
        return try {
            val root = JSONObject(rawJson)
            // 1. Check timedLyricsData if returned by InnerTube
            val timedLines = ArrayList<LyricLine>()
            collectTimedLyricsData(root, timedLines)
            if (timedLines.isNotEmpty()) {
                return timedLines
                    .sortedBy { it.timeMs }
                    .withBackgroundVocals()
                    .withInstrumentalGaps()
            }

            // 2. Fallback to plain descriptionShelfRenderer text
            val descriptions = ArrayList<String>()
            collectPlainDescriptions(root, descriptions)
            val bestPlain = descriptions.maxByOrNull { it.length }?.trim().orEmpty()
            if (bestPlain.isNotEmpty()) {
                Genius.textToLyricLines(bestPlain).takeIf { it.isNotEmpty() }
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun collectTimedLyricsData(node: Any?, out: MutableList<LyricLine>) {
        when (node) {
            is JSONObject -> {
                val timedArr = node.optJSONArray("timedLyricsData")
                if (timedArr != null) {
                    for (i in 0 until timedArr.length()) {
                        val item = timedArr.optJSONObject(i) ?: continue
                        val lineText = item.optString("lyricLine", "").trim()
                        if (lineText.isEmpty() || lineText == "♪" || isGeniusSectionHeader(lineText)) continue
                        val cue = item.optJSONObject("cueRange")
                        val startMs = cue?.optString("startTimeMilliseconds")?.toLongOrNull()
                            ?: cue?.optLong("startTimeMilliseconds", -1L)
                            ?: -1L
                        val endMs = cue?.optString("endTimeMilliseconds")?.toLongOrNull()
                            ?: cue?.optLong("endTimeMilliseconds", -1L)
                        if (startMs >= 0L) {
                            out.add(
                                LyricLine(
                                    timeMs = startMs,
                                    text = lineText,
                                    sungUntilMs = endMs?.takeIf { it > startMs },
                                )
                            )
                        }
                    }
                }
                val keys = node.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    if (k != "timedLyricsData") {
                        collectTimedLyricsData(node.opt(k), out)
                    }
                }
            }
            is JSONArray -> {
                for (i in 0 until node.length()) {
                    collectTimedLyricsData(node.opt(i), out)
                }
            }
        }
    }

    private fun collectPlainDescriptions(node: Any?, out: MutableList<String>) {
        when (node) {
            is JSONObject -> {
                val shelf = node.optJSONObject("musicDescriptionShelfRenderer")
                if (shelf != null) {
                    val desc = shelf.optJSONObject("description")
                    val runs = desc?.optJSONArray("runs")
                    if (runs != null) {
                        val sb = StringBuilder()
                        for (i in 0 until runs.length()) {
                            sb.append(runs.optJSONObject(i)?.optString("text", "").orEmpty())
                        }
                        if (sb.isNotBlank()) out.add(sb.toString())
                    }
                }
                val keys = node.keys()
                while (keys.hasNext()) {
                    collectPlainDescriptions(node.opt(keys.next()), out)
                }
            }
            is JSONArray -> {
                for (i in 0 until node.length()) {
                    collectPlainDescriptions(node.opt(i), out)
                }
            }
        }
    }
}

object YouTubeTranscriptLyrics {
    private const val INNERTUBE_PLAYER = "https://www.youtube.com/youtubei/v1/player?prettyPrint=false"

    suspend fun lyrics(videoId: String): List<LyricLine>? {
        val cleanId = cleanVideoId(videoId)
        if (cleanId.isEmpty()) return null

        val playerBody = """
            {
              "context": {
                "client": {
                  "clientName": "ANDROID",
                  "clientVersion": "19.09.37",
                  "androidSdkVersion": 34,
                  "hl": "en",
                  "gl": "US"
                }
              },
              "videoId": "$cleanId"
            }
        """.trimIndent()

        val playerJson = lyricsPostJson(
            url = INNERTUBE_PLAYER,
            jsonBody = playerBody,
            userAgent = "com.google.android.youtube/19.09.37 (Linux; U; Android 14) gzip",
        ) ?: return null

        val baseUrl = extractCaptionTrackUrl(playerJson) ?: return null
        val json3Url = if (baseUrl.contains("fmt=")) {
            baseUrl.replace(Regex("fmt=[^&]+"), "fmt=json3")
        } else {
            "$baseUrl&fmt=json3"
        }

        lyricsGet(json3Url)?.let { rawJson3 ->
            parseJson3Captions(rawJson3)?.takeIf { it.isNotEmpty() }?.let { return it }
        }

        val xmlRaw = lyricsGet(baseUrl) ?: return null
        return parseXmlCaptions(xmlRaw)
    }

    private fun extractCaptionTrackUrl(rawPlayerJson: String): String? {
        return try {
            val root = JSONObject(rawPlayerJson)
            val tracks = root.optJSONObject("captions")
                ?.optJSONObject("playerCaptionsTracklistRenderer")
                ?.optJSONArray("captionTracks")
                ?: return null
            if (tracks.length() == 0) return null

            // Prefer manual non-ASR track over auto-generated track
            var selected: JSONObject? = null
            for (i in 0 until tracks.length()) {
                val track = tracks.optJSONObject(i) ?: continue
                val kind = track.optString("kind", "")
                if (kind != "asr") {
                    selected = track
                    break
                }
            }
            val chosen = selected ?: tracks.optJSONObject(0) ?: return null
            chosen.optString("baseUrl", "").takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }

    internal fun parseJson3Captions(raw: String): List<LyricLine>? {
        return try {
            val root = JSONObject(raw)
            val events = root.optJSONArray("events") ?: return null
            val lines = ArrayList<LyricLine>()

            for (i in 0 until events.length()) {
                val ev = events.optJSONObject(i) ?: continue
                val tStartMs = ev.optLong("tStartMs", -1L)
                if (tStartMs < 0L) continue
                val dDurationMs = ev.optLong("dDurationMs", 3_500L).coerceAtLeast(300L)
                val segs = ev.optJSONArray("segs") ?: continue

                val runs = ArrayList<TimedRun>()
                val plainBuilder = StringBuilder()
                for (j in 0 until segs.length()) {
                    val seg = segs.optJSONObject(j) ?: continue
                    val utf8 = seg.optString("utf8", "")
                    if (utf8.isEmpty() || utf8 == "\n") continue
                    plainBuilder.append(utf8)
                    val offsetMs = seg.optLong("tOffsetMs", 0L).coerceAtLeast(0L)
                    val segStart = tStartMs + offsetMs
                    val nextOffset = segs.optJSONObject(j + 1)?.optLong("tOffsetMs", dDurationMs) ?: dDurationMs
                    val segEnd = (tStartMs + nextOffset.coerceAtLeast(offsetMs + 60L)).coerceAtMost(tStartMs + dDurationMs)
                    runs.add(TimedRun(segStart, segEnd, utf8))
                }

                val words = if (runs.size > 1) wordsFromRuns(runs, spacingIsExplicit = true) else emptyList()
                val text = cleanCaptionLine(
                    if (words.isNotEmpty()) words.joinToString(" ") { it.text } else plainBuilder.toString()
                )
                if (text.isEmpty() || isGeniusSectionHeader(text)) continue

                lines.add(
                    LyricLine(
                        timeMs = words.firstOrNull()?.startMs ?: tStartMs,
                        text = text,
                        words = words,
                        sungUntilMs = tStartMs + dDurationMs,
                    )
                )
            }

            lines.takeIf { it.isNotEmpty() }
                ?.sortedBy { it.timeMs }
                ?.withBackgroundVocals()
                ?.withInstrumentalGaps()
        } catch (_: Exception) {
            null
        }
    }

    internal fun parseXmlCaptions(rawXml: String): List<LyricLine>? {
        return try {
            val factory = DocumentBuilderFactory.newInstance()
            val doc = factory.newDocumentBuilder()
                .parse(ByteArrayInputStream(rawXml.toByteArray(Charsets.UTF_8)))
            val nodes = doc.getElementsByTagName("text")
            val lines = ArrayList<LyricLine>(nodes.length)
            for (i in 0 until nodes.length) {
                val el = nodes.item(i) as? Element ?: continue
                val startSec = el.getAttribute("start").toDoubleOrNull() ?: continue
                val durSec = el.getAttribute("dur").toDoubleOrNull() ?: 3.5
                val rawText = EnhancedLrc.decodeEntities(el.textContent.orEmpty())
                val clean = cleanCaptionLine(rawText)
                if (clean.isEmpty() || isGeniusSectionHeader(clean)) continue
                val startMs = (startSec * 1000.0).roundToLong()
                val endMs = ((startSec + durSec) * 1000.0).roundToLong()
                lines.add(
                    LyricLine(
                        timeMs = startMs,
                        text = clean,
                        sungUntilMs = endMs.coerceAtLeast(startMs + 250L),
                    )
                )
            }
            lines.takeIf { it.isNotEmpty() }
                ?.sortedBy { it.timeMs }
                ?.withBackgroundVocals()
                ?.withInstrumentalGaps()
        } catch (_: Exception) {
            null
        }
    }

    private fun cleanCaptionLine(raw: String): String {
        return raw
            .replace("\n", " ")
            .replace("♪", "")
            .replace("♫", "")
            .replace(Regex("""^\s*[\[(](?:music|applause|instrumental|intro|outro|cheering|laughter)[)\]]\s*$""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}

private fun cleanVideoId(videoId: String): String {
    val clean = videoId
        .removePrefix("yt_track_")
        .removePrefix("yt_vid_")
        .removePrefix("artist_video_")
        .substringBefore("_")
        .trim()
    if (clean.startsWith("local_") || clean.startsWith("custom_") || clean.length < 6) return ""
    return clean
}
