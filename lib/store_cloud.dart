import 'dart:async';
import 'dart:convert';
import 'dart:io';
import 'dart:math' as math;
import 'dart:typed_data';
import 'dart:ui' as ui;
import 'package:crypto/crypto.dart';
import 'package:http/http.dart' as http;
import 'package:image/image.dart' as imglib;
import 'package:path_provider/path_provider.dart';
import 'api.dart';
import 'apk_info.dart';
import 'chat_cloud.dart' show kSupabaseUrl, kSupabaseAnonKey, kStoreFilesUrl, kStoreFilesAnonKey;
import 'store_auth.dart';

/// True when new Store files go to the second Supabase project (both values set in chat_cloud.dart).
bool get _newFiles => kStoreFilesUrl.startsWith('https://') && kStoreFilesAnonKey.length > 20;

/// Files uploaded to the second project are saved in the database as 'n/<path>'. Older files keep their plain
/// path and are still read from the main project, so nothing that already exists breaks.
String _tag(String obj) => _newFiles ? 'n/$obj' : obj;

/// Public link of a Store file, from whichever project holds it.
String storeFileUrl(String p) => p.startsWith('n/') && kStoreFilesUrl.startsWith('https://')
    ? '${kStoreFilesUrl.replaceAll(RegExp(r'/+$'), '')}/storage/v1/object/public/store/${p.substring(2)}'
    : '$kSupabaseUrl/storage/v1/object/public/store/$p';

const kStoreMaxApps = 2; // apps one phone can have in the Store (keep in sync with supabase/store.sql)
const kStoreMaxMb = 25; // store upload cap per APK (keep in sync with supabase/store.sql)
const kStoreMaxShots = 4; // screenshots per app (each is a ~60-120 KB JPEG, keep in sync with supabase/store_shots.sql)
const kStoreCats = ['Tools', 'Games', 'Social', 'Media', 'Education', 'Other'];

class StoreApp {
  final String id, name, about, cat, pkg, version, apkPath, logoPath, status;
  final List<String> shots; // screenshot paths in the store bucket (empty = none)
  final int size, downloads, ratings, vcode; // vcode = Android versionCode of the APK in the Store (0 = unknown)
  final double rating;
  final DateTime? created, updated;
  StoreApp(Map<String, dynamic> j)
      : id = '${j['id']}',
        name = '${j['name']}',
        about = '${j['about'] ?? ''}',
        cat = '${j['cat'] ?? 'Other'}',
        pkg = '${j['pkg'] ?? ''}',
        version = '${j['version'] ?? ''}',
        vcode = (j['version_code'] as num?)?.toInt() ?? 0,
        apkPath = '${j['apk_path']}',
        logoPath = '${j['logo_path'] ?? ''}',
        shots = [for (final e in (j['shots'] as List? ?? const [])) if ('$e'.isNotEmpty) '$e'],
        status = '${j['status'] ?? 'pending'}',
        size = (j['size_bytes'] as num?)?.toInt() ?? 0,
        downloads = (j['downloads'] as num?)?.toInt() ?? 0,
        rating = double.tryParse('${j['rating_avg'] ?? 0}') ?? 0,
        ratings = (j['rating_count'] as num?)?.toInt() ?? 0,
        created = DateTime.tryParse('${j['created_at'] ?? ''}'),
        updated = DateTime.tryParse('${j['updated_at'] ?? ''}');


  static String _pub(String p) => storeFileUrl(p);
  String get apkUrl => _pub(apkPath);
  String get logoUrl => logoPath.isEmpty ? '' : _pub(logoPath);
  List<String> get shotUrls => [for (final p in shots) _pub(p)];
}

