package com.pb.browser;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.util.List;

public class MainActivity extends Activity {
    LinearLayout list;
    TextView statTotal, statProxy, statDirect, countPill;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        UI.init(this);
        UI.bars(this, UI.HERO[0], UI.BG, false, true);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(UI.BG);
        root.setFitsSystemWindows(true);
        ScrollView sv = new ScrollView(this);
        sv.setFillViewport(true);
        sv.setVerticalScrollBarEnabled(false);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        sv.addView(col);
        root.addView(sv, new FrameLayout.LayoutParams(-1, -1));

        col.addView(hero(), new LinearLayout.LayoutParams(-1, -2));
        if (UI.master) col.addView(turboEntry());
        col.addView(perfEntry());
        col.addView(appsEntry());
        col.addView(backupEntry());

        LinearLayout section = new LinearLayout(this);
        section.setGravity(Gravity.CENTER_VERTICAL);
        section.setPadding(UI.dp(this, 22), UI.dp(this, 20), UI.dp(this, 22), UI.dp(this, 10));
        TextView lb = UI.label(this, "HỒ SƠ DUYỆT WEB");
        lb.setTextSize(11);
        section.addView(lb);
        countPill = UI.chip(this, "0", UI.alpha(UI.INDIGO, 0x14), UI.INDIGO);
        LinearLayout.LayoutParams cpl = new LinearLayout.LayoutParams(-2, -2);
        cpl.leftMargin = UI.dp(this, 8);
        section.addView(countPill, cpl);
        section.addView(new View(this), new LinearLayout.LayoutParams(0, 1, 1));
        ImageView hint = UI.icon(this, R.drawable.ic_shield, 14, UI.MUTED);
        section.addView(hint);
        TextView hintT = UI.text(this, "Ẩn danh", 11, UI.MUTED, false);
        hintT.setPadding(UI.dp(this, 4), 0, 0, 0);
        section.addView(hintT);
        col.addView(section);

        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(UI.dp(this, 20), 0, UI.dp(this, 20), UI.dp(this, 8));
        col.addView(list);

        LinearLayout footer = new LinearLayout(this);
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(UI.dp(this, 20), UI.dp(this, 10), UI.dp(this, 20), UI.dp(this, 120));
        footer.addView(UI.icon(this, R.drawable.ic_shield, 12, UI.MUTED));
        TextView ft = UI.text(this, "Mỗi hồ sơ là một danh tính số riêng biệt", 11, UI.MUTED, false);
        ft.setPadding(UI.dp(this, 5), 0, 0, 0);
        footer.addView(ft);
        col.addView(footer);

