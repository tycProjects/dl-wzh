# Android Island 1.4.59

## Perbaikan build (compileReleaseKotlin)
- CompactIsland.kt: menambahkan import `androidx.compose.material.icons.filled.Call` untuk ikon panggilan di kapsul.
- WhatsAppNotificationListener.kt: membaca rating pemutar dengan API yang benar `Rating.hasHeart()` dan `Rating.isThumbUp()` (sebelumnya memakai properti `isHeart` yang tidak ada).

## Catatan
Fitur dari 1.4.58 (panggilan expanded, glow per kata, status like dari pemutar, profil animasi, pengelompokan pengaturan, pengecilan artwork) tidak berubah.
