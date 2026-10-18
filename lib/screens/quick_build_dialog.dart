import 'dart:io';
import 'package:flutter/material.dart';
import '../api.dart';
import '../theme.dart';
import '../anim.dart';
import '../quick/quick_builder.dart';

/// Runs a Quick build on the phone and shows it like the download dialog does.
/// Pops 'full' if the person asks for a server build instead (or when this app has no template).
class QuickBuildDialog extends StatefulWidget {
  final BuildRequest req;
  final String appName;
  const QuickBuildDialog({super.key, required this.req, required this.appName});
  @override
  State<QuickBuildDialog> createState() => _QuickBuildDialogState();
}

class _QuickBuildDialogState extends State<QuickBuildDialog> {
  String step = 'Starting';
  String? path, error;
  bool templateMissing = false;
  int? size;

  @override
  void initState() {
    super.initState();
    _run();
  }

  Future<void> _run() async {
    try {
      final p = await QuickBuild.run(widget.req, appName: widget.appName, onStep: (s) {
        if (mounted) setState(() => step = s);
      });
      int? n;
      try {
        n = await File(p).length();
      } catch (_) {}
      if (mounted) setState(() { path = p; size = n; });
    } on QuickBuildException catch (e) {
      if (mounted) setState(() { error = e.message; templateMissing = e.templateMissing; });
    } catch (e) {
      if (mounted) setState(() => error = 'Something went wrong while building: $e');
    }
  }

  String _mb(int b) => b >= 1048576 ? '${(b / 1048576).toStringAsFixed(1)} MB' : '${(b / 1024).toStringAsFixed(0)} KB';

  Widget _btn(String label, VoidCallback onTap, {bool primary = true}) => Pressable(
        onTap: onTap,
        child: Container(
          height: 48, alignment: Alignment.center,
          decoration: BoxDecoration(color: primary ? C.tealBtn : const Color(0xFF2A2A2C), borderRadius: BorderRadius.circular(24)),
          child: Text(label, style: inter(17, w: FontWeight.w700, c: primary ? const Color(0xFF0F2A24) : C.text)),
        ),
      );

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
          Text(complete ? 'Your app is ready' : (failed ? 'Quick build didn\u2019t finish' : 'Building on your phone\u2026'), style: inter(20, w: FontWeight.w700)),
          const SizedBox(height: 8),
          if (failed)
            Text(error!, style: inter(16, c: C.muted, h: 1.35))
          else if (complete) ...[
            Text('Saved to $shown${size != null ? ' (${_mb(size!)})' : ''}', style: inter(15, c: C.muted, h: 1.35)),
            const SizedBox(height: 6),
            Text('Open it from your Files app to install. It works without internet.', style: inter(15, c: C.muted, h: 1.35)),
          ] else
            Text(step, style: inter(16, c: C.muted, h: 1.35)),
          const SizedBox(height: 20),
          if (failed && templateMissing) ...[
            _btn('Build on the server instead', () => Navigator.pop(context, 'full')),
            const SizedBox(height: 10),
            _btn('Cancel', () => Navigator.pop(context), primary: false),
          ] else if (complete || failed)
            _btn('Done', () => Navigator.pop(context))
          else
            _btn('Please wait', () {}, primary: false),
        ]),
      ),
    );
  }
}
