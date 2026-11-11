# EVERLINE — Native Android Life Simulator

**Temporary test build / source prototype 0.1.0**  
**Application ID:** `com.everline.lifesim`  
**Stack:** Kotlin, Jetpack Compose / Material 3, Room, Coroutines, Kotlin Serialization, Gradle Kotlin DSL  
**Android:** min API 26 · compile/target API 35 · portrait-first, adaptive Compose layouts  
**Visuals:** original midnight-blue / gold theme and original vector app icon.

## What this delivers

An editable native Android Studio source project containing a functioning data-backed *implementation* of the first prototype, not an HTML wrapper or a screenshot mockup. Android compilation has **not** been performed in the source-authoring environment because the Android SDK and Gradle distribution are unavailable. It is therefore **not yet a verified APK**, and device compatibility / buildability must be verified in Android Studio.

### Implemented in source

- Persistent Room save slots, automatic saving, explicit Save Now validation, delete confirmation.
- Random Life and Custom Life, six country starter presets, historical calendar *dates* (not historical content), founding family of five individuals.
- Standard, Basic Sandbox, Advanced Sandbox, Dynasty, and Sandbox-Dynasty playable selectable profiles. Basic Sandbox has active-character stat/bank edits; Advanced Sandbox additionally edits family profiles, Dynasty switches relatives, and Sandbox-Dynasty combines switching and family editing. **Other mode-specific distinctions in their eventual full designs are not all implemented.**
- Exactly one calendar month per `Advance +1 Month` operation; database transaction wraps the complete month and rolls back on failure.
- Age/birthday updates, deterministic health/aging changes, school progress and graduation, employment, salary and expense accounting, debt accrual, NPC job choices, promotions, occasional death, life-history entries.
- Salary credits, living-expense debits, and debt entries with unique monthly source keys. Monetary values use signed 64-bit integer minor units, not floating-point math.
- Working career applications (qualification checks, hiring/rejection), resignation, retirement; basic school enrollment and graduation; character-development activities.
- Permanent parent, adoptive-parent, spouse, partner, and step-parent edges, plus derived sibling, aunt/uncle, cousin, grandparent and descendant kinship. New relatives can be created, with bridge relatives added where necessary.
- Advanced editor of name, gender, date, nationality, city, appearance, attributes, studies, careers, military rank, salary, balances, assets, liabilities, and relationship quality. Structured historical records can be added. All edits audit old and new field values.
- Sandbox-Dynasty and Dynasty switches between eligible living relatives, without death, calendar changes, or asset transfers. Character and switch history survive restarts.
- Family generation explorer, relationship activities, persistent month timeline and ledger.
- Local backup export and import through Android Storage Access Framework, SHA-256 validation, separate imported save ID, ID remapping and transaction-based restoration.
- English UI, scaffolding for Filipino Android string resources, Room schema v1, JUnit tests and source-only Kotlin smoke tests.

### Not yet implemented (intentionally not presented as working)

Full Hardcore realism, Challenge gameplay, Historical Scenario world content, sophisticated family guardianship decisions, actual inheritance/property transfers, cross-world migration, professional licenses, multiple compatible concurrent jobs, businesses/investments, court/crime/government, comprehensive global institutions, achievements/rewards, complex sports/entertainment, dynamic geopolitical events, advanced child-birth simulation, fully translated UI, UI accessibility audits, and cross-device QA.

## Build in Android Studio

1. Install **Android Studio** with **JDK 17**, **Android SDK Platform 35**, and standard Android build tools. Internet access is required at least once to download Gradle and project dependencies.
2. Extract this archive, open the directory containing `settings.gradle.kts` as an Android Studio project.
3. Allow Gradle Sync. The checked-in **transparent bootstrap wrapper** downloads pinned Gradle 8.9 over HTTPS and runs it. It is a *custom* minimal bootstrap, not the official Gradle wrapper binary; its Java source is included at `gradle/wrapper/BootstrapMain.java` for inspection. After syncing, you may replace it with the official wrapper using `gradle wrapper --gradle-version 8.9`.
4. Connect an Android 8.0+ device/emulator and select **Run > Run 'app'**.
5. For the debug APK run `./gradlew :app:assembleDebug` (or `gradlew.bat :app:assembleDebug` on Windows). If it builds successfully the output will be `app/build/outputs/apk/debug/app-debug.apk`.
6. Install using Android Studio Run, or `adb install -r app/build/outputs/apk/debug/app-debug.apk`.
7. Run unit tests with `./gradlew :app:testDebugUnitTest`, and connected Android tests with `./gradlew :app:connectedDebugAndroidTest`.

**No APK is present in this source archive.** Building on a machine with the SDK is required. Android Studio may propose newer dependency versions; do not change versions casually, because the dependency set is intentionally pinned for reproducible sync.

## Tests that do not need Android

A machine with the Kotlin command-line compiler can run:

```sh
./scripts/verify-domain.sh
```

This executes 14 assertions for calendar math, birthdays, financial settlement, genealogy and switching. The source-only environment ran these assertions successfully. Android Room tests are present in `app/src/androidTest/...`, but have **not** been run.

## Key code locations

| Path | Responsibility |
| --- | --- |
| `domain/SimulationRules.kt` | Platform-independent calendar, money, kinship and switch rules |
| `data/Entities.kt` | Persistent Room entities including audits, finance journal and switch log |
| `data/GameDao.kt` | Room queries and writes |
| `data/GameRepository.kt` | Atomic gameplay transactions, creation, months, editor, switching and backups |
| `data/WorldCatalog.kt` | Fictional starter country and job data (not official wage data) |
| `ui/GameViewModel.kt` | UI events, save selection, async operations and errors |
| `ui/EverlineApp.kt` | Compose screen navigation |
| `ui/Screens.kt` | Core gameplay screens |
| `ui/EditorScreen.kt` | Advanced Family Editor |
| `docs/ROADMAP_30_AREAS.md` | Coverage map and future milestones |
| `docs/TEST_MATRIX.md` | Honest test execution record |
| `docs/ARCHITECTURE.md` | Data consistency, transaction and schema documentation |

## Test-build monetization policy

**No ads, paywalls, billing SDK, app purchase tokens, subscriptions, license validation, or premium gates are implemented.** All features actually implemented are available without payment in the modes where they belong. Future commercial policies, including the previously approved basic Sandbox free / optional permanent Advanced Sandbox and Sandbox-Dynasty purchase model, are not activated or simulated in this build. This testing choice does not change the final commercial design.

## Content statement

The named test employers, schools, demographic context, salaries and expense coefficients are **fictional/model estimates** for gameplay. The starter preset list is not a comprehensive worldwide geographic or historical database; no numeric wage is represented as an externally verified market salary.

## Known limitations / follow-on work

See `docs/IMPLEMENTATION_STATUS.md`. Key issues include: the family view is a grouped generation explorer rather than a pan/zoom tree canvas; advanced guardian-controlled choices and adoption legality are not yet modeled; history records can be added but not selectively revised; the month engine assumes a single active job; import is capped at 20 MB; and app runtime, release signing, accessibility, performance and Room integration are not yet device-verified. Never use these game balances to represent real financial information.
