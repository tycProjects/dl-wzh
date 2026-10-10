package com.leonteam.sourceweb;

import android.app.*;
import android.content.*;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.net.Uri;
import android.os.*;
import android.text.InputType;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.*;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import java.io.*;
import java.net.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.*;
import java.util.zip.*;

public class MainActivity extends Activity {

    // ------------------------------------------------------------------ constants
    private static final int MP = ViewGroup.LayoutParams.MATCH_PARENT;
    private static final int WC = ViewGroup.LayoutParams.WRAP_CONTENT;
    private static final String[] TABS = {"HTML", "CSS", "JS", "MEDIA"};
    private static final String[] BG_NAMES = {"Tối", "AMOLED", "Nửa đêm", "Sáng"};
    private static final String[] FONT_NAMES = {"Không chân", "Có chân", "Đơn cách", "Có chân đơn cách", "Thu hẹp", "Mảnh", "Vừa", "Phóng khoáng", "Viết tay"};
    private static final String[] FONT_FAMILIES = {"sans-serif", "serif", "monospace", "serif-monospace",
            "sans-serif-condensed", "sans-serif-light", "sans-serif-medium", "casual", "cursive"};
    private static final int DEFAULT_ACCENT = Color.rgb(174, 92, 255);
    private static final int[] ACCENTS = {
            Color.rgb(174, 92, 255), Color.rgb(0, 224, 255), Color.rgb(57, 255, 136), Color.rgb(255, 64, 160),
            Color.rgb(255, 159, 28), Color.rgb(255, 82, 82), Color.rgb(77, 141, 255), Color.rgb(255, 214, 10)};
    private static final String HINT = "Mã nguồn tải về sẽ hiển thị tại đây.\n\nChỉ kiểm tra website mà bạn được phép truy cập. "
            + "Nội dung được JavaScript tạo động có thể không xuất hiện trong HTML thô.";
    private static final int SPAN_FLAGS = Spannable.SPAN_EXCLUSIVE_EXCLUSIVE;

    private static final Pattern P_TAG = Pattern.compile("<[/!]?[A-Za-z][^<>]*>");
    private static final Pattern P_COMMENT = Pattern.compile("<!--.*?-->", Pattern.DOTALL);
    private static final Pattern P_RAW = Pattern.compile("<(script|style)\\b[^>]*>(.*?)</\\1\\s*>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    private static final Pattern P_STR = Pattern.compile("\"[^\"]*\"|'[^']*'");
    private static final Pattern P_ATTR = Pattern.compile("\\s([A-Za-z_:@][\\w:.@-]*)(?==)");
    private static final Pattern P_ANYTAG = Pattern.compile("<(/?)([A-Za-z][\\w:-]*)([^<>]*?)(/?)>");
    private static final HashSet<String> VOID_TAGS = new HashSet<>(Arrays.asList(
            "area", "base", "br", "col", "embed", "hr", "img", "input", "link", "meta", "param", "source", "track", "wbr", "!doctype"));

    // ------------------------------------------------------------------ settings / giao diện
    private SharedPreferences prefs;
    private int accent = DEFAULT_ACCENT, bgMode = 0, uiFont = 0, codeFont = 2, codeSize = 12;
    private boolean highlightOn = true;
    private int BG, PANEL, TEXT, MUTED, STROKE, ACC_T, ON_ACCENT;
    private Typeface uiTf, uiTfBold, codeTf;
    private int inL, inT, inR, inB;

    // ------------------------------------------------------------------ views
    private EditText urlInput, searchInput;
    private TextView status, sourceView, matchLabel, prevCode;
    private ScrollView sourceScroll, sheetScroll;
    private LinearLayout root;
    private ProgressBar progress;
    private View statusDot;
    private final TextView[] tabViews = new TextView[4];
    private Dialog settingsDialog;

    // ------------------------------------------------------------------ page state
    private String pageUrl = "", html = "", currentCode = "", activeTab = "HTML", statusMsg = "Sẵn sàng. Nhập địa chỉ website công khai để kiểm tra.";
    private String pageKind = "", contentType = "", serverHdr = "", poweredHdr = "";
    private int statusCode = 0;
    private long sizeBytes = 0, fetchMs = 0;
    private boolean busy = false;
    private Analysis analysis;
    private final ArrayList<Resource> resources = new ArrayList<>();
    private SpannableStringBuilder baseSpan = new SpannableStringBuilder("");
    private final ArrayList<Integer> matches = new ArrayList<>();
    private int matchIdx = -1;
    private String lastQuery = "";

    static class Resource {
        String type, url;
        Resource(String t, String u) { type = t; url = u; }
    }

    static class Analysis {
        String title = "", lang = "", ký tựet = "", viewport = "", generator = "", description = "", robots = "", ogTitle = "", canonical = "", favicon = "";
        int extCss, inlCss, extJs, inlJs, inlJsBytes, imgs, media, iframes, forms, links, elements, textLen, htmlLen;
        ArrayList<String> frameworks = new ArrayList<>(), cms = new ArrayList<>(), libs = new ArrayList<>(), services = new ArrayList<>();
        int level; // 0 static, 1 hybrid, 2 JS-rendered
        String badge = "", vTitle = "", vDesc = "";
        boolean pureStatic, needsJs;
    }

    /** Simple wrapping layout for chips and colour swatches. */
    static class Flow extends ViewGroup {
        private final int gap;
        Flow(Context c, int gap) { super(c); this.gap = gap; }
        private void measureChild2(View c, int maxW) {
            ViewGroup.LayoutParams p = c.getLayoutParams();
            int ws = (p != null && p.width > 0) ? MeasureSpec.makeMeasureSpec(p.width, MeasureSpec.EXACTLY) : MeasureSpec.makeMeasureSpec(maxW, MeasureSpec.AT_MOST);
            int hs = (p != null && p.height > 0) ? MeasureSpec.makeMeasureSpec(p.height, MeasureSpec.EXACTLY) : MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
            c.measure(ws, hs);
        }
        @Override protected void onMeasure(int wSpec, int hSpec) {
            int maxW = MeasureSpec.getSize(wSpec);
            int x = 0, y = 0, rowH = 0;
            for (int i = 0; i < getChildCount(); i++) {
                View c = getChildAt(i);
                if (c.getVisibility() == GONE) continue;
                measureChild2(c, maxW);
                int w = c.getMeasuredWidth(), h = c.getMeasuredHeight();
                if (x > 0 && x + w > maxW) { x = 0; y += rowH + gap; rowH = 0; }
                x += w + gap; rowH = Math.max(rowH, h);
            }
            setMeasuredDimension(maxW, y + rowH);
        }
        @Override protected void onLayout(boolean changed, int l, int t, int r, int b) {
            int maxW = r - l, x = 0, y = 0, rowH = 0;
            for (int i = 0; i < getChildCount(); i++) {
                View c = getChildAt(i);
                if (c.getVisibility() == GONE) continue;
                int w = c.getMeasuredWidth(), h = c.getMeasuredHeight();
                if (x > 0 && x + w > maxW) { x = 0; y += rowH + gap; rowH = 0; }
                c.layout(x, y, x + w, y + h);
                x += w + gap; rowH = Math.max(rowH, h);
            }
        }
    }

