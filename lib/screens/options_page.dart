import 'dart:async';
import 'package:flutter/material.dart' hide Text;
import 'package:flutter/cupertino.dart' show CupertinoPageRoute;
import 'dart:io';
import 'package:file_selector/file_selector.dart';
import '../api.dart';
import '../store.dart';
import '../theme.dart';
import '../widgets.dart';
import '../anim.dart';
import '../banner.dart';
import 'create_sheet.dart' show showEditSourceSheet;
import 'permissions_page.dart';
import 'signing_page.dart';
import '../signing_key.dart';
import 'splash_sheet.dart';
import 'gallery_picker.dart';
import 'build_page.dart';
import 'quick_build_dialog.dart';
import '../tr.dart';
import '../appearance.dart';
import '../notify.dart';

class OptionsPage extends StatefulWidget {
  final BuildRequest base;
  final String appName, source;
  const OptionsPage({super.key, required this.base, required this.appName, required this.source});
  @override
  State<OptionsPage> createState() => _OptionsPageState();
}

class _OptionsPageState extends State<OptionsPage> {
  int orient = 0;
  int gMode = 0; // Google Sign-In setup: 0 = upload google-services.json, 1 = paste Firebase details
  bool advanced = false, dex2c = false, strings = false, cache = true, signRelease = true, quickTemplate = false;
  String splash = 'Off', splashSpeed = 'Normal';
  String? logoPath, logoName, gsPath, gsName, splashFile, splashFileName;
  int logoRev = 0;
  bool building = false;
  static const splashCodes = {'Off': 'off', 'Custom': 'custom', 'Native': 'native', 'iOS zoom': 'ios', 'Slide away': 'slideaway', 'Pop': 'pop'};
  final perms = defaultPerms();
  late BuildRequest base = widget.base;
  late String source = widget.source;
  late final TextEditingController name = TextEditingController(text: widget.appName);
  final suffix = TextEditingController(), links = TextEditingController(), clientId = TextEditingController(), owner = TextEditingController();
  final fbAppId = TextEditingController(), fbApiKey = TextEditingController(), fbProjectId = TextEditingController();
  static const hints = ['Follows how the phone is held.', 'Always upright.', 'Always sideways.'];

