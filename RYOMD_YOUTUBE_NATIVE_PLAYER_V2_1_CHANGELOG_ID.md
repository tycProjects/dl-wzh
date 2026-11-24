# RYOMD YouTube Native Player V2.1 — Perbaikan UI & branding

Basis source: `RYOMD_NATIVE_YOUTUBE_PLAYER_V2_WZH_SOURCE.zip` yang telah dihasilkan dari source Android MediaStore V2 pengguna.

## Perbaikan
- Menghapus injeksi tombol/panel `position: fixed` yang sebelumnya menutupi dashboard.
- Native playback kini dipanggil dari halaman detail video pada tool YouTube Desk yang sudah ada.
- Video dan audio memiliki tombol native terpisah.
- Pemutar video membuka Activity fullscreen immersive Media3; musik membuka layar audio dan tetap mendukung MediaSession background.
- Status ekstraksi dikirim kembali ke UI, termasuk pesan gagal agar tidak berhenti pada tulisan “Mengambil stream…”.
- Tidak lagi memasang panel tambahan di pojok dashboard.

## App name dan launcher icon
- `AndroidManifest.xml` kini menggunakan `@string/app_name`, `@mipmap/ic_launcher`, dan `@mipmap/ic_launcher_round`.
- Nama aplikasi dapat diedit di `app/src/main/res/values/strings.xml`.
- Foreground icon dapat diganti di `app/src/main/res/drawable/ic_launcher_foreground.xml`; warna latar di `app/src/main/res/values/colors.xml`.
- Ini membuat resource Android mengikuti konvensi yang bisa diperbarui WZH; jika panel setting WZH tidak menimpa resource project, edit file di atas setelah import.

## Versi
- `applicationId`: `com.ryomd.nativeapp` (dipertahankan).
- `versionCode`: 4 (dinaikkan dari 3 agar dapat menjadi update atas build V2 sebelumnya).
- `versionName`: `1.1.3-youtube-native-v2.1`.
- compileSdk 36, minSdk 26, targetSdk 35 dipertahankan.

## File/fitur yang dipertahankan
- 13 aset web Ryomd dari source dasar.
- `RyomdNativeBridge.java`, `RyomdMediaSaver.java`, media store, WebView, Termux bridge.
- Music Studio dan modul lain tetap ada; tidak dihapus atau di-rollback.
- Konfigurasi Gradle dan dependency NewPipe/Media3 dipertahankan.

## Pengujian
Validasi statis: ZIP CRC, XML parse, manifest references, applicationId/version, keberadaan bridge/service/activity, JavaScript syntax (Node --check), web asset parity.
Belum dikompilasi Gradle, belum dites APK/signature, dan belum diuji playback langsung di perangkat.
