# Backend integration — security requirements

Aplikasi saat ini masih berjalan dalam mode akun lokal/offline. Tidak ada backend produksi yang disertakan, jadi fitur online belum terlindungi oleh server sampai backend sungguhan dipasang.

## Minimum requirements sebelum online

1. HTTPS dengan sertifikat valid dan host yang dikelola sendiri.
2. Login server-side dengan Argon2id atau bcrypt, rate limiting, access token pendek, dan refresh-token rotation/revocation.
3. Validasi JWT di setiap endpoint (signature, issuer, audience, expiry, algoritma yang diizinkan).
4. Authorization di tiap request berdasarkan user terautentikasi dan kepemilikan resource; jangan percaya userId/role dari client.
5. Verifikasi Play Integrity di server melalui Google API, termasuk nonce sekali pakai yang dibuat server. Jangan menaruh service-account credentials di APK.
6. Skor, hadiah, kuota, inventaris, dan hasil pertandingan dihitung server.
7. Logging aman, monitoring, backup, rate limits, dan pengujian akses lintas akun.

Lihat `SECURITY_HARDENING.md` untuk checklist rinci.

## Android configuration

Ubah `API_HOST`, `PIN_1`, dan `PIN_2` pada `app/build.gradle.kts` sebelum rilis. Ambil pin SHA-256 SPKI dari sertifikat/key server yang benar dan masukkan juga ke `app/src/main/res/xml/network_security_config.xml`. Gunakan pin cadangan yang valid untuk rotasi sertifikat. Placeholder saat ini sengaja tidak cocok dengan server mana pun; request pinning akan gagal sampai konfigurasi benar.
