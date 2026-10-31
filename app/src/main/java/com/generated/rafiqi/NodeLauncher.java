package com.generated.rafiqi;

import android.app.Activity;
import android.content.Context;
import android.system.Os;
import android.webkit.WebView;
import com.wzh.nodert.NodeBridge;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** Starts the bundled Node.js server (nodejs-mobile) once per process and loads it in the WebView. */
public final class NodeLauncher {
    private static final Object LOCK = new Object();
    private static boolean started = false;
    private static volatile String startError = null;
    private static int port = 0;

    private NodeLauncher() {}

    private static synchronized int port() {
        if (port == 0) {
            try (ServerSocket s = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))) {
                port = s.getLocalPort();
            } catch (Exception e) {
                port = 30000 + (int) (Math.random() * 20000);
            }
        }
        return port;
    }

    public static String baseUrl() {
        return "http://127.0.0.1:" + port() + "/";
    }

    public static void startThenLoad(final Activity act, final WebView wv, final String url) {
        final Context app = act.getApplicationContext();
        new Thread(() -> {
            String err = null;
            try {
                synchronized (LOCK) {
                    if (!started) {
                        startNode(app);
                        started = true;
                    }
                }
                if (!waitForServer(port(), 40000)) {
                    err = startError != null ? startError : "The app's server did not start in time.";
                }
            } catch (Throwable t) {
                err = String.valueOf(t);
            }
            final String message = err;
            act.runOnUiThread(() -> {
                if (act.isFinishing()) return;
                if (message == null) {
                    wv.loadUrl(url);
                } else {
                    String safe = message.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
                    wv.loadDataWithBaseURL(null,
                        "<html><body style='font-family:sans-serif;padding:24px'><h3>The app could not start its server</h3><pre style='white-space:pre-wrap'>"
                            + safe + "</pre></body></html>", "text/html", "UTF-8", null);
                }
            });
        }, "wzh-node-boot").start();
    }

    private static void startNode(Context ctx) throws Exception {
        File base = new File(ctx.getFilesDir(), "nodejs-project");
        syncProject(ctx, base);
        File data = new File(ctx.getFilesDir(), "nodejs-data");
        data.mkdirs();
        Os.setenv("PORT", String.valueOf(port()), true);
        Os.setenv("HOST", "127.0.0.1", true);
        Os.setenv("NODE_ENV", "production", true);
        Os.setenv("HOME", ctx.getFilesDir().getAbsolutePath(), true);
        Os.setenv("TMPDIR", ctx.getCacheDir().getAbsolutePath(), true);
        Os.setenv("WZH_DATA_DIR", data.getAbsolutePath(), true);
        final String script = new File(base, "wzh-node-start.cjs").getAbsolutePath();
        System.loadLibrary("node");
        System.loadLibrary("wzhnode");
        Thread t = new Thread(null, () -> {
            try {
                int rc = NodeBridge.start(new String[] { "node", script });
                if (startError == null) startError = "The server process exited (code " + rc + ").";
            } catch (Throwable x) {
                startError = String.valueOf(x);
            }
        }, "wzh-node", 2 * 1024 * 1024);
        t.start();
    }

    // Unpacks assets/nodejs-project.zip into filesDir. Files are overwritten but never deleted,
    // so data the server wrote into its own folder survives app updates.
    private static void syncProject(Context ctx, File base) throws Exception {
        String ver = readAsset(ctx, "nodejs-project.ver").trim();
        File stamp = new File(ctx.getFilesDir(), "nodejs-project.ver");
        if (base.isDirectory() && stamp.isFile() && ver.equals(new String(readFile(stamp), "UTF-8").trim())) return;
        base.mkdirs();
        String basePath = base.getCanonicalPath() + File.separator;
        try (ZipInputStream zin = new ZipInputStream(new BufferedInputStream(ctx.getAssets().open("nodejs-project.zip")))) {
            ZipEntry e;
            byte[] buf = new byte[65536];
            while ((e = zin.getNextEntry()) != null) {
                File out = new File(base, e.getName());
                String canon = out.getCanonicalPath();
                if (!canon.startsWith(basePath)) continue; // zip-slip guard
                if (e.isDirectory()) { out.mkdirs(); continue; }
                File parent = out.getParentFile();
                if (parent != null) parent.mkdirs();
                try (OutputStream os = new FileOutputStream(out)) {
                    int n;
                    while ((n = zin.read(buf)) > 0) os.write(buf, 0, n);
                }
            }
        }
        try (OutputStream os = new FileOutputStream(stamp)) { os.write(ver.getBytes("UTF-8")); }
    }

    private static String readAsset(Context ctx, String name) throws Exception {
        try (InputStream in = ctx.getAssets().open(name)) {
            return new String(readAll(in), "UTF-8");
        }
    }

    private static byte[] readFile(File f) throws Exception {
        try (InputStream in = new java.io.FileInputStream(f)) { return readAll(in); }
    }

    private static byte[] readAll(InputStream in) throws Exception {
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        byte[] b = new byte[4096];
        int n;
        while ((n = in.read(b)) > 0) bo.write(b, 0, n);
        return bo.toByteArray();
    }

    private static boolean waitForServer(int p, long timeoutMs) {
        long end = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < end) {
            if (startError != null) return false;
            try (Socket s = new Socket()) {
                s.connect(new InetSocketAddress("127.0.0.1", p), 300);
                return true;
            } catch (Exception ignored) {
                try { Thread.sleep(150); } catch (InterruptedException ie) { return false; }
            }
        }
        return false;
    }
}
