# Rencana Perbaikan UI, Optimasi Performa Scrolling, & Top Result Search

## 1. Tujuan & Gambaran Umum
Menyempurnakan tampilan visual halaman **Playlist/Album**, **Home (Listen Now)**, **Library (Local Music)**, dan **Search**, sekaligus mengoptimalkan performa *scrolling* agar tetap mulus (tanpa *frame drop* / *lag*) meskipun sampul lagu di pemutar musik utama tetap beresolusi HD (1080p).

---

## 2. Detail Fitur & Perbaikan

### A. Header Playlist & Album Menyatu Tanpa Batas Garis (`PlaylistDetailScreen.kt`)
- Menghapus perbedaan tinggi gambar sampul utama (`350.dp` di dalam `386.dp`) yang sebelumnya menyebabkan munculnya garis batas horizontal yang kaku.
- Menerapkan *alpha mask* (`CompositingStrategy.Offscreen` + `BlendMode.DstIn`) dan gradasi warna dominan (`rememberDominantAccentColor`) langsung dari sampul album/playlist sehingga bagian bawah dan tepi gambar memudar (*feathering*) dan menyatu 100% halus dengan latar belakang halaman yang mengikuti warna sampul.

### B. Perbaikan Halaman Home: "Hits Indonesia", "Rilis Baru", & "Pintasan Cepat" (`ListenNowComponents.kt`)
1. **Hits Indonesia**:
   - Menghapus badge merah **"LAGU TERPOPULER"** di pojok kiri atas kartu utama.
   - Menghaluskan transisi gradasi gambar di sisi kanan kartu utama dengan *alpha mask* horizontal sehingga tidak ada garis batas tegak di tengah kartu.
2. **Rilis Baru (Semua Kartu)**:
   - Menerapkan ekstraksi warna sampul adaptif (`rememberDominantAccentColor`) dan *seamless opacity gradient blend* pada **kartu utama di sebelah kiri** maupun **kedua kartu di sebelah kanan**, sehingga gambar sampul di setiap kartu menyatu mulus ke warna latar kartu tanpa garis batas kaku.
3. **Pintasan Cepat (Rasio 1:1 Kotak Penuh)**:
   - Mengubah tata letak kartu di bagian paling atas (**Pintasan cepat**) agar menggunakan rasio **1:1 persegi sempurna** (`aspectRatio(1f)`) dengan gambar sampul memenuhi seluruh kotak dan teks judul tampil di atas gradasi halus bagian bawah gambar, serta memastikan ikon thumbnail kecil di samping judul seksi *Pintasan cepat* juga 1:1.

### C. Optimasi Performa Scrolling Tanpa Lag / Drop FPS (`MusicModels.kt`, `ListenNowComponents.kt`, `PlaylistDetailScreen.kt`, `SearchPlaceholderScreen.kt`)
- Memisahkan resolusi gambar berdasarkan konteks:
  - **Daftar lagu, grid, dan carousel (`smallThumbnail` & `highResThumbnail`)**: Menggunakan ukuran ringan & tajam (`226x226` untuk baris daftar kecil dan `400x400` untuk kartu sedang) dengan `allowHardware(true)` dan ukuran *decode* piksel eksplisit di Coil agar GPU dan memori tidak terbebani saat *scrolling* vertikal maupun horizontal.
  - **Full Music Player (`ultraResThumbnail` / 1080p HD)**: Tetap menggunakan resolusi penuh HD (`1080x1080` + endpoint `/player`) khusus untuk sampul besar di halaman pemutar musik.
- Mengganti operasi `.blur()` real-time yang berat saat *scrolling* dengan *pre-sized downsampled blur* + *gradient feathering* yang jauh lebih ringan di GPU.

### D. Akses Instan ke Halaman "Local Music" Tanpa Delay (`MusicHomeViewModel.kt`)
- Mengubah `openLocalMusicPlaylist()` agar **langsung membuka halaman Local Music seketika (0ms delay)** menggunakan daftar lagu lokal yang sudah tersimpan di *state* (`_localDeviceTracks.value`).
- Pemindaian ulang penyimpanan perangkat (`scanLocalDeviceMusic()`) dijalankan secara *asynchronous* di latar belakang dan langsung memperbarui daftar lagu begitu selesai tanpa menahan navigasi layar.

### E. Kartu "Top Result" (Hasil Teratas) di Halaman Search (`SearchPlaceholderScreen.kt`, `MusicHomeViewModel.kt`, `YtMusicScraper.kt`)
- Saat pengguna memasukkan kueri pencarian lengkap dan menekan *Search*, bagian paling atas tab **All** menampilkan kartu besar **Top Result (Hasil Teratas)** sesuai referensi desain:
  - Latar kartu bernuansa warna adaptif dari sampul hasil teratas dengan *seamless image blend*.
  - Menampilkan sampul/avatar, judul utama tebal, subjudul tipe item (Artis / Lagu / Album / Playlist + info pendukung), serta dua tombol aksi utama (**Putar** & **Radio**).
  - Menampilkan daftar lagu terkait langsung di bagian bawah kartu *Top Result* sebelum masuk ke kategori hasil pencarian lainnya (*Songs*, *Albums*, *Artists*, *Playlists*).
- Seluruh teks tetap menggunakan font eksklusif **SFPRODISPLAYBOLD.OTF** (`SfProDisplayBold`).
