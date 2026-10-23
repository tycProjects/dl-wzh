import 'dart:async';
import 'dart:convert';
import 'dart:io';
import 'dart:math';
import 'package:flutter/foundation.dart' show debugPrint;
import 'package:http/http.dart' as http;
import 'package:path_provider/path_provider.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'app_proof.dart';
import 'config.dart';

class ApiError implements Exception {
  final String message;
  final int? status;
  final String? code;
  final int retryAfter;
  ApiError(this.message, {this.status, this.code, this.retryAfter = 30});
  @override
  String toString() => message;
}

class BuildLimit {
  final int used, limit, remaining;
  final DateTime? resetsAt;
  BuildLimit(this.used, this.limit, this.remaining, this.resetsAt);
  factory BuildLimit.fromJson(Map<String, dynamic> j) {
    int n(dynamic v) => v is num ? v.toInt() : int.tryParse('$v') ?? 0;
    return BuildLimit(n(j['used']), n(j['limit']), n(j['remaining']),
        j['resetsAt'] == null ? null : DateTime.tryParse('${j['resetsAt']}'));
  }
}


/// One site the user has hosted on Cloudflare Pages.
class HostedSite {
  final String name, url;
  HostedSite(this.name, this.url);
}

class HostedSites {
  final int limit;
  final String suffix; // added to every name by the server, e.g. myshop-a1b2c3
  final List<HostedSite> sites;
  HostedSites(this.limit, this.suffix, this.sites);
}

class Job {
  final Map<String, dynamic> j;
  Job(this.j);
  String get id => '${j['jobId'] ?? j['id'] ?? ''}';
  String get status => '${j['status'] ?? ''}';
  String get message => '${j['message'] ?? ''}';
  String get name => ('${j['originalName'] ?? j['appName'] ?? id}').replaceAll(RegExp(r'\.zip$', caseSensitive: false), '');
  String get platform => '${j['platform'] ?? 'android'}';
  String get buildType => '${j['buildType'] ?? 'apk'}';
  String? get filename => j['apkDownloadFilename'] == null ? null : '${j['apkDownloadFilename']}';
  bool get expired => j['expired'] == true;
  DateTime? get createdAt => j['createdAt'] == null ? null : DateTime.tryParse('${j['createdAt']}')?.toLocal();
  int? get sizeBytes {
    for (final k in ['apkDownloadSizeBytes', 'apkSizeBytes', 'apkSize', 'sizeBytes', 'size']) {
      final v = j[k];
      if (v is num && v > 0) return v.toInt();
    }
    return null;
  }
  int? get durationMs => j['durationMs'] is num ? (j['durationMs'] as num).toInt() : null;
  int get logsTotal => j['logsTotal'] is num ? (j['logsTotal'] as num).toInt() : ((j['logs'] is List) ? (j['logs'] as List).length : 0);
  List<Map<String, dynamic>> get logs =>
      (j['logs'] is List) ? (j['logs'] as List).whereType<Map>().map((e) => Map<String, dynamic>.from(e)).toList() : [];
  String get typeLabel => platform == 'windows' ? 'EXE' : (buildType == 'both' ? 'APK + AAB' : buildType.toUpperCase());
  bool get isDone => status == 'done';
  bool get isError => status == 'error';
  bool get isCancelled => status == 'cancelled';
  bool get isActive => !isDone && !isError && !isCancelled;
}

