package com.sharingun.xyz;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.app.NotificationCompat;

public class OverlayService extends Service {

    public static boolean isRunning = false;

    private WindowManager windowManager;
    private View overlayView;
    private View panelView;

    private WindowManager.LayoutParams iconParams;
    private WindowManager.LayoutParams panelParams;

    private boolean panelVisible = false;

    private static final String CHANNEL_ID = "sharingun_channel";

    // Touch drag variables
    private int initialX, initialY;
    private float initialTouchX, initialTouchY;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        isRunning = true;
        startForegroundNotification();
        setupOverlay();
        return START_STICKY;
    }

    private void startForegroundNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Sharingun XYZ",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Sharingun XYZ Headshot Panel");
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(channel);
        }

        Intent notifIntent = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(
            this, 0, notifIntent,
            PendingIntent.FLAG_IMMUTABLE
        );

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("𝐒𝐡𝐚𝐫𝐢𝐧𝐠𝐮𝐧 𝐗𝐘𝐙")
            .setContentText("Headshot Panel Active")
            .setSmallIcon(R.drawable.ic_eye)
            .setContentIntent(pi)
            .setOngoing(true)
            .build();

        startForeground(1, notification);
    }

    private void setupOverlay() {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        int overlayType = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
            ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            : WindowManager.LayoutParams.TYPE_PHONE;

        // ── Floating ICON ──────────────────────────────────────────────
        LayoutInflater inflater = LayoutInflater.from(this);
        overlayView = inflater.inflate(R.layout.overlay_icon, null);

        iconParams = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        );
        iconParams.gravity = Gravity.TOP | Gravity.START;
        iconParams.x = 0;
        iconParams.y = 300;

        windowManager.addView(overlayView, iconParams);

        // ── Panel ──────────────────────────────────────────────────────
        panelView = inflater.inflate(R.layout.overlay_panel, null);

        panelParams = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        );
        panelParams.gravity = Gravity.TOP | Gravity.START;
        panelParams.x = 60;
        panelParams.y = 200;

        windowManager.addView(panelView, panelParams);
        panelView.setVisibility(View.GONE);

        setupIconTouch();
        setupPanelButtons();
    }

    // ── Drag + tap for icon ────────────────────────────────────────────
    private void setupIconTouch() {
        ImageView icon = overlayView.findViewById(R.id.overlayIcon);

        icon.setOnTouchListener(new View.OnTouchListener() {
            long lastDown;
            boolean moved;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = iconParams.x;
                        initialY = iconParams.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        lastDown = System.currentTimeMillis();
                        moved = false;
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        int dx = (int)(event.getRawX() - initialTouchX);
                        int dy = (int)(event.getRawY() - initialTouchY);
                        if (Math.abs(dx) > 5 || Math.abs(dy) > 5) moved = true;
                        iconParams.x = initialX + dx;
                        iconParams.y = initialY + dy;
                        windowManager.updateViewLayout(overlayView, iconParams);
                        return true;

                    case MotionEvent.ACTION_UP:
                        if (!moved && System.currentTimeMillis() - lastDown < 300) {
                            togglePanel();
                        }
                        return true;
                }
                return false;
            }
        });
    }

    private void togglePanel() {
        panelVisible = !panelVisible;
        panelView.setVisibility(panelVisible ? View.VISIBLE : View.GONE);
        // Position panel near icon
        panelParams.x = iconParams.x + 80;
        panelParams.y = iconParams.y - 20;
        if (panelVisible) windowManager.updateViewLayout(panelView, panelParams);
    }

    // ── Panel button logic ─────────────────────────────────────────────
    private void setupPanelButtons() {
        Button btn80  = panelView.findViewById(R.id.btn80);
        Button btn100 = panelView.findViewById(R.id.btn100);
        Button btnClose = panelView.findViewById(R.id.btnClose);
        TextView modeLabel = panelView.findViewById(R.id.modeLabel);

        btn80.setOnClickListener(v -> {
            HeadshotEngine.setMode(HeadshotEngine.MODE_80);
            btn80.setAlpha(1f);
            btn100.setAlpha(0.5f);
            modeLabel.setText("80% Headshot ●");
            Toast.makeText(getApplicationContext(), "80% Headshot ON", Toast.LENGTH_SHORT).show();
        });

        btn100.setOnClickListener(v -> {
            HeadshotEngine.setMode(HeadshotEngine.MODE_100);
            btn100.setAlpha(1f);
            btn80.setAlpha(0.5f);
            modeLabel.setText("100% Headshot ●");
            Toast.makeText(getApplicationContext(), "100% Headshot ON", Toast.LENGTH_SHORT).show();
        });

        btnClose.setOnClickListener(v -> {
            panelView.setVisibility(View.GONE);
            panelVisible = false;
        });
    }

    @Override
    public void onDestroy() {
        isRunning = false;
        if (overlayView != null) windowManager.removeView(overlayView);
        if (panelView != null) windowManager.removeView(panelView);
        super.onDestroy();
    }
}
