package com.web2apk.v3;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import com.android.apksig.ApkSigner;
import com.android.apksig.ApkVerifier;
import com.reandroid.apk.ApkModule;
import com.reandroid.apk.ResFile;
import com.reandroid.arsc.chunk.xml.AndroidManifestBlock;
import com.reandroid.arsc.value.Entry;
import com.reandroid.archive.FileInputSource;
import com.reandroid.archive.InputSource;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Locale;
import java.util.UUID;
import org.json.JSONObject;
import java.util.zip.ZipEntry;
import java.util.zip.CRC32;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipFile;
import java.util.Enumeration;
import javax.security.auth.x500.X500Principal;

/**
 * On-device APK packager for WebView website wrappers.
 * It customizes a bundled, precompiled runtime APK and signs the result in-process.
 * It does not invoke Gradle, Termux, a shell, or an external executable.
 */
public final class InternalApkBuilder {
    public interface Progress {
        void update(String message, int percent);
    }

    private static final String KEY_ALIAS = "web2apk_v4_runtime_signer";
    private static final long MAX_SOURCE_BYTES = 120L * 1024L * 1024L;
    private static final long MAX_ZIP_EXPANDED_BYTES = 160L * 1024L * 1024L;
    private static final int MAX_ZIP_ENTRIES = 4000;
    private static final String RUNTIME_ACTIVITY = "com.web2apk.runtime.WebActivity";

    private InternalApkBuilder() { }

