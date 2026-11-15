# Android Island 1.4.19 — Akira 1.6.5 overlay architecture port (initial integration)

## Implemented in this source snapshot
- Renderer window uses `MATCH_PARENT` width and `WRAP_CONTENT` height; Compose scene is centered inside the stable-width drawing surface.
- Independent transparent touch window tracks the measured interaction footprint and forwards `MotionEvent`s to the renderer using window-coordinate offsets.
- Touch bounds updates are debounced (48 ms after layout changes; 250 ms for state/visibility changes) to avoid moving the touch window during the morph.
- Hidden state disposes the Compose composition and both windows; renderer start/stop events are recorded.
- Diagnostic events are mirrored to Android Logcat under tag `AndroidIsland` and retained in the in-app bounded log buffer.
- Additional logs include settings emission cadence/fingerprint, drawing layout, touch forwarding, touch bounds updates/failures, renderer lifecycle and outside touches.
- Existing Shizuku classes and status-bar control implementation are retained unchanged.

## Reference
Akira Dynamic Island 1.6.5 source supplied by the user. This is a port of the overlay-window pattern, not a wholesale replacement of Android Island features or UI.

## Verification status
- ZIP integrity and source-level structural checks: performed.
- Gradle compile: NOT VERIFIED. The wrapper requires Gradle 8.9 from services.gradle.org; this environment cannot resolve that host (UnknownHostException). Do not treat this archive as a confirmed build until `./gradlew assembleDebug` succeeds in an Android/Gradle environment.

## Runtime checks still required on device
- Confirm touch coordinates across rotation, cutout offsets, expanded card and WhatsApp banner.
- Confirm quick-reply focus behavior and outside-touch dismissal.
- Confirm touch-window settle timing against actual animation duration on target Samsung firmware.
- Confirm Shizuku status-bar icon/Now Bar behavior remains unchanged on device.
