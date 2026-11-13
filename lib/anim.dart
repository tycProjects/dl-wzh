import 'package:flutter/material.dart';
import 'package:flutter/cupertino.dart' show CupertinoActivityIndicator;
import 'dart:math' as math;
import 'package:flutter/physics.dart' show SpringDescription, SpringSimulation;
import 'theme.dart';

/// The curve iOS uses for sheets, drawers and page pushes.
const iosCurve = Cubic(.32, .72, 0, 1);

/// Springs defined the way iOS defines them: a response (seconds) and a damping ratio.
/// smooth 1.0 (no overshoot), snappy 0.85 (tiny overshoot), bouncy 0.70 (visible overshoot).
class IosSpring {
  static SpringDescription of(double response, double ratio) {
    final w = 2 * math.pi / response; // natural frequency
    return SpringDescription(mass: 1, stiffness: w * w, damping: 2 * ratio * w);
  }

  static SpringDescription smooth([double response = .45]) => of(response, 1.0);
  static SpringDescription snappy([double response = .35]) => of(response, .85);
  static SpringDescription bouncy([double response = .50]) => of(response, .70);
}

/// A route curve that follows a real spring (0 -> 1), so sheets and drawers settle like iOS.
/// [seconds] is how long the spring takes to settle; use it as the route's duration.
class SpringCurve extends Curve {
  final SpringDescription spring;
  final double seconds;
  final SpringSimulation _sim;
  SpringCurve._(this.spring, this.seconds) : _sim = SpringSimulation(spring, 0, 1, 0);

  factory SpringCurve(SpringDescription spring) {
    final sim = SpringSimulation(spring, 0, 1, 0);
    var t = 0.0;
    var settled = 0.0;
    while (t < 3) {
      t += 1 / 120;
      if ((sim.x(t) - 1).abs() > .004) settled = t;
    }
    return SpringCurve._(spring, math.max(settled + 1 / 120, .2));
  }

  Duration get duration => Duration(milliseconds: (seconds * 1000).ceil());

  @override
  double transformInternal(double t) => _sim.x(t * seconds);
}

/// Shared spring curves (built once; each one also says how long it takes to settle).
final sheetSpring = SpringCurve(IosSpring.smooth(.34));
final drawerSpring = SpringCurve(IosSpring.smooth(.34));
final popSpring = SpringCurve(IosSpring.snappy(.30));
final dialogSpring = SpringCurve(IosSpring.snappy(.34));

/// UIKit rubber band: offset = (1 - 1 / ((c * x / d) + 1)) * d, with c = 0.55 and d the size of
/// the scroll view. [x] is how far the finger has pulled past the edge.
double rubberBand(double x, double d, {double c = .55}) {
  if (x <= 0 || d <= 0) return 0;
  return (1 - 1 / ((c * x / d) + 1)) * d;
}

/// Inverse of [rubberBand]: how far the finger had to pull to get [o] of stretch.
double _rubberBandInverse(double o, double d, {double c = .55}) {
  if (o <= 0) return 0;
  final r = (o / d).clamp(0.0, .999);
  return o / (c * (1 - r));
}

/// Scroll physics with the exact UIKit rubber band while dragging past an edge, and a
/// critically damped spring (no overshoot) bringing it back. Fling velocity is kept.
class IosScrollPhysics extends BouncingScrollPhysics {
  const IosScrollPhysics({super.parent});

  @override
  IosScrollPhysics applyTo(ScrollPhysics? ancestor) => IosScrollPhysics(parent: buildParent(ancestor));

  @override
  SpringDescription get spring => IosSpring.smooth(.45);

