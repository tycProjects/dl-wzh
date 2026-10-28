import 'dart:math' as math;
import 'dart:ui' as ui;
import 'package:flutter/material.dart' hide Text;
import 'package:google_fonts/google_fonts.dart';
import 'theme.dart';
import 'anim.dart';
import 'tr.dart';

/// Startup animation (ported from wzh-startup.html). The W, Z and H are drawn by glowing pens,
/// guide lines and a blue block appear, water fills the letters, a shine sweeps over them, the
/// logo moves up and the wordmark "WZH Flutter IDE" fades in under it.
///
/// Timeline (seconds, same as the HTML): 0 .. 5.9 animation, then it holds on the final frame
/// while the home screen is built underneath, then 5.9 .. 6.7 exits and fades to REVEAL the
/// already-built home screen.
class IntroSplash extends StatefulWidget {
  /// True once the home screen is mounted under this splash and it is safe to fade out.
  final bool reveal;
  /// Called when the animation has reached its final frame (the home screen is mounted now).
  final VoidCallback onHold;
  final VoidCallback onDone;
  const IntroSplash({super.key, required this.reveal, required this.onHold, required this.onDone});
  @override
  State<IntroSplash> createState() => _IntroSplashState();
}

// ---------------------------------------------------------------- easing / helpers
const _out = Cubic(.22, .61, .36, 1);
const _io = Cubic(.65, .05, .36, 1);
const _ios = Cubic(.32, .72, 0, 1);
const _inn = Cubic(.42, 0, 1, 1);
const _back = Cubic(.175, .885, .32, 1.275);
const _lin = Curves.linear;

double _seg(double t, double s, double e, [Curve c = _out]) => c.transform(((t - s) / (e - s)).clamp(0.0, 1.0));

