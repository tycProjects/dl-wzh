# Android Island 1.11.4 — AI screenshot composer, reliable clear, and file picker repair

## Changes
- Change the screen-analysis action from immediate auto-send to an explicit screenshot capture. The screenshot is staged as an image attachment; the user then writes their own instruction and sends it.
- Capture the foreground application window through Accessibility screenshot APIs. Prefer `takeScreenshotOfWindow` on API 34+ to avoid capturing the Android Island overlay itself.
- Downscale screenshot images to a maximum 1024 px edge and JPEG-compress before Base64 staging to limit RAM and request size.
- Attach available accessibility screen context as supporting prompt context without replacing the actual screenshot.
- Keep the screenshot/file attachment chips next to the composer and convert the action row to horizontal scrolling so it cannot wrap and push the input field off-screen.
- Fix conversation deletion: clear visible messages and pending attachments immediately, invalidate in-flight requests, serialize history persistence/deletion, atomically replace history snapshots, and prevent old callbacks from recreating a cleared conversation.
- Fix the SAF file picker flow: remove `noHistory` from the proxy activity, keep it alive while the system picker is shown, retain the returned URI grant when supported, and finish only after file staging completes.
- Bump app version to 1.11.4 (versionCode 125).

## Verification
- Source-level checks completed for manifest, accessibility screenshot capability, screenshot staging path, conversation generation guards, and file picker lifecycle.
- Gradle build and unit tests could not be run in this environment: Gradle 8.9 distribution download failed with `UnknownHostException: services.gradle.org`. APK compilation therefore remains unverified and must be run in Newly/Android Studio.
- Screenshot capture requires Accessibility to be enabled and may fail for secure/protected windows or if Android reports a screenshot capture error.
