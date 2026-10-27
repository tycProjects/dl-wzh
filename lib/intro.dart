import 'dart:async';
import 'dart:math' as math;
import 'package:flutter/material.dart' hide Text;
import 'package:flutter/scheduler.dart' show SchedulerBinding;
import 'theme.dart';
import 'anim.dart';
import 'tr.dart';

/// Startup animation, 3 seconds, flat (no gradients). Mint floods the screen, the logo pops in, the
/// screen folds down with a little spring into a rounded app icon and "2 APK" rises in under it.
/// The exit is a shutter: the icon stretches into a thin mint line across the screen and the dark
/// screen splits along it, sliding apart to reveal the home screen.
///
/// Timeline (seconds): 0 .. 2.4 animation, then it holds on the final frame while the home
/// screen is built underneath, then 2.4 .. 3.0 the shutter opens to REVEAL the already-built home screen.
class IntroSplash extends StatefulWidget {
  /// True once the home screen is mounted under this splash and it is safe to open the shutter.
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
const _back = Cubic(.175, .885, .32, 1.275);
const _spr = Cubic(.3, 1.45, .5, 1); // overshoots a little, like a spring
const _lin = Curves.linear;

double _seg(double t, double s, double e, [Curve c = _out]) => c.transform(((t - s) / (e - s)).clamp(0.0, 1.0));

Color _rgba(int rgb, double a) => Color.fromRGBO((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255, a.clamp(0.0, 1.0));

const _blue = Color(0xFF4FD5A6);
const _total = 3.0; // seconds, whole animation
const _holdAt = 2.4; // seconds, where it waits for the home screen

class _IntroSplashState extends State<IntroSplash> with SingleTickerProviderStateMixin {
  static const _inMs = 2400, _outMs = 550;
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
    // frame in the middle of the exit makes the shutter skip, so wait until frames are smooth again.
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
    final cyc = size.height / 2;
    return AbsorbPointer(
      child: Material(
        type: MaterialType.transparency,
        child: RepaintBoundary(
          child: AnimatedBuilder(
            animation: _c,
            builder: (_, __) {
              final T = _c.value * _total;
              final ta = (1 - _seg(T, 2.4, 2.55)).clamp(0.0, 1.0); // text goes before the shutter opens
              final mk = _seg(T, 2.0, 2.3);

              return Stack(fit: StackFit.expand, clipBehavior: Clip.hardEdge, children: [
                Positioned.fill(child: CustomPaint(painter: _SplashPainter(T, u))),
                if (T > 1.65 && ta > 0)
                  Positioned(top: cyc + 60 * u, left: 0, right: 0, child: Opacity(opacity: ta, child: Center(child: _Wordmark(T, u)))),
                Positioned(
                  left: 0, right: 0, bottom: mq.padding.bottom + 30,
                  child: Opacity(
                    opacity: (mk * ta * .3).clamp(0.0, 1.0),
                    child: RichText(
                      textAlign: TextAlign.center,
                      text: TextSpan(text: 'Tycept', style: inter(12, w: FontWeight.w500, c: const Color(0xFFF5F5F5)).copyWith(letterSpacing: 12 * .18)),
                    ),
                  ),
                ),
                // Only if loading takes longer than the animation (slow network).
                if (_holding && !widget.reveal)
                  Positioned(left: 0, right: 0, bottom: mq.padding.bottom + 80, child: const Center(child: IosSpinner(size: 22))),
              ]);
            },
          ),
        ),
      ),
    );
  }
}

// ---------------------------------------------------------------- the wordmark
/// "2 APK" on one line under the icon: it rises out of a hard mask, then the tagline fades in.
class _Wordmark extends StatelessWidget {
  final double t; // seconds
  final double u;
  const _Wordmark(this.t, this.u);

  @override
  Widget build(BuildContext context) {
    final ls = 6 * u;
    final style = inter(24 * u, w: FontWeight.w700, c: const Color(0xFFF5F5F5)).copyWith(letterSpacing: ls);
    final rise = _seg(t, 1.7, 2.05, _back);
    return Column(mainAxisSize: MainAxisSize.min, children: [
      ClipRect(
        child: Transform.translate(
          offset: Offset(0, 34 * u * (1 - rise)),
          // The padding balances the letter spacing that is added after the last letter.
          child: Padding(
            padding: EdgeInsets.only(left: ls),
            child: RichText(text: TextSpan(style: style, children: [TextSpan(text: '2 ', style: style.copyWith(color: _blue)), const TextSpan(text: 'APK')])),
          ),
        ),
      ),
      Padding(
        padding: EdgeInsets.only(top: 16 * u),
        child: Opacity(
          opacity: _seg(t, 2.0, 2.3),
          child: Text('HTML to APK, simplified', style: inter(13 * u, c: C.muted).copyWith(letterSpacing: .4)),
        ),
      ),
    ]);
  }
}

// ---------------------------------------------------------------- the logo
typedef _P = List<double>;

class _Geo {
  static const L = <String, List<_P>>{
    'W': [[16, 35], [29, 35], [38, 77], [49, 41], [59, 41], [70, 77], [79, 35], [92, 35], [78, 95], [65, 95], [54, 61], [43, 95], [30, 95]],
    'Z': [[104, 35], [158, 35], [158, 47], [124, 83], [158, 83], [158, 95], [104, 95], [104, 83], [138, 47], [104, 47]],
    'H': [[170, 35], [184, 35], [184, 57], [210, 57], [210, 35], [224, 35], [224, 95], [210, 95], [210, 73], [184, 73], [184, 95], [170, 95]],
  };
  static const Q = <_P>[[138, 47], [158, 47], [124, 83], [104, 83]];
  static const keys = ['W', 'Z', 'H'];

