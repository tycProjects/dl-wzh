package com.bubbleai.app;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Mendukung 3 format, dideteksi otomatis dari URL endpoint:
 *  - Anthropic Messages      (.../v1/messages)
 *  - OpenAI Responses        (.../v1/responses)
 *  - OpenAI Chat Completions (.../chat/completions, juga untuk provider OpenAI-compatible)
 * Semua callback dijalankan di main thread.
 */
public class ApiClient {

    public interface ResultCallback {
        void onSuccess(String reply);
        void onError(String message);
    }

    public static final int FMT_ANTHROPIC = 0;
    public static final int FMT_RESPONSES = 1;
    public static final int FMT_CHAT = 2;

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final Handler main = new Handler(Looper.getMainLooper());
    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .build();

    public static int detectFormat(String url) {
        String u = url == null ? "" : url.toLowerCase();
        if (u.contains("/responses")) return FMT_RESPONSES;
        if (u.contains("/chat/completions")) return FMT_CHAT;
        if (u.contains("/v1/messages") || u.contains("anthropic")) return FMT_ANTHROPIC;
        return FMT_CHAT;
    }

    public static String providerName(String url) {
        String u = url == null ? "" : url.toLowerCase();
        if (u.contains("anthropic.com")) return "Anthropic (Messages API)";
        if (u.contains("openai.com")) {
            return detectFormat(u) == FMT_RESPONSES ? "OpenAI (Responses API)" : "OpenAI (Chat Completions)";
        }
        if (u.isEmpty()) return "-";
        return "Kustom (" + (detectFormat(u) == FMT_ANTHROPIC ? "format Anthropic" : "format OpenAI") + ")";
    }

    public void sendMessage(String apiUrl, String apiKey, String model, String system,
                            JSONArray history, ResultCallback cb) {
        final int fmt = detectFormat(apiUrl);
        try {
            JSONObject body = new JSONObject();
            body.put("model", model);
            boolean hasSystem = system != null && !system.trim().isEmpty();

            Request.Builder rb = new Request.Builder().url(apiUrl);

            if (fmt == FMT_ANTHROPIC) {
                body.put("max_tokens", 2048);
                body.put("messages", history);
                if (hasSystem) body.put("system", system);
                rb.addHeader("x-api-key", apiKey).addHeader("anthropic-version", "2023-06-01");
            } else if (fmt == FMT_RESPONSES) {
                body.put("input", history);
                if (hasSystem) body.put("instructions", system);
                rb.addHeader("Authorization", "Bearer " + apiKey);
            } else {
                JSONArray msgs = new JSONArray();
                if (hasSystem) msgs.put(Prefs.msg("system", system));
                for (int i = 0; i < history.length(); i++) msgs.put(history.get(i));
                body.put("messages", msgs);
                rb.addHeader("Authorization", "Bearer " + apiKey);
            }

            rb.addHeader("content-type", "application/json");
            rb.post(RequestBody.create(body.toString(), JSON));

            client.newCall(rb.build()).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    fail(cb, "Gagal konek: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String bodyStr = response.body() != null ? response.body().string() : "";
                    if (!response.isSuccessful()) {
                        fail(cb, "HTTP " + response.code() + ": " + extractError(bodyStr));
                        return;
                    }
                    try {
                        String text = parseReply(fmt, new JSONObject(bodyStr)).trim();
                        if (text.isEmpty()) fail(cb, "Respons kosong dari server.");
                        else ok(cb, text);
                    } catch (Exception e) {
                        fail(cb, "Gagal membaca respons: " + e.getMessage());
                    }
                }
            });
        } catch (Exception e) {
            fail(cb, "Error: " + e.getMessage());
        }
    }

    private static String parseReply(int fmt, JSONObject json) throws Exception {
        StringBuilder sb = new StringBuilder();
        if (fmt == FMT_ANTHROPIC) {
            JSONArray content = json.optJSONArray("content");
            if (content != null) {
                for (int i = 0; i < content.length(); i++) {
                    JSONObject b = content.getJSONObject(i);
                    if ("text".equals(b.optString("type"))) sb.append(b.optString("text"));
                }
            }
        } else if (fmt == FMT_RESPONSES) {
            JSONArray out = json.optJSONArray("output");
            if (out != null) {
                for (int i = 0; i < out.length(); i++) {
                    JSONObject item = out.getJSONObject(i);
                    if (!"message".equals(item.optString("type"))) continue;
                    JSONArray parts = item.optJSONArray("content");
                    if (parts == null) continue;
                    for (int j = 0; j < parts.length(); j++) {
                        JSONObject p = parts.getJSONObject(j);
                        String t = p.optString("type");
                        if ("output_text".equals(t) || "text".equals(t)) sb.append(p.optString("text"));
                    }
                }
            }
            if (sb.length() == 0) sb.append(json.optString("output_text", ""));
        } else {
            JSONArray choices = json.optJSONArray("choices");
            if (choices != null && choices.length() > 0) {
                JSONObject m = choices.getJSONObject(0).optJSONObject("message");
                if (m != null) sb.append(m.optString("content", ""));
            }
        }
        return sb.toString();
    }

    private static String extractError(String body) {
        try {
            JSONObject j = new JSONObject(body);
            Object e = j.opt("error");
            if (e instanceof JSONObject) {
                String m = ((JSONObject) e).optString("message");
                if (!m.isEmpty()) return m;
            } else if (e instanceof String) {
                return (String) e;
            }
        } catch (Exception ignored) { }
        return body.length() > 300 ? body.substring(0, 300) : body;
    }

    private void ok(ResultCallback cb, String s) { main.post(() -> cb.onSuccess(s)); }
    private void fail(ResultCallback cb, String m) { main.post(() -> cb.onError(m)); }
}
