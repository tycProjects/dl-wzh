// Prettier-style "Format document" for the code editor.
//
// Pure Dart (no Flutter), so it can also run in a background isolate.
//
// Safety net: every formatter only ever changes spaces, line breaks, quote style and (CSS) a missing
// last ";". After formatting, [formatSource] compares the text with all of those removed. If anything
// else differs, it throws instead of returning, so a bug here can never damage your code.

import 'dart:convert';

const _jsLike = {'js', 'mjs', 'cjs', 'ts'};
const _braceExts = {'kt', 'kts', 'java', 'dart', 'gradle', 'groovy', 'js', 'mjs', 'cjs', 'ts'};

String? _kind(String ext) {
  if (_braceExts.contains(ext)) return 'brace';
  if (ext == 'css') return 'css';
  if (ext == 'scss') return 'scss';
  if (ext == 'json' || ext == 'jsonc' || ext == 'json5' || ext == 'prettierrc') return 'json';
  if (ext == 'html' || ext == 'htm') return 'html';
  if (ext == 'xml' || ext == 'svg') return 'xml';
  if (ext == 'yaml' || ext == 'yml' || ext == 'properties' || ext == 'toml') return 'basic';
  return null;
}

/// True when [ext] (file extension without the dot) has a formatter.
bool canFormat(String ext) => _kind(ext) != null;

/// Prettier-style options (the same ideas as a .prettierrc file).
class FormatOptions {
  /// Spaces per indent level. null = what the usual style guide uses for that file type.
  final int? tabWidth;

  /// JavaScript and TypeScript strings: 'single' instead of \"double\" when it needs no escaping.
  final bool singleQuote;

  /// Where HTML and XML start putting attributes and text on their own lines.
  final int printWidth;
  const FormatOptions({this.tabWidth, this.singleQuote = false, this.printWidth = 80});

  FormatOptions copy({int? tabWidth, bool? singleQuote, int? printWidth, bool autoTab = false}) => FormatOptions(
        tabWidth: autoTab ? null : (tabWidth ?? this.tabWidth),
        singleQuote: singleQuote ?? this.singleQuote,
        printWidth: printWidth ?? this.printWidth,
      );
}

FormatOptions _opts = const FormatOptions();

/// Reads the text of a .prettierrc / .prettierrc.json on top of [base]. Returns null when it isn't JSON
/// or has nothing we use (tabWidth, singleQuote, printWidth).
FormatOptions? parsePrettierrc(String text, FormatOptions base) {
  try {
    final j = jsonDecode(text);
    if (j is! Map) return null;
    var o = base;
    var used = false;
    final tw = j['tabWidth'];
    if (tw is int && tw >= 1 && tw <= 8) {
      o = o.copy(tabWidth: tw);
      used = true;
    }
    final sq = j['singleQuote'];
    if (sq is bool) {
      o = o.copy(singleQuote: sq);
      used = true;
    }
    final pw = j['printWidth'];
    if (pw is int && pw >= 40 && pw <= 200) {
      o = o.copy(printWidth: pw);
      used = true;
    }
    return used ? o : null;
  } catch (_) {
    return null;
  }
}

/// The usual spaces per indent level for that file type.
int defaultIndent(String ext) => const {'kt', 'kts', 'java', 'gradle', 'groovy', 'xml'}.contains(ext) ? 4 : 2;

/// Spaces per indent level: the chosen tab width, else what the usual style guide uses for that file type.
int indentSize(String ext) => _opts.tabWidth ?? defaultIndent(ext);

String _sig(String s) => s.replaceAll(RegExp(r'''[\s'";]'''), '');

/// Formats [text]. Throws [FormatException] (with a message to show the person) when it can't.
String formatSource(String text, String ext, {FormatOptions? opts}) {
  final before = _opts;
  _opts = opts ?? const FormatOptions();
  try {
    return _formatSource(text, ext);
  } finally {
    _opts = before;
  }
}

String _formatSource(String text, String ext) {
  final kind = _kind(ext);
  if (kind == null) throw FormatException('Formatting isn\u2019t available for .$ext files yet.');
  final src = text.replaceAll('\r\n', '\n').replaceAll('\r', '\n');
  if (src.trim().isEmpty) return '';
  String out;
  switch (kind) {
    case 'brace':
      out = _fmtBrace(src, ext);
      break;
    case 'css':
      out = _fmtCss(src, false);
      break;
    case 'scss':
      out = _fmtCss(src, true);
      break;
    case 'json':
      try {
        out = _fmtJson(src);
      } on FormatException catch (e) {
        if (ext == 'json') throw FormatException('Not valid JSON: ${e.message}');
        out = _fmtBrace(src, 'js');
      }
      break;
    case 'html':
      out = _fmtMarkup(src, true);
      break;
    case 'xml':
      out = _fmtMarkup(src, false);
      break;
    default:
      out = _fmtBasic(src);
  }
  if (_sig(out) != _sig(src)) {
    throw const FormatException('Formatting would have changed your code, so nothing was changed.');
  }
  return out;
}

// ------------------------------------------------------------------ shared lexing helpers

bool _isWordStart(int c) => (c >= 65 && c <= 90) || (c >= 97 && c <= 122) || c == 95 || c == 36 || c > 127;
bool _isWordPart(int c) => _isWordStart(c) || (c >= 48 && c <= 57);
bool _isDigit(int c) => c >= 48 && c <= 57;
bool _isWs(String c) => c == ' ' || c == '\t' || c == '\n' || c == '\r';

/// Index just after the string literal that starts at [i] (handles ${...} nesting, triple quotes, raw strings).
int skipCodeString(String t, int i, String ext, {bool raw = false}) {
  final n = t.length;
  final q = t[i];
  final kt = ext == 'kt' || ext == 'kts';
  final groovy = ext == 'gradle' || ext == 'groovy';
  final dart = ext == 'dart';
  final tri = (kt || groovy || dart || ext == 'java') && q != '`' && t.startsWith(q * 3, i);
  final interp = !raw && ((q == '"' && (kt || groovy || dart)) || (q == "'" && dart) || q == '`');
  var j = i + (tri ? 3 : 1);
  while (j < n) {
    final c = t[j];
    if (c == '\\' && !raw) {
      j += 2;
      continue;
    }
    if (tri) {
      if (t.startsWith(q * 3, j)) return j + 3;
    } else if (c == q) {
      return j + 1;
    } else if (c == '\n' && q != '`') {
      return j;
    }
    if (interp && c == r'$' && j + 1 < n && t[j + 1] == '{') {
      j = _skipInterp(t, j + 2, ext);
      continue;
    }
    j++;
  }
  return n;
}

