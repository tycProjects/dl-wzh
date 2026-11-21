# Android Island 1.15.2 — AI UX human-first

## Tujuan
Menyederhanakan pengaturan Asisten AI agar pengguna yang tidak memahami istilah teknis tetap dapat menghubungkan AI tanpa harus mengatur endpoint atau model secara manual.

## Perubahan
- Mengubah penjelasan Asisten AI menjadi bahasa yang berorientasi pada tindakan pengguna.
- Memisahkan kebutuhan inti chat (penyedia, kunci akses, uji koneksi) dari fitur opsional.
- Menyatakan bahwa Accessibility hanya diperlukan untuk analisis layar, bukan untuk chat biasa.
- Mengganti label verifikasi teknis menjadi **Uji koneksi chat** dan menampilkan status koneksi yang lebih jelas.
- Tombol uji kini memeriksa daftar model lalu mengirim satu pesan singkat memakai model terpilih; UI memberi tahu bahwa provider mungkin menghitung sedikit penggunaan.
- Memindahkan alamat server, kunci/provider cadangan, pilihan model manual, instruksi khusus, dan pengaturan generasi ke bagian **Pengaturan lanjutan**.
- Mengizinkan pemeriksaan endpoint lokal tanpa API key untuk alamat loopback dan jaringan privat LAN, sesuai pola autentikasi server lokal.
- Menghapus klaim bahwa Groq selalu gratis; batas penggunaan mengikuti paket akun provider.
- Menjelaskan setup dari panel chat dengan langkah yang singkat.

## Catatan verifikasi
- Uji smoke Kotlin untuk `AgentLoopPolicy`, `NumberGuard`, `RateLimitPolicy`, dan `PublicApis` lulus.
- Pemeriksaan parser Kotlin untuk file Compose tidak menemukan diagnostic sintaks, tetapi kompilasi mandiri tidak dapat selesai karena dependency AndroidX Compose tidak tersedia pada pemanggilan tersebut.
- Build Gradle penuh belum dapat dijalankan: Gradle 8.9 belum tersedia lokal dan pengunduhan `services.gradle.org` gagal karena DNS (`UnknownHostException`).
- Karena build penuh belum berhasil, perubahan UI ini belum dinyatakan terverifikasi di perangkat Android.
