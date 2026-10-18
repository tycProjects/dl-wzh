import 'package:flutter/material.dart';
import '../anim.dart';
import '../theme.dart';

/// Asked when you tap Continue on an HTML or website build.
/// Returns 'quick' (built on the phone with the local wrapper), 'full' (built on the server,
/// every feature), or null if the sheet was closed without choosing.
/// Full build is recommended and pre-selected.
Future<String?> showBuildModeSheet(BuildContext context) =>
    showIosSheet<String>(context, (_) => const _BuildModeSheet());

class _BuildModeSheet extends StatefulWidget {
  const _BuildModeSheet();
  @override
  State<_BuildModeSheet> createState() => _BuildModeSheetState();
}

class _BuildModeSheetState extends State<_BuildModeSheet> {
  String selected = 'full';

  @override
  Widget build(BuildContext context) {
    final full = selected == 'full';
    return Container(
      width: double.infinity,
      padding: EdgeInsets.fromLTRB(18, 10, 18, 18 + MediaQuery.of(context).padding.bottom),
      decoration: const BoxDecoration(color: C.sheet, borderRadius: BorderRadius.vertical(top: Radius.circular(28))),
      child: Column(mainAxisSize: MainAxisSize.min, crossAxisAlignment: CrossAxisAlignment.stretch, children: [
        Center(
          child: Container(width: 38, height: 5, decoration: BoxDecoration(color: const Color(0xFF4A4A4C), borderRadius: BorderRadius.circular(3))),
        ),
        const SizedBox(height: 18),
        Text('How should it build?', textAlign: TextAlign.center, style: inter(22, w: FontWeight.w700)),
        const SizedBox(height: 4),
        Text('You can pick a different way next time.', textAlign: TextAlign.center, style: inter(14, c: C.muted)),
        const SizedBox(height: 20),
        _ModeCard(
          selected: full,
          icon: Icons.cloud_done_rounded,
          title: 'Full build',
          badge: 'Recommended',
          text: 'Built on the server with every feature.',
          chips: const ['Protection', 'Splash screen', 'App bundle', 'Google sign-in'],
          onTap: () => setState(() => selected = 'full'),
        ),
        const SizedBox(height: 12),
        _ModeCard(
          selected: !full,
          icon: Icons.bolt_rounded,
          title: 'Quick build',
          badge: null,
          text: 'Built on your phone in seconds, no internet needed.',
          chips: const ['Offline', 'HTML & websites'],
          onTap: () => setState(() => selected = 'quick'),
        ),
        const SizedBox(height: 20),
        Pressable(
          onTap: () => Navigator.of(context, rootNavigator: true).pop(selected),
          child: Container(
            height: 54, alignment: Alignment.center,
            decoration: BoxDecoration(color: C.tealBtn, borderRadius: BorderRadius.circular(27)),
            child: Text(full ? 'Continue with Full build' : 'Continue with Quick build',
                style: inter(17, w: FontWeight.w700, c: const Color(0xFF0F2A24))),
          ),
        ),
        const SizedBox(height: 6),
        Pressable(
          onTap: () => Navigator.of(context, rootNavigator: true).pop(),
          child: SizedBox(height: 44, child: Center(child: Text('Cancel', style: inter(16, w: FontWeight.w600, c: C.muted)))),
        ),
      ]),
    );
  }
}

class _ModeCard extends StatelessWidget {
  final bool selected;
  final IconData icon;
  final String title, text;
  final String? badge;
  final List<String> chips;
  final VoidCallback onTap;
  const _ModeCard({
    required this.selected,
    required this.icon,
    required this.title,
    required this.text,
    required this.chips,
    required this.onTap,
    this.badge,
  });

  @override
  Widget build(BuildContext context) => Pressable(
        scale: .98,
        onTap: onTap,
        child: AnimatedContainer(
          duration: const Duration(milliseconds: 220),
          curve: Curves.easeOutCubic,
          padding: const EdgeInsets.all(16),
          decoration: BoxDecoration(
            color: selected ? const Color(0xFF1F2B28) : C.field,
            borderRadius: BorderRadius.circular(22),
            border: Border.all(color: selected ? C.teal : Colors.transparent, width: 2),
          ),
          child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            Row(children: [
              Container(
                width: 46, height: 46,
                decoration: BoxDecoration(color: selected ? C.tealBtn : C.tealDeep, borderRadius: BorderRadius.circular(14)),
                child: Icon(icon, size: 26, color: selected ? const Color(0xFF0F2A24) : C.teal),
              ),
              const SizedBox(width: 14),
              Expanded(
                child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                  Text(title, style: inter(19, w: FontWeight.w700)),
                  if (badge != null) ...[
                    const SizedBox(height: 4),
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 9, vertical: 3),
                      decoration: BoxDecoration(color: C.tealBtn, borderRadius: BorderRadius.circular(10)),
                      child: Text(badge!, style: inter(12, w: FontWeight.w700, c: const Color(0xFF0F2A24))),
                    ),
                  ],
                ]),
              ),
              AnimatedContainer(
                duration: const Duration(milliseconds: 220),
                curve: Curves.easeOutCubic,
                width: 26, height: 26,
                decoration: BoxDecoration(
                  shape: BoxShape.circle,
                  color: selected ? C.tealBtn : Colors.transparent,
                  border: Border.all(color: selected ? C.tealBtn : const Color(0xFF5A5A5E), width: 2),
                ),
                child: selected ? const Icon(Icons.check_rounded, size: 18, color: Color(0xFF0F2A24)) : null,
              ),
            ]),
            const SizedBox(height: 12),
            Text(text, style: inter(14, c: C.muted, h: 1.35)),
            const SizedBox(height: 10),
            Wrap(spacing: 6, runSpacing: 6, children: [
              for (final c in chips)
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
                  decoration: BoxDecoration(color: const Color(0x1AFFFFFF), borderRadius: BorderRadius.circular(10)),
                  child: Text(c, style: inter(12, w: FontWeight.w600, c: C.text)),
                ),
            ]),
          ]),
        ),
      );
}
