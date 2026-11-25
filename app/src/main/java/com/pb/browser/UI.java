package com.pb.browser;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.StateListDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

final class UI {
    // Hai giao dien: "Midnight Identity" (mac dinh) va "MASTER" (robot / cyber toi). Mau la bien tinh,
    // doi bang UI.setTheme() roi tao lai man hinh (recreate) -> khong phai sua tung man hinh.
    static boolean master;
    static int INK, INK_2, INK_3, INK_CHIP;
    static int INDIGO, BLUE, CYAN, VIOLET, PINK, TEAL, ORANGE;
    static int GREEN, AMBER, RED;
    static int BG, SURFACE, LINE, TEXT, MUTED;
    static final int WHITE = 0xFFFFFFFF;
    static int ON_INK, ON_INK_MUTED, ON_HERO, GREEN_TEXT, AMBER_TEXT;
    static int CHIP_ACTIVE, HANDLE, SEG, HINT, SW_OFF_THUMB, SW_OFF_TRACK, STATUS_BG, STATUS_TEXT, DOT_OFF, DOT_OFF_SHEET;
    static int[] HERO = new int[3];
    static int[][] AVATARS;
    static { setTheme(false); }

    static void init(Context c) { setTheme(Master.isOn(c)); }

    static void setTheme(boolean m) {
        master = m;
        if (!m) {
            INK = 0xFF0D1330; INK_2 = 0xFF161E45; INK_3 = 0xFF1D2650; INK_CHIP = 0xFF1A2350;
            INDIGO = 0xFF4F46E5; BLUE = 0xFF3D5AFE; CYAN = 0xFF00C2FF; VIOLET = 0xFF8B5CF6;
            PINK = 0xFFEC4899; TEAL = 0xFF0D9488; ORANGE = 0xFFF97316;
            GREEN = 0xFF10B981; AMBER = 0xFFF59E0B; RED = 0xFFF43F5E;
            BG = 0xFFEDF0F9; SURFACE = 0xFFFFFFFF; LINE = 0xFFE2E7F3; TEXT = 0xFF141B38; MUTED = 0xFF66708C;
            ON_INK = 0xFFE9EDFF; ON_INK_MUTED = 0xFF98A2CB; ON_HERO = 0xFFC6CDFB;
            GREEN_TEXT = 0xFF0B7A55; AMBER_TEXT = 0xFFB45309;
            CHIP_ACTIVE = 0xFFFFFFFF; HANDLE = 0xFFD5DBEC; SEG = 0xFFE7EBF7; HINT = 0xFF9AA3BE;
            SW_OFF_THUMB = 0xFFB0B8CE; SW_OFF_TRACK = 0xFFD5DBEC; STATUS_BG = 0xFF0A0F28; STATUS_TEXT = 0xFFDDE3FF;
            DOT_OFF = 0xFF3D478A; DOT_OFF_SHEET = 0xFFC9D1E8;
            HERO[0] = 0xFF312E81; HERO[1] = 0xFF4F46E5; HERO[2] = 0xFF6D28D9;
        } else {
            INK = 0xFF04070D; INK_2 = 0xFF0A111C; INK_3 = 0xFF101C2E; INK_CHIP = 0xFF0D1726;
            INDIGO = 0xFF0891B2; BLUE = 0xFF0E7490; CYAN = 0xFF22E5FF; VIOLET = 0xFFA855F7;
            PINK = 0xFFFF2E93; TEAL = 0xFF14B8A6; ORANGE = 0xFFFF8A00;
            GREEN = 0xFF2BF08A; AMBER = 0xFFFFC227; RED = 0xFFFF4D6D;
            BG = 0xFF060A12; SURFACE = 0xFF0D1522; LINE = 0xFF1C3350; TEXT = 0xFFE6F4FF; MUTED = 0xFF7F96B2;
            ON_INK = 0xFFE6F4FF; ON_INK_MUTED = 0xFF7F96B2; ON_HERO = 0xFF9BD9F0;
            GREEN_TEXT = 0xFF2BF08A; AMBER_TEXT = 0xFFFFC227;
            CHIP_ACTIVE = 0xFF123A52; HANDLE = 0xFF1C3350; SEG = 0xFF0A1220; HINT = 0xFF56708F;
            SW_OFF_THUMB = 0xFF4A5E78; SW_OFF_TRACK = 0xFF16263A; STATUS_BG = 0xFF020409; STATUS_TEXT = 0xFFBFEFFF;
            DOT_OFF = 0xFF2A4A68; DOT_OFF_SHEET = 0xFF2A4A68;
            HERO[0] = 0xFF020617; HERO[1] = 0xFF07213F; HERO[2] = 0xFF1A0B3D;
        }
        AVATARS = new int[][]{{INDIGO, CYAN}, {VIOLET, PINK}, {TEAL, CYAN}, {ORANGE, RED},
                {BLUE, VIOLET}, {PINK, VIOLET}, {CYAN, BLUE}, {GREEN, TEAL}};
    }

