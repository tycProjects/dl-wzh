import 'dart:async';
import 'dart:math' as math;
import 'dart:io';
import 'package:flutter/material.dart' hide Text;
import 'package:flutter/scheduler.dart' show Ticker;
import 'package:path_provider/path_provider.dart';
import '../api.dart';
import '../store.dart';
import '../theme.dart';
import '../widgets.dart';
import '../anim.dart';
import '../appearance.dart';
import '../save_install.dart';
import '../banner.dart';
import '../tr.dart';
import '../notify.dart';
import 'code_editor.dart';


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
  /// The app icon picked in Options, shown on the progress screen. Optional (a letter tile is used without it).
  final String? logoPath;
  const BuildPage({super.key, required this.jobId, required this.appName, this.logoPath});
  @override
  State<BuildPage> createState() => _BuildPageState();
}

class _BuildPageState extends State<BuildPage> with TickerProviderStateMixin {
  late final AnimationController intro = AnimationController(vsync: this, duration: const Duration(milliseconds: 1400))..forward();
  Job? job;
  Timer? poll, tick;
  DateTime started = DateTime.now();
  Duration elapsed = Duration.zero;
  int lastLogs = 0, stageIdx = -1, failures = 0;
  final stageTimes = <int, Duration>{};
  Duration stageStart = Duration.zero;
  bool cancelling = false, gone = false, repairing = false, finished = false;
  String? _logo;

  // Smooth progress: one value that only ever moves forward, a little every frame.
  // Each stage owns a quarter of the bar; inside it the bar crawls toward (never past) the
  // quarter's end, then glides on when the real stage changes. No jumps, no sweeping chunk.
  final ValueNotifier<double> _shown = ValueNotifier<double>(0);
  Ticker? _crawl;
  Duration _lastFrame = Duration.zero;
  DateTime _stageWall = DateTime.now();
  static const _tau = [8.0, 30.0, 120.0]; // seconds a stage takes to reach ~63% of its quarter

  double _target(int kind) {
    if (kind == 1) return 1.0;
    final s = stageIdx < 0 ? 0 : (stageIdx > 2 ? 2 : stageIdx);
    final t = DateTime.now().difference(_stageWall).inMilliseconds / 1000.0;
    final frac = 1 - math.exp(-t / _tau[s]);
    return s / 4 + .25 * .94 * frac;
  }

  void _onFrame(Duration now) {
    final dt = ((now - _lastFrame).inMicroseconds / 1e6).clamp(0.0, .1).toDouble();
    _lastFrame = now;
    if (finished && job?.isDone != true) return; // failed or cancelled: freeze where it stopped
    final done = job?.isDone == true;
    final gap = _target(done ? 1 : 0) - _shown.value;
    if (gap <= 0) return;
    // ease toward the target, with a speed limit so even a big change is a slow glide
    final maxSpeed = done ? .35 : .08; // fraction of the bar per second
    final step = math.min(gap * (1 - math.exp(-dt * 1.5)), maxSpeed * dt);
    _shown.value = (_shown.value + step).clamp(0.0, 1.0).toDouble();
  }

  Animation<double> _iv(double a, double b, [Curve k = iosCurve]) => CurvedAnimation(parent: intro, curve: Interval(a, b, curve: k));

  @override
  void initState() {
    super.initState();
    _logo = widget.logoPath;
    _loadLogo();
    _crawl = createTicker(_onFrame)..start();
    tick = Timer.periodic(const Duration(seconds: 1), (_) {
      if (!mounted || finished) return;
      setState(() => elapsed = DateTime.now().difference(started));
    });
    _poll();
  }

  /// Keeps the icon the person picked (copied into the app's own folder) so reopening this build from
  /// Recent builds still shows it.
  Future<void> _loadLogo() async {
    try {
      final prefs = Api.i.prefs;
      final key = 'logo_${widget.jobId}';
      var path = widget.logoPath;
      if (path != null && File(path).existsSync()) {
        final dir = Directory('${(await getApplicationDocumentsDirectory()).path}/build_icons');
        await dir.create(recursive: true);
        final dot = path.lastIndexOf('.');
        final ext = dot > path.lastIndexOf('/') ? path.substring(dot) : '.png';
        final dest = File('${dir.path}/${widget.jobId}$ext');
        await File(path).copy(dest.path);
        await prefs.setString(key, dest.path);
        path = dest.path;
      } else {
        path = prefs.getString(key);
      }
      if (path != null && File(path).existsSync() && mounted) {
        await FileImage(File(path)).evict();
        setState(() => _logo = path);
      }
    } catch (_) {}
  }

