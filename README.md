# FILE MAX — Android Studio project

FILE MAX is an independent Android file-manager project. The attached specification was used as a feature reference; the original brand name and artwork were not copied.

## Stack
- Kotlin 2.0.21
- Jetpack Compose + Material 3
- Android Gradle Plugin 8.7.3
- Minimum Android 8.0 (API 26), compile/target API 35

## Open and build
1. Open this folder in Android Studio (Jellyfish or newer recommended).
2. Use JDK 17 for Gradle.
3. Allow Android Studio to download the Android Gradle Plugin and dependencies.
4. Build with `gradle :app:assembleDebug` or use **Build > Make Project**.

A Gradle wrapper JAR is not included because no authentic wrapper binary was available in the build environment. Do not substitute a placeholder JAR. Android Studio can generate the wrapper using its installed Gradle distribution.

## Implemented in this source snapshot
- First-run request for Android's all-files access on Android 11+; legacy storage permission request on older supported versions.
- Refreshes permission state when the app resumes from Android Settings.
- Live listing of shared-storage files and folders using the real filesystem (not sample data).
- Limited-mode browsing of a user-selected SAF tree and its subfolders when broad storage access is denied.
- Folder navigation, refresh, filename search, hidden-file toggle, list/grid view, and name/size/date sorting.
- Open files through Android's content-sharing mechanism (FileProvider) or the SAF URI.
- Create folder, create empty file, rename, delete, recursive copy/move, destination-path dialogs, file properties, and Android share-sheet support.
- Basic ZIP archive creation and ZIP extraction using streaming I/O.
- ZIP extraction validates canonical destination paths to block `../` path traversal.
- Basic folder-size summary, largest-files scan, and SHA-256 calculation for a user-selected file.
- Arabic-first UI, basic light/dark theme, and FILE MAX yellow/charcoal identity.

## Known limitations — not a complete release
This is a working foundation, not the complete 55–65k-line product described in the source prompt. The following are not implemented in this snapshot and must not be treated as available features:
- File operations (create/rename/delete/copy/move) on SAF-only documents; SAF mode currently supports browsing and opening only.
- 7Z, RAR/RAR5, TAR, GZ, BZ2, XZ, ZST, ISO, CAB, ARJ, LZH, EPUB engines.
- ZIP AES encryption, ZipCrypto, password prompts, split volumes, archive comments, archive test and full archive metadata browser.
- Copy/move destination picker, recycle bin, batch rename, conflict policies, undo, and durable background operation queue/notifications.
- Full media/PDF/GLB/GLTF/OBJ viewers, APK inspector/installer flow, duplicate finder, advanced search, persistent favorites/history, Room and DataStore settings.
- Complete English localization, complete dark-mode audit, accessibility audit, adaptive/monochrome launcher icon set, and full automated/instrumented test suite.
- Figma editable screens: the Figma document was created, but the connected editing quota prevented authoring the actual frames and components.

## Storage access notes
On Android 11 and later, broad shared-storage access is a special permission controlled by Android Settings. If denied, the app offers Android's Storage Access Framework so the user can choose a folder to browse. Android's restrictions on other apps' private directories still apply. `Android/data` and other protected locations are not bypassed.

## Privacy
Core file operations are local. This project includes no analytics SDK, network permission, or file upload code.

## Verification status
The pure Kotlin ZIP engine was compiled independently and tested with a create/extract round trip and a malicious `../` entry; both checks passed. The Android app itself could not be built here because this environment did not contain Android SDK or Gradle tooling. `assembleDebug`, UI tests, and real-device permission tests remain unverified and must be run in Android Studio on a device before release.

## Design handoff
See `docs/FIGMA_HANDOFF.md` for the created Figma file link, design tokens, screen list, and component handoff. The Figma canvas is not yet populated with editable screens.
