# ZTrongz FF Menu (Android)

A native Android Studio starter project for a red/black neon floating menu UI.

## Included
- Main control panel
- Floating draggable `ZT` bubble (requires Android overlay permission)
- Open/close floating panel
- Aim Assist-labelled UI toggle and saved UI preference
- Local settings only; does not read or modify Free Fire or control game aiming

## Build APK
1. Install Android Studio (with Android SDK Platform 35).
2. Open this folder as a project.
3. Wait for Gradle sync.
4. Choose **Build > Build APK(s)**.
5. Debug APK output: `app/build/outputs/apk/debug/app-debug.apk`

Command line (with JDK 17 and Android SDK configured):
```bash
gradle assembleDebug
```

If Gradle isn't installed globally, use Android Studio's Gradle tooling or generate a Gradle wrapper from Android Studio.

## First run
Open the app. Tap **BẬT MENU NỔI** and grant “Display over other apps”. Return to the app and tap the button again. Tap the floating `ZT` bubble to open/close the panel.
