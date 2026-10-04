import java.util.Vector;

/** A loaded add-on (.sbc file): a list of sections plus info. */
public class Addon {
    public String id;
    public String name;
    public String version = "1";
    public String author = "";
    public String desc = "";
    public boolean enabled = true;
    public boolean builtin = false;
    public Vector sections;

    public Addon(String id, String text) {
        this.id = id;
        this.name = id;
        if (id.toLowerCase().startsWith("addonmsv1") && id.length() > 13) {
            this.name = id.substring(9, id.length() - 4);
        }
        this.sections = Sbc.parse(text);
        for (int i = 0; i < sections.size(); i++) {
            Section s = (Section) sections.elementAt(i);
            if (s.type.equals("addon")) {
                name = s.get("name", id);
                version = s.get("version", "1");
                author = s.get("author", "");
                desc = s.get("desc", "");
                break;
            }
        }
    }

    public int count(String type) {
        int c = 0;
        for (int i = 0; i < sections.size(); i++) {
            if (((Section) sections.elementAt(i)).type.equals(type)) {
                c++;
            }
        }
        return c;
    }
}
