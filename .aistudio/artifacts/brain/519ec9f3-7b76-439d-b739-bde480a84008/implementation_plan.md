# Rencana Implementasi Pembaruan BitChord

Berikut adalah rencana pembaruan aplikasi berdasarkan permintaan dan konfirmasi Anda:

---

### 1. Desain Bottom Sheet Titik Tiga (`•••`) & Sleep Timer dari Bawah (Gaya iOS)
- **Tampilan Slide-Up dari Bawah (`BottomCenter`)**:
  - Mengubah menu titik tiga (`•••`) dan **Sleep timer** pada `FullMusicPlayerScreen` dari popup tengah menjadi **Bottom Sheet yang muncul dari bawah layar** (`fillMaxWidth()`, sudut atas melengkung `28.dp`, dan *drag handle pill* di bagian tengah atas) persis seperti gambar referensi nomor 1.
  - Menggunakan efek *frosted dark gradient* yang menyatu langsung di atas latar belakang player yang sudah buram (**tanpa menambahkan layer overlay gelap ganda**).
- **Daftar Menu Utama yang Sering Dipakai**:
  - **Header Lagu**: Menampilkan thumbnail lagu, judul lagu, dan nama artis di bagian atas sheet.
  - **Menu Utama**:
    1. `Convert to audio` / `Switch to video` (beralih antara mode sampul audio dan video musik)
    2. `Like` (menyukai/batal menyukai lagu)
    3. `Add to playlist` (menambahkan lagu ke playlist)
    4. `Download` (mengunduh lagu offline / melihat status unduhan / menghapus unduhan)
    5. `Play next` (putar lagu ini berikutnya)
    6. `Add to queue` (tambahkan ke antrean)
    7. `Open artist` (buka profil artis)
    8. `Sleep timer` (membuka bottom sheet Sleep Timer)
- **Bottom Sheet Sleep Timer**:
  - Muncul dari bawah layar dengan tombol panah kembali (`←`), judul **Sleep timer**, dan subjudul *Pause playback after a while*.
  - Daftar pilihan vertikal persis sesuai desain referensi:
    - `After this song`
    - `15 minutes`
    - `30 minutes`
    - `45 minutes`
    - `1 hour`
    - *(Serta opsi `Turn off sleep timer` saat timer sedang aktif)*.

---

### 2. Perbaikan Kualitas Gambar / Thumbnail HD di Music Player (Anti-Buram)
- **Upgrade Resolusi Dinamis (Sesuai Menu `[12] Get Thumbnail` Scraper)**:
  - Memperbarui fungsi resolusi gambar di `MusicModels.kt` dan `YtMusicScraper.kt`:
    - Untuk URL `googleusercontent.com` / `ggpht.com`: Mengubah parameter ukuran dinamis (`=w60-h60`, `=w120-h120`, `=w226-h226`, `=w544-h544`, dll.) menjadi **`=w1200-h1200-l90-rj`** (Ultra HD) khusus pada tampilan **Full Screen Music Player**, sementara daftar list tetap menggunakan resolusi ringan (`w226` / `w544`) agar performa aplikasi tetap cepat dan hemat memori.
    - Untuk URL `i.ytimg.com`: Mengambil resolusi tertinggi yang tersedia (`maxresdefault.jpg` / `sddefault.jpg` dengan *fallback* otomatis ke `hqdefault.jpg`) serta mengekstrak thumbnail resolusi tertinggi dari endpoint `player` (`videoDetails.thumbnail.thumbnails`) saat lagu diputar.

---

### 3. Auto-Scan Semua Musik di Penyimpanan HP (`Local Music`) dengan Konfirmasi Izin
- **Konfirmasi Izin Penyimpanan (Storage/Audio Permission)**:
  - Menambahkan izin `READ_MEDIA_AUDIO` (Android 13+) dan `READ_EXTERNAL_STORAGE` (Android 12 ke bawah).
  - Saat pengguna membuka **Local Music** di halaman Library (atau menekan tombol konfirmasi izin), aplikasi menampilkan dialog/konfirmasi izin akses penyimpanan terlebih dahulu.
- **Pemindaian Otomatis Seluruh File Audio**:
  - Setelah izin diberikan, aplikasi otomatis memindai **semua file audio/musik** yang ada di penyimpanan HP melalui `MediaStore.Audio.Media` (lengkap dengan metadata judul, artis, durasi, dan sampul album lokal jika ada) dan langsung menampilkannya di daftar **Local Music** untuk diputar.

---

### 4. Download Musik `.mp3` + Thumbnail Offline & Pengaturan Lokasi Simpan (Aplikasi vs Penyimpanan HP)
- **Unduh Musik Beserta Thumbnail Offline**:
  - Saat mengunduh musik untuk mode offline, aplikasi juga mengunduh dan menyimpan file gambar sampul (thumbnail HD) ke penyimpanan lokal (`offline_thumbs/`).
  - Saat musik diputar secara offline (tanpa internet), gambar thumbnail lagu tetap tampil jernih di daftar maupun di Music Player.
- **Pengaturan 2 Pilihan Lokasi Penyimpanan di Halaman Pengaturan (`Settings`)**:
  - Mengaktifkan menu **Storage & Offline Cache (Lokasi Penyimpanan Unduhan)** di halaman **Settings** dengan 2 pilihan:
    1. **Simpan di Aplikasi (Internal APK)**: File `.mp3` disimpan langsung di memori internal aplikasi tanpa masuk ke folder penyimpanan publik HP.
    2. **Simpan di Penyimpanan HP (Device Storage)**: File `.mp3` disimpan ke folder penyimpanan HP (`Music/BitChord/*.mp3`) sehingga muncul di penyimpanan perangkat.
- **Pemindahan Otomatis & Penghapusan Sinkron**:
  - **Migrasi Otomatis**: Saat pengguna mengubah pilihan lokasi penyimpanan di Pengaturan, semua file unduhan lama akan **dipindahkan secara otomatis** ke lokasi baru yang dipilih (dari Aplikasi ke Penyimpanan HP atau sebaliknya).
  - **Penghapusan Sinkron**: Saat pengguna menghapus musik unduhan dari aplikasi, file `.mp3` dan thumbnail-nya akan **otomatis terhapus** dari Penyimpanan HP (jika disimpan di storage) maupun dari memori internal aplikasi (sehingga ukuran memori APK langsung berkurang).

---

### 5. Penggunaan Penuh Font `SFPRODISPLAYBOLD.OTF` di Seluruh Aplikasi
- Memasang file font **`SFPRODISPLAYBOLD.OTF`** (`res/font/sf_pro_display_bold.otf`) sebagai satu-satunya font utama di `Type.kt` (`AppTypography`, `PlusJakartaSans`, dan `SpaceGrotesk` semuanya diarahkan ke `SFPRODISPLAYBOLD.OTF`).
- Memastikan seluruh teks di semua halaman aplikasi menggunakan font **`SFPRODISPLAYBOLD.OTF`** tanpa ada campuran font lain.
