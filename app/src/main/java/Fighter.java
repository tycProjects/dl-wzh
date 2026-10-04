import javax.microedition.lcdui.Graphics;

/** A stick figure: head, torso, belly, upper arms, forearms, thighs, shins. Player, NPC or remote player. */
public class Fighter {
    public static final int W = 10;
    public static final int H = 38;
    public static final int HEAD = 0, NECK = 1, MID = 2, HIP = 3, LELB = 4, RELB = 5,
            LHAND = 6, RHAND = 7, LKNEE = 8, RKNEE = 9, LFOOT = 10, RFOOT = 11;
    static final float GRAV = 0.5f;
    static final float PI = 3.14159f;
    static final float MAXV = 2.6f;

    public float x, y, vx, vy;
    public int face = 1;
    public boolean onGround;
    public float phase;
    public int hp = 100;
    public int maxHp = 100;
    public boolean dead;
    public int deadTimer;
    public Ragdoll rag;
    public boolean limp;           // voluntary ragdoll (R key)
    public int team;               // 0 local player, 1 NPCs, 2+ remote players
    public int color = 0x202020;
    public int headR = 5;
    public int hatColor = -1;
    public int attackT;
    public int attackMax = 8;
    public int kickT;
    public float kdx = 1f, kdy = 0f;
    public int cooldown;
    public int flash;
    public int lastDmg;
    public int sinceHurt;
    public boolean crouch;
    public float speedMul = 1f;
    public float jumpV = -8f;
    public float ax = 1f, ay = 0f;
    public Tool tool;
    public boolean hookOn;
    public float hookX, hookY;
    public float tx, ty;   // network target position (remote players)
    public float[] jx = new float[12];
    public float[] jy = new float[12];
    private float[] la = new float[2];
    private float[] lb = new float[2];

    public static int r(float v) {
        return (int) (v + 4000.5f) - 4000;
    }

    public void place(float px, float py) {
        x = px;
        y = py;
        vx = 0f;
        vy = 0f;
        dead = false;
        limp = false;
        rag = null;
        hp = maxHp;
        deadTimer = 0;
        attackT = 0;
        kickT = 0;
        cooldown = 0;
        flash = 0;
        onGround = false;
        crouch = false;
        hookOn = false;
        lastDmg = 0;
        sinceHurt = 0;
    }

    public void control(int dir, boolean jump) {
        float maxv = MAXV * speedMul * ((crouch && onGround) ? 0.5f : 1f);
        if (dir != 0) {
            face = dir > 0 ? 1 : -1;
            vx += dir * (onGround ? 0.7f : 0.45f) * speedMul;
            if (dir > 0 && vx > maxv) {
                vx = maxv;
            }
            if (dir < 0 && vx < -maxv) {
                vx = -maxv;
            }
        } else {
            vx *= onGround ? 0.7f : 0.96f;
        }
        if (jump && onGround && !crouch) {
            vy = jumpV;
            onGround = false;
        }
    }

    public void physics(World w) {
        if (cooldown > 0) {
            cooldown--;
        }
        if (attackT > 0) {
            attackT--;
        }
        if (kickT > 0) {
            kickT--;
        }
        if (flash > 0) {
            flash--;
        }
        sinceHurt++;
        vy += GRAV;
        if (vy > 11f) {
            vy = 11f;
        }
        x += vx;
        if (x < W / 2) {
            x = W / 2;
            vx = 0f;
        }
        if (x > w.w - W / 2) {
            x = w.w - W / 2;
            vx = 0f;
        }
        for (int i = 0; i < w.n; i++) {
            float rx = w.rx[i];
            float ry = w.ry[i];
            float rr = rx + w.rw[i];
            float rb = ry + w.rh[i];
            if (x + 5 > rx && x - 5 < rr && y > ry && y - H < rb) {
                if (vx > 0f) {
                    x = rx - 5;
                } else if (vx < 0f) {
                    x = rr + 5;
                }
                vx = 0f;
            }
        }
        y += vy;
        onGround = false;
        for (int i = 0; i < w.n; i++) {
            float rx = w.rx[i];
            float ry = w.ry[i];
            float rr = rx + w.rw[i];
            float rb = ry + w.rh[i];
            if (x + 5 > rx && x - 5 < rr && y > ry && y - H < rb) {
                if (vy >= 0f) {
                    y = ry;
                    onGround = true;
                } else {
                    y = rb + H;
                }
                vy = 0f;
            }
        }
        if (onGround) {
            float sp = vx < 0f ? -vx : vx;
            phase += sp * 0.13f;
        }
    }

    public void hurt(int dmg, float kx, float ky) {
        if (dead) {
            return;
        }
        hp -= dmg;
        lastDmg += dmg;
        sinceHurt = 0;
        flash = 10;
        if (!limp) {
            vx += kx;
            vy += ky;
            if (ky < 0f) {
                onGround = false;
            }
        } else if (rag != null) {
            for (int i = 0; i < Ragdoll.N; i++) {
                rag.ox[i] -= kx * 0.6f;
                rag.oy[i] -= ky * 0.6f;
            }
        }
        if (hp <= 0) {
            hp = 0;
            die();
        }
    }

