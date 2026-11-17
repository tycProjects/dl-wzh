# WEB2APK V4.8.2 — Studio feature update

This update preserves the existing dashboard and overall page structure. The bottom navigation receives a floating animated capsule treatment; the supplied reference GIF could not be retrieved from its host in the authoring environment, so the navigation treatment is provisional rather than a verified pixel-match.

## Added and connected capabilities

- Project Manager: lists local Code Studio workspaces and supports opening, creating, renaming, duplicating and deleting projects. Existing ZIP export/import paths are used for backup and restore. Project switching saves the active editor draft first.
- Advanced Icon Studio: image picker accepts image media; PNG/JPG/WebP decoding, center crop/zoom, background color, square/rounded/circle raster masks and adaptive/round/legacy preview samples are provided. The preview labels describe visual masks; the output remains a 512px raster launcher icon rather than a separate Android adaptive foreground/background resource pair.
- Splash Screen Studio: configurable text, background color, progress indicator and 0–5 second minimum display time. Existing splash styles, custom image/video, and Slow/Normal/Fast animation timing remain.
- Code Studio: lightweight syntax highlighting overlay added without replacing the existing textarea editor. Existing HTML/CSS/JS tabs, save/autosave, find/replace, preview and code checks remain.
- WebView Pro: User-Agent mode (system/mobile/desktop/custom), JavaScript, DOM Storage, cookies and file upload toggles are packaged into the runtime config. Existing cache, download, external-link, zoom, orientation and navigation controls remain.
- Permission Manager: existing permission selection is retained.
- Performance Monitor: optional generated-app badge shows app/runtime version, WebView version, uptime, last top-level page load duration, and main-frame load/HTTP errors.
- Release Manager: Release is default; Debug sets `android:debuggable=true`. Both APK modes are still signed by the existing local Android Keystore identity. Package name and version fields remain in Create.
- APK Share Center: existing Install, Share and Export actions are retained; Rename and Copy actions are added to build history.

## Important limitations

- The existing signing private key is generated/stored in Android Keystore and is non-exportable. This update deliberately does not replace it with a different key: doing so could break the ability to update already-installed generated APKs. A certificate export alone is not a restorable keystore backup.
- With no hosting server/domain, a QR code cannot be a public download link to a locally stored APK on another device. Share/Export transfers the actual APK file through Android's system share/document flows.
- The supplied navigation GIF host was not reachable from the authoring environment. Upload the GIF directly in chat to reproduce its exact animation; current nav styling is an approximate floating capsule.
- Android SDK/Gradle build and runtime behavior must be verified on the target Termux device. Static checks are not a substitute for a device build.

## Build

```bash
cd ~/WEB2APK-V4-Internal
bash ./build-v4.sh
```
