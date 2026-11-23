package com.ryomd.nativeapp;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class RyomdNativeBridge {
    private static final int MAX_RESPONSE_BYTES = 256 * 1024;
    private static final int MAX_REQUEST_BYTES = 8 * 1024;
    private static final Set<String> ALLOWED_ACTIONS = new HashSet<>(Arrays.asList(
            "start", "start-all", "stop", "rvc", "stop-rvc"
    ));

    private final Context context;

    RyomdNativeBridge(Context context) {
        this.context = context.getApplicationContext();
    }

    @JavascriptInterface
    public String getAppInfo() {
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            JSONObject out = new JSONObject();
            out.put("app", "Ryomd Overdrive");
            out.put("package", context.getPackageName());
            out.put("version", info.versionName == null ? "unknown" : info.versionName);
            out.put("androidSdk", Build.VERSION.SDK_INT);
            out.put("nativeShell", true);
            out.put("termuxNativeBridge", true);
            return out.toString();
        } catch (Exception e) {
            return "{\"app\":\"Ryomd Overdrive\",\"nativeShell\":true,\"termuxNativeBridge\":true}";
        }
    }

    /**
     * Calls only the local Ryomd Control Agent. This bypasses browser CORS and
     * mixed-content fetch restrictions without exposing a general HTTP proxy.
     * Allowed targets: http://127.0.0.1:8787/api/status and /api/action.
     */
    @JavascriptInterface
    public String requestTermux(String urlText, String token, String method, String body) {
        HttpURLConnection connection = null;
        try {
            if (urlText == null || token == null || method == null) {
                return bridgeError("Parameter koneksi tidak lengkap.");
            }
            String cleanToken = token.trim();
            if (cleanToken.isEmpty() || cleanToken.length() > 512 || cleanToken.contains("\n") || cleanToken.contains("\r")) {
                return bridgeError("Token autentikasi kosong atau tidak valid.");
            }
            URL url = new URL(urlText);
            String host = url.getHost() == null ? "" : url.getHost().toLowerCase();
            String path = url.getPath() == null ? "" : url.getPath();
            String verb = method.trim().toUpperCase();
            if (!"http".equalsIgnoreCase(url.getProtocol())
                    || !("127.0.0.1".equals(host) || "localhost".equals(host))
                    || url.getPort() != 8787
                    || url.getUserInfo() != null || url.getQuery() != null || url.getRef() != null) {
                return bridgeError("Alamat ditolak. Hanya http://127.0.0.1:8787 yang diizinkan.");
            }
            boolean statusPath = "/api/status".equals(path) && "GET".equals(verb);
            boolean actionPath = "/api/action".equals(path) && "POST".equals(verb);
            if (!statusPath && !actionPath) {
                return bridgeError("Endpoint/metode tidak termasuk allowlist native bridge.");
            }

            byte[] requestBody = body == null ? new byte[0] : body.getBytes(StandardCharsets.UTF_8);
            if (requestBody.length > MAX_REQUEST_BYTES) {
                return bridgeError("Body permintaan terlalu besar.");
            }
            if (actionPath) {
                JSONObject actionObject = new JSONObject(body == null ? "{}" : body);
                String action = actionObject.optString("action", "");
                if (!ALLOWED_ACTIONS.contains(action)) {
                    return bridgeError("Aksi tidak diizinkan oleh native bridge.");
                }
            }

            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(verb);
            connection.setConnectTimeout(6000);
            connection.setReadTimeout(actionPath ? 150000 : 45000);
            connection.setInstanceFollowRedirects(false);
            connection.setUseCaches(false);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + cleanToken);
            connection.setRequestProperty("Cache-Control", "no-store");
            connection.setRequestProperty("Connection", "close");
            if (actionPath) {
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                connection.setFixedLengthStreamingMode(requestBody.length);
                try (OutputStream out = connection.getOutputStream()) {
                    out.write(requestBody);
                    out.flush();
                }
            }

            int status = connection.getResponseCode();
            InputStream stream = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
            String responseText = stream == null ? "" : readBounded(stream, MAX_RESPONSE_BYTES);
            JSONObject envelope = new JSONObject();
            envelope.put("transportOk", true);
            envelope.put("status", status);
            envelope.put("body", responseText);
            return envelope.toString();
        } catch (Exception e) {
            String message = e.getMessage();
            if (message == null || message.trim().isEmpty()) message = e.getClass().getSimpleName();
            return bridgeError("Gagal menghubungi Control Agent: " + message);
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private static String readBounded(InputStream input, int maximum) throws Exception {
        try (InputStream in = input; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int total = 0;
            int count;
            while ((count = in.read(buffer)) != -1) {
                total += count;
                if (total > maximum) throw new Exception("Respons server melebihi batas ukuran.");
                out.write(buffer, 0, count);
            }
            return out.toString("UTF-8");
        }
    }

    private static String bridgeError(String message) {
        try {
            JSONObject out = new JSONObject();
            out.put("transportOk", false);
            out.put("error", message);
            return out.toString();
        } catch (Exception ignored) {
            return "{\"transportOk\":false,\"error\":\"Native bridge error\"}";
        }
    }

    @JavascriptInterface
    public void showToast(String message) {
        if (message == null) return;
        String safe = message.trim();
        if (safe.length() > 180) safe = safe.substring(0, 180);
        if (safe.isEmpty()) return;
        final String toastText = safe;
        android.os.Handler main = new android.os.Handler(context.getMainLooper());
        main.post(() -> Toast.makeText(context, toastText, Toast.LENGTH_SHORT).show());
    }
}
