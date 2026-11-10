package com.pb.browser;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.view.Gravity;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

// Tai App: luoi cac o ten -> bam vao se mo thang link trong ho so da chon
public class AppsActivity extends Activity {
    static final String[][] APPS = {
        {"J88", "https://j6951.quest/?app=1", ""},
        {"U888", "https://u1295.buzz/?app=1", ""},
        {"ABC88", "https://abc11.link/?app=1", ""},
        {"88CLB", "https://88clb89.ink/?app=1", ""},
        {"8KBET", "https://8k1571k.top/?app=1", ""},
        {"AD88", "https://ad88vip3.com/?id=985027020&fixed.iswebclip=2", ""},
        {"XX8", "https://xx8v22.com/?id=672251583&fixed.iswebclip=2", ""},
        {"HI88", "https://m.www-24h-com-vn-com.com", ""},
        {"F8BET", "https://m.jsdujkg8djjkuqw8czbm.com", ""},
        {"SHBET", "https://m.djifshuie89shjdhsdxbvckrgio.com/", ""},
        {"NEW88", "https://m.dxiidnc84gkoopfcmbpwhsgf.com/", ""},
        {"78WIN", "https://d1bc2a6xl4d4in.cloudfront.net/78win.html", ""},
        {"JUN88V1", "https://sasa2.xn--8866-um1g.com", ""},
        {"JUN88V2", "https://d1ltas43ykyiqy.cloudfront.net/jun88cmd.html", ""},
        {"MB66", "https://m.wtyurfuijkxcvklrhyli3imxjuuflegmgkiow3i.com/", ""},
        {"OK8386", "https://m.ok8386pwa.com/home?type=ios", ""},
        {"789BET", "https://m.hsdh99hjsbcnjiufkxuuwvg.com/", ""},
        {"XX88", "https://www.xx88.fun/m/index.html", ""},
        {"RR88", "http://gioithieu.rr6958.com/?referralCode=hed5708", ""},
        {"MM88", "https://www.mm88b.com/m/index.html", ""},
        {"BL555", "http://www.bl555bb.cc/?r=xek8422", ""},
        {"KK55", "https://www.kk55.la/m/index.html", ""},
        {"OPEN88", "https://www.open8819.vip/m/index.html", ""},
        {"QQ88", "https://gioithieu.qq9900.com/?referralCode=wsf6638", ""},
        {"SC88", "https://sc88a5.cc/?id=302998280", ""},
        {"C168", "https://c1688.net/?id=286296114", ""},
        {"GG88", "https://gg8824.com/?id=364107317", ""},
        {"F168", "https://m.f168sa.com/?id=799302607", ""},
        {"CM88", "https://m.cm8828.com/?id=250672201", ""},
        {"FLY88", "https://m.fly88b.co/?id=830797767", ""},
        {"555WIN", "https://555win33.com?fixed.iswebclip=2", ""},
        {"888NEW", "https://d1mx88fmujobte.cloudfront.net", ""},
        {"28BET", "https://d2dasx87iii59t.cloudfront.net/", ""},
        {"TV88", "https://d1c3if1r02zn00.cloudfront.net", ""},
        {"888VI", "https://d1gj6lb2gvoecr.cloudfront.net", ""},
        {"888P", "https://m.888p28.com/?fixed.iswebclip=2", ""},
        {"WIN678", "https://dnlqadub1wwf0.cloudfront.net", ""},
        {"888NOW", "https://d39k3tzhkthi1p.cloudfront.net/", ""},
        {"OK9", "https://www.ok98.tv/", ""},
        {"OKVIP", "https://m.oklavip16.live/home?isIOSPure", ""},
        {"PKWIN", "https://m.pk7958874768.club/?app=1", ""},
        {"99OK", "https://m.88ok7.net?app=1", ""},
        {"OKKING", "https://m.kopap2king.club?app=1", ""},
        {"GO99", "https://m.1go99.vip/?f=3528698", ""},
        {"App (46)", "https://m.ghhdj-567dhdhhmm.asia/?app=1", ""},
        {"NOHU", "https://m.8nohu.vip/?f=6344995", ""},
        {"App (48)", "https://m.88111188.com/?app=1", ""},
        {"TT88", "https://m.1tt88.vip/?f=3535864", ""},
        {"App (50)", "https://m.1bedd-fb89bj53gg9hjs0bka.club/?app=1", ""},
        {"MMOO", "https://m.mmoo.team/?f=394579", ""},
        {"App (52)", "https://m.0mmoo.com/?app=1", ""},
        {"789P", "https://m.789p1.vip/?f=784461", ""},
        {"App (54)", "https://m.jvdf76fd92jk87gfuj60o", ""}
    };

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        UI.init(this);
        UI.bars(this, UI.BG, UI.SURFACE, true, true);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(UI.BG);
        root.setFitsSystemWindows(true);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(UI.dp(this, 8), UI.dp(this, 10), UI.dp(this, 20), UI.dp(this, 10));
        ImageView back = UI.iconBtn(this, R.drawable.ic_back, UI.TEXT, 0, 40, 20);
        back.setOnClickListener(v -> finish());
        header.addView(back);
        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0, -2, 1);
        tlp.leftMargin = UI.dp(this, 4);
        titles.addView(UI.text(this, "Tải App", 20, UI.TEXT, true));
        TextView sub = UI.text(this, APPS.length + " link · bấm vào để mở ngay", 11, UI.MUTED, false);
        sub.setPadding(0, UI.dp(this, 1), 0, 0);
        titles.addView(sub);
        header.addView(titles, tlp);
        header.addView(UI.icon(this, R.drawable.ic_open, 24, UI.INDIGO));
        root.addView(header);

        final TextView uaTg = UI.text(this, "", 12, UI.TEXT, true);
        uaTg.setPadding(UI.dp(this, 20), UI.dp(this, 4), UI.dp(this, 20), UI.dp(this, 8));
        uaTg.setOnClickListener(v -> {
            Store.setPerf(this, "uaBySite", !Store.perf(this, "uaBySite", true));
            uaTg.setText(uaLabel());
        });
        uaTg.setText(uaLabel());
        root.addView(uaTg);

        ScrollView sv = new ScrollView(this);
        sv.setVerticalScrollBarEnabled(false);
        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(3);
        grid.setPadding(UI.dp(this, 14), UI.dp(this, 6), UI.dp(this, 14), UI.dp(this, 24));
        int cell = (getResources().getDisplayMetrics().widthPixels - UI.dp(this, 28)) / 3;
        for (int i = 0; i < APPS.length; i++) {
            final String[] a = APPS[i];
            LinearLayout c = new LinearLayout(this);
            c.setOrientation(LinearLayout.VERTICAL);
            c.setGravity(Gravity.CENTER);
            c.setClickable(true);
            c.setElevation(UI.dp(this, 2));
            c.setBackground(UI.ripple(UI.round(this, UI.SURFACE, 16)));
            c.setPadding(UI.dp(this, 4), UI.dp(this, 18), UI.dp(this, 4), UI.dp(this, 18));
            TextView t = UI.text(this, a[0], 13, UI.TEXT, true);
            t.setGravity(Gravity.CENTER);
            t.setSingleLine(true);
            c.addView(t);
            if (!a[2].isEmpty()) {
                TextView n = UI.text(this, a[2], 10, UI.MUTED, false);
                n.setGravity(Gravity.CENTER);
                n.setPadding(0, UI.dp(this, 3), 0, 0);
                c.addView(n);
            }
            c.setOnClickListener(v -> choose(a[0], a[1]));
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = cell - UI.dp(this, 8);
            lp.height = GridLayout.LayoutParams.WRAP_CONTENT;
            lp.setMargins(UI.dp(this, 4), UI.dp(this, 4), UI.dp(this, 4), UI.dp(this, 4));
            grid.addView(c, lp);
        }
        sv.addView(grid);
        root.addView(sv, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
    }

    @Override protected void onResume() { super.onResume(); Master.sync(this); }

    String uaLabel() {
        return "UA theo web: " + (Store.perf(this, "uaBySite", true) ? "BẬT" : "TẮT") + "  (bấm để đổi)";
    }

    // 1 ho so -> mo luon; nhieu ho so -> hoi chon ho so (de dung dung proxy/danh tinh)
    void choose(String name, String url) {
        final JSONArray ps = Store.load(this);
        if (ps.length() == 0) { Toast.makeText(this, "Hãy tạo một hồ sơ trước", Toast.LENGTH_SHORT).show(); return; }
        if (ps.length() == 1) { open(ps.optJSONObject(0), url); return; }
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        TextView t = UI.text(this, "Mở " + name + " bằng hồ sơ", 16, UI.TEXT, true);
        t.setPadding(UI.dp(this, 20), UI.dp(this, 6), UI.dp(this, 20), UI.dp(this, 8));
        body.addView(t);
        ScrollView sv = new ScrollView(this);
        LinearLayout rows = new LinearLayout(this);
        rows.setOrientation(LinearLayout.VERTICAL);
        sv.addView(rows);
        body.addView(sv, new LinearLayout.LayoutParams(-1, Math.min(UI.dp(this, 380), UI.dp(this, 64) * ps.length())));
        final android.app.Dialog[] dh = new android.app.Dialog[1];
        for (int i = 0; i < ps.length(); i++) {
            final JSONObject p = ps.optJSONObject(i);
            if (p == null) continue;
            LinearLayout r = UI.menuRow(this, R.drawable.ic_badge, UI.INDIGO, p.optString("name", "Hồ sơ"),
                    Store.proxyLabel(p.optString("proxy"), p.optString("ptype", "http")));
            r.setOnClickListener(v -> { dh[0].dismiss(); open(p, url); });
            rows.addView(r);
        }
        dh[0] = UI.sheet(this, body);
    }

    // UA theo web: link ghi ro iOS (webclip / type=ios / isIOSPure) -> iPhone; link trang mobile (m., /m/) -> khong de may tinh
    static String wantFor(String url) {
        String u = url.toLowerCase();
        if (u.contains("iswebclip") || u.contains("type=ios") || u.contains("isiospure")) return "ios";
        String host = u.replaceAll("^https?://", "");
        host = host.contains("/") ? host.substring(0, host.indexOf('/')) : host;
        if (host.startsWith("m.") || u.contains("/m/")) return "mobile";
        return "";
    }

    String applyUaForSite(JSONObject o, String url) {
        if (!Store.prefs(this).getBoolean("uaBySite", true)) return null;
        String want = wantFor(url);
        String cur = o.optString("device", "");
        Devices.Dev dv = null;
        if ("ios".equals(want)) {
            Devices.Dev now = Devices.byId(cur);
            if (now == null || !"iPhone".equals(now.brand)) dv = Devices.byId("iphone-iphone-15-pro-max");
        } else if ("mobile".equals(want) && "desktop".equals(cur)) {
            dv = Devices.byId("samsung-galaxy-s24-ultra");
        }
        if (dv == null) return null;
        try {
            o.put("device", dv.id);
            o.put("ua", dv.ua);
            o.put("fp", dv.fp());
        } catch (Exception ignored) { return null; }
        return dv.name;
    }

    void open(JSONObject p, String url) {
        try {
            JSONObject o = new JSONObject(p.toString());
            o.put("url", url);
            o.put("openUrl", url);
            o.remove("tabs");
            String sw = applyUaForSite(o, url);
            if (sw != null) Toast.makeText(this, "UA theo web: " + sw, Toast.LENGTH_SHORT).show();
            boolean killed = false;
            ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
            List<ActivityManager.RunningAppProcessInfo> list = am.getRunningAppProcesses();
            if (list != null) for (ActivityManager.RunningAppProcessInfo i : list)
                if (i.processName.endsWith(":browser") && i.pid != Process.myPid()) { Process.killProcess(i.pid); killed = true; }
            final Intent it = new Intent(this, BrowserActivity.class)
                    .putExtra("profile", o.toString())
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            if (killed) new Handler(Looper.getMainLooper()).postDelayed(() -> startActivity(it), 400);
            else startActivity(it);
        } catch (Exception e) {
            Toast.makeText(this, "Không mở được link", Toast.LENGTH_SHORT).show();
        }
    }
}
