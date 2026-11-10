package com.pb.browser;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;

public class PerfActivity extends Activity {
    LinearLayout col, specBox;
    TextView fpsNow, pingResult;
    EditText pingHost;

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
        titles.addView(UI.text(this, "Hiệu năng & Cấu hình", 20, UI.TEXT, true));
        TextView sub = UI.text(this, "Tối ưu thật · thông số máy thật", 11, UI.MUTED, false);
        sub.setPadding(0, UI.dp(this, 1), 0, 0);
        titles.addView(sub);
        header.addView(titles, tlp);
        header.addView(UI.icon(this, R.drawable.ic_speed, 24, UI.INDIGO));
        root.addView(header);

        ScrollView sv = new ScrollView(this);
        sv.setVerticalScrollBarEnabled(false);
        col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(UI.dp(this, 20), UI.dp(this, 6), UI.dp(this, 20), UI.dp(this, 24));
        sv.addView(col);
        root.addView(sv, new LinearLayout.LayoutParams(-1, 0, 1));

        // 0) MASTER Turbo
        buildTurbo();

        // 1) Doc cau hinh may
        LinearLayout s1 = section("Đọc cấu hình máy", R.drawable.ic_cpu, UI.INDIGO);
        specBox = new LinearLayout(this);
        specBox.setOrientation(LinearLayout.VERTICAL);
        s1.addView(specBox);
        TextView refresh = UI.button(this, "Làm mới thông số", UI.SURFACE, UI.INDIGO, true);
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(-1, -2);
        rlp.topMargin = UI.dp(this, 12);
        refresh.setOnClickListener(v -> { fillSpecs(); toast("Đã cập nhật thông số"); });
        s1.addView(refresh, rlp);

        // 2) Tang FPS
        LinearLayout s2 = section("Tăng FPS — tần số quét", R.drawable.ic_speed, UI.GREEN);
        fpsNow = UI.text(this, "", 12, UI.GREEN_TEXT, true);
        fpsNow.setPadding(UI.dp(this, 2), 0, 0, UI.dp(this, 8));
        s2.addView(fpsNow);
        toggleRow(s2, R.drawable.ic_speed, UI.GREEN, "Tần số quét cao",
                "Yêu cầu mức Hz cao nhất màn hình hỗ trợ", "highFps", false);
        note(s2, "Áp dụng cho cửa sổ trình duyệt khi mở hồ sơ. Chỉ tăng FPS nếu màn hình hỗ trợ 90/120Hz; máy 60Hz sẽ giữ nguyên.");

        // 3) Fix lag
        LinearLayout s3 = section("Fix lag — tối ưu WebView", R.drawable.ic_memory, UI.BLUE);
        toggleRow(s3, R.drawable.ic_memory, UI.BLUE, "Tăng tốc phần cứng",
                "Vẽ trang bằng GPU (tắt nếu máy bị lỗi hình)", "hwAccel", true);
        toggleRow(s3, R.drawable.ic_globe, UI.BLUE, "Chặn ảnh",
                "Tải nhanh hơn · tự bật lại ảnh cho captcha", "blockImg", false);
        LinearLayout btns = new LinearLayout(this);
        btns.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(-1, -2);
        blp.topMargin = UI.dp(this, 12);
        TextView cache = UI.button(this, "Xóa cache", UI.SURFACE, UI.BLUE, true);
        cache.setOnClickListener(v -> { clearWebCache(); fillSpecs(); toast("Đã xóa cache WebView"); });
        btns.addView(cache, weight());
        TextView ram = UI.button(this, "Dọn RAM", UI.SURFACE, UI.BLUE, true);
        LinearLayout.LayoutParams rmlp = weight();
        rmlp.leftMargin = UI.dp(this, 10);
        ram.setOnClickListener(v -> { System.gc(); fillSpecs(); toast("Đã dọn bộ nhớ · RAM trống: " + SysInfo.gb(SysInfo.ram(this)[1])); });
        btns.addView(ram, rmlp);
        s3.addView(btns, blp);
        note(s3, "Xóa cache dọn thư mục Cache/Code Cache của WebView (không đụng cookie/danh tính). Dọn RAM gợi ý thu hồi bộ nhớ tiến trình.");

