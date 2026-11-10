package com.pb.browser;

import android.app.Activity;
import android.app.Dialog;
import android.app.DownloadManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.os.Process;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.GeolocationPermissions;
import android.webkit.HttpAuthHandler;
import android.webkit.PermissionRequest;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.WebResourceResponse;
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
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.webkit.ProxyConfig;
import androidx.webkit.ProxyController;
import androidx.webkit.ScriptHandler;
import androidx.webkit.WebViewCompat;
import androidx.webkit.WebViewFeature;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
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
        SwipeRefreshLayout holder; WebView web; LinearLayout chip; TextView chipTitle; View chipDot; ImageView chipClose;
        String title = "Thẻ mới", url = "", script, pendingUrl; boolean desktop, loading, pullOk, imgForced; ScriptHandler handler; int crashes;
    }

    final ArrayList<Tab> tabs = new ArrayList<>();
    final ArrayList<String> closedUrls = new ArrayList<>();
    Tab cur;
    LinearLayout stripRow;
    HorizontalScrollView stripScroll;
    FrameLayout content;
    EditText bar;
    ImageView secIcon, reloadBtn, backBtn, fwdBtn, stripShield;
    TextView tabCount, stripName, stripProxy, stripTabs;
    ProgressBar progress;

    String openUrl = ""; boolean knownDevice; String uuid, lang, rtc, tz = "", customUa = "", baseUa, home, proxyUser, proxyPass, profileName, ptype = "http";
    JSONObject deviceFp; String deviceName = "";
    boolean hasProxy, docStartOk;
    boolean highFps, hwAccel = true, blockImg, perfHintOn;
    Object hintSession; android.view.Choreographer.FrameCallback hintCb; long lastFrame;
    long authWin; int authCount;
    // MASTER / Turbo
    boolean master, adblock, lazy, darkOn; int split;
    Turbo.Px px; String proxyHost = "", proxyLabelStr = "", turboSrc;
    final Set<String> reqHosts = java.util.Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
    final AtomicInteger nDirect = new AtomicInteger(), nBlocked = new AtomicInteger();
    TextView stripTurbo;
    final Handler ui = new Handler(Looper.getMainLooper());
    boolean tabsStarted; String injectSrc;
    ValueCallback<Uri[]> fileCb;
    View customView;
    WebChromeClient.CustomViewCallback customCb;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        UI.init(this);
        master = Master.isOn(this);
        split = master ? Master.split(this) : 0;
        adblock = master && Master.adblock(this);
        lazy = master && Master.lazy(this);
        darkOn = master && Master.darkWeb(this);
        JSONObject p;
        try { p = new JSONObject(getIntent().getStringExtra("profile")); }
        catch (Exception e) { finish(); return; }
        uuid = p.optString("uuid", "default");
        Turbo.profile = uuid;
        profileName = p.optString("name", "Hồ sơ");
        lang = p.optString("lang", "en-US");
        rtc = p.optString("rtc", "off");
        tz = p.optString("tz", "").trim();
        openUrl = p.optString("openUrl", "").trim();
        customUa = p.optString("ua", "").trim();
        home = p.optString("url", "https://www.google.com");
        if (home.isEmpty()) home = "https://www.google.com";
        deviceFp = p.optJSONObject("fp");
        String devId = p.optString("device", "");
        Devices.Dev dv = Devices.byId(devId);
        if (dv != null) deviceFp = dv.fp();
        else if ("desktop".equals(devId)) deviceFp = Devices.desktopFp();
        knownDevice = dv != null || "desktop".equals(devId);
        if ("desktop".equals(devId)) deviceName = "Máy tính Windows";
        else if (dv != null) deviceName = dv.name;
        else if (!customUa.isEmpty()) deviceName = "UA tùy chỉnh";

        // Moi UUID = thu muc du lieu WebView rieng (phai goi truoc khi tao WebView dau tien)
        try { WebView.setDataDirectorySuffix(uuid); } catch (Exception ignored) {}

        String def = WebSettings.getDefaultUserAgent(this);
        baseUa = def.replace("; wv", "").replaceAll("Version/[\\d.]+ ", "");
        docStartOk = WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT);
        CookieManager.getInstance().setAcceptCookie(true);

        buildUi();

        highFps = Store.perf(this, "highFps", false);
        hwAccel = Store.perf(this, "hwAccel", true);
        blockImg = Store.perf(this, "blockImg", false);
        perfHintOn = Store.perf(this, "perfHint", false);
        SysInfo.applyRefresh(this, highFps);

        String proxyStr = p.optString("proxy", "").replaceAll("\\s", "");
        ptype = Store.schemeOf(proxyStr, p.optString("ptype", "http"));
        stripName.setText(profileName);
        if (master) startTurboStats();
        if (proxyStr.isEmpty()) {
            stripShield.setColorFilter(UI.AMBER);
            stripProxy.setText("Kết nối trực tiếp · không ẩn IP");
            toast("Cảnh báo: đang KHÔNG dùng proxy");
            restoreTabs();
        } else {
            hasProxy = true;
            proxyLabelStr = Store.proxyLabel(proxyStr, ptype);
            stripShield.setColorFilter(UI.GREEN);
            stripProxy.setText(proxyLabelStr);
            installTurboCache();
            applyProxy(proxyStr, () -> {
                // Mui gio: nhap tay > cache theo (ho so + proxy) > do o nen (cho toi da 4s roi van tai trang)
                // Khoa cache la ma bam: truoc day khoa chua nguyen user:pass cua proxy (luu ro trong prefs)
                final String tzKey = uuid + "|" + Integer.toHexString(proxyStr.hashCode());
                final boolean manualTz = !tz.isEmpty();
                final String cached = manualTz ? "" : getSharedPreferences("tzcache", 0).getString(tzKey, "");
                if (!manualTz && !cached.isEmpty()) tz = cached;
                final boolean waitTz = !manualTz && cached.isEmpty();
                if (waitTz) {
                    stripProxy.setText("Đang đồng bộ múi giờ theo IP proxy…");
                    ui.postDelayed(this::startTabsOnce, 4000);
                } else startTabsOnce();
                // Luon do IP that cua proxy de hien tren thanh trang thai (va cap nhat mui gio neu IP doi nuoc)
                ProxyCheck.run(proxyStr, ptype, 6000, (r, err) -> {
                    if (isFinishing() || isDestroyed()) return;
                    if (err != null) {
                        stripProxy.setText(proxyLabelStr + " · chưa kiểm tra được IP");
                        if (waitTz) { toast("Không lấy được múi giờ từ proxy — dùng múi giờ máy"); startTabsOnce(); }
                        return;
                    }
                    stripProxy.setText(r.optString("query") + " · " + r.optString("countryCode") + " · " + r.optLong("ms") + "ms");
                    String z = r.optString("timezone");
                    if (!manualTz && !z.isEmpty() && !z.equals(tz)) {
                        tz = z;
                        getSharedPreferences("tzcache", 0).edit().putString(tzKey, z).apply();
                        for (Tab t : tabs) installScript(t);   // ap dung cho cac trang tai tiep theo
                        if (waitTz) toast("Múi giờ theo proxy: " + z + " (" + r.optString("countryCode") + ")");
                    }
                    if (waitTz) startTabsOnce();
                });
            });
        }
    }

    void startTabsOnce() {
        if (tabsStarted) return;
        tabsStarted = true;
        restoreTabs();
    }

    // ================= UI =================
    void buildUi() {
        UI.bars(this, UI.INK, UI.INK, false, false);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(UI.INK);
        root.setFitsSystemWindows(true);

        // --- vung 1: dai tab ---
        stripScroll = new HorizontalScrollView(this);
        stripScroll.setHorizontalScrollBarEnabled(false);
        stripRow = new LinearLayout(this);
        stripRow.setGravity(Gravity.CENTER_VERTICAL);
        stripRow.setPadding(UI.dp(this, 10), UI.dp(this, 6), UI.dp(this, 10), UI.dp(this, 6));
        ImageView add = UI.icon(this, R.drawable.ic_add, 32, UI.CYAN);
        int ap = UI.dp(this, 8);
        add.setPadding(ap, ap, ap, ap);
        add.setBackground(UI.rippleLight(UI.round(this, UI.INK_CHIP, 16)));
        add.setClickable(true);
        add.setOnClickListener(v -> newTab(home, true));
        stripRow.addView(add, new LinearLayout.LayoutParams(UI.dp(this, 32), UI.dp(this, 32)));
        stripScroll.addView(stripRow, new ViewGroup.LayoutParams(-2, -1));
        root.addView(stripScroll, new LinearLayout.LayoutParams(-1, UI.dp(this, 46)));

        // --- vung 2: thanh die huong + dia chi ---
        LinearLayout tool = new LinearLayout(this);
        tool.setGravity(Gravity.CENTER_VERTICAL);
        tool.setBackgroundColor(UI.INK);
        tool.setPadding(UI.dp(this, 8), UI.dp(this, 2), UI.dp(this, 8), UI.dp(this, 8));
        backBtn = navBtn(R.drawable.ic_back, v -> {
            if (cur != null && cur.web.canGoBack()) { cur.web.goBack(); cur.web.post(this::updateNav); }
        });
        fwdBtn = navBtn(R.drawable.ic_forward, v -> {
            if (cur != null && cur.web.canGoForward()) { cur.web.goForward(); cur.web.post(this::updateNav); }
        });
        reloadBtn = navBtn(R.drawable.ic_refresh, v -> {
            if (cur == null) return;
            if (cur.loading) cur.web.stopLoading(); else cur.web.reload();
        });
        tool.addView(backBtn);
        tool.addView(fwdBtn);
        tool.addView(reloadBtn);

        LinearLayout pill = new LinearLayout(this);
        pill.setGravity(Gravity.CENTER_VERTICAL);
        GradientDrawable pillBg = UI.round(this, UI.INK_3, 21);
        if (UI.master) pillBg.setStroke(UI.dp(this, 1), UI.alpha(UI.CYAN, 0x66));
        pill.setBackground(pillBg);
        pill.setPadding(UI.dp(this, 12), 0, UI.dp(this, 8), 0);
        secIcon = UI.icon(this, R.drawable.ic_search, 15, UI.ON_INK_MUTED);
        LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(UI.dp(this, 15), UI.dp(this, 15));
        ilp.rightMargin = UI.dp(this, 8);
        pill.addView(secIcon, ilp);
        bar = new EditText(this);
        bar.setBackground(null);
        bar.setSingleLine(true);
        bar.setTextSize(14);
        bar.setTextColor(UI.ON_INK);
        bar.setHintTextColor(UI.ON_INK_MUTED);
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
        tabCount = UI.text(this, "1", 11, UI.ON_INK, true);
        tabCount.setGravity(Gravity.CENTER);
        GradientDrawable tcb = UI.round(this, 0x00000000, 7);
        tcb.setStroke(UI.dp(this, 1), UI.ON_INK_MUTED);
        tabCount.setBackground(UI.rippleLight(tcb));
        tabCount.setClickable(true);
        tabCount.setOnClickListener(v -> showTabSheet());
        LinearLayout.LayoutParams tcl = new LinearLayout.LayoutParams(UI.dp(this, 24), UI.dp(this, 24));
        tcl.leftMargin = UI.dp(this, 6);
        pill.addView(tabCount, tcl);
        LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(0, UI.dp(this, 42), 1);
        plp.setMargins(UI.dp(this, 4), 0, UI.dp(this, 4), 0);
        tool.addView(pill, plp);
        ImageView more = navBtn(R.drawable.ic_more, null);
        more.setOnClickListener(v -> showMenu());
        tool.addView(more);
        root.addView(tool, new LinearLayout.LayoutParams(-1, -2));

        // --- vung 3: thanh trang thai ho so ---
        LinearLayout status = new LinearLayout(this);
        status.setGravity(Gravity.CENTER_VERTICAL);
        status.setBackgroundColor(UI.STATUS_BG);
        status.setPadding(UI.dp(this, 16), 0, UI.dp(this, 16), 0);
        if (UI.master) {
            RobotView rb = new RobotView(this, true);
            LinearLayout.LayoutParams rbl = new LinearLayout.LayoutParams(UI.dp(this, 26), UI.dp(this, 26));
            rbl.rightMargin = UI.dp(this, 6);
            status.addView(rb, rbl);
        }
        stripShield = UI.icon(this, R.drawable.ic_shield, 12, UI.GREEN);
        status.addView(stripShield);
        stripName = UI.text(this, "", 11, UI.STATUS_TEXT, true);
        stripName.setSingleLine(true);
        stripName.setEllipsize(TextUtils.TruncateAt.END);
        stripName.setMaxWidth(UI.dp(this, 140));
        stripName.setPadding(UI.dp(this, 6), 0, 0, 0);
        status.addView(stripName);
        stripProxy = UI.text(this, "", 11, UI.ON_INK_MUTED, false);
        stripProxy.setSingleLine(true);
        stripProxy.setEllipsize(TextUtils.TruncateAt.END);
        stripProxy.setPadding(UI.dp(this, 6), 0, UI.dp(this, 8), 0);
        status.addView(stripProxy, new LinearLayout.LayoutParams(0, -2, 1));
        if (UI.master) {
            stripTurbo = UI.text(this, "TURBO", 10, UI.CYAN, true);
            stripTurbo.setPadding(0, 0, UI.dp(this, 8), 0);
            status.addView(stripTurbo);
        }
        stripTabs = UI.text(this, "1 thẻ", 11, UI.ON_INK_MUTED, false);
        status.addView(stripTabs);
        root.addView(status, new LinearLayout.LayoutParams(-1, UI.dp(this, 30)));

        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100);
        GradientDrawable barGrad = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{UI.INDIGO, UI.CYAN});
        ClipDrawable clip = new ClipDrawable(barGrad, Gravity.LEFT, ClipDrawable.HORIZONTAL);
        LayerDrawable layers = new LayerDrawable(new Drawable[]{new ColorDrawable(0x00000000), clip});
        layers.setId(0, android.R.id.background);
        layers.setId(1, android.R.id.progress);
        progress.setProgressDrawable(layers);
        progress.setVisibility(View.INVISIBLE);
        root.addView(progress, new LinearLayout.LayoutParams(-1, UI.dp(this, 3)));

        content = new FrameLayout(this);
        content.setBackgroundColor(UI.master ? UI.INK : UI.WHITE);
        root.addView(content, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
        updateNav();
    }

    ImageView navBtn(int res, View.OnClickListener l) {
        ImageView v = UI.icon(this, res, 38, UI.ON_INK);
        int pd = UI.dp(this, 9);
        v.setPadding(pd, pd, pd, pd);
        v.setBackground(UI.rippleLight(null));
        v.setClickable(true);
        if (l != null) v.setOnClickListener(l);
        return v;
    }

    void updateNav() {
        boolean back = cur != null && cur.web.canGoBack();
        boolean fwd = cur != null && cur.web.canGoForward();
        backBtn.setAlpha(back ? 1f : 0.3f);
        fwdBtn.setAlpha(fwd ? 1f : 0.3f);
    }

    void updateCounts() {
        tabCount.setText(String.valueOf(tabs.size()));
        stripTabs.setText(tabs.size() + " thẻ");
    }

    void buildChip(Tab t) {
        LinearLayout c = new LinearLayout(this);
        c.setGravity(Gravity.CENTER_VERTICAL);
        c.setPadding(UI.dp(this, 11), 0, UI.dp(this, 4), 0);
        View dot = new View(this);
        c.addView(dot, new LinearLayout.LayoutParams(UI.dp(this, 7), UI.dp(this, 7)));
        TextView tv = UI.text(this, t.title, 12, UI.ON_INK_MUTED, false);
        tv.setSingleLine(true);
        tv.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(UI.dp(this, 104), -2);
        tlp.leftMargin = UI.dp(this, 7);
        c.addView(tv, tlp);
        ImageView x = UI.icon(this, R.drawable.ic_close, 26, UI.ON_INK_MUTED);
        int pd = UI.dp(this, 6);
        x.setPadding(pd, pd, pd, pd);
        x.setBackground(UI.rippleLight(null));
        x.setClickable(true);
        x.setOnClickListener(v -> closeTab(t));
        c.addView(x);
        c.setClickable(true);
        c.setOnClickListener(v -> select(t));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, UI.dp(this, 34));
        lp.rightMargin = UI.dp(this, 6);
        stripRow.addView(c, stripRow.getChildCount() - 1, lp);
        t.chip = c; t.chipTitle = tv; t.chipDot = dot; t.chipClose = x;
    }

    void styleChips() {
        for (Tab t : tabs) {
            boolean active = t == cur;
            t.chip.setBackground(UI.round(this, active ? UI.CHIP_ACTIVE : UI.INK_CHIP, 11));
            t.chipTitle.setTextColor(active ? (UI.master ? UI.WHITE : UI.TEXT) : UI.ON_INK_MUTED);
            t.chipTitle.setTypeface(active ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
            t.chipDot.setBackground(active ? UI.gradient(this, UI.INDIGO, UI.CYAN, 4) : UI.round(this, UI.DOT_OFF, 4));
            t.chipClose.setColorFilter(active ? UI.MUTED : UI.ON_INK_MUTED);
        }
    }

    // ================= Tabs =================
    Tab newTab(String url, boolean select) {
        final Tab t = new Tab();
        t.web = new WebView(this);
        configure(t);
        t.holder = new SwipeRefreshLayout(this);
        t.holder.addView(t.web, new ViewGroup.LayoutParams(-1, -1));
        t.holder.setColorSchemeColors(UI.INDIGO, UI.CYAN, UI.VIOLET);
        t.holder.setOnRefreshListener(() -> t.web.reload());
        // Chi cho keo-xuong-tai-lai khi cham bat dau o mep tren trang: vuot trong captcha / khung cuon
        // khong con kich hoat tai lai trang (nguyen nhan captcha bi "do" / mat)
        t.holder.setOnChildScrollUpCallback((p, c) -> !t.pullOk);
        t.holder.setVisibility(View.GONE);
        content.addView(t.holder, new FrameLayout.LayoutParams(-1, -1));
        buildChip(t);
        tabs.add(t);
        updateCounts();
        if (select) select(t);
        if (url != null) {
            if (select) t.web.loadUrl(url);
            else {   // the nen: chi tai khi bam vao the (nhanh hon, nhe proxy)
                t.pendingUrl = url; t.url = url;
                String h = Uri.parse(url).getHost();
                if (h != null) { t.title = h; t.chipTitle.setText(h); }
            }
        }
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
        updateNav();
        updateCounts();
        t.web.requestFocus();
        if (t.pendingUrl != null) { String u = t.pendingUrl; t.pendingUrl = null; t.web.loadUrl(u); }
    }

    void closeTab(Tab t) {
        int i = tabs.indexOf(t);
        if (i < 0) return;
        if (t.url != null && t.url.startsWith("http")) {
            closedUrls.add(t.url);
            if (closedUrls.size() > 10) closedUrls.remove(0);
        }
        tabs.remove(t);
        stripRow.removeView(t.chip);
        content.removeView(t.holder);
        t.web.stopLoading();
        t.holder.removeAllViews();
        t.web.destroy();
        updateCounts();
        if (tabs.isEmpty()) newTab(home, true);
        else if (cur == t) select(tabs.get(Math.min(i, tabs.size() - 1)));
    }

    void showHost(Tab t) {
        if (bar.hasFocus()) return;
        String u = t.url;
        if (u == null || u.isEmpty()) {
            bar.setText("");
            secIcon.setImageResource(R.drawable.ic_search);
            secIcon.setColorFilter(UI.ON_INK_MUTED);
            return;
        }
        String h = Uri.parse(u).getHost();
        if (h == null) h = u; else if (h.startsWith("www.")) h = h.substring(4);
        bar.setText(h);
        if (u.startsWith("https://")) {
            secIcon.setImageResource(R.drawable.ic_lock);
            secIcon.setColorFilter(UI.GREEN);
        } else if (u.startsWith("http://")) {
            secIcon.setImageResource(R.drawable.ic_alert);
            secIcon.setColorFilter(UI.AMBER);
        } else {
            secIcon.setImageResource(R.drawable.ic_search);
            secIcon.setColorFilter(UI.ON_INK_MUTED);
        }
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
        String low = u.toLowerCase(Locale.US);
        if (!(low.startsWith("http://") || low.startsWith("https://") || low.startsWith("about:"))) {
            toast("Chỉ hỗ trợ địa chỉ http / https");
            return;
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
        if (customUa.isEmpty()) return baseUa;
        return knownDevice ? Devices.fixUa(customUa, baseUa) : customUa;
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
            boolean mobile = !t.desktop && (ua.contains("Mobile") || ua.contains("Android") || ua.contains("iPhone"));
            int seed = uuid.hashCode() & 0x7fffffff;
            JSONObject f = t.desktop ? Devices.desktopFp() : (deviceFp != null ? deviceFp : autoFp(mobile, seed));
            JSONObject cfg = new JSONObject();
            cfg.put("ua", ua);
            cfg.put("spoofUa", !customUa.isEmpty() || t.desktop || deviceFp != null);
            cfg.put("platform", f.optString("platform", platformOf(ua)));
            cfg.put("lang", lang);
            cfg.put("cores", f.optInt("cores", 8));
            cfg.put("mem", f.optInt("mem", 8));
            cfg.put("touch", f.optInt("touch", mobile ? 5 : 0));
            cfg.put("sw", f.optInt("sw", mobile ? 390 : 1920));
            cfg.put("sh", f.optInt("sh", mobile ? 844 : 1080));
            cfg.put("dpr", f.optDouble("dpr", mobile ? 3 : 1));
            cfg.put("seed", seed);
            cfg.put("rtc", rtc);
            cfg.put("tz", tz);
            cfg.put("gpuV", f.optString("gpuV", ""));
            cfg.put("gpuR", f.optString("gpuR", ""));
            if (injectSrc == null) injectSrc = readAsset("inject.js");
            String out = injectSrc.replace("__CFG__", cfg.toString());
            if (lazy) {
                if (turboSrc == null) turboSrc = readAsset("turbo.js");
                out += "\n" + turboSrc;
            }
            return out;
        } catch (Exception e) { return ""; }
    }

    JSONObject autoFp(boolean mobile, int seed) {
        JSONObject o = new JSONObject();
        try {
            int[] dc = {4, 8, 12};
            o.put("cores", mobile ? 8 : dc[(seed >>> 3) % 3]);
            o.put("mem", mobile ? ((seed & 1) == 0 ? 4 : 8) : 8);
            o.put("touch", mobile ? 5 : 0);
            o.put("sw", mobile ? 390 : 1920);
            o.put("sh", mobile ? 844 : 1080);
            o.put("dpr", mobile ? 3 : 1);
        } catch (Exception ignored) {}
        return o;
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
        s.setLoadsImagesAutomatically(true);
        s.setBlockNetworkImage(blockImg);
        // Safe Browsing: them do tre moi trang va goi thang ve Google bang IP that (khong qua proxy)
        try { s.setSafeBrowsingEnabled(false); } catch (Throwable ignored) {}
        if (master) {
            s.setOffscreenPreRaster(true);
            try { w.setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_IMPORTANT, false); } catch (Throwable ignored) {}
        }
        // KHONG dung LAYER_TYPE_HARDWARE: ep WebView vao 1 texture lon -> trang trang, iframe/captcha do, ton RAM.
        // Cua so da bat hardwareAccelerated san; chi tat khi nguoi dung chu dong tat "Tang toc phan cung".
        if (!hwAccel) w.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        w.setOnTouchListener((v, ev) -> {
            if (ev.getActionMasked() == MotionEvent.ACTION_DOWN)
                t.pullOk = w.getScrollY() == 0 && ev.getY() < UI.dp(BrowserActivity.this, 56);
            return false;
        });
        w.setOnLongClickListener(v -> {
            WebView.HitTestResult hr = w.getHitTestResult();
            String ex = hr.getExtra();
            int ty = hr.getType();
            if (ex == null || !ex.startsWith("http")) return false;
            if (ty == WebView.HitTestResult.SRC_ANCHOR_TYPE || ty == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) { showLinkSheet(ex, false); return true; }
            if (ty == WebView.HitTestResult.IMAGE_TYPE) { showLinkSheet(ex, true); return true; }
            return false;
        });
        CookieManager.getInstance().setAcceptThirdPartyCookies(w, true);
        applyUa(t);
        installScript(t);

        w.setWebViewClient(new WebViewClient() {
            @Override public void onPageStarted(WebView v, String u, Bitmap f) {
                if (!docStartOk && t.script != null) v.evaluateJavascript(t.script, null);
                t.url = u; t.loading = true;
                if (t.imgForced) { t.imgForced = false; v.getSettings().setBlockNetworkImage(blockImg); }
                if (t == cur) {
                    showHost(t);
                    progress.setVisibility(View.VISIBLE);
                    reloadBtn.setImageResource(R.drawable.ic_close);
                    updateNav();
                }
            }
            @Override public void onPageFinished(WebView v, String u) {
                t.url = u; t.loading = false;
                t.holder.setRefreshing(false);
                if (darkOn) applyDark(v, true);
                if (t.title == null || t.title.isEmpty() || "Thẻ mới".equals(t.title)) {
                    String h = Uri.parse(u).getHost();
                    if (h != null) { t.title = h; t.chipTitle.setText(h); }
                }
                if (t == cur) {
                    showHost(t);
                    progress.setVisibility(View.INVISIBLE);
                    reloadBtn.setImageResource(R.drawable.ic_refresh);
                    updateNav();
                }
            }
            @Override public void onReceivedHttpAuthRequest(WebView v, HttpAuthHandler h, String host, String realm) {
                // Ham nay duoc goi CA khi proxy doi mat khau LAN khi chinh trang web doi dang nhap HTTP.
                // Truoc day luon gui user:pass cua proxy -> mot trang xau co the lay mat khau proxy. Nay: neu "host" la
                // mot may chu ma trang da tai (khong phai proxy) thi tu choi.
                String hn = host == null ? "" : host.toLowerCase(Locale.US);
                int colon = hn.lastIndexOf(':');
                if (colon > 0 && hn.indexOf(':') == colon) hn = hn.substring(0, colon);
                boolean isProxy = !proxyHost.isEmpty() && hn.equals(proxyHost);
                if (!isProxy && reqHosts.contains(hn)) { siteAuth(h, hn, realm); return; }   // trang web tu doi dang nhap -> hoi nguoi dung, KHONG gui mat khau proxy
                long now = SystemClock.uptimeMillis();
                if (now - authWin > 20000) { authWin = now; authCount = 0; }
                if (proxyUser != null && authCount++ < 60) h.proceed(proxyUser, proxyPass); else h.cancel();
            }
            // Turbo: chan quang cao, cho anh/CSS/font/media di thang WiFi (chi khi MASTER bat)
            @Override public WebResourceResponse shouldInterceptRequest(WebView v, WebResourceRequest r) {
                Uri u = r.getUrl();
                String host = u.getHost();
                if (host != null && reqHosts.size() < 4000) reqHosts.add(host.toLowerCase(Locale.US));
                // "Chan anh" lam captcha dang anh khong hien -> tu bat lai anh khi trang goi dich vu captcha
                if (blockImg && !t.imgForced && isCaptchaUrl(u)) {
                    t.imgForced = true;
                    v.post(() -> v.getSettings().setBlockNetworkImage(false));
                }
                if (!master || host == null) return null;
                String sc = u.getScheme();
                if (!"http".equals(sc) && !"https".equals(sc)) return null;
                if (isCaptchaUrl(u)) return null;   // captcha cham diem theo IP -> luon di cung proxy
                if (adblock && Turbo.isAd(host)) { nBlocked.incrementAndGet(); return Turbo.empty(u); }
                if (hasProxy && split > 0 && !r.isForMainFrame() && "https".equals(sc) && Turbo.isStatic(u, r.getRequestHeaders(), false)) {
                    if (split == 1) {   // can bang: chi tai nguyen cua ten mien KHAC trang dang xem moi di thang
                        String top = Uri.parse(t.url == null ? "" : t.url).getHost();
                        if (top != null && Turbo.site(host).equals(Turbo.site(top))) return null;
                    }
                    WebResourceResponse d = Turbo.direct(r);
                    if (d != null) nDirect.incrementAndGet();
                    return d;
                }
                return null;
            }
            // Renderer bi he thong giet (het RAM / loi GPU) -> truoc day trang dung hinh vinh vien hoac app crash
            @Override public boolean onRenderProcessGone(WebView v, RenderProcessGoneDetail d) {
                recoverTab(t, v);
                return true;
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
                String desc = TextUtils.htmlEncode(String.valueOf(e.getDescription()));
                String target = TextUtils.htmlEncode(r.getUrl().toString());
                String html = "<!doctype html><html><head><meta name='viewport' content='width=device-width,initial-scale=1'>"
                        + "<style>body{margin:0;font-family:-apple-system,'Segoe UI',Roboto,sans-serif;background:#EDF0F9;color:#141B38;"
                        + "display:flex;align-items:center;justify-content:center;min-height:100vh;padding:24px;box-sizing:border-box}"
                        + ".card{background:#fff;border-radius:24px;padding:36px 28px;max-width:340px;text-align:center;"
                        + "box-shadow:0 12px 40px rgba(20,27,56,.10)}h1{font-size:19px;margin:18px 0 8px}"
                        + "p{font-size:13px;color:#66708C;line-height:1.6;margin:6px 0}"
                        + ".btn{display:inline-block;margin-top:20px;padding:12px 28px;border-radius:22px;color:#fff;text-decoration:none;"
                        + "font-weight:600;font-size:14px;background:linear-gradient(135deg,#4F46E5,#3D5AFE)}"
                        + ".foot{margin-top:18px;font-size:11px;color:#98A2CB;letter-spacing:.06em}</style></head>"
                        + "<body><div class='card'><svg width='72' height='72' viewBox='0 0 72 72'>"
                        + "<defs><linearGradient id='g' x1='0' y1='0' x2='1' y2='1'>"
                        + "<stop offset='0' stop-color='#4F46E5'/><stop offset='1' stop-color='#00C2FF'/></linearGradient></defs>"
                        + "<circle cx='36' cy='36' r='34' fill='url(#g)' opacity='.14'/>"
                        + "<circle cx='36' cy='36' r='24' fill='url(#g)'/>"
                        + "<path d='M36 22l-9 4v6c0 5.6 3.8 10.8 9 12 5.2-1.2 9-6.4 9-12v-6l-9-4zm-1.8 18.2l-4-4 1.9-1.9 2.1 2.1 5.7-5.7 1.9 1.9-7.6 7.6z' fill='#fff'/>"
                        + "</svg><h1>Không thể mở trang</h1><p>" + desc + "</p>"
                        + "<p>Kiểm tra lại proxy hoặc kết nối mạng của hồ sơ này.</p>"
                        + "<a class='btn' href=\"" + target + "\">Thử lại</a>"
                        + "<div class='foot'>TUANANH BROWSER · ẨN DANH</div></div></body></html>";
                if (UI.master) html = html.replace("#EDF0F9", "#060A12").replace("background:#fff", "background:#0D1522;border:1px solid #1C3350")
                        .replace("color:#141B38", "color:#E6F4FF").replace("#4F46E5", "#0891B2").replace("#3D5AFE", "#0E7490");
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
            if (hasProxy) {   // tai qua dung proxy cua ho so (DownloadManager se lo IP that)
                toast("Đang tải qua proxy…");
                Turbo.download(this, url, ua, cd, mime, px, ui);
                return;
            }
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

    // Trang web doi dang nhap HTTP (khac mat khau proxy)
    void siteAuth(final HttpAuthHandler h, String host, String realm) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(UI.dp(this, 20), UI.dp(this, 8), UI.dp(this, 20), 0);
        final EditText u = new EditText(this);
        u.setHint("Tên đăng nhập"); u.setSingleLine(true); u.setTextColor(UI.TEXT); u.setHintTextColor(UI.HINT);
        final EditText pw = new EditText(this);
        pw.setHint("Mật khẩu"); pw.setSingleLine(true); pw.setTextColor(UI.TEXT); pw.setHintTextColor(UI.HINT);
        pw.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        box.addView(u); box.addView(pw);
        final boolean[] done = {false};
        UI.dialog(this).setTitle("Đăng nhập " + host)
                .setMessage(realm == null ? "" : realm)
                .setView(box)
                .setPositiveButton("Đăng nhập", (x, which) -> { done[0] = true; h.proceed(u.getText().toString(), pw.getText().toString()); })
                .setNegativeButton("Hủy", null)
                .setOnDismissListener(x -> { if (!done[0]) h.cancel(); })
                .show();
    }

    // Nhan giu vao lien ket / anh
    void showLinkSheet(final String url, final boolean image) {
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        TextView hd = UI.text(this, url, 12, UI.MUTED, false);
        hd.setSingleLine(true);
        hd.setEllipsize(TextUtils.TruncateAt.MIDDLE);
        hd.setPadding(UI.dp(this, 20), UI.dp(this, 4), UI.dp(this, 20), UI.dp(this, 10));
        body.addView(hd);
        body.addView(UI.divider(this));
        final Dialog d = UI.sheet(this, body);
        LinearLayout a = UI.menuRow(this, R.drawable.ic_open, UI.INDIGO, image ? "Mở ảnh trong thẻ mới" : "Mở trong thẻ mới", null);
        a.setOnClickListener(v -> { d.dismiss(); newTab(url, true); });
        LinearLayout b = UI.menuRow(this, R.drawable.ic_copy, UI.BLUE, image ? "Sao chép địa chỉ ảnh" : "Sao chép liên kết", null);
        b.setOnClickListener(v -> {
            d.dismiss();
            ((ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("url", url));
            toast("Đã sao chép");
        });
        body.addView(a); body.addView(b);
        if (image) {
            LinearLayout c = UI.menuRow(this, R.drawable.ic_storage, UI.TEAL, "Tải ảnh về máy", hasProxy ? "Tải qua proxy của hồ sơ" : null);
            c.setOnClickListener(v -> { d.dismiss(); saveUrl(url); });
            body.addView(c);
        }
    }

    void saveUrl(String url) {
        String ua = cur != null ? effectiveUa(cur) : baseUa;
        if (hasProxy) { toast("Đang tải qua proxy…"); Turbo.download(this, url, ua, null, null, px, ui); return; }
        try {
            DownloadManager.Request r = new DownloadManager.Request(Uri.parse(url));
            r.addRequestHeader("User-Agent", ua);
            String ck = CookieManager.getInstance().getCookie(url);
            if (ck != null) r.addRequestHeader("Cookie", ck);
            r.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            r.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, URLUtil.guessFileName(url, null, null));
            ((DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE)).enqueue(r);
            toast("Đang tải xuống…");
        } catch (Exception e) { toast("Lỗi tải: " + e.getMessage()); }
    }

    void hideCustom() {
        if (customView == null) return;
        ((FrameLayout) getWindow().getDecorView()).removeView(customView);
        customView = null;
        if (customCb != null) customCb.onCustomViewHidden();
        getWindow().getDecorView().setSystemUiVisibility(0);
    }

    static boolean isCaptchaUrl(Uri u) {
        String h = u.getHost();
        if (h == null) return false;
        if (h.endsWith("hcaptcha.com") || h.endsWith("challenges.cloudflare.com") || h.endsWith("recaptcha.net")
                || h.endsWith("arkoselabs.com") || h.endsWith("funcaptcha.com") || h.endsWith("geetest.com")
                || h.endsWith("captcha-delivery.com") || h.endsWith("perimeterx.net")) return true;
        String p = u.getPath();
        return p != null && p.contains("recaptcha") && (h.endsWith("google.com") || h.endsWith("gstatic.com"));
    }

    void recoverTab(Tab t, WebView dead) {
        if (t.web != dead) return;
        String u = t.url;
        t.crashes++;
        if (t.crashes > 3) { u = null; toast("Trang liên tục bị treo — đã dừng tự tải lại. Hãy đóng bớt thẻ."); }
        try { t.holder.removeView(dead); dead.destroy(); } catch (Exception ignored) {}
        Turbo.clearRam();
        t.handler = null; t.loading = false; t.pendingUrl = null;
        t.web = new WebView(this);
        configure(t);
        t.holder.addView(t.web, new ViewGroup.LayoutParams(-1, -1));
        t.holder.setRefreshing(false);
        if (t == cur) { progress.setVisibility(View.INVISIBLE); reloadBtn.setImageResource(R.drawable.ic_refresh); t.web.requestFocus(); updateNav(); }
        toast("Trang bị treo do thiếu RAM — đã tải lại thẻ");
        if (u != null && u.startsWith("http")) t.web.loadUrl(u);
    }

    // ================= Proxy =================
    void applyProxy(String raw, Runnable then) {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
            toast("WebView này không hỗ trợ proxy. Hãy cập nhật Android System WebView / Chrome.");
            stripProxy.setText("Không hỗ trợ proxy — đã chặn tải trang (tránh lộ IP)");
            return;
        }
        px = Turbo.parse(raw, ptype);
        proxyUser = px.user; proxyPass = px.pass;
        proxyHost = px.host.toLowerCase(Locale.US);
        String scheme = px.scheme, hostPort = px.host + ":" + px.port;
        try {
            ProxyConfig cfg = new ProxyConfig.Builder().addProxyRule(scheme + "://" + hostPort).build();
            ProxyController.getInstance().setProxyOverride(cfg, getMainExecutor(), then);
        } catch (Exception e) {
            toast("Proxy lỗi: " + e.getMessage());
            stripProxy.setText("Proxy lỗi — kiểm tra lại định dạng host:port");
        }
    }

    // ================= Menu / tab sheet =================
    void showMenu() {
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        LinearLayout head = new LinearLayout(this);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.setPadding(UI.dp(this, 20), UI.dp(this, 4), UI.dp(this, 20), UI.dp(this, 12));
        head.addView(UI.avatar(this, profileName, uuid.hashCode(), 40));
        LinearLayout hc = new LinearLayout(this);
        hc.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(0, -2, 1);
        hlp.leftMargin = UI.dp(this, 12);
        hc.addView(UI.text(this, profileName, 15, UI.TEXT, true));
        String subTxt = hasProxy ? proxyLabelStr : "Kết nối trực tiếp";
        if (!deviceName.isEmpty()) subTxt = deviceName + " · " + subTxt;
        TextView hp = UI.text(this, subTxt, 11, UI.MUTED, false);
        hp.setSingleLine(true);
        hp.setEllipsize(TextUtils.TruncateAt.END);
        hc.addView(hp);
        head.addView(hc, hlp);
        head.addView(hasProxy ? UI.chip(this, "socks5".equals(ptype) ? "SOCKS5" : "HTTP",
                UI.alpha("socks5".equals(ptype) ? UI.BLUE : UI.GREEN, 0x1C), "socks5".equals(ptype) ? UI.BLUE : UI.GREEN_TEXT)
                : UI.chip(this, "TRỰC TIẾP", UI.alpha(UI.AMBER, 0x24), UI.AMBER_TEXT));
        body.addView(head);
        body.addView(UI.divider(this));

        ScrollView msv = new ScrollView(this);
        msv.setVerticalScrollBarEnabled(false);
        LinearLayout rows = new LinearLayout(this);
        rows.setOrientation(LinearLayout.VERTICAL);
        msv.addView(rows);
        int maxH = (int) (getResources().getDisplayMetrics().heightPixels * 0.58f);
        body.addView(msv, new LinearLayout.LayoutParams(-1, Math.min(UI.dp(this, 460), maxH)));
        final Dialog d = UI.sheet(this, body);

        // MASTER: doi giao dien robot + bat Turbo (chia luong proxy, chan quang cao, anh luoi)
        LinearLayout rm = UI.menuRow(this, R.drawable.ic_robot, UI.CYAN, "Chế độ MASTER",
                master ? "Đang BẬT · bấm để về giao diện thường" : "Giao diện robot + Turbo tăng tốc");
        if (master) rm.addView(UI.icon(this, R.drawable.ic_check, 18, UI.GREEN));
        rm.setOnClickListener(v -> {
            d.dismiss();
            Master.set(this, !master);
            toast(master ? "Đã tắt MASTER" : "Đã bật MASTER");
            recreate();
        });
        LinearLayout r1 = UI.menuRow(this, R.drawable.ic_add, UI.INDIGO, "Thẻ mới", null);
        r1.setOnClickListener(v -> { d.dismiss(); newTab(home, true); });
        LinearLayout r2 = UI.menuRow(this, R.drawable.ic_home, UI.BLUE, "Trang chủ", null);
        r2.setOnClickListener(v -> { d.dismiss(); if (cur != null) cur.web.loadUrl(home); });
        LinearLayout rf = UI.menuRow(this, R.drawable.ic_search, UI.TEAL, "Tìm trong trang", null);
        rf.setOnClickListener(v -> { d.dismiss(); showFind(); });
        LinearLayout ri = UI.menuRow(this, R.drawable.ic_globe, UI.GREEN, "Kiểm tra IP đang dùng",
                "Mở trang tra IP trong thẻ mới · xem web thấy IP nào");
        ri.setOnClickListener(v -> { d.dismiss(); newTab("https://ipwho.is/", true); });
        LinearLayout r3 = UI.menuRow(this, R.drawable.ic_desktop, UI.VIOLET, "Trang web cho máy tính",
                cur != null && cur.desktop ? "Đang bật" : "Đang tắt");
        if (cur != null && cur.desktop) r3.addView(UI.icon(this, R.drawable.ic_check, 18, UI.GREEN));
        r3.setOnClickListener(v -> {
            d.dismiss();
            if (cur != null) { cur.desktop = !cur.desktop; applyUa(cur); installScript(cur); cur.web.reload(); }
        });
        LinearLayout rd = UI.menuRow(this, R.drawable.ic_memory, UI.VIOLET, "Nền tối cho trang web",
                darkOn ? "Đang bật · tạm tắt nếu captcha/hình bị lạ" : "Đảo màu trang (đang tắt)");
        if (darkOn) rd.addView(UI.icon(this, R.drawable.ic_check, 18, UI.GREEN));
        rd.setOnClickListener(v -> {
            d.dismiss();
            darkOn = !darkOn;
            for (Tab t : tabs) applyDark(t.web, darkOn);
        });
        LinearLayout r4 = UI.menuRow(this, R.drawable.ic_share, UI.TEAL, "Chia sẻ", null);
        r4.setOnClickListener(v -> {
            d.dismiss();
            if (cur != null) {
                Intent s = new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, cur.url);
                startActivity(Intent.createChooser(s, "Chia sẻ"));
            }
        });
        LinearLayout r5 = UI.menuRow(this, R.drawable.ic_copy, UI.BLUE, "Sao chép liên kết", null);
        r5.setOnClickListener(v -> {
            d.dismiss();
            if (cur != null) {
                ((ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("url", cur.url));
                toast("Đã sao chép");
            }
        });
        LinearLayout r6 = UI.menuRow(this, R.drawable.ic_cookie, UI.AMBER, "Xóa cookie của hồ sơ", "Xóa toàn bộ cookie và dữ liệu web");
        r6.setOnClickListener(v -> {
            d.dismiss();
            CookieManager.getInstance().removeAllCookies(null);
            WebStorage.getInstance().deleteAllData();
            Turbo.clearRam();
            toast("Đã xóa cookie và dữ liệu web");
        });
        LinearLayout r7 = UI.menuRow(this, R.drawable.ic_exit, UI.RED, "Thoát hồ sơ", "Đóng trình duyệt, giữ lại các thẻ");
        r7.setOnClickListener(v -> { d.dismiss(); finish(); });
        final ArrayList<View> extra = new ArrayList<>();
        if (!closedUrls.isEmpty()) {
            String lastClosed = closedUrls.get(closedUrls.size() - 1);
            String lh = Uri.parse(lastClosed).getHost();
            LinearLayout rc = UI.menuRow(this, R.drawable.ic_refresh, UI.AMBER, "Mở lại thẻ vừa đóng", lh == null ? lastClosed : lh);
            rc.setOnClickListener(v -> { d.dismiss(); if (!closedUrls.isEmpty()) newTab(closedUrls.remove(closedUrls.size() - 1), true); });
            extra.add(rc);
        }
        if (tabs.size() > 1) {
            LinearLayout ro = UI.menuRow(this, R.drawable.ic_close, UI.RED, "Đóng các thẻ khác", "Giữ lại thẻ đang xem · có thể mở lại");
            ro.setOnClickListener(v -> {
                d.dismiss();
                for (Tab x : new ArrayList<>(tabs)) if (x != cur) closeTab(x);
            });
            extra.add(ro);
        }
        LinearLayout rs = UI.menuRow(this, R.drawable.ic_speed, UI.TEAL, "Tiết kiệm dữ liệu",
                blockImg ? "Đang bật · không tải ảnh" : "Chặn ảnh để trang nhẹ và nhanh hơn");
        if (blockImg) rs.addView(UI.icon(this, R.drawable.ic_check, 18, UI.GREEN));
        rs.setOnClickListener(v -> {
            d.dismiss();
            blockImg = !blockImg;
            for (Tab x : tabs) x.web.getSettings().setBlockNetworkImage(blockImg);
            toast(blockImg ? "Đã chặn ảnh" : "Đã bật lại ảnh");
        });
        extra.add(rs);
        LinearLayout rk = UI.menuRow(this, R.drawable.ic_trash, UI.VIOLET, "Dọn bộ nhớ đệm", "Xóa cache trang + ảnh trong RAM, giữ cookie");
        rk.setOnClickListener(v -> {
            d.dismiss();
            Turbo.clearRam();
            if (cur != null) cur.web.clearCache(true);
            toast("Đã dọn bộ nhớ đệm");
        });
        extra.add(rk);
        rows.addView(rm); rows.addView(r1); rows.addView(r2); rows.addView(rf); rows.addView(ri); rows.addView(r3);
        rows.addView(rd); rows.addView(r4); rows.addView(r5); rows.addView(r6);
        for (View x : extra) rows.addView(x);
        rows.addView(r7);
    }

    // ================= Tim trong trang =================
    String lastFind = "";
    void showFind() {
        if (cur == null) return;
        final EditText e = new EditText(this);
        e.setSingleLine(true);
        e.setHint("Nhập từ cần tìm");
        e.setTextColor(UI.TEXT);
        e.setHintTextColor(UI.HINT);
        final android.app.AlertDialog dlg = UI.dialog(this).setTitle("Tìm trong trang").setView(e)
                .setPositiveButton("Tiếp", null).setNegativeButton("Đóng", null).create();
        dlg.setOnDismissListener(x -> { if (cur != null) cur.web.clearMatches(); lastFind = ""; });
        dlg.show();
        cur.web.setFindListener((idx, n, done) -> dlg.setTitle(n == 0 ? "Không thấy kết quả" : "Kết quả " + (idx + 1) + "/" + n));
        dlg.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String q = e.getText().toString();
            if (cur == null || q.isEmpty()) return;
            if (q.equals(lastFind)) cur.web.findNext(true); else { lastFind = q; cur.web.findAllAsync(q); }
        });
    }

    // ================= Nen toi cho web (dao mau CSS, anh/video giu nguyen) =================
    void applyDark(WebView w, boolean on) {
        String js = on
                ? "(function(){var d=document;if(d.getElementById('__pbdark'))return;var s=d.createElement('style');s.id='__pbdark';"
                + "s.textContent='html{filter:invert(1) hue-rotate(180deg)!important;background:#fff}img,video,canvas,picture,svg image,iframe{filter:invert(1) hue-rotate(180deg)!important}';"
                + "(d.head||d.documentElement).appendChild(s);})()"
                : "(function(){var s=document.getElementById('__pbdark');if(s)s.remove();})()";
        try { w.evaluateJavascript(js, null); } catch (Exception ignored) {}
    }

    // ================= Turbo: cache dia + bo dem thong ke =================
    void installTurboCache() {
        if (!master || split == 0) return;
        try {
            java.io.File dir = new java.io.File(getCacheDir(), "turbo_" + uuid);
            // nhieu ket noi song song hon -> nhieu anh tai cung luc (mac dinh he thong chi 5/may chu)
            System.setProperty("http.maxConnections", "32");
            System.setProperty("http.keepAlive", "true");
            android.net.http.HttpResponseCache.install(dir, 256L * 1024 * 1024);
        } catch (Exception ignored) {}
    }
    void startTurboStats() {
        if (stripTurbo == null) return;
        ui.post(new Runnable() {
            @Override public void run() {
                if (isFinishing() || isDestroyed()) return;
                String m = split == 2 ? "MAX" : split == 1 ? "CB" : "OFF";
                if (split == 0) stripTurbo.setText("\uD83D\uDD12PROXY · ⛔" + nBlocked.get());
                else stripTurbo.setText("⚡" + m + " " + nDirect.get() + " · ⛔" + nBlocked.get());
                ui.postDelayed(this, 1500);
            }
        });
    }

    void showTabSheet() {
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        LinearLayout head = new LinearLayout(this);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.setPadding(UI.dp(this, 20), UI.dp(this, 6), UI.dp(this, 20), UI.dp(this, 10));
        head.addView(UI.text(this, "Thẻ đang mở", 16, UI.TEXT, true));
        LinearLayout.LayoutParams cpl = new LinearLayout.LayoutParams(-2, -2);
        cpl.leftMargin = UI.dp(this, 8);
        head.addView(UI.chip(this, String.valueOf(tabs.size()), UI.alpha(UI.INDIGO, 0x14), UI.INDIGO), cpl);
        body.addView(head);

        ScrollView sv = new ScrollView(this);
        sv.setVerticalScrollBarEnabled(false);
        LinearLayout rows = new LinearLayout(this);
        rows.setOrientation(LinearLayout.VERTICAL);
        sv.addView(rows);
        int h = Math.min(tabs.size() * UI.dp(this, 62), UI.dp(this, 310));
        body.addView(sv, new LinearLayout.LayoutParams(-1, h));

        final Dialog d = UI.sheet(this, body);
        for (Tab t : tabs) {
            LinearLayout r = new LinearLayout(this);
            r.setGravity(Gravity.CENTER_VERTICAL);
            r.setPadding(UI.dp(this, 20), UI.dp(this, 10), UI.dp(this, 12), UI.dp(this, 10));
            r.setBackgroundColor(t == cur ? UI.alpha(UI.INDIGO, 0x08) : 0x00000000);
            r.setClickable(true);
            View dot = new View(this);
            dot.setBackground(t == cur ? UI.gradient(this, UI.INDIGO, UI.CYAN, 4) : UI.round(this, UI.DOT_OFF_SHEET, 4));
            r.addView(dot, new LinearLayout.LayoutParams(UI.dp(this, 8), UI.dp(this, 8)));
            LinearLayout col = new LinearLayout(this);
            col.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(0, -2, 1);
            clp.leftMargin = UI.dp(this, 12);
            TextView tt = UI.text(this, t.title, 13, UI.TEXT, true);
            tt.setSingleLine(true);
            tt.setEllipsize(TextUtils.TruncateAt.END);
            col.addView(tt);
            String host = Uri.parse(t.url == null ? "" : t.url).getHost();
            TextView hh = UI.text(this, host == null ? "Thẻ mới" : host, 11, UI.MUTED, false);
            hh.setSingleLine(true);
            hh.setEllipsize(TextUtils.TruncateAt.END);
            col.addView(hh);
            r.addView(col, clp);
            ImageView x = UI.iconBtn(this, R.drawable.ic_close, UI.MUTED, 0, 32, 16);
            x.setOnClickListener(v -> { d.dismiss(); closeTab(t); });
            r.addView(x);
            r.setOnClickListener(v -> { d.dismiss(); select(t); });
            rows.addView(r);
        }
        TextView add = UI.gradButton(this, "+  Thẻ mới");
        LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(-1, UI.dp(this, 46));
        alp.setMargins(UI.dp(this, 20), UI.dp(this, 10), UI.dp(this, 20), UI.dp(this, 4));
        add.setOnClickListener(v -> { d.dismiss(); newTab(home, true); });
        body.addView(add, alp);
    }

    // ================= Khoi phuc / vong doi =================
    void restoreTabs() {
        JSONArray saved;
        int sel = 0;
        try {
            JSONObject o = new JSONObject(getSharedPreferences("tabs", 0).getString(uuid, "{}"));
            saved = o.optJSONArray("urls");
            sel = o.optInt("sel", 0);
        } catch (Exception e) { saved = null; }
        if (saved == null || saved.length() == 0) { newTab(openUrl.isEmpty() ? home : openUrl, true); return; }
        for (int i = 0; i < saved.length(); i++) newTab(saved.optString(i), false);
        select(tabs.get(Math.max(0, Math.min(sel, tabs.size() - 1))));
        if (!openUrl.isEmpty()) { newTab(openUrl, true); openUrl = ""; }
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
        stopHint();
        saveTabs();
        CookieManager.getInstance().flush();
        try { android.net.http.HttpResponseCache c = android.net.http.HttpResponseCache.getInstalled(); if (c != null) c.flush(); } catch (Exception ignored) {}
        for (Tab t : tabs) t.web.onPause();
    }

    @Override public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (level >= TRIM_MEMORY_RUNNING_LOW) Turbo.clearRam();
    }

    @Override protected void onResume() {
        super.onResume();
        SysInfo.applyRefresh(this, highFps);
        startHint();
        for (Tab t : tabs) t.web.onResume();
    }

    // Goi y uu tien hieu nang CPU (API 31+) — bao cao nhip khung that, khong ep xung
    void startHint() {
        if (!perfHintOn || !SysInfo.hintSupported() || hintSession != null) return;
        float hz = highFps ? SysInfo.maxRefreshRate(this) : SysInfo.refreshRate(this);
        long target = (long) (1000000000L / Math.max(30f, hz));
        hintSession = SysInfo.hintCreate(this, target);
        if (hintSession == null) return;
        lastFrame = 0;
        hintCb = new android.view.Choreographer.FrameCallback() {
            @Override public void doFrame(long frameTimeNanos) {
                if (hintSession == null) return;
                if (lastFrame != 0) SysInfo.hintReport(hintSession, frameTimeNanos - lastFrame);
                lastFrame = frameTimeNanos;
                android.view.Choreographer.getInstance().postFrameCallback(this);
            }
        };
        android.view.Choreographer.getInstance().postFrameCallback(hintCb);
    }
    void stopHint() {
        if (hintCb != null) { android.view.Choreographer.getInstance().removeFrameCallback(hintCb); hintCb = null; }
        SysInfo.hintClose(hintSession);
        hintSession = null; lastFrame = 0;
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
        ui.removeCallbacksAndMessages(null);
        // Giai phong WebView (truoc day khong destroy -> ro ri RAM khi tao lai man hinh)
        for (Tab t : tabs) { try { t.holder.removeAllViews(); t.web.destroy(); } catch (Exception ignored) {} }
        // Giet process de lan mo sau dung proxy/UUID moi hoan toan
        if (isFinishing()) Process.killProcess(Process.myPid());
    }
}