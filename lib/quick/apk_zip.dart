import 'dart:convert';
import 'dart:io' show ZLibCodec;
import 'dart:typed_data';

class ZipFileData {
  final String name;
  final Uint8List data;
  final bool stored; // true = not compressed, and 4-byte aligned (resources.arsc must be)
  const ZipFileData(this.name, this.data, {this.stored = false});
}

class ZipParts {
  final Uint8List entries, centralDir;
  final int count;
  const ZipParts(this.entries, this.centralDir, this.count);
}

final List<int> _crcTable = () {
  final t = List<int>.filled(256, 0);
  for (var n = 0; n < 256; n++) {
    var c = n;
    for (var k = 0; k < 8; k++) {
      c = (c & 1) != 0 ? (0xEDB88320 ^ (c >> 1)) : (c >> 1);
    }
    t[n] = c;
  }
  return t;
}();

int crc32(List<int> data) {
  var c = 0xFFFFFFFF;
  for (var i = 0; i < data.length; i++) {
    c = _crcTable[(c ^ data[i]) & 0xFF] ^ (c >> 8);
  }
  return (c ^ 0xFFFFFFFF) & 0xFFFFFFFF;
}

void _w16(BytesBuilder o, int v) {
  o.addByte(v & 0xff);
  o.addByte((v >> 8) & 0xff);
}

void _w32(BytesBuilder o, int v) {
  o.addByte(v & 0xff);
  o.addByte((v >> 8) & 0xff);
  o.addByte((v >> 16) & 0xff);
  o.addByte((v >> 24) & 0xff);
}

const int _dosDate = 0x5021; // 2020-01-01, so every build is byte-for-byte repeatable
const int _dosTime = 0;

/// Writes the local entries and the central directory (but not the end record, so the signer can
/// slip the APK Signing Block in between). Stored entries get padding so their data starts on a
/// 4-byte boundary, which is what zipalign does.
ZipParts zipBuild(List<ZipFileData> files) {
  final out = BytesBuilder(copy: false);
  final cd = BytesBuilder(copy: false);
  final deflate = ZLibCodec(raw: true, level: 6);
  for (final f in files) {
    final nameBytes = utf8.encode(f.name);
    final stored = f.stored || f.data.isEmpty;
    final method = stored ? 0 : 8;
    final comp = stored ? f.data : Uint8List.fromList(deflate.encode(f.data));
    final off = out.length;
    final pad = stored ? (4 - ((off + 30 + nameBytes.length) % 4)) % 4 : 0;
    final extraLen = pad == 0 ? 0 : 4 + pad;
    final c = crc32(f.data);

    _w32(out, 0x04034b50);
    _w16(out, 20);
    _w16(out, 0x0800); // names are UTF-8
    _w16(out, method);
    _w16(out, _dosTime);
    _w16(out, _dosDate);
    _w32(out, c);
    _w32(out, comp.length);
    _w32(out, f.data.length);
    _w16(out, nameBytes.length);
    _w16(out, extraLen);
    out.add(nameBytes);
    if (extraLen != 0) {
      _w16(out, 0xD935); // unknown extra field id, only there to push the data onto a 4-byte boundary
      _w16(out, pad);
      out.add(Uint8List(pad));
    }
    out.add(comp);

    _w32(cd, 0x02014b50);
    _w16(cd, 20);
    _w16(cd, 20);
    _w16(cd, 0x0800);
    _w16(cd, method);
    _w16(cd, _dosTime);
    _w16(cd, _dosDate);
    _w32(cd, c);
    _w32(cd, comp.length);
    _w32(cd, f.data.length);
    _w16(cd, nameBytes.length);
    _w16(cd, 0); // extra
    _w16(cd, 0); // comment
    _w16(cd, 0); // disk
    _w16(cd, 0); // internal attrs
    _w32(cd, 0); // external attrs
    _w32(cd, off);
    cd.add(nameBytes);
  }
  return ZipParts(out.toBytes(), cd.toBytes(), files.length);
}

Uint8List zipEocd(int count, int cdSize, int cdOffset) {
  final o = BytesBuilder();
  _w32(o, 0x06054b50);
  _w16(o, 0);
  _w16(o, 0);
  _w16(o, count);
  _w16(o, count);
  _w32(o, cdSize);
  _w32(o, cdOffset);
  _w16(o, 0);
  return o.toBytes();
}
