package com.ryan.autotyper;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * PrefsHelper — thin wrapper around SharedPreferences for:
 *
 *   • Typing config (rows / typingMode / delayMs / suffixName)
 *   • Theme mode (system / light / dark)
 *   • Recent files (top 10, deduped by Uri string, newest first)
 *
 * All access is synchronous and cheap; safe to call from the main thread.
 */
public final class PrefsHelper {

    private static final String PREFS = "yemvt_prefs";
    private static final int RECENT_MAX = 10;

    // Config keys
    private static final String K_ROWS    = "cfg.rows";
    private static final String K_MODE    = "cfg.mode";
    private static final String K_DELAY   = "cfg.delay_ms";
    private static final String K_SUFFIX  = "cfg.suffix";
    private static final String K_THEME   = "ui.theme";  // 0=system, 1=light, 2=dark

    // Recent files (a single JSON-encoded array)
    private static final String K_RECENT  = "files.recent";

    /**
     * In-memory cache of the parsed recent-files list, keyed by the raw JSON
     * string currently stored in prefs. Lookup is O(1) on cache hit and avoids
     * re-parsing the JSON on every UI refresh (FilesFragment.refreshRecentList
     * can fire several times per second during typing in the search box).
     *
     * The cache is process-wide (SharedPreferences are process-wide too), and
     * is invalidated by any mutation in this class (push / forget / clear).
     */
    private static volatile String cachedRecentJson = null;
    private static volatile List<RecentFile> cachedRecentList = Collections.emptyList();

    private PrefsHelper() { /* no instances */ }

