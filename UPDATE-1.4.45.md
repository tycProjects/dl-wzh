# Android Island 1.4.45 — Anti-kedip, animasi mulus, hemat daya

## Kedip latar blur tiap ~2 detik (diperbaiki)
Penyebab: `MediaController.metadata` memberi objek Bitmap BARU di setiap callback pemutar (±2 detik). Bitmap baru dianggap "foto baru" → blur dibangun ulang + cross-fade 420 ms. Cross-fade dua foto beralpha 0,8 membuat kecerahan turun sesaat (kedip), dan latar island transparan sehingga isi aplikasi di belakangnya ikut tembus.
- Listener menyimpan artwork per lagu; Bitmap yang sama dipakai ulang.
- Callback yang hanya mengubah posisi (selisih < 1,5 dtk) tidak lagi memicu update UI.
- Backdrop memakai kunci isi gambar: foto sama = instance sama = tanpa animasi ulang.
- Dasar hitam solid di bawah foto; foto digambar opaque + 1 lapisan gelap (bukan alpha). Foto baru masuk di atas foto lama, tanpa penurunan kecerahan.
- Blur dibuat di latar belakang begitu lagu berganti (pre-warm), jadi buka expand langsung siap.

## Animasi buka-tutup patah-patah (diperbaiki)
- `IslandRoot` membaca `progress` di fase komposisi → seluruh tree recompose di SETIAP frame. Sekarang komposisi hanya melihat 2 boolean (derivedStateOf); alpha, skala, dan radius sudut dihitung di `graphicsLayer` (GPU saja).
- Jam drift fluid glass: 30 Hz dengan `delay` (bukan tiap vsync), siklus 24 dtk.

## Hemat CPU / GPU / RAM
- Perekaman log mati secara default. Dulu setiap catatan menyalin hingga 1.800 baris + format tanggal + Logcat, belasan kali per detik saat animasi. Sekarang buffer 600 baris, snapshot UI maksimal tiap 0,7 dtk, Logcat hanya di build debug.
- Cache blur 3 entri (beberapa KB) berdasar isi; `asImageBitmap()` dibungkus `remember`.

## Mudah untuk pengguna awam
- BARU: "Profil cepat" — Mewah / Seimbang / Hemat dalam satu ketukan (Pengaturan → Performa & baterai).
- BARU: tombol "Setel ulang pengaturan" (bagian Pemulihan, paling bawah) dengan dialog konfirmasi. Izin sistem tidak berubah. Sebelumnya fungsi `resetAll()` ada tetapi tidak punya tombol.

## Catatan
Belum dikompilasi/diuji di perangkat dari sisi saya (tanpa Android SDK & jaringan). Struktur paket dan keseimbangan kurung sudah dicek; kompilasi diverifikasi oleh build Anda.
