import 'dart:async';
import 'dart:math' as math;
import 'dart:ui' show PathMetric;
import 'package:flutter/material.dart';
import '../api.dart';
import '../store.dart';
import '../theme.dart';
import '../widgets.dart';
import '../anim.dart';

enum _S { pending, active, done }

String fmtSize(int b) {
  if (b >= 1024 * 1024) return '${(b / 1048576).toStringAsFixed(1)} MB';
  return '${(b / 1024).toStringAsFixed(0)} KB';
}

String fmtClock(Duration d) {
  final m = d.inMinutes.toString().padLeft(2, '0');
  final s = (d.inSeconds % 60).toString().padLeft(2, '0');
  return '$m:$s';
}

/// Live progress, then the result, of one build. Works for a build you just started,
/// one still running from before, or a finished one opened from Recent builds.
class BuildPage extends StatefulWidget {
  final String jobId, appName;
  const BuildPage({super.key, required this.jobId, required this.appName});
  @override
  State<BuildPage> createState() => _BuildPageState();
}

class _BuildPageState extends State<BuildPage> with TickerProviderStateMixin {
  late final AnimationController intro = AnimationController(vsync: this, duration: const Duration(milliseconds: 1400))..forward();
  late final AnimationController spin = AnimationController(vsync: this, duration: const Duration(milliseconds: 1100))..repeat();
  Job? job;
  Timer? poll, tick;
  DateTime started = DateTime.now();
  Duration elapsed = Duration.zero;
  int lastLogs = 0, stageIdx = -1, failures = 0;
  final stageTimes = <int, Duration>{};
  Duration stageStart = Duration.zero;
  bool cancelling = false, gone = false, repairing = false, finished = false;

  Animation<double> _iv(double a, double b, [Curve k = iosCurve]) => CurvedAnimation(parent: intro, curve: Interval(a, b, curve: k));

  @override
  void initState() {
    super.initState();
    tick = Timer.periodic(const Duration(seconds: 1), (_) {
      if (!mounted || finished) return;
      setState(() => elapsed = DateTime.now().difference(started));
    });
    _poll();
  }

  @override
  void dispose() {
    poll?.cancel();
    tick?.cancel();
    intro.dispose();
    spin.dispose();
    super.dispose();
  }

  int _stage(String s) {
    switch (s) {
      case 'pushing':
        return 0;
      case 'queued':
      case 'queued-waiting':
      case 'queued_waiting':
        return 1;
      case 'done':
        return 3;
      default:
        return 2; // building, publishing, awaiting-repair-confirmation, anything else running
    }
  }

  Future<void> _poll() async {
    if (!mounted) return;
    Job j;
    try {
      j = await Api.i.status(widget.jobId, since: lastLogs);
      failures = 0;
    } on ApiError catch (e) {
      if (e.status == 404) {
        await Store.i.setActiveJob(null);
        if (mounted) setState(() { gone = true; finished = true; });
        return;
      }
      if (e.code == 'unauthorized') {
        if (mounted) showInfoDialog(context, title: 'Session expired', message: e.message, icon: Icons.lock_outline_rounded, color: C.red);
        return;
      }
      failures++;
      poll = Timer(Duration(seconds: failures > 3 ? 8 : 4), _poll);
      return;
    } catch (_) {
      failures++;
      poll = Timer(Duration(seconds: failures > 3 ? 8 : 4), _poll);
      return;
    }
    if (!mounted) return;
    lastLogs = j.logsTotal > lastLogs ? j.logsTotal : lastLogs;
    // The server's own start time keeps the clock right when reopening a running build.
    if (job == null && j.createdAt != null) {
      started = j.createdAt!;
      elapsed = DateTime.now().difference(started);
    }
    final idx = _stage(j.status);
    if (idx != stageIdx) {
      if (stageIdx >= 0) stageTimes[stageIdx] = elapsed - stageStart;
      stageIdx = idx;
      stageStart = elapsed;
    }
    final end = j.isDone || j.isError || j.isCancelled;
    setState(() {
      job = j;
      if (end) finished = true;
    });
    if (end) {
      await Store.i.setActiveJob(null);
      Store.i.refreshHistory();
      Store.i.refreshLimit();
      return;
    }
    if (j.status == 'awaiting-repair-confirmation') return; // paused until the person answers
    poll = Timer(Duration(seconds: (j.status == 'queued' || j.status == 'queued-waiting') ? 4 : 3), _poll);
  }

