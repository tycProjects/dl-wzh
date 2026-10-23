import 'dart:math' as math;
import 'package:flutter/material.dart' hide Text;
import '../theme.dart';
import '../anim.dart';
import '../tr.dart';

const splashInfo = <String, String>{
  'Off': 'No loading screen at all, opens straight into your site',
  'Custom': 'Your own video or image plays instead',
  'Native': "Your app icon on your site's color, then a soft fade into the app",
  'iOS zoom': 'Launch screen zooms toward you as the app opens',
  'Slide away': 'Launch screen lifts up to reveal the app',
  'Pop': 'Icon swells, then vanishes into the app',
};

const splashIcons = <String, IconData>{
  'Off': Icons.block_rounded,
  'Custom': Icons.movie_creation_outlined,
  'Native': Icons.blur_on_rounded,
  'iOS zoom': Icons.zoom_out_map_rounded,
  'Slide away': Icons.vertical_align_top_rounded,
  'Pop': Icons.auto_awesome_rounded,
};

/// What the sheet hands back: the chosen style and how fast the launch screen plays.
class SplashChoice {
  final String style, speed;
  const SplashChoice(this.style, this.speed);
}

/// Same look as the Appearance and Language sheets: one tap picks and closes.
class SplashSheet extends StatefulWidget {
  final String selected, speed;
  const SplashSheet({super.key, required this.selected, this.speed = 'Normal'});
  @override
  State<SplashSheet> createState() => _SplashSheetState();
}

class _SplashSheetState extends State<SplashSheet> {
  late String sel = widget.selected;
  String get speed => widget.speed;

  static const _order = ['Off', 'Custom', 'Native', 'iOS zoom', 'Slide away', 'Pop'];

