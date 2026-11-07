package com.pb.browser;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.TextView;

final class UI {
    static final int BLUE = 0xFF1A73E8, TEXT = 0xFF202124, GREY = 0xFF5F6368,
            BG = 0xFFF8F9FA, LINE = 0xFFDADCE0, RED = 0xFFD93025, WHITE = 0xFFFFFFFF;

    static int dp(Context c, int v) { return (int) (v * c.getResources().getDisplayMetrics().density + 0.5f); }

    static GradientDrawable round(Context c, int color, int radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color); g.setCornerRadius(dp(c, radiusDp));
        return g;
    }
    static GradientDrawable outline(Context c, int color, int radiusDp) {
        GradientDrawable g = round(c, color, radiusDp);
        g.setStroke(dp(c, 1), LINE);
        return g;
    }
    static RippleDrawable ripple(android.graphics.drawable.Drawable content) {
        return new RippleDrawable(ColorStateList.valueOf(0x22000000), content, null);
    }
    static TextView text(Context c, String s, int sp, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setText(s); t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp); t.setTextColor(color);
        if (bold) t.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
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
    static ImageView icon(Context c, int res, int sizeDp) {
        ImageView v = new ImageView(c);
        v.setImageResource(res);
        v.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        v.setLayoutParams(new android.widget.LinearLayout.LayoutParams(dp(c, sizeDp), dp(c, sizeDp)));
        return v;
    }
}
