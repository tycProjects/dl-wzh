package com.ryan.autotyper.ui;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;

import androidx.annotation.NonNull;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;

import com.ryan.autotyper.R;

/**
 * AnimKit — small util for app-wide animation patterns.
 *
 *   • staggeredEntrance — animate each direct child of a ViewGroup with a
 *     pop_in-style scale+alpha animator, each delayed by `delayStep` ms.
 *     Uses ObjectAnimator (hardware-accelerated) instead of loadAnimation
 *     for smoother playback on older devices, and a shorter default step
 *     (45ms) so the whole sequence finishes well under 500ms.
 *
 *   • pressScale — small spring press-down on tap, used on cards/buttons.
 *
 *   • fadeInFromBottom — lightweight single-shot anim for tab re-shows.
 */
public final class AnimKit {

    private AnimKit() { /* no instances */ }

    /**
     * Animate each direct child of the root with a staggered scale + alpha
     * animator. Uses ObjectAnimator (hardware-accelerated) so the UI thread
     * is freed up as much as possible during the entrance.
     *
     * Children with visibility GONE are skipped (their layout box is gone
     * anyway, animating them would just waste cycles).
     */
    public static void staggeredEntrance(@NonNull View root, long delayStep) {
        if (!(root instanceof ViewGroup)) return;
        ViewGroup vg = (ViewGroup) root;
        int n = vg.getChildCount();
        long now = 0;
        for (int i = 0; i < n; i++) {
            View child = vg.getChildAt(i);
            if (child == null || child.getVisibility() == View.GONE) continue;
            // Hardware layer for the duration of the entrance — pays off
            // because each child animates alpha+scale together.
            child.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            ObjectAnimator sx = ObjectAnimator.ofFloat(child, View.SCALE_X, 0.94f, 1f);
            ObjectAnimator sy = ObjectAnimator.ofFloat(child, View.SCALE_Y, 0.94f, 1f);
            ObjectAnimator ty = ObjectAnimator.ofFloat(child, View.TRANSLATION_Y, 14f, 0f);
            ObjectAnimator a  = ObjectAnimator.ofFloat(child, View.ALPHA, 0f, 1f);
            AnimatorSet set = new AnimatorSet();
            set.playTogether(sx, sy, ty, a);
            set.setStartDelay(now);
            set.setDuration(280);
            set.setInterpolator(new FastOutSlowInInterpolator());
            set.addListener(new android.animation.AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(android.animation.Animator animation) {
                    child.setLayerType(View.LAYER_TYPE_NONE, null);
                }
            });
            set.start();
            now += delayStep;
        }
    }

    /** Convenience: stagger with default 45ms step (snappy). */
    public static void staggeredEntrance(@NonNull View root) {
        staggeredEntrance(root, 45L);
    }

    /** Spring press-down on tap (call in onClick). Returns immediately. */
    public static void pressScale(@NonNull View v) {
        v.setPivotX(v.getWidth() / 2f);
        v.setPivotY(v.getHeight() / 2f);
        ObjectAnimator down = ObjectAnimator.ofFloat(v, View.SCALE_X, 1f, 0.94f);
        ObjectAnimator downY = ObjectAnimator.ofFloat(v, View.SCALE_Y, 1f, 0.94f);
        AnimatorSet setDown = new AnimatorSet();
        setDown.playTogether(down, downY);
        setDown.setDuration(80);
        setDown.setInterpolator(new FastOutSlowInInterpolator());

        ObjectAnimator up = ObjectAnimator.ofFloat(v, View.SCALE_X, 0.94f, 1f);
        ObjectAnimator upY = ObjectAnimator.ofFloat(v, View.SCALE_Y, 0.94f, 1f);
        AnimatorSet setUp = new AnimatorSet();
        setUp.playTogether(up, upY);
        setUp.setDuration(220);
        setUp.setInterpolator(new OvershootInterpolator(2.5f));

        AnimatorSet all = new AnimatorSet();
        all.playSequentially(setDown, setUp);
        all.start();
    }

    /** Lightweight fade-in + slight Y translation for tab re-show. */
    public static void fadeInFromBottom(@NonNull View v) {
        v.setAlpha(0f);
        v.setTranslationY(10f);
        v.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(220)
                .setInterpolator(new FastOutSlowInInterpolator())
                .withLayer()
                .start();
    }
}
