package com.sscp.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
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
    private MediaProjection projection;
    private VirtualDisplay virtualDisplay;
    private ImageReader imageReader;
    private HandlerThread thread;
    private Handler handler;
    private long lastFrameAt = 0L;
    private String lastCandidate = "";
    private String stableCells = null;
    private int repeated = 0;
    private int missing = 0;
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
                .setContentText("Đọc bảng kết quả trên màn hình theo quyền bạn cấp")
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
        int width = Math.max(1, metrics.widthPixels / 2);
        int height = Math.max(1, metrics.heightPixels / 2);
        int density = metrics.densityDpi;

        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2);
        imageReader.setOnImageAvailableListener(reader -> processLatestImage(), handler);
        virtualDisplay = projection.createVirtualDisplay(
                "SSCP-display", width, height, density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.getSurface(), null, handler);

        projection.registerCallback(new MediaProjection.Callback() {
            @Override public void onStop() {
                stopSelf();
            }
        }, handler);

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
            if (now - lastFrameAt < 500 || !processing.compareAndSet(false, true)) return;
            lastFrameAt = now;

            final Bitmap bitmap = imageToBitmap(image);
            if (bitmap == null) {
                processing.set(false);
                return;
            }

            try {
                int w = bitmap.getWidth(), h = bitmap.getHeight();
                int[] px = new int[w * h];
                bitmap.getPixels(px, 0, w, 0, 0, w, h);
                consume(BeadPlateReader.read(px, w, h));
            } finally {
                bitmap.recycle();
                processing.set(false);
            }
        } catch (Exception ignored) {
            processing.set(false);
        } finally {
            if (image != null) image.close();
        }
    }

    private Bitmap imageToBitmap(Image image) {
        Image.Plane[] planes = image.getPlanes();
        if (planes.length == 0) return null;

        ByteBuffer buffer = planes[0].getBuffer();
        int pixelStride = planes[0].getPixelStride();
        int rowStride = planes[0].getRowStride();
        int rowPadding = rowStride - pixelStride * image.getWidth();

        Bitmap padded = Bitmap.createBitmap(
                image.getWidth() + rowPadding / pixelStride,
                image.getHeight(), Bitmap.Config.ARGB_8888);
        padded.copyPixelsFromBuffer(buffer);
        Bitmap cropped = Bitmap.createBitmap(
                padded, 0, 0, image.getWidth(), image.getHeight());
        padded.recycle();
        return cropped;
    }

    /** Chỉ công bố khi 2 khung liên tiếp đọc giống nhau (chống nhiễu video / hiệu ứng lật bài). */
    private void consume(BeadPlateReader.Result r) {
        if (r == null || r.isEmpty()) {
            // Không thấy bảng (app casino có thể chặn chụp màn hình, hoặc bảng đang bị che)
            if (++missing >= 4) OverlayService.publishCells(this, null);
            return;
        }
        missing = 0;
        if (r.cells.equals(lastCandidate)) repeated++;
        else { lastCandidate = r.cells; repeated = 1; }
        if (repeated >= 2 && !r.cells.equals(stableCells)) {
            stableCells = r.cells;
            OverlayService.publishCells(this, r.cells);
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
