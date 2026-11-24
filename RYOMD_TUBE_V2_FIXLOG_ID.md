# Ryomd Tube V2 — Fix log (basis: Media Center + Google Account V1)

`applicationId` tetap `com.ryomd.nativeapp` · `versionCode 7` · `1.1.6-ryomd-tube-v2`

## Bug yang diperbaiki
1. **Beranda/pencarian mati bila Termux tidak jalan** — semua data bergantung pada `/api/youtube/*`. Kini ada katalog native
   (`RyomdYouTubeCatalogBridge`, NewPipe Extractor): pencarian, channel, info video. Urutan: native → cadangan backend Termux.
2. **Halaman video gagal total bila metadata gagal** — sekarang tetap tampil dan tetap bisa diputar (memakai data kartu/riwayat).
3. **Tab Langganan kosong tanpa login Google** — kini ada langganan lokal (tombol Berlangganan di channel/video), feed video terbaru
   dari channel langganan, dan impor `subscriptions.csv` dari Google Takeout.
4. **Riwayat hanya tersimpan saat membuka detail** — kini juga saat video diputar (kartu, tautan, tombol musik); dikelompokkan
   Hari ini/Kemarin/Lebih lama, bisa hapus satu item, dan impor `watch-history.json` dari Takeout.
5. **Tombol "Kembali" selalu ke beranda** — kini memakai riwayat navigasi (video → channel → video → ... kembali bertahap).
6. **Putuskan akun** memakai `RevokeAccessRequest` tanpa akun (berisiko galat) — diganti pencabutan OAuth standar
   (`oauth2.googleapis.com/revoke`).
7. **Langganan/playlist Google terpotong 50 item** — kini membaca sampai 6 halaman (~300 item).
8. **Galat OAuth tidak informatif** — kode 10 (package/SHA-1 belum cocok), 7/8 (jaringan), 16/17 (Play Services) dijelaskan.
9. Sesi Google yang pernah tersambung dipulihkan otomatis saat membuka Langganan/Koleksi.
10. Teks 9–10px di UI media dinaikkan (≥12px), tombol ≥44px.

## Tampilan (UI/UX Pro Max + pola YouTube)
Feed vertikal 16:9 dengan durasi, avatar channel, judul 2 baris, chip topik, bottom-nav ikon SVG, halaman tonton
(pratinjau + tombol putar besar, Berlangganan, deskripsi, komentar, "Berikutnya"), tombol putar cepat di setiap kartu,
skeleton loading, mode terang/gelap.

## Login akun YouTube — yang perlu dipahami
Google melarang login di WebView. Satu-satunya cara resmi adalah OAuth native (sudah ada). Agar tombol **Masuk Google** bekerja:
Google Cloud → aktifkan YouTube Data API v3 → OAuth client tipe **Android** → package `com.ryomd.nativeapp` + **SHA-1 kunci
tanda tangan WZH** → tambahkan akun sebagai test user. Tanpa itu tombol menampilkan kode galat 10. Alternatif tanpa setup:
langganan lokal + impor Takeout. Watch History akun tidak tersedia lewat API resmi (hanya lewat impor Takeout).

## Belum dilakukan / batasan
- Belum dikompilasi Gradle dan belum diuji di perangkat. Pemutaran bisa gagal bila YouTube menolak stream (kode galat tampil di layar pemutar).
- Playlist akun dibuka di YouTube (belum diputar native). Komentar masih membutuhkan backend Termux.
- Antarmuka JavaScript native dapat dipanggil dari iframe pihak ketiga di WebView (batasan Android); jangan memuat iframe tidak tepercaya.
