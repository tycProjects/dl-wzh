package com.termux.hz;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;

/** Minimal OpenAI-compatible streaming client for Groq and OpenRouter (with tool calling). */
public final class LlmClient {

    public enum Provider {
        GROQ("Groq", Hz.GROQ_BASE, Hz.K_GROQ_KEY, Hz.K_GROQ_MODEL, "llama-3.3-70b-versatile"),
        OPENROUTER("OpenRouter", Hz.OPENROUTER_BASE, Hz.K_OR_KEY, Hz.K_OR_MODEL, "openrouter/auto");

        public final String label, base, keyPref, modelPref, defaultModel;
        Provider(String l, String b, String k, String m, String d) { label = l; base = b; keyPref = k; modelPref = m; defaultModel = d; }
    }

    public static final class Model {
        public final String id; public final int ctx; public final boolean tools;
        Model(String id, int ctx, boolean tools) { this.id = id; this.ctx = ctx; this.tools = tools; }
        @Override public String toString() {
            return id + (ctx > 0 ? "  ·  " + (ctx >= 1000 ? ctx / 1000 + "k" : ctx) : "") + (tools ? "" : "  ·  no tools");
        }
    }

    public interface Stream { void onText(String delta); }

    public static final class Reply {
        public String content = "";
        public JSONArray toolCalls = new JSONArray();
        public String finish = "";
        public String error;
        public int httpCode;
        public boolean cancelled;
    }

    public static final class Cancel { public volatile boolean cancelled; volatile HttpURLConnection conn;
        public void cancel() { cancelled = true; HttpURLConnection c = conn; if (c != null) try { c.disconnect(); } catch (Exception ignored) {} } }

    private LlmClient() {}

    public static List<Model> listModels(Provider p, String key) throws IOException, JSONException {
        HttpURLConnection c = open(p, key, "/models", "GET");
        int code = c.getResponseCode();
        String body = read(code >= 400 ? c.getErrorStream() : c.getInputStream());
        if (code >= 400) throw new IOException("HTTP " + code + ": " + errorMessage(body));
        JSONArray data = new JSONObject(body).getJSONArray("data");
        List<Model> out = new ArrayList<>();
        for (int i = 0; i < data.length(); i++) {
            JSONObject m = data.getJSONObject(i);
            String id = m.optString("id");
            if (id.isEmpty()) continue;
            String low = id.toLowerCase();
            if (p == Provider.GROQ && (low.contains("whisper") || low.contains("tts") || low.contains("guard") || low.contains("playai"))) continue;
            int ctx = m.optInt("context_window", m.optInt("context_length", 0));
            boolean tools = true;
            JSONArray sp = m.optJSONArray("supported_parameters");
            if (sp != null) {
                tools = false;
                for (int k = 0; k < sp.length(); k++) if ("tools".equals(sp.optString(k))) tools = true;
            }
            out.add(new Model(id, ctx, tools));
        }
        Collections.sort(out, (a, b) -> a.id.compareToIgnoreCase(b.id));
        return out;
    }