  @override
  void initState() {
    super.initState();
    // Same hint the HTML app drops in when you reach the options step.
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted) IosBanner.show(context, kind: BannerKind.ok, title: 'Step 2 of 2', msg: 'Adjust options if you like, then tap Build');
    });
  }

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


  /// iOS-style form field: label on top, a rounded box with a teal focus ring, small help text.
  Widget _boxField(String label, TextEditingController c, String hint,
          {String? prefix, String? opt, String? desc, bool descAbove = false, bool info = false}) =>
      Padding(
        padding: const EdgeInsets.fromLTRB(16, 14, 16, 16),
        child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          if (label.isNotEmpty)
            Row(children: [
              Text(label, style: inter(16, w: FontWeight.w600)),
              if (opt != null) ...[const SizedBox(width: 8), Text(opt, style: inter(14, c: const Color(0xFF6E6E73)))],
              if (info) ...[const SizedBox(width: 8), const Icon(Icons.info_outline_rounded, size: 17, color: Color(0xFF6E6E73))],
            ]),
          if (desc != null && descAbove) ...[const SizedBox(height: 4), Text(desc, style: inter(13, c: C.muted, h: 1.3))],
          SizedBox(height: label.isEmpty ? 0 : 8),
          FocusRing(
            height: 50, radius: 14,
            color: const Color(0xFF2A2A2C), idleBorder: const Color(0xFF3A3A3C),
            child: Row(children: [
              if (prefix != null) ...[Text(prefix, style: inter(17, w: FontWeight.w600, c: const Color(0xFFE5E5EA))), const SizedBox(width: 10)],
              Expanded(
                child: TextField(
                  controller: c,
                  scrollPadding: const EdgeInsets.fromLTRB(20, 20, 20, 140),
                  onChanged: (_) => setState(() {}),
                  style: inter(17),
                  cursorColor: C.teal,
                  decoration: InputDecoration(border: InputBorder.none, isCollapsed: true, hintText: hint, hintStyle: inter(17, c: const Color(0xFF6E6E73))),
                ),
              ),
            ]),
          ),
          if (desc != null && !descAbove) ...[const SizedBox(height: 8), Text(desc, style: inter(13, c: C.muted, h: 1.3))],
        ]),
      );

  Widget _chip(IconData icon, String text, VoidCallback onTap, {Color color = C.teal}) => Padding(
        padding: const EdgeInsets.only(top: 10),
        child: Align(
          alignment: Alignment.centerLeft,
          child: Pressable(
            onTap: onTap,
            child: Container(
              height: 42,
              padding: const EdgeInsets.symmetric(horizontal: 16),
              decoration: BoxDecoration(color: const Color(0xFF2A2A2C), borderRadius: BorderRadius.circular(12)),
              child: Row(mainAxisSize: MainAxisSize.min, children: [
                Icon(icon, size: 18, color: color),
                const SizedBox(width: 8),
                Flexible(child: Text(text, maxLines: 1, overflow: TextOverflow.ellipsis, style: inter(16, w: FontWeight.w500, c: color == C.teal ? C.text : color))),
              ]),
            ),
          ),
        ),
      );

  Widget _btn(IconData icon, String text, VoidCallback onTap) => Expanded(
        child: Pressable(
          onTap: onTap,
          child: Container(
            height: 46,
            decoration: BoxDecoration(color: const Color(0xFF2C2C2E), borderRadius: BorderRadius.circular(12)),
            child: Row(mainAxisAlignment: MainAxisAlignment.center, children: [
              Icon(icon, size: 19, color: C.teal),
              const SizedBox(width: 8),
              Text(text, style: inter(16, w: FontWeight.w500, c: C.teal)),
            ]),
          ),
        ),
      );

  // ---- Google Sign-In: one card, status line, File / Paste switch ----

  bool get _fbAll => fbAppId.text.trim().isNotEmpty && fbApiKey.text.trim().isNotEmpty && fbProjectId.text.trim().isNotEmpty;
  bool get _fbAny => fbAppId.text.trim().isNotEmpty || fbApiKey.text.trim().isNotEmpty || fbProjectId.text.trim().isNotEmpty;

  Widget _seg(List<String> labels, int sel, ValueChanged<int> f) => Container(
        height: 42,
        padding: const EdgeInsets.all(3),
        decoration: BoxDecoration(color: const Color(0xFF2A2A2C), borderRadius: BorderRadius.circular(11)),
        child: Row(children: [
          for (var i = 0; i < labels.length; i++)
            Expanded(
              child: GestureDetector(
                behavior: HitTestBehavior.opaque,
                onTap: () => f(i),
                child: AnimatedContainer(
                  duration: const Duration(milliseconds: 240),
                  curve: iosCurve,
                  alignment: Alignment.center,
                  decoration: BoxDecoration(
                    color: i == sel ? const Color(0xFF48484A) : Colors.transparent,
                    borderRadius: BorderRadius.circular(8),
                  ),
                  child: Text(labels[i], style: inter(14, w: FontWeight.w600, c: i == sel ? C.text : C.muted)),
                ),
              ),
            ),
        ]),
      );

  Widget _uploadTile() {
    final has = gsName != null;
    return Pressable(
      onTap: _pickGoogleServices,
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 16),
        decoration: BoxDecoration(
          color: has ? C.tealDeep : const Color(0xFF2A2A2C),
          borderRadius: BorderRadius.circular(14),
          border: Border.all(color: has ? const Color(0x804FD5A6) : const Color(0xFF3A3A3C)),
        ),
        child: Row(children: [
          _tile(has ? Icons.check_rounded : Icons.file_upload_outlined),
          const SizedBox(width: 14),
          Expanded(
            child: Column(crossAxisAlignment: CrossAxisAlignment.start, mainAxisSize: MainAxisSize.min, children: [
              Text(has ? gsName! : 'Choose google-services.json',
                  maxLines: 1, overflow: TextOverflow.ellipsis, style: inter(16, w: FontWeight.w500)),
              const SizedBox(height: 4),
              Text(has ? 'Tap to replace' : 'Firebase console > Project settings > Your apps',
                  style: inter(13, c: C.muted, h: 1.4)),
            ]),
          ),
          if (has)
            GestureDetector(
              behavior: HitTestBehavior.opaque,
              onTap: () => setState(() { gsPath = null; gsName = null; }),
              child: const Padding(padding: EdgeInsets.all(6), child: Icon(Icons.close_rounded, size: 20, color: C.muted)),
            ),
        ]),
      ),
    );
  }

  /// Compact labelled field with a leading icon and a clear (x) button once it has text.
  Widget _gField(String label, TextEditingController c, String hint, {IconData? icon}) => Padding(
        padding: const EdgeInsets.only(top: 18),
        child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Text(label, style: inter(13, c: C.muted)),
          const SizedBox(height: 8),
          FocusRing(
            height: 52, radius: 12,
            color: const Color(0xFF2A2A2C), idleBorder: const Color(0xFF3A3A3C),
            child: Row(children: [
              if (icon != null) ...[Icon(icon, size: 18, color: C.muted), const SizedBox(width: 10)],
              Expanded(
                child: TextField(
                  controller: c,

                  scrollPadding: const EdgeInsets.fromLTRB(20, 20, 20, 140),
                  onChanged: (_) => setState(() {}),
                  style: inter(16),
                  cursorColor: C.teal,
                  decoration: InputDecoration(border: InputBorder.none, isCollapsed: true, hintText: hint, hintStyle: inter(16, c: const Color(0xFF6E6E73))),
                ),
              ),
              if (c.text.isNotEmpty)
                GestureDetector(
                  behavior: HitTestBehavior.opaque,
                  onTap: () => setState(c.clear),
                  child: const Padding(padding: EdgeInsets.only(left: 8), child: Icon(Icons.cancel_rounded, size: 18, color: Color(0xFF6E6E73))),
                ),
            ]),
          ),
        ]),
      );

  Widget _googleSection() {
    final ready = gMode == 0 ? gsName != null : _fbAll;
    final hasClient = clientId.text.trim().isNotEmpty;
    final status = ready
        ? (gMode == 0 ? 'google-services.json added' : 'Firebase details added')
        : (hasClient ? 'Client ID added' : 'Not set up');
    final on = ready || hasClient;
    return Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
      _cell(Icons.login_rounded, 'Google Sign-In',
          sub: status, on: on,
          trailing: on ? const Icon(Icons.check_circle_rounded, size: 22, color: C.teal) : null),
      Padding(
        padding: const EdgeInsets.fromLTRB(16, 4, 16, 20),
        child: Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
          _seg(const ['Upload file', 'Paste details'], gMode, (i) => setState(() => gMode = i)),
          const SizedBox(height: 16),
          AnimatedSize(
            duration: const Duration(milliseconds: 300),
            curve: iosCurve,
            alignment: Alignment.topCenter,
            child: gMode == 0
                ? _uploadTile()
                : Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
                    _gField('App ID', fbAppId, '1:1234567890:android:abc123', icon: Icons.apps_rounded),
                    _gField('API key', fbApiKey, 'AIza...', icon: Icons.key_rounded),
                    _gField('Project ID', fbProjectId, 'my-app-1234', icon: Icons.folder_open_rounded),
                    if (_fbAny && !_fbAll)
                      Padding(
                        padding: const EdgeInsets.only(top: 10),
                        child: Row(children: [
                          const Icon(Icons.info_outline_rounded, size: 16, color: Color(0xFFE0A030)),
                          const SizedBox(width: 6),
                          Expanded(child: Text('Fill in all three, or leave them empty.', style: inter(13, c: const Color(0xFFE0A030)))),
                        ]),
                      ),
                  ]),
          ),
          _gField('Web client ID, optional', clientId, '123-abc.apps.googleusercontent.com', icon: Icons.badge_outlined),
          const SizedBox(height: 18),
          Text('Lets people sign in with Google. Use the file from Firebase, or paste its details.',
              style: inter(13, c: C.muted, h: 1.5)),
        ]),
      ),
    ]);
  }

  Future<void> _pickGoogleServices() async {
    final f = await openFile(acceptedTypeGroups: [const XTypeGroup(label: 'JSON', extensions: ['json'])]);
    if (f != null) setState(() { gsPath = f.path; gsName = f.name; });
  }

  Future<void> _pickSplashFile() async {
    final f = await openFile(acceptedTypeGroups: [const XTypeGroup(label: 'Video or image', extensions: ['mp4', 'webm', 'mov', 'gif', 'png', 'jpg', 'jpeg', 'webp'])]);
    if (f != null) setState(() { splashFile = f.path; splashFileName = f.name; });
  }

  Future<void> _pickSplash() async {
    final r = await showIosSheet<SplashChoice>(context, (_) => SplashSheet(selected: splash, speed: splashSpeed));
    if (r != null) setState(() { splash = r.style; splashSpeed = r.speed; });
  }

  Future<void> _pickLogo() async {
    final f = await pickIconImage(context);
    if (f == null || !mounted) return;
    // Flutter caches images by file path: drop the old copy so the new photo shows up immediately.
    await FileImage(File(f.path)).evict();
    if (logoPath != null) await FileImage(File(logoPath!)).evict();
    PaintingBinding.instance.imageCache.clear();
    PaintingBinding.instance.imageCache.clearLiveImages();
    if (!mounted) return;
    setState(() { logoPath = f.path; logoName = f.name; logoRev++; });
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
  Widget build(BuildContext context) => PopScope(
        canPop: false,
        onPopInvokedWithResult: (didPop, _) async {
          if (didPop) return;
          final leave = await confirmLeave(context,
              title: 'Leave this page?',
              message: 'Your app options will be gone and no build will start.',
              stay: 'Keep editing');
          if (leave && context.mounted) Navigator.of(context).pop();
        },
        child: _page(context),
      );

  Widget _page(BuildContext context) {
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
                  // ---- App summary ----
                  _group([
                    Padding(
                      padding: const EdgeInsets.all(14),
                      child: Row(children: [
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
                  Padding(
                    padding: const EdgeInsets.fromLTRB(4, 8, 4, 0),
                    child: Text('Step 2 of 2 · Everything below is optional. Tap Build when you are ready.', style: inter(13, c: C.muted, h: 1.35)),
                  ),

                  // ---- Orientation ----
                  _hdr('Orientation'),
                  _group([
                    Container(
                      color: C.card,
                      padding: const EdgeInsets.fromLTRB(16, 24, 16, 18),
                      child: Column(children: [
                        // Fixed-size stage: the phone icon turns inside it without ever changing the card's height.
                        SizedBox(height: 80, width: double.infinity, child: RepaintBoundary(child: Center(child: _OrientIcon(orient: orient)))),
                        const SizedBox(height: 20),
                        Seg(
                          labels: const ['Auto', 'Portrait', 'Landscape'],
                          icons: const [Icons.autorenew_rounded, Icons.stay_current_portrait_rounded, Icons.stay_current_landscape_rounded],
                          index: orient, onChanged: (i) => setState(() => orient = i),
                          height: 46, radius: 12, pad: 3, thumbRadius: 9, fontSize: 13, bordered: true),
                        const SizedBox(height: 16),
                        SizedBox(
                          height: 22,
                          child: AnimatedSwitcher(
                            duration: const Duration(milliseconds: 200),
                            child: Text(hints[orient], key: ValueKey(orient), style: inter(13, c: C.muted)),
                          ),
                        ),
                      ]),
                    ),
                  ]),

                  // ---- App icon ----
                  _hdr('App icon'),
                  _group([
                    _IosCell(
                      onTap: _pickLogo,
                      child: Padding(
                        padding: const EdgeInsets.all(14),
                        child: Row(children: [
                          Container(
                            width: 62, height: 62,
                            alignment: Alignment.center,
                            clipBehavior: Clip.antiAlias,
                            decoration: BoxDecoration(color: const Color(0xFF2C2C2E), borderRadius: BorderRadius.circular(14)),
                            child: logoPath != null
                                ? Unfilter(child: Image.file(File(logoPath!), key: ValueKey('logo$logoRev'), fit: BoxFit.cover, width: 62, height: 62, gaplessPlayback: false))
                                : Column(mainAxisAlignment: MainAxisAlignment.center, children: [
                                    const Icon(Icons.file_upload_outlined, size: 22, color: Color(0xFFE5E5EA)),
                                    const SizedBox(height: 2),
                                    Text('Tap to\nupload', textAlign: TextAlign.center, style: inter(11, c: const Color(0xFFE5E5EA), h: 1.15)),
                                  ]),
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
                      trailing: Container(
                        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
                        decoration: BoxDecoration(color: C.card, borderRadius: BorderRadius.circular(9)),
                        child: Text(pkg, style: const TextStyle(fontFamily: 'monospace', fontSize: 13, color: Color(0xFFC8C8CC))),
                      )),
                  _group([
                    _boxField('App name', name, 'App name'),
                    _sep(left: 0),
                    _cell(Icons.tune_rounded, 'Advanced',
                        sub: 'Only if you want it', on: false,
                        onTap: () => setState(() => advanced = !advanced),
                        trailing: AnimatedRotation(
                            turns: advanced ? -.25 : .25,
                            duration: const Duration(milliseconds: 300),
                            curve: iosCurve,
                            child: _chev)),
                    AnimatedSize(
                      duration: const Duration(milliseconds: 340),
                      curve: iosCurve,
                      alignment: Alignment.topCenter,
                      child: advanced
                          ? Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
                              _sep(left: 0),
                              _boxField('Package suffix', suffix, 'e.g. myapp or test.build', prefix: 'com.', opt: 'optional', desc: "Your app's ID, like com.myapp."),
                              _sep(),
                              _boxField('App Links domain', links, 'e.g. example.com', opt: 'optional', info: true, descAbove: true, desc: "Your site's address. Its links open in the app."),
                              _sep(),
                              _googleSection(),
                            ])
                          : const SizedBox(width: double.infinity),
                    ),
                  ]),
                  const SizedBox(height: 16),
                  _group([
                    _cell(Icons.shield_outlined, 'Permissions',
                        sub: '$permCount selected',
                        trailing: _chev,
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
                    _cell(splashIcons[splash], splash,
                        sub: splashInfo[splash],
                        on: splash != 'Off',
                        trailing: _chev,
                        onTap: _pickSplash),
                    if (splash == 'Custom') ...[
                      _sep(left: 60),
                      _cell(Icons.movie_outlined, splashFileName ?? 'Choose your video or image',
                          titleColor: splashFileName != null ? C.teal : C.text,
                          trailing: splashFileName == null ? _chev : null,
                          onTap: _pickSplashFile),
                    ],
                    if (splash != 'Off' && splash != 'Custom') ...[
                      _sep(left: 60),
                      Padding(
                        padding: const EdgeInsets.fromLTRB(16, 12, 16, 14),
                        child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                          Text('Speed of the launch animation', style: inter(13, c: C.muted)),
                          const SizedBox(height: 8),
                          _seg(const ['Slow', 'Normal', 'Fast'], const ['Slow', 'Normal', 'Fast'].indexOf(splashSpeed),
                              (i) => setState(() => splashSpeed = const ['Slow', 'Normal', 'Fast'][i])),
                        ]),
                      ),
                    ],
                  ]),

                  // ---- Signing ----
                  _hdr('Signing'),
                  _group([
                    _toggleCell(Icons.vpn_key_outlined, 'Sign as release build', 'Installable outside developer mode', signRelease, (v) => setState(() => signRelease = v)),
                    _sep(left: 60),
                    ValueListenableBuilder<SigningKeyInfo?>(
                      valueListenable: SigningKeys.i.current,
                      builder: (_, k, __) => _cell(Icons.key_rounded, 'Your signing key',
                          sub: k == null ? 'Create, import or back up' : 'Using your key (${k.shortFingerprint})',
                          on: k != null, trailing: _chev,
                          onTap: () async {
                            await Navigator.of(context).push(SigningPage.route());
                            if (mounted) setState(() {});
                          }),
                    ),
                    _sep(left: 0),
                    _boxField('Owner key', owner, "You're not Tycept, leave this blank"),
                    _sep(left: 60),
                    _toggleCell(Icons.bolt_rounded, 'Build the Quick template', 'Owner only. Makes the shared APK Quick builds use', quickTemplate, (v) => setState(() => quickTemplate = v)),
                  ]),
                  Padding(
                    padding: const EdgeInsets.fromLTRB(16, 8, 16, 0),
                    child: Text('Owner key is for the Tycept team only.', style: inter(13, c: C.muted, h: 1.35)),
                  ),
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
                    duration: const Duration(milliseconds: 300),
                    curve: iosCurve,
                    child: AnimatedOpacity(opacity: hidden ? 0 : 1, duration: const Duration(milliseconds: 180), child: const BotFab()),
                  ),
                ),
              ),
            ),
          // ---- Frosted bottom bar with the Build button (iOS toolbar style) ----
          if (!kbOpen)
            Positioned(
              left: 0, right: 0, bottom: 0,
              child: RepaintBoundary(
                child: ClipRect(
                  child: Container(
                    padding: const EdgeInsets.fromLTRB(20, 12, 20, 12),
                    decoration: BoxDecoration(
                      // Solid instead of a live blur: blurring scrolling content every frame is what makes low-end phones stutter.
                      color: C.bg,
                      border: const Border(top: BorderSide(color: Color(0xFF2A2A2C), width: .5)),
                    ),
                    child: Pressable(
                      scale: .975,
                      onTap: building ? null : _build,
                      child: AnimatedContainer(
                        duration: const Duration(milliseconds: 200),
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
      ..splashSpeed = splashSpeed.toLowerCase()
      ..permissions = selectedPerms(perms)
      ..packageSuffix = suffix.text.trim()
      ..appLinkHost = links.text.trim()
      ..googleWebClientId = clientId.text.trim()
      ..firebaseAppId = gMode == 1 ? fbAppId.text.trim() : ''
      ..firebaseApiKey = gMode == 1 ? fbApiKey.text.trim() : ''
      ..firebaseProjectId = gMode == 1 ? fbProjectId.text.trim() : ''
      ..ownerKey = owner.text
      ..quickTemplate = quickTemplate
      ..logoPath = logoPath
      ..googleServicesPath = gMode == 0 ? gsPath : null
      ..customSplashPath = splashFile;
    final myKey = SigningKeys.i.current.value;
    if (myKey != null && signRelease && Api.i.customSigningOk && !quickTemplate) {
      req
        ..signingKeyPath = myKey.keyPath
        ..signingCertPath = myKey.certPath;
    }
    // The owner's Quick template is a plain APK: the server refuses it if any of these are set, so drop them.
    if (quickTemplate && owner.text.trim().isNotEmpty) {
      req
        ..platform = 'android'
        ..buildType = 'apk'
        ..dex2c = false
        ..stringProtect = false
        ..appLinkHost = ''
        ..remoteUrl = null
        ..googleServicesPath = null
        ..firebaseAppId = ''
        ..firebaseApiKey = ''
        ..firebaseProjectId = ''
        ..signingKeyPath = null
        ..signingCertPath = null;
      if (req.splashStyle == 'custom') req.splashStyle = 'off';
      req.customSplashPath = null;
    }
    if (splash == 'Custom' && splashFile == null && !(quickTemplate && owner.text.trim().isNotEmpty)) {
      IosBanner.show(context, kind: BannerKind.warn, title: 'Add your startup file', msg: 'You picked a custom startup screen. Choose the video or image to play first.', duration: const Duration(seconds: 6));
      return;
    }
    // The owner's "Build the Quick template" switch always runs on the server.
    if (req.mode == 'quick' && quickTemplate) req.mode = 'full';
    if (req.mode == 'quick') {
      // Built right here on the phone: no server, no internet.
      setState(() => building = true);
      String? next;
      try {
        next = await showIosDialog<String>(context, (_) => QuickBuildDialog(req: req, appName: name.text.trim().isEmpty ? widget.appName : name.text.trim()));
      } finally {
        if (mounted) setState(() => building = false);
      }
      if (next != 'full' || !mounted) return;
      // This version of the app has no Quick template, so build on the server (every option still applies).
      req.mode = 'full';
      IosBanner.show(context, kind: BannerKind.warn, title: 'Building on the server', msg: 'Quick build isn\u2019t available in this version, so this one is built on the server.', duration: const Duration(seconds: 5));
    }
    setState(() => building = true);
    // Render's free tier sleeps: if the server is slow to answer, say so (same as the HTML app).
    final wakeTimer = Timer(const Duration(seconds: 5), () {
      if (mounted) IosBanner.show(context, kind: BannerKind.warn, title: 'Please wait', msg: 'Waking up the build server. This can take up to a minute.', duration: const Duration(seconds: 6));
    });
    try {
      final lim = await Api.i.limit(ownerKey: req.ownerKey);
      if (lim != null) Store.i.limit.value = lim;
      if (lim != null && lim.remaining <= 0) {
        if (mounted) _limitPopup(lim);
        return;
      }
      final id = await Api.i.startBuild(req);
      await Store.i.setActiveJob(id);
      BuildNotifier.i.watch(id, name.text.trim().isEmpty ? widget.appName : name.text.trim());
      Store.i.refreshLimit(ownerKey: req.ownerKey);
      Store.i.refreshHistory();
      if (!mounted) return;
      nav.push(CupertinoPageRoute(builder: (_) => BuildPage(jobId: id, appName: name.text.trim().isEmpty ? widget.appName : name.text.trim(), logoPath: logoPath)));
    } on ApiError catch (e) {
      if (!mounted) return;
      if (e.status == 429) {
        final lim = await Api.i.limit(ownerKey: req.ownerKey);
        if (lim != null) Store.i.limit.value = lim;
        if (mounted) {
          if (lim != null) {
            _limitPopup(lim);
          } else {
            IosBanner.show(context, kind: BannerKind.warn, title: 'Daily limit reached', msg: e.message, duration: const Duration(seconds: 7));
          }
        }
      } else if (e.status == 503 && e.code == 'server_busy') {
        IosBanner.show(context, kind: BannerKind.warn, title: 'Server busy', msg: 'The server is busy. Nothing was started and no build was used. Try again in about ${e.retryAfter} seconds.', duration: const Duration(seconds: 6));
      } else {
        IosBanner.show(context, kind: BannerKind.err, title: 'Couldn\u2019t start the build', msg: e.message, duration: const Duration(seconds: 6));
      }
    } catch (e) {
      if (mounted) IosBanner.show(context, kind: BannerKind.err, title: 'Couldn\u2019t reach the server', msg: 'Check your connection and try again.', duration: const Duration(seconds: 6));
    } finally {
      wakeTimer.cancel();
      if (mounted) setState(() => building = false);
    }
  }

  void _limitPopup(BuildLimit l) => IosBanner.show(context,
      kind: BannerKind.warn,
      title: 'Daily limit reached',
      msg: 'You used all ${l.limit} builds today. They reset at midnight UTC.',
      duration: const Duration(seconds: 7));
}

class _OrientIcon extends StatefulWidget {
  final int orient; // 0 auto, 1 portrait, 2 landscape
  const _OrientIcon({required this.orient});
  @override
  State<_OrientIcon> createState() => _OrientIconState();
}

class _OrientIconState extends State<_OrientIcon> with SingleTickerProviderStateMixin {
  late final AnimationController _c = AnimationController(vsync: this, duration: const Duration(milliseconds: 3200));
  // 0 = portrait, 1 = landscape. Rests a moment at each end, then turns.
  static final _loop = TweenSequence<double>([
    TweenSequenceItem(tween: ConstantTween<double>(0), weight: 20),
    TweenSequenceItem(tween: Tween<double>(begin: 0, end: 1).chain(CurveTween(curve: iosCurve)), weight: 30),
    TweenSequenceItem(tween: ConstantTween<double>(1), weight: 20),
    TweenSequenceItem(tween: Tween<double>(begin: 1, end: 0).chain(CurveTween(curve: iosCurve)), weight: 30),
  ]);

  @override
  void initState() {
    super.initState();
    if (widget.orient == 0) _c.repeat();
  }

  @override
  void didUpdateWidget(_OrientIcon old) {
    super.didUpdateWidget(old);
    if (widget.orient == 0) {
      if (!_c.isAnimating) _c.repeat();
    } else {
      _c.stop();
    }
  }

  @override
  void dispose() {
    _c.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => AnimatedBuilder(
        animation: _c,
        builder: (_, __) {
          // Auto loops portrait <-> landscape. Portrait / Landscape just show their fixed shape.
          final t = widget.orient == 0 ? _loop.evaluate(_c) : (widget.orient == 2 ? 1.0 : 0.0);
          double l(double a, double b) => a + (b - a) * t;
          return Container(
            width: l(50, 93), height: l(80, 50),
            padding: EdgeInsets.fromLTRB(l(7, 16), l(14, 7), l(7, 9), 0),
            decoration: BoxDecoration(border: Border.all(color: C.teal, width: 3), borderRadius: BorderRadius.circular(14)),
            child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              _bar(l(28, 48), const Color(0xFF555555)), const SizedBox(height: 5),
              _bar(l(36, 66), C.teal), const SizedBox(height: 5),
              _bar(l(22, 36), const Color(0xFF555555)),
            ]),
          );
        },
      );

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
      onTap: on ? () => widget.onTap!() : null,
      child: AnimatedContainer(
        duration: Duration(milliseconds: down ? 0 : 260),
        color: down ? const Color(0xFF2C2C2E) : Colors.transparent,
        child: widget.child,
      ),
    );
  }
}
