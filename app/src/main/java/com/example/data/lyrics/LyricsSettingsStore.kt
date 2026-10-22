package com.example.data.lyrics

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class LyricsSettingsState(
    val syncedLyrics: Boolean = true,
    val enabledSources: Set<LyricsSource> = LyricsSource.defaultEnabled,
    val sourceOrder: List<LyricsSource> = LyricsSource.offered,
    val prioritizeSyllableSync: Boolean = true,
    val paxSenixApiKey: String = "",
    val trackOffsetsMs: Map<String, Int> = emptyMap(),
    val globalOffsetMs: Int = 0,
    val translationLanguage: String = "id",
    val lyricsBlur: Boolean = false,
    val reduceAnimation: Boolean = false,
) {
    fun offsetForTrack(trackId: String?): Int {
        if (trackId.isNullOrBlank()) return globalOffsetMs
        return trackOffsetsMs[trackId] ?: globalOffsetMs
    }
}

class LyricsSettingsStore private constructor(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("bitchord_lyrics_settings", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(loadInitialState())
    val state: StateFlow<LyricsSettingsState> = _state.asStateFlow()

    private fun loadInitialState(): LyricsSettingsState {
        val synced = prefs.getBoolean(KEY_SYNCED_LYRICS, true)
        val savedOrderStr = prefs.getString(KEY_SOURCE_ORDER, null)
        val orderList = if (!savedOrderStr.isNullOrBlank()) {
            LyricsSource.ordered(savedOrderStr.split(",").map { it.trim() }.filter { it.isNotEmpty() })
        } else {
            LyricsSource.offered
        }

        val savedEnabledStr = prefs.getString(KEY_ENABLED_SOURCES, null)
        val enabledSet = if (savedEnabledStr != null) {
            val names = savedEnabledStr.split(",").map { it.trim() }.toSet()
            LyricsSource.offered.filter { it.name in names }.toSet().ifEmpty { setOf(LyricsSource.LRC_RED) }
        } else {
            LyricsSource.defaultEnabled
        }

        val prioritizeSyllable = prefs.getBoolean(KEY_PRIORITIZE_SYLLABLE, true)
        val apiKey = prefs.getString(KEY_PAXSENIX_API_KEY, "") ?: ""
        val globalOffset = prefs.getInt(KEY_GLOBAL_OFFSET_MS, 0).coerceIn(-5000, 5000)
        val transLang = prefs.getString(KEY_TRANSLATION_LANGUAGE, "id") ?: "id"
        val blur = prefs.getBoolean(KEY_LYRICS_BLUR, false)
        val reduceAnim = prefs.getBoolean(KEY_REDUCE_ANIMATION, false)

        val offsetsMap = LinkedHashMap<String, Int>()
        val rawOffsets = prefs.getString(KEY_TRACK_OFFSETS, "").orEmpty()
        if (rawOffsets.isNotBlank()) {
            for (entry in rawOffsets.split(";")) {
                val eq = entry.indexOf('=')
                if (eq > 0) {
                    val k = entry.substring(0, eq)
                    val v = entry.substring(eq + 1).toIntOrNull()
                    if (k.isNotEmpty() && v != null) {
                        offsetsMap[k] = v.coerceIn(-5000, 5000)
                    }
                }
            }
        }

        return LyricsSettingsState(
            syncedLyrics = synced,
            enabledSources = enabledSet,
            sourceOrder = orderList,
            prioritizeSyllableSync = prioritizeSyllable,
            paxSenixApiKey = apiKey,
            trackOffsetsMs = offsetsMap,
            globalOffsetMs = globalOffset,
            translationLanguage = transLang,
            lyricsBlur = blur,
            reduceAnimation = reduceAnim,
        )
    }

    fun setSyncedLyrics(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SYNCED_LYRICS, enabled).apply()
        _state.update { it.copy(syncedLyrics = enabled) }
    }

    fun setEnabledSources(sources: Set<LyricsSource>) {
        val valid = sources.ifEmpty { setOf(LyricsSource.LRC_RED) }
        prefs.edit().putString(KEY_ENABLED_SOURCES, valid.joinToString(",") { it.name }).apply()
        _state.update { it.copy(enabledSources = valid) }
    }

    fun toggleSourceEnabled(source: LyricsSource) {
        val current = _state.value.enabledSources
        val next = if (source in current) {
            if (current.size > 1) current - source else current
        } else {
            current + source
        }
        setEnabledSources(next)
    }

    fun setSourceOrder(order: List<LyricsSource>) {
        val normalized = LyricsSource.ordered(order.map { it.name })
        prefs.edit().putString(KEY_SOURCE_ORDER, normalized.joinToString(",") { it.name }).apply()
        _state.update { it.copy(sourceOrder = normalized) }
    }

    fun setPrioritizeSyllableSync(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_PRIORITIZE_SYLLABLE, enabled).apply()
        _state.update { it.copy(prioritizeSyllableSync = enabled) }
    }

    fun setPaxSenixApiKey(key: String) {
        val norm = normalizePaxSenixApiKey(key)
        prefs.edit().putString(KEY_PAXSENIX_API_KEY, norm).apply()
        _state.update { it.copy(paxSenixApiKey = norm) }
    }

    fun setTrackOffsetMs(trackId: String?, offsetMs: Int) {
        val clamped = offsetMs.coerceIn(-5000, 5000)
        if (trackId.isNullOrBlank()) {
            prefs.edit().putInt(KEY_GLOBAL_OFFSET_MS, clamped).apply()
            _state.update { it.copy(globalOffsetMs = clamped) }
            return
        }
        val nextMap = LinkedHashMap(_state.value.trackOffsetsMs)
        if (clamped == 0) {
            nextMap.remove(trackId)
        } else {
            nextMap[trackId] = clamped
        }
        val serialized = nextMap.entries.take(200).joinToString(";") { "${it.key}=${it.value}" }
        prefs.edit()
            .putString(KEY_TRACK_OFFSETS, serialized)
            .putInt(KEY_GLOBAL_OFFSET_MS, clamped)
            .apply()
        _state.update { it.copy(trackOffsetsMs = nextMap, globalOffsetMs = clamped) }
    }

    fun setTranslationLanguage(code: String) {
        val clean = code.trim().ifEmpty { "id" }
        prefs.edit().putString(KEY_TRANSLATION_LANGUAGE, clean).apply()
        _state.update { it.copy(translationLanguage = clean) }
    }

    fun setLyricsBlur(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_LYRICS_BLUR, enabled).apply()
        _state.update { it.copy(lyricsBlur = enabled) }
    }

    fun setReduceAnimation(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_REDUCE_ANIMATION, enabled).apply()
        _state.update { it.copy(reduceAnimation = enabled) }
    }

    fun resetSourcesToDefault() {
        prefs.edit()
            .remove(KEY_SOURCE_ORDER)
            .remove(KEY_ENABLED_SOURCES)
            .putBoolean(KEY_PRIORITIZE_SYLLABLE, true)
            .apply()
        _state.update {
            it.copy(
                enabledSources = LyricsSource.defaultEnabled,
                sourceOrder = LyricsSource.offered,
                prioritizeSyllableSync = true,
            )
        }
    }

    companion object {
        private const val KEY_SYNCED_LYRICS = "synced_lyrics"
        private const val KEY_SOURCE_ORDER = "lyrics_source_order"
        private const val KEY_ENABLED_SOURCES = "lyrics_enabled_sources"
        private const val KEY_PRIORITIZE_SYLLABLE = "prioritize_syllable_sync"
        private const val KEY_PAXSENIX_API_KEY = "paxsenix_api_key"
        private const val KEY_GLOBAL_OFFSET_MS = "lyrics_global_offset_ms"
        private const val KEY_TRACK_OFFSETS = "lyrics_track_offsets"
        private const val KEY_TRANSLATION_LANGUAGE = "lyrics_translation_language"
        private const val KEY_LYRICS_BLUR = "lyrics_blur"
        private const val KEY_REDUCE_ANIMATION = "lyrics_reduce_animation"

        @Volatile
        private var INSTANCE: LyricsSettingsStore? = null

        fun getInstance(context: Context): LyricsSettingsStore {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LyricsSettingsStore(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