  Future<void> _cancel() async {
    setState(() => cancelling = true);
    final r = await Api.i.cancel(widget.jobId);
    if (!mounted) return;
    if (r == null) {
      setState(() => cancelling = false); // already finished; the next poll shows the real end state
      return;
    }
    poll?.cancel();
    _poll();
  }

  Future<void> _repair(bool ok) async {
    setState(() => repairing = true);
    try {
      await Api.i.repair(widget.jobId, ok);
      poll?.cancel();
      _poll();
    } on ApiError catch (e) {
      if (mounted) showInfoDialog(context, title: 'Couldn\u2019t do that', message: e.message, icon: Icons.error_outline_rounded, color: C.red);
    } finally {
      if (mounted) setState(() => repairing = false);
    }
  }

  Widget _in(Animation<double> a, Widget child) => FadeTransition(
        opacity: a,
        child: SlideTransition(position: Tween(begin: const Offset(0, .25), end: Offset.zero).animate(a), child: child),
      );

  Widget _ring(int kind) {
    // 0 running, 1 done, 2 error, 3 cancelled
    final ring = _iv(0, .5, Curves.easeOutBack);
    final check = _iv(.25, .65, Curves.easeOutCubic);
    final color = kind == 2 ? C.red : (kind == 3 ? const Color(0xFF6A6A6E) : C.teal);
    return ScaleTransition(
      scale: Tween(begin: .55, end: 1.0).animate(ring),
      child: SizedBox(
        width: 126, height: 126,
        child: Stack(alignment: Alignment.center, children: [
          if (kind == 0) ...[
            Container(width: 126, height: 126, decoration: BoxDecoration(shape: BoxShape.circle, border: Border.all(color: const Color(0xFF24473E), width: 9))),
            RotationTransition(turns: spin, child: CustomPaint(size: const Size(126, 126), painter: _Arc())),
            Text(fmtClock(elapsed), style: inter(24, w: FontWeight.w700)),
          ] else ...[
            Container(width: 126, height: 126, decoration: BoxDecoration(color: color, shape: BoxShape.circle)),
            Container(width: 108, height: 108, decoration: const BoxDecoration(color: Color(0xFF0A1512), shape: BoxShape.circle)),
            Container(width: 95, height: 95, decoration: BoxDecoration(color: kind == 1 ? const Color(0xFF4CD1A6) : color, shape: BoxShape.circle)),
            if (kind == 1)
              AnimatedBuilder(animation: check, builder: (_, __) => CustomPaint(size: const Size(40, 40), painter: _Check(check.value, const Color(0xFF062019), 5)))
            else
              Icon(kind == 2 ? Icons.priority_high_rounded : Icons.close_rounded, size: 46, color: Colors.white),
          ],
        ]),
      ),
    );
  }

  Widget _outlined(String t, VoidCallback? f, {Color c = C.text}) => Pressable(
        onTap: f,
        child: Container(
          width: 204, height: 52, alignment: Alignment.center,
          decoration: BoxDecoration(color: const Color(0xFF2A2A2C), borderRadius: BorderRadius.circular(26)),
          child: Text(t, style: inter(18, w: FontWeight.w600, c: f == null ? C.muted : c)),
        ),
      );

  Widget _solid(String t, VoidCallback f) => Pressable(
        onTap: f,
        child: Container(
          width: 204, height: 52, alignment: Alignment.center,
          decoration: BoxDecoration(color: C.tealBtn, borderRadius: BorderRadius.circular(26)),
          child: Text(t, style: inter(20, w: FontWeight.w700, c: const Color(0xFF0F2A24))),
        ),
      );

