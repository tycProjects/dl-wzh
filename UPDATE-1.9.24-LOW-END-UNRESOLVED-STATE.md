# 1.9.24 — Three-state renderer resolution (closes the first-upgrade gap)

## Problem with 1.9.23

`DeviceProfile.init()` read a fast SharedPreferences flag synchronously, but a *missing* flag
(fresh install, or upgrading from a build that predates 1.9.23) was read back as `false` and
interpreted as a confirmed Normal preference. For a user upgrading from an older version with
Low-End enabled, the first launch after the upgrade would initialize `DeviceProfile` as Normal and
only correct itself once `SettingsRepository`'s async DataStore read completed and an overlay
happened to recompose. In that window, a notification could still trigger premium shader/backdrop
creation for a user who had saved Low-End — a real violation of frame-zero isolation, just on a
narrower (first-launch-after-upgrade) trigger than the original 1.9.23 bug.

## Fix: UNRESOLVED is a real third state, not "assume Normal"

`DeviceProfile` now has `RenderTier { UNRESOLVED, NORMAL, LOW_END }` instead of a single `tier`
int:

- **`tier`** and **`staticGlass`** are now *derived* read-only properties: `tier` reads `2` and
  `staticGlass` reads `true` for both `LOW_END` and `UNRESOLVED` — only confirmed `NORMAL` reads
  as Normal. Every existing call site (`FluidGlass.kt`, `ExpandedIsland.kt`, `WhatsAppBanner.kt`,
  `CallBanner.kt`, `LyricsRepository.kt`) already branches on "not Normal" vs "Normal", so folding
  `UNRESOLVED` into the same safe bucket required **no changes at any of those sites** — the
  existing isolation automatically extends to the unresolved window.
- **`lensEnabled`** is `true` only for confirmed `NORMAL`, so the RuntimeShader/RenderEffect lens
  path is unreachable while unresolved, exactly like Low-End.
- **`DeviceProfile.init(context)`**: `SettingsRepository.readLowEndFastFlag()` now returns
  `Boolean?` — `null` means "no entry has ever been written", distinct from a confirmed `false`.
  - Fast-flag entry exists → resolves synchronously to `NORMAL`/`LOW_END`, exactly the 1.9.23 fast
    path, unchanged.
  - No entry → stays `UNRESOLVED`. No arbitrary delay, no blocking DataStore read here.
- **Closing the gap, not just deferring it:** `SettingsRepository`'s `init` coroutine now calls
  `DeviceProfile.setLowEnd(loaded.lowEndMode)` the instant its first DataStore read completes —
  directly, not by waiting for `IslandRoot` to compose. This matters because the overlay only
  exists while a notification is visible; relying on `IslandRoot`'s `SideEffect` alone would leave
  `DeviceProfile` sitting `UNRESOLVED` indefinitely whenever no notification happens to be showing
  right after a resolve. The same direct call was added to `update()` (so toggling Low-End from
  Settings with no active notification takes effect immediately, not only once an overlay next
  composes) and `resetAll()`.
- **`IslandAccessibilityService.settingsFlow`** is now seeded from `DeviceProfile.staticGlass`
  (true for Low-End *or* unresolved) instead of `DeviceProfile.tier == 2`, so the very first
  overlay frame takes the safe path in both cases, not just confirmed Low-End.

## Fast-flag path is unchanged for existing 1.9.23+ installs

Anyone who has already run 1.9.23 has a fast-flag entry (written at first DataStore load, or on
every toggle since). For those installs `init()` still resolves synchronously in the exact same
way as 1.9.23 — no new asynchronous step, no behavior change, no regression to startup speed.

## Diagnostics added (Phase 7 of this request)

Additive, behind the existing `IslandDiagnostics` toggle (off by default):

- `RENDER_TIER_UNRESOLVED` — `DeviceProfile.init()`, no fast-flag entry found.
- `SAFE_RENDER_PATH_SELECTED` — same moment, explicit record that the safe path was chosen.
- `RENDER_TIER_RESOLVED` — `DeviceProfile.setLowEnd()`, only fired the one time a transition leaves
  `UNRESOLVED` for a confirmed state (distinct from the existing `RENDER_TIER_CHANGED`, which fires
  on every real transition including later toggles).
- `PREMIUM_RESOURCE_BLOCKED` — added next to the existing `LENS_SKIPPED`/`PREMIUM_SHADER_CREATION_SKIPPED`
  (RuntimeShader/RenderEffect site) and `LOW_END_BITMAP_PATH` (premium backdrop site), each now
  tagged with `reason=tier_unresolved` or `reason=low_end`/`unresolved` so a diagnostics session
  can tell the two cases apart.
- The two existing debug-only (`BuildConfig.DEBUG`) `check()` tripwires at the RuntimeShader and
  premium-backdrop creation sites were widened from `tier != 2` to `lensEnabled` /
  `!staticGlass`, so they now also fire if either site is ever reached while `UNRESOLVED` — not
  only while confirmed Low-End — and their failure messages print `renderTierState` so a
  stack trace states which of the three states let it slip through.

## Preserved unchanged

Normal Glass Haze constants, chromatic dispersion, lens geometry, artwork rendering, the Low-End
black backgrounds, WhatsApp avatar rendering, notification/music functionality, gestures and
animations — none of these were touched. Every change in this update is confined to
`DeviceProfile.kt`, `SettingsRepository.kt`, `IslandAccessibilityService.kt`'s `settingsFlow` seed,
and the diagnostics/assertions around the two premium-allocation sites already instrumented in
1.9.23's `FluidGlass.kt`.

## Build status

**Not built** — same environment limitation as 1.9.23 (no network to fetch the Gradle 8.9
distribution, no `ANDROID_HOME`). All four changed files were checked for brace/paren balance and
reviewed line-by-line; a real build (Android Studio, or a machine with SDK + network) is still
required before shipping. No on-device runtime diagnostic evidence is claimed here — please watch
for `RENDER_TIER_UNRESOLVED` → `RENDER_TIER_RESOLVED` in the diagnostics log on a real upgrade
install, and confirm no `PREMIUM_RESOURCE_BLOCKED`-adjacent `check()` ever fires, before relying on
this.
