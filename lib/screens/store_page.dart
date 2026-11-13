import 'dart:async';
import 'dart:io';
import 'dart:ui' as ui;
import 'package:file_selector/file_selector.dart';
import 'package:flutter/cupertino.dart' show CupertinoPageRoute, CupertinoSliverRefreshControl;
import 'package:flutter/foundation.dart' show ValueListenable;
import 'package:flutter/material.dart' hide Text;
import '../anim.dart';
import '../api.dart';
import '../appearance.dart' show Unfilter;
import '../installed.dart';
import '../save_install.dart';
import '../sign_in_card.dart';
import '../store_auth.dart';
import '../store_cloud.dart';
import '../theme.dart';
import '../tr.dart';
import '../widgets.dart';
import '../pick_apk.dart';
import 'gallery_picker.dart';

const _apkType = XTypeGroup(label: 'Android app (apk)', extensions: ['apk'], mimeTypes: ['application/vnd.android.package-archive', 'application/octet-stream']);
const _imgType = XTypeGroup(label: 'Picture', extensions: ['png', 'jpg', 'jpeg', 'webp'], mimeTypes: ['image/png', 'image/jpeg', 'image/webp']);

// Store action buttons (Install / Update / Open) use the Google Play style colours.
const _btnBlue = Color(0xFFA8C7FA);
const _btnBlueText = Color(0xFF062E6F);
const _btnBlueDeep = Color(0xFF1B2A44);

const _amber = Color(0xFFE8B84B);
const _skel = Color(0xFF232326);
const _hair = Color(0x1FFFFFFF);

String _mb(int b) => b >= 1048576 ? '${(b / 1048576).toStringAsFixed(1)} MB' : '${(b / 1024).ceil()} KB';
String _err(Object e) => e is ApiError ? e.message : 'Something went wrong. Try again. (${e.toString().split('\n').first})';

/// 1200 -> 1.2K, 3400000 -> 3.4M
String _cnt(int n) {
  String f(double v) {
    final s = v.toStringAsFixed(v >= 10 ? 0 : 1);
    return s.endsWith('.0') ? s.substring(0, s.length - 2) : s;
  }

  if (n >= 1000000) return '${f(n / 1000000)}M';
  if (n >= 1000) return '${f(n / 1000)}K';
  return '$n';
}

const _months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
String _date(DateTime? d) {
  if (d == null) return '—';
  final l = d.toLocal();
  return '${_months[l.month - 1]} ${l.day}, ${l.year}';
}

IconData _catIcon(String c) {
  switch (c) {
    case 'Tools':
      return Icons.build_rounded;
    case 'Games':
      return Icons.sports_esports_rounded;
    case 'Social':
      return Icons.forum_rounded;
    case 'Media':
      return Icons.play_circle_rounded;
    case 'Education':
      return Icons.school_rounded;
    default:
      return Icons.apps_rounded;
  }
}

String _statusText(String s) => s == 'approved' ? 'Live' : (s == 'rejected' ? 'Rejected' : 'In review');
Color _statusColor(String s) => s == 'approved' ? C.teal : (s == 'rejected' ? C.red : _amber);

final _epoch = DateTime.fromMillisecondsSinceEpoch(0);

const _starColor = Color(0xFFF2B84B);
String _rate(StoreApp a) => '${a.rating.toStringAsFixed(1)}\u2605';

class _Dl {
  final String id;
  final double? p;
  const _Dl(this.id, this.p);
}

/// Store tab, laid out like Google Play: search pill, For you / Top charts / My apps tabs,
/// category chips, carousels, and a full details page for every app.
class StorePage extends StatefulWidget {
  final bool embedded;
  final bool visible;
  /// Height of the top bar that floats over this page; content starts below it and scrolls under it.
  final double topInset;
  const StorePage({super.key, this.embedded = false, this.visible = true, this.topInset = 0});
  @override
  State<StorePage> createState() => _StorePageState();
}

