package com.ryomd.nativeapp.youtube;

import android.app.Activity;
import android.content.ComponentName;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import android.widget.Button;
import android.widget.PopupMenu;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;
import androidx.media3.ui.PlayerView;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.Executor;

/** Immersive full-screen native video/audio surface attached to MediaSessionService. */
@UnstableApi
public final class RyomdYouTubePlayerActivity extends Activity {
    public static final String EXTRA_TITLE = "ryomd_player_title";
    public static final String EXTRA_ARTIST = "ryomd_player_artist";
    public static final String EXTRA_VIDEO_ID = "ryomd_player_video_id";
    public static final String EXTRA_AUDIO_ONLY = "ryomd_player_audio_only";
    public static final String EXTRA_SELECTED_HEIGHT = "ryomd_player_selected_height";
    public static final String EXTRA_MAX_HEIGHT = "ryomd_player_max_height";
    public static final String EXTRA_AVAILABLE_HEIGHTS = "ryomd_player_available_heights";

    private PlayerView playerView;
    private ListenableFuture<MediaController> controllerFuture;
    private TextView statusView;
    private boolean audioOnlyMode;
    private Button qualityButton;
    private java.util.ArrayList<Integer> availableHeights = new java.util.ArrayList<>();
    private int selectedHeight;
    private int requestedMaxHeight;
    private int pendingMaxHeight;
    private boolean qualityReceiverRegistered;
    private final BroadcastReceiver qualityReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (intent == null || qualityButton == null) return;
            boolean success = intent.getBooleanExtra(RyomdYouTubePlaybackService.EXTRA_QUALITY_SUCCESS, false);
            if (qualityButton != null) qualityButton.setEnabled(true);
            if (!success) {
                String message = intent.getStringExtra(RyomdYouTubePlaybackService.EXTRA_QUALITY_ERROR);
                android.widget.Toast.makeText(RyomdYouTubePlayerActivity.this,
                        message == null ? "Tidak dapat mengubah kualitas video." : message,
                        android.widget.Toast.LENGTH_LONG).show();
                qualityButton.setText(qualityLabel());
                return;
            }
            selectedHeight = intent.getIntExtra(RyomdYouTubePlaybackService.EXTRA_QUALITY_HEIGHT, 0);
            requestedMaxHeight = pendingMaxHeight;
            qualityButton.setText(qualityLabel());
            String quality = selectedHeight > 0 ? selectedHeight + "p" : "Otomatis";
            android.widget.Toast.makeText(RyomdYouTubePlayerActivity.this,
                    "Kualitas video: " + quality, android.widget.Toast.LENGTH_SHORT).show();
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        applyImmersiveMode();

        String title = safe(getIntent().getStringExtra(EXTRA_TITLE), "YouTube");
        String artist = safe(getIntent().getStringExtra(EXTRA_ARTIST), "YouTube");
        boolean audioOnly = getIntent().getBooleanExtra(EXTRA_AUDIO_ONLY, false);
        audioOnlyMode = audioOnly;
        selectedHeight = getIntent().getIntExtra(EXTRA_SELECTED_HEIGHT, 0);
        requestedMaxHeight = getIntent().getIntExtra(EXTRA_MAX_HEIGHT, 0);
        java.util.ArrayList<Integer> received = getIntent().getIntegerArrayListExtra(EXTRA_AVAILABLE_HEIGHTS);
        if (received != null) availableHeights = received;

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

        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(topRow, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView back = new TextView(this);
        back.setText("‹  KEMBALI");
        back.setTextColor(Color.WHITE);
        back.setTextSize(13);
        back.setTypeface(null, android.graphics.Typeface.BOLD);
        back.setMinHeight(dp(48));
        back.setGravity(Gravity.CENTER_VERTICAL);
        back.setPadding(0, dp(6), dp(12), dp(6));
        back.setOnClickListener(v -> finish());
        topRow.addView(back, new LinearLayout.LayoutParams(0, dp(48), 1f));

        if (!audioOnly) {
            qualityButton = new Button(this);
            qualityButton.setAllCaps(false);
            qualityButton.setTextSize(12);
            qualityButton.setTypeface(null, android.graphics.Typeface.BOLD);
            qualityButton.setTextColor(Color.WHITE);
            qualityButton.setMinHeight(dp(44));
            qualityButton.setPadding(dp(12), 0, dp(12), 0);
            GradientDrawable qualityBg = new GradientDrawable();
            qualityBg.setColor(0xAA252525);
            qualityBg.setCornerRadius(dp(22));
            qualityBg.setStroke(dp(1), 0x66FFFFFF);
            qualityButton.setBackground(qualityBg);
            qualityButton.setContentDescription("Pilih kualitas atau resolusi video");
            qualityButton.setText(qualityLabel());
            qualityButton.setOnClickListener(v -> showQualityMenu(v));
            topRow.addView(qualityButton, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, dp(48)));
        }

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

