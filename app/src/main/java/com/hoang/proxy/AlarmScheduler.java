package com.hoang.proxy;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

public final class AlarmScheduler {
    private AlarmScheduler() {}

    public static void scheduleNext(Context context) {
        long remaining = KeyManager.remainingMs(context);
        long trigger = System.currentTimeMillis() + Math.max(1000L, remaining);

        AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(context, KeyAlarmReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(
                context, 99, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        alarm.cancel(pi);
        if (android.os.Build.VERSION.SDK_INT >= 23) {
            alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi);
        } else {
            alarm.setExact(AlarmManager.RTC_WAKEUP, trigger, pi);
        }
    }
}