/// Everything the build needs, mirroring the form fields the HTML app posts.
class BuildRequest {
  String appName, platform, buildType, orientation, cacheMode, splashStyle, splashSpeed;
  String packageSuffix, appLinkHost, googleWebClientId, ownerKey;
  // Firebase values pasted instead of a google-services.json (the server builds the file from them).
  String firebaseAppId, firebaseApiKey, firebaseProjectId;
  // 'full' = built on the server (every feature); 'quick' = on-phone wrapper (HTML and websites only).
  String mode;
  // Owner only: ask the server to build the shared Quick template APK instead of a normal app.
  bool quickTemplate;
  String? remoteUrl;
  bool signRelease, dex2c, stringProtect;
  List<String> permissions;
  // Source: a path on disk, or in-memory bytes (single-HTML and website-link builds are zipped first).
  String? zipPath;
  List<int>? zipBytes;
  String zipName;
  String? logoPath, googleServicesPath, customSplashPath;
  // The user's own release key (.pk8 + certificate). Both or neither.
  String? signingKeyPath, signingCertPath;
  BuildRequest({
    this.appName = '', this.platform = 'android', this.buildType = 'apk', this.orientation = 'auto',
    this.cacheMode = 'fast', this.splashStyle = 'off', this.splashSpeed = 'normal',
    this.packageSuffix = '', this.appLinkHost = '', this.googleWebClientId = '', this.firebaseAppId = '', this.firebaseApiKey = '', this.firebaseProjectId = '', this.ownerKey = '', this.mode = 'full', this.quickTemplate = false,
    this.remoteUrl, this.signRelease = true, this.dex2c = false, this.stringProtect = false,
    this.permissions = const [], this.zipPath, this.zipBytes, this.zipName = 'project.zip',
    this.logoPath, this.googleServicesPath, this.customSplashPath, this.signingKeyPath, this.signingCertPath,
  });
}

/// Small settings store. Uses shared_preferences when the native plugin works,
/// and silently falls back to memory so a plugin/version mismatch can never
/// block app startup.
class Prefs {
  SharedPreferences? _sp;
  final Map<String, String> _mem = {};

  Future<void> load() async {
    try {
      _sp = await SharedPreferences.getInstance();
    } catch (e) {
      debugPrint('SharedPreferences unavailable, using memory: $e');
      _sp = null;
    }
  }

  String? getString(String key) {
    try {
      final v = _sp?.getString(key);
      if (v != null) return v;
    } catch (_) {}
    return _mem[key];
  }

  Future<void> setString(String key, String value) async {
    _mem[key] = value;
    try {
      await _sp?.setString(key, value);
    } catch (_) {}
  }

  Future<void> remove(String key) async {
    _mem.remove(key);
    try {
      await _sp?.remove(key);
    } catch (_) {}
  }
}

/// One compiler error from the server's compile-only check.
class CompileIssue {
  final String file;
  final int line;
  final String message;
  const CompileIssue(this.file, this.line, this.message);
}

class CompileCheck {
  final bool ok;
  final List<CompileIssue> issues;
  const CompileCheck(this.ok, this.issues);
  factory CompileCheck.fromJson(Map<String, dynamic> m) {
    final list = <CompileIssue>[];
    for (final e in (m['errors'] as List? ?? const [])) {
      if (e is! Map) continue;
      list.add(CompileIssue('${e['file'] ?? ''}', int.tryParse('${e['line'] ?? 1}') ?? 1, '${e['message'] ?? 'Error'}'));
    }
    return CompileCheck(m['ok'] == true && list.isEmpty, list);
  }
}

class Api {
  Api._();
  static final Api i = Api._();

  final Prefs prefs = Prefs();
  String clientId = '';
  String? _token;
  Future<String>? _tokenFuture;
  bool customSigningOk = true;

  Future<void> init() async {
    // Reads this app's real signature + code fingerprint before the first request. If it is slow
    // it keeps going in the background; builds are only sent much later.
    await AppProof.load().timeout(const Duration(seconds: 6), onTimeout: () {});
    await prefs.load();
    var id = prefs.getString('apkBuilderClientId');
    if (id == null || id.isEmpty) {
      final r = Random.secure();
      id = 'c${DateTime.now().millisecondsSinceEpoch.toRadixString(36)}${List.generate(8, (_) => r.nextInt(36).toRadixString(36)).join()}';
      await prefs.setString('apkBuilderClientId', id);
    }
    clientId = id;
    wake();
  }

