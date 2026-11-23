package com.ryomd.nativeapp;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

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

    private WebView webView;
    private ValueCallback<Uri[]> pendingFileCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(android.graphics.Color.rgb(9, 7, 17));
        getWindow().setNavigationBarColor(android.graphics.Color.rgb(9, 7, 17));

        webView = new WebView(this);
        webView.setBackgroundColor(android.graphics.Color.rgb(9, 7, 17));
        setContentView(webView);

        configureWebView();
        webView.loadUrl(START_URL);
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

        webView.addJavascriptInterface(new RyomdNativeBridge(this), "RyomdNative");

        webView.setWebViewClient(new WebViewClient() {
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

        webView.setWebChromeClient(new WebChromeClient() {
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
        });
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
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (pendingFileCallback != null) {
            pendingFileCallback.onReceiveValue(null);
            pendingFileCallback = null;
        }
        if (webView != null) {
            webView.stopLoading();
            webView.removeJavascriptInterface("RyomdNative");
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}
