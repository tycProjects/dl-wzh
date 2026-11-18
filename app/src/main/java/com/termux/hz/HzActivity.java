package com.termux.hz;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.termux.R;
import com.termux.app.TermuxActivity;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/** HZ main screen: project explorer, editor, agent and console with a Build button on top. */
public class HzActivity extends AppCompatActivity implements ProjectPanel.Host, ConsolePanel.Host, AgentPanel.Host {

    private static final int REQ_ZIP = 11;

    private ProjectPanel projectPanel;
    private EditorPanel editorPanel;
    private AgentPanel agentPanel;
    private ConsolePanel consolePanel;
    private View[] panels;
    private BottomNavigationView nav;
    private TextView buildBtn, subtitle;
    private BuildManager builder;
    private final Sh.Handle cmdHandle = new Sh.Handle();

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        if (!Hz.prefs(this).getBoolean(Hz.K_SETUP_DONE, false) || !Hz.isBootstrapped()) {
            startActivity(new Intent(this, SetupActivity.class));
            finish();
            return;
        }
        builder = new BuildManager(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.BG);
        root.setFitsSystemWindows(true);

        // top bar
        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setBackgroundColor(Ui.SURFACE);
        top.setPadding(Ui.dp(this, 16), Ui.dp(this, 8), Ui.dp(this, 4), Ui.dp(this, 8));
        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        TextView logo = Ui.text(this, "HZ", 20, Ui.ACCENT);
        logo.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        logo.setLetterSpacing(0.1f);
        subtitle = Ui.text(this, "", 11.5f, Ui.MUTED);
        subtitle.setSingleLine(true);
        titles.addView(logo);
        titles.addView(subtitle);
        top.addView(titles, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        buildBtn = Ui.button(this, "▶  Build", Ui.ACCENT, 0xFF06210F, this::onBuildClicked);
        top.addView(buildBtn);
        top.addView(Ui.icon(this, R.drawable.hz_ic_more, "Menu", () -> overflow(top)));
        root.addView(top);

        // panels
        projectPanel = new ProjectPanel(this, this);
        editorPanel = new EditorPanel(this);
        agentPanel = new AgentPanel(this, this);
        consolePanel = new ConsolePanel(this, this);
        panels = new View[]{projectPanel, editorPanel, agentPanel, consolePanel};
        FrameLayout content = new FrameLayout(this);
        for (View p : panels) content.addView(p, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        root.addView(content, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        // bottom navigation
        nav = new BottomNavigationView(this);
        nav.inflateMenu(R.menu.hz_bottom);
        nav.setBackgroundColor(Ui.SURFACE);
        ColorStateList tint = new ColorStateList(new int[][]{{android.R.attr.state_checked}, {}}, new int[]{Ui.ACCENT, Ui.MUTED});
        nav.setItemIconTintList(tint);
        nav.setItemTextColor(tint);
        nav.setItemActiveIndicatorColor(ColorStateList.valueOf(0x334ADE80));
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            show(id == R.id.nav_project ? 0 : id == R.id.nav_editor ? 1 : id == R.id.nav_agent ? 2 : 3);
            return true;
        });
        root.addView(nav);
        setContentView(root);
        show(0);
        projectChanged(projectPanel.project());
    }

    private void show(int i) {
        for (int k = 0; k < panels.length; k++) panels[k].setVisibility(k == i ? View.VISIBLE : View.GONE);
        int id = i == 0 ? R.id.nav_project : i == 1 ? R.id.nav_editor : i == 2 ? R.id.nav_agent : R.id.nav_console;
        if (nav.getSelectedItemId() != id) nav.setSelectedItemId(id);
    }

    private int currentTab() {
        for (int k = 0; k < panels.length; k++) if (panels[k].getVisibility() == View.VISIBLE) return k;
        return 0;
    }

    @Override public void onBackPressed() {
        if (currentTab() != 0) show(0); else super.onBackPressed();
    }

    @Override protected void onPause() {
        super.onPause();
        if (editorPanel != null) editorPanel.saveAll();
    }

