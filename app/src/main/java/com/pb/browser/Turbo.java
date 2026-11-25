package com.pb.browser;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.util.Base64;
import android.webkit.CookieManager;
import android.webkit.URLUtil;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

// MASTER Turbo: (1) chia luong - anh/CSS/font/media di thang mang WiFi, trang HTML/API van di qua proxy
// (2) chan quang cao / tracker  (3) tai file qua proxy (khong lo IP)
final class Turbo {
    static final class Px { String scheme = "http", host = "", user, pass = ""; int port; }

    // Tach chuoi proxy: scheme://user:pass@host:port | user:pass@host:port | host:port:user:pass | host:port
    static Px parse(String raw, String ptype) {
        Px p = new Px();
        p.scheme = Store.schemeOf(raw, ptype);
        String rest = raw == null ? "" : raw.trim();
        int i = rest.indexOf("://");
        if (i > 0) rest = rest.substring(i + 3);
        String hp = rest;
        if (rest.contains("@")) {
            int at = rest.lastIndexOf('@');
            String cred = rest.substring(0, at); hp = rest.substring(at + 1);
            int c = cred.indexOf(':');
            p.user = c < 0 ? cred : cred.substring(0, c);
            p.pass = c < 0 ? "" : cred.substring(c + 1);
        } else {
            String[] a = rest.split(":");
            if (a.length == 4) { hp = a[0] + ":" + a[1]; p.user = a[2]; p.pass = a[3]; }
        }
        String[] h = hp.split(":");
        p.host = h[0];
        try { p.port = Integer.parseInt(h[1].trim()); } catch (Exception e) { p.port = 0; }
        return p;
    }

    // ---------- chan quang cao ----------
    static final String[] ADS = {
            "doubleclick.net", "googlesyndication.com", "googleadservices.com", "adservice.google.com", "2mdn.net",
            "adnxs.com", "taboola.com", "outbrain.com", "criteo.com", "criteo.net", "pubmatic.com", "rubiconproject.com",
            "openx.net", "adsrvr.org", "scorecardresearch.com", "quantserve.com", "moatads.com", "amazon-adsystem.com",
            "adform.net", "casalemedia.com", "smartadserver.com", "mgid.com", "popads.net", "propellerads.com",
            "exoclick.com", "juicyads.com", "adcolony.com", "google-analytics.com", "hotjar.com"};
    static final Set<String> AD_SET = new HashSet<>(Arrays.asList(ADS));
    // v3.3.5: tra theo tung cap ten mien (a.b.c -> b.c -> c): ~3-4 lan tra HashSet thay vi 29 lan endsWith + noi chuoi cho MOI request
    static boolean isAd(String host) {
        if (host == null) return false;
        String h = host;
        while (true) {
            if (AD_SET.contains(h)) return true;
            int dot = h.indexOf('.');
            if (dot < 0) return false;
            h = h.substring(dot + 1);
        }
    }
    static WebResourceResponse empty(Uri u) {
        String p = u.getPath() == null ? "" : u.getPath().toLowerCase(Locale.US);
        String mime = p.endsWith(".js") ? "application/javascript" : p.endsWith(".css") ? "text/css" : "text/plain";
        Map<String, String> h = new HashMap<>();
        h.put("Access-Control-Allow-Origin", "*");
        return new WebResourceResponse(mime, "utf-8", 200, "OK", h, new ByteArrayInputStream(new byte[0]));
    }

    // ---------- chia luong ----------
    static String site(String host) {
        if (host == null) return "";
        String[] p = host.split("\\.");
        int n = p.length;
        if (n <= 2) return host;
        String last = p[n - 1], sec = p[n - 2];
        if (last.length() == 2 && n >= 3 && (sec.equals("co") || sec.equals("com") || sec.equals("net")
                || sec.equals("org") || sec.equals("gov") || sec.equals("edu") || sec.equals("ac")))
            return p[n - 3] + "." + sec + "." + last;
        return sec + "." + last;
    }

    static final Set<String> STATIC_EXT = new HashSet<>(Arrays.asList(
            "png", "jpg", "jpeg", "gif", "webp", "avif", "svg", "ico", "bmp", "css",
            "woff", "woff2", "ttf", "otf", "eot", "mp4", "webm", "m4s", "m4a", "mp3", "aac", "ogg", "wav"));
    static final Set<String> JS_EXT = new HashSet<>(Arrays.asList("js", "mjs"));

