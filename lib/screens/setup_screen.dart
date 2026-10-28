import 'dart:async';
import 'dart:io';
import 'package:flutter/material.dart';
import 'package:wakelock_plus/wakelock_plus.dart';
import '../core/bootstrap.dart';
import '../core/linux.dart';
import '../core/shell_session.dart';
import '../theme.dart';
import 'terminal_panel.dart';

enum _Phase { download, terminal, failed, done }

/// First launch: downloads the Linux, then opens a terminal that installs Java, Flutter and the
/// Android tools by itself. The person only has to keep the app open.
class SetupScreen extends StatefulWidget {
  final VoidCallback onDone;
  const SetupScreen({super.key, required this.onDone});
  @override
  State<SetupScreen> createState() => _SetupScreenState();
}

class _SetupScreenState extends State<SetupScreen> {
  _Phase phase = _Phase.download;
  String status = 'Starting…';
  double? prog;
  String? error;
  int stage = 0, stages = 7;
  String stageName = '';
  StreamSubscription<String>? _outSub;
  StreamSubscription<int>? _exitSub;
  String _buf = '';
  DateTime _last = DateTime.fromMillisecondsSinceEpoch(0);

  @override
  void initState() {
    super.initState();
    WakelockPlus.enable();
    _go();
  }

  @override
  void dispose() {
    _outSub?.cancel();
    _exitSub?.cancel();
    WakelockPlus.disable();
    super.dispose();
  }

  Future<void> _go() async {
    setState(() {
      phase = _Phase.download;
      error = null;
      status = 'Starting…';
      prog = null;
    });
    try {
      await Bootstrap((s, p) {
        final now = DateTime.now();
        if (now.difference(_last).inMilliseconds < 120 && p != null && p < 1) return;
        _last = now;
        if (mounted) setState(() {
          status = s;
          prog = p;
        });
      }).run();
    } catch (e) {
      if (mounted) setState(() {
        phase = _Phase.failed;
        error = e.toString();
      });
      return;
    }
    if (!mounted) return;
    _startTerminal();
  }

  void _startTerminal() {
    ShellSession.resetSetup();
    final s = ShellSession.setup;
    setState(() => phase = _Phase.terminal);
    _outSub?.cancel();
    _exitSub?.cancel();
    _outSub = s.output.listen((chunk) {
      _buf += chunk;
      if (_buf.length > 600) _buf = _buf.substring(_buf.length - 300);
      final m = RegExp(r'##STAGE (\d+)/(\d+) ([^\r\n]*)').allMatches(_buf).toList();
      if (m.isNotEmpty) {
        final x = m.last;
        final n = int.parse(x.group(1)!);
        if (n != stage && mounted) setState(() {
          stage = n;
          stages = int.parse(x.group(2)!);
          stageName = x.group(3)!.trim();
        });
      }
    });
    _exitSub = s.onExit.listen((code) {
      if (!mounted) return;
      if (Linux.ready) {
        setState(() => phase = _Phase.done);
        Future<void>.delayed(const Duration(milliseconds: 1600), () {
          if (mounted) widget.onDone();
        });
      } else {
        setState(() => phase = _Phase.failed);
      }
    });
    s.start();
  }

  Future<void> _retry({bool compat = false}) async {
    if (compat) File('${Linux.base}/no_seccomp').writeAsStringSync('1');
    await Bootstrap.writeConfig(); // refreshes the setup script too
    if (Linux.prootInstalled && Linux.rootfsInstalled) {
      _startTerminal();
    } else {
      _go();
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: C.bg,
      body: SafeArea(child: switch (phase) {
        _Phase.download => _download(),
        _Phase.failed when error != null => _failedBootstrap(),
        _ => _terminal(),
      }),
    );
  }

