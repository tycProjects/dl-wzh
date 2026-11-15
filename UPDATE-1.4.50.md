# Android Island 1.4.50

## Foreground color modes
- Adaptive: choose white or black foreground using available album/avatar artwork luminance.
- White: white text and UI foreground.
- Black: black text and UI foreground.
- Outline: preserve the existing black-filled, white-outlined text style.
- Foreground modes do not recolor the capsule, expanded panel, or WhatsApp banner surfaces.
- Legacy LIGHT/DARK/NATIVE preferences are migrated to the corresponding foreground choice.

## Known limitation
Wallpaper color sampling is not implemented in this revision. Adaptive uses the available album artwork or WhatsApp avatar analysis and falls back to its existing contrast behavior.
