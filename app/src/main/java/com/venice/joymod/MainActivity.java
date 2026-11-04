package com.venice.joymod;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private static final int ACCENT = 0xFF00E07A;

    private TextView overlayState;
    private TextView accState;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);

        ScrollView sc = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(20);
        root.setPadding(pad, dp(30), pad, dp(30));
        sc.addView(root);
        setContentView(sc);

        TextView h = new TextView(this);
        h.setText("JOYARK QUEUE SKIP");
        h.setTextSize(22f);
        h.setTypeface(Typeface.DEFAULT_BOLD);
        root.addView(h);

        TextView sub = new TextView(this);
        sub.setText("Mod menu flutuante para pular a fila do JoyArk Cloud Gaming.");
        sub.setTextSize(13f);
        sub.setPadding(0, dp(6), 0, dp(22));
        root.addView(sub);

        overlayState = new TextView(this);
        accState = new TextView(this);
        root.addView(stateLabel(overlayState));
        root.addView(btn("1. Permitir sobrepor outros apps", false, new View.OnClickListener() {
            public void onClick(View v) {
                Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivity(i);
            }
        }));
        root.addView(btn("2. Ativar o leitor de fila (acessibilidade)", false, new View.OnClickListener() {
            public void onClick(View v) {
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            }
        }));

        if (Build.VERSION.SDK_INT >= 33) {
            root.addView(btn("3. Permitir notificacoes", false, new View.OnClickListener() {
                public void onClick(View v) {
                    requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 10);
                }
            }));
        }

        root.addView(btn("INICIAR MOD MENU", true, new View.OnClickListener() {
            public void onClick(View v) { iniciar(); }
        }));
        root.addView(btn("PARAR MOD MENU", false, new View.OnClickListener() {
            public void onClick(View v) {
                stopService(new Intent(MainActivity.this, OverlayService.class));
                Toast.makeText(MainActivity.this, "Menu encerrado.", Toast.LENGTH_SHORT).show();
            }
        }));

        TextView cfg = new TextView(this);
        cfg.setText("Deep link do alvo (opcional)");
        cfg.setTextSize(12f);
        cfg.setPadding(0, dp(20), 0, dp(6));
        root.addView(cfg);

        final android.widget.EditText link = new android.widget.EditText(this);
        link.setHint("https://...");
        link.setInputType(InputType.TYPE_TEXT_VARIATION_URI);
        link.setText(Prefs.target(this));
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(dp(10));
        g.setColor(0xFF1B2430);
        g.setStroke(dp(1), 0xFF2A3644);
        link.setBackground(g);
        link.setTextSize(13f);
        link.setPadding(dp(12), dp(10), dp(12), dp(10));
        root.addView(link);

        root.addView(btn("Salvar link", false, new View.OnClickListener() {
            public void onClick(View v) {
                Prefs.setTarget(MainActivity.this, link.getText().toString().trim());
                Toast.makeText(MainActivity.this, "Link salvo.", Toast.LENGTH_SHORT).show();
            }
        }));

        TextView foot = new TextView(this);
        foot.setText("Abra o JoyArk, entre na fila, toque na bolha verde e em PULAR FILA.");
        foot.setTextSize(12f);
        foot.setPadding(0, dp(24), 0, 0);
        root.addView(foot);
    }

    private int dp(int v) {
        return Math.round(getResources().getDisplayMetrics().density * v);
    }

    private TextView stateLabel(TextView t) {
        t.setTextSize(12f);
        t.setPadding(0, dp(2), 0, dp(2));
        return t;
    }

    private Button btn(String label, boolean primary, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(14f);
        b.setTextColor(primary ? 0xFF06110B : 0xFFE6EDF5);
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(dp(12));
        g.setColor(primary ? ACCENT : 0xFF1B2430);
        b.setBackground(g);
        b.setStateListAnimator(null);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(50));
        lp.bottomMargin = dp(10);
        b.setLayoutParams(lp);
        b.setOnClickListener(l);
        return b;
    }

    private void iniciar() {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Falta a permissao de sobreposicao.", Toast.LENGTH_LONG).show();
            return;
        }
        if (ScreenReader.get() == null) {
            Toast.makeText(this, "Ative o servico de acessibilidade primeiro.", Toast.LENGTH_LONG).show();
        }
        Intent i = new Intent(this, OverlayService.class);
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(i);
        else startService(i);
        Toast.makeText(this, "Mod ativo. A bolha vai aparecer.", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        boolean ov = Settings.canDrawOverlays(this);
        overlayState.setText("Sobreposicao: " + (ov ? "OK" : "PENDENTE"));
        overlayState.setTextColor(ov ? ACCENT : 0xFFEF4444);
        boolean acc = ScreenReader.get() != null;
        accState.setText("Acessibilidade: " + (acc ? "OK" : "PENDENTE"));
        accState.setTextColor(acc ? ACCENT : 0xFFEF4444);
    }
}