int _skipInterp(String t, int j, String ext) {
  final n = t.length;
  var d = 1;
  while (j < n && d > 0) {
    final c = t[j];
    if (c == '{') {
      d++;
    } else if (c == '}') {
      d--;
    } else if (c == '"' || c == "'" || c == '`') {
      j = skipCodeString(t, j, ext);
      continue;
    }
    j++;
  }
  return j;
}

// ------------------------------------------------------------------ brace languages (Kotlin, Java, Dart, JS/TS, Gradle)

class _T {
  final int k; // 0 word, 1 number, 2 string, 3 line comment, 4 block comment, 5 punctuation, 6 regex
  final String s;
  final int nl; // line breaks before it
  final bool sp; // whitespace before it on the same line
  final int col; // column where it started
  _T(this.k, this.s, this.nl, this.sp, this.col);
}

const _ops4 = ['>>>=', '===', '!==', '>>>', '<<=', '>>=', '??=', '||=', '&&=', '...', '..<'];
const _ops2 = ['?.', '=>', '->', '::', '==', '!=', '<=', '>=', '&&', '||', '??', '++', '--', '+=', '-=', '*=', '/=', '%=', '&=', '|=', '^=', '<<', '>>', '..', '!!', '?:'];
const _kotlinTargets = {'get', 'set', 'field', 'param', 'property', 'receiver', 'file', 'setparam', 'delegate'};
const _regexKw = {'return', 'typeof', 'case', 'in', 'of', 'delete', 'void', 'throw', 'new', 'else', 'do', 'yield', 'await'};

bool _regexOk(_T? p) {
  if (p == null) return true;
  if (p.k == 5) return !(p.s == ')' || p.s == ']' || p.s == '}' || p.s == '++' || p.s == '--');
  if (p.k == 0) return _regexKw.contains(p.s);
  return false;
}

List<_T> _lex(String t, String ext) {
  final js = _jsLike.contains(ext);
  final kt = ext == 'kt' || ext == 'kts';
  final dart = ext == 'dart';
  final out = <_T>[];
  final n = t.length;
  var i = 0, nl = 0, lineStart = 0;
  var sp = false;
  while (i < n) {
    final cu = t.codeUnitAt(i);
    if (cu == 10) {
      nl++;
      sp = false;
      i++;
      lineStart = i;
      continue;
    }
    if (cu == 32 || cu == 9 || cu == 13) {
      sp = true;
      i++;
      continue;
    }
    final start = i;
    final col = i - lineStart;
    final c = t[i];
    var kind = 5;
    if (c == '/' && i + 1 < n && t[i + 1] == '/') {
      var e = t.indexOf('\n', i);
      if (e < 0) e = n;
      i = e;
      kind = 3;
    } else if (c == '/' && i + 1 < n && t[i + 1] == '*') {
      final e = t.indexOf('*/', i + 2);
      i = e < 0 ? n : e + 2;
      kind = 4;
    } else if (c == '"' || c == "'" || (c == '`' && js)) {
      i = skipCodeString(t, i, ext);
      kind = 2;
    } else if (dart && c == 'r' && i + 1 < n && (t[i + 1] == '"' || t[i + 1] == "'")) {
      i = skipCodeString(t, i + 1, ext, raw: true);
      kind = 2;
    } else if (_isWordStart(cu)) {
      while (i < n && _isWordPart(t.codeUnitAt(i))) {
        i++;
      }
      kind = 0;
    } else if (_isDigit(cu)) {
      while (i < n) {
        final d = t.codeUnitAt(i);
        if (_isWordPart(d)) {
          i++;
        } else if (t[i] == '.' && i + 1 < n && _isDigit(t.codeUnitAt(i + 1))) {
          i++;
        } else if ((t[i] == '+' || t[i] == '-') && i > start && (t[i - 1] == 'e' || t[i - 1] == 'E') && i + 1 < n && _isDigit(t.codeUnitAt(i + 1)) && !t.substring(start, i).startsWith('0x')) {
          i++;
        } else {
          break;
        }
      }
      kind = 1;
    } else if (c == '@' && i + 1 < n && _isWordStart(t.codeUnitAt(i + 1))) {
      i++;
      while (i < n && _isWordPart(t.codeUnitAt(i))) {
        i++;
      }
      if (kt && i < n && t[i] == ':' && _kotlinTargets.contains(t.substring(start + 1, i))) {
        i++;
        while (i < n && _isWordPart(t.codeUnitAt(i))) {
          i++;
        }
      }
      kind = 0;
    } else if (js && c == '/' && _regexOk(out.isEmpty ? null : out.last) && _scanRegex(t, i) > 0) {
      i = _scanRegex(t, i);
      kind = 6;
    } else {
      String? hit;
      for (final o in _ops4) {
        if (t.startsWith(o, i)) {
          hit = o;
          break;
        }
      }
      if (hit == null) {
        for (final o in _ops2) {
          if ((o == '?:' || o == '!!') && !kt) continue;
          if (t.startsWith(o, i)) {
            hit = o;
            break;
          }
        }
      }
      i += hit == null ? 1 : hit.length;
      kind = 5;
    }
    out.add(_T(kind, t.substring(start, i), nl, sp, col));
    nl = 0;
    sp = false;
  }
  return out;
}

/// End index of a regex literal starting at [i], or -1 when it isn't one.
int _scanRegex(String t, int i) {
  final n = t.length;
  var j = i + 1;
  var inClass = false;
  while (j < n) {
    final c = t[j];
    if (c == '\n') return -1;
    if (c == '\\') {
      j += 2;
      continue;
    }
    if (c == '[') {
      inClass = true;
    } else if (c == ']') {
      inClass = false;
    } else if (c == '/' && !inClass) {
      j++;
      while (j < n && _isWordPart(t.codeUnitAt(j))) {
        j++;
      }
      return j;
    }
    j++;
  }
  return -1;
}

const _blockWords = {'else', 'try', 'finally', 'do', 'async', 'sync', 'static', 'init'};
const _classWords = {'class', 'interface', 'enum', 'object', 'struct', 'mixin', 'extension', 'trait', 'namespace', 'record'};
const _headWords = {'if', 'for', 'while', 'foreach', 'elif'};
const _ctlParen = {'if', 'for', 'while', 'switch', 'catch', 'when', 'foreach', 'synchronized', 'return', 'throw', 'in', 'is', 'as', 'typeof', 'await', 'yield', 'else', 'case', 'of', 'using', 'lock', 'elif', 'function', 'with'};
const _noOperand = {'return', 'throw', 'case', 'in', 'is', 'as', 'typeof', 'instanceof', 'new', 'delete', 'void', 'yield', 'await', 'else', 'do', 'when', 'if', 'while', 'for', 'of', 'not', 'and', 'or'};
const _spacedOps = {'=', '==', '!=', '===', '!==', '<=', '>=', '+=', '-=', '*=', '/=', '%=', '&&', '||', '??', '=>', '->', '??=', '||=', '&&=', '&=', '|=', '^=', '<<=', '>>=', '>>>=', '?:'};
const _binOps = {'+', '-', '*', '/', '%', '|', '&', '^'};
const _contStart = {'.', '?.', '&&', '||', '??', '?', ':', '?:', '..', '=>', '->', '==', '!=', '+', '/', '%'};
const _contEnd = {'=', '+=', '-=', '*=', '/=', '%=', '&&', '||', '??', '+', '-', '/', '%', '=>', '==', '!=', '??=', '|', '&', '^'};

