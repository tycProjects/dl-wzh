import 'dart:async';
import 'dart:convert' show utf8;
import 'dart:math' as math;
import 'dart:io';
import 'package:flutter/material.dart' hide Text;
import 'package:flutter/scheduler.dart' show Ticker;
import 'package:flutter/services.dart' show Clipboard, ClipboardData;
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

/// One compiler error that points at a place in the person's project.
class _Err {
  final String file;
  final int line;
  final String msg;
  final String? ident;
  final int? col;
  const _Err(this.file, this.line, this.msg, this.ident, [this.col]);
}

/// Why a build stopped, worked out once from the server's message and log.
class _Fail {
  /// 0 = general (shown on the steps list), 1 = a code error, 2 = a library that couldn't be found.
  final int type;
  final String body, tip;
  final String? file;     // project file to open (code error or build file)
  final int line;
  final int? col;
  final String? ident;    // the misspelled / undefined name, underlined in the code preview
  final String? snippet;  // one line of the compiler's own text (library name or first error)
  final List<_Err> errors; // every error that points into the project, first one first
  const _Fail(this.type, {this.body = '', this.tip = '', this.file, this.line = 1, this.col, this.ident, this.snippet, this.errors = const []});
}

class _BuildPageState extends State<BuildPage> with TickerProviderStateMixin {
  late final AnimationController intro = AnimationController(vsync: this, duration: const Duration(milliseconds: 1400))..forward();
  Job? job;
  // The server only sends lines newer than the last poll, so every line is kept here.
  // That is what lets the copied details hold the real compiler error, not just the last status line.
  final List<Map<String, dynamic>> _allLogs = [];
  Timer? poll, tick;
  DateTime started = DateTime.now();
  Duration elapsed = Duration.zero;
  int lastLogs = 0, stageIdx = -1, failures = 0;
  /// The step the build stopped at (failed or cancelled).
  int failedIdx = 0;
  final stageTimes = <int, Duration>{};
  Duration stageStart = Duration.zero;
  bool cancelling = false, gone = false, repairing = false, finished = false;
  bool _sawBuild = false, _sawPublish = false;
  /// True once this screen has seen the build still running, so a build that finishes while the person
  /// is watching downloads by itself, but opening an old finished build from history does not.
  bool _wasLive = false;
  bool _autoOpened = false;
  int _errIdx = 0; // which of several code errors is shown
  _Fail? _fail;
  String? _logo;

  static const _names = ['Upload', 'Queue', 'Prepare', 'Build', 'Publish', 'Ready'];
  static const _live = ['Uploading', 'Queued', 'Preparing', 'Compiling', 'Publishing', 'Ready'];
  static const _rule = Color(0xFF1F1F22);
  static const _track = Color(0xFF1F1F22);

  // Smooth progress: one value that only ever moves forward, a little every frame.
  // Each of the 6 steps owns a sixth of the bar; inside it the bar crawls toward (never past) the
  // sixth's end, then glides on when the real step changes. No jumps, no sweeping chunk.
  final ValueNotifier<double> _shown = ValueNotifier<double>(0);
  Ticker? _crawl;
  Duration _lastFrame = Duration.zero;
  DateTime _stageWall = DateTime.now();
  // Where each step's slice of the bar starts (Upload, Queue, Prepare, Compile, Publish). Compile is
  // the long one, so it gets most of the bar. A step's slice ends where the next one starts.
  static const _starts = [0.0, .05, .09, .20, .90, 1.0];
  static const _tau = [10.0, 40.0, 30.0, 150.0, 20.0]; // seconds a step takes to reach ~63% of its slice

  // What the server's log says about where the build is. The server only reports "building", so
  // these decide when Prepare turns into Build and Build into Publish.
  static final _buildMark = RegExp(r'> Task :|:app:|compile(Release|Debug)|Compiling|Running Gradle task|flutter build|kotlinc|\be: ', caseSensitive: false);
  static final _publishMark = RegExp(r'zipalign|apksigner|Signing|Signed|Publishing|Uploading (the )?(apk|aab|artifact)|BUILD SUCCESSFUL|Built build', caseSensitive: false);

  double _target(int kind) {
    if (kind == 1) return 1.0;
    final s = stageIdx < 0 ? 0 : (stageIdx > 4 ? 4 : stageIdx);
    final t = DateTime.now().difference(_stageWall).inMilliseconds / 1000.0;
    final frac = 1 - math.exp(-t / _tau[s]);
    return _starts[s] + (_starts[s + 1] - _starts[s]) * .94 * frac;
  }

