import 'dart:async';
import 'dart:convert';
import 'dart:io';
import 'package:archive/archive.dart';
import 'package:file_selector/file_selector.dart';
import 'package:flutter/cupertino.dart' show CupertinoPageRoute;
import 'package:flutter/foundation.dart' show compute;
import 'package:flutter/material.dart' hide Text;
import 'package:flutter/services.dart';
import 'package:path_provider/path_provider.dart';
import '../anim.dart' show showIosDialog;
import '../api.dart';
import '../banner.dart';
import '../save_install.dart';
import '../theme.dart';
import '../tr.dart';
import '../notify.dart';
import '../signing_key.dart';
import '../store.dart';
import 'build_mode_sheet.dart';
import 'build_page.dart';
import 'build_target_sheet.dart';
import 'code_apps.dart';
import 'code_assist.dart';
import 'code_format.dart';
import 'quick_build_page.dart';
import 'web_preview.dart';
import 'permissions_page.dart' show selectedPerms;

/// The project that is open in the code editor. Kept here so the build page can jump back to a file.
class CodeProject {
  static final CodeProject i = CodeProject._();
  CodeProject._();
  String name = '';
  final files = <String, List<int>>{};
  final modified = <String>{};
  double fontSize = 13;
  bool wrap = false;
  bool suggest = true; // the suggestion popup
  (String, int)? pending;
  String? hint; // what to tell the person when a failed build sends them to a line
  // What was open when the editor was left, so coming back puts you where you were.
  final openPaths = <String>[];
  String? curPath;
  final cursor = <String, int>{};
  final scroll = <String, double>{};
  void resetView() {
    openPaths.clear();
    curPath = null;
    cursor.clear();
    scroll.clear();
  }
  final jumpTick = ValueNotifier<int>(0);
  bool get isOpen => files.isNotEmpty;

  /// Finds the project file for a path printed in a build error (any prefix, same ending).
  String? find(String printed) {
    final p = printed.replaceAll('\\', '/');
    if (files.containsKey(p)) return p;
    String? hit;
    for (final k in files.keys) {
      if (p.endsWith('/$k') || p == k) return k;
      if (hit == null && k.split('/').last == p.split('/').last) hit = k;
    }
    return hit;
  }
}

Route<void> codeEditorRoute() => CupertinoPageRoute<void>(settings: const RouteSettings(name: 'code-editor'), builder: (_) => const CodeEditorPage());

/// Opens a file of the open project in the full-screen editor at [line] (comes back to the editor if it is further down the stack).
void openCodeFile(BuildContext context, String path, int line, {String? hint}) {
  CodeProject.i.pending = (path, line);
  CodeProject.i.hint = hint;
  final nav = Navigator.of(context);
  var found = false;
  nav.popUntil((r) {
    if (r.settings.name == 'code-editor') {
      found = true;
      return true;
    }
    return r.isFirst;
  });
  if (found) {
    CodeProject.i.jumpTick.value++;
  } else {
    nav.push(codeEditorRoute());
  }
}

/// Finds "File.kt:12" style locations in a failed build's text and returns the project file + line.
(String, int)? errorLocation(String blob) {
  if (!CodeProject.i.isOpen) return null;
  final re = RegExp(r'([\w./\\\-]+\.(?:kt|java|xml|gradle|kts|dart|html|htm|css|js|json|yaml)):(\d+)');
  for (final line in blob.split('\n')) {
    if (!(line.contains('e: ') || line.contains('error') || line.contains('Error'))) continue;
    for (final m in re.allMatches(line)) {
      final f = CodeProject.i.find(m.group(1)!);
      if (f != null) return (f, int.parse(m.group(2)!));
    }
  }
  return null;
}

/// A failed build's error in plain words, or null when it isn't one we know.
String? friendlyError(String blob) {
  final un = RegExp(r'Unresolved reference:?\s*[\x27\x22]?(\w+)').firstMatch(blob);
  if (un != null) return 'The code uses \u201C${un.group(1)}\u201D but it isn\u2019t defined. Check the spelling, or add the missing import.';
  if (blob.contains('cannot find symbol')) return 'The code uses something that isn\u2019t defined. Check the spelling, or add the missing import.';
  if (RegExp(r'package .* does not exist').hasMatch(blob)) return 'An import points to something that isn\u2019t in the project.';
  if (RegExp(r'resource .*not found|AAPT: error|No resource found', caseSensitive: false).hasMatch(blob)) return 'The code points to a file or resource that doesn\u2019t exist, like an image or a layout. Add it or fix the name.';
  if (RegExp(r"Expecting|expected|Syntax error|Unexpected tokens", caseSensitive: false).hasMatch(blob)) return 'A bracket, quote or semicolon is missing around this line.';
  if (blob.contains('Type mismatch') || blob.contains('incompatible types')) return 'A value here is the wrong type.';
  if (blob.contains('Redeclaration') || blob.contains('already defined') || blob.contains('Duplicate')) return 'Something is defined twice. Remove one of them.';
  return null;
}

/// One short line to show next to the code: the plain-words version, else the compiler's own first error line.
String? errorHint(String blob) {
  final f = friendlyError(blob);
  if (f != null) return f;
  for (final line in blob.split('\n')) {
    if (line.contains('e: ') || line.contains('error:') || line.contains('Error')) {
      final t = line.replaceAll(RegExp(r'^.*?(e: |error: )'), '').trim();
      if (t.isNotEmpty) return t.length > 140 ? '${t.substring(0, 140)}\u2026' : t;
    }
  }
  return null;
}

Map<String, List<int>> _unpack(List<int> zip) {
  final out = <String, List<int>>{};
  for (final f in ZipDecoder().decodeBytes(zip)) {
    if (!f.isFile || f.name.startsWith('__MACOSX/') || f.name.endsWith('.DS_Store')) continue;
    out[f.name] = f.content as List<int>;
  }
  return out;
}

List<int> _pack(Map<String, List<int>> files) {
  final a = Archive();
  files.forEach((n, b) => a.addFile(ArchiveFile(n, b.length, b)));
  return ZipEncoder().encode(a)!;
}

const _binExt = {'png', 'jpg', 'jpeg', 'webp', 'gif', 'ico', 'so', 'jar', 'apk', 'aab', 'dex', 'ttf', 'otf', 'woff', 'woff2', 'zip', 'arsc', 'mp3', 'mp4', 'ogg', 'wav', 'pdf', 'keystore', 'jks', 'class'};
const _maxEditBytes = 2560 * 1024; // 2.5 MB: room for a 2 MB page
/// A file bigger than this opens with a notice: colours are limited to what is on screen and typing can be slower.
const _bigFileBytes = 400 * 1024;
/// A line longer than this makes the file wrap on its own (minified pages are one giant line).
const _autoWrapLine = 20000;
/// Most characters that are ever coloured in one go, however long the lines are.
const _maxColourChars = 60000;

String _ext(String path) {
  final n = path.split('/').last;
  if (!n.contains('.')) return const {'dockerfile': 'dockerfile', 'makefile': 'mk', 'gradlew': 'sh', 'jenkinsfile': 'groovy'}[n.toLowerCase()] ?? '';
  return n.split('.').last.toLowerCase();
}

bool _isText(String path, List<int> b) {
  if (_binExt.contains(_ext(path))) return false;
  final n = b.length < 2000 ? b.length : 2000;
  for (var i = 0; i < n; i++) {
    if (b[i] == 0) return false;
  }
  return true;
}

int _cmp(String a, String b) {
  final x = a.split('/'), y = b.split('/');
  for (var i = 0; i < x.length && i < y.length; i++) {
    if (x[i] == y[i]) continue;
    final xd = i < x.length - 1, yd = i < y.length - 1;
    if (xd != yd) return xd ? -1 : 1;
    return x[i].toLowerCase().compareTo(y[i].toLowerCase());
  }
  return x.length - y.length;
}

class _Row {
  final String path, label;
  final int depth;
  final bool folder;
  _Row(this.path, this.label, this.depth, this.folder);
}

IconData _iconFor(String path) => switch (_ext(path)) {
      'kt' || 'java' || 'kts' || 'dart' => Icons.code_rounded,
      'xml' => Icons.data_object_rounded,
      'html' || 'htm' => Icons.html_rounded,
      'css' || 'scss' => Icons.css_rounded,
      'js' || 'mjs' || 'cjs' => Icons.javascript_rounded,
      'gradle' => Icons.build_circle_outlined,
      'png' || 'jpg' || 'jpeg' || 'webp' || 'gif' => Icons.image_outlined,
      _ when _cExts.contains(_ext(path)) || _hashExts.contains(_ext(path)) || _ext(path) == 'smali' => Icons.code_rounded,
      _ => Icons.description_outlined,
    };

Color _colorFor(String path) => switch (_ext(path)) {
      'kt' || 'kts' => const Color(0xFFC792EA),
      'java' => const Color(0xFFFFCB6B),
      'xml' => const Color(0xFFF07178),
      'gradle' => const Color(0xFF82AAFF),
      'dart' => const Color(0xFF54C5F8),
      'html' || 'htm' => const Color(0xFFFF8A65),
      'css' || 'scss' => const Color(0xFF64B5F6),
      'js' || 'mjs' || 'cjs' => const Color(0xFFFFD54F),
      _ => C.muted,
    };

String _template(String path) {
  final name = path.split('/').last;
  final stem = name.contains('.') ? name.substring(0, name.lastIndexOf('.')) : name;
  final ext = _ext(path);
  final m = RegExp(r'(?:java|kotlin)/(.+)/[^/]+$').firstMatch(path);
  final pkg = m == null ? '' : m.group(1)!.replaceAll('/', '.');
  final act = stem.endsWith('Activity');
  if (ext == 'kt') {
    final head = pkg.isEmpty ? '' : 'package $pkg\n\n';
    if (act) {
      return '${head}import android.os.Bundle\nimport androidx.appcompat.app.AppCompatActivity\n\nclass $stem : AppCompatActivity() {\n    override fun onCreate(savedInstanceState: Bundle?) {\n        super.onCreate(savedInstanceState)\n    }\n}\n';
    }
    return '${head}class $stem {\n    \n}\n';
  }
  if (ext == 'java') {
    final head = pkg.isEmpty ? '' : 'package $pkg;\n\n';
    if (act) {
      return '${head}import android.os.Bundle;\nimport androidx.appcompat.app.AppCompatActivity;\n\npublic class $stem extends AppCompatActivity {\n    @Override\n    protected void onCreate(Bundle savedInstanceState) {\n        super.onCreate(savedInstanceState);\n    }\n}\n';
    }
    return '${head}public class $stem {\n    \n}\n';
  }
  if (ext == 'dart') {
    if (stem == 'main') {
      return "import 'package:flutter/material.dart';\n\nvoid main() {\n  runApp(const MyApp());\n}\n\nclass MyApp extends StatelessWidget {\n  const MyApp({super.key});\n\n  @override\n  Widget build(BuildContext context) {\n    return const MaterialApp(home: Scaffold());\n  }\n}\n";
    }
    final cls = stem.split('_').where((w) => w.isNotEmpty).map((w) => w[0].toUpperCase() + w.substring(1)).join();
    return 'class ${cls.isEmpty ? 'Untitled' : cls} {\n  \n}\n';
  }
  if (ext == 'html' || ext == 'htm') {
    return '<!DOCTYPE html>\n<html lang="en">\n<head>\n  <meta charset="utf-8">\n  <meta name="viewport" content="width=device-width, initial-scale=1">\n  <title>${stem.isEmpty ? 'My app' : stem}</title>\n  <link rel="stylesheet" href="style.css">\n</head>\n<body>\n  \n  <script src="script.js"></script>\n</body>\n</html>\n';
  }
  if (ext == 'css') return '* {\n  box-sizing: border-box;\n}\n\nbody {\n  margin: 0;\n  font-family: system-ui, sans-serif;\n}\n';
  if (ext == 'js' || ext == 'mjs') return "'use strict';\n\n";
  if (ext == 'xml' && path.contains('/layout/')) {
    return '<?xml version="1.0" encoding="utf-8"?>\n<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"\n    android:layout_width="match_parent"\n    android:layout_height="match_parent"\n    android:orientation="vertical">\n\n</LinearLayout>\n';
  }
  if (ext == 'xml') return '<?xml version="1.0" encoding="utf-8"?>\n';
  return '';
}

// ---------------------------------------------------------------- check before building

typedef _Issue = (String, int, String, bool); // file, line, what is wrong, true = error / false = warning

String _joinPath(String from, String rel) {
  final parts = from.split('/')..removeLast();
  for (final s in rel.split('/')) {
    if (s == '..') {
      if (parts.isNotEmpty) parts.removeLast();
    } else if (s != '.' && s.isNotEmpty) {
      parts.add(s);
    }
  }
  return parts.join('/');
}

/// Brackets, strings and comments of Kotlin / Java / Dart / Gradle: the mistakes that always stop a build.
List<_Issue> _lexCheck(String path, String t, String ext) {
  final out = <_Issue>[];
  final dart = ext == 'dart', kt = ext == 'kt' || ext == 'kts', groovy = ext == 'gradle';
  final br = <(String, int)>[]; // open brackets: char, line
  final sq = <(String, bool, int, int)>[]; // open strings: delimiter, raw, start line, bracket depth while inside ${ } (-1 = in the text part)
  final n = t.length;
  var line = 1, i = 0;
  void err(int ln, String m) => out.add((path, ln, m, true));
  while (i < n) {
    final c = t[i];
    if (sq.isNotEmpty && sq.last.$4 < 0) {
      final f = sq.last;
      if (c == '\n') {
        if (f.$1.length == 1) {
          err(f.$3, 'String is never closed (missing ${f.$1})');
          return out;
        }
        line++;
        i++;
        continue;
      }
      if (c == '\\' && !f.$2 && !(kt && f.$1.length == 3)) {
        if (i + 1 < n && t[i + 1] == '\n') line++;
        i += 2;
        continue;
      }
      if (t.startsWith(f.$1, i)) {
        sq.removeLast();
        i += f.$1.length;
        continue;
      }
      if (c == '\$' && i + 1 < n && t[i + 1] == '{' && !f.$2 && (dart || kt || (groovy && f.$1[0] == '"'))) {
        br.add(('{', line));
        sq[sq.length - 1] = (f.$1, f.$2, f.$3, br.length);
        i += 2;
        continue;
      }
      i++;
      continue;
    }
    if (c == '\n') {
      line++;
      i++;
      continue;
    }
    if (c == '/' && i + 1 < n) {
      final d = t[i + 1];
      if (d == '/') {
        final e = t.indexOf('\n', i);
        i = e < 0 ? n : e;
        continue;
      }
      if (d == '*') {
        final start = line;
        var depth = 1, j = i + 2;
        while (j < n && depth > 0) {
          if (t.startsWith('*/', j)) {
            depth--;
            j += 2;
          } else if ((dart || kt) && t.startsWith('/*', j)) {
            depth++;
            j += 2;
          } else {
            if (t[j] == '\n') line++;
            j++;
          }
        }
        if (depth > 0) {
          err(start, 'Comment is never closed (missing */)');
          return out;
        }
        i = j;
        continue;
      }
    }
    if (c == '"' || c == "'") {
      final tri = i + 2 < n && t[i + 1] == c && t[i + 2] == c;
      final raw = dart && i > 0 && t[i - 1] == 'r' && (i < 2 || !RegExp(r'\w').hasMatch(t[i - 2]));
      sq.add((tri ? c * 3 : c, raw, line, -1));
      i += tri ? 3 : 1;
      continue;
    }
    if (c == '(' || c == '[' || c == '{') {
      br.add((c, line));
      i++;
      continue;
    }
    if (c == ')' || c == ']' || c == '}') {
      if (c == '}' && sq.isNotEmpty && sq.last.$4 >= 0 && br.length == sq.last.$4) {
        br.removeLast();
        final f = sq.last;
        sq[sq.length - 1] = (f.$1, f.$2, f.$3, -1);
        i++;
        continue;
      }
      final open = c == ')' ? '(' : (c == ']' ? '[' : '{');
      if (br.isEmpty) {
        err(line, "Extra '$c' \u2013 nothing was opened for it");
        return out;
      }
      if (br.last.$1 != open) {
        final want = br.last.$1 == '(' ? ')' : (br.last.$1 == '[' ? ']' : '}');
        err(line, "Found '$c' but expected '$want' (the '${br.last.$1}' from line ${br.last.$2} is still open)");
        return out;
      }
      br.removeLast();
      i++;
      continue;
    }
    i++;
  }
  if (sq.isNotEmpty) {
    err(sq.last.$3, 'String is never closed (missing ${sq.last.$1})');
    return out;
  }
  if (br.isNotEmpty) {
    final b = br.last;
    final want = b.$1 == '(' ? ')' : (b.$1 == '[' ? ']' : '}');
    err(b.$2, "'${b.$1}' is never closed (missing '$want')");
  }
  return out;
}

/// Is the XML well formed (every tag closed, attributes quoted)? aapt stops on the first mistake here.
List<_Issue> _xmlCheck(String path, String t) {
  final out = <_Issue>[];
  final st = <(String, int)>[];
  final n = t.length;
  final nameRe = RegExp(r'[A-Za-z_][\w:.\-]*');
  var i = 0, line = 1;
  int nl(int a, int b) {
    var c = 0;
    for (var k = a; k < b; k++) {
      if (t.codeUnitAt(k) == 10) c++;
    }
    return c;
  }

  void err(int ln, String m) => out.add((path, ln, m, true));
  int skipWs(int j) {
    while (j < n && ' \t\r\n'.contains(t[j])) {
      if (t[j] == '\n') line++;
      j++;
    }
    return j;
  }

  while (i < n) {
    final lt = t.indexOf('<', i);
    if (lt < 0) break;
    line += nl(i, lt);
    i = lt;
    if (t.startsWith('<!--', i) || t.startsWith('<![CDATA[', i) || t.startsWith('<?', i)) {
      final isC = t.startsWith('<!--', i);
      final isD = t.startsWith('<![CDATA[', i);
      final end = isC ? '-->' : (isD ? ']]>' : '?>');
      final e = t.indexOf(end, i + 2);
      if (e < 0) {
        err(line, isC ? 'Comment is never closed (missing -->)' : (isD ? 'CDATA is never closed (missing ]]>)' : 'The <? ?> header is never closed'));
        return out;
      }
      line += nl(i, e);
      i = e + end.length;
      continue;
    }
    if (t.startsWith('<!', i)) {
      final e = t.indexOf('>', i);
      if (e < 0) return out;
      line += nl(i, e);
      i = e + 1;
      continue;
    }
    final startLine = line;
    final close = t.startsWith('</', i);
    var j = i + (close ? 2 : 1);
    final nm = nameRe.matchAsPrefix(t, j);
    if (nm == null) {
      err(line, "A lone '<' \u2013 write &lt; if you meant the character");
      return out;
    }
    final name = nm.group(0)!;
    j = nm.end;
    if (close) {
      j = skipWs(j);
      if (j >= n || t[j] != '>') {
        err(startLine, '</$name is missing its >');
        return out;
      }
      if (st.isEmpty) {
        err(startLine, '</$name> has no matching opening tag');
        return out;
      }
      if (st.last.$1 != name) {
        err(startLine, '</$name> doesn\u2019t match <${st.last.$1}> from line ${st.last.$2}');
        return out;
      }
      st.removeLast();
      i = j + 1;
      continue;
    }
    var self = false;
    while (true) {
      j = skipWs(j);
      if (j >= n) {
        err(startLine, '<$name> is never finished (missing >)');
        return out;
      }
      if (t[j] == '>') {
        j++;
        break;
      }
      if (t.startsWith('/>', j)) {
        self = true;
        j += 2;
        break;
      }
      final a = nameRe.matchAsPrefix(t, j);
      if (a == null) {
        err(line, "Unexpected '${t[j]}' inside <$name>");
        return out;
      }
      j = skipWs(a.end);
      if (j >= n || t[j] != '=') {
        err(line, "Attribute '${a.group(0)}' in <$name> has no value");
        return out;
      }
      j = skipWs(j + 1);
      if (j >= n || (t[j] != '"' && t[j] != "'")) {
        err(line, "Attribute '${a.group(0)}' needs its value in quotes");
        return out;
      }
      final e = t.indexOf(t[j], j + 1);
      if (e < 0) {
        err(line, 'A quote opened in <$name> is never closed');
        return out;
      }
      line += nl(j, e);
      j = e + 1;
    }
    if (!self) st.add((name, startLine));
    i = j;
  }
  if (st.isNotEmpty) err(st.last.$2, '<${st.last.$1}> is never closed');
  return out;
}

const _libRes = ['abc_', 'material_', 'mtrl_', 'design_', 'm3_', 'common_', 'mr_', 'notification_', 'exo_', 'fragment_', 'preference_', 'androidx_', 'appbar_', 'googleg_', 'google_', 'firebase_', 'gcm_', 'default_web_client_id', 'ic_launcher'];
bool _libName(String n) => n.contains('material') || n.contains('mtrl') || _libRes.any(n.startsWith);

bool pubsExist(Map<String, List<int>> files) => files.keys.any((k) => k == 'pubspec.yaml' || k.endsWith('/pubspec.yaml'));

