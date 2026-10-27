# Local AIDE — embedded, no-Termux source kit

This source kit changes Local AIDE from a Termux `RUN_COMMAND` bridge to an **embedded Android PRoot engine**. The intended installed-app flow is:

1. Open Local AIDE and grant the file access needed for `/storage/emulated/0/LocalAIDE`.
2. Tap the help/download icon, then **Install toolchain**.
3. The app creates an Ubuntu container in its own private app storage, downloads Flutter 3.27.4, Java 17, Android SDK 35, ARM64 Android build-tool replacements, and NDK r27c + CMake.
4. Create or open a project, edit Dart files, then press **Build Debug APK**. The APK is written to `/storage/emulated/0/LocalAIDE/builds/<project>-debug.apk`.

The installed app does **not** call Termux, a Render build API, GitHub Actions, or another cloud builder to compile your project. The first setup and dependency downloads do require internet. Source code and build output remain on the device.

## Important: this is a source kit, not a prebuilt/tested APK

I could not compile or run an Android APK in this environment: Flutter, the Android SDK/NDK and Rust Android target toolchain are not installed here, and direct GitHub cloning from this container is unavailable. The scripts below perform the native-engine build on a development host. The resulting APK still needs to be installed and tested on a real ARM64 Android phone before it should be considered working.

This distinction matters: the embedded `proot-engine` needs native ARM64 libraries included inside the IDE APK. Merely adding the Flutter UI without those libraries will not produce a working local builder.

## Requirements to build the Local AIDE IDE APK itself

On the computer or build host used to assemble the *IDE application*, install Flutter, Java 17+, Git, Rust/Cargo with the `aarch64-linux-android` target, and an Android SDK/NDK compatible with the upstream engine build. The IDE build compiles PRoot native code into the APK. This developer-side setup is separate from the target-app toolchain the installed IDE later downloads on your phone.

Run from this folder on a Linux build host:

```bash
scripts/build_ide_debug.sh
```

The script creates a Flutter Android host project, fetches the open-source PRoot engine repository, compiles its ARM64 native pieces, patches the Flutter Android host, and attempts to build a debug APK. Expected output:

```text
build/LocalAIDE-NoTermux-debug.apk
```

The script is intentionally set to stop if a required native binary is missing. Do not distribute an APK if the engine build failed or the required `libproot.so`, `libproot-loader.so`, `libpr-cli.so`, `libbusybox.so`, or `libptyjni.so` files are missing.

## Toolchain and build limitations

- First-time toolchain setup needs internet and roughly **6–8 GB of free storage is recommended** for the Linux root filesystem, Flutter, Android SDK/NDK, Gradle caches and dependencies.
- The referenced ARM64 Flutter toolchain is intended for debug APK builds. Release/AOT builds may need an x86-64 build host or another working ARM64 Flutter AOT toolchain.
- The Android PRoot engine targets Android 9/API 28 or later and ARM64 (`arm64-v8a`).
- The IDE stores editable projects in `/storage/emulated/0/LocalAIDE/projects/`, logs in `/storage/emulated/0/LocalAIDE/logs/`, and APKs in `/storage/emulated/0/LocalAIDE/builds/`.
- To avoid trying to execute Gradle scripts directly from shared storage, build steps copy the project into the app's internal Ubuntu environment, compile there, then copy only the resulting APK back to shared storage.
- The toolchain installer downloads the published `arm64-flutter-setup` script from its upstream repository when the user explicitly taps Install toolchain. Review that upstream source before distributing this app to others.

## Engine integration and upstream source

At IDE build time, `scripts/prepare_no_termux_engine.sh` clones <https://github.com/oonid/pr> and embeds its `:proot-engine` module plus the required ARM64 native libraries. No separate Linux-terminal app is installed on the target phone; the native runtime ships inside the IDE APK. The Ubuntu root filesystem and large Flutter/Android toolchain components are downloaded later into Local AIDE's private app storage.

The ARM64 toolchain installer used at runtime is documented at <https://github.com/0xR0/arm64-flutter-setup>. Its upstream setup uses Flutter 3.27.4, Java 17, Android SDK 35/build-tools 35.0.1, and ARM64 replacements for Android build tools. The Local AIDE integration also downloads NDK r27c and CMake. However, the NDK package may contain x86-64 host tools that cannot execute natively in an ARM64 Linux container, so Flutter plugins that compile native code are not guaranteed to work on-device. The primary supported target for this prototype is a debug APK whose dependencies do not require an unsupported native host compiler.

Keep the upstream repository and license files in the fetched vendor tree when building or redistributing. Review the license terms of the PRoot engine, its vendored components, Flutter, Android SDK components and toolchain binaries before public distribution.

## What this kit changed

- Removed the `com.termux.RUN_COMMAND` bridge.
- Added a native `ProotHost` implementation and app-private PRoot bootstrap.
- Added an embedded command runner for Ubuntu installation, Flutter toolchain setup, project creation and debug APK builds.
- Added a toolchain setup UI in the editor.
- Added a repeatable developer script to build and package the embedded ARM64 PRoot engine.

## Validation status

- Shell scripts: syntax-checked in this environment.
- Android Gradle/Kotlin build: **not run** (required toolchains are unavailable here).
- On-device Ubuntu install and debug APK build: **not run**.

Please treat this as an implementation source kit to build and test, not a verified release APK.
