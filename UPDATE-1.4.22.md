# Android Island 1.4.22 — content bounds and tap behavior

- Non-compact banner touch bounds now use the non-compact banner width and height settings instead of compact dimensions.
- Banner hit area includes the full card height and the capsule-to-banner gap; the visible drawing host remains full-width and wrap-content.
- Tapping a notification banner invokes its notification content PendingIntent first, then falls back to launching WhatsApp / WhatsApp Business.
- Banner text, actions, and reply field now use OLED black text; muted lyric labels remain black with reduced alpha.
- Added visible lyric font-size and font-weight sliders wired to the existing persisted settings and renderer.
- App version bumped to 1.4.22 (versionCode 32).

Build note: Gradle 8.9 could not be downloaded in this environment (DNS/network unavailable), so compilation is not claimed as verified.
