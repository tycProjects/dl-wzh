# Ziko TV

Android Studio project built with Kotlin, Jetpack Compose and AndroidX Media3 / ExoPlayer.

## App sections
- One main content section: **Channels**.
- Channel groups use English labels, with known providers consolidated into a single group (including beIN Sports & Entertainment, DAZN, MBC & Rotana, Netflix, Sky, OSN, Shahid, Disney+, Amazon Prime Video, and others).
- The former 18 TOD movie-list entries are now part of **TOD Entertainment** inside Channels. They use the TOD logo supplied by the playlist and neutral English labels `TOD 1` through `TOD 18`, because the playlist does not contain real movie titles or individual posters.
- Each company/group appears as a card with a logo drawn from its available channel logos. Opening it shows its channels, individual logos when provided, and an in-app play action.
- Search is available for groups and for channels within a selected group.

## Channel library
- The bundled `app/src/main/assets/all_channels.m3u8` contains all **14,553** entries from the supplied channel playlist plus the **18** TOD entries from the earlier movie playlist (**14,571 total**), organized into **322** normalized groups.
- Original channel stream URLs and supplied channel logos are preserved. Where an entry has no logo in the supplied source, the UI falls back to that group's available logo or a text-based placeholder instead of inventing an image.
- Replace `all_channels.m3u8` to update the channel list. Each item follows the M3U `#EXTINF` plus stream URL format.

## Internal player
- Channels launch the built-in AndroidX Media3 / ExoPlayer player.
- Supports HLS (`.m3u8`), DASH (`.mpd`) and progressive streams such as MP4/TS subject to device and provider support.
- HTTP/HTTPS streams are permitted by the manifest. Some stream sources may be expired, region-restricted, or require provider authorization; the app cannot guarantee each external URL is live.

## Build
Open the `ZikoTV` folder in Android Studio and use **Sync Project with Gradle Files**, then build the app. The Gradle wrapper distribution must be available to the build environment.