    static String hdr(Map<String, String> h, String k) {
        if (h == null) return null;
        for (Map.Entry<String, String> e : h.entrySet()) if (k.equalsIgnoreCase(e.getKey())) return e.getValue();
        return null;
    }

    // Tai nguyen tinh (khong chua du lieu dang nhap/giao dich) -> cho phep di thang
    static boolean isStatic(Uri u, Map<String, String> h, boolean allowJs) {
        String acc = hdr(h, "Accept");
        if (acc != null && (acc.startsWith("image/") || acc.startsWith("text/css"))) return true;
        String path = u.getPath();
        if (path == null) return false;
        int dot = path.lastIndexOf('.');
        if (dot < 0 || dot < path.lastIndexOf('/')) return false;
        String ext = path.substring(dot + 1).toLowerCase(Locale.US);
        return STATIC_EXT.contains(ext) || (allowJs && JS_EXT.contains(ext));
    }

    static final Set<String> SKIP_REQ = new HashSet<>(Arrays.asList(
            "accept-encoding", "host", "connection", "content-length", "if-none-match", "if-modified-since",
            "x-requested-with", "cookie"));
    static final Set<String> SKIP_RESP = new HashSet<>(Arrays.asList(
            "content-encoding", "content-length", "transfer-encoding", "connection", "keep-alive"));

    // Cache RAM (32MB) cho file tinh nho: lan sau / trang khac dung lai la co ngay, khong cham mang
    static final class Hit { String mime, charset; byte[] data; Map<String, String> headers; long exp; }
    // RAM cache tu chinh theo may: 1/8 heap, toi da 64MB. Khoa theo HO SO -> ho so nay khong thay anh cua ho so kia.
    static final android.util.LruCache<String, Hit> RAM = new android.util.LruCache<String, Hit>(
            (int) Math.max(16L * 1024 * 1024, Math.min(64L * 1024 * 1024, Runtime.getRuntime().maxMemory() / 8))) {
        @Override protected int sizeOf(String k, Hit v) { return v.data.length + 256; }
    };
    static final int RAM_MAX_ITEM = 1024 * 1024;   // v3.3.2: >1MB stream thang, khong doi doc het
    static volatile String profile = "";
    static void clearRam() { RAM.evictAll(); }
    // May chu tai thang bi loi / chan (403, timeout...) -> tam ngung tai thang, de WebView dung proxy ngay, khong cho 4s moi anh
    static final java.util.concurrent.ConcurrentHashMap<String, Long> BAD = new java.util.concurrent.ConcurrentHashMap<>();
    static void bad(String host, long ms) { if (host != null) BAD.put(host, System.currentTimeMillis() + ms); }
    static boolean isBad(String host) {
        if (host == null) return false;
        Long t = BAD.get(host);
        if (t == null) return false;
        if (System.currentTimeMillis() > t) { BAD.remove(host); return false; }
        return true;
    }
    // Thoi gian giu trong RAM theo Cache-Control: no-store/private -> khong giu; max-age -> toi da 1 gio; mac dinh 10 phut
    static long ttl(String cc) {
        if (cc == null) return 600000L;
        String c = cc.toLowerCase(Locale.US);
        if (c.contains("no-store") || c.contains("private") || c.contains("no-cache")) return 0;
        int i = c.indexOf("max-age=");
        if (i >= 0) {
            int j = i + 8, e = j;
            while (e < c.length() && Character.isDigit(c.charAt(e))) e++;
            try { return Math.min(3600L, Long.parseLong(c.substring(j, e))) * 1000L; } catch (Exception ignored) {}
        }
        return 600000L;
    }

