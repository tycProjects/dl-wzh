import 'dart:convert';
import 'dart:io';
import 'dart:typed_data';
import 'dart:ui' as ui;
import 'package:archive/archive.dart';
import 'package:flutter/services.dart' show rootBundle;
import 'package:path_provider/path_provider.dart';
import 'aapt_tools.dart';
import 'apk_signer.dart';
import 'apk_zip.dart';
import 'manifest_gen.dart';
import 'quick_protect.dart';
import '../quick/quick_builder.dart' show quickPackageName; // naming stays identical to today's Quick build

/// assets/aapt/android.jar (registered in pubspec.yaml) copied out to a real file once --
/// aapt2 link needs an actual path on disk, not a Flutter asset-bundle entry. Same pattern
/// the project already uses for assets/quick/template.apk.
Future<String> androidJarPath() async {
  final dir = await getApplicationSupportDirectory();
  final out = File('${dir.path}/android.jar');
  if (!await out.exists() || await out.length() < 1000) {
    final d = await rootBundle.load('assets/aapt/android.jar');
    await out.writeAsBytes(d.buffer.asUint8List(d.offsetInBytes, d.lengthInBytes));
  }
  return out.path;
}

/// Real build #2 for Quick build: generates a fresh manifest + resources and compiles them
/// with the actual aapt2 (see aapt_tools.dart / ANDROID_SETUP_AAPT2.md), instead of patching
/// bytes inside a fixed template. The app's logic (classes.dex, native libs) still comes
/// from a prebuilt template -- that part never needs a compiler, so this stays lightweight.
///
/// Needs, bundled once in the app: aapt2 + an android.jar stub as native-lib-dir tools
/// (ANDROID_SETUP_AAPT2.md), and a prebuilt "logic-only" template APK (template_dex.apk --
/// just classes.dex + any native libs, no manifest/resources of its own to fight with aapt2's).
class AaptQuickResult {
  final Uint8List apkBytes;
  final String log;
  const AaptQuickResult(this.apkBytes, this.log);
}

const optionalPerms = <String, List<String>>{
  'camera': ['android.permission.CAMERA'],
  'microphone': ['android.permission.RECORD_AUDIO'],
  'locationFine': ['android.permission.ACCESS_FINE_LOCATION'],
  'locationCoarse': ['android.permission.ACCESS_COARSE_LOCATION'],
  'storageLegacy': ['android.permission.WRITE_EXTERNAL_STORAGE'],
  'mediaImages': ['android.permission.READ_MEDIA_IMAGES'],
  'vibrate': ['android.permission.VIBRATE'],
  // add the rest of _optionalPerms from quick_builder.dart here as you wire each one up
};

