package com.lsupra.fpsmeter;

import android.app.*;
import android.content.*;
import android.content.pm.ServiceInfo;
import android.graphics.drawable.Icon;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.*;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.*;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Range;
import android.view.Surface;
import android.view.WindowManager;
import android.widget.Toast;
import java.io.File;
import java.nio.ByteBuffer;
import org.json.JSONObject;

/** Quay màn hình: MediaProjection -> bộ mã hóa phần cứng (Surface, không sao chép khung hình) -> MP4. Tới 1080p/1440p, 30-120 fps. */
public class RecService extends Service {
    static volatile int phase = 0;              // 0 rảnh, 1 đếm ngược, 2 đang quay
    static volatile int cdLeft = 0;
    static volatile long startedMs = 0, bytes = 0;
    static volatile String info = "", lastFile = "";

    private final Handler h = new Handler(Looper.getMainLooper());
    private MediaProjection mp;
    private VirtualDisplay vd;
    private MediaCodec venc, aenc;
    private MediaMuxer mux;
    private ParcelFileDescriptor pfd;
    private Uri outUri;
    private AudioRecord ar;
    private Thread vThread, aThread;
    private final Object muxLock = new Object();
    private int tracks = 0, expected = 1;
    private boolean muxStarted = false, abort = false;
    private volatile boolean stopFlag = false, stopping = false;
    private String where = "";
    private static final String CH = "rec";

    @Override public IBinder onBind(Intent i) { return null; }

    private JSONObject cfg() {
        try {
            JSONObject c = new JSONObject(getSharedPreferences("s", MODE_PRIVATE).getString("cfg", "{}")).optJSONObject("rec");
            return c == null ? new JSONObject() : c;
        } catch (Exception e) { return new JSONObject(); }
    }

