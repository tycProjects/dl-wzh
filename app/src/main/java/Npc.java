/** An NPC / mob with a small "brain": wander, chase, keep distance, flee, hop obstacles, dodge. */
public class Npc {
    public Fighter f = new Fighter();
    public NpcDef def;
    public Tool tool;
    public boolean counted;
    private int sx, sy;
    private int wanderDir;
    private int dirTimer;
    private int reactT;
    private int diff;
    private long rs;

    public Npc(NpcDef d, Tool t, int x, int y, int diff, int seed) {
        def = d;
        tool = t;
        sx = x;
        sy = y;
        this.diff = diff;
        rs = seed * 7919L + 17L;
        f.team = 1;
        f.color = d.color;
        f.headR = d.head;
        f.maxHp = d.hp + diff * 10;
        f.speedMul = d.speed / 100f;
        f.tool = t;
        respawn();
    }

    public void respawn() {
        f.place(sx, sy);
        counted = false;
        reactT = 20;
        dirTimer = 0;
    }

    private float rnd() {
        rs = (rs * 1103515245L + 12345L) & 0x7FFFFFFFL;
        return ((rs >> 8) & 0xFFFF) / 65536f;
    }

    private boolean ranged() {
        return tool.kind == Tool.SHOT || tool.kind == Tool.BOMB || tool.kind == Tool.KICK;
    }

    public void update(World w, Fighter p, MsvCanvas cv) {
        if (f.dead) {
            f.deadTimer++;
            if (f.rag != null) {
                f.rag.step(w);
            }
            if (f.deadTimer > 240) {
                respawn();
            }
            return;
        }
        float dx = p.x - f.x;
        float dy = p.y - f.y;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        boolean see = !p.dead && dist < 190 + def.brain * 40 && (dy > -80f && dy < 80f || def.brain >= 2);
        int dir = 0;
        boolean jump = false;
        int toward = dx >= 0f ? 1 : -1;
        boolean flee = def.brain >= 2 && f.hp * 100 < f.maxHp * 30 && see;
        if (see) {
            f.face = toward;
            if (flee) {
                dir = -toward;
            } else if (!ranged()) {
                if (dist > tool.range - 6) {
                    dir = toward;
                }
            } else {
                if (dist < 70f) {
                    dir = -toward;
                } else if (dist > 150f) {
                    dir = toward;
                }
            }
            reactT--;
            if (reactT <= 0 && f.cooldown <= 0 && (!flee || def.brain >= 3)) {
                float err = (0.38f - 0.14f * diff - 0.04f * def.brain) * (rnd() - 0.5f) * 2f;
                if (err < 0f) {
                    err = -err * 0.8f * (rnd() < 0.5f ? 1f : -1f);
                }
                boolean ok;
                if (ranged()) {
                    ok = dist < 220f && dy > -90f && dy < 90f;
                } else {
                    ok = dist < tool.range + 4;
                }
                if (ok) {
                    float ax = dx / (dist < 1f ? 1f : dist);
                    float ay = dy / (dist < 1f ? 1f : dist);
                    if (tool.kind == Tool.BOMB) {
                        ay -= 0.35f;
                    }
                    float c = (float) Math.cos(err);
                    float sn = (float) Math.sin(err);
                    float bx = ax * c - ay * sn;
                    float by = ax * sn + ay * c;
                    float bl = (float) Math.sqrt(bx * bx + by * by);
                    cv.npcFire(f, tool, bx / bl, by / bl);
                    reactT = 24 - diff * 7 - def.brain * 2;
                    if (reactT < 4) {
                        reactT = 4;
                    }
                }
            }
        } else {
            dirTimer--;
            if (dirTimer <= 0) {
                int r = (int) (rnd() * 3f);
                wanderDir = r - 1;
                dirTimer = 40 + (int) (rnd() * 80f);
            }
            dir = wanderDir;
        }
        if (dir != 0 && f.onGround) {
            float ahead = f.x + dir * 14;
            boolean wall = w.solid(ahead, f.y - 10);
            boolean ground = false;
            for (int k = 6; k <= 30 && !ground; k += 6) {
                if (w.solid(ahead, f.y + k)) {
                    ground = true;
                }
            }
            if (wall) {
                jump = true;
            } else if (!ground) {
                boolean landing = false;
                for (int k = 6; k <= 30 && !landing; k += 6) {
                    if (w.solid(ahead + dir * 42, f.y + k)) {
                        landing = true;
                    }
                }
                if (def.brain >= 2 && see && landing) {
                    jump = true;
                } else {
                    dir = 0;
                    if (!see) {
                        wanderDir = -wanderDir;
                    }
                }
            }
        }
        if (see && !flee && dy < -30f && dist < 100f && f.onGround && def.brain >= 2) {
            jump = true;
        }
        if (def.brain >= 3 && f.onGround && cv.incoming(f)) {
            jump = true;
        }
        f.control(dir, jump);
        f.physics(w);
        if (f.y > w.h + 100) {
            respawn();
        }
    }
}
