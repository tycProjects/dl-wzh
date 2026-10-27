# PorStore

- Package: `com.game.play` · Kotlin + Jetpack Compose + Material 3
- API: https://por-gamecp.vercel.app (`/api/apps`, `/api/detail`, `/api/download`, `/api/rating`, `/api/upload-token`, `/api/submit`)

## Build di Code On The Go
1. Ekstrak zip, buka folder yang berisi `settings.gradle.kts` (folder `PorGameList`).
2. Jalankan `:app:assembleDebug`.

## Fitur
- Data dari API, disimpan lokal → tetap tampil saat offline (tarik layar ke bawah untuk refresh).
- Tidak ada demo: kalau tidak ada aplikasi / APK, tampil pesan saja.
- Tombol **Buka** hanya muncul setelah aplikasi terpasang; **Perbarui** muncul kalau versi server lebih baru.
- Akun: tanpa email, nama `user1`, `user2`…; nama dan foto profil bisa diganti. Tidak ada logout.
- **Buat Aplikasi**: nama, package, deskripsi, kategori, file `.apk.ROP`, foto/ikon, screenshot + checklist verifikasi.
- Pengaturan yang benar-benar berfungsi: Notifikasi (progres unduhan, aplikasi baru, pembaruan), Mode gelap, Unduh hanya Wi-Fi.

## Catatan
- Parser respons API toleran (array langsung atau `{apps|data|items|results: [...]}`; nama field umum seperti `name/title`, `packageName/package`, `downloadUrl/apkUrl`, `icon/iconUrl`).
- Alur kirim `/api/upload-token` → unggah → `/api/submit` memakai dugaan nama field. Kalau server menolak, pesan asli server tampil di layar → sesuaikan di `data/SubmitRepository.kt`.
- Nomor pengguna (`user1`) saat ini lokal per perangkat.
- Kode belum dikompilasi di lingkungan penyusunan; build ulang di Code On The Go.
