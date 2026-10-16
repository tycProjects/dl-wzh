package com.sscp.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import androidx.core.app.NotificationCompat;


import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

public class CaptureService extends Service {
    public static final String EXTRA_RESULT_CODE = "resultCode";
    public static final String EXTRA_RESULT_DATA = "resultData";
    private static final String CHANNEL = "sscp_capture";
    /** Dải màn hình chứa Big Road + 3 bảng nhỏ (tỉ lệ theo chiều cao màn hình). */
    private static final float BAND_TOP = 0.55f, BAND_BOTTOM = 0.95f;
    private MediaProjection projection;
    private VirtualDisplay virtualDisplay;
    private ImageReader imageReader;
    private HandlerThread thread;
    private Handler handler;
    private long lastFrameAt = 0L;
    private String lastCandidate = "";
    private String stableKey = null;
    private int repeated = 0;
    private int missing = 0;
    private final RoadBrain brain = new RoadBrain();
    private final AtomicBoolean processing = new AtomicBoolean(false);

    @Override public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        thread = new HandlerThread("sscp-screen-reader");
        thread.start();
        handler = new Handler(thread.getLooper());
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || !intent.hasExtra(EXTRA_RESULT_DATA)) {
            stopSelf();
            return START_NOT_STICKY;
        }

        Notification notification = new NotificationCompat.Builder(this, CHANNEL)
                .setContentTitle("SSCP đang đọc màn hình")
                .setContentText("Đọc Big Road và 3 đường phụ theo quyền bạn cấp")
                .setSmallIcon(android.R.drawable.ic_menu_view)
                .setOngoing(true)
                .build();

        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(23, notification,
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
        } else {
            startForeground(23, notification);
        }

        int resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0);
        Intent resultData = getIntentExtra(intent, EXTRA_RESULT_DATA);
        MediaProjectionManager mgr =
                (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        projection = mgr.getMediaProjection(resultCode, resultData);

        if (projection == null) {
            stopSelf();
            return START_NOT_STICKY;
        }

        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        wm.getDefaultDisplay().getRealMetrics(metrics);
        int width = Math.max(1, metrics.widthPixels);
        int height = Math.max(1, metrics.heightPixels);
        int density = metrics.densityDpi;

        // Android 14+ bắt buộc đăng ký callback TRƯỚC khi tạo màn hình ảo.
        projection.registerCallback(new MediaProjection.Callback() {
            @Override public void onStop() {
                stopSelf();
            }
        }, handler);

        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2);
        imageReader.setOnImageAvailableListener(reader -> processLatestImage(), handler);
        virtualDisplay = projection.createVirtualDisplay(
                "SSCP-display", width, height, density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.getSurface(), null, handler);


        return START_NOT_STICKY;
    }

    private Intent getIntentExtra(Intent intent, String key) {
        if (Build.VERSION.SDK_INT >= 33) {
            return intent.getParcelableExtra(key, Intent.class);
        }
        return intent.getParcelableExtra(key);
    }

    private void processLatestImage() {
        Image image = null;
        try {
            image = imageReader.acquireLatestImage();
            if (image == null) return;
            long now = System.currentTimeMillis();
            if (now - lastFrameAt < 400 || !processing.compareAndSet(false, true)) return;
            lastFrameAt = now;
            try {
                int w = image.getWidth(), h = image.getHeight();
                int y0 = (int) (h * BAND_TOP), y1 = (int) (h * BAND_BOTTOM);
                int[] roi = extractRoi(image, y0, y1);
                consume(RoadReader.read(roi, w, y0, y1 - y0));
            } finally {
                processing.set(false);
            }
        } catch (Exception ignored) {
            processing.set(false);
        } finally {
            if (image != null) image.close();
        }
    }

    /** Chỉ lấy dải màn hình chứa 4 bảng (không đọc video người chia bài hay phần khác). */
    private int[] extractRoi(Image image, int y0, int y1) {
        Image.Plane plane = image.getPlanes()[0];
        ByteBuffer buf = plane.getBuffer();
        int ps = plane.getPixelStride(), rs = plane.getRowStride(), w = image.getWidth();
        int[] out = new int[w * (y1 - y0)];
        for (int y = y0; y < y1; y++) {
            int base = y * rs, o = (y - y0) * w;
            for (int x = 0; x < w; x++) {
                int i = base + x * ps;
                out[o + x] = 0xFF000000 | ((buf.get(i) & 0xFF) << 16)
                        | ((buf.get(i + 1) & 0xFF) << 8) | (buf.get(i + 2) & 0xFF);
            }
        }
        return out;
    }

    /**
     * Chỉ công bố khi 3 khung liên tiếp (~1,2 giây) đọc giống hệt nhau và không có ô nào "không chắc":
     * video người chia bài, hiệu ứng lật bài hay khung lỗi không thể làm nhảy kết quả.
     * Kết quả chỉ đổi khi chính bàn đã ghi ván đó lên Big Road (tức là sau khi lật xong).
     */
    private void consume(RoadReader.Snapshot s) {
        if (s == null || s.big.isEmpty() || s.uncertain > 0) {
            if (++missing >= 6) {
                stableKey = null; lastCandidate = ""; repeated = 0;
                OverlayService.publish(this, null, s == null
                        ? "Chưa thấy Big Road trên màn hình"
                        : "Đọc chưa chắc (" + s.uncertain + " ô) — không đoán");
            }
            return;
        }
        missing = 0;
        String key = s.signature();
        if (key.equals(lastCandidate)) repeated++;
        else { lastCandidate = key; repeated = 1; }
        if (repeated >= 3 && !key.equals(stableKey)) {
            stableKey = key;
            int added = brain.merge(s);
            OverlayService.publish(this, brain.describe(s, added > 0), null);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL, "SSCP screen analysis",
                    NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    @Override public void onDestroy() {
        if (virtualDisplay != null) virtualDisplay.release();
        if (imageReader != null) imageReader.close();
        if (projection != null) projection.stop();
        if (thread != null) thread.quitSafely();
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