  Widget _row(BuildContext context, String name) {
    final on = sel == name;
    return Padding(
      padding: const EdgeInsets.only(bottom: 8),
      child: Pressable(
        scale: .985,
        onTap: () => setState(() => sel = name),
        child: Container(
          constraints: const BoxConstraints(minHeight: 64),
          padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
          decoration: BoxDecoration(
            color: C.card,
            borderRadius: BorderRadius.circular(16),
            border: Border.all(color: on ? C.teal : Colors.transparent, width: 1.5)),
          child: Row(children: [
            Container(
              width: 34, height: 34,
              decoration: BoxDecoration(
                color: on ? const Color(0xFF1F3B32) : const Color(0xFF2A2A2C),
                borderRadius: BorderRadius.circular(9)),
              child: Icon(splashIcons[name], size: 20, color: on ? C.teal : const Color(0xFFB4B4B8)),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: Column(crossAxisAlignment: CrossAxisAlignment.start, mainAxisSize: MainAxisSize.min, children: [
                Row(children: [
                  Flexible(child: Text(name, maxLines: 1, overflow: TextOverflow.ellipsis, style: inter(17, w: FontWeight.w600))),
                  if (name == 'Custom') ...[
                    const SizedBox(width: 8),
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                      decoration: BoxDecoration(color: C.tealDeep, borderRadius: BorderRadius.circular(8)),
                      child: Text('Recommended', style: inter(11, w: FontWeight.w600, c: C.teal, h: 1.1)),
                    ),
                  ],
                ]),
                const SizedBox(height: 2),
                Text(splashInfo[name]!, style: inter(13, c: C.muted, h: 1.3)),
              ]),
            ),
            if (on) ...[
              const SizedBox(width: 10),
              const Icon(Icons.check_rounded, size: 22, color: C.teal),
            ],
          ]),
        ),
      ),
    );
  }

  /// A small phone that plays the chosen launch animation on a loop, next to what it does.
  Widget _preview() => Container(
        padding: const EdgeInsets.all(12),
        decoration: BoxDecoration(color: C.card, borderRadius: BorderRadius.circular(20)),
        child: Row(children: [
          SplashPreview(style: sel),
          const SizedBox(width: 16),
          Expanded(
            child: Column(crossAxisAlignment: CrossAxisAlignment.start, mainAxisSize: MainAxisSize.min, children: [
              Text('What it looks like', style: inter(12, w: FontWeight.w700, c: C.teal)),
              const SizedBox(height: 4),
              Text(sel, style: inter(18, w: FontWeight.w700)),
              const SizedBox(height: 4),
              Text(splashInfo[sel]!, style: inter(13, c: C.muted, h: 1.35)),
              const SizedBox(height: 8),
              Text('Tap a style below to preview it.', style: inter(12, c: C.muted)),
            ]),
          ),
        ]),
      );

  @override
  Widget build(BuildContext context) {
    final mq = MediaQuery.of(context);
    return Container(
      width: double.infinity,
      constraints: BoxConstraints(maxHeight: mq.size.height * .88),
      padding: EdgeInsets.fromLTRB(16, 10, 16, 14 + mq.padding.bottom),
      decoration: const BoxDecoration(color: C.sheet, borderRadius: BorderRadius.vertical(top: Radius.circular(30))),
      child: Column(mainAxisSize: MainAxisSize.min, crossAxisAlignment: CrossAxisAlignment.stretch, children: [
        Center(child: Container(width: 38, height: 5, decoration: BoxDecoration(color: const Color(0xFF4A4A4C), borderRadius: BorderRadius.circular(3)))),
        const SizedBox(height: 16),
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 4),
          child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            Text('Startup loading', style: inter(20, w: FontWeight.w700)),
            const SizedBox(height: 1),
            Text('Pick how your app opens.', style: inter(13, c: C.muted)),
          ]),
        ),
        const SizedBox(height: 12),
        _preview(),
        const SizedBox(height: 12),
        Flexible(
          child: SingleChildScrollView(
            physics: const BouncingScrollPhysics(),
            child: Column(children: [for (final n in _order) _row(context, n)]),
          ),
        ),
        const SizedBox(height: 4),
        Pressable(
          onTap: () => Navigator.pop(context, SplashChoice(sel, speed)),
          child: Container(
            height: 54,
            alignment: Alignment.center,
            decoration: BoxDecoration(color: C.tealBtn, borderRadius: BorderRadius.circular(27)),
            child: Text(sel == widget.selected ? 'Done' : 'Use $sel', style: inter(17, w: FontWeight.w700, c: const Color(0xFF0F2A24))),
          ),
        ),
      ]),
    );
  }
}

/// A looping mini launch: the launch screen shows, plays the chosen exit, then the app is revealed.
class SplashPreview extends StatefulWidget {
  final String style;
  const SplashPreview({super.key, required this.style});
  @override
  State<SplashPreview> createState() => _SplashPreviewState();
}

class _SplashPreviewState extends State<SplashPreview> with SingleTickerProviderStateMixin {
  late final AnimationController c = AnimationController(vsync: this, duration: const Duration(milliseconds: 3600))..repeat();
  static const double w = 92, h = 184;
  static const launch = Color(0xFF2E7D6B);

  @override
  void dispose() {
    c.dispose();
    super.dispose();
  }

