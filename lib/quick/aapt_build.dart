import 'dart:io';
import 'dart:typed_data';
import 'package:archive/archive.dart';
import 'package:flutter/services.dart' show rootBundle;
import 'package:path_provider/path_provider.dart';
import 'aapt_tools.dart';
import 'manifest_gen.dart';

/// assets/aapt/android.jar copied out to a real file once -- aapt2 link needs an actual path
/// on disk, not a Flutter asset-bundle entry.
Future<String> androidJarPath() async {
  final dir = await getApplicationSupportDirectory();
  final out = File('${dir.path}/android.jar');
  if (!await out.exists() || await out.length() < 1000000) {
    final d = await rootBundle.load('assets/aapt/android.jar');
    await out.writeAsBytes(d.buffer.asUint8List(d.offsetInBytes, d.lengthInBytes), flush: true);
  }
  return out.path;
}

/// Builds the app's binary AndroidManifest.xml with the REAL aapt2 on the phone.
///
/// Why only the manifest: the Quick template's classes.dex is compiled against the template's own
/// resources.arsc (androidx styles, layouts, ids...). Replacing that table with a freshly linked one
/// breaks every one of those ids and the app crashes on launch. So the template keeps its resources,
/// and aapt2 does the one job it is needed for: turning a normal text manifest (any app name, any
/// package, any permission list, no same-length limits) into the binary manifest.
///
/// The manifest refers to four template resources (theme, icon, file_paths, shortcuts). To keep those
/// references pointing at the template's real ids, tiny placeholder resources are linked with
/// --stable-ids pinned to the template's ids, and the resources.arsc that comes out is thrown away.
///
/// Throws when aapt2 is missing or fails; the caller falls back to the byte-patch builder.
Future<Uint8List> buildManifestWithAapt2({
  required String packageName,
  required String appName,
  required String versionName,
  required int versionCode,
  required String orientation,
  required Set<String> permissionIds,
  required Map<String, List<String>> optionalPerms,
  required bool allowInternet,
  required bool hasDccApplication,
  String appLinkHost = '',
  required String deepLinkScheme,
}) async {
  final tmp = await Directory.systemTemp.createTemp('aapt_manifest_');
  try {
    final resDir = Directory('${tmp.path}/res')..createSync(recursive: true);
    placeholderFiles.forEach((path, xml) {
      final f = File('${resDir.path}/$path');
      f.parent.createSync(recursive: true);
      f.writeAsStringSync(xml);
    });

    final manifestPath = '${tmp.path}/AndroidManifest.xml';
    File(manifestPath).writeAsStringSync(buildManifest(
      packageName: packageName,
      appName: appName,
      versionName: versionName,
      versionCode: versionCode,
      orientation: orientation,
      permissionIds: permissionIds,
      optionalPerms: optionalPerms,
      allowInternet: allowInternet,
      hasDccApplication: hasDccApplication,
      appLinkHost: appLinkHost,
      deepLinkScheme: deepLinkScheme,
    ));
    final idsPath = '${tmp.path}/stable_ids.txt';
    File(idsPath).writeAsStringSync(stableIdsFile(packageName));

    final compiledZip = '${tmp.path}/compiled.zip';
    await AaptTools.compile(resDir: resDir.path, outZip: compiledZip);
    final linked = '${tmp.path}/linked.apk';
    await AaptTools.link(
      androidJarPath: await androidJarPath(),
      manifestPath: manifestPath,
      outApkPath: linked,
      compiledZips: [compiledZip],
      stableIdsPath: idsPath,
    );

    for (final f in ZipDecoder().decodeBytes(await File(linked).readAsBytes()).files) {
      if (f.isFile && f.name == 'AndroidManifest.xml') {
        final bytes = Uint8List.fromList(f.content as List<int>);
        if (bytes.length < 100) break;
        return bytes;
      }
    }
    throw StateError('aapt2 finished but its output has no AndroidManifest.xml');
  } finally {
    try {
      await tmp.delete(recursive: true);
    } catch (_) {}
  }
}
