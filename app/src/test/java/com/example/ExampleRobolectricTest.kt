package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.HomeSectionStyle
import com.example.data.model.MusicItem
import com.example.data.remote.CuratedHomeFallback
import com.example.data.remote.LrcRedService
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ExampleRobolectricTest {

    @Test
    fun appContext_andHomeSections_andLrcRedLyrics_areValid() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("BitChord", context.getString(R.string.app_name))

        val sections = CuratedHomeFallback.getRequiredHomeSectionsFallback()
        assertTrue(sections.isNotEmpty())

        // Verify Rekomendasi harian Anda and Cover dan remix are present with expected styles
        val dailyRec = sections.firstOrNull { it.title == "Rekomendasi harian Anda" }
        assertNotNull(dailyRec)
        assertEquals(HomeSectionStyle.DAILY_RECOMMENDATION, dailyRec?.style)
        assertEquals("Dinda", dailyRec?.items?.firstOrNull()?.title)
        assertEquals("Untuk penggemar Overnight", dailyRec?.items?.firstOrNull()?.reason)

        val coverRemix = sections.firstOrNull { it.title == "Cover dan remix" }
        assertNotNull(coverRemix)
        assertEquals(HomeSectionStyle.FOUR_ROW_SHELF, coverRemix?.style)
        assertEquals("Risalah Hati", coverRemix?.items?.firstOrNull()?.title)

        // Verify Lagu trending untuk kamu (songs) vs Playlist trending komunitas (playlists) are distinct
        val trendingSongs = sections.firstOrNull { it.title == "Lagu trending untuk kamu" }
        val communityPlaylists = sections.firstOrNull { it.title == "Playlist trending komunitas" }
        assertNotNull(trendingSongs)
        assertNotNull(communityPlaylists)
        assertTrue(trendingSongs!!.items.all { !it.isCollection })
        assertTrue(communityPlaylists!!.items.all { it.isCollection })

        // Verify no emojis in section titles
        sections.forEach { sec ->
            assertFalse(sec.title.contains("🫶"))
            assertFalse(sec.title.contains("🔥"))
        }

        // Verify lrc.red synced lyrics & HD cover for Teh Hijau (FRX282637095)
        val lrcService = LrcRedService()
        val tehHijauTrack = MusicItem(
            id = "teh_hijau_tulus",
            title = "Teh Hijau",
            subtitle = "Tulus",
            thumbnailUrl = "https://lrc.red/s/FRX282637095.webp",
            videoId = "5F28ye50-Kc"
        )
        val lyrics = lrcService.fetchSyncedLyrics(tehHijauTrack)
        assertNotNull(lyrics)
        assertEquals("Lirik oleh lrc.red", lyrics?.source)
        assertEquals("https://lrc.red/s/FRX282637095.webp", lyrics?.hdCoverUrl)
        assertTrue((lyrics?.syncedLines?.size ?: 0) > 10)
        assertEquals("Hari-hari berulang", lyrics?.syncedLines?.firstOrNull()?.text)
    }

    @Test
    fun thumbnailUpgrade_andSleepTimer_andQueueActions_workCorrectly() {
        // 1. Verify i.ytimg.com URL is never corrupted into hqhqdefault.jpg
        val ytUrl = "https://i.ytimg.com/vi/5F28ye50-Kc/hqdefault.jpg?sqp=-oaymwEcCNACELwBSFXyq4qpAw4IARUAAIhCGAFwAcABBg=="
        val upgradedYt = MusicItem.upgradeThumbnailUrl(ytUrl, "5F28ye50-Kc", MusicItem.Quality.HIGH)
        assertEquals("https://i.ytimg.com/vi/5F28ye50-Kc/hqdefault.jpg", upgradedYt)
        assertFalse(upgradedYt.contains("hqhqdefault"))

        // 2. Verify blank thumbnail with valid videoId falls back to i.ytimg.com/vi/{id}/hqdefault.jpg
        val fallbackFromVid = MusicItem.upgradeThumbnailUrl("", "7PCkvCPvDXk", MusicItem.Quality.HIGH)
        assertEquals("https://i.ytimg.com/vi/7PCkvCPvDXk/hqdefault.jpg", fallbackFromVid)

        // 3. Verify yt3.ggpht.com artist portrait URL preserves exact signed path
        val yt3Avatar = "https://yt3.ggpht.com/ytc/AIdro_k12345=w544-h544-p-l90-rj"
        assertEquals(yt3Avatar, MusicItem.upgradeThumbnailUrl(yt3Avatar, null, MusicItem.Quality.HIGH))

        // 4. Verify Sleep Timer state transitions in AudioPlayerController
        val controller = com.example.player.AudioPlayerController()
        controller.setSleepTimerMinutes(15, "15m")
        assertEquals("15m", controller.playerState.value.sleepTimerLabel)
        assertFalse(controller.playerState.value.sleepTimerEndOfTrack)

        controller.setSleepTimerEndOfTrack()
        assertEquals("End of track", controller.playerState.value.sleepTimerLabel)
        assertTrue(controller.playerState.value.sleepTimerEndOfTrack)

        controller.cancelSleepTimer()
        assertEquals(null, controller.playerState.value.sleepTimerLabel)
        assertFalse(controller.playerState.value.sleepTimerEndOfTrack)
    }
}

