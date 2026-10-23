import 'dart:io';
import 'package:file_selector/file_selector.dart';
import 'package:flutter/material.dart' hide Text;
import 'package:flutter/cupertino.dart' show CupertinoPageRoute;
import '../api.dart';
import '../theme.dart';
import '../widgets.dart';
import '../anim.dart';
import '../banner.dart';
import '../zip_util.dart';
import 'options_page.dart';
import 'build_mode_sheet.dart';
import '../tr.dart';

void showCreateSheet(BuildContext context) => showIosSheet(context, (_) => const _CreateSheet());

/// What the Create sheet hands back when it is opened from the Edit button on App options.
class CreateEdit {
  final BuildRequest base;
  final String name, source;
  const CreateEdit({required this.base, required this.name, required this.source});
}

/// Edit button on App options: the same Create sheet slides up over the page, already filled in
/// with the current app. Continue returns the new source; closing it changes nothing.
Future<CreateEdit?> showEditSourceSheet(BuildContext context,
        {required BuildRequest base, required String name, required String source}) =>
    showIosSheet<CreateEdit>(context, (_) => _CreateSheet(edit: CreateEdit(base: base, name: name, source: source)));

/// Platforms that still have rough edges get a small BUGS tag in the menu.
const _buggy = {'windows', 'flutter'};

const _platforms = {
  'android': ('Android (APK / AAB)', 'Installable package'),
  'windows': ('Windows (.exe)', 'Desktop app · .exe installer'),
  'flutter': ('Flutter (ZIP)', 'Upload project zip · builds to APK'),
};

class _CreateSheet extends StatefulWidget {
  final CreateEdit? edit;
  const _CreateSheet({this.edit});
  @override
  State<_CreateSheet> createState() => _CreateSheetState();
}

class _CreateSheetState extends State<_CreateSheet> {
  int mode = 0; // 0 Upload, 1 Website
  int kind = 0; // 0 HTML, 1 ZIP
  String platform = 'android';
  final name = TextEditingController();
  final link = TextEditingController();
  final nameFocus = FocusNode();
  final linkFocus = FocusNode();
  final nameKey = GlobalKey();
  final linkKey = GlobalKey();
  final sheetKey = GlobalKey();
  final platformKey = GlobalKey();
  bool platformOpen = false;
  double _relName = 0, _relLink = 0; // field bottoms measured inside the sheet (animation-proof)
  XFile? picked;
  String? kept; // file already chosen before pressing Edit (name shown, nothing re-read)
  String? linkError;
  bool busy = false;

  bool get ready => !busy && (mode == 0 ? (picked != null || kept != null) : link.text.trim().isNotEmpty);

  @override
  void initState() {
    super.initState();
    final e = widget.edit;
    if (e != null) {
      name.text = e.name;
      platform = e.base.platform;
      if (e.base.remoteUrl != null) {
        mode = 1;
        link.text = e.base.remoteUrl!.replaceFirst(RegExp(r'^https?://'), '');
      } else {
        mode = 0;
        kept = e.source.replaceFirst('Upload · ', '');
        kind = kept!.toLowerCase().endsWith('.html') || kept!.toLowerCase().endsWith('.htm') ? 0 : 1;
      }
    }
    WidgetsBinding.instance.addPostFrameCallback((_) => _measure());
    nameFocus.addListener(_onFocus);
    linkFocus.addListener(_onFocus);
  }

  @override
  void dispose() {
    nameFocus.dispose();
    linkFocus.dispose();
    name.dispose();
    link.dispose();
    super.dispose();
  }

  void _onFocus() {
    if (mounted) setState(() {});
  }

  /// Where each field's bottom edge sits inside the sheet. Measured relative to the sheet itself,
  /// so the number doesn't change while the sheet is sliding up or down.
  void _measure() {
    if (!mounted) return;
    final sheet = sheetKey.currentContext?.findRenderObject();
    if (sheet is! RenderBox || !sheet.attached) return;
    double rel(GlobalKey k, double old) {
      final ro = k.currentContext?.findRenderObject();
      if (ro is! RenderBox || !ro.attached) return old;
      return ro.localToGlobal(Offset(0, ro.size.height), ancestor: sheet).dy;
    }
    final n = rel(nameKey, _relName), l = rel(linkKey, _relLink);
    if ((n - _relName).abs() > .5 || (l - _relLink).abs() > .5) setState(() { _relName = n; _relLink = l; });
  }

