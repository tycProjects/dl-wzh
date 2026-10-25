import 'dart:async';
import 'dart:io';
import 'dart:math' as math;
import 'package:flutter/material.dart' hide Text;
import 'package:flutter/scheduler.dart' show Ticker;
import '../api.dart';
import '../theme.dart';
import '../widgets.dart';
import '../anim.dart';
import '../appearance.dart';
import '../save_install.dart';
import '../quick/quick_builder.dart';
import '../tr.dart';
import 'build_page.dart' show fmtClock, fmtSize;

/// Quick build progress: the same screen as the full build (app icon, smooth bar, stats, step timeline),
/// but the work happens on this phone. Pops 'full' if the person asks for a server build instead.
class QuickBuildPage extends StatefulWidget {
  final BuildRequest req;
  final String appName;
  const QuickBuildPage({super.key, required this.req, required this.appName});
  @override
  State<QuickBuildPage> createState() => _QuickBuildPageState();
}

class _QuickBuildPageState extends State<QuickBuildPage> with TickerProviderStateMixin {
  late final AnimationController intro = AnimationController(vsync: this, duration: const Duration(milliseconds: 1400))..forward();
  static const _names = ['Prepare', 'Manifest', 'Build', 'Ready'];
  static const _hints = ['Preparing the template and icon', 'Building the manifest', 'Building and signing your app', 'Ready to install'];

  Timer? tick;
  final DateTime started = DateTime.now();
  Duration elapsed = Duration.zero, finalTime = Duration.zero;
  int stageIdx = 0;
  final stageTimes = <int, Duration>{};
  Duration stageStart = Duration.zero;
  String step = '';
  String? path, error, savedAt;
  bool templateMissing = false, finished = false, showDetails = false;
  int? size;

  // Same smooth bar as the full build: each stage owns a quarter, the bar crawls toward (never past)
  // the end of the current quarter, then glides on when the real stage changes.
  final ValueNotifier<double> _shown = ValueNotifier<double>(0);
  Ticker? _crawl;
  Duration _lastFrame = Duration.zero;
  DateTime _stageWall = DateTime.now();
  static const _tau = [1.5, 1.5, 4.0]; // seconds a stage takes to reach ~63% of its quarter (quick builds are short)

  bool get _done => path != null;
  bool get _failed => error != null;

  double _target() {
    if (_done) return 1.0;
    final s = stageIdx.clamp(0, 2);
    final t = DateTime.now().difference(_stageWall).inMilliseconds / 1000.0;
    return s / 4 + .25 * .94 * (1 - math.exp(-t / _tau[s]));
  }

  void _onFrame(Duration now) {
    final dt = ((now - _lastFrame).inMicroseconds / 1e6).clamp(0.0, .1).toDouble();
    _lastFrame = now;
    if (_failed) return; // freeze where it stopped
    final gap = _target() - _shown.value;
    if (gap <= 0) return;
    final maxSpeed = _done ? .35 : .22;
    final stepV = math.min(gap * (1 - math.exp(-dt * 1.5)), maxSpeed * dt);
    _shown.value = (_shown.value + stepV).clamp(0.0, 1.0).toDouble();
  }

  Animation<double> _iv(double a, double b, [Curve k = iosCurve]) => CurvedAnimation(parent: intro, curve: Interval(a, b, curve: k));

  int _stageOf(String s) {
    if (s.startsWith('Building the manifest')) return 1;
    if (s.startsWith('Building and signing') || s == 'Saving') return 2;
    return 0;
  }

  void _setStage(int idx) {
    if (idx == stageIdx) return;
    stageTimes[stageIdx] = elapsed - stageStart;
    stageIdx = idx;
    stageStart = elapsed;
    _stageWall = DateTime.now();
  }

  @override
  void initState() {
    super.initState();
    _crawl = createTicker(_onFrame)..start();
    tick = Timer.periodic(const Duration(milliseconds: 500), (_) {
      if (!mounted || finished) return;
      setState(() => elapsed = DateTime.now().difference(started));
    });
    _run();
  }

  @override
  void dispose() {
    tick?.cancel();
    intro.dispose();
    _crawl?.dispose();
    _shown.dispose();
    super.dispose();
  }

