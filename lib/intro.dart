import 'package:flutter/material.dart' hide Text;
import 'theme.dart';
import 'anim.dart';
import 'tr.dart';

/// Startup animation in the app's own look. The story: the cube's three faces assemble, the four
/// scan brackets close in and lock (the cube squashes and springs back, a shine sweeps over it,
/// one ripple goes out), then the name appears letter by letter.
/// Timeline: 1.3 s of animation, a short beat while the home screen is built underneath, then a
/// 0.3 s fade that REVEALS the already-built home screen. Total about 1.7 s.
class IntroSplash extends StatefulWidget {
  /// True once the home screen is mounted under this splash and it is safe to fade out.
  final bool reveal;
  /// Called when the 1.3 s of animation are done (the home screen is mounted now).
  final VoidCallback onHold;
  final VoidCallback onDone;
  const IntroSplash({super.key, required this.reveal, required this.onHold, required this.onDone});
  @override
  State<IntroSplash> createState() => _IntroSplashState();
}

double _seg(double u, double s, double e, [Curve c = Curves.easeOutCubic]) =>
    c.transform(((u - s) / (e - s)).clamp(0.0, 1.0));

Color _rgba(int rgb, double a) => Color.fromRGBO((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255, a.clamp(0.0, 1.0));

class _IntroSplashState extends State<IntroSplash> with SingleTickerProviderStateMixin {
  static const _inMs = 1300, _outMs = 300;
  static const _hold = _inMs / (_inMs + _outMs); // controller value where the animation pauses
    static const _word = 'WZH 2 APK';

  late final AnimationController _c = AnimationController(vsync: this, duration: const Duration(milliseconds: _inMs + _outMs));
  late final Animation<double> _fade = Tween<double>(begin: 1, end: 0)
      .animate(CurvedAnimation(parent: _c, curve: const Interval(_hold, 1.0, curve: Curves.easeOut)));
  bool _holding = false, _exiting = false;

  @override
  void initState() {
    super.initState();
    // Wait for the first frame so the animation never starts mid-build.
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!mounted) return;
      _c.animateTo(_hold, duration: const Duration(milliseconds: _inMs), curve: Curves.linear).whenComplete(() {
        if (!mounted) return;
        setState(() => _holding = true);
        widget.onHold();
        _maybeExit();
      });
    });
  }

  @override
  void didUpdateWidget(covariant IntroSplash old) {
    super.didUpdateWidget(old);
    _maybeExit();
  }

  void _maybeExit() {
    if (!_holding || _exiting || !widget.reveal) return;
    _exiting = true;
    // One short beat so the home screen has drawn its first frame before the fade starts.
    Future.delayed(const Duration(milliseconds: 100), () {
      if (!mounted) return;
      _c.animateTo(1.0, duration: const Duration(milliseconds: _outMs), curve: Curves.linear).whenComplete(() {
        if (mounted) widget.onDone();
      });
    });
  }

  @override
  void dispose() {
    _c.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return FadeTransition(
      opacity: _fade,
      child: Material(
        color: C.bg,
        child: RepaintBoundary(
          child: AnimatedBuilder(
            animation: _c,
            builder: (_, __) {
              final u = (_c.value / _hold).clamp(0.0, 1.0);
              final tileIn = _seg(u, 0, .3);
              final tag = _seg(u, .80, .95);
              return Center(
                child: Column(mainAxisSize: MainAxisSize.min, children: [
                  SizedBox(
                    width: 190, height: 190,
                    child: Stack(alignment: Alignment.center, clipBehavior: Clip.none, children: [
                      CustomPaint(size: const Size(190, 190), painter: _FxPainter(u)),
                      Transform.scale(
                        scale: .9 + .1 * tileIn,
                        child: Opacity(opacity: _seg(u, 0, .15, Curves.easeOut), child: CustomPaint(size: const Size(132, 132), painter: _TilePainter(u))),
                      ),
                    ]),
                  ),
                  const SizedBox(height: 14),
                  Row(mainAxisSize: MainAxisSize.min, children: [
                    for (var i = 0; i < _word.length; i++)
                      Builder(builder: (_) {
                        final t = _seg(u, .55 + i * .022, .55 + i * .022 + .14);
                        return Opacity(
                          opacity: t,
                          child: Transform.translate(offset: Offset(0, (1 - t) * 14), child: Text(_word[i], style: inter(32, w: FontWeight.w800))),
                        );
                      }),
                  ]),
                  const SizedBox(height: 6),
                  Opacity(
                    opacity: tag,
                    child: Transform.translate(offset: Offset(0, (1 - tag) * 8), child: Text('HTML to APK, simplified', style: inter(15, c: C.muted))),
                  ),
                  // Only if loading takes longer than the animation (slow network).
                  SizedBox(height: 44, child: Center(child: _holding && !widget.reveal ? const IosSpinner(size: 22) : null)),
                ]),
              );
            },
          ),
        ),
      ),
    );
  }
}

/// Soft glow behind the tile and the single ripple that goes out when the arrow lands.
class _FxPainter extends CustomPainter {
  final double u;
  _FxPainter(this.u);

