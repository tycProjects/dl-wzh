package com.htmltoapk.builder;

import android.content.Context;

import com.android.apksig.ApkSigner;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/** Turns a {@link ProjectSpec} into a signed, installable APK entirely on the device. */
public final class ApkBuilder {
    /** Values baked into the template (see template/build.gradle.kts and its manifest). */
    private static final String TEMPLATE_PACKAGE = "com.template.webapp";
    private static final String TEMPLATE_LABEL = "__APP_LABEL__";
    private static final String TEMPLATE_VERSION_NAME = "__VERSION_NAME__";
    private static final String INTERNET = "android.permission.INTERNET";
    private static final String HARMLESS_PERMISSION = "android.permission.ACCESS_NETWORK_STATE";
    private static final int TEMPLATE_VERSION_CODE = 1234567;

    public interface Listener {
        void onStep(String message);
    }

    private ApkBuilder() {}

    public static File build(Context ctx, ProjectSpec spec, Listener log) throws Exception {
        File work = new File(ctx.getCacheDir(), "build");
        deleteRecursively(work);
        if (!work.mkdirs()) throw new Exception("Cannot create work folder");

        log.onStep("Preparing template");
        File template = new File(work, "template.apk");
        copy(ctx.getAssets().open("template.apk"), new FileOutputStream(template));

        log.onStep("Writing app manifest");
        Map<String, String> strings = new LinkedHashMap<>();
        strings.put(TEMPLATE_PACKAGE, spec.packageName);
        strings.put(TEMPLATE_LABEL, spec.appName);
        strings.put(TEMPLATE_VERSION_NAME, spec.versionName);
        String url = spec.websiteUrl == null ? "" : spec.websiteUrl;
        boolean online = spec.allowInternet || !url.isEmpty();
        if (!online) strings.put(INTERNET, HARMLESS_PERMISSION);
        byte[] manifest = AxmlPatcher.patch(
                ApkAssembler.readEntry(template, "AndroidManifest.xml"),
                strings, TEMPLATE_VERSION_CODE, spec.versionCode);

        Map<String, byte[]> replace = new HashMap<>();
        replace.put("AndroidManifest.xml", manifest);
        if (spec.iconPng != null) {
            String iconEntry = ApkAssembler.findIconEntry(template);
            if (iconEntry == null) throw new Exception("Template icon not found");
            replace.put(iconEntry, spec.iconPng);
        }

        JSONObject cfg = new JSONObject();
        cfg.put("orientation", spec.orientation);
        cfg.put("fullscreen", spec.fullscreen);
        cfg.put("keepScreenOn", spec.keepScreenOn);
        cfg.put("allowInternet", online);
        cfg.put("url", url);
        Map<String, byte[]> add = new HashMap<>();
        add.put("assets/config.json", cfg.toString().getBytes(StandardCharsets.UTF_8));

        log.onStep("Packing your website");
        File unsigned = new File(work, "unsigned.apk");
        ApkAssembler.assemble(template, unsigned, replace, add, url.isEmpty() ? spec.wwwDir : null);

        log.onStep("Signing");
        File outDir = new File(ctx.getCacheDir(), "out");
        deleteRecursively(outDir);
        if (!outDir.mkdirs()) throw new Exception("Cannot create output folder");
        File signed = new File(outDir, safeFileName(spec.appName) + ".apk");
        sign(ctx, unsigned, signed);

        deleteRecursively(work);
        log.onStep("Done");
        return signed;
    }

    private static void sign(Context ctx, File in, File out) throws Exception {
        PrivateKey key;
        try (InputStream is = ctx.getAssets().open("signing.pk8")) {
            key = java.security.KeyFactory.getInstance("RSA")
                    .generatePrivate(new PKCS8EncodedKeySpec(readAll(is)));
        }
        X509Certificate cert;
        try (InputStream is = ctx.getAssets().open("signing.x509.der")) {
            cert = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(is);
        }
        ApkSigner.SignerConfig signer = new ApkSigner.SignerConfig.Builder(
                "htmltoapk", key, Collections.singletonList(cert)).build();
        new ApkSigner.Builder(Collections.singletonList(signer))
                .setInputApk(in)
                .setOutputApk(out)
                .setMinSdkVersion(21)
                .setV1SigningEnabled(true)
                .setV2SigningEnabled(true)
                .setV3SigningEnabled(false)
                .build()
                .sign();
    }

    public static String safeFileName(String name) {
        String s = name.replaceAll("[^A-Za-z0-9._-]+", "_").replaceAll("^_+|_+$", "");
        return s.isEmpty() ? "app" : s;
    }

    static void copy(InputStream in, OutputStream out) throws java.io.IOException {
        try (InputStream i = in; OutputStream o = out) {
            byte[] buf = new byte[16384];
            int n;
            while ((n = i.read(buf)) > 0) o.write(buf, 0, n);
        }
    }

    static byte[] readAll(InputStream in) throws java.io.IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) bos.write(buf, 0, n);
        return bos.toByteArray();
    }

    static void deleteRecursively(File f) {
        if (f == null || !f.exists()) return;
        File[] kids = f.listFiles();
        if (kids != null) for (File k : kids) deleteRecursively(k);
        //noinspection ResultOfMethodCallIgnored
        f.delete();
    }
}
