# Android Island 1.4.55

## Default baru (dari pengaturan yang dipakai)
Animasi: Opsi 3 Jelly, SPRING, HIGH, kecepatan 0,5x · Getaran halus aktif · Glass Sweep aktif, opacity 25%, durasi 0,7 s · Opasitas latar 55% · Durasi banner 8 s ·
Glow lirik OFF (intensitas 14%) · Shine lirik OFF (1,00x, 35%) · Font lirik 14 sp / 500 · Banner: compact aktif, indikator di kiri, lebar 376 dp (compact & non-compact), tinggi 88/58 dp, teks 17 sp / 600 ·
Mode iPhone Notch aktif (130 x 32 dp), sembunyikan saat idle, sharp notch, auto deteksi cutout, offset 0 · Expanded: lebar 360, tinggi min 164, skala 100%, sudut 24 dp, offset Y 8 dp ·
Hemat RAM & baterai aktif, Ultra Mode aktif · Bersihkan ikon status bar aktif · Tema warna Adaptive.
Default hanya berlaku pada instalasi baru atau setelah data aplikasi / pengaturan di-reset; nilai yang sudah tersimpan tidak ditimpa.

## Glass Sweep tidak lagi kotak-kotak
Sebelumnya sapuan digambar sebagai persegi panjang selebar pita dengan gradien diagonal, sehingga tepi kiri/kanan persegi masih terlihat. Sekarang:
- gradien digambar di seluruh kartu (tidak ada tepi persegi sama sekali),
- profil seperti gauss yang mencapai alpha 0 di kedua ujung (11 titik),
- pita miring ~24 derajat, lebih lebar (55%), dan memudar halus di awal/akhir lintasan.

## Glow lirik diperbaiki total
- Glow kini hanya menyala pada kata yang sedang dibaca: "kepala baca" bergerak di sepanjang baris sesuai waktu lirik, dengan ekor memudar di belakang dan sedikit cahaya di depan. Mendukung baris 2 baris. Tetap opsional (OFF secara default).
- Gerak kepala baca halus (glide linear di antara pembaruan posisi 4x/detik), snap saat seek, berhenti saat pause.
- Radius blur kini proporsional dengan ukuran font (sebelumnya 28-100 px tetap, jauh lebih besar dari teks 14 sp sehingga menjadi noda putih). Dua lapis (rapat + lebar) untuk efek bloom; mode hemat daya memakai satu lapis.
- Lapisan glow diberi margin agar tidak terpotong rata di batas teks.
- Tanpa lirik tersinkron, baris judul mendapat halo tipis merata.

Catatan: kode belum dikompilasi di lingkungan ini (tanpa Gradle/jaringan). Jalankan build debug dan kabari bila ada error.
