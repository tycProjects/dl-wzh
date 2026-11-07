package com.pb.browser;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Process;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.webkit.*;
import android.widget.*;
import androidx.webkit.*;
import org.json.JSONObject;
import java.io.InputStream;
import java.util.Collections;

public class BrowserActivity extends Activity {
    WebView web;
    EditText bar;
    String proxyUser, proxyPass, script;
    int authTries = 0;
    boolean docStartOk = false;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        JSONObject p;
        try { p = new JSONObject(getIntent().getStringExtra("profile")); }
        catch (Exception e) { finish(); return; }

        // Moi UUID = thu muc du lieu WebView rieng (phai goi truoc khi tao WebView)
        try { WebView.setDataDirectorySuffix(p.optString("uuid", "default")); } catch (Exception ignored) {}

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        LinearLayout top = new LinearLayout(this);
        bar = new EditText(this);
        bar.setSingleLine(true);
        bar.setImeOptions(EditorInfo.IME_ACTION_GO);
        bar.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_URI);
        Button go = new Button(this); go.setText("Go");
        top.addView(bar, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        top.addView(go);
        web = new WebView(this);
        root.addView(top);
        root.addView(web, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
        root.setFitsSystemWindows(true);

        String ua = p.optString("ua");
        String lang = p.optString("lang", "en-US");
        String uuid = p.optString("uuid");
        String rtc = p.optString("rtc", "off");

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        s.setGeolocationEnabled(false);
        s.setSupportMultipleWindows(false);
        if (!ua.isEmpty()) s.setUserAgentString(ua);
        boolean mobile = ua.contains("Mobile") || ua.contains("Android");
        if (!mobile) { s.setUseWideViewPort(true); s.setLoadWithOverviewMode(true); }

        // Script gia lap + chan WebRTC
        try {
            JSONObject cfg = new JSONObject();
            int seed = uuid.hashCode();
            int[] cores = {4, 6, 8, 12};
            cfg.put("ua", ua);
            cfg.put("platform", platformOf(ua));
            cfg.put("lang", lang);
            cfg.put("cores", cores[(seed >>> 3) & 3]);
            cfg.put("mem", (seed & 1) == 0 ? 4 : 8);
            cfg.put("seed", seed & 0x7fffffff);
            cfg.put("touch", mobile ? 5 : 0);
            cfg.put("rtc", rtc);
            script = readAsset("inject.js").replace("__CFG__", cfg.toString());
        } catch (Exception e) { toast("Loi tao script: " + e); finish(); return; }

        if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
            WebViewCompat.addDocumentStartJavaScript(web, script, Collections.singleton("*"));
            docStartOk = true;
        }

        web.setWebChromeClient(new WebChromeClient() {
            @Override public void onPermissionRequest(PermissionRequest r) { r.deny(); }
            @Override public void onGeolocationPermissionsShowPrompt(String o, GeolocationPermissions.Callback c) { c.invoke(o, false, false); }
        });
        web.setWebViewClient(new WebViewClient() {
            @Override public void onPageStarted(WebView v, String u, Bitmap f) {
                if (!docStartOk) v.evaluateJavascript(script, null);
                bar.setText(u);
            }
            @Override public void onReceivedHttpAuthRequest(WebView v, HttpAuthHandler h, String host, String realm) {
                if (proxyUser != null && authTries++ < 3) h.proceed(proxyUser, proxyPass);
                else h.cancel();
            }
        });

        go.setOnClickListener(v -> load(bar.getText().toString()));
        bar.setOnEditorActionListener((v, a, e) -> { load(bar.getText().toString()); return true; });

        final String start = p.optString("url").isEmpty() ? "https://browserleaks.com/webrtc" : p.optString("url");
        String proxyStr = p.optString("proxy");
        if (proxyStr.isEmpty()) {
            toast("Canh bao: dang KHONG dung proxy");
            load(start);
        } else {
            applyProxy(proxyStr, () -> load(start));
        }
    }

    // Chi load trang SAU KHI proxy da ap dung -> khong lo IP that
    void applyProxy(String raw, Runnable then) {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
            toast("WebView nay khong ho tro proxy. Hay cap nhat Android System WebView / Chrome.");
            return;
        }
        String scheme = "http", rest = raw.trim();
        int i = rest.indexOf("://");
        if (i > 0) { scheme = rest.substring(0, i).toLowerCase(); rest = rest.substring(i + 3); }
        String hostPort = rest;
        if (rest.contains("@")) {
            int at = rest.lastIndexOf('@');
            String cred = rest.substring(0, at); hostPort = rest.substring(at + 1);
            int c = cred.indexOf(':');
            proxyUser = c < 0 ? cred : cred.substring(0, c);
            proxyPass = c < 0 ? "" : cred.substring(c + 1);
        } else {
            String[] a = rest.split(":");
            if (a.length == 4) { hostPort = a[0] + ":" + a[1]; proxyUser = a[2]; proxyPass = a[3]; }
        }
        try {
            ProxyConfig cfg = new ProxyConfig.Builder().addProxyRule(scheme + "://" + hostPort).build();
            ProxyController.getInstance().setProxyOverride(cfg, getMainExecutor(), then);
        } catch (Exception e) {
            toast("Proxy loi: " + e.getMessage());
        }
    }

    void load(String u) {
        u = u.trim();
        if (u.isEmpty()) return;
        if (!u.matches("^[a-zA-Z][a-zA-Z0-9+.-]*://.*")) u = "https://" + u;
        web.loadUrl(u);
    }

    static String platformOf(String ua) {
        if (ua.contains("iPhone")) return "iPhone";
        if (ua.contains("Android")) return "Linux armv8l";
        if (ua.contains("Windows")) return "Win32";
        if (ua.contains("Macintosh")) return "MacIntel";
        return "Linux x86_64";
    }

    String readAsset(String n) throws Exception {
        try (InputStream in = getAssets().open(n)) {
            java.io.ByteArrayOutputStream o = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[4096]; int r;
            while ((r = in.read(buf)) > 0) o.write(buf, 0, r);
            return o.toString("UTF-8");
        }
    }

    void toast(String m) { Toast.makeText(this, m, Toast.LENGTH_LONG).show(); }

    @Override public boolean onKeyDown(int k, KeyEvent e) {
        if (k == KeyEvent.KEYCODE_BACK && web != null && web.canGoBack()) { web.goBack(); return true; }
        return super.onKeyDown(k, e);
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        // Giet process de lan mo sau dung UUID/proxy moi hoan toan
        if (isFinishing()) Process.killProcess(Process.myPid());
    }
}
