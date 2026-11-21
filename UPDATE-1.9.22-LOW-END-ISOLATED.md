# Update 1.9.22 — Isolated Low-End Renderer

- Low-End uses a separate static drawing path: no AGSL RuntimeShader, lens RenderEffect, blur RenderEffect, radial/linear/sweep glass gradients, bloom, lyric glow, or lyric shine.
- Music expanded surface remains pure black. WhatsApp keeps its contact avatar as a plain bounded bitmap background.
- Low-End draws the already-bounded activity bitmap directly instead of creating a second ArtBackdrop bitmap or inserting it into the artwork LRU.
- Low-End cache trimming clears the artwork LRU references and lens caches; idle trimming clears the LRU completely. Premium retains its own cached glass path.
- Media artwork is reduced to the active profile's source bound. Intermediate owned bitmaps are released after conversion; stale asynchronous artwork results are discarded and released.
- Switching Low-End/Premium asks the active media listener to rebuild the retained artwork for the selected memory budget. No active UI bitmap is forcibly recycled while a frame may still reference it.
- Build target: versionCode 118, versionName 1.9.22.