    private static SharedPreferences sp(Context ctx) {
        return ctx.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    // ============================================================
    // Config
    // ============================================================

    public static void saveConfig(@NonNull Context ctx, @NonNull AppState s) {
        SharedPreferences.Editor e = sp(ctx).edit();
        e.putInt(K_ROWS, s.getRows());
        e.putInt(K_MODE, s.getTypingMode());
        e.putInt(K_DELAY, s.getDelayMs());
        e.putString(K_SUFFIX, s.getSuffixName());
        e.apply();
    }

    /** Restores config into AppState; called once at app start. */
    public static void restoreConfig(@NonNull Context ctx, @NonNull AppState s) {
        SharedPreferences p = sp(ctx);
        s.setRows(p.getInt(K_ROWS, 1));
        s.setTypingMode(p.getInt(K_MODE, AppState.MODE_TYPE_CHAR));
        s.setDelayMs(p.getInt(K_DELAY, 1000));
        s.setSuffixName(p.getString(K_SUFFIX, ""));
    }

    // ============================================================
    // Theme
    // ============================================================

    /** Theme mode constants. */
    public static final int THEME_SYSTEM = 0;
    public static final int THEME_LIGHT  = 1;
    public static final int THEME_DARK   = 2;

    public static int getThemeMode(@NonNull Context ctx) {
        return sp(ctx).getInt(K_THEME, THEME_SYSTEM);
    }

    public static void setThemeMode(@NonNull Context ctx, int mode) {
        sp(ctx).edit().putInt(K_THEME, mode).apply();
        applyTheme(mode);
    }

    /** Apply the saved theme mode to AppCompat. Call once at app start (in MainActivity.onCreate). */
    public static void applySavedTheme(@NonNull Context ctx) {
        applyTheme(getThemeMode(ctx));
    }

    private static void applyTheme(int mode) {
        switch (mode) {
            case THEME_LIGHT:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case THEME_DARK:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }

    // ============================================================
    // Recent files
    // ============================================================

    /**
     * A recent file entry. {@code uri} is the SAF Uri (persistable).
     * {@code name} is the display name shown in the UI.
     * {@code openedAt} is the load timestamp (millis).
     * {@code sizeBytes} is the file size in bytes (0 if unknown).
     */
    public static final class RecentFile {
        public final String uri;
        public final String name;
        public final long   openedAt;
        public final long   sizeBytes;

        public RecentFile(String uri, String name, long openedAt) {
            this(uri, name, openedAt, 0);
        }

        public RecentFile(String uri, String name, long openedAt, long sizeBytes) {
            this.uri = uri;
            this.name = name;
            this.openedAt = openedAt;
            this.sizeBytes = sizeBytes;
        }

        public Uri toUri() {
            return uri != null ? Uri.parse(uri) : null;
        }
    }

    /** Returns the recent file list, newest first (max 10). Uses an in-memory cache. */
    @NonNull
    public static List<RecentFile> getRecentFiles(@NonNull Context ctx) {
        String json = sp(ctx).getString(K_RECENT, null);
        if (json == null) json = "";
        // Cache hit: same JSON as last time → return the cached list as-is.
        if (json.equals(cachedRecentJson)) {
            return cachedRecentList;
        }
        // Cache miss: parse fresh.
        List<RecentFile> out;
        if (json.isEmpty()) {
            out = Collections.emptyList();
        } else {
            out = new ArrayList<>();
            try {
                JSONArray arr = new JSONArray(json);
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.optJSONObject(i);
                    if (o == null) continue;
                    String uri = o.optString("uri", null);
                    String name = o.optString("name", "");
                    long ts = o.optLong("openedAt", 0);
                    long size = o.optLong("sizeBytes", 0);
                    if (uri != null) out.add(new RecentFile(uri, name, ts, size));
                }
            } catch (JSONException ignored) {
                out = Collections.emptyList();
            }
        }
        cachedRecentJson = json;
        cachedRecentList = out;
        return out;
    }

    /** Serialise a list of RecentFile → JSON string. */
    private static String toJson(@NonNull List<RecentFile> list) {
        JSONArray arr = new JSONArray();
        for (RecentFile f : list) {
            JSONObject o = new JSONObject();
            try {
                o.put("uri", f.uri);
                o.put("name", f.name);
                o.put("openedAt", f.openedAt);
                o.put("sizeBytes", f.sizeBytes);
                arr.put(o);
            } catch (JSONException ignored) { /* skip */ }
        }
        return arr.toString();
    }

    /** Write the list to prefs and invalidate the cache. */
    private static void writeRecents(@NonNull Context ctx, @NonNull List<RecentFile> list) {
        String json = toJson(list);
        sp(ctx).edit().putString(K_RECENT, json).apply();
        // Update cache immediately so the next getRecentFiles() is a hit.
        cachedRecentJson = json;
        cachedRecentList = list;
    }

    /**
     * Push a file onto the top of the recent list. Dedupes by Uri string.
     * Caps to {@link #RECENT_MAX} entries.
     */
    public static void pushRecentFile(@NonNull Context ctx, @NonNull Uri uri, @NonNull String name) {
        pushRecentFile(ctx, uri, name, 0);
    }

    /** Push a file with size in bytes (used for sorting by size + display). */
    public static void pushRecentFile(@NonNull Context ctx, @NonNull Uri uri, @NonNull String name, long sizeBytes) {
        List<RecentFile> current = new ArrayList<>(getRecentFiles(ctx));
        String key = uri.toString();
        // Remove any existing entry with the same uri
        for (Iterator<RecentFile> it = current.iterator(); it.hasNext(); ) {
            if (key.equals(it.next().uri)) it.remove();
        }
        // Insert at the top
        current.add(0, new RecentFile(key, name, System.currentTimeMillis(), sizeBytes));
        // Trim
        while (current.size() > RECENT_MAX) current.remove(current.size() - 1);
        writeRecents(ctx, current);
    }

    /** Remove a single entry from the recent list (e.g. user long-press → "Forget"). */
    public static void forgetRecentFile(@NonNull Context ctx, @NonNull String uriString) {
        List<RecentFile> current = new ArrayList<>(getRecentFiles(ctx));
        for (Iterator<RecentFile> it = current.iterator(); it.hasNext(); ) {
            if (uriString.equals(it.next().uri)) it.remove();
        }
        writeRecents(ctx, current);
    }

    /** Wipe all recent files. */
    public static void clearRecentFiles(@NonNull Context ctx) {
        sp(ctx).edit().remove(K_RECENT).apply();
        cachedRecentJson = "";
        cachedRecentList = Collections.emptyList();
    }

    // ============================================================
    // Misc boolean prefs (used for onboarding-seen flag etc.)
    // ============================================================

    public static boolean getBoolean(@NonNull Context ctx, @NonNull String key, boolean def) {
        return sp(ctx).getBoolean(key, def);
    }

    public static void putBoolean(@NonNull Context ctx, @NonNull String key, boolean v) {
        sp(ctx).edit().putBoolean(key, v).apply();
    }

    /** Reset all config to defaults. */
    public static void resetConfig(@NonNull Context ctx, @NonNull AppState s) {
        s.setRows(1);
        s.setTypingMode(AppState.MODE_TYPE_CHAR);
        s.setDelayMs(1000);
        s.setSuffixName("");
        sp(ctx).edit()
                .remove(K_ROWS)
                .remove(K_MODE)
                .remove(K_DELAY)
                .remove(K_SUFFIX)
                .apply();
    }
}