  Future<void> _run() async {
    try {
      final p = await QuickBuild.run(widget.req, appName: widget.appName, onStep: (s) {
        if (!mounted) return;
        setState(() {
          step = s;
          elapsed = DateTime.now().difference(started);
          _setStage(_stageOf(s));
        });
      });
      int? n;
      try {
        n = await File(p).length();
      } catch (_) {}
      final pub = await FileSaver.saveToDownloads(p, widget.appName, fileName: '${FileSaver.safe(widget.appName)}.apk');
      if (!mounted) return; // left the page: the file is still saved to Downloads
      final total = DateTime.now().difference(started);
      setState(() {
        stageTimes[stageIdx] = total - stageStart;
        stageIdx = 3;
        finalTime = total;
        elapsed = total;
        path = p;
        size = n;
        savedAt = pub;
        finished = true;
      });
      // Open the installer by itself (Android asks to allow "install unknown apps" if needed).
      await Future.delayed(const Duration(milliseconds: 450));
      if (mounted) FileSaver.install(p);
    } on QuickBuildException catch (e) {
      if (mounted) setState(() { error = e.message; templateMissing = e.templateMissing; finished = true; });
    } catch (e) {
      if (mounted) setState(() { error = 'Something broke while building. Try again or build on the server instead.'; finished = true; });
    }
  }

  Widget _in(Animation<double> a, Widget child) => FadeTransition(
        opacity: a,
        child: SlideTransition(position: Tween(begin: const Offset(0, .25), end: Offset.zero).animate(a), child: child),
      );

  Widget _outlined(String t, VoidCallback? f, {Color c = C.text}) => Pressable(
        onTap: f,
        child: Container(
          width: double.infinity, height: 52, alignment: Alignment.center,
          decoration: BoxDecoration(color: const Color(0xFF2A2A2C), borderRadius: BorderRadius.circular(26)),
          child: Text(t, style: inter(18, w: FontWeight.w600, c: f == null ? C.muted : c)),
        ),
      );

  Widget _solid(String t, VoidCallback f) => Pressable(
        onTap: f,
        child: Container(
          width: double.infinity, height: 52, alignment: Alignment.center,
          decoration: BoxDecoration(color: C.tealBtn, borderRadius: BorderRadius.circular(26)),
          child: Text(t, style: inter(20, w: FontWeight.w700, c: const Color(0xFF0F2A24))),
        ),
      );

  Widget _appIcon() {
    final name = widget.appName.trim();
    final letter = name.isEmpty ? 'A' : name.characters.first.toUpperCase();
    final fallback = Container(
      color: const Color(0xFF1F3B32),
      alignment: Alignment.center,
      child: Text(letter, verbatim: true, style: inter(34, w: FontWeight.w700, c: C.teal, h: 1)),
    );
    final lp = widget.req.logoPath;
    return ClipRRect(
      borderRadius: BorderRadius.circular(20),
      child: SizedBox(
        width: 76, height: 76,
        child: lp != null && File(lp).existsSync()
            ? Unfilter(child: Image.file(File(lp), key: ValueKey(lp), fit: BoxFit.cover, errorBuilder: (_, __, ___) => fallback))
            : fallback,
      ),
    );
  }

  Widget _bar() => ClipRRect(
        borderRadius: BorderRadius.circular(3),
        child: SizedBox(
          height: 6,
          child: Stack(children: [
            Container(color: const Color(0xFF2A2A2C)),
            ValueListenableBuilder<double>(
              valueListenable: _shown,
              builder: (_, v, __) => FractionallySizedBox(
                alignment: Alignment.centerLeft,
                widthFactor: v.clamp(0.0, 1.0).toDouble(),
                heightFactor: 1,
                child: Container(color: C.teal),
              ),
            ),
          ]),
        ),
      );

  Widget _stat(String value, String label) => Expanded(
        child: Column(children: [
          Text(value, verbatim: true, maxLines: 1, overflow: TextOverflow.ellipsis, style: inter(17, w: FontWeight.w600)),
          const SizedBox(height: 3),
          Text(label, style: inter(13, c: C.muted)),
        ]),
      );

  Widget _statsRow(int kind) {
    final d = kind == 1 ? finalTime : elapsed;
    final cells = kind == 1
        ? [if (size != null) _stat(fmtSize(size!), 'Size'), _stat('APK', 'Type'), _stat(fmtClock(d), 'Time')]
        : [_stat('APK', 'Type'), _stat('${stageIdx + 1}/4', 'Stage'), _stat(fmtClock(d), 'Time')];
    final row = <Widget>[];
    for (var i = 0; i < cells.length; i++) {
      if (i > 0) row.add(Container(width: .5, height: 30, color: const Color(0xFF2E2E30)));
      row.add(cells[i]);
    }
    return Container(
      padding: const EdgeInsets.symmetric(vertical: 14),
      decoration: const BoxDecoration(border: Border.symmetric(horizontal: BorderSide(color: C.line))),
      child: Row(children: row),
    );
  }

