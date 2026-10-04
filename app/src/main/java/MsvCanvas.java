import java.util.Vector;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

/**
 * Msv mai co dac xe - 320x240, Nokia E72.
 * Flow: logo + load -> main menu (mouse cursor) -> Play -> map list / create map -> load -> game.
 * Cursor = D-pad (in menus and in "D-pad" mode). L soft key cycles control modes: D-pad, D-pad2, Ban phim.
 * R soft key = back / exit. In game: * = exit game.
 */
public class MsvCanvas extends Canvas implements Runnable, TouchCanvas {
    static final int ST_LOAD = 0;
    static final int ST_MENU = 1;
    static final int ST_MAPS = 2;
    static final int ST_CREATE = 3;
    static final int ST_GEN = 4;
    static final int ST_PLAY = 5;
    static final int ST_ADDON = 6;
    static final int ST_NET = 7;
    static final int ST_PACK = 8;

    static final int K_NONE = 0, K_UP = 1, K_DOWN = 2, K_LEFT = 3, K_RIGHT = 4, K_OK = 5,
            K_SOFTL = 6, K_SOFTR = 7, K_MLEFT = 8, K_MRIGHT = 9, K_MJUMP = 10, K_USE = 11,
            K_PREV = 12, K_NEXT = 13, K_ADDON = 14, K_RESPAWN = 15, K_NET = 16, K_MDOWN = 17,
            K_RAGDOLL = 18, K_PACK = 19, K_EXIT = 20;

    static final int BG_SPLASH = 0xFBFAF8;
    static final int LOAD_FRAMES = 40;
    static final String[] CTRL_NAMES = { "D-pad", "D-pad2", "Ban phim" };

    private MsvMidlet midlet;
    private Thread thread;
    private boolean running;
    private Image buf;
    private Image splash;
    private Image logo;
    private boolean useBuf;
    private int sw = 320;
    private int sh = 240;
    private int state = ST_LOAD;
    private int loadTimer;
    private int genTimer;
    private int tickCount;
    private String err = "";
    private int errT;
    private Font fs;

    private AddonManager am = new AddonManager();
    private MapStore maps = new MapStore();
    private World world;
    private MapInfo curMap;
    private MapInfo pendingMap;
    private Vector tools = new Vector();
    private Vector npcDefs = new Vector();
    private Vector extraMaps = new Vector();
    private Inventory inv = new Inventory();
    private Fighter player = new Fighter();
    private Fighter menuFight = new Fighter();
    private boolean placed;
    private Vector npcs = new Vector();
    private Vector shots = new Vector();
    private Vector booms = new Vector();
    private Vector drops = new Vector();
    private Vector popups = new Vector();
    private int kills;

    private boolean kLeft, kRight, kJump, kDown, kUse, kOkHeld;
    private boolean dUp, dDown, dLeft, dRight;
    private int ctrlMode;
    private int curX = 200;
    private int curY = 100;
    private int curHold;
    private float aimX = 1f, aimY = 0f;
    private boolean confirmExit;
    private int camX, camY;
    private int cpX, cpY;
    private boolean[] flagHit = new boolean[0];
    private String toast = "";
    private int toastT;

    // hook
    private int hookState;
    private int hookT;
    private float hkx, hky, hkvx, hkvy;
    private int hookPull = 5;
    private int hookRange = 140;
    private Fighter hookTarget;

    // screens
    private Vector mapList = new Vector();
    private int mapSel;
    private int mapTop;
    private boolean confirmDel;
    private String cName = "";
    private String cAddr = "";
    private int cDiff;
    private int addonReturn = ST_MENU;
    private Vector menu = new Vector();
    private int menuSel, menuTop;
    private String menuMsg = "";
    private int packTab;
    private Tool carried;

    // network
    private Net net = new Net();
    private BtLink bt;
    private Fighter[] rf = new Fighter[Net.MAXP];
    private int netTick;
    private int netSel;
    private static final int[] PALETTE = { 0x202020, 0x2060D0, 0x20A040, 0xD08020 };

    private long rseed = 12345L;

