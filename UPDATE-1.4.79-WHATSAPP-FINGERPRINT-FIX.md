# Update 1.4.79 — WhatsApp message identity and package metadata

- WhatsApp notification deduplication now fingerprints the latest MessagingStyle message timestamps, sender names, and message bodies instead of comparing only EXTRA_TEXT.
- Identical consecutive messages can therefore alert again when WhatsApp supplies its message timestamp payload, while metadata-only notification refreshes remain silent.
- Existing active WhatsApp notifications are seeded into the fingerprint cache when the listener reconnects.
- If WhatsApp omits MessagingStyle messages, notification post time is used as a conservative fallback.
- Synchronized app version metadata: 1.4.79 / versionCode 72.
- Fixed the offline verifier's stale version expectations and diagnostic text.

## Verification boundary
- XML and offline source-package checks are run on this archive.
- Gradle/Kotlin compilation and device behavior still require the Android build environment and on-device testing; neither is claimed here.