  @override
  double applyPhysicsToUserOffset(ScrollMetrics position, double offset) {
    if (offset == 0) return 0;
    final d = position.viewportDimension;
    final dir = offset.sign;
    final amount = offset.abs();
    final pastStart = math.max(position.minScrollExtent - position.pixels, 0.0);
    final pastEnd = math.max(position.pixels - position.maxScrollExtent, 0.0);
    final over = math.max(pastStart, pastEnd);

    if (over > 0) {
      // offset > 0 moves pixels down (towards the start edge); < 0 towards the end edge.
      final easing = (pastStart > 0 && offset < 0) || (pastEnd > 0 && offset > 0);
      final pulled = _rubberBandInverse(over, d);
      if (easing) {
        if (amount >= pulled) return dir * (over + (amount - pulled)); // back inside: normal scroll
        return dir * (over - rubberBand(pulled - amount, d));
      }
      return dir * (rubberBand(pulled + amount, d) - over);
    }

    // Inside the content: the part of this move that crosses an edge is rubber-banded.
    final next = position.pixels - offset;
    if (next < position.minScrollExtent) {
      final inside = position.pixels - position.minScrollExtent;
      return dir * (inside + rubberBand(amount - inside, d));
    }
    if (next > position.maxScrollExtent) {
      final inside = position.maxScrollExtent - position.pixels;
      return dir * (inside + rubberBand(amount - inside, d));
    }
    return offset;
  }
}

/// iOS-style press feedback: dips fast under the finger, then springs back with a tiny
/// overshoot. A very quick tap still shows the full dip, (no vibration).
class Pressable extends StatefulWidget {
  final Widget child;
  final VoidCallback? onTap;
  final double scale;
  final bool haptic;
  const Pressable({super.key, required this.child, this.onTap, this.scale = .97, this.haptic = true});
  @override
  State<Pressable> createState() => _PressableState();
}

class _PressableState extends State<Pressable> with SingleTickerProviderStateMixin {
  late final AnimationController _c = AnimationController.unbounded(vsync: this, value: 1);
  DateTime _downAt = DateTime.fromMillisecondsSinceEpoch(0);
  bool _held = false;

  static final _press = IosSpring.smooth(.18);
  static final _release = IosSpring.snappy(.30);

  void _to(double v, SpringDescription d) =>
      _c.animateWith(SpringSimulation(d, _c.value, v, _c.velocity));

  void _down(TapDownDetails _) {
    _held = true;
    _downAt = DateTime.now();
    _to(widget.scale, _press);
  }

  void _up() {
    if (!_held) return;
    _held = false;
    // Let the dip be seen even on a very quick tap.
    final wait = 70 - DateTime.now().difference(_downAt).inMilliseconds;
    if (wait > 0) {
      Future.delayed(Duration(milliseconds: wait), () { if (mounted && !_held) _to(1, _release); });
    } else {
      _to(1, _release);
    }
  }

  @override
  void dispose() {
    _c.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final enabled = widget.onTap != null;
    return GestureDetector(
      behavior: HitTestBehavior.opaque,
      onTapDown: enabled ? _down : null,
      onTapUp: enabled ? (_) => _up() : null,
      onTapCancel: enabled ? _up : null,
      onTap: enabled
          ? () {
              widget.onTap!();
            }
          : null,
      child: AnimatedBuilder(
        animation: _c,
        // Its own layer: the pressed widget is drawn once and only scaled while the spring runs.
        child: RepaintBoundary(child: widget.child),
        builder: (context, child) {
          final v = _c.value;
          // scale 1 means "no dip": dividing by zero here would turn the opacity into NaN.
          final span = 1 - widget.scale;
          final dip = span <= 0 ? 0.0 : ((1 - v) / span).clamp(0.0, 1.0);
          return Opacity(
            opacity: 1 - .12 * dip,
            child: Transform.scale(scale: v, child: child),
          );
        },
      ),
    );
  }
}

/// iOS toggle: the thumb is a snappy spring, can be dragged, and keeps the finger's velocity
/// when let go.
class IosToggle extends StatefulWidget {
  final bool value;
  final ValueChanged<bool> onChanged;
  const IosToggle({super.key, required this.value, required this.onChanged});
  @override
  State<IosToggle> createState() => _IosToggleState();
}

class _IosToggleState extends State<IosToggle> with SingleTickerProviderStateMixin {
  late final AnimationController _c = AnimationController.unbounded(vsync: this, value: widget.value ? 1 : 0);
  static final _spring = IosSpring.snappy(.30);
  static const _travel = 20.0; // px the thumb moves between off and on
  bool _dragging = false;

  void _to(double target, double velocity) =>
      _c.animateWith(SpringSimulation(_spring, _c.value, target, velocity));

