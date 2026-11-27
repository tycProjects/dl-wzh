# One-time native-project setup for real aapt2 builds

I don't have your android/ folder in the zip you gave me (this upload only has the
Dart side), and I have no internet access in this chat to fetch the actual aapt2
binary. Everything below is written out exactly so you can do it without guessing.

## 1. Add the Kotlin method (3 lines)

In android/app/src/main/kotlin/<your/package/path>/MainActivity.kt, inside the
class, add a MethodChannel that hands Dart the folder Android actually extracted
your .so files into:

```kotlin
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

// inside configureFlutterEngine(flutterEngine: FlutterEngine)
MethodChannel(flutterEngine.dartExecutor.binaryMessenger, "wzh/native_tools")
    .setMethodCallHandler { call, result ->
        if (call.method == "nativeLibraryDir") {
            result.success(applicationContext.applicationInfo.nativeLibraryDir)
        } else {
            result.notImplemented()
        }
    }
```

This is the piece lib/quick/aapt_tools.dart calls to find the bundled tools.

## 2. Get the aapt2 binary (per CPU architecture)

You need an aapt2 built to RUN ON ANDROID (arm64/arm), not the desktop aapt2 that
ships in the Android SDK's build-tools folder -- that one runs on your PC, not on
a phone.

Two sources:
- Termux's own aapt2 package is built this way already (termux-packages repo,
  package "aapt2"). Pull the .deb for aarch64 and arm, and extract the actual
  ELF binary from inside it.
- Or cross-compile aapt2 yourself from the AOSP frameworks/base source with the
  NDK toolchain, targeting arm64-v8a/armeabi-v7a.

Check the license on whichever binary you use before shipping it (Termux's own
build is Apache-2.0, same as AOSP's aapt2 source -- but re-check this yourself,
since I can't verify a specific build's provenance from here).

Place each one at:
```
android/app/src/main/jniLibs/arm64-v8a/libaapt2.so
android/app/src/main/jniLibs/armeabi-v7a/libaapt2.so
```
(The lib*.so naming and that exact folder are what make Android extract it as an
executable automatically -- any other name or location will not work the same way.)

## 3. Get an android.jar stub for aapt2 link

aapt2 link needs the platform's public resource IDs to link against. Pull
android.jar from any Android SDK install (platforms/android-34/android.jar,
roughly 1-2 MB) and bundle it as a Flutter asset, e.g. assets/aapt/android.jar,
then add it under pubspec.yaml's assets: list. Pass its on-disk path (after
copying it out of the asset bundle to a real file, the same way
assets/quick/template.apk is read today) as androidJarAssetPath in
lib/quick/aapt_build.dart. Double-check the Android SDK License covers bundling
this specific file inside your own app before shipping it.

## 4. How the local build uses aapt2 (v111)

The Quick template's classes.dex is compiled against the template's OWN resources.arsc, so that
table stays in the APK untouched (a freshly linked one would break every resource id and crash
the app). aapt2 builds the one thing it is needed for: the binary AndroidManifest.xml.

- lib/quick/manifest_gen.dart writes a normal text manifest that mirrors the template's real one
  (activity com.template.webapp.MainActivity, providers, theme, shortcuts, file paths).
- Four tiny placeholder resources are linked with `--stable-ids` pinned to the template's real ids
  (Theme.GeneratedApp 0x7f0e010a, ic_launcher 0x7f0c0000, file_paths 0x7f100000, shortcuts 0x7f100001).
  The resources.arsc aapt2 writes is thrown away; only its manifest is kept.
- QuickBuild.run() tries this first and falls back to the old byte-patch if aapt2 is missing or
  fails. The result dialog shows "Engine: aapt2" or "Engine: patch (aapt2 skipped: ...)".

If you rebuild the template and its resource ids change, re-read them from its resources.arsc
and update templateStableIds in manifest_gen.dart.

## Testing order
1. Options > Test real build engine (compile only) -- already passing for you.
2. Quick-build any HTML site; the dialog should say "Engine: aapt2".
3. Install it and open it. If it says "Engine: patch (...)", that line is aapt2's actual error.
4. Then a website-link app, then encryptContent/blockCapture/blockRoot.
