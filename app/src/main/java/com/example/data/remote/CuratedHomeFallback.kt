package com.example.data.remote

import com.example.data.model.ArtistProfileDetail
import com.example.data.model.HomeSection
import com.example.data.model.HomeSectionStyle
import com.example.data.model.LyricsData
import com.example.data.model.LyricsProvider
import com.example.data.model.MusicItem
import com.example.data.model.PlaylistDetail

object CuratedHomeFallback {

    fun getQuickPicksFallback(): List<MusicItem> = listOf(
        MusicItem(
            id = "rayuan_perempuan_gila_nadin",
            title = "Rayuan Perempuan Gila",
            subtitle = "Nadin Amizah • 136 jt pemutaran",
            thumbnailUrl = "https://i.ytimg.com/vi/kOkQ4T5WO9E/hqdefault.jpg",
            videoId = "kOkQ4T5WO9E",
            type = "song",
            duration = "5:20",
            playsText = "136 jt pemutaran"
        ),
        MusicItem(
            id = "mesin_waktu_budi_doremi",
            title = "Mesin Waktu",
            subtitle = "Budi Doremi • 341 jt pemutaran",
            thumbnailUrl = "https://i.ytimg.com/vi/YQHsXMglC9A/hqdefault.jpg",
            videoId = "YQHsXMglC9A",
            type = "song",
            duration = "4:21",
            playsText = "341 jt pemutaran"
        ),
        MusicItem(
            id = "yang_patah_tumbuh_banda_neira",
            title = "Yang Patah Tumbuh, Yang Hilang Berganti",
            subtitle = "Banda Neira • 77 jt pemutaran",
            thumbnailUrl = "https://i.ytimg.com/vi/bo_efYhYU2A/hqdefault.jpg",
            videoId = "bo_efYhYU2A",
            type = "song",
            duration = "6:25",
            playsText = "77 jt pemutaran"
        ),
        MusicItem(
            id = "suara_hati_seorang_kekasih_melly",
            title = "Suara Hati Seorang Kekasih",
            subtitle = "Melly • 10 jt pemutaran",
            thumbnailUrl = "https://i.ytimg.com/vi/lp-EO5I60KA/hqdefault.jpg",
            videoId = "lp-EO5I60KA",
            type = "song",
            duration = "4:12",
            playsText = "10 jt pemutaran"
        ),
        MusicItem(
            id = "sakura_rossa_pick",
            title = "Sakura",
            subtitle = "Rossa • 45 jt pemutaran",
            thumbnailUrl = "https://lrc.red/s/IDA170321018.webp",
            videoId = "7PCkvCPvDXk",
            type = "song",
            duration = "3:59",
            playsText = "45 jt pemutaran"
        ),
        MusicItem(
            id = "tak_kan_hilang_budi_doremi",
            title = "Tak Kan Hilang",
            subtitle = "Budi Doremi • 178 jt pemutaran",
            thumbnailUrl = "https://i.ytimg.com/vi/5F28ye50-Kc/hqdefault.jpg",
            videoId = "5F28ye50-Kc",
            type = "song",
            duration = "4:01",
            playsText = "178 jt pemutaran"
        ),
        MusicItem(
            id = "5F28ye50-Kc",
            title = "Resah Jadi Luka",
            subtitle = "Daun Jatuh • 142 jt pemutaran",
            thumbnailUrl = "https://i.ytimg.com/vi/5F28ye50-Kc/hqdefault.jpg",
            videoId = "5F28ye50-Kc",
            type = "song",
            duration = "4:16",
            playsText = "142 jt pemutaran"
        ),
        MusicItem(
            id = "bertaut_nadin_amizah",
            title = "Bertaut",
            subtitle = "Nadin Amizah • 195 jt pemutaran",
            thumbnailUrl = "https://i.ytimg.com/vi/SlPhMPnQ58k/hqdefault.jpg",
            videoId = "SlPhMPnQ58k",
            type = "song",
            duration = "5:16",
            playsText = "195 jt pemutaran"
        )
    )

    fun getDailyRecommendationsFallback(): List<MusicItem> = listOf(
        MusicItem(
            id = "dinda_masdo_daily",
            title = "Dinda",
            subtitle = "MASDO • 18 jt pemutaran",
            thumbnailUrl = "https://i.ytimg.com/vi/JGwWNGJdvx8/hqdefault.jpg",
            videoId = "JGwWNGJdvx8",
            type = "daily_recommendation_track",
            duration = "3:44",
            reason = "Untuk penggemar Overnight",
            playsText = "18 jt pemutaran"
        ),
        MusicItem(
            id = "sakura_rossa_daily",
            title = "Sakura",
            subtitle = "Rossa • 45 jt pemutaran",
            thumbnailUrl = "https://lrc.red/s/IDA170321018.webp",
            videoId = "7PCkvCPvDXk",
            type = "daily_recommendation_track",
            duration = "3:59",
            reason = "Berdasarkan lagu yang sering Anda putar",
            playsText = "45 jt pemutaran"
        ),
        MusicItem(
            id = "teh_hijau_tulus_daily",
            title = "Teh Hijau",
            subtitle = "Tulus • 12 jt pemutaran",
            thumbnailUrl = "https://lrc.red/s/FRX282637095.webp",
            videoId = "5F28ye50-Kc",
            type = "daily_recommendation_track",
            duration = "3:33",
            reason = "Untuk penggemar Nadin Amizah",
            playsText = "12 jt pemutaran"
        ),
        MusicItem(
            id = "satu_bulan_bernadya_daily",
            title = "Satu Bulan",
            subtitle = "Bernadya • 210 jt pemutaran",
            thumbnailUrl = "https://i.ytimg.com/vi/60ItHLz5WEA/hqdefault.jpg",
            videoId = "60ItHLz5WEA",
            type = "daily_recommendation_track",
            duration = "3:20",
            reason = "Untuk penggemar Sal Priadi",
            playsText = "210 jt pemutaran"
        ),
        MusicItem(
            id = "gala_bunga_matahari_daily",
            title = "Gala bunga matahari",
            subtitle = "Sal Priadi • 164 jt pemutaran",
            thumbnailUrl = "https://i.ytimg.com/vi/nfWlot6h_JM/hqdefault.jpg",
            videoId = "nfWlot6h_JM",
            type = "daily_recommendation_track",
            duration = "3:29",
            reason = "Untuk penggemar Hindia",
            playsText = "164 jt pemutaran"
        )
    )

