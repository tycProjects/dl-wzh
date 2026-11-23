# Ryomd Overdrive — Native Android + Termux Control Bridge v2

## Isi
- Android Gradle shell dengan semua 31 tool dari source panel penuh.
- Termux Server Control memakai URL `http://127.0.0.1:8787`.
- Native bridge `RyomdNative.requestTermux()` menghubungi Control Agent melalui HTTP loopback tanpa browser CORS/mixed-content fetch.
- Native bridge hanya menerima `GET /api/status` dan `POST /api/action`, host loopback, port 8787, serta aksi allowlist `start`, `start-all`, `stop`, `rvc`, `stop-rvc`.

## Arsitektur runtime
`MainActivity` tetap membuka dashboard Termux pada `http://127.0.0.1:3002/`. Karena itu, file web panel pada `public/` server Termux harus diperbarui dengan source web pada ZIP induk. Script pemasangan disediakan pada `termux-deploy/install-panel-ui.sh`.

## Pengaturan Android
- `INTERNET` permission tersedia.
- Network Security Config menolak cleartext secara default dan mengizinkan hanya `127.0.0.1` serta `localhost`.
- `MIXED_CONTENT_NEVER_ALLOW` dipertahankan. Jalur native bridge tidak bergantung pada mixed-content fetch.
- applicationId proyek ini tetap `com.ryomd.nativeapp`; signing key APK lama tidak tersedia dalam source ini. Pembaruan APK hanya akan memasang sebagai update jika applicationId dan signing key cocok dengan APK yang terpasang.

## Build
Paket menyediakan Gradle 8.13 bootstrap script. Build release membutuhkan JDK 17, Android SDK platform/build tools 36, akses jaringan untuk plugin/dependensi bila belum tersimpan, dan signing release yang sesuai. Pemeriksaan pada paket ini adalah pemeriksaan statis; APK belum dikompilasi atau diuji di perangkat.
