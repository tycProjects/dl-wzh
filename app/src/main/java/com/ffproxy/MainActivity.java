package com.ffproxy;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.net.VpnService;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.ffproxy.engine.FeatureConfig;
import com.ffproxy.engine.FeatureEngine;
import com.ffproxy.util.RootUtil;
import com.ffproxy.vpn.ProxyVpnService;

public class MainActivity extends Activity {

    private WebView webView;
    private static final int REQ_VPN = 200;
    private static final int REQ_OVERLAY = 201;
    private boolean waitingOverlay = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        initWebView();
    }

    private void initWebView() {
        webView = new WebView(this);
        setContentView(webView);
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        if (Build.VERSION.SDK_INT >= 21) {
            s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }
        webView.addJavascriptInterface(new JsBridge(), "Android");
        webView.setWebViewClient(new WebViewClient());
        webView.loadUrl("file:///android_asset/index.html");
    }

    private boolean hasStorage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        }
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasOverlay() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(this);
        }
        return true;
    }

    private void requestStorage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Intent i = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                i.setData(Uri.parse("package:" + getPackageName()));
                startActivity(i);
            } catch (Exception e) {
                startActivity(new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION));
            }
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE},
                    202);
        }
    }

    private void requestOverlay() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            waitingOverlay = true;
            Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(i, REQ_OVERLAY);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (waitingOverlay) waitingOverlay = false;
        if (webView != null) {
            webView.post(() -> webView.evaluateJavascript(
                    "if(window.App)App.refreshPerms()", null));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQ_VPN && resultCode == RESULT_OK) {
            startService(new Intent(this, ProxyVpnService.class));
            toast("VPN Proxy ON");
            webView.post(() -> webView.evaluateJavascript(
                    "if(window.App)App.showMenu()", null));
        }
    }

    private void toast(String m) {
        Toast.makeText(this, m, Toast.LENGTH_SHORT).show();
    }

    public class JsBridge {
        @JavascriptInterface
        public boolean isRoot() { return RootUtil.isRoot(); }

        @JavascriptInterface
        public boolean hasOverlay() { return MainActivity.this.hasOverlay(); }

        @JavascriptInterface
        public boolean hasStorage() { return MainActivity.this.hasStorage(); }

        @JavascriptInterface
        public void startProxy() {
            if (!RootUtil.isRoot()) {
                runOnUiThread(() -> new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Cần ROOT")
                        .setMessage("Bật Magisk / root trước.")
                        .setPositiveButton("OK", null).show());
                return;
            }
            if (!hasStorage()) {
                requestStorage();
                toast("Cấp quyền Storage");
                return;
            }
            if (!hasOverlay()) {
                requestOverlay();
                toast("Cấp quyền Overlay");
                return;
            }
            Intent prep = VpnService.prepare(MainActivity.this);
            if (prep != null) {
                startActivityForResult(prep, REQ_VPN);
            } else {
                startService(new Intent(MainActivity.this, ProxyVpnService.class));
                runOnUiThread(() -> webView.evaluateJavascript(
                        "if(window.App)App.showMenu()", null));
            }
        }

        @JavascriptInterface
        public void stopProxy() {
            stopService(new Intent(MainActivity.this, ProxyVpnService.class));
            toast("Proxy OFF");
        }

        @JavascriptInterface
        public void setFeature(String key, boolean value) {
            FeatureConfig.set(key, value);
            // Auto apply file replace immediately when toggled ON
            if (value) {
                String result = FeatureEngine.autoReplace(key);
                final String msg = result;
                runOnUiThread(() -> {
                    toast(msg);
                    webView.evaluateJavascript(
                            "if(window.App)App.setMsg(" + JSONQuote(msg) + ")", null);
                });
            } else {
                FeatureEngine.restore(key);
                runOnUiThread(() -> toast("Đã khôi phục: " + key));
            }
        }

        private String JSONQuote(String s) {
            return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"")
                    .replace("\n", "\\n") + "\"";
        }

        @JavascriptInterface
        public void apply() {
            String r = FeatureEngine.applyAll();
            final String msg = r;
            runOnUiThread(() -> {
                toast(msg);
                webView.evaluateJavascript(
                        "if(window.App)App.setMsg(" + JSONQuote(msg) + ")", null);
            });
        }

        @JavascriptInterface
        public void reset() {
            FeatureConfig.reset();
            FeatureEngine.restoreAll();
            toast("Reset + restore files");
        }

        @JavascriptInterface
        public String scanGame() {
            return FeatureEngine.scanGamePaths();
        }

        @JavascriptInterface
        public void toast(String m) {
            runOnUiThread(() -> Toast.makeText(MainActivity.this, m, Toast.LENGTH_SHORT).show());
        }
    }
}
