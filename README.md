# VYRX Teacher Studio 2.0

Aplikasi Android mode guru untuk membuat bank soal, menyimpan kunci/pembahasan secara lokal, dan memindai teks dari gambar menggunakan ML Kit Text Recognition on-device.

## Fitur
- Editor soal, kunci jawaban, dan pembahasan.
- Penyimpanan lokal di perangkat (SharedPreferences).
- OCR on-device dari gambar melalui pemilih gambar Android.
- Analisis pola sederhana secara offline.
- Tidak meminta izin overlay, aksesibilitas, atau izin khusus untuk menghindari aplikasi ujian.

## Catatan AI lokal
Versi ini menyediakan OCR on-device dan analisis pola offline. Ini **belum menyertakan model LLM generatif lokal**. Untuk jawaban generatif offline, pilih model dengan lisensi yang sesuai dan kompatibel dengan RAM/perangkat, lalu integrasikan sebagai modul terpisah. Soal dan gambar tidak dikirim ke cloud oleh fitur dalam project ini.

## Build
- Android Studio stabil terbaru
- JDK 17
- Android SDK 35
- Buka folder project, tunggu Gradle Sync, lalu Build > Build Bundle(s) / APK(s) > Build APK(s).

APK debug: `app/build/outputs/apk/debug/app-debug.apk`

Package: `com.vyrx.teacherstudio`

## Batasan
Project ini belum dikompilasi atau diuji pada perangkat. Uji di emulator/perangkat sebelum digunakan. Guru harus memeriksa kunci jawaban sebelum membagikan soal.
