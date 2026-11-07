package com.pb.browser;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.util.List;

public class MainActivity extends Activity {
    LinearLayout list;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(UI.BG);
        getWindow().setNavigationBarColor(UI.BG);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(UI.BG);
        root.setFitsSystemWindows(true);
        ScrollView sv = new ScrollView(this);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        int p = UI.dp(this, 20);
        col.setPadding(p, UI.dp(this, 24), p, UI.dp(this, 110));
        sv.addView(col);
        root.addView(sv, new FrameLayout.LayoutParams(-1, -1));

        col.addView(UI.text(this, "Proxy Browser", 28, UI.TEXT, true));
        TextView sub = UI.text(this, "Mỗi hồ sơ = proxy + User-Agent + UUID + WebRTC riêng", 14, UI.GREY, false);
        sub.setPadding(0, UI.dp(this, 4), 0, UI.dp(this, 20));
        col.addView(sub);
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        col.addView(list);

        TextView fab = UI.button(this, "+  Hồ sơ mới", UI.BLUE, UI.WHITE, false);
        fab.setTextSize(15);
        fab.setElevation(UI.dp(this, 6));
        fab.setOnClickListener(v -> startActivity(new Intent(this, EditActivity.class).putExtra("idx", -1)));
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM | Gravity.END);
        lp.setMargins(0, 0, p, p);
        root.addView(fab, lp);
        setContentView(root);
    }

    @Override protected void onResume() { super.onResume(); render(); }

    void render() {
        list.removeAllViews();
        JSONArray a = Store.load(this);
        if (a.length() == 0) { a.put(Store.create(1)); Store.save(this, a); }
        for (int i = 0; i < a.length(); i++) list.addView(card(a, i));
    }

    View card(JSONArray a, int i) {
        JSONObject o = a.optJSONObject(i);
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        int pd = UI.dp(this, 16);
        c.setPadding(pd, pd, pd, pd);
        c.setBackground(UI.outline(this, UI.WHITE, 16));
        c.setElevation(UI.dp(this, 1));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = UI.dp(this, 12);
        c.setLayoutParams(lp);

        c.addView(UI.text(this, o.optString("name"), 18, UI.TEXT, true));
        String uuid = o.optString("uuid");
        String ua = o.optString("ua").isEmpty() ? "UA tự động" : "UA tùy chỉnh";
        String rtc = "relay".equals(o.optString("rtc")) ? "WebRTC relay" : "WebRTC tắt";
        String[] lines = {"🌐  " + Store.proxyLabel(o.optString("proxy")),
                "🆔  " + (uuid.length() > 8 ? uuid.substring(0, 8) : uuid) + "…",
                "🛡  " + ua + "  •  " + rtc + "  •  " + o.optString("lang")};
        for (String s : lines) {
            TextView t = UI.text(this, s, 13, UI.GREY, false);
            t.setPadding(0, UI.dp(this, 6), 0, 0);
            c.addView(t);
        }
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, UI.dp(this, 14), 0, 0);
        TextView open = UI.button(this, "Mở", UI.BLUE, UI.WHITE, false);
        TextView edit = UI.button(this, "Sửa", UI.WHITE, UI.BLUE, true);
        TextView del = UI.button(this, "Xóa", UI.WHITE, UI.RED, false);
        open.setOnClickListener(v -> openProfile(o));
        final int idx = i;
        edit.setOnClickListener(v -> startActivity(new Intent(this, EditActivity.class).putExtra("idx", idx)));
        del.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Xóa hồ sơ?").setMessage("Cookie và dữ liệu của hồ sơ này sẽ bị xóa.")
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Xóa", (d, w) -> { deleteProfile(idx); }).show());
        LinearLayout.LayoutParams bl = new LinearLayout.LayoutParams(-2, -2);
        bl.rightMargin = UI.dp(this, 8);
        row.addView(open, bl); row.addView(edit, bl); row.addView(del);
        c.addView(row);
        return c;
    }

    void deleteProfile(int i) {
        JSONArray a = Store.load(this);
        String uuid = a.optJSONObject(i).optString("uuid");
        a.remove(i);
        Store.save(this, a);
        File dir = getDataDir();
        File[] fs = dir.listFiles();
        if (fs != null) for (File f : fs) if (f.getName().contains(uuid)) rm(f);
        getSharedPreferences("tabs", 0).edit().remove(uuid).apply();
        render();
    }
    static void rm(File f) {
        File[] k = f.listFiles();
        if (k != null) for (File c : k) rm(c);
        f.delete();
    }

    void openProfile(JSONObject o) {
        // Dong process trinh duyet cu (neu co) de proxy/du lieu moi ap dung sach
        boolean killed = false;
        ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> ps = am.getRunningAppProcesses();
        if (ps != null) for (ActivityManager.RunningAppProcessInfo i : ps)
            if (i.processName.endsWith(":browser") && i.pid != Process.myPid()) { Process.killProcess(i.pid); killed = true; }
        Intent it = new Intent(this, BrowserActivity.class)
                .putExtra("profile", o.toString())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        if (killed) new Handler(Looper.getMainLooper()).postDelayed(() -> startActivity(it), 400);
        else startActivity(it);
    }
}