  Uri _u(String path, [Map<String, String>? q]) => Uri.parse('$kApiBase$path').replace(queryParameters: q);
  Map<String, String> headers([Map<String, String>? extra]) => _h(extra);
  // Every request carries the site Origin and the app User-Agent the server checks for.
  Map<String, String> _h([Map<String, String>? extra]) => {
        'Origin': kSiteOrigin,
        'User-Agent': AppProof.userAgent,
        ...AppProof.extraHeaders,
        ...?extra,
      };

  /// Render's free tier sleeps; a quick ping at launch starts waking it while the app opens.
  void wake() {
    http.get(_u('/healthz'), headers: _h()).timeout(const Duration(seconds: 60)).then((_) {}, onError: (_) {});
  }

  /// Session token. Render's free tier can take ~a minute to wake, so retry for a while.
  Future<String> token({bool refresh = false}) {
    if (refresh) {
      _token = null;
      _tokenFuture = null;
    }
    if (_token != null) return Future.value(_token);
    return _tokenFuture ??= _fetchToken().whenComplete(() => _tokenFuture = null);
  }

  Future<String> _fetchToken() async {
    const delays = [0, 4, 8, 15, 20];
    Object? last;
    for (final d in delays) {
      if (d > 0) await Future.delayed(Duration(seconds: d));
      try {
        final res = await http.get(_u('/api/session'), headers: _h()).timeout(const Duration(seconds: 30));
        if (res.statusCode == 403) {
          throw ApiError('The server said no to this app. Update to the latest version and try again.', status: 403, code: 'unauthorized');
        }
        if (res.statusCode != 200) throw ApiError('Couldn\u2019t get things started. Give it a sec and try again.', status: res.statusCode);
        final data = jsonDecode(res.body) as Map<String, dynamic>;
        _token = '${data['token']}';
        if (data['customSigning'] is bool) customSigningOk = data['customSigning'] as bool;
        return _token!;
      } catch (e) {
        last = e;
        if (e is ApiError && e.code == 'unauthorized') rethrow;
      }
    }
    throw ApiError(last is ApiError ? last.message : 'Can\u2019t reach the server. Check your connection and try again.');
  }

  String? _serverMessage(String body) {
    try {
      final e = (jsonDecode(body) as Map)['error'];
      return e == null ? null : '$e';
    } catch (_) {
      return null;
    }
  }

  Future<http.Response> _authed(Future<http.Response> Function(Map<String, String> h) send) async {
    var res = await send(_h({'X-Session-Token': await token()}));
    if (res.statusCode == 403) {
      res = await send(_h({'X-Session-Token': await token(refresh: true)}));
    }
    return res;
  }

  // ---- Host my site (Cloudflare Pages through the build server) ----------------------------
  // The user is the app's install id; the server allows 2 live sites per id.
  Map<String, String> _hostH() => _h({'X-Host-Owner': clientId});

  String _hostErr(http.Response res, String fallback) {
    final m = _serverMessage(res.body);
    return m ?? fallback;
  }

  Future<HostedSites> hostedSites() async {
    final res = await http.get(_u('/api/host/mine'), headers: _hostH()).timeout(const Duration(seconds: 40));
    if (res.statusCode != 200) throw ApiError(_hostErr(res, 'Couldn\u2019t load your sites'), status: res.statusCode);
    final j = jsonDecode(res.body) as Map<String, dynamic>;
    final list = (j['sites'] is List) ? (j['sites'] as List).whereType<Map>() : const <Map>[];
    return HostedSites(
      j['limit'] is num ? (j['limit'] as num).toInt() : 2,
      '${j['suffix'] ?? ''}',
      [for (final m in list) HostedSite('${m['name']}', '${m['url']}')],
    );
  }

