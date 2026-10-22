package com.example.data.lyrics

import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.net.URLEncoder

object SimpMusicLyrics {
    private const val BASE_URL = "https://api-lyrics.simpmusic.org/v1"

    suspend fun lyrics(videoId: String): List<LyricLine>? {
        val cleanId = videoId
            .removePrefix("yt_track_")
            .removePrefix("yt_vid_")
            .substringBefore("_")
            .trim()
        if (cleanId.isEmpty() || cleanId.startsWith("local_")) return null

        val encoded = URLEncoder.encode(cleanId, "UTF-8")
        val raw = lyricsGet("$BASE_URL/$encoded") ?: return null
        return parse(raw)
    }

    internal fun parse(raw: String): List<LyricLine>? {
        return try {
            val token = JSONTokener(raw.trim()).nextValue()
            val candidateObj: JSONObject? = when (token) {
                is JSONObject -> {
                    val data = token.opt("data")
                    when (data) {
                        is JSONArray -> data.optJSONObject(0)
                        is JSONObject -> data
                        else -> token
                    }
                }
                is JSONArray -> token.optJSONObject(0)
                else -> null
            }
            if (candidateObj != null) {
                for (key in listOf("richSyncLyrics", "syncedLyrics", "lrc", "plainLyrics", "lyrics")) {
                    val str = candidateObj.optString(key, "").trim()
                    if (str.isNotEmpty()) {
                        ProviderLyrics.parse(str)?.takeIf { it.isNotEmpty() }?.let { return it }
                    }
                }
            }
            ProviderLyrics.parse(raw)
        } catch (_: Exception) {
            ProviderLyrics.parse(raw)
        }
    }
}
