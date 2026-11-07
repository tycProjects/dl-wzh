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
    static JSONObject create(int n) {
        JSONObject o = new JSONObject();
        try {
            o.put("name", "Hồ sơ " + n);
            o.put("uuid", UUID.randomUUID().toString());
            o.put("proxy", ""); o.put("ua", ""); o.put("lang", "en-US");
            o.put("rtc", "off"); o.put("url", "https://www.google.com");
        } catch (Exception ignored) {}
        return o;
    }
    static String proxyLabel(String raw) {
        if (raw == null || raw.trim().isEmpty()) return "Không dùng proxy";
        String s = raw.trim(), scheme = "http";
        int i = s.indexOf("://");
        if (i > 0) { scheme = s.substring(0, i); s = s.substring(i + 3); }
        if (s.contains("@")) s = s.substring(s.lastIndexOf('@') + 1);
        else { String[] a = s.split(":"); if (a.length == 4) s = a[0] + ":" + a[1]; }
        return scheme + "://" + s;
    }
}
