# WEB2APK V4.7 — Startup Loading & Creator Credit

- Moved Made By Tg - @Hop3_1 to the top of the Android Permissions card; clearer gray text and the existing ON-by-default generated-app watermark switch are retained.
- The generated-app watermark is now readable neutral gray.
- Added startup loading modes: Off, Custom image/video, Native, iOS Zoom, Slide Away and Pop.
- Added Slow Smooth, Normal Smooth and Fast animation speeds.
- Startup style and speed are remembered in the builder UI.
- Custom media is copied into the generated APK (30 MB limit); custom video is muted and loops only while startup is visible.
- Startup overlay closes after the first page load; Off bypasses it.
- Existing AI default OFF, Code Studio, ZIP import, permissions, icon picker, signing and history code paths are retained.

Build using the existing `bash ./build-v4.sh` workflow. Android build must still be verified on the target Termux/SDK installation.
