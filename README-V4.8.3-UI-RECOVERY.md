# WEB2APK V4.8.3 — UI Recovery Build

This release is a recovery update after the V4.8.2 UI became unresponsive on device.

## What changed
- Restores the exact V4.8.1 builder `index.html` UI and interaction layer which predates the V4.8.2 regression.
- Keeps the V4.8.2 native project workspace, project operations, build engine, runtime controls, and Android bridge code.
- Keeps the same application ID (`com.web2apk.v4.internal`) and increments versionCode to 22 / versionName 4.8.3 so it can be installed as an update without uninstalling V4.8.2.
- Does not clear application data or signing identity.

## Important
The new V4.8.2 Studio control screens are temporarily not exposed in this recovery UI. They should be reintroduced in small, tested changes after confirming the builder is responsive again. The visible UI is intentionally restored to the V4.8.1 layout, not redesigned.

## Build
```bash
cd ~/WEB2APK-V4-Internal
bash ./build-v4.sh
```

The archive was checked for ZIP integrity and static source syntax. A full Android Gradle build and on-device touch test have not been run in this environment.
