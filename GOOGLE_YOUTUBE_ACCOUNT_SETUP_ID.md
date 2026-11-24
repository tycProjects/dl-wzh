# RYOMD Media — Sambungan Akun Google / YouTube

Versi fitur: Ryomd Tube V3 — OAuth diagnostics dan home-feed pagination  
Paket Android: `com.ryomd.nativeapp`  
Versi APK source: `versionCode 8`, `versionName 1.1.7-ryomd-tube-v3`

## Fitur yang ditambahkan

- Beranda penemuan video publik dengan pilihan topik dan pencarian.
- Tab Langganan yang membaca channel yang diikuti oleh akun YouTube setelah izin Google diberikan.
- Tab Koleksi yang membaca playlist akun YouTube.
- Tab Riwayat lokal yang mencatat video yang dibuka dari modul media Ryomd pada perangkat ini.
- Tombol sambungkan/putuskan akun menggunakan layar otorisasi Google native.
- Access token OAuth hanya disimpan di memori proses aplikasi; password Google tidak pernah diminta atau disimpan.
- Pemutar dan layanan Termux yang sudah ada dipertahankan.

## Yang diperlukan sebelum tombol login dapat dipakai

1. Buka Google Cloud Console dan pilih/buat satu project.
2. Aktifkan **YouTube Data API v3** untuk project tersebut.
3. Siapkan OAuth consent screen. Untuk pengujian personal dengan status aplikasi **Testing**, tambahkan akun Google yang akan dipakai sebagai test user.
4. Buat OAuth client dengan tipe **Android** menggunakan package name persis `com.ryomd.nativeapp` dan SHA-1 sertifikat signing release yang dipakai WZH untuk APK final. Jangan memakai SHA-1 debug bila APK yang dipasang ditandatangani dengan key WZH lain.
5. Pastikan konfigurasi OAuth dan kebijakan verifikasi Google selesai untuk distribusi di luar daftar test user. Scope `youtube.readonly` adalah scope sensitif; Google dapat meminta verifikasi sebelum akses publik.
6. Build ulang APK setelah konfigurasi OAuth tersedia di Google Cloud. Pertahankan applicationId dan signing key yang sama agar APK dapat memperbarui instalasi lama.

Tidak ada client secret, password, token atau API key yang perlu ditempel ke file source ini. Android AuthorizationClient menggunakan konfigurasi OAuth Android berdasarkan package dan fingerprint signing.

## Data akun yang dapat dibaca

- **Langganan**: YouTube Data API `subscriptions.list` dengan `mine=true`.
- **Playlist**: YouTube Data API `playlists.list` dengan `mine=true`.
- API dipanggil dengan OAuth scope `https://www.googleapis.com/auth/youtube.readonly` setelah persetujuan pengguna.

## Batasan riwayat tontonan

Tab **Riwayat** saat ini adalah riwayat lokal Ryomd, bukan seluruh Watch History akun YouTube. YouTube Data API standar tidak menyediakan endpoint umum untuk membaca Watch History akun. Google Data Portability API memiliki scope khusus `dataportability.myactivity.youtube` untuk ekspor aktivitas YouTube, tetapi akses ini memerlukan konfigurasi Data Portability API dan proses verifikasi scope yang lebih ketat. Scope tersebut sengaja tidak diminta oleh build ini.

## Batasan teknis

- Sinkronisasi akun hanya diuji secara statis dalam source; OAuth, kuota YouTube API, dan respons akun nyata belum dapat diuji tanpa OAuth client Google Cloud dan APK yang ditandatangani key final.
- Access token hanya tersedia selama proses APK berjalan. Saat aplikasi dibuka ulang, tekan **Hubungkan ulang** untuk meminta token sesi baru. Google dapat mengembalikannya tanpa consent ulang bila izin masih aktif.
- Tombol **Putuskan** menghapus sesi lokal Ryomd. Untuk mencabut izin aplikasi di sisi Google, buka https://myaccount.google.com/connections.
- UI di APK berasal dari aset bundled (`USE_BUNDLED_UI = true`), sehingga fitur ini memerlukan rebuild APK; ZIP HTML App Update Center saja tidak dapat menambahkan kode Java/native.
- Backend pencarian publik tetap memakai endpoint Termux yang sudah ada: `/api/youtube/search`, `/api/youtube/video`, `/api/youtube/channel`, dan `/api/youtube/comments`. Server tetap di port 3002 sesuai source dasar ini.


## Diagnostik login V3
Handler hasil OAuth sekarang memeriksa Intent hasil Google sebelum menyimpulkan pengguna membatalkan alur. Kode status Google ditampilkan untuk membantu membedakan pembatalan pengguna, SHA-1/package mismatch, jaringan, dan Google Play Services. Bila layar otorisasi ditutup tanpa data hasil, pesan akan menyarankan memeriksa OAuth Android client dan fingerprint signing release WZH.
