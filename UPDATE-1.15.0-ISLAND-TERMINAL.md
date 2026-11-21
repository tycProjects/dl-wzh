# Android Island 1.15.0 — Hidden AI Terminal (ported from aShellYou)

## Apa yang berubah
- `system/IslandTerminal.kt` (baru): satu terminal tersembunyi, diport dari `ShellCommandExecutor` aShellYou.
  - Direktori kerja (cwd) bertahan antar perintah lewat `cd` (`cd` tunggal dilacak, `cd /x && ls` berjalan di cwd saat ini).
  - Output stdout/stderr dan exit code dibaca penuh lewat Shizuku (`ShizukuShell.captureResult`).
  - Satu sumber aturan: `ShellPolicy` (blokir operasi destruktif) dan `ConfirmPolicy` (perubahan `settings put` butuh "ya").
- `/shell` hanya membuka terminal untuk AI. Pengguna menyebut tujuan, AI memilih dan mengirim perintah lewat aksi shell, lalu menjelaskan output.
- Dihapus karena digantikan: `system/ShizukuTerminal.kt`, `ai/TerminalPolicy.kt` (+ testnya), `DeviceActions.shell()`, dan `AiSession.runTypedShell()`.
- Test baru: `IslandTerminalTest` (parsing `cd`).

## Batasan
- `cd` dilacak hanya untuk perintah `cd` tunggal. Perintah majemuk tidak mengubah cwd.
- Kebijakan read-only/confirm dari TerminalPolicy tidak lagi dipakai; yang tersisa adalah ShellPolicy + ConfirmPolicy.
- Belum dikompilasi di lingkungan ini (Gradle tidak bisa diunduh). Jalankan `:app:compileReleaseKotlin` dan `testDebugUnitTest`.

## Tambahan 1.15.0 (putaran kedua)
- **Banner telepon**: tombol Jawab selalu muncul untuk panggilan masuk. Urutan: aksi jawab notifikasi, lalu Shizuku `input keyevent 5`, lalu buka aplikasi telepon (`Controls.answerCall`). Belum diuji di perangkat.
- **Timer telepon dihapus** (kartu panggilan dan kapsul).
- **Banner WhatsApp**: setelah banner selesai, panel AI tidak dibuka lagi. Mekanisme `aiSuspendedByWhatsApp` dihapus; prompt AI diperbarui agar tidak lagi menyebut pemulihan panel.
- **Model/API/URL**: default dicek ke dokumentasi resmi (OpenAI gpt-6.1-sol, Gemini 3.8 Flash, DeepSeek deepseek-flash). Bug diperbaiki: model dipilih dari urutan alfabet (bisa `babbage-002`). Sekarang `ModelPicker.pickDefault` memilih dari daftar `/models` penyedia, model non-chat disaring, dan jika model tersimpan ditolak ("tidak ditemukan"/kosong) sesi memilih ulang sekali dan mengingatnya. xAI dikosongkan karena `grok-4.7` tidak terverifikasi. Suhu permintaan dibatasi maks 0,5.
- **Shell bebas**: `ShellPolicy` kini FATAL (selalu ditolak), CONFIRM (minta "ya"), selain itu bebas. Pencocokan hanya pada kata perintah, bukan argumen; semua `dumpsys` selalu jalan. Batas 4096 karakter, multi-baris diizinkan, timeout 60 dtk dan 24 rb karakter untuk dumpsys. Output terpotong ditandai jujur.
- **Kejujuran AI**: blok HONESTY di prompt sistem; duplikat dan aturan usang dibuang.
- Diuji: 24 tes logika murni (ShellPolicy, ModelPicker, ModelFallback) lolos dengan kotlinc. Kode Android (Compose, Shizuku, AiSession) belum dikompilasi.

## Perbaikan dari log (503 Gemini, 429 Groq)
- **Dua prompt sistem terkirim bersamaan.** `systemPrompt()` lama (sisa sebelum protokol JSON) berisi "anggap app sudah mengeksekusinya", yang memancing AI mengaku sukses tanpa hasil. Kini hanya konteks runtime (tanggal, instruksi pengguna, aturan file ZIP).
- **Prompt `SYSTEM` dipadatkan** dari ±3.400 menjadi ±1.700 token (nama field tetap cocok dengan parser). Riwayat yang dikirim 24 → 12 pesan. Penyebab 429 Groq: batas ITPM 7000/menit, satu permintaan 3.900–4.600 token.
- **Gemini 503 "high demand"**: AiClient kini mencoba model berikutnya dari katalog langsung sekali, bukan mengulang model yang sama. Log baru `AI_GEMINI_ROUTE` mencatat model yang benar-benar dipakai (log lama hanya mencatat model yang dikonfigurasi, jadi `gemini-3.1-flash-lite` di log belum tentu model yang dipanggil).
