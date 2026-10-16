# NovaFloating Assistive Reader - Android Native (Kotlin)

Aplikasi utilitas pembantu aksesibilitas layar (Assistive Screen Reader Tool) dengan bilah widget melayang (WindowManager TYPE_APPLICATION_OVERLAY) dan AccessibilityNodeInfo traversal untuk mengekstrak teks pada jendela aktif lalu meneruskan pencarian ke Google.

## Spesifikasi Proyek:
- **Application Name**: NovaFloating Assistive Reader
- **Package Name / applicationId**: com.novafloating.app
- **minSdk**: 24 (Android 7.0 Nougat) -> 100% Anti-Parse Error
- **compileSdk**: 35 (Android 15)
- **targetSdk**: 35 (Android 15)
- **Bahasa**: 100% Kotlin Native
- **Izin Utama**:
  - `SYSTEM_ALERT_WINDOW`
  - `FOREGROUND_SERVICE` & `FOREGROUND_SERVICE_SPECIAL_USE`
  - `POST_NOTIFICATIONS`
  - `android.permission.BIND_ACCESSIBILITY_SERVICE`

## Fitur Bilah Melayang (FloatingService):
1. **Ikon ⠿ (Drag Handle)**: OnTouchListener untuk memindahkan posisi widget di layar secara halus.
2. **Tombol "⚡ SCAN & BACA LAYAR"**:
   - Membaca seluruh teks pada jendela aktif via `ScreenReaderAccessibilityService`.
   - Menghasilkan query: `"Informasi dan pembahasan materi: [Teks yang diekstrak dari layar]"`.
   - Membuka browser Google otomatis (`Intent.ACTION_VIEW`).
   - Dapat ditekan berulang kali tanpa batas (bar TIDAK BOLEH tertutup otomatis setelah scan).
3. **Tombol "✕"**: Menutup bilah melayang dan menghentikan Foreground Service.

## Kontak & Lisensi:
- Telegram: https://t.me/croxfloatingujian (@croxfloatingujian)
- WhatsApp: https://wa.me/6288211614094 (+6288211614094)
