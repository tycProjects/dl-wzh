package com.ryan.autotyper.ui.home;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.ryan.autotyper.AppState;
import com.ryan.autotyper.AutoTyperService;
import com.ryan.autotyper.PrefsHelper;
import com.ryan.autotyper.R;
import com.ryan.autotyper.Snack;
import com.ryan.autotyper.ui.AnimKit;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Home tab v3.5 — admin hero + accessibility + Start/Stop + status + onboarding.
 *
 * Avatar effects (layered, all animated):
 *   • Outer rotating sweep progress ring (fast, 1.2s/rev)
 *   • Mid rotating color ring (4 arcs, slow 8s/rev)
 *   • Inner pulsing glow (scale + alpha 2.2s)
 *   • 4 sparkle particles orbiting the avatar (translate + alpha 2.5s)
 *
 * Status card shows current typing progress (from AutoTyperService broadcasts).
 * Onboarding card shown on first run, dismissed permanently on tap.
 */
public class HomeFragment extends Fragment {

    private TextView tvAccessibilityStatus;
    private Button btnOpenAccessibility, btnStart, btnStop;
    private View avatarRing, avatarGlow, avatarProgressRing;

    // Status indicator
    private View statusCard;
    private TextView statusTitle, statusProgress, statusMode;

    // Onboarding
    private View onboardingCard;
    private Button btnGotIt;

