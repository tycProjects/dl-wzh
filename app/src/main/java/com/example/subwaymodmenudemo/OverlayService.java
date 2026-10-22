package com.example.subwaymodmenudemo;

import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.WindowManager;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class OverlayService extends Service {
    private WindowManager wm;
    private View panel;
    private boolean magnet = false, multiplier = false, infiniteJump = false;
    private int dp(float n) { return (int)(n * getResources().getDisplayMetrics().density + 0.5f); }

    @Override public IBinder onBind(Intent intent) { return null; }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (Build.VERSION.SDK_INT >= 23 && !android.provider.Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Ative a permissão de sobreposição primeiro.", Toast.LENGTH_LONG).show();
            stopSelf();
            return START_NOT_STICKY;
        }
        if (panel == null) showPanel();
        return START_NOT_STICKY;
    }

    private void showPanel() {
        wm = (WindowManager)getSystemService(WINDOW_SERVICE);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(12), dp(10), dp(12), dp(10));
        box.setBackgroundColor(Color.rgb(18, 24, 34));

        TextView header = new TextView(this);
        header.setText("SUBWAY MOD MENU  ×");
        header.setTextColor(Color.rgb(255, 83, 83));
        header.setTextSize(16);
        header.setTypeface(null, Typeface.BOLD);
        header.setPadding(0, 0, 0, dp(8));
        header.setOnClickListener(v -> stopSelf());
        box.addView(header);

        TextView disclaimer = new TextView(this);
        disclaimer.setText("DEMO: alterna estados locais; não altera o jogo.");
        disclaimer.setTextColor(Color.LTGRAY);
        disclaimer.setTextSize(11);
        box.addView(disclaimer);

        addToggle(box, "Ímã de moedas", () -> magnet, v -> magnet = v);
        addToggle(box, "Multiplicador de moedas", () -> multiplier, v -> multiplier = v);
        addToggle(box, "Pulo infinito", () -> infiniteJump, v -> infiniteJump = v);

        TextView hint = new TextView(this);
        hint.setText("Toque no título para fechar.");
        hint.setTextColor(Color.GRAY);
        hint.setTextSize(10);
        hint.setPadding(0, dp(5), 0, 0);
        box.addView(hint);

        int type = Build.VERSION.SDK_INT >= 26 ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                                               : WindowManager.LayoutParams.TYPE_PHONE;
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
            dp(250), WindowManager.LayoutParams.WRAP_CONTENT, type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.END;
        lp.x = dp(12); lp.y = dp(100);
        panel = box;
        try { wm.addView(panel, lp); }
        catch (Exception e) {
            Toast.makeText(this, "Não foi possível mostrar a sobreposição.", Toast.LENGTH_LONG).show();
            stopSelf();
        }
    }

    interface Getter { boolean get(); }
    interface Setter { void set(boolean value); }
    private void addToggle(LinearLayout parent, String label, Getter getter, Setter setter) {
        Button b = new Button(this);
        b.setAllCaps(false);
        b.setTextSize(13);
        b.setTextColor(Color.WHITE);
        b.setPadding(dp(5), 0, dp(5), 0);
        Runnable refresh = () -> {
            b.setText(label + ": " + (getter.get() ? "ON" : "OFF"));
            b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                getter.get() ? Color.rgb(37, 135, 83) : Color.rgb(64, 72, 85)));
        };
        refresh.run();
        b.setOnClickListener(v -> {
            setter.set(!getter.get());
            refresh.run();
            Toast.makeText(this, label + " " + (getter.get() ? "ativado (demo)" : "desativado"), Toast.LENGTH_SHORT).show();
        });
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(44));
        p.setMargins(0, dp(4), 0, dp(4));
        parent.addView(b, p);
    }

    @Override public void onDestroy() {
        if (wm != null && panel != null) {
            try { wm.removeView(panel); } catch (Exception ignored) {}
        }
        panel = null;
        super.onDestroy();
    }
}
