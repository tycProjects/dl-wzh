package com.lsupra.fpsmeter;

import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.database.ContentObserver;
import android.hardware.display.DisplayManager;
import android.os.*;
import android.provider.Settings;
import android.view.Display;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Khóa cứng tần số quét (Hz): ghi min = peak = user = Hz muốn giữ, rồi canh liên tục bằng
 * ContentObserver (cài đặt đổi), DisplayListener (màn hình đổi chế độ), bật/mở khóa màn hình và đồng hồ canh.
 * Hệ thống hay game hạ Hz xuống là bị ép lại ngay.
 */
public class HzService extends Service {
    static volatile boolean up = false;
    static volatile int fixes = 0;
    static volatile long lastFix = 0;

    private final Handler h = new Handler(Looper.getMainLooper());
    private final ExecutorService ex = Executors.newSingleThreadExecutor();
    private SharedPreferences sp;
    private long lastAt = 0;
    private DisplayManager dm;

    // ---------- tiện ích dùng chung (MainActivity cũng gọi) ----------
    static int cur(Context c) {
        try {
            DisplayManager m = (DisplayManager) c.getSystemService(Context.DISPLAY_SERVICE);
            return Math.round(m.getDisplay(Display.DEFAULT_DISPLAY).getRefreshRate());
        } catch (Throwable t) { return 0; }
    }

    static int[] supported(Context c) {
        try {
            DisplayManager m = (DisplayManager) c.getSystemService(Context.DISPLAY_SERVICE);
            Display.Mode[] ms = m.getDisplay(Display.DEFAULT_DISPLAY).getSupportedModes();
            ArrayList<Integer> l = new ArrayList<>();
            for (Display.Mode x : ms) { int r = Math.round(x.getRefreshRate()); if (!l.contains(r)) l.add(r); }
            Collections.sort(l);
            int[] o = new int[l.size()];
            for (int i = 0; i < o.length; i++) o[i] = l.get(i);
            return o;
        } catch (Throwable t) { return new int[0]; }
    }

    private static int rd(Context c, String k) {
        try { return Math.round(Settings.System.getFloat(c.getContentResolver(), k, 0f)); } catch (Throwable t) { return 0; }
    }

    /** Hz đã đúng ý muốn chưa. */
    static boolean held(Context c, int want) {
        int mn = rd(c, "min_refresh_rate"), pk = rd(c, "peak_refresh_rate");
        return cur(c) == want && (mn == 0 || mn == want) && (pk == 0 || pk == want);
    }

    /** Ép Hz: min = peak = user = hz. Dùng quyền Ghi cài đặt hệ thống, hoặc Shizuku/root. */
    static boolean apply(Context c, int hz) {
        boolean done = false;
        try {
            if (Settings.System.canWrite(c)) {
                ContentResolver cr = c.getContentResolver();
                Settings.System.putFloat(cr, "peak_refresh_rate", hz);
                Settings.System.putFloat(cr, "min_refresh_rate", hz);
                try { Settings.System.putFloat(cr, "user_refresh_rate", hz); } catch (Throwable ignored) {}
                done = true;
            }
        } catch (Throwable ignored) {}
        int mode = hz <= 60 ? 0 : 1;
        try {
            if (c.checkSelfPermission("android.permission.WRITE_SECURE_SETTINGS") == PackageManager.PERMISSION_GRANTED)
                Settings.Secure.putInt(c.getContentResolver(), "refresh_rate_mode", mode);
            else if (Priv.level() > 0) { Priv.exec("settings put secure refresh_rate_mode " + mode); done = true; }
        } catch (Throwable ignored) {}
        if (!done && Priv.level() > 0) {
            Priv.exec("settings put system peak_refresh_rate " + hz + ".0; settings put system min_refresh_rate " + hz
                    + ".0; settings put system user_refresh_rate " + hz + ".0");
            done = true;
        }
        return done;
    }

    /** Trả về tự động. */
    static boolean clear(Context c) {
        if (Priv.level() > 0) {
            Priv.exec("settings delete system peak_refresh_rate; settings delete system min_refresh_rate; "
                    + "settings delete system user_refresh_rate; settings put secure refresh_rate_mode 1");
            return true;
        }
        try {
            if (Settings.System.canWrite(c)) {
                int[] s = supported(c);
                ContentResolver cr = c.getContentResolver();
                Settings.System.putFloat(cr, "min_refresh_rate", 0f);
                Settings.System.putFloat(cr, "peak_refresh_rate", s.length > 0 ? s[s.length - 1] : 120f);
                return true;
            }
        } catch (Throwable ignored) {}
        return false;
    }

