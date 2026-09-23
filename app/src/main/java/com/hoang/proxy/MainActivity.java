package com.hoang.proxy;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.*;

public class MainActivity extends Activity {
    private int dp(float v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    private TextView label(String text, float size, int color) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextSize(size);
        v.setTextColor(color);
        return v;
    }

    private GradientDrawableLike bg(int color, float radius) {
        return new GradientDrawableLike(color, dp(radius));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(Color.rgb(5, 9, 20));
        getWindow().setNavigationBarColor(Color.rgb(5, 9, 20));

        if (android.os.Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 20);
        }

        KeyManager.getOrCreate(this);
        NotificationHelper.show(this);
        AlarmScheduler.scheduleNext(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(28), dp(38), dp(28), dp(24));
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setBackgroundColor(Color.rgb(5, 9, 20));

        TextView title = label("HOANG PROXY", 28, Color.rgb(33, 230, 208));
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setLetterSpacing(0.14f);
        root.addView(title);

        TextView sub = label("SHIZUKU EDITION", 13, Color.rgb(130, 145, 165));
        sub.setGravity(Gravity.CENTER);
        sub.setLetterSpacing(0.20f);
        root.addView(sub, new LinearLayout.LayoutParams(-1, dp(42)));

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(24), dp(22), dp(24), dp(24));
        card.setBackground(bg(Color.rgb(8, 17, 31), 22));
        root.addView(card, new LinearLayout.LayoutParams(-1, 0, 1f));

        TextView keyTitle = label("KEY HÔM NAY", 14, Color.rgb(135, 147, 166));
        keyTitle.setLetterSpacing(0.10f);
        card.addView(keyTitle);

        TextView keyHint = label("Key miễn phí được gửi trong thông báo Android.\nHiệu lực 12 giờ.", 15, Color.WHITE);
        keyHint.setPadding(0, dp(12), 0, dp(22));
        card.addView(keyHint);

        EditText input = new EditText(this);
        input.setHint("Nhập key");
        input.setSingleLine(true);
        input.setTextColor(Color.WHITE);
        input.setHintTextColor(Color.rgb(100, 110, 125));
        input.setBackground(bg(Color.rgb(3, 8, 16), 16));
        input.setPadding(dp(16), 0, dp(16), 0);
        card.addView(input, new LinearLayout.LayoutParams(-1, dp(54)));

        Button check = new Button(this);
        check.setText("KIỂM TRA KEY");
        check.setTextColor(Color.WHITE);
        card.addView(check, new LinearLayout.LayoutParams(-1, dp(54)));

        TextView status = label("Trạng thái: Chưa kiểm tra", 14, Color.rgb(135, 147, 166));
        status.setPadding(0, dp(10), 0, dp(10));
        card.addView(status);

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.HORIZONTAL);

        TextView server = label("SERVER\nAUTO", 15, Color.WHITE);
        server.setGravity(Gravity.CENTER);
        server.setBackground(bg(Color.rgb(7, 13, 24), 16));
        info.addView(server, new LinearLayout.LayoutParams(0, dp(74), 1f));

        Space space = new Space(this);
        info.addView(space, new LinearLayout.LayoutParams(dp(10), dp(1)));

        TextView fps = label("FPS\n--", 15, Color.WHITE);
        fps.setGravity(Gravity.CENTER);
        fps.setBackground(bg(Color.rgb(7, 13, 24), 16));
        info.addView(fps, new LinearLayout.LayoutParams(0, dp(74), 1f));
        card.addView(info);

        Button start = new Button(this);
        start.setText("S T A R T");
        start.setTextSize(18);
        start.setTextColor(Color.WHITE);
        start.setTypeface(Typeface.DEFAULT_BOLD);
        card.addView(start, new LinearLayout.LayoutParams(-1, dp(66)));

        TextView footer = label("HOANG PROXY • DAILY KEY SYSTEM\nHOÀNG GROUP", 12, Color.rgb(100, 112, 130));
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(0, dp(18), 0, 0);
        card.addView(footer);

        check.setOnClickListener(v -> {
            boolean ok = KeyManager.isValid(this, input.getText().toString());
            status.setText(ok ? "Trạng thái: KEY HỢP LỆ ✓" : "Trạng thái: KEY SAI / ĐÃ HẾT 12 GIỜ");
            status.setTextColor(ok ? Color.rgb(33, 230, 208) : Color.rgb(255, 110, 110));
        });

        start.setOnClickListener(v -> {
            // START chỉ mở ứng dụng đích; không sửa/ghi dữ liệu của ứng dụng khác.
            Intent launch = getPackageManager().getLaunchIntentForPackage("com.dts.freefireth");
            if (launch != null) {
                startActivity(launch);
            } else {
                Toast.makeText(this, "Không tìm thấy ứng dụng đích.", Toast.LENGTH_SHORT).show();
            }
        });

        setContentView(root);
    }

    static class GradientDrawableLike extends android.graphics.drawable.GradientDrawable {
        GradientDrawableLike(int color, int radius) {
            setColor(color);
            setCornerRadius(radius);
            setStroke(1, Color.rgb(22, 45, 65));
        }
    }
}
