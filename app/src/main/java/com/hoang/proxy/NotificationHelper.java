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

    public static void show(Context context) {
        if (Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) return;

        NotificationManager manager =
            (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(new NotificationChannel(
                CHANNEL,"HOANG PROXY - Key",NotificationManager.IMPORTANCE_HIGH));
        }

        String key = KeyManager.getOrCreate(context);
        Intent open = new Intent(context,MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(context,10,open,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        android.app.Notification.Builder b =
            Build.VERSION.SDK_INT >= 26
            ? new android.app.Notification.Builder(context,CHANNEL)
            : new android.app.Notification.Builder(context);

        b.setSmallIcon(android.R.drawable.ic_lock_idle_lock)
         .setContentTitle("HOANG PROXY")
         .setContentText("Key: "+key+" • Hạn dùng 12 giờ")
         .setStyle(new android.app.Notification.BigTextStyle()
             .bigText("Đây là key "+key+"\nHạn sử dụng: 12 giờ.\nBắt đầu ngày mới thôi 🎉"))
         .setContentIntent(pi)
         .setAutoCancel(true)
         .setPriority(android.app.Notification.PRIORITY_HIGH);

        manager.notify(NOTIFICATION_ID,b.build());
    }
}
