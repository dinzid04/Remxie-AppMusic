package com.example.data.lyrics

import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

object LrcRed {
    private const val BASE_URL = "https://lrc.red"

    data class Entry(
        val id: String = "",
        val title: String = "",
        val artist: String = "",
        val album: String = "",
        val isrc: String = "",
    )

    suspend fun lyrics(
        title: String,
        artist: String,
        album: String? = null,
        isrc: String? = null,
    ): List<LyricLine>? {
        val cleanIsrc = isrc?.trim()?.uppercase()?.takeIf { it.length >= 10 }
        if (cleanIsrc != null) {
            fetchTtml(cleanIsrc)?.let { return it }
        }

        val cleanTitle = title.forLyricsSearch()
        val cleanArtist = artist.artistForLyricsSearch()
        val query = "$cleanTitle $cleanArtist".trim()
        if (query.isEmpty()) return null

        val encoded = URLEncoder.encode(query, "UTF-8")
        val json = lyricsGet("$BASE_URL/search.json?q=$encoded") ?: return null
        val entries = parseSearchEntries(json)
        val matched = best(entries, cleanTitle, cleanArtist, album) ?: return null
        val targetKey = matched.isrc.ifBlank { matched.id }
        if (targetKey.isBlank()) return null
        return fetchTtml(targetKey)
    }

    private suspend fun fetchTtml(key: String): List<LyricLine>? {
        val encoded = URLEncoder.encode(key.trim(), "UTF-8")
        val raw = lyricsGet("$BASE_URL/s/$encoded.ttml") ?: return null
        return ProviderLyrics.parse(raw)
    }

    private fun parseSearchEntries(raw: String): List<Entry> {
        return try {
            val trimmed = raw.trim()
            val arr = when {
                trimmed.startsWith("[") -> JSONArray(trimmed)
                trimmed.startsWith("{") -> {
                    val obj = JSONObject(trimmed)
                    obj.optJSONArray("results")
                        ?: obj.optJSONArray("data")
                        ?: obj.optJSONArray("items")
                        ?: JSONArray()
                }
                else -> JSONArray()
            }
            buildList {
                for (i in 0 until arr.length()) {
                    val item = arr.optJSONObject(i) ?: continue
                    val id = item.optString("id", item.optString("isrc", "")).trim()
                    val isrc = item.optString("isrc", id).trim()
                    val t = item.optString("title", item.optString("name", "")).trim()
                    val a = item.optString("artist", item.optString("artistName", "")).trim()
                    val al = item.optString("album", item.optString("albumName", "")).trim()
                    if (t.isNotEmpty() || isrc.isNotEmpty()) {
                        add(Entry(id = id, title = t, artist = a, album = al, isrc = isrc))
                    }
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    internal fun best(
        entries: List<Entry>,
        title: String,
        artist: String,
        album: String?,
    ): Entry? {
        if (entries.isEmpty()) return null
        val normTitle = normalize(title)
        val normArtist = normalize(artist)
        val normAlbum = album?.let(::normalize)?.takeIf { it.isNotEmpty() }

        var bestEntry: Entry? = null
        var bestScore = Int.MIN_VALUE

        for (entry in entries) {
            val eTitle = normalize(entry.title)
            val eArtist = normalize(entry.artist)
            val eAlbum = normalize(entry.album)
            if (eTitle.isEmpty()) continue

            var score = 0
            when {
                eTitle == normTitle -> score += 100
                eTitle.startsWith(normTitle) || normTitle.startsWith(eTitle) -> score += 65
                eTitle.contains(normTitle) || normTitle.contains(eTitle) -> score += 40
                else -> continue
            }

            if (normArtist.isNotEmpty()) {
                when {
                    eArtist == normArtist -> score += 80
                    eArtist.contains(normArtist) || normArtist.contains(eArtist) -> score += 50
                    else -> score -= 25
                }
            }

            if (normAlbum != null && eAlbum.isNotEmpty()) {
                if (eAlbum == normAlbum || eAlbum.contains(normAlbum) || normAlbum.contains(eAlbum)) {
                    score += 20
                }
            }

            if (score > bestScore) {
                bestScore = score
                bestEntry = entry
            }
        }
        return if (bestScore >= 40) bestEntry else null
    }

    private fun normalize(text: String): String {
        return text.forLyricsSearch()
            .lowercase()
            .replace(Regex("[^\\p{L}\\p{N}\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
