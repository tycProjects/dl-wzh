package com.pb.browser;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.UUID;

final class Store {
    static JSONArray load(Context c) {
        try { return new JSONArray(c.getSharedPreferences("pb", 0).getString("profiles", "[]")); }
        catch (Exception e) { return new JSONArray(); }
    }
    static void save(Context c, JSONArray a) {
        c.getSharedPreferences("pb", 0).edit().putString("profiles", a.toString()).apply();
    }
    // Chi so UA (0..49) luu bang FILE de 2 process (giao dien / trinh duyet) cung thay ngay
    static java.io.File uaFile(Context c, String uuid) { return new java.io.File(c.getFilesDir(), "ua_" + uuid + ".idx"); }
    static int uaIdx(Context c, JSONObject p) {
        try {
            java.io.File f = uaFile(c, p.optString("uuid"));
            if (f.exists()) {
                java.io.BufferedReader r = new java.io.BufferedReader(new java.io.FileReader(f));
                String s = r.readLine(); r.close();
                return Math.max(0, Integer.parseInt(s.trim()));
            }
        } catch (Exception ignored) {}
        return Math.max(0, p.optInt("uaIdx", 0));
    }
    static void setUaIdx(Context c, String uuid, int i) {
        try { java.io.FileWriter w = new java.io.FileWriter(uaFile(c, uuid)); w.write(String.valueOf(i)); w.close(); }
        catch (Exception ignored) {}
    }
    static JSONObject create(int n) {
        JSONObject o = new JSONObject();
        try {
            o.put("name", "Hồ sơ " + n);
            o.put("uuid", UUID.randomUUID().toString());
            o.put("proxy", ""); o.put("ptype", "http"); o.put("ua", ""); o.put("lang", "vi-VN");
            o.put("rtc", "off"); o.put("url", "https://www.google.com");
        } catch (Exception ignored) {}
        return o;
    }
    // Scheme trong chuoi proxy (neu co) luon thang lua chon ptype; socks/socks4 quy ve socks5
    static String schemeOf(String raw, String ptype) {
        String s = raw == null ? "" : raw.trim();
        int i = s.indexOf("://");
        if (i > 0) {
            String sc = s.substring(0, i).toLowerCase();
            if (sc.startsWith("socks")) return "socks5";
            if (sc.equals("http") || sc.equals("https")) return "http";
        }
        return "socks5".equals(ptype) ? "socks5" : "http";
    }
    static String proxyLabel(String raw, String ptype) {
        if (raw == null || raw.trim().isEmpty()) return "Không dùng proxy";
        String s = raw.trim();
        int i = s.indexOf("://");
        if (i > 0) s = s.substring(i + 3);
        if (s.contains("@")) s = s.substring(s.lastIndexOf('@') + 1);
        else { String[] a = s.split(":"); if (a.length == 4) s = a[0] + ":" + a[1]; }
        return schemeOf(raw, ptype) + "://" + s;
    }
    // Tuy chon hieu nang (luu trong prefs "pb")
    static android.content.SharedPreferences prefs(Context c) { return c.getSharedPreferences("pb", 0); }
    static boolean perf(Context c, String key, boolean def) { return prefs(c).getBoolean(key, def); }
    static void setPerf(Context c, String key, boolean v) { prefs(c).edit().putBoolean(key, v).apply(); }
}
