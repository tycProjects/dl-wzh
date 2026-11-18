package com.example.oi;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(28, 28, 28, 28);
        layout.setBackgroundColor(0xFF090510);

        TextView title = new TextView(this);
        title.setText("LIVE VOZ");
        title.setTextColor(0xFFBB86FC);
        title.setTextSize(32);
        title.setGravity(Gravity.CENTER);
        layout.addView(title);

        TextView info = new TextView(this);
        info.setText("Leitor de comentários em voz alta\n\nAtive o serviço de acessibilidade para testar. Depois abra uma LIVE no TikTok e use o botão flutuante.");
        info.setTextColor(0xFFFFFFFF);
        info.setTextSize(16);
        info.setGravity(Gravity.CENTER);
        info.setPadding(0, 24, 0, 24);
        layout.addView(info);

        Button settings = new Button(this);
        settings.setText("ATIVAR ACESSIBILIDADE");
        settings.setOnClickListener(v ->
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        );
        layout.addView(settings);

        setContentView(layout);
    }
}
