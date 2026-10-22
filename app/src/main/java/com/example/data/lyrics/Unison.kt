package com.example.data.lyrics

import java.net.URLEncoder

object Unison {
    private const val BASE_URL = "https://unison.boidu.dev/lyrics"

    suspend fun lyrics(
        title: String,
        artist: String,
        album: String? = null,
        durationMs: Long = 0L,
        isrc: String? = null,
    ): List<LyricLine>? {
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
            if (!isrc.isNullOrBlank()) {
                append("&isrc=").append(URLEncoder.encode(isrc.trim(), "UTF-8"))
            }
        }
        val raw = lyricsGet("$BASE_URL?$params") ?: return null
        return ProviderLyrics.parse(raw)
    }
}
