package com.example.data.lyrics

internal fun lineAlignments(
    singers: List<String?>,
    types: Map<String, String> = emptyMap(),
): List<LyricAlignment> {
    if (singers.isEmpty()) return emptyList()
    val distinct = singers.mapNotNull { it?.trim()?.takeIf(String::isNotEmpty) }.distinct()
    if (distinct.size <= 1) return List(singers.size) { LyricAlignment.Start }
    val primary = distinct.first()
    return singers.map { raw ->
        val id = raw?.trim()?.takeIf(String::isNotEmpty) ?: return@map LyricAlignment.Start
        if (id == primary) LyricAlignment.Start else LyricAlignment.End
    }
}