  Future<void> _pick() async {
    final html = kind == 0 && platform != 'flutter';
    final f = await openFile(acceptedTypeGroups: [
      XTypeGroup(label: html ? 'HTML' : 'ZIP', extensions: html ? ['html', 'htm'] : ['zip']),
    ]);
    if (f == null || !mounted) return;
    setState(() { picked = f; kept = null; });
  }

  /// Opens the platform menu as a small popover that grows upward out of the Platform pill.
  Future<void> _choosePlatform() async {
    final box = platformKey.currentContext?.findRenderObject();
    if (box is! RenderBox || !box.attached) return;
    final anchor = box.localToGlobal(Offset.zero) & box.size;
    setState(() => platformOpen = true);
    final r = await Navigator.of(context, rootNavigator: true).push<String>(PageRouteBuilder<String>(
      opaque: false,
      barrierDismissible: true,
      barrierLabel: 'close',
      barrierColor: Colors.transparent,
      transitionDuration: const Duration(milliseconds: 270),
      reverseTransitionDuration: const Duration(milliseconds: 160),
      pageBuilder: (c, a, s) => _PlatformMenu(anchor: anchor, selected: platform),
      transitionsBuilder: (c, a, s, child) => child, // the menu animates itself, from the pill
    ));
    if (mounted) setState(() => platformOpen = false);
    if (r == null || !mounted) return;
    if (_buggy.contains(r)) {
      IosBanner.show(context, kind: BannerKind.warn,
          title: '${r == 'windows' ? 'Windows' : 'Flutter'} builds have known bugs',
          msg: 'Android is recommended.', duration: const Duration(milliseconds: 3600));
    }
    setState(() {
      platform = r;
      if (r == 'flutter') {
        mode = 0;
        kind = 1;
        picked = null;
        kept = null;
      }
    });
  }

  Uri? _parseLink() {
    var t = link.text.trim();
    if (t.isEmpty) return null;
    if (!RegExp(r'^[a-z]+://', caseSensitive: false).hasMatch(t)) t = 'https://$t';
    final u = Uri.tryParse(t);
    if (u == null || !(u.scheme == 'http' || u.scheme == 'https') || !u.host.contains('.')) return null;
    return u;
  }

  Future<void> _continue() async {
    final nav = Navigator.of(context);
    var base = BuildRequest(platform: platform);
    var source = '';
    var fallbackName = widget.edit?.name ?? 'My app';
    var quickOk = false; // Quick build only exists for a single HTML file or a website link.
    setState(() => busy = true);
    try {
      if (mode == 0 && picked == null && kept != null && widget.edit != null) {
        base = widget.edit!.base..platform = platform;
        source = widget.edit!.source;
      } else if (mode == 0) {
        final f = picked!;
        final base0 = f.name.replaceAll(RegExp(r'\.(zip|html?)$', caseSensitive: false), '');
        fallbackName = base0;
        final isHtml = RegExp(r'\.html?$', caseSensitive: false).hasMatch(f.name);
        if (isHtml) {
          base.zipBytes = zipSingle('index.html', await File(f.path).readAsBytes());
          base.zipName = '$base0.zip';
        } else {
          base.zipPath = f.path;
          base.zipName = f.name;
        }
        source = 'Upload · ${f.name}';
        quickOk = isHtml;
      } else {
        final u = _parseLink();
        if (u == null) {
          setState(() {
            linkError = 'Enter a full link, like https://yoursite.com';
            busy = false;
          });
          return;
        }
        base.zipBytes = websiteZip(u.toString());
        base.zipName = '${u.host}.zip';
        base.remoteUrl = u.toString();
        fallbackName = u.host.replaceFirst(RegExp(r'^www\.'), '');
        source = 'Website · ${u.host}${u.path == '/' ? '' : u.path}';
        quickOk = true;
      }
    } catch (e) {
      if (mounted) {
        setState(() => busy = false);
        IosBanner.show(context, kind: BannerKind.err, title: 'Couldn\u2019t open that file', msg: e is FormatException ? e.message : 'That file isn\u2019t it. Try a different one.', duration: const Duration(seconds: 6));
      }
      return;
    }
    final n = name.text.trim().isEmpty ? fallbackName : name.text.trim();
    if (widget.edit != null) {
      nav.pop(CreateEdit(base: base, name: n, source: source));
      return;
    }
    if (quickOk && platform == 'android') {
      final choice = await showBuildModeSheet(context);
      if (choice == null) {
        // Closed without choosing: stay on this sheet so nothing is lost.
        if (mounted) setState(() => busy = false);
        return;
      }
      base.mode = choice;
    }
    nav.pop();
    nav.push(CupertinoPageRoute(builder: (_) => OptionsPage(base: base, appName: n, source: source)));
  }