class _StorePageState extends State<StorePage> with WidgetsBindingObserver {
  final _q = TextEditingController();
  Timer? _deb;
  List<StoreApp> _all = [], _mine = [];
  bool _loading = true;
  int _tab = 0; // 0 For you, 1 Top charts, 2 My apps (publish form + your apps)
  int _formKey = 0;
  String? _cat; // category chip, null = all
  String? _error;
  final ValueNotifier<_Dl?> _dl = ValueNotifier(null);
  final ValueNotifier<int> _rev = ValueNotifier(0); // bumped so the Publish page rebuilds with the store

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _load();
  }

  /// Back from the installer / uninstall screen / another app: Install, Open and Update may have changed.
  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) _refreshInstalled();
  }

  void _refreshInstalled() => InstalledApps.refresh([..._all, ..._mine].map((a) => a.pkg));

  @override
  void didUpdateWidget(StorePage old) {
    super.didUpdateWidget(old);
    if (widget.visible && !old.visible) _load();
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    _deb?.cancel();
    _q.dispose();
    _dl.dispose();
    _rev.dispose();
    super.dispose();
  }

  bool get _searching => _q.text.trim().isNotEmpty;

  Future<void> _load() async {
    if (mounted) setState(() => _loading = _all.isEmpty && _mine.isEmpty);
    try {
      final r = await Future.wait([StoreCloud.list(), StoreCloud.mine().catchError((_) => <StoreApp>[])]);
      if (!mounted) return;
      setState(() {
        _all = r[0];
        _mine = r[1];
        _error = null;
        _loading = false;
      });
      _refreshInstalled();
      _rev.value++;
    } catch (e) {
      if (mounted) {
        setState(() {
          _error = _err(e);
          _loading = false;
        });
      }
    }
  }

  Future<void> _install(StoreApp a) async {
    if (_dl.value != null) return;
    _dl.value = _Dl(a.id, 0);
    try {
      // Already downloaded? Open the installer straight away, no second download.
      var path = await StoreCloud.cached(a);
      if (path == null) {
        path = await StoreCloud.download(a, (s, t) {
          if (mounted) _dl.value = _Dl(a.id, t > 0 ? (s / t).clamp(0, 1).toDouble() : null);
        });
        StoreCloud.hit(a.id);
      }
      final ok = await FileSaver.install(path);
      if (!ok && mounted) showInfoDialog(context, title: 'Couldn’t open the installer', message: 'Allow “install unknown apps” for this app in Android settings, then try again.');
    } catch (e) {
      if (mounted) showInfoDialog(context, title: 'Download failed', message: _err(e));
    } finally {
      if (mounted) _dl.value = null;
    }
  }

  Future<void> _publish() async {
    if (!StoreAuth.signedIn) {
      final go = await showSignInCard(context);
      if (!go || !mounted) return;
      try {
        if (!await StoreAuth.signIn() || !mounted) return;
        await StoreCloud.claimOld();
        await _load();
      } catch (e) {
        if (mounted && await showSignInError(context, _err(e)) && mounted) _publish();
        return;
      }
      if (!mounted) return;
    }
    if (_mine.length >= kStoreMaxApps) {
      showInfoDialog(context, title: 'App limit reached', message: 'You can have $kStoreMaxApps apps in the Store. Delete one under My apps to publish another.', icon: Icons.lock_outline_rounded, color: C.red);
      return;
    }
    final done = await showModalBottomSheet<bool>(
      context: context,
      isScrollControlled: true,
      backgroundColor: C.sheet,
      shape: const RoundedRectangleBorder(borderRadius: BorderRadius.vertical(top: Radius.circular(24))),
      builder: (_) => const _PublishSheet(),
    );
    if (done == true && mounted) {
      _q.clear();
      await _load();
      if (!mounted) return;
      setState(() => _tab = 2);
      showInfoDialog(context, title: 'Sent for review', message: 'Your app shows in the Store as soon as it is approved. You can follow it under My apps.');
    }
  }

  /// Publish opens its own page: the upload form on top, your published apps below.
  void _openPublishPage() {
    FocusScope.of(context).unfocus();
    Navigator.of(context).push(CupertinoPageRoute(
      builder: (_) => Scaffold(
        backgroundColor: C.bg,
        body: SafeArea(
          child: Column(children: [
            Padding(
              padding: const EdgeInsets.fromLTRB(4, 2, 4, 0),
              child: Row(children: [
                IconButton(icon: const Icon(Icons.arrow_back_rounded, color: C.text), onPressed: () => Navigator.of(context).pop()),
                const SizedBox(width: 4),
                Text('Publish', style: inter(18, w: FontWeight.w700)),
              ]),
            ),
            Expanded(
              child: AnimatedBuilder(
                animation: Listenable.merge([_rev, StoreAuth.changes]),
                builder: (_, __) => _myApps(),
              ),
            ),
          ]),
        ),
      ),
    ));
  }

  Future<bool> _delete(StoreApp a) async {
    final ok = await confirmLeave(context,
        title: 'Delete this app?', message: '“${a.name}” will be removed from the Store for everyone.', stay: 'Cancel', leave: 'Delete', destructive: true);
    if (!ok || !mounted) return false;
    try {
      await StoreCloud.delete(a.id);
      await _load();
      return true;
    } catch (e) {
      if (mounted) showInfoDialog(context, title: 'Couldn’t delete', message: _err(e));
      return false;
    }
  }

  void _open(StoreApp a) {
    FocusScope.of(context).unfocus();
    final mine = _mine.any((m) => m.id == a.id);
    final similar = _all.where((x) => x.id != a.id && x.cat == a.cat).toList()..sort((x, y) => y.downloads.compareTo(x.downloads));
    Navigator.of(context).push(CupertinoPageRoute(
      builder: (_) => _AppPage(app: a, mine: mine, similar: similar, dl: _dl, onInstall: _install, onDelete: _delete, onOpen: _open, onReviewed: _load),
    ));
  }

  List<StoreApp> get _byDl => [..._all]..sort((a, b) => b.downloads.compareTo(a.downloads));
  List<StoreApp> get _byNew => [..._all]..sort((a, b) => (b.updated ?? b.created ?? _epoch).compareTo(a.updated ?? a.created ?? _epoch));
  List<StoreApp> _filter(List<StoreApp> l) => _cat == null ? l : l.where((a) => a.cat == _cat).toList();

  // ---------------------------------------------------------------- pieces

  // Top bar + search row (+ chip row while browsing): the floating header's height.
  double get _headTop => widget.topInset + (_searching ? 50 : 96);

  Widget _searchBar() => Padding(
        padding: const EdgeInsets.fromLTRB(16, 8, 16, 0),
        child: Row(children: [
          Expanded(
            child: Container(
          height: 42,
          decoration: BoxDecoration(color: C.card, borderRadius: BorderRadius.circular(21)),
          child: Row(children: [
            const SizedBox(width: 16),
            const Icon(Icons.search_rounded, color: C.text, size: 20),
            const SizedBox(width: 10),
            Expanded(
              child: TextField(
                controller: _q,
                style: inter(15),
                cursorColor: C.teal,
                textInputAction: TextInputAction.search,
                autocorrect: true,
                enableSuggestions: true,
                onChanged: (_) => setState(() {}),
                onSubmitted: (_) {},
                decoration: InputDecoration(
                  hintText: 'Search apps',
                  hintStyle: inter(15, c: C.muted),
                  border: InputBorder.none,
                  isDense: true,
                  contentPadding: const EdgeInsets.symmetric(vertical: 10),
                ),
              ),
            ),
            if (_q.text.isNotEmpty)
              Pressable(
                onTap: () => setState(() => _q.clear()),
                child: const Padding(padding: EdgeInsets.all(12), child: Icon(Icons.close_rounded, color: C.muted, size: 20)),
              )
            else
              const SizedBox(width: 16),
          ]),
        ),
          ),
          const SizedBox(width: 10),
          // Publish is its own button, apart from the search field: it opens My apps, where the form is.
          Pressable(
            scale: .95,
            onTap: _openPublishPage,
            child: Container(
              height: 42,
              padding: const EdgeInsets.symmetric(horizontal: 13),
              decoration: BoxDecoration(color: const Color(0xFF3A3C40), borderRadius: BorderRadius.circular(21)),
              child: Row(mainAxisSize: MainAxisSize.min, children: [
                const Icon(Icons.add_rounded, size: 18, color: C.text),
                const SizedBox(width: 4),
                Text('Publish', style: inter(13, w: FontWeight.w600, c: C.text)),
              ]),
            ),
          ),
        ]),
      );

  /// One row of pill chips like Google Play's: the three sections first, then the categories.
  Widget _chipBar() {
    Widget chip(String label, IconData icon, bool on, VoidCallback f) => Padding(
          padding: const EdgeInsets.only(right: 8),
          child: Pressable(
            scale: .96,
            haptic: false,
            onTap: f,
            child: Container(
              height: 34,
              padding: const EdgeInsets.symmetric(horizontal: 12),
              decoration: BoxDecoration(color: on ? const Color(0xFF3A3C40) : const Color(0xFF26282B), borderRadius: BorderRadius.circular(17)),
              child: Row(mainAxisSize: MainAxisSize.min, children: [
                Icon(icon, size: 16, color: C.text),
                const SizedBox(width: 6),
                Text(label, verbatim: true, style: inter(13, w: FontWeight.w500, c: C.text)),
              ]),
            ),
          ),
        );
    return SizedBox(
      height: 46,
      child: ListView(
        scrollDirection: Axis.horizontal,
        padding: const EdgeInsets.fromLTRB(16, 8, 16, 4),
        children: [
          chip('For you', Icons.auto_awesome_rounded, _tab == 0 && _cat == null, () => setState(() {
                _tab = 0;
                _cat = null;
              })),
          chip('Top charts', Icons.bar_chart_rounded, _tab == 1, () => setState(() => _tab = 1)),
          for (final k in kStoreCats)
            chip(k, _catIcon(k), _cat == k && _tab != 2, () => setState(() {
                  _cat = _cat == k ? null : k;
                  if (_tab == 2) _tab = 0;
                })),
        ],
      ),
    );
  }

  Widget _scroll(List<Widget> children) => CustomScrollView(
        keyboardDismissBehavior: ScrollViewKeyboardDismissBehavior.onDrag,
        physics: const AlwaysScrollableScrollPhysics(parent: IosScrollPhysics()),
        slivers: [
          SliverToBoxAdapter(child: SizedBox(height: _headTop)),
          CupertinoSliverRefreshControl(onRefresh: _load),
          SliverPadding(
            padding: const EdgeInsets.only(bottom: 28),
            sliver: SliverList(delegate: SliverChildListDelegate(children)),
          ),
        ],
      );

  Widget _empty(IconData icon, String title, String msg, {String? action, VoidCallback? onAction}) => _scroll([
        const SizedBox(height: 56),
        Center(
          child: Container(
            width: 84,
            height: 84,
            decoration: const BoxDecoration(color: Color(0xFF2C2C2F), shape: BoxShape.circle),
            child: Icon(icon, size: 38, color: Colors.white),
          ),
        ),
        const SizedBox(height: 18),
        Center(child: Text(title, verbatim: true, style: inter(19, w: FontWeight.w600))),
        const SizedBox(height: 6),
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 40),
          child: Text(msg, verbatim: true, textAlign: TextAlign.center, style: inter(14.5, c: C.muted, h: 1.4)),
        ),
        if (action != null) ...[
          const SizedBox(height: 20),
          Center(
            child: Pressable(
              onTap: onAction,
              child: Container(
                padding: const EdgeInsets.symmetric(horizontal: 22, vertical: 12),
                decoration: BoxDecoration(color: C.tealBtn, borderRadius: BorderRadius.circular(24)),
                child: Text(action, verbatim: true, style: inter(15, w: FontWeight.w600, c: C.onTeal)),
              ),
            ),
          ),
        ],
      ]);

  Widget _skeleton() => _Pulse(
        child: ListView(
          physics: const NeverScrollableScrollPhysics(),
          padding: EdgeInsets.fromLTRB(16, 12 + _headTop, 16, 0),
          children: [
            _box(double.infinity, 176, 24),
            const SizedBox(height: 26),
            _box(150, 18, 6),
            const SizedBox(height: 14),
            Row(children: [for (var i = 0; i < 3; i++) ...[Expanded(child: AspectRatio(aspectRatio: 1, child: _box(double.infinity, double.infinity, 22))), if (i < 2) const SizedBox(width: 14)]]),
            const SizedBox(height: 28),
            for (var i = 0; i < 4; i++)
              Padding(
                padding: const EdgeInsets.only(bottom: 18),
                child: Row(children: [
                  _box(56, 56, 14),
                  const SizedBox(width: 14),
                  Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [_box(160, 14, 5), const SizedBox(height: 9), _box(100, 11, 5)])),
                ]),
              ),
          ],
        ),
      );

  Widget _box(double w, double h, double r) => Container(width: w, height: h, decoration: BoxDecoration(color: _skel, borderRadius: BorderRadius.circular(r)));

  Widget _errorView() => _empty(Icons.cloud_off_rounded, 'Can’t load the Store', _error ?? 'Check your connection and try again.', action: 'Try again', onAction: () {
        setState(() => _loading = true);
        _load();
      });

  Widget _row(StoreApp a, {int? rank, bool mineView = false, bool button = false}) => _Row(app: a, rank: rank, mineView: mineView, button: button, dl: _dl, onOpen: _open, onInstall: _install);

  // ---------------------------------------------------------------- tabs

  Widget _forYou() {
    if (_all.isEmpty) {
      return _empty(Icons.storefront_rounded, 'No apps yet', 'Be the first to publish one and it will show up here.', action: 'Publish an app', onAction: _publish);
    }
    if (_cat != null) {
      final l = _filter(_byDl);
      if (l.isEmpty) return _empty(_catIcon(_cat!), 'Nothing in $_cat yet', 'Try another category, or publish the first one.');
      return _scroll([_Head('$_cat apps'), for (final a in l) _row(a)]);
    }
    final top = _byDl;
    final fresh = _byNew.take(8).toList();
    final cats = <Widget>[];
    for (final k in kStoreCats) {
      final l = top.where((a) => a.cat == k).toList();
      if (l.length >= 2 && cats.length < 6) {
        cats.add(_Carousel(title: k, apps: l, onOpen: _open, onMore: () => setState(() => _cat = k)));
      }
    }
    final featured = top.take(5).toList();
    final suggested = top.length > 5 ? top.skip(5).take(6).toList() : top.take(6).toList();
    return _scroll([
      _Featured(apps: featured, dl: _dl, onOpen: _open, onInstall: _install),
      _Head('Suggested for you', onMore: () => setState(() => _tab = 1)),
      for (final a in suggested) _row(a),
      if (top.length > 1) _Carousel(title: 'Popular apps', apps: top.skip(1).take(10).toList(), onOpen: _open, onMore: () => setState(() => _tab = 1)),
      _Head('New & updated'),
      for (final a in fresh) _row(a),
      ...cats,
    ]);
  }

  Widget _charts() {
    final l = _filter(_byDl);
    if (l.isEmpty) return _empty(Icons.leaderboard_rounded, 'No apps to rank', _cat == null ? 'Apps are ranked by downloads once they are published.' : 'Nothing in $_cat yet.');
    return _scroll([
      _Head(_cat == null ? 'Top downloaded' : 'Top in $_cat'),
      for (var i = 0; i < l.length; i++) _row(l[i], rank: i + 1),
    ]);
  }

  Future<void> _signOut() async {
    final ok = await confirmLeave(context,
        title: 'Sign out?', message: 'You can sign in again any time. Your published apps stay on your account.', stay: 'Cancel', leave: 'Sign out', destructive: true);
    if (!ok) return;
    await StoreAuth.signOut();
    if (mounted) _load();
  }

  Future<void> _signIn() async {
    final go = await showSignInCard(context);
    if (!go || !mounted) return;
    try {
      if (!await StoreAuth.signIn() || !mounted) return;
      await StoreCloud.claimOld();
      await _load();
    } catch (e) {
      if (mounted && await showSignInError(context, _err(e)) && mounted) _signIn();
    }
  }

  Widget _accountRow() => ValueListenableBuilder<int>(
        valueListenable: StoreAuth.changes,
        builder: (context, _, __) {
          final on = StoreAuth.signedIn;
          final email = StoreAuth.email;
          return Padding(
            padding: const EdgeInsets.fromLTRB(16, 14, 16, 0),
            child: Container(
              padding: const EdgeInsets.fromLTRB(14, 12, 12, 12),
              decoration: BoxDecoration(color: C.card, borderRadius: BorderRadius.circular(20), border: Border.all(color: _hair, width: .7)),
              child: Row(children: [
                Container(
                  width: 40,
                  height: 40,
                  alignment: Alignment.center,
                  decoration: BoxDecoration(color: on ? C.tealDeep : const Color(0xFF2A2A2C), shape: BoxShape.circle),
                  child: on
                      ? Text(email.isNotEmpty ? email[0].toUpperCase() : '?', verbatim: true, style: inter(18, w: FontWeight.w700, c: C.teal))
                      : const Icon(Icons.person_outline_rounded, size: 22, color: C.muted),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                    Text(on ? 'Signed in' : 'Not signed in', verbatim: true, style: inter(14.5, w: FontWeight.w600, c: on ? C.teal : C.muted)),
                    const SizedBox(height: 2),
                    Text(on ? (email.isEmpty ? 'Google account' : email) : 'Sign in to publish apps', verbatim: true, maxLines: 1, overflow: TextOverflow.ellipsis, style: inter(13, c: C.muted)),
                  ]),
                ),
                const SizedBox(width: 10),
                Pressable(
                  onTap: on ? _signOut : _signIn,
                  scale: .95,
                  child: Container(
                    height: 36,
                    padding: const EdgeInsets.symmetric(horizontal: 16),
                    alignment: Alignment.center,
                    decoration: BoxDecoration(
                      color: on ? C.field : C.tealDeep,
                      borderRadius: BorderRadius.circular(18),
                      border: on ? Border.all(color: const Color(0xFF4A4A4E), width: 1) : null,
                    ),
                    child: Text(on ? 'Sign out' : 'Sign in', verbatim: true, style: inter(14, w: FontWeight.w600, c: on ? C.text : C.teal)),
                  ),
                ),
              ]),
            ),
          );
        },
      );

  Widget _myApps() {
    return _scroll([
      _accountRow(),
      if (!StoreAuth.signedIn)
        Padding(
          padding: const EdgeInsets.fromLTRB(40, 28, 40, 0),
          child: Text('Sign in to publish an app and follow its review here.', verbatim: true, textAlign: TextAlign.center, style: inter(14.5, c: C.muted, h: 1.4)),
        )
      else if (_mine.length >= kStoreMaxApps)
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 12, 16, 0),
          child: Container(
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(color: C.card, borderRadius: BorderRadius.circular(20), border: Border.all(color: _hair, width: .7)),
            child: Text('You have $kStoreMaxApps apps in the Store, the most allowed. Delete one below to publish another.', verbatim: true, style: inter(14, c: C.muted, h: 1.4)),
          ),
        )
      else
        _PublishSheet(
          key: ValueKey('publish$_formKey'),
          embedded: true,
          onDone: () async {
            _q.clear();
            await _load();
            if (!mounted) return;
            setState(() => _formKey++);
            _rev.value++;
            showInfoDialog(context, title: 'Sent for review', message: 'Your app shows in the Store as soon as it is approved. You can follow it below.');
          },
        ),
      if (_mine.isEmpty)
        Padding(
          padding: const EdgeInsets.fromLTRB(40, 40, 40, 0),
          child: Text('Apps you publish show up here with their review status.', verbatim: true, textAlign: TextAlign.center, style: inter(14.5, c: C.muted, h: 1.4)),
        )
      else ...[
        _Head('Your apps'),
        for (final a in _mine) _MineCard(app: a, onOpen: _open),
      ],
    ]);
  }

  Widget _searchResults() {
    final hits = _fuzzySearch(_all, _q.text);
    if (hits.apps.isEmpty) {
      return _empty(Icons.search_off_rounded, 'No results', 'Nothing matches “${_q.text.trim()}”. Check the spelling or try a shorter word.');
    }
    return _scroll([
      Padding(
        padding: const EdgeInsets.fromLTRB(16, 10, 16, 4),
        child: Text(
          hits.close ? 'No exact match. Showing close matches for “${_q.text.trim()}”' : '${hits.apps.length} ${hits.apps.length == 1 ? 'result' : 'results'}',
          verbatim: true,
          style: inter(13, c: C.muted),
        ),
      ),
      for (final a in hits.apps) _row(a, button: true),
    ]);
  }

  Widget _body() {
    if (_loading) return _skeleton();
    if (_error != null && _all.isEmpty && _mine.isEmpty) return _errorView();
    if (_searching) return _searchResults();
    switch (_tab) {
      case 1:
        return _charts();
      case 2:
        return _myApps();
      default:
        return _forYou();
    }
  }

  @override
  Widget build(BuildContext context) {
    final browse = !_searching;
    // The content fills the whole page and scrolls under the header; the header is a solid layer on top.
    return Stack(children: [
      Positioned.fill(child: AnimatedSwitcher(duration: const Duration(milliseconds: 180), child: KeyedSubtree(key: ValueKey('$_tab-$_searching-$_loading-$_cat'), child: _body()))),
      Positioned(
        top: 0, left: 0, right: 0,
        child: Stack(clipBehavior: Clip.none, children: [
          // Solid header: nothing shows through the title bar, search bar or chips. A short fade under it
          // keeps the list from ending in a hard edge.
          Positioned.fill(child: IgnorePointer(child: ColoredBox(color: C.bg))),
          Positioned(
            left: 0, right: 0, bottom: -16, height: 16,
            child: IgnorePointer(
              child: DecoratedBox(
                decoration: BoxDecoration(
                  gradient: LinearGradient(begin: Alignment.topCenter, end: Alignment.bottomCenter, colors: [C.bg, C.bg.withOpacity(0)]),
                ),
              ),
            ),
          ),
          Padding(
            padding: EdgeInsets.only(top: widget.topInset),
            child: Column(mainAxisSize: MainAxisSize.min, children: [
              _searchBar(),
              if (browse) _chipBar(),
            ]),
          ),
        ]),
      ),
    ]);
  }
}

