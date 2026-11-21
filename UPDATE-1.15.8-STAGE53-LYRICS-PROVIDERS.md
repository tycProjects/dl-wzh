# Android Island 1.15.8 — Stage 53: verified free lyric provider expansion

- Added Better Lyrics provider-specific QQ endpoint as a fallback after the default TTML endpoint. Parses QRC envelope and preserves absolute word-level timing.
- Added Better Lyrics Kugou endpoint as a line-synced LRC fallback. The provider documentation states these provider-specific endpoints do not require an API key for cache misses.
- Kept LRCLIB as a no-key fallback and updated the HTTP User-Agent to identify the app as Android Island 1.15.8 build, package `com.android.island`.
- Better Lyrics and LRCLIB requests use an app-identifying User-Agent. Better Lyrics attribution is maintained in provider/source labels and app documentation.
- lrc.red was not enabled: its current API terms require both a client URL and contact email in the client identification plus visible credit. The APK contains no verified public project URL or contact email; neither was invented. Add after the owner supplies/approves those public contact details.
- No paid API key is embedded. Provider requests remain sequential, bounded by existing response-size/time limits and existing cache.
- Version: versionCode 153, versionName 1.15.8.

## Verification
- Source-level checks: provider order, endpoint paths, app identity header, and QRC/LRC parsing patterns inspected.
- Gradle compile attempted but could not start because Gradle 8.9 distribution could not be downloaded (`UnknownHostException: services.gradle.org`).
- Live API/device playback tests were not run; provider availability and real-track matching remain to be verified on device.
