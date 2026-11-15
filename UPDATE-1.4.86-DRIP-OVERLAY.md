# Update 1.4.86 — Dripping liquid glass above the blur

- New `dripGlassOverlay()`: a thin liquid film runs along the lower band of every glass surface and
  tongues of liquid hang from it, each ending in a bead with a glint. It is drawn on the glass
  surface after the artwork blur, so it sits above the blur, not below it.
- Applied to every glass surface: expanded media, expanded WhatsApp stack, WhatsApp banner, and call banner.
- Settings (Latar album / avatar): "Efek netes (drip)" (strength, 0 = off) and "Panjang tetesan"
  (length, 0–28 dp).
- Drips stay inside the card's bottom band. They do not extend outside the card, because the overlay
  window has little room below the expanded panel and banners.

Known limits:
- Not compiled or run on a device in this environment. Test on Android 13+ (AGSL). Older Android
  gets a simple gradient band instead of drips.
- Text near the bottom edge can overlap the drips at high strength or long length.
