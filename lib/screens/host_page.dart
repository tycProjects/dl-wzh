import 'package:file_selector/file_selector.dart';
import 'package:flutter/material.dart' hide Text;
import 'package:flutter/services.dart';
import 'package:url_launcher/url_launcher.dart';
import '../anim.dart';
import '../api.dart';
import '../banner.dart';
import '../theme.dart';
import '../widgets.dart';
import '../tr.dart';

/// Host tab: each person gets [limit] site slots. Tap the free slot to name a site and pick a
/// .zip or .html; tap a site for Open / Update files / Delete. Deleting a site frees its slot.
class HostPage extends StatefulWidget {
  /// [embedded] = shown as a tab (no back arrow). [visible] = the tab is the one on screen;
  /// the sites load the first time it is, and refresh whenever it comes back.
  final bool embedded;
  final bool visible;
  const HostPage({super.key, this.embedded = false, this.visible = true});
  @override
  State<HostPage> createState() => _HostPageState();
}

class _HostChoice {
  final String name;
  final XFile file;
  _HostChoice(this.name, this.file);
}

const _kFileTypes = XTypeGroup(label: 'Website (zip or html)', extensions: ['zip', 'html', 'htm']);
final _kNameOk = RegExp(r'^[a-z0-9][a-z0-9-]{1,28}[a-z0-9]$');

class _HostPageState extends State<HostPage> {
  HostedSites? _data;
  String? _loadError;
  bool _loading = true;
  String? _busyName; // 'new' while publishing a new site, or the name of the site being updated/deleted
  String _busyLabel = '';

  @override
  void initState() {
    super.initState();
    if (widget.visible) _load();
  }

  @override
  void didUpdateWidget(HostPage old) {
    super.didUpdateWidget(old);
    if (widget.visible && !old.visible && _busyName == null) _load();
  }

  Future<void> _load() async {
    setState(() { _loading = true; _loadError = null; });
    try {
      final d = await Api.i.hostedSites();
      if (!mounted) return;
      setState(() { _data = d; _loading = false; });
    } catch (e) {
      if (!mounted) return;
      setState(() { _loadError = e is ApiError ? e.message : 'Can\u2019t reach the server. Check your connection.'; _loading = false; });
    }
  }

  int get _limit => _data?.limit ?? 2;
  List<HostedSite> get _sites => _data?.sites ?? const [];
  bool get _full => _sites.length >= _limit;

  // "myshop-a1b2c3" -> "myshop" (the short code at the end is the server's, not the user's)
  String _title(HostedSite s) => s.name.replaceFirst(RegExp(r'-[a-f0-9]{6}$'), '');
  String _host(HostedSite s) => s.url.replaceFirst('https://', '');

  /// New site: the sheet asks for a name and the file in one go. Update: just the file.
  Future<void> _upload({HostedSite? update}) async {
    if (_busyName != null) return;
    String? siteName;
    XFile? file;
    if (update == null) {
      if (_full) {
        showInfoDialog(context, title: 'Limit reached', message: 'You can host $_limit sites. Delete one to host another.', icon: Icons.lock_outline_rounded, color: C.red);
        return;
      }
      final choice = await showIosSheet<_HostChoice>(context, (_) => _HostSheet(suffix: _data?.suffix ?? ''));
      if (choice == null || !mounted) return;
      siteName = choice.name;
      file = choice.file;
    } else {
      file = await openFile(acceptedTypeGroups: [_kFileTypes]);
      if (file == null || !mounted) return;
    }
    setState(() { _busyName = update?.name ?? 'new'; _busyLabel = update == null ? 'Putting your site online\u2026' : 'Updating your site\u2026'; });
    try {
      final site = await Api.i.hostSite(file.path, file.name, updateName: update?.name, siteName: siteName);
      await Clipboard.setData(ClipboardData(text: site.url));
      if (!mounted) return;
      await _load();
      if (!mounted) return;
      IosBanner.show(context, title: update == null ? 'Your site is live' : 'Site updated', msg: 'Link copied: ${site.url}');
    } catch (e) {
      if (!mounted) return;
      final limitHit = e is ApiError && e.code == 'site_limit';
      showInfoDialog(context,
          title: limitHit ? 'Limit reached' : 'Hosting didn\u2019t work',
          message: e is ApiError ? e.message : 'Something broke. Try again.',
          icon: limitHit ? Icons.lock_outline_rounded : Icons.error_outline_rounded,
          color: C.red);
      if (limitHit) _load();
    } finally {
      if (mounted) setState(() => _busyName = null);
    }
  }

