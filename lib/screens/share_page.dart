import 'package:file_selector/file_selector.dart';
import 'package:flutter/material.dart' hide Text;
import 'package:flutter/services.dart';
import 'package:url_launcher/url_launcher.dart';
import '../anim.dart';
import '../api.dart';
import '../banner.dart';
import 'gallery_picker.dart';
import '../theme.dart';
import '../widgets.dart';
import '../tr.dart';

/// Share tab: pick a finished .apk and get a public download link anyone can open.
/// Each person gets a few link slots; tap a link for Open / Copy / Replace APK / Delete.
class SharePage extends StatefulWidget {
  final bool embedded;
  final bool visible;
  const SharePage({super.key, this.embedded = false, this.visible = true});
  @override
  State<SharePage> createState() => _SharePageState();
}

class _ShareChoice {
  final String name, slug;
  final XFile file;
  final XFile? logo;
  _ShareChoice(this.name, this.slug, this.file, [this.logo]);
}

const _kApkTypes = XTypeGroup(
  label: 'Android app (apk)',
  extensions: ['apk'],
  mimeTypes: ['application/vnd.android.package-archive', 'application/octet-stream'],
);
const _kImgTypes = XTypeGroup(
  label: 'Picture',
  extensions: ['png', 'jpg', 'jpeg', 'webp'],
  mimeTypes: ['image/png', 'image/jpeg', 'image/webp'],
);
String _mb(int bytes) => bytes >= 1048576 ? '${(bytes / 1048576).toStringAsFixed(1)} MB' : '${(bytes / 1024).ceil()} KB';

class _SharePageState extends State<SharePage> {
  SharedApks? _data;
  String? _loadError;
  bool _loading = true;
  String? _busySlug; // 'new' while uploading a new link, or the slug being replaced / deleted
  String _busyLabel = '';
  double? _progress; // 0..1 while an upload is going out

  @override
  void initState() {
    super.initState();
    if (widget.visible) _load();
  }

  @override
  void didUpdateWidget(SharePage old) {
    super.didUpdateWidget(old);
    if (widget.visible && !old.visible && _busySlug == null) _load();
  }

  Future<void> _load() async {
    setState(() { _loading = true; _loadError = null; });
    try {
      final d = await Api.i.sharedApks();
      if (!mounted) return;
      setState(() { _data = d; _loading = false; });
    } catch (e) {
      if (!mounted) return;
      setState(() { _loadError = e is ApiError ? e.message : 'Can’t reach the server. Check your connection.'; _loading = false; });
    }
  }

  int get _limit => _data?.limit ?? 3;
  List<SharedApk> get _items => _data?.items ?? const [];
  bool get _full => _items.length >= _limit;

  String _host(SharedApk s) => s.url.replaceFirst('https://', '');
  String _meta(SharedApk s) => s.downloads < 0 ? _mb(s.sizeBytes) : '${_mb(s.sizeBytes)} · ${s.downloads} ${s.downloads == 1 ? 'download' : 'downloads'}';

