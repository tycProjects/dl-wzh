import 'package:flutter/foundation.dart' show defaultTargetPlatform, TargetPlatform;
import 'package:flutter/material.dart';
import 'accent.dart';
import 'package:flutter/cupertino.dart' show CupertinoPageTransitionsBuilder;

/// Colors measured from the real app's screenshots (dark theme).
class C {
  static const bg = Color(0xFF111111);
  static const card = Color(0xFF1C1C1E);
  static const sheet = Color(0xFF262626);
  static const field = Color(0xFF1A1A1A);
  static const line = Color(0xFF262626);
  static Color get teal => Accent.i.base;
  static Color get tealBtn => Accent.i.base;
  static Color get tealDeep => Accent.i.deep;
  static Color get tealDim => Accent.i.dim;
  static Color get onTeal => Accent.i.on;
  static const muted = Color(0xFF8F8F94);
  static const red = Color(0xFFF0483E);
  static const text = Color(0xFFE7EAEF);
}

/// One place that sets the type scale: slightly smaller than the raw sizes, with Apple's own
/// San Francisco tracking per size so text looks like iOS.
const _typeScale = .92;

/// On iPhone the real system font (San Francisco) is used. Android can't legally ship SF, so there
/// it uses Inter (the closest match, drawn after SF) with SF's exact tracking and line height.
bool get _isIos => defaultTargetPlatform == TargetPlatform.iOS;

/// SF Pro tracking in logical px by point size (Apple's published values, interpolated).
double _sfTracking(double size) {
  final t = <double, double>{
    10: .12, 11: .06, 12: 0, 13: -.08, 14: -.15, 15: -.24, 16: -.32, 17: -.41,
    18: -.43, 20: -.45, 22: -.26, 24: -.2, 28: .36, 34: .38,
  };
  final ks = t.keys.toList();
  if (size <= ks.first) return t[ks.first]!;
  if (size >= ks.last) return t[ks.last]!;
  for (var i = 1; i < ks.length; i++) {
    if (size <= ks[i]) {
      final a = ks[i - 1], b = ks[i];
      return t[a]! + (t[b]! - t[a]!) * ((size - a) / (b - a));
    }
  }
  return 0;
}

final Map<int, TextStyle> _styleCache = {};
TextStyle inter(double size, {FontWeight w = FontWeight.w400, Color c = C.text, double? h}) {
  // Built once per distinct style and reused: looking the font up on every rebuild is slow on weak phones.
  final key = Object.hash(size, w.index, c.value, h);
  return _styleCache[key] ??= _build(size, w, c, h);
}

TextStyle _build(double size, FontWeight w, Color c, double? h) {
  final fs = (size * _typeScale * 2).round() / 2;
  final ls = _sfTracking(fs);
  if (_isIos) {
    // SF Pro Display from 20pt up, SF Pro Text below, exactly like iOS does.
    return TextStyle(
      fontFamily: fs >= 20 ? '.SF Pro Display' : '.SF Pro Text',
      fontSize: fs, fontWeight: w, color: c, height: h ?? 1.28, letterSpacing: ls,
    );
  }
  // Inter is bundled in the app (assets/fonts), so the text never changes to another font while a download is pending.
  return TextStyle(fontFamily: 'Inter', fontSize: fs, fontWeight: w, color: c, height: h ?? 1.28, letterSpacing: ls);
}

/// Screen and section titles: the same iOS-style font as everything else, just heavier and tighter.
TextStyle heading(double size, {FontWeight w = FontWeight.w700, Color c = C.text, double? h}) => inter(size, w: w, c: c, h: h ?? 1.15);

ThemeData buildTheme() => ThemeData.dark().copyWith(
      scaffoldBackgroundColor: C.bg,
      colorScheme: ColorScheme.dark(primary: C.teal, surface: C.card),
      splashFactory: NoSplash.splashFactory,
      pageTransitionsTheme: const PageTransitionsTheme(builders: {
        TargetPlatform.android: CupertinoPageTransitionsBuilder(),
        TargetPlatform.iOS: CupertinoPageTransitionsBuilder(),
      }),
      highlightColor: Colors.transparent,
      textTheme: _isIos
          ? ThemeData.dark().textTheme.apply(fontFamily: '.SF Pro Text')
          : ThemeData.dark().textTheme.apply(fontFamily: 'Inter'),
    );
