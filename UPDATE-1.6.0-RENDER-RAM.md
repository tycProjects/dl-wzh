# Update 1.6.0 — Cheaper rendering and less RAM

## Analysis
- Defaults: lens and frost are off by default (lens is forced to 0, frost is 0). The main cost in
  normal use was the edge sheen shader, which ran per pixel every frame on every glass surface.
- The drawing window was 80% of the screen tall at all times, which costs about 8 MB of buffers
  (720 x 1459 px, two buffers) even when the island is idle.
- The WhatsApp icon was decoded once per card.
- Liked tracks were stored without a limit.
- Already in place before this update: artwork cache bounded (3 entries, 512 px), avatars shrunk to
  480 px, lyrics cache bounded (40 entries), diagnostics recording off by default.

## Changes
- Edge sheen: now path strokes and one gradient per surface (no shader, paths built once per size).
  Same placement and intent. The falloff is stepped, so it is close to, not identical with, the
  previous shader.
- Frost (Gaussian slider): cheap blur. The artwork is shrunk to 1/8, blurred with three box passes and
  stretched back up. Computed once per artwork and slider step, off the main thread. No GPU blur per frame.
- Lens (Android 13+, when on): baked once per artwork and size at half resolution, then drawn as an image.
  The lens shader is no longer run every frame.
- Removed unused code: domeGlassOverlay and its shader, the lens modifier (replaced by the baked lens), and
  the edge sheen shader.
- Drawing window: compact (WRAP_CONTENT) while idle. It grows to full height on touch-down (before the state
  changes) or when a banner or the expanded panel is shown. It shrinks again 1.2 s after the island is
  compact, or 1.5 s after a touch that did not expand it.
- WhatsApp icon: decoded once and shared by all cards.
- Liked tracks: capped at 1000 entries (oldest removed first).

## Known limits / to check on device
- The edge sheen is not bit-identical to the 1.5.x shader. Compare with the reference screenshot.
- The crossfade layer (the artwork that is fading out) is drawn without frost and lens for the fade duration.
- Growing the window for a banner happens when the banner appears, which can still coincide with that
  morph. Check that it does not look like a jump.
- The wave loops (LiquidWave, mini bars) still run at 33 ms while music plays; they were left as they are
  because they are the visible animation.
- Not compiled or run on a device here. Check: expand/collapse, drag, call banner, WhatsApp stack, frost
  slider, lens slider (if enabled), and that touches outside the island still reach the apps below.
