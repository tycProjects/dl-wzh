# Desain: UI Panel AI Android Island

Status: MENUNGGU PERSETUJUAN. Belum ada kode UI yang diubah.

## Konteks dan tujuan
Panel AI tampil di dalam island di bagian atas layar. Pengguna memakainya untuk
perintah cepat, membaca jawaban singkat, dan sekarang juga mengikuti langkah-langkah
asisten saat ia menjalankan tugas berurutan. Tujuannya: mudah dibaca sekilas, dan
jelas terlihat apa yang sedang dikerjakan asisten.

## Pengguna dan pekerjaan utama
Pengguna Indonesia yang memakai HP sehari-hari. Pekerjaan utama panel: mengetik
perintah, lalu melihat asisten mengerjakannya langkah demi langkah dan hasil akhirnya.

## Tujuan (goals)
- Status "sedang apa" selalu terlihat tanpa membaca chat panjang.
- Langkah multi-langkah tampak sebagai urutan yang nyata.
- Jawaban asisten mudah dibaca, bukan tampil sebagai kartu-kartu identik.
- Tombol berhenti selalu mudah dijangkau saat asisten bekerja.

## Bukan tujuan (non-goals)
- Tidak mengubah logika AI, protokol, atau aksi perangkat.
- Tidak menambah font dari luar aplikasi (menjaga ukuran APK dan RAM).
- Tidak mengubah island di luar panel AI.

## Anti-tujuan
- Tampilan generik kit kartu SaaS: setiap elemen dalam kartu radius sama dengan bayangan sama.
- Animasi yang tidak menjawab tindakan pengguna.
- Label huruf kapital berjarak lebar di atas setiap judul.

## Konsep: "jejak langkah"
Elemen yang diingat adalah jejak langkah: satu garis vertikal tipis yang menghubungkan
setiap langkah asisten, dengan satu titik yang bergerak ke langkah yang sedang berjalan.
Urutan di sini memang nyata (langkah 1, 2, 3), jadi penomoran dan garis waktu punya
arti. Elemen lain dibuat tenang agar jejak ini menonjol.

## Palet (6 warna, dari palet aplikasi yang sudah ada)
- Tinta: #0E0E12, latar panel
- Kaca: #1C1C22, permukaan input dan chip
- Lilac: #B8B4F8, warna aksen utama dan titik jejak langkah
- Teks: #F0EDF5, teks utama (kontras di atas Tinta sekitar 16:1)
- Redup: #9B98A6, teks sekunder (kontras sekitar 6:1)
- Ember: #FF8A65, berhenti dan kesalahan. Berbeda dari merah Tile agar tidak tertukar

Tidak memakai krem, terakota, hijau neon, atau gradasi dekoratif.

## Tipografi
- Teks pengguna dan status: sans-serif sistem (Roboto), medium untuk judul status.
- Jawaban asisten: serif sistem (FontFamily.Serif), tinggi baris 1.5 kali. Ini memberi
  jawaban karakter tersendiri tanpa menambah font.
- Skala: 12 / 14 / 16 sp. Lebar baris maksimal sekitar 70 karakter.
- Tidak ada huruf kapital berjarak lebar, tidak ada label kecil di atas judul.

## Tata letak (ASCII)

    ┌──────────────────────────────┐
    │ Asisten        ● Membuka... │  status: langkah berjalan
    ├──────────────────────────────┤
    │             buka tiktok  ▐   │  pesan pengguna, kanan
    │ ┃ ● Membuka TikTok           │  jejak langkah
    │ ┃ ○ Tunggu 2 detik           │
    │ ┃ ○ Scroll ke bawah 2 kali   │
    │                              │
    │ Oke, TikTok sudah terbuka... │  jawaban, serif, tanpa gelembung
    ├──────────────────────────────┤
    │ [ Ketik perintah...   ] (➤)  │  input, tombol Kirim
    │  Bersihkan   Analisis layar   │  aksi kecil di bawah
    └──────────────────────────────┘

