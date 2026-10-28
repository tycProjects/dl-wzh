# WZH Flutter IDE

An AIDE-style IDE for Flutter that runs entirely on the phone. Your WZH 2 APK code editor
(highlighting, suggestions, formatter, find and replace, project search) on top, a real terminal under it.

## What happens on the first launch

1. The startup animation plays.
2. **Setup** opens by itself and needs nothing from the person:
   * downloads `proot` (from the Termux package repository) and an Ubuntu 24.04 arm64 root,
   * opens a visible terminal that installs Java 17, the Flutter SDK, the Android SDK pieces,
     points Gradle at a working `aapt2`, downloads Flutter's Android files, and runs a test build.
   * Finished steps are remembered, so "Try again" (or reopening the app) continues where it stopped.
3. The **Projects** screen: New project (runs `flutter create`), Import zip, Terminal.
4. In a project: edit with the editor; the green terminal icon shows the terminal; **Run** builds a
   debug APK with `flutter build apk --debug` and opens Android's installer. The `...` menu has
   `flutter pub get`, `flutter analyze`, release build, `flutter clean`, reload from disk.

Edits are written into the project folder inside Ubuntu about 1.5 s after typing, and before every command.

## Building this app

```
flutter create --org com.wzh --project-name wzh_flutter_ide .
python3 tool/patch_android.py
flutter build apk --release --target-platform android-arm64
```

`patch_android.py` sets targetSdk 28 (so the app may run the tools it unpacks), arm64 only,
extractNativeLibs, permissions, the one Kotlin method, and copies `libaapt2.so`.
`flutter_pty` has a small native part, so the Android NDK is needed to build (Flutter downloads it itself).

## Needs on the phone

Android 8 or newer, 64-bit ARM, about 6 GB free, Wi-Fi for the first run (1.5 to 2 GB download).
Install it from the APK (it targets Android 9 on purpose, so it is not for Google Play).

## Not tested yet

This was written without being able to run Flutter or a phone. Expect the first real run to need
one or two small fixes, most likely in the installer: see `lib/core/bootstrap.dart` (downloads, unpacking)
and `lib/core/setup_script.dart` (the commands the terminal runs). The setup terminal shows exactly where it stops.
Known soft spots: release APKs (Flutter's `gen_snapshot` may not exist for ARM Linux hosts, debug builds do not need it),
plugins that need the NDK, and phones that stop background processes (keep the app open; the screen stays on during setup).
