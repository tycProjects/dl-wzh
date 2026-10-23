import 'dart:convert';
import 'dart:io';
import 'dart:math';
import 'dart:typed_data';
import 'package:archive/archive.dart';
import 'package:crypto/crypto.dart';
import 'package:flutter/foundation.dart';
import 'package:path_provider/path_provider.dart';
import 'package:pointycastle/export.dart';
import 'api.dart';

/// The user's own release key on this phone, in the two files the build server takes:
/// a .pk8 private key and its certificate (.pem). See `signingKey` / `signingCert` in /api/build.
class SigningKeyInfo {
  final String fingerprint, keyPath, certPath;
  const SigningKeyInfo(this.fingerprint, this.keyPath, this.certPath);
  /// "55:08:34:9f:21:84..." - the first bytes of the certificate's SHA-256.
  String get shortFingerprint => '$fingerprint...';
}

class SigningKeys {
  SigningKeys._() {
    final f = Api.i.prefs.getString('signKeyFp');
    final k = Api.i.prefs.getString('signKeyPath');
    final c = Api.i.prefs.getString('signCertPath');
    if (f != null && k != null && c != null && File(k).existsSync() && File(c).existsSync()) {
      current.value = SigningKeyInfo(f, k, c);
    }
  }
  static final SigningKeys i = SigningKeys._();

  final ValueNotifier<SigningKeyInfo?> current = ValueNotifier(null);

  Future<Directory> _dir() async {
    final base = await getApplicationDocumentsDirectory();
    final d = Directory('${base.path}/signing');
    await d.create(recursive: true);
    return d;
  }

  /// Makes a brand-new key on the phone (RSA 2048, valid 30 years) and keeps it.
  Future<SigningKeyInfo> create() async {
    final made = await compute(_generate, 0);
    return _save(made['key'] as Uint8List, made['cert'] as Uint8List);
  }

  /// Imports a key from files the user picked: a .pk8 plus a .pem/.crt/.cer, or one of our backup .zip files.
  Future<SigningKeyInfo> importFiles(List<({String name, Uint8List bytes})> files) async {
    Uint8List? key, cert;
    for (final f in files) {
      final n = f.name.toLowerCase();
      if (n.endsWith('.zip')) {
        final z = ZipDecoder().decodeBytes(f.bytes);
        for (final e in z.files) {
          if (!e.isFile) continue;
          final en = e.name.toLowerCase();
          if (en.endsWith('.pk8')) key = Uint8List.fromList(e.content as List<int>);
          if (en.endsWith('.pem') || en.endsWith('.crt') || en.endsWith('.cer')) cert = Uint8List.fromList(e.content as List<int>);
        }
      } else if (n.endsWith('.pk8')) {
        key = f.bytes;
      } else if (n.endsWith('.pem') || n.endsWith('.crt') || n.endsWith('.cer')) {
        cert = f.bytes;
      }
    }
    if (key == null || cert == null || key.isEmpty || cert.isEmpty) {
      throw const FormatException('Pick both files together: the .pk8 key and its certificate (.pem, .crt or .cer). A backup .zip works too.');
    }
    _certDer(cert); // throws if it isn't a certificate
    return _save(key, cert);
  }

  Uint8List _certDer(Uint8List bytes) {
    final text = latin1.decode(bytes, allowInvalid: true);
    final m = RegExp(r'-----BEGIN CERTIFICATE-----([\s\S]+?)-----END CERTIFICATE-----').firstMatch(text);
    if (m != null) return base64.decode(m.group(1)!.replaceAll(RegExp(r'\s'), ''));
    if (bytes.isNotEmpty && bytes[0] == 0x30) return bytes; // already DER
    throw const FormatException('That doesn\u2019t look like a certificate file.');
  }

  Future<SigningKeyInfo> _save(Uint8List key, Uint8List cert) async {
    final fp = sha256.convert(_certDer(cert)).bytes.take(6).map((b) => b.toRadixString(16).padLeft(2, '0')).join(':');
    final d = await _dir();
    final kf = File('${d.path}/signing-key.pk8');
    final cf = File('${d.path}/signing-cert.pem');
    await kf.writeAsBytes(key, flush: true);
    await cf.writeAsBytes(cert, flush: true);
    await Api.i.prefs.setString('signKeyFp', fp);
    await Api.i.prefs.setString('signKeyPath', kf.path);
    await Api.i.prefs.setString('signCertPath', cf.path);
    final info = SigningKeyInfo(fp, kf.path, cf.path);
    current.value = info;
    return info;
  }

  /// Zips the key and certificate into the Download folder and returns where the zip went.
  Future<String> backup() async {
    final k = current.value;
    if (k == null) throw StateError('No key to back up.');
    final kb = await File(k.keyPath).readAsBytes();
    final cb = await File(k.certPath).readAsBytes();
    final z = Archive()
      ..addFile(ArchiveFile('signing-key.pk8', kb.length, kb))
      ..addFile(ArchiveFile('signing-cert.pem', cb.length, cb));
    Directory? base;
    try {
      base = await getExternalStorageDirectory();
    } catch (_) {}
    base ??= await getApplicationDocumentsDirectory();
    final dir = Directory('${base.path}/Download');
    await dir.create(recursive: true);
    final out = File('${dir.path}/signing-key-${k.fingerprint.replaceAll(':', '')}.zip');
    await out.writeAsBytes(ZipEncoder().encode(z)!, flush: true);
    return out.path;
  }

  Future<void> remove() async {
    final k = current.value;
    if (k != null) {
      for (final p in [k.keyPath, k.certPath]) {
        try {
          await File(p).delete();
        } catch (_) {}
      }
    }
    for (final key in ['signKeyFp', 'signKeyPath', 'signCertPath']) {
      await Api.i.prefs.remove(key);
    }
    current.value = null;
  }
}

