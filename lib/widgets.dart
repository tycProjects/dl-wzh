import 'dart:ui' show PathMetric;
import 'package:flutter/material.dart';
import 'theme.dart';
import 'anim.dart';
import 'store.dart';

/// Dashed rounded rectangle border (the "Build your APK" card).
class DashedRRect extends StatelessWidget {
  final Widget child;
  final double radius;
  final Color color;
  final Color fill;
  const DashedRRect({super.key, required this.child, this.radius = 24, this.color = const Color(0xFFC9C9C9), this.fill = C.card});
  @override
  Widget build(BuildContext context) => CustomPaint(
        painter: _DashPainter(radius, color, fill),
        child: child,
      );
}

class _DashPainter extends CustomPainter {
  final double r;
  final Color color, fill;
  _DashPainter(this.r, this.color, this.fill);
  @override
  void paint(Canvas canvas, Size s) {
    final rr = RRect.fromRectAndRadius(Rect.fromLTWH(1, 1, s.width - 2, s.height - 2), Radius.circular(r));
    canvas.drawRRect(rr, Paint()..color = fill);
    final path = Path()..addRRect(rr);
    final p = Paint()..color = color..style = PaintingStyle.stroke..strokeWidth = 2;
    for (final PathMetric m in path.computeMetrics()) {
      double d = 0;
      while (d < m.length) {
        canvas.drawPath(m.extractPath(d, d + 6), p);
        d += 10;
      }
    }
  }
  @override
  bool shouldRepaint(_) => false;
}

/// Corner brackets around the HTML / ZIP tiles.
class BracketTile extends StatelessWidget {
  final String title, sub;
  final bool on;
  final VoidCallback onTap;
  const BracketTile({super.key, required this.title, required this.sub, required this.on, required this.onTap});
  @override
  Widget build(BuildContext context) => GestureDetector(
        onTap: onTap,
        child: CustomPaint(
          foregroundPainter: _Brackets(),
          child: Container(
            height: 55,
            decoration: BoxDecoration(color: on ? C.teal : C.field, borderRadius: BorderRadius.circular(6)),
            child: Column(mainAxisAlignment: MainAxisAlignment.center, children: [
              Text(title, style: inter(17, w: FontWeight.w700, c: on ? C.onTeal : Colors.white, h: 1.1)),
              Text(sub, style: inter(14, c: on ? C.onTeal : Colors.white, h: 1.1)),
            ]),
          ),
        ),
      );
}

class _Brackets extends CustomPainter {
  @override
  void paint(Canvas c, Size s) {
    final p = Paint()..color = const Color(0xFFE8E8E8)..style = PaintingStyle.stroke..strokeWidth = 2..strokeCap = StrokeCap.round;
    const l = 14.0, o = 3.0;
    final w = s.width, h = s.height;
    Path corner(double x, double y, double dx, double dy) =>
        Path()..moveTo(x + dx * l, y)..lineTo(x, y)..lineTo(x, y + dy * l);
    c.drawPath(corner(-o, -o, 1, 1), p);
    c.drawPath(corner(w + o, -o, -1, 1), p);
    c.drawPath(corner(-o, h + o, 1, -1), p);
    c.drawPath(corner(w + o, h + o, -1, -1), p);
  }
  @override
  bool shouldRepaint(_) => false;
}

/// Pill segmented control with a thumb that slides like iOS.
class Seg extends StatelessWidget {
  final List<String> labels;
  final List<IconData>? icons;
  final int index;
  final ValueChanged<int> onChanged;
  final double height, radius;
  final bool bordered;
  final Color bg;
  const Seg({super.key, required this.labels, this.icons, required this.index, required this.onChanged,
      this.height = 54, this.radius = 22, this.bordered = false, this.bg = C.field});
  @override
  Widget build(BuildContext context) => Container(
        height: height,
        padding: EdgeInsets.all(bordered ? 5 : 6),
        decoration: BoxDecoration(
          color: bg, borderRadius: BorderRadius.circular(radius),
          border: bordered ? Border.all(color: const Color(0xFF333333)) : null),
        child: LayoutBuilder(builder: (c, box) {
          final w = box.maxWidth / labels.length;
          return Stack(children: [
            AnimatedPositioned(
              duration: const Duration(milliseconds: 380),
              curve: iosCurve,
              left: w * index, top: 0, bottom: 0, width: w,
              child: Container(
                decoration: BoxDecoration(
                  color: bordered ? const Color(0xFF1F4A40) : const Color(0xFF333333),
                  borderRadius: BorderRadius.circular(bordered ? 15 : 17),
                  border: bordered ? Border.all(color: C.teal, width: 1.5) : null),
              ),
            ),
            Row(children: [
              for (var i = 0; i < labels.length; i++)
                Expanded(
                  child: GestureDetector(
                    behavior: HitTestBehavior.opaque,
                    onTap: () => onChanged(i),
                    child: Center(
                      child: TweenAnimationBuilder<Color?>(
                        tween: ColorTween(end: i == index ? C.teal : (bordered ? const Color(0xFFD8D8DC) : Colors.white)),
                        duration: const Duration(milliseconds: 260),
                        builder: (c, col, _) => FittedBox(
                          fit: BoxFit.scaleDown,
                          child: Row(mainAxisSize: MainAxisSize.min, children: [
                            if (icons != null) ...[Icon(icons![i], size: 18, color: col), const SizedBox(width: 7)],
                            Text(labels[i], style: inter(16, w: bordered ? FontWeight.w600 : FontWeight.w700, c: col ?? C.text)),
                          ]),
                        ),
                      ),
                    ),
                  ),
                ),
            ]),
          ]);
        }),
      );
}

