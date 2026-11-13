// The editor's suggestion engine: works out what can be typed at the cursor and ranks it.
// Pure Dart (no Flutter). The editor shows the list and calls [applySuggestion] when one is tapped.
//
// What it understands:
//   Kotlin / Java  classes (with the import added for you), Android members by the variable's type,
//                  R.id / R.layout / R.string ... taken from your res folder, your own classes and functions
//   Dart           Flutter widgets (+ import), named parameters of the widget you are inside, Colors / Icons /
//                  EdgeInsets ..., members by type, Theme.of(context)., your own classes
//   JS / TS        DOM, document / window / console, members by type (Element, String, Array ...), your functions
//   HTML           tags, attributes per tag, values (class names from your CSS, file paths), closing tags,
//                  and JavaScript / CSS inside <script> / <style>
//   CSS            properties, values per property, colours, var(--x), class / id names from your HTML, pseudo, @rules
//   Android XML    views, attributes, values, @string / @color / @drawable / @id lists from your res folder,
//                  activities and permissions in the manifest

import 'dart:convert';
import 'code_assist_data.dart';
import 'code_format.dart' show skipCodeString;

enum SK { keyword, cls, method, field, variable, snippet, tag, attr, value, file, resource, module }

class Sug {
  final String label; // what is shown
  final String insert; // what goes into the file ("§" marks the cursor)
  final String detail;
  final SK kind;
  final String? imp; // import to add (Kotlin/Java: the full class name, Dart: the whole import line)
  final String? ns; // xmlns line to add on the root tag of an Android XML file
  double score = 0;
  Sug(this.label, this.insert, this.kind, {this.detail = '', this.imp, this.ns});
}

class Completion {
  final int start; // everything from here to the cursor is replaced
  final List<Sug> items;
  Completion(this.start, this.items);
}

class Applied {
  final String text;
  final int caret;
  Applied(this.text, this.caret);
}

// ------------------------------------------------------------------ small helpers

final _splitCache = <String, List<String>>{};
List<String> _w(String s) => _splitCache.putIfAbsent(s, () => s.split(RegExp(r'\s+')).where((e) => e.isNotEmpty).toList());

String _extOf(String path) {
  final n = path.split('/').last;
  final i = n.lastIndexOf('.');
  return i < 0 ? '' : n.substring(i + 1).toLowerCase();
}

bool _isWordCh(int c) => (c >= 48 && c <= 57) || (c >= 65 && c <= 90) || (c >= 97 && c <= 122) || c == 95;
bool _isIdStart(int c) => (c >= 65 && c <= 90) || (c >= 97 && c <= 122) || c == 95;

String _initials(String l) {
  final b = StringBuffer();
  for (var i = 0; i < l.length; i++) {
    final c = l.codeUnitAt(i);
    if (i == 0) {
      b.write(l[i]);
    } else {
      final p = l.codeUnitAt(i - 1);
      final isUpper = c >= 65 && c <= 90;
      final prevLower = (p >= 97 && p <= 122) || (p >= 48 && p <= 57);
      if ((isUpper && prevLower) || p == 95 || p == 45 || p == 58) b.write(l[i]);
    }
  }
  return b.toString();
}

/// 0 = no match. Higher is better. Prefix beats camel-case initials ("aca" finds AppCompatActivity) beats substring.
double _score(String prefix, String label) {
  if (prefix.isEmpty) return 40;
  if (label.startsWith(prefix)) return 100 - ((label.length - prefix.length) * 0.15).clamp(0, 15).toDouble();
  final pl = prefix.toLowerCase(), ll = label.toLowerCase();
  if (ll.startsWith(pl)) return 88 - ((label.length - prefix.length) * 0.15).clamp(0, 15).toDouble();
  if (prefix.length >= 2 && _initials(label).toLowerCase().startsWith(pl)) return 70;
  final idx = ll.indexOf(pl);
  if (idx > 0 && prefix.length >= 2) return 50 - (idx * 0.3).clamp(0, 10).toDouble();
  if (prefix.length >= 3) {
    var j = 0;
    for (var i = 0; i < ll.length && j < pl.length; i++) {
      if (ll[i] == pl[j]) j++;
    }
    if (j == pl.length) return 25;
  }
  return 0;
}

class _Col {
  final String prefix;
  final best = <String, Sug>{};
  _Col(this.prefix);

  void add(String label, String insert, SK kind, {String detail = '', String? imp, String? ns, double bonus = 0, String? matchOn}) {
    final m = _score(prefix, matchOn ?? label);
    if (m <= 0) return;
    final s = m + bonus;
    final old = best[label];
    if (old != null && old.score >= s) return;
    best[label] = Sug(label, insert, kind, detail: detail, imp: imp, ns: ns)..score = s;
  }

  List<Sug> top(int n) {
    final l = best.values.where((s) => s.insert.replaceAll('§', '') != prefix || prefix.isEmpty).toList();
    l.sort((a, b) {
      final r = b.score.compareTo(a.score);
      if (r != 0) return r;
      final q = a.label.length.compareTo(b.label.length);
      return q != 0 ? q : a.label.compareTo(b.label);
    });
    return l.length > n ? l.sublist(0, n) : l;
  }
}

/// Turns a table word (name( / name() / name{ / name< / name) into a suggestion.
void _addTok(_Col c, String tok, String detail, {double bonus = 0, String? imp}) {
  String name = tok, label = tok, ins = tok;
  var k = SK.field;
  if (tok.endsWith('()')) {
    name = tok.substring(0, tok.length - 2);
    k = SK.method;
  } else if (tok.endsWith('(')) {
    name = tok.substring(0, tok.length - 1);
    label = '$name()';
    ins = '$name(§)';
    k = SK.method;
  } else if (tok.endsWith('{')) {
    name = tok.substring(0, tok.length - 1);
    label = '$name { }';
    ins = '$name { § }';
    k = SK.method;
  } else if (tok.endsWith('<')) {
    name = tok.substring(0, tok.length - 1);
    label = '$name<>';
    ins = '$name<§>()';
    k = SK.method;
  }
  c.add(label, ins, k, detail: detail, bonus: bonus, matchOn: name, imp: imp);
}

// ------------------------------------------------------------------ project index

class _Sym {
  final String name, detail;
  final SK kind;
  _Sym(this.name, this.kind, this.detail);
}

const _textExts = {'kt', 'kts', 'java', 'dart', 'js', 'mjs', 'ts', 'html', 'htm', 'css', 'scss', 'xml', 'gradle', 'yaml', 'yml'};

class _FI {
  final String path;
  final List<int> bytes;
  _FI(this.path, this.bytes);
  String ext = '';
  bool _tried = false;
  String? _text;
  List<_Sym>? _syms;
  String? _pkg;
  Set<String>? _cssNames;
  String get lang => _langOf(ext);

  String? get text {
    if (_tried) return _text;
    _tried = true;
    ext = _extOf(path);
    if (_textExts.contains(ext) && bytes.length <= 300000) {
      try {
        _text = utf8.decode(bytes, allowMalformed: true);
      } catch (_) {
        _text = null;
      }
    }
    return _text;
  }

  List<_Sym> get syms => _syms ??= (text == null ? const [] : _symbols(text!, lang));

  String get pkg {
    if (_pkg != null) return _pkg!;
    final t = text;
    if (t == null) return _pkg = '';
    return _pkg = RegExp(r'^\s*package\s+([\w.]+)', multiLine: true).firstMatch(t)?.group(1) ?? '';
  }
}

String _langOf(String ext) {
  switch (ext) {
    case 'kt':
    case 'kts':
      return 'kt';
    case 'java':
      return 'java';
    case 'dart':
      return 'dart';
    case 'js':
    case 'mjs':
    case 'ts':
      return 'js';
    case 'gradle':
      return 'gradle';
  }
  return ext;
}

final _idx = <String, _FI>{};

void _sync(Map<String, List<int>> files) {
  _idx.removeWhere((k, _) => !files.containsKey(k));
  files.forEach((k, b) {
    final f = _idx[k];
    if (f == null || !identical(f.bytes, b)) {
      final n = _FI(k, b);
      n.ext = _extOf(k);
      _idx[k] = n;
    }
  });
}

// ------------------------------------------------------------------ reading symbols out of source

final _rKtClass = RegExp(r'\b(class|object|interface|typealias)\s+(\w+)');
final _rKtFun = RegExp(r'\bfun\s+(?:<[^>]*>\s*)?(?:[\w.]+\.)?(\w+)\s*\(([^)]*)\)(?:\s*:\s*([\w.<>?]+))?');
final _rKtVal = RegExp(r'\b(val|var)\s+(\w+)');
final _rTypedName = RegExp(r'\b(\w+)\s*:\s*([A-Z][\w.]*(?:<[^>]*>)?\??)');
final _rJavaClass = RegExp(r'\b(class|interface|enum|record)\s+(\w+)');
final _rJavaMethod = RegExp(r'\b([A-Za-z_][\w<>\[\].,?]*)\s+(\w+)\s*\(([^()]*)\)\s*(?:throws[\w\s.,]+)?\{');
final _rJavaVar = RegExp(r'\b([A-Z][\w.]*(?:<[^>;()]*>)?(?:\[\])?|int|long|float|double|boolean|char|byte|short|var)\s+(\w+)\s*(?:=|;|,|\))');
final _rDartClass = RegExp(r'\b(class|mixin|enum|extension|typedef)\s+(\w+)');
final _rDartFun = RegExp(r'^\s*(?:static\s+)?(?:Future(?:<[^>]*>)?|Stream(?:<[^>]*>)?|void|bool|int|double|num|String|dynamic|[A-Z]\w*(?:<[^>]*>)?\??)\s+(\w+)\s*\(', multiLine: true);
final _rDartVar = RegExp(r'\b(?:final|var|late|const)\s+(?:[A-Za-z_][\w<>?, ]*\s+)?(\w+)\s*(?:=|;)');
final _rDartTyped = RegExp(r'\b([A-Z]\w*(?:<[^>;()]*>)?\??|int|double|bool|String|num)\s+(\w+)\s*(?:=|;|,|\))');
final _rJsFun = RegExp(r'\bfunction\s*\*?\s+(\w+)\s*\(([^)]*)\)');
final _rJsClass = RegExp(r'\bclass\s+(\w+)');
final _rJsVar = RegExp(r'\b(?:const|let|var)\s+(\w+)');
final _rJsDestr = RegExp(r'\b(?:const|let|var)\s*[{\[]([^}\]=]+)[}\]]');
final _rJsMethod = RegExp(r'^\s+(?:async\s+|static\s+|get\s+|set\s+)*(\w+)\s*\(([^)]*)\)\s*\{', multiLine: true);
final _rJsArrow = RegExp(r'\(([^()]*)\)\s*=>');
const _notNames = {'if', 'for', 'while', 'switch', 'catch', 'synchronized', 'function', 'return', 'new', 'else', 'try', 'do', 'when', 'case', 'super', 'this', 'throw', 'in', 'is', 'as', 'it', 'val', 'var', 'final'};

List<_Sym> _symbols(String t, String lang) {
  final out = <_Sym>[];
  final seen = <String>{};
  void add(String n, SK k, String d) {
    if (n.length < 2 || _notNames.contains(n) || !seen.add('${k.index}$n')) return;
    out.add(_Sym(n, k, d));
  }

  String shortSig(String? params, [String? ret]) {
    var p = (params ?? '').replaceAll(RegExp(r'\s+'), ' ').trim();
    if (p.length > 34) p = '${p.substring(0, 33)}\u2026';
    return '($p)${ret == null || ret.isEmpty ? '' : ': $ret'}';
  }

  switch (lang) {
    case 'kt':
      for (final m in _rKtClass.allMatches(t)) {
        add(m.group(2)!, SK.cls, m.group(1)!);
      }
      for (final m in _rKtFun.allMatches(t)) {
        add(m.group(1)!, SK.method, shortSig(m.group(2), m.group(3)));
      }
      for (final m in _rKtVal.allMatches(t)) {
        add(m.group(2)!, SK.variable, m.group(1)!);
      }
      for (final m in _rTypedName.allMatches(t)) {
        add(m.group(1)!, SK.variable, m.group(2)!);
      }
      break;
    case 'java':
      for (final m in _rJavaClass.allMatches(t)) {
        add(m.group(2)!, SK.cls, m.group(1)!);
      }
      for (final m in _rJavaMethod.allMatches(t)) {
        const badType = {'new', 'return', 'else', 'throw', 'case', 'if', 'while', 'for'};
        if (badType.contains(m.group(1))) continue;
        add(m.group(2)!, SK.method, shortSig(m.group(3), m.group(1)));
      }
      for (final m in _rJavaVar.allMatches(t)) {
        add(m.group(2)!, SK.variable, m.group(1)!);
      }
      break;
    case 'dart':
      for (final m in _rDartClass.allMatches(t)) {
        add(m.group(2)!, SK.cls, m.group(1)!);
      }
      for (final m in _rDartFun.allMatches(t)) {
        add(m.group(1)!, SK.method, 'function');
      }
      for (final m in _rDartVar.allMatches(t)) {
        add(m.group(1)!, SK.variable, 'variable');
      }
      for (final m in _rDartTyped.allMatches(t)) {
        add(m.group(2)!, SK.variable, m.group(1)!);
      }
      for (final m in RegExp(r'\bthis\.(\w+)').allMatches(t)) {
        add(m.group(1)!, SK.field, 'field');
      }
      break;
    case 'js':
      for (final m in _rJsFun.allMatches(t)) {
        add(m.group(1)!, SK.method, shortSig(m.group(2)));
        for (final a in (m.group(2) ?? '').split(',')) {
          final n = a.split('=').first.replaceAll(RegExp(r'[:?].*$'), '').trim();
          if (RegExp(r'^[A-Za-z_$][\w$]*$').hasMatch(n)) add(n, SK.variable, 'parameter');
        }
      }
      for (final m in _rJsClass.allMatches(t)) {
        add(m.group(1)!, SK.cls, 'class');
      }
      for (final m in _rJsVar.allMatches(t)) {
        add(m.group(1)!, SK.variable, 'variable');
      }
      for (final m in _rJsDestr.allMatches(t)) {
        for (final a in m.group(1)!.split(',')) {
          final parts = a.split(':');
          final n = parts.last.split('=').first.trim();
          if (RegExp(r'^[A-Za-z_$][\w$]*$').hasMatch(n)) add(n, SK.variable, 'variable');
        }
      }
      for (final m in _rJsMethod.allMatches(t)) {
        add(m.group(1)!, SK.method, shortSig(m.group(2)));
      }
      for (final m in _rJsArrow.allMatches(t)) {
        for (final a in m.group(1)!.split(',')) {
          final n = a.split('=').first.trim();
          if (RegExp(r'^[A-Za-z_$][\w$]*$').hasMatch(n)) add(n, SK.variable, 'parameter');
        }
      }
      break;
  }
  return out;
}

