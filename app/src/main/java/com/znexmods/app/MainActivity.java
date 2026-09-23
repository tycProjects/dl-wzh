package com.znexmods.app;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Space;
import android.widget.TextView;
import android.graphics.drawable.GradientDrawable;

import java.io.File;

import rikka.shizuku.Shizuku;

public class MainActivity extends Activity {
    private static final String GAME = "com.dts.freefire";
    private static final int RC_STORAGE = 3001;
    private static final int RC_SHIZUKU = 4001;

    private final Handler handler = new Handler();
    private LinearLayout root;
    private TextView ramValue;
    private TextView shizukuValue;
    private TextView storageValue;

    private static final int BG = Color.rgb(6, 8, 12);
    private static final int SURFACE = Color.rgb(17, 20, 27);
    private static final int WHITE = Color.WHITE;
    private static final int MUTED = Color.rgb(164, 171, 184);
    private static final int GREEN = Color.rgb(126, 255, 188);
    private static final int RED = Color.rgb(255, 150, 150);
    private static final int BLUE = Color.rgb(121, 174, 255);

    private final Shizuku.OnRequestPermissionResultListener shizukuPermissionListener = (requestCode, grantResult) -> {
        if (requestCode != RC_SHIZUKU) return;
        handler.post(this::refreshDeviceState);
        if (grantResult == PackageManager.PERMISSION_GRANTED) {
            toast("Đã cấp quyền Shizuku cho ZnexMos.");
        } else {
            toast("Quyền Shizuku chưa được cấp.");
        }
    };

    private final Shizuku.OnBinderReceivedListener shizukuBinderListener = () -> handler.post(this::requestShizukuOnLaunch);

    private int dp(float v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window w = getWindow();
        w.setStatusBarColor(BG);
        w.setNavigationBarColor(BG);
        if (Build.VERSION.SDK_INT >= 23) {
            w.getDecorView().setSystemUiVisibility(0);
        }

        registerShizukuListener();
        buildUi();
        refreshDeviceState();

        // Yêu cầu Shizuku ngay khi app vừa mở, sau khi UI đã hiển thị.
        handler.postDelayed(this::requestShizukuOnLaunch, 350);
    }

    private void registerShizukuListener() {
        try {
            Shizuku.addRequestPermissionResultListener(shizukuPermissionListener);
            Shizuku.addBinderReceivedListenerSticky(shizukuBinderListener);
        } catch (Throwable ignored) {
        }
    }

