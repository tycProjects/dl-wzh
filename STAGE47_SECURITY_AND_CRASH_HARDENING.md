# Stage 47 — Penguatan izin tindakan dan ketahanan alur AI

Berdasarkan Stage 46.

## Perubahan
- Menambahkan `UserIntentActionGuard` sebagai pemeriksaan deterministik kedua terhadap setiap tindakan yang diusulkan model.
- Pemeriksaan selalu memakai pesan asli pengguna. Tindakan seperti mengubah pengaturan, menelepon, SMS, memaksa berhenti, shell, alarm, timer, berbagi, dan membuka fitur tertentu memerlukan kata/tujuan yang sesuai dari pesan pengguna.
- Langkah navigasi antarmuka tetap dapat berlanjut dalam Looping Agent untuk menyelesaikan tujuan yang memang diminta pengguna.
- Jika tindakan yang diajukan tidak cocok dengan maksud pengguna, tindakan ditolak dan aplikasi menjelaskan bahwa tidak ada tindakan yang dijalankan.
- Kegagalan/exception saat menjalankan satu langkah ditangkap sebagai hasil gagal agar tidak langsung menjatuhkan seluruh sesi AI; diagnostik hanya dicatat jika fitur diagnostik aktif.
- Saat Diagnostik nonaktif, ekspor tidak membaca berkas log dan penghapusan hanya membersihkan keadaan di memori tanpa operasi disk.
- Memperluas `ClickGuard` agar label tindakan berisiko dalam bahasa Indonesia dan Inggris (hapus/delete, bayar/pay, beli/buy, kirim/send, dan sejenisnya) tidak dapat ditekan tanpa kata yang sesuai dari pengguna.
- Menambahkan pengujian unit untuk izin tindakan berdasarkan pesan asli pengguna dan perlindungan label berisiko.

## Pemeriksaan
- Arsip Stage 46 valid sebelum perubahan.
- Pemeriksaan sintaks Kotlin untuk `UserIntentActionGuard` berhasil dengan kompilasi terisolasi memakai stub tipe Android.
- Enam pemeriksaan perilaku kebijakan berhasil dijalankan.
- `:app:testDebugUnitTest` belum dapat dijalankan: Gradle Wrapper perlu mengunduh Gradle 8.9, tetapi jaringan ke `services.gradle.org` tidak tersedia (`UnknownHostException`).
- Build Android penuh dan uji pada perangkat belum dilakukan. Karena itu belum dapat dinyatakan bebas crash; perlu build dan uji perangkat setelah ZIP ini dibuka di lingkungan Android/Gradle yang tersedia.
