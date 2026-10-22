import 'dart:math' as math;
import 'package:flutter/material.dart';
import '../theme.dart';

String fmtTime(double t) {
  final m = t ~/ 60;
  final s = t - m * 60;
  return '$m:${s.toStringAsFixed(2).padLeft(5, '0')}';
}

/// Keyframe diamond.
class Diamond extends StatelessWidget {
  final double size;
  final Color color;
  final bool filled;
  const Diamond({super.key, this.size = 10, this.color = Colors.amber, this.filled = true});
  @override
  Widget build(BuildContext context) => Transform.rotate(
        angle: math.pi / 4,
        child: Container(
          width: size,
          height: size,
          decoration: BoxDecoration(
            color: filled ? color : Colors.transparent,
            border: Border.all(color: color, width: 1.6),
          ),
        ),
      );
}

const swatches = <int>[
  0xFFFFFFFF, 0xFF000000, 0xFFF0483E, 0xFFFF9F0A, 0xFFFFD60A, 0xFF30D158,
  0xFF4FD5A6, 0xFF0A84FF, 0xFF5E5CE6, 0xFFBF5AF2, 0xFFFF375F, 0xFF8E8E93,
];

class Swatches extends StatelessWidget {
  final int current;
  final void Function(int) onPick;
  const Swatches({super.key, required this.current, required this.onPick});
  @override
  Widget build(BuildContext context) => Wrap(
        spacing: 10,
        runSpacing: 10,
        children: [
          for (final c in swatches)
            GestureDetector(
              onTap: () => onPick(c),
              child: Container(
                width: 30,
                height: 30,
                decoration: BoxDecoration(
                  color: Color(c),
                  shape: BoxShape.circle,
                  border: Border.all(color: c == current ? C.teal : C.line, width: c == current ? 3 : 1),
                ),
              ),
            ),
        ],
      );
}

String fmtFrames(double t, int fps) {
  final total = (t * fps).round();
  String two(int v) => v.toString().padLeft(2, '0');
  return '${two(total ~/ fps ~/ 60)}:${two(total ~/ fps % 60)}:${two(total % fps)}';
}