    fun getCoverDanRemixFallback(): List<MusicItem> = listOf(
        MusicItem(
            id = "risalah_hati_dewa_yura",
            title = "Risalah Hati",
            subtitle = "Dewa 19 & Yura Yunita • 18 jt pemutaran",
            thumbnailUrl = "https://i.ytimg.com/vi/pRpeEdMmmQ0/hqdefault.jpg",
            videoId = "pRpeEdMmmQ0",
            type = "song",
            duration = "4:35",
            playsText = "18 jt pemutaran"
        ),
        MusicItem(
            id = "andai_aku_bisa_tulus_cover",
            title = "Tulus - Andai Aku Bisa (Lirik Lagu)",
            subtitle = "1001 LYRICS • 8,4 jt x ditonton",
            thumbnailUrl = "https://i.ytimg.com/vi/CevxZvSJLk8/hqdefault.jpg",
            videoId = "CevxZvSJLk8",
            type = "song",
            duration = "4:18",
            playsText = "8,4 jt x ditonton"
        ),
        MusicItem(
            id = "sempurna_acoustic_cover",
            title = "Sempurna (Acoustic Session)",
            subtitle = "Gangga & Dere • 6,2 jt pemutaran",
            thumbnailUrl = "https://i.ytimg.com/vi/2Vv-BfVoq4g/hqdefault.jpg",
            videoId = "2Vv-BfVoq4g",
            type = "song",
            duration = "4:10",
            playsText = "6,2 jt pemutaran"
        ),
        MusicItem(
            id = "pupus_dewa_live_cover",
            title = "Pupus (Live Acoustic Cover)",
            subtitle = "Fiersa Besari • 9,1 jt pemutaran",
            thumbnailUrl = "https://i.ytimg.com/vi/LjhCEhWiKXk/hqdefault.jpg",
            videoId = "LjhCEhWiKXk",
            type = "song",
            duration = "4:45",
            playsText = "9,1 jt pemutaran"
        ),
        MusicItem(
            id = "dan_sheila_cover",
            title = "Dan... (Unplugged Cover)",
            subtitle = "Pamungkas • 5,4 jt pemutaran",
            thumbnailUrl = "https://i.ytimg.com/vi/hT_nvWreIhg/hqdefault.jpg",
            videoId = "hT_nvWreIhg",
            type = "song",
            duration = "4:22",
            playsText = "5,4 jt pemutaran"
        ),
        MusicItem(
            id = "kangen_dewa_remix",
            title = "Kangen (Lo-Fi Midnight Remix)",
            subtitle = "Chrisye & Sophia Latjuba • 7,8 jt pemutaran",
            thumbnailUrl = "https://i.ytimg.com/vi/afwK0Mv0IsY/hqdefault.jpg",
            videoId = "afwK0Mv0IsY",
            type = "song",
            duration = "3:58",
            playsText = "7,8 jt pemutaran"
        ),
        MusicItem(
            id = "pedih_last_child_acoustic",
            title = "Pedih (Acoustic Version)",
            subtitle = "Virgoun • 14 jt pemutaran",
            thumbnailUrl = "https://i.ytimg.com/vi/kJQP7kiw5Fk/hqdefault.jpg",
            videoId = "kJQP7kiw5Fk",
            type = "song",
            duration = "4:15",
            playsText = "14 jt pemutaran"
        ),
        MusicItem(
            id = "monokrom_live_orchestra",
            title = "Monokrom (Live Orchestra)",
            subtitle = "Tulus & Erwin Gutawa • 11 jt pemutaran",
            thumbnailUrl = "https://i.ytimg.com/vi/OPf0YbXqDm0/hqdefault.jpg",
            videoId = "OPf0YbXqDm0",
            type = "song",
            duration = "4:05",
            playsText = "11 jt pemutaran"
        )
    )

    fun getCommunityPreviewTracksFallback(playlistIndex: Int = 0): List<MusicItem> {
        val pool = listOf(
            listOf(
                MusicItem(
                    id = "comm_bunga_hatiku",
                    title = "Bunga Hatiku",
                    subtitle = "Nidji • 5,2 jt pemutaran",
                    thumbnailUrl = "https://i.ytimg.com/vi/09R8_2nJtjg/hqdefault.jpg",
                    videoId = "09R8_2nJtjg",
                    type = "song",
                    duration = "4:02",
                    playsText = "5,2 jt pemutaran"
                ),
                MusicItem(
                    id = "comm_dia_sammy",
                    title = "Dia",
                    subtitle = "Sammy Simorangkir • 6,7 jt pemutaran",
                    thumbnailUrl = "https://i.ytimg.com/vi/fRh_vgS2dFE/hqdefault.jpg",
                    videoId = "fRh_vgS2dFE",
                    type = "song",
                    duration = "4:18",
                    playsText = "6,7 jt pemutaran"
                ),
                MusicItem(
                    id = "comm_jangan_kau_lepas",
                    title = "Jangan Kau Lepas",
                    subtitle = "Alexa • 17 jt pemutaran",
                    thumbnailUrl = "https://i.ytimg.com/vi/YQHsXMglC9A/hqdefault.jpg",
                    videoId = "YQHsXMglC9A",
                    type = "song",
                    duration = "4:25",
                    playsText = "17 jt pemutaran"
                )
            ),
            listOf(
                MusicItem(
                    id = "comm_sakura_rossa",
                    title = "Sakura",
                    subtitle = "Rossa • 45 jt pemutaran",
                    thumbnailUrl = "https://lrc.red/s/IDA170321018.webp",
                    videoId = "7PCkvCPvDXk",
                    type = "song",
                    duration = "3:59",
                    playsText = "45 jt pemutaran"
                ),
                MusicItem(
                    id = "comm_dan_so7",
                    title = "Dan...",
                    subtitle = "Sheila On 7 • 112 jt pemutaran",
                    thumbnailUrl = "https://i.ytimg.com/vi/hT_nvWreIhg/hqdefault.jpg",
                    videoId = "hT_nvWreIhg",
                    type = "song",
                    duration = "4:46",
                    playsText = "112 jt pemutaran"
                ),
                MusicItem(
                    id = "comm_kangen_dewa",
                    title = "Kangen",
                    subtitle = "Dewa 19 • 95 jt pemutaran",
                    thumbnailUrl = "https://i.ytimg.com/vi/JGwWNGJdvx8/hqdefault.jpg",
                    videoId = "JGwWNGJdvx8",
                    type = "song",
                    duration = "5:30",
                    playsText = "95 jt pemutaran"
                )
            ),
            listOf(
                MusicItem(
                    id = "comm_resah_jadi_luka",
                    title = "Resah Jadi Luka",
                    subtitle = "Daun Jatuh • 142 jt pemutaran",
                    thumbnailUrl = "https://i.ytimg.com/vi/5F28ye50-Kc/hqdefault.jpg",
                    videoId = "5F28ye50-Kc",
                    type = "song",
                    duration = "4:16",
                    playsText = "142 jt pemutaran"
                ),
                MusicItem(
                    id = "comm_bertaut_nadin",
                    title = "Bertaut",
                    subtitle = "Nadin Amizah • 195 jt pemutaran",
                    thumbnailUrl = "https://i.ytimg.com/vi/SlPhMPnQ58k/hqdefault.jpg",
                    videoId = "SlPhMPnQ58k",
                    type = "song",
                    duration = "5:16",
                    playsText = "195 jt pemutaran"
                ),
                MusicItem(
                    id = "comm_gala_bunga_matahari",
                    title = "Gala bunga matahari",
                    subtitle = "Sal Priadi • 164 jt pemutaran",
                    thumbnailUrl = "https://i.ytimg.com/vi/nfWlot6h_JM/hqdefault.jpg",
                    videoId = "nfWlot6h_JM",
                    type = "song",
                    duration = "3:29",
                    playsText = "164 jt pemutaran"
                )
            )
        )
        return pool[playlistIndex % pool.size]
    }

