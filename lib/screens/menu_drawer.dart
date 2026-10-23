import 'package:flutter/material.dart' hide Text;
import 'package:flutter/cupertino.dart' show CupertinoPageRoute;
import '../theme.dart';
import '../anim.dart';
import '../api.dart';
import '../store.dart';
import '../widgets.dart';
import 'info_pages.dart';
import 'tour.dart';
import '../tr.dart';
import '../appearance.dart';
import 'prefs_sheets.dart';
import 'code_editor.dart';

class MenuDrawer extends StatelessWidget {
  const MenuDrawer({super.key});

  Widget _label(String t) => Padding(
        padding: const EdgeInsets.fromLTRB(4, 26, 4, 10),
        child: Text(t, style: inter(15, w: FontWeight.w500, c: C.muted).copyWith(letterSpacing: .6)),
      );

  Widget _group(List<_Item> items) => Container(
        decoration: BoxDecoration(color: C.card, borderRadius: BorderRadius.circular(22)),
        child: Column(children: [
          for (var i = 0; i < items.length; i++) ...[
            Pressable(onTap: items[i].onTap ?? () {}, scale: .985, child: _row(items[i])),
            if (i != items.length - 1)
              Padding(padding: const EdgeInsets.only(left: 76), child: Container(height: 1, color: const Color(0xFF2A2A2C))),
          ],
        ]),
      );

  Widget _row(_Item it) => SizedBox(
        height: it.value == null ? 61 : 60,
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 14),
          child: Row(children: [
            Container(
              width: 32, height: 32,
              decoration: BoxDecoration(color: it.bg ?? const Color(0xFF2A2A2C), borderRadius: BorderRadius.circular(it.bg != null ? 16 : 10)),
              child: Icon(it.icon, size: 19, color: it.bg != null ? Colors.white : C.teal),
            ),
            const SizedBox(width: 15),
            Expanded(child: Text(it.title, style: inter(19))),
            if (it.value != null) Text(it.value!, style: inter(17, c: C.muted)),
            const SizedBox(width: 10),
            const Icon(Icons.chevron_right_rounded, size: 22, color: Color(0xFF6A6A6E)),
          ]),
        ),
      );

  @override
  Widget build(BuildContext context) => AnimatedBuilder(
        animation: Listenable.merge([Lang.i.tick, Appearance.i.light]),
        builder: (context, _) => _content(context),
      );

  Widget _content(BuildContext context) {
    final w = MediaQuery.of(context).size.width * .86;
    return Container(
      width: w,
      height: double.infinity,
      decoration: const BoxDecoration(
        color: Color(0xFF121212),
        borderRadius: BorderRadius.only(topRight: Radius.circular(36), bottomRight: Radius.circular(36))),
      child: SafeArea(
        child: ListView(
          physics: const BouncingScrollPhysics(),
          padding: const EdgeInsets.fromLTRB(15, 38, 15, 30),
          children: [
            Padding(padding: const EdgeInsets.only(left: 7), child: Text('Menu', style: inter(36, w: FontWeight.w700, h: 1.1))),
            const SizedBox(height: 22),
            ValueListenableBuilder<BuildLimit?>(
              valueListenable: Store.i.limit,
              builder: (context, l, _) {
                final total = (l?.limit ?? 5).clamp(1, 12);
                final used = (l?.used ?? 0).clamp(0, total);
                final full = l != null && l.remaining <= 0;
                return Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                  Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 7),
                    child: Row(children: [
                      Text('BUILDS TODAY', style: inter(15, w: FontWeight.w500, c: C.muted).copyWith(letterSpacing: .6)),
                      const Spacer(),
                      Text(l == null ? '…' : '$used of $total', style: inter(15, w: FontWeight.w700, c: full ? const Color(0xFFF27D85) : C.teal)),
                    ]),
                  ),
                  const SizedBox(height: 10),
                  Container(
                    height: 39,
                    padding: const EdgeInsets.symmetric(horizontal: 17),
                    decoration: BoxDecoration(color: C.card, borderRadius: BorderRadius.circular(20)),
                    child: Row(children: [
                      for (var i = 0; i < total; i++) ...[
                        Expanded(
                          child: TweenAnimationBuilder<double>(
                            tween: Tween(begin: 0, end: i < used ? 1 : 0),
                            duration: Duration(milliseconds: 500 + i * 90),
                            curve: iosCurve,
                            builder: (c, v, _) => Container(
                              height: 8,
                              decoration: BoxDecoration(
                                color: Color.lerp(const Color(0xFF3A3A3C), full ? const Color(0xFFF08086) : C.teal, v),
                                borderRadius: BorderRadius.circular(4)),
                            ),
                          ),
                        ),
                        if (i != total - 1) const SizedBox(width: 4),
                      ],
                    ]),
                  ),
                  const SizedBox(height: 14),
                  Padding(
                    padding: const EdgeInsets.only(left: 7),
                    child: Text(l == null ? 'Checking today\u2019s builds…' : (full ? 'Daily limit reached · resets at midnight UTC' : '${total - used} builds left today'), style: inter(15, c: C.muted)),
                  ),
                ]);
              },
            ),
            _label('LANGUAGE'),
            _group([
              _Item(Icons.language_rounded, 'Language', value: Lang.i.name, onTap: () => showLanguageSheet(context)),
              _Item(Icons.contrast_rounded, 'Appearance', value: Appearance.i.light.value ? 'Light' : 'Dark', onTap: () => showAppearanceSheet(context)),
            ]),
            _label('GUIDE'),
            _group([
              _Item(Icons.play_circle_outline_rounded, 'Take the tour', onTap: () => showTour(context)),
              _Item(Icons.auto_awesome_outlined, 'Features', onTap: () => _open(context, Pages.features)),
              _Item(Icons.help_outline_rounded, 'How it works', onTap: () => _open(context, Pages.how)),
              _Item(Icons.quiz_outlined, 'FAQ', onTap: () => _open(context, Pages.faq)),
              _Item(Icons.build_outlined, 'Tips & troubleshooting', onTap: () => _open(context, Pages.tips)),
            ]),
            _label('LEGAL'),
            _group([
              _Item(Icons.verified_user_outlined, 'Privacy Policy', onTap: () => _open(context, Pages.privacy)),
              _Item(Icons.description_outlined, 'Terms of Service', onTap: () => _open(context, Pages.terms)),
              _Item(Icons.notifications_none_rounded, 'Reminder', onTap: () => _open(context, Pages.reminder)),
            ]),
            _label('SUPPORT'),
            _group([
              _Item(Icons.person_outline_rounded, 'About developer', onTap: () => _open(context, Pages.about)),
              _Item(Icons.flag_outlined, 'Report a problem', onTap: () => _open(context, Pages.report)),
            ]),
            _label('CONTACT'),
            _group([
              _Item(Icons.send_rounded, 'Open Telegram', bg: const Color(0xFF2AA3E0), onTap: () => openTelegram(context)),
            ]),
            const SizedBox(height: 22),
            Center(child: Text('Tycept \u00B7 WZH 2 APK', style: inter(14, c: C.muted))),
          ],
        ),
      ),
    );
  }
}

class _Item {
  final IconData icon;
  final String title;
  final String? value;
  final Color? bg;
  final VoidCallback? onTap;
  const _Item(this.icon, this.title, {this.value, this.bg, this.onTap});
}

void _open(BuildContext context, Widget page) =>
    Navigator.of(context).push(CupertinoPageRoute(builder: (_) => page));

