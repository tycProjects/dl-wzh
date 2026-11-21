# Android Island 1.4.23

## WhatsApp indicator and banner lifetime
- Added a persisted banner display-duration setting (3–60 seconds, default 8 seconds).
- When the timer expires, the visible banner collapses to the compact capsule state while queued WhatsApp notifications remain in the notification queue.
- The WhatsApp side indicator remains visible beside the capsule and is clickable to open the retained message stack.
- The capsule anchor and dimensions are not changed by the expiry timer or indicator interaction.
- Added an upward exit animation before the banner collapses on swipe-up.
- Expanded WhatsApp stack continues to render each queued notification as its own card with its own available reply action.

## Lyrics
- Added a separate enable/disable control for glyph glow and its intensity.
- Kept moving lyric shine as an independent effect with its own enable/disable, speed, and intensity controls.
- Applied a soft green glyph shadow for visible letter-level glow; it is independent of the moving shine layer.

## Verification
- ZIP integrity is checked after packaging.
- Gradle compilation could not be run in this environment because the Gradle 8.9 distribution is not cached and network access to services.gradle.org is unavailable.