  Widget _field(TextEditingController c, String hint,
      {required FocusNode fn, required GlobalKey k, bool clear = false, double h = 50, double fs = 18, double r = 12, EdgeInsets? pad, TextInputType? kb, TextInputAction action = TextInputAction.next}) {
    final on = fn.hasFocus;
    return AnimatedContainer(
      key: k,
      duration: const Duration(milliseconds: 130),
      curve: Curves.easeOut,
      height: h,
      padding: pad ?? const EdgeInsets.only(left: 14, right: 12),
      decoration: BoxDecoration(
        color: C.field,
        borderRadius: BorderRadius.circular(r),
        border: Border.all(color: on ? C.teal : Colors.transparent, width: 1.8),
      ),
      child: Row(children: [
        Expanded(
          child: TextField(
            controller: c,
            focusNode: fn,
            scrollPadding: const EdgeInsets.fromLTRB(20, 20, 20, 40),
            keyboardType: kb,
            textInputAction: action,
            onChanged: (_) => setState(() => linkError = null),
            style: inter(fs),
            cursorColor: C.teal,
            decoration: InputDecoration(border: InputBorder.none, hintText: hint, hintStyle: inter(fs, c: C.muted), isCollapsed: true),
          ),
        ),
        if (clear)
          Pressable(
            onTap: () => setState(c.clear),
            child: Container(
              width: 18, height: 18,
              decoration: const BoxDecoration(color: Color(0xFF6B6B70), shape: BoxShape.circle),
              child: const Icon(Icons.close_rounded, size: 12, color: C.field),
            ),
          ),
      ]),
    );
  }

