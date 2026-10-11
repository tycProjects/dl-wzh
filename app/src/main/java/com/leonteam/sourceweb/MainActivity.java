package com.leonteam.sourceweb;

import android.app.*;
import android.content.*;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.net.Uri;
import android.os.*;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.InputType;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.TextWatcher;
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

    // ------------------------------------------------------------------ hằng số
    private static final int MP = ViewGroup.LayoutParams.MATCH_PARENT;
    private static final int WC = ViewGroup.LayoutParams.WRAP_CONTENT;
    private static final int REQ_ZIP = 1001;
    private static final String[] TABS = {"HTML", "CSS", "JS", "MEDIA", "LINK", "TEXT"};
    private static final String[] TAB_LABELS = {"HTML", "CSS", "JS", "MEDIA", "LIÊN KẾT", "CHỮ"};
    private static final String[] BG_NAMES = {"Tối", "AMOLED", "Xanh đêm", "Sáng"};
    private static final String[] FONT_NAMES = {"Sans", "Serif", "Mono", "Serif mono", "Hẹp", "Mảnh", "Vừa", "Viết tay", "Nghiêng"};
    private static final String[] FONT_FAMILIES = {"sans-serif", "serif", "monospace", "serif-monospace",
            "sans-serif-condensed", "sans-serif-light", "sans-serif-medium", "casual", "cursive"};
    private static final int DEFAULT_ACCENT = Color.rgb(174, 92, 255);
    private static final int[] ACCENTS = {
            Color.rgb(174, 92, 255), Color.rgb(0, 224, 255), Color.rgb(57, 255, 136), Color.rgb(255, 64, 160),
            Color.rgb(255, 159, 28), Color.rgb(255, 82, 82), Color.rgb(77, 141, 255), Color.rgb(255, 214, 10)};
    private static final String HINT = "Mã nguồn sẽ hiện ở đây.\n\n"
            + "•  Nhập địa chỉ website rồi bấm TẢI\n"
            + "•  Hoặc bấm “Mở ZIP” để duyệt và xem web trong tệp ZIP\n"
            + "•  Bấm “Xem trước” để chạy thử trang web ngay trong app\n\n"
            + "Chỉ kiểm tra website bạn được phép truy cập. Nội dung do JavaScript tạo ra có thể không có trong HTML gốc.";
    private static final int SPAN_FLAGS = Spannable.SPAN_EXCLUSIVE_EXCLUSIVE;
    private static final String[] SEC_HEADERS = {"Strict-Transport-Security", "Content-Security-Policy", "X-Frame-Options",
            "X-Content-Type-Options", "Referrer-Policy", "Cache-Control", "Content-Encoding", "Last-Modified"};

    private static final Pattern P_TAG = Pattern.compile("<[/!]?[A-Za-z][^<>]*>");
    private static final Pattern P_COMMENT = Pattern.compile("<!--.*?-->", Pattern.DOTALL);
    private static final Pattern P_RAW = Pattern.compile("<(script|style)\\b[^>]*>(.*?)</\\1\\s*>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    private static final Pattern P_STR = Pattern.compile("\"[^\"]*\"|'[^']*'");
    private static final Pattern P_ATTR = Pattern.compile("\\s([A-Za-z_:@][\\w:.@-]*)(?==)");
    private static final Pattern P_ANYTAG = Pattern.compile("<(/?)([A-Za-z][\\w:-]*)([^<>]*?)(/?)>");
    private static final HashSet<String> VOID_TAGS = new HashSet<>(Arrays.asList(
            "area", "base", "br", "col", "embed", "hr", "img", "input", "link", "meta", "param", "source", "track", "wbr", "!doctype"));
    private static final HashSet<String> HTML_EXT = new HashSet<>(Arrays.asList("html", "htm", "xhtml"));
    private static final HashSet<String> IMG_EXT = new HashSet<>(Arrays.asList("png", "jpg", "jpeg", "gif", "webp", "bmp", "ico", "svg", "avif"));
    private static final HashSet<String> MEDIA_EXT = new HashSet<>(Arrays.asList("mp4", "webm", "mp3", "ogg", "wav"));

    // ------------------------------------------------------------------ cài đặt / giao diện
    private SharedPreferences prefs;
    private int accent = DEFAULT_ACCENT, bgMode = 0, uiFont = 0, codeFont = 2, codeSize = 12;
    private boolean highlightOn = true;
    private int BG, PANEL, TEXT, MUTED, STROKE, ACC_T, ON_ACCENT;
    private Typeface uiTf, uiTfBold, codeTf;
    private int inL, inT, inR, inB;

    // ------------------------------------------------------------------ view
    private EditText urlInput, searchInput;
    private TextView status, sourceView, matchLabel, prevCode;
    private ScrollView sourceScroll, sheetScroll;
    private LinearLayout root;
    private ProgressBar progress;
    private final TextView[] tabViews = new TextView[TABS.length];
    private Dialog settingsDialog;

    // ------------------------------------------------------------------ trạng thái trang
    private String pageUrl = "", html = "", currentCode = "", activeTab = "HTML";
    private String statusMsg = "Sẵn sàng. Nhập địa chỉ website công khai hoặc mở tệp ZIP.";
    private String pageKind = "", contentType = "", serverHdr = "", poweredHdr = "";
    private int statusCode = 0;
    private long sizeBytes = 0, fetchMs = 0;
    private boolean busy = false, showingHint = true, viewHtml = false;
    private Analysis analysis;
    private LinkedHashMap<String, String> headers = new LinkedHashMap<>();
    private final ArrayList<Resource> resources = new ArrayList<>();
    private SpannableStringBuilder baseSpan = new SpannableStringBuilder("");
    private final ArrayList<Integer> matches = new ArrayList<>();
    private int matchIdx = -1, hlGen = 0;
    private String lastQuery = "";

    // ------------------------------------------------------------------ trạng thái ZIP
    private File zipDir;
    private String zipName = "";
    private long zipTotal = 0;
    private ArrayList<ZEntry> zipEntries;

    static class Resource {
        String type, url;
        Resource(String t, String u) { type = t; url = u; }
    }

    static class ZEntry {
        String path; long size;
        ZEntry(String p, long s) { path = p; size = s; }
    }

    static class Item {
        String title, sub = "", key = ""; boolean preview;
        Item(String t) { title = t; }
    }

    interface ItemPick { void pick(Item it); }
    interface QueryRun { void run(String q); }

    static class Analysis {
        String title = "", lang = "", charset = "", viewport = "", generator = "", description = "", robots = "", ogTitle = "", canonical = "", favicon = "", text = "";
        int extCss, inlCss, extJs, inlJs, inlJsBytes, imgs, media, iframes, forms, links, elements, textLen, htmlLen;
        ArrayList<String> frameworks = new ArrayList<>(), cms = new ArrayList<>(), libs = new ArrayList<>(), services = new ArrayList<>();
        int level; // 0 tĩnh, 1 lai, 2 render bằng JS
        String badge = "", vTitle = "", vDesc = "";
        boolean pureStatic, needsJs;
    }

    /** Layout tự xuống dòng cho chip và ô màu. */
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

    // ================================================================== vòng đời
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("leon_prefs", MODE_PRIVATE);
        accent = prefs.getInt("accent", DEFAULT_ACCENT);
        bgMode = prefs.getInt("bg", 0);
        uiFont = Math.max(0, Math.min(FONT_FAMILIES.length - 1, prefs.getInt("uiFont", 0)));
        codeFont = Math.max(0, Math.min(FONT_FAMILIES.length - 1, prefs.getInt("codeFont", 2)));
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

    // ================================================================== giao diện: màu, phông
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

    /** Áp dụng lại màu/phông trên toàn app nhưng giữ nguyên dữ liệu đang xem. */
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
        if (showingHint) {
            showTab(activeTab.isEmpty() ? "HTML" : activeTab);
        } else {
            rebuildBase();
            applyView();
            final ScrollView ns = sourceScroll;
            ns.post(() -> ns.scrollTo(0, sy));
        }
        renderSettings();
    }

    // ================================================================== view helper
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
    private EditText inputBox(String hint, Typeface tf, float size) {
        EditText e = new EditText(this);
        e.setBackground(null);
        e.setTextColor(TEXT);
        e.setHintTextColor(MUTED);
        e.setHint(hint);
        e.setTextSize(size);
        e.setTypeface(tf);
        e.setHighlightColor(withAlpha(accent, 90));
        return e;
    }

    // ================================================================== giao diện chính
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

        // ---- tiêu đề
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
        titles.addView(label("Soi mã nguồn web  •  nền " + BG_NAMES[bgMode], 11, MUTED, false));
        header.addView(titles, lp(0, WC, 1));
        TextView infoBtn = roundBtn("i");
        TextView setBtn = roundBtn("\u2699");
        header.addView(infoBtn, lpm(dp(42), dp(42), 0, 0, 0, dp(8), 0));
        header.addView(setBtn, lp(dp(42), dp(42)));
        root.addView(header, lpm(MP, WC, 0, 0, 0, 0, dp(12)));
        infoBtn.setOnClickListener(v -> showInfo());
        setBtn.setOnClickListener(v -> openSettings());

        // ---- ô nhập URL
        LinearLayout urlCard = new LinearLayout(this);
        urlCard.setGravity(Gravity.CENTER_VERTICAL);
        urlCard.setBackground(rect(PANEL, STROKE, 16));
        urlCard.setPadding(dp(14), dp(4), dp(6), dp(4));
        urlInput = inputBox("https://vidu.com  hoặc  .../tep.zip", codeTf, 14);
        urlInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        urlInput.setImeOptions(EditorInfo.IME_ACTION_GO);
        urlInput.setOnEditorActionListener((v, actionId, ev) -> { fetchPage(); return true; });
        urlCard.addView(urlInput, lp(0, dp(46), 1));
        TextView go = chip("TẢI", true);
        go.setTextSize(13);
        urlCard.addView(go, lp(dp(72), dp(40)));
        root.addView(urlCard, lp(MP, WC));
        go.setOnClickListener(v -> fetchPage());

        // ---- tiến trình + trạng thái
        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setIndeterminate(true);
        progress.setIndeterminateTintList(ColorStateList.valueOf(accent));
        progress.setVisibility(busy ? View.VISIBLE : View.INVISIBLE);
        root.addView(progress, lpm(MP, dp(3), 0, dp(4), dp(6), dp(4), 0));

        LinearLayout statusRow = new LinearLayout(this);
        statusRow.setGravity(Gravity.CENTER_VERTICAL);
        View dotView = new View(this);
        GradientDrawable dot = new GradientDrawable();
        dot.setShape(GradientDrawable.OVAL); dot.setColor(accent);
        dotView.setBackground(dot);
        statusRow.addView(dotView, lpm(dp(8), dp(8), 0, dp(2), 0, dp(8), 0));
        status = label(statusMsg, 12, MUTED, false);
        status.setMaxLines(2);
        status.setEllipsize(TextUtils.TruncateAt.END);
        statusRow.addView(status, lp(0, WC, 1));
        root.addView(statusRow, lpm(MP, WC, 0, 0, dp(4), 0, dp(10)));

        // ---- tab (cuộn ngang)
        HorizontalScrollView tabScroll = new HorizontalScrollView(this);
        tabScroll.setHorizontalScrollBarEnabled(false);
        tabScroll.setBackground(rect(PANEL, STROKE, 16));
        LinearLayout tabs = new LinearLayout(this);
        tabs.setPadding(dp(4), dp(4), dp(4), dp(4));
        for (int i = 0; i < TABS.length; i++) {
            final String name = TABS[i];
            TextView tv = new TextView(this);
            tv.setGravity(Gravity.CENTER);
            tv.setTextSize(12.5f);
            tv.setTypeface(uiTfBold);
            tv.setSingleLine(true);
            tv.setClickable(true);
            tv.setPadding(dp(14), 0, dp(14), 0);
            ripple(tv, 12);
            tv.setOnClickListener(v -> showTab(name));
            tabViews[i] = tv;
            tabs.addView(tv, lpm(WC, dp(38), 0, dp(2), 0, dp(2), 0));
        }
        tabScroll.addView(tabs);
        root.addView(tabScroll, lp(MP, WC));
        updateTabs();

        // ---- vùng mã
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

        // ---- tìm kiếm
        LinearLayout searchRow = new LinearLayout(this);
        searchRow.setGravity(Gravity.CENTER_VERTICAL);
        searchRow.setBackground(rect(PANEL, STROKE, 16));
        searchRow.setPadding(dp(14), dp(2), dp(6), dp(2));
        searchInput = inputBox("Tìm trong mã đang xem…", uiTf, 13);
        searchInput.setInputType(InputType.TYPE_CLASS_TEXT);
        searchInput.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
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

        // ---- hàng chức năng (cuộn ngang)
        HorizontalScrollView actScroll = new HorizontalScrollView(this);
        actScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout actions = new LinearLayout(this);
        TextView bPreview = chip("▶ Xem trước", true), bZip = chip("Mở ZIP", true), bRes = chip("Tài nguyên", false),
                bFormat = chip("Định dạng", false), bMin = chip("Thu gọn", false), bCopy = chip("Sao chép", false),
                bExport = chip("Xuất ZIP", false), bHist = chip("Lịch sử", false), bPaste = chip("Dán URL", false),
                bExt = chip("Mở ngoài", false);
        TextView[] acts = {bPreview, bZip, bRes, bFormat, bMin, bCopy, bExport, bHist, bPaste, bExt};
        for (TextView b : acts) actions.addView(b, lpm(WC, dp(40), 0, dp(3), 0, dp(3), 0));
        actScroll.addView(actions);
        root.addView(actScroll, lpm(MP, WC, 0, 0, dp(8), 0, 0));
        bPreview.setOnClickListener(v -> openPreviewMenu());
        bZip.setOnClickListener(v -> pickZip());
        bRes.setOnClickListener(v -> showResources());
        bFormat.setOnClickListener(v -> transform(true));
        bMin.setOnClickListener(v -> transform(false));
        bCopy.setOnClickListener(v -> {
            if (!hasContent()) { toast("Chưa có nội dung để sao chép"); return; }
            ((ClipboardManager) getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("source", currentCode));
            toast("Đã sao chép " + currentCode.length() + " ký tự");
        });
        bExport.setOnClickListener(v -> exportZip());
        bHist.setOnClickListener(v -> showHistory());
        bPaste.setOnClickListener(v -> pasteUrl());
        bExt.setOnClickListener(v -> openExternal());
    }

    private boolean hasContent() { return !showingHint && !currentCode.isEmpty(); }

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
            int n = (i >= 1 && i <= 4) ? countOf(TABS[i]) : 0;
            boolean sel = TABS[i].equals(activeTab);
            t.setText(n > 0 ? TAB_LABELS[i] + " " + n : TAB_LABELS[i]);
            t.setTextColor(sel ? ON_ACCENT : MUTED);
            t.setBackground(sel ? rect(accent, 0, 12) : null);
        }
    }

    // ================================================================== tải trang
    private HttpURLConnection openConnection(String url) throws IOException {
        String cur = url;
        for (int i = 0; i < 6; i++) {
            HttpURLConnection c = (HttpURLConnection) new URL(cur).openConnection();
            c.setConnectTimeout(15000);
            c.setReadTimeout(20000);
            c.setInstanceFollowRedirects(false);
            c.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14) Leon-Source-web/3.0");
            c.setRequestProperty("Accept", "text/html,application/xhtml+xml,*/*;q=0.8");
            int code = c.getResponseCode();
            if (code == 301 || code == 302 || code == 303 || code == 307 || code == 308) {
                String loc = c.getHeaderField("Location");
                c.disconnect();
                if (loc == null) throw new IOException("Chuyển hướng không có địa chỉ đích");
                cur = new URL(new URL(cur), loc).toString();
                continue;
            }
            return c;
        }
        throw new IOException("Chuyển hướng quá nhiều lần");
    }

    private void fetchPage() {
        String input = urlInput.getText().toString().trim();
        if (input.isEmpty()) { toast("Hãy nhập địa chỉ website trước"); return; }
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
                final String ctype = nz(c.getContentType());
                final String finalUrl = c.getURL().toString();
                String pathLow = nz(c.getURL().getPath()).toLowerCase(Locale.ROOT);
                if (code < 400 && (ctype.toLowerCase(Locale.ROOT).contains("zip") || pathLow.endsWith(".zip"))) {
                    downloadAndOpenZip(c, finalUrl);
                    return;
                }
                InputStream in = (code >= 400) ? c.getErrorStream() : c.getInputStream();
                byte[] data = readLimited(in, 5 * 1024 * 1024);
                final String server = nz(c.getHeaderField("Server"));
                final String powered = nz(c.getHeaderField("X-Powered-By"));
                final boolean cf = c.getHeaderField("CF-RAY") != null;
                final LinkedHashMap<String, String> hdr = new LinkedHashMap<>();
                for (String hn : SEC_HEADERS) { String hv = c.getHeaderField(hn); if (hv != null) hdr.put(hn, hv); }
                c.disconnect();
                final long ms = System.currentTimeMillis() - t0;
                final String kind = detectKind(data, ctype, finalUrl);
                if (kind.equals("ZIP archive")) {
                    publishZip(extractZip(new ByteArrayInputStream(data)), zipNameFromUrl(finalUrl));
                    return;
                }
                final String body = (kind.equals("HTML") || kind.equals("Text") || kind.equals("JSON"))
                        ? new String(data, charsetFor(ctype, data)) : "";
                final Analysis an = kind.equals("HTML") ? analyze(body, server, powered, cf) : null;
                final int fcode = code;
                final int size = data.length;
                runOnUiThread(() -> {
                    pageUrl = finalUrl;
                    urlInput.setText(finalUrl);
                    html = body; pageKind = kind; analysis = an; statusCode = fcode; headers = hdr;
                    contentType = ctype; serverHdr = server; poweredHdr = powered; sizeBytes = size; fetchMs = ms;
                    resources.clear();
                    if (kind.equals("HTML")) parseResources(body, finalUrl);
                    prefs.edit().putString("lastUrl", finalUrl).apply();
                    addHistory(finalUrl);
                    activeTab = "HTML";
                    setBusy(false);
                    setStatus("HTTP " + fcode + "  •  " + fmtBytes(size) + "  •  "
                            + (kind.equals("HTML") ? resources.size() + " tài nguyên/liên kết" : kind) + "  •  " + ms + " ms");
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
            if (total > max) throw new IOException("Dung lượng vượt giới hạn " + fmtBytes(max));
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
        if (d.length >= 4 && d[0] == 'P' && d[1] == 'K' && (d[2] == 3 || d[2] == 5) && (d[3] == 4 || d[3] == 6)) return "ZIP archive";
        if (ct.contains("zip") || u.endsWith(".zip")) return "ZIP archive";
        if (d.length >= 2 && (d[0] & 0xff) == 0x1f && (d[1] & 0xff) == 0x8b) return "GZIP";
        if (d.length >= 4 && d[0] == 0x25 && d[1] == 'P' && d[2] == 'D' && d[3] == 'F') return "PDF";
        if (ct.contains("html")) return "HTML";
        if (ct.contains("json")) return "JSON";
        if (ct.startsWith("text/") || ct.contains("xml") || ct.contains("javascript")) return "Text";
        if (ct.startsWith("image/")) return "Ảnh";
        if (ct.startsWith("video/") || ct.startsWith("audio/")) return "Media";
        if (ct.isEmpty()) {
            int n = Math.min(d.length, 512);
            String head = new String(d, 0, n, StandardCharsets.ISO_8859_1).toLowerCase(Locale.ROOT);
            if (head.contains("<html") || head.contains("<!doctype")) return "HTML";
            for (int i = 0; i < n; i++) if (d[i] == 0) return "Tệp nhị phân";
            return "Text";
        }
        return "Tệp nhị phân";
    }

    private Charset charsetFor(String ctype, byte[] d) {
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
        Pattern p = Pattern.compile("(?is)<(script|link|img|source|video|audio|embed)\\b([^>]+)>?");
        Matcher m = p.matcher(src);
        while (m.find()) {
            String tg = m.group(1).toLowerCase(Locale.ROOT), attrs = m.group(2);
            String raw = null, type;
            Matcher a = Pattern.compile("(?is)(src|href)\\s*=\\s*['\\\"]([^'\\\"]+)['\\\"]").matcher(attrs);
            if (a.find()) raw = a.group(2);
            if (raw == null) continue;
            if (tg.equals("link")) {
                if (attrs.toLowerCase(Locale.ROOT).contains("stylesheet")) type = "CSS"; else continue;
            } else if (tg.equals("script")) type = "JS";
            else type = "MEDIA";
            try {
                String abs = new URL(new URL(base), raw).toString();
                if (abs.startsWith("http://") || abs.startsWith("https://")) resources.add(new Resource(type, abs));
            } catch (Exception ignored) { }
        }
        Matcher lm = Pattern.compile("(?is)<a\\b[^>]*?\\bhref\\s*=\\s*['\\\"]([^'\\\"#][^'\\\"]*)['\\\"]").matcher(src);
        int links = 0;
        while (lm.find() && links < 3000) {
            String raw = lm.group(1).trim();
            try {
                String low = raw.toLowerCase(Locale.ROOT);
                if (low.startsWith("javascript:")) continue;
                String abs = (low.startsWith("mailto:") || low.startsWith("tel:")) ? raw : new URL(new URL(base), raw).toString();
                resources.add(new Resource("LINK", abs));
                links++;
            } catch (Exception ignored) { }
        }
        LinkedHashMap<String, Resource> unique = new LinkedHashMap<>();
        for (Resource r : resources) unique.put(r.type + "|" + r.url, r);
        resources.clear();
        resources.addAll(unique.values());
    }

    // ================================================================== tab + hiển thị
    private String nonHtmlMessage() {
        return "Địa chỉ này trả về: " + pageKind + "\nContent-Type: " + (contentType.isEmpty() ? "không rõ" : contentType)
                + "\nDung lượng: " + fmtBytes(sizeBytes) + "\n\nĐây không phải trang HTML nên không có mã để hiển thị."
                + "\n\nMở nút (i) để xem chi tiết.";
    }

    private void showTab(String tab) {
        activeTab = tab;
        updateTabs();
        StringBuilder b = new StringBuilder();
        showingHint = pageUrl.isEmpty();
        viewHtml = false;
        if (showingHint) {
            b.append(HINT);
        } else if (tab.equals("HTML")) {
            if (!pageKind.equals("HTML") && html.isEmpty()) b.append(nonHtmlMessage()); else b.append(html);
            viewHtml = pageKind.equals("HTML");
        } else if (tab.equals("TEXT")) {
            if (analysis == null || analysis.text.isEmpty()) b.append("Không có nội dung chữ trong HTML gốc (có thể trang được tạo bằng JavaScript).");
            else b.append(analysis.text);
        } else {
            int i = 0;
            for (Resource r : resources) if (r.type.equals(tab)) b.append(String.format(Locale.US, "%02d  %s\n\n", ++i, r.url));
            if (i == 0) b.append("Không tìm thấy mục nào trong HTML gốc.");
            else b.append("Mẹo: bấm “Tài nguyên” để mở từng mục (xem mã CSS/JS, xem ảnh, đi theo liên kết).");
        }
        currentCode = b.toString();
        resetSearch();
        rebuildBase();
        applyView();
        sourceScroll.scrollTo(0, 0);
    }

    /** Hiện một đoạn văn bản bất kỳ (tệp trong ZIP, CSS/JS tải riêng…). */
    private void showText(String title, String text, boolean htmlLike) {
        showingHint = false;
        viewHtml = htmlLike;
        activeTab = "";
        currentCode = text;
        updateTabs();
        resetSearch();
        rebuildBase();
        applyView();
        sourceScroll.scrollTo(0, 0);
        setStatus(title + "  •  " + text.length() + " ký tự");
    }

    /** Hiện chữ thường ngay, tô màu ở luồng nền rồi cập nhật. */
    private void rebuildBase() {
        baseSpan = new SpannableStringBuilder(currentCode);
        final int gen = ++hlGen;
        if (highlightOn && viewHtml && currentCode.length() <= 250000) {
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

    // ================================================================== tìm kiếm
    private void resetSearch() {
        matches.clear();
        lastQuery = "";
        matchIdx = -1;
        if (matchLabel != null) matchLabel.setText("");
    }

    private void searchSource(boolean forward) {
        String q = searchInput.getText().toString();
        if (q.isEmpty()) { toast("Hãy nhập từ khoá cần tìm"); return; }
        hideKeyboard();
        if (!q.equals(lastQuery) || matches.isEmpty()) {
            matches.clear();
            Matcher m = Pattern.compile(Pattern.quote(q), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE).matcher(currentCode);
            while (m.find() && matches.size() < 5000) matches.add(m.start());
            lastQuery = q;
            matchIdx = forward ? -1 : 0;
        }
        int n = matches.size();
        if (n == 0) {
            matchLabel.setText("0/0");
            setStatus("Không tìm thấy: " + q);
            applyView();
            return;
        }
        matchIdx = (((matchIdx + (forward ? 1 : -1)) % n) + n) % n;
        applyView();
        int pos = matches.get(matchIdx);
        int line = 1;
        for (int i = 0; i < pos; i++) if (currentCode.charAt(i) == '\n') line++;
        matchLabel.setText((matchIdx + 1) + "/" + n + (n >= 5000 ? "+" : ""));
        setStatus("Kết quả " + (matchIdx + 1) + "/" + n + " cho “" + q + "”  •  dòng " + line);
        final int fpos = pos;
        sourceScroll.post(() -> {
            Layout l = sourceView.getLayout();
            if (l != null) {
                int ln = l.getLineForOffset(Math.min(fpos, Math.max(0, sourceView.length() - 1)));
                sourceScroll.smoothScrollTo(0, Math.max(0, l.getLineTop(ln) - dp(80)));
            }
        });
    }

    // ================================================================== định dạng / thu gọn
    private void transform(boolean pretty) {
        if (!hasContent()) { toast("Chưa có mã để xử lý"); return; }
        if (!viewHtml) { toast("Định dạng / Thu gọn chỉ dùng cho mã HTML"); return; }
        String s = currentCode;
        if (pretty) s = prettyHtml(s);
        else s = s.replaceAll(">\\s+<", "><").replaceAll("\\s{2,}", " ");
        currentCode = s;
        resetSearch();
        rebuildBase();
        applyView();
        setStatus(pretty ? "Đã định dạng thụt lề (cơ bản)." : "Đã thu gọn khoảng trắng.  " + s.length() + " ký tự");
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

    // ================================================================== xuất ZIP
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
                    if (r.type.equals("LINK")) continue;
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
                        c.setRequestProperty("User-Agent", "Leon-Source-web/3.0");
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
                String readme = "Leon-Source web - bản xuất\nĐịa chỉ nguồn: " + pageUrl + "\nSố tài nguyên đã tải: " + downloaded
                        + "\nLưu ý: đây là bản chụp tĩnh; nội dung động và đường dẫn tương đối có thể cần chỉnh lại.\n";
                addEntry(zip, "README.txt", readme.getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
                final String msg = e.getMessage();
                runOnUiThread(() -> { setBusy(false); setStatus("Tạo ZIP thất bại: " + msg); });
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
                    setStatus("ZIP đã sẵn sàng  •  đã tải " + done + " tệp liên kết (tối đa 30)");
                } catch (Exception e) {
                    setStatus("Đã tạo ZIP trong bộ nhớ đệm nhưng không có ứng dụng nhận chia sẻ.");
                }
            });
        }).start();
    }

    private void addEntry(ZipOutputStream zip, String name, byte[] data) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(data);
        zip.closeEntry();
    }

    // ================================================================== mở / duyệt ZIP
    private void pickZip() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        i.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/zip", "application/x-zip-compressed", "application/x-zip", "application/octet-stream"});
        try { startActivityForResult(i, REQ_ZIP); }
        catch (Exception e) { toast("Không mở được trình chọn tệp"); }
    }

    @Override protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == REQ_ZIP && res == RESULT_OK && data != null && data.getData() != null) openZipUri(data.getData());
    }

    private String queryName(Uri uri) {
        try (Cursor c = getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (c != null && c.moveToFirst()) return c.getString(0);
        } catch (Exception ignored) { }
        String p = uri.getLastPathSegment();
        return p == null ? "tep.zip" : p.substring(p.lastIndexOf('/') + 1);
    }

    private void openZipUri(final Uri uri) {
        setBusy(true);
        setStatus("Đang giải nén ZIP…");
        new Thread(() -> {
            try {
                String name = queryName(uri);
                InputStream in = getContentResolver().openInputStream(uri);
                if (in == null) throw new IOException("Không đọc được tệp");
                publishZip(extractZip(in), name);
            } catch (Exception e) {
                final String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                runOnUiThread(() -> { setBusy(false); setStatus("Mở ZIP thất bại: " + msg); });
            }
        }).start();
    }

    private String zipNameFromUrl(String url) {
        String n = url;
        int q = n.indexOf('?');
        if (q > 0) n = n.substring(0, q);
        n = n.substring(n.lastIndexOf('/') + 1);
        return n.isEmpty() ? "tai-ve.zip" : n;
    }

    /** Chạy trong luồng nền: tải ZIP từ web về bộ nhớ đệm rồi giải nén. */
    private void downloadAndOpenZip(HttpURLConnection c, String url) throws IOException {
        File tmp = new File(getCacheDir(), "download.zip");
        final long len = c.getContentLengthLong();
        try (InputStream in = c.getInputStream(); OutputStream os = new FileOutputStream(tmp)) {
            byte[] buf = new byte[16384];
            int r;
            long total = 0, lastUi = 0;
            while ((r = in.read(buf)) > 0) {
                total += r;
                if (total > 100L * 1024 * 1024) throw new IOException("Tệp ZIP lớn hơn 100 MB");
                os.write(buf, 0, r);
                if (total - lastUi > 400 * 1024) {
                    lastUi = total;
                    final long t = total;
                    runOnUiThread(() -> setStatus("Đang tải ZIP… " + fmtBytes(t) + (len > 0 ? " / " + fmtBytes(len) : "")));
                }
            }
        }
        c.disconnect();
        runOnUiThread(() -> setStatus("Đang giải nén ZIP…"));
        ArrayList<ZEntry> list;
        try (InputStream fin = new FileInputStream(tmp)) { list = extractZip(fin); }
        tmp.delete();
        publishZip(list, zipNameFromUrl(url));
    }

    private void deleteRec(File f) {
        if (f == null || !f.exists()) return;
        File[] kids = f.listFiles();
        if (kids != null) for (File k : kids) deleteRec(k);
        f.delete();
    }

    /** Giải nén an toàn (chống zip-slip, zip-bomb) vào bộ nhớ đệm. */
    private ArrayList<ZEntry> extractZip(InputStream raw) throws IOException {
        File base = new File(getCacheDir(), "zipview");
        deleteRec(base);
        base.mkdirs();
        String baseCanon = base.getCanonicalPath();
        ArrayList<ZEntry> list = new ArrayList<>();
        long total = 0;
        byte[] buf = new byte[16384];
        try (ZipInputStream zin = new ZipInputStream(new BufferedInputStream(raw))) {
            ZipEntry e;
            while ((e = zin.getNextEntry()) != null) {
                String n = e.getName().replace('\\', '/');
                if (e.isDirectory() || n.startsWith("__MACOSX/") || n.endsWith(".DS_Store")) continue;
                if (list.size() >= 5000) throw new IOException("ZIP có quá nhiều tệp (giới hạn 5000)");
                File out = new File(base, n);
                if (!out.getCanonicalPath().startsWith(baseCanon + File.separator)) continue;
                File parent = out.getParentFile();
                if (parent != null) parent.mkdirs();
                long size = 0;
                try (OutputStream os = new BufferedOutputStream(new FileOutputStream(out))) {
                    int r;
                    while ((r = zin.read(buf)) > 0) {
                        size += r;
                        total += r;
                        if (size > 80L * 1024 * 1024 || total > 400L * 1024 * 1024) throw new IOException("ZIP giải nén quá lớn (giới hạn 400 MB)");
                        os.write(buf, 0, r);
                    }
                }
                list.add(new ZEntry(n, size));
            }
        }
        Collections.sort(list, (x, y) -> x.path.compareToIgnoreCase(y.path));
        return list;
    }

    private void publishZip(final ArrayList<ZEntry> list, final String name) {
        long total = 0;
        for (ZEntry z : list) total += z.size;
        final long tot = total;
        runOnUiThread(() -> {
            zipDir = new File(getCacheDir(), "zipview");
            zipEntries = list;
            zipName = name;
            zipTotal = tot;
            setBusy(false);
            setStatus("Đã mở ZIP: " + name + "  •  " + list.size() + " tệp  •  " + fmtBytes(tot));
            showZipSheet();
        });
    }

    private static String ext(String path) {
        int s = path.lastIndexOf('/');
        int i = path.lastIndexOf('.');
        return (i < 0 || i < s) ? "" : path.substring(i + 1).toLowerCase(Locale.ROOT);
    }

    private String typeLabel(String ext) {
        if (HTML_EXT.contains(ext)) return "Trang HTML";
        if (ext.equals("css")) return "CSS";
        if (ext.equals("js") || ext.equals("mjs")) return "JavaScript";
        if (IMG_EXT.contains(ext)) return "Ảnh";
        if (MEDIA_EXT.contains(ext)) return "Âm thanh/Video";
        if (ext.equals("json") || ext.equals("xml") || ext.equals("txt") || ext.equals("md")) return "Văn bản";
        return ext.isEmpty() ? "Tệp" : ext.toUpperCase(Locale.ROOT);
    }

    private boolean previewable(String ext) {
        return HTML_EXT.contains(ext) || IMG_EXT.contains(ext) || MEDIA_EXT.contains(ext);
    }

    /** Trang chính của web trong ZIP: index.html nông nhất, nếu không có thì html nông nhất. */
    private String bestIndexPath() {
        if (zipEntries == null) return null;
        String best = null; int bd = 999;
        for (ZEntry z : zipEntries) {
            String l = z.path.toLowerCase(Locale.ROOT);
            boolean idx = l.equals("index.html") || l.endsWith("/index.html") || l.equals("index.htm") || l.endsWith("/index.htm");
            if (!idx) continue;
            int d = 0;
            for (int i = 0; i < l.length(); i++) if (l.charAt(i) == '/') d++;
            if (d < bd) { bd = d; best = z.path; }
        }
        if (best == null) {
            for (ZEntry z : zipEntries) if (HTML_EXT.contains(ext(z.path))) { best = z.path; break; }
        }
        return best;
    }

    private void showZipSheet() {
        if (zipEntries == null) { toast("Chưa mở tệp ZIP nào"); return; }
        int nHtml = 0, nCss = 0, nJs = 0, nImg = 0, nOther = 0;
        for (ZEntry z : zipEntries) {
            String e = ext(z.path);
            if (HTML_EXT.contains(e)) nHtml++;
            else if (e.equals("css")) nCss++;
            else if (e.equals("js") || e.equals("mjs")) nJs++;
            else if (IMG_EXT.contains(e)) nImg++;
            else nOther++;
        }
        final String best = bestIndexPath();
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(dp(16), dp(4), dp(16), dp(4));
        LinearLayout sum = card(header, null);
        sum.addView(label(zipName, 15, TEXT, true));
        TextView st = label(zipEntries.size() + " tệp  •  " + fmtBytes(zipTotal) + "\nHTML " + nHtml + "  •  CSS " + nCss + "  •  JS " + nJs
                + "  •  Ảnh " + nImg + "  •  Khác " + nOther, 12, MUTED, false);
        st.setPadding(0, dp(4), 0, dp(4));
        sum.addView(st);
        sum.addView(label(best != null ? "Trang chính: " + best : "Không thấy trang HTML nào trong ZIP", 12.5f, best != null ? ACC_T : MUTED, true));
        if (best != null) {
            TextView go = chip("▶ Xem trước web trong ZIP", true);
            go.setOnClickListener(v -> previewZipIndex());
            header.addView(go, lpm(MP, dp(44), 0, 0, 0, 0, dp(8)));
        }
        ArrayList<Item> items = new ArrayList<>();
        for (ZEntry z : zipEntries) {
            Item it = new Item(z.path);
            String e = ext(z.path);
            it.sub = fmtBytes(z.size) + "  •  " + typeLabel(e);
            it.preview = previewable(e);
            it.key = z.path;
            items.add(it);
        }
        listSheet("Tệp ZIP", header, items, true,
                it -> showZipFile(it.key),
                it -> previewZipFile(it.key),
                q -> searchZip(q));
    }

    private void showZipFile(String path) {
        try {
            File f = new File(zipDir, path);
            long len = f.length();
            int cap = 2 * 1024 * 1024;
            byte[] data;
            try (InputStream in = new FileInputStream(f)) {
                ByteArrayOutputStream bo = new ByteArrayOutputStream();
                byte[] buf = new byte[16384];
                int r;
                while ((r = in.read(buf)) > 0 && bo.size() < cap) bo.write(buf, 0, r);
                data = bo.toByteArray();
            }
            boolean bin = false;
            for (int i = 0; i < Math.min(data.length, 4096); i++) if (data[i] == 0) { bin = true; break; }
            String e = ext(path);
            if (bin) {
                showText(zipName + " › " + path, "Đây là tệp nhị phân (" + typeLabel(e) + ", " + fmtBytes(len) + ").\n\nDùng nút “Xem” bên cạnh tên tệp trong danh sách ZIP để xem trước.", false);
                return;
            }
            String text = new String(data, StandardCharsets.UTF_8);
            if (len > cap) text += "\n\n… (đã cắt, chỉ hiển thị 2 MB đầu)";
            showText(zipName + " › " + path, text, HTML_EXT.contains(e) || e.equals("svg") || e.equals("xml"));
        } catch (Exception e) {
            toast("Không đọc được tệp: " + e.getMessage());
        }
    }

    private void searchZip(final String q) {
        if (q.trim().isEmpty()) { toast("Hãy gõ từ khoá vào ô lọc rồi bấm lại"); return; }
        setBusy(true);
        setStatus("Đang tìm “" + q + "” trong ZIP…");
        new Thread(() -> {
            ArrayList<Item> res = new ArrayList<>();
            Pattern p = Pattern.compile(Pattern.quote(q), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
            for (ZEntry z : zipEntries) {
                if (z.size > 2 * 1024 * 1024 || z.size == 0) continue;
                try {
                    byte[] d = new byte[(int) z.size];
                    try (DataInputStream in = new DataInputStream(new FileInputStream(new File(zipDir, z.path)))) { in.readFully(d); }
                    boolean bin = false;
                    for (int i = 0; i < Math.min(d.length, 2048); i++) if (d[i] == 0) { bin = true; break; }
                    if (bin) continue;
                    Matcher m = p.matcher(new String(d, StandardCharsets.UTF_8));
                    int n = 0;
                    while (m.find() && n < 9999) n++;
                    if (n > 0) {
                        Item it = new Item(z.path);
                        it.sub = n + " lần khớp";
                        it.key = z.path;
                        res.add(it);
                        if (res.size() >= 300) break;
                    }
                } catch (Exception ignored) { }
            }
            runOnUiThread(() -> {
                setBusy(false);
                setStatus("Tìm “" + q + "”: " + res.size() + " tệp có kết quả");
                if (res.isEmpty()) { toast("Không tệp nào chứa “" + q + "”"); return; }
                listSheet("Kết quả: “" + q + "”", null, res, true, it -> {
                    showZipFile(it.key);
                    searchInput.setText(q);
                    searchSource(true);
                }, null, null);
            });
        }).start();
    }

    // ================================================================== xem trước (PreviewActivity)
    private void startPreview(String mode, String url, String rootDir, String start, String file, String title) {
        Intent i = new Intent(this, PreviewActivity.class);
        i.putExtra("mode", mode);
        i.putExtra("url", url);
        i.putExtra("root", rootDir);
        i.putExtra("start", start);
        i.putExtra("file", file);
        i.putExtra("title", title);
        startActivity(i);
    }

    private void previewZipIndex() {
        String best = bestIndexPath();
        if (best == null || zipDir == null) { toast("Không có trang HTML trong ZIP"); return; }
        int s = best.lastIndexOf('/');
        File rootDir = s < 0 ? zipDir : new File(zipDir, best.substring(0, s));
        startPreview("zip", "", rootDir.getPath(), best.substring(s + 1), "", zipName);
    }

    private void previewZipFile(String path) {
        if (zipDir == null) return;
        startPreview("zip", "", zipDir.getPath(), path, "", path);
    }

    private void previewSnapshot() {
        try {
            File dir = new File(getCacheDir(), "preview");
            dir.mkdirs();
            File f = new File(dir, "snapshot.html");
            try (FileOutputStream os = new FileOutputStream(f)) { os.write(html.getBytes(StandardCharsets.UTF_8)); }
            startPreview("html", pageUrl, "", "", f.getPath(), "Bản HTML đã tải");
        } catch (Exception e) {
            toast("Không tạo được bản xem trước: " + e.getMessage());
        }
    }

    private void openPreviewMenu() {
        ArrayList<Item> opts = new ArrayList<>();
        if (!pageUrl.isEmpty()) {
            Item a = new Item("Trang web trực tiếp");
            a.sub = "Mở thẳng website như trình duyệt (chạy JavaScript)";
            a.key = "live";
            opts.add(a);
            if (!html.isEmpty()) {
                Item b = new Item("Bản HTML đã tải");
                b.sub = "Dựng lại từ HTML vừa tải — thử tắt JavaScript để xem bản tĩnh";
                b.key = "snap";
                opts.add(b);
            }
        }
        if (zipEntries != null && bestIndexPath() != null) {
            Item c = new Item("Web trong ZIP: " + zipName);
            c.sub = "Trang chính: " + bestIndexPath() + " (chạy hoàn toàn trong app)";
            c.key = "zip";
            opts.add(c);
        }
        if (opts.isEmpty()) { toast("Hãy tải một trang web hoặc mở tệp ZIP trước"); return; }
        ItemPick run = it -> {
            if (it.key.equals("live")) startPreview("url", pageUrl, "", "", "", pageUrl);
            else if (it.key.equals("snap")) previewSnapshot();
            else previewZipIndex();
        };
        if (opts.size() == 1) { run.pick(opts.get(0)); return; }
        listSheet("Chọn kiểu xem trước", null, opts, false, run, null, null);
    }

    // ================================================================== tài nguyên / lịch sử / tiện ích
    private void showResources() {
        if (pageUrl.isEmpty() || resources.isEmpty()) { toast("Chưa có tài nguyên nào — hãy tải một trang HTML"); return; }
        boolean one = activeTab.equals("CSS") || activeTab.equals("JS") || activeTab.equals("MEDIA") || activeTab.equals("LINK");
        ArrayList<Item> items = new ArrayList<>();
        for (Resource r : resources) {
            if (one && !r.type.equals(activeTab)) continue;
            Item it = new Item(r.url);
            it.sub = r.type.equals("LINK") ? "Liên kết  •  chạm để mở trang này" : r.type + "  •  chạm để " + (r.type.equals("MEDIA") ? "xem trước" : "xem mã");
            it.key = r.type;
            items.add(it);
        }
        if (items.isEmpty()) { toast("Tab này chưa có mục nào"); return; }
        listSheet(one ? "Tài nguyên: " + activeTab : "Tất cả tài nguyên", null, items, true, it -> {
            if (it.key.equals("CSS") || it.key.equals("JS")) fetchText(it.title);
            else if (it.key.equals("MEDIA")) startPreview("url", it.title, "", "", "", it.title);
            else if (it.title.startsWith("http")) { urlInput.setText(it.title); fetchPage(); }
            else toast(it.title);
        }, null, null);
    }

    private void fetchText(final String url) {
        setBusy(true);
        setStatus("Đang tải " + url + " …");
        new Thread(() -> {
            try {
                HttpURLConnection c = openConnection(url);
                InputStream in = c.getResponseCode() >= 400 ? c.getErrorStream() : c.getInputStream();
                byte[] data = readLimited(in, 3 * 1024 * 1024);
                String ctype = nz(c.getContentType());
                c.disconnect();
                final String kind = detectKind(data, ctype, url);
                final String text = new String(data, charsetFor(ctype, data));
                runOnUiThread(() -> {
                    setBusy(false);
                    if (kind.equals("Tệp nhị phân") || kind.equals("ZIP archive")) setStatus("Tệp này là " + kind + ", không hiển thị dạng chữ được.");
                    else showText(url, text, kind.equals("HTML"));
                });
            } catch (Exception e) {
                final String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                runOnUiThread(() -> { setBusy(false); setStatus("Tải thất bại: " + msg); });
            }
        }).start();
    }

    private ArrayList<String> loadHistory() {
        ArrayList<String> l = new ArrayList<>();
        String s = prefs.getString("history", "");
        if (!s.isEmpty()) for (String x : s.split("\n")) if (!x.isEmpty()) l.add(x);
        return l;
    }

    private void addHistory(String url) {
        ArrayList<String> l = loadHistory();
        l.remove(url);
        l.add(0, url);
        while (l.size() > 20) l.remove(l.size() - 1);
        prefs.edit().putString("history", TextUtils.join("\n", l)).apply();
    }

    private void showHistory() {
        ArrayList<String> h = loadHistory();
        if (h.isEmpty()) { toast("Chưa có lịch sử"); return; }
        ArrayList<Item> items = new ArrayList<>();
        for (String u : h) { Item it = new Item(u); it.sub = "Chạm để tải lại"; it.key = u; items.add(it); }
        final Dialog[] hold = new Dialog[1];
        LinearLayout header = new LinearLayout(this);
        header.setPadding(dp(16), dp(2), dp(16), dp(8));
        TextView clear = chip("Xoá lịch sử", false);
        clear.setOnClickListener(v -> {
            prefs.edit().remove("history").apply();
            if (hold[0] != null) hold[0].dismiss();
            toast("Đã xoá lịch sử");
        });
        header.addView(clear, lp(WC, dp(38)));
        hold[0] = listSheet("Lịch sử", header, items, true, it -> { urlInput.setText(it.key); fetchPage(); }, null, null);
    }

    private void pasteUrl() {
        try {
            ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            ClipData d = cm.getPrimaryClip();
            if (d == null || d.getItemCount() == 0) { toast("Bộ nhớ tạm đang trống"); return; }
            CharSequence t = d.getItemAt(0).coerceToText(this);
            if (t == null || t.toString().trim().isEmpty()) { toast("Bộ nhớ tạm đang trống"); return; }
            urlInput.setText(t.toString().trim());
            urlInput.setSelection(urlInput.getText().length());
        } catch (Exception e) { toast("Không đọc được bộ nhớ tạm"); }
    }

    private void openExternal() {
        String u = pageUrl.isEmpty() ? urlInput.getText().toString().trim() : pageUrl;
        if (u.isEmpty()) { toast("Chưa có địa chỉ để mở"); return; }
        if (!u.matches("(?i)^https?://.*")) u = "https://" + u;
        try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(u))); }
        catch (Exception e) { toast("Không mở được trình duyệt"); }
    }

    // ================================================================== phân tích website
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
                .replaceAll("(?s)<!--.*?-->", " ")
                .replaceAll("(?i)<br\\s*/?>|</(p|div|li|h[1-6]|tr|section|article|header|footer|ul|ol|table)>", "\n")
                .replaceAll("(?s)<[^>]*>", " ")
                .replace("&nbsp;", " ").replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"")
                .replaceAll("[ \\t\\x0B\\f\\r]+", " ")
                .replaceAll(" ?\\n ?", "\n").replaceAll("\\n{3,}", "\n\n").trim();
        a.textLen = text.length();
        a.text = text.length() > 300000 ? text.substring(0, 300000) : text;

        // framework
        if (has(lo, "__next_data__", "/_next/", "id=\"__next\"")) a.frameworks.add("Next.js (React)");
        else if (has(lo, "data-reactroot", "react-dom", "react.production", "react.development")) a.frameworks.add("React");
        if (has(lo, "__nuxt", "/_nuxt/")) a.frameworks.add("Nuxt (Vue)");
        else if (has(lo, "data-v-", "vue.js", "vue.min.js", "vue.runtime", "/vue@")) a.frameworks.add("Vue");
        if (has(lo, "ng-version", "<app-root", "ng-app", "angular.js", "angular.min.js", "/angular@")) a.frameworks.add("Angular");
        if (has(lo, "__sveltekit", "/_app/immutable", "svelte-")) a.frameworks.add("Svelte / SvelteKit");
        if (has(lo, "___gatsby", "gatsby-focus-wrapper")) a.frameworks.add("Gatsby (React)");
        if (has(lo, "astro-island", "data-astro-", "/_astro/")) a.frameworks.add("Astro");
        if (has(lo, "__remixcontext")) a.frameworks.add("Remix (React)");
        // CMS / trình dựng web
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
        // thư viện
        if (has(lo, "jquery")) a.libs.add("jQuery");
        if (has(lo, "bootstrap")) a.libs.add("Bootstrap");
        if (has(lo, "tailwind")) a.libs.add("Tailwind CSS");
        if (has(lo, "alpinejs", "x-data=")) a.libs.add("Alpine.js");
        if (has(lo, "htmx")) a.libs.add("htmx");
        if (has(lo, "gsap")) a.libs.add("GSAP");
        if (has(lo, "swiper")) a.libs.add("Swiper");
        if (has(lo, "font-awesome", "fontawesome")) a.libs.add("Font Awesome");
        if (has(lo, "fonts.googleapis.com")) a.libs.add("Google Fonts");
        // dịch vụ
        if (has(lo, "google-analytics.com", "googletagmanager.com", "gtag(")) a.services.add("Google Analytics / Tag Manager");
        if (has(lo, "connect.facebook.net", "fbq(")) a.services.add("Meta Pixel");
        if (has(lo, "adsbygoogle")) a.services.add("Google AdSense");
        if (has(lo, "recaptcha")) a.services.add("reCAPTCHA");
        if (has(lo, "js.stripe.com")) a.services.add("Stripe");
        if (has(lo, "youtube.com/embed", "youtube-nocookie.com")) a.services.add("YouTube nhúng");
        if (cf || server.toLowerCase(Locale.ROOT).contains("cloudflare")) a.services.add("Cloudflare");
        if (!server.isEmpty() && !server.toLowerCase(Locale.ROOT).contains("cloudflare")) a.services.add("Máy chủ: " + server);
        if (!powered.isEmpty()) a.services.add("Powered by: " + powered);

        // kết luận
        int scripts = a.extJs + a.inlJs;
        a.pureStatic = scripts == 0 && a.frameworks.isEmpty() && a.cms.isEmpty();
        String fw = TextUtils.join(", ", a.frameworks), cm = TextUtils.join(", ", a.cms);
        if (!a.frameworks.isEmpty() && a.textLen < 400) {
            a.level = 2; a.badge = "JS RENDER";
            a.vTitle = "Ứng dụng JavaScript (SPA)";
            a.vDesc = "Xây bằng " + fw + ". HTML gốc gần như là khung rỗng — nội dung thật được tạo trong trình duyệt nên không thấy trong mã nguồn ở đây. Hãy dùng “Xem trước”.";
        } else if (!a.frameworks.isEmpty()) {
            a.level = 1; a.badge = "LAI";
            a.vTitle = "Web dùng framework (" + fw + ")";
            a.vDesc = "HTML được dựng sẵn ở máy chủ kèm JavaScript. Phần lớn nội dung vẫn thấy trong mã nguồn nhưng đây không phải HTML thuần.";
        } else if (!a.cms.isEmpty()) {
            a.level = 1; a.badge = "CMS / BUILDER";
            a.vTitle = "Làm bằng " + cm;
            a.vDesc = "Được tạo bởi nền tảng quản trị nội dung. HTML chủ yếu dựng ở máy chủ, kèm script/style của giao diện và plugin.";
        } else if (scripts == 0) {
            a.level = 0; a.badge = "THUẦN HTML";
            a.vTitle = "HTML tĩnh thuần" + (ssg ? " (sinh bởi static site generator)" : "");
            a.vDesc = "Hoàn toàn không có JavaScript — chỉ HTML" + ((a.extCss + a.inlCss) > 0 ? " và CSS." : ".");
        } else if (a.textLen < 200) {
            a.level = 2; a.badge = "JS RENDER";
            a.vTitle = "Nhiều khả năng render bằng JavaScript";
            a.vDesc = "HTML gốc có rất ít chữ nhưng lại có script, nên nội dung có thể được tạo lúc chạy.";
        } else if (a.libs.isEmpty() && scripts <= 4) {
            a.level = 0; a.badge = "TĨNH";
            a.vTitle = "HTML tĩnh kèm ít script" + (ssg ? " (sinh bởi static site generator)" : "");
            a.vDesc = "Trang HTML/CSS thông thường, chỉ có vài script nhỏ và không phát hiện framework.";
        } else {
            a.level = 1; a.badge = "LAI";
            a.vTitle = "HTML + thư viện JavaScript";
            a.vDesc = "Nội dung HTML thông thường được bổ sung bằng script và thư viện.";
        }
        a.needsJs = a.level == 2;
        return a;
    }

    // ================================================================== bottom sheet
    private LinearLayout newBody() {
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(16), dp(6), dp(16), dp(28) + inB);
        return body;
    }

    private View sheetView(String title, View content, final Dialog d, boolean scroll) {
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
        TextView tt = label(title, 18, TEXT, true);
        tt.setSingleLine(true);
        tt.setEllipsize(TextUtils.TruncateAt.END);
        head.addView(tt, lp(0, WC, 1));
        TextView close = chip("Đóng", false);
        close.setOnClickListener(v -> d.dismiss());
        head.addView(close, lp(WC, dp(36)));
        sheet.addView(head);
        if (scroll) {
            ScrollView sv = new ScrollView(this);
            sv.setVerticalScrollBarEnabled(false);
            sv.addView(content);
            sheetScroll = sv;
            sheet.addView(sv, lp(MP, 0, 1));
        } else {
            sheet.addView(content, lp(MP, 0, 1));
        }
        return sheet;
    }

    private Dialog shell() {
        Dialog d = new Dialog(this);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        Window w = d.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            w.setGravity(Gravity.BOTTOM);
            w.setLayout(MP, (int) (getResources().getDisplayMetrics().heightPixels * 0.88f));
            w.setWindowAnimations(android.R.style.Animation_InputMethod);
            w.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
        return d;
    }

    private Dialog openSheet(String title, LinearLayout body) {
        Dialog d = shell();
        d.setContentView(sheetView(title, body, d, true));
        return d;
    }

    private class ItemAdapter extends BaseAdapter {
        final ArrayList<Item> all;
        final ArrayList<Item> shown = new ArrayList<>();
        final ItemPick previewCb;
        ItemAdapter(ArrayList<Item> a, ItemPick pv) { all = a; shown.addAll(a); previewCb = pv; }
        void filter(String q) {
            shown.clear();
            String l = q.toLowerCase(Locale.ROOT).trim();
            for (Item it : all) if (l.isEmpty() || it.title.toLowerCase(Locale.ROOT).contains(l)) shown.add(it);
            notifyDataSetChanged();
        }
        @Override public int getCount() { return shown.size(); }
        @Override public Object getItem(int i) { return shown.get(i); }
        @Override public long getItemId(int i) { return i; }
        @Override public View getView(int pos, View cv, ViewGroup parent) {
            final Item it = shown.get(pos);
            LinearLayout row = new LinearLayout(MainActivity.this);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(16), dp(10), dp(12), dp(10));
            LinearLayout col = new LinearLayout(MainActivity.this);
            col.setOrientation(LinearLayout.VERTICAL);
            TextView t = label(it.title, 12.5f, TEXT, false);
            t.setTypeface(codeTf);
            t.setMaxLines(2);
            t.setEllipsize(TextUtils.TruncateAt.MIDDLE);
            col.addView(t);
            if (!it.sub.isEmpty()) col.addView(label(it.sub, 11, MUTED, false));
            row.addView(col, lp(0, WC, 1));
            if (it.preview && previewCb != null) {
                TextView pv = chip("Xem", true);
                pv.setTextSize(11.5f);
                pv.setOnClickListener(v -> previewCb.pick(it));
                row.addView(pv, lp(WC, dp(34)));
            }
            return row;
        }
    }

    /** Bảng chọn dạng danh sách (có ô lọc, nút xem trước, tìm trong nội dung). */
    private Dialog listSheet(String title, View header, ArrayList<Item> items, boolean filterable,
                             final ItemPick pick, ItemPick preview, final QueryRun deep) {
        final Dialog d = shell();
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        if (header != null) content.addView(header, lp(MP, WC));
        final ItemAdapter adapter = new ItemAdapter(items, preview);
        if (filterable) {
            LinearLayout fr = new LinearLayout(this);
            fr.setGravity(Gravity.CENTER_VERTICAL);
            fr.setBackground(rect(PANEL, STROKE, 16));
            fr.setPadding(dp(14), dp(2), dp(6), dp(2));
            final EditText et = inputBox("Lọc theo tên…", uiTf, 13);
            et.setInputType(InputType.TYPE_CLASS_TEXT);
            et.setSingleLine(true);
            et.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
                @Override public void onTextChanged(CharSequence s, int a, int b, int c) { adapter.filter(s.toString()); }
                @Override public void afterTextChanged(Editable e) { }
            });
            fr.addView(et, lp(0, dp(42), 1));
            if (deep != null) {
                TextView dc = chip("Tìm trong nội dung", true);
                dc.setTextSize(11.5f);
                dc.setOnClickListener(v -> { d.dismiss(); deep.run(et.getText().toString()); });
                fr.addView(dc, lp(WC, dp(34)));
            }
            content.addView(fr, lpm(MP, WC, 0, dp(16), 0, dp(16), dp(6)));
        }
        ListView lv = new ListView(this);
        lv.setAdapter(adapter);
        lv.setDivider(new ColorDrawable(withAlpha(MUTED, 50)));
        lv.setDividerHeight(1);
        lv.setSelector(rect(withAlpha(accent, 40), 0, 0));
        lv.setOnItemClickListener((p, v, position, id) -> {
            if (position < adapter.shown.size()) {
                Item it = adapter.shown.get(position);
                d.dismiss();
                pick.pick(it);
            }
        });
        content.addView(lv, lp(MP, 0, 1));
        d.setContentView(sheetView(title, content, d, false));
        d.show();
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
            if (rep != null) rep.append(title).append(": không phát hiện\n");
            return;
        }
        Flow f = new Flow(this, dp(8));
        for (String s : items) f.addView(tag(s));
        parent.addView(f, lp(MP, WC));
        if (rep != null) rep.append(title).append(": ").append(TextUtils.join(", ", items)).append('\n');
    }

    private static String yn(boolean b) { return b ? "Có" : "Không"; }

    // ================================================================== thông tin website
    private void showInfo() {
        boolean hasPage = !pageUrl.isEmpty();
        if (!hasPage && zipEntries == null) { toast("Hãy tải một trang web hoặc mở ZIP trước"); return; }
        LinearLayout body = newBody();
        final StringBuilder rep = new StringBuilder("Báo cáo Leon-Source web\n\n");
        Analysis a = analysis;

        if (hasPage) {
            int level; String badge, vt, vd;
            if (a != null) { level = a.level; badge = a.badge; vt = a.vTitle; vd = a.vDesc; }
            else {
                level = 1; badge = "KHÔNG PHẢI HTML";
                vt = pageKind.isEmpty() ? "Nội dung không rõ" : pageKind;
                vd = "Địa chỉ này không trả về trang HTML (" + (contentType.isEmpty() ? "không rõ loại" : contentType) + ").";
                if (pageKind.equals("JSON") || pageKind.equals("Text")) vd = "Máy chủ trả về " + pageKind + " thuần, được hiển thị nguyên văn ở khung mã.";
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

            LinearLayout glance = card(body, "TÓM TẮT NHANH");
            kv(glance, "Loại nội dung", pageKind, rep);
            if (a != null) {
                kv(glance, "HTML thuần (không JS)", yn(a.pureStatic), rep);
                kv(glance, "Cần JS mới hiện nội dung", a.needsJs ? "Có" : (a.level == 1 ? "Một phần" : "Không"), rep);
                kv(glance, "Framework", TextUtils.join(", ", a.frameworks), rep);
                kv(glance, "CMS / trình dựng web", TextUtils.join(", ", a.cms), rep);
            }

            LinearLayout ov = card(body, "TỔNG QUAN");
            if (a != null) kv(ov, "Tiêu đề", a.title, rep);
            kv(ov, "Địa chỉ", pageUrl, rep);
            kv(ov, "Mã HTTP", String.valueOf(statusCode), rep);
            kv(ov, "Content-Type", contentType, rep);
            kv(ov, "Dung lượng", fmtBytes(sizeBytes), rep);
            kv(ov, "Thời gian tải", fetchMs + " ms", rep);
            kv(ov, "Máy chủ", serverHdr, rep);
            kv(ov, "Powered by", poweredHdr, rep);
            if (a != null) {
                kv(ov, "Ngôn ngữ", a.lang, rep);
                kv(ov, "Bảng mã", a.charset, rep);
                kv(ov, "Hiển thị di động", a.viewport.isEmpty() ? "Không có viewport" : "Có  (" + a.viewport + ")", rep);
                kv(ov, "Generator", a.generator, rep);
            }

            LinearLayout sec = card(body, "BẢO MẬT & HEADER");
            kv(sec, "HTTPS", yn(pageUrl.startsWith("https://")), rep);
            kv(sec, "HSTS", headers.containsKey("Strict-Transport-Security") ? "Có" : "Không", rep);
            kv(sec, "Content-Security-Policy", headers.containsKey("Content-Security-Policy") ? "Có" : "Không", rep);
            kv(sec, "X-Frame-Options", headers.get("X-Frame-Options"), rep);
            kv(sec, "X-Content-Type-Options", headers.get("X-Content-Type-Options"), rep);
            kv(sec, "Referrer-Policy", headers.get("Referrer-Policy"), rep);
            kv(sec, "Cache-Control", headers.get("Cache-Control"), rep);
            kv(sec, "Content-Encoding", headers.get("Content-Encoding"), rep);
            kv(sec, "Cập nhật lần cuối", headers.get("Last-Modified"), rep);

            if (a != null) {
                LinearLayout st = card(body, "CẤU TRÚC");
                kv(st, "Stylesheet", a.extCss + " ngoài  •  " + a.inlCss + " nội tuyến", rep);
                kv(st, "Script", a.extJs + " ngoài  •  " + a.inlJs + " nội tuyến (" + fmtBytes(a.inlJsBytes) + ")", rep);
                kv(st, "Ảnh", String.valueOf(a.imgs), rep);
                kv(st, "Video / âm thanh", String.valueOf(a.media), rep);
                kv(st, "iFrame", String.valueOf(a.iframes), rep);
                kv(st, "Biểu mẫu (form)", String.valueOf(a.forms), rep);
                kv(st, "Liên kết", String.valueOf(a.links), rep);
                kv(st, "Số thẻ HTML", String.valueOf(a.elements), rep);
                int pct = a.htmlLen == 0 ? 0 : Math.round(100f * a.textLen / a.htmlLen);
                kv(st, "Chữ hiển thị", a.textLen + " ký tự  (" + pct + "% mã nguồn)", rep);

                LinearLayout te = card(body, "CÔNG NGHỆ PHÁT HIỆN");
                tagGroup(te, "Framework", a.frameworks, rep);
                tagGroup(te, "CMS / trình dựng", a.cms, rep);
                tagGroup(te, "Thư viện", a.libs, rep);
                tagGroup(te, "Dịch vụ & hosting", a.services, rep);

                LinearLayout seo = card(body, "SEO & META");
                kv(seo, "Mô tả", a.description, rep);
                kv(seo, "Canonical", a.canonical, rep);
                kv(seo, "Robots", a.robots, rep);
                kv(seo, "Tiêu đề Open Graph", a.ogTitle, rep);
                kv(seo, "Favicon", a.favicon, rep);
            }
        }

        if (zipEntries != null) {
            LinearLayout zc = card(body, "TỆP ZIP ĐANG MỞ");
            kv(zc, "Tên tệp", zipName, rep);
            kv(zc, "Số tệp", String.valueOf(zipEntries.size()), rep);
            kv(zc, "Tổng dung lượng", fmtBytes(zipTotal), rep);
            String best = bestIndexPath();
            kv(zc, "Trang chính", best, rep);
            kv(zc, "Loại", best != null ? "Có trang HTML — xem trước được" : "Không có trang HTML", rep);
        }

        TextView note = label("Việc phát hiện dựa trên dấu hiệu trong HTML tĩnh và header phản hồi nên có thể sót hoặc đoán sai. "
                + "Chỉ kiểm tra website bạn được phép truy cập.", 11, MUTED, false);
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

    // ================================================================== cài đặt
    private void openSettings() {
        settingsDialog = openSheet("Cài đặt", buildSettingsBody());
        settingsDialog.show();
    }

    private void renderSettings() {
        if (settingsDialog == null || !settingsDialog.isShowing()) return;
        final int keep = sheetScroll != null ? sheetScroll.getScrollY() : 0;
        settingsDialog.setContentView(sheetView("Cài đặt", buildSettingsBody(), settingsDialog, true));
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

    private LinearLayout buildSettingsBody() {
        LinearLayout body = newBody();

        // ---- xem thử
        LinearLayout pv = card(body, "XEM THỬ");
        final TextView p1 = label("Aa  Leon-Source web", 20, ACC_T, true);
        pv.addView(p1);
        pv.addView(label("Chữ giao diện trông như thế này.", 13, TEXT, false));
        prevCode = label("<div class=\"leon\">Xin chào</div>", codeSize, TEXT, false);
        prevCode.setTypeface(codeTf);
        prevCode.setPadding(0, dp(6), 0, 0);
        pv.addView(prevCode);

        // ---- màu chủ đạo
        LinearLayout ac = card(body, "MÀU CHỦ ĐẠO");
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
        TextView hl = label("Tông màu tuỳ chỉnh (kéo rồi thả)", 12, MUTED, false);
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

        // ---- nền
        LinearLayout bgc = card(body, "NỀN");
        Flow bf = new Flow(this, dp(8));
        for (int i = 0; i < BG_NAMES.length; i++) {
            final int idx = i;
            TextView ch = chip(BG_NAMES[i], i == bgMode);
            ch.setOnClickListener(v -> { bgMode = idx; applyTheme(); });
            bf.addView(ch);
        }
        bgc.addView(bf, lp(MP, WC));

        // ---- phông giao diện
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

        // ---- phông mã
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

        // ---- cỡ chữ mã
        LinearLayout cs = card(body, "CỠ CHỮ MÃ");
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

        // ---- tuỳ chọn
        LinearLayout op = card(body, "TUỲ CHỌN");
        Flow of = new Flow(this, dp(8));
        TextView hlChip = chip("Tô màu cú pháp: " + (highlightOn ? "BẬT" : "TẮT"), highlightOn);
        hlChip.setOnClickListener(v -> { highlightOn = !highlightOn; applyTheme(); });
        of.addView(hlChip);
        TextView reset = chip("Khôi phục mặc định", false);
        reset.setOnClickListener(v -> {
            accent = DEFAULT_ACCENT; bgMode = 0; uiFont = 0; codeFont = 2; codeSize = 12; highlightOn = true;
            applyTheme();
        });
        of.addView(reset);
        op.addView(of, lp(MP, WC));
        return body;
    }

    // ================================================================== khác
    private void hideKeyboard() {
        try {
            View f = getCurrentFocus();
            ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(
                    f != null ? f.getWindowToken() : urlInput.getWindowToken(), 0);
        } catch (Exception ignored) { }
    }

    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }
}