bool _classHeader(List<_T> toks, int i) {
  var cnt = 0;
  for (var k = i - 1; k >= 0 && cnt < 80; k--, cnt++) {
    final x = toks[k];
    if (x.k == 5 && (x.s == ';' || x.s == '{' || x.s == '}')) return false;
    if (x.k == 0 && _classWords.contains(x.s)) return true;
  }
  return false;
}

bool _isBlockBrace(List<_T> toks, int i) {
  if (i == 0) return true;
  final p = toks[i - 1];
  if (p.k == 5) {
    if (p.s == ')' || p.s == '=>' || p.s == '->' || p.s == ';' || p.s == '{' || p.s == '}') return true;
    if (p.s == '>') return _classHeader(toks, i);
    return false;
  }
  if (p.k == 0) {
    if (_blockWords.contains(p.s)) return true;
    return _classHeader(toks, i);
  }
  return false;
}

bool _operandEnd(_T t) {
  if (t.k == 1 || t.k == 2 || t.k == 6) return true;
  if (t.k == 0) return !_noOperand.contains(t.s);
  return t.k == 5 && (t.s == ')' || t.s == ']' || t.s == '!!');
}

bool _operandStart(_T t) {
  if (t.k == 1 || t.k == 2 || t.k == 6) return true;
  if (t.k == 0) return !const {'in', 'is', 'as', 'else', 'instanceof', 'of'}.contains(t.s);
  return t.k == 5 && (t.s == '(' || t.s == '[' || t.s == '!' || t.s == '~' || t.s == '-' || t.s == '+');
}

class _Op {
  final int line;
  final bool sw;
  bool caseOpen = false;
  _Op(this.line, this.sw);
}

/// 1 = one space, 0 = none, -1 = keep what the person wrote.
int _gap(List<_T> toks, int bi, List<bool> spaced) {
  final a = toks[bi - 1], b = toks[bi];
  if (b.k == 3 || b.k == 4) return 1;
  if (a.k == 4) return -1;
  final asv = a.s, bs = b.s;
  if (spaced[bi] || spaced[bi - 1]) return 1;
  if (a.k == 5 && _spacedOps.contains(asv)) return 1;
  if (b.k == 5) {
    if (bs == ',' || bs == ';' || bs == ')' || bs == ']') return 0;
    if (bs == '.' || bs == '?.' || bs == '::' || bs == '!!' || bs == '..' || bs == '..<') return _operandEnd(a) ? 0 : -1;
    if (bs == '(') {
      if (a.k == 0) return _ctlParen.contains(asv) ? 1 : 0;
      if (a.k == 5) {
        if (asv == '(' || asv == '[' || asv == ')' || asv == ']' || asv == '!!') return 0;
        if (asv == '>' || asv == '}') return -1;
        if (asv == ',' || asv == ';' || asv == ':' || asv == '?') return 1;
      }
      return -1;
    }
    if (bs == '[') {
      if (a.k == 0) return _noOperand.contains(asv) ? 1 : 0;
      if (a.k == 5) {
        if (asv == '(' || asv == '[' || asv == ')' || asv == ']' || asv == '!!') return 0;
        if (asv == '}') return -1;
        if (asv == ',' || asv == ';' || asv == ':') return 1;
      }
      return -1;
    }
    if (bs == '{') return (asv == '(' || asv == '[') ? 0 : 1;
    if (bs == '}') return asv == '{' ? 0 : 1;
    if (_spacedOps.contains(bs)) return 1;
  }
  if (a.k == 5) {
    switch (asv) {
      case '(':
      case '[':
      case '.':
      case '?.':
      case '::':
      case '..':
      case '..<':
      case '...':
      case '@':
      case '!':
      case '~':
        return 0;
      case ',':
      case ';':
      case '{':
        return 1;
      case ':':
        return 1;
      case '}':
        return b.k == 0 ? 1 : -1;
      default:
        return -1;
    }
  }
  if (a.k == 0 && b.k == 0) return 1;
  return -1;
}

String _quoteDouble(String s) {
  if (s.length < 2) return s;
  final from = _opts.singleQuote ? '"' : "'";
  final to = _opts.singleQuote ? "'" : '"';
  if (s[0] != from || s[s.length - 1] != from) return s;
  final body = s.substring(1, s.length - 1);
  if (body.contains(to) || body.contains('\\') || body.contains('\n')) return s;
  return '$to$body$to';
}

String _fixBlockComment(String s, String ind, int oldCol) {
  final ls = s.split('\n');
  if (ls.length == 1) return s;
  final b = StringBuffer(ls[0].trimRight());
  for (var i = 1; i < ls.length; i++) {
    final l = ls[i].trimRight();
    final tl = l.trimLeft();
    if (tl.isEmpty) {
      b.write('\n');
      continue;
    }
    if (tl.startsWith('*')) {
      b.write('\n$ind $tl');
    } else {
      final keep = (l.length - tl.length) - oldCol;
      b.write('\n$ind${' ' * (keep > 0 ? keep : 0)}$tl');
    }
  }
  return b.toString();
}

