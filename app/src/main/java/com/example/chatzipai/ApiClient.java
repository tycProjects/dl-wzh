package com.example.chatzipai;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Client cho mọi API kiểu OpenAI (/chat/completions, /models): Groq, DeepSeek, OpenAI, OpenRouter... */
final class ApiClient {
    interface Listener {
        void onToken(String t);
        void onReasoning(String t);
        void onDone();
        void onError(String message);
    }

    /** Lỗi 429: có thể thử lại sau waitMs. */
    static final class RateLimit extends Exception {
        final long waitMs;       // thời gian nên chờ (ms)
        final int limit;         // giới hạn token/phút đọc được từ thông báo (0 = không rõ)
        final boolean tooBig;    // true: một lần gửi đã vượt giới hạn, chờ cũng không hết
        RateLimit(String m, long waitMs, int limit, boolean tooBig) {
            super(m);
            this.waitMs = waitMs;
            this.limit = limit;
            this.tooBig = tooBig;
        }
    }

    private static final Pattern WAIT = Pattern.compile("try again in (?:(\\d+)m)?([0-9.]+)(ms|s)");
    private static final Pattern LIMIT = Pattern.compile("Limit (\\d+), Used (\\d+), Requested (\\d+)");

    private volatile HttpURLConnection conn;
    private volatile boolean cancelled;

    /** Đọc thời gian chờ từ thông báo "try again in 2.04s" hoặc header Retry-After. */
    private static long waitMs(String body, String retryAfter) {
        try {
            Matcher m = WAIT.matcher(body == null ? "" : body);
            if (m.find()) {
                double v = Double.parseDouble(m.group(2));
                double ms = "ms".equals(m.group(3)) ? v : v * 1000;
                if (m.group(1) != null) ms += Integer.parseInt(m.group(1)) * 60000.0;
                return (long) ms;
            }
            if (retryAfter != null) return (long) (Double.parseDouble(retryAfter.trim()) * 1000);
        } catch (Exception ignored) { }
        return 5000;
    }

    static String base(String b) {
        b = b == null ? "" : b.trim();
        while (b.endsWith("/")) b = b.substring(0, b.length() - 1);
        return b;
    }

    void cancel() {
        cancelled = true;
        HttpURLConnection c = conn;
        if (c != null) {
            try { c.disconnect(); } catch (Exception ignored) { }
        }
    }

