package com.studio.offline;

import android.util.Base64;
import android.webkit.JavascriptInterface;

import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Exposed to pages loaded from the studio's own host as window.AndroidStudio.
 * Methods run on a background thread, so file writes happen off the UI thread.
 */
public class NativeBridge {

    private final MainActivity activity;
    private final Map<String, OutputStream> streams = new HashMap<>();

    NativeBridge(MainActivity activity) {
        this.activity = activity;
    }

    /** Opens the save-location screen and returns a request id. The page waits for __studioSaveReady. */
    @JavascriptInterface
    public String openSave(final String name, final String mime) {
        final String id = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        activity.runOnUiThread(() -> activity.startSavePicker(id, name, mime));
        return id;
    }

    synchronized void attachStream(String id, OutputStream os) {
        streams.put(id, os);
    }

    /** Appends one base64-encoded chunk to the file chosen by the user. */
    @JavascriptInterface
    public boolean writeChunk(String id, String base64) {
        OutputStream os;
        synchronized (this) {
            os = streams.get(id);
        }
        if (os == null) return false;
        try {
            os.write(Base64.decode(base64, Base64.DEFAULT));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @JavascriptInterface
    public boolean closeSave(String id) {
        OutputStream os;
        synchronized (this) {
            os = streams.remove(id);
        }
        if (os == null) return false;
        try {
            os.flush();
            os.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** Used by the offline page's "retry" button. */
    @JavascriptInterface
    public void retry() {
        activity.reloadStudio();
    }

    @JavascriptInterface
    public void toast(String message) {
        activity.toast(message);
    }

    @JavascriptInterface
    public String appVersion() {
        return "1.0";
    }

    void closeAll() {
        synchronized (this) {
            for (OutputStream os : streams.values()) {
                try {
                    os.close();
                } catch (Exception ignored) {
                }
            }
            streams.clear();
        }
    }
}