  /// Uploads a .zip or .html and returns the live link. Pass [updateName] to replace one of your sites.
  Future<HostedSite> hostSite(String path, String fileName, {String? updateName, String? siteName}) async {
    final req = http.MultipartRequest('POST', _u('/api/host'));
    req.headers.addAll(_hostH());
    if (updateName != null) req.fields['name'] = updateName;
    if (updateName == null && siteName != null && siteName.isNotEmpty) req.fields['siteName'] = siteName;
    req.files.add(await http.MultipartFile.fromPath('projectZip', path, filename: fileName));
    final res = await http.Response.fromStream(await req.send().timeout(const Duration(minutes: 4)));
    if (res.statusCode != 200) {
      String? code;
      try { code = (jsonDecode(res.body) as Map)['code']?.toString(); } catch (_) {}
      throw ApiError(_hostErr(res, 'Hosting didn\u2019t work'), status: res.statusCode, code: code);
    }
    final j = jsonDecode(res.body) as Map<String, dynamic>;
    return HostedSite('${j['name']}', '${j['url']}');
  }

  Future<void> deleteHostedSite(String name) async {
    final res = await http.delete(_u('/api/host/$name'), headers: _hostH()).timeout(const Duration(seconds: 40));
    if (res.statusCode != 200) throw ApiError(_hostErr(res, 'Couldn\u2019t delete that site'), status: res.statusCode);
  }

  Future<BuildLimit?> limit({String ownerKey = ''}) async {
    try {
      final res = await http.get(_u('/api/build-limit'),
          headers: _h({'X-Client-Id': clientId, if (ownerKey.isNotEmpty) 'X-Admin-Password': ownerKey})).timeout(const Duration(seconds: 12));
      if (res.statusCode != 200) return null;
      return BuildLimit.fromJson(jsonDecode(res.body) as Map<String, dynamic>);
    } catch (_) {
      return null;
    }
  }

  Future<http.StreamedResponse> _post(BuildRequest r, String token, {bool withKey = true, bool check = false}) async {
    final req = http.MultipartRequest('POST', _u('/api/build', check ? {'check': '1'} : null));
    req.headers.addAll(_h({'X-Session-Token': token, 'X-Client-Id': clientId, if (r.ownerKey.isNotEmpty) 'X-Admin-Password': r.ownerKey}));
    final f = req.fields;
    f['buildType'] = r.buildType;
    f['platform'] = r.platform;
    f['clientId'] = clientId;
    if (r.quickTemplate && r.ownerKey.isNotEmpty) f['quickTemplate'] = '1';
    f['signRelease'] = r.signRelease ? 'true' : 'false';
    f['dex2c'] = r.dex2c ? 'true' : 'false';
    f['stringProtect'] = r.stringProtect ? 'true' : 'false';
    f['permissions'] = jsonEncode(r.permissions);
    f['cacheMode'] = r.cacheMode;
    f['splashStyle'] = r.splashStyle;
    f['splashSpeed'] = r.splashSpeed;
    if (r.orientation != 'auto') f['orientation'] = r.orientation;
    if (r.appName.trim().isNotEmpty) f['appName'] = r.appName.trim();
    if (r.packageSuffix.trim().isNotEmpty) f['packageSuffix'] = r.packageSuffix.trim();
    if (r.appLinkHost.trim().isNotEmpty) f['appLinkHost'] = r.appLinkHost.trim();
    if (r.googleWebClientId.trim().isNotEmpty) f['googleWebClientId'] = r.googleWebClientId.trim();
    if (r.googleServicesPath == null &&
        (r.firebaseAppId.trim().isNotEmpty || r.firebaseApiKey.trim().isNotEmpty || r.firebaseProjectId.trim().isNotEmpty)) {
      f['firebaseAppId'] = r.firebaseAppId.trim();
      f['firebaseApiKey'] = r.firebaseApiKey.trim();
      f['firebaseProjectId'] = r.firebaseProjectId.trim();
    }
    if (r.remoteUrl != null) f['remoteUrl'] = r.remoteUrl!;
    if (r.zipBytes != null) {
      req.files.add(http.MultipartFile.fromBytes('projectZip', r.zipBytes!, filename: r.zipName));
    } else if (r.zipPath != null) {
      req.files.add(await http.MultipartFile.fromPath('projectZip', r.zipPath!, filename: r.zipName));
    }
    if (r.logoPath != null) req.files.add(await http.MultipartFile.fromPath('logo', r.logoPath!));
    if (r.googleServicesPath != null) req.files.add(await http.MultipartFile.fromPath('googleServicesJson', r.googleServicesPath!));
    if (r.signingKeyPath != null && r.signingCertPath != null) {
      req.files.add(await http.MultipartFile.fromPath('signingKey', r.signingKeyPath!, filename: 'signing-key.pk8'));
      req.files.add(await http.MultipartFile.fromPath('signingCert', r.signingCertPath!, filename: 'signing-cert.pem'));
    }
    if (r.customSplashPath != null && r.splashStyle == 'custom') {
      req.files.add(await http.MultipartFile.fromPath('customSplash', r.customSplashPath!));
    }
    return req.send();
  }

