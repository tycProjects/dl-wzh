# UHD Enhancer (Flutter)
Photo enhancer: before/after slider, plain output sizes (1080p, 2K, 4K), halo-free sharpening,
and real AI upscaling (Real-ESRGAN via ncnn) for small photos.

Layout matches WZH 2 APK, so the apk builder accepts it:
- pubspec.yaml + lib/ at the root; android/ only holds a jniLibs placeholder (builder regenerates the rest)
- patch_android.py runs on the build machine: sets the app name, then downloads the AI engine
  and model from the RealSR-NCNN-Android release and bundles them (best effort: if the download
  fails the app still builds and uses the built-in Smart engine)
- no .github/ folder on purpose

AI is used when the photo is under ~1 megapixel and needs to get bigger. Bigger photos use Smart enhance.
