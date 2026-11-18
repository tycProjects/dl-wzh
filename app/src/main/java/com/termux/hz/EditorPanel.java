package com.termux.hz;

import android.app.Activity;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.BackgroundColorSpan;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.termux.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Multi-tab code editor with find/replace, undo/redo and a symbol bar. */
public class EditorPanel extends LinearLayout {

    static final class Doc {
        File file; String text; boolean dirty; int cursor; CodeEditText.History history = new CodeEditText.History();
    }

    private final Activity act;
    private final List<Doc> docs = new ArrayList<>();
    private int current = -1;
    private LinearLayout tabs;
    private CodeEditText editor;
    private TextView empty;
    private LinearLayout findBar;
    private EditText findField, replaceField;
    private TextView findCount;
    private boolean useRegex, matchCase;
    private float fontSp;
    private final BackgroundColorSpan matchSpan = new BackgroundColorSpan(0x99FBBF24);

    public EditorPanel(Activity a) {
        super(a);
        act = a;
        setOrientation(VERTICAL);
        setBackgroundColor(Ui.BG);
        fontSp = Hz.prefs(a).getFloat(Hz.K_FONT_SIZE, 13f);

        // tab strip
        HorizontalScrollView hs = new HorizontalScrollView(a);
        hs.setHorizontalScrollBarEnabled(false);
        hs.setBackgroundColor(Ui.SURFACE);
        tabs = new LinearLayout(a);
        tabs.setOrientation(HORIZONTAL);
        hs.addView(tabs);
        addView(hs, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(a, 40)));

