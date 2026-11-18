package com.termux.hz;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.termux.app.TermuxInstaller;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** First-run installer: Termux bootstrap, base tools and JDKs. */
public class SetupActivity extends AppCompatActivity {

    public static final String EXTRA_ONLY = "only_versions";

    private final Handler ui = new Handler(Looper.getMainLooper());
    private final List<TextView> stepViews = new ArrayList<>();
    private final List<String> stepNames = new ArrayList<>();
    private final CheckBox[] jdkBoxes = new CheckBox[Hz.JAVA_VERSIONS.length];
    private TextView log, startBtn, subtitle;
    private ScrollView logScroll;
    private ProgressBar bar;
    private volatile boolean running;
    private boolean onlyMode;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        int[] only = getIntent().getIntArrayExtra(EXTRA_ONLY);
        onlyMode = only != null && only.length > 0;

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.BG);
        root.setPadding(Ui.dp(this, 20), Ui.dp(this, 36), Ui.dp(this, 20), Ui.dp(this, 16));

        TextView title = Ui.text(this, onlyMode ? "Installing Java" : "Welcome to HZ", 26, Ui.TEXT);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        subtitle = Ui.text(this, "HZ needs Java and build tools to compile Minecraft mods. This downloads about 300–800 MB, so use Wi-Fi.", 14, Ui.MUTED);
        subtitle.setPadding(0, Ui.dp(this, 6), 0, Ui.dp(this, 14));
        root.addView(title);
        root.addView(subtitle);

        TextView lbl = Ui.text(this, "JAVA VERSIONS", 11, Ui.MUTED);
        lbl.setTypeface(Typeface.DEFAULT_BOLD);
        root.addView(lbl);
        LinearLayout row = new LinearLayout(this);
        String saved = Hz.prefs(this).getString(Hz.K_JDKS_WANTED, "8,11,17,21");
        for (int i = 0; i < Hz.JAVA_VERSIONS.length; i++) {
            int v = Hz.JAVA_VERSIONS[i];
            CheckBox cb = new CheckBox(this);
            cb.setText("Java " + v);
            cb.setTextColor(Ui.TEXT);
            boolean on = onlyMode ? contains(only, v) : (saved.contains(String.valueOf(v)) || JdkManager.installed(v));
            cb.setChecked(on);
            jdkBoxes[i] = cb;
            row.addView(cb, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        }
        root.addView(row);

        LinearLayout steps = new LinearLayout(this);
        steps.setOrientation(LinearLayout.VERTICAL);
        steps.setPadding(0, Ui.dp(this, 10), 0, Ui.dp(this, 6));
        String[] names = onlyMode ? new String[]{"Update package lists", "Install selected Java versions", "Finish"}
                : new String[]{"Storage permission", "Termux environment", "Update package lists", "Install build tools (git, curl, zip…)",
                "Install selected Java versions", "Configure Gradle", "Finish"};
        for (String n : names) {
            TextView t = Ui.text(this, "○  " + n, 14, Ui.MUTED);
            t.setPadding(0, Ui.dp(this, 4), 0, Ui.dp(this, 4));
            stepViews.add(t);
            stepNames.add(n);
            steps.addView(t);
        }
        root.addView(steps);
        bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(names.length);
        root.addView(bar);

        log = new TextView(this);
        log.setTypeface(Typeface.MONOSPACE);
        log.setTextSize(10.5f);
        log.setTextColor(0xFFB6BFD1);
        log.setPadding(Ui.dp(this, 10), Ui.dp(this, 8), Ui.dp(this, 10), Ui.dp(this, 8));
        log.setBackground(Ui.round(0xFF12151B, 10, this));
        logScroll = new ScrollView(this);
        logScroll.addView(log);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        lp.topMargin = Ui.dp(this, 8);
        root.addView(logScroll, lp);

        startBtn = Ui.button(this, "Download & install", Ui.ACCENT, 0xFF06210F, this::start);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bp.topMargin = Ui.dp(this, 12);
        root.addView(startBtn, bp);
        setContentView(root);

        if (onlyMode || !Hz.prefs(this).getBoolean(Hz.K_SETUP_DONE, false)) {
            if (onlyMode) ui.postDelayed(this::start, 300);
        }
    }

    private static boolean contains(int[] a, int v) { for (int x : a) if (x == v) return true; return false; }

    private void say(String s) {
        ui.post(() -> {
            CharSequence cur = log.getText();
            if (cur.length() > 20000) cur = cur.subSequence(cur.length() - 12000, cur.length());
            log.setText(cur + s + "\n");
            logScroll.post(() -> logScroll.fullScroll(ScrollView.FOCUS_DOWN));
        });
    }

    private void mark(int i, String icon, int color) {
        ui.post(() -> {
            stepViews.get(i).setText(icon + "  " + stepNames.get(i));
            stepViews.get(i).setTextColor(color);
            bar.setProgress(icon.equals("✓") ? i + 1 : i);
        });
    }

    private void start() {
        if (running) return;
        running = true;
        startBtn.setText("Working…");
        startBtn.setAlpha(0.6f);
        for (CheckBox cb : jdkBoxes) cb.setEnabled(false);
        StringBuilder w = new StringBuilder();
        for (int i = 0; i < jdkBoxes.length; i++) if (jdkBoxes[i].isChecked()) w.append(w.length() > 0 ? "," : "").append(Hz.JAVA_VERSIONS[i]);
        Hz.prefs(this).edit().putString(Hz.K_JDKS_WANTED, w.toString()).apply();
        int base = 0;
        if (!onlyMode) {
            base = 1;
            mark(0, "⏳", Ui.WARN);
            requestStorage();
            mark(0, "✓", Ui.ACCENT);
            mark(1, "⏳", Ui.WARN);
            say("Preparing Termux environment…");
            TermuxInstaller.setupBootstrapIfNeeded(this, () -> new Thread(() -> {
                if (!Hz.isBootstrapped()) { fail(1, "Termux environment is not ready."); return; }
                mark(1, "✓", Ui.ACCENT);
                runSteps(2, w.toString());
            }).start());
        } else {
            new Thread(() -> runSteps(0, w.toString())).start();
        }
    }

    private void requestStorage() {
        if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        }
    }

    private boolean sh(String script) {
        Sh.Result r = Sh.run(this, script, new File(Hz.HOME), null, 0, this::say, null);
        return r.ok();
    }

    private void runSteps(int first, String wantedCsv) {
        int i = first;
        // update
        mark(i, "⏳", Ui.WARN);
        say("$ apt-get update");
        sh("apt-get update -y 2>&1 || true");
        mark(i, "✓", Ui.ACCENT);
        i++;
        if (!onlyMode) {
            mark(i, "⏳", Ui.WARN);
            boolean ok = sh("apt-get install -y -o Dpkg::Options::=--force-confnew git curl wget unzip zip tar xz-utils 2>&1");
            if (!ok) { fail(i, "Could not install build tools. Check your connection and retry."); return; }
            mark(i, "✓", Ui.ACCENT);
            i++;
        }
        // java
        mark(i, "⏳", Ui.WARN);
        List<Integer> failed = new ArrayList<>();
        boolean any = false;
        for (String s : wantedCsv.split(",")) {
            if (s.isEmpty()) continue;
            int v = Integer.parseInt(s);
            any = true;
            if (JdkManager.installed(v)) { say("Java " + v + " already installed."); continue; }
            if (!sh(JdkManager.installScript(this, v)) || !JdkManager.installed(v)) failed.add(v);
        }
        boolean anyInstalled = false;
        for (int v : Hz.JAVA_VERSIONS) anyInstalled |= JdkManager.installed(v);
        if (!anyInstalled) { fail(i, "No Java version could be installed. Check the log, then retry."); return; }
        mark(i, failed.isEmpty() ? "✓" : "✓", failed.isEmpty() ? Ui.ACCENT : Ui.WARN);
        if (!failed.isEmpty()) say("\nNote: Java " + failed + " is unavailable from the default sources. Projects needing it will fall back to the nearest installed version. You can set a custom JDK URL in Settings > Java and install it later.");
        i++;
        if (!onlyMode) {
            mark(i, "⏳", Ui.WARN);
            sh("mkdir -p ~/.gradle && grep -q hz-managed ~/.gradle/gradle.properties 2>/dev/null || cat >> ~/.gradle/gradle.properties <<'EOP'\n# hz-managed\norg.gradle.daemon=false\norg.gradle.parallel=false\norg.gradle.jvmargs=-Xmx1536m -Dfile.encoding=UTF-8\nsystemProp.org.gradle.native=false\nEOP\n");
            mark(i, "✓", Ui.ACCENT);
            i++;
        }
        mark(i, "✓", Ui.ACCENT);
        if (!onlyMode) Hz.prefs(this).edit().putBoolean(Hz.K_SETUP_DONE, true).apply();
        ui.post(() -> {
            bar.setProgress(bar.getMax());
            startBtn.setAlpha(1f);
            startBtn.setText(onlyMode ? "Done" : "Open HZ");
            startBtn.setOnClickListener(v -> { if (!onlyMode) startActivity(new Intent(this, HzActivity.class)); finish(); });
        });
        running = false;
    }

    private void fail(int step, String msg) {
        mark(step, "✗", Ui.DANGER);
        say("\n" + msg);
        ui.post(() -> {
            running = false;
            startBtn.setAlpha(1f);
            startBtn.setText("Retry");
            for (CheckBox cb : jdkBoxes) cb.setEnabled(true);
        });
    }
}
