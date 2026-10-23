import 'dart:convert';
import 'dart:typed_data';
import 'package:pointycastle/export.dart';

/// Just enough DER to pull a certificate apart and read a PKCS#8 RSA key.

class DerItem {
  final int tag, hdr, start, end; // hdr = where the tag byte is, [start, end) = the value
  const DerItem(this.tag, this.hdr, this.start, this.end);
}

DerItem derRead(Uint8List b, int p) {
  final hdr = p;
  final tag = b[p++];
  var l = b[p++];
  if (l & 0x80 != 0) {
    final k = l & 0x7f;
    l = 0;
    for (var i = 0; i < k; i++) {
      l = (l << 8) | b[p + i];
    }
    p += k;
  }
  return DerItem(tag, hdr, p, p + l);
}

Uint8List derTlv(int tag, List<int> body) {
  final n = body.length;
  final len = <int>[];
  if (n < 128) {
    len.add(n);
  } else {
    final bs = <int>[];
    var x = n;
    while (x > 0) {
      bs.insert(0, x & 0xff);
      x >>= 8;
    }
    len.add(0x80 | bs.length);
    len.addAll(bs);
  }
  return Uint8List.fromList([tag, ...len, ...body]);
}

Uint8List concatBytes(List<List<int>> parts) {
  final out = BytesBuilder(copy: false);
  for (final p in parts) {
    out.add(p);
  }
  return out.toBytes();
}

BigInt _big(List<int> b) {
  var r = BigInt.zero;
  for (final x in b) {
    r = (r << 8) | BigInt.from(x);
  }
  return r;
}

class CertParts {
  final Uint8List serial, issuer, spki; // each one is a whole DER element (tag + length + value)
  const CertParts(this.serial, this.issuer, this.spki);
}

/// Accepts a PEM ("-----BEGIN CERTIFICATE-----") or a raw DER certificate.
Uint8List certToDer(Uint8List bytes) {
  final text = latin1.decode(bytes, allowInvalid: true);
  final m = RegExp(r'-----BEGIN CERTIFICATE-----([\s\S]+?)-----END CERTIFICATE-----').firstMatch(text);
  if (m != null) return Uint8List.fromList(base64.decode(m.group(1)!.replaceAll(RegExp(r'\s'), '')));
  if (bytes.isNotEmpty && bytes[0] == 0x30) return bytes;
  throw const FormatException('The signing certificate is not readable.');
}

CertParts parseCert(Uint8List cert) {
  final top = derRead(cert, 0); // Certificate
  final tbs = derRead(cert, top.start); // TBSCertificate
  var p = tbs.start;
  var it = derRead(cert, p);
  if (it.tag == 0xA0) {
    // optional [0] version
    p = it.end;
    it = derRead(cert, p);
  }
  final serial = Uint8List.sublistView(cert, p, it.end);
  p = it.end;
  it = derRead(cert, p); // signature algorithm
  p = it.end;
  it = derRead(cert, p); // issuer
  final issuer = Uint8List.sublistView(cert, p, it.end);
  p = it.end;
  it = derRead(cert, p); // validity
  p = it.end;
  it = derRead(cert, p); // subject
  p = it.end;
  it = derRead(cert, p); // subjectPublicKeyInfo
  final spki = Uint8List.sublistView(cert, p, it.end);
  return CertParts(serial, issuer, spki);
}

/// PKCS#8 (.pk8) RSA private key -> pointycastle key.
RSAPrivateKey parsePkcs8Rsa(Uint8List pk8) {
  final top = derRead(pk8, 0);
  var p = top.start;
  p = derRead(pk8, p).end; // version
  p = derRead(pk8, p).end; // algorithm identifier
  final oct = derRead(pk8, p); // OCTET STRING holding the PKCS#1 key
  final inner = derRead(pk8, oct.start);
  var q = inner.start;
  BigInt next() {
    final it = derRead(pk8, q);
    q = it.end;
    return _big(pk8.sublist(it.start, it.end));
  }

  next(); // version
  final n = next();
  next(); // public exponent
  final d = next();
  final pp = next();
  final qq = next();
  return RSAPrivateKey(n, d, pp, qq);
}

/// RSASSA-PKCS1-v1_5 with SHA-256.
Uint8List rsaSha256(RSAPrivateKey key, Uint8List data) {
  final s = RSASigner(SHA256Digest(), '0609608648016503040201');
  s.init(true, PrivateKeyParameter<RSAPrivateKey>(key));
  return Uint8List.fromList(s.generateSignature(data).bytes);
}
