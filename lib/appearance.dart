import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'api.dart';

/// Light mode: the whole app is drawn in its dark colors and flipped to a light look by one
/// color filter (lightness is inverted, hues are kept). Pictures the person picked are flipped
/// back with [Unfilter] so they keep their real colors.
const _kLightMatrix = <double>[
  0.574, -1.43, -0.144, 0, 255,
  -0.426, -0.43, -0.144, 0, 255,
  -0.426, -1.43, 0.856, 0, 255,
  0, 0, 0, 1, 0,
];
const kLightFilter = ColorFilter.matrix(_kLightMatrix);

class Appearance {
  Appearance._();
  static final Appearance i = Appearance._();

  /// true = light look, false = dark look (default).
  final ValueNotifier<bool> light = ValueNotifier(false);

  /// Reads the saved choice. Call after Api.i.init() (that is what loads the settings).
  void load() {
    final v = Api.i.prefs.getString('appearance') == 'light';
    light.value = v;
    _chrome(v);
  }

  Future<void> set(bool v) async {
    light.value = v;
    _chrome(v);
    if (v) {
      await Api.i.prefs.setString('appearance', 'light');
    } else {
      await Api.i.prefs.remove('appearance');
    }
  }

  void _chrome(bool v) {
    SystemChrome.setSystemUIOverlayStyle(SystemUiOverlayStyle(
      statusBarColor: Colors.transparent,
      statusBarIconBrightness: v ? Brightness.dark : Brightness.light,
      systemNavigationBarColor: v ? const Color(0xFFE9E9E9) : const Color(0xFF161616),
      systemNavigationBarIconBrightness: v ? Brightness.dark : Brightness.light,
    ));
  }
}

/// Wraps the whole app. The app below keeps its state when the look changes.
class LightLayer extends StatefulWidget {
  final Widget child;
  const LightLayer({super.key, required this.child});
  @override
  State<LightLayer> createState() => _LightLayerState();
}

class _LightLayerState extends State<LightLayer> {
  final GlobalKey _appKey = GlobalKey();

  @override
  Widget build(BuildContext context) => ValueListenableBuilder<bool>(
        valueListenable: Appearance.i.light,
        builder: (context, light, _) {
          final app = KeyedSubtree(key: _appKey, child: widget.child);
          return light ? ColorFiltered(colorFilter: kLightFilter, child: app) : app;
        },
      );
}

/// Keeps a picture's real colors in light mode.
class Unfilter extends StatelessWidget {
  final Widget child;
  const Unfilter({super.key, required this.child});

  @override
  Widget build(BuildContext context) => ValueListenableBuilder<bool>(
        valueListenable: Appearance.i.light,
        child: child,
        builder: (context, light, c) => light ? ColorFiltered(colorFilter: kLightFilter, child: c) : c!,
      );
}
