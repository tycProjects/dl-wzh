import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;
import javax.microedition.io.file.FileSystemRegistry;
import javax.microedition.rms.RecordStore;

/**
 * Loads add-ons: the built-in /default.sbc (inside the JAR) plus every *.sbc found in
 * <root>/Sibubalachu/addons/ on the phone / memory card. *.sbc files in
 * <root>/Sibubalachu/store/ are "available" and can be installed (copied) from the menu.
 * Enabled/disabled state is kept in RMS so it survives game updates.
 */
public class AddonManager {
    public static final String BASE = "Sibubalachu/";
    public static final String DIR = "Sibubalachu/addons/";
    public static final String STORE = "Sibubalachu/store/";

    public Vector addons = new Vector();
    public Vector storeFiles = new Vector();
    public Vector storeRoots = new Vector();
    public Vector packs = new Vector();
    public String info = "";
    private Hashtable prefs = new Hashtable();

    public void loadAll() {
        addons.removeAllElements();
        storeFiles.removeAllElements();
        storeRoots.removeAllElements();
        packs.removeAllElements();
        loadPrefs();

        String t = readResource("/default.sbc");
        if (t != null) {
            Addon a = new Addon("default.sbc", t);
            a.builtin = true;
            a.enabled = true;
            addons.addElement(a);
        }

        int found = 0;
        Vector roots = roots();
        for (int i = 0; i < roots.size(); i++) {
            String root = (String) roots.elementAt(i);
            Vector names = listSbc(root + DIR);
            for (int k = 0; k < names.size(); k++) {
                String name = (String) names.elementAt(k);
                if (hasAddon(name)) {
                    continue;
                }
                String txt = readFile(root + DIR + name);
                if (txt != null) {
                    Addon a = new Addon(name, txt);
                    a.enabled = isEnabled(name);
                    addons.addElement(a);
                    found++;
                }
            }
        }
        for (int i = 0; i < roots.size(); i++) {
            String root = (String) roots.elementAt(i);
            Vector names = listSbc(root + STORE);
            for (int k = 0; k < names.size(); k++) {
                String name = (String) names.elementAt(k);
                if (!hasAddon(name) && !inStore(name)) {
                    storeFiles.addElement(name);
                    storeRoots.addElement(root);
                }
            }
        }
        for (int i = 0; i < roots.size(); i++) {
            scanPacks((String) roots.elementAt(i));
        }
        if (roots.size() == 0) {
            info = "Khong doc duoc the nho";
        } else {
            info = "SD: " + found + " addon, " + storeFiles.size() + " trong kho, " + packs.size() + " zip/thu muc";
        }
    }

    /** Same value on every phone that has the same add-ons enabled (used to warn about mismatches). */
    public int signature() {
        int sum = 0;
        for (int i = 0; i < addons.size(); i++) {
            Addon a = (Addon) addons.elementAt(i);
            if (a.enabled) {
                sum += (a.id + "|" + a.version).hashCode();
            }
        }
        return sum;
    }

    private boolean hasAddon(String id) {
        for (int i = 0; i < addons.size(); i++) {
            if (((Addon) addons.elementAt(i)).id.equals(id)) {
                return true;
            }
        }
        return false;
    }

    private boolean inStore(String id) {
        for (int i = 0; i < storeFiles.size(); i++) {
            if (((String) storeFiles.elementAt(i)).equals(id)) {
                return true;
            }
        }
        return false;
    }

    private boolean isEnabled(String id) {
        Object o = prefs.get(id);
        if (o == null) {
            return true;
        }
        return "1".equals((String) o);
    }

    public void setEnabled(Addon a, boolean v) {
        a.enabled = v;
        prefs.put(a.id, v ? "1" : "0");
        savePrefs();
    }

    /** Copies store/<name> into addons/. Returns true on success. */
    public boolean install(int idx) {
        try {
            String name = (String) storeFiles.elementAt(idx);
            String root = (String) storeRoots.elementAt(idx);
            byte[] data = readBytes(root + STORE + name);
            if (data == null) {
                return false;
            }
            ensureDir(root + BASE);
            ensureDir(root + DIR);
            return writeBytes(root + DIR + name, data);
        } catch (Throwable t) {
            return false;
        }
    }

    // ------------------------------------------------------------------ files

    private Vector roots() {
        Vector v = new Vector();
        try {
            Enumeration e = FileSystemRegistry.listRoots();
            while (e.hasMoreElements()) {
                String r = (String) e.nextElement();
                if (!r.endsWith("/")) {
                    r = r + "/";
                }
                v.addElement("file:///" + r);
            }
        } catch (Throwable t) {
            // no JSR-75 or permission denied
        }
        return v;
    }

