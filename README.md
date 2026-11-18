# AI APK Builder — Android Native Java starter

Package: `com.aiapkbuilder.app`  
App label: `AI APK Builder`  
UI: dark/teal, terinspirasi tata letak screenshot WZH 2 APK (Build, Code, Host, Share, recent builds).

## Yang sudah ada
- Android Native Java app shell.
- Build dashboard dan recent-build history lokal.
- Prompt dialog untuk mengirim permintaan ke server.
- Host settings (HTTPS endpoint).
- Code, Host, Share navigation tabs.
- HTTP POST ke `POST /v1/build`.

## Yang belum ada
- Belum ada AI coding agent/server worker.
- Belum ada kompilasi APK otomatis yang berjalan.
- Belum ada editor kode penuh, polling status, login, cloud storage, atau unduhan artefak.
- ZIP ini adalah source starter, bukan APK siap pasang.

## Build di Android Studio
1. Buka folder ini di Android Studio.
2. Gunakan JDK 17 dan Android SDK Platform 35.
3. Sync Gradle.
4. Build > Build APK(s).

Plugin Android Gradle: 8.7.3. Gradle wrapper tidak disertakan; Android Studio dapat menggunakan Gradle yang sesuai atau wrapper dibuat dari instalasi Gradle 8.9.

## Catatan
Agar tombol Build benar-benar menghasilkan APK, siapkan Build Server sesuai `backend/README.md` dan `docs/API_CONTRACT.md`, lalu masukkan URL HTTPS server di tab Host. Jangan gunakan server publik tanpa autentikasi dan sandbox build.