String _baseType(String t) {
  var s = t.trim();
  final lt = s.indexOf('<');
  if (lt > 0) s = s.substring(0, lt);
  s = s.replaceAll('?', '').replaceAll('!', '').trim();
  return s;
}

/// variable name -> type, read from declarations in [t].
Map<String, String> _typesOf(String t, String lang) {
  final m = <String, String>{};
  if (t.length > 300000) return m;
  void put(String n, String ty) {
    final b = _baseType(ty);
    if (b.isNotEmpty && !m.containsKey(n)) m[n] = ty.trim();
  }

  switch (lang) {
    case 'kt':
      for (final x in _rTypedName.allMatches(t)) {
        put(x.group(1)!, x.group(2)!);
      }
      for (final x in RegExp(r'\b(?:val|var)\s+(\w+)\s*=\s*([^\n;]{0,60})').allMatches(t)) {
        final rhs = x.group(2)!.trimLeft();
        final fv = RegExp(r'^(?:requireViewById|findViewById)\s*<\s*([\w.]+)').firstMatch(rhs);
        if (fv != null) {
          put(x.group(1)!, fv.group(1)!);
        } else if (RegExp(r'^[A-Z]\w*\s*[(.<]').hasMatch(rhs) && !RegExp(r'^[A-Z][A-Z_0-9]+\b').hasMatch(rhs)) {
          put(x.group(1)!, RegExp(r'^[A-Z]\w*').firstMatch(rhs)!.group(0)!);
        } else if (rhs.startsWith('"')) {
          put(x.group(1)!, 'String');
        } else if (RegExp(r'^(?:mutableListOf|listOf|arrayListOf|emptyList|mutableStateListOf)\b').hasMatch(rhs)) {
          put(x.group(1)!, 'List');
        } else if (RegExp(r'^(?:mutableMapOf|mapOf|hashMapOf|emptyMap|linkedMapOf)\b').hasMatch(rhs)) {
          put(x.group(1)!, 'Map');
        } else if (RegExp(r'^\d+$|^\d+[.\d]*[fFL]?\b').hasMatch(rhs)) {
          put(x.group(1)!, rhs.contains('.') ? 'Double' : 'Int');
        }
      }
      break;
    case 'java':
      for (final x in _rJavaVar.allMatches(t)) {
        if (x.group(1) != 'var') put(x.group(2)!, x.group(1)!);
      }
      for (final x in RegExp(r'\bvar\s+(\w+)\s*=\s*new\s+(\w+)').allMatches(t)) {
        put(x.group(1)!, x.group(2)!);
      }
      for (final x in RegExp(r'\b(\w+)\s*=\s*\(?\s*\(([A-Z]\w*)\)\s*findViewById').allMatches(t)) {
        put(x.group(1)!, x.group(2)!);
      }
      for (final x in RegExp(r'\b(\w+)\s*=\s*findViewById\s*\(').allMatches(t)) {
        put(x.group(1)!, '?');
      }
      m.removeWhere((k, v) => v == '?');
      break;
    case 'dart':
      for (final x in _rDartTyped.allMatches(t)) {
        put(x.group(2)!, x.group(1)!);
      }
      for (final x in RegExp(r'\b(?:final|var|late|const)\s+(\w+)\s*=\s*([^\n;]{0,60})').allMatches(t)) {
        final rhs = x.group(2)!.trimLeft().replaceFirst(RegExp(r'^(?:const\s+|await\s+)'), '');
        final ctor = RegExp(r'^([A-Z]\w*)\s*[(.<]').firstMatch(rhs);
        if (ctor != null) {
          put(x.group(1)!, ctor.group(1)!);
        } else if (rhs.startsWith("'") || rhs.startsWith('"')) {
          put(x.group(1)!, 'String');
        } else if (rhs.startsWith('[') || RegExp(r'^<\w+>\[').hasMatch(rhs)) {
          put(x.group(1)!, 'List');
        } else if (rhs.startsWith('{') || RegExp(r'^<[\w, ]+>\{').hasMatch(rhs)) {
          put(x.group(1)!, 'Map');
        } else if (RegExp(r'^\d+\.\d').hasMatch(rhs)) {
          put(x.group(1)!, 'double');
        } else if (RegExp(r'^\d+\b').hasMatch(rhs)) {
          put(x.group(1)!, 'int');
        }
      }
      break;
    case 'js':
      for (final x in RegExp(r'\b(?:const|let|var)\s+(\w+)\s*=\s*([^\n;]{0,70})').allMatches(t)) {
        var rhs = x.group(2)!.trimLeft();
        var awaited = false;
        if (rhs.startsWith('await ')) {
          awaited = true;
          rhs = rhs.substring(6).trimLeft();
        }
        final n = x.group(1)!;
        String? ty;
        if (RegExp(r'getContext\s*\(').hasMatch(rhs)) {
          ty = 'Ctx2D';
        } else if (RegExp(r'^(?:document|[\w.]+)\.(?:getElementById|querySelector|createElement|closest)\s*\(').hasMatch(rhs) || RegExp(r'\.(?:parentElement|firstElementChild|lastElementChild|nextElementSibling|previousElementSibling)\b').hasMatch(rhs)) {
          ty = 'Element';
        } else if (RegExp(r'\.querySelectorAll\s*\(|\.getElementsBy\w+\s*\(|^\[|^Array\.|\.split\s*\(|\.map\s*\(|\.filter\s*\(').hasMatch(rhs)) {
          ty = 'Array';
        } else if (rhs.startsWith("'") || rhs.startsWith('"') || rhs.startsWith('`')) {
          ty = 'String';
        } else if (RegExp(r'^fetch\s*\(').hasMatch(rhs)) {
          ty = awaited ? 'Response' : 'Promise';
        } else if (rhs.startsWith('{')) {
          ty = 'Object';
        } else if (RegExp(r'^\d').hasMatch(rhs)) {
          ty = 'Number';
        } else if (rhs.startsWith('localStorage') || rhs.startsWith('sessionStorage')) {
          ty = 'Storage';
        } else {
          final nw = RegExp(r'^new\s+(\w+)').firstMatch(rhs);
          if (nw != null && kJsMembers.containsKey(nw.group(1))) ty = nw.group(1);
        }
        if (ty != null) put(n, ty);
      }
      break;
  }
  return m;
}

// ------------------------------------------------------------------ the entry point

String? _curText;
List<_Sym> _curSyms = const [];
Map<String, String> _curTypes = const {};
Map<String, int> _curWords = const {};
final _reWords = RegExp(r'[A-Za-z_$][\w$]{2,}');

class _Cx {
  final String text, path, ext, lang;
  final int off;
  final Map<String, List<int>> files;
  _Cx(this.text, this.off, this.path, this.files)
      : ext = _extOf(path),
        lang = _langOf(_extOf(path));
  List<_Sym> get syms {
    _own();
    return _curSyms;
  }

  Map<String, String> get types {
    _own();
    return _curTypes;
  }

  Map<String, int> get words {
    _own();
    return _curWords;
  }

  void _own() {
    if (identical(_curText, text)) return;
    _curText = text;
    _curSyms = _symbols(text, lang);
    _curTypes = _typesOf(text, lang);
    final w = <String, int>{};
    if (text.length <= 120000) {
      for (final m in _reWords.allMatches(text)) {
        final s = m.group(0)!;
        w[s] = (w[s] ?? 0) + 1;
      }
    }
    _curWords = w;
  }

  String get dir {
    final i = path.lastIndexOf('/');
    return i < 0 ? '' : path.substring(0, i);
  }
}

/// What can be typed at [offset]. Returns null when nothing should pop up.
Completion? completeAt({required String text, required int offset, required String path, required Map<String, List<int>> files}) {
  if (offset < 0 || offset > text.length) return null;
  _sync(files);
  final c = _Cx(text, offset, path, files);
  switch (c.ext) {
    case 'html':
    case 'htm':
      return _html(c);
    case 'css':
    case 'scss':
      return _css(c, text, offset, 0);
    case 'xml':
      return _xml(c);
    case 'kt':
    case 'kts':
    case 'java':
    case 'dart':
    case 'js':
    case 'mjs':
    case 'ts':
    case 'gradle':
      return _code(c, text, offset, 0);
  }
  return null;
}

// ------------------------------------------------------------------ comment / string state

/// (0 code | 1 comment | 2 string, where it starts)
(int, int) _state(String t, int off, String ext) {
  final js = ext == 'js' || ext == 'mjs' || ext == 'ts';
  var i = off > 200000 ? off - 20000 : 0;
  final n = off < t.length ? off : t.length;
  while (i < n) {
    final c = t[i];
    if (c == '/' && i + 1 < t.length) {
      if (t[i + 1] == '/') {
        final e = t.indexOf('\n', i);
        if (e < 0 || e >= n) return (1, i);
        i = e;
        continue;
      }
      if (t[i + 1] == '*') {
        final e = t.indexOf('*/', i + 2);
        if (e < 0 || e + 2 > n) return (1, i);
        i = e + 2;
        continue;
      }
    }
    if (c == '"' || c == "'" || (c == '`' && js)) {
      final e = skipCodeString(t, i, ext);
      if (e > n || (e == n && !(e - 1 > i && t[e - 1] == c))) return (2, i);
      i = e;
      continue;
    }
    i++;
  }
  return (0, 0);
}

// ------------------------------------------------------------------ Kotlin / Java / Dart / JS / Gradle

class _Chain {
  final List<String> segs; // "name" or "name()" for calls ("name<T>()" keeps the generic)
  final bool unknown; // the receiver is some other expression, e.g. (a + b).
  _Chain(this.segs, this.unknown);
}

int _skipBack(String t, int close, String open, String closeCh) {
  var d = 0;
  for (var i = close; i >= 0; i--) {
    final c = t[i];
    if (c == closeCh) {
      d++;
    } else if (c == open) {
      d--;
      if (d == 0) return i;
    }
  }
  return -1;
}

_Chain _chainBefore(String t, int dotPos) {
  final segs = <String>[];
  var pos = dotPos;
  var unknown = false;
  for (var guard = 0; guard < 8; guard++) {
    while (pos > 0 && (t[pos - 1] == '?' || t[pos - 1] == '!')) {
      pos--;
    }
    var call = false;
    var generic = '';
    if (pos > 0 && (t[pos - 1] == ')' || t[pos - 1] == ']')) {
      final isCall = t[pos - 1] == ')';
      final o = _skipBack(t, pos - 1, isCall ? '(' : '[', isCall ? ')' : ']');
      if (o < 0) {
        unknown = true;
        break;
      }
      pos = o;
      call = isCall;
      if (!isCall) {
        segs.insert(0, '[]');
        unknown = true;
        break;
      }
      if (pos > 0 && t[pos - 1] == '>') {
        final g = _skipBack(t, pos - 1, '<', '>');
        if (g > 0) {
          generic = t.substring(g, pos);
          pos = g;
        }
      }
    }
    final end = pos;
    while (pos > 0 && (_isWordCh(t.codeUnitAt(pos - 1)) || t[pos - 1] == r'$')) {
      pos--;
    }
    final name = t.substring(pos, end);
    if (name.isEmpty) {
      unknown = true;
      break;
    }
    segs.insert(0, call ? '$name$generic()' : name);
    if (pos > 0 && t[pos - 1] == '.') {
      pos--;
      continue;
    }
    break;
  }
  return _Chain(segs, unknown);
}

String _segName(String s) {
  var n = s;
  if (n.endsWith('()')) n = n.substring(0, n.length - 2);
  final lt = n.indexOf('<');
  return lt > 0 ? n.substring(0, lt) : n;
}

