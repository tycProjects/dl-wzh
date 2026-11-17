package com.lsupra.fpsmeter;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.*;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.webkit.*;
import android.media.projection.MediaProjectionManager;
import android.widget.TextView;
import android.widget.Toast;
import java.io.*;
import org.json.JSONObject;

public class MainActivity extends Activity {
    private SharedPreferences sp;
    private WebView wv;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        sp = getSharedPreferences("s", MODE_PRIVATE);
        if (Build.VERSION.SDK_INT >= 33) requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 1);
        getWindow().setStatusBarColor(0xFF07090F);
        wv = new WebView(this);
        wv.setBackgroundColor(0xFF07090F);
        WebSettings ws = wv.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        wv.addJavascriptInterface(new Bridge(), "Android");
        wv.loadUrl("file:///android_asset/index.html");
        setContentView(wv);
        if (getIntent().getBooleanExtra("startRec", false)) startRec();
    }

    @Override protected void onNewIntent(Intent i) {
        super.onNewIntent(i);
        setIntent(i);
        if (i.getBooleanExtra("startRec", false)) startRec();
    }

    // ---------- quay màn hình ----------
    private void startRec() {
        try {
            JSONObject c = new JSONObject(sp.getString("cfg", "{}")).optJSONObject("rec");
            String au = c == null ? "sys" : c.optString("audio", "sys");
            if (!au.equals("none") && Build.VERSION.SDK_INT >= 23 && checkSelfPermission("android.permission.RECORD_AUDIO") != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{"android.permission.RECORD_AUDIO"}, 2);
                return;
            }
        } catch (Exception ignored) {}
        launchCapture();
    }

    private void launchCapture() {
        MediaProjectionManager m = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        startActivityForResult(m.createScreenCaptureIntent(), 1001);
    }

    @Override public void onRequestPermissionsResult(int code, String[] p, int[] r) {
        super.onRequestPermissionsResult(code, p, r);
        if (code == 2) launchCapture();      // từ chối mic thì vẫn quay hình, âm thanh sẽ báo lỗi nhẹ
    }

    @Override protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req != 1001) return;
        if (res == RESULT_OK && data != null) {
            Intent s = new Intent(this, RecService.class).putExtra("rc", res).putExtra("data", data);
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(s); else startService(s);
            try {
                JSONObject c = new JSONObject(sp.getString("cfg", "{}")).optJSONObject("rec");
                if (c == null || c.optInt("hide", 1) == 1) moveTaskToBack(true);
            } catch (Exception e) { moveTaskToBack(true); }
        } else Toast.makeText(this, "Đã hủy quay màn hình", Toast.LENGTH_SHORT).show();
    }


    private void openPerm() {
        startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())));
    }

    private void push() {
        Intent i = new Intent(this, OverlayService.class);
        boolean any = false;
        for (int k = 0; k < OverlayService.N.length; k++) {
            boolean on = sp.getBoolean(OverlayService.KEY[k], false);
            i.putExtra(OverlayService.N[k], on); any |= on;
        }
        if (any) { if (Build.VERSION.SDK_INT >= 26) startForegroundService(i); else startService(i); }
        else stopService(new Intent(this, OverlayService.class));
    }

    class Bridge {
        @JavascriptInterface public String getConfig() { return sp.getString("cfg", ""); }
        @JavascriptInterface public void setConfig(String j) {
            // Gộp an toàn: nếu chữ nổi vừa được sửa trên màn hình (mới hơn bản web đang giữ) thì giữ bản đó
            try {
                JSONObject in = new JSONObject(j);
                JSONObject old = new JSONObject(sp.getString("cfg", "{}"));
                JSONObject ot = old.optJSONObject("text"), it = in.optJSONObject("text");
                if (ot != null && it != null && ot.optLong("_t", 0) > it.optLong("_t", 0)) in.put("text", ot);
                sp.edit().putString("cfg", in.toString()).apply();
            } catch (Exception e) { sp.edit().putString("cfg", j).apply(); }
        }
        @JavascriptInterface public void copyText(String t) {
            runOnUiThread(() -> {
                try {
                    ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                    cm.setPrimaryClip(ClipData.newPlainText("LONG SUPRA", t));
                } catch (Exception ignored) {}
            });
        }
        @JavascriptInterface public boolean hasPermission() { return Settings.canDrawOverlays(MainActivity.this); }
        @JavascriptInterface public void openPermission() { runOnUiThread(() -> openPerm()); }
        @JavascriptInterface public String getState() {
            StringBuilder s = new StringBuilder();
            for (String k : OverlayService.KEY) s.append(sp.getBoolean(k, false) ? "1" : "0");
            return s.toString();
        }
        @JavascriptInterface public boolean toggle(String w, boolean on) {
            if (on && !Settings.canDrawOverlays(MainActivity.this)) { openPermission(); return false; }
            for (int k = 0; k < OverlayService.N.length; k++) if (OverlayService.N[k].equals(w)) sp.edit().putBoolean(OverlayService.KEY[k], on).apply();
            runOnUiThread(() -> push());
            return true;
        }
        /** "thật,giả,pin" — số liệu đang hiển thị trên ô nổi (nếu dịch vụ chưa chạy thì thật = tần số quét). */
        @JavascriptInterface public String live() {
            float r, f = -1;
            if (OverlayService.up) { r = OverlayService.liveR; f = OverlayService.liveF; }
            else { try { r = getWindowManager().getDefaultDisplay().getRefreshRate(); } catch (Exception e) { r = 60; } }
            String[] sy = OverlayService.sys(MainActivity.this);
            return String.format(java.util.Locale.US, "%.1f,%.1f,%s,%s,%s", r, f, sy[0], sy[1], sy[2]);
        }
        @JavascriptInterface public void recStart() { runOnUiThread(() -> startRec()); }
        @JavascriptInterface public void recStop() { runOnUiThread(() -> startService(new Intent(MainActivity.this, RecService.class).setAction("stop"))); }
        @JavascriptInterface public String recState() {
            long ms = RecService.phase == 2 ? SystemClock.elapsedRealtime() - RecService.startedMs : 0;
            return "{\"phase\":" + RecService.phase + ",\"cd\":" + RecService.cdLeft + ",\"ms\":" + ms + ",\"mb\":" + (RecService.bytes / 1048576)
                    + ",\"info\":" + JSONObject.quote(RecService.info) + ",\"file\":" + JSONObject.quote(RecService.lastFile) + "}";
        }
        @JavascriptInterface public String recCaps() { return RecService.caps(MainActivity.this); }
        @JavascriptInterface public String stats() {
            Intent b = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            int lvl = b.getIntExtra("level", 0), sc = Math.max(1, b.getIntExtra("scale", 100));
            float t = b.getIntExtra("temperature", 0) / 10f;
            ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
            ((ActivityManager) getSystemService(ACTIVITY_SERVICE)).getMemoryInfo(mi);
            int ram = (int) (100 - mi.availMem * 100 / Math.max(1, mi.totalMem));
            return "{\"bat\":" + (lvl * 100 / sc) + ",\"temp\":" + String.format(java.util.Locale.US, "%.1f", t) + ",\"ram\":" + ram + "}";
        }
        @JavascriptInterface public String games() {
            String[][] g = {{"Free Fire", "com.dts.freefireth"}, {"Free Fire MAX", "com.dts.freefiremax"},
                {"PUBG Mobile", "com.tencent.ig"}, {"PUBG Mobile VN", "com.vng.pubgmobile"}, {"Liên Quân Mobile", "com.garena.game.kgvn"},
                {"Call of Duty Mobile", "com.activision.callofduty.shooter"}, {"Mobile Legends", "com.mobile.legends"},
                {"Genshin Impact", "com.miHoYo.GenshinImpact"}, {"Minecraft", "com.mojang.minecraftpe"}, {"Roblox", "com.roblox.client"},
                {"Brawl Stars", "com.supercell.brawlstars"}, {"Clash of Clans", "com.supercell.clashofclans"}};
            StringBuilder s = new StringBuilder("[");
            for (String[] x : g) {
                try {
                    getPackageManager().getPackageInfo(x[1], 0);
                    if (s.length() > 1) s.append(",");
                    s.append("{\"n\":\"").append(x[0]).append("\",\"p\":\"").append(x[1]).append("\"}");
                } catch (PackageManager.NameNotFoundException ignored) {}
            }
            return s.append("]").toString();
        }
        @JavascriptInterface public String hzInfo() { return HzService.info(MainActivity.this); }
        @JavascriptInterface public boolean hzSet(int hz) {
            sp.edit().putInt("hzVal", hz).apply();
            new Thread(() -> { HzService.apply(MainActivity.this, hz); }).start();
            return Settings.System.canWrite(MainActivity.this) || Priv.level() > 0;
        }
        @JavascriptInterface public boolean hzAuto() {
            sp.edit().putBoolean("hzOn", false).apply();
            runOnUiThread(() -> stopService(new Intent(MainActivity.this, HzService.class)));
            new Thread(() -> { HzService.clear(MainActivity.this); }).start();
            return Settings.System.canWrite(MainActivity.this) || Priv.level() > 0;
        }
        @JavascriptInterface public void hzLock(boolean on, boolean hard, int iv, boolean scr) {
            sp.edit().putBoolean("hzOn", on).putBoolean("hzHard", hard).putInt("hzIv", iv).putBoolean("hzScr", scr).apply();
            runOnUiThread(() -> {
                Intent i = new Intent(MainActivity.this, HzService.class);
                if (on) { if (Build.VERSION.SDK_INT >= 26) startForegroundService(i); else startService(i); }
                else stopService(i);
            });
        }
        @JavascriptInterface public void openWrite() {
            runOnUiThread(() -> startActivity(new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:" + getPackageName()))));
        }
        @JavascriptInterface public String hash(String s) {
            try {
                byte[] d = java.security.MessageDigest.getInstance("SHA-256").digest(("lsupra|" + s).getBytes("UTF-8"));
                StringBuilder sb = new StringBuilder();
                for (byte x : d) sb.append(String.format("%02x", x));
                return sb.toString();
            } catch (Exception e) { return ""; }
        }
        @JavascriptInterface public String getAcc() { return sp.getString("acc", ""); }
        @JavascriptInterface public void setAcc(String j) { sp.edit().putString("acc", j).apply(); }
        @JavascriptInterface public void openUrl(String u) {
            if (u == null || !u.startsWith("https://")) return;
            runOnUiThread(() -> { try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(u)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); } catch (Exception e) { } });
        }
        @JavascriptInterface public void shAuto() {
            new Thread(() -> {
                final String r = Priv.autoGrant(MainActivity.this);
                runOnUiThread(() -> wv.evaluateJavascript("window.privDone&&window.privDone(" + JSONObject.quote(r) + ")", null));
            }).start();
        }
        @JavascriptInterface public String privState() { return Priv.state(MainActivity.this); }
        @JavascriptInterface public void privCheck() {
            new Thread(() -> {
                Priv.detectRoot();
                final String r = Priv.rootGranted ? "✔ Đã có quyền root" : "✖ Không có root (hoặc bạn đã từ chối)";
                runOnUiThread(() -> wv.evaluateJavascript("window.privDone&&window.privDone(" + JSONObject.quote(r) + ")", null));
            }).start();
        }
        @JavascriptInterface public void shizukuAction() {
            runOnUiThread(() -> {
                if (!Priv.shInstalled(MainActivity.this)) startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://shizuku.rikka.app/download/")));
                else if (!Priv.shRunning()) {
                    Intent i = getPackageManager().getLaunchIntentForPackage("moe.shizuku.privileged.api");
                    if (i != null) startActivity(i);
                } else Priv.shRequest();
            });
        }
    }
}
