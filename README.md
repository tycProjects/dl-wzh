# Musify 5.1 — Android native (Kotlin + Jetpack Compose + Media3)

Port dari web Musify v5.1. Tampilan, pemutar, favorit, dan playlist ditulis ulang native.
**Backend tidak ikut di dalam APK**: aplikasi memanggil folder `api/` dari proyek web
(`/api/search`, `/api/ytplay`, `/api/proxy-audio`, `/api/lyrics`, `/api/artist`, `/api/album`, `/api/suggest`),
jadi backend itu harus tetap ter-deploy (Vercel / VPS / Railway).

## 1. Atur alamat backend
Pilih salah satu:
- Edit `gradle.properties` -> `MUSIFY_BASE_URL=https://domain-kamu.vercel.app`, atau
- Setelah aplikasi terpasang: tab **Profil > Server**, isi alamat, tekan *Tes koneksi*.

## 2. Build APK
**Android Studio**: File > Open folder ini > tunggu sync > Build > Build APK(s).
Hasil: `app/build/outputs/apk/debug/app-debug.apk`.

**Tanpa komputer (GitHub Actions)**: upload folder ini ke repo GitHub -> tab *Actions* ->
*Build APK* -> *Run workflow* -> unduh artefak `Musify-5.1-debug-apk`.

**Command line**: `gradle assembleDebug` (Gradle 8.7, JDK 17).

APK debug sudah bisa dipasang. Untuk rilis, buat keystore lalu `assembleRelease` dengan signing config.

## Struktur
```
app/src/main/java/com/zychocuwek/musify/
├─ data/    Models · Api (klien backend) · Store (favorit, playlist, URL server)
├─ player/  PlaybackService (ExoPlayer + notifikasi + layar kunci) · PlayerController
└─ ui/      MainActivity · Screens (Beranda, Cari, Pustaka, Artis, Album, Profil)
            NowPlaying (lirik tersinkron, antrean, kecepatan) · Components · Theme
```

## Yang dipindahkan dari versi web
Pencarian real-time + saran, beranda, artis, album/playlist, pemutar penuh, lirik tersinkron,
antrean, acak/ulang/kecepatan, Disukai, playlist buatan sendiri (maks 15 lagu), pemulihan
pemutaran otomatis (link baru x3 -> versi lain lagu -> lewati), putar di latar belakang,
palet "terong & saffron", halaman Profil.

## Belum dipindahkan
Banner video MP4 (diganti poster), animasi loading kustom, Pusat Notifikasi dalam aplikasi,
terjemahan lirik, tombol unduh, mode offline PWA.
