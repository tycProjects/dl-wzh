package com.termux.hz;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.termux.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import io.noties.markwon.Markwon;

/** Chat with an AI agent (Groq / OpenRouter) that can read and edit the project. */
public class AgentPanel extends LinearLayout {

    public interface Host {
        File project();
        void fileChanged(File f);
        String buildBlocking();
        Sh.Result runCommand(String cmd, Sh.Handle h);
        void openSettings();
    }

    private static final int MAX_STEPS = 30;

    private final Activity act;
    private final Host host;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private final Markwon markwon;
    private LinearLayout messages;
    private ScrollView scroll;
    private EditText input;
    private TextView sendBtn, modelChip, providerChip, notice;
    private AgentTools tools;
    private JSONArray history = new JSONArray();
    private volatile boolean running;
    private final LlmClient.Cancel cancel = new LlmClient.Cancel();
    private final Sh.Handle shHandle = new Sh.Handle();
    private boolean toolsEnabled = true;
    private final java.util.Map<LlmClient.Provider, List<LlmClient.Model>> modelCache = new java.util.HashMap<>();

    public AgentPanel(Activity a, Host h) {
        super(a);
        act = a; host = h;
        setOrientation(VERTICAL);
        setBackgroundColor(Ui.BG);
        markwon = Markwon.create(a);

        tools = new AgentTools(a, new AgentTools.Host() {
            @Override public File project() { return host.project(); }
            @Override public boolean approve(String t, String d, boolean cmd) { return askApproval(t, d, cmd); }
            @Override public void fileChanged(File f) { ui.post(() -> host.fileChanged(f)); }
            @Override public String buildBlocking() { return host.buildBlocking(); }
            @Override public Sh.Result runCommand(String c, Sh.Handle hd) { return host.runCommand(c, hd); }
        });

        // header
        LinearLayout bar = new LinearLayout(a);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setBackgroundColor(Ui.SURFACE);
        bar.setPadding(Ui.dp(a, 10), Ui.dp(a, 6), Ui.dp(a, 2), Ui.dp(a, 6));
        providerChip = Ui.chip(a, "", this::pickProvider);
        modelChip = Ui.chip(a, "", this::pickModel);
        modelChip.setSingleLine(true);
        modelChip.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE);
        bar.addView(providerChip);
        LayoutParams mp = new LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        mp.leftMargin = Ui.dp(a, 8);
        bar.addView(modelChip, mp);
        bar.addView(Ui.icon(a, R.drawable.hz_ic_more, "Agent menu", () -> menu(bar)));
        addView(bar);

        notice = Ui.text(a, "", 12.5f, Ui.WARN);
        notice.setPadding(Ui.dp(a, 14), Ui.dp(a, 8), Ui.dp(a, 14), Ui.dp(a, 8));
        notice.setBackgroundColor(0xFF2A2412);
        notice.setVisibility(GONE);
        notice.setOnClickListener(v -> host.openSettings());
        addView(notice);

