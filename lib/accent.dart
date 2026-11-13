import 'package:flutter/material.dart';
import 'api.dart';

/// Theme color: five hues, five shades each (same hue per row, only saturation and brightness change).
const kAccentHues = <String>['Teal', 'Blue', 'Purple', 'Coral', 'Pink'];
const kAccentShades = <List<Color>>[
  [Color(0xFF8FD3C2), Color(0xFF66C2AB), Color(0xFF44B197), Color(0xFF2E8F78), Color(0xFF1F6F5C)],
  [Color(0xFF8CC4F5), Color(0xFF5AA9EE), Color(0xFF2F8FE6), Color(0xFF1F70BF), Color(0xFF17558F)],
  [Color(0xFFC3A8F0), Color(0xFFA883E6), Color(0xFF8C5FDB), Color(0xFF6F46B8), Color(0xFF54348F)],
  [Color(0xFFF5B5A6), Color(0xFFF29683), Color(0xFFEE765F), Color(0xFFCC5A45), Color(0xFFA04434)],
  [Color(0xFFF4A9C4), Color(0xFFEE84AB), Color(0xFFE4608F), Color(0xFFBC4A73), Color(0xFF903758)],
];

/// The app's one accent color. Everything that used to be a fixed teal reads from here.
class Accent {
  Accent._();
  static final Accent i = Accent._();
  static const _defaultIndex = 2; // teal, shade 3 (the original look)

  /// row * 5 + column
  final ValueNotifier<int> index = ValueNotifier(_defaultIndex);

  Color get base => kAccentShades[index.value ~/ 5][index.value % 5];

  /// Text and icons that sit on a filled accent button: dark on light shades, white on deep ones.
  Color get on => base.computeLuminance() > .3 ? const Color(0xFF252A27) : Colors.white;

  Color get deep => Color.lerp(const Color(0xFF111111), base, .26)!;
  Color get dim => Color.lerp(const Color(0xFF111111), base, .62)!;

  /// Reads the saved choice. Call after Api.i.init().
  void load() {
    final v = int.tryParse(Api.i.prefs.getString('accent') ?? '');
    if (v != null && v >= 0 && v < 25) index.value = v;
  }

  Future<void> set(int v) async {
    if (v < 0 || v >= 25 || v == index.value) return;
    index.value = v;
    _repaintAll();
    await Api.i.prefs.setString('accent', '$v');
  }

  /// The accent is read when each widget is built, so every widget is told to build again.
  /// Their state is kept (same as hot reload).
  void _repaintAll() {
    void visit(Element e) {
      e.markNeedsBuild();
      e.visitChildren(visit);
    }

    WidgetsBinding.instance.rootElement?.visitChildren(visit);
  }
}