// ==================================================================== shared widgets

class _Logo extends StatelessWidget {
  final StoreApp a;
  final double size;
  const _Logo(this.a, this.size);
  @override
  Widget build(BuildContext context) {
    final letter = Container(
      width: size,
      height: size,
      alignment: Alignment.center,
      color: Color(0xFF2C2C2F),
      child: Text(a.name.isEmpty ? '?' : a.name.substring(0, 1).toUpperCase(), verbatim: true, style: inter(size * .42, w: FontWeight.w700, c: Colors.white)),
    );
    return ClipRRect(
      borderRadius: BorderRadius.circular(size * .23),
      child: a.logoUrl.isEmpty
          ? letter
          : Image.network(a.logoUrl, width: size, height: size, fit: BoxFit.cover, cacheWidth: (size * 3).round(), gaplessPlayback: true, errorBuilder: (_, __, ___) => letter),
    );
  }
}

// ---------------------------------------------------------------- forgiving search

class _Hits {
  final List<StoreApp> apps;
  final bool close; // true when nothing matched exactly and these are spelling-tolerant guesses
  const _Hits(this.apps, this.close);
}

String _fold(String s) {
  const from = 'àáâãäåçèéêëìíîïñòóôõöùúûüýÿ';
  const to = 'aaaaaaceeeeiiiinooooouuuuyy';
  final b = StringBuffer();
  for (final r in s.toLowerCase().runes) {
    final ch = String.fromCharCode(r);
    final i = from.indexOf(ch);
    b.write(i >= 0 ? to[i] : ch);
  }
  return b.toString();
}

final _splitter = RegExp(r'[^\p{L}\p{N}]+', unicode: true);
List<String> _words(String s) => _fold(s).split(_splitter).where((w) => w.isNotEmpty).toList();

/// Edit distance that counts a swapped pair ("tcye" -> "tyce") as one mistake. Stops early past [max].
int _editDist(String a, String b, int max) {
  if ((a.length - b.length).abs() > max) return max + 1;
  var prev2 = List<int>.filled(b.length + 1, 0);
  var prev = List<int>.generate(b.length + 1, (j) => j);
  for (var i = 1; i <= a.length; i++) {
    final cur = List<int>.filled(b.length + 1, 0);
    cur[0] = i;
    for (var j = 1; j <= b.length; j++) {
      final cost = a[i - 1] == b[j - 1] ? 0 : 1;
      var v = prev[j] + 1;
      if (cur[j - 1] + 1 < v) v = cur[j - 1] + 1;
      if (prev[j - 1] + cost < v) v = prev[j - 1] + cost;
      if (i > 1 && j > 1 && a[i - 1] == b[j - 2] && a[i - 2] == b[j - 1] && prev2[j - 2] + 1 < v) v = prev2[j - 2] + 1;
      cur[j] = v;
    }
    prev2 = prev;
    prev = cur;
  }
  return prev[b.length];
}

/// 0 = no match. 70+ = real (exact) match. Below 70 = spelling-tolerant match.
int _wordScore(String t, String w, bool fuzzy) {
  if (w == t) return 100;
  if (w.startsWith(t)) return 90;
  if (w.contains(t)) return 70;
  if (!fuzzy) return 0;
  final max = t.length <= 3 ? 0 : (t.length <= 5 ? 1 : 2);
  if (max == 0) return 0;
  var d = _editDist(t, w, max);
  // the person may still be typing, so also compare against the start of the word
  for (final extra in const [0, 1]) {
    final n = t.length + extra;
    if (w.length > n) {
      final p = _editDist(t, w.substring(0, n), max);
      if (p < d) d = p;
    }
  }
  return d <= max ? 60 - d * 12 : 0;
}

_Hits _fuzzySearch(List<StoreApp> all, String query) {
  final tokens = _words(query);
  if (tokens.isEmpty) return const _Hits([], false);
  final scored = <MapEntry<StoreApp, int>>[];
  var anyExact = false;
  for (final a in all) {
    final nameWords = _words(a.name);
    final fields = <(List<String>, double, bool)>[
      ([...nameWords, nameWords.join()], 1.0, true), // joined name so "tycerecorder" still finds "Tyce Recorder"
      (_words(a.pkg), .7, true),
      (_words(a.cat), .6, true),
      (_words(a.about), .35, false),
    ];
    var total = 0.0;
    var exact = true;
    var ok = true;
    for (final t in tokens) {
      var best = 0.0, bestRaw = 0;
      for (final f in fields) {
        for (final w in f.$1) {
          final raw = _wordScore(t, w, f.$3);
          if (raw * f.$2 > best) {
            best = raw * f.$2;
            bestRaw = raw;
          }
        }
      }
      if (best <= 0) {
        ok = false;
        break;
      }
      if (bestRaw < 70) exact = false;
      total += best;
    }
    if (!ok) continue;
    if (exact) anyExact = true;
    scored.add(MapEntry(a, exact ? total.round() + 1000 : total.round()));
  }
  scored.sort((x, y) {
    final c = y.value.compareTo(x.value);
    return c != 0 ? c : y.key.downloads.compareTo(x.key.downloads);
  });
  return _Hits([for (final e in scored) e.key], !anyExact && scored.isNotEmpty);
}


class _Head extends StatelessWidget {
  final String title;
  final VoidCallback? onMore;
  const _Head(this.title, {this.onMore});
  @override
  Widget build(BuildContext context) => Pressable(
        scale: 1,
        haptic: false,
        onTap: onMore,
        child: Padding(
          padding: const EdgeInsets.fromLTRB(16, 22, 12, 8),
          child: Row(children: [
            Expanded(child: Text(title, verbatim: true, maxLines: 1, overflow: TextOverflow.ellipsis, style: inter(20, w: FontWeight.w500))),
            if (onMore != null)
              Container(
                width: 36,
                height: 36,
                decoration: const BoxDecoration(color: Color(0xFF2B2D31), shape: BoxShape.circle),
                child: const Icon(Icons.arrow_forward_rounded, color: C.text, size: 20),
              ),
          ]),
        ),
      );
}

class _Tile extends StatelessWidget {
  final StoreApp app;
  final VoidCallback onTap;
  const _Tile(this.app, this.onTap);
  @override
  Widget build(BuildContext context) => Pressable(
        scale: .96,
        haptic: false,
        onTap: onTap,
        child: SizedBox(
          width: 104,
          child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            _Logo(app, 104),
            const SizedBox(height: 9),
            Text(app.name, verbatim: true, maxLines: 1, overflow: TextOverflow.ellipsis, style: inter(14, w: FontWeight.w500)),
            const SizedBox(height: 2),
            Text('${app.ratings > 0 ? _rate(app) : app.cat} · ${_mb(app.size)}', verbatim: true, maxLines: 1, overflow: TextOverflow.ellipsis, style: inter(12, c: C.muted)),
          ]),
        ),
      );
}

class _Carousel extends StatelessWidget {
  final String title;
  final List<StoreApp> apps;
  final void Function(StoreApp) onOpen;
  final VoidCallback? onMore;
  const _Carousel({required this.title, required this.apps, required this.onOpen, this.onMore});
  @override
  Widget build(BuildContext context) => Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        _Head(title, onMore: onMore),
        SizedBox(
          height: 172,
          child: ListView.separated(
            scrollDirection: Axis.horizontal,
            padding: const EdgeInsets.symmetric(horizontal: 16),
            itemCount: apps.length,
            separatorBuilder: (_, __) => const SizedBox(width: 14),
            itemBuilder: (_, i) => _Tile(apps[i], () => onOpen(apps[i])),
          ),
        ),
      ]);
}

/// The big swipeable banners at the top of "For you", like Google Play's featured cards.
class _Featured extends StatelessWidget {
  final List<StoreApp> apps;
  final ValueListenable<_Dl?> dl;
  final void Function(StoreApp) onOpen, onInstall;
  const _Featured({required this.apps, required this.dl, required this.onOpen, required this.onInstall});
  @override
  Widget build(BuildContext context) => LayoutBuilder(builder: (context, c) {
        final w = apps.length == 1 ? c.maxWidth - 32 : c.maxWidth - 56;
        return SizedBox(
          height: 262,
          child: ListView.separated(
            scrollDirection: Axis.horizontal,
            physics: const IosScrollPhysics(),
            padding: const EdgeInsets.fromLTRB(16, 6, 16, 0),
            itemCount: apps.length,
            separatorBuilder: (_, __) => const SizedBox(width: 12),
            itemBuilder: (_, i) => _Banner(app: apps[i], width: w, dl: dl, onOpen: onOpen, onInstall: onInstall),
          ),
        );
      });
}

class _Banner extends StatelessWidget {
  final StoreApp app;
  final double width;
  final ValueListenable<_Dl?> dl;
  final void Function(StoreApp) onOpen, onInstall;
  const _Banner({required this.app, required this.width, required this.dl, required this.onOpen, required this.onInstall});
  @override
  Widget build(BuildContext context) {
    final a = app;
    final tagline = a.about.trim().isEmpty ? a.name : a.about.trim().split('\n').first;
    return SizedBox(
      width: width,
      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Pressable(
          scale: .985,
          haptic: false,
          onTap: () => onOpen(a),
          child: ClipRRect(
            borderRadius: BorderRadius.circular(22),
            child: SizedBox(
              height: 186,
              width: width,
              child: Stack(fit: StackFit.expand, children: [
                Container(decoration: const BoxDecoration(gradient: LinearGradient(begin: Alignment.topLeft, end: Alignment.bottomRight, colors: [Color(0xFF2B3A34), Color(0xFF1B2420)]))),
                if (a.logoUrl.isNotEmpty)
                  ImageFiltered(
                    imageFilter: ui.ImageFilter.blur(sigmaX: 26, sigmaY: 26),
                    child: Transform.scale(
                      scale: 1.7,
                      child: Image.network(a.logoUrl, fit: BoxFit.cover, cacheWidth: 160, gaplessPlayback: true, errorBuilder: (_, __, ___) => const SizedBox.shrink()),
                    ),
                  ),
                Container(decoration: const BoxDecoration(gradient: LinearGradient(begin: Alignment.topCenter, end: Alignment.bottomCenter, colors: [Color(0x14000000), Color(0xB3000000)]))),
                Positioned(top: 14, right: 14, child: _Logo(a, 58)),
                Positioned(
                  left: 16,
                  right: 16,
                  bottom: 14,
                  child: Text(tagline, verbatim: true, maxLines: 2, overflow: TextOverflow.ellipsis, style: inter(24, w: FontWeight.w700, c: Colors.white, h: 1.15)),
                ),
              ]),
            ),
          ),
        ),
        const SizedBox(height: 12),
        Pressable(
          scale: .99,
          haptic: false,
          onTap: () => onOpen(a),
          child: Row(children: [
            _Logo(a, 46),
            const SizedBox(width: 12),
            Expanded(
              child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                Text(a.name, verbatim: true, maxLines: 1, overflow: TextOverflow.ellipsis, style: inter(16, w: FontWeight.w500)),
                const SizedBox(height: 2),
                Row(children: [
                  Flexible(child: Text(a.cat, verbatim: true, maxLines: 1, overflow: TextOverflow.ellipsis, style: inter(13, c: C.muted))),
                  if (a.ratings > 0) ...[
                    Text('  \u2022  ${a.rating.toStringAsFixed(1)} ', verbatim: true, style: inter(13, c: C.muted)),
                    const Icon(Icons.star_rounded, size: 13, color: C.muted),
                  ],
                ]),
              ]),
            ),
            const SizedBox(width: 10),
            _InstallBtn(app: a, dl: dl, onInstall: onInstall, mini: true, soft: true),
          ]),
        ),
      ]),
    );
  }
}

