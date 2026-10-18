import 'package:flutter/material.dart';
import '../theme.dart';
import '../anim.dart';

const splashInfo = <String, String>{
  'Off': 'No loading screen at all, opens straight into your site',
  'Custom': 'Your own video or image plays instead',
  'Native': "Your app icon on your site's color, then a soft fade into the app",
  'iOS zoom': 'Launch screen zooms toward you as the app opens',
  'Slide away': 'Launch screen lifts up to reveal the app',
  'Pop': 'Icon swells, then vanishes into the app',
};

class SplashSheet extends StatefulWidget {
  final String selected;
  const SplashSheet({super.key, required this.selected});
  @override
  State<SplashSheet> createState() => _SplashSheetState();
}

class _SplashSheetState extends State<SplashSheet> with SingleTickerProviderStateMixin {
  late String sel = widget.selected;
  String speed = 'Normal';
  static const _ms = {'Slow': 3600, 'Normal': 2600, 'Fast': 1700};
  late final AnimationController _loop =
      AnimationController(vsync: this, duration: Duration(milliseconds: _ms[speed]!))..repeat();

  bool get _isLaunch => sel != 'Off' && sel != 'Custom';

  void _setSpeed(String v) {
    setState(() => speed = v);
    _loop.duration = Duration(milliseconds: _ms[v]!);
    _loop.repeat();
  }

  @override
  void dispose() {
    _loop.dispose();
    super.dispose();
  }

  Widget _indicator(bool on) => AnimatedContainer(
        duration: const Duration(milliseconds: 250),
        curve: iosCurve,
        width: 20, height: 20,
        decoration: BoxDecoration(
          shape: BoxShape.circle,
          color: on ? C.teal : Colors.transparent,
          border: Border.all(color: on ? C.teal : const Color(0xFF8A8A8E), width: 1.5)),
        child: AnimatedScale(
          scale: on ? 1 : 0,
          duration: const Duration(milliseconds: 260),
          curve: Curves.easeOutBack,
          child: const Icon(Icons.check_rounded, size: 14, color: C.onTeal),
        ),
      );

  Widget _card(String name) {
    final on = sel == name;
    final black = name == 'Off' || name == 'Custom';
    return Pressable(
      scale: .975,
      onTap: () => setState(() => sel = name),
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 300),
        curve: iosCurve,
        decoration: BoxDecoration(
          color: C.card, borderRadius: BorderRadius.circular(22),
          border: Border.all(color: on ? C.teal : const Color(0xFF2B2B2D), width: on ? 2 : 1)),
        clipBehavior: Clip.antiAlias,
        child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          SizedBox(
            height: 76,
            child: Stack(children: [
              Positioned.fill(child: Container(color: black ? Colors.black : const Color(0xFF16171A))),
              if (black)
                Center(child: Padding(
                  padding: const EdgeInsets.only(top: 14),
                  child: Text(name == 'Off' ? 'Straight to site' : 'Upload below', style: inter(14, c: Colors.white)),
                ))
              else
                Align(alignment: Alignment.bottomCenter, child: _LaunchPreview(style: name, t: _loop)),
              if (name == 'Custom')
                Positioned(
                  left: 11, top: 10,
                  child: Container(
                    padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                    decoration: BoxDecoration(color: const Color(0xFF1F3B32), borderRadius: BorderRadius.circular(12)),
                    child: Text('Recommended', style: inter(12, w: FontWeight.w600, c: C.teal, h: 1.1)),
                  ),
                ),
              Positioned(right: 11, top: 10, child: _indicator(on)),
            ]),
          ),
          Padding(
            padding: const EdgeInsets.fromLTRB(14, 12, 12, 14),
            child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              Text(name, style: inter(18, w: FontWeight.w700)),
              const SizedBox(height: 4),
              Text(splashInfo[name]!, style: inter(14.5, c: C.muted, h: 1.3)),
            ]),
          ),
        ]),
      ),
    );
  }

  Widget _speed(String v) {
    final on = _isLaunch && speed == v;
    return Expanded(
      child: Pressable(
        scale: .96,
        onTap: () => _setSpeed(v),
        child: AnimatedContainer(
          duration: const Duration(milliseconds: 260),
          curve: iosCurve,
          height: 46,
          alignment: Alignment.center,
          decoration: BoxDecoration(
            color: on ? const Color(0xFF1F3B32) : Colors.transparent,
            borderRadius: BorderRadius.circular(16),
            border: Border.all(color: on ? C.teal : const Color(0xFF3A3A3C), width: on ? 1.6 : 1.2)),
          child: Text(v, style: inter(16, w: FontWeight.w600, c: on ? C.teal : C.text)),
        ),
      ),
    );
  }

  Widget _label(String t) => Padding(
        padding: const EdgeInsets.only(bottom: 10),
        child: Text(t, style: inter(16, w: FontWeight.w600, c: const Color(0xFFD8D8DC))),
      );

  @override
  Widget build(BuildContext context) {
    final h = MediaQuery.of(context).size.height;
    return Container(
      height: h * .69,
      decoration: const BoxDecoration(color: Color(0xFF181818), borderRadius: BorderRadius.vertical(top: Radius.circular(28))),
      padding: const EdgeInsets.fromLTRB(21, 10, 21, 18),
      child: Column(children: [
        Container(width: 38, height: 5, decoration: BoxDecoration(color: const Color(0xFF3C3C40), borderRadius: BorderRadius.circular(3))),
        const SizedBox(height: 18),
        Expanded(
          child: ListView(
            physics: const BouncingScrollPhysics(),
            padding: EdgeInsets.zero,
            children: [
              Row(children: [
                const Icon(Icons.bolt_outlined, size: 24, color: Color(0xFFD8D8DC)),
                const SizedBox(width: 10),
                Text('Startup loading', style: inter(25, w: FontWeight.w600)),
              ]),
              const SizedBox(height: 10),
              Text('Pick how your app opens: a native-style launch screen, your own media, or nothing.',
                  style: inter(15.5, c: C.muted, h: 1.35)),
              const SizedBox(height: 16),
              Row(children: [
                _speed('Slow'), const SizedBox(width: 10),
                _speed('Normal'), const SizedBox(width: 10),
                _speed('Fast'),
              ]),
              const SizedBox(height: 22),
              _label('Off or your own media'),
              Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
                Expanded(child: _card('Off')),
                const SizedBox(width: 10),
                Expanded(child: _card('Custom')),
              ]),
              const SizedBox(height: 22),
              _label('Launch screen'),
              Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
                Expanded(child: _card('Native')),
                const SizedBox(width: 10),
                Expanded(child: _card('iOS zoom')),
              ]),
              const SizedBox(height: 10),
              Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
                Expanded(child: _card('Slide away')),
                const SizedBox(width: 10),
                Expanded(child: _card('Pop')),
              ]),
              const SizedBox(height: 8),
            ],
          ),
        ),
        const SizedBox(height: 12),
        Pressable(
          onTap: () => Navigator.pop(context, sel),
          child: Container(
            height: 52,
            alignment: Alignment.center,
            decoration: BoxDecoration(color: C.tealBtn, borderRadius: BorderRadius.circular(26)),
            child: Text('Done', style: inter(18, w: FontWeight.w700, c: const Color(0xFF0F2A24))),
          ),
        ),
      ]),
    );
  }
}

