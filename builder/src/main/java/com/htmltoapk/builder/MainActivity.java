package com.htmltoapk.builder;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Home screen with a dashed "Build your APK" card, a side menu, and a two-step bottom sheet:
 * 1) add the app source (Upload HTML/ZIP/folder, or a Website link)  2) customize, then build.
 */
public class MainActivity extends Activity {
    private static final int REQ_HTML = 1, REQ_ZIP = 2, REQ_TREE = 3, REQ_ICON = 4;
    private static final int PH_FORM = 0, PH_BUILDING = 1, PH_DONE = 2, PH_FAILED = 3;
    private static final int ERROR = 0xFFF87171;

    /** Colour palette (dark / light). */
    private static final class Pal {
        int bg, sheet, input, panel, text, muted, line, accent, accentText, accentSoft, accentOn, accentDim, segSel, scrim;
    }

    private final Pal p = new Pal();
    private final ProjectSpec spec = new ProjectSpec();
    private final Handler ui = new Handler(Looper.getMainLooper());
    private SharedPreferences prefs;
    private FrameLayout root;

    private boolean dark = true;
    private boolean sheetOpen, drawerOpen, animateSheet, animateDrawer;
    private int sheetStep = 1;
    private int phase = PH_FORM;
    private int srcType = REQ_ZIP;
    private boolean websiteMode, packageEdited, importing;
    private String sourceError = "", buildStatus = "", failSummary = "", failTrace = "";
    private File builtApk;
    private String builtName = "app.apk";

    private TextView hintView, buildStatusView;
    private Button continueBtn;

