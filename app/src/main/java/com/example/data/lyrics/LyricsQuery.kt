package com.example.data.lyrics

private val BRACKET_NOISE_REGEX = Regex(
    """\s*[\[(](?:[^)\]]*?\b(?:official|lyric|lyrics|video|audio|visualizer|mv|m/v|hd|hq|4k|remaster|remastered|live|version|ver\.|edit|feat\.?|ft\.?|with|prod\.?|explicit|clean|bonus|deluxe|ost|soundtrack|karaoke|instrumental|topic)\b[^)\]]*?)[)\]]""",
    RegexOption.IGNORE_CASE,
)
private val FEAT_TAIL_REGEX = Regex(
    """\s+(?:feat\.?|ft\.?|featuring|with)\s+.*$""",
    RegexOption.IGNORE_CASE,
)
private val DASH_NOISE_TAIL_REGEX = Regex(
    """\s+-\s+(?:official\s+.*|lyric\s+.*|audio.*|video.*|remastered.*|live.*)$""",
    RegexOption.IGNORE_CASE,
)

fun String.forLyricsSearch(): String {
    var s = this
        .replace(BRACKET_NOISE_REGEX, "")
        .replace(FEAT_TAIL_REGEX, "")
        .replace(DASH_NOISE_TAIL_REGEX, "")
        .replace(Regex("\\s+"), " ")
        .trim()
    if (s.isEmpty()) s = this.trim()
    return s
}

fun String.artistForLyricsSearch(): String {
    val clean = this
        .replace(Regex("""\s*-\s*Topic$""", RegexOption.IGNORE_CASE), "")
        .replace(Regex("""\bVEVO$""", RegexOption.IGNORE_CASE), "")
        .split(Regex("""\s*(?:,|&|/|;|\bx\b|\bfeat\.?\b|\bft\.?\b|\bwith\b)\s*""", RegexOption.IGNORE_CASE))
        .firstOrNull()
        ?.trim()
        .orEmpty()
    return clean.ifEmpty { this.trim() }
}
