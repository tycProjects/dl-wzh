# WEB2APK V4.8.0 — Generated App Controls

This update is built on top of V4.7.0 and keeps the existing AI (OFF by default), Code Studio, HTML/ZIP import, permissions, icon picker, creator credit, startup loading presets, custom splash media, speed options, local signing and build history.

## New APK options
- Screen orientation: System / Auto, Portrait, Landscape.
- Full screen / immersive navigation bars.
- Pull down to refresh the current website.
- Android Back: website history, confirm before exit, or exit immediately.
- Open off-site HTTP(S), email and SMS links in compatible external apps/browser. Same website host remains inside the app.
- WebView cache: Normal, Cache first, Cache only (offline), or No cache. Cache-only depends on previously cached resources and is not a guarantee of full offline operation.
- Pinch to zoom, Desktop website user-agent mode, and downloads ON/OFF.

## Build
```bash
cd ~/WEB2APK-V4-Internal
bash ./build-v4.sh
```

Android build result still needs to be confirmed on the target Termux installation. Static source checks do not guarantee a successful APK build on every device/SDK combination.
