# LEN TOOLS v1.1 — Combined Android Project

Satu proyek Android Native Kotlin + Jetpack Compose yang menggabungkan fitur LEN TOOLS awal dengan halaman Netflix Token.

## Fitur yang tergabung
- Dashboard Dark Premium.
- GPT LEN AI melalui `https://api.nexadev.my.id/ai/gpt4?q=...`.
- DEEPSEK AI melalui `https://api.nexadev.my.id/ai/deepsek?text=...`.
- Netflix Token UI: pilihan region (Indonesia/ID sebagai default), input NexaDev API key, tombol Generate Token Netflix, indikator loading, error, dan tampilan hasil API.
- Halaman Alight Motion sebagai placeholder untuk alur verifikasi resmi.
- Settings dan navigasi bawah.

## Netflix API
Request memakai:
`https://api.nexadev.my.id/api/netflix?key=API_KEY&targetCountry=ID`

Region yang tersedia di UI:
- Indonesia (ID) — default
- India (IN)
- United States (US)
- Singapore (SG)

API key hanya disimpan dalam state layar selama aplikasi berjalan dan tidak ditulis ke penyimpanan permanen. Karena endpoint yang diberikan menerima key melalui query parameter, gunakan hanya key yang memang diizinkan untuk aplikasi ini. Untuk distribusi publik, disarankan membuat backend proxy agar key tidak terekspos di URL/log jaringan.

## Build APK
1. Ekstrak ZIP.
2. Buka folder hasil ekstrak di Android Studio.
3. Gunakan JDK 17 dan Android SDK Platform 35.
4. Tunggu Gradle Sync.
5. Pilih **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
6. APK debug biasanya ada di `app/build/outputs/apk/debug/app-debug.apk`.

## Catatan
- Ini adalah source project; APK belum dikompilasi atau diuji pada perangkat.
- Format respons API bergantung pada implementasi NexaDev. Aplikasi menampilkan JSON yang diterima (atau teks mentah jika bukan JSON).
- Integrasi Alight Motion belum diaktifkan; halaman hanya menjelaskan alur resmi. Tidak ada fitur untuk melewati pembayaran atau autentikasi.
- Package: `id.len.tools`
- Version: `1.1.0`