    /**
     * Plain-text fallback for YouTube Music default lyrics (un-synced, so `isSynced = false`
     * and `syncedLines = emptyList()` — never blurred in UI!).
     */
    fun getLyricsFallback(
        track: MusicItem,
        preferredProvider: LyricsProvider = LyricsProvider.YOUTUBE_MUSIC
    ): LyricsData {
        val low = track.title.lowercase()
        val lines = when {
            low.contains("teh hijau") -> listOf(
                "Hari-hari berulang",
                "Misteri lenyap senang",
                "Ada saran kudengar",
                "Lebih sering keluar ke alam",
                "Saran lain kudengar",
                "Cari hal asing yang menantang",
                "Keluar dari benteng",
                "Dari tempatmu yang sekarang",
                "Ada yang hilang dariku belakangan",
                "Sedang tak mudah bertemu rasa senang",
                "Sedang kucari yang jadi pencetusnya",
                "Mungkin hilangnya atau siklus hidupku",
                "Mungkin aku sedang tak bisa",
                "Tak bisa jatuh cinta",
                "Membuka hati 'tuk apa pun, siapa pun",
                "Dan mungkin aku memang sedang tak bisa",
                "Tak bisa jatuh cinta",
                "Membuka hati 'tuk apa pun, siapa pun",
                "Wo-wo",
                "Wo-wo-wo",
                "Apa pun, siapa pun",
                "Tambah gerak tubuhmu",
                "Baca buku yang baru",
                "Ragam saran brilian",
                "Yang belum kunjung jadi penawar",
                "Untuk yang hilang dariku belakangan",
                "Sedang kucoba memahami alurnya",
                "Semoga segera kutemukan jawaban",
                "Tapi kini kurayakan hampa ini, oh",
                "Mungkin aku sedang tak bisa",
                "Tak bisa jatuh cinta",
                "Membuka hati 'tuk apa pun, siapa pun",
                "Dan mungkin aku memang sedang tak bisa",
                "Tak bisa jatuh cinta",
                "Membuka hati 'tuk apa pun, siapa pun",
                "Kulihat mana di kendaliku",
                "Teh hijau ini yang di tanganku",
                "Di tengah seram sedih yang menghantamku",
                "Uh-uh",
                "Lepaslah, lepas kemurunganku",
                "Hijau kembali jiwa gersangku",
                "Ambillah, sayang, sebanyak waktu yang kau perlu, uh",
                "Mungkin aku sedang tak bisa",
                "Tak bisa jatuh cinta",
                "Membuka hati 'tuk apa pun, siapa pun"
            )
            low.contains("sakura") -> listOf(
                "Senada cinta bersemi di antara kita",
                "Menyandang anggunnya peranan jiwa asmara",
                "Terlanjur untuk terhenti",
                "Di jalan yang telah tertempuh semenjak dini",
                "Sehidup semati",
                "Kian lama kian pasrah kurasakan jua",
                "Janji yang terucap tak mungkin terhapus saja",
                "Walau rintangan berjuta",
                "Walau cobaan memaksa, diriku terjerat",
                "Dipeluk asmara",
                "Bersama dirimu terbebas dari nestapa",
                "Dalam wangi bunga cita, cinta dan bahagia",
                "Walau rintangan berjuta",
                "Walau cobaan memaksa, diriku terbuai",
                "Di batas asmara",
                "Kian lama kian pasrah kurasakan jua",
                "Janji yang terucap tak mungkin terhapus saja",
                "Walau rintangan berjuta",
                "Walau cobaan memaksa, diriku terjerat",
                "Dipeluk asmara",
                "Terlambat untuk berdusta, terlambatlah sudah",
                "Menipu sanubari tak semudah kusangka",
                "Yakin akan cintamu yakinkan segalanya",
                "Perlahan dan pasti daku",
                "'Kan melangkah menuju damai jiwa"
            )
            low.contains("tak kan hilang") -> listOf(
                "Jangan pergi dari diriku",
                "Tetaplah di sini temani hariku",
                "Bila kita bersama",
                "Seperti yang terjadi kemarin",
                "Kini harus aku lewati",
                "Langkah demi langkah yang menyepi",
                "Membalut luka lagi",
                "Yang kutahan hingga kini",
                "Habis sudah nafasku",
                "Menyebut namamu, ho-oh",
                "Tak kan hilang cintaku padamu",
                "Takkan hilang walau kau memilih pergi",
                "Biarlah kurasakan rindu ini sendiri",
                "Sampai nanti kau kembali"
            )
            low.contains("resah jadi luka") -> listOf(
                "Kutemukan arti hadirnya dirimu",
                "Di sela-sela sepi yang membisu",
                "Namun mengapa semua berubah",
                "Saat ku mulai menaruh harapan",
                "Kini resah berubah menjadi luka",
                "Yang tak mudah untuk kulupakan",
                "Andai waktu bisa kuputar kembali",
                "Takkan kubiarkan kau pergi menjauh",
                "Biarkan hujan menghapus jejakmu",
                "Walau hatiku masih menyebut namamu",
                "Di setiap malam yang dingin dan sunyi",
                "Aku belajar mengikhlaskan dirimu",
                "Meski bayangmu tetap tinggal di sini",
                "Menemani sisa rindu yang tertahan"
            )
            low.contains("bertaut") -> listOf(
                "Bun, hidup berjalan seperti bajingan",
                "Seperti landak yang tak punya teman",
                "Ia menggonggong bak suara hujan",
                "Dan kau pangeranku, mengambil peran",
                "Bun, kalau saat hancur ku disayang",
                "Apalagi saat ku jadi juara",
                "Saat tak tahu arah kau di sana",
                "Menjadi gagah saat ku tak bisa",
                "Sedikit jelaskan tentangku dan kamu",
                "Agar seisi dunia tahu",
                "Keras kepalaku sama denganmu",
                "Caraku marah, caraku tersenyum",
                "Seperti detak jantung yang bertaut",
                "Nyawaku nyala karena denganmu"
            )
            low.contains("rayuan perempuan gila") -> listOf(
                "Menurutmu berapa lama lagi",
                "Kau kan mencintaiku?",
                "Menurutmu apa yang bisa terjadi",
                "Dalam sewindu?",
                "Bukan apa, hanya bersiap",
                "Tak ada yang tahu, aku takut",
                "Tak pernah ada yang lama",
                "Menemaniku yang gila",
                "Lewat pintu depan, ku kan menunggu",
                "Cinta yang takkan pergi dariku",
                "Janganlah kau takut padaku",
                "Peluklah segala kurangku"
            )
            else -> emptyList()
        }

        val isLrcMode = preferredProvider == LyricsProvider.LRC_RED && lines.isNotEmpty()
        val synced: List<com.example.data.model.LyricLine> = if (isLrcMode) {
            lines.mapIndexed { idx, lineText ->
                val start = 4.5 + (idx * 3.8)
                val end = start + 3.4
                val tokens = Regex("\\S+\\s*").findAll(lineText).map { it.value }.toList()
                val totalChars = tokens.sumOf { it.trim().length.coerceAtLeast(1) }.toDouble().coerceAtLeast(1.0)
                val span = (end - start - 0.15).coerceAtLeast(0.5)
                var cursor = start
                val words = tokens.map { token ->
                    val dur = (span * (token.trim().length.coerceAtLeast(1) / totalChars)).coerceAtLeast(0.08)
                    val wStart = cursor
                    val wEnd = (wStart + dur).coerceAtMost(end)
                    cursor = wEnd
                    com.example.data.model.LyricSyllable(
                        text = token,
                        beginSec = wStart,
                        endSec = wEnd
                    )
                }
                com.example.data.model.LyricLine(
                    text = lineText,
                    beginSec = start,
                    endSec = end,
                    words = words
                )
            }
        } else {
            emptyList()
        }

        return LyricsData(
            videoId = track.videoId ?: track.id,
            lines = lines,
            syncedLines = synced,
            isSynced = isLrcMode,
            source = preferredProvider.creditLabel,
            provider = preferredProvider
        )
    }

