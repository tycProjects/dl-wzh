package com.pb.browser;

import org.json.JSONObject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Bo thiet bi mau: chon 1 thiet bi -> UA + UUID + fingerprint (platform/cores/mem/touch/screen/dpr) dong bo
final class Devices {
    static final class Dev {
        final String id, brand, name, ua, platform;
        final int cores, mem, touch, sw, sh;
        final double dpr;
        Dev(String id, String brand, String name, String ua, String platform,
            int cores, int mem, int touch, int sw, int sh, double dpr) {
            this.id = id; this.brand = brand; this.name = name; this.ua = ua; this.platform = platform;
            this.cores = cores; this.mem = mem; this.touch = touch; this.sw = sw; this.sh = sh; this.dpr = dpr;
        }
        JSONObject fp() {
            JSONObject o = new JSONObject();
            try {
                o.put("platform", platform); o.put("cores", cores); o.put("mem", mem);
                o.put("touch", touch); o.put("sw", sw); o.put("sh", sh); o.put("dpr", dpr);
                String[] g = gpuOf(brand, name);
                o.put("gpuV", g[0]); o.put("gpuR", g[1]);
            } catch (Exception ignored) {}
            return o;
        }
        String sub() { return sw + "×" + sh + " · " + mem + "GB · " + cores + " nhân · " + dpr + "x"; }
    }

    static final List<Dev> ALL = new ArrayList<>();
    static final Map<String, List<Dev>> GROUPS = new LinkedHashMap<>();

    static String ios(String ver, String safari) {
        return "Mozilla/5.0 (iPhone; CPU iPhone OS " + ver + " like Mac OS X) AppleWebKit/605.1.15 "
                + "(KHTML, like Gecko) Version/" + safari + " Mobile/15E148 Safari/604.1";
    }
    static String android(String ver, String model) {
        return "Mozilla/5.0 (Linux; Android " + ver + "; " + model + ") AppleWebKit/537.36 "
                + "(KHTML, like Gecko) Chrome/130.0.0.0 Mobile Safari/537.36";
    }
    static void add(String brand, String name, String ua, String platform,
                    int cores, int mem, int touch, int sw, int sh, double dpr) {
        String id = (brand + "-" + name).toLowerCase().replaceAll("[^a-z0-9]+", "-");
        Dev d = new Dev(id, brand, name, ua, platform, cores, mem, touch, sw, sh, dpr);
        ALL.add(d);
        List<Dev> g = GROUPS.get(brand);
        if (g == null) { g = new ArrayList<>(); GROUPS.put(brand, g); }
        g.add(d);
    }

    // ===== 50 User-Agent tao san cho moi thiet bi (idx 0 = UA goc; 1..49 = bien the) =====
    static final int UA_COUNT = 50;
    static final String[] IOS_VERS = ("15_0 15_0_2 15_1 15_1_1 15_2 15_2_1 15_3 15_3_1 15_4 15_4_1 15_5 15_6 15_6_1 15_7 15_7_1 15_8 "
            + "16_0 16_0_2 16_0_3 16_1 16_1_1 16_1_2 16_2 16_3 16_3_1 16_4 16_4_1 16_5 16_5_1 16_6 16_6_1 16_7 "
            + "17_0 17_0_3 17_1 17_1_1 17_1_2 17_2 17_2_1 17_3 17_3_1 17_4 17_4_1 17_5 17_5_1 17_6 17_6_1 17_7 "
            + "18_0_1 18_1 18_1_1 18_2 18_2_1 18_3 18_3_1 18_3_2 18_4 18_4_1 18_5 18_6").split(" ");
    static String uaAt(Dev d, int idx) {
        if (d == null) return "";
        if (idx <= 0) return d.ua;
        int i = (idx - 1) % (UA_COUNT - 1);
        if (d.ua.contains("iPhone")) {
            String v = IOS_VERS[i % IOS_VERS.length];
            String[] p = v.split("_");
            return ios(v, p[0] + "." + p[1]);
        }
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("Android (\\d+); ([^)]+)\\)").matcher(d.ua);
        if (!m.find()) return d.ua;
        int base = Integer.parseInt(m.group(1));
        int lo = Math.max(10, base - 2), hi = Math.min(15, base + 1), nv = hi - lo + 1;
        int chrome = 120 + i / nv;
        if (chrome >= 130) chrome++;
        return "Mozilla/5.0 (Linux; Android " + (lo + i % nv) + "; " + m.group(2) + ") AppleWebKit/537.36 "
                + "(KHTML, like Gecko) Chrome/" + chrome + ".0.0.0 Mobile Safari/537.36";
    }
    static Dev byId(String id) {
        if (id == null) return null;
        for (Dev d : ALL) if (d.id.equals(id)) return d;
        return null;
    }


