# ZEPHYR MUSIC — Android project

This ZIP is packaged with `settings.gradle`, `build.gradle`, and `app/build.gradle` at the project root so build services that scan for Groovy Gradle files can detect it.

## Build
1. Upload this ZIP to your Android build service, or open the extracted folder in Android Studio.
2. Use JDK 17 and Android Gradle Plugin 8.6.1.
3. Sync Gradle, then build `:app:assembleDebug` (or Build > Build APKs).
4. APK output: `app/build/outputs/apk/debug/app-debug.apk`.

Minimum Android: 8.0 (API 26). Compile/target SDK: 35.

## App icons
Launcher icons are in `app/src/main/res/mipmap-*` including `mipmap-hdpi/ic_launcher.png` (72×72), plus mdpi, xhdpi, xxhdpi, xxxhdpi and adaptive icon resources.

## Notes
The app can play supported local audio and direct HTTPS audio streams. It does not include a licensed universal music catalogue; streaming availability depends on the lawful stream URLs or provider integrations configured by the app owner.
