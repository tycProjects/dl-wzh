# Android Island 1.4.7 — Audit report

## Fixed in this revision

1. **Manifest parse failure:** removed a literal `\n` token embedded between permission elements.
2. **Shizuku status-bar flag:** corrected the notification-icon disable mask to `1 << 1` (`DISABLE_NOTIFICATION_ICONS`).
3. **Shizuku lifecycle:** listener registration is idempotent; permission requests only run after the binder is detected and permission is actually required.
4. **Status-bar compatibility:** tries the legacy `disable()` signature and falls back to `disableForUser()` using `UserHandle.myUserId()`.
5. **Event queue safety:** serializes mutations to the shared IslandBus queue/snapshot so concurrent listener callbacks cannot interleave queue rebuilds.
6. **Cutout detection:** uses `DisplayCutout.boundingRects` and safe platform geometry on Android 11+; older versions retain a clearly approximate status-bar-height fallback.
7. **Accessibility privacy:** disables view-tree content retrieval because the app does not need to inspect other apps' UI to render its overlay.
8. **Overlay hit bounds:** compact and idle scenes no longer reserve the entire display width. This reduces the risk of a transparent accessibility overlay intercepting taps outside the island. The WhatsApp banner window remains as wide as the banner card itself.
9. **Build hygiene:** increments app version to 1.4.7 / versionCode 18, adds offline structural verification and a GitHub Actions build workflow.

## Validation performed here

- All XML files parsed successfully with Python's XML parser.
- Structural verifier passed: required services, version metadata, manifest escape check and accessibility privacy flag.
- Static source checks passed for Shizuku registration, status-bar flag, bounded compact overlay, DisplayCutout API and synchronized event queue.
- ZIP archive integrity is checked after packaging.

## Validation not possible in this environment

`./gradlew :app:assembleDebug` was attempted. The Gradle wrapper could not download Gradle 8.9 because `services.gradle.org` was unreachable (`UnknownHostException`). No Android SDK is installed in this environment. Therefore Kotlin compilation, Android Lint, unit tests, APK installation and physical-device tests are **not verified** here. The added GitHub Actions workflow builds the debug APK when pushed to a network-enabled GitHub runner with Android SDK packages installed.

## Remaining engineering gaps (not silently marked complete)

- Notification listener currently mirrors WhatsApp and WhatsApp Business, not Telegram, Instagram, Messenger, Gmail or SMS. Those need a generic notification model, package/category policy, per-app settings and a dedicated presenter.
- Media is based on active Android MediaSession controllers; player-specific favorite/rating support varies.
- Shizuku cannot grant Accessibility or Notification Listener consent. Those remain explicit Android Settings actions.
- Legacy Android cutout classification is approximate before the public `DisplayCutout` geometry is available.
- No physical-device test has been run for Samsung One UI, Xiaomi HyperOS, Pixel Android or OEM-specific background restrictions.
