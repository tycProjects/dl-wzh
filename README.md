# Ziko TV — Movies Edition

Android Studio project built with Kotlin, Jetpack Compose and AndroidX Media3 / ExoPlayer.

## What changed
- The **Channels** section is intentionally blank. It shows only the centered app logo and section name; no channel cards, list, or channel asset is loaded by the UI.
- Added **Movies** as the active content section, with a centered logo/title, updated dark glass styling, and an animated startup screen.
- Imported the supplied `all_movies.m3u8` playlist as `app/src/main/assets/all_movies.m3u8`.
- The source playlist parses to **33,680 movie entries**. Original stream URLs, movie titles, source groups, and available `tvg-logo` poster URLs are retained.
- Added the **All** collection plus collections such as Disney, Marvel, DC, Pixar, DreamWorks, Star Wars, Harry Potter, Anime, Netflix, Jurassic, Fast & Furious, Transformers, The Lord of the Rings, and James Bond when matches exist in the playlist. Remaining source groups are also available as English-labeled categories.
- Category covers use matching well-known titles when a candidate is present in that category; otherwise the first available supplied poster is used.
- Search looks up movie titles across the complete playlist. Movie cards display the full supplied title and launch the selected URL in the built-in Media3 player.
- The parser handles records whose `#EXTINF` metadata or display titles wrap across multiple lines.

## Posters and streams
The source playlist provides poster URLs for **32,908** entries; **772** entries have no poster URL, so the UI displays a styled title-based fallback for those entries (or when a remote poster cannot be loaded). Poster URLs are loaded over the network and therefore depend on the image host being reachable. The stream URLs are preserved from the supplied playlist; this project does not guarantee that providers remain online or authorize access.

## Internal player
- Channels and movies use the existing AndroidX Media3 / ExoPlayer player activity.
- Supports HLS (`.m3u8`), DASH (`.mpd`) and progressive streams such as MP4/TS subject to device and provider support.
- HTTP/HTTPS streams are permitted by the manifest.

## Build
Open the `ZikoTV` folder in Android Studio, run **Sync Project with Gradle Files**, then build the app. The Gradle wrapper distribution must be available to the build environment.
