package com.venice.joymod;

import android.accessibilityservice.AccessibilityService;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

public class ScreenReader extends AccessibilityService {

    private static ScreenReader inst;

    private String lastText = "";
    private String foreground = "";
    private long lastEvent = 0;

    public static ScreenReader get() { return inst; }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        inst = this;
    }

    @Override
    public void onDestroy() {
        if (inst == this) inst = null;
        super.onDestroy();
    }

    @Override
    public void onInterrupt() { }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent e) {
        if (e == null) return;
        CharSequence pkg = e.getPackageName();
        if (pkg != null && pkg.length() > 0) {
            foreground = pkg.toString();
            lastEvent = SystemClock.elapsedRealtime();
        }
        if (QueueEngine.PKG.equals(foreground)) {
            AccessibilityNodeInfo root = null;
            try {
                root = getRootInActiveWindow();
                if (root != null) lastText = dump(root, 0, new StringBuilder()).toString();
            } catch (Throwable t) {
                // ignora
            } finally {
                if (root != null) root.recycle();
            }
        }
    }

    private StringBuilder dump(AccessibilityNodeInfo n, int depth, StringBuilder sb) {
        if (n == null || depth > 28 || sb.length() > 6000) return sb;
        CharSequence t = n.getText();
        if (t != null && t.length() > 0) sb.append(t.toString().trim()).append("\n");
        CharSequence d = n.getContentDescription();
        if (d != null && d.length() > 0) sb.append(d.toString().trim()).append("\n");
        for (int i = 0; i < n.getChildCount(); i++) {
            AccessibilityNodeInfo c = null;
            try {
                c = n.getChild(i);
                if (c != null) dump(c, depth + 1, sb);
            } catch (Throwable t2) {
                // ignora
            } finally {
                if (c != null) c.recycle();
            }
        }
        return sb;
    }

    public void refresh() {
        try {
            AccessibilityNodeInfo root = getRootInActiveWindow();
            if (root != null) {
                lastText = dump(root, 0, new StringBuilder()).toString();
                foreground = root.getPackageName() == null ? foreground : root.getPackageName().toString();
                root.recycle();
            }
        } catch (Throwable t) {
            // ignora
        }
    }

    public void goHome() {
        performGlobalAction(GLOBAL_ACTION_HOME);
    }

    public void goBack() {
        performGlobalAction(GLOBAL_ACTION_BACK);
    }

    public String getText() { return lastText == null ? "" : lastText; }

    public String getForeground() { return foreground == null ? "" : foreground; }

    public long msSinceEvent() { return SystemClock.elapsedRealtime() - lastEvent; }
}
