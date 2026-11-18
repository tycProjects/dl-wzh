package com.termux.hz;

import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** Builds a mod project with Gradle (wrapper or system) or Maven using the right JDK. */
public final class BuildManager {

    public interface Callback {
        void onStart(String command, int javaVersion);
        void onLine(String line);
        void onFinish(boolean success, boolean cancelled, List<File> jars, String log);
    }

    private final Context ctx;
    private final Handler main = new Handler(Looper.getMainLooper());
    private volatile Sh.Handle handle;
    private volatile boolean running;
    private String lastLog = "";

    public BuildManager(Context ctx) { this.ctx = ctx.getApplicationContext(); }

    public boolean isRunning() { return running; }
    public String lastLog() { return lastLog; }

    public void cancel() { Sh.Handle h = handle; if (h != null) h.cancel(); }

    public boolean build(final File project, final Callback cb) {
        if (running) return false;
        running = true;
        final Sh.Handle h = new Sh.Handle();
        handle = h;
        new Thread(() -> {
            ProjectInfo info = ProjectInfo.detect(project);
            int jv = JdkManager.resolve(ctx, info, project.getName());
            File jh = JdkManager.home(jv);
            String task = Hz.prefs(ctx).getString(Hz.K_BUILD_TASK, "build").trim();
            String extra = Hz.prefs(ctx).getString(Hz.K_BUILD_ARGS, "").trim();
            int xmx = Hz.prefs(ctx).getInt(Hz.K_XMX, 1536);
            String cmd;
            if (info.hasGradleWrapper) {
                cmd = "chmod +x ./gradlew; ./gradlew " + (task.isEmpty() ? "build" : task)
                        + " --no-daemon --console=plain -Dorg.gradle.native=false " + extra;
            } else if (info.hasGradle) {
                cmd = "command -v gradle >/dev/null || apt-get install -y gradle; gradle "
                        + (task.isEmpty() ? "build" : task) + " --no-daemon --console=plain -Dorg.gradle.native=false " + extra;
            } else if (info.hasMaven) {
                cmd = "command -v mvn >/dev/null || apt-get install -y maven; mvn -B -q package -DskipTests " + extra;
            } else {
                cmd = "echo 'No build.gradle, build.gradle.kts or pom.xml found in this project.'; exit 1";
            }
            final String shown = cmd;
            final int jvf = jv;
            post(() -> cb.onStart(shown, jvf));
            if (jh == null) {
                post(() -> cb.onLine("Java " + jvf + " is not installed. Open Settings > Java and install it."));
            }
            long started = System.currentTimeMillis();
            Sh.Result r = Sh.run(ctx, cmd, project, jh != null ? jh.getAbsolutePath() : null, xmx,
                    line -> post(() -> cb.onLine(line)), h);
            lastLog = r.output;
            List<File> jars = r.ok() ? findJars(project, started) : new ArrayList<File>();
            running = false;
            post(() -> cb.onFinish(r.ok(), r.cancelled, jars, r.output));
        }, "hz-build").start();
        return true;
    }

    /** Mod jars produced by the build (skips sources / javadoc / dev jars). */
    public static List<File> findJars(File project, long since) {
        List<File> out = new ArrayList<>();
        collect(new File(project, "build/libs"), out, since);
        collect(new File(project, "target"), out, since);
        collect(new File(project, "build/devlibs"), new ArrayList<File>(), since);
        File[] subs = project.listFiles();
        if (subs != null) for (File s : subs) {
            if (s.isDirectory() && !s.getName().startsWith(".")) {
                collect(new File(s, "build/libs"), out, since);
                collect(new File(s, "target"), out, since);
            }
        }
        return out;
    }

    private static void collect(File dir, List<File> out, long since) {
        File[] fs = dir.listFiles();
        if (fs == null) return;
        for (File f : fs) {
            String n = f.getName().toLowerCase();
            if (!n.endsWith(".jar")) continue;
            if (n.contains("-sources") || n.contains("-javadoc") || n.contains("-dev") || n.startsWith("original-")) continue;
            if (f.lastModified() + 5000 < since) continue;
            out.add(f);
        }
    }

    /** Copies jars to Downloads/HZ so they can be picked up by the Minecraft launcher. */
    public static List<File> export(List<File> jars) {
        List<File> out = new ArrayList<>();
        File dst = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "HZ");
        dst.mkdirs();
        for (File j : jars) {
            File t = new File(dst, j.getName());
            try (java.io.InputStream in = new java.io.FileInputStream(j);
                 java.io.OutputStream o = new java.io.FileOutputStream(t)) {
                byte[] b = new byte[16384];
                int n;
                while ((n = in.read(b)) > 0) o.write(b, 0, n);
                out.add(t);
            } catch (Exception ignored) {}
        }
        return out;
    }

    private void post(Runnable r) { main.post(r); }
}
