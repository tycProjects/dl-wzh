# Testing report

**Build: NOT performed. Device/runtime tests: NOT performed.** The authoring environment had no Android SDK, Gradle cache or network.

Done:
- XML resources parse; Kotlin brace/paren balance checked per file.
- JNI contract recovered from Imagine's bytecode (RGBA in, ARGB out, scale derived from length, `onProgress(F)V` sink, `loadModel(param, bin, scale, prepadding, 0)`) and exported symbols confirmed against `libengine_jni.so`.

NOT done (please run): `./gradlew assembleDebug`; run each ncnn model on a real device (the `processImage` byte layout is inferred from bytecode, not executed); Replicate validate/enhance with a real key; theme switching; save formats; image-quality comparison at 100/200% zoom and file-size comparison (no test photographs or device were available).

Known limits:
- GPEN face model and Smooth-video replacement files were not in the archives; the Add/Replace buttons only work with files you supply (existing Tyce ONNX behaviour).
- ncnn models can't be user-replaced (needs param+bin+scale); only the bundled five are selectable.
- Instagram URL is empty; set `AppConfig.INSTAGRAM_URL`.
- Max ncnn input is capped at 4 MP in code (original limit unknown).
- Light/Dark colors are applied via the existing palette; some fixed decorative colors (demo hero art, share-target brand colors) are intentionally unchanged.

## 2.0.1 additions (also unbuilt/untested on device)
- ncnn import validation path (magic number check, real engine load, 24x24 test inference, scale check) is untested.
- Workspace screen and import dialog are untested visually.
- GPEN face model: not provided anywhere, cannot be bundled.
