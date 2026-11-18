package com.termux.hz;

import android.content.Context;

import com.termux.shared.termux.shell.command.environment.TermuxShellEnvironment;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/** Runs commands inside the Termux environment (bash, PATH, PREFIX ...). */
public final class Sh {
    public interface Listener { void onLine(String line); }

    public static final class Result {
        public final int code;
        public final String output;
        public final boolean cancelled;
        Result(int code, String output, boolean cancelled) {
            this.code = code; this.output = output; this.cancelled = cancelled;
        }
        public boolean ok() { return code == 0 && !cancelled; }
    }

    /** Handle that lets callers stop a running command. */
    public static final class Handle {
        volatile Process process;
        volatile boolean cancelled;
        public void cancel() {
            cancelled = true;
            Process p = process;
            if (p != null) p.destroy();
        }
    }

    private Sh() {}

    public static Map<String, String> env(Context ctx, String javaHome, int xmxMb) {
        HashMap<String, String> env;
        try {
            env = new TermuxShellEnvironment().getEnvironment(ctx.getApplicationContext(), false);
        } catch (Throwable t) {
            env = new HashMap<>();
            env.put("HOME", Hz.HOME);
            env.put("PREFIX", Hz.PREFIX);
            env.put("PATH", Hz.PREFIX + "/bin");
            env.put("TMPDIR", Hz.PREFIX + "/tmp");
        }
        env.put("LANG", "en_US.UTF-8");
        env.put("TERM", "dumb");
        env.put("DEBIAN_FRONTEND", "noninteractive");
        env.put("NO_COLOR", "1");
        if (javaHome != null) {
            env.put("JAVA_HOME", javaHome);
            env.put("PATH", javaHome + "/bin:" + env.get("PATH"));
        }
        if (xmxMb > 0) {
            env.put("GRADLE_OPTS", "-Dorg.gradle.native=false -Dfile.encoding=UTF-8 -Xmx" + xmxMb + "m");
            env.put("MAVEN_OPTS", "-Xmx" + xmxMb + "m");
        }
        return env;
    }

    /** Runs a bash script. Blocking; call from a worker thread. */
    public static Result run(Context ctx, String script, File cwd, String javaHome, int xmxMb,
                             Listener listener, Handle handle) {
        StringBuilder all = new StringBuilder();
        try {
            ProcessBuilder pb = new ProcessBuilder(Hz.PREFIX + "/bin/bash", "-c", script);
            pb.directory(cwd != null && cwd.isDirectory() ? cwd : new File(Hz.HOME));
            pb.redirectErrorStream(true);
            pb.environment().clear();
            pb.environment().putAll(env(ctx, javaHome, xmxMb));
            Process p = pb.start();
            if (handle != null) {
                handle.process = p;
                if (handle.cancelled) p.destroy();
            }
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), "UTF-8"))) {
                String line;
                while ((line = r.readLine()) != null) {
                    line = stripAnsi(line);
                    if (all.length() < 400_000) all.append(line).append('\n');
                    if (listener != null) listener.onLine(line);
                }
            }
            int code = p.waitFor();
            return new Result(code, all.toString(), handle != null && handle.cancelled);
        } catch (IOException | InterruptedException e) {
            String msg = "Failed to run command: " + e.getMessage();
            if (listener != null) listener.onLine(msg);
            all.append(msg).append('\n');
            return new Result(-1, all.toString(), handle != null && handle.cancelled);
        }
    }

    public static Result run(Context ctx, String script, File cwd, Listener l) {
        return run(ctx, script, cwd, null, 0, l, null);
    }

    public static String stripAnsi(String s) {
        return s.replaceAll("\u001B\\[[0-9;?]*[ -/]*[@-~]", "").replace("\r", "");
    }

    public static String quote(String s) {
        return "'" + s.replace("'", "'\\''") + "'";
    }

    public static String tail(String s, int maxChars) {
        if (s.length() <= maxChars) return s;
        return "…(truncated)…\n" + s.substring(s.length() - maxChars);
    }

    /** Keeps the last N chars of a result holder (used by the AtomicReference helper). */
    public static void set(AtomicReference<String> ref, String v) { ref.set(v); }
}
