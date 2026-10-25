import 'dart:async';
import 'dart:convert';
import 'dart:io';
import 'dart:typed_data';
import 'package:archive/archive.dart';
import 'package:http/http.dart' as http;
import 'api.dart';
import 'blake3.dart';
import 'config.dart';

/// "Host a site" without the build server. The phone unzips the site, names every file the way
/// Cloudflare Pages wants and uploads the files straight to Cloudflare with a short-lived upload
/// pass. The small Worker at [kHostWorker] (worker/host-worker.js) only creates / lists / deletes
/// sites and publishes the finished upload, so it needs no server and never sees the files.

bool get hostViaWorker => kHostWorker.trim().startsWith('https://');

const String _cfApi = 'https://api.cloudflare.com/client/v4';
const int _maxFile = 25 * 1024 * 1024;
const int _maxTotal = 100 * 1024 * 1024;
const int _maxFiles = 2000;
const int _batchBytes = 20 * 1024 * 1024;
const int _batchFiles = 40;
const Set<String> _skipParts = {'__MACOSX', '.git', 'node_modules', '.DS_Store', '.svn'};
const Map<String, String> _mime = {
  'html': 'text/html', 'htm': 'text/html', 'css': 'text/css', 'js': 'application/javascript', 'mjs': 'application/javascript',
  'json': 'application/json', 'webmanifest': 'application/manifest+json', 'txt': 'text/plain', 'xml': 'application/xml',
  'svg': 'image/svg+xml', 'png': 'image/png', 'jpg': 'image/jpeg', 'jpeg': 'image/jpeg', 'gif': 'image/gif', 'webp': 'image/webp',
  'avif': 'image/avif', 'ico': 'image/x-icon', 'woff': 'font/woff', 'woff2': 'font/woff2', 'ttf': 'font/ttf', 'otf': 'font/otf',
  'mp3': 'audio/mpeg', 'wav': 'audio/wav', 'ogg': 'audio/ogg', 'mp4': 'video/mp4', 'webm': 'video/webm', 'pdf': 'application/pdf',
  'wasm': 'application/wasm', 'map': 'application/json',
};
const List<String> _special = ['_headers', '_redirects', '_routes.json'];

Uri _wu(String path) => Uri.parse('${kHostWorker.trim().replaceAll(RegExp(r'/+$'), '')}$path');
Map<String, String> _wh() => Api.i.headers({'X-Host-Owner': Api.i.clientId, 'Content-Type': 'application/json'});

ApiError _workerErr(http.Response res, String fallback) {
  String? msg, code;
  try {
    final d = jsonDecode(res.body);
    if (d is Map) {
      if (d['error'] != null) msg = '${d['error']}';
      if (d['code'] != null) code = '${d['code']}';
    }
  } catch (_) {}
  return ApiError(msg ?? fallback, status: res.statusCode, code: code);
}

Future<Map<String, dynamic>> _workerCall(String method, String path, [Map<String, dynamic>? body, Duration? wait]) async {
  http.Response res;
  try {
    final u = _wu(path);
    final h = _wh();
    final t = wait ?? const Duration(seconds: 60);
    switch (method) {
      case 'GET':
        res = await http.get(u, headers: h).timeout(t);
        break;
      case 'DELETE':
        res = await http.delete(u, headers: h).timeout(t);
        break;
      default:
        res = await http.post(u, headers: h, body: jsonEncode(body ?? {})).timeout(t);
    }
  } on ApiError {
    rethrow;
  } catch (_) {
    throw ApiError('Can\u2019t connect. Check your connection.');
  }
  if (res.statusCode != 200) throw _workerErr(res, 'Hosting didn\u2019t work. Please try again.');
  final d = jsonDecode(res.body);
  return d is Map<String, dynamic> ? d : <String, dynamic>{};
}

