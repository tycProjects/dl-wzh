package com.ryomd.nativeapp;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import org.json.JSONObject;
import java.net.URL;
import java.util.Locale;

public final class RyomdNativeBridge {
    private final Context context;
    private final RyomdMediaSaver mediaSaver;
    private final String userAgent;
    private final String trustedReferer;

    RyomdNativeBridge(Context context, RyomdMediaSaver mediaSaver, WebView webView, String trustedReferer) {
        this.context = context.getApplicationContext();
        this.mediaSaver = mediaSaver;
        this.userAgent = webView == null ? "" : webView.getSettings().getUserAgentString();
        this.trustedReferer = trustedReferer == null ? "http://127.0.0.1:3002/" : trustedReferer;
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
            out.put("mediaStore", true);
            out.put("saveToGallery", true);
            return out.toString();
        } catch (Exception e) {
            return "{\"app\":\"Ryomd Overdrive\",\"nativeShell\":true,\"mediaStore\":true,\"saveToGallery\":true}";
        }
    }

    @JavascriptInterface
    public boolean canSaveToGallery() {
        return mediaSaver != null && (Build.VERSION.SDK_INT >= 29
                || context.checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == android.content.pm.PackageManager.PERMISSION_GRANTED);
    }

    @JavascriptInterface
    public boolean isMediaStoreAvailable() { return canSaveToGallery(); }

    @JavascriptInterface
    public boolean saveToGallery(String rawUrl) { return queueSave(rawUrl, null, null); }

    @JavascriptInterface
    public boolean saveMedia(String rawUrl, String fileName, String mimeType) {
        String disposition = (fileName == null || fileName.trim().isEmpty()) ? null
                : "attachment; filename=\"" + fileName.replace("\"", "") + "\"";
        return queueSave(rawUrl, disposition, mimeType);
    }

    @JavascriptInterface
    public boolean saveToGalleryWithDetails(String rawUrl, String fileName, String mimeType) {
        return saveMedia(rawUrl, fileName, mimeType);
    }

    @JavascriptInterface
    public void showToast(String message) {
        if (message == null) return;
        String safe = message.trim();
        if (safe.length() > 180) safe = safe.substring(0, 180);
        if (safe.isEmpty()) return;
        final String toastText = safe;
        new android.os.Handler(context.getMainLooper()).post(() ->
                android.widget.Toast.makeText(context, toastText, android.widget.Toast.LENGTH_SHORT).show());
    }

    private boolean queueSave(String rawUrl, String disposition, String mimeType) {
        if (mediaSaver == null || rawUrl == null || rawUrl.trim().isEmpty()) return false;
        try {
            URL url = new URL(new URL(trustedReferer), rawUrl.trim());
            String host = url.getHost() == null ? "" : url.getHost().toLowerCase(Locale.ROOT);
            boolean loopback = "127.0.0.1".equals(host) || "localhost".equals(host) || "::1".equals(host);
            if (!"http".equalsIgnoreCase(url.getProtocol()) || !loopback || url.getPort() != 3002
                    || !url.getPath().startsWith("/api/downloads/file")) return false;
            return mediaSaver.download(url.toString(), userAgent, disposition,
                    mimeType == null ? "" : mimeType, trustedReferer);
        } catch (Exception ignored) { return false; }
    }
}
