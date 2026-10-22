package com.example.data.lyrics

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

internal const val LYRICS_AGENT = "BitChord (https://github.com/bitchord)"

internal val lyricsHttpClient: OkHttpClient by lazy {
    OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()
}

internal suspend fun lyricsGet(
    url: String,
    userAgent: String = LYRICS_AGENT,
    extraHeaders: Map<String, String> = emptyMap(),
): String? = withContext(Dispatchers.IO) {
    try {
        val builder = Request.Builder()
            .url(url)
            .header("User-Agent", userAgent)
            .header("Accept", "application/json, text/plain, application/xml, text/xml, */*")
        for ((k, v) in extraHeaders) {
            builder.header(k, v)
        }
        lyricsHttpClient.newCall(builder.build()).execute().use { res ->
            if (!res.isSuccessful) return@use null
            res.body?.string()?.takeIf { it.isNotBlank() }
        }
    } catch (_: Exception) {
        null
    }
}

internal suspend fun lyricsGetBearer(
    url: String,
    bearer: String,
    extraHeaders: Map<String, String> = emptyMap(),
): String? = lyricsGet(
    url = url,
    extraHeaders = extraHeaders + mapOf("Authorization" to "Bearer $bearer"),
)

internal suspend fun lyricsGetAuthorized(
    url: String,
    bearer: String,
): String? {
    val key = bearer.trim()
    if (key.isEmpty()) return null
    return lyricsGetBearer(
        url = url,
        bearer = key,
        extraHeaders = mapOf("X-Api-Key" to key),
    )
}

internal suspend fun lyricsPostJson(
    url: String,
    jsonBody: String,
    userAgent: String = LYRICS_AGENT,
    extraHeaders: Map<String, String> = emptyMap(),
): String? = withContext(Dispatchers.IO) {
    try {
        val reqBody = jsonBody.toRequestBody("application/json; charset=utf-8".toMediaType())
        val builder = Request.Builder()
            .url(url)
            .post(reqBody)
            .header("User-Agent", userAgent)
            .header("Content-Type", "application/json")
        for ((k, v) in extraHeaders) {
            builder.header(k, v)
        }
        lyricsHttpClient.newCall(builder.build()).execute().use { res ->
            if (!res.isSuccessful) return@use null
            res.body?.string()?.takeIf { it.isNotBlank() }
        }
    } catch (_: Exception) {
        null
    }
}
