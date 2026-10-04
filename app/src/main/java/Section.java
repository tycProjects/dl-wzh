import java.util.Vector;

/** One [section] of a .sbc file: an ordered list of key=value pairs. */
public class Section {
    public String type;
    public Vector keys = new Vector();
    public Vector vals = new Vector();

    public Section(String type) {
        this.type = type;
    }

    public void add(String k, String v) {
        keys.addElement(k);
        vals.addElement(v);
    }

    public String get(String key, String def) {
        for (int i = 0; i < keys.size(); i++) {
            if (key.equals((String) keys.elementAt(i))) {
                return (String) vals.elementAt(i);
            }
        }
        return def;
    }

    public Vector getAll(String key) {
        Vector r = new Vector();
        for (int i = 0; i < keys.size(); i++) {
            if (key.equals((String) keys.elementAt(i))) {
                r.addElement(vals.elementAt(i));
            }
        }
        return r;
    }

    public int getInt(String key, int def) {
        return Sbc.num(get(key, null), def);
    }

    public int getColor(String key, int def) {
        return Sbc.hex(get(key, null), def);
    }
}
