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

    // os = "18_6", "16_7_3"... ; iOS 26 ghi "26_x" (Safari 26 DONG BANG phan OS trong UA o 18_6, chi Version/26.x doi)
    static String ios(String os) {
        String[] p = os.split("_");
        int major = Integer.parseInt(p[0]), minor = Integer.parseInt(p[1]);
        String osUa = os, ver;
        if (major >= 26) { osUa = "18_6"; ver = major + "." + minor; }
        else if (major == 15 && minor >= 7) ver = "15.6.1";   // iOS 15.7.x / 15.8.x van bao Version/15.6.1
        else if (major == 16 && minor >= 7) ver = "16.6";     // iOS 16.7.x bao Version/16.6 (khong co 16.7)
        else if (major == 17 && minor >= 7) ver = "17.6";     // iOS 17.7.x bao Version/17.6
        else ver = major + "." + minor;
        return "Mozilla/5.0 (iPhone; CPU iPhone OS " + osUa + " like Mac OS X) AppleWebKit/605.1.15 "
                + "(KHTML, like Gecko) Version/" + ver + " Mobile/15E148 Safari/604.1";
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

    // ===== 30 User-Agent tao san cho moi thiet bi (idx 0 = UA goc; 1..29 = bien the) =====
    static final int UA_COUNT = 30;

    // Cac ban iOS that (chi liet ke ban co that). iOS 26: UA van ghi 18_6, chi doi Version/26.x
    static final Map<Integer, String[]> IOS_POOL = new LinkedHashMap<>();
    static {
        IOS_POOL.put(15, ("15_0 15_0_1 15_0_2 15_1 15_1_1 15_2 15_2_1 15_3 15_3_1 15_4 15_4_1 15_5 15_6 15_6_1 "
                + "15_7 15_7_1 15_7_2 15_7_3 15_7_4 15_7_5 15_7_6 15_7_7 15_7_8 15_7_9 "
                + "15_8 15_8_1 15_8_2 15_8_3 15_8_4 15_8_5").split(" "));
        IOS_POOL.put(16, ("16_0 16_0_1 16_0_2 16_0_3 16_1 16_1_1 16_1_2 16_2 16_3 16_3_1 16_4 16_4_1 16_5 16_5_1 16_6 16_6_1 "
                + "16_7 16_7_1 16_7_2 16_7_3 16_7_4 16_7_5 16_7_6 16_7_7 16_7_8 16_7_9 16_7_10").split(" "));
        IOS_POOL.put(17, ("17_0 17_0_1 17_0_2 17_0_3 17_1 17_1_1 17_1_2 17_2 17_2_1 17_3 17_3_1 17_4 17_4_1 17_5 17_5_1 17_6 17_6_1 "
                + "17_7 17_7_1 17_7_2 17_7_3 17_7_4 17_7_5 17_7_6 17_7_7 17_7_8 17_7_9 17_7_10").split(" "));
        IOS_POOL.put(18, "18_0 18_0_1 18_1 18_1_1 18_2 18_2_1 18_3 18_3_1 18_3_2 18_4 18_4_1 18_5 18_6 18_6_1 18_6_2".split(" "));
        IOS_POOL.put(26, "26_0 26_1 26_2 26_3 26_4".split(" "));
    }
    // iOS thap nhat / cao nhat ma tung doi iPhone chay duoc (theo Apple)
    static int[] iosRange(String name) {
        switch (name) {
            case "iPhone 7": return new int[]{15, 15};            // toi da iOS 15.8
            case "iPhone 8": case "iPhone 8 Plus": case "iPhone X": return new int[]{15, 16};   // toi da iOS 16.7
            case "iPhone XR": case "iPhone XS": case "iPhone XS Max": return new int[]{15, 18}; // toi da iOS 18 (khong len 26)
            case "iPhone SE 2020": case "iPhone SE 2022":
            case "iPhone 11": case "iPhone 11 Pro": case "iPhone 11 Pro Max":
            case "iPhone 12 mini": case "iPhone 12": case "iPhone 12 Pro": case "iPhone 12 Pro Max":
            case "iPhone 13 mini": case "iPhone 13": case "iPhone 13 Pro": case "iPhone 13 Pro Max": return new int[]{15, 26};
            case "iPhone 14": case "iPhone 14 Plus": case "iPhone 14 Pro": case "iPhone 14 Pro Max": return new int[]{16, 26};
            case "iPhone 15": case "iPhone 15 Plus": case "iPhone 15 Pro": case "iPhone 15 Pro Max": return new int[]{17, 26};
            case "iPhone 16e": case "iPhone 16": case "iPhone 16 Plus": case "iPhone 16 Pro": case "iPhone 16 Pro Max": return new int[]{18, 26};
            default: return new int[]{26, 26};                    // iPhone 17 / Air / 18: xuat xuong voi iOS 26
        }
    }

    private static final Map<String, String[]> UA_CACHE = new java.util.HashMap<>();

    // Tao san danh sach UA cua 1 may iPhone: [0] = UA goc, [1..29] = ban iOS hop le cua dung doi may do
    static synchronized String[] iosList(Dev d) {
        String[] c = UA_CACHE.get(d.id);
        if (c != null) return c;
        int[] rg = iosRange(d.name);
        int[] majors = {15, 16, 17, 18, 26};
        List<Integer> use = new ArrayList<>();
        for (int m : majors) if (m >= rg[0] && m <= rg[1]) use.add(m);
        while (use.size() > 3) use.remove(0);          // chi lay toi da 3 dong iOS moi nhat cua may do
        java.util.LinkedHashSet<String> pool = new java.util.LinkedHashSet<>();
        for (int m : use) for (String os : IOS_POOL.get(m)) pool.add(ios(os));
        pool.remove(d.ua);
        String[] all = pool.toArray(new String[0]);
        String[] out = new String[UA_COUNT];
        out[0] = d.ua;
        int need = UA_COUNT - 1, k = Math.min(need, all.length);
        for (int j = 0; j < need; j++) {
            if (all.length == 0) { out[j + 1] = d.ua; continue; }
            int pos = (j < k) ? (k == 1 ? 0 : (int) Math.round(j * (all.length - 1) / (double) (k - 1)))
                              : (j % k == 0 ? 0 : (int) Math.round((j % k) * (all.length - 1) / (double) (k - 1)));
            out[j + 1] = all[pos];
        }
        UA_CACHE.put(d.id, out);
        return out;
    }

    static String uaAt(Dev d, int idx) {
        if (d == null) return "";
        idx = ((idx % UA_COUNT) + UA_COUNT) % UA_COUNT;   // chi so cu (0..49 cua ban 50 UA) van hop le
        if (idx == 0) return d.ua;
        if (d.ua.contains("iPhone")) return iosList(d)[idx];
        int i = idx - 1;
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
        // ---- them v3.3.7 ----
        GPU.put("Galaxy S25 Ultra", "Adreno (TM) 830");
        GPU.put("Galaxy S23 Ultra", "Adreno (TM) 740");
        GPU.put("Galaxy Z Flip5", "Adreno (TM) 740");
        GPU.put("Galaxy A55", "Mali-G68 MP5");
        GPU.put("Galaxy A54", "Mali-G68 MP5");
        GPU.put("Galaxy A34", "Mali-G68 MP4");
        GPU.put("Galaxy A15", "Mali-G52 MC2");
        GPU.put("Redmi Note 11", "Adreno (TM) 610");
        GPU.put("Redmi 12", "Mali-G52 MC2");
        GPU.put("Redmi Note 13", "Adreno (TM) 610");
        GPU.put("Xiaomi 12T", "Mali-G610 MC6");
        GPU.put("Xiaomi 14T", "Mali-G615 MC6");
        GPU.put("POCO F5", "Adreno (TM) 725");
        GPU.put("POCO X5 Pro", "Adreno (TM) 642L");
        GPU.put("POCO M6 Pro", "Mali-G57 MC2");
        GPU.put("Honor 90", "Adreno (TM) 644");
        GPU.put("Honor Magic6 Pro", "Adreno (TM) 750");
        GPU.put("Honor X9b", "Adreno (TM) 710");
        GPU.put("Moto G84", "Adreno (TM) 619");
        GPU.put("Moto G54", "Mali-G68 MC4");
        GPU.put("Moto Edge 40", "Mali-G77 MC9");
        GPU.put("Moto Edge 50 Pro", "Adreno (TM) 720");
        GPU.put("Nothing Phone (2)", "Adreno (TM) 730");
        GPU.put("Nothing Phone (2a)", "Mali-G715 MC4");
        GPU.put("ROG Phone 8", "Adreno (TM) 750");
        GPU.put("Zenfone 11 Ultra", "Adreno (TM) 750");
        GPU.put("Tecno Camon 30", "Mali-G68 MC4");
        GPU.put("Tecno Spark 20", "Mali-G52 MC2");
        GPU.put("Infinix Note 40", "Mali-G57 MC2");
        GPU.put("Infinix Hot 40", "Mali-G52 MC2");
        GPU.put("Realme 11 Pro", "Mali-G68 MC4");
        GPU.put("Realme C55", "Mali-G52 MC2");
        GPU.put("Realme Narzo 60", "Mali-G57 MC2");
        GPU.put("OPPO A78", "Mali-G57 MC2");
        GPU.put("Reno 10", "Mali-G68 MC4");
        GPU.put("Vivo V30", "Adreno (TM) 720");
        GPU.put("Vivo Y100", "Adreno (TM) 610");
        GPU.put("Pixel 6a", "Mali-G78 MP20");
        GPU.put("Pixel 7a", "Mali-G710 MC10");
        GPU.put("Pixel 8", "Mali-G715-Immortalis MC10");
        GPU.put("Pixel 9", "Mali-G715-Immortalis MC10");
        GPU.put("OnePlus 13", "Adreno (TM) 830");
        GPU.put("OnePlus 12R", "Adreno (TM) 740");
        GPU.put("OnePlus Nord 3", "Mali-G710 MC10");
        GPU.put("OnePlus Nord CE3 Lite", "Adreno (TM) 619");
        GPU.put("Xperia 1 VI", "Adreno (TM) 750");
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
        add("iPhone", "iPhone 7", ios("15_8"), IP, 2, 2, 5, 375, 667, 2);
        add("iPhone", "iPhone 8", ios("16_7"), IP, 2, 2, 5, 375, 667, 2);
        add("iPhone", "iPhone 8 Plus", ios("16_7"), IP, 6, 3, 5, 414, 736, 3);
        add("iPhone", "iPhone X", ios("16_7"), IP, 6, 3, 5, 375, 812, 3);
        add("iPhone", "iPhone XR", ios("18_6"), IP, 6, 3, 5, 414, 896, 2);
        add("iPhone", "iPhone XS", ios("18_6"), IP, 6, 4, 5, 375, 812, 3);
        add("iPhone", "iPhone XS Max", ios("18_6"), IP, 6, 4, 5, 414, 896, 3);
        add("iPhone", "iPhone SE 2020", ios("18_6"), IP, 6, 3, 5, 375, 667, 2);
        add("iPhone", "iPhone SE 2022", ios("18_6"), IP, 6, 4, 5, 375, 667, 2);
        add("iPhone", "iPhone 11", ios("18_6"), IP, 6, 4, 5, 414, 896, 2);
        add("iPhone", "iPhone 11 Pro", ios("18_6"), IP, 6, 4, 5, 375, 812, 3);
        add("iPhone", "iPhone 11 Pro Max", ios("18_6"), IP, 6, 4, 5, 414, 896, 3);
        add("iPhone", "iPhone 12 mini", ios("18_6"), IP, 6, 4, 5, 375, 812, 3);
        add("iPhone", "iPhone 12", ios("18_6"), IP, 6, 4, 5, 390, 844, 3);
        add("iPhone", "iPhone 12 Pro", ios("18_6"), IP, 6, 6, 5, 390, 844, 3);
        add("iPhone", "iPhone 12 Pro Max", ios("18_6"), IP, 6, 6, 5, 428, 926, 3);
        add("iPhone", "iPhone 13 mini", ios("18_6"), IP, 6, 4, 5, 375, 812, 3);
        add("iPhone", "iPhone 13", ios("18_6"), IP, 6, 4, 5, 390, 844, 3);
        add("iPhone", "iPhone 13 Pro", ios("18_6"), IP, 6, 6, 5, 390, 844, 3);
        add("iPhone", "iPhone 13 Pro Max", ios("18_6"), IP, 6, 6, 5, 428, 926, 3);
        add("iPhone", "iPhone 14", ios("18_6"), IP, 6, 6, 5, 390, 844, 3);
        add("iPhone", "iPhone 14 Plus", ios("18_6"), IP, 6, 6, 5, 428, 926, 3);
        add("iPhone", "iPhone 14 Pro", ios("18_6"), IP, 6, 6, 5, 393, 852, 3);
        add("iPhone", "iPhone 14 Pro Max", ios("18_6"), IP, 6, 6, 5, 430, 932, 3);
        add("iPhone", "iPhone 15", ios("18_6"), IP, 6, 6, 5, 393, 852, 3);
        add("iPhone", "iPhone 15 Plus", ios("18_6"), IP, 6, 6, 5, 430, 932, 3);
        add("iPhone", "iPhone 15 Pro", ios("18_6"), IP, 6, 8, 5, 393, 852, 3);
        add("iPhone", "iPhone 15 Pro Max", ios("18_6"), IP, 6, 8, 5, 430, 932, 3);
        add("iPhone", "iPhone 16e", ios("26_0"), IP, 6, 8, 5, 390, 844, 3);
        add("iPhone", "iPhone 16", ios("26_0"), IP, 6, 8, 5, 393, 852, 3);
        add("iPhone", "iPhone 16 Plus", ios("26_0"), IP, 6, 8, 5, 430, 932, 3);
        add("iPhone", "iPhone 16 Pro", ios("26_0"), IP, 6, 8, 5, 402, 874, 3);
        add("iPhone", "iPhone 16 Pro Max", ios("26_0"), IP, 6, 8, 5, 440, 956, 3);
        add("iPhone", "iPhone 17", ios("26_2"), IP, 6, 8, 5, 402, 874, 3);
        add("iPhone", "iPhone 17 Air", ios("26_2"), IP, 6, 12, 5, 420, 912, 3);
        add("iPhone", "iPhone 17 Pro", ios("26_2"), IP, 6, 12, 5, 402, 874, 3);
        add("iPhone", "iPhone 17 Pro Max", ios("26_2"), IP, 6, 12, 5, 440, 956, 3);
        add("iPhone", "iPhone 18 Pro Max", ios("26_2"), IP, 6, 12, 5, 440, 956, 3);
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
        // ---- them v3.3.7: da dang thiet bi ----
        add("Samsung", "Galaxy S25 Ultra", android("15", "SM-S938B"), AP, 8, 12, 5, 412, 915, 3);
        add("Samsung", "Galaxy S23 Ultra", android("14", "SM-S918B"), AP, 8, 12, 5, 412, 915, 3.5);
        add("Samsung", "Galaxy Z Flip5", android("14", "SM-F731B"), AP, 8, 8, 5, 412, 915, 2.625);
        add("Samsung", "Galaxy A55", android("14", "SM-A556B"), AP, 8, 8, 5, 360, 780, 3);
        add("Samsung", "Galaxy A54", android("14", "SM-A546B"), AP, 8, 8, 5, 360, 780, 3);
        add("Samsung", "Galaxy A34", android("14", "SM-A346B"), AP, 8, 6, 5, 360, 780, 3);
        add("Samsung", "Galaxy A15", android("14", "SM-A155F"), AP, 8, 4, 5, 360, 800, 2);
        add("Xiaomi · Redmi", "Redmi Note 11", android("12", "2201117TG"), AP, 8, 6, 5, 393, 873, 2.75);
        add("Xiaomi · Redmi", "Redmi 12", android("13", "23053RN02A"), AP, 8, 4, 5, 393, 873, 2.75);
        add("Xiaomi · Redmi", "Redmi Note 13", android("14", "23124RA7EO"), AP, 8, 8, 5, 393, 873, 2.75);
        add("Xiaomi · Redmi", "Xiaomi 12T", android("13", "22071212AG"), AP, 8, 8, 5, 393, 873, 2.75);
        add("Xiaomi · Redmi", "Xiaomi 14T", android("14", "2407FPN8EG"), AP, 8, 12, 5, 412, 915, 2.625);
        add("Xiaomi · Redmi", "POCO F5", android("13", "23049PCD8G"), AP, 8, 8, 5, 393, 873, 2.75);
        add("Xiaomi · Redmi", "POCO X5 Pro", android("13", "22101320G"), AP, 8, 8, 5, 393, 873, 2.75);
        add("Xiaomi · Redmi", "POCO M6 Pro", android("14", "2312FPCA6G"), AP, 8, 8, 5, 393, 873, 2.75);
        add("OPPO", "OPPO A78", android("13", "CPH2565"), AP, 8, 8, 5, 360, 800, 2);
        add("OPPO", "Reno 10", android("13", "CPH2531"), AP, 8, 8, 5, 360, 800, 3);
        add("Vivo", "Vivo V30", android("14", "V2318"), AP, 8, 12, 5, 393, 873, 2.75);
        add("Vivo", "Vivo Y100", android("13", "V2307"), AP, 8, 8, 5, 393, 873, 2.75);
        add("Google Pixel", "Pixel 6a", android("14", "Pixel 6a"), AP, 8, 6, 5, 412, 915, 2.625);
        add("Google Pixel", "Pixel 7a", android("14", "Pixel 7a"), AP, 8, 8, 5, 412, 915, 2.625);
        add("Google Pixel", "Pixel 8", android("14", "Pixel 8"), AP, 8, 8, 5, 412, 915, 2.625);
        add("Google Pixel", "Pixel 9", android("15", "Pixel 9"), AP, 8, 12, 5, 412, 923, 2.625);
        add("OnePlus", "OnePlus 13", android("15", "CPH2655"), AP, 8, 16, 5, 412, 915, 3.5);
        add("OnePlus", "OnePlus 12R", android("14", "CPH2585"), AP, 8, 16, 5, 412, 915, 2.625);
        add("OnePlus", "OnePlus Nord 3", android("14", "CPH2493"), AP, 8, 16, 5, 412, 915, 2.625);
        add("OnePlus", "OnePlus Nord CE3 Lite", android("13", "CPH2465"), AP, 8, 8, 5, 412, 915, 2.625);
        add("Sony", "Xperia 1 VI", android("14", "XQ-EC72"), AP, 8, 12, 5, 412, 915, 3);
        add("Honor", "Honor 90", android("13", "REA-NX9"), AP, 8, 12, 5, 412, 915, 2.625);
        add("Honor", "Honor Magic6 Pro", android("14", "BVL-N49"), AP, 8, 12, 5, 412, 915, 3.5);
        add("Honor", "Honor X9b", android("13", "ALI-NX1"), AP, 8, 8, 5, 412, 915, 2.625);
        add("Motorola", "Moto G54", android("13", "XT2343-2"), AP, 8, 8, 5, 412, 915, 2.625);
        add("Motorola", "Moto G84", android("13", "XT2347-2"), AP, 8, 12, 5, 412, 915, 2.625);
        add("Motorola", "Moto Edge 40", android("13", "XT2303-2"), AP, 8, 8, 5, 412, 915, 2.625);
        add("Motorola", "Moto Edge 50 Pro", android("14", "XT2403-1"), AP, 8, 12, 5, 412, 915, 2.625);
        add("Nothing", "Nothing Phone (2)", android("14", "A065"), AP, 8, 12, 5, 412, 915, 2.625);
        add("Nothing", "Nothing Phone (2a)", android("14", "A142"), AP, 8, 8, 5, 412, 915, 2.625);
        add("Asus", "ROG Phone 8", android("14", "AI2401"), AP, 8, 16, 5, 412, 915, 2.625);
        add("Asus", "Zenfone 11 Ultra", android("14", "AI2401_A"), AP, 8, 12, 5, 412, 915, 2.625);
        add("Tecno · Infinix", "Tecno Camon 30", android("14", "CL6"), AP, 8, 8, 5, 360, 800, 2);
        add("Tecno · Infinix", "Tecno Spark 20", android("13", "KJ5"), AP, 8, 8, 5, 360, 800, 2);
        add("Tecno · Infinix", "Infinix Note 40", android("14", "X6853"), AP, 8, 8, 5, 360, 800, 2);
        add("Tecno · Infinix", "Infinix Hot 40", android("13", "X6836"), AP, 8, 8, 5, 360, 800, 2);
        add("Realme", "Realme 11 Pro", android("13", "RMX3771"), AP, 8, 8, 5, 360, 800, 3);
        add("Realme", "Realme C55", android("13", "RMX3710"), AP, 8, 6, 5, 360, 800, 2);
        add("Realme", "Realme Narzo 60", android("13", "RMX3750"), AP, 8, 8, 5, 360, 800, 3);
    }
}