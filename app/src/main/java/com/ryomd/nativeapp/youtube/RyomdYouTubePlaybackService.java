package com.ryomd.nativeapp.youtube;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.Build;

import androidx.annotation.Nullable;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.exoplayer.source.MergingMediaSource;
import androidx.media3.session.DefaultMediaNotificationProvider;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaSessionService;

import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Media3 playback service; audio remains available when Ryomd is backgrounded. */
@UnstableApi
public final class RyomdYouTubePlaybackService extends MediaSessionService {
    static final String ACTION_PLAY = "com.ryomd.nativeapp.youtube.PLAY";
    static final String ACTION_SET_QUALITY = "com.ryomd.nativeapp.youtube.SET_QUALITY";
    static final String ACTION_QUALITY_RESULT = "com.ryomd.nativeapp.youtube.QUALITY_RESULT";
    static final String EXTRA_MAX_HEIGHT = "ryomd_max_height";
    static final String EXTRA_QUALITY_SUCCESS = "ryomd_quality_success";
    static final String EXTRA_QUALITY_HEIGHT = "ryomd_quality_height";
    static final String EXTRA_QUALITY_ERROR = "ryomd_quality_error";
    static final String EXTRA_STREAM_URL = "ryomd_stream_url";
    static final String EXTRA_AUDIO_URL = "ryomd_audio_url";
    static final String EXTRA_MIME_TYPE = "ryomd_mime_type";
    static final String EXTRA_THUMBNAIL = "ryomd_thumbnail";
    static final String EXTRA_TITLE = "ryomd_title";
    static final String EXTRA_ARTIST = "ryomd_artist";
    static final String EXTRA_VIDEO_ID = "ryomd_video_id";
    static final String EXTRA_AUDIO_ONLY = "ryomd_audio_only";

    private static final String CHANNEL_ID = "ryomd_youtube_playback_v2";
    private ExoPlayer player;
    private MediaSession mediaSession;
    private DefaultMediaSourceFactory mediaSourceFactory;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService qualityExecutor = Executors.newSingleThreadExecutor();
    private String currentVideoId;
    private String currentTitle;
    private String currentArtist;
    private String currentThumbnail;
    private boolean currentAudioOnly;
    private int currentMaxHeight;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        setMediaNotificationProvider(new DefaultMediaNotificationProvider.Builder(this)
                .setChannelId(CHANNEL_ID)
                .build());

        DefaultHttpDataSource.Factory httpFactory = new DefaultHttpDataSource.Factory()
                .setUserAgent("Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 " +
                        "(KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36")
                .setAllowCrossProtocolRedirects(true)
                .setConnectTimeoutMs((int) TimeUnit.SECONDS.toMillis(20))
                .setReadTimeoutMs((int) TimeUnit.SECONDS.toMillis(40))
                .setDefaultRequestProperties(Collections.singletonMap(
                        "Referer", "https://www.youtube.com/"));
        DefaultDataSource.Factory dataSourceFactory = new DefaultDataSource.Factory(this, httpFactory);
        mediaSourceFactory = new DefaultMediaSourceFactory(dataSourceFactory);