  Widget _row(String label, String sub, Widget ctrl, {double w = 192, bool warn = false}) => Padding(
        padding: const EdgeInsets.fromLTRB(2, 12, 2, 0),
        child: Row(children: [
          Expanded(
            child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              Text(label, maxLines: 1, style: inter(18, w: FontWeight.w700)),
              if (warn)
                Row(children: [
                  const Icon(Icons.warning_amber_rounded, size: 15, color: Color(0xFFF5A524)),
                  const SizedBox(width: 4),
                  Text('Has bugs', style: inter(13.5, w: FontWeight.w600, c: const Color(0xFFF5A524))),
                ])
              else
                Text(sub, maxLines: 2, overflow: TextOverflow.ellipsis, style: inter(12.5, c: C.muted)),
            ]),
          ),
          SizedBox(width: w, height: 46, child: ctrl),
        ]),
      );

  Widget _pill(Widget child, {VoidCallback? onTap, Color? outline}) => Pressable(
        onTap: onTap,
        scale: .98,
        child: Container(
          padding: const EdgeInsets.fromLTRB(13, 0, 11.5, 0),
          decoration: BoxDecoration(
              color: C.field,
              borderRadius: BorderRadius.circular(14),
              border: outline == null ? null : Border.all(color: outline, width: 1.8)),
          child: child,
        ),
      );

  String get _hint {
    if (busy) return 'Getting your project ready…';
    if (linkError != null) return linkError!;
    if (ready) return '';
    return mode == 0 ? 'Choose a file to continue' : 'Enter your website link to continue';
  }

  @override
  Widget build(BuildContext context) {
    final mq = MediaQuery.of(context);
    final h = mq.size.height;
    final kb = mq.viewInsets.bottom;
    // The sheet keeps its fixed 67% size and its place. The keyboard simply sits on top of it,
    // and the sheet only rises by the smallest amount needed to keep the focused field
    // visible just above the keyboard.
    final sheetH = h * .672;
    final rel = linkFocus.hasFocus ? _relLink : (nameFocus.hasFocus ? _relName : null);
    var lift = 0.0;
    if (kb > 0 && rel != null && rel > 0) {
      final naturalBottom = h - sheetH + rel;
      lift = (naturalBottom + 22 - (h - kb)).clamp(0.0, kb).toDouble();
    }
    WidgetsBinding.instance.addPostFrameCallback((_) => _measure());
    final flutter = platform == 'flutter';
    final html = kind == 0 && !flutter;
    return AnimatedPadding(
      duration: const Duration(milliseconds: 200),
      curve: iosCurve,
      padding: EdgeInsets.only(bottom: lift),
      child: Container(
        key: sheetKey,
        height: sheetH,
        decoration: const BoxDecoration(color: C.sheet, borderRadius: BorderRadius.vertical(top: Radius.circular(25))),
        padding: const EdgeInsets.fromLTRB(18, 15, 18, 16),
        child: Column(children: [
          Row(children: [
            Pressable(
              scale: .85,
              onTap: () => Navigator.pop(context),
              child: const Padding(padding: EdgeInsets.fromLTRB(4, 0, 16, 0), child: Icon(Icons.close_rounded, size: 22)),
            ),
            Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              Text('Create', style: inter(26.5, w: FontWeight.w700, h: 1.1)),
              const SizedBox(height: 1),
              Text('Step 1 of 2 · Add your app source', style: inter(13, c: C.muted)),
            ]),
          ]),
          const SizedBox(height: 13.5),
          Expanded(
            // Not scrollable: the form is laid out once, exactly like the design.
            child: SingleChildScrollView(physics: const NeverScrollableScrollPhysics(), padding: EdgeInsets.zero, child: Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
              Seg(labels: const ['Upload', 'Website'], index: mode, height: 50, radius: 14, pad: 2, thumbInset: 2, thumbRadius: 10, fontSize: 15,
                  onChanged: (i) => setState(() {
                        mode = i;
                        if (i == 1 && flutter) platform = 'android'; // a link can't be a Flutter project
                      })),
              const SizedBox(height: 16),
              _field(name, 'App name', fn: nameFocus, k: nameKey, clear: true),
              if (mode == 0) ...[
                const SizedBox(height: 16),
                Padding(
                  padding: const EdgeInsets.only(left: 4, right: 2),
                  child: Row(children: [
                    // Flutter builds only take a project zip, so the HTML tile goes away and ZIP fills the row.
                    if (!flutter) ...[
                      Expanded(child: BracketTile(title: 'HTML', sub: 'One .html file', on: kind == 0, onTap: () => setState(() { kind = 0; picked = null; kept = null; }))),
                      const SizedBox(width: 14),
                    ],
                    Expanded(child: BracketTile(title: 'ZIP', sub: 'Zipped project folder', on: kind == 1 || flutter, onTap: () => setState(() { kind = 1; picked = null; kept = null; }))),
                  ]),
                ),
                const SizedBox(height: 6),
              ] else
                const SizedBox(height: 10),
              _row('Platform', 'What to build', warn: _buggy.contains(platform),
                  KeyedSubtree(key: platformKey, child: _pill(onTap: _choosePlatform, outline: _buggy.contains(platform) ? const Color(0xFFF5A524) : null, Row(mainAxisAlignment: MainAxisAlignment.spaceBetween, children: [
                    Expanded(child: Text(_platforms[platform]!.$1, maxLines: 1, overflow: TextOverflow.ellipsis, style: inter(17))),
                    AnimatedRotation(
                      turns: platformOpen ? .5 : 0,
                      duration: const Duration(milliseconds: 220),
                      curve: iosCurve,
                      child: const Icon(Icons.keyboard_arrow_down_rounded, size: 24),
                    ),
                  ])))),
              if (mode == 0) ...[
                _row('File', html ? 'Your .html file' : (flutter ? 'Your Flutter project .zip' : 'Your .zip project'),
                    _pill(onTap: _pick,
                        Row(mainAxisAlignment: MainAxisAlignment.spaceBetween, children: [
                          Expanded(child: Text(picked?.name ?? kept ?? 'Choose file', overflow: TextOverflow.ellipsis, style: inter(17, c: (picked ?? kept) == null ? C.text : C.teal))),
                          const Icon(Icons.file_upload_outlined, size: 22),
                        ]))),
                const SizedBox(height: 16.5),
                Padding(
                  padding: const EdgeInsets.only(left: 2),
                  child: Text('How to prep your file',
                      style: inter(16, c: const Color(0xFFB4B4B8)).copyWith(decoration: TextDecoration.underline, decorationColor: const Color(0xFFB4B4B8))),
                ),
              ] else ...[
                _row('Link', 'Loads live in the app',
                    _field(link, 'https://yoursite.com', fn: linkFocus, k: linkKey, h: 46, fs: 17, r: 14, kb: TextInputType.url, action: TextInputAction.done, pad: const EdgeInsets.symmetric(horizontal: 11.2))),
                const SizedBox(height: 16),
                Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 2),
                  child: Text('Your app opens this link live, so changes on your site show up automatically.', style: inter(13, c: C.muted, h: 1.4)),
                ),
              ],
            ])),
          ),
          Padding(
            padding: EdgeInsets.zero,
            child: AnimatedSwitcher(
              duration: const Duration(milliseconds: 160),
              layoutBuilder: (cur, prev) => Stack(alignment: Alignment.center, children: [...prev, if (cur != null) cur]),
              child: Text(_hint, key: ValueKey(_hint), style: inter(13, c: linkError != null ? C.red : C.muted)),
            ),
          ),
          const SizedBox(height: 11),
          Padding(
            padding: EdgeInsets.zero,
            child: Pressable(
              onTap: ready ? _continue : null,
              child: AnimatedContainer(
                duration: const Duration(milliseconds: 160),
                height: 54,
                alignment: Alignment.center,
                decoration: BoxDecoration(color: ready ? C.tealBtn : const Color(0xFF3D7367), borderRadius: BorderRadius.circular(14)),
                child: Text('Continue', style: inter(17, w: FontWeight.w700, c: ready ? C.onTeal : const Color(0xFF1A2E29))),
              ),
            ),
          ),
        ]),
      ),
    );
  }
}