Color _rgba(int rgb, double a) => Color.fromRGBO((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255, a.clamp(0.0, 1.0));

const _blue = Color(0xFF4FD5A6);
const _total = 6.7; // seconds, whole animation
const _holdAt = 5.9; // seconds, where it waits for the home screen

class _IntroSplashState extends State<IntroSplash> with SingleTickerProviderStateMixin {
  static const _inMs = 5900, _outMs = 800;
  static const _hold = _holdAt / _total;

  late final AnimationController _c = AnimationController(vsync: this, duration: const Duration(milliseconds: 6700));
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
    // One short beat so the home screen has drawn its first frame before the exit starts.
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
    final mq = MediaQuery.of(context);
    final size = mq.size;
    return AbsorbPointer(
      child: Material(
        type: MaterialType.transparency,
        child: RepaintBoundary(
          child: AnimatedBuilder(
            animation: _c,
            builder: (_, __) {
              final T = _c.value * _total;
              final ex = _seg(T, 5.9, 6.5, _ios);
              final fade = 1 - _seg(T, 2.6, 3.15, _inn);
              final glow = (.3 * _seg(T, 0, 1.2) + .7 * _seg(T, 2.6, 3.5)) * (1 - ex);
              final w = _seg(T, 3.85, 4.65, _out);
              final wo = w * (1 - ex);
              final blur = (1 - w) * 8 + ex * 6;
              final ulW = 34 * _seg(T, 4.6, 5.0);
              final tag = _seg(T, 4.8, 5.3);
              final mk = _seg(T, 5.0, 5.6);
              final rootOp = 1 - _seg(T, 6.1, _total);

              Widget word = Opacity(
                opacity: wo.clamp(0.0, 1.0),
                child: Transform.translate(
                  offset: Offset(0, (1 - w) * 10),
                  child: Column(mainAxisSize: MainAxisSize.min, children: [
                    RichText(
                      textAlign: TextAlign.center,
                      text: TextSpan(
                        style: GoogleFonts.playfairDisplay(
                          fontSize: 30, fontWeight: FontWeight.w700, fontStyle: FontStyle.italic, height: 1.1,
                          color: const Color(0xFFF5F5F5), letterSpacing: (1 - w) * 6,
                          shadows: [Shadow(color: _rgba(0x4FD5A6, .35), blurRadius: 26, offset: const Offset(0, 2))],
                        ),
                        children: const [TextSpan(text: 'WZH '), TextSpan(text: 'Flutter IDE', style: TextStyle(color: _blue))],
                      ),
                    ),
                    Container(
                      width: ulW, height: 2, margin: const EdgeInsets.only(top: 9),
                      decoration: BoxDecoration(
                        color: _blue, borderRadius: BorderRadius.circular(1),
                        boxShadow: ulW > 0.5 ? [BoxShadow(color: _rgba(0x4FD5A6, .85), blurRadius: 14)] : null,
                      ),
                    ),
                    Opacity(
                      opacity: tag,
                      child: Padding(
                        padding: const EdgeInsets.only(top: 12),
                        child: Text('Flutter, right on your phone', style: inter(13, c: C.muted).copyWith(letterSpacing: .39)),
                      ),
                    ),
                  ]),
                ),
              );
              if (blur > .3 && wo > 0) word = ImageFiltered(imageFilter: ui.ImageFilter.blur(sigmaX: blur, sigmaY: blur), child: word);

              return Opacity(
                opacity: rootOp.clamp(0.0, 1.0),
                child: Stack(fit: StackFit.expand, clipBehavior: Clip.hardEdge, children: [
                  CustomPaint(painter: _BgPainter(glow)),
                  Center(child: SizedBox(width: 300, height: 162.5, child: CustomPaint(painter: _LogoPainter(T)))),
                  Positioned(top: size.height / 2 - 22, left: 0, right: 0, child: wo > 0 ? Center(child: word) : const SizedBox()),
                  Positioned(
                    left: 0, right: 0, bottom: mq.padding.bottom + 30,
                    child: Opacity(
                      opacity: mk,
                      child: RichText(
                        textAlign: TextAlign.center,
                        text: TextSpan(
                          text: 'Tycept',
                          style: inter(12, w: FontWeight.w500, c: _rgba(0xF5F5F5, .3)).copyWith(letterSpacing: 12 * .18),
                        ),
                      ),
                    ),
                  ),
                  // Only if loading takes longer than the animation (slow network).
                  if (_holding && !widget.reveal)
                    Positioned(left: 0, right: 0, bottom: mq.padding.bottom + 80, child: const Center(child: IosSpinner(size: 22))),
                ]),
              );
            },
          ),
        ),
      ),
    );
  }
}

/// Dark background with a soft vignette, plus the mint glow behind the logo.
class _BgPainter extends CustomPainter {
  final double glow;
  _BgPainter(this.glow);

  @override
  void paint(Canvas canvas, Size size) {
    canvas.drawRect(Offset.zero & size, Paint()..color = C.bg);
    // radial-gradient(120% 90% at 50% 42%, #1c1c1e, bg 70%)
    canvas.save();
    canvas.translate(size.width * .5, size.height * .42);
    canvas.scale(size.width * 1.2, size.height * .9);
    canvas.drawCircle(
      Offset.zero, .7,
      Paint()..shader = const RadialGradient(colors: [Color(0xFF1C1C1E), Color(0xFF111111)]).createShader(Rect.fromCircle(center: Offset.zero, radius: .7)),
    );
    canvas.restore();
    if (glow > 0) {
      // ellipse 65% 42% at 50% 48%: .2 -> .07 at 48% -> transparent at 78%
      canvas.save();
      canvas.translate(size.width * .5, size.height * .48);
      canvas.scale(size.width * .65, size.height * .42);
      canvas.drawCircle(
        Offset.zero, .78,
        Paint()
          ..shader = RadialGradient(
            colors: [_rgba(0x4FD5A6, .2 * glow), _rgba(0x4FD5A6, .07 * glow), _rgba(0x4FD5A6, 0)],
            stops: const [0, .48 / .78, 1],
          ).createShader(Rect.fromCircle(center: Offset.zero, radius: .78)),
      );
      canvas.restore();
    }
  }

