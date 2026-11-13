package com.example.chatzipai;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Đội AI: AI Trung tâm lập kế hoạch và giám sát; đúng 4 worker sửa toàn file được giao, sau đó AI Trung tâm kiểm tra từng file, dependency liên file và Final ZIP.
 * Mỗi thành viên có avatar, màu, vai trò, và có thể dùng nhà cung cấp / model khác nhau.
 */
public class TeamActivity extends Activity {
    private static final int PICK_ZIP = 21, SAVE_ZIP = 22;
    private static final int BLUE = 0xFF4D6BFE, PURPLE = 0xFF8B5CF6;
    private static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT;
    private static final int WRAP = ViewGroup.LayoutParams.WRAP_CONTENT;
    private static final int DIALOG_THEME = android.R.style.Theme_Material_Light_Dialog_Alert;
    private static final int[] COLORS = {
            0xFF4D6BFE, 0xFF10B981, 0xFFF59E0B, 0xFFEF4444, 0xFF8B5CF6, 0xFF0EA5E9, 0xFFEC4899, 0xFF14B8A6 };
    private static final String[] AVATARS = {
            "🧠", "🎨", "⚙️", "🐞", "🚀", "🔍", "🌐", "🛡️", "🤖", "🦊",
            "🐼", "🐯", "🦉", "🐙", "👾", "🧑‍💻", "🧙", "🦄", "🐲", "🔥" };
    /** Mẫu vai trò: {avatar, tên vai trò, mô tả giao cho AI}. */
    private static final String[][] PRESETS = {
            { "🧠", "Kiến trúc sư", "Trưởng nhóm: chia việc hợp lý, giữ quy ước chung thống nhất, ưu tiên thiết kế gọn và nhất quán." },
            { "🎨", "Dev giao diện", "Chuyên UI/UX: bố cục, màu sắc, hiệu ứng, trải nghiệm trên điện thoại." },
            { "⚙️", "Dev logic", "Chuyên logic nghiệp vụ, xử lý dữ liệu, mạng và gọi API." },
            { "🐞", "Tester", "Chuyên tìm lỗi, xử lý ngoại lệ, trường hợp biên và lỗi crash." },
            { "🚀", "Tối ưu", "Chuyên hiệu năng, bộ nhớ, làm code gọn và dễ đọc." },
            { "🔍", "Reviewer", "Soi lỗi lệch tên hàm/biến giữa các file, đảm bảo code biên dịch được." },
            { "🌐", "Việt hoá", "Chuyên chữ hiển thị, chuỗi tiếng Việt, đa ngôn ngữ." },
            { "🛡️", "Bảo mật", "Chuyên bảo mật API key, quyền truy cập và dữ liệu người dùng." }
    };
    private static final String[] QUICK = {
            "🌙 Thêm chế độ tối cho toàn app", "🐞 Tìm và sửa lỗi tiềm ẩn", "🎨 Làm đẹp giao diện hiện đại hơn",
            "🌐 Việt hoá toàn bộ chữ hiển thị", "⚡ Tối ưu hiệu năng và dọn code", "📝 Thêm chú thích tiếng Việt cho code" };
    private static final int MAX_AGENTS = 8;
    private static final int MAX_ZIP_ENTRIES = 2000;
    private static final long MAX_TOTAL_UNCOMPRESSED = 80L * 1024L * 1024L;
    private static final long MAX_SINGLE_FILE = 8L * 1024L * 1024L;
    private static final int MAX_REPAIR_ROUNDS = 2;

    /** Một thành viên của đội. */
    private static final class Agent {
        String name, provider, model, avatar, title, role;
        int color;
        Agent(String name, String provider, String model, String avatar, int color, String title, String role) {
            this.name = name; this.provider = provider; this.model = model; this.avatar = avatar;
            this.color = color; this.title = title; this.role = role;
        }
        Agent copy() { return new Agent(name, provider, model, avatar, color, title, role); }
    }

    private Config cfg;
    private android.content.SharedPreferences prefs;
    private final ExecutorService io = Executors.newCachedThreadPool();
    private final Handler ui = new Handler(Looper.getMainLooper());
    private final List<Agent> agents = new ArrayList<>();
    private final Map<String, List<String>> modelCache = new HashMap<>();

    // zip
    private final LinkedHashMap<String, byte[]> archive = new LinkedHashMap<>();
    private final LinkedHashMap<String, Boolean> archiveDirs = new LinkedHashMap<>();
    private final Map<String, byte[]> changes = new ConcurrentHashMap<>();
    private boolean zipLoaded = false;

    // trạng thái làm việc
    private volatile boolean busy = false, cancelled = false;
    private volatile boolean planned = false, edited = false, integrationApproved = false;
    private volatile String integrationReport = "";
    private final List<ApiClient> liveClients = new ArrayList<>();
    private final Map<String, List<String>> plan = new ConcurrentHashMap<>();   // tên thành viên -> danh sách file
    private final Set<String> workerFailures = ConcurrentHashMap.newKeySet();
    private final Map<String, String> workerFileStatus = new ConcurrentHashMap<>();
    private final Map<String, byte[]> approvedSnapshot = new ConcurrentHashMap<>();
    private final Map<String, String> approvedManifest = new ConcurrentHashMap<>();
    private String planNotes = "";
    private final List<String[]> transcript = new ArrayList<>();                // {người nói, nội dung}
    private final Map<String, String> status = new LinkedHashMap<>();

    // UI
    private LinearLayout chat, stripRow;
    private ScrollView scroll;
    private EditText input;
    private TextView subtitle, bPick, bMeet, bWork, bExport, bStop;