    // ================================================================== lifecycle
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("leon_prefs", MODE_PRIVATE);
        accent = prefs.getInt("accent", DEFAULT_ACCENT);
        bgMode = prefs.getInt("bg", 0);
        uiFont = prefs.getInt("uiFont", 0);
        codeFont = prefs.getInt("codeFont", 2);
        codeSize = prefs.getInt("codeSize", 12);
        highlightOn = prefs.getBoolean("hl", true);
        computeTheme();
        makeTypefaces();
        buildUi();
        String last = prefs.getString("lastUrl", "");
        if (!last.isEmpty()) urlInput.setText(last);
        showTab("HTML");
    }

    private void savePrefs() {
        prefs.edit().putInt("accent", accent).putInt("bg", bgMode).putInt("uiFont", uiFont)
                .putInt("codeFont", codeFont).putInt("codeSize", codeSize).putBoolean("hl", highlightOn).apply();
    }

    // ================================================================== giao diện helpers
    private static int withAlpha(int c, int a) { return Color.argb(a, Color.red(c), Color.green(c), Color.blue(c)); }
    private static int blend(int a, int b, float t) {
        return Color.rgb((int) (Color.red(a) + (Color.red(b) - Color.red(a)) * t),
                (int) (Color.green(a) + (Color.green(b) - Color.green(a)) * t),
                (int) (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * t));
    }
    private static double lum(int c) { return 0.299 * Color.red(c) + 0.587 * Color.green(c) + 0.114 * Color.blue(c); }

    private void computeTheme() {
        switch (bgMode) {
            case 1: BG = Color.BLACK; PANEL = Color.rgb(16, 16, 18); TEXT = Color.rgb(240, 240, 245); MUTED = Color.rgb(150, 150, 165); break;
            case 2: BG = Color.rgb(7, 13, 26); PANEL = Color.rgb(15, 25, 46); TEXT = Color.rgb(228, 238, 255); MUTED = Color.rgb(140, 158, 190); break;
            case 3: BG = Color.rgb(244, 242, 250); PANEL = Color.WHITE; TEXT = Color.rgb(28, 25, 42); MUTED = Color.rgb(106, 100, 128); break;
            default: BG = Color.rgb(12, 10, 20); PANEL = Color.rgb(24, 19, 37); TEXT = Color.rgb(241, 235, 255); MUTED = Color.rgb(169, 157, 190);
        }
        PANEL = blend(PANEL, accent, bgMode == 3 ? 0.04f : 0.07f);
        STROKE = withAlpha(accent, bgMode == 3 ? 110 : 90);
        ACC_T = bgMode == 3 ? blend(accent, Color.BLACK, 0.30f) : accent;
        ON_ACCENT = lum(accent) > 150 ? Color.rgb(18, 14, 28) : Color.WHITE;
    }

    private void makeTypefaces() {
        uiTf = Typeface.create(FONT_FAMILIES[uiFont], Typeface.NORMAL);
        uiTfBold = Typeface.create(FONT_FAMILIES[uiFont], Typeface.BOLD);
        codeTf = Typeface.create(FONT_FAMILIES[codeFont], Typeface.NORMAL);
    }

    private void applySystemBars() {
        Window w = getWindow();
        w.setBackgroundDrawable(new ColorDrawable(BG));
        w.setStatusBarColor(BG);
        w.setNavigationBarColor(BG);
        View dv = w.getDecorView();
        int f = dv.getSystemUiVisibility();
        int light = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        if (Build.VERSION.SDK_INT >= 26) light |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        if (bgMode == 3) f |= light; else f &= ~light;
        dv.setSystemUiVisibility(f);
    }

    /** Re-apply giao diện/fonts everywhere, keeping page state. */
    private void applyTheme() {
        savePrefs();
        computeTheme();
        makeTypefaces();
        final int sy = sourceScroll != null ? sourceScroll.getScrollY() : 0;
        String url = urlInput != null ? urlInput.getText().toString() : "";
        String q = searchInput != null ? searchInput.getText().toString() : "";
        buildUi();
        urlInput.setText(url);
        searchInput.setText(q);
        if (pageUrl.isEmpty()) {
            showTab(activeTab);
        } else {
            rebuildBase();
            applyView();
            final ScrollView ns = sourceScroll;
            ns.post(() -> ns.scrollTo(0, sy));
        }
        renderCài đặt();
    }

    // ================================================================== small view helpers
    private int dp(float v) { return (int) (v * getResources().getDisplayMetrics().density + .5f); }
    private LinearLayout.LayoutParams lp(int w, int h) { return new LinearLayout.LayoutParams(w, h); }
    private LinearLayout.LayoutParams lp(int w, int h, float wt) { return new LinearLayout.LayoutParams(w, h, wt); }
    private LinearLayout.LayoutParams lpm(int w, int h, float wt, int l, int t, int r, int b) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, h, wt);
        p.setMargins(l, t, r, b);
        return p;
    }
    private GradientDrawable rect(int fill, int stroke, float rDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        d.setCornerRadius(dp(rDp));
        if (stroke != 0) d.setStroke(dp(1), stroke);
        return d;
    }
    private void ripple(View v, float rDp) {
        GradientDrawable mask = new GradientDrawable();
        mask.setColor(Color.WHITE);
        mask.setCornerRadius(dp(rDp));
        v.setForeground(new RippleDrawable(ColorStateList.valueOf(withAlpha(accent, 70)), null, mask));
    }
    private TextView label(String s, float size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s); t.setTextSize(size); t.setTextColor(color);
        t.setTypeface(bold ? uiTfBold : uiTf);
        return t;
    }
    private TextView chip(String text, boolean filled) {
        TextView t = new TextView(this);
        t.setText(text); t.setGravity(Gravity.CENTER); t.setTextSize(12.5f);
        t.setTypeface(uiTfBold); t.setSingleLine(true);
        t.setTextColor(filled ? ON_ACCENT : TEXT);
        t.setBackground(filled ? rect(accent, 0, 14) : rect(PANEL, STROKE, 14));
        t.setPadding(dp(12), dp(8), dp(12), dp(8));
        t.setClickable(true);
        ripple(t, 14);
        return t;
    }
    private TextView tag(String text) {
        TextView t = label(text, 12, ACC_T, true);
        t.setBackground(rect(withAlpha(accent, 40), withAlpha(accent, 120), 12));
        t.setPadding(dp(10), dp(5), dp(10), dp(5));
        return t;
    }
    private TextView roundBtn(String glyph) {
        TextView t = new TextView(this);
        t.setText(glyph); t.setGravity(Gravity.CENTER); t.setTextSize(17); t.setTextColor(ACC_T); t.setTypeface(uiTfBold);
        GradientDrawable o = new GradientDrawable();
        o.setShape(GradientDrawable.OVAL); o.setColor(PANEL); o.setStroke(dp(1), STROKE);
        t.setBackground(o);
        t.setClickable(true);
        ripple(t, 30);
        return t;
    }

    // ================================================================== main UI
    private void buildUi() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        root.setPadding(dp(16) + inL, dp(10) + inT, dp(16) + inR, dp(8) + inB);
        root.setOnApplyWindowInsetsListener((v, ins) -> {
            int l = ins.getSystemWindowInsetLeft(), t = ins.getSystemWindowInsetTop();
            int r = ins.getSystemWindowInsetRight(), b = ins.getSystemWindowInsetBottom();
            if (l != inL || t != inT || r != inR || b != inB) {
                inL = l; inT = t; inR = r; inB = b;
                v.setPadding(dp(16) + l, dp(10) + t, dp(16) + r, dp(8) + b);
            }
            return ins;
        });
        setContentView(root);
        applySystemBars();
        root.requestApplyInsets();

        // ---- header
        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        LinearLayout logoRow = new LinearLayout(this);
        logoRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView logo = label("LEON", 24, ACC_T, true);
        logo.setLetterSpacing(0.08f);
        logoRow.addView(logo);
        logoRow.addView(label("-SOURCE WEB", 18, TEXT, true));
        titles.addView(logoRow);
        titles.addView(label("Công cụ xem mã nguồn web  •  " + BG_NAMES[bgMode] + " giao diện", 11, MUTED, false));
        header.addView(titles, lp(0, WC, 1));
        TextView infoBtn = roundBtn("i");
        TextView setBtn = roundBtn("\u2699");
        header.addView(infoBtn, lpm(dp(42), dp(42), 0, 0, 0, dp(8), 0));
        header.addView(setBtn, lp(dp(42), dp(42)));
        root.addView(header, lpm(MP, WC, 0, 0, 0, 0, dp(12)));
        infoBtn.setOnClickListener(v -> showInfo());
        setBtn.setOnClickListener(v -> openCài đặt());

        // ---- URL card
        LinearLayout urlCard = new LinearLayout(this);
        urlCard.setGravity(Gravity.CENTER_VERTICAL);
        urlCard.setBackground(rect(PANEL, STROKE, 16));
        urlCard.setPadding(dp(14), dp(4), dp(6), dp(4));
        urlInput = new EditText(this);
        urlInput.setBackground(null);
        urlInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        urlInput.setImeOptions(EditorInfo.IME_ACTION_GO);
        urlInput.setTextColor(TEXT);
        urlInput.setHintTextColor(MUTED);
        urlInput.setHint("https://example.com");
        urlInput.setTextSize(14);
        urlInput.setTypeface(codeTf);
        urlInput.setHighlightColor(withAlpha(accent, 90));
        urlInput.setOnEditorActionListener((v, actionId, ev) -> { fetchPage(); return true; });
        urlCard.addView(urlInput, lp(0, dp(46), 1));
        TextView go = chip("TẢI", true);
        go.setTextSize(13);
        urlCard.addView(go, lp(dp(84), dp(40)));
        root.addView(urlCard, lp(MP, WC));
        go.setOnClickListener(v -> fetchPage());

        // ---- progress + status
        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setIndeterminate(true);
        progress.setIndeterminateTintList(ColorStateList.valueOf(accent));
        progress.setVisibility(busy ? View.VISIBLE : View.INVISIBLE);
        root.addView(progress, lpm(MP, dp(3), 0, dp(4), dp(6), dp(4), 0));

        LinearLayout statusRow = new LinearLayout(this);
        statusRow.setGravity(Gravity.CENTER_VERTICAL);
        statusDot = new View(this);
        GradientDrawable dot = new GradientDrawable();
        dot.setShape(GradientDrawable.OVAL); dot.setColor(accent);
        statusDot.setBackground(dot);
        statusRow.addView(statusDot, lpm(dp(8), dp(8), 0, dp(2), 0, dp(8), 0));
        status = label(statusMsg, 12, MUTED, false);
        status.setMaxLines(2);
        status.setEllipsize(TextUtils.TruncateAt.END);
        statusRow.addView(status, lp(0, WC, 1));
        root.addView(statusRow, lpm(MP, WC, 0, 0, dp(4), 0, dp(10)));

        // ---- tabs
        LinearLayout tabs = new LinearLayout(this);
        tabs.setPadding(dp(4), dp(4), dp(4), dp(4));
        tabs.setBackground(rect(PANEL, STROKE, 16));
        for (int i = 0; i < TABS.length; i++) {
            final String name = TABS[i];
            TextView tv = new TextView(this);
            tv.setGravity(Gravity.CENTER);
            tv.setTextSize(12.5f);
            tv.setTypeface(uiTfBold);
            tv.setSingleLine(true);
            tv.setClickable(true);
            ripple(tv, 12);
            tv.setOnClickListener(v -> showTab(name));
            tabViews[i] = tv;
            tabs.addView(tv, lpm(0, dp(38), 1, dp(2), 0, dp(2), 0));
        }
        root.addView(tabs, lp(MP, WC));
        updateTabs();

        // ---- code area
        FrameLayout area = new FrameLayout(this);
        area.setBackground(rect(PANEL, STROKE, 16));
        root.addView(area, lpm(MP, 0, 1, 0, dp(10), 0, dp(10)));
        sourceScroll = new ScrollView(this);
        sourceScroll.setVerticalScrollBarEnabled(true);
        sourceView = new TextView(this);
        sourceView.setTextColor(TEXT);
        sourceView.setTypeface(codeTf);
        sourceView.setTextSize(codeSize);
        sourceView.setTextIsSelectable(true);
        sourceView.setHighlightColor(withAlpha(accent, 90));
        sourceView.setPadding(dp(12), dp(12), dp(12), dp(12));
        sourceScroll.addView(sourceView);
        area.addView(sourceScroll, new FrameLayout.LayoutParams(MP, MP));

        // ---- search row
        LinearLayout searchRow = new LinearLayout(this);
        searchRow.setGravity(Gravity.CENTER_VERTICAL);
        searchRow.setBackground(rect(PANEL, STROKE, 16));
        searchRow.setPadding(dp(14), dp(2), dp(6), dp(2));
        searchInput = new EditText(this);
        searchInput.setBackground(null);
        searchInput.setInputType(InputType.TYPE_CLASS_TEXT);
        searchInput.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        searchInput.setTextColor(TEXT);
        searchInput.setHintTextColor(MUTED);
        searchInput.setHint("Tìm trong mã nguồn hiện tại…");
        searchInput.setTextSize(13);
        searchInput.setTypeface(uiTf);
        searchInput.setHighlightColor(withAlpha(accent, 90));
        searchInput.setOnEditorActionListener((v, actionId, ev) -> { searchSource(true); return true; });
        searchRow.addView(searchInput, lp(0, dp(42), 1));
        matchLabel = label("", 11.5f, MUTED, false);
        matchLabel.setGravity(Gravity.CENTER);
        searchRow.addView(matchLabel, lpm(WC, WC, 0, dp(4), 0, dp(6), 0));
        TextView prev = chip("\u2039", false);
        TextView next = chip("\u203A", true);
        prev.setTextSize(18); next.setTextSize(18);
        prev.setPadding(0, 0, 0, dp(2)); next.setPadding(0, 0, 0, dp(2));
        searchRow.addView(prev, lpm(dp(38), dp(34), 0, 0, 0, dp(4), 0));
        searchRow.addView(next, lp(dp(38), dp(34)));
        root.addView(searchRow, lp(MP, WC));
        prev.setOnClickListener(v -> searchSource(false));
        next.setOnClickListener(v -> searchSource(true));

        // ---- action row
        LinearLayout actions = new LinearLayout(this);
        TextView format = chip("Định dạng", false), minify = chip("Thu gọn", false), copy = chip("Sao chép", false), zip = chip("Xuất ZIP", true);
        TextView[] acts = {format, minify, copy, zip};
        for (TextView b : acts) actions.addView(b, lpm(0, dp(42), 1, dp(3), 0, dp(3), 0));
        root.addView(actions, lpm(MP, WC, 0, 0, dp(8), 0, 0));
        format.setOnClickListener(v -> transform(true));
        minify.setOnClickListener(v -> transform(false));
        copy.setOnClickListener(v -> {
            if (pageUrl.isEmpty() || currentCode.isEmpty()) { toast("Hãy tải trang trước"); return; }
            ((ClipboardManager) getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("source", currentCode));
            toast("Đã sao chép " + currentCode.length() + " ký tự");
        });
        zip.setOnClickListener(v -> exportZip());
    }

    private void setStatus(String s) {
        statusMsg = s;
        if (status != null) status.setText(s);
    }
    private void setBusy(boolean b) {
        busy = b;
        if (progress != null) progress.setVisibility(b ? View.VISIBLE : View.INVISIBLE);
    }
    private int countOf(String type) {
        int n = 0;
        for (Resource r : resources) if (r.type.equals(type)) n++;
        return n;
    }
    private void updateTabs() {
        for (int i = 0; i < TABS.length; i++) {
            TextView t = tabViews[i];
            if (t == null) continue;
            int n = i > 0 ? countOf(TABS[i]) : 0;
            boolean sel = TABS[i].equals(activeTab);
            t.setText(n > 0 ? TABS[i] + " " + n : TABS[i]);
            t.setTextColor(sel ? ON_ACCENT : MUTED);
            t.setBackground(sel ? rect(accent, 0, 12) : null);
        }
    }

    // ================================================================== fetching
    private HttpURLConnection openConnection(String url) throws IOException {
        String cur = url;
        for (int i = 0; i < 6; i++) {
            HttpURLConnection c = (HttpURLConnection) new URL(cur).openConnection();
            c.setConnectTimeout(15000);
            c.setReadTimeout(20000);
            c.setInstanceFollowRedirects(false);
            c.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14) Leon-Source-web/2.0");
            c.setRequestProperty("Accept", "text/html,application/xhtml+xml,*/*;q=0.8");
            int code = c.getResponseCode();
            if (code == 301 || code == 302 || code == 303 || code == 307 || code == 308) {
                String loc = c.getHeaderField("Location");
                c.disconnect();
                if (loc == null) throw new IOException("Chuyển hướng nhưng thiếu tiêu đề Location");
                cur = new URL(new URL(cur), loc).toString();
                continue;
            }
            return c;
        }
        throw new IOException("Quá nhiều lần chuyển hướng");
    }

    private void fetchPage() {
        String input = urlInput.getText().toString().trim();
        if (input.isEmpty()) { toast("Vui lòng nhập địa chỉ URL trước"); return; }
        if (!input.matches("(?i)^https?://.*")) input = "https://" + input;
        final String target = input;
        try { new URL(target); } catch (Exception e) { toast("Địa chỉ HTTP/HTTPS không hợp lệ"); return; }
        setBusy(true);
        setStatus("Đang tải trang…");
        hideKeyboard();
        new Thread(() -> {
            try {
                long t0 = System.currentTimeMillis();
                HttpURLConnection c = openConnection(target);
                int code = c.getResponseCode();
                InputStream in = (code >= 400) ? c.getErrorStream() : c.getInputStream();
                byte[] data = readLimited(in, 5 * 1024 * 1024);
                final String ctype = nz(c.getContentType());
                final String server = nz(c.getHeaderField("Server"));
                final String powered = nz(c.getHeaderField("X-Powered-By"));
                final boolean cf = c.getHeaderField("CF-RAY") != null;
                final String finalUrl = c.getURL().toString();
                c.disconnect();
                final long ms = System.currentTimeMillis() - t0;
                final String kind = detectKind(data, ctype, finalUrl);
                final String body = (kind.equals("HTML") || kind.equals("Text") || kind.equals("JSON"))
                        ? new String(data, ký tựetFor(ctype, data)) : "";
                final Analysis an = kind.equals("HTML") ? analyze(body, server, powered, cf) : null;
                final int fcode = code;
                final int size = data.length;
                runOnUiThread(() -> {
                    pageUrl = finalUrl;
                    urlInput.setText(finalUrl);
                    html = body; pageKind = kind; analysis = an; statusCode = fcode;
                    contentType = ctype; serverHdr = server; poweredHdr = powered; sizeBytes = size; fetchMs = ms;
                    resources.clear();
                    if (kind.equals("HTML")) parseResources(body, finalUrl);
                    prefs.edit().putString("lastUrl", finalUrl).apply();
                    activeTab = "HTML";
                    setBusy(false);
                    setStatus("HTTP " + fcode + "  \u2022  " + fmtBytes(size) + "  \u2022  "
                            + (kind.equals("HTML") ? resources.size() + " tài nguyên liên kết" : kind) + "  \u2022  " + ms + " ms");
                    showTab("HTML");
                });
            } catch (Exception e) {
                final String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                runOnUiThread(() -> { setBusy(false); setStatus("Tải thất bại: " + msg); });
            }
        }).start();
    }

    private byte[] readLimited(InputStream in, int max) throws IOException {
        if (in == null) return new byte[0];
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] b = new byte[8192];
        int n, total = 0;
        while ((n = in.read(b)) > 0) {
            total += n;
            if (total > max) throw new IOException("Trang vượt quá giới hạn 5 MB");
            out.write(b, 0, n);
        }
        in.close();
        return out.toByteArray();
    }

    private String detectKind(byte[] d, String ctype, String url) {
        String ct = ctype.toLowerCase(Locale.ROOT);
        String u = url.toLowerCase(Locale.ROOT);
        int q = u.indexOf('?');
        if (q > 0) u = u.substring(0, q);
        if (d.length >= 4 && d[0] == 'P' && d[1] == 'K' && (d[2] == 3 || d[2] == 5) && (d[3] == 4 || d[3] == 6)) return "Tệp ZIP";
        if (ct.contains("zip") || u.endsWith(".zip")) return "Tệp ZIP";
        if (d.length >= 2 && (d[0] & 0xff) == 0x1f && (d[1] & 0xff) == 0x8b) return "GTệp ZIP";
        if (d.length >= 4 && d[0] == 0x25 && d[1] == 'P' && d[2] == 'D' && d[3] == 'F') return "Tài liệu PDF";
        if (ct.contains("html")) return "HTML";
        if (ct.contains("json")) return "JSON";
        if (ct.startsWith("text/") || ct.contains("xml") || ct.contains("javascript")) return "Text";
        if (ct.startsWith("image/")) return "Image";
        if (ct.startsWith("video/") || ct.startsWith("audio/")) return "Tệp đa phương tiện";
        if (ct.isEmpty()) {
            int n = Math.min(d.length, 512);
            String head = new String(d, 0, n, StandardCharsets.ISO_8859_1).toLowerCase(Locale.ROOT);
            if (head.contains("<html") || head.contains("<!doctype")) return "HTML";
            for (int i = 0; i < n; i++) if (d[i] == 0) return "Tệp nhị phân";
            return "Text";
        }
        return "Tệp nhị phân";
    }

    private Charset ký tựetFor(String ctype, byte[] d) {
        String cs = grab("charset\\s*=\\s*[\"']?([\\w-]+)", ctype);
        if (cs.isEmpty()) {
            int n = Math.min(d.length, 4096);
            String head = new String(d, 0, n, StandardCharsets.ISO_8859_1);
            cs = grab("<meta[^>]+charset\\s*=\\s*[\"']?([\\w-]+)", head);
        }
        try { if (!cs.isEmpty()) return Charset.forName(cs); } catch (Exception ignored) { }
        return StandardCharsets.UTF_8;
    }

    private void parseResources(String src, String base) {
        Pattern p = Pattern.compile("(?is)<(script|link|img|source|video|audio|iframe|embed)\\b([^>]+)>?");
        Matcher m = p.matcher(src);
        while (m.find()) {
            String tag = m.group(1).toLowerCase(Locale.ROOT), attrs = m.group(2);
            String raw = null, type = "";
            Matcher a = Pattern.compile("(?is)(src|href)\\s*=\\s*['\\\"]([^'\\\"]+)['\\\"]").matcher(attrs);
            if (a.find()) raw = a.group(2);
            if (raw == null) continue;
            if (tag.equals("link")) {
                if (attrs.toLowerCase(Locale.ROOT).contains("stylesheet")) type = "CSS";
                else continue;
            } else if (tag.equals("script")) type = "JS";
            else if (tag.equals("img") || tag.equals("source") || tag.equals("video") || tag.equals("audio") || tag.equals("embed")) type = "MEDIA";
            else continue;
            try {
                String abs = new URL(new URL(base), raw).toString();
                if (abs.startsWith("http://") || abs.startsWith("https://")) resources.add(new Resource(type, abs));
            } catch (Exception ignored) { }
        }
        LinkedHashMap<String, Resource> unique = new LinkedHashMap<>();
        for (Resource r : resources) unique.put(r.type + "|" + r.url, r);
        resources.clear();
        resources.addAll(unique.values());
    }

    // ================================================================== tabs + rendering
    private String nonHtmlMessage() {
        return "URL này trả về: " + pageKind + "\nLoại nội dung: " + (contentType.isEmpty() ? "unknown" : contentType)
                + "\nDung lượng: " + fmtBytes(sizeBytes) + "\n\nĐây không phải trang HTML nên không có mã đánh dấu để hiển thị."
                + (pageKind.equals("Tệp ZIP") ? "\n\nZIP là tệp nén chứa nhiều tệp. Hãy tải xuống rồi dùng trình quản lý tệp để giải nén." : "")
                + "\n\nMở bảng thông tin (i) để xem chi tiết.";
    }

    private void showTab(String tab) {
        activeTab = tab;
        updateTabs();
        StringBuilder b = new StringBuilder();
        if (pageUrl.isEmpty()) {
            b.append(HINT);
        } else if (tab.equals("HTML")) {
            if (!pageKind.equals("HTML") && html.isEmpty()) b.append(nonHtmlMessage()); else b.append(html);
        } else {
            int i = 0;
            for (Resource r : resources) if (r.type.equals(tab)) b.append(String.format(Locale.US, "%02d  %s\n\n", ++i, r.url));
            if (i == 0) b.append("Không tìm thấy tệp ").append(tab).append(" được liên kết trong HTML tĩnh.");
            else if (!tab.equals("MEDIA")) b.append("Lưu ý: địa chỉ tài nguyên liên kết được liệt kê tại đây. Chọn Xuất ZIP để tải chúng xuống.");
        }
        currentCode = b.toString();
        resetSearch();
        rebuildBase();
        applyView();
        sourceScroll.scrollTo(0, 0);
    }

    private int hlGen = 0;

    /** Shows plain text instantly, then colours it on a background thread. */
    private void rebuildBase() {
        baseSpan = new SpannableStringBuilder(currentCode);
        final int gen = ++hlGen;
        if (highlightOn && !pageUrl.isEmpty() && activeTab.equals("HTML") && pageKind.equals("HTML") && currentCode.length() <= 250000) {
            final String code = currentCode;
            new Thread(() -> {
                final SpannableStringBuilder sb = new SpannableStringBuilder(code);
                try { highlightHtml(sb, code); } catch (Throwable ignored) { return; }
                runOnUiThread(() -> {
                    if (gen == hlGen) { baseSpan = sb; applyView(); }
                });
            }).start();
        }
    }

    private void highlightHtml(SpannableStringBuilder sb, String s) {
        int cStr = bgMode == 3 ? Color.rgb(16, 122, 66) : Color.rgb(126, 226, 160);
        int cAttr = bgMode == 3 ? Color.rgb(170, 92, 0) : Color.rgb(255, 188, 112);
        ArrayList<int[]> raw = new ArrayList<>();
        Matcher rm = P_RAW.matcher(s);
        while (rm.find()) raw.add(new int[]{rm.start(2), rm.end(2)});
        int k = 0;
        Matcher tm = P_TAG.matcher(s);
        while (tm.find()) {
            int a = tm.start(), z = tm.end();
            while (k < raw.size() && raw.get(k)[1] <= a) k++;
            if (k < raw.size() && a >= raw.get(k)[0]) continue;
            sb.setSpan(new ForegroundColorSpan(ACC_T), a, z, SPAN_FLAGS);
            String tg = s.substring(a, z);
            Matcher am = P_ATTR.matcher(tg);
            while (am.find()) sb.setSpan(new ForegroundColorSpan(cAttr), a + am.start(1), a + am.end(1), SPAN_FLAGS);
            Matcher sm = P_STR.matcher(tg);
            while (sm.find()) sb.setSpan(new ForegroundColorSpan(cStr), a + sm.start(), a + sm.end(), SPAN_FLAGS);
        }
        Matcher cm = P_COMMENT.matcher(s);
        while (cm.find()) {
            sb.setSpan(new ForegroundColorSpan(MUTED), cm.start(), cm.end(), SPAN_FLAGS);
            sb.setSpan(new StyleSpan(Typeface.ITALIC), cm.start(), cm.end(), SPAN_FLAGS);
        }
    }

    private void applyView() {
        CharSequence show = baseSpan;
        if (!matches.isEmpty() && matchIdx >= 0 && !lastQuery.isEmpty()) {
            SpannableStringBuilder sb = new SpannableStringBuilder(baseSpan);
            int ql = lastQuery.length();
            int shown = Math.min(matches.size(), 800);
            int soft = withAlpha(accent, 70);
            for (int i = 0; i < shown; i++) {
                int p = matches.get(i);
                sb.setSpan(new BackgroundColorSpan(soft), p, p + ql, SPAN_FLAGS);
            }
            int cur = matches.get(matchIdx);
            sb.setSpan(new BackgroundColorSpan(accent), cur, cur + ql, SPAN_FLAGS);
            sb.setSpan(new ForegroundColorSpan(ON_ACCENT), cur, cur + ql, SPAN_FLAGS);
            show = sb;
        }
        sourceView.setText(show);
    }

    // ================================================================== search
    private void resetSearch() {
        matches.clear();
        lastQuery = "";
        matchIdx = -1;
        if (matchLabel != null) matchLabel.setText("");
    }

    private void searchSource(boolean forward) {
        String q = searchInput.getText().toString();
        if (q.isEmpty()) { toast("Nhập từ khóa tìm kiếm"); return; }
        hideKeyboard();
        boolean fresh = false;
        if (!q.equals(lastQuery) || matches.isEmpty()) {
            matches.clear();
            Matcher m = Pattern.compile(Pattern.quote(q), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE).matcher(currentCode);
            while (m.find() && matches.size() < 5000) matches.add(m.start());
            lastQuery = q;
            matchIdx = forward ? -1 : 0;
            fresh = true;
        }
        int n = matches.size();
        if (n == 0) {
            matchLabel.setText("0/0");
            setStatus("Không tìm thấy kết quả cho: " + q);
            applyView();
            return;
        }
        matchIdx = (((matchIdx + (forward ? 1 : -1)) % n) + n) % n;
        applyView();
        int pos = matches.get(matchIdx);
        int line = 1;
        for (int i = 0; i < pos; i++) if (currentCode.charAt(i) == '\n') line++;
        matchLabel.setText((matchIdx + 1) + "/" + n + (n >= 5000 ? "+" : ""));
        setStatus("Kết quả " + (matchIdx + 1) + " trên " + n + " cho “" + q + "\u201D  \u2022  line " + line);
        final int fpos = pos;
        sourceScroll.post(() -> {
            Layout l = sourceView.getLayout();
            if (l != null) {
                int ln = l.getLineForOffset(Math.min(fpos, Math.max(0, sourceView.length() - 1)));
                sourceScroll.smoothScrollTo(0, Math.max(0, l.getLineTop(ln) - dp(80)));
            }
        });
    }

    // ================================================================== format / minify
    private void transform(boolean pretty) {
        if (pageUrl.isEmpty() || currentCode.isEmpty()) { toast("Hãy tải trang trước"); return; }
        if (!activeTab.equals("HTML") || !pageKind.equals("HTML")) { toast("Định dạng / Thu gọn chỉ hoạt động trong thẻ HTML"); return; }
        String s = currentCode;
        if (pretty) s = prettyHtml(s);
        else s = s.replaceAll(">\\s+<", "><").replaceAll("\\s{2,}", " ");
        currentCode = s;
        resetSearch();
        rebuildBase();
        applyView();
        setStatus(pretty ? "Đã định dạng thụt lề (cơ bản)." : "Đã thu gọn khoảng trắng (cơ bản).  " + s.length() + " ký tự");
    }

    private String prettyHtml(String src) {
        String[] lines = src.replaceAll("(?i)>\\s*<", ">\n<").split("\n");
        StringBuilder out = new StringBuilder();
        int depth = 0;
        boolean inRaw = false;
        for (String line0 : lines) {
            String line = line0.trim();
            if (line.isEmpty()) continue;
            String low = line.toLowerCase(Locale.ROOT);
            if (inRaw) {
                if (low.contains("</script") || low.contains("</style")) inRaw = false;
                else { out.append(line0).append('\n'); continue; }
            }
            int net = 0;
            Matcher m = P_ANYTAG.matcher(line);
            while (m.find()) {
                boolean close = !m.group(1).isEmpty();
                String name = m.group(2).toLowerCase(Locale.ROOT);
                if (VOID_TAGS.contains(name) || m.group(4).equals("/")) continue;
                net += close ? -1 : 1;
            }
            int ind = Math.max(0, Math.min(depth, depth + net));
            for (int i = 0; i < Math.min(ind, 40); i++) out.append("  ");
            out.append(line).append('\n');
            depth = Math.max(0, depth + net);
            if ((low.startsWith("<script") || low.startsWith("<style")) && !low.contains("</script") && !low.contains("</style")) inRaw = true;
        }
        return out.toString();
    }

    // ================================================================== ZIP export
    private void exportZip() {
        if (pageUrl.isEmpty() || html.isEmpty()) { toast("Hãy tải một trang HTML trước"); return; }
        setBusy(true);
        setStatus("Đang chuẩn bị ZIP…");
        new Thread(() -> {
            File out = new File(getCacheDir(), "leon-source-" + System.currentTimeMillis() + ".zip");
            int downloaded = 0;
            try (ZipOutputStream zip = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(out)))) {
                addEntry(zip, "index.html", html.getBytes(StandardCharsets.UTF_8));
                HashSet<String> used = new HashSet<>();
                int count = 0;
                for (Resource r : resources) {
                    if (count >= 30) break;
                    String path;
                    try {
                        URL u = new URL(r.url);
                        String name = u.getPath();
                        if (name == null || name.isEmpty() || name.endsWith("/")) name = "/resource-" + count;
                        name = name.substring(name.lastIndexOf('/') + 1);
                        name = name.replaceAll("[^A-Za-z0-9._-]", "_");
                        if (name.isEmpty()) name = "resource-" + count;
                        path = r.type.toLowerCase(Locale.ROOT) + "/" + name;
                        int n = 1;
                        String candidate = path;
                        while (used.contains(candidate)) candidate = r.type.toLowerCase(Locale.ROOT) + "/" + (n++) + "_" + name;
                        path = candidate;
                        used.add(path);
                        HttpURLConnection c = (HttpURLConnection) u.openConnection();
                        c.setConnectTimeout(7000);
                        c.setReadTimeout(10000);
                        c.setRequestProperty("User-Agent", "Leon-Source-web/2.0");
                        if (c.getResponseCode() >= 400) { c.disconnect(); continue; }
                        InputStream in = c.getInputStream();
                        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                        byte[] buf = new byte[4096];
                        int nread, total = 0;
                        while ((nread = in.read(buf)) > 0) {
                            total += nread;
                            if (total > 2 * 1024 * 1024) break;
                            bytes.write(buf, 0, nread);
                        }
                        in.close();
                        c.disconnect();
                        addEntry(zip, path, bytes.toByteArray());
                        downloaded++;
                        count++;
                    } catch (Exception ignored) { }
                }
                String readme = "Leon-Source web export\nSource URL: " + pageUrl + "\nSố tài nguyên liên kết đã tải: " + downloaded
                        + "\nLưu ý: đây là bản chụp tĩnh; nội dung động và đường dẫn tương đối có thể cần chỉnh sửa thủ công.\n";
                addEntry(zip, "README.txt", readme.getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
                final String msg = e.getMessage();
                runOnUiThread(() -> { setBusy(false); setStatus("Xuất ZIP thất bại: " + msg); });
                return;
            }
            final int done = downloaded;
            runOnUiThread(() -> {
                setBusy(false);
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("application/zip");
                Uri zipUri = Uri.parse("content://" + getPackageName() + ".fileprovider/" + out.getName());
                intent.putExtra(Intent.EXTRA_STREAM, zipUri);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                try {
                    startActivity(Intent.createChooser(intent, "Lưu hoặc chia sẻ ZIP"));
                    setStatus("Đã tạo ZIP  •  " + done + " tệp liên kết đã tải (tối đa 30)");
                } catch (Exception e) {
                    setStatus("Đã tạo ZIP trong bộ nhớ đệm ứng dụng nhưng không có ứng dụng chia sẻ phù hợp.");
                }
            });
        }).start();
    }

    private void addEntry(ZipOutputStream zip, String name, byte[] data) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(data);
        zip.closeEntry();
    }

    // ================================================================== website analysis
    private static String nz(String s) { return s == null ? "" : s; }

    private static String grab(String regex, String s) {
        Matcher m = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(s);
        return m.find() ? m.group(1).trim() : "";
    }
    private static int count(String regex, String s) {
        Matcher m = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(s);
        int n = 0;
        while (m.find()) n++;
        return n;
    }
    private static boolean has(String lo, String... keys) {
        for (String k : keys) if (lo.contains(k)) return true;
        return false;
    }
    private static String metaContent(String h, String name) {
        String q = Pattern.quote(name);
        String v = grab("<meta[^>]+(?:name|property)\\s*=\\s*[\"']" + q + "[\"'][^>]*content\\s*=\\s*[\"']([^\"']*)[\"']", h);
        if (v.isEmpty()) v = grab("<meta[^>]+content\\s*=\\s*[\"']([^\"']*)[\"'][^>]*(?:name|property)\\s*=\\s*[\"']" + q + "[\"']", h);
        return v;
    }
    private static String decodeEnt(String s) {
        return s.replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"")
                .replace("&#39;", "'").replace("&nbsp;", " ").replaceAll("\\s+", " ").trim();
    }
    private static String fmtBytes(long n) {
        if (n < 1024) return n + " B";
        if (n < 1024 * 1024) return String.format(Locale.US, "%.1f KB", n / 1024.0);
        return String.format(Locale.US, "%.2f MB", n / 1048576.0);
    }

    private Analysis analyze(String h, String server, String powered, boolean cf) {
        Analysis a = new Analysis();
        String lo = h.toLowerCase(Locale.ROOT);
        a.htmlLen = h.length();
        a.title = decodeEnt(grab("<title[^>]*>(.*?)</title>", h));
        a.lang = grab("<html[^>]*?\\slang\\s*=\\s*[\"']([^\"']+)[\"']", h);
        a.charset = grab("<meta[^>]+charset\\s*=\\s*[\"']?([\\w-]+)", h);
        a.viewport = metaContent(h, "viewport");
        a.generator = metaContent(h, "generator");
        a.description = decodeEnt(metaContent(h, "description"));
        a.robots = metaContent(h, "robots");
        a.ogTitle = decodeEnt(metaContent(h, "og:title"));
        a.canonical = grab("<link[^>]+rel\\s*=\\s*[\"']canonical[\"'][^>]*href\\s*=\\s*[\"']([^\"']+)[\"']", h);
        a.favicon = grab("<link[^>]+rel\\s*=\\s*[\"'](?:shortcut )?icon[\"'][^>]*href\\s*=\\s*[\"']([^\"']+)[\"']", h);

        a.extCss = count("<link\\b[^>]*rel\\s*=\\s*[\"']?stylesheet", h);
        a.inlCss = count("<style\\b", h);
        a.extJs = count("<script\\b[^>]*\\bsrc\\s*=", h);
        a.inlJs = Math.max(0, count("<script\\b", h) - a.extJs);
        Matcher im = Pattern.compile("<script\\b(?![^>]*\\bsrc\\s*=)[^>]*>(.*?)</script>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(h);
        while (im.find()) a.inlJsBytes += im.group(1).length();
        a.imgs = count("<img\\b", h);
        a.media = count("<(?:video|audio)\\b", h);
        a.iframes = count("<iframe\\b", h);
        a.forms = count("<form\\b", h);
        a.links = count("<a\\s[^>]*href", h);
        a.elements = count("<[a-z][a-z0-9-]*", h);
        String text = h.replaceAll("(?is)<(script|style|noscript|template|svg)\\b.*?</\\1>", " ")
                .replaceAll("(?s)<!--.*?-->", " ").replaceAll("(?s)<[^>]*>", " ")
                .replace("&nbsp;", " ").replaceAll("\\s+", " ").trim();
        a.textLen = text.length();

        // frameworks
        if (has(lo, "__next_data__", "/_next/", "id=\"__next\"")) a.frameworks.add("Next.js (React)");
        else if (has(lo, "data-reactroot", "react-dom", "react.production", "react.development")) a.frameworks.add("React");
        if (has(lo, "__nuxt", "/_nuxt/")) a.frameworks.add("Nuxt (Vue)");
        else if (has(lo, "data-v-", "vue.js", "vue.min.js", "vue.runtime", "/vue@")) a.frameworks.add("Vue");
        if (has(lo, "ng-version", "<app-root", "ng-app", "angular.js", "angular.min.js", "/angular@")) a.frameworks.add("Angular");
        if (has(lo, "__sveltekit", "/_app/immutable", "svelte-")) a.frameworks.add("Svelte / SvelteKit");
        if (has(lo, "___gatsby", "gatsby-focus-wrapper")) a.frameworks.add("Gatsby (React)");
        if (has(lo, "astro-island", "data-astro-", "/_astro/")) a.frameworks.add("Astro");
        if (has(lo, "__remixcontext")) a.frameworks.add("Remix (React)");
        // CMS / công cụ tạo web
        if (has(lo, "wp-content", "wp-includes", "wp-json")) a.cms.add("WordPress");
        if (has(lo, "cdn.shopify.com", "shopify.theme")) a.cms.add("Shopify");
        if (has(lo, "wixstatic.com", "static.parastorage.com")) a.cms.add("Wix");
        if (has(lo, "squarespace.com", "static1.squarespace")) a.cms.add("Squarespace");
        if (has(lo, "data-wf-page", "assets.website-files.com")) a.cms.add("Webflow");
        if (has(lo, "blogger.com", "blogspot.com", "<b:skin")) a.cms.add("Blogger");
        if (has(lo, "/media/jui/", "joomla")) a.cms.add("Joomla");
        if (has(lo, "drupal-settings-json", "/sites/default/files")) a.cms.add("Drupal");
        if (has(lo, "framerusercontent.com", "data-framer")) a.cms.add("Framer");
        String gen = a.generator.toLowerCase(Locale.ROOT);
        boolean ssg = has(gen, "hugo", "jekyll", "hexo", "docusaurus", "eleventy", "pelican", "mkdocs", "sphinx", "vitepress");
        // libraries
        if (has(lo, "jquery")) a.libs.add("jQuery");
        if (has(lo, "bootstrap")) a.libs.add("Bootstrap");
        if (has(lo, "tailwind")) a.libs.add("Tailwind CSS");
        if (has(lo, "alpinejs", "x-data=")) a.libs.add("Alpine.js");
        if (has(lo, "htmx")) a.libs.add("htmx");
        if (has(lo, "gsap")) a.libs.add("GSAP");
        if (has(lo, "swiper")) a.libs.add("Swiper");
        if (has(lo, "font-awesome", "fontawesome")) a.libs.add("Font Awesome");
        if (has(lo, "fonts.googleapis.com")) a.libs.add("Google Fonts");
        // services
        if (has(lo, "google-analytics.com", "googletagmanager.com", "gtag(")) a.services.add("Google Analytics / Tag Manager");
        if (has(lo, "connect.facebook.net", "fbq(")) a.services.add("Meta Pixel");
        if (has(lo, "adsbygoogle")) a.services.add("Google AdSense");
        if (has(lo, "recaptcha")) a.services.add("reCAPTCHA");
        if (has(lo, "js.stripe.com")) a.services.add("Stripe");
        if (has(lo, "youtube.com/embed", "youtube-nocookie.com")) a.services.add("YouTube embed");
        if (cf || server.toLowerCase(Locale.ROOT).contains("cloudflare")) a.services.add("Cloudflare");
        if (!server.isEmpty() && !server.toLowerCase(Locale.ROOT).contains("cloudflare")) a.services.add("Server: " + server);
        if (!powered.isEmpty()) a.services.add("Cung cấp bởi: " + powered);

        // verdict
        int scripts = a.extJs + a.inlJs;
        a.pureStatic = scripts == 0 && a.frameworks.isEmpty() && a.cms.isEmpty();
        String fw = TextUtils.join(", ", a.frameworks), cm = TextUtils.join(", ", a.cms);
        if (!a.frameworks.isEmpty() && a.textLen < 400) {
            a.level = 2; a.badge = "JS-RENDERED";
            a.vTitle = "Ứng dụng JavaScript (SPA)";
            a.vDesc = "Xây dựng bằng " + fw + ". HTML thô gần như chỉ là khung rỗng — nội dung thật được tạo trong trình duyệt nên sẽ không xuất hiện ở đây.";
        } else if (!a.frameworks.isEmpty()) {
            a.level = 1; a.badge = "HYBRID";
            a.vTitle = "Website dùng framework (" + fw + ")";
            a.vDesc = "HTML được máy chủ kết xuất hoặc dựng sẵn cùng JavaScript. Phần lớn nội dung có trong mã nguồn thô, nhưng đây không phải HTML thuần.";
        } else if (!a.cms.isEmpty()) {
            a.level = 1; a.badge = "CMS / CÔNG CỤ TẠO WEB";
            a.vTitle = "Tạo bằng " + cm;
            a.vDesc = "Được tạo bởi nền tảng quản lý nội dung. HTML chủ yếu được máy chủ kết xuất, kèm tập lệnh và kiểu giao diện.";
        } else if (scripts == 0) {
            a.level = 0; a.badge = "HTML THUẦN";
            a.vTitle = "HTML tĩnh thuần" + (ssg ? " (công cụ tạo website tĩnh)" : "");
            a.vDesc = "No JavaScript at all \u2014 just HTML" + ((a.extCss + a.inlCss) > 0 ? " và CSS." : ".");
        } else if (a.textLen < 200) {
            a.level = 2; a.badge = "JS-RENDERED";
            a.vTitle = "Có thể được kết xuất bằng JavaScript";
            a.vDesc = "HTML thô có rất ít văn bản hiển thị nhưng có tập lệnh, nên nội dung có thể được tạo khi chạy.";
        } else if (a.libs.isEmpty() && scripts <= 4) {
            a.level = 0; a.badge = "STATIC";
            a.vTitle = "HTML tĩnh với ít tập lệnh" + (ssg ? " (công cụ tạo website tĩnh)" : "");
            a.vDesc = "Trang HTML/CSS thông thường, chỉ có vài tập lệnh nhỏ và không phát hiện framework.";
        } else {
            a.level = 1; a.badge = "HYBRID";
            a.vTitle = "HTML + thư viện JavaScript";
            a.vDesc = "Chủ yếu là HTML thông thường được bổ sung bằng tập lệnh và thư viện.";
        }
        a.needsJs = a.level == 2;
        return a;
    }

    // ================================================================== bottom sheets
    private LinearLayout newBody() {
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(16), dp(6), dp(16), dp(28) + inB);
        return body;
    }

    private View sheetView(String title, LinearLayout body, final Dialog d) {
        LinearLayout sheet = new LinearLayout(this);
        sheet.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable g = new GradientDrawable();
        g.setColor(BG);
        float r = dp(24);
        g.setCornerRadii(new float[]{r, r, r, r, 0, 0, 0, 0});
        g.setStroke(dp(1), STROKE);
        sheet.setBackground(g);
        View handle = new View(this);
        handle.setBackground(rect(withAlpha(MUTED, 120), 0, 3));
        LinearLayout.LayoutParams hp = lp(dp(40), dp(4));
        hp.gravity = Gravity.CENTER_HORIZONTAL;
        hp.topMargin = dp(10);
        sheet.addView(handle, hp);
        LinearLayout head = new LinearLayout(this);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.setPadding(dp(20), dp(10), dp(14), dp(6));
        head.addView(label(title, 19, TEXT, true), lp(0, WC, 1));
        TextView close = chip("Close", false);
        close.setOnClickListener(v -> d.dismiss());
        head.addView(close, lp(WC, dp(36)));
        sheet.addView(head);
        ScrollView sv = new ScrollView(this);
        sv.setVerticalScrollBarEnabled(false);
        sv.addView(body);
        sheetScroll = sv;
        sheet.addView(sv, lp(MP, 0, 1));
        return sheet;
    }

    private Dialog openSheet(String title, LinearLayout body) {
        Dialog d = new Dialog(this);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        d.setContentView(sheetView(title, body, d));
        Window w = d.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            w.setGravity(Gravity.BOTTOM);
            w.setLayout(MP, (int) (getResources().getDisplayMetrics().heightPixels * 0.88f));
            w.setWindowAnimations(android.R.style.Animation_InputMethod);
        }
        return d;
    }

    private LinearLayout card(LinearLayout parent, String title) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(14), dp(12), dp(14), dp(12));
        c.setBackground(rect(PANEL, STROKE, 16));
        if (title != null) {
            TextView t = label(title, 11, ACC_T, true);
            t.setLetterSpacing(0.12f);
            t.setPadding(0, 0, 0, dp(8));
            c.addView(t);
        }
        parent.addView(c, lpm(MP, WC, 0, 0, 0, 0, dp(10)));
        return c;
    }

    private void kv(LinearLayout p, String k, String v, StringBuilder rep) {
        String val = (v == null || v.isEmpty()) ? "\u2014" : v;
        if (rep != null) rep.append(k).append(": ").append(val).append('\n');
        LinearLayout row = new LinearLayout(this);
        row.setPadding(0, dp(5), 0, dp(5));
        TextView a = label(k, 12.5f, MUTED, false);
        TextView b = label(val, 13, TEXT, false);
        b.setTextIsSelectable(true);
        row.addView(a, lp(0, WC, 0.38f));
        row.addView(b, lp(0, WC, 0.62f));
        p.addView(row);
    }

    private void tagGroup(LinearLayout parent, String title, List<String> items, StringBuilder rep) {
        TextView t = label(title, 12.5f, MUTED, false);
        t.setPadding(0, dp(6), 0, dp(4));
        parent.addView(t);
        if (items.isEmpty()) {
            parent.addView(label("Không phát hiện", 13, TEXT, false));
            if (rep != null) rep.append(title).append(": none detected\n");
            return;
        }
        Flow f = new Flow(this, dp(8));
        for (String s : items) f.addView(tag(s));
        parent.addView(f, lp(MP, WC));
        if (rep != null) rep.append(title).append(": ").append(TextUtils.join(", ", items)).append('\n');
    }

    private static String yn(boolean b) { return b ? "Yes" : "No"; }

    private void showInfo() {
        if (pageUrl.isEmpty()) { toast("Hãy tải trang trước"); return; }
        LinearLayout body = newBody();
        final StringBuilder rep = new StringBuilder("Báo cáo Leon-Source web\n\n");
        Analysis a = analysis;

        int level; String badge, vt, vd;
        if (a != null) { level = a.level; badge = a.badge; vt = a.vTitle; vd = a.vDesc; }
        else {
            level = 1; badge = "KHÔNG PHẢI HTML";
            vt = pageKind.isEmpty() ? "Nội dung không xác định" : pageKind;
            vd = pageKind.equals("Tệp ZIP")
                    ? "URL này cung cấp tệp ZIP chứ không phải trang web. Hãy tải bằng trình duyệt hoặc trình quản lý tệp rồi giải nén."
                    : "URL này không trả về trang HTML (" + (contentType.isEmpty() ? "loại không xác định" : contentType) + ").";
            if (pageKind.equals("JSON") || pageKind.equals("Text")) vd = "Máy chủ trả về nội dung văn bản thuần " + pageKind.toLowerCase(Locale.ROOT) + ", shown as-is in the source view.";
        }
        int vc = level == 0 ? Color.rgb(70, 214, 130) : level == 1 ? Color.rgb(255, 190, 60) : Color.rgb(255, 95, 120);
        LinearLayout vcard = new LinearLayout(this);
        vcard.setOrientation(LinearLayout.VERTICAL);
        vcard.setPadding(dp(16), dp(14), dp(16), dp(16));
        vcard.setBackground(rect(blend(PANEL, vc, 0.14f), withAlpha(vc, 170), 18));
        TextView bd = label(badge, 10.5f, Color.rgb(18, 14, 28), true);
        bd.setLetterSpacing(0.1f);
        bd.setBackground(rect(vc, 0, 10));
        bd.setPadding(dp(10), dp(3), dp(10), dp(3));
        vcard.addView(bd, lp(WC, WC));
        TextView vtv = label(vt, 19, TEXT, true);
        vtv.setPadding(0, dp(8), 0, 0);
        vcard.addView(vtv);
        TextView vdv = label(vd, 13, MUTED, false);
        vdv.setLineSpacing(0, 1.15f);
        vdv.setPadding(0, dp(4), 0, 0);
        vcard.addView(vdv);
        body.addView(vcard, lpm(MP, WC, 0, 0, 0, 0, dp(10)));
        rep.append("Kết luận: ").append(vt).append("\n").append(vd).append("\n\n");

        LinearLayout glance = card(body, "TỔNG QUAN");
        kv(glance, "Loại trang", pageKind, rep);
        if (a != null) {
            kv(glance, "HTML thuần (không JavaScript)", yn(a.pureStatic), rep);
            kv(glance, "Cần JS để hiển thị nội dung", a.needsJs ? "Yes" : (a.level == 1 ? "Partly" : "No"), rep);
            kv(glance, "Framework", TextUtils.join(", ", a.frameworks), rep);
            kv(glance, "CMS / công cụ tạo web", TextUtils.join(", ", a.cms), rep);
        }

        LinearLayout ov = card(body, "OVERVIEW");
        if (a != null) kv(ov, "Title", a.title, rep);
        kv(ov, "URL", pageUrl, rep);
        kv(ov, "Trạng thái HTTP", String.valueOf(statusCode), rep);
        kv(ov, "Content-Type", contentType, rep);
        kv(ov, "Size", fmtBytes(sizeBytes), rep);
        kv(ov, "Thời gian tải", fetchMs + " ms", rep);
        kv(ov, "Server", serverHdr, rep);
        kv(ov, "Cung cấp bởi", poweredHdr, rep);
        if (a != null) {
            kv(ov, "Language", a.lang, rep);
            kv(ov, "Charset", a.charset, rep);
            kv(ov, "Khung nhìn di động", a.viewport.isEmpty() ? "No" : "Có  (" + a.viewport + ")", rep);
            kv(ov, "Generator", a.generator, rep);

            LinearLayout st = card(body, "CẤU TRÚC");
            kv(st, "Tệp CSS", a.extCss + " external  \u2022  " + a.inlCss + " inline", rep);
            kv(st, "Tập lệnh", a.extJs + " external  \u2022  " + a.inlJs + " inline (" + fmtBytes(a.inlJsBytes) + ")", rep);
            kv(st, "Hình ảnh", String.valueOf(a.imgs), rep);
            kv(st, "Video / âm thanh", String.valueOf(a.media), rep);
            kv(st, "iFrame", String.valueOf(a.iframes), rep);
            kv(st, "Biểu mẫu", String.valueOf(a.forms), rep);
            kv(st, "Liên kết", String.valueOf(a.links), rep);
            kv(st, "Phần tử HTML", String.valueOf(a.elements), rep);
            int pct = a.htmlLen == 0 ? 0 : Math.round(100f * a.textLen / a.htmlLen);
            kv(st, "Văn bản hiển thị", a.textLen + " ký tự  (" + pct + "% trên source)", rep);

            LinearLayout te = card(body, "TECHNOLOGIES");
            tagGroup(te, "Framework", a.frameworks, rep);
            tagGroup(te, "CMS / công cụ tạo web", a.cms, rep);
            tagGroup(te, "Thư viện", a.libs, rep);
            tagGroup(te, "Dịch vụ & lưu trữ", a.services, rep);

            LinearLayout seo = card(body, "SEO & META");
            kv(seo, "Mô tả", a.description, rep);
            kv(seo, "URL chuẩn", a.canonical, rep);
            kv(seo, "Robots", a.robots, rep);
            kv(seo, "Tiêu đề Open Graph", a.ogTitle, rep);
            kv(seo, "Biểu tượng trang", a.favicon, rep);
        }

        TextView note = label("Việc nhận diện dựa trên dấu hiệu trong HTML tĩnh và tiêu đề phản hồi nên có thể bỏ sót hoặc nhận diện sai. "
                + "Chỉ kiểm tra website mà bạn được phép truy cập.", 11, MUTED, false);
        note.setPadding(dp(4), dp(2), dp(4), dp(10));
        body.addView(note);
        TextView copyRep = chip("Sao chép báo cáo", true);
        copyRep.setOnClickListener(v -> {
            ((ClipboardManager) getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("report", rep.toString()));
            toast("Đã sao chép báo cáo");
        });
        body.addView(copyRep, lp(MP, dp(44)));

        Dialog d = openSheet("Thông tin website", body);
        d.show();
    }

    // ================================================================== settings
    private void openCài đặt() {
        settingsDialog = openSheet("Cài đặt", buildCài đặtBody());
        settingsDialog.show();
    }

    private void renderCài đặt() {
        if (settingsDialog == null || !settingsDialog.isShowing()) return;
        final int keep = sheetScroll != null ? sheetScroll.getScrollY() : 0;
        settingsDialog.setContentView(sheetView("Cài đặt", buildCài đặtBody(), settingsDialog));
        final ScrollView sv = sheetScroll;
        sv.post(() -> sv.scrollTo(0, keep));
    }

    private SeekBar styledSeek(int max, int progressVal) {
        SeekBar sb = new SeekBar(this);
        sb.setMax(max);
        sb.setProgress(progressVal);
        sb.setProgressTintList(ColorStateList.valueOf(accent));
        sb.setThumbTintList(ColorStateList.valueOf(accent));
        sb.setProgressBackgroundTintList(ColorStateList.valueOf(withAlpha(MUTED, 120)));
        return sb;
    }

    private LinearLayout buildCài đặtBody() {
        LinearLayout body = newBody();

        // ---- preview
        LinearLayout pv = card(body, "XEM TRƯỚC");
        final TextView p1 = label("Aa  Leon-Source web", 20, ACC_T, true);
        pv.addView(p1);
        pv.addView(label("Văn bản giao diện sẽ trông như thế này.", 13, TEXT, false));
        prevCode = label("<div class=\"leon\">Hello</div>", codeSize, TEXT, false);
        prevCode.setTypeface(codeTf);
        prevCode.setPadding(0, dp(6), 0, 0);
        pv.addView(prevCode);

        // ---- accent colour
        LinearLayout ac = card(body, "MÀU NHẤN");
        Flow sw = new Flow(this, dp(12));
        for (final int c : ACCENTS) {
            TextView s = new TextView(this);
            s.setGravity(Gravity.CENTER);
            s.setText(c == accent ? "\u2713" : "");
            s.setTextColor(lum(c) > 150 ? Color.rgb(18, 14, 28) : Color.WHITE);
            s.setTextSize(16);
            s.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            GradientDrawable o = new GradientDrawable();
            o.setShape(GradientDrawable.OVAL);
            o.setColor(c);
            if (c == accent) o.setStroke(dp(3), TEXT);
            s.setBackground(o);
            s.setLayoutParams(new ViewGroup.LayoutParams(dp(44), dp(44)));
            s.setOnClickListener(v -> { accent = c; applyTheme(); });
            sw.addView(s);
        }
        ac.addView(sw, lp(MP, WC));
        TextView hl = label("Tùy chỉnh sắc độ", 12, MUTED, false);
        hl.setPadding(0, dp(14), 0, dp(2));
        ac.addView(hl);
        float[] hsv = new float[3];
        Color.colorToHSV(accent, hsv);
        SeekBar hue = styledSeek(360, (int) hsv[0]);
        hue.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                if (fromUser) p1.setTextColor(Color.HSVToColor(new float[]{p % 360, 0.72f, 1f}));
            }
            @Override public void onStartTrackingTouch(SeekBar s) { }
            @Override public void onStopTrackingTouch(SeekBar s) {
                accent = Color.HSVToColor(new float[]{s.getProgress() % 360, 0.72f, 1f});
                applyTheme();
            }
        });
        ac.addView(hue, lp(MP, WC));

        // ---- background
        LinearLayout bgc = card(body, "NỀN");
        Flow bf = new Flow(this, dp(8));
        for (int i = 0; i < BG_NAMES.length; i++) {
            final int idx = i;
            TextView ch = chip(BG_NAMES[i], i == bgMode);
            ch.setOnClickListener(v -> { bgMode = idx; applyTheme(); });
            bf.addView(ch);
        }
        bgc.addView(bf, lp(MP, WC));

        // ---- interface font
        LinearLayout uf = card(body, "PHÔNG GIAO DIỆN");
        Flow ufl = new Flow(this, dp(8));
        for (int i = 0; i < FONT_NAMES.length; i++) {
            final int idx = i;
            TextView ch = chip(FONT_NAMES[i], i == uiFont);
            ch.setTypeface(Typeface.create(FONT_FAMILIES[i], Typeface.BOLD));
            ch.setOnClickListener(v -> { uiFont = idx; applyTheme(); });
            ufl.addView(ch);
        }
        uf.addView(ufl, lp(MP, WC));

        // ---- code font
        LinearLayout cf = card(body, "PHÔNG MÃ NGUỒN");
        Flow cfl = new Flow(this, dp(8));
        for (int i = 0; i < FONT_NAMES.length; i++) {
            final int idx = i;
            TextView ch = chip(FONT_NAMES[i], i == codeFont);
            ch.setTypeface(Typeface.create(FONT_FAMILIES[i], Typeface.BOLD));
            ch.setOnClickListener(v -> { codeFont = idx; applyTheme(); });
            cfl.addView(ch);
        }
        cf.addView(cfl, lp(MP, WC));

        // ---- code size
        LinearLayout cs = card(body, "CỠ CHỮ MÃ NGUỒN");
        final TextView sizeLbl = label(codeSize + " sp", 13, TEXT, true);
        cs.addView(sizeLbl);
        SeekBar sz = styledSeek(11, codeSize - 9);
        sz.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                codeSize = 9 + p;
                sizeLbl.setText(codeSize + " sp");
                if (sourceView != null) sourceView.setTextSize(codeSize);
                if (prevCode != null) prevCode.setTextSize(codeSize);
            }
            @Override public void onStartTrackingTouch(SeekBar s) { }
            @Override public void onStopTrackingTouch(SeekBar s) { savePrefs(); }
        });
        cs.addView(sz, lp(MP, WC));

        // ---- options
        LinearLayout op = card(body, "TÙY CHỌN");
        Flow trên = new Flow(this, dp(8));
        TextView hlChip = chip("Tô sáng cú pháp: " + (highlightOn ? "ON" : "OFF"), highlightOn);
        hlChip.setOnClickListener(v -> { highlightOn = !highlightOn; applyTheme(); });
        of.addView(hlChip);
        TextView reset = chip("Đặt lại mặc định", false);
        reset.setOnClickListener(v -> {
            accent = DEFAULT_ACCENT; bgMode = 0; uiFont = 0; codeFont = 2; codeSize = 12; highlightOn = true;
            applyTheme();
        });
        of.addView(reset);
        op.addView(of, lp(MP, WC));
        return body;
    }

    // ================================================================== misc
    private void hideKeyboard() {
        try {
            View f = getCurrentFocus();
            ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(
                    f != null ? f.getWindowToken() : urlInput.getWindowToken(), 0);
        } catch (Exception ignored) { }
    }

    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }
}
