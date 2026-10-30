# Screen Recorder (Flutter)

No android/ folder on purpose: your build workflow runs `flutter create` and generates it.
The native recorder lives in a local plugin, vendor/screen_capture_native, which Flutter wires in
automatically (its manifest adds the foreground service + permissions). The plugin sits in `vendor/`
so the workflow's "find pubspec.yaml | sort | head -n1" still picks the app's own pubspec.yaml first.

- lib/main.dart: control screen
- vendor/screen_capture_native/: MediaProjection + MediaCodec engine (Kotlin)
Judge smoothness on a release build only.
