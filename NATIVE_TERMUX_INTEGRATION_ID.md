# Ryomd Overdrive — Native Android + Termux Integration

Paket ini berisi proyek Android Gradle yang memuat panel Ryomd melalui backend Node.js lokal di Termux.

## Perubahan integrasi

- `MainActivity` membuka `http://127.0.0.1:3002/` sebagai satu origin dengan endpoint `/api/...`.
- Dengan same-origin, frontend yang memakai `fetch('/api/...')` tetap memakai jalur API yang sudah ada. Tidak diperlukan perubahan CORS/OPTIONS pada `server.js` untuk koneksi panel-ke-backend ini.
- `res/xml/network_security_config.xml` menolak cleartext HTTP secara default dan memberi pengecualian hanya untuk loopback `127.0.0.1` dan `localhost`.
- Jika backend tidak tersedia, WebView menampilkan instruksi untuk menjalankan backend di Termux dan tombol coba lagi.
- Aset panel disalin ulang dari `RYOMD_WEB_SOURCE_WZH2_SAFE.zip`. Log, PID, dan direktori backup lama tidak disertakan.

## Cara menjalankan

1. Di Termux, jalankan backend dari direktori proyek:

   ```bash
   cd "$HOME/ryomd-overdrive-v2.2-termux/ryomd-assistant-v2.2"
   PORT=3002 npm start
   ```

2. Pastikan backend merespons:

   ```bash
   curl -i http://127.0.0.1:3002/api/system
   ```

   Respons yang diharapkan adalah `HTTP 200`.

3. Bangun dan instal APK dari proyek Android ini. Perintah build debug standar:

   ```bash
   ./gradlew assembleDebug
   ```

   APK debug normalnya berada di:

   `app/build/outputs/apk/debug/app-debug.apk`

4. Biarkan Termux tetap berjalan ketika menggunakan panel APK. APK ini belum memulai Node.js atau layanan Termux secara otomatis.

## Build requirements

- JDK 17
- Android SDK dengan platform/build tools yang sesuai untuk `compileSdk 36`
- Gradle 8.13 (skrip `gradlew` yang disertakan mengunduh distribusi Gradle 8.13 resmi dan memverifikasi SHA-256)
- Internet saat Gradle/Android Gradle Plugin/dependensi belum tersedia secara lokal

## Yang belum dipindahkan menjadi native

Paket ini **tidak** mengubah WhatsApp, Telegram, Hermes, RVC, Anime backend, atau Node.js menjadi layanan Android native. Layanan tersebut tetap harus tersedia melalui backend/service yang sudah ada di Termux. APK hanya mengakses server lokal pada port 3002.

WhatsApp/Telegram yang sudah berjalan dapat terus dikelola Termux. Hermes dan RVC yang sebelumnya terdeteksi berhenti belum otomatis diaktifkan oleh paket ini.

## Keamanan dan batasan

- Backend server harus tetap bind ke `127.0.0.1`; jangan ubah menjadi `0.0.0.0` tanpa kebutuhan dan pengamanan tambahan.
- HTTP cleartext hanya diizinkan untuk loopback lewat Network Security Config; host lain tetap mengikuti kebijakan `cleartextTrafficPermitted=false`.
- Jangan taruh API keys, token, session WhatsApp, atau database pribadi ke dalam source ZIP/APK.
- Simpan signing keystore yang sama untuk pembaruan APK di masa depan.
- Pengujian pada perangkat Android fisik tetap wajib. Pemeriksaan ZIP/XML di paket ini bukan bukti bahwa APK sudah berhasil dikompilasi atau semua fitur telah diuji.

## Catatan tentang WZH 2 APK

Ini adalah **proyek Android Gradle**, bukan ZIP situs HTML untuk menu “Proyek .zip Anda” di WZH. Gunakan builder yang memang dapat mengimpor/membangun proyek Android Gradle. Mengunggah paket ini ke pembungkus situs web tidak otomatis memasukkan kode Java dan Network Security Config.