/// A tiny phone that loops through what each launch style does: launch screen, then the app.
class _LaunchPreview extends StatelessWidget {
  final String style;
  final Animation<double> t;
  const _LaunchPreview({required this.style, required this.t});

  Widget _app() => Container(
        color: const Color(0xFFEDEDEF),
        padding: const EdgeInsets.fromLTRB(6, 10, 6, 0),
        child: Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
          for (var i = 0; i < 5; i++)
            Container(
              height: 4,
              margin: const EdgeInsets.only(bottom: 5),
              width: i == 0 ? 22 : double.infinity,
              alignment: Alignment.centerLeft,
              decoration: BoxDecoration(color: i == 0 ? const Color(0xFF9A9AA0) : const Color(0xFFD0D0D4), borderRadius: BorderRadius.circular(2)),
            ),
        ]),
      );

  Widget _launch() => Container(
        color: const Color(0xFF0F2428),
        alignment: Alignment.center,
        child: Text('A', style: inter(12, w: FontWeight.w800)),
      );

  @override
  Widget build(BuildContext context) => Container(
        width: 41, height: 66,
        clipBehavior: Clip.antiAlias,
        decoration: BoxDecoration(
          borderRadius: const BorderRadius.vertical(top: Radius.circular(8)),
          border: Border.all(color: const Color(0xFF2E3A3D))),
        child: AnimatedBuilder(
          animation: t,
          builder: (context, _) {
            // 0–.3 launch screen, .3–.65 transition, then the app is shown until the loop restarts.
            final p = Curves.easeInOutCubic.transform(((t.value - .30) / .35).clamp(0.0, 1.0));
            Widget top;
            switch (style) {
              case 'iOS zoom':
                top = Opacity(opacity: 1 - p, child: Transform.scale(scale: 1 + p * 1.1, child: _launch()));
                break;
              case 'Slide away':
                top = Transform.translate(offset: Offset(0, -66 * p), child: _launch());
                break;
              case 'Pop':
                top = Stack(fit: StackFit.expand, children: [
                  Opacity(opacity: 1 - p, child: Container(color: const Color(0xFF0F2428))),
                  Center(child: Opacity(opacity: 1 - p, child: Transform.scale(scale: 1 + p * .9, child: Text('A', style: inter(12, w: FontWeight.w800)))))
                ]);
                break;
              default: // Native: a soft fade
                top = Opacity(opacity: 1 - p, child: _launch());
            }
            return Stack(fit: StackFit.expand, children: [_app(), top]);
          },
        ),
      );
}
