import 'dart:ui' show PathMetric;
import 'package:flutter/material.dart' hide Text;
import 'theme.dart';
import 'anim.dart';
import 'store.dart';
import 'notices.dart';
import 'banner.dart';
import 'tr.dart';
import 'appearance.dart';
import 'screens/prefs_sheets.dart';
import 'screens/updates_page.dart';
import 'package:flutter/cupertino.dart' show CupertinoPageRoute;

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
/// Picking a tile cross-fades its fill, text and brackets (about 220 ms) and gives the newly
/// chosen tile a small springy "settle" so the switch feels alive instead of snapping.
class BracketTile extends StatefulWidget {
  final String title, sub;
  final bool on;
  final VoidCallback onTap;
  const BracketTile({super.key, required this.title, required this.sub, required this.on, required this.onTap});
  @override
  State<BracketTile> createState() => _BracketTileState();
}

class _BracketTileState extends State<BracketTile> with SingleTickerProviderStateMixin {
  late final AnimationController _pop =
      AnimationController(vsync: this, duration: const Duration(milliseconds: 420), value: 1);

  @override
  void didUpdateWidget(BracketTile old) {
    super.didUpdateWidget(old);
    if (widget.on && !old.on) _pop.forward(from: 0);
  }

  @override
  void dispose() {
    _pop.dispose();
    super.dispose();
  }

  static const _fade = Duration(milliseconds: 220);

  @override
  Widget build(BuildContext context) {
    final on = widget.on;
    return Pressable(
      onTap: widget.onTap,
      scale: .95,
      child: AnimatedBuilder(
        animation: _pop,
        builder: (_, child) => Transform.scale(
          scale: .93 + .07 * Curves.easeOutBack.transform(_pop.value),
          child: child,
        ),
        child: TweenAnimationBuilder<Color?>(
          tween: ColorTween(end: on ? const Color(0xFFBDEFD9) : const Color(0xFFA2A2A6)),
          duration: _fade,
          curve: Curves.easeOut,
          builder: (_, bracket, child) => CustomPaint(foregroundPainter: _Brackets(bracket ?? const Color(0xFFA2A2A6)), child: child),
          child: AnimatedContainer(
            duration: _fade,
            curve: Curves.easeOut,
            height: 50,
            decoration: BoxDecoration(color: on ? C.teal : C.field, borderRadius: BorderRadius.circular(6)),
            child: Column(mainAxisAlignment: MainAxisAlignment.center, children: [
              AnimatedDefaultTextStyle(
                duration: _fade,
                curve: Curves.easeOut,
                style: inter(16, w: FontWeight.w700, c: on ? C.onTeal : Colors.white, h: 1.2),
                child: Text(widget.title, maxLines: 1, textAlign: TextAlign.center),
              ),
              AnimatedDefaultTextStyle(
                duration: _fade,
                curve: Curves.easeOut,
                style: inter(12, c: on ? const Color(0xFF1F6B55) : const Color(0xFFA2A2A6), h: 1.2),
                child: Text(widget.sub, maxLines: 1, textAlign: TextAlign.center),
              ),
            ]),
          ),
        ),
      ),
    );
  }
}