  @override
  Widget build(BuildContext context) {
    final j = job;
    final kind = gone ? 2 : (j == null ? 0 : (j.isDone ? 1 : (j.isError ? 2 : (j.isCancelled ? 3 : 0))));
    final label = (j?.platform == 'windows') ? 'EXE' : 'APK';
    final title = '${widget.appName} $label';
    String sub1, sub2;
    if (gone) {
      sub1 = 'This build is no longer on the server';
      sub2 = 'Start a new build to get a fresh file.';
    } else if (j == null) {
      sub1 = 'Connecting…';
      sub2 = 'Getting the latest from the server.';
    } else if (j.isDone) {
      final sz = j.sizeBytes;
      sub1 = sz != null ? 'Ready to install, ${fmtSize(sz)}' : 'Ready to install';
      final d = j.durationMs != null ? Duration(milliseconds: j.durationMs!) : elapsed;
      sub2 = 'Finished in ${fmtClock(d)}.';
    } else if (j.isError) {
      sub1 = 'Build failed';
      sub2 = j.message.isEmpty ? 'Something went wrong.' : j.message;
    } else if (j.isCancelled) {
      sub1 = 'Build cancelled';
      sub2 = j.message.isEmpty ? 'Nothing was kept.' : j.message;
    } else {
      sub1 = switch (j.status) {
        'pushing' => 'Uploading your project',
        'queued' || 'queued-waiting' => 'Waiting for a free builder',
        'awaiting-repair-confirmation' => 'An automatic fix is ready',
        _ => 'Building your app',
      };
      sub2 = j.message;
    }
    final subColor = kind == 1 ? C.teal : (kind == 2 ? C.red : (kind == 3 ? C.muted : C.teal));

    final states = List<_S>.generate(4, (i) {
      if (kind == 1) return _S.done;
      if (j == null || stageIdx < 0) return i == 0 ? _S.active : _S.pending;
      if (i < stageIdx) return _S.done;
      if (i == stageIdx) return kind == 0 ? _S.active : _S.pending;
      return _S.pending;
    });
    final times = List<String?>.generate(3, (i) => stageTimes[i] != null ? fmtClock(stageTimes[i]!) : null);

    return Scaffold(
      body: SafeArea(
        child: Stack(children: [
          Column(children: [
            const TopBar(back: true),
            Expanded(
              child: ListView(
                physics: const BouncingScrollPhysics(parent: AlwaysScrollableScrollPhysics()),
                padding: const EdgeInsets.fromLTRB(18, 52, 18, 120),
                children: [
                  Center(child: AnimatedSwitcher(duration: const Duration(milliseconds: 450), switchInCurve: iosCurve, child: KeyedSubtree(key: ValueKey(kind), child: _ring(kind)))),
                  const SizedBox(height: 28),
                  _in(_iv(.4, .75), Column(children: [
                    Text(kind == 2 && !gone ? 'Build failed' : (kind == 3 ? 'Build cancelled' : title), textAlign: TextAlign.center, style: inter(24, w: FontWeight.w700)),
                    const SizedBox(height: 10),
                    AnimatedSwitcher(duration: const Duration(milliseconds: 300), child: Text(kind == 2 && !gone ? title : sub1, key: ValueKey('$kind$sub1'), textAlign: TextAlign.center, style: inter(16, w: FontWeight.w500, c: kind == 2 ? C.muted : subColor))),
                    const SizedBox(height: 6),
                    if (kind == 2 && !gone)
                      Container(
                        margin: const EdgeInsets.only(top: 8),
                        padding: const EdgeInsets.all(14),
                        width: double.infinity,
                        decoration: BoxDecoration(color: C.card, borderRadius: BorderRadius.circular(18), border: Border.all(color: const Color(0xFF3A2A2A))),
                        child: SelectableText(sub2, maxLines: 8, style: inter(14, c: const Color(0xFFD8B4B0), h: 1.4)),
                      )
                    else if (kind != 3 || sub2.isNotEmpty)
                      Text(sub2, textAlign: TextAlign.center, maxLines: 3, overflow: TextOverflow.ellipsis, style: inter(15, c: C.muted)),
                  ])),
                  const SizedBox(height: 28),
                  _in(_iv(.5, .85), Center(child: _action(j, kind))),
                  if (j?.status == 'awaiting-repair-confirmation') ...[
                    const SizedBox(height: 16),
                    Center(child: _outlined('Skip', repairing ? null : () => _repair(false))),
                  ],
                  const SizedBox(height: 26),
                  if (kind == 0 || kind == 1) _in(_iv(.6, 1), _Steps(states: states, times: times, spin: spin)),
                ],
              ),
            ),
          ]),
          const Positioned(right: 17, bottom: 103, child: BotFab()),
        ]),
      ),
    );
  }

  Widget _action(Job? j, int kind) {
    if (kind == 1 && j != null) {
      final aab = j.aabName;
      return Column(children: [
        _solid('Download', () => showIosDialog(context, (_) => DownloadDialog(job: j))),
        if (aab != null) ...[
          const SizedBox(height: 12),
          _outlined('Download AAB', () => showIosDialog(context, (_) => DownloadDialog(job: j, aab: aab))),
        ],
      ]);
    }
    if (kind == 2 || kind == 3) return _solid(gone ? 'Back' : (kind == 2 ? 'Try again' : 'Back'), () => Navigator.of(context).maybePop());
    if (j?.status == 'awaiting-repair-confirmation') return _solid('Apply fix', repairing ? () {} : () => _repair(true));
    return _outlined(cancelling ? 'Cancelling…' : 'Cancel build', (cancelling || j == null) ? null : _cancel);
  }
}