    // GPU that cua tung may (WebGL UNMASKED_VENDOR/RENDERER) de khop voi UA + phan cung
    static final Map<String, String> GPU = new LinkedHashMap<>();
    static {
        GPU.put("Galaxy S21", "Adreno (TM) 660"); GPU.put("Galaxy S22 Ultra", "Adreno (TM) 730");
        GPU.put("Galaxy S23", "Adreno (TM) 740"); GPU.put("Galaxy S24 Ultra", "Adreno (TM) 750");
        GPU.put("Galaxy Z Fold5", "Adreno (TM) 740");
        GPU.put("Redmi Note 12", "Adreno (TM) 610"); GPU.put("Redmi Note 13 Pro", "Mali-G57 MC2");
        GPU.put("Xiaomi 13", "Adreno (TM) 740"); GPU.put("Xiaomi 14", "Adreno (TM) 750");
        GPU.put("POCO X6 Pro", "Mali-G615 MC6");
        GPU.put("Find X5", "Adreno (TM) 730"); GPU.put("Find X7 Ultra", "Adreno (TM) 750");
        GPU.put("Reno 11", "Mali-G610 MC4"); GPU.put("Reno 12", "Mali-G615 MC2");
        GPU.put("Vivo X90", "Mali-G710 MC10"); GPU.put("Vivo X100 Pro", "Mali-G720-Immortalis MC12");
        GPU.put("Vivo V29", "Adreno (TM) 642L"); GPU.put("Vivo Y36", "Adreno (TM) 610");
        GPU.put("Xperia 1 V", "Adreno (TM) 740"); GPU.put("Xperia 5 V", "Adreno (TM) 740");
        GPU.put("Xperia 10 VI", "Adreno (TM) 710");
        GPU.put("Pixel 7", "Mali-G710 MC10"); GPU.put("Pixel 8 Pro", "Mali-G715-Immortalis MC10");
        GPU.put("Pixel 9 Pro", "Mali-G715-Immortalis MC10");
        GPU.put("OnePlus 11", "Adreno (TM) 740"); GPU.put("OnePlus 12", "Adreno (TM) 750");
        GPU.put("Realme GT 5", "Adreno (TM) 740"); GPU.put("Realme 12 Pro", "Adreno (TM) 710");
    }
    static String[] gpuOf(String brand, String name) {
        if ("iPhone".equals(brand)) return new String[]{"Apple Inc.", "Apple GPU"};
        String r = GPU.get(name);
        if (r == null) r = "Adreno (TM) 740";
        return new String[]{r.startsWith("Mali") ? "ARM" : "Qualcomm", r};
    }
    // Doi Chrome/xxx trong UA mau thanh dung ban Chrome cua WebView dang chay (tranh lech phien ban)
    static String fixUa(String ua, String realUa) {
        if (ua == null || realUa == null || !ua.contains("Chrome/")) return ua;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("Chrome/(\\d+)").matcher(realUa);
        if (!m.find()) return ua;
        return ua.replaceAll("Chrome/\\d+\\.0\\.0\\.0", "Chrome/" + m.group(1) + ".0.0.0");
    }

    static final String WINDOWS_UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
            + "(KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36";
    static JSONObject desktopFp() {
        JSONObject o = new JSONObject();
        try {
            o.put("platform", "Win32"); o.put("cores", 8); o.put("mem", 8);
            o.put("touch", 0); o.put("sw", 1920); o.put("sh", 1080); o.put("dpr", 1);
            o.put("gpuV", "Google Inc. (NVIDIA)");
            o.put("gpuR", "ANGLE (NVIDIA, NVIDIA GeForce GTX 1650 Direct3D11 vs_5_0 ps_5_0, D3D11)");
        } catch (Exception ignored) {}
        return o;
    }

