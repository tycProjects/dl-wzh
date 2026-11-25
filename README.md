# Live Video Wallpaper

Android app (Kotlin, Material 3, Media3 ExoPlayer) that plays a **silent, looping video** as a live wallpaper.
Import single videos or a ZIP of videos, preview them, then apply.

> **Build status:** the source was written and checked by reading, but it has **not been compiled yet**
> (the authoring sandbox had no access to the Android SDK / Google Maven). The first GitHub Actions run is the real test.
> If it fails, copy the red error lines from the Actions log and fix them (see Troubleshooting).

## Features
- Import a video (Storage Access Framework – no storage permission) or a ZIP with many videos
- Gallery with thumbnails, names, durations; full-screen muted preview
- Apply Home Screen / Lock Screen / Both
- Always muted (volume 0 and audio track disabled), loops with `REPEAT_MODE_ONE`
- Player exists only while the wallpaper surface exists, plays only while visible, released otherwise
- Settings: loop on/off, fill vs fit, pause in Battery Saver, battery guidance, storage usage, delete all

## New in 1.1.0 – Video Phone, call screen, video lock screen
- **Video Phone (dialer)**: keypad, recents, contacts with search, drawn over your own video or photo background. Has its own launcher icon ("Video Phone").
- **Call screen**: incoming / outgoing / ongoing calls with a video or photo background, pulsing premium Answer / Decline buttons (glossy or glass style), mute, speaker, hold, call timer. Shows above the lock screen.
- **Video lock screen**: works on phones such as Honor MagicOS where the system lock screen ignores live wallpapers. A small foreground service waits for the screen to turn on and opens a video screen above the lock screen; swipe up and Android asks for your PIN / fingerprint as usual.
- **Customize** screen: upload a photo or video for the dialer, the call screen and the lock screen (or pick one of your imported videos), reset to default, choose button style.

### One-time setup on the phone
1. Open **Video Phone** -> allow Contacts, Call log, Phone and Notifications.
2. Tap the blue banner (or Customize -> *Set as default phone app*) and choose this app. Only the default phone app can show the video call screen.
3. Customize -> turn on **Play video on my lock screen** -> allow **Display over other apps**.
4. Settings -> Apps -> this app -> Battery: *Unrestricted*; allow *Auto-launch* / *Run in background* (names differ on MagicOS). Without this the phone may kill the lock-screen service.

### Honest limits
- Not compiled in the authoring sandbox (no Android SDK / network). The first GitHub Actions run is the real test.
- The video lock screen is an app screen shown *above* the real Android lock screen, not the system wallpaper. If a phone blocks background launches, the lock video may not appear until the permissions in step 3-4 are granted.
- Call screen and dialer need the default-phone-app role. Emergency calls always use the system.
- Keypad has no touch tones, no in-call keypad, no call recording.

## Lock screen – honest limitations
Android has no API that lets an app force a *live* wallpaper onto the lock screen.
- **Apply Home Screen / Apply Both** open the system live-wallpaper preview. On Android 14+ the system dialog lets you choose Home, Lock or Both. On older phones the live wallpaper usually shows on both screens, but some brands (Samsung, Xiaomi, etc.) handle the lock screen with their own settings.
- **Apply Lock Screen** sets a *still frame* of the video as lock-screen wallpaper through `WallpaperManager` (`FLAG_LOCK`). This is supported widely, but it is a picture, not video.

## ZIP import safety
Only the file name part of each entry is used (no path traversal; entries containing `..` are rejected),
only video extensions (mp4, m4v, 3gp, webm, mkv) are extracted, limits are enforced while copying
(500 MB per file, 2 GB total, 500 entries), each file must open as a video or it is deleted, damaged archives
are reported, and nothing from a ZIP is ever executed.
Videos can be in any folder inside the ZIP (e.g. `wallpapers/nature.mp4` or just `nature.mp4`).

## Sample videos
No sample videos are bundled in this repository. Use your own ZIP of videos with **Import ZIP**.

## Build

Requirements for a local build: JDK 17, Android SDK platform 34, build-tools 34.0.0.

