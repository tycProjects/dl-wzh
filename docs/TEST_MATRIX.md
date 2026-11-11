# Everline 0.1 — test matrix

| User scenario | Automated coverage in project | Actual execution status here |
| --- | --- | --- |
| A — Start a random life, progress +1, restore | Instrumentation test covers create / +1 / stored model. Process-kill/restart manual test still required. | **NOT RUN** (Android SDK/emulator unavailable) |
| B — Salary and expenses | Instrumentation asserts cash delta and journal, deterministic JVM settlement checks. | **JVM domain assertions PASS; Room test NOT RUN** |
| C — Edit parent degrees/occupation/salary and persist | Instrumentation checks full edit and audit; process restart manual test required. | **NOT RUN** |
| D — Living-parent/child/parent switching | Instrumentation checks switch back, same calendar, same balances, switch count. JVM eligibility assertions. | **JVM eligibility assertions PASS; Room test NOT RUN** |
| E — Inactive NPC progression | Instrumentation checks inactive relative account changes over 3 months. | **NOT RUN** |
| F — Generational continuity | Instrumentation checks new descendant, original relatives remain, degree persisted. | **NOT RUN** |
| G — Fully offline gameplay | Manifest has no INTERNET permission; offline airplane-mode device verification required. | **NOT RUN** |
| H — Edit/switch restart integrity | Backup test checks new slot import and relationship references; app process restart, corruption/recovery, import UX still require manual testing. | **NOT RUN** |
| Source-only Kotlin rules | `scripts/verify-domain.sh` executes 14 deterministic assertions. | **PASSED** via `kotlinc` and `java` |
| Data/repository source syntax | Kotlin compiler run with minimal temporary Room/serialization/Flow stubs. | **PASSED** as *source type-check only*, not Android compilation |
| Gradle sync / Compose UI / Room KSP | Android Studio Gradle build required. | **NOT RUN** |
| Genuine debug APK and device installation | Android SDK + Gradle distribution required. | **NOT PRODUCED** |

## How to verify the delivered source

1. From project root, `./scripts/verify-domain.sh` on a system with `kotlinc`.
2. In Android Studio, sync, then run `./gradlew :app:testDebugUnitTest`.
3. Connect Android 8.0+ emulator/device and run `./gradlew :app:connectedDebugAndroidTest`.
4. Launch UI and use scenarios A–H from the development prompt. Include force-stop and relaunch, all modes, repeated month processing, family birth/death, malformed backup import, and airplane mode.
5. Check `app/build/outputs/apk/debug/app-debug.apk` exists **only after a successful real build**.

Do not equate source files or UI buttons with verified runtime functionality. Passing the JVM domain smoke suite does not prove an Android APK is buildable.