    public static File build(Context context,
                             Uri sourceUri, String sourceMode, Uri iconUri,
                             String appName, String packageId, String versionName, int versionCode,
                             boolean camera, boolean microphone, boolean locationApprox, boolean locationPrecise,
                             boolean notifications, boolean keepScreenAwake, boolean blockScreenshots, boolean creatorWatermark,
                             boolean phoneDialer, boolean vibration, boolean contactPicker,
                             boolean addContact, boolean viewCalendar, boolean calendarInsert, boolean enableAiApi,
                             String aiEndpoint, String aiModel, String aiApiKey, boolean embedAiKey,
                             String websiteUrl, String splashStyle, String splashSpeed, Uri splashMediaUri,
                             String appOrientation, boolean fullscreen, boolean pullToRefresh, String backBehavior,
                             boolean externalLinks, String cacheMode, boolean allowZoom, boolean desktopMode, boolean downloadsEnabled,
                             String designOptionsJson, Progress progress) throws Exception {
        if (context == null) throw new IOException("Context is missing");
        validateBuildRequest(sourceUri, sourceMode, appName, packageId, versionName, versionCode, websiteUrl);
        File work = new File(context.getCacheDir(), "web2apk-internal-" + UUID.randomUUID());
        if (!work.mkdirs()) throw new IOException("Cannot create internal build workspace");
        File template = new File(work, "runtime-template.apk");
        File unsignedApk = new File(work, "unsigned.apk");
        File signedTemp = new File(work, "signed.apk");
        File siteDir = new File(work, "site");
        File iconFile = new File(work, "app_icon.png");
        File splashMediaFile = null;
        String splashMediaType = "";
        JSONObject designOptions;
        try { designOptions = new JSONObject(designOptionsJson == null ? "{}" : designOptionsJson); }
        catch (Exception ignored) { designOptions = new JSONObject(); }

        try {
            report(progress, "Step 1/5 · Preparing local runtime", 12);
            copyAsset(context, "runtime-template.apk", template);
            validateRuntimeTemplate(template);

            report(progress, "Step 2/5 · Importing website files", 28);
            if (sourceUri != null && "html".equals(sourceMode)) {
                siteDir.mkdirs();
                File index = new File(siteDir, "index.html");
                try (InputStream in = context.getContentResolver().openInputStream(sourceUri);
                     OutputStream out = new FileOutputStream(index)) {
                    if (in == null) throw new IOException("Cannot open selected HTML file");
                    copyLimited(in, out, MAX_SOURCE_BYTES);
                }
                if (!index.isFile() || index.length() == 0L) {
                    throw new IOException("Selected HTML file is empty. Choose a valid HTML file.");
                }
            } else if (sourceUri != null && "zip".equals(sourceMode)) {
                extractZip(context, sourceUri, siteDir);
                promoteIndexFolder(siteDir);
                File importedIndex = new File(siteDir, "index.html");
                if (!importedIndex.isFile() || importedIndex.length() == 0L) {
                    throw new IOException("ZIP index.html is empty. Check the website project files.");
                }
            } else {
                String u = websiteUrl == null ? "" : websiteUrl.trim();
                if (!(u.startsWith("https://") || u.startsWith("http://"))) {
                    throw new IOException("Enter a valid http:// or https:// website URL");
                }
            }
            if (enableAiApi) {
                if (!siteDir.exists() && !siteDir.mkdirs()) throw new IOException("Cannot prepare AI API helper folder");
                copyAsset(context, "web2apk-ai.js", new File(siteDir, "web2apk-ai.js"));
                // Always bundle the non-secret endpoint/model defaults so the installed app's
                // AI settings dialog is ready to use even when key embedding is OFF.
                JSONObject aiConfig = new JSONObject();
                aiConfig.put("endpoint", aiEndpoint == null ? "https://generativelanguage.googleapis.com/v1beta/interactions" : aiEndpoint);
                aiConfig.put("model", aiModel == null ? "gemini-3.8-flash" : aiModel);
                if (embedAiKey && aiApiKey != null && !aiApiKey.isEmpty()) {
                    aiConfig.put("apiKey", aiApiKey);
                    report(progress, "AI helper ready · API key embedded by explicit choice", 42);
                }
                String configText = aiConfig.toString();
                writeText(new File(siteDir, "web2apk-ai-config.json"), configText);
                if (sourceUri != null) injectAiConfigIntoIndex(siteDir, configText);
                if (sourceUri != null) injectAiHelperIntoIndex(siteDir);
            }
            if (iconUri != null) {
                double iconZoom = Math.max(1.0d, Math.min(2.5d, designOptions.optDouble("iconZoom", 1.0d)));
                String iconBackground = safeColor(designOptions.optString("iconBackground", "#00000000"), "#00000000");
                String iconMask = safeChoice(designOptions.optString("iconMask", "square"),
                        new String[]{"square", "rounded", "circle"}, "square");
                decodeAndSaveIcon(context, iconUri, iconFile, iconZoom, iconBackground, iconMask);
            }
            String safeSplashStyle = splashStyle == null ? "ios_zoom" : splashStyle;
            if (!("off".equals(safeSplashStyle) || "custom".equals(safeSplashStyle) || "native".equals(safeSplashStyle)
                    || "ios_zoom".equals(safeSplashStyle) || "slide".equals(safeSplashStyle) || "pop".equals(safeSplashStyle))) {
                safeSplashStyle = "ios_zoom";
            }
            String safeSplashSpeed = splashSpeed == null ? "normal" : splashSpeed;
            if (!("slow".equals(safeSplashSpeed) || "normal".equals(safeSplashSpeed) || "fast".equals(safeSplashSpeed))) safeSplashSpeed = "normal";
            String splashBackground = safeColor(designOptions.optString("splashBackground", "#FFF3F4F8"), "#FFF3F4F8");
            String splashTitle = designOptions.optString("splashTitle", "Loading…").replace("\r", " ").replace("\n", " ").trim();
            if (splashTitle.length() > 70) splashTitle = splashTitle.substring(0, 70);
            boolean splashProgress = designOptions.optBoolean("splashProgress", true);
            int splashMinDuration = Math.max(0, Math.min(5000, designOptions.optInt("splashMinDuration", 0)));
            if ("custom".equals(safeSplashStyle) && splashMediaUri != null) {
                String mime = context.getContentResolver().getType(splashMediaUri);
                boolean video = mime != null && mime.toLowerCase(Locale.ROOT).startsWith("video/");
                String lowerMime = mime == null ? "" : mime.toLowerCase(Locale.ROOT);
                String ext = video ? (lowerMime.contains("webm") ? "webm" : "mp4")
                        : (lowerMime.contains("png") ? "png" : (lowerMime.contains("webp") ? "webp" : "jpg"));
                splashMediaType = video ? "video" : "image";
                splashMediaFile = new File(work, "splash_media." + ext);
                try (InputStream in = context.getContentResolver().openInputStream(splashMediaUri);
                     OutputStream out = new FileOutputStream(splashMediaFile)) {
                    if (in == null) throw new IOException("Cannot open selected startup media");
                    copyLimited(in, out, 30L * 1024L * 1024L);
                }
            }

            report(progress, "Step 3/5 · Customizing package, icon, loading and permissions", 52);
            File unsigned = unsignedApk;
            String iconReport = null;
            try (ApkModule module = ApkModule.loadApkFile(template)) {
                module.setLoadDefaultFramework(false);
                module.setApkSignatureBlock(null); // discard the template's old signature before editing
                AndroidManifestBlock manifest = module.getAndroidManifest();
                if (manifest == null) throw new IOException("Runtime template has no AndroidManifest.xml");
                manifest.setPackageName(packageId);
                manifest.setApplicationLabel(appName);
                manifest.setVersionCode(versionCode);
                manifest.setVersionName(versionName);
                manifest.setMinSdkVersion(23);
                manifest.setTargetSdkVersion(33);
                manifest.setMainActivityClassName(RUNTIME_ACTIVITY);
                String buildMode = safeChoice(designOptions.optString("buildMode", "release"),
                        new String[]{"release", "debug"}, "release");
                // The output remains APK-signed in both modes. Debug mode only enables
                // Android's debuggable flag; Release remains the safer default.
                manifest.getOrCreateApplicationElement()
                        .getOrCreateAndroidAttribute("debuggable", 0x0101000f)
                        .setValueAsBoolean("debug".equals(buildMode));
                if (camera) manifest.addUsesPermission("android.permission.CAMERA");
                if (microphone) manifest.addUsesPermission("android.permission.RECORD_AUDIO");
                if (locationApprox || locationPrecise) {
                    manifest.addUsesPermission("android.permission.ACCESS_COARSE_LOCATION");
                }
                if (locationPrecise) manifest.addUsesPermission("android.permission.ACCESS_FINE_LOCATION");
                if (notifications) manifest.addUsesPermission("android.permission.POST_NOTIFICATIONS");
                if (keepScreenAwake) manifest.addUsesPermission("android.permission.WAKE_LOCK");
                if (vibration) manifest.addUsesPermission("android.permission.VIBRATE");
                module.refreshManifest();

                // Replace the actual indexed Android resource files BEFORE ARSCLib writes the APK.
                // AAPT2/ARSCLib may normalize ZIP entry paths, so patching raw APK ZIP names after
                // serialization is unreliable. The resource table keeps the app_icon resource ID;
                // this swaps the bytes behind each referenced drawable/mipmap app_icon variant.
                if (iconFile.isFile()) {
                    iconReport = replaceLauncherIconResources(module, iconFile);
                }

                // Remove all previous website assets from the template, then insert this build's site.
                InputSource[] current = module.getInputSources();
                for (InputSource source : current) {
                    String alias = source.getAlias();
                    if (alias == null) continue;
                    String upper = alias.toUpperCase(Locale.ROOT);
                    boolean oldSite = alias.startsWith("assets/site/") || "assets/site_url.txt".equals(alias)
                            || "assets/web2apk_config.properties".equals(alias);
                    boolean oldSignature = upper.startsWith("META-INF/") &&
                            (upper.endsWith(".SF") || upper.endsWith(".RSA") || upper.endsWith(".DSA") ||
                             upper.endsWith(".EC") || upper.equals("META-INF/MANIFEST.MF"));
                    if (oldSite || oldSignature) module.removeInputSource(alias);
                }
                if (sourceUri != null) {
                    addDirectory(module, siteDir, siteDir, "assets/site/");
                    if (sourceMode.equals("zip") || sourceMode.equals("html")) {
                        File packagedIndex = new File(siteDir, "index.html");
                        if (!packagedIndex.isFile() || packagedIndex.length() == 0L) {
                            throw new IOException("Website source does not contain a valid, non-empty index.html");
                        }
                    }
                } else {
                    if (enableAiApi && new File(siteDir, "web2apk-ai.js").isFile()) {
                        addDirectory(module, siteDir, siteDir, "assets/site/");
                    }
                    File urlFile = new File(work, "site_url.txt");
                    writeText(urlFile, websiteUrl.trim());
                    module.add(new FileInputSource(urlFile, "assets/site_url.txt"));
                }
                if (splashMediaFile != null && splashMediaFile.isFile()) {
                    String ext = splashMediaFile.getName().substring(splashMediaFile.getName().lastIndexOf('.') + 1);
                    module.add(new FileInputSource(splashMediaFile, "assets/splash_media." + ext));
                }
                String safeOrientation = ("portrait".equals(appOrientation) || "landscape".equals(appOrientation)) ? appOrientation : "system";
                String safeBackBehavior = ("confirm".equals(backBehavior) || "exit".equals(backBehavior)) ? backBehavior : "history";
                String safeCacheMode = ("cache_first".equals(cacheMode) || "offline".equals(cacheMode) || "no_cache".equals(cacheMode)) ? cacheMode : "default";
                String userAgentMode = safeChoice(designOptions.optString("userAgentMode", desktopMode ? "desktop" : "system"),
                        new String[]{"system", "mobile", "desktop", "custom"}, "system");
                String customUserAgent = designOptions.optString("customUserAgent", "").replace("\r", " ").replace("\n", " ").trim();
                if (customUserAgent.length() > 300) customUserAgent = customUserAgent.substring(0, 300);
                boolean javascriptEnabled = designOptions.optBoolean("javascriptEnabled", true);
                boolean domStorageEnabled = designOptions.optBoolean("domStorageEnabled", true);
                boolean cookiesEnabled = designOptions.optBoolean("cookiesEnabled", true);
                boolean fileUploadEnabled = designOptions.optBoolean("fileUploadEnabled", true);
                boolean performanceMonitor = designOptions.optBoolean("performanceMonitor", false);
                File runtimeConfig = new File(work, "web2apk_config.properties");
                writeText(runtimeConfig,
                        "block_screenshots=" + (blockScreenshots ? "true" : "false") + "\n" +
                        "creator_watermark=" + (creatorWatermark ? "true" : "false") + "\n" +
                        "keep_screen_awake=" + (keepScreenAwake ? "true" : "false") + "\n" +
                        "allow_phone_dialer=" + (phoneDialer ? "true" : "false") + "\n" +
                        "allow_vibration=" + (vibration ? "true" : "false") + "\n" +
                        "allow_contact_picker=" + (contactPicker ? "true" : "false") + "\n" +
                        "allow_add_contact=" + (addContact ? "true" : "false") + "\n" +
                        "allow_view_calendar=" + (viewCalendar ? "true" : "false") + "\n" +
                        "allow_calendar_insert=" + (calendarInsert ? "true" : "false") + "\n" +
                        "enable_ai_api=" + (enableAiApi ? "true" : "false") + "\n" +
                        "splash_style=" + safeSplashStyle + "\n" +
                        "splash_speed=" + safeSplashSpeed + "\n" +
                        "splash_media_type=" + splashMediaType + "\n" +
                        "splash_media_file=" + (splashMediaFile == null ? "" : splashMediaFile.getName()) + "\n" +
                        "splash_background=" + splashBackground + "\n" +
                        "splash_title_b64=" + android.util.Base64.encodeToString(splashTitle.getBytes(java.nio.charset.StandardCharsets.UTF_8), android.util.Base64.NO_WRAP) + "\n" +
                        "splash_progress=" + (splashProgress ? "true" : "false") + "\n" +
                        "splash_min_duration_ms=" + splashMinDuration + "\n" +
                        "app_orientation=" + safeOrientation + "\n" +
                        "fullscreen=" + (fullscreen ? "true" : "false") + "\n" +
                        "pull_to_refresh=" + (pullToRefresh ? "true" : "false") + "\n" +
                        "back_behavior=" + safeBackBehavior + "\n" +
                        "external_links_browser=" + (externalLinks ? "true" : "false") + "\n" +
                        "cache_mode=" + safeCacheMode + "\n" +
                        "allow_zoom=" + (allowZoom ? "true" : "false") + "\n" +
                        "desktop_mode=" + (desktopMode ? "true" : "false") + "\n" +
                        "downloads_enabled=" + (downloadsEnabled ? "true" : "false") + "\n" +
                        "user_agent_mode=" + userAgentMode + "\n" +
                        "custom_user_agent_b64=" + android.util.Base64.encodeToString(customUserAgent.getBytes(java.nio.charset.StandardCharsets.UTF_8), android.util.Base64.NO_WRAP) + "\n" +
                        "javascript_enabled=" + (javascriptEnabled ? "true" : "false") + "\n" +
                        "dom_storage_enabled=" + (domStorageEnabled ? "true" : "false") + "\n" +
                        "cookies_enabled=" + (cookiesEnabled ? "true" : "false") + "\n" +
                        "file_upload_enabled=" + (fileUploadEnabled ? "true" : "false") + "\n" +
                        "performance_monitor=" + (performanceMonitor ? "true" : "false") + "\n" +
                        "build_mode=" + buildMode + "\n");
                module.add(new FileInputSource(runtimeConfig, "assets/web2apk_config.properties"));

                module.writeApk(unsigned);
            }
            if (iconReport != null) {
                report(progress, "Launcher icon resources updated and verified · " + iconReport, 64);
            }

            report(progress, "Step 4/5 · Signing APK on this device", 78);
            signApk(unsignedApk, signedTemp);
            validateBuiltApk(signedTemp, sourceUri != null);

            report(progress, "Step 5/5 · Verifying and saving signed APK", 94);
            File outDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
            if (outDir == null) outDir = new File(context.getFilesDir(), "downloads");
            if (!outDir.exists() && !outDir.mkdirs()) throw new IOException("Cannot create APK output directory");
            String safeName = appName.replaceAll("[^A-Za-z0-9._-]+", "_").replaceAll("^_+|_+$", "");
            if (safeName.isEmpty()) safeName = "Website_App";
            File output = uniqueOutputFile(outDir, safeName + "_v" + versionCode);
            File stagedOutput = new File(outDir, "." + output.getName() + "." + UUID.randomUUID() + ".tmp");
            try {
                copyFile(signedTemp, stagedOutput);
                validateBuiltApk(stagedOutput, sourceUri != null);
                if (!stagedOutput.renameTo(output)) {
                    throw new IOException("Could not move the verified APK into the Downloads folder");
                }
            } finally {
                if (stagedOutput.exists()) {
                    //noinspection ResultOfMethodCallIgnored
                    stagedOutput.delete();
                }
            }
            report(progress, "BUILD SUCCESSFUL · APK verified and ready", 100);
            return output;
        } finally {
            deleteTree(work);
        }
    }


