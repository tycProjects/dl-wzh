package com.ryomd.nativeapp.youtube;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.Toast;

import org.json.JSONObject;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

/** JavaScript bridge for full-screen native YouTube playback from the Ryomd YouTube Desk. */
public final class RyomdYouTubeBridge {
    private static final ExecutorService EXTRACTOR_EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Pattern VIDEO_ID = Pattern.compile("^[A-Za-z0-9_-]{11}$");
    private final Activity activity;
    private final WebView webView;
    private final Handler main = new Handler(Looper.getMainLooper());

    public RyomdYouTubeBridge(Activity activity, WebView webView) {
        this.activity = activity;
        this.webView = webView;
    }

    @JavascriptInterface
    public void playYouTube(String rawUrl, boolean audioOnly) {
        playYouTubeQuality(rawUrl, audioOnly, 0);
    }

    /** maxHeight=0 means Auto; otherwise resolve a stream at that resolution or the nearest lower one. */
    @JavascriptInterface
    public void playYouTubeQuality(String rawUrl, boolean audioOnly, int maxHeight) {
        final String videoId;
        try {
            videoId = extractVideoId(rawUrl);
        } catch (Exception error) {
            reportStatus("error", "Tautan YouTube tidak valid atau tidak didukung.");
            showToast("Tautan YouTube tidak valid atau tidak didukung.");
            return;
        }
        final int requestedHeight = audioOnly ? 0 : Math.max(0, Math.min(maxHeight, 2160));
        reportStatus("loading", audioOnly ? "Mengambil stream audio melalui NewPipe…"
                : (requestedHeight == 0 ? "Mengambil kualitas otomatis…" : "Mengambil stream " + requestedHeight + "p…"));
        EXTRACTOR_EXECUTOR.execute(() -> {
            try {
                RyomdYouTubeExtractor.ResolvedMedia media =
                        RyomdYouTubeExtractor.resolve(videoId, audioOnly, requestedHeight);
                main.post(() -> startPlayback(media, requestedHeight));
            } catch (Exception error) {
                String message = error.getMessage();
                if (message == null || message.trim().isEmpty()) {
                    message = "NewPipe tidak dapat mengambil stream. Video mungkin dibatasi atau formatnya tidak tersedia.";
                }
                final String safeMessage = message.length() > 220 ? message.substring(0, 220) : message;
                reportStatus("error", safeMessage);
                showToast("YouTube: " + safeMessage);
            }
        });
    }

    private void startPlayback(RyomdYouTubeExtractor.ResolvedMedia media, int requestedHeight) {
        if (activity.isFinishing() || activity.isDestroyed()) return;
        Intent service = new Intent(activity, RyomdYouTubePlaybackService.class)
                .setAction(RyomdYouTubePlaybackService.ACTION_PLAY)
                .putExtra(RyomdYouTubePlaybackService.EXTRA_STREAM_URL, media.streamUrl)
                .putExtra(RyomdYouTubePlaybackService.EXTRA_AUDIO_URL, media.companionAudioUrl)
                .putExtra(RyomdYouTubePlaybackService.EXTRA_MIME_TYPE, media.mimeType)
                .putExtra(RyomdYouTubePlaybackService.EXTRA_TITLE, media.title)
                .putExtra(RyomdYouTubePlaybackService.EXTRA_ARTIST, media.artist)
                .putExtra(RyomdYouTubePlaybackService.EXTRA_THUMBNAIL, media.thumbnailUrl)
                .putExtra(RyomdYouTubePlaybackService.EXTRA_VIDEO_ID, media.videoId)
                .putExtra(RyomdYouTubePlaybackService.EXTRA_AUDIO_ONLY, media.audioOnly)
                .putExtra(RyomdYouTubePlaybackService.EXTRA_MAX_HEIGHT, requestedHeight);
        try {
            // Aplikasi sedang di foreground, jadi startService() diizinkan. JANGAN memakai
            // startForegroundService(): Media3 baru memanggil startForeground() setelah audio
            // benar-benar berjalan, sehingga ekstraksi yang lama memicu
            // ForegroundServiceDidNotStartInTimeException (crash) pada Android 8+.
            activity.startService(service);

            java.util.ArrayList<Integer> heights = new java.util.ArrayList<>();
            for (int height : media.availableHeights) heights.add(height);
            Intent player = new Intent(activity, RyomdYouTubePlayerActivity.class)
                    .putExtra(RyomdYouTubePlayerActivity.EXTRA_TITLE, media.title)
                    .putExtra(RyomdYouTubePlayerActivity.EXTRA_ARTIST, media.artist)
                    .putExtra(RyomdYouTubePlayerActivity.EXTRA_VIDEO_ID, media.videoId)
                    .putExtra(RyomdYouTubePlayerActivity.EXTRA_AUDIO_ONLY, media.audioOnly)
                    .putExtra(RyomdYouTubePlayerActivity.EXTRA_SELECTED_HEIGHT, media.videoHeight)
                    .putExtra(RyomdYouTubePlayerActivity.EXTRA_MAX_HEIGHT, requestedHeight)
                    .putIntegerArrayListExtra(RyomdYouTubePlayerActivity.EXTRA_AVAILABLE_HEIGHTS, heights);
            activity.startActivity(player);
            reportStatus("ready", media.audioOnly
                    ? "Musik diputar melalui Media3. Pemutaran dapat berlanjut di latar belakang."
                    : "Pemutar video native dibuka dalam layar penuh.");
        } catch (Exception error) {
            String message = error.getMessage() == null ? "Tidak dapat membuka pemutar native." : error.getMessage();
            reportStatus("error", message);
            showToast("Tidak dapat memulai pemutar native: " + message);
        }
    }