/// Bot floating button (bottom right).
class BotFab extends StatelessWidget {
  const BotFab({super.key});
  @override
  Widget build(BuildContext context) => Container(
        width: 54, height: 54,
        decoration: const BoxDecoration(color: Color(0xFF2C2C2E), shape: BoxShape.circle),
        child: CustomPaint(painter: _BotFace()),
      );
}

class _BotFace extends CustomPainter {
  @override
  void paint(Canvas c, Size s) {
    final fill = Paint()..color = C.teal;
    final st = Paint()..color = C.teal..style = PaintingStyle.stroke..strokeWidth = 2.4..strokeCap = StrokeCap.round;
    final cx = s.width / 2, cy = s.height / 2;
    c.drawCircle(Offset(cx - 5, cy - 5), 1.8, fill);
    c.drawCircle(Offset(cx + 5, cy - 5), 1.8, fill);
    c.drawPath(Path()..moveTo(cx - 6, cy + 2)..quadraticBezierTo(cx, cy + 8, cx + 6, cy + 2), st);
    c.drawLine(Offset(cx - 9, cy + 7), Offset(cx - 10, cy + 10), st);
    c.drawLine(Offset(cx + 9, cy + 7), Offset(cx + 10, cy + 10), st);
  }
  @override
  bool shouldRepaint(_) => false;
}

class TopBar extends StatelessWidget {
  final String title;
  final bool back;
  final VoidCallback? onMenu;
  const TopBar({super.key, this.title = 'WZH 2 APK', this.back = false, this.onMenu});
  Widget _b(IconData i, VoidCallback f) => SizedBox(
      width: 44, height: 44,
      child: Pressable(
        onTap: f, scale: .88,
        child: Center(child: Icon(i, size: 24, color: const Color(0xFFE8E8E8))),
      ));
  @override
  Widget build(BuildContext context) => Container(
        height: 54,
        padding: const EdgeInsets.symmetric(horizontal: 6),
        decoration: const BoxDecoration(border: Border(bottom: BorderSide(color: C.line))),
        child: Row(children: [
          SizedBox(
            width: 100,
            child: Row(children: [
              back
                  ? _b(Icons.arrow_back_ios_new_rounded, () => Navigator.of(context).maybePop())
                  : _b(Icons.menu_rounded, onMenu ?? () {}),
              _b(Icons.wb_sunny_outlined, () => showInfoDialog(context, title: 'Appearance', message: 'The app uses the dark look for now. A light mode is coming.')),
            ]),
          ),
          Expanded(child: Text(title, textAlign: TextAlign.center, style: inter(19, w: FontWeight.w700))),
          SizedBox(
            width: 100,
            child: Row(mainAxisAlignment: MainAxisAlignment.end,
                children: [
                  _b(Icons.language_rounded, () => showInfoDialog(context, title: 'Language', message: 'The app is in English for now. Auto-translate to other languages is coming.')),
                  _b(Icons.more_horiz_rounded, () {
                    Store.i.refreshLimit();
                    Store.i.refreshHistory();
                  }),
                ]),
          ),
        ]),
      );
}


/// Simple popup (icon badge, title, muted text, one button) used for errors and limits.
Future<void> showInfoDialog(BuildContext context, {required String title, required String message, IconData icon = Icons.info_outline_rounded, Color? color, String button = 'OK'}) {
  final c = color ?? C.teal;
  return showIosDialog<void>(
    context,
    (ctx) => Container(
      width: MediaQuery.of(ctx).size.width - 64,
      padding: const EdgeInsets.all(22),
      decoration: BoxDecoration(color: const Color(0xFF1E1E20), borderRadius: BorderRadius.circular(28)),
      child: Column(mainAxisSize: MainAxisSize.min, crossAxisAlignment: CrossAxisAlignment.start, children: [
        Container(
          width: 56, height: 56,
          decoration: BoxDecoration(color: c.withOpacity(.16), shape: BoxShape.circle),
          child: Icon(icon, size: 26, color: c),
        ),
        const SizedBox(height: 16),
        Text(title, style: inter(20, w: FontWeight.w700)),
        const SizedBox(height: 8),
        Text(message, style: inter(16, c: C.muted, h: 1.35)),
        const SizedBox(height: 20),
        Pressable(
          onTap: () => Navigator.pop(ctx),
          child: Container(
            height: 48, alignment: Alignment.center,
            decoration: BoxDecoration(color: C.tealBtn, borderRadius: BorderRadius.circular(24)),
            child: Text(button, style: inter(17, w: FontWeight.w700, c: const Color(0xFF0F2A24))),
          ),
        ),
      ]),
    ),
  );
}

/// Text-field container that draws the teal focus ring when anything inside it has focus.
class FocusRing extends StatefulWidget {
  final Widget child;
  final double height, radius;
  final Color color, idleBorder;
  final EdgeInsetsGeometry padding;
  const FocusRing({
    super.key,
    required this.child,
    required this.height,
    required this.radius,
    this.color = C.field,
    this.idleBorder = Colors.transparent,
    this.padding = const EdgeInsets.symmetric(horizontal: 16),
  });
  @override
  State<FocusRing> createState() => _FocusRingState();
}

class _FocusRingState extends State<FocusRing> {
  bool on = false;
  @override
  Widget build(BuildContext context) => Focus(
        onFocusChange: (v) => setState(() => on = v),
        child: AnimatedContainer(
          duration: const Duration(milliseconds: 160),
          curve: Curves.easeOut,
          height: widget.height,
          padding: widget.padding,
          decoration: BoxDecoration(
            color: widget.color,
            borderRadius: BorderRadius.circular(widget.radius),
            border: Border.all(color: on ? C.teal : widget.idleBorder, width: on ? 1.8 : 1),
          ),
          child: widget.child,
        ),
      );
}