  @override
  bool shouldRepaint(covariant _BgPainter old) => old.glow != glow;
}

// ---------------------------------------------------------------- the logo
typedef _P = List<double>;

class _Node {
  final double x, y, f;
  final int color, kind;
  _Node(this.x, this.y, this.color, this.kind, double f) : f = math.min(f, .93);
}

class _Geo {
  static const L = <String, List<_P>>{
    'W': [[16, 35], [29, 35], [38, 77], [49, 41], [59, 41], [70, 77], [79, 35], [92, 35], [78, 95], [65, 95], [54, 61], [43, 95], [30, 95]],
    'Z': [[104, 35], [158, 35], [158, 47], [124, 83], [158, 83], [158, 95], [104, 95], [104, 83], [138, 47], [104, 47]],
    'H': [[170, 35], [184, 35], [184, 57], [210, 57], [210, 35], [224, 35], [224, 95], [210, 95], [210, 73], [184, 73], [184, 95], [170, 95]],
  };
  static const Q = <_P>[[138, 47], [158, 47], [124, 83], [104, 83]];
  static const keys = ['W', 'Z', 'H'];
  static const ST = <double>[.4, .8, 1.15];

  static Path poly(List<_P> a) {
    final p = Path()..moveTo(a[0][0], a[0][1]);
    for (var i = 1; i < a.length; i++) {
      p.lineTo(a[i][0], a[i][1]);
    }
    return p..close();
  }

  static List<double> cum(List<_P> a) {
    var s = 0.0;
    final c = <double>[0];
    for (var i = 0; i < a.length; i++) {
      final q = a[(i + 1) % a.length];
      s += math.sqrt(math.pow(q[0] - a[i][0], 2) + math.pow(q[1] - a[i][1], 2));
      c.add(s);
    }
    return c.map((x) => x / s).toList();
  }

  static final letterPaths = [for (final k in keys) poly(L[k]!)];
  static final letterMetrics = [for (final p in letterPaths) p.computeMetrics().first];
  static final qPath = poly(Q);
  static final qMetric = qPath.computeMetrics().first;
  static final allLetters = Path()..addPath(letterPaths[0], Offset.zero)..addPath(letterPaths[1], Offset.zero)..addPath(letterPaths[2], Offset.zero);
  static final qBounds = qPath.getBounds();
  static final letterBounds = [for (final p in letterPaths) p.getBounds()];

  static final nodes = () {
    final n = <_Node>[];
    for (var i = 0; i < 3; i++) {
      final a = L[keys[i]]!, c = cum(a);
      for (var j = 0; j < a.length; j++) {
        n.add(_Node(a[j][0], a[j][1], 0xFFFFFF, i, c[j]));
      }
    }
    n.add(_Node(16, 47, 0x8EE9CB, 3, 0));
    n.add(_Node(224, 47, 0x8EE9CB, 3, 1));
    n.add(_Node(16, 83, 0x8EE9CB, 4, 0));
    n.add(_Node(224, 83, 0x8EE9CB, 4, 1));
    final cq = cum(Q);
    for (var j = 0; j < Q.length; j++) {
      n.add(_Node(Q[j][0], Q[j][1], 0x8EE9CB, 5, cq[j]));
    }
    return n;
  }();

  static double prog(int kind, double T) {
    switch (kind) {
      case 0:
      case 1:
      case 2:
        return _seg(T, ST[kind], ST[kind] + .95, _io);
      case 3:
        return _seg(T, 1.35, 2, _io);
      case 4:
        return _seg(T, 1.55, 2.2, _io);
      default:
        return _seg(T, 1.7, 2.4, _io);
    }
  }
}