Perataan: teks kiri, pesan pengguna kanan (konvensi chat yang sudah dikenal).

## Komponen yang berubah
1. Header: nama "Asisten" dan baris status yang menyebut langkah aktif
   ("Membuka TikTok, langkah 1 dari 3"). Menggantikan label dekoratif.
2. Jejak langkah: menggantikan pesan log teks "Langkah 1: ..." yang sekarang menumpuk.
3. Jawaban asisten tanpa gelembung. Hanya pesan pengguna yang berbentuk pil.
4. Chip aksi: dari empat chip berwarna menjadi satu baris teks kecil, tanpa warna per chip.
   Tinggi sentuh minimal 48 dp.
5. Tombol Berhenti: muncul menggantikan tombol Kirim saat asisten bekerja, warna Ember.
6. Pesan kesalahan menjelaskan langkah perbaikan, dalam bahasa antarmuka. Contoh:
   "Accessibility belum aktif. Nyalakan di Pengaturan > Aksesibilitas > Android Island."

## Gerak
Satu momen yang terorkestrasi: titik jejak bergerak ke langkah berikutnya saat langkah
selesai. Jawaban muncul dengan fade pendek saat selesai, sebagai respons atas tindakan
pengguna. Tidak ada animasi hover atau fade-in acak di setiap elemen.
Jika pengaturan animasi sistem dimatikan (ANIMATOR_DURATION_SCALE = 0), gerak dihilangkan.

## Kualitas minimal
- Kontras teks minimal 4.5:1.
- Semua target sentuh minimal 48 dp.
- Responsif: panel tetap rapi di layar sempit dan saat keyboard terbuka.
- Gerak dihormati saat pengaturan animasi dimatikan.

## Tinjauan terhadap pola generik
- Bukan krem/terakota, bukan neon di hitam: palet berasal dari aplikasi yang sudah ada.
- Bukan kit kartu SaaS: hanya pesan pengguna yang berbentuk, jawaban tanpa kartu.
- Tanpa gradasi dekoratif, tanpa label kapital, tanpa "WORD — fragment", tanpa "→".
- Satu elemen yang diingat (jejak langkah). Sisanya sengaja dibuat diam.

## Keputusan yang saya ambil (silakan dikoreksi)
- Serif hanya untuk jawaban asisten, bukan untuk judul. Ini keputusan yang paling bisa
  dibantah: jika terasa terlalu editorial untuk island, kembali ke sans.
- Tidak menambah font dari luar, karena ukuran APK dan RAM adalah perhatian utama proyek.
- Pesan kesalahan tidak meminta maaf dan tidak samar: langsung menyebut apa yang harus dilakukan.

## Yang diserahkan ke pembangun
- Penempatan tepat jejak langkah terhadap teks dan lebar yang tersedia di island.
- Perilaku status saat beberapa langkah selesai bersamaan.

---

## Revisi 2 (menunggu persetujuan)

### Nuansa AI
- Jawaban muncul sebagai teks yang mengalir dengan kursor kecil berkedip di ujungnya
  selama asisten menulis. Ini sinyal "sedang berpikir" yang lebih jujur daripada spinner.
- Saat asisten bekerja, titik pada jejak langkah berdenyut lembut. Berhenti saat selesai.
- Judul panel memakai "Asisten" dengan titik lilac kecil, tanpa ikon robot atau kilau
  dekoratif.

### Pembuatan gambar
Pengguna cukup menulis "buatkan gambar kucing oranye di pantai". Asisten memakai aksi baru
`generate_image` dengan satu field `prompt`. Hasil tampil di panel. Gambar disimpan ke
galeri hanya jika pengguna menekan "Simpan". Tidak ada penyimpanan diam-diam.