    @Override
    protected void onDestroy() {
        try {
            Shizuku.removeRequestPermissionResultListener(shizukuPermissionListener);
            Shizuku.removeBinderReceivedListener(shizukuBinderListener);
        } catch (Throwable ignored) {
        }
        super.onDestroy();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);
        scroll.setClipToPadding(false);

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(12), dp(16), dp(28));

        root.addView(topBar());
        root.addView(space(10));
        root.addView(heroCard());
        root.addView(space(14));
        root.addView(sectionTitle("TRẠNG THÁI HỆ THỐNG"));
        root.addView(statusCard());
        root.addView(space(14));
        root.addView(sectionTitle("CÔNG CỤ TỐI ƯU"));
        root.addView(toolGrid());
        root.addView(space(14));
        root.addView(sectionTitle("QUYỀN TRUY CẬP"));
        root.addView(permissionCard());
        root.addView(space(14));
        root.addView(sectionTitle("FREE FIRE"));
        root.addView(gameCard());
        root.addView(space(16));
        root.addView(disclaimer());

        scroll.addView(root, new ScrollView.LayoutParams(-1, -1));
        setContentView(scroll);
    }

    private View topBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(2), dp(4), dp(2), dp(4));

        TextView logo = text("ZNEXMOS", 20, WHITE, Typeface.BOLD);
        logo.setLetterSpacing(0.14f);
        bar.addView(logo, new LinearLayout.LayoutParams(0, -2, 1f));

        TextView version = pill("v1.0");
        bar.addView(version, new LinearLayout.LayoutParams(-2, -2));
        return bar;
    }

    private View heroCard() {
        LinearLayout card = glassCard(22);
        card.setPadding(dp(18), dp(20), dp(18), dp(20));

        LinearLayout badgeRow = new LinearLayout(this);
        badgeRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView badge = pill("GAME OPTIMIZER");
        badge.setTextColor(GREEN);
        badge.setBackground(roundBg(Color.argb(45, 126, 255, 188), Color.argb(100, 126, 255, 188), 999));
        badgeRow.addView(badge);
        card.addView(badgeRow);

        TextView title = text("Tối ưu máy yếu\ncho Free Fire", 28, WHITE, Typeface.BOLD);
        title.setLineSpacing(0, 0.92f);
        title.setPadding(0, dp(12), 0, dp(5));
        card.addView(title);

        TextView sub = text("Giao diện kính tối • phản hồi nhanh • tập trung vào các thao tác an toàn trên thiết bị.", 13.5f, MUTED, Typeface.NORMAL);
        sub.setLineSpacing(0, 0.12f);
        card.addView(sub);

        LinearLayout chipRow = new LinearLayout(this);
        chipRow.setPadding(0, dp(14), 0, 0);
        chipRow.setGravity(Gravity.CENTER_VERTICAL);
        chipRow.addView(infoChip("⚡", "Nhẹ máy"));
        chipRow.addView(spaceH(8));
        chipRow.addView(infoChip("🧹", "Dọn cache"));
        chipRow.addView(spaceH(8));
        chipRow.addView(infoChip("🔐", "Shizuku"));
        card.addView(chipRow);
        return card;
    }

    private View statusCard() {
        LinearLayout card = glassCard(20);
        card.setPadding(dp(14), dp(8), dp(14), dp(8));
        card.addView(statusRow("RAM khả dụng", "Đang đọc…", 1));
        ramValue = (TextView) ((ViewGroup) card.getChildAt(0)).getChildAt(1);
        card.addView(statusRow("Shizuku", "Đang kiểm tra…", 2));
        shizukuValue = (TextView) ((ViewGroup) card.getChildAt(1)).getChildAt(1);
        card.addView(statusRow("Bộ nhớ", "Đang kiểm tra…", 3));
        storageValue = (TextView) ((ViewGroup) card.getChildAt(2)).getChildAt(1);
        return card;
    }

    private View statusRow(String label, String value, int index) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(4), dp(12), dp(4), dp(12));
        if (index > 1) row.setBackground(roundBg(Color.TRANSPARENT, Color.argb(35, 255,255,255), 0));

        TextView l = text(label, 13.5f, MUTED, Typeface.NORMAL);
        row.addView(l, new LinearLayout.LayoutParams(0, -2, 1f));
        TextView v = text(value, 13.5f, WHITE, Typeface.BOLD);
        v.setGravity(Gravity.RIGHT);
        row.addView(v, new LinearLayout.LayoutParams(-2, -2));
        return row;
    }

    private View toolGrid() {
        LinearLayout grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);

        LinearLayout r1 = new LinearLayout(this);
        r1.setWeightSum(2f);
        r1.addView(toolButton("⚡", "Tối ưu máy yếu", "Làm mới trạng thái", this::optimize), weightParams());
        r1.addView(spaceH(10));
        r1.addView(toolButton("🚀", "Tăng tốc", "Chuẩn bị chơi game", this::optimize), weightParams());
        grid.addView(r1);

        grid.addView(space(10));
        LinearLayout r2 = new LinearLayout(this);
        r2.setWeightSum(2f);
        r2.addView(toolButton("🧹", "Dọn cache ZnexMos", "Xóa cache ứng dụng", this::cleanOwnCache), weightParams());
        r2.addView(spaceH(10));
        r2.addView(toolButton("⚙", "Cài đặt ứng dụng", "Quản lý quyền/cache", this::openAppSettings), weightParams());
        grid.addView(r2);
        return grid;
    }

    private LinearLayout.LayoutParams weightParams() {
        return new LinearLayout.LayoutParams(0, dp(116), 1f);
    }

    private View toolButton(String icon, String title, String sub, Runnable action) {
        LinearLayout b = glassCard(18);
        b.setClickable(true);
        b.setFocusable(true);
        b.setPadding(dp(14), dp(14), dp(14), dp(14));
        b.setOnClickListener(v -> action.run());

        TextView iconV = text(icon, 24, WHITE, Typeface.NORMAL);
        b.addView(iconV, new LinearLayout.LayoutParams(-1, dp(34)));
        TextView t = text(title, 14.5f, WHITE, Typeface.BOLD);
        t.setPadding(0, dp(4), 0, 0);
        b.addView(t);
        TextView s = text(sub, 11.5f, MUTED, Typeface.NORMAL);
        s.setPadding(0, dp(3), 0, 0);
        b.addView(s);
        return b;
    }

    private View permissionCard() {
        LinearLayout card = glassCard(20);
        card.setPadding(dp(14), dp(6), dp(14), dp(10));

        LinearLayout sh = permissionRow("🔐", "Quyền Shizuku", "Cấp quyền cho ZnexMos", GREEN);
        sh.setOnClickListener(v -> requestShizukuManually());
        card.addView(sh);

        LinearLayout st = permissionRow("📁", "Quyền bộ nhớ", "Truy cập bộ nhớ dùng chung", BLUE);
        st.setOnClickListener(v -> requestStorageAccess());
        card.addView(st);
        return card;
    }

    private LinearLayout permissionRow(String icon, String title, String sub, int accent) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(4), dp(12), dp(4), dp(12));

        TextView iv = text(icon, 22, WHITE, Typeface.NORMAL);
        row.addView(iv, new LinearLayout.LayoutParams(dp(38), dp(38)));

        LinearLayout meta = new LinearLayout(this);
        meta.setOrientation(LinearLayout.VERTICAL);
        TextView t = text(title, 14.5f, WHITE, Typeface.BOLD);
        TextView s = text(sub, 11.5f, MUTED, Typeface.NORMAL);
        meta.addView(t);
        meta.addView(s);
        row.addView(meta, new LinearLayout.LayoutParams(0, -2, 1f));

        TextView chevron = text("›", 30, accent, Typeface.NORMAL);
        chevron.setGravity(Gravity.CENTER);
        row.addView(chevron, new LinearLayout.LayoutParams(dp(30), dp(42)));
        return row;
    }

    private View gameCard() {
        LinearLayout card = glassCard(20);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));

        LinearLayout head = new LinearLayout(this);
        head.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout iconBox = glassCard(16);
        iconBox.setGravity(Gravity.CENTER);
        iconBox.setPadding(0, 0, 0, 0);
        TextView icon = text("🎮", 30, WHITE, Typeface.NORMAL);
        icon.setGravity(Gravity.CENTER);
        iconBox.addView(icon, new LinearLayout.LayoutParams(dp(64), dp(64)));
        head.addView(iconBox, new LinearLayout.LayoutParams(dp(64), dp(64)));

        LinearLayout meta = new LinearLayout(this);
        meta.setOrientation(LinearLayout.VERTICAL);
        meta.setPadding(dp(14), 0, 0, 0);
        meta.addView(text("Free Fire", 17, WHITE, Typeface.BOLD));
        meta.addView(text(GAME, 11.5f, MUTED, Typeface.NORMAL));
        TextView state = pill("APP TARGET");
        state.setTextColor(BLUE);
        meta.addView(state, new LinearLayout.LayoutParams(-2, -2));
        head.addView(meta, new LinearLayout.LayoutParams(0, -2, 1f));
        card.addView(head);

        LinearLayout buttons = new LinearLayout(this);
        buttons.setPadding(0, dp(14), 0, 0);
        Button open = actionButton("MỞ FREE FIRE", true);
        open.setOnClickListener(v -> launchGame());
        buttons.addView(open, new LinearLayout.LayoutParams(0, dp(48), 1f));
        card.addView(buttons);
        return card;
    }

    private View disclaimer() {
        LinearLayout box = new LinearLayout(this);
        box.setPadding(dp(14), dp(12), dp(14), dp(12));
        GradientDrawable bg = roundBg(Color.argb(24, 255,255,255), Color.argb(55, 255,255,255), 16);
        box.setBackground(bg);
        TextView note = text("Lưu ý: ZnexMos không thể vượt giới hạn FPS phần cứng và không tự ý sửa dữ liệu riêng của ứng dụng khác. Quyền Shizuku/bộ nhớ chỉ được dùng khi bạn cấp phép.", 11.5f, MUTED, Typeface.NORMAL);
        note.setLineSpacing(0, 0.15f);
        box.addView(note);
        return box;
    }

    private TextView infoChip(String icon, String label) {
        TextView v = pill(icon + "  " + label);
        v.setTextSize(11.5f);
        v.setTextColor(MUTED);
        v.setBackground(roundBg(Color.argb(26,255,255,255), Color.argb(38,255,255,255), 999));
        v.setPadding(dp(10), dp(8), dp(10), dp(8));
        return v;
    }

    private TextView pill(String s) {
        TextView v = text(s, 10.5f, MUTED, Typeface.BOLD);
        v.setGravity(Gravity.CENTER);
        v.setPadding(dp(10), dp(6), dp(10), dp(6));
        v.setBackground(roundBg(Color.argb(28,255,255,255), Color.argb(55,255,255,255), 999));
        return v;
    }

    private TextView sectionTitle(String s) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        TextView bar = text("", 1, GREEN, Typeface.BOLD);
        bar.setBackground(roundBg(GREEN, Color.TRANSPARENT, 999));
        row.addView(bar, new LinearLayout.LayoutParams(dp(5), dp(22)));
        TextView t = text(s, 12.5f, WHITE, Typeface.BOLD);
        t.setLetterSpacing(0.08f);
        t.setPadding(dp(10), 0, 0, 0);
        row.addView(t);
        row.setPadding(0, 0, 0, dp(8));
        return row;
    }

    private TextView text(String s, float size, int color, int style) {
        TextView v = new TextView(this);
        v.setText(s);
        v.setTextColor(color);
        v.setTextSize(size);
        v.setTypeface(Typeface.create("sans", style));
        return v;
    }

    private Button actionButton(String label, boolean primary) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(13.5f);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setTextColor(primary ? Color.rgb(8, 24, 17) : WHITE);
        b.setAllCaps(false);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(10), 0, dp(10), 0);
        b.setBackground(roundBg(primary ? GREEN : Color.argb(30,255,255,255),
                primary ? GREEN : Color.argb(55,255,255,255), 15));
        return b;
    }

    private LinearLayout glassCard(float radius) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(roundBg(Color.argb(28,255,255,255), Color.argb(42,255,255,255), radius));
        card.setElevation(dp(3));
        return card;
    }

    private GradientDrawable roundBg(int fill, int stroke, float radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(dp(radiusDp));
        if (stroke != Color.TRANSPARENT) g.setStroke(dp(1), stroke);
        return g;
    }

    private Space space(int dp) {
        Space s = new Space(this);
        s.setLayoutParams(new LinearLayout.LayoutParams(1, this.dp(dp)));
        return s;
    }

    private Space spaceH(int dp) {
        Space s = new Space(this);
        s.setLayoutParams(new LinearLayout.LayoutParams(this.dp(dp), 1));
        return s;
    }

    private void refreshDeviceState() {
        ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(mi);
        long total = mi.totalMem / 1048576L;
        long avail = mi.availMem / 1048576L;
        if (ramValue != null) ramValue.setText(avail + " MB / " + total + " MB");

        boolean shizukuGranted = false;
        try {
            shizukuGranted = !Shizuku.isPreV11() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED;
        } catch (Throwable ignored) {
        }
        if (shizukuValue != null) {
            boolean running = false;
            try { running = Shizuku.pingBinder(); } catch (Throwable ignored) {}
            if (shizukuGranted) {
                shizukuValue.setText("Đã cấp quyền");
                shizukuValue.setTextColor(GREEN);
            } else if (running) {
                shizukuValue.setText("Đang chạy • chưa cấp quyền");
                shizukuValue.setTextColor(Color.rgb(255, 214, 135));
            } else {
                shizukuValue.setText("Chưa sẵn sàng");
                shizukuValue.setTextColor(RED);
            }
        }

        boolean storage = hasStorageAccess();
        if (storageValue != null) {
            storageValue.setText(storage ? "Đã cấp" : "Chưa cấp");
            storageValue.setTextColor(storage ? GREEN : Color.rgb(255, 214, 135));
        }
    }

    private boolean hasStorageAccess() {
        if (Build.VERSION.SDK_INT >= 30) return Environment.isExternalStorageManager();
        if (Build.VERSION.SDK_INT >= 23) {
            return checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                    || checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private void requestShizukuOnLaunch() {
        try {
            if (Shizuku.isPreV11()) return;
            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                refreshDeviceState();
                return;
            }
            if (Shizuku.pingBinder()) {
                Shizuku.requestPermission(RC_SHIZUKU);
            } else {
                showShizukuNotReadyDialog();
            }
        } catch (Throwable ignored) {
            showShizukuNotReadyDialog();
        }
    }

    private void requestShizukuManually() {
        try {
            if (Shizuku.pingBinder()) {
                if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                    Shizuku.requestPermission(RC_SHIZUKU);
                } else {
                    toast("ZnexMos đã có quyền Shizuku.");
                }
            } else {
                showShizukuNotReadyDialog();
            }
        } catch (Throwable e) {
            showShizukuNotReadyDialog();
        }
    }

    private void showShizukuNotReadyDialog() {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Cần Shizuku")
                .setMessage("Shizuku chưa chạy hoặc chưa được cài. Hãy khởi động Shizuku, sau đó quay lại ZnexMos để cấp quyền.")
                .setNegativeButton("Đóng", null)
                .setPositiveButton("Mở Shizuku", (d, w) -> openShizuku())
                .show();
    }

    private void openShizuku() {
        try {
            Intent i = getPackageManager().getLaunchIntentForPackage("moe.shizuku.privileged.api");
            if (i != null) {
                startActivity(i);
                return;
            }
        } catch (Throwable ignored) {
        }
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://shizuku.rikka.app/download/")));
        } catch (Throwable ignored) {
            toast("Không thể mở trang Shizuku.");
        }
    }

    private void requestStorageAccess() {
        if (hasStorageAccess()) {
            toast("ZnexMos đã có quyền bộ nhớ.");
            return;
        }

        if (Build.VERSION.SDK_INT >= 30) {
            try {
                Intent i = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivity(i);
                return;
            } catch (ActivityNotFoundException e) {
                try {
                    startActivity(new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION));
                    return;
                } catch (Throwable ignored) {
                }
            }
        }

        if (Build.VERSION.SDK_INT >= 23) {
            requestPermissions(new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            }, RC_STORAGE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == RC_STORAGE) {
            refreshDeviceState();
            toast(hasStorageAccess() ? "Đã cấp quyền bộ nhớ." : "Quyền bộ nhớ chưa được cấp.");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.postDelayed(this::refreshDeviceState, 150);
    }

    private void optimize() {
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                getSystemService(android.os.Vibrator.class).vibrate(35);
            } else {
                ((android.os.Vibrator) getSystemService(VIBRATOR_SERVICE)).vibrate(35);
            }
        } catch (Throwable ignored) {
        }
        refreshDeviceState();
        toast("Đã làm mới trạng thái và áp dụng tối ưu an toàn cho ZnexMos.");
    }

    private void cleanOwnCache() {
        try {
            File c = getCacheDir();
            delete(c);
            c.mkdirs();
            toast("Đã dọn cache của ZnexMos.");
        } catch (Exception e) {
            toast("Không thể dọn cache.");
        }
    }

    private void delete(File f) {
        if (f == null || !f.exists()) return;
        if (f.isDirectory()) {
            File[] files = f.listFiles();
            if (files != null) for (File child : files) delete(child);
        }
        f.delete();
    }

    private void launchGame() {
        PackageManager pm = getPackageManager();
        Intent i = pm.getLaunchIntentForPackage(GAME);
        if (i != null) {
            startActivity(i);
        } else {
            toast("Chưa tìm thấy Free Fire (com.dts.freefire).");
        }
    }

    private void openAppSettings() {
        Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:" + getPackageName()));
        startActivity(i);
    }

    private void toast(String s) {
        android.widget.Toast.makeText(this, s, android.widget.Toast.LENGTH_SHORT).show();
    }
}
