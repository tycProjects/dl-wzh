import java.util.Vector;

/** Tiny parser for the .sbc add-on format (INI-like text). */
public class Sbc {

    /** Parses text into a Vector of Section. Lines: [type], key=value, # comment. */
    public static Vector parse(String text) {
        Vector out = new Vector();
        if (text == null) {
            return out;
        }
        Section cur = null;
        int n = text.length();
        int i = 0;
        while (i < n) {
            int j = text.indexOf('\n', i);
            if (j < 0) {
                j = n;
            }
            String line = text.substring(i, j).trim();
            i = j + 1;
            if (line.length() > 0 && line.charAt(0) == '\uFEFF') {
                line = line.substring(1).trim();
            }
            if (line.length() == 0) {
                continue;
            }
            char c0 = line.charAt(0);
            if (c0 == '#' || c0 == ';') {
                continue;
            }
            if (c0 == '[') {
                int e = line.indexOf(']');
                String name = (e > 0 ? line.substring(1, e) : line.substring(1)).trim().toLowerCase();
                cur = new Section(name);
                out.addElement(cur);
                continue;
            }
            int eq = line.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            if (cur == null) {
                cur = new Section("addon");
                out.addElement(cur);
            }
            cur.add(line.substring(0, eq).trim().toLowerCase(), line.substring(eq + 1).trim());
        }
        return out;
    }

    public static int num(String s, int def) {
        if (s == null) {
            return def;
        }
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return def;
        }
    }

    public static int hex(String s, int def) {
        if (s == null) {
            return def;
        }
        try {
            String t = s.trim();
            if (t.startsWith("#")) {
                t = t.substring(1);
            }
            if (t.startsWith("0x") || t.startsWith("0X")) {
                t = t.substring(2);
            }
            return Integer.parseInt(t, 16);
        } catch (Exception e) {
            return def;
        }
    }

    /** Splits on a separator char, trimming each token. */
    public static String[] split(String s, char sep) {
        if (s == null) {
            return new String[0];
        }
        Vector v = new Vector();
        int start = 0;
        for (int i = 0; i <= s.length(); i++) {
            if (i == s.length() || s.charAt(i) == sep) {
                v.addElement(s.substring(start, i).trim());
                start = i + 1;
            }
        }
        String[] r = new String[v.size()];
        for (int k = 0; k < r.length; k++) {
            r[k] = (String) v.elementAt(k);
        }
        return r;
    }
}
