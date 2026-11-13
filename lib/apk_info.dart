import 'dart:convert' show Utf8Decoder;
import 'dart:typed_data';
import 'dart:io';

/// What the details page shows about a shared APK: read straight from its AndroidManifest.xml.
class ApkInfo {
  final String pkg, versionName;
  final int versionCode;
  const ApkInfo(this.pkg, this.versionName, this.versionCode);
}

/// Reads the package name and version of an .apk. Returns null when it can't be read (never throws),
/// so sharing never depends on it.
Future<ApkInfo?> readApkInfo(String path) async {
  // Reads only the zip's directory and the one small AndroidManifest.xml entry, so a 200 MB APK
  // never gets loaded into memory (decoding the whole archive made big files crash the app).
  RandomAccessFile? raf;
  try {
    raf = await File(path).open();
    final len = await raf.length();
    final tailLen = len < 65557 ? len : 65557;
    await raf.setPosition(len - tailLen);
    final tail = await raf.read(tailLen);
    var e = -1;
    for (var i = tail.length - 22; i >= 0; i--) {
      if (tail[i] == 0x50 && tail[i + 1] == 0x4b && tail[i + 2] == 5 && tail[i + 3] == 6) {
        e = i;
        break;
      }
    }
    if (e < 0) return null;
    final td = ByteData.sublistView(tail);
    final count = td.getUint16(e + 10, Endian.little);
    final cdSize = td.getUint32(e + 12, Endian.little);
    final cdOff = td.getUint32(e + 16, Endian.little);
    if (cdSize == 0 || cdSize > 64 * 1048576) return null;
    await raf.setPosition(cdOff);
    final cd = await raf.read(cdSize);
    final d = ByteData.sublistView(cd);
    var p = 0;
    for (var i = 0; i < count && p + 46 <= cd.length; i++) {
      if (d.getUint32(p, Endian.little) != 0x02014b50) break;
      final method = d.getUint16(p + 10, Endian.little);
      final csize = d.getUint32(p + 20, Endian.little);
      final nlen = d.getUint16(p + 28, Endian.little);
      final elen = d.getUint16(p + 30, Endian.little);
      final clen = d.getUint16(p + 32, Endian.little);
      final lho = d.getUint32(p + 42, Endian.little);
      final name = String.fromCharCodes(cd.sublist(p + 46, p + 46 + nlen));
      if (name == 'AndroidManifest.xml') {
        if (csize == 0 || csize > 8 * 1048576) return null;
        await raf.setPosition(lho);
        final lh = ByteData.sublistView(await raf.read(30));
        final ln = lh.getUint16(26, Endian.little);
        final le = lh.getUint16(28, Endian.little);
        await raf.setPosition(lho + 30 + ln + le);
        final data = await raf.read(csize);
        Uint8List? bytes;
        if (method == 0) {
          bytes = data;
        } else if (method == 8) {
          bytes = Uint8List.fromList(ZLibCodec(raw: true).decode(data));
        }
        return bytes == null ? null : _parseManifest(bytes);
      }
      p += 46 + nlen + elen + clen;
    }
    return null;
  } catch (_) {
    return null;
  } finally {
    try {
      await raf?.close();
    } catch (_) {}
  }
}

ApkInfo? _parseManifest(Uint8List b) {
  final d = ByteData.sublistView(b);
  if (b.length < 16 || d.getUint16(0, Endian.little) != 0x0003) return null;
  var pos = d.getUint16(2, Endian.little); // after the file header
  List<String> strings = const [];
  while (pos + 8 <= b.length) {
    final type = d.getUint16(pos, Endian.little);
    final headSize = d.getUint16(pos + 2, Endian.little);
    final size = d.getUint32(pos + 4, Endian.little);
    if (size < 8) return null;
    if (type == 0x0001) {
      strings = _pool(b, d, pos);
    } else if (type == 0x0102) {
      // The first start tag is <manifest ...>.
      final attrStart = d.getUint16(pos + 24, Endian.little);
      final attrSize = d.getUint16(pos + 26, Endian.little);
      final attrCount = d.getUint16(pos + 28, Endian.little);
      var pkg = '', vname = '', vcode = 0;
      for (var i = 0; i < attrCount; i++) {
        final a = pos + 16 + attrStart + i * (attrSize == 0 ? 20 : attrSize);
        final nameIdx = d.getUint32(a + 4, Endian.little);
        final raw = d.getUint32(a + 8, Endian.little);
        final dataType = b[a + 15];
        final data = d.getUint32(a + 16, Endian.little);
        if (nameIdx >= strings.length) continue;
        final name = strings[nameIdx];
        final str = raw != 0xFFFFFFFF && raw < strings.length ? strings[raw] : '';
        if (name == 'package') pkg = str;
        if (name == 'versionName') vname = str;
        if (name == 'versionCode' && (dataType == 0x10 || dataType == 0x11)) vcode = data;
      }
      return pkg.isEmpty ? null : ApkInfo(pkg, vname, vcode);
    }
    pos += size;
  }
  return null;
}

List<String> _pool(Uint8List b, ByteData d, int at) {
  final count = d.getUint32(at + 8, Endian.little);
  final flags = d.getUint32(at + 16, Endian.little);
  final strStart = d.getUint32(at + 20, Endian.little);
  final head = d.getUint16(at + 2, Endian.little);
  final utf8 = (flags & 0x100) != 0;
  final out = <String>[];
  for (var i = 0; i < count; i++) {
    var p = at + strStart + d.getUint32(at + head + i * 4, Endian.little);
    if (utf8) {
      var n = b[p++];
      if (n & 0x80 != 0) p++; // long char count
      var blen = b[p++];
      if (blen & 0x80 != 0) {
        blen = ((blen & 0x7F) << 8) | b[p++];
      }
      out.add(const Utf8Decoder(allowMalformed: true).convert(Uint8List.sublistView(b, p, p + blen)));
    } else {
      var n = d.getUint16(p, Endian.little);
      p += 2;
      if (n & 0x8000 != 0) {
        n = ((n & 0x7FFF) << 16) | d.getUint16(p, Endian.little);
        p += 2;
      }
      final units = <int>[for (var k = 0; k < n; k++) d.getUint16(p + k * 2, Endian.little)];
      out.add(String.fromCharCodes(units));
    }
  }
  return out;
}
