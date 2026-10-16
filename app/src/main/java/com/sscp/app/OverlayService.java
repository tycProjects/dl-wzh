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
    private TextView pattern;
    private TextView sub;
    private TextView note;
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
        title.setText("SSCP · đọc Big Road + 3 cầu phụ");
        title.setTextColor(0xFFF7C969);
        title.setTextSize(11);
        box.addView(title);

        result = new TextView(this);
        result.setText("—");
        result.setTextColor(0xFFE8EFFA);
        result.setTextSize(24);
        result.setGravity(Gravity.CENTER);
        box.addView(result);

        pattern = line(0xFFF7C969, 12);
        box.addView(pattern);
        sub = line(0xFFB8C4D6, 11);
        box.addView(sub);
        info = line(0xFFB8C4D6, 11);
        box.addView(info);

        recent = line(0xFF8EA0B8, 11);
        box.addView(recent);
        note = line(0xFF6F8099, 10);
        note.setText("Mỗi ván độc lập: B 45,9% · P 44,6% · Hòa 9,5%");
        box.addView(note);

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
        params.x = 8;
        params.y = 90;   // trên video người chia bài, không che Big Road / cầu phụ

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
        show(null, "Đang chờ đọc Big Road…");
    }

    private TextView line(int color, int size) {
        TextView t = new TextView(this);
        t.setTextColor(color);
        t.setTextSize(size);
        t.setGravity(Gravity.CENTER);
        return t;
    }

    /** d = null: hiện thông báo msg (không thấy bảng / đọc chưa chắc). */
    public static void publish(android.content.Context context, RoadBrain.Display d, String msg) {
        Intent intent = new Intent(context, OverlayService.class);
        intent.setAction("SHOW");
        if (d != null) {
            intent.putExtra("headline", d.headline);
            intent.putExtra("color", d.color);
            intent.putExtra("pattern", d.pattern);
            intent.putExtra("sub", d.subRoads);
            intent.putExtra("stats", d.stats);
            intent.putExtra("hist", d.history);
        } else {
            intent.putExtra("msg", msg == null ? "" : msg);
        }
        context.startService(intent);
    }

    private void show(Intent i, String msg) {
        if (result == null) return;
        if (i == null || i.getStringExtra("headline") == null) {
            result.setText("—");
            result.setTextColor(0xFFE8EFFA);
            pattern.setText(msg == null ? "" : msg);
            sub.setText("");
            info.setText("");
            recent.setText("");
            return;
        }
        result.setText(i.getStringExtra("headline"));
        result.setTextColor(i.getIntExtra("color", 0xFFE8EFFA));
        pattern.setText(i.getStringExtra("pattern"));
        sub.setText(i.getStringExtra("sub"));
        info.setText(i.getStringExtra("stats"));
        recent.setText(i.getStringExtra("hist"));
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && "SHOW".equals(intent.getAction())) {
            show(intent, intent.getStringExtra("msg"));
        }
        return START_STICKY;
    }

    @Override public void onDestroy() {
        if (manager != null && panel != null) manager.removeView(panel);
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
