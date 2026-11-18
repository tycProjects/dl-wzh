package com.lsupra.fpsmeter;

import android.content.Context;
import android.content.pm.PackageManager;
import android.provider.Settings;
import java.io.*;
import java.lang.reflect.Method;
import java.util.*;
import org.json.JSONObject;
import rikka.shizuku.Shizuku;

/** Quyền cao (root hoặc Shizuku/ADB) chỉ dùng để đọc FPS rendering của game. Dùng 1 shell bền để không tốn tài nguyên. */
final class Priv {
    static volatile boolean rootGranted = false, rootChecked = false;
    private static Process sh;
    private static BufferedWriter in;
    private static BufferedReader out;
    private static int shLv = -1;
    private static String layer;
    private static long lastList, lastFg;

    static boolean shRunning() { try { return Shizuku.pingBinder(); } catch (Throwable t) { return false; } }
    static boolean shGranted() {
        try { return Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED; }
        catch (Throwable t) { return false; }
    }
    static void shRequest() { try { Shizuku.requestPermission(100); } catch (Throwable ignored) {} }
    static boolean shInstalled(Context c) {
        try { c.getPackageManager().getPackageInfo("moe.shizuku.privileged.api", 0); return true; } catch (Exception e) { return false; }
    }
    /** 2 = root, 1 = Shizuku/ADB, 0 = không có quyền cao */
    static int level() { return rootGranted ? 2 : shGranted() ? 1 : 0; }