class StoreReview {
  final String id, name, body;
  final int stars;
  final bool mine;
  final DateTime? created;
  final String reply; // the app owner's answer ('' = none)
  final String avatarPath; // the reviewer's profile photo in the store bucket ('' = none, the default silhouette shows)
  final DateTime? replyAt;
  String get avatarUrl => StoreCloud.avatarUrl(avatarPath);
  StoreReview(Map<String, dynamic> j)
      : id = '${j['id']}',
        name = '${j['name'] ?? 'WZH user'}',
        body = '${j['body'] ?? ''}',
        stars = ((j['stars'] as num?)?.toInt() ?? 0).clamp(0, 5),
        mine = j['mine'] == true,
        created = DateTime.tryParse('${j['created_at'] ?? ''}'),
        reply = '${j['reply'] ?? ''}',
        avatarPath = '${j['avatar'] ?? ''}',
        replyAt = DateTime.tryParse('${j['reply_at'] ?? ''}');
}

/// The Store lives in Supabase: a table (store_apps) with an approval status, and a public
/// Storage bucket (store) for the APKs and logos. The app only talks to small SQL functions.
class StoreCloud {
  /// This phone's old id (used before Google sign-in existed).
  static String get deviceOwner => sha256.convert(utf8.encode('wzh-store:${Api.i.clientId}')).toString().substring(0, 32);
  /// Who owns the apps: the Google account when signed in, otherwise this phone's id.
  static String get owner => StoreAuth.uid ?? deviceOwner;

  /// Headers for every call: the signed-in user's token when there is one, else the public key.
  static Future<Map<String, String>> _hdr() async {
    final t = await StoreAuth.token();
    return {
      'apikey': kSupabaseAnonKey,
      if (t != null) 'Authorization': 'Bearer $t' else if (kSupabaseAnonKey.startsWith('eyJ')) 'Authorization': 'Bearer $kSupabaseAnonKey',
    };
  }

  /// After the first sign-in: moves apps made on this phone before login to the account.
  static Future<void> claimOld() async {
    try {
      await _rpc('store_claim', {'p_owner': deviceOwner});
    } catch (_) {}
  }

  static Future<dynamic> _rpc(String fn, Map<String, dynamic> body) async {
    try {
      final r = await http
          .post(Uri.parse('$kSupabaseUrl/rest/v1/rpc/$fn'), headers: {...await _hdr(), 'Content-Type': 'application/json'}, body: jsonEncode(body))
          .timeout(const Duration(seconds: 20));
      if (r.statusCode >= 200 && r.statusCode < 300) return r.body.isEmpty ? null : jsonDecode(r.body);
      var msg = r.statusCode == 300
          ? 'Store setup is out of date (two copies of a function). Run store_update.sql in Supabase again.'
          : 'Store error (${r.statusCode}).';
      try {
        final m = (jsonDecode(r.body) as Map)['message'];
        if (m is String && m.isNotEmpty && !m.contains('function')) msg = m;
      } catch (_) {}
      throw ApiError(msg, status: r.statusCode);
    } on SocketException {
      throw ApiError('Can\u2019t connect. Check your connection.');
    }
  }

  static List<StoreApp> _apps(dynamic j) => [for (final e in (j as List? ?? const [])) StoreApp(Map<String, dynamic>.from(e as Map))];

  static Future<List<StoreApp>> list([String q = '']) async => _apps(await _rpc('store_list', {'p_q': q}));
  static Future<List<StoreApp>> mine() async => _apps(await _rpc('store_mine', {'p_owner': owner}));
  static Future<List<StoreReview>> reviews(String appId) async {
    final j = await _rpc('store_reviews_list', {'p_app': appId, 'p_owner': owner});
    return [for (final e in (j as List? ?? const [])) StoreReview(Map<String, dynamic>.from(e as Map))];
  }

  // ---- profile photo (shown on this person's reviews; with none, a fixed silhouette is drawn) ----
  static String avatarUrl(String p) => p.isEmpty ? '' : storeFileUrl(p);
  static String get _avatarKey => 'storeAvatar_$owner';

