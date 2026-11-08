# Android Island 1.4.65

## Heart (like) real-time
- Status like dibaca ulang berkala dari pemutar: tiap 1 detik saat island expanded, tiap 4 detik saat tidak. Jika kamu un-like di aplikasi musik, heart di island ikut berubah.
- Hanya memicu pembaruan UI saat nilai benar-benar berubah.
- Catatan: jika aplikasi musik tidak melaporkan rating sama sekali, status yang tampil berasal dari penyimpanan lokal dan tidak bisa mengikuti perubahan di aplikasi itu.

## Animasi buka dan tutup
- Panel sekarang tumbuh dari ukuran dan posisi capsule, dan menyusut kembali ke capsule. Berlaku untuk profil Stabil dan Flow (iOS). Profil Jelly tetap dengan squash-nya.
- Membuka sedikit lebih lambat (kecepatan pegas x0,8), menutup tetap cepat.

## Izin telepon
- Tidak perlu izin tambahan. Banner panggilan dibaca dari notifikasi panggilan lewat akses Notification Listener yang sudah diminta.
- READ_PHONE_STATE hanya dibutuhkan kalau ingin mendeteksi panggilan tanpa notifikasi. Belum ditambahkan.

## Catatan
Kode belum dikompilasi di lingkungan ini. Jalankan build debug dan kirim log bila ada error.
