package com.studio.offline;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.webkit.WebViewAssetLoader;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    // Only the offline notice is bundled. The studio itself is loaded from the online URL.
    private static final String ASSET_HOST = "appassets.androidplatform.net";
    private static final String OFFLINE_PAGE = "https://" + ASSET_HOST + "/assets/offline.html";
    // Used only when studio_url is not configured yet, so the app still opens and works.
    private static final String BUNDLED_STUDIO = "https://" + ASSET_HOST + "/assets/index.html";

    private WebView webView;
    private NativeBridge bridge;
    private String studioUrl;
    private String studioHost;
    private boolean studioConfigured;
    private ValueCallback<Uri[]> filePathCallback;
    private PermissionRequest pendingWebPermission;
    private String pendingSaveId;

    private final ActivityResultLauncher<Intent> chooserLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (filePathCallback != null) {
                    Uri[] uris = WebChromeClient.FileChooserParams.parseResult(
                            result.getResultCode(), result.getData());
                    filePathCallback.onReceiveValue(uris);
                    filePathCallback = null;
                }
            });

    private final ActivityResultLauncher<Intent> saveLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                String id = pendingSaveId;
                pendingSaveId = null;
                if (id == null) return;
                Uri uri = (result.getResultCode() == RESULT_OK && result.getData() != null)
                        ? result.getData().getData() : null;
                if (uri == null) {
                    notifySave(id, "cancel");
                    return;
                }
                try {
                    OutputStream os = getContentResolver().openOutputStream(uri, "w");
                    if (os == null) throw new IOException("no output stream");
                    bridge.attachStream(id, os);
                    notifySave(id, "ok");
                } catch (Exception e) {
                    notifySave(id, "error");
                    toast("تعذّر الحفظ في المسار المختار");
                }
            });

    private final ActivityResultLauncher<String[]> permissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            granted -> {
                PermissionRequest request = pendingWebPermission;
                pendingWebPermission = null;
                if (request == null) return;
                if (!granted.containsValue(false)) {
                    request.grant(request.getResources());
                } else {
                    request.deny();
                    toast("يجب السماح بالكاميرا والمايك لاستخدام التصوير والتسجيل");
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        webView = findViewById(R.id.webview);

        studioUrl = getString(R.string.studio_url).trim();
        Uri parsed = Uri.parse(studioUrl);
        studioHost = parsed.getHost();
        studioConfigured = "https".equals(parsed.getScheme())
                && studioHost != null && !studioHost.isEmpty()
                && !studioHost.startsWith("YOUR-SITE");

        bridge = new NativeBridge(this);
        setupWebView();
        loadStudio();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);          // localStorage
        s.setDatabaseEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        s.setSupportZoom(false);
        s.setBuiltInZoomControls(false);
        s.setUseWideViewPort(true);
        s.setLoadWithOverviewMode(true);

        WebViewAssetLoader loader = new WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .build();

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                return loader.shouldInterceptRequest(request.getUrl());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                String host = uri.getHost();
                if (host != null && (host.equals(studioHost) || host.equals(ASSET_HOST))) {
                    return false; // stays inside the app
                }
                // Any other site opens in the phone's browser, so the save bridge is never exposed to it.
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, uri));
                } catch (ActivityNotFoundException e) {
                    toast("لا يوجد تطبيق لفتح هذا الرابط");
                }
                return true;
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame() && studioConfigured
                        && !request.getUrl().toString().startsWith(OFFLINE_PAGE)) {
                    view.loadUrl(OFFLINE_PAGE);
                }
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(PermissionRequest request) {
                handleWebPermission(request);
            }

            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback,
                                             FileChooserParams params) {
                if (filePathCallback != null) filePathCallback.onReceiveValue(null);
                filePathCallback = callback;
                try {
                    chooserLauncher.launch(params.createIntent());
                } catch (ActivityNotFoundException e) {
                    filePathCallback = null;
                    toast("لا يوجد تطبيق لاختيار الملفات");
                    return false;
                }
                return true;
            }
        });

        webView.addJavascriptInterface(bridge, "AndroidStudio");
    }

    /** Loads the studio, or a setup notice when the URL has not been configured yet. */
    private void loadStudio() {
        webView.loadUrl(studioConfigured ? studioUrl : BUNDLED_STUDIO);
    }

    /** Called from the offline page's retry button through NativeBridge. */
    void reloadStudio() {
        runOnUiThread(this::loadStudio);
    }

    private void handleWebPermission(PermissionRequest request) {
        List<String> needed = new ArrayList<>();
        for (String resource : request.getResources()) {
            if (PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(resource) && !has(Manifest.permission.CAMERA)) {
                needed.add(Manifest.permission.CAMERA);
            }
            if (PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(resource) && !has(Manifest.permission.RECORD_AUDIO)) {
                needed.add(Manifest.permission.RECORD_AUDIO);
            }
        }
        if (needed.isEmpty()) {
            request.grant(request.getResources());
            return;
        }
        pendingWebPermission = request;
        permissionLauncher.launch(needed.toArray(new String[0]));
    }

    private boolean has(String permission) {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED;
    }

    /** Called by NativeBridge on the UI thread: opens the system "save as" screen so the user picks the folder. */
    void startSavePicker(String id, String name, String mime) {
        pendingSaveId = id;
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(mime == null || mime.isEmpty() ? "application/octet-stream" : mime);
        intent.putExtra(Intent.EXTRA_TITLE, name);
        try {
            saveLauncher.launch(intent);
        } catch (ActivityNotFoundException e) {
            pendingSaveId = null;
            notifySave(id, "error");
            toast("لا يوجد تطبيق لاختيار مكان الحفظ");
        }
    }

    void notifySave(String id, String status) {
        runOnUiThread(() -> webView.evaluateJavascript(
                "window.__studioSaveReady&&window.__studioSaveReady('" + id + "','" + status + "')", null));
    }

    void toast(String message) {
        runOnUiThread(() -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show());
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
        if (bridge != null) bridge.closeAll();
        if (webView != null) webView.destroy();
        super.onDestroy();
    }
}