    // Mau thanh trang thai / thanh dieu huong + mau icon (sang/toi) theo giao dien hien tai
    static void bars(Activity a, int status, int nav, boolean statusLight, boolean navLight) {
        a.getWindow().setStatusBarColor(status);
        a.getWindow().setNavigationBarColor(nav);
        int f = 0;
        if (!master && statusLight) f |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        if (!master && navLight) f |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        a.getWindow().getDecorView().setSystemUiVisibility(f);
    }

    // Hop thoai (xoa ho so, tim trong trang...) theo dung giao dien sang/toi
    static android.app.AlertDialog.Builder dialog(Activity a) {
        return new android.app.AlertDialog.Builder(a, master ? android.R.style.Theme_Material_Dialog_Alert
                : android.R.style.Theme_Material_Light_Dialog_Alert);
    }

    // Hieu ung bam: thu nho nhe khi cham, bat lai khi nha. Khong chan onClick.
    @android.annotation.SuppressLint("ClickableViewAccessibility")
    static <T extends View> T press(T v) {
        v.setOnTouchListener((x, e) -> {
            int a = e.getActionMasked();
            if (a == android.view.MotionEvent.ACTION_DOWN) x.animate().scaleX(0.96f).scaleY(0.96f).setDuration(90).start();
            else if (a == android.view.MotionEvent.ACTION_UP || a == android.view.MotionEvent.ACTION_CANCEL)
                x.animate().scaleX(1f).scaleY(1f).setDuration(160).setInterpolator(new android.view.animation.OvershootInterpolator(2f)).start();
            return false;
        });
        return v;
    }

    static int dp(Context c, int v) { return (int) (v * c.getResources().getDisplayMetrics().density + 0.5f); }
    static int alpha(int color, int a) { return (a << 24) | (color & 0xFFFFFF); }

