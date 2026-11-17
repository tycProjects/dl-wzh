# WEB2APK V4.2.1 build compatibility fix

## Cause
The V4.2 source used `androidx.webkit:webkit:1.9.0`, whose AAR metadata requires `compileSdk` 34 or later. The Termux setup used by this project checks Android SDK Platform 33.

## Fix
Pinned `androidx.webkit:webkit` to `1.8.0`, which predates that compileSdk 34 metadata requirement and still provides `WebViewAssetLoader`, the only AndroidX WebKit API referenced by `WebActivity`. Both modules remain on `compileSdk 33` / `targetSdk 33`, so no new SDK platform is required. The app version is now 4.2.1 (versionCode 6).

## Build
From the project directory run:

```bash
bash ./build-v4.sh
```

This is a source-level compatibility correction. It has not been built on the user's device; if a new error appears, use the new Gradle log to address that separate issue.
