package com.sharingun.xyz;

/**
 * HeadshotEngine
 * Holds the current mode state used by the overlay.
 * MODE_OFF  → panel idle
 * MODE_80   → 80% headshot sensitivity config
 * MODE_100  → 100% headshot sensitivity config
 */
public class HeadshotEngine {

    public static final int MODE_OFF  = 0;
    public static final int MODE_80   = 1;
    public static final int MODE_100  = 2;

    private static int currentMode = MODE_OFF;

    public static void setMode(int mode) {
        currentMode = mode;
    }

    public static int getMode() {
        return currentMode;
    }

    public static boolean isActive() {
        return currentMode != MODE_OFF;
    }

    // Sensitivity values tuned for Free Fire 1.132.x
    // These are config values stored and readable by the panel UI
    public static float getSensitivityX() {
        switch (currentMode) {
            case MODE_80:  return 78f;   // ~80% headshot zone bias
            case MODE_100: return 95f;   // ~100% headshot zone bias
            default:       return 50f;
        }
    }

    public static float getSensitivityY() {
        switch (currentMode) {
            case MODE_80:  return 72f;
            case MODE_100: return 90f;
            default:       return 50f;
        }
    }
}
