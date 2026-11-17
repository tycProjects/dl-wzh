package com.lsupra.fpsmeter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

/** Sau khi khởi động máy: nếu đang bật khóa Hz thì bật lại dịch vụ canh Hz. */
public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c, Intent i) {
        try {
            if (c.getSharedPreferences("s", Context.MODE_PRIVATE).getBoolean("hzOn", false)) {
                Intent s = new Intent(c, HzService.class);
                if (Build.VERSION.SDK_INT >= 26) c.startForegroundService(s); else c.startService(s);
            }
        } catch (Throwable ignored) {}
    }
}