extension on Job {
  String? get aabName {
    final v = j['apkVariantAssets'];
    if (buildType == 'both' && v is Map) {
      for (final k in v.keys) {
        if (RegExp(r'\.aab$', caseSensitive: false).hasMatch('$k')) return '$k';
      }
    }
    return null;
  }
}

class _Arc extends CustomPainter {
  @override
  void paint(Canvas c, Size s) {
    final p = Paint()..color = C.teal..style = PaintingStyle.stroke..strokeWidth = 9..strokeCap = StrokeCap.round;
    c.drawArc(Rect.fromLTWH(4.5, 4.5, s.width - 9, s.height - 9), -math.pi / 2, math.pi * .55, false, p);
  }
  @override
  bool shouldRepaint(_) => false;
}

class _Check extends CustomPainter {
  final double t;
  final Color color;
  final double w;
  _Check(this.t, this.color, this.w);
  @override
  void paint(Canvas canvas, Size s) {
    final path = Path()
      ..moveTo(s.width * .08, s.height * .52)
      ..lineTo(s.width * .38, s.height * .8)
      ..lineTo(s.width * .94, s.height * .2);
    final p = Paint()..color = color..style = PaintingStyle.stroke..strokeWidth = w..strokeCap = StrokeCap.round..strokeJoin = StrokeJoin.round;
    for (final PathMetric m in path.computeMetrics()) {
      canvas.drawPath(m.extractPath(0, m.length * t), p);
    }
  }
  @override
  bool shouldRepaint(_Check o) => o.t != t;
}

class _Steps extends StatelessWidget {
  final List<_S> states;
  final List<String?> times;
  final Animation<double> spin;
  const _Steps({required this.states, required this.times, required this.spin});
  @override
  Widget build(BuildContext context) {
    final icons = [Icons.file_upload_outlined, Icons.fact_check_outlined, Icons.hardware_outlined, Icons.view_in_ar_outlined];
    final names = ['Upload', 'Prepare', 'Build', 'Ready'];
    Widget col(int i) {
      final st = states[i];
      final on = st != _S.pending;
      return SizedBox(
        width: 62,
        child: Column(children: [
          SizedBox(
            width: 50, height: 46,
            child: Stack(children: [
              Positioned(left: 3, top: 0, child: AnimatedContainer(
                duration: const Duration(milliseconds: 400), curve: iosCurve,
                width: 43, height: 43,
                decoration: BoxDecoration(color: on ? const Color(0xFF24473E) : const Color(0xFF2A2A2C), shape: BoxShape.circle),
                child: Icon(icons[i], size: 21, color: on ? C.teal : const Color(0xFF6E6E73)))),
              if (st == _S.done)
                Positioned(right: 0, bottom: 0, child: Container(
                  width: 18, height: 18,
                  decoration: BoxDecoration(color: C.teal, shape: BoxShape.circle, border: Border.all(color: C.card, width: 1.5)),
                  child: const Icon(Icons.check_rounded, size: 12, color: Color(0xFF0F2A24))))
              else if (st == _S.active)
                Positioned(right: 0, bottom: 0, child: Container(
                  width: 18, height: 18, padding: const EdgeInsets.all(3),
                  decoration: BoxDecoration(color: C.card, shape: BoxShape.circle, border: Border.all(color: C.card, width: 1.5)),
                  child: const CircularProgressIndicator(strokeWidth: 2, color: C.teal))),
            ]),
          ),
          const SizedBox(height: 7),
          Text(names[i], style: inter(17, w: FontWeight.w500, c: on ? C.text : C.muted)),
          const SizedBox(height: 7),
          Text(i < 3 ? (times[i] ?? '') : '', style: inter(15, c: C.muted)),
        ]),
      );
    }
    Widget line(int i) => Expanded(
        child: Padding(
          padding: const EdgeInsets.only(top: 21),
          child: AnimatedContainer(duration: const Duration(milliseconds: 500), height: 2, color: states[i] == _S.done ? C.teal : const Color(0xFF333335)),
        ));
    return Container(
      padding: const EdgeInsets.fromLTRB(6, 18, 6, 16),
      decoration: BoxDecoration(color: C.card, borderRadius: BorderRadius.circular(26)),
      child: Row(crossAxisAlignment: CrossAxisAlignment.start, children: [col(0), line(0), col(1), line(1), col(2), line(2), col(3)]),
    );
  }
}

