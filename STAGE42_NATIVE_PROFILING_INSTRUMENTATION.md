# Stage 42 — Native Migration Profiling Instrumentation

## Goal
Collect real-device timing evidence before adding Rust/JNI. Stage 41 showed that the active Liquid Glass blur is GPU-backed/cached and the legacy CPU blur helpers have no call sites. A Rust port is therefore gated on measured CPU hotspots.

## Source change
`app/src/main/java/com/android/island/island/FluidGlass.kt`

- Added a single balanced Android trace section: `AIS.artBackdrop`.
- Added opt-in `ART_BACKDROP_PROFILE` diagnostics for cache hits and cache misses.
- Cache-miss records break out source/output dimensions, pixel count, scaling time, `Bitmap.getPixels` time, luminance loop time, hash loop time, and total elapsed time.
- Timing uses `SystemClock.elapsedRealtimeNanos()` and is emitted only when the existing internal diagnostic recording switch is enabled.
- No per-frame instrumentation was added. No Rust/JNI/NDK dependency was added. Existing rendering and caching behavior is preserved.

## How to collect a useful baseline on a low-end phone

1. Install the Stage 42 debug or release build on the target phone.
2. Open Android Island settings and enable **Perekaman log** under **Diagnostik internal**.
3. Reproduce a repeatable workload: expand the island with media artwork, change tracks/artwork 5–10 times, collapse/expand a few times, then leave the screen idle.
4. Open **Diagnostik internal** and tap **Salin semua**. Keep the `ART_BACKDROP_PROFILE` entries and the device profile / resource snapshot entries.
5. For animation frame timing, capture a Perfetto/System Tracing trace while repeating the same expand/collapse sequence. Include FrameTimeline, sched, freq, gfx, view, and am categories when available. The app trace section `AIS.artBackdrop` can be correlated with CPU work; Compose/GPU frame timing must be read from FrameTimeline rather than inferred from this CPU timer.
6. Repeat once with the same workload after the device cools. Avoid comparing traces taken under different thermal states or refresh rates.

## Decision gate for Rust
Do not port the pixel loop merely because it is a loop. The backdrop is bounded by `DeviceProfile.artMaxPx` (up to 320 px longest edge), cached, and processed off the main thread. Port only if repeatable device traces show the luminance/hash/pixel stage is a material CPU bottleneck. If the bottleneck is GPU composition, JNI/Rust is not the right fix.

If a Rust candidate is justified, implement it behind a Kotlin interface, keep the Kotlin implementation as fallback, compare identical inputs and outputs, and measure CPU, allocations/native heap, p95/p99 frame time, APK size, cold start, and battery before accepting it.

## Validation status

- Source inspection: completed.
- Instrumentation is opt-in and not called per frame: confirmed by code path.
- Rust/NDK integration: intentionally not added pending measurements.
- Unit tests / Gradle build: not verified in this environment because Gradle 8.9 distribution is not locally installed and external download may be unavailable.
- Physical-device profiling: pending user-provided low-end-device diagnostics and Perfetto trace.
