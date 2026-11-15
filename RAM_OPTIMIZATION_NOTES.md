# RAM optimization notes

## Historical balanced-profile work (1.9.18)

The following bullets document the earlier balanced profile and are retained as implementation history. In 1.9.20, the explicit Low-End toggle alone selects the separate plain-rendering path; the old RAM & battery switch no longer silently changes the visual renderer.

## Historical changes
- Connected the existing **Mode hemat RAM & baterai** switch to the actual `DeviceProfile` and glass rendering efficiency tier. The separate low-end switch remains supported; either switch selects the efficient profile.
- Starts with the balanced profile while saved preferences are loading; the user's saved setting is applied by the overlay afterward.
- Reduced retained artwork dimensions in the efficient profile: backdrop longest edge 192 px and track artwork longest edge 224 px.
- Reduced decorative ambient update cadence in the efficient profile without changing core interaction animation timing or the lyric/progress clock beyond its existing efficient tier.
- Wired the existing lens divisor into the lens effect so the efficient profile uses half-resolution lens input.
- Fixed artwork shrinking for hardware-backed bitmaps and portrait images. Hardware artwork is copied into a bounded software bitmap rather than bypassing the size cap.
- Recycles intermediate artwork copies after the compact RGB_565 cache bitmap is created, and reuses cached duplicate artwork without retaining an unnecessary temporary bitmap.
- Kept the artwork cache bounded to one entry and preserved the existing notification, media, WhatsApp, lyric, and Liquid Glass feature paths.

## Build verification status
A Gradle compile was attempted with `./gradlew :app:compileDebugKotlin --no-daemon`, but this environment could not resolve `services.gradle.org` (`UnknownHostException`) to download the Gradle 8.9 wrapper distribution. No Android SDK was configured in this environment either. Therefore, a complete Android Gradle build could not be run here; this package must not be described as build-verified. The modified Kotlin files were inspected and a compiler syntax pass was attempted, but standalone compilation lacks the Android/AndroidX dependencies.


## Low-End renderer contract (1.9.20)

The older release notes were reviewed before this change: 1.6.0 documents avoiding per-frame GPU effects and caching the lens once; 1.7.0 documents keeping the drawing window compact while idle, using a local playback clock, and trimming old artwork under memory pressure; 1.9.7 documents a one-entry lens/effect cache and a short 180 ms expanded-panel keep-alive; 1.9.18 defines the exact Low-End media presentation (black expanded music and plain WhatsApp avatar + fixed scrim).

The explicit Low-End toggle now follows a stricter rule than the old static-reflection implementation: it does not create AGSL RuntimeShader/RenderEffect objects, lens or overlay ImageBitmaps, reflection strokes, gradient brushes, glass sweep/bloom, lyric glow, or lyric shine. Expanded Music is black with the small cover and controls; WhatsApp retains its compact cached avatar with one dark scrim. Call banners use a plain image/scrim and flat fill. Playback, lyrics text, notification actions, and core open/close/jelly motion remain functional.

The normal-mode path remains separate and keeps the cached convex mirror/magnifier lens and premium glass effects. The artwork cache remains bounded to one compact RGB_565 entry; Low-End dimensions are 96 px for the backdrop and 112 px for source artwork.