    // Sparkle particles
    private final List<View> sparkles = new ArrayList<>();

    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    /** Receiver for status broadcasts from AutoTyperService. */
    private final BroadcastReceiver statusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!"com.ryan.autotyper.ACTION_STATUS".equals(intent.getAction())) return;
            int typed = intent.getIntExtra("typed", 0);
            int total = intent.getIntExtra("total", 0);
            boolean running = intent.getBooleanExtra("running", false);
            boolean session = intent.getBooleanExtra("session", false);
            updateStatusCard(typed, total, running, session);
        }
    };

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvAccessibilityStatus = view.findViewById(R.id.tvAccessibilityStatus);
        btnOpenAccessibility = view.findViewById(R.id.btnOpenAccessibility);
        btnStart = view.findViewById(R.id.btnStart);
        btnStop  = view.findViewById(R.id.btnStop);
        avatarRing = view.findViewById(R.id.avatarRing);
        avatarGlow = view.findViewById(R.id.avatarGlow);
        avatarProgressRing = view.findViewById(R.id.avatarProgressRing);

        // Status card
        statusCard     = view.findViewById(R.id.statusCard);
        statusTitle    = view.findViewById(R.id.statusTitle);
        statusProgress = view.findViewById(R.id.statusProgress);
        statusMode     = view.findViewById(R.id.statusMode);

        // Onboarding card
        onboardingCard = view.findViewById(R.id.onboardingCard);
        btnGotIt       = view.findViewById(R.id.btnGotIt);

        // === Avatar effects ===
        // 1. Sweep progress ring (fast)
        Animation rotateProgress = AnimationUtils.loadAnimation(requireContext(), R.anim.rotate_progress);
        avatarProgressRing.startAnimation(rotateProgress);

        // 2. Color ring rotation
        Animation rotate = AnimationUtils.loadAnimation(requireContext(), R.anim.rotate_avatar);
        rotate.setStartTime(0);
        avatarRing.startAnimation(rotate);

        // 3. Glow pulse
        Animation pulse = AnimationUtils.loadAnimation(requireContext(), R.anim.pulse_avatar_glow);
        avatarGlow.startAnimation(pulse);

        // 4. Sparkle particles
        sparkles.add(view.findViewById(R.id.sparkle1));
        sparkles.add(view.findViewById(R.id.sparkle2));
        sparkles.add(view.findViewById(R.id.sparkle3));
        sparkles.add(view.findViewById(R.id.sparkle4));
        for (int i = 0; i < sparkles.size(); i++) {
            View sp = sparkles.get(i);
            if (sp == null) continue;
            Animation a = AnimationUtils.loadAnimation(requireContext(), R.anim.sparkle_orbit);
            a.setStartOffset(i * 350L);
            sp.startAnimation(a);
        }

        // Pop-in the cards (replaced by staggered entrance on the root LinearLayout)
        View rootLinear = view.findViewById(R.id.home_root);
        if (rootLinear != null) {
            AnimKit.staggeredEntrance(rootLinear, 60L);
        }
        // Pulse glow on Start button when ready (accessibility on, no session active)
        if (btnStart != null) {
            btnStart.postDelayed(() -> {
                Animation pulseReady = AnimationUtils.loadAnimation(requireContext(), R.anim.pulse_ready);
                btnStart.startAnimation(pulseReady);
            }, 1200);
        }

        // Button listeners — with haptic feedback
        btnOpenAccessibility.setOnClickListener(v -> {
            haptic();
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(intent);
            if (getView() != null) {
                Snack.show(getView(), "Tìm và bật \"YEU EM MOI VU TRU\"");
            }
        });

        btnStart.setOnClickListener(v -> { haptic(); startTyping(); });
        btnStop.setOnClickListener(v -> { haptic(); stopTyping(); });

        // Support & contact buttons
        com.google.android.material.button.MaterialButton btnTiktok =
                view.findViewById(R.id.btnTiktok);
        com.google.android.material.button.MaterialButton btnDiscord =
                view.findViewById(R.id.btnDiscord);

        btnTiktok.setOnClickListener(v -> {
            haptic();
            openTiktok();
        });

        btnDiscord.setOnClickListener(v -> {
            haptic();
            openDiscord();
        });

        // Onboarding: only show on first run
        boolean seenOnboarding = PrefsHelper.getBoolean(requireContext(), "onboarding_seen", false);
        onboardingCard.setVisibility(seenOnboarding ? View.GONE : View.VISIBLE);
        btnGotIt.setOnClickListener(v -> {
            haptic();
            PrefsHelper.putBoolean(requireContext(), "onboarding_seen", true);
            onboardingCard.animate().alpha(0).setDuration(280).withEndAction(() ->
                    onboardingCard.setVisibility(View.GONE)).start();
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        updateAccessibilityStatus();
        // Register for status broadcasts
        IntentFilter f = new IntentFilter("com.ryan.autotyper.ACTION_STATUS");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireContext().registerReceiver(statusReceiver, f, Context.RECEIVER_NOT_EXPORTED);
        } else {
            requireContext().registerReceiver(statusReceiver, f);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        try { requireContext().unregisterReceiver(statusReceiver); } catch (Exception ignored) {}
    }

    /** Update the running status card. */
    private void updateStatusCard(int typed, int total, boolean running, boolean session) {
        if (statusCard == null) return;
        if (!session) {
            statusCard.setVisibility(View.GONE);
            return;
        }
        statusCard.setVisibility(View.VISIBLE);
        if (running) {
            statusTitle.setText("▶ ĐANG CHẠY");
            statusTitle.setTextColor(0xFF34D399);
        } else {
            statusTitle.setText("⏸ TẠM DỪNG");
            statusTitle.setTextColor(0xFFFCD34D);
        }
        statusProgress.setText(typed + " / " + total + " tin");
        int pct = total > 0 ? (int) (typed * 100L / total) : 0;
        statusMode.setText(pct + "% · LOOP tự động");
    }

    /** Light haptic tap. */
    private void haptic() {
        if (getView() == null) return;
        getView().performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY,
                android.view.HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING);
    }

    /** Open TikTok profile — try app first, fallback to browser. */
    private void openTiktok() {
        Intent appIntent = new Intent(Intent.ACTION_VIEW,
                android.net.Uri.parse("snssdk1233://user/profile/ryandz036"));
        appIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(appIntent);
            return;
        } catch (Exception ignored) { /* app not installed */ }
        // Fallback: open in browser
        Intent webIntent = new Intent(Intent.ACTION_VIEW,
                android.net.Uri.parse("https://www.tiktok.com/@ryandz036"));
        webIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(webIntent);
        } catch (Exception e) {
            if (getView() != null) Snack.show(getView(), "Không mở được TikTok");
        }
    }

    /** Open Discord — try app first, fallback to browser. */
    private void openDiscord() {
        // Discord doesn't support arbitrary username deep-link, so we copy to clipboard
        android.content.ClipboardManager cm = (android.content.ClipboardManager)
                requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        android.content.ClipData clip = android.content.ClipData.newPlainText("Discord", "fyzerx_");
        if (cm != null) cm.setPrimaryClip(clip);
        if (getView() != null) {
            Snack.show(getView(), "Đã copy \"fyzerx_\" — mở Discord và gửi lời mời kết bạn");
        }
        // Try to open Discord app
        try {
            Intent launchIntent = requireContext().getPackageManager()
                    .getLaunchIntentForPackage("com.discord");
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(launchIntent);
            }
        } catch (Exception ignored) { /* Discord not installed */ }
    }

    // ============================================================
    // Accessibility
    // ============================================================

    private void updateAccessibilityStatus() {
        boolean enabled = isAccessibilityServiceEnabled();
        if (enabled) {
            tvAccessibilityStatus.setText("✅  Đã bật Trợ năng (Accessibility)");
            tvAccessibilityStatus.setTextColor(0xFF34D399);
            tvAccessibilityStatus.setBackgroundResource(R.drawable.bg_status_ok);
        } else {
            tvAccessibilityStatus.setText("⚠️  Chưa bật Trợ năng (Accessibility)");
            tvAccessibilityStatus.setTextColor(0xFFFCD34D);
            tvAccessibilityStatus.setBackgroundResource(R.drawable.bg_status_warn);
        }
    }

    private boolean isAccessibilityServiceEnabled() {
        if (getContext() == null) return false;
        android.view.accessibility.AccessibilityManager am =
                (android.view.accessibility.AccessibilityManager)
                        requireContext().getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (am == null) return false;
        List<AccessibilityServiceInfo> enabledServices =
                am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK);
        String pkg = requireContext().getPackageName();
        for (AccessibilityServiceInfo info : enabledServices) {
            if (info.getId().contains(pkg)) return true;
        }
        return false;
    }

    // ============================================================
    // Start / Stop
    // ============================================================

    private void startTyping() {
        if (!isAccessibilityServiceEnabled()) {
            if (getView() != null) Snack.show(getView(), "❌ Hãy bật quyền Trợ năng trước!");
            return;
        }
        AppState s = AppState.get();
        if (!s.hasFile() || s.getLineCount() == 0) {
            if (getView() != null) Snack.show(getView(), "❌ Chưa có nội dung! Vào tab Files để chọn/tạo file.");
            return;
        }

        // Stop the "ready" pulse glow
        if (btnStart != null) btnStart.clearAnimation();

        btnStart.setEnabled(false);
        if (getView() != null) {
            Snack.show(getView(), "⏳ Đang chuẩn bị " + s.getLineCount() + " dòng…");
        }

        final List<String> srcLines = s.getLinesSnapshot();
        final int rows = s.getRows();
        final int delayMs = s.getDelayMs();
        final String suffixName = s.getSuffixName();
        final boolean pasteMode = s.getTypingMode() == AppState.MODE_PASTE_LINE;
        final int charDelayMs = 50;

        ioExecutor.execute(() -> {
            final List<String> messages = new ArrayList<>((srcLines.size() / rows) + 1);
            try {
                for (int i = 0; i < srcLines.size(); i += rows) {
                    StringBuilder msg = new StringBuilder();
                    for (int j = 0; j < rows && (i + j) < srcLines.size(); j++) {
                        if (j > 0) msg.append('\n');
                        msg.append(srcLines.get(i + j));
                    }
                    if (!suffixName.isEmpty()) {
                        msg.append(' ').append(suffixName);
                    }
                    messages.add(msg.toString());
                }
            } catch (OutOfMemoryError oom) {
                mainHandler.post(() -> {
                    btnStart.setEnabled(true);
                    if (getView() != null) Snack.show(getView(), "❌ Hết bộ nhớ khi chuẩn bị tin");
                });
                return;
            }

            mainHandler.post(() -> {
                btnStart.setEnabled(true);
                AutoTyperService.setPendingMessages(messages, delayMs, charDelayMs, pasteMode);

                Intent intent = new Intent("com.ryan.autotyper.ACTION_START");
                intent.putExtra("use_pending", true);
                intent.setPackage(requireContext().getPackageName());
                requireContext().sendBroadcast(intent);

                // Show status card immediately with 0 progress
                updateStatusCard(0, messages.size(), true, true);

                String modeLabel = pasteMode ? "Dán từng dòng" : "Gõ từng chữ";
                if (getView() != null) {
                    Snack.show(getView(), "🚀 Đã START (" + modeLabel + ") — " + messages.size() + " tin · LOOP");
                }
            });
        });
    }

    private void stopTyping() {
        Intent intent = new Intent("com.ryan.autotyper.ACTION_STOP");
        intent.setPackage(requireContext().getPackageName());
        requireContext().sendBroadcast(intent);
        if (getView() != null) Snack.show(getView(), "⏹️ Đã STOP — tắt hết (kể cả phím volume)");
        // Hide status card
        if (statusCard != null) statusCard.setVisibility(View.GONE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ioExecutor.shutdownNow();
    }
}
