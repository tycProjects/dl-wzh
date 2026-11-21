# Update 1.12.0 — Tugas Berurutan & Scroll Berulang

## 1. Brainstorming & analisis kebutuhan
Contoh permintaan: "buka tiktok lalu scroll 2 kali".

Masalah yang ditemukan di kode 1.11.7:
- Jalur lokal (AI belum diatur) tidak mengenali kata "scroll" sama sekali.
- Aksi scroll tidak punya jumlah. Model harus mengulang aksi, dan itu tidak andal.
- Scroll hanya memakai ACTION_SCROLL_FORWARD. Feed seperti TikTok sering tidak punya node
  yang bisa di-scroll, sehingga muncul pesan "Tidak ada area yang bisa di-scroll".
- Tidak ada aksi tunggu, sehingga langkah berikutnya bisa jalan sebelum aplikasi siap.
- Tidak ada batas jumlah aksi per giliran.

## 2. Desain
- `Scroll(down, times)`: times 1–20. Dikenali dari "scroll/gulir/geser", arah "atas/bawah",
  dan jumlah "2 kali", "dua kali", "tiga kali", dst.
- Fallback swipe lewat `dispatchGesture` ketika tidak ada node scrollable.
- `Wait(ms)`: "tunggu 3 detik", atau dari JSON model `{"type":"wait","ms":2000}`. Maks 10 detik.
- Batas 12 aksi per giliran (`AiActionProtocol.limit`).
- Prompt sistem menjelaskan urutan multi-langkah dengan contoh TikTok.

## 3. Rencana implementasi (TDD)
1. Test pada AiCommandRouterTest, IntentResolverTest, AiActionProtocolTest, ScrollGeometryTest.
2. Tambah `Scroll.times` dan `Wait`, parser lokal, parser JSON.
3. `ScrollGeometry.swipeY` (murni, dapat diuji) dan fallback swipe di accessibility service.
4. `DeviceActions.scrollTimes` dan `DeviceActions.wait`, dihubungkan ke `executeLocal`.
5. Batas aksi per giliran dan pembaruan prompt.

## 4. Berkas yang berubah
- ai/AiCommandRouter.kt, ai/IntentResolver.kt, ai/AiActionProtocol.kt, ai/AiSession.kt, ai/DeviceActions.kt
- ai/ScrollGeometry.kt (baru)
- service/IslandAccessibilityService.kt (scroll fallback + swipe)
- test: AiCommandRouterTest, IntentResolverTest, AiActionProtocolTest, ScrollGeometryTest (baru)
- build.gradle.kts (versionCode 127, versionName 1.12.0), VERSION.txt

## 5. Status verifikasi
Test ditulis lebih dulu. Lingkungan build di sesi ini tidak memiliki Android SDK maupun Gradle,
sehingga test belum dijalankan. Jalankan `./gradlew testDebugUnitTest` untuk memverifikasi.

## 6. Yang belum (rekomendasi berikutnya)
- Aksi long_press, swipe dengan koordinat, dan pengulangan "sampai teks X muncul".
- Konfirmasi sebelum aksi sensitif (kirim SMS, hapus, pembelian).
- Pemulihan bila aksi gagal di tengah rencana.

## 7. Tambahan 1.12.1 — Putar lagu langsung di aplikasi pilihan
- Aksi baru `play_in_app {app, query}`: buka aplikasi (mis. Auralys), cari lagu, ketuk hasil yang cocok,
  lalu kirim perintah putar. Jika aplikasi tidak memulai sendiri, fallback ke kontrol media.
- Dipetakan di IntentResolver, AiActionProtocol (prompt), AiSession (executor) dan DeviceActions.playInApp.
- Test: IntentResolverTest (parse dan validasi query kosong).
- Status: belum dikompilasi/dijalankan (tidak ada Android SDK di sesi ini).
