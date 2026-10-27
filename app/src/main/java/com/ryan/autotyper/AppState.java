package com.ryan.autotyper;

import android.net.Uri;

import java.util.ArrayList;
import java.util.List;

/**
 * AppState — single source of truth shared across all fragments.
 *
 * Holds:
 *   - loaded file content (List<String> lines) + Uri + display name
 *   - user-configurable typing settings (rows / typingMode / delayMs / suffixName)
 *   - edited text (the user can edit the file inside FilesFragment; the edited text
 *     is split back into lines on demand so AutoTyperService always uses the latest
 *     version the user sees in the editor).
 */
public final class AppState {

    public static final int MODE_TYPE_CHAR = 0;   // "Gõ từng chữ"
    public static final int MODE_PASTE_LINE = 1;  // "Dán từng dòng"

    private static volatile AppState INSTANCE;

    private AppState() { /* no-op */ }

    public static AppState get() {
        if (INSTANCE == null) {
            synchronized (AppState.class) {
                if (INSTANCE == null) INSTANCE = new AppState();
            }
        }
        return INSTANCE;
    }

    // ===== Loaded file state =====
    private final List<String> lines = new ArrayList<>();
    private String fileName = "";
    private Uri fileUri = null;
    private long fileLoadedAt = 0L;

    // ===== Settings (read from ConfigFragment) =====
    private int rows = 1;
    private int typingMode = MODE_TYPE_CHAR;
    private int delayMs = 1000;
    private String suffixName = "";

    // ===== Edited text (kept in sync with FilesFragment's EditText) =====
    private String editedText = "";

    // ===== App foreground state (used to silence toasts when running in other apps) =====
    private volatile boolean appInForeground = false;

    public void setAppInForeground(boolean v) { appInForeground = v; }
    public boolean isAppInForeground() { return appInForeground; }

    // ---------- File ----------
    public synchronized List<String> getLinesSnapshot() {
        return new ArrayList<>(lines);
    }

    public synchronized void setLines(List<String> newLines, String name, Uri uri) {
        lines.clear();
        if (newLines != null) lines.addAll(newLines);
        fileName = name != null ? name : "";
        fileUri = uri;
        fileLoadedAt = System.currentTimeMillis();
        // Re-seed edited text from the loaded lines so the editor opens with the
        // original content (overwriting any previous edits belonging to another file).
        editedText = joinLines(lines);
    }

    public synchronized String getFileName() { return fileName; }
    public synchronized Uri getFileUri() { return fileUri; }
    public synchronized boolean hasFile() { return !lines.isEmpty() || fileUri != null; }
    public synchronized int getLineCount() { return lines.size(); }

    // ---------- Editor (real-time) ----------
    public synchronized void setEditedText(String text) {
        editedText = text != null ? text : "";
        // Refresh the lines list so AutoTyperService picks up edits immediately.
        lines.clear();
        lines.addAll(splitLines(editedText));
    }

    public synchronized String getEditedText() { return editedText; }

    // ---------- Settings ----------
    public synchronized int getRows() { return rows; }
    public synchronized void setRows(int v) { rows = clamp(v, 1, 5); }

    public synchronized int getTypingMode() { return typingMode; }
    public synchronized void setTypingMode(int v) {
        typingMode = (v == MODE_PASTE_LINE) ? MODE_PASTE_LINE : MODE_TYPE_CHAR;
    }

    public synchronized int getDelayMs() { return delayMs; }
    public synchronized void setDelayMs(int v) { delayMs = Math.max(0, v); }

    public synchronized String getSuffixName() { return suffixName; }
    public synchronized void setSuffixName(String s) { suffixName = s != null ? s : ""; }

    // ---------- Helpers ----------
    private static String joinLines(List<String> ls) {
        if (ls == null || ls.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ls.size(); i++) {
            if (i > 0) sb.append('\n');
            sb.append(ls.get(i));
        }
        return sb.toString();
    }

    private static List<String> splitLines(String text) {
        List<String> out = new ArrayList<>();
        if (text == null || text.isEmpty()) return out;
        int start = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\n') {
                out.add(text.substring(start, i));
                start = i + 1;
            }
        }
        out.add(text.substring(start)); // last segment (may be empty)
        return out;
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}
