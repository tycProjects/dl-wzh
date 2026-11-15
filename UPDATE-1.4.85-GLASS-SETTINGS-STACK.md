# Update 1.4.85 — Glass controls, stack background, WhatsApp banner fixes

## Fixes
- Opening or replying to one WhatsApp chat no longer clears every WhatsApp card. Only that chat's
  card is removed (IslandRoot tap-to-open, horizontal dismiss, and reply send).
- Three-dome glass: the dome field is measured in units of the card's short side, so domes keep
  the same round shape on every card (banner, expanded, call). Three unequal hills merge into one
  liquid body instead of three equal circles on a line. A broad sheen lifts the upper body.
- Dome overlay now also draws on the stack surface, not on each card.
- Settings values are Compose state (GlassTuning). Changing a slider updates every glass surface
  immediately, including composables whose parameters did not change.
- Stack gap and inner padding follow their sliders; the measured stack height includes the padding,
  so the expanded panel and touch window match the content.

## Stack
- Stack background is drawn once for the whole stack: the avatar of the chat in focus, or the
  installed WhatsApp icon when the chat has no avatar (green disc if WhatsApp is not readable).
- Cards draw text only; they no longer carry their own artwork or dome overlay.

## New settings (Pengaturan → Latar album / avatar)
- Intensitas dome 3D (domeStrength, 0 = off)
- Kedalaman dome / relief (domeRelief)
- Kilau liquid / sheen (sheenStrength)
- Opasitas badan kaca (glassSurfaceOpacity, replaces the fixed 22%)
- Jarak antar kartu stack (stackGap, 0–24 dp)
- Padding dalam stack (stackInset, 0–16 dp)

## Known limits
- Source-level changes were not compiled or run on a device in this environment. Build with
  `./gradlew assembleRelease` and test on Android 13+ (AGSL shaders) and on an older device (fallback).
- Entrance stagger with overshoot can overlap neighbouring cards for a moment during the jelly motion.
