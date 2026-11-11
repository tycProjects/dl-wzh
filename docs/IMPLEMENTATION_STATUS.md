# Everline implementation status — temporary prototype v0.1

**Authoring environment:** 2026-11-05 · source archive delivered · Android SDK/Gradle distribution and network unavailable.

| Feature | Source status | Verified? | Important scope |
| --- | --- | --- | --- |
| Android app + icon / Material 3 UI | Implemented in source | No Android runtime | Compose navigation, native screens, original vector asset |
| Offline save slots | Implemented in source | No device test | Room autosave/manual timestamp, CRUD |
| Advance +1 Month | Implemented in source | Pure Kotlin calendar passes | Room transaction/device runtime not tested |
| Birthdays, age, health, mortality | Implemented in source | Calendar assertions pass | Simplified mortality model |
| Random/Custom Life | Implemented in source | No Android runtime | Starter world and five-person founding family |
| World geography and history | Partial | No | Six modeled country presets; historical date handling only |
| Careers / applications / promotions | Partial foundation | No device test | One active job, starter occupations, modeled wages |
| Education | Partial foundation | No device test | Four sequential stages, completion record; fictional schools |
| Month wages and expenses | Implemented foundation | Arithmetic assertions pass | Single job; modeled living expenses |
| NPC autonomy | Implemented foundation | No integration run | Family NPCs job search, health, income, aging |
| Relationships | Partial foundation | Family graph assertions pass | Explicit kinship + quality changes; no full dating/marriage workflows |
| Family tree | Implemented basic explorer | No Android runtime | Generational cards, parent links, edit access; pan/zoom canvas later |
| Advanced Sandbox Family Editor | Implemented foundation | No Android runtime | Broad fields, records, added family links, audit trail |
| Sandbox-Dynasty switching | Implemented in source | Eligibility assertions pass | Living relatives, configurable minor policy; guardian decisions partial |
| Permanent historical records | Implemented foundation | No device test | Profile records and event/audit logs; editing existing records later |
| Backup export / restore | Implemented in source | No device test | SHA-256 JSON, remap into new save slot, transactional import |
| Standard / Basic Sandbox / Advanced Sandbox / Dynasty / Sandbox-Dynasty | Selectable, with distinct sandbox/switch rules | No Android runtime | Some future specialized rules still missing |
| Hardcore / Challenge / Historical Scenario | Not implemented | — | Clearly disclosed as unavailable, never called 'premium locked' |
| Businesses, investing, taxes, real estate | Not implemented | — | Domain roadmap preserved |
| Crime, justice, government, political elections | Not implemented | — | Domain roadmap preserved |
| Sports, fame, entertainment, religion, dynamic world events | Not implemented | — | Domain roadmap preserved |
| All-free testing / no billing | Implemented by omission | Source inspected | Zero monetization dependencies or code |
| Production APK | Not produced | — | Android build environment unavailable |

## What remains to be done first

1. Import into Android Studio and successfully sync/build/repair any Android SDK or dependency compatibility issues discovered.
2. Execute tests on Android 8–16 devices/emulators, including force-stop/relaunch and offline scenarios.
3. Fix any Room query compilation warnings or Compose UI issues revealed by full Android compilation.
4. Add automated data-integrity, full genealogy, long-running dynasty, monthly idempotency and backup-corruption tests.
5. Implement richer dynasty inheritance and guardian management without disrupting the shared world calendar.
6. Expand world content using verified datasets with clearly documented factual vs fictional fields.
7. Extend the other roadmap areas while leaving source and saves migratable.

**The phrase 'implemented in source' is not a claim of Android build or device verification.**