    private static void validateBuildRequest(Uri sourceUri, String sourceMode,
                                             String appName, String packageId,
                                             String versionName, int versionCode,
                                             String websiteUrl) throws IOException {
        String cleanName = appName == null ? "" : appName.trim();
        String cleanPackage = packageId == null ? "" : packageId.trim();
        String cleanVersion = versionName == null ? "" : versionName.trim();
        if (cleanName.isEmpty() || cleanName.length() > 40) {
            throw new IOException("App name must contain 1–40 characters");
        }
        if (!cleanPackage.matches("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+")) {
            throw new IOException("Package ID must look like com.example.myapp");
        }
        if (!cleanVersion.matches("[0-9A-Za-z._-]{1,30}") || versionCode < 1 || versionCode > 2100000000) {
            throw new IOException("Version name or version code is invalid");
        }
        if (sourceUri != null) {
            if (!("html".equals(sourceMode) || "zip".equals(sourceMode))) {
                throw new IOException("Select an HTML file or ZIP project");
            }
        } else {
            String cleanUrl = websiteUrl == null ? "" : websiteUrl.trim();
            if (cleanUrl.isEmpty() || cleanUrl.length() > 2048) {
                throw new IOException("Enter a valid website URL (maximum 2048 characters)");
            }
            Uri parsed = Uri.parse(cleanUrl);
            String scheme = parsed.getScheme();
            if (parsed.getHost() == null || parsed.getHost().trim().isEmpty()
                    || parsed.getUserInfo() != null
                    || !("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))) {
                throw new IOException("Website URL must be a valid http:// or https:// address");
            }
        }
    }

    private static File uniqueOutputFile(File directory, String baseName) throws IOException {
        File candidate = new File(directory, baseName + ".apk");
        int suffix = 2;
        while (candidate.exists()) {
            candidate = new File(directory, baseName + "_build" + suffix + ".apk");
            suffix++;
            if (suffix > 10000) throw new IOException("Too many APK builds with the same name; rename the app or clean old builds");
        }
        return candidate;
    }

    private static void validateBuiltApk(File apkFile, boolean localWebsite) throws Exception {
        if (apkFile == null || !apkFile.isFile() || apkFile.length() < 10_000L) {
            throw new IOException("APK build output is missing or too small");
        }
        try (ZipFile zip = new ZipFile(apkFile)) {
            if (zip.getEntry("AndroidManifest.xml") == null) {
                throw new IOException("Generated APK is missing AndroidManifest.xml");
            }
            if (zip.getEntry("assets/web2apk_config.properties") == null) {
                throw new IOException("Generated APK is missing its app settings");
            }
            boolean hasDex = false;
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                String name = entries.nextElement().getName();
                if (name.matches("classes(\\d+)?\\.dex")) {
                    hasDex = true;
                    break;
                }
            }
            if (!hasDex) throw new IOException("Generated APK is missing executable DEX code");
            if (localWebsite && zip.getEntry("assets/site/index.html") == null) {
                throw new IOException("Generated APK is missing the website index.html file");
            }
            if (!localWebsite && zip.getEntry("assets/site_url.txt") == null) {
                throw new IOException("Generated APK is missing the live website URL");
            }
        } catch (java.util.zip.ZipException e) {
            throw new IOException("Generated APK is not a valid ZIP archive", e);
        }
        ApkVerifier.Result verification = new ApkVerifier.Builder(apkFile).build().verify();
        if (!verification.isVerified()) {
            String detail = String.valueOf(verification.getErrors());
            if (detail.length() > 700) detail = detail.substring(0, 700);
            throw new IOException("APK signature verification failed: " + detail);
        }
    }

