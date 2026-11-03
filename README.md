# CAM STREAM MOBILE

Native Android project (Kotlin + Jetpack Compose). RTMP/live streaming has been removed as requested.

## Build configuration
- Android Gradle Plugin: 8.7.3
- Kotlin: 2.0.21
- Compose compiler plugin: org.jetbrains.kotlin.plugin.compose 2.0.21
- JDK: 17
- Android compile SDK: 35
- Gradle task: :app:assembleDebug

Open the folder containing `settings.gradle.kts` and `build.gradle.kts` as an Android Gradle project in a builder that supports native Android Gradle projects.

## Included
- MediaProjection screen recording service and recording controls
- Recording library with play/share/delete actions
- CameraX front/back camera preview
- Recording preferences and app settings

## Important
This archive is a native Android Gradle source project, not a prebuilt APK. A build service must support Android Gradle projects and Android SDK 35. The `404` shown while polling `Build in progress` is a response from the build service's status endpoint; a ZIP alone cannot guarantee or repair that remote service response.