/// Everything the build would trip over that can be seen without compiling. Runs in a background isolate.
List<_Issue> _checkProject(Map<String, List<int>> files) {
  final all = <_Issue>[];
  String text(String k) => utf8.decode(files[k]!, allowMalformed: true);
  final keys = files.keys.where((k) => _isText(k, files[k]!) && files[k]!.length <= 1500000).toList()..sort(_cmp);
  final has = files.keys.toSet();

  // Android resources that exist in the project.
  final hasRes = files.keys.any((k) => RegExp(r'(^|/)res/[^/]+/[^/]+$').hasMatch(k));
  final defined = <String>{};
  if (hasRes) {
    final resRe = RegExp(r'(?:^|/)res/([^/]+)/([^/]+)$');
    final idRe = RegExp(r'@\+id/([\w.]+)');
    final tagRe = RegExp(r'<([\w-]+)\s([^>]*)>');
    final nameRe = RegExp(r'name\s*=\s*[\x22\x27]([^\x22\x27]+)');
    final typeRe = RegExp(r'type\s*=\s*[\x22\x27](\w+)');
    for (final k in keys) {
      final m = resRe.firstMatch(k);
      final isXml = _ext(k) == 'xml';
      if (m != null) {
        final type = m.group(1)!.split('-').first;
        if (type == 'values' && isXml) {
          for (final tg in tagRe.allMatches(text(k))) {
            final tag = tg.group(1)!, attrs = tg.group(2)!;
            final nm = nameRe.firstMatch(attrs)?.group(1);
            if (nm == null) continue;
            final t = tag == 'string-array' || tag == 'integer-array' ? 'array' : (tag == 'item' ? (typeRe.firstMatch(attrs)?.group(1) ?? '') : tag);
            if (t.isNotEmpty) defined.add('$t/${nm.replaceAll('.', '_')}');
          }
        } else {
          defined.add('$type/${m.group(2)!.split('.').first}');
        }
      }
      if (isXml) {
        for (final d in idRe.allMatches(text(k))) {
          defined.add('id/${d.group(1)!.replaceAll('.', '_')}');
        }
      }
    }
  }
  final refRe = RegExp(r'@(drawable|mipmap|layout|string|color|dimen|menu|xml|raw|anim|font|integer|bool|array|plurals|id)/([\w.]+)');
  final rRe = RegExp(r'(?<![\w.])R\.(drawable|layout|string|color|id|mipmap|menu|raw|xml|anim|dimen|integer|bool|array|plurals|font)\.(\w+)');
  void scanRefs(List<_Issue> into, String path, String src, RegExp re, bool xmlStyle) {
    final ls = src.split('\n');
    for (var i = 0; i < ls.length; i++) {
      if (!xmlStyle) {
        final tr = ls[i].trimLeft();
        if (tr.startsWith('//') || tr.startsWith('*') || tr.startsWith('/*')) continue;
      }
      for (final m in re.allMatches(ls[i])) {
        final nm = m.group(2)!.replaceAll('.', '_');
        if (_libName(nm)) continue;
        if (!defined.contains('${m.group(1)}/$nm')) into.add((path, i + 1, 'Missing resource: ${m.group(1)}/${m.group(2)}', true));
      }
    }
  }

  // A website needs an index.html to open.
  final isWebProject = !pubsExist(files) && !files.keys.any((k) => k.endsWith('AndroidManifest.xml') || k.endsWith('.kt') || k.endsWith('.java')) && files.keys.any((k) => const {'html', 'htm'}.contains(_ext(k)));
  if (isWebProject && !files.keys.any((k) => k == 'index.html' || (k.split('/').length == 2 && k.endsWith('/index.html')))) {
    all.add((files.keys.firstWhere((k) => const {'html', 'htm'}.contains(_ext(k))), 1, 'A website needs a file called index.html at the top. Rename your main page to index.html.', true));
  }

  // Flutter / Dart project facts.
  String? pubName;
  var pubRoot = '';
  final pubKeys = <String>{};
  final pubs = files.keys.where((k) => k == 'pubspec.yaml' || k.endsWith('/pubspec.yaml')).toList()..sort((a, b) => a.length.compareTo(b.length));
  if (pubs.isNotEmpty) {
    final s = text(pubs.first);
    pubName = RegExp(r'^name:\s*(\w+)', multiLine: true).firstMatch(s)?.group(1);
    pubRoot = pubs.first.substring(0, pubs.first.length - 'pubspec.yaml'.length);
    for (final m in RegExp(r'^[ \t]+([A-Za-z_]\w*)\s*:', multiLine: true).allMatches(s)) {
      pubKeys.add(m.group(1)!);
    }
  }
  final dartRe = RegExp(r'^\s*(?:import|export|part)\s+[\x22\x27]([^\x22\x27]+)[\x22\x27]', multiLine: true);
  final genRe = RegExp(r'\.(?:g|freezed|gr|config|mocks)\.dart$|generated_plugin_registrant\.dart$');

  for (final k in keys) {
    final ext = _ext(k);
    final src = text(k);
    final found = <_Issue>[];
    if (const {'kt', 'kts', 'java', 'dart', 'gradle'}.contains(ext)) found.addAll(_lexCheck(k, src, ext));
    if (ext == 'java') {
      final stem = k.split('/').last.replaceAll('.java', '');
      final pub = RegExp(r'^public\s+(?:(?:final|abstract|static|sealed)\s+)*(?:class|interface|enum|record)\s+(\w+)', multiLine: true);
      for (final m in pub.allMatches(src)) {
        if (m.group(1) != stem) found.add((k, _lineOf(src, m.start) + 1, 'Public class ${m.group(1)} must be in a file called ${m.group(1)}.java', true));
      }
    }
    if (ext == 'dart') {
      for (final m in dartRe.allMatches(src)) {
        final u = m.group(1)!;
        final ln = _lineOf(src, m.start) + 1;
        if (u.startsWith('dart:') || genRe.hasMatch(u)) continue;
        if (u.startsWith('package:')) {
          final rest = u.substring(8);
          final slash = rest.indexOf('/');
          if (slash < 0) continue;
          final pk = rest.substring(0, slash), sub = rest.substring(slash + 1);
          if (pk == 'flutter_gen') continue;
          if (pubName != null && pk == pubName) {
            if (!has.contains('${pubRoot}lib/$sub')) found.add((k, ln, 'File not found: lib/$sub', true));
          } else if (pubKeys.isNotEmpty && !pubKeys.contains(pk)) {
            found.add((k, ln, "Package '$pk' isn\u2019t listed in pubspec.yaml", true));
          }
          continue;
        }
        if (u.contains(':')) continue;
        if (!has.contains(_joinPath(k, u))) found.add((k, ln, 'File not found: $u', true));
      }
    }
    if (ext == 'json' && src.trim().isNotEmpty) {
      try {
        jsonDecode(src);
      } on FormatException catch (e) {
        found.add((k, e.offset == null ? 1 : _lineOf(src, e.offset!) + 1, 'Not valid JSON: ${e.message}', true));
      }
    }
    if (ext == 'yaml' || ext == 'yml') {
      final ls = src.split('\n');
      for (var i = 0; i < ls.length; i++) {
        if (RegExp(r'^ *\t').hasMatch(ls[i])) {
          found.add((k, i + 1, 'YAML can\u2019t be indented with tabs \u2013 use spaces', true));
          break;
        }
      }
    }
    if (ext == 'xml') {
      final bad = _xmlCheck(k, src);
      found.addAll(bad);
      final isManifest = k.endsWith('AndroidManifest.xml');
      if (bad.isEmpty && hasRes && (isManifest || k.startsWith('res/') || k.contains('/res/'))) {
        final clean = src.replaceAllMapped(RegExp(r'<!--[\s\S]*?-->'), (m) => m.group(0)!.replaceAll(RegExp(r'[^\n]'), ' '));
        scanRefs(found, k, clean, refRe, true);
      }
      if (isManifest) {
        final act = RegExp(r'<(?:activity|service|receiver|provider|application)\b[^>]*?android:name\s*=\s*"(\.[\w.]+)"');
        for (final m in act.allMatches(src)) {
          final cls = m.group(1)!.substring(1).replaceAll('.', '/');
          final ok = has.any((f) => f == '$cls.kt' || f == '$cls.java' || f.endsWith('/$cls.kt') || f.endsWith('/$cls.java'));
          if (!ok) found.add((k, _lineOf(src, m.start) + 1, 'No source file for ${m.group(1)} \u2013 the app would crash when it starts', false));
        }
      }
    }
    if (hasRes && (ext == 'kt' || ext == 'java')) scanRefs(found, k, src, rRe, false);
    all.addAll(found.take(5));
    if (all.length >= 100) break;
  }
  return [...all.where((x) => x.$4), ...all.where((x) => !x.$4)];
}

// ---------------------------------------------------------------- the file editor

const _kw = {
  'package', 'import', 'class', 'interface', 'object', 'enum', 'fun', 'val', 'var', 'if', 'else', 'when', 'for', 'while', 'do', 'return', 'break', 'continue', 'try', 'catch', 'finally', 'throw',
  'new', 'this', 'super', 'null', 'true', 'false', 'is', 'in', 'as', 'override', 'private', 'public', 'protected', 'internal', 'static', 'final', 'abstract', 'open', 'data', 'sealed', 'companion',
  'lateinit', 'by', 'lazy', 'suspend', 'void', 'int', 'long', 'float', 'double', 'boolean', 'char', 'byte', 'short', 'extends', 'implements', 'throws', 'instanceof', 'switch', 'case', 'default',
  'synchronized', 'volatile', 'const', 'typealias', 'init', 'get', 'set', 'plugins', 'dependencies', 'android', 'defaultConfig', 'buildTypes', 'function', 'let', 'async', 'await',
};
const _dartKw = {
  'abstract', 'as', 'assert', 'async', 'await', 'base', 'break', 'case', 'catch', 'class', 'const', 'continue', 'covariant', 'default', 'deferred', 'do', 'dynamic', 'else', 'enum', 'export',
  'extends', 'extension', 'external', 'factory', 'false', 'final', 'finally', 'for', 'get', 'hide', 'if', 'implements', 'import', 'in', 'interface', 'is', 'late', 'library', 'mixin', 'new', 'null',
  'on', 'operator', 'part', 'required', 'rethrow', 'return', 'sealed', 'set', 'show', 'static', 'super', 'switch', 'sync', 'this', 'throw', 'true', 'try', 'typedef', 'var', 'void', 'when', 'while',
  'with', 'yield',
};
const _dartTypes = {'int', 'double', 'num', 'bool', 'String', 'List', 'Map', 'Set', 'Future', 'Stream', 'Object', 'Function', 'Never', 'Iterable'};
const _vsEditor = Color(0xFF1E1E1E), _vsSide = Color(0xFF252526), _vsTitle = Color(0xFF323233), _vsTabOff = Color(0xFF2D2D2D),
    _vsBlue = Color(0xFF007ACC), _vsBtn = Color(0xFF0E639C), _vsText = Color(0xFFD4D4D4), _vsDim = Color(0xFF858585), _vsSel = Color(0xFF37373D), _vsLine = Color(0xFF3C3C3C);
const _cXml = Color(0xFF569CD6), _cAttr = Color(0xFF9CDCFE);
const _cKw = Color(0xFF569CD6), _cStr = Color(0xFFCE9178), _cCom = Color(0xFF6A9955), _cFn = Color(0xFFDCDCAA), _cType = Color(0xFF4EC9B0), _cNum = Color(0xFFB5CEA8), _cTag = Color(0xFFF48771);

final _reCode = RegExp(r'//[^\n]*|/\*[\s\S]*?(?:\*/|$)|"(?:\\.|[^"\\\n])*"?|' "'(?:\\\\.|[^'\\\\\\n])*'?" r'|`(?:\\.|[^`\\])*`?|@\w+|\b\d[\w.]*|\b[A-Za-z_]\w*');
final _reDart = RegExp(
  r'//[^\n]*|/\*[\s\S]*?(?:\*/|$)|'
  r"r?'''[\s\S]*?(?:'''|$)|"
  r'r?"""[\s\S]*?(?:"""|$)|'
  r'r?"(?:\\.|[^"\\\n])*"?|'
  r"r?'(?:\\.|[^'\\\n])*'?|"
  r'@\w+|\b\d[\w.]*|\b[A-Za-z_]\w*',
);
final _reXml = RegExp(r'<!--[\s\S]*?(?:-->|$)|"[^"\n]*"?|</?[\w:.\-]+|/?>|[\w:.\-]+(?==)');
final _reData = RegExp(r'#[^\n]*|"(?:\\.|[^"\\\n])*"?|\b\d[\w.]*|\b(?:true|false|null)\b');

const _xmlExts = {'xml', 'html', 'htm', 'svg', 'xhtml', 'xsd', 'xsl', 'fxml', 'vue', 'svelte'};
const _cExts = {'kt', 'kts', 'java', 'gradle', 'js', 'mjs', 'cjs', 'jsx', 'ts', 'tsx', 'c', 'cpp', 'cc', 'cxx', 'h', 'hpp', 'css', 'scss', 'sass', 'less', 'go', 'rs', 'swift', 'cs', 'php', 'scala', 'groovy', 'proto', 'glsl', 'vert', 'frag'};
const _hashExts = {'py', 'sh', 'bash', 'zsh', 'rb', 'pl', 'r', 'mk', 'dockerfile', 'cmake', 'ps1', 'jl', 'ex', 'exs', 'nim', 'cr', 'pro', 'gitignore', 'gitattributes'};
const _confExts = {'yaml', 'yml', 'toml', 'ini', 'cfg', 'conf', 'properties', 'env', 'editorconfig', 'gitconfig'};
const _jsonExts = {'json', 'jsonc', 'json5'};

const _kwMore = {
  'func', 'defer', 'chan', 'fallthrough', 'fn', 'mut', 'pub', 'impl', 'trait', 'crate', 'Self', 'self', 'unsafe', 'dyn', 'guard', 'protocol', 'extension', 'deinit', 'inout', 'nil', 'rethrows',
  'namespace', 'using', 'readonly', 'virtual', 'partial', 'foreach', 'elseif', 'endif', 'typeof', 'declare', 'undefined', 'union', 'typedef', 'sizeof', 'typename', 'inline', 'extern', 'auto',
  'signed', 'unsigned', 'nullptr', 'operator', 'friend', 'mutable', 'explicit', 'constexpr', 'yield', 'NaN', 'struct', 'echo', 'isset', 'export', 'delete', 'with',
};
final _kwC = <String>{..._kw, ..._kwMore};
const _kwHash = {
  'def', 'class', 'import', 'from', 'as', 'if', 'elif', 'else', 'for', 'while', 'in', 'not', 'and', 'or', 'is', 'return', 'yield', 'lambda', 'with', 'try', 'except', 'finally', 'raise', 'pass',
  'break', 'continue', 'global', 'nonlocal', 'None', 'True', 'False', 'self', 'async', 'await', 'del', 'assert', 'print', 'echo', 'fi', 'then', 'done', 'esac', 'case', 'function', 'export',
  'local', 'set', 'unset', 'source', 'exit', 'readonly', 'do', 'until', 'alias', 'cd', 'end', 'module', 'begin', 'rescue', 'ensure', 'unless', 'nil', 'require', 'puts', 'elsif', 'FROM', 'RUN',
  'CMD', 'COPY', 'ENV', 'ARG', 'WORKDIR', 'EXPOSE', 'ENTRYPOINT', 'my', 'our', 'sub', 'use', 'package', 'foreach', 'let', 'fn',
};
const _luaKw = {'and', 'break', 'do', 'else', 'elseif', 'end', 'false', 'for', 'function', 'goto', 'if', 'in', 'local', 'nil', 'not', 'or', 'repeat', 'return', 'then', 'true', 'until', 'while'};
const _sqlKw = {
  'SELECT', 'FROM', 'WHERE', 'INSERT', 'INTO', 'VALUES', 'UPDATE', 'SET', 'DELETE', 'CREATE', 'TABLE', 'ALTER', 'DROP', 'INDEX', 'PRIMARY', 'KEY', 'FOREIGN', 'REFERENCES', 'NOT', 'NULL', 'AND', 'OR',
  'IN', 'LIKE', 'JOIN', 'LEFT', 'RIGHT', 'INNER', 'OUTER', 'ON', 'AS', 'ORDER', 'BY', 'GROUP', 'HAVING', 'LIMIT', 'OFFSET', 'DISTINCT', 'UNION', 'ALL', 'CASE', 'WHEN', 'THEN', 'ELSE', 'END', 'IS',
  'BETWEEN', 'EXISTS', 'DEFAULT', 'UNIQUE', 'CHECK', 'VIEW', 'TRIGGER', 'BEGIN', 'COMMIT', 'ROLLBACK', 'TRANSACTION', 'PRAGMA', 'IF', 'AUTOINCREMENT', 'INTEGER', 'TEXT', 'REAL', 'BLOB', 'VARCHAR',
  'INT', 'BOOLEAN', 'ASC', 'DESC', 'COUNT', 'SUM', 'AVG', 'MIN', 'MAX',
};
const _logErr = {'ERROR', 'FATAL', 'SEVERE', 'Exception', 'FAILED', 'FAILURE'};
const _logWarn = {'WARN', 'WARNING', 'Warning'};
Set<String> _kwFor(String ext) => ext == 'dart' ? _dartKw : (_hashExts.contains(ext) ? _kwHash : (ext == 'sql' ? _sqlKw : (ext == 'lua' ? _luaKw : _kwC)));
String _lineTag(String ext) => ext == 'ini' ? ';' : ((ext == 'sql' || ext == 'lua') ? '--' : ((_hashExts.contains(ext) || _confExts.contains(ext)) ? '#' : '//'));

final _reHash = RegExp(
  r'#[^\n]*|"""[\s\S]*?(?:"""|$)|'
  r"'''[\s\S]*?(?:'''|$)|"
  r'"(?:\\.|[^"\\\n])*"?|'
  r"'(?:\\.|[^'\\\n])*'?|"
  r'@\w+|\$\{?\w+\}?|\b\d[\w.]*|\b[A-Za-z_]\w*',
);
final _reSql = RegExp(
  r'--[^\n]*|/\*[\s\S]*?(?:\*/|$)|'
  r"'(?:''|[^'])*'?|"
  r'"(?:""|[^"])*"?|'
  r'`[^`\n]*`?|\b\d[\w.]*|\b[A-Za-z_]\w*',
);
final _reMd = RegExp(
  r'```[\s\S]*?(?:```|(?![\s\S]))|<!--[\s\S]*?(?:-->|(?![\s\S]))|^#{1,6}[ \t].*$|`[^`\n]+`|\*\*[^*\n]+\*\*|__[^_\n]+__|!?\[[^\]\n]*\]\([^)\n]*\)|^[ \t]*(?:[-*+]|\d+\.)[ \t]|^>.*$',
  multiLine: true,
);
final _reConf = RegExp(
  r'^[ \t]*[#;][^\n]*|[ \t]#[^\n]*|^\[[^\]\n]*\]|"(?:\\.|[^"\\\n])*"?|'
  r"'(?:\\.|[^'\\\n])*'?|"
  r'\b\d[\w.]*|\b(?:true|false|null|yes|no|on|off)\b|[\w.\-]+(?=[ \t]*[:=])',
  multiLine: true,
);
final _reSmali = RegExp(
  r'#[^\n]*|"(?:\\.|[^"\\\n])*"?|\.[a-z][\w-]*|\b[vp]\d+\b|L[\w/$]+;|-?\b0x[0-9a-fA-F]+\b|-?\b\d+\b|'
  r'\b(?:invoke|move|const|return|if|goto|iget|iput|sget|sput|new|check|instance|throw|nop|add|sub|mul|div|rem|and|or|xor|shl|shr|cmp|aget|aput|array|fill|monitor|neg|not|packed|sparse|rsub|int|long|float|double)[\w/-]*',
);
final _reAny = RegExp(
  r'\bhttps?://[^\s"' "')\\]]+" r'|#[^\n]*|//[^\n]*|/\*[\s\S]*?(?:\*/|$)|"(?:\\.|[^"\\\n])*"?|'
  r'\b(?:ERROR|FATAL|SEVERE|Exception|FAILED|FAILURE|WARN|WARNING|Warning|INFO|DEBUG|SUCCESS)\b|\b\d[\w.]*',
);

bool _keyAfter(String t, int i) {
  while (i < t.length && (t[i] == ' ' || t[i] == '\t')) {
    i++;
  }
  return i < t.length && t[i] == ':';
}

// HTML pages hold JavaScript in <script> and CSS in <style>: find those blocks so each gets its own colours.
final _reBlock = RegExp(r'(<(script|style)\b[^>]*>)([\s\S]*?)(?=</\2\s*>|$)', caseSensitive: false);
String? _blkText;
List<(int, int, String)> _blkList = const [];

List<(int, int, String)> _blocks(String t) {
  if (identical(t, _blkText)) return _blkList;
  final out = <(int, int, String)>[];
  for (final m in _reBlock.allMatches(t)) {
    final start = m.start + m.group(1)!.length;
    if (m.end <= start) continue;
    final open = m.group(1)!.toLowerCase();
    String lang;
    if (m.group(2)!.toLowerCase() == 'style') {
      lang = 'css';
    } else if (open.contains('json')) {
      lang = 'json';
    } else if (open.contains('template') || open.contains('text/html') || open.contains('x-handlebars')) {
      continue;
    } else {
      lang = 'js';
    }
    out.add((start, m.end, lang));
  }
  _blkText = t;
  return _blkList = out;
}

/// Colours t[from, to) of an HTML-like file: tags and attributes as markup, script and style bodies as code.
List<InlineSpan> _highlightXml(String t, int from, int to, String ext, TextStyle? base) {
  final out = <InlineSpan>[];
  var pos = from;
  for (final b in _blocks(t)) {
    if (b.$2 <= from) continue;
    if (b.$1 >= to) break;
    final s = b.$1 < from ? from : b.$1;
    final e = b.$2 > to ? to : b.$2;
    if (s > pos) out.addAll(_highlight(t.substring(pos, s), ext, base));
    out.addAll(_highlight(t.substring(s, e), b.$3, base));
    pos = e;
  }
  if (pos < to) out.addAll(_highlight(t.substring(pos, to), ext, base));
  return out;
}