const _chainTypes = <String, String>{
  'Context.getSharedPreferences': 'SharedPreferences',
  'SharedPreferences.edit': 'SharedPreferences.Editor',
  'Context.getIntent': 'Intent',
  'Context.intent': 'Intent',
  'Intent.getExtras': 'Bundle',
  'Intent.extras': 'Bundle',
  'Intent.getData': 'Uri',
  'Intent.data': 'Uri',
  'Context.getWindow': 'Window',
  'Context.window': 'Window',
  'WebView.getSettings': 'WebSettings',
  'WebView.settings': 'WebSettings',
  'URL.openConnection': 'HttpURLConnection',
  'Context.getPackageManager': 'PackageManager.Instance',
  'Context.packageManager': 'PackageManager.Instance',
  'Context.getMenuInflater': 'MenuInflater',
  'Context.getLayoutInflater': 'LayoutInflater',
  'Context.layoutInflater': 'LayoutInflater',
  'Fragment.requireContext': 'Context',
  'Fragment.requireActivity': 'Context',
  'Fragment.getActivity': 'Context',
  'Fragment.activity': 'Context',
  'View.getContext': 'Context',
  'View.context': 'Context',
  'Context.getApplicationContext': 'Context',
  'Context.applicationContext': 'Context',
  'Context.getFilesDir': 'File',
  'Context.filesDir': 'File',
  'Context.getCacheDir': 'File',
  'Context.cacheDir': 'File',
  'File.getParentFile': 'File',
  'File.parentFile': 'File',
  'Bundle.getString': 'String',
  'Intent.getStringExtra': 'String',
  'SharedPreferences.getString': 'String',
  'TextView.getText': 'String',
  'TextView.text': 'String',
  'EditText.getText': 'String',
  'EditText.text': 'String',
  'Any.toString': 'String',
  'JSONObject.getJSONObject': 'JSONObject',
  'JSONObject.optJSONObject': 'JSONObject',
  'JSONObject.getJSONArray': 'JSONArray',
  'JSONObject.optJSONArray': 'JSONArray',
  'JSONArray.getJSONObject': 'JSONObject',
  'JSONArray.optJSONObject': 'JSONObject',
  'Window.getDecorView': 'View',
  'Window.decorView': 'View',
  'Context.getResources': 'Any',
  'AlertDialog.Builder.create': 'Dialog',
  'Dialog.getWindow': 'Window',
  'document.getElementById': 'Element',
  'document.querySelector': 'Element',
  'document.createElement': 'Element',
  'document.body': 'Element',
  'document.documentElement': 'Element',
  'document.querySelectorAll': 'Array',
  'document.getElementsByClassName': 'Array',
  'Element.querySelector': 'Element',
  'Element.closest': 'Element',
  'Element.parentElement': 'Element',
  'Element.parentNode': 'Element',
  'Element.firstElementChild': 'Element',
  'Element.lastElementChild': 'Element',
  'Element.nextElementSibling': 'Element',
  'Element.previousElementSibling': 'Element',
  'Element.cloneNode': 'Element',
  'Element.classList': 'classList',
  'Element.style': 'Element.style',
  'Element.querySelectorAll': 'Array',
  'Element.children': 'Array',
  'Element.getContext': 'Ctx2D',
  'Element.getBoundingClientRect': 'Any',
  'window.localStorage': 'Storage',
  'window.sessionStorage': 'Storage',
  'window.document': 'document',
  'String.split': 'Array',
  'Array.map': 'Array',
  'Array.filter': 'Array',
  'Array.slice': 'Array',
  'Array.concat': 'Array',
  'Array.sort': 'Array',
  'Array.reverse': 'Array',
  'Array.flat': 'Array',
  'String.trim': 'String',
  'String.slice': 'String',
  'String.substring': 'String',
  'String.replace': 'String',
  'String.toLowerCase': 'String',
  'String.toUpperCase': 'String',
  'Array.join': 'String',
  'Response.json': 'Any',
  'Response.text': 'String',
  'fetch': 'Promise',
  'Promise.then': 'Promise',
  'Promise.catch': 'Promise',
  'Theme.of': 'Theme',
  'Toast.makeText': 'Toast',
  'Snackbar.make': 'Snackbar.Instance',
  'LayoutInflater.from': 'LayoutInflater',
  'Uri.parse': 'Uri',
  'Uri.fromFile': 'Uri',
  'Intent.createChooser': 'Intent',
  'Bitmap.createBitmap': 'Bitmap',
  'Bitmap.createScaledBitmap': 'Bitmap',
  'BitmapFactory.decodeFile': 'Bitmap',
  'BitmapFactory.decodeStream': 'Bitmap',
  'Environment.getExternalStorageDirectory': 'File',
  'Environment.getExternalStoragePublicDirectory': 'File',
  'Environment.getDataDirectory': 'File',
  'Pattern.compile': 'Pattern',
  'Calendar.getInstance': 'Calendar',
  'Executors.newSingleThreadExecutor': 'ExecutorService',
  'Looper.getMainLooper': 'Looper',
  'Settings.Secure': 'Any',
};

String? _nameHint(String lang, String n) {
  final l = n.toLowerCase();
  if (lang == 'js') {
    if (l == 'e' || l == 'ev' || l == 'evt' || l == 'event') return 'Event';
    if (l == 'ctx' || l == 'context' || l == 'c2d') return 'Ctx2D';
    if (l == 'res' || l == 'response') return 'Response';
    if (l == 'el' || l == 'elem' || l == 'element' || l == 'node' || l == 'btn' || l == 'button' || l == 'input' || l == 'container' || l == 'target' || l == 'form' || l == 'img' || l == 'canvas' || l == 'div' || l == 'parent' || l == 'child' || l == 'card') return 'Element';
    if (l == 'arr' || l == 'list' || l == 'items' || l == 'results' || l == 'array') return 'Array';
    if (l == 'str' || l == 'text' || l == 'name' || l == 'url' || l == 'html') return 'String';
    if (l == 'date') return 'Date';
    if (l == 'map') return 'Map';
    return null;
  }
  if (lang == 'kt' || lang == 'java') {
    if (l == 'context' || l == 'ctx' || l == 'activity' || l == 'this') return 'Context';
    if (l == 'intent') return 'Intent';
    if (l == 'bundle' || l == 'savedinstancestate' || l == 'extras') return 'Bundle';
    if (l == 'prefs' || l == 'sharedpreferences' || l == 'sp') return 'SharedPreferences';
    if (l == 'editor') return 'SharedPreferences.Editor';
    if (l == 'file') return 'File';
    if (l == 'sb' || l == 'builder' || l == 'stringbuilder') return 'StringBuilder';
    if (l == 'handler') return 'Handler';
    if (l == 'uri') return 'Uri';
    if (l == 'json' || l == 'jsonobject' || l == 'obj') return 'JSONObject';
    if (l == 'jsonarray' || l == 'arr') return 'JSONArray';
    if (l == 'conn' || l == 'connection') return 'HttpURLConnection';
    if (l == 'window') return 'Window';
    if (l == 'settings' || l == 'websettings') return 'WebSettings';
    if (l == 'cursor') return 'Cursor';
    if (l == 'mediaplayer' || l == 'player') return 'MediaPlayer';
    if (l == 'bitmap' || l == 'bmp') return 'Bitmap';
    if (l == 'canvas') return 'Canvas';
    if (l == 'paint') return 'Paint';
    const pre = {'tv': 'TextView', 'et': 'EditText', 'iv': 'ImageView', 'btn': 'Button', 'rv': 'RecyclerView', 'pb': 'ProgressBar', 'cb': 'CheckBox', 'wv': 'WebView', 'lv': 'ListView'};
    for (final e in pre.entries) {
      if (n.startsWith(e.key) && n.length > e.key.length && n.codeUnitAt(e.key.length) >= 65 && n.codeUnitAt(e.key.length) <= 90) return e.value;
    }
    String? bestV;
    for (final v in _w(kViewTypes)) {
      if (n.endsWith(v) && (bestV == null || v.length > bestV.length)) bestV = v;
    }
    if (bestV != null) return bestV;
    if (n.endsWith('Layout')) return 'LinearLayout';
  }
  if (lang == 'dart') {
    if (l == 'controller' || l == 'textcontroller' || l.endsWith('controller') && l.startsWith('text')) return 'TextEditingController';
    if (l == 'scrollcontroller') return 'ScrollController';
    if (l == 'context') return 'BuildContext';
    if (l == 'prefs') return 'SharedPreferences';
    if (l == 'color') return 'Color';
  }
  return null;
}

/// members (and their owner) for a value of type [type].
void _typeMembers(_Cx c, _Col col, String type, {double bonus = 0, int depth = 0}) {
  var b = _baseType(type);
  if (b.endsWith('[]')) b = c.lang == 'js' ? 'Array' : 'List';
  final lang = c.lang;
  void tbl(String key, String owner) {
    String? s;
    if (lang == 'kt' || lang == 'java') {
      s = kMembers['$lang:$key'] ?? kMembers[key];
    } else if (lang == 'dart') {
      s = kDartMembers[key];
    } else if (lang == 'js') {
      s = kJsMembers[key];
    }
    if (s == null) return;
    for (final t in _w(s)) {
      _addTok(col, t, owner, bonus: bonus);
    }
  }

  if (lang == 'kt' || lang == 'java') {
    const lists = {'MutableList', 'ArrayList', 'LinkedList', 'Collection', 'Iterable', 'Array', 'IntArray', 'MutableCollection', 'Set', 'MutableSet', 'HashSet', 'LinkedHashSet', 'Stack', 'Queue', 'Deque', 'ArrayDeque'};
    const maps = {'MutableMap', 'HashMap', 'LinkedHashMap', 'TreeMap', 'ConcurrentHashMap', 'SortedMap'};
    if (b == 'List' || lists.contains(b)) {
      tbl('List', 'List');
      return;
    }
    if (b == 'Map' || maps.contains(b)) {
      tbl('Map', 'Map');
      return;
    }
    if (b == 'String' || b == 'CharSequence') {
      tbl('String', 'String');
      return;
    }
    if (const {'Int', 'Long', 'Double', 'Float', 'Short', 'Byte', 'Number'}.contains(b)) {
      for (final t in _w('toString() toInt() toLong() toDouble() toFloat() coerceIn( coerceAtLeast( coerceAtMost( absoluteValue sign compareTo( rem( times( plus( minus( div( until( downTo( rangeTo( toChar()')) {
        _addTok(col, t, b, bonus: bonus);
      }
      return;
    }
    final fam = kViewFamily[b];
    if (fam != null) {
      for (final f in _w(fam)) {
        tbl(f, f);
      }
      tbl('View', 'View');
      return;
    }
    if (b == 'View' || b == 'Button') {
      tbl('View', 'View');
      return;
    }
    if (_w(kContextTypes).contains(b)) {
      tbl('Context', 'Context');
      return;
    }
  } else if (lang == 'dart') {
    const lists = {'Iterable', 'List', 'Queue'};
    if (lists.contains(b)) {
      tbl('List', 'List');
      return;
    }
    if (b == 'num' || b == 'double' || b == 'int') {
      tbl(b, b);
      return;
    }
  } else if (lang == 'js') {
    if (b == 'HTMLElement' || b == 'Node' || b == 'HTMLInputElement' || b == 'HTMLCanvasElement') b = 'Element';
    if (b == 'NodeList' || b == 'HTMLCollection') b = 'Array';
  }
  String? direct;
  if (lang == 'kt' || lang == 'java') {
    direct = kMembers['$lang:$b'] ?? kMembers[b];
  } else if (lang == 'dart') {
    direct = kDartMembers[b];
  } else if (lang == 'js') {
    direct = kJsMembers[b];
  }
  if (direct != null) {
    for (final t in _w(direct)) {
      _addTok(col, t, b, bonus: bonus);
    }
  }
  if (lang == 'js' && b == 'Element') {
    for (final t in _w(kJsMembers['Object']!)) {
      _addTok(col, t, 'Object', bonus: bonus - 5);
    }
  }
  // one of the project's own classes (this file first, then the others)
  var found = false;
  void fromSyms(List<_Sym> syms, String owner) {
    for (final s in syms) {
      if (s.kind == SK.cls) continue;
      col.add(s.kind == SK.method ? '${s.name}()' : s.name, s.kind == SK.method ? '${s.name}(§)' : s.name, s.kind, detail: owner, bonus: bonus + 3, matchOn: s.name);
    }
  }

  bool declares(List<_Sym> syms) => syms.any((s) => s.kind == SK.cls && s.name == b);
  if (declares(c.syms)) {
    fromSyms(c.syms, b);
    found = true;
  }
  for (final f in _idx.values) {
    if (f.path == c.path || f.text == null || f.lang != lang) continue;
    if (declares(f.syms)) {
      fromSyms(f.syms, b);
      found = true;
      break;
    }
  }
  if (found && depth < 2) {
    // what the class extends (AppCompatActivity gives you all the Context members)
    final src = declares(c.syms) ? c.text : _idx.values.firstWhere((f) => f.path != c.path && f.lang == lang && f.text != null && declares(f.syms)).text!;
    final eb = RegExp.escape(b);
    final sup = (lang == 'kt'
            ? RegExp('class\\s+$eb\\b(?:\\s*<[^>]*>)?(?:\\s*\\([^)]*\\))?\\s*:\\s*([A-Za-z_][\\w.]*)')
            : RegExp('(?:class|interface)\\s+$eb\\b[^{]*?extends\\s+([A-Za-z_][\\w.]*)'))
        .firstMatch(src)
        ?.group(1);
    if (sup != null && sup != b) _typeMembers(c, col, sup, bonus: bonus - 4, depth: depth + 1);
  }
}

void _membersAfterDot(_Cx c, _Col col, _Chain ch) {
  final lang = c.lang;
  final segs = ch.segs;
  if (segs.isEmpty) {
    _fallbackMembers(c, col);
    return;
  }
  final names = segs.map(_segName).toList();
  final key = names.join('.');
  final first = names[0];
  final firstIsVar = segs.length == 1 && c.types.containsKey(first);

  // Android resources: R.layout.  R.id.
  if ((lang == 'kt' || lang == 'java') && first == 'R' && segs.every((s) => !s.endsWith('()'))) {
    if (segs.length == 1) {
      for (final k in _w(kResKinds)) {
        col.add(k, k, SK.module, detail: 'R.$k', bonus: 10);
      }
    } else if (segs.length == 2) {
      for (final n in _resNames(names[1], c)) {
        col.add(n.replaceAll('.', '_'), n.replaceAll('.', '_'), SK.resource, detail: 'R.${names[1]}', bonus: 12);
      }
    }
    return;
  }

  // Dart icons and colours
  if (lang == 'dart') {
    if (key == 'Icons') {
      for (final n in _w(kDartIcons)) {
        for (final suf in const ['', '_rounded', '_outlined']) {
          col.add('$n$suf', '$n$suf', SK.field, detail: 'IconData', bonus: suf.isEmpty ? 8 : 4);
        }
      }
      return;
    }
    if (segs.length == 2 && first == 'Colors' && !segs[1].endsWith('()')) {
      for (final s in _w('shade50 shade100 shade200 shade300 shade400 shade500 shade600 shade700 shade800 shade900 withOpacity( withValues( value')) {
        _addTok(col, s, 'Color', bonus: 10);
      }
      return;
    }
    if (segs.length >= 2 && segs.last == 'of()' && kDartOf.containsKey(first)) {
      for (final t in _w(kDartOf[first]!)) {
        _addTok(col, t, '$first.of', bonus: 12);
      }
      return;
    }
  }

  // static members: Toast.  Build.VERSION.  document.  navigator.clipboard.
  if (!firstIsVar) {
    final table = lang == 'dart' ? kDartStatics : (lang == 'js' ? kJsStatics : kStatics);
    var st = table[key];
    if (st == null && lang == 'js' && names.length > 1 && names.last == 'style') st = kJsStatics['Element.style'];
    if (st != null && st.isNotEmpty) {
      final k = segs.last.endsWith('()') ? null : st;
      if (k != null) {
        for (final t in _w(k)) {
          _addTok(col, t, key, bonus: 12);
        }
        if (lang != 'kt' && lang != 'java') return;
        // a static table can also be the start of a chain such as Intent. (still fine)
        return;
      }
    }
    if (lang == 'kt' || lang == 'java') {
      final m = kMembers[key];
      if (m != null) {
        for (final t in _w(m)) {
          _addTok(col, t, key, bonus: 12);
        }
        return;
      }
    }
  }

  // follow the chain type by type
  String? t;
  final seg0 = segs[0];
  if (seg0.endsWith('()')) {
    final fv = RegExp(r'^(?:requireViewById|findViewById)<\s*([\w.]+)').firstMatch(seg0);
    if (fv != null) {
      t = fv.group(1);
    } else if (first.isNotEmpty && first.codeUnitAt(0) >= 65 && first.codeUnitAt(0) <= 90) {
      t = first;
    } else if (lang == 'js' && (first == 'fetch')) {
      t = 'Promise';
    } else {
      t = _chainTypes[first] ?? _chainTypes['Context.$first'];
    }
  } else if (c.types.containsKey(first)) {
    t = c.types[first];
  } else if (first == 'this' || first == 'self') {
    t = null;
    if (segs.length == 1) {
      for (final s in c.syms) {
        if (s.kind == SK.cls) continue;
        col.add(s.kind == SK.method ? '${s.name}()' : s.name, s.kind == SK.method ? '${s.name}(§)' : s.name, s.kind, detail: 'this', bonus: 14, matchOn: s.name);
      }
      if (lang == 'kt' || lang == 'java' || lang == 'dart') _typeMembers(c, col, lang == 'dart' ? 'State' : 'Context', bonus: 2);
      return;
    }
  } else if ((lang == 'js') && kJsStatics.containsKey(first)) {
    t = first;
  } else if (first.isNotEmpty && first.codeUnitAt(0) >= 65 && first.codeUnitAt(0) <= 90 && segs.length > 1) {
    t = first;
  } else {
    t = _nameHint(lang, first);
  }
  for (var i = 1; i < segs.length && t != null; i++) {
    final n = names[i];
    final cur = _baseType(t);
    final direct = _chainTypes['$cur.$n'] ?? _chainTypes['${_aliasFor(cur)}.$n'];
    if (direct != null) {
      t = direct;
    } else if (segs[i].endsWith('()') && RegExp(r'^(set|add|put|apply|append|with|enable|disable)').hasMatch(n)) {
      // fluent builder: the same object comes back
    } else if (names.sublist(0, i + 1).join('.') == 'AlertDialog.Builder' || names.sublist(0, i + 1).join('.') == 'NotificationCompat.Builder') {
      t = names.sublist(0, i + 1).join('.');
    } else if (c.types.containsKey(n) && i == segs.length - 1) {
      t = c.types[n];
    } else {
      t = null;
    }
  }
  if (t != null) {
    final before = col.best.length;
    final k = _baseType(t);
    if (lang == 'js' && kJsStatics.containsKey(k) && !kJsMembers.containsKey(k)) {
      for (final s in _w(kJsStatics[k]!)) {
        _addTok(col, s, k, bonus: 12);
      }
    } else {
      _typeMembers(c, col, t, bonus: 12);
    }
    if (col.best.length > before) return;
  }
  _fallbackMembers(c, col);
}