Future<HostedSites> hostedSitesViaWorker() async {
  final j = await _workerCall('GET', '/host/mine', null, const Duration(seconds: 40));
  final list = (j['sites'] is List) ? (j['sites'] as List).whereType<Map>() : const <Map>[];
  return HostedSites(
    j['limit'] is num ? (j['limit'] as num).toInt() : 2,
    '${j['suffix'] ?? ''}',
    [for (final m in list) HostedSite('${m['name']}', '${m['url']}')],
  );
}

Future<void> deleteHostedSiteViaWorker(String name) async {
  await _workerCall('DELETE', '/host/${Uri.encodeComponent(name)}', null, const Duration(seconds: 40));
}

class _File {
  final String rel;
  final Uint8List data;
  _File(this.rel, this.data);
}

String _ext(String rel) {
  final base = rel.substring(rel.lastIndexOf('/') + 1);
  final i = base.lastIndexOf('.');
  return (i > 0 && i < base.length - 1) ? base.substring(i + 1).toLowerCase() : '';
}

/// Same rules as the server had: publish from the folder where the entry page lives.
List<_File> _collect(Uint8List bytes, bool isZip) {
  if (!isZip) return [_File('index.html', bytes)];
  Archive zip;
  try {
    zip = ZipDecoder().decodeBytes(bytes);
  } catch (_) {
    throw ApiError('That file is not a valid zip.');
  }
  final all = <_File>[];
  for (final f in zip) {
    if (!f.isFile) continue;
    final name = f.name.replaceAll('\\', '/');
    final parts = name.split('/');
    if (name.startsWith('/') || parts.any((p) => p == '..' || p.isEmpty || _skipParts.contains(p))) continue;
    all.add(_File(name, Uint8List.fromList(f.content as List<int>)));
  }
  // entry page: the shallowest index.html, else the shallowest .html
  _File? entry;
  int depth(_File f) => f.rel.split('/').length;
  for (final f in all) {
    final base = f.rel.substring(f.rel.lastIndexOf('/') + 1).toLowerCase();
    if (base == 'index.html' && (entry == null || depth(f) < depth(entry))) entry = f;
  }
  if (entry == null) {
    for (final f in all) {
      if (RegExp(r'\.html?$', caseSensitive: false).hasMatch(f.rel) && (entry == null || depth(f) < depth(entry))) entry = f;
    }
  }
  if (entry == null) throw ApiError('No .html file found in the upload.');
  final cut = entry.rel.lastIndexOf('/');
  final prefix = cut < 0 ? '' : entry.rel.substring(0, cut + 1);
  final out = <_File>[];
  for (final f in all) {
    if (f.rel.startsWith(prefix)) out.add(_File(f.rel.substring(prefix.length), f.data));
  }
  final entryBase = entry.rel.substring(cut + 1);
  if (entryBase.toLowerCase() != 'index.html') out.add(_File('index.html', entry.data));
  return out;
}

Future<Map<String, dynamic>> _cfJwtPost(String path, String jwt, Object body) async {
  http.Response res;
  try {
    res = await http
        .post(Uri.parse('$_cfApi$path'),
            headers: {'Authorization': 'Bearer $jwt', 'Content-Type': 'application/json'}, body: jsonEncode(body))
        .timeout(const Duration(minutes: 4));
  } catch (_) {
    throw ApiError('Can\u2019t connect. Check your connection.');
  }
  Map<String, dynamic> d = {};
  try {
    final x = jsonDecode(res.body);
    if (x is Map<String, dynamic>) d = x;
  } catch (_) {}
  if (res.statusCode != 200 || d['success'] == false) {
    throw ApiError('Hosting didn\u2019t work. Please try again.', status: res.statusCode);
  }
  return d;
}

