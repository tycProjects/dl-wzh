import 'dart:convert';
import 'dart:io';
import 'dart:math' as math;
import 'dart:typed_data';
import 'dart:ui' as ui;
import 'package:archive/archive.dart';
import 'package:flutter/foundation.dart' show compute;
import 'package:flutter/services.dart' show rootBundle;
import 'package:http/http.dart' as http;
import 'package:path_provider/path_provider.dart';
import '../api.dart';
import '../config.dart';
import '../signing_key.dart';
import 'apk_signer.dart';
import 'apk_zip.dart';
import 'axml.dart';

/// Quick build: makes the APK on the phone, no server and no internet.
///
/// It takes the prebuilt offline WebView template (assets/quick/template.apk, bundled with the app),
/// patches its binary manifest (package, name, version, internet permission), swaps the icon, adds
/// the site (or the website link) and a small config, aligns the zip and signs it (v1 + v2) with the
/// user's own key.

class QuickBuildException implements Exception {
  final String message;
  final bool templateMissing;
  QuickBuildException(this.message, {this.templateMissing = false});
  @override
  String toString() => message;
}

// Values baked into the template (see the template's manifest and build.gradle.kts).
const _tplPackage = 'com.template.webapp';
const _tplLabel = '__APP_LABEL__';
const _tplVersionName = '__VERSION_NAME__';
const _tplVersionCode = 1234567;
const _internet = 'android.permission.INTERNET';
const _harmless = 'android.permission.ACCESS_NETWORK_STATE';
const _templateAsset = 'assets/quick/template.apk';

const _javaReserved = {
  'abstract', 'assert', 'boolean', 'break', 'byte', 'case', 'catch', 'char', 'class', 'const', 'continue', 'default',
  'do', 'double', 'else', 'enum', 'extends', 'final', 'finally', 'float', 'for', 'goto', 'if', 'implements', 'import',
  'instanceof', 'int', 'interface', 'long', 'native', 'new', 'package', 'private', 'protected', 'public', 'return',
  'short', 'static', 'strictfp', 'super', 'switch', 'synchronized', 'this', 'throw', 'throws', 'transient', 'try',
  'void', 'volatile', 'while', 'true', 'false', 'null', '_',
};

/// Same naming as the server: a typed suffix becomes com.<suffix>, otherwise com.generated.<name>.
String quickPackageName(String appName, String suffix) {
  String seg(String s) {
    var t = s.toLowerCase().replaceAll(RegExp(r'[^a-z0-9]'), '');
    if (t.isEmpty) return '';
    if (RegExp(r'^[0-9]').hasMatch(t)) t = 'a$t';
    return _javaReserved.contains(t) ? '${t}_' : t;
  }

  String pkg;
  final parts = suffix.trim().isEmpty ? <String>[] : suffix.split('.').map(seg).where((e) => e.isNotEmpty).toList();
  if (parts.isNotEmpty) {
    pkg = 'com.${parts.join('.')}';
  } else {
    final s = seg(appName);
    pkg = 'com.generated.${s.isEmpty ? 'app' : s}';
  }
  // Never collide with the template itself or with this app.
  if (pkg == _tplPackage || pkg == 'com.wzh.builder') pkg = '${pkg}app';
  return pkg;
}

String _safeName(String n) {
  final s = n.replaceAll(RegExp(r'[^A-Za-z0-9._-]+'), '_').replaceAll(RegExp(r'^_+|_+$'), '');
  return s.isEmpty ? 'app' : s;
}

class QuickBuild {
  /// Builds the APK and returns the path it was saved to.
  static Future<String> run(BuildRequest req, {required String appName, void Function(String step)? onStep}) async {
    void step(String s) => onStep?.call(s);

    step('Preparing the template');
    var template = await _cached() ?? await _bundled();
    if (template == null) {
      step('Getting the template (one time only)');
      template = await _download();
    }
    if (template == null) {
      throw QuickBuildException(
          kQuickTemplateRepo.isEmpty
              ? 'Quick build has no template yet.'
              : (lastDownloadError ?? 'Quick build needs to fetch its template once. Connect to the internet and try again.'),
          templateMissing: true);
    }

    var key = SigningKeys.i.current.value;
    if (key == null) {
      step('Making your signing key');
      key = await SigningKeys.i.create();
    }
    final pk8 = await File(key.keyPath).readAsBytes();
    final cert = await File(key.certPath).readAsBytes();

    final url = req.remoteUrl ?? '';
    Uint8List? zip;
    if (url.isEmpty) {
      if (req.zipBytes != null) {
        zip = Uint8List.fromList(req.zipBytes!);
      } else if (req.zipPath != null) {
        zip = await File(req.zipPath!).readAsBytes();
      } else {
        throw QuickBuildException('There is nothing to put in the app.');
      }
    }

    Uint8List? icon;
    if (req.logoPath != null) {
      step('Preparing the icon');
      icon = await _iconPng(req.logoPath!);
    }

    final name = appName.trim().isEmpty ? 'My app' : appName.trim();
    final online = url.isNotEmpty || req.permissions.contains('internet');
    final job = _Job(
      template: template,
      zip: zip,
      pk8: Uint8List.fromList(pk8),
      cert: Uint8List.fromList(cert),
      icon: icon,
      appName: name,
      packageName: quickPackageName(name, req.packageSuffix),
      versionName: '1.0',
      versionCode: DateTime.now().millisecondsSinceEpoch ~/ 1000,
      orientation: req.orientation,
      keepScreenOn: req.permissions.contains('wakeLock'),
      allowInternet: online,
      url: url,
    );

    step('Building and signing');
    final Uint8List apk;
    try {
      apk = await compute(_assemble, job);
    } on FormatException catch (e) {
      throw QuickBuildException(e.message);
    }

    step('Saving');
    Directory? base;
    try {
      base = await getExternalStorageDirectory();
    } catch (_) {}
    base ??= await getApplicationDocumentsDirectory();
    final dir = Directory('${base.path}/Download');
    await dir.create(recursive: true);
    final file = File('${dir.path}/${_safeName(name)}.apk');
    await file.writeAsBytes(apk, flush: true);
    return file.path;
  }


