import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

/// The app's startup animation: hills rise, a seedling grows, the scenery pops in and the
/// name fades up. Exactly 1.7 seconds, one controller, a handful of cheap draw calls, so it
/// stays smooth on weak phones.
const kIntroDuration = Duration(milliseconds: 1700);

class IntroSplash extends StatefulWidget {
  final VoidCallback onDone;
  const IntroSplash({super.key, required this.onDone});
  @override
  State<IntroSplash> createState() => _IntroSplashState();
}

class _IntroSplashState extends State<IntroSplash> with SingleTickerProviderStateMixin {
  late final AnimationController _c = AnimationController(vsync: this, duration: kIntroDuration);

  static double _seg(double t, double s, double e, [Curve c = Curves.easeOutCubic]) =>
      c.transform(((t - s) / (e - s)).clamp(0.0, 1.0));

  @override
  void initState() {
    super.initState();
    // Light scene: dark status-bar icons while it plays (main.dart's style is put back afterwards).
    SystemChrome.setSystemUIOverlayStyle(const SystemUiOverlayStyle(
      statusBarColor: Colors.transparent,
      statusBarIconBrightness: Brightness.dark,
      systemNavigationBarColor: Color(0xFF4B8A3F),
      systemNavigationBarIconBrightness: Brightness.light,
    ));
    _c.addStatusListener((s) {
      if (s == AnimationStatus.completed) widget.onDone();
    });
    // Start after the first frame so the very first frame is never skipped.
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted) _c.forward();
    });
  }

  @override
  void dispose() {
    SystemChrome.setSystemUIOverlayStyle(const SystemUiOverlayStyle(
      statusBarColor: Colors.transparent,
      statusBarIconBrightness: Brightness.light,
      systemNavigationBarColor: Color(0xFF161616),
      systemNavigationBarIconBrightness: Brightness.light,
    ));
    _c.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFFFBF7EA),
      body: Stack(fit: StackFit.expand, children: [
        RepaintBoundary(child: CustomPaint(painter: _ScenePainter(_c), size: Size.infinite)),
        Align(
          alignment: const Alignment(0, .1),
          child: AnimatedBuilder(
            animation: _c,
            builder: (_, __) {
              final a = _seg(_c.value, .55, .78, Curves.easeOut);
              final b = _seg(_c.value, .65, .86, Curves.easeOut);
              return Column(mainAxisSize: MainAxisSize.min, children: [
                Opacity(
                  opacity: a,
                  child: Transform.translate(
                    offset: Offset(0, (1 - a) * 10),
                    child: const Text('WZH 2 APK',
                        style: TextStyle(fontSize: 34, fontWeight: FontWeight.w800, letterSpacing: -.6, color: Color(0xFF1F2A24), decoration: TextDecoration.none)),
                  ),
                ),
                const SizedBox(height: 6),
                Opacity(
                  opacity: b,
                  child: const Text('HTML to APK, simplified',
                      style: TextStyle(fontSize: 14, fontWeight: FontWeight.w500, color: Color(0xFF8A8A80), decoration: TextDecoration.none)),
                ),
              ]);
            },
          ),
        ),
        // Fades the whole scene out into the app's own background at the very end.
        Positioned.fill(
          child: IgnorePointer(
            child: AnimatedBuilder(
              animation: _c,
              builder: (_, __) {
                final e = _seg(_c.value, .88, 1.0, Curves.easeIn);
                return ColoredBox(color: Color.fromRGBO(17, 17, 17, e));
              },
            ),
          ),
        ),
      ]),
    );
  }
}

class _ScenePainter extends CustomPainter {
  final Animation<double> a;
  _ScenePainter(this.a) : super(repaint: a);

  Size? _sized;
  Paint _sky = Paint();

  static double _seg(double t, double s, double e, [Curve c = Curves.easeOutCubic]) =>
      c.transform(((t - s) / (e - s)).clamp(0.0, 1.0));

