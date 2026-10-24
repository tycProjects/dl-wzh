# DEV RAFA GAME BOOSTER X

Proyek Android native Kotlin + Jetpack Compose, tanpa login. Tema hitam/putih/silver.

## Fitur yang diimplementasikan
- Dashboard dan empat profil mode (Performance, Balanced, Battery Saver, Light).
- Cursor Lab dengan 5 gaya kursor visual dan pengaturan ukuran, transparansi, ketebalan, serta posisi.
- Overlay kursor dan floating panel melalui Android `WindowManager` (izin overlay diperlukan).
- Device Care: RAM tersedia, kapasitas penyimpanan, baterai, suhu baterai jika tersedia, refresh rate.
- Pemeriksaan konektivitas dasar dan shortcut ke pengaturan Android.
- Penyimpanan preferensi lokal memakai SharedPreferences.
- Status FPS ditandai tidak tersedia kecuali aplikasi mempunyai sumber data yang sah; tidak memalsukan angka FPS.
- Tidak memerlukan root dan tidak mengubah file internal Free Fire.

## Build APK
1. Buka folder proyek ini di Android Studio versi terbaru.
2. Pastikan Android SDK Platform 35 dan JDK 17 tersedia.
3. Tunggu Gradle Sync.
4. Pilih **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
5. APK debug biasanya muncul di `app/build/outputs/apk/debug/app-debug.apk`.

Proyek ini adalah source project, bukan APK yang sudah dikompilasi. Build dan uji di perangkat fisik sebelum distribusi.

## Overlay
Aktifkan tombol izin overlay dari aplikasi. Android akan membuka halaman pengaturan izin khusus. Setelah izin diberikan, aktifkan Cursor Overlay atau Floating Panel. Hentikan dari aplikasi untuk menghapus overlay.

## Catatan kemampuan Android
Aplikasi biasa tidak dapat menjamin kenaikan FPS, membaca FPS internal Free Fire secara universal, membersihkan RAM aplikasi lain secara paksa, atau mengubah governor CPU/GPU tanpa hak khusus. Optimasi di proyek ini berfokus pada profil, monitoring data yang tersedia, pengurangan kerja aplikasi sendiri, dan shortcut pengaturan resmi.