    @Override protected void onResume() {
        super.onResume();
        if (agentPanel != null) agentPanel.refreshHeader();
    }

    private void overflow(View anchor) {
        PopupMenu m = new PopupMenu(this, anchor);
        m.getMenu().add(0, 1, 0, "Save all");
        m.getMenu().add(0, 2, 1, "Terminal");
        m.getMenu().add(0, 3, 2, "Settings");
        m.getMenu().add(0, 4, 3, "Install more Java versions");
        m.getMenu().add(0, 5, 4, "About HZ");
        m.setOnMenuItemClickListener(it -> {
            switch (it.getItemId()) {
                case 1: editorPanel.saveAll(); Ui.toast(this, "Saved"); break;
                case 2: openTerminal(); break;
                case 3: openSettings(); break;
                case 4: startActivity(new Intent(this, SetupActivity.class).putExtra(SetupActivity.EXTRA_ONLY, new int[]{8, 11, 17, 21})); break;
                case 5: new MaterialAlertDialogBuilder(this).setTitle("HZ")
                        .setMessage("Minecraft mod builder for Android, powered by Termux.\nJava 8–21 · Gradle & Maven · AI agent (Groq / OpenRouter).")
                        .setPositiveButton("OK", null).show(); break;
            }
            return true;
        });
        m.show();
    }

    // ---------- build ----------

    private void onBuildClicked() {
        if (builder.isRunning()) { builder.cancel(); return; }
        startBuild(null);
    }

    private void setBuilding(boolean on) {
        buildBtn.setText(on ? "■  Stop" : "▶  Build");
        buildBtn.setTextColor(on ? 0xFFFFFFFF : 0xFF06210F);
        buildBtn.setBackground(Ui.ripple(Ui.round(on ? Ui.DANGER : Ui.ACCENT, 10, this), this));
    }

    private boolean startBuild(BuildManager.Callback extra) {
        File proj = projectPanel.project();
        if (proj == null) { Ui.toast(this, "Open or import a project first"); show(0); return false; }
        editorPanel.saveAll();
        show(3);
        consolePanel.clear();
        return builder.build(proj, new BuildManager.Callback() {
            @Override public void onStart(String command, int javaVersion) {
                setBuilding(true);
                consolePanel.setStatus("Building with Java " + javaVersion + "…", Ui.WARN, true);
                consolePanel.appendColored("== HZ build · " + proj.getName() + " · Java " + javaVersion + " ==", Ui.ACCENT);
                consolePanel.appendColored("$ " + command, 0xFF7FDBCA);
                if (extra != null) extra.onStart(command, javaVersion);
            }
            @Override public void onLine(String line) { consolePanel.append(line); }
            @Override public void onFinish(boolean success, boolean cancelled, List<File> jars, String log) {
                setBuilding(false);
                if (cancelled) consolePanel.setStatus("Build cancelled", Ui.MUTED, false);
                else if (success) {
                    List<File> out = BuildManager.export(jars);
                    consolePanel.setStatus("Build successful", Ui.ACCENT, false);
                    consolePanel.appendColored("\nBUILD SUCCESSFUL", Ui.ACCENT);
                    for (File j : jars) consolePanel.appendColored("  jar: " + FileOps.rel(proj, j), Ui.ACCENT);
                    if (!out.isEmpty()) consolePanel.appendColored("  copied to " + out.get(0).getParent(), Ui.ACCENT);
                    else if (jars.isEmpty()) consolePanel.appendColored("  (no jar found in build/libs or target)", Ui.WARN);
                    if (extra == null) showBuildDone(jars, out);
                } else {
                    consolePanel.setStatus("Build failed", Ui.DANGER, false);
                    consolePanel.appendColored("\nBUILD FAILED", Ui.DANGER);
                    if (extra == null) showBuildFailed(log);
                }
                projectPanel.rebuild();
                if (extra != null) extra.onFinish(success, cancelled, jars, log);
            }
        });
    }

