# Validasi statis paket

Hasil pemeriksaan lokal:
- ZIP sumber web dan ZIP Android unggahan: integritas arsip valid.
- Aset web di proyek Android diganti dari source web SAFE: 13 file, cocok persis.
- File runtime `logs/`, `pids/`, dan folder backup lama tidak ikut di aset Android.
- AndroidManifest dan resource XML dapat diparse.
- `MainActivity` diarahkan ke `http://127.0.0.1:3002/`.
- Network Security Config menolak cleartext secara default dan memberi pengecualian hanya untuk `127.0.0.1` serta `localhost`.
- Backend Node.js, database, sesi bot, dan service manager tidak disalin/diubah.
- Pemeriksaan ini belum termasuk kompilasi APK atau uji perangkat. Android SDK tidak tersedia pada lingkungan penyusunan paket ini.
