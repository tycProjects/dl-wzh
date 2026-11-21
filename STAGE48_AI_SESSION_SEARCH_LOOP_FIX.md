# Stage 48 — Perbaikan kesinambungan sesi dan navigasi pencarian

Berdasarkan `Log bikin kesal.txt` dan pemeriksaan source Stage 47:

1. `runSearch()` sebelumnya selalu memanggil `openApp()` untuk pencarian di aplikasi. Ini dapat meluncurkan ulang aplikasi yang sudah berada di depan dan mengembalikan navigasi ke awal.
2. `clickByText()` sebelumnya menerima kecocokan sebagian. Label pendek seperti `Cari` dapat cocok dengan teks yang lebih panjang seperti `Pencarian · ...`, sehingga target yang salah dapat diketuk.
3. `runObserveActLoop()` sebelumnya memulai riwayat internal hanya dari pesan pengguna saat ini. Permintaan AI berikutnya setelah setiap observasi tidak membawa riwayat percakapan sebelumnya.
4. Batas riwayat yang dikirim pada permintaan baru dinaikkan agar memakai seluruh riwayat chat yang masih disimpan (maksimal `MAX_HISTORY`).
5. Status API diubah menjadi “Memproses jawaban…”, bukan seolah-olah membuat koneksi baru atau mengulang koneksi kunci.
6. Pemulihan format tindakan tidak lagi memanggil resolver tambahan untuk pesan yang bukan perintah eksplisit.

Perubahan:
- Pencarian dalam aplikasi memeriksa aplikasi foreground terlebih dahulu dan tidak membuka ulang aplikasi yang sudah aktif.
- `openApp()` menjadi idempoten untuk aplikasi yang sudah di depan.
- Klik berbasis teks hanya menerima label yang cocok persis; tidak lagi mengetuk hasil kecocokan sebagian.
- Panduan agen diperjelas untuk alur TikTok: gunakan pencarian utama, kirim kueri, periksa hasil, lalu buka hasil relevan pertama; jangan mengetuk chip rekomendasi pencarian.
- Setiap langkah Looping Agent membawa konteks percakapan user/assistant dan konteks sistem yang sama.

Verifikasi:
- Pemeriksaan statis: seluruh titik pemanggilan `runObserveActLoop()` menggunakan parameter konteks baru.
- Pemeriksaan statis: tiga jalur permintaan ulang Looping Agent menyertakan konteks sistem dan riwayat percakapan.
- Pemeriksaan statis: fungsi klik tidak memiliki fallback ke kecocokan teks sebagian.
- Pemeriksaan ZIP dilakukan setelah pengemasan.
- Build dan pengujian Gradle belum dapat dijalankan karena wrapper memerlukan unduhan Gradle 8.9 dan `services.gradle.org` tidak dapat diakses dari lingkungan ini.
