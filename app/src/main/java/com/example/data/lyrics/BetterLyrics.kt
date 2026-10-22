package com.example.data.lyrics

import java.net.URLEncoder

object BetterLyrics {
    private const val APPLE_URL = "https://lyrics-api.boidu.dev/getLyrics"
    private const val PORTATO_URL = "https://lyrics-api.boidu.dev/qq/getLyrics"

    suspend fun lyrics(
        title: String,
        artist: String,
        album: String? = null,
        durationMs: Long = 0L,
    ): List<LyricLine>? = queryEndpoint(
        endpoint = APPLE_URL,
        title = title,
        artist = artist,
        album = album,
        durationMs = durationMs,
    )

    suspend fun portato(
        title: String,
        artist: String,
        album: String? = null,
        durationMs: Long = 0L,
    ): List<LyricLine>? = queryEndpoint(
        endpoint = PORTATO_URL,
        title = title,
        artist = artist,
        album = album,
        durationMs = durationMs,
    )

    private suspend fun queryEndpoint(
        endpoint: String,
        title: String,
        artist: String,
        album: String?,
        durationMs: Long,
    ): List<LyricLine>? {
        val cleanTitle = title.forLyricsSearch()
        val cleanArtist = artist.artistForLyricsSearch()
        if (cleanTitle.isEmpty()) return null

        val query = buildString {
            append("s=").append(URLEncoder.encode(cleanTitle, "UTF-8"))
            if (cleanArtist.isNotEmpty()) {
                append("&a=").append(URLEncoder.encode(cleanArtist, "UTF-8"))
            }
            if (!album.isNullOrBlank()) {
                append("&al=").append(URLEncoder.encode(album.trim(), "UTF-8"))
            }
            if (durationMs > 0L) {
                append("&d=").append(durationMs / 1000L)
            }
        }

        val raw = lyricsGet("$endpoint?$query") ?: return null
        return ProviderLyrics.parse(raw)
    }
}