  @override
  void dispose() {
    poll?.cancel();
    tick?.cancel();
    intro.dispose();
    _crawl?.dispose();
    _shown.dispose();
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
        if (mounted) IosBanner.show(context, kind: BannerKind.err, title: 'Session expired', msg: e.message, duration: const Duration(seconds: 6));
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
      final first = stageIdx < 0;
      stageIdx = idx;
      stageStart = elapsed;
      _stageWall = DateTime.now();
      // A build reopened mid-way starts at its stage instead of crawling up from zero.
      if (first && idx > 0 && _shown.value < idx / 4) _shown.value = idx / 4;
    }
    final end = j.isDone || j.isError || j.isCancelled;
    setState(() {
      job = j;
      if (end) finished = true;
    });
    if (end) {
      BuildNotifier.i.finished(j, widget.appName);
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
      if (mounted) IosBanner.show(context, kind: BannerKind.err, title: 'Couldn\u2019t do that', msg: e.message, duration: const Duration(seconds: 6));
    } finally {
      if (mounted) setState(() => repairing = false);
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

  @override
  Widget build(BuildContext context) => PopScope(
        // A build that is still running asks first; a finished / failed / gone one just closes.
        canPop: finished || gone,
        onPopInvokedWithResult: (didPop, _) async {
          if (didPop) return;
          final leave = await confirmLeave(context,
              title: 'Leave build progress?',
              message: 'Your build keeps running in the background. Find it in Recent builds and tap it to come back.',
              stay: 'Stay here');
          if (leave && context.mounted) Navigator.of(context).pop();
        },
        child: _page(context),
      );

  String _blobOf(Job j) => '${j.message}\n${j.logs.map((e) => e.values.join(' ')).join('\n')}';

  dynamic _locOf(Job? j, int kind) => (kind == 2 && j != null) ? errorLocation(_blobOf(j)) : null;

  Widget _appIcon() {
    final name = widget.appName.trim();
    final letter = name.isEmpty ? 'A' : name.characters.first.toUpperCase();
    final fallback = Container(
      color: const Color(0xFF1F3B32),
      alignment: Alignment.center,
      child: Text(letter, verbatim: true, style: inter(34, w: FontWeight.w700, c: C.teal, h: 1)),
    );
    final lp = _logo;
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

  /// Thin bar like an app store install. One smooth value that crawls forward and never jumps back.
  Widget _bar(int kind) {
    return ClipRRect(
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
  }

  Widget _stat(String value, String label) => Expanded(
        child: Column(children: [
          Text(value, verbatim: true, maxLines: 1, overflow: TextOverflow.ellipsis, style: inter(17, w: FontWeight.w600)),
          const SizedBox(height: 3),
          Text(label, style: inter(13, c: C.muted)),
        ]),
      );

  Widget _statsRow(Job? j, int kind) {
    final d = (kind == 1 && j?.durationMs != null) ? Duration(milliseconds: j!.durationMs!) : elapsed;
    final type = j?.typeLabel ?? 'APK';
    final cells = kind == 1
        ? [if (j?.sizeBytes != null) _stat(fmtSize(j!.sizeBytes!), 'Size'), _stat(type, 'Type'), _stat(fmtClock(d), 'Time')]
        : [_stat(type, 'Type'), _stat('${(stageIdx < 0 ? 0 : stageIdx) + 1}/4', 'Stage'), _stat(fmtClock(d), 'Time')];
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
  Widget _steps(Job? j, int kind, List<String?> times) {
    const names = ['Upload', 'Prepare', 'Build', 'Ready'];
    const hints = ['Uploading your project', 'Waiting for a free builder', 'Building your app', 'Ready to install'];
    Widget row(int i) {
      final bool d = kind == 1 || (stageIdx >= 0 && i < stageIdx);
      final bool a = kind == 0 && (stageIdx < 0 ? i == 0 : i == stageIdx);
      final bool f = (kind == 2 || kind == 3) && i == stageIdx;
      final dot = d ? C.teal : (f ? (kind == 2 ? C.red : const Color(0xFF6A6A6E)) : (a ? const Color(0xFF24473E) : const Color(0xFF2A2A2C)));
      final sub = a ? ((j?.message ?? '').isNotEmpty ? j!.message : hints[i]) : null;
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
                        ? Icon(kind == 2 ? Icons.close_rounded : Icons.remove_rounded, size: 16, color: Colors.white)
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
                Text(names[i], style: inter(17, c: (d || a || f) ? C.text : C.muted)),
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

  Widget _xButton() => Pressable(
        onTap: (cancelling || job == null) ? null : _cancel,
        scale: .88,
        child: Container(
          width: 36, height: 36,
          decoration: const BoxDecoration(color: Color(0xFF2A2A2C), shape: BoxShape.circle),
          child: cancelling
              ? const Padding(padding: EdgeInsets.all(10), child: CircularProgressIndicator(strokeWidth: 2, color: C.muted))
              : const Icon(Icons.close_rounded, size: 20, color: C.text),
        ),
      );

  /// What sits where the Install button is on an app store page.
  Widget _status(Job? j, int kind, String sub1, String sub2) {
    if (kind == 1 && j != null) {
      final aab = j.aabName;
      return Row(children: [
        Expanded(child: _solid('Download', () => showIosDialog(context, (_) => DownloadDialog(job: j)))),
        if (aab != null) ...[
          const SizedBox(width: 10),
          Expanded(child: _outlined('Download AAB', () => showIosDialog(context, (_) => DownloadDialog(job: j, aab: aab), ), c: C.teal)),
        ],
      ]);
    }
    if (kind == 2 || kind == 3) {
      final loc = (kind == 2 && !gone) ? _locOf(j, kind) : null;
      final blob = (kind == 2 && !gone && j != null) ? _blobOf(j) : '';
      final plain = blob.isEmpty ? null : friendlyError(blob);
      final canFix = kind == 2 && !gone && CodeProject.i.isOpen;
      return Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
        if (plain != null) ...[
          Container(
            padding: const EdgeInsets.all(14),
            decoration: BoxDecoration(color: C.card, borderRadius: BorderRadius.circular(14)),
            child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              Text('What went wrong', style: inter(13, w: FontWeight.w700, c: C.teal)),
              const SizedBox(height: 4),
              Text(plain, style: inter(15, h: 1.4)),
            ]),
          ),
          const SizedBox(height: 12),
        ],
        if (canFix) ...[
          _solid('Fix it', () {
            if (loc != null) {
              openCodeFile(context, loc.$1, loc.$2, hint: errorHint(blob));
            } else {
              Navigator.of(context).push(codeEditorRoute());
            }
          }),
          const SizedBox(height: 10),
          _outlined('Try again', () => Navigator.of(context).maybePop()),
        ] else
          _solid(kind == 2 && !gone ? 'Try again' : 'Back', () => Navigator.of(context).maybePop()),
        if (kind == 2 && !gone) ...[
          const SizedBox(height: 14),
          Container(
            padding: const EdgeInsets.all(14),
            decoration: BoxDecoration(color: C.card, borderRadius: BorderRadius.circular(14), border: Border.all(color: const Color(0xFF3A2A2A))),
            child: SelectableText(sub2, maxLines: 10, style: inter(14, c: const Color(0xFFD8B4B0), h: 1.4)),
          ),
        ] else if (sub2.isNotEmpty) ...[
          const SizedBox(height: 12),
          Text(sub2, style: inter(15, c: C.muted)),
        ],
      ]);
    }
    if (j?.status == 'awaiting-repair-confirmation') {
      return Row(children: [
        Expanded(child: _solid('Apply fix', repairing ? () {} : () => _repair(true))),
        const SizedBox(width: 10),
        Expanded(child: _outlined('Skip', repairing ? null : () => _repair(false))),
      ]);
    }
    final mm = fmtClock(elapsed);
    final step = (stageIdx < 0 ? 0 : stageIdx) + 1;
    return Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
      Row(children: [
        Expanded(child: Text(sub1, maxLines: 1, overflow: TextOverflow.ellipsis, style: inter(14, c: C.muted))),
        Text('Step $step of 4 \u00B7 $mm', verbatim: true, style: inter(14, c: C.muted)),
      ]),
      const SizedBox(height: 10),
      Row(children: [Expanded(child: _bar(kind)), const SizedBox(width: 14), _xButton()]),
    ]);
  }

