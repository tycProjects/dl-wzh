# WEB2APK V4 Internal — Resource Fix 4.0.1

This update fixes the Android resource-linking error in `runtimeTemplate/src/main/res/drawable/runtime_splash.xml`.

Previous invalid resource value:

```xml
<item android:drawable="#EEF0FF" />
```

`android:drawable` requires a drawable resource reference, not a literal color. The corrected file uses a `<shape>` drawable with a `<solid android:color="#EEF0FF" />` background and keeps the centered app icon.

## Apply

Extract this source archive over the existing `WEB2APK-V4-Internal` project, or replace the project with this archive. Then run:

```bash
cd ~/WEB2APK-V4-Internal
bash ./build-v4.sh
```

This is a source correction for the reported AAPT resource-linking failure. It has not been built on the target Android/Termux device in this environment; report any subsequent Gradle error for the next targeted fix.