    private Vector listSbc(String dirUrl) {
        Vector names = new Vector();
        FileConnection fc = null;
        try {
            fc = (FileConnection) Connector.open(dirUrl, Connector.READ);
            if (fc.exists() && fc.isDirectory()) {
                Enumeration e = fc.list("*", false);
                while (e.hasMoreElements()) {
                    String n = (String) e.nextElement();
                    String ln = n.toLowerCase();
                    if (ln.startsWith("addonmsv1") && ln.endsWith(".sbc")) {
                        names.addElement(n);
                    }
                }
            }
        } catch (Throwable t) {
            // ignore: folder missing or no access
        }
        closeFc(fc);
        return names;
    }

    private Vector listAll(String dirUrl) {
        Vector names = new Vector();
        FileConnection fc = null;
        try {
            fc = (FileConnection) Connector.open(dirUrl, Connector.READ);
            if (fc.exists() && fc.isDirectory()) {
                Enumeration e = fc.list("*", false);
                while (e.hasMoreElements()) {
                    names.addElement((String) e.nextElement());
                }
            }
        } catch (Throwable t) {
            // ignore
        }
        closeFc(fc);
        return names;
    }

    private boolean hasPack(String url) {
        for (int i = 0; i < packs.size(); i++) {
            if (((Pack) packs.elementAt(i)).url.equals(url)) {
                return true;
            }
        }
        return false;
    }

    /** Finds .zip files in Sibubalachu/, addons/, store/ and sub-folders of addons/ and store/. */
    private void scanPacks(String root) {
        String[] dirs = { root + BASE, root + DIR, root + STORE };
        for (int d = 0; d < dirs.length; d++) {
            Vector names = listAll(dirs[d]);
            for (int i = 0; i < names.size(); i++) {
                String n = (String) names.elementAt(i);
                String ln = n.toLowerCase();
                String url = dirs[d] + n;
                if (ln.endsWith(".zip") && !hasPack(url)) {
                    Pack p = new Pack();
                    p.name = n;
                    p.zip = true;
                    p.url = url;
                    p.root = root;
                    packs.addElement(p);
                } else if (d > 0 && n.endsWith("/") && !hasPack(url)) {
                    Pack p = new Pack();
                    p.name = n.substring(0, n.length() - 1);
                    p.zip = false;
                    p.url = url;
                    p.root = root;
                    packs.addElement(p);
                }
            }
        }
    }

    /** Names of everything inside a zip / folder (for the "view" screen). */
    public Vector packEntries(Pack p) {
        Vector out = new Vector();
        if (p.zip) {
            byte[] z = readBytes(p.url, 1500000);
            if (z == null) {
                out.addElement("(khong doc duoc file zip - lon qua?)");
                return out;
            }
            Vector es = Zip.list(z);
            for (int i = 0; i < es.size(); i++) {
                String n = ((ZipEntry) es.elementAt(i)).name;
                if (!n.endsWith("/")) {
                    out.addElement(n);
                }
            }
            if (out.size() == 0) {
                out.addElement("(zip rong hoac hong)");
            }
        } else {
            Vector names = listAll(p.url);
            for (int i = 0; i < names.size(); i++) {
                out.addElement(names.elementAt(i));
            }
            if (out.size() == 0) {
                out.addElement("(thu muc rong)");
            }
        }
        return out;
    }

    private static String installName(String path) {
        int s = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        String base = s >= 0 ? path.substring(s + 1) : path;
        if (!base.toLowerCase().startsWith("addonmsv1")) {
            base = "AddonMsv1" + base;
        }
        return base;
    }

    /** Copies every .sbc in the pack into addons/. Returns how many were installed. */
    public int activatePack(Pack p) {
        int count = 0;
        try {
            ensureDir(p.root + BASE);
            ensureDir(p.root + DIR);
            if (p.zip) {
                byte[] z = readBytes(p.url, 1500000);
                if (z == null) {
                    return 0;
                }
                Vector es = Zip.list(z);
                for (int i = 0; i < es.size(); i++) {
                    ZipEntry e = (ZipEntry) es.elementAt(i);
                    if (e.name.toLowerCase().endsWith(".sbc")) {
                        byte[] data = Zip.read(z, e);
                        if (data != null && writeBytes(p.root + DIR + installName(e.name), data)) {
                            count++;
                        }
                    }
                }
            } else {
                Vector names = listAll(p.url);
                for (int i = 0; i < names.size(); i++) {
                    String n = (String) names.elementAt(i);
                    if (n.toLowerCase().endsWith(".sbc")) {
                        byte[] data = readBytes(p.url + n);
                        if (data != null && writeBytes(p.root + DIR + installName(n), data)) {
                            count++;
                        }
                    }
                }
            }
        } catch (Throwable t) {
            // return what was installed
        }
        return count;
    }

