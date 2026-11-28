import 'dart:convert';
import 'dart:io';
import 'dart:typed_data';
import 'package:archive/archive.dart';
import 'package:flutter/foundation.dart' show compute;
import 'quick/quick_builder.dart' show quickPackageName;

/// Makes "Package suffix", "Version name" and "Version code" work for Flutter and Kotlin/Java
/// (Gradle) projects too: the project zip is edited on the phone before it is sent, so it does not
/// matter which build server picks it up. Returns null when nothing needs changing (a website zip,
/// no options set, a zip that is too big to edit here, or no file in it we know how to edit).
const _maxEditBytes = 60 * 1024 * 1024;

Future<Uint8List?> patchProjectZip({
  Uint8List? bytes,
  String? path,
  required String appName,
  required String suffix,
  required String versionName,
  required int versionCode,
}) async {
  if (suffix.trim().isEmpty && versionName.trim().isEmpty && versionCode <= 0) return null;
  try {
    Uint8List? data = bytes;
    if (data == null && path != null) {
      final f = File(path);
      if (await f.length() > _maxEditBytes) return null;
      data = await f.readAsBytes();
    }
    if (data == null) return null;
    return await compute(_patch, <String, Object>{
      'bytes': data,
      'pkg': suffix.trim().isEmpty ? '' : quickPackageName(appName, suffix),
      'vname': versionName.trim(),
      'vcode': versionCode,
    });
  } catch (_) {
    return null; // never block a build because the edit failed: the server still gets the options
  }
}

int _depth(String n) => '/'.allMatches(n).length;

String _text(ArchiveFile f) => utf8.decode(f.content as List<int>, allowMalformed: true);

ArchiveFile _file(String name, String text) {
  final b = utf8.encode(text);
  return ArchiveFile(name, b.length, b);
}

/// Changes [re] only when it matches exactly once. No match, or several (product flavors, build
/// variants), means "not sure which one", so the text is left alone.
String? _editOne(String t, RegExp re, String Function(Match) f) {
  if (re.allMatches(t).length != 1) return null;
  return t.replaceFirstMapped(re, f);
}

/// Flutter wants a semver-looking name: 1.2 -> 1.2.0
String _semver(String v) {
  if (!RegExp(r'^\d+(\.\d+)*$').hasMatch(v)) return v;
  final p = v.split('.');
  while (p.length < 3) {
    p.add('0');
  }
  return p.join('.');
}

Uint8List? _patch(Map<String, Object> a) {
  final pkg = a['pkg'] as String, vname = a['vname'] as String, vcode = a['vcode'] as int;
  Archive z;
  try {
    z = ZipDecoder().decodeBytes(a['bytes'] as Uint8List);
  } catch (_) {
    return null;
  }

  ArchiveFile? pub;
  final gradles = <ArchiveFile>[];
  for (final f in z.files) {
    if (!f.isFile) continue;
    final n = f.name;
    if ((n == 'pubspec.yaml' || n.endsWith('/pubspec.yaml')) && (pub == null || _depth(n) < _depth(pub.name))) pub = f;
    if (RegExp(r'(^|/)app/build\.gradle(\.kts)?$').hasMatch(n)) gradles.add(f);
  }

  final replaced = <String, ArchiveFile>{};

  // Flutter: version lives in pubspec.yaml as "name+code".
  if (pub != null && pub.size <= 100000 && (vname.isNotEmpty || vcode > 0)) {
    final t = _text(pub);
    if (RegExp(r'^flutter:', multiLine: true).hasMatch(t)) {
      final m = RegExp(r'^version:[ \t]*([^\s#]+)', multiLine: true).firstMatch(t);
      final cur = (m?.group(1) ?? '1.0.0+1').split('+');
      final name = vname.isNotEmpty ? _semver(vname) : cur.first;
      final code = vcode > 0 ? '$vcode' : (cur.length > 1 ? cur[1] : '');
      final line = 'version: ${code.isEmpty ? name : '$name+$code'}';
      final newText = m != null ? t.replaceFirst(RegExp(r'^version:[ \t]*[^\s#]+', multiLine: true), line) : '$t\n$line\n';
      replaced[pub.name] = _file(pub.name, newText);
    }
  }

  // Flutter and Kotlin/Java: app/build.gradle(.kts) holds applicationId, and the version when it is written out.
  for (final g in gradles) {
    if (g.size > 300000) continue; // not a normal build file: leave it alone
    var t = _text(g);
    final before = t;
    if (pkg.isNotEmpty) {
      t = _editOne(t, RegExp('''(applicationId\\s*=?\\s*)(["'])[^"']*\\2'''), (m) => '${m[1]}${m[2]}$pkg${m[2]}') ?? t;
    }
    if (vname.isNotEmpty) {
      t = _editOne(t, RegExp('''(versionName\\s*=?\\s*)(["'])[^"']*\\2'''), (m) => '${m[1]}${m[2]}$vname${m[2]}') ?? t;
    }
    if (vcode > 0) {
      t = _editOne(t, RegExp(r'(versionCode\s*=?\s*)\d+'), (m) => '${m[1]}$vcode') ?? t;
    }
    if (t != before) replaced[g.name] = _file(g.name, t);
  }

  if (replaced.isEmpty) return null;
  final out = Archive();
  for (final f in z.files) {
    out.addFile(replaced[f.name] ?? f);
  }
  final enc = ZipEncoder().encode(out);
  if (enc == null) return null;
  final bytes = Uint8List.fromList(enc);
  // Safety check: read the new zip back. If anything is off, send the original untouched.
  try {
    final back = ZipDecoder().decodeBytes(bytes);
    if (back.files.length != z.files.length) return null;
    for (final e in replaced.entries) {
      final f = back.findFile(e.key);
      if (f == null || _text(f) != _text(e.value)) return null;
    }
  } catch (_) {
    return null;
  }
  return bytes;
}
