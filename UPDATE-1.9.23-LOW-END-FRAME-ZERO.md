# 1.9.23 — Low-End frame-zero isolation

## Problem

Premium Liquid Glass gating (`DeviceProfile.staticGlass` / `tier` / `lensEnabled`) was already
implemented correctly everywhere it's consumed: `FluidGlass.kt`, `ExpandedIsland.kt`,
`WhatsAppBanner.kt`, `CallBanner.kt` all skip the RuntimeShader/RenderEffect/backdrop paths when
`DeviceProfile.staticGlass` is true. The defect was entirely in *when* that state became correct:

1. `DeviceProfile.init()` hardcoded `tier = 0` (Normal) on every process start, regardless of the
   user's saved Low-End choice.
2. The real choice lives in `IslandSettings.lowEndMode`, loaded asynchronously from a
   `Preferences DataStore` (`SettingsRepository`). That load is a suspend/IO read and is not done
   by the time the accessibility service can create its first overlay `ComposeView`.
3. `IslandAccessibilityService.settingsFlow` was seeded with a default `IslandSettings()`
   (`lowEndMode = false`) for exactly that window.
4. `IslandRoot`'s `SideEffect`/`LaunchedEffect` call `DeviceProfile.setLowEnd(settings.lowEndMode)`
   unconditionally on every composition — including the first one. So even a correctly
   Low-End-initialized `DeviceProfile` would have been immediately overwritten back to Normal by
   the first frame's stale default settings object, re-arming premium shader/backdrop creation for
   a user who had saved Low-End, until the DataStore flow caught up a few dozen milliseconds later.

This is a real race, not a cosmetic one: a WhatsApp message or call banner that arrives in that
window (most likely right after boot, when the accessibility service reconnects) could initialize
a RuntimeShader and premium backdrop bitmap it would never otherwise have created.

## Fix

- **Fast flag.** `SettingsRepository` now mirrors only `lowEndMode` into a tiny one-entry
  `SharedPreferences` file (`island_fast_flags`), written synchronously (`apply()`, not the
  debounced DataStore persist) the moment the user toggles it, and backfilled once after the first
  real DataStore read completes (so an upgrade from a build predating this flag self-heals on its
  next launch without the user having to retoggle).
- **`DeviceProfile.init(context)`** now reads that flag synchronously and resolves `tier`/
  `staticGlass` to the user's actual saved choice before returning, instead of hardcoding Normal.
  This runs in `IslandApplication.onCreate()`, before any Activity, Service, or Composable exists.
- **`IslandAccessibilityService.settingsFlow`** is now seeded from `DeviceProfile.tier` (already
  correctly resolved by the time the service exists) instead of `IslandSettings()`'s default. This
  closes the remaining gap: `IslandRoot`'s first-composition `SideEffect` now re-applies the same
  value it already had, instead of overwriting it with a stale default.
- No blocking disk I/O was added to the main thread beyond what `SharedPreferences` already does on
  first access for any app (the same pattern `LikedTracks` already uses in this codebase); the
  DataStore flow remains the single source of truth and full settings model — the fast flag is
  strictly a narrow, synchronous mirror of one boolean used only to resolve the renderer tier
  before first paint.
- `DeviceProfile.setLowEnd()` is now a no-op when the tier isn't actually changing, and logs
  `RENDER_TIER_CHANGED` / `LOW_END_RENDER_PATH_SELECTED` / `NORMAL_RENDER_PATH_SELECTED` when it
  does.

## Diagnostics added (Phase 8)

All additive, all behind the existing `IslandDiagnostics` enabled-flag (off by default, single
volatile read when off — no production log volume change):

- `RENDER_TIER_INITIALIZED` — `DeviceProfile.init()`, records the resolved tier.
- `RENDER_TIER_CHANGED`, `LOW_END_RENDER_PATH_SELECTED`, `NORMAL_RENDER_PATH_SELECTED` —
  `DeviceProfile.setLowEnd()`, only on an actual change.
- `PREMIUM_SHADER_CREATION_SKIPPED` — alongside the existing `LENS_SKIPPED`, when the lens
  RuntimeShader/RenderEffect path is skipped for `tier == 2`.
- `LOW_END_BITMAP_PATH` — `rememberArtBackdrop`'s Low-End branch (direct source bitmap, no
  backdrop copy, no LRU entry).
- `PREMIUM_CACHE_RELEASED` — `trimArtCaches()`, when entering Low-End clears the artwork LRU and
  the cached lens effect/bitmap.

Debug-only (`BuildConfig.DEBUG`) `check()` tripwires were added immediately before the two most
expensive premium allocation sites (`cachedLensEffect` — the `RuntimeShader`/`RenderEffect`
creation — and `buildArtBackdrop` — the premium backdrop bitmap) that throw if either is ever
reached while `DeviceProfile.tier == 2`. These only run in debug builds and only fire if isolation
actually breaks; they add no release-build cost.

## What was not changed

- Normal Glass Haze constants, shader code, bitmap sizing, and every existing Low-End visual
  branch (expanded music black background, WhatsApp/Call banner flat background, gradients,
  reflections) — all were already correctly gated on `DeviceProfile.staticGlass`/`tier` prior to
  this update and did not need modification.
- Bitmap lifecycle (Phase 5): already RGB_565 for opaque art, single-entry LRU
  (`DeviceProfile.artCacheSize == 1`), bounded dimensions per tier (`artMaxPx`, `sourceArtMaxPx`).
  No changes made here; none were required by the stated problem.

## Build status

**Not built.** This container has no network access (Gradle 8.9 cannot be fetched from
`services.gradle.org`) and no `ANDROID_HOME`/Android SDK configured, so `./gradlew assembleDebug`
could not be run here, matching the same limitation recorded in the prior
`UPDATE-LOW-END-MODE-ISOLATION.md`. All four changed files were checked for brace/paren balance and
reviewed line-by-line against their surrounding code; please run a real build (Android Studio or a
machine with network + SDK access) before shipping, and watch the debug-build `check()` tripwires
above during a Low-End test session — an exception there means isolation regressed, which would be
real evidence of a problem, not a guess.
