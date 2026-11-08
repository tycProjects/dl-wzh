import 'dart:async';
import 'dart:convert';
import 'dart:io';
import 'dart:typed_data';
import 'package:flutter/services.dart';
import 'package:flutter/material.dart' hide Text;
import 'package:http/http.dart' as http;
import '../anim.dart';
import '../api.dart';
import '../chat_cloud.dart';
import '../chat_media.dart';
import '../config.dart';
import '../notify.dart';
import '../theme.dart';
import '../widgets.dart';
import 'info_pages.dart';
import 'gallery_picker.dart';
import 'video_viewer.dart';
import '../tr.dart';

enum _St { sending, delivered, failed }

class _Item {
  final String kind; // 'stamp' | 'admin' | 'me'
  final String text;
  _St? st;
  String? fail;
  bool tgLink = false;
  // Photo / video messages
  String? mediaKind; // 'photo' | 'video'
  Uint8List? thumb;
  PreparedMedia? media; // kept so Retry doesn't shrink the file again
  String? note; // status line while working ("Shrinking video...")
  final DateTime at;
  _Item(this.kind, this.text, {DateTime? at}) : at = at ?? DateTime.now();
}

/// Message tab: sends a note to the admin through the server, which forwards it to Telegram.
class MessagePage extends StatefulWidget {
  final VoidCallback? onClose;
  /// True while the sheet is on screen: replies are only polled for then.
  final bool open;
  const MessagePage({super.key, this.onClose, this.open = false});
  @override
  State<MessagePage> createState() => _MessagePageState();
}