    public void die() {
        if (dead) {
            return;
        }
        if (!limp || rag == null) {
            computePose();
            rag = new Ragdoll(jx, jy, vx, vy);
        }
        limp = false;
        dead = true;
        deadTimer = 0;
    }

    /** Voluntary ragdoll: go limp. */
    public void goLimp() {
        if (dead || limp) {
            return;
        }
        computePose();
        rag = new Ragdoll(jx, jy, vx, vy);
        limp = true;
        crouch = false;
        hookOn = false;
    }

    public void syncFromRag() {
        if (rag == null) {
            return;
        }
        x = rag.px[HIP];
        float f = rag.py[LFOOT] > rag.py[RFOOT] ? rag.py[LFOOT] : rag.py[RFOOT];
        y = f;
    }

    public void getUp() {
        if (!limp || rag == null) {
            return;
        }
        syncFromRag();
        y -= 1f;
        vx = 0f;
        vy = 0f;
        rag = null;
        limp = false;
        onGround = false;
    }

    /** Fills jx/jy with the 12 joint positions for the current state. */
    public void computePose() {
        float speed = vx < 0f ? -vx : vx;
        float amp = speed / MAXV;
        if (amp > 1f) {
            amp = 1f;
        }
        boolean cr = crouch && onGround;
        if (cr) {
            amp = 0f;
        }
        float f = face;
        float drop = 0f;
        for (int s = 0; s < 2; s++) {
            float ph = phase + s * PI;
            float a;
            float b;
            if (cr) {
                a = 1.0f;
                b = -0.3f;
            } else if (onGround) {
                a = (float) Math.sin(ph) * 0.8f * amp;
                b = a - (float) Math.abs(Math.sin(ph + 1.2)) * 0.5f * amp;
            } else {
                a = (s == 0) ? 0.7f : -0.4f;
                b = a - 0.5f;
            }
            la[s] = a;
            lb[s] = b;
            float d = (float) Math.cos(a) * 8f + (float) Math.cos(b) * 8f;
            if (d > drop) {
                drop = d;
            }
        }
        float hy = y - drop;
        float lean = amp * 1.5f + (cr ? 3f : 0f);
        float nx = x + f * lean;
        jx[HIP] = x;
        jy[HIP] = hy;
        jx[MID] = x;
        jy[MID] = hy - 4f;
        jx[NECK] = nx;
        jy[NECK] = hy - 12f;
        jx[HEAD] = nx + f * lean * 0.3f;
        jy[HEAD] = hy - 12f - headR - 1;
        for (int s = 0; s < 2; s++) {
            int ki = LKNEE + s;
            int fi = LFOOT + s;
            jx[ki] = x + (float) Math.sin(la[s]) * 8f * f;
            jy[ki] = hy + (float) Math.cos(la[s]) * 8f;
            jx[fi] = jx[ki] + (float) Math.sin(lb[s]) * 8f * f;
            jy[fi] = jy[ki] + (float) Math.cos(lb[s]) * 8f;
        }
        if (kickT > 0) {
            jx[RKNEE] = x + kdx * 8f;
            jy[RKNEE] = hy + kdy * 8f;
            jx[RFOOT] = x + kdx * 16f;
            jy[RFOOT] = hy + kdy * 16f;
        }
        float sx = nx;
        float sy = jy[NECK] + 1f;
        for (int s = 0; s < 2; s++) {
            int ei = LELB + s;
            int hi = LHAND + s;
            boolean armed = (s == 1) && tool != null;
            float dx = 0f;
            float dy = 0f;
            boolean aimed = false;
            if (armed && attackT > 0 && tool.kind != Tool.KICK) {
                dx = ax;
                dy = ay;
                if (tool.kind == Tool.MELEE) {
                    float prog = 1f - (float) attackT / attackMax;
                    float th = (-0.9f + 1.8f * prog) * (ax >= 0f ? 1f : -1f);
                    float c = (float) Math.cos(th);
                    float sn = (float) Math.sin(th);
                    dx = ax * c - ay * sn;
                    dy = ax * sn + ay * c;
                }
                aimed = true;
            } else if (armed && (tool.kind == Tool.SHOT || tool.kind == Tool.HOOK) && onGround && !cr) {
                dx = f;
                dy = 0.1f;
                aimed = true;
            }
            if (aimed) {
                jx[ei] = sx + dx * 6f;
                jy[ei] = sy + dy * 6f;
                jx[hi] = sx + dx * 12f;
                jy[hi] = sy + dy * 12f;
            } else {
                float aa;
                if (onGround) {
                    aa = -(float) Math.sin(phase + s * PI) * 0.7f * amp + 0.2f;
                } else {
                    aa = 2.6f - s * 0.6f;
                }
                jx[ei] = sx + (float) Math.sin(aa) * 6f * f;
                jy[ei] = sy + (float) Math.cos(aa) * 6f;
                jx[hi] = jx[ei] + (float) Math.sin(aa + 0.3) * 6f * f;
                jy[hi] = jy[ei] + (float) Math.cos(aa + 0.3) * 6f;
            }
        }
    }

