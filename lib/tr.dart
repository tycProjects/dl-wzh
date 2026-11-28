import 'dart:async';
import 'dart:convert';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart' hide Text;
import 'package:flutter/material.dart' as m show Text;
import 'package:http/http.dart' as http;
import 'api.dart';
import 'tr_keys.dart';

/// Languages in the picker. [name] is how the language writes its own name.
class AppLanguage {
  final String code, name, english;
  final bool rtl;
  const AppLanguage(this.code, this.name, this.english, {this.rtl = false});
}

const kLanguages = <AppLanguage>[
  AppLanguage('en', 'English', 'English'),
  AppLanguage('es', 'Espa\u00F1ol', 'Spanish'),
  AppLanguage('fr', 'Fran\u00E7ais', 'French'),
  AppLanguage('pt', 'Portugu\u00EAs', 'Portuguese'),
  AppLanguage('de', 'Deutsch', 'German'),
  AppLanguage('it', 'Italiano', 'Italian'),
  AppLanguage('nl', 'Nederlands', 'Dutch'),
  AppLanguage('ru', '\u0420\u0443\u0441\u0441\u043A\u0438\u0439', 'Russian'),
  AppLanguage('tr', 'T\u00FCrk\u00E7e', 'Turkish'),
  AppLanguage('ar', '\u0627\u0644\u0639\u0631\u0628\u064A\u0629', 'Arabic', rtl: true),
  AppLanguage('hi', '\u0939\u093F\u0928\u094D\u0926\u0940', 'Hindi'),
  AppLanguage('bn', '\u09AC\u09BE\u0982\u09B2\u09BE', 'Bengali'),
  AppLanguage('ur', '\u0627\u0631\u062F\u0648', 'Urdu', rtl: true),
  AppLanguage('id', 'Bahasa Indonesia', 'Indonesian'),
  AppLanguage('vi', 'Ti\u1EBFng Vi\u1EC7t', 'Vietnamese'),
  AppLanguage('th', '\u0E44\u0E17\u0E22', 'Thai'),
  AppLanguage('zh-CN', '\u4E2D\u6587 (\u7B80\u4F53)', 'Chinese'),
  AppLanguage('ja', '\u65E5\u672C\u8A9E', 'Japanese'),
  AppLanguage('ko', '\uD55C\uAD6D\uC5B4', 'Korean'),
  AppLanguage('sw', 'Kiswahili', 'Swahili'),
  AppLanguage('fil', 'Filipino', 'Filipino'),
  AppLanguage('ha', 'Hausa', 'Hausa'),
  AppLanguage('pl', 'Polski', 'Polish'),
  AppLanguage('uk', '\u0423\u043A\u0440\u0430\u0457\u043D\u0441\u044C\u043A\u0430', 'Ukrainian'),
  AppLanguage('cs', '\u010Ce\u0161tina', 'Czech'),
  AppLanguage('ro', 'Rom\u00E2n\u0103', 'Romanian'),
  AppLanguage('el', '\u0395\u03BB\u03BB\u03B7\u03BD\u03B9\u03BA\u03AC', 'Greek'),
  AppLanguage('hu', 'Magyar', 'Hungarian'),
  AppLanguage('sv', 'Svenska', 'Swedish'),
  AppLanguage('no', 'Norsk', 'Norwegian'),
  AppLanguage('da', 'Dansk', 'Danish'),
  AppLanguage('fi', 'Suomi', 'Finnish'),
  AppLanguage('bg', '\u0411\u044A\u043B\u0433\u0430\u0440\u0441\u043A\u0438', 'Bulgarian'),
  AppLanguage('sr', '\u0421\u0440\u043F\u0441\u043A\u0438', 'Serbian'),
  AppLanguage('hr', 'Hrvatski', 'Croatian'),
  AppLanguage('sk', 'Sloven\u010Dina', 'Slovak'),
  AppLanguage('sl', 'Sloven\u0161\u010Dina', 'Slovenian'),
  AppLanguage('lt', 'Lietuvi\u0173', 'Lithuanian'),
  AppLanguage('lv', 'Latvie\u0161u', 'Latvian'),
  AppLanguage('et', 'Eesti', 'Estonian'),
  AppLanguage('ca', 'Catal\u00E0', 'Catalan'),
  AppLanguage('eu', 'Euskara', 'Basque'),
  AppLanguage('gl', 'Galego', 'Galician'),
  AppLanguage('sq', 'Shqip', 'Albanian'),
  AppLanguage('mk', '\u041C\u0430\u043A\u0435\u0434\u043E\u043D\u0441\u043A\u0438', 'Macedonian'),
  AppLanguage('is', '\u00CDslenska', 'Icelandic'),
  AppLanguage('ga', 'Gaeilge', 'Irish'),
  AppLanguage('cy', 'Cymraeg', 'Welsh'),
  AppLanguage('iw', '\u05E2\u05D1\u05E8\u05D9\u05EA', 'Hebrew', rtl: true),
  AppLanguage('fa', '\u0641\u0627\u0631\u0633\u06CC', 'Persian', rtl: true),
  AppLanguage('ps', '\u067E\u069A\u062A\u0648', 'Pashto', rtl: true),
  AppLanguage('ku', 'Kurd\u00EE', 'Kurdish'),
  AppLanguage('ms', 'Bahasa Melayu', 'Malay'),
  AppLanguage('jw', 'Basa Jawa', 'Javanese'),
  AppLanguage('su', 'Basa Sunda', 'Sundanese'),
  AppLanguage('ceb', 'Cebuano', 'Cebuano'),
  AppLanguage('zh-TW', '\u4E2D\u6587 (\u7E41\u9AD4)', 'Chinese Traditional'),
  AppLanguage('ta', '\u0BA4\u0BAE\u0BBF\u0BB4\u0BCD', 'Tamil'),
  AppLanguage('te', '\u0C24\u0C46\u0C32\u0C41\u0C17\u0C41', 'Telugu'),
  AppLanguage('mr', '\u092E\u0930\u093E\u0920\u0940', 'Marathi'),
  AppLanguage('gu', '\u0A97\u0AC1\u0A9C\u0AB0\u0ABE\u0AA4\u0AC0', 'Gujarati'),
  AppLanguage('kn', '\u0C95\u0CA8\u0CCD\u0CA8\u0CA1', 'Kannada'),
  AppLanguage('ml', '\u0D2E\u0D32\u0D2F\u0D3E\u0D33\u0D02', 'Malayalam'),
  AppLanguage('pa', '\u0A2A\u0A70\u0A1C\u0A3E\u0A2C\u0A40', 'Punjabi'),
  AppLanguage('ne', '\u0928\u0947\u092A\u093E\u0932\u0940', 'Nepali'),
  AppLanguage('si', '\u0DC3\u0DD2\u0D82\u0DC4\u0DBD', 'Sinhala'),
  AppLanguage('my', '\u1019\u103C\u1014\u103A\u1019\u102C', 'Burmese'),
  AppLanguage('km', '\u1781\u17D2\u1798\u17C2\u179A', 'Khmer'),
  AppLanguage('lo', '\u0EA5\u0EB2\u0EA7', 'Lao'),
  AppLanguage('mn', '\u041C\u043E\u043D\u0433\u043E\u043B', 'Mongolian'),
  AppLanguage('kk', '\u049A\u0430\u0437\u0430\u049B\u0448\u0430', 'Kazakh'),
  AppLanguage('uz', 'O\u02BBzbekcha', 'Uzbek'),
  AppLanguage('az', 'Az\u0259rbaycanca', 'Azerbaijani'),
  AppLanguage('ka', '\u10E5\u10D0\u10E0\u10D7\u10E3\u10DA\u10D8', 'Georgian'),
  AppLanguage('hy', '\u0540\u0561\u0575\u0565\u0580\u0565\u0576', 'Armenian'),
  AppLanguage('af', 'Afrikaans', 'Afrikaans'),
  AppLanguage('am', '\u12A0\u121B\u122D\u129B', 'Amharic'),
  AppLanguage('yo', 'Yor\u00F9b\u00E1', 'Yoruba'),
  AppLanguage('ig', 'Igbo', 'Igbo'),
  AppLanguage('zu', 'isiZulu', 'Zulu'),
  AppLanguage('so', 'Soomaali', 'Somali'),
];