    public MsvCanvas(MsvMidlet m) {
        midlet = m;
        try {
            setFullScreenMode(true);
        } catch (Throwable t) {
            // ignore
        }
        sw = getWidth();
        sh = getHeight();
        useBuf = !isDoubleBuffered();
        if (useBuf) {
            buf = Image.createImage(sw, sh);
        }
        try {
            splash = Image.createImage("/sibubalachu.png");
        } catch (Throwable t) {
            splash = null;
        }
        try {
            logo = Image.createImage("/logo.png");
        } catch (Throwable t) {
            logo = null;
        }
        fs = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_PLAIN, Font.SIZE_SMALL);
        player.color = 0x202020;
        player.team = 0;
        rseed = System.currentTimeMillis() & 0x7FFFFFFFL;
    }

    protected void sizeChanged(int w, int h) {
        sw = w;
        sh = h;
        if (useBuf) {
            buf = Image.createImage(w, h);
        }
    }

    public void start() {
        running = true;
        thread = new Thread(this);
        thread.start();
    }

    public void stop() {
        running = false;
        try {
            netStop(null);
        } catch (Throwable t) {
            // ignore
        }
    }

    private float rand() {
        rseed = (rseed * 1103515245L + 12345L) & 0x7FFFFFFFL;
        return ((rseed >> 8) & 0xFFFF) / 65536f;
    }

    // ------------------------------------------------------------------ main loop

    public void run() {
        while (running) {
            long t0 = System.currentTimeMillis();
            try {
                tick();
            } catch (Throwable e) {
                err = "" + e;
                errT = 150;
            }
            repaint();
            serviceRepaints();
            long dt = System.currentTimeMillis() - t0;
            long sl = 40 - dt;
            if (sl < 5) {
                sl = 5;
            }
            try {
                Thread.sleep(sl);
            } catch (InterruptedException ie) {
                // ignore
            }
        }
    }

    void tick() {
        if (errT > 0) {
            errT--;
            if (errT == 0) {
                err = "";
            }
        }
        if (toastT > 0) {
            toastT--;
        }
        tickCount++;
        if (state != ST_LOAD) {
            netUpdate();
        }
        switch (state) {
            case ST_LOAD:
                loadTimer++;
                if (loadTimer == 3) {
                    am.loadAll();
                } else if (loadTimer == 5) {
                    rebuild();
                } else if (loadTimer >= LOAD_FRAMES) {
                    state = ST_MENU;
                }
                break;
            case ST_MENU:
            case ST_MAPS:
            case ST_CREATE:
            case ST_PACK:
                moveCursor();
                menuFight.vx = 2.6f;
                menuFight.onGround = true;
                menuFight.phase += 0.32f;
                break;
            case ST_GEN:
                genTimer++;
                if (genTimer == 6) {
                    buildPending();
                }
                if (genTimer >= 36) {
                    state = world != null ? ST_PLAY : ST_MAPS;
                }
                break;
            case ST_PLAY:
                if (!confirmExit && world != null) {
                    updatePlay();
                }
                break;
            default:
                break;
        }
    }

    // ------------------------------------------------------------------ setup

    /** Reads tools / npc definitions / extra maps / character from the enabled add-ons. */
    private void rebuild() {
        tools.removeAllElements();
        npcDefs.removeAllElements();
        extraMaps.removeAllElements();
        Section charSec = null;
        for (int i = 0; i < am.addons.size(); i++) {
            Addon a = (Addon) am.addons.elementAt(i);
            if (!a.enabled) {
                continue;
            }
            for (int j = 0; j < a.sections.size(); j++) {
                Section s = (Section) a.sections.elementAt(j);
                if (s.type.equals("tool")) {
                    tools.addElement(Tool.fromSection(s));
                } else if (s.type.equals("npc")) {
                    npcDefs.addElement(NpcDef.fromSection(s));
                } else if (s.type.equals("map")) {
                    extraMaps.addElement(s);
                } else if (s.type.equals("character")) {
                    charSec = s;
                }
            }
        }
        if (charSec != null) {
            player.color = charSec.getColor("color", 0x202020);
            player.headR = World.clampInt(charSec.getInt("head", 5), 3, 8);
        } else {
            player.color = 0x202020;
            player.headR = 5;
        }
        menuFight.color = player.color;
        menuFight.headR = player.headR;
        menuFight.face = 1;
        menuFight.x = 68f;
        menuFight.y = 216f;
        menuFight.tool = findTool("Katana");
    }

    private Tool findTool(String name) {
        for (int i = 0; i < tools.size(); i++) {
            Tool t = (Tool) tools.elementAt(i);
            if (t.name.equalsIgnoreCase(name)) {
                return t;
            }
        }
        return null;
    }

    private NpcDef findNpc(String name) {
        for (int i = 0; i < npcDefs.size(); i++) {
            NpcDef d = (NpcDef) npcDefs.elementAt(i);
            if (d.name.equalsIgnoreCase(name)) {
                return d;
            }
        }
        return npcDefs.size() > 0 ? (NpcDef) npcDefs.elementAt(0) : null;
    }

    private Vector buildMapList() {
        Vector v = new Vector();
        for (int i = 0; i < extraMaps.size(); i++) {
            Section s = (Section) extraMaps.elementAt(i);
            v.addElement(new MapInfo(s.get("name", "Map"), 1, "(add-on)", s));
        }
        Vector mine = maps.list();
        for (int i = 0; i < mine.size(); i++) {
            v.addElement(mine.elementAt(i));
        }
        return v;
    }

    private void startMap(MapInfo mi) {
        netStop(null);
        pendingMap = mi;
        genTimer = 0;
        world = null;
        state = ST_GEN;
    }

    private String[] namesOf(int kind) {
        Vector v = new Vector();
        if (kind == 0) {
            for (int i = 0; i < npcDefs.size(); i++) {
                v.addElement(((NpcDef) npcDefs.elementAt(i)).name);
            }
        } else {
            for (int i = 0; i < tools.size(); i++) {
                Tool t = (Tool) tools.elementAt(i);
                if (t.kind == Tool.FOOD) {
                    v.addElement(t.name);
                }
            }
        }
        String[] r = new String[v.size()];
        for (int i = 0; i < r.length; i++) {
            r[i] = (String) v.elementAt(i);
        }
        return r;
    }

    private void buildPending() {
        MapInfo mi = pendingMap;
        World w;
        if (mi.sec != null) {
            w = World.fromSection(mi.sec);
        } else {
            w = Gen.make(mi.name, mi.diff, mi.address, namesOf(0), namesOf(1));
        }
        curMap = mi;
        enterWorld(w, mi.diff);
    }

    private void buildInventory() {
        inv.clear();
        String[] kit = { "Sung", "Dao", "Katana", "Bom", "Ban tia", "Moc", "Dep", "AK47", "Tay khong" };
        boolean[] used = new boolean[tools.size()];
        for (int k = 0; k < kit.length; k++) {
            for (int i = 0; i < tools.size(); i++) {
                Tool t = (Tool) tools.elementAt(i);
                if (!used[i] && t.name.equalsIgnoreCase(kit[k])) {
                    inv.hot[k] = t;
                    used[i] = true;
                    break;
                }
            }
        }
        for (int i = 0; i < tools.size(); i++) {
            if (!used[i]) {
                Tool t = (Tool) tools.elementAt(i);
                for (int c = 0; c < t.stock; c++) {
                    inv.addStore(t);
                }
            }
        }
    }

    private void enterWorld(World w, int diff) {
        world = w;
        flagHit = new boolean[w.nf];
        cpX = w.spawnX;
        cpY = w.spawnY;
        player.place(cpX, cpY);
        shots.removeAllElements();
        booms.removeAllElements();
        drops.removeAllElements();
        popups.removeAllElements();
        npcs.removeAllElements();
        hookState = 0;
        kills = 0;
        carried = null;
        buildInventory();
        for (int i = 0; i < w.npcs.size(); i++) {
            Object[] o = (Object[]) w.npcs.elementAt(i);
            NpcDef d = findNpc((String) o[0]);
            if (d == null) {
                continue;
            }
            Tool t = findTool(d.tool);
            if (t == null) {
                t = Tool.FIST;
            }
            npcs.addElement(new Npc(d, t, ((Integer) o[1]).intValue(), ((Integer) o[2]).intValue(), diff, i + 1));
        }
        for (int i = 0; i < w.items.size(); i++) {
            Object[] o = (Object[]) w.items.elementAt(i);
            Tool t = findTool((String) o[0]);
            if (t != null) {
                spawnDrop(t, ((Integer) o[1]).intValue(), ((Integer) o[2]).intValue(), 0f, 0f, 0);
            }
        }
        camX = World.clampInt((int) player.x - sw / 2, 0, Math.max(0, world.w - sw));
        camY = World.clampInt((int) player.y - (sh * 2) / 3, 0, Math.max(0, world.h - sh));
        placed = true;
    }

    private void showToast(String s) {
        toast = s;
        toastT = 40;
    }

    private void popup(float x, float y, String text, int color) {
        popups.addElement(new Object[] { new int[] { Fighter.r(x), Fighter.r(y), 0, color }, text });
    }

    private void spawnDrop(Tool t, float x, float y, float vx, float vy, int delay) {
        Pickup p = new Pickup();
        p.t = t;
        p.x = x;
        p.y = y;
        p.vx = vx;
        p.vy = vy;
        p.delay = delay;
        drops.addElement(p);
    }

    // ------------------------------------------------------------------ gameplay

    private Tool curTool() {
        return inv.current();
    }

    private void updatePlay() {
        // ---- inputs
        int dir = 0;
        boolean jump = kJump;
        boolean crouch = kDown;
        if (kLeft) {
            dir--;
        }
        if (kRight) {
            dir++;
        }
        if (ctrlMode == 1) {
            if (dLeft) {
                dir--;
            }
            if (dRight) {
                dir++;
            }
            if (dUp) {
                jump = true;
            }
            if (dDown) {
                crouch = true;
            }
        } else if (ctrlMode == 0) {
            moveCursor();
        }
        if (dir > 1) {
            dir = 1;
        }
        if (dir < -1) {
            dir = -1;
        }
        Tool ct = curTool();
        player.tool = ct;
        player.speedMul = ct.kind == Tool.KICK ? 1f + ct.speedBonus / 100f : 1f;
        player.jumpV = ct.kind == Tool.KICK ? -8f * (1f + ct.jumpBonus / 100f) : -8f;
        player.hatColor = inv.worn != null ? inv.worn.color : -1;
        player.crouch = crouch && !player.limp;
        if ((kUse || (kOkHeld && ct.auto)) && (ct.auto || kUse)) {
            if (kUse || ct.auto) {
                playerUse();
            }
        }

        // ---- player
        boolean selfPull = false;
        if (player.dead) {
            player.deadTimer++;
            if (player.rag != null) {
                player.rag.step(world);
            }
            if (player.deadTimer > 70) {
                respawnPlayer();
            }
        } else if (player.limp) {
            if (player.rag != null) {
                player.rag.step(world);
                player.syncFromRag();
            }
            player.sinceHurt++;
            if (player.cooldown > 0) {
                player.cooldown--;
            }
        } else {
            selfPull = updateHook(dir, crouch, jump);
            if (selfPull) {
                player.vx = pullX;
                player.vy = pullY;
            } else {
                player.control(dir, jump);
            }
            player.physics(world);
            if (player.y > world.h + 100) {
                respawnPlayer();
            }
            checkFlags();
        }
        popDamage(player);
        if (player.sinceHurt > 100 && player.hp < player.maxHp && !player.dead && (tickCount % 12) == 0) {
            player.hp++;
        }

        // ---- npcs
        if (!net.active()) {
            for (int i = 0; i < npcs.size(); i++) {
                Npc n = (Npc) npcs.elementAt(i);
                n.update(world, player, this);
                popDamage(n.f);
                if (n.f.dead && !n.counted) {
                    n.counted = true;
                    kills++;
                    popup(n.f.x, n.f.y - 44, "+1", 0xFFD000);
                    Vector foods = new Vector();
                    for (int k = 0; k < tools.size(); k++) {
                        if (((Tool) tools.elementAt(k)).kind == Tool.FOOD) {
                            foods.addElement(tools.elementAt(k));
                        }
                    }
                    if (foods.size() > 0 && rand() < 0.5f) {
                        spawnDrop((Tool) foods.elementAt((int) (rand() * foods.size())), n.f.x, n.f.y - 20, 0f, -3f, 20);
                    }
                }
            }
        }
        updateDrops();
        updateShots();
        updateBooms();
        for (int i = popups.size() - 1; i >= 0; i--) {
            int[] p = (int[]) ((Object[]) popups.elementAt(i))[0];
            p[2]++;
            if (p[2] > 28) {
                popups.removeElementAt(i);
            }
        }
        camX = World.clampInt((int) player.x - sw / 2, 0, Math.max(0, world.w - sw));
        camY = World.clampInt((int) player.y - (sh * 2) / 3, 0, Math.max(0, world.h - sh));
    }

    private void popDamage(Fighter f) {
        if (f.lastDmg > 0) {
            popup(f.x, f.y - 42, "-" + f.lastDmg, 0xFF3030);
            f.lastDmg = 0;
        }
    }

    private void moveCursor() {
        int dx = 0;
        int dy = 0;
        if (dLeft) {
            dx--;
        }
        if (dRight) {
            dx++;
        }
        if (dUp) {
            dy--;
        }
        if (dDown) {
            dy++;
        }
        if (dx != 0 || dy != 0) {
            curHold++;
            int sp = curHold > 8 ? 7 : 4;
            curX = World.clampInt(curX + dx * sp, 0, sw - 1);
            curY = World.clampInt(curY + dy * sp, 0, sh - 1);
        } else {
            curHold = 0;
        }
    }

    private void respawnPlayer() {
        player.place(cpX + net.myId * 12, cpY);
        hookState = 0;
        player.hookOn = false;
    }

    private void checkFlags() {
        for (int i = 0; i < world.nf; i++) {
            float dx = player.x - world.fx[i];
            if (!flagHit[i] && dx > -12f && dx < 12f && player.y > world.fy[i] - 40
                    && player.y < world.fy[i] + 8) {
                cpX = world.fx[i];
                cpY = world.fy[i] - 1;
                for (int j = 0; j < flagHit.length; j++) {
                    flagHit[j] = (j == i);
                }
                showToast("Luu moc!");
            }
        }
    }

    // ---- aiming / tool use

    private void computeAim() {
        boolean l = kLeft || (ctrlMode == 1 && dLeft);
        boolean r = kRight || (ctrlMode == 1 && dRight);
        boolean u = kJump || (ctrlMode == 1 && dUp);
        boolean d = kDown || (ctrlMode == 1 && dDown);
        float ax = 0f;
        float ay = 0f;
        if (r && !l) {
            ax = 1f;
        } else if (l && !r) {
            ax = -1f;
        }
        if (u && !d) {
            ay = -1f;
        } else if (d && !u) {
            ay = 1f;
        }
        if (ax == 0f && ay == 0f) {
            ax = player.face;
        }
        float len = (float) Math.sqrt(ax * ax + ay * ay);
        aimX = ax / len;
        aimY = ay / len;
    }

    private void playerUse() {
        if (player.dead || player.limp) {
            return;
        }
        Tool t = curTool();
        float sx = player.x + player.face * 3;
        float sy = player.y - 26;
        float ax;
        float ay;
        if (ctrlMode == 0) {
            float tx = curX + camX - sx;
            float ty = curY + camY - sy;
            float d = (float) Math.sqrt(tx * tx + ty * ty);
            if (d < 1f) {
                tx = player.face;
                ty = 0f;
                d = 1f;
            }
            ax = tx / d;
            ay = ty / d;
        } else {
            computeAim();
            ax = aimX;
            ay = aimY;
        }
        useToolDir(player, t, sx, sy, ax, ay);
    }

    /** Called by NPC brains. */
    void npcFire(Fighter f, Tool t, float ax, float ay) {
        useToolDir(f, t, f.x + f.face * 3, f.y - 26, ax, ay);
    }

    private int netToolIndex(Tool t) {
        return tools.indexOf(t);
    }

    private void useToolDir(Fighter f, Tool t, float sx, float sy, float ax, float ay) {
        if (f.dead || f.cooldown > 0) {
            return;
        }
        if (f == player) {
            if (t.kind == Tool.FOOD) {
                eatFood(t);
                return;
            }
            if (t.kind == Tool.HAT) {
                wearHat(t);
                return;
            }
        }
        f.ax = ax;
        f.ay = ay;
        if (ax > 0.15f) {
            f.face = 1;
        } else if (ax < -0.15f) {
            f.face = -1;
        }
        if (t.kind == Tool.HOOK) {
            if (f == player) {
                hookUse(sx, sy, ax, ay, t);
            }
            return;
        }
        if (!t.isAttack()) {
            return;
        }
        f.tool = t;
        f.cooldown = t.cooldown;
        f.attackT = f.attackMax;
        if (t.kind == Tool.KICK) {
            f.kickT = 10;
            f.kdx = ax;
            f.kdy = ay;
        }
        if (net.active() && f == player) {
            net.send(Net.fire(t.kind == Tool.MELEE ? Net.T_MELEE : Net.T_SHOT, net.myId, netToolIndex(t),
                    Net.rnd(sx), Net.rnd(sy), ax, ay));
        }
        doFire(f, t, sx, sy, ax, ay);
    }

    private void eatFood(Tool t) {
        if (player.hp >= player.maxHp) {
            showToast("Mau day roi!");
            return;
        }
        int before = player.hp;
        player.hp = Math.min(player.maxHp, player.hp + t.heal);
        popup(player.x, player.y - 42, "+" + (player.hp - before), 0x30C030);
        showToast("Ngon! " + t.name);
        inv.hot[inv.sel] = null;
        player.cooldown = 10;
    }

    private void wearHat(Tool t) {
        Tool old = inv.worn;
        inv.worn = t;
        inv.hot[inv.sel] = old;
        showToast("Doi non: " + t.name);
        player.cooldown = 10;
    }

    private void meleeCheck(Tool t, float sx, float sy, float ax, float ay, Fighter tg) {
        if (tg.dead) {
            return;
        }
        float dx = tg.x - sx;
        float dy = (tg.y - 19) - sy;
        float d = (float) Math.sqrt(dx * dx + dy * dy);
        if (d < t.range + 6 && (dx * ax + dy * ay) > -4f) {
            tg.hurt(t.damage, ax * t.knock, ay * t.knock - 1.5f);
        }
    }

    /** Applies one tool use (local, NPC or network): melee check or spawning bullets/bombs. */
    private void doFire(Fighter f, Tool t, float sx, float sy, float ax, float ay) {
        if (t.kind == Tool.MELEE) {
            if (f == player) {
                if (!net.active()) {
                    for (int i = 0; i < npcs.size(); i++) {
                        meleeCheck(t, sx, sy, ax, ay, ((Npc) npcs.elementAt(i)).f);
                    }
                }
            } else if (f.team == 1) {
                meleeCheck(t, sx, sy, ax, ay, player);
            } else {
                meleeCheck(t, sx, sy, ax, ay, player);
            }
            return;
        }
        int n = (t.kind == Tool.BOMB || t.kind == Tool.KICK) ? 1 : t.count;
        for (int i = 0; i < n; i++) {
            float ang = 0f;
            if (n > 1) {
                ang = (i - (n - 1) / 2f) * (t.spread / 100f);
            }
            float c = (float) Math.cos(ang);
            float sn = (float) Math.sin(ang);
            float dx = ax * c - ay * sn;
            float dy = ax * sn + ay * c;
            Shot st = new Shot();
            st.x = sx + dx * 8f;
            st.y = sy + dy * 8f;
            st.vx = dx * t.speed;
            st.vy = dy * t.speed;
            st.grav = t.grav / 100f;
            st.life = t.life;
            st.dmg = t.damage;
            st.color = t.color;
            st.owner = f;
            st.bomb = (t.kind == Tool.BOMB);
            st.slipper = (t.kind == Tool.KICK);
            st.width = t.width;
            st.pierce = t.pierce;
            if (st.slipper) {
                st.life = 60;
            }
            st.fuse = t.fuse;
            st.radius = t.radius;
            st.knock = t.knock;
            shots.addElement(st);
        }
    }

    /** True if a hostile bullet is flying toward f (used by smart NPCs to dodge). */
    boolean incoming(Fighter f) {
        for (int i = 0; i < shots.size(); i++) {
            Shot s = (Shot) shots.elementAt(i);
            if (s.owner == null || s.owner.team == f.team) {
                continue;
            }
            float dx = f.x - s.x;
            float dy = (f.y - 19) - s.y;
            if (dx * dx + dy * dy < 4900f && (dx * s.vx + dy * s.vy) > 0f) {
                return true;
            }
        }
        return false;
    }

    // ---- hook

    private float pullX, pullY;

    private void hookUse(float sx, float sy, float ax, float ay, Tool t) {
        if (hookState != 0) {
            hookState = 0;
            player.hookOn = false;
            player.cooldown = 6;
            return;
        }
        hookState = 1;
        hookT = 0;
        hkx = sx;
        hky = sy;
        hkvx = ax * t.speed;
        hkvy = ay * t.speed;
        hookTarget = null;
        hookPull = t.pull;
        hookRange = t.range > 20 ? t.range : 140;
        player.cooldown = 6;
        player.attackT = 6;
        player.hookOn = true;
        player.hookX = hkx;
        player.hookY = hky;
    }

    /** Returns true if the player is being pulled (pullX/pullY hold the velocity). */
    private boolean updateHook(int dir, boolean crouch, boolean jump) {
        if (hookState == 0) {
            player.hookOn = false;
            return false;
        }
        hookT++;
        float px = player.x;
        float py = player.y - 26;
        boolean self = false;
        if (hookState == 1) {
            for (int k = 0; k < 3 && hookState == 1; k++) {
                hkx += hkvx / 3f;
                hky += hkvy / 3f;
                if (world.solid(hkx, hky)) {
                    hookState = 2;
                    hookT = 0;
                } else if (!net.active()) {
                    for (int i = 0; i < npcs.size(); i++) {
                        Fighter f = ((Npc) npcs.elementAt(i)).f;
                        if (!f.dead && hkx > f.x - 6 && hkx < f.x + 6 && hky > f.y - Fighter.H && hky < f.y) {
                            hookState = 3;
                            hookTarget = f;
                            hookT = 0;
                        }
                    }
                }
            }
            float dx = hkx - px;
            float dy = hky - py;
            if (hookState == 1 && dx * dx + dy * dy > (float) hookRange * hookRange) {
                hookState = 0;
            }
        }
        if (hookState == 2) {
            float dx = hkx - px;
            float dy = hky - py;
            float d = (float) Math.sqrt(dx * dx + dy * dy);
            if (d < 12f || hookT > 80 || (jump && hookT > 3)) {
                hookState = 0;
            } else {
                pullX = dx / d * hookPull;
                pullY = dy / d * hookPull;
                self = true;
            }
        } else if (hookState == 3) {
            if (hookTarget == null || hookTarget.dead || hookT > 90) {
                hookState = 0;
            } else {
                hkx = hookTarget.x;
                hky = hookTarget.y - 19;
                float dx = hkx - px;
                float dy = hky - py;
                float d = (float) Math.sqrt(dx * dx + dy * dy);
                if (d < 16f) {
                    hookState = 0;
                } else {
                    boolean reverse = crouch || (dir != 0 && dir * dx < 0f);
                    if (reverse) {
                        hookTarget.vx = -dx / d * hookPull;
                        hookTarget.vy = -dy / d * hookPull * 0.6f - 0.5f;
                        hookTarget.onGround = false;
                    } else {
                        pullX = dx / d * hookPull;
                        pullY = dy / d * hookPull;
                        self = true;
                    }
                }
            }
        }
        player.hookOn = hookState != 0;
        player.hookX = hkx;
        player.hookY = hky;
        return self;
    }

    // ---- shots / drops

    private Fighter hitOne(Shot s, Fighter f) {
        float m = 5f + s.width / 2f;
        float hh = s.width / 2f;
        if (f != s.owner && !f.dead && (s.owner == null || f.team != s.owner.team) && !s.hits.contains(f)
                && s.x > f.x - m && s.x < f.x + m && s.y > f.y - Fighter.H - hh && s.y < f.y + hh) {
            return f;
        }
        return null;
    }

    private Fighter hitTest(Shot s) {
        Fighter f = hitOne(s, player);
        if (f != null) {
            return f;
        }
        if (!net.active()) {
            for (int i = 0; i < npcs.size(); i++) {
                f = hitOne(s, ((Npc) npcs.elementAt(i)).f);
                if (f != null) {
                    return f;
                }
            }
        }
        for (int i = 0; i < rf.length; i++) {
            if (rf[i] != null) {
                f = hitOne(s, rf[i]);
                if (f != null) {
                    return f;
                }
            }
        }
        return null;
    }

    private void updateShots() {
        for (int i = shots.size() - 1; i >= 0; i--) {
            Shot s = (Shot) shots.elementAt(i);
            boolean remove = false;
            if (s.bomb) {
                if (!s.rest) {
                    s.vy += s.grav;
                    float nx = s.x + s.vx;
                    float ny = s.y + s.vy;
                    if (world.solid(nx, s.y)) {
                        s.vx = 0f;
                        nx = s.x;
                    }
                    if (world.solid(nx, ny)) {
                        if (s.vy > 0f) {
                            s.rest = true;
                            s.vx = 0f;
                        }
                        s.vy = 0f;
                        ny = s.y;
                    }
                    s.x = nx;
                    s.y = ny;
                    if (s.y > world.h + 60) {
                        remove = true;
                    }
                }
                s.fuse--;
                if (s.fuse <= 0) {
                    explode(s);
                    remove = true;
                }
            } else {
                float sp = (float) Math.sqrt(s.vx * s.vx + s.vy * s.vy);
                int steps = (int) (sp / 5f) + 1;
                float stx = s.vx / steps;
                float sty = s.vy / steps;
                for (int k = 0; k < steps && !remove; k++) {
                    s.x += stx;
                    s.y += sty;
                    if (world.solid(s.x, s.y)) {
                        remove = true;
                    } else {
                        Fighter hit = hitTest(s);
                        if (hit != null) {
                            float d = sp < 0.1f ? 1f : sp;
                            if (hit == player || hit.team == 1) {
                                hit.hurt(s.dmg, s.vx / d * s.knock, s.vy / d * s.knock - 1f);
                            }
                            if (s.pierce) {
                                s.hits.addElement(hit);
                            } else {
                                remove = true;
                            }
                        }
                    }
                }
                s.vy += s.grav;
                s.life--;
                if (s.life <= 0 || s.x < -50 || s.x > world.w + 50 || s.y > world.h + 60 || s.y < -200) {
                    remove = true;
                }
            }
            if (remove) {
                shots.removeElementAt(i);
            }
        }
    }

    private void explode(Shot s) {
        booms.addElement(new int[] { (int) s.x, (int) s.y, s.radius, 0 });
        explodeHit(player, s);
        if (!net.active()) {
            for (int i = 0; i < npcs.size(); i++) {
                explodeHit(((Npc) npcs.elementAt(i)).f, s);
            }
        }
    }

    private void explodeHit(Fighter f, Shot s) {
        if (f.dead) {
            return;
        }
        if (s.owner != null && f != s.owner && f.team == s.owner.team) {
            return;
        }
        float dx = f.x - s.x;
        float dy = (f.y - 19) - s.y;
        float d = (float) Math.sqrt(dx * dx + dy * dy);
        if (d < s.radius) {
            if (d < 1f) {
                d = 1f;
            }
            int dmg = (int) (s.dmg * (1f - d / s.radius));
            if (dmg < 1) {
                dmg = 1;
            }
            f.hurt(dmg, dx / d * s.knock, dy / d * s.knock - 3f);
        }
    }

    private void updateBooms() {
        for (int i = booms.size() - 1; i >= 0; i--) {
            int[] b = (int[]) booms.elementAt(i);
            b[3]++;
            if (b[3] >= 10) {
                booms.removeElementAt(i);
            }
        }
    }

    private void updateDrops() {
        for (int i = drops.size() - 1; i >= 0; i--) {
            Pickup p = (Pickup) drops.elementAt(i);
            if (p.delay > 0) {
                p.delay--;
            }
            if (!p.ground) {
                p.vy += 0.4f;
                if (p.vy > 8f) {
                    p.vy = 8f;
                }
                float nx = p.x + p.vx;
                if (world.solid(nx, p.y)) {
                    p.vx = 0f;
                    nx = p.x;
                }
                float ny = p.y + p.vy;
                if (world.solid(nx, ny + 5)) {
                    p.vy = 0f;
                    p.vx = 0f;
                    p.ground = true;
                    ny = p.y;
                }
                p.x = nx;
                p.y = ny;
                if (p.y > world.h + 80) {
                    drops.removeElementAt(i);
                    continue;
                }
            }
            if (!player.dead && p.delay <= 0 && Math.abs(p.x - player.x) < 14f && p.y > player.y - 40f
                    && p.y < player.y + 8f) {
                if (inv.add(p.t)) {
                    showToast("Nhat: " + p.t.name);
                    drops.removeElementAt(i);
                }
            }
        }
    }

    private void dropItem(Tool t) {
        spawnDrop(t, player.x + player.face * 14, player.y - 24, player.face * 2f, -3f, 45);
    }

    private void selectTool(int delta) {
        inv.sel = (inv.sel + delta + Inventory.HOT) % Inventory.HOT;
        showToast(curTool().name);
        if (hookState != 0) {
            hookState = 0;
            player.hookOn = false;
        }
    }

    private void toggleLimp() {
        if (player.dead || world == null) {
            return;
        }
        if (player.limp) {
            player.getUp();
        } else {
            player.goLimp();
        }
    }

    // ------------------------------------------------------------------ input

    private int mapKey(int kc) {
        if (kc == -6 || kc == -21) {
            return K_SOFTL;
        }
        if (kc == -7 || kc == -22) {
            return K_SOFTR;
        }
        switch (kc) {
            case -1:
                return K_UP;
            case -2:
                return K_DOWN;
            case -3:
                return K_LEFT;
            case -4:
                return K_RIGHT;
            case -5:
                return K_OK;
            case '4':
            case 'a':
            case 'A':
                return K_MLEFT;
            case '6':
            case 'd':
            case 'D':
                return K_MRIGHT;
            case '2':
            case 'w':
            case 'W':
            case ' ':
                return K_MJUMP;
            case '8':
            case 's':
            case 'S':
                return K_MDOWN;
            case '5':
            case 'f':
            case 'F':
                return K_USE;
            case '1':
            case 'q':
            case 'Q':
                return K_PREV;
            case '3':
            case '#':
            case 'e':
            case 'E':
                return K_NEXT;
            case '7':
            case 'b':
            case 'B':
                return K_PACK;
            case 'm':
            case 'M':
                return K_ADDON;
            case '9':
                return K_NET;
            case '0':
                return K_RESPAWN;
            case 'r':
            case 'R':
                return K_RAGDOLL;
            case '*':
                return K_EXIT;
            default:
                break;
        }
        int ga = 0;
        try {
            ga = getGameAction(kc);
        } catch (IllegalArgumentException e) {
            ga = 0;
        }
        if (ga == UP) {
            return K_UP;
        }
        if (ga == DOWN) {
            return K_DOWN;
        }
        if (ga == LEFT) {
            return K_LEFT;
        }
        if (ga == RIGHT) {
            return K_RIGHT;
        }
        if (ga == FIRE) {
            return K_OK;
        }
        return K_NONE;
    }

    protected void keyPressed(int kc) {
        handleKey(mapKey(kc), true);
    }

    protected void keyReleased(int kc) {
        handleKey(mapKey(kc), false);
    }

    protected void keyRepeated(int kc) {
        if (state == ST_ADDON || state == ST_NET || state == ST_MAPS) {
            int k = mapKey(kc);
            if (k == K_UP || k == K_DOWN || k == K_MJUMP || k == K_MDOWN) {
                handleKey(k, true);
            }
        }
    }

    protected void hideNotify() {
        clearKeys();
    }

    private void clearKeys() {
        kLeft = false;
        kRight = false;
        kJump = false;
        kDown = false;
        kUse = false;
        kOkHeld = false;
        dUp = false;
        dDown = false;
        dLeft = false;
        dRight = false;
    }

    public void androidTouch(int x, int y, boolean down) {
        if (state != ST_PLAY) { if (down) click(x, y); return; }
        float nx = getWidth() == 0 ? 0f : x / (float)getWidth();
        float ny = getHeight() == 0 ? 0f : y / (float)getHeight();
        int k = K_NONE;
        if (ny > 0.62f) {
            if (nx < 0.22f) k = K_MLEFT;
            else if (nx < 0.44f) k = K_MRIGHT;
            else if (nx < 0.66f) k = K_USE;
            else k = K_MJUMP;
        } else if (nx > 0.82f && ny < 0.22f) {
            k = K_PACK;
        }
        if (k != K_NONE) handleKey(k, down);
    }

    void click(int x, int y) {
        curX = x;
        curY = y;
        handleKey(K_OK, true);
        handleKey(K_OK, false);
    }

    void handleKey(int k, boolean down) {
        if (k == K_NONE || state == ST_LOAD || state == ST_GEN) {
            return;
        }
        if (confirmExit) {
            if (down) {
                if (k == K_OK || k == K_USE || k == K_EXIT) {
                    midlet.exitApp();
                } else if (k == K_SOFTR || k == K_SOFTL) {
                    confirmExit = false;
                }
            }
            return;
        }
        if (k == K_EXIT) {
            if (down) {
                confirmExit = true;
            }
            return;
        }
        switch (k) {
            case K_UP:
                dUp = down;
                break;
            case K_DOWN:
                dDown = down;
                break;
            case K_LEFT:
                dLeft = down;
                break;
            case K_RIGHT:
                dRight = down;
                break;
            case K_MLEFT:
                kLeft = down;
                break;
            case K_MRIGHT:
                kRight = down;
                break;
            case K_MJUMP:
                kJump = down;
                break;
            case K_MDOWN:
                kDown = down;
                break;
            case K_USE:
                kUse = down;
                break;
            case K_OK:
                kOkHeld = down;
                break;
            default:
                break;
        }
        if (!down) {
            return;
        }
        switch (state) {
            case ST_MENU:
                if (k == K_OK) {
                    clickMenu();
                } else if (k == K_SOFTR) {
                    midlet.exitApp();
                }
                break;
            case ST_MAPS:
                mapsKey(k);
                break;
            case ST_CREATE:
                if (k == K_OK) {
                    clickCreate();
                } else if (k == K_SOFTR) {
                    state = ST_MAPS;
                }
                break;
            case ST_PACK:
                if (k == K_OK) {
                    packClick();
                } else if (k == K_SOFTR || k == K_PACK) {
                    closePack();
                }
                break;
            case ST_ADDON:
                menuKey(k);
                break;
            case ST_NET:
                netMenuKey(k);
                break;
            case ST_PLAY:
                playKey(k);
                break;
            default:
                break;
        }
    }

    private void playKey(int k) {
        switch (k) {
            case K_OK:
                if (ctrlMode == 0) {
                    if (!uiClick()) {
                        playerUse();
                    }
                } else {
                    playerUse();
                }
                break;
            case K_SOFTL:
                ctrlMode = (ctrlMode + 1) % 3;
                dUp = false;
                dDown = false;
                dLeft = false;
                dRight = false;
                showToast("Dieu khien: " + CTRL_NAMES[ctrlMode]);
                break;
            case K_SOFTR:
                leaveMap();
                break;
            case K_PREV:
                selectTool(-1);
                break;
            case K_NEXT:
                selectTool(1);
                break;
            case K_PACK:
                openPack();
                break;
            case K_ADDON:
                addonReturn = ST_PLAY;
                openMenu();
                break;
            case K_NET:
                openNetMenu();
                break;
            case K_RAGDOLL:
                toggleLimp();
                break;
            case K_RESPAWN:
                respawnPlayer();
                break;
            default:
                break;
        }
    }

    private void leaveMap() {
        netStop(null);
        hookState = 0;
        player.hookOn = false;
        clearKeys();
        mapList = buildMapList();
        mapSel = 0;
        mapTop = 0;
        state = ST_MAPS;
    }

    // ------------------------------------------------------------------ main menu / map list / create map

    private void clickMenu() {
        int bx = sw - 170;
        if (Ui.in(curX, curY, bx, 74, 150, 30)) {
            mapList = buildMapList();
            mapSel = 0;
            mapTop = 0;
            confirmDel = false;
            state = ST_MAPS;
        } else if (Ui.in(curX, curY, bx, 112, 150, 26)) {
            addonReturn = ST_MENU;
            openMenu();
        } else if (Ui.in(curX, curY, bx, 146, 150, 26)) {
            midlet.exitApp();
        }
    }

    static final int ROW_H = 32;
    static final int ROWS_VIS = 5;
    static final int LIST_Y = 38;

    private void mapsKey(int k) {
        if (k == K_OK) {
            clickMaps();
        } else if (k == K_SOFTR) {
            state = ST_MENU;
        } else if (k == K_MJUMP) {
            scrollMaps(-1);
        } else if (k == K_MDOWN) {
            scrollMaps(1);
        } else if (k == K_SOFTL) {
            if (mapSel >= extraMapCount() && mapSel < mapList.size()) {
                if (confirmDel) {
                    maps.remove(mapSel - extraMapCount());
                    mapList = buildMapList();
                    if (mapSel >= mapList.size()) {
                        mapSel = Math.max(0, mapList.size() - 1);
                    }
                    confirmDel = false;
                    showToast("Da xoa map");
                } else {
                    confirmDel = true;
                    showToast("Bam L lan nua de xoa");
                }
            } else {
                showToast("Map goc khong xoa duoc");
            }
        }
    }

    private int extraMapCount() {
        return extraMaps.size();
    }

    private void scrollMaps(int d) {
        if (mapList.size() == 0) {
            return;
        }
        confirmDel = false;
        mapSel = World.clampInt(mapSel + d, 0, mapList.size() - 1);
        if (mapSel < mapTop) {
            mapTop = mapSel;
        }
        if (mapSel >= mapTop + ROWS_VIS) {
            mapTop = mapSel - ROWS_VIS + 1;
        }
    }

    private void clickMaps() {
        if (Ui.in(curX, curY, 4, 4, 64, 22)) {
            state = ST_MENU;
            return;
        }
        if (Ui.in(curX, curY, sw - 186, 4, 88, 22)) {
            cName = "";
            cAddr = "";
            cDiff = 0;
            cMulti = false;
            state = ST_CREATE;
            return;
        }
        if (Ui.in(curX, curY, sw - 94, 4, 90, 22)) {
            netReturn = ST_MAPS;
            openNetMenu();
            return;
        }
        if (Ui.in(curX, curY, sw - 24, LIST_Y, 20, 20)) {
            scrollMaps(-1);
            return;
        }
        if (Ui.in(curX, curY, sw - 24, LIST_Y + ROWS_VIS * ROW_H - 20, 20, 20)) {
            scrollMaps(1);
            return;
        }
        for (int i = 0; i < ROWS_VIS; i++) {
            int idx = mapTop + i;
            if (idx < mapList.size() && Ui.in(curX, curY, 4, LIST_Y + i * ROW_H, sw - 32, ROW_H - 2)) {
                mapSel = idx;
                confirmDel = false;
                startMap((MapInfo) mapList.elementAt(idx));
                return;
            }
        }
    }

    private boolean cMulti;

    void textDone(int field, String value) {
        if (field == 0) {
            cName = value == null ? "" : value;
        } else if (field == 1) {
            cAddr = value == null ? "" : value;
        }
    }

    private void ask(int field, String title, String init, int max) {
        try {
            new TextAsk(this, midlet.display(), field, title, init, max);
        } catch (Throwable t) {
            err = "" + t;
            errT = 100;
        }
    }

    private String randomAddress() {
        return "" + (100000 + (int) (rand() * 899999f));
    }

    private void clickCreate() {
        if (Ui.in(curX, curY, 10, 196, 90, 28)) {
            state = ST_MAPS;
        } else if (Ui.in(curX, curY, 80, 44, 230, 22)) {
            ask(0, "Ten map", cName, 20);
        } else if (Ui.in(curX, curY, 80, 104, 230, 22)) {
            ask(1, "Dia chi map (de trong = ngau nhien)", cAddr, 20);
        } else if (Ui.in(curX, curY, 80, 74, 70, 22)) {
            cDiff = 0;
        } else if (Ui.in(curX, curY, 154, 74, 70, 22)) {
            cDiff = 1;
        } else if (Ui.in(curX, curY, 228, 74, 70, 22)) {
            cDiff = 2;
        } else if (Ui.in(curX, curY, 80, 134, 110, 22)) {
            cMulti = false;
        } else if (Ui.in(curX, curY, 194, 134, 116, 22)) {
            cMulti = true;
        } else if (Ui.in(curX, curY, sw - 130, 196, 120, 28)) {
            createMap();
        }
    }

    private void createMap() {
        String nm = cName.trim();
        if (nm.length() == 0) {
            nm = "Map " + (maps.list().size() + 1);
        }
        String ad = cAddr.trim();
        if (ad.length() == 0) {
            ad = randomAddress();
        }
        MapInfo mi = new MapInfo(nm, cDiff, ad, null);
        maps.add(mi);
        if (cMulti) {
            startMapHost(mi);
        } else {
            startMap(mi);
        }
    }

    /** Host a room with this map (Bluetooth) and load the map. */
    void startMapHost(MapInfo mi) {
        netStop(null);
        netBegin(true);
        startMapKeep(mi);
    }

    private void startMapKeep(MapInfo mi) {
        pendingMap = mi;
        genTimer = 0;
        world = null;
        clearKeys();
        state = ST_GEN;
    }

    // ------------------------------------------------------------------ backpack

    private void openPack() {
        clearKeys();
        carried = null;
        packTab = 0;
        state = ST_PACK;
    }

    private void stash(Tool t) {
        if (t == null) {
            return;
        }
        if (!inv.addStore(t) && !inv.add(t)) {
            dropItem(t);
        }
    }

    private void closePack() {
        if (carried != null) {
            stash(carried);
            carried = null;
        }
        clearKeys();
        state = ST_PLAY;
    }

    private int packHit() {
        int x = curX;
        int y = curY;
        if (Ui.in(x, y, sw - 62, 2, 58, 18)) {
            return 500;
        }
        for (int i = 0; i < 4; i++) {
            if (Ui.in(x, y, 4, 26 + i * 24, 62, 22)) {
                return 100 + i;
            }
        }
        if (Ui.in(x, y, 72, 26, Inventory.COLS * 27, Inventory.ROWS * 27)) {
            return 200 + ((y - 26) / 27) * Inventory.COLS + (x - 72) / 27;
        }
        if (Ui.in(x, y, 292, 26, 26, 26)) {
            return 300;
        }
        if (Ui.in(x, y, 292, 56, 26, 26)) {
            return 301;
        }
        if (Ui.in(x, y, 292, 86, 26, 26)) {
            return 302;
        }
        for (int i = 0; i < 10; i++) {
            if (Ui.in(x, y, 20 + i * 28, 150, 27, 26)) {
                return 400 + i;
            }
        }
        return -1;
    }

    private void packClick() {
        int h = packHit();
        if (h < 0) {
            return;
        }
        if (h == 500 || h == 409) {
            closePack();
        } else if (h >= 100 && h < 104) {
            packTab = h - 100;
        } else if (h >= 200 && h < 200 + Inventory.CELLS) {
            int idx = h - 200;
            Tool[] arr = inv.store[packTab];
            Tool cell = arr[idx];
            if (carried == null) {
                carried = cell;
                arr[idx] = null;
            } else if (carried.category == packTab) {
                arr[idx] = carried;
                carried = cell;
            } else if (inv.addStore(carried)) {
                showToast("Da cat vao tab " + Tool.CAT_NAMES[carried.category]);
                carried = null;
            } else {
                showToast("Tab day");
            }
        } else if (h == 300) {
            if (carried != null) {
                showToast("Da huy " + carried.name);
                carried = null;
            }
        } else if (h == 301) {
            if (carried != null) {
                dropItem(carried);
                showToast("Da vut " + carried.name);
                carried = null;
            }
        } else if (h == 302) {
            if (carried != null && carried.kind == Tool.HAT) {
                Tool old = inv.worn;
                inv.worn = carried;
                carried = old;
            } else if (carried == null && inv.worn != null) {
                carried = inv.worn;
                inv.worn = null;
            } else if (carried != null) {
                showToast("O nay chi nhan non");
            }
        } else if (h >= 400 && h < 409) {
            int i = h - 400;
            Tool old = inv.hot[i];
            inv.hot[i] = carried;
            carried = old;
        }
    }

    // ------------------------------------------------------------------ UI hit tests (in game)

    private int hotX() {
        return (sw - 280) / 2;
    }

    private boolean uiClick() {
        if (Ui.in(curX, curY, sw - 54, 2, 52, 16)) {
            addonReturn = ST_PLAY;
            openMenu();
            return true;
        }
        if (Ui.in(curX, curY, sw - 114, 2, 58, 16)) {
            openNetMenu();
            return true;
        }
        int hy = sh - 26;
        for (int i = 0; i < 10; i++) {
            if (Ui.in(curX, curY, hotX() + i * 28, hy, 27, 24)) {
                if (i == 9) {
                    openPack();
                } else {
                    inv.sel = i;
                    showToast(curTool().name);
                }
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ add-on menu (list + packs)

    private Pack viewPack;
    private Vector viewEntries = new Vector();

    private void openMenu() {
        clearKeys();
        state = ST_ADDON;
        menuSel = 0;
        menuTop = 0;
        menuMsg = "";
        viewPack = null;
        buildMenu();
    }

    private void buildMenu() {
        menu.removeAllElements();
        for (int i = 0; i < am.addons.size(); i++) {
            menu.addElement(am.addons.elementAt(i));
        }
        for (int i = 0; i < am.storeFiles.size(); i++) {
            menu.addElement(new Integer(i));
        }
        for (int i = 0; i < am.packs.size(); i++) {
            menu.addElement(am.packs.elementAt(i));
        }
        if (menuSel >= menu.size()) {
            menuSel = menu.size() - 1;
        }
        if (menuSel < 0) {
            menuSel = 0;
        }
    }

    private void closeMenu() {
        rebuild();
        clearKeys();
        state = addonReturn;
        if (state == ST_MAPS) {
            mapList = buildMapList();
        }
    }

    private void menuKey(int k) {
        if (viewPack != null) {
            if (k == K_SOFTR) {
                viewPack = null;
            } else if (k == K_UP || k == K_MJUMP) {
                menuSel = Math.max(0, menuSel - 1);
            } else if (k == K_DOWN || k == K_MDOWN) {
                menuSel = Math.min(Math.max(0, viewEntries.size() - 1), menuSel + 1);
            } else if (k == K_OK || k == K_USE) {
                int n = am.activatePack(viewPack);
                menuMsg = n > 0 ? "Da kich hoat " + n + " add-on" : "Khong cai duoc (the nho day / khong co quyen)";
                am.loadAll();
                viewPack = null;
                menuSel = 0;
                buildMenu();
            }
            return;
        }
        if (k == K_UP || k == K_MJUMP) {
            if (menuSel > 0) {
                menuSel--;
            }
        } else if (k == K_DOWN || k == K_MDOWN) {
            if (menuSel < menu.size() - 1) {
                menuSel++;
            }
        } else if (k == K_SOFTR || k == K_ADDON) {
            closeMenu();
        } else if (k == K_SOFTL) {
            am.loadAll();
            buildMenu();
            menuMsg = am.info;
        } else if (k == K_OK || k == K_USE) {
            if (menu.size() == 0) {
                return;
            }
            Object o = menu.elementAt(menuSel);
            if (o instanceof Addon) {
                Addon a = (Addon) o;
                if (a.builtin) {
                    menuMsg = "Goc luon bat";
                } else {
                    am.setEnabled(a, !a.enabled);
                    menuMsg = a.name + (a.enabled ? ": BAT" : ": TAT");
                }
            } else if (o instanceof Pack) {
                viewPack = (Pack) o;
                viewEntries = am.packEntries(viewPack);
                menuSel = 0;
                menuMsg = "";
            } else {
                int idx = ((Integer) o).intValue();
                if (am.install(idx)) {
                    am.loadAll();
                    buildMenu();
                    menuMsg = "Da cai xong";
                } else {
                    menuMsg = "Cai that bai";
                }
            }
        }
    }

    // ------------------------------------------------------------------ multiplayer (Bluetooth)

    private int netReturn = ST_PLAY;
    private boolean roomPending;
    private int roomId, roomPlayers, roomDiff, roomSig;
    private String roomName = "", roomAddr = "";

    private int remoteCount() {
        int c = 0;
        for (int i = 0; i < rf.length; i++) {
            if (rf[i] != null) {
                c++;
            }
        }
        return c;
    }

    private Tool toolAt(int idx) {
        if (tools.size() == 0) {
            return Tool.FIST;
        }
        if (idx < 0 || idx >= tools.size()) {
            idx = 0;
        }
        return (Tool) tools.elementAt(idx);
    }

    private void netUpdate() {
        if (!net.active()) {
            return;
        }
        Object[] m;
        while ((m = net.next()) != null) {
            netHandle((Peer) m[0], (byte[]) m[1]);
        }
        if (!net.active() || world == null || state != ST_PLAY) {
            return;
        }
        netTick++;
        if ((netTick & 1) == 0 && net.peerCount() > 0) {
            netSendState();
        }
        for (int i = 0; i < rf.length; i++) {
            Fighter f = rf[i];
            if (f == null) {
                continue;
            }
            if (f.attackT > 0) {
                f.attackT--;
            }
            if (f.kickT > 0) {
                f.kickT--;
            }
            if (f.flash > 0) {
                f.flash--;
            }
            if (f.dead) {
                if (f.rag != null) {
                    f.rag.step(world);
                }
            } else {
                float dx = f.tx - f.x;
                float dy = f.ty - f.y;
                if (dx > 60f || dx < -60f || dy > 60f || dy < -60f) {
                    f.x = f.tx;
                    f.y = f.ty;
                } else {
                    f.x += dx * 0.5f;
                    f.y += dy * 0.5f;
                }
                if (f.onGround) {
                    float sp = f.vx < 0f ? -f.vx : f.vx;
                    f.phase += sp * 0.13f;
                }
            }
        }
    }

    private void netSendState() {
        int flags = 0;
        if (player.onGround) {
            flags |= 1;
        }
        if (player.dead) {
            flags |= 2;
        }
        net.send(Net.state(net.myId, Net.rnd(player.x), Net.rnd(player.y), player.vx, player.face,
                player.hp, flags, Math.max(0, netToolIndex(curTool()))));
    }

    private void netHandle(Peer p, byte[] d) {
        if (d == null) {
            netPeerGone(p);
            return;
        }
        if (!net.hasPeer(p) || d.length < 2) {
            return;
        }
        int type = d[0];
        boolean host = net.mode == Net.HOST;
        if (type == Net.T_HELLO) {
            if (host && d.length >= 7) {
                String nm = curMap != null ? curMap.name : "?";
                String ad = curMap != null ? curMap.address : "";
                int df = curMap != null ? curMap.diff : 0;
                p.send(Net.room(p.id, 1 + net.peerCount(), df, net.localSig, nm, ad));
                showToast("P" + (p.id + 1) + " vao phong");
            }
        } else if (type == Net.T_ROOM) {
            if (!host && d.length >= 11) {
                roomId = d[2];
                roomPlayers = d[3];
                roomDiff = d[4];
                roomSig = Net.r32(d, 5);
                int nl = d[9] & 255;
                if (10 + nl >= d.length) {
                    return;
                }
                int al = d[10 + nl] & 255;
                if (11 + nl + al > d.length) {
                    return;
                }
                roomName = Net.str(d, 10, nl);
                roomAddr = Net.str(d, 11 + nl, al);
                roomPending = true;
                state = ST_NET;
            }
        } else if (type == Net.T_STATE || type == Net.T_SHOT || type == Net.T_MELEE) {
            int id = d[1];
            if (host) {
                id = p.id;
                d[1] = (byte) id;
                net.relay(d, p);
            }
            if (id < 0 || id >= rf.length || id == net.myId) {
                return;
            }
            if (type == Net.T_STATE) {
                if (d.length >= 11) {
                    applyState(id, d);
                }
            } else if (d.length >= 9) {
                applyFire(id, type, d);
            }
        } else if (type == Net.T_LEAVE) {
            if (!host) {
                int id = d[1];
                if (id >= 0 && id < rf.length) {
                    rf[id] = null;
                }
            }
        }
    }

    private void applyState(int id, byte[] d) {
        int x = Net.r16(d, 2);
        int y = Net.r16(d, 4);
        int flags = d[9];
        boolean dead = (flags & 2) != 0;
        Fighter f = rf[id];
        if (f == null) {
            f = new Fighter();
            f.color = PALETTE[id];
            f.team = 2 + id;
            f.place(x, y);
            rf[id] = f;
        }
        if (!dead && f.dead) {
            f.place(x, y);
        }
        f.tx = x;
        f.ty = y;
        f.vx = d[6] / 20f;
        f.face = d[7] >= 0 ? 1 : -1;
        f.onGround = (flags & 1) != 0;
        f.tool = toolAt(d[10]);
        if (dead && !f.dead) {
            f.x = x;
            f.y = y;
            f.vy = 0f;
            f.hp = 0;
            f.die();
        } else if (!dead) {
            f.hp = d[8];
        }
    }

    private void applyFire(int id, int type, byte[] d) {
        Fighter f = rf[id];
        if (f == null) {
            return;
        }
        Tool t = toolAt(d[2]);
        float sx = Net.r16(d, 3);
        float sy = Net.r16(d, 5);
        float ax = d[7] / 100f;
        float ay = d[8] / 100f;
        f.ax = ax;
        f.ay = ay;
        if (ax > 0.15f) {
            f.face = 1;
        } else if (ax < -0.15f) {
            f.face = -1;
        }
        f.tool = t;
        f.attackT = f.attackMax;
        if (t.kind == Tool.KICK) {
            f.kickT = 10;
            f.kdx = ax;
            f.kdy = ay;
        }
        if ((type == Net.T_MELEE) == (t.kind == Tool.MELEE) && t.isAttack()) {
            doFire(f, t, sx, sy, ax, ay);
        }
    }

    private void netPeerGone(Peer p) {
        if (!net.hasPeer(p)) {
            return;
        }
        if (net.mode == Net.HOST) {
            int id = p.id;
            net.removePeer(p);
            if (id >= 0 && id < rf.length) {
                rf[id] = null;
            }
            net.send(Net.leave(id));
            showToast("P" + (id + 1) + " roi phong");
        } else {
            netStop("Mat ket noi voi host");
            if (state == ST_PLAY || state == ST_GEN || state == ST_NET) {
                state = ST_MAPS;
            }
        }
    }

    private void netBegin(boolean host) {
        net.localSig = am.signature();
        net.mode = host ? Net.HOST : Net.CLIENT;
        net.myId = 0;
        bt = new BtLink(net);
        if (host) {
            bt.startHost();
        } else {
            bt.startSearch();
        }
    }

    private void netStop(String why) {
        if (bt != null) {
            bt.stop();
            bt = null;
        }
        net.stop();
        roomPending = false;
        for (int i = 0; i < rf.length; i++) {
            rf[i] = null;
        }
        if (why != null) {
            showToast(why);
        }
    }

    private void openNetMenu() {
        netReturn = (state == ST_MAPS) ? ST_MAPS : ST_PLAY;
        state = ST_NET;
        netSel = 0;
        clearKeys();
    }

    private boolean netListMode() {
        return bt != null && bt.state == BtLink.S_LIST && !roomPending;
    }

    private MapInfo resolveRoomMap() {
        if (roomAddr.equals("(add-on)")) {
            for (int i = 0; i < extraMaps.size(); i++) {
                Section s = (Section) extraMaps.elementAt(i);
                if (s.get("name", "Map").equals(roomName)) {
                    return new MapInfo(roomName, 1, roomAddr, s);
                }
            }
            return null;
        }
        return new MapInfo(roomName, roomDiff, roomAddr, null);
    }

    private void acceptRoom() {
        roomPending = false;
        MapInfo mi = resolveRoomMap();
        if (mi == null) {
            netStop("May ban khong co map add-on nay");
            return;
        }
        net.myId = roomId;
        if (roomSig != net.localSig) {
            showToast("Khac add-on!");
        }
        startMapKeep(mi);
    }

    private void netMenuKey(int k) {
        if (roomPending) {
            if (k == K_OK || k == K_USE) {
                acceptRoom();
            } else if (k == K_SOFTR) {
                netStop("Da huy");
            }
            return;
        }
        int n = netListMode() ? bt.names.size() + 1 : 3;
        if (k == K_UP || k == K_MJUMP) {
            if (netSel > 0) {
                netSel--;
            }
        } else if (k == K_DOWN || k == K_MDOWN) {
            if (netSel < n - 1) {
                netSel++;
            }
        } else if (k == K_SOFTR || k == K_NET) {
            state = netReturn;
        } else if (k == K_OK || k == K_USE) {
            if (netListMode()) {
                if (netSel < bt.names.size()) {
                    bt.connectTo(netSel);
                } else {
                    bt.backToSearch();
                    netSel = 0;
                }
            } else if (netSel == 0) {
                if (netReturn == ST_MAPS) {
                    if (mapList.size() > 0) {
                        startMapHost((MapInfo) mapList.elementAt(mapSel));
                    } else {
                        showToast("Hay tao map truoc");
                    }
                } else {
                    netStop(null);
                    netBegin(true);
                    state = ST_PLAY;
                    showToast("Dang cho nguoi vao...");
                }
            } else if (netSel == 1) {
                netStop(null);
                netBegin(false);
            } else {
                netStop("Da ngat ket noi");
            }
        }
    }

    // ------------------------------------------------------------------ drawing

    protected void paint(Graphics g0) {
        Graphics g = g0;
        if (useBuf && buf != null) {
            g = buf.getGraphics();
        }
        try {
            render(g);
        } catch (Throwable t) {
            err = "" + t;
            errT = 150;
        }
        if (err.length() > 0) {
            g.setFont(fs);
            g.setColor(0xFFFFFF);
            g.fillRect(0, sh - 14, sw, 14);
            g.setColor(0xFF0000);
            g.drawString(err, 2, sh - 13, Graphics.TOP | Graphics.LEFT);
        }
        if (useBuf && buf != null) {
            g0.drawImage(buf, 0, 0, Graphics.TOP | Graphics.LEFT);
        }
    }

    private void render(Graphics g) {
        g.setFont(fs);
        switch (state) {
            case ST_LOAD:
                paintLoad(g);
                break;
            case ST_MENU:
                paintMainMenu(g);
                break;
            case ST_MAPS:
                paintMaps(g);
                break;
            case ST_CREATE:
                paintCreate(g);
                break;
            case ST_GEN:
                paintGen(g);
                break;
            case ST_ADDON:
                paintAddon(g);
                break;
            case ST_NET:
                paintNet(g);
                break;
            case ST_PACK:
                paintPack(g);
                break;
            default:
                if (world != null) {
                    paintPlay(g);
                }
                break;
        }
        if (toastT > 0 && state != ST_LOAD) {
            g.setColor(0x000000);
            g.drawString(toast, sw / 2 + 1, sh - 52, Graphics.TOP | Graphics.HCENTER);
            g.setColor(0xFFFFFF);
            g.drawString(toast, sw / 2, sh - 53, Graphics.TOP | Graphics.HCENTER);
        }
        if (state == ST_MENU || state == ST_MAPS || state == ST_CREATE || state == ST_PACK
                || (state == ST_PLAY && ctrlMode == 0)) {
            paintCursor(g);
        }
        if (confirmExit) {
            g.setColor(0x000000);
            g.fillRect(sw / 2 - 90, sh / 2 - 26, 180, 52);
            g.setColor(0xFFFFFF);
            g.drawRect(sw / 2 - 90, sh / 2 - 26, 179, 51);
            g.drawString("Thoat game?", sw / 2, sh / 2 - 20, Graphics.TOP | Graphics.HCENTER);
            g.drawString("OK = Co    R = Khong", sw / 2, sh / 2 + 2, Graphics.TOP | Graphics.HCENTER);
        }
    }

    private void paintLoad(Graphics g) {
        g.setColor(BG_SPLASH);
        g.fillRect(0, 0, sw, sh);
        if (splash != null) {
            g.drawImage(splash, (sw - splash.getWidth()) / 2, (sh - splash.getHeight()) / 2,
                    Graphics.TOP | Graphics.LEFT);
        }
        int lt = loadTimer > LOAD_FRAMES ? LOAD_FRAMES : loadTimer;
        g.setColor(0xDDDDDD);
        g.fillRect(20, sh - 10, sw - 40, 5);
        g.setColor(0x3AA0FF);
        g.fillRect(20, sh - 10, (sw - 40) * lt / LOAD_FRAMES, 5);
        g.setColor(0x707070);
        g.drawString("Dang tai...", sw - 4, 4, Graphics.TOP | Graphics.RIGHT);
    }

    private void paintGen(Graphics g) {
        Ui.gradient(g, sw, sh, 0x1C2A55, 0x4A7FD0);
        g.setColor(0xFFFFFF);
        String nm = pendingMap != null ? pendingMap.name : "";
        g.drawString("Dang tao map", sw / 2, sh / 2 - 34, Graphics.TOP | Graphics.HCENTER);
        g.setColor(0xFFD84A);
        g.drawString(nm, sw / 2, sh / 2 - 18, Graphics.TOP | Graphics.HCENTER);
        if (pendingMap != null && pendingMap.sec == null) {
            g.setColor(0xB0C8F0);
            g.drawString("Dia chi: " + pendingMap.address, sw / 2, sh / 2 - 2, Graphics.TOP | Graphics.HCENTER);
        }
        int lt = genTimer > 36 ? 36 : genTimer;
        g.setColor(0x102040);
        g.fillRect(40, sh / 2 + 24, sw - 80, 8);
        g.setColor(0x5CE070);
        g.fillRect(40, sh / 2 + 24, (sw - 80) * lt / 36, 8);
        if (net.active()) {
            g.setColor(0xB0E0C0);
            g.drawString("Bluetooth: dang cho vao phong...", sw / 2, sh / 2 + 40, Graphics.TOP | Graphics.HCENTER);
        }
    }

    private void skyBackdrop(Graphics g) {
        Ui.gradient(g, sw, sh, 0x2A6FD0, 0xC8EBFF);
        g.setColor(0xFFFFFF);
        for (int i = 0; i < 4; i++) {
            int cx = ((i * 110 + tickCount / 2) % (sw + 80)) - 60;
            int cy = 30 + (i * 41) % 70;
            g.fillRect(cx, cy, 46, 9);
            g.fillRect(cx + 9, cy - 6, 26, 9);
        }
        g.setColor(0x4C9A4C);
        g.fillArc(-60, sh - 70, 260, 140, 0, 360);
        g.setColor(0x3E8A44);
        g.fillArc(130, sh - 56, 280, 120, 0, 360);
        g.setColor(0x357A3A);
        g.fillRect(0, sh - 22, sw, 22);
    }

    private void paintMainMenu(Graphics g) {
        skyBackdrop(g);
        if (logo != null) {
            g.drawImage(logo, 12, 10, Graphics.TOP | Graphics.LEFT);
        }
        Font fb = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_BOLD, Font.SIZE_MEDIUM);
        g.setFont(fb);
        g.setColor(0x102040);
        g.drawString("MSV mai co dac xe", 82, 24, Graphics.TOP | Graphics.LEFT);
        g.setColor(0xFFFFFF);
        g.drawString("MSV mai co dac xe", 81, 23, Graphics.TOP | Graphics.LEFT);
        g.setFont(fs);
        int bx = sw - 170;
        Ui.button(g, fb, bx, 74, 150, 30, "Play", Ui.in(curX, curY, bx, 74, 150, 30), 0x2E9E3E);
        Ui.button(g, fs, bx, 112, 150, 26, "Addon", Ui.in(curX, curY, bx, 112, 150, 26), 0x3050A0);
        Ui.button(g, fs, bx, 146, 150, 26, "Thoat", Ui.in(curX, curY, bx, 146, 150, 26), 0xB03030);
        g.setColor(0x102040);
        g.drawString("D-pad = chuot, OK = bam, R = thoat", 4, sh - 14, Graphics.TOP | Graphics.LEFT);
        menuFight.x = 60f;
        menuFight.y = sh - 22;
        menuFight.draw(g, 0, 0);
    }

    private void paintMaps(Graphics g) {
        Ui.gradient(g, sw, sh, 0x1C3A70, 0x6AA0E0);
        Ui.button(g, fs, 4, 4, 64, 22, "Thoat", Ui.in(curX, curY, 4, 4, 64, 22), 0xB03030);
        g.setColor(0xFFFFFF);
        g.drawString("Chon map", 74, 9, Graphics.TOP | Graphics.LEFT);
        Ui.button(g, fs, sw - 186, 4, 88, 22, "Tao map", Ui.in(curX, curY, sw - 186, 4, 88, 22), 0x2E9E3E);
        Ui.button(g, fs, sw - 94, 4, 90, 22, "Nhieu nguoi", Ui.in(curX, curY, sw - 94, 4, 90, 22), 0x7A3AA0);
        if (mapList.size() == 0) {
            g.setColor(0xFFFFFF);
            g.drawString("Chua co map. Bam 'Tao map'!", sw / 2, 90, Graphics.TOP | Graphics.HCENTER);
        }
        for (int i = 0; i < ROWS_VIS; i++) {
            int idx = mapTop + i;
            if (idx >= mapList.size()) {
                break;
            }
            MapInfo m = (MapInfo) mapList.elementAt(idx);
            int y = LIST_Y + i * ROW_H;
            boolean hover = Ui.in(curX, curY, 4, y, sw - 32, ROW_H - 2);
            g.setColor(0x000000);
            g.fillRect(6, y + 2, sw - 32, ROW_H - 2);
            g.setColor(hover ? 0x4A78C8 : (idx == mapSel ? 0x3A5A9A : 0x2A4478));
            g.fillRect(4, y, sw - 32, ROW_H - 2);
            g.setColor(hover ? 0xFFFFFF : 0x8AA8E0);
            g.drawRect(4, y, sw - 33, ROW_H - 3);
            g.setColor(0xFFFFFF);
            String nm = m.name.length() > 22 ? m.name.substring(0, 22) : m.name;
            g.drawString(nm, 10, y + 3, Graphics.TOP | Graphics.LEFT);
            g.setColor(0xB8D0FF);
            String sub = m.sec != null ? "Map add-on" : "Do kho: " + MapInfo.DIFF_NAMES[m.diff] + "  Dia chi: " + m.address;
            if (sub.length() > 40) {
                sub = sub.substring(0, 40);
            }
            g.drawString(sub, 10, y + 16, Graphics.TOP | Graphics.LEFT);
            if (confirmDel && idx == mapSel) {
                g.setColor(0xFF8080);
                g.drawString("L = xoa", sw - 40, y + 3, Graphics.TOP | Graphics.RIGHT);
            }
        }
        Ui.button(g, fs, sw - 24, LIST_Y, 20, 20, "^", Ui.in(curX, curY, sw - 24, LIST_Y, 20, 20), 0x3050A0);
        Ui.button(g, fs, sw - 24, LIST_Y + ROWS_VIS * ROW_H - 20, 20, 20, "v",
                Ui.in(curX, curY, sw - 24, LIST_Y + ROWS_VIS * ROW_H - 20, 20, 20), 0x3050A0);
        g.setColor(0xFFFFFF);
        g.drawString("Bam vao map de choi   L: xoa map   2/8: cuon   R: quay lai", 4, sh - 14,
                Graphics.TOP | Graphics.LEFT);
    }

    private void paintCreate(Graphics g) {
        Ui.gradient(g, sw, sh, 0x1C3A70, 0x6AA0E0);
        g.setColor(0xFFFFFF);
        g.drawString("TAO MAP", 10, 12, Graphics.TOP | Graphics.LEFT);
        g.drawString("Ten map", 10, 49, Graphics.TOP | Graphics.LEFT);
        field(g, 80, 44, 230, 22, cName.length() > 0 ? cName : "(bam de dat ten)", cName.length() > 0);
        g.setColor(0xFFFFFF);
        g.drawString("Do kho", 10, 79, Graphics.TOP | Graphics.LEFT);
        for (int i = 0; i < 3; i++) {
            int x = 80 + i * 74;
            Ui.button(g, fs, x, 74, 70, 22, MapInfo.DIFF_NAMES[i], Ui.in(curX, curY, x, 74, 70, 22) || cDiff == i,
                    cDiff == i ? 0x2E9E3E : 0x3050A0);
        }
        g.setColor(0xFFFFFF);
        g.drawString("Dia chi", 10, 109, Graphics.TOP | Graphics.LEFT);
        field(g, 80, 104, 230, 22, cAddr.length() > 0 ? cAddr : "(ngau nhien)", cAddr.length() > 0);
        g.setColor(0xFFFFFF);
        g.drawString("Che do", 10, 139, Graphics.TOP | Graphics.LEFT);
        Ui.button(g, fs, 80, 134, 110, 22, "1 nguoi", Ui.in(curX, curY, 80, 134, 110, 22) || !cMulti,
                !cMulti ? 0x2E9E3E : 0x3050A0);
        Ui.button(g, fs, 194, 134, 116, 22, "Nhieu nguoi", Ui.in(curX, curY, 194, 134, 116, 22) || cMulti,
                cMulti ? 0x2E9E3E : 0x3050A0);
        g.setColor(0xD8E8FF);
        g.drawString("Cung dia chi + do kho = cung mot map (giong seed Minecraft)", 10, 166,
                Graphics.TOP | Graphics.LEFT);
        g.drawString("Nhieu nguoi: nguoi khac vao bang Bluetooth", 10, 180, Graphics.TOP | Graphics.LEFT);
        Ui.button(g, fs, 10, 196, 90, 28, "Thoat", Ui.in(curX, curY, 10, 196, 90, 28), 0xB03030);
        Ui.button(g, fs, sw - 130, 196, 120, 28, "Tao map", Ui.in(curX, curY, sw - 130, 196, 120, 28), 0x2E9E3E);
    }

    private void field(Graphics g, int x, int y, int w, int h, String text, boolean filled) {
        boolean hover = Ui.in(curX, curY, x, y, w, h);
        g.setColor(hover ? 0xFFFFE0 : 0xFFFFFF);
        g.fillRect(x, y, w, h);
        g.setColor(hover ? 0xFFB000 : 0x405080);
        g.drawRect(x, y, w - 1, h - 1);
        g.setColor(filled ? 0x000000 : 0x808080);
        String t = text.length() > 34 ? text.substring(0, 34) : text;
        g.drawString(t, x + 4, y + 4, Graphics.TOP | Graphics.LEFT);
    }

    private static int shade(int c) {
        return (c >> 1) & 0x7F7F7F;
    }

    private void drawItemBox(Graphics g, int x, int y, int w, int h, Tool t, boolean selected) {
        g.setColor(selected ? 0xFFE060 : 0xE0E0E0);
        g.fillRect(x, y, w, h);
        g.setColor(selected ? 0xC08000 : 0x707070);
        g.drawRect(x, y, w - 1, h - 1);
        if (t != null) {
            g.setColor(t.color);
            g.fillRect(x + 3, y + 3, w - 6, 6);
            g.setColor(0x000000);
            String n = t.name.length() > 4 ? t.name.substring(0, 4) : t.name;
            g.drawString(n, x + w / 2, y + h - 13, Graphics.TOP | Graphics.HCENTER);
        }
    }

    private void paintPlay(Graphics g) {
        g.setColor(world.sky);
        g.fillRect(0, 0, sw, sh);
        g.setColor(0xFFFFFF);
        for (int i = 0; i < 8; i++) {
            int cx = i * 170 + 30 - camX / 3;
            cx = ((cx % 1360) + 1360) % 1360 - 100;
            int cy = 20 + (i * 37) % 60 - camY / 4;
            g.fillRect(cx, cy, 44, 8);
            g.fillRect(cx + 8, cy - 6, 26, 8);
        }
        for (int i = 0; i < world.n; i++) {
            int x = world.rx[i] - camX;
            int y = world.ry[i] - camY;
            int w = world.rw[i];
            int h = world.rh[i];
            if (x + w < 0 || x > sw || y + h < 0 || y > sh) {
                continue;
            }
            g.setColor(world.rc[i]);
            g.fillRect(x, y, w, h);
            g.setColor(shade(world.rc[i]));
            g.drawRect(x, y, w - 1, h - 1);
        }
        for (int i = 0; i < world.deco.size(); i++) {
            int[] d = (int[]) world.deco.elementAt(i);
            int x = d[1] - camX;
            int y = d[2] - camY;
            if (x < -20 || x > sw + 20) {
                continue;
            }
            if (d[0] == 0) {
                g.setColor(0x7A4A20);
                g.fillRect(x - 3, y - 26, 6, 26);
                g.setColor(0x2E8A3A);
                g.fillArc(x - 15, y - 50, 30, 30, 0, 360);
            } else if (d[0] == 1) {
                g.setColor(0x3A9A3A);
                g.fillArc(x - 10, y - 12, 20, 14, 0, 360);
            } else {
                g.setColor(0x909090);
                g.fillArc(x - 8, y - 9, 16, 11, 0, 360);
            }
        }
        for (int i = 0; i < world.nf; i++) {
            int x = world.fx[i] - camX;
            int y = world.fy[i] - camY;
            if (x < -20 || x > sw + 20) {
                continue;
            }
            g.setColor(0x606060);
            g.fillRect(x, y - 34, 2, 34);
            g.setColor(flagHit[i] ? 0x20C040 : 0xE03030);
            g.fillTriangle(x + 2, y - 34, x + 16, y - 29, x + 2, y - 24);
        }
        for (int i = 0; i < drops.size(); i++) {
            Pickup p = (Pickup) drops.elementAt(i);
            int x = Fighter.r(p.x) - camX;
            int y = Fighter.r(p.y) - camY;
            int bob = p.ground ? ((tickCount / 6 + i) & 1) : 0;
            g.setColor(0x000000);
            g.fillRect(x - 5, y - 9 - bob, 11, 11);
            g.setColor(p.t.color);
            g.fillRect(x - 4, y - 8 - bob, 9, 9);
            g.setColor(0xFFFFFF);
            g.drawString(p.t.name.substring(0, 1), x, y - 16 - bob, Graphics.TOP | Graphics.HCENTER);
        }
        if (!net.active()) {
            for (int i = 0; i < npcs.size(); i++) {
                Npc n = (Npc) npcs.elementAt(i);
                n.f.draw(g, camX, camY);
                if (!n.f.dead) {
                    int lx = Fighter.r(n.f.x) - camX;
                    int ly = Fighter.r(n.f.y) - camY - Fighter.H;
                    g.setColor(0x000000);
                    g.fillRect(lx - 12, ly - 5, 24, 4);
                    g.setColor(0xFF3030);
                    g.fillRect(lx - 11, ly - 4, 22 * n.f.hp / n.f.maxHp, 2);
                    g.setColor(0x202020);
                    g.drawString(n.def.name, lx, ly - 17, Graphics.TOP | Graphics.HCENTER);
                }
            }
        }
        for (int i = 0; i < rf.length; i++) {
            Fighter r = rf[i];
            if (r == null) {
                continue;
            }
            r.draw(g, camX, camY);
            int lx = Fighter.r(r.x) - camX;
            int ly = Fighter.r(r.y) - camY - Fighter.H;
            g.setColor(0x000000);
            g.drawString("P" + (i + 1), lx, ly - 15, Graphics.TOP | Graphics.HCENTER);
            if (!r.dead) {
                g.fillRect(lx - 12, ly - 4, 24, 4);
                g.setColor(0xFF3030);
                g.fillRect(lx - 11, ly - 3, 22 * r.hp / r.maxHp, 2);
            }
        }
        player.draw(g, camX, camY);
        for (int i = 0; i < shots.size(); i++) {
            Shot s = (Shot) shots.elementAt(i);
            int x = Fighter.r(s.x) - camX;
            int y = Fighter.r(s.y) - camY;
            if (s.bomb) {
                g.setColor((s.fuse < 15 && (s.fuse & 2) == 0) ? 0xFF2020 : s.color);
                g.fillArc(x - 3, y - 3, 7, 7, 0, 360);
            } else if (s.slipper) {
                g.setColor(s.color);
                g.fillRect(x - 3, y - 1, 7, 3);
                g.setColor(0xFFFFFF);
                g.fillRect(x - 3, y - 2, 3, 1);
            } else if (s.width > 0) {
                int tx = x - Fighter.r(s.vx * 3f);
                int ty = y - Fighter.r(s.vy * 3f);
                int hw = s.width / 2;
                g.setColor(s.color);
                for (int o = -hw; o <= hw; o += 2) {
                    g.drawLine(tx, ty + o, x, y + o);
                }
                g.fillArc(x - hw - 1, y - hw - 1, s.width + 3, s.width + 3, 0, 360);
                g.setColor(0xFFFFFF);
                g.drawLine(tx, ty, x, y);
                g.drawLine(tx, ty + 1, x, y + 1);
                g.fillArc(x - hw / 2, y - hw / 2, hw + 1, hw + 1, 0, 360);
            } else {
                g.setColor(s.color);
                g.drawLine(x - Fighter.r(s.vx * 0.7f), y - Fighter.r(s.vy * 0.7f), x, y);
                g.drawLine(x - Fighter.r(s.vx * 0.7f), y - Fighter.r(s.vy * 0.7f) + 1, x, y + 1);
            }
        }
        for (int i = 0; i < booms.size(); i++) {
            int[] b = (int[]) booms.elementAt(i);
            int rad = b[2] * b[3] / 10;
            g.setColor(b[3] < 5 ? 0xFFE060 : 0xFF8030);
            g.fillArc(b[0] - camX - rad, b[1] - camY - rad, rad * 2, rad * 2, 0, 360);
        }
        for (int i = 0; i < popups.size(); i++) {
            Object[] o = (Object[]) popups.elementAt(i);
            int[] p = (int[]) o[0];
            int x = p[0] - camX;
            int y = p[1] - camY - p[2];
            g.setColor(0x000000);
            g.drawString((String) o[1], x + 1, y + 1, Graphics.TOP | Graphics.HCENTER);
            g.setColor(p[3]);
            g.drawString((String) o[1], x, y, Graphics.TOP | Graphics.HCENTER);
        }
        if (ctrlMode != 0 && !player.dead) {
            computeAim();
            int ax = Fighter.r(player.x + player.face * 3 + aimX * 30f) - camX;
            int ay = Fighter.r(player.y - 26 + aimY * 30f) - camY;
            g.setColor(0xFF2020);
            g.drawLine(ax - 4, ay, ax + 4, ay);
            g.drawLine(ax, ay - 4, ax, ay + 4);
        }
        paintHud(g);
    }

    private void paintHud(Graphics g) {
        g.setColor(0x000000);
        g.fillRect(4, 4, 62, 8);
        g.setColor(0xFF3030);
        g.fillRect(5, 5, 60 * player.hp / player.maxHp, 6);
        g.setColor(0x202020);
        g.drawString("" + player.hp, 70, 3, Graphics.TOP | Graphics.LEFT);
        g.drawString("Ha: " + kills, 4, 29, Graphics.TOP | Graphics.LEFT);
        g.drawString(CTRL_NAMES[ctrlMode] + " (L)", 4, 16, Graphics.TOP | Graphics.LEFT);
        if (net.active()) {
            g.setColor(0x106030);
            g.drawString("BT " + (net.mode == Net.HOST ? "host" : "khach") + " P" + (net.myId + 1)
                    + " (" + (1 + remoteCount()) + "/4)", 4, 42, Graphics.TOP | Graphics.LEFT);
        }
        Ui.button(g, fs, sw - 54, 2, 52, 16, "Addon", Ui.in(curX, curY, sw - 54, 2, 52, 16) && ctrlMode == 0, 0x3050A0);
        Ui.button(g, fs, sw - 114, 2, 58, 16, "Bluetooth", Ui.in(curX, curY, sw - 114, 2, 58, 16) && ctrlMode == 0,
                net.active() ? 0x208040 : 0x3050A0);
        int hy = sh - 26;
        for (int i = 0; i < 9; i++) {
            drawItemBox(g, hotX() + i * 28, hy, 27, 24, inv.hot[i], i == inv.sel);
            g.setColor(0x606060);
            g.drawString("" + (i + 1), hotX() + i * 28 + 2, hy + 1, Graphics.TOP | Graphics.LEFT);
        }
        int bx = hotX() + 9 * 28;
        g.setColor(Ui.in(curX, curY, bx, hy, 27, 24) && ctrlMode == 0 ? 0xFFE0A0 : 0xC09050);
        g.fillRect(bx, hy, 27, 24);
        g.setColor(0x503010);
        g.drawRect(bx, hy, 26, 23);
        g.fillRect(bx + 7, hy + 7, 13, 12);
        g.drawRect(bx + 9, hy + 3, 9, 6);
        g.setColor(0xFFFFFF);
        g.drawString("10", bx + 13, hy + 8, Graphics.TOP | Graphics.HCENTER);
    }

    private void paintCursor(Graphics g) {
        g.setColor(0x000000);
        g.fillTriangle(curX, curY, curX + 11, curY + 5, curX + 5, curY + 11);
        g.setColor(0xFFFFFF);
        g.fillTriangle(curX + 1, curY + 2, curX + 7, curY + 5, curX + 4, curY + 8);
    }

    private void paintPack(Graphics g) {
        Ui.gradient(g, sw, sh, 0x2A2030, 0x5A4A70);
        g.setColor(0xFFD84A);
        g.drawString("Backpack (beta)", 6, 7, Graphics.TOP | Graphics.LEFT);
        Ui.button(g, fs, sw - 62, 2, 58, 18, "Thoat", Ui.in(curX, curY, sw - 62, 2, 58, 18), 0xB03030);
        for (int i = 0; i < 4; i++) {
            boolean on = i == packTab;
            Ui.button(g, fs, 4, 26 + i * 24, 62, 22, Tool.CAT_NAMES[i], on || Ui.in(curX, curY, 4, 26 + i * 24, 62, 22),
                    on ? 0x2E9E3E : 0x3050A0);
        }
        Tool[] arr = inv.store[packTab];
        for (int i = 0; i < Inventory.CELLS; i++) {
            int x = 72 + (i % Inventory.COLS) * 27;
            int y = 26 + (i / Inventory.COLS) * 27;
            drawItemBox(g, x, y, 26, 26, arr[i], Ui.in(curX, curY, x, y, 27, 27));
        }
        // trash / drop / worn hat
        g.setColor(Ui.in(curX, curY, 292, 26, 26, 26) ? 0xFF8080 : 0xB04040);
        g.fillRect(292, 26, 26, 26);
        g.setColor(0xFFFFFF);
        g.fillRect(299, 34, 12, 12);
        g.fillRect(297, 31, 16, 2);
        g.drawString("Rac", 305, 52, Graphics.TOP | Graphics.HCENTER);
        g.setColor(Ui.in(curX, curY, 292, 56, 26, 26) ? 0xFFD080 : 0xC08030);
        g.fillRect(292, 56, 26, 26);
        g.setColor(0xFFFFFF);
        g.fillTriangle(305, 60, 297, 70, 313, 70);
        g.fillRect(303, 70, 5, 8);
        g.drawString("Vut", 305, 82, Graphics.TOP | Graphics.HCENTER);
        drawItemBox(g, 292, 86, 26, 26, inv.worn, Ui.in(curX, curY, 292, 86, 26, 26));
        g.setColor(0xFFFFFF);
        g.drawString("Non", 305, 112, Graphics.TOP | Graphics.HCENTER);
        // hotbar
        for (int i = 0; i < 9; i++) {
            drawItemBox(g, 20 + i * 28, 150, 27, 26, inv.hot[i], i == inv.sel || Ui.in(curX, curY, 20 + i * 28, 150, 27, 26));
        }
        g.setColor(Ui.in(curX, curY, 20 + 9 * 28, 150, 27, 26) ? 0xFFE0A0 : 0xC09050);
        g.fillRect(20 + 9 * 28, 150, 27, 26);
        g.setColor(0x503010);
        g.drawRect(20 + 9 * 28, 150, 26, 25);
        g.fillRect(20 + 9 * 28 + 7, 157, 13, 12);
        // info
        Tool over = null;
        int h = packHit();
        if (h >= 200 && h < 200 + Inventory.CELLS) {
            over = inv.store[packTab][h - 200];
        } else if (h >= 400 && h < 409) {
            over = inv.hot[h - 400];
        } else if (h == 302) {
            over = inv.worn;
        }
        g.setColor(0xFFFFFF);
        if (carried != null) {
            g.drawString("Dang cam: " + carried.name + " - bam o de dat", 6, 184, Graphics.TOP | Graphics.LEFT);
        } else if (over != null) {
            g.drawString(over.name + ": " + over.info(), 6, 184, Graphics.TOP | Graphics.LEFT);
        } else {
            g.drawString("Bam mon do de cam, bam o de dat (o duoi = thanh nhanh)", 6, 184,
                    Graphics.TOP | Graphics.LEFT);
        }
        g.setColor(0xC0C0FF);
        g.drawString("Rac = huy, Vut = tha xuong dat (nhat lai khi cham), Non = deo", 6, 198, Graphics.TOP | Graphics.LEFT);
        g.drawString("O 10 / R / Thoat = dong. Het do = dung tay khong", 6, 212, Graphics.TOP | Graphics.LEFT);
        if (carried != null) {
            drawItemBox(g, curX - 2, curY - 2, 26, 26, carried, true);
        }
    }

    private void paintAddon(Graphics g) {
        g.setColor(0x202840);
        g.fillRect(0, 0, sw, sh);
        g.setColor(0xFFD84A);
        int rowH = 18;
        int top = 24;
        int vis = (sh - top - 44) / rowH;
        if (vis < 1) {
            vis = 1;
        }
        if (viewPack != null) {
            g.drawString("ZIP/THU MUC: " + viewPack.name, 6, 4, Graphics.TOP | Graphics.LEFT);
            if (menuSel < menuTop) {
                menuTop = menuSel;
            }
            if (menuSel >= menuTop + vis) {
                menuTop = menuSel - vis + 1;
            }
            for (int i = 0; i < vis; i++) {
                int idx = menuTop + i;
                if (idx >= viewEntries.size()) {
                    break;
                }
                int y = top + i * rowH;
                if (idx == menuSel) {
                    g.setColor(0x405080);
                    g.fillRect(4, y - 1, sw - 8, rowH - 2);
                }
                String e = (String) viewEntries.elementAt(idx);
                g.setColor(e.toLowerCase().endsWith(".sbc") ? 0x80FF80 : 0xA0A0A0);
                if (e.length() > 40) {
                    e = e.substring(0, 40);
                }
                g.drawString(e, 8, y + 1, Graphics.TOP | Graphics.LEFT);
            }
            int sbc = 0;
            for (int i = 0; i < viewEntries.size(); i++) {
                if (((String) viewEntries.elementAt(i)).toLowerCase().endsWith(".sbc")) {
                    sbc++;
                }
            }
            g.setColor(0xB0C0E0);
            g.drawString(sbc > 0 ? sbc + " file .sbc co the kich hoat" : "Khong co file .sbc", 6, sh - 42,
                    Graphics.TOP | Graphics.LEFT);
            g.setColor(0xFFD84A);
            g.drawString(sbc > 0 ? "OK: KICH HOAT   R: quay lai" : "R: quay lai", 6, sh - 15,
                    Graphics.TOP | Graphics.LEFT);
            return;
        }
        g.drawString("ADD-ON  (" + am.addons.size() + " da cai)", 6, 4, Graphics.TOP | Graphics.LEFT);
        if (menuSel < menuTop) {
            menuTop = menuSel;
        }
        if (menuSel >= menuTop + vis) {
            menuTop = menuSel - vis + 1;
        }
        for (int i = 0; i < vis; i++) {
            int idx = menuTop + i;
            if (idx >= menu.size()) {
                break;
            }
            Object o = menu.elementAt(idx);
            int y = top + i * rowH;
            if (idx == menuSel) {
                g.setColor(0x405080);
                g.fillRect(4, y - 1, sw - 8, rowH - 2);
            }
            String txt;
            int col;
            if (o instanceof Addon) {
                Addon a = (Addon) o;
                txt = (a.enabled ? "[BAT] " : "[TAT] ") + a.name + " v" + a.version;
                col = a.enabled ? 0x80FF80 : 0xA0A0A0;
            } else if (o instanceof Pack) {
                Pack pk = (Pack) o;
                txt = (pk.zip ? "[ZIP] " : "[THU MUC] ") + pk.name;
                col = 0x80D0FF;
            } else {
                int si = ((Integer) o).intValue();
                txt = "[+] " + (String) am.storeFiles.elementAt(si) + " (chua cai)";
                col = 0xFFC070;
            }
            if (txt.length() > 38) {
                txt = txt.substring(0, 38);
            }
            g.setColor(col);
            g.drawString(txt, 8, y + 1, Graphics.TOP | Graphics.LEFT);
        }
        if (menu.size() == 0) {
            g.setColor(0xFFFFFF);
            g.drawString("Chua co add-on", 8, top, Graphics.TOP | Graphics.LEFT);
        }
        g.setColor(0xB0C0E0);
        String det = "";
        if (menu.size() > 0) {
            Object o = menu.elementAt(menuSel);
            if (o instanceof Addon) {
                Addon a = (Addon) o;
                det = "tool:" + a.count("tool") + " npc:" + a.count("npc") + " map:" + a.count("map")
                        + " nv:" + a.count("character");
            } else if (o instanceof Pack) {
                det = "OK de xem ben trong";
            } else {
                det = "OK de cai vao Sibubalachu/addons";
            }
        }
        g.drawString(det, 6, sh - 42, Graphics.TOP | Graphics.LEFT);
        g.setColor(0xFFFFFF);
        g.drawString(menuMsg.length() > 0 ? menuMsg : am.info, 6, sh - 29, Graphics.TOP | Graphics.LEFT);
        g.setColor(0xFFD84A);
        g.drawString("OK:chon  L:quet lai  R:quay lai", 6, sh - 15, Graphics.TOP | Graphics.LEFT);
    }

    private void paintNet(Graphics g) {
        g.setColor(0x103020);
        g.fillRect(0, 0, sw, sh);
        g.setColor(0xFFD84A);
        g.drawString("NHIEU NGUOI (Bluetooth, toi da 4)", 6, 4, Graphics.TOP | Graphics.LEFT);
        int rowH = 18;
        int top = 24;
        BtLink b = bt;
        if (roomPending) {
            g.setColor(0xFFFFFF);
            g.drawString("Tim thay phong!", 8, top, Graphics.TOP | Graphics.LEFT);
            g.setColor(0xB0E0C0);
            g.drawString("Map: " + roomName + "  (" + MapInfo.DIFF_NAMES[World.clampInt(roomDiff, 0, 2)] + ")", 8,
                    top + 18, Graphics.TOP | Graphics.LEFT);
            g.drawString("Dia chi: " + roomAddr, 8, top + 34, Graphics.TOP | Graphics.LEFT);
            g.drawString("So nguoi trong phong: " + roomPlayers + "/4", 8, top + 50, Graphics.TOP | Graphics.LEFT);
            g.setColor(roomSig == net.localSig ? 0x80FF80 : 0xFFA060);
            g.drawString(roomSig == net.localSig ? "Add-on: khop" : "Add-on: KHAC (co the lech)", 8, top + 66,
                    Graphics.TOP | Graphics.LEFT);
            g.setColor(0xFFD84A);
            g.drawString("OK = vao (doi load)    R = huy", 6, sh - 15, Graphics.TOP | Graphics.LEFT);
            return;
        }
        if (b != null && b.state == BtLink.S_LIST) {
            int cnt = b.names.size() + 1;
            if (netSel >= cnt) {
                netSel = cnt - 1;
            }
            for (int i = 0; i < cnt && i < 7; i++) {
                int y = top + i * rowH;
                if (i == netSel) {
                    g.setColor(0x305040);
                    g.fillRect(4, y - 1, sw - 8, rowH - 2);
                }
                g.setColor(0xFFFFFF);
                String t = i < b.names.size() ? "Vao: " + (String) b.names.elementAt(i) : "<< Tim lai";
                if (t.length() > 38) {
                    t = t.substring(0, 38);
                }
                g.drawString(t, 8, y + 1, Graphics.TOP | Graphics.LEFT);
            }
        } else {
            String[] base = { netReturn == ST_MAPS ? "Tao phong (host map dang chon)" : "Tao phong (host map nay)",
                    "Tim phong (vao choi)", "Ngat ket noi" };
            for (int i = 0; i < 3; i++) {
                int y = top + i * rowH;
                if (i == netSel) {
                    g.setColor(0x305040);
                    g.fillRect(4, y - 1, sw - 8, rowH - 2);
                }
                g.setColor(0xFFFFFF);
                g.drawString(base[i], 8, y + 1, Graphics.TOP | Graphics.LEFT);
            }
        }
        g.setColor(0xB0E0C0);
        String role = !net.active() ? "Chua ket noi" : (net.mode == Net.HOST ? "Dang la HOST" : "Dang la KHACH");
        g.drawString(role + "  -  nguoi trong phong: " + (1 + remoteCount()), 6, sh - 56, Graphics.TOP | Graphics.LEFT);
        g.setColor(0xFFFFFF);
        String m2 = (b != null) ? b.msg : "";
        if (m2.length() > 40) {
            m2 = m2.substring(0, 40);
        }
        g.drawString(m2, 6, sh - 42, Graphics.TOP | Graphics.LEFT);
        g.setColor(0xB0E0C0);
        g.drawString("Cac may phai bat cung add-on. Truoc do hay ghep doi Bluetooth.", 6, sh - 29,
                Graphics.TOP | Graphics.LEFT);
        g.setColor(0xFFD84A);
        g.drawString("OK: chon    R: quay lai", 6, sh - 15, Graphics.TOP | Graphics.LEFT);
    }
}
