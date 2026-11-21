# Update 1.4.89 — Drips fall below the card

- Drip tongues now hang down from the bottom edge, outside the card, each ending in a bead with a glint.
  The rim on all edges and the thicker bottom band are unchanged.
- The overlay draws down to the drip reach (1.5 x "Panjang tetesan").
- The expanded panel no longer clips at its bottom (clip = false), so drips are visible. Glass surfaces
  still clip their own artwork and backdrop to the card shape.
- The overlay window gets extra room below the card for the drip reach, for expanded, stack, and banner.

Known limits:
- Not compiled or run on a device here. Test on Android 13+ (AGSL). Older Android uses a gradient band
  inside the card.
- Turning the panel's clip off means any content that overflows the panel is no longer cropped. Check
  the jelly expand animation.
