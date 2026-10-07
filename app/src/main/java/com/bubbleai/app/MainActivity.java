package com.bubbleai.app;

import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    private static final String URL_ANTHROPIC = "https://api.anthropic.com/v1/messages";
    private static final String URL_RESPONSES = "https://api.openai.com/v1/responses";
    private static final String URL_CHAT = "https://api.openai.com/v1/chat/completions";

    private Prefs prefs;

    private View pageHome, pageAccount, pageSettings;
    private TextView navHome, navAccount, navSettings;

    // beranda
    private TextView tvGreeting, tvAvatarHome, tvBubbleStatus, tvA11yHint, tvTestResult, tvLastChat;
    private TextView tvStatSent, tvStatRecv, tvStatErr;
    private View dotStatus;
    private SwitchCompat switchBubble;
    private Button btnEnableA11y, btnAppInfo, btnTest, btnClearHistory;
    private boolean updatingSwitch = false;

    // akun
    private TextView tvAvatarBig, tvAccName, tvAccProvider, tvAccModel, tvAccKey;
    private EditText etName;

    // pengaturan
    private EditText etApiKey, etApiUrl, etModel, etSystem;
    private TextView tvToggleKey, chipAnthropic, chipResponses, chipChat, sideLeft, sideRight;
    private String side = "right";
    private boolean keyVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        prefs = new Prefs(this);
        bindViews();
        setupListeners();
        loadSettingsFields();
        selectTab(0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshAll();
    }

    private void bindViews() {
        pageHome = findViewById(R.id.pageHome);
        pageAccount = findViewById(R.id.pageAccount);
        pageSettings = findViewById(R.id.pageSettings);
        navHome = findViewById(R.id.navHome);
        navAccount = findViewById(R.id.navAccount);
        navSettings = findViewById(R.id.navSettings);

        tvGreeting = findViewById(R.id.tvGreeting);
        tvAvatarHome = findViewById(R.id.tvAvatarHome);
        tvBubbleStatus = findViewById(R.id.tvBubbleStatus);
        tvA11yHint = findViewById(R.id.tvA11yHint);
        tvTestResult = findViewById(R.id.tvTestResult);
        tvLastChat = findViewById(R.id.tvLastChat);
        tvStatSent = findViewById(R.id.tvStatSent);
        tvStatRecv = findViewById(R.id.tvStatRecv);
        tvStatErr = findViewById(R.id.tvStatErr);
        dotStatus = findViewById(R.id.dotStatus);
        switchBubble = findViewById(R.id.switchBubble);
        btnEnableA11y = findViewById(R.id.btnEnableA11y);
        btnAppInfo = findViewById(R.id.btnAppInfo);
        btnTest = findViewById(R.id.btnTest);
        btnClearHistory = findViewById(R.id.btnClearHistory);

        tvAvatarBig = findViewById(R.id.tvAvatarBig);
        tvAccName = findViewById(R.id.tvAccName);
        tvAccProvider = findViewById(R.id.tvAccProvider);
        tvAccModel = findViewById(R.id.tvAccModel);
        tvAccKey = findViewById(R.id.tvAccKey);
        etName = findViewById(R.id.etName);

        etApiKey = findViewById(R.id.etApiKey);
        etApiUrl = findViewById(R.id.etApiUrl);
        etModel = findViewById(R.id.etModel);
        etSystem = findViewById(R.id.etSystem);
        tvToggleKey = findViewById(R.id.tvToggleKey);
        chipAnthropic = findViewById(R.id.chipAnthropic);
        chipResponses = findViewById(R.id.chipResponses);
        chipChat = findViewById(R.id.chipChat);
        sideLeft = findViewById(R.id.sideLeft);
        sideRight = findViewById(R.id.sideRight);
    }

    private void setupListeners() {
        navHome.setOnClickListener(v -> selectTab(0));
        navAccount.setOnClickListener(v -> selectTab(1));
        navSettings.setOnClickListener(v -> selectTab(2));

        // --- beranda ---
        switchBubble.setOnCheckedChangeListener((b, checked) -> {
            if (updatingSwitch) return;
            if (checked) {
                prefs.setBubbleOn(true);
                if (!isA11yEnabled()) {
                    Toast.makeText(this, "Aktifkan layanan \"Bubble AI\" di Aksesibilitas", Toast.LENGTH_LONG).show();
                    openA11ySettings();
                } else if (BubbleAccessibilityService.instance != null) {
                    BubbleAccessibilityService.instance.showBubble();
                }
            } else {
                prefs.setBubbleOn(false);
                if (BubbleAccessibilityService.instance != null) {
                    BubbleAccessibilityService.instance.hideBubble();
                }
            }
            refreshHome();
        });
        btnEnableA11y.setOnClickListener(v -> openA11ySettings());
        btnAppInfo.setOnClickListener(v -> startActivity(new Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName()))));
        btnTest.setOnClickListener(v -> testConnection());
        btnClearHistory.setOnClickListener(v -> {
            prefs.clearHistory();
            Toast.makeText(this, "Riwayat chat dihapus", Toast.LENGTH_SHORT).show();
            refreshHome();
        });

        // --- akun ---
        findViewById(R.id.btnSaveName).setOnClickListener(v -> {
            prefs.setName(etName.getText().toString());
            hideKeyboard();
            Toast.makeText(this, "Nama disimpan", Toast.LENGTH_SHORT).show();
            refreshAll();
        });
        findViewById(R.id.btnRemoveKey).setOnClickListener(v -> {
            prefs.clearApiKey();
            etApiKey.setText("");
            Toast.makeText(this, "API key dihapus", Toast.LENGTH_SHORT).show();
            refreshAll();
        });

        // --- pengaturan ---
        tvToggleKey.setOnClickListener(v -> {
            keyVisible = !keyVisible;
            etApiKey.setInputType(InputType.TYPE_CLASS_TEXT | (keyVisible
                    ? InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                    : InputType.TYPE_TEXT_VARIATION_PASSWORD));
            etApiKey.setSelection(etApiKey.getText().length());
            tvToggleKey.setText(keyVisible ? "Sembunyikan" : "Tampilkan");
        });
        chipAnthropic.setOnClickListener(v -> etApiUrl.setText(URL_ANTHROPIC));
        chipResponses.setOnClickListener(v -> etApiUrl.setText(URL_RESPONSES));
        chipChat.setOnClickListener(v -> etApiUrl.setText(URL_CHAT));
        sideLeft.setOnClickListener(v -> { side = "left"; updateSideChips(); });
        sideRight.setOnClickListener(v -> { side = "right"; updateSideChips(); });
        findViewById(R.id.btnSave).setOnClickListener(v -> {
            prefs.setConnection(
                    etApiKey.getText().toString(),
                    etApiUrl.getText().toString(),
                    etModel.getText().toString(),
                    etSystem.getText().toString(),
                    side);
            hideKeyboard();
            if (BubbleAccessibilityService.instance != null) {
                BubbleAccessibilityService.instance.refreshPosition();
            }
            Toast.makeText(this, "Pengaturan disimpan", Toast.LENGTH_SHORT).show();
            refreshAll();
        });
    }

    private void loadSettingsFields() {
        etApiKey.setText(prefs.getApiKey());
        etApiUrl.setText(prefs.getApiUrl());
        etModel.setText(prefs.getModel());
        etSystem.setText(prefs.getSystem());
        etName.setText(prefs.getName().equals("Pengguna") ? "" : prefs.getName());
        side = prefs.getSide();
        updateSideChips();
    }

    private void updateSideChips() {
        boolean left = "left".equals(side);
        sideLeft.setBackgroundResource(left ? R.drawable.bg_chip_selected : R.drawable.bg_chip);
        sideRight.setBackgroundResource(left ? R.drawable.bg_chip : R.drawable.bg_chip_selected);
    }

    // ---------------- tab ----------------

    private void selectTab(int i) {
        pageHome.setVisibility(i == 0 ? View.VISIBLE : View.GONE);
        pageAccount.setVisibility(i == 1 ? View.VISIBLE : View.GONE);
        pageSettings.setVisibility(i == 2 ? View.VISIBLE : View.GONE);
        int on = getResources().getColor(R.color.accent);
        int off = getResources().getColor(R.color.text_muted);
        navHome.setTextColor(i == 0 ? on : off);
        navAccount.setTextColor(i == 1 ? on : off);
        navSettings.setTextColor(i == 2 ? on : off);
        hideKeyboard();
        refreshAll();
    }

    // ---------------- refresh UI ----------------

    private void refreshAll() {
        refreshHome();
        refreshAccount();
    }

    private void refreshHome() {
        String name = prefs.getName();
        tvGreeting.setText("Halo, " + name);
        tvAvatarHome.setText(initial(name));

        boolean a11y = isA11yEnabled();
        boolean on = prefs.isBubbleOn() && a11y;
        updatingSwitch = true;
        switchBubble.setChecked(on);
        updatingSwitch = false;

        dotStatus.setBackgroundResource(on ? R.drawable.dot_on : R.drawable.dot_off);
        if (!a11y) {
            tvBubbleStatus.setText("Perlu aktifkan layanan Aksesibilitas");
            tvBubbleStatus.setTextColor(getResources().getColor(R.color.warning));
        } else if (on) {
            tvBubbleStatus.setText("Aktif — bubble tampil di layar");
            tvBubbleStatus.setTextColor(getResources().getColor(R.color.accent));
        } else {
            tvBubbleStatus.setText("Mati");
            tvBubbleStatus.setTextColor(getResources().getColor(R.color.text_muted));
        }
        int vis = a11y ? View.GONE : View.VISIBLE;
        tvA11yHint.setVisibility(vis);
        btnEnableA11y.setVisibility(vis);
        btnAppInfo.setVisibility(vis);

        tvStatSent.setText(String.valueOf(prefs.getSent()));
        tvStatRecv.setText(String.valueOf(prefs.getRecv()));
        tvStatErr.setText(String.valueOf(prefs.getErr()));

        JSONArray h = prefs.getHistory();
        if (h.length() == 0) {
            tvLastChat.setText("Belum ada percakapan.");
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = Math.max(0, h.length() - 2); i < h.length(); i++) {
                JSONObject m = h.optJSONObject(i);
                if (m == null) continue;
                String who = "user".equals(m.optString("role")) ? "Kamu" : "AI";
                String c = m.optString("content");
                if (c.length() > 160) c = c.substring(0, 160) + "…";
                if (sb.length() > 0) sb.append("\n\n");
                sb.append(who).append(": ").append(c);
            }
            tvLastChat.setText(sb.toString());
        }
    }

    private void refreshAccount() {
        String name = prefs.getName();
        tvAccName.setText(name);
        tvAvatarBig.setText(initial(name));
        tvAccProvider.setText(ApiClient.providerName(prefs.getApiUrl()));
        tvAccModel.setText(prefs.getModel());
        tvAccKey.setText(maskKey(prefs.getApiKey()));
    }

    private static String initial(String name) {
        return name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase();
    }

    private static String maskKey(String k) {
        if (k.isEmpty()) return "Belum diisi";
        if (k.length() <= 10) return "••••••••";
        return k.substring(0, 4) + "••••••••" + k.substring(k.length() - 4);
    }

    // ---------------- aksesibilitas ----------------

    private boolean isA11yEnabled() {
        String enabled = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (enabled == null || enabled.isEmpty()) return false;
        ComponentName cn = new ComponentName(this, BubbleAccessibilityService.class);
        String full = cn.flattenToString();
        String shortName = cn.flattenToShortString();
        for (String s : enabled.split(":")) {
            if (s.equalsIgnoreCase(full) || s.equalsIgnoreCase(shortName)) return true;
        }
        return false;
    }

    private void openA11ySettings() {
        try {
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
        } catch (Exception e) {
            Toast.makeText(this, "Tidak bisa membuka pengaturan Aksesibilitas", Toast.LENGTH_LONG).show();
        }
    }

    // ---------------- tes koneksi ----------------

    private void testConnection() {
        if (prefs.getApiKey().isEmpty()) {
            Toast.makeText(this, "Isi API key dulu di Pengaturan", Toast.LENGTH_LONG).show();
            selectTab(2);
            return;
        }
        btnTest.setEnabled(false);
        tvTestResult.setVisibility(View.VISIBLE);
        tvTestResult.setTextColor(getResources().getColor(R.color.text_muted));
        tvTestResult.setText("Menguji koneksi…");

        JSONArray h = new JSONArray();
        h.put(Prefs.msg("user", "Balas dengan satu kata: OK"));
        new ApiClient().sendMessage(prefs.getApiUrl(), prefs.getApiKey(), prefs.getModel(), "", h,
                new ApiClient.ResultCallback() {
                    @Override
                    public void onSuccess(String reply) {
                        btnTest.setEnabled(true);
                        tvTestResult.setTextColor(getResources().getColor(R.color.accent));
                        tvTestResult.setText("✓ Terhubung. Balasan: " + reply);
                    }

                    @Override
                    public void onError(String message) {
                        btnTest.setEnabled(true);
                        tvTestResult.setTextColor(getResources().getColor(R.color.danger));
                        tvTestResult.setText("✗ " + message);
                    }
                });
    }

    private void hideKeyboard() {
        View v = getCurrentFocus();
        if (v != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        }
    }
}
