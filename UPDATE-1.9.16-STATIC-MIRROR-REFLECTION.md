# 1.9.16 — Static Mirror Reflection

- Replaced isolated white edge strokes in low-end mode with a full-surface, static mirror-like reflection field.
- Reflection wraps all four sides and rounded corners, then softly reaches toward the centre through overlapping translucent rounded strokes.
- The same reflection helper is shared by the low-end backdrop, liquid glass substrate, and edge sheen so they do not fight each other visually.
- No RuntimeShader, Brush gradient, GPU blur, RenderEffect, or animation clock is used by this static reflection path.
- Artwork remains visible beneath the reflection; the reflection is drawn above it at low opacity.
- Preserves the existing morph, jelly, touch, and anti-flash changes.