  @override
  void didUpdateWidget(IosToggle old) {
    super.didUpdateWidget(old);
    if (!_dragging && old.value != widget.value) _to(widget.value ? 1 : 0, _c.velocity);
  }

  void _dragUpdate(DragUpdateDetails d) {
    _dragging = true;
    _c.stop();
    _c.value = (_c.value + d.delta.dx / _travel).clamp(-.1, 1.1).toDouble();
  }

  void _dragEnd(DragEndDetails d) {
    _dragging = false;
    final v = d.velocity.pixelsPerSecond.dx / _travel;
    final on = _c.value + v * .12 > .5;
    _to(on ? 1 : 0, v);
    if (on != widget.value) widget.onChanged(on);
  }

  @override
  void dispose() {
    _c.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => Pressable(
        scale: .92,
        haptic: false,
        onTap: () => widget.onChanged(!widget.value),
        child: GestureDetector(
          onHorizontalDragUpdate: _dragUpdate,
          onHorizontalDragEnd: _dragEnd,
          onHorizontalDragCancel: () => _dragging = false,
          child: AnimatedBuilder(
            animation: _c,
            builder: (context, _) {
              final v = _c.value;
              return Container(
                width: 50, height: 30,
                padding: const EdgeInsets.all(3),
                decoration: BoxDecoration(
                  color: Color.lerp(const Color(0xFF3A3A3C), C.tealBtn, v.clamp(0.0, 1.0).toDouble()),
                  borderRadius: BorderRadius.circular(15)),
                child: Align(
                  alignment: Alignment.centerLeft,
                  child: Transform.translate(
                    offset: Offset(_travel * v, 0),
                    child: Container(
                      width: 24, height: 24,
                      decoration: const BoxDecoration(
                        color: Colors.white, shape: BoxShape.circle,
                        boxShadow: [BoxShadow(color: Color(0x44000000), blurRadius: 4, offset: Offset(0, 1))]),
                    ),
                  ),
                ),
              );
            },
          ),
        ),
      );
}

Future<T?> showIosSheet<T>(BuildContext context, WidgetBuilder builder) {
  // Set by the drag handler when a flick closes the sheet: from then on the sheet's own spring
  // (which carries the finger's velocity) moves it, and the route only fades the backdrop.
  final flung = ValueNotifier<bool>(false);
  return Navigator.of(context, rootNavigator: true).push(PageRouteBuilder<T>(
    opaque: false,
    barrierDismissible: true,
    barrierLabel: 'close',
    barrierColor: Colors.black.withOpacity(.62),
    transitionDuration: sheetSpring.duration,
    reverseTransitionDuration: const Duration(milliseconds: 300),
    pageBuilder: (c, a, s) => Material(type: MaterialType.transparency, child: Align(alignment: Alignment.bottomCenter, child: _SheetDrag(flung: flung, child: builder(c)))),
    transitionsBuilder: (c, a, s, child) {
      final curved = CurvedAnimation(parent: a, curve: sheetSpring, reverseCurve: iosCurve.flipped);
      return AnimatedBuilder(
        animation: Listenable.merge([curved, flung]),
        child: child,
        builder: (context, child) => FractionalTranslation(
          translation: Offset(0, flung.value ? 0 : 1 - curved.value),
          child: child,
        ),
      );
    },
  ));
}

Future<T?> showIosDrawer<T>(BuildContext context, WidgetBuilder builder) =>
    Navigator.of(context, rootNavigator: true).push(PageRouteBuilder<T>(
      opaque: false,
      barrierDismissible: true,
      barrierLabel: 'close',
      barrierColor: Colors.black.withOpacity(.55),
      transitionDuration: drawerSpring.duration,
      reverseTransitionDuration: const Duration(milliseconds: 290),
      pageBuilder: (c, a, s) => Material(type: MaterialType.transparency, child: Align(alignment: Alignment.centerLeft, child: builder(c))),
      transitionsBuilder: (c, a, s, child) => SlideTransition(
        position: Tween(begin: const Offset(-1, 0), end: Offset.zero)
            .animate(CurvedAnimation(parent: a, curve: drawerSpring, reverseCurve: iosCurve.flipped)),
        child: child,
      ),
    ));

Future<T?> showIosDialog<T>(BuildContext context, WidgetBuilder builder, {bool dismissible = false}) =>
    Navigator.of(context, rootNavigator: true).push(PageRouteBuilder<T>(
      opaque: false,
      barrierDismissible: dismissible,
      barrierLabel: dismissible ? 'Dismiss' : null,
      barrierColor: Colors.black.withOpacity(.5),
      transitionDuration: dialogSpring.duration,
      reverseTransitionDuration: const Duration(milliseconds: 180),
      pageBuilder: (c, a, s) => Material(type: MaterialType.transparency, child: Center(child: builder(c))),
      transitionsBuilder: (c, a, s, child) => FadeTransition(
        opacity: CurvedAnimation(parent: a, curve: Curves.easeOut),
        child: ScaleTransition(
          scale: Tween(begin: .9, end: 1.0)
              .animate(CurvedAnimation(parent: a, curve: dialogSpring, reverseCurve: Curves.easeIn)),
          child: child,
        ),
      ),
    ));

/// The iOS activity indicator (the spinning ticks) instead of Material's circle.
class IosSpinner extends StatelessWidget {
  final double size;
  final Color? color;
  const IosSpinner({super.key, this.size = 24, this.color});
  @override
  Widget build(BuildContext context) => SizedBox(
        width: size,
        height: size,
        child: FittedBox(child: CupertinoActivityIndicator(radius: 10, color: color ?? C.teal)),
      );
}

/// Lets a bottom sheet follow the finger down and be flicked away, like an iOS sheet.
/// Past the halfway-ish point (or a quick flick) it closes; otherwise it springs back.
class _SheetDrag extends StatefulWidget {
  final Widget child;
  final ValueNotifier<bool> flung;
  const _SheetDrag({required this.child, required this.flung});
  @override
  State<_SheetDrag> createState() => _SheetDragState();
}

class _SheetDragState extends State<_SheetDrag> with SingleTickerProviderStateMixin {
  late final AnimationController _c = AnimationController.unbounded(vsync: this, value: 0);
  bool _closing = false;
  static final _snap = IosSpring.smooth(.35);
  static final _fling = IosSpring.smooth(.30);
  static const _maxUp = 18.0; // the stretch it approaches when pulled up (px)
  final _box = GlobalKey();

