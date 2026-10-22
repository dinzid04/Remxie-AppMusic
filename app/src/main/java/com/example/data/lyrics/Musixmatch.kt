package com.example.data.lyrics

import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.abs
import kotlin.math.roundToLong

object Musixmatch {
    private const val API_BASE = "https://apic.musixmatch.com/ws/1.1"
    private const val FALLBACK_SECRET = "f09016176ba43a1cfd1031fbd6b3d26c"
    private const val APP_ID = "web-desktop-app-v1.0"

    @Volatile
    private var cachedSecret: String? = null

    suspend fun lyrics(
        title: String,
        artist: String,
        durationMs: Long = 0L,
        isrc: String? = null,
    ): List<LyricLine>? {
        val cleanTitle = title.forLyricsSearch()
        val cleanArtist = artist.artistForLyricsSearch()
        if (cleanTitle.isEmpty()) return null

        val trackId = searchTrackId(cleanTitle, cleanArtist, durationMs, isrc) ?: return null

        // 1. Try RichSync (word-by-word synced) first
        fetchRichSync(trackId)?.takeIf { it.isNotEmpty() }?.let { return it }

        // 2. Fallback to Subtitle (line-by-line LRC)
        fetchSubtitle(trackId)?.takeIf { it.isNotEmpty() }?.let { return it }

        return null
    }

