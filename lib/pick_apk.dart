import 'dart:io';
import 'package:file_picker/file_picker.dart';
import 'package:file_selector/file_selector.dart' show XFile;
import 'package:path_provider/path_provider.dart';

/// Picks an .apk without loading it into memory. The old picker copied the chosen file inside the app's
/// memory, so a 50 MB APK made Android close the app. This one copies it to the cache in small pieces.
///
/// The picker's copy lives in the CACHE folder, which Android (or a phone "cleaner") can empty at any time,
/// even while the form is still open. That is what caused "PathNotFoundException ... file_picker ... No such file".
/// So the file is moved straight away into the app's own files folder, which is never cleared that way.
Future<XFile?> pickApkFile() async {
  final r = await FilePicker.platform.pickFiles(
    type: FileType.custom,
    allowedExtensions: const ['apk'],
    allowMultiple: false,
    withData: false,
    withReadStream: false,
  );
  if (r == null || r.files.isEmpty) return null;
  final f = r.files.first;
  final path = f.path;
  if (path == null) return null;
  try {
    final root = Directory('${(await getApplicationSupportDirectory()).path}/picked_apk');
    await _sweep(root);
    final dir = Directory('${root.path}/${DateTime.now().millisecondsSinceEpoch}');
    await dir.create(recursive: true);
    final safe = f.name.replaceAll(RegExp(r'[^A-Za-z0-9._ -]'), '_');
    final kept = await File(path).copy('${dir.path}/$safe');
    try { await File(path).delete(); } catch (_) {} // the cache copy is no longer needed, and was doubling the space used
    return XFile(kept.path, name: f.name, length: await kept.length());
  } catch (_) {
    return XFile(path, name: f.name, length: f.size); // could not move it: use the picker's copy like before
  }
}

/// Removes picked APKs older than 6 hours (each pick sits in a folder named by its time).
Future<void> _sweep(Directory root) async {
  try {
    if (!await root.exists()) return;
    final cutoff = DateTime.now().millisecondsSinceEpoch - 6 * 3600 * 1000;
    await for (final e in root.list()) {
      final t = int.tryParse(e.path.split(Platform.pathSeparator).last);
      if (t != null && t < cutoff) {
        try { await e.delete(recursive: true); } catch (_) {}
      }
    }
  } catch (_) {}
}
