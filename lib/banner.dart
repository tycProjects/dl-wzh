import 'dart:async';
import 'dart:math' as math;
import 'package:flutter/material.dart' hide Text;
import 'anim.dart';
import 'theme.dart';
import 'tr.dart';

enum BannerKind { ok, warn, err, info, spin }

/// The iOS-style notification banner of the HTML app (wzhBanner): a translucent pill that pops
/// in from the top, reveals its text word by word, then shrinks away. Tap it or swipe it up to
/// dismiss. Only one is shown at a time; a new one replaces the old one.
class IosBanner {
  static OverlayEntry? _entry;
  static _BannerViewState? _state;

  static void show(
    BuildContext context, {
    required String title,
    String msg = '',
    BannerKind kind = BannerKind.info,
    Duration? duration = const Duration(milliseconds: 3500),
  }) {
    showOn(Overlay.maybeOf(context, rootOverlay: true), title: title, msg: msg, kind: kind, duration: duration);
  }

  /// Same as [show] but from an overlay you grabbed earlier (for places where the screen that
  /// wants to speak is already closing, e.g. leaving a build that is still running).
  static void showOn(
    OverlayState? overlay, {
    required String title,
    String msg = '',
    BannerKind kind = BannerKind.info,
    Duration? duration = const Duration(milliseconds: 3500),
  }) {
    if (overlay == null || !overlay.mounted) return;
    hide();
    late final OverlayEntry entry;
    final key = GlobalKey<_BannerViewState>();
    entry = OverlayEntry(
      builder: (_) => _BannerView(
        key: key,
        title: title,
        msg: msg,
        kind: kind,
        duration: duration,
        onGone: () {
          if (_entry == entry) {
            _entry = null;
            _state = null;
          }
          entry.remove();
        },
      ),
    );
    _entry = entry;
    overlay.insert(entry);
    WidgetsBinding.instance.addPostFrameCallback((_) => _state = key.currentState);
  }

  static void hide() {
    final s = _state;
    final e = _entry;
    if (s != null && s.mounted) {
      s.dismiss();
    } else if (e != null) {
      e.remove();
    }
    _entry = null;
    _state = null;
  }
}

class _BannerView extends StatefulWidget {
  final String title, msg;
  final BannerKind kind;
  final Duration? duration;
  final VoidCallback onGone;
  const _BannerView({super.key, required this.title, required this.msg, required this.kind, required this.duration, required this.onGone});
  @override
  State<_BannerView> createState() => _BannerViewState();
}

class _BannerViewState extends State<_BannerView> with TickerProviderStateMixin {
  late final AnimationController _in = AnimationController(vsync: this, duration: const Duration(milliseconds: 480));
  late final AnimationController _out = AnimationController(vsync: this, duration: const Duration(milliseconds: 240));
  late final List<String> _words = widget.title.split(RegExp(r'\s+')).where((w) => w.isNotEmpty).toList();
  late final double _msgDelay = math.min(.6, .16 + _words.length * .06 + .08);
  late final AnimationController _txt;
  double _drag = 0;
  bool _gone = false;
  Timer? _timer;

  @override
  void initState() {
    super.initState();
    final total = math.max(.16 + _words.length * .06 + .42, widget.msg.isEmpty ? 0 : _msgDelay + .45);
    _txt = AnimationController(vsync: this, duration: Duration(milliseconds: (total * 1000).round()));
    _in.forward();
    _txt.forward();
    final d = widget.duration;
    if (d != null) _timer = Timer(d, dismiss);
  }

  void dismiss() {
    if (_gone) return;
    _gone = true;
    _timer?.cancel();
    _out.forward().whenComplete(widget.onGone);
  }

  @override
  void dispose() {
    _timer?.cancel();
    _in.dispose();
    _out.dispose();
    _txt.dispose();
    super.dispose();
  }

  /// Fades and slides a piece of text up a few pixels, starting after [delay] seconds.
  Widget _reveal(double delay, double dur, Widget child) {
    final total = _txt.duration!.inMilliseconds / 1000;
    final a = CurvedAnimation(
      parent: _txt,
      curve: Interval((delay / total).clamp(0.0, 1.0), ((delay + dur) / total).clamp(0.0, 1.0), curve: Curves.easeOutCubic),
    );
    return FadeTransition(
      opacity: a,
      child: AnimatedBuilder(animation: a, builder: (_, c) => Transform.translate(offset: Offset(0, 5 * (1 - a.value)), child: c), child: child),
    );
  }

