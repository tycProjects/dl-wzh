package com.pb.browser;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class EditActivity extends Activity {
    int idx;
    JSONArray arr;
    EditText name, uuid, proxy, ua, lang, home, tz;
    TextView checkResult;
    String rtc = "off", ptype = "http";
    TextView chipOff, chipRelay, chipHttp, chipSocks;
    LinearLayout col;
    String deviceId = "auto"; JSONObject deviceFp; boolean settingDevice; String deviceUa = "";
    boolean noise = true; TextView chipNoiseOn, chipNoiseOff;   // v3.3.5: gay nhieu van tay (canvas/WebGL/audio/rects)
    TextView deviceName, deviceSub, uaSwitch; ImageView deviceIcon; int uaIdx;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        UI.init(this);
        UI.bars(this, UI.BG, UI.SURFACE, true, true);
        idx = getIntent().getIntExtra("idx", -1);
        arr = Store.load(this);
        JSONObject o = idx >= 0 && idx < arr.length() ? arr.optJSONObject(idx) : Store.create(arr.length() + 1);

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
        titles.addView(UI.text(this, idx >= 0 ? "Sửa hồ sơ" : "Hồ sơ mới", 20, UI.TEXT, true));
        TextView sub = UI.text(this, "Thiết lập một danh tính số riêng biệt", 11, UI.MUTED, false);
        sub.setPadding(0, UI.dp(this, 1), 0, 0);
        titles.addView(sub);
        header.addView(titles, tlp);
        header.addView(UI.icon(this, R.drawable.ic_shield, 22, UI.INDIGO));
        root.addView(header);

        ScrollView sv = new ScrollView(this);
        sv.setVerticalScrollBarEnabled(false);
        col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(UI.dp(this, 20), UI.dp(this, 6), UI.dp(this, 20), UI.dp(this, 20));
        sv.addView(col);
        root.addView(sv, new LinearLayout.LayoutParams(-1, 0, 1));

        // Section 1: danh tinh
        LinearLayout s1 = section("Danh tính hồ sơ", R.drawable.ic_badge, UI.INDIGO);
        name = field(s1, "TÊN HỒ SƠ", o.optString("name"), "Ví dụ: Công việc, Cá nhân…");
        name.setSingleLine(true);
        label(s1, "UUID — COOKIE, DỮ LIỆU & CANVAS RIÊNG");
        LinearLayout urow = new LinearLayout(this);
        urow.setGravity(Gravity.CENTER_VERTICAL);
        uuid = input(o.optString("uuid"), "");
        uuid.setTypeface(Typeface.MONOSPACE);
        uuid.setTextSize(12);
        uuid.setSingleLine(true);
        urow.addView(uuid, new LinearLayout.LayoutParams(0, -2, 1));
        ImageView regen = UI.iconBtn(this, R.drawable.ic_refresh, UI.INDIGO, UI.alpha(UI.INDIGO, 0x12), 46, 12);
        regen.setOnClickListener(v -> uuid.setText(UUID.randomUUID().toString()));
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(UI.dp(this, 46), UI.dp(this, 46));
        rlp.leftMargin = UI.dp(this, 8);
        urow.addView(regen, rlp);
        s1.addView(urow);

        // Section 2: ket noi
        LinearLayout s2 = section("Kết nối mạng", R.drawable.ic_server, UI.BLUE);
        label(s2, "LOẠI PROXY");
        LinearLayout pseg = new LinearLayout(this);
        pseg.setBackground(UI.round(this, UI.SEG, 14));
        pseg.setPadding(UI.dp(this, 4), UI.dp(this, 4), UI.dp(this, 4), UI.dp(this, 4));
        chipHttp = segItem("HTTP");
        chipSocks = segItem("SOCKS5");
        chipHttp.setOnClickListener(v -> setPtype("http"));
        chipSocks.setOnClickListener(v -> setPtype("socks5"));
        pseg.addView(chipHttp, new LinearLayout.LayoutParams(0, UI.dp(this, 40), 1));
        pseg.addView(chipSocks, new LinearLayout.LayoutParams(0, UI.dp(this, 40), 1));
        s2.addView(pseg);
        proxy = field(s2, "ĐỊA CHỈ PROXY", o.optString("proxy"), "host:port  ·  user:pass@host:port\nhost:port:user:pass  ·  socks5://host:port");
        TextView pNote = UI.text(this, "HTTP hỗ trợ user:pass. SOCKS5 chỉ dùng host:port — WebView không hỗ trợ xác thực SOCKS5. Để trống = kết nối trực tiếp.", 11, UI.MUTED, false);
        pNote.setPadding(UI.dp(this, 2), UI.dp(this, 8), 0, 0);
        s2.addView(pNote);
        setPtype(Store.schemeOf(o.optString("proxy"), o.optString("ptype", "http")));
        TextView checkBtn = UI.button(this, "Kiểm tra proxy", UI.INDIGO, UI.INDIGO, true);
        LinearLayout.LayoutParams cbl = new LinearLayout.LayoutParams(-1, -2);
        cbl.topMargin = UI.dp(this, 10);
        s2.addView(checkBtn, cbl);
        checkResult = UI.text(this, "Bấm để xem IP, quốc gia, múi giờ và độ trễ của proxy (đi qua đúng proxy đang nhập).", 11, UI.MUTED, false);
        checkResult.setPadding(UI.dp(this, 2), UI.dp(this, 8), 0, 0);
        s2.addView(checkResult);
        checkBtn.setOnClickListener(v -> runCheck(checkBtn));
        tz = field(s2, "MÚI GIỜ (ĐỒNG BỘ THEO IP PROXY)", o.optString("tz", ""), "Để trống = tự đồng bộ theo IP proxy · hoặc nhập Asia/Ho_Chi_Minh");
        tz.setSingleLine(true);
        TextView tzNote = UI.text(this, "Để trống: mỗi lần mở hồ sơ app tự dò múi giờ theo IP proxy (khuyên dùng). Nhập tay chỉ khi cần cố định.", 11, UI.MUTED, false);
        tzNote.setPadding(UI.dp(this, 2), UI.dp(this, 8), 0, 0);
        s2.addView(tzNote);
        home = field(s2, "TRANG CHỦ", o.optString("url", "https://www.google.com"), "https://www.google.com");
        home.setSingleLine(true);

        // Section 3: dau van tay
        LinearLayout s3 = section("Dấu vân tay trình duyệt", R.drawable.ic_fingerprint, UI.VIOLET);
        label(s3, "THIẾT BỊ MẪU — UA · UUID · FINGERPRINT TỰ ĐỒNG BỘ");
        LinearLayout deviceBtn = new LinearLayout(this);
        deviceBtn.setGravity(Gravity.CENTER_VERTICAL);
        deviceBtn.setBackground(UI.ripple(UI.outline(this, UI.SURFACE, 12)));
        deviceBtn.setPadding(UI.dp(this, 13), UI.dp(this, 12), UI.dp(this, 13), UI.dp(this, 12));
        deviceBtn.setClickable(true);
        deviceIcon = UI.icon(this, R.drawable.ic_phone, 22, UI.VIOLET);
        deviceBtn.addView(deviceIcon);
        LinearLayout dcol = new LinearLayout(this);
        dcol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(0, -2, 1);
        dlp.leftMargin = UI.dp(this, 11);
        deviceName = UI.text(this, "Tự động", 14, UI.TEXT, true);
        deviceSub = UI.text(this, "UA thật của WebView", 11, UI.MUTED, false);
        deviceSub.setPadding(0, UI.dp(this, 1), 0, 0);
        dcol.addView(deviceName); dcol.addView(deviceSub);
        deviceBtn.addView(dcol, dlp);
        deviceBtn.addView(UI.icon(this, R.drawable.ic_chevron, 18, UI.MUTED));
        deviceBtn.setOnClickListener(v -> pickDevice());
        s3.addView(deviceBtn, new LinearLayout.LayoutParams(-1, -2));
        ua = field(s3, "USER-AGENT", o.optString("ua"), "Để trống = UA thật của WebView");
        ua.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                if (settingDevice) return;
                if (deviceUa != null && s.toString().equals(deviceUa)) return;
                deviceId = ""; deviceFp = null; deviceUa = null;
                setDeviceLabel("Tùy chỉnh", "UA tự nhập · fingerprint theo UA", R.drawable.ic_desktop);
            }
        });
        uaSwitch = UI.button(this, "Chuyển đổi UA", UI.VIOLET, UI.VIOLET, true);
        LinearLayout.LayoutParams usl = new LinearLayout.LayoutParams(-1, -2);
        usl.topMargin = UI.dp(this, 8);
        s3.addView(uaSwitch, usl);
        uaSwitch.setOnClickListener(v -> nextUa());
        lang = field(s3, "NGÔN NGỮ", o.optString("lang", "vi-VN"), "vi-VN, en-US …");
        lang.setSingleLine(true);
        label(s3, "WEBRTC");
        LinearLayout seg = new LinearLayout(this);
        seg.setBackground(UI.round(this, UI.SEG, 14));
        seg.setPadding(UI.dp(this, 4), UI.dp(this, 4), UI.dp(this, 4), UI.dp(this, 4));
        chipOff = segItem("Tắt hẳn");
        chipRelay = segItem("Relay · không lộ IP");
        chipOff.setOnClickListener(v -> setRtc("off"));
        chipRelay.setOnClickListener(v -> setRtc("relay"));
        seg.addView(chipOff, new LinearLayout.LayoutParams(0, UI.dp(this, 40), 1));
        seg.addView(chipRelay, new LinearLayout.LayoutParams(0, UI.dp(this, 40), 1));
        s3.addView(seg);
        TextView rtcNote = UI.text(this, "Tắt hẳn chặn mọi kết nối WebRTC. Relay chỉ cho phép đi qua TURN để giấu IP thật.", 11, UI.MUTED, false);
        rtcNote.setPadding(UI.dp(this, 2), UI.dp(this, 8), 0, 0);
        s3.addView(rtcNote);
        setRtc("relay".equals(o.optString("rtc")) ? "relay" : "off");

        label(s3, "GÂY NHIỄU VÂN TAY");
        LinearLayout nseg = new LinearLayout(this);
        nseg.setBackground(UI.round(this, UI.SEG, 14));
        nseg.setPadding(UI.dp(this, 4), UI.dp(this, 4), UI.dp(this, 4), UI.dp(this, 4));
        chipNoiseOn = segItem("Bật");
        chipNoiseOff = segItem("Tắt");
        chipNoiseOn.setOnClickListener(v -> setNoise(true));
        chipNoiseOff.setOnClickListener(v -> setNoise(false));
        nseg.addView(chipNoiseOn, new LinearLayout.LayoutParams(0, UI.dp(this, 40), 1));
        nseg.addView(chipNoiseOff, new LinearLayout.LayoutParams(0, UI.dp(this, 40), 1));
        s3.addView(nseg);
        TextView noiseNote = UI.text(this, "Thêm nhiễu cố định theo UUID vào canvas, WebGL, âm thanh và kích thước phần tử — mỗi hồ sơ một dấu vân tay riêng. Tạm tắt nếu một trang đăng nhập/captcha chạy lạ.", 11, UI.MUTED, false);
        noiseNote.setPadding(UI.dp(this, 2), UI.dp(this, 8), 0, 0);
        s3.addView(noiseNote);
        setNoise(o.optBoolean("noise", true));

        // Khoi phua trang thai thiet bi tu ho so da luu
        deviceFp = o.optJSONObject("fp");
        String savedDev = o.optString("device", "");
        Devices.Dev sd = Devices.byId(savedDev);
        if ("desktop".equals(savedDev)) { deviceId = "desktop"; deviceUa = Devices.WINDOWS_UA; setDeviceLabel("Máy tính Windows", "1920×1080 · 8GB · Win32", R.drawable.ic_desktop); }
        else if (sd != null) { deviceId = sd.id; deviceUa = sd.ua; setDeviceLabel(sd.name, sd.sub(), R.drawable.ic_phone); }
        else if (!o.optString("ua").trim().isEmpty()) { deviceId = ""; deviceUa = null; setDeviceLabel("Tùy chỉnh", "UA tự nhập · fingerprint theo UA", R.drawable.ic_desktop); }
        else { deviceId = "auto"; deviceFp = null; deviceUa = ""; setDeviceLabel("Tự động", "UA thật của WebView", R.drawable.ic_phone); }

        uaIdx = sd != null ? Store.uaIdx(this, o) : 0;
        if (sd != null) deviceUa = Devices.uaAt(sd, uaIdx);
        updateUaBtn();

        // Thanh luu co dinh
        LinearLayout bar = new LinearLayout(this);
        bar.setBackgroundColor(UI.SURFACE);
        bar.setElevation(UI.dp(this, 8));
        bar.setPadding(UI.dp(this, 20), UI.dp(this, 12), UI.dp(this, 20), UI.dp(this, 12));
        LinearLayout save = new LinearLayout(this);
        save.setGravity(Gravity.CENTER);
        save.setBackground(UI.ripple(UI.gradient(this, UI.INDIGO, UI.BLUE, 25)));
        save.setClickable(true);
        save.addView(UI.icon(this, R.drawable.ic_check, 18, UI.WHITE));
        TextView st = UI.text(this, "Lưu hồ sơ", 15, UI.WHITE, true);
        st.setPadding(UI.dp(this, 8), 0, 0, 0);
        save.addView(st);
        save.setOnClickListener(v -> save(o));
        bar.addView(save, new LinearLayout.LayoutParams(-1, UI.dp(this, 50)));
        root.addView(bar);
        setContentView(root);
    }

    @Override protected void onResume() { super.onResume(); Master.sync(this); }

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
        h.setPadding(0, 0, 0, UI.dp(this, 4));
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

    TextView segItem(String s) {
        TextView t = UI.text(this, s, 12, UI.MUTED, true);
        t.setGravity(Gravity.CENTER);
        t.setClickable(true);
        return t;
    }

    void setRtc(String v) {
        rtc = v;
        boolean off = "off".equals(v);
        chipOff.setBackground(off ? UI.ripple(UI.round(this, UI.SURFACE, 11)) : null);
        chipOff.setTextColor(off ? UI.INDIGO : UI.MUTED);
        chipOff.setElevation(off ? UI.dp(this, 2) : 0);
        chipRelay.setBackground(!off ? UI.ripple(UI.round(this, UI.SURFACE, 11)) : null);
        chipRelay.setTextColor(!off ? UI.INDIGO : UI.MUTED);
        chipRelay.setElevation(!off ? UI.dp(this, 2) : 0);
    }

    void setNoise(boolean on) {
        noise = on;
        chipNoiseOn.setBackground(on ? UI.ripple(UI.round(this, UI.SURFACE, 11)) : null);
        chipNoiseOn.setTextColor(on ? UI.INDIGO : UI.MUTED);
        chipNoiseOn.setElevation(on ? UI.dp(this, 2) : 0);
        chipNoiseOff.setBackground(!on ? UI.ripple(UI.round(this, UI.SURFACE, 11)) : null);
        chipNoiseOff.setTextColor(!on ? UI.INDIGO : UI.MUTED);
        chipNoiseOff.setElevation(!on ? UI.dp(this, 2) : 0);
    }

    void setPtype(String v) {
        ptype = v;
        boolean http = "http".equals(v);
        chipHttp.setBackground(http ? UI.ripple(UI.round(this, UI.SURFACE, 11)) : null);
        chipHttp.setTextColor(http ? UI.INDIGO : UI.MUTED);
        chipHttp.setElevation(http ? UI.dp(this, 2) : 0);
        chipSocks.setBackground(!http ? UI.ripple(UI.round(this, UI.SURFACE, 11)) : null);
        chipSocks.setTextColor(!http ? UI.INDIGO : UI.MUTED);
        chipSocks.setElevation(!http ? UI.dp(this, 2) : 0);
    }

    void setDeviceLabel(String n, String sub, int iconRes) {
        deviceName.setText(n); deviceSub.setText(sub); deviceIcon.setImageResource(iconRes);
    }

    int brandTint(String brand) {
        switch (brand) {
            case "Samsung": return UI.BLUE;
            case "Xiaomi · Redmi": return UI.ORANGE;
            case "OPPO": return UI.GREEN;
            case "Vivo": return UI.BLUE;
            case "Sony": return UI.VIOLET;
            case "Google Pixel": return UI.TEAL;
            case "OnePlus": return UI.RED;
            case "Realme": return UI.AMBER;
            default: return UI.TEXT;
        }
    }

    void pickDevice() {
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        TextView t = UI.text(this, "Chọn thiết bị mẫu", 16, UI.TEXT, true);
        t.setPadding(UI.dp(this, 20), UI.dp(this, 6), UI.dp(this, 20), UI.dp(this, 2));
        body.addView(t);
        TextView note = UI.text(this, "UA, UUID và fingerprint (màn hình, RAM, nhân, cảm ứng…) sẽ đồng bộ theo thiết bị.", 11, UI.MUTED, false);
        note.setPadding(UI.dp(this, 20), 0, UI.dp(this, 20), UI.dp(this, 8));
        body.addView(note);
        ScrollView sv = new ScrollView(this);
        sv.setVerticalScrollBarEnabled(false);
        LinearLayout rows = new LinearLayout(this);
        rows.setOrientation(LinearLayout.VERTICAL);
        sv.addView(rows);
        body.addView(sv, new LinearLayout.LayoutParams(-1, UI.dp(this, 430)));
        final Dialog[] dh = new Dialog[1];

        LinearLayout auto = UI.menuRow(this, R.drawable.ic_refresh, UI.GREEN, "Tự động (khuyên dùng)", "UA thật của WebView · không giả thiết bị");
        if ("auto".equals(deviceId)) auto.addView(UI.icon(this, R.drawable.ic_check, 18, UI.GREEN));
        auto.setOnClickListener(v -> { dh[0].dismiss(); chooseAuto(); });
        rows.addView(auto);
        LinearLayout desk = UI.menuRow(this, R.drawable.ic_desktop, UI.BLUE, "Máy tính Windows", "1920×1080 · 8GB · Win32");
        if ("desktop".equals(deviceId)) desk.addView(UI.icon(this, R.drawable.ic_check, 18, UI.GREEN));
        desk.setOnClickListener(v -> { dh[0].dismiss(); chooseDesktop(); });
        rows.addView(desk);

        for (Map.Entry<String, List<Devices.Dev>> e : Devices.GROUPS.entrySet()) {
            TextView gh = UI.label(this, e.getKey().toUpperCase());
            gh.setPadding(UI.dp(this, 20), UI.dp(this, 14), UI.dp(this, 20), UI.dp(this, 4));
            rows.addView(gh);
            int tint = brandTint(e.getKey());
            for (final Devices.Dev dv : e.getValue()) {
                LinearLayout r = UI.menuRow(this, R.drawable.ic_phone, tint, dv.name, dv.sub());
                if (dv.id.equals(deviceId)) r.addView(UI.icon(this, R.drawable.ic_check, 18, UI.GREEN));
                r.setOnClickListener(v -> { dh[0].dismiss(); chooseDevice(dv); });
                rows.addView(r);
            }
        }
            dh[0] = UI.sheet(this, body);
    }


    void runCheck(final TextView btn) {
        String px = proxy.getText().toString().replaceAll("\\s", "");
        if (!px.isEmpty() && !validProxy(px)) { proxy.setError("Sai định dạng proxy"); proxy.requestFocus(); return; }
        btn.setText("Đang kiểm tra…");
        checkResult.setTextColor(UI.MUTED);
        checkResult.setText(px.isEmpty() ? "Đang tra IP trực tiếp (không proxy)…" : "Đang kết nối qua proxy…");
        ProxyCheck.run(px, ptype, (r, err) -> {
            btn.setText("Kiểm tra proxy");
            if (err != null) {
                checkResult.setTextColor(UI.RED);
                checkResult.setText("Proxy lỗi: " + err);
                return;
            }
            String zone = r.optString("timezone", "");
            boolean flagged = r.optBoolean("proxy") || r.optBoolean("hosting");
            StringBuilder sb = new StringBuilder();
            sb.append("IP: ").append(r.optString("query")).append("\n");
            sb.append("Quốc gia: ").append(r.optString("country")).append(" (").append(r.optString("countryCode")).append(")\n");
            sb.append("Múi giờ: ").append(zone).append("\n");
            sb.append("ISP: ").append(r.optString("isp")).append("\n");
            sb.append("Độ trễ: ").append(r.optLong("ms")).append(" ms");
            if (r.optBoolean("mobile")) sb.append("\nLoại: mạng di động");
            if (r.optBoolean("hosting")) sb.append("\nCảnh báo: IP thuộc trung tâm dữ liệu/hosting");
            if (r.optBoolean("proxy")) sb.append("\nCảnh báo: IP bị đánh dấu là proxy/VPN");
            checkResult.setTextColor(flagged ? UI.AMBER_TEXT : UI.GREEN_TEXT);
            checkResult.setText(sb.toString());
            // Khong tu dien vao o "Mui gio": dien vao se GHIM mui gio, doi proxy sang nuoc khac van giu mui gio cu (lech IP).
            if (!zone.isEmpty() && tz.getText().toString().trim().isEmpty())
                Toast.makeText(this, "Múi giờ theo IP: " + zone + " · để trống ô múi giờ = tự đồng bộ", Toast.LENGTH_LONG).show();
        });
    }

    boolean validTz(String z) {
        if (z.isEmpty()) return true;
        for (String id : java.util.TimeZone.getAvailableIDs()) if (id.equals(z)) return true;
        return false;
    }

    void updateUaBtn() {
        if (uaSwitch != null) uaSwitch.setText("Chuyển đổi UA · " + (uaIdx + 1) + "/" + Devices.UA_COUNT);
    }
    void nextUa() {
        Devices.Dev dv = Devices.byId(deviceId);
        if (dv == null) { Toast.makeText(this, "Hãy chọn thiết bị mẫu (iPhone, Samsung…) trước", Toast.LENGTH_SHORT).show(); return; }
        uaIdx = (uaIdx + 1) % Devices.UA_COUNT;
        String u = Devices.uaAt(dv, uaIdx);
        settingDevice = true; deviceUa = u; ua.setText(u); settingDevice = false;
        updateUaBtn();
        Toast.makeText(this, "UA " + (uaIdx + 1) + "/" + Devices.UA_COUNT + " · nhớ bấm Lưu hồ sơ", Toast.LENGTH_SHORT).show();
    }

    void chooseDevice(Devices.Dev dv) {
        uaIdx = 0; updateUaBtn();
        settingDevice = true;
        deviceId = dv.id; deviceFp = dv.fp(); deviceUa = dv.ua;
        ua.setText(dv.ua);
        uuid.setText(UUID.randomUUID().toString());
        setDeviceLabel(dv.name, dv.sub(), R.drawable.ic_phone);
        settingDevice = false;
        Toast.makeText(this, "Đã chọn " + dv.name + " · UUID & fingerprint mới", Toast.LENGTH_SHORT).show();
    }
    void chooseAuto() {
        uaIdx = 0; updateUaBtn();
        settingDevice = true;
        deviceId = "auto"; deviceFp = null; deviceUa = "";
        ua.setText("");
        uuid.setText(UUID.randomUUID().toString());
        setDeviceLabel("Tự động", "UA thật của WebView", R.drawable.ic_phone);
        settingDevice = false;
        Toast.makeText(this, "Dùng UA thật · UUID mới", Toast.LENGTH_SHORT).show();
    }
    void chooseDesktop() {
        uaIdx = 0; updateUaBtn();
        settingDevice = true;
        deviceId = "desktop"; deviceFp = Devices.desktopFp(); deviceUa = Devices.WINDOWS_UA;
        ua.setText(Devices.WINDOWS_UA);
        uuid.setText(UUID.randomUUID().toString());
        setDeviceLabel("Máy tính Windows", "1920×1080 · 8GB · Win32", R.drawable.ic_desktop);
        settingDevice = false;
        Toast.makeText(this, "Đã chọn máy tính Windows · UUID mới", Toast.LENGTH_SHORT).show();
    }

    boolean validProxy(String s) {
        if (s.isEmpty()) return true;
        String rest = s.trim();
        int i = rest.indexOf("://");
        if (i > 0) rest = rest.substring(i + 3);
        String hp = rest.contains("@") ? rest.substring(rest.lastIndexOf('@') + 1) : rest;
        String[] p = hp.split(":");
        try {
            if (p.length == 4) return !p[0].isEmpty() && !p[2].isEmpty() && port(p[1]);
            if (p.length == 2) return !p[0].isEmpty() && port(p[1]);
        } catch (Exception ignored) {}
        return false;
    }
    boolean port(String s) { int v = Integer.parseInt(s); return v > 0 && v < 65536; }

    void save(JSONObject old) {
        try {
            String px = proxy.getText().toString().replaceAll("\\s", "");
            if (!validProxy(px)) {
                proxy.setError("Sai định dạng. Ví dụ: host:port hoặc user:pass@host:port");
                proxy.requestFocus();
                return;
            }
            String type = Store.schemeOf(px, ptype);
            if ("socks5".equals(type) && !px.isEmpty()) {
                String rest = px.contains("://") ? px.substring(px.indexOf("://") + 3) : px;
                if (rest.contains("@") || rest.split(":").length == 4) {
                    proxy.setError("SOCKS5 không hỗ trợ user:pass — bỏ phần xác thực hoặc chọn HTTP");
                    proxy.requestFocus();
                    return;
                }
            }
            String tzv = tz.getText().toString().trim();
            if (!validTz(tzv)) {
                tz.setError("Múi giờ không hợp lệ. Ví dụ: Asia/Ho_Chi_Minh");
                tz.requestFocus();
                return;
            }
            JSONObject o = new JSONObject(old.toString());   // giu nguyen cac truong khac cua ho so
            o.remove("fp");
            o.put("tz", tzv);
            String id = uuid.getText().toString().trim().replaceAll("[^A-Za-z0-9-]", "");
            if (id.isEmpty()) id = UUID.randomUUID().toString();
            o.put("name", name.getText().toString().trim().isEmpty() ? "Hồ sơ" : name.getText().toString().trim());
            o.put("uuid", id);
            o.put("proxy", px);
            o.put("ptype", type);
            o.put("ua", ua.getText().toString().trim());
            o.put("device", deviceId);
            int saveIdx = Devices.byId(deviceId) != null ? uaIdx : 0;
            o.put("uaIdx", saveIdx);
            Store.setUaIdx(this, id, saveIdx);
            if (deviceFp != null) o.put("fp", deviceFp);
            o.put("lang", lang.getText().toString().trim().isEmpty() ? "vi-VN" : lang.getText().toString().trim());
            o.put("rtc", rtc);
            o.put("noise", noise);
            // v3.3.5: trang chu chi cho http/https (truoc day nhap "javascript:..." hoac "abc.com" khong scheme deu duoc luu)
            String hv = home.getText().toString().trim();
            if (hv.isEmpty()) hv = "https://www.google.com";
            else if (!hv.matches("(?i)^[a-z][a-z0-9+.-]*://.*")) hv = "https://" + hv;
            if (!hv.matches("(?i)^https?://.+")) {
                home.setError("Trang chủ phải bắt đầu bằng http:// hoặc https://");
                home.requestFocus();
                return;
            }
            o.put("url", hv);
            if (idx >= 0) arr.put(idx, o); else arr.put(o);
            Store.save(this, arr);
            Toast.makeText(this, "Đã lưu hồ sơ", Toast.LENGTH_SHORT).show();
            finish();
        } catch (Exception e) { Toast.makeText(this, "Lỗi: " + e, Toast.LENGTH_LONG).show(); }
    }

    void label(LinearLayout parent, String s) {
        TextView t = UI.label(this, s);
        t.setPadding(UI.dp(this, 2), UI.dp(this, 12), 0, UI.dp(this, 6));
        parent.addView(t);
    }
    EditText input(String value, String hint) {
        EditText e = new EditText(this);
        e.setText(value); e.setHint(hint);
        e.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        e.setTextColor(UI.TEXT);
        e.setHintTextColor(UI.HINT);
        e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_VARIATION_URI);
        int p = UI.dp(this, 13);
        e.setPadding(p, p, p, p);
        e.setBackground(UI.fieldBg(this));
        return e;
    }
    EditText field(LinearLayout parent, String label, String value, String hint) {
        label(parent, label);
        EditText e = input(value, hint);
        parent.addView(e, new LinearLayout.LayoutParams(-1, -2));
        return e;
    }
}