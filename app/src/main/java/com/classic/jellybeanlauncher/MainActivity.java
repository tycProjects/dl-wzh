package com.classic.jellybeanlauncher;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.provider.Telephony;
import android.telecom.TelecomManager;
import android.view.Gravity;
import android.view.View;
import android.widget.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends Activity {
    private LinearLayout root;
    private GridLayout grid;
    private final ArrayList<AppEntry> apps = new ArrayList<>();
    private String defaultSms = null;
    private String defaultDialer = null;

    private static class AppEntry {
        String label;
        String packageName;
        Intent launchIntent;
        Drawable icon;
        AppEntry(String l, String p, Intent i, Drawable d) {
            label=l; packageName=p; launchIntent=i; icon=d;
        }
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(24, 54, 83));
        getWindow().setNavigationBarColor(Color.rgb(18, 38, 58));
        readDefaultApps();
        buildHome();
        loadApps();
    }

    private void readDefaultApps() {
        try { defaultSms = Telephony.Sms.getDefaultSmsPackage(this); } catch (Exception ignored) {}
        try {
            TelecomManager tm = (TelecomManager)getSystemService(TELECOM_SERVICE);
            if (tm != null) defaultDialer = tm.getDefaultDialerPackage();
        } catch (Exception ignored) {}
    }

    private void buildHome() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{Color.rgb(65,139,190), Color.rgb(20,62,103)});
        root.setBackground(bg);

        TextView clock = new TextView(this);
        clock.setText("Jelly Bean");
        clock.setTextColor(Color.WHITE);
        clock.setTextSize(30);
        clock.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        clock.setGravity(Gravity.CENTER);
        clock.setPadding(12, 28, 12, 4);
        root.addView(clock, new LinearLayout.LayoutParams(-1, dp(76)));

        TextView subtitle = new TextView(this);
        subtitle.setText("Aplicativos");
        subtitle.setTextColor(0xFFEAF6FF);
        subtitle.setTextSize(14);
        subtitle.setGravity(Gravity.CENTER);
        root.addView(subtitle, new LinearLayout.LayoutParams(-1, dp(30)));

        ScrollView scroll = new ScrollView(this);
        grid = new GridLayout(this);
        grid.setColumnCount(4);
        grid.setPadding(dp(8), dp(10), dp(8), dp(16));
        scroll.addView(grid);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout dock = new LinearLayout(this);
        dock.setGravity(Gravity.CENTER);
        dock.setOrientation(LinearLayout.HORIZONTAL);
        dock.setPadding(dp(8), dp(8), dp(8), dp(8));
        GradientDrawable dockBg = new GradientDrawable();
        dockBg.setColor(0xAA163B5D);
        dockBg.setCornerRadius(dp(18));
        dock.setBackground(dockBg);
        String[] labels = {"Telefone", "SMS", "Câmera", "Navegador"};
        int[] icons = {R.drawable.ic_phone, R.drawable.ic_sms, R.drawable.ic_camera, R.drawable.ic_browser};
        for (int i=0;i<labels.length;i++) {
            final String action = labels[i];
            LinearLayout item = new LinearLayout(this);
            item.setGravity(Gravity.CENTER);
            item.setOrientation(LinearLayout.VERTICAL);
            ImageView iv = new ImageView(this);
            iv.setImageResource(icons[i]);
            item.addView(iv, new LinearLayout.LayoutParams(dp(42), dp(42)));
            TextView tv = new TextView(this);
            tv.setText(labels[i]); tv.setTextColor(Color.WHITE); tv.setTextSize(10);
            item.addView(tv);
            item.setOnClickListener(v -> launchDefault(action));
            dock.addView(item, new LinearLayout.LayoutParams(0, dp(66), 1));
        }
        LinearLayout.LayoutParams dockLp = new LinearLayout.LayoutParams(-1, dp(76));
        dockLp.setMargins(dp(10), 0, dp(10), dp(8));
        root.addView(dock, dockLp);
        setContentView(root);
    }

    private void loadApps() {
        apps.clear();
        Intent query = new Intent(Intent.ACTION_MAIN, null);
        query.addCategory(Intent.CATEGORY_LAUNCHER);
        PackageManager pm = getPackageManager();
        List<ResolveInfo> list = pm.queryIntentActivities(query, 0);
        for (ResolveInfo ri : list) {
            String pkg = ri.activityInfo.packageName;
            Intent launch = pm.getLaunchIntentForPackage(pkg);
            if (launch == null) continue;
            String label = ri.loadLabel(pm).toString();
            Drawable icon = chooseThemedIcon(pkg, ri);
            apps.add(new AppEntry(label, pkg, launch, icon));
        }
        Collections.sort(apps, Comparator.comparing(a -> a.label.toLowerCase()));
        grid.removeAllViews();
        for (AppEntry app : apps) addAppTile(app);
    }

    private Drawable chooseThemedIcon(String pkg, ResolveInfo ri) {
        // O aplicativo SMS padrão recebe o ícone Jelly Bean de SMS, mesmo que
        // o antigo Google Mensagens tenha sido removido.
        if (defaultSms != null && defaultSms.equals(pkg)) return getDrawable(R.drawable.ic_sms);
        if (defaultDialer != null && defaultDialer.equals(pkg)) return getDrawable(R.drawable.ic_phone);
        String p = pkg.toLowerCase();
        if (p.contains("camera")) return getDrawable(R.drawable.ic_camera);
        if (p.contains("browser") || p.contains("chrome")) return getDrawable(R.drawable.ic_browser);
        if (p.contains("settings")) return getDrawable(R.drawable.ic_settings);
        if (p.contains("gallery") || p.contains("photos")) return getDrawable(R.drawable.ic_gallery);
        if (p.contains("music") || p.contains("audio")) return getDrawable(R.drawable.ic_music);
        // Para apps sem correspondência, mantém o ícone original para não confundir.
        return ri.loadIcon(getPackageManager());
    }

    private void addAppTile(AppEntry app) {
        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.CENTER);
        tile.setPadding(dp(3), dp(8), dp(3), dp(8));
        ImageView icon = new ImageView(this);
        icon.setImageDrawable(app.icon);
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        tile.addView(icon, new LinearLayout.LayoutParams(dp(54), dp(54)));
        TextView name = new TextView(this);
        name.setText(app.label);
        name.setTextColor(Color.WHITE);
        name.setTextSize(11);
        name.setGravity(Gravity.CENTER);
        name.setMaxLines(2);
        name.setShadowLayer(2, 1, 1, Color.BLACK);
        tile.addView(name, new LinearLayout.LayoutParams(-1, dp(34)));
        tile.setOnClickListener(v -> {
            try { startActivity(app.launchIntent); }
            catch (Exception e) { Toast.makeText(this, "Não foi possível abrir o aplicativo", Toast.LENGTH_SHORT).show(); }
        });
        tile.setOnLongClickListener(v -> {
            Intent info = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            info.setData(Uri.parse("package:" + app.packageName));
            startActivity(info);
            return true;
        });
        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
        lp.width = 0; lp.height = dp(100); lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        grid.addView(tile, lp);
    }

    private void launchDefault(String which) {
        Intent i = null;
        if ("SMS".equals(which)) {
            i = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:"));
        } else if ("Telefone".equals(which)) {
            i = new Intent(Intent.ACTION_DIAL);
        } else if ("Câmera".equals(which)) {
            i = new Intent("android.media.action.IMAGE_CAPTURE");
        } else if ("Navegador".equals(which)) {
            i = new Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com"));
        }
        try { if (i != null) startActivity(i); }
        catch (Exception e) { Toast.makeText(this, "Nenhum aplicativo compatível encontrado", Toast.LENGTH_SHORT).show(); }
    }

    private int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density + 0.5f); }
}