  /// New link: the sheet asks for names, then the file. Replace: just the file.
  Future<void> _upload({SharedApk? update}) async {
    if (_busySlug != null) return;
    String slug, name;
    XFile? file, logo;
    if (update == null) {
      if (_full) {
        showInfoDialog(context, title: 'Limit reached', message: 'You can have $_limit links. Delete one to add another.', icon: Icons.lock_outline_rounded, color: C.red);
        return;
      }
      final choice = await showIosSheet<_ShareChoice>(context, (_) => _ShareSheet(base: _data?.base ?? ''));
      if (choice == null || !mounted) return;
      slug = choice.slug;
      name = choice.name;
      file = choice.file;
      logo = choice.logo;
    } else {
      file = await openFile(acceptedTypeGroups: [_kApkTypes]);
      if (file == null || !mounted) return;
      slug = update.slug;
      name = update.name;
    }
    final maxMb = _data?.maxMb ?? 100;
    final size = await file.length();
    if (!mounted) return;
    if (size > maxMb * 1024 * 1024) {
      showInfoDialog(context, title: 'File too big', message: 'That APK is ${_mb(size)}. The limit is $maxMb MB.', icon: Icons.error_outline_rounded, color: C.red);
      return;
    }
    setState(() {
      _busySlug = update?.slug ?? 'new';
      _busyLabel = update == null ? 'Uploading…' : 'Replacing…';
      _progress = 0;
    });
    try {
      final shared = await Api.i.shareApk(file.path, file.name, slug: slug, name: name, update: update != null, logoPath: logo?.path, onProgress: (sent, total) {
        if (!mounted || total <= 0) return;
        final p = (sent / total).clamp(0.0, 1.0);
        if (_progress == null || p - _progress! >= .01 || p >= 1) setState(() => _progress = p);
      });
      await Clipboard.setData(ClipboardData(text: shared.url));
      if (!mounted) return;
      await _load();
      if (!mounted) return;
      IosBanner.show(context, title: update == null ? 'Your link is live' : 'APK replaced', msg: 'Link copied: ${shared.url}');
    } catch (e) {
      if (!mounted) return;
      final limitHit = e is ApiError && e.code == 'share_limit';
      showInfoDialog(context,
          title: limitHit ? 'Limit reached' : 'Upload didn’t work',
          message: e is ApiError ? e.message : 'Something broke. Try again.',
          icon: limitHit ? Icons.lock_outline_rounded : Icons.error_outline_rounded,
          color: C.red);
      if (limitHit) _load();
    } finally {
      if (mounted) setState(() { _busySlug = null; _progress = null; });
    }
  }

  Future<void> _delete(SharedApk s) async {
    if (_busySlug != null) return;
    final ok = await confirmLeave(context,
        title: 'Delete this link?',
        message: '${s.url} stops working for everyone. You can make another one after.',
        stay: 'Cancel',
        leave: 'Delete');
    if (!ok || !mounted) return;
    setState(() { _busySlug = s.slug; _busyLabel = 'Deleting…'; _progress = null; });
    try {
      await Api.i.deleteSharedApk(s.slug);
      if (!mounted) return;
      await _load();
    } catch (e) {
      if (!mounted) return;
      showInfoDialog(context, title: 'Couldn’t delete', message: e is ApiError ? e.message : 'Try again in a sec.', icon: Icons.error_outline_rounded, color: C.red);
    } finally {
      if (mounted) setState(() => _busySlug = null);
    }
  }

  Future<void> _copy(SharedApk s) async {
    await Clipboard.setData(ClipboardData(text: s.url));
    if (mounted) IosBanner.show(context, title: 'Link copied', msg: s.url);
  }

  Future<void> _open(SharedApk s) async {
    try {
      await launchUrl(Uri.parse(s.url), mode: LaunchMode.externalApplication);
    } catch (_) {
      if (mounted) showInfoDialog(context, title: 'Couldn’t open that', message: s.url);
    }
  }

  Future<void> _download(SharedApk s) async {
    Api.i.hitSharedApk(s.slug).then((_) {
      if (mounted) _load();
    });
    await _open(s);
  }

