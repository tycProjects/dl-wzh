# Android Island 1.9.12 — Jelly Capsule Return + Static Low-End Glass

- Restores the 1.9.9 morph motion curves, including the Jelly close/rebound behavior.
- Keeps the newer panel lifecycle and touch-bounds hold to avoid the prior subtree flash and mid-morph hit-area resize.
- During the final part of closing, artwork and controls now scale/fade into the capsule instead of remaining visible until the card clips them.
- The dedicated Low-end / mid-range switch selects a shader-free Liquid Glass path: retained artwork bitmap, static translucent substrate, and solid highlights on all four edges.
- Static low-end glass skips RuntimeShader lens, RenderEffect blur, and animated/gradient backdrop processing. The regular full-quality mode remains unchanged.