Pilihan implementasi (saya rekomendasikan pilihan 1):
1. Memakai penyedia yang sudah diatur pengguna. Penyedia `gemini` memakai model gambar
   Gemini, penyedia `openai` memakai endpoint gambar OpenAI. Penyedia lain menampilkan
   pesan "belum mendukung gambar". Tidak perlu pengaturan baru, dan kunci API yang sama
   dipakai.
2. Pengaturan penyedia gambar terpisah (URL, kunci, model sendiri). Lebih fleksibel, tetapi
   menambah layar pengaturan dan kebingungan.

Batasan: satu gambar per permintaan, tidak ada loop otomatis, dan biaya dicatat di log
diagnostik. Pengguna diberi tahu jika gambar gagal, dengan alasan dan langkah perbaikan.

### Liquid glass
Panel memakai modifier `liquidGlass` dan profil `EXPANDED` yang sudah ada, sehingga tidak
ada shader baru. Pada mode LOW_END (ditentukan oleh `RenderTier` yang sudah ada), panel
memakai permukaan Tinta yang datar. Ini sejalan dengan perhatian proyek pada RAM.

---

## Revisi 3 (menunggu persetujuan): pemilihan model otomatis

### Keputusan pengguna
Pemilihan model gambar otomatis. Tidak ada pengaturan penyedia gambar terpisah.

### Cara kerja
1. Asisten memakai kunci dan penyedia yang sudah diatur pengguna.
2. Daftar model diambil dari endpoint model penyedia (`discoverModels` yang sudah ada).
3. Dari daftar itu dipilih model gambar dengan urutan prioritas dari katalog di bawah.
4. Jika penyedia tidak punya model gambar, panel menampilkan pesan yang menjelaskan itu,
   tanpa mencoba model lain secara diam-diam.
5. Jika model gagal dengan galat akses (misalnya organisasi belum terverifikasi), panel
   menyebut penyebabnya dan langkah perbaikannya, lalu tidak mencoba model lain yang
   berbiaya sama.

### Katalog gambar terverifikasi (dicek lewat pencarian, November 2026)
- OpenAI, endpoint generations: `gpt-image-2` (utama), `gpt-image-1.5`, `gpt-image-1-mini`
  (paling murah). Model GPT Image memerlukan verifikasi organisasi di konsol OpenAI.
- Gemini: `gemini-3.1-flash-image` (GA sejak 28 Mei 2026, utama),
  `gemini-3.1-flash-lite-image` (30 Juni 2026, paling cepat).

Penyedia lain (OpenRouter, Groq, Ollama, dan lainnya) tidak dijamin punya model gambar,
sehingga diperlakukan seperti aturan nomor 4.

### Katalog chat
Konstanta `GEMINI_DEFAULT_MODEL = "gemini-3.8-flash"` di kode sekarang belum terverifikasi
dan tidak cocok dengan daftar Gemini di atas. Pada tahap rencana, nama ini wajib dicek ulang
sebelum diganti. Daftar model chat per penyedia juga perlu diverifikasi ulang dengan
pencarian. Pada revisi ini saya belum memverifikasi daftar chat secara lengkap, jadi
angkanya tidak saya masukkan ke desain.

### Batasan
- Satu gambar per permintaan, tanpa loop otomatis.
- Biaya gambar dicatat di IslandDiagnostics.
- Gambar disimpan ke galeri hanya saat pengguna menekan "Simpan".

---

## Revisi 4 (disetujui): hasil verifikasi katalog model chat

Verifikasi lewat pencarian, November 2026. Status per penyedia:

### OpenAI (chat)
- Konflik antar sumber. Changelog resmi OpenAI (Agustus 2026) menyebut `gpt-5.6-sol`,
  `gpt-5.6-terra`, `gpt-5.6-luna`, dan alias `chat-latest`. Agregator (OpenRouter, BenchLM)
  menyebut `GPT-6.1 Terra`, `GPT-6 Luna`, dan `GPT-6 Cyber`.