String _fmtBrace(String t, String ext) {
  final toks = _lex(t, ext);
  if (toks.isEmpty) return '';
  final n = toks.length;
  final unit = ' ' * indentSize(ext);
  final js = _jsLike.contains(ext);

  // Which bracket closes which.
  final match = List<int>.filled(n, -1);
  final isStmtSemi = List<bool>.filled(n, false);
  final stk = <int>[];
  for (var i = 0; i < n; i++) {
    final x = toks[i];
    if (x.k != 5) continue;
    final s = x.s;
    if (s == '(' || s == '[' || s == '{') {
      stk.add(i);
    } else if (s == ')' || s == ']' || s == '}') {
      if (stk.isEmpty) throw const FormatException('There is a closing bracket with nothing open for it.');
      final o = stk.removeLast();
      final os = toks[o].s;
      if ((s == ')' && os != '(') || (s == ']' && os != '[') || (s == '}' && os != '{')) {
        throw const FormatException('Brackets don\u2019t match. Fix that first (Check code can find it).');
      }
      match[o] = i;
      match[i] = o;
    } else if (s == ';') {
      isStmtSemi[i] = stk.isEmpty || toks[stk.last].s == '{';
    }
  }
  if (stk.isNotEmpty) throw const FormatException('A bracket is never closed. Fix that first.');

  final force = List<bool>.filled(n, false);
  final switchOpen = List<bool>.filled(n, false);
  final headEnd = List<bool>.filled(n, false);
  for (var i = 0; i < n; i++) {
    final x = toks[i];
    if (x.k != 5) continue;
    if (x.s == ')') {
      final o = match[i];
      if (o > 0 && toks[o - 1].k == 0 && _headWords.contains(toks[o - 1].s)) headEnd[i] = true;
    } else if (x.s == '{') {
      final blk = _isBlockBrace(toks, i);
      final m = match[i];
      if (blk && i > 0 && toks[i - 1].s == ')') {
        final o = match[i - 1];
        if (o > 0 && toks[o - 1].s == 'switch') switchOpen[i] = true;
      }
      if (blk && m > i + 1) {
        var brk = _classHeader(toks, i);
        if (!brk) {
          for (var k = i + 1; k < m; k++) {
            final y = toks[k];
            if (y.k == 5 && (y.s == ';' || y.s == '{')) {
              brk = true;
              break;
            }
          }
        }
        if (brk) {
          force[i + 1] = toks[i + 1].k != 3; // a // comment stays on the { line
          force[m] = true;
          if (m + 1 < n) {
            final z = toks[m + 1];
            final stay = z.k == 3 || z.k == 4 || (z.k == 5 && const {')', ']', ',', ';', '.', '?.', '!!', '}'}.contains(z.s)) || (z.k == 0 && const {'else', 'catch', 'finally', 'while'}.contains(z.s));
            if (z.nl == 0 && !stay) force[m + 1] = true;
          }
        }
      }
    } else if (x.s == ';' && isStmtSemi[i] && i + 1 < n) {
      final z = toks[i + 1];
      if (z.nl == 0 && z.k != 3 && z.k != 4 && !(z.k == 5 && z.s == '}')) force[i + 1] = true;
    }
  }

  // Operators that get a space on both sides.
  final spaced = List<bool>.filled(n, false);
  var tern = 0;
  for (var i = 0; i < n; i++) {
    final x = toks[i];
    if (x.k != 5) continue;
    final s = x.s;
    if (s == ';' || s == '{' || s == '}') {
      tern = 0;
      continue;
    }
    final hasP = i > 0, hasN = i + 1 < n;
    if (_binOps.contains(s)) {
      if (hasP && hasN && x.nl == 0 && toks[i + 1].nl == 0 && !(js && s == '/') && _operandEnd(toks[i - 1]) && _operandStart(toks[i + 1]) && x.sp == toks[i + 1].sp) {
        spaced[i] = true;
      }
    } else if (s == '?') {
      if (hasP && hasN && x.sp && x.nl == 0 && toks[i + 1].nl == 0 && _operandEnd(toks[i - 1]) && _operandStart(toks[i + 1])) {
        spaced[i] = true;
        tern++;
      }
    } else if (s == ':' && tern > 0) {
      tern--;
      spaced[i] = true;
    }
  }

  final sb = StringBuffer();
  final cur = StringBuffer();
  final stack = <_Op>[];
  var lineNo = 0;
  var ind = '';
  void flush() {
    sb.write(ind);
    sb.write(cur.toString());
    sb.write('\n');
    cur.clear();
    lineNo++;
  }

  for (var i = 0; i < n; i++) {
    final x = toks[i];
    final newLine = i == 0 || x.nl > 0 || force[i];
    final isCloser = x.k == 5 && (x.s == ')' || x.s == ']' || x.s == '}');
    if (newLine) {
      if (i > 0) {
        flush();
        final p = toks[i - 1];
        if (x.nl >= 2 && !(p.k == 5 && p.s == '{') && !isCloser) sb.write('\n');
      }
      var k = 0;
      var j = i;
      while (j < n && toks[j].k == 5 && (toks[j].s == ')' || toks[j].s == ']' || toks[j].s == '}') && (j == i || (toks[j].nl == 0 && !force[j]))) {
        k++;
        j++;
      }
      final keep = stack.length - k < 0 ? 0 : stack.length - k;
      var lvl = 0;
      var last = -1;
      for (var s = 0; s < keep; s++) {
        if (stack[s].line != last) {
          lvl++;
          last = stack[s].line;
        }
      }
      if (keep > 0) {
        final top = stack[keep - 1];
        if (top.sw) {
          final isCase = x.k == 0 && (x.s == 'case' || (x.s == 'default' && i + 1 < n && toks[i + 1].s == ':'));
          if (isCase) {
            top.caseOpen = true;
          } else if (top.caseOpen) {
            lvl++;
          }
        }
      }
      var cont = false;
      if (i > 0 && x.k != 3 && x.k != 4 && k == 0) {
        final p = toks[i - 1];
        final isLb = x.k == 5 && x.s == '{';
        if (x.k == 5 && _contStart.contains(x.s)) {
          cont = true;
        } else if (p.k == 5 && _contEnd.contains(p.s)) {
          cont = true;
        } else if (p.k == 5 && p.s == ')' && headEnd[i - 1] && !isLb) {
          cont = true;
        } else if (p.k == 0 && (p.s == 'else' || p.s == 'do') && !isLb) {
          cont = true;
        }
      }
      ind = unit * (lvl + (cont ? 1 : 0));
    } else {
      final g = _gap(toks, i, spaced);
      if (g == 1 || (g == -1 && x.sp)) cur.write(' ');
    }
    // the token itself
    if (x.k == 2 && (js)) {
      cur.write(_quoteDouble(x.s));
    } else if (x.k == 4) {
      cur.write(newLine ? _fixBlockComment(x.s, ind, x.col) : x.s);
    } else if (x.k == 3) {
      cur.write(x.s.trimRight());
    } else {
      cur.write(x.s);
    }
    if (x.k == 5) {
      if (x.s == '(' || x.s == '[' || x.s == '{') {
        stack.add(_Op(lineNo, x.s == '{' && switchOpen[i]));
      } else if (x.s == ')' || x.s == ']' || x.s == '}') {
        if (stack.isNotEmpty) stack.removeLast();
      }
    }
  }
  flush();
  return sb.toString();
}

/// Trailing spaces off, at most one blank line in a row, exactly one newline at the end.
String _tidy(String s) {
  final ls = s.split('\n');
  final out = <String>[];
  var blank = 0;
  for (final l in ls) {
    final r = l.trimRight();
    if (r.isEmpty) {
      blank++;
      if (blank > 1 || out.isEmpty) continue;
    } else {
      blank = 0;
    }
    out.add(r);
  }
  while (out.isNotEmpty && out.last.isEmpty) {
    out.removeLast();
  }
  return out.isEmpty ? '' : '${out.join('\n')}\n';
}

