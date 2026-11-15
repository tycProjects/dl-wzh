# Android Island 1.4.51

## Perbaikan build dari 1.4.50
- Memulihkan implementasi `ExpandedIsland` yang tertimpa salinan `WhatsAppBanner`.
- Menghilangkan deklarasi `WhatsAppBanner` ganda yang menyebabkan conflicting overload dan pemanggilan ambigu.
- Memulihkan `LikedTracks` yang berada di dalam file `ExpandedIsland.kt`.
- Menghapus lapisan permukaan putih/hitam yang sebelumnya menimpa panel Expanded dan banner; pilihan warna hanya memengaruhi foreground.
- Menyesuaikan pemetaan warna foreground Expanded dengan opsi `ADAPTIVE`, `WHITE`, `BLACK`, dan `OUTLINE`.

Build release tetap perlu diverifikasi di lingkungan Gradle/Android SDK.