  /// Starts a build and returns the job id. Throws [ApiError] (429 = daily limit, 503 server_busy).
  Future<String> startBuild(BuildRequest r) async {
    var res = await http.Response.fromStream(await _post(r, await token()));
    if (res.statusCode == 403) {
      res = await http.Response.fromStream(await _post(r, await token(refresh: true)));
    }
    Map<String, dynamic> body = {};
    try {
      body = jsonDecode(res.body) as Map<String, dynamic>;
    } catch (_) {}
    if (res.statusCode != 200) {
      throw ApiError('${body['error'] ?? 'Server error. Please try again in a bit.'}',
          status: res.statusCode, code: body['code']?.toString(),
          retryAfter: int.tryParse('${body['retryAfter'] ?? res.headers['retry-after'] ?? 30}') ?? 30);
    }
    final id = body['jobId'];
    if (id == null) throw ApiError('${body['error'] ?? 'The server didn\u2019t start the build. Try again.'}');
    return '$id';
  }

  /// Compile-only check on the build server (POST /api/build?check=1). It does not use up a build.
  /// Returns null when the server can't do the check (older server, busy, daily check allowance used,
  /// offline, took too long), so the check itself can never stop someone from building.
  Future<CompileCheck?> compileCheck(List<int> zipBytes, String zipName, {bool Function()? cancelled, void Function(String)? onStatus}) async {
    String? id;
    try {
      final r = BuildRequest(platform: 'android')
        ..zipBytes = zipBytes
        ..zipName = zipName;
      var res = await http.Response.fromStream(await _post(r, await token(), check: true)).timeout(const Duration(seconds: 120));
      if (res.statusCode == 403) res = await http.Response.fromStream(await _post(r, await token(refresh: true), check: true)).timeout(const Duration(seconds: 120));
      if (res.statusCode != 200) return null;
      id = '${(jsonDecode(res.body) as Map<String, dynamic>)['jobId'] ?? ''}';
      if (id.isEmpty) return null;
      final end = DateTime.now().add(const Duration(minutes: 9));
      while (DateTime.now().isBefore(end)) {
        if (cancelled?.call() ?? false) break;
        await Future.delayed(const Duration(seconds: 3));
        if (cancelled?.call() ?? false) break;
        final jid = id;
        final sr = await _authed((h) => http.get(_u('/api/status/$jid'), headers: h)).timeout(const Duration(seconds: 30));
        if (sr.statusCode != 200) return null;
        final m = jsonDecode(sr.body) as Map<String, dynamic>;
        final st = '${m['status']}';
        if (st == 'check-done') {
          if (m['checkUnavailable'] == true) return null;
          return CompileCheck.fromJson({'ok': m['checkOk'], 'errors': m['checkErrors']});
        }
        if (st == 'error' || st == 'cancelled') return null;
        onStatus?.call(st == 'building' ? 'Compiling your code\u2026' : (st == 'queued' ? 'Waiting for the build server\u2026' : 'Sending your project\u2026'));
      }
    } catch (_) {
      return null;
    }
    // gave up or was cancelled: free the server's slot
    final jid = id;
    if (jid != null && jid.isNotEmpty) {
      _authed((h) => http.post(_u('/api/cancel/$jid'), headers: h)).then((_) {}, onError: (_) {});
    }
    return null;
  }

