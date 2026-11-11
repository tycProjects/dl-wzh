# Everline 0.1 architecture and invariants

## Data flow

`Compose UI → GameViewModel → GameRepository → Room DAOs → SQLite`.

`GameRepository` is the authoritative mutation layer. Every operation that could change multiple objects is enclosed in `RoomDatabase.withTransaction`. The ViewModel serializes the interface's mutation requests with a `busy` guard. A crash or exception within a month rolls back the full month instead of leaving the calendar ahead of balances or relatives.

Every save owns its `SaveSlotEntity` and a shared `year/month`. Characters have stable UUIDs, live independently, and are attached by `saveId`. The active controlled character is identified by `activeCharacterId`. Editing/switching never swaps or transfers account balances.

## Month progression sequence (v0.1)

1. Atomically update shared calendar by exactly one month.
2. Calculate birthdays/ages, including older and inactive relatives.
3. Apply health drift.
4. Advance active education progress and generate degree completion records.
5. Increment career tenure and modeled promotions; apply independent NPC job decisions.
6. Settle income and living costs with integer minor units; uncovered costs generate debt.
7. Persist per-source journal entries with unique `(saveId, sourceKey)` to guard against double charging.
8. Store real state changes in event history; append active person's monthly accounting summary.
9. Determine age/health-related death without deleting any genealogical link.
10. If the active person dies, optionally continue with a connected living successor.
11. Validate parent links and character references; commit entire transaction.

Business, investment, criminal justice, dynamic news and births are explicitly pending. There are no random story notifications pretending those economic/relationship systems already exist.

## Family and history

Canonical parenthood is a directed `PARENT` or `ADOPTIVE_PARENT` link. `SPOUSE`, `PARTNER`, and `STEP_PARENT` are explicit associations. Sibling/cousin/aunt/uncle/grandparent classifications derive from the canonical edges, rather than being mutually inconsistent parallel labels. The editor creates intermediary relatives when a requested extended relationship requires them; it never deletes existing ancestry.

The advanced editor applies all field changes in one transaction, writes one audit row for every changed field, and logs a sandbox event. A deliberate `sandboxOverride` can ignore normal parent/child age eligibility, but cannot create malformed dates or negative balances. The state history is preserved, not silently rewritten.

Dynasty switching checks relatedness, living status and the slot's eligibility policy. The switch transaction changes only active character metadata, switch/event audit records and `updatedAt`. The calendar and character balances are not touched by switching. Some minor-controlled decisions have explicit game-action age checks; sophisticated guardian decision trees are future work.

## Saving, import and schema evolution

- Room DB version `1`, exported schema on Android build, no destructive fallback.
- No version upgrades yet exist, so no migration implementation is warranted at schema v1. Before any schema v2 release, supply a tested `Migration(1, 2)` and a generated checked-in schema archive. Do not call `fallbackToDestructiveMigration`.
- Android Storage Access Framework local import/export avoids network permission.
- Exported envelope carries SHA-256 of the serialized payload. Import verifies envelope version, checksum, UUID uniqueness and references before any DB writes; imports into newly generated character/save IDs in one transaction.
- Manual Save Now validates references and updates the save timestamp. UI writes are always persisted immediately.
- No cloud sync or remote restore service is promised.

## World data

The initial catalog is intentionally representative, not a purported full globe: Philippines, USA, UK, Japan, India, Canada and seven modeled occupations. Salaries are *fictional game balancing inputs*, and currency names do not imply conversion to a live exchange rate. Professional licensing, actual institutions, historical legal rules and geopolitical simulation require separate datasets with factual provenance.

## Compilability and verification

The Android Studio project has all handwritten Gradle / Kotlin / XML / resource files and a Java-built bootstrap jar. Room/data/domain Kotlin sources were additionally type-checked in this authoring environment with temporary stubs for unavailable Android/Room/Serialization libraries. This **is not a Gradle/Android compile**. Platform-independent Kotlin source was compiled using the installed `kotlinc`. Gradle sync, actual Compose compilation and Room annotation processor output, device lifecycle/restart behavior, and APK installation remain unverified until an Android build environment is used.
