# Update 1.14.4

## Perbaikan
- Gambar Gemini: model gambar sebelumnya tersaring oleh filter chat, sehingga "Penyedia ini belum punya model gambar". Sekarang discovery untuk gambar menyertakan model media (includeMedia).
- Kunci cadangan: hingga 4 kunci tambahan (total 5). Dipakai berurutan jika kunci utama kena 429 (batas). Diatur di pengaturan AI, baris "Kunci cadangan".
- Server sibuk (503): menunggu 5 detik lalu mencoba lagi, hingga 8 percobaan.
- Tap hasil pertama: aksi baru `tap_first_result` mengetuk teks pertama di area konten (di antara 20% dan 85% tinggi layar), lalu menunggu 1,2 detik. Untuk alur "buka TikTok, cari X, tekan video pertama".
- Panel: lapisan gelap berkaca lebih pekat (opasitas 85%) dan surfaceOpacity 0.7, chip lebih kontras (alpha 0.36, SemiBold).

## Batasan yang jujur
- Blur latar belakang (wallpaper di belakang panel) tidak tersedia di Compose tanpa menangkap layar. Yang diterapkan adalah lapisan gelap berkaca, bukan blur sungguhan.
- Kunci cadangan disimpan di penyimpanan aplikasi seperti kunci utama, belum dienkripsi.
- Tap hasil pertama mengandalkan teks yang terlihat; halaman tanpa teks di area konten tidak bisa diketuk.

## Status
Test ditulis lebih dulu: KeyRingTest, TapFirstResultTest. Belum dijalankan (tidak ada toolchain di sandbox).