  /// This account's photo path: what this phone saved last, refreshed from the server when it can.
  static String get myAvatarPath => Api.i.prefs.getString(_avatarKey) ?? '';
  static Future<String> refreshAvatar() async {
    try {
      final j = await _rpc('store_profile_get', {'p_owner': owner});
      final p = j is String ? j : '';
      final before = myAvatarPath;
      if (p.isEmpty) {
        await Api.i.prefs.remove(_avatarKey);
      } else {
        await Api.i.prefs.setString(_avatarKey, p);
      }
      if (before != p) StoreAuth.changes.value++;
      return p;
    } catch (_) {
      return myAvatarPath;
    }
  }

  /// Opens a picture the cheap way: the phone's own decoder shrinks it while reading, so even a huge photo
  /// never gets unpacked at full size (decoding it by hand ran the app out of memory and it closed).
  static Future<(ui.Image, int, int)> _openSmall(String path, int target, {required bool cover}) async {
    final f = File(path);
    if (await f.length() > 40 * 1048576) throw ApiError('That picture is too big. Pick a smaller one.');
    ui.ImmutableBuffer? buf;
    ui.ImageDescriptor? desc;
    ui.Codec? codec;
    try {
      buf = await ui.ImmutableBuffer.fromUint8List(await f.readAsBytes());
      desc = await ui.ImageDescriptor.encoded(buf);
      final w = desc.width, h = desc.height;
      if (w < 1 || h < 1) throw ApiError('That picture can\u2019t be opened. Try another one.');
      final basis = cover ? math.min(w, h) : math.max(w, h);
      final k = basis > target ? target / basis : 1.0;
      codec = await desc.instantiateCodec(targetWidth: math.max(1, (w * k).round()), targetHeight: math.max(1, (h * k).round()));
      final img = (await codec.getNextFrame()).image;
      return (img, img.width, img.height);
    } on ApiError {
      rethrow;
    } catch (_) {
      throw ApiError('That picture can\u2019t be opened. Try another one.');
    } finally {
      codec?.dispose();
      desc?.dispose();
      buf?.dispose();
    }
  }

  /// A small PNG of the picture, longest side 256 px.
  static Future<List<int>> _logoBytes(String path) async {
    final (img, _, _) = await _openSmall(path, 256, cover: false);
    final data = await img.toByteData(format: ui.ImageByteFormat.png);
    img.dispose();
    if (data == null) throw ApiError('That picture can\u2019t be opened. Try another one.');
    return data.buffer.asUint8List();
  }

  /// A 256 px square PNG, cropped from the middle.
  static Future<List<int>> _squareBytes(String path) async {
    final (src, w, h) = await _openSmall(path, 256, cover: true);
    final side = math.min(w, h).toDouble();
    final rec = ui.PictureRecorder();
    ui.Canvas(rec).drawImageRect(
      src,
      ui.Rect.fromLTWH((w - side) / 2, (h - side) / 2, side, side),
      const ui.Rect.fromLTWH(0, 0, 256, 256),
      ui.Paint()..filterQuality = ui.FilterQuality.high,
    );
    final out = await rec.endRecording().toImage(256, 256);
    final data = await out.toByteData(format: ui.ImageByteFormat.png);
    src.dispose();
    out.dispose();
    if (data == null) throw ApiError('That picture can\u2019t be opened. Try another one.');
    return data.buffer.asUint8List();
  }

  /// Crops the picture to a square, shrinks it to 256 px, uploads it and makes it this account's photo.
  static Future<void> setAvatar(String imagePath) async {
    final bytes = await _squareBytes(imagePath);
    final obj = 'profiles/$owner/${DateTime.now().millisecondsSinceEpoch}.png';
    await _put(obj, Stream.value(bytes), bytes.length, 'image/png', null);
    await _rpc('store_profile_save', {'p_owner': owner, 'p_avatar': _tag(obj)});
    await Api.i.prefs.setString(_avatarKey, _tag(obj));
    StoreAuth.changes.value++;
  }

  /// Back to the default silhouette.
  static Future<void> removeAvatar() async {
    await _rpc('store_profile_save', {'p_owner': owner, 'p_avatar': ''});
    await Api.i.prefs.remove(_avatarKey);
    StoreAuth.changes.value++;
  }