  Widget _page(BuildContext context) {
    final j = job;
    final kind = gone ? 2 : (j == null ? 0 : (j.isDone ? 1 : (j.isError ? 2 : (j.isCancelled ? 3 : 0))));
    final label = (j?.platform == 'windows') ? 'EXE' : 'APK';
    String sub1, sub2;
    if (gone) {
      sub1 = 'This build is no longer on the server';
      sub2 = 'Start a new build to get a fresh file.';
    } else if (j == null) {
      sub1 = 'Connecting…';
      sub2 = 'Getting the latest from the server.';
    } else if (j.isDone) {
      sub1 = 'Ready to install';
      sub2 = '';
    } else if (j.isError) {
      sub1 = 'Build failed';
      sub2 = j.message.isEmpty ? 'Something went wrong. Try again.' : j.message;
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
                          child: Text(kind == 0 ? label : sub1, key: ValueKey('$kind$label'), maxLines: 1, overflow: TextOverflow.ellipsis, style: inter(13.5, c: subColor)),
                        ),
                      ]),
                    ),
                  ])),
                  const SizedBox(height: 22),
                  _in(_iv(.15, .6), AnimatedSize(
                    duration: const Duration(milliseconds: 280), curve: iosCurve, alignment: Alignment.topCenter,
                    child: AnimatedSwitcher(
                      duration: const Duration(milliseconds: 260),
                      child: KeyedSubtree(key: ValueKey('$kind${j?.status == 'awaiting-repair-confirmation'}'), child: _status(j, kind, sub1, sub2)),
                    ),
                  )),
                  if (kind == 0 || kind == 1) ...[
                    const SizedBox(height: 22),
                    _in(_iv(.3, .75), _statsRow(j, kind)),
                  ],
                  if (!gone && kind != 3) ...[
                    const SizedBox(height: 26),
                    _in(_iv(.4, .9), Text('Build steps', style: inter(19, w: FontWeight.w600))),
                    const SizedBox(height: 16),
                    _in(_iv(.5, 1), _steps(j, kind, times)),
                  ],
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
  String? path, error, savedAt;
  bool cancelled = false;

  bool get _isApk => widget.aab == null && (path?.toLowerCase().endsWith('.apk') ?? false);

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
      // The copy in Downloads keeps exactly the name the file was saved with (the server's file name).
      final fileName = p.split('/').last;
      final pub = await FileSaver.saveToDownloads(p, widget.job.name, fileName: fileName);
      if (mounted) setState(() { path = p; savedAt = pub; });
      // Open the installer by itself (Android asks to allow "install unknown apps" if needed).
      if (mounted && !cancelled && _isApk) {
        await Future.delayed(const Duration(milliseconds: 450));
        if (mounted) FileSaver.install(p);
      }
    } catch (e) {
      if (mounted) setState(() => error = cancelled ? 'Cancelled' : (e is ApiError ? e.message : 'Download didn\u2019t finish. Check your connection and try again.'));
    }
  }

  String _mb(int b) => '${(b / 1048576).toStringAsFixed(1)}MB';

  @override
  Widget build(BuildContext context) {
    final complete = path != null;
    final failed = error != null;
    final shown = (savedAt ?? path ?? '').split('/').last;
    return Material(
      color: Colors.transparent,
      child: Container(
        width: MediaQuery.of(context).size.width - 64,
        padding: const EdgeInsets.all(22),
        decoration: BoxDecoration(color: const Color(0xFF1E1E20), borderRadius: BorderRadius.circular(28)),
        child: Column(mainAxisSize: MainAxisSize.min, crossAxisAlignment: CrossAxisAlignment.start, children: [
          AnimatedContainer(
            duration: const Duration(milliseconds: 240),
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
            Text(complete ? (savedAt != null ? 'Saved to Downloads \u00B7 $shown' : 'Couldn\u2019t save to Downloads, but your file is ready: $shown') : 'Keep this open until it finishes', style: inter(15, c: C.muted, h: 1.35)),
            const SizedBox(height: 18),
            ClipRRect(
              borderRadius: BorderRadius.circular(3),
              child: LinearProgressIndicator(
                value: complete ? 1 : (total != null ? done / total! : null),
                minHeight: 6, backgroundColor: const Color(0xFF2E2E30), valueColor: const AlwaysStoppedAnimation(C.tealBtn)),
            ),
          ],
          const SizedBox(height: 20),
          if (complete && _isApk) ...[
            Pressable(
              onTap: () => FileSaver.install(path!),
              child: Container(
                height: 48, alignment: Alignment.center,
                decoration: BoxDecoration(color: C.tealBtn, borderRadius: BorderRadius.circular(24)),
                child: Text('Install', style: inter(17, w: FontWeight.w700, c: const Color(0xFF0F2A24))),
              ),
            ),
            const SizedBox(height: 10),
          ],
          Pressable(
            onTap: () {
              if (!complete && !failed) cancelled = true;
              Navigator.pop(context);
            },
            child: Container(
              height: 48, alignment: Alignment.center,
              decoration: BoxDecoration(color: (complete && !_isApk) || failed ? C.tealBtn : const Color(0xFF2A2A2C), borderRadius: BorderRadius.circular(24)),
              child: Text(complete || failed ? 'Done' : 'Cancel', style: inter(17, w: FontWeight.w700, c: (complete && !_isApk) || failed ? const Color(0xFF0F2A24) : C.text)),
            ),
          ),
        ]),
      ),
    );
  }
}