String _fmtBasic(String s) => _tidy(s);

// ------------------------------------------------------------------ CSS / SCSS

class _Css {
  final String t;
  final bool scss;
  final int n;
  int i = 0;
  final StringBuffer out = StringBuffer();
  _Css(this.t, this.scss) : n = t.length;
  static String get _unit => ' ' * (_opts.tabWidth ?? 2);

  String run() {
    _block(0);
    final s = out.toString().trimRight();
    return s.isEmpty ? '' : '$s\n';
  }

  void _put(int depth, String text, bool blank, {bool raw = false}) {
    if (blank && out.isNotEmpty) out.write('\n');
    final ind = _unit * depth;
    if (raw) {
      out.write(ind);
      out.write(text);
    } else {
      final ls = text.split('\n');
      for (var k = 0; k < ls.length; k++) {
        if (k > 0) out.write('\n');
        out.write(ind);
        out.write(ls[k]);
      }
    }
    out.write('\n');
  }

  int _strEnd(String s, int k) {
    final q = s[k];
    var j = k + 1;
    while (j < s.length) {
      final c = s[j];
      if (c == '\\') {
        j += 2;
        continue;
      }
      if (c == q) return j + 1;
      j++;
    }
    return s.length;
  }

  void _block(int depth) {
    var first = true;
    while (i < n) {
      var nls = 0;
      while (i < n && _isWs(t[i])) {
        if (t[i] == '\n') nls++;
        i++;
      }
      if (i >= n) return;
      if (t[i] == '}') {
        if (depth > 0) return;
        i++;
        continue;
      }
      final blank = nls >= 2 && !first;
      if (t.startsWith('/*', i)) {
        final e = t.indexOf('*/', i + 2);
        final end = e < 0 ? n : e + 2;
        _put(depth, t.substring(i, end).trimRight(), blank, raw: true);
        i = end;
        first = false;
        continue;
      }
      if (scss && t.startsWith('//', i)) {
        var e = t.indexOf('\n', i);
        if (e < 0) e = n;
        _put(depth, t.substring(i, e).trimRight(), blank, raw: true);
        i = e;
        first = false;
        continue;
      }
      final start = i;
      var paren = 0;
      String? term;
      while (i < n) {
        final c = t[i];
        if (c == '"' || c == "'") {
          i = _strEnd(t, i);
          continue;
        }
        if (t.startsWith('/*', i)) {
          final e = t.indexOf('*/', i + 2);
          i = e < 0 ? n : e + 2;
          continue;
        }
        if (c == '#' && i + 1 < n && t[i + 1] == '{') {
          var d = 0;
          while (i < n) {
            if (t[i] == '{') d++;
            if (t[i] == '}') {
              d--;
              if (d == 0) {
                i++;
                break;
              }
            }
            i++;
          }
          continue;
        }
        if (c == '(') {
          paren++;
        } else if (c == ')') {
          if (paren > 0) paren--;
        } else if (paren == 0 && (c == '{' || c == ';' || c == '}')) {
          term = c;
          break;
        }
        i++;
      }
      final prelude = t.substring(start, i).trim();
      if (term == '{') {
        i++;
        _put(depth, '${_selector(prelude)} {', blank);
        _block(depth + 1);
        if (i < n && t[i] == '}') i++;
        _put(depth, '}', false);
      } else {
        if (term == ';') i++;
        if (prelude.isNotEmpty) _put(depth, '${_decl(prelude)};', blank);
      }
      first = false;
    }
  }

  String _selector(String p) {
    if (p.startsWith('@')) return _atPrelude(p);
    final parts = <String>[];
    var depth = 0, start = 0;
    String? q;
    for (var k = 0; k < p.length; k++) {
      final c = p[k];
      if (q != null) {
        if (c == '\\') {
          k++;
        } else if (c == q) {
          q = null;
        }
        continue;
      }
      if (c == '"' || c == "'") {
        q = c;
      } else if (c == '(' || c == '[') {
        depth++;
      } else if (c == ')' || c == ']') {
        depth--;
      } else if (c == ',' && depth == 0) {
        parts.add(p.substring(start, k));
        start = k + 1;
      }
    }
    parts.add(p.substring(start));
    return parts.map((s) => _selPart(s.trim())).where((s) => s.isNotEmpty).join(',\n');
  }

  String _selPart(String s) {
    var cur = '';
    var depth = 0;
    String? q;
    for (var k = 0; k < s.length; k++) {
      final c = s[k];
      if (q != null) {
        cur += c;
        if (c == '\\' && k + 1 < s.length) {
          cur += s[++k];
        } else if (c == q) {
          q = null;
        }
        continue;
      }
      if (c == '"' || c == "'") {
        q = c;
        cur += c;
        continue;
      }
      if (c == '(' || c == '[') depth++;
      if (c == ')' || c == ']') depth--;
      if (depth == 0 && !scss && (c == '>' || c == '+' || c == '~')) {
        cur = '${cur.trimRight()} $c ';
        while (k + 1 < s.length && _isWs(s[k + 1])) {
          k++;
        }
        continue;
      }
      if (_isWs(c)) {
        if (cur.isNotEmpty && !cur.endsWith(' ')) cur += ' ';
        continue;
      }
      cur += c;
    }
    return _q(cur.trim());
  }

  String _atPrelude(String p) {
    var s = _valueNorm(p);
    s = s.replaceAllMapped(RegExp(r'\(\s*([A-Za-z-]+)\s*:\s*'), (m) => '(${m.group(1)}: ');
    return s;
  }

  int _colon(String p) {
    var depth = 0;
    for (var k = 0; k < p.length; k++) {
      final c = p[k];
      if (c == '"' || c == "'") {
        k = _strEnd(p, k) - 1;
        continue;
      }
      if (c == '(') depth++;
      if (c == ')') depth--;
      if (c == ':' && depth == 0) return k;
    }
    return -1;
  }

  String _decl(String p) {
    if (p.startsWith('@')) return _valueNorm(p);
    final c = _colon(p);
    if (c < 0) return _valueNorm(p);
    final prop = p.substring(0, c).trim();
    final v = p.substring(c + 1).trim();
    if (v.isEmpty) return '$prop:';
    return '$prop: ${_valueNorm(v)}';
  }

  String _q(String s) {
    final b = StringBuffer();
    var k = 0;
    while (k < s.length) {
      final c = s[k];
      if (c == '"' || c == "'") {
        final e = _strEnd(s, k);
        final lit = s.substring(k, e);
        if (c == "'" && lit.length >= 2 && lit.endsWith("'")) {
          final body = lit.substring(1, lit.length - 1);
          b.write(body.contains('"') || body.contains('\\') ? lit : '"$body"');
        } else {
          b.write(lit);
        }
        k = e;
        continue;
      }
      b.write(c);
      k++;
    }
    return b.toString();
  }

