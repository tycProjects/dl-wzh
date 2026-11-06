package com.blackmagic.gamerspace;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends Activity {
    private WebView webView;
    private static final int OVERLAY_REQUEST = 701;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(9,9,13));
        getWindow().setNavigationBarColor(Color.rgb(9,9,13));
        webView = new WebView(this);
        webView.setBackgroundColor(Color.rgb(9,9,13));
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient());
        webView.addJavascriptInterface(new AndroidBridge(), "BMAndroid");
        setContentView(webView);
        boolean openPanel = getIntent() != null && getIntent().getBooleanExtra("open_panel", false);
        // Launch from icon: play the bundled MP4 intro first. Floating shortcut: go straight to the game panel.
        webView.loadUrl(openPanel ? "file:///android_asset/index.html" : "file:///android_asset/splash.html");
        if (openPanel) {
            webView.postDelayed(() -> webView.evaluateJavascript("showGames()", null), 700);
        }
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent); setIntent(intent);
        if (intent.getBooleanExtra("open_panel", false) && webView != null)
            webView.postDelayed(() -> webView.evaluateJavascript("showGames()", null), 400);
    }

    private class AndroidBridge {
        @JavascriptInterface public String getApps() {
            JSONArray out = new JSONArray();
            PackageManager pm = getPackageManager();
            Intent launcher = new Intent(Intent.ACTION_MAIN, null);
            launcher.addCategory(Intent.CATEGORY_LAUNCHER);
            List<android.content.pm.ResolveInfo> apps = pm.queryIntentActivities(launcher, 0);
            Collections.sort(apps, Comparator.comparing(a -> String.valueOf(a.loadLabel(pm)).toLowerCase()));
            for (android.content.pm.ResolveInfo ri : apps) {
                try {
                    String pkg = ri.activityInfo.packageName;
                    if (pkg.equals(getPackageName())) continue;
                    JSONObject item = new JSONObject();
                    item.put("name", String.valueOf(ri.loadLabel(pm)));
                    item.put("packageName", pkg);
                    ApplicationInfo ai = pm.getApplicationInfo(pkg, 0);
                    item.put("isGame", ai.category == ApplicationInfo.CATEGORY_GAME);
                    out.put(item);
                } catch (Exception ignored) {}
            }
            return out.toString();
        }
        @JavascriptInterface public boolean launchApp(String packageName) {
            try {
                Intent i = getPackageManager().getLaunchIntentForPackage(packageName);
                if (i == null) return false;
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); startActivity(i); return true;
            } catch (Exception e) { return false; }
        }
        @JavascriptInterface public void requestOverlay() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(MainActivity.this)) {
                Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
                startActivityForResult(i, OVERLAY_REQUEST);
            } else startFloating();
        }
        @JavascriptInterface public boolean hasOverlayPermission() {
            return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(MainActivity.this);
        }
        @JavascriptInterface public void stopFloating() { stopService(new Intent(MainActivity.this, FloatingButtonService.class)); }
        @JavascriptInterface public void startBackground() {
            try { startService(new Intent(MainActivity.this, BackgroundMonitorService.class)); } catch (Exception ignored) {}
        }
        @JavascriptInterface public void stopBackground() { stopService(new Intent(MainActivity.this, BackgroundMonitorService.class)); }
        @JavascriptInterface public void openBatterySettings() {
            try {
                Intent i = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                i.setData(Uri.parse("package:" + getPackageName())); startActivity(i);
            } catch (Exception e) {
                startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
            }
        }
        @JavascriptInterface public String getStats() {
            JSONObject o = new JSONObject();
            try {
                ActivityManager am = (ActivityManager)getSystemService(ACTIVITY_SERVICE);
                ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo(); am.getMemoryInfo(mi);
                o.put("ramAvailableMb", mi.availMem / (1024*1024));
                o.put("ramTotalMb", mi.totalMem / (1024*1024));
                Intent battery = registerReceiver(null, new android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED));
                if (battery != null) { int level = battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1); int scale = battery.getIntExtra(BatteryManager.EXTRA_SCALE, 100); o.put("batteryPercent", scale > 0 ? Math.round(level * 100f / scale) : -1); o.put("batteryTempC", battery.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) / 10f); }
            } catch (Exception ignored) {}
            return o.toString();
        }
        @JavascriptInterface public void openAppSettings() {
            Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName())); startActivity(i);
        }
    }

    private void startFloating() { startService(new Intent(this, FloatingButtonService.class)); }
    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == OVERLAY_REQUEST && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) startFloating();
    }

}