    private static String replaceLauncherIconResources(ApkModule module, File iconFile) throws IOException {
        if (module == null) throw new IOException("Runtime resource module is missing");
        if (iconFile == null || !iconFile.isFile() || iconFile.length() < 100L) {
            throw new IOException("Selected icon image is missing or invalid");
        }

        ArrayList<String> aliases = new ArrayList<>();
        ArrayList<String> observed = new ArrayList<>();
        for (ResFile resFile : module.listResFiles()) {
            Entry entry = resFile.getFirst();
            if (entry == null) continue;
            String type = entry.getTypeName();
            String resourceName = entry.getName();
            if (observed.size() < 40) {
                observed.add(type + "/" + resourceName + " -> " + resFile.getFilePath());
            }
            if ("app_icon".equalsIgnoreCase(resourceName)
                    && ("mipmap".equalsIgnoreCase(type) || "drawable".equalsIgnoreCase(type))) {
                String alias = resFile.getFilePath();
                if (alias != null && !aliases.contains(alias)) aliases.add(alias);
            }
        }

        if (aliases.isEmpty()) {
            throw new IOException("The runtime resource table has no drawable/mipmap app_icon file. Indexed resources: " + observed);
        }

        CRC32 expectedCrc = new CRC32();
        try (InputStream in = new FileInputStream(iconFile)) {
            byte[] buffer = new byte[8192];
            int n;
            while ((n = in.read(buffer)) != -1) expectedCrc.update(buffer, 0, n);
        }

        ArrayList<String> verified = new ArrayList<>();
        for (String alias : aliases) {
            InputSource previous = module.getInputSource(alias);
            if (previous == null) {
                throw new IOException("Icon resource table points to a missing file: " + alias);
            }
            FileInputSource replacement = new FileInputSource(iconFile, alias);
            replacement.copyAttributes(previous);
            replacement.setAlias(alias);
            module.removeInputSource(alias);
            module.add(replacement);

            InputSource actual = module.getInputSource(alias);
            if (actual == null || actual.getLength() != iconFile.length()
                    || actual.getCrc() != expectedCrc.getValue()) {
                throw new IOException("Could not verify selected image for launcher resource: " + alias);
            }
            verified.add(alias);
        }
        return verified.size() + " resource variant(s): " + verified;
    }