String _aliasFor(String t) {
  if (kViewFamily.containsKey(t)) return 'View';
  if (_w(kContextTypes).contains(t)) return 'Context';
  if (t == 'HTMLElement') return 'Element';
  return t;
}

/// The receiver is unknown: offer the member names that already appear after a "." in this file and project.
void _fallbackMembers(_Cx c, _Col col) {
  final seen = <String>{};
  void scan(String? t, double bonus) {
    if (t == null) return;
    for (final m in RegExp(r'\.([A-Za-z_]\w{1,})(\()?').allMatches(t)) {
      final n = m.group(1)!;
      final isCall = m.group(2) != null;
      if (!seen.add('$n$isCall')) continue;
      if (isCall) {
        col.add('$n()', '$n(§)', SK.method, bonus: bonus, matchOn: n);
      } else {
        col.add(n, n, SK.field, bonus: bonus);
      }
    }
  }

  scan(c.text.length < 150000 ? c.text : null, 6);
  var n = 0;
  for (final f in _idx.values) {
    if (f.path == c.path || f.lang != c.lang || f.text == null) continue;
    scan(f.text, 0);
    if (++n > 30) break;
  }
  if (c.lang == 'kt') {
    for (final t in _w(kMembers['Any']!)) {
      _addTok(col, t, 'Any', bonus: 4);
    }
  }
  if (c.lang == 'js') {
    for (final k in const ['Element', 'Array', 'String', 'Object']) {
      for (final t in _w(kJsMembers[k]!)) {
        _addTok(col, t, k, bonus: -4);
      }
    }
  }
}

String _pkgOf(String text) => RegExp(r'^\s*package\s+([\w.]+)', multiLine: true).firstMatch(text)?.group(1) ?? '';

String? _impText;
final _impFull = <String>{}, _impSimple = <String>{}, _impPk = <String>{};

bool _hasImport(String text, String fqn) {
  if (!identical(_impText, text)) {
    _impText = text;
    _impFull.clear();
    _impSimple.clear();
    _impPk.clear();
    for (final m in RegExp(r'^\s*import\s+([\w.]+?)(\.\*)?(?:\s+as\s+(\w+))?\s*;?\s*$', multiLine: true).allMatches(text)) {
      final full = m.group(1)!;
      if (m.group(2) != null) {
        _impPk.add(full);
      } else {
        _impFull.add(full);
        _impSimple.add(m.group(3) ?? full.substring(full.lastIndexOf('.') + 1));
      }
    }
  }
  final simple = fqn.substring(fqn.lastIndexOf('.') + 1);
  final pk = fqn.contains('.') ? fqn.substring(0, fqn.lastIndexOf('.')) : '';
  return _impFull.contains(fqn) || _impPk.contains(pk) || _impSimple.contains(simple);
}

String? _importFor(_Cx c, String simple, String fqn) {
  final lang = c.lang;
  if (lang == 'kt' && _w(kKotlinBuiltin).contains(simple)) return null;
  if (fqn.startsWith('java.lang.')) return null;
  final pk = fqn.contains('.') ? fqn.substring(0, fqn.lastIndexOf('.')) : '';
  if (_pkgOf(c.text) == pk) return null;
  if (_hasImport(c.text, fqn)) return null;
  return fqn;
}

const _dartLibs = <String, String>{
  'jsonEncode': 'dart:convert', 'jsonDecode': 'dart:convert', 'utf8': 'dart:convert', 'base64Encode': 'dart:convert',
  'File': 'dart:io', 'Directory': 'dart:io', 'Platform': 'dart:io', 'HttpClient': 'dart:io', 'Process': 'dart:io', 'exit': 'dart:io', 'sleep': 'dart:io',
  'Random': 'dart:math', 'sqrt': 'dart:math', 'pow': 'dart:math', 'sin': 'dart:math', 'cos': 'dart:math', 'tan': 'dart:math', 'log': 'dart:math', 'exp': 'dart:math', 'atan2': 'dart:math', 'min': 'dart:math', 'max': 'dart:math',
  'Timer': 'dart:async', 'Completer': 'dart:async', 'StreamController': 'dart:async', 'unawaited': 'dart:async',
  'compute': 'package:flutter/foundation.dart', 'debugPrint': 'package:flutter/foundation.dart',
  'SystemChrome': 'package:flutter/services.dart', 'DeviceOrientation': 'package:flutter/services.dart', 'Clipboard': 'package:flutter/services.dart', 'ClipboardData': 'package:flutter/services.dart',
  'HapticFeedback': 'package:flutter/services.dart', 'LogicalKeyboardKey': 'package:flutter/services.dart', 'SystemUiOverlayStyle': 'package:flutter/services.dart', 'SystemUiMode': 'package:flutter/services.dart',
};

String? _dartImportLine(_Cx c, String lib) {
  final t = c.text;
  if (lib == 'material') {
    if (RegExp(r'''import\s+['"]package:flutter/(material|widgets|cupertino)\.dart['"]''').hasMatch(t)) return null;
    return kDartImportMaterial;
  }
  if (RegExp('''import\\s+['"]${RegExp.escape(lib)}['"]''').hasMatch(t)) return null;
  if (lib.startsWith('package:flutter/foundation') && RegExp(r'''import\s+['"]package:flutter/(material|widgets|cupertino|foundation)\.dart['"]''').hasMatch(t)) return null;
  return "import '$lib';";
}

Completion? _code(_Cx c, String text, int off, int shift) {
  final lang = c.lang;
  final ext = c.ext;
  final js = lang == 'js';
  // after the cursor must not be the middle of a word
  if (off < text.length && _isWordCh(text.codeUnitAt(off))) return null;
  var start = off;
  while (start > 0 && (_isWordCh(text.codeUnitAt(start - 1)) || (js && text[start - 1] == r'$'))) {
    start--;
  }
  final prefix = text.substring(start, off);
  final st = _state(text, off, ext);
  if (st.$1 == 1) return null;
  if (st.$1 == 2) return _inString(c, text, off, start, prefix, st.$2);
  final dotted = start > 0 && text[start - 1] == '.';
  final cascade = dotted && start > 1 && text[start - 2] == '.';
  final col = _Col(prefix);

  if (dotted && !(cascade && lang != 'dart')) {
    final lineStart = text.lastIndexOf('\n', start > 0 ? start - 1 : 0) + 1;
    if (RegExp(r'^\s*(?:import|package)\b').hasMatch(text.substring(lineStart < start ? lineStart : start, start))) return null;
    if (cascade) {
      _fallbackMembers(c, col);
    } else {
      // a number like 1.5 is not a member access
      if (start > 1 && text.codeUnitAt(start - 2) >= 48 && text.codeUnitAt(start - 2) <= 57 && !_isIdStart(start > 2 ? text.codeUnitAt(start - 3) : 32) && prefix.isNotEmpty && !_isIdStart(prefix.codeUnitAt(0))) return null;
      _membersAfterDot(c, col, _chainBefore(text, start - 1));
    }
    final items = col.top(14);
    if (items.isEmpty) return null;
    return Completion(start + shift, items);
  }
  if (prefix.length < 2) return null;
  if (prefix.isNotEmpty && _isDigitStart(prefix)) return null;
  // after "fun " / "val " / "class " the person is naming something new
  final before = text.substring(start - (start > 30 ? 30 : start), start);
  if (RegExp(r'\b(?:fun|val|var|class|object|interface|enum|typedef|mixin|extension|const|let|function|package)\s+$').hasMatch(before) && lang != 'dart') {
    if (!RegExp(r'\b(?:val|var)\s+$').hasMatch(before) || prefix.length < 3) return null;
  }
  if (lang == 'dart' && RegExp(r'\b(?:class|mixin|enum|typedef)\s+$').hasMatch(before)) return null;
  if (RegExp(r'\b(?:import|package)\s+[\w.]*$').hasMatch(before + prefix) && lang != 'dart' && lang != 'js') return null;

  final dollar = start > 0 && text[start - 1] == r'$';
  // names declared in this file
  for (final s in c.syms) {
    final b = s.kind == SK.variable || s.kind == SK.field ? 30.0 : 24.0;
    col.add(s.kind == SK.method ? '${s.name}()' : s.name, s.kind == SK.method ? '${s.name}(§)' : s.name, s.kind, detail: s.detail, bonus: b, matchOn: s.name);
  }
  if (!dollar) {
    for (final e in c.words.entries) {
      if (e.key == prefix) continue;
      col.add(e.key, e.key, SK.field, detail: '', bonus: (e.value > 5 ? 5 : e.value).toDouble() - 6);
    }
  }
  // keywords
  final kwKey = lang == 'js' ? (ext == 'ts' ? 'jsts' : 'js') : lang;
  if (!dollar) {
    for (final k in _w(kKeywords[kwKey == 'jsts' ? 'js' : kwKey] ?? '')) {
      col.add(k, k, SK.keyword, bonus: 2);
    }
    if (kwKey == 'jsts') {
      for (final k in _w(kKeywords['ts']!)) {
        col.add(k, k, SK.keyword, bonus: 2);
      }
    }
  }
  if (lang == 'kt' || lang == 'java') _addAndroid(c, col, text, dollar);
  if (lang == 'kt' && !dollar) {
    for (final t in _w(kKtTop)) {
      _addTok(col, t, 'Kotlin', bonus: 8);
    }
  }
  if (lang == 'dart') _addFlutter(c, col, text, start, dollar);
  if (lang == 'js') {
    for (final t in _w(kJsTop)) {
      _addTok(col, t, 'JavaScript', bonus: 8);
    }
  }
  // snippets
  if (!dollar) {
    final sn = kSnippets[lang == 'gradle' ? 'kt' : lang];
    if (sn != null && lang != 'gradle') {
      for (final line in sn.split('\n')) {
        final p = line.split('|');
        if (p.length < 3) continue;
        col.add(p[0], p[1].replaceAll(r'\n', '\n'), SK.snippet, detail: p[2], bonus: 6);
      }
    }
  }
  // the rest of the project
  if (!dollar) _addProjectSymbols(c, col);
  final items = col.top(14);
  if (items.isEmpty) return null;
  return Completion(start + shift, items);
}

bool _isDigitStart(String s) => s.codeUnitAt(0) >= 48 && s.codeUnitAt(0) <= 57;

void _addAndroid(_Cx c, _Col col, String text, bool dollar) {
  if (dollar) return;
  final lang = c.lang;
  for (final e in _w(kImports)) {
    final i = e.indexOf('=');
    final name = e.substring(0, i);
    final fqn = e.substring(i + 1);
    if (lang == 'java' && (name == 'launch' || name == 'withContext' || name == 'delay' || name == 'runBlocking' || name == 'Dispatchers' || name == 'CoroutineScope' || name == 'GlobalScope' || name == 'Job')) continue;
    if (_score(col.prefix, name) <= 0) continue;
    final decl = c.syms.any((s) => s.name == name && s.kind == SK.cls);
    if (decl) continue;
    final imp = _importFor(c, name, fqn);
    final pkgShort = fqn.substring(0, fqn.lastIndexOf('.'));
    final isFn = name == 'launch' || name == 'withContext' || name == 'delay' || name == 'runBlocking';
    col.add(name, isFn ? '$name { § }' : name, isFn ? SK.method : SK.cls, detail: pkgShort, imp: imp, bonus: 12);
  }
}