    // =============================================================== lifecycle

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        prefs = getSharedPreferences("settings", MODE_PRIVATE);
        cfg = new Config(prefs);
        getWindow().setStatusBarColor(BLUE);
        getWindow().setNavigationBarColor(0xFFFFFFFF);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        loadAgents();
        setContentView(build());
        post("Hệ thống", "Chào mừng đến Đội AI 👋\nChọn ZIP → nhắn mục tiêu vào nhóm → \"Họp & chia việc\" → \"Cùng sửa\" → \"Xuất ZIP\".\n"
                + "Bấm ⚙ để đặt avatar, vai trò và model riêng cho từng thành viên.");
        refreshUi();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cancelled = true;
        cancelAll();
    }

    // =============================================================== helpers

    private int dp(float v) { return (int) (v * getResources().getDisplayMetrics().density + 0.5f); }

    private GradientDrawable round(int color, float r) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(r));
        return g;
    }

    private TextView tv(String s, float sp, int color) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        return t;
    }

    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_LONG).show(); }

    private static JSONObject msg(String role, String content) {
        try {
            return new JSONObject().put("role", role).put("content", content);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** Trộn màu c1 với c2 theo tỉ lệ t (0..1). */
    private static int blend(int c1, int c2, float t) {
        int r = (int) (((c1 >> 16) & 255) * (1 - t) + ((c2 >> 16) & 255) * t);
        int g = (int) (((c1 >> 8) & 255) * (1 - t) + ((c2 >> 8) & 255) * t);
        int b = (int) ((c1 & 255) * (1 - t) + (c2 & 255) * t);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private int colorOf(Agent a) { return COLORS[Math.abs(a.color) % COLORS.length]; }

    private Agent agentOf(String name) {
        for (Agent a : agents) if (a.name.equals(name)) return a;
        return null;
    }

    private int colorOf(String name) {
        Agent a = agentOf(name);
        return a == null ? 0xFF6B7280 : colorOf(a);
    }

    private void styleAvatar(TextView v, Agent a, int sizeDp, int ring) {
        v.setText(a.avatar);
        v.setTextSize(sizeDp * 0.42f);
        v.setGravity(Gravity.CENTER);
        v.setIncludeFontPadding(false);
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(blend(colorOf(a), 0xFFFFFFFF, 0.78f));
        g.setStroke(dp(2.5f), ring);
        v.setBackground(g);
        v.setLayoutParams(new LinearLayout.LayoutParams(dp(sizeDp), dp(sizeDp)));
    }

    private TextView avatarView(Agent a, int sizeDp, int ring) {
        TextView v = new TextView(this);
        styleAvatar(v, a, sizeDp, ring);
        return v;
    }

    // =============================================================== thành viên

    private void loadAgents() {
        agents.clear();
        try {
            JSONArray a = new JSONArray(prefs.getString("team", "[]"));
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                agents.add(new Agent(o.optString("name"), o.optString("provider"), o.optString("model"),
                        o.optString("avatar", AVATARS[i % AVATARS.length]), o.optInt("color", i),
                        o.optString("title", ""), o.optString("role", "")));
            }
        } catch (Exception ignored) { }
        if (agents.isEmpty()) {
            for (int i = 0; i < 4; i++) {
                String[] p = PRESETS[i];
                agents.add(new Agent(p[1], cfg.provider(), "", p[0], i, p[1], p[2]));
            }
        }
        while (agents.size() > 4) agents.remove(agents.size() - 1);
        while (agents.size() < 4) {
            int i = agents.size();
            String[] p = PRESETS[i];
            agents.add(new Agent(p[1], cfg.provider(), "", p[0], i, p[1], p[2]));
        }
        // Tên hiển thị chỉ để người dùng nhìn; vẫn phải unique để không làm hỏng map phân công.
        for (int i = 0; i < agents.size(); i++) {
            Agent a = agents.get(i);
            String base = a.name == null || a.name.trim().isEmpty() ? "Worker " + (i + 1) : a.name.trim();
            a.name = uniqueName(agents, a, base);
        }
    }

    private void saveAgents() {
        JSONArray a = new JSONArray();
        try {
            for (Agent g : agents) {
                a.put(new JSONObject().put("name", g.name).put("provider", g.provider).put("model", g.model)
                        .put("avatar", g.avatar).put("color", g.color).put("title", g.title).put("role", g.role));
            }
        } catch (Exception ignored) { }
        prefs.edit().putString("team", a.toString()).apply();
    }

    private String urlOf(Agent a) { return cfg.urlOf(a.provider); }
    private String keyOf(Agent a) { return cfg.keyOf(a.provider); }
    private String modelOf(Agent a) { return a.model.trim().isEmpty() ? cfg.modelOf(a.provider) : a.model.trim(); }

    private Agent centralJudgeAgent() {
        return new Agent("AI Trung tâm", cfg.centralProvider(), cfg.centralModel(), "🧑‍⚖️", 4, "Giám khảo",
                "Giám khảo độc lập: lập kế hoạch, kiểm tra toàn bộ project sau khi 4 worker hoàn thành và quyết định APPROVED/REJECTED.");
    }

    private String centralUrl() { return cfg.centralUrl(); }
    private String centralKey() { return cfg.centralKey(); }
    private String centralModel() { return cfg.centralModel(); }

    private String persona(Agent a) {
        return a.role == null || a.role.trim().isEmpty() ? "" : " Vai trò của bạn: " + a.role.trim() + ".";
    }

    /** Trả về lý do thiếu cấu hình của một thành viên, hoặc null nếu ổn. */
    private String missingConfig(Agent a) {
        boolean custom = a.provider.equals(Config.CUSTOM);
        if (urlOf(a).trim().isEmpty()) return a.name + ": chưa có Base URL (" + a.provider + ")";
        if (keyOf(a).isEmpty() && !custom) return a.name + ": chưa có API key (" + a.provider + ")";
        if (modelOf(a).isEmpty()) return a.name + ": chưa chọn model";
        return null;
    }

    // ---- model đã chết (hết hạn / không có quyền) bị ẩn khỏi danh sách chọn

    private Set<String> deadSet() {
        return new HashSet<>(prefs.getStringSet("dead_models", new HashSet<String>()));
    }

    private void markDead(Agent a) {
        Set<String> d = deadSet();
        d.add(a.provider + "|" + modelOf(a));
        prefs.edit().putStringSet("dead_models", d).apply();
    }

    private static boolean isModelGone(String m) {
        String l = m == null ? "" : m.toLowerCase();
        return l.contains("decommission") || l.contains("deprecated")
                || (l.contains("model") && (l.contains("does not exist") || l.contains("not found")
                || l.contains("no access") || l.contains("do not have access") || l.contains("not supported")));
    }

    /** Bỏ model không phải chat (giọng nói, kiểm duyệt, nhúng…) và model đã bị đánh dấu chết. */
    private List<String> filterModels(String prov, List<String> all) {
        String[] junk = { "whisper", "orpheus", "guard", "tts", "embed", "moderation", "safeguard",
                "dall-e", "image", "transcribe", "speech", "rerank", "playai" };
        Set<String> dead = deadSet();
        List<String> out = new ArrayList<>();
        for (String m : all) {
            String l = m.toLowerCase();
            boolean bad = dead.contains(prov + "|" + m);
            for (String j : junk) if (l.contains(j)) bad = true;
            if (!bad) out.add(m);
        }
        return out.isEmpty() ? all : out;
    }

    private void pickModel(final String prov, final EditText target) {
        if (cfg.urlOf(prov).trim().isEmpty()) { toast("Chưa có Base URL cho " + prov + " (vào Cài đặt API & model)"); return; }
        if (cfg.keyOf(prov).isEmpty() && !prov.equals(Config.CUSTOM)) { toast("Chưa có API key cho " + prov + " (vào Cài đặt API & model)"); return; }
        List<String> cached = modelCache.get(prov);
        if (cached != null) { showModelPicker(prov, cached, target); return; }
        Toast.makeText(this, "Đang tải danh sách model…", Toast.LENGTH_SHORT).show();
        final ApiClient c = new ApiClient();
        io.execute(() -> {
            try {
                final List<String> ms = c.fetchModels(cfg.urlOf(prov), cfg.keyOf(prov));
                ui.post(() -> { modelCache.put(prov, ms); showModelPicker(prov, ms, target); });
            } catch (Exception e) {
                final String m = e.getMessage() == null ? e.toString() : e.getMessage();
                ui.post(() -> toast("Không tải được danh sách: " + m));
            }
        });
    }

    private void showModelPicker(String prov, List<String> all, final EditText target) {
        final List<String> src = filterModels(prov, all);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(16), dp(8), dp(16), 0);
        final EditText q = new EditText(this);
        q.setHint("Tìm model…");
        q.setSingleLine(true);
        box.addView(q);
        final ListView lv = new ListView(this);
        final ArrayList<String> shown = new ArrayList<>(src);
        final ArrayAdapter<String> ad = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, shown);
        lv.setAdapter(ad);
        box.addView(lv, new LinearLayout.LayoutParams(MATCH, dp(360)));
        final AlertDialog dlg = new AlertDialog.Builder(this, DIALOG_THEME)
                .setTitle("Chọn model (" + src.size() + ")")
                .setView(box)
                .setNegativeButton("Đóng", null)
                .create();
        q.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            public void onTextChanged(CharSequence s, int a, int b, int c) { }
            public void afterTextChanged(Editable e) {
                String f = e.toString().trim().toLowerCase();
                shown.clear();
                for (String m : src) if (m.toLowerCase().contains(f)) shown.add(m);
                ad.notifyDataSetChanged();
            }
        });
        lv.setOnItemClickListener((p, v, pos, id) -> {
            target.setText(shown.get(pos));
            dlg.dismiss();
        });
        dlg.show();
    }

    private void showAvatarPicker(final Agent a, final Runnable done) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER_HORIZONTAL);
        box.setPadding(dp(16), dp(8), dp(16), dp(4));
        final TextView preview = avatarView(a, 64, colorOf(a));
        box.addView(preview);
        TextView l1 = tv("Chọn avatar", 13, 0xFF6B7280);
        l1.setPadding(0, dp(12), 0, dp(4));
        box.addView(l1);
        LinearLayout row = null;
        for (int i = 0; i < AVATARS.length; i++) {
            if (i % 5 == 0) {
                row = new LinearLayout(this);
                row.setGravity(Gravity.CENTER);
                box.addView(row);
            }
            final String e = AVATARS[i];
            TextView t = tv(e, 26, 0xFF111827);
            t.setGravity(Gravity.CENTER);
            t.setOnClickListener(v -> { a.avatar = e; styleAvatar(preview, a, 64, colorOf(a)); });
            row.addView(t, new LinearLayout.LayoutParams(dp(52), dp(48)));
        }
        TextView l2 = tv("Chọn màu", 13, 0xFF6B7280);
        l2.setPadding(0, dp(12), 0, dp(6));
        box.addView(l2);
        LinearLayout cr = new LinearLayout(this);
        cr.setGravity(Gravity.CENTER);
        for (int i = 0; i < COLORS.length; i++) {
            final int ci = i;
            View dot = new View(this);
            GradientDrawable g = new GradientDrawable();
            g.setShape(GradientDrawable.OVAL);
            g.setColor(COLORS[i]);
            dot.setBackground(g);
            dot.setOnClickListener(v -> { a.color = ci; styleAvatar(preview, a, 64, colorOf(a)); });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(28), dp(28));
            lp.setMargins(dp(5), 0, dp(5), 0);
            cr.addView(dot, lp);
        }
        box.addView(cr);
        ScrollView sv = new ScrollView(this);
        sv.addView(box);
        new AlertDialog.Builder(this, DIALOG_THEME)
                .setTitle("Avatar của " + a.name)
                .setView(sv)
                .setPositiveButton("Xong", (d, w) -> done.run())
                .show();
    }

    private static String uniqueName(List<Agent> list, Agent self, String base) {
        String n = base;
        int k = 2;
        boolean clash = true;
        while (clash) {
            clash = false;
            for (Agent o : list) if (o != self && o.name.equalsIgnoreCase(n)) { clash = true; n = base + " " + k++; break; }
        }
        return n;
    }

    private void openTeamSettings() {
        if (busy) { toast("Đang làm việc, hãy dừng trước khi chỉnh đội"); return; }
        final List<Agent> draft = new ArrayList<>();
        for (Agent a : agents) draft.add(a.copy());
        final LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(16), dp(8), dp(16), dp(4));
        final List<EditText> names = new ArrayList<>(), models = new ArrayList<>(), roles = new ArrayList<>();
        final List<Spinner> provs = new ArrayList<>();
        final EditText centralModelField = new EditText(this);
        final EditText centralUrlField = new EditText(this);
        final EditText centralKeyField = new EditText(this);
        final Spinner centralProviderField = new Spinner(this);
        final Runnable[] rebuild = new Runnable[1];
        centralProviderField.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, Config.NAMES));
        centralProviderField.setSelection(Config.indexOf(cfg.centralProvider()), false);
        centralModelField.setSingleLine(true);
        centralModelField.setHint("Model AI trung tâm");
        centralModelField.setText(cfg.centralModel());
        centralUrlField.setSingleLine(true);
        centralUrlField.setHint("Base URL AI trung tâm");
        centralUrlField.setText(cfg.centralUrl());
        centralKeyField.setSingleLine(true);
        centralKeyField.setHint("API key AI trung tâm");
        centralKeyField.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        centralKeyField.setText(cfg.centralKey());

        // đọc những gì đang gõ trên form vào draft (gọi trước mọi thao tác làm vẽ lại form)
        final Runnable sync = () -> {
            for (int i = 0; i < names.size() && i < draft.size(); i++) {
                Agent d = draft.get(i);
                d.name = names.get(i).getText().toString();
                d.provider = Config.NAMES[provs.get(i).getSelectedItemPosition()];
                d.model = models.get(i).getText().toString();
                d.role = roles.get(i).getText().toString();
            }
        };

        rebuild[0] = () -> {
            form.removeAllViews();
            names.clear(); provs.clear(); models.clear(); roles.clear();
            form.addView(tv("Người đầu tiên là trưởng nhóm (chia việc). Bấm avatar để đổi hình/màu, bấm \"Mẫu\" để chọn vai trò có sẵn. "
                    + "Model để trống = dùng model mặc định của nhà cung cấp.", 12.5f, 0xFF6B7280));
            for (int i = 0; i < draft.size(); i++) {
                final int idx = i;
                final Agent a = draft.get(i);
                int col = colorOf(a);

                LinearLayout card = new LinearLayout(this);
                card.setOrientation(LinearLayout.VERTICAL);
                card.setPadding(dp(12), dp(10), dp(12), dp(10));
                GradientDrawable cb = round(blend(col, 0xFFFFFFFF, 0.93f), 16);
                cb.setStroke(dp(1), blend(col, 0xFFFFFFFF, 0.55f));
                card.setBackground(cb);
                LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(MATCH, WRAP);
                cp.topMargin = dp(12);

                LinearLayout r1 = new LinearLayout(this);
                r1.setGravity(Gravity.CENTER_VERTICAL);
                TextView av = avatarView(a, 52, col);
                ((LinearLayout.LayoutParams) av.getLayoutParams()).rightMargin = dp(10);
                av.setOnClickListener(v -> { sync.run(); showAvatarPicker(a, () -> rebuild[0].run()); });
                r1.addView(av);
                LinearLayout nameCol = new LinearLayout(this);
                nameCol.setOrientation(LinearLayout.VERTICAL);
                TextView tag = tv(i == 0 ? "👑 Trưởng nhóm" + (a.title.isEmpty() ? "" : " · " + a.title)
                        : "Thành viên " + (i + 1) + (a.title.isEmpty() ? "" : " · " + a.title), 12, col);
                tag.setTypeface(Typeface.DEFAULT_BOLD);
                nameCol.addView(tag);
                EditText n = new EditText(this);
                n.setHint("Tên");
                n.setSingleLine(true);
                n.setText(a.name);
                nameCol.addView(n);
                names.add(n);
                r1.addView(nameCol, new LinearLayout.LayoutParams(0, WRAP, 1f));
                TextView tpl = tv("🧩 Mẫu", 13, col);
                tpl.setPadding(dp(10), dp(8), dp(10), dp(8));
                tpl.setBackground(round(blend(col, 0xFFFFFFFF, 0.85f), 14));
                tpl.setOnClickListener(v -> {
                    PopupMenu pm = new PopupMenu(this, v);
                    for (int k = 0; k < PRESETS.length; k++) pm.getMenu().add(0, k, k, PRESETS[k][0] + " " + PRESETS[k][1]);
                    pm.setOnMenuItemClickListener(it -> {
                        sync.run();
                        String[] p = PRESETS[it.getItemId()];
                        a.avatar = p[0];
                        a.title = p[1];
                        a.role = p[2];
                        a.name = uniqueName(draft, a, p[1]);
                        rebuild[0].run();
                        return true;
                    });
                    pm.show();
                });
                r1.addView(tpl);
                card.addView(r1);

                Spinner sp = new Spinner(this);
                sp.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, Config.NAMES));
                sp.setSelection(Config.indexOf(a.provider), false);
                card.addView(sp);
                provs.add(sp);

                LinearLayout r3 = new LinearLayout(this);
                r3.setGravity(Gravity.CENTER_VERTICAL);
                final EditText m = new EditText(this);
                m.setHint("Model (trống = mặc định)");
                m.setSingleLine(true);
                m.setTextSize(14);
                m.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
                m.setText(a.model);
                r3.addView(m, new LinearLayout.LayoutParams(0, WRAP, 1f));
                models.add(m);
                TextView pick = tv("Chọn ▾", 13, 0xFFFFFFFF);
                pick.setTypeface(Typeface.DEFAULT_BOLD);
                pick.setPadding(dp(12), dp(8), dp(12), dp(8));
                pick.setBackground(round(col, 14));
                pick.setOnClickListener(v -> pickModel(Config.NAMES[provs.get(idx).getSelectedItemPosition()], m));
                r3.addView(pick);
                card.addView(r3);

                EditText ro = new EditText(this);
                ro.setHint("Vai trò / chuyên môn (AI sẽ nhập vai theo mô tả này)");
                ro.setTextSize(13);
                ro.setMinLines(2);
                ro.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
                ro.setText(a.role);
                card.addView(ro);
                roles.add(ro);

                if (i > 0) {
                    TextView del = tv("✕ Xoá thành viên này", 13, 0xFFDC2626);
                    del.setPadding(0, dp(6), 0, dp(2));
                    del.setOnClickListener(v -> { sync.run(); draft.remove(idx); rebuild[0].run(); });
                    card.addView(del);
                }
                form.addView(card, cp);
            }
            if (draft.size() < MAX_AGENTS) {
                Button add = new Button(this);
                add.setAllCaps(false);
                add.setText("＋ Thêm thành viên");
                add.setOnClickListener(v -> {
                    sync.run();
                    int ci = draft.size();
                    draft.add(new Agent("Dev " + (char) ('A' + ci), cfg.provider(), "", AVATARS[(ci + 8) % AVATARS.length], ci, "", ""));
                    rebuild[0].run();
                });
                form.addView(add);
            }
            final int hidden = deadSet().size();
            if (hidden > 0) {
                TextView un = tv("↺ Hiện lại " + hidden + " model đã ẩn vì hết hạn / không dùng được", 13, BLUE);
                un.setPadding(0, dp(8), 0, dp(8));
                un.setOnClickListener(v -> {
                    prefs.edit().remove("dead_models").apply();
                    sync.run();
                    rebuild[0].run();
                    toast("Đã hiện lại các model đã ẩn");
                });
                form.addView(un);
            }
        };
        rebuild[0].run();

        ScrollView sv = new ScrollView(this);
        sv.addView(form);
        new AlertDialog.Builder(this, DIALOG_THEME)
                .setTitle("Cài đặt đội AI")
                .setView(sv)
                .setPositiveButton("Lưu", (d, w) -> {
                    sync.run();
                    cfg.saveCentral(Config.NAMES[centralProviderField.getSelectedItemPosition()],
                            centralUrlField.getText().toString().trim(),
                            centralKeyField.getText().toString().trim(),
                            centralModelField.getText().toString().trim());
                    List<Agent> out = new ArrayList<>();
                    for (int i = 0; i < draft.size(); i++) {
                        Agent a = draft.get(i);
                        String nm = a.name.trim().isEmpty() ? "Thành viên " + (i + 1) : a.name.trim();
                        a.name = uniqueName(out, a, nm);
                        a.model = a.model.trim();
                        a.role = a.role.trim();
                        out.add(a);
                    }
                    if (out.size() != 4) { toast("Đội phải có đúng 4 worker"); return; }
                    agents.clear();
                    agents.addAll(out);
                    saveAgents();
                    plan.clear();
                    planned = false;
                    refreshUi();
                    toast("Đã lưu đội " + agents.size() + " thành viên");
                })
                .setNegativeButton("Huỷ", null)
                .show();
    }

    // =============================================================== UI

    private TextView pill(String s, int color) {
        TextView t = tv(s, 13, 0xFFFFFFFF);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setPadding(dp(14), dp(9), dp(14), dp(9));
        t.setGravity(Gravity.CENTER);
        t.setTag(color);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(WRAP, WRAP);
        p.rightMargin = dp(8);
        t.setLayoutParams(p);
        styleBtn(t, true);
        return t;
    }

    private void styleBtn(TextView t, boolean on) {
        int c = (Integer) t.getTag();
        t.setBackground(round(on ? c : 0xFFE5E7EB, 18));
        t.setTextColor(on ? 0xFFFFFFFF : 0xFF9CA3AF);
        t.setEnabled(on);
        t.setClickable(on);
    }

    private View build() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFFF8F9FC);

        // thanh tiêu đề gradient
        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(8), dp(8), dp(8), dp(10));
        bar.setBackground(new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[] { BLUE, PURPLE }));
        TextView back = tv("←", 22, 0xFFFFFFFF);
        back.setGravity(Gravity.CENTER);
        back.setOnClickListener(v -> finish());
        bar.addView(back, new LinearLayout.LayoutParams(dp(40), dp(40)));
        LinearLayout tc = new LinearLayout(this);
        tc.setOrientation(LinearLayout.VERTICAL);
        TextView title = tv("Đội AI", 19, 0xFFFFFFFF);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        tc.addView(title);
        subtitle = tv("", 12, 0xFFDCE3FF);
        tc.addView(subtitle);
        bar.addView(tc, new LinearLayout.LayoutParams(0, WRAP, 1f));
        TextView gear = tv("⚙", 22, 0xFFFFFFFF);
        gear.setGravity(Gravity.CENTER);
        gear.setOnClickListener(v -> openTeamSettings());
        bar.addView(gear, new LinearLayout.LayoutParams(dp(40), dp(40)));
        root.addView(bar, new LinearLayout.LayoutParams(MATCH, WRAP));

        // dải thành viên (avatar + trạng thái)
        HorizontalScrollView strip = new HorizontalScrollView(this);
        strip.setHorizontalScrollBarEnabled(false);
        stripRow = new LinearLayout(this);
        stripRow.setPadding(dp(8), dp(8), dp(8), dp(4));
        strip.addView(stripRow);
        root.addView(strip, new LinearLayout.LayoutParams(MATCH, WRAP));

        // các nút hành động
        HorizontalScrollView hs = new HorizontalScrollView(this);
        hs.setHorizontalScrollBarEnabled(false);
        LinearLayout row = new LinearLayout(this);
        row.setPadding(dp(12), dp(4), dp(8), dp(8));
        bPick = pill("📦 Chọn ZIP", BLUE);
        bPick.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("application/zip");
            startActivityForResult(i, PICK_ZIP);
        });
        bMeet = pill("🗣 Họp & chia việc", PURPLE);
        bMeet.setOnClickListener(v -> startMeeting());
        bWork = pill("⚡ Cùng sửa", 0xFF10B981);
        bWork.setOnClickListener(v -> startWork());
        bExport = pill("💾 Xuất ZIP", 0xFFF59E0B);
        bExport.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("application/zip");
            i.putExtra(Intent.EXTRA_TITLE, "du_an_doi_ai.zip");
            startActivityForResult(i, SAVE_ZIP);
        });
        bStop = pill("⏹ Dừng", 0xFFEF4444);
        bStop.setOnClickListener(v -> { cancelled = true; cancelAll(); post("Hệ thống", "Đã yêu cầu dừng…"); });
        for (TextView b : new TextView[] { bPick, bMeet, bWork, bExport, bStop }) row.addView(b);
        hs.addView(row);
        root.addView(hs, new LinearLayout.LayoutParams(MATCH, WRAP));

        View line = new View(this);
        line.setBackgroundColor(0xFFE5E7EB);
        root.addView(line, new LinearLayout.LayoutParams(MATCH, 1));

        scroll = new ScrollView(this);
        chat = new LinearLayout(this);
        chat.setOrientation(LinearLayout.VERTICAL);
        chat.setPadding(dp(12), dp(12), dp(12), dp(10));
        scroll.addView(chat, new ViewGroup.LayoutParams(MATCH, WRAP));
        root.addView(scroll, new LinearLayout.LayoutParams(MATCH, 0, 1f));

        // gợi ý nhanh
        HorizontalScrollView qs = new HorizontalScrollView(this);
        qs.setHorizontalScrollBarEnabled(false);
        LinearLayout qr = new LinearLayout(this);
        qr.setPadding(dp(12), dp(6), dp(8), 0);
        for (final String q : QUICK) {
            TextView c = tv(q, 12.5f, BLUE);
            c.setPadding(dp(12), dp(7), dp(12), dp(7));
            c.setBackground(round(0xFFE8EDFF, 16));
            c.setOnClickListener(v -> {
                input.setText(q.substring(q.indexOf(' ') + 1));
                input.setSelection(input.getText().length());
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(WRAP, WRAP);
            lp.rightMargin = dp(8);
            qr.addView(c, lp);
        }
        qs.addView(qr);
        root.addView(qs, new LinearLayout.LayoutParams(MATCH, WRAP));

        LinearLayout box = new LinearLayout(this);
        box.setGravity(Gravity.BOTTOM);
        box.setBackground(round(0xFFFFFFFF, 26));
        box.setPadding(dp(18), dp(6), dp(8), dp(6));
        input = new EditText(this);
        input.setBackground(null);
        input.setHint("Nhắn vào nhóm (mục tiêu, yêu cầu thêm…)");
        input.setTextSize(15);
        input.setMaxLines(5);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        box.addView(input, new LinearLayout.LayoutParams(0, WRAP, 1f));
        TextView send = tv("↑", 20, 0xFFFFFFFF);
        send.setGravity(Gravity.CENTER);
        send.setTypeface(Typeface.DEFAULT_BOLD);
        GradientDrawable sg = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[] { BLUE, PURPLE });
        sg.setShape(GradientDrawable.OVAL);
        send.setBackground(sg);
        send.setOnClickListener(v -> {
            String t = input.getText().toString().trim();
            if (t.isEmpty()) return;
            input.setText("");
            post("Bạn", t);
            if (planned && !edited) { planned = false; refreshUi(); post("Hệ thống", "Có yêu cầu mới, hãy bấm \"Họp & chia việc\" lại để cả đội cập nhật kế hoạch."); }
        });
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(dp(40), dp(40));
        sp.gravity = Gravity.BOTTOM;
        box.addView(send, sp);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(MATCH, WRAP);
        bp.setMargins(dp(12), dp(6), dp(12), dp(10));
        root.addView(box, bp);
        return root;
    }

    private void refreshUi() {
        ui.post(() -> {
            if (bPick == null) return;
            styleBtn(bPick, !busy);
            styleBtn(bMeet, zipLoaded && !busy);
            styleBtn(bWork, zipLoaded && planned && !busy);
            styleBtn(bExport, zipLoaded && !changes.isEmpty() && integrationApproved && !busy);
            styleBtn(bStop, busy);
            subtitle.setText(agents.size() + " thành viên" + (zipLoaded ? "  •  ZIP: " + archive.size() + " mục" : "  •  chưa chọn ZIP"));
            renderStatus();
        });
    }

    /** Vẽ lại dải thành viên: avatar có viền theo trạng thái + dòng trạng thái. */
    private void renderStatus() {
        if (stripRow == null) return;
        stripRow.removeAllViews();
        for (Agent a : agents) {
            String st;
            synchronized (status) { st = status.get(a.name); }
            int ring;
            String line;
            if (st == null) {
                ring = 0xFFD1D5DB;
                List<String> fs = plan.get(a.name);
                line = fs != null && !fs.isEmpty() ? fs.size() + " file" : "sẵn sàng";
            } else {
                ring = st.contains("chờ") ? 0xFFF59E0B : 0xFF10B981;
                line = st;
            }
            LinearLayout card = new LinearLayout(this);
            card.setGravity(Gravity.CENTER_VERTICAL);
            card.setPadding(dp(8), dp(6), dp(12), dp(6));
            GradientDrawable cb = round(0xFFFFFFFF, 18);
            cb.setStroke(dp(1), st == null ? 0xFFE5E7EB : blend(ring, 0xFFFFFFFF, 0.5f));
            card.setBackground(cb);
            TextView av = avatarView(a, 38, ring);
            ((LinearLayout.LayoutParams) av.getLayoutParams()).rightMargin = dp(8);
            card.addView(av);
            LinearLayout col = new LinearLayout(this);
            col.setOrientation(LinearLayout.VERTICAL);
            TextView nm = tv(a.name, 12.5f, colorOf(a));
            nm.setTypeface(Typeface.DEFAULT_BOLD);
            nm.setSingleLine(true);
            col.addView(nm);
            TextView sl = tv(line, 11, 0xFF6B7280);
            sl.setMaxLines(2);
            sl.setMaxWidth(dp(150));
            col.addView(sl);
            card.addView(col);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(WRAP, WRAP);
            lp.rightMargin = dp(8);
            stripRow.addView(card, lp);
        }
    }

    private void setStatus(final String who, final String s) {
        synchronized (status) {
            if (s == null) status.remove(who); else status.put(who, s);
        }
        ui.post(this::renderStatus);
    }

    /** Thêm một tin nhắn vào nhóm chat (gọi được từ thread nền). */
    private void post(final String who, final String text) {
        synchronized (transcript) { transcript.add(new String[] { who, text }); }
        ui.post(() -> addBubble(who, text));
    }

    private void addBubble(String who, String text) {
        boolean me = who.equals("Bạn");
        boolean sys = who.equals("Hệ thống");

        if (sys) {   // thông báo hệ thống: viên thuốc ở giữa
            TextView p = tv(text, 12.5f, 0xFF4B5563);
            p.setGravity(Gravity.CENTER);
            p.setPadding(dp(14), dp(7), dp(14), dp(7));
            p.setBackground(round(0xFFE9ECF3, 14));
            p.setMaxWidth((int) (getResources().getDisplayMetrics().widthPixels * 0.88f));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(WRAP, WRAP);
            lp.gravity = Gravity.CENTER_HORIZONTAL;
            lp.bottomMargin = dp(12);
            chat.addView(p, lp);
            scroll.post(() -> scroll.fullScroll(View.FOCUS_DOWN));
            return;
        }

        Agent ag = agentOf(who);
        if (ag == null && !me) ag = new Agent(who, "", "", "🤖", 7, "", "");
        int col = me ? BLUE : colorOf(ag);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(WRAP, WRAP);
        rp.bottomMargin = dp(12);
        rp.gravity = me ? Gravity.END : Gravity.START;

        if (!me) {
            TextView av = avatarView(ag, 36, col);
            ((LinearLayout.LayoutParams) av.getLayoutParams()).rightMargin = dp(8);
            row.addView(av);
        }
        LinearLayout colv = new LinearLayout(this);
        colv.setOrientation(LinearLayout.VERTICAL);
        if (!me) {
            LinearLayout nl = new LinearLayout(this);
            nl.setGravity(Gravity.CENTER_VERTICAL);
            TextView nm = tv(who, 12.5f, col);
            nm.setTypeface(Typeface.DEFAULT_BOLD);
            nl.addView(nm);
            if (!ag.title.isEmpty()) {
                TextView tt = tv("  " + ag.title, 11, 0xFF9CA3AF);
                nl.addView(tt);
            }
            nl.setPadding(dp(4), 0, 0, dp(2));
            colv.addView(nl);
        }

        int bg, fg = 0xFF111827;
        if (me) { bg = BLUE; fg = 0xFFFFFFFF; }
        else if (text.startsWith("✔")) bg = 0xFFDCFCE7;
        else if (text.startsWith("⚠")) bg = 0xFFFEF3C7;
        else if (text.startsWith("↔")) bg = 0xFFF1F5F9;
        else bg = blend(col, 0xFFFFFFFF, 0.88f);
        GradientDrawable g = new GradientDrawable();
        g.setColor(bg);
        float big = dp(18), small = dp(4);
        g.setCornerRadii(me ? new float[] { big, big, small, small, big, big, big, big }
                : new float[] { small, small, big, big, big, big, big, big });
        if (!me) g.setStroke(dp(1), blend(col, 0xFFFFFFFF, 0.65f));

        TextView t = tv("", 15, fg);
        t.setText(me ? text : Md.render(text));
        t.setTextIsSelectable(true);
        t.setPadding(dp(13), dp(9), dp(13), dp(9));
        t.setBackground(g);
        t.setMaxWidth((int) (getResources().getDisplayMetrics().widthPixels * (me ? 0.82f : 0.72f)));
        colv.addView(t);
        row.addView(colv);
        chat.addView(row, rp);
        scroll.post(() -> scroll.fullScroll(View.FOCUS_DOWN));
    }

    // =============================================================== ZIP

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (res != RESULT_OK || data == null || data.getData() == null) return;
        if (req == PICK_ZIP) loadZip(data.getData());
        else if (req == SAVE_ZIP) writeZip(data.getData());
    }

    private static boolean editable(String name) {
        return name.endsWith(".java") || name.endsWith(".kt") || name.endsWith(".xml")
                || name.endsWith(".gradle") || name.endsWith(".html") || name.endsWith(".js")
                || name.endsWith(".css") || name.endsWith(".json") || name.endsWith(".md")
                || name.endsWith(".py") || name.endsWith(".ts") || name.endsWith(".txt");
    }

    private void loadZip(final Uri uri) {
        busy = true;
        refreshUi();
        io.execute(() -> {
            try (InputStream is = getContentResolver().openInputStream(uri)) {
                if (is == null) throw new Exception("Không mở được ZIP");
                ZipInputStream zis = new ZipInputStream(is);
                archive.clear(); archiveDirs.clear(); changes.clear(); approvedSnapshot.clear(); approvedManifest.clear();
                workerFileStatus.clear(); plan.clear(); planned = false; edited = false; integrationApproved = false; integrationReport = "";
                ZipEntry ze; int count = 0; long total = 0;
                while ((ze = zis.getNextEntry()) != null) {
                    String name = ze.getName();
                    if (name == null || name.trim().isEmpty()) throw new Exception("ZIP có entry không tên");
                    if (name.startsWith("/") || name.startsWith("\\") || name.matches("^[A-Za-z]:.*") || java.util.Arrays.asList(name.split("[/\\\\]")).contains(".."))
                        throw new Exception("ZIP chứa đường dẫn nguy hiểm: " + name);
                    if (!archiveDirs.containsKey(name) && archive.containsKey(name))
                        throw new Exception("ZIP trùng entry: " + name);
                    if (archiveDirs.containsKey(name)) throw new Exception("ZIP trùng directory: " + name);
                    if (++count > MAX_ZIP_ENTRIES) throw new Exception("ZIP vượt giới hạn " + MAX_ZIP_ENTRIES + " entry");
                    if (ze.isDirectory()) { archiveDirs.put(name, true); continue; }
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    byte[] buf = new byte[8192]; int n; long fileTotal = 0;
                    while ((n = zis.read(buf)) != -1) {
                        fileTotal += n; total += n;
                        if (fileTotal > MAX_SINGLE_FILE) throw new Exception("File quá lớn: " + name);
                        if (total > MAX_TOTAL_UNCOMPRESSED) throw new Exception("ZIP giải nén vượt giới hạn an toàn");
                        baos.write(buf, 0, n);
                    }
                    archive.put(name, baos.toByteArray());
                }
                zipLoaded = !archive.isEmpty();
                int ed = editableFiles().size();
                post("Hệ thống", "Đã nạp ZIP: " + archive.size() + " file + " + archiveDirs.size() + " thư mục. " + ed + " file có thể sửa. Không tự động bỏ file lớn.");
            } catch (Exception e) {
                archive.clear(); archiveDirs.clear(); zipLoaded = false;
                post("Hệ thống", "Lỗi đọc ZIP: " + (e.getMessage() == null ? e.toString() : e.getMessage()));
            }
            busy = false; refreshUi();
        });
    }

    private void writeZip(final Uri uri) {
        if (!integrationApproved) { toast("AI trung tâm chưa APPROVED bản ghép; không thể xuất ZIP."); return; }
        if (approvedSnapshot.size() != archive.size() || approvedManifest.size() != archive.size()) { toast("Snapshot APPROVED toàn project chưa đầy đủ."); return; }
        busy = true; refreshUi();
        io.execute(() -> {
            try {
                ByteArrayOutputStream buf = new ByteArrayOutputStream();
                try (ZipOutputStream zos = new ZipOutputStream(buf)) {
                    for (String d : archiveDirs.keySet()) {
                        zos.putNextEntry(new ZipEntry(d)); zos.closeEntry();
                    }
                    for (Map.Entry<String, byte[]> e : approvedSnapshot.entrySet()) {
                        byte[] d = e.getValue();
                        String expected = approvedManifest.get(e.getKey());
                        if (expected == null || !expected.equals(sha256(d))) throw new Exception("Snapshot APPROVED đã thay đổi: " + e.getKey());
                        zos.putNextEntry(new ZipEntry(e.getKey())); zos.write(d); zos.closeEntry();
                    }
                }
                byte[] zipBytes = buf.toByteArray();
                if (!validateFinalZip(zipBytes)) throw new Exception("Final ZIP audit thất bại");
                if (!runCentralFinalZipReview(zipBytes)) throw new Exception("AI trung tâm REJECTED Final ZIP");
                try (OutputStream os = getContentResolver().openOutputStream(uri)) {
                    if (os == null) throw new Exception("Không mở được nơi lưu ZIP");
                    os.write(zipBytes);
                }
                post("Hệ thống", "💾 Đã lưu ZIP. Snapshot + hash 100% + Final Judge: PASS.");
            } catch (Exception e) { post("Hệ thống", "Lỗi lưu/kiểm tra ZIP: " + e.getMessage()); }
            busy = false; refreshUi();
        });
    }

    private boolean runCentralFinalZipReview(byte[] zipBytes) throws Exception {
        Agent judge = centralJudgeAgent();
        LinkedHashMap<String, byte[]> actualFiles = new LinkedHashMap<>();
        Set<String> seen = new HashSet<>(); int entries = 0;
        try (ZipInputStream zis = new ZipInputStream(new java.io.ByteArrayInputStream(zipBytes))) {
            ZipEntry e; byte[] b = new byte[8192];
            while ((e = zis.getNextEntry()) != null) {
                if (e.isDirectory()) continue;
                ByteArrayOutputStream file = new ByteArrayOutputStream(); int n;
                while ((n = zis.read(b)) != -1) file.write(b, 0, n);
                if (!seen.add(e.getName())) throw new Exception("ZIP trùng entry: " + e.getName());
                actualFiles.put(e.getName(), file.toByteArray()); entries++;
            }
        }
        if (actualFiles.size() != approvedSnapshot.size()) return false;
        for (Map.Entry<String, byte[]> e : approvedSnapshot.entrySet()) {
            byte[] actual = actualFiles.get(e.getKey());
            if (actual == null || !sha256(actual).equals(approvedManifest.get(e.getKey()))) return false;
        }
        // Final Judge đọc source/config thực tế theo từng file, không chỉ manifest.
        StringBuilder source = new StringBuilder();
        int reviewed = 0;
        for (Map.Entry<String, byte[]> e : actualFiles.entrySet()) {
            if (!editable(e.getKey())) continue;
            String code = new String(e.getValue(), StandardCharsets.UTF_8);
            if (code.length() > 160000) throw new Exception("File Final ZIP quá lớn để Final Judge đọc an toàn: " + e.getKey());
            source.append("\n===== ").append(e.getKey()).append(" =====\n").append(code);
            reviewed++;
            if (source.length() > 120000) {
                // Không được bỏ file; project lớn phải được review theo batch.
                String rawBatch = askCentral(judge, new JSONArray().put(msg("system", AiWorkflow.FINAL + "\nBạn là Final Judge. Đọc source batch thực tế. Chỉ trả JSON {\"approved\":true/false,\"issues\":[...]}"))
                        .put(msg("user", "ĐỀ BÀI:\n" + userGoal() + "\n\nSOURCE BATCH:\n" + source)), "Final Judge kiểm tra source batch…");
                JSONObject rb = parseReview(rawBatch);
                if (!rb.optBoolean("approved", false)) { integrationReport += "\nFINAL SOURCE BATCH REJECT: " + rb.optJSONArray("issues"); return false; }
                source.setLength(0);
            }
        }
        if (source.length() > 0) {
            String rawBatch = askCentral(judge, new JSONArray().put(msg("system", AiWorkflow.FINAL + "\nBạn là Final Judge. Đọc source thực tế của Final ZIP. Chỉ trả JSON {\"approved\":true/false,\"issues\":[...]}"))
                    .put(msg("user", "ĐỀ BÀI:\n" + userGoal() + "\n\nSOURCE FINAL ZIP:\n" + source)), "Final Judge kiểm tra source thực tế…");
            JSONObject rb = parseReview(rawBatch);
            if (!rb.optBoolean("approved", false)) { integrationReport += "\nFINAL SOURCE REJECT: " + rb.optJSONArray("issues"); return false; }
        }
        String manifest = approvedManifestText();
        String sys = AiWorkflow.FINAL + "\nBạn là AI TRUNG TÂM / GIÁM KHẢO cuối. Không sửa code. Kiểm tra manifest/hash, số lượng entry và kết quả source review. Chỉ trả JSON {\"approved\":true/false,\"issues\":[...]}";
        String user = "ĐỀ BÀI:\n" + userGoal() + "\n\nAPPROVED MANIFEST:\n" + manifest + "\n\nFINAL ENTRIES: " + entries + "\nREVIEWED SOURCE FILES: " + reviewed;
        JSONObject r = parseReview(askCentral(judge, new JSONArray().put(msg("system", sys)).put(msg("user", user)), "AI trung tâm kiểm tra toàn vẹn Final ZIP…"));
        return r.optBoolean("approved", false) && entries == approvedSnapshot.size() && seen.size() == approvedSnapshot.size();
    }

    private String approvedManifestText() {
        StringBuilder s = new StringBuilder();
        for (Map.Entry<String, String> e : approvedManifest.entrySet()) s.append(e.getKey()).append(" | sha256=").append(e.getValue()).append("\n");
        return s.toString();
    }

    private static String sha256(byte[] data) throws Exception {
        java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
        byte[] h = md.digest(data);
        StringBuilder s = new StringBuilder(h.length * 2);
        for (byte b : h) s.append(String.format("%02x", b & 0xff));
        return s.toString();
    }

    private boolean validateFinalZip(byte[] data) throws Exception {
        Set<String> seen = new HashSet<>(); int count = 0;
        try (ZipInputStream zis = new ZipInputStream(new java.io.ByteArrayInputStream(data))) {
            ZipEntry e; byte[] b = new byte[8192];
            while ((e = zis.getNextEntry()) != null) {
                if (e.isDirectory()) continue;
                if (!seen.add(e.getName())) throw new Exception("ZIP trùng entry: " + e.getName());
                ByteArrayOutputStream out = new ByteArrayOutputStream(); int n;
                while ((n = zis.read(b)) != -1) out.write(b, 0, n);
                byte[] actual = out.toByteArray();
                if (actual.length == 0) throw new Exception("ZIP có file rỗng: " + e.getName());
                String expected = approvedManifest.get(e.getName());
                if (expected == null || !expected.equals(sha256(actual))) throw new Exception("SHA-256 Final ZIP lệch APPROVED: " + e.getName());
                count++;
            }
        }
        return count == approvedSnapshot.size() && seen.size() == approvedSnapshot.size();
    }

    private List<String> editableFiles() {
        List<String> out = new ArrayList<>();
        for (Map.Entry<String, byte[]> e : archive.entrySet()) {
            if (editable(e.getKey())) out.add(e.getKey());
        }
        return out;
    }

    // =============================================================== gọi API (có chờ khi chạm giới hạn)

    private synchronized void track(ApiClient c, boolean add) {
        if (add) liveClients.add(c); else liveClients.remove(c);
    }

    private synchronized void cancelAll() {
        for (ApiClient c : liveClients) c.cancel();
    }

    private String askCentral(Agent a, JSONArray msgs, String statusLabel) throws Exception {
        if (cancelled) throw new Exception("Đã dừng");
        setStatus(a.name, statusLabel);
        ApiClient c = new ApiClient();
        track(c, true);
        try {
            return c.complete(centralUrl(), centralKey(), centralModel(), msgs);
        } finally {
            track(c, false);
        }
    }

    /** Gọi một thành viên; tự chờ và thử lại khi gặp 429. */
    private String ask(Agent a, JSONArray msgs, String statusLabel) throws Exception {
        String last = "";
        for (int attempt = 1; attempt <= 6; attempt++) {
            if (cancelled) throw new Exception("Đã dừng");
            setStatus(a.name, statusLabel + (attempt > 1 ? " [thử lần " + attempt + "]" : ""));
            ApiClient c = new ApiClient();
            track(c, true);
            try {
                return c.complete(urlOf(a), keyOf(a), modelOf(a), msgs);
            } catch (ApiClient.RateLimit rl) {
                if (rl.tooBig) throw rl;
                last = rl.getMessage();
                long wait = Math.min(rl.waitMs + 700, 65000);
                for (long left = wait; left > 0 && !cancelled; left -= 1000) {
                    setStatus(a.name, "chạm giới hạn, chờ " + (left / 1000 + 1) + "s…");
                    Thread.sleep(Math.min(1000, left));
                }
            } catch (Exception e) {
                if (cancelled) throw new Exception("Đã dừng");
                if (isModelGone(e.getMessage())) {
                    markDead(a);
                    modelCache.remove(a.provider);
                    post("Hệ thống", "🗑 Model " + modelOf(a) + " (" + a.provider + ") không dùng được nên đã bị ẩn khỏi danh sách chọn. "
                            + "Hãy bấm ⚙ đổi model cho " + a.name + ".");
                }
                throw e;
            } finally {
                track(c, false);
            }
        }
        throw new Exception("Thử 6 lần vẫn bị giới hạn: " + last);
    }

    // =============================================================== họp & chia việc

    private String transcriptText() {
        StringBuilder sb = new StringBuilder();
        synchronized (transcript) {
            int from = Math.max(0, transcript.size() - 24);
            for (int i = from; i < transcript.size(); i++) {
                String[] m = transcript.get(i);
                if (m[0].equals("Hệ thống")) continue;
                String t = m[1].length() > 1200 ? m[1].substring(0, 1200) + "…" : m[1];
                sb.append('[').append(m[0]).append("]: ").append(t).append("\n\n");
            }
        }
        return sb.toString();
    }

    private String fileOverview() {
        StringBuilder sb = new StringBuilder();
        for (String n : editableFiles()) {
            String code = new String(archive.get(n), StandardCharsets.UTF_8);
            int lines = 1;
            for (int i = 0; i < code.length(); i++) if (code.charAt(i) == '\n') lines++;
            sb.append("- ").append(n).append(" (").append(lines).append(" dòng)\n");
            if (sb.length() > 60000) { sb.append("[OVERVIEW_TRUNCATED]\n"); break; }
        }
        return sb.toString();
    }

    private String teamNames() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < agents.size(); i++) sb.append(i > 0 ? ", " : "").append(agents.get(i).name);
        return sb.toString();
    }

    private boolean hasUserGoal() {
        synchronized (transcript) {
            for (String[] m : transcript) if (m[0].equals("Bạn")) return true;
        }
        return false;
    }

    private void startMeeting() {
        if (busy) return;
        if (!hasUserGoal()) { toast("Hãy nhắn mục tiêu / yêu cầu sửa vào nhóm trước"); return; }
        if (editableFiles().isEmpty()) { toast("ZIP không có file nào sửa được"); return; }
        if (agents.size() != 4) { toast("Cần đúng 4 model worker"); return; }
        if (centralUrl().trim().isEmpty() || centralKey().trim().isEmpty() || centralModel().trim().isEmpty()) {
            toast("AI trung tâm chưa được cấu hình"); return;
        }
        Set<String> workerModels = new HashSet<>();
        for (Agent a : agents) {
            String miss = missingConfig(a);
            if (miss != null) { toast(miss); return; }
            String modelKey = a.provider + "|" + modelOf(a);
            if (!workerModels.add(modelKey)) { toast("4 worker phải dùng model khác nhau: trùng " + modelOf(a)); return; }
        }
        String centralKeyId = cfg.centralProvider() + "|" + cfg.centralModel();
        if (workerModels.contains(centralKeyId)) { toast("AI Trung tâm không được dùng cùng provider/model với worker"); return; }
        busy = true;
        cancelled = false;
        planned = false;
        plan.clear();
        refreshUi();
        io.execute(() -> {
            try {
                runMeeting();
            } catch (Exception e) {
                post("Hệ thống", "Họp dừng giữa chừng: " + e.getMessage());
            }
            synchronized (status) { status.clear(); }
            busy = false;
            refreshUi();
        });
    }

    private void runMeeting() throws Exception {
        String files = fileOverview();
        String team = teamNames();
        Agent judge = centralJudgeAgent();

        String sys = AiWorkflow.PLANNER + "\n\nBạn là AI TRUNG TÂM / GIÁM KHẢO độc lập. Bạn KHÔNG phải worker và không sửa code. "
                + "Nhiệm vụ duy nhất ở bước này: đọc đề bài, toàn bộ danh sách file và lập kế hoạch cho đúng 4 worker. "
                + "Phân công phải bao phủ toàn bộ file cần sửa, mỗi file đúng một worker, nhóm các file phụ thuộc gần nhau, "
                + "ghi rõ quy ước API/class/method/signature phải giữ thống nhất. Trả lời bằng tiếng Việt. "
                + "Chỉ trả JSON: {\"assignments\":{\"<worker>\":[\"<file>\"]},\"notes\":\"...\"}. "
                + "Worker hợp lệ: " + team;
        String userMsg = "ĐỀ BÀI:\n" + userGoal() + "\n\nCÁC FILE:\n" + files;
        String json = askCentral(judge, new JSONArray().put(msg("system", sys)).put(msg("user", userMsg)), "AI trung tâm đang lập kế hoạch…");
        parsePlan(json);

        StringBuilder sb = new StringBuilder("📋 Phân công đã chốt:\n");
        int total = 0;
        for (Agent a : agents) {
            List<String> fs = plan.get(a.name);
            int n = fs == null ? 0 : fs.size();
            total += n;
            sb.append("• ").append(a.name).append(": ");
            if (n == 0) sb.append("(không có file)");
            else for (int i = 0; i < n; i++) sb.append(i > 0 ? ", " : "").append(shortName(fs.get(i)));
            sb.append('\n');
        }
        if (!planNotes.isEmpty()) sb.append("\nQuy ước chung: ").append(planNotes);
        post("AI Trung tâm", sb.toString().trim());
        planned = total > 0;
        post("Hệ thống", planned ? "Kế hoạch đã được AI trung tâm duyệt. Bấm \"⚡ Cùng sửa\" để 4 worker thực hiện." : "Không có file nào được giao. Hãy nhắn rõ hơn rồi họp lại.");
    }

    private static String shortName(String path) {
        int i = path.lastIndexOf('/');
        return i < 0 ? path : path.substring(i + 1);
    }

    private static String clean(String raw) {
        return raw.replaceAll("(?s)<think>.*?</think>", "").trim();
    }

    /** Đọc JSON phân công. Kế hoạch không hợp lệ => DỪNG, không tự ý đổi kế hoạch của giám khảo. */
    private void parsePlan(String raw) throws Exception {
        plan.clear();
        planNotes = "";
        List<String> valid = editableFiles();
        String t = clean(raw);
        int s = t.indexOf('{'), e = t.lastIndexOf('}');
        if (s < 0 || e <= s) throw new Exception("AI trung tâm không trả JSON kế hoạch hợp lệ");
        JSONObject j = new JSONObject(t.substring(s, e + 1));
        planNotes = j.optString("notes", "").trim();
        JSONObject as = j.optJSONObject("assignments");
        if (as == null) throw new Exception("Kế hoạch thiếu assignments");
        Set<String> used = new HashSet<>();
        for (String who : agents.stream().map(a -> a.name).toArray(String[]::new)) plan.put(who, new ArrayList<>());
        java.util.Iterator<String> it = as.keys();
        while (it.hasNext()) {
            String who = it.next();
            Agent owner = null;
            for (Agent a : agents) if (a.name.equalsIgnoreCase(who.trim())) { owner = a; break; }
            if (owner == null) throw new Exception("Kế hoạch giao cho worker không tồn tại: " + who);
            JSONArray arr = as.optJSONArray(who);
            if (arr == null) throw new Exception("Danh sách file của " + who + " không hợp lệ");
            for (int i=0;i<arr.length();i++) {
                String f = arr.optString(i, "");
                if (!valid.contains(f)) throw new Exception("Kế hoạch chứa file không hợp lệ: " + f);
                if (!used.add(f)) throw new Exception("File bị giao cho nhiều worker: " + f);
                plan.get(owner.name).add(f);
            }
        }
        if (used.size() != valid.size()) {
            List<String> missing = new ArrayList<>();
            for (String f : valid) if (!used.contains(f)) missing.add(f);
            throw new Exception("Kế hoạch chưa bao phủ: " + missing);
        }
    }

    // =============================================================== cùng sửa (song song)

    private void startWork() {
        if (busy || !planned) return;
        busy = true;
        cancelled = false;
        edited = true;
        changes.clear();
        approvedSnapshot.clear();
        workerFileStatus.clear();
        workerFailures.clear();
        refreshUi();
        integrationApproved = false;
        integrationReport = "";
        post("Hệ thống", "⚡ Cả đội bắt đầu sửa song song…");
        final String goal = userGoal();
        final String notes = planNotes;
        final String ownerMap = ownerMapText();
        final List<Agent> workers = new ArrayList<>(agents);
        if (workers.size() != 4) { busy = false; toast("Cần đúng 4 worker"); refreshUi(); return; }
        final CountDownLatch latch = new CountDownLatch(workers.size());
        for (final Agent a : workers) {
            io.execute(() -> {
                try {
                    workAgent(a, plan.get(a.name), goal, notes, ownerMap);
                } catch (Exception e) {
                    post(a.name, "⚠ Dừng giữa chừng: " + e.getMessage());
                } finally {
                    setStatus(a.name, null);
                    latch.countDown();
                }
            });
        }
        io.execute(() -> {
            try { latch.await(); } catch (InterruptedException ignored) { }
            String lead = "AI Trung tâm";
            int want = 0;
            for (List<String> l : plan.values()) want += l.size();
            if (cancelled) {
                post(lead, "Đã dừng. Có " + changes.size() + "/" + want + " file được sửa xong.");
            } else {
                if (!workerFailures.isEmpty() || changes.size() < want) {
                    integrationApproved = false;
                    integrationReport = "Worker thất bại/bỏ qua file: " + workerFailures + "; kết quả " + changes.size() + "/" + want + ".";
                    post(lead, "🔴 Có worker không hoàn thành đầy đủ; chưa cho phép AI trung tâm duyệt.");
                } else {
                    post(lead, "✅ 4 worker đã hoàn thành: " + changes.size() + "/" + want + " file. Chuyển sang AI trung tâm kiểm tra…");
                    try {
                        integrationApproved = runCentralIntegrationReview();
                        for (int round = 1; !integrationApproved && round <= MAX_REPAIR_ROUNDS && !cancelled; round++) {
                            post("AI Trung tâm", "🔧 REJECTED → bắt đầu vòng sửa " + round + "/" + MAX_REPAIR_ROUNDS + " theo lỗi review.");
                            if (!runRepairRound(round, goal, notes, ownerMap)) break;
                            integrationApproved = runCentralIntegrationReview();
                        }
                    } catch (Exception e) {
                        integrationApproved = false;
                        integrationReport = "Không thể kiểm tra tích hợp: " + e.getMessage();
                        post("Hệ thống", "⚠ " + integrationReport + " — chưa cho phép xuất ZIP.");
                    }
                }
                if (integrationApproved) {
                    post(lead, "🟢 AI TRUNG TÂM: APPROVED — bản ghép đã qua kiểm tra. Có thể xuất ZIP.");
                } else {
                    post(lead, "🔴 AI TRUNG TÂM: REJECTED — bản ghép chưa đạt. Không cho xuất ZIP.\n" + integrationReport);
                }
            }
            busy = false;
            refreshUi();
        });
        refreshUi();
    }

    private boolean runRepairRound(int round, String goal, String notes, String ownerMap) throws Exception {
        Agent judge = centralJudgeAgent();
        String sys = AiWorkflow.REPAIR + "\nBạn là AI Trung tâm. Từ báo cáo REJECT hiện tại, hãy chọn file cần sửa và worker chịu trách nhiệm. Chỉ trả JSON {\"repairs\":[{\"worker\":\"tên\",\"file\":\"path\",\"instruction\":\"lỗi cần sửa\"}]}";
        String raw = askCentral(judge, new JSONArray().put(msg("system", sys)).put(msg("user", "ĐỀ BÀI:\n" + goal + "\n\nBÁO CÁO REJECT:\n" + integrationReport + "\n\nPHÂN CÔNG:\n" + ownerMap)), "AI trung tâm lập repair round " + round + "…");
        JSONObject j = parseReview(raw); JSONArray rs = j.optJSONArray("repairs");
        if (rs == null || rs.length() == 0) return false;
        for (int i = 0; i < rs.length(); i++) {
            JSONObject r = rs.getJSONObject(i); String worker = r.optString("worker").trim(); String file = r.optString("file").trim(); String instruction = r.optString("instruction").trim();
            Agent owner = null; for (Agent a : agents) if (a.name.equalsIgnoreCase(worker)) { owner = a; break; }
            if (owner == null || !planContains(file) || instruction.isEmpty()) throw new Exception("Repair plan không hợp lệ: " + worker + " -> " + file);
            byte[] current = changes.get(file); if (current == null) throw new Exception("Repair thiếu file hiện tại: " + file);
            String currentCode = new String(current, StandardCharsets.UTF_8);
            String repaired = editWholeFile(owner, goal, notes + "\nREPAIR ROUND " + round + ": " + instruction, ownerMap, file, currentCode, "repair " + shortName(file));
            changes.put(file, repaired.getBytes(StandardCharsets.UTF_8));
            workerFileStatus.put(owner.name + "::" + file, "MODIFIED");
        }
        return true;
    }

    private String userGoal() {
        StringBuilder sb = new StringBuilder();
        synchronized (transcript) {
            for (String[] m : transcript) if (m[0].equals("Bạn")) sb.append(m[1]).append('\n');
        }
        return sb.toString().trim();
    }

    private String ownerMapText() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, List<String>> e : plan.entrySet()) {
            for (String f : e.getValue()) sb.append("- ").append(f).append(" -> ").append(e.getKey()).append('\n');
        }
        return sb.toString();
    }

    private void workAgent(Agent a, List<String> files, String goal, String notes, String ownerMap) throws Exception {
        post(a.name, "Nhận " + files.size() + " file, bắt đầu làm.");
        int done = 0;
        for (int i = 0; i < files.size(); i++) {
            if (cancelled) return;
            String name = files.get(i);
            byte[] originalBytes = archive.get(name);
            if (originalBytes == null) throw new Exception("Không tìm thấy file được giao: " + name);
            String code = new String(originalBytes, StandardCharsets.UTF_8);
            workerFileStatus.put(a.name + "::" + name, "RUNNING");
            try {
                String result = editWholeFile(a, goal, notes, ownerMap, name, code,
                        "(" + (i + 1) + "/" + files.size() + ") " + shortName(name));
                if (result == null || result.trim().isEmpty()) throw new Exception("AI trả file rỗng");
                boolean same = result.equals(code);
                changes.put(name, result.getBytes(StandardCharsets.UTF_8));
                workerFileStatus.put(a.name + "::" + name, same ? "UNCHANGED" : "MODIFIED");
                done++;
                post(a.name, (same ? "↔ Không cần đổi " : "✔ Đã sửa ") + shortName(name));
            } catch (Exception ex) {
                workerFileStatus.put(a.name + "::" + name, "FAILED");
                workerFailures.add(a.name + " -> " + name);
                post(a.name, "⚠ Thất bại " + shortName(name) + ": " + (ex.getMessage() == null ? ex.toString() : ex.getMessage()));
            }
            refreshUi();
        }
        post(a.name, "Xong phần của mình: " + done + "/" + files.size() + " file.");
    }

    private String editWholeFile(Agent a, String goal, String notes, String ownerMap, String name, String code, String label) throws Exception {
        String ctx = "Mục tiêu chung của dự án (từ người dùng):\n" + goal
                + (notes.isEmpty() ? "" : "\nQuy ước chung cả đội đã thống nhất:\n" + notes)
                + "\nPhân công (file -> người sửa):\n" + ownerMap;
        String sys = AiWorkflow.WORKER + "\n\nYou are " + a.name + ", a member of an AI team editing one project in parallel. "
                + (a.role == null || a.role.trim().isEmpty() ? "" : "Your specialty: " + a.role.trim() + ". ")
                + "Edit ONLY the assigned file. Return ONLY the complete new file content, with no explanation and no markdown fences. "
                + "Preserve package, imports, public APIs and compatibility unless the task explicitly requires changing them. "
                + "Do not invent dependencies or files. If no change is needed, return the original file unchanged.";
        String user = ctx + "\n\nFile: " + name + "\nComplete file content:\n" + code;
        return normalizeWholeFile(ask(a, new JSONArray().put(msg("system", sys)).put(msg("user", user)), "đang sửa " + label), code);
    }

    private static String normalizeWholeFile(String raw, String original) throws Exception {
        String t = cleanOut(raw);
        if (t.isEmpty()) throw new Exception("AI trả nội dung rỗng");
        if (!balanced(t.getBytes(StandardCharsets.UTF_8))) throw new Exception("AI trả file có cấu trúc ngoặc không cân bằng");
        return t;
    }

    // =============================================================== AI trung tâm: giám khảo tích hợp

    /**
     * AI trung tâm KHÔNG sửa song song. Nó chỉ được chạy sau khi tất cả worker hoàn thành.
     * Nhiệm vụ: đối chiếu đề bài + kế hoạch + bản ghép, tìm xung đột và quyết định có cho xuất ZIP hay không.
     */
    private boolean runCentralIntegrationReview() throws Exception {
        Agent judge = centralJudgeAgent();
        String goal = userGoal();
        String local = localIntegrationAudit();
        if (!local.startsWith("PASS")) { integrationReport = local; return false; }

        StringBuilder fileReports = new StringBuilder();
        for (String name : editableFiles()) {
            byte[] beforeBytes = archive.get(name);
            byte[] afterBytes = changes.get(name);
            if (afterBytes == null) { integrationReport = "FAIL: thiếu kết quả worker cho " + name; return false; }
            String before = new String(beforeBytes, StandardCharsets.UTF_8);
            String after = new String(afterBytes, StandardCharsets.UTF_8);
            String sys = AiWorkflow.REVIEW + "\n\nBạn là AI TRUNG TÂM / GIÁM KHẢO. Kiểm tra MỘT FILE hoàn chỉnh trong project. "
                    + "Đối chiếu đề bài, file trước/sau và phân công. Không sửa code. "
                    + "Kiểm tra đúng mục đích, không làm mất code, syntax cơ bản, package/import/API và tính tương thích. "
                    + "Ngoài issues, hãy trích ra contracts gồm package, imports, public/protected class/interface, method và signature quan trọng "
                    + "để dùng cho kiểm tra liên file; contracts phải ngắn gọn, tối đa khoảng 1200 ký tự. Chỉ trả JSON "
                    + "{\"approved\":true/false,\"issues\":[\"...\"],\"contracts\":\"...\"}.";
            String user = "ĐỀ BÀI:\n" + goal + "\n\nFILE:\n" + name + "\n\nBEFORE:\n" + before + "\n\nAFTER:\n" + after;
            String raw = askCentral(judge, new JSONArray().put(msg("system", sys)).put(msg("user", user)), "AI trung tâm kiểm tra " + shortName(name) + "…");
            JSONObject r = parseReview(raw);
            if (!r.optBoolean("approved", false)) {
                fileReports.append("FAIL ").append(name).append(": ").append(r.optJSONArray("issues")).append("\n");
            } else {
                fileReports.append("PASS ").append(name).append("\n");
            }
            fileReports.append("CONTRACTS ").append(name).append(": ").append(r.optString("contracts", "")).append("\n");
        }

        String crossSys = AiWorkflow.REVIEW + "\n\nBạn là AI TRUNG TÂM / GIÁM KHẢO kiểm tra dependency liên file. Không sửa code. "
                + "Đối chiếu contracts của các file sau với nhau và với đề bài. Tìm class/method/signature/import/package/API mâu thuẫn, "
                + "tham chiếu không tồn tại, tên đã đổi nhưng nơi gọi chưa đổi và xung đột giữa 4 worker. "
                + "Chỉ trả JSON {\"approved\":true/false,\"issues\":[\"...\"]}.";
        String crossUser = "ĐỀ BÀI:\n" + goal + "\n\nCONTRACTS VÀ FILE REPORTS:\n" + fileReports;
        JSONObject crossReview = parseReview(askCentral(judge, new JSONArray().put(msg("system", crossSys)).put(msg("user", crossUser)), "AI trung tâm kiểm tra dependency liên file…"));
        if (!crossReview.optBoolean("approved", false)) {
            fileReports.append("CROSS-FILE FAIL: ").append(crossReview.optJSONArray("issues")).append("\n");
        }

        String sys2 = AiWorkflow.REVIEW + "\n\nBạn là AI TRUNG TÂM / GIÁM KHẢO cuối. Bạn không sửa code. "
                + "Dựa trên đề bài, kế hoạch, audit cục bộ và báo cáo kiểm tra TỪNG FILE, hãy kiểm tra tính nhất quán toàn project. "
                + "Nếu có bất kỳ file nào FAIL hoặc có dấu hiệu dependency/API giữa các file không khớp thì REJECT. "
                + "Chỉ trả JSON {\"approved\":true/false,\"score\":0-100,\"issues\":[\"...\"]}.";
        String user2 = "ĐỀ BÀI:\n" + goal + "\n\nKẾ HOẠCH:\n" + ownerMapText() + "\n" + planNotes
                + "\n\nLOCAL AUDIT:\n" + local + "\n\nFILE REPORTS:\n" + fileReports;
        String raw2 = askCentral(judge, new JSONArray().put(msg("system", sys2)).put(msg("user", user2)), "AI trung tâm kiểm tra tích hợp toàn project…");
        JSONObject finalReview = parseReview(raw2);
        StringBuilder report = new StringBuilder("Điểm: ").append(finalReview.optInt("score", 0)).append("/100\n").append(fileReports);
        JSONArray issues = finalReview.optJSONArray("issues");
        if (issues != null) for (int i=0;i<issues.length();i++) report.append("• ").append(issues.optString(i)).append("\n");
        integrationReport = report.toString().trim();
        boolean ok = finalReview.optBoolean("approved", false) && finalReview.optInt("score", 0) >= 80 && !fileReports.toString().contains("FAIL ") && !fileReports.toString().contains("CROSS-FILE FAIL");
        if (ok) {
            approvedSnapshot.clear(); approvedManifest.clear();
            for (Map.Entry<String, byte[]> e : archive.entrySet()) {
                byte[] approved = changes.containsKey(e.getKey()) ? changes.get(e.getKey()) : e.getValue();
                if (approved == null || approved.length == 0) throw new Exception("Snapshot APPROVED chứa file rỗng: " + e.getKey());
                approvedSnapshot.put(e.getKey(), approved.clone());
                approvedManifest.put(e.getKey(), sha256(approved));
            }
            if (approvedSnapshot.size() != archive.size()) throw new Exception("Snapshot APPROVED không đủ toàn project");
        }
        return ok;
    }

    private static JSONObject parseReview(String raw) throws Exception {
        String t = clean(raw);
        int a=t.indexOf('{'), b=t.lastIndexOf('}');
        if (a<0 || b<=a) throw new Exception("AI trung tâm trả JSON không hợp lệ");
        return new JSONObject(t.substring(a,b+1));
    }

    /** Kiểm tra rẻ tại máy trước khi tốn thêm token của AI trung tâm. */
    private String localIntegrationAudit() {
        StringBuilder problems = new StringBuilder();
        int expected = 0;
        for (Map.Entry<String, List<String>> e : plan.entrySet()) {
            for (String f : e.getValue()) {
                expected++;
                String st = workerFileStatus.get(e.getKey() + "::" + f);
                if (!"MODIFIED".equals(st) && !"UNCHANGED".equals(st)) problems.append("worker incomplete: ").append(e.getKey()).append(" -> ").append(f).append(" status=").append(st).append("\n");
                if (!changes.containsKey(f)) problems.append("missing worker result: ").append(f).append("\n");
                if (!archive.containsKey(f)) problems.append("missing source: ").append(f).append("\n");
            }
        }
        for (String f : changes.keySet()) if (!planContains(f)) problems.append("unassigned changed file: ").append(f).append("\n");
        for (Map.Entry<String, byte[]> e : changes.entrySet()) {
            if (e.getValue() == null || e.getValue().length == 0) problems.append("empty file: ").append(e.getKey()).append("\n");
            String path = e.getKey();
            String afterCode = new String(e.getValue(), StandardCharsets.UTF_8);
            if ((path.endsWith(".java") || path.endsWith(".kt") || path.endsWith(".js") || path.endsWith(".ts") || path.endsWith(".json")) && !balanced(e.getValue())) problems.append("unbalanced syntax: ").append(path).append("\n");
            if (path.endsWith(".java") || path.endsWith(".kt")) {
                String beforeCode = archive.containsKey(path) ? new String(archive.get(path), StandardCharsets.UTF_8) : "";
                String beforeApi = apiFingerprint(beforeCode), afterApi = apiFingerprint(afterCode);
                for (String sig : beforeApi.split("\\n")) if (!sig.isEmpty() && !afterApi.contains(sig)) problems.append("public/protected API removed or changed: ").append(path).append(" -> ").append(sig).append("\n");
                String expectedPkg = packageName(beforeCode), actualPkg = packageName(afterCode);
                if (!expectedPkg.equals(actualPkg)) problems.append("package changed unexpectedly: ").append(path).append(" (" ).append(expectedPkg).append(" -> ").append(actualPkg).append(")\n");
            }
            if (path.endsWith(".xml")) {
                try {
                    javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new java.io.ByteArrayInputStream(e.getValue()));
                } catch (Exception ex) { problems.append("invalid XML: ").append(path).append("\n"); }
            }
            if (path.endsWith(".json")) {
                try { new JSONObject(afterCode); } catch (Exception ex) { try { new JSONArray(afterCode); } catch (Exception ignored) { problems.append("invalid JSON: ").append(path).append("\n"); } }
            }
        }
        if (expected == 0) return "FAIL: kế hoạch không có file";
        return problems.length() == 0 ? "PASS: local audit OK, expected=" + expected : "FAIL:\n" + problems;
    }

    private boolean planContains(String file) {
        for (List<String> fs : plan.values()) if (fs.contains(file)) return true;
        return false;
    }

    private static String apiFingerprint(String code) {
        StringBuilder out = new StringBuilder();
        Pattern p = Pattern.compile("(?m)\\b(public|protected)\\s+(?:static\\s+|final\\s+|abstract\\s+|suspend\\s+|override\\s+)*[\\w<>\\[\\],.? ]+\\s+[A-Za-z_][A-Za-z0-9_]*\\s*\\([^;{]*\\)|\\b(public|protected)\\s+(?:class|interface|enum|object)\\s+[A-Za-z_][A-Za-z0-9_]*");
        Matcher m = p.matcher(code);
        while (m.find()) out.append(m.group().replaceAll("\\s+", " ").trim()).append('\n');
        return out.toString();
    }

    private static String packageName(String code) {
        Matcher m = Pattern.compile("(?m)^\\s*package\\s+([A-Za-z_][A-Za-z0-9_.]*)\\s*;?").matcher(code);
        return m.find() ? m.group(1) : "";
    }

    private static boolean balanced(byte[] data) {
        String s = new String(data, StandardCharsets.UTF_8);
        int braces = 0, parens = 0, brackets = 0;
        boolean str = false, esc = false, line = false, block = false;
        char quote = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i), n = i + 1 < s.length() ? s.charAt(i + 1) : 0;
            if (line) { if (c == '\n') line = false; continue; }
            if (block) { if (c == '*' && n == '/') { block = false; i++; } continue; }
            if (!str && c == '/' && n == '/') { line = true; i++; continue; }
            if (!str && c == '/' && n == '*') { block = true; i++; continue; }
            if (str) { if (esc) esc = false; else if (c == '\\') esc = true; else if (c == quote) str = false; continue; }
            if (c == '"' || c == '\'') { str = true; quote = c; continue; }
            if (c == '{') braces++; else if (c == '}') braces--; else if (c == '(') parens++; else if (c == ')') parens--; else if (c == '[') brackets++; else if (c == ']') brackets--;
            if (braces < 0 || parens < 0 || brackets < 0) return false;
        }
        return !str && !block && braces == 0 && parens == 0 && brackets == 0;
    }

    // =============================================================== xử lý nội dung (giống chế độ Sửa ZIP)

    private static String cleanOut(String raw) {
        String t = raw.replaceAll("(?s)<think>.*?</think>", "").trim();
        if (t.startsWith("```")) {
            int nl = t.indexOf('\n');
            if (nl > 0) t = t.substring(nl + 1);
            if (t.endsWith("```")) t = t.substring(0, t.length() - 3);
        }
        return t;
    }


}
