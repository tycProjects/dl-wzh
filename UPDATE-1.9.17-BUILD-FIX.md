# 1.9.17 — Kotlin DrawScope build fix

- Fixed the Compose `DrawScope` import in `LiquidGlass.kt`. The type belongs to `androidx.compose.ui.graphics.drawscope`, not `androidx.compose.ui.drawscope`.
- This resolves the root symbol error that caused the subsequent `size`, `drawRoundRect`, and arithmetic overload errors during Kotlin compilation.
- Kept the 1.9.16 static mirror-reflection drawing implementation and its rendering parameters unchanged.
- Incremented app version to 1.9.17 (114).

Build note: package structure checks are run locally. A full Gradle compile could not be completed in this environment because `services.gradle.org` could not be resolved to download Gradle 8.9.
