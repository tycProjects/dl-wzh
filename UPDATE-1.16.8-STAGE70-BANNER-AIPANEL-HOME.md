# UPDATE 1.16.8 — STAGE 70: Logika Banner/Panggilan, Panel AI Lebih Panjang, Home Screen Robotik

Tanggal: 20 Nov 2026

Permintaan: *"perbaiki logika ketika ada telpon masuk dan ketika ada banner lalu masuk ke stack
… agar tidak patah-patah atau terpotong … bisa di panjangin lagi panjang panel AI nya … rombak
tampilan homescreen … lebih robotik … penjelasannya diperjelas untuk pengguna awam … perbaiki
toggle dan slider … hapus yang tidak terpakai … perbaiki tampilan banner non compact."*

## 1. Analisis dumpsys (mode normal, lagu diputar) — angka dan artinya

| Kategori | Nilai | Penilaian |
|---|---|---|
| TOTAL PSS | 140,6 MB | Normal untuk overlay Compose aktif (musik + WhatsApp + AI) |
| GL mtrack | 41,5 MB | Buffer permukaan jendela gambar + layer GPU; proporsi terbesar, inherent |
| Native Heap | 42,1 MB (dirty) | Runtime Compose + AGSL + kamera/lirik cache — sudah dibatasi LRU 1 entri |
| Java Heap | 16,5 MB | Sehat (heap 19,4 MB, free 9,7 MB) |
| Bitmap | ±7,3 MB | Sudah RGB_565 + dibatasi 320 px + recycle |
| SwapPss | 9,3 MB | Sebagian proses pernah di-swap; wajar |

Tidak ada kebocoran pada pola ini (heap free 9,7 MB, bitmap bounded). Porsi terbesar (GL 41 MB)
adalah biaya jendela gambar yang sengaja tetap full-height agar morph tidak tersendat
(pengorbanan yang didokumentasikan sejak 1.6.0); menyusutkannya saat idle memicu realokasi
permukaan di tengah animasi = justru "patah-patah". Stage 69 (baterai) + stage 70 (di bawah)
tetap menekan CPU/GPU per frame.

## 2. Logika banner / panggilan / stack — tidak patah, tidak terpotong

- **Interupsi mulus**: panggilan masuk (atau chat baru) saat kartu EXPANDED tidak lagi menukar
  konten dalam satu frame. Island morph turun ke capsule dulu (±0,55 s), lalu banner naik
  (`raiseBannerAfterMorph` di IslandBus, guard id + state, batal bila pengguna berinteraksi).
- **Crossfade konten banner**: banner WhatsApp ↔ banner panggilan, atau pesan baru menggantikan
  banner lama, kini fade 200 ms lewat `AnimatedContent` (IslandRoot). Slot per-identitas
  menyimpan konten lama selama fade; metadata id sama tetap live, slot lama dibuang setelah
  400 ms (tidak ada bitmap avatar yang tertahan).
- **Stack tidak terpotong lagi**: tinggi jendela gambar (service) dan cap stack (IslandRoot)
  kini menghitung angka yang PERSIS sama lewat `WindowTuning.drawWindowRequiredHeightDp`,
  termasuk tinggi stack terukur, panel AI, banner + quick-reply, dan cadangan jelly. Sebelumnya
  Compose memakai rumus lebih kecil, jadi stack 2–3 kartu terpotong datar di dasar jendela
  (tangkapan layar Anda). Window sekarang ikut tumbuh (maks tinggi layar) dan hanya stack yang
  benar-benar lebih tinggi dari layar yang scroll ke dalam.

## 3. Panel AI lebih panjang

- Pengaturan baru **"Panjang panel AI"** (320–880 dp, bawaan 520) di kartu Asisten AI —
  langsung terlihat tanpa masuk "Pengaturan lanjutan". Panel menjamin tinggi minimum; isi chat
  yang lebih tinggi tetap mengikuti isi sampai batas ±94% layar.
- `IslandSettings.aiPanelMinHeight` tersimpan di DataStore (migrasi aman: field baru).

## 4. Home screen dirombak (robotik + ramah pengguna awam)

- **Header robotik**: chip versi monospace `1.16.8`, subtitle bahasa manusia
  ("Pesan, musik, dan asisten AI dalam satu kapsul kecil di atas layar").
- **Header seksi robotik**: penanda gradien neon + huruf kapital monospace — tanpa animasi,
  nol biaya scroll.
- **Toggle baru `IslandSwitch`**: pegas spring + titik hijau "berdaya" + glow lembut saat ON
  (draw-phase, tanpa loop) menggantikan switch Material bawaan di SEMUA baris.
  Kartu seksi dapat garis tepi 1 dp tipis (kesan panel mesin).
- **Istilah teknis diterjemahkan**: "Style morph" → "Cara membuka island" (Pegas/Halus/Instan),
  "Elasticity" → "Kelembutan jelly" (Kaku/Sedang/Lunak/Sangat lunak), "Opsi 1/2/3" →
  "Stabil / Mengalir / Jelly", "Rekomendasi" → "Pemulihan · Kembalikan semua ke awal" dengan
  dialog yang jujur menyebut kunci AI ikut terhapus.
- **Dua mode performa diberi nama jelas**: "Mode hemat daya" vs "Mode grafik ringan (Low-End)"
  kini berdampingan di satu seksi Performa — tidak lagi tertukar.
- **Cutout tanpa jalur ganda**: slider/chip manual hanya muncul saat deteksi otomatis dimatikan.
- **Dibersihkan**: kolom mati kosong di seksi capsule dihapus; rentang "Batas panjang jawaban"
  diperbaiki (dulu 4096–8192 = hanya bisa naik; kini 1024–16384).
- Slider tetap memakai `SettingSlider`/`AestheticSlider` (tick haptic, throttle 11 tulisan/detik,
  chip Reset) — perilaku hemat baterai dipertahankan.

## 5. Banner non-compact (standar) lebih mudah dibaca

- Baris identitas: ikon WhatsApp mini + label "WhatsApp" di kiri atas kartu.
- Tombol berlabel: **Dibaca** (DoneAll) dan **Balas** (Reply) sebagai chip tenang,
  **Buka** sebagai pil hijau WhatsApp terisi — semua aksi terbaca tanpa menebak ikon.
- Kartu compact tetap persis seminkyu sebelumnya (tanpa baris tambahan).
- Semua gerakan lama utuh: drag tuck, swipe horizontal dismiss, reply inline, ghost keluar.

## 6. Verifikasi

- 67 pemeriksaan struktural/regresi `tools/verify_package.py` LULUS; keseimbangan kurung 12
  file yang disentuh diperiksa skrip.
- Tidak bisa dikompilasi di lingkungan ini (tanpa Android SDK/Gradle) — sama seperti sebelumnya;
  silakan build dan laporkan error apa pun bila muncul.

## 7. Catatan uji cepat

1. Demo "WhatsApp grup" → stack 3 kartu harus utuh sampai kartu terbawah.
2. Saat media expanded, jalankan "Demo panggilan masuk": kartu turun halus ke capsule →
   banner panggilan naik — tanpa kedip.
3. Banner WhatsApp aktif → demo panggilan: banner chat memudar halus, banner panggilan naik.
4. Asisten AI: geser "Panjang panel AI" ke kanan → panel membuka tinggi; chat panjang tetap scroll.
5. Home screen: cek toggle baru (pegas + glow), chip animasi berbahasa Indonesia, seksi
   Performa berisi dua mode performa berdampingan.
