package com.ryan.autotyper;

import android.accessibilityservice.AccessibilityService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.core.app.NotificationCompat;

import java.util.ArrayList;
import java.util.List;

public class AutoTyperService extends AccessibilityService {

    private static final String TAG = "YeuEmMoiVuTru";
    private static final int NOTIF_ID = 7711;
    private static final String CHANNEL_ID = "yemvt_running";

    public static final String ACTION_PAUSE = "com.ryan.autotyper.ACTION_PAUSE";
    public static final String ACTION_RESUME = "com.ryan.autotyper.ACTION_RESUME";

    private BroadcastReceiver receiver;
    private final Handler handler = new Handler(Looper.getMainLooper());

    // ========== Static holder — tránh TransactionTooLarge với file lớn ==========
    private static List<String> pendingMessages = null;
    private static int pendingDelayMs = 1000;
    private static int pendingCharDelayMs = 50;
    private static boolean pendingPasteMode = false;

    /** Gọi từ MainActivity trước khi broadcast ACTION_START */
    public static void setPendingMessages(List<String> messages, int delayMs, int charDelayMs, boolean pasteMode) {
        pendingMessages = messages;
        pendingDelayMs = delayMs;
        pendingCharDelayMs = charDelayMs;
        pendingPasteMode = pasteMode;
    }

    // Trạng thái
    private boolean sessionActive = false;
    private boolean running = false;
    private int totalTyped = 0;        // cumulative count of typed messages this session

    private List<String> messages = null;
    private int currentIndex = 0;
    private int delayMs = 0;
    private int charDelayMs = 50;
    private boolean pasteMode = false;

    // Dùng khi gõ từng chữ
    private String currentMessage = "";
    private int charIndex = 0;
    private final StringBuilder typedSoFar = new StringBuilder();

    // Lưu config để loop lại
    private List<String> lastMessages = null;
    private int lastDelayMs = 0;
    private int lastCharDelayMs = 50;
    private boolean lastPasteMode = false;

    // Đánh dấu đã hiện toast chờ bàn phím (tránh spam)
    private boolean waitingKeyboardToastShown = false;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();