        messages = new LinearLayout(a);
        messages.setOrientation(VERTICAL);
        messages.setPadding(Ui.dp(a, 10), Ui.dp(a, 10), Ui.dp(a, 10), Ui.dp(a, 10));
        scroll = new ScrollView(a);
        scroll.addView(messages);
        addView(scroll, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        // quick actions
        HorizontalScrollView hs = new HorizontalScrollView(a);
        hs.setHorizontalScrollBarEnabled(false);
        LinearLayout chips = new LinearLayout(a);
        chips.setPadding(Ui.dp(a, 8), Ui.dp(a, 4), Ui.dp(a, 8), Ui.dp(a, 4));
        String[][] qa = {
                {"Explain project", "Explore this project and explain what it does and how it is structured."},
                {"Build & fix errors", "Build the project. If it fails, read the errors, fix the code, and build again until it succeeds."},
                {"Add a new item", "Add a new custom item to this mod, including registration, model JSON, texture placeholder reference and lang entry."},
                {"Add a block", "Add a new custom block with its item, blockstate, model, loot table and lang entry."},
                {"Port to newer MC", "Check which Minecraft version this mod targets and list what is needed to port it to the latest stable version. Then apply the changes."},
                {"Add config", "Add a simple config file system for this mod with a few sensible options."},
                {"Review code", "Review the code for bugs, crashes, and bad practices. Fix the clear problems."}};
        for (String[] q : qa) {
            TextView c = Ui.chip(a, q[0], () -> send(q[1]));
            LayoutParams p = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            p.rightMargin = Ui.dp(a, 8);
            chips.addView(c, p);
        }
        hs.addView(chips);
        addView(hs);

        // input
        LinearLayout in = new LinearLayout(a);
        in.setGravity(Gravity.BOTTOM);
        in.setBackgroundColor(Ui.SURFACE);
        in.setPadding(Ui.dp(a, 8), Ui.dp(a, 8), Ui.dp(a, 8), Ui.dp(a, 8));
        input = Ui.input(a, "Ask the agent in any language…");
        input.setMaxLines(5);
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE | android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        in.addView(input, new LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        sendBtn = Ui.button(a, "Send", Ui.ACCENT2, 0xFFFFFFFF, this::onSend);
        LayoutParams sp = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sp.leftMargin = Ui.dp(a, 8);
        in.addView(sendBtn, sp);
        addView(in);

        refreshHeader();
        addAssistant("Hi! I'm the HZ agent. I can read your project, edit files, run the build and fix errors. Tell me what you want to make — in any language.");
    }

    // ---------- header / settings ----------

    private LlmClient.Provider provider() {
        return "OPENROUTER".equals(Hz.prefs(act).getString(Hz.K_PROVIDER, "GROQ")) ? LlmClient.Provider.OPENROUTER : LlmClient.Provider.GROQ;
    }

    private String model(LlmClient.Provider p) { return Hz.prefs(act).getString(p.modelPref, p.defaultModel); }

    public void refreshHeader() {
        LlmClient.Provider p = provider();
        providerChip.setText(p.label + " ▾");
        modelChip.setText(model(p) + " ▾");
        String key = Hz.prefs(act).getString(p.keyPref, "");
        if (key.isEmpty()) {
            notice.setText("Add your " + p.label + " API key in Settings to use the agent. Tap here.");
            notice.setVisibility(VISIBLE);
        } else notice.setVisibility(GONE);
    }

    private void pickProvider() {
        String[] n = {"Groq", "OpenRouter"};
        new MaterialAlertDialogBuilder(act).setTitle("Provider").setItems(n, (d, w) -> {
            Hz.prefs(act).edit().putString(Hz.K_PROVIDER, w == 0 ? "GROQ" : "OPENROUTER").apply();
            toolsEnabled = true;
            refreshHeader();
        }).show();
    }

    private void pickModel() {
        LlmClient.Provider p = provider();
        List<LlmClient.Model> cached = modelCache.get(p);
        if (cached != null) { showModelDialog(p, cached); return; }
        Ui.toast(act, "Loading models…");
        new Thread(() -> {
            try {
                List<LlmClient.Model> ms = LlmClient.listModels(p, Hz.prefs(act).getString(p.keyPref, ""));
                modelCache.put(p, ms);
                ui.post(() -> showModelDialog(p, ms));
            } catch (Exception e) {
                ui.post(() -> {
                    Ui.toast(act, "Could not load models: " + e.getMessage());
                    Ui.prompt(act, "Model id", model(p), "e.g. " + p.defaultModel, id -> {
                        if (!id.isEmpty()) { Hz.prefs(act).edit().putString(p.modelPref, id).apply(); refreshHeader(); }
                    });
                });
            }
        }).start();
    }

    private void showModelDialog(LlmClient.Provider p, List<LlmClient.Model> all) {
        LinearLayout box = new LinearLayout(act);
        box.setOrientation(VERTICAL);
        box.setPadding(Ui.dp(act, 16), Ui.dp(act, 8), Ui.dp(act, 16), 0);
        EditText q = Ui.input(act, "Search " + all.size() + " models");
        q.setSingleLine(true);
        box.addView(q);
        ListView lv = new ListView(act);
        final List<LlmClient.Model> shown = new ArrayList<>(all);
        ArrayAdapter<LlmClient.Model> ad = new ArrayAdapter<LlmClient.Model>(act, android.R.layout.simple_list_item_1, shown) {
            @Override public View getView(int pos, View cv, ViewGroup parent) {
                TextView t = (TextView) super.getView(pos, cv, parent);
                t.setTextColor(shown.get(pos).tools ? Ui.TEXT : Ui.MUTED);
                t.setTextSize(13);
                return t;
            }
        };
        lv.setAdapter(ad);
        box.addView(lv, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(act, 380)));
        androidx.appcompat.app.AlertDialog dlg = new MaterialAlertDialogBuilder(act).setTitle(p.label + " models").setView(box)
                .setNegativeButton("Close", null).setNeutralButton("Enter id…", (d, w) ->
                        Ui.prompt(act, "Model id", model(p), "model id", id -> {
                            if (!id.isEmpty()) { Hz.prefs(act).edit().putString(p.modelPref, id).apply(); refreshHeader(); }
                        })).create();
        lv.setOnItemClickListener((pa, v, pos, id) -> {
            LlmClient.Model m = shown.get(pos);
            Hz.prefs(act).edit().putString(p.modelPref, m.id).apply();
            toolsEnabled = true;
            refreshHeader();
            if (!m.tools) Ui.toast(act, "This model may not support file tools");
            dlg.dismiss();
        });
        q.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            public void onTextChanged(CharSequence s, int a, int b, int c) {}
            public void afterTextChanged(android.text.Editable s) {
                String f = s.toString().toLowerCase().trim();
                shown.clear();
                for (LlmClient.Model m : all) if (f.isEmpty() || m.id.toLowerCase().contains(f)) shown.add(m);
                ad.notifyDataSetChanged();
            }
        });
        dlg.show();
    }

    private void menu(View anchor) {
        PopupMenu m = new PopupMenu(act, anchor);
        m.getMenu().add(0, 1, 0, "New chat");
        m.getMenu().add(0, 2, 1, "Revert agent changes (" + tools.changeCount() + ")");
        m.getMenu().add(0, 3, 2, "Refresh model list");
        m.getMenu().add(0, 4, 3, "Agent settings");
        m.setOnMenuItemClickListener(it -> {
            switch (it.getItemId()) {
                case 1: newChat(); break;
                case 2: Ui.confirm(act, "Revert changes", "Restore every file the agent changed in this session?", "Revert", () -> addAssistant(tools.revertAll())); break;
                case 3: modelCache.clear(); pickModel(); break;
                case 4: host.openSettings(); break;
            }
            return true;
        });
        m.show();
    }

    public void newChat() {
        if (running) stop();
        history = new JSONArray();
        messages.removeAllViews();
        toolsEnabled = true;
        addAssistant("New chat started. What should we build?");
    }

    // ---------- chat UI ----------

    private TextView bubble(boolean user) {
        TextView t = new TextView(act);
        t.setTextSize(14.5f);
        t.setTextColor(user ? 0xFFFFFFFF : Ui.TEXT);
        t.setTextIsSelectable(true);
        t.setLinkTextColor(Ui.ACCENT);
        int pad = Ui.dp(act, 12);
        t.setPadding(pad, Ui.dp(act, 9), pad, Ui.dp(act, 9));
        t.setBackground(Ui.round(user ? Ui.ACCENT2 : Ui.SURFACE2, 14, act));
        LayoutParams p = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.gravity = user ? Gravity.END : Gravity.START;
        p.topMargin = Ui.dp(act, 6);
        p.leftMargin = user ? Ui.dp(act, 48) : 0;
        p.rightMargin = user ? 0 : Ui.dp(act, 24);
        t.setLayoutParams(p);
        messages.addView(t);
        return t;
    }

    private void addUser(String s) { bubble(true).setText(s); scrollDown(); }

    private TextView addAssistant(String md) {
        TextView t = bubble(false);
        markwon.setMarkdown(t, md);
        scrollDown();
        return t;
    }

    private TextView toolCard(String text) {
        TextView t = Ui.text(act, "⚙  " + text, 12, Ui.MUTED);
        t.setTypeface(Typeface.MONOSPACE);
        t.setPadding(Ui.dp(act, 10), Ui.dp(act, 5), Ui.dp(act, 10), Ui.dp(act, 5));
        t.setBackground(Ui.round(0xFF12151B, 8, act));
        LayoutParams p = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.topMargin = Ui.dp(act, 4);
        messages.addView(t, p);
        scrollDown();
        return t;
    }

    private void scrollDown() { scroll.post(() -> scroll.fullScroll(FOCUS_DOWN)); }

    private void setRunning(boolean r) {
        running = r;
        sendBtn.setText(r ? "Stop" : "Send");
        sendBtn.setBackground(Ui.ripple(Ui.round(r ? Ui.DANGER : Ui.ACCENT2, 10, act), act));
    }

    private void onSend() {
        if (running) { stop(); return; }
        String s = input.getText().toString().trim();
        if (s.isEmpty()) return;
        input.setText("");
        send(s);
    }

    private void stop() { cancel.cancelled = true; cancel.cancel(); shHandle.cancel(); }

    // ---------- agent loop ----------

    /** Public entry for "Fix build errors" and other quick actions. */
    public void send(String text) {
        if (running) { Ui.toast(act, "The agent is busy"); return; }
        LlmClient.Provider p = provider();
        String key = Hz.prefs(act).getString(p.keyPref, "");
        if (key.isEmpty()) { refreshHeader(); host.openSettings(); return; }
        if (host.project() == null) { addAssistant("Open or import a project first (Project tab)."); return; }
        addUser(text);
        history.put(msg("user", text));
        cancel.cancelled = false;
        shHandle.cancelled = false;
        setRunning(true);
        new Thread(() -> loop(p, key), "hz-agent").start();
    }

    private static JSONObject msg(String role, String content) {
        try { return new JSONObject().put("role", role).put("content", content); } catch (Exception e) { return new JSONObject(); }
    }

    private String systemPrompt() {
        File proj = host.project();
        ProjectInfo info = ProjectInfo.detect(proj);
        return "You are HZ Agent, an expert Minecraft Java modding assistant running inside an Android app (Termux environment, bash). "
                + "You work on Fabric, Forge, NeoForge, Quilt and Paper/Bukkit projects, with Gradle or Maven, for Java 8 to 21.\n"
                + "Project: " + proj.getName() + " | " + info.summary() + " | build system: "
                + (info.hasGradleWrapper ? "Gradle wrapper" : info.hasGradle ? "Gradle" : info.hasMaven ? "Maven" : "unknown") + ".\n"
                + "Rules:\n"
                + "- Always reply in the same language the user writes in.\n"
                + "- Use your tools to inspect the real files before changing anything; never guess file contents.\n"
                + "- Prefer edit_file for small changes; write_file for new files. Keep code compatible with the project's Minecraft version, mappings and Java level.\n"
                + "- After code changes, call build_project and fix any compile errors, repeating until the build passes or you are blocked.\n"
                + "- Be concise. Explain what you changed in a short summary at the end.\n"
                + "- Paths are relative to the project root. Do not touch files outside it.\n\n"
                + "Project tree:\n" + FileOps.tree(proj, 2, 120);
    }

    private JSONArray buildMessages() throws Exception {
        JSONArray out = new JSONArray();
        out.put(msg("system", systemPrompt()));
        // compact very long histories: shorten old tool outputs first
        int total = 0;
        for (int i = 0; i < history.length(); i++) total += history.getJSONObject(i).optString("content", "").length();
        if (total > 90_000) {
            for (int i = 0; i < history.length() - 6; i++) {
                JSONObject m = history.getJSONObject(i);
                if ("tool".equals(m.optString("role")) && m.optString("content").length() > 400)
                    m.put("content", m.getString("content").substring(0, 400) + "\n…[older output trimmed]");
            }
        }
        for (int i = 0; i < history.length(); i++) out.put(history.get(i));
        return out;
    }

    private void loop(LlmClient.Provider p, String key) {
        try {
            for (int step = 0; step < MAX_STEPS && !cancel.cancelled; step++) {
                final TextView[] bub = new TextView[1];
                final StringBuilder live = new StringBuilder();
                LlmClient.Reply r = null;
                for (int attempt = 0; attempt < 3; attempt++) {
                    r = LlmClient.chat(p, key, model(p), buildMessages(), toolsEnabled ? AgentTools.schema() : null, delta -> {
                        live.append(delta);
                        ui.post(() -> {
                            if (bub[0] == null) bub[0] = bubble(false);
                            bub[0].setText(live.toString());
                            scrollDown();
                        });
                    }, cancel);
                    if (r.cancelled) break;
                    boolean retriable = r.error != null && (r.httpCode == 429 || r.httpCode >= 500);
                    if (!retriable) break;
                    final String note = "Rate limited / server busy, retrying…";
                    ui.post(() -> Ui.toast(act, note));
                    try { Thread.sleep(attempt == 0 ? 4000 : 10000); } catch (InterruptedException ignored) {}
                }
                if (r.cancelled) { ui.post(() -> addAssistant("_Stopped._")); break; }
                if (r.error != null) {
                    String low = r.error.toLowerCase();
                    if (toolsEnabled && (low.contains("tool") || low.contains("function")) && r.httpCode >= 400 && r.httpCode < 500) {
                        toolsEnabled = false;
                        ui.post(() -> addAssistant("This model does not support tool calling, so I can chat but cannot edit files. Pick another model (tap the model name) for full agent mode. Continuing in chat-only mode."));
                        step--;
                        continue;
                    }
                    final String err = r.error;
                    ui.post(() -> addAssistant("**Error:** " + err));
                    break;
                }
                final String content = r.content;
                if (bub[0] != null) ui.post(() -> markwon.setMarkdown(bub[0], content));
                JSONObject am = new JSONObject().put("role", "assistant");
                am.put("content", content.isEmpty() ? JSONObject.NULL : content);
                if (r.toolCalls.length() > 0) am.put("tool_calls", r.toolCalls);
                history.put(am);
                if (r.toolCalls.length() == 0) {
                    if (content.trim().isEmpty()) ui.post(() -> addAssistant("_(empty reply)_"));
                    break;
                }
                for (int i = 0; i < r.toolCalls.length() && !cancel.cancelled; i++) {
                    JSONObject tc = r.toolCalls.getJSONObject(i);
                    JSONObject fn = tc.getJSONObject("function");
                    String name = fn.getString("name"), args = fn.getString("arguments");
                    String label;
                    try { label = AgentTools.describe(name, new JSONObject(args)); } catch (Exception e) { label = name; }
                    final String fl = label;
                    final TextView[] card = new TextView[1];
                    final CountDownLatch made = new CountDownLatch(1);
                    ui.post(() -> { card[0] = toolCard(fl + " …"); made.countDown(); });
                    made.await();
                    String result = tools.execute(name, args, shHandle);
                    boolean ok = !result.startsWith("ERROR") && !result.startsWith("DENIED");
                    ui.post(() -> { card[0].setText("⚙  " + fl + (ok ? "  ✓" : "  ✗")); card[0].setTextColor(ok ? Ui.ACCENT : Ui.DANGER); });
                    history.put(new JSONObject().put("role", "tool").put("tool_call_id", tc.getString("id")).put("content", result));
                }
                if (step == MAX_STEPS - 1) ui.post(() -> addAssistant("_Reached the step limit. Say \"continue\" to keep going._"));
            }
        } catch (Exception e) {
            ui.post(() -> addAssistant("**Agent error:** " + e));
        } finally {
            ui.post(() -> setRunning(false));
        }
    }

    // ---------- approvals ----------

    private boolean askApproval(String title, String detail, boolean isCommand) {
        int mode = Hz.prefs(act).getInt(Hz.K_APPROVAL, 1);
        if (mode == 2) return true;
        if (mode == 1 && !isCommand) return true;
        final boolean[] ans = {false};
        final CountDownLatch latch = new CountDownLatch(1);
        ui.post(() -> new MaterialAlertDialogBuilder(act).setTitle(title).setMessage(detail.isEmpty() ? "Allow the agent to do this?" : detail)
                .setCancelable(false)
                .setPositiveButton("Allow", (d, w) -> { ans[0] = true; latch.countDown(); })
                .setNegativeButton("Deny", (d, w) -> latch.countDown()).show());
        try { latch.await(); } catch (InterruptedException e) { return false; }
        return ans[0];
    }
}