    private void showBuildDone(List<File> jars, List<File> out) {
        StringBuilder sb = new StringBuilder();
        for (File j : jars) sb.append("• ").append(j.getName()).append('\n');
        if (jars.isEmpty()) sb.append("No jar was found. Check the build output.");
        else if (!out.isEmpty()) sb.append("\nSaved to Downloads/HZ");
        MaterialAlertDialogBuilder d = new MaterialAlertDialogBuilder(this).setTitle("Build successful").setMessage(sb.toString())
                .setPositiveButton("OK", null);
        d.show();
    }

    private void showBuildFailed(String log) {
        new MaterialAlertDialogBuilder(this).setTitle("Build failed")
                .setMessage("The build did not finish. Let the agent read the errors and fix them?")
                .setPositiveButton("Fix with agent", (d, w) -> askFix(Sh.tail(log, 6000)))
                .setNegativeButton("Close", null).show();
    }

    // ---------- Console.Host ----------

    @Override public void stop() { builder.cancel(); cmdHandle.cancel(); }

    @Override public void askFix(String logTail) {
        show(2);
        agentPanel.send("The build failed. Read the errors below, inspect the relevant files, fix them, and run build_project again until it passes.\n\n```\n" + logTail + "\n```");
    }

    @Override public void openTerminal() { startActivity(new Intent(this, TermuxActivity.class)); }

    @Override public void runCommand(String cmd) {
        File proj = projectPanel.project();
        consolePanel.appendColored("$ " + cmd, 0xFF7FDBCA);
        consolePanel.setStatus("Running command…", Ui.WARN, true);
        cmdHandle.cancelled = false;
        new Thread(() -> {
            Sh.Result r = Sh.run(this, cmd, proj != null ? proj : new File(Hz.HOME), javaHomeFor(proj), 1024,
                    l -> runOnUiThread(() -> consolePanel.append(l)), cmdHandle);
            runOnUiThread(() -> {
                consolePanel.setStatus(r.ok() ? "Done" : "Exit code " + r.code, r.ok() ? Ui.ACCENT : Ui.DANGER, false);
                projectPanel.rebuild();
            });
        }).start();
    }

    private String javaHomeFor(File proj) {
        ProjectInfo info = proj != null ? ProjectInfo.detect(proj) : null;
        File h = JdkManager.home(JdkManager.resolve(this, info, proj != null ? proj.getName() : ""));
        return h != null ? h.getAbsolutePath() : null;
    }

    // ---------- Project.Host ----------

