package com.example.data.lyrics

import androidx.compose.runtime.Immutable
import kotlin.math.cos

@Immutable
data class LyricSyllable(
    val startMs: Long,
    val endMs: Long,
    val startChar: Int,
    val endChar: Int,
) {
    companion object {
        fun normalized(parts: List<LyricSyllable>, length: Int): List<LyricSyllable> {
            val valid = parts.mapNotNull { part ->
                val start = part.startChar.coerceIn(0, length)
                val end = part.endChar.coerceIn(start, length)
                val endMs = part.endMs.coerceAtLeast(part.startMs)
                if (end > start) {
                    LyricSyllable(startMs = part.startMs, endMs = endMs, startChar = start, endChar = end)
                } else {
                    null
                }
            }
            if (valid.size <= 1) return emptyList()
            for (index in 1 until valid.size) {
                if (valid[index].startChar != valid[index - 1].endChar) return emptyList()
                if (valid[index].startMs < valid[index - 1].startMs) return emptyList()
            }
            if (valid.first().startChar != 0 || valid.last().endChar != length) return emptyList()
            return valid
        }
    }
}

@Immutable
data class LyricWord(
    val startMs: Long,
    val endMs: Long,
    val text: String,
    val syllables: List<LyricSyllable> = emptyList(),
) {
    fun charsSungAt(positionMs: Long): Float {
        if (text.isEmpty()) return 0f
        if (positionMs >= endMs) return text.length.toFloat()
        if (positionMs < startMs) return 0f
        val runs = syllables
        if (runs.isEmpty()) {
            val span = (endMs - startMs).coerceAtLeast(1L).toFloat()
            return ((positionMs - startMs).toFloat() / span).coerceIn(0f, 1f) * text.length
        }
        for (syllable in runs) {
            if (positionMs < syllable.startMs) return syllable.startChar.toFloat()
            if (positionMs < syllable.endMs) {
                val span = (syllable.endMs - syllable.startMs).coerceAtLeast(1L).toFloat()
                val progress = ((positionMs - syllable.startMs).toFloat() / span).coerceIn(0f, 1f)
                return syllable.startChar + progress * (syllable.endChar - syllable.startChar)
            }
        }
        return text.length.toFloat()
    }

    internal fun withoutChars(drop: (Char) -> Boolean): LyricWord? {
        if (text.none(drop)) return this
        val out = StringBuilder(text.length)
        val indexMap = IntArray(text.length + 1)
        for (i in text.indices) {
            indexMap[i] = out.length
            val ch = text[i]
            if (!drop(ch)) out.append(ch)
        }
        indexMap[text.length] = out.length
        val stripped = out.toString()
        if (stripped.isEmpty()) return null
        val remapped = if (syllables.isEmpty()) {
            emptyList()
        } else {
            LyricSyllable.normalized(
                syllables.map { s ->
                    s.copy(startChar = indexMap[s.startChar], endChar = indexMap[s.endChar])
                },
                stripped.length,
            )
        }
        return copy(text = stripped, syllables = remapped)
    }
}

enum class LyricAlignment { Start, End }

private const val LIFT_RISE_MS = 240f
private const val LIFT_HOLD_FLOOR = 0.25f
private const val LIFT_FALL_MS = 500f
private const val FALL_RELEASE_MS = 280f
private const val HOLD_GROW_THRESHOLD_MS = 1_100L
private const val HOLD_GROW_MIN_LETTERS = 2
private const val CHAR_GROW_LEAD_MS = 100f
private const val CHAR_GROW_RISE_MIN_MS = 160f
private const val CHAR_GROW_RISE_MAX_MS = 380f
private const val CHAR_GROW_PEAK_HOLD_FRAC = 0.14f
private const val CHAR_GROW_SETTLE_MS = 300f

