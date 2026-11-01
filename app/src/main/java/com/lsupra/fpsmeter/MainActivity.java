package com.lsupra.fpsmeter;

import android.app.Activity;
import android.content.*;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.webkit.*;
import android.widget.Toast;

public class MainActivity extends Activity {
    private SharedPreferences sp;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        sp = getSharedPreferences("s", MODE_PRIVATE);
        if (Build.VERSION.SDK_INT >= 33) requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 1);
        getWindow().setStatusBarColor(0xFF07090F);
        WebView wv = new WebView(this);
        wv.setBackgroundColor(0xFF07090F);
        WebSettings ws = wv.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        wv.addJavascriptInterface(new Bridge(), "Android");
        wv.loadUrl("file:///android_asset/index.html");
        setContentView(wv);
    }

    private void openPerm() {
        startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())));
    }

    private void push() {
        Intent i = new Intent(this, OverlayService.class);
        boolean any = false;
        for (int k = 0; k < 3; k++) {
            boolean on = sp.getBoolean(OverlayService.KEY[k], false);
            i.putExtra(OverlayService.N[k], on); any |= on;
        }
        if (any) { if (Build.VERSION.SDK_INT >= 26) startForegroundService(i); else startService(i); }
        else stopService(new Intent(this, OverlayService.class));
    }

    class Bridge {
        @JavascriptInterface public String getConfig() { return sp.getString("cfg", ""); }
        @JavascriptInterface public void setConfig(String j) { sp.edit().putString("cfg", j).apply(); }
        @JavascriptInterface public boolean hasPermission() { return Settings.canDrawOverlays(MainActivity.this); }
        @JavascriptInterface public void openPermission() { runOnUiThread(() -> openPerm()); }
        @JavascriptInterface public String getState() {
            StringBuilder s = new StringBuilder();
            for (String k : OverlayService.KEY) s.append(sp.getBoolean(k, false) ? "1" : "0");
            return s.toString();
        }
        @JavascriptInterface public boolean toggle(String w, boolean on) {
            if (on && !Settings.canDrawOverlays(MainActivity.this)) { openPermission(); return false; }
            for (int k = 0; k < 3; k++) if (OverlayService.N[k].equals(w)) sp.edit().putBoolean(OverlayService.KEY[k], on).apply();
            runOnUiThread(() -> push());
            return true;
        }
        @JavascriptInterface public void openGame() {
            runOnUiThread(() -> {
                Intent i = getPackageManager().getLaunchIntentForPackage("com.dts.freefireth");
                if (i == null) i = getPackageManager().getLaunchIntentForPackage("com.dts.freefiremax");
                if (i != null) startActivity(i);
                else Toast.makeText(MainActivity.this, "Không tìm thấy Free Fire", Toast.LENGTH_SHORT).show();
            });
        }
    }
}
