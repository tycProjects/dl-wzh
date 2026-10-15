package com.sscp.app;

import android.app.Activity;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final int REQUEST_CAPTURE = 4102;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        render();
    }

    private TextView text(String value, int size) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(0xFFE8EFFA);
        t.setPadding(0, 12, 0, 12);
        return t;
    }

    private Button button(String label, Runnable action) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setOnClickListener(v -> action.run());
        return b;
    }

    private void render() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(28, 32, 28, 28);
        root.setBackgroundColor(0xFF090E18);

        root.addView(text("SSCP", 30));
        root.addView(text("Bộ đọc màn hình Baccarat · P / B", 16));
        root.addView(text("Ứng dụng đọc bảng kết quả B/P/T thật trên màn hình bàn Baccarat và hiện ván mới nhất, chuỗi và thống kê. Ứng dụng KHÔNG dự đoán ván sau: mỗi ván Baccarat là ngẫu nhiên, không công cụ nào đoán đúng được.", 14));

        root.addView(button("1. Cấp quyền cửa sổ nổi", this::requestOverlay));
        root.addView(button("2. Hiện cửa sổ nổi", () -> {
            if (!Settings.canDrawOverlays(this)) {
                requestOverlay();
                return;
            }
            startService(new Intent(this, OverlayService.class));
            Toast.makeText(this, "Đã yêu cầu mở cửa sổ SSCP", Toast.LENGTH_SHORT).show();
        }));
        root.addView(button("3. Bắt đầu đọc màn hình", this::requestCapture));
        root.addView(button("Dừng đọc màn hình", () ->
                stopService(new Intent(this, CaptureService.class))));
        root.addView(text("Cách dùng: mở SSCP → cấp quyền nổi → bắt đầu đọc màn hình và chấp thuận hộp thoại Android → chuyển sang bàn Baccarat. Android có thể giới hạn việc chụp một số ứng dụng.", 13));

        setContentView(root);
    }

    private void requestOverlay() {
        if (Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Đã có quyền cửa sổ nổi", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()));
        startActivity(intent);
    }

    private void requestCapture() {
        MediaProjectionManager manager =
                (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        startActivityForResult(manager.createScreenCaptureIntent(), REQUEST_CAPTURE);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CAPTURE && resultCode == RESULT_OK && data != null) {
            Intent service = new Intent(this, CaptureService.class);
            service.putExtra(CaptureService.EXTRA_RESULT_CODE, resultCode);
            service.putExtra(CaptureService.EXTRA_RESULT_DATA, data);
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(service);
            else startService(service);
            Toast.makeText(this, "Đã cấp quyền đọc màn hình", Toast.LENGTH_SHORT).show();
        }
    }
}
