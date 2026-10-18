import 'package:flutter/material.dart';
import 'package:flutter/cupertino.dart' show CupertinoPageRoute;
import 'dart:io';
import 'dart:ui' show ImageFilter;
import 'package:flutter/services.dart' show HapticFeedback;
import 'package:file_selector/file_selector.dart';
import '../api.dart';
import '../store.dart';
import '../theme.dart';
import '../widgets.dart';
import '../anim.dart';
import 'create_sheet.dart' show showEditSourceSheet;
import 'permissions_page.dart';
import 'splash_sheet.dart';
import 'build_page.dart';

class OptionsPage extends StatefulWidget {
  final BuildRequest base;
  final String appName, source;
  const OptionsPage({super.key, required this.base, required this.appName, required this.source});
  @override
  State<OptionsPage> createState() => _OptionsPageState();
}

class _OptionsPageState extends State<OptionsPage> {
  int orient = 0;
  bool advanced = false, dex2c = false, strings = false, cache = true, signRelease = true;
  String splash = 'Off';
  String? logoPath, logoName, gsPath, gsName, splashFile, splashFileName;
  bool building = false;
  static const splashCodes = {'Off': 'off', 'Custom': 'custom', 'Native': 'native', 'iOS zoom': 'ios', 'Slide away': 'slideaway', 'Pop': 'pop'};
  final perms = defaultPerms();
  late BuildRequest base = widget.base;
  late String source = widget.source;
  late final TextEditingController name = TextEditingController(text: widget.appName);
  final suffix = TextEditingController(), links = TextEditingController(), clientId = TextEditingController(), owner = TextEditingController();
  static const hints = ['Follows how the phone is held.', 'Always upright.', 'Always sideways.'];

  String get pkg => 'com.${name.text.toLowerCase().replaceAll(RegExp(r'[^a-z0-9]'), '')}';
  int get permCount => perms.values.where((v) => v).length;

  // ---- iOS "Settings" building blocks: inset grouped lists, hairlines, section footers ----

  Widget _hdr(String t, {Widget? trailing}) => Padding(
        padding: const EdgeInsets.fromLTRB(32, 26, 20, 8),
        child: Row(mainAxisAlignment: MainAxisAlignment.spaceBetween, children: [
          Text(t.toUpperCase(), style: inter(13, c: C.muted).copyWith(letterSpacing: .3)),
          if (trailing != null) trailing,
        ]),
      );

  Widget _ftr(String t) => Padding(
        padding: const EdgeInsets.fromLTRB(32, 8, 24, 0),
        child: Text(t, style: inter(13, c: C.muted, h: 1.35)),
      );

