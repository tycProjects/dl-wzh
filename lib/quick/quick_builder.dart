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
import 'aapt_build.dart';
import 'apk_signer.dart';
import 'apk_zip.dart';
import 'arsc.dart';
import 'axml.dart';
import 'quick_protect.dart';

/// Quick build: makes the APK on the phone, no server and no internet.
///
/// It takes the prebuilt offline WebView template (assets/quick/template.apk, bundled with the app),
/// patches its binary manifest (package, name, version, orientation, permissions), swaps the icon, adds
/// the site (or the website link + cache mode in assets/wzh_quick.json), aligns the zip and signs it
/// (v1 + v2) with the user's own key. The template is the server's own wrapper, built with
/// "Build the Quick template" (app name __APP_LABEL__, package suffix template.webapp).

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
const _tplVersionName = '1.0';
const _tplScheme = 'template.webapp'; // the deep-link scheme (package without "com.")
const _internet = 'android.permission.INTERNET';
const _harmless = 'android.permission.ACCESS_NETWORK_STATE';

/// Optional permissions the Quick template declares (same ids as the server's permission picker).
/// The ones the user did not pick are switched off by renaming them to a harmless permission.
const _optionalPerms = <String, List<String>>{
  'camera': ['android.permission.CAMERA'],
  'microphone': ['android.permission.RECORD_AUDIO'],
  'locationFine': ['android.permission.ACCESS_FINE_LOCATION'],
  'locationCoarse': ['android.permission.ACCESS_COARSE_LOCATION'],
  'locationBackground': ['android.permission.ACCESS_BACKGROUND_LOCATION'],
  'storageLegacy': ['android.permission.WRITE_EXTERNAL_STORAGE'],
  'mediaImages': ['android.permission.READ_MEDIA_IMAGES'],
  'mediaVideo': ['android.permission.READ_MEDIA_VIDEO'],
  'mediaAudio': ['android.permission.READ_MEDIA_AUDIO'],
  'contactsRead': ['android.permission.READ_CONTACTS'],
  'contactsWrite': ['android.permission.WRITE_CONTACTS'],
  'calendarRead': ['android.permission.READ_CALENDAR'],
  'calendarWrite': ['android.permission.WRITE_CALENDAR'],
  'smsRead': ['android.permission.READ_SMS'],
  'smsSend': ['android.permission.SEND_SMS'],
  'callPhone': ['android.permission.CALL_PHONE'],
  'phoneState': ['android.permission.READ_PHONE_STATE'],
  'callLog': ['android.permission.READ_CALL_LOG'],
  'bluetooth': [
    'android.permission.BLUETOOTH',
    'android.permission.BLUETOOTH_ADMIN',
    'android.permission.BLUETOOTH_CONNECT',
    'android.permission.BLUETOOTH_SCAN',
  ],
  'nearbyWifi': ['android.permission.NEARBY_WIFI_DEVICES'],
  'bodySensors': ['android.permission.BODY_SENSORS'],
  'activityRecognition': ['android.permission.ACTIVITY_RECOGNITION'],
  'vibrate': ['android.permission.VIBRATE'],
};

/// Same deep-link scheme the server writes: the package without its leading "com.generated." / "com.".
String _schemeOf(String pkg) {
  if (pkg.startsWith('com.generated.')) return pkg.substring('com.generated.'.length);
  if (pkg.startsWith('com.')) return pkg.substring(4);
  return pkg;
}
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
  // Never collide with the template itself.
  if (pkg == _tplPackage) pkg = '${pkg}app';
  return pkg;
}

String _safeName(String n) {
  final s = n.replaceAll(RegExp(r'[^A-Za-z0-9._-]+'), '_').replaceAll(RegExp(r'^_+|_+$'), '');
  return s.isEmpty ? 'app' : s;
}

class QuickBuild {
  /// Which engine made the manifest of the last build: 'aapt2' or 'patch' (with the reason aapt2 was skipped).
  static String lastEngine = '';