```
./gradlew --no-daemon assembleDebug
```
APK: `app/build/outputs/apk/debug/app-debug.apk`

### Recommended: build on GitHub Actions
1. Create an empty GitHub repository and push this project (see below).
2. The workflow `.github/workflows/build-apk.yml` runs on every push, or start it manually: **Actions → Build debug APK → Run workflow**.
3. When it is green, open the run and download the artifact **LiveVideoWallpaper-debug-apk** (a ZIP containing `app-debug.apk`).

### Upload this project to GitHub (from a computer)
```
git init -b main
git add .
git commit -m "Initial commit"
git remote add origin https://github.com/<YOUR-USER>/<YOUR-REPO>.git
git push -u origin main
```

## Termux

### Workflow A – GitHub Actions (recommended)
```
pkg update -y && pkg upgrade -y
pkg install -y git unzip gh
termux-setup-storage                      # allow storage access once

cd ~
unzip ~/storage/downloads/LiveVideoWallpaper-project.zip -d ~/
cd ~/LiveVideoWallpaper

git config --global user.name  "Your Name"
git config --global user.email "you@example.com"

git init -b main
git add .
git commit -m "Initial commit"

gh auth login                             # GitHub.com → HTTPS → login with a web browser (or paste a token)
gh repo create live-video-wallpaper --private --source=. --push
```
Trigger and follow the build, then download the APK:
```
gh workflow run build-apk.yml             # optional – a push already triggers it
gh run list --limit 3
gh run watch                              # pick the running build
gh run download --name LiveVideoWallpaper-debug-apk --dir ~/storage/downloads/
```
Install: open your **Downloads** folder in a file manager, tap `app-debug.apk`, allow “install unknown apps” for that file manager if asked.
(Or without GitHub CLI: open the repo on github.com → Actions → latest run → Artifacts → download.)

### Workflow B – local build in Termux (not recommended)
The official Android SDK build tools (aapt2, etc.) are x86-64 Linux binaries and do **not** run natively on
Termux (ARM). A full local build there needs unofficial ARM builds of the SDK tools or a proot-distro Linux with
x86 emulation, which is slow and fragile. Use Workflow A, or build on a PC with Android Studio / the command above.

## Troubleshooting
| Problem | Fix |
|---|---|
| `SDK location not found` | Install Android SDK and set `ANDROID_HOME`, or create `local.properties` with `sdk.dir=/path/to/sdk` (not needed on GitHub Actions). |
| `Unsupported class file major version` / JDK errors | Use JDK 17 (`java -version`). |
| `Failed to find Build Tools revision 34.0.0` or platform 34 missing | Install with `sdkmanager "platforms;android-34" "build-tools;34.0.0"`. |
| `Permission denied: ./gradlew` | `chmod +x gradlew` (the workflow already does this). |
| Actions: `Package 'tools' not found` | Do not list `tools` in the SDK step; this workflow installs `platform-tools`, `platforms;android-34`, `build-tools;34.0.0`. |
| Actions: APK artifact missing | The build step failed earlier; open the first red step in the log. |
| Wallpaper does not play after reboot / stops | Settings → Battery → set the app to Unrestricted; make sure Battery Saver is off or disable “Pause video in Battery Saver mode”. |
| Install blocked | Allow “Install unknown apps” for the app you opened the APK with. |
| Debug APK only | The APK is signed with the debug key, fine for personal use. |

## New in 1.2.0 – Status bar & battery
- **Status bar** drawn on the video lock screen: time, animated notification icons, live network speed (KB/s), signal bars, Wi-Fi and a battery.
- **Battery** turns **green** while charging (with a moving shine and bolt) and stays **black** when not charging. Two styles: *Classic outline* and *Pill (solid)*.
- Settings: Customize -> **Status bar & battery style** (live preview, show/hide, notification animation, battery style, preview-as-charging).
- Honest limits: Android does not let a live wallpaper draw over the real system status bar on the home screen, so the bar is shown on the video lock screen. Signal bars show connected / not connected, not exact strength (exact strength needs extra permissions). Real notification icons would need Notification access, so the notification icons are an animated visual only.
