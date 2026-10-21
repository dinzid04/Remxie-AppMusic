package com.example

import com.example.data.model.MusicItem
import com.example.data.remote.CuratedHomeFallback
import com.example.data.remote.LrcRedService
import com.example.data.remote.PlaylistIntegrationService
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LrcAndIntegrationTest {

    @Test
    fun lrcRedService_returnsExactSyncedLyricsForTehHijauAndSakura() = runBlocking {
        val lrcService = LrcRedService()

        val tehHijauTrack = MusicItem(
            id = "teh_hijau_tulus",
            title = "Teh Hijau",
            subtitle = "Tulus",
            thumbnailUrl = "https://lrc.red/s/FRX282637095.webp"
        )
        val tehHijauLyrics = lrcService.fetchSyncedLyrics(tehHijauTrack)
        assertNotNull(tehHijauLyrics)
        assertTrue(tehHijauLyrics!!.syncedLines.isNotEmpty())
        assertEquals("Hari-hari berulang", tehHijauLyrics.syncedLines.first().text)

        val sakuraTrack = MusicItem(
            id = "sakura_rossa",
            title = "Sakura",
            subtitle = "Rossa",
            thumbnailUrl = "https://lrc.red/s/IDA170321018.webp"
        )
        val sakuraLyrics = lrcService.fetchSyncedLyrics(sakuraTrack)
        assertNotNull(sakuraLyrics)
        assertTrue(sakuraLyrics!!.syncedLines.isNotEmpty())
        assertTrue(sakuraLyrics.syncedLines.first().text.contains("Senada cinta bersemi", ignoreCase = true))
    }

    @Test
    fun artistProfileFallback_providesRichProfileAndRelatedArtists() {
        val profile = CuratedHomeFallback.getArtistProfileFallback("Rossa")
        assertEquals("Rossa", profile.name)
        assertTrue(profile.topSongs.isNotEmpty())
        assertTrue(profile.albums.isNotEmpty())
        assertTrue(profile.relatedArtists.isNotEmpty())
    }

    @Test
    fun playlistIntegrationService_connectsSpotifyAndYouTubeMusic() = runBlocking {
        val service = PlaylistIntegrationService()
        val spotifyRes = service.connectAndImportSpotify("Tester Spotify", "")
        assertTrue(spotifyRes.account.isConnected)
        assertTrue(spotifyRes.importedPlaylists.isNotEmpty())

        val ytmRes = service.connectAndImportYouTubeMusic("Tester YTM", "")
        assertTrue(ytmRes.account.isConnected)
        assertTrue(ytmRes.importedPlaylists.isNotEmpty())
    }
}