class GrowingWord(
    val index: Int,
    word: LyricWord,
) {
    val startMs: Long = word.startMs
    val endMs: Long = word.endMs
    private val count: Int = word.text.length
    private val starts = FloatArray(count)
    private val ends = FloatArray(count)
    private val peakEnds = FloatArray(count)
    private val settleEnds = FloatArray(count)
    val finishedAtMs: Long

    init {
        val syllables = word.syllables
        for (charIndex in 0 until count) {
            var charStart = startMs.toFloat()
            var charEnd = endMs.toFloat()
            if (syllables.isNotEmpty()) {
                for (s in syllables) {
                    if (charIndex in s.startChar until s.endChar) {
                        val span = (s.endChar - s.startChar).coerceAtLeast(1)
                        val dur = (s.endMs - s.startMs).coerceAtLeast(1L).toFloat()
                        charStart = s.startMs + dur * (charIndex - s.startChar) / span
                        charEnd = s.startMs + dur * (charIndex + 1 - s.startChar) / span
                        break
                    }
                }
            } else {
                val span = count.coerceAtLeast(1)
                val dur = (endMs - startMs).coerceAtLeast(1L).toFloat()
                charStart = startMs + dur * charIndex / span
                charEnd = startMs + dur * (charIndex + 1) / span
            }
            val sweepDur = (charEnd - charStart).coerceAtLeast(1f)
            val animStart = charStart - CHAR_GROW_LEAD_MS
            val riseDur = (sweepDur * 0.9f).coerceIn(CHAR_GROW_RISE_MIN_MS, CHAR_GROW_RISE_MAX_MS)
            val animEnd = maxOf(charEnd, animStart + riseDur)
            val holdExtra = ((endMs - animEnd).coerceAtLeast(0f) * CHAR_GROW_PEAK_HOLD_FRAC)
            val peakEnd = animEnd + holdExtra
            val settleEnd = maxOf(endMs.toFloat(), peakEnd + CHAR_GROW_SETTLE_MS)
            starts[charIndex] = animStart
            ends[charIndex] = animEnd
            peakEnds[charIndex] = peakEnd
            settleEnds[charIndex] = settleEnd
        }
        finishedAtMs = (settleEnds.maxOrNull()?.toLong() ?: endMs)
    }

    fun sampleInto(positionMs: Long, scaleOut: FloatArray, liftOut: FloatArray, glowOut: FloatArray): Boolean {
        val pos = positionMs.toFloat()
        var anyActive = false
        for (i in 0 until count) {
            val s = starts[i]
            val e = ends[i]
            val p = peakEnds[i]
            val f = settleEnds[i]
            when {
                pos <= s || pos >= f -> {
                    scaleOut[i] = 0f
                    liftOut[i] = 0f
                    glowOut[i] = 0f
                }
                pos < e -> {
                    val t = ((pos - s) / (e - s).coerceAtLeast(1f)).coerceIn(0f, 1f)
                    val smooth = (1f - cos(t * Math.PI.toFloat())) * 0.5f
                    scaleOut[i] = smooth
                    liftOut[i] = smooth
                    glowOut[i] = smooth
                    anyActive = true
                }
                pos < p -> {
                    scaleOut[i] = 1f
                    liftOut[i] = 1f
                    glowOut[i] = 1f
                    anyActive = true
                }
                else -> {
                    val t = ((pos - p) / (f - p).coerceAtLeast(1f)).coerceIn(0f, 1f)
                    val fall = (1f + cos(t * Math.PI.toFloat())) * 0.5f
                    scaleOut[i] = fall
                    liftOut[i] = fall
                    glowOut[i] = fall * fall
                    if (fall > 0.005f) anyActive = true
                }
            }
        }
        return anyActive
    }
}

class CharGrowth(
    val wordIndex: Int,
    val scales: FloatArray,
    val lifts: FloatArray,
    val glows: FloatArray,
    val progress: Float,
)