  /// The four stages as a vertical timeline: dot, line, name, time.
  Widget _steps(int kind, List<String?> times) {
    Widget row(int i) {
      final bool d = kind == 1 || i < stageIdx;
      final bool a = kind == 0 && i == stageIdx;
      final bool f = kind == 2 && i == stageIdx;
      final dot = d ? C.teal : (f ? C.red : (a ? const Color(0xFF24473E) : const Color(0xFF2A2A2C)));
      final sub = a ? (step.isNotEmpty ? step : _hints[i]) : null;
      return IntrinsicHeight(
        child: Row(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
          SizedBox(
            width: 26,
            child: Column(children: [
              AnimatedContainer(
                duration: const Duration(milliseconds: 320), curve: iosCurve,
                width: 26, height: 26,
                decoration: BoxDecoration(color: dot, shape: BoxShape.circle),
                child: d
                    ? const Icon(Icons.check_rounded, size: 16, color: Color(0xFF062019))
                    : f
                        ? const Icon(Icons.close_rounded, size: 16, color: Colors.white)
                        : a
                            ? const Padding(padding: EdgeInsets.all(6), child: CircularProgressIndicator(strokeWidth: 2.2, color: C.teal))
                            : null,
              ),
              if (i < 3) Expanded(child: Container(width: 2, margin: const EdgeInsets.symmetric(vertical: 3), color: d ? C.teal : const Color(0xFF2A2A2C))),
            ]),
          ),
          const SizedBox(width: 14),
          Expanded(
            child: Padding(
              padding: EdgeInsets.only(top: 2, bottom: i < 3 ? 18 : 0),
              child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                Text(_names[i], style: inter(17, c: (d || a || f) ? C.text : C.muted)),
                if (sub != null) ...[const SizedBox(height: 2), Text(sub, maxLines: 2, overflow: TextOverflow.ellipsis, style: inter(13, c: C.muted, h: 1.3))],
              ]),
            ),
          ),
          if (d && i < 3 && times[i] != null)
            Padding(padding: const EdgeInsets.only(top: 4), child: Text(times[i]!, verbatim: true, style: inter(14, c: C.muted))),
        ]),
      );
    }
    return Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [for (var i = 0; i < 4; i++) row(i)]);
  }

  Future<void> _leave() async {
    final leave = await confirmLeave(context,
        title: 'Leave build progress?',
        message: 'Your app keeps building on this phone and is saved to Downloads when it is done.',
        stay: 'Stay here');
    if (leave && mounted) Navigator.of(context).pop();
  }

  Widget _xButton() => Pressable(
        onTap: _leave,
        scale: .88,
        child: Container(
          width: 36, height: 36,
          decoration: const BoxDecoration(color: Color(0xFF2A2A2C), shape: BoxShape.circle),
          child: const Icon(Icons.close_rounded, size: 20, color: C.text),
        ),
      );

  Widget _failCard() => Container(
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(color: C.card, borderRadius: BorderRadius.circular(16), border: Border.all(color: const Color(0xFF3A2A2A))),
        child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Row(children: [
            Container(
              width: 30, height: 30,
              decoration: const BoxDecoration(color: Color(0x33F0483E), shape: BoxShape.circle),
              child: const Icon(Icons.priority_high_rounded, size: 18, color: C.red),
            ),
            const SizedBox(width: 10),
            Expanded(child: Text('Quick build didn\u2019t finish', style: inter(17, w: FontWeight.w700, h: 1.2))),
          ]),
          const SizedBox(height: 10),
          Text(error ?? '', style: inter(15, h: 1.4)),
          const SizedBox(height: 14),
          Text('What to try', style: inter(13, w: FontWeight.w700, c: C.teal)),
          const SizedBox(height: 6),
          for (final t in (templateMissing
              ? ['Connect to the internet so it can fetch its template once.', 'Or build on the server instead.']
              : ['Try again once.', 'Or build on the server instead.']))
            Padding(
              padding: const EdgeInsets.only(bottom: 4),
              child: Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
                Text('\u2022  ', verbatim: true, style: inter(15, c: C.muted, h: 1.4)),
                Expanded(child: Text(t, style: inter(15, c: C.muted, h: 1.4))),
              ]),
            ),
        ]),
      );

  /// What sits where the Install button is on an app store page.
  Widget _status(int kind, String sub1) {
    if (kind == 1) {
      final shown = (savedAt ?? path ?? '').split('/').last;
      return Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
        _solid('Install', () => FileSaver.install(path!)),
        const SizedBox(height: 12),
        Text(
          '${savedAt != null ? 'Saved to Downloads \u00B7' : 'Couldn\u2019t save to Downloads, but your file is ready:'} $shown',
          style: inter(14, c: C.muted, h: 1.35),
        ),
        const SizedBox(height: 4),
        Text('The installer opens by itself. If it doesn\u2019t, tap Install.', style: inter(14, c: C.muted, h: 1.35)),
      ]);
    }
    if (kind == 2) {
      return Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
        _failCard(),
        const SizedBox(height: 12),
        if (templateMissing) ...[
          _solid('Build on the server instead', () => Navigator.of(context).pop('full')),
          const SizedBox(height: 10),
          _outlined('Back', () => Navigator.of(context).maybePop()),
        ] else ...[
          _solid('Try again', () => Navigator.of(context).maybePop()),
          const SizedBox(height: 10),
          _outlined('Build on the server instead', () => Navigator.of(context).pop('full')),
        ],
      ]);
    }
    return Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
      Row(children: [
        Expanded(child: Text(sub1, maxLines: 1, overflow: TextOverflow.ellipsis, style: inter(14, c: C.muted))),
        Text('Step ${stageIdx + 1} of 4 \u00B7 ${fmtClock(elapsed)}', verbatim: true, style: inter(14, c: C.muted)),
      ]),
      const SizedBox(height: 10),
      Row(children: [Expanded(child: _bar()), const SizedBox(width: 14), _xButton()]),
    ]);
  }

  @override
  Widget build(BuildContext context) => PopScope(
        // A build that is still running asks first; a finished or failed one just closes.
        canPop: finished,
        onPopInvokedWithResult: (didPop, _) {
          if (didPop) return;
          _leave();
        },
        child: _page(context),
      );

  Widget _page(BuildContext context) {
    final kind = _done ? 1 : (_failed ? 2 : 0);
    final sub1 = _done ? 'Ready to install' : (_failed ? 'Build failed' : _hints[stageIdx.clamp(0, 3)]);
    final subColor = kind == 1 ? C.teal : (kind == 2 ? C.red : C.muted);
    final times = List<String?>.generate(3, (i) => stageTimes[i] != null ? fmtClock(stageTimes[i]!) : null);

    return Scaffold(
      body: SafeArea(
        child: Stack(children: [
          Column(children: [
            const TopBar(back: true),
            Expanded(
              child: ListView(
                physics: const BouncingScrollPhysics(parent: AlwaysScrollableScrollPhysics()),
                padding: const EdgeInsets.fromLTRB(20, 24, 20, 110),
                children: [
                  _in(_iv(0, .5), Row(children: [
                    _appIcon(),
                    const SizedBox(width: 16),
                    Expanded(
                      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                        Text(widget.appName, verbatim: true, maxLines: 2, overflow: TextOverflow.ellipsis, style: inter(22, w: FontWeight.w700, h: 1.2)),
                        const SizedBox(height: 3),
                        Text('WZH 2 APK', verbatim: true, style: inter(14.5, c: C.teal)),
                        const SizedBox(height: 2),
                        AnimatedSwitcher(
                          duration: const Duration(milliseconds: 240),
                          child: Text(kind == 0 ? 'APK' : sub1, key: ValueKey('$kind'), maxLines: 1, overflow: TextOverflow.ellipsis, style: inter(13.5, c: subColor)),
                        ),
                      ]),
                    ),
                  ])),
                  const SizedBox(height: 22),
                  _in(_iv(.15, .6), AnimatedSize(
                    duration: const Duration(milliseconds: 280), curve: iosCurve, alignment: Alignment.topCenter,
                    child: AnimatedSwitcher(
                      duration: const Duration(milliseconds: 260),
                      child: KeyedSubtree(key: ValueKey('$kind'), child: _status(kind, sub1)),
                    ),
                  )),
                  if (kind == 0 || kind == 1) ...[
                    const SizedBox(height: 22),
                    _in(_iv(.3, .75), _statsRow(kind)),
                  ],
                  const SizedBox(height: 26),
                  _in(_iv(.4, .9), Text('Build steps', style: inter(19, w: FontWeight.w600))),
                  const SizedBox(height: 16),
                  _in(_iv(.5, 1), _steps(kind, times)),
                ],
              ),
            ),
          ]),
          const Positioned(right: 17, bottom: 20, child: BotFab()),
        ]),
      ),
    );
  }
}
