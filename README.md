# AX Render & Freeze PRO — Android Studio Project

This is an Android Studio Gradle project. It recreates the visible UI style and navigation from the provided video as a non-root utility prototype.


## Included
- Home, Games library, Render presets, Freeze checklist, Device dashboard, Settings
- Locally saved toggles/preset/game library (WebView localStorage)
- Native device information bridge
- Native launcher bridge for user-entered package IDs
- Android launcher icons in mdpi, hdpi, xhdpi, xxhdpi, xxxhdpi and adaptive icon (Android 8+)

## Important limitations
- Render presets are in-app preferences only; they do not change game graphics settings, GPU drivers, or Android performance.
- The Freeze page is a reminder/checklist only. Third-party apps cannot be frozen by an ordinary non-root app without privileged access.
- Android SDK/Gradle are not bundled. Build using Android Studio with SDK 35 and JDK 17+.

## Build
1. Open this folder in Android Studio.
2. Let Gradle sync; install Android SDK Platform 35 if prompted.
3. Select Build > Build APK(s).
4. Output is typically `app/build/outputs/apk/debug/app-debug.apk`.

Application ID: `com.axrender.freezepro`
