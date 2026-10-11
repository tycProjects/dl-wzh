package com.leonteam.sourceweb;

import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;

/** In-app preview: live URL, downloaded HTML snapshot, or a website unpacked from a ZIP. */
public class PreviewActivity extends Activity {
    private static final String DESKTOP_UA = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36";
    private static final String ZIP_HOST = "zip.leon";
    private static final HashMap<String, String> MIME = new HashMap<>();
    static {
        String[][] m = {{"html", "text/html"}, {"htm", "text/html"}, {"xhtml", "application/xhtml+xml"}, {"css", "text/css"},
                {"js", "text/javascript"}, {"mjs", "text/javascript"}, {"json", "application/json"}, {"map", "application/json"},
                {"webmanifest", "application/manifest+json"}, {"svg", "image/svg+xml"}, {"png", "image/png"}, {"jpg", "image/jpeg"},
                {"jpeg", "image/jpeg"}, {"gif", "image/gif"}, {"webp", "image/webp"}, {"avif", "image/avif"}, {"ico", "image/x-icon"},
                {"bmp", "image/bmp"}, {"woff", "font/woff"}, {"woff2", "font/woff2"}, {"ttf", "font/ttf"}, {"otf", "font/otf"},
                {"mp4", "video/mp4"}, {"webm", "video/webm"}, {"mp3", "audio/mpeg"}, {"ogg", "audio/ogg"}, {"wav", "audio/wav"},
                {"txt", "text/plain"}, {"xml", "text/xml"}, {"wasm", "application/wasm"}};
        for (String[] x : m) MIME.put(x[0], x[1]);
    }

    private Theme th;
    private WebView web;
    private TextView titleView, jsChip, deskChip, offChip;
    private ProgressBar bar;
    private LinearLayout rootView;
    private String mode = "", urlArg = "", defaultUa = "";
    private File root;
    private String rootCanon = "";
    private boolean js = true, desktop = false, offline = false;
    private int inL, inT, inR, inB;

    private int dp(float v) { return (int) (v * getResources().getDisplayMetrics().density + .5f); }
    private static String nz(String s) { return s == null ? "" : s; }
    private static int alpha(int c, int a) { return Color.argb(a, Color.red(c), Color.green(c), Color.blue(c)); }

