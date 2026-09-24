package com.hoangmod.shizuku;

import android.app.*;
import android.os.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.widget.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import rikka.shizuku.Shizuku;
import rikka.shizuku.ShizukuRemoteProcess;

public class MainActivity extends Activity {
    private static final int SHIZUKU_REQ = 100;
    private static final int NOTIFY_REQ = 200;
    private static final String PREFS = "daily_key";
    private static final String CHANNEL = "daily_key_channel";
    private static final long KEY_DURATION = 24L * 60L * 60L * 1000L;
    private static final String PREMIUM_KEY = "HOANG-PREMIUM";

    private TextView status, timer, keyStatus;
    private EditText keyInput;
    private Handler handler = new Handler(Looper.getMainLooper());

    private final Runnable countdown = new Runnable() {
        @Override public void run() {
            long left = getExpiry() - System.currentTimeMillis();
            if (left <= 0) {
                expireKey();
                return;
            }
            timer.setText("KEY HẾT HẠN SAU  " + formatLeft(left));
            handler.postDelayed(this, 1000);
        }
    };

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_main);

        status = findViewById(R.id.status);
        timer = findViewById(R.id.timer);
        keyStatus = findViewById(R.id.keyStatus);
        keyInput = findViewById(R.id.keyInput);
        Button keyButton = findViewById(R.id.keyButton);
        Button start = findViewById(R.id.start);

        createNotificationChannel();
        ensureKey();
        requestNotificationPermissionIfNeeded();
        scheduleExpiryNotification();
        updateShizukuStatus();
        handler.post(countdown);

        keyButton.setOnClickListener(v -> checkKey());
        start.setOnClickListener(v -> startFlow());

        Shizuku.addRequestPermissionResultListener((requestCode, grantResult) -> updateShizukuStatus());
    }

    private void ensureKey() {
        if (getExpiry() <= System.currentTimeMillis()) {
            generateKey();
        }
        keyStatus.setText("✓ KEY MIỄN PHÍ • ĐANG HOẠT ĐỘNG");
    }

    private String getKey() {
        return getSharedPreferences(PREFS, MODE_PRIVATE).getString("key", "");
    }

    private long getExpiry() {
        return getSharedPreferences(PREFS, MODE_PRIVATE).getLong("expiry", 0);
    }

    private void generateKey() {
        String key = randomKey(12);
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putString("key", key)
                .putLong("expiry", System.currentTimeMillis() + KEY_DURATION)
                .apply();
    }

    private String randomKey(int n) {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        SecureRandomLike r = new SecureRandomLike();
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < n; i++) b.append(chars.charAt(r.nextInt(chars.length())));
        return b.toString();
    }

    private static class SecureRandomLike {
        private final java.security.SecureRandom r = new java.security.SecureRandom();
        int nextInt(int n) { return r.nextInt(n); }
    }

    private String formatLeft(long ms) {
        long total = ms / 1000;
        long h = total / 3600;
        long m = (total % 3600) / 60;
        long s = total % 60;
        return String.format(Locale.US, "%02d:%02d:%02d", h, m, s);
    }

    private boolean isPremiumKey(String entered) {
        return PREMIUM_KEY.equals(entered);
    }

    private void checkKey() {
        String entered = keyInput.getText().toString().trim();
        if (isPremiumKey(entered)) {
            keyStatus.setText("✓ HOANG-PREMIUM • VĨNH VIỄN");
            Toast.makeText(this, "Key vĩnh viễn hợp lệ.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (getExpiry() <= System.currentTimeMillis()) {
            expireKey();
            return;
        }
        if (getKey().equals(entered)) {
            keyStatus.setText("✓ KEY 24H • ĐANG HOẠT ĐỘNG");
            Toast.makeText(this, "Key hợp lệ.", Toast.LENGTH_SHORT).show();
        } else {
            keyStatus.setText("✕ KEY KHÔNG HỢP LỆ");
            Toast.makeText(this, "Key không đúng.", Toast.LENGTH_SHORT).show();
        }
    }

    private void expireKey() {
        handler.removeCallbacks(countdown);
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().clear().apply();
        generateKey();
        keyInput.setText("");
        keyStatus.setText("✓ KEY MỚI ĐÃ SẴN SÀNG");
        showNewKeyNotification(getKey());
        scheduleExpiryNotification();
        handler.post(countdown);
        Toast.makeText(this, "Key cũ đã hết hạn. Key mới đã được tạo.", Toast.LENGTH_LONG).show();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel c = new NotificationChannel(
                    CHANNEL, "Key 24h", NotificationManager.IMPORTANCE_HIGH);
            c.setDescription("Thông báo key và thời gian hết hạn");
            getSystemService(NotificationManager.class).createNotificationChannel(c);
        }
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission("android.permission.POST_NOTIFICATIONS")
                        != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, NOTIFY_REQ);
        }
    }

    private void scheduleExpiryNotification() {
        long when = getExpiry();
        Intent i = new Intent(this, ExpiryReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(
                this, 7001, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (am != null) {
            am.cancel(pi);
            if (Build.VERSION.SDK_INT >= 23)
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, pi);
            else
                am.setExact(AlarmManager.RTC_WAKEUP, when, pi);
        }
    }

    private void showNewKeyNotification(String key) {
        if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission("android.permission.POST_NOTIFICATIONS")
                        != PackageManager.PERMISSION_GRANTED) return;
        Notification.Builder n = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL)
                : new Notification.Builder(this);
        n.setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("HOÀNG MOD • KEY 24H MỚI")
                .setContentText("Key hôm nay: " + key)
                .setStyle(new Notification.BigTextStyle().bigText(
                        "Đây là key ngẫu nhiên — " + key + " — có hiệu lực 24 giờ."))
                .setAutoCancel(true);
        ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).notify(9001, n.build());
    }

    private void updateShizukuStatus() {
        if (!Shizuku.pingBinder()) status.setText("SHIZUKU  •  CHƯA KẾT NỐI");
        else if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED)
            status.setText("SHIZUKU  •  ĐÃ KẾT NỐI");
        else status.setText("SHIZUKU  •  CẦN CẤP QUYỀN");
    }

    private void startFlow() {
        String entered = keyInput.getText().toString().trim();
        if (!isPremiumKey(entered) && getExpiry() <= System.currentTimeMillis()) {
            expireKey();
            return;
        }
        if (!isPremiumKey(entered) && !getKey().equals(entered)) {
            Toast.makeText(this, "Nhập đúng key 24h hoặc HOANG-PREMIUM.", Toast.LENGTH_LONG).show();
            return;
        }
        if (!Shizuku.pingBinder()) {
            Toast.makeText(this, "Hãy khởi động Shizuku trước.", Toast.LENGTH_LONG).show();
            return;
        }
        if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
            Shizuku.requestPermission(SHIZUKU_REQ);
            return;
        }

        new Thread(() -> {
            try {
                File a = copyAssetToCache("localconfig.json");
                File b = copyAssetToCache("Assembly-CSharp-patch.bytes");
                String target = "/sdcard/Android/data/com.dts.freefireth/files";
                String cmd = "mkdir -p " + q(target)
                        + " && cp " + q(a.getAbsolutePath()) + " " + q(target + "/localconfig.json")
                        + " && cp " + q(b.getAbsolutePath()) + " " + q(target + "/Assembly-CSharp-patch.bytes");
                int rc = execShizuku(cmd);
                runOnUiThread(() -> {
                    if (rc == 0) {
                        Toast.makeText(this, "Đã chép 2 file. Đang mở game...", Toast.LENGTH_SHORT).show();
                        new Handler(Looper.getMainLooper()).postDelayed(this::openFreeFire, 500);
                    } else {
                        Toast.makeText(this, "Shizuku lỗi, mã " + rc, Toast.LENGTH_LONG).show();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void openFreeFire() {
        try {
            Intent launch = getPackageManager().getLaunchIntentForPackage("com.dts.freefireth");
            if (launch == null) {
                Toast.makeText(this, "Không tìm thấy Free Fire trên máy.", Toast.LENGTH_LONG).show();
                return;
            }
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(launch);
        } catch (Exception e) {
            Toast.makeText(this, "Không thể mở Free Fire: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private File copyAssetToCache(String name) throws Exception {
        File out = new File(getCacheDir(), name);
        try (InputStream in = getAssets().open(name);
             OutputStream os = new FileOutputStream(out)) {
            byte[] buf = new byte[8192]; int n;
            while ((n = in.read(buf)) != -1) os.write(buf, 0, n);
        }
        return out;
    }

    private int execShizuku(String command) throws Exception {
        ShizukuRemoteProcess p = Shizuku.newProcess(new String[]{"sh", "-c", command}, null, null);
        return p.waitFor();
    }

    private static String q(String s) { return "'" + s.replace("'", "'\\''") + "'"; }

    @Override protected void onDestroy() {
        handler.removeCallbacks(countdown);
        super.onDestroy();
    }
}
