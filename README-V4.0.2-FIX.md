# WEB2APK V4.0.2 — Runtime APK discovery fix

Fixed `:builder:bundleRuntimeTemplate` failing after `:runtimeTemplate:assembleRelease`. The previous build script assumed the output was exactly `runtimeTemplate-release.apk` and rejected files smaller than 50 KB. The updated Gradle task searches `runtimeTemplate/build/outputs/apk/` recursively for a real `.apk` file larger than 1 KB, logs the chosen APK and size, and provides a directory diagnostic if none exists.

Builder application version: 4.0.2 (versionCode 2). No Termux RUN_COMMAND permission was added.
