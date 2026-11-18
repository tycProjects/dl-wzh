# VEXORA Studio Android starter — v0.2.0

Open the `vexora` folder in Android Studio and build an APK.

## Added
- Animated VEXORA splash screen and responsive dark studio UI
- Animated button interactions and tabbed inspector
- Media/asset import and multi-layer timeline
- Layer selection and basic grouping
- Group preview snapshot cache (not a full video pre-render)
- Text layers with Fade, Pop, Slide, Glitch, Typewriter, Bounce, Cinematic Zoom and Float presets
- 3D-style primitive stage with rotating cube, sphere and pyramid
- Project export (.vxproj/JSON) and cached preview PNG export
- Frame stepping for video preview

## Important limitations
This is a prototype, not a full replacement for After Effects, Alight Motion, Node Video, Blurrr or Blender. Full timeline video encoding, true GPU 3D mesh editing/rendering, true 3D conversion of arbitrary video, and importing/executing proprietary AE/AM/Node Video projects or native third-party plugins are not implemented. Imported 3D model files are accepted as project assets but are not yet rendered as editable meshes. Pre-render currently caches a preview snapshot for a group, not a complete video proxy.

## Build
1. Install Android Studio.
2. Open this `vexora` project folder.
3. Allow Gradle sync.
4. Build > Build Bundle(s) / APK(s) > Build APK(s).

Package: `com.vexora.studio`; minimum Android: API 26 (Android 8.0).


## VEXORA Studio 0.3.0 update

This update adds a Performance & Stability panel to the existing mobile-first editor prototype:
- Preview quality choices (Performance, Balanced, High quality).
- Preview target frame-rate choices (15, 30, 60 FPS); actual playback depends on device/browser/media.
- Local recovery snapshot action and a page-hide recovery snapshot attempt.
- Preview/group cache clearing control.
- Performance status messaging that explains browser/GPU dependencies.

### Important implementation notes
This is still the Android Studio starter project supplied for VEXORA 0.2.0, updated to 0.3.0. The performance panel is a working prototype control surface, not a native GPU profiler or a guarantee of export recovery. It does not yet implement a native rendering engine, hardware-specific GPU optimization, guaranteed autosave restoration, or full-resolution export recovery. The app's current web-based preview remains subject to Android WebView and device limits.

The ZIP is an Android project. It is not an APK, and it is not an iOS/Xcode project. iOS support requires a separate iOS app target and platform-specific build/signing work.


## VEXORA Studio 0.3.1 — Landscape orientation
- The Android main activity is locked to landscape orientation.
- Added a small landscape-responsive CSS rule to the web editor.
- This changes the Android app orientation; it does not create a separate iOS build.