void _addFlutter(_Cx c, _Col col, String text, int start, bool dollar) {
  if (dollar) return;
  const needProvider = {'ChangeNotifierProvider', 'Consumer', 'Provider'};
  final material = _dartImportLine(c, 'material');
  for (final w in _w(kDartWidgets)) {
    if (needProvider.contains(w)) continue;
    final isType = const {'StatelessWidget', 'StatefulWidget', 'State', 'BuildContext', 'Widget', 'Key', 'GlobalKey', 'ValueNotifier', 'ChangeNotifier', 'FocusNode', 'TextEditingController', 'ScrollController', 'AnimationController', 'Color', 'Curve', 'Size', 'Offset', 'Rect', 'Radius', 'InheritedWidget', 'Tab'}.contains(w);
    col.add(w, isType ? w : '$w(§)', SK.cls, detail: 'Flutter', imp: material, bonus: 14);
  }
  for (final t in _w(kDartTop)) {
    final name = t.endsWith('(') ? t.substring(0, t.length - 1) : (t.endsWith('()') ? t.substring(0, t.length - 2) : t);
    final lib = _dartLibs[name];
    _addTok(col, t, 'Dart', bonus: 8, imp: lib == null ? null : _dartImportLine(c, lib));
  }
  for (final e in _dartLibs.entries) {
    if (e.key.codeUnitAt(0) >= 65 && e.key.codeUnitAt(0) <= 90 && _score(col.prefix, e.key) > 0) col.add(e.key, e.key, SK.cls, detail: e.value, imp: _dartImportLine(c, e.value), bonus: 10);
  }
  final widgetNames = _w(kDartWidgets);
  const dartCore = {'Future', 'DateTime', 'Uri', 'Duration', 'Platform', 'String'};
  for (final k in kDartStatics.keys) {
    if (k.codeUnitAt(0) < 65 || k.codeUnitAt(0) > 90 || widgetNames.contains(k) || _dartLibs.containsKey(k)) continue;
    col.add(k, k, SK.cls, detail: dartCore.contains(k) ? 'Dart' : 'Flutter', imp: dartCore.contains(k) ? null : material, bonus: 10);
  }
  for (final k in kDartMembers.keys) {
    if (_dartLibs.containsKey(k) || widgetNames.contains(k)) continue;
    if (k.codeUnitAt(0) >= 65 && k.codeUnitAt(0) <= 90) col.add(k, k, SK.cls, detail: 'Dart', bonus: 6);
  }
  col.add('Icons', 'Icons', SK.cls, detail: 'Flutter', imp: material, bonus: 12);
  col.add('Colors', 'Colors', SK.cls, detail: 'Flutter', imp: material, bonus: 12);
  // named parameters of the widget the cursor is inside
  final call = _dartCall(text, start);
  if (call != null) {
    final key = call.$1;
    final used = call.$2;
    final params = kDartParams[key] ?? kDartParams[key.split('.').first];
    if (params != null) {
      for (final p in _w(params)) {
        if (used.contains(p)) continue;
        col.add('$p:', _paramInsert(key, p), SK.attr, detail: key, bonus: 46, matchOn: p);
      }
    }
  }
}

String _paramInsert(String widget, String p) {
  switch (p) {
    case 'onPressed':
    case 'onTap':
    case 'onLongPress':
    case 'onDoubleTap':
      return '$p: () {\n  §\n},';
    case 'onChanged':
    case 'onSubmitted':
    case 'onPageChanged':
      return '$p: (value) {\n  §\n},';
    case 'itemBuilder':
      return '$p: (context, index) {\n  return §;\n},';
    case 'separatorBuilder':
      return '$p: (context, index) => const Divider(),';
    case 'builder':
      if (widget == 'FutureBuilder' || widget == 'StreamBuilder') return '$p: (context, snapshot) {\n  return §;\n},';
      if (widget == 'ValueListenableBuilder') return '$p: (context, value, child) {\n  return §;\n},';
      if (widget == 'MaterialPageRoute') return '$p: (context) => §,';
      return '$p: (context) {\n  return §;\n},';
    case 'children':
    case 'actions':
    case 'items':
    case 'boxShadow':
    case 'colors':
    case 'stops':
      return '$p: [\n  §\n],';
    case 'validator':
      return '$p: (value) {\n  §\n  return null;\n},';
  }
  return '$p: §,';
}

/// (callee, parameters already given) when the cursor is at the start of an argument, e.g.  Text('x', |
(String, Set<String>)? _dartCall(String t, int start) {
  var i = start - 1;
  while (i >= 0 && (t[i] == ' ' || t[i] == '\n' || t[i] == '\t' || t[i] == '\r')) {
    i--;
  }
  if (i < 0 || (t[i] != '(' && t[i] != ',')) return null;
  var depth = 0;
  var j = i;
  if (t[i] == '(') {
    depth = 0;
  }
  var open = -1;
  for (var k = i; k >= 0 && i - k < 6000; k--) {
    final ch = t[k];
    if (ch == ')' || ch == ']' || ch == '}') {
      depth++;
    } else if (ch == '(' || ch == '[' || ch == '{') {
      if (depth == 0) {
        open = k;
        break;
      }
      depth--;
    } else if (ch == ';' && depth == 0) {
      return null;
    }
  }
  if (open < 0 || t[open] != '(') return null;
  var e = open;
  while (e > 0 && t[e - 1] == ' ') {
    e--;
  }
  if (e > 0 && t[e - 1] == '>') {
    final g = _skipBack(t, e - 1, '<', '>');
    if (g > 0) e = g;
  }
  var s = e;
  while (s > 0 && (_isWordCh(t.codeUnitAt(s - 1)) || t[s - 1] == '.')) {
    s--;
  }
  final callee = t.substring(s, e);
  if (callee.isEmpty || !_isIdStart(callee.codeUnitAt(0))) return null;
  j = open + 1;
  final used = <String>{};
  var d = 0;
  while (j < start && j < t.length) {
    final ch = t[j];
    if (ch == '"' || ch == "'") {
      j = skipCodeString(t, j, 'dart');
      continue;
    }
    if (ch == '(' || ch == '[' || ch == '{') {
      d++;
    } else if (ch == ')' || ch == ']' || ch == '}') {
      d--;
    } else if (d == 0 && _isIdStart(t.codeUnitAt(j)) && (j == 0 || !_isWordCh(t.codeUnitAt(j - 1)))) {
      var q = j;
      while (q < t.length && _isWordCh(t.codeUnitAt(q))) {
        q++;
      }
      var r = q;
      while (r < t.length && t[r] == ' ') {
        r++;
      }
      if (r < t.length && t[r] == ':' && (r + 1 >= t.length || t[r + 1] != ':')) used.add(t.substring(j, q));
      j = q;
      continue;
    }
    j++;
  }
  return (callee, used);
}

void _addProjectSymbols(_Cx c, _Col col) {
  final lang = c.lang;
  var n = 0;
  for (final f in _idx.values) {
    if (f.path == c.path || f.lang != lang || f.text == null) continue;
    if (++n > 400) break;
    final pk = f.pkg;
    final file = f.path.split('/').last;
    for (final s in f.syms) {
      if (s.kind != SK.cls && s.kind != SK.method) continue;
      if (s.kind == SK.method && lang != 'js' && lang != 'dart') continue;
      String? imp;
      if (_score(col.prefix, s.name) <= 0) continue;
      if (s.kind == SK.cls) {
        if (lang == 'kt' || lang == 'java') {
          if (pk.isNotEmpty) imp = _importFor(c, s.name, '$pk.${s.name}');
        } else if (lang == 'dart') {
          imp = _dartProjectImport(c, f.path);
        }
      }
      if (c.syms.any((x) => x.name == s.name)) continue;
      col.add(s.kind == SK.method ? '${s.name}()' : s.name, s.kind == SK.method ? '${s.name}(§)' : s.name, s.kind, detail: file, imp: imp, bonus: 14, matchOn: s.name);
    }
  }
}

String? _dartProjectImport(_Cx c, String filePath) {
  String? pubName;
  final pub = c.files['pubspec.yaml'];
  if (pub != null) {
    try {
      pubName = RegExp(r'^name:\s*([\w]+)', multiLine: true).firstMatch(utf8.decode(pub, allowMalformed: true))?.group(1);
    } catch (_) {}
  }
  String line;
  if (pubName != null && filePath.startsWith('lib/')) {
    line = "import 'package:$pubName/${filePath.substring(4)}';";
  } else {
    line = "import '${_rel(c.dir, filePath)}';";
  }
  final target = line.substring(line.indexOf("'") + 1, line.lastIndexOf("'"));
  if (c.text.contains("'$target'") || c.text.contains('"$target"')) return null;
  // the same file through the other kind of path
  final base = filePath.split('/').last;
  if (RegExp('''import\\s+['"][^'"]*${RegExp.escape(base)}['"]''').hasMatch(c.text)) return null;
  return line;
}

String _rel(String fromDir, String to) {
  final a = fromDir.isEmpty ? <String>[] : fromDir.split('/');
  final b = to.split('/');
  var i = 0;
  while (i < a.length && i < b.length - 1 && a[i] == b[i]) {
    i++;
  }
  final up = List<String>.filled(a.length - i, '..');
  return [...up, ...b.sublist(i)].join('/');
}

Completion? _inString(_Cx c, String text, int off, int start, String prefix, int strStart) {
  final lang = c.lang;
  // Kotlin / Dart:  "$name"  and  "${name"
  if (lang == 'kt' || lang == 'dart') {
    final isDollar = start > 0 && text[start - 1] == r'$';
    final inBrace = !isDollar && text.lastIndexOf(r'${', off) > strStart && !text.substring(text.lastIndexOf(r'${', off), off).contains('}');
    if ((isDollar || inBrace) && prefix.length >= 1) {
      final col = _Col(prefix);
      for (final s in c.syms) {
        if (s.kind == SK.variable || s.kind == SK.field) col.add(s.name, s.name, s.kind, detail: s.detail, bonus: 30);
      }
      for (final e in c.words.entries) {
        if (e.key != prefix) col.add(e.key, e.key, SK.field, bonus: -6);
      }
      final items = col.top(10);
      return items.isEmpty ? null : Completion(start, items);
    }
  }
  // Dart: import '...'
  if (lang == 'dart') {
    final ls = text.lastIndexOf('\n', off - 1) + 1;
    final line = text.substring(ls, off);
    final m = RegExp(r'''^\s*(?:import|export|part)\s+['"]([^'"]*)$''').firstMatch(line);
    if (m != null) {
      final qStart = off - m.group(1)!.length;
      final typed = m.group(1)!;
      final col = _Col(typed);
      for (final l in _w('dart:async dart:convert dart:io dart:math dart:typed_data dart:ui dart:developer dart:collection dart:isolate')) {
        col.add(l, l, SK.module, detail: 'Dart', bonus: 6);
      }
      for (final l in _w('material widgets cupertino services foundation painting rendering gestures animation scheduler semantics')) {
        col.add('package:flutter/$l.dart', 'package:flutter/$l.dart', SK.module, detail: 'Flutter', bonus: 10);
      }
      final pub = c.files['pubspec.yaml'];
      String? pubName;
      if (pub != null) {
        final y = utf8.decode(pub, allowMalformed: true);
        pubName = RegExp(r'^name:\s*(\w+)', multiLine: true).firstMatch(y)?.group(1);
        final dep = RegExp(r'^dependencies:\s*\n((?:[ \t]+.*\n?|\n)*)', multiLine: true).firstMatch(y)?.group(1) ?? '';
        for (final d in RegExp(r'^\s{2}(\w+):', multiLine: true).allMatches(dep)) {
          final n = d.group(1)!;
          if (n == 'flutter') continue;
          col.add('package:$n/$n.dart', 'package:$n/$n.dart', SK.module, detail: 'package', bonus: 8);
        }
      }
      for (final k in c.files.keys) {
        if (!k.endsWith('.dart') || k == c.path) continue;
        if (pubName != null && k.startsWith('lib/')) col.add('package:$pubName/${k.substring(4)}', 'package:$pubName/${k.substring(4)}', SK.file, detail: 'your file', bonus: 14);
        col.add(_rel(c.dir, k), _rel(c.dir, k), SK.file, detail: 'your file', bonus: 8);
      }
      final items = col.top(12);
      return items.isEmpty ? null : Completion(qStart, items);
    }
  }
  return null;
}

// ------------------------------------------------------------------ resources (res folder)

final _resCache = <String, List<String>>{};
int _resSig = -1;

List<String> _resNames(String kind, _Cx c) {
  var sig = c.files.length;
  for (final f in _idx.values) {
    if (f.path.contains('/res/') || f.path.startsWith('res/')) sig = sig * 31 + f.bytes.length + f.path.length;
  }
  if (sig != _resSig) {
    _resCache.clear();
    _resSig = sig;
  }
  return _resCache.putIfAbsent(kind, () => _computeRes(kind));
}

List<String> _computeRes(String kind) {
  final out = <String>{};
  const fileKinds = {'layout', 'drawable', 'mipmap', 'menu', 'anim', 'xml', 'raw', 'font', 'navigation', 'animator', 'interpolator', 'transition'};
  if (fileKinds.contains(kind)) {
    final re = RegExp('(?:^|/)res/$kind(?:-[^/]*)?/([^/]+?)(?:\\.9)?(?:\\.[^./]+)?\$');
    for (final f in _idx.keys) {
      final m = re.firstMatch(f);
      if (m != null) out.add(m.group(1)!);
    }
    if (kind == 'drawable') {
      for (final f in _idx.values) {
        if (!(f.path.contains('/res/values') || f.path.startsWith('res/values'))) continue;
        final t = f.text;
        if (t == null) continue;
        for (final m in RegExp(r'<drawable\s+name="([^"]+)"').allMatches(t)) {
          out.add(m.group(1)!);
        }
      }
    }
  }
  if (kind == 'id') {
    for (final f in _idx.values) {
      if (f.ext != 'xml') continue;
      final t = f.text;
      if (t == null) continue;
      for (final m in RegExp(r'@\+?id/([A-Za-z0-9_.]+)').allMatches(t)) {
        out.add(m.group(1)!);
      }
      for (final m in RegExp(r'<item\s+[^>]*type="id"[^>]*name="([^"]+)"').allMatches(t)) {
        out.add(m.group(1)!);
      }
    }
  } else if (!fileKinds.contains(kind)) {
    final tags = <String, String>{
      'string': 'string', 'color': 'color', 'dimen': 'dimen', 'style': 'style', 'integer': 'integer', 'bool': 'bool', 'plurals': 'plurals', 'attr': 'attr',
      'array': '(?:string-array|integer-array|array)', 'styleable': 'declare-styleable',
    };
    final tag = tags[kind];
    if (tag != null) {
      final re = RegExp('<$tag\\s+[^>]*?name="([^"]+)"');
      for (final f in _idx.values) {
        if (f.ext != 'xml' || !(f.path.contains('/res/values') || f.path.startsWith('res/values'))) continue;
        final t = f.text;
        if (t == null) continue;
        for (final m in re.allMatches(t)) {
          out.add(m.group(1)!);
        }
      }
    }
  }
  final l = out.toList()..sort();
  return l;
}

