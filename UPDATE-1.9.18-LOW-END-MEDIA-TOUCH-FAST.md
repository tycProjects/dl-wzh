# 1.9.18 — Low-End media surfaces and touch responsiveness

- Low-End expanded music now uses a pure black surface; album-art background treatment, glass body/edge, sweep, lens, blur, shader and bloom are bypassed. The small album-cover artwork and all playback/lyrics controls remain.
- Low-End WhatsApp banners keep the contact avatar (or WhatsApp fallback artwork) as a plain cropped bitmap background with a fixed dark scrim for legibility. No FluidGlassBackdrop, lens, blur, shader, edge sheen or animated reflection is invoked for that banner.
- The non-Low-End visual paths are unchanged.
- Fixed the static reflection helper placement so it no longer depends on the problematic DrawScope import in LiquidGlass.kt.
- Version: 1.9.18 (115).