  void _update(DragUpdateDetails d) {
    if (_closing) return;
    _c.stop();
    // Where the finger is relative to the resting spot, in raw pixels (up is negative). Past the
    // resting spot the sheet follows the UIKit rubber band, so it stretches a little and stops.
    final raw = _c.value < 0 ? -_rubberBandInverse(-_c.value, _maxUp) : _c.value;
    final next = raw + d.delta.dy;
    _c.value = next >= 0 ? next : -rubberBand(-next, _maxUp);
  }

  void _end(DragEndDetails d) {
    if (_closing) return;
    final v = d.velocity.pixelsPerSecond.dy;
    if (_c.value > 110 || v > 800) {
      _closing = true;
      // The sheet keeps the speed of the finger and flies off on its own spring; the route only
      // fades the backdrop.
      widget.flung.value = true;
      final off = MediaQuery.of(context).size.height;
      _c.animateWith(SpringSimulation(_fling, _c.value, off, v));
      Navigator.of(context).maybePop();
    } else {
      _c.animateWith(SpringSimulation(_snap, _c.value, 0, v));
    }
  }

  @override
  void dispose() {
    _c.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => GestureDetector(
        onVerticalDragUpdate: _update,
        onVerticalDragEnd: _end,
        child: AnimatedBuilder(
          animation: _c,
          child: KeyedSubtree(key: _box, child: widget.child),
          builder: (context, child) {
            final v = _c.value;
            if (v < 0) {
              // Pulled up: the sheet only stretches a little taller, its top edge rises and the
              // bottom (Continue / Done) stays exactly where it is, so no gap opens underneath.
              final measured = _box.currentContext?.size?.height ?? 0;
              final h = measured > 0 ? measured : 600.0;
              return Transform(
                alignment: Alignment.bottomCenter,
                transform: Matrix4.diagonal3Values(1, 1 - v / h, 1),
                child: child,
              );
            }
            return Transform.translate(offset: Offset(0, v), child: child);
          },
        ),
      );
}