    private HttpURLConnection open(String url, String key, String method) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setRequestMethod(method);
        c.setConnectTimeout(20000);
        c.setReadTimeout(120000);
        if (key != null && !key.trim().isEmpty()) c.setRequestProperty("Authorization", "Bearer " + key.trim());
        c.setRequestProperty("Content-Type", "application/json");
        return c;
    }

    private static String readAll(InputStream is) throws Exception {
        if (is == null) return "";
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = is.read(buf)) != -1) bo.write(buf, 0, n);
        return new String(bo.toByteArray(), StandardCharsets.UTF_8);
    }

    private static String str(JSONObject o, String k) {
        if (o == null || o.isNull(k)) return "";
        return o.optString(k, "");
    }

    static String errorText(int code, String body) {
        String msg = body == null ? "" : body.trim();
        try {
            JSONObject j = new JSONObject(msg);
            Object e = j.opt("error");
            if (e instanceof JSONObject) msg = str((JSONObject) e, "message");
            else if (e != null) msg = String.valueOf(e);
            else if (j.has("message")) msg = str(j, "message");
        } catch (Exception ignored) { }
        if (msg.length() > 400) msg = msg.substring(0, 400) + "…";
        String hint = code == 401 || code == 403 ? " (sai hoặc thiếu API key)"
                : code == 404 ? " (sai Base URL hoặc tên model)"
                : code == 429 ? " (quá giới hạn / hết quota)" : "";
        String lower = msg.toLowerCase();
        if (lower.contains("model") && (lower.contains("decommission") || lower.contains("does not exist")
                || lower.contains("not found") || lower.contains("not supported"))) {
            hint = " (model này không có / đã ngừng, hãy bấm tên model ở thanh trên để chọn model khác)";
        }
        return "HTTP " + code + hint + (msg.isEmpty() ? "" : ": " + msg);
    }

    /** Chat dạng stream (SSE). Chạy trên thread nền; gọi Listener từ thread đó. */
    void stream(String baseUrl, String key, String model, JSONArray messages, Listener l) {
        cancelled = false;
        HttpURLConnection c = null;
        try {
            c = open(base(baseUrl) + "/chat/completions", key, "POST");
            conn = c;
            c.setRequestProperty("Accept", "text/event-stream");
            c.setRequestProperty("Accept-Encoding", "identity");
            c.setDoOutput(true);
            JSONObject body = new JSONObject()
                    .put("model", model).put("messages", messages).put("stream", true);
            try (OutputStream os = c.getOutputStream()) {
                os.write(body.toString().getBytes(StandardCharsets.UTF_8));
            }
            int code = c.getResponseCode();
            if (code / 100 != 2) {
                l.onError(errorText(code, readAll(c.getErrorStream())));
                return;
            }
            BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream(), StandardCharsets.UTF_8));
            String line;
            while ((line = br.readLine()) != null) {
                if (cancelled) break;
                line = line.trim();
                if (!line.startsWith("data:")) continue;
                String data = line.substring(5).trim();
                if (data.isEmpty()) continue;
                if (data.equals("[DONE]")) break;
                JSONObject j;
                try { j = new JSONObject(data); } catch (Exception e) { continue; }
                if (j.has("error") && !j.isNull("error")) {
                    Object e = j.opt("error");
                    l.onError(e instanceof JSONObject ? str((JSONObject) e, "message") : String.valueOf(e));
                    return;
                }
                JSONArray ch = j.optJSONArray("choices");
                if (ch == null || ch.length() == 0) continue;
                JSONObject delta = ch.optJSONObject(0) == null ? null : ch.optJSONObject(0).optJSONObject("delta");
                if (delta == null) continue;
                String r = str(delta, "reasoning_content");
                if (r.isEmpty()) r = str(delta, "reasoning");
                if (!r.isEmpty()) l.onReasoning(r);
                String t = str(delta, "content");
                if (!t.isEmpty()) l.onToken(t);
            }
            l.onDone();
        } catch (Exception e) {
            if (cancelled) l.onDone();
            else l.onError(e.getMessage() == null ? e.toString() : e.getMessage());
        } finally {
            if (c != null) {
                try { c.disconnect(); } catch (Exception ignored) { }
            }
        }
    }

    /** Chat không stream, trả về toàn bộ câu trả lời. */
    String complete(String baseUrl, String key, String model, JSONArray messages) throws Exception {
        HttpURLConnection c = open(base(baseUrl) + "/chat/completions", key, "POST");
        conn = c;
        try {
            c.setDoOutput(true);
            JSONObject body = new JSONObject().put("model", model).put("messages", messages);
            try (OutputStream os = c.getOutputStream()) {
                os.write(body.toString().getBytes(StandardCharsets.UTF_8));
            }
            int code = c.getResponseCode();
            if (code == 429) {
                String eb = readAll(c.getErrorStream());
                int limit = 0;
                boolean tooBig = false;
                Matcher lm = LIMIT.matcher(eb);
                if (lm.find()) {
                    limit = Integer.parseInt(lm.group(1));
                    tooBig = Long.parseLong(lm.group(3)) > Long.parseLong(lm.group(1));
                }
                throw new RateLimit(errorText(code, eb),
                        tooBig ? -1 : waitMs(eb, c.getHeaderField("Retry-After")), limit, tooBig);
            }
            if (code / 100 != 2) throw new Exception(errorText(code, readAll(c.getErrorStream())));
            JSONObject j = new JSONObject(readAll(c.getInputStream()));
            return j.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");
        } finally {
            c.disconnect();
        }
    }

    /** Lấy danh sách model từ GET /models. */
    List<String> fetchModels(String baseUrl, String key) throws Exception {
        HttpURLConnection c = open(base(baseUrl) + "/models", key, "GET");
        conn = c;
        try {
            int code = c.getResponseCode();
            if (code / 100 != 2) throw new Exception(errorText(code, readAll(c.getErrorStream())));
            String txt = readAll(c.getInputStream()).trim();
            JSONArray arr;
            if (txt.startsWith("[")) arr = new JSONArray(txt);
            else {
                JSONObject j = new JSONObject(txt);
                arr = j.has("data") ? j.getJSONArray("data") : j.optJSONArray("models");
            }
            List<String> out = new ArrayList<>();
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    Object o = arr.get(i);
                    if (o instanceof JSONObject) {
                        String id = str((JSONObject) o, "id");
                        if (id.isEmpty()) id = str((JSONObject) o, "name");
                        if (!id.isEmpty()) out.add(id);
                    } else if (o instanceof String) {
                        out.add((String) o);
                    }
                }
            }
            if (out.isEmpty()) throw new Exception("API không trả về model nào");
            Collections.sort(out);
            return out;
        } finally {
            c.disconnect();
        }
    }
}
