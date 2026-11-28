import 'dart:convert';
import 'dart:io';
import 'package:http/http.dart' as http;
import 'package:open_filex/open_filex.dart';
import 'package:path_provider/path_provider.dart';

/// Downloads the Termux app straight from its official GitHub releases and opens Android's installer,
/// so nobody has to visit a website. Android always shows its own "Install" screen (no silent installs).
class TermuxDownload {
  static const _api = 'https://api.github.com/repos/termux/termux-app/releases/latest';

  /// Picks the right APK file name for this phone's CPU.
  static String _pick(List<Map<String, dynamic>> assets, List<String> abis) {
    final apks = assets.map((a) => a['browser_download_url'] as String).where((u) => u.endsWith('.apk')).toList();
    for (final abi in abis) {
      for (final u in apks) {
        if (u.contains(abi.replaceAll('-', '_')) || u.contains(abi)) return u;
      }
    }
    for (final u in apks) {
      if (u.contains('universal')) return u;
    }
    if (apks.isEmpty) throw Exception('No Termux APK found in the latest release.');
    return apks.first;
  }

  static Future<List<String>> _abis() async {
    // The most common modern phone first; universal is the fallback in _pick.
    return ['arm64-v8a', 'armeabi-v7a', 'x86_64', 'x86'];
  }

  /// [onProgress] gets 0..1 (or null while the size is unknown). Returns null on success, else an error text.
  static Future<String?> downloadAndInstall({void Function(double? p)? onProgress}) async {
    final client = http.Client();
    try {
      onProgress?.call(null);
      final r = await client
          .get(Uri.parse(_api), headers: {'Accept': 'application/vnd.github+json'})
          .timeout(const Duration(seconds: 20));
      if (r.statusCode != 200) return 'Could not reach the Termux download (code ${r.statusCode}).';
      final j = jsonDecode(r.body) as Map<String, dynamic>;
      final assets = (j['assets'] as List).cast<Map<String, dynamic>>();
      final url = _pick(assets, await _abis());

      final dir = await getTemporaryDirectory();
      final file = File('${dir.path}/termux.apk');
      if (await file.exists()) await file.delete();

      final req = http.Request('GET', Uri.parse(url));
      final res = await client.send(req).timeout(const Duration(seconds: 30));
      if (res.statusCode != 200) return 'Download failed (code ${res.statusCode}).';
      final total = res.contentLength ?? 0;
      var got = 0;
      final sink = file.openWrite();
      try {
        await for (final chunk in res.stream) {
          sink.add(chunk);
          got += chunk.length;
          onProgress?.call(total > 0 ? got / total : null);
        }
      } finally {
        await sink.close();
      }
      if (total > 0 && got < total) return 'The download was cut off. Check your connection and try again.';

      final o = await OpenFilex.open(file.path, type: 'application/vnd.android.package-archive');
      if (o.type != ResultType.done) {
        return 'Could not open the installer. Allow “Install unknown apps” for this app in Android settings, then try again.';
      }
      return null;
    } catch (e) {
      return 'Download failed. Check your internet and try again.';
    } finally {
      client.close();
    }
  }
}
