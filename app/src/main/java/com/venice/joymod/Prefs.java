package com.venice.joymod;

import android.content.Context;
import android.content.SharedPreferences;

public final class Prefs {

    private static SharedPreferences p(Context c) {
        return c.getSharedPreferences("joymod", Context.MODE_PRIVATE);
    }

    public static int mode(Context c) { return p(c).getInt("mode", 1); }
    public static void setMode(Context c, int v) { p(c).edit().putInt("mode", v).apply(); }

    public static int attempts(Context c) { return p(c).getInt("attempts", 4); }
    public static void setAttempts(Context c, int v) { p(c).edit().putInt("attempts", v).apply(); }

    public static int delay(Context c) { return p(c).getInt("delay", 600); }
    public static void setDelay(Context c, int v) { p(c).edit().putInt("delay", v).apply(); }

    public static boolean auto(Context c) { return p(c).getBoolean("auto", false); }
    public static void setAuto(Context c, boolean v) { p(c).edit().putBoolean("auto", v).apply(); }

    public static String target(Context c) { return p(c).getString("target", ""); }
    public static void setTarget(Context c, String v) { p(c).edit().putString("target", v).apply(); }

    public static boolean cloudPc(Context c) { return p(c).getBoolean("cloudpc", false); }
    public static void setCloudPc(Context c, boolean v) { p(c).edit().putBoolean("cloudpc", v).apply(); }
}
