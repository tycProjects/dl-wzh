import 'dart:async';
import 'package:file_selector/file_selector.dart';
import 'package:flutter/cupertino.dart' show CupertinoPageRoute, CupertinoSliverRefreshControl;
import 'package:flutter/material.dart' hide Text;
import 'package:flutter/services.dart';
import 'package:url_launcher/url_launcher.dart';
import '../anim.dart';
import '../api.dart';
import '../banner.dart';
import 'gallery_picker.dart';
import '../theme.dart';
import '../widgets.dart';
import '../pick_apk.dart';
import '../tr.dart';
import 'hostshare_ui.dart';
import 'hs_cards.dart';
import 'hs_design.dart';
import 'share_detail.dart';
import 'dart:io';
import 'dart:ui' as ui;
import 'package:path_provider/path_provider.dart';
import 'package:qr/qr.dart';
import '../save_install.dart';

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
  bool _slow = false; // the server is taking a while (it sleeps when idle)
  Timer? _slowT;
  final Set<String> _seenLinks = {};

  // The details page listens to these so it stays up to date while a replace or delete runs.
  final _items$ = ValueNotifier<List<SharedApk>>(const []);
  final _busy$ = ValueNotifier<String>('');
  final _busyText$ = ValueNotifier<String>('');

  @override
  void dispose() {
    HsPlus.share.removeListener(_onPlus);
    _slowT?.cancel();
    _items$.dispose();
    _busy$.dispose();
    _busyText$.dispose();
    super.dispose();
  }

  void _onPlus() {
    if (widget.visible && _data != null) _upload();
  }

  void _publish() {
    _items$.value = _items;
    _busy$.value = _busySlug ?? '';
    _busyText$.value = _pct();
  }

  @override
  void setState(VoidCallback fn) {
    super.setState(fn);
    _publish();
  }

  @override
  void initState() {
    super.initState();
    HsPlus.share.addListener(_onPlus);
    // Paint the last known list right away; then refresh quietly in the background.
    _data = Api.i.sharedApksCached();
    _items$.value = _items;
    _load();
  }

  @override
  void didUpdateWidget(SharePage old) {
    super.didUpdateWidget(old);
    if (widget.visible && !old.visible && _busySlug == null) _load();
  }

  Future<void> _load() async {
    if (_data == null) {
      setState(() { _loading = true; _loadError = null; _slow = false; });
      _slowT?.cancel();
      _slowT = Timer(const Duration(seconds: 6), () {
        if (mounted && _data == null && _loadError == null) setState(() => _slow = true);
      });
    }
    try {
      final d = await Api.i.sharedApks();
      _slowT?.cancel();
      if (!mounted) return;
      setState(() { _data = d; _loading = false; _slow = false; });
    } catch (e) {
      _slowT?.cancel();
      if (!mounted) return;
      if (_data != null) return; // keep showing the saved list; no error screen over it
      setState(() { _slow = false; _loadError = e is ApiError ? e.message : 'Can’t reach the server. Check your connection.'; _loading = false; });
    }
  }

  int get _limit => _data?.limit ?? 3;
  List<SharedApk> get _items => _data?.items ?? const [];
  bool get _full => _items.length >= _limit;

  String _host(SharedApk s) => s.url.replaceFirst('https://', '');

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
      file = await pickApkFile();
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
      IosBanner.show(context, kind: BannerKind.ok, title: update == null ? 'Your link is live' : 'APK replaced',
          msg: update == null ? 'Link copied, ready to share: ${shared.url}' : 'The new APK is on the same link. Link copied: ${shared.url}');
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
        leave: 'Delete', destructive: true);
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
    if (mounted) IosBanner.show(context, kind: BannerKind.ok, title: 'Link copied', msg: s.url);
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

  void _openDetails(SharedApk s) {
    if (_busySlug != null && _busySlug != s.slug) return;
    Navigator.of(context).push(CupertinoPageRoute(
      builder: (_) => ShareDetailPage(
        slug: s.slug,
        items: _items$,
        busy: _busy$,
        busyText: _busyText$,
        onCopy: () => _copy(s),
        onOpen: () => _download(s),
        onReplace: () => _upload(update: s),
        onSaveQr: () => _saveQr(s),
        onDelete: () async {
          await _delete(s);
          if (mounted && !_items.any((e) => e.slug == s.slug) && Navigator.of(context).canPop()) Navigator.of(context).maybePop();
        },
      ),
    ));
  }

  String _pct() => (_progress != null && _progress! < 1) ? '$_busyLabel ${(_progress! * 100).round()}%' : _busyLabel;

  String _ago(SharedApk s) {
    final d = s.replaced ?? s.created;
    return d == null ? '' : hsAgo(d);
  }

  /// A shared APK as a card: name and address, downloads / size / age, then Copy link / Open / Replace.
  /// Tap the top of it for the details page; swipe it to the left to delete.
  Widget _card(SharedApk s) {
    final busy = _busySlug == s.slug;
    final ago = _ago(s);
    return HsSwipeDelete(
      id: s.slug,
      enabled: _busySlug == null,
      onDelete: () => _delete(s),
      child: HsCard(
        leading: HsAppIcon(s.name, logoUrl: s.logoUrl, size: 52),
        title: s.name,
        subtitle: _host(s),
        onTap: busy ? null : () => _openDetails(s),
        trailing: busy ? const SizedBox(width: 44, height: 44, child: Center(child: IosSpinner(size: 20))) : const HsChevron(),
        busy: busy,
        busyLabel: _pct(),
        progress: busy ? _progress : null,
        pills: [
          if (s.downloads >= 0)
            HsPill('${s.downloads} ${s.downloads == 1 ? 'download' : 'downloads'}', icon: Icons.download_rounded, color: C.teal, bg: kHsLiveBg),
          HsPill(_mb(s.sizeBytes), verbatim: true, icon: Icons.inventory_2_outlined),
          if (ago.isNotEmpty) HsPill('Updated $ago', verbatim: true, icon: Icons.schedule_rounded),
        ],
        quick: [
          HsQuick(Icons.content_copy_rounded, 'Copy link', () => _copy(s)),
          HsQuick(Icons.open_in_new_rounded, 'Open', () => _download(s)),
          HsQuick(Icons.sync_rounded, 'Replace', () => _upload(update: s)),
        ],
      ),
    );
  }

  // ---- Scan to install -------------------------------------------------------------------------

  Future<void> _saveQr(SharedApk s) async {
    try {
      final img = QrImage(QrCode.fromData(data: s.url, errorCorrectLevel: QrErrorCorrectLevel.M));
      const px = 1024.0, quiet = 72.0;
      final n = img.moduleCount;
      final cell = (px - quiet * 2) / n;
      final rec = ui.PictureRecorder();
      final c = Canvas(rec);
      c.drawRect(const Rect.fromLTWH(0, 0, px, px), Paint()..color = Colors.white);
      final p = Paint()..color = Colors.black;
      for (var r = 0; r < n; r++) {
        for (var k = 0; k < n; k++) {
          if (img.isDark(r, k)) c.drawRect(Rect.fromLTWH(quiet + k * cell, quiet + r * cell, cell + .5, cell + .5), p);
        }
      }
      final png = await (await rec.endRecording().toImage(px.toInt(), px.toInt())).toByteData(format: ui.ImageByteFormat.png);
      if (png == null) throw 'no image';
      final dir = await getTemporaryDirectory();
      final name = '${FileSaver.safe(s.name)}-QR.png';
      final f = File('${dir.path}/$name');
      await f.writeAsBytes(png.buffer.asUint8List(), flush: true);
      final shown = await FileSaver.saveToDownloads(f.path, s.name, fileName: name);
      if (!mounted) return;
      if (shown == null) {
        showInfoDialog(context, title: 'Couldn\u2019t save', message: 'The QR code couldn\u2019t be saved. Try again.', icon: Icons.error_outline_rounded, color: C.red);
      } else {
        IosBanner.show(context, kind: BannerKind.ok, title: 'QR code saved', msg: shown);
      }
    } catch (_) {
      if (mounted) showInfoDialog(context, title: 'Couldn\u2019t save', message: 'The QR code couldn\u2019t be saved. Try again.', icon: Icons.error_outline_rounded, color: C.red);
    }
  }

  Widget _qrCard(SharedApk s) => Container(
        padding: const EdgeInsets.all(12),
        decoration: BoxDecoration(color: kHsCard, borderRadius: BorderRadius.circular(18)),
        child: Row(crossAxisAlignment: CrossAxisAlignment.center, children: [
          HsQr(s.url, size: 92),
          const SizedBox(width: 14),
          Expanded(
            child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              Text(s.name, verbatim: true, maxLines: 2, overflow: TextOverflow.ellipsis, style: inter(16, w: FontWeight.w600)),
              const SizedBox(height: 3),
              Text('Point a camera at this code to open the download page.', style: inter(12.5, c: C.muted, h: 1.3)),
              const SizedBox(height: 8),
              Row(children: [
                Pressable(scale: .92, onTap: () => _copy(s), child: Text('Copy Link', style: inter(14, w: FontWeight.w500, c: kHsBlue))),
                const SizedBox(width: 18),
                Pressable(scale: .92, onTap: () => _saveQr(s), child: Text('Save', style: inter(14, w: FontWeight.w500, c: kHsBlue))),
              ]),
            ]),
          ),
        ]),
      );

  @override
  Widget build(BuildContext context) {
    final loadingFirst = _data == null && _loadError == null;
    final uploading = _busySlug == 'new';
    final children = <Widget>[
      HsHeroCard(
        icon: Icons.ios_share_rounded,
        title: 'Share an APK',
        subtitle: 'Get a download link anyone can open.',
        buttonLabel: 'Share an APK',
        used: _data == null ? null : _items.length,
        limit: _limit,
        busy: uploading,
        busyLabel: uploading ? (_progress != null && _progress! < 1 ? _pct() : 'Finishing\u2026') : '',
        progress: uploading ? _progress : null,
        onTap: () => _upload(),
      ),
    ];
    if (loadingFirst) {
      children.addAll([const HsListHeader('Your links'), const HsCardSkeleton(), if (_slow) const HsSlowHint()]);
    } else if (_data == null) {
      children.addAll([const SizedBox(height: 22), HsError(message: _loadError!, onRetry: _load)]);
    } else if (_items.isEmpty && !uploading) {
      children.add(const HsEmptyState(
        illus: 'empty_share',
        icon: Icons.ios_share_rounded,
        title: 'No links yet',
        message: 'Upload an APK and get a link. Anyone with the link can download it.',
      ));
    } else {
      children.add(HsListHeader('Your links', count: _items.length));
      if (uploading) children.add(const Padding(padding: EdgeInsets.only(bottom: 12), child: HsCardSkeleton()));
      for (var i = 0; i < _items.length; i++) {
        final k = _items[i].slug;
        // A card that was shown before (after a refresh, or when the one above it is deleted) doesn't animate again.
        final seen = _seenLinks.contains(k);
        _seenLinks.add(k);
        children.add(Padding(
          padding: const EdgeInsets.only(bottom: 12),
          child: HsAppear(key: ValueKey('link-$k'), index: seen ? -1 : i, child: _card(_items[i])),
        ));
      }
      children.add(const HsFootnote('Tap an app for its QR code and details. Swipe a link to the left to delete it.'));
    }
    final list = CustomScrollView(
      physics: const AlwaysScrollableScrollPhysics(parent: IosScrollPhysics()),
      slivers: [
        CupertinoSliverRefreshControl(onRefresh: _load),
        SliverPadding(
          padding: EdgeInsets.fromLTRB(17, widget.embedded ? 8 : 12, 17, 100),
          sliver: SliverList(delegate: SliverChildListDelegate(children)),
        ),
      ],
    );
    if (widget.embedded) return list;
    return Scaffold(
      backgroundColor: kHsBg,
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
    final f = await pickApkFile();
    _picking = false;
    if (f == null || !mounted) return;
    Navigator.pop(context, _ShareChoice(_name.text.trim(), '', f, _logo));
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: EdgeInsets.fromLTRB(16, 10, 16, 18 + MediaQuery.of(context).viewInsets.bottom + MediaQuery.of(context).padding.bottom),
      decoration: const BoxDecoration(color: C.sheet, borderRadius: BorderRadius.vertical(top: Radius.circular(30))),
      child: Column(mainAxisSize: MainAxisSize.min, crossAxisAlignment: CrossAxisAlignment.stretch, children: [
        Center(child: Container(width: 38, height: 5, decoration: BoxDecoration(color: const Color(0xFF4A4A4C), borderRadius: BorderRadius.circular(3)))),
        const SizedBox(height: 18),
        Row(children: [
          const HsIconTile(Icons.ios_share_rounded, size: 46),
          const SizedBox(width: 13),
          Expanded(
            child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              Text('Share an APK', style: inter(21, w: FontWeight.w700)),
              const SizedBox(height: 2),
              Text('Get a link anyone can download from', style: inter(13.5, c: C.muted)),
            ]),
          ),
        ]),
        const SizedBox(height: 18),
        Container(
          decoration: ShapeDecoration(color: C.field, shape: hsShape(18)),
          clipBehavior: Clip.antiAlias,
          child: Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 16),
              child: TextField(
                controller: _name,
                autofocus: true,
                maxLength: 40,
                style: inter(17),
                cursorColor: kHsBlue,
                textCapitalization: TextCapitalization.words,
                onChanged: (_) => setState(() {}),
                decoration: InputDecoration(
                  counterText: '',
                  hintText: 'App name',
                  hintStyle: inter(17, c: C.muted),
                  border: InputBorder.none,
                  contentPadding: const EdgeInsets.symmetric(vertical: 15),
                ),
              ),
            ),
            Container(height: .5, margin: const EdgeInsets.only(left: 16), color: const Color(0x1FFFFFFF)),
            Pressable(
              scale: .985,
              onTap: _pickLogo,
              child: Padding(
                padding: const EdgeInsets.fromLTRB(12, 10, 8, 10),
                child: Row(children: [
                  Container(
                    width: 44,
                    height: 44,
                    alignment: Alignment.center,
                    decoration: ShapeDecoration(color: const Color(0xFF262629), shape: hsShape(12)),
                    clipBehavior: Clip.antiAlias,
                    child: _logoBytes == null
                        ? Icon(Icons.add_photo_alternate_outlined, size: 24, color: kHsBlue)
                        : Image.memory(_logoBytes!, width: 44, height: 44, fit: BoxFit.cover),
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
                    )
                  else
                    const Icon(Icons.chevron_right_rounded, size: 22, color: Color(0xFF5A5A5E)),
                ]),
              ),
            ),
          ]),
        ),
        Padding(
          padding: const EdgeInsets.fromLTRB(6, 12, 6, 0),
          child: Row(children: [
            const Icon(Icons.link_rounded, size: 16, color: C.muted),
            const SizedBox(width: 6),
            Expanded(child: Text('Your link is made when the upload finishes.', style: inter(13, c: C.muted))),
          ]),
        ),
        const SizedBox(height: 16),
        Pressable(
          onTap: _choose,
          child: Opacity(
            opacity: _valid ? 1 : .45,
            child: Container(
              height: 54,
              alignment: Alignment.center,
              decoration: ShapeDecoration(color: C.tealBtn, shape: hsShape(27)),
              child: Row(mainAxisSize: MainAxisSize.min, children: [
                Icon(Icons.upload_file_rounded, size: 22, color: C.onTeal),
                const SizedBox(width: 9),
                Text('Choose the APK', style: inter(17, w: FontWeight.w700, c: C.onTeal)),
              ]),
            ),
          ),
        ),
        const SizedBox(height: 12),
        Text('Only share apps you made or may share.', textAlign: TextAlign.center, style: inter(12.5, c: C.muted)),
      ]),
    );
  }
}