// ---------------------------------------------------------------------------------------------
// Key generation (runs in a background isolate: a 2048-bit RSA key takes a few seconds).
// Builds a PKCS#8 private key and a self-signed X.509 certificate by hand in DER.
// ---------------------------------------------------------------------------------------------

Uint8List _len(int n) {
  if (n < 128) return Uint8List.fromList([n]);
  final b = <int>[];
  var x = n;
  while (x > 0) {
    b.insert(0, x & 0xff);
    x >>= 8;
  }
  return Uint8List.fromList([0x80 | b.length, ...b]);
}

Uint8List _tlv(int tag, List<int> v) => Uint8List.fromList([tag, ..._len(v.length), ...v]);
Uint8List _seq(List<List<int>> parts) => _tlv(0x30, [for (final p in parts) ...p]);
Uint8List _set(List<int> v) => _tlv(0x31, v);

Uint8List _int(BigInt v) {
  var hex = v.toRadixString(16);
  if (hex.length.isOdd) hex = '0$hex';
  final bytes = <int>[for (var i = 0; i < hex.length; i += 2) int.parse(hex.substring(i, i + 2), radix: 16)];
  if (bytes.isEmpty) bytes.add(0);
  if (bytes[0] & 0x80 != 0) bytes.insert(0, 0);
  return _tlv(0x02, bytes);
}

Uint8List _smallInt(int v) => _int(BigInt.from(v));

Uint8List _oid(List<int> arcs) {
  final out = <int>[40 * arcs[0] + arcs[1]];
  for (final a in arcs.skip(2)) {
    final chunk = <int>[a & 0x7f];
    var x = a >> 7;
    while (x > 0) {
      chunk.insert(0, 0x80 | (x & 0x7f));
      x >>= 7;
    }
    out.addAll(chunk);
  }
  return _tlv(0x06, out);
}

Uint8List _bitString(List<int> v) => _tlv(0x03, [0, ...v]);
Uint8List _null() => Uint8List.fromList([0x05, 0x00]);
Uint8List _utf8(String s) => _tlv(0x0c, utf8.encode(s));

Uint8List _time(DateTime t) {
  String p(int n, [int w = 2]) => n.toString().padLeft(w, '0');
  final u = t.toUtc();
  final tail = '${p(u.month)}${p(u.day)}${p(u.hour)}${p(u.minute)}${p(u.second)}Z';
  return u.year < 2050
      ? _tlv(0x17, ascii.encode('${p(u.year % 100)}$tail')) // UTCTime
      : _tlv(0x18, ascii.encode('${p(u.year, 4)}$tail')); // GeneralizedTime
}

Uint8List _name(String cn, String org) => _seq([
      _set(_seq([_oid([2, 5, 4, 10]), _utf8(org)])),
      _set(_seq([_oid([2, 5, 4, 3]), _utf8(cn)])),
    ]);

String _pem(String label, List<int> der) {
  final b = base64.encode(der);
  final lines = [for (var i = 0; i < b.length; i += 64) b.substring(i, min(i + 64, b.length))];
  return '-----BEGIN $label-----\n${lines.join('\n')}\n-----END $label-----\n';
}

Map<String, Object> _generate(int _) {
  final rnd = Random.secure();
  final fortuna = FortunaRandom()..seed(KeyParameter(Uint8List.fromList(List.generate(32, (_) => rnd.nextInt(256)))));
  final e = BigInt.from(65537);
  final gen = RSAKeyGenerator()..init(ParametersWithRandom(RSAKeyGeneratorParameters(e, 2048, 64), fortuna));
  final pair = gen.generateKeyPair();
  final priv = pair.privateKey as RSAPrivateKey;
  final n = priv.modulus!, d = priv.exponent!, p = priv.p!, q = priv.q!;

  final algRsa = _seq([_oid([1, 2, 840, 113549, 1, 1, 1]), _null()]);
  final algSha256Rsa = _seq([_oid([1, 2, 840, 113549, 1, 1, 11]), _null()]);

  // PKCS#1 RSAPrivateKey, wrapped as PKCS#8 (what a .pk8 file is).
  final pkcs1 = _seq([_smallInt(0), _int(n), _int(e), _int(d), _int(p), _int(q), _int(d % (p - BigInt.one)), _int(d % (q - BigInt.one)), _int(q.modInverse(p))]);
  final pk8 = _seq([_smallInt(0), algRsa, _tlv(0x04, pkcs1)]);

  // Self-signed certificate.
  final spki = _seq([algRsa, _bitString(_seq([_int(n), _int(e)]))]);
  final now = DateTime.now().toUtc();
  final serial = BigInt.parse(List.generate(8, (_) => rnd.nextInt(256).toRadixString(16).padLeft(2, '0')).join(), radix: 16) | (BigInt.one << 62);
  final who = _name('Release key', 'WZH Builder');
  final tbs = _seq([
    _tlv(0xA0, _smallInt(2)),
    _int(serial),
    algSha256Rsa,
    who,
    _seq([_time(now.subtract(const Duration(days: 1))), _time(now.add(const Duration(days: 365 * 30)))]),
    who,
    spki,
  ]);
  final signer = RSASigner(SHA256Digest(), '0609608648016503040201')..init(true, PrivateKeyParameter<RSAPrivateKey>(priv));
  final sig = signer.generateSignature(tbs).bytes;
  final cert = _seq([tbs, algSha256Rsa, _bitString(sig)]);

  return {'key': pk8, 'cert': Uint8List.fromList(utf8.encode(_pem('CERTIFICATE', cert)))};
}
