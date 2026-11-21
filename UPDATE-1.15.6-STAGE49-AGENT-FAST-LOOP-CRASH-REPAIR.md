# Stage 49 — Perbaikan crash, kecepatan, dan keandalan Looping Agent

Versi aplikasi: 1.15.6 (versionCode 151)

## Perbaikan

- Menghapus pola Regex perintah bertingkat dari `IntentResolver.looksLikeCommand` dan menggantinya dengan pencocokan token/prefix. Ini menutup jalur `PatternSyntaxException` yang terlihat pada log saat tombol kirim ditekan.
- Parser tindakan `click` menerima field `query` sebagai alias `text`, sehingga respons seperti `{"type":"click","query":"Cari"}` tidak lagi membuat seluruh envelope dianggap rusak.
- Memperbaiki prompt pemulihan format AI: contoh yang sebelumnya selalu menyebut WhatsApp dihapus. Agen sekarang diminta membaca tujuan asli dan layar terakhir, bukan membuka aplikasi contoh yang tidak diminta.
- Menambahkan jalur cepat deterministik untuk pola yang jelas: buka aplikasi → cari istilah di aplikasi → klik hasil/video pertama. Langkah pertama menggunakan `search_in_app`, yang membuka aplikasi hanya jika belum di depan, mengisi kolom pencarian, lalu menekan Enter. Permintaan perencanaan AI awal dilewati; agen tetap membaca layar hasil sebelum menentukan langkah berikutnya.
- Setelah pencarian berhasil, label generik `Cari`/`Search` tidak lagi diperlakukan sebagai hasil. Jika model mengusulkan label generik itu padahal pengguna meminta hasil pertama, tindakan dialihkan ke `tap_first_result`.
- Agen mencatat tindakan yang sudah berhasil dan menolak menjalankannya kembali pada tugas yang sama. Jika model terus mengulang tindakan selesai, loop dihentikan dengan pesan yang jelas, bukan membuka aplikasi atau mengetuk hal yang sama berkali-kali.
- `SearchInApp` sekarang melaporkan hasil pencarian yang sebenarnya sebagai status tindakan. Kegagalan pencarian tidak lagi dilaporkan sebagai tindakan berhasil hanya karena fungsi pencarian selesai dipanggil.
- Pencarian Accessibility lebih ketat: chip rekomendasi seperti `Pencarian · ...` tidak dianggap tombol pencarian utama. `tap_first_result` menghindari area kontrol pencarian dan navigasi.
- Batas loop dipersingkat dari 36 langkah/5 menit menjadi 12 langkah/90 detik. Pengamatan ulang dibatasi menjadi maksimal tiga percobaan singkat.
- Suhu model untuk perintah perangkat diturunkan agar JSON tindakan lebih konsisten. Batas panjang jawaban pengguna tetap dipertahankan.
- Menambahkan pengujian untuk pengenal perintah, parser tugas cepat, alias field click, dan pencegahan pemutaran ulang tindakan yang sudah selesai.

## Verifikasi

- Pemeriksaan mandiri parser tugas cepat dengan Kotlin: lulus untuk perintah TikTok, istilah pencarian, permintaan hasil pertama, serta penolakan urutan yang tidak didukung.
- Pemeriksaan mandiri pengenal perintah dengan Kotlin: 9 skenario lulus, termasuk perintah dengan awalan sopan dan pertanyaan penjelasan yang harus ditolak.
- Sumber tes unit baru/yang diperbarui disertakan di proyek.
- `bash gradlew :app:testDebugUnitTest --offline --no-daemon` tidak dapat dijalankan karena Gradle 8.9 belum tersedia di lingkungan dan unduhan `services.gradle.org` gagal (`UnknownHostException`). Karena itu build Android penuh dan pengujian perangkat belum terverifikasi.
