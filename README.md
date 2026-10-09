# Cyber vs Hacker — Android Project

Project Android berbasis Kotlin + Jetpack Compose. Buka folder `CyberVsHacker` di Android Studio, gunakan JDK 17, lakukan Gradle Sync, kemudian pilih **Build > Build APK(s)**.

## Fitur
- Akun lokal dengan hash kata sandi PBKDF2 dan penyimpanan lokal AES-GCM menggunakan Android Keystore.
- Pertandingan tim Cyber vs Hacker, pemilihan modul berdasarkan budget, preset loadout, dan bantuan strategi harian.
- Akademi keamanan offline dengan 20 modul pembelajaran dan kuis: kata sandi, phishing, MFA, pembaruan, Wi-Fi, backup, privasi, respons insiden, malware, rekayasa sosial, email, enkripsi, izin Android, browser, kebocoran data, pelaporan kerentanan, log login, rahasia API, jaringan, CTF legal, ransomware, dan Bluetooth/hotspot.
- Umpan balik jawaban, skor kuis, misi, dan pencapaian lokal.
- Panduan keamanan offline di `app/src/main/assets/panduan_keamanan_offline.md`.
- R8 obfuscation dan resource shrinking aktif pada build `release`.
- Android Keystore + AES-GCM untuk data lokal, cleartext HTTP dinonaktifkan, HTTPS host validation, OkHttp certificate pinning, timeout request, serta kerangka Play Integrity.
- Panduan hardening backend di `backend/SECURITY_HARDENING.md`.

## Lapisan keamanan — status sebenarnya

| Lapisan | Status |
|---|---|
| R8/ProGuard | Diaktifkan untuk release; perlu build dan uji regresi |
| Android Keystore | Dipakai oleh penyimpanan lokal terenkripsi |
| Play Integrity | Kerangka client; nonce harus berasal dari server dan token harus diverifikasi di backend |
| HTTPS | HTTP cleartext dinonaktifkan; klien menolak URL non-HTTPS |
| Certificate pinning | Implementasi ada, tetapi pin masih placeholder dan wajib diganti sebelum request produksi |
| Backend authorization | Belum ada backend produksi; wajib dibangun sebelum layanan online |

Tidak ada lapisan yang menjamin aplikasi kebal ditembus. Backend harus menjadi sumber kebenaran untuk semua tindakan sensitif. Jangan memasukkan rahasia server ke APK.

## Build
1. Gunakan JDK 17 dan Android SDK 34.
2. Buka folder `CyberVsHacker` di Android Studio.
3. Tunggu Gradle Sync selesai.
4. Pilih **Build > Build APK(s)**. APK debug biasanya muncul di `app/build/outputs/apk/debug/` jika build berhasil.
5. Untuk rilis, konfigurasi signing key secara aman, ganti API host dan pin dengan nilai produksi, lalu build varian `release` dan uji pada perangkat nyata.

## Batasan
- Mode akun saat ini lokal/offline; grup dan pemain adalah simulasi lokal, bukan multiplayer realtime.
- Host API dan certificate pin masih placeholder. Koneksi akan ditolak sampai nilai asli dikonfigurasi.
- Play Integrity tidak memberi keamanan jika token hanya dicek di client; backend harus memverifikasi token dengan Google.
- Build belum dikompilasi atau diuji pada perangkat dalam paket ini.
- Latihan dirancang untuk edukasi defensif dan lab berizin.