  @override
  void paint(Canvas canvas, Size size) {
    final c = size.center(Offset.zero);
    final glow = _seg(u, 0, .5, Curves.easeOut);
    if (glow > 0) {
      const r = 126.0;
      canvas.drawCircle(
        c, r,
        Paint()..shader = RadialGradient(colors: [_rgba(0x4FD5A6, .18 * glow), _rgba(0x4FD5A6, 0)]).createShader(Rect.fromCircle(center: c, radius: r)),
      );
    }
    final rp = _seg(u, .46, .88);
    if (rp > 0 && rp < 1) {
      canvas.drawCircle(
        c, 60 + 44 * rp,
        Paint()
          ..style = PaintingStyle.stroke
          ..strokeWidth = 2.5
          ..color = _rgba(0x4FD5A6, .5 * (1 - rp)),
      );
    }
  }

  @override
  bool shouldRepaint(covariant _FxPainter old) => old.u != u;
}

/// The logo: three cube faces assemble, four scan brackets close in and lock around it (the cube
/// squashes and springs back at that moment), then a shine sweeps across the cube.
class _TilePainter extends CustomPainter {
  final double u;
  _TilePainter(this.u);

  @override
  void paint(Canvas canvas, Size size) {
    final s = size.width;
    Offset p(double x, double y) => Offset(x * s, y * s);
    Path face(List<Offset> pts) {
      final path = Path()..moveTo(pts[0].dx, pts[0].dy);
      for (final q in pts.skip(1)) {
        path.lineTo(q.dx, q.dy);
      }
      return path..close();
    }

    // Coordinates are the icon's own, as fractions of its 1024 px square.
    final top = [p(.5, .290), p(.676, .392), p(.5, .493), p(.324, .392)];
    final left = [p(.318, .402), p(.494, .503), p(.494, .706), p(.318, .604)];
    final right = [p(.507, .503), p(.682, .402), p(.682, .604), p(.507, .706)];

    // Squash when the brackets lock, then spring back.
    final ramp = _seg(u, .44, .50, Curves.easeOut);
    final settle = Curves.elasticOut.transform(_seg(u, .50, .76, Curves.linear));
    final k = ramp * (1 - settle);

    canvas.save();
    canvas.translate(s * .5, s * .706);
    canvas.scale(1 + .06 * k, 1 - .10 * k);
    canvas.translate(-s * .5, -s * .706);

    // Three faces fly in and assemble (left, right, then top).
    final faces = <(List<Offset>, int, double, Offset)>[
      (left, 0x70E4C1, 0.0, const Offset(-.12, .05)),
      (right, 0x33A482, .06, const Offset(.12, .05)),
      (top, 0xF8F9F2, .12, const Offset(0, -.14)),
    ];
    final shape = Path();
    for (final f in faces) {
      final t = _seg(u, f.$3, f.$3 + .26);
      if (t <= 0) continue;
      canvas.save();
      canvas.translate(f.$4.dx * (1 - t) * s, f.$4.dy * (1 - t) * s);
      final path = face(f.$1);
      canvas.drawPath(path, Paint()..color = _rgba(f.$2, t));
      canvas.restore();
      if (t >= 1) shape.addPath(path, Offset.zero);
    }

    // A diagonal shine sweeps across the cube after the landing.
    final sh = _seg(u, .52, .80, Curves.easeInOut);
    if (sh > 0 && sh < 1) {
      canvas.save();
      canvas.clipPath(shape);
      final x = -.4 * s + 1.6 * s * sh;
      final w = .30 * s;
      canvas.drawPath(
        Path()
          ..moveTo(x, 0)
          ..lineTo(x + w, 0)
          ..lineTo(x + w - .2 * s, s)
          ..lineTo(x - .2 * s, s)
          ..close(),
        Paint()..shader = LinearGradient(colors: [_rgba(0xFFFFFF, 0), _rgba(0xFFFFFF, .45), _rgba(0xFFFFFF, 0)]).createShader(Rect.fromLTWH(x - .2 * s, 0, w + .2 * s, s)),
      );
      canvas.restore();
    }
    canvas.restore();

    // The four brackets start wide and faint, speed in and lock; a small give when they land.
    final a = _seg(u, .20, .34, Curves.easeOut);
    if (a > 0) {
      final fall = Curves.easeInCubic.transform(_seg(u, .28, .46, Curves.linear));
      final sc = 1 + .22 * (1 - fall) - .035 * k;
      canvas.save();
      canvas.translate(s * .5, s * .5);
      canvas.scale(sc);
      canvas.translate(-s * .5, -s * .5);
      final bp = Paint()
        ..style = PaintingStyle.stroke
        ..strokeWidth = s * .0254
        ..strokeCap = StrokeCap.round
        ..strokeJoin = StrokeJoin.round
        ..color = _rgba(0x4FD5A6, a);
      const near = .2168, far = .7832, arm = .1025;
      for (final c in const [(near, near, 1.0, 1.0), (far, near, -1.0, 1.0), (near, far, 1.0, -1.0), (far, far, -1.0, -1.0)]) {
        canvas.drawPath(
          Path()
            ..moveTo((c.$1 + c.$3 * arm) * s, c.$2 * s)
            ..lineTo(c.$1 * s, c.$2 * s)
            ..lineTo(c.$1 * s, (c.$2 + c.$4 * arm) * s),
          bp,
        );
      }
      canvas.restore();
    }
  }

  @override
  bool shouldRepaint(covariant _TilePainter old) => old.u != u;
}