/// The small "Installed" pill under an app that is already on the phone.
class _InstalledChip extends StatelessWidget {
  final StoreApp app;
  const _InstalledChip(this.app);
  @override
  Widget build(BuildContext context) => ValueListenableBuilder<int>(
        valueListenable: InstalledApps.tick,
        builder: (_, __, ___) {
          if (InstalledApps.stateOf(app.pkg, app.version, app.vcode) == 0) return const SizedBox.shrink();
          return Padding(
            padding: const EdgeInsets.only(top: 5),
            child: Container(
              padding: const EdgeInsets.fromLTRB(7, 3, 9, 3),
              decoration: BoxDecoration(color: const Color(0xFF26282B), borderRadius: BorderRadius.circular(8)),
              child: Row(mainAxisSize: MainAxisSize.min, children: [
                Icon(Icons.check_circle_outline_rounded, size: 14, color: C.teal),
                const SizedBox(width: 5),
                Text('Installed', verbatim: true, style: inter(12, c: C.text)),
              ]),
            ),
          );
        },
      );
}

/// One app in a list, laid out like Google Play: big icon, name, category, rating and size. No card behind it.
class _Row extends StatelessWidget {
  final StoreApp app;
  final int? rank;
  final bool mineView, button;
  final ValueListenable<_Dl?> dl;
  final void Function(StoreApp) onOpen, onInstall;
  const _Row({required this.app, required this.rank, required this.mineView, required this.button, required this.dl, required this.onOpen, required this.onInstall});
  @override
  Widget build(BuildContext context) {
    final a = app;
    return Pressable(
      scale: .985,
      haptic: false,
      onTap: () => onOpen(a),
      child: Padding(
        padding: const EdgeInsets.fromLTRB(16, 9, 16, 9),
        child: Row(crossAxisAlignment: CrossAxisAlignment.center, children: [
          if (rank != null) SizedBox(width: 28, child: Text('$rank', verbatim: true, style: inter(15, w: FontWeight.w600, c: rank! <= 3 ? C.teal : C.muted))),
          _Logo(a, 62),
          const SizedBox(width: 14),
          Expanded(
            child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              Text(a.name, verbatim: true, maxLines: 1, overflow: TextOverflow.ellipsis, style: inter(16, w: FontWeight.w500)),
              const SizedBox(height: 2),
              Text('${a.cat} \u2022 ${_cnt(a.downloads)} downloads', verbatim: true, maxLines: 1, overflow: TextOverflow.ellipsis, style: inter(13.5, c: C.muted)),
              const SizedBox(height: 3),
              Row(children: [
                if (a.ratings > 0) ...[
                  Text(a.rating.toStringAsFixed(1), verbatim: true, style: inter(13.5, c: C.muted)),
                  const SizedBox(width: 2),
                  const Icon(Icons.star_rounded, size: 13, color: C.muted),
                  const SizedBox(width: 12),
                ],
                Text(_mb(a.size), verbatim: true, style: inter(13.5, c: C.muted)),
              ]),
              _InstalledChip(a),
            ]),
          ),
          if (a.status != 'approved') ...[const SizedBox(width: 10), _StatusChip(a.status)] else if (button) ...[const SizedBox(width: 10), _InstallBtn(app: a, dl: dl, onInstall: onInstall, mini: true)],
        ]),
      ),
    );
  }
}

class _MineCard extends StatelessWidget {
  final StoreApp app;
  final void Function(StoreApp) onOpen;
  const _MineCard({required this.app, required this.onOpen});

  @override
  Widget build(BuildContext context) {
    final a = app;
    final c = _statusColor(a.status);
    final live = a.status == 'approved';
    return Pressable(
      scale: .985,
      haptic: false,
      onTap: () => onOpen(a),
      child: Padding(
        padding: const EdgeInsets.fromLTRB(16, 4, 16, 4),
        child: Container(
          padding: const EdgeInsets.fromLTRB(10, 9, 12, 9),
          decoration: BoxDecoration(
            color: C.card,
            borderRadius: BorderRadius.circular(18),
            border: Border.all(color: _hair, width: .7),
            boxShadow: const [BoxShadow(color: Color(0x44000000), blurRadius: 10, offset: Offset(0, 3))],
          ),
          child: Column(children: [
            Row(children: [
              _Logo(a, 46),
              const SizedBox(width: 12),
              Expanded(
                child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                  Text(a.name, verbatim: true, maxLines: 1, overflow: TextOverflow.ellipsis, style: inter(15.5, w: FontWeight.w600)),
                  const SizedBox(height: 2),
                  Text('${a.cat}${a.version.isEmpty ? '' : ' · v${a.version}'} · ${_cnt(a.downloads)} downloads · ${_mb(a.size)}', verbatim: true, maxLines: 1, overflow: TextOverflow.ellipsis, style: inter(12.5, c: C.muted)),
                ]),
              ),
              const SizedBox(width: 10),
              if (live)
                const Icon(Icons.chevron_right_rounded, color: C.muted)
              else
                Text(_statusText(a.status), verbatim: true, style: inter(13.5, w: FontWeight.w600, c: c)),
            ]),
          ]),
        ),
      ),
    );
  }
}

class _StatusChip extends StatelessWidget {
  final String status;
  const _StatusChip(this.status);
  @override
  Widget build(BuildContext context) {
    final c = _statusColor(status);
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 11, vertical: 6),
      decoration: BoxDecoration(color: c.withAlpha(34), borderRadius: BorderRadius.circular(16)),
      child: Text(_statusText(status), style: inter(12.5, w: FontWeight.w600, c: c)),
    );
  }
}

class _InstallBtn extends StatelessWidget {
  final StoreApp app;
  final ValueListenable<_Dl?> dl;
  final void Function(StoreApp) onInstall;
  final bool mini;
  /// Not solid: an outlined pill (used on the featured card row).
  final bool soft;
  final double h;
  const _InstallBtn({required this.app, required this.dl, required this.onInstall, this.mini = false, this.soft = false, this.h = 46});
  @override
  Widget build(BuildContext context) => ValueListenableBuilder<int>(
        valueListenable: InstalledApps.tick,
        builder: (_, __, ___) => ValueListenableBuilder<_Dl?>(
        valueListenable: dl,
        builder: (_, d, __) {
          final cur = (d != null && d.id == app.id) ? d : null;
          final other = d != null && cur == null;
          // Like Google Play: Install -> Open once installed (back to Install after an uninstall); Update when the Store has a newer version.
          final st = InstalledApps.stateOf(app.pkg, app.version, app.vcode);
          final label = st == 1 ? 'Open' : (st == 2 ? 'Update' : 'Install');
          final VoidCallback tap = st == 1 ? () => InstalledApps.open(app.pkg) : () => onInstall(app);
          if (mini) {
            if (cur != null) {
              return SizedBox(
                width: 32,
                height: 32,
                child: Stack(alignment: Alignment.center, children: [
                  CircularProgressIndicator(value: cur.p, strokeWidth: 3, color: _btnBlue, backgroundColor: _btnBlueDeep),
                  const Icon(Icons.arrow_downward_rounded, size: 14, color: _btnBlue),
                ]),
              );
            }
            return Opacity(
              opacity: other ? .45 : 1,
              child: Pressable(
                onTap: other ? null : tap,
                child: Container(
                  padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                  decoration: BoxDecoration(
                      color: soft ? Colors.transparent : _btnBlue,
                      border: soft ? Border.all(color: _btnBlue, width: 1.3) : null,
                      borderRadius: BorderRadius.circular(20)),
                  child: Text(label, verbatim: st != 0, style: inter(14, w: FontWeight.w600, c: soft ? _btnBlue : _btnBlueText)),
                ),
              ),
            );
          }
          if (cur != null) {
            final pct = cur.p == null ? null : (cur.p! * 100).round();
            return ClipRRect(
              borderRadius: BorderRadius.circular(h / 2),
              child: SizedBox(
                height: h,
                child: Stack(fit: StackFit.expand, children: [
                  LinearProgressIndicator(value: cur.p, minHeight: h, color: _btnBlue.withAlpha(120), backgroundColor: _btnBlueDeep),
                  Center(child: Text(pct == null ? 'Downloading…' : 'Downloading $pct%', verbatim: true, style: inter(15, w: FontWeight.w600))),
                ]),
              ),
            );
          }
          return Opacity(
            opacity: other ? .5 : 1,
            child: Pressable(
              onTap: other ? null : tap,
              child: Container(
                height: h,
                alignment: Alignment.center,
                decoration: BoxDecoration(
                    color: _btnBlue,
                    borderRadius: BorderRadius.circular(h / 2)),
                child: Text(label, verbatim: st != 0, style: inter(15, w: FontWeight.w600, c: _btnBlueText)),
              ),
            ),
          );
        },
      ));
}

class _Pulse extends StatefulWidget {
  final Widget child;
  const _Pulse({required this.child});
  @override
  State<_Pulse> createState() => _PulseState();
}

class _PulseState extends State<_Pulse> with SingleTickerProviderStateMixin {
  late final AnimationController _c = AnimationController(vsync: this, duration: const Duration(milliseconds: 900))..repeat(reverse: true);
  @override
  void dispose() {
    _c.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => FadeTransition(opacity: Tween<double>(begin: .4, end: 1).animate(CurvedAnimation(parent: _c, curve: Curves.easeInOut)), child: widget.child);
}

// ==================================================================== details page

class _AppPage extends StatefulWidget {
  final StoreApp app;
  final bool mine;
  final List<StoreApp> similar;
  final ValueListenable<_Dl?> dl;
  final void Function(StoreApp) onInstall, onOpen;
  final Future<bool> Function(StoreApp) onDelete;
  final Future<void> Function() onReviewed;
  const _AppPage({required this.app, required this.mine, required this.similar, required this.dl, required this.onInstall, required this.onDelete, required this.onOpen, required this.onReviewed});
  @override
  State<_AppPage> createState() => _AppPageState();
}

class _AppPageState extends State<_AppPage> {
  bool _more = false, _revLoading = true, _allRev = false;
  List<StoreReview> _reviews = [];
  String? _revError;
  late double _avg = widget.app.rating;
  late int _cntR = widget.app.ratings;

  @override
  void initState() {
    super.initState();
    if (widget.app.status == 'approved') {
      _loadReviews();
    } else {
      _revLoading = false;
    }
  }

  Future<void> _loadReviews() async {
    try {
      final r = await StoreCloud.reviews(widget.app.id);
      if (!mounted) return;
      setState(() {
        _reviews = r;
        _revLoading = false;
        _revError = null;
        // The list holds up to 100 reviews; below that it is every review, so the numbers can be exact.
        if (r.length < 100) {
          _cntR = r.length;
          _avg = r.isEmpty ? 0 : r.fold<int>(0, (t, e) => t + e.stars) / r.length;
        }
      });
    } catch (e) {
      if (mounted) {
        setState(() {
          _revLoading = false;
          _revError = _err(e);
        });
      }
    }
  }

  Future<void> _review([int stars = 0]) async {
    final own = _reviews.where((r) => r.mine).toList();
    final done = await showModalBottomSheet<bool>(
      context: context,
      isScrollControlled: true,
      backgroundColor: C.sheet,
      shape: const RoundedRectangleBorder(borderRadius: BorderRadius.vertical(top: Radius.circular(24))),
      builder: (_) => _ReviewSheet(appId: widget.app.id, appName: widget.app.name, existing: own.isEmpty ? null : own.first, stars: stars),
    );
    if (done == true && mounted) {
      await _loadReviews();
      widget.onReviewed();
    }
  }

  Future<void> _reply(StoreReview r) async {
    final done = await showModalBottomSheet<bool>(
      context: context,
      isScrollControlled: true,
      backgroundColor: C.sheet,
      shape: const RoundedRectangleBorder(borderRadius: BorderRadius.vertical(top: Radius.circular(24))),
      builder: (_) => _ReplySheet(review: r),
    );
    if (done == true && mounted) await _loadReviews();
  }

