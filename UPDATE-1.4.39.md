# Android Island 1.4.39

## Perbaikan swipe lagu di capsule
- Mengaktifkan gesture horizontal saat capsule compact menampilkan media.
- Swipe kiri mengirim `skipToNext()`, swipe kanan mengirim `skipToPrevious()`.
- Capsule tidak diterjemahkan/ditarik mengikuti jari; posisi X/Y tetap.
- Gesture horizontal tetap nonaktif untuk capsule WhatsApp.
- Swipe-dismiss tetap nonaktif pada capsule media agar tidak bertabrakan dengan navigasi lagu.

Catatan: perbaikan ini mengirim perintah ke MediaSession aktif. Perpindahan lagu tetap bergantung pada player yang sedang aktif dan dukungannya terhadap skip next/previous.