  Widget _group(List<Widget> rows) => Container(
        decoration: BoxDecoration(color: C.card, borderRadius: BorderRadius.circular(14)),
        clipBehavior: Clip.antiAlias,
        child: Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: rows),
      );

  Widget _sep({double left = 16}) =>
      Container(height: .5, margin: EdgeInsets.only(left: left), color: const Color(0xFF3A3A3C));

  Widget get _chev => const Icon(Icons.arrow_forward_ios_rounded, size: 14, color: Color(0xFF6A6A6E));

  Widget _tile(IconData i, {bool on = true}) => Container(
        width: 30, height: 30,
        decoration: BoxDecoration(color: on ? const Color(0xFF1F3B32) : const Color(0xFF2A2A2C), borderRadius: BorderRadius.circular(8)),
        child: Icon(i, size: 18, color: on ? C.teal : const Color(0xFFB4B4B8)),
      );

  Widget _cell(IconData? icon, String title,
          {String? sub, Widget? trailing, VoidCallback? onTap, bool on = true, Color? titleColor}) =>
      _IosCell(
        onTap: onTap,
        child: ConstrainedBox(
          constraints: const BoxConstraints(minHeight: 52),
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
            child: Row(children: [
              if (icon != null) ...[_tile(icon, on: on), const SizedBox(width: 14)],
              Expanded(
                child: Column(crossAxisAlignment: CrossAxisAlignment.start, mainAxisSize: MainAxisSize.min, children: [
                  Text(title, maxLines: 1, overflow: TextOverflow.ellipsis, style: inter(17, c: titleColor ?? C.text)),
                  if (sub != null) ...[const SizedBox(height: 2), Text(sub, style: inter(13, c: C.muted, h: 1.3))],
                ]),
              ),
              if (trailing != null) ...[const SizedBox(width: 10), trailing],
            ]),
          ),
        ),
      );

  Widget _toggleCell(IconData icon, String title, String sub, bool v, ValueChanged<bool> f) =>
      _cell(icon, title, sub: sub, on: v, trailing: IosToggle(value: v, onChanged: f), onTap: () => f(!v));

  /// A text field that sits right in the list (label on top, no box) - like iOS forms.
  Widget _fieldCell(String label, TextEditingController c, String hint, {String? prefix, String? opt, String? desc}) => Padding(
        padding: const EdgeInsets.fromLTRB(16, 12, 16, 12),
        child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Row(children: [
            Text(label, style: inter(13, w: FontWeight.w600, c: C.muted)),
            if (opt != null) ...[const SizedBox(width: 6), Text(opt, style: inter(13, c: const Color(0xFF6E6E73)))],
          ]),
          const SizedBox(height: 6),
          Row(children: [
            if (prefix != null) Text(prefix, style: inter(17, c: C.muted)),
            Expanded(
              child: TextField(
                controller: c,
                scrollPadding: const EdgeInsets.fromLTRB(20, 20, 20, 120),
                onChanged: (_) => setState(() {}),
                style: inter(17),
                cursorColor: C.teal,
                decoration: InputDecoration(border: InputBorder.none, isCollapsed: true, hintText: hint, hintStyle: inter(17, c: const Color(0xFF6E6E73))),
              ),
            ),
          ]),
          if (desc != null) ...[const SizedBox(height: 6), Text(desc, style: inter(13, c: C.muted, h: 1.3))],
        ]),
      );

  Future<void> _pickLogo() async {
    final f = await openFile(acceptedTypeGroups: [const XTypeGroup(label: 'Images', extensions: ['png', 'jpg', 'jpeg', 'webp'])]);
    if (f != null) setState(() { logoPath = f.path; logoName = f.name; });
  }

  Future<void> _editSource() async {
    final r = await showEditSourceSheet(context,
        base: base, name: name.text.trim().isEmpty ? widget.appName : name.text.trim(), source: source);
    if (r == null || !mounted) return;
    setState(() {
      base = r.base;
      source = r.source;
      name.text = r.name;
    });
  }

  @override
  Widget build(BuildContext context) {
    // While the keyboard is open the form shrinks to fit above it, so the floating
    // Build APK bar and bot button must get out of the way instead of riding on top of it.
    final kbOpen = MediaQuery.of(context).viewInsets.bottom > 0;
    final shownName = name.text.trim().isEmpty ? 'Your app' : name.text.trim();
    return Scaffold(
      resizeToAvoidBottomInset: true,
      body: SafeArea(
        child: Stack(children: [
          Column(children: [
            const TopBar(title: 'App options', back: true),
            Expanded(
              child: ListView(
                physics: const BouncingScrollPhysics(parent: AlwaysScrollableScrollPhysics()),
                padding: EdgeInsets.fromLTRB(16, 14, 16, kbOpen ? 24 : 120),
                children: [
                  // ---- App summary: an app-icon tile, the name, and a tinted Edit button ----
                  _group([
                    Padding(
                      padding: const EdgeInsets.fromLTRB(14, 14, 14, 14),
                      child: Row(children: [
                        Container(
                          width: 58, height: 58,
                          alignment: Alignment.center,
                          clipBehavior: Clip.antiAlias,
                          decoration: BoxDecoration(color: const Color(0xFF1F3B32), borderRadius: BorderRadius.circular(13)),
                          child: logoPath != null
                              ? Image.file(File(logoPath!), fit: BoxFit.cover, width: 58, height: 58)
                              : Text(shownName.characters.first.toUpperCase(), style: inter(26, w: FontWeight.w700, c: C.teal)),
                        ),
                        const SizedBox(width: 14),
                        Expanded(
                          child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                            Text(shownName, style: inter(19, w: FontWeight.w700), maxLines: 1, overflow: TextOverflow.ellipsis),
                            const SizedBox(height: 2),
                            Text(source, style: inter(14, c: C.muted), maxLines: 1, overflow: TextOverflow.ellipsis),
                          ]),
                        ),
                        const SizedBox(width: 10),
                        Pressable(
                          onTap: _editSource,
                          child: Container(
                            height: 34,
                            padding: const EdgeInsets.symmetric(horizontal: 18),
                            alignment: Alignment.center,
                            decoration: BoxDecoration(color: const Color(0xFF214037), borderRadius: BorderRadius.circular(17)),
                            child: Text('Edit', style: inter(16, w: FontWeight.w600, c: C.teal)),
                          ),
                        ),
                      ]),
                    ),
                  ]),
                  _ftr('Step 2 of 2 · Everything below is optional. Tap Build when you are ready.'),

                  // ---- Orientation ----
                  _hdr('Orientation'),
                  _group([
                    Padding(
                      padding: const EdgeInsets.fromLTRB(16, 22, 16, 16),
                      child: Column(children: [
                        _OrientIcon(orient: orient),
                        const SizedBox(height: 20),
                        Seg(
                          labels: const ['Auto', 'Portrait', 'Landscape'],
                          index: orient, onChanged: (i) => setState(() => orient = i),
                          height: 36, radius: 10, pad: 2, thumbRadius: 8, fontSize: 14, bg: const Color(0xFF2C2C2E)),
                      ]),
                    ),
                  ]),
                  Padding(
                    padding: const EdgeInsets.fromLTRB(32, 8, 24, 0),
                    child: AnimatedSwitcher(
                      duration: const Duration(milliseconds: 250),
                      layoutBuilder: (cur, prev) => Stack(alignment: Alignment.centerLeft, children: [...prev, if (cur != null) cur]),
                      child: Text(hints[orient], key: ValueKey(orient), style: inter(13, c: C.muted)),
                    ),
                  ),

                  // ---- App icon ----
                  _hdr('App icon'),
                  _group([
                    _IosCell(
                      onTap: _pickLogo,
                      child: Padding(
                        padding: const EdgeInsets.all(14),
                        child: Row(children: [
                          Container(
                            width: 60, height: 60,
                            alignment: Alignment.center,
                            clipBehavior: Clip.antiAlias,
                            decoration: BoxDecoration(color: C.sheet, borderRadius: BorderRadius.circular(14)),
                            child: logoPath != null
                                ? Image.file(File(logoPath!), fit: BoxFit.cover, width: 60, height: 60)
                                : const Icon(Icons.add_photo_alternate_outlined, size: 26, color: Color(0xFFD8D8DC)),
                          ),
                          const SizedBox(width: 14),
                          Expanded(
                            child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                              Text('Custom icon', style: inter(17)),
                              const SizedBox(height: 2),
                              Text(logoName ?? 'Square image, 512×512 or larger works best.',
                                  maxLines: 2, overflow: TextOverflow.ellipsis,
                                  style: inter(13, c: logoName != null ? C.teal : C.muted, h: 1.3)),
                            ]),
                          ),
                          const SizedBox(width: 10),
                          _chev,
                        ]),
                      ),
                    ),
                  ]),

                  // ---- App details ----
                  _hdr('App details',
                      trailing: Text(pkg, style: const TextStyle(fontFamily: 'monospace', fontSize: 12.5, color: Color(0xFF8F8F94)))),
                  _group([
                    SizedBox(
                      height: 52,
                      child: Padding(
                        padding: const EdgeInsets.symmetric(horizontal: 16),
                        child: Row(children: [
                          SizedBox(width: 84, child: Text('Name', style: inter(17))),
                          Expanded(
                            child: TextField(
                              controller: name,
                              scrollPadding: const EdgeInsets.fromLTRB(20, 20, 20, 120),
                              onChanged: (_) => setState(() {}),
                              style: inter(17),
                              cursorColor: C.teal,
                              decoration: InputDecoration(border: InputBorder.none, isCollapsed: true, hintText: 'App name', hintStyle: inter(17, c: const Color(0xFF6E6E73))),
                            ),
                          ),
                        ]),
                      ),
                    ),
                    _sep(),
                    _cell(Icons.tune_rounded, 'Advanced',
                        sub: 'Only if you want it',
                        onTap: () => setState(() => advanced = !advanced),
                        trailing: AnimatedRotation(
                            turns: advanced ? .25 : 0,
                            duration: const Duration(milliseconds: 380),
                            curve: iosCurve,
                            child: _chev)),
                    AnimatedSize(
                      duration: const Duration(milliseconds: 420),
                      curve: iosCurve,
                      alignment: Alignment.topCenter,
                      child: advanced
                          ? Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
                              _sep(left: 0),
                              _fieldCell('Package suffix', suffix, 'e.g. myapp or test.build', prefix: 'com.', opt: 'optional', desc: "Your app's ID, like com.myapp."),
                              _sep(),
                              _fieldCell('App Links domain', links, 'e.g. example.com', opt: 'optional', desc: "Your site's address. Its links open in the app."),
                              _sep(),
                              _fieldCell('Google Sign-In', clientId, 'Paste Client ID, e.g. 123-abc.apps.', opt: 'optional', desc: 'Lets people sign in with Google.'),
                              _sep(),
                              _cell(Icons.upload_file_rounded, gsName ?? 'Upload google-services.json',
                                  titleColor: gsName != null ? C.teal : C.text,
                                  trailing: gsName == null ? _chev : null,
                                  onTap: () async {
                                    final f = await openFile(acceptedTypeGroups: [const XTypeGroup(label: 'JSON', extensions: ['json'])]);
                                    if (f != null) setState(() { gsPath = f.path; gsName = f.name; });
                                  }),
                            ])
                          : const SizedBox(width: double.infinity),
                    ),
                  ]),
                  const SizedBox(height: 16),
                  _group([
                    _cell(Icons.shield_outlined, 'Permissions',
                        trailing: Row(mainAxisSize: MainAxisSize.min, children: [
                          Text('$permCount selected', style: inter(16, c: C.muted)),
                          const SizedBox(width: 8),
                          _chev,
                        ]),
                        onTap: () async {
                          await Navigator.of(context).push(PermissionsPage.route(perms));
                          setState(() {});
                        }),
                  ]),

                  // ---- Protection ----
                  _hdr('Protection'),
                  _group([
                    _toggleCell(Icons.gpp_good_outlined, 'Dex2C protection', 'Harder to decompile or clone', dex2c, (v) => setState(() => dex2c = v)),
                    _sep(left: 60),
                    _toggleCell(Icons.lock_outline_rounded, 'String protection', 'Hides strings from a decompiled APK', strings, (v) => setState(() => strings = v)),
                  ]),

                  // ---- Performance ----
                  _hdr('Performance'),
                  _group([
                    _toggleCell(Icons.bolt_rounded, 'Cache pages offline', 'Works without a connection after first launch', cache, (v) => setState(() => cache = v)),
                  ]),

                  // ---- Startup loading ----
                  _hdr('Startup loading'),
                  _group([
                    _IosCell(
                      onTap: () async {
                        final r = await showIosSheet<String>(context, (_) => SplashSheet(selected: splash));
                        if (r != null) setState(() => splash = r);
                      },
                      child: Padding(
                        padding: const EdgeInsets.all(14),
                        child: Row(children: [
                          Container(
                            width: 54, height: 42,
                            decoration: BoxDecoration(color: const Color(0xFF0A0A0A), borderRadius: BorderRadius.circular(10)),
                            child: Center(child: Container(width: 17, height: 28, decoration: BoxDecoration(border: Border.all(color: const Color(0xFFB4B4B8), width: 2), borderRadius: BorderRadius.circular(5)))),
                          ),
                          const SizedBox(width: 14),
                          Expanded(
                            child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                              Text(splash, style: inter(17)),
                              const SizedBox(height: 2),
                              Text(splashInfo[splash]!, style: inter(13, c: C.muted, h: 1.3)),
                            ]),
                          ),
                          const SizedBox(width: 10),
                          _chev,
                        ]),
                      ),
                    ),
                    if (splash == 'Custom') ...[
                      _sep(),
                      _cell(Icons.movie_outlined, splashFileName ?? 'Choose your video or image',
                          titleColor: splashFileName != null ? C.teal : C.text,
                          onTap: () async {
                            final f = await openFile(acceptedTypeGroups: [const XTypeGroup(label: 'Video or image', extensions: ['mp4', 'webm', 'mov', 'gif', 'png', 'jpg', 'jpeg', 'webp'])]);
                            if (f != null) setState(() { splashFile = f.path; splashFileName = f.name; });
                          }),
                    ],
                    _sep(),
                    _cell(Icons.play_arrow_rounded, 'Test splash', titleColor: C.teal, onTap: () {}),
                    _sep(left: 60),
                    _cell(Icons.refresh_rounded, 'Reset to default', titleColor: C.teal, onTap: () => setState(() => splash = 'Off')),
                  ]),

                  // ---- Signing ----
                  _hdr('Signing'),
                  _group([
                    _toggleCell(Icons.vpn_key_outlined, 'Sign as release build', 'Installable outside developer mode', signRelease, (v) => setState(() => signRelease = v)),
                    _sep(left: 60),
                    _cell(Icons.key_rounded, 'Your signing key', sub: 'Create, import or back up', trailing: _chev, onTap: () {}),
                    _sep(),
                    _fieldCell('Owner key', owner, "You're not Tycept, leave this blank"),
                  ]),
                  _ftr('Owner key is for the Tycept team only.'),
                ],
              ),
            ),
          ]),
          if (!kbOpen)
            Positioned(
              right: 17, bottom: 92,
              child: ValueListenableBuilder<bool>(
                valueListenable: Store.i.botHidden,
                builder: (_, hidden, __) => IgnorePointer(
                  ignoring: hidden,
                  child: AnimatedScale(
                    scale: hidden ? 0 : 1,
                    duration: const Duration(milliseconds: 380),
                    curve: iosCurve,
                    child: AnimatedOpacity(opacity: hidden ? 0 : 1, duration: const Duration(milliseconds: 220), child: const BotFab()),
                  ),
                ),
              ),
            ),
          // ---- Frosted bottom bar with the Build button (iOS toolbar style) ----
          if (!kbOpen)
            Positioned(
              left: 0, right: 0, bottom: 0,
              child: ClipRect(
                child: BackdropFilter(
                  filter: ImageFilter.blur(sigmaX: 18, sigmaY: 18),
                  child: Container(
                    padding: const EdgeInsets.fromLTRB(20, 12, 20, 12),
                    decoration: BoxDecoration(
                      color: C.bg.withOpacity(.78),
                      border: const Border(top: BorderSide(color: Color(0xFF2A2A2C), width: .5)),
                    ),
                    child: Pressable(
                      scale: .975,
                      onTap: building ? null : _build,
                      child: AnimatedContainer(
                        duration: const Duration(milliseconds: 250),
                        height: 54,
                        alignment: Alignment.center,
                        decoration: BoxDecoration(color: building ? const Color(0xFF3D7367) : C.tealBtn, borderRadius: BorderRadius.circular(16)),
                        child: building
                            ? const IosSpinner(size: 26, color: Color(0xFF0F2A24))
                            : Text('Build APK', style: inter(20, w: FontWeight.w700, c: const Color(0xFF0F2A24))),
                      ),
                    ),
                  ),
                ),
              ),
            ),
        ]),
      ),
    );
  }

  Future<void> _build() async {
    final nav = Navigator.of(context);
    final req = base
      ..appName = name.text.trim()
      ..orientation = const ['auto', 'portrait', 'landscape'][orient]
      ..signRelease = signRelease
      ..dex2c = dex2c
      ..stringProtect = strings
      ..cacheMode = cache ? 'fast' : 'fresh'
      ..splashStyle = splashCodes[splash]!
      ..permissions = selectedPerms(perms)
      ..packageSuffix = suffix.text.trim()
      ..appLinkHost = links.text.trim()
      ..googleWebClientId = clientId.text.trim()
      ..ownerKey = owner.text
      ..logoPath = logoPath
      ..googleServicesPath = gsPath
      ..customSplashPath = splashFile;
    if (splash == 'Custom' && splashFile == null) {
      showInfoDialog(context, title: 'Add your startup file', message: 'You picked a custom startup screen. Choose the video or image to play first.', icon: Icons.movie_outlined);
      return;
    }
    setState(() => building = true);
    try {
      final lim = await Api.i.limit(ownerKey: req.ownerKey);
      if (lim != null) Store.i.limit.value = lim;
      if (lim != null && lim.remaining <= 0) {
        if (mounted) _limitPopup(lim);
        return;
      }
      final id = await Api.i.startBuild(req);
      await Store.i.setActiveJob(id);
      Store.i.refreshLimit(ownerKey: req.ownerKey);
      Store.i.refreshHistory();
      if (!mounted) return;
      nav.push(CupertinoPageRoute(builder: (_) => BuildPage(jobId: id, appName: name.text.trim().isEmpty ? widget.appName : name.text.trim())));
    } on ApiError catch (e) {
      if (!mounted) return;
      if (e.status == 429) {
        final lim = await Api.i.limit(ownerKey: req.ownerKey);
        if (lim != null) Store.i.limit.value = lim;
        if (mounted) {
          if (lim != null) {
            _limitPopup(lim);
          } else {
            showInfoDialog(context, title: 'Out of builds for today', message: e.message, icon: Icons.hourglass_bottom_rounded, color: C.red);
          }
        }
      } else if (e.status == 503 && e.code == 'server_busy') {
        showInfoDialog(context, title: 'Server is busy', message: 'Nothing was started and no build was used. Try again in about ${e.retryAfter} seconds.', icon: Icons.hourglass_top_rounded, color: C.red);
      } else {
        showInfoDialog(context, title: 'Couldn\u2019t start the build', message: e.message, icon: Icons.error_outline_rounded, color: C.red);
      }
    } catch (e) {
      if (mounted) showInfoDialog(context, title: 'Couldn\u2019t reach the server', message: 'Check your connection and try again.', icon: Icons.wifi_off_rounded, color: C.red);
    } finally {
      if (mounted) setState(() => building = false);
    }
  }

  void _limitPopup(BuildLimit l) => showInfoDialog(context,
      title: 'Out of builds for today',
      message: 'You used all ${l.limit} builds today. They reset at midnight UTC.',
      icon: Icons.hourglass_bottom_rounded, color: C.red);
}