        // 4) Ep xung / uu tien hieu nang
        LinearLayout s4 = section("Ép xung — ưu tiên hiệu năng", R.drawable.ic_cpu, UI.ORANGE);
        String gov = SysInfo.governor();
        spec(s4, "CPU GOVERNOR", gov != null ? gov : "Không đọc được (cần quyền)");
        boolean hintOk = SysInfo.hintSupported();
        toggleRow(s4, R.drawable.ic_speed, UI.ORANGE, "Ưu tiên hiệu năng CPU",
                hintOk ? "PerformanceHintManager · Android 12+" : "Không hỗ trợ (cần Android 12+)", "perfHint", false);
        note(s4, "Ép xung thật cần root — app thường không đổi được xung nhịp. Mục này chỉ gửi gợi ý ưu tiên hiệu năng tới hệ thống (API 31+), không phải ép xung.");
        if (!hintOk) disableLastToggle(s4);

        // 5) Giam ping / do ping
        LinearLayout s5 = section("Giảm ping — đo độ trễ", R.drawable.ic_ping, UI.VIOLET);
        label(s5, "MÁY CHỦ ĐO (HOST:PORT)");
        pingHost = input("1.1.1.1:443", "host:port · ví dụ 1.1.1.1:443");
        s5.addView(pingHost);
        TextView pingBtn = UI.gradButton(this, "Đo ping");
        LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(-1, -2);
        plp.topMargin = UI.dp(this, 12);
        pingBtn.setOnClickListener(v -> runPing());
        s5.addView(pingBtn, plp);
        pingResult = UI.text(this, "Chưa đo. Bấm «Đo ping» để xem độ trễ thật (ms).", 12, UI.MUTED, false);
        pingResult.setPadding(UI.dp(this, 2), UI.dp(this, 10), 0, 0);
        s5.addView(pingResult);
        note(s5, "Ping phụ thuộc khoảng cách máy chủ/proxy và chất lượng mạng — không có nút «giảm» ảo. Đây là số đo TCP thật từ thiết bị (không qua proxy WebView). Muốn ping thấp: chọn proxy gần bạn hơn.");

