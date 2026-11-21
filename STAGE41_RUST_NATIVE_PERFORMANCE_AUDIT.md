# Stage 41 — Rust / Native Performance Audit

## Scope
Audit the current Android Island source before introducing Rust, JNI, or Android NDK. The objective is lower CPU time, RAM pressure, battery use, and frame-time variance without destabilizing Android services or rendering.

## Findings from the supplied Stage 40 source

### 1. Liquid-glass rendering
`app/src/main/java/com/android/island/island/FluidGlass.kt`

- The visible frost path builds an Android/Compose `RenderEffect` via `buildBlurEffect(...)`; it is not a per-frame CPU pixel blur loop.
- The lens path is guarded by device profile and Android API level, then baked/cached by artwork and surface settings. `LensBitmapCache` retains one result.
- Artwork preparation is bounded by `ART_MAX_PX`, and hardware bitmaps are drawn into a bounded software bitmap rather than copied at full source resolution.
- `sourceFingerprint()` samples a fixed 4 x 4 grid for software bitmaps. This is a small, bounded operation and is not a current Rust candidate.
- `gaussianBoxRadii()` and `blurAxis()` are present but have no call sites in the supplied source. They are dormant legacy helpers, not active frame-time costs.

### 2. Animation
The current visual animation and composition are driven by Jetpack Compose and Android graphics APIs. Moving animation state, lifecycle, or frame scheduling into Rust would add JNI crossings and duplicate Android framework responsibilities. Keep these in Kotlin/Compose unless profiling identifies a specific pure computation that dominates frame time.

### 3. Native toolchain availability
The project currently has no Rust crate, JNI bridge, CMake configuration, NDK build integration, or Rust Gradle integration. The available environment did not expose a verified `cargo`/`rustc` toolchain or installed Android NDK. Therefore no native binary was generated and no Rust integration can honestly be marked as built or tested in this stage.

## Stage 41 decision

No production Kotlin code is migrated to Rust in this audit stage. This is intentional: the inspected high-cost visual effects are already GPU-backed/cached, while the obvious CPU blur helpers are unused. Introducing Rust now would increase APK/build complexity without evidence of a performance win.

## Candidates for a later measured Rust module

Only consider Rust if profiling on representative low-end devices shows a repeatable CPU bottleneck in one of these isolated, pure-compute tasks:

1. Large-buffer pixel transforms or image analysis that cannot be moved to Android GPU APIs.
2. Repeated image/geometry calculations with enough work per JNI call to amortize bridge overhead.
3. A deterministic, allocation-bounded visual-analysis kernel used by the agent.

Do not move Android UI, Compose state, AccessibilityService, notification services, lifecycle, networking, or Android window management into Rust.

## Required acceptance measurements before migration

Record a Kotlin baseline and compare a Rust implementation on the same device, input, build type, and thermal conditions:

- CPU time per operation and total CPU utilization.
- p50 / p95 / p99 frame time and missed-frame count during animation.
- Java/Kotlin heap, native heap, and peak temporary allocation.
- Battery/current impact over a repeatable workload.
- APK/AAB size and cold-start impact.
- JNI call count and time spent crossing JNI.

A Rust port is accepted only if it improves the measured bottleneck without regressing frame time, memory, battery, startup, or reliability. Keep a Kotlin fallback until native compatibility is proven across supported ABIs and API 26+.

## Build and test status

- Source inspection: completed.
- Active blur call-site inspection: completed; no calls to `blurAxis()` or `gaussianBoxRadii()` found.
- Gradle unit tests: blocked. `./gradlew :app:testDebugUnitTest --offline --no-daemon` attempted to download Gradle 8.9 and failed with `UnknownHostException: services.gradle.org`.
- Rust/NDK build: not run; toolchain availability was not verified.
- Device profiling: not run; no physical-device profiler session was available.

## Next stage entry criteria

Before writing native code, obtain a profiling trace from a low-end target device showing the exact hot method and its share of frame/CPU time. Then isolate only that pure-compute method behind a Kotlin interface, implement a benchmarked Rust alternative, and retain the Kotlin fallback.