    fun getAutoPlayQueueFallback(seedTrack: MusicItem? = null): List<MusicItem> {
        val baseList = listOf(
            MusicItem(
                id = "queue_selamat_tinggal_virgoun",
                title = "Selamat (Selamat Tinggal)",
                subtitle = "Virgoun & Audy",
                thumbnailUrl = "https://i.ytimg.com/vi/7PCkvCPvDXk/hqdefault.jpg",
                videoId = "7PCkvCPvDXk",
                type = "song",
                duration = "5:12"
            ),
            MusicItem(
                id = "queue_sial_mahalini",
                title = "Sial",
                subtitle = "Mahalini",
                thumbnailUrl = "https://i.ytimg.com/vi/60ItHLz5WEA/hqdefault.jpg",
                videoId = "60ItHLz5WEA",
                type = "song",
                duration = "4:03"
            ),
            MusicItem(
                id = "queue_komang_raim_laode",
                title = "Komang",
                subtitle = "Raim Laode",
                thumbnailUrl = "https://i.ytimg.com/vi/JGwWNGJdvx8/hqdefault.jpg",
                videoId = "JGwWNGJdvx8",
                type = "song",
                duration = "3:42"
            ),
            MusicItem(
                id = "queue_mesin_waktu_budi_doremi",
                title = "Mesin Waktu",
                subtitle = "Budi Doremi",
                thumbnailUrl = "https://i.ytimg.com/vi/YQHsXMglC9A/hqdefault.jpg",
                videoId = "YQHsXMglC9A",
                type = "song",
                duration = "4:21"
            ),
            MusicItem(
                id = "queue_runtuh_febri_putri",
                title = "Runtuh",
                subtitle = "Feby Putri & Fiersa Besari",
                thumbnailUrl = "https://i.ytimg.com/vi/nfWlot6h_JM/hqdefault.jpg",
                videoId = "nfWlot6h_JM",
                type = "song",
                duration = "3:44"
            ),
            MusicItem(
                id = "queue_melukis_senja_budi_doremi",
                title = "Melukis Senja",
                subtitle = "Budi Doremi",
                thumbnailUrl = "https://i.ytimg.com/vi/RgKAFK5djSk/hqdefault.jpg",
                videoId = "RgKAFK5djSk",
                type = "song",
                duration = "4:12"
            ),
            MusicItem(
                id = "queue_teh_hijau_tulus",
                title = "Teh Hijau",
                subtitle = "Tulus",
                thumbnailUrl = "https://lrc.red/s/FRX282637095.webp",
                videoId = "5F28ye50-Kc",
                type = "song",
                duration = "3:33"
            )
        ) + getQuickPicksFallback()

        return baseList
            .distinctBy { it.title.lowercase() }
            .filter { seedTrack == null || !it.title.equals(seedTrack.title, ignoreCase = true) }
    }