// ------------------------------------------------------------------ markup helpers (HTML and Android XML)

class _Tag {
  final int lt;
  final String inner; // text after "<" up to the cursor
  _Tag(this.lt, this.inner);
}

_Tag? _tagAt(String t, int off) {
  if (off == 0) return null;
  final lt = t.lastIndexOf('<', off - 1);
  if (lt < 0) return null;
  if (t.startsWith('<!--', lt)) {
    final e = t.indexOf('-->', lt);
    if (e < 0 || e >= off - 2) return null;
  }
  // a quote that spans a ">" keeps us inside the tag, so look for ">" outside quotes between lt and off
  String? q;
  for (var i = lt + 1; i < off; i++) {
    final c = t[i];
    if (q != null) {
      if (c == q) q = null;
    } else if (c == '"' || c == "'") {
      q = c;
    } else if (c == '>') {
      return null;
    }
  }
  return _Tag(lt, t.substring(lt + 1, off));
}

class _AttrCx {
  final String tag;
  final Set<String> present;
  final String? valueOf; // attribute whose value we are inside (null = typing a name)
  final int valueStart; // offset of the first char of the value (inside the quote)
  final bool afterEquals; // typed "name=" and no quote yet
  _AttrCx(this.tag, this.present, this.valueOf, this.valueStart, this.afterEquals);
}

_AttrCx _attrCx(_Tag t, int off) {
  final s = t.inner;
  final m = RegExp(r'^[A-Za-z][\w:.-]*').firstMatch(s);
  final tag = m?.group(0) ?? '';
  var i = tag.length;
  final present = <String>{};
  String lastName = '';
  String? quote;
  String? valueOf;
  var valueStart = -1;
  var afterEquals = false;
  while (i < s.length) {
    final c = s[i];
    if (quote != null) {
      if (c == quote) {
        quote = null;
        valueOf = null;
      }
      i++;
      continue;
    }
    if (c == '"' || c == "'") {
      quote = c;
      valueOf = lastName;
      valueStart = t.lt + 1 + i + 1;
      afterEquals = false;
      i++;
      continue;
    }
    if (c == '=') {
      afterEquals = true;
      i++;
      continue;
    }
    if (c == ' ' || c == '\n' || c == '\t' || c == '\r') {
      i++;
      continue;
    }
    if (c == '/' || c == '>') {
      i++;
      continue;
    }
    var j = i;
    while (j < s.length && !' \n\t\r="\'/>'.contains(s[j])) {
      j++;
    }
    lastName = s.substring(i, j);
    present.add(lastName);
    afterEquals = false;
    i = j;
  }
  if (quote == null) valueOf = null;
  final endsEq = quote == null && RegExp(r'=\s*$').hasMatch(s);
  return _AttrCx(tag, present, valueOf, valueStart, endsEq);
}

List<String> _openStack(String t, int upTo) {
  final stack = <String>[];
  final re = RegExp(r'<(/?)([A-Za-z][\w:.-]*)([^<>]*?)(/?)>');
  const voidT = {'area', 'base', 'br', 'col', 'embed', 'hr', 'img', 'input', 'link', 'meta', 'param', 'source', 'track', 'wbr'};
  var pos = 0;
  while (pos < upTo) {
    final cm = t.indexOf('<!--', pos);
    final m = re.matchAsPrefix(t, t.indexOf('<', pos) < 0 ? t.length : t.indexOf('<', pos));
    final lt = t.indexOf('<', pos);
    if (lt < 0 || lt >= upTo) break;
    if (cm >= 0 && cm == lt) {
      final e = t.indexOf('-->', cm);
      pos = e < 0 ? upTo : e + 3;
      continue;
    }
    if (m == null || m.end > upTo) {
      pos = lt + 1;
      continue;
    }
    final closing = m.group(1) == '/';
    final name = m.group(2)!;
    final self = m.group(4) == '/';
    final low = name.toLowerCase();
    if (closing) {
      final i = stack.lastIndexWhere((s) => s.toLowerCase() == low);
      if (i >= 0) stack.removeRange(i, stack.length);
    } else if (!self && !voidT.contains(low)) {
      stack.add(name);
      if (low == 'script' || low == 'style') {
        final e = RegExp('</$low', caseSensitive: false).firstMatch(t.substring(m.end > t.length ? t.length : m.end));
        if (e == null) break;
        pos = m.end + e.start;
        continue;
      }
    }
    pos = m.end;
  }
  return stack;
}

// ------------------------------------------------------------------ HTML

Set<String>? _cssNamesCache;
int _cssSig = -1;
Set<String> _projectClasses(_Cx c, {bool ids = false}) {
  var sig = c.files.length;
  for (final f in _idx.values) {
    if (f.ext == 'css' || f.ext == 'scss' || f.ext == 'html' || f.ext == 'htm') sig = sig * 31 + f.bytes.length;
  }
  final key = ids ? sig + 7 : sig;
  if (key != _cssSig || _cssNamesCache == null || ids) {
    final out = <String>{};
    for (final f in _idx.values) {
      final t = f.text;
      if (t == null) continue;
      if (f.ext == 'html' || f.ext == 'htm') {
        final re = RegExp(ids ? r'''\bid\s*=\s*["']([^"']+)["']''' : r'''\bclass\s*=\s*["']([^"']+)["']''');
        for (final m in re.allMatches(t)) {
          for (final n in m.group(1)!.split(RegExp(r'\s+'))) {
            if (n.isNotEmpty && !n.contains('{')) out.add(n);
          }
        }
      }
      if (f.ext == 'css' || f.ext == 'scss' || f.ext == 'html' || f.ext == 'htm') {
        final reSel = RegExp(r'([^{}]+)\{');
        final reName = RegExp(ids ? r'#([A-Za-z_][\w-]*)' : r'\.([A-Za-z_][\w-]*)');
        for (final m in reSel.allMatches(t.length > 200000 ? t.substring(0, 200000) : t)) {
          final sel = m.group(1)!;
          if (sel.contains('(') && sel.contains(';')) continue;
          for (final n in reName.allMatches(sel)) {
            out.add(n.group(1)!);
          }
        }
      }
    }
    if (ids) return out;
    _cssNamesCache = out;
    _cssSig = key;
  }
  return _cssNamesCache!;
}

List<String> _projectPaths(_Cx c, Set<String> exts) {
  final out = <String>[];
  for (final k in c.files.keys) {
    if (k == c.path) continue;
    if (exts.contains(_extOf(k))) out.add(_rel(c.dir, k));
  }
  out.sort();
  return out;
}

Completion? _html(_Cx c) {
  final t = c.text;
  final off = c.off;
  // JavaScript / CSS inside <script> / <style>
  for (final m in RegExp(r'<(script|style)\b[^>]*>', caseSensitive: false).allMatches(t)) {
    if (m.end > off) break;
    final name = m.group(1)!.toLowerCase();
    final close = RegExp('</$name', caseSensitive: false).firstMatch(t.substring(m.end));
    final endAt = close == null ? t.length : m.end + close.start;
    if (off <= endAt && off >= m.end) {
      final body = t.substring(m.end, endAt);
      final typeM = RegExp(r'''type\s*=\s*["']([^"']*)''', caseSensitive: false).firstMatch(m.group(0)!);
      final type = (typeM?.group(1) ?? '').toLowerCase();
      if (name == 'style') return _css(c, body, off - m.end, m.end);
      if (type.isEmpty || type.contains('javascript') || type == 'module') {
        final sub = _Cx(body, off - m.end, '${c.path}.js', c.files);
        return _code(sub, body, off - m.end, m.end);
      }
      return null;
    }
  }
  final tag = _tagAt(t, off);
  if (tag == null) return null;
  final s = tag.inner;
  if (s.startsWith('/')) {
    final prefix = s.substring(1);
    if (prefix.contains(RegExp(r'\s'))) return null;
    final stack = _openStack(t, tag.lt);
    final col = _Col(prefix);
    for (var i = stack.length - 1; i >= 0; i--) {
      col.add(stack[i], '${stack[i]}>', SK.tag, detail: i == stack.length - 1 ? 'closes <${stack[i]}>' : 'outer tag', bonus: i == stack.length - 1 ? 50 : 20 - (stack.length - 1 - i).toDouble());
    }
    final items = col.top(8);
    return items.isEmpty ? null : Completion(tag.lt + 2, items);
  }
  if (s.startsWith('!')) {
    final col = _Col(s);
    col.add('!DOCTYPE html', '!DOCTYPE html>', SK.tag, bonus: 20);
    final items = col.top(3);
    return items.isEmpty ? null : Completion(tag.lt + 1, items);
  }
  if (!s.contains(RegExp(r'\s'))) {
    final col = _Col(s);
    final nextCh = off < t.length ? t[off] : '';
    const voidT = {'area', 'base', 'br', 'col', 'embed', 'hr', 'img', 'input', 'link', 'meta', 'param', 'source', 'track', 'wbr'};
    for (final n in _w(kHtmlTags)) {
      String ins;
      if (nextCh == '>' || nextCh == ' ') {
        ins = n;
      } else if (kHtmlTagInsert.containsKey(n)) {
        final base = kHtmlTagInsert[n]!;
        ins = voidT.contains(n) ? base : '$base§</$n>';
        if (!base.contains('§')) ins = voidT.contains(n) ? base : '$base§</$n>';
      } else if (voidT.contains(n)) {
        ins = '$n>';
      } else {
        ins = '$n>§</$n>';
      }
      col.add(n, ins, SK.tag, detail: voidT.contains(n) ? 'void tag' : 'tag', bonus: 6);
    }
    final items = col.top(14);
    return items.isEmpty ? null : Completion(tag.lt + 1, items);
  }
  final a = _attrCx(tag, off);
  final lower = a.tag.toLowerCase();
  if (a.valueOf != null || a.afterEquals) {
    final attr = (a.valueOf ?? RegExp(r'([\w:-]+)\s*=\s*$').firstMatch(s)?.group(1) ?? '').toLowerCase();
    final startVal = a.valueOf != null ? a.valueStart : off;
    var from = startVal;
    var typed = a.valueOf != null ? t.substring(startVal, off) : '';
    if (attr == 'class' && typed.contains(' ')) {
      final k = typed.lastIndexOf(' ') + 1;
      from = startVal + k;
      typed = typed.substring(k);
    }
    final col = _Col(typed);
    final quote = a.valueOf == null;
    String wrap(String v) => quote ? '"$v"' : v;
    void val(String v, SK k, String d, double b) => col.add(v, wrap(v), k, detail: d, bonus: b);
    if (attr == 'class') {
      for (final n in _projectClasses(c)) {
        val(n, SK.value, 'class in your CSS', 12);
      }
    } else if (attr == 'for' || attr == 'aria-labelledby' || attr == 'aria-controls' || attr == 'list') {
      for (final n in _projectClasses(c, ids: true)) {
        val(n, SK.value, 'id', 12);
      }
    } else if (attr == 'href' || attr == 'src' || attr == 'poster' || attr == 'action' || attr == 'srcset') {
      final exts = lower == 'link' ? const {'css', 'ico', 'png', 'webmanifest', 'json', 'svg'} : (lower == 'script' ? const {'js', 'mjs'} : (attr == 'href' ? const {'html', 'htm', 'css', 'pdf', 'zip'} : const {'png', 'jpg', 'jpeg', 'gif', 'webp', 'svg', 'ico', 'mp3', 'mp4', 'webm', 'js', 'html', 'avif'}));
      for (final p in _projectPaths(c, exts)) {
        val(p, SK.file, 'your file', 14);
      }
      if (attr == 'href' && lower == 'a') {
        for (final n in _projectClasses(c, ids: true)) {
          val('#$n', SK.value, 'id on a page', 8);
        }
        for (final v in const ['https://', 'mailto:', 'tel:', '#']) {
          val(v, SK.value, 'link', 2);
        }
      }
    } else {
      String? list = kHtmlValues[attr];
      if (attr == 'type') {
        if (lower == 'input') {
          list = kHtmlValues['type'];
        } else if (lower == 'button') {
          list = 'button submit reset';
        } else if (lower == 'script') {
          list = 'module text/javascript application/json importmap';
        } else if (lower == 'link' || lower == 'style') {
          list = 'text/css';
        } else {
          list = null;
        }
      }
      if (attr == 'name' && lower != 'meta') list = null;
      if (attr == 'content' && lower == 'meta') list = 'width=device-width, initial-scale=1';
      if (attr == 'rel' && lower != 'link' && lower != 'a') list = null;
      if (list != null) {
        for (final v in _w(list)) {
          val(v, SK.value, attr, 10);
        }
        if (attr == 'content' && lower == 'meta') col.add('width=device-width, initial-scale=1', wrap('width=device-width, initial-scale=1'), SK.value, detail: 'viewport', bonus: 10, matchOn: 'width');
      }
    }
    final items = col.top(14);
    return items.isEmpty ? null : Completion(from, items);
  }
  // attribute names
  final m = RegExp(r'[\w:@.-]*$').firstMatch(s);
  final prefix = m?.group(0) ?? '';
  if (prefix.isEmpty) return null;
  final col = _Col(prefix);
  final nextCh = off < t.length ? t[off] : '';
  final tagSpecific = _w(kHtmlAttrs[lower] ?? '');
  void addAttr(String n, double bonus, String d) {
    if (a.present.contains(n)) return;
    final isBool = _w(kHtmlBoolAttrs).contains(n);
    String ins;
    if (n.endsWith('-')) {
      ins = '$n§=""';
    } else if (isBool || nextCh == '=') {
      ins = n;
    } else {
      ins = '$n="§"';
    }
    col.add(n.endsWith('-') ? '${n}*' : n, ins, SK.attr, detail: d, bonus: bonus, matchOn: n);
  }

  for (final n in tagSpecific) {
    addAttr(n, 16, '<$lower>');
  }
  for (final n in _w(kHtmlGlobalAttrs)) {
    addAttr(n, n.startsWith('on') ? 0 : 6, 'global');
  }
  final items = col.top(14);
  return items.isEmpty ? null : Completion(off - prefix.length, items);
}

// ------------------------------------------------------------------ CSS