class _Brackets extends CustomPainter {
  final Color color;
  const _Brackets(this.color);
  @override
  void paint(Canvas c, Size s) {
    final p = Paint()..color = color..style = PaintingStyle.stroke..strokeWidth = 2..strokeCap = StrokeCap.round;
    const l = 14.0, o = 2.0;
    final w = s.width, h = s.height;
    Path corner(double x, double y, double dx, double dy) =>
        Path()..moveTo(x + dx * l, y)..lineTo(x, y)..lineTo(x, y + dy * l);
    c.drawPath(corner(-o, -o, 1, 1), p);
    c.drawPath(corner(w + o, -o, -1, 1), p);
    c.drawPath(corner(-o, h + o, 1, -1), p);
    c.drawPath(corner(w + o, h + o, -1, -1), p);
  }
  @override
  bool shouldRepaint(_Brackets old) => old.color != color;
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
  final double? pad, thumbRadius;
  final double thumbInset, fontSize;
  const Seg({super.key, required this.labels, this.icons, required this.index, required this.onChanged,
      this.height = 54, this.radius = 22, this.bordered = false, this.bg = C.field,
      this.pad, this.thumbRadius, this.thumbInset = 0, this.fontSize = 16});
  @override
  Widget build(BuildContext context) => Container(
        height: height,
        padding: EdgeInsets.all(pad ?? (bordered ? 5 : 6)),
        decoration: BoxDecoration(
          color: bg, borderRadius: BorderRadius.circular(radius),
          border: bordered ? Border.all(color: const Color(0xFF333333)) : null),
        child: LayoutBuilder(builder: (c, box) {
          final w = box.maxWidth / labels.length;
          return Stack(children: [
            AnimatedPositioned(
              duration: const Duration(milliseconds: 300),
              curve: iosCurve,
              left: w * index + thumbInset, top: thumbInset, bottom: thumbInset, width: w - thumbInset * 2,
              child: Container(
                decoration: BoxDecoration(
                  color: bordered ? const Color(0xFF1F4A40) : const Color(0xFF333333),
                  borderRadius: BorderRadius.circular(thumbRadius ?? (bordered ? 15 : 17)),
                  border: bordered ? Border.all(color: C.teal, width: 1.5) : null),
              ),
            ),
            Row(children: [
              for (var i = 0; i < labels.length; i++)
                Expanded(
                  child: Pressable(
                    scale: .94,
                    haptic: false,
                    onTap: () => onChanged(i),
                    child: Center(
                      child: TweenAnimationBuilder<Color?>(
                        tween: ColorTween(end: i == index ? C.teal : (bordered ? const Color(0xFFD8D8DC) : Colors.white)),
                        duration: const Duration(milliseconds: 210),
                        builder: (c, col, _) => Padding(
                          padding: const EdgeInsets.symmetric(horizontal: 8),
                          child: FittedBox(
                            fit: BoxFit.scaleDown,
                            child: Row(mainAxisSize: MainAxisSize.min, children: [
                              if (icons != null) ...[Icon(icons![i], size: fontSize + 2, color: col), const SizedBox(width: 5)],
                              Text(labels[i], style: inter(fontSize, w: bordered ? FontWeight.w500 : FontWeight.w700, c: col ?? C.text)),
                            ]),
                          ),
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
  final VoidCallback? onTap;
  const BotFab({super.key, this.onTap});
  @override
  Widget build(BuildContext context) {
    final face = RepaintBoundary(
      child: Container(
        width: 54, height: 54,
        decoration: const BoxDecoration(color: Color(0xFF2C2C2E), shape: BoxShape.circle),
        child: const CustomPaint(painter: _BotFace()),
      ),
    );
    return onTap == null ? face : Pressable(scale: .9, onTap: onTap, child: face);
  }
}

class _BotFace extends CustomPainter {
  const _BotFace();
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
              ValueListenableBuilder<bool>(
                valueListenable: Appearance.i.light,
                builder: (_, light, __) => _b(light ? Icons.dark_mode_outlined : Icons.wb_sunny_outlined, () => Appearance.i.set(!light)),
              ),
            ]),
          ),
          Expanded(child: Text(title, textAlign: TextAlign.center, style: inter(19, w: FontWeight.w700))),
          SizedBox(
            width: 100,
            child: Row(mainAxisAlignment: MainAxisAlignment.end,
                children: [
                  _b(Icons.language_rounded, () => showLanguageSheet(context)),
                  Builder(builder: (ctx) => _b(Icons.more_horiz_rounded, () {
                    final box = ctx.findRenderObject() as RenderBox;
                    showTopMenu(ctx, box.localToGlobal(Offset.zero) & box.size);
                  })),
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

/// "Are you sure you want to leave?" card with Stay / Leave. Returns true when the person leaves.
Future<bool> confirmLeave(BuildContext context, {required String title, required String message, String stay = 'Stay', String leave = 'Leave'}) async {
  final r = await showIosDialog<bool>(
    context,
    (ctx) => Container(
      width: MediaQuery.of(ctx).size.width - 64,
      padding: const EdgeInsets.all(22),
      decoration: BoxDecoration(color: const Color(0xFF1E1E20), borderRadius: BorderRadius.circular(28)),
      child: Column(mainAxisSize: MainAxisSize.min, crossAxisAlignment: CrossAxisAlignment.start, children: [
        Container(
          width: 56, height: 56,
          decoration: BoxDecoration(color: const Color(0xFFFFB340).withOpacity(.16), shape: BoxShape.circle),
          child: const Icon(Icons.logout_rounded, size: 26, color: Color(0xFFFFB340)),
        ),
        const SizedBox(height: 16),
        Text(title, style: inter(20, w: FontWeight.w700)),
        const SizedBox(height: 8),
        Text(message, style: inter(16, c: C.muted, h: 1.35)),
        const SizedBox(height: 20),
        Row(children: [
          Expanded(
            child: Pressable(
              onTap: () => Navigator.pop(ctx, false),
              child: Container(
                height: 48, alignment: Alignment.center,
                decoration: BoxDecoration(color: C.tealBtn, borderRadius: BorderRadius.circular(24)),
                child: Text(stay, style: inter(17, w: FontWeight.w700, c: const Color(0xFF0F2A24))),
              ),
            ),
          ),
          const SizedBox(width: 10),
          Expanded(
            child: Pressable(
              onTap: () => Navigator.pop(ctx, true),
              child: Container(
                height: 48, alignment: Alignment.center,
                decoration: BoxDecoration(color: const Color(0xFF2A2A2C), borderRadius: BorderRadius.circular(24)),
                child: Text(leave, style: inter(17, w: FontWeight.w700, c: C.text)),
              ),
            ),
          ),
        ]),
      ]),
    ),
  );
  return r == true;
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
          duration: const Duration(milliseconds: 130),
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


/// The small iOS-style popup under the "..." button: Refresh app / Updates.
/// It grows out of the button's corner with the iOS curve and closes on any tap outside.
Future<void> showTopMenu(BuildContext context, Rect anchor) {
  final screen = MediaQuery.of(context).size;
  return Navigator.of(context, rootNavigator: true).push(PageRouteBuilder<void>(
    opaque: false,
    barrierDismissible: true,
    barrierLabel: 'close',
    barrierColor: Colors.transparent,
    transitionDuration: const Duration(milliseconds: 300),
    reverseTransitionDuration: const Duration(milliseconds: 160),
    pageBuilder: (c, a, s) {
      void close(VoidCallback action) {
        Navigator.of(c).pop();
        action();
      }

      Widget row(IconData icon, String label, VoidCallback action) => Pressable(
            scale: .97,
            onTap: () => close(action),
            child: Padding(
              padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
              child: Row(children: [
                Icon(icon, size: 19, color: C.teal),
                const SizedBox(width: 11),
                Expanded(child: Text(label, maxLines: 1, overflow: TextOverflow.ellipsis, style: inter(15, w: FontWeight.w500))),
              ]),
            ),
          );

      return Material(
        type: MaterialType.transparency,
        child: Stack(children: [
          Positioned(
            top: anchor.bottom + 4,
            right: (screen.width - anchor.right).clamp(12.0, 400.0).toDouble(),
            child: FadeTransition(
              opacity: CurvedAnimation(parent: a, curve: Curves.easeOut),
              child: ScaleTransition(
                alignment: Alignment.topRight,
                scale: Tween<double>(begin: .82, end: 1.0)
                    .animate(CurvedAnimation(parent: a, curve: iosCurve, reverseCurve: Curves.easeIn)),
                child: Container(
                  width: 172,
                  padding: const EdgeInsets.symmetric(vertical: 2),
                  decoration: BoxDecoration(
                    color: const Color(0xFF262628),
                    borderRadius: BorderRadius.circular(16),
                    boxShadow: const [BoxShadow(color: Color(0x66000000), blurRadius: 12, offset: Offset(0, 6))],
                  ),
                  child: Column(mainAxisSize: MainAxisSize.min, children: [
                    row(Icons.refresh_rounded, 'Refresh app', () => Notices.refreshApp(context)),
                    Container(height: 1, margin: const EdgeInsets.symmetric(horizontal: 12), color: const Color(0xFF3A3A3C)),
                    row(Icons.notifications_none_rounded, 'Updates',
                        () => Navigator.of(context).push(CupertinoPageRoute(builder: (_) => const UpdatesPage()))),
                  ]),
                ),
              ),
            ),
          ),
        ]),
      );
    },
  ));
}
