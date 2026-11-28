# Pixel Calculator — WZH 2 Android source

This is a native Android Gradle wrapper project containing the original Pixel Calculator web app in:
`app/src/main/assets/www/`

## Import/build in WZH 2
1. Extract this ZIP only if WZH 2 asks for a folder; otherwise upload the ZIP as the Android project source.
2. Select the Android / APK-AAB project build flow.
3. Set application name to **Pixel Calculator** if the builder asks.
4. Use package ID `com.kerviii.pixelcalculator` and version `1.1.0` (version code `1`) unless you intentionally need to change them.
5. Build, install, and test the APK before publishing.

## Project notes
- Standard Android Gradle project layout (`settings.gradle`, root/app Gradle files, manifest, Java activity, resources).
- The app runs locally in an Android WebView. Its calculator interface, scientific engine, themes, history, memory, and utility tools are bundled in the APK.
- No backend, account, or internet permission is required for normal operation.
- The original source archive did not include the optional vendored Math.js library or font binaries. The app retains its built-in calculation engine and CSS fallback fonts; no external downloads were inserted.
- This is source code, not a prebuilt APK/AAB. WZH 2 must support importing a standard Android Gradle project ZIP. If it expects a different ZIP template, use its project-template/import format instead of treating this as an APK.

## Original web source
The source files are preserved under `app/src/main/assets/www/`, including its README, tests, manifest, styles, scripts, and service worker.