        setContentView(root);
        fillSpecs();
        SysInfo.applyRefresh(this, Store.perf(this, "highFps", false));
        updateFps();
    }

    @Override protected void onResume() { super.onResume(); Master.sync(this); }

    // ---------- MASTER Turbo ----------
    void buildTurbo() {
        LinearLayout t = section("MASTER Turbo — tăng tốc proxy", R.drawable.ic_robot, UI.CYAN);
        if (!UI.master) {
            note(t, "Bật nút MASTER ở màn hình chính để dùng Turbo: ảnh/CSS/font/video tải thẳng qua WiFi (không qua proxy), "
                    + "chặn quảng cáo, ảnh tải lười. Trang HTML và dữ liệu đăng nhập vẫn đi qua proxy nên web vẫn thấy IP proxy.");
            return;
        }
        label(t, "CHIA LUỒNG PROXY");
        final LinearLayout seg = new LinearLayout(this);
        seg.setBackground(UI.round(this, UI.SEG, 14));
        seg.setPadding(UI.dp(this, 4), UI.dp(this, 4), UI.dp(this, 4), UI.dp(this, 4));
        final String[] names = {"Tắt", "Cân bằng", "Tối đa"};
        final TextView[] items = new TextView[3];
        final TextView desc = UI.text(this, "", 11, UI.MUTED, false);
        final Runnable paint = () -> {
            int cur = Master.split(this);
            for (int i = 0; i < 3; i++) {
                boolean on = i == cur;
                items[i].setBackground(on ? UI.ripple(UI.round(this, UI.CHIP_ACTIVE, 11)) : null);
                items[i].setTextColor(on ? UI.CYAN : UI.MUTED);
            }
            desc.setText(cur == 2 ? "TỐI ĐA: mọi ảnh/CSS/font/video (https) tải thẳng WiFi → nhanh nhất, nhưng máy chủ file tĩnh sẽ thấy IP THẬT của bạn. Không dùng nếu cần ẩn danh tuyệt đối."
                    : cur == 1 ? "CÂN BẰNG: chỉ file tĩnh của tên miền KHÁC trang đang xem (CDN, quảng cáo) đi thẳng WiFi; file của chính trang vẫn qua proxy."
                    : "TẮT (khuyên dùng): mọi thứ đều đi qua proxy, IP thật không lộ. Vẫn nhanh nhờ cache RAM, chặn quảng cáo và ảnh tải lười.");
        };
        for (int i = 0; i < 3; i++) {
            final int k = i;
            TextView it = UI.text(this, names[i], 12, UI.MUTED, true);
            it.setGravity(Gravity.CENTER);
            it.setClickable(true);
            it.setOnClickListener(v -> { Store.prefs(this).edit().putInt("tSplitV2", k).apply(); paint.run(); });
            items[i] = it;
            seg.addView(it, new LinearLayout.LayoutParams(0, UI.dp(this, 40), 1));
        }
        t.addView(seg);
        desc.setPadding(UI.dp(this, 2), UI.dp(this, 8), 0, 0);
        desc.setLineSpacing(UI.dp(this, 2), 1f);
        t.addView(desc);
        paint.run();
        toggleRow(t, R.drawable.ic_shield, UI.RED, "Chặn quảng cáo & tracker", "Giảm tải, trang lên nhanh hơn", "tAds", true);
        toggleRow(t, R.drawable.ic_globe, UI.BLUE, "Ảnh tải lười", "Chỉ tải ảnh khi cuộn tới · mượt, nhẹ RAM", "tLazy", true);
        toggleRow(t, R.drawable.ic_memory, UI.VIOLET, "Nền tối web mặc định", "Đảo màu trang khi mở hồ sơ", "tDark", false);
        note(t, "Cài đặt Turbo áp dụng khi mở lại hồ sơ. Captcha luôn đi qua proxy để IP nhất quán. Tải trực tiếp có bộ nhớ đệm trên đĩa (64MB) nên lần sau vào lại nhanh hơn.");
    }

    void fillSpecs() {
        specBox.removeAllViews();
        DisplayMetrics dm = SysInfo.metrics(this);
        long[] ram = SysInfo.ram(this);
        long[] sto = SysInfo.storage();
        int bat = SysInfo.batteryLevel(this);
        String freq = SysInfo.cpuFreq();
        spec(specBox, "THIẾT BỊ", SysInfo.deviceName());
        spec(specBox, "ANDROID", SysInfo.androidVersion());
        spec(specBox, "CPU", SysInfo.cpuName() + " · " + SysInfo.cores() + " nhân");
        spec(specBox, "XUNG NHỊP", freq != null ? freq : "Không đọc được (cần quyền)");
        spec(specBox, "RAM", SysInfo.gb(ram[0]) + " · trống " + SysInfo.gb(ram[1]));
        spec(specBox, "MÀN HÌNH", dm.widthPixels + "×" + dm.heightPixels + " · " + dm.densityDpi + "dpi");
        spec(specBox, "TẦN SỐ QUÉT", Math.round(SysInfo.refreshRate(this)) + "Hz · tối đa " + Math.round(SysInfo.maxRefreshRate(this)) + "Hz");
        spec(specBox, "PIN", (bat >= 0 ? bat + "%" : "—") + " · " + SysInfo.batteryStatus(this));
        spec(specBox, "NHIỆT ĐỘ máy", SysInfo.thermal(this));
        spec(specBox, "BỘ NHỚ", SysInfo.gb(sto[1]) + " trống / " + SysInfo.gb(sto[0]));
        spec(specBox, "WEBVIEW", SysInfo.webViewVersion());
        updateFps();
    }

    void updateFps() {
        if (fpsNow == null) return;
        boolean on = Store.perf(this, "highFps", false);
        int cur = Math.round(SysInfo.refreshRate(this));
        int max = Math.round(SysInfo.maxRefreshRate(this));
        fpsNow.setText(on ? "Đang yêu cầu " + max + "Hz (hiện tại " + cur + "Hz)"
                : "Hiện tại " + cur + "Hz · tối đa " + max + "Hz");
    }

    void runPing() {
        String hp = pingHost.getText().toString().trim();
        String host = hp; int port = 443;
        int c = hp.lastIndexOf(':');
        if (c > 0) {
            host = hp.substring(0, c).trim();
            try { port = Integer.parseInt(hp.substring(c + 1).trim()); } catch (Exception e) { port = 443; }
        }
        if (host.isEmpty()) { pingResult.setText("Nhập host trước đã."); return; }
        if (host.contains("://")) host = host.substring(host.indexOf("://") + 3);
        final String h = host; final int p = port;
        pingResult.setText("Đang đo " + h + ":" + p + " …");
        pingResult.setTextColor(UI.MUTED);
        new Thread(() -> {
            long[] r = new long[3];
            for (int i = 0; i < 3; i++) { r[i] = SysInfo.ping(h, p, 2500); try { Thread.sleep(120); } catch (Exception ignored) {} }
            runOnUiThread(() -> {
                long sum = 0; int ok = 0; StringBuilder sb = new StringBuilder();
                for (int i = 0; i < 3; i++) {
                    if (r[i] >= 0) { sum += r[i]; ok++; }
                    sb.append(r[i] >= 0 ? r[i] + "ms" : "lỗi");
                    if (i < 2) sb.append(" · ");
                }
                if (ok == 0) {
                    pingResult.setText("Không kết nối được " + h + ":" + p + " (mạng yếu hoặc máy chủ chặn).");
                    pingResult.setTextColor(UI.RED);
                } else {
                    long avg = sum / ok;
                    pingResult.setText("Trung bình " + avg + "ms  (" + sb + ")  · " + ok + "/3 lần thành công");
                    pingResult.setTextColor(avg < 60 ? UI.GREEN_TEXT : avg < 150 ? UI.AMBER_TEXT : UI.RED);
                }
            });
        }).start();
    }

    void clearWebCache() {
        File dd = getDataDir();
        File[] fs = dd.listFiles();
        if (fs != null) for (File f : fs)
            if (f.isDirectory() && f.getName().startsWith("app_webview")) delCache(f);
    }
    void delCache(File dir) {
        File[] cs = dir.listFiles();
        if (cs == null) return;
        for (File f : cs) {
            if (!f.isDirectory()) continue;
            String n = f.getName();
            if (n.equals("Cache") || n.equals("Code Cache") || n.equals("GPUCache")) MainActivity.rm(f);
            else delCache(f);
        }
    }

    // ---------- helpers ----------
    LinearLayout section(String title, int iconRes, int tint) {
        LinearLayout s = new LinearLayout(this);
        s.setOrientation(LinearLayout.VERTICAL);
        s.setBackground(UI.round(this, UI.SURFACE, 20));
        s.setElevation(UI.dp(this, 1));
        s.setPadding(UI.dp(this, 16), UI.dp(this, 14), UI.dp(this, 16), UI.dp(this, 16));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = UI.dp(this, 16);
        col.addView(s, lp);
        LinearLayout h = new LinearLayout(this);
        h.setGravity(Gravity.CENTER_VERTICAL);
        h.setPadding(0, 0, 0, UI.dp(this, 6));
        ImageView ic = UI.icon(this, iconRes, 30, tint);
        int p = UI.dp(this, 7);
        ic.setPadding(p, p, p, p);
        ic.setBackground(UI.round(this, UI.alpha(tint, 0x14), 9));
        h.addView(ic);
        TextView t = UI.text(this, title, 14, UI.TEXT, true);
        t.setPadding(UI.dp(this, 10), 0, 0, 0);
        h.addView(t);
        s.addView(h);
        return s;
    }

    void spec(LinearLayout parent, String label, String value) {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setPadding(UI.dp(this, 2), UI.dp(this, 6), UI.dp(this, 2), UI.dp(this, 6));
        TextView l = UI.text(this, label, 11, UI.MUTED, true);
        l.setLetterSpacing(0.04f);
        r.addView(l, new LinearLayout.LayoutParams(0, -2, 0.42f));
        TextView v = UI.text(this, value, 12, UI.TEXT, true);
        v.setGravity(Gravity.END);
        r.addView(v, new LinearLayout.LayoutParams(0, -2, 0.58f));
        parent.addView(r);
    }

    void toggleRow(LinearLayout parent, int iconRes, int tint, String title, String sub, String key, boolean def) {
        LinearLayout r = UI.menuRow(this, iconRes, tint, title, sub);
        r.setPadding(UI.dp(this, 2), UI.dp(this, 8), UI.dp(this, 2), UI.dp(this, 8));
        final Switch sw = new Switch(this);
        boolean init = Store.perf(this, key, def);
        sw.setChecked(init);
        tintSwitch(sw, tint, init);
        sw.setOnCheckedChangeListener((bv, checked) -> {
            Store.setPerf(this, key, checked);
            tintSwitch((Switch) bv, tint, checked);
            if ("highFps".equals(key)) {
                SysInfo.applyRefresh(this, checked);
                updateFps();
                toast(checked ? "Đã yêu cầu tần số quét cao" : "Đã về tần số quét mặc định");
            } else if ("perfHint".equals(key)) {
                toast(checked ? "Bật ưu tiên hiệu năng (mở trình duyệt để áp dụng)" : "Đã tắt ưu tiên hiệu năng");
            } else if (key.startsWith("t")) {
                toast("Đã lưu · áp dụng khi mở lại hồ sơ");
            } else if ("blockImg".equals(key)) {
                toast(checked ? "Sẽ chặn ảnh khi duyệt" : "Sẽ tải ảnh bình thường");
            } else {
                toast(checked ? "Đã bật tăng tốc phần cứng" : "Đã tắt tăng tốc phần cứng");
            }
        });
        r.addView(sw);
        r.setOnClickListener(v -> sw.toggle());
        parent.addView(r);
    }
    void tintSwitch(Switch sw, int tint, boolean on) {
        sw.setThumbTintList(ColorStateList.valueOf(on ? tint : UI.SW_OFF_THUMB));
        sw.setTrackTintList(ColorStateList.valueOf(on ? UI.alpha(tint, 0x55) : UI.SW_OFF_TRACK));
    }
    void disableLastToggle(LinearLayout parent) {
        int n = parent.getChildCount();
        for (int i = n - 1; i >= 0; i--) {
            View v = parent.getChildAt(i);
            if (v instanceof LinearLayout) {
                LinearLayout row = (LinearLayout) v;
                for (int j = 0; j < row.getChildCount(); j++)
                    if (row.getChildAt(j) instanceof Switch) {
                        Switch sw = (Switch) row.getChildAt(j);
                        sw.setEnabled(false);
                        row.setEnabled(false);
                        row.setAlpha(0.5f);
                        row.setOnClickListener(null);
                        return;
                    }
            }
        }
    }

    LinearLayout.LayoutParams weight() { return new LinearLayout.LayoutParams(0, -2, 1); }
    void label(LinearLayout parent, String s) {
        TextView t = UI.label(this, s);
        t.setPadding(UI.dp(this, 2), UI.dp(this, 10), 0, UI.dp(this, 6));
        parent.addView(t);
    }
    void note(LinearLayout parent, String s) {
        TextView t = UI.text(this, s, 11, UI.MUTED, false);
        t.setPadding(UI.dp(this, 2), UI.dp(this, 8), 0, 0);
        t.setLineSpacing(UI.dp(this, 2), 1f);
        parent.addView(t);
    }
    EditText input(String value, String hint) {
        EditText e = new EditText(this);
        e.setText(value); e.setHint(hint);
        e.setTextSize(14);
        e.setTextColor(UI.TEXT);
        e.setHintTextColor(UI.HINT);
        e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        e.setSingleLine(true);
        int p = UI.dp(this, 13);
        e.setPadding(p, p, p, p);
        e.setBackground(UI.fieldBg(this));
        return e;
    }
    void toast(String m) { Toast.makeText(this, m, Toast.LENGTH_SHORT).show(); }
}