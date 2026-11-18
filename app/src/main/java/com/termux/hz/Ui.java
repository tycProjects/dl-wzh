package com.termux.hz;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.content.res.ColorStateList;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/** Small view-building toolkit so every screen shares the same look. */
public final class Ui {
    private Ui() {}

    public static final int BG = 0xFF0E1014, SURFACE = 0xFF161A21, SURFACE2 = 0xFF1E232D, STROKE = 0xFF2A303C;
    public static final int TEXT = 0xFFE6E9EF, MUTED = 0xFF8A93A5;
    public static final int ACCENT = 0xFF4ADE80, ACCENT2 = 0xFF8B7BFF, DANGER = 0xFFF87171, WARN = 0xFFFBBF24;

    public static int dp(Context c, float v) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, c.getResources().getDisplayMetrics()));
    }

    public static GradientDrawable round(int color, float radiusDp, Context c) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(c, radiusDp));
        return g;
    }

    public static Drawable ripple(Drawable content, Context c) {
        return new RippleDrawable(ColorStateList.valueOf(0x33FFFFFF), content, null);
    }

    public static TextView text(Context c, String s, float sp, int color) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        return t;
    }

    public static TextView label(Context c, String s) {
        TextView t = text(c, s.toUpperCase(), 11, MUTED);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setLetterSpacing(0.08f);
        t.setPadding(dp(c, 16), dp(c, 16), dp(c, 16), dp(c, 6));
        return t;
    }

    public static TextView button(Context c, String s, int bg, int fg, Runnable r) {
        TextView t = text(c, s, 14, fg);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(c, 16), dp(c, 10), dp(c, 16), dp(c, 10));
        t.setBackground(ripple(round(bg, 10, c), c));
        t.setClickable(true);
        t.setOnClickListener(v -> r.run());
        return t;
    }

    public static TextView chip(Context c, String s, Runnable r) {
        TextView t = text(c, s, 12.5f, TEXT);
        t.setPadding(dp(c, 12), dp(c, 7), dp(c, 12), dp(c, 7));
        GradientDrawable g = round(SURFACE2, 16, c);
        g.setStroke(dp(c, 1), STROKE);
        t.setBackground(ripple(g, c));
        t.setOnClickListener(v -> r.run());
        return t;
    }

    public static ImageButton icon(Context c, int res, String desc, Runnable r) {
        ImageButton b = new ImageButton(c);
        b.setImageResource(res);
        b.setContentDescription(desc);
        b.setColorFilter(0xFFC3CAD8);
        b.setBackground(ripple(round(0x00000000, 20, c), c));
        int p = dp(c, 10);
        b.setPadding(p, p, p, p);
        b.setLayoutParams(new LinearLayout.LayoutParams(dp(c, 44), dp(c, 44)));
        b.setOnClickListener(v -> r.run());
        return b;
    }

    public static EditText input(Context c, String hint) {
        EditText e = new EditText(c);
        e.setHint(hint);
        e.setHintTextColor(MUTED);
        e.setTextColor(TEXT);
        e.setTextSize(14);
        e.setPadding(dp(c, 12), dp(c, 10), dp(c, 12), dp(c, 10));
        GradientDrawable g = round(SURFACE2, 10, c);
        g.setStroke(dp(c, 1), STROKE);
        e.setBackground(g);
        return e;
    }

    public static View divider(Context c) {
        View v = new View(c);
        v.setBackgroundColor(STROKE);
        v.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
        return v;
    }

    public static LinearLayout.LayoutParams lp(int w, int h, float weight) {
        return new LinearLayout.LayoutParams(w, h, weight);
    }

    public static void toast(Context c, String s) { Toast.makeText(c, s, Toast.LENGTH_SHORT).show(); }

    public static void confirm(Activity a, String title, String msg, String yes, Runnable onYes) {
        new MaterialAlertDialogBuilder(a).setTitle(title).setMessage(msg)
                .setPositiveButton(yes, (d, w) -> onYes.run()).setNegativeButton("Cancel", null).show();
    }

    public static void prompt(Activity a, String title, String initial, String hint, java.util.function.Consumer<String> onOk) {
        EditText e = input(a, hint);
        e.setText(initial);
        e.setSingleLine(true);
        e.setSelection(initial == null ? 0 : initial.length());
        android.widget.FrameLayout box = new android.widget.FrameLayout(a);
        box.setPadding(dp(a, 20), dp(a, 8), dp(a, 20), 0);
        box.addView(e);
        new MaterialAlertDialogBuilder(a).setTitle(title).setView(box)
                .setPositiveButton("OK", (d, w) -> onOk.accept(e.getText().toString().trim()))
                .setNegativeButton("Cancel", null).show();
    }

    public static int parse(String hex) { return Color.parseColor(hex); }
}