  String _valueNorm(String v) {
    final b = StringBuffer();
    var lastSp = false;
    var k = 0;
    while (k < v.length) {
      final c = v[k];
      if (c == '"' || c == "'") {
        final e = _strEnd(v, k);
        b.write(_q(v.substring(k, e)));
        k = e;
        lastSp = false;
        continue;
      }
      if (v.length >= k + 4 && v.substring(k, k + 4).toLowerCase() == 'url(') {
        var d = 0;
        var e = k + 3;
        while (e < v.length) {
          if (v[e] == '"' || v[e] == "'") {
            e = _strEnd(v, e);
            continue;
          }
          if (v[e] == '(') d++;
          if (v[e] == ')') {
            d--;
            if (d == 0) {
              e++;
              break;
            }
          }
          e++;
        }
        b.write(v.substring(k, e > v.length ? v.length : e));
        k = e;
        lastSp = false;
        continue;
      }
      if (_isWs(c)) {
        if (!lastSp && b.isNotEmpty) {
          b.write(' ');
          lastSp = true;
        }
        k++;
        continue;
      }
      if (c == ',') {
        final s = b.toString();
        if (s.endsWith(' ')) {
          b.clear();
          b.write(s.substring(0, s.length - 1));
        }
        b.write(', ');
        lastSp = true;
        k++;
        continue;
      }
      if (c == '!') {
        if (b.isNotEmpty && !lastSp) b.write(' ');
        b.write('!');
        k++;
        while (k < v.length && _isWs(v[k])) {
          k++;
        }
        lastSp = false;
        continue;
      }
      b.write(c);
      lastSp = false;
      k++;
    }
    return b.toString().trim();
  }
}

String _fmtCss(String t, bool scss) => _Css(t, scss).run();

// ------------------------------------------------------------------ JSON

class _J {
  final int type; // 0 object, 1 array, 2 value
  String raw = '';
  bool broken = false;
  final keys = <String>[];
  final items = <_J>[];
  _J(this.type);
}

class _JP {
  final String t;
  int i = 0;
  _JP(this.t);

  void _ws() {
    while (i < t.length && _isWs(t[i])) {
      i++;
    }
    if (i < t.length && t[i] == '/') throw const FormatException('comments');
  }

  _J parseAll() {
    _ws();
    final v = _value();
    _ws();
    if (i < t.length) throw FormatException('unexpected "${t[i]}" at character $i');
    return v;
  }

  _J _value() {
    if (i >= t.length) throw const FormatException('the text ends too early');
    final c = t[i];
    if (c == '{') {
      i++;
      final o = _J(0);
      final before = i;
      _ws();
      o.broken = t.substring(before, i).contains('\n');
      if (i < t.length && t[i] == '}') {
        i++;
        return o;
      }
      while (true) {
        _ws();
        if (i >= t.length || t[i] != '"') throw FormatException('expected a "key" at character $i');
        o.keys.add(_str());
        _ws();
        if (i >= t.length || t[i] != ':') throw FormatException('expected ":" at character $i');
        i++;
        _ws();
        o.items.add(_value());
        _ws();
        if (i < t.length && t[i] == ',') {
          i++;
          continue;
        }
        if (i < t.length && t[i] == '}') {
          i++;
          return o;
        }
        throw FormatException('expected "," or "}" at character $i');
      }
    }
    if (c == '[') {
      i++;
      final a = _J(1);
      _ws();
      if (i < t.length && t[i] == ']') {
        i++;
        return a;
      }
      while (true) {
        _ws();
        a.items.add(_value());
        _ws();
        if (i < t.length && t[i] == ',') {
          i++;
          continue;
        }
        if (i < t.length && t[i] == ']') {
          i++;
          return a;
        }
        throw FormatException('expected "," or "]" at character $i');
      }
    }
    final v = _J(2);
    if (c == '"') {
      v.raw = _str();
      return v;
    }
    final st = i;
    while (i < t.length && !_isWs(t[i]) && t[i] != ',' && t[i] != ']' && t[i] != '}' && t[i] != ':') {
      i++;
    }
    if (i == st) throw FormatException('unexpected "${t[i]}" at character $i');
    v.raw = t.substring(st, i);
    return v;
  }

  String _str() {
    final st = i;
    i++;
    while (i < t.length) {
      final c = t[i];
      if (c == '\\') {
        i += 2;
        continue;
      }
      if (c == '"') {
        i++;
        return t.substring(st, i);
      }
      if (c == '\n') break;
      i++;
    }
    throw FormatException('a string is never closed (character $st)');
  }
}

String _jsonFlat(_J j) {
  if (j.type == 2) return j.raw;
  if (j.type == 0) {
    if (j.items.isEmpty) return '{}';
    final parts = <String>[for (var k = 0; k < j.items.length; k++) '${j.keys[k]}: ${_jsonFlat(j.items[k])}'];
    return '{ ${parts.join(', ')} }';
  }
  if (j.items.isEmpty) return '[]';
  return '[${j.items.map(_jsonFlat).join(', ')}]';
}

bool _jsonCanFlat(_J j) {
  if (j.type == 2) return true;
  if (j.type == 0) return !j.broken && j.items.every(_jsonCanFlat);
  return j.items.every(_jsonCanFlat);
}

String _jsonPrint(_J j, int depth, int col) {
  if (j.type == 2) return j.raw;
  if (j.items.isEmpty) return j.type == 0 ? '{}' : '[]';
  final bigArr = j.type == 1 && j.items.length >= 2 && j.items.every((e) => e.type != 2 && e.items.isNotEmpty);
  if (_jsonCanFlat(j) && !bigArr) {
    final f = _jsonFlat(j);
    if (col + f.length + 1 <= _opts.printWidth) return f;
  }
  final step = ' ' * (_opts.tabWidth ?? 2);
  final ind = step * (depth + 1);
  final b = StringBuffer(j.type == 0 ? '{\n' : '[\n');
  for (var k = 0; k < j.items.length; k++) {
    b.write(ind);
    var prefix = '';
    if (j.type == 0) {
      prefix = '${j.keys[k]}: ';
      b.write(prefix);
    }
    b.write(_jsonPrint(j.items[k], depth + 1, ind.length + prefix.length));
    if (k < j.items.length - 1) b.write(',');
    b.write('\n');
  }
  b.write('${step * depth}${j.type == 0 ? '}' : ']'}');
  return b.toString();
}

String _fmtJson(String t) {
  final root = _JP(t).parseAll();
  return '${_jsonPrint(root, 0, 0)}\n';
}

// ------------------------------------------------------------------ HTML and XML