        player = new ExoPlayer.Builder(this)
                .setMediaSourceFactory(mediaSourceFactory)
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                        .build(), true)
                .setHandleAudioBecomingNoisy(true)
                .setWakeMode(C.WAKE_MODE_NETWORK)
                .build();
        player.addListener(new Player.Listener() {
            @Override
            public void onPlayerError(androidx.media3.common.PlaybackException error) {
                android.util.Log.e("RyomdYouTube", "Playback error " + error.getErrorCodeName(), error);
            }
        });
        mediaSession = new MediaSession.Builder(this, player).build();
    }

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        if (intent != null && ACTION_PLAY.equals(intent.getAction())) {
            try {
                startResolvedMedia(intent);
            } catch (RuntimeException error) {
                android.util.Log.e("RyomdYouTube", "Unable to start media", error);
                stopSelf(startId);
            }
        } else if (intent != null && ACTION_SET_QUALITY.equals(intent.getAction())) {
            requestQuality(intent.getIntExtra(EXTRA_MAX_HEIGHT, 0));
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void startResolvedMedia(Intent intent) {
        String streamUrl = intent.getStringExtra(EXTRA_STREAM_URL);
        String audioUrl = intent.getStringExtra(EXTRA_AUDIO_URL);
        currentTitle = intent.getStringExtra(EXTRA_TITLE);
        currentArtist = intent.getStringExtra(EXTRA_ARTIST);
        currentVideoId = intent.getStringExtra(EXTRA_VIDEO_ID);
        String mimeType = intent.getStringExtra(EXTRA_MIME_TYPE);
        currentThumbnail = intent.getStringExtra(EXTRA_THUMBNAIL);
        currentMaxHeight = intent.getIntExtra(EXTRA_MAX_HEIGHT, 0);
        currentAudioOnly = intent.getBooleanExtra(EXTRA_AUDIO_ONLY, false);
        if (streamUrl == null || streamUrl.trim().isEmpty()) return;
        applySource(streamUrl, audioUrl, mimeType, currentVideoId, currentTitle, currentArtist,
                currentThumbnail, 0L, true);
    }

    private void applySource(String streamUrl, String audioUrl, String mimeType, String videoId,
                             String title, String artist, String thumbnail, long resumePosition,
                             boolean playWhenReady) {
        MediaMetadata.Builder metadataBuilder = new MediaMetadata.Builder()
                .setTitle(title == null ? "YouTube" : title)
                .setArtist(artist == null ? "YouTube" : artist);
        if (thumbnail != null && !thumbnail.trim().isEmpty()) {
            metadataBuilder.setArtworkUri(android.net.Uri.parse(thumbnail));
        }
        MediaMetadata metadata = metadataBuilder.build();
        MediaItem.Builder videoBuilder = new MediaItem.Builder()
                .setMediaId(videoId == null ? "ryomd-youtube" : videoId)
                .setUri(streamUrl)
                .setMediaMetadata(metadata);
        if (mimeType != null && !mimeType.trim().isEmpty()) videoBuilder.setMimeType(mimeType);
        MediaItem videoItem = videoBuilder.build();
        androidx.media3.exoplayer.source.MediaSource source;
        if (audioUrl != null && !audioUrl.trim().isEmpty()) {
            MediaItem audioItem = new MediaItem.Builder()
                    .setMediaId((videoId == null ? "ryomd-youtube" : videoId) + "-audio")
                    .setUri(audioUrl)
                    .build();
            source = new MergingMediaSource(
                    mediaSourceFactory.createMediaSource(videoItem),
                    mediaSourceFactory.createMediaSource(audioItem));
        } else {
            source = mediaSourceFactory.createMediaSource(videoItem);
        }
        player.setMediaSource(source, true);
        if (resumePosition > 0) player.seekTo(resumePosition);
        player.prepare();
        player.setPlayWhenReady(playWhenReady);
    }

    private void requestQuality(int requestedHeight) {
        final String videoId = currentVideoId;
        if (videoId == null || videoId.trim().isEmpty() || currentAudioOnly) {
            broadcastQualityResult(false, 0, "Pemilihan resolusi hanya tersedia untuk video yang sedang diputar.");
            return;
        }
        final int requested = Math.max(0, Math.min(requestedHeight, 2160));
        final long resumePosition = player == null ? 0L : player.getCurrentPosition();
        final boolean playWhenReady = player != null && player.getPlayWhenReady();
        qualityExecutor.execute(() -> {
            try {
                RyomdYouTubeExtractor.ResolvedMedia media =
                        RyomdYouTubeExtractor.resolve(videoId, false, requested);
                main.post(() -> {
                    if (player == null || !videoId.equals(currentVideoId)) return;
                    applySource(media.streamUrl, media.companionAudioUrl, media.mimeType,
                            media.videoId, media.title, media.artist, media.thumbnailUrl,
                            resumePosition, playWhenReady);
                    currentMaxHeight = requested;
                    currentTitle = media.title;
                    currentArtist = media.artist;
                    currentThumbnail = media.thumbnailUrl;
                    broadcastQualityResult(true, media.videoHeight, null);
                });
            } catch (Exception error) {
                String message = error.getMessage();
                if (message == null || message.trim().isEmpty()) message = "Resolusi ini tidak tersedia untuk video tersebut.";
                final String errorText = message.length() > 180 ? message.substring(0, 180) : message;
                main.post(() -> broadcastQualityResult(false, 0, errorText));
            }
        });
    }

    private void broadcastQualityResult(boolean success, int height, String error) {
        Intent result = new Intent(ACTION_QUALITY_RESULT)
                .setPackage(getPackageName())
                .putExtra(EXTRA_QUALITY_SUCCESS, success)
                .putExtra(EXTRA_QUALITY_HEIGHT, height);
        if (error != null) result.putExtra(EXTRA_QUALITY_ERROR, error);
        sendBroadcast(result);
    }

    @Nullable
    @Override
    public MediaSession onGetSession(MediaSession.ControllerInfo controllerInfo) {
        return mediaSession;
    }

    @Override
    public void onTaskRemoved(@Nullable Intent rootIntent) {
        Player current = mediaSession == null ? null : mediaSession.getPlayer();
        if (current == null || (!current.isPlaying() && current.getPlaybackState() != Player.STATE_BUFFERING)) {
            stopSelf();
        }
    }

    @Override
    public void onDestroy() {
        qualityExecutor.shutdownNow();
        main.removeCallbacksAndMessages(null);
        if (mediaSession != null) {
            mediaSession.release();
            mediaSession = null;
        }
        if (player != null) {
            player.release();
            player = null;
        }
        mediaSourceFactory = null;
        super.onDestroy();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Ryomd YouTube Player", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Kontrol pemutaran YouTube native Ryomd");
            channel.setShowBadge(false);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }
}
