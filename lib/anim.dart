import 'package:flutter/material.dart';
import 'package:flutter/cupertino.dart' show CupertinoActivityIndicator;
import 'package:flutter/physics.dart' show SpringDescription, SpringSimulation;
import 'theme.dart';

/// The curve iOS uses for sheets, drawers and page pushes.
const iosCurve = Cubic(.32, .72, 0, 1);

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

  static const _press = SpringDescription(mass: 1, stiffness: 900, damping: 46);
  static const _release = SpringDescription(mass: 1, stiffness: 420, damping: 15);

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

/// Switch with the slide-and-colour animation of an iOS toggle.
class IosToggle extends StatelessWidget {
  final bool value;
  final ValueChanged<bool> onChanged;
  const IosToggle({super.key, required this.value, required this.onChanged});
  @override
  Widget build(BuildContext context) => Pressable(
        scale: .92,
        haptic: false,
        onTap: () => onChanged(!value),
        child: AnimatedContainer(
          duration: const Duration(milliseconds: 260),
          curve: iosCurve,
          width: 50, height: 30,
          padding: const EdgeInsets.all(3),
          decoration: BoxDecoration(
            color: value ? C.tealBtn : const Color(0xFF3A3A3C),
            borderRadius: BorderRadius.circular(15)),
          child: AnimatedAlign(
            duration: const Duration(milliseconds: 260),
            curve: iosCurve,
            alignment: value ? Alignment.centerRight : Alignment.centerLeft,
            child: Container(
              width: 24, height: 24,
              decoration: const BoxDecoration(
                color: Colors.white, shape: BoxShape.circle,
                boxShadow: [BoxShadow(color: Color(0x44000000), blurRadius: 4, offset: Offset(0, 1))]),
            ),
          ),
        ),
      );
}

Future<T?> showIosSheet<T>(BuildContext context, WidgetBuilder builder) =>
    Navigator.of(context, rootNavigator: true).push(PageRouteBuilder<T>(
      opaque: false,
      barrierDismissible: true,
      barrierLabel: 'close',
      barrierColor: Colors.black.withOpacity(.62),
      transitionDuration: const Duration(milliseconds: 380),
      reverseTransitionDuration: const Duration(milliseconds: 300),
      pageBuilder: (c, a, s) => Material(type: MaterialType.transparency, child: Align(alignment: Alignment.bottomCenter, child: _SheetDrag(child: builder(c)))),
      transitionsBuilder: (c, a, s, child) => SlideTransition(
        position: Tween(begin: const Offset(0, 1), end: Offset.zero)
            .animate(CurvedAnimation(parent: a, curve: iosCurve, reverseCurve: iosCurve.flipped)),
        child: child,
      ),
    ));

Future<T?> showIosDrawer<T>(BuildContext context, WidgetBuilder builder) =>
    Navigator.of(context, rootNavigator: true).push(PageRouteBuilder<T>(
      opaque: false,
      barrierDismissible: true,
      barrierLabel: 'close',
      barrierColor: Colors.black.withOpacity(.55),
      transitionDuration: const Duration(milliseconds: 370),
      reverseTransitionDuration: const Duration(milliseconds: 290),
      pageBuilder: (c, a, s) => Material(type: MaterialType.transparency, child: Align(alignment: Alignment.centerLeft, child: builder(c))),
      transitionsBuilder: (c, a, s, child) => SlideTransition(
        position: Tween(begin: const Offset(-1, 0), end: Offset.zero)
            .animate(CurvedAnimation(parent: a, curve: iosCurve, reverseCurve: iosCurve.flipped)),
        child: child,
      ),
    ));

Future<T?> showIosDialog<T>(BuildContext context, WidgetBuilder builder) =>
    Navigator.of(context, rootNavigator: true).push(PageRouteBuilder<T>(
      opaque: false,
      barrierDismissible: false,
      barrierColor: Colors.black.withOpacity(.5),
      transitionDuration: const Duration(milliseconds: 340),
      reverseTransitionDuration: const Duration(milliseconds: 180),
      pageBuilder: (c, a, s) => Material(type: MaterialType.transparency, child: Center(child: builder(c))),
      transitionsBuilder: (c, a, s, child) => FadeTransition(
        opacity: CurvedAnimation(parent: a, curve: Curves.easeOut),
        child: ScaleTransition(
          scale: Tween(begin: .9, end: 1.0)
              .animate(CurvedAnimation(parent: a, curve: Curves.easeOutBack, reverseCurve: Curves.easeIn)),
          child: child,
        ),
      ),
    ));

/// The iOS activity indicator (the spinning ticks) instead of Material's circle.
class IosSpinner extends StatelessWidget {
  final double size;
  final Color color;
  const IosSpinner({super.key, this.size = 24, this.color = C.teal});
  @override
  Widget build(BuildContext context) => SizedBox(
        width: size,
        height: size,
        child: FittedBox(child: CupertinoActivityIndicator(radius: 10, color: color)),
      );
}

/// Lets a bottom sheet follow the finger down and be flicked away, like an iOS sheet.
/// Past the halfway-ish point (or a quick flick) it closes; otherwise it springs back.
class _SheetDrag extends StatefulWidget {
  final Widget child;
  const _SheetDrag({required this.child});
  @override
  State<_SheetDrag> createState() => _SheetDragState();
}

class _SheetDragState extends State<_SheetDrag> with SingleTickerProviderStateMixin {
  late final AnimationController _c = AnimationController.unbounded(vsync: this, value: 0);
  bool _closing = false;
  static const _snap = SpringDescription(mass: 1, stiffness: 380, damping: 30);
  static const _maxUp = 18.0; // most the sheet can stretch upward (px)
  final _box = GlobalKey();

  void _update(DragUpdateDetails d) {
    if (_closing) return;
    final dy = d.delta.dy;
    // Dragging up past the resting spot stretches a little, then stops (like iOS) —
    // the sheet can never be pulled up off the top of the screen.
    _c.stop();
    if (dy < 0 && _c.value <= 0) {
      final room = ((_maxUp + _c.value) / _maxUp).clamp(0.0, 1.0);
      _c.value = (_c.value + dy * .5 * room).clamp(-_maxUp, 0.0);
    } else {
      _c.value = (_c.value + dy).clamp(-_maxUp, double.infinity);
    }
  }

  void _end(DragEndDetails d) {
    if (_closing) return;
    final v = d.velocity.pixelsPerSecond.dy;
    if (_c.value > 110 || v > 800) {
      _closing = true;
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