/// [text] with comments and the insides of strings blanked out (same length).
String _blankCss(String t) {
  final b = StringBuffer();
  var i = 0;
  while (i < t.length) {
    final c = t[i];
    if (c == '/' && i + 1 < t.length && t[i + 1] == '*') {
      final e = t.indexOf('*/', i + 2);
      final end = e < 0 ? t.length : e + 2;
      b.write(' ' * (end - i));
      i = end;
      continue;
    }
    if (c == '"' || c == "'") {
      var j = i + 1;
      while (j < t.length && t[j] != c && t[j] != '\n') {
        if (t[j] == '\\') j++;
        j++;
      }
      final end = j < t.length ? j + 1 : t.length;
      b.write(c);
      b.write(' ' * (end - i - 2 < 0 ? 0 : end - i - 2));
      if (end - i >= 2) b.write(c);
      i = end;
      continue;
    }
    b.write(c);
    i++;
  }
  return b.toString();
}

Completion? _css(_Cx c, String text, int off, int shift) {
  if (off > text.length) return null;
  final blank = _blankCss(text.length > 400000 ? text.substring(0, 400000) : text);
  if (off > blank.length) return null;
  // comment or string at the cursor?
  final lastOpenC = text.lastIndexOf('/*', off > 0 ? off - 1 : 0);
  if (lastOpenC >= 0 && lastOpenC < off) {
    final e = text.indexOf('*/', lastOpenC + 2);
    if (e < 0 || e + 2 > off) return null;
  }
  final opens = <int>[];
  for (var i = 0; i < off; i++) {
    final ch = blank[i];
    if (ch == '{') {
      opens.add(i);
    } else if (ch == '}') {
      if (opens.isNotEmpty) opens.removeLast();
    }
  }
  int lastSep(int from, int to) {
    var r = -1;
    for (var i = to - 1; i >= from; i--) {
      final ch = blank[i];
      if (ch == '{' || ch == '}' || ch == ';') {
        r = i;
        break;
      }
    }
    return r;
  }

  final depth = opens.length;
  final boundary = lastSep(0, off);
  final seg = blank.substring(boundary + 1, off);
  var selector = depth == 0;
  if (!selector) {
    final pre = blank.substring(lastSep(0, opens.last) + 1, opens.last).trim();
    if (pre.startsWith('@') && !pre.startsWith('@font-face') && !pre.startsWith('@page') && !pre.startsWith('@property')) selector = true;
    final st = seg.trimLeft();
    if (st.isNotEmpty && '&.#>+~@:[*'.contains(st[0]) && !st.contains(':') || (st.startsWith('&') || st.startsWith('.') || st.startsWith('#') || st.startsWith('@'))) {
      if (!(st.contains(':') && !st.startsWith('&') && !st.startsWith('.') && !st.startsWith('#') && !st.startsWith('@') && !st.startsWith(':'))) selector = true;
    }
  }
  final classes = () => _projectClasses(c);
  if (selector) {
    final m = RegExp(r'(?:::?|[.#@])?[\w-]*$').firstMatch(seg);
    final prefix = m?.group(0) ?? '';
    final col = _Col(prefix);
    final start = off - prefix.length;
    if (prefix.startsWith('.')) {
      for (final n in classes()) {
        col.add('.$n', '.$n', SK.value, detail: 'class', bonus: 12);
      }
    } else if (prefix.startsWith('#')) {
      for (final n in _projectClasses(c, ids: true)) {
        col.add('#$n', '#$n', SK.value, detail: 'id', bonus: 12);
      }
    } else if (prefix.startsWith('::')) {
      for (final n in _w(kCssPseudoEl)) {
        col.add('::$n', '::$n', SK.value, detail: 'pseudo-element', bonus: 10);
      }
    } else if (prefix.startsWith(':')) {
      for (final n in _w(kCssPseudo)) {
        final fn = n.endsWith('(');
        col.add(':${fn ? n.substring(0, n.length - 1) : n}', fn ? ':${n}§)' : ':$n', SK.value, detail: 'pseudo-class', bonus: 10);
      }
      for (final n in _w(kCssPseudoEl)) {
        col.add('::$n', '::$n', SK.value, detail: 'pseudo-element', bonus: 4);
      }
    } else if (prefix.startsWith('@')) {
      if (depth == 0 || true) {
        const snips = <String, String>{
          'media': '@media (max-width: 600px) {\n  §\n}',
          'import': '@import url("§");',
          'font-face': '@font-face {\n  font-family: "§";\n  src: url("");\n}',
          'keyframes': '@keyframes § {\n  from {\n    \n  }\n  to {\n    \n  }\n}',
          'supports': '@supports (display: grid) {\n  §\n}',
          'layer': '@layer §;',
          'container': '@container (min-width: 400px) {\n  §\n}',
          'charset': '@charset "UTF-8";',
        };
        for (final n in _w(kCssAt)) {
          col.add('@$n', snips[n] ?? '@$n §', SK.keyword, detail: 'at-rule', bonus: 10);
        }
      }
    } else {
      for (final n in _w(kHtmlTags)) {
        col.add(n, n, SK.tag, detail: 'element', bonus: 4);
      }
      for (final n in classes()) {
        col.add('.$n', '.$n', SK.value, detail: 'class', bonus: 0, matchOn: n);
      }
      col.add(':root', ':root', SK.keyword, bonus: 6);
      col.add('*', '*', SK.keyword, bonus: -20);
    }
    if (prefix.isEmpty) return null;
    final items = col.top(14);
    return items.isEmpty ? null : Completion(start + shift, items);
  }
  final colon = seg.indexOf(':');
  final next = off < text.length ? text[off] : '';
  if (colon < 0) {
    final m = RegExp(r'[\w-]*$').firstMatch(seg);
    final prefix = m?.group(0) ?? '';
    if (prefix.length < 2 && !prefix.startsWith('-')) return null;
    final col = _Col(prefix);
    for (final p in _w(kCssProps)) {
      col.add(p, next == ':' ? p : '$p: §;', SK.attr, detail: 'property', bonus: 10);
    }
    for (final p in _customProps(c, text)) {
      col.add(p, next == ':' ? p : '$p: §;', SK.attr, detail: 'custom property', bonus: 6);
    }
    final items = col.top(14);
    return items.isEmpty ? null : Completion(off - prefix.length + shift, items);
  }
  // value
  final propM = RegExp(r'([\w-]+)\s*$').firstMatch(seg.substring(0, colon));
  final prop = propM?.group(1) ?? '';
  final valSeg = seg.substring(colon + 1);
  final m = RegExp(r'(?:var\(\s*)?[\w#%.-]*$').firstMatch(valSeg);
  var prefix = m?.group(0) ?? '';
  var inVar = false;
  if (prefix.startsWith('var(')) {
    inVar = true;
    prefix = prefix.substring(4).trimLeft();
  }
  final col = _Col(prefix);
  final start = off - prefix.length;
  if (prop.startsWith('--') && valSeg.trim().isEmpty) return null;
  final props = _customProps(c, text);
  if (inVar || prefix.startsWith('--')) {
    for (final p in props) {
      col.add(p, p, SK.value, detail: 'custom property', bonus: 20);
    }
    final items = col.top(12);
    return items.isEmpty ? null : Completion(start + shift, items);
  }
  if (prefix.isEmpty && valSeg.trim().isNotEmpty && !valSeg.trimRight().endsWith(',')) {
    // between two values: only offer when the property has a list of values
  }
  final list = kCssValues[prop];
  if (list != null) {
    for (final v in _w(list)) {
      if (v.endsWith('(')) {
        col.add('${v.substring(0, v.length - 1)}()', '$v§)', SK.method, detail: prop, bonus: 12, matchOn: v.substring(0, v.length - 1));
      } else {
        col.add(v, v, SK.value, detail: prop, bonus: 14);
      }
    }
  }
  final colorish = prop.contains('color') || const {'background', 'border', 'outline', 'fill', 'stroke', 'box-shadow', 'text-shadow', 'border-top', 'border-bottom', 'border-left', 'border-right', 'caret-color', 'accent-color'}.contains(prop);
  if (colorish) {
    for (final v in _w(kCssColors)) {
      col.add(v, v, SK.value, detail: 'colour', bonus: 8);
    }
    for (final p in props) {
      col.add('var($p)', 'var($p)', SK.value, detail: 'custom property', bonus: 14, matchOn: p);
    }
  } else if (props.isNotEmpty && prefix.isNotEmpty) {
    for (final p in props) {
      col.add('var($p)', 'var($p)', SK.value, detail: 'custom property', bonus: 6, matchOn: p);
    }
  }
  for (final v in _w(kCssFuncs)) {
    final name = v.contains('(') ? v.substring(0, v.indexOf('(')) : v;
    col.add('$name()', v.contains('§') ? v : '$v§)', SK.method, detail: 'function', bonus: 3, matchOn: name);
  }
  for (final v in _w(kCssGlobal)) {
    col.add(v, v, SK.value, detail: 'any property', bonus: 0);
  }
  if (RegExp(r'^-?\d*\.?\d+$').hasMatch(prefix) && prefix.isNotEmpty) {
    for (final u in _w(kCssUnits)) {
      col.add('$prefix$u', '$prefix$u', SK.value, detail: 'unit', bonus: 30);
    }
    final items = col.top(12);
    return items.isEmpty ? null : Completion(start + shift, items);
  }
  if (prefix.isEmpty) return null;
  final items = col.top(14);
  return items.isEmpty ? null : Completion(start + shift, items);
}

Set<String> _customProps(_Cx c, String localText) {
  final out = <String>{};
  final re = RegExp(r'(--[\w-]+)\s*:');
  for (final m in re.allMatches(localText)) {
    out.add(m.group(1)!);
  }
  for (final f in _idx.values) {
    if (f.path == c.path) continue;
    if (f.ext != 'css' && f.ext != 'scss' && f.ext != 'html' && f.ext != 'htm') continue;
    final t = f.text;
    if (t == null) continue;
    for (final m in re.allMatches(t)) {
      out.add(m.group(1)!);
    }
  }
  return out;
}

// ------------------------------------------------------------------ Android XML

String _xmlKind(String path) {
  if (path.endsWith('AndroidManifest.xml')) return 'manifest';
  if (RegExp(r'(?:^|/)res/layout[^/]*/').hasMatch(path) || RegExp(r'(?:^|/)layout[^/]*/').hasMatch(path)) return 'layout';
  if (RegExp(r'(?:^|/)res/values[^/]*/').hasMatch(path)) return 'values';
  if (RegExp(r'(?:^|/)res/menu/').hasMatch(path)) return 'menu';
  if (RegExp(r'(?:^|/)res/drawable[^/]*/').hasMatch(path)) return 'drawable';
  return 'other';
}

const _containers = {'LinearLayout', 'RelativeLayout', 'FrameLayout', 'ScrollView', 'HorizontalScrollView', 'NestedScrollView', 'TableLayout', 'TableRow', 'GridLayout', 'RadioGroup', 'merge'};

String _xmlTagInsert(String kind, String n) {
  final short = n.contains('.') ? n.substring(n.lastIndexOf('.') + 1) : n;
  if (kind == 'layout') {
    if (n == 'include') return 'include layout="@layout/§" />';
    if (n == 'merge') return 'merge xmlns:android="http://schemas.android.com/apk/res/android">\n    §\n</merge>';
    final isContainer = _containers.contains(n) || short == 'ConstraintLayout' || short == 'CoordinatorLayout' || short == 'CardView' || short == 'MaterialCardView' || short == 'DrawerLayout' || short == 'SwipeRefreshLayout' || short == 'AppBarLayout' || short == 'RadioGroup' || short == 'ChipGroup';
    if (isContainer) {
      return '$n\n    android:layout_width="match_parent"\n    android:layout_height="${n == 'ScrollView' || n == 'LinearLayout' ? 'wrap_content' : 'match_parent'}"${n == 'LinearLayout' ? '\n    android:orientation="vertical"' : ''}>\n    §\n</$n>';
    }
    final text = const {'TextView', 'Button', 'CheckBox', 'RadioButton', 'Switch', 'ToggleButton', 'EditText'}.contains(short) || short == 'MaterialButton' || short == 'AppCompatTextView' || short == 'AppCompatButton' || short == 'SwitchMaterial';
    final w = (short == 'RecyclerView' || short == 'ListView' || short == 'WebView' || short == 'ViewPager2' || short == 'ProgressBar' && false) ? 'match_parent' : 'wrap_content';
    final h = (short == 'RecyclerView' || short == 'ListView' || short == 'WebView' || short == 'ViewPager2') ? 'match_parent' : 'wrap_content';
    final extra = text ? '\n    android:text="§"' : (short == 'ImageView' || short == 'AppCompatImageView' ? '\n    android:src="§"\n    android:contentDescription="@null"' : '');
    return '$n\n    android:layout_width="$w"\n    android:layout_height="$h"$extra />';
  }
  if (kind == 'manifest') {
    switch (n) {
      case 'uses-permission':
        return 'uses-permission android:name="android.permission.§" />';
      case 'activity':
        return 'activity\n    android:name=".§"\n    android:exported="false" />';
      case 'service':
        return 'service\n    android:name=".§"\n    android:exported="false" />';
      case 'receiver':
        return 'receiver\n    android:name=".§"\n    android:exported="false" />';
      case 'intent-filter':
        return 'intent-filter>\n    <action android:name="android.intent.action.§" />\n</intent-filter>';
      case 'action':
        return 'action android:name="§" />';
      case 'category':
        return 'category android:name="§" />';
      case 'meta-data':
        return 'meta-data\n    android:name="§"\n    android:value="" />';
      case 'uses-feature':
        return 'uses-feature android:name="§" android:required="false" />';
      case 'data':
        return 'data android:scheme="§" />';
      case 'provider':
        return 'provider\n    android:name="§"\n    android:authorities=""\n    android:exported="false" />';
    }
    return '$n>§</$n>';
  }
  if (kind == 'values') {
    switch (n) {
      case 'string':
        return 'string name="§"></string>';
      case 'color':
        return 'color name="§">#</color>';
      case 'dimen':
        return 'dimen name="§">16dp</dimen>';
      case 'style':
        return 'style name="§" parent="">\n    <item name=""></item>\n</style>';
      case 'item':
        return 'item name="§"></item>';
      case 'integer':
        return 'integer name="§">0</integer>';
      case 'bool':
        return 'bool name="§">true</bool>';
      case 'string-array':
        return 'string-array name="§">\n    <item></item>\n</string-array>';
    }
  }
  if (kind == 'menu' && n == 'item') return 'item\n    android:id="@+id/§"\n    android:title="" />';
  return '$n>§</$n>';
}

