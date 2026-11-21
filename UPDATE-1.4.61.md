# Android Island 1.4.61

## Heart (like) per lagu
- Status hati dibaca ulang setiap lagu berganti. Sebelumnya kunci hanya memakai id pemutar, yang sama untuk semua lagu, sehingga hati tetap seperti lagu sebelumnya.
- Jika lagu sudah disukai, hati tetap merah; jika belum, hati kosong.

## Efek sweep kaca
- Sapuan sekarang masuk dari sudut kiri atas dan keluar di kanan bawah, mengikuti diagonal kartu.

## Animasi buka dan tutup (Opsi 2 / iOS Ringan)
- Membuka: lebar capsule melebar lebih dulu (ease-out cepat), lalu tinggi terbuka dengan smoothstep yang mulai sedikit setelahnya.
- Menutup: pegas lebih cepat (kekakuan 340 x kecepatan), tetap tanpa pantulan.
- Catatan: referensi repo SmartIsland (v4.0.0) tidak bisa saya baca kodenya dari sini; perilaku ini dibuat dari deskripsi kamu.

## Panggilan telepon sebagai banner
- Panggilan masuk dan berlangsung tampil sebagai banner, bukan expanded.
- Ukuran mengikuti "Banner compact" (compact atau standar, lebar dan tinggi sesuai pengaturan banner).
- Ukuran dan ketebalan teks mengikuti pengaturan teks banner.
- Latar: liquid glass dengan foto profil samar sebagai latar belakang, sama seperti banner WhatsApp.
- Tombol Terima dan Tolak merespons tekan (mengecil saat ditekan) dan menjalankan aksi notifikasi panggilan. Ketuk banner untuk membuka aplikasi telepon.

## Catatan
Kode belum dikompilasi di lingkungan ini. Jalankan build debug dan kirim log bila ada error.