    static {
        String IP = "iPhone", AP = "Linux armv8l";
        // ---- iPhone ----
        add("iPhone", "iPhone 7", ios("15_0", "15.0"), IP, 2, 2, 5, 375, 667, 2);
        add("iPhone", "iPhone 8", ios("16_7", "16.7"), IP, 2, 2, 5, 375, 667, 2);
        add("iPhone", "iPhone X", ios("16_7", "16.7"), IP, 6, 3, 5, 375, 812, 3);
        add("iPhone", "iPhone 11", ios("17_5", "17.5"), IP, 6, 4, 5, 414, 896, 2);
        add("iPhone", "iPhone 12", ios("17_5", "17.5"), IP, 6, 4, 5, 390, 844, 3);
        add("iPhone", "iPhone 13", ios("17_5", "17.5"), IP, 6, 4, 5, 390, 844, 3);
        add("iPhone", "iPhone 14", ios("17_5", "17.5"), IP, 6, 6, 5, 390, 844, 3);
        add("iPhone", "iPhone 14 Pro Max", ios("17_5", "17.5"), IP, 6, 6, 5, 430, 932, 3);
        add("iPhone", "iPhone 15", ios("17_5", "17.5"), IP, 6, 6, 5, 393, 852, 3);
        add("iPhone", "iPhone 15 Pro Max", ios("17_5", "17.5"), IP, 6, 8, 5, 430, 932, 3);
        add("iPhone", "iPhone 16 Pro Max", ios("18_0", "18.0"), IP, 6, 8, 5, 440, 956, 3);
        add("iPhone", "iPhone 17 Pro Max", ios("18_0", "18.0"), IP, 6, 12, 5, 440, 956, 3);
        add("iPhone", "iPhone 18 Pro Max", ios("18_0", "18.0"), IP, 6, 12, 5, 440, 956, 3);
        // ---- Samsung ----
        add("Samsung", "Galaxy S21", android("13", "SM-G991B"), AP, 8, 8, 5, 360, 800, 3);
        add("Samsung", "Galaxy S22 Ultra", android("14", "SM-S908B"), AP, 8, 12, 5, 412, 892, 3);
        add("Samsung", "Galaxy S23", android("14", "SM-S911B"), AP, 8, 8, 5, 360, 780, 3);
        add("Samsung", "Galaxy S24 Ultra", android("14", "SM-S928B"), AP, 8, 12, 5, 412, 915, 3);
        add("Samsung", "Galaxy Z Fold5", android("14", "SM-F946B"), AP, 8, 12, 5, 673, 841, 2);
        // ---- Xiaomi / Redmi ----
        add("Xiaomi · Redmi", "Redmi Note 12", android("13", "23021RAAEG"), AP, 8, 8, 5, 393, 873, 3);
        add("Xiaomi · Redmi", "Redmi Note 13 Pro", android("14", "2312DRA50G"), AP, 8, 8, 5, 412, 915, 3);
        add("Xiaomi · Redmi", "Xiaomi 13", android("14", "2211133C"), AP, 8, 12, 5, 393, 873, 3);
        add("Xiaomi · Redmi", "Xiaomi 14", android("14", "23127PN0CC"), AP, 8, 12, 5, 412, 915, 3);
        add("Xiaomi · Redmi", "POCO X6 Pro", android("14", "2311DRK48C"), AP, 8, 12, 5, 412, 915, 3);
        // ---- OPPO ----
        add("OPPO", "Find X5", android("13", "CPH2307"), AP, 8, 12, 5, 412, 915, 3);
        add("OPPO", "Find X7 Ultra", android("14", "PHZ110"), AP, 8, 16, 5, 412, 915, 3);
        add("OPPO", "Reno 11", android("14", "CPH2525"), AP, 8, 12, 5, 412, 915, 3);
        add("OPPO", "Reno 12", android("14", "CPH2625"), AP, 8, 12, 5, 412, 915, 3);
        // ---- Vivo ----
        add("Vivo", "Vivo X90", android("13", "V2254A"), AP, 8, 12, 5, 412, 915, 3);
        add("Vivo", "Vivo X100 Pro", android("14", "V2324A"), AP, 8, 16, 5, 412, 915, 3);
        add("Vivo", "Vivo V29", android("13", "V2250"), AP, 8, 12, 5, 412, 915, 3);
        add("Vivo", "Vivo Y36", android("13", "V2247"), AP, 8, 8, 5, 393, 873, 3);
        // ---- Sony ----
        add("Sony", "Xperia 1 V", android("13", "XQ-DQ72"), AP, 8, 12, 5, 412, 915, 3);
        add("Sony", "Xperia 5 V", android("13", "XQ-DE72"), AP, 8, 8, 5, 360, 840, 3);
        add("Sony", "Xperia 10 VI", android("14", "XQ-ES72"), AP, 8, 8, 5, 360, 840, 3);
        // ---- Google Pixel ----
        add("Google Pixel", "Pixel 7", android("14", "Pixel 7"), AP, 8, 8, 5, 412, 915, 3);
        add("Google Pixel", "Pixel 8 Pro", android("14", "Pixel 8 Pro"), AP, 8, 12, 5, 412, 915, 3);
        add("Google Pixel", "Pixel 9 Pro", android("14", "Pixel 9 Pro"), AP, 8, 16, 5, 412, 915, 3);
        // ---- OnePlus ----
        add("OnePlus", "OnePlus 11", android("14", "CPH2449"), AP, 8, 16, 5, 412, 915, 3);
        add("OnePlus", "OnePlus 12", android("14", "CPH2581"), AP, 8, 16, 5, 412, 915, 3);
        // ---- Realme ----
        add("Realme", "Realme GT 5", android("13", "RMX3708"), AP, 8, 16, 5, 412, 915, 3);
        add("Realme", "Realme 12 Pro", android("14", "RMX3840"), AP, 8, 12, 5, 412, 915, 3);
    }
}