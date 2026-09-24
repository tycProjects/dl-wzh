package com.hoangmod.shizuku;

import android.app.*;
import android.content.*;
import android.os.Build;

public class ExpiryReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission("android.permission.POST_NOTIFICATIONS")
                != android.content.pm.PackageManager.PERMISSION_GRANTED) return;

        Notification.Builder n = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(context, "daily_key_channel")
                : new Notification.Builder(context);
        n.setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("HOÀNG MOD • Key đã hết hạn")
                .setContentText("Key 24h đã hết hạn. Mở app để nhận key mới.")
                .setAutoCancel(true);
        ((NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE))
                .notify(9002, n.build());
    }
}
