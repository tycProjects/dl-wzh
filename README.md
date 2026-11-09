# HyperStatusBar Pro — source project 0.2.0

Kotlin + Jetpack Compose companion application for Xiaomi / Redmi / POCO.
This archive is an editable Android Studio project, not a prebuilt APK.

## New in 0.3.0
- Root is checked automatically at startup, but the app remains usable without root.
- Root is requested only when checking access / attempting a privileged operation.
- Added guarded System UI restart action with a confirmation dialog.
- Added expandable animated bottom-sheet controls for sliders.
- Kept diagnostic logs with one-tap copy and Android share sheet.

## New in 0.2.0
- Local diagnostic log with bounded rotation (512 KiB current + one backup).
- Logs screen with one-tap **Copy** and Android Sharesheet **Send**.
- Root check, config-write and apply events are written to the diagnostic log.
- The app does not collect the full system logcat or personal notification contents.

## Build
- Android Studio with JDK 17
- Android SDK Platform 35
- Gradle sync, then `:app:assembleDebug`

## Important technical scope
The companion app and config bridge do not by themselves patch Xiaomi SystemUI.
There is no universal stable API/resource map for arbitrary status-bar layout changes
across MIUI 14 / HyperOS releases. A real LSPosed hook/RRO backend must be developed
and validated per firmware fingerprint and device. This project intentionally does
not pretend that untested hooks are production-ready.

The ZIP is not padded with random data to meet an arbitrary size. Large proprietary
fonts, SDKs or binary assets are not bundled without redistribution rights.
