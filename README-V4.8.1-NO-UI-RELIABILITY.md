# WEB2APK V4.8.1 — No-UI Reliability Update

This patch deliberately keeps the existing V4.8.0 interface unchanged. The builder HTML, CSS, page layout, visual styles, labels, and current controls are preserved. Existing Startup Loading, animation speeds, Permissions, Icon Picker, creator watermark, Code Studio, Gemini AI (OFF by default), generated-app controls, local APK signing, and Build History remain in place.

## Reliability and safety changes

- Added a second validation layer inside the internal APK builder for app name, package ID, version, source mode, and HTTP/HTTPS URLs.
- Empty HTML files and empty ZIP `index.html` files are rejected with a clear error before APK packaging.
- ZIP import rejects duplicate file paths as well as unsafe paths, and retains the existing entry-count and expanded-size limits.
- The generated APK is checked as a ZIP/APK before it is saved. Required manifest, DEX, runtime configuration, and website source entries are checked.
- The signed APK is verified with Android apksig before it can appear as a successful build.
- APK output is staged and verified before final save. Existing builds are not deleted when the same app/version is built again; a unique `_build2`, `_build3`, etc. name is used.
- Build History is capped at the newest 100 stored entries (the Projects page shows the newest 50), and Install/Share/Save actions only accept APK paths inside WEB2APK's own output folders.
- Code Studio creates a best-effort safety snapshot at most once every five minutes per project. It keeps the newest 10 snapshots per project and skips snapshots larger than 30 MB so a backup problem does not block editing or saving.
- Optional native bridge actions in generated apps are restricted to the selected website's top-level origin or the appassets origin. Off-origin top-level pages cannot invoke the app's optional phone/contact/calendar/vibration/AI settings bridge. Camera/microphone and geolocation permission prompts are also denied for off-origin frames/pages. A normal same-host HTTP-to-HTTPS upgrade remains trusted.
- The missing `java.io.IOException` import required by the startup splash loader is present.

## Existing options preserved

- Startup Loading: Off, Custom, Native, iOS Zoom, Slide Away, Pop.
- Startup animation: Slow Smooth, Normal Smooth, Fast.
- Orientation, Fullscreen, Pull-to-refresh, Back behavior, external links, cache mode, pinch zoom, desktop mode, and downloads.
- HTML/ZIP import, Code Studio, custom launcher icon, permissions, creator watermark, AI API settings and optional generated-app AI.
- Local signing, APK install, share, export, and build history.

## Build

```bash
cd ~/WEB2APK-V4-Internal
bash ./build-v4.sh
```

The installer package version is 4.8.1 (versionCode 20). The in-app visual branding remains at the existing V4.8 design as requested.

## Verification note

Static checks cover ZIP integrity, project structure, JavaScript syntax, XML parsing, and shell syntax. The full Android Gradle build still needs to run in the user's Termux SDK environment; static checks cannot guarantee that an Android build or every generated app will run on every device.
