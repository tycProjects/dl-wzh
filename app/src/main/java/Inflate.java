/** Minimal DEFLATE decoder (RFC 1951), after Mark Adler's "puff". CLDC friendly. */
public class Inflate {
    private byte[] in;
    private int inPos;
    private int inEnd;
    private int bitBuf;
    private int bitCnt;
    private byte[] out;
    private int outPos;

    private static final short[] LBASE = { 3, 4, 5, 6, 7, 8, 9, 10, 11, 13, 15, 17, 19, 23, 27, 31, 35, 43, 51, 59,
            67, 83, 99, 115, 131, 163, 195, 227, 258 };
    private static final short[] LEXT = { 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 2, 2, 2, 2, 3, 3, 3, 3, 4, 4, 4, 4, 5,
            5, 5, 5, 0 };
    private static final short[] DBASE = { 1, 2, 3, 4, 5, 7, 9, 13, 17, 25, 33, 49, 65, 97, 129, 193, 257, 385, 513,
            769, 1025, 1537, 2049, 3073, 4097, 6145, 8193, 12289, 16385, 24577 };
    private static final short[] DEXT = { 0, 0, 0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 6, 7, 7, 8, 8, 9, 9, 10, 10, 11,
            11, 12, 12, 13, 13 };
    private static final byte[] ORDER = { 16, 17, 18, 0, 8, 7, 9, 6, 10, 5, 11, 4, 12, 3, 13, 2, 14, 1, 15 };

    private static class Huff {
        short[] count = new short[16];
        short[] symbol = new short[288];
    }

    /** Returns the decompressed bytes (exactly outSize) or null on any error. */
    public static byte[] inflate(byte[] src, int off, int len, int outSize) {
        try {
            Inflate z = new Inflate();
            z.in = src;
            z.inPos = off;
            z.inEnd = off + len;
            z.out = new byte[outSize];
            z.run();
            if (z.outPos != outSize) {
                return null;
            }
            return z.out;
        } catch (Throwable t) {
            return null;
        }
    }

    private int bits(int need) {
        int val = bitBuf;
        while (bitCnt < need) {
            if (inPos >= inEnd) {
                throw new RuntimeException("eof");
            }
            val |= (in[inPos++] & 0xFF) << bitCnt;
            bitCnt += 8;
        }
        bitBuf = val >> need;
        bitCnt -= need;
        return val & ((1 << need) - 1);
    }

    private int decode(Huff h) {
        int code = 0;
        int first = 0;
        int index = 0;
        for (int len = 1; len <= 15; len++) {
            code |= bits(1);
            int count = h.count[len];
            if (code - count < first) {
                return h.symbol[index + (code - first)];
            }
            index += count;
            first += count;
            first <<= 1;
            code <<= 1;
        }
        throw new RuntimeException("bad code");
    }

    private int construct(Huff h, short[] length, int off, int n) {
        for (int len = 0; len <= 15; len++) {
            h.count[len] = 0;
        }
        for (int s = 0; s < n; s++) {
            h.count[length[off + s]]++;
        }
        if (h.count[0] == n) {
            return 0;
        }
        int left = 1;
        for (int len = 1; len <= 15; len++) {
            left <<= 1;
            left -= h.count[len];
            if (left < 0) {
                return left;
            }
        }
        short[] offs = new short[16];
        offs[1] = 0;
        for (int len = 1; len < 15; len++) {
            offs[len + 1] = (short) (offs[len] + h.count[len]);
        }
        for (int s = 0; s < n; s++) {
            if (length[off + s] != 0) {
                h.symbol[offs[length[off + s]]++] = (short) s;
            }
        }
        return left;
    }

