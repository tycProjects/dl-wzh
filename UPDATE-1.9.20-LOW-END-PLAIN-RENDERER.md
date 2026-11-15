# 1.9.20 — Low-End plain renderer / GPU allocation reduction

- Low-End is a separate plain-rendering path, not a reduced premium-glass path.
- Expanded Music stays OLED black with artwork backdrop, glass body/edge, sweep, bloom, lens, blur, shader, and reflection layers bypassed. Album-cover thumbnail and all music/lyrics controls remain.
- WhatsApp keeps its small cached avatar bitmap with one fixed dark scrim. No FluidGlass premium stack, lens, blur, shader, gradient, reflection, or animated sheen is used.
- Replaced the old Low-End `StaticGlassBackdrop` overlay bake (half-resolution ImageBitmap plus multiple reflection strokes) with one direct artwork draw and a flat scrim. This avoids creating and retaining an additional full-surface GPU image.
- Low-End glass body is one flat dark fill; edge sheen is a no-op; expanded bloom is bypassed. This also covers call banners so they do not accidentally use premium glass layers.
- Normal mode remains unchanged: cached AGSL convex mirror/magnifier lens, normal glass body, edge sheen, and premium reflections remain available when Low-End is OFF.
- Retained the bounded one-entry artwork cache, compact RGB_565 artwork copy, reduced Low-End artwork dimensions, and cache trimming on entry to Low-End.
- Build verification must be performed with the project's Gradle wrapper and Android SDK; do not infer a successful Android build from source checks alone.