    private Notification notif(String text) {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26) nm.createNotificationChannel(new NotificationChannel(CH, "Quay màn hình", NotificationManager.IMPORTANCE_LOW));
        PendingIntent pi = PendingIntent.getService(this, 7, new Intent(this, RecService.class).setAction("stop"),
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));
        Notification.Builder b = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(this, CH) : new Notification.Builder(this);
        b.setContentTitle("LONG SUPRA · Quay màn hình").setContentText(text).setSmallIcon(android.R.drawable.ic_media_play).setOngoing(true);
        if (Build.VERSION.SDK_INT >= 23)
            b.addAction(new Notification.Action.Builder(Icon.createWithResource(this, android.R.drawable.ic_media_pause), "DỪNG", pi).build());
        return b.build();
    }

    private void toast(final String s) {
        h.post(new Runnable() { @Override public void run() { Toast.makeText(RecService.this, s, Toast.LENGTH_LONG).show(); } });
    }

    @Override public int onStartCommand(Intent in, int fl, int id) {
        if (in == null) { stopSelf(); return START_NOT_STICKY; }
        if ("stop".equals(in.getAction())) {
            if (phase == 0 && mp == null) { stopSelf(); return START_NOT_STICKY; }
            stopRec();
            return START_NOT_STICKY;
        }
        if (phase != 0) return START_NOT_STICKY;
        JSONObject c = cfg();
        boolean mic = "mic".equals(c.optString("audio", "sys"));
        Notification n = notif("Chuẩn bị quay…");
        if (Build.VERSION.SDK_INT >= 29) {
            int type = ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION;
            if (Build.VERSION.SDK_INT >= 30 && mic) type |= ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE;
            startForeground(2, n, type);
        } else startForeground(2, n);
        try {
            MediaProjectionManager m = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
            mp = m.getMediaProjection(in.getIntExtra("rc", 0), (Intent) in.getParcelableExtra("data"));
            mp.registerCallback(new MediaProjection.Callback() {
                @Override public void onStop() { h.post(new Runnable() { @Override public void run() { stopRec(); } }); }
            }, h);
        } catch (Throwable t) { fail("Không lấy được quyền quay màn hình"); return START_NOT_STICKY; }
        final int cd = Math.max(0, c.optInt("cd", 3));
        phase = 1; cdLeft = cd; bytes = 0; info = ""; lastFile = "";
        h.postDelayed(new Runnable() {
            @Override public void run() {
                if (phase != 1) return;
                if (cdLeft > 0) { cdLeft--; h.postDelayed(this, 1000); } else begin();
            }
        }, cd == 0 ? 700 : 1000);
        return START_NOT_STICKY;
    }

    // ---------- chọn bộ mã hóa phần cứng ----------
    static String pickEncoder(String mime, int w, int h, int fps) {
        try {
            for (MediaCodecInfo ci : new MediaCodecList(MediaCodecList.REGULAR_CODECS).getCodecInfos()) {
                if (!ci.isEncoder()) continue;
                String n = ci.getName();
                if (n.startsWith("OMX.google") || n.startsWith("c2.android")) continue;   // bỏ bộ mã hóa phần mềm (giật)
                for (String t : ci.getSupportedTypes()) {
                    if (!t.equalsIgnoreCase(mime)) continue;
                    try {
                        MediaCodecInfo.VideoCapabilities vc = ci.getCapabilitiesForType(mime).getVideoCapabilities();
                        if (vc.isSizeSupported(w, h) && vc.areSizeAndRateSupported(w, h, fps)) return n;
                    } catch (Exception ignored) {}
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private static int maxFps(String mime) {
        for (int f : new int[]{120, 90, 60, 30}) if (pickEncoder(mime, 1920, 1080, f) != null) return f;
        return 0;
    }

    static String caps(Context c) {
        float hz = 60;
        try { hz = ((WindowManager) c.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRefreshRate(); } catch (Exception ignored) {}
        return "{\"avc\":" + maxFps("video/avc") + ",\"hevc\":" + maxFps("video/hevc") + ",\"hz\":" + Math.round(hz) + "}";
    }

    // ---------- bắt đầu quay ----------
    private void begin() {
        try {
            JSONObject c = cfg();
            WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
            DisplayMetrics dm = new DisplayMetrics();
            wm.getDefaultDisplay().getRealMetrics(dm);
            int dl = Math.max(dm.widthPixels, dm.heightPixels), ds = Math.min(dm.widthPixels, dm.heightPixels);
            String orient = c.optString("orient", "land");
            boolean land = orient.equals("land") || (orient.equals("auto") && dm.widthPixels > dm.heightPixels);
            int res = c.optInt("res", 1080), fps = c.optInt("fps", 120);
            String mime = "hevc".equals(c.optString("codec", "hevc")) ? "video/hevc" : "video/avc";
            int[] chain = {fps, 90, 60, 30};
            int W = 0, H = 0, useFps = 0;
            String enc = null;
            outer:
            for (int rr : new int[]{res, 1080, 720}) {
                if (rr > res) continue;
                int s = Math.min(rr, ds);
                int lw = (int) (Math.round(s * dl / (double) ds) / 16) * 16, sh = s & ~1;
                W = land ? lw : sh; H = land ? sh : lw;
                for (int fr : chain) {
                    if (fr > fps) continue;
                    for (String mm : new String[]{mime, "video/avc"}) {
                        enc = pickEncoder(mm, W, H, fr);
                        if (enc != null) { mime = mm; useFps = fr; break outer; }
                    }
                }
            }
            if (enc == null) throw new RuntimeException("Máy không có bộ mã hóa phần cứng phù hợp");
            venc = MediaCodec.createByCodecName(enc);
            Range<Integer> br = venc.getCodecInfo().getCapabilitiesForType(mime).getVideoCapabilities().getBitrateRange();
            int bps = Math.max(br.getLower(), Math.min(br.getUpper(), c.optInt("mbps", 40) * 1000000));
            MediaFormat f = MediaFormat.createVideoFormat(mime, W, H);
            f.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
            f.setInteger(MediaFormat.KEY_BIT_RATE, bps);
            f.setInteger(MediaFormat.KEY_FRAME_RATE, useFps);
            f.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
            f.setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR);
            if (Build.VERSION.SDK_INT >= 23) f.setInteger(MediaFormat.KEY_PRIORITY, 0);
            venc.configure(f, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            Surface surf = venc.createInputSurface();
            venc.start();
            openOut();
            boolean audio = false;
            String au = c.optString("audio", "sys");
            if (!au.equals("none")) {
                try { audio = setupAudio(au.equals("sys")); }
                catch (Throwable t) { audio = false; toast("Không thu được âm thanh, vẫn quay hình"); }
            }
            expected = audio ? 2 : 1;
            vThread = new Thread(new Runnable() { @Override public void run() { videoLoop(); } }, "rec-video");
            vThread.start();
            if (audio) { aThread = new Thread(new Runnable() { @Override public void run() { audioLoop(); } }, "rec-audio"); aThread.start(); }
            vd = mp.createVirtualDisplay("LSRec", W, H, dm.densityDpi, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, surf, null, null);
            info = Math.min(W, H) + "p · " + useFps + "fps · " + (mime.contains("hevc") ? "HEVC" : "H.264") + " · " + (bps / 1000000) + " Mbps";
            if (useFps < fps || Math.min(W, H) < Math.min(res, ds)) info += " (máy không hỗ trợ đủ cấu hình đã chọn)";
            startedMs = SystemClock.elapsedRealtime();
            phase = 2;
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).notify(2, notif(info));
        } catch (Throwable t) {
            fail("Không bắt đầu quay được: " + t.getMessage());
        }
    }

    private void openOut() throws Exception {
        String name = "LongSupra_" + new java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US).format(new java.util.Date()) + ".mp4";
        if (Build.VERSION.SDK_INT >= 29) {
            ContentValues cv = new ContentValues();
            cv.put(MediaStore.Video.Media.DISPLAY_NAME, name);
            cv.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
            cv.put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/LongSupra");
            cv.put(MediaStore.Video.Media.IS_PENDING, 1);
            outUri = getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, cv);
            pfd = getContentResolver().openFileDescriptor(outUri, "rw");
            mux = new MediaMuxer(pfd.getFileDescriptor(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            where = "Movies/LongSupra/" + name;
        } else {
            File d = getExternalFilesDir(Environment.DIRECTORY_MOVIES);
            File fo = new File(d, name);
            mux = new MediaMuxer(fo.getAbsolutePath(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            where = fo.getAbsolutePath();
        }
        lastFile = name;
    }

    private boolean setupAudio(boolean sys) throws Exception {
        if (sys && Build.VERSION.SDK_INT < 29) return false;
        int sr = 48000, ch = AudioFormat.CHANNEL_IN_STEREO;
        AudioFormat af = new AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(sr).setChannelMask(ch).build();
        int buf = Math.max(AudioRecord.getMinBufferSize(sr, ch, AudioFormat.ENCODING_PCM_16BIT) * 4, 32768);
        AudioRecord.Builder b = new AudioRecord.Builder().setAudioFormat(af).setBufferSizeInBytes(buf);
        if (sys) {
            AudioPlaybackCaptureConfiguration pc = new AudioPlaybackCaptureConfiguration.Builder(mp)
                    .addMatchingUsage(AudioAttributes.USAGE_GAME).addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                    .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN).build();
            b.setAudioPlaybackCaptureConfig(pc);
        } else b.setAudioSource(MediaRecorder.AudioSource.MIC);
        ar = b.build();
        if (ar.getState() != AudioRecord.STATE_INITIALIZED) throw new RuntimeException("audio init");
        aenc = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC);
        MediaFormat a = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sr, 2);
        a.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        a.setInteger(MediaFormat.KEY_BIT_RATE, 192000);
        a.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384);
        aenc.configure(a, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        aenc.start();
        ar.startRecording();
        return true;
    }

    private int addTrack(MediaFormat f) {
        synchronized (muxLock) {
            int t = mux.addTrack(f);
            tracks++;
            if (tracks >= expected) { mux.start(); muxStarted = true; muxLock.notifyAll(); }
            else {
                while (!muxStarted && !abort) { try { muxLock.wait(200); } catch (InterruptedException e) { break; } }
            }
            return t;
        }
    }

    private void audioFailed() {
        synchronized (muxLock) {
            expected = 1;
            if (tracks >= 1 && !muxStarted) { mux.start(); muxStarted = true; muxLock.notifyAll(); }
        }
    }

    // ---------- luồng ghi hình ----------
    private void videoLoop() {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_DISPLAY);
        MediaCodec.BufferInfo bi = new MediaCodec.BufferInfo();
        int vTrack = -1, idle = 0;
        long v0 = -1;
        try {
            while (true) {
                int i = venc.dequeueOutputBuffer(bi, 10000);
                if (i == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) vTrack = addTrack(venc.getOutputFormat());
                else if (i >= 0) {
                    idle = 0;
                    ByteBuffer b = venc.getOutputBuffer(i);
                    if ((bi.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) bi.size = 0;
                    if (bi.size > 0 && muxStarted && vTrack >= 0) {
                        if (v0 < 0) v0 = bi.presentationTimeUs;
                        bi.presentationTimeUs -= v0;
                        b.position(bi.offset); b.limit(bi.offset + bi.size);
                        synchronized (muxLock) { mux.writeSampleData(vTrack, b, bi); }
                        bytes += bi.size;
                    }
                    venc.releaseOutputBuffer(i, false);
                    if ((bi.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) break;
                } else if (i == MediaCodec.INFO_TRY_AGAIN_LATER && stopFlag && ++idle > 300) break;
            }
        } catch (Throwable t) { toast("Lỗi ghi hình: " + t.getMessage()); }
    }

    private void audioLoop() {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
        MediaCodec.BufferInfo bi = new MediaCodec.BufferInfo();
        byte[] buf = new byte[4096];
        long base = System.nanoTime() / 1000, last = 0;
        int[] tr = {-1};
        try {
            while (!stopFlag) {
                int n = ar.read(buf, 0, buf.length);
                if (n <= 0) continue;
                int idx = aenc.dequeueInputBuffer(10000);
                if (idx >= 0) {
                    ByteBuffer in = aenc.getInputBuffer(idx);
                    in.clear(); in.put(buf, 0, n);
                    last = System.nanoTime() / 1000 - base;
                    aenc.queueInputBuffer(idx, 0, n, last, 0);
                }
                drainAudio(bi, tr, false);
            }
            int idx = aenc.dequeueInputBuffer(100000);
            if (idx >= 0) aenc.queueInputBuffer(idx, 0, 0, last + 1000, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
            drainAudio(bi, tr, true);
        } catch (Throwable t) { audioFailed(); }
    }

    private void drainAudio(MediaCodec.BufferInfo bi, int[] tr, boolean eos) {
        int tries = 0;
        while (true) {
            int i = aenc.dequeueOutputBuffer(bi, eos ? 10000 : 0);
            if (i == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) tr[0] = addTrack(aenc.getOutputFormat());
            else if (i >= 0) {
                ByteBuffer b = aenc.getOutputBuffer(i);
                if ((bi.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) bi.size = 0;
                if (bi.size > 0 && muxStarted && tr[0] >= 0) {
                    b.position(bi.offset); b.limit(bi.offset + bi.size);
                    synchronized (muxLock) { mux.writeSampleData(tr[0], b, bi); }
                }
                aenc.releaseOutputBuffer(i, false);
                if ((bi.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) return;
            } else if (i == MediaCodec.INFO_TRY_AGAIN_LATER) {
                if (!eos || ++tries > 100) return;
            }
        }
    }

    // ---------- dừng ----------
    private void fail(String msg) {
        toast(msg);
        stopping = true;
        phase = 0;
        stopFlag = true;
        new Thread(new Runnable() { @Override public void run() { finish(false); } }).start();
    }

    private void stopRec() {
        if (stopping) return;
        stopping = true;
        h.removeCallbacksAndMessages(null);
        final boolean was = phase == 2;
        phase = 0;
        stopFlag = true;
        new Thread(new Runnable() { @Override public void run() { finish(was); } }).start();
    }

    private void join(Thread t) { try { if (t != null) t.join(4000); } catch (Exception ignored) {} }

    private void finish(boolean was) {
        try { if (vd != null) vd.release(); } catch (Throwable ignored) {}
        try { if (venc != null && was) venc.signalEndOfInputStream(); } catch (Throwable ignored) {}
        join(vThread); join(aThread);
        try { if (venc != null) { venc.stop(); venc.release(); } } catch (Throwable ignored) {}
        try { if (aenc != null) { aenc.stop(); aenc.release(); } } catch (Throwable ignored) {}
        try { if (ar != null) { ar.stop(); ar.release(); } } catch (Throwable ignored) {}
        synchronized (muxLock) { abort = true; muxLock.notifyAll(); }
        try { if (mux != null) { if (muxStarted) mux.stop(); mux.release(); } } catch (Throwable ignored) {}
        try { if (pfd != null) pfd.close(); } catch (Throwable ignored) {}
        if (outUri != null && Build.VERSION.SDK_INT >= 29) {
            try {
                if (bytes > 0) { ContentValues cv = new ContentValues(); cv.put(MediaStore.Video.Media.IS_PENDING, 0); getContentResolver().update(outUri, cv, null, null); }
                else getContentResolver().delete(outUri, null, null);
            } catch (Throwable ignored) {}
        }
        try { if (mp != null) mp.stop(); } catch (Throwable ignored) {}
        if (was) toast(bytes > 0 ? "Đã lưu video: " + where : "Video trống, chưa lưu");
        stopForeground(true);
        stopSelf();
    }

    @Override public void onDestroy() {
        phase = 0;
        super.onDestroy();
    }
}
