package com.pb.browser;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Environment;
import android.os.PerformanceHintManager;
import android.os.PowerManager;
import android.os.Process;
import android.os.StatFs;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;
import java.io.BufferedReader;
import java.io.FileReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Locale;

// Doc thong so may THAT + do ping THAT + goi y hieu nang (PerformanceHintManager).
// Khong ep xung ao: app thuong khong the doi CPU freq (can root).
final class SysInfo {

    // ---------- CPU ----------
    static int cores() { return Runtime.getRuntime().availableProcessors(); }

    static String cpuName() {
        if (Build.VERSION.SDK_INT >= 31) {
            String m = Build.SOC_MANUFACTURER, mo = Build.SOC_MODEL;
            if (m != null && mo != null && !m.trim().isEmpty() && !mo.trim().isEmpty()
                    && !"unknown".equalsIgnoreCase(m.trim())) return cap(m) + " " + mo;
        }
        String info = readCpuInfo();
        if (info != null) return info;
        return Build.HARDWARE != null && !Build.HARDWARE.isEmpty() ? Build.HARDWARE : "Không rõ";
    }
    static String cap(String s) {
        s = s.trim();
        return s.isEmpty() ? s : s.substring(0, 1).toUpperCase(Locale.US) + s.substring(1);
    }
    static String readCpuInfo() {
        try (BufferedReader br = new BufferedReader(new FileReader("/proc/cpuinfo"))) {
            String line, hardware = null, model = null;
            while ((line = br.readLine()) != null) {
                String l = line.toLowerCase(Locale.US);
                if (l.startsWith("model name")) model = afterColon(line);
                else if (l.startsWith("hardware")) hardware = afterColon(line);
            }
            if (model != null && !model.isEmpty()) return model;
            if (hardware != null && !hardware.isEmpty()) return hardware;
        } catch (Exception ignored) {}
        return null;
    }
    static String afterColon(String line) {
        int i = line.indexOf(':');
        return i >= 0 ? line.substring(i + 1).trim() : null;
    }
    // Tan so hien tai (chi doc, nhieu may chan quyen -> null)
    static String cpuFreq() {
        String[] paths = {
                "/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq",
                "/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq"};
        for (String p : paths) {
            try (BufferedReader br = new BufferedReader(new FileReader(p))) {
                String s = br.readLine();
                if (s == null) continue;
                long khz = Long.parseLong(s.trim());
                if (khz <= 0) continue;
                double mhz = khz / 1000.0;
                return mhz >= 1000 ? String.format(Locale.US, "%.2f GHz", mhz / 1000.0)
                        : String.format(Locale.US, "%.0f MHz", mhz);
            } catch (Exception ignored) {}
        }
        return null;
    }
    static String governor() {
        try (BufferedReader br = new BufferedReader(new FileReader("/sys/devices/system/cpu/cpu0/cpufreq/scaling_governor"))) {
            String s = br.readLine();
            return s == null ? null : s.trim();
        } catch (Exception e) { return null; }
    }

    // ---------- RAM ----------
    static long[] ram(Context c) {
        ActivityManager am = (ActivityManager) c.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(mi);
        return new long[]{mi.totalMem, mi.availMem};
    }
    static String gb(long bytes) { return String.format(Locale.US, "%.1f GB", bytes / 1073741824.0); }
    static String mb(long bytes) { return String.format(Locale.US, "%.0f MB", bytes / 1048576.0); }

    // ---------- Man hinh / tan so quet ----------
    static Display display(Activity a) { return a.getWindowManager().getDefaultDisplay(); }
    static float refreshRate(Activity a) {
        Display d = display(a);
        return d == null ? 60f : d.getRefreshRate();
    }
    static Display.Mode bestMode(Activity a) {
        Display d = display(a);
        if (d == null) return null;
        Display.Mode cur = d.getMode();
        Display.Mode best = null;
        for (Display.Mode m : d.getSupportedModes()) {
            if (m.getPhysicalWidth() == cur.getPhysicalWidth()
                    && m.getPhysicalHeight() == cur.getPhysicalHeight()) {
                if (best == null || m.getRefreshRate() > best.getRefreshRate()) best = m;
            }
        }
        return best;
    }
    static float maxRefreshRate(Activity a) {
        Display.Mode b = bestMode(a);
        return b != null ? b.getRefreshRate() : refreshRate(a);
    }
    // Yêu cầu tần số quét cao nhất (TĂNG FPS thật). high=false -> trả về mặc định hệ thống.
    static void applyRefresh(Activity a, boolean high) {
        try {
            WindowManager.LayoutParams lp = a.getWindow().getAttributes();
            if (high) {
                Display.Mode b = bestMode(a);
                if (b != null) {
                    lp.preferredDisplayModeId = b.getModeId();
                    lp.preferredRefreshRate = b.getRefreshRate();
                } else {
                    lp.preferredRefreshRate = maxRefreshRate(a);
                }
            } else {
                lp.preferredDisplayModeId = 0;
                lp.preferredRefreshRate = 0f;
            }
            a.getWindow().setAttributes(lp);
        } catch (Exception ignored) {}
    }

