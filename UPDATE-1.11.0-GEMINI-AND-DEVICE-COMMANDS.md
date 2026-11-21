# Update 1.11.0 — Gemini, penyedia otomatis, perintah perangkat, perbaikan panel terpotong

## 1. Perbaikan: kolom input AI terpotong

**Akar masalah:** kartu AI ditarik di jendela overlay terpisah dari jendela sentuh. Jendela
sentuh dan `IslandRoot` sudah memakai tinggi **hasil ukur nyata** kartu (`state.aiPanelHeightDp`),
tapi jendela gambar (`drawWindowFixedHeightPx()` di `IslandAccessibilityService`) memakai angka
tetap `420dp`. Begitu konten sungguhan lebih tinggi dari 420dp — hint "AI belum diatur" tampil,
baris chip melipat dua baris, atau nama model panjang — jendela gambar lebih pendek daripada
kontennya, dan bagian bawah (kolom input + tombol kirim) terpotong persis di jendela itu, bukan
cuma tertutup keyboard.

**Perbaikan:** `drawWindowFixedHeightPx()` sekarang memakai `state.aiPanelHeightDp` yang sama
dengan jendela sentuh dan `IslandRoot`, dengan 420dp hanya sebagai jaga-jaga sebelum frame
pertama selesai diukur. Begitu panel terukur, jendela gambar otomatis ikut menyesuaikan —
termasuk saat keyboard aktif.

## 2. Dukungan Gemini / Google AI Studio

`AiClient` sebelumnya hanya bicara format `chat/completions` ala OpenAI. Gemini punya bentuk
request/response sendiri (key di URL, `contents`/`systemInstruction`, role `model` bukan
`assistant`, streaming `streamGenerateContent`). Ditambahkan sebagai jalur terpisah
(`geminiGenerateContent`) yang dipilih lewat field baru `IslandSettings.aiProvider`
(`"openai"` atau `"gemini"`), tanpa mengubah jalur OpenAI-compatible yang sudah ada.

## 3. Penyedia otomatis — pengaturan API yang lebih sederhana

Section **Asisten AI** kini punya baris chip **Penyedia**: OpenAI, Gemini, OpenRouter, Groq,
Ollama (lokal), Kustom. Pilih satu → Base URL dan Model terisi otomatis; tinggal isi API key
(Ollama boleh dikosongkan). "Kustom" mengembalikan kendali penuh ke tiga kolom manual yang sudah
ada (Base URL / API key / Model) seperti sebelumnya.

## 4. Perintah perangkat instan ala Galaxy AI

Ditik langsung di kolom chat, dua pola dikenali **secara lokal** (tanpa memanggil AI sama
sekali, jadi tetap jalan walau AI belum diisi):

- **Buka aplikasi** — `buka YouTube`, `jalankan WhatsApp`, `mulai Spotify` → dicocokkan ke label
  aplikasi terpasang (`PackageManager`, memakai `<queries>` MAIN/LAUNCHER yang sudah ada di
  manifest) dan langsung dibuka.
- **Nyalakan/matikan pengaturan** — WiFi, Bluetooth, data seluler, mode pesawat, lokasi, rotasi
  otomatis, senter, Jangan Ganggu. Urutan upaya:
  1. Senter dan Jangan Ganggu lewat API langsung (`CameraManager` / `NotificationManager`,
     Jangan Ganggu minta izin akses sekali seperti Accessibility).
  2. Sisanya lewat **Shizuku** (`ShizukuShell`, perintah shell satu baris — reflection yang sama
     persis dengan yang sudah dipakai `ShizukuRestart`) kalau Shizuku sudah diberi izin.
  3. Kalau Shizuku belum siap, membuka layar Pengaturan sistem yang sesuai agar user tinggal
     tap sekali secara manual.

File baru: `ai/AiCommandRouter.kt` (pengenal pola, regex/kata kunci sederhana — bukan model AI),
`ai/DeviceActions.kt` (eksekusi), `system/ShizukuShell.kt` (shell command sekali-jalan via
Shizuku).

**Keterbatasan yang diketahui:** pengenal pola memakai kata kunci sederhana ("buka", "nyalakan",
"matikan", dst.), bukan NLU. Kalimat panjang (>40 karakter setelah kata kerja) sengaja dilewati
ke AI normal; kalimat pendek yang kebetulan diawali kata tersebut tapi bukan perintah perangkat
("mulai kerja", dst.) bisa salah dianggap sebagai "buka aplikasi" dan gagal dengan pesan "tidak
ditemukan" — tidak merusak apa pun, hanya jawaban yang kurang pas, bukan diteruskan ke AI.

## Belum diverifikasi

Tidak ada Android SDK di lingkungan tempat perubahan ini dibuat, jadi **belum ada build Gradle
yang benar-benar dijalankan** — semua perubahan di atas baru diperiksa secara statis (struktur
kode, tanda kurung, nama API Android). Jalankan `./gradlew :app:compileDebugKotlin` (atau build
APK biasa) sebelum memasang ke perangkat.
