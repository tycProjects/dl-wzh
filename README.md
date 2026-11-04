# CAM STREAM MOBILE

Native Android Studio project using **Java + Android XML**. No HTML, CSS, JavaScript, WebView, cloud backend, or GitHub account is required.

## Implemented in this project
- Native home, settings, library, player, camera preview, and about screens.
- Real Android MediaProjection consent flow and foreground screen-recording service.
- MP4 screen recording through Android MediaRecorder surface input.
- Local app-private recording storage.
- Video playback, sharing, and delete confirmation.
- CameraX front/rear camera preview.
- Overlay permission workflow and draggable floating STOP/HIDE panel.
- Locally persisted capture profile preferences.

## Important scope and device limitations
This is a functional starter release, **not yet a fully validated production recorder**. The recording engine currently uses Android `MediaRecorder` (not a custom `MediaCodec`/`MediaMuxer` pipeline), captures the device display at 30 FPS using the current display dimensions, and does not yet implement pause/resume, audio capture, countdown, face-camera compositing over gameplay, full floating REC/PAUSE/RESUME/SHOW controls, rename, thumbnail metadata, or the requested 90/120 FPS and resolution negotiation. The corresponding settings currently store user choices but do not all alter the encoder. The camera screen is a real preview screen, not a facecam overlay embedded in the recorded video.

Android restricts internal playback audio capture: it is not generally available for arbitrary apps and depends on Android version, source-app capture policy, and user consent. Overlay windows require explicit special access. OEM behavior varies.

## Build
1. Open this folder in Android Studio.
2. Use JDK 17 and Android Gradle Plugin 8.7.3 with Gradle 8.9.
3. Install Android SDK Platform 35 and allow Gradle to download the declared AndroidX/CameraX dependencies.
4. Build and install on a physical Android device running Android 8.0 or newer.
5. Press Record and approve Android's screen-capture prompt.

The source ZIP does not bundle Gradle Wrapper scripts/JAR. Android Studio can use its configured Gradle installation, or generate a wrapper from a local Gradle 8.9 installation. See `BUILD_DIAGNOSIS.md` for the repair notes and validation limits.

A physical-device test is required before relying on long recordings. The generated ZIP has not been built or tested on a physical device in this environment.
