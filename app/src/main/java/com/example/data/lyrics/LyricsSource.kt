package com.example.data.lyrics

enum class LyricsProviderState {
    FETCHING,
    FOUND,
    NOT_FOUND,
    NOT_FETCHED,
}

enum class LyricsSource(
    val displayName: String,
    val subtitle: String,
    val hidden: Boolean = false,
) {
    LRC_RED(
        displayName = "Lrc.red",
        subtitle = "Apple Music TTML syllable-synced lyrics & duet alignment",
    ),
    BINI_LYRICS(
        displayName = "BiniLyrics",
        subtitle = "Syllable & word-synced lyrics with ISRC matching",
    ),
    BETTER_LYRICS(
        displayName = "Better Lyrics",
        subtitle = "Apple Music word-by-word TTML lyrics via Boidu API",
    ),
    BETTER_LYRICS_PORTATO(
        displayName = "Better Lyrics (Portato)",
        subtitle = "QQ Music syllable-synced lyrics via Boidu API",
    ),
    PAXSENIX(
        displayName = "PaxSenix (Apple Music)",
        subtitle = "Timed syllable & word-synced Apple Music lyrics",
    ),
    PAXSENIX_SPOTIFY(
        displayName = "PaxSenix (Spotify)",
        subtitle = "Spotify synced lyrics via PaxSenix API key",
    ),
    PAXSENIX_MUSIXMATCH(
        displayName = "PaxSenix (Musixmatch)",
        subtitle = "Musixmatch RichSync & subtitles via PaxSenix API key",
    ),
    LYRICS_PLUS(
        displayName = "LyricsPlus",
        subtitle = "Multi-mirror syllable-synced lyrics with background vocals",
    ),
    SIMP_MUSIC(
        displayName = "SimpMusic",
        subtitle = "YouTube videoId-matched synced lyrics",
        hidden = true,
    ),
    UNISON(
        displayName = "Unison",
        subtitle = "Word & line-synced lyrics via Unison API",
    ),
    YOUTUBE_TRANSCRIPT(
        displayName = "YouTube Transcript",
        subtitle = "Timed captions & subtitles from YouTube player",
    ),
    YOUTUBE_MUSIC(
        displayName = "YouTube Music",
        subtitle = "Official YouTube Music timed & plain lyrics",
    ),
    MEGALOBIZ(
        displayName = "Megalobiz",
        subtitle = "Community LRC database scraper",
        hidden = true,
    ),
    KUGOU(
        displayName = "KuGou",
        subtitle = "Large Asian & global LRC database",
    ),
    LRCLIB(
        displayName = "LRCLIB",
        subtitle = "Open-source synced & plain LRC database",
    ),
    MUSIXMATCH(
        displayName = "Musixmatch",
        subtitle = "Direct RichSync word-by-word & subtitle LRC",
    ),
    GENIUS(
        displayName = "Genius",
        subtitle = "Comprehensive plain lyrics & annotations fallback",
    );

    companion object {
        val offered: List<LyricsSource> = entries.filter { !it.hidden }

        val defaultEnabled: Set<LyricsSource> = offered.toSet()

        fun ordered(savedOrder: List<String>): List<LyricsSource> {
            if (savedOrder.isEmpty()) return offered
            val byName = offered.associateBy { it.name }
            val result = ArrayList<LyricsSource>(offered.size)
            for (name in savedOrder) {
                val src = byName[name]
                if (src != null && src !in result) {
                    result.add(src)
                }
            }
            for (src in offered) {
                if (src !in result) {
                    result.add(src)
                }
            }
            return result
        }
    }
}
