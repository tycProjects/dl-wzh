# UPDATE 1.16.9 — STAGE 71 — BUILDFIX LEAN

Tanggal: 20 Nov 2026

## Ringkasan

Rilis perbaikan build. APK Stage 70 gagal dikompilasi karena **dua import
hilang** yang tertinggal dari editan Stage 70. Semua pesan error yang dilaporkan
compiler (`compileReleaseKotlin`) berasal dari dua akar masalah yang sama, dan
Kotlin selalu melaporkan seluruh error satu file sekaligus dalam satu kali
build, jadi setelah dua import ini ditambahkan, kompilasi selesai.

## Perbaikan build (akar masalah)

| Error compiler | File | Penyebab | Perbaikan |
| --- | --- | --- | --- |
| `IslandRoot.kt:634:34 Unresolved reference 'fillMaxSize'` | `island/IslandRoot.kt` | `Box(Modifier.fillMaxSize())` pada slot crossfade banner tanpa import | tambah `import androidx.compose.foundation.layout.fillMaxSize` |
| `UiKit.kt:51:11 Unresolved reference 'ImageVector'` | `ui/components/UiKit.kt` | parameter `icon: ImageVector` di `SettingsRow` tanpa import | tambah `import androidx.compose.ui.graphics.vector.ImageVector` |
| `UiKit.kt:169:11 Unresolved reference 'ImageVector'` | `ui/components/UiKit.kt` | parameter `icon: ImageVector` di `SwitchRow` (satu akar masalah dengan baris 51) | sama — satu import menutup dua error |

## Audit anti-error-berulang

Untuk memastikan build tidak gagal lagi pada error "Unresolved reference" yang
lain, seluruh file Kotlin (44 file) dipindai dengan pemeriksaan
import-vs-pemakaian untuk ±200 simbol Compose/coroutines/android yang paling
sering tertinggal. Semua temuan yang tersisa sudah diverifikasi manual dan
aman:

- `alpha`, `blur`, `shadow`, `clip` di file island → argumen bernama
  (`copy(alpha = …)`, `SizeTransform(clip = false)`) atau parameter fungsi,
  bukan pemakaian modifier.
- `Text`, `Card`, `State` di `Theme.kt` / `StepTrailModel.kt` /
  `ScreenStateAnalyzer.kt` / `AiClient.kt` → deklarasi lokal/nested milik file
  itu sendiri.
- `first`, `map`, `filter` → operator stdlib pada `List`/`Sequence`
  (otomatis tersedia, tidak butuh import), bukan operator Flow.
- `withContext` di `DeviceActions.kt` → ditulis fully-qualified
  (`kotlinx.coroutines.withContext(...)`), tidak butuh import.
- `scale` di `CallBanner.kt` / `WhatsAppNotificationListener.kt` /
  `ScreenReader.kt` → sudah ter-import / variabel lokal.
- `sample` di `AiPanel.kt` / `AiSession.kt` → variabel lokal bitmap sampling.

Tidak ada missing import lain. `fillMaxSize` pada `CompactIsland`,
`ExpandedIsland`, `HomeScreen`, `WhatsAppBanner` tertutup wildcard
`androidx.compose.foundation.layout.*`; `ImageVector` pada `CallIsland`,
`CallBanner`, `AiPanel` sudah ter-import.

## Ukuran APK (tanpa memangkas fitur)

- `packaging.resources.excludes += "/META-INF/*.version"` — ±40 file metadata
  versi AndroidX yang hanya dibaca tooling, tidak pernah dibaca runtime.
  Dipangkas dari APK tanpa dampak fungsional apa pun.
- Config hemat size yang sudah ada dari Stage 69 tetap: R8 + shrinkResources,
  `abiFilters` arm64-v8a + armeabi-v7a saja (x86 dieliminasi), locale
  resource hanya `en` + `id`, `dependenciesInfo` nonaktif.

## RAM / baterai (status, tanpa perubahan perilaku)

Audit runtime mengonfirmasi stack hemat yang sudah terpasang tetap utuh dan
tidak ada yang diregresi Stage 70:

- Semua loop animasi dekoratif berjalan `delay()` adaptif (30 Hz tier normal,
  lebih hemat di tier bawah) — bukan vsync-driven, jadi Choreographer tidak
  dibangunkan tiap frame.
- Semua loop mematuhi `ScreenState.awaitInteractive()` — benar-benar berhenti
  saat layar mati.
- Ticker posisi lagu 250 ms berbasis aritmetika (satu kali baca jam player,
  tanpa binder call per tick) dan hanya hidup saat panel benar-benar terbuka.
- Mini visualizer mati saat lifecycle STOP dan saat pause.
- Bitmap kelas berat bertingkat: artwork 320/192/96 px per tier, foto kontak
  WhatsApp dibatasi, screenshot OCR 720/1024, gambar AI `inSampleSize` +
  maks 1024 px, tanpa `WakeLock` di mana pun.
- Frost/blur dihitung sekali per artwork + step slider dan di-cache — bukan
  per frame.

## Catatan

Fitur premium, layout, dan resolusi visual tidak diubah. Perubahan rilis ini
murni: dua import yang hilang + pengecualian metadata APK.