    /** Validate the bundled template by its APK structure, not an arbitrary file-size cutoff. */
    private static void validateRuntimeTemplate(File template) throws IOException {
        if (!template.isFile() || template.length() < 1024L) {
            throw new IOException("Bundled runtime APK is missing or truncated (under 1 KB). Rebuild the V4 installer so runtimeTemplate is bundled.");
        }
        try (ZipFile apk = new ZipFile(template)) {
            if (apk.getEntry("AndroidManifest.xml") == null) {
                throw new IOException("Bundled runtime APK is invalid: AndroidManifest.xml is missing.");
            }
            if (apk.getEntry("assets/site/index.html") == null) {
                throw new IOException("Bundled runtime APK is incomplete: assets/site/index.html is missing.");
            }
            boolean hasDex = false;
            Enumeration<? extends ZipEntry> entries = apk.entries();
            while (entries.hasMoreElements()) {
                String name = entries.nextElement().getName();
                if (name.matches("classes(\\d+)?\\.dex")) {
                    hasDex = true;
                    break;
                }
            }
            if (!hasDex) {
                throw new IOException("Bundled runtime APK is invalid: no classes*.dex file was found.");
            }
        } catch (java.util.zip.ZipException e) {
            throw new IOException("Bundled runtime APK is not a valid ZIP/APK archive.", e);
        }
    }

