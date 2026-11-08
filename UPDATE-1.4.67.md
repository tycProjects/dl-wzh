# Android Island 1.4.67

## Perbaikan build
- MainActivity.kt: menghapus override onRequestPermissionsResult yang tidak cocok dengan ComponentActivity. Permintaan izin READ_PHONE_STATE sekarang memakai registerForActivityResult, dan layar disegarkan setelah jawaban.

## Catatan
Kode belum dikompilasi di lingkungan ini. Jika build masih gagal, kirim log terbaru.
