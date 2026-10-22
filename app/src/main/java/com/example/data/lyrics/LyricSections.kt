package com.example.data.lyrics

private val SECTION_KEYWORDS = setOf(
    "intro", "outro", "verse", "chorus", "pre-chorus", "post-chorus",
    "bridge", "refrain", "hook", "interlude", "instrumental", "solo",
    "guitar solo", "piano solo", " sax solo", "drum solo", "break",
    "breakdown", "drop", "buildup", "build-up", "coda", "tag", "skit",
    "spoken", "ad-lib", "adlib", "part", "act", "scene", "stanza",
    "couplet", "estribillo", "verso", "coro", "puente", "couplet", "refrain",
)

internal fun isGeniusSectionHeader(text: String): Boolean {
    val trimmed = text.trim()
    if (trimmed.length < 3 || trimmed.length > 72) return false
    val inner = when {
        trimmed.startsWith('[') && trimmed.endsWith(']') -> trimmed.substring(1, trimmed.length - 1).trim()
        trimmed.startsWith('(') && trimmed.endsWith(')') -> return false
        else -> return false
    }
    if (inner.isEmpty()) return true
    val head = inner.lowercase()
        .substringBefore(':')
        .substringBefore('-')
        .substringBefore('(')
        .trim()
    if (head in SECTION_KEYWORDS) return true
    return SECTION_KEYWORDS.any { kw ->
        head.startsWith("$kw ") || head.endsWith(" $kw")
    }
}