  static String get reviewerName => Api.i.prefs.getString('storeReviewName') ?? '';

  static Future<void> saveReview(String appId, int stars, String body, String name) async {
    await _rpc('store_review_save', {'p_owner': owner, 'p_app': appId, 'p_stars': stars, 'p_body': body, 'p_name': name});
    try {
      await Api.i.prefs.setString('storeReviewName', name.trim());
    } catch (_) {}
  }

  /// The app's owner answers a review. An empty [body] removes the reply.
  static Future<void> replyReview(String reviewId, String body) => _rpc('store_review_reply', {'p_owner': owner, 'p_review': reviewId, 'p_body': body});

  static Future<void> deleteReview(String appId) => _rpc('store_review_delete', {'p_owner': owner, 'p_app': appId});

  static Future<void> delete(String id) => _rpc('store_delete', {'p_owner': owner, 'p_id': id});
  static Future<void> hit(String id) async {
    try {
      await _rpc('store_hit', {'p_id': id});
    } catch (_) {}
  }

  static Future<void> _put(String objPath, Stream<List<int>> data, int total, String type, void Function(int, int)? onProgress) async {
    final base = _newFiles ? kStoreFilesUrl.replaceAll(RegExp(r'/+$'), '') : kSupabaseUrl;
    final req = http.StreamedRequest('POST', Uri.parse('$base/storage/v1/object/store/$objPath'));
    // The second project has its own accounts, so uploads there use its public key (the folder rules in its SQL still apply).
    final auth = _newFiles ? {'apikey': kStoreFilesAnonKey, 'Authorization': 'Bearer $kStoreFilesAnonKey'} : await _hdr();
    req.headers.addAll({...auth, 'Content-Type': type});
    req.contentLength = total;
    var sent = 0, lastPct = -1;
    final feed = req.sink.addStream(data.map((c) {
      sent += c.length;
      final pct = total > 0 ? sent * 100 ~/ total : 0;
      if (pct != lastPct) { // redraw once per percent, not once per 64 KB chunk
        lastPct = pct;
        onProgress?.call(sent, total);
      }
      return c;
    })).then((_) => req.sink.close());
    final res = await req.send().timeout(const Duration(minutes: 20));
    await feed;
    final body = await res.stream.bytesToString();
    if (res.statusCode != 200) {
      // Supabase says why in the body (bucket missing, policy, bad token...). Show it so the cause is visible.
      var why = '';
      try {
        final m = jsonDecode(body) as Map;
        why = '${m['message'] ?? m['error'] ?? ''}'.trim();
      } catch (_) {}
      throw ApiError(why.isEmpty ? 'Upload failed (${res.statusCode}).' : 'Upload failed (${res.statusCode}): $why');
    }
  }


  /// A screenshot as a small JPEG: longest side 960 px, quality 72 (about 60-120 KB instead of a 2 MB+ PNG).
  /// The phone's own decoder shrinks it while reading, so a full-size screenshot is never unpacked.
  static Future<List<int>> _shotBytes(String path) async {
    final (src, w, h) = await _openSmall(path, 960, cover: false);
    final data = await src.toByteData(format: ui.ImageByteFormat.rawRgba);
    src.dispose();
    if (data == null) throw ApiError('That picture can\u2019t be opened. Try another one.');
    final im = imglib.Image.fromBytes(width: w, height: h, bytes: data.buffer, numChannels: 4);
    return imglib.encodeJpg(im, quality: 72);
  }

