package com.ffproxy.engine;

import com.ffproxy.util.RootUtil;

/**
 * Auto-detect Free Fire install path → backup → replace files
 * when user toggles a feature.
 *
 * Real effect depends on having correct modded files in /sdcard/FFProxy/mods/
 * matching current game version. Generic replace of libil2cpp without proper
 * patch will crash or get detected.
 */
public class FeatureEngine {

    private static final String MOD_DIR = "/sdcard/FFProxy/mods/";
    private static final String BAK_DIR = "/sdcard/FFProxy/backup/";
    private static final String LOG = "/sdcard/FFProxy/engine.log";

    /** Common Free Fire package names by region */
    private static final String[] PACKAGES = {
            "com.dts.freefireth",
            "com.dts.freefiremax",
            "com.dts.freefirein",
            "com.garena.game.freefire",
            "com.garena.game.kgth"
    };

    private static String detectedPkg = null;
    private static String dataDir = null;
    private static String libDir = null;
    private static String obbDir = null;

    /** Scan and cache game paths */
    public static String scanGamePaths() {
        StringBuilder report = new StringBuilder();
        detectedPkg = null;
        dataDir = null;
        libDir = null;
        obbDir = null;

        for (String pkg : PACKAGES) {
            String path = "/data/data/" + pkg;
            String check = RootUtil.su("test -d " + path + " && echo OK");
            if (check != null && check.contains("OK")) {
                detectedPkg = pkg;
                dataDir = path;
                // native libs
                String libCheck = RootUtil.su("ls " + path + "/lib/ 2>/dev/null | head -1");
                if (libCheck != null && !libCheck.isEmpty() && !libCheck.contains("ERR")) {
                    libDir = path + "/lib";
                } else {
                    // split APK / extracted
                    String alt = RootUtil.su("find " + path + " -name 'libil2cpp.so' 2>/dev/null | head -1");
                    if (alt != null && alt.contains("libil2cpp")) {
                        libDir = alt.replace("/libil2cpp.so", "").trim();
                    }
                }
                // OBB
                String obb = "/storage/emulated/0/Android/obb/" + pkg;
                String obbOk = RootUtil.su("test -d " + obb + " && echo OK");
                if (obbOk != null && obbOk.contains("OK")) obbDir = obb;

                report.append("PKG: ").append(pkg).append("\n");
                report.append("DATA: ").append(dataDir).append("\n");
                report.append("LIB: ").append(libDir != null ? libDir : "not found").append("\n");
                report.append("OBB: ").append(obbDir != null ? obbDir : "not found").append("\n");
                break;
            }
        }

        if (detectedPkg == null) {
            return "Không tìm thấy Free Fire. Cài game trước.";
        }

        RootUtil.su("mkdir -p " + MOD_DIR + " " + BAK_DIR);
        RootUtil.su("echo '" + report.toString().replace("'", "") + "' > " + LOG);
        return report.toString();
    }

    /**
     * Called immediately when user toggles a feature ON.
     * Finds target files and replaces from mods/ folder.
     */
    public static String autoReplace(String feature) {
        if (detectedPkg == null) {
            String scan = scanGamePaths();
            if (detectedPkg == null) return scan;
        }

        RootUtil.su("mkdir -p " + MOD_DIR + feature + " " + BAK_DIR + feature);

        switch (feature) {
            case "esp":
            case "aimHead":
            case "aimNeck":
            case "magicBullet":
            case "aimDrag":
                return replaceNativeLibs(feature);
            case "highFps":
                return applyHighFps();
            case "antiban":
                return applyAntiban();
            default:
                return "Unknown feature: " + feature;
        }
    }

