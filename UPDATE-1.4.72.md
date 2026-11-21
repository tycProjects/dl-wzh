# Android Island 1.4.72

## UI expand kembali ke 1.4.70 (animasi kecil tetap dari 1.4.71)
- Susunan kartu media kembali seperti 1.4.70: cover kecil | judul + lirik, bar progres dengan waktu di bawahnya, lalu baris tombol.
- Dihapus: bingkai putih cover, gelombang di tepi bawah, kolam cahaya dan bevel dalam pada kaca (tampilan 1.4.71).
- Tetap dari 1.4.71: cascade per elemen, halo tekan, percikan hati, cincin play/pause, cross-fade cover, judul dan lirik naik masuk, denyut thumb progres, rubber band kapsul.
- Tinggi minimum kartu dihitung untuk susunan 1.4.70 (`mediaCardMinHeightDp`) sehingga baris tombol tidak terpotong.

## Efek liquid glass di pinggir (Foreground)
- Baru: `LiquidLens.kt`. Shader AGSL (Android 13+) membuat gambar latar kaca terlihat membesar dan melengkung di sepanjang tepi kartu, dengan sedikit dispersi warna.
- Berlaku untuk kartu expand, banner WhatsApp, dan banner panggilan. Kekuatannya mengikuti slider "Edge lens" (0 = mati).
- Android 12 ke bawah: tidak ada refraksi, rim kaca tetap tampil.
- Biaya: satu pass GPU kecil pada permukaan kartu, tanpa bitmap tambahan.

## Profil Putih
- Ambang adaptif dikembalikan ke semula (tidak berubah).
- Hanya profil Putih: kaca lebih putih dan rim lebih terang. Profil lain tidak berubah.

## Animasi buka lebih halus
- Panel kini disusun saat state berubah ke EXPANDED, bukan satu frame setelah spring berjalan. Di log sebelumnya progress melompat 0,000 -> 0,309 dalam satu langkah karena komposisi kartu terjadi di dalam animasi.
- Spring baru mulai 2 frame setelah komposisi, layout, dan perubahan ukuran jendela overlay selesai (kompilasi shader ikut terserap di sini).
- Jelly: ruang cadangan bawah selalu ada (tidak lagi hilang di mode hemat daya) dan dihitung dari overshoot maksimum, jadi bagian bawah tidak terpotong.

## Catatan
Kode belum dikompilasi di lingkungan ini. Jalankan build debug dan kirim log bila ada error.