  Widget _ratingsSection() {
    final own = _reviews.where((r) => r.mine).toList();
    final counts = List<int>.filled(6, 0);
    for (final r in _reviews) {
      counts[r.stars] += 1;
    }
    final total = _reviews.length;
    final shown = _allRev ? _reviews : _reviews.take(5).toList();
    return Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
      const _Head('Ratings and reviews'),
      if (_revLoading)
        Padding(padding: EdgeInsets.symmetric(vertical: 24), child: Center(child: SizedBox(width: 26, height: 26, child: IosSpinner(size: 20, color: C.teal))))
      else ...[
        if (_cntR > 0 && total > 0)
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16),
            child: Row(crossAxisAlignment: CrossAxisAlignment.center, children: [
              Column(children: [
                Text(_avg.toStringAsFixed(1), verbatim: true, style: inter(46, w: FontWeight.w600, h: 1.05)),
                const SizedBox(height: 4),
                _Stars(_avg, size: 15),
                const SizedBox(height: 4),
                Text('$_cntR ${_cntR == 1 ? 'review' : 'reviews'}', verbatim: true, style: inter(12, c: C.muted)),
              ]),
              const SizedBox(width: 22),
              Expanded(
                child: Column(children: [
                  for (var s = 5; s >= 1; s--)
                    Padding(
                      padding: const EdgeInsets.symmetric(vertical: 3),
                      child: Row(children: [
                        SizedBox(width: 12, child: Text('$s', verbatim: true, style: inter(12, c: C.muted))),
                        const SizedBox(width: 8),
                        Expanded(child: ClipRRect(borderRadius: BorderRadius.circular(4), child: LinearProgressIndicator(value: total == 0 ? 0 : counts[s] / total, minHeight: 6, color: _starColor, backgroundColor: C.card))),
                      ]),
                    ),
                ]),
              ),
            ]),
          )
        else
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16),
            child: Text(_revError ?? 'No reviews yet. Be the first to rate this app.', verbatim: true, style: inter(14.5, c: C.muted, h: 1.4)),
          ),
        const SizedBox(height: 18),
        if (widget.mine)
          Padding(padding: const EdgeInsets.symmetric(horizontal: 16), child: Text('You can\u2019t review your own app.', verbatim: true, style: inter(13.5, c: C.muted)))
        else if (own.isNotEmpty)
          Padding(padding: const EdgeInsets.symmetric(horizontal: 16), child: _ReviewTile(own.first, onEdit: () => _review(own.first.stars)))
        else
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16),
            child: Container(
              padding: const EdgeInsets.fromLTRB(16, 14, 16, 12),
              decoration: BoxDecoration(color: C.card, borderRadius: BorderRadius.circular(18)),
              child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                Text('Rate this app', style: inter(15.5, w: FontWeight.w600)),
                const SizedBox(height: 2),
                Text('Tell others what you think', style: inter(13, c: C.muted)),
                const SizedBox(height: 8),
                _StarPicker(value: 0, size: 38, onChanged: (n) => _review(n)),
              ]),
            ),
          ),
        if (shown.where((r) => !r.mine).isNotEmpty) ...[
          const SizedBox(height: 6),
          for (final r in shown.where((r) => !r.mine)) Padding(padding: const EdgeInsets.fromLTRB(16, 10, 16, 0), child: _ReviewTile(r, onReply: widget.mine ? () => _reply(r) : null)),
          if (_reviews.where((r) => !r.mine).length > shown.where((r) => !r.mine).length)
            Pressable(
              haptic: false,
              onTap: () => setState(() => _allRev = true),
              child: Padding(padding: const EdgeInsets.fromLTRB(16, 14, 16, 4), child: Text('See all reviews', style: inter(14, w: FontWeight.w600, c: C.teal))),
            ),
        ],
      ],
    ]);
  }

  Widget _stat(String top, String label, {IconData? icon}) => Expanded(
        child: Column(mainAxisSize: MainAxisSize.min, children: [
          if (icon != null)
            Icon(icon, size: 19, color: C.text)
          else
            Text(top, verbatim: true, maxLines: 1, overflow: TextOverflow.ellipsis, style: inter(16, w: FontWeight.w600)),
          const SizedBox(height: 4),
          Text(label, verbatim: true, maxLines: 1, overflow: TextOverflow.ellipsis, style: inter(12, c: C.muted)),
        ]),
      );

  Widget _div() => Container(width: .7, height: 30, color: _hair);

  Widget _chip(IconData ic, String label) => Container(
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
        decoration: BoxDecoration(borderRadius: BorderRadius.circular(12), border: Border.all(color: const Color(0x33FFFFFF), width: .8)),
        child: Row(mainAxisSize: MainAxisSize.min, children: [
          Icon(ic, size: 15, color: C.muted),
          const SizedBox(width: 6),
          Text(label, verbatim: true, style: inter(13.5, w: FontWeight.w500)),
        ]),
      );

  Widget _info(String k, String v) => Padding(
        padding: const EdgeInsets.symmetric(vertical: 9),
        child: Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Text(k, verbatim: true, style: inter(14.5, c: C.muted)),
          const SizedBox(width: 16),
          Expanded(child: Text(v, verbatim: true, textAlign: TextAlign.right, style: inter(14.5))),
        ]),
      );

  Future<void> _updateApp() async {
    final done = await showModalBottomSheet<bool>(
      context: context,
      isScrollControlled: true,
      backgroundColor: C.sheet,
      shape: const RoundedRectangleBorder(borderRadius: BorderRadius.vertical(top: Radius.circular(24))),
      builder: (_) => _PublishSheet(updating: widget.app),
    );
    if (done != true || !mounted) return;
    final live = widget.app.status == 'approved';
    await showInfoDialog(context,
        title: 'App updated',
        message: live ? 'The new version is live. People who installed your app will see Update in the Store.' : 'Your app was sent for review again.');
    if (!mounted) return;
    await widget.onReviewed();
    if (mounted) Navigator.of(context).pop();
  }

  Widget _updateBtn() => Pressable(
        onTap: _updateApp,
        child: Container(
          height: 46,
          alignment: Alignment.center,
          decoration: BoxDecoration(borderRadius: BorderRadius.circular(23), border: Border.all(color: _btnBlue, width: 1.3)),
          child: Row(mainAxisSize: MainAxisSize.min, children: [
            const Icon(Icons.upload_rounded, size: 19, color: _btnBlue),
            const SizedBox(width: 8),
            Text('Update app', style: inter(15, w: FontWeight.w600, c: _btnBlue)),
          ]),
        ),
      );

  Widget _delete() => Pressable(
        onTap: () async {
          final gone = await widget.onDelete(widget.app);
          if (gone && mounted) Navigator.of(context).pop();
        },
        child: Container(
          height: 46,
          alignment: Alignment.center,
          decoration: BoxDecoration(borderRadius: BorderRadius.circular(23), border: Border.all(color: C.red.withAlpha(130), width: 1)),
          child: Text('Delete', style: inter(15, w: FontWeight.w600, c: C.red)),
        ),
      );

  @override
  Widget build(BuildContext context) {
    final a = widget.app;
    final long = a.about.length > 150 || '\n'.allMatches(a.about).length >= 3;
    return Scaffold(
      backgroundColor: C.bg,
      body: SafeArea(
        child: Column(children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(4, 2, 4, 0),
            child: Row(children: [
              IconButton(icon: const Icon(Icons.arrow_back_rounded, color: C.text), onPressed: () => Navigator.of(context).pop()),
            ]),
          ),
          Expanded(
            child: ListView(padding: const EdgeInsets.only(bottom: 36), children: [
              // header
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: 16),
                child: Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
                  _Logo(a, 78),
                  const SizedBox(width: 16),
                  Expanded(
                    child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                      Text(a.name, verbatim: true, maxLines: 2, overflow: TextOverflow.ellipsis, style: inter(22, w: FontWeight.w700, h: 1.2)),
                      const SizedBox(height: 5),
                      Text(a.pkg.isEmpty ? 'Community app' : a.pkg, verbatim: true, maxLines: 1, overflow: TextOverflow.ellipsis, style: inter(14, w: FontWeight.w500, c: C.teal)),
                      const SizedBox(height: 2),
                      Text(a.cat, verbatim: true, style: inter(13, c: C.muted)),
                    ]),
                  ),
                ]),
              ),
              // stats
              Padding(
                padding: const EdgeInsets.fromLTRB(8, 22, 8, 0),
                child: Row(children: [
                  _stat(_cntR > 0 ? '${_avg.toStringAsFixed(1)}\u2605' : 'New', _cntR > 0 ? '$_cntR ${_cntR == 1 ? 'review' : 'reviews'}' : 'No ratings'),
                  _div(),
                  _stat(_cnt(a.downloads), 'Downloads'),
                  _div(),
                  _stat(_mb(a.size), 'Size'),
                  _div(),
                  _stat(a.version.isEmpty ? '—' : 'v${a.version}', 'Version'),
                ]),
              ),
              // actions
              Padding(
                padding: const EdgeInsets.fromLTRB(16, 22, 16, 0),
                child: a.status == 'approved'
                    ? Column(children: [
                        Row(children: [
                          if (widget.mine) ...[Expanded(flex: 2, child: _delete()), const SizedBox(width: 10)],
                          Expanded(flex: 3, child: _InstallBtn(app: a, dl: widget.dl, onInstall: widget.onInstall)),
                        ]),
                        if (widget.mine) ...[const SizedBox(height: 10), _updateBtn()],
                      ])
                    : Column(children: [
                        Container(
                          padding: const EdgeInsets.all(14),
                          decoration: BoxDecoration(color: _statusColor(a.status).withAlpha(28), borderRadius: BorderRadius.circular(16)),
                          child: Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
                            Icon(a.status == 'rejected' ? Icons.block_rounded : Icons.hourglass_top_rounded, size: 20, color: _statusColor(a.status)),
                            const SizedBox(width: 12),
                            Expanded(
                              child: Text(
                                a.status == 'rejected' ? 'This app was rejected, so it isn’t shown in the Store.' : 'This app is waiting for review. It appears in the Store once the admin approves it.',
                                verbatim: true,
                                style: inter(14, h: 1.35),
                              ),
                            ),
                          ]),
                        ),
                        if (widget.mine) ...[const SizedBox(height: 10), _updateBtn(), const SizedBox(height: 10), _delete()],
                      ]),
              ),
              if (a.status == 'approved')
                Padding(
                  padding: const EdgeInsets.fromLTRB(18, 14, 18, 0),
                  child: Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
                    const Padding(padding: EdgeInsets.only(top: 1), child: Icon(Icons.shield_outlined, size: 16, color: C.muted)),
                    const SizedBox(width: 8),
                    Expanded(child: Text('Installs from this Store, not Google Play. Android may ask you to allow “install unknown apps” the first time.', verbatim: true, style: inter(12.5, c: C.muted, h: 1.35))),
                  ]),
                ),
              // chips (Play-style tags)
              Padding(
                padding: const EdgeInsets.fromLTRB(16, 16, 16, 0),
                child: Wrap(spacing: 8, runSpacing: 8, children: [
                  _chip(_catIcon(a.cat), a.cat),
                  _chip(Icons.sell_outlined, 'Free'),
                  if (a.status == 'approved') _chip(Icons.verified_outlined, 'Checked by admin'),
                  if (a.downloads > 0) _chip(Icons.download_rounded, '${_cnt(a.downloads)} downloads'),
                ]),
              ),
              // screenshots (only built when the app has some; each loads when it scrolls into view)
              if (a.shots.isNotEmpty) ...[
                const _Head('Screenshots'),
                _ShotStrip(a.shotUrls),
              ],
              // about
              const _Head('About this app'),
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: 16),
                child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                  AnimatedSize(
                    duration: const Duration(milliseconds: 200),
                    alignment: Alignment.topCenter,
                    curve: Curves.easeOut,
                    child: Text(
                      a.about.isEmpty ? 'The developer hasn’t added a description yet.' : a.about,
                      verbatim: true,
                      maxLines: _more ? null : 4,
                      overflow: _more ? TextOverflow.visible : TextOverflow.ellipsis,
                      style: inter(14.5, c: a.about.isEmpty ? C.muted : const Color(0xFFD6D6D9), h: 1.45),
                    ),
                  ),
                  if (long)
                    Pressable(
                      haptic: false,
                      onTap: () => setState(() => _more = !_more),
                      child: Padding(
                        padding: const EdgeInsets.only(top: 8, bottom: 2),
                        child: Text(_more ? 'Show less' : 'Read more', style: inter(14, w: FontWeight.w600, c: C.teal)),
                      ),
                    ),
                  const SizedBox(height: 12),
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 7),
                    decoration: BoxDecoration(borderRadius: BorderRadius.circular(10), border: Border.all(color: const Color(0x33FFFFFF), width: .8)),
                    child: Row(mainAxisSize: MainAxisSize.min, children: [
                      Icon(_catIcon(a.cat), size: 16, color: C.muted),
                      const SizedBox(width: 7),
                      Text(a.cat, verbatim: true, style: inter(13.5)),
                    ]),
                  ),
                ]),
              ),
              if (a.status == 'approved') _ratingsSection(),
              // app info
              const _Head('App info'),
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: 16),
                child: Container(
                  padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 6),
                  decoration: BoxDecoration(color: C.card, borderRadius: BorderRadius.circular(18)),
                  child: Column(children: [
                    _info('Version', a.version.isEmpty ? '—' : a.version),
                    _info('Package', a.pkg.isEmpty ? '—' : a.pkg),
                    _info('Size', _mb(a.size)),
                    _info('Downloads', '${a.downloads}'),
                    _info('Added', _date(a.created)),
                  ]),
                ),
              ),
              if (widget.similar.isNotEmpty) _Carousel(title: 'Similar apps', apps: widget.similar.take(10).toList(), onOpen: widget.onOpen),
            ]),
          ),
        ]),
      ),
    );
  }
}