  void _onFrame(Duration now) {
    final dt = ((now - _lastFrame).inMicroseconds / 1e6).clamp(0.0, .1).toDouble();
    _lastFrame = now;
    if (finished && job?.isDone != true) return; // failed or cancelled: freeze where it stopped
    final done = job?.isDone == true;
    final gap = _target(done ? 1 : 0) - _shown.value;
    if (gap <= 0) return;
    // ease toward the target, with a speed limit so even a big change is a slow glide
    final maxSpeed = done ? .30 : .012; // fraction of the bar per second: a slow crawl, never a rush
    final step = math.min(gap * (1 - math.exp(-dt * .8)), maxSpeed * dt);
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
      setState(() {
        elapsed = DateTime.now().difference(started);
        // No sign of compiling in the log yet, but the build has been running for a while: it is building.
        final j = job;
        if (j != null && j.isActive && stageIdx == 2 && j.status != 'awaiting-repair-confirmation' &&
            DateTime.now().difference(_stageWall).inSeconds >= 25) {
          _setStage(3);
        }
      });
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

  String _lineOf(Map<String, dynamic> e) => '${e['message'] ?? e['text'] ?? e['line'] ?? e.values.join(' ')}';

  /// Looks at the newest log lines for signs of compiling and publishing.
  void _scan(Job j) {
    for (final t in [j.message, ...j.logs.map(_lineOf)]) {
      if (_publishMark.hasMatch(t)) _sawPublish = true;
      if (_buildMark.hasMatch(t)) _sawBuild = true;
    }
  }

  /// 0 Upload, 1 Queue, 2 Prepare, 3 Build, 4 Publish, 5 Ready.
  int _stage(Job j) {
    switch (j.status) {
      case 'pushing':
        return 0;
      case 'queued':
      case 'queued-waiting':
      case 'queued_waiting':
        return 1;
      case 'done':
        return 5;
      case 'awaiting-repair-confirmation':
        return 3; // a compile error was found; the fix is waiting for an answer
      default:
        return _sawPublish ? 4 : (_sawBuild ? 3 : 2);
    }
  }

  /// Moves to a step (never backwards) and keeps how long the one before it took.
  void _setStage(int idx) {
    if (idx == stageIdx || (stageIdx >= 0 && idx < stageIdx)) return;
    if (stageIdx >= 0) stageTimes[stageIdx] = elapsed - stageStart;
    stageIdx = idx;
    stageStart = elapsed;
    _stageWall = DateTime.now();
  }

  /// How long the build ran: the server's own number, else its finish time, else now (for one watched live).
  Duration _endElapsed(Job j) {
    Duration d = DateTime.now().difference(started);
    if (j.durationMs != null) {
      d = Duration(milliseconds: j.durationMs!);
    } else {
      for (final k in const ['finishedAt', 'completedAt', 'endedAt', 'updatedAt']) {
        final v = j.j[k];
        final t = v == null ? null : DateTime.tryParse('$v')?.toLocal();
        if (t != null) {
          d = t.difference(started);
          break;
        }
      }
    }
    return d.isNegative ? Duration.zero : d;
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
    if (lastLogs == 0) _allLogs.clear();
    _allLogs.addAll(j.logs);
    lastLogs = j.logsTotal > lastLogs ? j.logsTotal : lastLogs;
    // The server's own start time keeps the clock right when reopening a running build.
    if (job == null && j.createdAt != null) {
      started = j.createdAt!;
      elapsed = DateTime.now().difference(started);
    }
    _scan(j);
    if (!j.isDone && !j.isError && !j.isCancelled) _wasLive = true;
    final end = j.isDone || j.isError || j.isCancelled;
    if (end) elapsed = _endElapsed(j);
    if (j.isDone) {
      _setStage(5);
    } else if (j.isError || j.isCancelled) {
      if (stageIdx < 0) _setStage(math.min(_stage(j), 4));
      failedIdx = stageIdx;
      stageTimes[stageIdx] = elapsed - stageStart;
    } else {
      _setStage(_stage(j));
    }
    if (j.isError) {
      _fail = _diagnose(j);
      // A code error means it got as far as compiling; a missing library means it never got past preparing.
      if (_fail!.type == 1 && failedIdx < 3) failedIdx = 3;
      if (_fail!.type == 2 && failedIdx > 2) failedIdx = 2;
      final before = [for (var i = 0; i < failedIdx; i++) stageTimes[i] ?? Duration.zero].fold(Duration.zero, (a, b) => a + b);
      stageTimes[failedIdx] ??= (elapsed - before).isNegative ? Duration.zero : elapsed - before;
      _shown.value = math.min(_shown.value, _starts[failedIdx.clamp(0, 4)] + .02); // the bar stops where the build stopped
    }
    setState(() {
      job = j;
      if (end) finished = true;
    });
    // A build that fails while the person is watching jumps straight to the broken code (or the build
    // file, for a missing library). Opening an old failed build from history does not.
    if (j.isError && _wasLive && !_autoOpened) {
      final f = _fail;
      if (f != null && f.type != 0 && f.file != null && CodeProject.i.isOpen && CodeProject.i.files.containsKey(f.file)) {
        _autoOpened = true;
        Future.delayed(const Duration(milliseconds: 700), () {
          if (!mounted || (ModalRoute.of(context)?.isCurrent ?? false) == false) return;
          openCodeFile(context, f.file!, f.line, hint: f.type == 1 ? f.body : 'Check the version of this library.');
        });
      }
    }
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

  Future<void> _askCancel() async {
    if (cancelling || job == null) return;
    final yes = await confirmLeave(context,
        title: 'Cancel this build?',
        message: 'It stops right away and nothing is kept.',
        stay: 'Keep building',
        leave: 'Cancel build', destructive: true);
    if (yes && mounted) _cancel();
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

  TextStyle _mono(double size, Color c, {FontWeight w = FontWeight.w400, double h = 1.3}) =>
      TextStyle(fontFamily: 'monospace', fontSize: size, color: c, fontWeight: w, height: h, decoration: TextDecoration.none);

  Widget _solid(String t, VoidCallback f) => Pressable(
        onTap: f,
        child: Container(
          width: double.infinity, height: 56, alignment: Alignment.center,
          decoration: BoxDecoration(color: C.tealBtn, borderRadius: BorderRadius.circular(28)),
          child: Text(t, style: inter(20, w: FontWeight.w700, c: const Color(0xFF0F2A22))),
        ),
      );

  Widget _quiet(String t, VoidCallback f) => Pressable(
        onTap: f,
        child: Container(
          width: double.infinity, height: 52, alignment: Alignment.center,
          child: Text(t, style: inter(19, w: FontWeight.w500, c: C.muted)),
        ),
      );

  Widget _outlined(String t, VoidCallback? f) => Pressable(
        onTap: f,
        child: Container(
          width: double.infinity, height: 56, alignment: Alignment.center,
          decoration: BoxDecoration(color: const Color(0xFF2A2A2C), borderRadius: BorderRadius.circular(28)),
          child: Text(t, style: inter(19, w: FontWeight.w600, c: f == null ? C.muted : C.text)),
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

  String _blobOf(Job j) => '${j.message}\n${_allLines(j).join('\n')}';

  /// Every received log line, split into single lines.
  List<String> _allLines(Job j) {
    final src = _allLogs.isNotEmpty ? _allLogs : j.logs;
    final out = <String>[];
    for (final e in src) {
      for (final l in _lineOf(e).split('\n')) {
        out.add(l.trimRight());
      }
    }
    return out;
  }

  /// Decides which of the three failure layouts fits, and what to say in it.
  _Fail _diagnose(Job j) {
    final blob = _blobOf(j);
    final plain = friendlyError(blob);
    final loc = errorLocation(blob);
    final hint = errorHint(blob);
    final stage = failedIdx;

    // Every error that points into the project (up to 8), so each can be looked at in turn.
    final errs = [for (final e in parseBuildErrors(_allLines(j).toList())) _Err(e.file, e.line, e.msg, e.ident, e.col)];

    // 1. Something in the person's code.
    if (plain != null || loc != null || RegExp(r'\be: ').hasMatch(blob) || (stage >= 3 && hint != null)) {
      final first = errs.isEmpty ? null : errs.first;
      final ident = first?.ident ?? errorIdent(blob);
      return _Fail(
        1,
        body: ident != null ? '\u201C$ident\u201D isn\u2019t defined. Check the spelling.' : (plain ?? 'Something in your code stopped the build.'),
        file: first?.file ?? loc?.$1,
        line: first?.line ?? loc?.$2 ?? 1,
        col: first?.col,
        ident: ident,
        snippet: loc == null ? hint : null,
        errors: errs,
      );
    }

    // 2. A library the build file asks for can't be found.
    final dep = RegExp(r'Could not (resolve|find) ([\w.\-]+:[\w.\-]+(?::[\w.\-+]+)?)').firstMatch(blob);
    if (dep != null) {
      String? file;
      var line = 1;
      if (CodeProject.i.isOpen) {
        for (final c in const ['app/build.gradle', 'app/build.gradle.kts', 'build.gradle', 'build.gradle.kts', 'pubspec.yaml']) {
          final f = CodeProject.i.find(c);
          if (f != null) {
            file = f;
            break;
          }
        }
        if (file != null) {
          final parts = dep.group(2)!.split(':');
          final text = utf8.decode(CodeProject.i.files[file] ?? const <int>[], allowMalformed: true).split('\n');
          final at = text.indexWhere((l) => l.contains(parts.length > 1 ? parts[1] : parts[0]));
          if (at >= 0) line = at + 1;
        }
      }
      return _Fail(2, file: file, line: line, snippet: 'Could not ${dep.group(1)}\n${dep.group(2)}');
    }

    // 3. Everything else: said in plain words on the steps list.
    final m = j.message.toLowerCase();
    bool has(String re) => RegExp(re).hasMatch(m);
    if (has(r'limit|too many|quota')) {
      return const _Fail(0, body: 'You used all your builds for now.', tip: 'Builds reset at midnight UTC. Nothing was lost.');
    }
    if (has(r'busy|queue|capacity|no free|overload')) {
      return const _Fail(0, body: 'Every builder is taken right now. Nothing was used.', tip: 'Wait a minute, then try again.');
    }
    if (has(r'timeout|timed out|too long|gave up')) {
      if (stage <= 1) {
        return const _Fail(0, body: 'No builder was free in time. Your project is saved, so there\u2019s nothing to upload again.', tip: 'Wait a minute, then try again. Quieter hours get a builder faster.');
      }
      return const _Fail(0, body: 'The builder was too slow this time. Your project is fine.', tip: 'A second run is often faster. Removing unused videos and large images also helps.');
    }
    if (has(r'network|connect|reach|offline')) {
      return const _Fail(0, body: 'The build lost its connection.', tip: 'Check your internet, then try again.');
    }
    if (has(r'too large|too big|size')) {
      return const _Fail(0, body: 'The project is larger than the builder accepts.', tip: 'Remove big files you don\u2019t need, like videos and extra images.');
    }
    if (has(r'keystore|certificate|signing|password')) {
      return const _Fail(0, body: 'The app couldn\u2019t be signed.', tip: 'Check that your signing key and certificate match, or build without your own key.');
    }
    if (_allLines(j).length <= 3) {
      return const _Fail(0, body: 'The builder stopped before it began. Your project is fine.', tip: 'Wait a minute, then try again.');
    }
    return const _Fail(0, body: 'Something went wrong while building your app.', tip: 'Try again once. If it keeps happening, copy the code below and send it in the chat.');
  }

  /// Removes anything that could point back to the project, its repo, the builder or the machine.
  /// Returns null when the whole line should be dropped.
  String? _scrub(String line) {
    var t = line;
    t = t.replaceAll(RegExp(r'https?://\S+'), '');
    t = t.replaceAll(RegExp(r'/(?:home/runner/work|github/workspace)/[^/\s]+/[^/\s]+/?'), '');
    t = t.replaceAll(RegExp(r'/opt/hostedtoolcache/[^/\s]+/[^/\s]+/'), '');
    t = t.replaceAll(RegExp(r'/(?:home|Users)/[^/\s]+/'), '/');
    t = t.replaceAll(RegExp(r'[\w.+-]+@[\w-]+\.[\w.-]+'), '');
    if (RegExp(r'github|tyce|workflow|runner|hostedtoolcache|actions/|\.github', caseSensitive: false).hasMatch(t)) return null;
    t = t.trimRight();
    if (t.trim().isEmpty) return '';
    return t.length > 400 ? '${t.substring(0, 400)}\u2026' : t;
  }

  static final _noise = RegExp(
    r'^\s*\[=*\s*\]|Unzipping|Downloading|^\s*>?\s*(Run with|Get more help)|^\*\s*Try:|^\s*For more information'
    r'|Kotlin Gradle Plugin|Built-in Kotlin|plugin authors|changelogs of these plugins|Future versions of Flutter|report the issue to the plugin|filing$',
  );
  static final _strong = RegExp(r'\.(dart|kt|java|gradle|kts|xml|yaml):\d+|\be: |^\s*Error: |FAILURE: |What went wrong|Execution failed|Could not (resolve|find|determine|get)');
  static final _weak = RegExp(r'\bError\b|\berror\b|Exception|FAILED');

  String _detailsOf(Job j) {
    final b = StringBuffer('Step: ${_names[(stageIdx < 0 ? 0 : (finished ? failedIdx : stageIdx)).clamp(0, 5)]}\n');

    // Clean lines, with noise and anything identifying removed.
    final clean = <String>[];
    for (final raw in _allLines(j)) {
      final s = _scrub(raw);
      if (s == null) continue;
      if (_noise.hasMatch(s)) continue;
      if (s.isEmpty && (clean.isEmpty || clean.last.isEmpty)) continue;
      clean.add(s);
    }

    // The useful part starts at the first real error and runs to the end, like a copied build log.
    var from = clean.indexWhere(_strong.hasMatch);
    if (from < 0) from = clean.indexWhere(_weak.hasMatch);
    List<String> body;
    if (from >= 0) {
      body = clean.sublist(from);
    } else {
      body = clean.length > 40 ? clean.sublist(clean.length - 40) : clean;
    }
    if (body.length > 90) {
      body = [...body.take(60), '\u2026', ...body.skip(body.length - 25)];
    }
    while (body.isNotEmpty && body.last.isEmpty) {
      body.removeLast();
    }

    final msg = _scrub(j.message.trim());
    if (from < 0 && msg != null && msg.isNotEmpty && !_noise.hasMatch(msg)) b.writeln('Message: $msg');
    if (body.isNotEmpty) {
      b.writeln(from >= 0 ? '\nErrors:' : '\nLast lines:');
      for (final l in body) {
        b.writeln(l);
      }
    }
    return b.toString().trim();
  }

  /// "E-PREPARE" plus the first 8 characters of the build's id, so a build can be found on the server.
  String get _errorCode {
    var id = widget.jobId.replaceAll(RegExp(r'[^A-Za-z0-9]'), '').toLowerCase();
    if (id.length < 8) id = id.padRight(8, '0');
    return 'E-${_names[failedIdx.clamp(0, 5)].toUpperCase()} \u00B7 ${id.substring(0, 8)}';
  }

  Future<void> _copyDetails(BuildContext ctx) async {
    final j = job;
    if (j == null) return;
    await Clipboard.setData(ClipboardData(text: '$_errorCode\n${_detailsOf(j)}'));
    if (ctx.mounted) IosBanner.show(ctx, kind: BannerKind.ok, title: 'Copied', msg: 'The error details are on your clipboard.');
  }

  // ---------------------------------------------------------------- pieces

  Widget _appIcon() {
    final name = widget.appName.trim();
    final letter = name.isEmpty ? 'A' : name.characters.first.toUpperCase();
    final fallback = Container(
      color: C.tealDeep,
      alignment: Alignment.center,
      child: Text(letter, verbatim: true, style: inter(26, w: FontWeight.w700, c: C.teal, h: 1)),
    );
    final lp = _logo;
    return Container(
      width: 54, height: 54,
      decoration: BoxDecoration(borderRadius: BorderRadius.circular(14), border: Border.all(color: const Color(0xFF2A2A2C), width: 1)),
      child: ClipRRect(
        borderRadius: BorderRadius.circular(13),
        child: lp != null && File(lp).existsSync()
            ? Unfilter(child: Image.file(File(lp), key: ValueKey(lp), fit: BoxFit.cover, errorBuilder: (_, __, ___) => fallback))
            : fallback,
      ),
    );
  }

  Widget _appHeader({bool clock = true}) => Row(children: [
        _appIcon(),
        const SizedBox(width: 14),
        Expanded(
          child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            Text(widget.appName, verbatim: true, maxLines: 1, overflow: TextOverflow.ellipsis, style: inter(20, w: FontWeight.w700, h: 1.2)),
            const SizedBox(height: 3),
            Text('${job?.typeLabel ?? 'APK'} \u00B7 WZH 2 APK', verbatim: true, maxLines: 1, overflow: TextOverflow.ellipsis, style: inter(15, c: C.muted)),
          ]),
        ),
        if (clock) Text(fmtClock(elapsed), verbatim: true, style: _mono(16, C.muted)),
      ]);

  /// Back arrow on the left; Cancel (running) or a centred title (result pages) on the right / middle.
  Widget _topRow({bool cancel = false, String? title}) {
    final back = Pressable(
      onTap: () => Navigator.of(context).maybePop(),
      scale: .88,
      child: const SizedBox(width: 56, height: 56, child: Align(alignment: Alignment.centerLeft, child: Padding(padding: EdgeInsets.only(left: 18), child: Icon(Icons.arrow_back_ios_new_rounded, size: 22, color: C.text)))),
    );
    return SizedBox(
      height: 56,
      child: Row(children: [
        back,
        if (title != null)
          Expanded(child: Text(title, verbatim: true, maxLines: 1, overflow: TextOverflow.ellipsis, textAlign: TextAlign.center, style: inter(19, w: FontWeight.w600)))
        else
          const Spacer(),
        if (cancel)
          Pressable(
            onTap: (cancelling || job == null) ? null : _askCancel,
            scale: .92,
            child: Padding(
              padding: const EdgeInsets.fromLTRB(12, 14, 20, 14),
              child: cancelling
                  ? Text('Cancelling\u2026', style: inter(19, w: FontWeight.w600, c: C.muted))
                  : Text('Cancel', style: inter(19, w: FontWeight.w600, c: C.red)),
            ),
          )
        else
          const SizedBox(width: 56),
      ]),
    );
  }

  /// The thin bar. One smooth value that crawls forward and never jumps back.
  Widget _bar(Color color, {double h = 4}) => ClipRRect(
        borderRadius: BorderRadius.circular(h / 2),
        child: SizedBox(
          height: h,
          child: Stack(children: [
            Container(color: _track),
            ValueListenableBuilder<double>(
              valueListenable: _shown,
              builder: (_, v, __) => FractionallySizedBox(
                alignment: Alignment.centerLeft,
                widthFactor: v.clamp(0.0, 1.0).toDouble(),
                heightFactor: 1,
                child: Container(color: color),
              ),
            ),
          ]),
        ),
      );

  /// The reason, the tip and the copyable error code, shown inside the card.
  Widget _failBody(_Fail f) => Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Text(f.body, style: inter(17, h: 1.4)),
        const SizedBox(height: 8),
        Text(f.tip, style: inter(16, c: C.muted, h: 1.4)),
        Builder(
          builder: (ctx) => Pressable(
            onTap: () => _copyDetails(ctx),
            scale: .96,
            child: Padding(
              padding: const EdgeInsets.only(top: 14),
              child: Row(mainAxisSize: MainAxisSize.min, children: [
                const Icon(Icons.copy_rounded, size: 16, color: C.muted),
                const SizedBox(width: 8),
                Text(_errorCode, verbatim: true, style: _mono(14, C.muted)),
              ]),
            ),
          ),
        ),
      ]);

  // ---------------------------------------------------------------- pages

  Widget _page(BuildContext context) {
    final j = job;
    final kind = gone ? 2 : (j == null ? 0 : (j.isDone ? 1 : (j.isError ? 2 : (j.isCancelled ? 3 : 0))));
    final Widget body;
    if (gone) {
      body = _gonePage();
    } else if (kind == 2 && _fail != null && _fail!.type == 1) {
      body = _codeErrorPage(_fail!);
    } else if (kind == 2 && _fail != null && _fail!.type == 2) {
      body = _libraryPage(_fail!);
    } else if (kind == 2) {
      body = _failPage();
    } else {
      body = _classicPage(kind);
    }
    return Scaffold(body: SafeArea(child: body));
  }

  // ---------------------------------------------------------------- classic layout

  /// The six internal stages shown as the four the screen lists: Upload, Prepare (queue + prepare),
  /// Build (compile + publish), Ready.
  int get _s4 => const [0, 1, 1, 2, 2, 3][(stageIdx < 0 ? 0 : stageIdx).clamp(0, 5)];

  Duration? _time4(int i) {
    final parts = switch (i) { 0 => const [0], 1 => const [1, 2], 2 => const [3, 4], _ => const <int>[] };
    Duration? sum;
    for (final p in parts) {
      final t = stageTimes[p];
      if (t != null) sum = (sum ?? Duration.zero) + t;
    }
    return sum;
  }

  Widget _bigIcon() {
    final name = widget.appName.trim();
    final letter = name.isEmpty ? 'A' : name.characters.first.toUpperCase();
    final fallback = Container(
      color: C.tealDeep,
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
        : [_stat(type, 'Type'), _stat('${_s4 + 1}/4', 'Stage'), _stat(fmtClock(d), 'Time')];
    final row = <Widget>[];
    for (var i = 0; i < cells.length; i++) {
      if (i > 0) row.add(Container(width: .5, height: 30, color: const Color(0xFF2E2E30)));
      row.add(cells[i]);
    }
    return Container(
      padding: const EdgeInsets.symmetric(vertical: 6),
      child: Row(children: row),
    );
  }

  /// The four stages as a vertical timeline: dot, line, name, time.
  Widget _classicSteps(Job? j, int kind) {
    const names = ['Upload', 'Prepare', 'Build', 'Ready'];
    const hints = ['Uploading your project', 'Waiting for a free builder', 'Building your app', 'Ready to install'];
    Widget row(int i) {
      final bool d = kind == 1 || i < _s4;
      final bool a = kind == 0 && i == _s4;
      final sub = a ? ((j?.message ?? '').isNotEmpty ? j!.message : hints[i]) : null;
      final t = _time4(i);
      return Container(
        padding: const EdgeInsets.symmetric(vertical: 14),
        decoration: BoxDecoration(border: i < 3 ? const Border(bottom: BorderSide(color: Color(0xFF2A2A2C))) : null),
        child: Row(children: [
          _stepDot(d, a),
          const SizedBox(width: 14),
          Expanded(
            child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              Text(names[i], style: inter(17, c: (d || a) ? C.text : C.muted)),
              if (sub != null) ...[const SizedBox(height: 2), Text(sub, maxLines: 2, overflow: TextOverflow.ellipsis, style: inter(13, c: C.muted, h: 1.3))],
            ]),
          ),
          if (d && i < 3 && t != null) Text(fmtClock(t), verbatim: true, style: inter(14, c: C.muted)),
        ]),
      );
    }
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 16),
      decoration: BoxDecoration(color: C.card, borderRadius: BorderRadius.circular(24)),
      child: Column(children: [for (var i = 0; i < 4; i++) row(i)]),
    );
  }

  /// Summary card: a progress ring, a short title and one line under it.
  Widget _ringCard({required double value, required Color color, required String title, required String sub, bool titleVerbatim = false, Widget? trailing}) {
    final v = value.clamp(0.0, 1.0).toDouble();
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(color: C.card, borderRadius: BorderRadius.circular(24)),
      child: Row(children: [
        SizedBox(
          width: 64, height: 64,
          child: CustomPaint(
            painter: _RingPainter(v, color),
            child: Center(child: Text('${(v * 100).round()}%', verbatim: true, style: inter(16, w: FontWeight.w700, h: 1))),
          ),
        ),
        const SizedBox(width: 16),
        Expanded(
          child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            Text(title, verbatim: titleVerbatim, maxLines: 1, overflow: TextOverflow.ellipsis, style: inter(20, w: FontWeight.w600, h: 1.2)),
            const SizedBox(height: 3),
            Text(sub, verbatim: true, maxLines: 1, overflow: TextOverflow.ellipsis, style: inter(15, c: C.muted)),
          ]),
        ),
        if (trailing != null) ...[const SizedBox(width: 12), trailing],
      ]),
    );
  }

  /// The round marker in front of a step: empty circle, spinner, or filled with a tick.
  Widget _stepDot(bool d, bool a) => AnimatedContainer(
        duration: const Duration(milliseconds: 320), curve: iosCurve,
        width: 26, height: 26,
        decoration: BoxDecoration(
          color: d ? C.teal : Colors.transparent,
          shape: BoxShape.circle,
          border: d ? null : Border.all(color: a ? const Color(0xFF24473E) : const Color(0xFF4A4B50), width: 1.8),
        ),
        child: d
            ? const Icon(Icons.check_rounded, size: 16, color: Color(0xFF062019))
            : a
                ? Padding(padding: EdgeInsets.all(3), child: IosSpinner(size: 20, color: C.teal))
                : null,
      );

  Widget _xButton() => Pressable(
        onTap: (cancelling || job == null) ? null : _askCancel,
        scale: .88,
        child: Container(
          width: 36, height: 36,
          decoration: const BoxDecoration(color: Color(0xFF2A2A2C), shape: BoxShape.circle),
          child: cancelling
              ? const Padding(padding: EdgeInsets.all(10), child: IosSpinner(size: 20, color: C.muted))
              : const Icon(Icons.close_rounded, size: 20, color: C.text),
        ),
      );

  /// What sits where the Install button is on an app store page.
  Widget _status(Job? j, int kind, String sub1, String sub2) {
    if (kind == 1 && j != null) {
      return Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
        InlineDownload(key: ValueKey(j.id), job: j, auto: _wasLive, onAgain: () => Navigator.of(context).maybePop()),
      ]);
    }
    if (kind == 3) {
      return Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
        _solid('Back', () => Navigator.of(context).maybePop()),
        if (sub2.isNotEmpty) ...[const SizedBox(height: 12), Text(sub2, style: inter(15, c: C.muted))],
      ]);
    }
    if (j?.status == 'awaiting-repair-confirmation') {
      return Row(children: [
        Expanded(child: _solid('Apply fix', repairing ? () {} : () => _repair(true))),
        const SizedBox(width: 10),
        Expanded(child: _outlined('Skip', repairing ? null : () => _repair(false))),
      ]);
    }
    return Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
      ValueListenableBuilder<double>(
        valueListenable: _shown,
        builder: (_, v, __) => Row(children: [
          Expanded(child: Text(sub1, maxLines: 1, overflow: TextOverflow.ellipsis, style: inter(14, c: C.muted))),
          const SizedBox(width: 12),
          Text('${(v.clamp(0.0, 1.0) * 100).floor()}%', verbatim: true, style: inter(14, w: FontWeight.w600, c: C.text)),
        ]),
      ),
      const SizedBox(height: 12),
      Row(children: [Expanded(child: _bar(C.teal, h: 8)), const SizedBox(width: 14), _xButton()]),
    ]);
  }

  /// Running, done and cancelled: the app-store-like page.
  Widget _classicPage(int kind) {
    final j = job;
    final label = j?.typeLabel ?? 'APK';
    String sub1, sub2;
    if (j == null) {
      sub1 = 'Connecting\u2026';
      sub2 = 'Getting the latest from the server.';
    } else if (j.isDone) {
      sub1 = 'Ready to install';
      sub2 = '';
    } else if (j.isCancelled) {
      sub1 = 'Build cancelled';
      sub2 = j.message.isEmpty ? 'Nothing was kept.' : j.message;
    } else {
      sub1 = j.status == 'awaiting-repair-confirmation'
          ? 'An automatic fix is ready'
          : const ['Uploading your project', 'Waiting for a free builder', 'Building your app', 'Ready to install'][_s4.clamp(0, 3)];
      sub2 = j.message;
    }
    final subColor = kind == 1 ? C.teal : C.muted;
    return Stack(children: [
      Column(children: [
        const TopBar(back: true),
        Expanded(
          child: ListView(
            physics: const IosScrollPhysics(parent: AlwaysScrollableScrollPhysics()),
            padding: const EdgeInsets.fromLTRB(20, 24, 20, 110),
            children: [
              _in(_iv(0, .5), Row(children: [
                _bigIcon(),
                const SizedBox(width: 16),
                Expanded(
                  child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                    Text(widget.appName.replaceAll('_', '_\u200B'), verbatim: true, maxLines: 2, overflow: TextOverflow.ellipsis, style: inter(24, w: FontWeight.w700, h: 1.2)),
                    const SizedBox(height: 3),
                    Text('WZH 2 APK', verbatim: true, style: inter(15, w: FontWeight.w600, c: C.teal)),
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
                  child: KeyedSubtree(key: ValueKey('$kind${job?.status == 'awaiting-repair-confirmation'}'), child: _status(j, kind, sub1, sub2)),
                ),
              )),
              if (kind == 0 || kind == 1) ...[
                const SizedBox(height: 22),
                _in(_iv(.3, .75), _statsRow(j, kind)),
              ],
              if (kind != 3) ...[
                const SizedBox(height: 26),
                _in(_iv(.4, .9), Row(children: [Expanded(child: Text('Build steps', style: inter(20, w: FontWeight.w600))), const Icon(Icons.arrow_forward_rounded, size: 22, color: C.muted)])),
                const SizedBox(height: 16),
                _in(_iv(.5, 1), _classicSteps(j, kind)),
              ],
            ],
          ),
        ),
      ]),
      const Positioned(right: 17, bottom: 20, child: BotFab()),
    ]);
  }

  /// A step on the "Build failed" list: the one that failed shows the reason right under it.
  Widget _failRow(int i, _Fail f) {
    final done = i < failedIdx, failed = i == failedIdx;
    final t = (done || failed) && stageTimes[i] != null ? fmtClock(stageTimes[i]!) : null;
    final icon = done
        ? _stepDot(true, false)
        : failed
            ? Container(width: 26, height: 26, decoration: const BoxDecoration(color: C.red, shape: BoxShape.circle), child: const Icon(Icons.close_rounded, size: 16, color: Color(0xFF2A0F0D)))
            : _stepDot(false, false);
    return Container(
      decoration: BoxDecoration(border: i < 5 ? const Border(bottom: BorderSide(color: Color(0xFF2A2A2C))) : null),
      child: Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
        Padding(
          padding: const EdgeInsets.symmetric(vertical: 16),
          child: Row(children: [
            SizedBox(width: 26, child: Center(child: icon)),
            const SizedBox(width: 12),
            Expanded(child: Text(_names[i], style: inter(19, w: failed ? FontWeight.w700 : FontWeight.w400, c: (done || failed) ? C.text : const Color(0xFF45464A)))),
            if (t != null) Text(t, verbatim: true, style: _mono(16, C.muted)),
          ]),
        ),
        if (failed)
          Padding(
            padding: const EdgeInsets.only(left: 40, bottom: 14),
            child: Container(
              padding: const EdgeInsets.only(left: 16),
              decoration: const BoxDecoration(border: Border(left: BorderSide(color: C.red, width: 2))),
              child: _failBody(f),
            ),
          ),
      ]),
    );
  }

  /// "Build failed": the six steps with the reason under the one that stopped.
  Widget _failPage() {
    final f = _fail ?? const _Fail(0, body: 'Something went wrong while building your app.', tip: 'Try again once. If it keeps happening, copy the code below and send it in the chat.');
    return Column(children: [
      _topRow(),
      Expanded(
        child: ListView(
          physics: const IosScrollPhysics(parent: AlwaysScrollableScrollPhysics()),
          padding: const EdgeInsets.fromLTRB(20, 8, 20, 24),
          children: [
            _in(_iv(0, .5), _appHeader(clock: false)),
            const SizedBox(height: 24),
            _in(_iv(.1, .6), _ringCard(
              value: failedIdx / 6, color: C.red,
              title: 'Build failed',
              sub: 'Stopped at ${_names[failedIdx.clamp(0, 5).toInt()]} \u00B7 ${fmtClock(elapsed)}',
            )),
            const SizedBox(height: 16),
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 16),
              decoration: BoxDecoration(color: C.card, borderRadius: BorderRadius.circular(24)),
              child: Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [for (var i = 0; i < 6; i++) _failRow(i, f)]),
            ),
          ],
        ),
      ),
      Padding(padding: const EdgeInsets.fromLTRB(20, 8, 20, 20), child: _solid('Try again', () => Navigator.of(context).maybePop())),
    ]);
  }

  /// The build is no longer on the server.
  Widget _gonePage() => Column(children: [
        _topRow(),
        Expanded(
          child: ListView(padding: const EdgeInsets.fromLTRB(20, 8, 20, 24), children: [
            _appHeader(clock: false),
            const SizedBox(height: 28),
            Text('Not available', style: inter(34, w: FontWeight.w700, h: 1.1)),
            const SizedBox(height: 14),
            Text('This build is no longer on the server', style: inter(17, h: 1.4)),
            const SizedBox(height: 6),
            Text('Start a new build to get a fresh file.', style: inter(16, c: C.muted, h: 1.4)),
          ]),
        ),
        Padding(padding: const EdgeInsets.fromLTRB(20, 8, 20, 20), child: _solid('Back', () => Navigator.of(context).maybePop())),
      ]);

  /// Six small bars with the step names, the failed one in red.
  Widget _segments() => Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
        for (var i = 0; i < 6; i++)
          Expanded(
            child: Padding(
              padding: const EdgeInsets.symmetric(horizontal: 3),
              child: Column(children: [
                Container(
                  height: 4,
                  decoration: BoxDecoration(
                    color: i < failedIdx ? C.teal : (i == failedIdx ? C.red : const Color(0xFF2A2A2C)),
                    borderRadius: BorderRadius.circular(2),
                  ),
                ),
                const SizedBox(height: 12),
                Text(_names[i], maxLines: 1, style: inter(14, w: i == failedIdx ? FontWeight.w600 : FontWeight.w400, c: i == failedIdx ? C.red : C.muted)),
              ]),
            ),
          ),
      ]);

  Widget _stoppedLine() => Text(
        'Stopped at ${_names[failedIdx.clamp(0, 5)]} \u00B7 ${fmtClock(elapsed)}',
        textAlign: TextAlign.center,
        style: inter(17, c: C.muted),
      );

  /// Shared frame for the two "something specific is wrong" pages.
  Widget _resultPage({
    required IconData icon,
    required Color tint,
    required Color fg,
    required String title,
    required String body,
    Widget? card,
    required Widget actions,
  }) =>
      Builder(
        builder: (ctx) => Column(children: [
          _topRow(title: widget.appName),
          Expanded(
            child: ListView(
              physics: const IosScrollPhysics(parent: AlwaysScrollableScrollPhysics()),
              padding: const EdgeInsets.fromLTRB(20, 20, 20, 24),
              children: [
                _in(_iv(0, .5), Center(
                  child: Container(
                    width: 64, height: 64,
                    decoration: BoxDecoration(color: tint, shape: BoxShape.circle),
                    child: Icon(icon, size: 30, color: fg),
                  ),
                )),
                const SizedBox(height: 22),
                _in(_iv(.1, .6), Text(title, textAlign: TextAlign.center, style: inter(28, w: FontWeight.w700, h: 1.15))),
                const SizedBox(height: 10),
                _in(_iv(.15, .65), Text(body, textAlign: TextAlign.center, style: inter(19, c: C.muted, h: 1.4))),
                if (card != null) ...[
                  const SizedBox(height: 26),
                  _in(_iv(.25, .8), GestureDetector(
                    behavior: HitTestBehavior.translucent,
                    onLongPress: () => _copyDetails(ctx), // hidden: copies the full error text
                    child: card,
                  )),
                ],
                const SizedBox(height: 28),
                _in(_iv(.35, .9), _segments()),
                const SizedBox(height: 16),
                _in(_iv(.4, 1), _stoppedLine()),
              ],
            ),
          ),
          Padding(padding: const EdgeInsets.fromLTRB(20, 8, 20, 12), child: actions),
        ]),
      );

  Widget _cardShell({required Widget header, required Widget body}) => ClipRRect(
        borderRadius: BorderRadius.circular(22),
        child: Container(
          color: const Color(0xFF0C0C0D),
          child: Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
            Container(color: const Color(0xFF1B1B1D), padding: const EdgeInsets.fromLTRB(18, 16, 14, 16), child: header),
            body,
          ]),
        ),
      );

  Widget _cardHeader(String text, {VoidCallback? onTap, bool verbatim = true}) {
    final row = Row(children: [
      Icon(Icons.insert_drive_file_outlined, size: 22, color: C.teal),
      const SizedBox(width: 10),
      Expanded(child: Text(text, verbatim: verbatim, maxLines: 1, overflow: TextOverflow.ellipsis, style: inter(19, w: FontWeight.w600, c: C.teal))),
      if (onTap != null) Icon(Icons.chevron_right_rounded, size: 26, color: C.teal),
    ]);
    return onTap == null ? row : Pressable(onTap: onTap, scale: .98, child: row);
  }

  /// Three lines of the person's own code with the broken one marked.
  Widget _codePreview(_Fail f) {
    final bytes = CodeProject.i.files[f.file];
    if (bytes == null) return const SizedBox.shrink();
    final lines = utf8.decode(bytes, allowMalformed: true).split('\n');
    final at = (f.line - 1).clamp(0, lines.length - 1);
    final from = math.max(0, at - 1), to = math.min(lines.length - 1, at + 1);
    final shown = [for (var i = from; i <= to; i++) lines[i].replaceAll('\t', '    ').trimRight()];
    // Drop the indent every shown line shares, so the code starts at the left edge.
    var indent = 999;
    for (final l in shown) {
      if (l.trim().isEmpty) continue;
      indent = math.min(indent, l.length - l.trimLeft().length);
    }
    if (indent == 999) indent = 0;
    final rows = <Widget>[];
    for (var i = 0; i < shown.length; i++) {
      final n = from + i + 1;
      final hit = from + i == at;
      final text = shown[i].length >= indent ? shown[i].substring(indent) : shown[i].trimLeft();
      final base = _mono(16, hit ? const Color(0xFFF1F1F3) : const Color(0xFFC9CAD0), h: 1.2);
      final id = f.ident;
      final k = (hit && id != null) ? text.indexOf(id) : -1;
      final span = k >= 0
          ? TextSpan(style: base, children: [
              TextSpan(text: text.substring(0, k)),
              TextSpan(text: id, style: base.copyWith(decoration: TextDecoration.underline, decorationStyle: TextDecorationStyle.wavy, decorationColor: C.red, decorationThickness: 1.6)),
              TextSpan(text: text.substring(k + id!.length)),
            ])
          : TextSpan(text: text, style: base);
      rows.add(Container(
        height: 46,
        decoration: BoxDecoration(
          color: hit ? const Color(0xFF2A1618) : Colors.transparent,
          border: Border(left: BorderSide(color: hit ? C.red : Colors.transparent, width: 3)),
        ),
        padding: const EdgeInsets.only(left: 18, right: 14),
        child: Row(children: [
          SizedBox(width: 46, child: Text('$n', verbatim: true, style: _mono(15, hit ? C.red : const Color(0xFF55565A), w: hit ? FontWeight.w700 : FontWeight.w400))),
          Expanded(child: ClipRect(child: RichText(text: span, maxLines: 1, softWrap: false, overflow: TextOverflow.clip))),
        ]),
      ));
    }
    return Padding(padding: const EdgeInsets.symmetric(vertical: 6), child: Column(children: rows));
  }

  /// Plain words for one error.
  String _bodyFor(_Err e) {
    if (e.ident != null) return '\u201C${e.ident}\u201D isn\u2019t defined. Check the spelling.';
    final p = friendlyError(e.msg);
    if (p != null) return p;
    if (e.msg.isNotEmpty) return e.msg.length > 140 ? '${e.msg.substring(0, 140)}\u2026' : e.msg;
    return 'Something in your code stopped the build.';
  }

  /// "Your code has an error": the broken line, with Fix it. With several errors, numbered tabs switch between them.
  Widget _codeErrorPage(_Fail f0) {
    final canFix = CodeProject.i.isOpen;
    final list = f0.errors;
    final idx = list.isEmpty ? 0 : _errIdx.clamp(0, list.length - 1);
    final e = list.isEmpty ? null : list[idx];
    final body = e != null ? _bodyFor(e) : f0.body;
    final f = _Fail(1, body: body, file: e?.file ?? f0.file, line: e?.line ?? f0.line, col: e != null ? e.col : f0.col, ident: e?.ident ?? f0.ident, snippet: f0.snippet);
    final hasLine = f.file != null && CodeProject.i.files.containsKey(f.file);
    void openAtError() => openCodeFile(context, f.file!, f.line, hint: f.body, col: f.col, ident: f.ident);
    final Widget? card0 = hasLine
        ? Pressable(
            onTap: openAtError,
            scale: .985,
            child: _cardShell(
              header: _cardHeader('${f.file!.split('/').last} \u00B7 line ${f.line}', onTap: openAtError),
              body: _codePreview(f),
            ),
          )
        : (f.snippet != null
            ? _cardShell(
                header: _cardHeader('Build error', verbatim: false),
                body: Padding(
                  padding: const EdgeInsets.fromLTRB(18, 14, 18, 16),
                  child: Text(f.snippet!, verbatim: true, style: _mono(15, const Color(0xFFF2B8B0), h: 1.5)),
                ),
              )
            : null);
    Widget? card = card0;
    if (card0 != null && list.length > 1) {
      card = Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
        Row(children: [
          for (var i = 0; i < list.length; i++)
            Pressable(
              onTap: () => setState(() => _errIdx = i),
              scale: .9,
              child: Container(
                width: 32, height: 30, margin: const EdgeInsets.only(right: 8),
                alignment: Alignment.center,
                decoration: BoxDecoration(color: i == idx ? const Color(0xFF3A1B1A) : const Color(0xFF1B1B1D), borderRadius: BorderRadius.circular(8)),
                child: Text('${i + 1}', verbatim: true, style: inter(14, w: i == idx ? FontWeight.w700 : FontWeight.w400, c: i == idx ? C.red : C.muted)),
              ),
            ),
          const SizedBox(width: 4),
          Expanded(child: Text('${list.length} errors', style: inter(13.5, c: C.muted))),
          Pressable(
            onTap: () => setState(() => _errIdx = (idx + 1) % list.length),
            scale: .92,
            child: Padding(padding: const EdgeInsets.symmetric(vertical: 6), child: Text('Next', style: inter(14, w: FontWeight.w600, c: C.teal))),
          ),
        ]),
        const SizedBox(height: 12),
        card0,
      ]);
    }
    return _resultPage(
      icon: Icons.code_rounded,
      tint: const Color(0xFF3A1B1A),
      fg: C.red,
      title: 'Your code has an error',
      body: body,
      card: card,
      actions: Builder(
        builder: (ctx) => Column(mainAxisSize: MainAxisSize.min, children: [
          _solid(canFix ? 'Fix it' : 'Try again', () {
            if (!canFix) {
              Navigator.of(context).maybePop();
            } else if (hasLine) {
              openAtError();
            } else {
              Navigator.of(context).push(codeEditorRoute());
            }
          }),
          const SizedBox(height: 10),
          Row(children: [
            Expanded(child: _outlined('Copy error', () => _copyDetails(ctx))),
            if (canFix) ...[const SizedBox(width: 10), Expanded(child: _outlined('Try again', () => Navigator.of(context).maybePop()))],
          ]),
        ]),
      ),
    );
  }

  /// "A library couldn't be found": points at the build file.
  Widget _libraryPage(_Fail f) {
    final has = f.file != null;
    void open() => openCodeFile(context, f.file!, f.line, hint: 'Check the version of this library.');
    return _resultPage(
      icon: Icons.inventory_2_outlined,
      tint: const Color(0xFF3A2E17),
      fg: const Color(0xFFE8AC3E),
      title: 'A library couldn\u2019t be found',
      body: 'Check the version in your build file. If it looks right, it may be a temporary builder problem.',
      card: _cardShell(
        header: has ? _cardHeader('Open ${f.file}', onTap: open) : _cardHeader('Build file'),
        body: Padding(
          padding: const EdgeInsets.fromLTRB(18, 14, 18, 16),
          child: Text(f.snippet ?? '', verbatim: true, softWrap: true, style: _mono(15, const Color(0xFFF2B8B0), h: 1.5)),
        ),
      ),
      actions: Column(mainAxisSize: MainAxisSize.min, children: [
        if (has) ...[
          _solid('Open build file', open),
          _quiet('Try again', () => Navigator.of(context).maybePop()),
        ] else
          _solid('Try again', () => Navigator.of(context).maybePop()),
      ]),
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

/// Download that happens right on the build screen (no pop-up): the buttons turn into a progress card,
/// then into Install. Real byte progress (the bar is indeterminate when the server sends no length).
class InlineDownload extends StatefulWidget {
  final Job job;
  /// Start the download right away (a build that just finished while the person was watching it).
  final bool auto;
  /// "Build again": sits next to Install once the file is ready, and under the other states.
  final VoidCallback? onAgain;
  const InlineDownload({super.key, required this.job, this.auto = false, this.onAgain});
  @override
  State<InlineDownload> createState() => _InlineDownloadState();
}

enum _Dl { idle, running, done, failed }

class _InlineDownloadState extends State<InlineDownload> {
  _Dl mode = _Dl.idle;
  int done = 0;
  int? total;
  String? path, error, savedAt, aab;
  bool cancelled = false;

  @override
  void initState() {
    super.initState();
    if (widget.auto) {
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (mounted && mode == _Dl.idle) _start();
      });
    }
  }

  bool get _isApk => aab == null && (path?.toLowerCase().endsWith('.apk') ?? false);

  Future<void> _start({String? aabName}) async {
    setState(() {
      mode = _Dl.running; aab = aabName; done = 0; total = null;
      path = null; error = null; savedAt = null; cancelled = false;
    });
    final my = aabName;
    try {
      final p = await Api.i.download(widget.job, (d, t) {
        if (mounted && mode == _Dl.running) setState(() { done = d; total = t; });
      }, aab: my, cancelled: () => cancelled);
      // The copy in Downloads keeps exactly the name the file was saved with (the server's file name).
      final fileName = p.split('/').last;
      final pub = await FileSaver.saveToDownloads(p, widget.job.name, fileName: fileName);
      if (!mounted || cancelled) return;
      setState(() { path = p; savedAt = pub; mode = _Dl.done; });
      // Open the installer by itself (Android asks to allow "install unknown apps" if needed).
      if (_isApk) {
        await Future.delayed(const Duration(milliseconds: 450));
        if (mounted && !cancelled) FileSaver.install(p);
      }
    } catch (e) {
      if (!mounted) return;
      if (cancelled) {
        setState(() => mode = _Dl.idle);
      } else {
        setState(() {
          mode = _Dl.failed;
          error = e is ApiError ? e.message : 'Download didn\u2019t finish. Check your connection and try again.';
        });
      }
    }
  }

  String _mb(int b) => '${(b / 1048576).toStringAsFixed(1)}MB';

  Widget _btn(String t, VoidCallback f, {bool solid = true, bool plain = false}) => Pressable(
        onTap: f,
        child: Container(
          height: 52, alignment: Alignment.center,
          decoration: BoxDecoration(color: solid ? C.tealBtn : const Color(0xFF2A2A2C), borderRadius: BorderRadius.circular(26)),
          child: Text(t, maxLines: 1, style: inter(solid ? 20 : 18, w: plain ? FontWeight.w600 : FontWeight.w700, c: solid ? const Color(0xFF252A27) : (plain ? C.text : C.teal))),
        ),
      );

  Widget _card({required Widget icon, required String title, required Widget sub, Widget? trailing, Widget? bar}) => Container(
        padding: const EdgeInsets.fromLTRB(16, 14, 16, 14),
        decoration: BoxDecoration(color: C.card, borderRadius: BorderRadius.circular(20)),
        child: Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
          Row(children: [
            SizedBox(width: 40, height: 40, child: icon),
            const SizedBox(width: 12),
            Expanded(
              child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                Text(title, style: inter(17, w: FontWeight.w700, h: 1.2)),
                const SizedBox(height: 3),
                sub,
              ]),
            ),
            if (trailing != null) trailing,
          ]),
          if (bar != null) ...[const SizedBox(height: 12), bar],
        ]),
      );

  Widget _circle(Color bg, Widget child) => Container(decoration: BoxDecoration(color: bg, shape: BoxShape.circle), child: Center(child: child));

  @override
  Widget build(BuildContext context) {
    final aabName = widget.job.aabName;
    Widget body;
    switch (mode) {
      case _Dl.idle:
        body = Row(children: [
          Expanded(child: _btn('Download', () => _start())),
          if (aabName != null) ...[
            const SizedBox(width: 10),
            Expanded(child: _btn('Download AAB', () => _start(aabName: aabName), solid: false)),
          ],
        ]);
        break;
      case _Dl.running:
        body = _card(
          icon: _circle(const Color(0xFF26403A), const SizedBox(width: 20, height: 20, child: IosSpinner(size: 20))),
          title: aab != null ? 'Downloading AAB\u2026' : 'Downloading\u2026',
          sub: Text.rich(TextSpan(children: [
            TextSpan(text: _mb(done), style: inter(14, w: FontWeight.w600, c: C.teal)),
            if (total != null) TextSpan(text: ' / ${_mb(total!)}', style: inter(14, c: C.muted)),
          ])),
          trailing: Pressable(
            onTap: () => setState(() => cancelled = true),
            child: Padding(padding: const EdgeInsets.all(6), child: Text('Cancel', style: inter(15, w: FontWeight.w600, c: C.muted))),
          ),
          bar: ClipRRect(
            borderRadius: BorderRadius.circular(3),
            child: LinearProgressIndicator(
              value: total != null && total! > 0 ? (done / total!).clamp(0.0, 1.0) : null,
              minHeight: 6, backgroundColor: const Color(0xFF2E2E30), valueColor: AlwaysStoppedAnimation(C.tealBtn)),
          ),
        );
        break;
      case _Dl.done:
        final shown = (savedAt ?? path ?? '').split('/').last;
        body = Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
          if (_isApk || widget.onAgain != null) ...[
            Row(children: [
              if (widget.onAgain != null) Expanded(child: _btn('Build again', widget.onAgain!, solid: false, plain: true)),
              if (_isApk && widget.onAgain != null) const SizedBox(width: 10),
              if (_isApk) Expanded(child: _btn('Install', () => FileSaver.install(path!))),
            ]),
            const SizedBox(height: 12),
          ],
          Text(
            '${savedAt != null ? 'Saved to Downloads \u00B7' : 'Couldn\u2019t save to Downloads, but your file is ready:'} $shown',
            style: inter(14, c: C.muted, h: 1.35),
          ),
          if (_isApk) ...[
            const SizedBox(height: 4),
            Text('The installer opens by itself. If it doesn\u2019t, tap Install.', style: inter(14, c: C.muted, h: 1.35)),
          ],
          Pressable(
            onTap: () => setState(() => mode = _Dl.idle),
            child: Padding(
              padding: const EdgeInsets.symmetric(vertical: 12),
              child: Text('Download again', style: inter(14.5, w: FontWeight.w600, c: C.teal)),
            ),
          ),
        ]);
        break;
      case _Dl.failed:
        body = Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
          _card(
            icon: _circle(const Color(0x33F0483E), const Icon(Icons.priority_high_rounded, size: 22, color: C.red)),
            title: 'Download failed',
            sub: Text(error ?? '', style: inter(13.5, c: C.muted, h: 1.3)),
          ),
          const SizedBox(height: 12),
          _btn('Try again', () => _start(aabName: aab)),
        ]);
        break;
    }
    if (mode != _Dl.done && widget.onAgain != null) {
      body = Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
        body,
        const SizedBox(height: 12),
        _btn('Build again', widget.onAgain!, solid: false, plain: true),
      ]);
    }
    return AnimatedSize(
      duration: const Duration(milliseconds: 260), curve: iosCurve, alignment: Alignment.topCenter,
      child: AnimatedSwitcher(duration: const Duration(milliseconds: 200), child: KeyedSubtree(key: ValueKey(mode), child: body)),
    );
  }
}

/// The thin progress ring: a faint full circle with the done part drawn over it, round at the ends.
class _RingPainter extends CustomPainter {
  final double v;
  final Color color;
  _RingPainter(this.v, this.color);
  @override
  void paint(Canvas c, Size s) {
    const w = 7.0;
    final r = Rect.fromLTWH(w / 2, w / 2, s.width - w, s.height - w);
    c.drawArc(r, 0, math.pi * 2, false, Paint()..style = PaintingStyle.stroke..strokeWidth = w..color = color.withOpacity(.16));
    if (v > 0) {
      c.drawArc(r, -math.pi / 2, math.pi * 2 * v, false, Paint()..style = PaintingStyle.stroke..strokeWidth = w..strokeCap = StrokeCap.round..color = color);
    }
  }

  @override
  bool shouldRepaint(_RingPainter o) => o.v != v || o.color != color;
}
