# Normal mode: luxury liquid glass + haze (Low-End untouched)

Scope: Normal renderer only. No line of the Low-End path (`DeviceProfile.staticGlass`, `StaticGlassBackdrop`,
`liquidGlass` static branch, `edgeSheenOverlay` no-op) was changed. Version numbers were not bumped.

## Root-cause fix (found after the first delivery, from an on-device screenshot)
`DeviceProfile.lensDivisor` was 1 in Normal mode, but `LiveLensArt` always records the lens layer at HALF size.
The AGSL rounded-rect was therefore built for a card twice the size of its layer, so the whole rim (bevel, refraction,
and the new dispersion/haze) rendered only on the TOP and LEFT edges; bottom and right stayed flat. `lensDivisor` is now 2
(only the Normal lens path reads it; Low-End skips the lens). This visibly changes the existing lens: the bevel,
highlight and shade now go all the way around the card.

## What changed
1. **Rim dispersion** (LiquidLens.kt, AGSL): red and blue bend 9 % (x lens strength) less / more than green, each with
   the same 3-tap rim softening as green. Thin prism fringe on the bevel, like iOS 26. Baked once with the lens, so no
   per-frame cost. With dispersion = 0 the output equals the previous shader.
2. **Haze rim** (LiquidLens.kt, AGSL): the lens now thins the artwork alpha toward the rim (`uHaze`, 50 % at the very
   edge, back to 100 % at ~45 % of the bevel band). The screen behind shows through the bevel, where real glass
   shows refraction. Premultiplied alpha is preserved; the highlight keeps full strength. Also baked once.
3. **Haze veil** (FluidGlass.kt): a smoky `#14161A` layer (30 %) UNDER the artwork. Whatever shows through the artwork's
   translucency is dimmed and loses contrast first, so it reads as fog rather than a sharp ghost of the app behind.
   Scaled by artwork opacity like the scrim: artwork opacity 0 % draws nothing.
4. **Vibrancy** 1.22 -> 1.35 saturation on the artwork colour matrix (Kyant's `vibrancy()` uses 1.5 on a blurred wallpaper).
5. `FluidGlassBackdrop(haze = 0f)` is a new default-0 parameter. Only the expanded music card passes `haze = 1f`
   (ExpandedIsland.kt). WhatsApp / call banners keep an opaque rim and no veil, so the stacked-card overlap look is unchanged;
   they do pick up the dispersion and the vibrancy bump because they share the lens.

## Why translucency and not a real blur of what is behind
Android's window blur (`setBackgroundBlurRadius`, `FLAG_BLUR_BEHIND`) acts per window / whole screen, not per card shape,
and the island window is a fixed lane much taller than the card. Compose-level backdrop libraries (Haze, Kyant0
AndroidLiquidGlass) only blur content inside the same window. So the "see-through" is built from alpha + veil.

## References reviewed
- Kyant0/AndroidLiquidGlass: blur -> lens order, `vibrancy()` (saturation 1.5), chromatic aberration inside `lens`.
- chrisbanes/haze: tint + fallback scrim, cheap mask vs costly progressive blur (no new dependency was added).
- AOSP "Window blurs" guide: blur scope and the system disabling blur (battery saver), the reason for not using it here.

## Tune by eye (device)
`RIM_HAZE` and `DISPERSION` in LiquidLens.kt, `HAZE_VEIL` and `GlassVibrance` in FluidGlass.kt, `haze = 1f` in ExpandedIsland.kt.

## Verified / not verified
- Verified: numpy port of the old and new shader math (haze 0 + dispersion 0 is bit-identical; rgb <= alpha everywhere;
  centre stays 100 % opaque; fringe only near the rim); brace/paren balance; AGSL uniforms declared == set.
- NOT verified: Kotlin compile, AGSL compile on a real GPU, on-device look and frame time (no Android SDK / Gradle offline here).
- `tools/verify_package.py` already fails 7 checks on the untouched package (it still expects versionName 1.9.20).
