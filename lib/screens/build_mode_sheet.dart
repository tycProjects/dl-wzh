import 'package:flutter/material.dart' hide Text;
import '../anim.dart';
import '../theme.dart';
import '../tr.dart';

/// Asked when you tap Continue on an HTML or website build.
/// Returns 'quick' (built on the phone with the local wrapper), 'full' (built on the server,
/// every feature), or null if the sheet was closed without choosing.
/// Full build is recommended and pre-selected.
Future<String?> showBuildModeSheet(BuildContext context) =>
    showIosSheet<String>(context, (_) => const _BuildModeSheet());

const _ink = Color(0xFF0F2A24);

class _BuildModeSheet extends StatefulWidget {
  const _BuildModeSheet();
  @override
  State<_BuildModeSheet> createState() => _BuildModeSheetState();
}

class _BuildModeSheetState extends State<_BuildModeSheet> {
  String selected = 'full';

  void _pick(String v) {
    if (selected == v) return;
    setState(() => selected = v);
  }

  @override
  Widget build(BuildContext context) {
    final full = selected == 'full';
    return Container(
      width: double.infinity,
      padding: EdgeInsets.fromLTRB(16, 10, 16, 14 + MediaQuery.of(context).padding.bottom),
      decoration: const BoxDecoration(
        color: C.sheet,
        borderRadius: BorderRadius.vertical(top: Radius.circular(30)),
      ),
      child: Column(mainAxisSize: MainAxisSize.min, crossAxisAlignment: CrossAxisAlignment.stretch, children: [
        Center(
          child: Container(width: 38, height: 5, decoration: BoxDecoration(color: const Color(0xFF4A4A4C), borderRadius: BorderRadius.circular(3))),
        ),
        const SizedBox(height: 16),
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 4),
          child: Row(children: [
            Expanded(
              child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                Text('How should it build?', style: inter(20, w: FontWeight.w700)),
                const SizedBox(height: 1),
                Text('You can pick a different way next time.', style: inter(13, c: C.muted)),
              ]),
            ),
          ]),
        ),
        const SizedBox(height: 16),
        _ModeTile(
          selected: full,
          icon: Icons.cloud_done_rounded,
          title: 'Full build',
          badge: 'Recommended',
          subtitle: 'Built on the server with every feature',
          meta: const [(Icons.schedule_rounded, 'A few minutes'), (Icons.wifi_rounded, 'Needs internet')],
          features: const [
            ('Splash screen', true),
            ('Protection (Dex2C, strings)', true),
            ('Google sign-in & app links', true),
            ('App bundle (.aab)', true),
          ],
          onTap: () => _pick('full'),
        ),
        const SizedBox(height: 10),
        _ModeTile(
          selected: !full,
          icon: Icons.bolt_rounded,
          title: 'Quick build',
          subtitle: 'Built on your phone, ready in seconds',
          meta: const [(Icons.timer_outlined, 'Seconds'), (Icons.wifi_off_rounded, 'Works offline')],
          features: const [
            ('HTML sites & website links', true),
            ('Your icon, name & permissions', true),
            ('Splash screen, protection, sign-in', false),
          ],
          onTap: () => _pick('quick'),
        ),
        const SizedBox(height: 16),
        Pressable(
          onTap: () => Navigator.of(context, rootNavigator: true).pop(selected),
          child: Container(
            height: 56,
            alignment: Alignment.center,
            decoration: BoxDecoration(
              gradient: const LinearGradient(colors: [Color(0xFF6BE8CF), Color(0xFF4FD5A6)]),
              borderRadius: BorderRadius.circular(28),
              boxShadow: const [BoxShadow(color: Color(0x334FD5A6), blurRadius: 16, offset: Offset(0, 6))],
            ),
            child: Row(mainAxisAlignment: MainAxisAlignment.center, children: [
              Text(full ? 'Continue with Full build' : 'Continue with Quick build', style: inter(17, w: FontWeight.w700, c: _ink)),
              const SizedBox(width: 8),
              const Icon(Icons.arrow_forward_rounded, size: 20, color: _ink),
            ]),
          ),
        ),
        const SizedBox(height: 2),
        Pressable(
          onTap: () => Navigator.of(context, rootNavigator: true).pop(),
          child: SizedBox(height: 44, child: Center(child: Text('Cancel', style: inter(15, w: FontWeight.w600, c: C.muted)))),
        ),
      ]),
    );
  }
}

