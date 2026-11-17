# WEB2APK V4.8.6 — Generated App Creator Credit Visibility Fix

## Changes
- Added the exact line `You can hide this credit in Settings.` directly underneath `Made By Tg - @Hop3_1` in the native creator watermark shown inside generated website APKs.
- The two-line watermark remains controlled by the existing Creator Credit toggle in WEB2APK. When disabled before building, both lines are omitted from the generated APK.
- Kept the existing builder UI, navigation, project flow, package ID, and generated-app settings unchanged.
- Bumped the builder app version to 4.8.6 / versionCode 25 for upgrade installation.

## Note
The hide/show control is in the WEB2APK builder's Settings before generating the website APK. The generated website APK displays the two-line credit when that option is enabled.

## Build
```bash
cd ~/WEB2APK-V4-Internal
bash ./build-v4.sh
```

## Validation
The source change is limited to the generated-app watermark method plus version metadata and build labels. ZIP integrity and static source checks should be completed before use. A full Android Gradle build and on-device visual check are still required on the Termux device.
