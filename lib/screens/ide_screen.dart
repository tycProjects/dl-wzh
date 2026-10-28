import 'dart:async';
import 'dart:io';
import 'package:flutter/material.dart';
import 'package:open_filex/open_filex.dart';
import 'package:path_provider/path_provider.dart';
import '../banner.dart';
import '../core/linux.dart';
import '../core/project_disk.dart';
import '../core/shell_session.dart';
import '../theme.dart';
import 'code_editor.dart';
import 'terminal_panel.dart';

/// Your code editor with the terminal under it. Edits are written to the project folder inside
/// Ubuntu a moment after you type, so commands always see the latest code.
class IdeScreen extends StatefulWidget {
  final String name;
  /// True for a brand new project: runs `flutter create` first.
  final bool create;
  const IdeScreen({super.key, required this.name, this.create = false});
  @override
  State<IdeScreen> createState() => _IdeScreenState();
}

class _IdeScreenState extends State<IdeScreen> with WidgetsBindingObserver {
  final p = CodeProject.i;
  late final ProjectDisk disk = ProjectDisk('${Linux.projectsDir}/${widget.name}');
  Timer? _timer;
  bool panel = false, big = false, loading = true, running = false;
  String get _guestDir => '/root/projects/${widget.name}';

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    p
      ..resetView()
      ..files.clear()
      ..modified.clear()
      ..name = widget.name
      ..diskRoot = disk.root
      ..onAction = _onAction;
    _timer = Timer.periodic(const Duration(milliseconds: 1500), (_) => _syncNow());
    _load();
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    _timer?.cancel();
    p.onAction = null;
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state != AppLifecycleState.resumed) _syncNow();
  }

  Future<void> _load() async {
    if (widget.create) {
      Directory(Linux.projectsDir).createSync(recursive: true);
      setState(() {
        loading = false;
        panel = true;
      });
      _create();
      return;
    }
    final files = await disk.load();
    if (!mounted) return;
    p.files
      ..clear()
      ..addAll(files);
    setState(() => loading = false);
    if (p.files.containsKey('lib/main.dart')) _openMain();
  }

  void _openMain() {
    p.pending = ('lib/main.dart', 1);
    p.jumpTick.value++;
  }

  /// Puts what is typed into the project and writes the changed files to disk.
  Future<void> _syncNow() async {
    if (!p.isOpen) return;
    p.flushHook?.call();
    await disk.sync(p.files);
  }

  /// Picks up files changed by a command, then refreshes the open tabs.
  Future<void> _pullFromDisk() async {
    await _syncNow();
    final changed = await disk.refresh(p.files);
    if (changed && mounted) {
      setState(() {});
      p.reloadHook?.call();
    }
  }

  void _create() {
    final n = widget.name;
    final cmd = 'mkdir -p /root/projects && cd /root/projects && flutter create --org com.wzh --project-name $n $n'
        " && sed -i 's/-Xmx[0-9a-zA-Z]*/-Xmx1536M/' $n/android/gradle.properties";
    running = true;
    ShellSession.main.run(cmd, done: (code) async {
      running = false;
      if (!mounted) return;
      if (code == 0) {
        final files = await disk.load();
        if (!mounted) return;
        p.files
          ..clear()
          ..addAll(files);
        setState(() => panel = false);
        p.reloadHook?.call();
        _openMain();
        IosBanner.show(context, kind: BannerKind.ok, title: 'Project ready', msg: widget.name);
      } else {
        IosBanner.show(context, kind: BannerKind.err, title: 'Couldn’t create the project', msg: 'Look at the terminal for the reason.', duration: const Duration(seconds: 6));
      }
    });
  }

  void _onAction(String a) {
    switch (a) {
      case 'terminal':
        setState(() => panel = !panel);
        if (panel) ShellSession.main.start();
        return;
      case 'reload':
        _pullFromDisk();
        return;
      case 'pubget':
        _run('flutter pub get');
      case 'analyze':
        _run('flutter analyze');
      case 'clean':
        _run('flutter clean');
      case 'release':
        _run('flutter build apk --release --target-platform android-arm64', apk: 'build/app/outputs/flutter-apk/app-release.apk');
      case 'build' || 'run':
        _run('flutter build apk --debug --target-platform android-arm64', apk: 'build/app/outputs/flutter-apk/app-debug.apk');
    }
  }

  void _run(String cmd, {String? apk}) async {
    if (running) {
      IosBanner.show(context, kind: BannerKind.warn, title: 'Still busy', msg: 'Wait for the current command to finish (see the terminal).');
      setState(() => panel = true);
      return;
    }
    await _syncNow();
    if (!mounted) return;
    running = true;
    setState(() => panel = true);
    ShellSession.main.run('cd $_guestDir && $cmd', done: (code) async {
      running = false;
      if (!mounted) return;
      setState(() {});
      await _pullFromDisk();
      if (!mounted) return;
      if (code != 0) {
        IosBanner.show(context, kind: BannerKind.err, title: 'That didn’t work', msg: 'Exit code $code. The terminal shows why.', duration: const Duration(seconds: 6));
        return;
      }
      if (apk != null) {
        final f = File('${disk.root}/$apk');
        if (!f.existsSync()) {
          IosBanner.show(context, kind: BannerKind.warn, title: 'Built, but no APK found', msg: apk);
          return;
        }
        IosBanner.show(context, kind: BannerKind.ok, title: 'APK built', msg: 'Opening the installer…');
        try {
          final tmp = await getTemporaryDirectory();
          final copy = await f.copy('${tmp.path}/${widget.name}.apk');
          await OpenFilex.open(copy.path, type: 'application/vnd.android.package-archive');
        } catch (e) {
          if (mounted) IosBanner.show(context, kind: BannerKind.err, title: 'Couldn’t open the installer', msg: '$e');
        }
      } else {
        IosBanner.show(context, kind: BannerKind.ok, title: 'Done', msg: cmd);
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    return PopScope(
      canPop: false,
      onPopInvokedWithResult: (didPop, _) async {
        if (didPop) return;
        await _syncNow();
        if (mounted) Navigator.of(context).pop();
      },
      child: Scaffold(
        backgroundColor: C.bg,
        resizeToAvoidBottomInset: true,
        body: loading
            ? const Center(child: CircularProgressIndicator(color: C.teal))
            : LayoutBuilder(builder: (context, cons) {
                final h = !panel ? 0.0 : (big ? cons.maxHeight * .85 : (cons.maxHeight * .42).clamp(170.0, 380.0).toDouble());
                return Column(children: [
                  Expanded(child: CodeEditorPage(key: const ValueKey('editor'), embedded: true)),
                  if (panel)
                    SizedBox(
                      height: h,
                      child: TerminalPanel(
                        session: ShellSession.main..start(),
                        title: running ? 'Terminal · running…' : 'Terminal',
                        maximized: big,
                        onToggleSize: () => setState(() => big = !big),
                        onClose: () => setState(() => panel = false),
                      ),
                    ),
                ]);
              }),
      ),
    );
  }
}
