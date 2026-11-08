# Android Island 1.4.41 — Idle & Interaction Efficiency

## 1) MiniWaveform
- Animation now runs only while the Compose host View is attached and its Lifecycle is STARTED.
- Tick interval changed from 48 ms (~20.8 updates/s) to 96 ms (~10.4 updates/s), appropriate for a tiny 3-bar indicator.
- `DisposableEffect` removes attach and lifecycle observers on disposal.

## 2) Playback position
- Expanded view now updates active playback position every 250 ms, only while playing and only while Expanded is composed.
- Paused/seeked position is seeded from `media.position` on MediaSession callback-driven state updates; no paused polling loop.

## 3) Accessibility
- `accessibilityEventTypes="0"`, `accessibilityFlags="0"`, `canRetrieveWindowContent="false"`.
- No window event subscription or node-tree retrieval; service remains registered as overlay host.

## 4) DataStore sliders
- Slider thumb uses local Compose state during drag.
- Repository update callback runs once from `onValueChangeFinished`, not per drag event/frame.
- Toggles remain immediate.

## 5) Baseline Profile
- Added `:baselineprofile` Android test module and app profile consumer plugin.
- Generates a startup/dashboard profile on a connected physical device.
- Extend the test with stable UI Automator selectors for expand/collapse, media, and WhatsApp reply flows before treating those paths as covered.

## Validation
- Static source checks and ZIP integrity are performed at packaging.
- Full Gradle compile/profile generation and device performance measurements require network-resolved Gradle dependencies and a connected Android device; not claimed as passed here.

## Cara menghasilkan profile di perangkat
1. Pastikan Android Studio/SDK, platform-tools, dan perangkat fisik dengan USB debugging siap.
2. Jalankan `./gradlew :baselineprofile:connectedBenchmarkAndroidTest` untuk menjalankan generator pada perangkat terhubung.
3. Jika task agregat tersebut tidak tersedia pada versi plugin yang terpasang, jalankan `./gradlew :baselineprofile:tasks --all` dan gunakan task `generateBaselineProfile` yang dihasilkan plugin.
4. Profile hasil generator akan dipasangkan ke app melalui `baselineProfile(project(":baselineprofile"))`.
5. Bangun release: `./gradlew :app:assembleRelease` lalu install dan ukur cold start serta interaksi pada perangkat fisik.
6. Tambahkan selector UI Automator yang stabil untuk membuka capsule expanded, menekan media controls, membuka stack WhatsApp, dan fokus kolom reply. Profile awal yang disediakan hanya mencakup startup/dashboard; jangan menganggap jalur overlay sudah terprofil sebelum skenario tersebut direkam.
