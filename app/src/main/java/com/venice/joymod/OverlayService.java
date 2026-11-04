package com.venice.joymod;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class OverlayService extends Service {

    private static final int NOTIF_ID = 77;
    private static final int ACCENT = 0xFF00E07A;
    private static final int PANEL_BG = 0xF20B0F14;
    private static final int BTN_BG = 0xFF1B2430;

    private WindowManager wm;
    private View bubble;
    private LinearLayout panel;
    private WindowManager.LayoutParams bLp;
    private WindowManager.LayoutParams pLp;

    private TextView status;
    private TextView logView;

    private boolean open = false;
    private int startX, startY;
    private float downX, downY;
    private boolean moved;

    private final Handler ui = new Handler(Looper.getMainLooper());

    private final Runnable poller = new Runnable() {
        public void run() {
            refreshStatus();
            ui.postDelayed(this, 1200);
        }
    };

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onCreate() {
        super.onCreate();
        wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        startForeground(NOTIF_ID, buildNotification());
        addBubble();
        ui.postDelayed(poller, 900);
    }

    @Override
    public void onDestroy() {
        ui.removeCallbacksAndMessages(null);
        removeView(panel);
        removeView(bubble);
        panel = null;
        bubble = null;
        super.onDestroy();
    }

    private int dp(int v) {
        return Math.round(getResources().getDisplayMetrics().density * v);
    }

    private int overlayType() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
    }

    private void removeView(View v) {
        if (v == null) return;
        try { wm.removeView(v); } catch (Throwable t) { /* ignora */ }
    }

    private Notification buildNotification() {
        String ch = "joymod";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                NotificationChannel c = new NotificationChannel(ch, "Mod menu", NotificationManager.IMPORTANCE_LOW);
                nm.createNotificationChannel(c);
            }
        }
        PendingIntent pi = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class),
                Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0);

        Notification.Builder b = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(this, ch)
                : new Notification.Builder(this);

        return b.setContentTitle("JoyArk Queue Skip ativo")
                .setContentText("Bolha flutuante carregada.")
                .setSmallIcon(android.R.drawable.ic_menu_send)
                .setContentIntent(pi)
                .setOngoing(true)
                .build();
    }

    private void buzz(int ms) {
        try {
            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (v == null) return;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                v.vibrate(ms);
            }
        } catch (Throwable t) { /* ignora */ }
    }

    private void addBubble() {
        TextView t = new TextView(this);
        t.setText("JQ");
        t.setTextColor(Color.WHITE);
        t.setTextSize(15f);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setGravity(Gravity.CENTER);

        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(ACCENT);
        g.setStroke(dp(2), 0xCC0B0F14);
        t.setBackground(g);
        t.setElevation(dp(6));

        bLp = new WindowManager.LayoutParams(
                dp(56), dp(56),
                overlayType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        bLp.gravity = Gravity.TOP | Gravity.START;
        bLp.x = getResources().getDisplayMetrics().widthPixels - dp(74);
        bLp.y = dp(220);

        t.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent e) {
                switch (e.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        startX = bLp.x;
                        startY = bLp.y;
                        downX = e.getRawX();
                        downY = e.getRawY();
                        moved = false;
                        return true;
                    case MotionEvent.ACTION_MOVE: {
                        int dx = (int) (e.getRawX() - downX);
                        int dy = (int) (e.getRawY() - downY);
                        if (Math.abs(dx) > dp(8) || Math.abs(dy) > dp(8)) moved = true;
                        if (moved) {
                            bLp.x = startX + dx;
                            bLp.y = startY + dy;
                            try { wm.updateViewLayout(bubble, bLp); } catch (Throwable ig) { }
                        }
                        return true;
                    }
                    case MotionEvent.ACTION_UP:
                        if (!moved) {
                            buzz(12);
                            toggle();
                        } else {
                            snap();
                        }
                        return true;
                    default:
                        return false;
                }
            }
        });

        bubble = t;
        try { wm.addView(t, bLp); } catch (Throwable e) { stopSelf(); }
    }

    private void snap() {
        int w = getResources().getDisplayMetrics().widthPixels;
        bLp.x = bLp.x + dp(28) < w / 2 ? dp(8) : w - dp(64);
        if (bLp.y < dp(40)) bLp.y = dp(40);
        try { wm.updateViewLayout(bubble, bLp); } catch (Throwable ig) { }
    }

    private void toggle() {
        if (open) closePanel();
        else openPanel();
    }

    private void closePanel() {
        open = false;
        removeView(panel);
        panel = null;
    }

    private Button btn(String label, int bg, int fg, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(13f);
        b.setTextColor(fg);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(dp(12));
        g.setColor(bg);
        b.setBackground(g);
        b.setStateListAnimator(null);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(44));
        lp.bottomMargin = dp(7);
        b.setLayoutParams(lp);
        b.setOnClickListener(l);
        return b;
    }

    private LinearLayout row() {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(7);
        r.setLayoutParams(lp);
        return r;
    }

    private Button half(String label, int bg, int fg, View.OnClickListener l) {
        Button b = btn(label, bg, fg, l);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(42), 1f);
        lp.rightMargin = dp(7);
        b.setLayoutParams(lp);
        return b;
    }

    private void openPanel() {
        open = true;

        panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(14), dp(14), dp(14), dp(14));

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(18));
        bg.setColor(PANEL_BG);
        bg.setStroke(dp(1), 0xFF233040);
        panel.setBackground(bg);

        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hlp.bottomMargin = dp(8);
        head.setLayoutParams(hlp);

        TextView title = new TextView(this);
        title.setText("JOYARK MOD");
        title.setTextColor(ACCENT);
        title.setTextSize(13f);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setLayoutParams(new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        head.addView(title);

        TextView x = new TextView(this);
        x.setText("X");
        x.setTextColor(0xFF8A97A6);
        x.setTextSize(13f);
        x.setTypeface(Typeface.DEFAULT_BOLD);
        x.setPadding(dp(8), dp(2), dp(8), dp(2));
        x.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { closePanel(); }
        });
        head.addView(x);
        panel.addView(head);

        status = new TextView(this);
        status.setTextColor(0xFF8A97A6);
        status.setTextSize(11.5f);
        status.setPadding(0, 0, 0, dp(9));
        panel.addView(status);

        panel.addView(btn("PULAR FILA", ACCENT, 0xFF06110B, new View.OnClickListener() {
            public void onClick(View v) { pular(); }
        }));

        LinearLayout r1 = row();
        final Button autoBtn = half("", BTN_BG, 0xFFE6EDF5, new View.OnClickListener() {
            public void onClick(View v) {
                Prefs.setAuto(OverlayService.this, !Prefs.auto(OverlayService.this));
                refreshStatus();
            }
        });
        r1.addView(autoBtn);
        final Button modeBtn = half("", BTN_BG, 0xFFE6EDF5, new View.OnClickListener() {
            public void onClick(View v) {
                int m = (Prefs.mode(OverlayService.this) + 1) % 3;
                Prefs.setMode(OverlayService.this, m);
                refreshStatus();
            }
        });
        r1.addView(modeBtn);
        panel.addView(r1);

        LinearLayout r2 = row();
        r2.addView(half("CAPTURAR ALVO", BTN_BG, 0xFFE6EDF5, new View.OnClickListener() {
            public void onClick(View v) { capturar(); }
        }));
        r2.addView(half("CLOUD PC", BTN_BG, 0xFFE6EDF5, new View.OnClickListener() {
            public void onClick(View v) {
                Prefs.setCloudPc(OverlayService.this, !Prefs.cloudPc(OverlayService.this));
                log("Cloud PC: " + (Prefs.cloudPc(OverlayService.this) ? "ON" : "OFF"));
                refreshStatus();
            }
        }));
        panel.addView(r2);

        LinearLayout r3 = row();
        r3.addView(half("", BTN_BG, 0xFFE6EDF5, new View.OnClickListener() {
            public void onClick(View v) {
                int d = Prefs.delay(OverlayService.this);
                d = d >= 1500 ? 300 : d + 300;
                Prefs.setDelay(OverlayService.this, d);
                refreshStatus();
            }
        }));
        final Button attBtn = half("", BTN_BG, 0xFFE6EDF5, new View.OnClickListener() {
            public void onClick(View v) {
                int a = Prefs.attempts(OverlayService.this);
                a = a >= 8 ? 2 : a + 1;
                Prefs.setAttempts(OverlayService.this, a);
                refreshStatus();
            }
        });
        r3.addView(attBtn);
        panel.addView(r3);

        panel.addView(btn("ABRIR JOYARK", BTN_BG, 0xFFE6EDF5, new View.OnClickListener() {
            public void onClick(View v) { abrir(); }
        }));

        logView = new TextView(this);
        logView.setTextColor(0xFF7C8B9A);
        logView.setTextSize(10.5f);
        logView.setMaxLines(5);
        logView.setPadding(0, dp(4), 0, dp(8));
        panel.addView(logView);

        TextView hide = new TextView(this);
        hide.setText("Ocultar menu");
        hide.setTextColor(0xFF6C7A89);
        hide.setTextSize(11f);
        hide.setGravity(Gravity.CENTER);
        hide.setPadding(0, dp(6), 0, 0);
        hide.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { stopSelf(); }
        });
        panel.addView(hide);

        pLp = new WindowManager.LayoutParams(
                dp(258), WindowManager.LayoutParams.WRAP_CONTENT,
                overlayType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        pLp.gravity = Gravity.TOP | Gravity.START;
        pLp.x = dp(10);
        pLp.y = dp(110);

        try { wm.addView(panel, pLp); } catch (Throwable e) { open = false; }

        refreshStatus();
        ui.postDelayed(new Runnable() {
            public void run() { refreshStatus(); }
        }, 60);
    }

    private void pular() {
        if (QueueEngine.isRunning()) {
            log("Ja rodando...");
            return;
        }
        buzz(25);
        log("Iniciando pulo de fila...");
        QueueEngine.run(this, new QueueEngine.Log() {
            public void log(final String s) {
                ui.post(new Runnable() {
                    public void run() { OverlayService.this.log(s); }
                });
            }
        }, new Runnable() {
            public void run() { refreshStatus(); }
        });
    }

    private void capturar() {
        ScreenReader sr = ScreenReader.get();
        if (sr == null) { log("Acessibilidade off."); return; }
        sr.refresh();
        String g = Sniff.gameTitle(sr.getText());
        if (g == null) { log("Nada pra capturar."); return; }
        Prefs.setTarget(this, g);
        log("Alvo: " + g);
    }

    private void abrir() {
        Intent l = getPackageManager().getLaunchIntentForPackage(QueueEngine.PKG);
        if (l == null) { log("JoyArk nao instalado."); return; }
        l.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try { startActivity(l); } catch (Throwable t) { log("Falhou ao abrir."); }
    }

    private void log(String s) {
        if (logView != null) {
            String cur = logView.getText().toString();
            String nw = cur.length() == 0 ? s : cur + "\n" + s;
            String[] parts = nw.split("\n");
            if (parts.length > 5) {
                StringBuilder sb = new StringBuilder();
                for (int i = parts.length - 5; i < parts.length; i++) {
                    sb.append(parts[i]);
                    if (i < parts.length - 1) sb.append("\n");
                }
                nw = sb.toString();
            }
            logView.setText(nw);
        }
        if (Prefs.auto(this) && s.contains("Dentro do jogo")) {
            buzz(60);
        }
    }

    private void refreshStatus() {
        if (status == null) return;

        ScreenReader sr = ScreenReader.get();
        String txt;
        if (sr == null) {
            txt = "Acessibilidade desligada";
        } else {
            sr.refresh();
            String q = Sniff.queueLine(sr.getText());
            if (q != null) txt = "Fila: " + q;
            else if (Sniff.inStream(sr.getText())) txt = "Em jogo / sem fila";
            else txt = "JoyArk em segundo plano";
        }
        status.setText(txt);

        ViewGroup p = panel;
        if (p != null && p.getChildCount() > 3) {
            LinearLayout r1 = (LinearLayout) p.getChildAt(3);
            if (r1.getChildCount() == 2) {
                ((Button) r1.getChildAt(0)).setText("AUTO: " + (Prefs.auto(this) ? "ON" : "OFF"));
                int m = Prefs.mode(this);
                ((Button) r1.getChildAt(1)).setText(m == 0 ? "MODO: SUAVE" : m == 1 ? "MODO: AGRESSIVO" : "MODO: DEEP LINK");
            }
            LinearLayout r2 = (LinearLayout) p.getChildAt(4);
            if (r2.getChildCount() == 2) {
                ((Button) r2.getChildAt(1)).setText("CLOUD PC: " + (Prefs.cloudPc(this) ? "ON" : "OFF"));
            }
            LinearLayout r3 = (LinearLayout) p.getChildAt(5);
            if (r3.getChildCount() == 2) {
                ((Button) r3.getChildAt(0)).setText("ESPERA: " + Prefs.delay(this) + "ms");
                ((Button) r3.getChildAt(1)).setText("TENT: " + Prefs.attempts(this));
            }
        }
    }
}