    @Override public void pickZip() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        i.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/zip", "application/x-zip-compressed", "application/octet-stream"});
        startActivityForResult(i, REQ_ZIP);
    }

    @Override protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req != REQ_ZIP || res != Activity.RESULT_OK || data == null || data.getData() == null) return;
        final Uri uri = data.getData();
        String name = "project";
        try (Cursor c = getContentResolver().query(uri, null, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                int ix = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (ix >= 0) name = c.getString(ix);
            }
        } catch (Exception ignored) {}
        name = name.replaceAll("(?i)\\.zip$", "").replaceAll("[^A-Za-z0-9._-]", "_");
        File dest = new File(Hz.projectsDir(), name);
        int n = 2;
        while (dest.exists()) dest = new File(Hz.projectsDir(), name + "-" + n++);
        final File d = dest;
        Ui.toast(this, "Importing…");
        new Thread(() -> {
            try (InputStream in = getContentResolver().openInputStream(uri)) {
                FileOps.unzip(in, d);
                runOnUiThread(() -> { projectPanel.setProject(d); Ui.toast(this, "Imported " + d.getName()); });
            } catch (Exception e) {
                runOnUiThread(() -> Ui.toast(this, "Import failed: " + e.getMessage()));
            }
        }).start();
    }

    @Override public void openFile(File f) { editorPanel.open(f); show(1); }

    @Override public void fileChanged(File f) {
        editorPanel.onDiskChanged(f);
        projectPanel.rebuild();
    }

    @Override public void fileRenamed(File from, File to) { editorPanel.onRenamed(from, to); }

    @Override public void projectChanged(File p) {
        editorPanel.closeAll();
        subtitle.setText(p == null ? "No project" : p.getName() + "  ·  " + ProjectInfo.detect(p).summary());
    }

    @Override public void askAgent(String prompt) { show(2); agentPanel.send(prompt); }

    @Override public void runInConsole(String title, String script, File cwd, Runnable done) {
        show(3);
        consolePanel.appendColored("== " + title + " ==", Ui.ACCENT);
        consolePanel.appendColored("$ " + script, 0xFF7FDBCA);
        consolePanel.setStatus(title + "…", Ui.WARN, true);
        cmdHandle.cancelled = false;
        new Thread(() -> {
            Sh.Result r = Sh.run(this, script, cwd, null, 0, l -> runOnUiThread(() -> consolePanel.append(l)), cmdHandle);
            runOnUiThread(() -> {
                consolePanel.setStatus(r.ok() ? "Done" : "Failed (exit " + r.code + ")", r.ok() ? Ui.ACCENT : Ui.DANGER, false);
                if (done != null) done.run();
                projectPanel.rebuild();
            });
        }).start();
    }

    // ---------- Agent.Host ----------

    @Override public File project() { return projectPanel.project(); }

    @Override public String buildBlocking() {
        if (builder.isRunning()) return "ERROR: a build is already running.";
        final CountDownLatch done = new CountDownLatch(1);
        final String[] result = {""};
        runOnUiThread(() -> { if (!startBuild(new BuildManager.Callback() {
            @Override public void onStart(String command, int javaVersion) {}
            @Override public void onLine(String line) {}
            @Override public void onFinish(boolean success, boolean cancelled, List<File> jars, String log) {
                if (cancelled) result[0] = "BUILD CANCELLED";
                else if (success) {
                    StringBuilder sb = new StringBuilder("BUILD SUCCESS");
                    for (File j : jars) sb.append("\njar: ").append(j.getName());
                    result[0] = sb.toString();
                } else result[0] = "BUILD FAILED\n" + errorSummary(log);
                done.countDown();
            }
        })) { result[0] = "ERROR: could not start the build (no project open or a build is running)."; done.countDown(); } });
        try { done.await(); } catch (InterruptedException e) { return "ERROR: interrupted"; }
        return result[0];
    }

    /** Keeps the compile errors plus the end of the log so the model sees what matters. */
    static String errorSummary(String log) {
        String[] lines = log.split("\n");
        List<String> keep = new ArrayList<>();
        for (int i = 0; i < lines.length; i++) {
            String l = lines[i], s = l.toLowerCase();
            if (s.contains("error:") || s.contains("error ") || s.contains("[error]") || s.contains("what went wrong")
                    || s.contains("could not") || s.startsWith("e: ") || s.contains("exception") || s.contains("cannot find symbol")) {
                keep.add(l);
                if (i + 1 < lines.length && (s.contains("error:") || s.contains("cannot find symbol"))) keep.add(lines[i + 1]);
            }
        }
        StringBuilder sb = new StringBuilder();
        int from = Math.max(0, keep.size() - 60);
        for (int i = from; i < keep.size(); i++) sb.append(keep.get(i)).append('\n');
        sb.append("\n--- end of log ---\n");
        int s0 = Math.max(0, lines.length - 25);
        for (int i = s0; i < lines.length; i++) sb.append(lines[i]).append('\n');
        return Sh.tail(sb.toString(), 7000);
    }

    @Override public Sh.Result runCommand(String cmd, Sh.Handle h) {
        File proj = projectPanel.project();
        runOnUiThread(() -> consolePanel.appendColored("[agent] $ " + cmd, 0xFF7FDBCA));
        return Sh.run(this, cmd, proj, javaHomeFor(proj), 1024, l -> runOnUiThread(() -> consolePanel.append(l)), h);
    }

    @Override public void openSettings() { startActivity(new Intent(this, HzSettingsActivity.class)); }
}