    static String info(Context c) {
        try {
            SharedPreferences s = c.getSharedPreferences("s", MODE_PRIVATE);
            JSONObject o = new JSONObject();
            o.put("cur", cur(c));
            JSONArray a = new JSONArray();
            for (int x : supported(c)) a.put(x);
            o.put("sup", a);
            o.put("want", s.getInt("hzVal", 0));
            o.put("lock", s.getBoolean("hzOn", false));
            o.put("hard", s.getBoolean("hzHard", true));
            o.put("iv", s.getInt("hzIv", 500));
            o.put("scr", s.getBoolean("hzScr", true));
            o.put("fixes", fixes);
            o.put("lastFix", lastFix == 0 ? 0 : (System.currentTimeMillis() - lastFix) / 1000);
            o.put("up", up);
            o.put("write", Settings.System.canWrite(c));
            o.put("priv", Priv.level());
            o.put("held", s.getInt("hzVal", 0) > 0 && held(c, s.getInt("hzVal", 0)));
            return o.toString();
        } catch (Exception e) { return "{}"; }
    }

    // ---------- dịch vụ ----------
    private boolean on() { return sp.getBoolean("hzOn", false); }
    private boolean hard() { return on() && sp.getBoolean("hzHard", true); }

    private void enforce(final boolean force) {
        ex.execute(new Runnable() {
            @Override public void run() {
                try {
                    if (!on()) return;
                    int want = sp.getInt("hzVal", 0);
                    if (want <= 0) return;
                    long now = System.currentTimeMillis();
                    if (!force && now - lastAt < 120) return;
                    if (force || !held(HzService.this, want)) {
                        lastAt = now;
                        if (apply(HzService.this, want)) { fixes++; lastFix = now; }
                    }
                } catch (Throwable ignored) {}
            }
        });
    }

    private final ContentObserver obs = new ContentObserver(new Handler(Looper.getMainLooper())) {
        @Override public void onChange(boolean self) { if (hard()) enforce(false); }
    };

    private final DisplayManager.DisplayListener dl = new DisplayManager.DisplayListener() {
        @Override public void onDisplayAdded(int id) {}
        @Override public void onDisplayRemoved(int id) {}
        @Override public void onDisplayChanged(int id) { if (hard()) enforce(false); }
    };

    private final BroadcastReceiver scr = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) {
            if (!sp.getBoolean("hzScr", true)) return;
            enforce(true);
            h.postDelayed(() -> enforce(true), 1500);
        }
    };

    private final Runnable watch = new Runnable() {
        @Override public void run() {
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            boolean awake = pm == null || pm.isInteractive();
            if (on() && awake) enforce(false);
            int iv = !awake ? 5000 : hard() ? Math.max(150, Math.min(5000, sp.getInt("hzIv", 500))) : 3000;
            h.postDelayed(this, iv);
        }
    };

    @Override public IBinder onBind(Intent i) { return null; }

    @Override public void onCreate() {
        super.onCreate();
        up = true;
        sp = getSharedPreferences("s", MODE_PRIVATE);
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26)
            nm.createNotificationChannel(new NotificationChannel("hz", "Khóa Hz", NotificationManager.IMPORTANCE_LOW));
        Notification.Builder nb = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(this, "hz") : new Notification.Builder(this);
        PendingIntent pi = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class),
                Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0);
        Notification n = nb.setContentTitle("LONG SUPRA // MOD LAB").setContentText("Đang khóa cứng " + sp.getInt("hzVal", 0) + " Hz")
                .setSmallIcon(android.R.drawable.ic_menu_info_details).setContentIntent(pi).setOngoing(true).build();
        if (Build.VERSION.SDK_INT >= 34) startForeground(2, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        else startForeground(2, n);
        try {
            ContentResolver cr = getContentResolver();
            for (String k : new String[]{"peak_refresh_rate", "min_refresh_rate", "user_refresh_rate"})
                cr.registerContentObserver(Settings.System.getUriFor(k), false, obs);
            cr.registerContentObserver(Settings.Secure.getUriFor("refresh_rate_mode"), false, obs);
        } catch (Throwable ignored) {}
        try {
            dm = (DisplayManager) getSystemService(DISPLAY_SERVICE);
            dm.registerDisplayListener(dl, h);
        } catch (Throwable ignored) {}
        try {
            IntentFilter f = new IntentFilter();
            f.addAction(Intent.ACTION_SCREEN_ON);
            f.addAction(Intent.ACTION_USER_PRESENT);
            registerReceiver(scr, f);
        } catch (Throwable ignored) {}
        h.post(watch);
        enforce(true);
    }

    @Override public int onStartCommand(Intent in, int f, int id) {
        if (!on()) { stopSelf(); return START_NOT_STICKY; }
        enforce(true);
        return START_STICKY;
    }

    @Override public void onDestroy() {
        up = false;
        h.removeCallbacksAndMessages(null);
        try { getContentResolver().unregisterContentObserver(obs); } catch (Throwable ignored) {}
        try { if (dm != null) dm.unregisterDisplayListener(dl); } catch (Throwable ignored) {}
        try { unregisterReceiver(scr); } catch (Throwable ignored) {}
        ex.shutdownNow();
        super.onDestroy();
    }
}