  Widget _app() => Container(
        color: const Color(0xFF151517),
        padding: const EdgeInsets.fromLTRB(8, 14, 8, 8),
        child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Container(height: 10, width: 44, decoration: BoxDecoration(color: const Color(0xFF3A3A3C), borderRadius: BorderRadius.circular(5))),
          const SizedBox(height: 10),
          Container(height: 52, decoration: BoxDecoration(color: C.tealDeep, borderRadius: BorderRadius.circular(8))),
          const SizedBox(height: 8),
          Row(children: [
            Expanded(child: Container(height: 40, decoration: BoxDecoration(color: const Color(0xFF232326), borderRadius: BorderRadius.circular(8)))),
            const SizedBox(width: 6),
            Expanded(child: Container(height: 40, decoration: BoxDecoration(color: const Color(0xFF232326), borderRadius: BorderRadius.circular(8)))),
          ]),
          const SizedBox(height: 8),
          Container(height: 10, decoration: BoxDecoration(color: const Color(0xFF232326), borderRadius: BorderRadius.circular(5))),
          const SizedBox(height: 6),
          Container(height: 10, width: 54, decoration: BoxDecoration(color: const Color(0xFF232326), borderRadius: BorderRadius.circular(5))),
        ]),
      );

  Widget _icon(double size) => Container(
        width: size, height: size,
        decoration: BoxDecoration(color: Colors.white, borderRadius: BorderRadius.circular(size * .24)),
        child: Icon(Icons.bolt_rounded, size: size * .62, color: launch),
      );

  /// Launch screen: [p] is how far the exit has played (0 to 1), [t] the loop position.
  Widget _launch(String style, double p, double t) {
    switch (style) {
      case 'Custom':
        return Opacity(
          opacity: 1 - p,
          child: Container(
            color: const Color(0xFF0B0B0C),
            child: Stack(alignment: Alignment.center, children: [
              const Icon(Icons.play_circle_outline_rounded, size: 34, color: Colors.white70),
              Positioned(
                left: 10, right: 10, bottom: 12,
                child: Align(
                  alignment: Alignment.centerLeft,
                  child: Container(height: 3, width: (w - 20) * (t / .5).clamp(0.0, 1.0), decoration: BoxDecoration(color: C.teal, borderRadius: BorderRadius.circular(2))),
                ),
              ),
            ]),
          ),
        );
      case 'iOS zoom':
        return Opacity(
          opacity: 1 - p,
          child: Transform.scale(scale: 1 + p * 1.1, child: Container(color: launch, alignment: Alignment.center, child: _icon(40))),
        );
      case 'Slide away':
        return Transform.translate(offset: Offset(0, -p * h), child: Container(color: launch, alignment: Alignment.center, child: _icon(40)));
      case 'Pop':
        final s = p < .45 ? 1 + (p / .45) * .4 : 1.4 * (1 - (p - .45) / .55);
        final io = p < .45 ? 1.0 : 1 - (p - .45) / .55;
        return Opacity(
          opacity: 1 - Curves.easeIn.transform(p),
          child: Container(
            color: launch,
            alignment: Alignment.center,
            child: Opacity(opacity: io.clamp(0.0, 1.0), child: Transform.scale(scale: math.max(s, 0.0), child: _icon(40))),
          ),
        );
      default: // Native: the icon on the site's color, then a soft fade
        return Opacity(opacity: 1 - p, child: Container(color: launch, alignment: Alignment.center, child: _icon(40)));
    }
  }

  @override
  Widget build(BuildContext context) => Container(
        width: w + 6, height: h + 6,
        padding: const EdgeInsets.all(3),
        decoration: BoxDecoration(color: const Color(0xFF3A3A3C), borderRadius: BorderRadius.circular(20)),
        child: ClipRRect(
          borderRadius: BorderRadius.circular(17),
          child: SizedBox(
            width: w, height: h,
            child: widget.style == 'Off'
                ? _app()
                : AnimatedBuilder(
                    animation: c,
                    builder: (_, __) {
                      final t = c.value;
                      // 0-.2 hold, .2-.5 exit, .5-.88 app visible, .88-1 launch screen comes back
                      var p = t < .2 ? 0.0 : (t < .5 ? Curves.easeInOutCubic.transform((t - .2) / .3) : 1.0);
                      var back = 0.0;
                      if (t >= .88) {
                        p = 0;
                        back = (t - .88) / .12;
                      }
                      final tt = t >= .88 ? 0.0 : t;
                      return Stack(fit: StackFit.expand, children: [
                        _app(),
                        if (t >= .88) Opacity(opacity: back, child: _launch(widget.style, 0, tt)) else _launch(widget.style, p, tt),
                      ]);
                    },
                  ),
          ),
        ),
      );
}
