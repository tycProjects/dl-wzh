# Phone Guardian Pro

Android source project for `com.adam.phoneguardian`.

## Included
- Jetpack Compose + Material 3 interface (Arabic-first RTL through Android locale direction).
- Installed app list and requested-sensitive-permission review.
- Open Android App Info settings for permission management.
- Local heuristic URL indicators (not a reputation database or malware verdict).
- User-selected folder scan for files at least 50 MB, with explicit delete confirmation using Android's Storage Access Framework.

## Build
1. Install Android Studio (current stable) and Android SDK Platform 35.
2. Open this folder in Android Studio.
3. Allow Gradle sync; use JDK 17.
4. Build > Build APK(s).
5. Install the generated debug APK on a device running Android 10 or newer.

## Important limitations
- This source has not been built or device-tested in this environment because Android SDK/Gradle are not installed here. Do not treat it as a verified APK.
- `QUERY_ALL_PACKAGES` is used for a broad installed-app inventory. Google Play restricts this permission; if publishing, justify it under Play policy or replace with a limited package visibility query.
- Requested permissions are indicators for review, not proof of malware. The app does not claim to detect all malware.
- Link scanning is local and heuristic. It does not check live threat intelligence and cannot guarantee a URL is safe.
- Cleaner only analyzes the folder selected by the user and only deletes a selected file after confirmation. It cannot clean private caches belonging to other apps.
- Android does not expose every privacy permission or security state to ordinary apps. Some details require opening system settings.
