# WebTermux Android 3.0 — Debian + PRoot

Proyek Android Studio (Kotlin + WebView) dengan installer runtime Debian ARM64.

## Build APK
1. Buka folder ini dengan Android Studio terbaru.
2. Gunakan JDK 17 dan Android SDK Platform 35.
3. Lakukan Gradle Sync.
4. Pilih **Build > Build APK(s)**.
5. APK debug: `app/build/outputs/apk/debug/app-debug.apk`.

## Instalasi runtime
Saat aplikasi pertama dibuka, tekan **INSTALL LINUX RUNTIME**. Aplikasi akan:
- Mengambil paket `proot`, `libtalloc`, dan `libandroid-shmem` dari repositori paket Termux ARM64.
- Memverifikasi SHA-256 setiap paket berdasarkan metadata `Packages.gz` repositori.
- Mengunduh Debian Bookworm ARM64 dari rilis resmi `termux/proot-distro` dan memverifikasi SHA-256.
- Mengekstrak rootfs ke penyimpanan privat aplikasi dan menjalankan pemeriksaan awal.

Diperlukan internet dan beberapa GB ruang kosong. Runtime tidak dibundel ke APK agar ukuran APK tetap masuk akal.

## Perintah
Setelah runtime terpasang, perintah dijalankan melalui PRoot + `/bin/bash`. Contoh:

```bash
apt update
apt install -y python3 git nodejs
python3 -c 'print("Linux berjalan")'
ls -la
```

## Batasan penting
- Runtime hanya disediakan untuk `arm64-v8a`. Perangkat 32-bit-only tidak didukung oleh paket rootfs ini.
- Implementasi ini mengeksekusi satu proses shell per perintah, bukan terminal PTY penuh. Program CLI biasa, package manager, compiler, dan script non-interaktif dapat berjalan; aplikasi terminal interaktif yang memerlukan TTY seperti `nano`, `vim`, `htop`, `tmux`, `top` interaktif, dan REPL interaktif belum didukung.
- PRoot bukan kernel Linux penuh, tidak memberi akses root Android, tidak menjalankan modul kernel, Docker daemon, atau semua fitur yang membutuhkan privilege kernel.
- `apt` dan paket tambahan memerlukan koneksi internet serta ruang penyimpanan.

## Sumber runtime
- PRoot dan dependensi: repositori paket Termux (`packages.termux.dev`).
- Debian Bookworm rootfs: rilis `termux/proot-distro` v4.17.3.
- Rootfs SHA-256: `3a841a794ae5999b33e33b329582ed0379d4f54ca62c6ce5a8eb9cff5ef8900b`.

Proyek ini belum merupakan APK terkompilasi. Build harus dilakukan dengan Android Studio/SDK.

## Catatan audit source
- Proses instalasi dan eksekusi perintah diserialkan agar direktori kerja dan file runtime tidak saling bertabrakan.
- Ekstraksi TAR/DEB menyalin sesuai ukuran entri, bukan membaca sisa seluruh arsip sebagai satu file.
- Penantian proses memakai timeout sebelum membaca hasil, sehingga proses yang macet tidak menggantung pembacaan output selamanya.
- Tetap perlu build dan uji pada perangkat Android ARM64 nyata; pemeriksaan source tidak menggantikan pengujian APK di perangkat.
