# Update 1.4.93 — Edge sheen replaces drips and centre streak

- Drips (tetesan) are removed permanently: code, sliders, and settings.
- The centre streak is removed. The liquid sheen now hugs every inside edge of each glass surface,
  brightest at the top and left (light from the top-left).
- The bottom shine band is thicker, with twinkles along it.
- Dome overlays are no longer drawn, so the dome sliders ("Intensitas dome 3D", "Kedalaman dome") are
  removed from Settings. The dome code remains in LiquidLens.kt and can be restored if needed.
- The expanded panel is back to clip = true, because nothing is drawn below the card anymore.
- The overlay window no longer reserves space for drips.

Known limits:
- Not compiled or run on a device here. Test on Android 13+ (AGSL). Older Android gets a simple
  outline and bottom gradient.
