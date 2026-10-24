import 'dart:convert';
import 'package:flutter/material.dart' hide Text;
import 'package:http/http.dart' as http;
import 'package:package_info_plus/package_info_plus.dart';
import 'package:url_launcher/url_launcher.dart';
import 'anim.dart';
import 'api.dart';
import 'banner.dart';
import 'store.dart';
import 'config.dart';
import 'theme.dart';
import 'widgets.dart';
import 'tr.dart';

/// The two announcement popups of the HTML app, with the same server routes and the same rules:
///  1. "Update available" / What's new  (GET /api/version): shown once per version, or on every
///     launch while the admin has "alwaysShow" on. The very first launch only records the version.
///  2. "New version available" (GET /api/app-update): shown while the installed build number is
///     lower than the one set in /admin/app-update, again after coming back to the app
///     (at most once every 10 minutes). Never shown at the same time as the first popup.
class Notices {
  static bool _busy = false;
  static DateTime? _lastCheck;
  static const _cooldown = Duration(minutes: 10);

  /// The server compares against a build-time Unix timestamp (that is what /admin/app-update lists).
  /// A normal Flutter build number such as 5 can't be compared with it, so the APK popup is skipped
  /// unless the app was built with a timestamp-sized build number (flutter build apk --build-number=...).
  static const _minComparableCode = 1000000000;

  static Future<Map<String, dynamic>?> _get(String path) async {
    try {
      final res = await http
          .get(Uri.parse('$kApiBase$path'), headers: Api.i.headers({'Cache-Control': 'no-store'}))
          .timeout(const Duration(seconds: 12));
      if (res.statusCode != 200) return null;
      final d = jsonDecode(res.body);
      return d is Map<String, dynamic> ? d : null;
    } catch (_) {
      return null; // offline: try again next launch
    }
  }

  static List<String> _notes(dynamic v) => v is List
      ? [for (final n in v) if (n is String && n.trim().isNotEmpty) n.replaceAll(RegExp(r'<[^>]*>'), '').trim()]
      : const [];

  static Future<void> _open(String url) async {
    final u = Uri.tryParse(url.trim());
    if (u == null || (u.scheme != 'http' && u.scheme != 'https')) return;
    try {
      await launchUrl(u, mode: LaunchMode.externalApplication);
    } catch (_) {}
  }

  static Future<void> check(BuildContext context, {bool force = false}) async {
    if (_busy) return;
    final last = _lastCheck;
    if (!force && last != null && DateTime.now().difference(last) < _cooldown) return;
    _busy = true;
    _lastCheck = DateTime.now();
    try {
      await _whatsNew(context);
      if (context.mounted) await _apkUpdate(context);
    } finally {
      _busy = false;
    }
  }

  static Future<void> _whatsNew(BuildContext context) async {
    final info = await _get('/api/version');
    final version = info?['version']?.toString() ?? '';
    if (info == null || version.isEmpty) return;
    final seen = Api.i.prefs.getString('apkBuilderSeenVersion');
    final always = info['alwaysShow'] == true;
    if (!always && seen == version) return;
    await Api.i.prefs.setString('apkBuilderSeenVersion', version);
    if (seen == null && !always) return; // first launch ever: just remember it
    if (!context.mounted) return;
    final go = await _dialog(
      context,
      title: (info['title']?.toString().trim().isNotEmpty ?? false) ? info['title'].toString() : 'Update available',
      sub: 'v$version',
      notes: _notes(info['notes']),
      primary: (info['updateButtonText']?.toString().trim().isNotEmpty ?? false) ? info['updateButtonText'].toString() : 'Update now',
      later: (info['laterButtonText']?.toString().trim().isNotEmpty ?? false) ? info['laterButtonText'].toString() : 'Later',
    );
    if (go) await _open(info['updateUrl']?.toString() ?? '');
  }

  /// Top menu "Refresh app": reloads the data like before, then (same as the HTML refresh button)
  /// checks the server version. If this app already saw the latest version, a green
  /// "Up to date" banner drops in; otherwise the normal update popup is shown.
  static Future<void> refreshApp(BuildContext context) async {
    Store.i.refreshAll();
    final info = await _get('/api/version');
    final version = info?['version']?.toString() ?? '';
    if (!context.mounted) return;
    if (info == null || version.isEmpty) {
      IosBanner.show(context, kind: BannerKind.warn, title: 'Can\u2019t check for updates', msg: 'Check your wifi or data and try again.');
      return;
    }
    final seen = Api.i.prefs.getString('apkBuilderSeenVersion');
    if (seen != version) {
      await check(context, force: true); // shows What's new and records the version
      return;
    }
    // Already on this version: still tell about a newer APK if the admin published one.
    final shown = await _apkUpdate(context);
    if (shown || !context.mounted) return;
    IosBanner.show(context, kind: BannerKind.ok, title: 'You\u2019re all caught up', msg: 'You\u2019re on the latest version (v$version). Nothing new to grab.');
  }

