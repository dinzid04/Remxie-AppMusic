# BitChord — Bold Sans-Serif Typography, Perpustakaan (Library) & Playlist Manager, Offline Music Downloader, dan Optimasi Performa 120 FPS

Pembaruan menyeluruh pada aplikasi **BitChord** untuk menghadirkan tipografi **Sans-Serif SemiBold/Bold/ExtraBold** yang tegas di seluruh layar (termasuk lirik), merombak halaman **Perpustakaan (Library)** sesuai tangkapan layar referensi untuk menampung playlist yang diimpor maupun dibuat sendiri, menambahkan **Download Musik Offline** (per lagu & satu playlist/album) dengan bar persentase (`%`) di notifikasi Android, serta mengoptimalkan arsitektur rendering agar **scrolling atas-bawah dan geser kanan-kiri mulus tanpa lag, frame drop, atau crash**.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> Berikut adalah keputusan desain dan teknis yang telah dikonfirmasi dari jawaban Anda:

- **Tipografi Sans-Serif Super Tebal**: Menggunakan **Sistem Sans-Serif bawaan (`FontFamily.SansSerif`)** dengan ketebalan **Bold / ExtraBold / Black** di seluruh judul, daftar lagu, tombol, dan metadata. Untuk tampilan lirik di Full Player, baris lirik yang sedang aktif memakai **ExtraBold berukuran besar**, sedangkan baris lirik lainnya memakai **SemiBold** yang tetap tebal dan mudah dibaca.
- **Navigasi Kapsul 3 Tab (`Home`, `Library`, `Search`)**: Kapsul navigasi bawah menampilkan 3 tab utama yang fokus. Playlist hasil impor (Spotify / YouTube Music) tidak lagi memenuhi halaman `Home`, melainkan otomatis tersimpan dan terorganisir di dalam tab **Library (`Perpustakaan`)**.
- **Tata Letak Halaman `Perpustakaan` Sesuai Referensi**: Menampilkan kartu statistik **"Pengalaman Mendengarkan Anda"** (*Minutes Listened* & *Top Song*) di bagian atas, diikuti menu **Downloads**, **Local Music**, rak **Playlist** (*Playlist baru*, *Impor dari Spotify*, *Musik yang Disukai*, serta daftar playlist pengguna), **Artis**, dan **Podcasts**.
- **Penyimpanan Download Offline**: Lagu dan playlist yang di-download disimpan di **penyimpanan internal aplikasi** agar langsung bisa diputar secara **offline** tanpa kuota internet dan diakses cepat melalui menu **Downloads** di halaman Perpustakaan, lengkap dengan **notifikasi progress bar (`%`)** di status bar Android.

---

## 1. Overview & Core Concept

- **What It Does**:
  1. **Bold Sans-Serif Visual Overhaul**: Mengganti pemetaan font statis lama yang menyebabkan teks tampak tipis menjadi `FontFamily.SansSerif` dengan bobot `SemiBold` (600), `Bold` (700), `ExtraBold` (800), dan `Black` (900) yang dirender langsung oleh mesin teks Android.
  2. **Dedicated Library (`Perpustakaan`) & Playlist Studio**: Memindahkan seluruh koleksi playlist impor dan kustom ke halaman **Perpustakaan** yang persis mengikuti referensi desain Anda—lengkap dengan *bottom sheet* **"New playlist"** (*Private / Unlisted / Public*), fitur **Musik yang Disukai**, **Simpan Playlist ke Library**, **Tambah Lagu ke Playlist**, **Pin Playlist**, **Rename Playlist**, dan **Delete Playlist**.
  3. **Offline Downloader dengan Notifikasi Progress `%`**: Mengunduh stream audio lagu atau seluruh isi playlist/album ke penyimpanan lokal dengan *progress bar* persentase langsung di panel notifikasi HP.
  4. **Zero-Lag Scrolling & Swiping Optimization**: Memutus *recomposition* berulang akibat detak progres lagu (`currentPositionMs`) agar halaman `Home`, `Perpustakaan`, `Search`, dan `Playlist Detail` tidak ikut di-render ulang setiap 200ms saat pengguna melakukan *scrolling* atau *swiping*.
