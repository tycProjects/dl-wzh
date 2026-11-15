# Android Island 1.4.24 — Kotlin compile fix

- Fixed `IslandRoot.kt` declaration order: `bannerKey` is now resolved before the `LaunchedEffect` blocks that use it.
- This resolves the two reported Kotlin errors (`Unresolved reference: bannerKey` at the banner expiry effect).
- No banner timing, WhatsApp queue, indicator, capsule geometry, or lyric rendering behavior was intentionally changed in this build-fix release.

Build verification note: Gradle 8.9 could not be downloaded in the authoring environment because `services.gradle.org` was unreachable. The source-level fix is applied, but a full Gradle compile has not been verified here.
