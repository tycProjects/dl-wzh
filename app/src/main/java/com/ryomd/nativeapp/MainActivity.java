package com.ryomd.nativeapp;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Insets;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.webkit.ConsoleMessage;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Ryomd native shell.
 *
 * The web panel is served by the existing Termux Node.js backend on loopback.
 * Loading the panel and its /api routes from one origin avoids cross-origin
 * requests and keeps the existing backend API paths unchanged.
 */
public final class MainActivity extends Activity {
    private static final int FILE_PICKER_REQUEST = 4817;
    private static final String BACKEND_HOST = "127.0.0.1";
    private static final int BACKEND_PORT = 3002;
    private static final String START_URL = "http://127.0.0.1:3002/";

    /**
     * true  = UI (index.html, app.js, styles.css, modul) dilayani dari APK, sehingga tombol native
     *         (pemutar YouTube Media3, simpan ke galeri) selalu cocok dengan versi APK. API (/api/...)
     *         tetap diteruskan ke backend Termux di 127.0.0.1:3002.
     * false = semua dimuat dari server Termux seperti versi lama.
     */
    private static final boolean USE_BUNDLED_UI = true;
    private static final int NOTIFICATION_PERMISSION_REQUEST = 4919;
    private static final String TAG = "RyomdNative";

    private FrameLayout rootView;
    private WebView webView;
    private WebChromeClient chromeClient;
    private View customView;
    private WebChromeClient.CustomViewCallback customViewCallback;
    private volatile long lastProbeAt;
    private volatile boolean lastProbeUp;
    private ValueCallback<Uri[]> pendingFileCallback;
    private RyomdMediaSaver mediaSaver;
    private com.ryomd.nativeapp.youtube.RyomdYouTubeAccountBridge youtubeAccountBridge;
    private com.ryomd.nativeapp.youtube.RyomdYouTubeCatalogBridge youtubeCatalogBridge;
    private PendingMediaDownload pendingMediaDownload;
    private static final int MEDIA_STORAGE_PERMISSION_REQUEST = 4918;

    private static final class PendingMediaDownload {
        final String url;
        final String userAgent;
        final String contentDisposition;
        final String mimeType;
        final String referer;

        PendingMediaDownload(String url, String userAgent, String contentDisposition,
                             String mimeType, String referer) {
            this.url = url;
            this.userAgent = userAgent;
            this.contentDisposition = contentDisposition;
            this.mimeType = mimeType;
            this.referer = referer;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(android.graphics.Color.rgb(9, 7, 17));
        getWindow().setNavigationBarColor(android.graphics.Color.rgb(9, 7, 17));

        rootView = new FrameLayout(this);
        rootView.setBackgroundColor(android.graphics.Color.rgb(9, 7, 17));
        webView = new WebView(this);
        webView.setBackgroundColor(android.graphics.Color.rgb(9, 7, 17));
        rootView.addView(webView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        setContentView(rootView);
        applyWindowInsets();
        mediaSaver = new RyomdMediaSaver(getApplicationContext());
        youtubeAccountBridge = new com.ryomd.nativeapp.youtube.RyomdYouTubeAccountBridge(this, webView);
        youtubeCatalogBridge = new com.ryomd.nativeapp.youtube.RyomdYouTubeCatalogBridge(this, webView);

        configureWebView();
        webView.loadUrl(START_URL);
        requestNotificationPermissionOnce();
    }

    /**
     * Android 15 (targetSdk 35) memaksa edge-to-edge: setStatusBarColor() diabaikan dan WebView
     * tergambar di bawah status bar/navigation bar sehingga topbar panel tertutup. Beri padding
     * sesuai inset sistem (dan keyboard) agar konten tidak tertimpa.
     */
    private void applyWindowInsets() {
        if (Build.VERSION.SDK_INT < 35) return;
        rootView.setOnApplyWindowInsetsListener((v, insets) -> {
            Insets bars = insets.getInsets(
                    WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            Insets ime = insets.getInsets(WindowInsets.Type.ime());
            v.setPadding(bars.left, bars.top, bars.right, Math.max(bars.bottom, ime.bottom));
            return WindowInsets.CONSUMED;
        });
        rootView.requestApplyInsets();
    }

    private void requestNotificationPermissionOnce() {
        if (Build.VERSION.SDK_INT < 33) return;
        if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                == android.content.pm.PackageManager.PERMISSION_GRANTED) return;
        SharedPreferences prefs = getSharedPreferences("ryomd_native", MODE_PRIVATE);
        if (prefs.getBoolean("notification_permission_asked", false)) return;
        prefs.edit().putBoolean("notification_permission_asked", true).apply();
        requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                NOTIFICATION_PERMISSION_REQUEST);
    }

