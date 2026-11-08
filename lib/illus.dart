import 'package:flutter/material.dart';


/// A picture slot for an illustration. Put a PNG at `assets/illustrations/<name>.png` and it shows up here;
/// until then the [fallback] (the normal small icon) is shown, so nothing breaks if a file is missing.
class Illus extends StatelessWidget {
  final String name;
  final double height;
  final Widget fallback;
  const Illus(this.name, {super.key, this.height = 140, required this.fallback});
  @override
  Widget build(BuildContext context) => Image.asset(
        'assets/illustrations/$name.png',
        height: height,
        fit: BoxFit.contain,
        filterQuality: FilterQuality.medium,
        errorBuilder: (_, __, ___) => fallback,
      );
}
