import 'dart:convert';
import 'package:flutter/material.dart' hide Text;
import 'package:http/http.dart' as http;
import '../anim.dart';
import '../api.dart';
import '../config.dart';
import '../theme.dart';
import '../tr.dart';

const _kLogKey = 'dwUpdateLog';

class _Upd {
  final String version, title;
  final List<String> notes;
  final int seen;
  _Upd(this.version, this.title, this.notes, this.seen);
  Map<String, dynamic> toJson() => {'version': version, 'title': title, 'notes': notes, 'seen': seen};
  static _Upd? from(dynamic j) {
    if (j is! Map) return null;
    final v = '${j['version'] ?? ''}';
    if (v.isEmpty) return null;
    return _Upd(
      v,
      '${j['title'] ?? 'Update'}',
      (j['notes'] is List) ? (j['notes'] as List).map((e) => '$e').toList() : <String>[],
      j['seen'] is num ? (j['seen'] as num).toInt() : 0,
    );
  }
}

String _plain(dynamic s) => '${s ?? ''}'.replaceAll(RegExp(r'<[^>]*>'), '').replaceAll('&nbsp;', ' ').replaceAll('&amp;', '&').trim();

List<_Upd> _load() {
  try {
    final raw = Api.i.prefs.getString(_kLogKey);
    if (raw == null || raw.isEmpty) return [];
    final v = jsonDecode(raw);
    if (v is! List) return [];
    return v.map(_Upd.from).whereType<_Upd>().toList();
  } catch (_) {
    return [];
  }
}

Future<void> _save(List<_Upd> l) => Api.i.prefs.setString(_kLogKey, jsonEncode(l.take(20).map((e) => e.toJson()).toList()));

/// Fetches the server's current update and adds it to the local log (new versions on top).
Future<bool> fetchLatestUpdate() async {
  try {
    final res = await http.get(Uri.parse('$kApiBase/api/version'), headers: Api.i.headers()).timeout(const Duration(seconds: 25));
    if (res.statusCode != 200) return false;
    final info = jsonDecode(res.body);
    if (info is! Map || '${info['version'] ?? ''}'.isEmpty) return false;
    final title = _plain(info['title']);
    final entry = _Upd(
      '${info['version']}',
      title.isEmpty ? 'Update' : title,
      (info['notes'] is List ? info['notes'] as List : const []).map(_plain).where((e) => e.isNotEmpty).toList(),
      DateTime.now().millisecondsSinceEpoch,
    );
    final log = _load();
    final i = log.indexWhere((x) => x.version == entry.version);
    if (i == -1) {
      log.insert(0, entry);
    } else {
      log[i] = _Upd(entry.version, entry.title, entry.notes, log[i].seen > 0 ? log[i].seen : entry.seen);
    }
    log.sort((a, b) => b.seen.compareTo(a.seen));
    await _save(log);
    return true;
  } catch (_) {
    return false;
  }
}

class UpdatesPage extends StatefulWidget {
  const UpdatesPage({super.key});
  @override
  State<UpdatesPage> createState() => _UpdatesPageState();
}

class _UpdatesPageState extends State<UpdatesPage> {
  List<_Upd> _log = _load();
  bool _loading = true;

  @override
  void initState() {
    super.initState();
    fetchLatestUpdate().then((_) {
      if (!mounted) return;
      setState(() {
        _log = _load();
        _loading = false;
      });
    });
  }

  String _date(int ms) {
    const m = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
    final d = DateTime.fromMillisecondsSinceEpoch(ms == 0 ? DateTime.now().millisecondsSinceEpoch : ms);
    return '${m[d.month - 1]} ${d.day}, ${d.year}';
  }

  Widget _label(String t) => Padding(
        padding: const EdgeInsets.fromLTRB(6, 24, 6, 10),
        child: Text(t.toUpperCase(), style: inter(14, w: FontWeight.w500, c: C.muted).copyWith(letterSpacing: .6)),
      );

  @override
  Widget build(BuildContext context) {
    final rows = <Widget>[];
    if (_log.isEmpty) {
      rows.add(Padding(
        padding: const EdgeInsets.only(top: 30),
        child: Center(
          child: Text(
            _loading ? 'Checking for updates\u2026' : 'No updates to show yet. Check your connection and reopen the app.',
            textAlign: TextAlign.center,
            style: inter(16, c: C.muted, h: 1.4),
          ),
        ),
      ));
    } else {
      if (_loading) {
        rows.add(Padding(padding: const EdgeInsets.only(top: 6), child: Text('Checking for updates\u2026', style: inter(15, c: C.muted))));
      }
      for (var i = 0; i < _log.length; i++) {
        final u = _log[i];
        rows.add(_label('v${u.version}${i == 0 ? ' \u00B7 Latest' : ''} \u00B7 ${_date(u.seen)}'));
        rows.add(Container(
          decoration: BoxDecoration(color: C.card, borderRadius: BorderRadius.circular(22)),
          child: Column(children: [
            Padding(
              padding: const EdgeInsets.fromLTRB(18, 16, 18, 16),
              child: Align(alignment: Alignment.centerLeft, child: Text(u.title, style: inter(17, w: FontWeight.w600))),
            ),
            for (final n in u.notes) ...[
              Container(height: 1, margin: const EdgeInsets.only(left: 18), color: const Color(0xFF2A2A2C)),
              Padding(
                padding: const EdgeInsets.fromLTRB(18, 14, 18, 15),
                child: Align(alignment: Alignment.centerLeft, child: Text(n, style: inter(15.5, c: C.muted, h: 1.4))),
              ),
            ],
          ]),
        ));
      }
    }
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
          Expanded(
            child: ListView(
              physics: const BouncingScrollPhysics(),
              padding: const EdgeInsets.fromLTRB(16, 4, 16, 40),
              children: [
                Padding(
                  padding: const EdgeInsets.fromLTRB(6, 0, 6, 0),
                  child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                    Text('Updates', style: inter(32, w: FontWeight.w700, h: 1.1)),
                    const SizedBox(height: 8),
                    Text('What changed in the app, newest first.', style: inter(16, c: C.muted, h: 1.4)),
                  ]),
                ),
                ...rows,
                const SizedBox(height: 30),
                Center(child: Text('Tycept \u00B7 WZH 2 APK', style: inter(14, c: C.muted))),
              ],
            ),
          ),
        ]),
      ),
    );
  }
}
