# Android Island 1.4.58

## Panggilan telepon (baru)
- Notifikasi dengan kategori panggilan (masuk maupun sedang berlangsung) tampil sebagai kartu expanded.
- Kartu menampilkan nama, status, dan tombol Terima / Tolak (atau Akhiri saat berlangsung).
- Tombol memakai PendingIntent dari notifikasi panggilan itu sendiri, jadi panggilan tetap ditangani sistem.
- Saat panggilan berlangsung, durasi ditampilkan dan diperbarui tiap detik.
- Setelah panggilan berakhir, island kembali ke media (jika sedang ada) atau tersembunyi.
- Panggilan tidak bergetar tambahan; dering tetap dikendalikan sistem.

## Lirik
- Waktu per kata diperkirakan dari panjang kata dalam satu baris, karena sumber lirik (LRCLIB) hanya memberi waktu per baris.

## Hemat RAM
- Artwork dikecilkan ke maksimal 480 px sebelum disimpan. Island hanya menampilkan sekitar 46 dp, jadi sisanya tidak perlu disimpan di memori.
- Latar belakang dynamic island sudah dicache per gambar dan dibuat di thread latar belakang, jadi tidak diubah lagi.

## Animasi
- Profil Jelly sedikit lebih berpegas (rasio redaman 0,50 -> 0,46) agar efek jelly lebih terlihat.

## Catatan
Kode belum dikompilasi di lingkungan ini (tanpa Gradle/Kotlin). Jalankan build debug dan kirim log bila ada error.