  Widget _download() => Padding(
        padding: const EdgeInsets.symmetric(horizontal: 28),
        child: Column(mainAxisAlignment: MainAxisAlignment.center, crossAxisAlignment: CrossAxisAlignment.start, children: [
          Text('Setting up WZH Flutter IDE', style: inter(24, w: FontWeight.w800)),
          const SizedBox(height: 10),
          Text('This happens once. The app is getting its own small Linux so it can run Flutter on your phone. Nothing for you to do: just keep this screen open.', style: inter(14.5, c: C.muted, h: 1.4)),
          const SizedBox(height: 28),
          LinearProgressIndicator(value: prog, minHeight: 6, color: C.teal, backgroundColor: C.card, borderRadius: BorderRadius.circular(3)),
          const SizedBox(height: 12),
          Text(status, style: inter(13.5)),
          const SizedBox(height: 28),
          Text('Total download is about 1.5 to 2 GB, so Wi-Fi is recommended. Plan on 6 GB of free space.', style: inter(13, c: C.muted, h: 1.4)),
        ]),
      );

  Widget _failedBootstrap() => Padding(
        padding: const EdgeInsets.symmetric(horizontal: 28),
        child: Column(mainAxisAlignment: MainAxisAlignment.center, crossAxisAlignment: CrossAxisAlignment.start, children: [
          Text('Setup hit a problem', style: inter(22, w: FontWeight.w800)),
          const SizedBox(height: 10),
          Flexible(child: SingleChildScrollView(child: Text(error ?? '', style: inter(13.5, c: C.muted, h: 1.4)))),
          const SizedBox(height: 20),
          FilledButton(
            onPressed: _retry,
            style: FilledButton.styleFrom(backgroundColor: C.tealBtn, foregroundColor: C.onTeal, minimumSize: const Size(160, 46)),
            child: Text('Try again', style: inter(15, w: FontWeight.w700, c: C.onTeal)),
          ),
        ]),
      );

  Widget _terminal() {
    final s = ShellSession.setup;
    final failed = phase == _Phase.failed;
    final done = phase == _Phase.done;
    return Column(children: [
      Container(
        color: C.card,
        padding: const EdgeInsets.fromLTRB(16, 12, 16, 10),
        child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Row(children: [
            Expanded(
              child: Text(
                done ? 'All set' : failed ? 'Setup stopped' : (stage == 0 ? 'Starting…' : 'Step $stage of $stages · $stageName'),
                style: inter(14.5, w: FontWeight.w700, c: failed ? C.red : (done ? C.teal : C.text)),
              ),
            ),
          ]),
          const SizedBox(height: 8),
          LinearProgressIndicator(value: done ? 1 : (stage == 0 ? null : (stage - 1) / stages), minHeight: 5, color: failed ? C.red : C.teal, backgroundColor: C.bg, borderRadius: BorderRadius.circular(3)),
          if (!failed && !done) ...[
            const SizedBox(height: 8),
            Text('Keep the app open. Steps 1 to 7 download a few GB and can take 15 to 40 minutes.', style: inter(12, c: C.muted)),
          ],
        ]),
      ),
      Expanded(child: TerminalPanel(session: s, showHeader: false)),
      if (failed)
        Container(
          color: C.card,
          padding: const EdgeInsets.all(12),
          child: Row(children: [
            Expanded(
              child: FilledButton(
                onPressed: _retry,
                style: FilledButton.styleFrom(backgroundColor: C.tealBtn, foregroundColor: C.onTeal, minimumSize: const Size(0, 44)),
                child: Text('Try again', style: inter(14.5, w: FontWeight.w700, c: C.onTeal)),
              ),
            ),
            const SizedBox(width: 10),
            Expanded(
              child: OutlinedButton(
                onPressed: () => _retry(compat: true),
                style: OutlinedButton.styleFrom(minimumSize: const Size(0, 44), side: const BorderSide(color: C.line)),
                child: Text('Compatibility mode', style: inter(14, w: FontWeight.w600)),
              ),
            ),
          ]),
        ),
    ]);
  }
}
