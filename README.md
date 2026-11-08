# WA Sender WebView

Aplikasi Android sederhana untuk menyusun pesan dan membuka percakapan WhatsApp melalui tautan resmi `https://wa.me/`.

## Persyaratan
- Android Studio
- JDK 17
- Android SDK Platform 35
- Gradle 8.9 atau versi yang kompatibel dengan Android Gradle Plugin 8.7.3

## Cara membuka dan membangun APK
1. Ekstrak ZIP ini.
2. Buka Android Studio → **Open** → pilih folder `WASenderWebView`.
3. Tunggu Gradle Sync selesai. Jika diminta, pasang Android SDK Platform 35.
4. Pilih **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
5. APK debug biasanya tersedia di `app/build/outputs/apk/debug/app-debug.apk`.

## Cara menggunakan
1. Jalankan aplikasi.
2. Masukkan nomor tujuan tanpa angka 0 di depan (contoh: `81234567890`) dan kode negara `+62`.
3. Tulis pesan.
4. Tekan **BUKA WHATSAPP & KIRIM**.
5. Periksa penerima dan isi pesan di WhatsApp, lalu tekan tombol kirim di WhatsApp.

## Catatan
- Aplikasi ini bukan pengirim massal otomatis, bukan alat crash/bug WhatsApp, dan tidak mengirim pesan tanpa tindakan pengguna.
- Riwayat disimpan lokal di penyimpanan WebView aplikasi.
- Tidak memerlukan izin kontak, SMS, telepon, atau akses internet Android khusus untuk halaman lokal. WhatsApp/browser menangani tautan keluar.
- Untuk mengubah nama aplikasi, edit `app/src/main/AndroidManifest.xml`.
