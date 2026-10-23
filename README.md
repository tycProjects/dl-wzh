# Alaoke 🎤

**Don't Just Sing. Own the Stage.**

A modern, Platinum-style videoke (karaoke) player for Android phones, tablets, and Android TV —
built with Kotlin, Jetpack Compose, Material 3, and Room.

## Features

- **Song Library** — browse/search a starter library, favorite songs, see recently played.
- **YouTube Search** — search karaoke videos via the official YouTube Data API v3, and play them
  using the official **YouTube IFrame Player API** (embedded via WebView). The app never downloads
  or extracts YouTube video/audio — playback always happens inside YouTube's own player.
- **Party Queue** — add songs with a singer's name, reorder, skip, and remove — with a "Now
  Playing" / "Up Next" view.
- **Smart Party Mode** — pick an occasion (🎂 Birthday, 💍 Wedding, 🪩 Party, 🎄 Christmas, 🎆 New
  Year) to re-theme the app and get matching song suggestions.
- **Karaoke Player** — full-screen video/lyrics display, play/pause, next, volume, fullscreen —
  built to work well on TV remotes as well as touch.

## Project structure

```
Alaoke/
├── app/
│   ├── build.gradle.kts          # dependencies, YOUTUBE_API_KEY build config field
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/alaoke/app/
│       │   ├── MainActivity.kt
│       │   ├── AlaokeApp.kt          # Application class, wires up the repository
│       │   ├── data/                 # Room entities, DAOs, database, repository, seed data
│       │   ├── youtube/               # YouTube Data API client + IFrame Player WebView wrapper
│       │   └── ui/
│       │       ├── theme/            # Colors, type, dark Material 3 theme, Smart Party accents
│       │       ├── navigation/       # NavHost + adaptive bottom bar / side rail
│       │       ├── screens/           # Home, Songs, YouTube Search, Queue, Smart Party, Player, Settings
│       │       ├── components/        # Reusable song row, thumbnail, add-to-queue dialog
│       │       └── AlaokeViewModel.kt
│       └── res/
├── build.gradle.kts
└── settings.gradle.kts
```

## Setup

### 1. Open in Android Studio

Open the `Alaoke/` folder as a project in a recent version of Android Studio (Koala or newer
recommended). Let it sync Gradle — an internet connection is required the first time to download
dependencies.

### 2. Add your YouTube Data API v3 key

YouTube Search requires a free API key from Google Cloud Console:

1. Go to <https://console.cloud.google.com/>, create/select a project.
2. Enable **"YouTube Data API v3"** under APIs & Services.
3. Create an API key under **Credentials**.
4. Add it to your **local, un-committed** `local.properties` (or a `gradle.properties` in your
   Gradle user home, `~/.gradle/gradle.properties`) as:

   ```properties
   YOUTUBE_API_KEY=your_real_api_key_here
   ```

   `app/build.gradle.kts` reads this as a Gradle project property and exposes it to the app as
   `BuildConfig.YOUTUBE_API_KEY`. Never commit your real key to source control.

Until a key is configured, the Settings screen will show a warning and YouTube Search will show a
friendly error instead of crashing.

### 3. Run it

Run the `app` configuration on an emulator, a physical phone/tablet, or an Android TV device/
emulator (the manifest declares a Leanback launcher entry point for TV).

## Notes on YouTube usage

Per YouTube's Terms of Service, this app:
- Only uses the **YouTube Data API v3** for search (metadata only — titles, channels, thumbnails,
  video IDs).
- Only plays videos through the **official YouTube IFrame Player API**, embedded in a WebView.
- Never downloads, caches, re-hosts, or extracts audio/video from YouTube.

## Tech stack

- Kotlin + Jetpack Compose + Material 3
- Navigation Compose (adaptive: bottom bar on phones, `NavigationRail` on tablets/TV)
- Room (Favorites, History, Singers, Party Queue)
- Retrofit + Gson (YouTube Data API v3 search)
- Coil (thumbnail image loading)
- WebView + YouTube IFrame Player API (playback)

## Known limitations / good next steps

- Drag-to-reorder in the Party Queue currently uses up/down buttons rather than free drag; swap in
  a drag-and-drop `LazyColumn` implementation if you want true drag handles.
- The song library ships with a small starter list of well-known titles (no bundled audio/video);
  tapping a library song without a linked YouTube video routes you to YouTube Search.
- No user accounts/sync — all data (favorites, history, queue, singers) is local to the device via
  Room.