/// Auto-translates the app's own text. Only text that is part of the app (see tr_keys.dart) is
/// translated, never what the person typed. Each text is translated once, kept on the phone, and
/// shows in English until its translation arrives (or when there is no internet).
class Lang {
  Lang._();
  static final Lang i = Lang._();

  /// Bumped whenever the language changes or new translations arrive.
  final ValueNotifier<int> tick = ValueNotifier(0);

  String _code = 'en';
  String get code => _code;
  bool get rtl => kLanguages.any((l) => l.code == _code && l.rtl);
  String get name => kLanguages.firstWhere((l) => l.code == _code, orElse: () => kLanguages.first).name;

  static final Set<String> _exact = {for (final k in kUiKeys) if (!k.contains('{}')) k};
  static final List<_Tpl> _tpls = [for (final k in kUiKeys) if (k.contains('{}')) _Tpl.make(k)];

  final Map<String, String> _done = {};
  final Map<String, _Hit?> _memo = {};
  final Set<String> _queued = {};
  final List<String> _queue = [];
  int _workers = 0;
  Timer? _flush, _save;

  /// Reads the saved language. Call after Api.i.init().
  void load() => _apply(Api.i.prefs.getString('lang') ?? 'en', notify: false);

  Future<void> set(String code) async {
    _apply(code);
    if (_code == 'en') {
      await Api.i.prefs.remove('lang');
    } else {
      await Api.i.prefs.setString('lang', _code);
    }
  }

