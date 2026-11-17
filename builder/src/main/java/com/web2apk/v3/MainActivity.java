package com.web2apk.v3;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import android.view.WindowManager;
import android.view.Display;
import android.graphics.Color;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import org.json.JSONObject;
import org.json.JSONArray;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.security.KeyStore;
import javax.net.ssl.HttpsURLConnection;
import java.net.URL;
import androidx.core.content.FileProvider;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class MainActivity extends Activity {
    private static final int PICK_SOURCE = 1001;
    private static final int PICK_ICON = 1002;
    private static final int PICK_EXPORT = 1003;
    private static final int REQ_NOTIFY = 1004;
    private static final int PICK_EDITOR_ZIP = 1005;
    private static final int PICK_EDITOR_ASSET = 1006;
    private static final int PICK_EDITOR_EXPORT = 1007;
    private static final int PICK_SPLASH_MEDIA = 1008;
    private static final String BUILDER_AI_PREFS = "web2apk_builder_ai_settings";
    private static final String BUILDER_AI_KEY_ALIAS = "web2apk_builder_ai_key_v1";
    private static final String DEFAULT_AI_ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/interactions";
    private static final String DEFAULT_AI_MODEL = "gemini-3.8-flash";
    private static final long EDITOR_BACKUP_INTERVAL_MS = 5L * 60L * 1000L;
    private static final long EDITOR_BACKUP_MAX_BYTES = 30L * 1024L * 1024L;
    private static final int EDITOR_BACKUP_KEEP = 10;

    private WebView web;
    private Uri sourceUri;
    private Uri iconUri;
    private Uri splashMediaUri;
    private String sourceMode = "";
    private String sourceName = "";
    private String iconName = "";
    private String pendingInstallPath = "";
    private File pendingExport;
    private File pendingEditorZipExport;
    private File editorProjectRoot;
    private String editorProjectName = "My Web Project";
    private String editorHtmlPath = "index.html";
    private String editorCssPath = "style.css";
    private String editorJsPath = "script.js";
    private String currentDesignOptionsJson = "{}";
    private volatile int buildProgress = 0;
    private volatile String buildMessage = "Ready · Internal APK engine";
    private int lastProgressNotification = -1;
    private boolean buildRunning = false;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService buildExecutor = Executors.newSingleThreadExecutor();
    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(235, 238, 255));
        getWindow().setNavigationBarColor(Color.rgb(235, 238, 255));
        getWindow().getDecorView().setSystemUiVisibility(
                android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                        | android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
        requestPreferredRefreshRate();
        ensureEditorProject();

        web = new WebView(this);
        web.setBackgroundColor(Color.TRANSPARENT);
        web.setOverScrollMode(WebView.OVER_SCROLL_NEVER);
        web.setScrollBarStyle(WebView.SCROLLBARS_INSIDE_OVERLAY);
        WebSettings settings = web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setLoadsImagesAutomatically(true);
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        if (Build.VERSION.SDK_INT >= 26) web.setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_BOUND, true);
        web.addJavascriptInterface(new Bridge(), "AppBridge");
        web.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                loadEditorProjectToUi();
            }
        });
        setContentView(web);
        web.loadUrl("file:///android_asset/index.html");
        // No Termux, shared-storage, or all-files permission is requested on startup.
    }

    private void applySystemBarTheme(boolean dark) {
        int status = dark ? Color.BLACK : Color.rgb(235, 238, 255);
        int nav = dark ? Color.BLACK : Color.rgb(235, 238, 255);
        getWindow().setStatusBarColor(status);
        getWindow().setNavigationBarColor(nav);
        int flags = getWindow().getDecorView().getSystemUiVisibility();
        int lightFlags = android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                | android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        if (dark) flags &= ~lightFlags;
        else flags |= lightFlags;
        getWindow().getDecorView().setSystemUiVisibility(flags);
    }

    private void requestPreferredRefreshRate() {
        try {
            Display display = getWindowManager().getDefaultDisplay();
            float best = 0f;
            for (Display.Mode mode : display.getSupportedModes()) {
                float hz = mode.getRefreshRate();
                if (hz > best) best = hz;
            }
            if (best > 0f) {
                WindowManager.LayoutParams attributes = getWindow().getAttributes();
                attributes.preferredRefreshRate = best;
                getWindow().setAttributes(attributes);
            }
        } catch (Exception ignored) { }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!pendingInstallPath.isEmpty() && Build.VERSION.SDK_INT >= 26
                && getPackageManager().canRequestPackageInstalls()) {
            String path = pendingInstallPath;
            pendingInstallPath = "";
            handler.postDelayed(() -> installApk(new File(path)), 400L);
        }
    }

    private void js(String script) {
        runOnUiThread(() -> {
            if (web != null) web.evaluateJavascript(script, null);
        });
    }

    private void updateStatus(String message, int percent) {
        buildMessage = message;
        buildProgress = percent;
        js("setStatus(" + quote(message) + "," + percent + ")");
        if (buildRunning) notifyBuildProgress(message, percent);
    }

    private void toast(String message) {
        runOnUiThread(() -> Toast.makeText(this, message, Toast.LENGTH_LONG).show());
    }

    private void choose(boolean image) {
        Intent picker = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        picker.addCategory(Intent.CATEGORY_OPENABLE);
        picker.setType(image ? "image/*" : "*/*");
        picker.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(picker, image ? PICK_ICON : PICK_SOURCE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_EXPORT && resultCode != RESULT_OK) pendingExport = null;
        if (requestCode == PICK_EDITOR_EXPORT && resultCode != RESULT_OK) pendingEditorZipExport = null;
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri selected = data.getData();
        if (requestCode == PICK_SOURCE || requestCode == PICK_ICON || requestCode == PICK_SPLASH_MEDIA) {
            try {
                int grantFlags = data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                if (grantFlags != 0) getContentResolver().takePersistableUriPermission(selected, grantFlags);
            } catch (Exception ignored) {
                // Some document providers issue temporary grants only; the current selection remains usable.
            }
        }
        if (requestCode == PICK_EDITOR_ZIP) {
            try {
                importEditorZip(selected);
            } catch (Exception e) {
                toast("Project ZIP import failed: " + safeMessage(e));
            }
            return;
        } else if (requestCode == PICK_EDITOR_ASSET) {
            try {
                openEditorFileInCodeBox(selected);
            } catch (Exception e) {
                toast("Could not open project file: " + safeMessage(e));
            }
            return;
        } else if (requestCode == PICK_EDITOR_EXPORT) {
            File source = pendingEditorZipExport;
            pendingEditorZipExport = null;
            if (source != null && source.isFile()) {
                try (InputStream in = new FileInputStream(source);
                     OutputStream out = getContentResolver().openOutputStream(selected, "w")) {
                    if (out == null) throw new IOException("Cannot open ZIP destination");
                    byte[] buffer = new byte[16384];
                    int n;
                    while ((n = in.read(buffer)) != -1) out.write(buffer, 0, n);
                    toast("Project ZIP exported successfully.");
                } catch (Exception e) {
                    toast("Project ZIP export failed: " + safeMessage(e));
                }
            }
            return;
        }
        if (requestCode == PICK_SPLASH_MEDIA) {
            splashMediaUri = selected;
            js("selectedSplashMedia(" + quote(displayName(selected)) + ")");
        } else if (requestCode == PICK_SOURCE) {
            sourceUri = selected;
            sourceName = displayName(selected);
            js("selectedSource(" + quote(sourceName) + ")");
        } else if (requestCode == PICK_ICON) {
            iconUri = selected;
            iconName = displayName(selected);
            String preview = "";
            try { preview = createIconPreviewDataUri(selected); }
            catch (Exception e) { toast("Icon preview failed: " + e.getMessage()); }
            js("selectedIcon(" + quote(iconName) + "," + quote(preview) + ")");
        } else if (requestCode == PICK_EXPORT) {
            File source = pendingExport;
            pendingExport = null;
            if (source != null && source.isFile()) {
                try (InputStream in = new FileInputStream(source);
                     OutputStream out = getContentResolver().openOutputStream(selected, "w")) {
                    if (out == null) throw new IOException("Cannot open selected destination");
                    byte[] buffer = new byte[16384];
                    int n;
                    while ((n = in.read(buffer)) != -1) out.write(buffer, 0, n);
                    toast("APK saved successfully.");
                } catch (Exception e) {
                    toast("Export failed: " + e.getMessage());
                }
            }
        }
    }

    private String displayName(Uri uri) {
        String value = uri.getLastPathSegment();
        if (value == null || value.trim().isEmpty()) return "Selected file";
        int colon = value.lastIndexOf(':');
        return colon >= 0 ? value.substring(colon + 1) : value;
    }

    private String createIconPreviewDataUri(Uri uri) throws IOException {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream in = getContentResolver().openInputStream(uri)) {
            if (in == null) throw new IOException("Cannot read selected image");
            BitmapFactory.decodeStream(in, null, bounds);
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw new IOException("Unsupported image format");
        int sample = 1;
        while (Math.max(bounds.outWidth, bounds.outHeight) / sample > 384) sample *= 2;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sample;
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap source;
        try (InputStream in = getContentResolver().openInputStream(uri)) {
            if (in == null) throw new IOException("Cannot read selected image");
            source = BitmapFactory.decodeStream(in, null, options);
        }
        if (source == null) throw new IOException("Could not decode selected image");
        Bitmap square = null;
        Bitmap thumb = null;
        try {
            int side = Math.min(source.getWidth(), source.getHeight());
            square = Bitmap.createBitmap(source, (source.getWidth() - side) / 2,
                    (source.getHeight() - side) / 2, side, side);
            thumb = Bitmap.createScaledBitmap(square, 112, 112, true);
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            if (!thumb.compress(Bitmap.CompressFormat.PNG, 100, bytes)) throw new IOException("Could not prepare image preview");
            return "data:image/png;base64," + Base64.encodeToString(bytes.toByteArray(), Base64.NO_WRAP);
        } finally {
            if (thumb != null && thumb != square && thumb != source) thumb.recycle();
            if (square != null && square != source) square.recycle();
            if (source != null && !source.isRecycled()) source.recycle();
        }
    }

    private static String quote(String value) {
        String safe = String.valueOf(value)
                .replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\n", " ")
                .replace("\r", " ");
        return "'" + safe + "'";
    }

    public final class Bridge {
        @JavascriptInterface public void pickSource(String mode) {
            sourceMode = mode == null ? "html" : mode;
            sourceUri = null;
            sourceName = "";
            runOnUiThread(() -> choose(false));
        }
        @JavascriptInterface public void pickIcon() { runOnUiThread(() -> choose(true)); }
        @JavascriptInterface public void pickSplashMedia() { runOnUiThread(() -> {
            Intent picker = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            picker.addCategory(Intent.CATEGORY_OPENABLE);
            picker.setType("*/*");
            picker.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*", "video/*"});
            picker.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            startActivityForResult(picker, PICK_SPLASH_MEDIA);
        }); }
        @JavascriptInterface public void clearSplashMedia() { splashMediaUri = null; js("selectedSplashMedia('')"); }
        @JavascriptInterface public void clearIcon() {
            iconUri = null;
            iconName = "";
            js("selectedIcon('Default WEB2APK icon','')");
        }
        @JavascriptInterface public void clearSource() {
            sourceUri = null;
            sourceMode = "";
            sourceName = "";
            js("selectedSource('No source selected · URL mode')");
        }
        @JavascriptInterface public void preview(String url) { runOnUiThread(() -> showPreview(url)); }
        @JavascriptInterface public void setDarkMode(boolean dark) {
            runOnUiThread(() -> applySystemBarTheme(dark));
        }
        @JavascriptInterface public void openAppSettings() {
            runOnUiThread(() -> startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + getPackageName()))));
        }
        @JavascriptInterface public void openInstallSettings() {
            runOnUiThread(() -> {
                if (Build.VERSION.SDK_INT >= 26) {
                    startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                            Uri.parse("package:" + getPackageName())));
                }
            });
        }
        @JavascriptInterface public void openTelegram(String url) {
            final Uri uri;
            try {
                uri = Uri.parse(url == null ? "" : url.trim());
            } catch (Exception invalid) {
                toast("Invalid Telegram link.");
                return;
            }
            String path = uri.getPath();
            boolean allowed = "https".equalsIgnoreCase(uri.getScheme())
                    && "t.me".equalsIgnoreCase(uri.getHost())
                    && ("/Hop3_1".equalsIgnoreCase(path) || "/web2apkpro".equalsIgnoreCase(path));
            if (!allowed) {
                toast("Only the configured WEB2APK Telegram links are allowed.");
                return;
            }
            runOnUiThread(() -> {
                try {
                    Intent view = new Intent(Intent.ACTION_VIEW, uri);
                    startActivity(Intent.createChooser(view, "Open Telegram"));
                } catch (Exception e) {
                    toast("Could not open Telegram link: " + safeMessage(e));
                }
            });
        }
        @JavascriptInterface public void requestNotifications() {
            runOnUiThread(() -> {
                if (Build.VERSION.SDK_INT >= 33) {
                    requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_NOTIFY);
                } else {
                    toast("Notifications are controlled by Android settings on this version.");
                }
            });
        }
        @JavascriptInterface public String loadBuilderAiSettings() {
            return builderAiSettingsJson();
        }
        @JavascriptInterface public String saveBuilderAiSettings(String endpoint, String model, String apiKey, boolean embedKey) {
            return saveBuilderAiSettingsInternal(endpoint, model, apiKey, embedKey);
        }
        @JavascriptInterface public String clearBuilderAiKey() {
            try {
                boolean saved = getSharedPreferences(BUILDER_AI_PREFS, MODE_PRIVATE).edit().remove("api_key_enc").commit();
                KeyStore store = KeyStore.getInstance("AndroidKeyStore");
                store.load(null);
                if (store.containsAlias(BUILDER_AI_KEY_ALIAS)) store.deleteEntry(BUILDER_AI_KEY_ALIAS);
                return saved ? "{\"ok\":true,\"hasKey\":false}" : "{\"ok\":false,\"error\":\"Could not clear saved API key\"}";
            } catch (Exception e) {
                return "{\"ok\":false,\"error\":" + JSONObject.quote(safeMessage(e)) + "}";
            }
        }
        @JavascriptInterface public void testBuilderAiSettings() {
            runBuilderAiTest();
        }
        @JavascriptInterface public void refreshHistory() {
            js("setHistory(" + quote(historyJson()) + ");setEditorProjects(" + quote(editorProjectsJson()) + ")");
        }
        @JavascriptInterface public String listEditorProjects() { return editorProjectsJson(); }
        @JavascriptInterface public void setDesignOptions(String rawJson) {
            try {
                JSONObject raw = new JSONObject(rawJson == null ? "{}" : rawJson);
                JSONObject clean = new JSONObject();
                String[] strings = {"iconBackground", "iconMask", "splashBackground", "splashTitle", "userAgentMode", "customUserAgent", "buildMode"};
                for (String key : strings) if (raw.has(key)) clean.put(key, raw.optString(key, ""));
                String[] booleans = {"splashProgress", "javascriptEnabled", "domStorageEnabled", "cookiesEnabled", "fileUploadEnabled", "performanceMonitor"};
                for (String key : booleans) if (raw.has(key)) clean.put(key, raw.optBoolean(key));
                String[] numbers = {"iconZoom", "splashMinDuration"};
                for (String key : numbers) if (raw.has(key)) clean.put(key, raw.optDouble(key));
                currentDesignOptionsJson = clean.toString();
                getPreferences(0).edit().putString("design_options_json", currentDesignOptionsJson).apply();
            } catch (Exception e) {
                toast("Advanced settings were not saved: " + safeMessage(e));
            }
        }
        @JavascriptInterface public void openEditorProject(String projectId) {
            try {
                switchEditorProject(resolveEditorProjectFolder(projectId));
                loadEditorProjectToUi();
                toast("Project opened.");
                js("refreshProjectManager()");
            } catch (Exception e) { toast("Open project failed: " + safeMessage(e)); }
        }
        @JavascriptInterface public void renameEditorProject(String name) {
            try {
                String clean = name == null ? "" : name.trim();
                if (clean.isEmpty() || clean.length() > 50) throw new IOException("Project name must contain 1–50 characters");
                editorProjectName = clean;
                saveEditorPaths();
                loadEditorProjectToUi();
                toast("Project renamed.");
                js("refreshProjectManager()");
            } catch (Exception e) { toast("Rename failed: " + safeMessage(e)); }
        }
        @JavascriptInterface public void renameEditorProjectById(String projectId, String name) {
            try {
                String clean = name == null ? "" : name.trim();
                if (clean.isEmpty() || clean.length() > 50) throw new IOException("Project name must contain 1–50 characters");
                switchEditorProject(resolveEditorProjectFolder(projectId));
                editorProjectName = clean;
                saveEditorPaths();
                loadEditorProjectToUi();
                toast("Project renamed.");
                js("refreshProjectManager()");
            } catch (Exception e) { toast("Rename failed: " + safeMessage(e)); }
        }
        @JavascriptInterface public void duplicateEditorProjectById(String projectId, String name) {
            try {
                String clean = name == null ? "" : name.trim();
                if (clean.isEmpty() || clean.length() > 50) throw new IOException("Project name must contain 1–50 characters");
                File source = resolveEditorProjectFolder(projectId);
                File projects = new File(getFilesDir(), "code-editor/projects");
                File copy = new File(projects, safeProjectFileName(clean) + "-" + System.currentTimeMillis());
                if (!copy.mkdirs()) throw new IOException("Cannot create duplicate project folder");
                try { copyProjectTree(source, copy, new long[]{0L}); }
                catch (Exception e) { deleteTree(copy); throw e; }
                switchEditorProject(copy);
                editorProjectName = clean;
                saveEditorPaths();
                loadEditorProjectToUi();
                toast("Project duplicated.");
                js("refreshProjectManager()");
            } catch (Exception e) { toast("Duplicate failed: " + safeMessage(e)); }
        }
        @JavascriptInterface public void duplicateEditorProject(String name) {
            try {
                String clean = name == null ? (editorProjectName + " Copy") : name.trim();
                if (clean.isEmpty() || clean.length() > 50) throw new IOException("Project name must contain 1–50 characters");
                File source = getEditorProjectRoot();
                File projects = new File(getFilesDir(), "code-editor/projects");
                File copy = new File(projects, safeProjectFileName(clean) + "-" + System.currentTimeMillis());
                if (!copy.mkdirs()) throw new IOException("Cannot create duplicate project folder");
                try { copyProjectTree(source, copy, new long[]{0L}); }
                catch (Exception e) { deleteTree(copy); throw e; }
                switchEditorProject(copy);
                editorProjectName = clean;
                saveEditorPaths();
                loadEditorProjectToUi();
                toast("Project duplicated.");
                js("refreshProjectManager()");
            } catch (Exception e) { toast("Duplicate failed: " + safeMessage(e)); }
        }
        @JavascriptInterface public void deleteEditorProject(String projectId) {
            try {
                File target = resolveEditorProjectFolder(projectId);
                boolean wasActive = target.getCanonicalFile().equals(getEditorProjectRoot().getCanonicalFile());
                deleteTree(target);
                if (wasActive) {
                    File next = findNewestEditorProject();
                    if (next == null) {
                        editorProjectName = "My Web Project";
                        File fresh = createEditorWorkspace(editorProjectName);
                        editorHtmlPath = "index.html";
                        editorCssPath = "style.css";
                        editorJsPath = "script.js";
                        writeUtf8(new File(fresh, editorHtmlPath), defaultEditorHtml());
                        writeUtf8(new File(fresh, editorCssPath), defaultEditorCss());
                        writeUtf8(new File(fresh, editorJsPath), defaultEditorJs());
                        saveEditorPaths();
                    } else switchEditorProject(next);
                    loadEditorProjectToUi();
                }
                toast("Project deleted.");
                js("refreshProjectManager()");
            } catch (Exception e) { toast("Delete failed: " + safeMessage(e)); }
        }
        @JavascriptInterface public void renameHistory(String path, String newName) {
            runOnUiThread(() -> renameHistoryApk(path, newName));
        }
        @JavascriptInterface public void copyHistory(String path, String newName) {
            runOnUiThread(() -> copyHistoryApk(path, newName));
        }
        @JavascriptInterface public void startBuild(String name, String pkg, String version, int versionCode,
                                                     boolean camera, boolean mic, boolean locationApprox,
                                                     boolean locationPrecise, boolean notifications,
                                                     boolean keepScreenAwake, boolean blockScreenshots,
                                                     boolean phoneDialer, boolean vibration,
                                                     boolean contactPicker, boolean addContact,
                                                     boolean viewCalendar, boolean calendarInsert, boolean creatorWatermark, boolean enableAiApi, String url, String splashStyle, String splashSpeed,
                                                     String appOrientation, boolean fullscreen, boolean pullToRefresh, String backBehavior,
                                                     boolean externalLinks, String cacheMode, boolean allowZoom, boolean desktopMode, boolean downloadsEnabled) {
            runOnUiThread(() -> beginBuild(name, pkg, version, versionCode,
                    camera, mic, locationApprox, locationPrecise, notifications,
                    keepScreenAwake, blockScreenshots, phoneDialer, vibration,
                    contactPicker, addContact, viewCalendar, calendarInsert, creatorWatermark, enableAiApi, url, splashStyle, splashSpeed,
                    appOrientation, fullscreen, pullToRefresh, backBehavior, externalLinks, cacheMode, allowZoom, desktopMode, downloadsEnabled));
        }
        @JavascriptInterface public void createEditorProject(String name) {
            try {
                createNewEditorProject(name);
                loadEditorProjectToUi();
                toast("New code project created.");
            } catch (Exception e) { toast("New project failed: " + safeMessage(e)); }
        }
        @JavascriptInterface public void saveEditorProject(String name, String html64, String css64, String js64) {
            try { persistEditorProject(name, html64, css64, js64); }
            catch (Exception e) { toast("Project save failed: " + safeMessage(e)); }
        }
        @JavascriptInterface public void saveEditorExtraFile(String relativePath, String content64) {
            try {
                maybeBackupEditorWorkspace();
                String content = decodeEditorText(content64);
                writeUtf8(editorFile(relativePath), content);
            } catch (Exception e) { toast("Extra file save failed: " + safeMessage(e)); }
        }
        private void persistEditorExtraFile(String relativePath, String content64) throws IOException {
            if (relativePath == null || relativePath.trim().isEmpty()) return;
            maybeBackupEditorWorkspace();
            String content = decodeEditorText(content64);
            writeUtf8(editorFile(relativePath), content);
        }
        @JavascriptInterface public void openEditorZip() {
            runOnUiThread(() -> {
                Intent picker = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                picker.addCategory(Intent.CATEGORY_OPENABLE);
                picker.setType("application/zip");
                picker.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                startActivityForResult(picker, PICK_EDITOR_ZIP);
            });
        }
        @JavascriptInterface public void addEditorFile() {
            runOnUiThread(() -> {
                Intent picker = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                picker.addCategory(Intent.CATEGORY_OPENABLE);
                picker.setType("*/*");
                picker.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivityForResult(picker, PICK_EDITOR_ASSET);
            });
        }
        @JavascriptInterface public void exportEditorZip(String extraPath, String extraContent64) {
            try {
                persistEditorExtraFile(extraPath, extraContent64);
                File zip = createEditorProjectZip();
                pendingEditorZipExport = zip;
                runOnUiThread(() -> {
                    Intent create = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                    create.addCategory(Intent.CATEGORY_OPENABLE);
                    create.setType("application/zip");
                    create.putExtra(Intent.EXTRA_TITLE, safeProjectFileName(editorProjectName) + ".zip");
                    startActivityForResult(create, PICK_EDITOR_EXPORT);
                });
            } catch (Exception e) { toast("ZIP export failed: " + safeMessage(e)); }
        }
        @JavascriptInterface public void buildEditorProject(String projectName, String html64, String css64, String js64,
                String appName, String pkg, String version, int versionCode,
                boolean camera, boolean mic, boolean locationApprox, boolean locationPrecise,
                boolean notifications, boolean keepScreenAwake, boolean blockScreenshots,
                boolean phoneDialer, boolean vibration, boolean contactPicker, boolean addContact,
                boolean viewCalendar, boolean calendarInsert, boolean creatorWatermark, boolean enableAiApi,
                String splashStyle, String splashSpeed, String extraPath, String extraContent64,
                String appOrientation, boolean fullscreen, boolean pullToRefresh, String backBehavior,
                boolean externalLinks, String cacheMode, boolean allowZoom, boolean desktopMode, boolean downloadsEnabled) {
            try {
                persistEditorProject(projectName, html64, css64, js64);
                persistEditorExtraFile(extraPath, extraContent64);
                File zip = createEditorProjectZip();
                runOnUiThread(() -> {
                    Uri oldUri = sourceUri;
                    String oldMode = sourceMode;
                    String oldName = sourceName;
                    sourceUri = Uri.fromFile(zip);
                    sourceMode = "zip";
                    sourceName = "code-editor-project.zip";
                    beginBuild(appName, pkg, version, versionCode, camera, mic,
                            locationApprox, locationPrecise, notifications, keepScreenAwake,
                            blockScreenshots, phoneDialer, vibration, contactPicker, addContact,
                            viewCalendar, calendarInsert, creatorWatermark, enableAiApi, "", splashStyle, splashSpeed,
                            appOrientation, fullscreen, pullToRefresh, backBehavior, externalLinks, cacheMode, allowZoom, desktopMode, downloadsEnabled);
                    // beginBuild captures the editor ZIP synchronously before its worker starts.
                    sourceUri = oldUri;
                    sourceMode = oldMode;
                    sourceName = oldName;
                });
            } catch (Exception e) { toast("Could not prepare code project for APK build: " + safeMessage(e)); }
        }
        @JavascriptInterface public void installHistory(String path) {
            runOnUiThread(() -> { try { installApk(resolveHistoryApk(path)); } catch (Exception e) { toast("Install error: " + safeMessage(e)); } });
        }
        @JavascriptInterface public void shareHistory(String path) {
            runOnUiThread(() -> { try { shareApk(resolveHistoryApk(path)); } catch (Exception e) { toast("Share error: " + safeMessage(e)); } });
        }
        @JavascriptInterface public void exportHistory(String path) {
            runOnUiThread(() -> { try { exportApk(resolveHistoryApk(path)); } catch (Exception e) { toast("Export error: " + safeMessage(e)); } });
        }
    }

    private String safeMessage(Throwable error) {
        String message = error == null ? "Unknown error" : error.getMessage();
        return message == null || message.trim().isEmpty() ? error.getClass().getSimpleName() : message;
    }

    private File getEditorProjectRoot() {
        if (editorProjectRoot == null) {
            String relative = getPreferences(0).getString("editor_project_dir", "code-editor/projects/default");
            editorProjectRoot = new File(getFilesDir(), relative);
        }
        return editorProjectRoot;
    }

    private String relativeEditorPath(File root, File child) throws IOException {
        String base = root.getCanonicalPath() + File.separator;
        String path = child.getCanonicalPath();
        if (!path.startsWith(base)) throw new IOException("Editor path escaped its workspace");
        return path.substring(base.length()).replace(File.separatorChar, '/');
    }

    private File createEditorWorkspace(String projectName) throws IOException {
        String slug = safeProjectFileName(projectName);
        String relative = "code-editor/projects/" + slug + "-" + System.currentTimeMillis();
        File workspace = new File(getFilesDir(), relative);
        if (!workspace.mkdirs()) throw new IOException("Cannot create a new code project folder");
        editorProjectRoot = workspace;
        getPreferences(0).edit().putString("editor_project_dir", relative).apply();
        return workspace;
    }

    private void ensureEditorProject() {
        try {
            File root = getEditorProjectRoot();
            if (!root.isDirectory() && !root.mkdirs()) throw new IOException("Cannot create code workspace");
            android.content.SharedPreferences prefs = getPreferences(0);
            editorProjectName = prefs.getString("editor_project_name", "My Web Project");
            editorHtmlPath = prefs.getString("editor_html_path", "index.html");
            editorCssPath = prefs.getString("editor_css_path", "style.css");
            editorJsPath = prefs.getString("editor_js_path", "script.js");
            currentDesignOptionsJson = prefs.getString("design_options_json", "{}");
            File html = editorFile(editorHtmlPath);
            File css = editorFile(editorCssPath);
            File jsFile = editorFile(editorJsPath);
            if (!html.isFile()) {
                editorHtmlPath = "index.html";
                editorCssPath = "style.css";
                editorJsPath = "script.js";
                writeUtf8(editorFile(editorHtmlPath), defaultEditorHtml());
                writeUtf8(editorFile(editorCssPath), defaultEditorCss());
                writeUtf8(editorFile(editorJsPath), defaultEditorJs());
                saveEditorPaths();
            } else {
                if (!css.isFile()) writeUtf8(css, "/* Add your project styles here. */\n");
                if (!jsFile.isFile()) writeUtf8(jsFile, "// Add your project JavaScript here.\n");
            }
        } catch (Exception e) {
            toast("Code workspace unavailable: " + safeMessage(e));
        }
    }

    private String defaultEditorHtml() {
        return "<!doctype html>\n<html lang=\"en\">\n<head>\n  <meta charset=\"UTF-8\">\n  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n  <title>My Web App</title>\n  <link rel=\"stylesheet\" href=\"style.css\">\n</head>\n<body>\n  <main class=\"card\">\n    <h1>Hello, WEB2APK!</h1>\n    <p>Edit this project, preview it, then build your APK.</p>\n    <button id=\"helloButton\">Tap me</button>\n  </main>\n  <script src=\"script.js\"></script>\n</body>\n</html>\n";
    }
    private String defaultEditorCss() {
        return "* { box-sizing: border-box; }\nbody { margin: 0; min-height: 100vh; display: grid; place-items: center; font-family: system-ui, sans-serif; background: linear-gradient(135deg, #eef0ff, #e4fbff); }\n.card { width: min(90%, 420px); padding: 28px; border-radius: 24px; background: rgba(255,255,255,.78); box-shadow: 0 16px 44px rgba(60,70,130,.16); text-align: center; }\nbutton { border: 0; border-radius: 14px; padding: 12px 20px; background: #6b5ce7; color: white; font-weight: 700; }\n";
    }
    private String defaultEditorJs() {
        return "document.getElementById(\"helloButton\")?.addEventListener(\"click\", () => {\n  alert(\"Your WEB2APK project is working!\");\n});\n";
    }

    private void saveEditorPaths() {
        getPreferences(0).edit()
                .putString("editor_project_name", editorProjectName)
                .putString("editor_html_path", editorHtmlPath)
                .putString("editor_css_path", editorCssPath)
                .putString("editor_js_path", editorJsPath).apply();
        try {
            JSONObject meta = new JSONObject();
            meta.put("name", editorProjectName);
            meta.put("html", editorHtmlPath);
            meta.put("css", editorCssPath);
            meta.put("js", editorJsPath);
            writeUtf8(new File(getEditorProjectRoot(), ".web2apk-project.json"), meta.toString());
        } catch (Exception ignored) { }
    }

    private String safeProjectFileName(String value) {
        String safe = value == null ? "web-project" : value.replaceAll("[^A-Za-z0-9._-]+", "-").replaceAll("^-+|-+$", "");
        return safe.isEmpty() ? "web-project" : safe;
    }

    private File resolveEditorProjectFolder(String projectId) throws IOException {
        if (projectId == null || !projectId.matches("[A-Za-z0-9._-]{1,120}") || projectId.contains("..")) {
            throw new IOException("Invalid project identifier");
        }
        File root = new File(getFilesDir(), "code-editor/projects").getCanonicalFile();
        File candidate = new File(root, projectId).getCanonicalFile();
        if (!candidate.getParentFile().equals(root) || !candidate.isDirectory() || projectId.startsWith(".")) {
            throw new IOException("Project folder was not found");
        }
        return candidate;
    }

    private void switchEditorProject(File folder) throws IOException {
        File root = folder.getCanonicalFile();
        File projectRoot = new File(getFilesDir(), "code-editor/projects").getCanonicalFile();
        if (!root.getParentFile().equals(projectRoot) || !root.isDirectory()) throw new IOException("Project folder is outside the project library");
        editorProjectRoot = root;
        JSONObject meta = new JSONObject();
        File metaFile = new File(root, ".web2apk-project.json");
        if (metaFile.isFile()) {
            try { meta = new JSONObject(readUtf8(metaFile, 64 * 1024)); } catch (Exception ignored) { }
        }
        editorProjectName = meta.optString("name", root.getName().replaceFirst("-[0-9]{10,}$", "").replace('-', ' '));
        editorHtmlPath = meta.optString("html", "index.html");
        editorCssPath = meta.optString("css", "style.css");
        editorJsPath = meta.optString("js", "script.js");
        File html = new File(root, editorHtmlPath);
        if (!html.isFile() || !html.getCanonicalPath().startsWith(root.getCanonicalPath() + File.separator)) {
            html = findIndex(root);
            if (html == null) throw new IOException("Project has no index.html file");
            editorHtmlPath = relativeEditorPath(root, html);
        }
        String htmlText = readUtf8(html, 2 * 1024 * 1024);
        File css = new File(root, editorCssPath);
        if (!css.isFile() || !css.getCanonicalPath().startsWith(root.getCanonicalPath() + File.separator)) {
            String found = findReferencedAsset(htmlText, root, ".css");
            editorCssPath = found == null ? "style.css" : found;
        }
        File jsFile = new File(root, editorJsPath);
        if (!jsFile.isFile() || !jsFile.getCanonicalPath().startsWith(root.getCanonicalPath() + File.separator)) {
            String found = findReferencedAsset(htmlText, root, ".js");
            editorJsPath = found == null ? "script.js" : found;
        }
        if (!new File(root, editorCssPath).isFile()) writeUtf8(new File(root, editorCssPath), "/* Project stylesheet */\n");
        if (!new File(root, editorJsPath).isFile()) writeUtf8(new File(root, editorJsPath), "// Project JavaScript\n");
        String relative = "code-editor/projects/" + root.getName();
        getPreferences(0).edit().putString("editor_project_dir", relative).apply();
        saveEditorPaths();
    }

    private File findNewestEditorProject() {
        File root = new File(getFilesDir(), "code-editor/projects");
        File[] dirs = root.listFiles(File::isDirectory);
        if (dirs == null) return null;
        Arrays.sort(dirs, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
        for (File dir : dirs) {
            if (dir.getName().startsWith(".")) continue;
            if (findIndex(dir) != null) return dir;
        }
        return null;
    }

    private void copyProjectTree(File source, File destination, long[] total) throws IOException {
        File[] children = source.listFiles();
        if (children == null) return;
        for (File child : children) {
            String name = child.getName();
            if (".git".equals(name) || "node_modules".equals(name) || ".gradle".equals(name) || ".idea".equals(name) || "build".equals(name)) continue;
            File target = new File(destination, name);
            if (child.isDirectory()) {
                if (!target.mkdirs() && !target.isDirectory()) throw new IOException("Cannot copy project folder");
                copyProjectTree(child, target, total);
            } else if (child.isFile()) {
                total[0] += child.length();
                if (total[0] > 30L * 1024L * 1024L) throw new IOException("Project copy exceeds the 30 MB safety limit");
                File parent = target.getParentFile();
                if (parent != null && !parent.mkdirs() && !parent.isDirectory()) throw new IOException("Cannot create copied file folder");
                try (InputStream in = new FileInputStream(child); OutputStream out = new FileOutputStream(target)) {
                    byte[] buffer = new byte[8192]; int n;
                    while ((n = in.read(buffer)) != -1) out.write(buffer, 0, n);
                }
            }
        }
    }

    private String editorProjectsJson() {
        JSONArray result = new JSONArray();
        try {
            File root = new File(getFilesDir(), "code-editor/projects");
            File[] dirs = root.listFiles(File::isDirectory);
            if (dirs == null) return result.toString();
            Arrays.sort(dirs, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
            String active = getEditorProjectRoot().getCanonicalPath();
            for (File dir : dirs) {
                if (dir.getName().startsWith(".") || findIndex(dir) == null) continue;
                JSONObject meta = new JSONObject();
                File metaFile = new File(dir, ".web2apk-project.json");
                if (metaFile.isFile()) {
                    try { meta = new JSONObject(readUtf8(metaFile, 64 * 1024)); } catch (Exception ignored) { }
                }
                JSONObject item = new JSONObject();
                item.put("id", dir.getName());
                item.put("name", meta.optString("name", dir.getName().replaceFirst("-[0-9]{10,}$", "").replace('-', ' ')));
                item.put("modified", Math.max(dir.lastModified(), metaFile.isFile() ? metaFile.lastModified() : 0L));
                item.put("active", dir.getCanonicalPath().equals(active));
                result.put(item);
            }
        } catch (Exception ignored) { }
        return result.toString();
    }

    private File editorFile(String relative) throws IOException {
        String clean = relative == null ? "" : relative.replace('\\', '/');
        if (clean.startsWith("/") || clean.contains(":") || hasParentTraversal(clean)) {
            throw new IOException("Unsafe editor file path");
        }
        File root = getEditorProjectRoot();
        File target = new File(root, clean);
        if (!target.getCanonicalPath().startsWith(root.getCanonicalPath() + File.separator)) {
            throw new IOException("Editor file escaped project folder");
        }
        return target;
    }

    private void writeUtf8(File file, String content) throws IOException {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) throw new IOException("Cannot create project folder");
        try (OutputStream out = new FileOutputStream(file)) {
            out.write((content == null ? "" : content).getBytes(StandardCharsets.UTF_8));
        }
    }

    private String readUtf8(File file, int maxBytes) throws IOException {
        if (!file.isFile()) return "";
        if (file.length() > maxBytes) throw new IOException("Editor file is larger than the supported 2 MB limit: " + file.getName());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (InputStream in = new FileInputStream(file)) {
            byte[] buffer = new byte[8192];
            int n;
            while ((n = in.read(buffer)) != -1) {
                if (out.size() + n > maxBytes) throw new IOException("Editor file is larger than the supported limit");
                out.write(buffer, 0, n);
            }
        }
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }

    private String encodeEditorText(String text) {
        return Base64.encodeToString((text == null ? "" : text).getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);
    }

    private String decodeEditorText(String encoded) throws IOException {
        try {
            byte[] bytes = Base64.decode(encoded == null ? "" : encoded, Base64.DEFAULT);
            if (bytes.length > 2 * 1024 * 1024) throw new IOException("Each editor file is limited to 2 MB");
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw new IOException("Editor content encoding is invalid", e);
        }
    }

    /** Create a throttled safety snapshot without changing the editor UI or blocking saves. */
    private void maybeBackupEditorWorkspace() {
        File pending = null;
        try {
            File source = getEditorProjectRoot();
            File[] existing = source.listFiles();
            if (!source.isDirectory() || existing == null || existing.length == 0) return;
            android.content.SharedPreferences prefs = getPreferences(0);
            long now = System.currentTimeMillis();
            String backupClockKey = "editor_last_backup_time_" + source.getName();
            long last = prefs.getLong(backupClockKey, 0L);
            if (now - last < EDITOR_BACKUP_INTERVAL_MS) return;

            File root = new File(getFilesDir(), "code-editor/backups/" + safeProjectFileName(editorProjectName));
            if (!root.exists() && !root.mkdirs()) return;
            pending = new File(root, ".pending-" + now);
            if (!pending.mkdirs()) return;
            long[] copied = new long[]{0L};
            copyEditorBackupTree(source, pending, copied);
            File[] contents = pending.listFiles();
            if (contents == null || contents.length == 0) throw new IOException("Backup snapshot is empty");
            File snapshot = new File(root, "snapshot-" + now);
            if (!pending.renameTo(snapshot)) throw new IOException("Could not finalize editor backup");
            pending = null;
            prefs.edit().putLong(backupClockKey, now).apply();
            pruneEditorBackups(root);
        } catch (Exception ignored) {
            // Saving the current project must continue even when the safety copy cannot be made.
        } finally {
            if (pending != null && pending.exists()) deleteTree(pending);
        }
    }

    private void copyEditorBackupTree(File source, File destination, long[] copied) throws IOException {
        File[] children = source.listFiles();
        if (children == null) return;
        for (File child : children) {
            String name = child.getName();
            if (child.isDirectory() && (".git".equals(name) || "node_modules".equals(name)
                    || ".gradle".equals(name) || ".idea".equals(name) || "build".equals(name))) continue;
            File target = new File(destination, name);
            if (child.isDirectory()) {
                if (!target.mkdirs() && !target.isDirectory()) throw new IOException("Cannot create backup folder");
                copyEditorBackupTree(child, target, copied);
            } else if (child.isFile()) {
                if (child.length() > EDITOR_BACKUP_MAX_BYTES - copied[0]) {
                    throw new IOException("Editor backup exceeds the safety-copy size limit");
                }
                File parent = target.getParentFile();
                if (parent != null && !parent.mkdirs() && !parent.isDirectory()) throw new IOException("Cannot create backup folder");
                try (InputStream in = new FileInputStream(child); FileOutputStream out = new FileOutputStream(target)) {
                    byte[] buffer = new byte[8192];
                    int count;
                    while ((count = in.read(buffer)) != -1) {
                        copied[0] += count;
                        if (copied[0] > EDITOR_BACKUP_MAX_BYTES) throw new IOException("Editor backup exceeds the safety-copy size limit");
                        out.write(buffer, 0, count);
                    }
                    out.getFD().sync();
                }
            }
        }
    }

    private void pruneEditorBackups(File root) {
        File[] backups = root.listFiles(File::isDirectory);
        if (backups == null || backups.length <= EDITOR_BACKUP_KEEP) return;
        Arrays.sort(backups, (a, b) -> Long.compare(a.lastModified(), b.lastModified()));
        for (int i = 0; i < backups.length - EDITOR_BACKUP_KEEP; i++) deleteTree(backups[i]);
    }

    private void createNewEditorProject(String name) throws IOException {
        ensureEditorProject();
        maybeBackupEditorWorkspace();
        editorProjectName = (name == null || name.trim().isEmpty()) ? "My Web Project" : name.trim();
        File root = createEditorWorkspace(editorProjectName);
        editorHtmlPath = "index.html";
        editorCssPath = "style.css";
        editorJsPath = "script.js";
        writeUtf8(editorFile(editorHtmlPath), defaultEditorHtml());
        writeUtf8(editorFile(editorCssPath), defaultEditorCss());
        writeUtf8(editorFile(editorJsPath), defaultEditorJs());
        saveEditorPaths();
    }

    private void persistEditorProject(String name, String html64, String css64, String js64) throws IOException {
        ensureEditorProject();
        maybeBackupEditorWorkspace();
        String html = decodeEditorText(html64);
        String css = decodeEditorText(css64);
        String js = decodeEditorText(js64);
        if (html.trim().isEmpty()) throw new IOException("index.html cannot be empty");
        long total = html.getBytes(StandardCharsets.UTF_8).length + css.getBytes(StandardCharsets.UTF_8).length + js.getBytes(StandardCharsets.UTF_8).length;
        if (total > 5L * 1024L * 1024L) throw new IOException("Editor code total exceeds 5 MB");
        editorProjectName = (name == null || name.trim().isEmpty()) ? "My Web Project" : name.trim();
        writeUtf8(editorFile(editorHtmlPath), html);
        writeUtf8(editorFile(editorCssPath), css);
        writeUtf8(editorFile(editorJsPath), js);
        saveEditorPaths();
    }

    private void loadEditorProjectToUi() {
        try {
            ensureEditorProject();
            String html = readUtf8(editorFile(editorHtmlPath), 2 * 1024 * 1024);
            String css = readUtf8(editorFile(editorCssPath), 2 * 1024 * 1024);
            String jsText = readUtf8(editorFile(editorJsPath), 2 * 1024 * 1024);
            js("loadEditorProject(" + quote(editorProjectName) + "," + quote(encodeEditorText(html)) + "," +
                    quote(encodeEditorText(css)) + "," + quote(encodeEditorText(jsText)) + ")");
        } catch (Exception e) { toast("Could not load code project: " + safeMessage(e)); }
    }

    private File findByExtension(File root, String extension) {
        File[] children = root.listFiles();
        if (children == null) return null;
        Arrays.sort(children, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        for (File child : children) {
            if (child.isFile() && child.getName().toLowerCase(Locale.ROOT).endsWith(extension)) return child;
        }
        for (File child : children) if (child.isDirectory()) {
            if (".git".equals(child.getName()) || "node_modules".equals(child.getName())) continue;
            File found = findByExtension(child, extension);
            if (found != null) return found;
        }
        return null;
    }

    private String findReferencedAsset(String html, File root, String extension) throws IOException {
        String attribute = ".css".equals(extension) ? "href" : "src";
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(attribute + "\\s*=\\s*[\\\"']([^\\\"']+" + java.util.regex.Pattern.quote(extension) + "(?:\\?[^\\\"']*)?)[\\\"']", java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher matcher = pattern.matcher(html == null ? "" : html);
        while (matcher.find()) {
            String value = matcher.group(1).split("[?#]", 2)[0];
            if (value.startsWith("http:") || value.startsWith("https:") || value.startsWith("//") || value.startsWith("data:")) continue;
            try {
                File candidate = new File(root, value);
                if (candidate.getCanonicalPath().startsWith(root.getCanonicalPath() + File.separator) && candidate.isFile()) {
                    return relativeEditorPath(root, candidate);
                }
            } catch (Exception ignored) { }
        }
        File found = findByExtension(root, extension);
        if (found == null) return null;
        return relativeEditorPath(root, found);
    }

    private void importEditorZip(Uri uri) throws Exception {
        // Import into a staging folder first. Do not switch the active project until the ZIP
        // has been safely extracted and a valid index page has been found.
        String importedName = safeProjectFileName(displayName(uri).replaceFirst("(?i)\\.zip$", ""));
        File projects = new File(getFilesDir(), "code-editor/projects");
        if (!projects.isDirectory() && !projects.mkdirs()) throw new IOException("Cannot create project folder");
        File staging = new File(projects, ".import-" + UUID.randomUUID());
        if (!staging.mkdirs()) throw new IOException("Cannot create ZIP import workspace");
        try {
            extractPreviewZip(uri, staging);
            File index = findIndex(staging);
            if (index == null) throw new IOException("Project ZIP must contain index.html or index.htm");
            String newHtmlPath = relativeEditorPath(staging, index);
            String html = readUtf8(index, 2 * 1024 * 1024);
            String cssPath = findReferencedAsset(html, staging, ".css");
            String jsPath = findReferencedAsset(html, staging, ".js");
            String newCssPath = cssPath == null ? "style.css" : cssPath;
            String newJsPath = jsPath == null ? "script.js" : jsPath;
            if (!new File(staging, newCssPath).isFile()) writeUtf8(new File(staging, newCssPath), "/* Project stylesheet */\n");
            if (!new File(staging, newJsPath).isFile()) writeUtf8(new File(staging, newJsPath), "// Project JavaScript\n");

            String relative = "code-editor/projects/" + importedName + "-" + System.currentTimeMillis();
            File committed = new File(getFilesDir(), relative);
            if (committed.exists()) throw new IOException("A project with this timestamp already exists; please retry");
            if (!staging.renameTo(committed)) throw new IOException("Could not finalize imported project");

            // Switch active project only after successful extraction and validation.
            editorProjectRoot = committed;
            editorProjectName = importedName;
            editorHtmlPath = newHtmlPath;
            editorCssPath = newCssPath;
            editorJsPath = newJsPath;
            getPreferences(0).edit().putString("editor_project_dir", relative).apply();
            saveEditorPaths();
            loadEditorProjectToUi();
            toast("Project ZIP opened. Edit HTML, CSS and JavaScript tabs.");
        } catch (Exception e) {
            deleteTree(staging);
            throw e;
        }
    }

    private String copyEditorAsset(Uri uri) throws IOException {
        ensureEditorProject();
        String name = displayName(uri).replaceAll("[^A-Za-z0-9._-]+", "_");
        if (name.isEmpty() || name.equals(".") || name.equals("..")) name = "project_asset";
        File assets = new File(getEditorProjectRoot(), "assets");
        if (!assets.exists() && !assets.mkdirs()) throw new IOException("Cannot create assets folder");
        File output = new File(assets, name);
        if (output.exists()) {
            String base = name;
            String extension = "";
            int dot = name.lastIndexOf('.');
            if (dot > 0) { base = name.substring(0, dot); extension = name.substring(dot); }
            int suffix = 2;
            do { output = new File(assets, base + "-" + suffix++ + extension); }
            while (output.exists() && suffix < 10000);
            if (output.exists()) throw new IOException("Too many files with the same name");
        }
        try (InputStream in = getContentResolver().openInputStream(uri); OutputStream out = new FileOutputStream(output)) {
            if (in == null) throw new IOException("Cannot read selected file");
            byte[] buffer = new byte[8192];
            long total = 0;
            int n;
            while ((n = in.read(buffer)) != -1) {
                total += n;
                if (total > 20L * 1024L * 1024L) throw new IOException("Each added project asset is limited to 20 MB");
                out.write(buffer, 0, n);
            }
        }
        return relativeEditorPath(getEditorProjectRoot(), output);
    }

    private void openEditorFileInCodeBox(Uri uri) throws IOException {
        String path = copyEditorAsset(uri);
        String name = displayName(uri);
        String lower = name.toLowerCase(Locale.ROOT);
        String type;
        if (lower.endsWith(".html") || lower.endsWith(".htm")) {
            type = "html"; editorHtmlPath = path;
        } else if (lower.endsWith(".css")) {
            type = "css"; editorCssPath = path;
        } else if (lower.endsWith(".js") || lower.endsWith(".mjs") || lower.endsWith(".cjs")) {
            type = "js"; editorJsPath = path;
        } else {
            String[] textExt = {".json", ".jsonc", ".xml", ".java", ".kt", ".kts", ".ts", ".tsx", ".jsx", ".py", ".md", ".mdx", ".txt", ".log", ".yml", ".yaml", ".sql", ".sh", ".bash", ".gradle", ".properties", ".scss", ".less", ".svg", ".csv", ".ini", ".conf", ".cfg", ".toml", ".env", ".php", ".vue", ".svelte", ".astro", ".dart", ".rb", ".go", ".rs", ".c", ".h", ".cpp", ".cs", ".bat", ".ps1", ".gitignore"};
            type = "asset";
            for (String ext : textExt) if (lower.endsWith(ext)) { type = "extra"; break; }
        }
        saveEditorPaths();
        if ("extra".equals(type) || "html".equals(type) || "css".equals(type) || "js".equals(type)) {
            String content = readUtf8(editorFile(path), 2 * 1024 * 1024);
            js("editorFileLoaded(" + quote(type) + "," + quote(name) + "," + quote(path) + "," + quote(encodeEditorText(content)) + ")");
            toast("File opened in Code Editor: " + name);
        } else {
            js("editorAssetAdded(" + quote(path) + ")");
            toast("Project asset added: " + path);
        }
    }

    private File createEditorProjectZip() throws IOException {
        ensureEditorProject();
        File zipFile = new File(getCacheDir(), "web2apk-editor-project-" + System.currentTimeMillis() + ".zip");
        File root = getEditorProjectRoot();
        final long[] total = {0L};
        try (ZipOutputStream zip = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(zipFile)))) {
            addEditorFolderToZip(root, root, zip, total);
        }
        if (!zipFile.isFile() || zipFile.length() < 100L) throw new IOException("Could not create editor ZIP");
        return zipFile;
    }

    private void addEditorFolderToZip(File root, File current, ZipOutputStream zip, long[] total) throws IOException {
        File[] children = current.listFiles();
        if (children == null) return;
        Arrays.sort(children, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        byte[] buffer = new byte[8192];
        for (File child : children) {
            if (child.isDirectory()) {
                if (".git".equalsIgnoreCase(child.getName()) || "node_modules".equalsIgnoreCase(child.getName())) continue;
                addEditorFolderToZip(root, child, zip, total);
            } else if (child.isFile()) {
                String relative = relativeEditorPath(root, child);
                if (relative.startsWith(".") || relative.contains("../") || relative.endsWith("~")) continue;
                total[0] += child.length();
                if (total[0] > 120L * 1024L * 1024L) throw new IOException("Project files exceed the 120 MB build limit");
                zip.putNextEntry(new ZipEntry(relative));
                try (InputStream in = new FileInputStream(child)) {
                    int n;
                    while ((n = in.read(buffer)) != -1) zip.write(buffer, 0, n);
                }
                zip.closeEntry();
            }
        }
    }

    private void showPreview(String url) {
        try {
            final WebView preview = new WebView(this);
            preview.getSettings().setJavaScriptEnabled(true);
            preview.getSettings().setDomStorageEnabled(true);
            preview.getSettings().setAllowFileAccess(true);
            preview.getSettings().setAllowContentAccess(true);
            preview.setWebViewClient(new WebViewClient());
            if (sourceUri != null && "html".equals(sourceMode)) {
                String html = readText(sourceUri);
                preview.loadDataWithBaseURL("https://preview.local/", html, "text/html", "UTF-8", null);
            } else if (sourceUri != null && "zip".equals(sourceMode)) {
                File root = new File(getCacheDir(), "web2apk-preview");
                deleteTree(root);
                if (!root.mkdirs() && !root.isDirectory()) throw new IOException("Cannot create preview folder");
                extractPreviewZip(sourceUri, root);
                File index = findIndex(root);
                if (index == null) throw new IOException("ZIP ထဲမှာ index.html မတွေ့ပါ");
                preview.loadUrl(Uri.fromFile(index).toString());
            } else if (url != null && (url.startsWith("https://") || url.startsWith("http://"))) {
                preview.loadUrl(url.trim());
            } else {
                new AlertDialog.Builder(this).setTitle("Live Preview")
                        .setMessage("HTML / ZIP ရွေးပါ သို့မဟုတ် http(s) Website URL ထည့်ပါ။")
                        .setPositiveButton("OK", null).show();
                return;
            }
            new AlertDialog.Builder(this).setTitle("Live Preview").setView(preview)
                    .setPositiveButton("Close", (dialog, which) -> preview.destroy()).show();
        } catch (Exception e) {
            toast("Preview failed: " + e.getMessage());
        }
    }

    private android.content.SharedPreferences builderAiPrefs() {
        return getSharedPreferences(BUILDER_AI_PREFS, MODE_PRIVATE);
    }

    private String builderAiSettingsJson() {
        android.content.SharedPreferences prefs = builderAiPrefs();
        if (!prefs.getBoolean("v463_safe_defaults", false)) {
            // One-time migration for earlier builds, which defaulted to embedding secrets.
            prefs.edit().putBoolean("embed_key", false).putBoolean("v463_safe_defaults", true).commit();
        }
        String endpoint = prefs.getString("endpoint", DEFAULT_AI_ENDPOINT);
        // Migrate the old Google OpenAI-compatible URL to Google's native Interactions API.
        if ("https://generativelanguage.googleapis.com/v1beta/openai/chat/completions".equals(endpoint)) {
            endpoint = DEFAULT_AI_ENDPOINT;
            prefs.edit().putString("endpoint", endpoint).commit();
        }
        String model = prefs.getString("model", DEFAULT_AI_MODEL);
        boolean hasKey = !prefs.getString("api_key_enc", "").isEmpty();
        boolean embed = prefs.getBoolean("embed_key", false);
        try {
            JSONObject result = new JSONObject();
            result.put("endpoint", endpoint); result.put("model", model);
            result.put("hasKey", hasKey); result.put("embed", embed);
            return result.toString();
        } catch (Exception ignored) { return "{}"; }
    }

    private String saveBuilderAiSettingsInternal(String rawEndpoint, String rawModel, String rawKey, boolean embedKey) {
        try {
            String endpoint = normalizeBuilderAiEndpoint(rawEndpoint);
            String model = rawModel == null ? "" : rawModel.trim();
            String key = rawKey == null ? "" : rawKey.trim();
            if (model.isEmpty() || model.length() > 120) throw new IllegalArgumentException("Model name is required (maximum 120 characters)");
            if (key.length() > 4096) throw new IllegalArgumentException("API key is too long");
            android.content.SharedPreferences.Editor edit = builderAiPrefs().edit()
                    .putString("endpoint", endpoint).putString("model", model).putBoolean("embed_key", embedKey);
            if (!key.isEmpty()) edit.putString("api_key_enc", encryptBuilderAiKey(key));
            if (!edit.commit()) throw new IOException("Could not save API settings");
            JSONObject result = new JSONObject();
            result.put("ok", true); result.put("hasKey", !builderAiPrefs().getString("api_key_enc", "").isEmpty());
            result.put("message", "API settings saved securely on this device");
            return result.toString();
        } catch (Exception e) {
            try { JSONObject result = new JSONObject(); result.put("ok", false); result.put("error", safeMessage(e)); return result.toString(); }
            catch (Exception ignored) { return "{\"ok\":false}"; }
        }
    }

    private String normalizeBuilderAiEndpoint(String raw) {
        String value = raw == null ? "" : raw.trim();
        Uri uri = Uri.parse(value);
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null) {
            throw new IllegalArgumentException("Use a valid HTTPS Gemini or OpenAI-compatible API endpoint");
        }
        if (value.length() > 2048 || uri.getFragment() != null) throw new IllegalArgumentException("Endpoint URL is invalid or too long");
        String path = uri.getPath() == null ? "" : uri.getPath();
        if (!(path.endsWith("/chat/completions") || path.endsWith("/interactions")
                || path.endsWith("/v1beta") || path.endsWith("/v1")
                || path.endsWith("/openai") || path.isEmpty())) {
            throw new IllegalArgumentException("Use Gemini /v1beta/interactions or an OpenAI-compatible /chat/completions endpoint");
        }
        value = value.replaceAll("/+$", "");
        if (value.endsWith("/chat/completions") || value.endsWith("/interactions")) return value;
        if (value.endsWith("/v1beta")) return value + "/interactions";
        if (value.endsWith("/v1") || value.endsWith("/openai")) return value + "/chat/completions";
        return value + "/chat/completions";
    }

    private SecretKey getBuilderAiEncryptionKey() throws Exception {
        KeyStore store = KeyStore.getInstance("AndroidKeyStore");
        store.load(null);
        if (!store.containsAlias(BUILDER_AI_KEY_ALIAS)) {
            KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
            KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(BUILDER_AI_KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(true).build();
            generator.init(spec); generator.generateKey();
        }
        store.load(null);
        return (SecretKey) store.getKey(BUILDER_AI_KEY_ALIAS, null);
    }

    private String encryptBuilderAiKey(String plain) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, getBuilderAiEncryptionKey());
        byte[] encrypted = cipher.doFinal(plain.getBytes("UTF-8"));
        byte[] iv = cipher.getIV();
        byte[] joined = new byte[iv.length + encrypted.length];
        System.arraycopy(iv, 0, joined, 0, iv.length);
        System.arraycopy(encrypted, 0, joined, iv.length, encrypted.length);
        return Base64.encodeToString(joined, Base64.NO_WRAP);
    }

    private String decryptBuilderAiKey(String encoded) throws Exception {
        if (encoded == null || encoded.isEmpty()) return "";
        byte[] all = Base64.decode(encoded, Base64.DEFAULT);
        if (all.length <= 12) throw new IOException("Saved API key is invalid. Enter it again.");
        byte[] iv = Arrays.copyOfRange(all, 0, 12);
        byte[] body = Arrays.copyOfRange(all, 12, all.length);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, getBuilderAiEncryptionKey(), new GCMParameterSpec(128, iv));
        return new String(cipher.doFinal(body), "UTF-8");
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

    private String extractGeminiInteractionText(String responseText) throws Exception {
        JSONObject response = new JSONObject(responseText);
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
        // Accept the earlier Interactions response shape as a compatibility fallback.
        JSONArray outputs = response.optJSONArray("outputs");
        if (answer.length() == 0 && outputs != null) {
            for (int i = 0; i < outputs.length(); i++) {
                JSONObject part = outputs.optJSONObject(i);
                if (part != null && "text".equals(part.optString("type"))) answer.append(part.optString("text", ""));
            }
        }
        if (answer.toString().trim().isEmpty()) {
            String status = response.optString("status", "");
            throw new IOException("Gemini returned no text" + (status.isEmpty() ? "." : " (status: " + status + ")."));
        }
        return answer.toString().trim();
    }

    private void runBuilderAiTest() {
        new Thread(() -> {
            HttpsURLConnection connection = null;
            try {
                android.content.SharedPreferences prefs = builderAiPrefs();
                String endpoint = normalizeBuilderAiEndpoint(prefs.getString("endpoint", DEFAULT_AI_ENDPOINT));
                String model = prefs.getString("model", DEFAULT_AI_MODEL);
                String encrypted = prefs.getString("api_key_enc", "");
                if (encrypted.isEmpty()) throw new IOException("Paste and save your Gemini API key first");
                String key = decryptBuilderAiKey(encrypted);
                boolean gemini = isGeminiInteractionsEndpoint(endpoint);
                JSONObject bodyJson = new JSONObject();
                bodyJson.put("model", model);
                if (gemini) {
                    bodyJson.put("input", "Reply with exactly OK.");
                    bodyJson.put("store", false);
                } else {
                    JSONObject message = new JSONObject(); message.put("role", "user"); message.put("content", "Reply with exactly OK.");
                    org.json.JSONArray messages = new org.json.JSONArray(); messages.put(message);
                    bodyJson.put("messages", messages); bodyJson.put("stream", false);
                }
                byte[] body = bodyJson.toString().getBytes("UTF-8");
                connection = (HttpsURLConnection) new URL(endpoint).openConnection();
                connection.setRequestMethod("POST"); connection.setConnectTimeout(15000); connection.setReadTimeout(35000);
                connection.setDoOutput(true);
                if (gemini) connection.setRequestProperty("x-goog-api-key", key);
                else connection.setRequestProperty("Authorization", "Bearer " + key);
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                connection.setRequestProperty("Accept", "application/json"); connection.setFixedLengthStreamingMode(body.length);
                try (OutputStream out = connection.getOutputStream()) { out.write(body); }
                int code = connection.getResponseCode();
                InputStream responseStream = code >= 200 && code < 300 ? connection.getInputStream() : connection.getErrorStream();
                String responseText = "";
                if (responseStream != null) {
                    ByteArrayOutputStream out = new ByteArrayOutputStream(); byte[] buf = new byte[2048]; int n;
                    while ((n = responseStream.read(buf)) > 0 && out.size() < 2 * 1024 * 1024) out.write(buf, 0, n);
                    responseText = out.toString("UTF-8"); responseStream.close();
                }
                if (code < 200 || code >= 300) {
                    String detail = responseText;
                    try { JSONObject error = new JSONObject(responseText).optJSONObject("error"); if (error != null) detail = error.optString("message", responseText); } catch (Exception ignored) { }
                    throw new IOException("API HTTP " + code + ": " + (detail.length() > 350 ? detail.substring(0, 350) : detail));
                }
                if (gemini) extractGeminiInteractionText(responseText);
                else {
                    JSONObject response = new JSONObject(responseText);
                    JSONArray choices = response.optJSONArray("choices");
                    if (choices == null || choices.length() == 0) throw new IOException("API response has no choices / message content");
                }
                js("aiApiTestResult(true," + JSONObject.quote((gemini ? "Gemini Interactions API" : "Chat Completions API") + " test successful · HTTP " + code) + ")");
            } catch (Exception e) {
                js("aiApiTestResult(false," + JSONObject.quote(safeMessage(e)) + ")");
            } finally { if (connection != null) connection.disconnect(); }
        }, "WEB2APK-AI-Test").start();
    }

    private void beginBuild(String name, String packageId, String versionName, int versionCode,
                            boolean camera, boolean microphone, boolean locationApprox, boolean locationPrecise,
                            boolean notifications, boolean keepScreenAwake, boolean blockScreenshots,
                            boolean phoneDialer, boolean vibration, boolean contactPicker,
                            boolean addContact, boolean viewCalendar, boolean calendarInsert, boolean creatorWatermark, boolean enableAiApi, String websiteUrl, String splashStyle, String splashSpeed,
                            String appOrientation, boolean fullscreen, boolean pullToRefresh, String backBehavior,
                            boolean externalLinks, String cacheMode, boolean allowZoom, boolean desktopMode, boolean downloadsEnabled) {
        name = name == null ? "" : name.trim();
        packageId = packageId == null ? "" : packageId.trim();
        versionName = versionName == null ? "1.0.0" : versionName.trim();
        websiteUrl = websiteUrl == null ? "" : websiteUrl.trim();
        if (buildRunning) { toast("A build is already running."); return; }
        if (name.isEmpty() || name.length() > 40) { toast("App Name must contain 1–40 characters."); return; }
        if (!packageId.matches("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+")) {
            toast("Package ID ဥပမာ com.example.myapp ပုံစံဖြစ်ရပါမယ်။"); return;
        }
        if (!versionName.matches("[0-9A-Za-z._-]{1,30}") || versionCode < 1 || versionCode > 2100000000) {
            toast("Version Name / Code မမှန်ပါ။"); return;
        }
        if (sourceUri == null && !(websiteUrl.startsWith("https://") || websiteUrl.startsWith("http://"))) {
            toast("HTML / ZIP ရွေးပါ သို့မဟုတ် http(s) Website URL ထည့်ပါ။"); return;
        }
        if (sourceUri != null && !("html".equals(sourceMode) || "zip".equals(sourceMode))) {
            toast("Source type မမှန်ပါ။"); return;
        }
        if (sourceUri != null && "html".equals(sourceMode)
                && !(sourceName.toLowerCase(Locale.ROOT).endsWith(".html") || sourceName.toLowerCase(Locale.ROOT).endsWith(".htm"))) {
            toast("HTML mode အတွက် .html သို့မဟုတ် .htm ဖိုင်ရွေးပါ။"); return;
        }
        if (sourceUri != null && "zip".equals(sourceMode)
                && !sourceName.toLowerCase(Locale.ROOT).endsWith(".zip")) {
            toast("ZIP mode အတွက် .zip ဖိုင်ရွေးပါ။"); return;
        }

        android.content.SharedPreferences aiPrefs = builderAiPrefs();
        final String fAiEndpoint = aiPrefs.getString("endpoint", DEFAULT_AI_ENDPOINT);
        final String fAiModel = aiPrefs.getString("model", DEFAULT_AI_MODEL);
        final boolean fEmbedAiKey = enableAiApi && aiPrefs.getBoolean("embed_key", false);
        final String fAiKey;
        try {
            String encryptedAiKey = aiPrefs.getString("api_key_enc", "");
            fAiKey = fEmbedAiKey ? decryptBuilderAiKey(encryptedAiKey) : "";
            if (fEmbedAiKey && fAiKey.isEmpty()) {
                toast("API Key မထည့်ရသေးပါ။ Notification > AI API Settings ထဲမှာ key ထည့်ပြီး Save လုပ်ပါ။");
                return;
            }
            if (enableAiApi) normalizeBuilderAiEndpoint(fAiEndpoint);
        } catch (Exception apiError) {
            toast("AI API settings error: " + safeMessage(apiError)); return;
        }

        buildRunning = true;
        lastProgressNotification = -1;
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        updateStatus("Starting internal APK builder…", 5);

        final String fName = name;
        final String fPackage = packageId;
        final String fVersion = versionName;
        final String fUrl = websiteUrl;
        final int fVersionCode = versionCode;
        final boolean fCamera = camera;
        final boolean fMic = microphone;
        final boolean fLocationApprox = locationApprox;
        final boolean fLocationPrecise = locationPrecise;
        final boolean fNotifications = notifications;
        final boolean fKeepScreenAwake = keepScreenAwake;
        final boolean fBlockScreenshots = blockScreenshots;
        final boolean fCreatorWatermark = creatorWatermark;
        final boolean fEnableAiApi = enableAiApi;
        final boolean fPhoneDialer = phoneDialer;
        final boolean fVibration = vibration;
        final boolean fContactPicker = contactPicker;
        final boolean fAddContact = addContact;
        final boolean fViewCalendar = viewCalendar;
        final boolean fCalendarInsert = calendarInsert;
        final Uri fSourceUri = sourceUri;
        final String fSourceMode = sourceMode;
        final Uri fIconUri = iconUri;
        final Uri fSplashMediaUri = splashMediaUri;
        final String fSplashStyle = splashStyle == null ? "ios_zoom" : splashStyle;
        final String fSplashSpeed = splashSpeed == null ? "normal" : splashSpeed;
        final String fAppOrientation = appOrientation == null ? "system" : appOrientation;
        final boolean fFullscreen = fullscreen;
        final boolean fPullToRefresh = pullToRefresh;
        final String fBackBehavior = backBehavior == null ? "history" : backBehavior;
        final boolean fExternalLinks = externalLinks;
        final String fCacheMode = cacheMode == null ? "default" : cacheMode;
        final boolean fAllowZoom = allowZoom;
        final boolean fDesktopMode = desktopMode;
        final boolean fDownloadsEnabled = downloadsEnabled;
        final String fDesignOptionsJson = currentDesignOptionsJson == null ? "{}" : currentDesignOptionsJson;

        buildExecutor.execute(() -> {
            try {
                File apk = InternalApkBuilder.build(this, fSourceUri, fSourceMode, fIconUri,
                        fName, fPackage, fVersion, fVersionCode,
                        fCamera, fMic, fLocationApprox, fLocationPrecise, fNotifications,
                        fKeepScreenAwake, fBlockScreenshots, fCreatorWatermark, fPhoneDialer, fVibration,
                        fContactPicker, fAddContact, fViewCalendar, fCalendarInsert, fEnableAiApi,
                        fAiEndpoint, fAiModel, fAiKey, fEmbedAiKey, fUrl, fSplashStyle, fSplashSpeed, fSplashMediaUri,
                        fAppOrientation, fFullscreen, fPullToRefresh, fBackBehavior, fExternalLinks, fCacheMode, fAllowZoom, fDesktopMode, fDownloadsEnabled,
                        fDesignOptionsJson,
                        (message, percent) -> runOnUiThread(() -> {
                            if (buildRunning) updateStatus(message, percent);
                        }));
                addHistory(fName, fPackage, fVersion,
                        new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(new Date()),
                        apk.getAbsolutePath());
                runOnUiThread(() -> {
                    buildRunning = false;
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    updateStatus("BUILD SUCCESSFUL · " + apk.getName(), 100);
                    notifyBuild("APK Build Complete", apk.getName());
                    installApk(apk);
                });
            } catch (Exception e) {
                String message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
                runOnUiThread(() -> {
                    buildRunning = false;
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    updateStatus("BUILD FAILED · " + message, 0);
                    notifyBuild("APK Build Failed", message);
                    Toast.makeText(this, "Build failed: " + message, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void notifyBuildProgress(String message, int percent) {
        if (percent == lastProgressNotification) return;
        lastProgressNotification = percent;
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return;
        try {
            android.app.NotificationManager manager = (android.app.NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (Build.VERSION.SDK_INT >= 26) {
                manager.createNotificationChannel(new android.app.NotificationChannel(
                        "web2apk_build", "WEB2APK Build", android.app.NotificationManager.IMPORTANCE_LOW));
            }
            android.app.Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                    ? new android.app.Notification.Builder(this, "web2apk_build")
                    : new android.app.Notification.Builder(this);
            builder.setSmallIcon(android.R.drawable.stat_sys_download)
                    .setContentTitle("Building website APK")
                    .setContentText(message)
                    .setOngoing(true)
                    .setProgress(100, Math.max(0, Math.min(100, percent)), false);
            manager.notify(5, builder.build());
        } catch (Exception ignored) { }
    }

    private void notifyBuild(String title, String message) {
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return;
        try {
            android.app.NotificationManager manager = (android.app.NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (Build.VERSION.SDK_INT >= 26) {
                manager.createNotificationChannel(new android.app.NotificationChannel(
                        "web2apk_build", "WEB2APK Build", android.app.NotificationManager.IMPORTANCE_DEFAULT));
            }
            android.app.Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                    ? new android.app.Notification.Builder(this, "web2apk_build")
                    : new android.app.Notification.Builder(this);
            builder.setSmallIcon(android.R.drawable.stat_sys_download_done)
                    .setContentTitle(title).setContentText(message).setAutoCancel(true);
            manager.notify(5, builder.build());
        } catch (Exception ignored) { }
    }

    private void addHistory(String name, String pkg, String version, String date, String path) {
        name = name == null ? "Website App" : name.replace("|", "/").replace("\n", " ").replace("\r", " ");
        pkg = pkg == null ? "" : pkg.replace("|", "/").replace("\n", " ").replace("\r", " ");
        version = version == null ? "" : version.replace("|", "/").replace("\n", " ").replace("\r", " ");
        date = date == null ? "" : date.replace("|", "/").replace("\n", " ").replace("\r", " ");
        path = path == null ? "" : path.replace("|", "/").replace("\n", "").replace("\r", "");
        String old = getPreferences(0).getString("history", "");
        ArrayList<String> rows = new ArrayList<>();
        for (String row : old.split("\n")) if (!row.trim().isEmpty()) rows.add(row);
        rows.add(name + "|" + pkg + "|" + version + "|" + date + "|" + path);
        while (rows.size() > 100) rows.remove(0);
        StringBuilder saved = new StringBuilder();
        for (String row : rows) saved.append(row).append('\n');
        getPreferences(0).edit().putString("history", saved.toString()).apply();
    }

    private File resolveHistoryApk(String path) throws IOException {
        if (path == null || path.trim().isEmpty()) throw new IOException("APK history path is empty");
        File candidate = new File(path).getCanonicalFile();
        if (!candidate.isFile() || !candidate.getName().toLowerCase(Locale.ROOT).endsWith(".apk")) {
            throw new IOException("The selected APK is no longer available");
        }
        boolean allowed = false;
        File external = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        File internal = new File(getFilesDir(), "downloads");
        File[] roots = new File[]{external, internal};
        for (File root : roots) {
            if (root == null) continue;
            String prefix = root.getCanonicalPath() + File.separator;
            if (candidate.getPath().startsWith(prefix)) { allowed = true; break; }
        }
        if (!allowed) throw new IOException("APK history path is outside WEB2APK's output folder");
        return candidate;
    }

    private String historyJson() {
        String[] rows = getPreferences(0).getString("history", "").split("\n");
        StringBuilder json = new StringBuilder("[");
        int count = 0;
        for (int i = rows.length - 1; i >= 0 && count < 50; i--) {
            String[] parts = rows[i].split("\\|", 5);
            if (parts.length < 5) continue;
            if (count++ > 0) json.append(',');
            json.append("{\"name\":\"").append(jsonEscape(parts[0]))
                    .append("\",\"pkg\":\"").append(jsonEscape(parts[1]))
                    .append("\",\"version\":\"").append(jsonEscape(parts[2]))
                    .append("\",\"date\":\"").append(jsonEscape(parts[3]))
                    .append("\",\"path\":\"").append(jsonEscape(parts[4])).append("\"}");
        }
        return json.append(']').toString();
    }

    private String jsonEscape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ").replace("\r", " ");
    }

    private String safeApkBaseName(String name) throws IOException {
        String safe = name == null ? "" : name.trim().replaceAll("(?i)\\.apk$", "")
                .replaceAll("[^A-Za-z0-9._ -]+", "_").replaceAll("\\s+", "_");
        safe = safe.replaceAll("^[_ .-]+|[_ .-]+$", "");
        if (safe.isEmpty() || safe.length() > 80) throw new IOException("APK filename must contain 1–80 safe characters");
        return safe;
    }

    private File uniqueApkFile(File parent, String base) throws IOException {
        File file = new File(parent, base + ".apk");
        int suffix = 2;
        while (file.exists()) {
            file = new File(parent, base + "_" + suffix++ + ".apk");
            if (suffix > 10000) throw new IOException("Too many files with this name");
        }
        return file;
    }

    private void replaceHistoryPath(String oldPath, String newPath, String newName) {
        String old = getPreferences(0).getString("history", "");
        StringBuilder updated = new StringBuilder();
        for (String row : old.split("\n")) {
            if (row.trim().isEmpty()) continue;
            String[] parts = row.split("\\|", 5);
            if (parts.length == 5 && parts[4].equals(oldPath)) {
                if (newName != null && !newName.isEmpty()) parts[0] = newName;
                parts[4] = newPath;
                updated.append(parts[0]).append('|').append(parts[1]).append('|').append(parts[2]).append('|')
                        .append(parts[3]).append('|').append(parts[4]).append('\n');
            } else updated.append(row).append('\n');
        }
        getPreferences(0).edit().putString("history", updated.toString()).apply();
    }

    private void renameHistoryApk(String path, String newName) {
        try {
            File source = resolveHistoryApk(path);
            File destination = uniqueApkFile(source.getParentFile(), safeApkBaseName(newName));
            if (!source.renameTo(destination)) throw new IOException("Could not rename APK file");
            replaceHistoryPath(source.getAbsolutePath(), destination.getAbsolutePath(), destination.getName().replaceFirst("(?i)\\.apk$", ""));
            toast("APK renamed: " + destination.getName());
            js("setHistory(" + quote(historyJson()) + ")");
        } catch (Exception e) { toast("Rename APK failed: " + safeMessage(e)); }
    }

    private void copyHistoryApk(String path, String newName) {
        try {
            File source = resolveHistoryApk(path);
            File destination = uniqueApkFile(source.getParentFile(), safeApkBaseName(newName));
            try (InputStream in = new FileInputStream(source); OutputStream out = new FileOutputStream(destination)) {
                byte[] buffer = new byte[16384]; int n;
                while ((n = in.read(buffer)) != -1) out.write(buffer, 0, n);
                out.flush();
            } catch (Exception e) { if (destination.exists()) destination.delete(); throw e; }
            if (destination.length() != source.length()) { destination.delete(); throw new IOException("Copied APK size does not match the original"); }
            addHistory(destination.getName().replaceFirst("(?i)\\.apk$", ""), "", "copy", 
                    new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(new Date()), destination.getAbsolutePath());
            toast("APK copy created: " + destination.getName());
            js("setHistory(" + quote(historyJson()) + ")");
        } catch (Exception e) { toast("Copy APK failed: " + safeMessage(e)); }
    }

    private void installApk(File apk) {
        try {
            if (!apk.isFile()) { toast("APK file not found."); return; }
            if (Build.VERSION.SDK_INT >= 26 && !getPackageManager().canRequestPackageInstalls()) {
                pendingInstallPath = apk.getAbsolutePath();
                startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:" + getPackageName())));
                toast("Allow WEB2APK to install unknown apps. The installer will open when you return.");
                return;
            }
            pendingInstallPath = "";
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".files", apk);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            toast("Install error: " + e.getMessage());
        }
    }


    private void exportApk(File apk) {
        try {
            if (!apk.isFile()) { toast("APK file not found."); return; }
            pendingExport = apk;
            Intent create = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            create.addCategory(Intent.CATEGORY_OPENABLE);
            create.setType("application/vnd.android.package-archive");
            create.putExtra(Intent.EXTRA_TITLE, apk.getName());
            startActivityForResult(create, PICK_EXPORT);
        } catch (Exception e) {
            pendingExport = null;
            toast("Export error: " + e.getMessage());
        }
    }

    private void shareApk(File apk) {
        try {
            if (!apk.isFile()) { toast("APK file not found."); return; }
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".files", apk);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("application/vnd.android.package-archive");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Share APK"));
        } catch (Exception e) {
            toast("Share error: " + e.getMessage());
        }
    }

    private String readText(Uri uri) throws Exception {
        StringBuilder text = new StringBuilder();
        try (InputStreamReader reader = new InputStreamReader(getContentResolver().openInputStream(uri), StandardCharsets.UTF_8);
             BufferedReader buffered = new BufferedReader(reader)) {
            String line;
            while ((line = buffered.readLine()) != null) text.append(line).append('\n');
        }
        return text.toString();
    }

    private void extractPreviewZip(Uri uri, File root) throws Exception {
        try (InputStream raw = getContentResolver().openInputStream(uri); ZipInputStream zip = new ZipInputStream(raw)) {
            if (raw == null) throw new IOException("Cannot open selected ZIP");
            ZipEntry entry;
            byte[] buffer = new byte[8192];
            long total = 0L;
            int count = 0;
            while ((entry = zip.getNextEntry()) != null) {
                if (++count > 3000) throw new IOException("Too many ZIP entries");
                String name = entry.getName().replace('\\', '/');
                if (name.startsWith("/") || name.contains(":") || hasParentTraversal(name)) throw new IOException("Unsafe ZIP path");
                File output = new File(root, name);
                if (!output.getCanonicalPath().startsWith(root.getCanonicalPath() + File.separator)) throw new IOException("Unsafe ZIP path");
                if (entry.isDirectory()) output.mkdirs();
                else {
                    File parent = output.getParentFile();
                    if (parent != null) parent.mkdirs();
                    try (FileOutputStream out = new FileOutputStream(output)) {
                        int n;
                        while ((n = zip.read(buffer)) != -1) {
                            total += n;
                            if (total > 120L * 1024L * 1024L) throw new IOException("ZIP too large");
                            out.write(buffer, 0, n);
                        }
                    }
                }
                zip.closeEntry();
            }
        }
    }

    private boolean hasParentTraversal(String name) {
        for (String part : name.split("/")) if ("..".equals(part)) return true;
        return false;
    }

    private File findIndex(File root) {
        File[] items = root.listFiles();
        if (items == null) return null;
        for (File item : items) {
            if (item.isFile() && ("index.html".equalsIgnoreCase(item.getName()) || "index.htm".equalsIgnoreCase(item.getName()))) return item;
        }
        for (File item : items) if (item.isDirectory()) {
            File result = findIndex(item);
            if (result != null) return result;
        }
        return null;
    }

    private void deleteTree(File file) {
        if (file == null || !file.exists()) return;
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) for (File child : children) deleteTree(child);
        }
        //noinspection ResultOfMethodCallIgnored
        file.delete();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_NOTIFY) {
            toast(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED
                    ? "Build notifications enabled."
                    : "Notifications disabled; APK building still works.");
        }
    }

    @Override
    public void onBackPressed() {
        if (web != null && web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        buildExecutor.shutdown();
        handler.removeCallbacksAndMessages(null);
        if (web != null) {
            web.stopLoading();
            web.destroy();
            web = null;
        }
        super.onDestroy();
    }
}