    fun getRequiredHomeSectionsFallback(): List<HomeSection> = listOf(
        HomeSection(
            id = "req_pilihan_cepat",
            title = "Pilihan cepat",
            items = getQuickPicksFallback(),
            style = HomeSectionStyle.FOUR_ROW_SHELF
        ),
        HomeSection(
            id = "req_rekomendasi_harian_anda",
            title = "Rekomendasi harian Anda",
            items = getDailyRecommendationsFallback(),
            style = HomeSectionStyle.DAILY_RECOMMENDATION
        ),
        HomeSection(
            id = "req_cover_dan_remix",
            title = "Cover dan remix",
            items = getCoverDanRemixFallback(),
            style = HomeSectionStyle.FOUR_ROW_SHELF
        ),
        HomeSection(
            id = "req_lagu_trending_untuk_kamu",
            title = "Lagu trending untuk kamu",
            items = listOf(
                MusicItem(
                    id = "satu_bulan_bernadya",
                    title = "Satu Bulan",
                    subtitle = "Bernadya • 210 jt pemutaran",
                    thumbnailUrl = "https://i.ytimg.com/vi/60ItHLz5WEA/hqdefault.jpg",
                    videoId = "60ItHLz5WEA",
                    type = "song",
                    duration = "3:20",
                    playsText = "210 jt pemutaran"
                ),
                MusicItem(
                    id = "gala_bunga_matahari_sal",
                    title = "Gala bunga matahari",
                    subtitle = "Sal Priadi • 164 jt pemutaran",
                    thumbnailUrl = "https://i.ytimg.com/vi/nfWlot6h_JM/hqdefault.jpg",
                    videoId = "nfWlot6h_JM",
                    type = "song",
                    duration = "3:29",
                    playsText = "164 jt pemutaran"
                ),
                MusicItem(
                    id = "sialan_juicy_luicy",
                    title = "Sialan",
                    subtitle = "Adrian Khalif & Juicy Luicy • 189 jt pemutaran",
                    thumbnailUrl = "https://i.ytimg.com/vi/RgKAFK5djSk/hqdefault.jpg",
                    videoId = "RgKAFK5djSk",
                    type = "song",
                    duration = "3:51",
                    playsText = "189 jt pemutaran"
                ),
                MusicItem(
                    id = "untungnya_hidup_harus_tetap_berjalan",
                    title = "Untungnya, Hidup Harus Tetap Berjalan",
                    subtitle = "Bernadya • 152 jt pemutaran",
                    thumbnailUrl = "https://i.ytimg.com/vi/09R8_2nJtjg/hqdefault.jpg",
                    videoId = "09R8_2nJtjg",
                    type = "song",
                    duration = "3:04",
                    playsText = "152 jt pemutaran"
                ),
                MusicItem(
                    id = "lampu_kuning_juicy_luicy",
                    title = "Lampu Kuning",
                    subtitle = "Juicy Luicy • 128 jt pemutaran",
                    thumbnailUrl = "https://i.ytimg.com/vi/JGwWNGJdvx8/hqdefault.jpg",
                    videoId = "JGwWNGJdvx8",
                    type = "song",
                    duration = "3:46",
                    playsText = "128 jt pemutaran"
                ),
                MusicItem(
                    id = "kata_mereka_ini_berlebihan",
                    title = "Kata Mereka Ini Berlebihan",
                    subtitle = "Bernadya • 175 jt pemutaran",
                    thumbnailUrl = "https://i.ytimg.com/vi/hT_nvWreIhg/hqdefault.jpg",
                    videoId = "hT_nvWreIhg",
                    type = "song",
                    duration = "3:12",
                    playsText = "175 jt pemutaran"
                ),
                MusicItem(
                    id = "bunga_maaf_the_lantis",
                    title = "Bunga Maaf",
                    subtitle = "The Lantis • 94 jt pemutaran",
                    thumbnailUrl = "https://i.ytimg.com/vi/2Vv-BfVoq4g/hqdefault.jpg",
                    videoId = "2Vv-BfVoq4g",
                    type = "song",
                    duration = "3:38",
                    playsText = "94 jt pemutaran"
                ),
                MusicItem(
                    id = "penjaga_hati_nadhif",
                    title = "penjaga hati",
                    subtitle = "Nadhif Basalamah • 230 jt pemutaran",
                    thumbnailUrl = "https://i.ytimg.com/vi/OPf0YbXqDm0/hqdefault.jpg",
                    videoId = "OPf0YbXqDm0",
                    type = "song",
                    duration = "4:20",
                    playsText = "230 jt pemutaran"
                )
            ),
            style = HomeSectionStyle.FOUR_ROW_SHELF
        ),
        HomeSection(
            id = "req_playlist_trending_komunitas",
            title = "Playlist trending komunitas",
            items = listOf(
                MusicItem(
                    id = "VLPL5aUxvuyD6pMyvsuFrR0wb7gJrENr3cNf",
                    title = "Lagu Indonesia Tahun 2000an - Nostalgia SMA",
                    subtitle = "Musik Hits",
                    thumbnailUrl = "https://i.ytimg.com/vi/09R8_2nJtjg/hqdefault.jpg",
                    browseId = "VLPL5aUxvuyD6pMyvsuFrR0wb7gJrENr3cNf",
                    type = "playlist",
                    viewsAndCountText = "53 rb x ditonton • 60 lagu",
                    previewTracks = getCommunityPreviewTracksFallback(0)
                ),
                MusicItem(
                    id = "VLPL_nostalgia_sma_2000",
                    title = "GALAUUUUUUUUU BGTTT BGSTTTTT",
                    subtitle = "Qiaa",
                    thumbnailUrl = "https://i.ytimg.com/vi/5F28ye50-Kc/hqdefault.jpg",
                    browseId = "VLPL5aUxvuyD6pMyvsuFrR0wb7gJrENr3cNf",
                    type = "playlist",
                    viewsAndCountText = "84 rb x ditonton • 45 lagu",
                    previewTracks = getCommunityPreviewTracksFallback(1)
                ),
                MusicItem(
                    id = "VLPL_indie_senja_kopi",
                    title = "Teman Ngopi & Hujan Sore",
                    subtitle = "Indie Nusantara",
                    thumbnailUrl = "https://i.ytimg.com/vi/SlPhMPnQ58k/hqdefault.jpg",
                    browseId = "VLPL5aUxvuyD6pMyvsuFrR0wb7gJrENr3cNf",
                    type = "playlist",
                    viewsAndCountText = "39 rb x ditonton • 50 lagu",
                    previewTracks = getCommunityPreviewTracksFallback(2)
                )
            ),
            style = HomeSectionStyle.COMMUNITY_PLAYLIST_CARDS
        ),
        HomeSection(
            id = "req_favorit_selamanya",
            title = "Favorit selamanya!",
            strapline = "Lagu-lagu yang selalu ada di hati.",
            items = listOf(
                MusicItem(
                    id = "dan_sheila_on_7",
                    title = "Dan...",
                    subtitle = "Sheila On 7",
                    thumbnailUrl = "https://i.ytimg.com/vi/hT_nvWreIhg/hqdefault.jpg",
                    videoId = "hT_nvWreIhg",
                    type = "song"
                ),
                MusicItem(
                    id = "kangen_dewa_19",
                    title = "Kangen",
                    subtitle = "Dewa 19",
                    thumbnailUrl = "https://i.ytimg.com/vi/JGwWNGJdvx8/hqdefault.jpg",
                    videoId = "JGwWNGJdvx8",
                    type = "song"
                ),
                MusicItem(
                    id = "sakura_rossa_fav",
                    title = "Sakura",
                    subtitle = "Rossa",
                    thumbnailUrl = "https://lrc.red/s/IDA170321018.webp",
                    videoId = "7PCkvCPvDXk",
                    type = "song"
                ),
                MusicItem(
                    id = "sempurna_andra",
                    title = "Sempurna",
                    subtitle = "Andra and The Backbone",
                    thumbnailUrl = "https://i.ytimg.com/vi/2Vv-BfVoq4g/hqdefault.jpg",
                    videoId = "2Vv-BfVoq4g",
                    type = "song"
                ),
                MusicItem(
                    id = "mungkin_nanti_peterpan",
                    title = "Mungkin Nanti",
                    subtitle = "Peterpan",
                    thumbnailUrl = "https://i.ytimg.com/vi/OPf0YbXqDm0/hqdefault.jpg",
                    videoId = "OPf0YbXqDm0",
                    type = "song"
                )
            ),
            style = HomeSectionStyle.FAVORITES_FOREVER_CARDS
        ),
        HomeSection(
            id = "req_hits_indonesia",
            title = "Hits Indonesia",
            strapline = "Lagu-lagu paling populer di Indonesia saat ini.",
            items = listOf(
                MusicItem(
                    id = "hits_sial_mahalini",
                    title = "Sial",
                    subtitle = "Mahalini",
                    thumbnailUrl = "https://i.ytimg.com/vi/60ItHLz5WEA/hqdefault.jpg",
                    videoId = "60ItHLz5WEA",
                    type = "song",
                    reason = "Kisah cinta yang berakhir dengan penyesalan mendalam."
                ),
                MusicItem(
                    id = "hits_komang_raim",
                    title = "Komang",
                    subtitle = "Raim Laode",
                    thumbnailUrl = "https://i.ytimg.com/vi/JGwWNGJdvx8/hqdefault.jpg",
                    videoId = "JGwWNGJdvx8",
                    type = "song"
                ),
                MusicItem(
                    id = "hits_tertawan_hati_awdella",
                    title = "Tertawan Hati",
                    subtitle = "Awdella",
                    thumbnailUrl = "https://i.ytimg.com/vi/fRh_vgS2dFE/hqdefault.jpg",
                    videoId = "fRh_vgS2dFE",
                    type = "song"
                ),
                MusicItem(
                    id = "hits_tak_segampang_itu_anggi",
                    title = "Tak Segampang Itu",
                    subtitle = "Anggi Marito",
                    thumbnailUrl = "https://i.ytimg.com/vi/nfWlot6h_JM/hqdefault.jpg",
                    videoId = "nfWlot6h_JM",
                    type = "song"
                )
            ),
            style = HomeSectionStyle.HITS_INDONESIA_SPOTLIGHT
        ),
        HomeSection(
            id = "req_rilis_baru",
            title = "Rilis baru",
            strapline = "Album dan single terbaru untuk kamu.",
            items = listOf(
                MusicItem(
                    id = "rilis_fabula_mahalini",
                    title = "Fabula",
                    subtitle = "Mahalini • Album • Pop",
                    thumbnailUrl = "https://i.ytimg.com/vi/60ItHLz5WEA/hqdefault.jpg",
                    videoId = "60ItHLz5WEA",
                    browseId = "MPREb_mati_matian_mahalini",
                    type = "album"
                ),
                MusicItem(
                    id = "rilis_menari_dengan_bayangan",
                    title = "Menari Dengan Bayangan",
                    subtitle = "Hindia • Album",
                    thumbnailUrl = "https://i.ytimg.com/vi/bo_efYhYU2A/hqdefault.jpg",
                    videoId = "bo_efYhYU2A",
                    browseId = "MPREb_pelanggaran_rully",
                    type = "album"
                ),
                MusicItem(
                    id = "rilis_manusia_tulus",
                    title = "Manusia",
                    subtitle = "Tulus • Album",
                    thumbnailUrl = "https://lrc.red/s/FRX282637095.webp",
                    videoId = "5F28ye50-Kc",
                    browseId = "MPREb_stuck_on_you_alex",
                    type = "album"
                ),
                MusicItem(
                    id = "rilis_sialan_juicy",
                    title = "Sialan",
                    subtitle = "Juicy Luicy • Single",
                    thumbnailUrl = "https://i.ytimg.com/vi/RgKAFK5djSk/hqdefault.jpg",
                    videoId = "RgKAFK5djSk",
                    type = "album"
                )
            ),
            style = HomeSectionStyle.NEW_RELEASES_BENTO
        ),
        HomeSection(
            id = "req_musik_cadas",
            title = "Musik Cadas",
            strapline = "Rock, metal, dan musik keras pilihan.",
            items = listOf(
                MusicItem(
                    id = "welcome_to_black_parade",
                    title = "The Black Parade",
                    subtitle = "My Chemical Romance",
                    thumbnailUrl = "https://i.ytimg.com/vi/RRKJiM9Njr8/hqdefault.jpg",
                    videoId = "RRKJiM9Njr8",
                    type = "song"
                ),
                MusicItem(
                    id = "numb_linkin_park",
                    title = "Hybrid Theory",
                    subtitle = "Linkin Park",
                    thumbnailUrl = "https://i.ytimg.com/vi/kXYiU_JCYtU/hqdefault.jpg",
                    videoId = "kXYiU_JCYtU",
                    type = "song"
                ),
                MusicItem(
                    id = "dear_god_avenged",
                    title = "Nightmare",
                    subtitle = "Avenged Sevenfold",
                    thumbnailUrl = "https://i.ytimg.com/vi/mzX0rhF8buo/hqdefault.jpg",
                    videoId = "mzX0rhF8buo",
                    type = "song"
                ),
                MusicItem(
                    id = "bring_me_to_life",
                    title = "Fallen",
                    subtitle = "Evanescence",
                    thumbnailUrl = "https://i.ytimg.com/vi/3YxaaGgTQYM/hqdefault.jpg",
                    videoId = "3YxaaGgTQYM",
                    type = "song"
                )
            ),
            style = HomeSectionStyle.MUSIK_CADAS_ROCK
        )
    )

