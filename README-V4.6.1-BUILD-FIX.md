# WEB2APK V4.6.1 Build Fix

This is a narrowly scoped build reliability update on top of V4.6.0.

## What changed
- The builder and runtime Gradle projects now accept the Build Tools version selected by `build-v4.sh`.
- The build script no longer requires exactly `34.0.0`. It uses `WEB2APK_BUILD_TOOLS_VERSION` when provided, otherwise prefers installed `34.0.0`, and otherwise selects the highest installed numeric Build Tools version.
- The script verifies Java 17+, Android Platform 33, Gradle wrapper presence, and whether Termux AAPT2 can actually execute.
- Gradle output is saved to `build-v4.log`; a failed build prints the last 100 lines instead of closing with little context.
- Builder app version is 4.6.1 (versionCode 15).

## Termux
Run from the extracted project directory:

```bash
bash ./build-v4.sh
```

To choose a specific installed Build Tools version:

```bash
WEB2APK_BUILD_TOOLS_VERSION=34.0.4 bash ./build-v4.sh
```

The project still requires Android SDK Platform 33 at `$ANDROID_HOME/platforms/android-33/android.jar`, Java 17+, Gradle 8.9 wrapper download (internet on first run), and Maven dependency access on first build.

## Diagnostics
If it still fails, send `build-v4.log` from the project directory. Do not paste the API key; build logs should not contain it.

A full Android Gradle build could not be run in the authoring container because it has no Android SDK and no network access to download Gradle/Maven artifacts. Shell syntax, XML manifests, and ZIP packaging were checked locally.
