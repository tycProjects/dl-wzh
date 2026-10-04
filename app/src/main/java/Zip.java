import java.util.Vector;

/** Reads .zip files (stored or deflated entries) held in memory. */
public class Zip {
    static int u16(byte[] b, int o) {
        return (b[o] & 255) | ((b[o + 1] & 255) << 8);
    }

    static int u32(byte[] b, int o) {
        return u16(b, o) | (u16(b, o + 2) << 16);
    }

    /** Returns a Vector of ZipEntry (empty if the data is not a valid zip). */
    public static Vector list(byte[] z) {
        Vector out = new Vector();
        try {
            int eocd = -1;
            for (int i = z.length - 22; i >= 0 && i >= z.length - 65557; i--) {
                if (u32(z, i) == 0x06054b50) {
                    eocd = i;
                    break;
                }
            }
            if (eocd < 0) {
                return out;
            }
            int count = u16(z, eocd + 10);
            int p = u32(z, eocd + 16);
            for (int n = 0; n < count; n++) {
                if (p < 0 || p + 46 > z.length || u32(z, p) != 0x02014b50) {
                    break;
                }
                ZipEntry e = new ZipEntry();
                e.method = u16(z, p + 10);
                e.csize = u32(z, p + 20);
                e.usize = u32(z, p + 24);
                int nl = u16(z, p + 28);
                int el = u16(z, p + 30);
                int cl = u16(z, p + 32);
                e.offset = u32(z, p + 42);
                if (p + 46 + nl > z.length) {
                    break;
                }
                try {
                    e.name = new String(z, p + 46, nl, "UTF-8");
                } catch (Exception ex) {
                    e.name = new String(z, p + 46, nl);
                }
                out.addElement(e);
                p += 46 + nl + el + cl;
            }
        } catch (Throwable t) {
            // return what we have
        }
        return out;
    }

    /** Returns the file data, or null (unsupported method, corrupt, or bigger than 256 KB). */
    public static byte[] read(byte[] z, ZipEntry e) {
        try {
            if (e.usize < 0 || e.usize > 262144 || e.csize < 0) {
                return null;
            }
            int lp = e.offset;
            if (lp < 0 || lp + 30 > z.length || u32(z, lp) != 0x04034b50) {
                return null;
            }
            int ds = lp + 30 + u16(z, lp + 26) + u16(z, lp + 28);
            if (ds + e.csize > z.length) {
                return null;
            }
            if (e.method == 0) {
                byte[] r = new byte[e.usize];
                System.arraycopy(z, ds, r, 0, e.usize);
                return r;
            }
            if (e.method == 8) {
                return Inflate.inflate(z, ds, e.csize, e.usize);
            }
        } catch (Throwable t) {
            // fall through
        }
        return null;
    }
}
