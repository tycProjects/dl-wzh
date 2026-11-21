# Stage 57 — Chat cepat, lirik andal, capsule/banner pulih

## Verifikasi perbaikan Stage 51–56
- Stage 54 (mode eksplisit, awalan `. `): terverifikasi di source — hanya `agentRequested` yang mengaktifkan agen.
- Stage 56 (lirik): klaim "kegagalan jaringan tidak membuat lirik palsu" TIDAK sepenuhnya benar. `get()` menelan semua
  error sehingga gangguan jaringan tetap tersimpan sebagai "tidak ada lirik" 10 menit. Diperbaiki di bawah.
- Stage 56 (capsule): pemulihan callback no-op ada, tetapi akar masalah (state `Ai` basi) belum tersentuh. Diperbaiki di bawah.

## Percakapan biasa (lambat)
- `doSend` memaksa `streaming = false` walau pengaturan streaming ada → jawaban baru tampil setelah selesai penuh. Chat biasa
  kini memakai `settings.aiStreaming`; agen tetap non-streaming (butuh satu JSON utuh).
- Skema 37 tool agen ikut terkirim di tiap chat biasa. Kini `AiEndpoint.allowTools` hanya true untuk mode agen
  (juga diteruskan ke profil API cadangan).
- Riwayat yang dikirim dibatasi 24.000 karakter (2 pesan terakhir selalu ikut) agar chat panjang tidak makin lambat.
- Fallback: relay yang mengabaikan `stream=true` dan membalas JSON utuh kini tetap terbaca (sebelumnya "jawaban kosong").

## Capsule/banner hilang setelah lama memakai AI (akar masalah)
- `closeAi()` tanpa media memanggil `hide()`, yang hanya mengubah `islandState`; `current` tetap `Ai`. Setelah itu semua media
  dianggap "menunggu di belakang chat AI" dan tidak pernah tampil lagi. Kini `closeAi()`/`hide()` membuang aktivitas `Ai`,
  dan ada guard `aiForeground()` (Ai hanya foreground bila tidak HIDDEN) sebagai pertahanan.
- Pembaruan media saat AI terbuka kini disimpan sebagai `pendingMedia` (sebelumnya dibuang/di-null-kan) agar menutup AI
  memulihkan lagu terbaru.
- Telepon yang mengambil alih kartu AI kini melepas flag `aiTyping`/`externalAi` (sebelumnya macet sehingga tap di luar
  tidak menutup kartu apa pun). `aiTypingActive` hanya berlaku bila `current` = Ai.
- Media yang masih diputar tetapi island HIDDEN dengan `current` = Media dikembalikan ke COMPACT.
- Dedupe WhatsApp: notifikasi tanpa timestamp pesan menekan teks identik ("OK") secara permanen. Kini hanya 60 detik.

## Lirik
- `get()` kini melempar IOException untuk error jaringan/timeout/429/5xx dan hanya mengembalikan null untuk 404/4xx.
  `find()` membedakan "provider tak terjangkau" (IOException, coba ulang 20 dtk, TIDAK di-cache) dari "memang tidak ada" (10 menit).
  Hanya LRCLIB yang menentukan "tidak ada"; provider yang melewati jaringan (album kosong) tidak dihitung.
- Listener tidak lagi menyimpan hasil kosong permanen setelah gagal; mencoba lagi ≥30 dtk kemudian. CancellationException tidak lagi ditelan.
- Provider dijalankan paralel dengan batas total 9 dtk (sebelumnya berurutan: bisa >15 dtk sebelum LRCLIB dicoba). Prioritas hasil tetap sama.
- Artis kosong: tidak lagi menolak semua hasil LRCLIB; memakai pencarian judul saja.
- Baris kredit (作词/作曲/Lyrics by/Composer/…) dan header "judul – artis" di tepi lirik disaring; tag kata LRC enhanced `<mm:ss.xx>` dibuang.
- UI: baris pertama tidak lagi muncul saat intro sebelum timestamp-nya.

## Verifikasi
- Pemeriksaan sintaks tree-sitter pada semua file yang diubah: bersih (satu temuan di AiSession.kt sudah ada di zip asli).
- Build Gradle/unit test TIDAK dijalankan (tidak ada Android SDK/Gradle di lingkungan ini). Uji di perangkat tetap diperlukan.
- Tes baru: tag kata LRC enhanced tidak tampil sebagai teks lirik.
