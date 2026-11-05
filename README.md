# Lapu Launcher — Android Studio project

A Kotlin + Jetpack Compose starter project for Android 10+ (minSdk 29), using a blue/black theme.

## Open and build
1. Install Android Studio (a recent stable release) and Android SDK Platform 35.
2. Open this folder in Android Studio.
3. Allow Gradle sync to complete.
4. Run the `app` configuration on an Android 10+ device or emulator.
5. To build a debug APK: **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
   The APK is normally created at `app/build/outputs/apk/debug/app-debug.apk`.

## Important implementation boundaries
- This project is a launcher UI starter, not a complete Minecraft runtime.
- Java Edition requires integration with a compatible PojavLauncher runtime and its supported distribution/licensing requirements. This project does not bundle PojavLauncher or Minecraft.
- Bedrock Edition is not launched by this app. It must be installed separately from an official source; this starter displays an explanatory message.
- Version selector entries are sample UI profiles, not downloaded game versions.
- Mod, shader, resource-pack, FPS, and control pages are UI placeholders to be implemented with edition-specific storage/runtime APIs.
- No Microsoft authentication, game assets, proprietary binaries, or game files are included.

## Release signing
For a real release APK, configure a signing key in Android Studio via **Build > Generate Signed Bundle / APK**. Keep your keystore and passwords private; never commit them to source control.

## Tech
- Kotlin
- Jetpack Compose / Material 3
- Gradle Kotlin DSL
- minSdk 29 (Android 10)
