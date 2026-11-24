# Alter Enhancer (Android)

> v2.0 integration build: **not yet compiled or device-tested** (see TESTING.md, CHANGELOG.md). Build with `./gradlew assembleDebug` (JDK 17, Android SDK 34).

Architecture additions: `ModelCatalog` / `NcnnUpscaler` (ncnn via `com.kaminari.imagine.Engine`), `Cloud.kt` (provider registry), `SecureKeyStore`, `Appearance`, `OutputEncoder`, `AppConfig`/`Links`, `Sheets.kt`.


On-device photo upscaler using Real-ESRGAN + ONNX Runtime. Free to run, no server, photos never leave the phone.

## Setup

1. Install **Android Studio** (free) on a PC/laptop.
2. (Already done: the **HD** model, realesr-general-x4v3, is bundled in `app/src/main/assets/realesrgan.onnx`, so the app works right away. To use a different model, replace that file or use "Replace model" in the app.)
   Download a Real-ESRGAN **ONNX** model, for example from
   https://huggingface.co/anakhiu/realesrgan-onnx
   (or convert the official weights from
   https://github.com/xinntao/Real-ESRGAN yourself).
   Smaller models (like the "general x4v3" one) run much faster on phones.
3. Rename the file to `realesrgan.onnx` and put it in:
   `app/src/main/assets/realesrgan.onnx`
4. Open this folder in Android Studio, let Gradle sync.
5. Plug in your phone (USB debugging on) and press Run.
   Or use Build > Build APK(s) to get an installable APK.

## Design

Dark navy UI with a violet-to-cyan gradient, in four screens:

1. **Home**: pick *Ultra HD* (2x, smaller) or *AI Ultra HD* (4x, largest), optionally a
   *Scenario* (a light tone tweak applied to the result), then **Try Now** to choose a photo.
   *History* shows saved photos; *Model* opens model and speed settings.
2. **Processing**: live percent. **View Later** sends it to the background (a banner on Home
   shows progress and opens the result), **Cancel** or the X stops it.
3. **Result**: drag the line to compare, or use the Before / Before & After / After tiles.
   **Save** writes to Pictures/AIEnhancer.
4. **Saved to album** sheet: share to Instagram, WhatsApp, TikTok, LINE or any app,
   **Continue editing**, or the home button.

## No model bundled? Use "Load model"

If you see `Failed: realesrgan.onnx`, the model is not inside the APK.
Open the app, tap **Load model (.onnx)** and pick your Real-ESRGAN file
from the phone. It is copied into the app and remembered, so no rebuild
is needed.

## How it works

- `Enhancer.kt` splits the photo into tiles, runs the model on each tile,
  and stitches the results (so big images don't crash the phone).
- `MainActivity.kt` is the UI: pick photo, Enhance, Compare / Before / After, Save.
- Photos are shrunk to 800 px on the long side before enhancing
  (change `MAX_INPUT_SIDE` in `MainActivity.kt`).
  4x output of 800 px = 3200 px.

## If something goes wrong

- **Crash on Enhance / "Failed: ..."**: the model may expect different
  input. The code assumes input `[1,3,H,W]`, RGB, values 0 to 1.
  Models that want 0 to 255 need a small change in `Enhancer.kt`.
- **Out of memory**: lower `MAX_INPUT_SIDE`.
- **Too slow**: use a smaller model, or lower `MAX_INPUT_SIDE`.
- Check the license of the model you use before publishing the app.

## Quality upgrades

- **HD model** (realesr-general-x4v3) is bundled, fast and good for low-end phones.
- **Best model (optional):** in the Model sheet tap "Add Best model (x4plus)" and pick a
  Real-ESRGAN x4plus `.onnx`. Then switch between "HD fast" and "Best slower".
- Tiles are larger with a wider overlap (less visible tile borders), the ONNX graph is
  optimised, and a light sharpening pass (`Finish.kt`) runs after upscaling.
- **Face model (optional):** download a GPEN-BFR-256 `.onnx` (for example
  `GPEN-BFR-256.fp16.onnx`, 38 MB) to the phone, then in the Model sheet tap
  "Add Face model (GPEN)". Faces are found with Android's built-in FaceDetector,
  restored and blended back softly. Works best on front-facing, upright faces.

## Video (beta)

Tap **Choose video (beta)** on Home. Every frame goes through the same model, is re-encoded
to H.264 and the original audio is copied. The result is saved to Movies/AIEnhancer.
To stay usable on low-end phones (`VideoEnhancer.kt`): only the first 15 seconds, frames
shrunk to 480 px on the long side before enhancing, 15 fps max. Change `MAX_SECONDS`,
`IN_SIDE` and `OUT_FPS` to trade speed for quality. Keep the screen on and the phone cool:
a 10 second clip can take many minutes. Faces are not restored in video.

### Smooth video (frame interpolation)

In the Model sheet, **Smooth video: On** adds a made-up frame between every two frames
(15 fps becomes 30 fps) using motion estimation in `FrameInterp.kt`. No extra model needed.
It is off by default. It works well on slow, gentle motion; fast motion and scene cuts can
show slight ghosting or fall back to a plain blend. It adds some time and file size.

### Video speed-ups (no quality loss)

- Areas that did not change since the previous frame (static background) are copied from the
  previous enhanced frame instead of running the model again (`Enhancer.enhance(ref, refOut)`).
- The next frame is extracted in the background while the current one is enhanced.

### Smooth video (AI, RIFE v4.25 lite)

The RIFE v4.25 lite model is bundled (`app/src/main/assets/rife.onnx`, 21.5 MB). In the Model
sheet set **Smooth video: On** and the in-between frames are made by this AI model
(`RifeInterp.kt`), doubling the frame rate (15 to 30 fps). "Use another Smooth video model"
lets you pick a different RIFE .onnx from the phone. If the model fails, the built-in motion
method (`FrameInterp.kt`) is used. The model runs at up to 1280 px on the long side
(`RifeInterp.MAX_SIDE`). Smooth video is off by default because it makes processing longer.
