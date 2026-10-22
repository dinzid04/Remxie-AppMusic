# Rencana Implementasi Penuh Sistem Lirik BitChord (Tahap 1 – Tahap 3)

## 1. Tujuan & Arsitektur
Mengintegrasikan seluruh mesin pengambil, *parser*, *clock* sinkronisasi, translasi/romanisasi, penyimpanan lirik *offline*, serta antarmuka lirik dari referensi **BitChord** (`BitChord_Khusus_Lirik_dfc19cb0.txt`) ke dalam arsitektur aplikasi (`com.example.data.lyrics` & `com.example.ui.player`) secara utuh (100% tanpa *placeholder*) dalam satu eksekusi menyeluruh (**Tahap 1, Tahap 2, dan Tahap 3 sekaligus**).

---

## 2. Rincian Implementasi per Tahap

### Tahap 1: Model Data, Parser Lirik Tingkat Lanjut, & HTTP/Query Helper (`com.example.data.lyrics`)
1. **`LyricLine.kt`**:
   - Implementasi lengkap `LyricWord`, `LyricSyllable` (`normalized`, `charsSungAt`, `withoutChars`), `LyricAlignment` (`Start`, `End`), `LyricLine` (`wordSpans`, `sweepSpans`, `revealedChars`, `wordLift`, `wordFall`, `growingWords`, `animatesFromMs`, `animatesUntilMs`), serta `GrowingWord` & `CharGrowth` untuk efek *letter-by-letter swell & bloom* pada nada panjang.
2. **`SyllableRuns.kt`, `LyricGaps.kt`, `LyricAlignments.kt`, `LyricSections.kt`, & `BackgroundVocals.kt`**:
   - Penggabungan suku kata menjadi kata (`wordsFromRuns` dengan deteksi aksara CJK/Thai dan *explicit spacing*).
   - Penyisipan jeda instrumental otomatis (`withInstrumentalGaps`, `MIN_GAP_MS = 4_000L`).
   - Penentuan sisi duet kiri/kanan (`lineAlignments` berdasarkan `ttm:agent` `person`/`group`/`other`).
   - Pemisahan vokal latar di akhir baris (`withBackgroundVocals`) dan pendeteksi bagian lagu (`isGeniusSectionHeader`).
3. **`EnhancedLrc.kt`, `TtmlLyrics.kt`, `ProviderLyrics.kt` (`KaraokeLrc`), & `LrcWriter.kt`**:
   - Parser LRC standar (`[mm:ss.xx]`), Enhanced A2 LRC (`<mm:ss.xx>`), Karaoke QRC/YRC (`[start,dur](start,dur,0)`), serta Apple Music TTML (`<p>`, `<span>`, `ttm:agent`, `ttm:role="x-bg"`).
   - Penulis LRC & Enhanced A2 LRC (`toLrc()`, `toEnhancedLrc()`, marker `<R>`) untuk penyimpanan lirik *offline*.
4. **`LyricsHttp.kt` & `LyricsQuery.kt`**:
   - Klien HTTP bersama berbasis `OkHttpClient` dengan batas waktu ketat (`lyricsGet`, `lyricsGetBearer`, `lyricsGetAuthorized`) serta fungsi pembersih judul/artis (`forLyricsSearch()`, `artistForLyricsSearch()`).

---

