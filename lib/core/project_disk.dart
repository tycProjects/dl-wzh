import 'dart:io';

/// Keeps the editor's in-memory project in step with the real folder inside Ubuntu.
class ProjectDisk {
  final String root;
  ProjectDisk(this.root);

  /// Folders that are never loaded (generated, huge or private).
  static const skipDirs = {'build', '.dart_tool', '.gradle', '.git', '.idea', '.cxx', 'node_modules', 'Pods', '.symlinks', '.pub-cache', '.pub'};
  static const maxFile = 3 * 1024 * 1024;

  /// What was last read from / written to disk, by path: used to tell what the editor changed.
  final Map<String, List<int>> baseline = {};

  Future<Map<String, List<int>>> load() async {
    final out = <String, List<int>>{};
    final dir = Directory(root);
    if (!dir.existsSync()) return out;
    Future<void> walk(Directory d, String rel) async {
      await for (final e in d.list(followLinks: false)) {
        final name = e.uri.pathSegments.where((s) => s.isNotEmpty).last;
        final r = rel.isEmpty ? name : '$rel/$name';
        if (e is Directory) {
          if (skipDirs.contains(name)) continue;
          await walk(e, r);
        } else if (e is File) {
          try {
            if (await e.length() > maxFile) continue;
            out[r] = await e.readAsBytes();
          } catch (_) {}
        }
      }
    }

    await walk(dir, '');
    baseline
      ..clear()
      ..addAll(out);
    return out;
  }

  /// Writes what the editor changed (compared by identity with what was loaded) and removes what it deleted.
  /// Returns how many files were touched.
  Future<int> sync(Map<String, List<int>> files) async {
    var n = 0;
    for (final k in files.keys) {
      final b = files[k]!;
      if (identical(baseline[k], b)) continue;
      try {
        final f = File('$root/$k');
        await f.parent.create(recursive: true);
        await f.writeAsBytes(b, flush: true);
        baseline[k] = b;
        n++;
      } catch (_) {}
    }
    for (final k in baseline.keys.toList()) {
      if (files.containsKey(k)) continue;
      try {
        final f = File('$root/$k');
        if (f.existsSync()) await f.delete();
      } catch (_) {}
      baseline.remove(k);
      n++;
    }
    return n;
  }
}

extension ProjectDiskRefresh on ProjectDisk {
  /// Picks up changes made on disk by commands in the terminal (a new pubspec.lock, a whole new
  /// project from `flutter create` ...) without touching files the editor has changed. Returns true if anything changed.
  Future<bool> refresh(Map<String, List<int>> files) async {
    final disk = await ProjectDisk(root).load();
    var changed = false;
    for (final e in disk.entries) {
      final mine = files[e.key];
      final base = baseline[e.key];
      final untouched = mine == null || identical(mine, base);
      if (!untouched) continue; // edited in the editor: its version wins and is written by the next sync
      if (mine == null || !_same(mine, e.value)) {
        files[e.key] = e.value;
        baseline[e.key] = e.value;
        changed = true;
      }
    }
    for (final k in files.keys.toList()) {
      if (disk.containsKey(k)) continue;
      if (identical(files[k], baseline[k])) {
        files.remove(k);
        baseline.remove(k);
        changed = true;
      }
    }
    return changed;
  }

  bool _same(List<int> a, List<int> b) {
    if (a.length != b.length) return false;
    for (var i = 0; i < a.length; i++) {
      if (a[i] != b[i]) return false;
    }
    return true;
  }
}
