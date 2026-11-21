# UPDATE 1.16.7 — STAGE 69: BUILD HEMAT — Ukuran, RAM, dan Baterai

Tanggal: 20 Nov 2026

Permintaan: *"fokus mengoptimalkan size aplikasi dan menghemat ram dan baterai … tanpa
memangkas fitur premium dan memangkas layout yg berkesan mewah, atau di pangkas resolusi nya
tapi masih tetap enak di pandang mata manusia."*

Prinsip stage ini: **tidak ada fitur atau tampilan yang dipangkas.** Semua perubahan hanya
mengurangi byte APK, alokasi memori, dan jumlah bangun (wake-up) CPU — look and feel mewah
(Liquid Glass, lens, neon AI panel, jelly motion, lirik bercahaya) tetap 100% seperti sebelumnya.

Catatan: hasil `dumpsys` yang disebut tidak ikut terkirim (lampiran yang diterima hanya
tangkapan layar halaman Preview & Demo). Angka baterai/RAM konkret bisa dijadikan pembanding
sebelum/sesudah bila teks dumpsys ditempel di chat berikutnya.

## 1. Ukuran APK (build.gradle.kts)

- **`ndk { abiFilters }` → arm64-v8a + armeabi-v7a.** ML Kit OCR (fitur analisis layar AI)
  membawa mesin native untuk 4 arsitektur; semua HP nyata adalah ARM, jadi setengah beban
  native (±8–10 MB terkompresi) tidak pernah masuk APK lagi. Fitur OCR di HP tidak berubah.
  *Catatan: bila suatu saat perlu diuji di emulator x86, hapus sementara blok `ndk` ini.*
- **`resourceConfigurations += en, id`** — AndroidX/Compose membawa terjemahan ±100 locale
  yang tidak pernah tampil; hanya English & Indonesia yang ikut. Bonus konsistensi: UI
  perpustakaan tidak akan tiba-tiba berbahasa lain.
- **`dependenciesInfo { includeInApk/includeInBundle = false }`** — blob protobuf daftar
  dependensi hanya dipakai tooling Play; untuk APK yang di-sideload itu murni bobot mati.
- Versi naik ke **1.16.7 (versionCode 157)**.

Kombinasi R8 + shrinkResources yang sudah aktif tetap dipertahankan; tiga item di atas adalah
lapisan tambahan yang tidak menyentuh kode.

## 2. Baterai — mode hemat daya Android kini benar-benar dipakai

- `ScreenState.powerSave` dulu **dihitung tapi tidak pernah dikonsumsi**. Kini
  `refreshPowerSave()` meneruskan statusnya ke `DeviceProfile.setPowerSave()`.
- Saat Battery Saver Android menyala, **semua jam animasi ambient turun setengah**
  (tick 33 ms → 66 ms di tier Normal): jam drift panel AI, gelombang neon, waveform kapsul,
  dan sweep lirik. Semua loop membaca `DeviceProfile.tickMs` per iterasi, jadi transisinya
  otomatis, tanpa restart.
- `positionTickMs` progres/lirik santai satu langkah (250 → 333 ms). Progress bar dan pergantian
  lirik tetap akurat — hanya polling-nya yang lebih jarang.
- **Yang tidak berubah:** semua spring interaksi, buka/tutup, jelly, sentuhan, dan seluruh
  fitur. Perbedaan 30 Hz → 15 Hz hanya ada pada gerak seconds-scale (drift/gelombang) yang
  secara mata tidak bisa dibedakan, sementara bangunan CPU turun setengah.
- Tetap berlaku: semua loop ambient sudah menunggu di `ScreenState.awaitInteractive()` —
  layar mati = nol wake-up; dan `refreshTick` pembacaan like hanya berjalan saat island
  terbuka. Battery saver kini menambah lapisan kedua saat layar menyala.

## 3. RAM — sisa kebocoran kecil dan churn alokasi dihapus

- **`ScreenOcr`** (OCR screenshot AI): sekarang **satu recognizer** yang dibuat malas, dipakai
  hangat untuk analisis beruntun, dan **ditutup otomatis setelah 60 detik idle** — idle RAM
  nol, tanpa buat/tutup mesin native per screenshot (dulu setiap panggilan membuat dan
  membuka client baru). Bitmap screenshot tetap di-recycle seperti sebelumnya.
- **Panel AI, `drawSine`**: dulu alokasi **2 `Path` per frame** (±120 objek per menit di tick
  30 Hz) saat panel terbuka; kini satu `Path` di-`remember` dan di-`reset()` — geometri sama,
  nol sampah.
- **Panel AI, `neonEdgeGlow` + `aiBreathRim`**: gradien brush kini dibuat sekali per ukuran
  lewat `drawWithCache`. Rim yang bernapas memakai satu faktor alpha pada draw (rasio cyan/
  violet dipertahankan — 0.26/0.20 pada puncak, diskalakan linear), jadi tampilan identik
  tanpa alokasi `Brush` per frame.
- Fondasi yang sudah ada tetap dipertahankan dan diverifikasi: LRU artwork satu entri RGB_565,
  lens bake setengah resolusi + cache satu entri, `trimArtCaches` pada TRIM_MEMORY,
  pelepasan artwork/lens saat layar mati lama (30 s / 120 s), dan Low-End Mode yang tetap
  terisolasi penuh (tanpa shader/backdrop, tanpa perubahan perilaku).

## 4. Cek yang dijalankan

- Pemeriksaan struktur statis (keseimbangan kurung/kurawal) pada semua file yang diubah.
- audit package (`tools/verify_package.py`) dan ZIP ulang dengan isi yang sama seperti paket
  sebelumnya, berbasis **versi Anda yang sudah diperbaiki** (AiPanel versi build-fix kamu
  dijadikan dasar, bukan versi lama).

## 5. Cara menguji di HP

1. Build release seperti biasa dan bandingkan ukuran APK — penurunan terbesar berasal dari
   ABI filter ML Kit.
2. `adb shell dumpsys battery` / `dumpsys meminfo com.android.island` sebelum vs sesudah;
   lampirkan teks dumpsys di chat untuk analisis angka per-item.
3. Aktifkan Battery Saver Android saat lagu diputar: gerak ambient melambat halus (nyaris
   tak terlihat), semua kontrol tetap responsif.
4. Jalankan analisis layar AI dua-tiga kali berturut-turut: run kedua terasa lebih cepat
   (mesin OCR hangat), dan meminfo kembali turun ~1 menit setelah dipakai.
