import java.util.Vector;

/** Map: solid rectangles, flags (checkpoints), spawn point. Built from a [map] section. */
public class World {
    public String name = "Baseplate";
    public int w = 1000;
    public int h = 240;
    public int n = 0;
    public int[] rx = new int[0];
    public int[] ry = new int[0];
    public int[] rw = new int[0];
    public int[] rh = new int[0];
    public int[] rc = new int[0];
    public int nf = 0;
    public int[] fx = new int[0];
    public int[] fy = new int[0];
    public int spawnX = 60;
    public int spawnY = 190;
    public int sky = 0x9ED8FF;
    public Vector deco = new Vector();   // int[] { type(0 tree,1 bush,2 rock), x, y(base) }
    public Vector npcs = new Vector();   // Object[] { String name, Integer x, Integer y }
    public Vector items = new Vector();  // Object[] { String name, Integer x, Integer y }

    static int clampInt(int v, int lo, int hi) {
        if (v < lo) {
            return lo;
        }
        if (v > hi) {
            return hi;
        }
        return v;
    }

    public static World fromSection(Section s) {
        World m = new World();
        m.name = s.get("name", "Map");
        String[] sz = Sbc.split(s.get("size", "1000,240"), ',');
        if (sz.length >= 2) {
            m.w = clampInt(Sbc.num(sz[0], 1000), 320, 4000);
            m.h = clampInt(Sbc.num(sz[1], 240), 240, 1000);
        }
        String[] sp = Sbc.split(s.get("spawn", "60,190"), ',');
        if (sp.length >= 2) {
            m.spawnX = Sbc.num(sp[0], 60);
            m.spawnY = Sbc.num(sp[1], 190);
        }
        m.sky = s.getColor("sky", 0x9ED8FF);

        Vector rs = s.getAll("rect");
        m.rx = new int[rs.size()];
        m.ry = new int[rs.size()];
        m.rw = new int[rs.size()];
        m.rh = new int[rs.size()];
        m.rc = new int[rs.size()];
        int k = 0;
        for (int i = 0; i < rs.size(); i++) {
            String[] p = Sbc.split((String) rs.elementAt(i), ',');
            if (p.length < 4) {
                continue;
            }
            m.rx[k] = Sbc.num(p[0], 0);
            m.ry[k] = Sbc.num(p[1], 0);
            m.rw[k] = Sbc.num(p[2], 8);
            m.rh[k] = Sbc.num(p[3], 8);
            m.rc[k] = p.length >= 5 ? Sbc.hex(p[4], 0x7A5C3A) : 0x7A5C3A;
            k++;
        }
        m.n = k;

        Vector fs = s.getAll("flag");
        m.fx = new int[fs.size()];
        m.fy = new int[fs.size()];
        int q = 0;
        for (int i = 0; i < fs.size(); i++) {
            String[] p = Sbc.split((String) fs.elementAt(i), ',');
            if (p.length < 2) {
                continue;
            }
            m.fx[q] = Sbc.num(p[0], 0);
            m.fy[q] = Sbc.num(p[1], 208);
            q++;
        }
        m.nf = q;

        Vector ds = s.getAll("tree");
        for (int i = 0; i < ds.size(); i++) {
            String[] p = Sbc.split((String) ds.elementAt(i), ',');
            if (p.length >= 2) {
                m.deco.addElement(new int[] { 0, Sbc.num(p[0], 0), Sbc.num(p[1], 208) });
            }
        }
        Vector ns = s.getAll("npc");
        for (int i = 0; i < ns.size(); i++) {
            String[] p = Sbc.split((String) ns.elementAt(i), ',');
            if (p.length >= 3) {
                m.npcs.addElement(new Object[] { p[0], new Integer(Sbc.num(p[1], 0)), new Integer(Sbc.num(p[2], 0)) });
            }
        }
        Vector is = s.getAll("item");
        for (int i = 0; i < is.size(); i++) {
            String[] p = Sbc.split((String) is.elementAt(i), ',');
            if (p.length >= 3) {
                m.items.addElement(new Object[] { p[0], new Integer(Sbc.num(p[1], 0)), new Integer(Sbc.num(p[2], 0)) });
            }
        }
        return m;
    }

    /** Used only if no enabled add-on provides a map. */
    public static World fallback() {
        World m = new World();
        m.name = "Fallback";
        m.n = 1;
        m.rx = new int[] { 0 };
        m.ry = new int[] { 208 };
        m.rw = new int[] { 1000 };
        m.rh = new int[] { 32 };
        m.rc = new int[] { 0x5BA34A };
        return m;
    }

    public boolean solid(float x, float y) {
        for (int i = 0; i < n; i++) {
            if (x >= rx[i] && x < rx[i] + rw[i] && y >= ry[i] && y < ry[i] + rh[i]) {
                return true;
            }
        }
        return false;
    }
}
