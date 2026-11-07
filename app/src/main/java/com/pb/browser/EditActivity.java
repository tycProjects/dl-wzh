package com.pb.browser;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.util.TypedValue;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.UUID;

public class EditActivity extends Activity {
    static final String[] UA_NAMES = {"Tự động (khuyên dùng)", "Android Chrome", "Windows Chrome", "macOS Chrome", "iPhone Safari"};
    static final String[] UA_VALUES = {"",
        "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Mobile Safari/537.36",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36",
        "Mozilla/5.0 (iPhone; CPU iPhone OS 17_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.5 Mobile/15E148 Safari/604.1"};

    int idx;
    JSONArray arr;
    EditText name, uuid, proxy, ua, lang, home;
    String rtc = "off";
    TextView chipOff, chipRelay;
    LinearLayout col;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(UI.BG);
        getWindow().setNavigationBarColor(UI.BG);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        idx = getIntent().getIntExtra("idx", -1);
        arr = Store.load(this);
        JSONObject o = idx >= 0 && idx < arr.length() ? arr.optJSONObject(idx) : Store.create(arr.length() + 1);

        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(UI.BG);
        sv.setFitsSystemWindows(true);
        col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        int p = UI.dp(this, 20);
        col.setPadding(p, UI.dp(this, 20), p, UI.dp(this, 40));
        sv.addView(col);
        setContentView(sv);

        col.addView(UI.text(this, idx >= 0 ? "Sửa hồ sơ" : "Hồ sơ mới", 26, UI.TEXT, true));
        name = field("Tên hồ sơ", o.optString("name"), "");
        uuid = field("UUID (mỗi UUID = cookie, dữ liệu và dấu vân tay canvas riêng)", o.optString("uuid"), "");
        TextView nu = UI.button(this, "Tạo UUID mới", UI.WHITE, UI.BLUE, true);
        nu.setOnClickListener(v -> uuid.setText(UUID.randomUUID().toString()));
        add(nu, 8);
        proxy = field("Proxy", o.optString("proxy"), "host:port  |  host:port:user:pass  |  user:pass@host:port  |  socks5://host:port");
        ua = field("User-Agent (để trống = tự động, giống Chrome thật)", o.optString("ua"), "Để trống để dùng UA thật của WebView");
        TextView pick = UI.button(this, "Chọn UA mẫu", UI.WHITE, UI.BLUE, true);
        pick.setOnClickListener(v -> new AlertDialog.Builder(this).setItems(UA_NAMES, (d, w) -> ua.setText(UA_VALUES[w])).show());
        add(pick, 8);
        lang = field("Ngôn ngữ", o.optString("lang", "en-US"), "en-US, vi-VN ...");
        label("WebRTC");
        LinearLayout r = new LinearLayout(this);
        chipOff = UI.button(this, "Tắt hẳn", UI.WHITE, UI.TEXT, true);
        chipRelay = UI.button(this, "Relay (không lộ IP)", UI.WHITE, UI.TEXT, true);
        chipOff.setOnClickListener(v -> setRtc("off"));
        chipRelay.setOnClickListener(v -> setRtc("relay"));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.rightMargin = UI.dp(this, 8);
        r.addView(chipOff, lp); r.addView(chipRelay);
        add(r, 4);
        setRtc("relay".equals(o.optString("rtc")) ? "relay" : "off");
        home = field("Trang chủ", o.optString("url", "https://www.google.com"), "https://www.google.com");

        TextView save = UI.button(this, "Lưu hồ sơ", UI.BLUE, UI.WHITE, false);
        save.setTextSize(16);
        save.setOnClickListener(v -> save(o));
        add(save, 28);
    }

    void setRtc(String v) {
        rtc = v;
        chipOff.setBackground(UI.ripple(UI.round(this, "off".equals(v) ? 0xFFD2E3FC : UI.WHITE, 20)));
        chipRelay.setBackground(UI.ripple(UI.round(this, "relay".equals(v) ? 0xFFD2E3FC : UI.WHITE, 20)));
    }

    void save(JSONObject old) {
        try {
            JSONObject o = new JSONObject();
            String id = uuid.getText().toString().trim().replaceAll("[^A-Za-z0-9-]", "");
            if (id.isEmpty()) id = UUID.randomUUID().toString();
            o.put("name", name.getText().toString().trim().isEmpty() ? "Hồ sơ" : name.getText().toString().trim());
            o.put("uuid", id);
            o.put("proxy", proxy.getText().toString().trim());
            o.put("ua", ua.getText().toString().trim());
            o.put("lang", lang.getText().toString().trim().isEmpty() ? "en-US" : lang.getText().toString().trim());
            o.put("rtc", rtc);
            o.put("url", home.getText().toString().trim().isEmpty() ? "https://www.google.com" : home.getText().toString().trim());
            if (idx >= 0) arr.put(idx, o); else arr.put(o);
            Store.save(this, arr);
            Toast.makeText(this, "Đã lưu", Toast.LENGTH_SHORT).show();
            finish();
        } catch (Exception e) { Toast.makeText(this, "Lỗi: " + e, Toast.LENGTH_LONG).show(); }
    }

    void label(String s) {
        TextView t = UI.text(this, s, 12, UI.GREY, true);
        t.setPadding(UI.dp(this, 4), UI.dp(this, 18), 0, UI.dp(this, 6));
        col.addView(t);
    }
    void add(View v, int topDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.topMargin = UI.dp(this, topDp);
        col.addView(v, lp);
    }
    EditText field(String label, String value, String hint) {
        label(label);
        EditText e = new EditText(this);
        e.setText(value); e.setHint(hint);
        e.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        e.setTextColor(UI.TEXT);
        e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_VARIATION_URI);
        int p = UI.dp(this, 14);
        e.setPadding(p, p, p, p);
        e.setBackground(UI.outline(this, UI.WHITE, 12));
        col.addView(e, new LinearLayout.LayoutParams(-1, -2));
        return e;
    }
}
