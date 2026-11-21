# Android Island 1.4.62

## Liquid expansion (terinspirasi Dynamic Island karya Akira, versi 1.6.5)
- Saat konten berganti (misalnya media ke WhatsApp), konten baru tumbuh dari lebar konten sebelumnya dengan pegas island, lalu memudar dengan kurva yang lebih cepat.
- Implementasi ditulis ulang sendiri di `island/LiquidReveal.kt`. Tidak ada kode yang disalin dari repo Akira, karena repo tersebut tidak memuat lisensi.
- Pengaturan baru di bagian "Kustomisasi · Efek liquid":
  - Liquid expansion: aktif secara bawaan.
  - Blur saat berganti: mati secara bawaan. Hanya Android 12 ke atas.
- Dimatikan otomatis saat mode hemat (reduced motion) aktif.

## Batasan
- Versi ini hanya menganimasikan konten yang masuk. Konten lama langsung hilang, tidak ikut keluar dengan animasi seperti di repo Akira.
- Fitur lain dari Akira (kilau kaca, goyang, gelombang pembayaran, dan lain-lain) belum diport.

## Catatan
Kode belum dikompilasi di lingkungan ini. Jalankan build debug dan kirim log bila ada error.
