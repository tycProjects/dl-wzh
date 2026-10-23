package com.template.webapp;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

// Quick builds can ship the site sealed: every file is AES-256-GCM encrypted and stored
// under an opaque name (m/<hash of its path>) instead of plain files in assets/. The key is
// derived from this app's own signing certificate, so a re-signed or edited copy cannot
// open it. Nothing readable (no index.html, no file names) is left in the APK.
public final class SealedSite {
    private SealedSite() {}

    private static final String SALT_HEX = "260b98dfbf7272f53ec1bbc8af9a3e3db94be7d0a4155ffeae50c6f44372e570";

    private static boolean tried = false;
    private static ZipFile zip = null;
    private static byte[] key = null;

    private static byte[] hexToBytes(String h) {
        byte[] out = new byte[h.length() / 2];
        for (int i = 0; i < out.length; i++) out[i] = (byte) Integer.parseInt(h.substring(i * 2, i * 2 + 2), 16);
        return out;
    }

    private static String hex(byte[] b, int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) sb.append(String.format("%02x", b[i] & 0xff));
        return sb.toString();
    }

    private static String entryName(String rel) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(hexToBytes(SALT_HEX));
        md.update(rel.getBytes("UTF-8"));
        return "m/" + hex(md.digest(), 16);
    }

    private static synchronized void init(Context context) {
        if (tried) return;
        tried = true;
        try {
            Context app = context.getApplicationContext();
            PackageManager pm = app.getPackageManager();
            byte[] cert;
            if (Build.VERSION.SDK_INT >= 28) {
                PackageInfo pi = pm.getPackageInfo(app.getPackageName(), PackageManager.GET_SIGNING_CERTIFICATES);
                Signature[] sigs = pi.signingInfo.getApkContentsSigners();
                cert = sigs[0].toByteArray();
            } else {
                PackageInfo pi = pm.getPackageInfo(app.getPackageName(), PackageManager.GET_SIGNATURES);
                cert = pi.signatures[0].toByteArray();
            }
            byte[] ch = MessageDigest.getInstance("SHA-256").digest(cert);
            MessageDigest kd = MessageDigest.getInstance("SHA-256");
            kd.update(hexToBytes(SALT_HEX));
            kd.update((byte) 1);
            kd.update(ch);
            key = kd.digest();
            zip = new ZipFile(app.getApplicationInfo().sourceDir);
        } catch (Throwable t) {
            zip = null;
            key = null;
        }
    }

    // True when this APK carries a sealed copy of the given page.
    public static boolean present(Context context, String entryRel) {
        init(context);
        if (zip == null) return false;
        try {
            return zip.getEntry(entryName(entryRel)) != null;
        } catch (Throwable t) {
            return false;
        }
    }

    // Decrypted bytes of one site file, or null when it is not there.
    public static byte[] get(Context context, String rel) {
        init(context);
        if (zip == null || key == null) return null;
        try {
            ZipEntry e = zip.getEntry(entryName(rel));
            if (e == null) return null;
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try (InputStream in = zip.getInputStream(e)) {
                byte[] buf = new byte[16384];
                int n;
                while ((n = in.read(buf)) > 0) bos.write(buf, 0, n);
            }
            byte[] all = bos.toByteArray();
            if (all.length < 12 + 16) return null;
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, all, 0, 12));
            return c.doFinal(all, 12, all.length - 12);
        } catch (Throwable t) {
            return null;
        }
    }

    public static String mime(String rel) {
        String l = rel.toLowerCase();
        if (l.endsWith(".html") || l.endsWith(".htm")) return "text/html";
        if (l.endsWith(".css")) return "text/css";
        if (l.endsWith(".js") || l.endsWith(".mjs")) return "application/javascript";
        if (l.endsWith(".json")) return "application/json";
        if (l.endsWith(".svg")) return "image/svg+xml";
        if (l.endsWith(".png")) return "image/png";
        if (l.endsWith(".jpg") || l.endsWith(".jpeg")) return "image/jpeg";
        if (l.endsWith(".gif")) return "image/gif";
        if (l.endsWith(".webp")) return "image/webp";
        if (l.endsWith(".woff2")) return "font/woff2";
        if (l.endsWith(".woff")) return "font/woff";
        if (l.endsWith(".ttf")) return "font/ttf";
        if (l.endsWith(".mp3")) return "audio/mpeg";
        if (l.endsWith(".mp4")) return "video/mp4";
        if (l.endsWith(".webm")) return "video/webm";
        if (l.endsWith(".ico")) return "image/x-icon";
        return "application/octet-stream";
    }
}
