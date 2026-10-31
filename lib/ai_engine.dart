import 'dart:io';
import 'dart:typed_data';
import 'package:flutter/services.dart';
import 'package:path_provider/path_provider.dart';

/// Real AI upscaling (Real-ESRGAN style, ncnn). The program and model are added by
/// patch_android.py when the APK is built. Any problem returns null, and the app
/// falls back to the built-in smart engine, so this never breaks the app.
class AiEngine {
  static const _channel = MethodChannel('wzh/native_tools');
  static String? _exe;

  static Future<String?> _findExe() async {
    if (_exe != null) return _exe;
    try {
      final dir = await _channel.invokeMethod<String>('nativeLibraryDir');
      if (dir == null) return null;
      final f = File('$dir/librealsr_exec.so');
      if (await f.exists()) _exe = f.path;
    } catch (_) {}
    return _exe;
  }

  /// Copies the model out of the app assets (once) and returns its path without extension.
  static Future<String?> _modelBase() async {
    try {
      final dir = Directory('${(await getApplicationSupportDirectory()).path}/ai');
      await dir.create(recursive: true);
      for (final ext in ['param', 'bin']) {
        final f = File('${dir.path}/model.$ext');
        if (!await f.exists() || await f.length() == 0) {
          final data = await rootBundle.load('assets/ai/model.$ext');
          await f.writeAsBytes(
              data.buffer.asUint8List(data.offsetInBytes, data.lengthInBytes),
              flush: true);
        }
      }
      return '${dir.path}/model';
    } catch (_) {
      return null;
    }
  }

  static Future<bool> available() async =>
      await _findExe() != null && await _modelBase() != null;

  /// Upscales a PNG 4x with the AI model. Returns PNG bytes, or null if anything fails.
  static Future<Uint8List?> upscale4x(Uint8List png) async {
    final exe = await _findExe();
    final model = await _modelBase();
    if (exe == null || model == null) return null;
    final tmp = await getTemporaryDirectory();
    final stamp = DateTime.now().millisecondsSinceEpoch;
    final inF = File('${tmp.path}/ai_in_$stamp.png');
    final outF = File('${tmp.path}/ai_out_$stamp.png');
    try {
      await inF.writeAsBytes(png, flush: true);
      final libDir = File(exe).parent.path;
      // Try the phone's graphics chip first, then the slower CPU.
      for (final gpu in ['0', '-1']) {
        try {
          final r = await Process.run(
            exe,
            ['-i', inF.path, '-o', outF.path, '-s', '4', '-m', model, '-g', gpu],
            environment: {'LD_LIBRARY_PATH': libDir},
          ).timeout(const Duration(minutes: 5));
          if (r.exitCode == 0 && await outF.exists() && await outF.length() > 0) {
            return await outF.readAsBytes();
          }
        } catch (_) {}
      }
      return null;
    } catch (_) {
      return null;
    } finally {
      try { if (await inF.exists()) await inF.delete(); } catch (_) {}
      try { if (await outF.exists()) await outF.delete(); } catch (_) {}
    }
  }
}
