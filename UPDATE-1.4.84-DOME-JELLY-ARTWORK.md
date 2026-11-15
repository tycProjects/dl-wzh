# Update 1.4.84 — Three-dome glass everywhere, black-free artwork opacity, jelly without cuts

- Artwork at 0% is now truly transparent. The lens shader previously forced an opaque fill
  (`max(original.a, coverage)`), which turned empty glass black. Lens output is now premultiplied
  and its alpha follows the artwork alpha.
- The artwork scrim (photo dimming) is scaled by artwork opacity, so 0% never darkens the card.
- New `Modifier.domeGlassOverlay()` draws the three-dome light field on the glass itself. It works
  on every card (expanded, WhatsApp banner, call banner), with or without artwork. Android 13+ uses
  an AGSL normal-lit height field (highlights on the lit flanks, shade on the far flanks, rim glint);
  older versions use three radial highlights.
- Lens refraction of the three domes was made stronger, with the same dome field as the overlay.
- Jelly (Opsi 3): the expanded panel now reserves enough vertical slack for the real overshoot, and
  the width overshoot is clamped to the scene width, so the sides are no longer cut flat.
- WhatsApp banner: the outer clip after graphicsLayer was cutting the stagger overshoot. The clip
  now lives on the artwork and glass layers, so the card travels and bounces as one piece.

Build status: source-level edits only. Gradle/Kotlin compilation and on-device animation tests still
need to be run. Visual tuning constants are in LiquidLens.kt (`uShift`, `DOME_OVERLAY_AGSL` gains).
