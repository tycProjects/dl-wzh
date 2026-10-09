# CyberVsHacker — checklist keamanan produksi

Lapisan keamanan harus saling melengkapi. Tidak ada aplikasi yang bisa dijamin “tidak bisa ditembus”. Client Android selalu dapat dianalisis; keputusan penting wajib dilakukan di server.

## 1. R8 / ProGuard

- `app/build.gradle.kts` mengaktifkan `isMinifyEnabled = true` dan `isShrinkResources = true` untuk `release`.
- Build release, uji seluruh alur, dan simpan file `mapping.txt` dari setiap rilis secara privat untuk deobfuscation crash.
- Jangan memasukkan password, signing key, private API key, atau rahasia server ke APK. Obfuscation bukan enkripsi rahasia.

## 2. Android Keystore

- `SecureStore` memakai AES-256-GCM dengan key non-exportable yang dibuat Android Keystore.
- Data terenkripsi disimpan di SharedPreferences; bila kunci hilang/berubah, data lama bisa tidak dapat dibaca dan perlu dihapus/diinisialisasi ulang.
- Jangan simpan refresh token jangka panjang tanpa kebijakan rotasi/revokasi. Jangan menyimpan password mentah.
- Backup Android dimatikan di manifest. Tetap tinjau kebijakan backup saat menambah provider/fitur baru.

## 3. Play Integrity API

Alur produksi yang disarankan:
1. Client meminta nonce sekali pakai dari backend yang terikat ke sesi dan tindakan.
2. Client meminta token Play Integrity dengan nonce dari server.
3. Client mengirim token ke backend melalui HTTPS.
4. Backend memverifikasi token menggunakan Google Play Integrity API di server, lalu mengecek app recognition/package, sertifikat penandatanganan, verdict integritas perangkat sesuai kebijakan, nonce, waktu, dan bahwa nonce belum pernah dipakai.
5. Backend menolak request sensitif bila verifikasi gagal atau layanan verifikasi tidak tersedia. Terapkan kebijakan bertahap agar gangguan layanan tidak membuka akses.

Jangan hanya memeriksa keberadaan token di client. Jangan pernah menaruh kredensial service account Google di APK. Implementasi client menyediakan `Integrity.token(ctx, serverNonce)`, tetapi backend tetap harus membuat dan memverifikasi nonce/token.

## 4. HTTPS + certificate pinning

- `usesCleartextTraffic=false` dan `network_security_config.xml` menolak HTTP cleartext.
- `Net.client()` mewajibkan HTTPS, host yang sama dengan `API_HOST`, timeouts, serta CertificatePinner.
- Nilai `API_HOST`, `PIN_1`, dan `PIN_2` saat ini adalah placeholder. Pinning akan gagal untuk sertifikat asli sampai diganti dengan SPKI SHA-256 pin dari server yang benar.
- Siapkan setidaknya satu backup pin yang benar-benar valid untuk rotasi sertifikat. Uji rotasi dan pemulihan sebelum rilis. Jangan pernah menonaktifkan pinning sebagai fallback otomatis.
- Pinning mengurangi risiko MITM pada konfigurasi tertentu, tetapi bukan pengganti validasi server dan tidak membuat aplikasi kebal terhadap perangkat yang dikompromikan.

## 5. Backend authorization (wajib untuk data/aksi online)

Untuk SETIAP endpoint privat:
- Validasi access token di server (signature, issuer, audience, expiry, dan algoritma yang diizinkan); jangan percaya user ID/role dari body.
- Otorisasi objek: cek kepemilikan setiap resource yang diminta untuk mencegah IDOR/BOLA.
- Gunakan least privilege, rate limiting, batas percobaan login, rotasi/revokasi refresh token, dan audit log tanpa token/password.
- Hitung skor, hadiah, saldo, hasil pertandingan, kuota, dan izin di server. Jangan percaya nilai dari client.
- Validasi skema input, ukuran request, dan content type; gunakan error generik untuk autentikasi yang gagal.
- Gunakan TLS pada server, secret manager/environment variables, patch rutin, dan backup yang diuji.

Contoh alur otorisasi pseudocode:

```text
request -> validate bearer token -> resolve authenticated user from token
        -> validate input -> load requested object
        -> authorize user against that exact object/action
        -> apply rate limit / integrity policy where appropriate
        -> perform server-side operation -> audit result
```

Jangan anggap `backend/README.md` atau contoh dokumentasi sebagai server produksi. Host API dan pin masih perlu dikonfigurasi; fitur akun saat ini lokal/offline dan belum menjadi autentikasi server.