  void _apply(String code, {bool notify = true}) {
    _code = kLanguages.any((l) => l.code == code) ? code : 'en';
    _done.clear();
    _queue.clear();
    _queued.clear();
    if (_code != 'en') {
      try {
        final raw = Api.i.prefs.getString('tr_$_code');
        if (raw != null) {
          (jsonDecode(raw) as Map).forEach((k, v) => _done['$k'] = '$v');
        }
      } catch (_) {}
    }
    if (notify) tick.value++;
  }

  /// The translation of [s], or null to keep it as it is.
  String? translate(String s) {
    if (_code == 'en') return null;
    final hit = _find(s);
    if (hit == null) return null;
    final done = _done[hit.key];
    if (done == null) {
      _want(hit.key);
      return null;
    }
    if (hit.args.isEmpty) return done;
    var out = done;
    for (var g = 0; g < hit.args.length; g++) {
      out = out.replaceAll('{$g}', hit.args[g]);
    }
    return out;
  }

  _Hit? _find(String s) {
    if (_memo.containsKey(s)) return _memo[s];
    _Hit? hit;
    if (_exact.contains(s)) {
      hit = _Hit(s, const []);
    } else {
      for (final t in _tpls) {
        final m = t.re.firstMatch(s);
        if (m != null) {
          hit = _Hit(t.key, [for (var g = 1; g <= t.n; g++) m.group(g) ?? '']);
          break;
        }
      }
    }
    if (_memo.length > 3000) _memo.clear();
    _memo[s] = hit;
    return hit;
  }

  void _want(String key) {
    if (!_queued.add(key)) return;
    _queue.add(key);
    _pump();
  }

  void _pump() {
    while (_workers < 4 && _queue.isNotEmpty) {
      final key = _queue.removeAt(0);
      final lang = _code;
      _workers++;
      _fetch(key, lang).then((tr) {
        _workers--;
        if (lang == _code && tr != null) {
          _done[key] = tr;
          _dirty();
        }
        _pump();
      });
    }
  }

  Future<String?> _fetch(String key, String lang) async {
    try {
      var n = 0;
      final src = key.replaceAllMapped('{}', (_) => '{${n++}}');
      final uri = Uri.https('translate.googleapis.com', '/translate_a/single', {
        'client': 'gtx',
        'sl': 'en',
        'tl': lang,
        'dt': 't',
        'q': src,
      });
      final res = await http.get(uri).timeout(const Duration(seconds: 15));
      if (res.statusCode != 200) return null;
      final data = jsonDecode(res.body);
      final segs = data[0] as List;
      final out = segs.map((s) => (s is List && s.isNotEmpty && s[0] != null) ? '${s[0]}' : '').join().trim();
      if (out.isEmpty) return null;
      for (var g = 0; g < n; g++) {
        if (!out.contains('{$g}')) return null;
      }
      return out;
    } catch (_) {
      return null;
    }
  }

