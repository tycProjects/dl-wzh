# Update 1.14.10 — Fitur AI yang dijanjikan (lanjutan)

## Persetujuan tindakan sensitif (Automation H)
- `ConfirmPolicy`: telepon, SMS, paksa berhenti, ubah pengaturan sistem, bagikan teks, dan
  perubahan `settings put` lewat shell memerlukan balasan "ya" dari pengguna.
- Pemeriksaan kata kunci yang sudah ada tetap dijalankan lebih dulu, sehingga aksi hanya ditawarkan
  bila pengguna memang memintanya.
- Loop agen berhenti saat menunggu persetujuan dan balasan tidak mengklaim tindakan sudah berjalan.
- Balasan "batal" membatalkan; balasan lain membatalkan persetujuan dan pesan diproses biasa.
- Tombol Stop juga membatalkan persetujuan yang menunggu.

## OCR layar (Screen Intelligence A, Smart Search E, Image F)
- Dependensi `com.google.mlkit:text-recognition:16.0.1` (model bawaan, berjalan offline).
- `ScreenOcr` dipakai sebagai cadangan saat aksesibilitas hanya membaca sedikit teks (< 5 node).
- `readScreenWithOcr` dipakai pada aksi `read_screen`.
- Terjemahan layar dilakukan model dari teks yang dibaca (read_screen lalu terjemahkan).

## Ringkasan notifikasi WhatsApp (Notification G)
- `NotificationDigest`: 30 pesan terakhir disimpan hanya di memori, tidak dipersist.
- Aksi `notification_summary` menyerahkan daftar pesan ke model untuk diringkas atau diprioritaskan.

## Belum selesai
- Aksi penulisan khusus (tone, panjang-pendek, balasan WhatsApp) masih lewat prompt.
- Preferensi bahasa favorit dan mode terjemahan offline belum ada.
- Circle to Search (pencarian dari seleksi layar) belum ada.
- Pengenalan suara masih `id-ID`.
- API key masih tersimpan tanpa enkripsi.
- Deteksi kemampuan Vision masih berbasis ID model.
- Belum ada build Gradle dan test perangkat di lingkungan ini.