        IntentFilter filter = new IntentFilter();
        filter.addAction("com.ryan.autotyper.ACTION_START");
        filter.addAction("com.ryan.autotyper.ACTION_STOP");
        filter.addAction(ACTION_PAUSE);
        filter.addAction(ACTION_RESUME);

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if ("com.ryan.autotyper.ACTION_START".equals(action)) {
                    // Ưu tiên data từ static holder (file lớn)
                    if (pendingMessages != null && !pendingMessages.isEmpty()) {
                        List<String> msgs = pendingMessages;
                        int delay = pendingDelayMs;
                        int charDelay = pendingCharDelayMs;
                        boolean paste = pendingPasteMode;
                        // Clear pending ngay để tránh giữ reference thừa
                        pendingMessages = null;

                        lastMessages = msgs;
                        lastDelayMs = delay;
                        lastCharDelayMs = charDelay;
                        lastPasteMode = paste;

                        totalTyped = 0;
                        sessionActive = true;
                        startTyping(msgs, delay, charDelay, paste);
                    }
                } else if ("com.ryan.autotyper.ACTION_STOP".equals(action)) {
                    fullStop();
                } else if (ACTION_PAUSE.equals(action)) {
                    if (running) {
                        pauseTyping();
                        updateNotification("⏸ Tạm dừng", totalTyped + "/" + (messages != null ? messages.size() : 0) + " tin");
                    }
                } else if (ACTION_RESUME.equals(action)) {
                    if (!running && sessionActive) {
                        running = true;
                        handler.post(typingRunnable);
                        updateNotification("▶ Đang chạy", totalTyped + "/" + (messages != null ? messages.size() : 0) + " tin");
                    }
                }
            }
        };

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(receiver, filter);
        }
        Log.d(TAG, "Service created");
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID, "Auto Typer đang chạy", NotificationManager.IMPORTANCE_LOW);
            ch.setDescription("Hiển thị khi app đang tự động gõ");
            ch.setShowBadge(false);
            ch.enableVibration(false);
            ch.enableLights(false);
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(ch);
        }
    }

    /** Build the foreground notification. */
    private Notification buildNotification(String title, String content) {
        // Tap → open MainActivity
        Intent open = new Intent(this, MainActivity.class);
        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent openPi = PendingIntent.getActivity(this, 0, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Pause action
        Intent pauseI = new Intent(ACTION_PAUSE).setPackage(getPackageName());
        PendingIntent pausePi = PendingIntent.getBroadcast(this, 1, pauseI,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Resume action
        Intent resumeI = new Intent(ACTION_RESUME).setPackage(getPackageName());
        PendingIntent resumePi = PendingIntent.getBroadcast(this, 2, resumeI,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Stop action
        Intent stopI = new Intent("com.ryan.autotyper.ACTION_STOP").setPackage(getPackageName());
        PendingIntent stopPi = PendingIntent.getBroadcast(this, 3, stopI,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        int total = (messages != null) ? messages.size() : 0;
        int typed = totalTyped;
        int pct = total > 0 ? (int) (typed * 100L / total) : 0;
        // In loop mode the count keeps growing past total; show progress within current round
        int roundTyped = total > 0 ? (typed % total) : 0;
        int roundPct = total > 0 ? (int) (roundTyped * 100L / total) : 0;

        NotificationCompat.Builder b = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_play)
                .setContentTitle(title)
                .setContentText(content)
                .setSubText("LOOP · " + total + " tin")
                .setContentIntent(openPi)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setShowWhen(false)
                .setColor(0xFF8B5CF6)
                .setColorized(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_PROGRESS);

        // Progress bar (determinate within current round; LOOP keeps it visible)
        if (total > 0) {
            b.setProgress(total, roundTyped, false);
        }

        // Custom expanded content view via RemoteViews would be ideal, but the
        // default NotificationCompat progress display is clean enough.

        // Show pause or resume depending on state
        if (running) {
            b.addAction(0, "⏸ Tạm dừng", pausePi);
        } else {
            b.addAction(0, "▶ Tiếp tục", resumePi);
        }
        b.addAction(0, "⏹ STOP", stopPi);

        return b.build();
    }

    private void startForegroundWithNotification(String title, String content) {
        startForeground(NOTIF_ID, buildNotification(title, content));
    }

    private void updateNotification(String title, String content) {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.notify(NOTIF_ID, buildNotification(title, content));
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Not used
    }

    @Override
    public void onInterrupt() {
        fullStop();
    }

    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        if (!sessionActive) {
            return super.onKeyEvent(event);
        }

        if (event.getAction() != KeyEvent.ACTION_DOWN) {
            return super.onKeyEvent(event);
        }

        int keyCode = event.getKeyCode();

        // Volume UP = tiếp tục / resume / loop
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            if (!running) {
                if (messages != null && currentIndex < messages.size()) {
                    running = true;
                    handler.post(typingRunnable);
                    updateNotification("▶ Đang chạy", totalTyped + "/" + messages.size() + " tin · LOOP");
                } else if (lastMessages != null && !lastMessages.isEmpty()) {
                    startTyping(lastMessages, lastDelayMs, lastCharDelayMs, lastPasteMode);
                }
            }
            return true;
        }

        // Volume DOWN = tạm dừng
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (running) {
                pauseTyping();
                updateNotification("⏸ Tạm dừng", totalTyped + "/" + (messages != null ? messages.size() : 0) + " tin");
            }
            return true;
        }

        return super.onKeyEvent(event);
    }

    private void showToast(final String msg) {
        // v3.5: Toasts removed entirely — they were too intrusive and Service-hosted
        // Snackbars aren't possible. We just log the message for debugging.
        Log.d(TAG, "Status: " + msg);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (receiver != null) {
            try {
                unregisterReceiver(receiver);
            } catch (Exception ignored) {}
        }
        fullStop();
        Log.d(TAG, "Service destroyed");
    }

    private void startTyping(List<String> msgs, int delay, int charDelay, boolean paste) {
        if (msgs == null || msgs.isEmpty()) {
            Log.e(TAG, "Empty messages");
            return;
        }

        handler.removeCallbacksAndMessages(null);

        messages = msgs;
        currentIndex = 0;
        delayMs = delay;
        charDelayMs = Math.max(0, charDelay);
        pasteMode = paste;
        currentMessage = "";
        charIndex = 0;
        typedSoFar.setLength(0);
        running = true;
        sessionActive = true;
        waitingKeyboardToastShown = false;

        Log.d(TAG, "Start typing " + messages.size() + " messages, msgDelay=" + delayMs
                + ", charDelay=" + charDelayMs + ", pasteMode=" + pasteMode);

        // Start foreground notification
        startForegroundWithNotification("▶ Đang chạy", "0/" + messages.size() + " tin · LOOP");

        handler.post(typingRunnable);
    }

    private void pauseTyping() {
        running = false;
        handler.removeCallbacksAndMessages(null);
        Log.d(TAG, "Paused");
    }

    private void fullStop() {
        running = false;
        sessionActive = false;
        handler.removeCallbacksAndMessages(null);
        messages = null;
        currentIndex = 0;
        currentMessage = "";
        charIndex = 0;
        typedSoFar.setLength(0);
        pasteMode = false;
        waitingKeyboardToastShown = false;
        totalTyped = 0;
        // Stop foreground + cancel notification
        stopForeground(true);
        // Không clear lastMessages — vẫn cho phép loop sau STOP nếu user muốn start lại
        Log.d(TAG, "Full stopped");
    }

    /** Update notification progress + broadcast for any listeners (HomeFragment status). */
    private void onTyped() {
        totalTyped++;
        int total = messages != null ? messages.size() : 0;
        updateNotification("▶ Đang chạy", totalTyped + "/" + total + " tin · LOOP");
        // Broadcast status
        Intent s = new Intent("com.ryan.autotyper.ACTION_STATUS");
        s.putExtra("typed", totalTyped);
        s.putExtra("total", total);
        s.putExtra("running", running);
        s.putExtra("session", sessionActive);
        s.setPackage(getPackageName());
        sendBroadcast(s);
    }

    private final Runnable typingRunnable = new Runnable() {
        @Override
        public void run() {
            if (!running || !sessionActive || messages == null) {
                return;
            }

            if (!isKeyboardOpen()) {
                if (!waitingKeyboardToastShown) {
                    showToast("⏳ Đang chờ mở bàn phím...");
                    waitingKeyboardToastShown = true;
                }
                Log.d(TAG, "Keyboard closed, waiting...");
                handler.postDelayed(this, 600);
                return;
            }
            waitingKeyboardToastShown = false;

            final int total = messages.size();

            // ========== CHẾ ĐỘ DÁN TỪNG DÒNG ==========
            if (pasteMode) {
                if (currentIndex >= total) {
                    // LOOP MODE mặc định: tự động chạy lại từ đầu
                    currentIndex = 0;
                    Log.d(TAG, "Loop restart (paste)");
                    showToast("🔄 Đã hết — tự động chạy lại từ đầu");
                }

                String msg = messages.get(currentIndex);
                currentIndex++;
                boolean success = setTextToFocused(msg);

                if (!success) {
                    currentIndex--;
                    handler.postDelayed(this, 800);
                    return;
                }

                handler.postDelayed(() -> {
                    if (!running || !sessionActive) return;
                    performSendAction();
                    onTyped();
                    if (running && sessionActive) {
                        // Loop mode: luôn chạy tiếp (sẽ tự loop ở đầu runnable nếu hết)
                        handler.postDelayed(typingRunnable, delayMs);
                    }
                }, 200);
                return;
            }

            // ========== CHẾ ĐỘ GÕ TỪNG CHỮ ==========
            if (currentMessage.isEmpty() || charIndex >= currentMessage.length()) {
                if (currentIndex >= total) {
                    // LOOP MODE mặc định: tự động chạy lại từ đầu
                    currentIndex = 0;
                    Log.d(TAG, "Loop restart (char)");
                    showToast("🔄 Đã hết — tự động chạy lại từ đầu");
                }
                currentMessage = messages.get(currentIndex);
                currentIndex++;
                charIndex = 0;
                typedSoFar.setLength(0);
            }

            if (charIndex < currentMessage.length()) {
                char c = currentMessage.charAt(charIndex);
                typedSoFar.append(c);
                charIndex++;
                boolean success = setTextToFocused(typedSoFar.toString());

                if (!success) {
                    charIndex--;
                    if (typedSoFar.length() > 0) {
                        typedSoFar.setLength(typedSoFar.length() - 1);
                    }
                    handler.postDelayed(this, 800);
                    return;
                }

                if (charIndex < currentMessage.length()) {
                    handler.postDelayed(this, charDelayMs);
                } else {
                    handler.postDelayed(() -> {
                        if (!running || !sessionActive) return;
                        performSendAction();
                        onTyped();
                        if (running && sessionActive) {
                            // Loop mode: luôn chạy tiếp (sẽ tự loop ở đầu runnable nếu hết)
                            currentMessage = "";
                            handler.postDelayed(typingRunnable, delayMs);
                        }
                    }, 200);
                }
            }
        }
    };

    private boolean isKeyboardOpen() {
        try {
            AccessibilityNodeInfo root = getRootInActiveWindow();
            if (root == null) return false;
            AccessibilityNodeInfo focused = findFocusedEditText(root);
            boolean open = focused != null;
            if (focused != null) focused.recycle();
            root.recycle();
            return open;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean setTextToFocused(String text) {
        try {
            AccessibilityNodeInfo root = getRootInActiveWindow();
            if (root == null) {
                Log.w(TAG, "No root window");
                return false;
            }

            AccessibilityNodeInfo focused = findFocusedEditText(root);
            if (focused != null) {
                Bundle args = new Bundle();
                args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text);
                boolean ok = focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args);
                focused.recycle();
                root.recycle();
                return ok;
            } else {
                Log.w(TAG, "No focused EditText found");
                root.recycle();
                return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting text", e);
            return false;
        }
    }

    private static final String OWN_PACKAGE = "com.ryan.autotyper";

    private AccessibilityNodeInfo findFocusedEditText(AccessibilityNodeInfo node) {
        if (node == null) return null;

        CharSequence pkg = node.getPackageName();
        if (pkg != null && OWN_PACKAGE.equals(pkg.toString())) {
            return null;
        }

        if (node.isFocused() && node.isEditable()) {
            return AccessibilityNodeInfo.obtain(node);
        }

        CharSequence className = node.getClassName();
        if (className != null && className.toString().contains("EditText") && node.isFocused()) {
            return AccessibilityNodeInfo.obtain(node);
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                AccessibilityNodeInfo result = findFocusedEditText(child);
                if (result != null) {
                    child.recycle();
                    return result;
                }
                child.recycle();
            }
        }
        return null;
    }

    /**
     * Find and click the "Send" button in the active chat app.
     *
     * Multi-strategy detection (v3.6):
     *   1. By resource ID — match known IDs from major chat apps:
     *      Zalo, Messenger, WhatsApp, Telegram, Messages, Instagram, KakaoTalk, Viber
     *   2. By content description — multi-language (EN/VI/FR/DE/RU/JA/KO/ZH/AR/ES/PT)
     *   3. By text — same multi-language keywords
     *   4. By class+position — ImageButton that is the last clickable child of the
     *      composer container (i.e. the rightmost icon button — usually the paper plane)
     *
     * Strategy order: ID → Desc → Text → Position fallback.
     */
    private void performSendAction() {
        try {
            AccessibilityNodeInfo root = getRootInActiveWindow();
            if (root == null) return;

            // === Strategy 1: by resource ID ===
            if (clickByResourceId(root, SEND_IDS)) {
                root.recycle();
                return;
            }
            // === Strategy 2: by content description (multi-language) ===
            if (clickByContentDescription(root, SEND_KEYWORDS)) {
                root.recycle();
                return;
            }
            // === Strategy 3: by text (multi-language) ===
            if (clickByText(root, SEND_KEYWORDS)) {
                root.recycle();
                return;
            }
            // === Strategy 4: positional fallback — find ImageButton in composer ===
            if (clickByPosition(root)) {
                root.recycle();
                return;
            }

            root.recycle();
            Log.d(TAG, "No send button found");
        } catch (Exception e) {
            Log.e(TAG, "Error sending", e);
        }
    }

    /** Known resource IDs of send buttons in popular chat apps. */
    private static final String[] SEND_IDS = {
            // Zalo
            "com.zing.zalo:id/send", "com.zing.zalo:id/btn_send", "com.zing.zalo:id/send_container",
            "com.zing.zalo:id/send_btn", "com.zing.zalo:id/iv_send",
            // Messenger
            "com.facebook.orca:id/send_button", "com.facebook.orca:id/composer_send_button",
            "com.facebook.orca:id/send_button_container",
            "com.facebook.katana:id/send_button",
            // WhatsApp
            "com.whatsapp:id/send", "com.whatsapp:id/send_btn", "com.whatsapp:id/sendbutton",
            "com.whatsapp.w4b:id/send", "com.whatsapp.w4b:id/send_btn",
            // Telegram
            "org.telegram.messenger:id/chat_input_send_button",
            "org.telegram.messenger:id/send_button",
            "org.telegram.plus:id/send_button",
            // Google Messages
            "com.google.android.apps.messaging:id/send_button",
            "com.google.android.apps.messaging:id/send_button_container",
            // Instagram DM
            "com.instagram.android:id/layout_message_send_button",
            "com.instagram.android:id/send_button",
            // KakaoTalk
            "com.kakao.talk:id/send", "com.kakao.talk:id/btn_send",
            // Viber
            "com.viber.voip:id/send_btn", "com.viber.voip:id/btn_send",
            // Snapchat, Discord, Line, WeChat
            "com.snapchat.android:id/send_button",
            "com.discord:id/chat_input_send_button",
            "jp.naver.line.android:id/send", "jp.naver.line.android:id/btn_send",
            "com.tencent.mm:id/send_btn", "com.tencent.mm:id/b4u",
            // Generic
            "send_button", "send_btn", "btn_send", "submit_button", "send", "composer_send_button"
    };

    /** Send keywords in many languages + arrow symbols. */
    private static final String[] SEND_KEYWORDS = {
            // English
            "send", "send message", "submit",
            // Vietnamese
            "gửi", "gửi tin nhắn",
            // French
            "envoyer", "envoi",
            // German
            "senden", "absenden",
            // Spanish / Portuguese
            "enviar", "envío", "envio",
            // Russian
            "отправить", "послать",
            // Japanese
            "送信", "送る",
            // Korean
            "전송", "보내기",
            // Chinese
            "发送", "送出",
            // Arabic
            "إرسال", "ارسال",
            // Italian
            "invia",
            // Symbols
            "➤", "→", "↗", "↪", "❯"
    };

    /** Strategy 1: search by resource ID. */
    private boolean clickByResourceId(AccessibilityNodeInfo root, String[] ids) {
        for (String id : ids) {
            List<AccessibilityNodeInfo> matches = root.findAccessibilityNodeInfosByViewId(id);
            if (matches != null && !matches.isEmpty()) {
                for (AccessibilityNodeInfo n : matches) {
                    if (clickNodeOrParent(n)) {
                        Log.d(TAG, "Send clicked by ID: " + id);
                        for (AccessibilityNodeInfo m : matches) m.recycle();
                        return true;
                    }
                }
                for (AccessibilityNodeInfo m : matches) m.recycle();
            }
        }
        return false;
    }

    /** Strategy 2: walk tree, match content description (case-insensitive). */
    private boolean clickByContentDescription(AccessibilityNodeInfo node, String[] keywords) {
        if (node == null) return false;
        CharSequence desc = node.getContentDescription();
        if (desc != null) {
            String d = desc.toString().toLowerCase();
            for (String kw : keywords) {
                if (d.contains(kw.toLowerCase())) {
                    if (clickNodeOrParent(node)) {
                        Log.d(TAG, "Send clicked by desc: " + kw);
                        return true;
                    }
                }
            }
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo c = node.getChild(i);
            if (c != null) {
                if (clickByContentDescription(c, keywords)) {
                    c.recycle();
                    return true;
                }
                c.recycle();
            }
        }
        return false;
    }

    /** Strategy 3: use findAccessibilityNodeInfosByText (system does case-insensitive contains). */
    private boolean clickByText(AccessibilityNodeInfo root, String[] keywords) {
        for (String kw : keywords) {
            List<AccessibilityNodeInfo> matches = root.findAccessibilityNodeInfosByText(kw);
            if (matches != null) {
                for (AccessibilityNodeInfo n : matches) {
                    if (clickNodeOrParent(n)) {
                        Log.d(TAG, "Send clicked by text: " + kw);
                        for (AccessibilityNodeInfo m : matches) m.recycle();
                        return true;
                    }
                }
                for (AccessibilityNodeInfo m : matches) m.recycle();
            }
        }
        return false;
    }

    /**
     * Strategy 4: positional fallback. Find the deepest container with a focused EditText
     * (the composer), then click its LAST clickable ImageButton child (the paper plane).
     * This catches apps where the send button is an icon-only ImageButton with no text/desc.
     */
    private boolean clickByPosition(AccessibilityNodeInfo root) {
        AccessibilityNodeInfo focused = findFocusedEditText(root);
        if (focused == null) return false;

        // Walk up to find the composer container (the focused EditText's grandparent)
        AccessibilityNodeInfo parent = focused.getParent();
        AccessibilityNodeInfo grand = (parent != null) ? parent.getParent() : null;
        AccessibilityNodeInfo composer = grand != null ? grand : parent;
        if (composer == null) {
            focused.recycle();
            return false;
        }

        // Find the LAST clickable ImageButton/Button in the composer (usually the rightmost send icon)
        AccessibilityNodeInfo[] lastBtn = new AccessibilityNodeInfo[1];
        findLastClickableButton(composer, lastBtn);
        if (lastBtn[0] != null) {
            boolean ok = clickNodeOrParent(lastBtn[0]);
            Log.d(TAG, "Send clicked by position (last button in composer): " + ok);
            lastBtn[0].recycle();
            composer.recycle();
            if (parent != null) parent.recycle();
            if (grand != null) grand.recycle();
            focused.recycle();
            return ok;
        }

        composer.recycle();
        if (parent != null) parent.recycle();
        if (grand != null) grand.recycle();
        focused.recycle();
        return false;
    }

    /** Find the last clickable Button/ImageButton in a subtree (DFS in document order). */
    private void findLastClickableButton(AccessibilityNodeInfo node, AccessibilityNodeInfo[] out) {
        if (node == null) return;
        String cls = node.getClassName() != null ? node.getClassName().toString() : "";
        boolean isButton = cls.contains("Button") || cls.contains("ImageButton") || cls.contains("ImageView");
        if (isButton && node.isClickable()) {
            if (out[0] != null) out[0].recycle();
            out[0] = AccessibilityNodeInfo.obtain(node);
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo c = node.getChild(i);
            if (c != null) {
                findLastClickableButton(c, out);
                c.recycle();
            }
        }
    }

    /** Click the node if clickable, otherwise walk up to find a clickable ancestor. */
    private boolean clickNodeOrParent(AccessibilityNodeInfo node) {
        AccessibilityNodeInfo cur = node;
        int hops = 0;
        while (cur != null && hops < 4) {
            if (cur.isClickable()) {
                cur.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                return true;
            }
            cur = cur.getParent();
            hops++;
        }
        return false;
    }
}