/// Real download with real byte progress (the bar is indeterminate when the server sends no length).
class DownloadDialog extends StatefulWidget {
  final Job job;
  final String? aab;
  const DownloadDialog({super.key, required this.job, this.aab});
  @override
  State<DownloadDialog> createState() => _DownloadDialogState();
}

class _DownloadDialogState extends State<DownloadDialog> {
  int done = 0;
  int? total;
  String? path, error;
  bool cancelled = false;

  @override
  void initState() {
    super.initState();
    _run();
  }

  Future<void> _run() async {
    try {
      final p = await Api.i.download(widget.job, (d, t) {
        if (mounted) setState(() { done = d; total = t; });
      }, aab: widget.aab, cancelled: () => cancelled);
      if (mounted) setState(() => path = p);
    } catch (e) {
      if (mounted) setState(() => error = cancelled ? 'Cancelled' : (e is ApiError ? e.message : 'The download didn\u2019t finish. Check your connection and try again.'));
    }
  }

  String _mb(int b) => '${(b / 1048576).toStringAsFixed(1)}MB';

  @override
  Widget build(BuildContext context) {
    final complete = path != null;
    final failed = error != null;
    final shown = path?.replaceFirst(RegExp(r'^/storage/emulated/0/'), '');
    return Material(
      color: Colors.transparent,
      child: Container(
        width: MediaQuery.of(context).size.width - 64,
        padding: const EdgeInsets.all(22),
        decoration: BoxDecoration(color: const Color(0xFF1E1E20), borderRadius: BorderRadius.circular(28)),
        child: Column(mainAxisSize: MainAxisSize.min, crossAxisAlignment: CrossAxisAlignment.start, children: [
          AnimatedContainer(
            duration: const Duration(milliseconds: 300),
            width: 56, height: 56,
            decoration: BoxDecoration(color: failed ? const Color(0x33F0483E) : const Color(0xFF26403A), shape: BoxShape.circle),
            child: complete
                ? const Icon(Icons.check_rounded, size: 26, color: C.teal)
                : failed
                    ? const Icon(Icons.priority_high_rounded, size: 26, color: C.red)
                    : const Padding(padding: EdgeInsets.all(16), child: IosSpinner(size: 26)),
          ),
          const SizedBox(height: 16),
          Text(complete ? 'Download complete' : (failed ? 'Download failed' : 'Downloading…'), style: inter(20, w: FontWeight.w700)),
          const SizedBox(height: 8),
          if (failed)
            Text(error!, style: inter(16, c: C.muted, h: 1.35))
          else ...[
            Text.rich(TextSpan(children: [
              TextSpan(text: _mb(done), style: inter(18, w: FontWeight.w600, c: C.teal)),
              if (total != null) TextSpan(text: ' / ${_mb(total!)}', style: inter(18, c: C.muted)),
            ])),
            const SizedBox(height: 6),
            Text(complete ? 'Saved to $shown' : 'Keep this open until it finishes', style: inter(15, c: C.muted, h: 1.35)),
            const SizedBox(height: 18),
            ClipRRect(
              borderRadius: BorderRadius.circular(3),
              child: LinearProgressIndicator(
                value: complete ? 1 : (total != null ? done / total! : null),
                minHeight: 6, backgroundColor: const Color(0xFF2E2E30), valueColor: const AlwaysStoppedAnimation(C.tealBtn)),
            ),
          ],
          const SizedBox(height: 20),
          Pressable(
            onTap: () {
              if (!complete && !failed) cancelled = true;
              Navigator.pop(context);
            },
            child: Container(
              height: 48, alignment: Alignment.center,
              decoration: BoxDecoration(color: complete || failed ? C.tealBtn : const Color(0xFF2A2A2C), borderRadius: BorderRadius.circular(24)),
              child: Text(complete || failed ? 'Done' : 'Cancel', style: inter(17, w: FontWeight.w700, c: complete || failed ? const Color(0xFF0F2A24) : C.text)),
            ),
          ),
        ]),
      ),
    );
  }
}
