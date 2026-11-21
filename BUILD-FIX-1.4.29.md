# Android Island 1.4.29 — Kotlin compile fix

- Fixed `WhatsAppBanner.kt` compile error at the graphics-layer block: removed the attempted assignment to the read-only `GraphicsLayerScope.shape` property.
- Rounded clipping remains owned by the existing outer `Modifier.clip(shape)` modifier; the graphics layer no longer applies a second rectangular clip.
- Version: 1.4.29 / versionCode 39.
