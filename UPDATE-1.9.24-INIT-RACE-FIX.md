# 1.9.24 correction — SettingsRepository first-read initialization race

Version metadata is unchanged (1.9.24 / 120): this is a correction to the 1.9.24 source package.

## The race

`update()` read DataStore itself when `live.value == null`, but only called
`DeviceProfile.setLowEnd()` / wrote the fast flag when `lowEndMode` *changed*. If `update()` took the
mutex before the init coroutine and changed an unrelated setting, `live` was populated without
resolving anything; the init coroutine then saw `live.value != null` and skipped its own resolution.
`DeviceProfile` stayed `UNRESOLVED` (safe Low-End-equivalent rendering, so a Normal user was denied
premium rendering until some other path resolved it) and the fast flag was never backfilled.

## The fix (SettingsRepository.kt only, plus test wiring)

All first-read work now goes through one guard, `live.value == null`, evaluated under the existing
`Mutex`, and one helper, `resolveLowEnd()` (`DeviceProfile.setLowEnd` + fast-flag write):

- **init**: under the mutex, if `live == null` → `readAuthoritative()`, set `live`, `resolveLowEnd`.
- **update()**: `firstRead = live.value == null`; if so it performs `readAuthoritative()` itself and
  resolves after the transform: `if (firstRead || lowEndMode changed) resolveLowEnd(next.lowEndMode)`.
  Resolving once, to the post-transform value, means a first-ever Low-End toggle goes
  UNRESOLVED → final tier without passing through the other tier.
- **resetAll()**: unchanged behavior; it now uses the same `resolveLowEnd()`.

Because `live` only changes under the mutex, exactly one of init / update() / resetAll() performs
the first resolution; the others see `live != null` and skip. `DeviceProfile.init()`'s synchronous
fast-flag path and the UNRESOLVED safe-rendering behavior are untouched.

### Two consequential changes
1. The one-time defaults migration (`migrateDefaults()`, body unchanged) moved from "before the lock
   in init" into `readAuthoritative()`. Otherwise an `update()` that won the race would read
   DataStore *before* the migration and its debounced persist would later overwrite the migrated
   values while the migration flags stay set. It now runs inside the first authoritative read,
   under the mutex, whichever caller performs it. If `resetAll()` wins, migration is skipped this
   process — the same end state as any reset, which clears the migration flags.
2. **Build break found and fixed:** `SettingsRepository` declared two companion objects (the 1.9.23
   fast-flag one and the pre-existing `private companion object` holding `PERSIST_DEBOUNCE_MS`).
   Kotlin allows one per class, so 1.9.24 would not compile. `PERSIST_DEBOUNCE_MS` was merged into
   the single companion.

## Test

`app/src/test/.../SettingsRepositoryInitRaceTest.kt` (Robolectric, JVM unit test). The repository's
scope runs on a single-thread executor whose only worker is parked on a latch, so the init coroutine
is queued but cannot run: `update()` / `resetAll()` deterministically win the mutex. Cases:
unrelated update wins (Normal stored), unrelated update wins (Low-End stored), first update toggles
Low-End, valid fast flag survives the race, `resetAll()` wins and late init does not resurrect the
old value. Each releases the latch afterwards and asserts the late init changes nothing.

Test wiring: `SettingsRepository` takes an optional `scope` (default identical to before),
`Context.dataStore` is `internal` (tests seed/clear it), and `app/build.gradle.kts` gains
`testImplementation` junit 4.13.2 + robolectric 4.14.1 and `unitTests.isIncludeAndroidResources`.

## Not changed (noticed during the audit)
- A Low-End toggle writes the fast flag immediately but DataStore only after the 400 ms debounce. If
  the process dies inside that window, the next launch resolves from the flag and then reverts to the
  stale DataStore value on the first authoritative read (pre-existing; same in init before this change).
- A debounced persist already inside `dataStore.edit` when `resetAll()` runs is not cancelled by it
  and can write a stale snapshot after the clear.
- `resetAll()`'s `clear()` also removes the migration flags, so the next launch re-runs the defaults
  migration and overwrites the seven keys it touches (frost, white, edge lens, surface opacity, sweep
  duration, lyric size, expanded corner radius) with their default values.
- `tools/verify_package.py` still expects 1.9.20 and fails on the unmodified 1.9.24 tree.

## Build / verification status
**Not built and tests not run.** This environment has no Gradle distribution, Android SDK or network.
Verified only by static review (brace/paren balance, a single companion object, call sites of the
changed constructor). A real `./gradlew :app:testDebugUnitTest` and `:app:assembleDebug` are required;
Robolectric downloads its SDK 35 jar on first run.
