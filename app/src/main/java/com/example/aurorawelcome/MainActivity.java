package com.example.aurorawelcome;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private final int bg = Color.rgb(10, 14, 25);
    private final int panel = Color.rgb(20, 27, 44);
    private final int mint = Color.rgb(83, 226, 184);
    private final int white = Color.rgb(245, 248, 255);
    private final int muted = Color.rgb(157, 170, 194);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(bg);
        getWindow().setNavigationBarColor(bg);
        getWindow().getDecorView().setSystemUiVisibility(0);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(bg);

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(22), dp(18), dp(22), dp(26));
        page.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        scroll.addView(page);

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setOrientation(LinearLayout.HORIZONTAL);
        TextView brand = text("✦  A U R O R A", 15, mint, true);
        brand.setLetterSpacing(0.08f);
        top.addView(brand, new LinearLayout.LayoutParams(0, dp(42), 1));
        TextView badge = text("الإصدار 1.0", 11, muted, false);
        badge.setPadding(dp(12), dp(8), dp(12), dp(8));
        badge.setBackground(round(panel, 30, Color.rgb(44, 59, 78), dp(1)));
        top.addView(badge);
        page.addView(top);

        View spacer = new View(this);
        page.addView(spacer, new LinearLayout.LayoutParams(1, dp(28)));

        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setPadding(dp(22), dp(25), dp(22), dp(25));
        hero.setBackground(new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{Color.rgb(31, 75, 82), Color.rgb(28, 39, 67), Color.rgb(24, 29, 48)}));
        ((GradientDrawable)hero.getBackground()).setCornerRadius(dp(28));

        TextView orb = text("✧", 40, mint, true);
        orb.setGravity(Gravity.CENTER);
        hero.addView(orb, new LinearLayout.LayoutParams(-1, dp(55)));

        TextView welcome = text("أهلاً بك في عالمك", 29, white, true);
        welcome.setGravity(Gravity.CENTER);
        welcome.setPadding(0, dp(8), 0, dp(4));
        hero.addView(welcome);

        TextView subtitle = text("مساحة هادئة لأفكارك، خططك، وبداياتك الجديدة.", 14, muted, false);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setLineSpacing(dp(4), 1f);
        hero.addView(subtitle);

        View gap = new View(this);
        hero.addView(gap, new LinearLayout.LayoutParams(1, dp(22)));

        TextView start = text("ابدأ رحلتك  〈", 16, Color.rgb(9, 28, 30), true);
        start.setGravity(Gravity.CENTER);
        start.setPadding(dp(12), dp(15), dp(12), dp(15));
        start.setBackground(round(mint, 18, mint, 0));
        start.setOnClickListener(v -> Toast.makeText(this, "أهلاً بك! رحلتك تبدأ الآن ✨", Toast.LENGTH_LONG).show());
        hero.addView(start, new LinearLayout.LayoutParams(-1, -2));
        page.addView(hero);

        View gap2 = new View(this);
        page.addView(gap2, new LinearLayout.LayoutParams(1, dp(25)));

        TextView section = text("مساحتك، بطريقتك", 21, white, true);
        page.addView(section);

        TextView desc = text("اختر بطاقة لتجربة الواجهة التفاعلية.", 13, muted, false);
        desc.setPadding(0, dp(6), 0, dp(15));
        page.addView(desc);

        page.addView(card("✎", "أفكاري", "دوّن فكرة جديدة ولا تدع الإلهام يفوتك.", "مساحة الأفكار"));
        page.addView(makeGap(12));
        page.addView(card("◷", "خططي", "رتّب أهدافك الصغيرة خطوة بخطوة.", "مساحة الخطط"));
        page.addView(makeGap(12));
        page.addView(card("✧", "إنجازاتي", "احتفل بالتقدم، حتى لو كان بسيطاً.", "مساحة الإنجازات"));

        TextView footer = text("صُنع بشغف  •  AURORA", 12, muted, false);
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(0, dp(25), 0, dp(4));
        page.addView(footer);

        setContentView(scroll);
    }

    private View card(String icon, String title, String body, String toast) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(16), dp(17), dp(16), dp(17));
        row.setBackground(round(panel, 22, Color.rgb(35, 45, 65), dp(1)));

        TextView symbol = text(icon, 24, mint, true);
        symbol.setGravity(Gravity.CENTER);
        symbol.setBackground(round(Color.rgb(30, 54, 62), 16, Color.TRANSPARENT, 0));
        row.addView(symbol, new LinearLayout.LayoutParams(dp(52), dp(52)));

        LinearLayout words = new LinearLayout(this);
        words.setOrientation(LinearLayout.VERTICAL);
        words.setPadding(dp(14), 0, 0, 0);
        TextView heading = text(title, 17, white, true);
        TextView detail = text(body, 12, muted, false);
        detail.setPadding(0, dp(5), 0, 0);
        words.addView(heading);
        words.addView(detail);
        row.addView(words, new LinearLayout.LayoutParams(0, -2, 1));

        TextView arrow = text("‹", 26, muted, true);
        row.addView(arrow);
        row.setOnClickListener(v -> Toast.makeText(this, toast + " — قريباً", Toast.LENGTH_SHORT).show());
        return row;
    }

    private View makeGap(int height) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(1, dp(height)));
        return v;
    }

    private TextView text(String value, float size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER_VERTICAL);
        t.setTypeface(Typeface.create("sans-serif", bold ? Typeface.BOLD : Typeface.NORMAL));
        return t;
    }

    private GradientDrawable round(int color, int radius, int strokeColor, int strokeWidth) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        if (strokeWidth > 0) d.setStroke(strokeWidth, strokeColor);
        return d;
    }

    private int dp(int value) {
        return (int)(value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
