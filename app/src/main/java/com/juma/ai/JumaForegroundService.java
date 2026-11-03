package com.juma.ai;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;

public class JumaForegroundService extends Service {
    public static final String ACTION_START = "com.juma.ai.action.START_FOREGROUND";
    public static final String ACTION_STOP = "com.juma.ai.action.STOP_FOREGROUND";
    public static final String CHANNEL_ID = "juma_foreground_channel";
    public static final int NOTIFICATION_ID = 2001;

    private static boolean isServiceRunning = false;

    public static boolean isRunning() {
        return isServiceRunning;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopForeground(true);
            stopSelf();
            isServiceRunning = false;
            return START_NOT_STICKY;
        }

        // Android 14 (API 34) Compliance:
        // A foreground service declared with foregroundServiceType="microphone" strictly requires
        // android.permission.RECORD_AUDIO. If not granted, attempting to start with microphone type
        // will throw a SecurityException.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                isServiceRunning = false;
                stopSelf();
                return START_NOT_STICKY;
            }
        }

        try {
            Notification notification = buildForegroundNotification();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Must match manifest foregroundServiceType="microphone"
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE);
            } else {
                startForeground(NOTIFICATION_ID, notification);
            }
            isServiceRunning = true;
        } catch (Exception e) {
            // Catches ForegroundServiceStartNotAllowedException on Android 14 if started from background
            // or SecurityException if system constraints are violated.
            isServiceRunning = false;
            stopSelf();
            return START_NOT_STICKY;
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isServiceRunning = false;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "JUMA AI Foreground Assistant",
                    NotificationManager.IMPORTANCE_LOW
                );
                channel.setDescription("Maintains JUMA voice readiness and active assistant monitoring.");
                nm.createNotificationChannel(channel);
            }
        }
    }

    private Notification buildForegroundNotification() {
        Intent launchIntent = new Intent(this, MainActivity.class);
        launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0
        );

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
        }

        builder.setContentTitle("JUMA AI (জুমা AI)")
               .setContentText("Voice Assistant active • Ready for commands")
               .setSmallIcon(android.R.drawable.ic_btn_speak_now)
               .setContentIntent(pendingIntent)
               .setOngoing(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setCategory(Notification.CATEGORY_SERVICE);
        }

        return builder.build();
    }
}
