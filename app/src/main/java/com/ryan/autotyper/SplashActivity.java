package com.ryan.autotyper;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/**
 * SplashActivity v3.5 — modern loading splash.
 *
 * Loading effects:
 *   • Avatar: glow pulse + rotating color ring + sweep progress ring (rotates 360°/1.2s)
 *   • Title: shimmer (alpha 0.6 ⇄ 1.0)
 *   • 3 dots: wave pulse (each dot scales 0.6→1.3 + alpha 0.35→1, staggered 150ms)
 *   • Whole avatar block pops in on start
 *
 * Total splash duration: 1.6s, then fade-out into MainActivity.
 */
public class SplashActivity extends AppCompatActivity {

    private static final long DURATION_MS = 1600L;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PrefsHelper.applySavedTheme(this);
        setContentView(R.layout.activity_splash);

        View container       = findViewById(R.id.splashAvatarContainer);
        View glow            = findViewById(R.id.splashAvatarGlow);
        ImageView ring       = findViewById(R.id.splashAvatarRing);
        ImageView progress   = findViewById(R.id.splashProgressRing);
        TextView title       = findViewById(R.id.splashTitle);
        View dot1            = findViewById(R.id.dot1);
        View dot2            = findViewById(R.id.dot2);
        View dot3            = findViewById(R.id.dot3);

        // 1. Pop-in the whole logo block (scale 0.6→1 + fade)
        container.startAnimation(AnimationUtils.loadAnimation(this, R.anim.splash_logo));

        // 2. Glow pulse
        glow.startAnimation(AnimationUtils.loadAnimation(this, R.anim.pulse_avatar_glow));

        // 3. Sweep progress ring rotation (1.2s/rev — fast)
        if (progress != null) {
            progress.startAnimation(AnimationUtils.loadAnimation(this, R.anim.rotate_progress));
        }

        // 4. Color ring rotation (8s/rev)
        ring.startAnimation(AnimationUtils.loadAnimation(this, R.anim.rotate_avatar));

        // 5. Title shimmer
        title.startAnimation(AnimationUtils.loadAnimation(this, R.anim.shimmer_title));

        // 6. Loading dots wave — each dot uses the same anim but with staggered startOffset
        startDotWave(dot1, 0);
        startDotWave(dot2, 150);
        startDotWave(dot3, 300);

        // Schedule transition to MainActivity
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            View root = findViewById(android.R.id.content);
            Animation fadeOut = AnimationUtils.loadAnimation(this, R.anim.splash_fade_out);
            fadeOut.setAnimationListener(new Animation.AnimationListener() {
                @Override public void onAnimationStart(Animation a) {}
                @Override public void onAnimationRepeat(Animation a) {}
                @Override public void onAnimationEnd(Animation a) {
                    startActivity(new Intent(SplashActivity.this, MainActivity.class));
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                    finish();
                }
            });
            root.startAnimation(fadeOut);
        }, DURATION_MS);
    }

    private void startDotWave(View dot, long startOffsetMs) {
        Animation a = AnimationUtils.loadAnimation(this, R.anim.loading_dot);
        a.setStartOffset(startOffsetMs);
        dot.startAnimation(a);
    }
}