@Immutable
data class LyricLine(
    val timeMs: Long,
    val text: String,
    val words: List<LyricWord> = emptyList(),
    val sungUntilMs: Long? = null,
    val background: LyricLine? = null,
    val alignment: LyricAlignment = LyricAlignment.Start,
    val timingSource: LyricLine? = null,
) {
    val isGap: Boolean get() = text.isEmpty()
    val isWordSynced: Boolean get() = words.isNotEmpty() || timingSource?.words?.isNotEmpty() == true

    private val timing: LyricLine get() = timingSource ?: this

    val wordSpans: IntArray by lazy(LazyThreadSafetyMode.NONE) {
        if (words.isEmpty()) IntArray(0)
        else {
            val out = IntArray(words.size * 2)
            var cursor = 0
            for (index in words.indices) {
                if (index > 0) cursor += 1
                val start = cursor
                cursor += words[index].text.length
                out[index * 2] = start
                out[index * 2 + 1] = cursor
            }
            out
        }
    }

    val sweepSpans: IntArray by lazy(LazyThreadSafetyMode.NONE) {
        if (timingSource != null && words.isEmpty() && timing.words.isNotEmpty()) {
            val count = timing.words.size
            val len = text.length
            val out = IntArray(count * 2)
            for (index in 0 until count) {
                val start = len * index / count
                val end = if (index == count - 1) len else len * (index + 1) / count
                out[index * 2] = start
                out[index * 2 + 1] = end.coerceAtLeast(start)
            }
            out
        } else {
            wordSpans
        }
    }

    val isLifted: Boolean get() = words.isNotEmpty()

    val animatesFromMs: Long by lazy(LazyThreadSafetyMode.NONE) {
        val w = timing.words
        if (w.isEmpty()) timeMs
        else minOf(timeMs, w.first().startMs - CHAR_GROW_LEAD_MS.toLong())
    }

    val animatesUntilMs: Long by lazy(LazyThreadSafetyMode.NONE) {
        val w = timing.words
        if (w.isEmpty()) timeMs
        else {
            val lastWordEnd = w.maxOf { it.endMs }
            val liftTail = lastWordEnd + LIFT_FALL_MS.toLong()
            val fallTail = (sungUntilMs ?: lastWordEnd) + FALL_RELEASE_MS.toLong()
            val growTail = growingWords.maxOfOrNull { it.finishedAtMs } ?: 0L
            maxOf(liftTail, fallTail, growTail)
        }
    }

    fun wordLift(word: LyricWord, positionMs: Long): Float {
        if (positionMs < word.startMs) return 0f
        val endMs = word.endMs.coerceAtLeast(word.startMs + 1L)
        if (positionMs <= endMs) {
            val elapsed = (positionMs - word.startMs).toFloat()
            val duration = (endMs - word.startMs).toFloat()
            val riseMs = minOf(LIFT_RISE_MS, duration)
            if (elapsed <= riseMs) {
                val t = (elapsed / riseMs).coerceIn(0f, 1f)
                return (1f - cos(t * Math.PI.toFloat())) * 0.5f
            }
            val holdSpan = (duration - riseMs).coerceAtLeast(1f)
            val holdT = ((elapsed - riseMs) / holdSpan).coerceIn(0f, 1f)
            val settle = (1f + cos(holdT * Math.PI.toFloat())) * 0.5f
            return LIFT_HOLD_FLOOR + (1f - LIFT_HOLD_FLOOR) * settle
        }
        val pastEnd = (positionMs - endMs).toFloat()
        if (pastEnd >= LIFT_FALL_MS) return 0f
        val endElapsed = (endMs - word.startMs).toFloat()
        val riseMs = minOf(LIFT_RISE_MS, endElapsed)
        val levelAtEnd = if (endElapsed <= riseMs) {
            1f
        } else {
            val holdSpan = (endElapsed - riseMs).coerceAtLeast(1f)
            val holdT = ((endElapsed - riseMs) / holdSpan).coerceIn(0f, 1f)
            val settle = (1f + cos(holdT * Math.PI.toFloat())) * 0.5f
            LIFT_HOLD_FLOOR + (1f - LIFT_HOLD_FLOOR) * settle
        }
        val t = (pastEnd / LIFT_FALL_MS).coerceIn(0f, 1f)
        return levelAtEnd * (1f + cos(t * Math.PI.toFloat())) * 0.5f
    }

    val hasKnownEnd: Boolean
        get() = timing.sungUntilMs != null || timing.words.isNotEmpty()

    fun endMs(nextLineMs: Long?): Long {
        val src = timing
        val explicit = src.sungUntilMs ?: src.words.lastOrNull()?.endMs
        if (explicit != null) {
            return if (nextLineMs != null && nextLineMs > src.timeMs) minOf(explicit, nextLineMs) else explicit
        }
        if (nextLineMs != null && nextLineMs > src.timeMs) return nextLineMs
        return src.timeMs + 4_000L
    }

    fun revealedChars(positionMs: Long): Float {
        if (text.isEmpty()) return 0f
        val src = timing
        if (src.words.isEmpty()) return text.length.toFloat()
        val spans = sweepSpans
        var revealed = 0f
        for (index in src.words.indices) {
            val word = src.words[index]
            val start = spans[index * 2]
            val end = spans[index * 2 + 1]
            val spanLen = (end - start).coerceAtLeast(0)
            when {
                positionMs >= word.endMs -> revealed = end.toFloat()
                positionMs > word.startMs -> {
                    val wordLen = word.text.length
                    val progress = if (wordLen > 0) {
                        (word.charsSungAt(positionMs) / wordLen).coerceIn(0f, 1f)
                    } else 0f
                    return start + progress * spanLen
                }
                else -> return revealed
            }
        }
        return text.length.toFloat()
    }

    fun wordFall(positionMs: Long): Float {
        val holdUntil = timing.sungUntilMs ?: timing.words.lastOrNull()?.endMs ?: return 0f
        if (positionMs <= holdUntil) return 0f
        val elapsed = (positionMs - holdUntil).toFloat()
        if (elapsed >= FALL_RELEASE_MS) return 1f
        val t = (elapsed / FALL_RELEASE_MS).coerceIn(0f, 1f)
        return (1f - cos(t * Math.PI.toFloat())) * 0.5f
    }

    val growingWords: List<GrowingWord> by lazy(LazyThreadSafetyMode.NONE) {
        if (words.isEmpty()) emptyList()
        else buildList {
            for (index in words.indices) {
                val word = words[index]
                if (word.endMs - word.startMs < HOLD_GROW_THRESHOLD_MS) continue
                val letters = word.text.count { it.isLetterOrDigit() }
                if (letters < HOLD_GROW_MIN_LETTERS) continue
                add(GrowingWord(index = index, word = word))
            }
        }
    }

    fun growingAt(positionMs: Long): List<CharGrowth> {
        val list = growingWords
        if (list.isEmpty()) return emptyList()
        var out: MutableList<CharGrowth>? = null
        for (gw in list) {
            if (positionMs < gw.startMs - CHAR_GROW_LEAD_MS.toLong() || positionMs >= gw.finishedAtMs) continue
            val len = words[gw.index].text.length
            val scales = FloatArray(len)
            val lifts = FloatArray(len)
            val glows = FloatArray(len)
            if (gw.sampleInto(positionMs, scales, lifts, glows)) {
                val span = (gw.endMs - gw.startMs).coerceAtLeast(1L).toFloat()
                val progress = ((positionMs - gw.startMs).toFloat() / span).coerceIn(0f, 1f)
                val target = out ?: ArrayList<CharGrowth>(2).also { out = it }
                target.add(CharGrowth(gw.index, scales, lifts, glows, progress))
            }
        }
        return out ?: emptyList()
    }

    val isGrowing: Boolean get() = growingWords.isNotEmpty()
}