    private String qualityLabel() {
        if (requestedMaxHeight <= 0) {
            return selectedHeight > 0 ? "Kualitas · Auto · " + selectedHeight + "p" : "Kualitas · Otomatis";
        }
        return "Kualitas · " + (selectedHeight > 0 ? selectedHeight + "p" : requestedMaxHeight + "p");
    }

    private void showQualityMenu(View anchor) {
        if (audioOnlyMode || qualityButton == null) return;
        PopupMenu menu = new PopupMenu(this, anchor);
        menu.getMenu().add(0, 0, 0, "Otomatis" + (requestedMaxHeight <= 0 ? "  ✓" : ""));
        java.util.TreeSet<Integer> sorted = new java.util.TreeSet<>(java.util.Collections.reverseOrder());
        for (Integer h : availableHeights) if (h != null && h > 0) sorted.add(h);
        int order = 1;
        for (Integer height : sorted) {
            menu.getMenu().add(0, height, order++, height + "p" + (requestedMaxHeight == height ? "  ✓" : ""));
        }
        if (sorted.isEmpty()) {
            menu.getMenu().add(0, -1, order, "Resolusi manual tidak tersedia").setEnabled(false);
        }
        menu.setOnMenuItemClickListener(item -> {
            int target = item.getItemId();
            if (target < 0) return false;
            requestQuality(target);
            return true;
        });
        menu.show();
    }

    private void requestQuality(int maxHeight) {
        if (qualityButton == null) return;
        pendingMaxHeight = maxHeight;
        qualityButton.setText("Kualitas · Menerapkan…");
        qualityButton.setEnabled(false);
        Intent request = new Intent(this, RyomdYouTubePlaybackService.class)
                .setAction(RyomdYouTubePlaybackService.ACTION_SET_QUALITY)
                .putExtra(RyomdYouTubePlaybackService.EXTRA_MAX_HEIGHT, maxHeight);
        try {
            startService(request);
        } catch (Exception error) {
            qualityButton.setEnabled(true);
            qualityButton.setText(qualityLabel());
            android.widget.Toast.makeText(this, "Tidak dapat mengubah kualitas video.",
                    android.widget.Toast.LENGTH_LONG).show();
        }
    }

    @Override protected void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter(RyomdYouTubePlaybackService.ACTION_QUALITY_RESULT);
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(qualityReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(qualityReceiver, filter);
        }
        qualityReceiverRegistered = true;
    }

    @Override protected void onStop() {
        if (qualityReceiverRegistered) {
            try { unregisterReceiver(qualityReceiver); } catch (IllegalArgumentException ignored) { }
            qualityReceiverRegistered = false;
        }
        super.onStop();
    }

    private void connectController() {
        SessionToken token = new SessionToken(this,
                new ComponentName(this, RyomdYouTubePlaybackService.class));
        controllerFuture = new MediaController.Builder(this, token).buildAsync();
        Executor mainExecutor = command -> new Handler(Looper.getMainLooper()).post(command);
        controllerFuture.addListener(() -> {
            try {
                MediaController controller = controllerFuture.get();
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
        if (controllerFuture != null) {
            MediaController.releaseFuture(controllerFuture);
            controllerFuture = null;
        }
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        super.onDestroy();
    }
}
