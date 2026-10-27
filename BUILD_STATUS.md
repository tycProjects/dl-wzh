# Build status (2026-10-27)

**Source integration created; Android build not verified.**

Verified in this environment:
- `scripts/patch_flutter_android.py` passes Python bytecode compilation.
- Shell scripts pass `bash -n` syntax checks.

Not verified:
- Fetching `oonid/pr` and building its ARM64 native binaries.
- Flutter host APK compilation.
- Engine bootstrap on a real ARM64 Android device.
- Ubuntu / Flutter / Android SDK installation and local target APK build on a phone.

Reason: this environment has Java but no Flutter SDK, Dart, Gradle command, Android SDK/NDK, or Rust Android build setup; its container cannot resolve GitHub directly. No claim of a working APK is made.
