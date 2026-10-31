# UHD Enhancer (Flutter)
Wink-style photo enhancer: AI Enhance, Portrait, Crisp, HDR, Upscale. Hold the photo to compare.

Same layout as the WZH 2 APK project, so the apk builder accepts it:
- pubspec.yaml and lib/ at the project root (no android/ folder, the builder generates it)
- patch_android.py sets the app name and storage permission after flutter create
- no .github/ folder on purpose: a project workflow would replace the builder's own signed build

Zip the `uhd_enhancer` folder and upload it as a Flutter project.
