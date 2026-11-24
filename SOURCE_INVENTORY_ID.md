# Inventaris Source — Ryomd Tube V3 (basis Native YouTube Player V2.3)

Full Android Gradle source. Dashboard web assets are bundled under `app/src/main/assets/ryomd/`; the existing `RyomdNativeBridge.java`, `RyomdMediaSaver.java`, Media3 playback components, NewPipe extractor integration, and Termux backend routing remain.

## Added in Media Center v1 (retained)

- `app/src/main/java/com/ryomd/nativeapp/youtube/RyomdYouTubeAccountBridge.java` — native Google OAuth authorization, YouTube Data API subscriptions/playlists requests, and scope revocation.
- `app/src/main/assets/ryomd/ryomd-youtube-media.css` — responsive Ryomd Media visual system.
- `GOOGLE_YOUTUBE_ACCOUNT_SETUP_ID.md` — Google Cloud OAuth and YouTube Data API setup instructions.
- `RYOMD_YOUTUBE_MEDIA_CENTER_V1_CHANGELOG_ID.md` — feature changelog and verification limits.

## Updated files

- `app/src/main/assets/ryomd/app.js` — Beranda, Langganan, Riwayat lokal, Koleksi, account connection actions.
- `app/src/main/assets/ryomd/index.html` — adds the media stylesheet.
- `app/src/main/java/com/ryomd/nativeapp/MainActivity.java` — registers the account bridge and routes OAuth activity results.
- `app/build.gradle.kts` — bumped to versionCode 9, versionName 1.1.8-ryomd-tube-v3.1.
- `app/src/main/java/com/ryomd/nativeapp/youtube/RyomdYouTubeExtractor.java` — enumerates available progressive resolutions and resolves the selected height.
- `app/src/main/java/com/ryomd/nativeapp/youtube/RyomdYouTubeCatalogBridge.java` — exposes available quality options to the native app.
- `app/src/main/java/com/ryomd/nativeapp/youtube/RyomdYouTubeBridge.java` — accepts a requested resolution when playback starts.
- `app/src/main/java/com/ryomd/nativeapp/youtube/RyomdYouTubePlayerActivity.java` — adds the fullscreen Kualitas picker and re-extracts the selected stream.
- `app/src/main/java/com/ryomd/nativeapp/youtube/RyomdYouTubePlaybackService.java` — resumes at the previous position and preserves play/pause state after switching resolution.

## Preserved

- Package name `com.ryomd.nativeapp` and local backend origin `http://127.0.0.1:3002/`.
- Existing MediaStore, native playback service, fullscreen player, and Termux API behavior.
- Existing application ID, app identity, and original dashboard tools.

- `RYOMD_TUBE_V3_OAUTH_HOME_FEED_FIXLOG_ID.md` — catatan perbaikan OAuth dan pagination beranda.

- `RYOMD_TUBE_V3_OAUTH_HOME_FEED_FIXLOG_ID.md` — log perbaikan OAuth dan pagination feed.
- `STATIC_VALIDATION_REPORT_TUBE_V3.txt` — hasil pemeriksaan statis dan batas build.

- `RYOMD_TUBE_V3_1_VIDEO_QUALITY_FIXLOG_ID.md` — behavior and limits of video quality selection.
- `STATIC_VALIDATION_REPORT_TUBE_V3_1.txt` — current static checks and build caveats.


## 043 — WZH project-detection packaging fix
- Flattened project layout so `settings.gradle`, `build.gradle`, and `app/` are at ZIP root.
- Replaced Kotlin settings/build scripts with equivalent standard Groovy Gradle scripts for WZH scanner compatibility. Android app coordinates, versionCode 9, dependencies, and native/web assets are retained from 042.
