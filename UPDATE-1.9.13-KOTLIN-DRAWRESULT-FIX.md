# Update 1.9.13 — Kotlin DrawResult Fix

- Fixed the `drawWithCache` static Liquid Glass branches in `LiquidGlass.kt` and `LiquidLens.kt`.
- Both branches now return the `DrawResult` produced by `onDrawWithContent` directly, rather than returning `Unit`.
- Preserved the shader-free static substrate and four-sided reflection strokes for low-end mode.
- No animation curves, jelly behavior, capsule positioning, or notification gesture logic were changed in this fix.
