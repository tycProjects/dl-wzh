# Update 1.14.6 — Model yang Sudah Dihentikan

- Jika Gemini membalas 404 "model tidak tersedia lagi" dan menyebut model pengganti, aplikasi memakai pengganti itu untuk permintaan yang sedang berjalan dan memberi tahu pengguna.
- Pengganti hanya dipilih dari nama yang disebut penyedia, bukan dari tebakan.
- Pengaturan tidak diubah otomatis. Pengguna diminta memilih ulang model di pengaturan agar permanen.

Test: ModelFallbackTest. Belum dijalankan (tidak ada toolchain di sandbox).
