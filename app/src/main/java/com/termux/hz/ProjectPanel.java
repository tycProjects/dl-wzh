package com.termux.hz;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.termux.R;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Project switcher + file explorer. */
public class ProjectPanel extends LinearLayout {

    public interface Host {
        void pickZip();
        void openFile(File f);
        void fileChanged(File f);
        void fileRenamed(File from, File to);
        void projectChanged(File project);
        void runInConsole(String title, String script, File cwd, Runnable done);
        void askAgent(String prompt);
    }

    private static final class Row { File f; int depth; Row(File f, int d) { this.f = f; depth = d; } }

    private final Activity act;
    private final Host host;
    private File project;
    private final Set<String> expanded = new HashSet<>();
    private final List<Row> rows = new ArrayList<>();
    private final ListView list;
    private final TextView title, info;
    private final LinearLayout emptyBox;
    private final LinearLayout header;
    private final ArrayAdapter<Row> adapter;

    public ProjectPanel(Activity a, Host h) {
        super(a);
        act = a; host = h;
        setOrientation(VERTICAL);
        setBackgroundColor(Ui.BG);

        header = new LinearLayout(a);
        header.setOrientation(HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setBackgroundColor(Ui.SURFACE);
        header.setPadding(Ui.dp(a, 16), Ui.dp(a, 8), Ui.dp(a, 4), Ui.dp(a, 8));
        LinearLayout tx = new LinearLayout(a);
        tx.setOrientation(VERTICAL);
        title = Ui.text(a, "No project", 16, Ui.TEXT);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        info = Ui.text(a, "Import a project to begin", 12, Ui.MUTED);
        tx.addView(title);
        tx.addView(info);
        header.addView(tx, new LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        header.addView(Ui.icon(a, R.drawable.hz_ic_search, "Search in project", this::searchDialog));
        header.addView(Ui.icon(a, R.drawable.hz_ic_add, "New / import", () -> showAddMenu(header)));
        header.addView(Ui.icon(a, R.drawable.hz_ic_more, "Project menu", () -> showProjectMenu(header)));
        header.setOnClickListener(v -> switchDialog());
        addView(header);
        addView(Ui.divider(a));

        emptyBox = new LinearLayout(a);
        emptyBox.setOrientation(VERTICAL);
        emptyBox.setGravity(Gravity.CENTER);
        emptyBox.setPadding(Ui.dp(a, 32), Ui.dp(a, 32), Ui.dp(a, 32), Ui.dp(a, 32));
        TextView hi = Ui.text(a, "Build Minecraft mods on your phone", 20, Ui.TEXT);
        hi.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        hi.setGravity(Gravity.CENTER);
        TextView sub = Ui.text(a, "Import a project (.zip), clone a Git repo, or start from a Fabric / NeoForge template. Then tap Build.", 14, Ui.MUTED);
        sub.setGravity(Gravity.CENTER);
        sub.setPadding(0, Ui.dp(a, 10), 0, Ui.dp(a, 20));
        emptyBox.addView(hi);
        emptyBox.addView(sub);
        emptyBox.addView(btn("Import ZIP", Ui.ACCENT, 0xFF06210F, host::pickZip));
        emptyBox.addView(btn("Clone from Git", Ui.SURFACE2, Ui.TEXT, this::cloneDialog));
        emptyBox.addView(btn("Fabric template", Ui.SURFACE2, Ui.TEXT, () -> clone("https://github.com/FabricMC/fabric-example-mod", "fabric-example")));
        emptyBox.addView(btn("NeoForge template", Ui.SURFACE2, Ui.TEXT, () -> clone("https://github.com/neoforged/MDK", "neoforge-mdk")));
        addView(emptyBox, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        adapter = new ArrayAdapter<Row>(a, 0, rows) {
            @Override public View getView(int pos, View cv, ViewGroup parent) {
                Row r = rows.get(pos);
                LinearLayout l = new LinearLayout(a);
                l.setGravity(Gravity.CENTER_VERTICAL);
                l.setPadding(Ui.dp(a, 12 + r.depth * 16), 0, Ui.dp(a, 12), 0);
                android.widget.ImageView ic = new android.widget.ImageView(a);
                ic.setImageResource(r.f.isDirectory() ? R.drawable.hz_ic_folder : R.drawable.hz_ic_file);
                ic.setColorFilter(r.f.isDirectory() ? Ui.WARN : iconColor(r.f.getName()));
                l.addView(ic, new LayoutParams(Ui.dp(a, 20), Ui.dp(a, 20)));
                TextView n = Ui.text(a, r.f.getName(), 14, Ui.TEXT);
                n.setSingleLine(true);
                n.setPadding(Ui.dp(a, 10), 0, 0, 0);
                l.addView(n, new LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                if (r.f.isDirectory()) {
                    android.widget.ImageView ch = new android.widget.ImageView(a);
                    ch.setImageResource(R.drawable.hz_ic_chevron);
                    ch.setColorFilter(Ui.MUTED);
                    ch.setRotation(expanded.contains(r.f.getAbsolutePath()) ? 90 : 0);
                    l.addView(ch, new LayoutParams(Ui.dp(a, 18), Ui.dp(a, 18)));
                }
                l.setLayoutParams(new ListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(a, 44)));
                return l;
            }
        };
        list = new ListView(a);
        list.setDivider(null);
        list.setAdapter(adapter);
        list.setOnItemClickListener((p, v, pos, id) -> {
            Row r = rows.get(pos);
            if (r.f.isDirectory()) {
                String k = r.f.getAbsolutePath();
                if (!expanded.remove(k)) expanded.add(k);
                rebuild();
            } else host.openFile(r.f);
        });
        list.setOnItemLongClickListener((p, v, pos, id) -> { rowMenu(v, rows.get(pos).f); return true; });
        addView(list, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        String last = Hz.prefs(a).getString(Hz.K_LAST_PROJECT, "");
        File lf = new File(Hz.projectsDir(), last);
        if (!last.isEmpty() && lf.isDirectory()) project = lf;
        else {
            File[] ps = Hz.projectsDir().listFiles(File::isDirectory);
            if (ps != null && ps.length > 0) project = ps[0];
        }
        rebuild();
    }

    private TextView btn(String t, int bg, int fg, Runnable r) {
        TextView b = Ui.button(act, t, bg, fg, r);
        LayoutParams p = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.topMargin = Ui.dp(act, 10);
        b.setLayoutParams(p);
        return b;
    }

    private static int iconColor(String n) {
        n = n.toLowerCase();
        if (n.endsWith(".java") || n.endsWith(".kt")) return 0xFFF59E0B;
        if (n.endsWith(".json") || n.endsWith(".toml")) return 0xFF38BDF8;
        if (n.endsWith(".gradle") || n.endsWith(".kts") || n.endsWith(".properties")) return 0xFF4ADE80;
        if (n.endsWith(".png") || n.endsWith(".jar")) return 0xFFF472B6;
        return 0xFF8A93A5;
    }

    public File project() { return project; }

    public void setProject(File p) {
        project = p;
        Hz.prefs(act).edit().putString(Hz.K_LAST_PROJECT, p == null ? "" : p.getName()).apply();
        expanded.clear();
        if (p != null) expanded.add(p.getAbsolutePath());
        rebuild();
        host.projectChanged(p);
    }

    public void rebuild() {
        rows.clear();
        boolean has = project != null && project.isDirectory();
        emptyBox.setVisibility(has ? GONE : VISIBLE);
        list.setVisibility(has ? VISIBLE : GONE);
        title.setText(has ? project.getName() : "No project");
        if (has) {
            info.setText(ProjectInfo.detect(project).summary());
            walk(project, 0);
        } else info.setText("Import a project to begin");
        adapter.notifyDataSetChanged();
    }

    private void walk(File dir, int depth) {
        for (File f : FileOps.list(dir)) {
            String n = f.getName();
            if (f.isDirectory() && (n.equals(".git") || n.equals(".gradle") || n.equals(".idea"))) continue;
            rows.add(new Row(f, depth));
            if (f.isDirectory() && expanded.contains(f.getAbsolutePath())) walk(f, depth + 1);
        }
    }

    // ---------- menus ----------

    private void showAddMenu(View anchor) {
        PopupMenu m = new PopupMenu(act, anchor);
        m.getMenu().add(0, 1, 0, "Import ZIP project…");
        m.getMenu().add(0, 2, 1, "Clone from Git…");
        m.getMenu().add(0, 3, 2, "Fabric example template");
        m.getMenu().add(0, 4, 3, "NeoForge MDK template");
        m.getMenu().add(0, 5, 4, "New empty project…");
        if (project != null) {
            m.getMenu().add(0, 6, 5, "New file…");
            m.getMenu().add(0, 7, 6, "New folder…");
        }
        m.setOnMenuItemClickListener(it -> {
            switch (it.getItemId()) {
                case 1: host.pickZip(); break;
                case 2: cloneDialog(); break;
                case 3: clone("https://github.com/FabricMC/fabric-example-mod", "fabric-example"); break;
                case 4: clone("https://github.com/neoforged/MDK", "neoforge-mdk"); break;
                case 5: Ui.prompt(act, "New project", "", "Project name", n -> { if (!n.isEmpty()) { File d = uniqueDir(n); d.mkdirs(); setProject(d); } }); break;
                case 6: newFile(targetDir(null), false); break;
                case 7: newFile(targetDir(null), true); break;
            }
            return true;
        });
        m.show();
    }

    private void showProjectMenu(View anchor) {
        PopupMenu m = new PopupMenu(act, anchor);
        m.getMenu().add(0, 1, 0, "Switch project…");
        if (project != null) {
            m.getMenu().add(0, 2, 1, "Java version for this project…");
            m.getMenu().add(0, 3, 2, "Export as ZIP (Downloads/HZ)");
            m.getMenu().add(0, 4, 3, "Rename project…");
            m.getMenu().add(0, 5, 4, "Delete project…");
            m.getMenu().add(0, 6, 5, "Refresh");
            m.getMenu().add(0, 7, 6, "Ask agent to explain project");
            m.getMenu().add(0, 8, 7, "Git status");
        }
        m.setOnMenuItemClickListener(it -> {
            switch (it.getItemId()) {
                case 1: switchDialog(); break;
                case 2: javaDialog(); break;
                case 3: exportZip(); break;
                case 4: Ui.prompt(act, "Rename project", project.getName(), "Name", n -> {
                    if (n.isEmpty()) return;
                    File d = new File(Hz.projectsDir(), n);
                    if (d.exists()) { Ui.toast(act, "Name already used"); return; }
                    if (project.renameTo(d)) setProject(d); }); break;
                case 5: Ui.confirm(act, "Delete project", "Delete " + project.getName() + " and all its files?", "Delete", () -> {
                    FileOps.deleteTree(project); setProject(null); }); break;
                case 6: rebuild(); break;
                case 7: host.askAgent("Explore this project and explain what it does: loader, Minecraft version, main classes, and anything that looks wrong."); break;
                case 8: host.runInConsole("git status", "git status && git log --oneline -5", project, null); break;
            }
            return true;
        });
        m.show();
    }

    private File targetDir(File f) {
        if (f == null) return project;
        return f.isDirectory() ? f : f.getParentFile();
    }

    private void rowMenu(View anchor, File f) {
        PopupMenu m = new PopupMenu(act, anchor);
        m.getMenu().add(0, 1, 0, "New file here…");
        m.getMenu().add(0, 2, 1, "New folder here…");
        m.getMenu().add(0, 3, 2, "Rename…");
        m.getMenu().add(0, 4, 3, "Duplicate");
        m.getMenu().add(0, 5, 4, "Delete");
        m.getMenu().add(0, 6, 5, "Copy relative path");
        m.setOnMenuItemClickListener(it -> {
            switch (it.getItemId()) {
                case 1: newFile(targetDir(f), false); break;
                case 2: newFile(targetDir(f), true); break;
                case 3: Ui.prompt(act, "Rename", f.getName(), "Name", n -> {
                    if (n.isEmpty()) return;
                    File t = new File(f.getParentFile(), n);
                    if (t.exists()) { Ui.toast(act, "Already exists"); return; }
                    if (f.renameTo(t)) { host.fileRenamed(f, t); rebuild(); } }); break;
                case 4: try {
                    File t = new File(f.getParentFile(), "copy_of_" + f.getName());
                    FileOps.copyTree(f, t); rebuild();
                } catch (Exception e) { Ui.toast(act, e.getMessage()); } break;
                case 5: Ui.confirm(act, "Delete", "Delete " + f.getName() + "?", "Delete", () -> {
                    FileOps.deleteTree(f); host.fileChanged(f); rebuild(); }); break;
                case 6: {
                    android.content.ClipboardManager cm = (android.content.ClipboardManager) act.getSystemService(Context.CLIPBOARD_SERVICE);
                    cm.setPrimaryClip(android.content.ClipData.newPlainText("path", FileOps.rel(project, f)));
                    Ui.toast(act, "Copied");
                    break;
                }
            }
            return true;
        });
        m.show();
    }

    private void newFile(File dir, boolean folder) {
        if (dir == null) return;
        Ui.prompt(act, folder ? "New folder" : "New file", "", folder ? "folder/name" : "src/main/java/Example.java", n -> {
            if (n.isEmpty()) return;
            try {
                File t = FileOps.safe(project, new File(dir, n).getAbsolutePath());
                if (folder) t.mkdirs();
                else {
                    if (t.exists()) { Ui.toast(act, "Already exists"); return; }
                    FileOps.writeText(t, "");
                }
                expanded.add(dir.getAbsolutePath());
                rebuild();
                if (!folder) host.openFile(t);
            } catch (Exception e) { Ui.toast(act, e.getMessage()); }
        });
    }

    private File uniqueDir(String name) {
        String base = name.replaceAll("[^A-Za-z0-9._-]", "_");
        if (base.isEmpty()) base = "project";
        File d = new File(Hz.projectsDir(), base);
        int i = 2;
        while (d.exists()) d = new File(Hz.projectsDir(), base + "-" + i++);
        return d;
    }

    private void switchDialog() {
        File[] ps = Hz.projectsDir().listFiles(File::isDirectory);
        if (ps == null || ps.length == 0) { Ui.toast(act, "No projects yet"); return; }
        final String[] names = new String[ps.length];
        for (int i = 0; i < ps.length; i++) names[i] = ps[i].getName();
        final File[] fs = ps;
        new MaterialAlertDialogBuilder(act).setTitle("Projects").setItems(names, (d, w) -> setProject(fs[w])).show();
    }

    private void javaDialog() {
        String[] opts = {"Auto-detect", "Java 8", "Java 11", "Java 17", "Java 21"};
        int[] vals = {0, 8, 11, 17, 21};
        new MaterialAlertDialogBuilder(act).setTitle("Java version for " + project.getName())
                .setItems(opts, (d, w) -> {
                    Hz.prefs(act).edit().putInt(Hz.K_JAVA_PREFIX + project.getName(), vals[w]).apply();
                    Ui.toast(act, "Using " + opts[w]);
                }).show();
    }

    private void exportZip() {
        try {
            File dir = new File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS), "HZ");
            dir.mkdirs();
            File out = new File(dir, project.getName() + ".zip");
            FileOps.zip(project, out);
            Ui.toast(act, "Saved to " + out.getAbsolutePath());
        } catch (Exception e) { Ui.toast(act, "Export failed: " + e.getMessage()); }
    }

    private void cloneDialog() {
        Ui.prompt(act, "Clone Git repository", "", "https://github.com/user/repo", url -> {
            if (url.isEmpty()) return;
            String n = url.replaceAll("/+$", "").replaceAll("\\.git$", "");
            n = n.substring(n.lastIndexOf('/') + 1);
            clone(url, n);
        });
    }

    private void clone(String url, String name) {
        File d = uniqueDir(name);
        host.runInConsole("git clone", "git clone --depth 1 " + Sh.quote(url) + " " + Sh.quote(d.getAbsolutePath()), Hz.projectsDir(), () -> {
            if (d.isDirectory()) setProject(d);
        });
    }

    // ---------- search ----------

    private void searchDialog() {
        if (project == null) return;
        Ui.prompt(act, "Search in project", "", "Text to find", q -> {
            if (q.isEmpty()) return;
            new Thread(() -> {
                List<FileOps.Hit> hits = FileOps.grep(project, q, false, false, 300);
                act.runOnUiThread(() -> showHits(q, hits));
            }).start();
        });
    }

    private void showHits(String q, List<FileOps.Hit> hits) {
        if (hits.isEmpty()) { Ui.toast(act, "No results for \"" + q + "\""); return; }
        String[] labels = new String[hits.size()];
        for (int i = 0; i < labels.length; i++) {
            FileOps.Hit h = hits.get(i);
            labels[i] = FileOps.rel(project, h.file) + ":" + h.line + "\n" + h.text.trim();
        }
        new MaterialAlertDialogBuilder(act).setTitle(hits.size() + " results")
                .setItems(labels, (d, w) -> host.openFile(hits.get(w).file)).show();
    }
}
