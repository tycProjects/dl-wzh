import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'theme.dart';
import 'anim.dart';

/// Startup animation in the app's own look. The story: a package box assembles, an arrow drops
/// in and lands (the box squashes and springs back, a shine sweeps over the tile, one ripple
/// goes out), then the name appears letter by letter.
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
  static const _landing = .46; // the moment the arrow lands (animation progress)
  static const _word = 'WZH 2 APK';

  late final AnimationController _c = AnimationController(vsync: this, duration: const Duration(milliseconds: _inMs + _outMs));
  late final Animation<double> _fade = Tween<double>(begin: 1, end: 0)
      .animate(CurvedAnimation(parent: _c, curve: const Interval(_hold, 1.0, curve: Curves.easeOut)));
  bool _holding = false, _exiting = false, _buzzed = false;

  @override
  void initState() {
    super.initState();
    _c.addListener(() {
      if (!_buzzed && _c.value / _hold >= _landing) {
        _buzzed = true;
        HapticFeedback.lightImpact(); // a tiny tap when the arrow lands
      }
    });
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
                    width: 180, height: 180,
                    child: Stack(alignment: Alignment.center, clipBehavior: Clip.none, children: [
                      CustomPaint(size: const Size(180, 180), painter: _FxPainter(u)),
                      Transform.scale(
                        scale: .88 + .12 * tileIn,
                        child: Opacity(opacity: _seg(u, 0, .15, Curves.easeOut), child: CustomPaint(size: const Size(104, 104), painter: _TilePainter(u))),
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
      const r = 120.0;
      canvas.drawCircle(
        c, r,
        Paint()..shader = RadialGradient(colors: [_rgba(0x4FD5A6, .18 * glow), _rgba(0x4FD5A6, 0)]).createShader(Rect.fromCircle(center: c, radius: r)),
      );
    }
    final rp = _seg(u, .46, .88);
    if (rp > 0 && rp < 1) {
      canvas.drawCircle(
        c, 56 + 40 * rp,
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

/// The tile with the package-box logo: faces assemble, the arrow lands, the box squashes and
/// springs back, a shine sweeps across.
class _TilePainter extends CustomPainter {
  final double u;
  _TilePainter(this.u);

  @override
  void paint(Canvas canvas, Size size) {
    final s = size.width;
    final rr = RRect.fromRectAndRadius(Offset.zero & size, Radius.circular(s * .29));
    canvas.drawRRect(rr, Paint()..color = const Color(0xFF1F3B32));
    canvas.drawRRect(
      rr.deflate(.75),
      Paint()
        ..style = PaintingStyle.stroke
        ..strokeWidth = 1.5
        ..color = const Color(0x594FD5A6),
    );

    Offset p(double x, double y) => Offset(x * s, y * s);
    Path face(List<Offset> pts) {
      final path = Path()..moveTo(pts[0].dx, pts[0].dy);
      for (final q in pts.skip(1)) {
        path.lineTo(q.dx, q.dy);
      }
      return path..close();
    }

    final top = p(.5, .36), ur = p(.725, .49), lr = p(.725, .75), bt = p(.5, .88), ll = p(.275, .75), ul = p(.275, .49), c = p(.5, .62);

    // Squash when the arrow lands, then spring back.
    final ramp = _seg(u, .44, .50, Curves.easeOut);
    final settle = Curves.elasticOut.transform(_seg(u, .50, .76, Curves.linear));
    final k = ramp * (1 - settle);
    canvas.save();
    canvas.translate(s * .5, s * .88);
    canvas.scale(1 + .06 * k, 1 - .10 * k);
    canvas.translate(-s * .5, -s * .88);

    // Three faces fly in and assemble (left, right, then top).
    final faces = <(List<Offset>, int, double, Offset)>[
      ([ul, c, bt, ll], 0x4FD5A6, 0.0, const Offset(-.12, .05)),
      ([ur, lr, bt, c], 0x2FA882, .06, const Offset(.12, .05)),
      ([top, ur, c, ul], 0x8CF0D6, .12, const Offset(0, -.14)),
    ];
    for (final f in faces) {
      final t = _seg(u, f.$3, f.$3 + .26);
      if (t <= 0) continue;
      canvas.save();
      canvas.translate(f.$4.dx * (1 - t) * s, f.$4.dy * (1 - t) * s);
      final path = face(f.$1);
      canvas.drawPath(path, Paint()..color = _rgba(f.$2, t));
      canvas.drawPath(
        path,
        Paint()
          ..style = PaintingStyle.stroke
          ..strokeWidth = s * .014
          ..strokeJoin = StrokeJoin.round
          ..color = _rgba(0x1F3B32, t),
      );
      canvas.restore();
    }
    canvas.restore();

    // The arrow speeds up and lands.
    final a = _seg(u, .28, .36, Curves.easeOut);
    if (a > 0) {
      final fall = Curves.easeInCubic.transform(_seg(u, .28, .46, Curves.linear));
      final dy = -.32 * s * (1 - fall);
      final ap = Paint()
        ..style = PaintingStyle.stroke
        ..strokeWidth = s * .05
        ..strokeCap = StrokeCap.round
        ..strokeJoin = StrokeJoin.round
        ..color = _rgba(0xF5F5F5, a);
      canvas.drawLine(p(.5, .10) + Offset(0, dy), p(.5, .24) + Offset(0, dy), ap);
      canvas.drawPath(
        Path()
          ..moveTo(p(.41, .20).dx, p(.41, .20).dy + dy)
          ..lineTo(p(.5, .29).dx, p(.5, .29).dy + dy)
          ..lineTo(p(.59, .20).dx, p(.59, .20).dy + dy),
        ap,
      );
    }

    // A diagonal shine sweeps across the tile after the landing.
    final sh = _seg(u, .52, .80, Curves.easeInOut);
    if (sh > 0 && sh < 1) {
      canvas.save();
      canvas.clipRRect(rr);
      final x = -.4 * s + 1.6 * s * sh;
      final w = .30 * s;
      canvas.drawPath(
        Path()
          ..moveTo(x, 0)
          ..lineTo(x + w, 0)
          ..lineTo(x + w - .2 * s, s)
          ..lineTo(x - .2 * s, s)
          ..close(),
        Paint()..shader = LinearGradient(colors: [_rgba(0xFFFFFF, 0), _rgba(0xFFFFFF, .22), _rgba(0xFFFFFF, 0)]).createShader(Rect.fromLTWH(x - .2 * s, 0, w + .2 * s, s)),
      );
      canvas.restore();
    }
  }

  @override
  bool shouldRepaint(covariant _TilePainter old) => old.u != u;
}
