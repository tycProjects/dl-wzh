package vn.ztrongz.ffmenu;

import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.os.IBinder;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class FloatingMenuService extends Service {
    private WindowManager wm;
    private View bubble;
    private View panel;
    private WindowManager.LayoutParams bubbleParams, panelParams;
    private boolean expanded = false;
    private final int red = Color.rgb(255, 54, 74);

    @Override public void onCreate() {
        super.onCreate();
        wm = (WindowManager)getSystemService(WINDOW_SERVICE);
        int type = android.os.Build.VERSION.SDK_INT >= 26
            ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            : WindowManager.LayoutParams.TYPE_PHONE;
        bubbleParams = new WindowManager.LayoutParams(dp(58), dp(58), type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);
        bubbleParams.gravity = Gravity.TOP | Gravity.START;
        bubbleParams.x = dp(18); bubbleParams.y = dp(180);

        TextView b = new TextView(this);
        b.setText("ZT"); b.setTextColor(Color.WHITE); b.setTextSize(17);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD); b.setGravity(Gravity.CENTER);
        b.setBackground(roundDrawable(Color.rgb(190, 20, 43), dp(29)));
        bubble = b;
        bubble.setOnClickListener(v -> togglePanel());
        bubble.setOnTouchListener(new View.OnTouchListener() {
            float downX, downY; int startX, startY; long downTime;
            public boolean onTouch(View v, MotionEvent e) {
                if (e.getAction() == MotionEvent.ACTION_DOWN) {
                    downX=e.getRawX(); downY=e.getRawY();
                    startX=bubbleParams.x; startY=bubbleParams.y; downTime=System.currentTimeMillis();
                    return false;
                }
                if (e.getAction() == MotionEvent.ACTION_MOVE) {
                    float dx=e.getRawX()-downX, dy=e.getRawY()-downY;
                    if (Math.abs(dx)>8 || Math.abs(dy)>8) {
                        bubbleParams.x=startX+(int)dx; bubbleParams.y=startY+(int)dy;
                        try { wm.updateViewLayout(bubble, bubbleParams); } catch(Exception ignored) {}
                        return true;
                    }
                }
                return false;
            }
        });
        wm.addView(bubble, bubbleParams);
    }

    private void togglePanel() {
        if (expanded) {
            try { wm.removeView(panel); } catch(Exception ignored) {}
            panel = null; expanded = false; return;
        }
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL); box.setPadding(dp(14),dp(12),dp(14),dp(12));
        box.setBackground(roundDrawable(Color.rgb(22,22,28), dp(16)));
        TextView head = label("ZTRONGZ  •  FF MENU", 16, red, true);
        box.addView(head);
        TextView desc = label("AIM ASSIST  /  UI DEMO", 12, Color.WHITE, true);
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(-1,-2); dlp.topMargin=dp(10);
        box.addView(desc, dlp);
        TextView status = label("Trạng thái: " +
            (getSharedPreferences("ui", MODE_PRIVATE).getBoolean("aim", false) ? "ON" : "OFF"), 13, Color.LTGRAY, false);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(-1,-2); slp.topMargin=dp(8);
        box.addView(status, slp);
        TextView toggle = label("●  BẬT / TẮT GIAO DIỆN", 13, red, true);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(-1,dp(42)); tlp.topMargin=dp(8);
        toggle.setGravity(Gravity.CENTER_VERTICAL);
        box.addView(toggle, tlp);
        toggle.setOnClickListener(v -> {
            boolean on = !getSharedPreferences("ui", MODE_PRIVATE).getBoolean("aim", false);
            getSharedPreferences("ui", MODE_PRIVATE).edit().putBoolean("aim",on).apply();
            status.setText("Trạng thái: " + (on ? "ON" : "OFF"));
            Toast.makeText(this, "Đã cập nhật trạng thái giao diện", Toast.LENGTH_SHORT).show();
        });
        TextView close = label("ĐÓNG MENU", 12, Color.WHITE, true);
        close.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(-1,dp(36)); clp.topMargin=dp(6);
        box.addView(close, clp);
        close.setOnClickListener(v -> togglePanel());

        int type = android.os.Build.VERSION.SDK_INT >= 26
            ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            : WindowManager.LayoutParams.TYPE_PHONE;
        panelParams = new WindowManager.LayoutParams(dp(250), -2, type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);
        panelParams.gravity = Gravity.TOP | Gravity.START;
        panelParams.x = bubbleParams.x; panelParams.y = bubbleParams.y + dp(64);
        panel = box;
        wm.addView(panel, panelParams);
        expanded = true;
    }

    private TextView label(String s, int size, int color, boolean bold) {
        TextView t = new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }
    private android.graphics.drawable.GradientDrawable roundDrawable(int color, int radius) {
        android.graphics.drawable.GradientDrawable d = new android.graphics.drawable.GradientDrawable();
        d.setColor(color); d.setCornerRadius(radius); d.setStroke(dp(1), red); return d;
    }
    private int dp(int n) { return (int)(n * getResources().getDisplayMetrics().density + 0.5f); }
    @Override public void onDestroy() {
        if (wm != null) {
            try { if (panel != null) wm.removeView(panel); } catch(Exception ignored) {}
            try { if (bubble != null) wm.removeView(bubble); } catch(Exception ignored) {}
        }
        super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent) { return null; }
}
