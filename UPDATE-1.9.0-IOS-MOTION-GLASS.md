# Update 1.9.0 — iOS-style motion + luxury liquid glass

## Motion
1. Open morph: the panel now starts as the same pure-black pill as the capsule (black skin) and the glass develops on top of it;
   it returns to black on close. The old capsule/glass cross-fade (see-through gap mid-morph) is gone. Capsule hand-over: first 22 % of the morph.
2. Content choreography is direction-aware: opening = glass develops at 8-50 %, closing = content gone by 40 % so the glass never shrinks around visible text.
3. Close spring is critically damped on every option (no undershoot, keeps velocity if re-opened mid-close). Open springs have less overshoot (Option 1/2).
4. Banner: grows out of the capsule (scale 0.80 -> 1 from the top centre + fade, group-composited) with a slightly springy settle; tuck exit 420 ms.
5. Glass sweep: ONE gradient shader built per size and translated per frame (was: a new shader every frame). Same look, much less GPU/CPU churn.
6. Artwork: late-arriving art fades in (was a pop); track change cross-fade uses the iOS ease (520 ms / 300 ms in low-power).
7. Press feedback: firmer jelly (damping 0.58, stiffness 520, less vertical squash); capsule press is a spring, not a 90 ms tween.
8. System "Remove animations" (animator scale 0) now also turns off the decorative cascade/bloom.
New tokens in IslandMotion.kt: `IosEase`, `smooth()`, `bannerEnterSpring()`, `bannerEnterT()`.

## Liquid glass (edgeSheenOverlay / liquidGlass / FluidGlass)
- Specular rim: sweep-gradient stroke, bright top-left and bottom-right, faint elsewhere (light catches two corners, it does not wrap evenly).
- Second inner rim (inset 1.9 dp) = glass thickness; top gloss dome; 1 px top hairline; faint inner glow; thin bottom caustic.
- Pillow vignette on the glass body (depth toward the ends), artwork saturation +22 % (iOS lifts the colour behind glass), stronger soft highlight.
- All drawn with paths/gradients, no new shader, no new bitmap; the lens bake and flat-glass tiers are unchanged.

## Cleanup
Removed dead code: `LiquidGlassSurface`, `morphNotchShape`, `outlineToPath`, two unused locals in `LiquidReveal`, duplicated KDoc in `AnimationPreset`.
versionCode 99 / versionName 1.9.0.

Not verified here: no Android SDK/Gradle in this environment, so nothing was compiled or run. Build once and check on a device.
Tune by eye: rim strength follows the existing "sheenStrength" setting; vibrance is `GlassVibrance` in FluidGlass.kt; skin timing is the two `smooth(...)` calls in IslandRoot.kt.
