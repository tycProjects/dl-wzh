import 'package:flutter/material.dart' hide Text;
import '../anim.dart';
import '../theme.dart';
import '../tr.dart';

/// Asked when you tap Build in the code editor: Android or Flutter.
/// Returns 'android', 'flutter', or null if the sheet was closed without choosing.
/// A target that doesn't fit the open project is greyed out with the reason underneath.
Future<String?> showBuildTargetSheet(
  BuildContext context, {
  required bool androidOk,
  required bool flutterOk,
  required String androidNote,
  required String flutterNote,
  required String initial,
}) =>
    showIosSheet<String>(
      context,
      (_) => _BuildTargetSheet(androidOk: androidOk, flutterOk: flutterOk, androidNote: androidNote, flutterNote: flutterNote, initial: initial),
    );

const _ink = Color(0xFF0F2A24);

class _BuildTargetSheet extends StatefulWidget {
  final bool androidOk, flutterOk;
  final String androidNote, flutterNote, initial;
  const _BuildTargetSheet({required this.androidOk, required this.flutterOk, required this.androidNote, required this.flutterNote, required this.initial});
  @override
  State<_BuildTargetSheet> createState() => _BuildTargetSheetState();
}

class _BuildTargetSheetState extends State<_BuildTargetSheet> {
  late String selected = widget.initial;

  @override
  Widget build(BuildContext context) {
    final ok = selected == 'android' ? widget.androidOk : widget.flutterOk;
    return Container(
      width: double.infinity,
      padding: EdgeInsets.fromLTRB(16, 10, 16, 14 + MediaQuery.of(context).padding.bottom),
      decoration: const BoxDecoration(color: C.sheet, borderRadius: BorderRadius.vertical(top: Radius.circular(30))),
      child: Column(mainAxisSize: MainAxisSize.min, crossAxisAlignment: CrossAxisAlignment.stretch, children: [
        Center(child: Container(width: 38, height: 5, decoration: BoxDecoration(color: const Color(0xFF4A4A4C), borderRadius: BorderRadius.circular(3)))),
        const SizedBox(height: 16),
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 4),
          child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            Text('Build as', style: inter(20, w: FontWeight.w700)),
            const SizedBox(height: 1),
            Text('Pick what to make from this project.', style: inter(13, c: C.muted)),
          ]),
        ),
        const SizedBox(height: 16),
        _Tile(
          selected: selected == 'android',
          enabled: widget.androidOk,
          icon: Icons.android_rounded,
          title: 'Android',
          subtitle: 'Kotlin, Java or a website (HTML, CSS, JS)',
          note: widget.androidNote,
          onTap: () => setState(() => selected = 'android'),
        ),
        const SizedBox(height: 10),
        _Tile(
          selected: selected == 'flutter',
          enabled: widget.flutterOk,
          icon: Icons.flutter_dash_rounded,
          title: 'Flutter',
          subtitle: 'A Flutter project with a pubspec.yaml',
          note: widget.flutterNote,
          onTap: () => setState(() => selected = 'flutter'),
        ),
        const SizedBox(height: 16),
        Opacity(
          opacity: ok ? 1 : .4,
          child: Pressable(
            onTap: () {
              if (ok) Navigator.of(context, rootNavigator: true).pop(selected);
            },
            child: Container(
              height: 56,
              alignment: Alignment.center,
              decoration: BoxDecoration(
                gradient: const LinearGradient(colors: [Color(0xFF6BE8CF), Color(0xFF4FD5A6)]),
                borderRadius: BorderRadius.circular(28),
              ),
              child: Row(mainAxisAlignment: MainAxisAlignment.center, children: [
                Text(selected == 'android' ? 'Continue with Android' : 'Continue with Flutter', style: inter(17, w: FontWeight.w700, c: _ink)),
                const SizedBox(width: 8),
                const Icon(Icons.arrow_forward_rounded, size: 20, color: _ink),
              ]),
            ),
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

class _Tile extends StatelessWidget {
  final bool selected, enabled;
  final IconData icon;
  final String title, subtitle, note;
  final VoidCallback onTap;
  const _Tile({required this.selected, required this.enabled, required this.icon, required this.title, required this.subtitle, required this.note, required this.onTap});

  @override
  Widget build(BuildContext context) {
    final on = selected && enabled;
    return Pressable(
      scale: .98,
      onTap: enabled ? onTap : () {},
      child: Opacity(
        opacity: enabled ? 1 : .55,
        child: AnimatedContainer(
          duration: const Duration(milliseconds: 240),
          curve: Curves.easeOutCubic,
          padding: const EdgeInsets.all(14),
          decoration: BoxDecoration(
            gradient: on ? const LinearGradient(begin: Alignment.topLeft, end: Alignment.bottomRight, colors: [Color(0xFF21352F), Color(0xFF1A2623)]) : null,
            color: on ? null : C.field,
            borderRadius: BorderRadius.circular(22),
            border: Border.all(color: on ? C.teal : const Color(0xFF2E2E30), width: on ? 1.6 : 1),
          ),
          child: Row(children: [
            Container(
              width: 44, height: 44,
              decoration: BoxDecoration(color: on ? C.tealBtn : C.tealDeep, borderRadius: BorderRadius.circular(14)),
              child: Icon(icon, size: 24, color: on ? _ink : C.teal),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                Text(title, style: inter(17, w: FontWeight.w700)),
                const SizedBox(height: 2),
                Text(subtitle, style: inter(13, c: C.muted, h: 1.3)),
                if (note.isNotEmpty) ...[
                  const SizedBox(height: 4),
                  Text(note, style: inter(12.5, w: FontWeight.w600, c: enabled ? C.teal : const Color(0xFFF5A524), h: 1.3)),
                ],
              ]),
            ),
            const SizedBox(width: 8),
            Container(
              width: 24, height: 24,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                color: on ? C.tealBtn : Colors.transparent,
                border: Border.all(color: on ? C.tealBtn : const Color(0xFF5A5A5E), width: 2),
              ),
              child: on ? const Icon(Icons.check_rounded, size: 16, color: _ink) : null,
            ),
          ]),
        ),
      ),
    );
  }
}
