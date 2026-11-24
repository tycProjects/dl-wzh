# RYOMD YouTube Native Player V2.2 — Fix log

Basis: V2.1. `applicationId` tetap `com.ryomd.nativeapp`. `versionCode` 5, `versionName` 1.1.4-youtube-native-v2.2-fix.

## Bug yang diperbaiki
1. **Tombol native tidak muncul/tidak jalan** — WebView memuat UI dari server Termux, bukan dari APK, sehingga
   app.js versi APK (tombol pemutar native) tidak pernah terpakai. Sekarang UI (index.html, app.js, styles, modul)
   dilayani dari APK lewat `shouldInterceptRequest`; `/api/*` tetap ke Termux `127.0.0.1:3002`.
   Konstanta `USE_BUNDLED_UI` di `MainActivity.java` (false = perilaku lama).
2. **Crash saat memutar (Android 8+)** — `startForegroundService()` tanpa `startForeground()` tepat waktu →
   `ForegroundServiceDidNotStartInTimeException`. Diganti `startService()` (Media3 menaikkan ke foreground sendiri).
3. **Layar tertutup status bar di Android 15** (targetSdk 35 = edge-to-edge paksa) — ditambahkan penanganan WindowInsets
   (status bar, navigation bar, cutout, keyboard).
4. **Tidak ada notifikasi kontrol pemutar di Android 13+** — izin `POST_NOTIFICATIONS` ditambahkan dan diminta sekali.
5. **Kualitas video mentok 360p** — resolver kini memilih video adaptif sampai 1080p (hindari AV1) digabung audio
   terbaik; fallback ke stream progressive; manifest DASH/HLS diberi MIME eksplisit.
6. **Fullscreen video/iframe di WebView tidak berfungsi** — ditambah `onShowCustomView/onHideCustomView`.
7. **Crash bila proses render WebView dimatikan sistem** — `onRenderProcessGone` ditangani.
8. **Layar pemutar**: pesan status/galat tampil (bukan layar hitam diam), kontrol musik selalu terlihat, video berhenti
   saat layar ditutup (musik tetap lanjut di latar), orientasi bebas, target sentuh tombol kembali 48dp.
9. **Tab "Video / Channel / Playlist" hanya teks, tidak bisa diklik** — jadi tab sungguhan (Video, Channel, Dari tautan).
   Tab baru **Dari tautan**: tempel URL/ID → putar native tanpa backend pencarian. (Playlist native belum diimplementasi.)
10. Kode mati: `case 'yt-player'` ganda dihapus; deskripsi tool tidak lagi menjanjikan playlist.

## UI/UX (repo UI/UX Pro Max)
File baru `assets/ryomd/ryomd-uiux.css` (additive): palet Dark OLED + "play green", target sentuh 44–48dp, jarak ≥8px,
focus ring, skeleton loader + `aria-busy`, `prefers-reduced-motion`, `overscroll-behavior`, safe-area bottom sheet,
ikon SVG menggantikan emoji pada tombol pemutar.

## Catatan penting
- Karena UI kini dari APK, fitur "App Update Center" (unggah ZIP UI ke server) tidak lagi mengubah tampilan di dalam APK.
  Untuk update UI: bangun ulang APK, atau set `USE_BUNDLED_UI = false`.
- Signing key WZH harus sama dengan APK terpasang agar bisa update-in-place.
- Belum dikompilasi Gradle & belum diuji di perangkat (lihat STATIC_VALIDATION_REPORT_V2_2.txt).
