import 'dart:convert';
import 'dart:typed_data';

/// Tiny reader for the compiled resource table (resources.arsc) of the Quick template.
///
/// The template is built with resource-path obfuscation (res/Qr.xml and the like), so the
/// launcher icon can't be found by its name. This looks the path up through the table instead.

class _Pool {
  final bool utf8;
  final List<String> strings;
  final int size;
  _Pool(this.utf8, this.strings, this.size);
}

_Pool? _readPool(Uint8List d, int o) {
  final b = ByteData.sublistView(d);
  if (o + 28 > d.length || b.getUint16(o, Endian.little) != 0x0001) return null;
  final hs = b.getUint16(o + 2, Endian.little);
  final size = b.getUint32(o + 4, Endian.little);
  final count = b.getUint32(o + 8, Endian.little);
  final flags = b.getUint32(o + 16, Endian.little);
  final start = b.getUint32(o + 20, Endian.little);
  final isUtf8 = (flags & 0x100) != 0;
  final out = <String>[];
  for (var i = 0; i < count; i++) {
    final off = b.getUint32(o + hs + 4 * i, Endian.little);
    var p = o + start + off;
    if (isUtf8) {
      var c = d[p++];
      if (c & 0x80 != 0) p++;
      var len = d[p++];
      if (len & 0x80 != 0) {
        len = ((len & 0x7f) << 8) | d[p++];
      }
      out.add(utf8.decode(d.sublist(p, p + len), allowMalformed: true));
    } else {
      var len = b.getUint16(p, Endian.little);
      p += 2;
      if (len & 0x8000 != 0) {
        len = ((len & 0x7fff) << 16) | b.getUint16(p, Endian.little);
        p += 2;
      }
      out.add(String.fromCharCodes([for (var k = 0; k < len; k++) b.getUint16(p + 2 * k, Endian.little)]));
    }
  }
  return _Pool(isUtf8, out, size);
}

/// The file path (for example "res/Qr.xml") that the resource @[type]/[name] points to, or null.
String? arscFilePath(Uint8List arsc, String type, String name) {
  try {
    final b = ByteData.sublistView(arsc);
    int u16(int o) => b.getUint16(o, Endian.little);
    int u32(int o) => b.getUint32(o, Endian.little);
    if (arsc.length < 12 || u16(0) != 0x0002) return null;
    var o = 12;
    final global = _readPool(arsc, o);
    if (global == null) return null;
    o += global.size;
    while (o + 8 <= arsc.length && u16(o) != 0x0200) {
      final s = u32(o + 4);
      if (s < 8) return null;
      o += s;
    }
    if (o + 8 > arsc.length) return null;
    final pkgHeader = u16(o + 2);
    final pkgSize = u32(o + 4);
    final types = _readPool(arsc, o + u32(o + 8 + 4 + 256));
    final keys = _readPool(arsc, o + u32(o + 8 + 4 + 256 + 8));
    if (types == null || keys == null) return null;
    var p = o + pkgHeader;
    final end = o + pkgSize;
    while (p + 8 <= end) {
      final t = u16(p);
      final h = u16(p + 2);
      final s = u32(p + 4);
      if (s < 8) break;
      if (t == 0x0201) {
        final tid = arsc[p + 8];
        if (tid >= 1 && tid <= types.strings.length && types.strings[tid - 1] == type) {
          final count = u32(p + 12);
          final entriesStart = u32(p + 16);
          for (var i = 0; i < count; i++) {
            final eo = u32(p + h + 4 * i);
            if (eo == 0xffffffff) continue;
            final ep = p + entriesStart + eo;
            final flags = u16(ep + 2);
            if (flags & 1 != 0) continue; // complex entry
            final key = u32(ep + 4);
            if (key >= keys.strings.length || keys.strings[key] != name) continue;
            final dataType = arsc[ep + 8 + 3];
            final data = u32(ep + 8 + 4);
            if (dataType == 3 && data < global.strings.length) return global.strings[data];
          }
        }
      }
      p += s;
    }
  } catch (_) {}
  return null;
}

/// Renames one string of the table's global pool to another one of the same length (an in-place
/// edit, so nothing else in the table has to move). Returns false when it can't be done safely.
bool arscRenameSameLength(Uint8List arsc, String from, String to) {
  if (from.length != to.length || from.length >= 0x80) return false;
  final pool = _readPool(arsc, 12);
  if (pool == null) return false;
  List<int> enc(String s) => pool.utf8
      ? [s.length, s.length, ...utf8.encode(s), 0]
      : [s.length & 0xff, s.length >> 8, ...[for (final u in s.codeUnits) ...[u & 0xff, u >> 8]], 0, 0];
  final a = enc(from);
  final r = enc(to);
  var hit = -1;
  var hits = 0;
  final limit = 12 + pool.size;
  for (var i = 12; i + a.length <= limit && i + a.length <= arsc.length; i++) {
    var ok = true;
    for (var k = 0; k < a.length; k++) {
      if (arsc[i + k] != a[k]) {
        ok = false;
        break;
      }
    }
    if (ok) {
      hits++;
      hit = i;
    }
  }
  if (hits != 1) return false;
  arsc.setRange(hit, hit + r.length, r);
  return true;
}
