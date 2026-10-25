package com.ffproxy.engine;

public class FeatureConfig {
    public static volatile boolean esp = false;
    public static volatile boolean aimHead = false;
    public static volatile boolean aimNeck = false;
    public static volatile boolean magicBullet = false;
    public static volatile boolean aimDrag = false;
    public static volatile boolean highFps = false;
    public static volatile boolean antiban = false;

    public static void set(String key, boolean v) {
        switch (key) {
            case "esp": esp = v; break;
            case "aimHead": aimHead = v; break;
            case "aimNeck": aimNeck = v; break;
            case "magicBullet": magicBullet = v; break;
            case "aimDrag": aimDrag = v; break;
            case "highFps": highFps = v; break;
            case "antiban": antiban = v; break;
        }
    }

    public static void reset() {
        esp = aimHead = aimNeck = magicBullet = aimDrag = highFps = antiban = false;
    }
}
