package com.htmltoapk.builder;

import java.io.File;
import java.util.regex.Pattern;

/** Everything the wizard collects. */
public final class ProjectSpec {
    private static final Pattern PACKAGE = Pattern.compile("^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)+$");

    public String appName = "";
    public String packageName = "com.htmltoapk.app";
    public int versionCode = 1;
    public String versionName = "1.0";

    /** Folder that contains index.html (set after the user picks a source). */
    public File wwwDir;
    public String sourceLabel = "";

    /** "auto", "portrait" or "landscape". */
    public String orientation = "auto";
    public boolean fullscreen = false;
    public boolean keepScreenOn = false;

    /** PNG bytes of the custom icon, or null for the default icon. */
    public byte[] iconPng;

    public boolean allowInternet = false;

    /** If not empty, the app opens this link live instead of bundled files (needs internet). */
    public String websiteUrl = "";

    public static boolean isValidPackage(String p) {
        return PACKAGE.matcher(p).matches()
                && !p.equals("com.template.webapp")
                && !p.equals("com.htmltoapk.builder");
    }

    /** Suggests a package name from the app name, e.g. "My App" -> com.htmltoapk.myapp. */
    public static String suggestPackage(String appName) {
        String s = appName.toLowerCase().replaceAll("[^a-z0-9]", "");
        if (s.isEmpty() || !Character.isLetter(s.charAt(0))) s = "app" + s;
        return "com.htmltoapk." + s;
    }
}
