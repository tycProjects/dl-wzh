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

## 4. Build a logic-only template

lib/quick/aapt_build.dart expects templateDex: a small APK/zip containing only
classes.dex (and lib/ if you have native code) -- no AndroidManifest.xml or
resources.arsc of its own, since aapt2 is now the one generating those fresh
every build. The simplest way to get this: build your existing template.apk as
usual, then strip AndroidManifest.xml and resources.arsc/res/* out of it,
keeping classes.dex and lib/.

## 5. Wire it up

lib/quick/aapt_build.dart's buildWithAapt2(...) is the new entry point -- it
sits next to (not replacing) the existing QuickBuild.run(...) in
lib/quick/quick_builder.dart, so your current byte-patched Quick build keeps
working while you test this one. Point your UI at whichever one you're ready
to ship.

## Testing order
1. Call AaptTools.compile/link directly on a trivial res/ folder and confirm the
   binary actually runs on a real device (not just the emulator) -- a 32-bit-only
   binary on a 64-bit-only device, or vice versa, will fail silently different
   ways depending on the phone.
2. Then try buildWithAapt2 end to end on an HTML site.
3. Then a website-link app.
4. Then turn on encryptContent/blockCapture/blockRoot and retest.
