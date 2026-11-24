# Ryomd Tube V3 + Native YouTube Player + Media Center — WZH Android Gradle Source

## Versi

- `applicationId`: `com.ryomd.nativeapp` (dipertahankan)
- `versionCode`: `9`
- `versionName`: `1.1.8-ryomd-tube-v3.1`
- UI aset: bundled di APK (`USE_BUNDLED_UI = true`)

## Fitur baru Media Center v1

- Beranda penemuan video publik + pencarian.
- Tab Langganan dari akun YouTube.
- Tab Riwayat lokal Ryomd (bukan history akun YouTube lintas aplikasi/perangkat).
- Tab Koleksi playlist akun YouTube.
- Sambungkan/putuskan akun Google melalui izin native Android; tidak meminta password.

## Persiapan sebelum pengujian login

Ikuti `GOOGLE_YOUTUBE_ACCOUNT_SETUP_ID.md`: aktifkan YouTube Data API v3, siapkan OAuth consent screen, tambahkan test user bila masih Testing, dan buat OAuth Android client dengan package name persis serta SHA-1 signing key final WZH. Scope `youtube.readonly` dapat memerlukan verifikasi Google sebelum distribusi publik.

## Cara build

1. Impor ZIP sebagai **Android Gradle project**, bukan sebagai ZIP HTML/App Update Center.
2. Pastikan JDK 17 dan Android SDK platform/build tools untuk `compileSdk 36` tersedia.
3. Pertahankan `applicationId` dan signing key/keystore yang sama dengan APK terpasang.
4. Jalankan tugas build `assembleRelease` melalui panel build Android Gradle di WZH. Jika WZH meminta Gradle Wrapper, gunakan wrapper yang disediakan oleh lingkungan WZH; ZIP source ini tidak menyertakan `gradle-wrapper.jar`.
5. Pasang APK di atas aplikasi lama. Jangan uninstall APK lama karena data aplikasi bisa hilang.

Backend Termux tetap di `127.0.0.1:3002`. Jalankan service Ryomd yang sudah ada sebelum menggunakan aplikasi. Source ini tidak memindahkan atau mengubah WhatsApp, Hermes, RVC, maupun database Termux.

## Validasi

Berkas JavaScript, XML, dan checksum diperiksa secara statis. Kompilasi Gradle/Javac, OAuth Google Cloud, pengambilan data akun nyata, build WZH, tanda tangan, dan pengujian pada perangkat belum dilakukan di lingkungan penyusunan ini. Ikuti laporan `STATIC_VALIDATION_REPORT_TUBE_V3.txt` untuk status validasi source terbaru. Laporan Media Center V1 disimpan sebagai riwayat validasi versi lama.


## Pembaruan V3
- Login Google memeriksa payload hasil Authorization API terlebih dahulu agar error OAuth tidak disamarkan sebagai pembatalan umum.
- Beranda memakai pagination NewPipe sampai 40 item per permintaan, dengan tombol Segarkan dan Muat video lainnya.
- Lihat `RYOMD_TUBE_V3_OAUTH_HOME_FEED_FIXLOG_ID.md` untuk rincian dan batasan.


## v3.1 — Pemilih kualitas video
Setelah membuka video di pemutar layar penuh, gunakan tombol **Kualitas** di bagian atas kanan. Pilih **Otomatis** atau salah satu resolusi yang tersedia (sampai 1080p). Ketika kualitas diganti, Ryomd mengambil stream baru dan melanjutkan dari posisi terakhir, sambil mempertahankan status putar/jeda. Pilihan terakhir disimpan lokal untuk video berikutnya. Opsi yang muncul bergantung pada stream yang disediakan video.

Paket ini menaikkan `versionCode` ke 9 dan `versionName` ke `1.1.8-ryomd-tube-v3.1`. Build sebagai Android Gradle Project di WZH; jangan mengganti signing key APK yang terpasang.


## Perbaikan kemasan WZH E-PUBLISH (043)

- ZIP ini sudah diratakan: `settings.gradle`, `build.gradle`, dan folder `app/` berada langsung di root ZIP (tanpa folder pembungkus proyek).
- File Gradle memakai nama standar Groovy (`settings.gradle`, `build.gradle`, `app/build.gradle`) agar pemindai WZH yang mencari `settings.gradle` dapat mengenali proyek. Konfigurasi Android/dependency tetap sama dengan source 042.
- Hanya ada satu settings file; varian `.kts` dihapus agar Gradle tidak mendeteksi dua settings file bersamaan.
- Impor sebagai Android Gradle Project / project source, bukan sebagai HTML ZIP atau App Update Center.
- ZIP ini tidak menyertakan `gradle-wrapper.jar`; jalankan melalui Gradle build environment yang disediakan WZH.
