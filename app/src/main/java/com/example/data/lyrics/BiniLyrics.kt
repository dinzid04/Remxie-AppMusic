package com.example.data.lyrics

import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import kotlin.math.abs

object BiniLyrics {
    private const val BASE_URL = "https://lyrics-api.binimum.org"

    data class Track(
        val id: String = "",
        val title: String = "",
        val artist: String = "",
        val album: String? = null,
        val isrc: String? = null,
        val durationMs: Long? = null,
    )

    suspend fun identify(
        title: String,
        artist: String,
        album: String? = null,
        durationMs: Long = 0L,
    ): Track? {
        val cleanTitle = title.forLyricsSearch()
        val cleanArtist = artist.artistForLyricsSearch()
        if (cleanTitle.isEmpty()) return null

        val params = buildString {
            append("track=").append(URLEncoder.encode(cleanTitle, "UTF-8"))
            if (cleanArtist.isNotEmpty()) {
                append("&artist=").append(URLEncoder.encode(cleanArtist, "UTF-8"))
            }
            if (!album.isNullOrBlank()) {
                append("&album=").append(URLEncoder.encode(album.trim(), "UTF-8"))
            }
            if (durationMs > 0L) {
                append("&duration=").append(durationMs / 1000L)
            }
        }

        val raw = lyricsGet("$BASE_URL/?$params") ?: return null
        return parseTrackFromResponse(raw, cleanTitle, cleanArtist, durationMs)
    }

    suspend fun lyricsFor(track: Track): List<LyricLine>? {
        val isrc = track.isrc?.takeIf { it.isNotBlank() }
        val params = buildString {
            if (isrc != null) {
                append("isrc=").append(URLEncoder.encode(isrc, "UTF-8"))
            }
            if (track.title.isNotBlank()) {
                if (isNotEmpty()) append("&")
                append("track=").append(URLEncoder.encode(track.title, "UTF-8"))
            }
            if (track.artist.isNotBlank()) {
                if (isNotEmpty()) append("&")
                append("artist=").append(URLEncoder.encode(track.artist, "UTF-8"))
            }
            if (!track.album.isNullOrBlank()) {
                if (isNotEmpty()) append("&")
                append("album=").append(URLEncoder.encode(track.album, "UTF-8"))
            }
        }
        if (params.isEmpty()) return null
        val raw = lyricsGet("$BASE_URL/?$params") ?: return null
        return ProviderLyrics.parse(raw)
    }

    suspend fun lyrics(
        title: String,
        artist: String,
        album: String? = null,
        durationMs: Long = 0L,
        isrc: String? = null,
    ): List<LyricLine>? {
        val cleanTitle = title.forLyricsSearch()
        val cleanArtist = artist.artistForLyricsSearch()
        val params = buildString {
            if (!isrc.isNullOrBlank()) {
                append("isrc=").append(URLEncoder.encode(isrc.trim(), "UTF-8"))
            }
            if (cleanTitle.isNotEmpty()) {
                if (isNotEmpty()) append("&")
                append("track=").append(URLEncoder.encode(cleanTitle, "UTF-8"))
            }
            if (cleanArtist.isNotEmpty()) {
                if (isNotEmpty()) append("&")
                append("artist=").append(URLEncoder.encode(cleanArtist, "UTF-8"))
            }
            if (!album.isNullOrBlank()) {
                if (isNotEmpty()) append("&")
                append("album=").append(URLEncoder.encode(album.trim(), "UTF-8"))
            }
            if (durationMs > 0L) {
                if (isNotEmpty()) append("&")
                append("duration=").append(durationMs / 1000L)
            }
        }
        if (params.isEmpty()) return null
        val raw = lyricsGet("$BASE_URL/?$params") ?: return null
        return ProviderLyrics.parse(raw)
    }

    private fun parseTrackFromResponse(
        raw: String,
        fallbackTitle: String,
        fallbackArtist: String,
        targetDurationMs: Long,
    ): Track? {
        return try {
            val trimmed = raw.trim()
            if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) return null
            val obj = if (trimmed.startsWith("{")) JSONObject(trimmed) else {
                val arr = JSONArray(trimmed)
                arr.optJSONObject(0) ?: return null
            }
            val isrc = obj.optString("isrc", "")
                .ifBlank { obj.optJSONObject("track")?.optString("isrc", "").orEmpty() }
                .ifBlank { null }
            val title = obj.optString("track", obj.optString("title", fallbackTitle)).ifBlank { fallbackTitle }
            val artist = obj.optString("artist", fallbackArtist).ifBlank { fallbackArtist }
            val album = obj.optString("album", "").ifBlank { null }
            val durMs = obj.optLong("durationMs", obj.optLong("duration", 0L)).let {
                if (it in 1L..1_500L) it * 1000L else it.takeIf { d -> d > 0L }
            }
            if (targetDurationMs > 0L && durMs != null && abs(durMs - targetDurationMs) > 15_000L) {
                // Keep if ISRC exists and closely matches title
            }
            Track(
                id = obj.optString("id", isrc ?: ""),
                title = title,
                artist = artist,
                album = album,
                isrc = isrc,
                durationMs = durMs,
            )
        } catch (_: Exception) {
            null
        }
    }
}
