import 'package:flutter/material.dart';
import 'package:flutter/cupertino.dart' show CupertinoActivityIndicator;
import 'dart:math' as math;
import 'dart:ui' as ui;
import 'package:flutter/foundation.dart' show ValueListenable;
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

  /// iOS 26 / 27 "liquid" feel: while the finger is held and moves, a small control stretches toward it
  /// and springs back with a little wobble when let go. Large cards (over ~96 px) never stretch.
  final bool liquid;
  const Pressable({super.key, required this.child, this.onTap, this.scale = .97, this.haptic = true, this.liquid = true});
  @override
  State<Pressable> createState() => _PressableState();
}

class _PressableState extends State<Pressable> with TickerProviderStateMixin {
  late final AnimationController _c = AnimationController.unbounded(vsync: this, value: 1);
  late final AnimationController _cx = AnimationController.unbounded(vsync: this, value: 0);
  late final AnimationController _cy = AnimationController.unbounded(vsync: this, value: 0);
  DateTime _downAt = DateTime.fromMillisecondsSinceEpoch(0);
  bool _held = false;
  Offset _origin = Offset.zero;
  Size _size = Size.zero;
  bool _stretchy = false;

  static final _press = IosSpring.smooth(.18);
  static final _release = IosSpring.snappy(.30);
  static final _wobble = IosSpring.bouncy(.42);

  void _to(double v, SpringDescription d) =>
      _c.animateWith(SpringSimulation(d, _c.value, v, _c.velocity));

  void _down(TapDownDetails d) {
    _held = true;
    _downAt = DateTime.now();
    _origin = d.globalPosition;
    final box = context.findRenderObject();
    _size = box is RenderBox && box.hasSize ? box.size : Size.zero;
    _stretchy = widget.liquid && _size != Size.zero && _size.shortestSide <= 96;
    _cx.stop();
    _cy.stop();
    _to(widget.scale, _press);
  }

  void _move(PointerMoveEvent e) {
    if (!_held || !_stretchy) return;
    final dx = e.position.dx - _origin.dx, dy = e.position.dy - _origin.dy;
    // The same rubber band as scrolling, so the stretch gets heavier the further the finger goes.
    _cx.value = dx.sign * rubberBand(dx.abs(), 16);
    _cy.value = dy.sign * rubberBand(dy.abs(), 16);
  }

