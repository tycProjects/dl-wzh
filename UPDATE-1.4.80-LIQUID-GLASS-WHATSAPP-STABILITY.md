# Update 1.4.80 — Liquid Glass and WhatsApp stack stability

- Strengthened the three-dome AGSL lens: broader raised domes, stronger refraction/dispersion, a wider bevel, and more directional specular highlights.
- Reworked the artwork opacity control to cover 0–100%. The artwork remains full-opacity while the material tint/fill becomes more substantial at 100%.
- WhatsApp source-notification removal no longer removes the Island-owned banner or clears its fingerprint. This preserves expanded cards after inline reply/read actions; cards remain dismissible from the Island UI.
- Replaced the expanded WhatsApp stack's eager scroll Column with a keyed LazyColumn and remembered list state to preserve item identity and scroll position as notifications update.
- Kept per-notification message fingerprints so reposts of unchanged content are silent while new MessagingStyle message timestamps/content still update the same chat entry.
- Bumped package metadata to 1.4.80 / versionCode 73.

## Verification boundary

Offline source/package checks are run for this archive. Gradle/Kotlin compilation and runtime behavior still require an Android build environment and on-device testing.