- **Target Audience / Persona**: Pendengar musik aktif yang menginginkan keterbacaan teks ekstra tegas, pengelolaan koleksi playlist yang rapi seperti aplikasi streaming premium, pemutaran musik offline saat tanpa sinyal, dan performa gulir layar yang sangat mulus.
- **Key Value**: Tampilan teks yang tajam dan tebal di setiap sudut aplikasi, manajemen playlist yang utuh di satu tempat (`Perpustakaan`), kemampuan memutar lagu secara offline, serta responsivitas UI yang stabil.

---

## 2. User Experience & Visual Design

### Key User Flows

1. **Membaca Judul, Menu, dan Lirik dengan Font Tebal**:
   - Begitu aplikasi dibuka, seluruh teks mulai dari header `BitChord` dan `Perpustakaan`, nama lagu, nama artis, badge, hingga lirik `lrc.red` tampil menggunakan font **Sans-Serif** tebal (`SemiBold` hingga `ExtraBold`).
   - Saat membuka lirik di **Full Player**, baris lirik yang sedang dinyanyikan tampil menonjol dengan ukuran besar dan bobot `ExtraBold` putih terang, sementara baris lirik sebelum/sesudahnya tampil dengan bobot `SemiBold` yang tetap tegas.

2. **Mengelola Koleksi & Playlist di Halaman `Perpustakaan` (Library)**:
   - Pengguna mengetuk tab **Library** di kapsul navigasi bawah.
   - Bagian atas menampilkan header **Perpustakaan** (beserta tombol Riwayat & Profil di kanan atas) dan 2 kartu geser **Pengalaman Mendengarkan Anda**:
     - Kartu 1: Total **MINUTES LISTENED** & jumlah pemutaran lagu.
     - Kartu 2: **TOP SONG** (lagu yang paling sering diputar beserta nama artisnya).
   - Di bawahnya terdapat baris navigasi **Downloads** dan **Local Music**.
   - Pada bagian **Playlist** (dengan tombol `Lihat semua`):
     - Kartu 1: **Playlist baru (`+`)** — mengetuk kartu ini memunculkan *bottom sheet* **"New playlist"** (*Saved to your YouTube Music account / Library*, kolom input `Playlist name`, pilihan `Private`, `Unlisted`, `Public`, dan tombol `Create playlist`).
     - Kartu 2: **Impor dari Spotify** — mengetuk kartu ini membuka dialog impor playlist dari tautan/akun Spotify atau YouTube Music, dan hasilnya langsung masuk ke deretan Playlist di **Perpustakaan** (tidak lagi menumpuk di Home).
     - Kartu 3: **Musik yang Disukai** — kartu gradien ungu-merah muda dengan ikon *Thumbs Up* yang otomatis mengumpulkan semua lagu yang diberi tanda suka (*Like*).
     - Kartu 4 dst.: Playlist yang di-pin (*Pinned*), playlist buatan pengguna, playlist hasil impor, dan playlist yang disimpan dari halaman lain.
   - Mengetuk ikon titik tiga (`⋮`) pada playlist pengguna membuka opsi: **Pin / Lepas Pin Playlist**, **Ubah Nama Playlist (Rename)**, **Download Playlist**, dan **Hapus Playlist (Delete)**.
   - Mengetuk ikon titik tiga (`⋮`) pada lagu mana pun (di Home, Search, Playlist, atau Player) menyediakan opsi **Tambahkan ke Playlist** (memilih playlist di Library atau membuat playlist baru) dan **Download Lagu**.
   - Di bawah rak Playlist terdapat rak horizontal **Artis** (menampilkan foto artis beserta jumlah lagu di koleksi) dan **Podcasts** (`Episode untuk Nanti`).