// ==================================================================== screenshots

/// Horizontal strip of screenshots. Built lazily (only the pictures on screen load) and decoded at the size they
/// are shown, so it stays light on data and memory. Nothing is written to the phone's storage.
class _ShotStrip extends StatelessWidget {
  final List<String> urls;
  const _ShotStrip(this.urls);

  @override
  Widget build(BuildContext context) {
    final mq = MediaQuery.of(context);
    final cw = (mq.size.width * .46).clamp(150.0, 230.0);
    final ch = cw * 1.9;
    final px = (cw * mq.devicePixelRatio).round().clamp(240, 640);
    return SizedBox(
      height: ch,
      child: ListView.separated(
        scrollDirection: Axis.horizontal,
        padding: const EdgeInsets.symmetric(horizontal: 16),
        itemCount: urls.length,
        separatorBuilder: (_, __) => const SizedBox(width: 10),
        itemBuilder: (c, i) => Pressable(
          haptic: false,
          scale: .98,
          onTap: () => Navigator.of(c).push(MaterialPageRoute(fullscreenDialog: true, builder: (_) => _ShotViewer(urls: urls, index: i))),
          child: Container(
            width: cw,
            height: ch,
            decoration: BoxDecoration(color: _skel, borderRadius: BorderRadius.circular(16), border: Border.all(color: _hair, width: .7)),
            clipBehavior: Clip.antiAlias,
            child: Unfilter(
              child: Image.network(
                urls[i],
                fit: BoxFit.cover,
                cacheWidth: px,
                gaplessPlayback: true,
                frameBuilder: (_, child, frame, sync) => sync ? child : AnimatedOpacity(opacity: frame == null ? 0 : 1, duration: const Duration(milliseconds: 220), child: child),
                errorBuilder: (_, __, ___) => const Center(child: Icon(Icons.broken_image_outlined, color: C.muted, size: 26)),
              ),
            ),
          ),
        ),
      ),
    );
  }
}

/// Full screen view of the screenshots: swipe between them, pinch to zoom. A picture is only fetched when its page opens.
class _ShotViewer extends StatefulWidget {
  final List<String> urls;
  final int index;
  const _ShotViewer({required this.urls, required this.index});
  @override
  State<_ShotViewer> createState() => _ShotViewerState();
}

class _ShotViewerState extends State<_ShotViewer> {
  late final PageController _pc = PageController(initialPage: widget.index);
  late int _i = widget.index;

  @override
  void dispose() {
    _pc.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final mq = MediaQuery.of(context);
    final px = (mq.size.width * mq.devicePixelRatio).round().clamp(480, 1080);
    return Scaffold(
      backgroundColor: Colors.black,
      body: Stack(children: [
        PageView.builder(
          controller: _pc,
          itemCount: widget.urls.length,
          onPageChanged: (v) => setState(() => _i = v),
          itemBuilder: (_, i) => InteractiveViewer(
            minScale: 1,
            maxScale: 4,
            child: Center(
              child: Unfilter(
                child: Image.network(
                  widget.urls[i],
                  fit: BoxFit.contain,
                  cacheWidth: px,
                  gaplessPlayback: true,
                  loadingBuilder: (_, child, p) => p == null ? child : const Center(child: SizedBox(width: 26, height: 26, child: CircularProgressIndicator(strokeWidth: 2.4, color: Colors.white54))),
                  errorBuilder: (_, __, ___) => const Icon(Icons.broken_image_outlined, color: Colors.white54, size: 36),
                ),
              ),
            ),
          ),
        ),
        SafeArea(
          child: Padding(
            padding: const EdgeInsets.fromLTRB(4, 2, 4, 0),
            child: Row(children: [
              IconButton(icon: const Icon(Icons.close_rounded, color: Colors.white), onPressed: () => Navigator.of(context).pop()),
              const Spacer(),
              if (widget.urls.length > 1) Padding(padding: const EdgeInsets.only(right: 14), child: Text('${_i + 1} / ${widget.urls.length}', verbatim: true, style: inter(14, w: FontWeight.w500, c: Colors.white70))),
            ]),
          ),
        ),
      ]),
    );
  }
}

// ==================================================================== ratings & reviews

class _Stars extends StatelessWidget {
  final double value;
  final double size;
  const _Stars(this.value, {this.size = 14});
  @override
  Widget build(BuildContext context) => Row(mainAxisSize: MainAxisSize.min, children: [
        for (var i = 1; i <= 5; i++)
          Icon(value >= i - .25 ? Icons.star_rounded : (value >= i - .75 ? Icons.star_half_rounded : Icons.star_outline_rounded), size: size, color: value >= i - .75 ? _starColor : C.muted),
      ]);
}

class _StarPicker extends StatelessWidget {
  final int value;
  final double size;
  final ValueChanged<int> onChanged;
  const _StarPicker({required this.value, required this.onChanged, this.size = 40});
  @override
  Widget build(BuildContext context) => Row(mainAxisSize: MainAxisSize.min, children: [
        for (var i = 1; i <= 5; i++)
          Pressable(
            scale: .88,
            onTap: () => onChanged(i),
            child: Padding(
              padding: const EdgeInsets.only(right: 4),
              child: Icon(i <= value ? Icons.star_rounded : Icons.star_outline_rounded, size: size, color: i <= value ? _starColor : C.muted, semanticLabel: '$i stars'),
            ),
          ),
      ]);
}

/// A reviewer's picture. With no photo it is always the same silhouette, never a letter.
class _Avatar extends StatelessWidget {
  final String url;
  final double size;
  const _Avatar(this.url, {this.size = 34});
  @override
  Widget build(BuildContext context) {
    final blank = Icon(Icons.person_rounded, size: size * .64, color: const Color(0xFF9A9AA0));
    return Container(
      width: size,
      height: size,
      clipBehavior: Clip.antiAlias,
      alignment: Alignment.center,
      decoration: const BoxDecoration(color: Color(0xFF3A3A3F), shape: BoxShape.circle),
      child: url.isEmpty
          ? blank
          : Unfilter(child: Image.network(url, width: size, height: size, fit: BoxFit.cover, cacheWidth: (size * 3).round(), gaplessPlayback: true, errorBuilder: (_, __, ___) => blank)),
    );
  }
}

class _ReviewTile extends StatelessWidget {
  final StoreReview r;
  final VoidCallback? onEdit;
  final VoidCallback? onReply; // only for the owner of the app
  const _ReviewTile(this.r, {this.onEdit, this.onReply});
  @override
  Widget build(BuildContext context) => Container(
        width: double.infinity,
        padding: const EdgeInsets.all(14),
        decoration: BoxDecoration(color: C.card, borderRadius: BorderRadius.circular(18), border: r.mine ? Border.all(color: C.teal.withAlpha(90), width: 1) : null),
        child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Row(children: [
            _Avatar(r.avatarUrl, size: 34),
            const SizedBox(width: 10),
            Expanded(child: Text(r.mine ? 'Your review' : r.name, verbatim: true, maxLines: 1, overflow: TextOverflow.ellipsis, style: inter(14.5, w: FontWeight.w600))),
            if (onEdit != null)
              Pressable(
                haptic: false,
                onTap: onEdit,
                child: Padding(padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 6), child: Text('Edit', style: inter(14, w: FontWeight.w600, c: C.teal))),
              ),
          ]),
          const SizedBox(height: 8),
          Row(children: [
            _Stars(r.stars.toDouble(), size: 14),
            const SizedBox(width: 8),
            Text(_date(r.created), verbatim: true, style: inter(12, c: C.muted)),
          ]),
          if (r.body.isNotEmpty) ...[
            const SizedBox(height: 8),
            Text(r.body, verbatim: true, style: inter(14, c: const Color(0xFFD6D6D9), h: 1.4)),
          ],
          if (r.reply.isNotEmpty) ...[
            const SizedBox(height: 12),
            Container(
              width: double.infinity,
              padding: const EdgeInsets.fromLTRB(12, 10, 12, 12),
              decoration: BoxDecoration(color: C.tealDeep.withAlpha(120), borderRadius: BorderRadius.circular(12)),
              child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                Row(children: [
                  Icon(Icons.subdirectory_arrow_right_rounded, size: 16, color: C.teal),
                  const SizedBox(width: 6),
                  Expanded(child: Text('Developer reply', verbatim: true, style: inter(13, w: FontWeight.w600, c: C.teal))),
                  Text(_date(r.replyAt), verbatim: true, style: inter(11.5, c: C.muted)),
                ]),
                const SizedBox(height: 6),
                Text(r.reply, verbatim: true, style: inter(13.5, c: const Color(0xFFD6D6D9), h: 1.4)),
              ]),
            ),
          ],
          if (onReply != null) ...[
            const SizedBox(height: 6),
            Align(
              alignment: Alignment.centerLeft,
              child: Pressable(
                haptic: false,
                onTap: onReply,
                child: Padding(
                  padding: const EdgeInsets.symmetric(vertical: 6),
                  child: Row(mainAxisSize: MainAxisSize.min, children: [
                    Icon(Icons.reply_rounded, size: 17, color: C.teal),
                    const SizedBox(width: 5),
                    Text(r.reply.isEmpty ? 'Reply' : 'Edit reply', verbatim: true, style: inter(14, w: FontWeight.w600, c: C.teal)),
                  ]),
                ),
              ),
            ),
          ],
        ]),
      );
}

/// The app owner writes (or edits / removes) the reply to one review.
class _ReplySheet extends StatefulWidget {
  final StoreReview review;
  final bool embedded;
  final VoidCallback? onDone;
  const _ReplySheet({required this.review, this.embedded = false, this.onDone});
  @override
  State<_ReplySheet> createState() => _ReplySheetState();
}

class _ReplySheetState extends State<_ReplySheet> {
  late final _body = TextEditingController(text: widget.review.reply);
  bool _busy = false;
  String? _error;

  @override
  void dispose() {
    _body.dispose();
    super.dispose();
  }

