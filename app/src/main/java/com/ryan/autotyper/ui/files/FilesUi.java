package com.ryan.autotyper.ui.files;

import android.animation.ValueAnimator;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.ryan.autotyper.R;

/**
 * Small util bundle for the Files tab:
 *   • animateCount — count a TextView from 0 to target with overshoot
 *   • popIn        — apply the pop_in animation to a View
 */
final class FilesUi {

    private FilesUi() { /* no instances */ }

    static void animateCount(@NonNull TextView tv, int target) {
        if (target <= 0) {
            tv.setText("0");
            return;
        }
        // Short duration for small numbers, longer for big ones
        int duration = Math.min(800, 200 + target / 10);
        ValueAnimator anim = ValueAnimator.ofInt(0, target);
        anim.setDuration(duration);
        anim.addUpdateListener(a -> tv.setText(String.valueOf((int) a.getAnimatedValue())));
        anim.start();
    }

    static void popIn(@NonNull View v) {
        Animation a = AnimationUtils.loadAnimation(v.getContext(), R.anim.pop_in);
        v.startAnimation(a);
    }
}