  /// Uploads the chosen screenshots next to the APK and returns their paths (one bad picture is skipped; all failing stops the publish).
  static Future<List<String>> _putShots(List<String> paths, String dir) async {
    final out = <String>[];
    Object? firstError;
    for (final p in paths.take(kStoreMaxShots)) {
      try {
        final b = await _shotBytes(p);
        final obj = '$dir/shot${out.length + 1}.jpg';
        await _put(obj, Stream.value(b), b.length, 'image/jpeg', null);
        out.add(_tag(obj));
      } catch (e) {
        firstError ??= e;
      }
    }
    if (out.isEmpty && firstError != null) {
      final m = firstError is ApiError ? firstError.message : '';
      throw ApiError(m.contains('mime') || m.contains('403') || m.contains('policy')
          ? 'Screenshots need the new Store setup. Run supabase/store_shots.sql in Supabase once.'
          : (m.isEmpty ? 'Couldn\u2019t upload the screenshots. Try again.' : m));
    }
    return out;
  }

  /// A missing p_shots parameter means store_shots.sql was not run yet.
  static Future<dynamic> _rpcShots(String fn, Map<String, dynamic> body, List<String> shots) async {
    if (shots.isEmpty) return _rpc(fn, body);
    try {
      return await _rpc(fn, {...body, 'p_shots': shots});
    } on ApiError catch (e) {
      if (e.status == 404) throw ApiError('Screenshots need the new Store setup. Run supabase/store_shots.sql in Supabase once.', status: 404);
      rethrow;
    }
  }

  /// Uploads the APK (and a small logo) and files the app as "pending" until the admin approves it.
  static Future<void> publish(String apkPath, {required String name, required String about, required String cat, String? logoPath, List<String> shotPaths = const [], void Function(int, int)? onProgress}) async {
    final f = File(apkPath);
    final total = await f.length();
    if (total > kStoreMaxMb * 1048576) throw ApiError('That APK is over $kStoreMaxMb MB.');
    final head = await f.openRead(0, 2).expand((b) => b).toList();
    if (head.length < 2 || head[0] != 0x50 || head[1] != 0x4b) throw ApiError('That file is not a valid APK.');
    if (logoPath == null) throw ApiError('Add an app icon first. Apps can\u2019t be published without one.');
    if (shotPaths.isEmpty) throw ApiError('Add at least one screenshot first. Apps can\u2019t be published without one.');
    final info = await readApkInfo(apkPath);
    List<int>? logo;
    if (logoPath != null) {
      try {
        logo = await _logoBytes(logoPath);
      } on ApiError {
        rethrow;
      }
    }
    final o = owner;
    final dir = '$o/${DateTime.now().millisecondsSinceEpoch}';
    final apk = '$dir/app.apk';
    var logoObj = '';
    if (logo != null) {
      logoObj = '$dir/logo.png';
      await _put(logoObj, Stream.value(logo), logo.length, 'image/png', null); // a failed icon stops the publish
    }
    final shots = await _putShots(shotPaths, dir); // small files first, so a problem shows before the big APK upload
    await _put(apk, f.openRead(), total, 'application/vnd.android.package-archive', onProgress);
    final id = await _rpcShots('store_publish', {
      'p_owner': o, 'p_name': name, 'p_about': about, 'p_cat': cat,
      'p_pkg': info?.pkg ?? '', 'p_version': info?.versionName ?? '', 'p_size': total, 'p_apk': _tag(apk), 'p_logo': logoObj.isEmpty ? '' : _tag(logoObj),
      'p_vcode': info?.versionCode ?? 0,
    }, shots);
    // Tells the admin's Telegram bot (with Approve / Reject buttons). A courtesy call: if it fails the upload still counts.
    try {
      await http
          .post(Uri.parse('$kSupabaseUrl/functions/v1/store-notify'), headers: {...await _hdr(), 'Content-Type': 'application/json'}, body: jsonEncode({'id': '$id'}))
          .timeout(const Duration(seconds: 15));
    } catch (_) {}
  }

  static bool _newerVersion(String a, String b) {
    List<int> parts(String s) => RegExp(r'\d+').allMatches(s).map((m) => int.parse(m.group(0)!)).toList();
    final x = parts(a), y = parts(b);
    for (var i = 0; i < (x.length > y.length ? x.length : y.length); i++) {
      final p = i < x.length ? x[i] : 0, q = i < y.length ? y[i] : 0;
      if (p != q) return p > q;
    }
    return false;
  }