  Future<void> _run(String text) async {
    setState(() {
      _busy = true;
      _error = null;
    });
    try {
      await StoreCloud.replyReview(widget.review.id, text);
      if (mounted) {
        if (widget.embedded) {
          widget.onDone?.call();
        } else {
          Navigator.pop(context, true);
        }
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _busy = false;
          _error = _err(e);
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final r = widget.review;
    final edit = r.reply.isNotEmpty;
    final empty = _body.text.trim().isEmpty;
    return Padding(
      padding: EdgeInsets.fromLTRB(20, 12, 20, 20 + MediaQuery.of(context).viewInsets.bottom),
      child: SingleChildScrollView(
        child: Column(mainAxisSize: MainAxisSize.min, crossAxisAlignment: CrossAxisAlignment.start, children: [
          Center(child: Container(width: 38, height: 4, decoration: BoxDecoration(color: const Color(0x33FFFFFF), borderRadius: BorderRadius.circular(2)))),
          const SizedBox(height: 16),
          Text(edit ? 'Edit your reply' : 'Reply to review', style: inter(21, w: FontWeight.w700)),
          const SizedBox(height: 12),
          Container(
            width: double.infinity,
            padding: const EdgeInsets.all(12),
            decoration: BoxDecoration(color: C.card, borderRadius: BorderRadius.circular(14)),
            child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              Row(children: [
                Expanded(child: Text(r.name, verbatim: true, maxLines: 1, overflow: TextOverflow.ellipsis, style: inter(14, w: FontWeight.w600))),
                _Stars(r.stars.toDouble(), size: 13),
              ]),
              if (r.body.isNotEmpty) ...[
                const SizedBox(height: 6),
                Text(r.body, verbatim: true, maxLines: 4, overflow: TextOverflow.ellipsis, style: inter(13.5, c: const Color(0xFFD6D6D9), h: 1.35)),
              ],
            ]),
          ),
          const SizedBox(height: 14),
          TextField(
            controller: _body,
            enabled: !_busy,
            autofocus: true,
            minLines: 3,
            maxLines: 5,
            maxLength: 500,
            onChanged: (_) => setState(() {}),
            style: inter(16),
            cursorColor: C.teal,
            decoration: InputDecoration(
              labelText: 'Your reply (shown publicly)',
              labelStyle: inter(15, c: C.muted),
              floatingLabelStyle: inter(14, c: C.teal),
              filled: true,
              fillColor: C.field,
              counterText: '',
              enabledBorder: OutlineInputBorder(borderRadius: BorderRadius.circular(14), borderSide: const BorderSide(color: _hair)),
              focusedBorder: OutlineInputBorder(borderRadius: BorderRadius.circular(14), borderSide: BorderSide(color: C.teal, width: 1.4)),
              disabledBorder: OutlineInputBorder(borderRadius: BorderRadius.circular(14), borderSide: const BorderSide(color: _hair)),
            ),
          ),
          if (_error != null) ...[
            const SizedBox(height: 12),
            Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
              const Icon(Icons.error_outline_rounded, size: 18, color: C.red),
              const SizedBox(width: 8),
              Expanded(child: Text(_error!, verbatim: true, style: inter(14, c: C.red))),
            ]),
          ],
          const SizedBox(height: 18),
          Row(children: [
            if (edit) ...[
              Expanded(
                flex: 2,
                child: Pressable(
                  onTap: _busy ? null : () => _run(''),
                  child: Container(
                    height: 50,
                    alignment: Alignment.center,
                    decoration: BoxDecoration(borderRadius: BorderRadius.circular(25), border: Border.all(color: C.red.withAlpha(130))),
                    child: Text('Delete', style: inter(15, w: FontWeight.w600, c: C.red)),
                  ),
                ),
              ),
              const SizedBox(width: 10),
            ],
            Expanded(
              flex: 3,
              child: Pressable(
                onTap: _busy || empty ? null : () => _run(_body.text.trim()),
                child: Container(
                  height: 50,
                  alignment: Alignment.center,
                  decoration: BoxDecoration(color: _busy || empty ? C.tealDeep : C.tealBtn, borderRadius: BorderRadius.circular(25)),
                  child: _busy
                      ? IosSpinner(size: 22, color: C.teal)
                      : Text(edit ? 'Save reply' : 'Post reply', verbatim: true, style: inter(16, w: FontWeight.w600, c: empty ? C.muted : C.onTeal)),
                ),
              ),
            ),
          ]),
        ]),
      ),
    );
  }
}

class _ReviewSheet extends StatefulWidget {
  final String appId, appName;
  final StoreReview? existing;
  final int stars;
  const _ReviewSheet({required this.appId, required this.appName, required this.existing, required this.stars});
  @override
  State<_ReviewSheet> createState() => _ReviewSheetState();
}

class _ReviewSheetState extends State<_ReviewSheet> {
  late int _stars = widget.existing?.stars ?? widget.stars;
  late final _name = TextEditingController(text: widget.existing?.name ?? StoreCloud.reviewerName);
  late final _body = TextEditingController(text: widget.existing?.body ?? '');
  bool _busy = false;
  String? _error;
  String _photo = StoreCloud.myAvatarPath;
  bool _photoBusy = false;

  @override
  void initState() {
    super.initState();
    StoreCloud.refreshAvatar().then((p) {
      if (mounted && !_photoBusy) setState(() => _photo = p);
    });
  }

  Future<void> _pickPhoto() async {
    final f = await pickIconImage(context);
    if (f == null || !mounted) return;
    setState(() {
      _photoBusy = true;
      _error = null;
    });
    try {
      await StoreCloud.setAvatar(f.path);
      if (mounted) setState(() => _photo = StoreCloud.myAvatarPath);
    } catch (e) {
      if (mounted) setState(() => _error = _err(e));
    }
    if (mounted) setState(() => _photoBusy = false);
  }

  Future<void> _removePhoto() async {
    setState(() {
      _photoBusy = true;
      _error = null;
    });
    try {
      await StoreCloud.removeAvatar();
      if (mounted) setState(() => _photo = '');
    } catch (e) {
      if (mounted) setState(() => _error = _err(e));
    }
    if (mounted) setState(() => _photoBusy = false);
  }

  @override
  void dispose() {
    _name.dispose();
    _body.dispose();
    super.dispose();
  }

  Future<void> _run(Future<void> Function() f) async {
    setState(() {
      _busy = true;
      _error = null;
    });
    try {
      await f();
      if (mounted) Navigator.pop(context, true);
    } catch (e) {
      if (mounted) {
        setState(() {
          _busy = false;
          _error = _err(e);
        });
      }
    }
  }

  void _send() {
    if (_stars < 1) {
      setState(() => _error = 'Tap a star to rate.');
      return;
    }
    _run(() => StoreCloud.saveReview(widget.appId, _stars, _body.text.trim(), _name.text.trim()));
  }

  InputDecoration _dec(String label) => InputDecoration(
        labelText: label,
        labelStyle: inter(15, c: C.muted),
        floatingLabelStyle: inter(14, c: C.teal),
        filled: true,
        fillColor: C.field,
        counterText: '',
        enabledBorder: OutlineInputBorder(borderRadius: BorderRadius.circular(14), borderSide: const BorderSide(color: _hair)),
        focusedBorder: OutlineInputBorder(borderRadius: BorderRadius.circular(14), borderSide: BorderSide(color: C.teal, width: 1.4)),
        disabledBorder: OutlineInputBorder(borderRadius: BorderRadius.circular(14), borderSide: const BorderSide(color: _hair)),
      );

  @override
  Widget build(BuildContext context) {
    final edit = widget.existing != null;
    return Padding(
      padding: EdgeInsets.fromLTRB(20, 12, 20, 20 + MediaQuery.of(context).viewInsets.bottom),
      child: SingleChildScrollView(
        child: Column(mainAxisSize: MainAxisSize.min, crossAxisAlignment: CrossAxisAlignment.start, children: [
          Center(child: Container(width: 38, height: 4, decoration: BoxDecoration(color: const Color(0x33FFFFFF), borderRadius: BorderRadius.circular(2)))),
          const SizedBox(height: 16),
          Text(edit ? 'Edit your review' : 'Rate this app', style: inter(21, w: FontWeight.w700)),
          const SizedBox(height: 4),
          Text(widget.appName, verbatim: true, maxLines: 1, overflow: TextOverflow.ellipsis, style: inter(13.5, c: C.muted)),
          const SizedBox(height: 16),
          Row(children: [
            Pressable(
              onTap: _busy || _photoBusy ? null : _pickPhoto,
              child: SizedBox(
                width: 62,
                height: 62,
                child: Stack(children: [
                  Positioned(left: 0, top: 0, child: _Avatar(StoreCloud.avatarUrl(_photo), size: 58)),
                  Positioned(
                    right: 0,
                    bottom: 0,
                    child: Container(
                      width: 24,
                      height: 24,
                      decoration: BoxDecoration(color: const Color(0xFF2A2A2C), shape: BoxShape.circle, border: Border.all(color: C.sheet, width: 2)),
                      child: const Icon(Icons.camera_alt_rounded, size: 13, color: C.text),
                    ),
                  ),
                  if (_photoBusy) Positioned(left: 0, top: 0, child: Container(width: 58, height: 58, decoration: const BoxDecoration(color: Color(0x88000000), shape: BoxShape.circle), child: Center(child: IosSpinner(size: 22, color: C.teal)))),
                ]),
              ),
            ),
            const SizedBox(width: 14),
            Expanded(
              child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                Text('Profile photo', style: inter(15, w: FontWeight.w600)),
                const SizedBox(height: 2),
                Row(children: [
                  Pressable(
                    haptic: false,
                    onTap: _busy || _photoBusy ? null : _pickPhoto,
                    child: Padding(padding: const EdgeInsets.symmetric(vertical: 6), child: Text(_photo.isEmpty ? 'Upload photo' : 'Change photo', style: inter(14, w: FontWeight.w600, c: C.teal))),
                  ),
                  if (_photo.isNotEmpty) ...[
                    const SizedBox(width: 18),
                    Pressable(
                      haptic: false,
                      onTap: _busy || _photoBusy ? null : _removePhoto,
                      child: Padding(padding: const EdgeInsets.symmetric(vertical: 6), child: Text('Remove', style: inter(14, w: FontWeight.w600, c: C.muted))),
                    ),
                  ],
                ]),
              ]),
            ),
          ]),
          const SizedBox(height: 16),
          Center(child: _StarPicker(value: _stars, size: 44, onChanged: (n) => setState(() => _stars = _busy ? _stars : n))),
          const SizedBox(height: 16),
          TextField(controller: _name, enabled: !_busy, maxLength: 40, style: inter(16), cursorColor: C.teal, decoration: _dec('Your name (optional)')),
          const SizedBox(height: 12),
          TextField(controller: _body, enabled: !_busy, minLines: 3, maxLines: 5, maxLength: 500, style: inter(16), cursorColor: C.teal, decoration: _dec('Write a review (optional)')),
          if (_error != null) ...[
            const SizedBox(height: 12),
            Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
              const Icon(Icons.error_outline_rounded, size: 18, color: C.red),
              const SizedBox(width: 8),
              Expanded(child: Text(_error!, verbatim: true, style: inter(14, c: C.red))),
            ]),
          ],
          const SizedBox(height: 18),
          Row(children: [
            if (edit) ...[
              Expanded(
                flex: 2,
                child: Pressable(
                  onTap: _busy ? null : () => _run(() => StoreCloud.deleteReview(widget.appId)),
                  child: Container(
                    height: 50,
                    alignment: Alignment.center,
                    decoration: BoxDecoration(borderRadius: BorderRadius.circular(25), border: Border.all(color: C.red.withAlpha(130))),
                    child: Text('Delete', style: inter(15, w: FontWeight.w600, c: C.red)),
                  ),
                ),
              ),
              const SizedBox(width: 10),
            ],
            Expanded(
              flex: 3,
              child: Pressable(
                onTap: _busy ? null : _send,
                child: Container(
                  height: 50,
                  alignment: Alignment.center,
                  decoration: BoxDecoration(color: _stars < 1 || _busy ? C.tealDeep : C.tealBtn, borderRadius: BorderRadius.circular(25)),
                  child: _busy
                      ? IosSpinner(size: 22, color: C.teal)
                      : Text(edit ? 'Save' : 'Post review', style: inter(16, w: FontWeight.w600, c: _stars < 1 ? C.muted : C.onTeal)),
                ),
              ),
            ),
          ]),
        ]),
      ),
    );
  }
}

// ==================================================================== publish sheet

class _PublishSheet extends StatefulWidget {
  /// When set, the sheet publishes a new version of this app instead of a new app.
  final StoreApp? updating;
  /// True when the form sits inside the My apps tab instead of a bottom sheet.
  final bool embedded;
  final VoidCallback? onDone;
  const _PublishSheet({super.key, this.updating, this.embedded = false, this.onDone});
  @override
  State<_PublishSheet> createState() => _PublishSheetState();
}

class _PublishSheetState extends State<_PublishSheet> with AutomaticKeepAliveClientMixin {
  @override
  bool get wantKeepAlive => widget.embedded;

  final _name = TextEditingController(), _about = TextEditingController();
  XFile? _apk, _logo;
  List<XFile> _shots = [];
  int _apkSize = 0;
  String _cat = 'Tools';
  bool _busy = false;
  double? _p;
  String? _error;

  @override
  void initState() {
    super.initState();
    final u = widget.updating;
    if (u != null) {
      _name.text = u.name;
      _about.text = u.about;
      if (kStoreCats.contains(u.cat)) _cat = u.cat;
    }
  }

  @override
  void dispose() {
    _name.dispose();
    _about.dispose();
    super.dispose();
  }

  Future<void> _pickApk() async {
    final f = await pickApkFile();
    if (f == null) return;
    final n = await f.length();
    setState(() {
      _apk = f;
      _apkSize = n;
      _error = n > kStoreMaxMb * 1048576 ? 'That APK is over $kStoreMaxMb MB.' : null;
      if (widget.updating == null && _name.text.trim().isEmpty) _name.text = f.name.replaceAll(RegExp(r'\.apk$', caseSensitive: false), '').replaceAll(RegExp(r'[_-]+'), ' ').trim();
    });
  }

  Future<void> _pickShots() async {
    if (_shots.length >= kStoreMaxShots) return;
    final f = await pickIconImage(context); // the app's own gallery, one picture at a time
    if (f == null || !mounted) return;
    setState(() {
      _shots = [..._shots, XFile(f.path, name: f.name)];
      if (_error != null && _error!.contains('required')) _error = null;
    });
  }