  Future<Job> status(String jobId, {int since = 0}) async {
    final res = await _authed((h) => http.get(_u('/api/status/$jobId', {'since': '$since'}), headers: h));
    if (res.statusCode == 404) throw ApiError('Can\u2019t find that build', status: 404);
    if (res.statusCode != 200) throw ApiError('Server error. Please try again in a bit.', status: res.statusCode);
    final m = jsonDecode(res.body) as Map<String, dynamic>;
    m.putIfAbsent('jobId', () => jobId);
    return Job(m);
  }

  Future<Job?> cancel(String jobId) async {
    final res = await _authed((h) => http.post(_u('/api/cancel/$jobId'), headers: h));
    if (res.statusCode != 200) return null;
    return Job(jsonDecode(res.body) as Map<String, dynamic>);
  }

  Future<void> repair(String jobId, bool confirm) async {
    final res = await _authed((h) => http.post(_u('/api/repair/$jobId/${confirm ? 'confirm' : 'reject'}'), headers: h));
    if (res.statusCode != 200) {
      String m = 'Could not ${confirm ? 'apply' : 'skip'} the repair';
      try {
        m = '${(jsonDecode(res.body) as Map)['error'] ?? m}';
      } catch (_) {}
      throw ApiError(m);
    }
  }

  Future<List<Job>> history() async {
    final res = await _authed((h) => http.get(_u('/api/history', {'clientId': clientId}), headers: h));
    if (res.statusCode != 200) return [];
    final d = jsonDecode(res.body);
    return d is List ? d.whereType<Map>().map((e) => Job(Map<String, dynamic>.from(e))).toList() : [];
  }

  Future<void> deleteHistory(String jobId) async {
    try {
      await _authed((h) => http.delete(_u('/api/history/${Uri.encodeComponent(jobId)}', {'clientId': clientId}), headers: h));
    } catch (_) {}
  }

  Uri downloadUri(Job job, {String? aab}) {
    if (aab != null) return Uri.parse('$kApiBase/api/download/${job.id}?apk=${Uri.encodeComponent(aab)}');
    return job.filename != null
        ? Uri.parse('$kApiBase/api/download/${job.id}/${Uri.encodeComponent(job.filename!)}')
        : Uri.parse('$kApiBase/api/download/${job.id}');
  }

  /// Streams the file to disk with real byte progress. Returns the saved path.
  Future<String> download(Job job, void Function(int done, int? total) onProgress, {String? aab, bool Function()? cancelled}) async {
    Directory? base;
    try {
      base = await getExternalStorageDirectory();
    } catch (_) {}
    base ??= await getApplicationDocumentsDirectory();
    final dir = Directory('${base.path}/Download');
    await dir.create(recursive: true);
    final ext = job.platform == 'windows' ? 'exe' : 'apk';
    final name = (aab ?? job.filename ?? '${job.name}.$ext').replaceAll(RegExp(r'[\\/:*?"<>|]'), '_');
    final file = File('${dir.path}/$name');
    final client = http.Client();
    try {
      final res = await client.send(http.Request('GET', downloadUri(job, aab: aab))..headers.addAll(_h()));
      if (res.statusCode != 200) throw ApiError('That download isn\u2019t available anymore.', status: res.statusCode);
      final total = (res.contentLength != null && res.contentLength! > 0) ? res.contentLength : null;
      final sink = file.openWrite();
      var done = 0;
      try {
        await for (final chunk in res.stream) {
          if (cancelled != null && cancelled()) {
            await sink.close();
            try { await file.delete(); } catch (_) {}
            throw ApiError('Cancelled');
          }
          sink.add(chunk);
          done += chunk.length;
          onProgress(done, total);
        }
      } finally {
        await sink.close();
      }
      return file.path;
    } finally {
      client.close();
    }
  }
}
