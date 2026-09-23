package com.hoang.proxy;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

public final class NotificationHelper {
    public static final String CHANNEL = "hoang_proxy_daily_key";
    public static final int NOTIFICATION_ID = 1200;

    private NotificationHelper() {}

    public static void show(Context context) {
        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(new NotificationChannel(
                    CHANNEL, "HOANG PROXY - Key", NotificationManager.IMPORTANCE_HIGH));
        }

        if (Build.VERSION.SDK_INT >= 33 &&
                context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        String key = KeyManager.getOrCreate(context);

        Intent open = new Intent(context, MainActivity.class);
        PendingIntent pending = PendingIntent.getActivity(
                context, 10, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        android.app.Notification.Builder b;
        if (Build.VERSION.SDK_INT >= 26) {
            b = new android.app.Notification.Builder(context, CHANNEL);
        } else {
            b = new android.app.Notification.Builder(context);
        }

        b.setSmallIcon(android.R.drawable.ic_lock_idle_lock)
                .setContentTitle("HOANG PROXY")
                .setContentText("Key: " + key + " • Hạn dùng 12 giờ")
                .setStyle(new android.app.Notification.BigTextStyle()
                        .bigText("Đây là key " + key +
                                "\nHạn sử dụng: 12 giờ.\nBắt đầu ngày mới thôi 🎉"))
                .setContentIntent(pending)
                .setAutoCancel(false)
                .setOngoing(false)
                .setPriority(android.app.Notification.PRIORITY_HIGH);

        manager.notify(NOTIFICATION_ID, b.build());
    }
}