  static Future<bool> _apkUpdate(BuildContext context) async {
    int? installed;
    try {
      installed = int.tryParse((await PackageInfo.fromPlatform()).buildNumber);
    } catch (_) {}
    if (installed == null || installed < _minComparableCode) return false;
    final info = await _get('/api/app-update');
    if (info == null || info['active'] != true) return false;
    final apk = info['apkUrl']?.toString() ?? '';
    final latest = (info['latestVersionCode'] as num?)?.toInt() ?? 0;
    if (apk.isEmpty || installed >= latest) return false;
    if (!context.mounted) return false;
    final name = info['latestVersionName']?.toString().replaceFirst(RegExp(r'^v', caseSensitive: false), '') ?? '';
    final go = await _dialog(
      context,
      title: (info['title']?.toString().trim().isNotEmpty ?? false) ? info['title'].toString() : 'New version available',
      sub: name.isEmpty ? 'New version' : 'v$name',
      notes: _notes(info['notes']),
      primary: (info['updateButtonText']?.toString().trim().isNotEmpty ?? false) ? info['updateButtonText'].toString() : 'Download update',
      later: (info['laterButtonText']?.toString().trim().isNotEmpty ?? false) ? info['laterButtonText'].toString() : 'Later',
    );
    if (go) await _open(apk);
    return true;
  }

  /// The popup: title, version, numbered changelog, a green pill and a plain "Later".
  static Future<bool> _dialog(BuildContext context,
      {required String title, required String sub, required List<String> notes, required String primary, required String later}) async {
    final r = await showIosDialog<bool>(
      context,
      (ctx) {
        final screen = MediaQuery.of(ctx).size;
        return Container(
          width: screen.width - 32 < 320 ? screen.width - 32 : 320,
          padding: const EdgeInsets.all(20),
          decoration: BoxDecoration(color: const Color(0xFF1E1E20), borderRadius: BorderRadius.circular(26)),
          child: Column(mainAxisSize: MainAxisSize.min, crossAxisAlignment: CrossAxisAlignment.start, children: [
            Text(title, style: inter(20, w: FontWeight.w700)),
            const SizedBox(height: 4),
            Text(sub, style: inter(15, c: C.muted)),
            if (notes.isNotEmpty) ...[
              const SizedBox(height: 14),
              ConstrainedBox(
                constraints: BoxConstraints(maxHeight: screen.height * .4),
                child: SingleChildScrollView(
                  physics: const BouncingScrollPhysics(),
                  child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                    for (var i = 0; i < notes.length; i++)
                      Padding(
                        padding: const EdgeInsets.only(bottom: 10),
                        child: Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
                          Container(
                            width: 22, height: 22, margin: const EdgeInsets.only(top: 1),
                            alignment: Alignment.center,
                            decoration: const BoxDecoration(color: C.tealDeep, shape: BoxShape.circle),
                            child: Text('${i + 1}', style: inter(12, w: FontWeight.w700, c: C.teal, h: 1)),
                          ),
                          const SizedBox(width: 12),
                          Expanded(child: Text(notes[i], style: inter(15, c: const Color(0xFFD8D8DC), h: 1.35))),
                        ]),
                      ),
                  ]),
                ),
              ),
            ],
            const SizedBox(height: 10),
            Pressable(
              onTap: () => Navigator.pop(ctx, true),
              child: Container(
                height: 46, alignment: Alignment.center,
                decoration: BoxDecoration(color: C.tealBtn, borderRadius: BorderRadius.circular(23)),
                child: Text(primary, style: inter(16, w: FontWeight.w700, c: C.onTeal)),
              ),
            ),
            Pressable(
              onTap: () => Navigator.pop(ctx, false),
              child: Container(height: 42, alignment: Alignment.center, child: Text(later, style: inter(16, w: FontWeight.w500, c: C.muted))),
            ),
          ]),
        );
      },
    );
    return r == true;
  }
}
