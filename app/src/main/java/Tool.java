/** An item definition read from a [tool] section: weapon, tool, food or hat. Plain ints (J2ME friendly). */
public class Tool {
    public static final int MELEE = 0;
    public static final int SHOT = 1;
    public static final int BOMB = 2;
    public static final int HOOK = 3;
    public static final int KICK = 4;
    public static final int FOOD = 5;
    public static final int HAT = 6;

    public static final int CAT_WEAPON = 0;
    public static final int CAT_TOOL = 1;
    public static final int CAT_FOOD = 2;
    public static final int CAT_CLOTH = 3;
    public static final String[] CAT_NAMES = { "Vu khi", "Tool", "Do an", "Trang phuc" };

    /** Bare hands: used whenever the selected slot is empty. */
    public static final Tool FIST = new Tool();

    public String name = "Tay khong";
    public int kind = MELEE;
    public int category = CAT_WEAPON;
    public int damage = 4;
    public int cooldown = 10;
    public int range = 16;
    public int speed = 8;
    public int life = 40;
    public int color = 0x808080;
    public int count = 1;
    public int spread = 0;     // hundredths of a radian between pellets
    public int radius = 40;
    public int fuse = 45;
    public int len = 0;        // drawn length in pixels
    public int grav = 0;       // hundredths of px/frame^2
    public int knock = 2;
    public int heal = 20;      // food
    public boolean auto = false;   // keeps firing while the button is held
    public int speedBonus = 35;    // percent, slippers
    public int jumpBonus = 18;     // percent, slippers
    public int pull = 5;           // hook pull speed
    public int stock = 1;          // copies placed in the backpack at the start
    public int width = 0;          // projectile thickness in pixels (0 = thin bullet)
    public boolean pierce = false; // projectile passes through targets (each hit once)

    public static Tool fromSection(Section s) {
        Tool t = new Tool();
        t.name = s.get("name", "Tool");
        String b = s.get("behavior", "melee").toLowerCase();
        if (b.equals("projectile") || b.equals("shot") || b.equals("gun")) {
            t.kind = SHOT;
        } else if (b.equals("explosive") || b.equals("bomb")) {
            t.kind = BOMB;
        } else if (b.equals("hook") || b.equals("grapple")) {
            t.kind = HOOK;
        } else if (b.equals("kick") || b.equals("slipper")) {
            t.kind = KICK;
        } else if (b.equals("food")) {
            t.kind = FOOD;
        } else if (b.equals("hat")) {
            t.kind = HAT;
        } else {
            t.kind = MELEE;
        }
        if (t.kind == FOOD) {
            t.category = CAT_FOOD;
        } else if (t.kind == HAT) {
            t.category = CAT_CLOTH;
        } else if (t.kind == HOOK || t.kind == KICK) {
            t.category = CAT_TOOL;
        } else {
            t.category = CAT_WEAPON;
        }
        String cat = s.get("category", "").toLowerCase();
        if (cat.equals("weapon")) {
            t.category = CAT_WEAPON;
        } else if (cat.equals("tool")) {
            t.category = CAT_TOOL;
        } else if (cat.equals("food")) {
            t.category = CAT_FOOD;
        } else if (cat.equals("clothing") || cat.equals("cloth") || cat.equals("hat")) {
            t.category = CAT_CLOTH;
        }
        t.damage = s.getInt("damage", t.damage);
        t.cooldown = Math.max(1, s.getInt("cooldown", t.cooldown));
        t.range = s.getInt("range", t.range);
        t.speed = s.getInt("speed", t.speed);
        t.life = s.getInt("life", t.life);
        t.color = s.getColor("color", t.color);
        t.count = Math.min(9, Math.max(1, s.getInt("count", t.count)));
        t.spread = s.getInt("spread", t.spread);
        t.radius = s.getInt("radius", t.radius);
        t.fuse = s.getInt("fuse", t.fuse);
        t.len = s.getInt("length", t.len);
        t.grav = s.getInt("gravity", t.grav);
        t.knock = s.getInt("knock", t.knock);
        t.heal = s.getInt("heal", t.heal);
        t.auto = s.getInt("auto", 0) != 0;
        t.speedBonus = s.getInt("speedbonus", t.speedBonus);
        t.jumpBonus = s.getInt("jumpbonus", t.jumpBonus);
        t.pull = s.getInt("pull", t.pull);
        t.stock = Math.max(1, s.getInt("stock", t.stock));
        t.width = Math.max(0, Math.min(30, s.getInt("width", t.width)));
        t.pierce = s.getInt("pierce", 0) != 0;
        return t;
    }

    public String shortName() {
        if (name.length() <= 5) {
            return name;
        }
        return name.substring(0, 5);
    }

    public boolean isAttack() {
        return kind == MELEE || kind == SHOT || kind == BOMB || kind == KICK;
    }

    public String info() {
        switch (kind) {
            case MELEE:
                return "can chien, sat thuong " + damage;
            case SHOT:
                return "ban dan, sat thuong " + damage + (auto ? ", giu de ban lien" : "");
            case BOMB:
                return "bom no, sat thuong " + damage;
            case HOOK:
                return "moc: leo / keo dich";
            case KICK:
                return "dep: chay nhanh, nhay cao, da dep";
            case FOOD:
                return "an de hoi " + heal + " mau";
            default:
                return "non (mac vao o Non)";
        }
    }
}