    // MASTER: goc vuong hon (kieu robot) + vien mong cho the/bang
    static float rad(Context c, int radiusDp) { return dp(c, master ? Math.max(3, Math.round(radiusDp * 0.42f)) : radiusDp); }
    static GradientDrawable round(Context c, int color, int radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color); g.setCornerRadius(rad(c, radiusDp));
        if (master && color == SURFACE) g.setStroke(dp(c, 1), LINE);
        return g;
    }
    static GradientDrawable roundTop(Context c, int color, int radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        float r = rad(c, radiusDp);
        g.setCornerRadii(new float[]{r, r, r, r, 0, 0, 0, 0});
        if (master) g.setStroke(dp(c, 1), LINE);
        return g;
    }
    static GradientDrawable outline(Context c, int color, int radiusDp) {
        GradientDrawable g = round(c, color, radiusDp);
        g.setStroke(dp(c, 1), LINE);
        return g;
    }
    static GradientDrawable gradient(Context c, int c1, int c2, int radiusDp) {
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{c1, c2});
        g.setCornerRadius(rad(c, radiusDp));
        return g;
    }
    // Vien NEON chuyen mau cyan -> tim -> hong quanh the (giao dien tuong lai)
    static Drawable neon(Context c, int fill, int radiusDp) {
        float r = rad(c, radiusDp);
        int a = master ? 0xCC : 0x80;
        GradientDrawable edge = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{alpha(CYAN, a), alpha(VIOLET, a), alpha(PINK, a)});
        edge.setCornerRadius(r);
        GradientDrawable in = new GradientDrawable();
        in.setColor(fill);
        in.setCornerRadius(Math.max(0f, r - dp(c, 1)));
        android.graphics.drawable.LayerDrawable l = new android.graphics.drawable.LayerDrawable(new Drawable[]{edge, in});
        int w = Math.max(1, dp(c, 1));
        l.setLayerInset(1, w, w, w, w);
        return l;
    }

    // Nen "cuc quang": mau tu tu tro nhay qua lai (chi chay khi view dang hien tren man hinh)
    static void aurora(final View v, final GradientDrawable g, final int c0, final int c1, final int c2) {
        final android.animation.ValueAnimator an = android.animation.ValueAnimator.ofFloat(0f, 1f);
        an.setDuration(7000);
        an.setRepeatCount(android.animation.ValueAnimator.INFINITE);
        an.setRepeatMode(android.animation.ValueAnimator.REVERSE);
        final android.animation.ArgbEvaluator ev = new android.animation.ArgbEvaluator();
        an.addUpdateListener(x -> {
            float f = (Float) x.getAnimatedValue();
            g.setColors(new int[]{(Integer) ev.evaluate(f * 0.35f, c0, VIOLET),
                    (Integer) ev.evaluate(f * 0.30f, c1, CYAN),
                    (Integer) ev.evaluate(f * 0.35f, c2, PINK)});
            v.invalidate();
        });
        v.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override public void onViewAttachedToWindow(View x) { if (!an.isStarted()) an.start(); }
            @Override public void onViewDetachedFromWindow(View x) { an.cancel(); }
        });
    }

    static RippleDrawable ripple(Drawable content) {
        return new RippleDrawable(ColorStateList.valueOf(0x22000000), content, null);
    }
    static RippleDrawable rippleLight(Drawable content) {
        return new RippleDrawable(ColorStateList.valueOf(0x33FFFFFF), content, null);
    }
    static StateListDrawable fieldBg(Context c) {
        GradientDrawable focus = round(c, SURFACE, 12);
        focus.setStroke(dp(c, 2), INDIGO);
        GradientDrawable normal = outline(c, SURFACE, 12);
        StateListDrawable s = new StateListDrawable();
        s.addState(new int[]{android.R.attr.state_focused}, focus);
        s.addState(new int[]{}, normal);
        return s;
    }

    static TextView text(Context c, String s, int sp, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setText(s); t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp); t.setTextColor(color);
        if (master) t.setTypeface(Typeface.MONOSPACE, bold ? Typeface.BOLD : Typeface.NORMAL);
        else if (bold) t.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        else t.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        t.setIncludeFontPadding(false);
        return t;
    }
    static TextView label(Context c, String s) {
        TextView t = text(c, s, 10, MUTED, true);
        t.setLetterSpacing(0.09f);
        return t;
    }
    static TextView button(Context c, String s, int bg, int fg, boolean stroke) {
        TextView t = text(c, s, 14, fg, true);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(c, 20), dp(c, 10), dp(c, 20), dp(c, 10));
        t.setMinHeight(dp(c, 44));
        if (stroke) {
            // v3.3.5: truoc day nen = bg = mau chu (VIOLET/INDIGO) -> chu TRUNG mau nen nen mat chu, vien xam LINE lam mau loang.
            // Nay: vien mau chu + nen pha nhe cung mau chu (neu bg trung fg) hoac nen bg that.
            GradientDrawable g = new GradientDrawable();
            g.setColor(bg == fg ? alpha(fg, 0x14) : bg);
            g.setCornerRadius(rad(c, 20));
            g.setStroke(Math.max(1, Math.round(c.getResources().getDisplayMetrics().density * 1.5f)), fg);
            t.setBackground(ripple(g));
        } else {
            t.setBackground(ripple(round(c, bg, 20)));
        }
        t.setClickable(true);
        return press(t);
    }
    static TextView gradButton(Context c, String s) {
        TextView t = text(c, s, 14, WHITE, true);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(c, 20), dp(c, 12), dp(c, 20), dp(c, 12));
        GradientDrawable gb = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[]{INDIGO, VIOLET, PINK});
        gb.setCornerRadius(rad(c, 22));
        t.setBackground(ripple(gb));
        t.setElevation(dp(c, 6));
        t.setOutlineSpotShadowColor(VIOLET);      // bong sang mau tim
        t.setOutlineAmbientShadowColor(VIOLET);
        t.setClickable(true);
        return press(t);
    }
    static TextView chip(Context c, String s, int bg, int fg) {
        TextView t = text(c, s, 10, fg, true);
        t.setLetterSpacing(0.07f);
        t.setPadding(dp(c, 10), dp(c, 4), dp(c, 10), dp(c, 4));
        t.setBackground(round(c, bg, 20));
        return t;
    }
    static ImageView icon(Context c, int res, int sizeDp, int tint) {
        ImageView v = new ImageView(c);
        v.setImageResource(res);
        v.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        v.setColorFilter(tint);
        v.setLayoutParams(new LinearLayout.LayoutParams(dp(c, sizeDp), dp(c, sizeDp)));
        return v;
    }
    static ImageView iconBtn(Context c, int res, int tint, int bg, int sizeDp, int radiusDp) {
        ImageView v = icon(c, res, sizeDp, tint);
        int p = dp(c, (sizeDp - 20) / 2);
        v.setPadding(p, p, p, p);
        v.setBackground(ripple(bg == 0 ? null : round(c, bg, radiusDp)));
        v.setClickable(true);
        return press(v);
    }
    static TextView avatar(Context c, String name, int idx, int sizeDp) {
        int[] pair = AVATARS[Math.abs(idx) % AVATARS.length];
        TextView t = text(c, name == null || name.isEmpty() ? "?" : new String(Character.toChars(name.codePointAt(0))).toUpperCase(), (int) (sizeDp * 0.42f), WHITE, true);
        t.setGravity(Gravity.CENTER);
        t.setBackground(gradient(c, pair[0], pair[1], (int) (sizeDp * 0.32f)));
        t.setLayoutParams(new LinearLayout.LayoutParams(dp(c, sizeDp), dp(c, sizeDp)));
        t.setElevation(dp(c, 1));
        return t;
    }
    static View divider(Context c) {
        View v = new View(c);
        v.setBackgroundColor(LINE);
        v.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(c, 1)));
        return v;
    }

    static LinearLayout menuRow(Context c, int res, int tint, String title, String sub) {
        LinearLayout r = new LinearLayout(c);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(dp(c, 20), dp(c, 10), dp(c, 20), dp(c, 10));
        r.setBackground(ripple(null));
        r.setClickable(true);
        press(r);
        ImageView ic = icon(c, res, 36, tint);
        int p = dp(c, 9);
        ic.setPadding(p, p, p, p);
        ic.setBackground(round(c, alpha(tint, 0x17), 11));
        r.addView(ic);
        LinearLayout col = new LinearLayout(c);
        col.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(0, -2, 1);
        clp.leftMargin = dp(c, 13);
        col.addView(text(c, title, 14, TEXT, true));
        if (sub != null && !sub.isEmpty()) {
            TextView s = text(c, sub, 11, MUTED, false);
            s.setPadding(0, dp(c, 1), 0, 0);
            col.addView(s);
        }
        r.addView(col, clp);
        return r;
    }

    static Dialog sheet(Activity a, LinearLayout body) {
        final Dialog d = new Dialog(a);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        Window w = d.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            w.setDimAmount(0.55f);
            WindowManager.LayoutParams lp = w.getAttributes();
            lp.gravity = Gravity.BOTTOM;
            lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
            lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            w.setAttributes(lp);
        }
        final LinearLayout box = new LinearLayout(a);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackground(roundTop(a, SURFACE, 24));
        box.setPadding(0, dp(a, 10), 0, dp(a, 10));
        View handle = new View(a);
        handle.setBackground(round(a, HANDLE, 4));
        LinearLayout.LayoutParams hl = new LinearLayout.LayoutParams(dp(a, 40), dp(a, 4));
        hl.gravity = Gravity.CENTER_HORIZONTAL;
        hl.bottomMargin = dp(a, 6);
        box.addView(handle, hl);
        box.addView(body, new LinearLayout.LayoutParams(-1, -2));
        d.setContentView(box);
        d.show();
        if (w != null) {
            w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            w.setGravity(Gravity.BOTTOM);
        }
        box.getViewTreeObserver().addOnGlobalLayoutListener(new android.view.ViewTreeObserver.OnGlobalLayoutListener() {
            @Override public void onGlobalLayout() {
                box.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                box.setTranslationY(box.getHeight());
                box.animate().translationY(0).setDuration(300).setInterpolator(new android.view.animation.PathInterpolator(0.2f, 0.9f, 0.25f, 1f)).start();
            }
        });
        return d;
    }
}