package com.sscp.app;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

public class OverlayService extends Service {
    private WindowManager manager;
    private View panel;
    private TextView result;
    private TextView info;
    private TextView recent;
    private WindowManager.LayoutParams params;

    @Override public void onCreate() {
        super.onCreate();
        if (!android.provider.Settings.canDrawOverlays(this)) {
            stopSelf();
            return;
        }

        manager = (WindowManager) getSystemService(WINDOW_SERVICE);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(18, 10, 18, 10);
        box.setBackgroundColor(0xEE111A29);

        TextView title = new TextView(this);
        title.setText("SSCP · kết quả thật (không dự đoán)");
        title.setTextColor(0xFFF7C969);
        title.setTextSize(13);
        box.addView(title);

        result = new TextView(this);
        result.setText("—");
        result.setTextColor(0xFFE8EFFA);
        result.setTextSize(30);
        result.setGravity(Gravity.CENTER);
        box.addView(result);

        info = new TextView(this);
        info.setTextColor(0xFFB8C4D6);
        info.setTextSize(12);
        info.setGravity(Gravity.CENTER);
        box.addView(info);

        recent = new TextView(this);
        recent.setTextColor(0xFF8EA0B8);
        recent.setTextSize(11);
        recent.setGravity(Gravity.CENTER);
        box.addView(recent);

        TextView close = new TextView(this);
        close.setText("ĐÓNG");
        close.setTextColor(0xFF8EA0B8);
        close.setGravity(Gravity.CENTER);
        close.setPadding(4, 6, 4, 2);
        close.setOnClickListener(v -> stopSelf());
        box.addView(close);

        int type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 30;
        params.y = 180;

        box.setOnTouchListener(new View.OnTouchListener() {
            private int startX, startY;
            private float downX, downY;
            @Override public boolean onTouch(View v, MotionEvent e) {
                if (e.getAction() == MotionEvent.ACTION_DOWN) {
                    startX = params.x; startY = params.y;
                    downX = e.getRawX(); downY = e.getRawY();
                    return true;
                }
                if (e.getAction() == MotionEvent.ACTION_MOVE) {
                    params.x = startX + (int)(e.getRawX() - downX);
                    params.y = startY + (int)(e.getRawY() - downY);
                    manager.updateViewLayout(panel, params);
                    return true;
                }
                return e.getAction() == MotionEvent.ACTION_UP;
            }
        });

        panel = box;
        manager.addView(panel, params);
        updateCells(getSharedPreferences("sscp", MODE_PRIVATE).getString("cells", ""));
    }

    /** cells: chuỗi B/P/T theo thứ tự ván trên bảng của bàn; null = không thấy bảng. */
    public static void publishCells(android.content.Context context, String cells) {
        String v = cells == null ? "" : cells;
        context.getSharedPreferences("sscp", MODE_PRIVATE).edit().putString("cells", v).apply();
        Intent intent = new Intent(context, OverlayService.class);
        intent.setAction("UPDATE_CELLS");
        intent.putExtra("cells", v);
        context.startService(intent);
    }

    private void updateCells(String cells) {
        if (result == null) return;
        if (cells == null || cells.isEmpty()) {
            result.setText("—");
            result.setTextColor(0xFFE8EFFA);
            info.setText("Chưa thấy bảng kết quả trên màn hình");
            recent.setText("");
            return;
        }
        char last = cells.charAt(cells.length() - 1);
        String name = last == 'B' ? "B · Nhà Cái" : last == 'P' ? "P · Tay con" : "T · Hòa";
        result.setText(name);
        result.setTextColor(last == 'B' ? 0xFFFF526B : last == 'P' ? 0xFF36A7FF : 0xFF6FCF6A);
        int b = 0, p = 0, t = 0;
        for (char c : cells.toCharArray()) { if (c == 'B') b++; else if (c == 'P') p++; else t++; }
        int streak = 1;
        for (int i = cells.length() - 2; i >= 0 && cells.charAt(i) == last; i--) streak++;
        info.setText("Ván mới nhất · chuỗi " + streak + "\nĐang hiện trên bảng: B " + b + " · P " + p + " · T " + t);
        String tail = cells.length() > 12 ? cells.substring(cells.length() - 12) : cells;
        recent.setText("12 ván gần nhất: " + tail);
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && "UPDATE_CELLS".equals(intent.getAction())) {
            updateCells(intent.getStringExtra("cells"));
        }
        return START_STICKY;
    }

    @Override public void onDestroy() {
        if (manager != null && panel != null) manager.removeView(panel);
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