    /** Dipakai panel web untuk mendeteksi bahwa APK ini punya pemutar native. */
    @JavascriptInterface
    public boolean isAvailable() { return true; }

    /** Menghentikan pemutaran yang sedang berjalan dari panel web. */
    @JavascriptInterface
    public void stopPlayback() {
        main.post(() -> activity.stopService(new Intent(activity, RyomdYouTubePlaybackService.class)));
    }

    private void reportStatus(String state, String message) {
        try {
            JSONObject payload = new JSONObject();
            payload.put("state", state);
            payload.put("message", message == null ? "" : message);
            String script = "(function(){if(typeof window.ryomdNativeYouTubeStatus==='function')window.ryomdNativeYouTubeStatus(" + payload + ");})();";
            main.post(() -> {
                if (webView != null) webView.evaluateJavascript(script, null);
            });
        } catch (Exception ignored) { }
    }

    private void showToast(String message) {
        if (message == null) return;
        String safe = message.length() > 190 ? message.substring(0, 190) : message;
        main.post(() -> Toast.makeText(activity.getApplicationContext(), safe, Toast.LENGTH_LONG).show());
    }

    private static String extractVideoId(String rawUrl) {
        if (rawUrl == null || rawUrl.trim().isEmpty()) throw new IllegalArgumentException("empty");
        String input = rawUrl.trim();
        if (VIDEO_ID.matcher(input).matches()) return input;

        Uri uri = Uri.parse(input);
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
        if (!"https".equals(scheme) && !"http".equals(scheme)) throw new IllegalArgumentException("scheme");
        boolean youtubeHost = "youtu.be".equals(host) || "youtube.com".equals(host)
                || "www.youtube.com".equals(host) || "m.youtube.com".equals(host)
                || "music.youtube.com".equals(host) || "youtube-nocookie.com".equals(host)
                || "www.youtube-nocookie.com".equals(host);
        if (!youtubeHost) throw new IllegalArgumentException("host");

        String id = uri.getQueryParameter("v");
        if (id == null || id.isEmpty()) {
            String path = uri.getPath() == null ? "" : uri.getPath();
            String[] parts = path.split("/");
            for (int i = 0; i < parts.length; i++) {
                if (("shorts".equals(parts[i]) || "embed".equals(parts[i]) || "live".equals(parts[i]))
                        && i + 1 < parts.length) {
                    id = parts[i + 1];
                    break;
                }
            }
            if ((id == null || id.isEmpty()) && "youtu.be".equals(host) && parts.length > 1) id = parts[1];
        }
        if (id == null || !VIDEO_ID.matcher(id).matches()) throw new IllegalArgumentException("id");
        return id;
    }
}