        // action row
        LinearLayout row = new LinearLayout(a);
        row.setBackgroundColor(Ui.SURFACE);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(Ui.icon(a, R.drawable.hz_ic_undo, "Undo", () -> { if (editor.undo()) syncDirty(); }));
        row.addView(Ui.icon(a, R.drawable.hz_ic_redo, "Redo", () -> { if (editor.redo()) syncDirty(); }));
        row.addView(Ui.icon(a, R.drawable.hz_ic_save, "Save", () -> saveCurrent(true)));
        row.addView(Ui.icon(a, R.drawable.hz_ic_search, "Find", this::toggleFind));
        View sp = new View(a);
        row.addView(sp, new LayoutParams(0, 1, 1f));
        row.addView(Ui.icon(a, R.drawable.hz_ic_more, "More", () -> showMore(row)));
        addView(row, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(a, 44)));
        addView(Ui.divider(a));

        // find bar
        findBar = new LinearLayout(a);
        findBar.setOrientation(VERTICAL);
        findBar.setBackgroundColor(Ui.SURFACE);
        findBar.setPadding(Ui.dp(a, 8), Ui.dp(a, 6), Ui.dp(a, 8), Ui.dp(a, 6));
        findBar.setVisibility(GONE);
        findField = Ui.input(a, "Find");
        findField.setSingleLine(true);
        replaceField = Ui.input(a, "Replace with");
        replaceField.setSingleLine(true);
        findCount = Ui.text(a, "", 12, Ui.MUTED);
        LinearLayout r1 = new LinearLayout(a);
        r1.setGravity(Gravity.CENTER_VERTICAL);
        r1.addView(findField, new LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        r1.addView(findCount, new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        LinearLayout r2 = new LinearLayout(a);
        r2.setGravity(Gravity.CENTER_VERTICAL);
        r2.addView(replaceField, new LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        LinearLayout r3 = new LinearLayout(a);
        r3.setGravity(Gravity.CENTER_VERTICAL);
        TextView bPrev = Ui.chip(a, "◀ Prev", () -> findStep(false));
        TextView bNext = Ui.chip(a, "Next ▶", () -> findStep(true));
        TextView bRep = Ui.chip(a, "Replace", this::replaceOne);
        TextView bAll = Ui.chip(a, "All", this::replaceAll);
        TextView bRe = Ui.chip(a, ".*", () -> {});
        TextView bCase = Ui.chip(a, "Aa", () -> {});
        bRe.setOnClickListener(v -> { useRegex = !useRegex; bRe.setTextColor(useRegex ? Ui.ACCENT : Ui.TEXT); updateCount(); });
        bCase.setOnClickListener(v -> { matchCase = !matchCase; bCase.setTextColor(matchCase ? Ui.ACCENT : Ui.TEXT); updateCount(); });
        for (TextView t : new TextView[]{bPrev, bNext, bRep, bAll, bRe, bCase}) {
            LayoutParams p = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            p.setMargins(0, Ui.dp(a, 6), Ui.dp(a, 6), 0);
            r3.addView(t, p);
        }
        findBar.addView(r1);
        findBar.addView(r2, lpTop(a));
        HorizontalScrollView h3 = new HorizontalScrollView(a);
        h3.setHorizontalScrollBarEnabled(false);
        h3.addView(r3);
        findBar.addView(h3);
        addView(findBar);
        findField.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a1, int b, int c) {}
            public void onTextChanged(CharSequence s, int a1, int b, int c) {}
            public void afterTextChanged(Editable s) { updateCount(); }
        });

        // editor + empty state
        editor = new CodeEditText(a);
        editor.setCodeTextSize(fontSp);
        editor.setChangeListener(this::syncDirty);
        empty = Ui.text(a, "Open a file from the Project tab to start editing", 14, Ui.MUTED);
        empty.setGravity(Gravity.CENTER);
        addView(empty, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        addView(editor, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        // symbol bar
        HorizontalScrollView sb = new HorizontalScrollView(a);
        sb.setHorizontalScrollBarEnabled(false);
        sb.setBackgroundColor(Ui.SURFACE);
        LinearLayout keys = new LinearLayout(a);
        String[][] ks = {{"TAB", "    ", "0"}, {"{ }", "{}", "1"}, {"( )", "()", "1"}, {"[ ]", "[]", "1"}, {"\" \"", "\"\"", "1"},
                {";", ";", "0"}, {"<", "<", "0"}, {">", ">", "0"}, {"=", "=", "0"}, {"/", "/", "0"}, {"\\", "\\", "0"},
                {".", ".", "0"}, {",", ",", "0"}, {":", ":", "0"}, {"'", "'", "0"}, {"!", "!", "0"}, {"&", "&", "0"},
                {"|", "|", "0"}, {"_", "_", "0"}, {"#", "#", "0"}, {"$", "$", "0"}, {"@", "@", "0"}};
        for (String[] k : ks) {
            TextView t = Ui.text(a, k[0], 15, Ui.TEXT);
            t.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
            t.setGravity(Gravity.CENTER);
            t.setPadding(Ui.dp(a, 14), 0, Ui.dp(a, 14), 0);
            t.setOnClickListener(v -> { if (current >= 0) editor.insertAtCursor(k[1], Integer.parseInt(k[2])); });
            keys.addView(t, new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, Ui.dp(a, 40)));
        }
        sb.addView(keys);
        addView(sb, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(a, 40)));
        refreshVisibility();
    }

    private LayoutParams lpTop(Activity a) {
        LayoutParams p = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.topMargin = Ui.dp(a, 6);
        return p;
    }

    // ---------- documents ----------

    public void open(File f) {
        for (int i = 0; i < docs.size(); i++) if (docs.get(i).file.equals(f)) { select(i); return; }
        if (f.length() > 2_000_000) { Ui.toast(act, "File too large to edit (>2 MB)"); return; }
        if (FileOps.isBinary(f)) { Ui.toast(act, "Binary file – cannot open as text"); return; }
        try {
            Doc d = new Doc();
            d.file = f;
            d.text = FileOps.readText(f, 2_000_000);
            docs.add(d);
            select(docs.size() - 1);
        } catch (Exception e) { Ui.toast(act, "Cannot open: " + e.getMessage()); }
    }

    private void stash() {
        if (current >= 0 && current < docs.size()) {
            Doc d = docs.get(current);
            d.text = editor.getText().toString();
            d.cursor = editor.getSelectionStart();
        }
    }

    private void select(int i) {
        stash();
        current = i;
        Doc d = docs.get(i);
        editor.load(d.text, d.file.getName(), d.history, d.cursor);
        refreshTabs();
        refreshVisibility();
        updateCount();
    }

    private void refreshVisibility() {
        boolean has = current >= 0;
        editor.setVisibility(has ? VISIBLE : GONE);
        empty.setVisibility(has ? GONE : VISIBLE);
        if (!has) findBar.setVisibility(GONE);
    }

    private void refreshTabs() {
        tabs.removeAllViews();
        for (int i = 0; i < docs.size(); i++) {
            final int idx = i;
            Doc d = docs.get(i);
            LinearLayout t = new LinearLayout(act);
            t.setGravity(Gravity.CENTER_VERTICAL);
            t.setPadding(Ui.dp(act, 12), 0, Ui.dp(act, 4), 0);
            t.setBackgroundColor(i == current ? Ui.BG : Ui.SURFACE);
            TextView n = Ui.text(act, (d.dirty ? "● " : "") + d.file.getName(), 13, i == current ? Ui.TEXT : Ui.MUTED);
            t.addView(n);
            android.widget.ImageButton x = Ui.icon(act, R.drawable.hz_ic_close, "Close", () -> closeDoc(idx));
            x.setLayoutParams(new LayoutParams(Ui.dp(act, 30), Ui.dp(act, 30)));
            x.setPadding(Ui.dp(act, 7), Ui.dp(act, 7), Ui.dp(act, 7), Ui.dp(act, 7));
            t.addView(x);
            t.setOnClickListener(v -> select(idx));
            tabs.addView(t, new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }
    }

    private void closeDoc(int i) {
        Doc d = docs.get(i);
        Runnable close = () -> {
            stash();
            docs.remove(i);
            if (docs.isEmpty()) {
                current = -1;
                editor.load("", "", null, 0);
            } else {
                if (i < current) current--;
                else if (i == current) current = Math.min(i, docs.size() - 1);
                Doc n = docs.get(current);
                editor.load(n.text, n.file.getName(), n.history, n.cursor);
            }
            refreshTabs();
            refreshVisibility();
        };
        if (d.dirty) {
            new com.google.android.material.dialog.MaterialAlertDialogBuilder(act).setTitle("Unsaved changes")
                    .setMessage(d.file.getName() + " has unsaved changes.")
                    .setPositiveButton("Save", (x, w) -> { select(i); saveCurrent(false); close.run(); })
                    .setNegativeButton("Discard", (x, w) -> close.run())
                    .setNeutralButton("Cancel", null).show();
        } else close.run();
    }

    private void syncDirty() {
        if (current < 0) return;
        Doc d = docs.get(current);
        if (!d.dirty) { d.dirty = true; refreshTabs(); }
    }

    public boolean saveCurrent(boolean toast) {
        if (current < 0) return false;
        Doc d = docs.get(current);
        try {
            d.text = editor.getText().toString();
            FileOps.writeText(d.file, d.text);
            d.dirty = false;
            refreshTabs();
            if (toast) Ui.toast(act, "Saved " + d.file.getName());
            return true;
        } catch (Exception e) { Ui.toast(act, "Save failed: " + e.getMessage()); return false; }
    }

    public void saveAll() {
        stash();
        for (Doc d : docs) if (d.dirty) {
            try {
                if (docs.indexOf(d) == current) d.text = editor.getText().toString();
                FileOps.writeText(d.file, d.text);
                d.dirty = false;
            } catch (Exception ignored) {}
        }
        refreshTabs();
    }

    public boolean hasDirty() { for (Doc d : docs) if (d.dirty) return true; return false; }

    public File currentFile() { return current >= 0 ? docs.get(current).file : null; }

    public String currentText() { return current >= 0 ? editor.getText().toString() : ""; }

    /** The agent or an external action changed a file on disk. */
    public void onDiskChanged(File f) {
        for (int i = 0; i < docs.size(); i++) {
            Doc d = docs.get(i);
            if (d.file.equals(f)) {
                if (!f.exists()) { docs.remove(i); current = docs.isEmpty() ? -1 : Math.min(current, docs.size() - 1); if (current >= 0) { Doc n = docs.get(current); editor.load(n.text, n.file.getName(), n.history, n.cursor); } refreshTabs(); refreshVisibility(); return; }
                if (d.dirty) continue;
                try {
                    d.text = FileOps.readText(f, 2_000_000);
                    d.history = new CodeEditText.History();
                    if (i == current) editor.load(d.text, d.file.getName(), d.history, editor.getSelectionStart());
                } catch (Exception ignored) {}
            }
        }
    }

    public void onRenamed(File from, File to) {
        for (Doc d : docs) if (d.file.equals(from)) d.file = to;
        refreshTabs();
    }

    public void closeAll() { docs.clear(); current = -1; refreshTabs(); refreshVisibility(); }

    // ---------- menu ----------

    private void showMore(View anchor) {
        PopupMenu m = new PopupMenu(act, anchor);
        m.getMenu().add(0, 1, 0, "Go to line…");
        m.getMenu().add(0, 2, 1, "Toggle word wrap");
        m.getMenu().add(0, 3, 2, "Font size +");
        m.getMenu().add(0, 4, 3, "Font size −");
        m.getMenu().add(0, 5, 4, "Select all");
        m.getMenu().add(0, 6, 5, "Duplicate line");
        m.getMenu().add(0, 7, 6, "Delete line");
        m.getMenu().add(0, 8, 7, "Reload from disk");
        m.getMenu().add(0, 9, 8, "Save all");
        m.setOnMenuItemClickListener(it -> {
            if (current < 0 && it.getItemId() != 9) return true;
            switch (it.getItemId()) {
                case 1: Ui.prompt(act, "Go to line", "", "Line number", s -> { try { editor.goToLine(Integer.parseInt(s)); } catch (Exception ignored) {} }); break;
                case 2: {
                    boolean wrap = !Hz.prefs(act).getBoolean(Hz.K_WRAP, false);
                    Hz.prefs(act).edit().putBoolean(Hz.K_WRAP, wrap).apply();
                    editor.setHorizontallyScrolling(!wrap);
                    Ui.toast(act, wrap ? "Word wrap on" : "Word wrap off");
                    break;
                }
                case 3: setFont(fontSp + 1); break;
                case 4: setFont(fontSp - 1); break;
                case 5: editor.selectAll(); break;
                case 6: lineOp(false); break;
                case 7: lineOp(true); break;
                case 8: { File f = currentFile(); Doc d = docs.get(current); d.dirty = false; onDiskChanged(f); refreshTabs(); break; }
                case 9: saveAll(); Ui.toast(act, "All files saved"); break;
            }
            return true;
        });
        m.show();
    }

    private void setFont(float sp) {
        fontSp = Math.max(9, Math.min(26, sp));
        Hz.prefs(act).edit().putFloat(Hz.K_FONT_SIZE, fontSp).apply();
        editor.setCodeTextSize(fontSp);
    }

    private void lineOp(boolean delete) {
        Editable t = editor.getText();
        int pos = Math.max(0, editor.getSelectionStart());
        int s = pos, e = pos;
        while (s > 0 && t.charAt(s - 1) != '\n') s--;
        while (e < t.length() && t.charAt(e) != '\n') e++;
        if (delete) { t.delete(s, Math.min(t.length(), e + 1)); }
        else { t.insert(e, "\n" + t.subSequence(s, e)); }
    }

    // ---------- find / replace ----------

    private void toggleFind() {
        if (current < 0) return;
        findBar.setVisibility(findBar.getVisibility() == VISIBLE ? GONE : VISIBLE);
        if (findBar.getVisibility() == VISIBLE) {
            int a = editor.getSelectionStart(), b = editor.getSelectionEnd();
            if (a >= 0 && b > a && b - a < 80) findField.setText(editor.getText().subSequence(a, b));
            findField.requestFocus();
        } else clearMatch();
    }

    private Pattern pattern() {
        String q = findField.getText().toString();
        if (q.isEmpty()) return null;
        try {
            return Pattern.compile(useRegex ? q : Pattern.quote(q), matchCase ? 0 : Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        } catch (Exception e) { return null; }
    }

    private void updateCount() {
        if (current < 0) return;
        Pattern p = pattern();
        if (p == null) { findCount.setText(findField.getText().length() > 0 ? " invalid" : ""); return; }
        Matcher m = p.matcher(editor.getText());
        int n = 0;
        while (m.find() && n < 9999) { n++; if (m.end() == m.start()) break; }
        findCount.setText("  " + n + " found");
    }

    private void clearMatch() { editor.getText().removeSpan(matchSpan); }

    private void findStep(boolean forward) {
        Pattern p = pattern();
        if (p == null || current < 0) return;
        Editable t = editor.getText();
        Matcher m = p.matcher(t);
        int from = forward ? Math.max(editor.getSelectionEnd(), 0) : Math.max(editor.getSelectionStart(), 0);
        int ms = -1, me = -1;
        if (forward) {
            if (m.find(Math.min(from, t.length()))) { ms = m.start(); me = m.end(); }
            else if (m.find(0)) { ms = m.start(); me = m.end(); }
        } else {
            int ls = -1, le = -1;
            m.reset();
            while (m.find()) {
                if (m.start() >= from - 0 && ls >= 0) break;
                if (m.start() < from) { ls = m.start(); le = m.end(); }
                if (m.end() == m.start() && m.end() >= t.length()) break;
            }
            if (ls < 0) { m.reset(); while (m.find()) { ls = m.start(); le = m.end(); if (m.end() == m.start()) break; } }
            ms = ls; me = le;
        }
        if (ms < 0) { Ui.toast(act, "No matches"); return; }
        clearMatch();
        t.setSpan(matchSpan, ms, me, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        editor.setSelection(ms, me);
        editor.bringPointIntoView(ms);
    }

    private void replaceOne() {
        Pattern p = pattern();
        if (p == null || current < 0) return;
        int a = editor.getSelectionStart(), b = editor.getSelectionEnd();
        Editable t = editor.getText();
        if (a >= 0 && b > a) {
            Matcher m = p.matcher(t.subSequence(a, b));
            if (m.matches()) {
                String rep = useRegex ? m.replaceFirst(replaceField.getText().toString()) : replaceField.getText().toString();
                editor.replaceRange(a, b, rep);
            }
        }
        findStep(true);
        updateCount();
    }

    private void replaceAll() {
        Pattern p = pattern();
        if (p == null || current < 0) return;
        String src = editor.getText().toString();
        Matcher m = p.matcher(src);
        String rep = replaceField.getText().toString();
        String out;
        try { out = m.replaceAll(useRegex ? rep : Matcher.quoteReplacement(rep)); }
        catch (Exception e) { Ui.toast(act, "Bad replacement: " + e.getMessage()); return; }
        if (out.equals(src)) { Ui.toast(act, "Nothing to replace"); return; }
        clearMatch();
        editor.replaceRange(0, src.length(), out);
        updateCount();
        Ui.toast(act, "Replaced");
    }
}