    // Tai truc tiep qua WiFi (khong qua proxy). Bat ky loi/ma la nao -> tra null de WebView tu tai qua proxy.
    static WebResourceResponse direct(WebResourceRequest r) {
        if (!"GET".equalsIgnoreCase(r.getMethod()) || !"https".equals(r.getUrl().getScheme())) return null;
        final boolean ranged = hdr(r.getRequestHeaders(), "Range") != null;
        final String url0 = r.getUrl().toString();
        final String host0 = r.getUrl().getHost();
        final String key = profile + "|" + url0;
        if (isBad(host0)) return null;
        if (!ranged) {
            Hit c = RAM.get(key);
            if (c != null && System.currentTimeMillis() > c.exp) { RAM.remove(key); c = null; }
            if (c != null) return new WebResourceResponse(c.mime, c.charset, 200, "OK", new HashMap<>(c.headers),
                    new ByteArrayInputStream(c.data));
        }
        try {
            URL url = new URL(url0);
            HttpURLConnection h = (HttpURLConnection) url.openConnection(Proxy.NO_PROXY);
            h.setConnectTimeout(1500);   // v3.3.2: 3s -> 1.5s, may chu loi thi chuyen proxy nhanh hon
            h.setReadTimeout(12000);
            h.setInstanceFollowRedirects(true);
            Map<String, String> rh = r.getRequestHeaders();
            if (rh != null) for (Map.Entry<String, String> e : rh.entrySet()) {
                String k = e.getKey();
                if (k == null || SKIP_REQ.contains(k.toLowerCase(Locale.US))) continue;
                h.setRequestProperty(k, e.getValue());
            }
            // KHONG gui cookie / KHONG luu Set-Cookie qua duong IP that -> may chu file tinh khong lien ket duoc tai khoan voi IP that
            int code = h.getResponseCode();
            if (!(code == 200 || code == 206 || code == 404 || code == 410)) {
                if (code == 401 || code == 403 || code == 429 || code >= 500) bad(host0, 5 * 60000L);   // chan IP / hotlink -> dung proxy
                h.disconnect(); return null;
            }
            String ce = h.getContentEncoding();
            if (ce != null && !ce.equalsIgnoreCase("identity")) { h.disconnect(); return null; }

            String ct = h.getContentType(), mime = null, charset = null;
            if (ct != null) {
                String[] parts = ct.split(";");
                mime = parts[0].trim().toLowerCase(Locale.US);
                for (int i = 1; i < parts.length; i++) {
                    String s = parts[i].trim();
                    if (s.toLowerCase(Locale.US).startsWith("charset=")) charset = s.substring(8).replace("\"", "");
                }
            }
            if (mime == null || mime.isEmpty()) {
                mime = java.net.URLConnection.guessContentTypeFromName(url.getPath());
                if (mime == null) mime = "application/octet-stream";
            }
            Map<String, String> out = new HashMap<>();
            for (Map.Entry<String, List<String>> e : h.getHeaderFields().entrySet()) {
                String k = e.getKey();
                List<String> vs = e.getValue();
                if (k == null || vs == null || vs.isEmpty()) continue;
                String lk = k.toLowerCase(Locale.US);
                if (lk.equals("set-cookie") || lk.equals("set-cookie2")) continue;
                if (SKIP_RESP.contains(lk)) continue;
                out.put(k, android.text.TextUtils.join(", ", vs));
            }
            InputStream in;
            try { in = code >= 400 ? h.getErrorStream() : h.getInputStream(); } catch (Exception ex) { in = null; }
            if (in == null) in = new ByteArrayInputStream(new byte[0]);
            String reason = h.getResponseMessage();
            if (reason == null || reason.isEmpty()) reason = code == 200 ? "OK" : code == 206 ? "Partial Content" : "Not Found";
            // File nho (<=1.5MB): doc het 1 lan -> luu RAM, tra ve nhanh. File lon / Range: stream nhu cu.
            if (code == 200 && !ranged) {
                long len = h.getContentLengthLong();
                if (len <= RAM_MAX_ITEM) {
                    java.io.ByteArrayOutputStream bo = new java.io.ByteArrayOutputStream(len > 0 ? (int) len : 16384);
                    byte[] buf = new byte[16384]; int n; boolean big = false;
                    while ((n = in.read(buf)) > 0) {
                        bo.write(buf, 0, n);
                        if (bo.size() > RAM_MAX_ITEM) { big = true; break; }
                    }
                    if (!big) {
                        in.close();
                        Hit hit = new Hit();
                        hit.mime = mime; hit.charset = charset; hit.data = bo.toByteArray(); hit.headers = out;
                        long life = ttl(h.getHeaderField("Cache-Control"));
                        if (life > 0) { hit.exp = System.currentTimeMillis() + life; RAM.put(key, hit); }
                        return new WebResourceResponse(mime, charset, code, reason, new HashMap<>(out),
                                new ByteArrayInputStream(hit.data));
                    }
                    in = new java.io.SequenceInputStream(new ByteArrayInputStream(bo.toByteArray()), in);
                }
            }
            return new WebResourceResponse(mime, charset, code, reason, out, in);
        } catch (java.net.SocketTimeoutException | java.net.ConnectException | java.net.UnknownHostException t) {
            bad(host0, 60000L);
            return null;
        } catch (Throwable t) {
            return null;
        }
    }

