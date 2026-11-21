# Update 1.9.7 — Artwork shader reuse, RAM and morph motion

- Added a process-wide, single-entry cache for the half-resolution lensed artwork image. The AGSL shader is rendered once for an artwork/settings key and the cached image is reused when the island collapses and expands again. A new artwork or lens setting replaces the one-entry cache.
- Added a matching one-entry RuntimeShader/RenderEffect cache so the shader program/effect is not recreated for the same artwork and render geometry.
- Cache cleanup is connected to Android memory-pressure trimming; this deliberately releases the cached GPU/artwork result when the system asks the app to free memory.
- Hardware artwork instances now use a cheap identity fingerprint, avoiding repeated bitmap copies and pixel scans during recomposition when the player reuses the same hardware Bitmap object. New Bitmap instances still pass through the content hash to deduplicate equivalent artwork.
- Added direction-aware capsule morph motion: opening has a softer acceleration, closing settles more decisively. The regular iOS/Flow morph is critically damped to avoid progress overshoot; Jelly retains restrained overshoot.
- Reduced the expanded-panel keep-alive tail from 700 ms to 180 ms. The panel remains composed while morph progress is non-zero, then its extra resources can be released sooner.
- No feature modules or settings were removed.

## Build verification

Not verified in this environment: Gradle 8.9 wrapper download failed because `services.gradle.org` could not be resolved (`UnknownHostException`). Run `./gradlew :app:compileDebugKotlin` and a full debug/release build in an environment with the Gradle distribution and Android SDK available.
