package com.example.subwaymodmenudemo;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    private int dp(float n) { return (int)(n * getResources().getDisplayMetrics().density + 0.5f); }

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(22), dp(28), dp(22), dp(22));
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setBackgroundColor(Color.rgb(14, 18, 27));

        TextView title = new TextView(this);
        title.setText("SUBWAY SURFERS\\nMOD MENU DEMO");
        title.setTextColor(Color.rgb(255, 80, 75));
        title.setTextSize(26);
        title.setGravity(Gravity.CENTER);
        title.setTypeface(null, 1);
        root.addView(title, new LinearLayout.LayoutParams(-1, dp(100)));

        TextView note = new TextView(this);
        note.setText("Projeto demonstrativo separado. Os controles abaixo guardam preferências de demonstração; não modificam o Subway Surfers original.");
        note.setTextColor(Color.LTGRAY);
        note.setTextSize(15);
        note.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(-1, -2);
        np.setMargins(0, dp(12), 0, dp(22));
        root.addView(note, np);

        Button permission = button("1. Permitir sobreposição (opcional)");
        permission.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(this)) {
                Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
                startActivity(i);
            } else {
                startService(new Intent(this, OverlayService.class));
            }
        });
        root.addView(permission, matchButton());

        Button launch = button("2. Mostrar menu flutuante");
        launch.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(this)) {
                startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName())));
            } else {
                startService(new Intent(this, OverlayService.class));
            }
        });
        root.addView(launch, matchButton());

        TextView status = new TextView(this);
        status.setText("Mods de demonstração: ímã de moedas • multiplicador • pulo infinito");
        status.setTextColor(Color.rgb(170, 185, 205));
        status.setTextSize(13);
        status.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, -2);
        sp.setMargins(0, dp(20), 0, 0);
        root.addView(status, sp);
        setContentView(root);
    }

    private LinearLayout.LayoutParams matchButton() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(54));
        p.setMargins(0, dp(7), 0, dp(7));
        return p;
    }
    private Button button(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(14);
        b.setAllCaps(false);
        b.setTextColor(Color.WHITE);
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(190, 35, 48)));
        return b;
    }
}
