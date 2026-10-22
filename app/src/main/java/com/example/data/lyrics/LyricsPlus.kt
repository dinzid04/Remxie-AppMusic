package com.example.data.lyrics

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.selects.select
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.net.URLEncoder
import java.util.concurrent.atomic.AtomicReference

object LyricsPlus {
    private val SERVERS = listOf(
        "https://lyricsplus.prjktla.my.id",
        "https://lyricsplus.atomix.one",
        "https://lyricsplus.binimum.org",
        "https://lyricsplus.prjktla.workers.dev",
        "https://lyricsplus-seven.vercel.app",
        "https://lyrics-plus-backend.vercel.app",
    )

    private val lastGood = AtomicReference<String?>(null)

    suspend fun lyrics(
        title: String,
        artist: String,
        album: String? = null,
        durationMs: Long = 0L,
        isrc: String? = null,
    ): List<LyricLine>? = coroutineScope {
        val cleanTitle = title.forLyricsSearch()
        val cleanArtist = artist.artistForLyricsSearch()
        if (cleanTitle.isEmpty()) return@coroutineScope null

        val query = buildString {
            append("title=").append(URLEncoder.encode(cleanTitle, "UTF-8"))
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

        val preferred = lastGood.get()
        if (preferred != null) {
            fetchFromBase(preferred, query)?.let { return@coroutineScope it }
        }

        val serversToRace = SERVERS.filter { it != preferred }
        val jobs = serversToRace.map { base ->
            async {
                val res = fetchFromBase(base, query)
                if (res != null) base to res else null
            }
        }

        val pending = jobs.toMutableList()
        var winner: List<LyricLine>? = null
        while (pending.isNotEmpty() && winner == null) {
            val finished = select<Pair< kotlinx.coroutines.Deferred<Pair<String, List<LyricLine>>?>, Pair<String, List<LyricLine>>? >> {
                for (job in pending) {
                    job.onAwait { res -> job to res }
                }
            }
            pending.remove(finished.first)
            val pair = finished.second
            if (pair != null && pair.second.isNotEmpty()) {
                lastGood.set(pair.first)
                winner = pair.second
            }
        }
        pending.forEach { it.cancel() }
        winner
    }

    private suspend fun fetchFromBase(base: String, query: String): List<LyricLine>? {
        val raw = lyricsGet("$base/v2/lyrics/get?$query")
            ?: lyricsGet("$base/v1/lyrics/get?$query")
            ?: return null
        return parseResponse(raw)
    }

    internal fun parseResponse(raw: String): List<LyricLine>? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        return try {
            val root = JSONTokener(trimmed).nextValue()
            if (root is JSONObject) {
                val lyricsArr = root.optJSONArray("lyrics")
                    ?: root.optJSONObject("data")?.optJSONArray("lyrics")
                if (lyricsArr != null && lyricsArr.length() > 0) {
                    val lines = ArrayList<LyricLine>(lyricsArr.length())
                    val singers = ArrayList<String?>(lyricsArr.length())
                    for (i in 0 until lyricsArr.length()) {
                        val lineObj = lyricsArr.optJSONObject(i) ?: continue
                        val timeMs = lineObj.optLong("time", lineObj.optLong("startTimeMs", 0L))
                        val durMs = lineObj.optLong("duration", 0L)
                        val endMs = lineObj.optLong("endTimeMs", if (durMs > 0L) timeMs + durMs else -1L).takeIf { it > 0L }
                        val text = lineObj.optString("text", "").trim()
                        val elementObj = lineObj.optJSONObject("element")
                        val singer = elementObj?.optString("singer", "")?.ifBlank { null }
                        singers.add(singer)

                        val syllabusArr = lineObj.optJSONArray("syllabus")
                            ?: lineObj.optJSONArray("syllables")
                            ?: lineObj.optJSONArray("words")
                        val runs = ArrayList<TimedRun>()
                        val bgRuns = ArrayList<TimedRun>()

                        if (syllabusArr != null && syllabusArr.length() > 0) {
                            for (j in 0 until syllabusArr.length()) {
                                val sObj = syllabusArr.optJSONObject(j) ?: continue
                                val sText = sObj.optString("text", "")
                                if (sText.isEmpty()) continue
                                val sTime = sObj.optLong("time", timeMs)
                                val sDur = sObj.optLong("duration", 280L).coerceAtLeast(40L)
                                val isBg = sObj.optBoolean("isBackground", false)
                                val run = TimedRun(sTime, sTime + sDur, sText)
                                if (isBg) bgRuns.add(run) else runs.add(run)
                            }
                        }

                        val words = wordsFromRuns(runs, spacingIsExplicit = true)
                        val finalLineText = if (words.isNotEmpty()) {
                            words.joinToString(" ") { it.text }
                        } else {
                            text
                        }
                        if (finalLineText.isEmpty() || isGeniusSectionHeader(finalLineText)) continue

                        val bgWords = wordsFromRuns(bgRuns, spacingIsExplicit = true)
                        val bgLine = if (bgWords.isNotEmpty()) {
                            val cleanBgWords = bgWords.mapNotNull { w ->
                                w.withoutChars { it == '(' || it == ')' }
                            }
                            LyricLine(
                                timeMs = cleanBgWords.firstOrNull()?.startMs ?: timeMs,
                                text = cleanBgWords.joinToString(" ") { it.text },
                                words = cleanBgWords,
                                sungUntilMs = cleanBgWords.lastOrNull()?.endMs ?: endMs,
                            )
                        } else null

                        lines.add(
                            LyricLine(
                                timeMs = words.firstOrNull()?.startMs ?: timeMs,
                                text = finalLineText,
                                words = words,
                                sungUntilMs = endMs ?: words.lastOrNull()?.endMs,
                                background = bgLine,
                            )
                        )
                    }
                    if (lines.isNotEmpty()) {
                        val alignments = lineAlignments(singers)
                        return lines.mapIndexed { idx, l ->
                            val align = alignments.getOrElse(idx) { LyricAlignment.Start }
                            l.copy(alignment = align, background = l.background?.copy(alignment = align))
                        }
                            .sortedBy { it.timeMs }
                            .withBackgroundVocals()
                            .withInstrumentalGaps()
                    }
                }
            }
            ProviderLyrics.parse(trimmed)
        } catch (_: Exception) {
            ProviderLyrics.parse(trimmed)
        }
    }
}
