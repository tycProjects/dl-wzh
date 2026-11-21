# Update 1.14.2 — Katalog Gemini Diverifikasi

## Verifikasi (November 2026)
- gemini-3.8-flash: ada. Disebut sebagai pengganti di changelog dan daftar deprekasi Google. Penolakan sebelumnya keliru.
- gemini-2.5-pro dan gemini-2.5-flash: deprecated sejak 2 November 2026. Mulai 1 Desember 2026, permintaannya dialihkan Google ke Gemini 3.8 Flash dan 3.5 Flash-Lite. Tanggal 16 Oktober tidak berlaku.
- gemini-4-argon: tersedia sejak 13 Oktober 2026 menurut laporan yang mengutip Google.
- gemini-4-carbon, gemini-4-hydrogen, gemini-decision-1: belum terverifikasi. Tidak ditambahkan ke katalog.
- gemini-nano-banana-2.1: disebut sebagai pengganti di daftar deprekasi. Ditambahkan ke katalog gambar di urutan terakhir.

## Perubahan kode
- Filter chat mengecualikan "decision" (klasifikasi, bukan chat).
- Katalog gambar menambahkan gemini-nano-banana-2.1 di urutan terakhir.
- Daftar model tetap diambil dari /models, sehingga model yang dihentikan atau belum ada tidak muncul.

## Status
Test ditambahkan lebih dulu. Belum dijalankan (tidak ada toolchain di sandbox).
