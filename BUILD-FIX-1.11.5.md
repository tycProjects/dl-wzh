# Android Island 1.11.5 — Kotlin compile fixes

## Fixed
- `AiLauncherActivity.kt`: `AiFilePickerActivity` extends framework `android.app.Activity`, not `LifecycleOwner` / `ComponentActivity`. Replaced invalid `lifecycleScope.launch` usage with a Main-dispatcher coroutine scope; file staging remains on `Dispatchers.IO` and UI feedback/finish remain on Main.
- `AiPanel.kt`: removed the explicit import of `androidx.compose.foundation.layout.weight`. `weight` is a RowScope/ColumnScope member extension and is available in the relevant scope; importing its internal backing property caused the compiler access error.

## Validation
- Source-level checks confirm the invalid lifecycleScope reference and explicit weight import are removed.
- Full Gradle build has not been run in this environment. Build with the project's configured Gradle 8.9 / AGP 8.7.2 / Java 21 to verify.
