package com.protectapk.app;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final int PICK_APK = 1001;
    // Local emulator: http://10.0.2.2:8000/
    // Physical phone: http://YOUR_COMPUTER_LAN_IP:8000/
    // Production: use your HTTPS API domain.
    private static final String API_BASE_URL = "http://10.0.2.2:8000/";

    private TextView status;
    private TextView details;
    private Button scanButton;
    private ProgressBar progress;
    private Uri selectedUri;
    private String selectedName = "";
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        buildUi();
    }

    private void buildUi() {
        int navy = 0xFF071B3A;
        int blue = 0xFF1769D2;
        int white = 0xFFFFFFFF;
        LinearLayout outer = new LinearLayout(this);
        outer.setOrientation(LinearLayout.VERTICAL);
        outer.setBackgroundColor(0xFFF3F7FC);
        outer.setPadding(dp(20), dp(24), dp(20), dp(20));

        TextView title = new TextView(this);
        title.setText("PROTECT APK");
        title.setTextSize(27);
        title.setTextColor(navy);
        title.setGravity(Gravity.CENTER);
        title.setTypeface(null, 1);
        outer.addView(title, new LinearLayout.LayoutParams(-1, -2));

        TextView subtitle = new TextView(this);
        subtitle.setText("APK INTEGRITY • SHA-256 • SIGNATURE");
        subtitle.setTextSize(12);
        subtitle.setTextColor(0xFF426184);
        subtitle.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(-1, -2);
        subLp.topMargin = dp(8);
        subLp.bottomMargin = dp(24);
        outer.addView(subtitle, subLp);

        Button choose = new Button(this);
        choose.setText("PILIH FILE APK");
        choose.setTextColor(white);
        choose.setBackgroundTintList(android.content.res.ColorStateList.valueOf(blue));
        outer.addView(choose, new LinearLayout.LayoutParams(-1, dp(52)));
        choose.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("application/vnd.android.package-archive");
            startActivityForResult(i, PICK_APK);
        });

        scanButton = new Button(this);
        scanButton.setText("PERIKSA APK");
        scanButton.setEnabled(false);
        LinearLayout.LayoutParams scanLp = new LinearLayout.LayoutParams(-1, dp(52));
        scanLp.topMargin = dp(10);
        outer.addView(scanButton, scanLp);
        scanButton.setOnClickListener(v -> scanSelected());

        progress = new ProgressBar(this);
        progress.setVisibility(View.GONE);
        LinearLayout.LayoutParams pLp = new LinearLayout.LayoutParams(-2, -2);
        pLp.gravity = Gravity.CENTER_HORIZONTAL;
        pLp.topMargin = dp(12);
        outer.addView(progress, pLp);

        status = new TextView(this);
        status.setText("Pilih file APK untuk memulai pemeriksaan.");
        status.setTextColor(navy);
        status.setTextSize(15);
        status.setTypeface(null, 1);
        LinearLayout.LayoutParams stLp = new LinearLayout.LayoutParams(-1, -2);
        stLp.topMargin = dp(18);
        outer.addView(status, stLp);

        details = new TextView(this);
        details.setTextColor(0xFF25364D);
        details.setTextSize(13);
        details.setTextIsSelectable(true);
        details.setPadding(dp(14), dp(14), dp(14), dp(14));
        details.setBackgroundTintList(android.content.res.ColorStateList.valueOf(white));
        details.setBackgroundColor(white);
        LinearLayout.LayoutParams dLp = new LinearLayout.LayoutParams(-1, 0, 1);
        dLp.topMargin = dp(10);
        outer.addView(details, dLp);

        TextView foot = new TextView(this);
        foot.setText("Catatan: tanda tangan valid tidak menjamin APK bebas malware.");
        foot.setTextColor(0xFF5D6E83);
        foot.setTextSize(11);
        foot.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams fLp = new LinearLayout.LayoutParams(-1, -2);
        fLp.topMargin = dp(12);
        outer.addView(foot, fLp);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(outer);
        setContentView(scroll);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_APK && resultCode == RESULT_OK && data != null) {
            selectedUri = data.getData();
            selectedName = getDisplayName(selectedUri);
            scanButton.setEnabled(true);
            status.setText("File dipilih: " + selectedName);
            details.setText("Siap diperiksa.\nFile akan diunggah ke server untuk verifikasi tanda tangan dan pencocokan hash tepercaya.");
        }
    }

    private String getDisplayName(Uri uri) {
        try (Cursor c = getContentResolver().query(uri, null, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                int ix = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (ix >= 0) return c.getString(ix);
            }
        } catch (Exception ignored) {}
        return "selected.apk";
    }

    private void scanSelected() {
        if (selectedUri == null) return;
        progress.setVisibility(View.VISIBLE);
        scanButton.setEnabled(false);
        status.setText("Memeriksa APK…");
        details.setText("Menghitung SHA-256 dan menghubungi server.");
        executor.execute(() -> {
            try {
                JSONObject result = uploadAndScan(selectedUri, selectedName);
                runOnUiThread(() -> showResult(result));
            } catch (Exception e) {
                runOnUiThread(() -> {
                    status.setText("Pemeriksaan gagal");
                    details.setText(e.getMessage() == null ? e.toString() : e.getMessage());
                    progress.setVisibility(View.GONE);
                    scanButton.setEnabled(true);
                });
            }
        });
    }

    private JSONObject uploadAndScan(Uri uri, String filename) throws Exception {
        String boundary = "----ProtectBoundary" + System.currentTimeMillis();
        HttpURLConnection conn = (HttpURLConnection) new URL(API_BASE_URL + "scan").openConnection();
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(90000);
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        conn.setRequestProperty("Accept", "application/json");

        try (OutputStream out = conn.getOutputStream()) {
            write(out, "--" + boundary + "\r\n");
            write(out, "Content-Disposition: form-data; name=\"file\"; filename=\"" + safeFilename(filename) + "\"\r\n");
            write(out, "Content-Type: application/vnd.android.package-archive\r\n\r\n");
            try (InputStream in = getContentResolver().openInputStream(uri)) {
                if (in == null) throw new Exception("Tidak bisa membaca file yang dipilih.");
                byte[] buf = new byte[64 * 1024];
                int n;
                while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
            }
            write(out, "\r\n--" + boundary + "--\r\n");
        }

        int code = conn.getResponseCode();
        InputStream response = code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream();
        StringBuilder body = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(response))) {
            String line;
            while ((line = reader.readLine()) != null) body.append(line);
        }
        conn.disconnect();
        JSONObject json = new JSONObject(body.toString());
        if (code < 200 || code >= 300) throw new Exception(json.optString("detail", "Server error HTTP " + code));
        return json;
    }

    private void showResult(JSONObject j) {
        String assessment = j.optString("assessment", "UNKNOWN");
        status.setText(assessment.replace('_', ' '));
        StringBuilder b = new StringBuilder();
        b.append("Nama file: ").append(j.optString("filename")).append("\n");
        b.append("Ukuran: ").append(j.optLong("size_bytes")).append(" byte\n\n");
        b.append("SHA-256:\n").append(j.optString("sha256")).append("\n\n");
        b.append("Hash tepercaya: ").append(j.optString("trusted_hash")).append("\n");
        b.append("Label tepercaya: ").append(j.optString("trusted_label", "-")).append("\n");
        b.append("Tanda tangan valid: ").append(j.optBoolean("signature_verified") ? "YA" : "TIDAK / TIDAK DAPAT DIVERIFIKASI").append("\n");
        b.append("Sertifikat penandatangan:\n");
        org.json.JSONArray certs = j.optJSONArray("signer_certificates");
        if (certs == null || certs.length() == 0) b.append("- Tidak ada sertifikat yang terbaca\n");
        else for (int i = 0; i < certs.length(); i++) b.append("- ").append(certs.optString(i)).append("\n");
        b.append("\nPenilaian: ").append(assessment).append("\n\n");
        b.append("Peringatan: hasil ini bukan pemindaian antivirus. Hash harus dibandingkan dengan rilis resmi yang tepercaya.");
        String sigError = j.optString("signature_error", "");
        if (!sigError.isEmpty() && !sigError.equals("null")) b.append("\n\nDetail verifikasi:\n").append(sigError);
        details.setText(b.toString());
        progress.setVisibility(View.GONE);
        scanButton.setEnabled(true);
    }

    private static String safeFilename(String name) {
        return name == null ? "upload.apk" : name.replace("\"", "_").replace("\r", "_").replace("\n", "_");
    }
    private static void write(OutputStream out, String s) throws Exception {
        out.write(s.getBytes("UTF-8"));
    }
    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }
}
