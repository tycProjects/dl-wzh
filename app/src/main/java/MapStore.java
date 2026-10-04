import java.util.Vector;
import javax.microedition.rms.RecordStore;

/** Saves the list of created maps (name | difficulty | address) in RMS. */
public class MapStore {
    private static final String RS = "msv_maps";

    public Vector list() {
        Vector out = new Vector();
        try {
            RecordStore rs = RecordStore.openRecordStore(RS, true);
            try {
                if (rs.getNumRecords() > 0) {
                    String s = new String(rs.getRecord(1), "UTF-8");
                    String[] lines = Sbc.split(s, '\n');
                    for (int i = 0; i < lines.length; i++) {
                        String[] p = Sbc.split(lines[i], '|');
                        if (p.length >= 3 && p[0].length() > 0) {
                            out.addElement(new MapInfo(p[0], Sbc.num(p[1], 0), p[2], null));
                        }
                    }
                }
            } finally {
                rs.closeRecordStore();
            }
        } catch (Exception e) {
            // ignore: empty list
        }
        return out;
    }

    private void save(Vector maps) {
        try {
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < maps.size(); i++) {
                MapInfo m = (MapInfo) maps.elementAt(i);
                sb.append(clean(m.name)).append('|').append(m.diff).append('|').append(clean(m.address)).append('\n');
            }
            byte[] d;
            try {
                d = sb.toString().getBytes("UTF-8");
            } catch (Exception e) {
                d = sb.toString().getBytes();
            }
            RecordStore rs = RecordStore.openRecordStore(RS, true);
            try {
                if (rs.getNumRecords() == 0) {
                    rs.addRecord(d, 0, d.length);
                } else {
                    rs.setRecord(1, d, 0, d.length);
                }
            } finally {
                rs.closeRecordStore();
            }
        } catch (Exception e) {
            // ignore
        }
    }

    private static String clean(String s) {
        return s.replace('|', '/').replace('\n', ' ').replace('\r', ' ');
    }

    public void add(MapInfo m) {
        Vector v = list();
        v.addElement(m);
        while (v.size() > 40) {
            v.removeElementAt(0);
        }
        save(v);
    }

    public void remove(int index) {
        Vector v = list();
        if (index >= 0 && index < v.size()) {
            v.removeElementAt(index);
            save(v);
        }
    }
}