  Widget _shotTile({String? url, XFile? file, VoidCallback? onRemove}) => SizedBox(
        width: 66,
        height: 118,
        child: Stack(children: [
          Positioned.fill(
            child: ClipRRect(
              borderRadius: BorderRadius.circular(11),
              child: Unfilter(
                child: file != null
                    ? Image.file(File(file.path), fit: BoxFit.cover, cacheWidth: 200, errorBuilder: (_, __, ___) => const ColoredBox(color: _skel))
                    : Image.network(url!, fit: BoxFit.cover, cacheWidth: 200, errorBuilder: (_, __, ___) => const ColoredBox(color: _skel)),
              ),
            ),
          ),
          if (onRemove != null)
            Positioned(
              top: 3,
              right: 3,
              child: GestureDetector(
                onTap: _busy ? null : onRemove,
                child: Container(width: 22, height: 22, decoration: const BoxDecoration(color: Color(0xCC000000), shape: BoxShape.circle), child: const Icon(Icons.close_rounded, size: 14, color: Colors.white)),
              ),
            ),
        ]),
      );

  Future<void> _pickLogo() async {
    final f = await pickIconImage(context); // the app's own gallery
    if (f == null || !mounted) return;
    setState(() {
      _logo = XFile(f.path, name: f.name);
      if (_error != null && _error!.contains('required')) _error = null;
    });
  }

  bool get _hasLogo => _logo != null || (widget.updating?.logoPath.isNotEmpty ?? false);
  bool get _hasShots => _shots.isNotEmpty || (widget.updating?.shots.isNotEmpty ?? false);

  Future<void> _send() async {
    if (_apk == null || _error != null) return;
    if (!_hasLogo || !_hasShots) {
      setState(() => _error = !_hasLogo ? 'Add an app icon. It’s required.' : 'Add at least one screenshot. It’s required.');
      return;
    }
    if (_name.text.trim().isEmpty) {
      setState(() => _error = 'Give your app a name.');
      return;
    }
    if (!await File(_apk!.path).exists()) {
      // Android can empty the cache while the form is open. Say so plainly instead of showing a file path.
      setState(() {
        _apk = null;
        _apkSize = 0;
        _error = 'Android removed the APK file. Tap the APK box and choose it again.';
      });
      return;
    }
    setState(() {
      _busy = true;
      _error = null;
      _p = 0;
    });
    try {
      void prog(int s, int t) {
        if (mounted && t > 0) setState(() => _p = s / t);
      }

      final u = widget.updating;
      if (u != null) {
        await StoreCloud.update(u, _apk!.path, name: _name.text.trim(), about: _about.text.trim(), cat: _cat, logoPath: _logo?.path, shotPaths: [for (final f in _shots) f.path], onProgress: prog);
      } else {
        await StoreCloud.publish(_apk!.path, name: _name.text.trim(), about: _about.text.trim(), cat: _cat, logoPath: _logo?.path, shotPaths: [for (final f in _shots) f.path], onProgress: prog);
      }
      if (mounted) {
        // Embedded in the My apps tab there is no sheet to close: popping here would remove the whole Store screen (black).
        if (widget.embedded) {
          widget.onDone?.call();
        } else {
          Navigator.pop(context, true);
        }
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _busy = false;
          _error = _err(e);
        });
      }
    }
  }

  InputDecoration _dec(String label) => InputDecoration(
        labelText: label,
        labelStyle: inter(15, c: C.muted),
        floatingLabelStyle: inter(14, c: C.teal),
        filled: true,
        fillColor: C.field,
        counterText: '',
        enabledBorder: OutlineInputBorder(borderRadius: BorderRadius.circular(14), borderSide: const BorderSide(color: _hair)),
        focusedBorder: OutlineInputBorder(borderRadius: BorderRadius.circular(14), borderSide: BorderSide(color: C.teal, width: 1.4)),
        disabledBorder: OutlineInputBorder(borderRadius: BorderRadius.circular(14), borderSide: const BorderSide(color: _hair)),
      );

  @override
  Widget build(BuildContext context) {
    super.build(context);
    final bad = _apk == null || _error != null || !_hasLogo || !_hasShots;
    final emb = widget.embedded;
    final column = Column(mainAxisSize: MainAxisSize.min, crossAxisAlignment: CrossAxisAlignment.start, children: [
          if (!emb) ...[
            Center(child: Container(width: 38, height: 4, decoration: BoxDecoration(color: const Color(0x33FFFFFF), borderRadius: BorderRadius.circular(2)))),
            const SizedBox(height: 16),
          ],
          Text(widget.updating != null ? 'Update app' : 'Publish an app', style: inter(21, w: FontWeight.w700)),
          const SizedBox(height: 4),
          Text(
              widget.updating != null
                  ? 'Pick the new APK. It must keep the same package name and have a higher version${widget.updating!.version.isEmpty ? '' : ' than v${widget.updating!.version}'}.'
                  : 'The admin checks every app before it appears in the Store.',
              verbatim: widget.updating != null,
              style: inter(13, c: C.muted)),
          const SizedBox(height: 18),
          // 1. APK
          Pressable(
            onTap: _busy ? null : _pickApk,
            child: Container(
              padding: const EdgeInsets.all(14),
              decoration: BoxDecoration(color: C.field, borderRadius: BorderRadius.circular(16), border: Border.all(color: _apk == null ? const Color(0x33FFFFFF) : C.teal.withAlpha(120), width: 1)),
              child: Row(children: [
                Container(
                  width: 44,
                  height: 44,
                  decoration: BoxDecoration(color: Color(0xFF2C2C2F), borderRadius: BorderRadius.circular(12)),
                  child: Icon(_apk == null ? Icons.upload_file_rounded : Icons.android_rounded, color: Colors.white),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                    Text(_apk == null ? (widget.updating != null ? 'Choose the new APK' : 'Choose APK file') : _apk!.name, verbatim: true, maxLines: 1, overflow: TextOverflow.ellipsis, style: inter(15, w: FontWeight.w500)),
                    const SizedBox(height: 2),
                    Text(_apk == null ? 'Up to $kStoreMaxMb MB' : _mb(_apkSize), verbatim: true, style: inter(12.5, c: C.muted)),
                  ]),
                ),
                if (_apk != null) Icon(Icons.check_circle_rounded, color: C.teal, size: 22),
              ]),
            ),
          ),
          const SizedBox(height: 14),
          // 2. icon + name
          Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
            Pressable(
              onTap: _busy ? null : _pickLogo,
              child: Container(
                width: 58,
                height: 58,
                decoration: BoxDecoration(color: C.field, borderRadius: BorderRadius.circular(14), border: Border.all(color: const Color(0x33FFFFFF), width: 1)),
                child: _logo == null && widget.updating != null && widget.updating!.logoUrl.isNotEmpty
                    ? ClipRRect(borderRadius: BorderRadius.circular(13), child: Image.network(widget.updating!.logoUrl, fit: BoxFit.cover, cacheWidth: 200, errorBuilder: (_, __, ___) => Icon(Icons.image_rounded, color: C.teal)))
                    : _logo == null
                    ? const Icon(Icons.add_photo_alternate_outlined, color: C.muted, size: 26)
                    : ClipRRect(borderRadius: BorderRadius.circular(13), child: Image.file(File(_logo!.path), fit: BoxFit.cover, cacheWidth: 200, errorBuilder: (_, __, ___) => Icon(Icons.image_rounded, color: C.teal))),
              ),
            ),
            const SizedBox(width: 12),
            Expanded(child: TextField(controller: _name, enabled: !_busy, maxLength: 60, style: inter(16), cursorColor: C.teal, decoration: _dec('App name'))),
          ]),
          const SizedBox(height: 4),
          Padding(padding: const EdgeInsets.only(left: 2), child: Text(widget.updating != null ? 'Tap the square to change the icon (required)' : 'Tap the square to add an icon (required)', style: inter(12, c: _hasLogo ? C.muted : C.red))),
          const SizedBox(height: 12),
          TextField(controller: _about, enabled: !_busy, minLines: 3, maxLines: 5, maxLength: 1000, style: inter(16), cursorColor: C.teal, decoration: _dec('What does it do?')),
          const SizedBox(height: 14),
          Text('Category', style: inter(13, w: FontWeight.w500, c: C.muted)),
          const SizedBox(height: 8),
          Wrap(spacing: 8, runSpacing: 8, children: [
            for (final c in kStoreCats)
              Pressable(
                haptic: false,
                onTap: _busy ? null : () => setState(() => _cat = c),
                child: Container(
                  padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 9),
                  decoration: BoxDecoration(
                    color: _cat == c ? C.tealDeep : Colors.transparent,
                    borderRadius: BorderRadius.circular(10),
                    border: Border.all(color: _cat == c ? Colors.transparent : const Color(0x33FFFFFF), width: .8),
                  ),
                  child: Row(mainAxisSize: MainAxisSize.min, children: [
                    Icon(_cat == c ? Icons.check_rounded : _catIcon(c), size: 16, color: _cat == c ? C.teal : C.muted),
                    const SizedBox(width: 6),
                    Text(c, verbatim: true, style: inter(14, w: FontWeight.w500, c: _cat == c ? C.teal : C.text)),
                  ]),
                ),
              ),
          ]),
          const SizedBox(height: 16),
          Text('Screenshots (at least 1, required)', style: inter(13, w: FontWeight.w500, c: C.muted)),
          const SizedBox(height: 8),
          SizedBox(
            height: 118,
            child: ListView(scrollDirection: Axis.horizontal, children: [
              if (_shots.isEmpty && widget.updating != null)
                for (final u in widget.updating!.shotUrls) Padding(padding: const EdgeInsets.only(right: 8), child: _shotTile(url: u)),
              for (var i = 0; i < _shots.length; i++) Padding(padding: const EdgeInsets.only(right: 8), child: _shotTile(file: _shots[i], onRemove: () => setState(() => _shots.removeAt(i)))),
              if (_shots.length < kStoreMaxShots)
                Pressable(
                  onTap: _busy ? null : _pickShots,
                  child: Container(
                    width: 66,
                    height: 118,
                    decoration: BoxDecoration(color: C.field, borderRadius: BorderRadius.circular(11), border: Border.all(color: const Color(0x33FFFFFF), width: 1)),
                    child: const Icon(Icons.add_photo_alternate_outlined, color: C.muted, size: 26),
                  ),
                ),
            ]),
          ),
          const SizedBox(height: 4),
          Text(
              widget.updating != null && widget.updating!.shots.isNotEmpty && _shots.isEmpty
                  ? 'Pick new pictures to replace the current ones'
                  : 'Tap + to add a picture, up to $kStoreMaxShots. They are shrunk to small files, so they load fast.',
              verbatim: true,
              style: inter(12, c: C.muted)),
          if (_error != null) ...[
            const SizedBox(height: 14),
            Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
              const Icon(Icons.error_outline_rounded, size: 18, color: C.red),
              const SizedBox(width: 8),
              Expanded(child: Text(_error!, verbatim: true, style: inter(14, c: C.red))),
            ]),
          ],
          const SizedBox(height: 18),
          if (_busy)
            Column(children: [
              ClipRRect(borderRadius: BorderRadius.circular(4), child: LinearProgressIndicator(value: _p, color: C.teal, backgroundColor: C.field, minHeight: 6)),
              const SizedBox(height: 8),
              Text(_p == null ? 'Uploading…' : 'Uploading ${(_p! * 100).round()}%', verbatim: true, style: inter(13, c: C.muted)),
            ])
          else
            Pressable(
              onTap: _send,
              child: Container(
                width: double.infinity,
                height: 50,
                alignment: Alignment.center,
                decoration: BoxDecoration(color: bad ? C.tealDeep : C.tealBtn, borderRadius: BorderRadius.circular(25)),
                child: Text(widget.updating != null ? 'Update app' : 'Send for review', style: inter(16, w: FontWeight.w600, c: bad ? C.muted : C.onTeal)),
              ),
            ),
        ]);
    if (emb) {
      return Padding(
        padding: const EdgeInsets.fromLTRB(16, 12, 16, 6),
        child: Container(
          padding: const EdgeInsets.fromLTRB(16, 16, 16, 16),
          decoration: BoxDecoration(color: C.card, borderRadius: BorderRadius.circular(22), border: Border.all(color: _hair, width: .7)),
          child: column,
        ),
      );
    }
    return Padding(
      padding: EdgeInsets.fromLTRB(20, 12, 20, 20 + MediaQuery.of(context).viewInsets.bottom),
      child: SingleChildScrollView(child: column),
    );
  }
}
