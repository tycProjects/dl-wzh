package com.bubbleai.app;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Menampilkan bubble AI lewat overlay aksesibilitas (TYPE_ACCESSIBILITY_OVERLAY),
 * sehingga tidak butuh izin "Tampil di atas aplikasi lain".
 * Layanan ini TIDAK membaca isi layar dan tidak mendengarkan event apa pun.
 */
public class BubbleAccessibilityService extends AccessibilityService {

    public static volatile BubbleAccessibilityService instance;

    private final Handler main = new Handler(Looper.getMainLooper());
    private WindowManager wm;
    private Prefs prefs;
    private ApiClient api;

    private View bubbleView;
    private WindowManager.LayoutParams bubbleParams;

    private ChatRootLayout chatView;
    private LinearLayout container;
    private ScrollView scroll;
    private TextView tvEmpty;
    private EditText etMessage;
    private TextView typingView;
    private boolean busy = false;

    // ---------------- lifecycle ----------------

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        prefs = new Prefs(this);
        api = new ApiClient();
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        if (prefs.isBubbleOn()) showBubble();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) { }

    @Override
    public void onInterrupt() { }

    @Override
    public boolean onUnbind(Intent intent) {
        removeAll();
        instance = null;
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        removeAll();
        instance = null;
        super.onDestroy();
    }

    private void removeAll() {
        hideBubble();
    }

    private int dp(float v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    // ---------------- bubble (tidak bisa digeser, hanya toggle) ----------------

    public void showBubble() {
        if (wm == null) return;
        if (bubbleView != null) {
            refreshPosition();
            return;
        }
        bubbleView = LayoutInflater.from(this).inflate(R.layout.layout_bubble, null);
        bubbleParams = new WindowManager.LayoutParams(
                dp(56), dp(56),
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);
        applyBubblePosition();
        bubbleView.setOnClickListener(v -> toggleChat());
        try {
            wm.addView(bubbleView, bubbleParams);
        } catch (Exception e) {
            bubbleView = null;
        }
    }

    private void applyBubblePosition() {
        DisplayMetrics dm = getResources().getDisplayMetrics();
        boolean left = "left".equals(prefs.getSide());
        bubbleParams.gravity = Gravity.TOP | (left ? Gravity.START : Gravity.END);
        bubbleParams.x = dp(6);
        bubbleParams.y = (int) (dm.heightPixels * 0.58f);
    }

    public void refreshPosition() {
        if (bubbleView == null || wm == null) return;
        applyBubblePosition();
        try {
            wm.updateViewLayout(bubbleView, bubbleParams);
        } catch (Exception ignored) { }
    }

    public void hideBubble() {
        closeChat();
        if (bubbleView != null && wm != null) {
            try {
                wm.removeView(bubbleView);
            } catch (Exception ignored) { }
        }
        bubbleView = null;
    }

    // ---------------- chat window ----------------

    private void toggleChat() {
        if (chatView != null) closeChat();
        else openChat();
    }

    private void openChat() {
        if (chatView != null || wm == null) return;

        chatView = (ChatRootLayout) LayoutInflater.from(this).inflate(R.layout.layout_chat_window, null);
        container = chatView.findViewById(R.id.messagesContainer);
        scroll = chatView.findViewById(R.id.scrollMessages);
        tvEmpty = chatView.findViewById(R.id.tvEmpty);
        etMessage = chatView.findViewById(R.id.etMessage);
        TextView btnSend = chatView.findViewById(R.id.btnSend);
        TextView btnClose = chatView.findViewById(R.id.btnCloseChat);
        TextView btnClear = chatView.findViewById(R.id.btnClearChat);
        TextView tvModel = chatView.findViewById(R.id.tvChatModel);
        tvModel.setText(prefs.getModel());

        chatView.setBackListener(this::closeChat);
        btnClose.setOnClickListener(v -> closeChat());
        btnSend.setOnClickListener(v -> doSend());
        btnClear.setOnClickListener(v -> {
            if (busy) return;
            prefs.clearHistory();
            container.removeAllViews();
            typingView = null;
            updateEmpty();
        });
        etMessage.setOnClickListener(v -> showKeyboard());

        // tampilkan riwayat
        JSONArray h = prefs.getHistory();
        for (int i = 0; i < h.length(); i++) {
            JSONObject m = h.optJSONObject(i);
            if (m == null) continue;
            addMessage(m.optString("content"), "user".equals(m.optString("role")));
        }
        updateEmpty();

        DisplayMetrics dm = getResources().getDisplayMetrics();
        int w = Math.min(dp(340), dm.widthPixels - dp(24));
        int h2 = Math.max(dp(220), Math.min(dp(460), (int) (dm.heightPixels * 0.5f)));

        // Jendela FOKUS (tanpa FLAG_NOT_FOCUSABLE) agar keyboard bisa muncul.
        WindowManager.LayoutParams p = new WindowManager.LayoutParams(
                w, h2,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);
        p.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        p.y = dp(44);
        p.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                | WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE;

        try {
            wm.addView(chatView, p);
        } catch (Exception e) {
            chatView = null;
            return;
        }

        scrollToBottom();
        etMessage.requestFocus();
        main.postDelayed(this::showKeyboard, 250);
    }

    private void showKeyboard() {
        if (chatView == null || etMessage == null) return;
        etMessage.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) imm.showSoftInput(etMessage, InputMethodManager.SHOW_IMPLICIT);
    }

    private void closeChat() {
        if (chatView == null) return;
        try {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) imm.hideSoftInputFromWindow(chatView.getWindowToken(), 0);
        } catch (Exception ignored) { }
        try {
            wm.removeView(chatView);
        } catch (Exception ignored) { }
        chatView = null;
        container = null;
        scroll = null;
        etMessage = null;
        typingView = null;
    }

    // ---------------- kirim pesan ----------------

    private void doSend() {
        if (busy || etMessage == null) return;
        String text = etMessage.getText().toString().trim();
        if (text.isEmpty()) return;
        etMessage.setText("");

        addMessage(text, true);
        updateEmpty();

        String key = prefs.getApiKey();
        if (key.isEmpty()) {
            addMessage("API key belum diisi. Buka app Bubble AI → Pengaturan, isi API key, lalu simpan.", false);
            return;
        }

        prefs.incSent();
        final JSONArray history = prefs.getHistory();
        history.put(Prefs.msg("user", text));

        busy = true;
        typingView = addMessage("Mengetik…", false);

        api.sendMessage(prefs.getApiUrl(), key, prefs.getModel(), prefs.getSystem(), history,
                new ApiClient.ResultCallback() {
                    @Override
                    public void onSuccess(String reply) {
                        busy = false;
                        removeTyping();
                        addMessage(reply, false);
                        history.put(Prefs.msg("assistant", reply));
                        prefs.saveHistory(history);
                        prefs.incRecv();
                    }

                    @Override
                    public void onError(String message) {
                        busy = false;
                        removeTyping();
                        addMessage("⚠ " + message, false);
                        prefs.incErr();
                    }
                });
    }

    private TextView addMessage(String text, boolean isUser) {
        if (chatView == null || container == null) return null;

        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(13.5f);
        tv.setPadding(dp(12), dp(8), dp(12), dp(8));
        tv.setMaxWidth(dp(250));
        tv.setBackgroundResource(isUser ? R.drawable.bg_msg_user : R.drawable.bg_msg_ai);
        tv.setTextIsSelectable(true);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(dp(4), dp(4), dp(4), dp(4));
        lp.gravity = isUser ? Gravity.END : Gravity.START;
        container.addView(tv, lp);

        updateEmpty();
        scrollToBottom();
        return tv;
    }

    private void removeTyping() {
        if (container != null && typingView != null) container.removeView(typingView);
        typingView = null;
        updateEmpty();
    }

    private void updateEmpty() {
        if (tvEmpty != null && container != null) {
            tvEmpty.setVisibility(container.getChildCount() == 0 ? View.VISIBLE : View.GONE);
        }
    }

    private void scrollToBottom() {
        if (scroll == null) return;
        final ScrollView s = scroll;
        s.post(() -> s.fullScroll(View.FOCUS_DOWN));
    }
}
