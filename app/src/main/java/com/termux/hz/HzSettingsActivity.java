package com.termux.hz;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/** API keys, agent permissions, build options and Java management. */
public class HzSettingsActivity extends AppCompatActivity {

    private LinearLayout col;
    private final TextView[] javaStatus = new TextView[Hz.JAVA_VERSIONS.length];

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(Ui.BG);
        sv.setFitsSystemWindows(true);
        col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(0, 0, 0, Ui.dp(this, 32));
        sv.addView(col);
        setContentView(sv);

        TextView t = Ui.text(this, "Settings", 24, Ui.TEXT);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setPadding(Ui.dp(this, 16), Ui.dp(this, 20), Ui.dp(this, 16), Ui.dp(this, 4));
        col.addView(t);

        col.addView(Ui.label(this, "AI agent"));
        field("Groq API key", Hz.K_GROQ_KEY, "gsk_…  (console.groq.com/keys)", true);
        field("OpenRouter API key", Hz.K_OR_KEY, "sk-or-…  (openrouter.ai/keys)", true);
        field("Groq model id", Hz.K_GROQ_MODEL, LlmClient.Provider.GROQ.defaultModel, false);
        field("OpenRouter model id", Hz.K_OR_MODEL, LlmClient.Provider.OPENROUTER.defaultModel, false);
        row("Agent permissions", approvalText(), v -> {
            String[] o = {"Ask before every change", "Auto-approve file edits, ask for commands & builds", "Full auto (no questions)"};
            new MaterialAlertDialogBuilder(this).setTitle("Agent permissions").setItems(o, (d, w) -> {
                Hz.prefs(this).edit().putInt(Hz.K_APPROVAL, w).apply();
                v.setText(approvalText());
            }).show();
        });
        note("Keys are stored only on this device. Pick any model from the Agent tab; models that support tool calling can edit files.");

        col.addView(Ui.label(this, "Build"));
        field("Gradle task", Hz.K_BUILD_TASK, "build", false);
        field("Extra build arguments", Hz.K_BUILD_ARGS, "e.g. --offline  -x test", false);
        row("Memory for builds", Hz.prefs(this).getInt(Hz.K_XMX, 1536) + " MB", v -> {
            String[] o = {"768 MB", "1024 MB", "1536 MB", "2048 MB", "3072 MB"};
            int[] val = {768, 1024, 1536, 2048, 3072};
            new MaterialAlertDialogBuilder(this).setTitle("Max heap").setItems(o, (d, w) -> {
                Hz.prefs(this).edit().putInt(Hz.K_XMX, val[w]).apply();
                v.setText(o[w]);
            }).show();
        });
        note("Finished jars are copied to Downloads/HZ.");

        col.addView(Ui.label(this, "Java"));
        for (int i = 0; i < Hz.JAVA_VERSIONS.length; i++) {
            final int v = Hz.JAVA_VERSIONS[i];
            final int idx = i;
            javaStatus[idx] = row("Java " + v, JdkManager.versionLabel(v), tv ->
                    startActivity(new Intent(this, SetupActivity.class).putExtra(SetupActivity.EXTRA_ONLY, new int[]{v})));
            field("Custom JDK URL for Java " + v, Hz.K_JDK_URL_PREFIX + v, "https://…/jdk" + v + "-aarch64.tar.xz (optional)", false);
        }
        note("Tap a Java row to install it. Java 17 and 21 come from the Termux repository. If Java 8 or 11 is not available there, paste a download URL for an Android-compatible JDK archive (.tar.gz, .tar.xz or .zip) above, then tap the row again.");
    }

    @Override protected void onResume() {
        super.onResume();
        for (int i = 0; i < Hz.JAVA_VERSIONS.length; i++)
            if (javaStatus[i] != null) javaStatus[i].setText(JdkManager.versionLabel(Hz.JAVA_VERSIONS[i]));
    }

    private String approvalText() {
        int m = Hz.prefs(this).getInt(Hz.K_APPROVAL, 1);
        return m == 0 ? "Ask every time" : m == 1 ? "Auto edits, ask commands" : "Full auto";
    }

    /** Title on the left, tappable value on the right. Returns the value view. */
    private TextView row(String title, String value, java.util.function.Consumer<TextView> click) {
        LinearLayout r = new LinearLayout(this);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(Ui.dp(this, 16), Ui.dp(this, 14), Ui.dp(this, 16), Ui.dp(this, 14));
        TextView a = Ui.text(this, title, 15, Ui.TEXT);
        TextView v = Ui.text(this, value, 13, Ui.ACCENT);
        v.setGravity(Gravity.END);
        r.addView(a, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        r.addView(v, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        r.setBackground(Ui.ripple(Ui.round(0x00000000, 0, this), this));
        r.setOnClickListener(x -> click.accept(v));
        col.addView(r);
        return v;
    }

    private void field(String title, String key, String hint, boolean secret) {
        TextView l = Ui.text(this, title, 13, Ui.MUTED);
        l.setPadding(Ui.dp(this, 16), Ui.dp(this, 10), Ui.dp(this, 16), Ui.dp(this, 4));
        col.addView(l);
        EditText e = Ui.input(this, hint);
        e.setSingleLine(true);
        e.setText(Hz.prefs(this).getString(key, ""));
        e.setInputType(secret ? InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD
                : InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        e.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            public void onTextChanged(CharSequence s, int a, int b, int c) {}
            public void afterTextChanged(Editable s) {
                Hz.prefs(HzSettingsActivity.this).edit().putString(key, s.toString().trim()).apply();
            }
        });
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.leftMargin = p.rightMargin = Ui.dp(this, 16);
        col.addView(e, p);
    }

    private void note(String s) {
        TextView n = Ui.text(this, s, 12, Ui.MUTED);
        n.setPadding(Ui.dp(this, 16), Ui.dp(this, 8), Ui.dp(this, 16), Ui.dp(this, 4));
        col.addView(n);
    }
}
