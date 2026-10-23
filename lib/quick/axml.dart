import 'dart:convert';
import 'dart:typed_data';

/// Edits a compiled (binary) AndroidManifest.xml without decompiling it.
///
/// Strings: every string in the pool that exactly equals a key of [repl] is replaced and the
/// pool is re-encoded, so the new string may have a different length.
/// Integers: versionCode and the activity's screenOrientation are overwritten in place.
Uint8List axmlPatch(Uint8List input, Map<String, String> repl,
    {required int versionCode, int? screenOrientation, Set<String> optional = const {}}) {
  final b = ByteData.sublistView(input);
  int u16(int o) => b.getUint16(o, Endian.little);
  int i32(int o) => b.getInt32(o, Endian.little);

  if (input.length < 36 || u16(0) != 0x0003) throw const FormatException('The template manifest is not binary XML.');
  const poolPos = 8;
  if (u16(poolPos) != 0x0001) throw const FormatException('The template manifest has no string pool.');
  final headerSize = u16(poolPos + 2);
  final poolSize = i32(poolPos + 4);
  final stringCount = i32(poolPos + 8);
  final styleCount = i32(poolPos + 12);
  final flags = i32(poolPos + 16);
  final stringsStart = i32(poolPos + 20);
  if (styleCount != 0) throw const FormatException('Styled string pool not supported.');
  final isUtf8 = (flags & 0x100) != 0;

  // 1. read + replace strings
  final strings = List<String>.filled(stringCount, '');
  final found = <String>{};
  for (var i = 0; i < stringCount; i++) {
    final off = i32(poolPos + headerSize + 4 * i);
    var s = _readString(input, b, poolPos + stringsStart + off, isUtf8);
    final r = repl[s];
    if (r != null) {
      found.add(s);
      s = r;
    }
    strings[i] = s;
  }
  for (final k in repl.keys) {
    if (!found.contains(k) && !optional.contains(k)) throw FormatException('The template manifest is missing "$k".');
  }

  // 2. re-encode the pool
  final data = BytesBuilder();
  final offsets = List<int>.filled(stringCount, 0);
  for (var i = 0; i < stringCount; i++) {
    offsets[i] = data.length;
    _writeString(data, strings[i], isUtf8);
  }
  while (data.length % 4 != 0) {
    data.addByte(0);
  }
  final newStringsStart = headerSize + 4 * stringCount;
  final newPoolSize = newStringsStart + data.length;

  // 3. the remaining chunks (the XML tree) are copied as they are, except the version code
  final restStart = poolPos + poolSize;
  final rest = Uint8List.fromList(input.sublist(restStart));
  _patchAttrs(rest, strings, versionCode, screenOrientation);

  final total = 8 + newPoolSize + rest.length;
  final out = Uint8List(total);
  final o = ByteData.sublistView(out);
  o.setUint16(0, 0x0003, Endian.little);
  o.setUint16(2, 8, Endian.little);
  o.setUint32(4, total, Endian.little);
  out.setRange(poolPos, poolPos + headerSize, input, poolPos); // pool header
  o.setUint32(poolPos + 4, newPoolSize, Endian.little);
  o.setUint32(poolPos + 20, newStringsStart, Endian.little);
  o.setUint32(poolPos + 24, 0, Endian.little); // no styles
  var w = poolPos + headerSize;
  for (final off in offsets) {
    o.setUint32(w, off, Endian.little);
    w += 4;
  }
  final d = data.toBytes();
  out.setRange(w, w + d.length, d);
  w += d.length;
  out.setRange(w, w + rest.length, rest);
  return out;
}

/// Walks the XML tree and overwrites two integer attributes in place: <manifest versionCode> and
/// every <activity screenOrientation> (when [orientation] is given). The template carries a real
/// build-time versionCode, so it is found by attribute name, not by its value.
void _patchAttrs(Uint8List rest, List<String> strings, int versionCode, int? orientation) {
  final b = ByteData.sublistView(rest);
  var p = 0;
  var gotVersion = false;
  var gotOrientation = false;
  while (p + 8 <= rest.length) {
    final type = b.getUint16(p, Endian.little);
    final size = b.getUint32(p + 4, Endian.little);
    if (size < 8) break;
    if (type == 0x0102 && p + 36 <= rest.length) {
      final nameIdx = b.getInt32(p + 20, Endian.little);
      final attrStart = b.getUint16(p + 24, Endian.little);
      final attrSize = b.getUint16(p + 26, Endian.little);
      final attrCount = b.getUint16(p + 28, Endian.little);
      final el = (nameIdx >= 0 && nameIdx < strings.length) ? strings[nameIdx] : '';
      for (var k = 0; k < attrCount; k++) {
        final a = p + 16 + attrStart + k * attrSize;
        if (a + 20 > rest.length) break;
        final an = b.getInt32(a + 4, Endian.little);
        final aName = (an >= 0 && an < strings.length) ? strings[an] : '';
        if (el == 'manifest' && aName == 'versionCode') {
          b.setInt32(a + 16, versionCode, Endian.little);
          gotVersion = true;
        } else if (el == 'activity' && aName == 'screenOrientation' && orientation != null) {
          b.setInt32(a + 16, orientation, Endian.little);
          gotOrientation = true;
        }
      }
    }
    p += size;
  }
  if (!gotVersion) throw const FormatException('The template manifest has no versionCode.');
  if (orientation != null && !gotOrientation) throw const FormatException('The template manifest has no screenOrientation.');
}

String _readString(Uint8List input, ByteData b, int pos, bool isUtf8) {
  if (isUtf8) {
    var p = pos;
    var chars = input[p++];
    if (chars & 0x80 != 0) chars = ((chars & 0x7f) << 8) | input[p++];
    var len = input[p++];
    if (len & 0x80 != 0) len = ((len & 0x7f) << 8) | input[p++];
    return utf8.decode(input.sublist(p, p + len), allowMalformed: true);
  }
  var len = b.getUint16(pos, Endian.little);
  var p = pos + 2;
  if (len & 0x8000 != 0) {
    len = ((len & 0x7fff) << 16) | b.getUint16(p, Endian.little);
    p += 2;
  }
  final units = <int>[for (var i = 0; i < len; i++) b.getUint16(p + 2 * i, Endian.little)];
  return String.fromCharCodes(units);
}

void _writeString(BytesBuilder out, String s, bool isUtf8) {
  if (isUtf8) {
    final bytes = utf8.encode(s);
    if (s.length > 0x7fff || bytes.length > 0x7fff) throw const FormatException('A manifest string is too long.');
    _len8(out, s.length);
    _len8(out, bytes.length);
    out.add(bytes);
    out.addByte(0);
  } else {
    final len = s.length;
    if (len > 0x7fff) {
      _u16(out, 0x8000 | (len >> 16));
      _u16(out, len & 0xffff);
    } else {
      _u16(out, len);
    }
    for (final u in s.codeUnits) {
      _u16(out, u);
    }
    _u16(out, 0);
  }
}

void _len8(BytesBuilder out, int n) {
  if (n > 0x7f) {
    out.addByte((n >> 8) | 0x80);
    out.addByte(n & 0xff);
  } else {
    out.addByte(n);
  }
}

void _u16(BytesBuilder out, int v) {
  out.addByte(v & 0xff);
  out.addByte((v >> 8) & 0xff);
}