List<InlineSpan> _highlight(String t, String ext, TextStyle? base) {
  final xml = _xmlExts.contains(ext);
  final hash = _hashExts.contains(ext);
  final dash = ext == 'sql' || ext == 'lua';
  final json = _jsonExts.contains(ext);
  final md = ext == 'md' || ext == 'markdown';
  final conf = _confExts.contains(ext);
  final smali = ext == 'smali';
  final cLike = _cExts.contains(ext);
  final code = ext == 'dart' || cLike || hash || dash;
  final any = !xml && !code && !json && !md && !conf && !smali;
  final RegExp re = xml
      ? _reXml
      : ext == 'dart'
          ? _reDart
          : cLike
              ? _reCode
              : hash
                  ? _reHash
                  : dash
                      ? _reSql
                      : md
                          ? _reMd
                          : conf
                              ? _reConf
                              : smali
                                  ? _reSmali
                                  : json
                                      ? _reData
                                      : _reAny;
  final kws = _kwFor(ext);
  final out = <InlineSpan>[];
  var last = 0;
  for (final m in re.allMatches(t)) {
    if (m.start > last) out.add(TextSpan(text: t.substring(last, m.start)));
    final s = m.group(0)!;
    Color? c;
    if (xml) {
      c = s.startsWith('<!--') ? _cCom : s.startsWith('"') ? _cStr : (s.startsWith('<') ? _cXml : (s == '>' || s == '/>' ? _cXml : _cAttr));
    } else if (md) {
      c = s.startsWith('<!--') ? _cCom : (s.startsWith('#') ? _cFn : (s.startsWith('>') ? _cCom : (s.startsWith('`') ? _cStr : ((s.startsWith('**') || s.startsWith('__')) ? _cType : ((s.startsWith('[') || s.startsWith('![')) ? _cKw : _cNum)))));
    } else if (conf) {
      final st = s.trimLeft();
      if (st.startsWith('#') || st.startsWith(';')) {
        c = _cCom;
      } else if (st.startsWith('[')) {
        c = _cType;
      } else if (st.startsWith('"') || st.startsWith("'")) {
        c = _cStr;
      } else if (_reNumStart.hasMatch(st)) {
        c = _cNum;
      } else if (_reBoolWord.hasMatch(st)) {
        c = _cKw;
      } else {
        c = _cFn;
      }
    } else if (smali) {
      c = s.startsWith('#')
          ? _cCom
          : (s.startsWith('"')
              ? _cStr
              : (s.startsWith('.')
                  ? _cKw
                  : ((s.startsWith('L') && s.endsWith(';'))
                      ? _cType
                      : (_reSmaliReg.hasMatch(s) ? _cTag : (_reSmaliNum.hasMatch(s) ? _cNum : _cFn)))));
    } else if (any) {
      if (s.startsWith('#') || s.startsWith('//') || s.startsWith('/*')) {
        c = _cCom;
      } else if (s.startsWith('"')) {
        c = _cStr;
      } else if (s.startsWith('http')) {
        c = _cFn;
      } else if (_reNumStart.hasMatch(s)) {
        c = _cNum;
      } else if (_logErr.contains(s)) {
        c = _cTag;
      } else if (_logWarn.contains(s)) {
        c = _cNum;
      } else {
        c = _cFn;
      }
    } else if (s.startsWith('//') || s.startsWith('/*') || s.startsWith('#') || s.startsWith('--')) {
      c = _cCom;
    } else if (s.startsWith('"') || s.startsWith("'") || s.startsWith('`') || (ext == 'dart' && s.length > 1 && s[0] == 'r' && (s[1] == '"' || s[1] == "'"))) {
      c = (json && _keyAfter(t, m.end)) ? _cAttr : _cStr;
    } else if (s.startsWith('@')) {
      c = _cType;
    } else if (s.startsWith(r'$')) {
      c = _cAttr;
    } else if (_reNumStart.hasMatch(s)) {
      c = _cNum;
    } else if (kws.contains(ext == 'sql' ? s.toUpperCase() : s)) {
      c = _cKw;
    } else if (ext == 'dart' && _dartTypes.contains(s)) {
      c = _cType;
    } else if (code && !dash && s.codeUnitAt(0) >= 65 && s.codeUnitAt(0) <= 90) {
      c = _cType;
    } else if (code && m.end < t.length && t[m.end] == '(') {
      c = _cFn;
    } else if (!code) {
      c = _cKw;
    }
    out.add(c == null ? TextSpan(text: s) : TextSpan(text: s, style: TextStyle(color: c)));
    last = m.end;
  }
  if (last < t.length) out.add(TextSpan(text: t.substring(last)));
  return out;
}

class _CodeController extends TextEditingController {
  final String ext;
  _CodeController(String text, this.ext) : super(text: text);

  /// Files up to this many characters are coloured in full; bigger ones only around the lines on screen.
  static const _fullMax = 20000;
  /// Lines coloured above and below the screen, so a small scroll never shows uncoloured text.
  static const _margin = 80;

  String? _cText;
  TextStyle? _cStyle;
  TextSpan? _cSpan;
  int _cFrom = -1, _cTo = -1;

  // Where each line starts, rebuilt only when the text changes.
  String? _lsText;
  List<int> _ls = const [0];
  List<int> get lineStarts {
    final t = text;
    if (!identical(t, _lsText)) {
      _lsText = t;
      final out = <int>[0];
      for (var i = 0; i < t.length; i++) {
        if (t.codeUnitAt(i) == 10) out.add(i + 1);
      }
      _ls = out;
    }
    return _ls;
  }

  /// 0-based line that the offset [off] sits on.
  int lineAt(int off) {
    final ls = lineStarts;
    var lo = 0, hi = ls.length - 1;
    while (lo < hi) {
      final mid = (lo + hi + 1) >> 1;
      if (ls[mid] <= off) {
        lo = mid;
      } else {
        hi = mid - 1;
      }
    }
    return lo;
  }

  // The band of lines currently coloured (0-based, inclusive).
  int _winFirst = -1, _winLast = -1;

  /// Tells the controller which lines are on screen (0-based). Colours are only recomputed when the
  /// screen leaves the coloured band, or when [force] is set (after an edit or a zoom change).
  void setView(int first, int last, {bool force = false}) {
    final n = lineStarts.length;
    final f = first.clamp(0, n - 1).toInt();
    final l = last.clamp(0, n - 1).toInt();
    if (!force && _winFirst >= 0 && f >= _winFirst && l <= _winLast) return;
    _winFirst = (f - _margin).clamp(0, n - 1).toInt();
    _winLast = (l + _margin).clamp(0, n - 1).toInt();
    notifyListeners();
  }

  bool get _mixed => const {'html', 'htm', 'xhtml', 'vue', 'svelte', 'svg'}.contains(ext);

  @override
  TextSpan buildTextSpan({required BuildContext context, TextStyle? style, required bool withComposing}) {
    final t = text;
    var from = 0, to = t.length;
    if (t.length > _fullMax) {
      final ls = lineStarts;
      if (_winFirst < 0) {
        // Nothing reported yet (first frame): colour the top of the file.
        _winFirst = 0;
        _winLast = 2 * _margin;
      }
      final wf = _winFirst.clamp(0, ls.length - 1).toInt();
      final wl = _winLast.clamp(0, ls.length - 1).toInt();
      from = ls[wf];
      to = wl + 1 < ls.length ? ls[wl + 1] : t.length;
      if (to - from > _maxColourChars) to = from + _maxColourChars; // very long lines: never colour megabytes at once
    }
    // Moving the cursor or selecting must not re-colour anything: reuse the last result.
    if (identical(t, _cText) && style == _cStyle && from == _cFrom && to == _cTo && _cSpan != null) return _cSpan!;
    _cText = t;
    _cStyle = style;
    _cFrom = from;
    _cTo = to;
    if (from == 0 && to == t.length) {
      return _cSpan = TextSpan(style: style, children: _mixed ? _highlightXml(t, 0, t.length, ext, style) : _highlight(t, ext, style));
    }
    // Plain text outside the band keeps the text exactly as it is, so the caret maths is unchanged.
    return _cSpan = TextSpan(style: style, children: [
      if (from > 0) TextSpan(text: t.substring(0, from)),
      ..._mixed ? _highlightXml(t, from, to, ext, style) : _highlight(t.substring(from, to), ext, style),
      if (to < t.length) TextSpan(text: t.substring(to)),
    ]);
  }
}

int _lineOf(String t, int off) {
  final e = off.clamp(0, t.length).toInt();
  var n = 0;
  for (var i = 0; i < e; i++) {
    if (t.codeUnitAt(i) == 10) n++;
  }
  return n;
}

// Compiled once: the highlighter used to build these for every single token it coloured.
final _reNumStart = RegExp(r'^\d');
final _reSmaliReg = RegExp(r'^[vp]\d+$');
final _reSmaliNum = RegExp(r'^-?(?:0x[\da-fA-F]+|\d+)$');
final _reBoolWord = RegExp(r'^(?:true|false|null|yes|no|on|off)$');

// The real height of one line of code. Flutter rounds a line's ascent and descent, so a font of 13 with
// height 1.5 is drawn 20 px tall, not 19.5. Line numbers and the cursor highlight use this measured
// number, so they stay on the same line as the text however far down the file you are.
double _lhVal = 0, _lhFs = -1, _lhScale = -1;

double measureLineHeight(double fs, TextScaler scaler) {
  final sc = scaler.scale(fs);
  if (_lhVal > 0 && _lhFs == fs && _lhScale == sc) return _lhVal;
  final tp = TextPainter(
    text: TextSpan(text: 'A\nA', style: TextStyle(fontFamily: 'monospace', fontSize: fs, height: 1.5)),
    textDirection: TextDirection.ltr,
    textScaler: scaler,
  )..layout();
  final h = tp.height / 2;
  tp.dispose();
  _lhFs = fs;
  _lhScale = sc;
  return _lhVal = h > 0 ? h : fs * 1.5;
}

double _cwVal = 0, _cwFs = -1, _cwScale = -1;

/// The real width of one monospace character (the suggestion popup lines up with the cursor using it).
double measureCharWidth(double fs, TextScaler scaler) {
  final sc = scaler.scale(fs);
  if (_cwVal > 0 && _cwFs == fs && _cwScale == sc) return _cwVal;
  final tp = TextPainter(
    text: TextSpan(text: 'MMMMMMMMMM', style: TextStyle(fontFamily: 'monospace', fontSize: fs, height: 1.5)),
    textDirection: TextDirection.ltr,
    textScaler: scaler,
  )..layout();
  final w = tp.width / 10;
  tp.dispose();
  _cwFs = fs;
  _cwScale = sc;
  return _cwVal = w > 0 ? w : fs * .6;
}

/// The last measured line height for [fs] (before the first measure, the plain guess).
double currentLineHeight(double fs) => (_lhVal > 0 && _lhFs == fs) ? _lhVal : fs * 1.5;

/// Paints the line numbers of the lines on screen only (cheap even for huge files).
class _GutterPainter extends CustomPainter {
  final ScrollController v;
  final double lh, fs;
  final int count, cur;
  final TextScaler scaler;
  _GutterPainter(this.v, this.lh, this.fs, this.count, this.cur, this.scaler) : super(repaint: v);
  @override
  void paint(Canvas canvas, Size size) {
    final off = v.hasClients ? v.offset : 0.0;
    final first = ((off - 8) / lh).floor().clamp(0, count - 1).toInt();
    final last = ((off + size.height - 8) / lh).ceil().clamp(0, count - 1).toInt();
    // One painter reused for every line: far fewer allocations on a low-end phone.
    final tp = TextPainter(textDirection: TextDirection.ltr, textScaler: scaler);
    final on = TextStyle(fontFamily: 'monospace', fontSize: fs, height: 1.5, color: const Color(0xFFC6C6C6));
    final off2 = TextStyle(fontFamily: 'monospace', fontSize: fs, height: 1.5, color: _vsDim);
    for (var i = first; i <= last; i++) {
      tp.text = TextSpan(text: '${i + 1}', style: i == cur ? on : off2);
      tp.layout();
      tp.paint(canvas, Offset(size.width - 8 - tp.width, 8 + i * lh - off));
    }
    tp.dispose();
  }

  @override
  bool shouldRepaint(_GutterPainter o) => o.count != count || o.cur != cur || o.fs != fs || o.lh != lh || o.scaler != scaler;
}

/// Soft highlight behind the line the cursor is on.
class _BandPainter extends CustomPainter {
  final double top, h;
  _BandPainter(this.top, this.h);
  @override
  void paint(Canvas canvas, Size size) => canvas.drawRect(Rect.fromLTWH(0, top, size.width, h), Paint()..color = const Color(0x0FFFFFFF));
  @override
  bool shouldRepaint(_BandPainter o) => o.top != top || o.h != h;
}

const _m = '\u00A7'; // where the cursor lands after a snippet goes in
const _snipCode = <(String, String)>[
  ('Toast', 'Toast.makeText(this, "${_m}", Toast.LENGTH_SHORT).show()'),
  ('findViewById', 'val v = findViewById<TextView>(R.id.${_m})'),
  ('Start activity', 'startActivity(Intent(this, ${_m}::class.java))'),
  ('Click listener', 'view.setOnClickListener {\n    ${_m}\n}'),
  ('Log.d', 'Log.d("TAG", "${_m}")'),
  ('if / else', 'if (${_m}) {\n    \n} else {\n    \n}'),
  ('for loop', 'for (i in 0 until ${_m}) {\n    \n}'),
  ('when', 'when (${_m}) {\n    else -> {}\n}'),
  ('try / catch', 'try {\n    ${_m}\n} catch (e: Exception) {\n    e.printStackTrace()\n}'),
  ('fun', 'fun ${_m}() {\n    \n}'),
];
const _snipJava = <(String, String)>[
  ('Toast', 'Toast.makeText(this, "${_m}", Toast.LENGTH_SHORT).show();'),
  ('findViewById', 'TextView v = findViewById(R.id.${_m});'),
  ('Start activity', 'startActivity(new Intent(this, ${_m}.class));'),
  ('Click listener', 'view.setOnClickListener(v -> {\n    ${_m}\n});'),
  ('Log.d', 'Log.d("TAG", "${_m}");'),
  ('if / else', 'if (${_m}) {\n    \n} else {\n    \n}'),
  ('for loop', 'for (int i = 0; i < ${_m}; i++) {\n    \n}'),
  ('try / catch', 'try {\n    ${_m}\n} catch (Exception e) {\n    e.printStackTrace();\n}'),
];
const _snipXml = <(String, String)>[
  ('TextView', '<TextView\n    android:layout_width="wrap_content"\n    android:layout_height="wrap_content"\n    android:text="${_m}" />'),
  ('Button', '<Button\n    android:id="@+id/${_m}"\n    android:layout_width="wrap_content"\n    android:layout_height="wrap_content"\n    android:text="Button" />'),
  ('EditText', '<EditText\n    android:id="@+id/${_m}"\n    android:layout_width="match_parent"\n    android:layout_height="wrap_content"\n    android:hint="" />'),
  ('ImageView', '<ImageView\n    android:layout_width="wrap_content"\n    android:layout_height="wrap_content"\n    android:src="@drawable/${_m}" />'),
  ('LinearLayout', '<LinearLayout\n    android:layout_width="match_parent"\n    android:layout_height="wrap_content"\n    android:orientation="vertical">\n    ${_m}\n</LinearLayout>'),
  ('ScrollView', '<ScrollView\n    android:layout_width="match_parent"\n    android:layout_height="match_parent">\n    ${_m}\n</ScrollView>'),
  ('Permission', '<uses-permission android:name="android.permission.${_m}" />'),
  ('Activity', '<activity\n    android:name=".${_m}"\n    android:exported="false" />'),
];
const _snipDart = <(String, String)>[
  ('StatelessWidget', 'class ${_m} extends StatelessWidget {\n  const ${_m}({super.key});\n\n  @override\n  Widget build(BuildContext context) {\n    return Container();\n  }\n}'),
  ('StatefulWidget', 'class MyWidget extends StatefulWidget {\n  const MyWidget({super.key});\n\n  @override\n  State<MyWidget> createState() => _MyWidgetState();\n}\n\nclass _MyWidgetState extends State<MyWidget> {\n  @override\n  Widget build(BuildContext context) {\n    return ${_m}Container();\n  }\n}'),
  ('setState', 'setState(() {\n  ${_m}\n});'),
  ('Navigator.push', 'Navigator.of(context).push(MaterialPageRoute(builder: (_) => ${_m}()));'),
  ('print', 'print(${_m});'),
  ('if / else', 'if (${_m}) {\n  \n} else {\n  \n}'),
  ('for loop', 'for (var i = 0; i < ${_m}; i++) {\n  \n}'),
  ('for in', 'for (final x in ${_m}) {\n  \n}'),
  ('try / catch', 'try {\n  ${_m}\n} catch (e) {\n  print(e);\n}'),
  ('async function', 'Future<void> ${_m}() async {\n  \n}'),
];
const _snipHtml = <(String, String)>[
  ('Page skeleton', '<!DOCTYPE html>\n<html lang="en">\n<head>\n  <meta charset="utf-8">\n  <meta name="viewport" content="width=device-width, initial-scale=1">\n  <title>${_m}</title>\n</head>\n<body>\n  \n</body>\n</html>'),
  ('Link stylesheet', '<link rel="stylesheet" href="${_m}style.css">'),
  ('Script tag', '<script src="${_m}script.js"></script>'),
  ('Inline style', '<style>\n  ${_m}\n</style>'),
  ('Inline script', '<script>\n  ${_m}\n</script>'),
  ('div', '<div class="${_m}">\n  \n</div>'),
  ('Button', '<button id="${_m}">Button</button>'),
  ('Input', '<input type="text" placeholder="${_m}">'),
  ('Image', '<img src="${_m}" alt="">'),
  ('Link', '<a href="${_m}">Link</a>'),
  ('List', '<ul>\n  <li>${_m}</li>\n</ul>'),
];
const _snipCss = <(String, String)>[
  ('Rule', '.${_m} {\n  \n}'),
  ('Flex center', 'display: flex;\nalign-items: center;\njustify-content: center;'),
  ('Grid', 'display: grid;\ngrid-template-columns: repeat(${_m}2, 1fr);\ngap: 12px;'),
  ('Media query', '@media (max-width: 600px) {\n  ${_m}\n}'),
  ('Dark mode', '@media (prefers-color-scheme: dark) {\n  ${_m}\n}'),
  ('Variables', ':root {\n  --accent: #4fd5a6;\n  ${_m}\n}'),
  ('Animation', '@keyframes ${_m}fade {\n  from { opacity: 0; }\n  to { opacity: 1; }\n}'),
];
const _snipJs = <(String, String)>[
  ('Function', 'function ${_m}name() {\n  \n}'),
  ('Arrow function', 'const ${_m}name = () => {\n  \n};'),
  ('Click listener', "document.querySelector('${_m}').addEventListener('click', () => {\n  \n});"),
  ('On page load', "document.addEventListener('DOMContentLoaded', () => {\n  ${_m}\n});"),
  ('Select element', "const el = document.querySelector('${_m}');"),
  ('fetch JSON', "fetch('${_m}')\n  .then((r) => r.json())\n  .then((data) => {\n    console.log(data);\n  })\n  .catch((e) => console.error(e));"),
  ('for of', 'for (const x of ${_m}) {\n  \n}'),
  ('if / else', 'if (${_m}) {\n  \n} else {\n  \n}'),
  ('try / catch', 'try {\n  ${_m}\n} catch (e) {\n  console.error(e);\n}'),
  ('console.log', 'console.log(${_m});'),
];
List<(String, String)> _snippetsFor(String ext) => switch (ext) {
      'html' || 'htm' => _snipHtml,
      'css' => _snipCss,
      'js' || 'mjs' => _snipJs,
      'dart' => _snipDart,
      'kt' || 'kts' => _snipCode,
      'java' => _snipJava,
      'xml' => _snipXml,
      _ => const [],
    };


/// Brackets close themselves, typing a closing one skips over it, Backspace on an empty pair removes
/// both, Enter keeps the indent (and opens a block), and in XML typing ">" adds the closing tag.
const _voidTags = {'meta', 'link', 'br', 'hr', 'img', 'input', 'source', 'area', 'base', 'col', 'embed', 'param', 'track', 'wbr', '!doctype'};

class _EditFormatter extends TextInputFormatter {
  final bool xml, single;
  _EditFormatter(this.xml, {this.single = false});
  static const _base = {'(': ')', '[': ']', '{': '}', '"': '"'};
  static const _withSingle = {'(': ')', '[': ']', '{': '}', '"': '"', "'": "'"};
  Map<String, String> get _pairs => single ? _withSingle : _base;

