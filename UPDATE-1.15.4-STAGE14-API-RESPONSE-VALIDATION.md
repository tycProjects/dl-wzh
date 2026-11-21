# Tahap 14 — Validasi struktur dan transparansi data Public APIs

## Tujuan
Mengurangi kemungkinan AI memperlakukan respons API yang tidak valid sebagai fakta. Tahap ini tidak mengklaim dapat membuktikan kebenaran semantik seluruh penyedia API.

## Perubahan
- Respons JSON diperiksa sintaksnya dan ditolak jika ada data tambahan setelah nilai JSON utama.
- Respons XML diperiksa sebagai dokumen well-formed dengan pemrosesan deklarasi DOCTYPE dinonaktifkan.
- Teks biasa diberi label bahwa tidak ada skema untuk memvalidasi isinya.
- Respons kosong, JSON/XML rusak, dan format tidak didukung tidak diteruskan ke model sebagai data valid.
- Setiap respons sukses menyertakan URL sumber yang sudah disamarkan, status HTTP, tipe konten, waktu pengambilan UTC, dan jumlah karakter yang dibaca.
- Waktu pengambilan dibedakan secara eksplisit dari waktu pembaruan data oleh penyedia.
- Instruksi AI melarang klaim bahwa HTTP 2xx atau sintaks valid membuktikan kebenaran, kelengkapan, maupun kebaruan data.
- Status aksi API tidak lagi menyebut panggilan berhasil bila validasi struktur gagal.

## Batasan yang tetap ada
- Belum ada skema per penyedia API, jadi field/units/coverage tidak bisa divalidasi universal.
- Belum ada verifikasi silang dengan sumber kedua atau bukti tanda tangan digital.
- Waktu data diperbarui oleh penyedia hanya bisa diketahui jika penyedia menyertakannya dan belum dinormalisasi untuk semua API.
- Runner masih GET HTTPS tanpa API key, OAuth, header autentikasi khusus, POST, atau WebSocket.
- Validasi JSON/XML hanya memeriksa struktur, bukan kebenaran faktual.

## Verifikasi
Pemeriksaan statis dan kompilasi terisolasi dapat dilakukan bila toolchain tersedia. Unit test Gradle, build APK, dan uji endpoint live/device harus dilaporkan terpisah; jangan menganggap berhasil tanpa dijalankan.