class _MessagePageState extends State<MessagePage> with WidgetsBindingObserver {
  // A little under the server limit: it adds a short reference line to each message.
  static const _max = 980;
  final _contact = TextEditingController();
  final _msg = TextEditingController();
  final _scroll = ScrollController();
  final List<_Item> _items = [];
  bool _busy = false;
  bool _typing = false;
  Timer? _poll;
  ChatCloud? _cloud;
  int _cloudSince = 0;
  bool _cloudLoaded = false;
  bool _syncing = false;
  _Item? _welcome;
  bool _checking = false;
  bool _receiving = false;
  bool _contactOpen = false;
  bool _stick = true; // the list is at the bottom, so it stays there when the keyboard moves
  int _newBelow = 0; // admin replies that arrived while the list was scrolled up
  String _toast = '';
  Timer? _toastT;
  String _err = '';

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _scroll.addListener(_onScroll);
    _items.add(_Item('stamp', _stamp()));
    _welcome = _Item('admin', 'Hi! Write your message here and the admin will answer in this chat.');
    _items.add(_welcome!);
    _msg.addListener(() => setState(() {}));
    // One quiet check shortly after start so an answer sent while the app was closed is waiting.
    if (!kChatCloudOn) Future.delayed(const Duration(seconds: 2), _checkReplies);
    ChatNotifier.chatOpen = widget.open;
    if (widget.open) ChatNotifier.unread.value = 0;
    _syncPoll();
  }

  void _onScroll() {
    if (!_scroll.hasClients) return;
    final near = _scroll.position.maxScrollExtent - _scroll.offset < 90;
    if (near == _stick) return;
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!mounted) return;
      setState(() {
        _stick = near;
        if (near) _newBelow = 0;
      });
    });
  }

  void _flash(String t) {
    setState(() => _toast = t);
    _toastT?.cancel();
    _toastT = Timer(const Duration(milliseconds: 1400), () {
      if (mounted) setState(() => _toast = '');
    });
  }

  // The keyboard opening or closing resizes the list: keep the newest message in view.
  @override
  void didChangeMetrics() {
    if (!widget.open || !_stick) return;
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted && _scroll.hasClients) _scroll.jumpTo(_scroll.position.maxScrollExtent);
    });
  }

  @override
  void didUpdateWidget(MessagePage old) {
    super.didUpdateWidget(old);
    if (old.open != widget.open) {
      ChatNotifier.chatOpen = widget.open;
    if (widget.open) ChatNotifier.unread.value = 0;
      _syncPoll();
      if (widget.open) _checkReplies();
    }
  }

  // ---- replies from the admin (sent from Telegram, picked up through the server) ----
  // Replies are read from the first server: that is the one the Telegram webhook points at.
  String get _replyBase => kApiBases.first;

  void _syncPoll() {
    _poll?.cancel();
    _poll = null;
    if (kChatCloudOn) {
      _cloudToggle();
      return;
    }
    if (widget.open) _poll = Timer.periodic(const Duration(seconds: 6), (_) => _checkReplies());
  }

  // ---- Supabase chat: history, plus replies pushed the moment the admin answers ----
  void _cloudToggle() {
    final c = _cloud;
    if (!widget.open) {
      c?.stop();
      return;
    }
    final id = Api.i.clientId;
    if (id.isEmpty) return;
    final cloud = _cloud ??= ChatCloud(ChatCloud.keyFor(id));
    _cloudSync();
    cloud.listen(_cloudSync);
    // Safety net in case the live connection drops.
    _poll = Timer.periodic(const Duration(seconds: 30), (_) => _cloudSync());
  }

  Future<void> _cloudSync() async {
    final cloud = _cloud;
    if (cloud == null || _syncing || !mounted) return;
    _syncing = true;
    try {
      final rows = await cloud.fetch(_cloudSince);
      if (!mounted) return;
      if (!_cloudLoaded) {
        // First load: bring back the earlier conversation.
        _cloudLoaded = true;
        final mine = [for (final i in _items) if (i.kind == 'me') i.text];
        final past = <_Item>[];
        for (final r in rows) {
          if (r.id > _cloudSince) _cloudSince = r.id;
          if (!r.admin) {
            final k = mine.indexOf(r.body);
            if (k >= 0) {
              mine.removeAt(k);
              continue;
            }
          }
          final it = _Item(r.admin ? 'admin' : 'me', r.body, at: r.at);
          if (!r.admin) it.st = _St.delivered;
          past.add(it);
        }
        if (past.isEmpty) return;
        setState(() {
          // An earlier chat replaces the first-time welcome.
          _items.removeWhere((i) => identical(i, _welcome));
          _items.insertAll(1, past);
        });
        _toBottom();
        return;
      }
      for (final r in rows) {
        if (!mounted) return;
        if (r.id > _cloudSince) _cloudSince = r.id;
        if (r.admin) await _showAdmin(r.body, r.at);
      }
    } catch (_) {
      // Offline: the next check tries again.
    } finally {
      _syncing = false;
    }
  }

  Future<void> _showAdmin(String text, [DateTime? at]) async {
    if (!mounted) return;
    setState(() => _typing = true);
    if (_stick) _toBottom();
    await Future.delayed(const Duration(milliseconds: 650));
    if (!mounted) return;
    setState(() {
      _typing = false;
      _items.add(_Item('admin', text, at: at));
      if (!_stick) _newBelow++;
    });
    HapticFeedback.lightImpact();
    if (_stick) _toBottom();
  }

  Future<void> _checkReplies() async {
    if (kChatCloudOn) return _cloudSync();
    if (_checking || _receiving || !mounted) return;
    _checking = true;
    try {
      final since = int.tryParse(Api.i.prefs.getString('dwReplyLast') ?? '') ?? 0;
      final res = await http
          .get(
            Uri.parse('$_replyBase/api/contact-replies').replace(queryParameters: {'clientId': Api.i.clientId, 'since': '$since'}),
            headers: Api.i.headers({'X-Client-Id': Api.i.clientId}),
          )
          .timeout(const Duration(seconds: 12));
      if (res.statusCode != 200 || !mounted) return;
      final list = ((jsonDecode(res.body) as Map)['replies'] as List?) ?? const [];
      if (list.isEmpty) return;
      _receiving = true;
      for (final r in list) {
        if (!mounted) return;
        final id = ((r as Map)['id'] as num).toInt();
        final text = '${r['text'] ?? ''}'.trim();
        await Api.i.prefs.setString('dwReplyLast', '$id');
        if (text.isEmpty) continue;
        setState(() => _typing = true);
        _toBottom();
        await Future.delayed(const Duration(milliseconds: 650));
        if (!mounted) return;
        setState(() {
          _typing = false;
          _items.add(_Item('admin', text));
        });
        _toBottom();
      }
    } catch (_) {
      // Offline or the server is waking up: the next check tries again.
    } finally {
      _checking = false;
      _receiving = false;
      if (mounted && _typing) setState(() => _typing = false);
    }
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    _toastT?.cancel();
    ChatNotifier.chatOpen = false;
    _poll?.cancel();
    _cloud?.dispose();
    ChatMedia.clearCache(); // shrunk copies are kept while the chat is open so they can be played
    _contact.dispose();
    _msg.dispose();
    _scroll.dispose();
    super.dispose();
  }

  String _stamp() {
    final d = DateTime.now();
    final h = d.hour % 12 == 0 ? 12 : d.hour % 12;
    return '$h:${d.minute.toString().padLeft(2, '0')} ${d.hour >= 12 ? 'PM' : 'AM'}';
  }

  static const _mon = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];

  String _time(DateTime d) {
    final h = d.hour % 12 == 0 ? 12 : d.hour % 12;
    return '$h:${d.minute.toString().padLeft(2, '0')} ${d.hour >= 12 ? 'PM' : 'AM'}';
  }

  String _day(DateTime d) {
    final n = DateTime.now();
    final diff = DateTime(n.year, n.month, n.day).difference(DateTime(d.year, d.month, d.day)).inDays;
    if (diff == 0) return 'Today';
    if (diff == 1) return 'Yesterday';
    final yr = d.year != n.year ? ', ${d.year}' : '';
    return '${_mon[d.month - 1]} ${d.day}$yr';
  }

  /// True when message [i] is the first one of a new day (a date label goes above it).
  bool _dayBreak(int i) {
    for (var k = i - 1; k >= 0; k--) {
      if (_items[k].kind == 'stamp') continue;
      final a = _items[k].at, b = _items[i].at;
      return a.year != b.year || a.month != b.month || a.day != b.day;
    }
    return true;
  }

  void _toBottom() {
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (_scroll.hasClients) {
        _scroll.animateTo(_scroll.position.maxScrollExtent, duration: const Duration(milliseconds: 220), curve: iosCurve);
      }
    });
  }

  void _submit() {
    if (_busy) return;
    final text = _msg.text.trim();
    if (text.isEmpty) return;
    final last = int.tryParse(Api.i.prefs.getString('dwContactLast') ?? '') ?? 0;
    if (DateTime.now().millisecondsSinceEpoch - last < 30000) {
      setState(() => _err = 'Wait a few seconds before sending another.');
      return;
    }
    final m = _Item('me', text);
    setState(() {
      _err = '';
      _items.add(m);
      _msg.clear();
    });
    _toBottom();
    _deliver(m);
  }

  Future<void> _deliver(_Item m) async {
    setState(() {
      _busy = true;
      m.st = _St.sending;
      m.fail = null;
      m.tgLink = false;
    });
    void fail(String why, {bool link = true}) {
      if (!mounted) return;
      setState(() {
        m.st = _St.failed;
        m.fail = why;
        m.tgLink = link;
      });
    }

    try {
      final res = await http
          .post(
            Uri.parse('$kApiBase/api/contact'),
            headers: Api.i.headers({'Content-Type': 'application/json', 'X-Client-Id': Api.i.clientId}),
            body: jsonEncode({'message': m.text, 'contact': _contact.text.trim(), 'clientId': Api.i.clientId, 'website': ''}),
          )
          // The free server can take a while to wake up.
          .timeout(const Duration(seconds: 40));
      if (res.statusCode == 429) {
        fail('You\u2019ve sent a lot already. Try again later.', link: false);
      } else if (res.statusCode < 200 || res.statusCode >= 300) {
        var code = '';
        try {
          code = '${(jsonDecode(res.body) as Map)['error'] ?? ''}';
        } catch (_) {}
        final why = res.statusCode == 404
            ? 'Messaging isn\u2019t live yet. Try again later.'
            : (res.statusCode == 503 || code == 'not_configured')
                ? 'Admin messaging isn\u2019t set up yet.'
                : res.statusCode == 502
                    ? 'Couldn\u2019t reach Telegram. Try again soon.'
                    : res.statusCode == 413
                        ? 'That message is too long. Trim it a bit.'
                        : 'Couldn\u2019t send that. Try again in a bit.';
        fail(why);
      } else {
        await Api.i.prefs.setString('dwContactLast', '${DateTime.now().millisecondsSinceEpoch}');
        if (!mounted) return;
        setState(() => m.st = _St.delivered);
        ChatNotifier.i.askPermission();
        final c = _contact.text.trim();
        // The "Sent to the admin" note is only shown the first time; after that the
        // small "Delivered" label under your message is enough.
        final noteShown = Api.i.prefs.getString('dwSentNote') == '1';
        if (!noteShown) await Api.i.prefs.setString('dwSentNote', '1');
        // Typing dots first, then the confirmation, like a real chat.
        if (!noteShown) Future.delayed(const Duration(milliseconds: 380), () {
          if (!mounted) return;
          setState(() => _typing = true);
          _toBottom();
          Future.delayed(const Duration(milliseconds: 1100), () {
            if (!mounted) return;
            setState(() {
              _typing = false;
              _items.add(_Item(
                  'admin',
                  c.isNotEmpty
                      ? '\u2713 Sent to the admin. Their answer will show up here, or they\u2019ll write to $c.'
                      : '\u2713 Sent to the admin. Their answer will show up here.'));
            });
            _toBottom();
          });
        });
      }
    } catch (_) {
      fail('Can\u2019t reach the server. Check your connection.');
    } finally {
      if (mounted) setState(() => _busy = false);
      _toBottom();
    }
  }

  // ---------------------------------------------------------------- photos and videos

  void _retry(_Item m) => m.media != null ? _uploadMedia(m) : _deliver(m);

  Future<void> _attach() async {
    if (_busy) return;
    FocusManager.instance.primaryFocus?.unfocus();
    try {
      final p = await pickChatMedia(context);
      if (p == null || !mounted) return;
      await _sendMedia(p.video ? 'video' : 'photo', p.path);
    } catch (_) {
      if (mounted) setState(() => _err = 'Couldn\u2019t open that file.');
    }
  }

  Future<void> _sendMedia(String kind, String path) async {
    final m = _Item('me', kind == 'video' ? '\u{1F3AC} Video' : '\u{1F4F7} Photo');
    m.mediaKind = kind;
    m.st = _St.sending;
    m.note = kind == 'video' ? 'Shrinking video\u2026' : 'Preparing photo\u2026';
    setState(() {
      _err = '';
      _busy = true;
      _items.add(m);
    });
    _toBottom();
    try {
      final prep = kind == 'video' ? await ChatMedia.prepareVideo(path) : await ChatMedia.preparePhoto(path);
      if (!mounted) return;
      setState(() {
        m.media = prep;
        m.thumb = prep.thumb;
      });
      _toBottom();
    } on MediaError catch (e) {
      _mediaFail(m, e.message, link: false);
      if (mounted) setState(() => _busy = false);
      return;
    }
    await _uploadMedia(m);
  }

  void _mediaFail(_Item m, String why, {bool link = true}) {
    if (!mounted) return;
    setState(() {
      m.st = _St.failed;
      m.fail = why;
      m.tgLink = link;
    });
  }

  Future<void> _uploadMedia(_Item m) async {
    final prep = m.media;
    if (prep == null) return;
    setState(() {
      _busy = true;
      m.st = _St.sending;
      m.note = 'Sending\u2026';
      m.fail = null;
      m.tgLink = false;
    });
    try {
      await ChatMedia.send(prep, Api.i.clientId);
      if (!mounted) return;
      setState(() => m.st = _St.delivered);
      ChatNotifier.i.askPermission();
    } on MediaError catch (e) {
      _mediaFail(m, e.message);
    } catch (_) {
      _mediaFail(m, 'Couldn\u2019t send that. Try again in a bit.');
    } finally {
      if (mounted) setState(() => _busy = false);
      _toBottom();
    }
  }

  /// Opens the photo or video inside the app (full screen, with playback for videos).
  Future<void> _openMedia(_Item m) async {
    final prep = m.media;
    if (prep == null || m.st == _St.sending) return;
    try {
      FocusManager.instance.primaryFocus?.unfocus();
      final nav = Navigator.of(context, rootNavigator: true);
      if (prep.kind == 'video') {
        final path = prep.path;
        if (path == null || !await File(path).exists()) {
          _flash('That video is no longer on this phone');
          return;
        }
        await nav.push(PageRouteBuilder<void>(
          opaque: true,
          transitionDuration: const Duration(milliseconds: 220),
          reverseTransitionDuration: const Duration(milliseconds: 180),
          pageBuilder: (_, __, ___) => ChatVideoPage(
            path: path,
            title: 'You',
            subtitle: '${_day(m.at)} at ${_time(m.at)}',
            caption: m.text,
          ),
          transitionsBuilder: (_, anim, __, child) => FadeTransition(opacity: anim, child: child),
        ));
      } else {
        final bytes = prep.bytes;
        if (bytes == null) return;
        await nav.push(PageRouteBuilder<void>(
          opaque: true,
          transitionDuration: const Duration(milliseconds: 220),
          reverseTransitionDuration: const Duration(milliseconds: 180),
          pageBuilder: (_, __, ___) => ChatPhotoPage(
            bytes: bytes,
            title: 'You',
            subtitle: '${_day(m.at)} at ${_time(m.at)}',
            caption: m.text,
          ),
          transitionsBuilder: (_, anim, __, child) => FadeTransition(opacity: anim, child: child),
        ));
      }
    } catch (_) {
      _flash('Couldn\u2019t open this');
    }
  }

  String _dur(int ms) {
    final s = (ms / 1000).round();
    return '${s ~/ 60}:${(s % 60).toString().padLeft(2, '0')}';
  }

  /// Time and delivery ticks that sit in the bottom-right corner of a bubble.
  Widget _meta(_Item m, bool me, {Color? color}) {
    final c = color ?? (me ? const Color(0x99252A27) : C.muted);
    return Row(mainAxisSize: MainAxisSize.min, children: [
      Text(_time(m.at), style: inter(11, c: c, h: 1.0)),
      if (me && m.st == _St.delivered) ...[
        const SizedBox(width: 3),
        Icon(Icons.done_all_rounded, size: 15, color: c),
      ] else if (me && m.st == _St.sending) ...[
        const SizedBox(width: 3),
        Icon(Icons.access_time_rounded, size: 12, color: c),
      ],
    ]);
  }

  /// Message text with the time tucked into the last line, like Telegram.
  Widget _textBody(_Item m, bool me) {
    return Stack(children: [
      RichText(
        text: TextSpan(
          style: inter(17, c: me ? C.onTeal : C.text, h: 1.25),
          children: [
            TextSpan(text: m.text),
            WidgetSpan(child: SizedBox(width: me ? 74 : 58, height: 1), alignment: PlaceholderAlignment.middle),
          ],
        ),
      ),
      Positioned(right: 0, bottom: 1, child: _meta(m, me)),
    ]);
  }

  /// Photo / video bubble: picture on top, caption and time underneath.
  Widget _mediaBubble(_Item m, double maxW, bool me) {
    final w = maxW - 8;
    return SizedBox(
      width: w,
      child: Column(mainAxisSize: MainAxisSize.min, crossAxisAlignment: CrossAxisAlignment.start, children: [
        _mediaThumb(m, w),
        Padding(
          padding: const EdgeInsets.fromLTRB(8, 6, 6, 3),
          child: Stack(children: [
            RichText(
              text: TextSpan(
                style: inter(15, c: me ? C.onTeal : C.text, h: 1.25),
                children: [
                  TextSpan(text: m.text),
                  WidgetSpan(child: SizedBox(width: me ? 74 : 58, height: 1), alignment: PlaceholderAlignment.middle),
                ],
              ),
            ),
            Positioned(right: 0, bottom: 1, child: _meta(m, me)),
          ]),
        ),
      ]),
    );
  }

  Widget _mediaThumb(_Item m, double w) {
    final ms = m.media?.durationMs;
    return ClipRRect(
      borderRadius: BorderRadius.circular(15),
      child: SizedBox(
        width: w,
        child: Stack(alignment: Alignment.center, children: [
          Image.memory(m.thumb!, width: w, fit: BoxFit.cover, gaplessPlayback: true, errorBuilder: (_, __, ___) => SizedBox(width: w, height: 160)),
          if (m.mediaKind == 'video')
            Container(
              width: 50, height: 50,
              decoration: const BoxDecoration(color: Color(0x99000000), shape: BoxShape.circle),
              child: const Icon(Icons.play_arrow_rounded, size: 34, color: Colors.white),
            ),
          if (m.mediaKind == 'video' && ms != null && ms > 0)
            Positioned(
              left: 8, top: 8,
              child: Container(
                padding: const EdgeInsets.symmetric(horizontal: 7, vertical: 3),
                decoration: BoxDecoration(color: const Color(0x99000000), borderRadius: BorderRadius.circular(10)),
                child: Text(_dur(ms), style: inter(12, w: FontWeight.w600, c: Colors.white, h: 1.0)),
              ),
            ),
          if (m.st == _St.sending)
            Positioned.fill(
              child: Container(
                color: const Color(0x66000000),
                alignment: Alignment.center,
                child: IosSpinner(size: 26, color: Colors.white),
              ),
            ),
        ]),
      ),
    );
  }

  // ---------------------------------------------------------------- UI

  bool _groupedWithNext(int i) {
    if (i + 1 >= _items.length) return _typing && _items[i].kind == 'admin';
    final a = _items[i], b = _items[i + 1];
    return b.kind != 'stamp' && a.kind == b.kind;
  }

  _Item? get _lastMine {
    for (var i = _items.length - 1; i >= 0; i--) {
      if (_items[i].kind == 'me') return _items[i];
    }
    return null;
  }

  Widget _status(_Item m) {
    switch (m.st) {
      case _St.sending:
        return Text(m.note ?? 'Sending\u2026', style: inter(12, c: C.muted));
      case _St.delivered:
        return Text('Delivered', style: inter(12, w: FontWeight.w500, c: C.muted));
      case _St.failed:
        return Column(crossAxisAlignment: CrossAxisAlignment.end, children: [
          Row(mainAxisSize: MainAxisSize.min, children: [
            Text('Not Delivered', style: inter(12, w: FontWeight.w600, c: C.red)),
            Text(' \u00B7 ', style: inter(12, c: C.muted)),
            Pressable(
              onTap: _busy ? null : () => _retry(m),
              child: Text('Retry', style: inter(12, w: FontWeight.w600, c: C.teal)),
            ),
          ]),
          if (m.fail != null) ...[
            const SizedBox(height: 2),
            Text(m.fail!, textAlign: TextAlign.right, style: inter(12, c: C.muted)),
          ],
          if (m.tgLink)
            Pressable(
              onTap: () => openTelegram(context),
              child: Padding(
                padding: const EdgeInsets.only(top: 3),
                child: Text('Message the admin on Telegram instead', textAlign: TextAlign.right, style: inter(12, w: FontWeight.w600, c: C.teal)),
              ),
            ),
        ]);
      case null:
        return const SizedBox.shrink();
    }
  }

  Widget _avatar(double size) => SizedBox(
        width: size,
        height: size,
        child: FittedBox(child: const BotFab()),
      );

  Widget _bubbleBox({required bool me, required bool tail, required Widget child, EdgeInsets? pad}) {
    const r = Radius.circular(19);
    const t = Radius.circular(5);
    return Container(
      padding: pad ?? const EdgeInsets.symmetric(horizontal: 14, vertical: 9),
      decoration: BoxDecoration(
        color: me ? C.teal : const Color(0xFF2A2A2D),
        borderRadius: BorderRadius.only(
          topLeft: r,
          topRight: r,
          bottomLeft: (!me && tail) ? t : r,
          bottomRight: (me && tail) ? t : r,
        ),
      ),
      child: child,
    );
  }

  Widget _bubble(int i) {
    final m = _items[i];
    final me = m.kind == 'me';
    final grouped = _groupedWithNext(i);
    final maxW = MediaQuery.of(context).size.width * .74;
    final showStatus = me && (m.st == _St.sending || m.st == _St.failed);
    final failed = me && m.st == _St.failed;

    final bubble = GestureDetector(
      onTap: m.media != null ? () => _openMedia(m) : null,
      onLongPress: () {
        Clipboard.setData(ClipboardData(text: m.text));
        HapticFeedback.mediumImpact();
        _flash('Copied');
      },
      child: ConstrainedBox(
        constraints: BoxConstraints(maxWidth: maxW),
        child: _bubbleBox(
          me: me,
          tail: !grouped,
          pad: m.thumb != null ? const EdgeInsets.all(4) : const EdgeInsets.fromLTRB(13, 7, 11, 6),
          child: m.thumb != null ? _mediaBubble(m, maxW, me) : _textBody(m, me),
        ),
      ),
    );

    Widget row;
    if (me) {
      row = Row(mainAxisAlignment: MainAxisAlignment.end, crossAxisAlignment: CrossAxisAlignment.center, children: [
        if (failed)
          Pressable(
            onTap: _busy ? null : () => _retry(m),
            scale: .85,
            child: Container(
              width: 22, height: 22,
              margin: const EdgeInsets.only(right: 8),
              decoration: const BoxDecoration(color: C.red, shape: BoxShape.circle),
              child: const Icon(Icons.priority_high_rounded, size: 15, color: Colors.white),
            ),
          ),
        Flexible(child: bubble),
      ]);
    } else {
      row = Row(crossAxisAlignment: CrossAxisAlignment.end, children: [
        SizedBox(width: 28, child: grouped ? null : _avatar(28)),
        const SizedBox(width: 8),
        Flexible(child: bubble),
      ]);
    }

    return TweenAnimationBuilder<double>(
      key: ObjectKey(m),
      tween: Tween(begin: 0, end: 1),
      duration: const Duration(milliseconds: 320),
      curve: iosCurve,
      builder: (_, v, child) => Opacity(
        opacity: v.clamp(0.0, 1.0),
        child: Transform.scale(
          scale: .88 + .12 * v,
          alignment: me ? Alignment.bottomRight : Alignment.bottomLeft,
          child: child,
        ),
      ),
      child: Padding(
        padding: EdgeInsets.only(bottom: grouped ? 3 : 10),
        child: Column(crossAxisAlignment: me ? CrossAxisAlignment.end : CrossAxisAlignment.start, children: [
          row,
          if (showStatus)
            Padding(padding: const EdgeInsets.only(top: 4, right: 4), child: _status(m)),
        ]),
      ),
    );
  }

  Widget _typingBubble() => Padding(
        padding: const EdgeInsets.only(bottom: 10),
        child: Row(crossAxisAlignment: CrossAxisAlignment.end, children: [
          SizedBox(width: 28, child: _avatar(28)),
          const SizedBox(width: 8),
          _bubbleBox(me: false, tail: true, child: const _TypingDots()),
        ]),
      );

  Widget _header() {
    final has = _contact.text.trim().isNotEmpty;
    final on = _contactOpen || has;
    return GestureDetector(
      behavior: HitTestBehavior.opaque,
      onVerticalDragEnd: (d) {
        if ((d.primaryVelocity ?? 0) > 400) widget.onClose?.call();
      },
      child: Column(children: [
        const SizedBox(height: 8),
        Container(
          width: 36, height: 5,
          decoration: BoxDecoration(color: const Color(0xFF3A3A3C), borderRadius: BorderRadius.circular(3)),
        ),
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 10, 12, 10),
          child: Row(children: [
            _avatar(40),
            const SizedBox(width: 12),
            Expanded(
              child: Column(crossAxisAlignment: CrossAxisAlignment.start, mainAxisSize: MainAxisSize.min, children: [
                Text('Admin', style: inter(17, w: FontWeight.w600, h: 1.15)),
                const SizedBox(height: 2),
                Row(mainAxisSize: MainAxisSize.min, children: [
                  const Icon(Icons.send_rounded, size: 11, color: Color(0xFF2AA3E0)),
                  const SizedBox(width: 4),
                  Text('Replies via Telegram', style: inter(12, c: C.muted)),
                ]),
              ]),
            ),
            Pressable(
              onTap: () => setState(() => _contactOpen = !_contactOpen),
              scale: .9,
              child: Container(
                width: 34, height: 34,
                decoration: BoxDecoration(color: on ? C.tealDeep : const Color(0xFF2C2C2E), shape: BoxShape.circle),
                child: Icon(Icons.alternate_email_rounded, size: 18, color: on ? C.teal : C.muted),
              ),
            ),
            if (widget.onClose != null) ...[
              const SizedBox(width: 8),
              Pressable(
                onTap: widget.onClose,
                scale: .9,
                child: Container(
                  width: 34, height: 34,
                  decoration: const BoxDecoration(color: Color(0xFF2C2C2E), shape: BoxShape.circle),
                  child: const Icon(Icons.close_rounded, size: 19, color: C.muted),
                ),
              ),
            ],
          ]),
        ),
        Container(height: .5, color: const Color(0xFF2E2E30)),
      ]),
    );
  }

  /// iOS "To:" style row. Collapsed it is a single quiet line; tap to type a way to be reached.
  Widget _replyRow() {
    final has = _contact.text.trim().isNotEmpty;
    return Column(children: [
      Padding(
        padding: const EdgeInsets.symmetric(horizontal: 16),
        child: SizedBox(
          height: 44,
          child: Row(children: [
            Text('Reply to:', style: inter(16, c: C.muted)),
            const SizedBox(width: 10),
            Expanded(
              child: TextField(
                controller: _contact,
                maxLength: 120,
                keyboardType: TextInputType.emailAddress,
                textInputAction: TextInputAction.next,
                onChanged: (_) {
                  if (_err.isNotEmpty) setState(() => _err = '');
                  setState(() {});
                },
                style: inter(16, c: C.teal),
                cursorColor: C.teal,
                decoration: InputDecoration(
                  counterText: '',
                  border: InputBorder.none,
                  isCollapsed: true,
                  hintText: '@username or email (optional)',
                  hintStyle: inter(16, c: const Color(0xFF5C5C61)),
                ),
              ),
            ),
            if (has)
              Pressable(
                onTap: () => setState(() => _contact.clear()),
                scale: .85,
                child: const Padding(
                  padding: EdgeInsets.only(left: 8),
                  child: Icon(Icons.cancel_rounded, size: 18, color: Color(0xFF5C5C61)),
                ),
              ),
          ]),
        ),
      ),
      Container(height: .5, color: const Color(0xFF2E2E30)),
    ]);
  }

  Widget _composer(bool canSend, int len) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(8, 6, 8, 10),
      child: Column(mainAxisSize: MainAxisSize.min, crossAxisAlignment: CrossAxisAlignment.end, children: [
        if (len > _max - 200)
          Padding(padding: const EdgeInsets.only(right: 10, bottom: 4), child: Text('$len / $_max', style: inter(12, c: C.muted))),
        Row(crossAxisAlignment: CrossAxisAlignment.end, children: [
          Expanded(
            child: Container(
              constraints: const BoxConstraints(minHeight: 44),
              padding: const EdgeInsets.fromLTRB(16, 0, 4, 0),
              decoration: BoxDecoration(
                color: const Color(0xFF1F1F21),
                borderRadius: BorderRadius.circular(22),
              ),
              child: Row(crossAxisAlignment: CrossAxisAlignment.end, children: [
                Expanded(
                  child: TextField(
                    controller: _msg,
                    minLines: 1,
                    maxLines: 5,
                    maxLength: _max,
                    textCapitalization: TextCapitalization.sentences,
                    keyboardType: TextInputType.multiline,
                    style: inter(17),
                    cursorColor: C.teal,
                    onChanged: (_) {
                      if (_err.isNotEmpty) setState(() => _err = '');
                    },
                    decoration: InputDecoration(
                      counterText: '',
                      border: InputBorder.none,
                      hintText: 'Message',
                      hintStyle: inter(17, c: const Color(0xFF6E6E73)),
                      contentPadding: const EdgeInsets.symmetric(vertical: 11),
                    ),
                  ),
                ),
                Pressable(
                  onTap: _busy ? null : _attach,
                  scale: .88,
                  child: SizedBox(
                    width: 40, height: 44,
                    child: Transform.rotate(
                      angle: .6,
                      child: Icon(Icons.attach_file_rounded, size: 24, color: _busy ? const Color(0xFF5C5C61) : C.muted),
                    ),
                  ),
                ),
              ]),
            ),
          ),
          AnimatedSize(
            duration: const Duration(milliseconds: 220),
            curve: iosCurve,
            alignment: Alignment.centerLeft,
            child: canSend
                ? Padding(
                    padding: const EdgeInsets.only(left: 8),
                    child: Pressable(
                      onTap: _submit,
                      scale: .88,
                      child: Container(
                        width: 44, height: 44,
                        decoration: BoxDecoration(color: C.teal, shape: BoxShape.circle),
                        child: Icon(Icons.arrow_upward_rounded, size: 24, color: C.onTeal),
                      ),
                    ),
                  )
                : const SizedBox(width: 0, height: 44),
          ),
        ]),
      ]),
    );
  }

  @override
  Widget build(BuildContext context) {
    final len = _msg.text.length;
    final canSend = !_busy && _msg.text.trim().isNotEmpty;
    return Column(children: [
      _header(),
      AnimatedSize(
        duration: const Duration(milliseconds: 220),
        curve: iosCurve,
        alignment: Alignment.topCenter,
        child: _contactOpen ? _replyRow() : const SizedBox(width: double.infinity),
      ),
      Expanded(
        child: Stack(children: [
        ListView(
          controller: _scroll,
          physics: const IosScrollPhysics(),
          keyboardDismissBehavior: ScrollViewKeyboardDismissBehavior.onDrag,
          padding: const EdgeInsets.fromLTRB(14, 8, 14, 8),
          children: [
            for (var i = 0; i < _items.length; i++) ...[
              if (_items[i].kind != 'stamp' && _dayBreak(i))
                Padding(
                  padding: const EdgeInsets.symmetric(vertical: 10),
                  child: Center(child: Text(_day(_items[i].at), style: inter(12, w: FontWeight.w600, c: C.muted))),
                ),
              if (_items[i].kind != 'stamp') _bubble(i),
            ],
            if (_typing) _typingBubble(),
          ],
        ),
        Positioned(
          left: 0,
          right: 0,
          top: 8,
          child: IgnorePointer(
            child: Center(
              child: AnimatedOpacity(
                opacity: _toast.isEmpty ? 0 : 1,
                duration: const Duration(milliseconds: 180),
                child: Container(
                  padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 7),
                  decoration: BoxDecoration(color: const Color(0xFF2C2C2E), borderRadius: BorderRadius.circular(16)),
                  child: Text(_toast.isEmpty ? ' ' : _toast, style: inter(13, w: FontWeight.w600)),
                ),
              ),
            ),
          ),
        ),
        Positioned(
          right: 14,
          bottom: 10,
          child: IgnorePointer(
            ignoring: _stick,
            child: AnimatedScale(
              scale: _stick ? 0 : 1,
              duration: const Duration(milliseconds: 200),
              curve: iosCurve,
              child: Pressable(
                onTap: () {
                  setState(() {
                    _stick = true;
                    _newBelow = 0;
                  });
                  _toBottom();
                },
                scale: .9,
                child: Stack(clipBehavior: Clip.none, children: [
                  Container(
                    width: 38,
                    height: 38,
                    decoration: BoxDecoration(
                      color: const Color(0xFF2C2C2E),
                      shape: BoxShape.circle,
                      border: Border.all(color: const Color(0xFF3A3A3C)),
                    ),
                    child: const Icon(Icons.keyboard_arrow_down_rounded, size: 24, color: C.text),
                  ),
                  if (_newBelow > 0)
                    Positioned(
                      top: -6,
                      right: -4,
                      child: Container(
                        constraints: const BoxConstraints(minWidth: 19, minHeight: 19),
                        padding: const EdgeInsets.symmetric(horizontal: 5),
                        alignment: Alignment.center,
                        decoration: BoxDecoration(color: C.teal, borderRadius: BorderRadius.circular(10)),
                        child: Text('$_newBelow', style: inter(11, w: FontWeight.w800, c: C.onTeal)),
                      ),
                    ),
                ]),
              ),
            ),
          ),
        ),
        ]),
      ),
      if (_err.isNotEmpty)
        Padding(
          padding: const EdgeInsets.fromLTRB(20, 0, 20, 4),
          child: Align(alignment: Alignment.centerLeft, child: Text(_err, style: inter(13, c: C.red))),
        ),
      _composer(canSend, len),
    ]);
  }
}

