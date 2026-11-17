# WEB2APK V4.8.5 — Create Label & Telegram Settings Fix

This is a narrowly-scoped UI interaction update based on V4.8.4.

## Changes
- Replaced the centre Create navigation pseudo-element label with a real, fixed-position span inside the button.
- Disabled transform/press transitions on the centre Create button so its label does not jump when selected or tapped.
- Moved the Telegram User and Channel card to the first position in Settings.
- Added a Creator Credit switch in Settings and synchronised it with the existing Create-page switch.
- Added the note under the creator line: "You can hide this credit in Settings."
- Preserved the application ID, data location, existing page IDs, and existing WebAPK builder flow.

## Version
- versionName: 4.8.5
- versionCode: 24
- applicationId: com.web2apk.v4.internal

## Build
```bash
cd ~/WEB2APK-V4-Internal
bash ./build-v4.sh
```

## Validation
ZIP integrity, HTML inline JavaScript syntax, shell syntax, and AndroidManifest XML are checked for this archive. A full Android Gradle build and device touch test must still be completed on the user's Termux device.
