# Android Island 1.4.32 — WhatsApp stack, dedup & continuous glass sweep

## Implemented
- WhatsApp notification deduplication uses a bounded `alertedNotificationKeys` set plus exact `lastProcessedText` per key. Identical reposts refresh queued notification metadata/actions silently and do not retrigger the banner/vibration. Changed text on the same key is treated as a new message.
- Notification actions only select `RemoteInput` entries that allow free-form input. Existing `Controls.sendReply()` delivers the reply through the notification action's `PendingIntent`.
- Expanded WhatsApp stack now uses a keyed `LazyColumn` and renders each active notification as its own interactive banner/card, including per-notification reply and mark-read actions.
- Main WhatsApp banner tap fires `contentIntent` first, then falls back to launching `com.whatsapp` (or WhatsApp Business when applicable).
- Removed the additional translucent white avatar veil from `WhatsAppBanner`; each card keeps one Liquid Glass surface and the host remains transparent.
- Global WhatsApp banner font slider is constrained to 10–20sp and applied to sender, message, count, actions, and reply input in compact and expanded banners.
- Glass Sweep now runs as one uninterrupted linear animation from start to finish. No midpoint pause or split animation remains; duration remains configurable.
- Version bumped to 1.4.32 (versionCode 42).

## Validation
- XML parsing and source-level structural checks are run before packaging.
- Gradle Kotlin compilation could not be completed in this environment because the wrapper distribution host (`services.gradle.org`) could not be resolved (DNS/network unavailable). Device behavior is not claimed as tested.
