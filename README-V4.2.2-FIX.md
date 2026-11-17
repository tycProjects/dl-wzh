# WEB2APK V4.2.2 build compatibility fix

- Replaced `androidx.webkit:webkit:1.8.0` with `1.6.1` so the runtime template can remain on compileSdk 33.
- Kept compileSdk 33 and targetSdk 33.
- Builder versionName 4.2.2 / versionCode 7.
- No Termux RUN_COMMAND permission is added to the installed builder.

Build the installer APK with `bash ./build-v4.sh`. This script is only for building the WEB2APK installer itself; after installation, website APK packaging runs inside WEB2APK.
