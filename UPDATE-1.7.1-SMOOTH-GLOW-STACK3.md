# Update 1.7.1 — Smooth glow, light blur, 3-card stack with pile, smoother motion

1. Glow (expanded music / cards): the rim glow was 6 stacked rectangles; now ONE blurred stroke baked once into a
   half-resolution bitmap and drawn as a single image (LiquidLens.kt `bakeRimGlow`): no steps, no layers. Open bloom and the light gradients use eased multi-stop falloff
   (LiquidWave.kt, FluidGlass.kt `easedStops`). Artwork is drawn with FilterQuality.Medium.
2. Stack: at most 3 newest WhatsApp cards in full; older ones are piled under the last card with a
   "+N (names)" strip. Tap the pile to show all (scrolls), "Tampilkan lebih sedikit" to fold back.
3. Blur: sigma = amount * 12 art px (4 % = ~0.5 px, almost none). Downscale only 1x/2x/4x (was always 1/8),
   3-box Gaussian approximation, 50 slider steps (was 20).
4/5. Motion: lower base stiffness and higher damping, calmer swipe-back springs, softer dismiss spring,
   list items animate placement/fade (animateItem), stack height animates, wave advances per frame.
6. Music no longer pauses when a WhatsApp message arrives (IslandBus.kt: removed the pause call in the
   WhatsApp branch). Only an incoming call (`pushCall`) pauses playback.
7. New defaults (from the user's own settings screenshots), in SettingsRepository.kt `read()`, the
   IslandSettings data class and GlassConfig: glass sweep ON (70 %, 1.0 s), animation Option 3 Jelly /
   Spring / Elasticity HIGH / speed 0.5x, capsule profile iPhone, expanded min height Auto, expanded
   offset Y 0, artwork opacity 80 %, artwork blur 20 %, liquid sheen 85 %, hide status icons ON.
   Defaults apply on a fresh install or "reset all"; values already saved on a device are kept.
8. Battery/RAM audit: removed the unused WAKE_LOCK permission (no wake lock is ever taken); wave tick kept at
   ~30 Hz; baseline profile generator also scrolls the settings screen. Everything else suggested (30 Hz
   tick loops that wait for screen-on, LRU art cache of 3 at <=512 px, trim on low memory, Default
   dispatcher for bitmap/palette work, distinctUntilChanged on settings) was already in the code.
