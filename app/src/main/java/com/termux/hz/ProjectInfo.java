package com.termux.hz;

import java.io.File;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Detects loader, Minecraft version and required Java version of a mod project. */
public final class ProjectInfo {
    public String loader = "Unknown";   // Fabric, Forge, NeoForge, Quilt, Paper/Bukkit, Maven, Gradle
    public String mcVersion = "";
    public int javaVersion = 17;
    public boolean hasGradleWrapper, hasGradle, hasMaven;

    public static ProjectInfo detect(File dir) {
        ProjectInfo p = new ProjectInfo();
        p.hasGradleWrapper = new File(dir, "gradlew").exists();
        p.hasGradle = new File(dir, "build.gradle").exists() || new File(dir, "build.gradle.kts").exists();
        p.hasMaven = new File(dir, "pom.xml").exists();
        String gp = read(new File(dir, "gradle.properties"));
        String bg = read(new File(dir, "build.gradle")) + read(new File(dir, "build.gradle.kts"));
        String all = gp + "\n" + bg + "\n" + read(new File(dir, "pom.xml"))
                + "\n" + read(new File(dir, "settings.gradle")) + read(new File(dir, "settings.gradle.kts"));
        String low = all.toLowerCase();

        if (low.contains("fabric-loom") || low.contains("net.fabricmc")) p.loader = "Fabric";
        if (low.contains("org.quiltmc") || low.contains("quilt-loom")) p.loader = "Quilt";
        if (low.contains("net.minecraftforge.gradle") || low.contains("minecraftforge")) p.loader = "Forge";
        if (low.contains("net.neoforged") || low.contains("neoforge")) p.loader = "NeoForge";
        if (p.loader.equals("Unknown")) {
            if (low.contains("spigot") || low.contains("papermc") || low.contains("bukkit")) p.loader = "Paper/Bukkit";
            else if (p.hasMaven) p.loader = "Maven";
            else if (p.hasGradle) p.loader = "Gradle";
        }

        p.mcVersion = first(all, "minecraft_version\\s*[=:]\\s*['\"]?([0-9]+\\.[0-9]+(?:\\.[0-9]+)?)");
        if (p.mcVersion.isEmpty()) p.mcVersion = first(all, "minecraft\\s*[\"']com\\.mojang:minecraft:([0-9.]+)");
        if (p.mcVersion.isEmpty()) p.mcVersion = first(all, "mcVersion\\s*=\\s*['\"]([0-9.]+)");

        int explicit = 0;
        String j = first(all, "JavaLanguageVersion\\.of\\(\\s*(\\d+)\\s*\\)");
        if (j.isEmpty()) j = first(all, "JavaVersion\\.VERSION_(?:1_)?(\\d+)");
        if (j.isEmpty()) j = first(all, "<(?:maven\\.compiler\\.(?:release|source|target)|java\\.version)>\\s*(?:1\\.)?(\\d+)");
        if (j.isEmpty()) j = first(all, "sourceCompatibility\\s*=\\s*['\"]?(?:1\\.)?(\\d+)");
        if (!j.isEmpty()) try { explicit = Integer.parseInt(j); } catch (Exception ignored) {}

        int fromMc = javaForMinecraft(p.mcVersion);
        p.javaVersion = snap(explicit > 0 ? Math.max(explicit, fromMc > 0 && explicit < 8 ? fromMc : explicit) : (fromMc > 0 ? fromMc : 17));
        // An older Minecraft needs the old toolchain even when build.gradle says a newer source level.
        if (fromMc > 0 && explicit > 0 && fromMc < explicit && fromMc <= 11) p.javaVersion = snap(fromMc);
        return p;
    }

    /** Required Java for running the toolchain of a Minecraft version. */
    public static int javaForMinecraft(String mc) {
        if (mc == null || mc.isEmpty()) return 0;
        String[] a = mc.split("\\.");
        try {
            int minor = Integer.parseInt(a[1]);
            int patch = a.length > 2 ? Integer.parseInt(a[2]) : 0;
            if (minor <= 16) return 8;                 // <=1.16.x: Java 8
            if (minor == 17) return 17;                // 1.17 uses 16 but 17 works
            if (minor < 20 || (minor == 20 && patch < 5)) return 17;
            return 21;                                 // 1.20.5+ and 1.21.x
        } catch (Exception e) { return 0; }
    }

    private static int snap(int v) {
        if (v <= 8) return 8;
        if (v <= 11) return 11;
        if (v <= 17) return 17;
        return 21;
    }

    private static String first(String s, String re) {
        Matcher m = Pattern.compile(re).matcher(s);
        return m.find() ? m.group(1) : "";
    }

    private static String read(File f) {
        try {
            if (f.isFile() && f.length() < 512_000) {
                try (InputStream in = new FileInputStream(f)) {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    byte[] buf = new byte[8192];
                    int n;
                    while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                    return out.toString("UTF-8");
                }
            }
        } catch (Exception ignored) {}
        return "";
    }

    public String summary() {
        return loader + (mcVersion.isEmpty() ? "" : " · MC " + mcVersion) + " · Java " + javaVersion;
    }
}
