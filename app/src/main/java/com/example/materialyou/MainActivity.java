package com.example.materialyou;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.card.MaterialCardView;

public class MainActivity extends Activity {
    private int bg = Color.rgb(248, 246, 255);
    private int surface = Color.rgb(255, 255, 255);
    private int primary = Color.rgb(91, 75, 138);
    private int onSurface = Color.rgb(29, 27, 32);
    private LinearLayout root;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(bg);
        getWindow().setNavigationBarColor(bg);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        buildUi();
    }

    private int dp(float v) { return (int)(v * getResources().getDisplayMetrics().density + 0.5f); }

    private GradientDrawable shape(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        return d;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(22), dp(22), dp(22), dp(28));
        root.setBackgroundColor(bg);
        scroll.addView(root);
        setContentView(scroll);

        TextView eyebrow = text("MATERIAL YOU • DEMO", 12, primary, true);
        eyebrow.setLetterSpacing(.12f);
        root.addView(eyebrow, margin(0, 8, 0, 10));

        TextView title = text("Good evening", 32, onSurface, true);
        root.addView(title, margin(0, 0, 0, 6));
        root.addView(text("A little space that feels like you.", 16, Color.rgb(95, 91, 102), false),
                margin(0, 0, 0, 22));

        MaterialCardView hero = card(primary, 30);
        LinearLayout heroContent = column(20);
        heroContent.setPadding(dp(22), dp(22), dp(22), dp(22));
        TextView badge = text("✦  YOUR SPACE", 12, Color.WHITE, true);
        heroContent.addView(badge);
        TextView heroTitle = text("Make today
feel lighter.", 27, Color.WHITE, true);
        heroContent.addView(heroTitle);
        TextView heroSub = text("A calm, colorful Material You starter app.", 14,
                Color.rgb(237, 231, 250), false);
        heroContent.addView(heroSub);
        MaterialButton action = new MaterialButton(this);
        action.setText("Explore");
        action.setTextColor(primary);
        action.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.WHITE));
        action.setCornerRadius(dp(22));
        action.setOnClickListener(v -> Toast.makeText(this, "Welcome to your space ✨", Toast.LENGTH_SHORT).show());
        heroContent.addView(action, new LinearLayout.LayoutParams(-2, dp(48)));
        hero.addView(heroContent);
        root.addView(hero, margin(0, 0, 0, 24));

        root.addView(text("Quick actions", 21, onSurface, true), margin(0, 0, 0, 12));
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.addView(actionCard("🎨", "Colors", "Personalize", Color.rgb(238, 231, 255)), weightParams());
        View gap = new View(this);
        row.addView(gap, new LinearLayout.LayoutParams(dp(12), 1));
        row.addView(actionCard("🔔", "Reminders", "Stay on track", Color.rgb(224, 240, 231)), weightParams());
        root.addView(row, margin(0, 0, 0, 24));

        root.addView(text("Preferences", 21, onSurface, true), margin(0, 0, 0, 12));
        MaterialCardView settings = card(surface, 26);
        LinearLayout settingsContent = column(0);
        settingsContent.setPadding(dp(18), dp(8), dp(18), dp(8));
        addSetting(settingsContent, "Dynamic color", "Use a softer color palette", true);
        addDivider(settingsContent);
        addSetting(settingsContent, "Notifications", "Gentle reminders", false);
        addDivider(settingsContent);
        addSetting(settingsContent, "Animations", "Smooth little transitions", true);
        settings.addView(settingsContent);
        root.addView(settings, margin(0, 0, 0, 20));

        TextView footer = text("Designed with rounded shapes and soft surfaces.", 12,
                Color.rgb(105, 101, 112), false);
        footer.setGravity(Gravity.CENTER);
        root.addView(footer, margin(0, 4, 0, 0));
    }

    private LinearLayout.LayoutParams margin(int l, int t, int r, int b) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.setMargins(dp(l), dp(t), dp(r), dp(b));
        return p;
    }

    private LinearLayout.LayoutParams weightParams() {
        return new LinearLayout.LayoutParams(0, -1, 1f);
    }

    private LinearLayout column(int spacing) {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    private MaterialCardView card(int color, int radius) {
        MaterialCardView c = new MaterialCardView(this);
        c.setCardBackgroundColor(color);
        c.setRadius(dp(radius));
        c.setStrokeWidth(0);
        c.setCardElevation(0);
        return c;
    }

    private View actionCard(String icon, String label, String subtitle, int tint) {
        MaterialCardView c = card(tint, 24);
        LinearLayout content = column(0);
        content.setPadding(dp(16), dp(18), dp(12), dp(18));
        content.addView(text(icon, 25, onSurface, false), margin(0, 0, 0, 12));
        content.addView(text(label, 16, onSurface, true), margin(0, 0, 0, 4));
        content.addView(text(subtitle, 12, Color.rgb(85, 82, 92), false));
        c.addView(content);
        c.setOnClickListener(v -> Toast.makeText(this, label + " opened", Toast.LENGTH_SHORT).show());
        return c;
    }

    private void addSetting(LinearLayout parent, String title, String subtitle, boolean checked) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(12), 0, dp(12));
        LinearLayout labels = column(0);
        labels.addView(text(title, 16, onSurface, true), margin(0, 0, 0, 4));
        labels.addView(text(subtitle, 12, Color.rgb(103, 99, 110), false));
        row.addView(labels, new LinearLayout.LayoutParams(0, -2, 1f));
        MaterialSwitch sw = new MaterialSwitch(this);
        sw.setChecked(checked);
        row.addView(sw);
        parent.addView(row);
    }

    private void addDivider(LinearLayout parent) {
        View line = new View(this);
        line.setBackgroundColor(Color.rgb(232, 229, 237));
        parent.addView(line, new LinearLayout.LayoutParams(-1, dp(1)));
    }
}