  /// Uploads a new APK for an app the person already published. Same package name, higher version.
  /// A live app stays live and serves the new file straight away (people who installed it see Update).
  static Future<void> update(StoreApp app, String apkPath, {required String name, required String about, required String cat, String? logoPath, List<String> shotPaths = const [], void Function(int, int)? onProgress}) async {
    final f = File(apkPath);
    final total = await f.length();
    if (total > kStoreMaxMb * 1048576) throw ApiError('That APK is over $kStoreMaxMb MB.');
    final head = await f.openRead(0, 2).expand((b) => b).toList();
    if (head.length < 2 || head[0] != 0x50 || head[1] != 0x4b) throw ApiError('That file is not a valid APK.');
    if (logoPath == null && app.logoPath.isEmpty) throw ApiError('Add an app icon first. Apps can\u2019t be updated without one.');
    if (shotPaths.isEmpty && app.shots.isEmpty) throw ApiError('Add at least one screenshot first. Apps can\u2019t be updated without one.');
    final info = await readApkInfo(apkPath);
    if (info != null && app.pkg.isNotEmpty && info.pkg.isNotEmpty && info.pkg != app.pkg) {
      throw ApiError('This APK is a different app (${info.pkg}). An update must keep the package name ${app.pkg}.');
    }
    if (info != null && app.vcode > 0 && info.versionCode > 0) {
      // Android only installs an update over an installed app when the versionCode is higher.
      if (info.versionCode <= app.vcode) {
        throw ApiError('The new versionCode (${info.versionCode}) must be higher than the current one (${app.vcode}), or phones that have the app cannot update it.');
      }
    } else if (info != null && app.version.isNotEmpty && info.versionName.isNotEmpty && !_newerVersion(info.versionName, app.version)) {
      throw ApiError('The new version (v${info.versionName}) must be higher than the current one (v${app.version}).');
    }
    List<int>? logo;
    if (logoPath != null) {
      try {
        logo = await _logoBytes(logoPath);
      } on ApiError {
        rethrow;
      }
    }
    final dir = '$owner/${DateTime.now().millisecondsSinceEpoch}';
    final apk = '$dir/app.apk';
    var logoObj = '';
    if (logo != null) {
      logoObj = '$dir/logo.png';
      await _put(logoObj, Stream.value(logo), logo.length, 'image/png', null); // a failed icon stops the publish
    }
    final shots = await _putShots(shotPaths, dir); // small files first, so a problem shows before the big APK upload
    await _put(apk, f.openRead(), total, 'application/vnd.android.package-archive', onProgress);
    await _rpcShots('store_update', {
      'p_owner': owner, 'p_id': app.id, 'p_name': name, 'p_about': about, 'p_cat': cat,
      'p_pkg': info?.pkg ?? '', 'p_version': info?.versionName ?? '', 'p_size': total, 'p_apk': _tag(apk), 'p_logo': logoObj.isEmpty ? '' : _tag(logoObj),
      'p_vcode': info?.versionCode ?? 0,
    }, shots);
  }

  /// Path of an APK that was already downloaded completely (same app, same size), or null.
  /// Lets Install open the installer right away instead of downloading the same file again.
  static Future<String?> cached(StoreApp a) async {
    if (a.size <= 0) return null;
    final dir = await getTemporaryDirectory();
    final safe = a.name.replaceAll(RegExp(r'[^A-Za-z0-9_-]+'), '-');
    final f = File('${dir.path}/store_${safe}_${a.id}_${a.size}_${a.updated?.millisecondsSinceEpoch ?? 0}.apk');
    try {
      if (await f.exists() && await f.length() == a.size) return f.path;
    } catch (_) {}
    return null;
  }

