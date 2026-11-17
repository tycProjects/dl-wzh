# WEB2APK V4.3 — Icon, Theme and Creator Branding Update

This source update keeps the on-device APK packager and updates:

- Launcher icon replacement: patch all compiled `res/drawable*/app_icon.png` and `res/mipmap*/app_icon.png` entries directly in the unsigned APK ZIP after ARSCLib writes it, then verify the CRC against the selected Gallery image.
- Runtime template declares the launcher icon as `@mipmap/app_icon` and carries both drawable and mipmap resource copies.
- Permission selector: collapsed single-row `Permissions you want` control with smooth expansion.
- Dark mode: pure black background, white text, softly translucent panels, coloured accents, and animated SVG sun/moon-with-stars icon.
- Creator watermark: enabled by default; switch in Settings. When enabled, generated website APKs show a very faint `Made By Tg - @Hop3_1` mark at the bottom. When disabled, it is not inserted.
- No Termux RUN_COMMAND permission and no Termux invocation from the installed builder app.

## Build WEB2APK installer

This is a source project. On the development phone, use the existing local Android SDK and run:

```bash
cd ~/WEB2APK-V4-Internal
bash ./build-v4.sh
```

The installed WEB2APK app builds website wrapper APKs internally. The one-time installation build of WEB2APK itself still uses Gradle/Android SDK in the developer environment.

## Honest limitations

Icon replacement is verified against the selected PNG bytes in the output APK. Device-side Gradle build and real OEM launcher rendering must still be checked on the target phone. Android protected permissions (SMS/call log/root/all-files access) are not falsely enabled. `FLAG_SECURE` blocks normal screenshots/non-secure capture but cannot block an external camera.
