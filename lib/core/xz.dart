import 'dart:typed_data';

/// A small .xz (LZMA2) decoder, so unpacking Termux's .deb files does not depend on any
/// library or phone tool. Port of a reference decoder that was checked against `xz` on single
/// and multi block files with CRC32 and CRC64.
Uint8List xzDecode(Uint8List d) {
  if (d.length < 32 || d[0] != 0xFD || d[1] != 0x37 || d[2] != 0x7A || d[3] != 0x58 || d[4] != 0x5A || d[5] != 0) {
    throw const FormatException('not an .xz file');
  }
  final check = d[7] & 0x0F;
  final csz = switch (check) { 0 => 0, 1 => 4, 4 => 8, 10 => 32, _ => throw FormatException('unsupported xz check type $check') };
  final out = _Out(d.length * 6 + 65536);
  var p = 12;
  while (true) {
    if (p >= d.length) throw const FormatException('xz data is cut short');
    if (d[p] == 0) break; // index: the blocks are done
    final hs = (d[p] + 1) * 4;
    final hdr = p;
    final flags = d[p + 1];
    var q = p + 2;
    if (flags & 0x40 != 0) q = _varint(d, q).$2;
    if (flags & 0x80 != 0) q = _varint(d, q).$2;
    for (var i = 0; i < (flags & 3) + 1; i++) {
      final f = _varint(d, q);
      final n = _varint(d, f.$2);
      q = n.$2 + n.$1;
      if (f.$1 != 0x21) throw FormatException('unsupported xz filter ${f.$1}');
    }
    p = hdr + hs;
    final start = p;
    final lz = _Lz(out, out.len);
    while (true) {
      final c = d[p++];
      if (c == 0) break;
      if (c < 0x80) {
        if (c > 2) throw const FormatException('bad xz chunk');
        final n = ((d[p] << 8) | d[p + 1]) + 1;
        p += 2;
        out.addRange(d, p, p + n);
        p += n;
        continue;
      }
      final u = ((c & 0x1F) << 16) + (d[p] << 8) + d[p + 1] + 1;
      final pk = (d[p + 2] << 8) + d[p + 3] + 1;
      p += 4;
      final mode = (c >> 5) & 3;
      if (mode >= 2) {
        lz.setProps(d[p]);
        p++;
      }
      if (mode >= 1) lz.resetState();
      lz.chunk(d, p, u);
      p += pk;
    }
    p += (4 - ((p - start) % 4)) % 4;
    p += csz;
  }
  return Uint8List.sublistView(out.buf, 0, out.len);
}

(int, int) _varint(Uint8List d, int p) {
  var r = 0, sh = 0;
  while (true) {
    final b = d[p++];
    r |= (b & 0x7F) << sh;
    sh += 7;
    if (b < 0x80) return (r, p);
  }
}

class _Out {
  Uint8List buf;
  int len = 0;
  _Out(int cap) : buf = Uint8List(cap < 1024 ? 1024 : cap);
  void _need(int n) {
    if (len + n <= buf.length) return;
    var c = buf.length * 2;
    while (c < len + n) {
      c *= 2;
    }
    final nb = Uint8List(c);
    nb.setRange(0, len, buf);
    buf = nb;
  }

  void add(int b) {
    _need(1);
    buf[len++] = b;
  }

  void addRange(Uint8List s, int from, int to) {
    _need(to - from);
    buf.setRange(len, len + (to - from), s, from);
    len += to - from;
  }
}

class _Rc {
  final Uint8List d;
  int p;
  int range = 0xFFFFFFFF;
  int code = 0;
  _Rc(this.d, int p0) : p = p0 + 1 {
    for (var i = 0; i < 4; i++) {
      code = (code << 8) | d[p++];
    }
  }

  int bit(Uint16List probs, int i) {
    if (range < 0x1000000) {
      range = (range << 8) & 0xFFFFFFFF;
      code = ((code << 8) | d[p++]) & 0xFFFFFFFF;
    }
    final pr = probs[i];
    final bound = (range >> 11) * pr;
    if (code < bound) {
      range = bound;
      probs[i] = pr + ((2048 - pr) >> 5);
      return 0;
    }
    range -= bound;
    code -= bound;
    probs[i] = pr - (pr >> 5);
    return 1;
  }

  int direct(int n) {
    var r = 0;
    for (var i = 0; i < n; i++) {
      if (range < 0x1000000) {
        range = (range << 8) & 0xFFFFFFFF;
        code = ((code << 8) | d[p++]) & 0xFFFFFFFF;
      }
      range >>= 1;
      var b = 0;
      if (code >= range) {
        code -= range;
        b = 1;
      }
      r = (r << 1) | b;
    }
    return r;
  }
}

Uint16List _probs(int n) => Uint16List(n)..fillRange(0, n, 1024);