  Future<void> _actions(SharedApk s) async {
    if (_busySlug != null) return;
    final pick = await showIosSheet<String>(context, (ctx) {
      Widget row(IconData icon, String text, String value, {Color? color, bool last = false}) => Pressable(
            scale: .985,
            onTap: () => Navigator.pop(ctx, value),
            child: Container(
              height: 56,
              padding: const EdgeInsets.symmetric(horizontal: 16),
              decoration: BoxDecoration(border: last ? null : const Border(bottom: BorderSide(color: Color(0xFF333336)))),
              child: Row(children: [
                Icon(icon, size: 22, color: color ?? C.teal),
                const SizedBox(width: 14),
                Text(text, style: inter(17, c: color ?? C.text)),
              ]),
            ),
          );
      Widget chip(IconData icon, String text) => Container(
            padding: const EdgeInsets.symmetric(horizontal: 11, vertical: 6),
            decoration: BoxDecoration(color: const Color(0xFF1E1E20), borderRadius: BorderRadius.circular(20), border: Border.all(color: const Color(0xFF333336))),
            child: Row(mainAxisSize: MainAxisSize.min, children: [
              Icon(icon, size: 15, color: C.teal),
              const SizedBox(width: 5),
              Text(text, style: inter(12.5, c: const Color(0xFFC5CDD1))),
            ]),
          );
      return Container(
        width: double.infinity,
        padding: EdgeInsets.fromLTRB(16, 10, 16, 14 + MediaQuery.of(ctx).padding.bottom),
        decoration: const BoxDecoration(color: C.sheet, borderRadius: BorderRadius.vertical(top: Radius.circular(30))),
        child: Column(mainAxisSize: MainAxisSize.min, crossAxisAlignment: CrossAxisAlignment.stretch, children: [
          Center(child: Container(width: 38, height: 5, decoration: BoxDecoration(color: const Color(0xFF4A4A4C), borderRadius: BorderRadius.circular(3)))),
          const SizedBox(height: 16),
          Center(
            child: Container(
              width: 72, height: 72,
              alignment: Alignment.center,
              decoration: BoxDecoration(color: C.tealDeep, borderRadius: BorderRadius.circular(22), border: Border.all(color: const Color(0xFF1F4A41))),
              child: Text(s.name.trim().isEmpty ? 'A' : s.name.trim().substring(0, 1).toUpperCase(), style: inter(32, w: FontWeight.w700, c: C.teal)),
            ),
          ),
          const SizedBox(height: 12),
          Text(s.name, textAlign: TextAlign.center, maxLines: 1, overflow: TextOverflow.ellipsis, style: inter(20, w: FontWeight.w700)),
          const SizedBox(height: 10),
          Wrap(alignment: WrapAlignment.center, spacing: 8, runSpacing: 8, children: [
            chip(Icons.android_rounded, 'Android'),
            chip(Icons.insert_drive_file_outlined, _mb(s.sizeBytes)),
            if (s.downloads >= 0) chip(Icons.download_rounded, '${s.downloads} ${s.downloads == 1 ? 'download' : 'downloads'}'),
          ]),
          const SizedBox(height: 16),
          Pressable(
            scale: .98,
            onTap: () => Navigator.pop(ctx, 'download'),
            child: Container(
              height: 54,
              alignment: Alignment.center,
              decoration: BoxDecoration(color: C.tealBtn, borderRadius: BorderRadius.circular(27)),
              child: Row(mainAxisSize: MainAxisSize.min, children: [
                const Icon(Icons.download_rounded, size: 22, color: C.onTeal),
                const SizedBox(width: 8),
                Text('Download APK', style: inter(17, w: FontWeight.w700, c: C.onTeal)),
              ]),
            ),
          ),
          const SizedBox(height: 14),
          Container(
            decoration: BoxDecoration(color: const Color(0xFF1E1E20), borderRadius: BorderRadius.circular(20)),
            child: Column(children: [
              row(Icons.copy_rounded, 'Copy link', 'copy'),
              row(Icons.upload_rounded, 'Replace the APK', 'update'),
              row(Icons.delete_outline_rounded, 'Delete link', 'delete', color: const Color(0xFFF27D85), last: true),
            ]),
          ),
        ]),
      );
    });
    if (!mounted || pick == null) return;
    switch (pick) {
      case 'download': await _download(s); break;
      case 'copy': await _copy(s); break;
      case 'update': await _upload(update: s); break;
      case 'delete': await _delete(s); break;
    }
  }