  Widget _icon() {
    switch (widget.kind) {
      case BannerKind.spin:
        return const IosSpinner(size: 20);
      case BannerKind.ok:
        return _dot(const Color(0xFF4CD1A2), Icons.check_rounded);
      case BannerKind.warn:
        return _dot(const Color(0xFFFFB340), Icons.priority_high_rounded);
      case BannerKind.err:
        return _dot(const Color(0xFFFF6B6B), Icons.close_rounded);
      case BannerKind.info:
        // A plain "i" in a grey circle. (It used to draw an exclamation mark, which reads as a warning.)
        return const Icon(Icons.info_rounded, size: 22, color: Color(0xFF8E8E93));
    }
  }

  Widget _dot(Color c, IconData i) => Container(
        width: 22, height: 22, alignment: Alignment.center,
        decoration: BoxDecoration(color: c, shape: BoxShape.circle),
        child: Icon(i, size: 15, color: const Color(0xFF111111)),
      );

  @override
  Widget build(BuildContext context) {
    final top = MediaQuery.of(context).padding.top + 10;
    final scaleIn = TweenSequence<double>([
      TweenSequenceItem(tween: Tween(begin: .8, end: 1.04).chain(CurveTween(curve: Curves.easeOut)), weight: 55),
      TweenSequenceItem(tween: Tween(begin: 1.04, end: 1.0).chain(CurveTween(curve: Curves.easeInOut)), weight: 45),
    ]).animate(_in);
    final fadeIn = CurvedAnimation(parent: _in, curve: const Interval(0, .55, curve: Curves.easeOut));
    final outC = CurvedAnimation(parent: _out, curve: Curves.easeIn);

    final words = <Widget>[
      for (var i = 0; i < _words.length; i++)
        _reveal(.16 + i * .06, .42, Text(i < _words.length - 1 ? '${_words[i]} ' : _words[i], style: inter(16, w: FontWeight.w600, h: 1.25))),
    ];

    return Positioned(
      top: top + _drag,
      left: 0,
      right: 0,
      child: Center(
        child: ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: 420),
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 10),
            child: AnimatedBuilder(
              animation: Listenable.merge([_in, _out]),
              builder: (_, child) => Opacity(
                opacity: (fadeIn.value * (1 - outC.value)).clamp(0.0, 1.0),
                child: Transform.scale(scale: scaleIn.value * (1 - .12 * outC.value), child: child),
              ),
              child: GestureDetector(
                behavior: HitTestBehavior.opaque,
                onTap: dismiss,
                onVerticalDragUpdate: (d) => setState(() => _drag = math.min(0, _drag + d.delta.dy)),
                onVerticalDragEnd: (_) {
                  if (_drag < -30) {
                    dismiss();
                  } else {
                    setState(() => _drag = 0);
                  }
                },
                child: Material(
                  type: MaterialType.transparency,
                  child: ClipRRect(
                    borderRadius: BorderRadius.circular(22),
                    child: RepaintBoundary(
                      child: Container(
                        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
                        decoration: BoxDecoration(
                          color: const Color(0xF22C2C2E),
                          borderRadius: BorderRadius.circular(22),
                          border: Border.all(color: const Color(0x1AFFFFFF)),
                          boxShadow: const [BoxShadow(color: Color(0x73000000), blurRadius: 16, offset: Offset(0, 6))],
                        ),
                        child: Row(children: [
                          SizedBox(width: 22, height: 22, child: Center(child: _icon())),
                          const SizedBox(width: 12),
                          Expanded(
                            child: Column(crossAxisAlignment: CrossAxisAlignment.start, mainAxisSize: MainAxisSize.min, children: [
                              Wrap(children: words),
                              if (widget.msg.isNotEmpty)
                                _reveal(_msgDelay, .45, Padding(padding: const EdgeInsets.only(top: 1), child: Text(widget.msg, style: inter(13.5, c: const Color(0xFF8E8E93), h: 1.3)))),
                            ]),
                          ),
                        ]),
                      ),
                    ),
                  ),
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}