### Tahap 2: Semua Provider Lirik Eksternal & Lirik Offline (`com.example.data.lyrics`)
Implementasi lengkap seluruh *provider* lirik sesuai referensi:
1. **`LrcRed.kt`**: Pencarian & pengunduhan TTML berbasis ISRC (`https://lrc.red/s/{ISRC}.ttml` & `/search.json`).
2. **`BiniLyrics.kt`**: Identifikasi rekaman (`identify` untuk mendapatkan kode ISRC) & pengambilan TTML (`lyrics-api.binimum.org`).
3. **`BetterLyrics.kt`**: Endpoint utama (`lyrics-api.boidu.dev/getLyrics`) dan endpoint QQ Portato (`lyrics-api.boidu.dev/qq/getLyrics`).
4. **`PaxSenix.kt`**: Pencarian katalog Apple Music + proxy TTML (`lyrics.paxsenix.org/apple-music/lyrics`), parser payload terstruktur (`parseTimedApple`, `parseLrcGet`), serta rute terautentikasi **PaxSenix: Spotify** dan **PaxSenix: Musixmatch** dengan dukungan API key.
5. **`LyricsPlus.kt`**: Balapan paralel antar *mirror* komunitas YouLy+ (`/v2/lyrics/get`) dengan memori *mirror* tercepat (`lastGood`) dan penggabungan suku kata (`syllabus`).
6. **`SimpMusicLyrics.kt`**: Pencarian berbasis `videoId` (`api-lyrics.simpmusic.org/v1/`).
7. **`Unison.kt`**: Lirik komunitas (`unison.boidu.dev/lyrics`) dengan dukungan TTML, Enhanced LRC, dan Plain.
8. **`YouTubeLyrics.kt`**: `YouTubeMusicLyrics` (tab Lyrics dari endpoint `next`/`browse` Innertube) dan `YouTubeTranscriptLyrics` (timed captions `transcriptCueRenderer`).
9. **`KuGou.kt`**: Pencarian *hash* lagu, kandidat lirik, dekode Base64 LRC, pembersihan baris kredit (`stripCredits`), dan deteksi instrumental (`isInstrumentalNotice`).
10. **`LrcLib.kt`**: Pencocokan eksak (`/api/get`) dan *fuzzy search* (`/api/search`) beserta `parseLrc`.
11. **`Musixmatch.kt`**: Penandatanganan HMAC-SHA256 otomatis, rotasi *user token* & *signing secret*, pengambilan `track.richsync.get` (per kata/suku kata) dan *fallback* `track.subtitle.get`.
12. **`Megalobiz.kt` & `Genius.kt`**: Scraper LRC komunitas Megalobiz serta scraper HTML Genius (`/api/search/multi` + ekstraksi kontainer `data-lyrics-container` & pembersihan artefak).
13. **`EmbeddedLyrics.kt`**: Pembacaan lirik *offline* dari file `.lrc` pendamping (*sidecar*) maupun tag kontainer audio (`MP4/M4A`, `FLAC`, `Matroska/WebM`) serta penyimpanan otomatis lirik Enhanced LRC saat mengunduh lagu di `MusicDownloadManager`.

---

### Tahap 3: Orkestrasi `LyricsRepository`, Sistem Translasi/Romanisasi, `LyricClock`, & Integrasi UI
1. **`LyricsSource.kt`, `LyricsProviderState.kt`, & `LyricsRepository.kt`**:
   - Definisi seluruh 17 sumber lirik beserta prioritas bawaan (`LRC_RED`, `BINI_LYRICS`, `BETTER_LYRICS`, `BETTER_LYRICS_PORTATO`, `PAXSENIX`, `PAXSENIX_SPOTIFY`, `PAXSENIX_MUSIXMATCH`, `LYRICS_PLUS`, `UNISON`, `YOUTUBE_TRANSCRIPT`, `YOUTUBE_MUSIC`, `KUGOU`, `LRCLIB`, `MUSIXMATCH`, `GENIUS`, serta `SIMP_MUSIC` & `MEGALOBIZ`).
   - Resolusi ISRC awal via `BiniLyrics.identify`, berbagi unduhan dokumen (`SharedDocuments`), balapan paralel semua sumber aktif dengan pengambilan hasil sesuai urutan prioritas pengguna, opsi **Prioritize Syllable Sync**, dan pelaporan status per *provider* (`NOT_FETCHED`, `FETCHING`, `FOUND`, `NOT_FOUND`).
2. **`LyricsTranslation.kt` & `TranslationLanguages.kt`**:
   - Translasi & romanisasi *on-demand* dengan pemetaan ke `LyricLine.timingSource` agar sapuan per kata tetap sinkron di atas teks terjemahan, lengkap dengan *cache* kompresi GZIP di direktori *cache* aplikasi.
3. **`LyricClock.kt` & `LyricFocus.kt`**:
   - *Playhead* lirik 60fps yang dikemudikan (*steered*) oleh pembacaan posisi pemutar musik tanpa pernah melompat mundur atau macet, mendukung *glide* saat *seek* dan pergeseran *offset* lirik (`adjustedLyricsPosition` & `adjustedLyricsSeekTarget`).
4. **Komponen UI Player & Pengaturan (`SyncedLyricsPanel.kt`, `LyricsProviderSheet.kt`, `LyricsOffsetSheet.kt`, `LyricsSourcesDialog.kt`)**:
   - Integrasi penuh ke `FullMusicPlayerScreen.kt` (tampilan sapuan per suku kata/kata, *word lift*, *held-word letter swell & bloom*, vokal latar `x-bg`, duet kiri/kanan, 3 titik indikator interlude instrumental, tombol **Translate** & **Romanize**, pemilih sumber lirik langsung dari panel lirik & menu titik tiga, serta pengatur **Lyrics Offset** `-5.0s` s/d `+5.0s`).
   - Integrasi **`LyricsSourcesDialog`** (daftar sumber lirik yang dapat dicentang, diurutkan ulang dengan *drag-and-drop*, toggle *Prioritize Syllable Lyrics*, dan konfigurasi API key PaxSenix) di halaman **Pengaturan (`SettingsAndIntegrationsScreen.kt`)** serta dari dalam **Music Player**.