  Widget _counter() {
    final used = _items.length;
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
      decoration: BoxDecoration(color: C.card, borderRadius: BorderRadius.circular(14)),
      child: Row(mainAxisSize: MainAxisSize.min, children: [
        for (var i = 0; i < _limit; i++) ...[
          Container(width: 6, height: 6, decoration: BoxDecoration(color: i < used ? C.teal : const Color(0xFF3A3A3C), shape: BoxShape.circle)),
          const SizedBox(width: 3),
        ],
        const SizedBox(width: 3),
        Text('$used of $_limit', style: inter(12.5, c: C.muted)),
      ]),
    );
  }

  Widget _progressBar() => Padding(
        padding: const EdgeInsets.only(top: 7, right: 12),
        child: ClipRRect(
          borderRadius: BorderRadius.circular(3),
          child: LinearProgressIndicator(
            value: (_progress ?? 0) >= 1 ? null : _progress,
            minHeight: 4,
            color: C.teal,
            backgroundColor: const Color(0xFF3A3A3C),
          ),
        ),
      );

  Widget _row(SharedApk s, {required bool last}) {
    final busy = _busySlug == s.slug;
    return Pressable(
      scale: .985,
      onTap: () => _actions(s),
      child: Container(
        padding: const EdgeInsets.fromLTRB(16, 13, 6, 13),
        decoration: BoxDecoration(border: last ? null : const Border(bottom: BorderSide(color: Color(0xFF2A2A2C)))),
        child: Row(children: [
          Expanded(
            child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              Text(s.name, maxLines: 1, overflow: TextOverflow.ellipsis, style: inter(17, w: FontWeight.w500)),
              const SizedBox(height: 2),
              Text(busy ? (_progress != null && _progress! < 1 ? '$_busyLabel ${(_progress! * 100).round()}%' : _busyLabel) : _host(s),
                  maxLines: 1, overflow: TextOverflow.ellipsis, style: inter(13, c: busy ? C.teal : C.muted)),
              if (!busy) ...[
                const SizedBox(height: 1),
                Text(_meta(s), maxLines: 1, overflow: TextOverflow.ellipsis, style: inter(12.5, c: C.muted)),
              ],
              if (busy && _progress != null) _progressBar(),
            ]),
          ),
          if (busy && _progress == null)
            const Padding(padding: EdgeInsets.only(right: 12), child: IosSpinner(size: 20))
          else if (!busy) ...[
            Pressable(
              scale: .85,
              onTap: () => _copy(s),
              child: const SizedBox(width: 40, height: 40, child: Icon(Icons.copy_rounded, size: 20, color: C.teal)),
            ),
            Pressable(
              scale: .85,
              onTap: () => _actions(s),
              child: const SizedBox(width: 40, height: 40, child: Icon(Icons.more_horiz_rounded, size: 24, color: C.muted)),
            ),
          ],
        ]),
      ),
    );
  }

  Widget _slotRow() {
    final uploading = _busySlug == 'new';
    final free = _limit - _items.length;
    return Pressable(
      scale: .985,
      onTap: () => _upload(),
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 11),
        child: Row(children: [
          DashedRRect(
            radius: 12,
            color: const Color(0xFF5A5A5E),
            child: SizedBox(
              width: 38, height: 38,
              child: Center(
                child: uploading ? const IosSpinner(size: 18) : const Icon(Icons.add_rounded, size: 22, color: C.teal),
              ),
            ),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              Text('Share an APK', style: inter(17)),
              const SizedBox(height: 1),
              Text(
                  uploading
                      ? (_progress != null && _progress! < 1 ? '$_busyLabel ${(_progress! * 100).round()}%' : 'Finishing…')
                      : '$free free ${free == 1 ? 'slot' : 'slots'}',
                  style: inter(13, c: uploading ? C.teal : C.muted)),
              if (uploading && _progress != null) _progressBar(),
            ]),
          ),
          if (!uploading) const Icon(Icons.chevron_right_rounded, size: 24, color: Color(0xFF5A5A5E)),
        ]),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final children = <Widget>[];
    if (_data == null && _loadError == null) {
      children.add(const Padding(padding: EdgeInsets.only(top: 80), child: Center(child: IosSpinner(size: 26))));
    } else if (_data == null) {
      children.addAll([
        const SizedBox(height: 60),
        Center(child: Text(_loadError!, textAlign: TextAlign.center, style: inter(14, c: C.muted, h: 1.4))),
        const SizedBox(height: 14),
        Center(
          child: Pressable(
            onTap: _load,
            scale: .92,
            child: Padding(padding: const EdgeInsets.all(8), child: Text('Try again', style: inter(15, w: FontWeight.w500, c: C.teal))),
          ),
        ),
      ]);
    } else {
      final rows = <Widget>[
        for (var i = 0; i < _items.length; i++) _row(_items[i], last: _full && i == _items.length - 1),
        if (!_full) ...[
          if (_items.isNotEmpty) Container(height: 1, color: const Color(0xFF2A2A2C)),
          _slotRow(),
        ],
      ];
      children.addAll([
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 3),
          child: Row(children: [
            Text('Share APK', style: inter(22, w: FontWeight.w500)),
            const Spacer(),
            _counter(),
          ]),
        ),
        const SizedBox(height: 12),
        Container(
          decoration: BoxDecoration(color: C.card, borderRadius: BorderRadius.circular(20)),
          child: Column(children: rows),
        ),
        Padding(
          padding: const EdgeInsets.fromLTRB(6, 12, 6, 0),
          child: Text(
            _full
                ? 'All $_limit links are used. Delete one to share another APK.'
                : (_items.isEmpty
                    ? 'Upload an APK and get a link. Anyone with the link can download it.'
                    : 'Tap a link for Download, Copy, Replace the APK and Delete. Delete a link to free its slot.'),
            style: inter(13, c: C.muted, h: 1.4),
          ),
        ),
      ]);
    }
    final list = RefreshIndicator(
      onRefresh: _load,
      color: C.teal,
      backgroundColor: C.card,
      child: ListView(
        physics: const AlwaysScrollableScrollPhysics(parent: BouncingScrollPhysics()),
        padding: EdgeInsets.fromLTRB(17, widget.embedded ? 22 : 12, 17, 100),
        children: children,
      ),
    );
    if (widget.embedded) return list;
    return Scaffold(
      backgroundColor: C.bg,
      body: SafeArea(
        child: Column(children: [
          Container(
            height: 54,
            alignment: Alignment.centerLeft,
            padding: const EdgeInsets.symmetric(horizontal: 6),
            child: SizedBox(
              width: 44, height: 44,
              child: Pressable(
                onTap: () => Navigator.of(context).maybePop(),
                scale: .88,
                child: const Center(child: Icon(Icons.arrow_back_ios_new_rounded, size: 22, color: Color(0xFFE8E8E8))),
              ),
            ),
          ),
          Expanded(child: list),
        ]),
      ),
    );
  }
}

