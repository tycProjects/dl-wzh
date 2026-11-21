# Update 1.14.0 — Model Otomatis, Gambar, dan Tampilan Pesan

Desain: docs/superpowers/specs/2026-11-17-ai-panel-ui-design.md (revisi 1–5)
Rencana: docs/superpowers/plans/2026-11-17-ai-panel-models-images-ui.md

## Yang berubah
- Model chat Gemini dipilih otomatis dari daftar `/models`: versi stabil tertinggi keluarga Flash.
- Model gambar dipilih otomatis dari katalog terverifikasi sesuai penyedia yang diatur.
- Aksi baru `generate_image`: "buatkan gambar ..." membuat satu gambar dan menampilkannya di chat.
- Galat gambar memakai pesan Indonesia dengan langkah perbaikan (mis. verifikasi organisasi OpenAI).
- Preset penyedia memakai nama yang sudah diverifikasi (lihat revisi 5 di desain).
- Konstanta `gemini-3.8-flash` dihapus; nama itu tidak ditemukan di sumber resmi.
- Preset Together, Fireworks, dan Cerebras dikosongkan: default lamanya sudah dihentikan.
- Gambar yang dibuat tampil di gelembung pesan asisten.

## Belum ada di versi ini (Task 6 bagian Compose)
- Restyle panel sesuai desain: jejak langkah tampil di UI (logika `StepTrailModel` sudah ada dan teruji), tombol Berhenti, tipografi serif untuk jawaban, dan liquid glass.
- Tombol "Simpan" untuk gambar ke galeri. Saat ini gambar hanya ada di cache aplikasi.

## Status verifikasi
Test unit ditulis lebih dulu: ModelPickerTest, ImageResponseParserTest, ImageClientErrorTest, IntentResolverTest (generate_image), StepTrailModelTest. Sesi ini tidak punya Kotlin compiler atau Gradle, jadi test belum dijalankan. Jalankan `./gradlew testDebugUnitTest` dan `./gradlew assembleDebug`.

## Risiko yang perlu diuji di perangkat
- Gemini: endpoint gambar memakai root `normalizeApiRoot(...)` dengan `/models/{model}:generateContent`. Jika root berbeda, galat 404 akan muncul.
- Gambar tidak disimpan di riwayat chat; setelah aplikasi dibuka ulang, gambar hilang dari chat.

## Tambahan: UI selesai (menutup daftar "belum ada")
- Jejak langkah tampil di panel. Saat tugas berjalan, langkah muncul satu per satu lewat `_streamText`. Setelah selesai, jejak tetap tampil di pesan.
- Titik langkah aktif berdenyut. Jika animasi sistem dimatikan (ANIMATOR_DURATION_SCALE = 0), titik tetap solid.
- Jawaban asisten tanpa gelembung, memakai FontFamily.Serif dengan tinggi baris 23 sp. Pesan pengguna tetap pil.
- Tombol "Hentikan" menjadi "Berhenti" dengan warna Ember (#FF8A65).
- Judul panel "Asisten".
- Permukaan panel memakai `liquidGlass` hanya jika `DeviceProfile.renderTierState == NORMAL`. UNRESOLVED dan LOW_END memakai permukaan datar.
- Gambar punya tombol "Simpan ke galeri" yang menyimpan ke Pictures/AI Island lewat MediaStore, hanya saat diketuk.

## Keputusan yang diambil saat implementasi (ledger)
- Ruling: liquid glass hanya untuk tier NORMAL. Biaya jika salah: perangkat NORMAL yang tidak terdeteksi memakai permukaan datar (lebih hemat, kurang efek).
- Ruling: judul "Asisten" menggantikan "AI Island" sesuai desain. Biaya: identitas merek di panel berubah.
- Ruling: gambar tidak disimpan ke riwayat chat. Biaya: gambar hilang setelah aplikasi dibuka ulang.
- Ruling: teks status di header (mis. "Membaca layar") belum ditambahkan. Status tetap terlihat di jejak dan bubble "Membaca layar…". Biaya: kurang informatif di header.