    // ---- Sajian UI dari APK -------------------------------------------------------------

    private boolean backendReachable() {
        long now = SystemClock.elapsedRealtime();
        if (now - lastProbeAt < 2500) return lastProbeUp;
        boolean up = false;
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(BACKEND_HOST, BACKEND_PORT), 400);
            up = true;
        } catch (IOException ignored) { }
        lastProbeUp = up;
        lastProbeAt = now;
        return up;
    }

    private static String mimeForPath(String path) {
        String p = path.toLowerCase(Locale.ROOT);
        if (p.endsWith(".html") || p.endsWith(".htm")) return "text/html";
        if (p.endsWith(".js") || p.endsWith(".mjs")) return "application/javascript";
        if (p.endsWith(".css")) return "text/css";
        if (p.endsWith(".json")) return "application/json";
        if (p.endsWith(".svg")) return "image/svg+xml";
        if (p.endsWith(".png")) return "image/png";
        if (p.endsWith(".jpg") || p.endsWith(".jpeg")) return "image/jpeg";
        if (p.endsWith(".webp")) return "image/webp";
        if (p.endsWith(".gif")) return "image/gif";
        if (p.endsWith(".mp4")) return "video/mp4";
        if (p.endsWith(".woff2")) return "font/woff2";
        if (p.endsWith(".txt")) return "text/plain";
        return "application/octet-stream";
    }

    private WebResourceResponse interceptBundled(WebResourceRequest request) {
        if (!USE_BUNDLED_UI || request == null) return null;
        if (!"GET".equalsIgnoreCase(request.getMethod())) return null;
        Uri uri = request.getUrl();
        if (!isBackendUrl(uri)) return null;

        String path = uri.getPath();
        if (path == null || path.isEmpty() || "/".equals(path)) path = "/index.html";
        if (path.startsWith("/api/") || path.contains("..")) return null;

        // Media yang dapat diganti pengguna lewat Media Studio (banner/avatar) tetap diambil dari
        // server selama server hidup; bila server mati, pakai salinan bawaan APK.
        if (path.startsWith("/assets/") && backendReachable()) return null;

        try {
            InputStream stream = getAssets().open("ryomd" + path);
            String mime = mimeForPath(path);
            boolean text = mime.startsWith("text/") || mime.equals("application/javascript")
                    || mime.equals("application/json") || mime.equals("image/svg+xml");
            Map<String, String> headers = new HashMap<>();
            headers.put("Cache-Control", "no-cache");
            return new WebResourceResponse(mime, text ? "UTF-8" : null, 200, "OK", headers, stream);
        } catch (IOException notBundled) {
            return null; // tidak ada di APK -> biarkan WebView meminta ke server Termux
        }
    }

    private void setImmersiveMode(boolean on) {
        if (Build.VERSION.SDK_INT >= 30) {
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller == null) return;
            if (on) {
                controller.hide(WindowInsets.Type.systemBars());
                controller.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            } else {
                controller.show(WindowInsets.Type.systemBars());
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(on
                    ? View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    : View.SYSTEM_UI_FLAG_VISIBLE);
        }
    }

    private void configureWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setSupportMultipleWindows(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setSafeBrowsingEnabled(true);
        settings.setGeolocationEnabled(false);
        settings.setMediaPlaybackRequiresUserGesture(false);

        webView.addJavascriptInterface(new RyomdNativeBridge(this, mediaSaver, webView, START_URL), "RyomdNative");
        webView.addJavascriptInterface(new com.ryomd.nativeapp.youtube.RyomdYouTubeBridge(this, webView), "RyomdYouTube");
        webView.addJavascriptInterface(youtubeAccountBridge, "RyomdYouTubeAccount");
        webView.addJavascriptInterface(youtubeCatalogBridge, "RyomdYouTubeCatalog");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
            }
            @Override
            public boolean shouldOverrideUrlLoading(
                    WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                String scheme = uri.getScheme() == null ? "" : uri.getScheme();

                if ("http".equalsIgnoreCase(scheme)
                        && isLoopbackHost(uri.getHost())) {
                    return false;
                }

                // Keep HTTPS subframes (for embeds) inside WebView, but open
                // top-level external sites in the user's browser. This avoids
                // exposing the JavaScript bridge to unrelated top-level sites.
                if ("https".equalsIgnoreCase(scheme) && !request.isForMainFrame()) {
                    return false;
                }
                if ("https".equalsIgnoreCase(scheme) && request.isForMainFrame()) {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, uri));
                    } catch (ActivityNotFoundException ignored) {
                        Toast.makeText(MainActivity.this,
                                "Tidak ada browser untuk membuka tautan",
                                Toast.LENGTH_SHORT).show();
                    }
                    return true;
                }

                // Block non-web schemes and non-loopback top-level HTTP links.
                return true;
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(
                    WebView view, WebResourceRequest request) {
                WebResourceResponse bundled = interceptBundled(request);
                return bundled != null ? bundled : super.shouldInterceptRequest(view, request);
            }

            @Override
            public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
                // Hindari crash seluruh aplikasi ketika proses render WebView dimatikan sistem.
                Log.w(TAG, "WebView render process gone; reloading");
                if (view != null && webView == view) {
                    ((FrameLayout) view.getParent()).removeView(view);
                    view.destroy();
                    webView = null;
                }
                recreate();
                return true;
            }

            @Override
            public void onReceivedError(
                    WebView view,
                    WebResourceRequest request,
                    WebResourceError error) {
                super.onReceivedError(view, request, error);
                if (request.isForMainFrame() && isBackendUrl(request.getUrl())) {
                    showBackendUnavailablePage();
                }
            }
        });

        chromeClient = new WebChromeClient() {
            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                if (customView != null) { callback.onCustomViewHidden(); return; }
                customView = view;
                customViewCallback = callback;
                rootView.addView(view, new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
                if (webView != null) webView.setVisibility(View.GONE);
                setImmersiveMode(true);
            }

            @Override
            public void onHideCustomView() {
                if (customView == null) return;
                rootView.removeView(customView);
                customView = null;
                if (customViewCallback != null) customViewCallback.onCustomViewHidden();
                customViewCallback = null;
                if (webView != null) webView.setVisibility(View.VISIBLE);
                setImmersiveMode(false);
            }

            @Override
            public boolean onConsoleMessage(ConsoleMessage message) {
                Log.d(TAG, "js: " + message.message() + " (" + message.sourceId() + ":"
                        + message.lineNumber() + ")");
                return true;
            }

            @Override
            public boolean onShowFileChooser(
                    WebView view,
                    ValueCallback<Uri[]> filePathCallback,
                    FileChooserParams fileChooserParams) {
                if (pendingFileCallback != null) {
                    pendingFileCallback.onReceiveValue(null);
                }
                pendingFileCallback = filePathCallback;

                Intent intent = fileChooserParams.createIntent();
                intent.setType("*/*");
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                try {
                    startActivityForResult(intent, FILE_PICKER_REQUEST);
                    return true;
                } catch (ActivityNotFoundException e) {
                    pendingFileCallback = null;
                    Toast.makeText(
                            MainActivity.this,
                            "Tidak ada aplikasi pemilih file",
                            Toast.LENGTH_SHORT
                    ).show();
                    return false;
                }
            }
        };
        webView.setWebChromeClient(chromeClient);

        // WebView does not save <a download> links by itself. Route those
        // clicks through MediaStore without changing the existing web UI/API.
        webView.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> {
            String referer = webView == null ? START_URL : webView.getUrl();
            requestMediaDownload(url, userAgent, contentDisposition, mimeType, referer);
        });
    }

    private void requestMediaDownload(String url, String userAgent,
                                      String contentDisposition, String mimeType,
                                      String referer) {
        if (url == null || url.trim().isEmpty()) {
            Toast.makeText(this, "Tautan unduhan tidak valid.", Toast.LENGTH_SHORT).show();
            return;
        }

        PendingMediaDownload request = new PendingMediaDownload(
                url, userAgent, contentDisposition, mimeType, referer
        );

        // Android 9 and older need the legacy public-storage permission.
        // Android 10+ uses scoped MediaStore and does not request broad storage access.
        if (android.os.Build.VERSION.SDK_INT <= 28
                && checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            pendingMediaDownload = request;
            requestPermissions(
                    new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    MEDIA_STORAGE_PERMISSION_REQUEST
            );
            return;
        }
        startMediaDownload(request);
    }

    private void startMediaDownload(PendingMediaDownload request) {
        if (mediaSaver == null) mediaSaver = new RyomdMediaSaver(getApplicationContext());
        mediaSaver.download(
                request.url,
                request.userAgent,
                request.contentDisposition,
                request.mimeType,
                request.referer
        );
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != MEDIA_STORAGE_PERMISSION_REQUEST) return;

        PendingMediaDownload request = pendingMediaDownload;
        pendingMediaDownload = null;
        if (request == null) return;

        if (grantResults.length > 0
                && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            startMediaDownload(request);
        } else {
            Toast.makeText(this,
                    "Izin penyimpanan ditolak. Unduhan tidak disimpan.",
                    Toast.LENGTH_LONG).show();
        }
    }

    private boolean isBackendUrl(Uri uri) {
        return uri != null
                && "http".equalsIgnoreCase(uri.getScheme())
                && BACKEND_HOST.equalsIgnoreCase(uri.getHost())
                && uri.getPort() == BACKEND_PORT;
    }

    private boolean isLoopbackHost(String host) {
        return host != null
                && (BACKEND_HOST.equalsIgnoreCase(host)
                || "localhost".equalsIgnoreCase(host)
                || "[::1]".equalsIgnoreCase(host)
                || "::1".equalsIgnoreCase(host));
    }

    private void showBackendUnavailablePage() {
        if (webView == null || isFinishing()) return;

        String html =
                "<!doctype html><html lang='id'><meta charset='utf-8'>" +
                "<meta name='viewport' content='width=device-width,initial-scale=1'>" +
                "<title>Ryomd belum terhubung</title>" +
                "<style>" +
                "body{margin:0;min-height:100vh;display:grid;place-items:center;" +
                "background:#090711;color:#f5f1ff;font:16px system-ui;padding:24px;box-sizing:border-box}" +
                "main{max-width:440px;text-align:center;background:#171225;padding:28px;" +
                "border:1px solid #3b2b55;border-radius:20px}" +
                "h1{font-size:24px;margin:0 0 12px}p{color:#c5badb;line-height:1.6}" +
                "code{color:#d7c1ff}a{display:inline-block;margin-top:12px;padding:12px 20px;" +
                "border-radius:12px;background:#8b5cf6;color:white;text-decoration:none;font-weight:700}" +
                "</style><main><h1>Ryomd belum terhubung</h1>" +
                "<p>Backend Termux belum merespons pada <code>127.0.0.1:3002</code>.</p>" +
                "<p>Buka Termux, jalankan server Ryomd, lalu coba lagi.</p>" +
                "<a href='" + START_URL + "'>Coba hubungkan kembali</a></main></html>";

        webView.loadDataWithBaseURL(
                START_URL, html, "text/html", "UTF-8", null
        );
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (youtubeAccountBridge != null && youtubeAccountBridge.handleActivityResult(requestCode, resultCode, data)) return;
        if (requestCode != FILE_PICKER_REQUEST) return;

        Uri[] results = null;
        if (resultCode == RESULT_OK && data != null) {
            if (data.getClipData() != null) {
                int count = data.getClipData().getItemCount();
                results = new Uri[count];
                for (int i = 0; i < count; i++) {
                    results[i] = data.getClipData().getItemAt(i).getUri();
                }
            } else if (data.getData() != null) {
                results = new Uri[]{data.getData()};
            }
        }

        if (pendingFileCallback != null) {
            pendingFileCallback.onReceiveValue(results);
            pendingFileCallback = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) webView.onResume();
    }

    @Override
    protected void onPause() {
        if (webView != null) webView.onPause();
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        if (customView != null && chromeClient != null) {
            chromeClient.onHideCustomView();
        } else if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (customView != null && chromeClient != null) chromeClient.onHideCustomView();
        if (youtubeAccountBridge != null) youtubeAccountBridge.shutdown();
        if (youtubeCatalogBridge != null) youtubeCatalogBridge.shutdown();
        if (pendingFileCallback != null) {
            pendingFileCallback.onReceiveValue(null);
            pendingFileCallback = null;
        }
        if (webView != null) {
            webView.stopLoading();
            webView.removeJavascriptInterface("RyomdNative");
            webView.removeJavascriptInterface("RyomdYouTube");
            webView.removeJavascriptInterface("RyomdYouTubeAccount");
            webView.removeJavascriptInterface("RyomdYouTubeCatalog");
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}