class _OrientIcon extends StatelessWidget {
  final int orient;
  const _OrientIcon({required this.orient});
  @override
  Widget build(BuildContext context) {
    final portrait = orient == 1;
    return AnimatedContainer(
      duration: const Duration(milliseconds: 420),
      curve: iosCurve,
      width: portrait ? 50 : 93, height: portrait ? 80 : 50,
      padding: portrait ? const EdgeInsets.fromLTRB(7, 14, 7, 0) : const EdgeInsets.fromLTRB(16, 7, 9, 0),
      decoration: BoxDecoration(border: Border.all(color: C.teal, width: 3), borderRadius: BorderRadius.circular(14)),
      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        _bar(portrait ? 28 : 48, const Color(0xFF555555)), const SizedBox(height: 5),
        _bar(portrait ? 36 : 66, C.teal), const SizedBox(height: 5),
        _bar(portrait ? 22 : 36, const Color(0xFF555555)),
      ]),
    );
  }

  Widget _bar(double w, Color c) => Container(width: w, height: 6, decoration: BoxDecoration(color: c, borderRadius: BorderRadius.circular(3)));
}

/// A list row with the iOS press feedback: the whole row lights up while the finger is down.
class _IosCell extends StatefulWidget {
  final Widget child;
  final VoidCallback? onTap;
  const _IosCell({required this.child, this.onTap});
  @override
  State<_IosCell> createState() => _IosCellState();
}

class _IosCellState extends State<_IosCell> {
  bool down = false;
  @override
  Widget build(BuildContext context) {
    final on = widget.onTap != null;
    return GestureDetector(
      behavior: HitTestBehavior.opaque,
      onTapDown: on ? (_) => setState(() => down = true) : null,
      onTapUp: on ? (_) => setState(() => down = false) : null,
      onTapCancel: on ? () => setState(() => down = false) : null,
      onTap: on ? () { HapticFeedback.selectionClick(); widget.onTap!(); } : null,
      child: AnimatedContainer(
        duration: Duration(milliseconds: down ? 0 : 260),
        color: down ? const Color(0xFF2C2C2E) : Colors.transparent,
        child: widget.child,
      ),
    );
  }
}
