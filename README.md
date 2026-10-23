# Anime Tracker for Android

Native Kotlin + Jetpack Compose app with anime search, a persistent watchlist, episode progress, watch statuses, and removal.

## Run it
1. Install Android Studio on a computer and extract this ZIP.
2. Open the `AnimeTrackerAndroid` folder in Android Studio.
3. Let Gradle sync. Install Android SDK 35 if prompted and use JDK 17.
4. Connect an Android phone with USB debugging enabled, or launch an emulator, then press **Run**.
5. To create an APK, use **Build > Build Bundle(s) / APK(s) > Build APK(s)**. Usually it appears at `app/build/outputs/apk/debug/app-debug.apk`.

## Features
- Search catalogue through the public Jikan API (`https://api.jikan.moe/v4/`).
- Add and remove anime from a watchlist.
- Increment/decrement watched episodes and view progress.
- Status options: Plan to Watch, Watching, Completed, On Hold, Dropped.
- Saves the list locally on the device with SharedPreferences.

Search requires internet and may be rate-limited by Jikan. This ZIP is Android Studio source, not a precompiled APK. Anime data is provided by Jikan; this app is unofficial and not affiliated with MyAnimeList.
