import javax.microedition.lcdui.Graphics;

/** Verlet ragdoll with 12 joints (head, neck, belly, hip, elbows, hands, knees, feet). */
public class Ragdoll {
    static final int N = 12;
    // joint indices (same as Fighter): 0 head,1 neck,2 mid,3 hip,4/5 elbows,6/7 hands,8/9 knees,10/11 feet
    static final int[] CA = { 0, 1, 2, 3, 8, 3, 9, 1, 4, 1, 5, 0, 1, 8 };
    static final int[] CB = { 1, 2, 3, 8, 10, 9, 11, 4, 6, 5, 7, 2, 3, 9 };
    static final float[] CL = { 6f, 8f, 4f, 8f, 8f, 8f, 8f, 6f, 6f, 6f, 6f, 14f, 12f, 4f };

    float[] px = new float[N];
    float[] py = new float[N];
    float[] ox = new float[N];
    float[] oy = new float[N];

    public Ragdoll(float[] jx, float[] jy, float vx, float vy) {
        for (int i = 0; i < N; i++) {
            px[i] = jx[i];
            py[i] = jy[i];
            ox[i] = px[i] - vx - (i % 3 - 1) * 0.5f;
            oy[i] = py[i] - vy;
        }
    }

    static float lim(float v) {
        if (v > 7f) {
            return 7f;
        }
        if (v < -7f) {
            return -7f;
        }
        return v;
    }

    public void step(World w) {
        for (int i = 0; i < N; i++) {
            float vx = lim((px[i] - ox[i]) * 0.985f);
            float vy = lim((py[i] - oy[i]) * 0.985f + 0.45f);
            ox[i] = px[i];
            oy[i] = py[i];
            px[i] += vx;
            py[i] += vy;
        }
        for (int it = 0; it < 5; it++) {
            for (int k = 0; k < CA.length; k++) {
                int a = CA[k];
                int b = CB[k];
                float dx = px[b] - px[a];
                float dy = py[b] - py[a];
                float d = (float) Math.sqrt(dx * dx + dy * dy);
                if (d < 0.0001f) {
                    continue;
                }
                float diff = (d - CL[k]) / d * 0.5f;
                px[a] += dx * diff;
                py[a] += dy * diff;
                px[b] -= dx * diff;
                py[b] -= dy * diff;
            }
            for (int i = 0; i < N; i++) {
                collide(w, i);
            }
        }
    }

    private void collide(World w, int i) {
        if (px[i] < 2f) {
            px[i] = 2f;
        }
        if (px[i] > w.w - 2) {
            px[i] = w.w - 2;
        }
        for (int r = 0; r < w.n; r++) {
            float rx = w.rx[r];
            float ry = w.ry[r];
            float rr = rx + w.rw[r];
            float rb = ry + w.rh[r];
            float x = px[i];
            float y = py[i];
            if (x > rx && x < rr && y > ry && y < rb) {
                float dl = x - rx;
                float dr = rr - x;
                float dt = y - ry;
                float db = rb - y;
                float m = dl;
                int side = 0;
                if (dr < m) {
                    m = dr;
                    side = 1;
                }
                if (dt < m) {
                    m = dt;
                    side = 2;
                }
                if (db < m) {
                    m = db;
                    side = 3;
                }
                if (side == 0) {
                    px[i] = rx;
                } else if (side == 1) {
                    px[i] = rr;
                } else if (side == 2) {
                    py[i] = ry;
                    oy[i] = py[i];
                    ox[i] = px[i] - (px[i] - ox[i]) * 0.5f;
                } else {
                    py[i] = rb;
                    oy[i] = py[i];
                }
            }
        }
    }

    public void draw(Graphics g, int cx, int cy, int color, int headR) {
        g.setColor(color);
        Fighter.bone(g, px[1], py[1], px[2], py[2], cx, cy);
        Fighter.bone(g, px[2], py[2], px[3], py[3], cx, cy);
        Fighter.bone(g, px[3], py[3], px[8], py[8], cx, cy);
        Fighter.bone(g, px[8], py[8], px[10], py[10], cx, cy);
        Fighter.bone(g, px[3], py[3], px[9], py[9], cx, cy);
        Fighter.bone(g, px[9], py[9], px[11], py[11], cx, cy);
        Fighter.bone(g, px[1], py[1], px[4], py[4], cx, cy);
        Fighter.bone(g, px[4], py[4], px[6], py[6], cx, cy);
        Fighter.bone(g, px[1], py[1], px[5], py[5], cx, cy);
        Fighter.bone(g, px[5], py[5], px[7], py[7], cx, cy);
        g.fillArc(Fighter.r(px[0]) - cx - headR, Fighter.r(py[0]) - cy - headR, headR * 2, headR * 2, 0, 360);
    }
}
