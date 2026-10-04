import java.io.InputStream;
import java.io.OutputStream;
import javax.microedition.io.Connection;

/** One link to another phone. Frames are [len][payload]; a reader thread pushes them into Net. */
public class Peer implements Runnable {
    public int id;
    public volatile boolean alive = true;
    private Net net;
    private InputStream in;
    private OutputStream out;
    private Connection conn;

    public Peer(Net net, int id, InputStream in, OutputStream out, Connection conn) {
        this.net = net;
        this.id = id;
        this.in = in;
        this.out = out;
        this.conn = conn;
    }

    public void start() {
        Thread th = new Thread(this);
        th.start();
    }

    public void run() {
        try {
            while (alive) {
                int len = in.read();
                if (len <= 0) {
                    break;
                }
                byte[] d = new byte[len];
                int got = 0;
                while (got < len) {
                    int n = in.read(d, got, len - got);
                    if (n < 0) {
                        got = -1;
                        break;
                    }
                    got += n;
                }
                if (got < 0) {
                    break;
                }
                net.push(this, d);
            }
        } catch (Throwable t) {
            // link closed or broken
        }
        alive = false;
        net.push(this, null);
    }

    public synchronized void send(byte[] payload) {
        if (!alive) {
            return;
        }
        try {
            byte[] f = new byte[payload.length + 1];
            f[0] = (byte) payload.length;
            System.arraycopy(payload, 0, f, 1, payload.length);
            out.write(f, 0, f.length);
            out.flush();
        } catch (Throwable t) {
            alive = false;
        }
    }

    public void close() {
        alive = false;
        try {
            in.close();
        } catch (Throwable t) {
            // ignore
        }
        try {
            out.close();
        } catch (Throwable t) {
            // ignore
        }
        if (conn != null) {
            try {
                conn.close();
            } catch (Throwable t) {
                // ignore
            }
        }
    }
}
