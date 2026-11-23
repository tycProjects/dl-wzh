# Ryomd Native MediaStore Fix v2 — WZH Android Gradle Source

## Perubahan v2
- Bridge JavaScript native kini menyediakan `canSaveToGallery()`, `isMediaStoreAvailable()`, `saveToGallery(url)`, `saveMedia(url, filename, mimeType)`, dan `saveToGalleryWithDetails(...)`.
- Bridge hanya menerima endpoint file lokal Ryomd pada `127.0.0.1:3002/api/downloads/file`.
- DownloadListener lama tetap dipertahankan sebagai fallback; deduplikasi URL aktif untuk mencegah dua penangan mengunduh URL sama bersamaan.
- MIME media dipilih berdasarkan ekstensi saat server memberi MIME generik; HTML/JSON ditolak sebagai respons error.
- Ukuran transfer diperiksa terhadap Content-Length bila tersedia. Record sementara dibersihkan bila tidak selesai.
- Setelah MediaStore dipublikasikan, record di-query ulang. Toast sukses hanya muncul setelah record ditemukan, ukuran > 0, MIME/nama cocok, dan `IS_PENDING=0`.
- Toast sukses menyebut lokasi dan ukuran file, misalnya `Movies/Ryomd/nama.mp4 · 18.4 MB`.
- `applicationId` tetap `com.ryomd.nativeapp`; `versionCode=3`; `versionName=1.1.2-termux-bridge-mediastore-verified`.

## Lokasi
Video: `Internal storage/Movies/Ryomd/`; audio: `Internal storage/Music/Ryomd/`; gambar: `Internal storage/Pictures/Ryomd/`.

## Build
Impor ZIP sebagai Android Gradle source di WZH. Pertahankan `applicationId` dan signing key/keystore yang sama dengan APK terpasang. Source memakai versionCode 3. Build APK lalu pasang di atas APK lama; jangan uninstall APK lama.

## Batas verifikasi
ZIP dan XML diperiksa secara statis. Android SDK/Gradle build dan uji HP fisik belum tersedia di lingkungan penyusunan. Bila signing key WZH berbeda, update langsung akan ditolak Android. Jangan uninstall aplikasi lama.