    // ---------- Pin / nhiet ----------
    static int batteryLevel(Context c) {
        try {
            BatteryManager bm = (BatteryManager) c.getSystemService(Context.BATTERY_SERVICE);
            return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        } catch (Exception e) { return -1; }
    }
    static Intent batteryIntent(Context c) {
        try {
            IntentFilter f = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            if (Build.VERSION.SDK_INT >= 33) return c.registerReceiver(null, f, Context.RECEIVER_NOT_EXPORTED);
            return c.registerReceiver(null, f);
        } catch (Exception e) { return null; }
    }
    static String batteryStatus(Context c) {
        Intent it = batteryIntent(c);
        if (it == null) return "—";
        int st = it.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        String s = st == BatteryManager.BATTERY_STATUS_CHARGING ? "Đang sạc"
                : st == BatteryManager.BATTERY_STATUS_FULL ? "Đầy"
                : st == BatteryManager.BATTERY_STATUS_DISCHARGING ? "Dùng pin" : "—";
        int temp = it.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1);
        if (temp > 0) s += String.format(Locale.US, " · %.1f°C", temp / 10.0);
        return s;
    }
    static String thermal(Context c) {
        if (Build.VERSION.SDK_INT < 29) return "—";
        try {
            PowerManager pm = (PowerManager) c.getSystemService(Context.POWER_SERVICE);
            switch (pm.getCurrentThermalStatus()) {
                case PowerManager.THERMAL_STATUS_NONE: return "Mát";
                case PowerManager.THERMAL_STATUS_LIGHT: return "Ấm nhẹ";
                case PowerManager.THERMAL_STATUS_MODERATE: return "Ấm";
                case PowerManager.THERMAL_STATUS_SEVERE: return "Nóng";
                case PowerManager.THERMAL_STATUS_CRITICAL: return "Rất nóng";
                case PowerManager.THERMAL_STATUS_EMERGENCY: return "Nguy cấp";
                case PowerManager.THERMAL_STATUS_SHUTDOWN: return "Quá nhiệt";
                default: return "—";
            }
        } catch (Exception e) { return "—"; }
    }

    // ---------- Bo nho ----------
    static long[] storage() {
        try {
            StatFs st = new StatFs(Environment.getDataDirectory().getAbsolutePath());
            return new long[]{st.getTotalBytes(), st.getAvailableBytes()};
        } catch (Exception e) { return new long[]{0, 0}; }
    }

    // ---------- Thiet bi / WebView ----------
    static String deviceName() {
        String m = Build.MANUFACTURER == null ? "" : cap(Build.MANUFACTURER);
        String mo = Build.MODEL == null ? "" : Build.MODEL;
        return (m + " " + mo).trim();
    }
    static String androidVersion() { return Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")"; }
    static String webViewVersion() {
        try {
            PackageInfo pi = android.webkit.WebView.getCurrentWebViewPackage();
            return pi != null && pi.versionName != null ? pi.versionName : "—";
        } catch (Exception e) { return "—"; }
    }
    static DisplayMetrics metrics(Context c) { return c.getResources().getDisplayMetrics(); }

    // ---------- Do ping THAT (TCP handshake, ms) ----------
    static long ping(String host, int port, int timeoutMs) {
        long t0 = SystemClock.elapsedRealtime();
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(host, port), timeoutMs);
            return SystemClock.elapsedRealtime() - t0;
        } catch (Exception e) { return -1; }
    }

    // ---------- Goi y hieu nang CPU (API 31+) — khong phai ep xung ----------
    static boolean hintSupported() { return Build.VERSION.SDK_INT >= 31; }
    static Object hintCreate(Context c, long targetNanos) {
        if (Build.VERSION.SDK_INT < 31) return null;
        try {
            PerformanceHintManager phm = (PerformanceHintManager) c.getSystemService(Context.PERFORMANCE_HINT_SERVICE);
            if (phm == null) return null;
            return phm.createHintSession(new int[]{Process.myTid()}, targetNanos);
        } catch (Throwable e) { return null; }
    }
    static void hintReport(Object session, long actualNanos) {
        if (Build.VERSION.SDK_INT < 31 || session == null) return;
        try { ((PerformanceHintManager.Session) session).reportActualWorkDuration(actualNanos); } catch (Throwable ignored) {}
    }
    static void hintTarget(Object session, long targetNanos) {
        if (Build.VERSION.SDK_INT < 31 || session == null) return;
        try { ((PerformanceHintManager.Session) session).updateTargetWorkDuration(targetNanos); } catch (Throwable ignored) {}
    }
    static void hintClose(Object session) {
        if (Build.VERSION.SDK_INT < 31 || session == null) return;
        try { ((PerformanceHintManager.Session) session).close(); } catch (Throwable ignored) {}
    }
}