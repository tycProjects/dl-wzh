# Android Island 1.4.40 — Performance audit & changes

## Implemented
- `LiquidGlass.kt`: replace stacked background/drawBehind gradients with one size/config-cached diagonal gradient fill; keep the edge stroke for the glass rim. Removed redundant nested clipping/layering in `LiquidGlassSurface`.
- `WhatsAppBanner.kt`, `ExpandedIsland.kt`: remove full-surface `Modifier.blur()` from album/avatar art. Keep artwork, opacity, glass tint and edge highlights; this avoids the extra blur render target.
- `ExpandedIsland.kt`: use `derivedStateOf` for lyric text so lyric text consumers update when the lyric line changes rather than on every playback position update. The playback clock still updates for the progress indicator.
- `LyricsRepository.kt`: cap a cached lyric response at 600 lines in addition to the existing 40-entry LRU cache.
- `app/build.gradle.kts`: enable R8 minification and resource shrinking for release.
- `proguard-rules.pro`: remove the previous blanket `-keep class com.android.island.** { *; }` rule that prevented meaningful shrinking; add narrow runtime/Parcelable keep rules.

## Existing protections confirmed
- Accessibility service already has `canRetrieveWindowContent=false`; it does not retrieve the global node tree.
- `IslandAccessibilityService.onDestroy()` cancels its CoroutineScope and disposes the overlay.
- Accessibility config no longer subscribes to window events because `onAccessibilityEvent()` is empty; `canRetrieveWindowContent` remains false.
- `WhatsAppNotificationListener.onListenerDisconnected()` now cancels any in-flight lyric/media update job and clears its pending track key.
- `WhatsAppNotificationListener` cancels its jobs, unregisters MediaController and MediaSession callbacks, clears caches and cancels its scope in `onDestroy()`; it also unregisters on listener disconnect.
- Settings flow already uses `distinctUntilChanged()`.
- Lyrics cache already uses access-order LRU with a 40-entry bound; the new line cap bounds large values too.

## Still requires device profiling / follow-up
- No measured FPS, frame-time, heap, GC, or battery data is available in this source audit. Do not interpret these source changes as proof of 60/120 FPS or near-zero idle drain.
- `MiniWaveform` still uses a continuous 48ms loop while media is playing; consider pausing it when the compact UI is not visible or replacing it with a static indicator on devices where frame-time traces show it is costly.
- `MediaExpanded` playback position polling still updates at 100ms while playing; its progress UI therefore has a deliberate update cost while expanded. Profile before changing cadence because this affects visible progress/lyrics synchronization.
- `IslandAccessibilityService` observes state and settings; verify `applyLayout()` avoids redundant WindowManager updates. `onAccessibilityEvent()` is empty, but the service config currently subscribes to window events. Removing event types entirely should be tested against OEM overlay lifecycle before shipping.
- `SettingsRepository.update()` persists a full settings snapshot per edit. Ensure UI sliders are not writing to DataStore on every drag frame; persist on drag-stop/debounced commit.
- Baseline Profiles are not wired into this project yet. See implementation notes below.

## Baseline Profile setup (recommended next build step)
1. In Android Studio, use **File → New → New Module → Baseline Profile Generator**. The template creates the matching `:baselineprofile` module and plugin setup for the installed Android Studio/AGP; this is safer than guessing a plugin version by hand.
2. Keep the app module's `release` build minified, and use the template's non-minified profile-generation variant. Add `androidx.profileinstaller:profileinstaller:1.4.1` to app dependencies when local/sideloaded installation is part of the workflow.
3. Extend `BaselineProfileGenerator` with deterministic journeys: launch `com.android.island`, open Home settings, activate the overlay, expand a media capsule, collapse it, and scroll the WhatsApp stack. UI automation must arrange test notification/media fixtures; do not rely on a real personal WhatsApp account.
4. Generate with `./gradlew :app:generateBaselineProfile` (or the task created by the template) on a connected physical device or supported emulator. Commit the generated profile in the generated source-set path; wire `baselineProfile(project(":baselineprofile"))` in the app module as created by the template.
5. Compare Macrobenchmark `StartupTimingMetric`, `FrameTimingMetric`, and `FrameOverrun` on the same device/build type before/after. Baseline Profiles improve startup/hot code paths; they do not reduce idle service wakeups or guarantee 120 FPS.

## R8 rollout safety
- Build a release APK and test: first launch, notification listener binding, accessibility overlay, Shizuku permissions, RemoteInput reply, media controls, lyrics parsing, and settings restore.
- Inspect `mapping.txt` and `usage.txt`; only add keep rules for classes proven to require reflection/serialization. Avoid package-wide keep rules.
