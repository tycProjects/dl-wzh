/** NPC / mob definition read from a [npc] section. */
public class NpcDef {
    public String name = "NPC";
    public int hp = 60;
    public int color = 0x7A2020;
    public String tool = "Dao";
    public int speed = 90;    // percent of normal walking speed
    public int brain = 1;     // 1 simple, 2 avoids gaps / flees, 3 dodges bullets
    public int head = 5;

    public static NpcDef fromSection(Section s) {
        NpcDef d = new NpcDef();
        d.name = s.get("name", "NPC");
        d.hp = Math.max(10, s.getInt("hp", d.hp));
        d.color = s.getColor("color", d.color);
        d.tool = s.get("tool", d.tool);
        d.speed = Math.max(40, Math.min(160, s.getInt("speed", d.speed)));
        d.brain = Math.max(1, Math.min(3, s.getInt("brain", d.brain)));
        d.head = Math.max(3, Math.min(8, s.getInt("head", d.head)));
        return d;
    }
}
