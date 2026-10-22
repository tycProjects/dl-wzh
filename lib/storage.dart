import 'dart:convert';
import 'dart:io';
import 'package:path_provider/path_provider.dart';
import 'model.dart';

class Store {
  static Future<Directory> _dir(String sub) async {
    final d = Directory('${(await getApplicationDocumentsDirectory()).path}/$sub');
    if (!await d.exists()) await d.create(recursive: true);
    return d;
  }

  static Future<List<Project>> list() async {
    final d = await _dir('projects');
    final out = <Project>[];
    for (final f in d.listSync()) {
      if (f is File && f.path.endsWith('.json')) {
        try {
          out.add(Project.fromJson(jsonDecode(await f.readAsString()) as Map<String, dynamic>));
        } catch (_) {}
      }
    }
    out.sort((a, b) => b.modified.compareTo(a.modified));
    return out;
  }

  static Future<void> save(Project p) async {
    p.modified = DateTime.now().millisecondsSinceEpoch;
    final d = await _dir('projects');
    await File('${d.path}/${p.id}.json').writeAsString(jsonEncode(p.toJson()));
  }

  /// Fresh folder for the extracted frames of one imported video.
  static Future<Directory> newVideoDir() => _dir('media/video_${DateTime.now().microsecondsSinceEpoch}');

  static Future<void> delete(String id) async {
    final d = await _dir('projects');
    final f = File('${d.path}/$id.json');
    if (await f.exists()) {
      try {
        final p = Project.fromJson(jsonDecode(await f.readAsString()) as Map<String, dynamic>);
        for (final l in p.layers) {
          final v = l.videoDir;
          if (v != null && await Directory(v).exists()) await Directory(v).delete(recursive: true);
        }
      } catch (_) {}
      await f.delete();
    }
  }

  /// Copies a picked image into app storage so it survives temp-cache cleaning.
  static Future<String> importImage(String src) async {
    final d = await _dir('media');
    final dest = '${d.path}/${DateTime.now().microsecondsSinceEpoch}_${src.split('/').last}';
    await File(src).copy(dest);
    return dest;
  }
}
