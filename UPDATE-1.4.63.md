# Android Island 1.4.63

## Liquid expansion (animasi diambil sebagai contoh dari Dynamic Island karya Akira 1.6.5)
- Konten lama dan baru sekarang hidup bersamaan selama perpindahan (AnimatedContent), seperti di referensi.
- Konten baru tumbuh dari lebar konten lama, dan konten lama menyusut ke lebar baru sambil memudar.
- Gerak bentuk memakai pegas island yang sama dengan pengaturan "Animasi". Pudar memakai pegas yang lebih kaku dan tanpa pantulan.
- Blur (Android 12+) kuat di awal lalu mereda. Tetap mati secara bawaan.
- Otomatis mati saat mode hemat dihapus. Liquid expansion sekarang mengikuti pengaturan saja.
- Implementasi ditulis ulang sendiri; tidak ada kode yang disalin dari repo Akira.

## Catatan
Kode belum dikompilasi di lingkungan ini. Jalankan build debug dan kirim log bila ada error.
