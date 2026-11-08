import 'dart:convert';
import 'dart:typed_data';
import 'package:crypto/crypto.dart';
import 'package:pointycastle/export.dart';
import 'apk_zip.dart';
import 'der.dart';

/// Signs an APK with the v1 (JAR) and v2 schemes, like Google's apksig with minSdk 21 and v3 off.
/// v1 keeps old phones happy; v2 is required for apps targeting Android 11 and up.

final Uint8List _sha256Alg = Uint8List.fromList([0x30, 0x0d, 0x06, 0x09, 0x60, 0x86, 0x48, 0x01, 0x65, 0x03, 0x04, 0x02, 0x01, 0x05, 0x00]);
final Uint8List _rsaAlg = Uint8List.fromList([0x30, 0x0d, 0x06, 0x09, 0x2a, 0x86, 0x48, 0x86, 0xf7, 0x0d, 0x01, 0x01, 0x01, 0x05, 0x00]);
final Uint8List _oidData = Uint8List.fromList([0x06, 0x09, 0x2a, 0x86, 0x48, 0x86, 0xf7, 0x0d, 0x01, 0x07, 0x01]);
final Uint8List _oidSignedData = Uint8List.fromList([0x06, 0x09, 0x2a, 0x86, 0x48, 0x86, 0xf7, 0x0d, 0x01, 0x07, 0x02]);

Uint8List _sha(List<int> b) => Uint8List.fromList(sha256.convert(b).bytes);
String _b64(List<int> b) => base64.encode(b);

/// A manifest line is at most 72 bytes; longer ones continue on the next line after one space.
List<int> _wrap72(List<int> line) {
  final out = <int>[];
  var i = 0;
  var first = true;
  while (i < line.length) {
    final max = first ? 72 : 71;
    var end = i + max;
    if (end >= line.length) {
      end = line.length;
    } else {
      while (end > i + 1 && (line[end] & 0xC0) == 0x80) {
        end--; // never cut a multi-byte character in half
      }
    }
    if (!first) out.add(0x20);
    out.addAll(line.sublist(i, end));
    out.add(13);
    out.add(10);
    i = end;
    first = false;
  }
  if (out.isEmpty) out.addAll([13, 10]);
  return out;
}

List<int> _ascii(String s) => ascii.encode(s);

/// META-INF/MANIFEST.MF, CERT.SF and CERT.RSA for [files] (names + uncompressed bytes).
List<ZipFileData> v1Files(List<ZipFileData> files, RSAPrivateKey key, Uint8List cert) {
  final mf = BytesBuilder();
  mf.add(_ascii('Manifest-Version: 1.0\r\nCreated-By: 1.0 (Android)\r\n\r\n'));
  final names = <List<int>>[];
  final sections = <List<int>>[];
  for (final f in files) {
    final nameLine = _wrap72(utf8.encode('Name: ${f.name}'));
    final sec = <int>[...nameLine, ..._ascii('SHA-256-Digest: ${_b64(_sha(f.data))}\r\n\r\n')];
    names.add(nameLine);
    sections.add(sec);
    mf.add(sec);
  }
  final mfBytes = mf.toBytes();

  final sf = BytesBuilder();
  sf.add(_ascii('Signature-Version: 1.0\r\nCreated-By: 1.0 (Android)\r\n'
      'SHA-256-Digest-Manifest: ${_b64(_sha(mfBytes))}\r\nX-Android-APK-Signed: 2\r\n\r\n'));
  for (var i = 0; i < files.length; i++) {
    sf.add(names[i]);
    sf.add(_ascii('SHA-256-Digest: ${_b64(_sha(sections[i]))}\r\n\r\n'));
  }
  final sfBytes = sf.toBytes();

  final parts = parseCert(cert);
  final sig = rsaSha256(key, sfBytes);
  final signerInfo = derTlv(0x30, concatBytes([
    [0x02, 0x01, 0x01],
    derTlv(0x30, concatBytes([parts.issuer, parts.serial])),
    _sha256Alg,
    _rsaAlg,
    derTlv(0x04, sig),
  ]));
  final signedData = derTlv(0x30, concatBytes([
    [0x02, 0x01, 0x01],
    derTlv(0x31, _sha256Alg),
    derTlv(0x30, _oidData),
    derTlv(0xA0, cert),
    derTlv(0x31, signerInfo),
  ]));
  final rsa = derTlv(0x30, concatBytes([_oidSignedData, derTlv(0xA0, signedData)]));

  return [
    ZipFileData('META-INF/MANIFEST.MF', mfBytes),
    ZipFileData('META-INF/CERT.SF', sfBytes),
    ZipFileData('META-INF/CERT.RSA', rsa),
  ];
}

Uint8List _le32(int v) => Uint8List.fromList([v & 0xff, (v >> 8) & 0xff, (v >> 16) & 0xff, (v >> 24) & 0xff]);
Uint8List _le64(int v) => Uint8List.fromList([for (var i = 0; i < 8; i++) (v >> (8 * i)) & 0xff]);
Uint8List _lp(List<int> b) => concatBytes([_le32(b.length), b]);

const int _chunk = 1 << 20;

Uint8List _topDigest(List<Uint8List> sections) {
  final digests = BytesBuilder();
  var count = 0;
  for (final s in sections) {
    for (var i = 0; i < s.length; i += _chunk) {
      final end = i + _chunk < s.length ? i + _chunk : s.length;
      final c = Uint8List.sublistView(s, i, end);
      digests.add(_sha(concatBytes([[0xa5], _le32(c.length), c])));
      count++;
    }
  }
  return _sha(concatBytes([[0x5a], _le32(count), digests.toBytes()]));
}

Uint8List _v2Block(ZipParts z, RSAPrivateKey key, Uint8List cert) {
  final parts = parseCert(cert);
  // For the digest, the end record must say the central directory starts where the signing block will.
  final eocd = zipEocd(z.count, z.centralDir.length, z.entries.length);
  final digest = _topDigest([z.entries, z.centralDir, eocd]);

  final digests = _lp(_lp(concatBytes([_le32(0x0103), _le32(32), digest])));
  final certs = _lp(_lp(cert));
  final signedData = concatBytes([digests, certs, _lp(Uint8List(0))]);
  final sig = rsaSha256(key, signedData);
  final signatures = _lp(_lp(concatBytes([_le32(0x0103), _lp(sig)])));
  final signer = concatBytes([_lp(signedData), signatures, _lp(parts.spki)]);
  final value = _lp(_lp(signer));
  final pair = concatBytes([_le64(4 + value.length), _le32(0x7109871a), value]);
  final size = pair.length + 8 + 16;
  return concatBytes([_le64(size), pair, _le64(size), _ascii('APK Sig Block 42')]);
}

/// [files] are the APK's entries in order (no META-INF). Returns the finished, signed APK.
Uint8List signApk(List<ZipFileData> files, Uint8List pk8, Uint8List certBytes) {
  final key = parsePkcs8Rsa(pk8);
  final cert = certToDer(certBytes);
  final all = <ZipFileData>[...v1Files(files, key, cert), ...files];
  final z = zipBuild(all);
  final block = _v2Block(z, key, cert);
  final eocd = zipEocd(z.count, z.centralDir.length, z.entries.length + block.length);
  return concatBytes([z.entries, block, z.centralDir, eocd]);
}