/// Three dots that pulse one after another (the "typing" bubble).
class _TypingDots extends StatefulWidget {
  const _TypingDots();
  @override
  State<_TypingDots> createState() => _TypingDotsState();
}

class _TypingDotsState extends State<_TypingDots> with SingleTickerProviderStateMixin {
  late final AnimationController _c = AnimationController(vsync: this, duration: const Duration(milliseconds: 1100))..repeat();

  @override
  void dispose() {
    _c.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      height: 20,
      child: AnimatedBuilder(
        animation: _c,
        builder: (_, __) => Row(mainAxisSize: MainAxisSize.min, crossAxisAlignment: CrossAxisAlignment.center, children: [
          for (var i = 0; i < 3; i++) ...[
            if (i > 0) const SizedBox(width: 5),
            Builder(builder: (_) {
              final p = ((_c.value - i * .16) % 1.0 + 1.0) % 1.0;
              final k = p < .5 ? Curves.easeInOut.transform(p * 2) : Curves.easeInOut.transform((1 - p) * 2);
              return Container(
                width: 8, height: 8,
                decoration: BoxDecoration(color: Color.lerp(const Color(0xFF6A6A6E), const Color(0xFFC4C4C8), k), shape: BoxShape.circle),
              );
            }),
          ],
        ]),
      ),
    );
  }
}
