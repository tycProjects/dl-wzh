# Android Island 1.4.56

## Glow lirik lebih tegas
- Opasitas glow pada kata yang sedang dibaca dinaikkan (alpha dasar 0,25 -> 0,55; sebelumnya redup di intensitas rendah).
- Lapisan halo lebar ikut dinaikkan sedikit, dan mode tanpa lirik tersinkron juga lebih terlihat.

## Rekomendasi (menggantikan "Setel ulang pengaturan")
- Tombol tunggal "Terapkan rekomendasi" di bagian Rekomendasi. Opsional: tidak ada yang diubah sampai kamu menekannya.
- Nilai rekomendasi = nilai bawaan aplikasi (lihat UPDATE-1.4.55). Izin sistem tidak terpengaruh.

## Profil layar cutout
Pilihan baru di "Posisi & cutout":
- Otomatis: ukuran mengikuti deteksi cutout perangkat (notch lebar, waterdrop, punch hole); jika tidak terdeteksi memakai ukuran iPhone.
- iPhone: 130 x 32 dp
- Punch hole: 110 x 34 dp
- Waterdrop: 124 x 36 dp
- Notch lebar: 176 x 34 dp
- Kustom: memakai slider. Menggeser slider otomatis beralih ke Kustom.

Ukuran profil dipakai di tampilan dan di area sentuh (accessibility service), sehingga keduanya selalu sama.

## Animasi
- Tutup panel (dan buka lagi saat sedang menutup) sekarang memakai spring pada profil Flow dan Jelly di semua mode performa. Sebelumnya, pada default Low Power + Ultra, memakai tween yang berhenti mendadak ketika dibuka lagi di tengah jalan.
- Area sentuh tinggi kini mengikuti tinggi notch (sebelumnya memakai tinggi compact).
- Transisi lirik keluar lebih halus (170/230 ms, ease-out, sebelumnya 130/200 ms).
- Masuk konten (ekspanded dan banner WhatsApp) memakai ease-out, bukan linear.

Catatan: kode belum dikompilasi di lingkungan ini (tanpa Gradle/Kotlin). Jalankan build debug dan kabari bila ada error.