    /** One streamed chat completion. Blocking. */
    public static Reply chat(Provider p, String key, String model, JSONArray messages, JSONArray tools,
                             Stream stream, Cancel cancel) {
        Reply r = new Reply();
        HttpURLConnection c = null;
        try {
            JSONObject body = new JSONObject();
            body.put("model", model);
            body.put("messages", messages);
            body.put("stream", true);
            body.put("temperature", 0.3);
            if (tools != null && tools.length() > 0) {
                body.put("tools", tools);
                body.put("tool_choice", "auto");
            }
            c = open(p, key, "/chat/completions", "POST");
            cancel.conn = c;
            c.setDoOutput(true);
            c.setRequestProperty("Content-Type", "application/json");
            c.setRequestProperty("Accept", "text/event-stream");
            try (OutputStream o = c.getOutputStream()) { o.write(body.toString().getBytes("UTF-8")); }
            int code = c.getResponseCode();
            r.httpCode = code;
            if (code >= 400) {
                r.error = "HTTP " + code + ": " + errorMessage(read(c.getErrorStream()));
                return r;
            }
            TreeMap<Integer, JSONObject> calls = new TreeMap<>();
            StringBuilder content = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream(), "UTF-8"))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (cancel.cancelled) { r.cancelled = true; break; }
                    if (!line.startsWith("data:")) continue;
                    String d = line.substring(5).trim();
                    if (d.isEmpty()) continue;
                    if (d.equals("[DONE]")) break;
                    JSONObject j;
                    try { j = new JSONObject(d); } catch (JSONException e) { continue; }
                    if (j.has("error")) { r.error = errorMessage(d); break; }
                    JSONArray ch = j.optJSONArray("choices");
                    if (ch == null || ch.length() == 0) continue;
                    JSONObject choice = ch.getJSONObject(0);
                    if (!choice.isNull("finish_reason")) r.finish = choice.optString("finish_reason");
                    JSONObject delta = choice.optJSONObject("delta");
                    if (delta == null) continue;
                    String t = delta.isNull("content") ? "" : delta.optString("content", "");
                    if (!t.isEmpty()) { content.append(t); if (stream != null) stream.onText(t); }
                    JSONArray tc = delta.optJSONArray("tool_calls");
                    if (tc != null) for (int i = 0; i < tc.length(); i++) {
                        JSONObject d1 = tc.getJSONObject(i);
                        int idx = d1.optInt("index", i);
                        JSONObject acc = calls.get(idx);
                        if (acc == null) {
                            acc = new JSONObject();
                            acc.put("id", "");
                            acc.put("type", "function");
                            acc.put("function", new JSONObject().put("name", "").put("arguments", ""));
                            calls.put(idx, acc);
                        }
                        if (d1.has("id") && !d1.isNull("id")) acc.put("id", d1.getString("id"));
                        JSONObject fn = d1.optJSONObject("function");
                        if (fn != null) {
                            JSONObject af = acc.getJSONObject("function");
                            if (fn.has("name") && !fn.isNull("name")) af.put("name", af.getString("name") + fn.getString("name"));
                            if (fn.has("arguments") && !fn.isNull("arguments")) af.put("arguments", af.getString("arguments") + fn.getString("arguments"));
                        }
                    }
                }
            }
            r.content = content.toString();
            int n = 0;
            for (JSONObject call : calls.values()) {
                if (call.getString("id").isEmpty()) call.put("id", "call_" + System.nanoTime() + "_" + n);
                n++;
                if (call.getJSONObject("function").getString("arguments").trim().isEmpty())
                    call.getJSONObject("function").put("arguments", "{}");
                r.toolCalls.put(call);
            }
            if (cancel.cancelled) r.cancelled = true;
        } catch (Exception e) {
            if (cancel.cancelled) r.cancelled = true;
            else r.error = e.getClass().getSimpleName() + ": " + e.getMessage();
        } finally {
            if (c != null) try { c.disconnect(); } catch (Exception ignored) {}
        }
        return r;
    }

    private static HttpURLConnection open(Provider p, String key, String path, String method) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(p.base + path).openConnection();
        c.setRequestMethod(method);
        c.setConnectTimeout(20000);
        c.setReadTimeout(180000);
        if (key != null && !key.isEmpty()) c.setRequestProperty("Authorization", "Bearer " + key);
        if (p == Provider.OPENROUTER) {
            c.setRequestProperty("HTTP-Referer", "https://github.com/hz-app");
            c.setRequestProperty("X-Title", "HZ Mod Builder");
        }
        return c;
    }

    private static String read(InputStream in) throws IOException {
        if (in == null) return "";
        try (BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"))) {
            StringBuilder sb = new StringBuilder();
            char[] b = new char[4096];
            int n;
            while ((n = br.read(b)) > 0) sb.append(b, 0, n);
            return sb.toString();
        }
    }

    static String errorMessage(String body) {
        try {
            JSONObject o = new JSONObject(body);
            Object e = o.opt("error");
            if (e instanceof JSONObject) {
                JSONObject eo = (JSONObject) e;
                String m = eo.optString("message", body);
                JSONObject meta = eo.optJSONObject("metadata");
                if (meta != null && meta.has("raw")) m += " (" + meta.optString("raw") + ")";
                return m;
            }
            if (e != null) return e.toString();
        } catch (Exception ignored) {}
        return body.length() > 400 ? body.substring(0, 400) : body;
    }
}