    private suspend fun searchTrackId(
        title: String,
        artist: String,
        durationMs: Long,
        isrc: String?,
    ): Long? {
        val params = LinkedHashMap<String, String>().apply {
            put("format", "json")
            put("app_id", APP_ID)
            put("q_track", title)
            if (artist.isNotEmpty()) put("q_artist", artist)
            if (!isrc.isNullOrBlank()) put("track_isrc", isrc.trim())
            put("page_size", "5")
            put("page", "1")
            put("s_track_rating", "desc")
        }
        val raw = signedGet("track.search", params) ?: return null
        return try {
            val root = JSONObject(raw)
            val list = root.optJSONObject("message")
                ?.optJSONObject("body")
                ?.optJSONArray("track_list")
                ?: return null
            if (list.length() == 0) return null

            var bestId: Long? = null
            var bestScore = Int.MIN_VALUE

            for (i in 0 until list.length()) {
                val trackObj = list.optJSONObject(i)?.optJSONObject("track") ?: continue
                val id = trackObj.optLong("track_id", -1L)
                if (id <= 0L) continue
                val hasRich = trackObj.optInt("has_richsync", 0) == 1
                val hasSub = trackObj.optInt("has_subtitles", 0) == 1
                if (!hasRich && !hasSub) continue

                val lengthSec = trackObj.optLong("track_length", 0L)
                val lengthMs = lengthSec * 1000L
                if (durationMs > 0L && lengthMs > 0L && abs(lengthMs - durationMs) > 15_000L) {
                    continue
                }

                var score = 0
                if (hasRich) score += 100
                if (hasSub) score += 50
                if (durationMs > 0L && lengthMs > 0L) {
                    val diff = abs(lengthMs - durationMs)
                    if (diff <= 3_000L) score += 40
                    else if (diff <= 8_000L) score += 20
                }
                if (score > bestScore) {
                    bestScore = score
                    bestId = id
                }
            }
            bestId
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun fetchRichSync(trackId: Long): List<LyricLine>? {
        val params = linkedMapOf(
            "format" to "json",
            "app_id" to APP_ID,
            "track_id" to trackId.toString(),
        )
        val raw = signedGet("track.richsync.get", params) ?: return null
        return try {
            val bodyStr = JSONObject(raw)
                .optJSONObject("message")
                ?.optJSONObject("body")
                ?.optJSONObject("richsync")
                ?.optString("richsync_body", "")
                .orEmpty()
            if (bodyStr.isBlank()) null else parseRichSyncBody(bodyStr)
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun fetchSubtitle(trackId: Long): List<LyricLine>? {
        val params = linkedMapOf(
            "format" to "json",
            "app_id" to APP_ID,
            "track_id" to trackId.toString(),
            "subtitle_format" to "mxm",
        )
        val raw = signedGet("track.subtitle.get", params) ?: return null
        return try {
            val bodyStr = JSONObject(raw)
                .optJSONObject("message")
                ?.optJSONObject("body")
                ?.optJSONObject("subtitle")
                ?.optString("subtitle_body", "")
                .orEmpty()
            if (bodyStr.isBlank()) return null
            val lrc = subtitleToLrc(bodyStr) ?: bodyStr
            LrcLib.parseLrc(lrc)
        } catch (_: Exception) {
            null
        }
    }

    internal fun parseRichSyncBody(rawBody: String): List<LyricLine>? {
        return try {
            val arr = JSONArray(rawBody.trim())
            if (arr.length() == 0) return null
            val lines = ArrayList<LyricLine>(arr.length())

            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val tsSec = obj.optDouble("ts", -1.0)
                if (tsSec < 0.0) continue
                val teSec = obj.optDouble("te", tsSec + 3.5)
                val lineStartMs = (tsSec * 1000.0).roundToLong()
                val lineEndMs = (teSec * 1000.0).roundToLong().coerceAtLeast(lineStartMs + 100L)
                val fullText = obj.optString("x", "").trim()
                val lArr = obj.optJSONArray("l")

                val runs = ArrayList<TimedRun>()
                if (lArr != null && lArr.length() > 0) {
                    for (j in 0 until lArr.length()) {
                        val tok = lArr.optJSONObject(j) ?: continue
                        val c = tok.optString("c", "")
                        if (c.isEmpty()) continue
                        val oSec = tok.optDouble("o", 0.0)
                        val nextOSec = lArr.optJSONObject(j + 1)?.optDouble("o", teSec - tsSec) ?: (teSec - tsSec)
                        val wStart = lineStartMs + (oSec * 1000.0).roundToLong()
                        val wEnd = (lineStartMs + (nextOSec * 1000.0).roundToLong()).coerceAtLeast(wStart + 40L)
                        runs.add(TimedRun(wStart, wEnd, c))
                    }
                }

                val words = wordsFromRuns(runs, spacingIsExplicit = true)
                val text = if (words.isNotEmpty()) {
                    words.joinToString(" ") { it.text }
                } else {
                    fullText
                }
                if (text.isEmpty() || isGeniusSectionHeader(text)) continue

                lines.add(
                    LyricLine(
                        timeMs = words.firstOrNull()?.startMs ?: lineStartMs,
                        text = text,
                        words = words,
                        sungUntilMs = lineEndMs,
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

    internal fun subtitleToLrc(subtitleBody: String): String? {
        val trimmed = subtitleBody.trim()
        if (trimmed.startsWith("[")) {
            return try {
                val arr = JSONArray(trimmed)
                val sb = StringBuilder()
                for (i in 0 until arr.length()) {
                    val item = arr.optJSONObject(i) ?: continue
                    val text = item.optString("text", "").trim()
                    val timeObj = item.optJSONObject("time")
                    val min = timeObj?.optInt("minutes", 0) ?: 0
                    val sec = timeObj?.optInt("seconds", 0) ?: 0
                    val hun = timeObj?.optInt("hundredths", 0) ?: 0
                    sb.append("[%02d:%02d.%02d]%s\n".format(min, sec, hun, text))
                }
                sb.toString().takeIf { it.isNotBlank() }
            } catch (_: Exception) {
                null
            }
        }
        return trimmed
    }

    private suspend fun signedGet(endpoint: String, params: Map<String, String>): String? {
        val query = params.entries.joinToString("&") { (k, v) ->
            "${URLEncoder.encode(k, "UTF-8")}=${URLEncoder.encode(v, "UTF-8")}"
        }
        val baseUrl = "$API_BASE/$endpoint?$query"
        val secret = resolveSecret()
        val signedUrl = signUrl(baseUrl, secret)
        return lyricsGet(
            url = signedUrl,
            userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
            extraHeaders = mapOf("Cookie" to "x-mxm-token-guid=undefined"),
        )
    }

    private suspend fun resolveSecret(): String {
        cachedSecret?.let { return it }
        val scraped = runCatching {
            val html = lyricsGet("https://www.musixmatch.com/search") ?: return@runCatching null
            val appScript = Regex("""https://[^\s"']+_app-[^\s"']+\.js""").findAll(html).lastOrNull()?.value
                ?: return@runCatching null
            val js = lyricsGet(appScript) ?: return@runCatching null
            Regex("""[0-9a-f]{32}""").find(js)?.value
        }.getOrNull()
        val secret = scraped ?: FALLBACK_SECRET
        cachedSecret = secret
        return secret
    }

    private fun signUrl(unsignedUrl: String, secret: String): String {
        return try {
            val dateStamp = SimpleDateFormat("yyyyMMdd", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }.format(Date())
            val message = unsignedUrl + dateStamp
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256"))
            val rawHmac = mac.doFinal(message.toByteArray(Charsets.UTF_8))
            val sig = Base64.encodeToString(rawHmac, Base64.NO_WRAP)
            "$unsignedUrl&signature=${URLEncoder.encode(sig, "UTF-8")}&signature_protocol=sha256"
        } catch (_: Exception) {
            unsignedUrl
        }
    }
}