/// One-step sheet: app name + link name (the link shows live), then choose the APK.
class _ShareSheet extends StatefulWidget {
  final String base;
  const _ShareSheet({required this.base});
  @override
  State<_ShareSheet> createState() => _ShareSheetState();
}

class _ShareSheetState extends State<_ShareSheet> {
  final _name = TextEditingController();
  bool _picking = false;
  XFile? _logo;
  Uint8List? _logoBytes;

  Future<void> _pickLogo() async {
    if (_picking) return;
    _picking = true;
    FocusManager.instance.primaryFocus?.unfocus();
    final picked = await pickIconImage(context);
    _picking = false;
    if (picked == null || !mounted) return;
    final f = XFile(picked.path, name: picked.name);
    final bytes = await f.readAsBytes();
    if (!mounted) return;
    setState(() { _logo = f; _logoBytes = bytes; });
  }

  @override
  void dispose() {
    _name.dispose();
    super.dispose();
  }

  bool get _valid => _name.text.trim().isNotEmpty;

  Future<void> _choose() async {
    if (!_valid || _picking) return;
    _picking = true;
    final f = await openFile(acceptedTypeGroups: [_kApkTypes]);
    _picking = false;
    if (f == null || !mounted) return;
    Navigator.pop(context, _ShareChoice(_name.text.trim(), '', f, _logo));
  }