/// Uploads a .zip or .html and returns the live link. Pass [updateName] to replace one of your sites.
Future<HostedSite> hostSiteViaWorker(String path, String fileName, {String? updateName, String? siteName}) async {
  final lower = fileName.toLowerCase();
  if (!RegExp(r'\.(zip|html?)$').hasMatch(lower)) throw ApiError('Upload a .zip or a single .html file.');
  final bytes = Uint8List.fromList(await File(path).readAsBytes());
  final files = _collect(bytes, lower.endsWith('.zip'));

  // size rules (same as before)
  var total = 0;
  final assets = <_File>[];
  final special = <String, String>{};
  for (final f in files) {
    if (f.data.length > _maxFile) {
      throw ApiError('"${f.rel.substring(f.rel.lastIndexOf('/') + 1)}" is bigger than 25 MB, which is the most Cloudflare Pages allows for one file.');
    }
    total += f.data.length;
    if (total > _maxTotal) throw ApiError('This site is over 100 MB. Hosting is for normal websites.');
    if (files.length > _maxFiles) throw ApiError('Too many files (limit $_maxFiles).');
    if (_special.contains(f.rel)) {
      special[f.rel] = base64Encode(f.data);
    } else if (f.rel == '_worker.js' || f.rel.startsWith('functions/')) {
      continue; // no server code
    } else {
      assets.add(f);
    }
  }
  if (assets.isEmpty) throw ApiError('Nothing to publish in that upload.');

  // 1. name the files the way Cloudflare Pages does
  final b64 = <String, String>{};
  final hashOf = <String, String>{};
  for (final f in assets) {
    final text = base64Encode(f.data);
    final raw = Uint8List.fromList([...text.codeUnits, ..._ext(f.rel).codeUnits]);
    hashOf[f.rel] = blake3Hex32(raw);
    b64[f.rel] = text;
    await Future<void>.delayed(Duration.zero); // keep the screen moving
  }

  // 2. a new site (or an upload pass for an existing one)
  final isUpdate = updateName != null && updateName.isNotEmpty;
  final started = isUpdate
      ? await _workerCall('POST', '/host/token', {'name': updateName})
      : await _workerCall('POST', '/host/new', {if (siteName != null && siteName.isNotEmpty) 'siteName': siteName});
  final name = '${started['name'] ?? updateName}';
  final jwt = '${started['jwt'] ?? ''}';
  if (jwt.isEmpty) throw ApiError('Hosting didn\u2019t work. Please try again.');

  try {
    // 3. upload only what Cloudflare doesn't already have
    final hashes = hashOf.values.toSet().toList();
    final checked = await _cfJwtPost('/pages/assets/check-missing', jwt, {'hashes': hashes});
    final missing = <String>{...((checked['result'] as List?) ?? const []).map((e) => '$e')};
    final seen = <String>{};
    var batch = <Map<String, dynamic>>[];
    var batchBytes = 0;
    Future<void> flush() async {
      if (batch.isEmpty) return;
      await _cfJwtPost('/pages/assets/upload', jwt, batch);
      batch = <Map<String, dynamic>>[];
      batchBytes = 0;
    }

    for (final f in assets) {
      final h = hashOf[f.rel]!;
      if (!missing.contains(h) || !seen.add(h)) continue;
      final text = b64[f.rel]!;
      if (batch.length >= _batchFiles || batchBytes + f.data.length > _batchBytes) await flush();
      batch.add({'key': h, 'value': text, 'metadata': {'contentType': _mime[_ext(f.rel)] ?? 'application/octet-stream'}, 'base64': true});
      batchBytes += f.data.length;
    }
    await flush();
    await _cfJwtPost('/pages/assets/upsert-hashes', jwt, {'hashes': hashes});

    // 4. publish
    final manifest = {for (final f in assets) '/${f.rel}': hashOf[f.rel]};
    final dep = await _workerCall('POST', '/host/deploy', {'name': name, 'manifest': manifest, 'special': special}, const Duration(minutes: 3));
    return HostedSite('${dep['name'] ?? name}', '${dep['url'] ?? 'https://$name.pages.dev'}');
  } catch (_) {
    if (!isUpdate) {
      // don't leave an empty project behind
      unawaited(deleteHostedSiteViaWorker(name).catchError((_) {}));
    }
    rethrow;
  }
}
