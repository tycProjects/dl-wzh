# Screen Recorder V2 — locked-60 / CBR build

This version replaces `MediaRecorder` with a direct `MediaCodec` + `MediaMuxer`
pipeline.

## Recording pipeline

- Native/even display dimensions (no arbitrary 1088 scaling)
- H.264 hardware encoder
- 60 FPS encoder target
- 24 Mbps constant-bitrate (CBR) request
- 2-second keyframe interval
- Encoder operating-rate request of 60 FPS
- On Android 11+, the encoder input surface is explicitly marked as a fixed 60 FPS source
- MediaProjection → VirtualDisplay → encoder Surface
- MediaMuxer writes MP4 directly
- Existing MediaStore output and protected-content behavior are retained

## Important Android limitation

Android does not expose a universal API that can force every phone/display pipeline
to physically deliver exactly 60 frames per second. V2 therefore makes the strongest
supported requests to the system: 60 FPS surface pacing, encoder operating rate,
60 FPS format, and CBR. The hardware/vendor encoder can still drop or pace frames
under thermal/load conditions.

Protected/secure content can still appear black. The app does not bypass Android's
secure-content protection.

## Build

Open the project root in Android Studio and sync Gradle. Compile/target SDK 34,
AGP 8.5.0, Kotlin 1.9.24, Java 17.
