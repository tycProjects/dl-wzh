package com.ryomd.nativeapp.youtube;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.ComponentName;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;
import androidx.media3.ui.PlayerView;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Immersive full-screen native video/audio surface attached to MediaSessionService. */
@UnstableApi
public final class RyomdYouTubePlayerActivity extends Activity {
    public static final String EXTRA_TITLE = "ryomd_player_title";
    public static final String EXTRA_ARTIST = "ryomd_player_artist";
    public static final String EXTRA_VIDEO_ID = "ryomd_player_video_id";
    public static final String EXTRA_AUDIO_ONLY = "ryomd_player_audio_only";

    private PlayerView playerView;
    private ListenableFuture<MediaController> controllerFuture;
    private TextView statusView;
    private TextView qualityButton;
    private MediaController mediaController;
    private boolean audioOnlyMode;
    private String currentVideoId = "";
    private int requestedQualityHeight;
    private int selectedQualityHeight;
    private boolean qualityChanging;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService qualityExecutor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        applyImmersiveMode();

        String title = safe(getIntent().getStringExtra(EXTRA_TITLE), "YouTube");
        String artist = safe(getIntent().getStringExtra(EXTRA_ARTIST), "YouTube");
        currentVideoId = safe(getIntent().getStringExtra(EXTRA_VIDEO_ID), "");
        requestedQualityHeight = Math.max(0, getIntent().getIntExtra(
                RyomdYouTubePlaybackService.EXTRA_QUALITY_REQUEST, 0));
        selectedQualityHeight = Math.max(0, getIntent().getIntExtra(
                RyomdYouTubePlaybackService.EXTRA_QUALITY_HEIGHT, 0));
        boolean audioOnly = getIntent().getBooleanExtra(EXTRA_AUDIO_ONLY, false);
        audioOnlyMode = audioOnly;

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        playerView = new PlayerView(this);
        playerView.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        playerView.setUseController(true);
        playerView.setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING);
        playerView.setControllerAutoShow(true);
        playerView.setKeepContentOnPlayerReset(true);
        if (audioOnly) {
            // Mode musik: kontrol selalu terlihat agar tombol putar/jeda/seek tidak hilang.
            playerView.setControllerShowTimeoutMs(0);
            playerView.setControllerHideOnTouch(false);
        }
        root.addView(playerView);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(dp(18), dp(20), dp(18), dp(34));
        header.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xCC000000, 0x00000000}));
        FrameLayout.LayoutParams hp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.TOP);
        root.addView(header, hp);

        LinearLayout topActions = new LinearLayout(this);
        topActions.setOrientation(LinearLayout.HORIZONTAL);
        topActions.setGravity(Gravity.CENTER_VERTICAL);

        TextView back = new TextView(this);
        back.setText("‹   KEMBALI KE RYOMD");
        back.setTextColor(Color.WHITE);
        back.setTextSize(13);
        back.setTypeface(null, android.graphics.Typeface.BOLD);
        back.setMinHeight(dp(48));
        back.setGravity(Gravity.CENTER_VERTICAL);
        back.setPadding(0, dp(6), dp(12), dp(6));
        back.setOnClickListener(v -> finish());
        topActions.addView(back, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        qualityButton = new TextView(this);
        qualityButton.setTextColor(Color.WHITE);
        qualityButton.setTextSize(12);
        qualityButton.setTypeface(null, android.graphics.Typeface.BOLD);
        qualityButton.setGravity(Gravity.CENTER);
        qualityButton.setMinHeight(dp(42));
        qualityButton.setPadding(dp(12), dp(7), dp(12), dp(7));
        GradientDrawable qualityBackground = new GradientDrawable();
        qualityBackground.setColor(0xBB24183D);
        qualityBackground.setCornerRadius(dp(12));
        qualityBackground.setStroke(dp(1), 0x887F61B5);
        qualityButton.setBackground(qualityBackground);
        qualityButton.setOnClickListener(v -> showQualityPicker());
        qualityButton.setVisibility(audioOnly ? View.GONE : View.VISIBLE);
        updateQualityButton();
        topActions.addView(qualityButton, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        header.addView(topActions);

        TextView heading = new TextView(this);
        heading.setText(audioOnly ? "RYOMD MUSIC · NATIVE PLAYER" : "RYOMD · YOUTUBE PLAYER");
        heading.setTextColor(0xFFE5D4FF);
        heading.setTextSize(11);
        heading.setLetterSpacing(0.08f);
        heading.setTypeface(null, android.graphics.Typeface.BOLD);
        header.addView(heading);

        TextView mediaTitle = new TextView(this);
        mediaTitle.setText(title);
        mediaTitle.setTextColor(Color.WHITE);
        mediaTitle.setTextSize(16);
        mediaTitle.setMaxLines(2);
        mediaTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        header.addView(mediaTitle);

        TextView subtitle = new TextView(this);
        subtitle.setText(artist);
        subtitle.setTextColor(0xFFD0C8D8);
        subtitle.setTextSize(12);
        subtitle.setPadding(0, dp(3), 0, 0);
        header.addView(subtitle);

        if (audioOnly) {
            TextView musicMark = new TextView(this);
            musicMark.setText("♫");
            musicMark.setTextColor(0xFFD7C1FF);
            musicMark.setTextSize(82);
            musicMark.setGravity(Gravity.CENTER);
            FrameLayout.LayoutParams mp = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
            mp.setMargins(0, dp(50), 0, dp(40));
            root.addView(musicMark, mp);
        }

        statusView = new TextView(this);
        statusView.setTextColor(0xFFFFE2A8);
        statusView.setTextSize(13);
        statusView.setGravity(Gravity.CENTER);
        statusView.setPadding(dp(24), dp(16), dp(24), dp(16));
        statusView.setText("Menyiapkan stream…");
        statusView.setVisibility(View.VISIBLE);
        root.addView(statusView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER));

        setContentView(root);
        connectController();
    }

    private void connectController() {
        SessionToken token = new SessionToken(this,
                new ComponentName(this, RyomdYouTubePlaybackService.class));
        controllerFuture = new MediaController.Builder(this, token).buildAsync();
        Executor mainExecutor = command -> new Handler(Looper.getMainLooper()).post(command);
        controllerFuture.addListener(() -> {
            try {
                MediaController controller = controllerFuture.get();
                mediaController = controller;
                if (playerView != null) playerView.setPlayer(controller);
                controller.addListener(new Player.Listener() {
                    @Override
                    public void onPlaybackStateChanged(int state) {
                        if (statusView == null) return;
                        if (state == Player.STATE_READY || state == Player.STATE_ENDED) {
                            statusView.setVisibility(View.GONE);
                        } else if (state == Player.STATE_BUFFERING) {
                            statusView.setText("Memuat…");
                            statusView.setVisibility(View.VISIBLE);
                        }
                    }

                    @Override
                    public void onPlayerError(PlaybackException playbackError) {
                        if (statusView == null) return;
                        statusView.setText("Gagal memutar (" + playbackError.getErrorCodeName()
                                + "). Stream mungkin kedaluwarsa atau ditolak YouTube; kembali lalu coba putar lagi.");
                        statusView.setVisibility(View.VISIBLE);
                    }
                });
                if (controller.getPlaybackState() == Player.STATE_READY) {
                    statusView.setVisibility(View.GONE);
                }
            } catch (Exception error) {
                android.widget.Toast.makeText(this,
                        "Tidak dapat menghubungkan pemutar Ryomd. Coba putar ulang dari YouTube Desk.",
                        android.widget.Toast.LENGTH_LONG).show();
                finish();
            }
        }, mainExecutor);
    }

    private void updateQualityButton() {
        if (qualityButton == null) return;
        String label;
        if (requestedQualityHeight > 0) {
            label = "Kualitas " + requestedQualityHeight + "p  ▾";
        } else if (selectedQualityHeight > 0) {
            label = "Otomatis · " + selectedQualityHeight + "p  ▾";
        } else {
            label = "Kualitas Otomatis  ▾";
        }
        qualityButton.setText(label);
    }

    private void showQualityPicker() {
        if (audioOnlyMode || currentVideoId.isEmpty() || qualityChanging || qualityButton == null) return;
        qualityButton.setEnabled(false);
        qualityButton.setText("Memuat opsi…");
        final String videoId = currentVideoId;
        qualityExecutor.execute(() -> {
            try {
                List<RyomdYouTubeExtractor.VideoQuality> available =
                        RyomdYouTubeExtractor.availableVideoQualities(videoId);
                mainHandler.post(() -> {
                    if (isFinishing() || isDestroyed() || qualityButton == null) return;
                    qualityButton.setEnabled(true);
                    updateQualityButton();
                    if (available == null || available.isEmpty()) {
                        new AlertDialog.Builder(this)
                                .setTitle("Kualitas video")
                                .setMessage("NewPipe tidak menemukan pilihan resolusi langsung untuk video ini. Video mungkin hanya menyediakan stream manifest atau format terbatas.")
                                .setPositiveButton("Tutup", null)
                                .show();
                        return;
                    }
                    String[] labels = new String[available.size() + 1];
                    int[] heights = new int[available.size() + 1];
                    labels[0] = "Otomatis (kualitas terbaik tersedia)";
                    heights[0] = 0;
                    int checked = requestedQualityHeight == 0 ? 0 : -1;
                    for (int i = 0; i < available.size(); i++) {
                        RyomdYouTubeExtractor.VideoQuality item = available.get(i);
                        labels[i + 1] = item.label();
                        heights[i + 1] = item.height;
                        if (requestedQualityHeight == item.height) checked = i + 1;
                    }
                    new AlertDialog.Builder(this)
                            .setTitle("Pilih kualitas video")
                            .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                                dialog.dismiss();
                                changeVideoQuality(heights[which]);
                            })
                            .setNegativeButton("Batal", null)
                            .show();
                });
            } catch (Exception error) {
                String message = error.getMessage() == null ? "Tidak dapat membaca pilihan kualitas." : error.getMessage();
                mainHandler.post(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    if (qualityButton != null) {
                        qualityButton.setEnabled(true);
                        updateQualityButton();
                    }
                    Toast.makeText(this, "Kualitas video: " + message, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    /** Re-extracts the selected progressive stream and resumes at the current playback position. */
    private void changeVideoQuality(int requestedHeight) {
        if (audioOnlyMode || qualityChanging || currentVideoId.isEmpty()) return;
        if (requestedHeight == requestedQualityHeight) return;
        qualityChanging = true;
        if (qualityButton != null) {
            qualityButton.setEnabled(false);
            qualityButton.setText("Mengganti kualitas…");
        }
        long resumePosition = mediaController == null ? 0L
                : Math.max(0L, mediaController.getCurrentPosition());
        boolean resumePlaying = mediaController == null || mediaController.getPlayWhenReady();
        if (statusView != null) {
            statusView.setText(requestedHeight > 0
                    ? "Mengambil stream " + requestedHeight + "p…"
                    : "Mengambil kualitas otomatis…");
            statusView.setVisibility(View.VISIBLE);
        }
        final String videoId = currentVideoId;
        qualityExecutor.execute(() -> {
            try {
                RyomdYouTubeExtractor.ResolvedMedia media =
                        RyomdYouTubeExtractor.resolve(videoId, false, requestedHeight);
                mainHandler.post(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    try {
                        Intent service = new Intent(this, RyomdYouTubePlaybackService.class)
                                .setAction(RyomdYouTubePlaybackService.ACTION_PLAY)
                                .putExtra(RyomdYouTubePlaybackService.EXTRA_STREAM_URL, media.streamUrl)
                                .putExtra(RyomdYouTubePlaybackService.EXTRA_AUDIO_URL, media.companionAudioUrl)
                                .putExtra(RyomdYouTubePlaybackService.EXTRA_MIME_TYPE, media.mimeType)
                                .putExtra(RyomdYouTubePlaybackService.EXTRA_TITLE, media.title)
                                .putExtra(RyomdYouTubePlaybackService.EXTRA_ARTIST, media.artist)
                                .putExtra(RyomdYouTubePlaybackService.EXTRA_THUMBNAIL, media.thumbnailUrl)
                                .putExtra(RyomdYouTubePlaybackService.EXTRA_VIDEO_ID, media.videoId)
                                .putExtra(RyomdYouTubePlaybackService.EXTRA_START_POSITION, resumePosition)
                                .putExtra(RyomdYouTubePlaybackService.EXTRA_PLAY_WHEN_READY, resumePlaying)
                                .putExtra(RyomdYouTubePlaybackService.EXTRA_QUALITY_REQUEST, media.requestedHeight)
                                .putExtra(RyomdYouTubePlaybackService.EXTRA_QUALITY_HEIGHT, media.selectedHeight);
                        startService(service);
                        requestedQualityHeight = media.requestedHeight;
                        selectedQualityHeight = media.selectedHeight;
                        getSharedPreferences("ryomd_youtube_player_preferences", MODE_PRIVATE).edit()
                                .putInt("preferred_quality_height", media.requestedHeight).apply();
                        getIntent().putExtra(RyomdYouTubePlaybackService.EXTRA_QUALITY_REQUEST, media.requestedHeight);
                        getIntent().putExtra(RyomdYouTubePlaybackService.EXTRA_QUALITY_HEIGHT, media.selectedHeight);
                        qualityChanging = false;
                        if (qualityButton != null) {
                            qualityButton.setEnabled(true);
                            updateQualityButton();
                        }
                        if (statusView != null) {
                            statusView.setText("Kualitas diubah ke "
                                    + (media.selectedHeight > 0 ? media.selectedHeight + "p." : "mode otomatis."));
                            statusView.setVisibility(View.VISIBLE);
                        }
                    } catch (Exception startError) {
                        qualityChanging = false;
                        if (qualityButton != null) { qualityButton.setEnabled(true); updateQualityButton(); }
                        Toast.makeText(this, "Gagal menerapkan kualitas: " + safe(startError.getMessage(), "kesalahan pemutar"), Toast.LENGTH_LONG).show();
                    }
                });
            } catch (Exception error) {
                String message = error.getMessage() == null ? "Stream kualitas yang dipilih tidak tersedia." : error.getMessage();
                mainHandler.post(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    qualityChanging = false;
                    if (qualityButton != null) { qualityButton.setEnabled(true); updateQualityButton(); }
                    if (statusView != null) { statusView.setText("Gagal mengganti kualitas: " + message); statusView.setVisibility(View.VISIBLE); }
                });
            }
        });
    }

    private void applyImmersiveMode() {
        View decor = getWindow().getDecorView();
        int flags = View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN;
        decor.setSystemUiVisibility(flags);
        if (Build.VERSION.SDK_INT >= 30) {
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.systemBars());
                controller.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) applyImmersiveMode();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static String safe(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    @Override
    protected void onDestroy() {
        // Video berhenti saat layar pemutar ditutup (hemat data/baterai); musik tetap lanjut di latar.
        if (isFinishing() && !audioOnlyMode && controllerFuture != null
                && controllerFuture.isDone()) {
            try {
                MediaController controller = controllerFuture.get();
                controller.stop();
                controller.clearMediaItems();
            } catch (Exception ignored) { }
        }
        if (playerView != null) {
            playerView.setPlayer(null);
            playerView = null;
        }
        mediaController = null;
        qualityExecutor.shutdownNow();
        if (controllerFuture != null) {
            MediaController.releaseFuture(controllerFuture);
            controllerFuture = null;
        }
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        super.onDestroy();
    }
}
