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
    // "Midnight Identity" — navy chu dao, indigo/cyan/violet lam diem nhan
    static final int INK = 0xFF0D1330, INK_2 = 0xFF161E45, INK_3 = 0xFF1D2650, INK_CHIP = 0xFF1A2350;
    static final int INDIGO = 0xFF4F46E5, BLUE = 0xFF3D5AFE, CYAN = 0xFF00C2FF,
            VIOLET = 0xFF8B5CF6, PINK = 0xFFEC4899, TEAL = 0xFF0D9488, ORANGE = 0xFFF97316;
    static final int GREEN = 0xFF10B981, AMBER = 0xFFF59E0B, RED = 0xFFF43F5E;
    static final int BG = 0xFFEDF0F9, SURFACE = 0xFFFFFFFF, LINE = 0xFFE2E7F3,
            TEXT = 0xFF141B38, MUTED = 0xFF66708C, WHITE = 0xFFFFFFFF;
    static final int ON_INK = 0xFFE9EDFF, ON_INK_MUTED = 0xFF98A2CB, ON_HERO = 0xFFC6CDFB;
    static final int GREEN_TEXT = 0xFF0B7A55, AMBER_TEXT = 0xFFB45309;

    static final int[][] AVATARS = {
            {INDIGO, CYAN}, {VIOLET, PINK}, {TEAL, CYAN}, {ORANGE, RED},
            {BLUE, VIOLET}, {PINK, VIOLET}, {CYAN, BLUE}, {GREEN, TEAL}};

    static int dp(Context c, int v) { return (int) (v * c.getResources().getDisplayMetrics().density + 0.5f); }
    static int alpha(int color, int a) { return (a << 24) | (color & 0xFFFFFF); }

    static GradientDrawable round(Context c, int color, int radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color); g.setCornerRadius(dp(c, radiusDp));
        return g;
    }
    static GradientDrawable roundTop(Context c, int color, int radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        float r = dp(c, radiusDp);
        g.setCornerRadii(new float[]{r, r, r, r, 0, 0, 0, 0});
        return g;
    }
    static GradientDrawable outline(Context c, int color, int radiusDp) {
        GradientDrawable g = round(c, color, radiusDp);
        g.setStroke(dp(c, 1), LINE);
        return g;
    }
    static GradientDrawable gradient(Context c, int c1, int c2, int radiusDp) {
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{c1, c2});
        g.setCornerRadius(dp(c, radiusDp));
        return g;
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
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
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
        t.setBackground(ripple(stroke ? outline(c, bg, 20) : round(c, bg, 20)));
        t.setClickable(true);
        return t;
    }
    static TextView gradButton(Context c, String s) {
        TextView t = text(c, s, 14, WHITE, true);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(c, 20), dp(c, 12), dp(c, 20), dp(c, 12));
        t.setBackground(ripple(gradient(c, INDIGO, BLUE, 22)));
        t.setClickable(true);
        return t;
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
        return v;
    }
    static TextView avatar(Context c, String name, int idx, int sizeDp) {
        int[] pair = AVATARS[Math.abs(idx) % AVATARS.length];
        TextView t = text(c, name == null || name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase(), (int) (sizeDp * 0.42f), WHITE, true);
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
            w.setDimAmount(0.5f);
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
        handle.setBackground(round(a, 0xFFD5DBEC, 4));
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
                box.animate().translationY(0).setDuration(260).setInterpolator(new DecelerateInterpolator()).start();
            }
        });
        return d;
    }
}