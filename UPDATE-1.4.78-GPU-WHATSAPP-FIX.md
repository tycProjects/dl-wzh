# Update 1.4.78 — GPU Liquid Glass & WhatsApp re-alert fix

## Implemented in this source revision
- WhatsApp notifications that reuse the same Android notification key now trigger a new Island banner when the message text changes. Unchanged reposts continue to refresh silently. This fixes the common case where a chat was replied to/dismissed and a later incoming message from that same chat was suppressed as an already-alerted notification.
- Added `WA_REALERT` diagnostics for same-key notifications with changed message content.
- Replaced the single broad lens dome with three overlapping AGSL dome fields, coupled into one continuous card surface, with directional specular crowns, refraction displacement and a stronger bevel response.
- Applied the artwork Gaussian blur through Android hardware `RenderEffect` on the artwork-only layer. Text and controls remain outside that blurred layer.
- Lens and blur remain adjustable through their existing settings paths.
- Updated source version metadata to 1.4.78 / versionCode 71.

## Important implementation boundary
The glass is rendered as a GPU-backed Compose/Android `RenderEffect` layer. This revision does **not** perform a GPU readback into a separately cached opaque bitmap; doing that requires a dedicated hardware offscreen-surface/HardwareBuffer pipeline and additional device testing. The shader makes the interior of the card silhouette opaque where source pixels exist, while the Compose shape clip retains rounded edges. Do not interpret this source revision as proof of a separately materialized opaque bitmap.

## Verification
- Offline package/XML structural checks are run before packaging.
- A Gradle Kotlin compile could not be completed in this environment because the Gradle 8.9 distribution was not cached and network access to `services.gradle.org` was unavailable. Build success is therefore **not claimed**.
- Runtime GPU output and WhatsApp same-key repost behavior still require on-device verification on Android 15+ / Helio G99.
