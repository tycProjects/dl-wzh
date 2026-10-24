import 'dart:io';
import 'package:flutter/services.dart';

/// Finds and runs the real aapt2 / apksigner / zipalign binaries that ship inside this
/// app's own native-library folder (see ANDROID_SETUP_AAPT2.md for how they get there).
///
/// Why this folder and not somewhere the app downloads them to: Android extracts
/// anything under android/app/src/main/jniLibs/<abi>/ as an executable native library,
/// no matter what targetSdk this app uses -- unlike a file written to the app's normal
/// data directory, which Android 10+ refuses to execute for an app targeting SDK 29+.
/// Renaming a command-line tool to lib<name>.so and putting it there is a well-known,
/// legitimate use of that mechanism (NOT a hack around a security boundary -- Android's
/// own installer is the one extracting it with the execute bit set).
class AaptTools {
  static const _channel = MethodChannel('wzh/native_tools');

  static String? _nativeDir;

  /// The folder this app's own .so files (real native libs AND our renamed tools) live in.
  /// Backed by a 3-line Kotlin method -- see ANDROID_SETUP_AAPT2.md -- because Flutter has
  /// no built-in way to read ApplicationInfo.nativeLibraryDir itself.
  static Future<String> nativeLibraryDir() async {
    return _nativeDir ??= await _channel.invokeMethod<String>('nativeLibraryDir') ??
        (throw StateError('nativeLibraryDir channel not wired up -- see ANDROID_SETUP_AAPT2.md'));
  }

  static Future<String> _tool(String name) async => '${await nativeLibraryDir()}/lib$name.so';

  /// Runs a tool from the native-library folder. Throws with stdout+stderr on a non-zero exit,
  /// since a silent aapt2 failure is much harder to debug than a loud one.
  static Future<String> _run(String toolName, List<String> args) async {
    final exe = await _tool(toolName);
    if (!await File(exe).exists()) {
      throw StateError('$exe is missing -- the $toolName binary was not bundled for this '
          'device\'s CPU (ABI). See ANDROID_SETUP_AAPT2.md.');
    }
    final r = await Process.run(exe, args);
    if (r.exitCode != 0) {
      throw StateError('$toolName ${args.join(' ')} failed (${r.exitCode}):\n${r.stdout}\n${r.stderr}');
    }
    return r.stdout as String;
  }

  /// `aapt2 compile --dir <resDir> -o <outZip>`
  static Future<void> compile({required String resDir, required String outZip}) =>
      _run('aapt2', ['compile', '--dir', resDir, '-o', outZip]);

  /// `aapt2 link -I <androidJar> --manifest <manifest> -o <outApk> <compiledZips...>`
  /// androidJarPath is the small platform-resources stub bundled as a Flutter asset
  /// (see ANDROID_SETUP_AAPT2.md for where to get one and the licensing note).
  static Future<void> link({
    required String androidJarPath,
    required String manifestPath,
    required String outApkPath,
    required List<String> compiledZips,
    int minSdk = 21,
    int targetSdk = 34,
  }) =>
      _run('aapt2', [
        'link',
        '-I', androidJarPath,
        '--manifest', manifestPath,
        '--min-sdk-version', '$minSdk',
        '--target-sdk-version', '$targetSdk',
        '-o', outApkPath,
        ...compiledZips,
      ]);
}
