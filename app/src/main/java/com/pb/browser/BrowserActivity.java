package com.pb.browser;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.os.Process;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.GeolocationPermissions;
import android.webkit.HttpAuthHandler;
import android.webkit.PermissionRequest;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.webkit.ProxyConfig;
import androidx.webkit.ProxyController;
import androidx.webkit.ScriptHandler;
import androidx.webkit.WebViewCompat;
import androidx.webkit.WebViewFeature;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BrowserActivity extends Activity {
    static class Tab {
        SwipeRefreshLayout holder; WebView web; LinearLayout chip; TextView chipTitle;
        String title = "Thẻ mới", url = "", script; boolean desktop, loading; ScriptHandler handler;
    }

    final ArrayList<Tab> tabs = new ArrayList<>();
    Tab cur;
    LinearLayout stripRow;
    HorizontalScrollView stripScroll;
    FrameLayout content;
    EditText bar;
    ImageView secIcon, reloadBtn;
    ProgressBar progress;

    String uuid, lang, rtc, customUa = "", baseUa, home, proxyUser, proxyPass;
    boolean hasProxy, docStartOk;
    int authTries = 0;
    ValueCallback<Uri[]> fileCb;
    View customView;
    WebChromeClient.CustomViewCallback customCb;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        JSONObject p;
        try { p = new JSONObject(getIntent().getStringExtra("profile")); }
        catch (Exception e) { finish(); return; }
        uuid = p.optString("uuid", "default");
        lang = p.optString("lang", "en-US");
        rtc = p.optString("rtc", "off");
        customUa = p.optString("ua", "").trim();
        home = p.optString("url", "https://www.google.com");
        if (home.isEmpty()) home = "https://www.google.com";

        // Moi UUID = thu muc du lieu WebView rieng (phai goi truoc khi tao WebView dau tien)
        try { WebView.setDataDirectorySuffix(uuid); } catch (Exception ignored) {}

        String def = WebSettings.getDefaultUserAgent(this);
        baseUa = def.replace("; wv", "").replaceAll("Version/[\\d.]+ ", "");
        docStartOk = WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT);
        CookieManager.getInstance().setAcceptCookie(true);

        buildUi();

        String proxyStr = p.optString("proxy", "").trim();
        if (proxyStr.isEmpty()) {
            toast("Cảnh báo: đang KHÔNG dùng proxy");
            restoreTabs();
        } else {
            hasProxy = true;
            applyProxy(proxyStr, this::restoreTabs);
        }
    }

    // ================= UI =================
    void buildUi() {
        getWindow().setStatusBarColor(0xFFDEE1E6);
        getWindow().setNavigationBarColor(UI.WHITE);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFFDEE1E6);
        root.setFitsSystemWindows(true);

        // --- thanh tab ---
        stripScroll = new HorizontalScrollView(this);
        stripScroll.setHorizontalScrollBarEnabled(false);
        stripRow = new LinearLayout(this);
        stripRow.setPadding(UI.dp(this, 6), 0, UI.dp(this, 6), 0);
        ImageView add = UI.icon(this, R.drawable.ic_add, 20);
        add.setPadding(UI.dp(this, 8), UI.dp(this, 8), UI.dp(this, 8), UI.dp(this, 8));
        add.setBackground(UI.ripple(null));
        add.setOnClickListener(v -> { Tab t = newTab(home, true); });
        LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(UI.dp(this, 36), UI.dp(this, 36));
        alp.topMargin = UI.dp(this, 4);
        stripRow.addView(add, alp);
        stripScroll.addView(stripRow, new ViewGroup.LayoutParams(-2, -1));
        root.addView(stripScroll, new LinearLayout.LayoutParams(-1, UI.dp(this, 40)));

        // --- thanh dia chi ---
        LinearLayout tool = new LinearLayout(this);
        tool.setGravity(Gravity.CENTER_VERTICAL);
        tool.setBackgroundColor(UI.WHITE);
        tool.setPadding(UI.dp(this, 4), UI.dp(this, 4), UI.dp(this, 4), UI.dp(this, 4));
        tool.addView(navBtn(R.drawable.ic_back, v -> { if (cur != null && cur.web.canGoBack()) cur.web.goBack(); }));
        tool.addView(navBtn(R.drawable.ic_forward, v -> { if (cur != null && cur.web.canGoForward()) cur.web.goForward(); }));
        reloadBtn = navBtn(R.drawable.ic_refresh, v -> {
            if (cur == null) return;
            if (cur.loading) cur.web.stopLoading(); else cur.web.reload();
        });
        tool.addView(reloadBtn);

        LinearLayout pill = new LinearLayout(this);
        pill.setGravity(Gravity.CENTER_VERTICAL);
        pill.setBackground(UI.round(this, 0xFFF1F3F4, 20));
        pill.setPadding(UI.dp(this, 12), 0, UI.dp(this, 8), 0);
        secIcon = UI.icon(this, R.drawable.ic_search, 16);
        LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(UI.dp(this, 16), UI.dp(this, 16));
        ilp.rightMargin = UI.dp(this, 8);
        pill.addView(secIcon, ilp);
        bar = new EditText(this);
        bar.setBackground(null);
        bar.setSingleLine(true);
        bar.setTextSize(14);
        bar.setTextColor(UI.TEXT);
        bar.setHint("Tìm kiếm hoặc nhập địa chỉ");
        bar.setImeOptions(EditorInfo.IME_ACTION_GO);
        bar.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        bar.setSelectAllOnFocus(true);
        bar.setOnFocusChangeListener((v, f) -> {
            if (cur == null) return;
            if (f) { bar.setText(cur.url); bar.selectAll(); } else showHost(cur);
        });
        bar.setOnEditorActionListener((v, a, e) -> { go(bar.getText().toString()); return true; });
        pill.addView(bar, new LinearLayout.LayoutParams(0, -1, 1));
        LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(0, UI.dp(this, 40), 1);
        plp.setMargins(UI.dp(this, 4), 0, UI.dp(this, 4), 0);
        tool.addView(pill, plp);
        ImageView more = navBtn(R.drawable.ic_more, null);
        more.setOnClickListener(v -> showMenu(more));
        tool.addView(more);
        root.addView(tool, new LinearLayout.LayoutParams(-1, -2));

        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100);
        progress.getProgressDrawable().setColorFilter(UI.BLUE, PorterDuff.Mode.SRC_IN);
        progress.setVisibility(View.INVISIBLE);
        root.addView(progress, new LinearLayout.LayoutParams(-1, UI.dp(this, 3)));

        content = new FrameLayout(this);
        content.setBackgroundColor(UI.WHITE);
        root.addView(content, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
    }

    ImageView navBtn(int res, View.OnClickListener l) {
        ImageView v = UI.icon(this, res, 40);
        int pd = UI.dp(this, 9);
        v.setPadding(pd, pd, pd, pd);
        v.setBackground(UI.ripple(null));
        if (l != null) v.setOnClickListener(l);
        return v;
    }

    void buildChip(Tab t) {
        LinearLayout c = new LinearLayout(this);
        c.setGravity(Gravity.CENTER_VERTICAL);
        c.setPadding(UI.dp(this, 12), 0, UI.dp(this, 2), 0);
        TextView tv = UI.text(this, t.title, 12, UI.TEXT, false);
        tv.setSingleLine(true);
        tv.setEllipsize(TextUtils.TruncateAt.END);
        c.addView(tv, new LinearLayout.LayoutParams(UI.dp(this, 110), -2));
        ImageView x = UI.icon(this, R.drawable.ic_close, 28);
        int pd = UI.dp(this, 7);
        x.setPadding(pd, pd, pd, pd);
        x.setBackground(UI.ripple(null));
        x.setOnClickListener(v -> closeTab(t));
        c.addView(x);
        c.setOnClickListener(v -> select(t));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, UI.dp(this, 34));
        lp.setMargins(UI.dp(this, 2), UI.dp(this, 6), 0, 0);
        stripRow.addView(c, stripRow.getChildCount() - 1, lp);
        t.chip = c; t.chipTitle = tv;
    }

    void styleChips() {
        float r = UI.dp(this, 10);
        for (Tab t : tabs) {
            if (t == cur) {
                GradientDrawable g = new GradientDrawable();
                g.setColor(UI.WHITE);
                g.setCornerRadii(new float[]{r, r, r, r, 0, 0, 0, 0});
                t.chip.setBackground(g);
                t.chipTitle.setTextColor(UI.TEXT);
            } else {
                t.chip.setBackground(null);
                t.chipTitle.setTextColor(UI.GREY);
            }
        }
    }

    // ================= Tabs =================
    Tab newTab(String url, boolean select) {
        final Tab t = new Tab();
        t.web = new WebView(this);
        configure(t);
        t.holder = new SwipeRefreshLayout(this);
        t.holder.addView(t.web, new ViewGroup.LayoutParams(-1, -1));
        t.holder.setColorSchemeColors(UI.BLUE);
        t.holder.setOnRefreshListener(() -> t.web.reload());
        t.holder.setVisibility(View.GONE);
        content.addView(t.holder, new FrameLayout.LayoutParams(-1, -1));
        buildChip(t);
        tabs.add(t);
        if (select) select(t);
        if (url != null) t.web.loadUrl(url);
        return t;
    }

    void select(Tab t) {
        for (Tab x : tabs) x.holder.setVisibility(x == t ? View.VISIBLE : View.GONE);
        cur = t;
        styleChips();
        t.chip.post(() -> stripScroll.smoothScrollTo(Math.max(0, t.chip.getLeft() - UI.dp(this, 40)), 0));
        bar.clearFocus();
        showHost(t);
        progress.setVisibility(t.loading ? View.VISIBLE : View.INVISIBLE);
        reloadBtn.setImageResource(t.loading ? R.drawable.ic_close : R.drawable.ic_refresh);
        t.web.requestFocus();
    }

    void closeTab(Tab t) {
        int i = tabs.indexOf(t);
        if (i < 0) return;
        tabs.remove(t);
        stripRow.removeView(t.chip);
        content.removeView(t.holder);
        t.web.stopLoading();
        t.holder.removeAllViews();
        t.web.destroy();
        if (tabs.isEmpty()) newTab(home, true);
        else if (cur == t) select(tabs.get(Math.min(i, tabs.size() - 1)));
    }

    void showHost(Tab t) {
        if (bar.hasFocus()) return;
        String u = t.url;
        if (u == null || u.isEmpty()) { bar.setText(""); secIcon.setImageResource(R.drawable.ic_search); return; }
        String h = Uri.parse(u).getHost();
        if (h == null) h = u; else if (h.startsWith("www.")) h = h.substring(4);
        bar.setText(h);
        secIcon.setImageResource(u.startsWith("https://") ? R.drawable.ic_lock : R.drawable.ic_search);
    }

    void go(String input) {
        String u = input.trim();
        if (u.isEmpty() || cur == null) return;
        if (!u.matches("^[a-zA-Z][a-zA-Z0-9+.-]*://.*")) {
            if (!u.contains(" ") && (u.contains(".") || u.startsWith("localhost")) && !u.endsWith(".")) u = "https://" + u;
            else {
                try { u = "https://www.google.com/search?q=" + URLEncoder.encode(u, "UTF-8"); } catch (Exception ignored) {}
            }
        }
        cur.web.loadUrl(u);
        bar.clearFocus();
        InputMethodManager im = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        im.hideSoftInputFromWindow(bar.getWindowToken(), 0);
        cur.web.requestFocus();
    }

    // ================= WebView =================
    String effectiveUa(Tab t) {
        if (t.desktop) {
            Matcher m = Pattern.compile("Chrome/([\\d.]+)").matcher(baseUa);
            String v = m.find() ? m.group(1) : "130.0.0.0";
            return "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/" + v + " Safari/537.36";
        }
        return customUa.isEmpty() ? baseUa : customUa;
    }

    void applyUa(Tab t) {
        String ua = effectiveUa(t);
        WebSettings s = t.web.getSettings();
        s.setUserAgentString(ua);
        boolean mobile = ua.contains("Mobile") || ua.contains("Android");
        s.setUseWideViewPort(true);
        s.setLoadWithOverviewMode(!mobile);
    }

    String buildScript(Tab t) {
        try {
            String ua = effectiveUa(t);
            boolean mobile = ua.contains("Mobile") || ua.contains("Android");
            int seed = uuid.hashCode();
            int[] cores = {4, 6, 8, 12};
            JSONObject cfg = new JSONObject();
            cfg.put("ua", ua);
            cfg.put("spoofUa", !customUa.isEmpty() || t.desktop);
            cfg.put("platform", platformOf(ua));
            cfg.put("lang", lang);
            cfg.put("cores", cores[(seed >>> 3) & 3]);
            cfg.put("mem", (seed & 1) == 0 ? 4 : 8);
            cfg.put("seed", seed & 0x7fffffff);
            cfg.put("touch", mobile ? 5 : 0);
            cfg.put("rtc", rtc);
            return readAsset("inject.js").replace("__CFG__", cfg.toString());
        } catch (Exception e) { return ""; }
    }

    void installScript(Tab t) {
        t.script = buildScript(t);
        if (docStartOk) {
            if (t.handler != null) t.handler.remove();
            t.handler = WebViewCompat.addDocumentStartJavaScript(t.web, t.script, Collections.singleton("*"));
        }
    }

    void configure(final Tab t) {
        WebView w = t.web;
        WebSettings s = w.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        s.setGeolocationEnabled(false);
        s.setSupportMultipleWindows(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setSupportZoom(true);
        s.setBuiltInZoomControls(true);
        s.setDisplayZoomControls(false);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        CookieManager.getInstance().setAcceptThirdPartyCookies(w, true);
        applyUa(t);
        installScript(t);

        w.setWebViewClient(new WebViewClient() {
            @Override public void onPageStarted(WebView v, String u, Bitmap f) {
                if (!docStartOk && t.script != null) v.evaluateJavascript(t.script, null);
                t.url = u; t.loading = true;
                if (t == cur) {
                    showHost(t);
                    progress.setVisibility(View.VISIBLE);
                    reloadBtn.setImageResource(R.drawable.ic_close);
                }
            }
            @Override public void onPageFinished(WebView v, String u) {
                t.url = u; t.loading = false; authTries = 0;
                t.holder.setRefreshing(false);
                if (t.title == null || t.title.isEmpty() || "Thẻ mới".equals(t.title)) {
                    String h = Uri.parse(u).getHost();
                    if (h != null) { t.title = h; t.chipTitle.setText(h); }
                }
                if (t == cur) {
                    showHost(t);
                    progress.setVisibility(View.INVISIBLE);
                    reloadBtn.setImageResource(R.drawable.ic_refresh);
                }
            }
            @Override public void onReceivedHttpAuthRequest(WebView v, HttpAuthHandler h, String host, String realm) {
                if (proxyUser != null && authTries++ < 3) h.proceed(proxyUser, proxyPass); else h.cancel();
            }
            @Override public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                String u = r.getUrl().toString();
                if (u.startsWith("http://") || u.startsWith("https://") || u.startsWith("about:")
                        || u.startsWith("blob:") || u.startsWith("data:") || u.startsWith("javascript:")) return false;
                if (u.startsWith("tel:") || u.startsWith("mailto:") || u.startsWith("sms:")) {
                    try { startActivity(new Intent(Intent.ACTION_VIEW, r.getUrl())); } catch (Exception ignored) {}
                }
                return true;
            }
            @Override public void onReceivedError(WebView v, WebResourceRequest r, WebResourceError e) {
                if (!r.isForMainFrame()) return;
                String html = "<html><head><meta name='viewport' content='width=device-width,initial-scale=1'></head>"
                        + "<body style='font-family:sans-serif;text-align:center;padding:64px 24px;color:#202124'>"
                        + "<div style='font-size:56px'>&#9888;</div><h2>Không thể mở trang</h2>"
                        + "<p style='color:#5f6368'>" + TextUtils.htmlEncode(String.valueOf(e.getDescription())) + "</p>"
                        + "<p style='color:#5f6368'>Kiểm tra lại proxy hoặc kết nối mạng.</p></body></html>";
                v.loadDataWithBaseURL(r.getUrl().toString(), html, "text/html", "utf-8", r.getUrl().toString());
            }
        });

        w.setWebChromeClient(new WebChromeClient() {
            @Override public void onProgressChanged(WebView v, int p) {
                if (t == cur) { progress.setProgress(p); progress.setVisibility(p >= 100 ? View.INVISIBLE : View.VISIBLE); }
            }
            @Override public void onReceivedTitle(WebView v, String title) {
                if (title != null && !title.isEmpty()) { t.title = title; t.chipTitle.setText(title); }
            }
            @Override public boolean onCreateWindow(WebView v, boolean dialog, boolean gesture, Message msg) {
                if (!gesture) return false;
                Tab n = newTab(null, true);
                WebView.WebViewTransport tr = (WebView.WebViewTransport) msg.obj;
                tr.setWebView(n.web);
                msg.sendToTarget();
                return true;
            }
            @Override public void onCloseWindow(WebView v) { closeTab(t); }
            @Override public void onPermissionRequest(PermissionRequest r) { r.deny(); }
            @Override public void onGeolocationPermissionsShowPrompt(String o, GeolocationPermissions.Callback c) { c.invoke(o, false, false); }
            @Override public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> cb, FileChooserParams p) {
                if (fileCb != null) fileCb.onReceiveValue(null);
                fileCb = cb;
                try { startActivityForResult(p.createIntent(), 77); }
                catch (Exception e) { fileCb = null; return false; }
                return true;
            }
            @Override public void onShowCustomView(View view, CustomViewCallback cb) {
                if (customView != null) { cb.onCustomViewHidden(); return; }
                customView = view; customCb = cb;
                ((FrameLayout) getWindow().getDecorView()).addView(view, new FrameLayout.LayoutParams(-1, -1));
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            }
            @Override public void onHideCustomView() { hideCustom(); }
        });

        w.setDownloadListener((url, ua, cd, mime, len) -> {
            if (hasProxy) { toast("Tải file bị chặn khi dùng proxy (tránh lộ IP thật)"); return; }
            try {
                DownloadManager.Request r = new DownloadManager.Request(Uri.parse(url));
                r.setMimeType(mime);
                r.addRequestHeader("User-Agent", ua);
                String ck = CookieManager.getInstance().getCookie(url);
                if (ck != null) r.addRequestHeader("Cookie", ck);
                r.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                r.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, URLUtil.guessFileName(url, cd, mime));
                ((DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE)).enqueue(r);
                toast("Đang tải xuống…");
            } catch (Exception e) { toast("Lỗi tải: " + e.getMessage()); }
        });
    }

    void hideCustom() {
        if (customView == null) return;
        ((FrameLayout) getWindow().getDecorView()).removeView(customView);
        customView = null;
        if (customCb != null) customCb.onCustomViewHidden();
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
    }

    // ================= Proxy =================
    void applyProxy(String raw, Runnable then) {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
            toast("WebView này không hỗ trợ proxy. Hãy cập nhật Android System WebView / Chrome.");
            return;
        }
        String scheme = "http", rest = raw.trim();
        int i = rest.indexOf("://");
        if (i > 0) { scheme = rest.substring(0, i).toLowerCase(); rest = rest.substring(i + 3); }
        String hostPort = rest;
        if (rest.contains("@")) {
            int at = rest.lastIndexOf('@');
            String cred = rest.substring(0, at); hostPort = rest.substring(at + 1);
            int c = cred.indexOf(':');
            proxyUser = c < 0 ? cred : cred.substring(0, c);
            proxyPass = c < 0 ? "" : cred.substring(c + 1);
        } else {
            String[] a = rest.split(":");
            if (a.length == 4) { hostPort = a[0] + ":" + a[1]; proxyUser = a[2]; proxyPass = a[3]; }
        }
        try {
            ProxyConfig cfg = new ProxyConfig.Builder().addProxyRule(scheme + "://" + hostPort).build();
            ProxyController.getInstance().setProxyOverride(cfg, getMainExecutor(), then);
        } catch (Exception e) { toast("Proxy lỗi: " + e.getMessage()); }
    }

    // ================= Menu / lifecycle =================
    void showMenu(View anchor) {
        PopupMenu m = new PopupMenu(this, anchor);
        m.getMenu().add(0, 1, 0, "Thẻ mới");
        m.getMenu().add(0, 2, 0, "Trang chủ");
        m.getMenu().add(0, 3, 0, "Trang web cho máy tính").setCheckable(true).setChecked(cur != null && cur.desktop);
        m.getMenu().add(0, 4, 0, "Chia sẻ");
        m.getMenu().add(0, 5, 0, "Sao chép liên kết");
        m.getMenu().add(0, 6, 0, "Xóa cookie của hồ sơ");
        m.getMenu().add(0, 7, 0, "Thoát hồ sơ");
        m.setOnMenuItemClickListener(it -> {
            switch (it.getItemId()) {
                case 1: newTab(home, true); break;
                case 2: if (cur != null) cur.web.loadUrl(home); break;
                case 3:
                    if (cur != null) {
                        cur.desktop = !cur.desktop;
                        applyUa(cur); installScript(cur); cur.web.reload();
                    }
                    break;
                case 4:
                    if (cur != null) {
                        Intent s = new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, cur.url);
                        startActivity(Intent.createChooser(s, "Chia sẻ"));
                    }
                    break;
                case 5:
                    if (cur != null) {
                        ((ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("url", cur.url));
                        toast("Đã sao chép");
                    }
                    break;
                case 6:
                    CookieManager.getInstance().removeAllCookies(null);
                    WebStorage.getInstance().deleteAllData();
                    toast("Đã xóa cookie và dữ liệu web");
                    break;
                case 7: finish(); break;
            }
            return true;
        });
        m.show();
    }

    void restoreTabs() {
        JSONArray saved;
        int sel = 0;
        try {
            JSONObject o = new JSONObject(getSharedPreferences("tabs", 0).getString(uuid, "{}"));
            saved = o.optJSONArray("urls");
            sel = o.optInt("sel", 0);
        } catch (Exception e) { saved = null; }
        if (saved == null || saved.length() == 0) { newTab(home, true); return; }
        for (int i = 0; i < saved.length(); i++) newTab(saved.optString(i), false);
        select(tabs.get(Math.max(0, Math.min(sel, tabs.size() - 1))));
    }

    void saveTabs() {
        try {
            JSONArray a = new JSONArray();
            for (Tab t : tabs) if (t.url != null && t.url.startsWith("http")) a.put(t.url);
            JSONObject o = new JSONObject().put("urls", a).put("sel", Math.max(0, tabs.indexOf(cur)));
            getSharedPreferences("tabs", 0).edit().putString(uuid, o.toString()).apply();
        } catch (Exception ignored) {}
    }

    @Override protected void onPause() {
        super.onPause();
        saveTabs();
        CookieManager.getInstance().flush();
        for (Tab t : tabs) t.web.onPause();
    }

    @Override protected void onResume() {
        super.onResume();
        for (Tab t : tabs) t.web.onResume();
    }

    @Override protected void onActivityResult(int rq, int rs, Intent d) {
        if (rq == 77 && fileCb != null) {
            fileCb.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(rs, d));
            fileCb = null;
        } else super.onActivityResult(rq, rs, d);
    }

    @Override public void onBackPressed() {
        if (customView != null) { hideCustom(); return; }
        if (bar.hasFocus()) { bar.clearFocus(); return; }
        if (cur != null && cur.web.canGoBack()) cur.web.goBack();
        else if (tabs.size() > 1) closeTab(cur);
        else finish();
    }

    static String platformOf(String ua) {
        if (ua.contains("iPhone")) return "iPhone";
        if (ua.contains("Android")) return "Linux armv8l";
        if (ua.contains("Windows")) return "Win32";
        if (ua.contains("Macintosh")) return "MacIntel";
        return "Linux x86_64";
    }

    String readAsset(String n) throws Exception {
        try (InputStream in = getAssets().open(n)) {
            java.io.ByteArrayOutputStream o = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[4096]; int r;
            while ((r = in.read(buf)) > 0) o.write(buf, 0, r);
            return o.toString("UTF-8");
        }
    }

    void toast(String m) { Toast.makeText(this, m, Toast.LENGTH_LONG).show(); }

    @Override protected void onDestroy() {
        super.onDestroy();
        // Giet process de lan mo sau dung proxy/UUID moi hoan toan
        if (isFinishing()) Process.killProcess(Process.myPid());
    }
}