  Future<void> _delete(HostedSite s) async {
    if (_busyName != null) return;
    final ok = await confirmLeave(context,
        title: 'Delete this site?',
        message: '${s.url} goes offline for good. You can host another one after.',
        stay: 'Cancel',
        leave: 'Delete');
    if (!ok || !mounted) return;
    setState(() { _busyName = s.name; _busyLabel = 'Deleting\u2026'; });
    try {
      await Api.i.deleteHostedSite(s.name);
      if (!mounted) return;
      await _load();
    } catch (e) {
      if (!mounted) return;
      showInfoDialog(context, title: 'Couldn\u2019t delete', message: e is ApiError ? e.message : 'Try again in a sec.', icon: Icons.error_outline_rounded, color: C.red);
    } finally {
      if (mounted) setState(() => _busyName = null);
    }
  }

  Future<void> _copy(HostedSite s) async {
    await Clipboard.setData(ClipboardData(text: s.url));
    if (mounted) IosBanner.show(context, title: 'Link copied', msg: s.url);
  }

  Future<void> _open(HostedSite s) async {
    try {
      await launchUrl(Uri.parse(s.url), mode: LaunchMode.externalApplication);
    } catch (_) {
      if (mounted) showInfoDialog(context, title: 'Couldn\u2019t open that', message: s.url);
    }
  }

