package com.pb.browser;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.UUID;

public class MainActivity extends Activity {
    static final String[] UA_NAMES = {"-- UA mau --", "Android Chrome", "Windows Chrome", "macOS Chrome", "iPhone Safari"};
    static final String[] UA_VALUES = {"",
        "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Mobile Safari/537.36",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36",
        "Mozilla/5.0 (iPhone; CPU iPhone OS 17_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.5 Mobile/15E148 Safari/604.1"};

    JSONArray profiles = new JSONArray();
    int cur = -1;
    ArrayList<String> names = new ArrayList<>();
    ArrayAdapter<String> adapter;
    Spinner sp, rtc, preset;
    EditText name, uuid, proxy, ua, lang, url;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        try { profiles = new JSONArray(getSharedPreferences("pb", 0).getString("profiles", "[]")); } catch (Exception e) {}
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        ScrollView sv = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);
        sv.addView(root);
        setContentView(sv);

        label(root, "Profile");
        sp = new Spinner(this);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, names);
        sp.setAdapter(adapter);
        root.addView(sp);
        sp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) { cur = pos; fill(); }
            public void onNothingSelected(AdapterView<?> p) {}
        });

        LinearLayout row = new LinearLayout(this);
        root.addView(row);
        button(row, "+ Moi", v -> { profiles.put(newProfile()); persist(); refresh(profiles.length() - 1); });
        button(row, "Xoa", v -> {
            if (cur >= 0 && profiles.length() > 0) { profiles.remove(cur); persist(); refresh(0); }
        });

        name = field(root, "Ten");
        uuid = field(root, "UUID (moi UUID = du lieu/cookie/fingerprint rieng)");
        Button nu = new Button(this); nu.setText("Tao UUID moi");
        nu.setOnClickListener(v -> uuid.setText(UUID.randomUUID().toString()));
        root.addView(nu);
        proxy = field(root, "Proxy: host:port | host:port:user:pass | user:pass@host:port | socks5://host:port");
        ua = field(root, "User-Agent");
        preset = new Spinner(this);
        preset.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, UA_NAMES));
        preset.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) { if (pos > 0) ua.setText(UA_VALUES[pos]); }
            public void onNothingSelected(AdapterView<?> p) {}
        });
        root.addView(preset);
        lang = field(root, "Ngon ngu (vd en-US, vi-VN)");
        label(root, "WebRTC");
        rtc = new Spinner(this);
        rtc.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
                new String[]{"off (tat han - an toan nhat)", "relay (chi ung vien relay, khong lo IP)"}));
        root.addView(rtc);
        url = field(root, "Trang mo dau");

        LinearLayout row2 = new LinearLayout(this);
        root.addView(row2);
        button(row2, "Luu", v -> { save(); Toast.makeText(this, "Da luu", Toast.LENGTH_SHORT).show(); });
        button(row2, "MO TRINH DUYET", v -> {
            JSONObject o = save();
            if (o == null) return;
            Intent i = new Intent(this, BrowserActivity.class);
            i.putExtra("profile", o.toString());
            startActivity(i);
        });

        if (profiles.length() == 0) { profiles.put(newProfile()); persist(); }
        refresh(0);
    }

    JSONObject newProfile() {
        JSONObject o = new JSONObject();
        try {
            o.put("name", "Profile " + (profiles.length() + 1));
            o.put("uuid", UUID.randomUUID().toString());
            o.put("proxy", ""); o.put("ua", UA_VALUES[1]); o.put("lang", "en-US");
            o.put("rtc", "off"); o.put("url", "https://browserleaks.com/webrtc");
        } catch (Exception e) {}
        return o;
    }

    void refresh(int sel) {
        names.clear();
        for (int i = 0; i < profiles.length(); i++) names.add(profiles.optJSONObject(i).optString("name"));
        adapter.notifyDataSetChanged();
        if (names.isEmpty()) return;
        cur = Math.max(0, Math.min(sel, names.size() - 1));
        sp.setSelection(cur);
        fill();
    }

    void fill() {
        if (cur < 0 || cur >= profiles.length()) return;
        JSONObject o = profiles.optJSONObject(cur);
        name.setText(o.optString("name")); uuid.setText(o.optString("uuid"));
        proxy.setText(o.optString("proxy")); ua.setText(o.optString("ua"));
        lang.setText(o.optString("lang")); url.setText(o.optString("url"));
        rtc.setSelection("relay".equals(o.optString("rtc")) ? 1 : 0);
    }

    JSONObject save() {
        if (cur < 0) return null;
        try {
            JSONObject o = new JSONObject();
            o.put("name", name.getText().toString().trim());
            o.put("uuid", uuid.getText().toString().trim().replaceAll("[^A-Za-z0-9-]", ""));
            o.put("proxy", proxy.getText().toString().trim());
            o.put("ua", ua.getText().toString().trim());
            o.put("lang", lang.getText().toString().trim().isEmpty() ? "en-US" : lang.getText().toString().trim());
            o.put("rtc", rtc.getSelectedItemPosition() == 1 ? "relay" : "off");
            o.put("url", url.getText().toString().trim());
            profiles.put(cur, o);
            persist();
            names.set(cur, o.getString("name"));
            adapter.notifyDataSetChanged();
            return o;
        } catch (Exception e) { return null; }
    }

    void persist() { getSharedPreferences("pb", 0).edit().putString("profiles", profiles.toString()).apply(); }

    void label(LinearLayout p, String t) {
        TextView tv = new TextView(this); tv.setText(t); tv.setPadding(0, 24, 0, 0); p.addView(tv);
    }
    EditText field(LinearLayout p, String l) {
        label(p, l); EditText e = new EditText(this); e.setSingleLine(false); p.addView(e); return e;
    }
    void button(LinearLayout p, String t, View.OnClickListener c) {
        Button bt = new Button(this); bt.setText(t); bt.setOnClickListener(c);
        p.addView(bt, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
    }
}
