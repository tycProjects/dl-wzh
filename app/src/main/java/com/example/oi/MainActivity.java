package com.example.oi;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.*;
import android.graphics.drawable.GradientDrawable;

public class MainActivity extends Activity {
    private SharedPreferences prefs;
    private TextView status;
    private Button toggle;
    private Spinner voiceSpinner;
    private boolean enabled;

    private GradientDrawable shape(int color, int stroke) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(32);
        if (stroke != 0) d.setStroke(3, stroke);
        return d;
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("livevoz", MODE_PRIVATE);
        enabled = prefs.getBoolean("enabled", false);
        showHome();
    }

    private TextView text(String value, int size, int color) {
        TextView t = new TextView(this);
        t.setText(value); t.setTextSize(size); t.setTextColor(color);
        t.setGravity(Gravity.CENTER);
        return t;
    }

    private void showHome() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(24, 28, 24, 24);
        root.setBackgroundColor(0xFF08040F);

        TextView title = text("♛ LIVE VOZ", 32, 0xFFCA80FF);
        title.setTypeface(null, Typeface.BOLD);
        root.addView(title);
        root.addView(text("Leitor de comentários do TikTok", 15, 0xFFE9DDF5));

        TextView profile = text("Seu perfil (opcional)", 17, 0xFFFFFFFF);
        profile.setPadding(0, 26, 0, 8); root.addView(profile);
        EditText handle = new EditText(this);
        handle.setSingleLine(true);
        handle.setHint("@seuusuario");
        handle.setHintTextColor(0xFF9B8BA8);
        handle.setTextColor(0xFFFFFFFF);
        handle.setBackground(shape(0xFF1A1024, 0xFF873BCA));
        handle.setPadding(18, 8, 18, 8);
        handle.setText(prefs.getString("handle", ""));
        root.addView(handle, new LinearLayout.LayoutParams(-1, 54));
        root.addView(text("Não digite sua senha do TikTok. Este app não faz login na sua conta.", 12, 0xFFBDAFC8));

        root.addView(text("Voz da leitura", 17, 0xFFFFFFFF));
        voiceSpinner = new Spinner(this);
        String[] voices = {"Voz feminina", "Voz masculina"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, voices);
        voiceSpinner.setAdapter(adapter);
        voiceSpinner.setSelection(prefs.getInt("voice", 0));
        root.addView(voiceSpinner);

        status = text(enabled ? "● LEITURA ATIVADA" : "○ LEITURA DESATIVADA", 19,
                enabled ? 0xFF62F58B : 0xFFD8C7E8);
        status.setPadding(0, 28, 0, 12); root.addView(status);

        toggle = new Button(this);
        updateToggle();
        toggle.setOnClickListener(v -> {
            enabled = !enabled;
            prefs.edit().putBoolean("enabled", enabled)
                .putString("handle", handle.getText().toString().trim())
                .putInt("voice", voiceSpinner.getSelectedItemPosition()).apply();
            if (!enabled) {
                sendBroadcast(new Intent("com.example.oi.STOP_READING"));
            }
            updateToggle();
        });
        root.addView(toggle, new LinearLayout.LayoutParams(220, 220));

        Button access = new Button(this);
        access.setText("ATIVAR ACESSIBILIDADE");
        access.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        root.addView(access);

        TextView note = text("Ative a acessibilidade uma vez. Depois deixe o LIVE VOZ ativado e abra uma LIVE no TikTok. Para parar, volte aqui e desligue.", 13, 0xFFBDAFC8);
        note.setPadding(0, 20, 0, 0); root.addView(note);
        setContentView(root);
    }

    private void updateToggle() {
        status.setText(enabled ? "● LEITURA ATIVADA" : "○ LEITURA DESATIVADA");
        status.setTextColor(enabled ? 0xFF62F58B : 0xFFD8C7E8);
        toggle.setText(enabled ? "DESLIGAR" : "ATIVAR");
        toggle.setTextColor(0xFFFFFFFF);
        toggle.setTextSize(20);
        toggle.setBackground(shape(enabled ? 0xFF3B145A : 0xFF7515C5, 0xFFC47BFF));
    }
}