- Keputusan: tidak menulis salah satu nama sebagai fakta. Default `gpt-6.1-terra` di preset
  aplikasi dianggap belum terverifikasi.

### Gemini (chat)
- Terverifikasi di halaman resmi ai.google.dev: `gemini-3.5-flash` (stabil),
  `gemini-3.1-flash-lite` (stabil, dihentikan Mei 2027), `gemini-3.1-pro-preview` (preview).
- Konflik: sebagian halaman menyebut `gemini-3.6-flash` atau `gemini-3.7-flash` sebagai Flash
  terbaru. Keduanya belum bisa dipastikan.
- `gemini-3.8-flash` (default preset dan konstanta di kode) tidak ditemukan di halaman resmi
  mana pun. Dianggap tidak valid sampai ada bukti.
- Model lama yang perlu dihindari: `gemini-2.5-pro` (dihentikan 16 Oktober 2026),
  `gemini-3-pro-preview` (sudah dihentikan).

### Penyedia lain (DeepSeek, xAI, Mistral, Groq, OpenRouter, Together, Fireworks, Cerebras)
Belum diverifikasi. Nama default di preset (`deepseek-v4-flash`, `grok-4.7`, dll.) jangan
dipercaya sebelum dicek.

### Keputusan desain yang berubah akibat verifikasi
1. Daftar chat tidak ditulis sebagai konstanta statis dari ingatan.
2. Model dipilih saat runtime dari endpoint `/models` penyedia. Aturannya: ambil model dengan
   versi tertinggi dalam keluarga yang sama (misalnya Flash stabil terbaru), dan abaikan
   yang berakhiran `-preview`, `-live`, `-tts`, `-image`, `-embedding`, atau `-transcribe`
   kecuali diminta.
3. Preset default hanya dipakai bila endpoint `/models` gagal, dan nama preset itu harus
   sudah diverifikasi sebelum masuk kode.
4. Katalog gambar (`gpt-image-2`, `gemini-3.1-flash-image`) tetap, karena sudah terverifikasi.

---

## Revisi 5 (hasil verifikasi model chat, November 2026)

| Penyedia | Default yang dipakai (1.14.0) | Status | Sumber |
|---|---|---|---|
| OpenAI | `gpt-6-astra` (dipilih otomatis lewat regex `GPT_ASTRA`) | Terverifikasi | Changelog resmi developers.openai.com; halaman model `gpt-6-astra` |
| Gemini | `gemini-3.5-flash` (dipilih otomatis lewat `GEMINI_FLASH`) | Terverifikasi | ai.google.dev, daftar model stabil |
| OpenRouter | `~openai/gpt-latest` | Terverifikasi (alias resmi) | openrouter.ai/models |
| Groq | `openai/gpt-oss-120b` | Terverifikasi (produksi) | console.groq.com/docs/models |
| DeepSeek | `deepseek-v4-flash` | Terverifikasi | Dokumentasi DeepSeek, Apr 2026 |
| Mistral | `mistral-small-latest` | Terverifikasi (alias -latest) | docs.mistral.ai |
| xAI | `grok-4.7` | Terverifikasi | docs.x.ai/developers/models |
| Together AI | kosong | Default lama `Llama-3.3-70B-Instruct-Turbo` sudah dihentikan dari serverless | docs.together.ai changelog |
| Fireworks | kosong | Default lama `llama-v3p1-70b-instruct` tidak ada di daftar aktif; Llama 3.3 dihentikan 14 Mei 2026 | docs.fireworks.ai changelog |
| Cerebras | kosong | `llama-3.3-70b` dijadwalkan dihentikan 16 Feb 2026 | Pengumuman deprekasi Cerebras |
| Ollama, Kustom | kosong | Lokal, pengguna memasukkan nama sendiri | - |

Catatan: `gpt-6.1-terra` dari preset lama dikeluarkan karena hanya ditemukan di agregator.
Model gambar tidak berubah (`gpt-image-2`, `gemini-3.1-flash-image`, dst.).