class _Head {
  final double x, y, a, gr, cr;
  _Head(this.x, this.y, this.a, this.gr, this.cr);
}

class _LogoPainter extends CustomPainter {
  final double T;
  _LogoPainter(this.T);

  static const _white = LinearGradient(begin: Alignment.topCenter, end: Alignment.bottomCenter, colors: [Color(0xFFF5F5F5), Color(0xFFD2D6D9)]);
  static const _blueG = LinearGradient(begin: Alignment.topLeft, end: Alignment.bottomRight, stops: [0, .6, 1], colors: [Color(0xFFB4F2DC), Color(0xFF4FD5A6), Color(0xFF2FB98A)]);

  Path _wave(double y, double w) {
    final A = 2.6 * math.sin(math.pi * w);
    final p = Path()..moveTo(-5, 135);
    for (var x = -5.0; x <= 245; x += 5) {
      p.lineTo(x, y + A * math.sin(x / 16 + T * 7));
    }
    return p..lineTo(245, 135)..close();
  }

  @override
  void paint(Canvas canvas, Size size) {
    final fade = 1 - _seg(T, 2.6, 3.15, _inn);
    final ex = _seg(T, 5.9, 6.5, _ios);
    final lt = _seg(T, 3.75, 4.6, _io);
    final sc = 1 - .4 * lt - .1 * ex + .03 * math.sin(math.pi * _seg(T, 3, 3.6, _lin));
    final vis = 1 - ex;
    if (vis <= 0) return;

    canvas.save();
    if (vis < 1) canvas.saveLayer(const Rect.fromLTWH(-600, -600, 1500, 1500), Paint()..color = Color.fromRGBO(255, 255, 255, vis));
    canvas.translate(size.width / 2, size.height / 2 - 74 * lt);
    canvas.scale(sc);
    canvas.translate(-size.width / 2, -size.height / 2);
    canvas.scale(size.width / 240); // viewBox 240 x 130

    final heads = <_Head>[];
    void head(double x, double y, double a, double gr, double cr) {
      if (heads.length < 40) heads.add(_Head(x, y, a, gr, cr));
    }

    Paint stroke(Color c, double w) => Paint()
      ..style = PaintingStyle.stroke
      ..strokeWidth = w
      ..strokeJoin = StrokeJoin.round
      ..color = c;

    // initial pen dot
    if (T < .45) {
      final a = _seg(T, .15, .45);
      head(16, 35, a, 6 + 2 * math.sin(T * 30), 1.6 * a);
    }

    // guide lines + the blue block outline
    final g1 = _seg(T, 1.35, 2, _io), g2 = _seg(T, 1.55, 2.2, _io), gq = _seg(T, 1.7, 2.4, _io);
    final gl = stroke(_rgba(0x8EE9CB, .8 * fade), .8);
    for (final e in [(47.0, g1), (83.0, g2)]) {
      if (e.$2 > 0) canvas.drawLine(Offset(16, e.$1), Offset(16 + 208 * e.$2, e.$1), gl);
      if (e.$2 > 0 && e.$2 < 1) head(16 + 208 * e.$2, e.$1, 1, 7, 1.5);
    }
    if (gq > 0) {
      final m = _Geo.qMetric;
      canvas.drawPath(m.extractPath(0, m.length * gq), stroke(_rgba(0x8EE9CB, fade), .9));
      if (gq < 1) {
        final q = m.getTangentForOffset(gq * m.length)!.position;
        head(q.dx, q.dy, 1, 7, 1.5);
      }
    }

    // white letters, filled by water
    final w1 = _seg(T, 2.15, 3.05, _io), w2 = _seg(T, 2.5, 3.35, _io);
    if (w1 > 0) {
      canvas.save();
      canvas.clipPath(_wave(100 - 76 * w1, w1));
      for (var i = 0; i < 3; i++) {
        final sh = _white.createShader(_Geo.letterBounds[i]);
        canvas.drawPath(_Geo.letterPaths[i], Paint()..shader = sh);
        canvas.drawPath(_Geo.letterPaths[i], Paint()..shader = sh..style = PaintingStyle.stroke..strokeWidth = 1.6..strokeJoin = StrokeJoin.round);
      }
      canvas.restore();
    }
    // blue block, filled by water a little later
    if (w2 > 0) {
      canvas.save();
      canvas.clipPath(_wave(100 - 76 * w2, w2));
      final sh = _blueG.createShader(_Geo.qBounds);
      canvas.drawPath(_Geo.qPath, Paint()..shader = sh);
      canvas.drawPath(_Geo.qPath, Paint()..shader = sh..style = PaintingStyle.stroke..strokeWidth = 1.6..strokeJoin = StrokeJoin.round);
      canvas.restore();
    }

    // shine sweeping over the letters
    final s = _seg(T, 3.15, 3.85, _io);
    if (s > 0 && s < 1) {
      final sx = -60 + 340 * s;
      canvas.save();
      canvas.clipPath(_Geo.allLetters);
      final poly = Path()
        ..moveTo(sx, 20)
        ..lineTo(sx + 36, 20)
        ..lineTo(sx + 14, 110)
        ..lineTo(sx - 22, 110)
        ..close();
      canvas.drawPath(
        poly,
        Paint()..shader = LinearGradient(colors: [_rgba(0xFFFFFF, 0), _rgba(0xFFFFFF, .6), _rgba(0xFFFFFF, 0)]).createShader(Rect.fromLTRB(sx - 22, 20, sx + 36, 110)),
      );
      canvas.restore();
    }

    // glowing outlines drawn by the pens
    for (var i = 0; i < 3; i++) {
      final t = _seg(T, _Geo.ST[i], _Geo.ST[i] + .95, _io);
      if (t <= 0) continue;
      final m = _Geo.letterMetrics[i];
      final part = m.extractPath(0, m.length * t);
      canvas.drawPath(part, stroke(_rgba(0x8EE9CB, .3 * fade), 3.2));
      canvas.drawPath(part, stroke(_rgba(0xFFFFFF, .95 * fade), 1.1));
      if (t < 1) {
        for (var j = 0; j < 8; j++) {
          final q = m.getTangentForOffset(math.max(0.0, t - j * .022) * m.length)!.position;
          final f = 1 - j / 8;
          head(q.dx, q.dy, f, j > 0 ? 5 * f : 8, j > 0 ? 1.2 * f : 1.7);
        }
      }
    }

    // small square nodes at the corners
    final nodeFill = Paint()..style = PaintingStyle.fill;
    for (final n in _Geo.nodes) {
      final v = _seg(_Geo.prog(n.kind, T), n.f, n.f + .07, _back);
      if (v <= 0 || fade <= 0) continue;
      final sz = 3.6 * v;
      final r = Rect.fromCenter(center: Offset(n.x, n.y), width: sz, height: sz);
      canvas.drawRect(r, nodeFill..color = _rgba(0x111111, fade));
      canvas.drawRect(r, stroke(_rgba(n.color, fade), .9)..strokeJoin = StrokeJoin.miter);
    }

    // glowing pen heads on top of everything
    for (final h in heads) {
      canvas.drawCircle(
        Offset(h.x, h.y), h.gr,
        Paint()..shader = RadialGradient(colors: [_rgba(0xB4F2DC, .9 * h.a), _rgba(0xB4F2DC, 0)]).createShader(Rect.fromCircle(center: Offset(h.x, h.y), radius: h.gr)),
      );
      canvas.drawCircle(Offset(h.x, h.y), h.cr, Paint()..color = _rgba(0xFFFFFF, h.a));
    }

    if (vis < 1) canvas.restore();
    canvas.restore();
  }

  @override
  bool shouldRepaint(covariant _LogoPainter old) => old.T != T;
}
