import 'dart:io';
import 'package:flutter/foundation.dart';
import 'package:media_store_plus/media_store_plus.dart';
import 'package:open_filex/open_filex.dart';
import 'package:path_provider/path_provider.dart';

/// Saves finished files into Downloads/WZH 2 APK/ and opens the Android installer.
class FileSaver {
  /// Every built app lands in this one folder inside Downloads; the file keeps the built app's name.
  static const downloadFolder = 'WZH 2 APK';

  static String safe(String s) {
    final t = s.replaceAll(RegExp(r'[\\/:*?"<>|]'), '_').trim();
    return t.isEmpty ? 'App' : t;
  }

  /// Why the last public copy failed (for the screen and for debugging); null when it worked.
  static String? lastError;

  /// Copies [src] to Downloads/WZH 2 APK/<fileName>. Returns the text to show (\"Downloads/WZH 2 APK/MyApp.apk\"),
  /// or null when the public copy could not be made. The private copy at [src] always stays usable,
  /// so installing never depends on this step.
  /// Two routes, so a phone where one of them is blocked still ends up with the file in Downloads:
  /// 1) the system MediaStore (the proper way on Android 10+), 2) a direct copy into the Download folder.
  static Future<String?> saveToDownloads(String src, String appName, {required String fileName}) async {
    lastError = null;
    if (!Platform.isAndroid) return null;
    final shown = downloadFolder.isEmpty ? 'Downloads/$fileName' : 'Downloads/$downloadFolder/$fileName';
    final size = await File(src).length();

    // 1) MediaStore.
    File? copy;
    try {
      final tmp = await getTemporaryDirectory();
      // Use a separate subfolder: when src already sits in the temp folder under the same name,
      // deleting "copy" would delete src itself and the copy then fails (PathNotFoundException).
      final stage = Directory('${tmp.path}/share_stage');
      await stage.create(recursive: true);
      copy = File('${stage.path}/$fileName');
      if (await copy.exists()) await copy.delete();
      await File(src).copy(copy.path);
      await MediaStore.ensureInitialized();
      if (downloadFolder.isNotEmpty) MediaStore.appFolder = downloadFolder;
      final dynamic r = await MediaStore().saveFile(
        tempFilePath: copy.path,
        dirType: DirType.download,
        dirName: DirName.download,
      );
      // The plugin reports a failed save by returning false / null instead of throwing.
      if (r != null && r != false) return shown;
      lastError = 'MediaStore refused the file';
    } catch (e) {
      lastError = '$e';
      debugPrint('saveToDownloads (MediaStore) failed: $e');
    } finally {
      try {
        if (copy != null && await copy.exists()) await copy.delete();
      } catch (_) {}
    }

    // 2) Direct copy into the public Download folder.
    try {
      final dir = Directory(downloadFolder.isEmpty
          ? '/storage/emulated/0/Download'
          : '/storage/emulated/0/Download/$downloadFolder');
      await dir.create(recursive: true);
      final out = File('${dir.path}/$fileName');
      await File(src).copy(out.path);
      if (await out.exists() && await out.length() == size) {
        lastError = null;
        return shown;
      }
      lastError = 'The copy in Downloads was incomplete';
    } catch (e) {
      lastError = '${lastError ?? ''} / $e'.trim();
      debugPrint('saveToDownloads (direct) failed: $e');
    }
    return null;
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
