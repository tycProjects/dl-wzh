# SensiGreen FF Overlay PRO MAX (starter project)

Native Android/Kotlin + Jetpack Compose project. Package: `com.sensigreen.ffoverlay`.

## Build
1. Install Android Studio (a version compatible with Android Gradle Plugin 8.7.3) and JDK 17.
2. Open this folder in Android Studio.
3. Allow Gradle sync to download dependencies.
4. Build > Build Bundle(s) / APK(s) > Build APK(s).
5. Install the debug APK on an Android 12+ device.

## Included in this starter
- Local PIN login (initial PIN `123`), change PIN setting.
- Compose dashboard and navigation for Home, Sensi, Aim, Overlay, Anime and Settings.
- Sensitivity sliders with local DataStore persistence and text export.
- Crosshair preview controls.
- Overlay permission flow and floating `SG` bubble with a small lock/unlock panel.
- Anime character theme selection list.
- Device resolution / refresh-rate display.

## Important limitations
- This source package has not been built or tested on a physical device in this environment. Build and test in Android Studio before distributing.
- Anime gallery currently uses stylized in-app character markers, not bundled official character wallpaper files. Add properly licensed images under `app/src/main/res/drawable` if you have rights to use them.
- PIN is a basic local app gate, not high-security authentication. Do not reuse sensitive passwords.
- Crosshair shown in the main app is a preview. This starter's floating service creates a movable bubble and panel; it does not draw a true crosshair over the game, automate shooting, change game sensitivity, or lock enemies.
- Android 14/15 may require foreground-service policy adjustments depending on device and distribution target. Review Android Studio build warnings and test overlay permission on the target phone.
