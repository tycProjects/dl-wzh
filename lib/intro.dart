import 'dart:async';
import 'dart:math' as math;
import 'package:flutter/material.dart' hide Text;
import 'package:flutter/scheduler.dart' show SchedulerBinding;
import 'theme.dart';
import 'anim.dart';
import 'tr.dart';

/// Startup animation, about 3 seconds, calm and flat. One dark colour, the name fades in, a mint comet
/// sweeps in an arc over it and ends in a small sparkle, the tagline fades in, and the whole splash
/// dissolves into the home screen.
///
/// Timeline (seconds): 0 .. 2.4 animation, then it holds on the final frame while the home screen is
/// built underneath, then 2.4 .. 3.0 the splash fades out to REVEAL the already-built home screen.
class IntroSplash extends StatefulWidget {
  /// True once the home screen is mounted under this splash and it is safe to start the fade-out.
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
const _sweep = Cubic(.45, .05, .25, 1); // starts gently, glides to the end

double _seg(double t, double s, double e, [Curve c = _out]) => c.transform(((t - s) / (e - s)).clamp(0.0, 1.0));

Color _rgba(int rgb, double a) => Color.fromRGBO((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255, a.clamp(0.0, 1.0));

const _total = 3.0; // seconds, whole animation
const _holdAt = 2.4; // seconds, where it waits for the home screen
const _mint = 0xFF44B197;
const _white = 0xFFE7EAEF;

class _IntroSplashState extends State<IntroSplash> with SingleTickerProviderStateMixin {
  static const _inMs = 2400, _outMs = 600;
  static const _hold = _holdAt / _total;

  late final AnimationController _c = AnimationController(vsync: this, duration: const Duration(milliseconds: 3000));
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
    // The home screen is built right now, under the splash, and its first frames can be slow. A slow
    // frame in the middle of the fade makes it skip, so wait until frames are smooth again.
    _waitSmooth().then((_) {
      if (!mounted) return;
      _c.animateTo(1.0, duration: const Duration(milliseconds: _outMs), curve: Curves.linear).whenComplete(() {
        if (mounted) widget.onDone();
      });
    });
  }

  /// Completes after 6 quick frames in a row (each under 24 ms), or after 1.5 s at the latest.
  Future<void> _waitSmooth() {
    final done = Completer<void>();
    final started = DateTime.now();
    var good = 0;
    Duration? last;
    void tick(Duration t) {
      if (done.isCompleted) return;
      if (!mounted) {
        done.complete();
        return;
      }
      if (last != null) {
        final ms = (t - last!).inMicroseconds / 1000;
        good = ms < 24 ? good + 1 : 0;
      }
      last = t;
      if (good >= 6 || DateTime.now().difference(started).inMilliseconds > 1500) {
        done.complete();
        return;
      }
      SchedulerBinding.instance.scheduleFrameCallback(tick); // keeps frames coming while the screen is still
    }
    SchedulerBinding.instance.scheduleFrameCallback(tick);
    return done.future;
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
    // Everything is drawn in units of u, so it looks the same on small and big phones.
    final u = math.min(size.width / 300, 1.4);
    final wy = size.height / 2 - 18 * u; // vertical centre of the name
    return AbsorbPointer(
      child: Material(
        type: MaterialType.transparency,
        child: RepaintBoundary(
          child: AnimatedBuilder(
            animation: _c,
            builder: (_, __) {
              final T = _c.value * _total;
              final fade = 1 - _seg(T, 2.4, 3.0, _io); // the whole splash dissolves into the home screen
              final nameIn = _seg(T, 0.1, 0.7, _out);
              final tagIn = _seg(T, 1.55, 2.1, _out);
              final mk = _seg(T, 1.7, 2.2);

              return Opacity(
                opacity: fade.clamp(0.0, 1.0),
                child: Stack(fit: StackFit.expand, clipBehavior: Clip.hardEdge, children: [
                  Positioned.fill(child: CustomPaint(painter: _SplashPainter(T, u, wy))),
                  // the name
                  Positioned(
                    top: wy - 30 * u, left: 0, right: 0, height: 60 * u,
                    child: Opacity(
                      opacity: nameIn,
                      child: Transform.translate(
                        offset: Offset(0, 8 * u * (1 - nameIn)),
                        child: Center(child: _Name(u: u, maxW: size.width * .72)),
                      ),
                    ),
                  ),
                  // the tagline
                  Positioned(
                    top: wy + 40 * u, left: 0, right: 0,
                    child: Opacity(
                      opacity: tagIn,
                      child: Center(child: Text('HTML to APK, simplified', style: inter(13 * u, c: C.muted).copyWith(letterSpacing: .4))),
                    ),
                  ),
                  Positioned(
                    left: 0, right: 0, bottom: mq.padding.bottom + 30,
                    child: Opacity(
                      opacity: (mk * .3).clamp(0.0, 1.0),
                      child: RichText(
                        textAlign: TextAlign.center,
                        text: TextSpan(text: 'Tycept', style: inter(12, w: FontWeight.w500, c: const Color(0xFFE7EAEF)).copyWith(letterSpacing: 12 * .18)),
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

// ---------------------------------------------------------------- the name
/// "WZH 2 APK" on one line: WZH bold, "2" in mint, APK lighter.
class _Name extends StatelessWidget {
  final double u, maxW;
  const _Name({required this.u, required this.maxW});

  @override
  Widget build(BuildContext context) {
    final big = inter(40 * u, w: FontWeight.w800, c: const Color(_white)).copyWith(letterSpacing: 1.2 * u);
    return SizedBox(
      width: maxW,
      child: FittedBox(
        fit: BoxFit.scaleDown,
        child: RichText(
          text: TextSpan(style: big, children: [
            const TextSpan(text: 'WZH '),
            TextSpan(text: '2 ', style: big.copyWith(color: const Color(_mint))),
            TextSpan(text: 'APK', style: big.copyWith(fontWeight: FontWeight.w600, color: _rgba(_white, .82))),
          ]),
        ),
      ),
    );
  }
}

// ---------------------------------------------------------------- everything that is not text
/// The flat dark background, the comet that sweeps in an arc over the name, and the sparkle at its end.
class _SplashPainter extends CustomPainter {
  final double T, u, wy;
  const _SplashPainter(this.T, this.u, this.wy);

  @override
  void paint(Canvas canvas, Size size) {
    final W = size.width, H = size.height, cx = W / 2;

    // background: one flat dark colour, the same as the app
    canvas.drawRect(Offset.zero & size, Paint()..color = C.bg);

    // the arc: starts at the top-left of the name, rises, comes down at the top-right
    final span = math.min(W * .74, 300 * u);
    final xl = cx - span / 2 - 4 * u, xr = cx + span / 2 + 6 * u;
    final yl = wy - 14 * u, yr = wy - 36 * u, peak = wy - 76 * u;
    final path = Path()
      ..moveTo(xl, yl)
      ..cubicTo(xl + span * .22, peak, xr - span * .22, peak - 4 * u, xr, yr);
    final metric = path.computeMetrics().first;
    final len = metric.length;

    final p = _seg(T, 0.45, 1.55, _sweep); // comet head along the path, 0..1
    if (p <= 0) return;
    final head = len * p;
    final settle = _seg(T, 1.45, 2.0, _io); // the trail grows into the whole arc once the comet has arrived
    final trail = (len * .5) + (len * .5) * settle;
    final tail = math.max(0.0, head - trail);

    const n = 56;
    for (var i = 0; i < n; i++) {
      final f0 = i / n, f1 = (i + 1) / n;
      final a = tail + (head - tail) * f0, b = tail + (head - tail) * f1;
      final t0 = metric.getTangentForOffset(a), t1 = metric.getTangentForOffset(b);
      if (t0 == null || t1 == null) continue;
      final k = math.pow(f1, 1.7).toDouble();
      final w = (.8 + 2.8 * f1) * u;
      canvas.drawLine(t0.position, t1.position, Paint()..color = _rgba(_mint, k * .16)..strokeWidth = w * 4.5..strokeCap = StrokeCap.round);
      canvas.drawLine(t0.position, t1.position, Paint()..color = _rgba(0xFF9FE6D4, k)..strokeWidth = w..strokeCap = StrokeCap.round);
    }

    // the bright head while it travels
    final hp = metric.getTangentForOffset(head)?.position;
    final moving = 1 - _seg(T, 1.4, 1.7);
    if (hp != null && moving > 0) {
      canvas.drawCircle(hp, 5 * u, Paint()..color = _rgba(_mint, .28 * moving));
      canvas.drawCircle(hp, 2 * u, Paint()..color = _rgba(_white, moving));
    }

    // sparkle at the end of the arc
    final s = _seg(T, 1.4, 1.75, _out);
    if (s > 0) {
      final e = Offset(xr, yr);
      final glow = RadialGradient(colors: [_rgba(_mint, .45 * s), _rgba(_mint, 0)]).createShader(Rect.fromCircle(center: e, radius: 14 * u));
      canvas.drawCircle(e, 14 * u, Paint()..shader = glow);
      final hl = 26 * u * s, vl = 11 * u * s;
      final line = Paint()..strokeCap = StrokeCap.round..strokeWidth = 1.4 * u..color = _rgba(_white, .9 * s);
      canvas.drawLine(Offset(e.dx - hl, e.dy), Offset(e.dx + hl, e.dy), line);
      canvas.drawLine(Offset(e.dx, e.dy - vl), Offset(e.dx, e.dy + vl), line);
      canvas.drawCircle(e, 2 * u, Paint()..color = _rgba(_white, s));
    }
  }

  @override
  bool shouldRepaint(covariant _SplashPainter old) => old.T != T || old.u != u || old.wy != wy;
}