    // ================================================================== lifecycle

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("ui", MODE_PRIVATE);
        dark = prefs.getBoolean("dark", true);
        root = new FrameLayout(this);
        setContentView(root);
        resetSpec();
        applyPalette();
        render();
    }

    @Override
    public void onBackPressed() {
        if (drawerOpen) {
            drawerOpen = false;
            render();
        } else if (sheetOpen) {
            if (phase == PH_BUILDING) return;
            if (phase == PH_FAILED) {
                phase = PH_FORM;
                render();
            } else if (phase == PH_FORM && sheetStep == 2) {
                sheetStep = 1;
                render();
            } else {
                closeSheet();
            }
        } else {
            super.onBackPressed();
        }
    }

    private void resetSpec() {
        spec.appName = "";
        spec.packageName = "com.htmltoapk.app";
        spec.versionCode = 1;
        spec.versionName = "1.0";
        spec.wwwDir = null;
        spec.sourceLabel = "";
        spec.orientation = "auto";
        spec.fullscreen = false;
        spec.keepScreenOn = false;
        spec.iconPng = null;
        spec.allowInternet = false;
        spec.websiteUrl = "";
        packageEdited = false;
        websiteMode = false;
        srcType = REQ_ZIP;
        sourceError = "";
        importing = false;
        sheetStep = 1;
        phase = PH_FORM;
    }

    private void applyPalette() {
        if (dark) {
            p.bg = 0xFF111111;          // --k-bg
            p.sheet = 0xFF262626;       // --k-inner
            p.input = 0xFF1C1C1C;       // --k-card
            p.panel = 0xFF1C1C1C;
            p.text = 0xFFF5F5F5;
            p.muted = 0xFF8E8E8E;       // --k-dim
            p.line = 0xFF2A2A2A;
            p.accent = 0xFF4CD1A2;      // mint
            p.accentText = 0xFF4CD1A2;
            p.accentSoft = 0x294CD1A2;  // mint at 16%
            p.accentOn = 0xFF0A2A1F;
            p.accentDim = 0xFF3F7A6B;
            p.segSel = 0xFF333333;      // --k-high
            p.scrim = 0x99000000;
        } else {
            p.bg = 0xFFF2F2F7;
            p.sheet = 0xFFF0F0F5;
            p.input = 0xFFFFFFFF;
            p.panel = 0xFFFFFFFF;
            p.text = 0xFF111114;
            p.muted = 0xFF6C6C73;
            p.line = 0xFFE0E0E6;
            p.accent = 0xFF4CD1A2;
            p.accentText = 0xFF0B8A6D;
            p.accentSoft = 0x294CD1A2;
            p.accentOn = 0xFF0A2A1F;
            p.accentDim = 0xFFA6E3CD;
            p.segSel = 0xFFE4E4EA;
            p.scrim = 0x66000000;
        }
        getWindow().setStatusBarColor(p.bg);
        getWindow().setNavigationBarColor(p.bg);
        int flags = dark ? 0 : (View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        getWindow().getDecorView().setSystemUiVisibility(flags);
        root.setBackgroundColor(p.bg);
    }

    private void toggleTheme() {
        dark = !dark;
        prefs.edit().putBoolean("dark", dark).apply();
        applyPalette();
        render();
    }

    private void render() {
        root.removeAllViews();
        root.addView(buildHome(), match());
        if (sheetOpen) root.addView(buildSheet(), match());
        if (drawerOpen) root.addView(buildDrawer(), match());
    }

    // ================================================================== home

    private View buildHome() {
        LinearLayout page = vertical();
        page.setPadding(0, dp(24), 0, 0);

        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(8), 0, dp(8), 0);
        bar.addView(iconButton("☰", v -> {
            drawerOpen = true;
            animateDrawer = true;
            render();
        }), new LinearLayout.LayoutParams(dp(48), dp(56)));
        bar.addView(iconButton(dark ? "☀" : "☾", v -> toggleTheme()), new LinearLayout.LayoutParams(dp(48), dp(56)));
        TextView title = text("WZH 2 APK", 22, p.muted, true);
        title.setGravity(Gravity.CENTER);
        bar.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        bar.addView(iconButton("⋯", v -> showInfo("About",
                "WZH 2 APK builds an installable Android app from your website, right on your phone. "
                        + "No server, no account.")), new LinearLayout.LayoutParams(dp(48), dp(56)));
        bar.addView(new View(this), new LinearLayout.LayoutParams(dp(48), dp(56)));
        page.addView(bar);
        View divider = new View(this);
        divider.setBackgroundColor(p.line);
        page.addView(divider, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));

        // "Build your APK" card: dashed border, mint plus
        LinearLayout card = new LinearLayout(this);
        card.setGravity(Gravity.CENTER);
        GradientDrawable dash = rounded(p.panel, 0, 24);
        dash.setStroke(dp(2), dark ? 0xFFBDBDBD : 0xFF9A9AA0, dp(8), dp(6));
        card.setBackground(dash);
        TextView plus = text("+", 26, p.accentOn, false);
        plus.setGravity(Gravity.CENTER);
        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(p.accent);
        plus.setBackground(circle);
        card.addView(plus, new LinearLayout.LayoutParams(dp(40), dp(40)));
        TextView label = textMedium("Build your APK", 22, p.text);
        label.setPadding(dp(18), 0, 0, 0);
        card.addView(label);
        card.setOnClickListener(v -> {
            resetSpec();
            sheetOpen = true;
            animateSheet = true;
            render();
        });
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(88));
        clp.setMargins(dp(22), dp(24), dp(22), 0);
        page.addView(card, clp);

        // "Recent builds" header
        final JSONArray hist = loadHistory();
        LinearLayout head = new LinearLayout(this);
        head.setGravity(Gravity.CENTER_VERTICAL);
        TextView ht = text("Recent builds", 15, p.text, true);
        head.addView(ht, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        if (hist.length() > 0) {
            TextView clear = textMedium("Clear", 14, p.accentText);
            clear.setPadding(dp(8), dp(6), 0, dp(6));
            clear.setOnClickListener(v -> dialogBuilder().setTitle("Clear recent builds?")
                    .setMessage("This deletes the saved APK files from this app. Apps you already installed stay.")
                    .setPositiveButton("Clear", (d, w) -> {
                        clearHistory();
                        render();
                    }).setNegativeButton("Cancel", null).show());
            head.addView(clear);
        }
        LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hlp.setMargins(dp(24), dp(30), dp(24), dp(10));
        page.addView(head, hlp);

        ScrollView listScroll = new ScrollView(this);
        LinearLayout list = vertical();
        list.setPadding(dp(22), 0, dp(22), dp(24));
        listScroll.addView(list);
        if (hist.length() == 0) {
            LinearLayout empty = vertical();
            empty.setGravity(Gravity.CENTER_HORIZONTAL);
            empty.setPadding(dp(16), dp(24), dp(16), dp(24));
            GradientDrawable ebg = rounded(p.panel, 0, 16);
            ebg.setStroke(1, p.line);
            empty.setBackground(ebg);
            TextView ic = text("\u25A4", 22, p.muted, false);
            ic.setGravity(Gravity.CENTER);
            ic.setBackground(rounded(p.sheet, 0, 12));
            empty.addView(ic, new LinearLayout.LayoutParams(dp(44), dp(44)));
            empty.addView(space(10));
            TextView et = text("No apps built yet. Tap Build your APK to start.", 14, p.muted, false);
            et.setGravity(Gravity.CENTER);
            empty.addView(et);
            list.addView(empty);
        } else {
            for (int i = 0; i < hist.length(); i++) {
                final JSONObject o = hist.optJSONObject(i);
                if (o != null) list.addView(historyCard(o));
            }
        }
        page.addView(listScroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        return page;
    }

    // ---------------------------------------------------------------- recent builds (history)

    private View historyCard(final JSONObject o) {
        LinearLayout card = new LinearLayout(this);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(14), dp(14), dp(16), dp(14));
        card.setBackground(rounded(p.panel, 0, 16));
        String name = o.optString("name", "App");
        TextView ic = text(name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase(Locale.getDefault()), 20, p.accentText, true);
        ic.setGravity(Gravity.CENTER);
        ic.setBackground(rounded(p.accentSoft, 0, 12));
        card.addView(ic, new LinearLayout.LayoutParams(dp(44), dp(44)));
        LinearLayout col = vertical();
        col.setPadding(dp(14), 0, dp(8), 0);
        TextView nm = textMedium(name, 17, p.text);
        nm.setSingleLine(true);
        nm.setEllipsize(TextUtils.TruncateAt.END);
        col.addView(nm);
        String when = new java.text.SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(new java.util.Date(o.optLong("time")));
        TextView sub = text(o.optString("pkg") + " \u00b7 v" + o.optString("ver") + " \u00b7 "
                + formatSize(o.optLong("size")) + " \u00b7 " + when, 12, p.muted, false);
        sub.setSingleLine(true);
        sub.setEllipsize(TextUtils.TruncateAt.END);
        col.addView(sub);
        card.addView(col, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        card.addView(text("\u203A", 26, p.muted, false));
        card.setOnClickListener(v -> openHistoryItem(o));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(10);
        card.setLayoutParams(lp);
        return card;
    }

    private JSONArray loadHistory() {
        try {
            return new JSONArray(prefs.getString("history", "[]"));
        } catch (Exception e) {
            return new JSONArray();
        }
    }

    private File buildsDir() {
        return new File(getFilesDir(), "builds");
    }

    /** Copies a fresh APK into private storage and records it. Returns the stored copy. */
    private File addHistory(File apk) {
        try {
            File dir = buildsDir();
            if (!dir.exists() && !dir.mkdirs()) return apk;
            String id = String.valueOf(System.currentTimeMillis());
            File dest = new File(dir, id + ".apk");
            ApkBuilder.copy(new FileInputStream(apk), new java.io.FileOutputStream(dest));
            JSONObject o = new JSONObject();
            o.put("id", id);
            o.put("name", spec.appName);
            o.put("pkg", spec.packageName);
            o.put("ver", spec.versionName);
            o.put("size", dest.length());
            o.put("time", System.currentTimeMillis());
            o.put("file", apk.getName());
            JSONArray old = loadHistory();
            JSONArray fresh = new JSONArray();
            fresh.put(o);
            for (int i = 0; i < old.length() && i < 29; i++) fresh.put(old.get(i));
            prefs.edit().putString("history", fresh.toString()).apply();
            return dest;
        } catch (Exception e) {
            return apk;
        }
    }

    private void removeHistory(String id) {
        try {
            JSONArray old = loadHistory();
            JSONArray fresh = new JSONArray();
            for (int i = 0; i < old.length(); i++) {
                JSONObject o = old.getJSONObject(i);
                if (!id.equals(o.optString("id"))) fresh.put(o);
            }
            prefs.edit().putString("history", fresh.toString()).apply();
        } catch (Exception ignored) {
        }
    }

    private void clearHistory() {
        File[] files = buildsDir().listFiles();
        if (files != null) for (File f : files) f.delete();
        prefs.edit().remove("history").apply();
    }

    private void openHistoryItem(final JSONObject o) {
        final String id = o.optString("id");
        final File f = new File(buildsDir(), id + ".apk");
        final String fileName = o.optString("file", "app.apk");
        if (!f.exists()) {
            toast("That APK file is gone");
            removeHistory(id);
            render();
            return;
        }
        dialogBuilder().setTitle(o.optString("name"))
                .setItems(new String[]{"Install", "Save to Downloads", "Delete"}, (d, which) -> {
                    if (which == 0) install(f);
                    else if (which == 1) saveToDownloads(f, fileName);
                    else {
                        f.delete();
                        removeHistory(id);
                        render();
                    }
                }).show();
    }

    // ================================================================== drawer

    private View buildDrawer() {
        FrameLayout layer = new FrameLayout(this);
        View scrim = new View(this);
        scrim.setBackgroundColor(p.scrim);
        scrim.setOnClickListener(v -> {
            drawerOpen = false;
            render();
        });
        layer.addView(scrim, match());

        int w = (int) (getResources().getDisplayMetrics().widthPixels * 0.86f);
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(p.bg);
        LinearLayout col = vertical();
        col.setPadding(dp(14), dp(56), dp(14), dp(32));
        scroll.addView(col);

        col.addView(sectionLabel("APPEARANCE"));
        LinearLayout g1 = group();
        g1.addView(menuRow("◐", "Appearance", dark ? "Dark" : "Light", v -> toggleTheme(), true));
        col.addView(g1);

        col.addView(sectionLabel("GUIDE"));
        LinearLayout g2 = group();
        g2.addView(menuRow("?", "How it works", "", v -> showInfo("How it works",
                "1. Add your site — upload an HTML file, a ZIP, a folder, or paste a website link.\n\n"
                        + "2. Customize — name, package, version, icon, orientation.\n\n"
                        + "3. Build — your files are packed into an offline Android app shell, "
                        + "signed on your phone, and ready to install.\n\n"
                        + "Everything happens on this device."), true));
        g2.addView(menuRow("✦", "Features", "", v -> showInfo("Features",
                "• Works offline (upload mode)\n• HTML, ZIP, folder or live website link\n"
                        + "• Custom app icon\n• Orientation, fullscreen, keep screen on\n"
                        + "• Internet permission on/off\n• Install directly or save to Downloads\n"
                        + "• No server and no account"), false));
        g2.addView(menuRow("Q", "FAQ", "", v -> showInfo("FAQ",
                "Does the app need internet?\nNot in Upload mode with Internet off. Everything is inside the APK.\n\n"
                        + "Why do my fonts or scripts not load?\nOnline links (CDNs, Google Fonts) need internet. "
                        + "Download those files into your project, or turn Internet on.\n\n"
                        + "How do I update an app?\nBuild again with the same package name and a higher version code.\n\n"
                        + "Can I publish it on Google Play?\nApps are signed with a bundled key meant for personal use. "
                        + "Google Play needs your own release key."), false));
        g2.addView(menuRow("✓", "Tips & troubleshooting", "", v -> showInfo("Tips & troubleshooting",
                "• Your project needs an index.html at the top.\n"
                        + "• Use relative paths (css/style.css), keep all files inside the project.\n"
                        + "• \"App not installed\"? Uninstall the old version, or raise the version code.\n"
                        + "• Installer blocked? Allow \"Install unknown apps\" for this app when asked.\n"
                        + "• Blank page? Open your index.html in a browser first to check it works."), false));
        col.addView(g2);

        layer.addView(scroll, new FrameLayout.LayoutParams(w, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.START));
        if (animateDrawer) {
            animateDrawer = false;
            scroll.setTranslationX(-w);
            scroll.animate().translationX(0).setDuration(220).start();
            scrim.setAlpha(0f);
            scrim.animate().alpha(1f).setDuration(220).start();
        }
        return layer;
    }

    private LinearLayout group() {
        LinearLayout g = vertical();
        g.setBackground(rounded(p.panel, 0, 24));
        return g;
    }

    private TextView sectionLabel(String s) {
        TextView t = text(s, 15, p.muted, false);
        t.setLetterSpacing(0.06f);
        t.setPadding(dp(8), dp(24), 0, dp(10));
        return t;
    }

    private View menuRow(String glyph, String title, String value, View.OnClickListener l, boolean first) {
        LinearLayout wrap = vertical();
        if (!first) {
            View div = new View(this);
            div.setBackgroundColor(p.line);
            LinearLayout.LayoutParams dl = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1);
            dl.leftMargin = dp(74);
            wrap.addView(div, dl);
        }
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), dp(14), dp(18), dp(14));
        TextView ic = text(glyph, 20, p.accentText, true);
        ic.setGravity(Gravity.CENTER);
        ic.setBackground(rounded(p.sheet, 0, 12));
        row.addView(ic, new LinearLayout.LayoutParams(dp(44), dp(44)));
        TextView t = text(title, 19, p.text, false);
        t.setPadding(dp(16), 0, 0, 0);
        row.addView(t, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        if (!value.isEmpty()) row.addView(text(value, 17, p.muted, false));
        TextView chev = text("›", 26, p.muted, false);
        chev.setPadding(dp(14), 0, 0, 0);
        row.addView(chev);
        row.setOnClickListener(l);
        wrap.addView(row);
        return wrap;
    }

    // ================================================================== bottom sheet

    private View buildSheet() {
        FrameLayout layer = new FrameLayout(this);
        View scrim = new View(this);
        scrim.setBackgroundColor(p.scrim);
        scrim.setOnClickListener(v -> {
            if (phase == PH_FORM) closeSheet();
        });
        layer.addView(scrim, match());

        int h = (int) (getResources().getDisplayMetrics().heightPixels * 0.82f);
        LinearLayout sheet = vertical();
        GradientDrawable bg = rounded(p.sheet, 0, 0);
        float r = dp(36);
        bg.setCornerRadii(new float[]{r, r, r, r, 0, 0, 0, 0});
        sheet.setBackground(bg);
        layer.addView(sheet, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, h, Gravity.BOTTOM));

        hintView = null;
        continueBtn = null;
        buildStatusView = null;

        if (phase == PH_FORM) fillForm(sheet);
        else if (phase == PH_BUILDING) fillBuilding(sheet);
        else if (phase == PH_DONE) fillDone(sheet);
        else fillFailed(sheet);

        if (animateSheet) {
            animateSheet = false;
            sheet.setTranslationY(h);
            sheet.animate().translationY(0).setDuration(240).start();
            scrim.setAlpha(0f);
            scrim.animate().alpha(1f).setDuration(240).start();
        }
        return layer;
    }

    private void closeSheet() {
        sheetOpen = false;
        render();
    }

    private View sheetHeader(String glyph, View.OnClickListener onGlyph, String title, String sub) {
        LinearLayout h = new LinearLayout(this);
        h.setGravity(Gravity.CENTER_VERTICAL);
        h.setPadding(dp(24), dp(28), dp(24), dp(14));
        if (glyph != null) {
            TextView g = text(glyph, 26, p.text, false);
            g.setPadding(dp(4), dp(4), dp(20), dp(4));
            g.setOnClickListener(onGlyph);
            h.addView(g);
        }
        LinearLayout t = vertical();
        t.addView(text(title, 30, p.text, true));
        t.addView(text(sub, 15, p.muted, false));
        h.addView(t, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        return h;
    }

    // ---------------------------------------------------------------- form (steps 1 and 2)

    private void fillForm(LinearLayout sheet) {
        if (sheetStep == 1) {
            sheet.addView(sheetHeader("✕", v -> closeSheet(), "Create", "Step 1 of 2 · Add your app source"));
        } else {
            sheet.addView(sheetHeader("←", v -> {
                sheetStep = 1;
                render();
            }, "Create", "Step 2 of 2 · Customize your app"));
        }

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout body = vertical();
        body.setPadding(dp(24), dp(4), dp(24), dp(16));
        scroll.addView(body);
        sheet.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        if (sheetStep == 1) fillStep1(body);
        else fillStep2(body);

        LinearLayout footer = vertical();
        footer.setPadding(dp(24), dp(8), dp(24), dp(16));
        hintView = text("", 15, p.muted, false);
        hintView.setGravity(Gravity.CENTER);
        footer.addView(hintView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(28)));
        continueBtn = new Button(this);
        continueBtn.setAllCaps(false);
        continueBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        continueBtn.setTypeface(Typeface.DEFAULT_BOLD);
        continueBtn.setText(sheetStep == 1 ? "Continue" : "Build APK");
        continueBtn.setOnClickListener(v -> onContinue());
        footer.addView(continueBtn, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(60)));
        sheet.addView(footer);
        updateFooter();
    }

    private void fillStep1(LinearLayout body) {
        // Upload | Website segmented control
        LinearLayout seg = new LinearLayout(this);
        seg.setBackground(rounded(p.input, 0, 16));
        seg.setPadding(dp(5), dp(5), dp(5), dp(5));
        seg.addView(segButton("Upload", !websiteMode, v -> {
            websiteMode = false;
            render();
        }), new LinearLayout.LayoutParams(0, dp(48), 1f));
        seg.addView(segButton("Website", websiteMode, v -> {
            websiteMode = true;
            render();
        }), new LinearLayout.LayoutParams(0, dp(48), 1f));
        body.addView(seg, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        body.addView(space(16));

        // App name with clear button
        LinearLayout nameBox = new LinearLayout(this);
        nameBox.setGravity(Gravity.CENTER_VERTICAL);
        nameBox.setBackground(rounded(p.input, 0, 16));
        nameBox.setPadding(dp(22), 0, dp(16), 0);
        final EditText et = new EditText(this);
        et.setBackground(null);
        et.setHint("App name");
        et.setHintTextColor(p.muted);
        et.setTextColor(p.text);
        et.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        et.setSingleLine(true);
        et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        et.setText(spec.appName);
        final TextView clear = text("✕", 14, p.sheet, true);
        clear.setGravity(Gravity.CENTER);
        GradientDrawable cc = new GradientDrawable();
        cc.setShape(GradientDrawable.OVAL);
        cc.setColor(p.muted);
        clear.setBackground(cc);
        clear.setVisibility(spec.appName.isEmpty() ? View.INVISIBLE : View.VISIBLE);
        clear.setOnClickListener(v -> et.setText(""));
        nameBox.addView(et, new LinearLayout.LayoutParams(0, dp(64), 1f));
        nameBox.addView(clear, new LinearLayout.LayoutParams(dp(28), dp(28)));
        et.addTextChangedListener(watcher(s -> {
            spec.appName = s.trim();
            clear.setVisibility(s.isEmpty() ? View.INVISIBLE : View.VISIBLE);
            updateFooter();
        }));
        body.addView(nameBox, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        body.addView(space(20));

        if (!websiteMode) {
            LinearLayout tiles = new LinearLayout(this);
            tiles.addView(tile("HTML", "One .html file", REQ_HTML), tileParams(true));
            tiles.addView(tile("ZIP", "Zipped project", REQ_ZIP), tileParams(true));
            tiles.addView(tile("Folder", "Project folder", REQ_TREE), tileParams(false));
            body.addView(tiles, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(64)));
            body.addView(space(20));
        }

        body.addView(labelRow("Platform", "What to build", staticBox("Android (APK)"), true));
        body.addView(space(14));

        if (websiteMode) {
            final EditText link = new EditText(this);
            link.setSingleLine(true);
            link.setHint("https://yoursite.com");
            link.setHintTextColor(p.muted);
            link.setTextColor(p.text);
            link.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
            link.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
            link.setText(spec.websiteUrl);
            link.setBackground(rounded(p.input, 0, 18));
            link.setPadding(dp(18), 0, dp(18), 0);
            link.addTextChangedListener(watcher(s -> {
                spec.websiteUrl = s.trim();
                updateFooter();
            }));
            body.addView(labelRow("Link", "Loads live in the app", link, true));
            body.addView(space(14));
            body.addView(text("Your app opens this link live, so changes on your site show up automatically. "
                    + "The app needs internet in this mode.", 15, p.muted, false));
        } else {
            String shown;
            if (importing) shown = "Importing…";
            else if (spec.wwwDir != null) shown = spec.sourceLabel;
            else shown = "Choose " + (srcType == REQ_TREE ? "folder" : srcType == REQ_HTML ? "HTML" : "ZIP");
            LinearLayout fileBox = new LinearLayout(this);
            fileBox.setGravity(Gravity.CENTER_VERTICAL);
            fileBox.setBackground(rounded(p.input, 0, 18));
            fileBox.setPadding(dp(18), 0, dp(14), 0);
            TextView name = text(shown, 18, spec.wwwDir != null ? p.text : p.muted, false);
            name.setSingleLine(true);
            name.setEllipsize(TextUtils.TruncateAt.MIDDLE);
            fileBox.addView(name, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            fileBox.addView(text("⬆", 20, p.text, false));
            fileBox.setOnClickListener(v -> pick(srcType));
            String sub = srcType == REQ_TREE ? "Your project folder" : srcType == REQ_HTML ? "Your .html file" : "Your .zip project";
            body.addView(labelRow("File", sub, fileBox, true));
            if (!sourceError.isEmpty()) {
                body.addView(space(8));
                body.addView(text(sourceError, 14, ERROR, false));
            } else if (spec.wwwDir != null) {
                body.addView(space(8));
                body.addView(text("✓ Found index.html", 14, p.accentText, false));
            }
            body.addView(space(14));
            TextView how = text("How to prep your file", 18, p.muted, false);
            how.setPaintFlags(how.getPaintFlags() | android.graphics.Paint.UNDERLINE_TEXT_FLAG);
            how.setOnClickListener(v -> showInfo("How to prep your file",
                    "• Your project needs an index.html at the top.\n"
                            + "• Put every file it uses (CSS, JS, images, fonts) inside the project.\n"
                            + "• Use relative paths like css/style.css.\n"
                            + "• Online links (CDNs, Google Fonts, APIs) won't load offline.\n"
                            + "• For a ZIP: select the files and zip them, or zip the project folder."));
            body.addView(how);
        }
    }

    private void fillStep2(LinearLayout body) {
        // Package name
        EditText pkg = rowField(spec.packageName, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS, 15);
        pkg.addTextChangedListener(watcher(s -> {
            spec.packageName = s.trim();
            if (pkg.hasFocus()) packageEdited = true;
            updateFooter();
        }));
        body.addView(labelRow("Package", "Unique app ID", pkg, true));
        body.addView(space(14));

        EditText vc = rowField(String.valueOf(spec.versionCode), InputType.TYPE_CLASS_NUMBER, 18);
        vc.addTextChangedListener(watcher(s -> {
            try {
                spec.versionCode = Integer.parseInt(s.trim());
            } catch (NumberFormatException e) {
                spec.versionCode = 0;
            }
            updateFooter();
        }));
        body.addView(labelRow("Version code", "Raise it each update", vc, true));
        body.addView(space(14));

        EditText vn = rowField(spec.versionName, InputType.TYPE_CLASS_TEXT, 18);
        vn.addTextChangedListener(watcher(s -> {
            spec.versionName = s.trim();
            updateFooter();
        }));
        body.addView(labelRow("Version name", "Shown to users", vn, true));
        body.addView(space(14));

        // Icon
        ImageView icon = new ImageView(this);
        icon.setBackground(rounded(p.input, 0, 16));
        icon.setPadding(dp(6), dp(6), dp(6), dp(6));
        if (spec.iconPng != null) {
            icon.setImageBitmap(BitmapFactory.decodeByteArray(spec.iconPng, 0, spec.iconPng.length));
        } else {
            icon.setImageDrawable(rounded(p.accent, 0, 12));
        }
        icon.setOnClickListener(v -> pick(REQ_ICON));
        LinearLayout iconWrap = new LinearLayout(this);
        iconWrap.setGravity(Gravity.CENTER_VERTICAL);
        iconWrap.addView(icon, new LinearLayout.LayoutParams(dp(64), dp(64)));
        TextView change = text(spec.iconPng != null ? "Reset" : "Change", 16, p.accentText, true);
        change.setPadding(dp(16), dp(12), 0, dp(12));
        change.setOnClickListener(v -> {
            if (spec.iconPng != null) {
                spec.iconPng = null;
                render();
            } else {
                pick(REQ_ICON);
            }
        });
        iconWrap.addView(change);
        body.addView(labelRow("Icon", "App icon (square)", iconWrap, true));
        body.addView(space(14));

        // Orientation dropdown
        final String[] vals = {"auto", "portrait", "landscape"};
        final String[] labels = {"Auto", "Portrait", "Landscape"};
        int cur = 0;
        for (int i = 0; i < vals.length; i++) if (vals[i].equals(spec.orientation)) cur = i;
        LinearLayout dd = new LinearLayout(this);
        dd.setGravity(Gravity.CENTER_VERTICAL);
        dd.setBackground(rounded(p.input, 0, 18));
        dd.setPadding(dp(18), 0, dp(14), 0);
        dd.addView(text(labels[cur], 18, p.text, false), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        dd.addView(text("⌵", 22, p.text, false));
        dd.setOnClickListener(v -> dialogBuilder().setTitle("Orientation")
                .setItems(labels, (d, which) -> {
                    spec.orientation = vals[which];
                    render();
                }).show());
        body.addView(labelRow("Orientation", "How the app rotates", dd, true));
        body.addView(space(14));

        body.addView(switchRow("Fullscreen", "Hide the status bar", spec.fullscreen, b -> spec.fullscreen = b));
        body.addView(space(10));
        body.addView(switchRow("Keep screen on", "Screen stays awake", spec.keepScreenOn, b -> spec.keepScreenOn = b));
        if (!websiteMode) {
            body.addView(space(10));
            body.addView(switchRow("Internet", "Off = fully offline app", spec.allowInternet, b -> spec.allowInternet = b));
        }
    }

    private void onContinue() {
        if (sheetStep == 1) {
            if (!step1Hint().isEmpty()) return;
            if (!packageEdited) spec.packageName = ProjectSpec.suggestPackage(spec.appName);
            sheetStep = 2;
            render();
        } else {
            if (!step2Hint().isEmpty()) return;
            startBuild();
        }
    }

    private String step1Hint() {
        boolean nameOk = !spec.appName.isEmpty();
        if (websiteMode) {
            String u = spec.websiteUrl;
            if (!(u.startsWith("https://") || u.startsWith("http://")) || u.length() < 11) return "Enter your website link to continue";
            if (!nameOk) return "Add a project name to continue";
        } else {
            if (!nameOk) return "Add a project name to continue";
            if (importing) return "Importing your file…";
            if (!SourceImporter.hasIndex(spec.wwwDir)) return "Choose your file to continue";
        }
        return "";
    }

    private String step2Hint() {
        if (!ProjectSpec.isValidPackage(spec.packageName)) return "Package must look like com.name.app";
        if (spec.versionCode < 1) return "Version code must be 1 or higher";
        if (spec.versionName.isEmpty()) return "Enter a version name";
        return "";
    }

    private void updateFooter() {
        if (continueBtn == null || hintView == null) return;
        String hint = sheetStep == 1 ? step1Hint() : step2Hint();
        boolean ready = hint.isEmpty();
        hintView.setText(hint);
        continueBtn.setEnabled(ready);
        continueBtn.setTextColor(p.accentOn);
        continueBtn.setBackground(rounded(ready ? p.accent : p.accentDim, 0, 30));
        continueBtn.setAlpha(ready ? 1f : 0.85f);
    }

    // ---------------------------------------------------------------- build / done / failed

    private void startBuild() {
        phase = PH_BUILDING;
        buildStatus = "Starting…";
        if (!websiteMode) spec.websiteUrl = "";
        render();
        new Thread(() -> {
            try {
                File apk = ApkBuilder.build(this, spec, msg -> ui.post(() -> {
                    buildStatus = msg + "…";
                    if (buildStatusView != null) buildStatusView.setText(buildStatus);
                }));
                ui.post(() -> {
                    builtName = apk.getName();
                    builtApk = addHistory(apk);
                    phase = PH_DONE;
                    render();
                });
            } catch (Throwable t) {
                StringWriter sw = new StringWriter();
                t.printStackTrace(new PrintWriter(sw));
                ui.post(() -> {
                    failSummary = t.getClass().getSimpleName() + ": " + t.getMessage();
                    failTrace = sw.toString();
                    phase = PH_FAILED;
                    render();
                });
            }
        }).start();
    }

    private void fillBuilding(LinearLayout sheet) {
        sheet.addView(sheetHeader(null, null, "Building", "Please keep the app open"));
        LinearLayout c = vertical();
        c.setGravity(Gravity.CENTER);
        ProgressBar pb = new ProgressBar(this);
        pb.setIndeterminateTintList(ColorStateList.valueOf(p.accent));
        c.addView(pb, new LinearLayout.LayoutParams(dp(56), dp(56)));
        c.addView(space(20));
        buildStatusView = text(buildStatus, 18, p.text, false);
        c.addView(buildStatusView);
        sheet.addView(c, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
    }

    private void fillDone(LinearLayout sheet) {
        sheet.addView(sheetHeader("✕", v -> closeSheet(), "Your APK is ready", "Install it or save it for later"));
        LinearLayout body = vertical();
        body.setPadding(dp(24), dp(8), dp(24), dp(16));
        body.addView(text(builtName + " · " + formatSize(builtApk.length()), 17, p.muted, false));
        body.addView(space(24));
        body.addView(bigButton("Install", true, v -> install(builtApk)));
        body.addView(space(12));
        body.addView(bigButton("Save to Downloads", false, v -> saveToDownloads(builtApk, builtName)));
        body.addView(space(12));
        body.addView(bigButton("Build another", false, v -> {
            resetSpec();
            animateSheet = false;
            render();
        }));
        sheet.addView(body);
    }

    private void fillFailed(LinearLayout sheet) {
        sheet.addView(sheetHeader("←", v -> {
            phase = PH_FORM;
            render();
        }, "Build failed", "Details below"));
        LinearLayout body = vertical();
        body.setPadding(dp(24), dp(4), dp(24), dp(16));
        body.addView(text(failSummary, 16, ERROR, false));
        body.addView(space(12));
        ScrollView sv = new ScrollView(this);
        TextView tv = text(failTrace, 11, p.muted, false);
        tv.setTypeface(Typeface.MONOSPACE);
        tv.setTextIsSelectable(true);
        sv.addView(tv);
        body.addView(sv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        body.addView(space(12));
        body.addView(bigButton("Back", false, v -> {
            phase = PH_FORM;
            render();
        }));
        sheet.addView(body, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
    }

    private void install(File apk) {
        if (!getPackageManager().canRequestPackageInstalls()) {
            toast("Allow \"Install unknown apps\" for this app, then tap Install again");
            startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:" + getPackageName())));
            return;
        }
        try {
            Uri u = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", apk);
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setDataAndType(u, "application/vnd.android.package-archive");
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        } catch (Exception e) {
            toast("Couldn't start the installer: " + e.getMessage());
        }
    }

    private void saveToDownloads(File apk, String displayName) {
        try {
            ContentValues cv = new ContentValues();
            cv.put(MediaStore.Downloads.DISPLAY_NAME, displayName);
            cv.put(MediaStore.Downloads.MIME_TYPE, "application/vnd.android.package-archive");
            cv.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
            Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv);
            if (uri == null) throw new Exception("Could not create file");
            try (OutputStream out = getContentResolver().openOutputStream(uri);
                 InputStream in = new FileInputStream(apk)) {
                byte[] buf = new byte[16384];
                int n;
                while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            }
            toast("Saved to Downloads/" + displayName);
        } catch (Exception e) {
            toast("Save failed: " + e.getMessage());
        }
    }

    // ================================================================== file picking

    private void pick(int req) {
        Intent i;
        if (req == REQ_TREE) {
            i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        } else {
            i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType(req == REQ_ICON ? "image/*" : "*/*");
        }
        startActivityForResult(i, req);
    }

    @Override
    protected void onActivityResult(int req, int resultCode, Intent data) {
        super.onActivityResult(req, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
        final Uri uri = data.getData();
        if (req == REQ_ICON) {
            loadIcon(uri);
            return;
        }
        importing = true;
        sourceError = "";
        spec.wwwDir = null;
        render();
        final String label = req == REQ_TREE ? "Folder" : SourceImporter.displayName(this, uri);
        new Thread(() -> {
            try {
                SourceImporter.Result r;
                if (req == REQ_HTML) r = SourceImporter.importHtmlFile(this, uri);
                else if (req == REQ_ZIP) r = SourceImporter.importZip(this, uri);
                else r = SourceImporter.importTree(this, uri);
                final SourceImporter.Result res = r;
                ui.post(() -> {
                    importing = false;
                    if (!SourceImporter.hasIndex(res.root)) {
                        spec.wwwDir = null;
                        sourceError = "No index.html found in that " + (req == REQ_TREE ? "folder" : "file") + ".";
                    } else {
                        spec.wwwDir = res.root;
                        spec.sourceLabel = label;
                        sourceError = "";
                    }
                    render();
                });
            } catch (Exception e) {
                ui.post(() -> {
                    importing = false;
                    sourceError = "Import failed: " + e.getMessage();
                    render();
                });
            }
        }).start();
    }

    private void loadIcon(Uri uri) {
        try (InputStream in = getContentResolver().openInputStream(uri)) {
            Bitmap src = BitmapFactory.decodeStream(in);
            if (src == null) throw new Exception("Not an image");
            int side = Math.min(src.getWidth(), src.getHeight());
            Bitmap square = Bitmap.createBitmap(src, (src.getWidth() - side) / 2, (src.getHeight() - side) / 2, side, side);
            Bitmap icon = Bitmap.createScaledBitmap(square, 192, 192, true);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            icon.compress(Bitmap.CompressFormat.PNG, 100, bos);
            spec.iconPng = bos.toByteArray();
            render();
        } catch (Exception e) {
            toast("Couldn't load that image: " + e.getMessage());
        }
    }

    // ================================================================== view helpers

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private FrameLayout.LayoutParams match() {
        return new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    }

    private LinearLayout vertical() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    private TextView text(String s, int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    private View space(int dpHeight) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(1, dp(dpHeight)));
        return v;
    }

    private GradientDrawable rounded(int fill, int stroke, int radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(dp(radiusDp));
        if (stroke != 0) g.setStroke(dp(1), stroke);
        return g;
    }

    private TextView textMedium(String s, int sp, int color) {
        TextView t = text(s, sp, color, false);
        t.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        return t;
    }

    private TextView iconButton(String glyph, View.OnClickListener l) {
        TextView t = text(glyph, 24, p.muted, false);
        t.setGravity(Gravity.CENTER);
        t.setOnClickListener(l);
        return t;
    }

    private TextView segButton(String label, boolean selected, View.OnClickListener l) {
        TextView t = text(label, 16, selected ? p.accentText : p.text, true);
        t.setGravity(Gravity.CENTER);
        if (selected) t.setBackground(rounded(p.segSel, 0, 13));
        t.setOnClickListener(l);
        return t;
    }

    private View tile(String title, String sub, int type) {
        boolean sel = srcType == type;
        LinearLayout t = vertical();
        t.setGravity(Gravity.CENTER);
        GradientDrawable fill = rounded(sel ? p.accent : p.input, 0, 2);
        Brackets br = new Brackets(sel ? 0xB0FFFFFF : (dark ? 0xFF9A9AA0 : 0xFF8A8A90), dp(2), dp(12));
        t.setBackground(new android.graphics.drawable.LayerDrawable(new Drawable[]{fill, br}));
        t.addView(textMedium(title, 20, sel ? p.accentOn : p.text));
        TextView s = text(sub, 11, sel ? p.accentOn : p.muted, false);
        s.setGravity(Gravity.CENTER);
        t.addView(s);
        t.setOnClickListener(v -> {
            if (srcType != type) {
                srcType = type;
                spec.wwwDir = null;
                sourceError = "";
                render();
            }
        });
        return t;
    }

    /** The four corner brackets drawn around the HTML / ZIP / Folder tiles. */
    private static final class Brackets extends Drawable {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final float arm;

        Brackets(int color, float stroke, float arm) {
            paint.setColor(color);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(stroke);
            paint.setStrokeCap(Paint.Cap.SQUARE);
            this.arm = arm;
        }

        @Override
        public void draw(Canvas c) {
            Rect b = getBounds();
            float h = paint.getStrokeWidth() / 2f;
            float l = b.left + h, t = b.top + h, r = b.right - h, m = b.bottom - h;
            c.drawLine(l, t, l + arm, t, paint);
            c.drawLine(l, t, l, t + arm, paint);
            c.drawLine(r, t, r - arm, t, paint);
            c.drawLine(r, t, r, t + arm, paint);
            c.drawLine(l, m, l + arm, m, paint);
            c.drawLine(l, m, l, m - arm, paint);
            c.drawLine(r, m, r - arm, m, paint);
            c.drawLine(r, m, r, m - arm, paint);
        }

        @Override
        public void setAlpha(int a) {
            paint.setAlpha(a);
        }

        @Override
        public void setColorFilter(ColorFilter f) {
            paint.setColorFilter(f);
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }

    private LinearLayout.LayoutParams tileParams(boolean marginRight) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
        if (marginRight) lp.rightMargin = dp(10);
        return lp;
    }

    private View staticBox(String s) {
        TextView t = text(s, 18, p.text, false);
        t.setGravity(Gravity.CENTER_VERTICAL);
        t.setSingleLine(true);
        t.setBackground(rounded(p.input, 0, 18));
        t.setPadding(dp(18), 0, dp(18), 0);
        return t;
    }

    private EditText rowField(String value, int inputType, int sp) {
        EditText e = new EditText(this);
        e.setInputType(inputType);
        e.setSingleLine(true);
        e.setText(value);
        e.setTextColor(p.text);
        e.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        e.setBackground(rounded(p.input, 0, 18));
        e.setPadding(dp(18), 0, dp(18), 0);
        return e;
    }

    /** Left: bold title + muted sub-label. Right: the control. */
    private View labelRow(String title, String sub, View control, boolean fill) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout left = vertical();
        left.setPadding(dp(4), 0, dp(8), 0);
        left.addView(textMedium(title, 20, p.text));
        left.addView(text(sub, 14, p.muted, false));
        row.addView(left, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 36f));
        if (fill) {
            row.addView(control, new LinearLayout.LayoutParams(0, dp(54), 64f));
        } else {
            row.addView(control, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(54)));
        }
        return row;
    }

    private View switchRow(String title, String sub, boolean checked, Consumer<Boolean> onChange) {
        Switch sw = new Switch(this);
        sw.setChecked(checked);
        sw.setThumbTintList(ColorStateList.valueOf(checked ? p.accent : p.muted));
        sw.setTrackTintList(ColorStateList.valueOf(checked ? p.accentDim : p.line));
        sw.setOnCheckedChangeListener((b, on) -> {
            onChange.accept(on);
            sw.setThumbTintList(ColorStateList.valueOf(on ? p.accent : p.muted));
            sw.setTrackTintList(ColorStateList.valueOf(on ? p.accentDim : p.line));
        });
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout left = vertical();
        left.setPadding(dp(4), 0, dp(8), 0);
        left.addView(textMedium(title, 20, p.text));
        left.addView(text(sub, 14, p.muted, false));
        row.addView(left, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(sw);
        return row;
    }

    private Button bigButton(String label, boolean primary, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setTextColor(primary ? p.accentOn : p.text);
        b.setBackground(primary ? rounded(p.accent, 0, 30) : rounded(p.input, p.line, 30));
        b.setOnClickListener(l);
        b.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(60)));
        return b;
    }

    private TextWatcher watcher(Consumer<String> onChange) {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override
            public void onTextChanged(CharSequence s, int a, int b, int c) {
                onChange.accept(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        };
    }

    private AlertDialog.Builder dialogBuilder() {
        return new AlertDialog.Builder(this, dark
                ? android.R.style.Theme_Material_Dialog_Alert
                : android.R.style.Theme_Material_Light_Dialog_Alert);
    }

    private void showInfo(String title, String body) {
        dialogBuilder().setTitle(title).setMessage(body).setPositiveButton("OK", null).show();
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_LONG).show();
    }

    private static String formatSize(long b) {
        if (b < 1024) return b + " B";
        if (b < 1024 * 1024) return String.format(Locale.US, "%.1f KB", b / 1024.0);
        return String.format(Locale.US, "%.1f MB", b / 1024.0 / 1024.0);
    }
}