class _MN {
  final int type; // 0 text, 1 element, 2 raw (comment, doctype, stray tag), 3 raw text inside script/style/pre
  String name = '';
  String open = '';
  String close = '';
  String text = '';
  bool selfClose = false;
  bool hasClose = false;
  final kids = <_MN>[];
  _MN(this.type);
}

const _voidTags = {'area', 'base', 'br', 'col', 'embed', 'hr', 'img', 'input', 'link', 'meta', 'param', 'source', 'track', 'wbr'};
const _rawTags = {'script', 'style', 'textarea', 'pre'};
const _implicitSame = {'li', 'dt', 'dd', 'option', 'p', 'tr', 'td', 'th', 'thead', 'tbody'};
const _inlineTags = {
  'a', 'abbr', 'acronym', 'b', 'bdi', 'bdo', 'big', 'br', 'button', 'cite', 'code', 'data', 'del', 'dfn', 'em', 'font', 'i', 'img', 'input', 'ins', 'kbd', 'label', 'map', 'mark', 'meter',
  'output', 'progress', 'q', 's', 'samp', 'select', 'small', 'span', 'strike', 'strong', 'sub', 'sup', 'svg', 'time', 'tt', 'u', 'var', 'wbr', 'audio', 'video', 'canvas', 'iframe', 'object', 'picture',
};

bool _nameChar(int c) => _isWordPart(c) || c == 58 || c == 45 || c == 46;

_MN _parseMarkup(String t, bool html) {
  final root = _MN(1);
  final stack = <_MN>[root];
  final n = t.length;
  var i = 0;
  void addText(String s) {
    if (s.isEmpty) return;
    stack.last.kids.add(_MN(0)..text = s);
  }

  void raw(String s) {
    stack.last.kids.add(_MN(2)..text = s);
  }

  while (i < n) {
    final lt = t.indexOf('<', i);
    if (lt < 0) {
      addText(t.substring(i));
      break;
    }
    if (lt > i) addText(t.substring(i, lt));
    i = lt;
    String? endMark;
    var markLen = 0;
    if (t.startsWith('<!--', i)) {
      endMark = '-->';
      markLen = 4;
    } else if (t.startsWith('<![CDATA[', i)) {
      endMark = ']]>';
      markLen = 9;
    } else if (t.startsWith('<?', i)) {
      endMark = '?>';
      markLen = 2;
    } else if (t.startsWith('<!', i)) {
      endMark = '>';
      markLen = 2;
    }
    if (endMark != null) {
      final e = t.indexOf(endMark, i + markLen);
      final end = e < 0 ? n : e + endMark.length;
      raw(t.substring(i, end));
      i = end;
      continue;
    }
    final isClose = i + 1 < n && t[i + 1] == '/';
    final nameStart = i + (isClose ? 2 : 1);
    var j = nameStart;
    while (j < n && _nameChar(t.codeUnitAt(j))) {
      j++;
    }
    if (j == nameStart || !_isWordStart(t.codeUnitAt(nameStart))) {
      addText('<');
      i++;
      continue;
    }
    final name = t.substring(nameStart, j);
    var e = j;
    String? q;
    while (e < n) {
      final c = t[e];
      if (q != null) {
        if (c == q) q = null;
      } else if (c == '"' || c == "'") {
        q = c;
      } else if (c == '>') {
        break;
      }
      e++;
    }
    if (e >= n) {
      addText(t.substring(i));
      i = n;
      break;
    }
    final tag = t.substring(i, e + 1);
    i = e + 1;
    final lname = html ? name.toLowerCase() : name;
    if (isClose) {
      var idx = -1;
      for (var s = stack.length - 1; s >= 1; s--) {
        if ((html ? stack[s].name.toLowerCase() : stack[s].name) == lname) {
          idx = s;
          break;
        }
      }
      if (idx < 0) {
        raw(tag);
        continue;
      }
      while (stack.length - 1 > idx) {
        stack.removeLast();
      }
      final el = stack.removeLast();
      el.hasClose = true;
      el.close = tag;
      continue;
    }
    final el = _MN(1)
      ..name = name
      ..open = tag;
    if (html && _implicitSame.contains(lname) && stack.length > 1 && stack.last.name.toLowerCase() == lname) stack.removeLast();
    stack.last.kids.add(el);
    if (tag.endsWith('/>')) {
      el.selfClose = true;
      continue;
    }
    if (html && _voidTags.contains(lname)) continue;
    if (html && _rawTags.contains(lname)) {
      var end = n;
      for (final m in RegExp('</$lname\\b', caseSensitive: false).allMatches(t, i)) {
        end = m.start;
        break;
      }
      if (end > i) el.kids.add(_MN(3)..text = t.substring(i, end));
      i = end;
    }
    stack.add(el);
  }
  return root;
}

List<String> _attrsOf(String open, String name) {
  var s = open.substring(1 + name.length);
  s = s.substring(0, s.length - 1);
  if (s.endsWith('/')) s = s.substring(0, s.length - 1);
  final out = <String>[];
  var i = 0;
  final n = s.length;
  while (i < n) {
    while (i < n && _isWs(s[i])) {
      i++;
    }
    if (i >= n) break;
    final st = i;
    while (i < n && !_isWs(s[i]) && s[i] != '=') {
      i++;
    }
    var a = s.substring(st, i);
    var k = i;
    while (k < n && _isWs(s[k])) {
      k++;
    }
    if (k < n && s[k] == '=') {
      k++;
      while (k < n && _isWs(s[k])) {
        k++;
      }
      if (k < n && (s[k] == '"' || s[k] == "'")) {
        var e = s.indexOf(s[k], k + 1);
        if (e < 0) e = n - 1;
        a = '$a=${s.substring(k, e + 1)}';
        i = e + 1;
      } else {
        var e = k;
        while (e < n && !_isWs(s[e])) {
          e++;
        }
        a = '$a=${s.substring(k, e)}';
        i = e;
      }
    }
    out.add(a);
  }
  return out;
}

class _MP {
  final bool html;
  final int unit;
  final out = StringBuffer();
  static int get width => _opts.printWidth;
  _MP(this.html, this.unit);

  String _ind(int lv) => ' ' * (lv * unit);
  String _closeStr(_MN n) => n.close.replaceAll(RegExp(r'\s+'), '');
  String _collapse(String s) => s.replaceAll(RegExp(r'\s+'), ' ');

  bool _inline(_MN n) {
    if (n.type == 0) return true;
    if (n.type != 1 || !html) return false;
    if (!_inlineTags.contains(n.name.toLowerCase())) return false;
    for (final k in n.kids) {
      if (!_inline(k)) return false;
    }
    return true;
  }

  String _openFlat(_MN n) {
    final attrs = _attrsOf(n.open, n.name);
    return '<${n.name}${attrs.map((a) => ' $a').join()}${n.selfClose ? ' />' : '>'}';
  }

