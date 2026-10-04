/** A map in the list: either generated from a seed (address) or defined by an add-on [map] section. */
public class MapInfo {
    public String name;
    public int diff;          // 0 easy, 1 medium, 2 hard
    public String address;    // seed text
    public Section sec;       // non-null for add-on / built-in maps

    public static final String[] DIFF_NAMES = { "De", "Vua", "Kho" };

    public MapInfo(String name, int diff, String address, Section sec) {
        this.name = name;
        this.diff = diff;
        this.address = address;
        this.sec = sec;
    }
}
