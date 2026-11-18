# Build Server (komponen yang diperlukan)

Aplikasi Android ini adalah dashboard/controller. AI coding dan kompilasi APK harus berjalan di server terpisah yang kamu kelola, karena Android app tidak aman/realistis untuk menyimpan API secret dan menjalankan build toolchain penuh di perangkat.

## Endpoint minimum
`POST /v1/build` dengan JSON:
```json
{"prompt":"Buat aplikasi catatan sederhana","app_name":"AI APK Builder","platform":"android","language":"java"}
```
Respons sukses:
```json
{"build_id":"job-123","status":"queued","message":"Project masuk antrean build."}
```

## Worker yang harus disiapkan
1. Autentikasi pengguna dan rate limit.
2. AI coding agent di server dengan API key dari environment variable.
3. Buat file project di workspace sementara per pengguna.
4. Jalankan Gradle/Android SDK di container terisolasi, tanpa akses ke host atau secret lain.
5. Batasi CPU, RAM, waktu, ukuran file, dan akses jaringan.
6. Jalankan pemeriksaan malware/dependency dan scan izin aplikasi.
7. Simpan artefak APK/ZIP di object storage privat dengan URL unduhan berumur pendek.
8. Sediakan endpoint status dan unduhan yang terautentikasi.

Jangan menjalankan kode build dari prompt pengguna langsung di server utama. Jangan menaruh API key provider di APK.
