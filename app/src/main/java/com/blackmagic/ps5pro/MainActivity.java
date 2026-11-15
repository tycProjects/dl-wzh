package com.blackmagic.ps5pro;

import android.app.*;
import android.os.*;
import android.provider.Settings;
import android.content.*;
import android.graphics.Color;
import android.view.*;
import android.widget.*;
import android.content.pm.*;
import java.util.*;

public class MainActivity extends Activity {
    LinearLayout list;
    int white = Color.WHITE;
    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.rgb(8,8,12));
        getWindow().setNavigationBarColor(Color.BLACK);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(1); root.setPadding(22,24,22,16); root.setBackgroundColor(Color.rgb(8,8,12));
        TextView title = new TextView(this); title.setText("BLACKMAGIC  //  PS5 PRO"); title.setTextColor(white); title.setTextSize(23); title.setPadding(0,8,0,4);
        TextView sub = new TextView(this); sub.setText("LAUNCHER  •  V2.12"); sub.setTextColor(Color.LTGRAY); sub.setTextSize(12);
        root.addView(title); root.addView(sub);
        Button overlay = new Button(this); overlay.setText("ATIVAR CONTROLE FLUTUANTE");
        overlay.setOnClickListener(v -> {
            if (!Settings.canDrawOverlays(this)) {
                startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, android.net.Uri.parse("package:"+getPackageName())));
            } else {
                startService(new Intent(this, OverlayService.class));
                Toast.makeText(this,"Controle ativado. Abra um jogo pela biblioteca.",Toast.LENGTH_LONG).show();
            }
        });
        root.addView(overlay);
        Button access = new Button(this); access.setText("CONFIGURAR ACESSIBILIDADE (TOQUES)");
        access.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        root.addView(access);
        TextView heading = new TextView(this); heading.setText("BIBLIOTECA DE JOGOS E APLICATIVOS"); heading.setTextColor(white); heading.setTextSize(15); heading.setPadding(0,22,0,10); root.addView(heading);
        ScrollView scroll = new ScrollView(this); list = new LinearLayout(this); list.setOrientation(1); scroll.addView(list); root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        setContentView(root);
        loadApps();
    }
    void loadApps() {
        PackageManager pm = getPackageManager();
        Intent intent = new Intent(Intent.ACTION_MAIN, null); intent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> apps = pm.queryIntentActivities(intent,0);
        Collections.sort(apps,(a,b)->String.CASE_INSENSITIVE_ORDER.compare(a.loadLabel(pm).toString(),b.loadLabel(pm).toString()));
        for (ResolveInfo info: apps) {
            String pkg = info.activityInfo.packageName;
            if (pkg.equals(getPackageName())) continue;
            Button item = new Button(this);
            item.setText("▶  "+info.loadLabel(pm));
            item.setAllCaps(false);
            item.setOnClickListener(v -> {
                Intent launch = pm.getLaunchIntentForPackage(pkg);
                if (launch != null) { launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); startActivity(launch); }
                else Toast.makeText(this,"Não foi possível abrir este aplicativo.",Toast.LENGTH_SHORT).show();
            });
            list.addView(item);
        }
        if (apps.isEmpty()) { TextView empty=new TextView(this); empty.setText("Nenhum aplicativo encontrado."); empty.setTextColor(white); list.addView(empty); }
    }
}
