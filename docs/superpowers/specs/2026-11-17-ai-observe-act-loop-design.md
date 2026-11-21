# Desain: Loop Observasi–Aksi untuk Asisten AI Android Island

Status: MENUNGGU PERSETUJUAN. Belum ada kode yang dibangun.

## Tujuan dan alasan
Pengguna ingin asisten bisa menyelesaikan tugas berurutan dan bertahap, misalnya
"buka TikTok lalu scroll 2 kali" atau "buka Auralys, cari lagu X, lalu putar".
Daftar aksi sekaligus (versi 1.12.x) gagal ketika layar ternyata berbeda dari
perkiraan model. Loop observasi–aksi membuat model melihat hasil tiap langkah
sebelum memutuskan langkah berikutnya.

## Keputusan pengguna
- Pendekatan: loop observasi–aksi (opsi 2).
- Jika pengguna meminta sebuah tugas, asisten langsung mengeksekusi tanpa
  konfirmasi tambahan.

## Tujuan (goals)
- Asisten menjalankan tugas multi-langkah dengan membaca layar di antara langkah.
- Eksekusi langsung setelah permintaan pengguna, tanpa meminta konfirmasi per langkah.
- Setiap langkah tampil di chat sebagai log, sehingga pengguna melihat apa yang terjadi.
- Berhenti sendiri ketika tugas selesai, ketika batas tercapai, atau ketika macet.

## Bukan tujuan (non-goals)
- Tidak bekerja di latar belakang saat chat ditutup.
- Tidak memakai tangkapan layar (vision) pada loop ini.
- Tidak menambah perintah shell atau izin baru.

## Anti-tujuan (hal yang harus dihindari)
- Loop tanpa akhir yang menguras baterai atau biaya API.
- Mengulang aksi yang sama berkali-kali tanpa kemajuan.
- Aksi yang diam-diam mengirim pesan atau panggilan. Panggilan dan SMS tetap hanya
  membuka aplikasi dengan data terisi, dan pengguna yang menekan tombol akhir.

## Batasan (constraints)
- Hanya lewat layanan aksesibilitas yang sudah ada. Tidak ada izin baru.
- Setiap langkah adalah satu panggilan model. Maksimum 8 langkah per permintaan.
- Ringkasan layar harus kecil, sekitar 1500 karakter, agar biaya token terkendali.
- Lingkungan pengembangan sesi ini tidak punya Android SDK maupun Gradle, sehingga
  test tidak bisa dijalankan di sini. Verifikasi dilakukan pengguna di perangkat.

## Pendekatan yang dipertimbangkan
1. Daftar aksi sekaligus (sudah ada, rapuh untuk layar yang tidak terduga).
2. Loop observasi–aksi (dipilih): lebih andal, sedikit lebih lambat dan mahal.
3. Hibrid dengan resep deterministik (ditunda, bisa ditambahkan nanti sebagai jalur cepat).

## Bagian yang akan dibangun
1. `AgentLoopPolicy` (murni, tanpa Android): menghitung langkah, mendeteksi selesai,
   mendeteksi aksi berulang, dan menentukan kapan berhenti. Mudah diuji unit.
2. Protokol: envelope model mendapat field `"done": true|false`. Setiap putaran
   membawa ringkasan layar terbaru dari `readScreenText()`, dipangkas ke 1500 karakter.
3. Eksekusi: satu putaran menjalankan maksimum 3 aksi, lalu layar dibaca ulang.
   Pembatalan oleh pengguna tetap memakai `historyGeneration` yang sudah ada.
4. Log langkah tampil di chat, misalnya "Langkah 2: scroll ke bawah 1 kali".

## Keputusan yang saya ambil (silakan dikoreksi)
- Batas 8 langkah dan 3 aksi per putaran, sebagai angka awal yang bisa diubah.
- Dua aksi tetap tidak dieksekusi tanpa tombol pengguna: panggilan dan SMS.
- Jika dua kali berturut-turut aksi yang sama gagal, loop berhenti dan melapor.

## Yang diserahkan ke pembangun
- Implementasi detail `readScreenText` yang dipangkas.
- Penamaan dan struktur kelas Kotlin mengikuti pola kode yang ada.

## Tambahan yang saya usulkan (disetujui bersama desain)
1. Batal langsung: tombol Stop menghentikan loop di langkah berikutnya, termasuk saat sedang menunggu.
2. Batas waktu total 90 detik per permintaan, selain batas 8 langkah.
3. Layar login, kata sandi, dan OTP: asisten tidak pernah mengetik ke kolom kata sandi.
   Aksi `type` ditolak jika kolom yang fokus adalah kolom kata sandi, lalu asisten melapor
   kepada pengguna untuk mengisinya sendiri.
4. Penjaga tombol berisiko: tombol berlabel hapus, bayar, beli, uninstall, atau reset hanya
   diketuk jika kata itu juga muncul dalam pesan pengguna.
5. Jawaban model tanpa `done` true dan tanpa aksi dianggap selesai. JSON rusak dianggap berhenti,
   dan teks mentah ditampilkan sebagai jawaban.
6. Ringkasan akhir: jika loop berhenti karena batas, asisten menyebut apa yang sudah
   selesai dan apa yang belum.
7. Setiap langkah dicatat di IslandDiagnostics untuk pelacakan masalah.
8. Chat menampilkan "Membaca layar..." setiap kali layar dikirim ke penyedia AI, sehingga
   pengiriman itu tidak terjadi diam-diam.
