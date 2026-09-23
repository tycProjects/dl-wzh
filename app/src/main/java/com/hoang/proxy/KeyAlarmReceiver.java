package com.hoang.proxy;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class KeyAlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // Khi key 12h hết hạn, tạo key mới và đưa key mới vào thông báo.
        KeyManager.getOrCreate(context);
        NotificationHelper.show(context);
        AlarmScheduler.scheduleNext(context);
    }
}