3. **Download Musik Per Lagu & Per Playlist/Album dengan Notifikasi Progress (`%`)**:
   - Pengguna mengetuk tombol titik tiga (`⋮`) pada sebuah lagu, di dalam **Full Music Player**, atau pada halaman **Playlist / Album**, lalu memilih **Download**.
   - Sistem langsung memulai pengunduhan di latar belakang dan menampilkan **Notifikasi Android** dengan *progress bar* persentase (`0%` ... `100%`) beserta judul lagu atau progres jumlah lagu dalam playlist (`Mengunduh 3/12 lagu • 45%`).
   - Setelah selesai (`100%`), lagu ditandai dengan ikon *Downloaded* (`✓`) dan tersimpan di menu **Downloads** pada halaman **Perpustakaan**, siap diputar secara **offline** kapan saja.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Menggunakan `FontFamily.SansSerif` dengan Bobot Eksplisit (`SemiBold`, `Bold`, `ExtraBold`, `Black`)**
  - *Chosen Approach*: Menghapus deklarasi `Font(R.font.plus_jakarta_sans, ...)` yang memetakan satu file TTF statis Reguler ke semua bobot, dan menggantinya dengan `FontFamily.SansSerif` murni di `Type.kt` serta seluruh komponen UI.
  - *Why*: Pada Android Compose, memetakan satu file `.ttf` Reguler ke semua `FontWeight` membuat sistem menganggap file Reguler tersebut sudah tebal sehingga menolak merender huruf tebal. Dengan `FontFamily.SansSerif` bawaan sistem, bobot `SemiBold` (600), `Bold` (700), `ExtraBold` (800), dan `Black` (900) dirender dengan ketebalan asli yang tajam dan nol *overhead* memori font.

- **Decision 2: Isolasi State Progres Pemutaran (`currentPositionMs`) untuk Mencegah Frame Drop & Lag**
  - *Chosen Approach*: Memisahkan state posisi waktu lagu (`currentPositionMs` yang berubah setiap 200ms) dari state halaman utama (`ListenNowScreen`, `LibraryScreen`, `SearchScreen`, `PlaylistDetailScreen`) dan menandai seluruh model data dengan `@Immutable` / `@Stable`.
  - *Why*: Sebelumnya, setiap kali detik lagu berjalan (5 kali per detik), seluruh layar yang sedang di-scroll ikut melakukan *recomposition* penuh sehingga memicu *frame drop* (patah-patah) saat pengguna menggeser layar ke atas/bawah maupun kanan/kiri. Dengan mengisolasi pembacaan `currentPositionMs` hanya pada komponen *seekbar* dan lirik aktif, proses *scrolling* berjalan mulus di 60–120 FPS.

- **Decision 3: Penyimpanan Playlist & Offline Audio Terpusat di `Perpustakaan`**
  - *Chosen Approach*: Memindahkan seluruh daftar playlist hasil impor dan kustom ke state `Library` yang disimpan secara persisten di penyimpanan lokal perangkat, lengkap dengan layanan `MusicDownloadManager` yang mengunduh file audio ke `filesDir/offline_downloads/` dan mengirimkan pembaruan progres ke `NotificationManagerCompat`.
  - *Why*: Menjaga halaman `Home` tetap bersih dan cepat dimuat, sekaligus memastikan semua playlist pengguna dan lagu yang sudah di-download tetap utuh serta dapat diputar secara offline.

---

## 4. Technical Architecture & Data Strategy *(Technical Reference)*

### Architecture & Component Diagram

