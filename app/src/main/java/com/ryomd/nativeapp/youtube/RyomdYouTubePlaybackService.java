package com.ryomd.nativeapp.youtube;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
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

/** Media3 playback service; audio remains available when Ryomd is backgrounded. */
@UnstableApi
public final class RyomdYouTubePlaybackService extends MediaSessionService {
    static final String ACTION_PLAY = "com.ryomd.nativeapp.youtube.PLAY";
    static final String EXTRA_STREAM_URL = "ryomd_stream_url";
    static final String EXTRA_AUDIO_URL = "ryomd_audio_url";
    static final String EXTRA_MIME_TYPE = "ryomd_mime_type";
    static final String EXTRA_THUMBNAIL = "ryomd_thumbnail";
    static final String EXTRA_TITLE = "ryomd_title";
    static final String EXTRA_ARTIST = "ryomd_artist";
    static final String EXTRA_VIDEO_ID = "ryomd_video_id";
    static final String EXTRA_START_POSITION = "ryomd_start_position_ms";
    static final String EXTRA_PLAY_WHEN_READY = "ryomd_play_when_ready";
    static final String EXTRA_QUALITY_REQUEST = "ryomd_quality_request_height";
    static final String EXTRA_QUALITY_HEIGHT = "ryomd_quality_selected_height";

    private static final String CHANNEL_ID = "ryomd_youtube_playback_v2";
    private ExoPlayer player;
    private MediaSession mediaSession;
    private DefaultMediaSourceFactory mediaSourceFactory;

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
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void startResolvedMedia(Intent intent) {
        String streamUrl = intent.getStringExtra(EXTRA_STREAM_URL);
        String audioUrl = intent.getStringExtra(EXTRA_AUDIO_URL);
        String title = intent.getStringExtra(EXTRA_TITLE);
        String artist = intent.getStringExtra(EXTRA_ARTIST);
        String videoId = intent.getStringExtra(EXTRA_VIDEO_ID);
        String mimeType = intent.getStringExtra(EXTRA_MIME_TYPE);
        String thumbnail = intent.getStringExtra(EXTRA_THUMBNAIL);
        long startPositionMs = Math.max(0L, intent.getLongExtra(EXTRA_START_POSITION, 0L));
        boolean playWhenReady = intent.getBooleanExtra(EXTRA_PLAY_WHEN_READY, true);
        if (streamUrl == null || streamUrl.trim().isEmpty()) return;

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

        if (audioUrl != null && !audioUrl.trim().isEmpty()) {
            MediaItem audioItem = new MediaItem.Builder()
                    .setMediaId((videoId == null ? "ryomd-youtube" : videoId) + "-audio")
                    .setUri(audioUrl)
                    .build();
            androidx.media3.exoplayer.source.MediaSource merged = new MergingMediaSource(
                    mediaSourceFactory.createMediaSource(videoItem),
                    mediaSourceFactory.createMediaSource(audioItem));
            if (startPositionMs > 0) player.setMediaSource(merged, startPositionMs);
            else player.setMediaSource(merged, true);
        } else {
            if (startPositionMs > 0) player.setMediaItem(videoItem, startPositionMs);
            else player.setMediaItem(videoItem, true);
        }
        player.prepare();
        player.setPlayWhenReady(playWhenReady);
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