  InputDecoration _deco(String hint) => InputDecoration(
        counterText: '',
        hintText: hint,
        hintStyle: inter(17, c: C.muted),
        filled: true,
        fillColor: C.field,
        contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
        border: OutlineInputBorder(borderRadius: BorderRadius.circular(15), borderSide: BorderSide.none),
      );

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: EdgeInsets.fromLTRB(16, 10, 16, 18 + MediaQuery.of(context).viewInsets.bottom + MediaQuery.of(context).padding.bottom),
      decoration: const BoxDecoration(color: C.sheet, borderRadius: BorderRadius.vertical(top: Radius.circular(30))),
      child: Column(mainAxisSize: MainAxisSize.min, crossAxisAlignment: CrossAxisAlignment.stretch, children: [
        Center(child: Container(width: 38, height: 5, decoration: BoxDecoration(color: const Color(0xFF4A4A4C), borderRadius: BorderRadius.circular(3)))),
        const SizedBox(height: 18),
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 4),
          child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            Text('Share an APK', style: inter(21, w: FontWeight.w700)),
            const SizedBox(height: 2),
            Text('Get a link anyone can download from', style: inter(13.5, c: C.muted)),
          ]),
        ),
        const SizedBox(height: 16),
        TextField(
          controller: _name,
          autofocus: true,
          maxLength: 40,
          style: inter(17),
          cursorColor: C.teal,
          textCapitalization: TextCapitalization.words,
          onChanged: (_) => setState(() {}),
          decoration: _deco('App name'),
        ),
        const SizedBox(height: 12),
        Row(children: [
          Pressable(
            scale: .94,
            onTap: _pickLogo,
            child: ClipRRect(
              borderRadius: BorderRadius.circular(15),
              child: Container(
                width: 56, height: 56,
                color: C.field,
                alignment: Alignment.center,
                child: _logoBytes == null
                    ? const Icon(Icons.add_photo_alternate_outlined, size: 26, color: C.teal)
                    : Image.memory(_logoBytes!, width: 56, height: 56, fit: BoxFit.cover),
              ),
            ),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              Text('App logo', style: inter(16)),
              const SizedBox(height: 1),
              Text(_logo == null ? 'Optional. Shown on the download page.' : 'Tap the picture to change it.', style: inter(13, c: C.muted)),
            ]),
          ),
          if (_logo != null)
            Pressable(
              scale: .85,
              onTap: () => setState(() { _logo = null; _logoBytes = null; }),
              child: const SizedBox(width: 40, height: 40, child: Icon(Icons.close_rounded, size: 22, color: C.muted)),
            ),
        ]),
        Padding(
          padding: const EdgeInsets.fromLTRB(4, 12, 4, 0),
          child: Text('Your link is made when the upload finishes.', style: inter(13.5, c: C.muted)),
        ),
        const SizedBox(height: 14),
        Pressable(
          onTap: _choose,
          child: Opacity(
            opacity: _valid ? 1 : .45,
            child: Container(
              height: 52, alignment: Alignment.center,
              decoration: BoxDecoration(color: C.tealBtn, borderRadius: BorderRadius.circular(26)),
              child: Text('Choose the APK', style: inter(17, w: FontWeight.w700, c: C.onTeal)),
            ),
          ),
        ),
        const SizedBox(height: 12),
        Text('Only share apps you made or may share.', textAlign: TextAlign.center, style: inter(12.5, c: C.muted)),
      ]),
    );
  }
}
