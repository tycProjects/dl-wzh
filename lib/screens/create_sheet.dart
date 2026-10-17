import 'dart:io';
import 'package:file_selector/file_selector.dart';
import 'package:flutter/material.dart';
import 'package:flutter/cupertino.dart' show CupertinoPageRoute;
import '../api.dart';
import '../theme.dart';
import '../widgets.dart';
import '../anim.dart';
import '../zip_util.dart';
import 'options_page.dart';

void showCreateSheet(BuildContext context) => showIosSheet(context, (_) => const _CreateSheet());

const _platforms = {
  'android': ('Android (APK / AAB)', 'Installable package'),
  'windows': ('Windows (EXE)', 'Desktop app · .exe installer'),
  'flutter': ('Flutter (APK)', 'Upload project zip · builds to APK'),
};

class _CreateSheet extends StatefulWidget {
  const _CreateSheet();
  @override
  State<_CreateSheet> createState() => _CreateSheetState();
}

class _CreateSheetState extends State<_CreateSheet> {
  int mode = 0; // 0 Upload, 1 Website
  int kind = 0; // 0 HTML, 1 ZIP
  String platform = 'android';
  final name = TextEditingController();
  final link = TextEditingController();
  XFile? picked;
  String? linkError;
  bool busy = false;

  bool get ready => !busy && (mode == 0 ? picked != null : link.text.trim().isNotEmpty);

  @override
  void dispose() {
    name.dispose();
    link.dispose();
    super.dispose();
  }

  Future<void> _pick() async {
    final html = kind == 0 && platform != 'flutter';
    final f = await openFile(acceptedTypeGroups: [
      XTypeGroup(label: html ? 'HTML' : 'ZIP', extensions: html ? ['html', 'htm'] : ['zip']),
    ]);
    if (f == null || !mounted) return;
    setState(() => picked = f);
  }