  @override
  TextEditingValue formatEditUpdate(TextEditingValue old, TextEditingValue nw) {
    final o = old.text, n = nw.text;
    if (n.length == o.length - 1 && old.selection.isCollapsed && nw.selection.isCollapsed) {
      final p = old.selection.baseOffset;
      if (p > 0 && p < o.length && nw.selection.baseOffset == p - 1 && _pairs[o[p - 1]] == o[p] && n == o.replaceRange(p - 1, p, '')) {
        return TextEditingValue(text: o.replaceRange(p - 1, p + 1, ''), selection: TextSelection.collapsed(offset: p - 1));
      }
      return nw;
    }
    if (n.length != o.length + 1 || !nw.selection.isCollapsed) return nw;
    final i = nw.selection.baseOffset - 1;
    if (i < 0 || i >= n.length) return nw;
    final c = n[i];
    final next = i + 1 < n.length ? n[i + 1] : '';
    if (c == '\n') {
      final ls = i == 0 ? 0 : n.lastIndexOf('\n', i - 1) + 1;
      final line = n.substring(ls, i);
      var indent = RegExp(r'^[ \t]*').stringMatch(line) ?? '';
      final tr = line.trimRight();
      final last = tr.isEmpty ? '' : tr[tr.length - 1];
      final closer = last == '{' ? '}' : (last == '(' ? ')' : (last == '[' ? ']' : ''));
      if (closer.isNotEmpty && next == closer) {
        final mid = '$indent    ';
        return TextEditingValue(text: '${n.substring(0, i + 1)}$mid\n$indent${n.substring(i + 1)}', selection: TextSelection.collapsed(offset: i + 1 + mid.length));
      }
      if (closer.isNotEmpty) indent += '    ';
      if (indent.isEmpty) return nw;
      return TextEditingValue(text: n.substring(0, i + 1) + indent + n.substring(i + 1), selection: TextSelection.collapsed(offset: i + 1 + indent.length));
    }
    if (xml && c == '>') {
      final from = i > 400 ? i - 400 : 0;
      final before = n.substring(from, i);
      final m = RegExp(r'<([A-Za-z][\w:.\-]*)(?:\s[^<>]*)?$').firstMatch(before);
      if (m != null && !before.endsWith('/') && !_voidTags.contains(m.group(1)!.toLowerCase()) && !n.startsWith('</${m.group(1)}', i + 1)) {
        return TextEditingValue(text: '${n.substring(0, i + 1)}</${m.group(1)}>${n.substring(i + 1)}', selection: TextSelection.collapsed(offset: i + 1));
      }
      return nw;
    }
    if ((single ? ')]}"\'' : ')]}"').contains(c) && next == c) {
      return TextEditingValue(text: o, selection: TextSelection.collapsed(offset: i + 1));
    }
    final close = _pairs[c];
    if (close != null && (next.isEmpty || next == '\n' || next == ' ' || next == ')' || next == ']' || next == '}')) {
      if ((c == '"' || c == "'") && i > 0 && RegExp('[A-Za-z0-9"\']').hasMatch(n[i - 1])) return nw;
      return TextEditingValue(text: '${n.substring(0, i + 1)}$close${n.substring(i + 1)}', selection: TextSelection.collapsed(offset: i + 1));
    }
    return nw;
  }
}

/// One open file (a tab).
/// Runs in a background isolate for big files. Returns ["ok", text] or ["err", message].
List<String> _formatJob(List<String> a) {
  try {
    final opts = FormatOptions(tabWidth: int.tryParse(a[2]), singleQuote: a[3] == '1', printWidth: int.tryParse(a[4]) ?? 80);
    return ['ok', formatSource(a[0], a[1], opts: opts)];
  } on FormatException catch (e) {
    return ['err', e.message];
  } catch (_) {
    return ['err', 'Something went wrong. Your code was not changed.'];
  }
}

class _Doc {
  final String path;
  final _CodeController ctl;
  final undo = UndoHistoryController();
  final v = ScrollController();
  final h = ScrollController(); // sideways scroll (the suggestion popup follows it)
  bool dirty = false;
  Timer? t;
  // Cheap facts about the text, refreshed only when the text really changed.
  String? _mText;
  int lineCount = 1, longest = 0;
  _Doc(this.path, String text) : ctl = _CodeController(text, _ext(path)) {
    // Scrolling moves the coloured band when the screen reaches its edge.
    v.addListener(syncView);
  }

  // The line index lives in the controller; these just forward to it.
  List<int> get lineStarts => ctl.lineStarts;
  int lineAt(int off) => ctl.lineAt(off);

  /// Tells the controller which lines the screen shows, so only those get coloured.
  void syncView({bool force = false}) {
    if (!v.hasClients) return;
    final lh = currentLineHeight(CodeProject.i.fontSize);
    final first = ((v.offset - 8) / lh).floor();
    final last = ((v.offset + v.position.viewportDimension - 8) / lh).ceil();
    ctl.setView(first < 0 ? 0 : first, last < 0 ? 0 : last, force: force);
  }

  void measure() {
    final t = ctl.text;
    if (identical(t, _mText)) return;
    _mText = t;
    var n = 1, cur = 0, best = 0;
    for (var i = 0; i < t.length; i++) {
      if (t.codeUnitAt(i) == 10) {
        n++;
        if (cur > best) best = cur;
        cur = 0;
      } else {
        cur++;
      }
    }
    if (cur > best) best = cur;
    lineCount = n;
    longest = best;
  }

  void dispose() {
    t?.cancel();
    ctl.dispose();
    undo.dispose();
    v.dispose();
    h.dispose();
  }
}

class CodeEditorPage extends StatefulWidget {
  /// True when it is a tab of the home shell (the shell handles the keyboard and the bottom bar).
  final bool embedded;
  const CodeEditorPage({super.key, this.embedded = false});
  @override
  State<CodeEditorPage> createState() => _CodeEditorPageState();
}

class _CodeEditorPageState extends State<CodeEditorPage> {
  final p = CodeProject.i;
  final docs = <_Doc>[];
  final open = <String>{};
  final scaffold = GlobalKey<ScaffoldState>();
  final focus = FocusNode();
  final findC = TextEditingController(), replC = TextEditingController();
  int cur = -1, hit = -1;
  // Prettier settings (kept on the phone). A .prettierrc in the project wins over these.
  int _fmtTab = 0; // 0 = the usual for the file type
  bool _fmtSingle = false;
  int _fmtWidth = 80;
  bool _fmtOnSave = false;
  bool busy = false, findOn = false, caseSens = false;
  bool ready = false; // the heavy text view is built only after the page has finished sliding in
  Timer? _readyT;
  List<int> hits = [];

  _Doc? get doc => cur >= 0 && cur < docs.length ? docs[cur] : null;
  double get _lh => currentLineHeight(p.fontSize);

  @override
  void initState() {
    super.initState();
    p.jumpTick.addListener(_onJump);
    _loadFmtPrefs();
    _restoreTabs();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!mounted) return;
      final a = ModalRoute.of(context)?.animation;
      if (a == null || a.isCompleted) {
        _markReady();
        return;
      }
      void l(AnimationStatus s) {
        if (s == AnimationStatus.completed) {
          a.removeStatusListener(l);
          _markReady();
        }
      }