  /// Same sheet style as the rest of the app.
  Future<void> _actions(HostedSite s) async {
    if (_busyName != null) return;
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
      return Container(
        width: double.infinity,
        padding: EdgeInsets.fromLTRB(16, 10, 16, 14 + MediaQuery.of(ctx).padding.bottom),
        decoration: const BoxDecoration(color: C.sheet, borderRadius: BorderRadius.vertical(top: Radius.circular(30))),
        child: Column(mainAxisSize: MainAxisSize.min, crossAxisAlignment: CrossAxisAlignment.stretch, children: [
          Center(child: Container(width: 38, height: 5, decoration: BoxDecoration(color: const Color(0xFF4A4A4C), borderRadius: BorderRadius.circular(3)))),
          const SizedBox(height: 16),
          Text(_title(s), textAlign: TextAlign.center, style: inter(18, w: FontWeight.w700)),
          const SizedBox(height: 3),
          Text(_host(s), textAlign: TextAlign.center, maxLines: 1, overflow: TextOverflow.ellipsis, style: inter(13.5, c: C.muted)),
          const SizedBox(height: 14),
          Container(
            decoration: BoxDecoration(color: const Color(0xFF1E1E20), borderRadius: BorderRadius.circular(20)),
            child: Column(children: [
              row(Icons.open_in_new_rounded, 'Open in browser', 'open'),
              row(Icons.upload_rounded, 'Update files', 'update'),
              row(Icons.delete_outline_rounded, 'Delete site', 'delete', color: const Color(0xFFF27D85), last: true),
            ]),
          ),
        ]),
      );
    });
    if (!mounted || pick == null) return;
    switch (pick) {
      case 'open': await _open(s); break;
      case 'update': await _upload(update: s); break;
      case 'delete': await _delete(s); break;
    }
  }

  Widget _counter() {
    final used = _sites.length;
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

  Widget _siteRow(HostedSite s, {required bool last}) {
    final busy = _busyName == s.name;
    return Pressable(
      scale: .985,
      onTap: () => _actions(s),
      child: Container(
        padding: const EdgeInsets.fromLTRB(16, 13, 6, 13),
        decoration: BoxDecoration(border: last ? null : const Border(bottom: BorderSide(color: Color(0xFF2A2A2C)))),
        child: Row(children: [
          Expanded(
            child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              Text(_title(s), maxLines: 1, overflow: TextOverflow.ellipsis, style: inter(17, w: FontWeight.w500)),
              const SizedBox(height: 2),
              Text(busy ? _busyLabel : _host(s), maxLines: 1, overflow: TextOverflow.ellipsis, style: inter(13, c: C.muted)),
            ]),
          ),
          if (busy)
            const Padding(padding: EdgeInsets.only(right: 12), child: IosSpinner(size: 20))
          else ...[
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
    final publishing = _busyName == 'new';
    final free = _limit - _sites.length;
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
                child: publishing ? const IosSpinner(size: 18) : const Icon(Icons.add_rounded, size: 22, color: C.teal),
              ),
            ),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              Text('Host a site', style: inter(17)),
              const SizedBox(height: 1),
              Text(publishing ? _busyLabel : '$free free ${free == 1 ? 'slot' : 'slots'}', style: inter(13, c: C.muted)),
            ]),
          ),
          if (!publishing) const Icon(Icons.chevron_right_rounded, size: 24, color: Color(0xFF5A5A5E)),
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
        for (var i = 0; i < _sites.length; i++) _siteRow(_sites[i], last: _full && i == _sites.length - 1),
        if (!_full) ...[
          if (_sites.isNotEmpty) Container(height: 1, color: const Color(0xFF2A2A2C)),
          _slotRow(),
        ],
      ];
      children.addAll([
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 3),
          child: Row(children: [
            Text('Hosting', style: inter(22, w: FontWeight.w500)),
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
                ? 'All $_limit slots are taken. Delete a site to host another.'
                : (_sites.isEmpty
                    ? 'Upload a zip or an html file and get a free live link.'
                    : 'Tap the dots for Open, Update files and Delete. Delete a site to free its slot.'),
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

/// One-step sheet: type the name (the link shows live), then choose the zip or html.
class _HostSheet extends StatefulWidget {
  final String suffix;
  const _HostSheet({required this.suffix});
  @override
  State<_HostSheet> createState() => _HostSheetState();
}

class _HostSheetState extends State<_HostSheet> {
  final _ctrl = TextEditingController();
  bool _picking = false;

  @override
  void dispose() {
    _ctrl.dispose();
    super.dispose();
  }

  String get _typed => _ctrl.text.trim().toLowerCase().replaceAll(RegExp(r'[\s_]+'), '-').replaceAll(RegExp(r'[^a-z0-9-]'), '');
  bool get _valid => _kNameOk.hasMatch(_typed);

  Future<void> _choose() async {
    if (!_valid || _picking) return;
    _picking = true;
    final f = await openFile(acceptedTypeGroups: [_kFileTypes]);
    _picking = false;
    if (f == null || !mounted) return;
    Navigator.pop(context, _HostChoice(_typed, f));
  }

  @override
  Widget build(BuildContext context) {
    final t = _typed;
    final preview = '${t.isEmpty ? 'your-name' : t}${widget.suffix.isEmpty ? '' : '-${widget.suffix}'}.pages.dev';
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
            Text('Host a site', style: inter(21, w: FontWeight.w700)),
            const SizedBox(height: 2),
            Text('Zip or html, free live link', style: inter(13.5, c: C.muted)),
          ]),
        ),
        const SizedBox(height: 16),
        TextField(
          controller: _ctrl,
          autofocus: true,
          maxLength: 30,

          style: inter(17),
          cursorColor: C.teal,
          onChanged: (_) => setState(() {}),
          decoration: InputDecoration(
            counterText: '',
            hintText: 'myshop',
            hintStyle: inter(17, c: C.muted),
            filled: true,
            fillColor: C.field,
            contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
            border: OutlineInputBorder(borderRadius: BorderRadius.circular(15), borderSide: BorderSide.none),
          ),
        ),
        Padding(
          padding: const EdgeInsets.fromLTRB(4, 9, 4, 0),
          child: Text(preview, maxLines: 2, overflow: TextOverflow.ellipsis, style: inter(13.5, w: FontWeight.w600, c: _valid ? C.teal : C.muted)),
        ),
        const SizedBox(height: 14),
        Pressable(
          onTap: _choose,
          child: Opacity(
            opacity: _valid ? 1 : .45,
            child: Container(
              height: 52, alignment: Alignment.center,
              decoration: BoxDecoration(color: C.tealBtn, borderRadius: BorderRadius.circular(26)),
              child: Text('Choose zip or html', style: inter(17, w: FontWeight.w700, c: C.onTeal)),
            ),
          ),
        ),
        const SizedBox(height: 12),
        Text('Static sites only. No password pages.', textAlign: TextAlign.center, style: inter(12.5, c: C.muted)),
      ]),
    );
  }
}
