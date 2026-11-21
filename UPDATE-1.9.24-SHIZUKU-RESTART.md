# 1.9.24 — Shizuku hard restart on mode switch (and on "Restart Island")

## Behavior
When the Low-End / Normal mode changes (Mode Low-End switch, or "Terapkan rekomendasi"/reset when it
flips the mode), and **both** hold:
- accessibility was already enabled before the change, and
- Shizuku is READY,

the app is restarted: `am force-stop`, then the accessibility permission that force-stop revokes is
re-granted automatically, then MainActivity is reopened. Otherwise nothing is restarted and the mode
applies live exactly as before. The existing **Restart Island** row behaves the same way when those
conditions hold; otherwise it falls back to the previous soft off/on restart.

## How it works
Force-stop kills our own process, so nothing inside it can re-grant accessibility. `ShizukuRestart`
starts ONE detached shell script through Shizuku (`Shizuku.newProcess`, private since API 13, called by
reflection; kept by a rule in proguard-rules.pro). The script runs in the Shizuku server's uid:
1. `am force-stop --user N <pkg>`
2. waits (max ~6 s) for the system to drop the service from `enabled_accessibility_services`
3. appends our service (keeping every other enabled service). If it was somehow still listed, toggles
   `accessibility_enabled` 0→1 to force a rebind
4. `accessibility_enabled 1`, then `am start` MainActivity.
The script is passed as `$0` (no quoting problems) and backgrounded, in its own session when `setsid`
exists, so it outlives the short-lived outer shell that Shizuku destroys when our process dies.

Before launching it, `SettingsRepository.flush()` writes the latest snapshot to DataStore and commits
(not apply()) the fast flag, under the same mutex — otherwise the new mode could be lost to the 400 ms
persist debounce or the async SharedPreferences write. After the restart `DeviceProfile.init()` therefore
resolves the new tier from the fast flag before the first frame.

## Files
SettingsRepository.kt (`flush()`, `writeLowEndFastFlag(sync)`), system/ShizukuRestart.kt (new),
ui/MainActivity.kt, ui/screens/HomeScreen.kt (two subtitles), proguard-rules.pro,
test: `flush_persistsModeAndFastFlagWithoutWaitingForDebounce`.

## Verification actually performed
- The generated shell script was extracted from the Kotlin source and run with stubbed `am`/`settings`
  in dash: other services preserved, ours appended after force-stop, empty-list case, short-form entry,
  and "never removed" toggle path all behaved as intended; detached wrapper returns immediately.
- **Not verified:** Gradle build, unit tests, and everything on a real device — Shizuku's
  `newProcess` reflection under R8, Android's own `sh`/`settings` behavior and timing, whether
  force-stop really drops the service on your ROM, and the notification listener reconnecting after the
  restart. Please test on the target device (Shizuku running, accessibility on) in a release build.