      a.addStatusListener(l);
      _readyT = Timer(const Duration(milliseconds: 800), () {
        a.removeStatusListener(l);
        _markReady();
      });
    });
  }

  /// Reopens the tabs (and cursor spots) that were open the last time the editor was left.
  void _restoreTabs() {
    if (!p.isOpen) return;
    for (final path in p.openPaths) {
      final b = p.files[path];
      if (b == null || !_isText(path, b) || b.length > _maxEditBytes) continue;
      docs.add(_Doc(path, utf8.decode(b, allowMalformed: true)));
    }
    if (docs.isEmpty) return;
    final i = docs.indexWhere((d) => d.path == p.curPath);
    cur = i < 0 ? 0 : i;
    for (final d in docs) {
      d.ctl.selection = TextSelection.collapsed(offset: (p.cursor[d.path] ?? 0).clamp(0, d.ctl.text.length).toInt());
      _expandTo(d.path);
    }
  }

  void _markReady() {
    if (ready || !mounted) return;
    _readyT?.cancel();
    setState(() => ready = true);
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!mounted) return;
      final d = doc;
      final sc = d == null ? null : p.scroll[d.path];
      if (d != null && sc != null && d.v.hasClients) d.v.jumpTo(sc.clamp(0, d.v.position.maxScrollExtent).toDouble());
      if (p.pending != null) {
        _onJump();
      } else if (p.isOpen && docs.isEmpty) {
        scaffold.currentState?.openDrawer();
      }
    });
  }

  @override
  void dispose() {
    p.jumpTick.removeListener(_onJump);
    _readyT?.cancel();
    if (p.isOpen) {
      p.openPaths
        ..clear()
        ..addAll(docs.map((d) => d.path));
      p.curPath = doc?.path;
      for (final d in docs) {
        p.cursor[d.path] = d.ctl.selection.isValid ? d.ctl.selection.start : 0;
        p.scroll[d.path] = d.v.hasClients ? d.v.offset : 0.0;
      }
    }
    for (final d in docs) {
      _flush(d);
      d.dispose();
    }
    focus.dispose();
    findC.dispose();
    replC.dispose();
    super.dispose();
  }

  void _onJump() {
    final j = p.pending;
    if (j == null || !mounted || !ready) return;
    p.pending = null;
    if (p.files.containsKey(j.$1)) _openDoc(j.$1, line: j.$2);
    final h = p.hint;
    p.hint = null;
    if (h != null && h.isNotEmpty) IosBanner.show(context, kind: BannerKind.warn, title: 'Fix this line', msg: h, duration: const Duration(seconds: 8));
  }

  // ------------------------------------------------------------ project

  void _expandTo(String path) {
    final s = path.split('/');
    var pre = '';
    for (var i = 0; i < s.length - 1; i++) {
      pre = pre.isEmpty ? s[i] : '$pre/${s[i]}';
      open.add(pre);
    }
  }

  /// Puts [files] in as the open project, replacing whatever was open before.
  void _loadProject(Map<String, List<int>> files, String name) {
    for (final d in docs) {
      d.dispose();
    }
    docs.clear();
    cur = -1;
    p.resetView();
    p.files
      ..clear()
      ..addAll(files);
    p.modified
      ..clear()
      ..addAll(files.keys);
    p.name = name;
    open.clear();
    for (final k in files.keys) {
      final s = k.split('/');
      var pre = '';
      for (var i = 0; i < s.length - 1 && i < 2; i++) {
        pre = pre.isEmpty ? s[i] : '$pre/${s[i]}';
        open.add(pre);
      }
    }
  }

  /// A blank project: a name and a manifest, with no Activity and no code to start from.
  Future<void> _createProject() async {
    if (p.isOpen) {
      final ok = await _confirm('Create a new project?', 'The project that is open now will be closed. Changes you haven’t saved as a zip will be lost.', 'Create new', C.red);
      if (ok != true || !mounted) return;
    }
    final kind = await showModalBottomSheet<String>(
      context: context,
      backgroundColor: C.sheet,
      showDragHandle: true,
      builder: (ctx) => SafeArea(
        child: Column(mainAxisSize: MainAxisSize.min, children: [
          ListTile(
            leading: const Icon(Icons.android_rounded, color: C.teal),
            title: Text('Android app', style: inter(16, w: FontWeight.w600)),
            subtitle: Text('Kotlin or Java with a manifest', style: inter(13, c: C.muted)),
            onTap: () => Navigator.pop(ctx, 'android'),
          ),
          ListTile(
            leading: const Icon(Icons.html_rounded, color: C.teal),
            title: Text('Website (HTML, CSS, JS)', style: inter(16, w: FontWeight.w600)),
            subtitle: Text('index.html, style.css and script.js', style: inter(13, c: C.muted)),
            onTap: () => Navigator.pop(ctx, 'web'),
          ),
        ]),
      ),
    );
    if (kind == null || !mounted) return;
    final r = await _ask('Project name', '', 'Create');
    if (r == null || r.isEmpty || !mounted) return;
    final name = r.replaceAll(RegExp(r'[\\/:*?"<>|]'), '_');
    if (kind == 'web') {
      final title = name.replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('"', '&quot;');
      _loadProject({
        'index.html': utf8.encode('<!DOCTYPE html>\n<html lang="en">\n<head>\n  <meta charset="utf-8">\n  <meta name="viewport" content="width=device-width, initial-scale=1">\n  <title>$title</title>\n  <link rel="stylesheet" href="style.css">\n</head>\n<body>\n  <h1>$title</h1>\n  <script src="script.js"></script>\n</body>\n</html>\n'),
        'style.css': utf8.encode(_template('style.css')),
        'script.js': utf8.encode(_template('script.js')),
      }, name);
      setState(() {});
      _openDoc('index.html');
      return;
    }
    // Package name: lower-case letters and digits only, and it can't start with a digit.
    var seg = name.toLowerCase().replaceAll(RegExp(r'[^a-z0-9]'), '');
    if (seg.isEmpty || RegExp(r'^[0-9]').hasMatch(seg)) seg = 'app$seg';
    final label = name.replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('"', '&quot;');
    final manifest = '<?xml version="1.0" encoding="utf-8"?>\n'
        '<manifest xmlns:android="http://schemas.android.com/apk/res/android"\n'
        '    package="com.example.$seg">\n\n'
        '    <application android:label="@string/app_name" />\n\n'
        '</manifest>\n';
    final strings = '<?xml version="1.0" encoding="utf-8"?>\n<resources>\n    <string name="app_name">$label</string>\n</resources>\n';
    _loadProject({
      'AndroidManifest.xml': utf8.encode(manifest),
      'res/values/strings.xml': utf8.encode(strings),
    }, name);
    setState(() {});
    _openDoc('AndroidManifest.xml');
  }

  Future<void> _openZip() async {
    // Opening a zip replaces the project in memory, so ask first: nothing here is written back to the zip.
    if (p.isOpen) {
      final ok = await _confirm('Open another zip?', 'The project that is open now will be closed. Changes you haven’t saved as a zip with “Save zip to Downloads” will be lost.', 'Open another', C.red);
      if (ok != true || !mounted) return;
    }
    final f = await openFile(acceptedTypeGroups: [const XTypeGroup(label: 'Project (zip) or web page', extensions: ['zip', 'html', 'htm'])]);
    if (f == null || !mounted) return;
    setState(() => busy = true);
    try {
      final isPage = RegExp(r'\.html?$', caseSensitive: false).hasMatch(f.name);
      final files = isPage ? <String, List<int>>{'index.html': await f.readAsBytes()} : await compute(_unpack, await f.readAsBytes());
      if (files.isEmpty) throw const FormatException('That zip has no files in it.');
      for (final d in docs) {
        d.dispose();
      }
      docs.clear();
      cur = -1;
      p.resetView();
      p.files
        ..clear()
        ..addAll(files);
      p.modified.clear();
      p.name = f.name.replaceAll(RegExp(r'\.(zip|html?)$', caseSensitive: false), '');
      open.clear();
      for (final k in files.keys) {
        final s = k.split('/');
        var pre = '';
        for (var i = 0; i < s.length - 1 && i < 2; i++) {
          pre = pre.isEmpty ? s[i] : '$pre/${s[i]}';
          open.add(pre);
        }
      }
      final src = (files.keys.toList()..sort(_cmp)).where((k) => k.endsWith('.kt') || k.endsWith('.java') || k.endsWith('.dart') || k.endsWith('.html'));
      if (src.isNotEmpty) _expandTo(src.first);
      if (mounted) {
        setState(() => busy = false);
        scaffold.currentState?.openDrawer();
      }
    } catch (e) {
      if (mounted) {
        setState(() => busy = false);
        IosBanner.show(context, kind: BannerKind.err, title: 'Couldn\u2019t open that file', msg: e is FormatException ? e.message : 'That file isn\u2019t a zip. Try a different one.');
      }
    }
  }

  Future<void> _closeProject() async {
    final ok = await _confirm('Close this project?', 'Your edits are only kept while the project is open. Save it as a zip first if you want to keep them.', 'Close', C.red);
    if (ok != true || !mounted) return;
    for (final d in docs) {
      d.dispose();
    }
    setState(() {
      docs.clear();
      cur = -1;
      p.resetView();
      p.files.clear();
      p.modified.clear();
      findOn = false;
    });
  }

  List<_Row> _rows() {
    final out = <_Row>[], seen = <String>{};
    for (final path in p.files.keys.toList()..sort(_cmp)) {
      final s = path.split('/');
      var visible = true, pre = '';
      for (var i = 0; i < s.length - 1; i++) {
        pre = pre.isEmpty ? s[i] : '$pre/${s[i]}';
        if (seen.add(pre)) out.add(_Row(pre, s[i], i, true));
        if (!open.contains(pre)) {
          visible = false;
          break;
        }
      }
      if (visible) out.add(_Row(path, s.last, s.length - 1, false));
    }
    return out;
  }

  Future<bool?> _confirm(String title, String msg, String action, Color c) => showDialog<bool>(
        context: context,
        builder: (ctx) => AlertDialog(
          backgroundColor: C.sheet,
          title: Text(title, style: inter(17, w: FontWeight.w700)),
          content: Text(msg, style: inter(14, c: C.muted)),
          actions: [
            TextButton(onPressed: () => Navigator.pop(ctx, false), child: Text('Cancel', style: inter(15, c: C.muted))),
            TextButton(onPressed: () => Navigator.pop(ctx, true), child: Text(action, style: inter(15, w: FontWeight.w700, c: c))),
          ],
        ),
      );

  Future<String?> _ask(String title, String initial, String action, {TextInputType? kb}) {
    final c = TextEditingController(text: initial);
    c.selection = TextSelection.collapsed(offset: initial.length);
    return showDialog<String>(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: C.sheet,
        title: Text(title, style: inter(17, w: FontWeight.w700)),
        content: TextField(
          controller: c,
          autofocus: true,

          keyboardType: kb,
          style: inter(15),
          cursorColor: C.teal,
          decoration: const InputDecoration(isDense: true, enabledBorder: UnderlineInputBorder(borderSide: BorderSide(color: C.line)), focusedBorder: UnderlineInputBorder(borderSide: BorderSide(color: C.teal))),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx), child: Text('Cancel', style: inter(15, c: C.muted))),
          TextButton(onPressed: () => Navigator.pop(ctx, c.text.trim()), child: Text(action, style: inter(15, w: FontWeight.w700, c: C.teal))),
        ],
      ),
    );
  }

  void _err(String t, String m) => IosBanner.show(context, kind: BannerKind.err, title: t, msg: m);

  String _clean(String s) => s.replaceAll('\\', '/').replaceAll(RegExp(r'^/+|/+$'), '');

  /// Why [path] can't be used as a new file name, or null when it can. Stops empty parts, "..", a name
  /// that is already a file, and a file named like a folder (that would break the file tree).
  String? _nameProblem(String path) {
    if (path.split('/').any((s) => s.isEmpty || s == '.' || s == '..')) return 'Use names like folder/file.kt, with no empty parts or "..".';
    if (p.files.containsKey(path)) return 'A file with that name is already in the project.';
    if (p.files.keys.any((k) => k.startsWith('$path/'))) return 'That name is already used by a folder.';
    return null;
  }

  Future<void> _newFile([String folder = '']) async {
    final r = await _ask('New file', folder.isEmpty ? '' : '$folder/', 'Create');
    if (r == null || r.isEmpty || r.endsWith('/') || !mounted) return;
    final path = _clean(r);
    final problem = _nameProblem(path);
    if (problem != null) return _err('Can’t create that file', problem);
    p.files[path] = utf8.encode(_template(path));
    p.modified.add(path);
    _expandTo(path);
    setState(() {});
    _openDoc(path);
  }

  Future<void> _addFromPhone([String folder = '']) async {
    final f = await openFile();
    if (f == null || !mounted) return;
    final bytes = await f.readAsBytes();
    if (!mounted) return;
    final r = await _ask('Add to project as', folder.isEmpty ? f.name : '$folder/${f.name}', 'Add');
    if (r == null || r.isEmpty || !mounted) return;
    final path = _clean(r);
    final problem = _nameProblem(path);
    if (problem != null) return _err('Can’t add that file', problem);
    p.files[path] = bytes;
    p.modified.add(path);
    _expandTo(path);
    setState(() {});
  }

  Future<void> _rename(_Row r, {bool copy = false}) async {
    _flushAll();
    final to = await _ask(copy ? 'Copy to' : 'Rename or move', r.path, copy ? 'Copy' : 'Save');
    if (to == null || to.isEmpty || to == r.path || !mounted) return;
    final n = _clean(to);
    if (n.isEmpty || n.split('/').any((s) => s.isEmpty || s == '.' || s == '..')) return _err('Can’t use that name', 'Use names like folder/file.kt, with no empty parts or "..".');
    final moves = <String, String>{};
    for (final k in p.files.keys) {
      if (r.folder ? k.startsWith('${r.path}/') : k == r.path) {
        moves[k] = r.folder ? '$n/${k.substring(r.path.length + 1)}' : n;
      }
    }
    if (moves.values.any((v) => p.files.containsKey(v) && !moves.containsKey(v))) return _err('Already exists', n);
    if (!r.folder && !copy && p.files.keys.any((k) => k.startsWith('$n/'))) return _err('Already a folder', n);
    // A renamed file that was open stays open under its new name, instead of its tab just vanishing.
    final wasOpen = !copy && !r.folder && docs.any((d) => d.path == r.path);
    if (!copy) _closeUnder(r.path, r.folder);
    final data = {for (final e in moves.entries) e.value: copy ? List<int>.from(p.files[e.key]!) : p.files.remove(e.key)!};
    p.files.addAll(data);
    if (!copy) {
      for (final old in moves.keys) {
        p.modified.remove(old);
      }
    }
    p.modified.addAll(data.keys);
    if (!copy && r.folder && open.remove(r.path)) open.add(n);
    _expandTo('$n/x');
    setState(() {});
    if (wasOpen) _openDoc(n);
  }

  Future<void> _delete(_Row r) async {
    final ok = await _confirm('Delete ${r.label}?', r.folder ? 'The folder and everything in it will be removed from this project.' : 'This file will be removed from this project.', 'Delete', C.red);
    if (ok != true) return;
    _closeUnder(r.path, r.folder);
    p.files.removeWhere((k, _) => r.folder ? k.startsWith('${r.path}/') : k == r.path);
    setState(() {});
  }

  void _closeUnder(String path, bool folder) {
    for (var i = docs.length - 1; i >= 0; i--) {
      if (folder ? docs[i].path.startsWith('$path/') : docs[i].path == path) {
        _flush(docs[i]); // never drop typed text that hasn't reached the project yet
        docs[i].dispose();
        docs.removeAt(i);
      }
    }
    cur = docs.isEmpty ? -1 : cur.clamp(0, docs.length - 1).toInt();
  }

  void _treeMenu(_Row r) {
    Widget item(IconData i, String t, VoidCallback f, {Color c = C.text}) => ListTile(
          dense: true,
          visualDensity: VisualDensity.compact,
          leading: Icon(i, color: c, size: 22),
          title: Text(t, style: inter(15, c: c)),
          onTap: () {
            Navigator.pop(context);
            f();
          },
        );
    showModalBottomSheet(
      context: context,
      backgroundColor: C.sheet,
      showDragHandle: true,
      builder: (_) => SafeArea(
        child: Column(mainAxisSize: MainAxisSize.min, children: [
          if (r.folder) item(Icons.note_add_outlined, 'New file here', () => _newFile(r.path)),
          if (r.folder) item(Icons.upload_file_rounded, 'Add a file from your phone', () => _addFromPhone(r.path)),
          item(Icons.drive_file_rename_outline_rounded, 'Rename or move', () => _rename(r)),
          item(Icons.copy_rounded, r.folder ? 'Copy folder' : 'Duplicate', () => _rename(r, copy: true)),
          item(Icons.delete_outline_rounded, 'Delete', () => _delete(r), c: C.red),
        ]),
      ),
    );
  }

  // ------------------------------------------------------------ build / save

  void _flush(_Doc d) {
    d.t?.cancel();
    if (!d.dirty) return;
    p.files[d.path] = utf8.encode(d.ctl.text);
    p.modified.add(d.path);
    d.dirty = false;
  }

  void _flushAll() {
    for (final d in docs) {
      if (d.dirty) _formatOnSave(d);
      _flush(d);
    }
  }

  bool get _hasWebPage => p.files.keys.any((k) => const {'html', 'htm'}.contains(_ext(k)));

  /// Opens the page in a live preview: the HTML file that is open, else index.html.
  void _preview() {
    _flushAll();
    final cur = doc?.path;
    String? entry;
    if (cur != null && const {'html', 'htm'}.contains(_ext(cur))) {
      entry = cur;
    } else {
      entry = p.files.containsKey('index.html')
          ? 'index.html'
          : p.files.keys.cast<String?>().firstWhere((k) => k!.endsWith('/index.html'), orElse: () => p.files.keys.cast<String?>().firstWhere((k) => const {'html', 'htm'}.contains(_ext(k!)), orElse: () => null));
    }
    if (entry == null) return _err('Nothing to preview', 'Add an index.html first.');
    Navigator.of(context).push(CupertinoPageRoute(builder: (_) => WebPreviewPage(files: Map<String, List<int>>.of(p.files), entry: entry!)));
  }

  bool get _isFlutter => pubsExist(p.files);
  bool get _isWeb =>
      !_isFlutter &&
      !p.files.keys.any((k) => k.endsWith('AndroidManifest.xml') || k.endsWith('.kt') || k.endsWith('.java')) &&
      p.files.keys.any((k) => const {'html', 'htm'}.contains(_ext(k)));

  Future<void> _build() async {
    _flushAll();
    final flutter = _isFlutter, web = _isWeb;
    final choice = await showBuildTargetSheet(
      context,
      projectName: p.name,
      androidOk: !flutter,
      flutterOk: flutter,
      androidNote: flutter ? 'This is a Flutter project, so build it as Flutter.' : (web ? 'Your website gets wrapped into an Android app.' : ''),
      flutterNote: flutter ? '' : 'Needs a Flutter project (a pubspec.yaml). This one has none.',
      initial: flutter ? 'flutter' : 'android',
    );
    if (choice == null || !mounted) return;
    final target = choice.target;
    if (!await _preflight()) return;
    // The server compile check is for Kotlin / Java only.
    if (target == 'android' && !web && !await _serverCheck()) return;
    await _buildNow(target, choice.app, web: web);
  }

  /// Compiles the project on the build server without using a build. Returns true when it is fine to
  /// go on: it compiled, the server couldn't check, or the user chose to build anyway.
  Future<bool> _serverCheck() async {
    var cancelled = false;
    final msg = ValueNotifier<String>('Compiling on the server\u2026');
    setState(() => busy = true);
    showDialog<void>(
      context: context,
      barrierDismissible: false,
      builder: (ctx) => PopScope(
        canPop: false,
        child: AlertDialog(
          backgroundColor: C.sheet,
          title: Text('Checking your code', style: inter(17, w: FontWeight.w700)),
          content: ValueListenableBuilder<String>(
            valueListenable: msg,
            builder: (_, v, __) => Row(children: [
              const SizedBox(width: 22, height: 22, child: CircularProgressIndicator(strokeWidth: 2.5)),
              const SizedBox(width: 14),
              Expanded(child: Text('$v\nThis doesn\u2019t use a build.', style: inter(13.5, c: C.muted, h: 1.4))),
            ]),
          ),
          actions: [
            TextButton(
              onPressed: () {
                cancelled = true;
                Navigator.of(ctx).pop();
              },
              child: Text('Cancel', style: inter(15, w: FontWeight.w600, c: C.muted)),
            ),
          ],
        ),
      ),
    );
    CompileCheck? res;
    try {
      final bytes = await compute(_pack, Map<String, List<int>>.of(p.files));
      res = await Api.i.compileCheck(bytes, '${p.name}.zip', cancelled: () => cancelled || !mounted, onStatus: (m) => msg.value = m);
    } catch (_) {}
    if (!mounted) return false;
    if (!cancelled) Navigator.of(context, rootNavigator: true).pop(); // close the dialog
    setState(() => busy = false);
    if (cancelled) return false;
    if (res == null) {
      IosBanner.show(context, kind: BannerKind.warn, title: 'Couldn\u2019t check on the server', msg: 'Building without the compile check.');
      return true;
    }
    if (res.ok) {
      IosBanner.show(context, kind: BannerKind.ok, title: 'Compiles fine', msg: 'No errors found, starting the build.');
      return true;
    }
    final issues = <_Issue>[for (final e in res.issues) (p.find(e.file) ?? e.file, e.line, e.message, true)];
    final r = await _issuesSheet(issues, true, server: true);
    if (!mounted || r == null) return false;
    if (r is (String, int)) {
      _openDoc(r.$1, line: r.$2);
      return false;
    }
    return r == 'build';
  }

  /// Looks for mistakes the build would stop on, before the project is packed and uploaded.
  /// Returns true when it is fine to go on (nothing found, or the user chose to build anyway).
  Future<bool> _preflight({bool building = true}) async {
    _flushAll();
    setState(() => busy = true);
    var issues = const <_Issue>[];
    try {
      issues = await compute(_checkProject, Map<String, List<int>>.of(p.files));
    } catch (_) {
      // the check failing must never get in the way of a build
    }
    if (!mounted) return false;
    setState(() => busy = false);
    if (issues.isEmpty) {
      if (!building) IosBanner.show(context, kind: BannerKind.ok, title: 'No problems found', msg: 'Checked ${p.files.length} files.');
      return true;
    }
    final r = await _issuesSheet(issues, building);
    if (!mounted || r == null) return false;
    if (r is (String, int)) {
      _openDoc(r.$1, line: r.$2);
      return false;
    }
    return r == 'build';
  }

  Future<Object?> _issuesSheet(List<_Issue> issues, bool building, {bool server = false}) {
    final errs = issues.where((i) => i.$4).length, warns = issues.length - errs;
    final title = [if (errs > 0) '$errs error${errs == 1 ? '' : 's'}', if (warns > 0) '$warns warning${warns == 1 ? '' : 's'}'].join(' and ');
    final shown = issues.take(60).toList();
    final fixFirst = errs > 0;
    final secLabel = !building ? null : (fixFirst ? 'Build anyway' : 'Not now');
    final secValue = fixFirst ? 'build' : null;
    final priLabel = (!building || fixFirst) ? 'Fix first' : 'Build';
    final priValue = (!building || fixFirst) ? null : 'build';
    return showModalBottomSheet<Object>(
      context: context,
      backgroundColor: C.sheet,
      showDragHandle: true,
      isScrollControlled: true,
      builder: (ctx) => SafeArea(
        child: ConstrainedBox(
          constraints: BoxConstraints(maxHeight: MediaQuery.of(ctx).size.height * .78),
          child: Column(mainAxisSize: MainAxisSize.min, crossAxisAlignment: CrossAxisAlignment.start, children: [
            Padding(
              padding: const EdgeInsets.fromLTRB(20, 0, 20, 8),
              child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                Text('$title found', style: inter(18, w: FontWeight.w700)),
                const SizedBox(height: 4),
                Text(fixFirst ? (server ? 'The server compiled your code and these stopped it. Fix them first so your build isn\u2019t wasted.' : 'Fix these first so the build isn\u2019t wasted. This catches syntax mistakes and missing files or resources, not every compiler error.') : 'These probably won\u2019t stop the build.', style: inter(13, c: C.muted, h: 1.4)),
              ]),
            ),
            Flexible(
              child: ListView.builder(
                shrinkWrap: true,
                itemCount: shown.length + (issues.length > shown.length ? 1 : 0),
                itemBuilder: (_, i) {
                  if (i >= shown.length) {
                    return Padding(padding: const EdgeInsets.fromLTRB(20, 8, 20, 8), child: Text('and ${issues.length - shown.length} more', style: inter(12, c: C.muted)));
                  }
                  final x = shown[i];
                  return InkWell(
                    onTap: () => Navigator.pop(ctx, (x.$1, x.$2)),
                    child: Padding(
                      padding: const EdgeInsets.fromLTRB(20, 8, 20, 8),
                      child: Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
                        Padding(padding: const EdgeInsets.only(top: 1), child: Icon(x.$4 ? Icons.error_rounded : Icons.warning_amber_rounded, size: 18, color: x.$4 ? C.red : _cNum)),
                        const SizedBox(width: 10),
                        Expanded(
                          child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                            Text(x.$3, style: inter(14, h: 1.3)),
                            const SizedBox(height: 2),
                            Text('${x.$1}:${x.$2}', maxLines: 1, overflow: TextOverflow.ellipsis, style: const TextStyle(fontFamily: 'monospace', fontSize: 11.5, color: C.muted)),
                          ]),
                        ),
                      ]),
                    ),
                  );
                },
              ),
            ),
            Padding(
              padding: const EdgeInsets.fromLTRB(16, 8, 16, 12),
              child: Row(children: [
                if (secLabel != null)
                  Expanded(
                    child: OutlinedButton(
                      onPressed: () => Navigator.pop(ctx, secValue),
                      style: OutlinedButton.styleFrom(minimumSize: const Size(0, 46), side: const BorderSide(color: C.line), shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(23))),
                      child: Text(secLabel, style: inter(15, w: FontWeight.w600, c: fixFirst ? C.red : C.muted)),
                    ),
                  ),
                if (secLabel != null) const SizedBox(width: 10),
                Expanded(
                  child: FilledButton(
                    onPressed: () => Navigator.pop(ctx, priValue),
                    style: FilledButton.styleFrom(backgroundColor: C.tealBtn, foregroundColor: C.onTeal, minimumSize: const Size(0, 46), shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(23))),
                    child: Text(priLabel, style: inter(15, w: FontWeight.w700, c: C.onTeal)),
                  ),
                ),
              ]),
            ),
          ]),
        ),
      ),
    );
  }

  /// Starts the build straight away and opens the build progress. The app settings come from the code
  /// editor's own app ([app]), never from the Build APK options page.
  /// Signed release APK, no Dex2C / string protection, and the user's own signing key if they saved one.
  Future<void> _buildNow(String target, CodeApp app, {bool web = false}) async {
    _flushAll();
    // A website on Android can also be built right on the phone (Quick build).
    var mode = 'full';
    if (target == 'android' && web) {
      final c = await showBuildModeSheet(context);
      if (c == null || !mounted) return;
      mode = c;
    }
    setState(() => busy = true);
    final nav = Navigator.of(context);
    Timer? wakeTimer;
    try {
      final bytes = await compute(_pack, Map<String, List<int>>.of(p.files));
      if (!mounted) return;
      final req = BuildRequest(platform: target)
        ..appName = app.name
        ..packageSuffix = app.suffix
        ..orientation = app.orientation
        ..logoPath = app.logoPath
        ..permissions = selectedPerms(app.perms)
        ..zipBytes = bytes
        ..zipName = '${p.name}.zip';
      if (mode == 'quick') {
        req.mode = 'quick';
        final next = await Navigator.of(context).push<String>(CupertinoPageRoute<String>(builder: (_) => QuickBuildPage(req: req, appName: app.name)));
        if (next != 'full' || !mounted) return;
        req.mode = 'full';
        IosBanner.show(context, kind: BannerKind.warn, title: 'Building on the server', msg: 'Quick build isn\u2019t available in this version, so this one is built on the server.', duration: const Duration(seconds: 5));
      }
      // Render's free tier sleeps: if the server is slow to answer, say so.
      wakeTimer = Timer(const Duration(seconds: 5), () {
        if (mounted) IosBanner.show(context, kind: BannerKind.warn, title: 'Please wait', msg: 'Waking up the build server. This can take up to a minute.', duration: const Duration(seconds: 6));
      });
      final myKey = SigningKeys.i.current.value;
      if (myKey != null && Api.i.customSigningOk) {
        req
          ..signingKeyPath = myKey.keyPath
          ..signingCertPath = myKey.certPath;
      }
      final lim = await Api.i.limit();
      if (lim != null) Store.i.limit.value = lim;
      if (lim != null && lim.remaining <= 0) {
        if (mounted) IosBanner.show(context, kind: BannerKind.warn, title: 'Daily limit reached', msg: 'You used all ${lim.limit} builds today. They reset at midnight UTC.', duration: const Duration(seconds: 7));
        return;
      }
      final id = await Api.i.startBuild(req);
      await Store.i.setActiveJob(id);
      BuildNotifier.i.watch(id, app.name);
      Store.i.refreshLimit();
      Store.i.refreshHistory();
      if (!mounted) return;
      nav.push(CupertinoPageRoute(builder: (_) => BuildPage(jobId: id, appName: app.name, logoPath: app.logoPath)));
    } on ApiError catch (e) {
      if (!mounted) return;
      if (e.status == 429) {
        IosBanner.show(context, kind: BannerKind.warn, title: 'Daily limit reached', msg: e.message, duration: const Duration(seconds: 7));
      } else if (e.status == 503 && e.code == 'server_busy') {
        IosBanner.show(context, kind: BannerKind.warn, title: 'Server busy', msg: 'The server is busy. Nothing was started and no build was used. Try again in about ${e.retryAfter} seconds.', duration: const Duration(seconds: 7));
      } else {
        IosBanner.show(context, kind: BannerKind.err, title: 'Couldn\u2019t start the build', msg: e.message, duration: const Duration(seconds: 6));
      }
    } catch (_) {
      if (mounted) IosBanner.show(context, kind: BannerKind.err, title: 'Couldn\u2019t reach the server', msg: 'Check your connection and try again.', duration: const Duration(seconds: 6));
    } finally {
      wakeTimer?.cancel();
      if (mounted) setState(() => busy = false);
    }
  }

  Future<void> _saveZip() async {
    _flushAll();
    setState(() => busy = true);
    try {
      final bytes = await compute(_pack, Map<String, List<int>>.of(p.files));
      final dir = await getTemporaryDirectory();
      final f = File('${dir.path}/${p.name}.zip');
      await f.writeAsBytes(bytes);
      final shown = await FileSaver.saveToDownloads(f.path, p.name, fileName: '${p.name}.zip');
      if (mounted) {
        if (shown != null) {
          IosBanner.show(context, kind: BannerKind.ok, title: 'Zip saved', msg: shown);
        } else {
          _err('Couldn\u2019t save the zip', FileSaver.lastError ?? 'Try again.');
        }
      }
    } catch (_) {
      if (mounted) _err('Couldn\u2019t save the zip', 'Try again.');
    }
    if (mounted) setState(() => busy = false);
  }

  Future<void> _projectSearch() async {
    _flushAll();
    final r = await Navigator.of(context).push<(String, int)>(CupertinoPageRoute(builder: (_) => const _SearchPage()));
    if (!mounted) return;
    for (final d in docs) {
      final raw = p.files[d.path];
      if (raw == null) continue;
      final t = utf8.decode(raw, allowMalformed: true);
      if (t != d.ctl.text) d.ctl.text = t;
    }
    if (r != null) _openDoc(r.$1, line: r.$2);
  }

  // ------------------------------------------------------------ tabs and editing

  void _openDoc(String path, {int line = 1}) {
    final b = p.files[path];
    if (b == null) return;
    if (!_isText(path, b)) return _err('Can\u2019t edit this file', 'It isn\u2019t a text file. It stays in the project as it is.');
    if (b.length > _maxEditBytes) return _err('File too big to edit here', 'It stays in the project as it is.');
    var i = docs.indexWhere((d) => d.path == path);
    final big = i < 0 && b.length > _bigFileBytes;
    if (i < 0) {
      docs.add(_Doc(path, utf8.decode(b, allowMalformed: true)));
      i = docs.length - 1;
    }
    _expandTo(path);
    final prev = doc;
    if (prev != null && prev.dirty && docs.indexOf(prev) != i) {
      _formatOnSave(prev);
      _flush(prev);
    }
    setState(() => cur = i);
    if (big) {
      IosBanner.show(context, kind: BannerKind.warn, title: 'Big file', msg: 'Colours show only around what is on screen, and typing can be a little slower.', duration: const Duration(seconds: 5));
    }
    if (scaffold.currentState?.isDrawerOpen ?? false) Navigator.of(context).pop();
    _hitsRefresh();
    if (line > 1) {
      WidgetsBinding.instance.addPostFrameCallback((_) => _goto(line));
    }
  }

  void _closeDoc(int i) {
    final d = docs[i];
    if (d.dirty) _formatOnSave(d);
    _flush(d);
    d.dispose();
    setState(() {
      docs.removeAt(i);
      cur = docs.isEmpty ? -1 : (cur > i ? cur - 1 : cur.clamp(0, docs.length - 1).toInt());
    });
    _hitsRefresh();
  }

  void _changed(_Doc d) {
    d.syncView(force: true);
    d.dirty = true;
    d.t?.cancel();
    d.t = Timer(const Duration(milliseconds: 500), () {
      _flush(d);
      if (mounted) setState(() {});
    });
    if (findOn) {
      _hitsRefresh();
    } else {
      setState(() {});
    }
  }

  void _goto(int line) {
    final d = doc;
    if (d == null) return;
    final t = d.ctl.text;
    var off = 0;
    for (var i = 1; i < line; i++) {
      final nl = t.indexOf('\n', off);
      if (nl < 0) break;
      off = nl + 1;
    }
    d.ctl.selection = TextSelection.collapsed(offset: off);
    _scrollToLine(d, line);
    focus.requestFocus();
  }

  void _scrollToLine(_Doc d, int line) {
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (d.v.hasClients) d.v.jumpTo((8 + (line - 3) * _lh).clamp(0, d.v.position.maxScrollExtent).toDouble());
    });
  }

  Future<void> _gotoDialog() async {
    final d = doc;
    if (d == null) return;
    final n = '\n'.allMatches(d.ctl.text).length + 1;
    final r = await _ask('Go to line (1-$n)', '', 'Go', kb: TextInputType.number);
    final v = int.tryParse(r ?? '');
    if (v != null && mounted) _goto(v.clamp(1, n).toInt());
  }

  void _setText(_Doc d, String text, TextSelection sel) {
    d.ctl.value = TextEditingValue(text: text, selection: sel);
    _changed(d);
    focus.requestFocus();
  }

  void _insert(String s, {int back = 0}) {
    final d = doc;
    if (d == null) return;
    final v = d.ctl.value;
    final a = v.selection.isValid ? v.selection.start : v.text.length;
    final b = v.selection.isValid ? v.selection.end : v.text.length;
    _setText(d, v.text.replaceRange(a, b, s), TextSelection.collapsed(offset: a + s.length - back));
  }

  /// Start and end of the whole lines the selection touches.
  (int, int) _lineSpan(_Doc d) {
    final t = d.ctl.text;
    final sel = d.ctl.selection;
    final a = sel.isValid ? sel.start.clamp(0, t.length).toInt() : 0;
    final b = sel.isValid ? sel.end.clamp(0, t.length).toInt() : 0;
    final s = a == 0 ? 0 : t.lastIndexOf('\n', a - 1) + 1;
    final bb = (b > a && t[b - 1] == '\n') ? b - 1 : b;
    var e = t.indexOf('\n', bb);
    if (e < 0) e = t.length;
    return (s, e);
  }

  void _indent(bool more) {
    final d = doc;
    if (d == null) return;
    final t = d.ctl.text;
    final sel = d.ctl.selection;
    if (more && sel.isValid && sel.isCollapsed) return _insert('    ');
    final (s, e) = _lineSpan(d);
    final lines = t.substring(s, e).split('\n').map((l) {
      if (more) return l.isEmpty ? l : '    $l';
      return l.startsWith('    ') ? l.substring(4) : (l.startsWith('\t') ? l.substring(1) : l.replaceFirst(RegExp(r'^ {1,3}'), ''));
    }).join('\n');
    _setText(d, t.replaceRange(s, e, lines), TextSelection(baseOffset: s, extentOffset: s + lines.length));
  }

  void _comment() {
    final d = doc;
    if (d == null) return;
    final t = d.ctl.text;
    final ext = d.ctl.ext;
    final xml = _xmlExts.contains(ext) || ext == 'md' || ext == 'markdown';
    final tag = _lineTag(ext);
    final (s, e) = _lineSpan(d);
    final lines = t.substring(s, e).split('\n');
    final body = lines.where((l) => l.trim().isNotEmpty).toList();
    final already = body.isNotEmpty && body.every((l) => xml ? (l.trim().startsWith('<!--') && l.trim().endsWith('-->')) : l.trim().startsWith(tag));
    final out = lines.map((l) {
      if (l.trim().isEmpty) return l;
      final ind = RegExp(r'^[ \t]*').stringMatch(l) ?? '';
      final rest = l.substring(ind.length);
      if (xml) return already ? '$ind${rest.substring(4, rest.length - 3).trim()}' : '$ind<!-- $rest -->';
      return already ? '$ind${rest.replaceFirst(RegExp('^${RegExp.escape(tag)} ?'), '')}' : '$ind$tag $rest';
    }).join('\n');
    _setText(d, t.replaceRange(s, e, out), TextSelection(baseOffset: s, extentOffset: s + out.length));
  }

  void _dupLine() {
    final d = doc;
    if (d == null) return;
    final t = d.ctl.text;
    final (s, e) = _lineSpan(d);
    final block = t.substring(s, e);
    _setText(d, t.replaceRange(e, e, '\n$block'), TextSelection.collapsed(offset: e + 1 + block.length));
  }

  void _delLine() {
    final d = doc;
    if (d == null) return;
    final t = d.ctl.text;
    final (s, e) = _lineSpan(d);
    final end = e < t.length ? e + 1 : e;
    final start = (end == t.length && s > 0 && e == t.length) ? s - 1 : s;
    _setText(d, t.replaceRange(start, end, ''), TextSelection.collapsed(offset: start.clamp(0, t.length - (end - start)).toInt()));
  }

  void _moveLine(bool up) {
    final d = doc;
    if (d == null) return;
    final t = d.ctl.text;
    final (s, e) = _lineSpan(d);
    if (up) {
      if (s == 0) return;
      final ps = s - 2 < 0 ? 0 : t.lastIndexOf('\n', s - 2) + 1;
      final prev = t.substring(ps, s - 1), block = t.substring(s, e);
      _setText(d, '${t.substring(0, ps)}$block\n$prev${t.substring(e)}', TextSelection(baseOffset: ps, extentOffset: ps + block.length));
    } else {
      if (e >= t.length) return;
      var ne = t.indexOf('\n', e + 1);
      if (ne < 0) ne = t.length;
      final next = t.substring(e + 1, ne), block = t.substring(s, e);
      final ns = s + next.length + 1;
      _setText(d, '${t.substring(0, s)}$next\n$block${t.substring(ne)}', TextSelection(baseOffset: ns, extentOffset: ns + block.length));
    }
  }

  Future<void> _paste() async {
    final d = doc;
    if (d == null) return;
    final t = (await Clipboard.getData(Clipboard.kTextPlain))?.text;
    if (t == null || t.isEmpty || !mounted) return;
    _insert(t);
  }

  void _selectAll() {
    final d = doc;
    if (d == null) return;
    d.ctl.selection = TextSelection(baseOffset: 0, extentOffset: d.ctl.text.length);
    focus.requestFocus();
  }

  void _toEdge(bool top) {
    final d = doc;
    if (d == null) return;
    d.ctl.selection = TextSelection.collapsed(offset: top ? 0 : d.ctl.text.length);
    if (d.v.hasClients) {
      d.v.animateTo(top ? 0 : d.v.position.maxScrollExtent, duration: const Duration(milliseconds: 280), curve: Curves.easeOutCubic);
    }
  }

  void _trim() {
    final d = doc;
    if (d == null) return;
    final t = d.ctl.text;
    final out = t.split('\n').map((l) => l.replaceFirst(RegExp(r'[ \t]+$'), '')).join('\n');
    if (out == t) {
      IosBanner.show(context, title: 'Nothing to trim', msg: 'No trailing spaces found.');
      return;
    }
    final at = d.ctl.selection.isValid ? d.ctl.selection.start : 0;
    _setText(d, out, TextSelection.collapsed(offset: at.clamp(0, out.length).toInt()));
  }

  void _snippets() {
    final d = doc;
    if (d == null) return;
    final list = _snippetsFor(d.ctl.ext);
    if (list.isEmpty) {
      IosBanner.show(context, title: 'No snippets', msg: 'There are none for this file type yet.');
      return;
    }
    showModalBottomSheet<void>(
      context: context,
      backgroundColor: C.sheet,
      showDragHandle: true,
      isScrollControlled: true,
      builder: (ctx) => SafeArea(
        child: ConstrainedBox(
          constraints: BoxConstraints(maxHeight: MediaQuery.of(ctx).size.height * .6),
          child: ListView(shrinkWrap: true, children: [
            for (final s in list)
              ListTile(
                dense: true,
                title: Text(s.$1, style: inter(15)),
                subtitle: Text(s.$2.replaceAll(_m, '').split('\n').first, maxLines: 1, overflow: TextOverflow.ellipsis, style: const TextStyle(fontFamily: 'monospace', fontSize: 12, color: C.muted)),
                onTap: () {
                  Navigator.pop(ctx);
                  _insertSnippet(d, s.$2);
                },
              ),
          ]),
        ),
      ),
    );
  }

  void _insertSnippet(_Doc d, String body) {
    final v = d.ctl.value;
    final t = v.text;
    final a = v.selection.isValid ? v.selection.start.clamp(0, t.length).toInt() : t.length;
    final b = v.selection.isValid ? v.selection.end.clamp(0, t.length).toInt() : t.length;
    final ls = a == 0 ? 0 : t.lastIndexOf('\n', a - 1) + 1;
    final ind = RegExp(r'^[ \t]*').stringMatch(t.substring(ls, a)) ?? '';
    var s = body.replaceAll('\n', '\n$ind');
    var caret = s.indexOf(_m);
    s = s.replaceFirst(_m, '');
    if (caret < 0) caret = s.length;
    _setText(d, t.replaceRange(a, b, s), TextSelection.collapsed(offset: a + caret));
  }

  // ------------------------------------------------------------ suggestions and formatting

  String? _cText;
  int _cOff = -1;
  String _cPath = '';
  Completion? _cVal;
  String? _supText; // after a suggestion is taken, stay quiet until the next edit

  /// What can be typed at the cursor (cached until the text or the cursor moves).
  Completion? _completion(_Doc d) {
    if (!p.suggest || !focus.hasFocus) return null;
    final v = d.ctl.value;
    if (!v.selection.isValid || !v.selection.isCollapsed) return null;
    final t = v.text;
    if (identical(t, _supText)) return null;
    final off = v.selection.start.clamp(0, t.length).toInt();
    if (identical(t, _cText) && off == _cOff && d.path == _cPath) return _cVal;
    Completion? r;
    try {
      r = completeAt(text: t, offset: off, path: d.path, files: p.files);
    } catch (_) {
      r = null; // never let a suggestion bug get in the way of typing
    }
    _cText = t;
    _cOff = off;
    _cPath = d.path;
    return _cVal = r;
  }

  void _acceptSug(_Doc d, Completion c, Sug s) {
    final v = d.ctl.value;
    final t = v.text;
    final off = v.selection.start.clamp(0, t.length).toInt();
    final r = applySuggestion(t, off, c.start.clamp(0, off).toInt(), s, d.path);
    _setText(d, r.text, TextSelection.collapsed(offset: r.caret.clamp(0, r.text.length).toInt()));
    _supText = d.ctl.text;
  }

  static const _kindStyle = <SK, (String, Color)>{
    SK.keyword: ('k', Color(0xFF569CD6)),
    SK.cls: ('C', Color(0xFFEE9D28)),
    SK.method: ('m', Color(0xFFB180D7)),
    SK.field: ('f', Color(0xFF75BEFF)),
    SK.variable: ('v', Color(0xFF75BEFF)),
    SK.snippet: ('s', Color(0xFFD7BA7D)),
    SK.tag: ('<>', Color(0xFFE06C75)),
    SK.attr: ('a', Color(0xFF9CDCFE)),
    SK.value: ('=', Color(0xFFCE9178)),
    SK.file: ('F', Color(0xFFCCCCCC)),
    SK.resource: ('R', Color(0xFF4EC9B0)),
    SK.module: ('M', Color(0xFFC586C0)),
  };

  Widget _sugRow(_Doc d, Completion c, Sug s, bool first, double w) {
    final k = _kindStyle[s.kind] ?? ('?', _vsDim);
    final typed = d.ctl.text.length >= c.start && d.ctl.selection.isValid ? d.ctl.text.substring(c.start.clamp(0, d.ctl.text.length).toInt(), d.ctl.selection.start.clamp(0, d.ctl.text.length).toInt()) : '';
    final lab = s.label;
    final hit = typed.isNotEmpty && lab.toLowerCase().startsWith(typed.toLowerCase()) ? typed.length : 0;
    const base = TextStyle(fontFamily: 'monospace', fontSize: 13, color: _vsText);
    return GestureDetector(
      behavior: HitTestBehavior.opaque,
      onTap: () => _acceptSug(d, c, s),
      child: Container(
        height: 32,
        color: first ? _vsSel : null,
        padding: const EdgeInsets.symmetric(horizontal: 8),
        child: Row(children: [
          Container(
            width: 22,
            height: 20,
            alignment: Alignment.center,
            decoration: BoxDecoration(color: k.$2.withOpacity(.18), borderRadius: BorderRadius.circular(4)),
            child: RichText(text: TextSpan(text: k.$1, style: TextStyle(fontSize: 11, fontWeight: FontWeight.w700, color: k.$2))),
          ),
          const SizedBox(width: 8),
          Flexible(
            child: RichText(
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              text: TextSpan(style: base, children: [
                if (hit > 0) TextSpan(text: lab.substring(0, hit), style: const TextStyle(color: Color(0xFF18A3FF), fontWeight: FontWeight.w700)),
                TextSpan(text: lab.substring(hit)),
              ]),
            ),
          ),
          if (s.detail.isNotEmpty) ...[
            const SizedBox(width: 10),
            Expanded(
              child: RichText(
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                textAlign: TextAlign.right,
                text: TextSpan(text: s.detail, style: const TextStyle(fontSize: 11, color: _vsDim)),
              ),
            ),
          ] else
            const Spacer(),
          if (s.imp != null) const Padding(padding: EdgeInsets.only(left: 6), child: Icon(Icons.add_link_rounded, size: 13, color: _vsDim)),
        ]),
      ),
    );
  }

  /// The suggestion list, floating next to the word being typed (a strip at the bottom when words wrap).
  Widget _sugLayer(_Doc d) {
    final fs = p.fontSize;
    final scaler = MediaQuery.textScalerOf(context);
    final lh = measureLineHeight(fs, scaler);
    final cw = measureCharWidth(fs, scaler);
    final wrap = p.wrap || d.longest > _autoWrapLine;
    final gutter = wrap ? 0.0 : 14.0 + fs * .62 * '${d.lineCount}'.length + 6;
    return LayoutBuilder(
      builder: (context, cons) => AnimatedBuilder(
        animation: Listenable.merge([d.ctl, d.v, d.h, focus]),
        builder: (_, __) {
          final c = _completion(d);
          if (c == null || c.items.isEmpty) return const SizedBox.shrink();
          Widget strip(Alignment where) {
            return Align(
              alignment: where,
              child: Container(
                height: 38,
                decoration: BoxDecoration(color: C.sheet, border: const Border(top: BorderSide(color: C.line))),
                child: ListView(
                  scrollDirection: Axis.horizontal,
                  padding: const EdgeInsets.fromLTRB(8, 5, 4, 5),
                  children: [
                    for (final s in c.items)
                      Padding(
                        padding: const EdgeInsets.only(right: 6),
                        child: GestureDetector(
                          behavior: HitTestBehavior.opaque,
                          onTap: () => _acceptSug(d, c, s),
                          child: Container(
                            padding: const EdgeInsets.symmetric(horizontal: 12),
                            alignment: Alignment.center,
                            decoration: BoxDecoration(color: C.tealDeep, borderRadius: BorderRadius.circular(8)),
                            child: RichText(text: TextSpan(text: s.label, style: const TextStyle(fontFamily: 'monospace', fontSize: 14, color: C.teal))),
                          ),
                        ),
                      ),
                  ],
                ),
              ),
            );
          }
          if (wrap) return strip(Alignment.bottomCenter);
          final text = d.ctl.text;
          final at = c.start.clamp(0, text.length).toInt();
          final line = d.lineAt(at);
          final col = at - d.lineStarts[line];
          final y = 8 + line * lh - (d.v.hasClients ? d.v.offset : 0.0);
          final x = gutter + 4 + col * cw - (d.h.hasClients ? d.h.offset : 0.0);
          if (y < -lh || y > cons.maxHeight) return const SizedBox.shrink();
          final w = (cons.maxWidth - 8).clamp(180.0, 340.0).toDouble();
          // The list must never cover the line being typed: use the room below the cursor line
          // or above it (whichever fits more rows). With the keyboard up there may be room for
          // only a few lines, so show fewer rows, and when not even 2 rows fit use the strip.
          final want = c.items.length > 6 ? 6 : c.items.length;
          final below = cons.maxHeight - (y + lh + 2) - 4;
          final above = y - 2 - 4;
          int fit(double room) => ((room - 2) / 32).floor().clamp(0, want).toInt();
          final rowsBelow = fit(below), rowsAbove = fit(above);
          final minRows = want < 2 ? want : 2;
          if (rowsBelow < minRows && rowsAbove < minRows) {
            return strip(y > cons.maxHeight / 2 ? Alignment.topCenter : Alignment.bottomCenter);
          }
          final useBelow = rowsBelow >= want || rowsBelow >= rowsAbove;
          final rows = useBelow ? rowsBelow : rowsAbove;
          final h = rows * 32.0 + 2;
          final top = useBelow ? y + lh + 2 : y - h - 2;
          final left = x.clamp(4.0, (cons.maxWidth - w - 4).clamp(4.0, 4000.0)).toDouble();
          return Stack(clipBehavior: Clip.none, children: [
            Positioned(
              left: left,
              top: top,
              width: w,
              height: h,
              child: Container(
                decoration: BoxDecoration(
                  color: _vsSide,
                  borderRadius: BorderRadius.circular(6),
                  border: Border.all(color: _vsLine),
                  boxShadow: const [BoxShadow(color: Color(0x66000000), blurRadius: 10, offset: Offset(0, 3))],
                ),
                clipBehavior: Clip.antiAlias,
                child: ListView.builder(
                  padding: EdgeInsets.zero,
                  itemExtent: 32,
                  physics: const ClampingScrollPhysics(),
                  itemCount: c.items.length,
                  itemBuilder: (_, i) => _sugRow(d, c, c.items[i], i == 0, w),
                ),
              ),
            ),
          ]);
        },
      ),
    );
  }

  // Count the letters and digits before the cursor, then find the same spot in the formatted text.
  int _mapCaret(String a, int off, String b) {
    var n = 0;
    for (var i = 0; i < off && i < a.length; i++) {
      final c = a.codeUnitAt(i);
      if (c != 32 && c != 9 && c != 10 && c != 13) n++;
    }
    var seen = 0, i = 0;
    while (i < b.length && seen < n) {
      final c = b.codeUnitAt(i);
      if (c != 32 && c != 9 && c != 10 && c != 13) seen++;
      i++;
    }
    // the cursor was at the start of a line: land at the start of that line, not the end of the one above
    final gap = a.substring(0, off.clamp(0, a.length).toInt());
    final lastNon = gap.replaceAll(RegExp(r'[ \t]+$'), '');
    if (lastNon.endsWith('\n') || (gap.isNotEmpty && gap.endsWith('\n'))) {
      final nl = b.indexOf('\n', i);
      if (nl >= 0 && b.substring(i, nl).trim().isEmpty) {
        i = nl + 1;
        while (i < b.length && (b[i] == ' ' || b[i] == '\t')) {
          i++;
        }
      }
    }
    return i.clamp(0, b.length).toInt();
  }

  Future<void> _format() async {
    final d = doc;
    if (d == null) return;
    final ext = d.ctl.ext;
    if (!canFormat(ext)) {
      return _err('Can\u2019t format .$ext files', 'It works for Kotlin, Java, Dart, JavaScript, TypeScript, HTML, CSS, JSON, XML, YAML and Gradle files.');
    }
    final src = d.ctl.text;
    final sel = d.ctl.selection;
    String? out;
    String? problem;
    try {
      if (src.length > 40000) {
        final o = _fo;
        final r = await compute(_formatJob, [src, ext, '${o.tabWidth ?? ''}', o.singleQuote ? '1' : '0', '${o.printWidth}']);
        if (r[0] == 'ok') {
          out = r[1];
        } else {
          problem = r[1];
        }
      } else {
        out = formatSource(src, ext, opts: _fo);
      }
    } on FormatException catch (e) {
      problem = e.message;
    } catch (_) {
      problem = 'Something went wrong. Your code was not changed.';
    }
    if (!mounted) return;
    if (problem != null || out == null) return _err('Couldn\u2019t format', problem ?? 'Your code was not changed.');
    if (d.ctl.text != src) return; // typed while it was working: leave it
    final name = d.path.split('/').last;
    if (out == src) {
      IosBanner.show(context, title: 'Already formatted', msg: name);
      return;
    }
    final at = sel.isValid ? sel.start.clamp(0, src.length).toInt() : 0;
    _setText(d, out, TextSelection.collapsed(offset: _mapCaret(src, at, out)));
    _supText = d.ctl.text;
    IosBanner.show(context, kind: BannerKind.ok, title: 'Formatted', msg: '$name, ${_spacesFor(ext)} spaces');
  }

  // ------------------------------------------------------------ Prettier settings

  void _loadFmtPrefs() {
    final pr = Api.i.prefs;
    _fmtTab = int.tryParse(pr.getString('fmt_tab') ?? '') ?? 0;
    _fmtSingle = pr.getString('fmt_single') == '1';
    _fmtWidth = int.tryParse(pr.getString('fmt_width') ?? '') ?? 80;
    _fmtOnSave = pr.getString('fmt_onsave') == '1';
    if (_fmtTab != 0 && _fmtTab != 2 && _fmtTab != 4) _fmtTab = 0;
    if (_fmtWidth != 80 && _fmtWidth != 100 && _fmtWidth != 120) _fmtWidth = 80;
  }

  void _saveFmtPrefs() {
    final pr = Api.i.prefs;
    pr.setString('fmt_tab', '$_fmtTab');
    pr.setString('fmt_single', _fmtSingle ? '1' : '0');
    pr.setString('fmt_width', '$_fmtWidth');
    pr.setString('fmt_onsave', _fmtOnSave ? '1' : '0');
  }

  /// The .prettierrc (or .prettierrc.json) in the project, when it sets something we use.
  FormatOptions? _rcOptions() {
    final names = p.files.keys.where((k) {
      final n = k.split('/').last;
      return n == '.prettierrc' || n == '.prettierrc.json';
    }).toList()
      ..sort((a, b) => a.length.compareTo(b.length));
    final base = FormatOptions(tabWidth: _fmtTab == 0 ? null : _fmtTab, singleQuote: _fmtSingle, printWidth: _fmtWidth);
    for (final n in names) {
      final open = docs.where((e) => e.path == n);
      final text = open.isNotEmpty ? open.first.ctl.text : utf8.decode(p.files[n]!, allowMalformed: true);
      final o = parsePrettierrc(text, base);
      if (o != null) return o;
    }
    return null;
  }

  /// What Format uses: the project's .prettierrc if it has one, else the settings.
  FormatOptions get _fo => _rcOptions() ?? FormatOptions(tabWidth: _fmtTab == 0 ? null : _fmtTab, singleQuote: _fmtSingle, printWidth: _fmtWidth);

  int _spacesFor(String ext) => _fo.tabWidth ?? defaultIndent(ext);

  /// Format on save: tidies a file the moment you leave it (another tab, save zip, build). Skips big
  /// files and anything it can't format, so it never blocks you or changes code it isn't sure about.
  void _formatOnSave(_Doc d) {
    if (!_fmtOnSave) return;
    final ext = d.ctl.ext;
    final src = d.ctl.text;
    if (!canFormat(ext) || src.length > 40000) return;
    String out;
    try {
      out = formatSource(src, ext, opts: _fo);
    } catch (_) {
      return;
    }
    if (out == src) return;
    final sel = d.ctl.selection;
    final at = sel.isValid ? sel.start.clamp(0, src.length).toInt() : 0;
    d.ctl.value = TextEditingValue(text: out, selection: TextSelection.collapsed(offset: _mapCaret(src, at, out)));
    d.syncView(force: true);
    d.dirty = true;
  }

  /// Format just the lines you selected (like VS Code's "Format Selection").
  void _formatSelection() {
    final d = doc;
    if (d == null) return;
    final ext = d.ctl.ext;
    if (!canFormat(ext)) {
      return _err('Can\u2019t format .$ext files', 'It works for Kotlin, Java, Dart, JavaScript, TypeScript, HTML, CSS, JSON, XML, YAML and Gradle files.');
    }
    final text = d.ctl.text;
    final sel = d.ctl.selection;
    if (!sel.isValid || sel.isCollapsed) {
      return _err('Select some lines first', 'Select the code you want tidied, or use Format document for the whole file.');
    }
    final a = sel.start.clamp(0, text.length).toInt();
    final b = sel.end.clamp(0, text.length).toInt();
    final ls = a == 0 ? 0 : text.lastIndexOf('\n', a - 1) + 1;
    var le = b > ls && text[b - 1] == '\n' ? b - 1 : b;
    final nl = text.indexOf('\n', le);
    le = nl < 0 ? text.length : nl;
    final chunk = text.substring(ls, le);
    if (chunk.trim().isEmpty) return _err('Nothing to format', 'The lines you selected are empty.');
    final base = RegExp(r'^[ \t]*').stringMatch(chunk) ?? '';
    String out;
    try {
      out = formatSource(chunk, ext, opts: _fo);
    } on FormatException catch (e) {
      return _err('Couldn\u2019t format the selection', '${e.message} Select whole blocks (every bracket or tag with its partner) and try again.');
    } catch (_) {
      return _err('Couldn\u2019t format the selection', 'Your code was not changed.');
    }
    if (base.isNotEmpty && (out.contains('`') || out.contains('"""') || out.contains("'''") || out.contains('<pre') || out.contains('<textarea'))) {
      return _err('Couldn\u2019t format the selection', 'It has text that must keep its exact spacing. Use Format document instead.');
    }
    out = out.trimRight().split('\n').map((l) => l.isEmpty ? l : '$base$l').join('\n');
    if (out == chunk) {
      IosBanner.show(context, title: 'Already formatted', msg: 'Selection');
      return;
    }
    _setText(d, text.replaceRange(ls, le, out), TextSelection(baseOffset: ls, extentOffset: ls + out.length));
    _supText = d.ctl.text;
    IosBanner.show(context, kind: BannerKind.ok, title: 'Formatted', msg: 'Selection, ${_spacesFor(ext)} spaces');
  }

  void _prettierSheet() {
    final d = doc;
    showModalBottomSheet<void>(
      context: context,
      backgroundColor: C.sheet,
      showDragHandle: true,
      isScrollControlled: true,
      builder: (ctx) => StatefulBuilder(
        builder: (ctx, setS) {
          final rc = _rcOptions() != null;
          void upd(VoidCallback f) {
            setState(f);
            setS(() {});
            _saveFmtPrefs();
          }

          Widget seg(List<(String, bool, VoidCallback)> items) => Row(children: [
                for (final it in items)
                  Padding(
                    padding: const EdgeInsets.only(left: 6),
                    child: GestureDetector(
                      onTap: rc ? null : it.$3,
                      child: AnimatedContainer(
                        duration: const Duration(milliseconds: 140),
                        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 7),
                        decoration: BoxDecoration(
                          color: it.$2 ? C.tealDeep : C.field,
                          borderRadius: BorderRadius.circular(9),
                          border: Border.all(color: it.$2 ? C.teal : C.line),
                        ),
                        child: Text(it.$1, style: inter(13, w: FontWeight.w600, c: rc ? C.muted : (it.$2 ? C.teal : C.text))),
                      ),
                    ),
                  ),
              ]);

          Widget row(String title, String sub, Widget trailing) => Padding(
                padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 8),
                child: Row(children: [
                  Expanded(
                    child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                      Text(title, style: inter(15, w: FontWeight.w600)),
                      if (sub.isNotEmpty) Text(sub, style: inter(12, c: C.muted)),
                    ]),
                  ),
                  trailing,
                ]),
              );

          return SafeArea(
            child: SingleChildScrollView(
              child: Column(mainAxisSize: MainAxisSize.min, crossAxisAlignment: CrossAxisAlignment.start, children: [
                Padding(
                  padding: const EdgeInsets.fromLTRB(20, 0, 20, 4),
                  child: Row(children: [
                    const Icon(Icons.auto_fix_high_rounded, color: C.teal, size: 22),
                    const SizedBox(width: 10),
                    Text('Prettier', style: inter(19, w: FontWeight.w700)),
                    const SizedBox(width: 8),
                    Text('Code formatter', style: inter(13, c: C.muted)),
                  ]),
                ),
                if (rc)
                  Padding(
                    padding: const EdgeInsets.fromLTRB(20, 6, 20, 2),
                    child: Text('Using the .prettierrc in your project, so these are set by that file.', style: inter(12.5, c: C.teal)),
                  ),
                row(
                  'Format on save',
                  'Tidies a file when you switch tabs, save the zip or build',
                  Switch(value: _fmtOnSave, activeColor: C.teal, onChanged: (v) => upd(() => _fmtOnSave = v)),
                ),
                row('Tab size', 'Spaces per indent', seg([
                  ('Auto', _fmtTab == 0, () => upd(() => _fmtTab = 0)),
                  ('2', _fmtTab == 2, () => upd(() => _fmtTab = 2)),
                  ('4', _fmtTab == 4, () => upd(() => _fmtTab = 4)),
                ])),
                row('Quotes', 'For JavaScript and TypeScript', seg([
                  ('Double', !_fmtSingle, () => upd(() => _fmtSingle = false)),
                  ('Single', _fmtSingle, () => upd(() => _fmtSingle = true)),
                ])),
                row('Line width', 'For HTML and XML', seg([
                  ('80', _fmtWidth == 80, () => upd(() => _fmtWidth = 80)),
                  ('100', _fmtWidth == 100, () => upd(() => _fmtWidth = 100)),
                  ('120', _fmtWidth == 120, () => upd(() => _fmtWidth = 120)),
                ])),
                if (d != null)
                  Padding(
                    padding: const EdgeInsets.fromLTRB(20, 10, 20, 12),
                    child: Row(children: [
                      Expanded(
                        child: _fmtBtn('Format document', Icons.auto_fix_high_rounded, () {
                          Navigator.pop(ctx);
                          _format();
                        }),
                      ),
                      const SizedBox(width: 10),
                      Expanded(
                        child: _fmtBtn('Format selection', Icons.format_align_justify_rounded, () {
                          Navigator.pop(ctx);
                          _formatSelection();
                        }),
                      ),
                    ]),
                  ),
                if (d == null) const SizedBox(height: 14),
              ]),
            ),
          );
        },
      ),
    );
  }

  Widget _fmtBtn(String t, IconData i, VoidCallback f) => GestureDetector(
        onTap: f,
        child: Container(
          padding: const EdgeInsets.symmetric(vertical: 12),
          decoration: BoxDecoration(color: C.tealDeep, borderRadius: BorderRadius.circular(12), border: Border.all(color: C.teal)),
          child: Row(mainAxisAlignment: MainAxisAlignment.center, children: [
            Icon(i, size: 17, color: C.teal),
            const SizedBox(width: 6),
            Text(t, style: inter(13.5, w: FontWeight.w600, c: C.teal)),
          ]),
        ),
      );

  // ------------------------------------------------------------ find and replace

  void _hitsRefresh() {
    final d = doc;
    hits = [];
    hit = -1;
    final q = findC.text;
    if (findOn && d != null && q.isNotEmpty) {
      // Matched on the real text: lower-casing the text first can change its length and shift every hit.
      final re = RegExp(RegExp.escape(q), caseSensitive: caseSens);
      for (final m in re.allMatches(d.ctl.text)) {
        if (hits.length >= 5000) break;
        hits.add(m.start);
      }
      final c = d.ctl.selection.isValid ? d.ctl.selection.start : 0;
      hit = hits.isEmpty ? -1 : hits.indexWhere((h) => h >= c);
      if (hits.isNotEmpty && hit < 0) hit = 0;
    }
    if (mounted) setState(() {});
  }

  void _selectHit() {
    final d = doc;
    if (d == null || hit < 0 || hit >= hits.length) return;
    final s = hits[hit];
    d.ctl.selection = TextSelection(baseOffset: s, extentOffset: s + findC.text.length);
    _scrollToLine(d, '\n'.allMatches(d.ctl.text.substring(0, s)).length + 1);
    focus.requestFocus();
    setState(() {});
  }

  void _step(int dir) {
    if (hits.isEmpty) return;
    hit = (hit + dir + hits.length) % hits.length;
    _selectHit();
  }

  void _replaceOne() {
    final d = doc;
    if (d == null || hit < 0 || hit >= hits.length) return;
    final s = hits[hit];
    final t = d.ctl.text.replaceRange(s, s + findC.text.length, replC.text);
    d.ctl.value = TextEditingValue(text: t, selection: TextSelection.collapsed(offset: s + replC.text.length));
    d.syncView(force: true);
    d.dirty = true;
    d.t?.cancel();
    d.t = Timer(const Duration(milliseconds: 500), () => _flush(d));
    _hitsRefresh();
    _selectHit();
  }

  void _replaceAll() {
    final d = doc;
    if (d == null || hits.isEmpty) return;
    var t = d.ctl.text;
    for (final s in hits.reversed) {
      t = t.replaceRange(s, s + findC.text.length, replC.text);
    }
    final n = hits.length;
    d.ctl.value = TextEditingValue(text: t, selection: const TextSelection.collapsed(offset: 0));
    _changed(d);
    IosBanner.show(context, kind: BannerKind.ok, title: 'Replaced $n', msg: findC.text);
  }

  // ------------------------------------------------------------ UI

  Widget _barBtn(IconData i, String tip, VoidCallback? f, {Color c = C.text}) => SizedBox(
        width: 38,
        height: 38,
        child: IconButton(padding: EdgeInsets.zero, onPressed: f, tooltip: tip, icon: Icon(i, size: 21, color: f == null ? C.muted : c)),
      );


  void _menuAct(String v) {
    switch (v) {
      case 'find':
        setState(() => findOn = !findOn);
        _hitsRefresh();
      case 'goto':
        _gotoDialog();
      case 'search':
        _projectSearch();
      case 'snippet':
        _snippets();
      case 'top':
        _toEdge(true);
      case 'bottom':
        _toEdge(false);
      case 'trim':
        _trim();
      case 'format':
        _format();
      case 'formatsel':
        _formatSelection();
      case 'prettier':
        _prettierSheet();
      case 'suggest':
        setState(() => p.suggest = !p.suggest);
      case 'wrap':
        setState(() => p.wrap = !p.wrap);
      case 'big':
        setState(() => p.fontSize = (p.fontSize + 1).clamp(9, 22).toDouble());
        for (final d in docs) {
          d.syncView(force: true);
        }
      case 'small':
        setState(() => p.fontSize = (p.fontSize - 1).clamp(9, 22).toDouble());
        for (final d in docs) {
          d.syncView(force: true);
        }
      case 'new':
        _newFile();
      case 'phone':
        _addFromPhone();
      case 'check':
        _preflight(building: false);
      case 'save':
        _saveZip();
      case 'open':
        _openZip();
      case 'close':
        _closeProject();
    }
  }

  PopupMenuEntry<String> _mi(String v, IconData i, String t) => PopupMenuItem<String>(
        value: v,
        height: 40,
        child: Row(children: [Icon(i, size: 20, color: C.muted), const SizedBox(width: 12), Text(t, style: inter(14.5))]),
      );

  Widget _top() {
    final d = doc;
    final parts = d?.path.split('/') ?? <String>[];
    return Container(
      height: 46,
      color: _vsTitle,
      child: Row(children: [
        _barBtn(Icons.arrow_back_ios_new_rounded, 'Back', () => Navigator.of(context).maybePop()),
        _barBtn(Icons.folder_outlined, 'Files', p.isOpen ? () => scaffold.currentState?.openDrawer() : null),
        Expanded(
          child: Padding(
            padding: const EdgeInsets.only(left: 2),
            child: Column(mainAxisAlignment: MainAxisAlignment.center, crossAxisAlignment: CrossAxisAlignment.start, children: [
              Text(p.isOpen ? (d == null ? '${p.name}.zip' : parts.last) : 'Code editor', maxLines: 1, overflow: TextOverflow.ellipsis, style: inter(15, w: FontWeight.w700)),
              if (p.isOpen) Text(d == null ? '${p.files.length} files' : (parts.length > 1 ? parts.sublist(0, parts.length - 1).join('/') : p.name), maxLines: 1, overflow: TextOverflow.ellipsis, style: inter(11, c: C.muted)),
            ]),
          ),
        ),
        if (p.isOpen && _hasWebPage) _barBtn(Icons.play_arrow_rounded, 'Preview', _preview, c: const Color(0xFF89D185)),
        if (d != null) _barBtn(Icons.search_rounded, 'Find', () => _menuAct('find'), c: findOn ? C.teal : C.text),
        PopupMenuButton<String>(
          color: C.sheet,
          icon: const Icon(Icons.more_vert_rounded, size: 21, color: C.text),
          padding: EdgeInsets.zero,
          onSelected: _menuAct,
          itemBuilder: (_) => [
            if (d != null) _mi('format', Icons.auto_fix_high_rounded, 'Format document'),
            if (d != null) _mi('formatsel', Icons.format_align_justify_rounded, 'Format selection'),
            _mi('prettier', Icons.tune_rounded, 'Prettier settings'),
            if (d != null) _mi('find', Icons.find_replace_rounded, 'Find and replace'),
            if (d != null) _mi('goto', Icons.format_list_numbered_rounded, 'Go to line'),
            if (d != null) _mi('top', Icons.vertical_align_top_rounded, 'Go to top'),
            if (d != null) _mi('bottom', Icons.vertical_align_bottom_rounded, 'Go to bottom'),
            if (d != null) _mi('snippet', Icons.integration_instructions_outlined, 'Insert snippet'),
            if (d != null) _mi('trim', Icons.format_align_left_rounded, 'Trim trailing spaces'),
            if (p.isOpen) _mi('search', Icons.manage_search_rounded, 'Search in project'),
            if (d != null) _mi('suggest', Icons.lightbulb_outline_rounded, p.suggest ? 'Suggestions: on' : 'Suggestions: off'),
            if (d != null) _mi('wrap', Icons.wrap_text_rounded, p.wrap ? 'Word wrap: on' : 'Word wrap: off'),
            if (d != null) _mi('big', Icons.text_increase_rounded, 'Bigger text'),
            if (d != null) _mi('small', Icons.text_decrease_rounded, 'Smaller text'),
            if (p.isOpen) _mi('new', Icons.note_add_outlined, 'New file'),
            if (p.isOpen) _mi('phone', Icons.upload_file_rounded, 'Add a file from your phone'),
            if (p.isOpen) _mi('check', Icons.fact_check_outlined, 'Check code (no build)'),
            if (p.isOpen) _mi('save', Icons.download_rounded, 'Save zip to Downloads'),
            _mi('open', Icons.folder_open_rounded, 'Open another zip'),
            if (p.isOpen) _mi('close', Icons.close_rounded, 'Close project'),
          ],
        ),
        if (p.isOpen)
          Padding(
            padding: const EdgeInsets.only(right: 8, left: 2),
            child: SizedBox(
              height: 30,
              child: FilledButton.icon(
                onPressed: busy ? null : _build,
                style: FilledButton.styleFrom(backgroundColor: _vsBtn, foregroundColor: Colors.white, padding: const EdgeInsets.symmetric(horizontal: 12), shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(3))),
                icon: busy ? const SizedBox(width: 15, height: 15, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white)) : const Icon(Icons.play_arrow_rounded, size: 19),
                label: Text('Build', style: inter(13.5, w: FontWeight.w600, c: Colors.white)),
              ),
            ),
          ),
      ]),
    );
  }

  Widget _tabs() => Container(
        height: 35,
        color: _vsSide,
        child: ListView.builder(
          scrollDirection: Axis.horizontal,
          itemCount: docs.length,
          itemBuilder: (_, i) {
            final d = docs[i], on = i == cur;
            final name = d.path.split('/').last;
            return GestureDetector(
              behavior: HitTestBehavior.opaque,
              onTap: () {
                final old = doc;
                if (old != null && old.dirty && i != cur) {
                  _formatOnSave(old);
                  _flush(old);
                }
                setState(() => cur = i);
                _hitsRefresh();
              },
              child: Container(
                padding: const EdgeInsets.only(left: 12, right: 2),
                decoration: BoxDecoration(
                  color: on ? _vsEditor : _vsTabOff,
                  border: Border(top: BorderSide(color: on ? _vsBlue : Colors.transparent, width: 1), right: const BorderSide(color: _vsSide)),
                ),
                child: Row(children: [
                  Icon(_iconFor(d.path), size: 15, color: _colorFor(d.path)),
                  const SizedBox(width: 6),
                  Text(name, style: inter(13, c: on ? Colors.white : const Color(0xFF969696))),
                  SizedBox(
                    width: 28,
                    height: 34,
                    child: d.dirty
                        ? const Center(child: Icon(Icons.circle, size: 9, color: Color(0xFFCCCCCC)))
                        : IconButton(padding: EdgeInsets.zero, onPressed: () => _closeDoc(i), tooltip: 'Close', icon: Icon(Icons.close_rounded, size: 15, color: on ? const Color(0xFFCCCCCC) : Colors.transparent)),
                  ),
                ]),
              ),
            );
          },
        ),
      );

  Widget _findBar() {
    InputDecoration dec(String h) => InputDecoration(isDense: true, hintText: h, hintStyle: inter(13, c: C.muted), contentPadding: const EdgeInsets.symmetric(horizontal: 10, vertical: 9), filled: true, fillColor: C.card, border: OutlineInputBorder(borderRadius: BorderRadius.circular(8), borderSide: BorderSide.none));
    return Container(
      padding: const EdgeInsets.fromLTRB(8, 5, 4, 5),
      decoration: const BoxDecoration(border: Border(bottom: BorderSide(color: C.line))),
      child: Column(children: [
        Row(children: [
          Expanded(child: SizedBox(height: 34, child: TextField(controller: findC, onChanged: (_) => _hitsRefresh(), style: inter(13.5), cursorColor: C.teal, decoration: dec('Find')))),
          SizedBox(width: 62, child: Text(hits.isEmpty ? (findC.text.isEmpty ? '' : 'No matches') : '${hit + 1} of ${hits.length}', textAlign: TextAlign.center, maxLines: 1, overflow: TextOverflow.ellipsis, style: inter(12, c: C.muted))),
          _barBtn(Icons.keyboard_arrow_up_rounded, 'Previous', hits.isEmpty ? null : () => _step(-1)),
          _barBtn(Icons.keyboard_arrow_down_rounded, 'Next', hits.isEmpty ? null : () => _step(1)),
        ]),
        const SizedBox(height: 4),
        Row(children: [
          Expanded(child: SizedBox(height: 34, child: TextField(controller: replC, style: inter(13.5), cursorColor: C.teal, decoration: dec('Replace with')))),
          const SizedBox(width: 4),
          TextButton(onPressed: hits.isEmpty ? null : _replaceOne, style: TextButton.styleFrom(minimumSize: const Size(0, 34), padding: const EdgeInsets.symmetric(horizontal: 8)), child: Text('Replace', style: inter(13, c: hits.isEmpty ? C.muted : C.teal, w: FontWeight.w600))),
          TextButton(onPressed: hits.isEmpty ? null : _replaceAll, style: TextButton.styleFrom(minimumSize: const Size(0, 34), padding: const EdgeInsets.symmetric(horizontal: 8)), child: Text('All', style: inter(13, c: hits.isEmpty ? C.muted : C.teal, w: FontWeight.w600))),
          GestureDetector(
            behavior: HitTestBehavior.opaque,
            onTap: () {
              setState(() => caseSens = !caseSens);
              _hitsRefresh();
            },
            child: Tooltip(
              message: 'Match case',
              child: Container(width: 34, height: 30, alignment: Alignment.center, decoration: BoxDecoration(color: caseSens ? C.tealDeep : null, borderRadius: BorderRadius.circular(6)), child: Text('Aa', style: inter(13, w: FontWeight.w700, c: caseSens ? C.teal : C.muted))),
            ),
          ),
        ]),
      ]),
    );
  }

  Widget _editor(_Doc d) {
    d.measure();
    final fs = p.fontSize;
    final wrap = p.wrap || d.longest > _autoWrapLine;
    final scaler = MediaQuery.textScalerOf(context);
    final lh = measureLineHeight(fs, scaler);
    final mono = TextStyle(fontFamily: 'monospace', fontSize: fs, height: 1.5, color: _vsText);
    final gutter = wrap ? 0.0 : 14.0 + fs * .62 * '${d.lineCount}'.length + 6;
    return LayoutBuilder(builder: (context, cons) {
      // The text field scrolls itself (vertically), so only what is on screen is painted and the
      // caret is always kept in view; the line numbers follow its scroll position.
      final field = TextField(
        key: ValueKey(d.path),
        controller: d.ctl,
        focusNode: focus,
        undoController: d.undo,
        scrollController: d.v,
        scrollPhysics: const ClampingScrollPhysics(),
        onChanged: (_) => _changed(d),
        maxLines: null,
        expands: true,
        textAlignVertical: TextAlignVertical.top,
        keyboardType: TextInputType.multiline,

        textCapitalization: TextCapitalization.none,
        inputFormatters: [_EditFormatter(const {'xml', 'html', 'htm', 'svg'}.contains(d.ctl.ext), single: const {'dart', 'js', 'mjs', 'css'}.contains(d.ctl.ext))],
        cursorColor: const Color(0xFFAEAFAD),
        style: mono,
        // contentPadding is NOT scroll room: it shrinks the area the text may draw in (a 160 bottom padding
        // cut the code off 160 px above the bottom and left a black block). The room to keep the caret clear
        // of the suggestion strip comes from scrollPadding instead.
        scrollPadding: const EdgeInsets.fromLTRB(20, 20, 20, 70),
        decoration: const InputDecoration(isCollapsed: true, border: InputBorder.none, contentPadding: EdgeInsets.fromLTRB(4, 8, 8, 8)),
      );
      Widget band() => AnimatedBuilder(
            animation: Listenable.merge([d.v, d.ctl]),
            builder: (_, __) {
              final sel = d.ctl.selection;
              if (wrap || !sel.isValid || !sel.isCollapsed) return const SizedBox.expand();
              final top = 8 + d.lineAt(sel.start) * lh - (d.v.hasClients ? d.v.offset : 0.0);
              return CustomPaint(size: Size.infinite, painter: _BandPainter(top, lh));
            },
          );
      final Widget text;
      if (wrap) {
        text = Stack(children: [Positioned.fill(child: field)]);
      } else {
        final w = (d.longest * fs * .62 + 48).clamp(cons.maxWidth - gutter, 200000.0).toDouble();
        text = SingleChildScrollView(
          key: ValueKey('h${d.path}'),
          controller: d.h,
          scrollDirection: Axis.horizontal,
          physics: const ClampingScrollPhysics(),
          child: SizedBox(
            width: w,
            height: cons.maxHeight,
            child: Stack(children: [Positioned.fill(child: band()), Positioned.fill(child: field)]),
          ),
        );
      }
      return Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
        if (!wrap)
          SizedBox(
            width: gutter,
            height: cons.maxHeight,
            child: AnimatedBuilder(
              animation: d.ctl,
              builder: (_, __) {
                final sel = d.ctl.selection;
                final cur = sel.isValid ? d.lineAt(sel.start) : -1;
                return CustomPaint(size: Size.infinite, painter: _GutterPainter(d.v, lh, fs, d.lineCount, cur, scaler));
              },
            ),
          ),
        Expanded(
          child: Scrollbar(
            controller: d.v,
            interactive: true,
            thickness: 5,
            radius: const Radius.circular(3),
            notificationPredicate: (n) => n.metrics.axis == Axis.vertical,
            child: text,
          ),
        ),
      ]);
    });
  }

  Widget _status(_Doc d) => Container(
        height: 22,
        color: _vsBlue,
        padding: const EdgeInsets.symmetric(horizontal: 10),
        child: ValueListenableBuilder<TextEditingValue>(
          valueListenable: d.ctl,
          builder: (_, v, __) {
            final off = v.selection.isValid ? v.selection.start : 0;
            final ln = d.lineAt(off);
            final col = off - d.lineStarts[ln] + 1;
            final picked = v.selection.isValid && !v.selection.isCollapsed ? ' (${v.selection.end - v.selection.start} selected)' : '';
            const st = TextStyle(fontSize: 11.5, color: Colors.white);
            final lang = d.ctl.ext.isEmpty ? 'Plain Text' : d.ctl.ext.toUpperCase();
            return Row(children: [
              Text('Ln ${ln + 1}, Col $col$picked', style: st),
              Expanded(
                child: Align(
                  alignment: Alignment.centerRight,
                  child: FittedBox(
                    fit: BoxFit.scaleDown,
                    child: Row(mainAxisSize: MainAxisSize.min, children: [
              Text(d.dirty ? 'Saving\u2026   ' : 'Saved   ', style: st),
              GestureDetector(
                behavior: HitTestBehavior.opaque,
                onTap: _prettierSheet,
                child: Text('Spaces: ${_spacesFor(d.ctl.ext)}   ', style: st),
              ),
              Text('UTF-8   LF   {} $lang', style: st),
              if (canFormat(d.ctl.ext))
                GestureDetector(
                  behavior: HitTestBehavior.opaque,
                  onTap: _prettierSheet,
                  child: Row(mainAxisSize: MainAxisSize.min, children: [
                    const SizedBox(width: 12),
                    const Icon(Icons.check_rounded, size: 13, color: Colors.white),
                    const SizedBox(width: 2),
                    Text('Prettier', style: st),
                  ]),
                ),
                    ]),
                  ),
                ),
              ),
            ]);
          },
        ),
      );

  /// Bottom panel: status line, then the editing tools and the symbols. Sits inside a SafeArea so it
  /// never slides under the phone's gesture bar or the keyboard.
  Widget _bottomPanel(_Doc d) => Column(mainAxisSize: MainAxisSize.min, children: [
        Container(
          decoration: const BoxDecoration(color: _vsSide, border: Border(top: BorderSide(color: _vsLine))),
          child: Column(mainAxisSize: MainAxisSize.min, children: [
            _toolRow(d),
            _symbolRow(),
            const SizedBox(height: 4),
          ]),
        ),
        Container(color: _vsBlue, padding: EdgeInsets.only(bottom: MediaQuery.of(context).padding.bottom), child: _status(d)),
      ]);

  // Both rows are fixed: every key is always on screen, nothing to scroll sideways to find.
  // Row 1 is editing (undo, indent, comment, paste, the "more" menu); row 2 is the symbols
  // that file type needs most, plus a key that opens all the others.
  Widget _toolRow(_Doc d) => SizedBox(
        height: 46,
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 6),
          child: Row(children: [
            Expanded(
              flex: 2,
              child: ValueListenableBuilder<UndoHistoryValue>(
                valueListenable: d.undo,
                builder: (_, v, __) => Row(children: [
                  _slot(Icons.undo_rounded, 'Undo', v.canUndo ? d.undo.undo : null),
                  _slot(Icons.redo_rounded, 'Redo', v.canRedo ? d.undo.redo : null),
                ]),
              ),
            ),
            _divider(),
            _slot(Icons.format_indent_increase_rounded, 'Indent (Tab)', () => _indent(true)),
            _slot(Icons.format_indent_decrease_rounded, 'Remove indent', () => _indent(false)),
            _slot(null, 'Comment or uncomment the line(s)', _comment, label: '//'),
            _slot(Icons.auto_fix_high_rounded, 'Format document (Prettier style)', _format),
            _divider(),
            _slot(Icons.content_paste_rounded, 'Paste', _paste),
            Expanded(
              child: PopupMenuButton<String>(
                color: C.sheet,
                tooltip: 'Select all, move, duplicate, delete line',
                padding: EdgeInsets.zero,
                position: PopupMenuPosition.over,
                onSelected: (v) {
                  switch (v) {
                    case 'all':
                      _selectAll();
                    case 'up':
                      _moveLine(true);
                    case 'down':
                      _moveLine(false);
                    case 'dup':
                      _dupLine();
                    case 'del':
                      _delLine();
                  }
                },
                itemBuilder: (_) => [
                  _mi('all', Icons.select_all_rounded, 'Select all'),
                  _mi('up', Icons.keyboard_arrow_up_rounded, 'Move line up'),
                  _mi('down', Icons.keyboard_arrow_down_rounded, 'Move line down'),
                  _mi('dup', Icons.copy_rounded, 'Duplicate line'),
                  _mi('del', Icons.delete_outline_rounded, 'Delete line'),
                ],
                child: _box(const Icon(Icons.more_horiz_rounded, size: 20, color: C.text)),
              ),
            ),
            _slot(Icons.keyboard_hide_rounded, 'Hide keyboard', () => focus.unfocus()),
          ]),
        ),
      );

  /// What each file type needs most, in the order it is typed: (what the key shows, what it inserts, steps back).
  List<(String, String, int)> _symbolsFor(String ext) {
    const pair = {'{ }': '{}', '( )': '()', '[ ]': '[]', '" "': '""'};
    List<(String, String, int)> mk(List<String> keys) => [
          for (final k in keys) pair.containsKey(k) ? (k, pair[k]!, 1) : (k, k, 0),
        ];
    return switch (ext) {
      'html' || 'htm' || 'svg' || 'xml' => mk(['<', '>', '/', '=', '" "', '!', '#']),
      'css' || 'scss' => mk(['{ }', ':', ';', '.', '#', '( )', '%']),
      'js' || 'mjs' || 'ts' => mk(['{ }', '( )', '[ ]', '" "', ';', '=', '>']),
      _ => mk(['{ }', '( )', '[ ]', '" "', ';', '=', '<']),
    };
  }

  Widget _symbolRow() {
    final list = _symbolsFor(doc?.ctl.ext ?? '');
    return SizedBox(
      height: 44,
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 6),
        child: Row(children: [
          for (final k in list) _slot(null, 'Insert ${k.$2}', () => _insert(k.$2, back: k.$3), label: k.$1),
          _divider(),
          _slot(Icons.apps_rounded, 'All symbols', _moreSymbols),
        ]),
      ),
    );
  }

  /// Every symbol, in groups, for the ones that aren't on the quick row.
  void _moreSymbols() {
    const groups = <(String, List<String>)>[
      ('Brackets', ['{ }', '( )', '[ ]', '< >', '" "', "' '", '` `']),
      ('Operators', ['=', '+', '-', '*', '/', '%', '<', '>', '!', '&', '|', '^', '~']),
      ('Marks', [';', ':', '.', ',', '?', "'", '"', '`', '\\', '_', '@', '#', '\$']),
    ];
    const pair = {'{ }': '{}', '( )': '()', '[ ]': '[]', '< >': '<>', '" "': '""', "' '": "''", '` `': '``'};
    showModalBottomSheet<void>(
      context: context,
      backgroundColor: C.sheet,
      showDragHandle: true,
      builder: (ctx) => SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.fromLTRB(16, 0, 16, 16),
          child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            for (final g in groups) ...[
              Padding(padding: const EdgeInsets.only(top: 6, bottom: 8), child: Text(g.$1, style: inter(12.5, w: FontWeight.w700, c: C.teal))),
              Wrap(spacing: 8, runSpacing: 8, children: [
                for (final k in g.$2)
                  InkResponse(
                    onTap: () {
                      Navigator.pop(ctx);
                      final ins = pair[k];
                      if (ins != null) {
                        _insert(ins, back: 1);
                      } else {
                        _insert(k);
                      }
                      focus.requestFocus();
                    },
                    child: Container(
                      width: k.length > 1 ? 64 : 48,
                      height: 44,
                      alignment: Alignment.center,
                      decoration: BoxDecoration(color: C.card, borderRadius: BorderRadius.circular(10)),
                      child: Text(k, style: const TextStyle(fontFamily: 'monospace', fontSize: 16, fontWeight: FontWeight.w700, color: C.text)),
                    ),
                  ),
              ]),
            ],
          ]),
        ),
      ),
    );
  }

  /// The look every key shares: a rounded tile that fills the space it is given.
  Widget _box(Widget child) => Container(
        height: 38,
        margin: const EdgeInsets.symmetric(horizontal: 2),
        alignment: Alignment.center,
        decoration: BoxDecoration(color: C.card, borderRadius: BorderRadius.circular(10)),
        child: child,
      );

  /// One key that takes an equal share of the row.
  Widget _slot(IconData? icon, String tip, VoidCallback? f, {String? label}) => Expanded(child: _tool(icon, tip, f, label: label));

  /// One tool button: a visible press ripple and a tooltip for its name.
  Widget _tool(IconData? icon, String tip, VoidCallback? f, {String? label}) => Tooltip(
        message: tip,
        waitDuration: const Duration(milliseconds: 600),
        child: InkResponse(
          onTap: f,
          radius: 24,
          child: _box(
            icon != null
                ? Icon(icon, size: 20, color: f == null ? C.muted : C.text)
                : Text(label ?? '', style: TextStyle(fontFamily: 'monospace', fontSize: 15, fontWeight: FontWeight.w700, color: f == null ? C.muted : C.text)),
          ),
        ),
      );

  /// Thin line between groups of tools.
  Widget _divider() => Container(width: 1, height: 22, margin: const EdgeInsets.symmetric(horizontal: 4, vertical: 12), color: C.line);

  Widget _drawer() {
    final rows = _rows();
    const tx = Color(0xFFCCCCCC);
    return Drawer(
      width: 310,
      backgroundColor: _vsSide,
      shape: const RoundedRectangleBorder(),
      child: SafeArea(
        child: Column(children: [
          SizedBox(
            height: 36,
            child: Row(children: [
              const SizedBox(width: 20),
              Expanded(child: Text('EXPLORER', style: inter(11, c: const Color(0xFFBBBBBB)).copyWith(letterSpacing: .6))),
              _barBtn(Icons.manage_search_rounded, 'Search in project', () {
                Navigator.pop(context);
                _projectSearch();
              }, c: tx),
              _barBtn(Icons.note_add_outlined, 'New file', _newFile, c: tx),
            ]),
          ),
          Container(
            height: 24,
            color: _vsTabOff,
            padding: const EdgeInsets.symmetric(horizontal: 6),
            child: Row(children: [
              const Icon(Icons.expand_more_rounded, size: 18, color: tx),
              const SizedBox(width: 2),
              Expanded(child: Text('${p.name}.zip'.toUpperCase(), maxLines: 1, overflow: TextOverflow.ellipsis, style: inter(11, w: FontWeight.w700, c: tx))),
            ]),
          ),
          Expanded(
            child: ListView.builder(
              physics: const BouncingScrollPhysics(),
              padding: const EdgeInsets.symmetric(vertical: 2),
              itemCount: rows.length,
              itemBuilder: (_, i) {
                final r = rows[i];
                final active = !r.folder && doc?.path == r.path;
                return InkWell(
                  onTap: () {
                    if (r.folder) {
                      setState(() => open.contains(r.path) ? open.remove(r.path) : open.add(r.path));
                    } else {
                      _openDoc(r.path);
                    }
                  },
                  onLongPress: () => _treeMenu(r),
                  child: Container(
                    height: 30,
                    color: active ? _vsSel : null,
                    padding: EdgeInsets.only(left: 6 + r.depth * 12.0, right: 8),
                    child: Row(children: [
                      if (r.folder)
                        Icon(open.contains(r.path) ? Icons.expand_more_rounded : Icons.chevron_right_rounded, size: 18, color: tx)
                      else
                        const SizedBox(width: 18),
                      const SizedBox(width: 2),
                      if (!r.folder) ...[Icon(_iconFor(r.path), size: 16, color: _colorFor(r.path)), const SizedBox(width: 6)],
                      Expanded(child: Text(r.label, maxLines: 1, overflow: TextOverflow.ellipsis, style: inter(13.5, c: active ? Colors.white : tx))),
                      if (!r.folder && p.modified.contains(r.path)) const Text('M', style: TextStyle(fontSize: 12, fontWeight: FontWeight.w600, color: Color(0xFFE2C08D))),
                    ]),
                  ),
                );
              },
            ),
          ),
        ]),
      ),
    );
  }

  Widget _empty() => Center(
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 32),
          child: Column(mainAxisSize: MainAxisSize.min, children: [
            const Icon(Icons.code_rounded, size: 48, color: C.muted),
            const SizedBox(height: 12),
            Text(p.isOpen ? 'Pick a file to edit' : 'Start a project or open one', textAlign: TextAlign.center, style: inter(18, w: FontWeight.w700)),
            const SizedBox(height: 6),
            Text(p.isOpen ? 'Open the file list, or search the whole project.' : 'Create a project, or open a zip with Kotlin, Java, Flutter or web code (HTML, CSS, JS). Change the files here, then tap Build.', textAlign: TextAlign.center, style: inter(14, c: C.muted, h: 1.4)),
            const SizedBox(height: 18),
            if (p.isOpen)
              FilledButton(
                onPressed: busy ? null : () => scaffold.currentState?.openDrawer(),
                style: FilledButton.styleFrom(backgroundColor: C.tealBtn, foregroundColor: C.onTeal, minimumSize: const Size(220, 46), shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(23))),
                child: busy ? const SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2.5, color: C.onTeal)) : Text('Open files', style: inter(15, w: FontWeight.w700, c: C.onTeal)),
              )
            else ...[
              FilledButton.icon(
                onPressed: busy ? null : _createProject,
                style: FilledButton.styleFrom(backgroundColor: C.tealBtn, foregroundColor: C.onTeal, minimumSize: const Size(260, 46), shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(23))),
                icon: const Icon(Icons.add_rounded, size: 20),
                label: Text('Create project', style: inter(15, w: FontWeight.w700, c: C.onTeal)),
              ),
              const SizedBox(height: 10),
              OutlinedButton.icon(
                onPressed: busy ? null : _openZip,
                style: OutlinedButton.styleFrom(minimumSize: const Size(260, 46), side: const BorderSide(color: C.line), shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(23))),
                icon: const Icon(Icons.folder_open_rounded, size: 20),
                label: Text('Open existing project (zip)', style: inter(15, w: FontWeight.w600)),
              ),
            ],
          ]),
        ),
      );

  @override
  Widget build(BuildContext context) {
    final d = doc;
    return Scaffold(
      key: scaffold,
      backgroundColor: _vsEditor,
      resizeToAvoidBottomInset: !widget.embedded,
      drawer: p.isOpen ? _drawer() : null,
      body: SafeArea(
        child: Column(children: [
          _top(),
          if (docs.isNotEmpty) _tabs() else Container(height: 1, color: _vsLine),
          if (findOn && d != null) _findBar(),
          Expanded(
            child: d == null
                ? (ready ? _empty() : const SizedBox.shrink())
                : (!ready
                    ? const Center(child: SizedBox(width: 22, height: 22, child: CircularProgressIndicator(strokeWidth: 2.5, color: C.teal)))
                    : Stack(children: [
                        Positioned.fill(child: _editor(d)),
                        Positioned.fill(child: _sugLayer(d)),
                      ])),
          ),
          if (d != null) _bottomPanel(d),
        ]),
      ),
    );
  }
}