Completion? _xml(_Cx c) {
  final t = c.text;
  final off = c.off;
  final kind = _xmlKind(c.path);
  final tag = _tagAt(t, off);
  if (tag == null) return null;
  final s = tag.inner;
  if (s.startsWith('/')) {
    final prefix = s.substring(1);
    if (prefix.contains(RegExp(r'\s'))) return null;
    final stack = _openStack(t, tag.lt);
    final col = _Col(prefix);
    for (var i = stack.length - 1; i >= 0; i--) {
      col.add(stack[i], '${stack[i]}>', SK.tag, detail: i == stack.length - 1 ? 'closes <${stack[i]}>' : 'outer tag', bonus: i == stack.length - 1 ? 50 : 20 - (stack.length - 1 - i).toDouble());
    }
    final items = col.top(8);
    return items.isEmpty ? null : Completion(tag.lt + 2, items);
  }
  if (s.startsWith('?') || s.startsWith('!')) return null;
  if (!s.contains(RegExp(r'\s'))) {
    final col = _Col(s);
    String list;
    switch (kind) {
      case 'layout':
        list = kXmlViews;
        break;
      case 'manifest':
        list = kXmlManifestTags;
        break;
      case 'values':
        list = kXmlValuesTags;
        break;
      case 'menu':
        list = kXmlMenuTags;
        break;
      default:
        list = '$kXmlViews $kXmlValuesTags';
    }
    final nextCh = off < t.length ? t[off] : '';
    for (final n in _w(list)) {
      final short = n.contains('.') ? n.substring(n.lastIndexOf('.') + 1) : n;
      col.add(n, (nextCh == '>' || nextCh == ' ') ? n : _xmlTagInsert(kind, n), SK.tag, detail: n.contains('.') ? n.substring(0, n.lastIndexOf('.')) : 'tag', bonus: n.contains('.') ? 4 : 8, matchOn: short == n ? n : '$short $n');
    }
    final items = col.top(14);
    return items.isEmpty ? null : Completion(tag.lt + 1, items);
  }
  final a = _attrCx(tag, off);
  if (a.valueOf != null || a.afterEquals) return _xmlValue(c, tag, a, kind);
  // attribute names
  final m = RegExp(r'[\w:.-]*$').firstMatch(s);
  final prefix = m?.group(0) ?? '';
  if (prefix.isEmpty) return null;
  final col = _Col(prefix);
  final nextCh = off < t.length ? t[off] : '';
  final fileHasApp = t.contains('xmlns:app=');
  final fileHasTools = t.contains('xmlns:tools=');
  final list = kind == 'manifest' ? kXmlManifestAttrs : kXmlAttrs;
  for (final n in _w(list)) {
    if (a.present.contains(n)) continue;
    final local = n.contains(':') ? n.substring(n.lastIndexOf(':') + 1) : n;
    String? ns;
    if (n.startsWith('app:') && !fileHasApp) ns = 'xmlns:app="http://schemas.android.com/apk/res-auto"';
    if (n.startsWith('tools:') && !fileHasTools) ns = 'xmlns:tools="http://schemas.android.com/tools"';
    // match on the full name and on the part after "android:"
    final m1 = _score(prefix, n), m2 = _score(prefix, local);
    if (m1 <= 0 && m2 <= 0) continue;
    final base = (m1 > m2 ? m1 : m2 * 0.97);
    final isLayout = n.startsWith('android:layout_') || n == 'android:id' || n == 'android:text';
    final sug = Sug(n, nextCh == '=' ? n : '$n="§"', SK.attr, detail: ns != null ? 'adds ${n.split(':').first} namespace' : 'attribute', ns: ns);
    sug.score = base + (isLayout ? 12 : 4);
    col.best[n] = sug;
  }
  final items = col.top(14);
  return items.isEmpty ? null : Completion(off - prefix.length, items);
}

Completion? _xmlValue(_Cx c, _Tag tag, _AttrCx a, String kind) {
  final t = c.text;
  final off = c.off;
  final attr = a.valueOf ?? RegExp(r'([\w:.-]+)\s*=\s*$').firstMatch(tag.inner)?.group(1) ?? '';
  final quote = a.valueOf == null;
  var from = a.valueOf != null ? a.valueStart : off;
  var typed = a.valueOf != null ? t.substring(from, off) : '';
  const flagAttrs = {'android:gravity', 'android:layout_gravity', 'android:inputType', 'android:textStyle', 'android:configChanges'};
  if (flagAttrs.contains(attr) && typed.contains('|')) {
    final k = typed.lastIndexOf('|') + 1;
    from += k;
    typed = typed.substring(k);
  }
  final col = _Col(typed);
  String wrap(String v) => quote ? '"$v"' : v;
  void add(String v, SK k, String d, double b) => col.add(v, wrap(v), k, detail: d, bonus: b);
  final local = attr.contains(':') ? attr.substring(attr.lastIndexOf(':') + 1) : attr;
  final tagName = tag.inner.isEmpty ? '' : (RegExp(r'^[\w:.-]+').firstMatch(tag.inner)?.group(0) ?? '');

  void res(String k, double b, {String prefix = '@'}) {
    for (final n in _resNames(k, c)) {
      add('$prefix$k/$n', SK.resource, '@$k', b);
    }
  }

  if (attr == 'android:id') {
    add('@+id/', SK.resource, 'new id', 20);
    col.add('@+id/…', wrap('@+id/§'), SK.resource, detail: 'new id', bonus: 20, matchOn: '@+id/');
  } else if (local.endsWith('_toTopOf') || local.endsWith('_toBottomOf') || local.endsWith('_toStartOf') || local.endsWith('_toEndOf') || local.endsWith('_toLeftOf') || local.endsWith('_toRightOf') ||
      local.startsWith('layout_align') || local == 'layout_below' || local == 'layout_above' || local == 'labelFor' || local == 'layout_anchor') {
    if (attr.startsWith('app:')) add('parent', SK.value, 'the parent view', 24);
    res('id', 18);
  } else if (const {'text', 'hint', 'contentDescription', 'title', 'label', 'summary', 'description', 'prompt'}.contains(local)) {
    res('string', 16);
  } else if (local.toLowerCase().contains('color') || local == 'background' && false || const {'tint', 'backgroundTint', 'strokeColor', 'cardBackgroundColor', 'itemIconTint', 'itemTextColor', 'drawableTint'}.contains(local)) {
    res('color', 16);
    for (final v in const ['@android:color/white', '@android:color/black', '@android:color/transparent', '#FFFFFF', '#000000']) {
      add(v, SK.value, 'colour', 3);
    }
  } else if (const {'src', 'srcCompat', 'icon', 'navigationIcon', 'drawableStart', 'drawableEnd', 'drawableTop', 'drawableBottom', 'drawableLeft', 'drawableRight', 'startIconDrawable', 'foreground', 'background', 'thumb', 'divider', 'logo'}.contains(local)) {
    res('drawable', 16);
    res('mipmap', 12);
    if (local == 'background' || local == 'foreground') res('color', 14);
  } else if (local == 'layout' || local == 'listitem' || local == 'headerLayout') {
    res('layout', 16);
  } else if (local == 'menu') {
    res('menu', 16);
  } else if (local == 'style' || local == 'theme') {
    res('style', 16);
    for (final v in const ['@style/Theme.AppCompat.Light.NoActionBar', '@style/Theme.MaterialComponents.DayNight.NoActionBar']) {
      add(v, SK.resource, 'theme', 4);
    }
  } else if (local == 'fontFamily') {
    res('font', 16);
    for (final v in const ['sans-serif', 'sans-serif-medium', 'sans-serif-light', 'monospace', 'serif']) {
      add(v, SK.value, 'font', 6);
    }
  } else if (local == 'entries') {
    res('array', 16);
  } else if (local == 'onClick') {
    for (final f in _idx.values) {
      if (f.lang != 'kt' && f.lang != 'java') continue;
      final src = f.text;
      if (src == null) continue;
      for (final m in RegExp(r'(?:fun\s+(\w+)\s*\(\s*\w+\s*:\s*View\s*\?*\s*\)|void\s+(\w+)\s*\(\s*View\s+\w+\s*\))').allMatches(src)) {
        add(m.group(1) ?? m.group(2)!, SK.method, 'in ${f.path.split('/').last}', 16);
      }
    }
  } else if (attr == 'tools:context') {
    for (final f in _idx.values) {
      if (f.lang != 'kt' && f.lang != 'java') continue;
      for (final s in f.syms) {
        if (s.kind == SK.cls && s.name.endsWith('Activity')) add('.${s.name}', SK.cls, f.path.split('/').last, 16);
      }
    }
  } else if (kind == 'manifest' && attr == 'android:name') {
    if (tagName == 'uses-permission' || tagName == 'permission') {
      for (final p in _w(kXmlPermissions)) {
        add('android.permission.$p', SK.value, 'permission', 14);
      }
    } else if (tagName == 'action' || tagName == 'category') {
      for (final p in _w(kXmlActions)) {
        add(p, SK.value, tagName, 14);
      }
    } else if (tagName == 'activity' || tagName == 'service' || tagName == 'receiver' || tagName == 'provider' || tagName == 'activity-alias') {
      final pkg = RegExp(r'package\s*=\s*"([\w.]+)"').firstMatch(t)?.group(1) ?? '';
      for (final f in _idx.values) {
        if (f.lang != 'kt' && f.lang != 'java') continue;
        for (final s in f.syms) {
          if (s.kind != SK.cls) continue;
          final okName = tagName == 'activity' ? s.name.endsWith('Activity') : (tagName == 'service' ? s.name.endsWith('Service') : (tagName == 'receiver' ? s.name.endsWith('Receiver') : true));
          if (!okName) continue;
          final fp = f.pkg;
          add(fp.isNotEmpty && fp == pkg ? '.${s.name}' : (fp.isEmpty ? '.${s.name}' : '$fp.${s.name}'), SK.cls, f.path.split('/').last, 16);
        }
      }
    }
  } else if (RegExp(r'^(?:android:)?(?:layout_(?:margin|padding)|padding|margin)').hasMatch(local) || local.startsWith('layout_margin') || local.startsWith('padding') || local == 'layout_marginTop') {
    for (final v in const ['0dp', '2dp', '4dp', '8dp', '12dp', '16dp', '20dp', '24dp', '32dp', '48dp']) {
      add(v, SK.value, 'size', 8);
    }
    res('dimen', 12);
  } else if (local == 'textSize') {
    for (final v in const ['12sp', '14sp', '16sp', '18sp', '20sp', '24sp', '32sp']) {
      add(v, SK.value, 'text size', 8);
    }
    res('dimen', 12);
  } else if (attr == 'android:layout_width' || attr == 'android:layout_height') {
    res('dimen', 6);
  } else if (local == 'layoutManager') {
    for (final v in const ['androidx.recyclerview.widget.LinearLayoutManager', 'androidx.recyclerview.widget.GridLayoutManager', 'androidx.recyclerview.widget.StaggeredGridLayoutManager']) {
      add(v, SK.cls, 'layout manager', 12);
    }
  } else if (local == 'name' && kind == 'values') {
    // naming something new: nothing to offer
  }
  final list = kXmlValueMap[attr];
  if (list != null) {
    for (final v in _w(list)) {
      add(v, SK.value, local, 14);
    }
  }
  if (attr == 'android:permission') {
    for (final p in _w(kXmlPermissions)) {
      add('android.permission.$p', SK.value, 'permission', 10);
    }
  }
  final items = col.top(14);
  return items.isEmpty ? null : Completion(from, items);
}

// ------------------------------------------------------------------ accepting a suggestion

(String, int)? _addImport(String t, String imp, String ext) {
  final lang = _langOf(ext);
  String line;
  if (lang == 'dart') {
    line = imp;
  } else if (lang == 'java') {
    line = 'import $imp;';
  } else {
    line = 'import $imp';
  }
  final imports = RegExp(r'^\s*import\s[^\n]*$', multiLine: true).allMatches(t).toList();
  if (imports.isNotEmpty) {
    // keep them in order: after the last import that sorts before this one
    final at = imports.last.end;
    final ins = '\n$line';
    return ('${t.substring(0, at)}$ins${t.substring(at)}', ins.length);
  }
  final pk = RegExp(r'^\s*package\s[^\n]*$', multiLine: true).firstMatch(t);
  if (pk != null) {
    final ins = '\n\n$line';
    final after = t.substring(pk.end);
    final trimmedBlank = after.startsWith('\n\n') ? '' : '\n';
    return ('${t.substring(0, pk.end)}$ins$trimmedBlank$after', ins.length + trimmedBlank.length);
  }
  final ins = '$line\n${t.startsWith('\n') ? '' : '\n'}';
  return ('$ins$t', ins.length);
}

/// Puts [s] into [text] in place of everything from [start] to [off].
Applied applySuggestion(String text, int off, int start, Sug s, String path) {
  final ext = _extOf(path);
  var ins = s.insert;
  final next = off < text.length ? text[off] : '';
  if (next == '(' && ins.contains('(§)')) ins = ins.replaceFirst('(§)', '§');
  if (next == ':' && ins.endsWith(': §,')) ins = ins.substring(0, ins.length - 4);
  final ls = start == 0 ? 0 : text.lastIndexOf('\n', start - 1) + 1;
  final lead = RegExp(r'^[ \t]*').stringMatch(text.substring(ls, start)) ?? '';
  ins = ins.replaceAll('\n', '\n$lead');
  var caret = ins.indexOf('§');
  ins = ins.replaceAll('§', '');
  if (caret < 0) caret = ins.length;
  var t = text.replaceRange(start, off, ins);
  var c = start + caret;
  if (s.imp != null) {
    final r = _addImport(t, s.imp!, ext);
    if (r != null) {
      t = r.$1;
      c += r.$2;
    }
  }
  if (s.ns != null && !t.contains(s.ns!.split('=').first)) {
    final root = RegExp(r'<(?![?!/])[A-Za-z]').firstMatch(t);
    if (root != null) {
      String? q;
      var i = root.end;
      var end = -1;
      for (; i < t.length; i++) {
        final ch = t[i];
        if (q != null) {
          if (ch == q) q = null;
        } else if (ch == '"' || ch == "'") {
          q = ch;
        } else if (ch == '>') {
          end = i;
          break;
        }
      }
      if (end > 0) {
        var pos = t[end - 1] == '/' ? end - 1 : end;
        while (pos > 0 && (t[pos - 1] == ' ')) {
          pos--;
        }
        final add = '\n    ${s.ns}';
        t = '${t.substring(0, pos)}$add${t.substring(pos)}';
        if (pos <= c) c += add.length;
      }
    }
  }
  return Applied(t, c);
}