  void _dirty() {
    _flush ??= Timer(const Duration(milliseconds: 150), () {
      _flush = null;
      tick.value++;
    });
    _save?.cancel();
    _save = Timer(const Duration(seconds: 2), () {
      if (_code == 'en' || _done.isEmpty) return;
      Api.i.prefs.setString('tr_$_code', jsonEncode(_done));
    });
  }
}

class _Hit {
  final String key;
  final List<String> args;
  const _Hit(this.key, this.args);
}

class _Tpl {
  final String key;
  final RegExp re;
  final int n;
  const _Tpl._(this.key, this.re, this.n);
  factory _Tpl.make(String key) {
    final parts = key.split('{}');
    final pattern = parts.map(RegExp.escape).join('(.+?)');
    return _Tpl._(key, RegExp('^$pattern\$', dotAll: true), parts.length - 1);
  }
}

/// Text that is translated when it is part of the app's own wording.
String tr(String s) => Lang.i.translate(s) ?? s;

/// Rebuilds everything below it that uses [Text] when the language (or a translation) changes.
class LangScope extends InheritedNotifier<ValueNotifier<int>> {
  LangScope({super.key, required ValueNotifier<int> notifier, required super.child}) : super(notifier: notifier);
  static void watch(BuildContext context) => context.dependOnInheritedWidgetOfExactType<LangScope>();
}

/// Drop-in replacement for Flutter's Text that translates the app's own wording.
class Text extends StatelessWidget {
  final String? data;
  final InlineSpan? textSpan;
  final TextStyle? style;
  final StrutStyle? strutStyle;
  final TextAlign? textAlign;
  final TextDirection? textDirection;
  final Locale? locale;
  final bool? softWrap;
  final TextOverflow? overflow;
  final TextScaler? textScaler;
  final int? maxLines;
  final String? semanticsLabel;
  final TextWidthBasis? textWidthBasis;
  final TextHeightBehavior? textHeightBehavior;
  final Color? selectionColor;

  /// true = show exactly as given (used for language names).
  final bool verbatim;

  const Text(
    String this.data, {
    super.key,
    this.style,
    this.strutStyle,
    this.textAlign,
    this.textDirection,
    this.locale,
    this.softWrap,
    this.overflow,
    this.textScaler,
    this.maxLines,
    this.semanticsLabel,
    this.textWidthBasis,
    this.textHeightBehavior,
    this.selectionColor,
    this.verbatim = false,
  }) : textSpan = null;

  const Text.rich(
    InlineSpan this.textSpan, {
    super.key,
    this.style,
    this.strutStyle,
    this.textAlign,
    this.textDirection,
    this.locale,
    this.softWrap,
    this.overflow,
    this.textScaler,
    this.maxLines,
    this.semanticsLabel,
    this.textWidthBasis,
    this.textHeightBehavior,
    this.selectionColor,
  })  : data = null,
        verbatim = true;

  @override
  Widget build(BuildContext context) {
    final span = textSpan;
    if (span != null) {
      return m.Text.rich(
        span,
        style: style,
        strutStyle: strutStyle,
        textAlign: textAlign,
        textDirection: textDirection,
        locale: locale,
        softWrap: softWrap,
        overflow: overflow,
        textScaler: textScaler,
        maxLines: maxLines,
        semanticsLabel: semanticsLabel,
        textWidthBasis: textWidthBasis,
        textHeightBehavior: textHeightBehavior,
        selectionColor: selectionColor,
      );
    }
    var s = data!;
    var dir = textDirection;
    if (!verbatim) {
      LangScope.watch(context);
      final t = Lang.i.translate(s);
      if (t != null) {
        s = t;
        if (Lang.i.rtl) dir ??= TextDirection.rtl;
      }
    }
    return m.Text(
      s,
      style: style,
      strutStyle: strutStyle,
      textAlign: textAlign,
      textDirection: dir,
      locale: locale,
      softWrap: softWrap,
      overflow: overflow,
      textScaler: textScaler,
      maxLines: maxLines,
      semanticsLabel: semanticsLabel,
      textWidthBasis: textWidthBasis,
      textHeightBehavior: textHeightBehavior,
      selectionColor: selectionColor,
    );
  }
}