// ---------------------------------------------------------------- search in project

class _SearchPage extends StatefulWidget {
  const _SearchPage();
  @override
  State<_SearchPage> createState() => _SearchPageState();
}

class _SearchPageState extends State<_SearchPage> {
  final q = TextEditingController(), r = TextEditingController();
  final hits = <(String, int, String)>[];
  bool cs = false, rep = false;
  int fileCount = 0;
  Timer? t;

  @override
  void dispose() {
    t?.cancel();
    q.dispose();
    r.dispose();
    super.dispose();
  }

  void _soon() {
    t?.cancel();
    t = Timer(const Duration(milliseconds: 250), _search);
  }

  void _search() {
    hits.clear();
    fileCount = 0;
    final needle = cs ? q.text : q.text.toLowerCase();
    if (needle.isNotEmpty) {
      final files = CodeProject.i.files;
      for (final k in files.keys.toList()..sort(_cmp)) {
        final b = files[k]!;
        if (!_isText(k, b) || b.length > _maxEditBytes) continue;
        final text = utf8.decode(b, allowMalformed: true);
        if (!(cs ? text : text.toLowerCase()).contains(needle)) continue;
        fileCount++;
        final lines = text.split('\n');
        for (var i = 0; i < lines.length && hits.length < 300; i++) {
          if ((cs ? lines[i] : lines[i].toLowerCase()).contains(needle)) hits.add((k, i + 1, lines[i].trim()));
        }
        if (hits.length >= 300) break;
      }
    }
    if (mounted) setState(() {});
  }

