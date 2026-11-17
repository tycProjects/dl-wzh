package com.web2apk.runtime;

import android.Manifest;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.Intent;
import android.database.Cursor;
import android.provider.CalendarContract;
import android.provider.ContactsContract;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.os.Vibrator;
import android.os.VibrationEffect;
import android.webkit.CookieManager;
import android.webkit.GeolocationPermissions;
import android.webkit.PermissionRequest;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.webkit.WebViewAssetLoader;
import androidx.webkit.WebViewCompat;
import androidx.webkit.WebViewFeature;
import androidx.webkit.WebMessageCompat;
import androidx.webkit.JavaScriptReplyProxy;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.VideoView;
import android.media.MediaPlayer;
import android.animation.ValueAnimator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.util.Base64;
import android.graphics.drawable.GradientDrawable;
import android.widget.Toast;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.text.InputType;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import javax.net.ssl.HttpsURLConnection;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/** WebView runtime embedded into every generated Website APK. */
public final class WebActivity extends Activity {
    private static final int REQ_FILE = 4101;
    private static final int REQ_MEDIA = 4102;
    private static final int REQ_LOCATION = 4103;
    private static final int REQ_NOTIFICATIONS = 4104;
    private static final int REQ_CONTACT_PICK = 4105;

    private WebView web;
    private ValueCallback<Uri[]> pendingFile;
    private PermissionRequest pendingMedia;
    private String[] pendingMediaPermissions;
    private String[] pendingMediaResources;
    private GeolocationPermissions.Callback pendingGeo;
    private String pendingGeoOrigin;
    private boolean creatorWatermark;
    private boolean allowPhoneDialer;
    private boolean allowVibration;
    private boolean allowContactPicker;
    private boolean allowAddContact;
    private boolean allowViewCalendar;
    private boolean allowCalendarInsert;
    private WebViewAssetLoader assetLoader;
    private String pendingContactCallback;
    private boolean enableAiApi;
    private boolean aiMessageListenerReady;
    private FrameLayout splashOverlay;
    private boolean splashDismissed;
    private String splashStyle = "ios_zoom";
    private String splashSpeed = "normal";
    private String splashMediaType = "";
    private String splashMediaFile = "";
    private String appOrientation = "system";
    private String backBehavior = "history";
    private String cacheMode = "default";
    private boolean fullscreen;
    private boolean pullToRefresh = true;
    private boolean externalLinksBrowser;
    private boolean allowZoom = true;
    private boolean desktopMode;
    private boolean downloadsEnabled = true;
    private boolean javascriptEnabled = true;
    private boolean domStorageEnabled = true;
    private boolean cookiesEnabled = true;
    private boolean fileUploadEnabled = true;
    private boolean performanceMonitorEnabled;
    private String userAgentMode = "system";
    private String customUserAgent = "";
    private String splashTitle = "Loading…";
    private int splashBackground = 0xFFF3F4F8;
    private boolean splashShowProgress = true;
    private long splashMinDurationMs;
    private long splashShownAt;
    private boolean splashDismissScheduled;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private long appStartTime;
    private long pageLoadStartedAt;
    private long lastPageLoadMs = -1L;
    private int loadErrorCount;
    private String lastLoadError = "None";
    private TextView performanceBadge;
    // Current top-level URL; used to prevent optional native bridge actions on off-origin pages.
    private volatile String currentMainFrameUrl = "";
    private float pullStartY;
    private final ExecutorService aiExecutor = Executors.newSingleThreadExecutor();
    private static final String AI_PREFS = "web2apk_ai_settings";
    private static final String AI_KEY_ALIAS = "web2apk_runtime_ai_key_v1";

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        appStartTime = SystemClock.elapsedRealtime();
        applyGeneratedWindowOptions();
        getWindow().setStatusBarColor(0xFFEEF0FF);
        getWindow().setNavigationBarColor(0xFFEEF0FF);
        getWindow().getDecorView().setSystemUiVisibility(
                android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                        | android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        if (fullscreen) applyImmersiveMode();

        FrameLayout root = new FrameLayout(this);
        web = new WebView(this);
        web.setBackgroundColor(0xFFFFFFFF);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(javascriptEnabled);
        s.setDomStorageEnabled(domStorageEnabled);
        s.setDatabaseEnabled(domStorageEnabled);
        s.setGeolocationEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setLoadsImagesAutomatically(true);
        s.setJavaScriptCanOpenWindowsAutomatically(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setSupportZoom(allowZoom);
        s.setBuiltInZoomControls(allowZoom);
        s.setDisplayZoomControls(false);
        if ("custom".equals(userAgentMode) && !customUserAgent.isEmpty()) {
            s.setUserAgentString(customUserAgent);
        } else if ("desktop".equals(userAgentMode) || desktopMode) {
            s.setUseWideViewPort(true);
            s.setLoadWithOverviewMode(true);
            String desktopUa = s.getUserAgentString();
            desktopUa = desktopUa.replaceAll("\\(Linux; Android [^)]+\\)", "(X11; Linux x86_64)")
                    .replace(" Mobile ", " ").replace("; wv)", ")");
            s.setUserAgentString(desktopUa);
        } else if ("mobile".equals(userAgentMode)) {
            s.setUserAgentString(s.getUserAgentString().replace("; wv)", ")"));
        }
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(cookiesEnabled);
        if (Build.VERSION.SDK_INT >= 21) cookieManager.setAcceptThirdPartyCookies(web, cookiesEnabled);
        if ("cache_first".equals(cacheMode)) s.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        else if ("offline".equals(cacheMode)) s.setCacheMode(WebSettings.LOAD_CACHE_ONLY);
        else if ("no_cache".equals(cacheMode)) { s.setCacheMode(WebSettings.LOAD_NO_CACHE); web.clearCache(true); }
        else s.setCacheMode(WebSettings.LOAD_DEFAULT);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        assetLoader = new WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .build();
        web.addJavascriptInterface(new RuntimeBridge(), "Web2ApkNative");
        setupAiMessageListener();
        web.setWebViewClient(new WebViewClient() {
            @Override public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                currentMainFrameUrl = url == null ? "" : url;
                pageLoadStartedAt = SystemClock.elapsedRealtime();
                super.onPageStarted(view, url, favicon);
            }
            @Override public void doUpdateVisitedHistory(WebView view, String url, boolean isReload) {
                currentMainFrameUrl = url == null ? "" : url;
                super.doUpdateVisitedHistory(view, url, isReload);
            }
            @Override public android.webkit.WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                return assetLoader.shouldInterceptRequest(request.getUrl());
            }
            @Override public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return handleExternalUrl(url);
            }
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return handleExternalUrl(request.getUrl().toString());
            }
            @Override public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (pageLoadStartedAt > 0L) lastPageLoadMs = SystemClock.elapsedRealtime() - pageLoadStartedAt;
                injectVibrationShim();
                injectAiHelper();
                dismissStartupLoading();
            }
            @Override public void onReceivedError(WebView view, WebResourceRequest request,
                                                  android.webkit.WebResourceError error) {
                super.onReceivedError(view, request, error);
                if (request != null && request.isForMainFrame()) {
                    loadErrorCount++;
                    lastLoadError = error == null ? "Main page load error" : String.valueOf(error.getDescription());
                    dismissStartupLoading();
                }
            }
            @Override public void onReceivedHttpError(WebView view, WebResourceRequest request,
                                                       android.webkit.WebResourceResponse response) {
                super.onReceivedHttpError(view, request, response);
                if (request != null && request.isForMainFrame() && response != null && response.getStatusCode() >= 400) {
                    loadErrorCount++;
                    lastLoadError = "HTTP " + response.getStatusCode();
                }
            }
        });
        web.setWebChromeClient(new RuntimeChrome());
        if (downloadsEnabled) {
            web.setDownloadListener((url, userAgent, contentDisposition, mimeType, length) ->
                    download(url, userAgent, contentDisposition, mimeType));
        }
        if (pullToRefresh) {
            web.setOnTouchListener((view, event) -> {
                if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                    pullStartY = event.getY();
                } else if (event.getAction() == android.view.MotionEvent.ACTION_UP) {
                    float distance = event.getY() - pullStartY;
                    if (distance > dp(88) && web.getScrollY() <= 0) {
                        Toast.makeText(WebActivity.this, "Refreshing…", Toast.LENGTH_SHORT).show();
                        web.reload();
                    }
                }
                return false;
            });
        }
        root.addView(web, new FrameLayout.LayoutParams(-1, -1));
        if (creatorWatermark) addCreatorWatermark(root);
        if (enableAiApi) addAiSettingsButton(root);
        if (performanceMonitorEnabled) addPerformanceMonitor(root);
        setContentView(root);
        showStartupLoading(root);

        String remote = readAssetText("site_url.txt");
        if (remote != null && (remote.startsWith("https://") || remote.startsWith("http://"))) {
            web.loadUrl(remote);
        } else {
            // Serve bundled HTML from a secure appassets origin so camera, microphone and
            // geolocation Web APIs can work for locally packaged sites when selected.
            web.loadUrl("https://appassets.androidplatform.net/assets/site/index.html");
        }

        if (Build.VERSION.SDK_INT >= 33 && isPermissionDeclared(Manifest.permission.POST_NOTIFICATIONS)
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_NOTIFICATIONS);
        }
    }

    private void applyGeneratedWindowOptions() {
        String config = readAssetText("web2apk_config.properties");
        if (config == null) return;
        if (configValue(config, "block_screenshots")) {
            getWindow().setFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE,
                    android.view.WindowManager.LayoutParams.FLAG_SECURE);
        }
        if (configValue(config, "keep_screen_awake")) {
            getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        creatorWatermark = configValue(config, "creator_watermark");
        allowPhoneDialer = configValue(config, "allow_phone_dialer");
        allowVibration = configValue(config, "allow_vibration");
        allowContactPicker = configValue(config, "allow_contact_picker");
        allowAddContact = configValue(config, "allow_add_contact");
        allowViewCalendar = configValue(config, "allow_view_calendar");
        allowCalendarInsert = configValue(config, "allow_calendar_insert");
        enableAiApi = configValue(config, "enable_ai_api");
        splashStyle = configText(config, "splash_style", "ios_zoom");
        splashSpeed = configText(config, "splash_speed", "normal");
        splashMediaType = configText(config, "splash_media_type", "");
        splashMediaFile = configText(config, "splash_media_file", "");
        appOrientation = configText(config, "app_orientation", "system");
        backBehavior = configText(config, "back_behavior", "history");
        cacheMode = configText(config, "cache_mode", "default");
        fullscreen = configValue(config, "fullscreen");
        pullToRefresh = !configHasKey(config, "pull_to_refresh") || configValue(config, "pull_to_refresh");
        externalLinksBrowser = configValue(config, "external_links_browser");
        allowZoom = !configHasKey(config, "allow_zoom") || configValue(config, "allow_zoom");
        desktopMode = configValue(config, "desktop_mode");
        downloadsEnabled = !configHasKey(config, "downloads_enabled") || configValue(config, "downloads_enabled");
        javascriptEnabled = !configHasKey(config, "javascript_enabled") || configValue(config, "javascript_enabled");
        domStorageEnabled = !configHasKey(config, "dom_storage_enabled") || configValue(config, "dom_storage_enabled");
        cookiesEnabled = !configHasKey(config, "cookies_enabled") || configValue(config, "cookies_enabled");
        fileUploadEnabled = !configHasKey(config, "file_upload_enabled") || configValue(config, "file_upload_enabled");
        performanceMonitorEnabled = configValue(config, "performance_monitor");
        userAgentMode = configText(config, "user_agent_mode", desktopMode ? "desktop" : "system");
        customUserAgent = decodeConfigBase64(configText(config, "custom_user_agent_b64", ""), "");
        splashBackground = parseConfigColor(configText(config, "splash_background", "#FFF3F4F8"), 0xFFF3F4F8);
        splashTitle = decodeConfigBase64(configText(config, "splash_title_b64", ""), "Loading…");
        splashShowProgress = !configHasKey(config, "splash_progress") || configValue(config, "splash_progress");
        try { splashMinDurationMs = Math.max(0L, Math.min(5000L, Long.parseLong(configText(config, "splash_min_duration_ms", "0")))); }
        catch (Exception ignored) { splashMinDurationMs = 0L; }
        if ("portrait".equals(appOrientation)) setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        else if ("landscape".equals(appOrientation)) setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        else setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    }

    private boolean configHasKey(String config, String key) {
        String prefix = key + "=";
        for (String line : config.split("\\n")) if (line.trim().startsWith(prefix)) return true;
        return false;
    }

    private void applyImmersiveMode() {
        getWindow().getDecorView().setSystemUiVisibility(
                android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                        | android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }

    private String configText(String config, String key, String fallback) {
        String prefix = key + "=";
        for (String line : config.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith(prefix)) return trimmed.substring(prefix.length()).trim();
        }
        return fallback;
    }

    private void showStartupLoading(FrameLayout root) {
        if ("off".equals(splashStyle)) return;
        splashShownAt = SystemClock.elapsedRealtime();
        splashOverlay = new FrameLayout(this);
        splashOverlay.setBackgroundColor(splashBackground);
        FrameLayout.LayoutParams full = new FrameLayout.LayoutParams(-1, -1);
        root.addView(splashOverlay, full);
        boolean customFallback = false;
        if ("custom".equals(splashStyle) && !splashMediaFile.isEmpty()) {
            try {
                if ("video".equals(splashMediaType)) {
                    File media = new File(getCacheDir(), splashMediaFile);
                    try (InputStream in = getAssets().open(splashMediaFile); OutputStream out = new FileOutputStream(media)) {
                        byte[] buffer = new byte[8192]; int n; while ((n = in.read(buffer)) != -1) out.write(buffer, 0, n);
                    }
                    VideoView video = new VideoView(this);
                    video.setVideoPath(media.getAbsolutePath());
                    video.setOnPreparedListener(player -> { player.setVolume(0f, 0f); player.setLooping(true); video.start(); });
                    splashOverlay.addView(video, full);
                } else {
                    ImageView image = new ImageView(this);
                    try (InputStream in = getAssets().open(splashMediaFile)) {
                        android.graphics.Bitmap bitmap = BitmapFactory.decodeStream(in);
                        if (bitmap == null) throw new IOException("Unsupported startup image format");
                        image.setImageBitmap(bitmap);
                    }
                    image.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    splashOverlay.addView(image, full);
                }
            } catch (Exception ignored) { addNativeSplashContent(); customFallback = true; }
        } else addNativeSplashContent();
        if ("custom".equals(splashStyle) && !customFallback && (!splashTitle.isEmpty() || splashShowProgress)) addSplashCaption();
        Animation enter;
        if ("ios_zoom".equals(splashStyle)) {
            enter = new ScaleAnimation(.88f, 1f, .88f, 1f, Animation.RELATIVE_TO_SELF, .5f, Animation.RELATIVE_TO_SELF, .5f);
        } else if ("pop".equals(splashStyle)) {
            enter = new ScaleAnimation(.72f, 1f, .72f, 1f, Animation.RELATIVE_TO_SELF, .5f, Animation.RELATIVE_TO_SELF, .5f);
        } else if ("slide".equals(splashStyle)) {
            enter = new TranslateAnimation(0, 0, dp(26), 0);
        } else enter = new AlphaAnimation(.25f, 1f);
        enter.setDuration(splashDuration()); enter.setInterpolator(new android.view.animation.DecelerateInterpolator());
        splashOverlay.startAnimation(enter);
    }

    private void addNativeSplashContent() {
        if (splashOverlay == null) return;
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER);
        ImageView icon = new ImageView(this);
        icon.setImageResource(R.mipmap.app_icon);
        int size = dp(76);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(size, size);
        iconParams.bottomMargin = dp(16);
        content.addView(icon, iconParams);
        TextView label = new TextView(this);
        label.setText(splashTitle.isEmpty() ? "Loading…" : splashTitle); label.setTextSize(13); label.setTextColor(0xFF687181);
        label.setGravity(Gravity.CENTER);
        content.addView(label, new LinearLayout.LayoutParams(-2, -2));
        if (splashShowProgress) {
            ProgressBar progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
            progress.setIndeterminate(true);
            LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(dp(144), dp(4));
            progressParams.topMargin = dp(18);
            content.addView(progress, progressParams);
        }
        splashOverlay.addView(content, new FrameLayout.LayoutParams(-1, -1));
    }

    private void addSplashCaption() {
        if (splashOverlay == null) return;
        LinearLayout caption = new LinearLayout(this);
        caption.setOrientation(LinearLayout.VERTICAL);
        caption.setGravity(Gravity.CENTER);
        caption.setPadding(dp(18), dp(10), dp(18), dp(18));
        TextView label = new TextView(this);
        label.setText(splashTitle);
        label.setTextColor(0xFF687181);
        label.setTextSize(13);
        label.setGravity(Gravity.CENTER);
        caption.addView(label, new LinearLayout.LayoutParams(-1, -2));
        if (splashShowProgress) {
            ProgressBar progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
            progress.setIndeterminate(true);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(150), dp(4));
            params.topMargin = dp(10);
            caption.addView(progress, params);
        }
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        splashOverlay.addView(caption, params);
    }

    private long splashDuration() {
        if (Build.VERSION.SDK_INT >= 26 && !ValueAnimator.areAnimatorsEnabled()) return 1L;
        if ("slow".equals(splashSpeed)) return 900L;
        if ("fast".equals(splashSpeed)) return 300L;
        return 560L;
    }

    private void dismissStartupLoading() {
        if (splashOverlay == null || splashDismissed) return;
        long elapsed = SystemClock.elapsedRealtime() - splashShownAt;
        long remaining = splashMinDurationMs - elapsed;
        if (remaining > 0L) {
            if (!splashDismissScheduled) {
                splashDismissScheduled = true;
                uiHandler.postDelayed(() -> {
                    splashDismissScheduled = false;
                    dismissStartupLoading();
                }, remaining);
            }
            return;
        }
        splashDismissed = true;
        Animation exit;
        if ("ios_zoom".equals(splashStyle)) {
            AnimationSet set = new AnimationSet(true);
            set.addAnimation(new ScaleAnimation(1f, 1.18f, 1f, 1.18f, Animation.RELATIVE_TO_SELF, .5f, Animation.RELATIVE_TO_SELF, .5f));
            set.addAnimation(new AlphaAnimation(1f, 0f)); exit = set;
        } else if ("slide".equals(splashStyle)) {
            exit = new TranslateAnimation(0, 0, 0, -getResources().getDisplayMetrics().heightPixels);
        } else if ("pop".equals(splashStyle)) {
            AnimationSet set = new AnimationSet(true);
            set.addAnimation(new ScaleAnimation(1f, 1.65f, 1f, 1.65f, Animation.RELATIVE_TO_SELF, .5f, Animation.RELATIVE_TO_SELF, .5f));
            set.addAnimation(new AlphaAnimation(1f, 0f)); exit = set;
        } else exit = new AlphaAnimation(1f, 0f);
        exit.setDuration(splashDuration()); exit.setFillAfter(true);
        exit.setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator());
        exit.setAnimationListener(new Animation.AnimationListener() {
            @Override public void onAnimationStart(Animation animation) { }
            @Override public void onAnimationRepeat(Animation animation) { }
            @Override public void onAnimationEnd(Animation animation) {
                if (splashOverlay != null && splashOverlay.getParent() instanceof FrameLayout) {
                    ((FrameLayout) splashOverlay.getParent()).removeView(splashOverlay);
                    splashOverlay = null;
                }
            }
        });
        splashOverlay.startAnimation(exit);
    }

    private void setupAiMessageListener() {
        aiMessageListenerReady = false;
        if (!enableAiApi || web == null) return;
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) return;
        Set<String> allowedOrigins = new HashSet<>();
        allowedOrigins.add("https://appassets.androidplatform.net");
        String remote = readAssetText("site_url.txt");
        if (remote != null && (remote.startsWith("https://") || remote.startsWith("http://"))) {
            String rule = toOriginRule(remote);
            if (rule != null) allowedOrigins.add(rule);
            try {
                Uri sourceUri = Uri.parse(remote);
                if ("http".equalsIgnoreCase(sourceUri.getScheme())
                        && (sourceUri.getPort() == -1 || sourceUri.getPort() == 80)
                        && sourceUri.getHost() != null) {
                    allowedOrigins.add("https://" + sourceUri.getHost().toLowerCase(java.util.Locale.ROOT));
                }
            } catch (Exception ignored) { }
        }
        try {
            WebViewCompat.addWebMessageListener(web, "Web2ApkAIHost", allowedOrigins,
                    (view, message, sourceOrigin, isMainFrame, replyProxy) -> {
                        // Never allow an iframe to spend the app owner's API key.
                        if (!isMainFrame || message == null
                                || message.getData() == null || message.getData().length() > 256 * 1024) return;
                        String requestId = "";
                        try {
                            JSONObject request = new JSONObject(message.getData());
                            requestId = request.optString("id", "");
                            if (!"chat".equals(request.optString("type"))
                                    || !requestId.matches("ai_[A-Za-z0-9_]{1,80}")) return;
                            JSONArray messages = request.optJSONArray("messages");
                            if (messages == null) throw new IllegalArgumentException("AI messages are missing");
                        } catch (Exception invalid) {
                            postAiReply(replyProxy, requestId, false, "Invalid AI request format", null, null);
                            return;
                        }
                        final String id = requestId;
                        final String messagesJson;
                        try { messagesJson = new JSONObject(message.getData()).getJSONArray("messages").toString(); }
                        catch (Exception invalid) {
                            postAiReply(replyProxy, id, false, "Invalid AI messages", null, null);
                            return;
                        }
                        aiExecutor.execute(() -> {
                            try {
                                JSONObject result = callAiChat(messagesJson);
                                postAiReply(replyProxy, id, true, null,
                                        result.optString("content", ""), result.optString("model", ""));
                            } catch (Exception error) {
                                String detail = error.getMessage() == null ? "AI API request failed" : error.getMessage();
                                postAiReply(replyProxy, id, false, detail, null, null);
                            }
                        });
                    });
            aiMessageListenerReady = true;
        } catch (Exception unsupported) {
            aiMessageListenerReady = false;
        }
    }

    private String toOriginRule(String raw) {
        try {
            Uri uri = Uri.parse(raw);
            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (scheme == null || host == null
                    || !("https".equalsIgnoreCase(scheme) || "http".equalsIgnoreCase(scheme))) return null;
            String rule = scheme.toLowerCase(java.util.Locale.ROOT) + "://" + host.toLowerCase(java.util.Locale.ROOT);
            if (uri.getPort() > 0) rule += ":" + uri.getPort();
            return rule;
        } catch (Exception ignored) { return null; }
    }

    private void postAiReply(JavaScriptReplyProxy replyProxy, String id, boolean ok,
                             String error, String content, String model) {
        if (replyProxy == null || id == null || !id.matches("ai_[A-Za-z0-9_]{1,80}")) return;
        try {
            JSONObject reply = new JSONObject();
            reply.put("id", id);
            reply.put("ok", ok);
            if (ok) {
                reply.put("content", content == null ? "" : content);
                reply.put("model", model == null ? "" : model);
            } else {
                String safe = error == null ? "AI API request failed" : error;
                reply.put("error", safe.length() > 500 ? safe.substring(0, 500) : safe);
            }
            String payload = reply.toString();
            runOnUiThread(() -> {
                try { replyProxy.postMessage(payload); } catch (Exception ignored) { }
            });
        } catch (Exception ignored) { }
    }

    private void addAiSettingsButton(FrameLayout root) {
        TextView button = new TextView(this);
        button.setText("✦  AI API");
        button.setTextColor(0xFFFFFFFF);
        button.setTextSize(12f);
        button.setTypeface(null, android.graphics.Typeface.BOLD);
        button.setPadding(dp(14), dp(10), dp(14), dp(10));
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{0xFF735CEB, 0xFF299FB5});
        bg.setCornerRadius(dp(22));
        bg.setStroke(dp(1), 0x66FFFFFF);
        button.setBackground(bg);
        button.setElevation(dp(8));
        button.setOnClickListener(v -> showAiSettingsDialog());
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM | Gravity.RIGHT);
        params.setMargins(dp(12), dp(12), dp(14), dp(42));
        root.addView(button, params);
    }

    private void injectAiHelper() {
        if (!enableAiApi || web == null) return;
        String helper = readAssetText("site/web2apk-ai.js");
        if (helper == null || helper.isEmpty()) return;
        String config = readAssetText("site/web2apk-ai-config.json");
        String configScript = "";
        if (config != null && !config.isEmpty()) {
            configScript = "window.__WEB2APK_AI_CONFIG__=JSON.parse(" + JSONObject.quote(config) + ");";
        }
        web.evaluateJavascript("(function(){" + configScript + "if(!window.Web2APKAI){\n" + helper
                .replace("\\", "\\\\").replace("`", "\\`") + "\n}})();", null);
    }

    private void showAiSettingsDialog() {
        if (!enableAiApi) return;
        if (!aiMessageListenerReady) {
            Toast.makeText(this, "AI API needs a newer Android System WebView with Web Message Listener support", Toast.LENGTH_LONG).show();
            return;
        }
        android.content.SharedPreferences prefs = getSharedPreferences(AI_PREFS, MODE_PRIVATE);
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(18); panel.setPadding(pad, dp(8), pad, 0);
        EditText endpoint = new EditText(this);
        endpoint.setSingleLine(true);
        endpoint.setHint("Gemini: https://generativelanguage.googleapis.com/v1beta/interactions");
        String bundledEndpoint = "https://generativelanguage.googleapis.com/v1beta/interactions";
        String bundledModel = "";
        try {
            JSONObject bundled = new JSONObject(readAssetText("site/web2apk-ai-config.json"));
            bundledEndpoint = bundled.optString("endpoint", bundledEndpoint);
            bundledModel = bundled.optString("model", bundledModel);
        } catch (Exception ignored) { }
        endpoint.setText(prefs.getString("endpoint", bundledEndpoint));
        EditText model = new EditText(this);
        model.setSingleLine(true); model.setHint("Model name (e.g. gemini-3.8-flash)");
        model.setText(prefs.getString("model", bundledModel));
        EditText key = new EditText(this);
        key.setSingleLine(true); key.setHint("API key (kept on this device)");
        key.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        key.setText("");
        panel.addView(endpoint); panel.addView(model); panel.addView(key);
        new android.app.AlertDialog.Builder(this)
                .setTitle("AI API settings")
                .setMessage(hasBundledAiKey()
                        ? "This APK contains a preconfigured API key from the builder. Anyone who receives the APK may extract and use that key. You can override it here with a different key, which is encrypted with Android Keystore on this device. API usage may cost money."
                        : "Gemini Interactions API is supported by default. OpenAI-compatible Chat Completions endpoints are also supported. Keys entered here are encrypted with Android Keystore on this device and are not copied into the APK. API usage may cost money.")
                .setView(panel)
                .setNegativeButton("Cancel", null)
                .setNeutralButton("Clear saved key", (d, w) -> {
                    prefs.edit().remove("api_key_enc").apply();
                    String message = hasBundledAiKey()
                            ? "Saved override key removed. This APK still contains its bundled key; rebuild without embedding to remove it."
                            : "Saved API key removed from this device.";
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                })
                .setPositiveButton("Save", (d, w) -> {
                    String url = endpoint.getText().toString().trim();
                    String modelName = model.getText().toString().trim();
                    String apiKey = key.getText().toString().trim();
                    try {
                        validateAiEndpoint(url);
                        if (modelName.isEmpty() || modelName.length() > 120) throw new IllegalArgumentException("Enter a valid model name");
                        android.content.SharedPreferences.Editor edit = prefs.edit().putString("endpoint", normalizeAiEndpoint(url)).putString("model", modelName);
                        if (!apiKey.isEmpty()) edit.putString("api_key_enc", encryptAiKey(apiKey));
                        edit.apply();
                        Toast.makeText(this, "AI API settings saved on this device", Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        Toast.makeText(this, "AI settings not saved: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }).show();
    }

    private boolean hasBundledAiKey() {
        try { return !new JSONObject(readAssetText("site/web2apk-ai-config.json")).optString("apiKey", "").isEmpty(); }
        catch (Exception ignored) { return false; }
    }

    private void validateAiEndpoint(String raw) {
        Uri uri = Uri.parse(raw == null ? "" : raw.trim());
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) {
            throw new IllegalArgumentException("Use a valid HTTPS API endpoint");
        }
        String path = uri.getPath() == null ? "" : uri.getPath();
        if (!(path.endsWith("/chat/completions") || path.endsWith("/interactions")
                || path.endsWith("/v1beta") || path.endsWith("/v1") || path.isEmpty())) {
            throw new IllegalArgumentException("Use Gemini /v1beta/interactions or an OpenAI-compatible /chat/completions URL");
        }
    }

    private String normalizeAiEndpoint(String raw) {
        String value = raw.trim().replaceAll("/+$", "");
        if (value.endsWith("/chat/completions") || value.endsWith("/interactions")) return value;
        if (value.endsWith("/v1beta")) return value + "/interactions";
        return value + "/chat/completions";
    }

    private SecretKey getAiEncryptionKey() throws Exception {
        KeyStore store = KeyStore.getInstance("AndroidKeyStore");
        store.load(null);
        if (!store.containsAlias(AI_KEY_ALIAS)) {
            KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
            KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(AI_KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(true)
                    .build();
            generator.init(spec); generator.generateKey(); store.load(null);
        }
        return (SecretKey) store.getKey(AI_KEY_ALIAS, null);
    }

    private String encryptAiKey(String plain) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, getAiEncryptionKey());
        byte[] encrypted = cipher.doFinal(plain.getBytes("UTF-8"));
        byte[] iv = cipher.getIV();
        byte[] joined = new byte[iv.length + encrypted.length];
        System.arraycopy(iv, 0, joined, 0, iv.length);
        System.arraycopy(encrypted, 0, joined, iv.length, encrypted.length);
        return android.util.Base64.encodeToString(joined, android.util.Base64.NO_WRAP);
    }

    private String decryptAiKey(String encoded) throws Exception {
        byte[] all = android.util.Base64.decode(encoded, android.util.Base64.DEFAULT);
        if (all.length <= 12) throw new java.io.IOException("Stored API key is invalid; enter it again");
        byte[] iv = java.util.Arrays.copyOfRange(all, 0, 12);
        byte[] body = java.util.Arrays.copyOfRange(all, 12, all.length);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, getAiEncryptionKey(), new GCMParameterSpec(128, iv));
        return new String(cipher.doFinal(body), "UTF-8");
    }

    private String readLimited(InputStream in, int maxBytes) throws Exception {
        try (InputStream source = in; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192]; int n;
            while ((n = source.read(buffer)) != -1) {
                if (out.size() + n > maxBytes) throw new java.io.IOException("AI API response exceeds 2 MB");
                out.write(buffer, 0, n);
            }
            return out.toString("UTF-8");
        }
    }

    private boolean isGeminiInteractionsEndpoint(String endpoint) {
        try {
            Uri uri = Uri.parse(endpoint);
            String host = uri.getHost();
            String path = uri.getPath() == null ? "" : uri.getPath();
            return "generativelanguage.googleapis.com".equalsIgnoreCase(host)
                    && path.endsWith("/interactions");
        } catch (Exception ignored) { return false; }
    }

    private String messagesToGeminiInput(JSONArray messages) throws Exception {
        StringBuilder input = new StringBuilder();
        for (int i = 0; i < messages.length(); i++) {
            JSONObject item = messages.optJSONObject(i);
            if (item == null) continue;
            String role = item.optString("role", "user").trim().toLowerCase(java.util.Locale.ROOT);
            String label = "assistant".equals(role) || "model".equals(role) ? "Assistant"
                    : "system".equals(role) ? "System" : "User";
            Object raw = item.opt("content");
            String content = "";
            if (raw instanceof String) content = (String) raw;
            else if (raw instanceof JSONArray) {
                JSONArray parts = (JSONArray) raw; StringBuilder partText = new StringBuilder();
                for (int j = 0; j < parts.length(); j++) {
                    JSONObject part = parts.optJSONObject(j);
                    if (part != null && part.has("text")) {
                        if (partText.length() > 0) partText.append("\n");
                        partText.append(part.optString("text", ""));
                    }
                }
                content = partText.toString();
            } else if (raw != null) content = String.valueOf(raw);
            input.append(label).append(":\n").append(content).append("\n\n");
        }
        if (input.length() == 0) throw new java.io.IOException("AI message history is empty");
        return input.toString();
    }

    private String extractGeminiInteractionText(JSONObject response) throws Exception {
        JSONArray steps = response.optJSONArray("steps");
        StringBuilder answer = new StringBuilder();
        if (steps != null) {
            for (int i = 0; i < steps.length(); i++) {
                JSONObject step = steps.optJSONObject(i);
                if (step == null || !"model_output".equals(step.optString("type"))) continue;
                JSONArray content = step.optJSONArray("content");
                if (content == null) continue;
                for (int j = 0; j < content.length(); j++) {
                    JSONObject part = content.optJSONObject(j);
                    if (part != null && "text".equals(part.optString("type"))) {
                        String value = part.optString("text", "");
                        if (!value.isEmpty()) { if (answer.length() > 0) answer.append("\n"); answer.append(value); }
                    }
                }
            }
        }
        JSONArray outputs = response.optJSONArray("outputs");
        if (answer.length() == 0 && outputs != null) {
            for (int i = 0; i < outputs.length(); i++) {
                JSONObject part = outputs.optJSONObject(i);
                if (part != null && "text".equals(part.optString("type"))) answer.append(part.optString("text", ""));
            }
        }
        if (answer.toString().trim().isEmpty()) {
            String status = response.optString("status", "");
            throw new java.io.IOException("Gemini returned no text" + (status.isEmpty() ? "." : " (status: " + status + ")."));
        }
        return answer.toString().trim();
    }

    private JSONObject callAiChat(String messagesJson) throws Exception {
        android.content.SharedPreferences prefs = getSharedPreferences(AI_PREFS, MODE_PRIVATE);
        String endpoint = prefs.getString("endpoint", "");
        String model = prefs.getString("model", "");
        String encryptedKey = prefs.getString("api_key_enc", "");
        String apiKey = "";
        if (!endpoint.isEmpty() && !model.isEmpty() && !encryptedKey.isEmpty()) {
            validateAiEndpoint(endpoint);
            apiKey = decryptAiKey(encryptedKey);
        } else {
            try {
                JSONObject bundled = new JSONObject(readAssetText("site/web2apk-ai-config.json"));
                endpoint = bundled.optString("endpoint", "");
                model = bundled.optString("model", "");
                apiKey = bundled.optString("apiKey", "");
            } catch (Exception ignored) { }
        }
        if (endpoint.isEmpty() || model.isEmpty() || apiKey.isEmpty()) {
            throw new java.io.IOException("Open the AI API button in this app and save the endpoint, model and API key first");
        }
        validateAiEndpoint(endpoint);
        JSONArray messages = new JSONArray(messagesJson);
        if (messages.length() < 1 || messages.length() > 50 || messagesJson.length() > 256 * 1024) {
            throw new java.io.IOException("Messages must contain 1–50 items and be under 256 KB");
        }
        for (int i = 0; i < messages.length(); i++) {
            JSONObject item = messages.optJSONObject(i);
            if (item == null || item.optString("role", "").trim().isEmpty() || !item.has("content")) {
                throw new java.io.IOException("Each message needs role and content fields");
            }
        }
        boolean gemini = isGeminiInteractionsEndpoint(endpoint);
        JSONObject request = new JSONObject();
        request.put("model", model);
        if (gemini) {
            request.put("input", messagesToGeminiInput(messages));
            request.put("store", false);
        } else {
            request.put("messages", messages); request.put("stream", false);
        }
        byte[] body = request.toString().getBytes("UTF-8");
        HttpsURLConnection connection = (HttpsURLConnection) new URL(endpoint).openConnection();
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(20000); connection.setReadTimeout(45000);
        connection.setDoOutput(true);
        if (gemini) connection.setRequestProperty("x-goog-api-key", apiKey);
        else connection.setRequestProperty("Authorization", "Bearer " + apiKey);
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        connection.setRequestProperty("Accept", "application/json");
        connection.setFixedLengthStreamingMode(body.length);
        try {
            try (OutputStream out = connection.getOutputStream()) { out.write(body); }
            int code = connection.getResponseCode();
            InputStream responseStream = code >= 200 && code < 300 ? connection.getInputStream() : connection.getErrorStream();
            String responseText = responseStream == null ? "" : readLimited(responseStream, 2 * 1024 * 1024);
            if (code < 200 || code >= 300) {
                String detail = responseText;
                try { detail = new JSONObject(responseText).optJSONObject("error").optString("message", responseText); } catch (Exception ignored) { }
                throw new java.io.IOException("AI API HTTP " + code + ": " + (detail.length() > 350 ? detail.substring(0, 350) : detail));
            }
            JSONObject response = new JSONObject(responseText);
            String answer;
            if (gemini) {
                answer = extractGeminiInteractionText(response);
            } else {
                JSONArray choices = response.optJSONArray("choices");
                if (choices == null || choices.length() == 0) throw new java.io.IOException("AI response has no choices / message content");
                JSONObject message = choices.getJSONObject(0).optJSONObject("message");
                if (message == null) throw new java.io.IOException("AI response has no message content");
                Object content = message.opt("content");
                if (content instanceof String) answer = (String) content;
                else if (content instanceof JSONArray) {
                    StringBuilder joined = new StringBuilder(); JSONArray parts = (JSONArray) content;
                    for (int i = 0; i < parts.length(); i++) { JSONObject part = parts.optJSONObject(i); if (part != null && part.has("text")) joined.append(part.optString("text")); }
                    answer = joined.toString();
                } else answer = content == null ? "" : String.valueOf(content);
            }
            if (answer.trim().isEmpty()) throw new java.io.IOException("AI returned an empty response");
            JSONObject result = new JSONObject(); result.put("ok", true); result.put("content", answer);
            result.put("model", response.optString("model", model));
            return result;
        } finally { connection.disconnect(); }
    }

    private void addCreatorWatermark(FrameLayout root) {
        // A compact two-line watermark is shown in generated apps when the
        // Creator Credit option is enabled in WEB2APK before building.
        LinearLayout credit = new LinearLayout(this);
        credit.setOrientation(LinearLayout.VERTICAL);
        credit.setGravity(Gravity.CENTER);
        credit.setPadding(dp(8), dp(4), dp(8), dp(4));
        credit.setClickable(false);
        credit.setFocusable(false);
        credit.setImportantForAccessibility(android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO);

        TextView mark = new TextView(this);
        mark.setText("Made By Tg - @Hop3_1");
        mark.setTextSize(10f);
        mark.setTextColor(0xFF7B8491);
        mark.setAlpha(0.92f);
        mark.setShadowLayer(1.5f, 0f, 1f, 0x22818A98);
        mark.setGravity(Gravity.CENTER);
        mark.setSingleLine(true);
        mark.setClickable(false);
        mark.setFocusable(false);
        credit.addView(mark, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView hint = new TextView(this);
        hint.setText("You can hide this credit in Settings.");
        hint.setTextSize(8f);
        hint.setTextColor(0xFF929AA5);
        hint.setAlpha(0.92f);
        hint.setGravity(Gravity.CENTER);
        hint.setSingleLine(true);
        hint.setClickable(false);
        hint.setFocusable(false);
        credit.addView(hint, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        params.bottomMargin = dp(7);
        root.addView(credit, params);
    }

    private String decodeConfigBase64(String encoded, String fallback) {
        if (encoded == null || encoded.isEmpty()) return fallback;
        try { return new String(Base64.decode(encoded, Base64.DEFAULT), java.nio.charset.StandardCharsets.UTF_8); }
        catch (Exception ignored) { return fallback; }
    }

    private int parseConfigColor(String raw, int fallback) {
        try {
            if (raw == null || !raw.matches("#[0-9A-Fa-f]{6}([0-9A-Fa-f]{2})?")) return fallback;
            return Color.parseColor(raw);
        } catch (Exception ignored) { return fallback; }
    }

    private void addPerformanceMonitor(FrameLayout root) {
        performanceBadge = new TextView(this);
        performanceBadge.setText("⚡");
        performanceBadge.setTextSize(16f);
        performanceBadge.setTextColor(0xFF5547B8);
        performanceBadge.setGravity(Gravity.CENTER);
        performanceBadge.setPadding(dp(12), dp(8), dp(12), dp(8));
        android.graphics.drawable.GradientDrawable background = new android.graphics.drawable.GradientDrawable();
        background.setColor(0xEFFFFFFF);
        background.setCornerRadius(dp(20));
        background.setStroke(dp(1), 0x887B73D8);
        performanceBadge.setBackground(background);
        performanceBadge.setElevation(dp(5));
        performanceBadge.setContentDescription("Open performance monitor");
        performanceBadge.setOnClickListener(v -> showPerformanceDialog());
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.END);
        params.topMargin = dp(12);
        params.rightMargin = dp(12);
        root.addView(performanceBadge, params);
    }

    private void showPerformanceDialog() {
        String webViewVersion = "Unknown";
        if (Build.VERSION.SDK_INT >= 26) {
            try {
                PackageInfo info = WebView.getCurrentWebViewPackage();
                if (info != null) webViewVersion = info.versionName;
            } catch (Exception ignored) { }
        }
        String appVersion = "Unknown";
        try { appVersion = getPackageManager().getPackageInfo(getPackageName(), 0).versionName; }
        catch (Exception ignored) { }
        long uptime = Math.max(0L, SystemClock.elapsedRealtime() - appStartTime);
        String lastLoad = lastPageLoadMs < 0L ? "Not completed yet" : lastPageLoadMs + " ms";
        String details = "App version: " + appVersion + "\n" +
                "WebView: " + webViewVersion + "\n" +
                "Uptime: " + (uptime / 1000L) + " s\n" +
                "Last page load: " + lastLoad + "\n" +
                "Main page errors: " + loadErrorCount + "\n" +
                "Last error: " + lastLoadError + "\n" +
                "Cache mode: " + cacheMode + "\n" +
                "User agent mode: " + userAgentMode;
        new AlertDialog.Builder(this).setTitle("Performance Monitor")
                .setMessage(details).setPositiveButton("Close", null)
                .setNeutralButton("Reload", (dialog, which) -> web.reload()).show();
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private boolean configValue(String config, String key) {
        String prefix = key + "=";
        for (String line : config.split("\\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith(prefix)) {
                return "true".equalsIgnoreCase(trimmed.substring(prefix.length()).trim());
            }
        }
        return false;
    }

    private String readAssetText(String path) {
        try (InputStream in = getAssets().open(path); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[2048];
            int n;
            while ((n = in.read(buffer)) > 0) out.write(buffer, 0, n);
            return out.toString("UTF-8").trim();
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean handleExternalUrl(String rawUrl) {
        try {
            Uri uri = Uri.parse(rawUrl);
            String scheme = uri.getScheme();
            if (scheme == null) return false;
            if ("tel".equalsIgnoreCase(scheme) && allowPhoneDialer) {
                startActivity(new Intent(Intent.ACTION_DIAL, uri));
                return true;
            }
            if (externalLinksBrowser && ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))) {
                if (isAppOrSourceOrigin(uri)) return false;
                try { startActivity(new Intent(Intent.ACTION_VIEW, uri)); return true; }
                catch (Exception noBrowser) { return false; }
            }
            if (externalLinksBrowser && ("mailto".equalsIgnoreCase(scheme) || "sms".equalsIgnoreCase(scheme))) {
                try { startActivity(new Intent(Intent.ACTION_VIEW, uri)); return true; }
                catch (Exception noHandler) { return false; }
            }
            return false;
        } catch (Exception ignored) { return false; }
    }

    private boolean isAppOrSourceOrigin(Uri target) {
        if (target == null) return false;
        if ("https".equalsIgnoreCase(target.getScheme())
                && "appassets.androidplatform.net".equalsIgnoreCase(target.getHost())) return true;
        String source = readAssetText("site_url.txt");
        if (source == null || source.isEmpty()) return false;
        try { return sameWebsiteOriginOrHttpsUpgrade(Uri.parse(source), target); }
        catch (Exception ignored) { return false; }
    }

    private boolean isTrustedRuntimePage() {
        String current = currentMainFrameUrl;
        if (current == null || current.isEmpty()) return false;
        try { return isAppOrSourceOrigin(Uri.parse(current)); }
        catch (Exception ignored) { return false; }
    }

    private boolean sameWebsiteOriginOrHttpsUpgrade(Uri source, Uri target) {
        if (sameWebOrigin(source, target)) return true;
        if (source == null || target == null || source.getHost() == null || target.getHost() == null) return false;
        if (!source.getHost().equalsIgnoreCase(target.getHost())) return false;
        if (!"http".equalsIgnoreCase(source.getScheme()) || !"https".equalsIgnoreCase(target.getScheme())) return false;
        int sourcePort = source.getPort() < 0 ? 80 : source.getPort();
        int targetPort = target.getPort() < 0 ? 443 : target.getPort();
        return sourcePort == 80 && targetPort == 443;
    }

    private boolean sameWebOrigin(Uri first, Uri second) {
        if (first == null || second == null) return false;
        String schemeA = first.getScheme();
        String schemeB = second.getScheme();
        String hostA = first.getHost();
        String hostB = second.getHost();
        if (schemeA == null || schemeB == null || hostA == null || hostB == null) return false;
        if (!schemeA.equalsIgnoreCase(schemeB) || !hostA.equalsIgnoreCase(hostB)) return false;
        int portA = first.getPort();
        int portB = second.getPort();
        if (portA < 0) portA = "https".equalsIgnoreCase(schemeA) ? 443 : ("http".equalsIgnoreCase(schemeA) ? 80 : -1);
        if (portB < 0) portB = "https".equalsIgnoreCase(schemeB) ? 443 : ("http".equalsIgnoreCase(schemeB) ? 80 : -1);
        return portA == portB;
    }

    private void injectVibrationShim() {
        if (!allowVibration || web == null) return;
        String script = "(function(){try{if(!navigator.vibrate){Object.defineProperty(navigator,'vibrate',{configurable:true,value:function(pattern){var n=0;if(Array.isArray(pattern)){for(var i=0;i<pattern.length;i++)n+=Math.max(0,Number(pattern[i])||0);}else n=Math.max(0,Number(pattern)||0);n=Math.min(5000,n);if(window.Web2ApkNative)window.Web2ApkNative.vibrate(n);return true;}});}}catch(e){}})();";
        web.evaluateJavascript(script, null);
    }

    private final class RuntimeBridge {
        @JavascriptInterface public void openAiSettings() { if (enableAiApi && isTrustedRuntimePage()) runOnUiThread(() -> showAiSettingsDialog()); }
        @JavascriptInterface public boolean isAiConfigured() {
            if (!enableAiApi || !isTrustedRuntimePage()) return false;
            android.content.SharedPreferences prefs = getSharedPreferences(AI_PREFS, MODE_PRIVATE);
            boolean saved = !prefs.getString("endpoint", "").isEmpty() && !prefs.getString("model", "").isEmpty() && !prefs.getString("api_key_enc", "").isEmpty();
            if (saved) return true;
            try {
                JSONObject bundled = new JSONObject(readAssetText("site/web2apk-ai-config.json"));
                return !bundled.optString("apiKey", "").isEmpty() && !bundled.optString("model", "").isEmpty();
            } catch (Exception ignored) { return false; }
        }
        @JavascriptInterface public void vibrate(int durationMs) {
            if (!allowVibration || !isTrustedRuntimePage()) return;
            runOnUiThread(() -> {
                try {
                    Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
                    if (vibrator == null || !vibrator.hasVibrator()) return;
                    long duration = Math.max(1L, Math.min(5000L, durationMs));
                    if (Build.VERSION.SDK_INT >= 26) vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE));
                    else vibrator.vibrate(duration);
                } catch (Exception ignored) { }
            });
        }

        @JavascriptInterface public void pickContact(String callbackName) {
            if (!allowContactPicker || !isTrustedRuntimePage()) return;
            if (callbackName == null || !callbackName.matches("[A-Za-z_$][A-Za-z0-9_$]{0,63}")) return;
            runOnUiThread(() -> {
                try {
                    pendingContactCallback = callbackName;
                    Intent pick = new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
                    startActivityForResult(pick, REQ_CONTACT_PICK);
                } catch (Exception e) {
                    pendingContactCallback = null;
                    Toast.makeText(WebActivity.this, "Contacts picker unavailable", Toast.LENGTH_SHORT).show();
                }
            });
        }

        @JavascriptInterface public void addContact(String name, String phone) {
            if (!allowAddContact || !isTrustedRuntimePage()) return;
            runOnUiThread(() -> {
                try {
                    Intent insert = new Intent(Intent.ACTION_INSERT, ContactsContract.Contacts.CONTENT_URI);
                    insert.putExtra(ContactsContract.Intents.Insert.NAME, name == null ? "" : name.substring(0, Math.min(120, name.length())));
                    insert.putExtra(ContactsContract.Intents.Insert.PHONE, phone == null ? "" : phone.substring(0, Math.min(60, phone.length())));
                    startActivity(insert);
                } catch (Exception e) {
                    Toast.makeText(WebActivity.this, "Contacts app unavailable", Toast.LENGTH_SHORT).show();
                }
            });
        }

        @JavascriptInterface public void openCalendar() {
            if (!allowViewCalendar || !isTrustedRuntimePage()) return;
            runOnUiThread(() -> {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, CalendarContract.CONTENT_URI));
                } catch (Exception e) {
                    Toast.makeText(WebActivity.this, "Calendar app unavailable", Toast.LENGTH_SHORT).show();
                }
            });
        }

        @JavascriptInterface public void createCalendarEvent(String title, long beginMillis, long endMillis, String description) {
            if (!allowCalendarInsert || !isTrustedRuntimePage()) return;
            runOnUiThread(() -> {
                try {
                    Intent insert = new Intent(Intent.ACTION_INSERT).setData(CalendarContract.Events.CONTENT_URI);
                    insert.putExtra(CalendarContract.Events.TITLE, title == null ? "" : title.substring(0, Math.min(120, title.length())));
                    insert.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, beginMillis);
                    insert.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, Math.max(beginMillis + 60000L, endMillis));
                    insert.putExtra(CalendarContract.Events.DESCRIPTION, description == null ? "" : description.substring(0, Math.min(1000, description.length())));
                    startActivity(insert);
                } catch (Exception e) {
                    Toast.makeText(WebActivity.this, "Calendar app unavailable", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private boolean isPermissionDeclared(String permission) {
        try {
            PackageInfo info;
            if (Build.VERSION.SDK_INT >= 33) {
                info = getPackageManager().getPackageInfo(getPackageName(), PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS));
            } else {
                //noinspection deprecation
                info = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_PERMISSIONS);
            }
            if (info.requestedPermissions != null) {
                for (String p : info.requestedPermissions) if (permission.equals(p)) return true;
            }
        } catch (Exception ignored) { }
        return false;
    }

    private final class RuntimeChrome extends WebChromeClient {
        @Override
        public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
            if (!fileUploadEnabled) {
                callback.onReceiveValue(null);
                return true;
            }
            if (pendingFile != null) pendingFile.onReceiveValue(null);
            pendingFile = callback;
            Intent pick = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            pick.addCategory(Intent.CATEGORY_OPENABLE);
            pick.setType("*/*");
            if (params.getMode() == FileChooserParams.MODE_OPEN_MULTIPLE) {
                pick.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            }
            try {
                startActivityForResult(pick, REQ_FILE);
                return true;
            } catch (Exception e) {
                pendingFile = null;
                Toast.makeText(WebActivity.this, "No file picker available", Toast.LENGTH_SHORT).show();
                return false;
            }
        }

        @Override
        public void onPermissionRequest(PermissionRequest request) {
            if (request == null || !isAppOrSourceOrigin(request.getOrigin())) {
                if (request != null) request.deny();
                return;
            }
            runOnUiThread(() -> requestMedia(request));
        }

        @Override
        public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
            boolean trusted = false;
            try { trusted = isAppOrSourceOrigin(Uri.parse(origin)); } catch (Exception ignored) { }
            if (!trusted) {
                callback.invoke(origin, false, false);
                return;
            }
            runOnUiThread(() -> requestLocation(origin, callback));
        }
    }

    private void requestMedia(PermissionRequest request) {
        ArrayList<String> wanted = new ArrayList<>();
        ArrayList<String> grantableResources = new ArrayList<>();
        for (String resource : request.getResources()) {
            if (PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(resource)) {
                wanted.add(Manifest.permission.CAMERA);
                grantableResources.add(resource);
            } else if (PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(resource)) {
                wanted.add(Manifest.permission.RECORD_AUDIO);
                grantableResources.add(resource);
            }
        }
        if (wanted.isEmpty() || grantableResources.isEmpty()) { request.deny(); return; }
        ArrayList<String> ask = new ArrayList<>();
        for (String permission : wanted) {
            if (isPermissionDeclared(permission) && checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                if (!ask.contains(permission)) ask.add(permission);
            } else if (!isPermissionDeclared(permission)) {
                request.deny();
                return;
            }
        }
        String[] safeResources = grantableResources.toArray(new String[0]);
        if (ask.isEmpty()) {
            request.grant(safeResources);
        } else {
            pendingMedia = request;
            pendingMediaPermissions = ask.toArray(new String[0]);
            pendingMediaResources = safeResources;
            requestPermissions(pendingMediaPermissions, REQ_MEDIA);
        }
    }

    private void requestLocation(String origin, GeolocationPermissions.Callback callback) {
        if (!isPermissionDeclared(Manifest.permission.ACCESS_COARSE_LOCATION)
                && !isPermissionDeclared(Manifest.permission.ACCESS_FINE_LOCATION)) {
            callback.invoke(origin, false, false);
            return;
        }
        ArrayList<String> ask = new ArrayList<>();
        if (isPermissionDeclared(Manifest.permission.ACCESS_COARSE_LOCATION)
                && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ask.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }
        if (isPermissionDeclared(Manifest.permission.ACCESS_FINE_LOCATION)
                && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ask.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (ask.isEmpty()) callback.invoke(origin, true, false);
        else {
            pendingGeo = callback;
            pendingGeoOrigin = origin;
            requestPermissions(ask.toArray(new String[0]), REQ_LOCATION);
        }
    }

    private void download(String url, String userAgent, String disposition, String mimeType) {
        try {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.setMimeType(mimeType);
            if (userAgent != null) request.addRequestHeader("User-Agent", userAgent);
            String cookie = CookieManager.getInstance().getCookie(url);
            if (cookie != null) request.addRequestHeader("Cookie", cookie);
            String fileName = URLUtil.guessFileName(url, disposition, mimeType);
            request.setTitle(fileName);
            request.setDescription("Downloading from website");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
            ((DownloadManager) getSystemService(DOWNLOAD_SERVICE)).enqueue(request);
            Toast.makeText(this, "Download started", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Download failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_CONTACT_PICK) {
            String callback = pendingContactCallback;
            pendingContactCallback = null;
            if (resultCode == RESULT_OK && data != null && data.getData() != null && callback != null) {
                String name = "";
                String phone = "";
                try (Cursor cursor = getContentResolver().query(data.getData(),
                        new String[]{ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER},
                        null, null, null)) {
                    if (cursor != null && cursor.moveToFirst()) {
                        name = cursor.getString(0);
                        phone = cursor.getString(1);
                    }
                } catch (Exception ignored) { }
                String result = "{\"name\":" + org.json.JSONObject.quote(name == null ? "" : name)
                        + ",\"phone\":" + org.json.JSONObject.quote(phone == null ? "" : phone) + "}";
                if (web != null) web.evaluateJavascript(callback + "(" + result + ")", null);
            }
            return;
        }
        if (requestCode == REQ_FILE && pendingFile != null) {
            Uri[] result = null;
            if (resultCode == RESULT_OK && data != null) {
                if (data.getClipData() != null) {
                    int count = data.getClipData().getItemCount();
                    result = new Uri[count];
                    for (int i = 0; i < count; i++) result[i] = data.getClipData().getItemAt(i).getUri();
                } else if (data.getData() != null) result = new Uri[]{data.getData()};
            }
            pendingFile.onReceiveValue(result);
            pendingFile = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_MEDIA && pendingMedia != null) {
            boolean ok = grantResults.length > 0;
            for (int result : grantResults) if (result != PackageManager.PERMISSION_GRANTED) ok = false;
            if (ok && pendingMediaResources != null && pendingMediaResources.length > 0) pendingMedia.grant(pendingMediaResources);
            else pendingMedia.deny();
            pendingMedia = null;
            pendingMediaPermissions = null;
            pendingMediaResources = null;
        } else if (requestCode == REQ_LOCATION && pendingGeo != null) {
            boolean ok = false;
            for (int result : grantResults) if (result == PackageManager.PERMISSION_GRANTED) ok = true;
            pendingGeo.invoke(pendingGeoOrigin, ok, false);
            pendingGeo = null;
            pendingGeoOrigin = null;
        }
    }

    @Override
    public void onBackPressed() {
        if ("exit".equals(backBehavior)) { finish(); return; }
        if (web != null && web.canGoBack()) { web.goBack(); return; }
        if ("confirm".equals(backBehavior)) {
            new android.app.AlertDialog.Builder(this)
                    .setTitle("Exit app?")
                    .setMessage("Do you want to close this app?")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Exit", (dialog, which) -> finish())
                    .show();
            return;
        }
        super.onBackPressed();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && fullscreen) applyImmersiveMode();
    }

    @Override
    protected void onDestroy() {
        aiExecutor.shutdownNow();
        uiHandler.removeCallbacksAndMessages(null);
        if (web != null) {
            web.stopLoading();
            web.destroy();
            web = null;
        }
        super.onDestroy();
    }
}
