# Country Detector — Android APK

Floating overlay app. Premium HTML UI inside WebView.
Draggable bubble stays on top of every app.

---

## What this does

1. **Main screen** — Premium HTML/CSS UI loaded in WebView
2. **Grant overlay** — triggers real `Settings.ACTION_MANAGE_OVERLAY_PERMISSION`  
3. **Grant screen capture** — triggers real `MediaProjectionManager` consent dialog
4. **Floating bubble** — draggable globe icon, lives above all apps
5. **Tool menu** — tap bubble → menu expands with Detect / Copy Name / Copy Code / Copy Flag

---

## How to build

### Requirements
- Android Studio Hedgehog or newer
- JDK 17
- Android SDK 34

### Steps

```bash
# 1 — Open project in Android Studio
File → Open → select CountryDetector folder

# 2 — Sync Gradle
(Android Studio does this automatically)

# 3 — Build APK
Build → Build Bundle(s) / APK(s) → Build APK(s)

# 4 — APK location
app/build/outputs/apk/debug/app-debug.apk
```

### Install via ADB
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## File structure

```
CountryDetector/
├── assets/
│   └── index.html          ← Premium HTML UI (WebView content)
├── app/
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/countrydetector/
│       │   ├── MainActivity.kt     ← WebView host + permission bridge
│       │   └── FloatingService.kt  ← Bubble + menu + detection
│       └── res/
│           ├── layout/
│           │   ├── bubble_view.xml ← Round draggable bubble
│           │   └── menu_view.xml   ← Floating tool panel
│           ├── drawable/           ← Shapes, gradients
│           └── values/styles.xml
└── app/build.gradle
```

---

## How floating bubble works

```
User taps Launch
    ↓
FloatingService.startForeground()   ← Required for Android 8+
    ↓
WindowManager.addView(bubble, TYPE_APPLICATION_OVERLAY)
    ↓
Bubble draggable via OnTouchListener
    ↓
Tap bubble → showMenu() → WindowManager.addView(menu)
    ↓
Tap Detect → captureAndDetect()
    ↓
Real flow: MediaProjection → ImageReader → Bitmap → ML Kit OCR → match DB
Demo flow: random sample from COUNTRY_DB
```

---

## Add real OCR (optional upgrade)

Uncomment in `build.gradle`:
```gradle
implementation 'com.google.mlkit:text-recognition:16.0.0'
```

Then in `FloatingService.captureAndDetect()`, replace the demo block with:
```kotlin
// 1. Grab frame from ImageReader (via MediaProjection virtual display)
val image = imageReader?.acquireLatestImage() ?: return
val bitmap = image.toBitmap()
image.close()

// 2. Run ML Kit
val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
recognizer.process(InputImage.fromBitmap(bitmap, 0))
    .addOnSuccessListener { result ->
        val text = result.text
        val countries = detectCountries(text)
        // update menu UI
    }
```

---

## Permissions declared

| Permission | Why |
|---|---|
| `SYSTEM_ALERT_WINDOW` | Floating bubble over other apps |
| `FOREGROUND_SERVICE` | Keep service alive when app minimised |
| `FOREGROUND_SERVICE_MEDIA_PROJECTION` | Android 14 requirement |
| `VIBRATE` | Haptic feedback on bubble tap |
| `WAKE_LOCK` | Keep CPU on during scan |
