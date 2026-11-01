RYUZEN TAS - Builder-compatible Android Gradle project

The project now uses Groovy Gradle filenames:
- settings.gradle
- build.gradle
- app/build.gradle

The ZIP has settings.gradle at its top level so upload scanners can detect it.
Upload this ZIP as an Android/Gradle project, not as a website or WebView project.

Build requirements:
- Android Gradle Plugin 8.7.3
- Kotlin Android plugin 2.0.21
- JDK 17
- Android SDK 35

This app provides a safe monitoring dashboard and Shizuku permission status.
It does not perform CPU/GPU overclocking or disable thermal protections.
