# Build fix 1.9.1 (cloud-builder friendly)

- Removed the `:baselineprofile` module and the `androidx.baselineprofile` plugin (it needs a connected device, which cloud builders do not have).
- `tools/verify_package.py` updated to 1.9.0 (versionCode 99); stale regression guards refreshed. It now passes.
- `VERSION.txt` synced to 1.9.0.
- Still: minSdk 35 (Android 15+). Build debug: `./gradlew assembleDebug`. Release is unsigned.
- Not compiled here (no Android SDK) - build once and test on a device.
