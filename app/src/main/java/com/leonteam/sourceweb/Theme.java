package com.leonteam.sourceweb;

import android.content.SharedPreferences;
import android.graphics.Color;

/** Theme colours shared by the screens (read from the same preferences as the settings sheet). */
final class Theme {
    int accent, bgMode, BG, PANEL, TEXT, MUTED, STROKE, ACC_T, ON_ACCENT, uiFont;
    static final String[] FAMILIES = {"sans-serif", "serif", "monospace", "serif-monospace",
            "sans-serif-condensed", "sans-serif-light", "sans-serif-medium", "casual", "cursive"};

    static int blend(int a, int b, float t) {
        return Color.rgb((int) (Color.red(a) + (Color.red(b) - Color.red(a)) * t),
                (int) (Color.green(a) + (Color.green(b) - Color.green(a)) * t),
                (int) (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * t));
    }

    static Theme load(SharedPreferences p) {
        Theme t = new Theme();
        t.accent = p.getInt("accent", Color.rgb(174, 92, 255));
        t.bgMode = p.getInt("bg", 0);
        t.uiFont = Math.max(0, Math.min(FAMILIES.length - 1, p.getInt("uiFont", 0)));
        switch (t.bgMode) {
            case 1: t.BG = Color.BLACK; t.PANEL = Color.rgb(16, 16, 18); t.TEXT = Color.rgb(240, 240, 245); t.MUTED = Color.rgb(150, 150, 165); break;
            case 2: t.BG = Color.rgb(7, 13, 26); t.PANEL = Color.rgb(15, 25, 46); t.TEXT = Color.rgb(228, 238, 255); t.MUTED = Color.rgb(140, 158, 190); break;
            case 3: t.BG = Color.rgb(244, 242, 250); t.PANEL = Color.WHITE; t.TEXT = Color.rgb(28, 25, 42); t.MUTED = Color.rgb(106, 100, 128); break;
            default: t.BG = Color.rgb(12, 10, 20); t.PANEL = Color.rgb(24, 19, 37); t.TEXT = Color.rgb(241, 235, 255); t.MUTED = Color.rgb(169, 157, 190);
        }
        t.PANEL = blend(t.PANEL, t.accent, t.bgMode == 3 ? 0.04f : 0.07f);
        t.STROKE = Color.argb(t.bgMode == 3 ? 110 : 90, Color.red(t.accent), Color.green(t.accent), Color.blue(t.accent));
        t.ACC_T = t.bgMode == 3 ? blend(t.accent, Color.BLACK, 0.30f) : t.accent;
        double lum = 0.299 * Color.red(t.accent) + 0.587 * Color.green(t.accent) + 0.114 * Color.blue(t.accent);
        t.ON_ACCENT = lum > 150 ? Color.rgb(18, 14, 28) : Color.WHITE;
        return t;
    }
}
