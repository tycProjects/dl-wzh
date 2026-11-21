# Update 1.14.11 — Circle to Search, provider terenkripsi, verifikasi Keystore, tampilan panel

## Circle to Search (Smart Search E)
- Aksi `search_screen`: mengambil screenshot layar (tanpa overlay), membaca teks dengan OCR
  (ML Kit), dan mengambil teks aksesibilitas. Hasilnya diberikan ke loop agen, lalu model memakai
  `web_search` dengan kata kunci yang paling relevan.
- Dipicu lewat kalimat seperti "cari ini", "apa ini", atau pertanyaan tentang objek di layar.
- Batasan: objek tanpa teks hanya dijelaskan jika model memakai gambar (lampiran screenshot).

## Provider (lintas sistem)
- API key dan cadangan key (`aiExtraKeys`) disimpan terenkripsi AES-256-GCM dengan kunci Android
  Keystore (`SecretVault`), tidak lagi sebagai teks biasa.
- Installasi lama dimigrasikan otomatis: plaintext dibaca sebagai cadangan lalu dihapus saat
  pengaturan berikutnya disimpan.
- Bila Keystore tidak tersedia, key tidak disimpan sama sekali (tidak pernah sebagai plaintext) dan
  kejadian dicatat di diagnostik.

## Verifikasi Keystore
- Ketik "cek keystore" di chat AI. Balasan melaporkan apakah penyimpanan API key dan memori AI
  benar-benar berfungsi di perangkat tersebut (uji enkripsi-dekripsi nyata).

## Panel AI
- Tinggi panel 80% layar (saat mengetik 86%), maksimum 720 dp, sehingga memanjang ke bawah.
- Chip aksi diperkecil: tinggi minimum 30 dp, ikon 13 dp, teks 11 sp, jarak antarbaris 4 dp.

## Catatan
- Key yang sudah terenkripsi tidak terbaca di perangkat lain atau setelah restore backup. Pengguna
  perlu mengisi ulang key di perangkat baru.
- Belum diverifikasi di perangkat; Gradle belum dijalankan di lingkungan ini.
