import java.io.InputStream;
import java.io.OutputStream;
import java.util.Vector;
import javax.microedition.io.Connection;

/**
 * Star-topology multiplayer (host + up to 3 clients). The host relays messages between clients.
 * Damage is always applied by the victim's own phone; see MsvCanvas.
 * This class does not know about Bluetooth: BtLink (or a test) hands it connected streams.
 */
public class Net {
    public static final int OFF = 0;
    public static final int HOST = 1;
    public static final int CLIENT = 2;
    public static final int MAXP = 4;
    public static final int VERSION = 1;

    public static final int T_HELLO = 1;
    public static final int T_WELCOME = 2;
    public static final int T_STATE = 3;
    public static final int T_SHOT = 4;
    public static final int T_MELEE = 5;
    public static final int T_LEAVE = 6;
    public static final int T_ROOM = 7;

    public volatile int mode = OFF;
    public int myId = 0;
    public int localSig = 0;
    private Vector peers = new Vector();
    private Vector inbox = new Vector();

    public boolean active() {
        return mode != OFF;
    }

    public synchronized int peerCount() {
        return peers.size();
    }

    public synchronized boolean hasPeer(Peer p) {
        return peers.contains(p);
    }

    private int freeId() {
        for (int id = 1; id < MAXP; id++) {
            boolean used = false;
            for (int i = 0; i < peers.size(); i++) {
                if (((Peer) peers.elementAt(i)).id == id) {
                    used = true;
                }
            }
            if (!used) {
                return id;
            }
        }
        return -1;
    }

    /** Called when a link is connected. Returns false (and closes the link) if it is refused. */
    public synchronized boolean addPeer(InputStream in, OutputStream out, Connection conn) {
        int id = 0;
        boolean ok = true;
        if (mode == HOST) {
            id = freeId();
            ok = id >= 0;
        } else if (mode == CLIENT) {
            ok = peers.size() == 0;
        } else {
            ok = false;
        }
        Peer p = new Peer(this, id, in, out, conn);
        if (!ok) {
            p.close();
            return false;
        }
        peers.addElement(p);
        p.start();
        if (mode == CLIENT) {
            byte[] h = new byte[7];
            h[0] = (byte) T_HELLO;
            h[1] = 0;
            h[2] = (byte) VERSION;
            w32(h, 3, localSig);
            p.send(h);
        }
        return true;
    }

    public synchronized void removePeer(Peer p) {
        peers.removeElement(p);
        p.close();
    }

    public synchronized void stop() {
        for (int i = 0; i < peers.size(); i++) {
            ((Peer) peers.elementAt(i)).close();
        }
        peers.removeAllElements();
        inbox.removeAllElements();
        mode = OFF;
        myId = 0;
    }

    /** Host: to every client. Client: to the host. */
    public synchronized void send(byte[] d) {
        for (int i = 0; i < peers.size(); i++) {
            ((Peer) peers.elementAt(i)).send(d);
        }
    }

    public synchronized void relay(byte[] d, Peer except) {
        for (int i = 0; i < peers.size(); i++) {
            Peer p = (Peer) peers.elementAt(i);
            if (p != except) {
                p.send(d);
            }
        }
    }

    public void push(Peer p, byte[] d) {
        synchronized (inbox) {
            inbox.addElement(new Object[] { p, d });
        }
    }

    /** Next received message as {Peer, byte[]} (byte[] null = link lost), or null if none. */
    public Object[] next() {
        synchronized (inbox) {
            if (inbox.size() == 0) {
                return null;
            }
            Object[] m = (Object[]) inbox.elementAt(0);
            inbox.removeElementAt(0);
            return m;
        }
    }

    // ------------------------------------------------------------------ message helpers

    public static void w16(byte[] b, int o, int v) {
        b[o] = (byte) (v >> 8);
        b[o + 1] = (byte) v;
    }

    public static int r16(byte[] b, int o) {
        return (short) (((b[o] & 0xFF) << 8) | (b[o + 1] & 0xFF));
    }

    public static void w32(byte[] b, int o, int v) {
        b[o] = (byte) (v >> 24);
        b[o + 1] = (byte) (v >> 16);
        b[o + 2] = (byte) (v >> 8);
        b[o + 3] = (byte) v;
    }

    public static int r32(byte[] b, int o) {
        return ((b[o] & 0xFF) << 24) | ((b[o + 1] & 0xFF) << 16) | ((b[o + 2] & 0xFF) << 8) | (b[o + 3] & 0xFF);
    }

    public static int rnd(float v) {
        return (int) (v + 40000.5f) - 40000;
    }

    public static int clampByte(int v) {
        if (v > 127) {
            return 127;
        }
        if (v < -127) {
            return -127;
        }
        return v;
    }

    public static byte[] welcome(int yourId, int sig) {
        byte[] d = new byte[7];
        d[0] = (byte) T_WELCOME;
        d[1] = 0;
        d[2] = (byte) yourId;
        w32(d, 3, sig);
        return d;
    }

    public static byte[] state(int id, int x, int y, float vx, int face, int hp, int flags, int tool) {
        byte[] d = new byte[11];
        d[0] = (byte) T_STATE;
        d[1] = (byte) id;
        w16(d, 2, x);
        w16(d, 4, y);
        d[6] = (byte) clampByte(rnd(vx * 20f));
        d[7] = (byte) face;
        d[8] = (byte) hp;
        d[9] = (byte) flags;
        d[10] = (byte) tool;
        return d;
    }

    /** type is T_SHOT or T_MELEE. */
    public static byte[] fire(int type, int id, int tool, int sx, int sy, float ax, float ay) {
        byte[] d = new byte[9];
        d[0] = (byte) type;
        d[1] = (byte) id;
        d[2] = (byte) tool;
        w16(d, 3, sx);
        w16(d, 5, sy);
        d[7] = (byte) clampByte(rnd(ax * 100f));
        d[8] = (byte) clampByte(rnd(ay * 100f));
        return d;
    }

    public static byte[] leave(int id) {
        byte[] d = new byte[2];
        d[0] = (byte) T_LEAVE;
        d[1] = (byte) id;
        return d;
    }

    private static byte[] utf8(String s, int maxChars) {
        if (s == null) {
            s = "";
        }
        if (s.length() > maxChars) {
            s = s.substring(0, maxChars);
        }
        try {
            return s.getBytes("UTF-8");
        } catch (Exception e) {
            return s.getBytes();
        }
    }

    public static String str(byte[] d, int off, int len) {
        try {
            return new String(d, off, len, "UTF-8");
        } catch (Exception e) {
            return new String(d, off, len);
        }
    }

    /** Host -> client: who you are, who is in the room and which map it is. */
    public static byte[] room(int yourId, int players, int diff, int sig, String name, String addr) {
        byte[] nb = utf8(name, 20);
        byte[] ab = utf8(addr, 20);
        byte[] d = new byte[11 + nb.length + ab.length];
        d[0] = (byte) T_ROOM;
        d[1] = 0;
        d[2] = (byte) yourId;
        d[3] = (byte) players;
        d[4] = (byte) diff;
        w32(d, 5, sig);
        d[9] = (byte) nb.length;
        System.arraycopy(nb, 0, d, 10, nb.length);
        d[10 + nb.length] = (byte) ab.length;
        System.arraycopy(ab, 0, d, 11 + nb.length, ab.length);
        return d;
    }
}
