package com.promptdrop.ragedemo;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.content.SharedPreferences;

public class MainActivity extends Activity {
    private SharedPreferences prefs;
    private DemoView demo;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("promptdrop_demo", MODE_PRIVATE);

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(16), dp(18), dp(16), dp(18));
        page.setBackgroundColor(Color.rgb(10, 13, 19));

        TextView header = new TextView(this);
        header.setText("PROMPT DROP  /  RAGE UI");
        header.setTextColor(Color.rgb(80, 255, 180));
        header.setTextSize(21);
        header.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        page.addView(header);

        TextView warning = new TextView(this);
        warning.setText("OFFLINE SANDBOX  •  MOCK TARGETS ONLY\nНе подключается к Standoff 2 и не читает память игры.");
        warning.setTextColor(Color.LTGRAY);
        warning.setTextSize(12);
        warning.setPadding(0, dp(7), 0, dp(12));
        page.addView(warning);

        demo = new DemoView(this);
        page.addView(demo, new LinearLayout.LayoutParams(-1, dp(260)));

        addToggle(page, "ESP preview", "esp", true);
        addToggle(page, "Box ESP", "box", true);
        addToggle(page, "Skeleton ESP", "skeleton", true);
        addToggle(page, "Health bar", "health", true);
        addToggle(page, "Distance label", "distance", true);
        addToggle(page, "FOV circle", "fov", true);

        addSlider(page, "Aim FOV (visual demo)", "fov_value", 5, 180, 45);
        addSlider(page, "Smooth (visual demo)", "smooth", 1, 20, 8);

        TextView footer = new TextView(this);
        footer.setText("CONFIG SAVED LOCALLY  •  ARM64  •  C++17 / NDK");
        footer.setTextColor(Color.GRAY);
        footer.setTextSize(10);
        footer.setPadding(0, dp(14), 0, 0);
        page.addView(footer);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.addView(page);
        setContentView(scroll);
    }

    private void addToggle(LinearLayout parent, String title, String key, boolean def) {
        Switch sw = new Switch(this);
        sw.setText(title);
        sw.setTextColor(Color.WHITE);
        sw.setTextSize(14);
        sw.setPadding(0, dp(5), 0, dp(5));
        sw.setChecked(prefs.getBoolean(key, def));
        sw.setOnCheckedChangeListener((button, checked) -> {
            prefs.edit().putBoolean(key, checked).apply();
            if (demo != null) demo.invalidate();
        });
        parent.addView(sw);
    }

    private void addSlider(LinearLayout parent, String title, String key,
                           int min, int max, int def) {
        TextView label = new TextView(this);
        label.setTextColor(Color.WHITE);
        label.setTextSize(13);
        parent.addView(label);

        SeekBar seek = new SeekBar(this);
        seek.setMax(max - min);
        seek.setProgress(prefs.getInt(key, def) - min);
        Runnable update = () -> {
            int value = seek.getProgress() + min;
            label.setText(title + ": " + value);
            prefs.edit().putInt(key, value).apply();
            if (demo != null) demo.invalidate();
        };
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) { update.run(); }
            public void onStartTrackingTouch(SeekBar bar) {}
            public void onStopTrackingTouch(SeekBar bar) {}
        });
        parent.addView(seek);
        update.run();
    }

    private int dp(float value) {
        return (int)(value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
