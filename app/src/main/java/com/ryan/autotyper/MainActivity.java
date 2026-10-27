package com.ryan.autotyper;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.ryan.autotyper.ui.config.ConfigFragment;
import com.ryan.autotyper.ui.files.FilesFragment;
import com.ryan.autotyper.ui.home.HomeFragment;

/**
 * MainActivity v3.9 — tabbed host with:
 *   • Top brand bar + top-right theme toggle button
 *   • Bottom nav with active pill indicator
 *   • Fragment caching: each tab's Fragment is created once and reused
 *     via show/hide, so tab switches are instant and state is preserved
 *     (no re-inflation, no re-fetching from prefs, no re-running entrance
 *     animations). This is the single biggest smoothness win.
 *   • Custom fragment enter animation only when switching to a previously
 *     hidden tab (lightweight alpha+translate, no full recreate).
 *   • Bounce animation on the active nav item icon.
 *
 * Shared state lives in {@link AppState}; theme mode persists in
 * {@link PrefsHelper}.
 */
public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_PERMISSION = 1002;

    private BottomNavigationView bottomNav;
    private ImageButton btnThemeToggle;

    /** Cached tab fragments — created lazily on first visit, reused after. */
    private Fragment homeFragment, filesFragment, configFragment;
    private int currentTabId = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Restore persisted config + apply saved theme before UI inflates.
        PrefsHelper.applySavedTheme(this);
        PrefsHelper.restoreConfig(this, AppState.get());

        setContentView(R.layout.activity_main);

        bottomNav = findViewById(R.id.bottomNav);
        btnThemeToggle = findViewById(R.id.btnThemeToggle);

        // Nav listener — show/hide cached fragments (no re-inflation).
        bottomNav.setOnItemSelectedListener(item -> {
            final int id = item.getItemId();
            if (id == currentTabId) return true; // already selected, no-op
            Fragment target;
            if (id == R.id.nav_files) {
                if (filesFragment == null) filesFragment = new FilesFragment();
                target = filesFragment;
            } else if (id == R.id.nav_config) {
                if (configFragment == null) configFragment = new ConfigFragment();
                target = configFragment;
            } else {
                if (homeFragment == null) homeFragment = new HomeFragment();
                target = homeFragment;
            }
            switchFragmentCached(id, target);
            return true;
        });

        // Theme toggle: cycle System → Dark → Light → System
        btnThemeToggle.setOnClickListener(v -> {
            int cur = PrefsHelper.getThemeMode(this);
            int next;
            if (cur == PrefsHelper.THEME_SYSTEM) {
                next = PrefsHelper.THEME_DARK;
            } else if (cur == PrefsHelper.THEME_DARK) {
                next = PrefsHelper.THEME_LIGHT;
            } else {
                next = PrefsHelper.THEME_SYSTEM;
            }
            PrefsHelper.setThemeMode(this, next);
            // recreate so palette applies
            recreate();
        });

        // Default tab = Home
        if (savedInstanceState == null) {
            // Defer initial selection so onCreate returns fast and the splash
            // exit animation isn't fighting with fragment initialisation.
            bottomNav.post(() -> bottomNav.setSelectedItemId(R.id.nav_home));
        }

        checkPermissions();
    }

    /**
     * Show the target fragment, hide all others. Adds the fragment to the
     * container only the first time it's needed (lazy). Uses commitAllowingStateLoss
     * for safety, and a lightweight alpha+translate animation on the newly-shown
     * fragment so tab switches feel responsive without re-running heavy entrance
     * animations on every existing child view.
     */
    private void switchFragmentCached(int newTabId, @NonNull Fragment target) {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction tx = fm.beginTransaction();

        // Hide whichever is currently shown.
        if (homeFragment != null && homeFragment.isVisible()) tx.hide(homeFragment);
        if (filesFragment != null && filesFragment.isVisible()) tx.hide(filesFragment);
        if (configFragment != null && configFragment.isVisible()) tx.hide(configFragment);

        if (!target.isAdded()) {
            tx.add(R.id.fragmentContainer, target, tagFor(newTabId));
        } else {
            tx.show(target);
        }
        tx.commitAllowingStateLoss();

        // Lightweight "settle" animation on the newly-shown fragment's view.
        // Skipped the very first time (let the fragment run its own entrance
        // staggeredEntrance in onViewCreated).
        View view = target.getView();
        if (view != null) {
            // view is non-null only if fragment was already created — animate
            view.clearAnimation();
            view.setAlpha(0f);
            view.setTranslationY(8f);
            view.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(220)
                    .withLayer() // hardware layer for the duration of the anim
                    .start();
        }

        currentTabId = newTabId;

        // Bounce the active nav icon
        View activeItem = bottomNav.findViewById(newTabId);
        if (activeItem != null) {
            View icon = activeItem.findViewById(
                    com.google.android.material.R.id.navigation_bar_item_icon_view);
            if (icon != null) {
                Animation bounce = AnimationUtils.loadAnimation(this, R.anim.nav_bounce);
                icon.startAnimation(bounce);
            }
        }
    }

    @Nullable
    private static String tagFor(int tabId) {
        if (tabId == R.id.nav_files) return "tab.files";
        if (tabId == R.id.nav_config) return "tab.config";
        if (tabId == R.id.nav_home) return "tab.home";
        return null;
    }

    @Override
    protected void onResume() {
        super.onResume();
        AppState.get().setAppInForeground(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        AppState.get().setAppInForeground(false);
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        REQUEST_PERMISSION);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // No-op: SAF (ACTION_OPEN_DOCUMENT) doesn't need storage permission on API 19+.
    }
}
