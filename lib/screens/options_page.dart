import 'package:flutter/material.dart';
import 'package:flutter/cupertino.dart' show CupertinoPageRoute;
import 'dart:io';
import 'package:file_selector/file_selector.dart';
import '../api.dart';
import '../store.dart';
import '../theme.dart';
import '../widgets.dart';
import '../anim.dart';
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
  late final TextEditingController name = TextEditingController(text: widget.appName);
  final suffix = TextEditingController(), links = TextEditingController(), clientId = TextEditingController(), owner = TextEditingController();
  static const hints = ['Follows how the phone is held.', 'Always upright.', 'Always sideways.'];

  String get pkg => 'com.${name.text.toLowerCase().replaceAll(RegExp(r'[^a-z0-9]'), '')}';
  int get permCount => perms.values.where((v) => v).length;

  BoxDecoration get _card => BoxDecoration(
      color: C.card, borderRadius: BorderRadius.circular(26), border: Border.all(color: const Color(0xFF2B2B2D)));

  Widget _label(String t) => Padding(
        padding: const EdgeInsets.fromLTRB(18, 28, 18, 10),
        child: Text(t, style: inter(15, w: FontWeight.w600, c: const Color(0xFFA8A8AD))),
      );

  Widget _tile(IconData i, {bool on = false, double size = 35}) => Container(
        width: size, height: size,
        decoration: BoxDecoration(color: on ? const Color(0xFF1F3B32) : const Color(0xFF2A2A2C), borderRadius: BorderRadius.circular(12)),
        child: Icon(i, size: size * .5, color: on ? C.teal : const Color(0xFFB4B4B8)),
      );

  Widget _row(IconData icon, String title, String sub, {Widget? trailing, VoidCallback? onTap, bool on = false}) => Pressable(
        onTap: onTap, scale: .985,
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 18, vertical: 14),
          child: Row(children: [
            _tile(icon, on: on),
            const SizedBox(width: 16),
            Expanded(
              child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                Text(title, style: inter(17, w: FontWeight.w500)),
                const SizedBox(height: 3),
                Text(sub, style: inter(14.5, c: C.muted, h: 1.3)),
              ]),
            ),
            if (trailing != null) ...[const SizedBox(width: 12), trailing],
          ]),
        ),
      );

  Widget _toggleRow(IconData icon, String title, String sub, bool v, ValueChanged<bool> f) =>
      _row(icon, title, sub, on: v, trailing: IosToggle(value: v, onChanged: f), onTap: () => f(!v));

  Widget _div({double left = 18}) => Padding(padding: EdgeInsets.only(left: left, right: 0), child: const Divider(height: 1, thickness: 1, color: C.line));

  Widget _input(TextEditingController c, String hint, {String? prefix}) => Container(
        height: 51,
        padding: const EdgeInsets.symmetric(horizontal: 16),
        decoration: BoxDecoration(color: C.sheet, borderRadius: BorderRadius.circular(18), border: Border.all(color: const Color(0xFF333333))),
        child: Row(children: [
          if (prefix != null) Padding(padding: const EdgeInsets.only(right: 26), child: Text(prefix, style: inter(17, w: FontWeight.w600, c: const Color(0xFFB4B4B8)))),
          Expanded(
            child: TextField(
              controller: c,
              onChanged: (_) => setState(() {}),
              style: inter(17),
              cursorColor: C.teal,
              decoration: InputDecoration(border: InputBorder.none, isCollapsed: true, hintText: hint, hintStyle: inter(17, c: const Color(0xFF6E6E73))),
            ),
          ),
        ]),
      );

  Widget _field(String label, {String? opt, bool info = false, String? hint, required Widget input, String? foot}) => Padding(
        padding: const EdgeInsets.fromLTRB(18, 22, 18, 0),
        child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Row(children: [
            Text(label, style: inter(15, w: FontWeight.w600, c: const Color(0xFFD0D0D4))),
            if (opt != null) ...[const SizedBox(width: 8), Text(opt, style: inter(15, c: C.muted))],
            if (info) ...[const SizedBox(width: 10), const Icon(Icons.info_outline_rounded, size: 18, color: C.muted)],
          ]),
          if (hint != null) ...[const SizedBox(height: 8), Text(hint, style: inter(15, c: C.muted))],
          const SizedBox(height: 10),
          input,
          if (foot != null) ...[const SizedBox(height: 10), Text(foot, style: inter(15, c: C.muted))],
        ]),
      );

  Widget _btn(IconData i, String t, VoidCallback f) => Expanded(
        child: Pressable(
          onTap: f,
          child: Container(
            height: 45,
            decoration: BoxDecoration(color: const Color(0xFF2A2A2C), borderRadius: BorderRadius.circular(18)),
            child: Row(mainAxisAlignment: MainAxisAlignment.center, children: [
              Icon(i, size: 18, color: C.text),
              const SizedBox(width: 9),
              Text(t, style: inter(16, w: FontWeight.w500)),
            ]),
          ),
        ),
      );

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: Stack(children: [
          Column(children: [
            const TopBar(title: 'App options', back: true),
            Expanded(
              child: ListView(
                physics: const BouncingScrollPhysics(parent: AlwaysScrollableScrollPhysics()),
                padding: const EdgeInsets.fromLTRB(17, 6, 17, 120),
                children: [
                  Container(
                    height: 77,
                    padding: const EdgeInsets.fromLTRB(33, 0, 16, 0),
                    decoration: BoxDecoration(color: C.card, borderRadius: BorderRadius.circular(22)),
                    child: Row(children: [
                      Expanded(
                        child: Column(mainAxisAlignment: MainAxisAlignment.center, crossAxisAlignment: CrossAxisAlignment.start, children: [
                          Text(name.text, style: inter(20, w: FontWeight.w700), overflow: TextOverflow.ellipsis),
                          const SizedBox(height: 4),
                          Text(widget.source, style: inter(15, c: C.muted), overflow: TextOverflow.ellipsis),
                        ]),
                      ),
                      Pressable(
                        onTap: () => Navigator.pop(context),
                        child: Container(
                          height: 40,
                          padding: const EdgeInsets.symmetric(horizontal: 22),
                          alignment: Alignment.center,
                          decoration: BoxDecoration(color: const Color(0xFF214037), borderRadius: BorderRadius.circular(16)),
                          child: Text('Edit', style: inter(17, w: FontWeight.w700, c: C.teal)),
                        ),
                      ),
                    ]),
                  ),
                  Padding(
                    padding: const EdgeInsets.fromLTRB(4, 14, 4, 0),
                    child: Text('Step 2 of 2 · Everything below is optional. Tap Build when you are ready.', style: inter(15, c: C.muted, h: 1.35)),
                  ),
                  _label('Orientation'),
                  Container(
                    height: 229,
                    padding: const EdgeInsets.fromLTRB(17, 38, 17, 0),
                    decoration: _card.copyWith(gradient: const LinearGradient(begin: Alignment.topCenter, end: Alignment.bottomCenter, colors: [Color(0xFF1D2A27), C.card], stops: [0, .55])),
                    child: Column(children: [
                      _OrientIcon(orient: orient),
                      const SizedBox(height: 30),
                      Seg(
                        labels: const ['Auto', 'Portrait', 'Landscape'],
                        icons: const [Icons.sync_rounded, Icons.stay_current_portrait_rounded, Icons.stay_current_landscape_rounded],
                        index: orient, onChanged: (i) => setState(() => orient = i),
                        height: 54, radius: 22, bordered: true, bg: const Color(0xFF242424)),
                      const SizedBox(height: 12),
                      AnimatedSwitcher(duration: const Duration(milliseconds: 250), child: Text(hints[orient], key: ValueKey(orient), style: inter(15, c: C.muted))),
                    ]),
                  ),
                  const SizedBox(height: 16),
                  Pressable(
                    scale: .985,
                    onTap: () async {
                      final f = await openFile(acceptedTypeGroups: [const XTypeGroup(label: 'Images', extensions: ['png', 'jpg', 'jpeg', 'webp'])]);
                      if (f != null) setState(() { logoPath = f.path; logoName = f.name; });
                    },
                    child: Container(
                      height: 102,
                      padding: const EdgeInsets.symmetric(horizontal: 18),
                      decoration: _card,
                      child: Row(children: [
                        Container(
                          width: 64, height: 64,
                          decoration: BoxDecoration(color: C.sheet, borderRadius: BorderRadius.circular(20)),
                          clipBehavior: Clip.antiAlias,
                          child: logoPath != null
                              ? Image.file(File(logoPath!), fit: BoxFit.cover, width: 64, height: 64)
                              : Column(mainAxisAlignment: MainAxisAlignment.center, children: [
                                  const Icon(Icons.file_upload_outlined, size: 20, color: Color(0xFFD8D8DC)),
                                  Text('Tap to\nupload', textAlign: TextAlign.center, style: inter(12, c: const Color(0xFFD8D8DC), h: 1.3)),
                                ]),
                        ),
                        const SizedBox(width: 20),
                        Expanded(
                          child: Column(mainAxisAlignment: MainAxisAlignment.center, crossAxisAlignment: CrossAxisAlignment.start, children: [
                            Text('Custom icon', style: inter(20, w: FontWeight.w600)),
                            const SizedBox(height: 4),
                            Text(logoName ?? 'Square image, 512×512 or larger works best.', maxLines: 2, overflow: TextOverflow.ellipsis, style: inter(15, c: logoName != null ? C.teal : C.muted, h: 1.3)),
                          ]),
                        ),
                      ]),
                    ),
                  ),
                  Padding(
                    padding: const EdgeInsets.fromLTRB(18, 28, 18, 8),
                    child: Row(mainAxisAlignment: MainAxisAlignment.spaceBetween, children: [
                      Text('App details', style: inter(15, w: FontWeight.w600, c: const Color(0xFFA8A8AD))),
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                        decoration: BoxDecoration(color: C.sheet, borderRadius: BorderRadius.circular(12)),
                        child: Text(pkg, style: const TextStyle(fontFamily: 'monospace', fontSize: 14, color: Color(0xFFD0D0D4))),
                      ),
                    ]),
                  ),
                  Container(
                    decoration: _card,
                    clipBehavior: Clip.antiAlias,
                    child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                      Padding(
                        padding: const EdgeInsets.fromLTRB(17, 16, 17, 16),
                        child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                          Text('App name', style: inter(15, c: C.muted)),
                          const SizedBox(height: 8),
                          _input(name, ''),
                        ]),
                      ),
                      _div(left: 0),
                      _row(Icons.tune_rounded, 'Advanced', 'Only if you want it',
                          onTap: () => setState(() => advanced = !advanced),
                          trailing: AnimatedRotation(turns: advanced ? .5 : 0, duration: const Duration(milliseconds: 380), curve: iosCurve,
                              child: const Icon(Icons.keyboard_arrow_down_rounded, size: 24, color: C.muted))),
                      AnimatedSize(
                        duration: const Duration(milliseconds: 420),
                        curve: iosCurve,
                        alignment: Alignment.topCenter,
                        child: advanced
                            ? Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                                _div(left: 0),
                                _field('Package suffix', opt: 'optional', input: _input(suffix, 'e.g. myapp or test.build', prefix: 'com.'), foot: "Your app's ID, like com.myapp."),
                                const SizedBox(height: 22),
                                _div(left: 18),
                                _field('App Links domain', opt: 'optional', info: true, hint: "Your site's address. Its links open in the app.", input: _input(links, 'e.g. example.com')),
                                const SizedBox(height: 22),
                                _div(left: 18),
                                _field('Google Sign-In', opt: 'optional', info: true, hint: 'Lets people sign in with Google.',
                                    input: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                                      Pressable(
                                        onTap: () async {
                                          final f = await openFile(acceptedTypeGroups: [const XTypeGroup(label: 'JSON', extensions: ['json'])]);
                                          if (f != null) setState(() { gsPath = f.path; gsName = f.name; });
                                        },
                                        child: Container(
                                          height: 43,
                                          padding: const EdgeInsets.symmetric(horizontal: 16),
                                          decoration: BoxDecoration(color: const Color(0xFF2A2A2C), borderRadius: BorderRadius.circular(16)),
                                          child: Row(mainAxisSize: MainAxisSize.min, children: [Text(gsName ?? 'Upload google-services.json', style: inter(16, w: FontWeight.w500, c: gsName != null ? C.teal : C.text))]),
                                        ),
                                      ),
                                      const SizedBox(height: 10),
                                      _input(clientId, 'or paste Client ID, e.g. 123-abc.apps.'),
                                    ])),
                                const SizedBox(height: 26),
                              ])
                            : const SizedBox(width: double.infinity),
                      ),
                    ]),
                  ),
                  const SizedBox(height: 16),
                  Container(
                    decoration: _card,
                    child: _row(Icons.shield_outlined, 'Permissions', '$permCount selected',
                        trailing: const Icon(Icons.chevron_right_rounded, size: 24, color: Color(0xFF6A6A6E)),
                        onTap: () async {
                          await Navigator.of(context).push(PermissionsPage.route(perms));
                          setState(() {});
                        }),
                  ),
                  _label('Protection'),
                  Container(
                    decoration: _card,
                    child: Column(children: [
                      _toggleRow(Icons.gpp_good_outlined, 'Dex2C protection', 'Harder to decompile or clone', dex2c, (v) => setState(() => dex2c = v)),
                      _div(left: 69),
                      _toggleRow(Icons.lock_outline_rounded, 'String protection', 'Hides strings from a decompiled APK', strings, (v) => setState(() => strings = v)),
                    ]),
                  ),
                  _label('Performance'),
                  Container(
                    decoration: _card,
                    child: _toggleRow(Icons.bolt_rounded, 'Cache pages offline', 'Works without a connection after first launch', cache, (v) => setState(() => cache = v)),
                  ),
                  _label('Startup loading'),
                  Container(
                    decoration: _card,
                    padding: const EdgeInsets.fromLTRB(18, 16, 18, 16),
                    child: Column(children: [
                      Pressable(
                        scale: .985,
                        onTap: () async {
                          final r = await showIosSheet<String>(context, (_) => SplashSheet(selected: splash));
                          if (r != null) setState(() => splash = r);
                        },
                        child: Row(children: [
                          Container(
                            width: 58, height: 45,
                            decoration: BoxDecoration(color: const Color(0xFF0A0A0A), borderRadius: BorderRadius.circular(12)),
                            child: Center(child: Container(width: 19, height: 30, decoration: BoxDecoration(border: Border.all(color: const Color(0xFFB4B4B8), width: 2), borderRadius: BorderRadius.circular(6)))),
                          ),
                          const SizedBox(width: 16),
                          Expanded(
                            child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                              Text(splash, style: inter(17, w: FontWeight.w500)),
                              const SizedBox(height: 3),
                              Text(splashInfo[splash]!, style: inter(14.5, c: C.muted, h: 1.3)),
                            ]),
                          ),
                          const Icon(Icons.chevron_right_rounded, size: 24, color: Color(0xFF6A6A6E)),
                        ]),
                      ),
                      const SizedBox(height: 14),
                      if (splash == 'Custom') ...[
                        Pressable(
                          onTap: () async {
                            final f = await openFile(acceptedTypeGroups: [const XTypeGroup(label: 'Video or image', extensions: ['mp4', 'webm', 'mov', 'gif', 'png', 'jpg', 'jpeg', 'webp'])]);
                            if (f != null) setState(() { splashFile = f.path; splashFileName = f.name; });
                          },
                          child: Container(
                            height: 45, width: double.infinity,
                            margin: const EdgeInsets.only(bottom: 10),
                            padding: const EdgeInsets.symmetric(horizontal: 16),
                            alignment: Alignment.centerLeft,
                            decoration: BoxDecoration(color: const Color(0xFF2A2A2C), borderRadius: BorderRadius.circular(18)),
                            child: Text(splashFileName ?? 'Choose your video or image', overflow: TextOverflow.ellipsis, style: inter(16, w: FontWeight.w500, c: splashFileName != null ? C.teal : C.text)),
                          ),
                        ),
                      ],
                      Row(children: [
                        _btn(Icons.play_arrow_rounded, 'Test splash', () {}),
                        const SizedBox(width: 10),
                        _btn(Icons.refresh_rounded, 'Reset to default', () => setState(() => splash = 'Off')),
                      ]),
                    ]),
                  ),
                  _label('Signing'),
                  Container(
                    decoration: _card,
                    child: Column(children: [
                      _toggleRow(Icons.vpn_key_outlined, 'Sign as release build', 'Installable outside developer mode', signRelease, (v) => setState(() => signRelease = v)),
                      Padding(
                        padding: const EdgeInsets.symmetric(horizontal: 18),
                        child: Pressable(
                          scale: .985,
                          onTap: () {},
                          child: Container(
                            height: 68,
                            padding: const EdgeInsets.symmetric(horizontal: 14),
                            decoration: BoxDecoration(color: const Color(0xFF2A2A2C), borderRadius: BorderRadius.circular(22)),
                            child: Row(children: [
                              _tile(Icons.vpn_key_outlined, on: true, size: 39),
                              const SizedBox(width: 14),
                              Expanded(
                                child: Column(mainAxisAlignment: MainAxisAlignment.center, crossAxisAlignment: CrossAxisAlignment.start, children: [
                                  Text('Your signing key', style: inter(17, w: FontWeight.w600)),
                                  const SizedBox(height: 2),
                                  Text('Create, import or back up', style: inter(14.5, c: C.muted)),
                                ]),
                              ),
                              const Icon(Icons.chevron_right_rounded, size: 24, color: Color(0xFF6A6A6E)),
                            ]),
                          ),
                        ),
                      ),
                      const SizedBox(height: 20),
                      _div(left: 18),
                      _field('Owner key', input: _input(owner, "You're not Tycept, leave this blank"), foot: 'Tycept team only.'),
                      const SizedBox(height: 18),
                    ]),
                  ),
                ],
              ),
            ),
          ]),
          const Positioned(right: 17, bottom: 103, child: BotFab()),
          Positioned(
            left: 24, right: 24, bottom: 19,
            child: Pressable(
              scale: .975,
              onTap: building ? null : _build,
              child: AnimatedContainer(
                duration: const Duration(milliseconds: 250),
                height: 61,
                alignment: Alignment.center,
                decoration: BoxDecoration(color: building ? const Color(0xFF3D7367) : C.tealBtn, borderRadius: BorderRadius.circular(30)),
                child: building
                    ? const SizedBox(width: 24, height: 24, child: CircularProgressIndicator(strokeWidth: 2.6, color: Color(0xFF0F2A24)))
                    : Text('Build APK', style: inter(22, w: FontWeight.w700, c: const Color(0xFF0F2A24))),
              ),
            ),
          ),
        ]),
      ),
    );
  }

  Future<void> _build() async {
    final nav = Navigator.of(context);
    final req = widget.base
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
