# Material You 3 Starter

Open this folder in Android Studio (not an individual Java file). Android Studio downloads the Android Gradle Plugin, Gradle distribution, Android SDK platform, and Material Components dependency as needed.

## Build
1. Open this project in Android Studio.
2. Let Gradle sync and install any requested SDK components.
3. Build > Build Bundle(s) / APK(s) > Build APK(s).

Output: `app/build/outputs/apk/debug/app-debug.apk`

## Gradle wrapper
The project includes `gradle/wrapper/gradle-wrapper.properties` and launcher scripts. If `gradle-wrapper.jar` is missing, Android Studio can still import/sync the project and you can regenerate wrapper files from a machine with Gradle installed using `gradle wrapper --gradle-version 8.9`. The launcher scripts print a helpful message rather than failing mysteriously.