int _tree(_Rc rc, Uint16List probs, int base, int nbits) {
  var m = 1;
  for (var i = 0; i < nbits; i++) {
    m = (m << 1) | rc.bit(probs, base + m);
  }
  return m - (1 << nbits);
}

int _rtree(_Rc rc, Uint16List probs, int base, int nbits) {
  var m = 1, r = 0;
  for (var i = 0; i < nbits; i++) {
    final b = rc.bit(probs, base + m);
    m = (m << 1) | b;
    r |= b << i;
  }
  return r;
}

class _Len {
  final choice = _probs(2);
  final low = _probs(16 * 8);
  final mid = _probs(16 * 8);
  final high = _probs(256);
  int dec(_Rc rc, int ps) {
    if (rc.bit(choice, 0) == 0) return _tree(rc, low, ps * 8, 3);
    if (rc.bit(choice, 1) == 0) return 8 + _tree(rc, mid, ps * 8, 3);
    return 16 + _tree(rc, high, 0, 8);
  }
}

class _Lz {
  final _Out o;
  final int dictStart;
  int lc = 0, lp = 0, pb = 0;
  int state = 0, r0 = 0, r1 = 0, r2 = 0, r3 = 0;
  late Uint16List isMatch, isRep, g0, g1, g2, rep0long, slot, spec, align, lit;
  late _Len ln, rl;
  _Lz(this.o, this.dictStart);

  void setProps(int b) {
    pb = b ~/ 45;
    b %= 45;
    lp = b ~/ 9;
    lc = b % 9;
  }

  void resetState() {
    state = 0;
    r0 = r1 = r2 = r3 = 0;
    isMatch = _probs(12 * 16);
    isRep = _probs(12);
    g0 = _probs(12);
    g1 = _probs(12);
    g2 = _probs(12);
    rep0long = _probs(12 * 16);
    slot = _probs(4 * 64);
    spec = _probs(115);
    align = _probs(16);
    ln = _Len();
    rl = _Len();
    lit = _probs(0x300 << (lc + lp));
  }

  void chunk(Uint8List d, int p, int usize) {
    final rc = _Rc(d, p);
    final end = o.len + usize;
    final pbm = (1 << pb) - 1, lpm = (1 << lp) - 1;
    while (o.len < end) {
      final pos = o.len - dictStart;
      final ps = pos & pbm;
      final st = state;
      if (rc.bit(isMatch, (st << 4) + ps) == 0) {
        final prev = o.len > dictStart ? o.buf[o.len - 1] : 0;
        final base = 0x300 * (((pos & lpm) << lc) + (prev >> (8 - lc)));
        var sym = 1;
        if (st >= 7) {
          var mb = o.buf[o.len - 1 - r0];
          while (sym < 0x100) {
            final m = (mb >> 7) & 1;
            mb = (mb << 1) & 0xFF;
            final b = rc.bit(lit, base + ((1 + m) << 8) + sym);
            sym = (sym << 1) | b;
            if (m != b) break;
          }
        }
        while (sym < 0x100) {
          sym = (sym << 1) | rc.bit(lit, base + sym);
        }
        o.add(sym & 0xFF);
        state = st < 4 ? 0 : (st < 10 ? st - 3 : st - 6);
        continue;
      }
      int len;
      if (rc.bit(isRep, st) == 0) {
        r3 = r2;
        r2 = r1;
        r1 = r0;
        len = ln.dec(rc, ps);
        state = st < 7 ? 7 : 10;
        final ls = len < 4 ? len : 3;
        final s = _tree(rc, slot, ls * 64, 6);
        if (s < 4) {
          r0 = s;
        } else {
          final nb = (s >> 1) - 1;
          var dist = (2 | (s & 1)) << nb;
          if (s < 14) {
            dist += _rtree(rc, spec, dist - s, nb);
          } else {
            dist += rc.direct(nb - 4) << 4;
            dist += _rtree(rc, align, 0, 4);
          }
          r0 = dist;
          if (r0 == 0xFFFFFFFF) break;
        }
        len += 2;
      } else {
        if (rc.bit(g0, st) == 0) {
          if (rc.bit(rep0long, (st << 4) + ps) == 0) {
            state = st < 7 ? 9 : 11;
            o.add(o.buf[o.len - 1 - r0]);
            continue;
          }
        } else {
          int dist;
          if (rc.bit(g1, st) == 0) {
            dist = r1;
          } else {
            if (rc.bit(g2, st) == 0) {
              dist = r2;
            } else {
              dist = r3;
              r3 = r2;
            }
            r2 = r1;
          }
          r1 = r0;
          r0 = dist;
        }
        len = rl.dec(rc, ps) + 2;
        state = st < 7 ? 8 : 11;
      }
      for (var i = 0; i < len; i++) {
        o.add(o.buf[o.len - 1 - r0]);
      }
    }
  }
}