  @override
  void paint(Canvas canvas, Size size) {
    final t = a.value;
    final w = size.width, h = size.height;
    final rect = Offset.zero & size;

    if (_sized != size) {
      _sized = size;
      _sky = Paint()
        ..shader = const LinearGradient(
          begin: Alignment.topCenter,
          end: Alignment.bottomCenter,
          colors: [Color(0xFFE6F2EF), Color(0xFFFBF7EA)],
        ).createShader(rect);
    }
    canvas.drawRect(rect, _sky);

    final cx = w / 2;
    final my = h * .47; // soil mound

    // Warm glow behind the seedling.
    final g = _seg(t, .25, .65, Curves.easeOut);
    if (g > 0) {
      final r = w * .42 * (.7 + .3 * g);
      canvas.drawCircle(
        Offset(cx, h * .41),
        r,
        Paint()
          ..shader = RadialGradient(colors: [
            Color.fromRGBO(255, 226, 160, .55 * g),
            const Color.fromRGBO(255, 226, 160, 0),
          ]).createShader(Rect.fromCircle(center: Offset(cx, h * .41), radius: r)),
      );
    }

    // Hills rise from the bottom.
    final dy = (1 - _seg(t, 0, .30)) * h * .30;
    final back = Path()
      ..moveTo(0, h * .80 + dy)
      ..quadraticBezierTo(w * .28, h * .72 + dy, w * .55, h * .78 + dy)
      ..quadraticBezierTo(w * .80, h * .83 + dy, w, h * .76 + dy)
      ..lineTo(w, h)
      ..lineTo(0, h)
      ..close();
    canvas.drawPath(back, Paint()..color = const Color(0xFF9BCB84));
    final front = Path()
      ..moveTo(0, h * .86 + dy)
      ..quadraticBezierTo(w * .35, h * .80 + dy, w * .65, h * .87 + dy)
      ..quadraticBezierTo(w * .85, h * .90 + dy, w, h * .85 + dy)
      ..lineTo(w, h)
      ..lineTo(0, h)
      ..close();
    canvas.drawPath(front, Paint()..color = const Color(0xFF5E9E4F));

    // Scenery pops in, anchored on the hills.
    final sc = _seg(t, .38, .70, Curves.easeOutBack);
    if (sc > 0) {
      // Left: a round tree and a pine.
      canvas.save();
      canvas.translate(w * .13, h * .83);
      canvas.scale(sc);
      canvas.drawRRect(
          RRect.fromRectAndRadius(Rect.fromLTRB(-w * .006, -w * .05, w * .006, 0), const Radius.circular(2)),
          Paint()..color = const Color(0xFF7A5230));
      canvas.drawCircle(Offset(0, -w * .075), w * .045, Paint()..color = const Color(0xFF3F8F43));
      canvas.drawCircle(Offset(-w * .02, -w * .055), w * .03, Paint()..color = const Color(0xFF4FA24D));
      canvas.restore();

      canvas.save();
      canvas.translate(w * .22, h * .84);
      canvas.scale(sc);
      canvas.drawPath(
          Path()
            ..moveTo(0, -w * .12)
            ..lineTo(w * .03, 0)
            ..lineTo(-w * .03, 0)
            ..close(),
          Paint()..color = const Color(0xFF2F7A3A));
      canvas.restore();

      // Right: silo and barn.
      canvas.save();
      canvas.translate(w * .86, h * .81);
      canvas.scale(sc);
      canvas.drawRect(Rect.fromLTWH(w * .045, -w * .09, w * .03, w * .09), Paint()..color = const Color(0xFFC9CDD0));
      canvas.drawArc(Rect.fromLTWH(w * .045, -w * .105, w * .03, w * .03), 3.14159, 3.14159, true, Paint()..color = const Color(0xFF9EA4A8));
      canvas.drawRect(Rect.fromLTWH(-w * .06, -w * .065, w * .10, w * .065), Paint()..color = const Color(0xFFC0392B));
      canvas.drawPath(
          Path()
            ..moveTo(-w * .07, -w * .065)
            ..lineTo(-w * .01, -w * .105)
            ..lineTo(w * .05, -w * .065)
            ..close(),
          Paint()..color = const Color(0xFF8E2A20));
      canvas.drawRect(Rect.fromLTWH(-w * .026, -w * .04, w * .032, w * .04), Paint()..color = const Color(0xFFF5E9DD));
      canvas.restore();
    }

    // Soil mound.
    final m = _seg(t, .08, .25, Curves.easeOutBack);
    if (m > 0) {
      canvas.save();
      canvas.translate(cx, my);
      canvas.scale(m);
      canvas.drawOval(Rect.fromCenter(center: Offset.zero, width: w * .13, height: w * .036), Paint()..color = const Color(0xFF8A5A3A));
      canvas.restore();
    }

    // Stem grows up out of the mound.
    final s = _seg(t, .18, .50);
    final top = Offset(cx, my - w * .13 * s);
    if (s > 0) {
      canvas.drawLine(
          Offset(cx, my), top, Paint()
            ..color = const Color(0xFF2F8F4E)
            ..strokeWidth = 4
            ..strokeCap = StrokeCap.round);
    }

    // Two leaves open at the top.
    final lv = _seg(t, .40, .65, Curves.easeOutBack);
    if (lv > 0) {
      final lw = w * .07;
      final leaf = Path()
        ..moveTo(0, 0)
        ..quadraticBezierTo(lw * .5, -lw * .9, lw * 1.3, -lw * .35)
        ..quadraticBezierTo(lw * .7, lw * .35, 0, 0)
        ..close();
      final paint = Paint()..color = const Color(0xFF3FA55B);
      canvas.save();
      canvas.translate(top.dx, top.dy);
      canvas.scale(lv);
      canvas.save();
      canvas.rotate(-.35);
      canvas.drawPath(leaf, paint);
      canvas.restore();
      canvas.scale(-1, 1);
      canvas.rotate(-.1);
      canvas.drawPath(leaf, paint..color = const Color(0xFF2F8F4E));
      canvas.restore();
    }

    // Two small birds.
    final b = _seg(t, .5, .8, Curves.easeOut);
    if (b > 0) {
      final bp = Paint()
        ..color = Color.fromRGBO(70, 70, 70, .55 * b)
        ..style = PaintingStyle.stroke
        ..strokeWidth = 1.6
        ..strokeCap = StrokeCap.round;
      for (final o in [Offset(w * .28, h * .30), Offset(w * .35, h * .275)]) {
        canvas.drawPath(
            Path()
              ..moveTo(o.dx - 7, o.dy)
              ..quadraticBezierTo(o.dx - 3.5, o.dy - 5, o.dx, o.dy)
              ..quadraticBezierTo(o.dx + 3.5, o.dy - 5, o.dx + 7, o.dy),
            bp);
      }
    }
  }

  @override
  bool shouldRepaint(covariant _ScenePainter old) => false;
}