    public static void bone(Graphics g, float x1, float y1, float x2, float y2, int cx, int cy) {
        int a = r(x1) - cx;
        int b = r(y1) - cy;
        int c = r(x2) - cx;
        int d = r(y2) - cy;
        g.drawLine(a, b, c, d);
        g.drawLine(a + 1, b, c + 1, d);
    }

    public void draw(Graphics g, int cx, int cy) {
        int col = color;
        if (flash > 0 && (flash & 2) == 0) {
            col = 0xFF3030;
        }
        if ((dead || limp) && rag != null) {
            rag.draw(g, cx, cy, col, headR);
            if (hatColor >= 0) {
                drawHat(g, r(rag.px[0]) - cx, r(rag.py[0]) - cy);
            }
            return;
        }
        computePose();
        g.setColor(col);
        bone(g, jx[NECK], jy[NECK], jx[MID], jy[MID], cx, cy);
        bone(g, jx[MID], jy[MID], jx[HIP], jy[HIP], cx, cy);
        bone(g, jx[HIP], jy[HIP], jx[LKNEE], jy[LKNEE], cx, cy);
        bone(g, jx[LKNEE], jy[LKNEE], jx[LFOOT], jy[LFOOT], cx, cy);
        bone(g, jx[HIP], jy[HIP], jx[RKNEE], jy[RKNEE], cx, cy);
        bone(g, jx[RKNEE], jy[RKNEE], jx[RFOOT], jy[RFOOT], cx, cy);
        bone(g, jx[NECK], jy[NECK], jx[LELB], jy[LELB], cx, cy);
        bone(g, jx[LELB], jy[LELB], jx[LHAND], jy[LHAND], cx, cy);
        bone(g, jx[NECK], jy[NECK], jx[RELB], jy[RELB], cx, cy);
        bone(g, jx[RELB], jy[RELB], jx[RHAND], jy[RHAND], cx, cy);
        int hx = r(jx[HEAD]) - cx;
        int hy = r(jy[HEAD]) - cy;
        g.fillArc(hx - headR, hy - headR, headR * 2, headR * 2, 0, 360);
        g.setColor(0xFFFFFF);
        g.fillRect(hx + face * 2 - 1, hy - 1, 2, 2);
        if (hatColor >= 0) {
            drawHat(g, hx, hy);
        }
        if (tool != null) {
            if (tool.kind == Tool.KICK) {
                g.setColor(tool.color);
                g.fillRect(r(jx[LFOOT]) - cx - 2, r(jy[LFOOT]) - cy - 1, 6, 3);
                g.fillRect(r(jx[RFOOT]) - cx - 2, r(jy[RFOOT]) - cy - 1, 6, 3);
            } else if (tool.kind == Tool.FOOD) {
                g.setColor(tool.color);
                g.fillRect(r(jx[RHAND]) - cx - 2, r(jy[RHAND]) - cy - 2, 5, 5);
            } else if (tool.kind != Tool.HAT) {
                float dx = jx[RHAND] - jx[RELB];
                float dy = jy[RHAND] - jy[RELB];
                float d = (float) Math.sqrt(dx * dx + dy * dy);
                if (d < 0.1f) {
                    dx = face;
                    dy = 0f;
                    d = 1f;
                }
                dx /= d;
                dy /= d;
                int ln = tool.kind == Tool.HOOK ? 5 : tool.len;
                if (ln > 0) {
                    int x1 = r(jx[RHAND]) - cx;
                    int y1 = r(jy[RHAND]) - cy;
                    int x2 = x1 + r(dx * ln);
                    int y2 = y1 + r(dy * ln);
                    g.setColor(tool.color);
                    g.drawLine(x1, y1, x2, y2);
                    g.drawLine(x1, y1 + 1, x2, y2 + 1);
                }
            }
        }
        if (hookOn) {
            g.setColor(0xB0B0B0);
            g.drawLine(r(jx[RHAND]) - cx, r(jy[RHAND]) - cy, r(hookX) - cx, r(hookY) - cy);
            g.fillRect(r(hookX) - cx - 1, r(hookY) - cy - 1, 3, 3);
        }
    }

    private void drawHat(Graphics g, int hx, int hy) {
        g.setColor(hatColor);
        g.fillRect(hx - headR - 1, hy - headR - 1, headR * 2 + 3, 2);
        g.fillRect(hx - headR + 1, hy - headR - 5, headR * 2 - 1, 5);
    }
}
