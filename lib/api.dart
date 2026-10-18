import 'dart:async';
import 'dart:convert';
import 'dart:io';
import 'dart:math';
import 'package:flutter/foundation.dart' show debugPrint;
import 'package:http/http.dart' as http;
import 'package:path_provider/path_provider.dart';
import 'package:shared_preferences/shared_preferences.dart';
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
  String? remoteUrl;
  bool signRelease, dex2c, stringProtect;
  List<String> permissions;
  // Source: a path on disk, or in-memory bytes (single-HTML and website-link builds are zipped first).
  String? zipPath;
  List<int>? zipBytes;
  String zipName;
  String? logoPath, googleServicesPath, customSplashPath;
  BuildRequest({
    this.appName = '', this.platform = 'android', this.buildType = 'apk', this.orientation = 'auto',
    this.cacheMode = 'fast', this.splashStyle = 'off', this.splashSpeed = 'normal',
    this.packageSuffix = '', this.appLinkHost = '', this.googleWebClientId = '', this.ownerKey = '',
    this.remoteUrl, this.signRelease = true, this.dex2c = false, this.stringProtect = false,
    this.permissions = const [], this.zipPath, this.zipBytes, this.zipName = 'project.zip',
    this.logoPath, this.googleServicesPath, this.customSplashPath,
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

class Api {
  Api._();
  static final Api i = Api._();

  final Prefs prefs = Prefs();
  String clientId = '';
  String? _token;
  Future<String>? _tokenFuture;
  bool customSigningOk = true;

  Future<void> init() async {
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
        if (kAppUserAgent.isNotEmpty) 'User-Agent': kAppUserAgent,
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
          throw ApiError('The build server refused this app (${_serverMessage(res.body) ?? 'not authorized'}). Check kSiteOrigin in config.dart matches your server.', status: 403, code: 'unauthorized');
        }
        if (res.statusCode != 200) throw ApiError('Could not start a build session (server returned ${res.statusCode}).', status: res.statusCode);
        final data = jsonDecode(res.body) as Map<String, dynamic>;
        _token = '${data['token']}';
        if (data['customSigning'] is bool) customSigningOk = data['customSigning'] as bool;
        return _token!;
      } catch (e) {
        last = e;
        if (e is ApiError && e.code == 'unauthorized') rethrow;
      }
    }
    throw ApiError(last is ApiError ? last.message : 'Could not reach the build server. Check your connection and try again.');
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

  Future<http.StreamedResponse> _post(BuildRequest r, String token, {bool withKey = true}) async {
    final req = http.MultipartRequest('POST', _u('/api/build'));
    req.headers.addAll(_h({'X-Session-Token': token, 'X-Client-Id': clientId, if (r.ownerKey.isNotEmpty) 'X-Admin-Password': r.ownerKey}));
    final f = req.fields;
    f['buildType'] = r.buildType;
    f['platform'] = r.platform;
    f['clientId'] = clientId;
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
    if (r.remoteUrl != null) f['remoteUrl'] = r.remoteUrl!;
    if (r.zipBytes != null) {
      req.files.add(http.MultipartFile.fromBytes('projectZip', r.zipBytes!, filename: r.zipName));
    } else if (r.zipPath != null) {
      req.files.add(await http.MultipartFile.fromPath('projectZip', r.zipPath!, filename: r.zipName));
    }
    if (r.logoPath != null) req.files.add(await http.MultipartFile.fromPath('logo', r.logoPath!));
    if (r.googleServicesPath != null) req.files.add(await http.MultipartFile.fromPath('googleServicesJson', r.googleServicesPath!));
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
      throw ApiError('${body['error'] ?? 'Server error (${res.statusCode})'}',
          status: res.statusCode, code: body['code']?.toString(),
          retryAfter: int.tryParse('${body['retryAfter'] ?? res.headers['retry-after'] ?? 30}') ?? 30);
    }
    final id = body['jobId'];
    if (id == null) throw ApiError('${body['error'] ?? 'The server did not start the build.'}');
    return '$id';
  }

  Future<Job> status(String jobId, {int since = 0}) async {
    final res = await _authed((h) => http.get(_u('/api/status/$jobId', {'since': '$since'}), headers: h));
    if (res.statusCode == 404) throw ApiError('Build not found', status: 404);
    if (res.statusCode != 200) throw ApiError('Server error (${res.statusCode})', status: res.statusCode);
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
      if (res.statusCode != 200) throw ApiError('The download isn\u2019t available (${res.statusCode}).', status: res.statusCode);
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