class _PlatformMenu extends StatefulWidget {
  final Rect anchor;
  final String selected;
  const _PlatformMenu({required this.anchor, required this.selected});
  @override
  State<_PlatformMenu> createState() => _PlatformMenuState();
}

class _PlatformMenuState extends State<_PlatformMenu> {
  final _sc = ScrollController();
  bool _more = false;

  @override
  void initState() {
    super.initState();
    _sc.addListener(_check);
    WidgetsBinding.instance.addPostFrameCallback((_) => _check());
  }

  void _check() {
    if (!_sc.hasClients) return;
    final more = _sc.position.extentAfter > 1;
    if (more != _more && mounted) setState(() => _more = more);
  }

  @override
  void dispose() {
    _sc.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final h = MediaQuery.of(context).size.height;
    final a = widget.anchor;
    final keys = _platforms.keys.toList();
    final anim = ModalRoute.of(context)?.animation ?? const AlwaysStoppedAnimation<double>(1);
    return Material(
      type: MaterialType.transparency,
      child: Stack(children: [
        Positioned(
          left: a.left, width: a.width, bottom: h - a.top + 6,
          child: FadeTransition(
            opacity: CurvedAnimation(parent: anim, curve: const Interval(0, .6, curve: Curves.easeOut), reverseCurve: Curves.easeIn),
            child: ScaleTransition(
              alignment: Alignment.bottomRight,
              scale: Tween(begin: .86, end: 1.0).animate(CurvedAnimation(parent: anim, curve: iosCurve, reverseCurve: Curves.easeIn)),
              child: Container(
            constraints: const BoxConstraints(maxHeight: 177),
            clipBehavior: Clip.antiAlias,
            decoration: BoxDecoration(
              color: const Color(0xFF3A3A3C),
              borderRadius: BorderRadius.circular(20),
              boxShadow: const [BoxShadow(color: Color(0x66000000), blurRadius: 12, offset: Offset(0, 6))],
            ),
            child: Column(mainAxisSize: MainAxisSize.min, children: [
              Flexible(
                child: ListView(
                  controller: _sc,
                  shrinkWrap: true,
                  padding: EdgeInsets.zero,
                  physics: const ClampingScrollPhysics(),
                  children: [
                    for (var i = 0; i < keys.length; i++) ...[
                      if (i > 0) Container(height: 1, color: const Color(0xFF4A4A4D)),
                      _item(keys[i]),
                    ],
                  ],
                ),
              ),
              if (_more)
                Container(
                  height: 29,
                  alignment: Alignment.center,
                  decoration: const BoxDecoration(border: Border(top: BorderSide(color: Color(0xFF4A4A4D)))),
                  child: const Icon(Icons.arrow_drop_down_rounded, size: 28, color: Colors.white),
                ),
            ]),
          ),
            ),
          ),
        ),
      ]),
    );
  }

  Widget _item(String key) {
    final on = key == widget.selected;
    return Pressable(
      scale: .98,
      onTap: () => Navigator.pop(context, key),
      child: Container(
        height: 50,
        padding: const EdgeInsets.symmetric(horizontal: 14),
        alignment: Alignment.centerLeft,
        child: Row(children: [
          // Name on the left; the BUGS tag sits at the right edge of the row.
          Expanded(
            child: Text(_platforms[key]!.$1,
                maxLines: 1, overflow: TextOverflow.ellipsis,
                style: inter(16, w: on ? FontWeight.w500 : FontWeight.w400, c: on ? C.teal : C.text)),
          ),
          if (_buggy.contains(key)) ...[
            const SizedBox(width: 8),
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 7, vertical: 3),
              decoration: BoxDecoration(color: const Color(0x40F5A524), borderRadius: BorderRadius.circular(8)),
              child: Text('BUGS', style: inter(13, w: FontWeight.w700, c: const Color(0xFFF5A524), h: 1.1)),
            ),
          ],
        ]),
      ),
    );
  }
}
