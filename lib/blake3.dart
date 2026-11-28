import 'dart:math' as math;
import 'dart:typed_data';

/// BLAKE3 (plain hash mode, 32-byte output). Cloudflare Pages names every uploaded file by
/// blake3(base64(content) + extension), so the app needs it to publish a site on its own.
/// Checked against the official test vectors (empty, 1, 1024, 1025, 2048, 3072, 102400 bytes).

const List<int> _iv = [0x6A09E667, 0xBB67AE85, 0x3C6EF372, 0xA54FF53A, 0x510E527F, 0x9B05688C, 0x1F83D9AB, 0x5BE0CD19];
const List<int> _perm = [2, 6, 3, 10, 7, 0, 4, 13, 1, 11, 12, 5, 9, 14, 15, 8];
const int _chunkStart = 1, _chunkEnd = 2, _parent = 4, _root = 8;
const int _mask = 0xFFFFFFFF;

int _rotr(int x, int n) => ((x >> n) | (x << (32 - n))) & _mask;

void _g(Uint32List s, int a, int b, int c, int d, int mx, int my) {
  s[a] = s[a] + s[b] + mx;
  s[d] = _rotr(s[d] ^ s[a], 16);
  s[c] = s[c] + s[d];
  s[b] = _rotr(s[b] ^ s[c], 12);
  s[a] = s[a] + s[b] + my;
  s[d] = _rotr(s[d] ^ s[a], 8);
  s[c] = s[c] + s[d];
  s[b] = _rotr(s[b] ^ s[c], 7);
}

void _round(Uint32List s, Uint32List m) {
  _g(s, 0, 4, 8, 12, m[0], m[1]);
  _g(s, 1, 5, 9, 13, m[2], m[3]);
  _g(s, 2, 6, 10, 14, m[4], m[5]);
  _g(s, 3, 7, 11, 15, m[6], m[7]);
  _g(s, 0, 5, 10, 15, m[8], m[9]);
  _g(s, 1, 6, 11, 12, m[10], m[11]);
  _g(s, 2, 7, 8, 13, m[12], m[13]);
  _g(s, 3, 4, 9, 14, m[14], m[15]);
}

Uint32List _compress(List<int> cv, Uint32List block, int counter, int blen, int flags) {
  final s = Uint32List(16);
  for (var i = 0; i < 8; i++) {
    s[i] = cv[i];
  }
  s[8] = _iv[0];
  s[9] = _iv[1];
  s[10] = _iv[2];
  s[11] = _iv[3];
  s[12] = counter & _mask;
  s[13] = (counter >> 32) & _mask;
  s[14] = blen;
  s[15] = flags;
  var m = Uint32List.fromList(block);
  for (var r = 0; r < 7; r++) {
    _round(s, m);
    if (r < 6) {
      final n = Uint32List(16);
      for (var i = 0; i < 16; i++) {
        n[i] = m[_perm[i]];
      }
      m = n;
    }
  }
  for (var i = 0; i < 8; i++) {
    s[i] = s[i] ^ s[i + 8];
    s[i + 8] = s[i + 8] ^ cv[i];
  }
  return s;
}

Uint32List _words(Uint8List b, int off, int len) {
  final w = Uint32List(16);
  for (var i = 0; i < len; i++) {
    w[i >> 2] = w[i >> 2] | (b[off + i] << (8 * (i & 3)));
  }
  return w;
}

/// The last block of a chunk or parent, not yet compressed (the root needs the ROOT flag on it).
class _Out {
  final List<int> cv;
  final Uint32List block;
  final int counter, blen, flags;
  _Out(this.cv, this.block, this.counter, this.blen, this.flags);
}

List<int> _chain(_Out o) => _compress(o.cv, o.block, o.counter, o.blen, o.flags).sublist(0, 8);

_Out _parentOut(List<int> l, List<int> r) {
  final b = Uint32List(16);
  for (var i = 0; i < 8; i++) {
    b[i] = l[i];
    b[i + 8] = r[i];
  }
  return _Out(_iv, b, 0, 64, _parent);
}

_Out _chunkOut(Uint8List data, int off, int len, int counter) {
  List<int> cv = _iv;
  final nblocks = len == 0 ? 1 : (len + 63) ~/ 64;
  for (var i = 0; i < nblocks - 1; i++) {
    cv = _compress(cv, _words(data, off + i * 64, 64), counter, 64, i == 0 ? _chunkStart : 0).sublist(0, 8);
  }
  final lastLen = len - (nblocks - 1) * 64;
  final flags = (nblocks == 1 ? _chunkStart : 0) | _chunkEnd;
  return _Out(cv, _words(data, off + (nblocks - 1) * 64, lastLen), counter, lastLen, flags);
}

Uint8List blake3(Uint8List data) {
  final n = data.length;
  final chunks = n == 0 ? 1 : (n + 1023) ~/ 1024;
  final stack = <List<int>>[];
  _Out? last;
  for (var c = 0; c < chunks; c++) {
    final off = c * 1024;
    final out = _chunkOut(data, off, math.min(1024, n - off), c);
    if (c == chunks - 1) {
      last = out;
      break;
    }
    var cv = _chain(out);
    var total = c + 1;
    while ((total & 1) == 0) {
      cv = _chain(_parentOut(stack.removeLast(), cv));
      total >>= 1;
    }
    stack.add(cv);
  }
  var out = last!;
  while (stack.isNotEmpty) {
    out = _parentOut(stack.removeLast(), _chain(out));
  }
  final w = _compress(out.cv, out.block, 0, out.blen, out.flags | _root);
  final res = Uint8List(32);
  final bd = ByteData.view(res.buffer);
  for (var i = 0; i < 8; i++) {
    bd.setUint32(i * 4, w[i], Endian.little);
  }
  return res;
}

/// What Cloudflare Pages wants as a file's name: the first 32 hex characters of the hash.
String blake3Hex32(Uint8List data) {
  final h = blake3(data);
  final sb = StringBuffer();
  for (var i = 0; i < 16; i++) {
    sb.write(h[i].toRadixString(16).padLeft(2, '0'));
  }
  return sb.toString();
}
