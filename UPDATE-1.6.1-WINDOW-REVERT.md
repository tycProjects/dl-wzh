# Update 1.6.1 — Revert runtime window resizing

## Analysis (from the 1.6.0 diagnostics log)
- On every tap the drawing window grew from 720x60 to 720x1280 (DRAW_WINDOW fixed=true), then shrank
  again after the collapse, then grew again. Each change reallocates the window surface (about 3.7 MB),
  which showed as stutter before and during the expand.
- After the window became tall, the island appeared away from the top edge in the user's test. The
  root container is TopCenter, so the exact source is not confirmed by the log alone.

## Change
- The drawing window is created once at full height (80% of the screen) and is never resized while in use.
  The grow-on-touch and shrink-after-collapse logic is removed.
- Everything else from 1.6.0 stays: frost blur, baked lens, path-based edge sheen, shared WhatsApp icon,
  bounded liked tracks.

## Trade-off
- The full-height window costs about 8 MB of buffers at all times again (the 1.5.0 cost). Smooth motion
  is kept, and RAM is not reduced by the window in this version.

## Still to check on device
- Whether the island now opens at the top edge on tap. If it still appears in the middle, send a screenshot.
- If RAM matters more than smoothness on the target device, the next step is a smaller, fixed window height
  sized to the expanded content (not the whole screen), with the stack limited to that height.
