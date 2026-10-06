package com.example.floatingwindow;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

/**
 * Service yang menampilkan jendela mengambang (floating window / overlay)
 * di atas aplikasi lain, mirip "chat head" Messenger.
 *
 * Alur:
 *  - bubble kecil muncul dan bisa di-drag ke mana saja di layar
 *  - tap pada bubble (tanpa drag) akan membuka panel yang lebih besar
 *  - tombol ✕ pada panel menutup panel dan mengembalikan ke bentuk bubble
 */
public class FloatingWindowService extends Service {

    private static final String CHANNEL_ID = "floating_window_channel";
    private static final int NOTIFICATION_ID = 1;

    private WindowManager windowManager;

    private View bubbleView;
    private WindowManager.LayoutParams bubbleParams;

    private View expandedView;
    private WindowManager.LayoutParams expandedParams;

    private boolean isExpanded = false;

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        createBubble();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIFICATION_ID, buildNotification());
        return START_STICKY;
    }

    // ---------- Bubble (jendela mengambang kecil) ----------

    private void createBubble() {
        bubbleView = LayoutInflater.from(this).inflate(R.layout.layout_floating_widget, null);

        int layoutFlag = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        bubbleParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );
        bubbleParams.gravity = Gravity.TOP | Gravity.START;
        bubbleParams.x = 0;
        bubbleParams.y = 300;

        windowManager.addView(bubbleView, bubbleParams);
        attachDragAndClickBehavior();
    }

    /** Drag untuk memindahkan bubble, tap singkat (tanpa gerak) untuk membuka panel. */
    private void attachDragAndClickBehavior() {
        bubbleView.setOnTouchListener(new View.OnTouchListener() {
            private int initialX, initialY;
            private float initialTouchX, initialTouchY;
            private long touchDownTime;
            private static final int CLICK_DRAG_TOLERANCE = 10;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = bubbleParams.x;
                        initialY = bubbleParams.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        touchDownTime = System.currentTimeMillis();
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        bubbleParams.x = initialX + (int) (event.getRawX() - initialTouchX);
                        bubbleParams.y = initialY + (int) (event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(bubbleView, bubbleParams);
                        return true;

                    case MotionEvent.ACTION_UP:
                        int movedX = Math.abs((int) (event.getRawX() - initialTouchX));
                        int movedY = Math.abs((int) (event.getRawY() - initialTouchY));
                        boolean wasClick = movedX < CLICK_DRAG_TOLERANCE
                                && movedY < CLICK_DRAG_TOLERANCE
                                && (System.currentTimeMillis() - touchDownTime) < 300;
                        if (wasClick) {
                            showExpandedPanel();
                        }
                        return true;
                }
                return false;
            }
        });
    }

    // ---------- Panel yang diperluas ----------

    private void showExpandedPanel() {
        if (isExpanded) return;
        isExpanded = true;

        windowManager.removeView(bubbleView);

        expandedView = LayoutInflater.from(this).inflate(R.layout.layout_floating_expanded, null);

        int layoutFlag = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        expandedParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );
        expandedParams.gravity = Gravity.TOP | Gravity.START;
        expandedParams.x = bubbleParams.x;
        expandedParams.y = bubbleParams.y;

        windowManager.addView(expandedView, expandedParams);

        View header = expandedView.findViewById(R.id.headerDrag);
        header.setOnTouchListener(new View.OnTouchListener() {
            private int initialX, initialY;
            private float initialTouchX, initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = expandedParams.x;
                        initialY = expandedParams.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        expandedParams.x = initialX + (int) (event.getRawX() - initialTouchX);
                        expandedParams.y = initialY + (int) (event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(expandedView, expandedParams);
                        return true;
                }
                return false;
            }
        });

        expandedView.findViewById(R.id.btnClose).setOnClickListener(v -> closeExpandedPanel());
    }

    private void closeExpandedPanel() {
        if (!isExpanded) return;
        isExpanded = false;

        bubbleParams.x = expandedParams.x;
        bubbleParams.y = expandedParams.y;

        windowManager.removeView(expandedView);
        expandedView = null;

        windowManager.addView(bubbleView, bubbleParams);
    }

    // ---------- Notifikasi foreground (wajib untuk layanan latar depan) ----------

    private Notification buildNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Jendela Mengambang",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent,
                PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Jendela Mengambang aktif")
                .setContentText("Ketuk untuk membuka aplikasi")
                .setSmallIcon(android.R.drawable.ic_menu_myplaces)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (bubbleView != null && bubbleView.getWindowToken() != null) {
            windowManager.removeView(bubbleView);
        }
        if (expandedView != null && expandedView.getWindowToken() != null) {
            windowManager.removeView(expandedView);
        }
    }
}
