# Android Island 1.15.3 — Tahap 11: Kejujuran AI, berita, shell, dan pembukaan aplikasi

## Perubahan source

- Berita terkini: instruksi sistem mewajibkan pencarian lebih dahulu, URL sumber untuk setiap item, tanggal publikasi hanya jika tersedia pada halaman, dan larangan membuat rangkuman terkini dari halaman tanpa tanggal/isi yang bisa diverifikasi.
- `PageReader` kini mengambil cuplikan isi sekaligus metadata tanggal publikasi yang eksplisit (article:published_time, datePublished, publishdate, pubdate, JSON-LD, atau elemen time datetime). `WebSearch.deepSearch` meneruskan URL, tanggal yang ditemukan, dan penanda jelas jika tanggal tidak tersedia.
- Shell: `/shelldumpsys ...` sekarang dikenali sebagai bentuk ringkas, sementara kata seperti `/shellfish` tetap bukan perintah shell. Bentuk `/shell dumpsys ...` tetap didukung.
- NumberGuard: konversi memori KB ke MB/MiB/GB/GiB yang setara kini dikenali, termasuk ringkasan `TOTAL PSS` dan `TOTAL RSS` dari `dumpsys meminfo` yang unit KB-nya dinyatakan di header. Instruksi penjelasan juga meminta label MiB untuk konversi basis 1024. Angka yang benar-benar tidak cocok tetap ditahan.
- Pembukaan aplikasi: permintaan tunggal yang jelas (mis. “buka WhatsApp”) kini dirutekan langsung ke `open_app`, tanpa bergantung pada model untuk memilih aksi. Kegagalan `startActivity` menyertakan detail error Android yang tersedia; hasil menyebut label dan package. Permintaan multi-langkah tetap memakai agent loop AI.

## Tes yang ditambahkan/diubah

- Bentuk `/shelldumpsys` dan penolakan `/shellfish`.
- PSS 90319 KB → sekitar 88,2 MB (penggunaan istilah MB untuk konversi basis 1024 juga diterima), RSS 214113 KB → sekitar 209,1 MB.
- Angka memori palsu tetap ditandai.
- Ekstraksi tanggal publikasi metadata dan JSON-LD, serta kasus tanpa tanggal.
- Router perintah buka aplikasi.

## Batas verifikasi

- Source dan tes telah diperbarui.
- Unit test Gradle belum dapat dijalankan: wrapper berusaha mengunduh Gradle 8.9 dan gagal dengan `UnknownHostException: services.gradle.org`.
- APK Release dan pengujian langsung pada HP belum diverifikasi. Keberhasilan membuka aplikasi tetap perlu uji perangkat karena bergantung pada package manager, aplikasi target, dan kondisi Android saat dijalankan.
