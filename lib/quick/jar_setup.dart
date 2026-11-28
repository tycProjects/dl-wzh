import 'package:flutter/material.dart' hide Text;
import '../anim.dart';
import '../theme.dart';
import '../tr.dart';
import '../widgets.dart';
import 'aapt_build.dart';

/// First-launch popup that downloads android.jar (the build resources) once, with a progress bar,
/// instead of leaving the download to happen silently the first time someone builds.
///
/// It shows only when android.jar is not already on the phone (or bundled in the app), once per app
/// start. "Hide" closes the popup and the download keeps going in the background.
class JarSetup {
  static bool _asked = false;

  static Future<void> maybeShow(BuildContext context) async {
    if (_asked) return;
    if (await androidJarReady()) return;
    if (!context.mounted) return;
    _asked = true;
    await showIosDialog<void>(context, (_) => const _JarDialog());
  }

  /// Menu item: always opens the popup. If android.jar is already installed it just says "Ready".
  static Future<void> show(BuildContext context) =>
      showIosDialog<void>(context, (_) => const _JarDialog());

  /// Called when a build starts: if android.jar is missing, shows the download popup and waits until
  /// it is installed (no Hide button, so the build never races a second download).
  static Future<void> ensure(BuildContext context) async {
    if (await androidJarReady()) return;
    if (!context.mounted) return;
    await showIosDialog<void>(context, (_) => const _JarDialog(canHide: false));
  }
}

class _JarDialog extends StatefulWidget {
  final bool canHide;
  const _JarDialog({this.canHide = true});
  @override
  State<_JarDialog> createState() => _JarDialogState();
}

class _JarDialogState extends State<_JarDialog> {
  int _got = 0;
  int? _total;
  String? _error;
  bool _done = false;
  int _lastPaint = 0;

  @override
  void initState() {
    super.initState();
    _run();
  }

  Future<void> _run() async {
    try {
      await androidJarPath(onProgress: (got, total) {
        // Repaint at most about 8 times a second so a fast download doesn't flood the UI.
        final now = DateTime.now().millisecondsSinceEpoch;
        if (!mounted || (now - _lastPaint < 120 && got != total)) return;
        _lastPaint = now;
        setState(() {
          _got = got;
          _total = total;
        });
      });
      if (!mounted) return;
      setState(() => _done = true);
      await Future.delayed(const Duration(milliseconds: 900));
      if (mounted) Navigator.of(context).pop();
    } catch (e) {
      if (!mounted) return;
      setState(() => _error = e.toString().replaceFirst('Exception: ', ''));
    }
  }

  void _retry() {
    setState(() {
      _error = null;
      _got = 0;
      _total = null;
    });
    _run();
  }

  String _mb(int b) => (b / 1048576).toStringAsFixed(1);

  @override
  Widget build(BuildContext context) {
    final total = _total;
    final pct = (total != null && total > 0) ? (_got / total).clamp(0.0, 1.0) : null;
    const muted = Color(0xFFB4B4B8);

    if (_error != null) {
      return DialogCard(
        title: 'Download failed',
        message: _error,
        actions: [
          DialogAction(widget.canHide ? 'Later' : 'Close', color: muted, onTap: () => Navigator.of(context).pop()),
          DialogAction('Try again', onTap: _retry),
        ],
      );
    }
    if (_done) {
      return const DialogCard(
        title: 'Ready',
        message: 'Build resources are installed. Building on your phone is ready to use.',
        actions: [],
      );
    }
    return DialogCard(
      title: 'Downloading build resources',
      message: 'One-time download so building on your phone works. Keep the app open until it finishes.',
      body: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        ClipRRect(
          borderRadius: BorderRadius.circular(4),
          child: SizedBox(
            height: 6,
            child: pct == null
                ? const LinearProgressIndicator(backgroundColor: Color(0x1FFFFFFF))
                : TweenAnimationBuilder<double>(
                    tween: Tween(end: pct),
                    duration: const Duration(milliseconds: 160),
                    builder: (_, v, __) => Stack(children: [
                      Container(color: const Color(0x1FFFFFFF)),
                      FractionallySizedBox(widthFactor: v, child: Container(color: C.teal)),
                    ]),
                  ),
          ),
        ),
        const SizedBox(height: 10),
        Row(children: [
          Expanded(
            child: Text(
              total != null ? '${_mb(_got)} MB of ${_mb(total)} MB' : '${_mb(_got)} MB',
              style: inter(13.5, c: C.muted),
            ),
          ),
          if (pct != null) Text('${(pct * 100).round()}%', style: inter(13.5, w: FontWeight.w600, c: C.muted)),
        ]),
      ]),
      actions: widget.canHide ? [DialogAction('Hide', color: muted, onTap: () => Navigator.of(context).pop())] : const [],
    );
  }
}
