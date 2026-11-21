# Stage 56 — Looping Agent, lirik bertimestamp, dan pemulihan Dynamic Island

Perubahan berdasarkan pemeriksaan source Stage 55 dan laporan pengguna:

- Looping Agent: pemilihan hasil pertama tidak lagi mengetuk teks pertama secara membabi buta. Kandidat harus berupa area konten yang benar-benar dapat diklik; chip pencarian/related-search dan navigasi dikecualikan. Area hasil pada baris teratas diprioritaskan.
- Pencarian di aplikasi: alur yang sudah ada tetap membuka aplikasi hanya bila belum berada di depan, mengetuk kontrol pencarian utama, mengisi kolom, dan menekan Enter. Setelah itu agen membuka kartu hasil, bukan chip pencarian.
- Lirik: hasil dari provider tanpa timestamp tidak lagi diberi timestamp perkiraan dan tidak ditampilkan. LRC/QRC/TTML tetap harus lolos pemeriksaan struktur dan waktu. Penanganan spasi antar-span TTML diperbaiki. Kegagalan jaringan tidak lagi membuat lirik palsu berupa judul lagu.
- Media: pemilihan MediaSession tidak lagi memakai sesi pertama dari Android; sesi diperingkat berdasarkan status pemutaran dan metadata judul agar sesi stale tidak menutupi pemutar aktif. Metadata kosong dicatat dan tidak ditampilkan sebagai lagu bernama Unknown.
- Media: callback yang tampak tidak berubah tetap dapat memulihkan capsule ketika lagu masih diputar tetapi UI sebelumnya tersembunyi. Ini memperbaiki kondisi callback no-op yang sebelumnya dapat membuat capsule tidak pernah muncul lagi.
- Diagnostik: menambahkan catatan pemilihan sesi media, metadata kosong, dan kesalahan pengambilan lirik.
- Tes unit baru: teks tanpa timestamp tidak menghasilkan cue; timestamp LRC valid tetap dipertahankan.

Build Release belum dapat diverifikasi di lingkungan ini karena Gradle Wrapper mencoba mengunduh Gradle 8.9 dari services.gradle.org dan koneksi DNS tidak tersedia (UnknownHostException). ZIP dan perubahan source diperiksa; pengujian perangkat nyata tetap diperlukan.
- Lirik: perakitan segmen kata QRC/TTML kini mempertahankan spasi yang tersedia dan menyisipkan batas kata yang hilang untuk teks alfabet, tanpa memaksakan spasi pada aksara CJK. Ambang kualitas provider dinaikkan agar hasil bertimestamp yang kualitasnya rendah juga ditolak.
