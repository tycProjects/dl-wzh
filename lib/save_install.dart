import 'dart:io';
import 'package:flutter/foundation.dart';
import 'package:media_store_plus/media_store_plus.dart';
import 'package:open_filex/open_filex.dart';
import 'package:path_provider/path_provider.dart';

/// Saves finished files into Downloads/<app name>/ and opens the Android installer.
class FileSaver {
  static String safe(String s) {
    final t = s.replaceAll(RegExp(r'[\\/:*?"<>|]'), '_').trim();
    return t.isEmpty ? 'App' : t;
  }

  /// Copies [src] to Downloads/<appName>/<fileName>. Returns the text to show ("Downloads/MyApp/MyApp.apk"),
  /// or null when the public copy could not be made. The private copy at [src] always stays usable,
  /// so installing never depends on this step.
  static Future<String?> saveToDownloads(String src, String appName, {required String fileName}) async {
    if (!Platform.isAndroid) return null;
    File? copy;
    try {
      final folder = safe(appName);
      final tmp = await getTemporaryDirectory();
      copy = File('${tmp.path}/$fileName');
      await File(src).copy(copy.path);
      await MediaStore.ensureInitialized();
      MediaStore.appFolder = folder;
      await MediaStore().saveFile(
        tempFilePath: copy.path,
        dirType: DirType.download,
        dirName: DirName.download,
      );
      return 'Downloads/$folder/$fileName';
    } catch (e) {
      debugPrint('saveToDownloads failed: $e');
      return null;
    } finally {
      try {
        if (copy != null && await copy.exists()) await copy.delete();
      } catch (_) {}
    }
  }

  /// Opens the system installer for an APK. If "Install unknown apps" is not allowed for this app yet,
  /// Android shows its own prompt that leads to that setting.
  static Future<bool> install(String path) async {
    try {
      final r = await OpenFilex.open(path, type: 'application/vnd.android.package-archive');
      return r.type == ResultType.done;
    } catch (e) {
      debugPrint('install failed: $e');
      return false;
    }
  }
}