    // ---------- tai file qua proxy ----------
    static void download(final Context c, final String url, final String ua, final String cd, final String mime,
                         final Px px, final Handler ui) {
        new Thread(() -> {
            String msg;
            try {
                if (px != null && px.user != null) {
                    final String fu = px.user, fp = px.pass;
                    java.net.Authenticator.setDefault(new java.net.Authenticator() {
                        @Override protected java.net.PasswordAuthentication getPasswordAuthentication() {
                            if (getRequestorType() != RequestorType.PROXY) return null;
                            return new java.net.PasswordAuthentication(fu, fp.toCharArray());
                        }
                    });
                }
                Proxy proxy = Proxy.NO_PROXY;
                if (px != null && !px.host.isEmpty() && px.port > 0)
                    proxy = new Proxy("socks5".equals(px.scheme) ? Proxy.Type.SOCKS : Proxy.Type.HTTP,
                            new InetSocketAddress(px.host, px.port));
                HttpURLConnection h = (HttpURLConnection) new URL(url).openConnection(proxy);
                h.setConnectTimeout(15000);
                h.setReadTimeout(30000);
                h.setInstanceFollowRedirects(true);
                if (ua != null) h.setRequestProperty("User-Agent", ua);
                String ck = CookieManager.getInstance().getCookie(url);
                if (ck != null) h.setRequestProperty("Cookie", ck);
                if (px != null && px.user != null && proxy.type() == Proxy.Type.HTTP)
                    h.setRequestProperty("Proxy-Authorization", "Basic "
                            + Base64.encodeToString((px.user + ":" + px.pass).getBytes("UTF-8"), Base64.NO_WRAP));
                int code = h.getResponseCode();
                if (code == 407) throw new Exception("Proxy yêu cầu đăng nhập (407)");
                if (code != 200) throw new Exception("HTTP " + code);
                String name = URLUtil.guessFileName(url, cd, mime);
                String mt = mime;
                if (mt == null || mt.isEmpty()) { mt = h.getContentType(); if (mt != null) mt = mt.split(";")[0].trim(); }
                // v3.3.5: Android 10+ -> luu vao Download/TUANANH (MediaStore) de mo duoc bang app Tep; truoc day luu vao
                // Android/data/<app>/files nen tren Android 11+ nguoi dung gan nhu khong tim thay file.
                String where = null;
                java.io.OutputStream os = null;
                if (android.os.Build.VERSION.SDK_INT >= 29) {
                    try {
                        android.content.ContentValues cv = new android.content.ContentValues();
                        cv.put(android.provider.MediaStore.Downloads.DISPLAY_NAME, name);
                        if (mt != null && !mt.isEmpty()) cv.put(android.provider.MediaStore.Downloads.MIME_TYPE, mt);
                        cv.put(android.provider.MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/TUANANH");
                        Uri out = c.getContentResolver().insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv);
                        if (out != null) { os = c.getContentResolver().openOutputStream(out); where = "Download/TUANANH"; }
                    } catch (Exception ignored) { os = null; }
                }
                if (os == null) {
                    File dir = c.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
                    if (dir == null) dir = c.getFilesDir();
                    dir.mkdirs();
                    File f = new File(dir, name);
                    int n = 1;
                    while (f.exists()) {
                        int dot = name.lastIndexOf('.');
                        f = new File(dir, dot > 0 ? name.substring(0, dot) + " (" + n + ")" + name.substring(dot) : name + " (" + n + ")");
                        n++;
                    }
                    os = new FileOutputStream(f);
                    name = f.getName(); where = f.getParent();
                }
                try (InputStream in = h.getInputStream(); java.io.OutputStream o2 = os) {
                    byte[] buf = new byte[32768]; int r;
                    while ((r = in.read(buf)) > 0) o2.write(buf, 0, r);
                } finally { h.disconnect(); }
                msg = "Đã tải xong qua proxy: " + name + "\n" + where;
            } catch (Exception e) {
                msg = "Lỗi tải: " + e.getMessage();
            }
            final String fm = msg;
            ui.post(() -> android.widget.Toast.makeText(c, fm, android.widget.Toast.LENGTH_LONG).show());
        }).start();
    }
}
