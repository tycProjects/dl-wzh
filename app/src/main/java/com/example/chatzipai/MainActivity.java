package com.example.chatzipai;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class MainActivity extends Activity {
    private static final int PICK_ZIP = 10, SAVE_ZIP = 11;
    private static final int BLUE = 0xFF4D6BFE;
    private static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT;
    private static final int WRAP = ViewGroup.LayoutParams.WRAP_CONTENT;
    private static final int DIALOG_THEME = android.R.style.Theme_Material_Light_Dialog_Alert;

    private Config cfg;
    private final ExecutorService io = Executors.newCachedThreadPool();
    private final Handler ui = new Handler(Looper.getMainLooper());
    private final Map<String, List<String>> modelCache = new HashMap<>();

    // ---- chat ----
    private View chatRoot, zipRoot;
    private ScrollView scroll;
    private LinearLayout list, emptyView;
    private EditText input;
    private TextView sendBtn, modelChip;
    private JSONArray history = new JSONArray();
    private boolean busy = false;
    private int generation = 0;
    private ApiClient active;
    private AiRow curRow;
    private TextView lastRegen;
    private final StringBuilder curBuf = new StringBuilder();
    private final StringBuilder curReason = new StringBuilder();
    private boolean renderPending = false, gotContent = false;

    private static final class AiRow {
        LinearLayout box, actions;
        TextView reasonHead, reason, text, error, regen;
        String raw = "";
    }

    // ---- zip ----
    private final LinkedHashMap<String, byte[]> archive = new LinkedHashMap<>();
    private final LinkedHashMap<String, byte[]> changes = new LinkedHashMap<>();
    private EditText zipInstruction, zipTpm;
    private TextView zipProgress;
    private Button zipPick, zipEdit, zipExport;
    private boolean zipReady = false, zipBusy = false;

    // =============================================================== lifecycle

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        cfg = new Config(getSharedPreferences("settings", MODE_PRIVATE));
        getWindow().setStatusBarColor(0xFFFFFFFF);
        getWindow().setNavigationBarColor(0xFFFFFFFF);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        chatRoot = buildChat();
        setContentView(chatRoot);
        restoreChat();
        refreshChip();
        refreshSend();
    }

    @Override
    public void onBackPressed() {
        if (zipRoot != null && zipRoot.getParent() != null) {
            setContentView(chatRoot);
        } else {
            super.onBackPressed();
        }
    }

    // =============================================================== helpers

    private int dp(float v) { return (int) (v * getResources().getDisplayMetrics().density + 0.5f); }

    private GradientDrawable round(int color, float radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(radiusDp));
        return g;
    }

    private GradientDrawable oval(int color) {
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(color);
        return g;
    }

    private TextView tv(String s, float sp, int color) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        return t;
    }

    private TextView iconBtn(String s) {
        TextView t = tv(s, 22, 0xFF374151);
        t.setGravity(Gravity.CENTER);
        t.setLayoutParams(new LinearLayout.LayoutParams(dp(40), dp(40)));
        return t;
    }

    private EditText field(String hint, String value, boolean secret) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setText(value);
        e.setSingleLine(true);
        e.setTextSize(15);
        if (secret) e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        else e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        return e;
    }

    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_LONG).show(); }

    private JSONObject msg(String role, String content) {
        try {
            return new JSONObject().put("role", role).put("content", content);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // =============================================================== chat UI

    private View buildChat() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFFFFFFFF);

        // thanh trên
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(16), dp(6), dp(8), dp(6));
        TextView title = tv("Chat AI", 18, 0xFF111827);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        bar.addView(title, new LinearLayout.LayoutParams(0, WRAP, 1f));

        modelChip = tv("", 12.5f, BLUE);
        modelChip.setSingleLine(true);
        modelChip.setEllipsize(TextUtils.TruncateAt.END);
        modelChip.setMaxWidth(dp(170));
        modelChip.setPadding(dp(12), dp(6), dp(12), dp(6));
        modelChip.setBackground(round(0xFFEAF0FF, 16));
        modelChip.setOnClickListener(v -> pickModelQuick());
        bar.addView(modelChip);

        TextView newBtn = iconBtn("＋");
        newBtn.setOnClickListener(v -> newChat());
        bar.addView(newBtn);
        TextView menuBtn = iconBtn("⋮");
        menuBtn.setOnClickListener(v -> showMenu(v));
        bar.addView(menuBtn);
        root.addView(bar, new LinearLayout.LayoutParams(MATCH, WRAP));

        View line = new View(this);
        line.setBackgroundColor(0xFFE5E7EB);
        root.addView(line, new LinearLayout.LayoutParams(MATCH, 1));

        // vùng tin nhắn
        scroll = new ScrollView(this);
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(dp(16), dp(12), dp(16), dp(12));
        scroll.addView(list, new ViewGroup.LayoutParams(MATCH, WRAP));
        root.addView(scroll, new LinearLayout.LayoutParams(MATCH, 0, 1f));

        emptyView = new LinearLayout(this);
        emptyView.setOrientation(LinearLayout.VERTICAL);
        emptyView.setGravity(Gravity.CENTER_HORIZONTAL);
        emptyView.setPadding(0, dp(90), 0, 0);
        TextView hi = tv("Xin chào 👋", 26, 0xFF111827);
        hi.setTypeface(Typeface.DEFAULT_BOLD);
        TextView hi2 = tv("Tôi có thể giúp gì cho bạn hôm nay?", 15, 0xFF6B7280);
        hi2.setPadding(0, dp(8), 0, 0);
        emptyView.addView(hi);
        emptyView.addView(hi2);
        list.addView(emptyView);

        // ô nhập
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.HORIZONTAL);
        box.setGravity(Gravity.BOTTOM);
        box.setBackground(round(0xFFF3F4F6, 26));
        box.setPadding(dp(18), dp(6), dp(8), dp(6));

        input = new EditText(this);
        input.setBackground(null);
        input.setHint("Nhắn cho AI");
        input.setTextSize(16);
        input.setMaxLines(6);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        input.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            public void onTextChanged(CharSequence s, int a, int b, int c) { }
            public void afterTextChanged(Editable e) { refreshSend(); }
        });
        input.setOnClickListener(v -> scrollToBottom(true));
        box.addView(input, new LinearLayout.LayoutParams(0, WRAP, 1f));

        sendBtn = tv("↑", 20, 0xFFFFFFFF);
        sendBtn.setGravity(Gravity.CENTER);
        sendBtn.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(dp(40), dp(40));
        sp.gravity = Gravity.BOTTOM;
        box.addView(sendBtn, sp);
        sendBtn.setOnClickListener(v -> {
            if (busy) { stopStream(); return; }
            String t = input.getText().toString().trim();
            if (t.isEmpty()) return;
            input.setText("");
            send(t);
        });

        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(MATCH, WRAP);
        bp.setMargins(dp(12), dp(6), dp(12), dp(10));
        root.addView(box, bp);
        return root;
    }

    private void refreshChip() {
        String m = cfg.model();
        modelChip.setText((m.isEmpty() ? "Chọn model" : m) + " ▾");
    }

    private void refreshSend() {
        if (sendBtn == null || input == null) return;
        boolean has = input.getText().toString().trim().length() > 0;
        if (busy) {
            sendBtn.setText("■");
            sendBtn.setBackground(oval(BLUE));
        } else {
            sendBtn.setText("↑");
            sendBtn.setBackground(oval(has ? BLUE : 0xFFBFC7E8));
        }
    }

    private void showMenu(View anchor) {
        PopupMenu pm = new PopupMenu(this, anchor);
        pm.getMenu().add(0, 1, 0, "Cài đặt API & model");
        pm.getMenu().add(0, 2, 1, "Sửa ZIP — 4 Worker + AI Trung tâm");
        pm.getMenu().add(0, 4, 2, "Đội AI (nhóm chat)");
        pm.getMenu().add(0, 3, 3, "Xoá cuộc trò chuyện");
        pm.setOnMenuItemClickListener(it -> {
            switch (it.getItemId()) {
                case 1: openSettings(); break;
                case 2: startActivity(new Intent(this, TeamActivity.class)); break;
                case 4: startActivity(new Intent(this, TeamActivity.class)); break;
                case 3: newChat(); break;
                default: break;
            }
            return true;
        });
        pm.show();
    }

    private void scrollToBottom(boolean force) {
        if (scroll == null) return;
        View c = scroll.getChildAt(0);
        if (c == null) return;
        int gap = c.getBottom() - (scroll.getHeight() + scroll.getScrollY());
        if (force || gap < dp(120)) {
            scroll.post(() -> scroll.scrollTo(0, list.getBottom()));
        }
    }

    private void hideEmpty() {
        if (emptyView.getParent() == list) list.removeView(emptyView);
    }

    private void addUser(String text) {
        TextView t = tv(text, 16, 0xFF111827);
        t.setPadding(dp(14), dp(10), dp(14), dp(10));
        t.setBackground(round(0xFFEFF1F5, 18));
        t.setMaxWidth((int) (getResources().getDisplayMetrics().widthPixels * 0.82f));
        t.setTextIsSelectable(true);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(WRAP, WRAP);
        p.gravity = Gravity.END;
        p.bottomMargin = dp(18);
        list.addView(t, p);
    }

    private AiRow addAi() {
        final AiRow r = new AiRow();
        r.box = new LinearLayout(this);
        r.box.setOrientation(LinearLayout.VERTICAL);

        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        TextView av = tv("AI", 11, 0xFFFFFFFF);
        av.setTypeface(Typeface.DEFAULT_BOLD);
        av.setGravity(Gravity.CENTER);
        av.setBackground(oval(BLUE));
        head.addView(av, new LinearLayout.LayoutParams(dp(26), dp(26)));
        TextView nm = tv("Trợ lý", 13, 0xFF6B7280);
        nm.setPadding(dp(8), 0, 0, 0);
        head.addView(nm);
        r.box.addView(head);

        r.reasonHead = tv("", 13, 0xFF6B7280);
        r.reasonHead.setPadding(0, dp(8), 0, 0);
        r.reasonHead.setVisibility(View.GONE);
        r.box.addView(r.reasonHead);
        r.reason = tv("", 13, 0xFF6B7280);
        r.reason.setPadding(dp(10), dp(4), 0, 0);
        r.reason.setVisibility(View.GONE);
        r.box.addView(r.reason);
        r.reasonHead.setOnClickListener(v -> {
            boolean show = r.reason.getVisibility() != View.VISIBLE;
            r.reason.setVisibility(show ? View.VISIBLE : View.GONE);
            setReasonLabel(r, show, !busy || curRow != r);
        });

        r.text = tv("", 16, 0xFF111827);
        r.text.setLineSpacing(0, 1.2f);
        r.text.setPadding(0, dp(8), 0, 0);
        r.box.addView(r.text);

        r.error = tv("", 14, 0xFFDC2626);
        r.error.setPadding(0, dp(8), 0, 0);
        r.error.setVisibility(View.GONE);
        r.box.addView(r.error);

        r.actions = new LinearLayout(this);
        r.actions.setOrientation(LinearLayout.HORIZONTAL);
        r.actions.setVisibility(View.GONE);
        TextView copy = tv("⧉ Sao chép", 13, 0xFF6B7280);
        copy.setPadding(0, dp(10), dp(18), dp(4));
        copy.setOnClickListener(v -> {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("chat", r.raw));
            Toast.makeText(this, "Đã sao chép", Toast.LENGTH_SHORT).show();
        });
        r.actions.addView(copy);
        r.regen = tv("↻ Tạo lại", 13, 0xFF6B7280);
        r.regen.setPadding(0, dp(10), dp(18), dp(4));
        r.regen.setOnClickListener(v -> regenerate());
        r.actions.addView(r.regen);
        r.box.addView(r.actions);

        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(MATCH, WRAP);
        p.bottomMargin = dp(20);
        list.addView(r.box, p);
        return r;
    }

    private void setReasonLabel(AiRow r, boolean expanded, boolean done) {
        r.reasonHead.setText("💭 " + (done ? "Đã suy nghĩ" : "Đang suy nghĩ…") + (expanded ? " ▾" : " ▸"));
    }

    // =============================================================== chat logic

    private void send(String text) {
        boolean custom = cfg.provider().equals(Config.CUSTOM);
        if ((cfg.apiKey().isEmpty() && !custom) || cfg.baseUrl().trim().isEmpty()) {
            toast("Hãy nhập API key / Base URL trước");
            openSettings();
            input.setText(text);
            return;
        }
        if (cfg.model().trim().isEmpty()) {
            toast("Hãy chọn model trước");
            pickModelQuick();
            input.setText(text);
            return;
        }
        hideEmpty();
        addUser(text);
        history.put(msg("user", text));
        startStream();
    }

    private void startStream() {
        busy = true;
        refreshSend();
        if (lastRegen != null) lastRegen.setVisibility(View.GONE);
        final AiRow row = addAi();
        curRow = row;
        curBuf.setLength(0);
        curReason.setLength(0);
        gotContent = false;
        row.text.setText("…");
        scrollToBottom(true);

        JSONArray msgs = new JSONArray();
        String sys = cfg.system();
        if (!sys.isEmpty()) msgs.put(msg("system", sys));
        for (int i = 0; i < history.length(); i++) msgs.put(history.optJSONObject(i));

        final int gen = ++generation;
        final ApiClient client = new ApiClient();
        active = client;
        final String base = cfg.baseUrl(), key = cfg.apiKey(), model = cfg.model();
        io.execute(() -> client.stream(base, key, model, msgs, new ApiClient.Listener() {
            @Override public void onToken(String t) {
                ui.post(() -> {
                    if (gen != generation) return;
                    if (!gotContent) {
                        gotContent = true;
                        if (curReason.length() > 0) {
                            row.reason.setVisibility(View.GONE);
                            setReasonLabel(row, false, true);
                        }
                    }
                    curBuf.append(t);
                    scheduleRender();
                });
            }
            @Override public void onReasoning(String t) {
                ui.post(() -> {
                    if (gen != generation) return;
                    if (curReason.length() == 0) {
                        row.reasonHead.setVisibility(View.VISIBLE);
                        row.reason.setVisibility(View.VISIBLE);
                        setReasonLabel(row, true, false);
                    }
                    curReason.append(t);
                    scheduleRender();
                });
            }
            @Override public void onDone() { ui.post(() -> endStream(gen, row, null)); }
            @Override public void onError(String m) { ui.post(() -> endStream(gen, row, m)); }
        }));
    }

    private void scheduleRender() {
        if (renderPending) return;
        renderPending = true;
        ui.postDelayed(() -> {
            renderPending = false;
            renderNow();
        }, 50);
    }

    private void renderNow() {
        AiRow r = curRow;
        if (r == null) return;
        boolean wasBottom = true;
        if (curReason.length() > 0) r.reason.setText(curReason.toString());
        if (curBuf.length() > 0) r.text.setText(Md.render(curBuf.toString()));
        if (wasBottom) scrollToBottom(false);
    }

    private void endStream(int gen, AiRow row, String error) {
        if (gen != generation) return;
        generation++;                       // bỏ qua mọi sự kiện đến muộn
        busy = false;
        renderPending = false;
        String full = curBuf.toString();
        row.raw = full;
        if (curReason.length() > 0) {
            row.reason.setText(curReason.toString());
            setReasonLabel(row, row.reason.getVisibility() == View.VISIBLE, true);
        }
        if (!full.isEmpty()) {
            history.put(msg("assistant", full));
            row.text.setText(Md.render(full));
            row.text.setTextIsSelectable(true);
        } else {
            row.text.setVisibility(View.GONE);
        }
        if (error != null) {
            row.error.setText("⚠ " + error);
            row.error.setVisibility(View.VISIBLE);
        } else if (full.isEmpty()) {
            row.text.setVisibility(View.VISIBLE);
            row.text.setText("(Không có phản hồi)");
        }
        row.actions.setVisibility(View.VISIBLE);
        lastRegen = row.regen;
        saveChat();
        refreshSend();
        scrollToBottom(false);
    }

    private void stopStream() {
        if (!busy) return;
        if (active != null) active.cancel();
        endStream(generation, curRow, null);
    }

    private void regenerate() {
        if (busy) return;
        int n = history.length();
        if (n > 0 && "assistant".equals(history.optJSONObject(n - 1).optString("role"))) {
            history.remove(n - 1);
        }
        int c = list.getChildCount();
        if (c > 0) list.removeViewAt(c - 1);
        startStream();
    }

    private void newChat() {
        if (busy) {
            if (active != null) active.cancel();
            generation++;
            busy = false;
        }
        history = new JSONArray();
        list.removeAllViews();
        list.addView(emptyView);
        lastRegen = null;
        saveChat();
        refreshSend();
    }

    private void saveChat() { cfg.setChat(history.toString()); }

    private void restoreChat() {
        try {
            history = new JSONArray(cfg.chat());
        } catch (Exception e) {
            history = new JSONArray();
        }
        if (history.length() == 0) return;
        hideEmpty();
        for (int i = 0; i < history.length(); i++) {
            JSONObject m = history.optJSONObject(i);
            if (m == null) continue;
            String role = m.optString("role"), content = m.optString("content");
            if ("user".equals(role)) {
                addUser(content);
            } else if ("assistant".equals(role)) {
                AiRow r = addAi();
                r.raw = content;
                r.text.setText(Md.render(content));
                r.text.setTextIsSelectable(true);
                r.actions.setVisibility(View.VISIBLE);
                r.regen.setVisibility(View.GONE);
                lastRegen = r.regen;
            }
        }
        if (lastRegen != null) lastRegen.setVisibility(View.VISIBLE);
        scrollToBottom(true);
    }

    // =============================================================== settings & model

    private void openSettings() {
        final HashMap<String, String[]> drafts = new HashMap<>();
        final String[] curProv = { cfg.provider() };

        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(20), dp(8), dp(20), dp(4));

        form.addView(tv("Nhà cung cấp", 13, 0xFF6B7280));
        final Spinner sp = new Spinner(this);
        sp.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, Config.NAMES));
        sp.setSelection(Config.indexOf(curProv[0]), false);
        form.addView(sp);

        form.addView(tv("Base URL (kiểu OpenAI, kết thúc bằng /v1)", 13, 0xFF6B7280));
        final EditText url = field("https://api.example.com/v1", cfg.urlOf(curProv[0]), false);
        form.addView(url);

        form.addView(tv("API key", 13, 0xFF6B7280));
        final EditText key = field("sk-...", cfg.keyOf(curProv[0]), true);
        form.addView(key);

        form.addView(tv("Model", 13, 0xFF6B7280));
        final EditText model = field("vd: deepseek-chat", cfg.modelOf(curProv[0]), false);
        form.addView(model);

        Button fetch = new Button(this);
        fetch.setText("Tải danh sách model từ API");
        fetch.setAllCaps(false);
        form.addView(fetch);

        form.addView(tv("Lời nhắc hệ thống (không bắt buộc)", 13, 0xFF6B7280));
        final EditText sys = new EditText(this);
        sys.setHint("vd: Trả lời ngắn gọn bằng tiếng Việt");
        sys.setText(cfg.system());
        sys.setTextSize(15);
        sys.setMinLines(2);
        sys.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        form.addView(sys);

        sp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> a, View v, int pos, long id) {
                String np = Config.NAMES[pos];
                if (np.equals(curProv[0])) return;
                drafts.put(curProv[0], new String[] {
                        url.getText().toString(), key.getText().toString(), model.getText().toString() });
                curProv[0] = np;
                String[] d = drafts.get(np);
                if (d == null) d = new String[] { cfg.urlOf(np), cfg.keyOf(np), cfg.modelOf(np) };
                url.setText(d[0]);
                key.setText(d[1]);
                model.setText(d[2]);
            }
            @Override public void onNothingSelected(AdapterView<?> a) { }
        });
        fetch.setOnClickListener(v -> fetchAndPick(url.getText().toString(), key.getText().toString(),
                curProv[0], s -> model.setText(s)));

        ScrollView sv = new ScrollView(this);
        sv.addView(form);
        new AlertDialog.Builder(this, DIALOG_THEME)
                .setTitle("Cài đặt API & model")
                .setView(sv)
                .setPositiveButton("Lưu", (d, w) -> {
                    drafts.put(curProv[0], new String[] {
                            url.getText().toString(), key.getText().toString(), model.getText().toString() });
                    for (Map.Entry<String, String[]> e : drafts.entrySet()) {
                        String[] v = e.getValue();
                        cfg.save(e.getKey(), v[0].trim(), v[1].trim(), v[2].trim());
                    }
                    cfg.setProvider(curProv[0]);
                    cfg.setSystem(sys.getText().toString().trim());
                    modelCache.clear();
                    refreshChip();
                    Toast.makeText(this, "Đã lưu cài đặt", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Huỷ", null)
                .show();
    }

    private void pickModelQuick() {
        String prov = cfg.provider();
        boolean custom = prov.equals(Config.CUSTOM);
        if ((cfg.apiKey().isEmpty() && !custom) || cfg.baseUrl().trim().isEmpty()) {
            openSettings();
            return;
        }
        Consumer<String> pick = m -> { cfg.setModel(m); refreshChip(); };
        List<String> cached = modelCache.get(prov);
        if (cached != null) showModelPicker(cached, pick);
        else fetchAndPick(cfg.baseUrl(), cfg.apiKey(), prov, pick);
    }

    private void fetchAndPick(String url, String key, String prov, Consumer<String> onPick) {
        if (url.trim().isEmpty()) { toast("Hãy nhập Base URL trước"); return; }
        Toast.makeText(this, "Đang tải danh sách model…", Toast.LENGTH_SHORT).show();
        final ApiClient c = new ApiClient();
        io.execute(() -> {
            try {
                final List<String> ms = c.fetchModels(url, key);
                ui.post(() -> {
                    modelCache.put(prov, ms);
                    showModelPicker(ms, onPick);
                });
            } catch (Exception e) {
                final String m = e.getMessage() == null ? e.toString() : e.getMessage();
                ui.post(() -> {
                    toast("Không tải được danh sách: " + m);
                    manualModel(onPick);
                });
            }
        });
    }

    private void showModelPicker(final List<String> all, final Consumer<String> onPick) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(16), dp(8), dp(16), 0);
        final EditText q = new EditText(this);
        q.setHint("Tìm model…");
        q.setSingleLine(true);
        box.addView(q);
        final ListView lv = new ListView(this);
        final ArrayList<String> shown = new ArrayList<>(all);
        final ArrayAdapter<String> ad = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, shown);
        lv.setAdapter(ad);
        box.addView(lv, new LinearLayout.LayoutParams(MATCH, dp(360)));
        final AlertDialog dlg = new AlertDialog.Builder(this, DIALOG_THEME)
                .setTitle("Chọn model (" + all.size() + ")")
                .setView(box)
                .setNeutralButton("Nhập tay", (d, w) -> manualModel(onPick))
                .setNegativeButton("Đóng", null)
                .create();
        q.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            public void onTextChanged(CharSequence s, int a, int b, int c) { }
            public void afterTextChanged(Editable e) {
                String f = e.toString().trim().toLowerCase();
                shown.clear();
                for (String m : all) if (m.toLowerCase().contains(f)) shown.add(m);
                ad.notifyDataSetChanged();
            }
        });
        lv.setOnItemClickListener((p, v, pos, id) -> {
            onPick.accept(shown.get(pos));
            dlg.dismiss();
        });
        dlg.show();
    }

    private void manualModel(final Consumer<String> onPick) {
        final EditText e = new EditText(this);
        e.setHint("Tên model, vd: gpt-4o-mini");
        e.setSingleLine(true);
        LinearLayout box = new LinearLayout(this);
        box.setPadding(dp(20), dp(8), dp(20), 0);
        box.addView(e, new LinearLayout.LayoutParams(MATCH, WRAP));
        new AlertDialog.Builder(this, DIALOG_THEME)
                .setTitle("Nhập tên model")
                .setView(box)
                .setPositiveButton("OK", (d, w) -> {
                    String m = e.getText().toString().trim();
                    if (!m.isEmpty()) onPick.accept(m);
                })
                .setNegativeButton("Huỷ", null)
                .show();
    }

    // =============================================================== ZIP editor

    private void showZip() {
        if (zipRoot == null) zipRoot = buildZip();
        setContentView(zipRoot);
        updateZipUi();
    }

    private View buildZip() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFFFFFFFF);

        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(8), dp(6), dp(16), dp(6));
        TextView back = iconBtn("←");
        back.setOnClickListener(v -> setContentView(chatRoot));
        bar.addView(back);
        TextView t = tv("Sửa ZIP bằng AI", 18, 0xFF111827);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        bar.addView(t);
        root.addView(bar);

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(16), dp(8), dp(16), dp(16));

        zipPick = new Button(this);
        zipPick.setText("1. Chọn ZIP cần sửa");
        zipPick.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("application/zip");
            startActivityForResult(i, PICK_ZIP);
        });
        body.addView(zipPick);

        zipInstruction = new EditText(this);
        zipInstruction.setHint("Nhập yêu cầu sửa cho toàn bộ ZIP…");
        zipInstruction.setMinLines(3);
        zipInstruction.setGravity(Gravity.TOP);
        zipInstruction.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        body.addView(zipInstruction);

        TextView tpmLabel = tv("Giới hạn token trả về mỗi phút (0 = tự nhận khi gặp lỗi 429)", 13, 0xFF6B7280);
        tpmLabel.setPadding(0, dp(10), 0, 0);
        body.addView(tpmLabel);
        zipTpm = new EditText(this);
        zipTpm.setHint("vd: 1000");
        zipTpm.setText(String.valueOf(cfg.tpm()));
        zipTpm.setSingleLine(true);
        zipTpm.setInputType(InputType.TYPE_CLASS_NUMBER);
        body.addView(zipTpm);

        zipEdit = new Button(this);
        zipEdit.setText("2. Mở quy trình 4 Worker + AI Trung tâm");
        zipEdit.setOnClickListener(v -> startZipEdit());
        body.addView(zipEdit);

        zipExport = new Button(this);
        zipExport.setText("3. Xuất ZIP sau khi AI Trung tâm APPROVED");
        zipExport.setOnClickListener(v -> saveZip());
        body.addView(zipExport);

        zipProgress = tv("", 14, 0xFF374151);
        zipProgress.setPadding(0, dp(12), 0, 0);
        body.addView(zipProgress);

        ScrollView sv = new ScrollView(this);
        sv.addView(body);
        root.addView(sv, new LinearLayout.LayoutParams(MATCH, 0, 1f));
        return root;
    }

    private void updateZipUi() {
        if (zipEdit == null) return;
        zipEdit.setEnabled(!archive.isEmpty() && !zipBusy);
        zipExport.setEnabled(zipReady && !zipBusy);
        zipPick.setEnabled(!zipBusy);
    }

    private void zipMsg(final String s) { ui.post(() -> zipProgress.setText(s)); }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (res != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        if (req == PICK_ZIP) loadZip(uri);
        else if (req == SAVE_ZIP) writeZip(uri);
    }

    private void loadZip(final Uri uri) {
        zipBusy = true;
        updateZipUi();
        io.execute(() -> {
            try (InputStream is = getContentResolver().openInputStream(uri);
                 ZipInputStream zis = new ZipInputStream(is)) {
                archive.clear();
                changes.clear();
                zipReady = false;
                ZipEntry ze;
                while ((ze = zis.getNextEntry()) != null) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    byte[] buf = new byte[8192];
                    int n;
                    while ((n = zis.read(buf)) != -1) baos.write(buf, 0, n);
                    archive.put(ze.getName(), baos.toByteArray());
                }
                zipMsg("Đã nhận " + archive.size() + " mục.");
            } catch (Exception e) {
                zipMsg("Lỗi đọc ZIP: " + e.getMessage());
            }
            zipBusy = false;
            ui.post(this::updateZipUi);
        });
    }

    private static boolean editable(String name) {
        return name.endsWith(".java") || name.endsWith(".kt") || name.endsWith(".xml")
                || name.endsWith(".gradle") || name.endsWith(".html") || name.endsWith(".js")
                || name.endsWith(".css") || name.endsWith(".json") || name.endsWith(".md");
    }

    /** Bỏ phần <think>…</think> và hàng rào ``` mà model có thể thêm vào. */
    private static String cleanOut(String raw) {
        String t = raw.replaceAll("(?s)<think>.*?</think>", "").trim();
        if (t.startsWith("```")) {
            int nl = t.indexOf('\n');
            if (nl > 0) t = t.substring(nl + 1);
            if (t.endsWith("```")) t = t.substring(0, t.length() - 3);
        }
        return t;
    }

    /** Cắt nội dung thành các phần theo dòng, mỗi phần tối đa ~max ký tự (ưu tiên cắt ở dòng trống). */
    private static List<String> splitParts(String s, int max) {
        List<String> out = new ArrayList<>();
        if (s.length() <= max) { out.add(s); return out; }
        StringBuilder cur = new StringBuilder();
        int pos = 0;
        while (pos < s.length()) {
            int nl = s.indexOf('\n', pos);
            int end = nl < 0 ? s.length() : nl + 1;
            String line = s.substring(pos, end);
            if (cur.length() > 0 && cur.length() + line.length() > max) {
                out.add(cur.toString());
                cur.setLength(0);
            }
            cur.append(line);
            pos = end;
            if (cur.length() >= max * 0.6 && line.trim().isEmpty()) {
                out.add(cur.toString());
                cur.setLength(0);
            }
        }
        if (cur.length() > 0) out.add(cur.toString());
        return out;
    }

    private int chunkChars() {
        if (effTpm <= 0) return 6000;
        return Math.max(400, Math.min(6000, effTpm * 3 * 6 / 10));
    }

    private int effTpm = 0;

    private void startZipEdit() {
        // Pipeline sửa ZIP cũ đã bị vô hiệu hóa để không bypass Central Judge.
        startActivity(new Intent(this, TeamActivity.class));
    }

    private void runZipEdit(String inst, String base, String key, String model) throws Exception {
        changes.clear();
        effTpm = cfg.tpm();
        List<String> names = new ArrayList<>();
        for (Map.Entry<String, byte[]> en : archive.entrySet()) {
            if (editable(en.getKey()) && en.getValue().length < 60000) names.add(en.getKey());
        }
        ApiClient client = new ApiClient();
        List<String> skipped = new ArrayList<>();
        String lastErr = "";
        int chunk = chunkChars();
        for (int i = 0; i < names.size(); i++) {
            String name = names.get(i);
            String code = new String(archive.get(name), StandardCharsets.UTF_8);
            String result = null;
            for (int round = 0; round < 4 && result == null; round++) {
                List<String> parts = splitParts(code, chunk);
                StringBuilder sb = new StringBuilder();
                try {
                    for (int k = 0; k < parts.size(); k++) {
                        String label = "Đang sửa (" + (i + 1) + "/" + names.size() + "): " + name
                                + (parts.size() > 1 ? " — phần " + (k + 1) + "/" + parts.size() : "");
                        long t0 = System.currentTimeMillis();
                        String out = editPiece(client, base, key, model, inst, name, parts.get(k), k, parts.size(), label);
                        sb.append(out);
                        boolean lastPiece = i == names.size() - 1 && k == parts.size() - 1;
                        if (!lastPiece) pace(out.length(), t0);
                    }
                    result = sb.toString();
                } catch (ApiClient.RateLimit rl) {          // chỉ tới đây khi một phần gửi đã vượt giới hạn
                    lastErr = rl.getMessage();
                    if (rl.limit > 0) effTpm = rl.limit;
                    if (chunk <= 400) break;
                    chunk = Math.max(400, chunk * 6 / 10);   // cắt nhỏ hơn rồi làm lại file này
                } catch (Exception ex) {
                    String m = ex.getMessage() == null ? ex.toString() : ex.getMessage();
                    if (m.startsWith("HTTP 401") || m.startsWith("HTTP 403")) throw ex;   // sai key: dừng hẳn
                    lastErr = m;
                    break;                                                                // bỏ qua file này
                }
            }
            if (result == null) skipped.add(name);
            else changes.put(name, result.getBytes(StandardCharsets.UTF_8));
        }
        if (effTpm > 0 && effTpm != cfg.tpm()) {
            final int learned = effTpm;
            cfg.setTpm(learned);
            ui.post(() -> zipTpm.setText(String.valueOf(learned)));
        }
        zipReady = true;
        String done = "XONG! Đã sửa " + changes.size() + "/" + names.size() + " file.";
        if (!skipped.isEmpty()) {
            done += "\nBỏ qua " + skipped.size() + " file (giữ nguyên bản gốc): " + skipped
                    + "\nLý do gần nhất: " + lastErr;
        }
        zipMsg(done + "\nBấm \"3. Xuất file ZIP\" để lưu.");
    }

    private String editPiece(ApiClient client, String base, String key, String model, String inst,
                             String name, String piece, int k, int n, String label) throws Exception {
        String sys, user;
        if (n == 1) {
            sys = "You are a code editor. Apply the user's request to the file. "
                    + "Return ONLY the complete new file content, with no explanation and no markdown fences. "
                    + "If the file needs no change, return it unchanged.";
            user = "Request: " + inst + "\nFile: " + name + "\nContent:\n" + piece;
        } else {
            sys = "You are a code editor working on one fragment of a larger file, cut at line boundaries. "
                    + "Apply the user's request only inside this fragment. "
                    + "Return ONLY the complete edited fragment, with no explanation and no markdown fences. "
                    + "Do not add package/import/class wrappers that are not in the fragment, and do not repeat other parts of the file. "
                    + "If the fragment needs no change, return it unchanged.";
            user = "Request: " + inst + "\nFile: " + name + " (fragment " + (k + 1) + " of " + n + ")\nContent:\n" + piece;
        }
        JSONArray msgs = new JSONArray().put(msg("system", sys)).put(msg("user", user));
        String lastMsg = "";
        for (int attempt = 1; attempt <= 8; attempt++) {
            zipMsg(label + (attempt > 1 ? "  [thử lần " + attempt + "]" : ""));
            try {
                return normalizePiece(client.complete(base, key, model, msgs), piece);
            } catch (ApiClient.RateLimit rl) {
                if (rl.tooBig) throw rl;
                if (rl.limit > 0 && effTpm == 0) effTpm = rl.limit;
                lastMsg = rl.getMessage();
                long wait = Math.min(rl.waitMs + 700, 65000);
                for (long left = wait; left > 0; left -= 1000) {
                    zipMsg("Chạm giới hạn token/phút. Chờ " + (left / 1000 + 1) + "s rồi thử lại: " + label);
                    Thread.sleep(Math.min(1000, left));
                }
            }
        }
        throw new Exception("Thử 8 lần vẫn bị giới hạn: " + lastMsg);
    }

    private static String normalizePiece(String raw, String orig) throws Exception {
        String t = cleanOut(raw);
        if (!orig.startsWith("\n")) t = t.replaceFirst("^\\n+", "");
        t = t.replaceFirst("\\s+$", "");
        if (orig.trim().length() > 0 && t.trim().isEmpty()) throw new Exception("Model trả về rỗng");
        if (orig.length() > 200 && t.length() < orig.length() * 0.25) {
            throw new Exception("Kết quả ngắn bất thường (có thể bị cắt giữa chừng)");
        }
        return orig.endsWith("\n") ? t + "\n" : t;
    }

    /** Nghỉ đủ lâu để lượng token trả về không vượt giới hạn mỗi phút. */
    private void pace(int outChars, long t0) throws InterruptedException {
        if (effTpm <= 0) return;
        long need = (long) (outChars / 2.8 / effTpm * 60000);
        long wait = need - (System.currentTimeMillis() - t0);
        for (long left = wait; left > 0; left -= 1000) {
            zipMsg("Nghỉ " + (left / 1000 + 1) + "s để không vượt " + effTpm + " token/phút…");
            Thread.sleep(Math.min(1000, left));
        }
    }

    private void saveZip() {
        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("application/zip");
        i.putExtra(Intent.EXTRA_TITLE, "du_an_da_sua.zip");
        startActivityForResult(i, SAVE_ZIP);
    }

    private void writeZip(final Uri uri) {
        io.execute(() -> {
            try (OutputStream os = getContentResolver().openOutputStream(uri);
                 ZipOutputStream zos = new ZipOutputStream(os)) {
                for (Map.Entry<String, byte[]> e : archive.entrySet()) {
                    zos.putNextEntry(new ZipEntry(e.getKey()));
                    byte[] data = changes.containsKey(e.getKey()) ? changes.get(e.getKey()) : e.getValue();
                    zos.write(data);
                    zos.closeEntry();
                }
                zipMsg("Đã lưu ZIP thành công!");
            } catch (Exception e) {
                zipMsg("Lỗi lưu: " + e.getMessage());
            }
        });
    }
}