    // Backwards-compatible alias
    fun getRequired8SectionsFallback(): List<HomeSection> = getRequiredHomeSectionsFallback()

    fun getPlaylistFallback(seedItem: MusicItem): PlaylistDetail {
        val tracks = if (seedItem.previewTracks.isNotEmpty()) {
            seedItem.previewTracks
        } else {
            getQuickPicksFallback()
        }
        val isAlbum = seedItem.type == "album" || seedItem.browseId?.startsWith("MPREb_") == true
        val author = seedItem.subtitle
            .replace("•", ",")
            .ifBlank { "user, ${tracks.size} lagu" }
        val kindLabel = if (isAlbum) "ALBUM" else "PLAYLIST"

        return PlaylistDetail(
            browseId = seedItem.browseId ?: seedItem.id,
            title = seedItem.title.ifBlank { "Playlist Pilihan" },
            authorSubtitle = if (author.contains("lagu", ignoreCase = true)) author else "$author, ${tracks.size} lagu",
            metaSubtitle = "$kindLabel • ${tracks.size} TRACKS",
            description = null,
            thumbnailUrl = seedItem.ultraResThumbnail.ifBlank { tracks.first().ultraResThumbnail },
            totalTracks = tracks.size,
            tracks = tracks
        )
    }

    private val FALLBACK_ARTIST_THUMBS = listOf(
        "https://i.ytimg.com/vi/kOkQ4T5WO9E/hqdefault.jpg",
        "https://i.ytimg.com/vi/60ItHLz5WEA/hqdefault.jpg",
        "https://i.ytimg.com/vi/fRh_vgS2dFE/hqdefault.jpg",
        "https://i.ytimg.com/vi/nfWlot6h_JM/hqdefault.jpg",
        "https://i.ytimg.com/vi/5F28ye50-Kc/hqdefault.jpg",
        "https://i.ytimg.com/vi/hT_nvWreIhg/hqdefault.jpg",
        "https://i.ytimg.com/vi/JGwWNGJdvx8/hqdefault.jpg",
        "https://i.ytimg.com/vi/bo_efYhYU2A/hqdefault.jpg",
        "https://i.ytimg.com/vi/YQHsXMglC9A/hqdefault.jpg",
        "https://i.ytimg.com/vi/RgKAFK5djSk/hqdefault.jpg",
        "https://i.ytimg.com/vi/SlPhMPnQ58k/hqdefault.jpg",
        "https://i.ytimg.com/vi/OPf0YbXqDm0/hqdefault.jpg"
    )

