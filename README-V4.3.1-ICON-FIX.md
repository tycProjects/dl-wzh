# WEB2APK V4.3.1 — Gallery Icon Build Fix

## Changes
- Fixes Gallery icon replacement by updating the indexed ARSCLib `ResFile` inputs before writing the customized APK.
- Replaces every `drawable/app_icon` and `mipmap/app_icon` resource variant, preserving resource-table paths and IDs.
- Verifies each replaced icon input by file size and CRC before the APK is serialized.
- Removes the unreliable post-serialization raw ZIP path matcher that caused `Launcher icon resource app_icon.png was not found` failures.
- Adds a slightly stronger but still subtle lilac/cyan/pink gradient tint to `Made By Tg - @Hop3_1`.
- Increments Builder version to 4.3.1 / versionCode 9.
- Uses AndroidX WebKit 1.5.0 with compileSdk 33 to avoid the user's previous minCompileSdk 34 mismatch.

## Build
Run `bash ./build-v4.sh` in Termux to compile the WEB2APK installer. The installed WEB2APK app builds website APKs internally and does not invoke Termux.

## Validation status
Source-level checks only in this package environment; the Android Gradle build and generated APK installation must be verified on the target device.
