# Inventaris Source — Ryomd Native YouTube Player V2.3 baseline + Ryomd Tube quality/feed update

Full Android Gradle source. Dashboard web assets remain bundled under `app/src/main/assets/ryomd/`; existing `RyomdNativeBridge.java`, `RyomdMediaSaver.java`, Media3 playback service, NewPipe Extractor catalog, and Termux backend routing are retained.

## Updated in this release

- `RyomdYouTubeCatalogBridge.java` — request and paginate up to 100 public video results, with duplicate filtering and partial-page failure recovery.
- `RyomdYouTubeExtractor.java` — expose actual progressive video heights and resolve requested/Auto stream quality.
- `RyomdYouTubeBridge.java`, `RyomdYouTubePlaybackService.java`, `RyomdYouTubePlayerActivity.java` — in-player quality menu that re-resolves the selected stream while preserving playback position.
- `app/src/main/assets/ryomd/app.js` — Google account flow removed; local-only subscriptions/history remain; public home feed requests 100 videos; refresh button and pull-down refresh added; link playback occupies fourth tab.
- `app/src/main/assets/ryomd/ryomd-uiux.css` — YouTube-inspired mobile layout and refresh affordance using UI/UX Pro Max guidance.
- `MainActivity.java` and `app/build.gradle` — remove Google account bridge and Play Services Auth dependency.
- `app/src/main/res/values/strings.xml` — launcher label set to `Ryomd panel`.
- Root `settings.gradle`, `build.gradle`, and `app/build.gradle` — conventional Groovy Gradle filenames at ZIP root/module for WZH project detection.

## Preserved

- Package name `com.ryomd.nativeapp` and backend origin `http://127.0.0.1:3002/`.
- Existing MediaStore, native playback, fullscreen player, local channel subscriptions, local watch history, Takeout import, and Termux API behavior.
- The visible YouTube feature name `Ryomd Tube`.


Catatan provenance: paket ini dibuat dari file sumber V2.3 yang diunggah ulang pengguna. File changelog versi Media Center/OAuth lama dipertahankan hanya sebagai dokumentasi sejarah; bridge, UI, dan dependensi login Google tidak berada di jalur build aktif paket ini.