    fun getArtistAvatarForName(artistName: String, fallbackUrl: String = ""): String {
        val low = artistName.trim().lowercase()
        val cleanFallback = fallbackUrl.trim().takeIf {
            it.isNotBlank() && !it.contains("FRX282637095.webp")
        }.orEmpty()
        if (cleanFallback.isNotBlank()) return cleanFallback
        return when {
            low.contains("nadin") -> "https://i.ytimg.com/vi/kOkQ4T5WO9E/hqdefault.jpg"
            low.contains("bernadya") -> "https://i.ytimg.com/vi/60ItHLz5WEA/hqdefault.jpg"
            low.contains("mahalini") -> "https://i.ytimg.com/vi/fRh_vgS2dFE/hqdefault.jpg"
            low.contains("sal priadi") -> "https://i.ytimg.com/vi/nfWlot6h_JM/hqdefault.jpg"
            low.contains("daun jatuh") -> "https://i.ytimg.com/vi/5F28ye50-Kc/hqdefault.jpg"
            low.contains("sheila on 7") -> "https://i.ytimg.com/vi/hT_nvWreIhg/hqdefault.jpg"
            low.contains("dewa 19") -> "https://i.ytimg.com/vi/JGwWNGJdvx8/hqdefault.jpg"
            low.contains("hindia") -> "https://i.ytimg.com/vi/bo_efYhYU2A/hqdefault.jpg"
            low.contains("budi doremi") -> "https://i.ytimg.com/vi/YQHsXMglC9A/hqdefault.jpg"
            low.contains("juicy luicy") -> "https://i.ytimg.com/vi/RgKAFK5djSk/hqdefault.jpg"
            else -> {
                val idx = (low.hashCode() and Int.MAX_VALUE) % FALLBACK_ARTIST_THUMBS.size
                FALLBACK_ARTIST_THUMBS[idx]
            }
        }
    }

