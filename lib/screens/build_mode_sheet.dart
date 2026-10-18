import 'package:flutter/material.dart';
import '../anim.dart';
import '../theme.dart';

/// Asked when you tap Continue on an HTML or website build.
/// Returns 'quick' (built on the phone with the local wrapper), 'full' (built on the server,
/// every feature), or null if the sheet was closed without choosing.
Future<String?> showBuildModeSheet(BuildContext context) =>
    showIosSheet<String>(context, (_) => const _BuildModeSheet());

class _BuildModeSheet extends StatelessWidget {
  const _BuildModeSheet();

  void _pick(BuildContext c, String v) => Navigator.of(c, rootNavigator: true).pop(v);

  @override
  Widget build(BuildContext context) => Container(
        width: double.infinity,
        padding: EdgeInsets.fromLTRB(16, 10, 16, 18 + MediaQuery.of(context).padding.bottom),
        decoration: const BoxDecoration(color: C.sheet, borderRadius: BorderRadius.vertical(top: Radius.circular(25))),
        child: Column(mainAxisSize: MainAxisSize.min, crossAxisAlignment: CrossAxisAlignment.stretch, children: [
          Center(
            child: Container(width: 38, height: 5, decoration: BoxDecoration(color: const Color(0xFF4A4A4C), borderRadius: BorderRadius.circular(3))),
          ),
          const SizedBox(height: 14),
          Row(children: [
            Pressable(
              onTap: () => Navigator.of(context, rootNavigator: true).pop(),
              child: const SizedBox(width: 36, height: 36, child: Icon(Icons.close_rounded, size: 24, color: Colors.white)),
            ),
            const SizedBox(width: 8),
            Expanded(
              child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                Text('How should it build?', style: inter(22, w: FontWeight.w700)),
                Text('You can pick a different way next time.', style: inter(13, c: C.muted)),
              ]),
            ),
          ]),
          const SizedBox(height: 16),
          _Option(
            icon: Icons.bolt_rounded,
            title: 'Quick build',
            tag: 'Offline',
            lines: const ['Built on your phone from a ready-made wrapper.', 'HTML and websites. Ready in seconds, works without internet.'],
            onTap: () => _pick(context, 'quick'),
          ),
          const SizedBox(height: 10),
          _Option(
            icon: Icons.cloud_outlined,
            title: 'Full build',
            lines: const ['Built on the server with every feature.', 'Slower, but includes protection, splash screens, app bundles and Google sign-in.'],
            onTap: () => _pick(context, 'full'),
          ),
        ]),
      );
}

class _Option extends StatelessWidget {
  final IconData icon;
  final String title;
  final String? tag;
  final List<String> lines;
  final VoidCallback onTap;
  const _Option({required this.icon, required this.title, this.tag, required this.lines, required this.onTap});

  @override
  Widget build(BuildContext context) => Pressable(
        scale: .97,
        onTap: onTap,
        child: Container(
          padding: const EdgeInsets.all(14),
          decoration: BoxDecoration(color: C.field, borderRadius: BorderRadius.circular(16)),
          child: Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
            Container(
              width: 42, height: 42,
              decoration: BoxDecoration(color: C.tealDeep, borderRadius: BorderRadius.circular(12)),
              child: Icon(icon, size: 24, color: C.teal),
            ),
            const SizedBox(width: 14),
            Expanded(
              child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                Row(children: [
                  Text(title, style: inter(18, w: FontWeight.w700)),
                  if (tag != null) ...[
                    const SizedBox(width: 8),
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                      decoration: BoxDecoration(color: C.tealDeep, borderRadius: BorderRadius.circular(8)),
                      child: Text(tag!, style: inter(11, w: FontWeight.w600, c: C.teal)),
                    ),
                  ],
                ]),
                const SizedBox(height: 4),
                for (final l in lines) Text(l, style: inter(13, c: C.muted, h: 1.35)),
              ]),
            ),
            const SizedBox(width: 6),
            const Padding(padding: EdgeInsets.only(top: 10), child: Icon(Icons.chevron_right_rounded, size: 22, color: C.muted)),
          ]),
        ),
      );
}