        LinearLayout fab = new LinearLayout(this);
        fab.setGravity(Gravity.CENTER);
        fab.setPadding(UI.dp(this, 22), 0, UI.dp(this, 24), 0);
        fab.setBackground(UI.ripple(UI.gradient(this, UI.INDIGO, UI.BLUE, 26)));
        fab.setElevation(UI.dp(this, 8));
        fab.setClickable(true);
        UI.press(fab);
        fab.addView(UI.icon(this, R.drawable.ic_add, 18, UI.WHITE));
        TextView fabT = UI.text(this, "Hồ sơ mới", 15, UI.WHITE, true);
        fabT.setPadding(UI.dp(this, 8), 0, 0, 0);
        fab.addView(fabT);
        fab.setOnClickListener(v -> startActivity(new Intent(this, EditActivity.class).putExtra("idx", -1)));
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(-2, UI.dp(this, 52), Gravity.BOTTOM | Gravity.END);
        lp.setMargins(0, 0, UI.dp(this, 20), UI.dp(this, 20));
        root.addView(fab, lp);
        setContentView(root);
        if (fadeIn) { fadeIn = false; root.setAlpha(0f); root.animate().alpha(1f).setDuration(320).start(); }
    }

    static boolean fadeIn;

    View hero() {
        FrameLayout h = new FrameLayout(this);
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{UI.HERO[0], UI.HERO[1], UI.HERO[2]});
        float r = UI.dp(this, 28);
        g.setCornerRadii(new float[]{0, 0, 0, 0, r, r, r, r});
        h.setBackground(g);
        h.addView(new HeroArt(this), new FrameLayout.LayoutParams(-1, -1));
        if (UI.master) {   // robot ben phai hero
            FrameLayout.LayoutParams rl = new FrameLayout.LayoutParams(UI.dp(this, 86), UI.dp(this, 86), Gravity.TOP | Gravity.END);
            rl.setMargins(0, UI.dp(this, 64), UI.dp(this, 14), 0);
            h.addView(new RobotView(this), rl);
        }

        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(UI.dp(this, 22), UI.dp(this, 20), UI.dp(this, 22), UI.dp(this, 22));
        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        FrameLayout logo = new FrameLayout(this);
        logo.setBackground(UI.round(this, 0x29FFFFFF, 14));
        ImageView shield = UI.icon(this, R.drawable.ic_shield, 26, UI.WHITE);
        FrameLayout.LayoutParams slp = new FrameLayout.LayoutParams(UI.dp(this, 26), UI.dp(this, 26), Gravity.CENTER);
        logo.addView(shield, slp);
        top.addView(logo, new LinearLayout.LayoutParams(UI.dp(this, 46), UI.dp(this, 46)));
        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0, -2, 1);
        tlp.leftMargin = UI.dp(this, 12);
        titles.addView(UI.text(this, UI.master ? "TUANANH // MASTER" : "TUANANH BROWSER", 20, UI.WHITE, true));
        TextView sub = UI.text(this, UI.master ? "SYS://ONLINE · TURBO ENGINE v3.0" : "Đa hồ sơ · đa danh tính · ẩn danh", 12, UI.ON_HERO, false);
        sub.setPadding(0, UI.dp(this, 2), 0, 0);
        titles.addView(sub);
        top.addView(titles, tlp);
        c.addView(top);
        c.addView(masterButton(), masterLp());

        LinearLayout stats = new LinearLayout(this);
        stats.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams slp2 = new LinearLayout.LayoutParams(-1, -2);
        slp2.topMargin = UI.dp(this, 18);
        statTotal = stat("0", "HỒ SƠ");
        statProxy = stat("0", "QUA PROXY");
        statDirect = stat("0", "TRỰC TIẾP");
        stats.addView(statBox(statTotal), statLp(true));
        stats.addView(statBox(statProxy), statLp(true));
        stats.addView(statBox(statDirect), statLp(false));
        c.addView(stats, slp2);
        h.addView(c, new FrameLayout.LayoutParams(-1, -2));
        return h;
    }

    LinearLayout.LayoutParams masterLp() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, UI.dp(this, 46));
        lp.topMargin = UI.dp(this, 16);
        return lp;
    }

    // NUT MASTER: bat/tat giao dien robot + Turbo. Doi xong tu ve lai man hinh.
    View masterButton() {
        final boolean on = UI.master;
        LinearLayout b = new LinearLayout(this);
        b.setGravity(Gravity.CENTER);
        b.setClickable(true);
        GradientDrawable g;
        if (on) {
            g = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[]{UI.CYAN, UI.GREEN});
            g.setCornerRadius(UI.rad(this, 24));
        } else {
            g = UI.round(this, 0x29FFFFFF, 24);
            g.setStroke(UI.dp(this, 1), 0x66FFFFFF);
        }
        b.setBackground(UI.rippleLight(g));
        int fg = on ? 0xFF04070D : UI.WHITE;
        b.addView(UI.icon(this, R.drawable.ic_robot, 22, fg));
        TextView t = UI.text(this, on ? "MASTER · ĐANG BẬT" : "MASTER · BẤM ĐỂ BẬT", 14, fg, true);
        t.setLetterSpacing(0.06f);
        t.setPadding(UI.dp(this, 10), 0, 0, 0);
        b.addView(t);
        b.setOnClickListener(v -> {
            final boolean next = !UI.master;
            View root = findViewById(android.R.id.content);
            root.animate().alpha(0f).setDuration(180).withEndAction(() -> {
                Master.set(this, next);
                UI.init(this);
                fadeIn = true;
                recreate();
            }).start();
        });
        return b;
    }

    // Tom tat Turbo (chi hien khi MASTER bat) -> bam de chinh trong man hinh Hieu nang
    View turboEntry() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setBackground(UI.ripple(UI.round(this, UI.SURFACE, 20)));
        c.setElevation(UI.dp(this, 2));
        c.setClickable(true);
        c.setPadding(UI.dp(this, 16), UI.dp(this, 14), UI.dp(this, 16), UI.dp(this, 14));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(UI.dp(this, 20), UI.dp(this, 16), UI.dp(this, 20), 0);
        c.setLayoutParams(lp);
        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.addView(UI.text(this, "TURBO ENGINE", 15, UI.CYAN, true));
        top.addView(new View(this), new LinearLayout.LayoutParams(0, 1, 1));
        top.addView(UI.chip(this, "ONLINE", UI.alpha(UI.GREEN, 0x22), UI.GREEN));
        c.addView(top);
        int sp = Master.split(this);
        String l1 = "Chia luồng proxy: " + (sp == 2 ? "TỐI ĐA" : sp == 1 ? "CÂN BẰNG" : "TẮT")
                + (sp == 0 ? "  ·  100% qua proxy" : "  ·  file tĩnh đi thẳng WiFi (lộ IP thật)");
        String l2 = "Chặn quảng cáo: " + (Master.adblock(this) ? "BẬT" : "TẮT") + "  ·  Ảnh lười: " + (Master.lazy(this) ? "BẬT" : "TẮT");
        TextView a = UI.text(this, l1, 11, UI.MUTED, false);
        a.setPadding(0, UI.dp(this, 8), 0, 0);
        c.addView(a);
        TextView b2 = UI.text(this, l2, 11, UI.MUTED, false);
        b2.setPadding(0, UI.dp(this, 3), 0, 0);
        c.addView(b2);
        c.setOnClickListener(v -> startActivity(new Intent(this, PerfActivity.class)));
        return c;
    }

    View appsEntry() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.HORIZONTAL);
        c.setGravity(Gravity.CENTER_VERTICAL);
        c.setBackground(UI.ripple(UI.round(this, UI.SURFACE, 20)));
        c.setElevation(UI.dp(this, 2));
        c.setClickable(true);
        c.setPadding(UI.dp(this, 16), UI.dp(this, 14), UI.dp(this, 16), UI.dp(this, 14));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(UI.dp(this, 20), UI.dp(this, 12), UI.dp(this, 20), 0);
        c.setLayoutParams(lp);
        ImageView ic = UI.icon(this, R.drawable.ic_open, 44, UI.WHITE);
        int p = UI.dp(this, 11);
        ic.setPadding(p, p, p, p);
        ic.setBackground(UI.gradient(this, UI.INDIGO, UI.CYAN, 13));
        c.addView(ic, new LinearLayout.LayoutParams(UI.dp(this, 44), UI.dp(this, 44)));
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(0, -2, 1);
        clp.leftMargin = UI.dp(this, 13);
        col.addView(UI.text(this, "Tải App", 15, UI.TEXT, true));
        TextView s = UI.text(this, "Danh sách link · bấm vào mở ngay", 11, UI.MUTED, false);
        s.setPadding(0, UI.dp(this, 2), 0, 0);
        col.addView(s);
        c.addView(col, clp);
        c.addView(UI.icon(this, R.drawable.ic_chevron, 20, UI.MUTED));
        c.setOnClickListener(v -> startActivity(new Intent(this, AppsActivity.class)));
        return c;
    }

    static final int REQ_EXPORT = 71, REQ_IMPORT = 72;

    View backupEntry() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setBackground(UI.round(this, UI.SURFACE, 20));
        c.setElevation(UI.dp(this, 2));
        c.setPadding(UI.dp(this, 16), UI.dp(this, 14), UI.dp(this, 16), UI.dp(this, 14));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(UI.dp(this, 20), UI.dp(this, 12), UI.dp(this, 20), 0);
        c.setLayoutParams(lp);
        c.addView(UI.text(this, "Sao lưu và khôi phục", 15, UI.TEXT, true));
        TextView s = UI.text(this, "Xuất toàn bộ hồ sơ ra file hoặc nhập lại khi đổi máy.", 11, UI.MUTED, false);
        s.setPadding(0, UI.dp(this, 2), 0, UI.dp(this, 10));
        c.addView(s);
        LinearLayout row = new LinearLayout(this);
        TextView ex = UI.button(this, "Sao lưu", UI.INDIGO, UI.INDIGO, true);
        TextView im = UI.button(this, "Khôi phục", UI.INDIGO, UI.INDIGO, true);
        LinearLayout.LayoutParams a = new LinearLayout.LayoutParams(0, -2, 1);
        a.rightMargin = UI.dp(this, 6);
        LinearLayout.LayoutParams b2 = new LinearLayout.LayoutParams(0, -2, 1);
        b2.leftMargin = UI.dp(this, 6);
        row.addView(ex, a);
        row.addView(im, b2);
        c.addView(row);
        ex.setOnClickListener(v -> UI.dialog(this)
                .setTitle("Sao lưu hồ sơ")
                .setMessage("File sao lưu chứa proxy kèm user:pass ở dạng văn bản thường. Hãy cất file ở nơi an toàn và đừng gửi cho người khác.")
                .setPositiveButton("Tiếp tục", (d, w) -> startActivityForResult(
                        new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE)
                                .setType("application/json").putExtra(Intent.EXTRA_TITLE, "tuananh-browser-backup.json"), REQ_EXPORT))
                .setNegativeButton("Huỷ", null).show());
        im.setOnClickListener(v -> startActivityForResult(
                new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*"), REQ_IMPORT));
        return c;
    }

    @Override protected void onActivityResult(int rc, int res, Intent d) {
        super.onActivityResult(rc, res, d);
        if (res != RESULT_OK || d == null || d.getData() == null) return;
        Uri u = d.getData();
        try {
            if (rc == REQ_EXPORT) {
                JSONObject root = new JSONObject();
                root.put("app", "TUANANH BROWSER");
                root.put("version", 1);
                root.put("profiles", Store.load(this));
                java.io.OutputStream os = getContentResolver().openOutputStream(u, "wt");
                os.write(root.toString(2).getBytes("UTF-8"));
                os.close();
                android.widget.Toast.makeText(this, "Đã sao lưu " + Store.load(this).length() + " hồ sơ", android.widget.Toast.LENGTH_LONG).show();
            } else if (rc == REQ_IMPORT) {
                java.io.InputStream is = getContentResolver().openInputStream(u);
                java.io.ByteArrayOutputStream bo = new java.io.ByteArrayOutputStream();
                byte[] buf = new byte[8192]; int n;
                while ((n = is.read(buf)) > 0) bo.write(buf, 0, n);
                is.close();
                String txt = bo.toString("UTF-8").trim();
                JSONArray in = txt.startsWith("[") ? new JSONArray(txt) : new JSONObject(txt).getJSONArray("profiles");
                JSONArray cur = Store.load(this);
                int added = 0, skipped = 0;
                for (int i = 0; i < in.length(); i++) {
                    JSONObject o = in.optJSONObject(i);
                    if (o == null || o.optString("uuid").isEmpty()) { skipped++; continue; }
                    boolean dup = false;
                    for (int j = 0; j < cur.length(); j++) if (o.optString("uuid").equals(cur.getJSONObject(j).optString("uuid"))) { dup = true; break; }
                    if (dup) { skipped++; continue; }
                    cur.put(o); added++;
                }
                Store.save(this, cur);
                render();
                android.widget.Toast.makeText(this, "Đã khôi phục " + added + " hồ sơ" + (skipped > 0 ? " · bỏ qua " + skipped + " (trùng hoặc lỗi)" : ""), android.widget.Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            android.widget.Toast.makeText(this, "Lỗi: " + e.getMessage(), android.widget.Toast.LENGTH_LONG).show();
        }
    }

    View perfEntry() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.HORIZONTAL);
        c.setGravity(Gravity.CENTER_VERTICAL);
        c.setBackground(UI.ripple(UI.round(this, UI.SURFACE, 20)));
        c.setElevation(UI.dp(this, 2));
        c.setClickable(true);
        c.setPadding(UI.dp(this, 16), UI.dp(this, 14), UI.dp(this, 16), UI.dp(this, 14));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(UI.dp(this, 20), UI.dp(this, 16), UI.dp(this, 20), 0);
        c.setLayoutParams(lp);
        ImageView ic = UI.icon(this, R.drawable.ic_speed, 44, UI.WHITE);
        int p = UI.dp(this, 11);
        ic.setPadding(p, p, p, p);
        ic.setBackground(UI.gradient(this, UI.INDIGO, UI.CYAN, 13));
        c.addView(ic, new LinearLayout.LayoutParams(UI.dp(this, 44), UI.dp(this, 44)));
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(0, -2, 1);
        clp.leftMargin = UI.dp(this, 13);
        col.addView(UI.text(this, "Hiệu năng & Cấu hình máy", 15, UI.TEXT, true));
        TextView s = UI.text(this, "Tăng FPS · Fix lag · Đo ping · Thông số máy", 11, UI.MUTED, false);
        s.setPadding(0, UI.dp(this, 2), 0, 0);
        col.addView(s);
        c.addView(col, clp);
        c.addView(UI.icon(this, R.drawable.ic_chevron, 20, UI.MUTED));
        c.setOnClickListener(v -> startActivity(new Intent(this, PerfActivity.class)));
        return c;
    }

    TextView stat(String num, String label) {
        TextView t = UI.text(this, num, 21, UI.WHITE, true);
        t.setTag(label);
        return t;
    }
    LinearLayout statBox(TextView num) {
        LinearLayout b = new LinearLayout(this);
        b.setOrientation(LinearLayout.VERTICAL);
        b.setBackground(UI.round(this, 0x1FFFFFFF, 16));
        b.setPadding(UI.dp(this, 14), UI.dp(this, 11), UI.dp(this, 14), UI.dp(this, 11));
        b.addView(num);
        TextView l = UI.text(this, String.valueOf(num.getTag()), 9, UI.ON_HERO, true);
        l.setLetterSpacing(0.08f);
        l.setPadding(0, UI.dp(this, 2), 0, 0);
        b.addView(l);
        return b;
    }
    LinearLayout.LayoutParams statLp(boolean margin) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1);
        if (margin) lp.rightMargin = UI.dp(this, 10);
        return lp;
    }

    static class HeroArt extends View {
        final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        HeroArt(android.content.Context c) { super(c); }
        @Override protected void onDraw(Canvas cv) {
            int w = getWidth(), h = getHeight();
            float d = getResources().getDisplayMetrics().density;
            if (UI.master) {   // luoi neon + mach dien
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(1f * d);
                p.setColor(0x1A22E5FF);
                float step = 24f * d;
                for (float x = 0; x < w; x += step) cv.drawLine(x, 0, x, h, p);
                for (float y = 0; y < h; y += step) cv.drawLine(0, y, w, y, p);
                p.setColor(0x6622E5FF);
                p.setStrokeWidth(1.6f * d);
                cv.drawLine(0, h * 0.62f, w * 0.30f, h * 0.62f, p);
                cv.drawLine(w * 0.30f, h * 0.62f, w * 0.36f, h * 0.55f, p);
                cv.drawLine(w * 0.36f, h * 0.55f, w * 0.52f, h * 0.55f, p);
                p.setStyle(Paint.Style.FILL);
                p.setColor(0xFF22E5FF);
                cv.drawCircle(w * 0.52f, h * 0.55f, 2.6f * d, p);
                return;
            }
            float cx = w * 0.88f, cy = h * 0.16f;
            p.setStyle(Paint.Style.FILL);
            p.setColor(0x14FFFFFF);
            cv.drawCircle(cx, cy, h * 0.42f, p);
            p.setColor(0x0DFFFFFF);
            cv.drawCircle(cx, cy, h * 0.62f, p);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(1.2f * d);
            p.setColor(0x2EFFFFFF);
            cv.drawCircle(cx, cy, h * 0.30f, p);
            p.setColor(0x1FFFFFFF);
            cv.drawCircle(cx, cy, h * 0.50f, p);
            p.setStyle(Paint.Style.FILL);
            p.setColor(0xFF00C2FF);
            p.setAlpha(90);
            cv.drawCircle(cx - h * 0.30f, cy, 3.2f * d, p);
            p.setAlpha(46);
            float sx = w * 0.06f, sy = h * 0.72f, gap = 15f * d;
            for (int i = 0; i < 5; i++) for (int j = 0; j < 3; j++)
                cv.drawCircle(sx + i * gap, sy + j * gap, 1.6f * d, p);
            p.setAlpha(255);
        }
    }

    @Override protected void onResume() { super.onResume(); Master.sync(this); render(); }

    void render() {
        JSONArray a = Store.load(this);
        if (!getSharedPreferences("pb", 0).getBoolean("seeded", false)) {
            if (a.length() == 0) { a.put(Store.create(1)); Store.save(this, a); }
            getSharedPreferences("pb", 0).edit().putBoolean("seeded", true).apply();
        }
        int proxy = 0;
        for (int i = 0; i < a.length(); i++)
            if (!a.optJSONObject(i).optString("proxy").trim().isEmpty()) proxy++;
        statTotal.setText(String.valueOf(a.length()));
        statProxy.setText(String.valueOf(proxy));
        statDirect.setText(String.valueOf(a.length() - proxy));
        countPill.setText(String.valueOf(a.length()));
        list.removeAllViews();
        if (a.length() == 0) { list.addView(empty()); return; }
        for (int i = 0; i < a.length(); i++) {
            View card = card(a, i);
            list.addView(card);
            card.setAlpha(0f);
            card.setTranslationY(UI.dp(this, 14));
            card.animate().alpha(1f).translationY(0).setStartDelay(i * 55)
                    .setDuration(280).setInterpolator(new DecelerateInterpolator()).start();
        }
    }

    View empty() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setGravity(Gravity.CENTER);
        c.setPadding(UI.dp(this, 30), UI.dp(this, 36), UI.dp(this, 30), UI.dp(this, 36));
        c.setBackground(UI.outline(this, UI.SURFACE, 20));
        FrameLayout circle = new FrameLayout(this);
        circle.setBackground(UI.gradient(this, UI.INDIGO, UI.CYAN, 42));
        circle.addView(UI.icon(this, R.drawable.ic_shield, 40, UI.WHITE),
                new FrameLayout.LayoutParams(UI.dp(this, 40), UI.dp(this, 40), Gravity.CENTER));
        c.addView(circle, new LinearLayout.LayoutParams(UI.dp(this, 84), UI.dp(this, 84)));
        TextView t = UI.text(this, "Chưa có hồ sơ nào", 18, UI.TEXT, true);
        t.setPadding(0, UI.dp(this, 18), 0, 0);
        c.addView(t);
        TextView s = UI.text(this, "Tạo hồ sơ đầu tiên để bắt đầu\nduyệt web với danh tính riêng", 12, UI.MUTED, false);
        s.setGravity(Gravity.CENTER);
        s.setPadding(0, UI.dp(this, 6), 0, UI.dp(this, 20));
        c.addView(s);
        TextView b = UI.gradButton(this, "Tạo hồ sơ đầu tiên");
        b.setOnClickListener(v -> startActivity(new Intent(this, EditActivity.class).putExtra("idx", -1)));
        c.addView(b);
        return c;
    }

    View card(JSONArray a, int i) {
        JSONObject o = a.optJSONObject(i);
        String name = o.optString("name", "Hồ sơ");
        boolean hasProxy = !o.optString("proxy").trim().isEmpty();
        boolean socks5 = "socks5".equals(Store.schemeOf(o.optString("proxy"), o.optString("ptype", "http")));
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setBackground(UI.round(this, UI.SURFACE, 20));
        c.setElevation(UI.dp(this, 2));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = UI.dp(this, 14);
        c.setLayoutParams(lp);

        // Vung 1: danh tinh
        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(UI.dp(this, 16), UI.dp(this, 16), UI.dp(this, 16), UI.dp(this, 12));
        top.addView(UI.avatar(this, name, i, 46));
        LinearLayout idc = new LinearLayout(this);
        idc.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(0, -2, 1);
        ilp.leftMargin = UI.dp(this, 12);
        TextView nm = UI.text(this, name, 16, UI.TEXT, true);
        nm.setSingleLine(true);
        nm.setEllipsize(TextUtils.TruncateAt.END);
        idc.addView(nm);
        LinearLayout uid = new LinearLayout(this);
        uid.setGravity(Gravity.CENTER_VERTICAL);
        uid.setPadding(0, UI.dp(this, 3), 0, 0);
        uid.addView(UI.icon(this, R.drawable.ic_key, 11, UI.MUTED));
        String uuid = o.optString("uuid");
        TextView ut = UI.text(this, uuid.length() > 8 ? uuid.substring(0, 8) + "…" : uuid, 11, UI.MUTED, false);
        ut.setTypeface(Typeface.MONOSPACE);
        ut.setPadding(UI.dp(this, 4), 0, 0, 0);
        uid.addView(ut);
        idc.addView(uid);
        top.addView(idc, ilp);
        top.addView(hasProxy ? UI.chip(this, socks5 ? "SOCKS5" : "HTTP",
                UI.alpha(socks5 ? UI.BLUE : UI.GREEN, 0x1C), socks5 ? UI.BLUE : UI.GREEN_TEXT)
                : UI.chip(this, "TRỰC TIẾP", UI.alpha(UI.AMBER, 0x24), UI.AMBER_TEXT));
        c.addView(top);

        // Vung 2: cau hinh
        String devId = o.optString("device", "");
        Devices.Dev dv = Devices.byId(devId);
        String ua = "desktop".equals(devId) ? "Máy tính Windows"
                : dv != null ? dv.name
                : (o.optString("ua").isEmpty() ? "UA tự động" : "UA tùy chỉnh");
        String rtc = "relay".equals(o.optString("rtc")) ? "relay" : "tắt";
        String home = o.optString("url", "https://www.google.com");
        String host = Uri.parse(home).getHost();
        if (host == null) host = home;
        c.addView(infoRow(R.drawable.ic_server, UI.INDIGO, "PROXY",
                hasProxy ? Store.proxyLabel(o.optString("proxy"), o.optString("ptype", "http")) : "Kết nối trực tiếp"));
        c.addView(infoRow(R.drawable.ic_fingerprint, UI.VIOLET, "DẤU VÂN TAY",
                ua + "  ·  WebRTC " + rtc));
        c.addView(infoRow(R.drawable.ic_translate, UI.BLUE, "NGÔN NGỮ · TRANG CHỦ",
                o.optString("lang", "en-US") + "  ·  " + host));

        LinearLayout dvd = new LinearLayout(this);
        dvd.setPadding(UI.dp(this, 16), UI.dp(this, 10), UI.dp(this, 16), 0);
        dvd.addView(UI.divider(this));
        c.addView(dvd);

        // Vung 3: hanh dong
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(UI.dp(this, 12), UI.dp(this, 10), UI.dp(this, 12), UI.dp(this, 12));
        LinearLayout open = new LinearLayout(this);
        open.setGravity(Gravity.CENTER);
        open.setBackground(UI.ripple(UI.gradient(this, UI.INDIGO, UI.BLUE, 21)));
        open.setClickable(true);
        open.addView(UI.icon(this, R.drawable.ic_open, 15, UI.WHITE));
        TextView ot = UI.text(this, "Mở trình duyệt", 13, UI.WHITE, true);
        ot.setPadding(UI.dp(this, 7), 0, 0, 0);
        open.addView(ot);
        open.setOnClickListener(v -> openProfile(o));
        row.addView(open, new LinearLayout.LayoutParams(0, UI.dp(this, 42), 1));
        final int idx = i;
        ImageView edit = UI.iconBtn(this, R.drawable.ic_pencil, UI.INDIGO, UI.alpha(UI.INDIGO, 0x12), 42, 13);
        edit.setOnClickListener(v -> startActivity(new Intent(this, EditActivity.class).putExtra("idx", idx)));
        LinearLayout.LayoutParams elp = new LinearLayout.LayoutParams(UI.dp(this, 42), UI.dp(this, 42));
        elp.leftMargin = UI.dp(this, 8);
        row.addView(edit, elp);
        ImageView del = UI.iconBtn(this, R.drawable.ic_trash, UI.RED, UI.alpha(UI.RED, 0x10), 42, 13);
        del.setOnClickListener(v -> {
            AlertDialog d = UI.dialog(this)
                    .setTitle("Xóa hồ sơ?")
                    .setMessage("Cookie và dữ liệu của hồ sơ \"" + name + "\" sẽ bị xóa vĩnh viễn.")
                    .setNegativeButton("Hủy", null)
                    .setPositiveButton("Xóa", (dg, w) -> deleteProfile(idx)).show();
            d.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(UI.RED);
        });
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(UI.dp(this, 42), UI.dp(this, 42));
        dlp.leftMargin = UI.dp(this, 8);
        row.addView(del, dlp);
        c.addView(row);
        return c;
    }

    View infoRow(int res, int tint, String label, String value) {
        LinearLayout r = new LinearLayout(this);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(UI.dp(this, 16), UI.dp(this, 4), UI.dp(this, 16), UI.dp(this, 4));
        ImageView ic = UI.icon(this, res, 32, tint);
        int p = UI.dp(this, 8);
        ic.setPadding(p, p, p, p);
        ic.setBackground(UI.round(this, UI.alpha(tint, 0x14), 10));
        r.addView(ic);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(0, -2, 1);
        clp.leftMargin = UI.dp(this, 10);
        col.addView(UI.label(this, label));
        TextView v = UI.text(this, value, 12, UI.TEXT, false);
        v.setSingleLine(true);
        v.setEllipsize(TextUtils.TruncateAt.END);
        v.setPadding(0, UI.dp(this, 1), 0, 0);
        col.addView(v);
        r.addView(col, clp);
        return r;
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
        // don cache Turbo + cache mui gio cua ho so nay
        rm(new File(getCacheDir(), "turbo_" + uuid));
        android.content.SharedPreferences tzp = getSharedPreferences("tzcache", 0);
        android.content.SharedPreferences.Editor te = tzp.edit();
        for (String k : tzp.getAll().keySet()) if (k.startsWith(uuid)) te.remove(k);
        te.apply();
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