package com.juma.ai;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.ConsoleMessage;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends Activity {
    private WebView webView;
    private JumaBridge bridge;
    private ValueCallback<Uri[]> filePathCallback;
    private static final int FILE_CHOOSER_REQUEST_CODE = 2001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Immersive Dark Status & Navigation Bar
        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.setStatusBarColor(Color.parseColor("#030712"));
            window.setNavigationBarColor(Color.parseColor("#030712"));
        }

        webView = new WebView(this);
        setContentView(webView);
        webView.setBackgroundColor(Color.parseColor("#030712"));

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        // Cache configuration for seamless offline / online operation
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }

        // Initialize Native Bridge
        bridge = new JumaBridge(this, webView);
        webView.addJavascriptInterface(bridge, "AndroidBridge");
        webView.addJavascriptInterface(bridge, "Android");
        webView.addJavascriptInterface(bridge, "jumaNative");

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            request.grant(request.getResources());
                        }
                    }
                });
            }

            @Override
            public boolean onShowFileChooser(
                WebView webView,
                ValueCallback<Uri[]> filePathCallback,
                WebChromeClient.FileChooserParams fileChooserParams
            ) {
                if (MainActivity.this.filePathCallback != null) {
                    MainActivity.this.filePathCallback.onReceiveValue(null);
                }
                MainActivity.this.filePathCallback = filePathCallback;

                try {
                    Intent intent = null;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        intent = fileChooserParams.createIntent();
                    } else {
                        intent = new Intent(Intent.ACTION_GET_CONTENT);
                        intent.setType("*/*");
                    }
                    startActivityForResult(intent, FILE_CHOOSER_REQUEST_CODE);
                    return true;
                } catch (Exception e) {
                    MainActivity.this.filePathCallback = null;
                    return false;
                }
            }

            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                return super.onConsoleMessage(consoleMessage);
            }
        });

        final String localFallbackUrl = "file:///android_asset/dist/index.html";
        String remoteUrl = null;
        try {
            remoteUrl = getString(R.string.remote_web_url);
        } catch (Exception ignored) {}

        final String targetRemoteUrl = remoteUrl;

        webView.setWebViewClient(new WebViewClient() {
            private boolean hasError = false;

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url == null) return false;

                if (url.startsWith("tel:") || url.startsWith("sms:") || url.startsWith("mailto:") ||
                    url.startsWith("whatsapp:") || url.startsWith("market:") || url.startsWith("tg:")) {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        return true;
                    } catch (Exception ignored) {}
                }

                if (url.startsWith("intent:#Intent;")) {
                    try {
                        Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                        if (intent != null) {
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                            return true;
                        }
                    } catch (Exception ignored) {}
                }

                return false;
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                Log.w("JumaMainActivity", "Web loading error: " + description + " for " + failingUrl);
                if (failingUrl != null && !failingUrl.startsWith("file://") && !hasError) {
                    hasError = true;
                    view.loadUrl(localFallbackUrl);
                }
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && request.isForMainFrame()) {
                    String failingUrl = request.getUrl().toString();
                    Log.w("JumaMainActivity", "Mainframe error: " + error.getDescription() + " for " + failingUrl);
                    if (!failingUrl.startsWith("file://") && !hasError) {
                        hasError = true;
                        view.loadUrl(localFallbackUrl);
                    }
                }
            }
        });

        // Request initial baseline permissions if missing (including Android 13+ POST_NOTIFICATIONS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(new String[]{
                android.Manifest.permission.RECORD_AUDIO,
                android.Manifest.permission.CAMERA,
                android.Manifest.permission.POST_NOTIFICATIONS
            }, 100);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{
                android.Manifest.permission.RECORD_AUDIO,
                android.Manifest.permission.CAMERA
            }, 100);
        }

        // Check network availability to load latest remote web layer or fallback to local bundle
        if (isNetworkAvailable() && targetRemoteUrl != null && targetRemoteUrl.startsWith("http")) {
            Log.i("JumaMainActivity", "Loading remote live web layer: " + targetRemoteUrl);
            webView.loadUrl(targetRemoteUrl);
        } else {
            Log.i("JumaMainActivity", "Offline or local mode. Loading local asset bundle: " + localFallbackUrl);
            webView.loadUrl(localFallbackUrl);
        }
    }

    private boolean isNetworkAvailable() {
        try {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm != null) {
                NetworkInfo netInfo = cm.getActiveNetworkInfo();
                return netInfo != null && netInfo.isConnected();
            }
        } catch (Exception ignored) {}
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (permissions != null && grantResults != null) {
            for (int i = 0; i < permissions.length; i++) {
                final String perm = permissions[i];
                final boolean granted = grantResults[i] == PackageManager.PERMISSION_GRANTED;

                String shortKey = perm;
                if (android.Manifest.permission.RECORD_AUDIO.equals(perm)) {
                    shortKey = "microphone";
                } else if (android.Manifest.permission.CAMERA.equals(perm)) {
                    shortKey = "camera";
                } else if (android.Manifest.permission.CALL_PHONE.equals(perm)) {
                    shortKey = "phone";
                } else if (android.Manifest.permission.SEND_SMS.equals(perm)) {
                    shortKey = "sms";
                } else if (android.Manifest.permission.READ_CONTACTS.equals(perm)) {
                    shortKey = "contacts";
                } else if (android.Manifest.permission.ACCESS_FINE_LOCATION.equals(perm)) {
                    shortKey = "location";
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                           android.Manifest.permission.POST_NOTIFICATIONS.equals(perm)) {
                    shortKey = "notifications";
                }

                final String finalKey = shortKey;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String js = "if (window.onAndroidPermissionResult) { " +
                                    "window.onAndroidPermissionResult('" + perm + "', " + granted + "); " +
                                    "window.onAndroidPermissionResult('" + finalKey + "', " + granted + "); " +
                                    "}";
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                            webView.evaluateJavascript(js, null);
                        } else {
                            webView.loadUrl("javascript:" + js);
                        }
                    }
                });
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_CHOOSER_REQUEST_CODE) {
            if (filePathCallback != null) {
                Uri[] results = null;
                if (resultCode == RESULT_OK && data != null) {
                    if (data.getData() != null) {
                        results = new Uri[]{data.getData()};
                    } else if (data.getClipData() != null) {
                        int count = data.getClipData().getItemCount();
                        results = new Uri[count];
                        for (int i = 0; i < count; i++) {
                            results[i] = data.getClipData().getItemAt(i).getUri();
                        }
                    }
                }
                filePathCallback.onReceiveValue(results);
                filePathCallback = null;
            }
        }
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
        if (bridge != null) {
            bridge.onDestroy();
        }
        if (webView != null) {
            webView.destroy();
        }
        super.onDestroy();
    }
}