    private void codes(Huff lencode, Huff distcode) {
        for (;;) {
            int sym = decode(lencode);
            if (sym < 256) {
                if (outPos >= out.length) {
                    throw new RuntimeException("overflow");
                }
                out[outPos++] = (byte) sym;
            } else if (sym == 256) {
                return;
            } else {
                sym -= 257;
                if (sym >= 29) {
                    throw new RuntimeException("bad len");
                }
                int len = LBASE[sym] + bits(LEXT[sym]);
                int ds = decode(distcode);
                if (ds >= 30) {
                    throw new RuntimeException("bad dist");
                }
                int dist = DBASE[ds] + bits(DEXT[ds]);
                if (dist > outPos || outPos + len > out.length) {
                    throw new RuntimeException("bad copy");
                }
                while (len-- > 0) {
                    out[outPos] = out[outPos - dist];
                    outPos++;
                }
            }
        }
    }

    private void stored() {
        bitBuf = 0;
        bitCnt = 0;
        if (inPos + 4 > inEnd) {
            throw new RuntimeException("eof");
        }
        int len = (in[inPos] & 0xFF) | ((in[inPos + 1] & 0xFF) << 8);
        int nlen = (in[inPos + 2] & 0xFF) | ((in[inPos + 3] & 0xFF) << 8);
        inPos += 4;
        if (len != (~nlen & 0xFFFF)) {
            throw new RuntimeException("bad stored");
        }
        if (inPos + len > inEnd || outPos + len > out.length) {
            throw new RuntimeException("overflow");
        }
        System.arraycopy(in, inPos, out, outPos, len);
        inPos += len;
        outPos += len;
    }

    private void fixed() {
        short[] lengths = new short[320];
        int s = 0;
        for (; s < 144; s++) {
            lengths[s] = 8;
        }
        for (; s < 256; s++) {
            lengths[s] = 9;
        }
        for (; s < 280; s++) {
            lengths[s] = 7;
        }
        for (; s < 288; s++) {
            lengths[s] = 8;
        }
        Huff lc = new Huff();
        construct(lc, lengths, 0, 288);
        for (s = 0; s < 30; s++) {
            lengths[s] = 5;
        }
        Huff dc = new Huff();
        construct(dc, lengths, 0, 30);
        codes(lc, dc);
    }

    private void dynamic() {
        int nlen = bits(5) + 257;
        int ndist = bits(5) + 1;
        int ncode = bits(4) + 4;
        if (nlen > 286 || ndist > 30) {
            throw new RuntimeException("bad counts");
        }
        short[] lengths = new short[320];
        int idx = 0;
        for (; idx < ncode; idx++) {
            lengths[ORDER[idx]] = (short) bits(3);
        }
        for (; idx < 19; idx++) {
            lengths[ORDER[idx]] = 0;
        }
        Huff lencode = new Huff();
        if (construct(lencode, lengths, 0, 19) != 0) {
            throw new RuntimeException("bad code lengths");
        }
        idx = 0;
        short[] ll = new short[320];
        while (idx < nlen + ndist) {
            int sym = decode(lencode);
            if (sym < 16) {
                ll[idx++] = (short) sym;
            } else {
                int len = 0;
                int rep;
                if (sym == 16) {
                    if (idx == 0) {
                        throw new RuntimeException("no prev");
                    }
                    len = ll[idx - 1];
                    rep = 3 + bits(2);
                } else if (sym == 17) {
                    rep = 3 + bits(3);
                } else {
                    rep = 11 + bits(7);
                }
                if (idx + rep > nlen + ndist) {
                    throw new RuntimeException("too many");
                }
                while (rep-- > 0) {
                    ll[idx++] = (short) len;
                }
            }
        }
        Huff lc = new Huff();
        construct(lc, ll, 0, nlen);
        Huff dc = new Huff();
        construct(dc, ll, nlen, ndist);
        codes(lc, dc);
    }

    private void run() {
        int last;
        do {
            last = bits(1);
            int type = bits(2);
            if (type == 0) {
                stored();
            } else if (type == 1) {
                fixed();
            } else if (type == 2) {
                dynamic();
            } else {
                throw new RuntimeException("bad block");
            }
        } while (last == 0);
    }
}
