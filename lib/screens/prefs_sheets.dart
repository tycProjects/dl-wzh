import 'package:flutter/material.dart' hide Text;
import 'package:flutter/services.dart';
import 'package:url_launcher/url_launcher.dart';
import '../anim.dart';
import '../appearance.dart';
import '../banner.dart';
import '../config.dart';
import '../theme.dart';
import '../tr.dart';

/// Pick the dark or the light look.
Future<void> showAppearanceSheet(BuildContext context) =>
    showIosSheet<void>(context, (_) => const _AppearanceSheet());

/// Pick the language of the app.
Future<void> showLanguageSheet(BuildContext context) =>
    showIosSheet<void>(context, (_) => const _LanguageSheet());

class _SheetFrame extends StatelessWidget {
  final String title, sub;
  final IconData icon;
  final Widget body;
  /// Extra room at the bottom (the keyboard). It is padding INSIDE the sheet, so the sheet's own
  /// background reaches the bottom edge and nothing from the page behind shows through under it.
  final double extraBottom;
  const _SheetFrame({required this.title, required this.sub, required this.icon, required this.body, this.extraBottom = 0});

  @override
  Widget build(BuildContext context) => AnimatedContainer(
        duration: const Duration(milliseconds: 180),
        curve: Curves.easeOut,
        width: double.infinity,
        padding: EdgeInsets.fromLTRB(16, 10, 16, 14 + MediaQuery.of(context).padding.bottom + extraBottom),
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
                  Text(title, style: inter(20, w: FontWeight.w700)),
                  const SizedBox(height: 1),
                  Text(sub, style: inter(13, c: C.muted)),
                ]),
              ),
            ]),
          ),
          const SizedBox(height: 14),
          body,
        ]),
      );
}

class _OptionRow extends StatelessWidget {
  final IconData? icon;
  final String title;
  final String? sub;
  final bool selected, verbatim;
  final VoidCallback onTap;
  const _OptionRow({this.icon, required this.title, this.sub, required this.selected, required this.onTap, this.verbatim = false});

  @override
  Widget build(BuildContext context) => Padding(
        padding: const EdgeInsets.only(bottom: 8),
        child: Pressable(
          onTap: onTap,
          scale: .985,
          child: Container(
            height: 58,
            padding: const EdgeInsets.symmetric(horizontal: 14),
            decoration: BoxDecoration(
              color: C.card,
              borderRadius: BorderRadius.circular(16),
              border: Border.all(color: selected ? C.teal : Colors.transparent, width: 1.5),
            ),
            child: Row(children: [
              if (icon != null) ...[
                Icon(icon, size: 22, color: selected ? C.teal : C.muted),
                const SizedBox(width: 12),
              ],
              Expanded(
                child: Row(children: [
                  Flexible(child: Text(title, verbatim: verbatim, maxLines: 1, overflow: TextOverflow.ellipsis, style: inter(17, w: FontWeight.w600))),
                  if (sub != null) ...[
                    const SizedBox(width: 8),
                    Text(sub!, verbatim: verbatim, maxLines: 1, style: inter(14, c: C.muted)),
                  ],
                ]),
              ),
              if (selected) const Icon(Icons.check_rounded, size: 22, color: C.teal),
            ]),
          ),
        ),
      );
}

class _AppearanceSheet extends StatelessWidget {
  const _AppearanceSheet();

  void _pick(BuildContext context, bool light) {
    Appearance.i.set(light);
    Navigator.of(context, rootNavigator: true).pop();
  }

  @override
  Widget build(BuildContext context) => _SheetFrame(
        title: 'Appearance',
        sub: 'Pick how the app looks.',
        icon: Icons.contrast_rounded,
        body: ValueListenableBuilder<bool>(
          valueListenable: Appearance.i.light,
          builder: (context, light, _) => Column(children: [
            _OptionRow(icon: Icons.dark_mode_outlined, title: 'Dark', selected: !light, onTap: () => _pick(context, false)),
            _OptionRow(icon: Icons.light_mode_outlined, title: 'Light', selected: light, onTap: () => _pick(context, true)),
          ]),
        ),
      );
}

class _LanguageSheet extends StatefulWidget {
  const _LanguageSheet();
  @override
  State<_LanguageSheet> createState() => _LanguageSheetState();
}

class _LanguageSheetState extends State<_LanguageSheet> {
  final _ctl = TextEditingController();
  String _q = '';

  @override
  void dispose() {
    _ctl.dispose();
    super.dispose();
  }

  bool _match(AppLanguage l) {
    final q = _q.trim().toLowerCase();
    if (q.isEmpty) return true;
    return l.name.toLowerCase().contains(q) || l.english.toLowerCase().contains(q) || l.code.toLowerCase().contains(q);
  }

  @override
  Widget build(BuildContext context) {
    final kb = MediaQuery.of(context).viewInsets.bottom;
    final shown = kLanguages.where(_match).toList();
    final screenH = MediaQuery.of(context).size.height;
    return _SheetFrame(
        extraBottom: kb,
        title: 'Language',
        sub: 'Needs internet the first time you pick a language.',
        icon: Icons.language_rounded,
        body: Column(mainAxisSize: MainAxisSize.min, crossAxisAlignment: CrossAxisAlignment.stretch, children: [
          Container(
            height: 48,
            margin: const EdgeInsets.only(bottom: 10),
            padding: const EdgeInsets.symmetric(horizontal: 14),
            decoration: BoxDecoration(color: C.card, borderRadius: BorderRadius.circular(16)),
            child: Row(children: [
              const Icon(Icons.search_rounded, size: 22, color: C.muted),
              const SizedBox(width: 10),
              Expanded(
                child: TextField(
                  controller: _ctl,
                  onChanged: (v) => setState(() => _q = v),
                  style: inter(16),
                  cursorColor: C.teal,
                  textInputAction: TextInputAction.search,

                  decoration: InputDecoration(
                    border: InputBorder.none,
                    isCollapsed: true,
                    hintText: tr('Search language'),
                    hintStyle: inter(16, c: const Color(0xFF6E6E73)),
                  ),
                ),
              ),
              if (_q.isNotEmpty)
                Pressable(
                  scale: .85,
                  onTap: () {
                    _ctl.clear();
                    setState(() => _q = '');
                  },
                  child: const Padding(padding: EdgeInsets.all(4), child: Icon(Icons.cancel_rounded, size: 20, color: Color(0xFF6E6E73))),
                ),
            ]),
          ),
          ConstrainedBox(
            constraints: BoxConstraints(maxHeight: screenH * (kb > 0 ? .28 : .5)),
            child: shown.isEmpty
                ? Padding(
                    padding: const EdgeInsets.symmetric(vertical: 28),
                    child: Center(child: Text('No language found', style: inter(15, c: C.muted))),
                  )
                : ListView(
                    shrinkWrap: true,
                    keyboardDismissBehavior: ScrollViewKeyboardDismissBehavior.onDrag,
                    physics: const BouncingScrollPhysics(),
                    padding: EdgeInsets.zero,
                    children: [
                      for (final l in shown)
                        _OptionRow(
                          title: l.name,
                          sub: l.code == 'en' ? null : l.english,
                          verbatim: true,
                          selected: Lang.i.code == l.code,
                          onTap: () {
                            Lang.i.set(l.code);
                            Navigator.of(context, rootNavigator: true).pop();
                          },
                        ),
                    ],
                  ),
          ),
        ]),
    );
  }
}
