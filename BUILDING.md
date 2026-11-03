# Build instructions

Project root is the directory containing `settings.gradle.kts`.

Requirements: JDK 17, Android SDK Platform 35, Android Gradle Plugin 8.7.3, Kotlin 2.0.21.

Run: `gradle :app:assembleDebug`

The first build requires network access to Google Maven and Maven Central to resolve plugins and dependencies.
