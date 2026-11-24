# Ryomd Media Center v1 — Changelog

Basis source: `RYOMD_NATIVE_YOUTUBE_PLAYER_V2_2_WZH_SOURCE.zip`  
Android package: `com.ryomd.nativeapp`  
Version: `versionCode 6` / `1.1.5-youtube-media-center-v1`

## Pembaruan

1. Modul YouTube Player diganti menjadi **Ryomd Media** dengan navigasi Beranda, Langganan, Riwayat, dan Koleksi; tombol pemanggil lama diarahkan ke layar baru.
2. Beranda menampilkan penemuan video publik dan chip topik; kolom pencarian tetap menggunakan endpoint backend Termux yang sudah ada.
3. Riwayat lokal menyimpan maksimal 100 video yang dibuka melalui Ryomd Media di perangkat ini.
4. Koleksi dapat memuat playlist pribadi akun YouTube setelah akun Google tersambung.
5. Langganan dapat memuat channel yang diikuti akun YouTube setelah izin Google diberikan.
6. Integrasi akun memakai `AuthorizationClient` Android, bukan login/password di dalam WebView. Permintaan izin menggunakan scope `youtube.readonly` dan identitas `openid`, `email`, `profile`.
7. Tombol putus akun meminta pencabutan izin melalui Google AuthorizationClient dan menghapus sesi lokal.
8. UI mendapat stylesheet media responsif baru; detail video dan jembatan pemutar native V2.2 tetap disertakan.
9. `versionCode` naik dari 5 ke 6; `applicationId` tetap `com.ryomd.nativeapp`.
10. Bridge OAuth menghentikan executor dan membersihkan access token saat Activity dimusnahkan.

## Batasan dan syarat

- OAuth tidak akan berhasil hingga YouTube Data API v3 diaktifkan dan OAuth Android client dibuat di Google Cloud dengan package name serta SHA-1 signing key final WZH.
- `youtube.readonly` merupakan scope sensitif yang dapat memerlukan verifikasi Google. Untuk tes, akun Google harus ditambahkan sebagai test user ketika consent screen berstatus Testing.
- Tab Riwayat saat ini hanya riwayat lokal Ryomd; bukan Watch History dari aplikasi YouTube/akun lintas perangkat.
- Akses playlist/langganan memakai YouTube Data API resmi. Pemutaran yang ada pada source V2.2 tidak diubah dalam patch ini.
- Tidak ada client secret, API key, password, atau token akun yang ditanam di ZIP.

## Validasi

- `node --check app.js`: PASS.
- Semua XML Android: PASS (XML parse).
- Pemeriksaan isi ZIP dan SHA-256: dicatat dalam `STATIC_VALIDATION_REPORT_MEDIA_V1.txt`.
- Gradle/Javac dan tes OAuth/API nyata: NOT TESTED karena Android SDK tidak tersedia di lingkungan penyusunan. APK final harus dibangun di WZH dan diuji pada perangkat dengan OAuth Google Cloud yang sesuai.
