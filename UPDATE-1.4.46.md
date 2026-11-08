# Android Island 1.4.46 — Lirik tidak gepeng, opasitas latar, kaca pembesar tepi

## Lirik 2 baris tidak lagi gepeng (diperbaiki)
Penyebab: tinggi panel expand dikunci (118 dp), sehingga lirik yang turun ke 2 baris dipotong/dikompres.
- Area lirik sekarang selalu menyediakan tinggi 2 baris (`heightIn(min)`), jadi lirik 1 baris atau 2 baris tidak menggeser layout.
- Tinggi panel expand ditambah satu baris lirik saat lirik aktif (`lyricExtraH` di IslandRoot).

## Opasitas latar belakang (BARU, Pengaturan)
- "Opasitas latar belakang" 35%–100%: foto album dan dasar hitamnya ikut transparan, sehingga aplikasi di belakang sedikit terlihat. Blur tetap murah (bitmap kecil yang diskalakan).

## Kaca pembesar di tepi (BARU, fluid glass)
- File baru `island/EdgeLens.kt`: cincin tepi yang membesarkan artwork (efek lensa), ditambah kilau yang bergerak mengelilingi tepi.
- Digerakkan oleh jam drift yang sama dengan foto latar (satu jam, tanpa recomposition).
- "Kekuatan lensa tepi" 0–100% di Pengaturan. Mode hemat mematikan efek ini.

## Catatan
Belum dikompilasi atau diuji di perangkat dari sisi saya (tanpa Android SDK). Periksa build dan tampilan di perangkat sebelum rilis.

## Banner WhatsApp ikut diperbarui
- Latar foto banner memakai opasitas yang sama dengan panel musik (Pengaturan → Opasitas latar belakang).
- Kaca pembesar di tepi juga aktif di banner (ukuran rim menyesuaikan mode ringkas).
- Foto dan tepi memakai satu jam drift per kartu, jadi keduanya bergerak sinkron dan tidak saling menggandakan animasi.

## Log untuk debug
- Saat Diagnostik dinyalakan, setiap perubahan latar/lensa dicatat sebagai `GLASS_FX` (artKey, opacity, lens, lowPower, album, lyricsOn).
- Log `SETTINGS_APPLIED` dan `DRAW_LAYOUT` yang sudah ada menunjukkan ukuran panel expand setelah lirik 2 baris.
- Cara melihat: Pengaturan → Diagnostik → "Salin semua" lalu tempel ke sini.

## Liquid glass gaya iOS (ringan)
- Tepi kaca: highlight terang di pojok kiri-atas dan kanan-bawah, redup di tengah, berdenyut pelan.
- Fringe warna tipis (dingin/hangat) di tepi, mirip dispersi lensa.
- Glow dalam yang memudar ke dalam, plus gloss tipis di setengah atas.
- Kilau yang berjalan mengelilingi tepi, tetap memakai jam drift yang sama.
- Tetap tanpa RenderEffect dan tanpa blur real-time: path dan brush di-cache per ukuran; per frame hanya primitive kecil yang digambar. Intensitas mengikuti slider "Kekuatan lensa tepi"; 0 mematikan semuanya.

## Profil "Rekomendasi" (developer)
- Chip baru di Pengaturan → Profil cepat: Rekomendasi · Mewah · Seimbang · Hemat.
- Rekomendasi: kilau lirik aktif, sapuan kaca mati, animasi Jelly, opasitas latar 85%, lensa tepi 55%.

## Banner WhatsApp tidak muncul berulang
- Setiap notifikasi WhatsApp hanya memunculkan banner dan getar satu kali. Repost (teks diedit, jumlah berubah, avatar diperbarui) hanya menyegarkan data secara diam-diam.
- Saat listener tersambung ulang, notifikasi yang sudah ada di tray ditandai "sudah dialert" dan tidak diputar ulang.
- Notifikasi yang dihapus lalu muncul lagi sebagai pesan baru tetap dialert.
- Log: `WA_ALERT` saat banner tampil, `WA_SILENT_REFRESH` saat repost ditahan.

## Opasitas latar belakang benar-benar terlihat
- Slider "Opasitas latar belakang" sekarang juga mengatur isi kaca (glass fill), bukan hanya foto. Nilai rendah membuat panel expand dan banner WhatsApp tembus pandang.
