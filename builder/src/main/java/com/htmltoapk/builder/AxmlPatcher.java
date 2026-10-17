package com.htmltoapk.builder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Edits a compiled (binary) AndroidManifest.xml without decompiling it.
 *
 * Strings: every string in the pool that exactly equals a key of {@code replacements} is
 * replaced and the pool is re-encoded (so the new string may have a different length).
 * Version code: the template uses a sentinel versionCode; its 4-byte value is overwritten.
 */
public final class AxmlPatcher {
    private static final int CHUNK_XML = 0x0003;
    private static final int CHUNK_STRING_POOL = 0x0001;
    private static final int FLAG_UTF8 = 0x100;

    private AxmlPatcher() {}

    public static byte[] patch(byte[] in, Map<String, String> replacements,
                               int oldVersionCode, int newVersionCode) throws IOException {
        ByteBuffer b = ByteBuffer.wrap(in).order(ByteOrder.LITTLE_ENDIAN);
        if (in.length < 36 || (b.getShort(0) & 0xffff) != CHUNK_XML) {
            throw new IOException("Template manifest is not binary XML");
        }
        final int poolPos = 8;
        if ((b.getShort(poolPos) & 0xffff) != CHUNK_STRING_POOL) {
            throw new IOException("Template manifest has no string pool");
        }
        int headerSize = b.getShort(poolPos + 2) & 0xffff;
        int poolSize = b.getInt(poolPos + 4);
        int stringCount = b.getInt(poolPos + 8);
        int styleCount = b.getInt(poolPos + 12);
        int flags = b.getInt(poolPos + 16);
        int stringsStart = b.getInt(poolPos + 20);
        if (styleCount != 0) throw new IOException("Styled string pool not supported");
        boolean utf8 = (flags & FLAG_UTF8) != 0;

        // 1. read + replace strings
        String[] strings = new String[stringCount];
        Set<String> found = new HashSet<>();
        for (int i = 0; i < stringCount; i++) {
            int off = b.getInt(poolPos + headerSize + 4 * i);
            String s = readString(in, b, poolPos + stringsStart + off, utf8);
            if (replacements.containsKey(s)) {
                found.add(s);
                s = replacements.get(s);
            }
            strings[i] = s;
        }
        for (String key : replacements.keySet()) {
            if (!found.contains(key)) throw new IOException("Template manifest is missing: " + key);
        }

        // 2. re-encode the pool
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        int[] offsets = new int[stringCount];
        for (int i = 0; i < stringCount; i++) {
            offsets[i] = data.size();
            writeString(data, strings[i], utf8);
        }
        while (data.size() % 4 != 0) data.write(0);
        int newStringsStart = headerSize + 4 * stringCount;
        int newPoolSize = newStringsStart + data.size();

        // 3. the remaining chunks (the XML tree) are copied as-is, except the version code
        int restStart = poolPos + poolSize;
        int restLen = in.length - restStart;
        byte[] rest = new byte[restLen];
        System.arraycopy(in, restStart, rest, 0, restLen);
        if (oldVersionCode != newVersionCode) patchInt(rest, oldVersionCode, newVersionCode);

        int total = 8 + newPoolSize + restLen;
        ByteBuffer out = ByteBuffer.allocate(total).order(ByteOrder.LITTLE_ENDIAN);
        out.putShort((short) CHUNK_XML).putShort((short) 8).putInt(total);
        out.put(in, poolPos, headerSize);
        out.putInt(poolPos + 4, newPoolSize);
        out.putInt(poolPos + 20, newStringsStart);
        out.putInt(poolPos + 24, 0);
        for (int off : offsets) out.putInt(off);
        out.put(data.toByteArray());
        out.put(rest);
        return out.array();
    }

    /** Overwrites the data word of the single Res_value {size=8, 0, TYPE_INT_DEC} holding oldV. */
    private static void patchInt(byte[] rest, int oldV, int newV) throws IOException {
        int hits = 0;
        int at = -1;
        for (int i = 0; i + 8 <= rest.length; i++) {
            if (rest[i] == 0x08 && rest[i + 1] == 0x00 && rest[i + 2] == 0x00 && rest[i + 3] == 0x10
                    && le(rest, i + 4) == oldV) {
                hits++;
                at = i + 4;
            }
        }
        if (hits != 1) throw new IOException("versionCode attribute not found (" + hits + ")");
        rest[at] = (byte) newV;
        rest[at + 1] = (byte) (newV >>> 8);
        rest[at + 2] = (byte) (newV >>> 16);
        rest[at + 3] = (byte) (newV >>> 24);
    }

    private static int le(byte[] a, int p) {
        return (a[p] & 0xff) | ((a[p + 1] & 0xff) << 8) | ((a[p + 2] & 0xff) << 16) | ((a[p + 3] & 0xff) << 24);
    }

    private static String readString(byte[] in, ByteBuffer b, int pos, boolean utf8) {
        if (utf8) {
            int p = pos;
            int chars = in[p++] & 0xff;
            if ((chars & 0x80) != 0) chars = ((chars & 0x7f) << 8) | (in[p++] & 0xff);
            int len = in[p++] & 0xff;
            if ((len & 0x80) != 0) len = ((len & 0x7f) << 8) | (in[p++] & 0xff);
            return new String(in, p, len, StandardCharsets.UTF_8);
        }
        int len = b.getShort(pos) & 0xffff;
        int p = pos + 2;
        if ((len & 0x8000) != 0) {
            len = ((len & 0x7fff) << 16) | (b.getShort(p) & 0xffff);
            p += 2;
        }
        return new String(in, p, len * 2, StandardCharsets.UTF_16LE);
    }

    private static void writeString(ByteArrayOutputStream out, String s, boolean utf8) throws IOException {
        if (utf8) {
            byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
            if (s.length() > 0x7fff || bytes.length > 0x7fff) throw new IOException("String too long");
            writeLen8(out, s.length());
            writeLen8(out, bytes.length);
            out.write(bytes);
            out.write(0);
        } else {
            int len = s.length();
            if (len > 0x7fff) {
                writeShort(out, 0x8000 | (len >>> 16));
                writeShort(out, len & 0xffff);
            } else {
                writeShort(out, len);
            }
            out.write(s.getBytes(StandardCharsets.UTF_16LE));
            writeShort(out, 0);
        }
    }

    private static void writeLen8(ByteArrayOutputStream out, int n) {
        if (n > 0x7f) {
            out.write((n >> 8) | 0x80);
            out.write(n & 0xff);
        } else {
            out.write(n);
        }
    }

    private static void writeShort(ByteArrayOutputStream out, int v) {
        out.write(v & 0xff);
        out.write((v >> 8) & 0xff);
    }
}
