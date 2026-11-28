import 'dart:io';
import 'dart:typed_data';
import 'package:archive/archive.dart';
import 'package:flutter/services.dart' show rootBundle;
import 'package:http/http.dart' as http;
import 'package:path_provider/path_provider.dart';
import 'aapt_tools.dart';
import 'manifest_gen.dart';

/// Where the app downloads android.jar from the first time it is needed (like a game's "downloading
/// resources" step). Put the file at this address (a GitHub Release asset or a Supabase public file) and
/// replace the placeholder. If assets/aapt/android.jar is bundled in the app, it is used instead and no
/// download happens.
const kAndroidJarUrl = 'https://github.com/tycweb/apk-files/releases/download/st-d9d1317e-a49-1795816579365/android.jar';

/// A real file on disk is needed by aapt2 (a Flutter asset-bundle entry will not do). Order: the copy
/// already saved -> the bundled asset (if present) -> a one-time download.
/// True when android.jar is already on the phone (saved earlier) or bundled in the app, so nothing
/// needs to be downloaded. Used by the first-launch setup popup.
Future<bool> androidJarReady() async {
  try {
    final dir = await getApplicationSupportDirectory();
    if (await _jarOk(File('${dir.path}/android.jar'))) return true;
  } catch (_) {}
  try {
    final d = await rootBundle.load('assets/aapt/android.jar');
    return d.lengthInBytes > 1000000;
  } catch (_) {
    return false;
  }
}

/// [onProgress] is told how many bytes have arrived so far and the total size (null when unknown),
/// so a screen can show a progress bar while the one-time download runs.
Future<String> androidJarPath({void Function(int got, int? total)? onProgress}) async {
  final dir = await getApplicationSupportDirectory();
  final out = File('${dir.path}/android.jar');
  if (await _jarOk(out)) return out.path;

  try {
    final d = await rootBundle.load('assets/aapt/android.jar');
    await out.writeAsBytes(d.buffer.asUint8List(d.offsetInBytes, d.lengthInBytes), flush: true);
    if (await _jarOk(out)) return out.path;
  } catch (_) {
    // Not bundled: download it below.
  }

  final tmp = File('${out.path}.part');
  try {
    final res = await http.Client().send(http.Request('GET', Uri.parse(kAndroidJarUrl))).timeout(const Duration(seconds: 30));
    if (res.statusCode != 200) throw HttpException('HTTP ${res.statusCode}');
    final sink = tmp.openWrite();
    var got = 0;
    final total = (res.contentLength ?? 0) > 0 ? res.contentLength : null;
    onProgress?.call(0, total);
    await res.stream
        .map((chunk) {
          got += chunk.length;
          onProgress?.call(got, total);
          return chunk;
        })
        .timeout(const Duration(seconds: 60))
        .pipe(sink);
    if (!await _jarOk(tmp)) throw const FormatException('not a valid android.jar');
    if (await out.exists()) await out.delete();
    await tmp.rename(out.path);
    return out.path;
  } catch (e) {
    try {
      if (await tmp.exists()) await tmp.delete();
    } catch (_) {}
    throw Exception('Could not download the build resources (android.jar). Check your connection and try again. ($e)');
  }
}

/// A usable jar is a zip (starts with "PK") and well over 1 MB.
Future<bool> _jarOk(File f) async {
  try {
    if (!await f.exists() || await f.length() < 1000000) return false;
    final h = await f.openRead(0, 2).first;
    return h.length == 2 && h[0] == 0x50 && h[1] == 0x4B;
  } catch (_) {
    return false;
  }
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
