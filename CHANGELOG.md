# Changelog

## 2.0.1 – (still UNBUILT)
- ncnn model import: pick a .param + .bin together (sorted by file content, not name), choose 2×/3×/4×, name it. The import is verified by really loading it in the engine and running a test image; the output scale must match what you chose or the import is rejected. Imported models appear in the dropdown and can be removed.
- Imagine-style Enhance workspace: after choosing a photo you land on a screen with the preview, Model dropdown, Photo detail, Output size chips (2×/Original/Custom), Enhance and New Image buttons. Photo detail is applied when you press Enhance.
- Restyled shared components: rounder cards, soft shadows, tonal chips, gradient primary button, theme-aware dialogs.
- Smooth video: the built-in `rife.onnx` already ships in assets; Replace/Reset work. GPEN: no GPEN file exists in any provided archive, so it is still user-supplied via Add.

## 2.0 – Alter Enhancer (UNBUILT, untested on device – see TESTING.md)
- Rebranded Tyce Enhancer → Alter Enhancer (label, splash, exit dialog). Package/applicationId `com.tyce.enhancer` kept on purpose.
- Added Imagine's ncnn engine (`libengine_jni.so`, `libncnn.so`) and its 5 models: AnimeVideo v3 ×2/×3/×4, Real-ESRGAN ×4 Anime, Real-ESRGAN ×4 Photo. `com.kaminari.imagine.Engine` is kept as-is because the native symbols depend on that name.
- New model dropdown (name, description, real file size), Output size (2×/4× per Quality, Original, Custom), Save format (JPEG/PNG/WebP + quality preset).
- **Quality root causes fixed**: removed the unconditional extra sharpening pass (`Finish.sharpen`) that was applied on top of the model's own sharpening; output is resampled at most once and never above what the model produced; default save is JPEG 92 instead of 100% PNG.
- Appearance: System / Light / Dark / AMOLED Black (#000000), persisted, applied live, per-theme window background to avoid white flashes.
- Cloud & API keys: modular `CloudProvider`; Replicate (`nightmareai/real-esrgan`) implemented with key validation. Keys encrypted with an Android Keystore AES-GCM key and excluded from backup.
- "How to get API keys" section with official Replicate links.
- Telegram and Instagram links via `AppConfig.kt` (Instagram hidden until you set a URL). Opens the app first, browser fallback.