```
┌────────────────────────────────────────────────────────────────────────────┐
│                        MainActivity (OLED Root Host)                       │
│  • Isolasi State: Halaman utama tidak recompose saat currentPositionMs tick│
├──────────────────────┬───────────────────────────┬─────────────────────────┤
│     Home Tab         │    Perpustakaan Tab       │       Search Tab        │
│  (ListenNowScreen)   │   (BitChordLibraryScreen) │     (SearchScreen)      │
│  • Quick Picks 4-Row │  • Kartu Statistik Dengar │  • Debounced Search     │
│  • Pintasan Cepat    │  • Row: Downloads & Local │  • Filter Chips         │
│  • Dari Komunitas    │  • Rak Playlist:          │  • Menu (⋮) Tambah ke   │
│  • Bersih dari       │    [+] Playlist Baru      │    Playlist & Download  │
│    Playlist Impor    │    [S] Impor dari Spotify │                         │
│                      │    [👍] Musik Disukai     │                         │
│                      │    [📌] Pinned & Kustom   │                         │
│                      │  • Rak Artis & Podcasts   │                         │
└──────────┬───────────┴─────────────┬─────────────┴────────────┬────────────┘
           │                         │                          │
           ▼                         ▼                          ▼
┌────────────────────────────────────────────────────────────────────────────┐
│            Modal & Bottom Sheet Layer (Sans-Serif Bold/ExtraBold)          │
│  • NewPlaylistBottomSheet (Input Nama, Private/Unlisted/Public, Create)    │
│  • AddToPlaylistBottomSheet (Pilih Playlist / Buat Baru)                   │
│  • PlaylistOptionsSheet (Pin/Unpin, Rename, Download Semua, Delete)        │
│  • TrackOptionsSheet (Tambah ke Playlist, Download Lagu, Buka Artis)       │
└────────────────────────────────────┬───────────────────────────────────────┘
                                     │
                                     ▼
┌────────────────────────────────────────────────────────────────────────────┐
│         MusicDownloadManager & Offline Storage + Android Notification      │
│  • Mengunduh stream audio per lagu atau antrean seluruh playlist/album     │
│  • Menampilkan NotificationCompat Progress Bar (0% - 100%) di Status Bar   │
│  • Menyimpan file audio di internal storage & memutar langsung saat offline│
└────────────────────────────────────────────────────────────────────────────┘
```

### Data Model & State
- **`UserLibraryPlaylist` (`@Immutable`)**: Menyimpan `id`, `title`, `subtitle`, `thumbnailUrl`, `privacy` (`PRIVATE`, `UNLISTED`, `PUBLIC`), `isPinned` (`Boolean`), `source` (`CUSTOM`, `SPOTIFY_IMPORT`, `YOUTUBE_IMPORT`, `SAVED_PLAYLIST`), `createdAt`, dan `tracks: List<MusicItem>`.
- **`ListeningStatsState` (`@Immutable`)**: Menghitung secara otomatis `minutesListened`, `totalPlays`, `topSongTitle`, `topSongArtist`, dan `topSongPlays` berdasarkan riwayat pemutaran pengguna untuk ditampilkan pada kartu **Pengalaman Mendengarkan Anda**.
- **`DownloadedTrackItem` (`@Immutable`)**: Menyimpan metadata lagu yang telah diunduh beserta `localFilePath`, ukuran file, status unduhan (`DOWNLOADING`, `COMPLETED`, `FAILED`), dan `progressPercent` (`0..100`).

### Interactive Component & State Mapping
- **Pembuatan & Pengelolaan Playlist di `Perpustakaan`**:
  - Mengetuk kartu **Playlist baru (`+`)** membuka `NewPlaylistBottomSheet`. Saat pengguna mengisi nama, memilih privasi (`Private` / `Unlisted` / `Public`), dan menekan **Create playlist**, playlist baru langsung muncul di rak Playlist `Perpustakaan`.
  - Mengetuk **Pin Playlist** akan memindahkan playlist tersebut ke urutan terdepan setelah kartu aksi utama.
  - Mengetuk **Rename Playlist** membuka dialog ubah nama instan, dan **Delete Playlist** menghapus playlist dari koleksi.
  - Mengetuk tombol **Simpan ke Perpustakaan** pada halaman detail playlist/album mana pun akan menyimpan playlist tersebut ke rak Playlist di `Perpustakaan`.
- **Alur Download & Pemutaran Offline**:
  - Mengetuk **Download** pada menu titik tiga (`⋮`) lagu atau playlist memicu `MusicDownloadManager`.
  - Selama proses unduhan berlangsung, notifikasi Android menampilkan progres persentase (`%`) secara *real-time*.
  - Saat pengguna memutar lagu yang sudah terunduh (baik dari menu **Downloads** di `Perpustakaan` maupun dari playlist mana pun), `MusicPlayerController` otomatis memutar file lokal (`Uri.fromFile(...)`) secara instan tanpa membutuhkan koneksi internet.