    fun getArtistProfileFallback(artistName: String, seedItem: MusicItem? = null): ArtistProfileDetail {
        val cleanName = artistName.trim().ifBlank { "Tulus" }
        val low = cleanName.lowercase()

        val avatarUrl = getArtistAvatarForName(
            artistName = cleanName,
            fallbackUrl = seedItem?.ultraResThumbnail.orEmpty()
        )

        val topSongs = listOf(
            MusicItem(
                id = "${cleanName}_top_1",
                title = if (low.contains("rossa")) "Sakura" else if (low.contains("nadin")) "Rayuan Perempuan Gila" else "Teh Hijau",
                subtitle = "$cleanName • 128 jt pemutaran",
                thumbnailUrl = avatarUrl,
                videoId = if (low.contains("rossa")) "7PCkvCPvDXk" else "5F28ye50-Kc",
                type = "song",
                duration = "3:33",
                playsText = "128 jt pemutaran"
            ),
            MusicItem(
                id = "${cleanName}_top_2",
                title = if (low.contains("rossa")) "Hati Yang Kau Sakiti" else if (low.contains("nadin")) "Bertaut" else "Hati-Hati di Jalan",
                subtitle = "$cleanName • 245 jt pemutaran",
                thumbnailUrl = "https://i.ytimg.com/vi/SlPhMPnQ58k/hqdefault.jpg",
                videoId = "SlPhMPnQ58k",
                type = "song",
                duration = "4:02",
                playsText = "245 jt pemutaran"
            ),
            MusicItem(
                id = "${cleanName}_top_3",
                title = if (low.contains("rossa")) "Terlalu Cinta" else if (low.contains("nadin")) "Sorai" else "Monokrom",
                subtitle = "$cleanName • 190 jt pemutaran",
                thumbnailUrl = "https://i.ytimg.com/vi/OPf0YbXqDm0/hqdefault.jpg",
                videoId = "OPf0YbXqDm0",
                type = "song",
                duration = "3:54",
                playsText = "190 jt pemutaran"
            ),
            MusicItem(
                id = "${cleanName}_top_4",
                title = if (low.contains("rossa")) "Pudar" else if (low.contains("nadin")) "Semua Aku Dirayakan" else "Interaksi",
                subtitle = "$cleanName • 114 jt pemutaran",
                thumbnailUrl = "https://i.ytimg.com/vi/YQHsXMglC9A/hqdefault.jpg",
                videoId = "YQHsXMglC9A",
                type = "song",
                duration = "3:18",
                playsText = "114 jt pemutaran"
            ),
            MusicItem(
                id = "${cleanName}_top_5",
                title = if (low.contains("rossa")) "Tegar" else if (low.contains("nadin")) "Rumpang" else "Diri",
                subtitle = "$cleanName • 98 jt pemutaran",
                thumbnailUrl = "https://i.ytimg.com/vi/nfWlot6h_JM/hqdefault.jpg",
                videoId = "nfWlot6h_JM",
                type = "song",
                duration = "3:42",
                playsText = "98 jt pemutaran"
            )
        )

        val albums = listOf(
            MusicItem(
                id = "alb_1_${cleanName.lowercase()}",
                title = if (low.contains("rossa")) "Another Journey: The Beginning" else if (low.contains("nadin")) "Untuk Dunia, Cinta, dan Kotornya" else "Manusia",
                subtitle = "Album • 2023 • 10 lagu",
                thumbnailUrl = avatarUrl,
                browseId = "MPREb_manusia_tulus",
                type = "album",
                previewTracks = topSongs
            ),
            MusicItem(
                id = "alb_2_${cleanName.lowercase()}",
                title = if (low.contains("rossa")) "Love, Life & Music" else if (low.contains("nadin")) "Selamat Ulang Tahun" else "Monokrom",
                subtitle = "Album • 2020 • 9 lagu",
                thumbnailUrl = "https://i.ytimg.com/vi/OPf0YbXqDm0/hqdefault.jpg",
                browseId = "MPREb_monokrom_tulus",
                type = "album",
                previewTracks = topSongs
            ),
            MusicItem(
                id = "alb_3_${cleanName.lowercase()}",
                title = if (low.contains("rossa")) "The Best of Rossa" else if (low.contains("nadin")) "Kalah Bertaruh" else "Gajah",
                subtitle = "Album • 2018 • 11 lagu",
                thumbnailUrl = "https://i.ytimg.com/vi/SlPhMPnQ58k/hqdefault.jpg",
                browseId = "MPREb_gajah_tulus",
                type = "album",
                previewTracks = topSongs
            )
        )

        val singles = listOf(
            MusicItem(
                id = "sgl_1_${cleanName.lowercase()}",
                title = if (low.contains("rossa")) "Sakura (Remastered)" else if (low.contains("nadin")) "Tawa" else "Teh Hijau",
                subtitle = "Single • 2024 • Rilis Terbaru",
                thumbnailUrl = avatarUrl,
                browseId = "MPREb_single_1",
                type = "album",
                previewTracks = topSongs.take(3)
            ),
            MusicItem(
                id = "sgl_2_${cleanName.lowercase()}",
                title = if (low.contains("rossa")) "Lupakan Cinta" else if (low.contains("nadin")) "Di Akhir Perang" else "Ingkar",
                subtitle = "Single • 2023",
                thumbnailUrl = "https://i.ytimg.com/vi/60ItHLz5WEA/hqdefault.jpg",
                browseId = "MPREb_single_2",
                type = "album",
                previewTracks = topSongs.drop(1).take(3)
            ),
            MusicItem(
                id = "sgl_3_${cleanName.lowercase()}",
                title = if (low.contains("rossa")) "Masih" else if (low.contains("nadin")) "Seperti Tulang" else "Adaptasi (EP)",
                subtitle = "EP • 2022",
                thumbnailUrl = "https://i.ytimg.com/vi/nfWlot6h_JM/hqdefault.jpg",
                browseId = "MPREb_single_3",
                type = "album",
                previewTracks = topSongs.drop(2).take(3)
            )
        )

        val videos = listOf(
            MusicItem(
                id = "vid_1_${cleanName.lowercase()}",
                title = "${topSongs.first().title} (Official Music Video)",
                subtitle = "$cleanName • 48 jt x ditonton",
                thumbnailUrl = "https://i.ytimg.com/vi/${topSongs.first().videoId ?: "7PCkvCPvDXk"}/hqdefault.jpg",
                videoId = topSongs.first().videoId ?: "7PCkvCPvDXk",
                type = "video",
                duration = "3:52"
            ),
            MusicItem(
                id = "vid_2_${cleanName.lowercase()}",
                title = "${topSongs[1].title} (Live Session)",
                subtitle = "$cleanName • 19 jt x ditonton",
                thumbnailUrl = "https://i.ytimg.com/vi/SlPhMPnQ58k/hqdefault.jpg",
                videoId = "SlPhMPnQ58k",
                type = "video",
                duration = "4:15"
            )
        )

        val featuredPlaylists = listOf(
            MusicItem(
                id = "feat_pl_1_${cleanName.lowercase()}",
                title = "Presenting $cleanName",
                subtitle = "YouTube Music • ${topSongs.size} lagu",
                thumbnailUrl = avatarUrl,
                browseId = "VLPL5aUxvuyD6pMyvsuFrR0wb7gJrENr3cNf",
                type = "playlist",
                previewTracks = topSongs
            ),
            MusicItem(
                id = "feat_pl_2_${cleanName.lowercase()}",
                title = "$cleanName Radio Mix",
                subtitle = "Koleksi Hits Terbaik • ${topSongs.size} lagu",
                thumbnailUrl = "https://i.ytimg.com/vi/OPf0YbXqDm0/hqdefault.jpg",
                browseId = "VLPL5aUxvuyD6pMyvsuFrR0wb7gJrENr3cNf",
                type = "playlist",
                previewTracks = topSongs.reversed()
            )
        )

        val relatedPool = listOf(
            MusicItem(
                id = "rel_tulus",
                title = "Tulus",
                subtitle = "3,2 jt subscriber",
                thumbnailUrl = "https://lrc.red/s/FRX282637095.webp",
                browseId = "UC_tulus",
                type = "artist"
            ),
            MusicItem(
                id = "rel_nadin_amizah",
                title = "Nadin Amizah",
                subtitle = "2,4 jt subscriber",
                thumbnailUrl = "https://i.ytimg.com/vi/kOkQ4T5WO9E/hqdefault.jpg",
                browseId = "UC_nadin_amizah",
                type = "artist"
            ),
            MusicItem(
                id = "rel_rossa",
                title = "Rossa",
                subtitle = "4,1 jt subscriber",
                thumbnailUrl = "https://lrc.red/s/IDA170321018.webp",
                browseId = "UC_rossa",
                type = "artist"
            ),
            MusicItem(
                id = "rel_bernadya",
                title = "Bernadya",
                subtitle = "1,9 jt subscriber",
                thumbnailUrl = "https://i.ytimg.com/vi/60ItHLz5WEA/hqdefault.jpg",
                browseId = "UC_bernadya",
                type = "artist"
            ),
            MusicItem(
                id = "rel_sal_priadi",
                title = "Sal Priadi",
                subtitle = "1,4 jt subscriber",
                thumbnailUrl = "https://i.ytimg.com/vi/nfWlot6h_JM/hqdefault.jpg",
                browseId = "UC_sal_priadi",
                type = "artist"
            ),
            MusicItem(
                id = "rel_mahalini",
                title = "Mahalini",
                subtitle = "3,5 jt subscriber",
                thumbnailUrl = "https://i.ytimg.com/vi/fRh_vgS2dFE/hqdefault.jpg",
                browseId = "UC_mahalini",
                type = "artist"
            ),
            MusicItem(
                id = "rel_hindia",
                title = "Hindia",
                subtitle = "1,6 jt subscriber",
                thumbnailUrl = "https://i.ytimg.com/vi/bo_efYhYU2A/hqdefault.jpg",
                browseId = "UC_hindia",
                type = "artist"
            ),
            MusicItem(
                id = "rel_sheila_on_7",
                title = "Sheila On 7",
                subtitle = "3,8 jt subscriber",
                thumbnailUrl = "https://i.ytimg.com/vi/hT_nvWreIhg/hqdefault.jpg",
                browseId = "UC_sheila_on_7",
                type = "artist"
            )
        ).filterNot { it.title.equals(cleanName, ignoreCase = true) }

        return ArtistProfileDetail(
            browseId = seedItem?.browseId ?: "UC_${cleanName.lowercase()}",
            name = cleanName,
            subscribersText = "2,8 jt subscriber",
            monthlyListenersText = "14,6 jt pendengar bulanan",
            description = "$cleanName adalah musisi dan penulis lagu papan atas yang dikenal lewat aransemen hangat, lirik puitis yang jujur, serta vokal yang berkarakter kuat di belantika musik Indonesia.",
            headerImageUrl = avatarUrl,
            avatarUrl = avatarUrl,
            latestRelease = singles.firstOrNull(),
            topSongs = topSongs,
            albums = albums,
            singlesAndEps = singles,
            videos = videos,
            featuredPlaylists = featuredPlaylists,
            relatedArtists = relatedPool
        )
    }
}