  static Future<File> _templateFile() async {
    final base = await getApplicationDocumentsDirectory();
    final dir = Directory('${base.path}/quick');
    await dir.create(recursive: true);
    return File('${dir.path}/template.apk');
  }

  static Future<Uint8List?> _cached() async {
    try {
      final f = await _templateFile();
      if (await f.exists() && await f.length() > 1000) return await f.readAsBytes();
    } catch (_) {}
    return null;
  }

  static Future<Uint8List?> _bundled() async {
    try {
      final d = await rootBundle.load(_templateAsset);
      if (d.lengthInBytes > 1000) return d.buffer.asUint8List(d.offsetInBytes, d.lengthInBytes);
    } catch (_) {}
    return null;
  }

  /// Downloads HtmlToApk.apk from the latest release and keeps the template that is inside it.
  /// Why the last template download failed (shown to the user so they can see what to fix).
  static String? lastDownloadError;

  static Future<Uint8List?> _download() async {
    lastDownloadError = null;
    if (kQuickTemplateRepo.isEmpty) return null;
    try {
      final res = await http
          .get(Uri.parse('https://github.com/$kQuickTemplateRepo/releases/latest/download/HtmlToApk.apk'))
          .timeout(const Duration(seconds: 120));
      if (res.statusCode == 404) {
        lastDownloadError = 'The latest release of $kQuickTemplateRepo has no file named HtmlToApk.apk (HTTP 404).';
        return null;
      }
      if (res.statusCode != 200) {
        lastDownloadError = 'GitHub answered HTTP ${res.statusCode} for HtmlToApk.apk.';
        return null;
      }
      final t = await compute(_extractTemplate, res.bodyBytes);
      if (t == null) {
        lastDownloadError = 'HtmlToApk.apk downloaded, but it has no valid assets/template.apk inside '
            '(the template must be a zip containing AndroidManifest.xml with the label __APP_LABEL__).';
        return null;
      }
      try {
        await (await _templateFile()).writeAsBytes(t, flush: true);
      } catch (_) {}
      return t;
    } catch (e) {
      lastDownloadError = 'Could not reach GitHub: $e';
      return null;
    }
  }

  /// Quietly fetches the template in the background so Quick build already works offline later.
  static Future<void> prefetch() async {
    if (await _cached() != null || await _bundled() != null) return;
    await _download();
  }

  /// Centre-crops the picked image to a square and saves it as a 512px PNG (a launcher icon).
  static Future<Uint8List?> _iconPng(String path) async {
    try {
      final bytes = await File(path).readAsBytes();
      final codec = await ui.instantiateImageCodec(bytes);
      final img = (await codec.getNextFrame()).image;
      final s = math.min(img.width, img.height).toDouble();
      final src = ui.Rect.fromLTWH((img.width - s) / 2, (img.height - s) / 2, s, s);
      final rec = ui.PictureRecorder();
      final canvas = ui.Canvas(rec);
      canvas.drawImageRect(img, src, const ui.Rect.fromLTWH(0, 0, 512, 512), ui.Paint()..filterQuality = ui.FilterQuality.high);
      final out = await rec.endRecording().toImage(512, 512);
      final data = await out.toByteData(format: ui.ImageByteFormat.png);
      return data?.buffer.asUint8List();
    } catch (_) {
      return null; // keep the template's own icon
    }
  }
}


