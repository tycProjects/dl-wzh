# Android Island 1.15.2 — Tahap 9: Integrasi dan regresi

## Perubahan
- Kebijakan observe-act tidak menghentikan giliran hanya karena model mengirim `done=true` ketika masih ada aksi yang harus dijalankan. Hasil aksi tetap harus dikirim kembali untuk diamati.
- `AiSession` meneruskan flag `done` asli ke kebijakan loop agar keputusan terpusat.
- Deduplikasi berita baru menyimpan kunci judul/URL setelah hasil lolos kedua pemeriksaan. Hasil yang ditolak karena URL duplikat tidak lagi menekan hasil valid berikutnya.
- Normalisasi URL menghapus parameter pelacak umum, mempertahankan parameter query bermakna, dan tidak mengubah kapitalisasi path.

## Pengujian ditambahkan/diubah
- Loop: aksi tetap dijalankan ketika model sekaligus menandai tugas selesai.
- Berita: URL duplikat tidak meracuni deduplikasi judul.
- URL: parameter query bermakna tetap dibedakan; parameter pelacak diabaikan.

## Verifikasi
- Pemeriksaan statis atas perubahan dan struktur proyek dilakukan.
- Gradle unit test/build belum dapat dijalankan: wrapper mencoba mengunduh Gradle 8.9 dan gagal karena `UnknownHostException: services.gradle.org`.
- Belum ada uji provider/API live atau instalasi perangkat. ZIP ini checkpoint source code, bukan APK terverifikasi.
