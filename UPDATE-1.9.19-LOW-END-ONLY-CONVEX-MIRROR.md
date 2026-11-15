# 1.9.19 — Low-End-only resource path and normal convex mirror lens

- The shader-free static/low-allocation rendering path is now controlled only by the explicit Low-End toggle. Low Power and Android Battery Saver no longer silently disable the normal glass lens.
- Low-End expanded music remains a plain black surface; WhatsApp keeps its avatar as a plain cropped bitmap without stacked glass effects.
- Enabling Low-End trims retained lens/RenderEffect caches while preserving the newest artwork entry for the WhatsApp avatar.
- Screen-off idle cache trimming is gated to Low-End; normal mode retains cached lens/artwork so reopening can reuse valid results instead of baking again.
- Normal mode retains the AGSL convex magnifier/reflection: the rim samples inward toward the centre to create a magnified mirrored edge, not just a bright outline.
- Compose owns and releases the per-composition GraphicsLayer when its artwork layer leaves composition; the reusable process cache remains bounded to one current lens result.


Superseded in 1.9.20: the Low-End static reflection overlay was removed in favour of a direct bitmap + flat scrim renderer. Normal-mode convex mirror rendering is unchanged.