    private GradientDrawable rect(int fill, int stroke, float r) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        d.setCornerRadius(dp(r));
        if (stroke != 0) d.setStroke(dp(1), stroke);
        return d;
    }

    private TextView chip(String text, boolean on) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setGravity(Gravity.CENTER);
        t.setTextSize(12.5f);
        t.setTypeface(Typeface.create(Theme.FAMILIES[th.uiFont], Typeface.BOLD));
        t.setSingleLine(true);
        t.setPadding(dp(13), dp(8), dp(13), dp(8));
        t.setClickable(true);
        style(t, on);
        GradientDrawable mask = new GradientDrawable();
        mask.setColor(Color.WHITE);
        mask.setCornerRadius(dp(14));
        t.setForeground(new RippleDrawable(ColorStateList.valueOf(alpha(th.accent, 70)), null, mask));
        return t;
    }

    private void style(TextView t, boolean on) {
        t.setTextColor(on ? th.ON_ACCENT : th.TEXT);
        t.setBackground(on ? rect(th.accent, 0, 14) : rect(th.PANEL, th.STROKE, 14));
    }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        th = Theme.load(getSharedPreferences("leon_prefs", MODE_PRIVATE));
        Intent in = getIntent();
        mode = nz(in.getStringExtra("mode"));
        urlArg = nz(in.getStringExtra("url"));
        String title = nz(in.getStringExtra("title"));
        if (mode.equals("zip")) {
            try {
                root = new File(nz(in.getStringExtra("root"))).getCanonicalFile();
                rootCanon = root.getPath();
            } catch (Exception e) { root = null; }
        }

        Window_setup();
        rootView = new LinearLayout(this);
        rootView.setOrientation(LinearLayout.VERTICAL);
        rootView.setBackgroundColor(th.BG);
        rootView.setPadding(dp(12) + inL, dp(8) + inT, dp(12) + inR, dp(8) + inB);
        rootView.setOnApplyWindowInsetsListener((v, ins) -> {
            inL = ins.getSystemWindowInsetLeft(); inT = ins.getSystemWindowInsetTop();
            inR = ins.getSystemWindowInsetRight(); inB = ins.getSystemWindowInsetBottom();
            v.setPadding(dp(12) + inL, dp(8) + inT, dp(12) + inR, dp(8) + inB);
            return ins;
        });
        setContentView(rootView);
        rootView.requestApplyInsets();

        // ---- title row
        LinearLayout head = new LinearLayout(this);
        head.setGravity(Gravity.CENTER_VERTICAL);
        titleView = new TextView(this);
        titleView.setText(title.isEmpty() ? "Xem trước" : title);
        titleView.setTextColor(th.TEXT);
        titleView.setTextSize(15);
        titleView.setTypeface(Typeface.create(Theme.FAMILIES[th.uiFont], Typeface.BOLD));
        titleView.setSingleLine(true);
        titleView.setEllipsize(TextUtils.TruncateAt.END);
        head.addView(titleView, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        TextView close = chip("Đóng", true);
        close.setOnClickListener(v -> finish());
        head.addView(close, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(36)));
        rootView.addView(head);

        // ---- control chips
        HorizontalScrollView sc = new HorizontalScrollView(this);
        sc.setHorizontalScrollBarEnabled(false);
        LinearLayout row = new LinearLayout(this);
        TextView back = chip("← Lùi", false), reload = chip("↻ Tải lại", false);
        jsChip = chip("", js); deskChip = chip("", desktop); offChip = chip("", offline);
        refreshChips();
        TextView[] cs = {back, reload, jsChip, deskChip};
        for (TextView c : cs) row.addView(c, margins(dp(36)));
        if (!mode.equals("url")) row.addView(offChip, margins(dp(36)));
        else {
            TextView ext = chip("Mở trình duyệt", false);
            ext.setOnClickListener(v -> {
                try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(web.getUrl() != null ? web.getUrl() : urlArg))); }
                catch (Exception e) { Toast.makeText(this, "Không mở được trình duyệt", Toast.LENGTH_SHORT).show(); }
            });
            row.addView(ext, margins(dp(36)));
        }
        sc.addView(row);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sp.topMargin = dp(8);
        rootView.addView(sc, sp);

        bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(100);
        bar.setProgressTintList(ColorStateList.valueOf(th.accent));
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(3));
        bp.topMargin = dp(8); bp.bottomMargin = dp(8);
        rootView.addView(bar, bp);

        // ---- web view
        web = new WebView(this);
        defaultUa = web.getSettings().getUserAgentString();
        applyWebSettings();
        web.setWebViewClient(new WebViewClient() {
            @Override public WebResourceResponse shouldInterceptRequest(WebView v, WebResourceRequest req) {
                Uri u = req.getUrl();
                if (ZIP_HOST.equals(u.getHost()) && root != null) return serveZip(u);
                String sch = u.getScheme();
                if (offline && ("http".equals(sch) || "https".equals(sch))) return reply(403, "text/plain", "Đã chặn (chế độ offline)");
                return null;
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override public void onProgressChanged(WebView v, int p) {
                bar.setProgress(p);
                bar.setVisibility(p >= 100 ? View.INVISIBLE : View.VISIBLE);
            }
            @Override public void onReceivedTitle(WebView v, String t) {
                if (t != null && !t.isEmpty()) titleView.setText(t);
            }
        });
        rootView.addView(web, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        back.setOnClickListener(v -> { if (web.canGoBack()) web.goBack(); else finish(); });
        reload.setOnClickListener(v -> web.reload());
        jsChip.setOnClickListener(v -> { js = !js; refreshChips(); applyWebSettings(); web.reload(); });
        deskChip.setOnClickListener(v -> { desktop = !desktop; refreshChips(); applyWebSettings(); web.reload(); });
        offChip.setOnClickListener(v -> { offline = !offline; refreshChips(); web.reload(); });

        loadContent(in);
    }

    private void Window_setup() {
        getWindow().setBackgroundDrawable(new ColorDrawable(th.BG));
        getWindow().setStatusBarColor(th.BG);
        getWindow().setNavigationBarColor(th.BG);
        View dv = getWindow().getDecorView();
        int f = dv.getSystemUiVisibility();
        int light = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        if (android.os.Build.VERSION.SDK_INT >= 26) light |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        if (th.bgMode == 3) f |= light; else f &= ~light;
        dv.setSystemUiVisibility(f);
    }

    private LinearLayout.LayoutParams margins(int h) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, h);
        p.rightMargin = dp(8);
        return p;
    }

    private void refreshChips() {
        jsChip.setText("JavaScript: " + (js ? "BẬT" : "TẮT"));
        deskChip.setText("Giao diện máy tính: " + (desktop ? "BẬT" : "TẮT"));
        offChip.setText("Chặn mạng ngoài: " + (offline ? "BẬT" : "TẮT"));
        style(jsChip, js);
        style(deskChip, desktop);
        style(offChip, offline);
    }

    private void applyWebSettings() {
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(js);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setSupportZoom(true);
        s.setBuiltInZoomControls(true);
        s.setDisplayZoomControls(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        s.setUserAgentString(desktop ? DESKTOP_UA : defaultUa);
    }

    private void loadContent(Intent in) {
        try {
            if (mode.equals("zip")) {
                if (root == null) throw new Exception("Thư mục ZIP không hợp lệ");
                String start = nz(in.getStringExtra("start"));
                if (start.isEmpty()) start = "index.html";
                web.loadUrl(new Uri.Builder().scheme("https").authority(ZIP_HOST).path("/" + start).build().toString());
            } else if (mode.equals("html")) {
                File f = new File(nz(in.getStringExtra("file")));
                byte[] data = readAll(new FileInputStream(f));
                web.loadDataWithBaseURL(urlArg, new String(data, StandardCharsets.UTF_8), "text/html", "UTF-8", null);
            } else {
                web.loadUrl(urlArg);
            }
        } catch (Exception e) {
            Toast.makeText(this, "Không thể xem trước: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private static byte[] readAll(InputStream in) throws java.io.IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] b = new byte[16384];
        int n;
        while ((n = in.read(b)) > 0) out.write(b, 0, n);
        in.close();
        return out.toByteArray();
    }

    private WebResourceResponse reply(int code, String mime, String msg) {
        HashMap<String, String> h = new HashMap<>();
        h.put("Access-Control-Allow-Origin", "*");
        return new WebResourceResponse(mime, "UTF-8", code, code == 404 ? "Not Found" : "Blocked", h,
                new ByteArrayInputStream(msg.getBytes(StandardCharsets.UTF_8)));
    }

    private WebResourceResponse serveZip(Uri u) {
        try {
            String rel = u.getPath();
            if (rel == null || rel.isEmpty() || rel.equals("/")) rel = "/index.html";
            File f = new File(root, rel).getCanonicalFile();
            if (!f.getPath().equals(rootCanon) && !f.getPath().startsWith(rootCanon + File.separator)) return reply(403, "text/plain", "Forbidden");
            if (f.isDirectory()) f = new File(f, "index.html");
            if (!f.isFile()) return reply(404, "text/plain", "Không tìm thấy: " + rel);
            String name = f.getName();
            int dot = name.lastIndexOf('.');
            String mime = dot >= 0 ? MIME.get(name.substring(dot + 1).toLowerCase(Locale.ROOT)) : null;
            if (mime == null) mime = "application/octet-stream";
            boolean text = mime.startsWith("text/") || mime.contains("json") || mime.contains("xml") || mime.contains("javascript");
            HashMap<String, String> h = new HashMap<>();
            h.put("Access-Control-Allow-Origin", "*");
            h.put("Cache-Control", "no-cache");
            WebResourceResponse r = new WebResourceResponse(mime, text ? "UTF-8" : null, new FileInputStream(f));
            r.setResponseHeaders(h);
            return r;
        } catch (Exception e) {
            return reply(404, "text/plain", "Lỗi đọc tệp");
        }
    }

    @Override public void onBackPressed() {
        if (web != null && web.canGoBack()) web.goBack(); else super.onBackPressed();
    }

    @Override protected void onDestroy() {
        if (web != null) {
            ViewGroup p = (ViewGroup) web.getParent();
            if (p != null) p.removeView(web);
            web.destroy();
        }
        super.onDestroy();
    }
}
