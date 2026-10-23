import 'dart:convert';
import 'package:flutter/material.dart' hide Text;
import 'package:http/http.dart' as http;
import '../anim.dart';
import '../api.dart';
import '../config.dart';
import '../theme.dart';
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
  const MessagePage({super.key, this.onClose});
  @override
  State<MessagePage> createState() => _MessagePageState();
}

class _MessagePageState extends State<MessagePage> {
  static const _max = 1000;
  final _contact = TextEditingController();
  final _msg = TextEditingController();
  final _scroll = ScrollController();
  final List<_Item> _items = [];
  bool _busy = false;
  String _err = '';

  @override
  void initState() {
    super.initState();
    _items.add(_Item('stamp', _stamp()));
    _items.add(_Item('admin', 'Hi! Write your message here and it goes straight to the admin on Telegram. Add a @username or email up top if you want a reply.'));
    _msg.addListener(() => setState(() {}));
  }

  @override
  void dispose() {
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
        Future.delayed(const Duration(milliseconds: 360), () {
          if (!mounted) return;
          setState(() => _items.add(_Item(
              'admin',
              c.isNotEmpty
                  ? '\u2713 Sent to the admin. They\u2019ll reply to $c when they can.'
                  : '\u2713 Sent to the admin. Add a @username or email up top next time if you want a reply.')));
          _toBottom();
        });
      }
    } catch (_) {
      fail('Can\u2019t reach the server. Check your connection.');
    } finally {
      if (mounted) setState(() => _busy = false);
      _toBottom();
    }
  }

  Widget _status(_Item m) {
    switch (m.st) {
      case _St.sending:
        return Text('Sending\u2026', style: inter(13, c: C.muted));
      case _St.delivered:
        return Text('Delivered', style: inter(13, c: C.muted));
      case _St.failed:
        return Column(crossAxisAlignment: CrossAxisAlignment.end, children: [
          Row(mainAxisSize: MainAxisSize.min, children: [
            Text('Not Delivered', style: inter(13, w: FontWeight.w700, c: C.red)),
            Text(' \u00B7 ', style: inter(13, c: C.muted)),
            Pressable(
              onTap: _busy ? null : () => _deliver(m),
              child: Text('Retry', style: inter(13, w: FontWeight.w600, c: C.teal)),
            ),
          ]),
          if (m.fail != null) ...[
            const SizedBox(height: 3),
            Text(m.fail!, textAlign: TextAlign.right, style: inter(13, c: C.muted)),
          ],
          if (m.tgLink)
            Pressable(
              onTap: () => openTelegram(context),
              child: Padding(
                padding: const EdgeInsets.only(top: 3),
                child: Text('Message the admin on Telegram instead', textAlign: TextAlign.right, style: inter(13, w: FontWeight.w600, c: C.teal)),
              ),
            ),
        ]);
      case null:
        return const SizedBox.shrink();
    }
  }

  Widget _bubble(_Item m) {
    final me = m.kind == 'me';
    final maxW = MediaQuery.of(context).size.width * .66;
    return Padding(
      padding: const EdgeInsets.only(bottom: 8),
      child: Column(crossAxisAlignment: me ? CrossAxisAlignment.end : CrossAxisAlignment.start, children: [
        ConstrainedBox(
          constraints: BoxConstraints(maxWidth: maxW),
          child: Container(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
            decoration: BoxDecoration(
              color: me ? C.teal : const Color(0xFF262628),
              borderRadius: BorderRadius.circular(22)),
            child: Text(m.text, style: inter(18, c: me ? C.onTeal : C.text, h: 1.3)),
          ),
        ),
        if (me && m.st != null) Padding(padding: const EdgeInsets.only(top: 4, right: 4), child: _status(m)),
      ]),
    );
  }

  @override
  Widget build(BuildContext context) {
    final len = _msg.text.length;
    final canSend = !_busy && _msg.text.trim().isNotEmpty;
    return Column(children: [
      Padding(
        padding: const EdgeInsets.fromLTRB(20, 14, 10, 10),
        child: Row(children: [
          Expanded(child: Text('Admin', style: inter(26, w: FontWeight.w700, h: 1.1))),
          if (widget.onClose != null)
            Pressable(
              onTap: widget.onClose,
              scale: .9,
              child: Container(
                width: 38, height: 38,
                decoration: const BoxDecoration(color: C.card, shape: BoxShape.circle),
                child: const Icon(Icons.close_rounded, size: 22, color: C.muted),
              ),
            ),
        ]),
      ),
      Padding(
        padding: const EdgeInsets.symmetric(horizontal: 14),
        child: Container(
          height: 52,
          padding: const EdgeInsets.symmetric(horizontal: 18),
          decoration: BoxDecoration(color: C.card, borderRadius: BorderRadius.circular(22)),
          child: Row(children: [
            Text('Reply to:', style: inter(17, c: C.muted)),
            const SizedBox(width: 10),
            Expanded(
              child: TextField(
                controller: _contact,
                maxLength: 120,

                keyboardType: TextInputType.emailAddress,
                textInputAction: TextInputAction.next,
                onChanged: (_) {
                  if (_err.isNotEmpty) setState(() => _err = '');
                },
                style: inter(17),
                cursorColor: C.teal,
                decoration: InputDecoration(
                  counterText: '',
                  border: InputBorder.none,
                  isCollapsed: true,
                  hintText: '@username or email (optional)',
                  hintStyle: inter(17, c: const Color(0xFF5C5C61)),
                ),
              ),
            ),
          ]),
        ),
      ),
      Expanded(
        child: ListView(
          controller: _scroll,
          physics: const BouncingScrollPhysics(),
          padding: const EdgeInsets.fromLTRB(14, 10, 14, 10),
          children: [
            for (final m in _items)
              if (m.kind == 'stamp')
                Padding(
                  padding: const EdgeInsets.symmetric(vertical: 10),
                  child: Center(
                    child: Text.rich(TextSpan(children: [
                      TextSpan(text: 'Today ', style: inter(14, w: FontWeight.w700, c: C.muted)),
                      TextSpan(text: m.text, style: inter(14, c: C.muted)),
                    ])),
                  ),
                )
              else
                _bubble(m),
          ],
        ),
      ),
      if (_err.isNotEmpty)
        Padding(
          padding: const EdgeInsets.fromLTRB(20, 0, 20, 6),
          child: Align(alignment: Alignment.centerLeft, child: Text(_err, style: inter(14, c: C.red))),
        ),
      Padding(
        padding: const EdgeInsets.fromLTRB(12, 4, 12, 12),
        child: Row(crossAxisAlignment: CrossAxisAlignment.end, children: [
          Expanded(
            child: Container(
              constraints: const BoxConstraints(minHeight: 50),
              padding: const EdgeInsets.symmetric(horizontal: 18, vertical: 4),
              decoration: BoxDecoration(color: C.card, borderRadius: BorderRadius.circular(25)),
              child: Column(mainAxisSize: MainAxisSize.min, crossAxisAlignment: CrossAxisAlignment.end, children: [
                TextField(
                  controller: _msg,
                  minLines: 1,
                  maxLines: 5,
                  maxLength: _max,
                  textCapitalization: TextCapitalization.sentences,
                  keyboardType: TextInputType.multiline,
                  style: inter(18),
                  cursorColor: C.teal,
                  onChanged: (_) {
                    if (_err.isNotEmpty) setState(() => _err = '');
                  },
                  decoration: InputDecoration(
                    counterText: '',
                    border: InputBorder.none,
                    hintText: 'Message',
                    hintStyle: inter(18, c: const Color(0xFF5C5C61)),
                    contentPadding: const EdgeInsets.symmetric(vertical: 12),
                  ),
                ),
                if (len > _max - 200)
                  Padding(padding: const EdgeInsets.only(bottom: 4), child: Text('$len / $_max', style: inter(12, c: C.muted))),
              ]),
            ),
          ),
          const SizedBox(width: 10),
          Pressable(
            onTap: canSend ? _submit : null,
            scale: .9,
            child: AnimatedContainer(
              duration: const Duration(milliseconds: 180),
              width: 50, height: 50,
              decoration: BoxDecoration(color: canSend ? C.teal : C.card, shape: BoxShape.circle),
              child: Icon(Icons.arrow_upward_rounded, size: 26, color: canSend ? C.onTeal : const Color(0xFF55555A)),
            ),
          ),
        ]),
      ),
    ]);
  }
}
