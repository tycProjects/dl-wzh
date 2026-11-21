# Android Island 1.4.57

## Profil animasi (baru)
Di bagian Animasi ada pilihan profil yang mengatur gaya, elastisitas, kecepatan, dan sapuan kaca sekaligus:
- Lembut: Opsi 2 Flow, elastisitas rendah, kecepatan 0,7x
- Jelly Halus (rekomendasi): Opsi 3 Jelly, elastisitas tinggi, kecepatan 0,8x (pantulan penuh, sedikit lebih tenang)
- Stabil: Opsi 1, tanpa pantulan

## Glow lirik per kata
- Hanya kata yang sedang dibaca yang menyala, bukan per huruf. Batas kata dihitung dari jumlah karakter.
- Glow tidak lagi menyapu sebagai pita yang terus bergerak.

## Status like akurat
- Status like kini dibaca dari pemutar (rating hati atau jempol). Jika pemutar tidak melaporkan rating, penyimpanan lokal dipakai sebagai cadangan.
- Perubahan status like memicu pembaruan tampilan, jadi ikon hati tidak tertinggal.

## Pengaturan dikelompokkan
Label bagian diberi awalan kelompok:
- Izin · Akses sistem
- Kustomisasi · (warna, tampilan, compact, posisi & cutout, ukuran, banner WhatsApp, animasi)
- Sistem · (performa & baterai, status bar & Shizuku, preview & demo, cakupan)
- Kontrol · Gesture

## Catatan
Kode belum dikompilasi di lingkungan ini (tanpa Gradle/Kotlin). Jalankan build debug dan kabari bila ada error.
