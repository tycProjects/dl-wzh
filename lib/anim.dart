import 'package:flutter/material.dart';
import 'theme.dart';

/// The curve iOS uses for sheets, drawers and page pushes.
const iosCurve = Cubic(.32, .72, 0, 1);

/// Soft "press in, spring back" feedback used on every tappable thing.
class Pressable extends StatefulWidget {
  final Widget child;
  final VoidCallback? onTap;
  final double scale;
  const Pressable({super.key, required this.child, this.onTap, this.scale = .97});
  @override
  State<Pressable> createState() => _PressableState();
}

class _PressableState extends State<Pressable> {
  bool down = false;
  @override
  Widget build(BuildContext context) => GestureDetector(
        behavior: HitTestBehavior.opaque,
        onTapDown: widget.onTap == null ? null : (_) => setState(() => down = true),
        onTapUp: (_) => setState(() => down = false),
        onTapCancel: () => setState(() => down = false),
        onTap: widget.onTap,
        child: AnimatedScale(
          scale: down ? widget.scale : 1,
          duration: Duration(milliseconds: down ? 90 : 420),
          curve: down ? Curves.easeOut : Curves.easeOutBack,
          child: AnimatedOpacity(
            opacity: down ? .85 : 1,
            duration: Duration(milliseconds: down ? 60 : 250),
            child: widget.child,
          ),
        ),
      );
}

/// Switch with the slide-and-colour animation of an iOS toggle.
class IosToggle extends StatelessWidget {
  final bool value;
  final ValueChanged<bool> onChanged;
  const IosToggle({super.key, required this.value, required this.onChanged});
  @override
  Widget build(BuildContext context) => GestureDetector(
        behavior: HitTestBehavior.opaque,
        onTap: () => onChanged(!value),
        child: AnimatedContainer(
          duration: const Duration(milliseconds: 320),
          curve: iosCurve,
          width: 50, height: 30,
          padding: const EdgeInsets.all(3),
          decoration: BoxDecoration(
            color: value ? C.tealBtn : const Color(0xFF3A3A3C),
            borderRadius: BorderRadius.circular(15)),
          child: AnimatedAlign(
            duration: const Duration(milliseconds: 320),
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
      transitionDuration: const Duration(milliseconds: 480),
      reverseTransitionDuration: const Duration(milliseconds: 380),
      pageBuilder: (c, a, s) => Material(type: MaterialType.transparency, child: Align(alignment: Alignment.bottomCenter, child: builder(c))),
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
      transitionDuration: const Duration(milliseconds: 460),
      reverseTransitionDuration: const Duration(milliseconds: 360),
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
      transitionDuration: const Duration(milliseconds: 420),
      reverseTransitionDuration: const Duration(milliseconds: 220),
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
