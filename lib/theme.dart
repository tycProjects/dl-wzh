import 'package:flutter/material.dart';
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

TextStyle inter(double size, {FontWeight w = FontWeight.w400, Color c = C.text, double? h}) =>
    GoogleFonts.inter(fontSize: size, fontWeight: w, color: c, height: h);

ThemeData buildTheme() => ThemeData.dark().copyWith(
      scaffoldBackgroundColor: C.bg,
      colorScheme: const ColorScheme.dark(primary: C.teal, surface: C.card),
      splashFactory: NoSplash.splashFactory,
      highlightColor: Colors.transparent,
      textTheme: GoogleFonts.interTextTheme(ThemeData.dark().textTheme),
    );
