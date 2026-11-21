# Android Island 1.4.35

## Capsule lock and media-face preservation
- Keep the active media artwork/title as the compact capsule face while WhatsApp notifications are queued or the WhatsApp stack indicator is visible.
- Preserve the pending media snapshot when collapsing the WhatsApp banner or opening the WhatsApp stack, so returning to compact mode does not replace the music capsule with a WhatsApp avatar.
- Disable horizontal drag/track-swipe/dismiss on the compact capsule. The capsule stays anchored at its configured notch X/Y; tap, double-tap, long-press and vertical expand gestures remain available.
- WhatsApp stack indicator remains a separate control and does not replace the media capsule content.

## Validation
- Static source checks and ZIP integrity check performed.
- Gradle compilation/device animation test not performed in this environment.
