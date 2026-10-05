# Smooth Camera

Production-oriented Android camera application built with Kotlin, CameraX 1.6.2 and AndroidX. The UI is an original premium, minimal camera design inspired by the simplicity of modern smartphone cameras; it does not use Apple's proprietary assets or implementation.

## Open
1. Extract `SmoothCamera.zip`.
2. Open the `SmoothCamera` folder in Android Studio.
3. Let Gradle sync and allow Android Studio to download the pinned SDK/dependencies if necessary.
4. Run the `app` configuration on a physical camera device or emulator with a camera.

## Build
From the project root:

```bash
./gradlew clean
./gradlew assembleDebug
```

Windows:

```bat
gradlew.bat clean
gradlew.bat assembleDebug
```

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`.

## Install
Use Android Studio's Run button, or:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Platform/tooling
- Kotlin: 2.4.10 plugin compatible with the selected Android Gradle Plugin
- Android Gradle Plugin: 9.4.0
- Gradle wrapper: 9.6.0
- Compile/target SDK: 37
- Minimum SDK: 23
- CameraX: 1.6.2
- Java/Kotlin JVM target: 17

## Camera features
- Rear/front camera switching
- Fast CameraX preview
- Hardware capability detection
- 24/30/60 FPS detection through CameraX session frame-rate ranges
- High-speed/slow-motion recording through CameraX `HighSpeedVideoSessionConfig` when supported
- 1080p/4K quality selection through CameraX `QualitySelector` when supported
- H.264 is the safe baseline; CameraX/MediaCodec chooses compatible hardware encoders
- Auto/on/off flash
- Tap-to-focus and auto-exposure metering
- Pinch zoom and double-tap 2x zoom
- MediaStore photo/video saving
- Optional audio for video
- Original Natural/Vibrant/Warm/Cool/Cinematic/Soft/Neutral photo profiles
- Capability-aware HDR/stabilization reporting hooks
- Lifecycle-aware camera binding and cleanup

## FPS detection
The app queries CameraX `CameraInfo.getSupportedFrameRateRanges()` for the exact session configuration. Preview prefers the highest fixed rate at or below 60 FPS. Normal video requests 60 FPS and falls back to the highest supported fixed rate at or below 60. Slow motion uses CameraX high-speed recording and selects the highest supported high-speed range, with 120 FPS preferred when the device reports it.

The app never shows 60/120 FPS merely because the device is expected to support it. A reported capability is required before selecting that mode.

## Known hardware limitations
Camera hardware and vendor encoder capabilities differ substantially. Some devices expose 60/120 FPS only at reduced resolutions, some require high-speed session constraints, and some cannot combine high FPS with stabilization, HDR, or certain lenses. CameraX is therefore allowed to choose a safe supported configuration and the app reports/falls back rather than forcing an invalid combination.

The color profiles are an original lightweight post-capture look, not a recreation of Apple's proprietary ISP pipeline. Non-Natural profiles are applied off the UI thread after capture to avoid adding latency to the shutter action.

## Permissions
Camera permission is requested at startup because the preview cannot exist without it. Microphone permission is requested only when the user enters recording and audio is needed. MediaStore is used for gallery output; no broad storage permission is requested.
