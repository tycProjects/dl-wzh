package vn.ztrongz.ffmenu;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private int bg = Color.rgb(15, 15, 19);
    private int panel = Color.rgb(29, 29, 36);
    private int red = Color.rgb(255, 54, 74);

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        getWindow().setStatusBarColor(bg);
        getWindow().setNavigationBarColor(bg);

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(20), dp(22), dp(20), dp(20));
        page.setBackgroundColor(bg);

        TextView title = text("ZTRONGZ  /  FF MENU", 23, red, true);
        page.addView(title);
        TextView sub = text("ANDROID CONTROL PANEL  •  v1.0", 11, Color.LTGRAY, false);
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(-1, -2);
        subLp.topMargin = dp(6);
        page.addView(sub, subLp);

        page.addView(section("AIM ASSIST  ·  GIAO DIỆN"));
        TextView info = text("Bảng điều khiển mô phỏng. Các tùy chọn chỉ lưu trong ứng dụng; không điều khiển ngắm trong Free Fire.", 13, Color.rgb(205,205,212), false);
        info.setPadding(dp(14), dp(12), dp(14), dp(12));
        info.setBackgroundColor(panel);
        LinearLayout.LayoutParams infoLp = new LinearLayout.LayoutParams(-1, -2);
        infoLp.topMargin = dp(10);
        page.addView(info, infoLp);

        Button aim = button("AIM ASSIST: OFF");
        aim.setOnClickListener(v -> {
            boolean on = !getSharedPreferences("ui", MODE_PRIVATE).getBoolean("aim", false);
            getSharedPreferences("ui", MODE_PRIVATE).edit().putBoolean("aim", on).apply();
            aim.setText("AIM ASSIST: " + (on ? "ON" : "OFF"));
            aim.setTextColor(on ? Color.WHITE : red);
        });
        LinearLayout.LayoutParams aimLp = new LinearLayout.LayoutParams(-1, dp(50));
        aimLp.topMargin = dp(12);
        page.addView(aim, aimLp);
        aim.setText("AIM ASSIST: " + (getSharedPreferences("ui", MODE_PRIVATE).getBoolean("aim", false) ? "ON" : "OFF"));

        TextView level = text("Mức hỗ trợ giao diện: " + getSharedPreferences("ui", MODE_PRIVATE).getInt("level", 50) + "%", 14, Color.WHITE, true);
        LinearLayout.LayoutParams levelLp = new LinearLayout.LayoutParams(-1, -2);
        levelLp.topMargin = dp(18);
        page.addView(level, levelLp);

        SeekBar seek = new SeekBar(this);
        seek.setMax(100);
        seek.setProgress(getSharedPreferences("ui", MODE_PRIVATE).getInt("level", 50));
        seek.setProgressTintList(android.content.res.ColorStateList.valueOf(red));
        seek.setThumbTintList(android.content.res.ColorStateList.valueOf(red));
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar b, int value, boolean user) {
                level.setText("Mức hỗ trợ giao diện: " + value + "%");
                getSharedPreferences("ui", MODE_PRIVATE).edit().putInt("level", value).apply();
            }
            public void onStartTrackingTouch(SeekBar b) {}
            public void onStopTrackingTouch(SeekBar b) {}
        });
        page.addView(seek);

        page.addView(section("MENU NỔI"));
        Button overlay = button("BẬT MENU NỔI");
        LinearLayout.LayoutParams overlayLp = new LinearLayout.LayoutParams(-1, dp(50));
        overlayLp.topMargin = dp(10);
        page.addView(overlay, overlayLp);
        overlay.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
                startActivity(i);
                Toast.makeText(this, "Cho phép hiển thị trên ứng dụng khác, rồi quay lại bấm nút.", Toast.LENGTH_LONG).show();
            } else {
                startService(new Intent(this, FloatingMenuService.class));
                Toast.makeText(this, "Menu nổi đã khởi chạy.", Toast.LENGTH_SHORT).show();
            }
        });

        Button stop = button("TẮT MENU NỔI");
        LinearLayout.LayoutParams stopLp = new LinearLayout.LayoutParams(-1, dp(46));
        stopLp.topMargin = dp(8);
        page.addView(stop, stopLp);
        stop.setOnClickListener(v -> stopService(new Intent(this, FloatingMenuService.class)));

        TextView foot = text("ZTrongz • Local settings • No game process access", 11, Color.GRAY, false);
        LinearLayout.LayoutParams footLp = new LinearLayout.LayoutParams(-1, -2);
        footLp.topMargin = dp(24);
        page.addView(foot, footLp);
        setContentView(page);
    }

    private TextView section(String s) {
        TextView t = text(s, 14, red, true);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(24);
        addTopMarginHack(t, lp);
        return t;
    }
    private void addTopMarginHack(View v, LinearLayout.LayoutParams lp) { v.setLayoutParams(lp); }
    private TextView text(String s, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s); t.setTextSize(size); t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }
    private Button button(String s) {
        Button b = new Button(this);
        b.setText(s); b.setTextColor(red); b.setTextSize(13);
        b.setAllCaps(false);
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(panel));
        return b;
    }
    private int dp(int n) { return (int)(n * getResources().getDisplayMetrics().density + 0.5f); }
}