    static void detectRoot() {
        rootChecked = true;
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"su", "-c", "id"});
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String l;
            while ((l = r.readLine()) != null) sb.append(l);
            p.waitFor();
            rootGranted = sb.toString().contains("uid=0");
        } catch (Throwable t) { rootGranted = false; }
    }

    static String state(Context c) {
        try {
            JSONObject o = new JSONObject();
            int lv = level();
            o.put("mode", lv == 2 ? "root" : lv == 1 ? "shizuku" : "none");
            o.put("rootChecked", rootChecked); o.put("rootGranted", rootGranted);
            int uid = -1;
            try { if (shGranted()) uid = Shizuku.getUid(); } catch (Throwable ignored) {}
            o.put("shUid", uid);
            o.put("shInst", shInstalled(c)); o.put("shRun", shRunning()); o.put("shOk", shGranted());
            return o.toString();
        } catch (Exception e) { return "{\"mode\":\"none\"}"; }
    }

    static synchronized void close() {
        try { if (sh != null) sh.destroy(); } catch (Throwable ignored) {}
        sh = null; in = null; out = null; shLv = -1; layer = null;
    }

    private static boolean open() {
        int lv = level();
        if (lv == 0) { close(); return false; }
        if (sh != null && shLv == lv) return true;
        close();
        try {
            Process p;
            if (lv == 2) p = Runtime.getRuntime().exec(new String[]{"su"});
            else {
                Method m = Shizuku.class.getDeclaredMethod("newProcess", String[].class, String[].class, String.class);
                m.setAccessible(true);
                p = (Process) m.invoke(null, new Object[]{new String[]{"sh"}, null, null});
            }
            sh = p; shLv = lv;
            in = new BufferedWriter(new OutputStreamWriter(p.getOutputStream()));
            out = new BufferedReader(new InputStreamReader(p.getInputStream()));
            return true;
        } catch (Throwable t) { close(); return false; }
    }

    private static synchronized String run(String cmd) {
        if (!open()) return null;
        try {
            in.write(cmd + " 2>&1\necho __E__\n");
            in.flush();
            StringBuilder sb = new StringBuilder();
            String l;
            while ((l = out.readLine()) != null) {
                if (l.equals("__E__")) return sb.toString();
                sb.append(l).append('\n');
            }
        } catch (Throwable ignored) {}
        close();
        return null;
    }

    /** Chạy lệnh shell bằng quyền Shizuku/root (null nếu chưa có quyền). */
    static String exec(String cmd) { return run(cmd); }

    /** Gói ứng dụng đang ở trên cùng (qua Shizuku/root). */
    private static String fgPkg() {
        String r = run("dumpsys activity activities | grep -m1 -E 'topResumedActivity|mResumedActivity'");
        if (r == null) return null;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\su\\d+\\s+([A-Za-z0-9_.]+)/").matcher(r);
        return m.find() ? m.group(1) : null;
    }

    /** Tự cấp quyền cho chính app bằng quyền Shizuku/root: nổi, chạy nền, thông báo, bỏ tối ưu pin. */
    static String autoGrant(Context c) {
        if (level() == 0) return "✖ Cần kết nối Shizuku hoặc root trước";
        String p = c.getPackageName();
        run("appops set " + p + " SYSTEM_ALERT_WINDOW allow");
        run("cmd appops set " + p + " RUN_IN_BACKGROUND allow; cmd appops set " + p + " RUN_ANY_IN_BACKGROUND allow");
        run("dumpsys deviceidle whitelist +" + p);
        run("pm grant " + p + " android.permission.POST_NOTIFICATIONS");
        run("appops set " + p + " WRITE_SETTINGS allow");
        run("pm grant " + p + " android.permission.WRITE_SECURE_SETTINGS");
        try { Thread.sleep(300); } catch (Exception ignored) {}
        return Settings.canDrawOverlays(c) ? "✔ Đã tự cấp quyền: nổi, chạy nền, thông báo, bỏ tối ưu pin"
                : "✖ Chưa cấp được quyền nổi, hãy cấp thủ công trong cài đặt";
    }

    /** Đọc FPS rendering của game (SurfaceFlinger). Trả {fps, 1%low}; null nếu chưa thấy game; {0,0} nếu game không vẽ khung mới. */
    static float[] sample(List<String> pkgs, boolean auto, String self) {
        long now = System.currentTimeMillis();
        if (layer == null) {
            if (now - lastList < 1500) return null;
            lastList = now;
            String list = run("dumpsys SurfaceFlinger --list");
            if (list == null) return null;
            String best = pickLayer(list, pkgs);
            if (best == null && auto && now - lastFg > 4000) {
                lastFg = now;
                String f = fgPkg();
                if (f != null && !f.equals(self) && !f.contains("launcher") && !f.equals("com.android.systemui") && !f.equals("android")) {
                    best = pickLayer(list, Collections.singletonList(f));
                }
            }
            if (best == null) return null;
            layer = best;
        }
        String res = run("dumpsys SurfaceFlinger --latency '" + layer.replace("'", "") + "'");
        float[] r = res == null ? null : parse(res);
        if (r == null) layer = null;
        return r;
    }

    private static String pickLayer(String list, List<String> pkgs) {
        String best = null;
        for (String l : list.split("\n")) {
            String t = l.trim();
            for (String p : pkgs) {
                if (p.isEmpty() || !t.contains(p)) continue;
                if (t.contains("SurfaceView")) return t;
                if (best == null) best = t;
            }
        }
        return best;
    }

    private static float[] parse(String out) {
        String[] ls = out.split("\n");
        ArrayList<Long> ts = new ArrayList<>();
        for (int i = 1; i < ls.length; i++) {
            String[] c = ls[i].trim().split("\\s+");
            if (c.length < 3) continue;
            try { long v = Long.parseLong(c[1]); if (v > 0 && v < Long.MAX_VALUE / 2) ts.add(v); } catch (Exception ignored) {}
        }
        int n = ts.size();
        if (n < 5) return null;
        long max = ts.get(n - 1);
        if (System.nanoTime() - max > 1500000000L) return new float[]{0, 0};
        int cnt = 0;
        long first = max;
        for (long v : ts) if (v > max - 1000000000L) { cnt++; if (v < first) first = v; }
        float fps = (cnt >= 3 && max > first) ? (cnt - 1) * 1e9f / (max - first) : (n - 1) * 1e9f / Math.max(1, max - ts.get(0));
        double[] ft = new double[n - 1];
        for (int i = 1; i < n; i++) ft[i - 1] = ts.get(i) - ts.get(i - 1);
        Arrays.sort(ft);
        double p99 = ft[(int) Math.min(ft.length - 1, Math.ceil(ft.length * 0.99) - 1)];
        return new float[]{fps, p99 > 0 ? (float) (1e9 / p99) : fps};
    }
}
