# Android Island 1.4.66

## Animasi buka dan tutup sama-sama lambat
- Menutup kini memakai kecepatan yang sama dengan membuka (x0,8), sehingga keduanya sama-sama halus dan bisa dinikmati.

## Izin Telepon (READ_PHONE_STATE)
- Ditambahkan ke manifest dan ada baris "Telepon" di bagian Izin · Akses sistem. Ketuk untuk meminta izin.
- Dengan izin ini, panggilan masuk dan panggilan berlangsung dideteksi langsung dari status telepon, sehingga tetap tampil walau tidak ada notifikasi.
- Panggilan yang sudah masuk lewat notifikasi tidak dobel: jika banner dari notifikasi sedang tampil, deteksi telepon tidak mengganti tampilannya.
- Tanpa izin, aplikasi tetap bekerja seperti sebelumnya (hanya lewat notifikasi).
- Catatan: nomor/nama penelepon tidak ditampilkan dari status telepon, karena itu butuh izin baca riwayat panggilan yang tidak diminta.

## Profil Jelly keluar dan masuk ke capsule
- Jelly memakai gerak capsule yang sama dengan profil lain. Efek squash dan pantulannya tetap ada.

## Catatan
Kode belum dikompilasi di lingkungan ini. Jalankan build debug dan kirim log bila ada error.