  static Path poly(List<_P> a) {
    final p = Path()..moveTo(a[0][0], a[0][1]);
    for (var i = 1; i < a.length; i++) {
      p.lineTo(a[i][0], a[i][1]);
    }
    return p..close();
  }

  static final letterPaths = [for (final k in keys) poly(L[k]!)];
  static final centers = [for (final p in letterPaths) p.getBounds().center];
  static final qPath = poly(Q);
}

/// Everything that is not text: the dark screen, the mint flood / icon / line and the logo.
class _SplashPainter extends CustomPainter {
  final double T, u;
  const _SplashPainter(this.T, this.u);

  static const _ink = 0x0F2A24, _white = 0xF5F5F5, _mint = 0x4FD5A6;

  void _rrect(Canvas canvas, double cx, double cy, double w, double h, double r, Paint p) {
    final rad = r.clamp(0.0, math.min(w, h) / 2).toDouble();
    canvas.drawRRect(RRect.fromRectAndRadius(Rect.fromCenter(center: Offset(cx, cy), width: w, height: h), Radius.circular(rad)), p);
  }

  /// W, Z and H pop in one after another (dark on the mint), the block slides in from the left (white).
  void _letters(Canvas canvas, double cx, double cy, double k, double alpha) {
    if (alpha <= 0) return;
    canvas.save();
    canvas.translate(cx, cy);
    canvas.scale(k);
    canvas.translate(-120, -65);
    for (var i = 0; i < 3; i++) {
      final p = _seg(T, .5 + i * .1, .85 + i * .1, _back);
      final c = _Geo.centers[i];
      canvas.save();
      canvas.translate(c.dx, c.dy);
      canvas.scale(.5 + .5 * p);
      canvas.translate(-c.dx, -c.dy);
      canvas.drawPath(_Geo.letterPaths[i], Paint()..color = _rgba(_ink, alpha * math.min(1.0, p * 1.5)));
      canvas.restore();
    }
    final q = _seg(T, .8, 1.25, _out);
    if (q > 0) {
      canvas.save();
      canvas.translate(-50 * (1 - q), 0);
      canvas.drawPath(_Geo.qPath, Paint()..color = _rgba(_white, alpha * math.min(1.0, q * 2)));
      canvas.restore();
    }
    canvas.restore();
  }

  @override
  void paint(Canvas canvas, Size size) {
    final W = size.width, H = size.height, cx = W / 2, cyc = H / 2;
    final dark = Paint()..color = C.bg;
    final mint = Paint()..color = const Color(0xFF4FD5A6);
    final iconCy = cyc - 55 * u; // where the icon rests

    if (T < 2.4) {
      canvas.drawRect(Offset.zero & size, dark);
      if (T < 1.05) {
        // mint floods the screen from the middle, the logo pops in
        final R = math.sqrt(W * W + H * H) / 2 * 1.05 * _seg(T, 0, .5, _io);
        canvas.drawCircle(Offset(cx, cyc), R, mint);
        _letters(canvas, cx, cyc, u, 1);
      } else {
        // the screen folds down into the icon, with a little spring
        final e = _spr.transform(_seg(T, 1.05, 1.7, _lin));
        final w = W + (170 * u - W) * e, h = H + (170 * u - H) * e, cy = cyc - 55 * u * e;
        _rrect(canvas, cx, cy, w, h, 48 * u * e.clamp(0.0, 1.0), mint);
        _letters(canvas, cx, cy, (1 - .51 * e) * u, 1);
      }
      return;
    }

    // shutter: the icon stretches into a line, then the dark screen splits along it and slides apart
    final ec = math.min(1.0, _spr.transform(_seg(T, 2.4, 2.68, _lin)));
    final d = _seg(T, 2.68, 3.0, _io);
    canvas.drawRect(Rect.fromLTWH(0, -cyc * d, W, cyc), dark);
    canvas.drawRect(Rect.fromLTWH(0, cyc + (H - cyc) * d, W, H - cyc), dark);
    final cy = iconCy + 55 * u * ec;
    final w = 170 * u + (W - 170 * u) * ec;
    final h = math.max(3 * u, 170 * u * (1 - ec));
    final la = 1 - _seg(T, 2.8, 3.0, _out);
    if (la > 0) _rrect(canvas, cx, cy, w, h, 48 * u, Paint()..color = _rgba(_mint, la)); // radius is clamped, so it ends as a thin line
    if (T < 2.55) _letters(canvas, cx, cy, .49 * u, 1 - _seg(T, 2.4, 2.5));
  }

  @override
  bool shouldRepaint(covariant _SplashPainter old) => old.T != T || old.u != u;
}
