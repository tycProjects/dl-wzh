import 'package:flutter/material.dart';
import 'package:flutter/cupertino.dart' show CupertinoPageTransitionsBuilder;
import 'package:google_fonts/google_fonts.dart';

/// Colors measured from the real app's screenshots (dark theme).
class C {
  static const bg = Color(0xFF111111);
  static const card = Color(0xFF1C1C1E);
  static const sheet = Color(0xFF262626);
  static const field = Color(0xFF1A1A1A);
  static const line = Color(0xFF262626);
  static const teal = Color(0xFF4FD5A6);
  static const tealBtn = Color(0xFF5FDFC8);
  static const tealDeep = Color(0xFF1F3B32);
  static const muted = Color(0xFF8F8F94);
  static const red = Color(0xFFF0483E);
  static const text = Color(0xFFF5F5F5);
  static const onTeal = Color(0xFF10201B);
}

/// One place that sets the type scale: slightly smaller than the raw sizes, a relaxed line
/// height and a hair of tightening, so text sits comfortably instead of crowding its box.
const _typeScale = .92;
final Map<int, TextStyle> _styleCache = {};
TextStyle inter(double size, {FontWeight w = FontWeight.w400, Color c = C.text, double? h}) {
  // Built once per distinct style and reused: looking the font up on every rebuild is slow on weak phones.
  final key = Object.hash(size, w.index, c.value, h);
  return _styleCache[key] ??= GoogleFonts.inter(
    fontSize: (size * _typeScale * 2).round() / 2,
    fontWeight: w,
    color: c,
    height: h ?? 1.28,
    letterSpacing: size >= 20 ? -.3 : -.1,
  );
}

ThemeData buildTheme() => ThemeData.dark().copyWith(
      scaffoldBackgroundColor: C.bg,
      colorScheme: const ColorScheme.dark(primary: C.teal, surface: C.card),
      splashFactory: NoSplash.splashFactory,
      pageTransitionsTheme: const PageTransitionsTheme(builders: {
        TargetPlatform.android: CupertinoPageTransitionsBuilder(),
        TargetPlatform.iOS: CupertinoPageTransitionsBuilder(),
      }),
      highlightColor: Colors.transparent,
      textTheme: GoogleFonts.interTextTheme(ThemeData.dark().textTheme),
    );
