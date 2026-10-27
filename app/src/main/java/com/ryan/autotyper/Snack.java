package com.ryan.autotyper;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.snackbar.Snackbar;

/**
 * Snack — thin wrapper around Material 3 Snackbar.
 *
 * Replaces all Toast calls because:
 *   • Toasts float over the whole system (cover other apps when our app is in foreground)
 *   • Snackbars dock inside our CoordinatorLayout → don't cover anything else
 *   • Material 3 styled → matches the rest of the app
 *
 * Always uses LENGTH_SHORT and the brand violet as the action color.
 */
public final class Snack {

    private Snack() { /* no instances */ }

    /** Show a Snackbar anchored to the given view's root. */
    public static void show(@NonNull View anchor, @NonNull String message) {
        View root = anchor.getRootView() != null ? anchor.getRootView() : anchor;
        // Try to find a CoordinatorLayout in the hierarchy for best Snackbar behavior;
        // falling back to anchor itself if none.
        Snackbar.make(anchor, message, Snackbar.LENGTH_SHORT)
                .setAnchorView(null)
                .show();
    }

    /** Show a Snackbar with an action button. */
    public static void show(@NonNull View anchor, @NonNull String message,
                            @Nullable String actionLabel, @Nullable View.OnClickListener action) {
        Snackbar s = Snackbar.make(anchor, message, Snackbar.LENGTH_SHORT);
        if (actionLabel != null && action != null) {
            s.setAction(actionLabel, action);
            s.setActionTextColor(anchor.getContext().getResources().getColor(R.color.brand_violet));
        }
        s.show();
    }
}
