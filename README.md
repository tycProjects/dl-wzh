# NovaFloating Area Reader - Android Native Kotlin

Aplikasi Floating Assistive Utility dengan sistem Crop Selection Box (kotak area seleksi melayang) dan In-App Floating AI Viewer (WebView overlay tanpa pindah ke Chrome).

## Spesifikasi:
- **Paket / Namespace**: `com.novafloating.app`
- **minSdk**: 24 (Android 7.0 Nougat) -> 100% Anti Parse Error
- **compileSdk / targetSdk**: 35 (Android 15)
- **Komponen Utama**:
  1. `MainActivity.kt`: Layar utama Cosmic Dark Space (#0B0C1A, #161B33, #00E5FF), pengecekan izin SYSTEM_ALERT_WINDOW, Accessibility, POST_NOTIFICATIONS, tombol START besar memanggil moveTaskToBack(true), dan lisensi resmi Telegram @croxfloatingujian & WhatsApp +6288211614094.
  2. `FloatingService.kt`: Mengelola Floating Trigger Bubble, Floating Selection Crop Box (dengan drag & resize serta tombol [✔] & [✕]), dan In-App Floating AI Viewer (WebView melayang tanpa pernah keluar dari aplikasi).
  3. `AreaReaderAccessibilityService.kt`: Traversal rekursif `rootInActiveWindow` menggunakan `getBoundsInScreen(rect)` untuk menyaring dan mengekstrak teks hanya di dalam area koordinat kotak seleksi.

## Lisensi Resmi & Kontak:
- Telegram: [@croxfloatingujian](https://t.me/croxfloatingujian)
- WhatsApp: [+6288211614094](https://wa.me/6288211614094)