  /// Builds the APK and returns the path it was saved to.
  static Future<String> run(BuildRequest req, {required String appName, void Function(String step)? onStep}) async {
    void step(String s) => onStep?.call(s);

    step('Preparing the template');
    var template = await _bundled() ?? await _cached();
    if (template == null) {
      step('Getting the template (one time only)');
      template = await _download();
    }
    if (template == null) {
      throw QuickBuildException(
          kQuickTemplateRepo.isEmpty
              ? 'Quick build has no template yet.'
              : 'Quick build needs to grab its template once. Connect to the internet and try again.',
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

    Uint8List? icon, iconBg;
    if (req.logoPath != null) {
      step('Preparing the icon');
      final ic = await _iconPngs(req.logoPath!);
      icon = ic?.$1;
      iconBg = ic?.$2;
    }

    final name = appName.trim().isEmpty ? 'My app' : appName.trim();
    final online = url.isNotEmpty || req.permissions.contains('internet');
    final job = _Job(
      template: template,
      zip: zip,
      pk8: Uint8List.fromList(pk8),
      cert: Uint8List.fromList(cert),
      icon: icon,
      iconBg: iconBg,
      appName: name,
      packageName: quickPackageName(name, req.packageSuffix),
      versionName: '1.0',
      versionCode: DateTime.now().millisecondsSinceEpoch ~/ 1000,
      orientation: req.orientation,
      cacheMode: req.cacheMode == 'fresh' ? 'fresh' : 'fast',
      permissions: req.permissions.toSet(),
      allowInternet: online,
      url: url,
      encrypt: req.encryptContent,
      blockCapture: req.blockCapture,
      blockRoot: req.blockRoot,
    );

    // Real aapt2 on the phone builds the manifest. If it is missing or fails for any reason, the proven
    // byte-patch path below still makes the app, so a build never dies because of aapt2.
    Uint8List? manifest;
    step('Building the manifest with aapt2');
    try {
      manifest = await buildManifestWithAapt2(
        packageName: job.packageName,
        appName: job.appName,
        versionName: job.versionName,
        versionCode: job.versionCode,
        orientation: job.orientation,
        permissionIds: job.permissions,
        optionalPerms: _optionalPerms,
        allowInternet: job.allowInternet,
        appLinkHost: req.appLinkHost.trim(),
        deepLinkScheme: _schemeOf(job.packageName),
      );
      lastEngine = 'aapt2';
    } catch (e) {
      lastEngine = 'patch (aapt2 skipped: ${e.toString().split('\n').first})';
      manifest = null;
    }

    step('Building and signing');
    final Uint8List apk;
    try {
      apk = await compute(_assemble, job.withManifest(manifest));
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

  /// Makes the launcher icon of an adaptive icon from the picked image: the image centred inside the
  /// safe zone (foreground, 432px) plus a flat background in the image's own corner colour.
  static Future<(Uint8List, Uint8List)?> _iconPngs(String path) async {
    try {
      final bytes = await File(path).readAsBytes();
      final codec = await ui.instantiateImageCodec(bytes);
      final img = (await codec.getNextFrame()).image;
      final s = math.min(img.width, img.height).toDouble();
      final src = ui.Rect.fromLTWH((img.width - s) / 2, (img.height - s) / 2, s, s);

      // the foreground: the picture in the middle 66% of a transparent 432px square
      final rec = ui.PictureRecorder();
      final canvas = ui.Canvas(rec);
      const inner = 432 * 0.66;
      const pad = (432 - inner) / 2;
      canvas.drawImageRect(img, src, const ui.Rect.fromLTWH(pad, pad, inner, inner),
          ui.Paint()..filterQuality = ui.FilterQuality.high);
      final fg = await (await rec.endRecording().toImage(432, 432)).toByteData(format: ui.ImageByteFormat.png);

      // the background: one flat colour taken from the picture's top-left pixel
      final raw = await img.toByteData(format: ui.ImageByteFormat.rawRgba);
      var color = const ui.Color(0xFFFFFFFF);
      if (raw != null && raw.lengthInBytes >= 4 && raw.getUint8(3) > 0) {
        color = ui.Color.fromARGB(255, raw.getUint8(0), raw.getUint8(1), raw.getUint8(2));
      }
      final rec2 = ui.PictureRecorder();
      ui.Canvas(rec2).drawRect(const ui.Rect.fromLTWH(0, 0, 16, 16), ui.Paint()..color = color);
      final bg = await (await rec2.endRecording().toImage(16, 16)).toByteData(format: ui.ImageByteFormat.png);
      if (fg == null || bg == null) return null;
      return (fg.buffer.asUint8List(), bg.buffer.asUint8List());
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
        final raw = Uint8List.fromList(e.content as List<int>);
        final u8 = latin1.decode(raw, allowInvalid: true);
        final u16 = latin1.decode(Uint8List.fromList([for (final c in _tplLabel.codeUnits) ...[c, 0]]));
        final asU16 = latin1.decode(raw, allowInvalid: true);
        return (u8.contains(_tplLabel) || asU16.contains(u16)) ? t : null;
      }
    }
  }
  return null;
}

class _Job {
  final Uint8List template, pk8, cert;
  final Uint8List? zip, icon, iconBg;
  final String appName, packageName, versionName, orientation, cacheMode, url;
  final int versionCode;
  final Set<String> permissions;
  final bool allowInternet, encrypt, blockCapture, blockRoot;
  final Uint8List? manifest; // binary manifest from aapt2; null = patch the template's own
  _Job withManifest(Uint8List? m) => _Job(
        template: template, zip: zip, pk8: pk8, cert: cert, icon: icon, iconBg: iconBg,
        appName: appName, packageName: packageName, versionName: versionName, versionCode: versionCode,
        orientation: orientation, cacheMode: cacheMode, permissions: permissions, allowInternet: allowInternet,
        url: url, encrypt: encrypt, blockCapture: blockCapture, blockRoot: blockRoot, manifest: m,
      );
  const _Job({
    required this.template,
    required this.zip,
    required this.pk8,
    required this.cert,
    required this.icon,
    required this.iconBg,
    required this.appName,
    required this.packageName,
    required this.versionName,
    required this.versionCode,
    required this.orientation,
    required this.cacheMode,
    required this.permissions,
    required this.allowInternet,
    required this.url,
    this.encrypt = false,
    this.blockCapture = false,
    this.blockRoot = false,
    this.manifest,
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

  // 2. manifest: package (and the names the package builds), app name, version, orientation, permissions
  final manifest = entries['AndroidManifest.xml'];
  if (manifest == null) throw const FormatException('The Quick template has no AndroidManifest.xml.');
  if (j.manifest != null) {
    entries['AndroidManifest.xml'] = j.manifest!; // already complete, made by aapt2
  }
  final pkg = j.packageName;
  final strings = <String, String>{
    _tplPackage: pkg,
    '$_tplPackage.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION': '$pkg.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION',
    '$_tplPackage.androidx-startup': '$pkg.androidx-startup',
    '$_tplPackage.fileprovider': '$pkg.fileprovider',
    _tplScheme: _schemeOf(pkg),
    _tplLabel: j.appName,
    _tplVersionName: j.versionName,
  };
  final optional = <String>{_tplScheme, _tplVersionName, '$_tplPackage.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION', '$_tplPackage.androidx-startup'};
  if (!j.allowInternet) strings[_internet] = _harmless;
  _optionalPerms.forEach((id, perms) {
    if (j.permissions.contains(id)) return;
    for (final perm in perms) {
      strings[perm] = _harmless;
      optional.add(perm);
    }
  });
  final int? orient = j.orientation == 'portrait' ? 1 : (j.orientation == 'landscape' ? 6 : null);
  if (j.manifest == null) {
    entries['AndroidManifest.xml'] =
        axmlPatch(manifest, strings, versionCode: j.versionCode, screenOrientation: orient, optional: optional);
  }

  // 3. icon: the template's launcher is an adaptive icon made of two drawables (foreground and
  // background). Their files are renamed .xml -> .png in the resource table (same length, so it is an
  // in-place edit) and replaced by the picture. If anything is not as expected the template icon stays.
  if (j.icon != null && j.iconBg != null) {
    final arscBytes = entries['resources.arsc'];
    final fgPath = arscBytes == null ? null : arscFilePath(arscBytes, 'drawable', 'ic_launcher_foreground');
    final bgPath = arscBytes == null ? null : arscFilePath(arscBytes, 'drawable', 'ic_launcher_background');
    if (arscBytes != null &&
        fgPath != null &&
        bgPath != null &&
        fgPath != bgPath &&
        fgPath.endsWith('.xml') &&
        bgPath.endsWith('.xml') &&
        entries.containsKey(fgPath) &&
        entries.containsKey(bgPath)) {
      final patched = Uint8List.fromList(arscBytes);
      final fgPng = '${fgPath.substring(0, fgPath.length - 4)}.png';
      final bgPng = '${bgPath.substring(0, bgPath.length - 4)}.png';
      if (arscRenameSameLength(patched, fgPath, fgPng) && arscRenameSameLength(patched, bgPath, bgPng)) {
        entries['resources.arsc'] = patched;
        entries.remove(fgPath);
        entries.remove(bgPath);
        entries[fgPng] = j.icon!;
        entries[bgPng] = j.iconBg!;
      }
    }
  }

  // 4. what the template reads when it starts (assets/dexopt/.wc): the website link and cache mode
  // Protection is made fresh for this app: its own AES key, and the hash of its own signing certificate.
  final prot = QuickProtect.create(
    encrypt: j.encrypt && j.url.isEmpty,
    blockCapture: j.blockCapture,
    blockRoot: j.blockRoot,
    cert: j.cert,
    packageName: j.packageName,
  );
  // Website-link builds never load the template's bundled placeholder page -- the template
  // reads remoteUrl from .wc and calls loadUrl() straight to the live site, falling back to
  // file:///android_asset/index.html only when there's no remoteUrl. So for a link build this
  // file just sits there unused; drop it from the APK entirely instead of merely hiding it.
  if (j.url.isNotEmpty) entries.remove('assets/index.html');
  // When "Encrypt app content" is on, mask the URL itself too (website-link builds have no
  // files to run through QuickProtect.encryptFile, so this is the one thing left in the open).
  final maskUrl = j.url.isNotEmpty && j.encrypt;
  // Tucked into assets/dexopt/ -- already reserved for the template (see the `continue` above)
  // and looks like Android's own bytecode cache, not a config file a normal user would open.
  entries['assets/dexopt/.wc'] = Uint8List.fromList(utf8.encode(jsonEncode({
    if (j.url.isNotEmpty && !maskUrl) 'remoteUrl': j.url,
    if (maskUrl) 'u': prot.maskUrl(j.url),
    'cacheMode': j.cacheMode,
    if (prot.active) 'protect': prot.toJson(),
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
      if (k.substring(prefix.length).startsWith('dexopt/')) continue; // reserved by the template
      final rel = k.substring(prefix.length);
      if (prot.encrypt) {
        // Opaque hashed name -- no original extension or path shows up on disk.
        entries['assets/${prot.assetName(rel)}'] = prot.encryptFile(rel, files[k]!);
      } else {
        entries['assets/$rel'] = files[k]!;
      }
    }
  }

  // 6. zip (resources.arsc uncompressed and aligned, as Android 11+ needs) and sign
  final list = <ZipFileData>[
    for (final e in entries.entries)
      ZipFileData(e.key, e.value, stored: e.key == 'resources.arsc' || _mediaExt.hasMatch(e.key)),
  ];
  return signApk(list, j.pk8, j.cert);
}
