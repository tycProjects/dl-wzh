package com.ponie.delayx;

import android.content.Context;
import android.os.Build;
import android.provider.Settings;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class KeyActivity {

    // Key mặc định - không dùng server
    private static final String DEFAULT_KEY = "muopdepzais1tg";

    public static class Result {
        public boolean success = false;
        public boolean killed  = false;
        public boolean banned  = false;
        public String  message = "";
        public long    expires = 0;
    }

    private Context context;

    public KeyActivity(Context context) {
        this.context = context;
    }

    public Result verify(String key) {
        Result r = new Result();
        if (key == null) {
            r.message = "Vui lòng nhập key!";
            return r;
        }
        key = key.trim();

        // Chỉ chấp nhận key mặc định, bỏ hoàn toàn server
        if (key.equalsIgnoreCase(DEFAULT_KEY)) {
            r.success = true;
            r.message = "Kích hoạt thành công!";
            // Hết hạn sau 10 năm (gần như vĩnh viễn)
            r.expires = (System.currentTimeMillis() / 1000) + (10L * 365 * 24 * 3600);
        } else {
            r.success = false;
            r.message = "Key không hợp lệ!";
        }
        return r;
    }

    public Result checkAlive(String key) {
        Result r = new Result();
        if (key == null) {
            r.message = "Key không hợp lệ!";
            return r;
        }
        key = key.trim();
        if (key.equalsIgnoreCase(DEFAULT_KEY)) {
            r.success = true;
            r.expires = (System.currentTimeMillis() / 1000) + (10L * 365 * 24 * 3600);
        } else {
            r.success = false;
            r.message = "Key không hợp lệ!";
        }
        return r;
    }

    public Result checkApp() {
        return new Result();
    }

    public String[] getKeyInfo() {
        return new String[]{"", ""};
    }

    private String postJson(String urlStr, String jsonBody) {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(12000);
            conn.setReadTimeout(12000);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setRequestProperty("User-Agent", "MuopVIP/1.0");
            conn.setRequestProperty("Accept", "application/json");

            OutputStream os = conn.getOutputStream();
            os.write(jsonBody.getBytes("UTF-8"));
            os.close();

            int code = conn.getResponseCode();
            BufferedReader br = new BufferedReader(new InputStreamReader(
                    (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream(), "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            br.close();
            conn.disconnect();
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    public String getHWID() {
        try {
            android.content.SharedPreferences prefs =
                    context.getSharedPreferences("hwid_prefs", Context.MODE_PRIVATE);
            String saved = prefs.getString("stable_hwid", "");
            if (!saved.isEmpty()) return saved;
            String androidId = Settings.Secure.getString(
                    context.getContentResolver(), Settings.Secure.ANDROID_ID);
            String raw  = androidId + Build.MODEL + Build.MANUFACTURER;
            String md5  = md5(raw).toUpperCase();
            String hwid = md5.substring(0, Math.min(20, md5.length()));
            prefs.edit().putString("stable_hwid", hwid).commit();
            return hwid;
        } catch (Exception e) { return "UNKNOWN"; }
    }

    private String parseString(String json, String key) {
        try {
            String token = "\"" + key + "\":\"";
            int s = json.indexOf(token);
            if (s < 0) return "";
            s += token.length();
            int e = json.indexOf("\"", s);
            return e < 0 ? "" : json.substring(s, e);
        } catch (Exception e) { return ""; }
    }

    private String parseNumber(String json, String key) {
        try {
            String token = "\"" + key + "\":";
            int s = json.indexOf(token);
            if (s < 0) return "";
            s += token.length();
            while (s < json.length() && (json.charAt(s) == ' ' || json.charAt(s) == '\t')) s++;
            int e = s;
            while (e < json.length() && (Character.isDigit(json.charAt(e)) || json.charAt(e) == '-' || json.charAt(e) == '.')) e++;
            return json.substring(s, e);
        } catch (Exception e) { return ""; }
    }

    private long parseIso8601ToEpochSeconds(String iso) {
        if (iso == null || iso.isEmpty()) return 0;
        String normalized = iso.replace("Z", "+00:00");
        String[] patterns = {
                "yyyy-MM-dd'T'HH:mm:ssXXX",
                "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
                "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX"
        };
        for (String p : patterns) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(p, Locale.US);
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                Date d = sdf.parse(normalized);
                if (d != null) return d.getTime() / 1000;
            } catch (Exception ignored) {}
        }
        return 0;
    }

    private static String md5(String input) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] bytes = md.digest(input.getBytes("UTF-8"));
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
