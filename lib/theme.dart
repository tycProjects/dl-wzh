import 'package:flutter/material.dart';

class C {
  static const bg = Color(0xFF111111);
  static const card = Color(0xFF1C1C1E);
  static const sheet = Color(0xFF262626);
  static const line = Color(0xFF333336);
  static const teal = Color(0xFF4FD5A6);
  static const tealDeep = Color(0xFF1F3B32);
  static const muted = Color(0xFF8F8F94);
  static const red = Color(0xFFF0483E);
  static const text = Color(0xFFF5F5F5);
  static const onTeal = Color(0xFF10201B);
}

ThemeData buildTheme() => ThemeData.dark(useMaterial3: true).copyWith(
      scaffoldBackgroundColor: C.bg,
      colorScheme: const ColorScheme.dark(primary: C.teal, surface: C.card),
      splashFactory: NoSplash.splashFactory,
      sliderTheme: const SliderThemeData(
        trackHeight: 3,
        activeTrackColor: C.teal,
        thumbColor: C.teal,
        inactiveTrackColor: Color(0xFF3A3A3C),
        overlayShape: RoundSliderOverlayShape(overlayRadius: 14),
      ),
      appBarTheme: const AppBarTheme(backgroundColor: C.bg, elevation: 0),
    );