class _ModeTile extends StatelessWidget {
  final bool selected;
  final IconData icon;
  final String title, subtitle;
  final String? badge;
  final List<(IconData, String)> meta;
  final List<(String, bool)> features;
  final VoidCallback onTap;
  const _ModeTile({
    required this.selected,
    required this.icon,
    required this.title,
    required this.subtitle,
    required this.meta,
    required this.features,
    required this.onTap,
    this.badge,
  });

  @override
  Widget build(BuildContext context) => Pressable(
        scale: .98,
        onTap: onTap,
        child: AnimatedContainer(
          duration: const Duration(milliseconds: 240),
          curve: Curves.easeOutCubic,
          padding: const EdgeInsets.fromLTRB(14, 14, 14, 14),
          decoration: BoxDecoration(
            gradient: selected
                ? const LinearGradient(begin: Alignment.topLeft, end: Alignment.bottomRight, colors: [Color(0xFF21352F), Color(0xFF1A2623)])
                : null,
            color: selected ? null : C.field,
            borderRadius: BorderRadius.circular(22),
            border: Border.all(color: selected ? C.teal : const Color(0xFF2E2E30), width: selected ? 1.6 : 1),
          ),
          child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            Row(children: [
              AnimatedContainer(
                duration: const Duration(milliseconds: 240),
                curve: Curves.easeOutCubic,
                width: 44, height: 44,
                decoration: BoxDecoration(
                  color: selected ? C.tealBtn : C.tealDeep,
                  borderRadius: BorderRadius.circular(14),
                ),
                child: Icon(icon, size: 24, color: selected ? _ink : C.teal),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                  Row(children: [
                    Flexible(child: Text(title, style: inter(17, w: FontWeight.w700))),
                    if (badge != null) ...[
                      const SizedBox(width: 8),
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                        decoration: BoxDecoration(color: C.tealBtn, borderRadius: BorderRadius.circular(8)),
                        child: Text(badge!, style: inter(11, w: FontWeight.w700, c: _ink)),
                      ),
                    ],
                  ]),
                  const SizedBox(height: 2),
                  Text(subtitle, style: inter(13, c: C.muted, h: 1.3)),
                ]),
              ),
              const SizedBox(width: 8),
              AnimatedContainer(
                duration: const Duration(milliseconds: 240),
                curve: Curves.easeOutCubic,
                width: 24, height: 24,
                decoration: BoxDecoration(
                  shape: BoxShape.circle,
                  color: selected ? C.tealBtn : Colors.transparent,
                  border: Border.all(color: selected ? C.tealBtn : const Color(0xFF5A5A5E), width: 2),
                ),
                child: selected ? const Icon(Icons.check_rounded, size: 16, color: _ink) : null,
              ),
            ]),
            const SizedBox(height: 10),
            Wrap(spacing: 6, runSpacing: 6, children: [
              for (final m in meta)
                Container(
                  padding: const EdgeInsets.fromLTRB(8, 4, 10, 4),
                  decoration: BoxDecoration(color: const Color(0x14FFFFFF), borderRadius: BorderRadius.circular(10)),
                  child: Row(mainAxisSize: MainAxisSize.min, children: [
                    Icon(m.$1, size: 14, color: selected ? C.teal : C.muted),
                    const SizedBox(width: 5),
                    Text(m.$2, style: inter(12, w: FontWeight.w600)),
                  ]),
                ),
            ]),
            AnimatedSize(
              duration: const Duration(milliseconds: 260),
              curve: Curves.easeOutCubic,
              alignment: Alignment.topCenter,
              child: selected
                  ? Padding(
                      padding: const EdgeInsets.only(top: 12),
                      child: Column(children: [
                        Container(height: 1, color: const Color(0x14FFFFFF)),
                        const SizedBox(height: 10),
                        for (final f in features)
                          Padding(
                            padding: const EdgeInsets.symmetric(vertical: 3),
                            child: Row(children: [
                              Icon(f.$2 ? Icons.check_circle_rounded : Icons.remove_circle_outline_rounded,
                                  size: 18, color: f.$2 ? C.teal : const Color(0xFF6E6E73)),
                              const SizedBox(width: 10),
                              Expanded(
                                child: Text(f.$1,
                                    style: inter(14, c: f.$2 ? C.text : const Color(0xFF8A8A8F), w: FontWeight.w500)),
                              ),
                            ]),
                          ),
                      ]),
                    )
                  : const SizedBox(width: double.infinity),
            ),
          ]),
        ),
      );
}