    private static void signApk(File input, File output) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            KeyPairGenerator generator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, "AndroidKeyStore");
            Calendar start = Calendar.getInstance();
            start.add(Calendar.DAY_OF_YEAR, -1);
            Calendar end = Calendar.getInstance();
            end.add(Calendar.YEAR, 30);
            KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(
                    KEY_ALIAS, KeyProperties.PURPOSE_SIGN | KeyProperties.PURPOSE_VERIFY)
                    .setKeySize(2048)
                    .setDigests(KeyProperties.DIGEST_SHA256)
                    .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
                    .setCertificateSubject(new X500Principal("CN=WEB2APK V4 Local Signer, O=WEB2APK"))
                    .setCertificateSerialNumber(BigInteger.valueOf(System.currentTimeMillis()))
                    .setCertificateNotBefore(start.getTime())
                    .setCertificateNotAfter(end.getTime())
                    .setUserAuthenticationRequired(false)
                    .build();
            generator.initialize(spec);
            generator.generateKeyPair();
            keyStore.load(null);
        }
        KeyStore.PrivateKeyEntry entry = (KeyStore.PrivateKeyEntry) keyStore.getEntry(KEY_ALIAS, null);
        if (entry == null || entry.getPrivateKey() == null || !(entry.getCertificate() instanceof X509Certificate)) {
            throw new IOException("Cannot access local APK signing identity");
        }
        List<X509Certificate> chain = Collections.singletonList((X509Certificate) entry.getCertificate());
        ApkSigner.SignerConfig signer = new ApkSigner.SignerConfig.Builder("WEB2APK", entry.getPrivateKey(), chain).build();
        new ApkSigner.Builder(Collections.singletonList(signer))
                .setInputApk(input)
                .setOutputApk(output)
                .setMinSdkVersion(23)
                .setV1SigningEnabled(true)
                .setV2SigningEnabled(true)
                .setV3SigningEnabled(true)
                .build()
                .sign();
    }

    private static void decodeAndSaveIcon(Context context, Uri uri, File out,
                                          double zoom, String backgroundHex, String mask) throws Exception {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream in = context.getContentResolver().openInputStream(uri)) {
            if (in == null) throw new IOException("Cannot read selected icon");
            BitmapFactory.decodeStream(in, null, bounds);
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw new IOException("Unsupported image. Choose PNG, JPG or WebP.");
        }
        int sample = 1;
        int longest = Math.max(bounds.outWidth, bounds.outHeight);
        while (longest / sample > 1536) sample *= 2;
        BitmapFactory.Options decode = new BitmapFactory.Options();
        decode.inSampleSize = sample;
        decode.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap source;
        try (InputStream in = context.getContentResolver().openInputStream(uri)) {
            if (in == null) throw new IOException("Cannot read selected icon");
            source = BitmapFactory.decodeStream(in, null, decode);
        }
        if (source == null) throw new IOException("Unsupported image. Choose PNG, JPG or WebP.");
        Bitmap output = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);
        int background;
        try { background = Color.parseColor(backgroundHex); }
        catch (Exception ignored) { background = Color.TRANSPARENT; }
        if ("circle".equals(mask) || "rounded".equals(mask)) {
            Path clipping = new Path();
            RectF boundsRect = new RectF(0f, 0f, 512f, 512f);
            if ("circle".equals(mask)) clipping.addCircle(256f, 256f, 256f, Path.Direction.CW);
            else clipping.addRoundRect(boundsRect, 86f, 86f, Path.Direction.CW);
            canvas.save();
            canvas.clipPath(clipping);
        }
        canvas.drawColor(background);
        double safeZoom = Math.max(1.0d, Math.min(2.5d, zoom));
        float cropSide = (float) (Math.min(source.getWidth(), source.getHeight()) / safeZoom);
        float cx = source.getWidth() / 2f;
        float cy = source.getHeight() / 2f;
        int half = Math.max(1, Math.round(cropSide / 2f));
        int left = Math.max(0, Math.round(cx) - half);
        int top = Math.max(0, Math.round(cy) - half);
        int right = Math.min(source.getWidth(), left + Math.max(1, Math.round(cropSide)));
        int bottom = Math.min(source.getHeight(), top + Math.max(1, Math.round(cropSide)));
        Rect src = new Rect(left, top, right, bottom);
        Rect dst = new Rect(0, 0, 512, 512);
        canvas.drawBitmap(source, src, dst, paint);
        if ("circle".equals(mask) || "rounded".equals(mask)) canvas.restore();
        try (FileOutputStream stream = new FileOutputStream(out)) {
            if (!output.compress(Bitmap.CompressFormat.PNG, 100, stream)) {
                throw new IOException("Could not encode the app icon");
            }
            stream.flush();
        } finally {
            source.recycle();
            output.recycle();
        }
    }

    private static String safeColor(String value, String fallback) {
        if (value == null || !value.matches("#[0-9A-Fa-f]{6}([0-9A-Fa-f]{2})?")) return fallback;
        return value.toUpperCase(Locale.ROOT);
    }

    private static String safeChoice(String value, String[] allowed, String fallback) {
        if (value == null) return fallback;
        for (String item : allowed) if (item.equals(value)) return item;
        return fallback;
    }


    private static void injectAiConfigIntoIndex(File siteRoot, String configJson) throws IOException {
        File index = findIndex(siteRoot);
        if (index == null) throw new IOException("Website index file was not found for AI API config");
        if (index.length() > 8L * 1024L * 1024L) throw new IOException("index.html is too large to prepare AI API settings");
        String html;
        try (InputStream in = new FileInputStream(index); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192]; int n;
            while ((n = in.read(buffer)) != -1) {
                if (out.size() + n > 8L * 1024L * 1024L) throw new IOException("index.html exceeds the 8 MB limit");
                out.write(buffer, 0, n);
            }
            html = out.toString("UTF-8");
        }
        if (html.contains("__WEB2APK_AI_CONFIG__")) return;
        String script = "<script>window.__WEB2APK_AI_CONFIG__=JSON.parse(" + JSONObject.quote(configJson) + ");</script>";
        java.util.regex.Pattern head = java.util.regex.Pattern.compile("</head\\s*>", java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher matcher = head.matcher(html);
        if (matcher.find()) html = matcher.replaceFirst(java.util.regex.Matcher.quoteReplacement(script + "</head>"));
        else html = script + html;
        writeText(index, html);
    }

    private static void injectAiHelperIntoIndex(File siteRoot) throws IOException {
        File index = findIndex(siteRoot);
        if (index == null) throw new IOException("Website index file was not found for AI API setup");
        if (index.length() > 8L * 1024L * 1024L) throw new IOException("index.html is too large to prepare AI API support");
        byte[] bytes;
        try (InputStream in = new FileInputStream(index); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int n;
            while ((n = in.read(buffer)) != -1) {
                if (out.size() + n > 8L * 1024L * 1024L) throw new IOException("index.html exceeds the 8 MB limit");
                out.write(buffer, 0, n);
            }
            bytes = out.toByteArray();
        }
        String html = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
        if (html.contains("web2apk-ai.js")) return;
        String tag = "<script src=\"https://appassets.androidplatform.net/assets/site/web2apk-ai.js\"></script>";
        java.util.regex.Pattern head = java.util.regex.Pattern.compile("</head\\s*>", java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher matcher = head.matcher(html);
        if (matcher.find()) html = matcher.replaceFirst(java.util.regex.Matcher.quoteReplacement(tag + "</head>"));
        else html = tag + html;
        writeText(index, html);
    }

    private static void extractZip(Context context, Uri uri, File destination) throws Exception {
        if (!destination.mkdirs() && !destination.isDirectory()) throw new IOException("Cannot create website folder");
        InputStream opened = context.getContentResolver().openInputStream(uri);
        if (opened == null) throw new IOException("Cannot read selected ZIP");
        try (InputStream raw = opened; ZipInputStream zip = new ZipInputStream(raw)) {
            ZipEntry entry;
            byte[] buffer = new byte[16 * 1024];
            long total = 0;
            int entries = 0;
            Set<String> seenFiles = new HashSet<>();
            while ((entry = zip.getNextEntry()) != null) {
                if (++entries > MAX_ZIP_ENTRIES) throw new IOException("ZIP contains too many entries");
                String name = entry.getName().replace('\\', '/');
                if (name.trim().isEmpty() || name.startsWith("/") || name.contains(":") || hasParentTraversal(name)) {
                    throw new IOException("Unsafe ZIP path detected");
                }
                if (!entry.isDirectory() && !seenFiles.add(name)) {
                    throw new IOException("ZIP contains a duplicate file path: " + name);
                }
                File output = new File(destination, name);
                String rootPath = destination.getCanonicalPath() + File.separator;
                if (!output.getCanonicalPath().startsWith(rootPath)) throw new IOException("ZIP path escaped destination");
                if (entry.isDirectory()) {
                    if (!output.mkdirs() && !output.isDirectory()) throw new IOException("Cannot create ZIP folder");
                } else {
                    File parent = output.getParentFile();
                    if (parent != null && !parent.mkdirs() && !parent.isDirectory()) throw new IOException("Cannot create ZIP parent folder");
                    try (OutputStream out = new FileOutputStream(output)) {
                        int n;
                        while ((n = zip.read(buffer)) != -1) {
                            total += n;
                            if (total > MAX_ZIP_EXPANDED_BYTES) throw new IOException("ZIP expands beyond the 160 MB limit");
                            out.write(buffer, 0, n);
                        }
                    }
                }
                zip.closeEntry();
            }
        }
        File index = findIndex(destination);
        if (index == null) throw new IOException("ZIP does not contain index.html or index.htm");
    }

    private static boolean hasParentTraversal(String name) {
        for (String part : name.split("/")) if ("..".equals(part)) return true;
        return false;
    }

    private static File findIndex(File root) {
        File[] files = root.listFiles();
        if (files == null) return null;
        for (File file : files) {
            if (file.isFile() && ("index.html".equalsIgnoreCase(file.getName()) || "index.htm".equalsIgnoreCase(file.getName()))) return file;
        }
        for (File file : files) if (file.isDirectory()) {
            File result = findIndex(file);
            if (result != null) return result;
        }
        return null;
    }

    private static void promoteIndexFolder(File siteRoot) throws Exception {
        File index = findIndex(siteRoot);
        if (index == null) throw new IOException("ZIP does not contain index.html or index.htm");
        File parent = index.getParentFile();
        if (!parent.equals(siteRoot)) {
            File promoted = new File(siteRoot.getParentFile(), "site-promoted");
            deleteTree(promoted);
            copyDirectory(parent, promoted);
            deleteTree(siteRoot);
            if (!promoted.renameTo(siteRoot)) throw new IOException("Could not prepare website root folder");
        }
        File htm = new File(siteRoot, "index.htm");
        File html = new File(siteRoot, "index.html");
        if (!html.exists() && htm.exists() && !htm.renameTo(html)) throw new IOException("Could not rename index.htm");
    }

    private static void copyDirectory(File from, File to) throws IOException {
        if (!to.exists() && !to.mkdirs()) throw new IOException("Cannot create promoted website folder");
        File[] children = from.listFiles();
        if (children == null) return;
        for (File child : children) {
            File target = new File(to, child.getName());
            if (child.isDirectory()) copyDirectory(child, target);
            else copyFile(child, target);
        }
    }

    private static void addDirectory(ApkModule module, File root, File current, String prefix) throws IOException {
        File[] children = current.listFiles();
        if (children == null) return;
        for (File child : children) {
            if (child.isDirectory()) addDirectory(module, root, child, prefix);
            else {
                String relative = root.toURI().relativize(child.toURI()).getPath();
                module.add(new FileInputSource(child, prefix + relative.replace('\\', '/')));
            }
        }
    }

    private static void copyAsset(Context context, String assetName, File out) throws IOException {
        try (InputStream in = context.getAssets().open(assetName); OutputStream stream = new FileOutputStream(out)) {
            copyLimited(in, stream, 300L * 1024L * 1024L);
        }
    }

    private static void copyLimited(InputStream in, OutputStream out, long max) throws IOException {
        byte[] buffer = new byte[16 * 1024];
        long total = 0;
        int n;
        while ((n = in.read(buffer)) != -1) {
            total += n;
            if (total > max) throw new IOException("Input file exceeds the size limit");
            out.write(buffer, 0, n);
        }
        out.flush();
    }

    private static void copyFile(File from, File to) throws IOException {
        File parent = to.getParentFile();
        if (parent != null && !parent.mkdirs() && !parent.isDirectory()) throw new IOException("Cannot create output folder");
        try (InputStream in = new FileInputStream(from); OutputStream out = new FileOutputStream(to)) {
            copyLimited(in, out, Long.MAX_VALUE);
        }
    }

    private static void writeText(File file, String text) throws IOException {
        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write(text.getBytes("UTF-8"));
        }
    }

    private static void report(Progress progress, String message, int percent) {
        if (progress != null) progress.update(message, percent);
    }

    private static void deleteTree(File file) {
        if (file == null || !file.exists()) return;
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) for (File child : children) deleteTree(child);
        }
        //noinspection ResultOfMethodCallIgnored
        file.delete();
    }
}
