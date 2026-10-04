/** 9 hotbar slots (slot 10 is the backpack button), a per-category backpack, and a worn hat. */
public class Inventory {
    public static final int HOT = 9;
    public static final int COLS = 8;
    public static final int ROWS = 3;
    public static final int CELLS = COLS * ROWS;
    public static final int CATS = 4;

    public Tool[] hot = new Tool[HOT];
    public Tool[][] store = new Tool[CATS][CELLS];
    public Tool worn;
    public int sel;

    public Tool current() {
        Tool t = hot[sel];
        return t == null ? Tool.FIST : t;
    }

    public void clear() {
        for (int i = 0; i < HOT; i++) {
            hot[i] = null;
        }
        for (int c = 0; c < CATS; c++) {
            for (int i = 0; i < CELLS; i++) {
                store[c][i] = null;
            }
        }
        worn = null;
        sel = 0;
    }

    public boolean addStore(Tool t) {
        Tool[] a = store[t.category];
        for (int i = 0; i < CELLS; i++) {
            if (a[i] == null) {
                a[i] = t;
                return true;
            }
        }
        return false;
    }

    /** Hotbar first, then the backpack. */
    public boolean add(Tool t) {
        for (int i = 0; i < HOT; i++) {
            if (hot[i] == null) {
                hot[i] = t;
                return true;
            }
        }
        return addStore(t);
    }
}
