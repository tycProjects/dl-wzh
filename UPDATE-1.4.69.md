# Android Island 1.4.69

## Memori: avatar notifikasi diperkecil
- Avatar WhatsApp dan panggilan diperkecil ke maksimal 128 px sebelum disimpan. Island hanya menampilkannya sekitar 20 sampai 46 dp.
- Dump meminfo menunjukkan bitmap non-malloc sekitar 14 MB di 39 bitmap. Sebagian berasal dari avatar yang belum diperkecil.

## Analisis dari dumpsys meminfo (build sebelumnya)
- Total PSS sekitar 103 MB, RSS sekitar 219 MB.
- GL mtrack 29 MB: memori GPU. Penyebab utama adalah jendela gambar overlay yang lebarnya MATCH_PARENT dan tingginya mengikuti konten. Untuk tumpukan WhatsApp, tingginya bisa sampai 78% layar. Setiap buffer surface ukurannya setara layar.
- Native heap sekitar 35 MB, termasuk bitmap.
- 4 ViewRootImpl: ada beberapa jendela yang masing-masing punya surface.

## Belum diubah
- Ukuran jendela gambar overlay. Memperkecilnya bisa menghemat banyak memori GPU, tetapi jendela ini sengaja dibuat lebar penuh untuk mencegah kapsul berpindah saat ukuran berubah. Perubahannya perlu diuji di HP dulu.

## Catatan
Kode belum dikompilasi di lingkungan ini. Jalankan build debug dan kirim log bila ada error.
