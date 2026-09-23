package com.hoang.proxy;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.*;

public class MainActivity extends Activity {
    private int dp(int v) {
        return (int)(v * getResources().getDisplayMetrics().density + 0.5f);
    }

    private TextView tv(String s, float size, int color) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(size);
        t.setTextColor(color);
        return t;
    }

    private GradientDrawable box(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        d.setStroke(dp(1), Color.rgb(22,45,65));
        return d;
    }

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(5,9,20));
        getWindow().setNavigationBarColor(Color.rgb(5,9,20));

        // Hiển thị giao diện trước; lỗi thông báo không được làm app crash.
        buildUi();

        try {
            KeyManager.getOrCreate(this);
            AlarmScheduler.scheduleNext(this);
            if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 20);
            } else {
                NotificationHelper.show(this);
            }
        } catch (Exception ignored) {}
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == 20) {
            try { NotificationHelper.show(this); } catch (Exception ignored) {}
        }
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(28),dp(28),dp(28),dp(20));
        root.setBackgroundColor(Color.rgb(5,9,20));

        TextView title = tv("HOANG PROXY",28,Color.rgb(33,230,208));
        title.setGravity(Gravity.CENTER);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        root.addView(title,new LinearLayout.LayoutParams(-1,dp(50)));

        TextView sub = tv("SHIZUKU EDITION",13,Color.rgb(130,145,165));
        sub.setGravity(Gravity.CENTER);
        root.addView(sub,new LinearLayout.LayoutParams(-1,dp(38)));

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(20),dp(20),dp(20),dp(20));
        card.setBackground(box(Color.rgb(8,17,31),22));
        root.addView(card,new LinearLayout.LayoutParams(-1,0,1));

        TextView kt = tv("KEY HÔM NAY",14,Color.rgb(135,147,166));
        card.addView(kt);

        TextView hint = tv("Key miễn phí được gửi trong thông báo Android.\nHiệu lực 12 giờ.",15,Color.WHITE);
        hint.setPadding(0,dp(12),0,dp(14));
        card.addView(hint);

        EditText input = new EditText(this);
        input.setHint("Nhập key");
        input.setSingleLine(true);
        input.setTextColor(Color.WHITE);
        input.setHintTextColor(Color.GRAY);
        input.setPadding(dp(15),0,dp(15),0);
        input.setBackground(box(Color.rgb(3,8,16),14));
        card.addView(input,new LinearLayout.LayoutParams(-1,dp(54)));

        Button check = new Button(this);
        check.setText("KIỂM TRA KEY");
        card.addView(check,new LinearLayout.LayoutParams(-1,dp(54)));

        TextView status = tv("Trạng thái: Chưa kiểm tra",14,Color.rgb(135,147,166));
        status.setPadding(0,dp(8),0,dp(8));
        card.addView(status);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);

        TextView server = tv("SERVER\nAUTO",15,Color.WHITE);
        server.setGravity(Gravity.CENTER);
        server.setBackground(box(Color.rgb(7,13,24),14));
        row.addView(server,new LinearLayout.LayoutParams(0,dp(70),1));

        Space gap = new Space(this);
        row.addView(gap,new LinearLayout.LayoutParams(dp(10),1));

        TextView fps = tv("FPS\n--",15,Color.WHITE);
        fps.setGravity(Gravity.CENTER);
        fps.setBackground(box(Color.rgb(7,13,24),14));
        row.addView(fps,new LinearLayout.LayoutParams(0,dp(70),1));
        card.addView(row);

        Button start = new Button(this);
        start.setText("S T A R T");
        start.setTextSize(18);
        start.setTextColor(Color.WHITE);
        start.setTypeface(Typeface.DEFAULT_BOLD);
        card.addView(start,new LinearLayout.LayoutParams(-1,dp(64)));

        TextView foot = tv("HOANG PROXY • DAILY KEY SYSTEM\nHOÀNG GROUP",12,Color.rgb(100,112,130));
        foot.setGravity(Gravity.CENTER);
        foot.setPadding(0,dp(14),0,0);
        card.addView(foot);

        check.setOnClickListener(v -> {
            try {
                boolean ok = KeyManager.isValid(this,input.getText().toString());
                status.setText(ok ? "Trạng thái: KEY HỢP LỆ ✓" : "Trạng thái: KEY SAI / ĐÃ HẾT 12 GIỜ");
                status.setTextColor(ok ? Color.rgb(33,230,208) : Color.rgb(255,110,110));
            } catch (Exception e) {
                status.setText("Không kiểm tra được key");
            }
        });

        start.setOnClickListener(v -> {
            Intent launch = getPackageManager().getLaunchIntentForPackage("com.dts.freefireth");
            if (launch != null) startActivity(launch);
            else Toast.makeText(this,"Không tìm thấy ứng dụng đích.",Toast.LENGTH_SHORT).show();
        });

        setContentView(root);
    }
}