  String _flat(_MN n) {
    if (n.type == 0) return _collapse(n.text);
    final b = StringBuffer(_openFlat(n));
    for (final k in n.kids) {
      b.write(_flat(k));
    }
    if (n.hasClose) b.write(_closeStr(n));
    return b.toString();
  }

  List<String> _openLines(_MN n, int lv) {
    final ind = _ind(lv);
    final attrs = _attrsOf(n.open, n.name);
    final single = '$ind${_openFlat(n)}';
    if (html) {
      if (attrs.length >= 2 && single.length > width) {
        return ['$ind<${n.name}', ...attrs.map((a) => '$ind${' ' * unit}$a'), '$ind${n.selfClose ? '/>' : '>'}'];
      }
      return [single];
    }
    if (attrs.length > 2 || (attrs.length >= 2 && single.length > width)) {
      final ls = <String>['$ind<${n.name}'];
      for (var k = 0; k < attrs.length; k++) {
        final end = k == attrs.length - 1 ? (n.selfClose ? ' />' : '>') : '';
        ls.add('$ind${' ' * unit}${attrs[k]}$end');
      }
      return ls;
    }
    return [single];
  }

  void _kids(List<_MN> kids, int lv) {
    var i = 0;
    while (i < kids.length) {
      final k = kids[i];
      if (k.type == 0 && k.text.trim().isEmpty) {
        i++;
        continue;
      }
      if (_inline(k)) {
        final run = <_MN>[];
        while (i < kids.length && _inline(kids[i])) {
          run.add(kids[i]);
          i++;
        }
        _run(run, lv);
        continue;
      }
      _block(k, lv);
      i++;
    }
  }

  void _run(List<_MN> run, int lv) {
    final chunks = <String>[];
    final sps = <bool>[];
    var pending = false;
    for (final nd in run) {
      if (nd.type == 0) {
        final parts = nd.text.split(RegExp(r'\s+'));
        for (var x = 0; x < parts.length; x++) {
          final p = parts[x];
          if (p.isEmpty) {
            pending = true;
            continue;
          }
          chunks.add(p);
          sps.add(pending || x > 0);
          pending = false;
        }
      } else {
        chunks.add(_flat(nd));
        sps.add(pending);
        pending = false;
      }
    }
    if (chunks.isEmpty) return;
    final ind = _ind(lv);
    var line = StringBuffer(ind);
    var len = ind.length;
    for (var k = 0; k < chunks.length; k++) {
      final c = chunks[k];
      if (k == 0) {
        line.write(c);
        len += c.length;
        continue;
      }
      final add = (sps[k] ? 1 : 0) + c.length;
      if (sps[k] && len + add > width) {
        out.writeln(line.toString());
        line = StringBuffer(ind);
        line.write(c);
        len = ind.length + c.length;
      } else {
        if (sps[k]) line.write(' ');
        line.write(c);
        len += add;
      }
    }
    out.writeln(line.toString());
  }

  void _writeLines(List<String> ls, {String tail = ''}) {
    for (var k = 0; k < ls.length; k++) {
      out.write(ls[k]);
      if (k == ls.length - 1) {
        out.write(tail);
      } else {
        out.write('\n');
      }
    }
  }

  void _block(_MN n, int lv) {
    final ind = _ind(lv);
    if (n.type == 2 || n.type == 3 || n.type == 0) {
      out.write(ind);
      out.writeln(n.text.trimRight());
      return;
    }
    final lname = html ? n.name.toLowerCase() : n.name;
    final ol = _openLines(n, lv);
    if (n.kids.isEmpty) {
      if (!n.hasClose) {
        out.writeln(ol.join('\n'));
      } else if (ol.length == 1) {
        out.writeln('${ol[0]}${_closeStr(n)}');
      } else {
        out.writeln(ol.join('\n'));
        out.writeln('$ind${_closeStr(n)}');
      }
      return;
    }
    if (html && (lname == 'pre' || lname == 'textarea')) {
      final body = n.kids.map((k) => k.text).join();
      _writeLines(ol, tail: '$body${n.hasClose ? _closeStr(n) : ''}\n');
      return;
    }
    if (html && (lname == 'script' || lname == 'style')) {
      final body = n.kids.map((k) => k.text).join();
      final close = n.hasClose ? _closeStr(n) : '';
      if (body.trim().isEmpty) {
        _writeLines(ol, tail: '$close\n');
        return;
      }
      final fmt = _scriptFormatted(n, lname, body);
      if (fmt == null) {
        _writeLines(ol, tail: '$body$close\n');
        return;
      }
      out.writeln(ol.join('\n'));
      final pad = _ind(lv + 1);
      for (final l in fmt.split('\n')) {
        out.writeln(l.trim().isEmpty ? '' : '$pad$l');
      }
      if (n.hasClose) out.writeln('$ind$close');
      return;
    }
    final allInline = n.kids.every((k) => k.type == 0 || _inline(k));
    if (allInline && (html || n.kids.every((k) => k.type == 0)) && ol.length == 1) {
      final flat = n.kids.map(_flat).join().trim();
      final single = '${ol[0]}$flat${n.hasClose ? _closeStr(n) : ''}';
      if (!flat.contains('\n') && (single.length <= width || !html)) {
        out.writeln(single);
        return;
      }
    }
    out.writeln(ol.join('\n'));
    _kids(n.kids, lv + 1);
    if (n.hasClose) out.writeln('$ind${_closeStr(n)}');
  }

  /// The body of a <script> or <style>, formatted like a file of its own kind. Null = leave it exactly as it is.
  String? _scriptFormatted(_MN n, String lname, String body) {
    String? lang;
    if (lname == 'style') {
      lang = 'css';
    } else {
      final type = RegExp(r'''type\s*=\s*["']?([^"'\s>]*)''', caseSensitive: false).firstMatch(n.open)?.group(1)?.toLowerCase() ?? '';
      if (type.isEmpty || type == 'module' || type.contains('javascript') || type == 'text/babel') {
        lang = 'js';
      } else if (type.contains('json')) {
        lang = 'json';
      }
    }
    if (lang == null) return null;
    if (lang == 'js') {
      // A string spanning lines would get extra indentation inside it, which changes the string.
      try {
        for (final t in _lex(body, 'js')) {
          if (t.k == 2 && t.s.contains('\n')) return null;
        }
      } catch (_) {
        return null;
      }
    }
    try {
      return formatSource(body, lang).trimRight();
    } catch (_) {
      return null;
    }
  }
}

String _fmtMarkup(String t, bool html) {
  final root = _parseMarkup(t, html);
  final p = _MP(html, _opts.tabWidth ?? (html ? 2 : 4));
  p._kids(root.kids, 0);
  final s = p.out.toString().trimRight();
  return s.isEmpty ? '' : '$s\n';
}
