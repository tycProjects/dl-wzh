package com.bubbleai.app;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

/** Penyimpanan lokal (SharedPreferences) untuk pengaturan, statistik, dan riwayat chat. */
public class Prefs {

    public static final String DEFAULT_URL = "https://api.anthropic.com/v1/messages";
    public static final String DEFAULT_MODEL = "claude-sonnet-4-6";
    private static final int MAX_HISTORY = 40;

    private static final String FILE = "bubble_ai_prefs";
    private static final String K_API_KEY = "api_key";
    private static final String K_API_URL = "api_url";
    private static final String K_MODEL = "model";
    private static final String K_SYSTEM = "system_prompt";
    private static final String K_NAME = "user_name";
    private static final String K_BUBBLE_ON = "bubble_on";
    private static final String K_SIDE = "bubble_side";
    private static final String K_HISTORY = "history";
    private static final String K_SENT = "stat_sent";
    private static final String K_RECV = "stat_recv";
    private static final String K_ERR = "stat_err";

    private final SharedPreferences sp;

    public Prefs(Context c) {
        sp = c.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public String getApiKey() { return sp.getString(K_API_KEY, "").trim(); }
    public String getApiUrl() {
        String u = sp.getString(K_API_URL, DEFAULT_URL).trim();
        return u.isEmpty() ? DEFAULT_URL : u;
    }
    public String getModel() {
        String m = sp.getString(K_MODEL, DEFAULT_MODEL).trim();
        return m.isEmpty() ? DEFAULT_MODEL : m;
    }
    public String getSystem() { return sp.getString(K_SYSTEM, "").trim(); }
    public String getName() {
        String n = sp.getString(K_NAME, "").trim();
        return n.isEmpty() ? "Pengguna" : n;
    }
    public boolean isBubbleOn() { return sp.getBoolean(K_BUBBLE_ON, false); }
    public String getSide() { return sp.getString(K_SIDE, "right"); }

    public void setConnection(String key, String url, String model, String system, String side) {
        sp.edit().putString(K_API_KEY, key.trim()).putString(K_API_URL, url.trim())
                .putString(K_MODEL, model.trim()).putString(K_SYSTEM, system.trim())
                .putString(K_SIDE, side).apply();
    }
    public void clearApiKey() { sp.edit().putString(K_API_KEY, "").apply(); }
    public void setName(String n) { sp.edit().putString(K_NAME, n.trim()).apply(); }
    public void setBubbleOn(boolean on) { sp.edit().putBoolean(K_BUBBLE_ON, on).apply(); }

    // ---- statistik ----
    public int getSent() { return sp.getInt(K_SENT, 0); }
    public int getRecv() { return sp.getInt(K_RECV, 0); }
    public int getErr() { return sp.getInt(K_ERR, 0); }
    public void incSent() { sp.edit().putInt(K_SENT, getSent() + 1).apply(); }
    public void incRecv() { sp.edit().putInt(K_RECV, getRecv() + 1).apply(); }
    public void incErr() { sp.edit().putInt(K_ERR, getErr() + 1).apply(); }

    // ---- riwayat ----
    public JSONArray getHistory() {
        try {
            return new JSONArray(sp.getString(K_HISTORY, "[]"));
        } catch (Exception e) {
            return new JSONArray();
        }
    }

    public void saveHistory(JSONArray h) {
        try {
            int start = Math.max(0, h.length() - MAX_HISTORY);
            // riwayat harus diawali pesan user
            while (start < h.length() && !"user".equals(h.getJSONObject(start).optString("role"))) start++;
            JSONArray out = new JSONArray();
            for (int i = start; i < h.length(); i++) out.put(h.getJSONObject(i));
            sp.edit().putString(K_HISTORY, out.toString()).apply();
        } catch (Exception ignored) { }
    }

    public void clearHistory() { sp.edit().putString(K_HISTORY, "[]").apply(); }

    public static JSONObject msg(String role, String content) {
        JSONObject o = new JSONObject();
        try {
            o.put("role", role);
            o.put("content", content);
        } catch (Exception ignored) { }
        return o;
    }
}
