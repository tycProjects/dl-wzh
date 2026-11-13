import 'dart:async';
import 'package:file_selector/file_selector.dart';
import 'package:flutter/cupertino.dart' show CupertinoSliverRefreshControl;
import 'package:flutter/material.dart' hide Text;
import 'package:flutter/services.dart';
import 'package:url_launcher/url_launcher.dart';
import '../anim.dart';
import '../api.dart';
import '../banner.dart';
import '../theme.dart';
import '../widgets.dart';
import '../tr.dart';
import 'hostshare_ui.dart';
import 'hs_cards.dart';
import 'hs_design.dart';

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
  bool _slow = false; // the server is taking a while (it sleeps when idle)
  Timer? _slowT;
  final Set<String> _seenSites = {};

  @override
  void dispose() {
    HsPlus.host.removeListener(_onPlus);
    _slowT?.cancel();
    super.dispose();
  }

  void _onPlus() {
    if (widget.visible && _data != null) _upload();
  }

  @override
  void initState() {
    super.initState();
    HsPlus.host.addListener(_onPlus);
    // Paint the last known list right away; then refresh quietly in the background.
    _data = Api.i.hostedSitesCached();
    _load();
  }

  @override
  void didUpdateWidget(HostPage old) {
    super.didUpdateWidget(old);
    if (widget.visible && !old.visible && _busyName == null) _load();
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
      final d = await Api.i.hostedSites();
      _slowT?.cancel();
      if (!mounted) return;
      setState(() { _data = d; _loading = false; _slow = false; });
    } catch (e) {
      _slowT?.cancel();
      if (!mounted) return;
      if (_data != null) return; // keep showing the saved list; no error screen over it
      setState(() { _slow = false; _loadError = e is ApiError ? e.message : 'Can\u2019t reach the server. Check your connection.'; _loading = false; });
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
      await Api.i.setHostStamp(site.name);
      if (!mounted) return;
      await _load();
      if (!mounted) return;
      IosBanner.show(context, kind: BannerKind.ok, title: update == null ? 'Your site is live' : 'Site updated',
          msg: update == null ? 'Link copied, ready to share: ${site.url}' : 'Your changes are live. Link copied: ${site.url}');
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
        leave: 'Delete', destructive: true);
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
    if (mounted) IosBanner.show(context, kind: BannerKind.ok, title: 'Link copied', msg: s.url);
  }

  Future<void> _open(HostedSite s) async {
    try {
      await launchUrl(Uri.parse(s.url), mode: LaunchMode.externalApplication);
    } catch (_) {
      if (mounted) showInfoDialog(context, title: 'Couldn\u2019t open that', message: s.url);
    }
  }

  /// iOS action sheet: the site on top, its actions under it, Cancel on its own.
  Future<void> _actions(HostedSite s) async {
    if (_busyName != null) return;
    final pick = await showHsActionSheet(
      context,
      leading: HsTile(_title(s), size: 44),
      title: _title(s),
      subtitle: _host(s),
      items: const [
        HsSheetItem(Icons.content_copy_rounded, 'Copy link', 'copy'),
        HsSheetItem(Icons.open_in_new_rounded, 'Open in browser', 'open'),
        HsSheetItem(Icons.upload_rounded, 'Update files', 'update'),
        HsSheetItem(Icons.delete_outline_rounded, 'Delete site', 'delete', danger: true),
      ],
    );
    if (!mounted || pick == null) return;
    switch (pick) {
      case 'copy': await _copy(s); break;
      case 'open': await _open(s); break;
      case 'update': await _upload(update: s); break;
      case 'delete': await _delete(s); break;
    }
  }

  DateTime? _updated(HostedSite s) => s.updated ?? Api.i.hostStamp(s.name);

  /// A hosted site as a card: name and address, Live and time left, then Copy link / Open / Update.
  /// Swipe it to the left to delete.
  Widget _siteCard(HostedSite s) {
    final busy = _busyName == s.name;
    final t = _title(s);
    final up = _updated(s);
    final left = up == null ? null : hsDaysLeft(up);
    final urgent = left != null && left <= 1;
    final visits = s.visits == null ? null : s.visits!.fold<int>(0, (a, b) => a + b);
    return HsSwipeDelete(
      id: s.name,
      enabled: _busyName == null,
      onDelete: () => _delete(s),
      child: HsCard(
        leading: HsTile(t, size: 52),
        title: t,
        subtitle: _host(s),
        onTap: busy ? null : () => _actions(s),
        trailing: busy
            ? const SizedBox(width: 44, height: 44, child: Center(child: IosSpinner(size: 20)))
            : HsMoreButton(onTap: () => _actions(s)),
        busy: busy,
        busyLabel: _busyLabel,
        pills: [
          HsPill('Live', dot: true, color: C.teal, bg: kHsLiveBg),
          if (left != null)
            HsPill(
              left == 0 ? 'Removed today' : '$left ${left == 1 ? 'day' : 'days'} left',
              verbatim: true,
              icon: Icons.schedule_rounded,
              color: urgent ? kHsOrange : C.muted,
              bg: urgent ? kHsWarnBg : const Color(0xFF262629),
            ),
          if (visits != null && visits > 0)
            HsPill('$visits ${visits == 1 ? 'visit' : 'visits'} this week', verbatim: true, icon: Icons.visibility_outlined),
        ],
        notice: urgent ? (left == 0 ? 'This site is removed today. Update it to keep it online.' : 'This site is removed tomorrow. Update it to keep it online.') : null,
        quick: [
          HsQuick(Icons.content_copy_rounded, 'Copy link', () => _copy(s)),
          HsQuick(Icons.open_in_new_rounded, 'Open', () => _open(s)),
          HsQuick(Icons.upload_rounded, 'Update', () => _upload(update: s)),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final loadingFirst = _data == null && _loadError == null;
    final publishing = _busyName == 'new';
    final children = <Widget>[
      HsHeroCard(
        icon: Icons.language_rounded,
        title: 'Host a website',
        subtitle: 'Put a zip or html file online with a free link.',
        buttonLabel: 'Host a site',
        used: _data == null ? null : _sites.length,
        limit: _limit,
        busy: publishing,
        busyLabel: _busyLabel,
        onTap: () => _upload(),
      ),
    ];
    if (loadingFirst) {
      children.addAll([const HsListHeader('Your sites'), const HsCardSkeleton(), if (_slow) const HsSlowHint()]);
    } else if (_data == null) {
      children.addAll([const SizedBox(height: 22), HsError(message: _loadError!, onRetry: _load)]);
    } else if (_sites.isEmpty && !publishing) {
      children.add(const HsEmptyState(
        illus: 'empty_host',
        icon: Icons.language_rounded,
        title: 'Nothing hosted yet',
        message: 'Your hosted sites will show up here.',
      ));
    } else {
      children.add(HsListHeader('Your sites', count: _sites.length));
      if (publishing) children.add(const Padding(padding: EdgeInsets.only(bottom: 12), child: HsCardSkeleton()));
      for (var i = 0; i < _sites.length; i++) {
        final n = _sites[i].name;
        // A card that was shown before (after a refresh, or when the one above it is deleted) doesn't animate again.
        final seen = _seenSites.contains(n);
        _seenSites.add(n);
        children.add(Padding(
          padding: const EdgeInsets.only(bottom: 12),
          child: HsAppear(key: ValueKey('site-$n'), index: seen ? -1 : i, child: _siteCard(_sites[i])),
        ));
      }
      children.add(const HsFootnote('Sites you don\u2019t update for 7 days are removed. Swipe a site to the left to delete it.'));
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
    final touched = _ctrl.text.trim().isNotEmpty;
    return Container(
      width: double.infinity,
      padding: EdgeInsets.fromLTRB(16, 10, 16, 18 + MediaQuery.of(context).viewInsets.bottom + MediaQuery.of(context).padding.bottom),
      decoration: const BoxDecoration(color: C.sheet, borderRadius: BorderRadius.vertical(top: Radius.circular(30))),
      child: Column(mainAxisSize: MainAxisSize.min, crossAxisAlignment: CrossAxisAlignment.stretch, children: [
        Center(child: Container(width: 38, height: 5, decoration: BoxDecoration(color: const Color(0xFF4A4A4C), borderRadius: BorderRadius.circular(3)))),
        const SizedBox(height: 18),
        Row(children: [
          const HsIconTile(Icons.language_rounded, size: 46),
          const SizedBox(width: 13),
          Expanded(
            child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              Text('Host a site', style: inter(21, w: FontWeight.w700)),
              const SizedBox(height: 2),
              Text('Zip or html, free live link', style: inter(13.5, c: C.muted)),
            ]),
          ),
        ]),
        const SizedBox(height: 18),
        Container(
          padding: const EdgeInsets.symmetric(horizontal: 16),
          decoration: ShapeDecoration(color: C.field, shape: hsShape(16)),
          child: TextField(
            controller: _ctrl,
            autofocus: true,
            maxLength: 30,
            style: inter(17),
            cursorColor: kHsBlue,
            onChanged: (_) => setState(() {}),
            decoration: InputDecoration(
              counterText: '',
              hintText: 'myshop',
              hintStyle: inter(17, c: C.muted),
              border: InputBorder.none,
              contentPadding: const EdgeInsets.symmetric(vertical: 15),
            ),
          ),
        ),
        const SizedBox(height: 10),
        Container(
          padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
          decoration: ShapeDecoration(color: const Color(0xFF1E1E20), shape: hsShape(14)),
          child: Row(children: [
            Icon(Icons.link_rounded, size: 19, color: _valid ? kHsBlue : C.muted),
            const SizedBox(width: 10),
            Expanded(child: Text(preview, verbatim: true, maxLines: 2, overflow: TextOverflow.ellipsis, style: inter(13.5, w: FontWeight.w600, c: _valid ? kHsBlue : C.muted))),
            if (_valid) ...[
              const SizedBox(width: 8),
              Icon(Icons.check_circle_rounded, size: 19, color: kHsBlue),
            ],
          ]),
        ),
        if (touched && !_valid)
          Padding(
            padding: const EdgeInsets.fromLTRB(6, 8, 6, 0),
            child: Text('Use 3 to 30 letters, numbers or dashes.', style: inter(12.5, c: kHsOrange)),
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
                Text('Choose zip or html', style: inter(17, w: FontWeight.w700, c: C.onTeal)),
              ]),
            ),
          ),
        ),
        const SizedBox(height: 12),
        Text('Static sites only. No password pages.', textAlign: TextAlign.center, style: inter(12.5, c: C.muted)),
      ]),
    );
  }
}