  Future<void> _replaceAll() async {
    if (q.text.isEmpty || fileCount == 0) return;
    final ok = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: C.sheet,
        title: Text('Replace in $fileCount files?', style: inter(17, w: FontWeight.w700)),
        content: Text('Every match of "${q.text}" becomes "${r.text}".', style: inter(14, c: C.muted)),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx, false), child: Text('Cancel', style: inter(15, c: C.muted))),
          TextButton(onPressed: () => Navigator.pop(ctx, true), child: Text('Replace', style: inter(15, w: FontWeight.w700, c: C.teal))),
        ],
      ),
    );
    if (ok != true || !mounted) return;
    final re = RegExp(RegExp.escape(q.text), caseSensitive: cs);
    var n = 0;
    final files = CodeProject.i.files;
    for (final k in files.keys.toList()) {
      final b = files[k]!;
      if (!_isText(k, b) || b.length > _maxEditBytes) continue;
      final text = utf8.decode(b, allowMalformed: true);
      final c = re.allMatches(text).length;
      if (c == 0) continue;
      n += c;
      files[k] = utf8.encode(text.replaceAll(re, r.text));
      CodeProject.i.modified.add(k);
    }
    IosBanner.show(context, kind: BannerKind.ok, title: 'Replaced $n', msg: q.text);
    _search();
  }

  @override
  Widget build(BuildContext context) {
    InputDecoration dec(String h) => InputDecoration(isDense: true, hintText: h, hintStyle: inter(14, c: C.muted), contentPadding: const EdgeInsets.symmetric(horizontal: 10, vertical: 10), filled: true, fillColor: C.card, border: OutlineInputBorder(borderRadius: BorderRadius.circular(8), borderSide: BorderSide.none));
    return Scaffold(
      backgroundColor: C.bg,
      body: SafeArea(
        child: Column(children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(4, 6, 8, 4),
            child: Row(children: [
              SizedBox(width: 40, height: 40, child: IconButton(padding: EdgeInsets.zero, onPressed: () => Navigator.of(context).maybePop(), icon: const Icon(Icons.arrow_back_ios_new_rounded, size: 20, color: C.text), tooltip: 'Back')),
              Expanded(child: SizedBox(height: 38, child: TextField(controller: q, autofocus: true, onChanged: (_) => _soon(), style: inter(14.5), cursorColor: C.teal, decoration: dec('Search in all files')))),
              const SizedBox(width: 6),
              GestureDetector(
                behavior: HitTestBehavior.opaque,
                onTap: () {
                  setState(() => cs = !cs);
                  _search();
                },
                child: Container(width: 36, height: 34, alignment: Alignment.center, decoration: BoxDecoration(color: cs ? C.tealDeep : null, borderRadius: BorderRadius.circular(6)), child: Text('Aa', style: inter(13, w: FontWeight.w700, c: cs ? C.teal : C.muted))),
              ),
              GestureDetector(
                behavior: HitTestBehavior.opaque,
                onTap: () => setState(() => rep = !rep),
                child: Container(width: 36, height: 34, alignment: Alignment.center, decoration: BoxDecoration(color: rep ? C.tealDeep : null, borderRadius: BorderRadius.circular(6)), child: Icon(Icons.find_replace_rounded, size: 19, color: rep ? C.teal : C.muted)),
              ),
            ]),
          ),
          if (rep)
            Padding(
              padding: const EdgeInsets.fromLTRB(48, 0, 8, 6),
              child: Row(children: [
                Expanded(child: SizedBox(height: 38, child: TextField(controller: r, style: inter(14.5), cursorColor: C.teal, decoration: dec('Replace with')))),
                TextButton(onPressed: fileCount == 0 ? null : _replaceAll, child: Text('Replace all', style: inter(14, w: FontWeight.w700, c: fileCount == 0 ? C.muted : C.teal))),
              ]),
            ),
          if (q.text.isNotEmpty)
            Padding(
              padding: const EdgeInsets.fromLTRB(14, 2, 14, 6),
              child: Align(alignment: Alignment.centerLeft, child: Text(hits.isEmpty ? 'No matches' : '${hits.length >= 300 ? '300+' : hits.length} matches in $fileCount files', style: inter(12, c: C.muted))),
            ),
          Container(height: 1, color: C.line),
          Expanded(
            child: ListView.builder(
              physics: const BouncingScrollPhysics(),
              itemCount: hits.length,
              itemBuilder: (_, i) {
                final h = hits[i];
                return InkWell(
                  onTap: () => Navigator.of(context).pop((h.$1, h.$2)),
                  child: Padding(
                    padding: const EdgeInsets.fromLTRB(14, 7, 14, 7),
                    child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                      Text('${h.$1}:${h.$2}', maxLines: 1, overflow: TextOverflow.ellipsis, style: inter(11, c: C.muted)),
                      const SizedBox(height: 2),
                      Text(h.$3, maxLines: 2, overflow: TextOverflow.ellipsis, style: const TextStyle(fontFamily: 'monospace', fontSize: 12.5, color: C.text)),
                    ]),
                  ),
                );
              },
            ),
          ),
        ]),
      ),
    );
  }
}
