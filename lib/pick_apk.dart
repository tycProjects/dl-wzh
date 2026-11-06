import 'package:file_picker/file_picker.dart';
import 'package:file_selector/file_selector.dart' show XFile;

/// Picks an .apk without loading it into memory. The old picker copied the chosen file inside the app's
/// memory, so a 50 MB APK made Android close the app. This one copies it to the cache in small pieces.
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
  return XFile(path, name: f.name, length: f.size);
}