Future<AaptQuickResult> buildWithAapt2({
  required String appName,
  required String packageSuffix,
  required Uint8List templateDex, // template_dex.apk: classes.dex (+ libs/) only
  required Uint8List pk8,
  required Uint8List cert,
  required Set<String> permissionIds,
  String orientation = 'auto',
  String appLinkHost = '',
  Uint8List? logoBytes,
  String? remoteUrl,
  Map<String, Uint8List>? siteFiles, // relative path -> bytes, for a bundled HTML site
  bool encryptContent = false,
  bool blockCapture = false,
  bool blockRoot = false,
  required String androidJarAssetPath, // where ANDROID_SETUP_AAPT2.md tells you to put the stub
}) async {
  final tmp = await Directory.systemTemp.createTemp('aapt_build_');
  final log = StringBuffer();
  try {
    final packageName = quickPackageName(appName, packageSuffix);
    final scheme = packageName.startsWith('com.') ? packageName.substring(4) : packageName;

    // 1. res/ folder: strings + launcher icon at every density aapt2 expects.
    final resDir = Directory('${tmp.path}/res')..createSync(recursive: true);
    Directory('${resDir.path}/values').createSync();
    File('${resDir.path}/values/strings.xml').writeAsStringSync(buildStringsXml(appName));
    await _writeIcon(resDir.path, logoBytes);

    // 2. AndroidManifest.xml -- real text, no same-length constraint.
    final manifestPath = '${tmp.path}/AndroidManifest.xml';
    File(manifestPath).writeAsStringSync(buildManifest(
      packageName: packageName,
      versionName: '1.0',
      versionCode: DateTime.now().millisecondsSinceEpoch ~/ 1000,
      orientation: orientation,
      permissionIds: permissionIds,
      optionalPerms: optionalPerms,
      appLinkHost: appLinkHost,
      deepLinkScheme: scheme,
    ));

    // 3. Compile + link with the real aapt2.
    final compiledZip = '${tmp.path}/compiled.zip';
    log.writeln('Compiling resources...');
    await AaptTools.compile(resDir: resDir.path, outZip: compiledZip);
    final linkedApk = '${tmp.path}/linked.apk';
    log.writeln('Linking manifest + resources...');
    await AaptTools.link(
      androidJarPath: androidJarAssetPath,
      manifestPath: manifestPath,
      outApkPath: linkedApk,
      compiledZips: [compiledZip],
    );

    // 4. Merge in the prebuilt logic (classes.dex, native libs) and the site, then re-sign --
    //    same ZipFileData + signApk path the byte-patched Quick build already uses, so
    //    protection (quick_protect.dart) plugs straight in unchanged.
    final entries = <String, Uint8List>{};
    for (final f in ZipDecoder().decodeBytes(File(linkedApk).readAsBytesSync()).files) {
      if (f.isFile) entries[f.name] = Uint8List.fromList(f.content as List<int>);
    }
    for (final f in ZipDecoder().decodeBytes(templateDex).files) {
      if (f.isFile && (f.name == 'classes.dex' || f.name.startsWith('lib/'))) {
        entries[f.name] = Uint8List.fromList(f.content as List<int>);
      }
    }

    final prot = QuickProtect.create(
      encrypt: encryptContent && (remoteUrl == null || remoteUrl.isEmpty),
      blockCapture: blockCapture,
      blockRoot: blockRoot,
      cert: cert,
      packageName: packageName,
    );
    final config = <String, dynamic>{
      if (remoteUrl != null && remoteUrl.isNotEmpty) 'remoteUrl': remoteUrl,
      if (prot.active) 'protect': prot.toJson(),
    };
    entries['assets/wzh_quick.json'] = Uint8List.fromList(utf8.encode(jsonEncode(config)));

    siteFiles?.forEach((path, bytes) {
      entries[prot.encrypt ? 'assets/$path.wzp' : 'assets/$path'] =
          prot.encrypt ? prot.encryptFile(path, bytes) : bytes;
    });

    final list = <ZipFileData>[
      for (final e in entries.entries)
        ZipFileData(e.key, e.value, stored: e.key == 'resources.arsc' || e.key.endsWith('.png')),
    ];
    log.writeln('Signing...');
    final apk = signApk(list, pk8, cert);
    log.writeln('Done: ${apk.length} bytes');
    return AaptQuickResult(apk, log.toString());
  } finally {
    await tmp.delete(recursive: true);
  }
}

/// Writes mipmap-*/ic_launcher.png at every density aapt2 needs. Reuses the same crop
/// logic as the current template patcher (centered square crop), just writes real files
/// instead of swapping bytes inside an existing resources.arsc entry.
Future<void> _writeIcon(String resPath, Uint8List? logoBytes) async {
  const sizes = {'mipmap-mdpi': 48, 'mipmap-hdpi': 72, 'mipmap-xhdpi': 96, 'mipmap-xxhdpi': 144, 'mipmap-xxxhdpi': 192};
  for (final e in sizes.entries) {
    Directory('$resPath/${e.key}').createSync(recursive: true);
    final bytes = logoBytes == null ? null : await _resizeSquare(logoBytes, e.value);
    if (bytes != null) {
      File('$resPath/${e.key}/ic_launcher.png').writeAsBytesSync(bytes);
    }
    // else: leave the folder empty and let aapt2 fall back to a style default --
    // or ship one default ic_launcher.png per density as a Flutter asset and copy it in here.
  }
}

Future<Uint8List?> _resizeSquare(Uint8List src, int size) async {
  try {
    final codec = await ui.instantiateImageCodec(src, targetWidth: size, targetHeight: size);
    final img = (await codec.getNextFrame()).image;
    final data = await img.toByteData(format: ui.ImageByteFormat.png);
    return data?.buffer.asUint8List();
  } catch (_) {
    return null;
  }
}
