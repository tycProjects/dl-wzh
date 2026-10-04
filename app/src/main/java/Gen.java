import java.util.Vector;

/** Builds a map from an "address" (seed text) and a difficulty. Same address + difficulty = same map. */
public class Gen {
    private long s;

    public Gen(long seed) {
        s = (seed ^ 0x5DEECE66DL) & ((1L << 48) - 1);
        next();
        next();
    }

    public int next() {
        s = (s * 0x5DEECE66DL + 0xBL) & ((1L << 48) - 1);
        return (int) (s >>> 17);
    }

    public int rnd(int n) {
        return next() % n;
    }

    public int range(int a, int b) {
        return a + rnd(b - a + 1);
    }

    public static long seedOf(String addr) {
        long h = 1125899906842597L;
        for (int i = 0; i < addr.length(); i++) {
            h = 31L * h + addr.charAt(i);
        }
        return h;
    }

    static final int[] SKIES = { 0x9ED8FF, 0xA8E0FF, 0xFFC890, 0xB0C8F0, 0x8FD0C0 };
    static final int[] GRASS = { 0x5BA34A, 0x6AAF50, 0x4F9A5A, 0x8AAA48, 0x4A9A40 };

    /** Segments are stored as { x0, x1, groundY, hasGround(1/0) }. */
    private static int groundAt(Vector segs, int x) {
        for (int i = 0; i < segs.size(); i++) {
            int[] g = (int[]) segs.elementAt(i);
            if (x >= g[0] && x < g[1]) {
                return g[3] == 1 ? g[2] : -1;
            }
        }
        return -1;
    }

    public static World make(String name, int diff, String address, String[] npcNames, String[] foodNames) {
        Gen g = new Gen(seedOf(address) + diff * 7919L);
        World w = new World();
        w.name = name;
        w.w = 2400 + diff * 400;
        w.h = 240;
        w.sky = SKIES[g.rnd(SKIES.length)];
        int grass = GRASS[g.rnd(GRASS.length)];
        w.spawnX = 60;
        w.spawnY = 190;

        Vector rects = new Vector();   // int[] { x, y, w, h, color }
        Vector segs = new Vector();
        int x = 0;
        int gy = 208;
        int lastHouse = -1000;
        int lastFlag = 0;
        boolean lastGap = false;
        Vector flags = new Vector();
        flags.addElement(new int[] { 100, 208 });
        while (x < w.w) {
            int segW = g.range(7, 15) * 10;
            boolean big = x > 300 && x - lastHouse > 500 && g.rnd(100) < 30 && w.w - x > 330;
            if (big) {
                segW = g.range(21, 26) * 10;
            }
            if (x == 0) {
                segW = 220;
            }
            if (x + segW > w.w) {
                segW = w.w - x;
            }
            boolean gap = !lastGap && x - lastHouse > 330 && x > 300 && !big && diff > 0 && g.rnd(100) < diff * 14 && x + segW < w.w - 200;
            lastGap = gap;
            if (gap) {
                int gw = g.range(34, 56);
                segs.addElement(new int[] { x, x + gw, gy, 0 });
                x += gw;
                continue;
            }
            rects.addElement(new int[] { x, gy, segW, 240 - gy, grass });
            segs.addElement(new int[] { x, x + segW, gy, 1 });
            if (big) {
                int hx = x + 76;
                int top = gy - 60;
                rects.addElement(new int[] { hx, top, 8, 60, 0xA0522D });
                rects.addElement(new int[] { hx + 112, top, 8, 12, 0xA0522D });
                rects.addElement(new int[] { hx - 4, top - 8, 128, 8, 0x8B3A3A });
                rects.addElement(new int[] { hx - 74, gy - 20, 30, 20, 0x8A8A8A });
                rects.addElement(new int[] { hx - 42, gy - 40, 30, 40, 0x8A8A8A });
                lastHouse = x;
            } else if (x > 300 && g.rnd(100) < 40) {
                int pw = g.range(40, 70);
                if (segW - pw >= 50) {
                    int px = x + 25 + g.range(0, segW - pw - 50);
                    int py = gy - (g.range(48, 52) / 2) * 2;
                    rects.addElement(new int[] { px, py, pw, 8, 0x8A8A8A });
                }
            }
            // decoration
            if (!big && g.rnd(100) < 60) {
                int t = g.rnd(3);
                w.deco.addElement(new int[] { t, x + g.range(10, Math.max(11, segW - 10)), gy });
            }
            if (x - lastFlag > 650 && segW >= 90) {
                flags.addElement(new int[] { x + segW / 2, gy });
                lastFlag = x;
            }
            x += segW;
            int ngy = gy + g.range(-2, 2) * 8;
            if (ngy < 152) {
                ngy = 152;
            }
            if (ngy > 216) {
                ngy = 216;
            }
            gy = ngy;
        }
        flags.addElement(new int[] { w.w - 60, gy });
        // make sure the last segment is wide and flat enough for the final flag
        rects.addElement(new int[] { w.w - 100, gy, 100, 240 - gy, grass });
        segs.addElement(new int[] { w.w - 100, w.w, gy, 1 });

        w.n = rects.size();
        w.rx = new int[w.n];
        w.ry = new int[w.n];
        w.rw = new int[w.n];
        w.rh = new int[w.n];
        w.rc = new int[w.n];
        for (int i = 0; i < w.n; i++) {
            int[] r = (int[]) rects.elementAt(i);
            w.rx[i] = r[0];
            w.ry[i] = r[1];
            w.rw[i] = r[2];
            w.rh[i] = r[3];
            w.rc[i] = r[4];
        }
        w.nf = flags.size();
        w.fx = new int[w.nf];
        w.fy = new int[w.nf];
        for (int i = 0; i < w.nf; i++) {
            int[] f = (int[]) flags.elementAt(i);
            w.fx[i] = f[0];
            w.fy[i] = f[1];
        }

        // NPCs
        if (npcNames.length > 0) {
            int count = 3 + diff * 2 + g.rnd(2);
            for (int i = 0; i < count; i++) {
                for (int tries = 0; tries < 20; tries++) {
                    int nx = g.range(450, w.w - 150);
                    int ny = groundAt(segs, nx);
                    if (ny > 0 && groundAt(segs, nx - 12) > 0 && groundAt(segs, nx + 12) > 0) {
                        w.npcs.addElement(new Object[] { npcNames[g.rnd(npcNames.length)], new Integer(nx),
                                new Integer(ny - 1) });
                        break;
                    }
                }
            }
        }
        // food lying around
        if (foodNames.length > 0) {
            int count = 5 - diff;
            for (int i = 0; i < count; i++) {
                for (int tries = 0; tries < 20; tries++) {
                    int ix = g.range(250, w.w - 100);
                    int iy = groundAt(segs, ix);
                    if (iy > 0) {
                        w.items.addElement(new Object[] { foodNames[g.rnd(foodNames.length)], new Integer(ix),
                                new Integer(iy - 6) });
                        break;
                    }
                }
            }
        }
        return w;
    }
}