    /** Replace libil2cpp / related .so if mod file exists */
    private static String replaceNativeLibs(String feature) {
        if (libDir == null) {
            return "Không tìm thấy thư mục lib. Scan lại.";
        }

        // Expected mod files user must place:
        // /sdcard/FFProxy/mods/<feature>/libil2cpp.so
        // or /sdcard/FFProxy/mods/common/libil2cpp.so
        String[] candidates = {
                MOD_DIR + feature + "/libil2cpp.so",
                MOD_DIR + "common/libil2cpp.so",
                MOD_DIR + "libil2cpp.so"
        };

        String modFile = null;
        for (String c : candidates) {
            String ok = RootUtil.su("test -f " + c + " && echo OK");
            if (ok != null && ok.contains("OK")) {
                modFile = c;
                break;
            }
        }

        if (modFile == null) {
            // No mod file → write Frida script fallback + try inject
            writeFridaFallback(feature);
            RootUtil.su("frida -U -n " + detectedPkg + " -l " + MOD_DIR + feature + ".js --no-pause >/dev/null 2>&1 &");
            return "Chưa có file mod .so cho [" + feature + "].\n"
                    + "Đặt libil2cpp.so vào:\n"
                    + MOD_DIR + feature + "/\n"
                    + "Tạm dùng Frida script fallback.";
        }

        // Backup original once
        String target = libDir + "/libil2cpp.so";
        // Also try arm64-v8a / armeabi-v7a subdirs
        String found = RootUtil.su("find " + dataDir + " -name 'libil2cpp.so' 2>/dev/null | head -1");
        if (found != null && found.contains("libil2cpp")) {
            target = found.trim();
        }

        String bak = BAK_DIR + feature + "/libil2cpp.so";
        String hasBak = RootUtil.su("test -f " + bak + " && echo OK");
        if (hasBak == null || !hasBak.contains("OK")) {
            RootUtil.su("cp -f '" + target + "' '" + bak + "'");
        }

        // Replace
        String r = RootUtil.su("cp -f '" + modFile + "' '" + target + "' && chmod 755 '" + target + "' && echo DONE");
        if (r != null && r.contains("DONE")) {
            // Force-stop so next launch loads new lib
            RootUtil.su("am force-stop " + detectedPkg);
            return "✓ Đã thay libil2cpp.so cho [" + feature + "]\n"
                    + "Target: " + target + "\n"
                    + "Mở lại Free Fire để có hiệu lực.";
        }
        return "Thay file thất bại. Kiểm tra SELinux / path.\n" + r;
    }

    private static void writeFridaFallback(String feature) {
        String js = "// Auto fallback for " + feature + "\n"
                + "Java.perform(function(){\n"
                + "  console.log('[FF] " + feature + " fallback loaded');\n"
                + "  // TODO: live offsets\n"
                + "});\n";
        RootUtil.su("cat > " + MOD_DIR + feature + ".js << 'EOF'\n" + js + "EOF");
    }

    private static String applyHighFps() {
        RootUtil.su("setprop debug.sf.high_fps 1 2>/dev/null");
        RootUtil.su("settings put system pointer_speed 7 2>/dev/null");
        // Touch game prefs if exist
        if (dataDir != null) {
            RootUtil.su("chmod -R 777 " + dataDir + "/shared_prefs 2>/dev/null");
        }
        return "✓ High FPS flags applied";
    }

    private static String applyAntiban() {
        RootUtil.su("setprop ro.debuggable 0 2>/dev/null");
        RootUtil.su("resetprop ro.boot.verifiedbootstate green 2>/dev/null");
        RootUtil.su("resetprop ro.boot.flash.locked 1 2>/dev/null");
        return "✓ Antiban basic props set (mitigation only)";
    }

    /** Restore original file for one feature */
    public static void restore(String feature) {
        String bak = BAK_DIR + feature + "/libil2cpp.so";
        String ok = RootUtil.su("test -f " + bak + " && echo OK");
        if (ok != null && ok.contains("OK") && dataDir != null) {
            String found = RootUtil.su("find " + dataDir + " -name 'libil2cpp.so' 2>/dev/null | head -1");
            if (found != null && found.contains("libil2cpp")) {
                RootUtil.su("cp -f '" + bak + "' '" + found.trim() + "'");
                RootUtil.su("am force-stop " + detectedPkg);
            }
        }
    }

    public static void restoreAll() {
        for (String f : new String[]{"esp", "aimHead", "aimNeck", "magicBullet", "aimDrag"}) {
            restore(f);
        }
        RootUtil.su("killall frida 2>/dev/null");
    }

    public static String applyAll() {
        if (detectedPkg == null) scanGamePaths();
        StringBuilder sb = new StringBuilder();
        if (FeatureConfig.esp) sb.append(autoReplace("esp")).append("\n");
        if (FeatureConfig.aimHead) sb.append(autoReplace("aimHead")).append("\n");
        if (FeatureConfig.aimNeck) sb.append(autoReplace("aimNeck")).append("\n");
        if (FeatureConfig.magicBullet) sb.append(autoReplace("magicBullet")).append("\n");
        if (FeatureConfig.aimDrag) sb.append(autoReplace("aimDrag")).append("\n");
        if (FeatureConfig.highFps) sb.append(autoReplace("highFps")).append("\n");
        if (FeatureConfig.antiban) sb.append(autoReplace("antiban")).append("\n");
        if (sb.length() == 0) return "Chưa bật tính năng nào";
        return sb.toString().trim();
    }
}
