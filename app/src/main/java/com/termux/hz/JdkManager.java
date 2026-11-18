package com.termux.hz;

import android.content.Context;

import java.io.File;
import java.util.Arrays;

/** Installs and locates JDKs 8, 11, 17, 21 inside the Termux prefix. */
public final class JdkManager {
    private JdkManager() {}

    /** Directory names that different packagers use for a given major version. */
    private static String[] candidates(int v) {
        return new String[]{
            "java-" + v + "-openjdk", "openjdk-" + v, "jdk" + v, "jdk-" + v,
            "java-" + v, "jdk1." + v + ".0", "jdk" + v + "u"
        };
    }

    public static File home(int v) {
        File jvm = Hz.JVM_DIR;
        if (!jvm.isDirectory()) return null;
        for (String c : candidates(v)) {
            File f = new File(jvm, c);
            if (new File(f, "bin/java").exists()) return f;
        }
        File[] all = jvm.listFiles();
        if (all != null) {
            Arrays.sort(all);
            for (File f : all) {
                String n = f.getName();
                if ((n.contains("-" + v + "-") || n.endsWith("-" + v) || n.contains(v == 8 ? "1.8" : "jdk-" + v))
                        && new File(f, "bin/java").exists()) return f;
            }
        }
        return null;
    }

    public static boolean installed(int v) { return home(v) != null; }

    public static boolean hasCompiler(int v) {
        File h = home(v);
        return h != null && new File(h, "bin/javac").exists();
    }

    /** Which Java version do we use for a project (override > detected > best installed). */
    public static int resolve(Context ctx, ProjectInfo info, String projectName) {
        int ov = Hz.prefs(ctx).getInt(Hz.K_JAVA_PREFIX + projectName, 0);
        if (ov > 0 && installed(ov)) return ov;
        int want = info != null ? info.javaVersion : 17;
        if (installed(want)) return want;
        // pick the nearest installed version that is >= wanted, else the highest installed
        for (int v : Hz.JAVA_VERSIONS) if (v >= want && installed(v)) return v;
        for (int i = Hz.JAVA_VERSIONS.length - 1; i >= 0; i--) if (installed(Hz.JAVA_VERSIONS[i])) return Hz.JAVA_VERSIONS[i];
        return want;
    }

    /**
     * Shell script that installs a JDK. Order: Termux repo package; then tur-repo; then the custom URL
     * from Settings (a .tar.gz/.tar.xz/.zip of an Android-compatible JDK).
     */
    public static String installScript(Context ctx, int v) {
        String url = Hz.prefs(ctx).getString(Hz.K_JDK_URL_PREFIX + v, "").trim();
        StringBuilder s = new StringBuilder();
        s.append("set +e\n");
        s.append("echo '== Installing Java ").append(v).append(" =='\n");
        s.append("apt-get install -y openjdk-").append(v).append(" 2>&1\n");
        s.append("if ls -d $PREFIX/lib/jvm/*").append(v).append("* >/dev/null 2>&1; then echo 'OK: from Termux repo'; exit 0; fi\n");
        s.append("echo 'Not in main repo, trying tur-repo...'\n");
        s.append("apt-get install -y tur-repo 2>&1 && apt-get update -y 2>&1 && apt-get install -y openjdk-").append(v).append(" 2>&1\n");
        s.append("if ls -d $PREFIX/lib/jvm/*").append(v).append("* >/dev/null 2>&1; then echo 'OK: from tur-repo'; exit 0; fi\n");
        if (!url.isEmpty()) {
            String q = Sh.quote(url);
            s.append("echo 'Trying custom URL...'\n");
            s.append("mkdir -p $PREFIX/lib/jvm $TMPDIR/hzjdk && cd $TMPDIR/hzjdk && rm -rf ./* \n");
            s.append("curl -fL --retry 3 -o jdk.pkg ").append(q).append(" 2>&1 || exit 2\n");
            s.append("mkdir x && cd x\n");
            s.append("case ").append(q).append(" in *.zip) unzip -q ../jdk.pkg;; *.xz) tar xJf ../jdk.pkg;; *) tar xzf ../jdk.pkg;; esac\n");
            s.append("top=$(ls | head -n1); if [ -d \"$top/bin\" ]; then src=\"$top\"; else src=\".\"; fi\n");
            s.append("rm -rf $PREFIX/lib/jvm/java-").append(v).append("-openjdk && mv \"$src\" $PREFIX/lib/jvm/java-").append(v).append("-openjdk\n");
            s.append("chmod -R u+rwx $PREFIX/lib/jvm/java-").append(v).append("-openjdk/bin 2>/dev/null\n");
            s.append("if [ -x $PREFIX/lib/jvm/java-").append(v).append("-openjdk/bin/java ]; then echo 'OK: from custom URL'; exit 0; fi\n");
        }
        s.append("echo 'FAILED: Java ").append(v).append(" is not available from the configured sources.'\n");
        s.append("echo 'Set a custom JDK URL for Java ").append(v).append(" in Settings > Java and retry.'\n");
        s.append("exit 3\n");
        return s.toString();
    }

    public static String versionLabel(int v) {
        File h = home(v);
        if (h == null) return "Not installed";
        return h.getName() + (hasCompiler(v) ? "" : " (runtime only)");
    }
}
