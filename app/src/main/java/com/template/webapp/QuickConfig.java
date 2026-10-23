package com.template.webapp;

import android.content.Context;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import org.json.JSONObject;

public final class QuickConfig {
    private QuickConfig() {}

    private static boolean loaded = false;
    private static String remoteUrl = "";
    private static boolean fastCache = false;

    // Called first thing in MainActivity.onCreate, before any WebView setting reads these.
    public static synchronized void init(Context context) {
        if (loaded) return;
        loaded = true;
        try (InputStream in = context.getApplicationContext().getAssets().open("wzh_quick.json")) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            JSONObject json = new JSONObject(new String(out.toByteArray(), "UTF-8"));
            String url = json.optString("remoteUrl", "");
            if (url.startsWith("http://") || url.startsWith(UrlObfuscator.decode(new int[] { 71, 58, 25, 252, 216, 240, 198, 39 }, 47))) remoteUrl = url;
            fastCache = "fast".equals(json.optString("cacheMode", "fresh"));
        } catch (Exception ignored) {
            // No config: behave like a plain local-files app.
        }
    }

    public static synchronized boolean isRemote() { return remoteUrl.length() > 0; }
    public static synchronized String remoteUrl() { return remoteUrl; }
    public static synchronized boolean fastCache() { return fastCache; }
}