  /// Downloads the APK into the temp folder and returns its path (the caller opens the installer).
  ///
  /// The download RESUMES: bytes are written to a ".part" file and, if the connection drops or the app
  /// was in the background (Android pauses its network), the next try continues from the bytes already
  /// saved (HTTP Range) instead of starting from 0%. Tapping Install again after a failure resumes too.
  static Future<String> download(StoreApp a, void Function(int, int) onProgress) async {
    final dir = await getTemporaryDirectory();
    final safe = a.name.replaceAll(RegExp(r'[^A-Za-z0-9_-]+'), '-');
    final out = File('${dir.path}/store_${safe}_${a.id}_${a.size}_${a.updated?.millisecondsSinceEpoch ?? 0}.apk');
    // The id and size are in the name, so a changed/updated APK never continues an old partial file.
    final part = File('${dir.path}/store_${safe}_${a.id}_${a.size}_${a.updated?.millisecondsSinceEpoch ?? 0}.part');
    var total = a.size;
    var idle = 0; // tries in a row that made no progress
    while (true) {
      var have = await part.exists() ? await part.length() : 0;
      if (total > 0 && have > total) {
        await part.delete();
        have = 0;
      }
      if (total > 0 && have == total) break; // everything is already here
      final client = http.Client();
      IOSink? sink;
      try {
        final req = http.Request('GET', Uri.parse(a.apkUrl));
        if (have > 0) req.headers['Range'] = 'bytes=$have-';
        final res = await client.send(req).timeout(const Duration(seconds: 25));
        if (res.statusCode == 416) {
          // The server says our start is past the end: the part file is bad. Start clean.
          await part.delete().catchError((_) => part);
          throw const SocketException('range');
        }
        if (res.statusCode != 200 && res.statusCode != 206) throw ApiError('Download failed (${res.statusCode}).');
        if (res.statusCode == 200 && have > 0) {
          // The server ignored Range and sends the whole file again.
          have = 0;
          await part.delete().catchError((_) => part);
        }
        final cr = RegExp(r'/(\d+)\s*$').firstMatch(res.headers['content-range'] ?? '');
        if (cr != null) {
          total = int.parse(cr.group(1)!);
        } else if (res.contentLength != null && res.contentLength! > 0) {
          total = have + res.contentLength!;
        }
        sink = part.openWrite(mode: have > 0 ? FileMode.append : FileMode.write);
        var got = have;
        onProgress(got, total);
        // If nothing arrives for 20 s (typical right after coming back from another app), stop and resume.
        await for (final c in res.stream.timeout(const Duration(seconds: 20))) {
          sink.add(c);
          got += c.length;
          onProgress(got, total);
        }
        await sink.flush();
        await sink.close();
        sink = null;
        if (total > 0 && got < total) throw const SocketException('cut short');
        break;
      } on SocketException {
        idle = await _keepPart(sink, part, have, idle);
      } on TimeoutException {
        idle = await _keepPart(sink, part, have, idle);
      } on http.ClientException {
        idle = await _keepPart(sink, part, have, idle);
      } on HandshakeException {
        idle = await _keepPart(sink, part, have, idle);
      } finally {
        client.close();
      }
    }
    if (await out.exists()) await out.delete();
    await part.rename(out.path);
    // Old copies of this app (earlier versions) are no longer needed.
    try {
      await for (final e in dir.list()) {
        final n = e.path.split('/').last;
        if (e is File && n.startsWith('store_${safe}_') && n.endsWith('.apk') && e.path != out.path) await e.delete();
      }
    } catch (_) {}
    return out.path;
  }

  /// Saves what arrived so far and decides whether to try again (up to 8 tries in a row without progress).
  static Future<int> _keepPart(IOSink? sink, File part, int startedAt, int idle) async {
    try {
      await sink?.flush();
      await sink?.close();
    } catch (_) {}
    final now = await part.exists() ? await part.length() : 0;
    final next = now > startedAt ? 0 : idle + 1;
    if (next >= 8) throw ApiError('Download paused. Check your connection, then tap Install to continue.');
    await Future.delayed(Duration(milliseconds: 600 + next * 700));
    return next;
  }
}
