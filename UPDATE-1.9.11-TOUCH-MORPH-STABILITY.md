# Update 1.9.11 — Touch morph stability

- Keep the expanded transparent touch-window bounds throughout the EXPANDED → COMPACT close morph.
- Prevent subsequent draw-layout/display callbacks from replacing the close hold with a shorter debounce.
- Recalculate compact touch bounds only after a 760 ms close-settle window; opening still installs expanded bounds promptly.
- Add `TOUCH_BOUNDS_MORPH_HOLD` and remaining-hold diagnostics for on-device verification.
- Preserve Jelly spring, squash/stretch rendering, glass layers, artwork, lyrics, and media-session behavior.

Audio note: this source contains no AudioManager volume writes, audio-focus requests, or audio-ducking calls. Media-session polling reads metadata/playback state and repeat/like state only; it does not set output volume. The short supplied clip is about 1.6 seconds and cannot verify a two-second volume cycle.