/// Runs in a background isolate: pulls assets/template.apk out of HtmlToApk.apk and checks it.
Uint8List? _extractTemplate(Uint8List apk) {
  for (final f in ZipDecoder().decodeBytes(apk).files) {
    if (!f.isFile || f.name != 'assets/template.apk') continue;
    final t = Uint8List.fromList(f.content as List<int>);
    for (final e in ZipDecoder().decodeBytes(t).files) {
      if (e.isFile && e.name == 'AndroidManifest.xml') {
        final text = latin1.decode(Uint8List.fromList(e.content as List<int>), allowInvalid: true);
        return text.contains(_tplLabel) ? t : null;
      }
    }
  }
  return null;
}

class _Job {
  final Uint8List template, pk8, cert;
  final Uint8List? zip, icon;
  final String appName, packageName, versionName, orientation, url;
  final int versionCode;
  final bool keepScreenOn, allowInternet;
  const _Job({
    required this.template,
    required this.zip,
    required this.pk8,
    required this.cert,
    required this.icon,
    required this.appName,
    required this.packageName,
    required this.versionName,
    required this.versionCode,
    required this.orientation,
    required this.keepScreenOn,
    required this.allowInternet,
    required this.url,
  });
}

final _mediaExt = RegExp(r'\.(png|jpe?g|gif|webp|avif|mp3|m4a|ogg|opus|mp4|webm|ogv|woff2?|zip|gz|7z)$', caseSensitive: false);

/// Runs in a background isolate.
Uint8List _assemble(_Job j) {
  // 1. the template's files (its old signature is dropped)
  final entries = <String, Uint8List>{};
  for (final f in ZipDecoder().decodeBytes(j.template).files) {
    if (!f.isFile) continue;
    if (f.name.toUpperCase().startsWith('META-INF/')) continue;
    entries[f.name] = Uint8List.fromList(f.content as List<int>);
  }

  // 2. manifest: package, name, version, internet permission
  final manifest = entries['AndroidManifest.xml'];
  if (manifest == null) throw const FormatException('The Quick template has no AndroidManifest.xml.');
  final strings = <String, String>{
    _tplPackage: j.packageName,
    _tplLabel: j.appName,
    _tplVersionName: j.versionName,
  };
  if (!j.allowInternet) strings[_internet] = _harmless;
  entries['AndroidManifest.xml'] = axmlPatch(manifest, strings, _tplVersionCode, j.versionCode);

  // 3. icon
  if (j.icon != null) {
    final iconRe = RegExp(r'^res/drawable-nodpi[^/]*/ic_launcher\.png$');
    String? iconName;
    for (final n in entries.keys) {
      if (iconRe.hasMatch(n)) {
        iconName = n;
        break;
      }
    }
    if (iconName == null) throw const FormatException('The Quick template has no icon to replace.');
    entries[iconName] = j.icon!;
  }

  // 4. settings the template reads when it starts
  entries['assets/config.json'] = Uint8List.fromList(utf8.encode(jsonEncode({
    'orientation': j.orientation,
    'fullscreen': false,
    'keepScreenOn': j.keepScreenOn,
    'allowInternet': j.allowInternet,
    'url': j.url,
  })));

  // 5. the site (website-link apps just open the link, so they bundle nothing)
  if (j.url.isEmpty) {
    final files = <String, Uint8List>{};
    var total = 0;
    for (final f in ZipDecoder().decodeBytes(j.zip!).files) {
      if (!f.isFile) continue;
      var n = f.name.replaceAll('\\', '/');
      while (n.startsWith('/')) {
        n = n.substring(1);
      }
      if (n.isEmpty || n.startsWith('__MACOSX/') || n.endsWith('.DS_Store') || n.split('/').contains('..')) continue;
      final bytes = Uint8List.fromList(f.content as List<int>);
      total += bytes.length;
      if (total > 150 * 1024 * 1024) throw const FormatException('That site is too big for a Quick build. Use Full build instead.');
      files[n] = bytes;
    }
    // index.html may sit inside one wrapper folder
    var prefix = '';
    for (var i = 0; i < 5 && !files.containsKey('${prefix}index.html'); i++) {
      final tops = <String>{};
      for (final k in files.keys) {
        if (k.startsWith(prefix)) tops.add(k.substring(prefix.length).split('/').first);
      }
      if (tops.length != 1) break;
      final t = tops.first;
      if (!files.keys.any((k) => k.startsWith('$prefix$t/'))) break; // a lone file, not a folder
      prefix = '$prefix$t/';
    }
    if (!files.containsKey('${prefix}index.html')) {
      throw const FormatException('Your site needs an index.html at the top.');
    }
    final keys = files.keys.where((k) => k.startsWith(prefix)).toList()..sort();
    for (final k in keys) {
      entries['assets/www/${k.substring(prefix.length)}'] = files[k]!;
    }
  }

  // 6. zip (resources.arsc uncompressed and aligned, as Android 11+ needs) and sign
  final list = <ZipFileData>[
    for (final e in entries.entries)
      ZipFileData(e.key, e.value, stored: e.key == 'resources.arsc' || _mediaExt.hasMatch(e.key)),
  ];
  return signApk(list, j.pk8, j.cert);
}
