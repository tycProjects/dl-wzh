import 'dart:convert';
import 'dart:io';
import 'dart:typed_data';
import 'package:crypto/crypto.dart';
import 'package:flutter/foundation.dart';
import 'package:package_info_plus/package_info_plus.dart';
import 'config.dart';
import 'integrity.dart';

/// What this app tells the build server about itself, read from the installed app at run time
/// (nothing here is a fixed value that a re-signed copy could simply keep sending):
///  * the signing certificate, sent in the User-Agent as TyceptSig/<sha256>. The server refuses
///    builds when it is not TYCEPT_SIGNING_SHA256.
///  * the code fingerprint, sent as the X-App-Code header: a SHA-256 over the real bytes of the
///    app's code files (classes*.dex, libapp.so, libflutter.so, AndroidManifest.xml,
///    resources.arsc) read straight from the installed APK. The server refuses builds when it is
///    not one of TYCEPT_CODE_SHA256. tool/code_fingerprint.py makes the same value from an APK.
///
/// Debug builds and non-Android platforms keep the old fixed value so you can still test.
class AppProof {
  static String _sig = '';
  static String _code = '';
  static bool _loaded = false;
  static Future<void>? _loading;

  /// The code fingerprint ('' when it couldn't be read on this device).
  static String get code => _code;

  /// Value for the User-Agent. 'none' tells the server the signature couldn't be read.
  static String get userAgent {
    final s = _loaded ? _sig : '';
    return '$kAppToken/1.0 TyceptSig/${s.length == 64 ? s : 'none'}';
  }

  static Map<String, String> get extraHeaders => _code.length == 64 ? {'X-App-Code': _code} : const {};

  static Future<void> load() => _loading ??= _doLoad();

  static Future<void> _doLoad() async {
    try {
      if (!Platform.isAndroid || kDebugMode) {
        _sig = kSigningSha256;
        return;
      }
      var sig = AppIntegrity.normalize(await AppIntegrity.currentSignature());
      if (sig.isEmpty) {
        await Future.delayed(const Duration(milliseconds: 350));
        sig = AppIntegrity.normalize(await AppIntegrity.currentSignature());
      }
      _sig = sig;
      try {
        final pkg = (await PackageInfo.fromPlatform()).packageName;
        final path = await _installedApkPath(pkg);
        if (path != null) _code = await compute(_fingerprintOf, path);
      } catch (_) {
        _code = '';
      }
    } finally {
      _loaded = true;
    }
  }

  /// This app's own APK, taken from the files the process has mapped (so no extra permission).
  static Future<String?> _installedApkPath(String pkg) async {
    try {
      final maps = await File('/proc/self/maps').readAsString();
      final re = RegExp('(/[^\\s]*/${RegExp.escape(pkg)}-[^\\s/]*/base\\.apk)');
      final m = re.firstMatch(maps);
      if (m != null && File(m.group(1)!).existsSync()) return m.group(1);
    } catch (_) {}
    return null;
  }
}

final RegExp _wanted = RegExp(r'^(classes[0-9]*\.dex|AndroidManifest\.xml|resources\.arsc|lib/[^/]+/lib(app|flutter)\.so)$');

int _u16(Uint8List b, int o) => b[o] | (b[o + 1] << 8);
int _u32(Uint8List b, int o) => (b[o] | (b[o + 1] << 8) | (b[o + 2] << 16) | (b[o + 3] << 24)) & 0xFFFFFFFF;

class _Cap implements Sink<Digest> {
  Digest? value;
  @override
  void add(Digest d) => value = d;
  @override
  void close() {}
}

class _Fwd implements Sink<List<int>> {
  final ByteConversionSink target;
  _Fwd(this.target);
  @override
  void add(List<int> data) => target.add(data);
  @override
  void close() {}
}

/// Runs in a background isolate. Returns the 64-char fingerprint, or '' if anything is off.
/// Must stay in step with tool/code_fingerprint.py.
String _fingerprintOf(String apkPath) {
  RandomAccessFile? f;
  try {
    f = File(apkPath).openSync();
    final size = f.lengthSync();
    final tailLen = size < 66000 ? size : 66000;
    f.setPositionSync(size - tailLen);
    final tail = Uint8List.fromList(f.readSync(tailLen));
    var e = -1;
    for (var i = tail.length - 22; i >= 0; i--) {
      if (tail[i] == 0x50 && tail[i + 1] == 0x4B && tail[i + 2] == 5 && tail[i + 3] == 6) {
        e = i;
        break;
      }
    }
    if (e < 0) return '';
    final cdSize = _u32(tail, e + 12);
    final cdOff = _u32(tail, e + 16);
    if (cdOff == 0xFFFFFFFF || cdSize == 0xFFFFFFFF) return '';
    f.setPositionSync(cdOff);
    final cd = Uint8List.fromList(f.readSync(cdSize));

    final found = <String, List<int>>{}; // name -> [method, compSize, localHeaderOffset]
    var p = 0;
    while (p + 46 <= cd.length && _u32(cd, p) == 0x02014B50) {
      final method = _u16(cd, p + 10);
      final comp = _u32(cd, p + 20);
      final nLen = _u16(cd, p + 28), eLen = _u16(cd, p + 30), cLen = _u16(cd, p + 32);
      final off = _u32(cd, p + 42);
      final name = utf8.decode(cd.sublist(p + 46, p + 46 + nLen), allowMalformed: true);
      if (_wanted.hasMatch(name)) found[name] = [method, comp, off];
      p += 46 + nLen + eLen + cLen;
    }
    if (!found.containsKey('classes.dex') || !found.containsKey('AndroidManifest.xml')) return '';

    final names = found.keys.toList()..sort();
    final lines = StringBuffer();
    for (final name in names) {
      final v = found[name]!;
      final method = v[0], comp = v[1], off = v[2];
      if (method != 0 && method != 8) return '';
      f.setPositionSync(off);
      final hdr = Uint8List.fromList(f.readSync(30));
      if (hdr.length < 30 || _u32(hdr, 0) != 0x04034B50) return '';
      f.setPositionSync(off + 30 + _u16(hdr, 26) + _u16(hdr, 28));

      final cap = _Cap();
      final hasher = sha256.startChunkedConversion(cap);
      final ByteConversionSink sink =
          method == 8 ? ZLibDecoder(raw: true).startChunkedConversion(_Fwd(hasher)) : hasher;
      var left = comp;
      while (left > 0) {
        final chunk = f.readSync(left < 65536 ? left : 65536);
        if (chunk.isEmpty) return '';
        left -= chunk.length;
        sink.add(chunk);
      }
      if (method == 8) sink.close();
      hasher.close();
      lines.write('$name:${cap.value}\n');
    }
    return sha256.convert(utf8.encode(lines.toString())).toString();
  } catch (_) {
    return '';
  } finally {
    try {
      f?.closeSync();
    } catch (_) {}
  }
}