  Future<void> _choosePlatform() async {
    final r = await showIosSheet<String>(context, (_) => _PlatformSheet(selected: platform));
    if (r == null || !mounted) return;
    setState(() {
      platform = r;
      if (r == 'flutter') {
        mode = 0;
        kind = 1;
        picked = null;
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
    final base = BuildRequest(platform: platform);
    var source = '';
    var fallbackName = 'My app';
    setState(() => busy = true);
    try {
      if (mode == 0) {
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
      }
    } catch (e) {
      if (mounted) {
        setState(() => busy = false);
        showInfoDialog(context, title: 'Couldn\u2019t read that file', message: '$e', icon: Icons.error_outline_rounded, color: C.red);
      }
      return;
    }
    final n = name.text.trim().isEmpty ? fallbackName : name.text.trim();
    nav.pop();
    nav.push(CupertinoPageRoute(builder: (_) => OptionsPage(base: base, appName: n, source: source)));
  }

  Widget _field(TextEditingController c, String hint, {bool clear = false, double h = 54, double fs = 18, EdgeInsets? pad, TextInputType? kb}) {
    return Container(
      height: h,
      padding: pad ?? const EdgeInsets.only(left: 18, right: 14),
      decoration: BoxDecoration(color: C.field, borderRadius: BorderRadius.circular(h / 2.4)),
      child: Row(children: [
        Expanded(
          child: TextField(
            controller: c,
            keyboardType: kb,
            onChanged: (_) => setState(() => linkError = null),
            style: inter(fs),
            cursorColor: C.teal,
            decoration: InputDecoration(border: InputBorder.none, hintText: hint, hintStyle: inter(fs, c: C.muted), isCollapsed: true),
          ),
        ),
        if (clear && c.text.isNotEmpty)
          Pressable(
            onTap: () => setState(c.clear),
            child: Container(
              width: 20, height: 20,
              decoration: const BoxDecoration(color: Color(0xFF6B6B70), shape: BoxShape.circle),
              child: const Icon(Icons.close_rounded, size: 13, color: C.field),
            ),
          ),
      ]),
    );
  }

  Widget _row(String label, String sub, Widget ctrl) => Padding(
        padding: const EdgeInsets.only(top: 14),
        child: Row(children: [
          Expanded(
            child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              Text(label, style: inter(19, w: FontWeight.w700)),
              Text(sub, style: inter(14, c: C.muted)),
            ]),
          ),
          SizedBox(width: 208, height: 49, child: ctrl),
        ]),
      );

  Widget _pill(Widget child, {VoidCallback? onTap}) => Pressable(
        onTap: onTap,
        scale: .98,
        child: Container(
          padding: const EdgeInsets.symmetric(horizontal: 18),
          decoration: BoxDecoration(color: C.field, borderRadius: BorderRadius.circular(25)),
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
    final h = MediaQuery.of(context).size.height;
    final kb = MediaQuery.of(context).viewInsets.bottom;
    final flutter = platform == 'flutter';
    final html = kind == 0 && !flutter;
    return AnimatedPadding(
      duration: const Duration(milliseconds: 250),
      curve: iosCurve,
      padding: EdgeInsets.only(bottom: kb),
      child: Container(
        height: h * .67,
        decoration: const BoxDecoration(color: C.sheet, borderRadius: BorderRadius.vertical(top: Radius.circular(30))),
        padding: const EdgeInsets.fromLTRB(16, 34, 16, 17),
        child: Column(children: [
          Row(children: [
            Pressable(
              scale: .85,
              onTap: () => Navigator.pop(context),
              child: const Padding(padding: EdgeInsets.fromLTRB(6, 0, 16, 0), child: Icon(Icons.close_rounded, size: 28)),
            ),
            Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              Text('Create', style: inter(24, w: FontWeight.w700, h: 1.1)),
              const SizedBox(height: 3),
              Text('Step 1 of 2 · Add your app source', style: inter(14, c: C.muted)),
            ]),
          ]),
          const SizedBox(height: 18),
          Expanded(
            child: ListView(physics: const BouncingScrollPhysics(), padding: const EdgeInsets.symmetric(horizontal: 4, vertical: 4), children: [
              if (!flutter) ...[
                Seg(labels: const ['Upload', 'Website'], index: mode, onChanged: (i) => setState(() => mode = i)),
                const SizedBox(height: 18),
              ],
              _field(name, 'App name', clear: true),
              if (mode == 0 && !flutter) ...[
                const SizedBox(height: 16),
                Row(children: [
                  Expanded(child: BracketTile(title: 'HTML', sub: 'One .html file', on: kind == 0, onTap: () => setState(() { kind = 0; picked = null; }))),
                  const SizedBox(width: 10),
                  Expanded(child: BracketTile(title: 'ZIP', sub: 'Zipped project folder', on: kind == 1, onTap: () => setState(() { kind = 1; picked = null; }))),
                ]),
                const SizedBox(height: 6),
              ] else
                const SizedBox(height: 6),
              _row('Platform', 'What to build',
                  _pill(onTap: _choosePlatform, Row(mainAxisAlignment: MainAxisAlignment.spaceBetween, children: [
                    Expanded(child: Text(_platforms[platform]!.$1, overflow: TextOverflow.ellipsis, style: inter(17))),
                    const Icon(Icons.keyboard_arrow_down_rounded, size: 24),
                  ]))),
              if (mode == 0) ...[
                _row('File', html ? 'Your .html file' : (flutter ? 'Your Flutter project .zip' : 'Your .zip project'),
                    _pill(onTap: _pick,
                        Row(mainAxisAlignment: MainAxisAlignment.spaceBetween, children: [
                          Expanded(child: Text(picked?.name ?? 'Choose file', overflow: TextOverflow.ellipsis, style: inter(17, c: picked == null ? C.text : C.teal))),
                          const Icon(Icons.file_upload_outlined, size: 22),
                        ]))),
                const SizedBox(height: 22),
                Text('How to prep your file',
                    style: inter(17, c: const Color(0xFFB4B4B8)).copyWith(decoration: TextDecoration.underline, decorationColor: const Color(0xFFB4B4B8))),
              ] else ...[
                _row('Link', 'Loads live in the app',
                    _field(link, 'https://yoursite.com', h: 49, fs: 17, kb: TextInputType.url, pad: const EdgeInsets.symmetric(horizontal: 18))),
                const SizedBox(height: 22),
                Text('Your app opens this link live, so changes on your site show up automatically.', style: inter(16, c: C.muted, h: 1.35)),
              ],
            ]),
          ),
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 4),
            child: AnimatedSwitcher(
              duration: const Duration(milliseconds: 250),
              child: Text(_hint, key: ValueKey(_hint), style: inter(14, c: linkError != null ? C.red : C.muted)),
            ),
          ),
          const SizedBox(height: 12),
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 4),
            child: Pressable(
              onTap: ready ? _continue : null,
              child: AnimatedContainer(
                duration: const Duration(milliseconds: 200),
                height: 58,
                alignment: Alignment.center,
                decoration: BoxDecoration(color: ready ? C.tealBtn : const Color(0xFF3D7367), borderRadius: BorderRadius.circular(24)),
                child: Text('Continue', style: inter(20, w: FontWeight.w700, c: ready ? C.onTeal : const Color(0xFF1A2E29))),
              ),
            ),
          ),
        ]),
      ),
    );
  }
}

class _PlatformSheet extends StatelessWidget {
  final String selected;
  const _PlatformSheet({required this.selected});
  @override
  Widget build(BuildContext context) {
    return Container(
      padding: EdgeInsets.fromLTRB(20, 22, 20, 22 + MediaQuery.of(context).padding.bottom),
      decoration: const BoxDecoration(color: Color(0xFF181818), borderRadius: BorderRadius.vertical(top: Radius.circular(28))),
      child: Column(mainAxisSize: MainAxisSize.min, crossAxisAlignment: CrossAxisAlignment.start, children: [
        Text('Platform', style: inter(22, w: FontWeight.w700)),
        const SizedBox(height: 14),
        for (final e in _platforms.entries)
          Pressable(
            scale: .985,
            onTap: () => Navigator.pop(context, e.key),
            child: Container(
              margin: const EdgeInsets.only(bottom: 10),
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
              decoration: BoxDecoration(
                color: C.card, borderRadius: BorderRadius.circular(20),
                border: Border.all(color: selected == e.key ? C.teal : const Color(0xFF2B2B2D), width: selected == e.key ? 2 : 1)),
              child: Row(children: [
                Expanded(
                  child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                    Text(e.value.$1, style: inter(17, w: FontWeight.w600)),
                    const SizedBox(height: 2),
                    Text(e.value.$2, style: inter(14, c: C.muted)),
                  ]),
                ),
                if (selected == e.key) const Icon(Icons.check_rounded, color: C.teal),
              ]),
            ),
          ),
      ]),
    );
  }
}
