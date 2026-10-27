import 'dart:async';
import 'dart:convert';
import 'package:flutter/material.dart' hide Text;
import 'package:http/http.dart' as http;
import '../anim.dart';
import '../api.dart';
import '../chat_cloud.dart';
import '../config.dart';
import '../theme.dart';
import '../widgets.dart';
import 'info_pages.dart';
import '../tr.dart';

enum _St { sending, delivered, failed }

class _Item {
  final String kind; // 'stamp' | 'admin' | 'me'
  final String text;
  _St? st;
  String? fail;
  bool tgLink = false;
  _Item(this.kind, this.text);
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

class _MessagePageState extends State<MessagePage> {
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
  String _err = '';

  @override
  void initState() {
    super.initState();
    _items.add(_Item('stamp', _stamp()));
    _welcome = _Item('admin', 'Hi! Write your message here and the admin will answer in this chat.');
    _items.add(_welcome!);
    _msg.addListener(() => setState(() {}));
    // One quiet check shortly after start so an answer sent while the app was closed is waiting.
    if (!kChatCloudOn) Future.delayed(const Duration(seconds: 2), _checkReplies);
    _syncPoll();
  }

  @override
  void didUpdateWidget(MessagePage old) {
    super.didUpdateWidget(old);
    if (old.open != widget.open) {
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
          final it = _Item(r.admin ? 'admin' : 'me', r.body);
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
        if (r.admin) await _showAdmin(r.body);
      }
    } catch (_) {
      // Offline: the next check tries again.
    } finally {
      _syncing = false;
    }
  }

  Future<void> _showAdmin(String text) async {
    if (!mounted) return;
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
    _poll?.cancel();
    _cloud?.dispose();
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
        final c = _contact.text.trim();
        // Typing dots first, then the confirmation, like a real chat.
        Future.delayed(const Duration(milliseconds: 380), () {
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
        return Text('Sending\u2026', style: inter(12, c: C.muted));
      case _St.delivered:
        return Text('Delivered', style: inter(12, w: FontWeight.w500, c: C.muted));
      case _St.failed:
        return Column(crossAxisAlignment: CrossAxisAlignment.end, children: [
          Row(mainAxisSize: MainAxisSize.min, children: [
            Text('Not Delivered', style: inter(12, w: FontWeight.w600, c: C.red)),
            Text(' \u00B7 ', style: inter(12, c: C.muted)),
            Pressable(
              onTap: _busy ? null : () => _deliver(m),
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

  Widget _bubbleBox({required bool me, required bool tail, required Widget child}) {
    const r = Radius.circular(19);
    const t = Radius.circular(5);
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 9),
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
    final maxW = MediaQuery.of(context).size.width * .64;
    final showStatus = me && m.st != null && (m.st != _St.delivered || identical(m, _lastMine));
    final failed = me && m.st == _St.failed;

    final bubble = ConstrainedBox(
      constraints: BoxConstraints(maxWidth: maxW),
      child: _bubbleBox(
        me: me,
        tail: !grouped,
        child: Text(m.text, style: inter(17, c: me ? C.onTeal : C.text, h: 1.25)),
      ),
    );

    Widget row;
    if (me) {
      row = Row(mainAxisAlignment: MainAxisAlignment.end, crossAxisAlignment: CrossAxisAlignment.center, children: [
        if (failed)
          Pressable(
            onTap: _busy ? null : () => _deliver(m),
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
          if (showStatus) Padding(padding: const EdgeInsets.only(top: 4, right: 4), child: _status(m)),
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
          padding: const EdgeInsets.fromLTRB(16, 8, 12, 10),
          child: SizedBox(
            height: 58,
            child: Stack(alignment: Alignment.center, children: [
              Column(mainAxisSize: MainAxisSize.min, children: [
                _avatar(34),
                const SizedBox(height: 3),
                Row(mainAxisSize: MainAxisSize.min, children: [
                  Text('Admin', style: inter(13, w: FontWeight.w600, h: 1.1)),
                  const SizedBox(width: 3),
                  const Icon(Icons.chevron_right_rounded, size: 14, color: C.muted),
                ]),
              ]),
              if (widget.onClose != null)
                Align(
                  alignment: Alignment.topRight,
                  child: Pressable(
                    onTap: widget.onClose,
                    scale: .9,
                    child: Container(
                      width: 32, height: 32,
                      decoration: const BoxDecoration(color: Color(0xFF2C2C2E), shape: BoxShape.circle),
                      child: const Icon(Icons.close_rounded, size: 19, color: C.muted),
                    ),
                  ),
                ),
              Align(
                alignment: Alignment.topLeft,
                child: Padding(
                  padding: const EdgeInsets.only(top: 8),
                  child: Row(mainAxisSize: MainAxisSize.min, children: [
                    const Icon(Icons.send_rounded, size: 12, color: Color(0xFF2AA3E0)),
                    const SizedBox(width: 4),
                    Text('Via Telegram', style: inter(12, c: C.muted)),
                  ]),
                ),
              ),
            ]),
          ),
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
      padding: const EdgeInsets.fromLTRB(12, 6, 12, 12),
      child: Column(mainAxisSize: MainAxisSize.min, crossAxisAlignment: CrossAxisAlignment.end, children: [
        if (len > _max - 200)
          Padding(padding: const EdgeInsets.only(right: 10, bottom: 4), child: Text('$len / $_max', style: inter(12, c: C.muted))),
        Container(
          constraints: const BoxConstraints(minHeight: 40),
          padding: const EdgeInsets.fromLTRB(16, 0, 5, 0),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(21),
            border: Border.all(color: const Color(0xFF3A3A3C), width: 1),
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
                  hintStyle: inter(17, c: const Color(0xFF5C5C61)),
                  contentPadding: const EdgeInsets.symmetric(vertical: 9),
                ),
              ),
            ),
            Padding(
              padding: const EdgeInsets.only(bottom: 5, left: 6),
              child: AnimatedScale(
                scale: canSend ? 1 : 0,
                duration: const Duration(milliseconds: 220),
                curve: iosCurve,
                child: Pressable(
                  onTap: canSend ? _submit : null,
                  scale: .88,
                  child: Container(
                    width: 30, height: 30,
                    decoration: const BoxDecoration(color: C.teal, shape: BoxShape.circle),
                    child: const Icon(Icons.arrow_upward_rounded, size: 20, color: C.onTeal),
                  ),
                ),
              ),
            ),
          ]),
        ),
      ]),
    );
  }

  @override
  Widget build(BuildContext context) {
    final len = _msg.text.length;
    final canSend = !_busy && _msg.text.trim().isNotEmpty;
    return Column(children: [
      _header(),
      _replyRow(),
      Expanded(
        child: ListView(
          controller: _scroll,
          physics: const BouncingScrollPhysics(),
          keyboardDismissBehavior: ScrollViewKeyboardDismissBehavior.onDrag,
          padding: const EdgeInsets.fromLTRB(14, 8, 14, 8),
          children: [
            for (var i = 0; i < _items.length; i++)
              if (_items[i].kind == 'stamp')
                Padding(
                  padding: const EdgeInsets.symmetric(vertical: 10),
                  child: Center(
                    child: Text.rich(TextSpan(children: [
                      TextSpan(text: 'Today ', style: inter(12, w: FontWeight.w600, c: C.muted)),
                      TextSpan(text: _items[i].text, style: inter(12, c: C.muted)),
                    ])),
                  ),
                )
              else
                _bubble(i),
            if (_typing) _typingBubble(),
          ],
        ),
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
