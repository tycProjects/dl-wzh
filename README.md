# SupBrother Android Launcher — starter project

This is an Android Studio Kotlin/Jetpack Compose **UI starter**, not a playable Minecraft launcher.

## Open the project
1. Extract `SupBrother-Android-Launcher-Project.zip`.
2. Open the extracted `SupBrother-Android-Launcher` folder in Android Studio.
3. Allow Gradle sync and install the Android SDK components Android Studio requests.
4. Select **Build > Build Bundle(s) / APK(s) > Build APK(s)**.

The expected debug APK location after a successful local build is:
`app/build/outputs/apk/debug/app-debug.apk`

## Current limitations
The Play button is intentionally disabled. This starter does not include:
- Minecraft game files or a Minecraft engine
- Microsoft/Minecraft account authentication
- version manifest and asset/library downloader
- Android Java runtime provisioning
- native OpenGL/Vulkan rendering integration
- actual game launching or FPS optimization

A licensed Android Java launcher core must be integrated before this can launch Minecraft. Do not distribute Minecraft game files or authentication credentials inside this project.

## Compatibility
The project is configured for Android min API 26 / target API 35 and Java 17 source compatibility. These settings alone do not guarantee compatibility with every Android device.