  void _up() {
    if (!_held) return;
    _held = false;
    if (_cx.value != 0 || _cy.value != 0) {
      _cx.animateWith(SpringSimulation(_wobble, _cx.value, 0, _cx.velocity));
      _cy.animateWith(SpringSimulation(_wobble, _cy.value, 0, _cy.velocity));
    }
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
    _cx.dispose();
    _cy.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final enabled = widget.onTap != null;
    return Listener(
      behavior: HitTestBehavior.translucent,
      onPointerMove: enabled ? _move : null,
      child: GestureDetector(
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
          animation: Listenable.merge([_c, _cx, _cy]),
          // Its own layer: the pressed widget is drawn once and only scaled while the spring runs.
          child: RepaintBoundary(child: widget.child),
          builder: (context, child) {
            final v = _c.value;
            // scale 1 means "no dip": dividing by zero here would turn the opacity into NaN.
            final span = 1 - widget.scale;
            final dip = span <= 0 ? 0.0 : ((1 - v) / span).clamp(0.0, 1.0);
            final ox = _cx.value, oy = _cy.value;
            Widget w = Transform.scale(scale: v, child: child);
            if ((ox != 0 || oy != 0) && _size != Size.zero) {
              // Volume-preserving: longer along the pull, a little thinner across it. It stays pinned on the
              // side away from the finger, so the near edge is the one that moves.
              final ax = ox.abs() / _size.width * .9, ay = oy.abs() / _size.height * .9;
              final sx = 1 + ax - ay * .5, sy = 1 + ay - ax * .5;
              w = Transform(
                alignment: Alignment(-ox.sign, -oy.sign),
                transform: Matrix4.diagonal3Values(sx, sy, 1),
                child: w,
              );
            }
            return Opacity(opacity: 1 - .12 * dip, child: w);
          },
        ),
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
  final origin = LastTap.fresh();
  return Navigator.of(context, rootNavigator: true).push(PageRouteBuilder<T>(
    opaque: false,
    barrierDismissible: true,
    barrierLabel: 'close',
    barrierColor: Colors.black.withOpacity(.62),
    transitionDuration: sheetSpring.duration,
    reverseTransitionDuration: const Duration(milliseconds: 300),
    pageBuilder: (c, a, s) => Material(
        type: MaterialType.transparency,
        child: Align(
            alignment: Alignment.bottomCenter,
            child: _SheetDrag(flung: flung, child: _DropReveal(animation: a, origin: origin, bypass: flung, child: builder(c))))),
  ));
}

Future<T?> showIosDrawer<T>(BuildContext context, WidgetBuilder builder) =>
    Navigator.of(context, rootNavigator: true).push(PageRouteBuilder<T>(
      opaque: false,
      barrierDismissible: true,
      barrierLabel: 'close',
      barrierColor: Colors.black.withOpacity(.55),
      transitionDuration: glassSpring.duration,
      reverseTransitionDuration: const Duration(milliseconds: 290),
      pageBuilder: (c, a, s) => Material(type: MaterialType.transparency, child: Align(alignment: Alignment.centerLeft, child: builder(c))),
      transitionsBuilder: glassTransition(fallback: Alignment.centerLeft),
    ));

Future<T?> showIosDialog<T>(BuildContext context, WidgetBuilder builder, {bool dismissible = false}) =>
    Navigator.of(context, rootNavigator: true).push(PageRouteBuilder<T>(
      opaque: false,
      barrierDismissible: dismissible,
      barrierLabel: dismissible ? 'Dismiss' : null,
      barrierColor: Colors.black.withOpacity(.5),
      transitionDuration: glassSpring.duration,
      reverseTransitionDuration: const Duration(milliseconds: 180),
      pageBuilder: (c, a, s) => Material(type: MaterialType.transparency, child: Center(child: builder(c))),
      transitionsBuilder: glassTransition(fallback: Alignment.center),
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


// ------------------------------------------------------------------ liquid glass pop-ups
// iOS 26 style: a pop-up grows out of the spot you tapped like a drop of glass, wobbling a little as
// it stretches, blurred at first and sharp when it settles. Closing pulls it back into that spot.

/// Where the last finger touched the screen (recorded in main.dart), so a pop-up can grow from there.
class LastTap {
  static Offset? _pos;
  static DateTime _at = DateTime.fromMillisecondsSinceEpoch(0);
  static void set(Offset p) {
    _pos = p;
    _at = DateTime.now();
  }

  /// The tap position if it was just now, else null.
  static Offset? fresh() => _pos != null && DateTime.now().difference(_at).inMilliseconds < 2500 ? _pos : null;
}

/// A little overshoot, then it settles (a bouncy spring, like the video).
final glassSpring = SpringCurve(IosSpring.of(.42, .80));

/// A route transition that morphs the pop-up out of the last tap. [bypass] true = show it plainly
/// (used while a sheet is being flicked away). [fallback] is the grow point when there was no tap.
RouteTransitionsBuilder glassTransition({ValueListenable<bool>? bypass, Alignment fallback = Alignment.center, Offset? origin0}) {
  final origin = origin0 ?? LastTap.fresh();
  return (c, a, s, child) => _GlassMorph(animation: a, origin: origin, fallback: fallback, bypass: bypass, child: child);
}

class _GlassMorph extends StatefulWidget {
  final Animation<double> animation;
  final Offset? origin;
  final Alignment fallback;
  final ValueListenable<bool>? bypass;
  final Widget child;
  const _GlassMorph({required this.animation, required this.origin, required this.fallback, required this.bypass, required this.child});
  @override
  State<_GlassMorph> createState() => _GlassMorphState();
}

class _GlassMorphState extends State<_GlassMorph> {
  // Keeps the pop-up's own state when the wrappers below are taken off at the end.
  final GlobalKey _key = GlobalKey();
  late final Animation<double> _t = CurvedAnimation(parent: widget.animation, curve: glassSpring, reverseCurve: iosCurve.flipped);
  late final Listenable _tick = Listenable.merge([_t, if (widget.bypass != null) widget.bypass!]);

  @override
  Widget build(BuildContext context) {
    // The pop-up is painted once into its own layer; each frame only moves/scales that finished picture,
    // so heavy content (lists, cards) cannot make the animation stutter.
    final kept = KeyedSubtree(key: _key, child: RepaintBoundary(child: widget.child));
    final size = MediaQuery.sizeOf(context);
    return AnimatedBuilder(
      animation: _tick,
      child: kept,
      builder: (ctx, ch) {
        if (widget.bypass?.value == true || widget.animation.status == AnimationStatus.completed) return ch!;
        final t = _t.value;
        final open = t.clamp(0.0, 1.0).toDouble();
        final o = widget.origin;
        final al = o == null
            ? widget.fallback
            : Alignment((o.dx / size.width * 2 - 1).clamp(-1.0, 1.0).toDouble(), (o.dy / size.height * 2 - 1).clamp(-1.0, 1.0).toDouble());
        // Height leads and width follows a beat later, so it stretches like a drop instead of scaling evenly.
        final sy = .06 + .94 * t;
        final sx = .10 + .90 * math.max(0.0, (t - .07) / .93);
        // Blur only while it is small and soft (the expensive part), gone by the halfway point.
        final blur = ((1 - open / .55) * 8).clamp(0.0, 8.0).toDouble();
        final op = Curves.easeOut.transform((open * 2.4).clamp(0.0, 1.0).toDouble());
        Widget w = ch!;
        if (blur > .6) w = ImageFiltered(imageFilter: ui.ImageFilter.blur(sigmaX: blur, sigmaY: blur), child: w);
        w = Transform(alignment: al, transform: Matrix4.diagonal3Values(sx, sy, 1), filterQuality: FilterQuality.low, child: w);
        return op >= .999 ? w : Opacity(opacity: op, child: w);
      },
    );
  }
}

/// Drop-in for showDialog: same look, but it grows out of the tap.
Future<T?> showGlassDialog<T>({required BuildContext context, required WidgetBuilder builder, bool barrierDismissible = true}) =>
    showGeneralDialog<T>(
      context: context,
      barrierDismissible: barrierDismissible,
      barrierLabel: 'Close',
      barrierColor: Colors.black54,
      transitionDuration: glassSpring.duration,
      pageBuilder: (c, _, __) => Builder(builder: builder),
      transitionBuilder: glassTransition(fallback: Alignment.center),
    );

/// The corner radius of every bottom sheet (iOS 26 / 27 sheets are rounder than before).
const kSheetRadius = 32.0;

/// Plain rounded sheet shapes get the shared, rounder top corners; anything else is kept as it was.
ShapeBorder _sheetShape(ShapeBorder? s) {
  if (s == null || s is RoundedRectangleBorder) {
    return const RoundedRectangleBorder(borderRadius: BorderRadius.vertical(top: Radius.circular(kSheetRadius)));
  }
  return s;
}

/// Drop-in for showModalBottomSheet (the options this app uses): grows out of the tap, can be dragged
/// down or flicked away.
Future<T?> showGlassSheet<T>({
  required BuildContext context,
  required WidgetBuilder builder,
  bool isScrollControlled = false,
  ShapeBorder? shape,
  bool showDragHandle = false,
  Color? backgroundColor,
}) {
  final flung = ValueNotifier<bool>(false);
  final origin = LastTap.fresh();
  return Navigator.of(context, rootNavigator: true).push(PageRouteBuilder<T>(
    opaque: false,
    barrierDismissible: true,
    barrierLabel: 'Close',
    barrierColor: Colors.black54,
    transitionDuration: sheetSpring.duration,
    reverseTransitionDuration: const Duration(milliseconds: 300),
    pageBuilder: (c, a, s) {
      final theme = Theme.of(c);
      final mq = MediaQuery.of(c);
      final maxH = isScrollControlled ? mq.size.height - mq.padding.top : mq.size.height * 9 / 16;
      return Material(
        type: MaterialType.transparency,
        child: Align(
          alignment: Alignment.bottomCenter,
          child: _SheetDrag(
            flung: flung,
            child: _DropReveal(
              animation: a,
              origin: origin,
              bypass: flung,
              child: ConstrainedBox(
              constraints: BoxConstraints(maxHeight: maxH, maxWidth: 640),
              child: Material(
                color: backgroundColor ?? theme.bottomSheetTheme.backgroundColor ?? theme.colorScheme.surface,
                shape: _sheetShape(shape ?? theme.bottomSheetTheme.shape),
                clipBehavior: Clip.antiAlias,
                child: Column(mainAxisSize: MainAxisSize.min, children: [
                  if (showDragHandle)
                    Container(
                      width: 36,
                      height: 4,
                      margin: const EdgeInsets.only(top: 10, bottom: 6),
                      decoration: BoxDecoration(color: const Color(0x66FFFFFF), borderRadius: BorderRadius.circular(2)),
                    ),
                  Flexible(child: Builder(builder: builder)),
                ]),
              ),
            ),
            ),
          ),
        ),
      );
    },
  ));
}


/// Drop-in for PopupMenuButton (the options this app uses): the menu grows out of the button as a glass
/// card, and shrinks back into it. Items are normal PopupMenuItem / PopupMenuDivider entries.
class GlassMenuButton<T> extends StatelessWidget {
  final List<PopupMenuEntry<T>> Function(BuildContext) itemBuilder;
  final void Function(T)? onSelected;
  final Widget? icon;
  final Widget? child;
  final String? tooltip;
  final Color? color;
  final EdgeInsetsGeometry padding;
  const GlassMenuButton({
    super.key,
    required this.itemBuilder,
    this.onSelected,
    this.icon,
    this.child,
    this.tooltip,
    this.color,
    this.padding = const EdgeInsets.all(8),
  });

  Future<void> _open(BuildContext context) async {
    final box = context.findRenderObject() as RenderBox;
    final r = box.localToGlobal(Offset.zero) & box.size;
    final items = itemBuilder(context);
    final v = await Navigator.of(context, rootNavigator: true).push<T>(PageRouteBuilder<T>(
      opaque: false,
      barrierDismissible: true,
      barrierLabel: 'Close',
      barrierColor: const Color(0x33000000),
      transitionDuration: glassSpring.duration,
      reverseTransitionDuration: const Duration(milliseconds: 260),
      pageBuilder: (c, a, s) => _GlassMenuPage<T>(anchor: r, items: items, color: color),
      transitionsBuilder: glassTransition(origin0: r.center),
    ));
    if (v != null) onSelected?.call(v);
  }

  @override
  Widget build(BuildContext context) => Builder(
        builder: (ctx) => child != null
            ? GestureDetector(behavior: HitTestBehavior.opaque, onTap: () => _open(ctx), child: child)
            : IconButton(onPressed: () => _open(ctx), icon: icon ?? const Icon(Icons.more_vert_rounded), tooltip: tooltip, padding: padding),
      );
}

class _GlassMenuPage<T> extends StatelessWidget {
  final Rect anchor;
  final List<PopupMenuEntry<T>> items;
  final Color? color;
  const _GlassMenuPage({required this.anchor, required this.items, this.color});

  @override
  Widget build(BuildContext context) {
    final mq = MediaQuery.of(context);
    final sz = mq.size;
    final up = anchor.center.dy > sz.height / 2; // buttons low on the screen open upward
    final leftSide = anchor.center.dx < sz.width / 2;
    final maxH = math.max(120.0, up ? anchor.top - 6 - mq.padding.top - 8 : sz.height - anchor.bottom - 6 - mq.padding.bottom - 8);
    final rows = <Widget>[];
    for (final e in items) {
      if (e is PopupMenuItem<T>) {
        rows.add(Opacity(
          opacity: e.enabled ? 1 : .4,
          child: InkWell(
            onTap: e.enabled ? () => Navigator.of(context).pop(e.value) : null,
            child: Container(
              constraints: BoxConstraints(minHeight: math.max(40.0, e.height)),
              padding: const EdgeInsets.symmetric(horizontal: 16),
              alignment: Alignment.centerLeft,
              child: e.child,
            ),
          ),
        ));
      } else if (e is PopupMenuDivider) {
        rows.add(const Divider(height: 8, thickness: 1, color: Color(0x1FFFFFFF)));
      }
    }
    return Material(
      type: MaterialType.transparency,
      child: Stack(children: [
        Positioned(
          left: leftSide ? math.max(8.0, anchor.left) : null,
          right: leftSide ? null : math.max(8.0, sz.width - anchor.right),
          top: up ? null : anchor.bottom + 6,
          bottom: up ? sz.height - anchor.top + 6 : null,
          child: ConstrainedBox(
            constraints: BoxConstraints(minWidth: 210, maxWidth: 300, maxHeight: maxH),
            child: DecoratedBox(
              decoration: BoxDecoration(
                color: (color ?? const Color(0xFF1C1F21)).withOpacity(.94),
                borderRadius: BorderRadius.circular(22),
                border: Border.all(color: const Color(0x29FFFFFF)),
                boxShadow: const [BoxShadow(color: Color(0x73000000), blurRadius: 40, offset: Offset(0, 16))],
              ),
              child: ClipRRect(
                borderRadius: BorderRadius.circular(22),
                child: SingleChildScrollView(
                  padding: const EdgeInsets.symmetric(vertical: 6),
                  child: IntrinsicWidth(child: Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: rows)),
                ),
              ),
            ),
          ),
        ),
      ]),
    );
  }
}


// ------------------------------------------------------------------ slide-up sheets
// A bottom sheet slides up from the bottom edge and slides back down when it closes (like iOS).
// Nothing is faded, blurred, clipped or scaled, so no half-transparent copy of the sheet can be
// left hanging over the page while it closes. The dim backdrop is faded by the route itself.

class _DropReveal extends StatefulWidget {
  final Animation<double> animation;
  final Offset? origin; // kept for the callers; a slide-up sheet does not need it
  final ValueListenable<bool>? bypass;
  final Widget child;
  const _DropReveal({required this.animation, required this.origin, required this.bypass, required this.child});
  @override
  State<_DropReveal> createState() => _DropRevealState();
}

class _DropRevealState extends State<_DropReveal> {
  final GlobalKey _key = GlobalKey();
  // Opens on a critically damped spring (no overshoot, so it never lifts past its resting place),
  // closes on the iOS curve. Both end exactly at 0 / 1.
  late final Animation<double> _t = CurvedAnimation(parent: widget.animation, curve: sheetSpring, reverseCurve: iosCurve.flipped);
  late final Listenable _tick = Listenable.merge([_t, if (widget.bypass != null) widget.bypass!]);

  @override
  Widget build(BuildContext context) {
    final kept = KeyedSubtree(key: _key, child: RepaintBoundary(child: widget.child));
    return AnimatedBuilder(
      animation: _tick,
      child: kept,
      builder: (ctx, ch) {
        // Being flicked away: the drag handler moves the sheet itself, so don't add a second motion.
        if (widget.bypass?.value == true || widget.animation.status == AnimationStatus.completed) return ch!;
        final t = _t.value.clamp(0.0, 1.0).toDouble();
        // 1 - t of the sheet's own height: fully below the screen at t = 0, in place at t = 1.
        return FractionalTranslation(translation: Offset(0, 1 - t), child: ch);
      },
    );
  }
}