    private void ensureDir(String url) {
        FileConnection fc = null;
        try {
            fc = (FileConnection) Connector.open(url, Connector.READ_WRITE);
            if (!fc.exists()) {
                fc.mkdir();
            }
        } catch (Throwable t) {
            // ignore
        }
        closeFc(fc);
    }

    private void closeFc(FileConnection fc) {
        if (fc != null) {
            try {
                fc.close();
            } catch (Throwable t) {
                // ignore
            }
        }
    }

    private byte[] readBytes(String url) {
        return readBytes(url, 65536);
    }

    private byte[] readBytes(String url, int limit) {
        FileConnection fc = null;
        InputStream is = null;
        byte[] result = null;
        try {
            fc = (FileConnection) Connector.open(url, Connector.READ);
            if (fc.exists()) {
                is = fc.openInputStream();
                result = readAll(is, limit);
            }
        } catch (Throwable t) {
            result = null;
        }
        try {
            if (is != null) {
                is.close();
            }
        } catch (Throwable t2) {
            // ignore
        }
        closeFc(fc);
        return result;
    }

    private boolean writeBytes(String url, byte[] data) {
        FileConnection fc = null;
        OutputStream os = null;
        boolean ok = false;
        try {
            fc = (FileConnection) Connector.open(url, Connector.READ_WRITE);
            if (fc.exists()) {
                fc.truncate(0);
            } else {
                fc.create();
            }
            os = fc.openOutputStream();
            os.write(data);
            os.flush();
            ok = true;
        } catch (Throwable t) {
            ok = false;
        }
        try {
            if (os != null) {
                os.close();
            }
        } catch (Throwable t2) {
            // ignore
        }
        closeFc(fc);
        return ok;
    }

    private String readFile(String url) {
        return toText(readBytes(url));
    }

    private String readResource(String name) {
        try {
            InputStream is = getClass().getResourceAsStream(name);
            if (is == null) {
                String p = name.startsWith("/") ? name.substring(1) : name;
                is = androidport.AndroidBridge.openAsset(p);
            }
            if (is == null) {
                return null;
            }
            byte[] b = readAll(is, 65536);
            is.close();
            return toText(b);
        } catch (Throwable t) {
            return null;
        }
    }

    private byte[] readAll(InputStream is, int limit) throws java.io.IOException {
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        byte[] b = new byte[512];
        int n;
        int total = 0;
        while ((n = is.read(b)) != -1) {
            bo.write(b, 0, n);
            total += n;
            if (total > limit) {
                return null;
            }
        }
        return bo.toByteArray();
    }

    private String toText(byte[] b) {
        if (b == null) {
            return null;
        }
        try {
            return new String(b, "UTF-8");
        } catch (Exception e) {
            return new String(b);
        }
    }

    // ------------------------------------------------------------------ prefs (RMS)

    private void loadPrefs() {
        prefs.clear();
        try {
            RecordStore rs = RecordStore.openRecordStore("msv_cfg", true);
            try {
                if (rs.getNumRecords() > 0) {
                    String s = new String(rs.getRecord(1));
                    String[] lines = Sbc.split(s, '\n');
                    for (int i = 0; i < lines.length; i++) {
                        int eq = lines[i].lastIndexOf('=');
                        if (eq > 0) {
                            prefs.put(lines[i].substring(0, eq), lines[i].substring(eq + 1).trim());
                        }
                    }
                }
            } finally {
                rs.closeRecordStore();
            }
        } catch (Exception e) {
            // ignore: defaults
        }
    }

    private void savePrefs() {
        try {
            StringBuffer sb = new StringBuffer();
            Enumeration k = prefs.keys();
            while (k.hasMoreElements()) {
                String key = (String) k.nextElement();
                sb.append(key).append('=').append((String) prefs.get(key)).append('\n');
            }
            byte[] d = sb.toString().getBytes();
            RecordStore rs = RecordStore.openRecordStore("msv_cfg", true);
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
}
