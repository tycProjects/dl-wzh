package com.example.oi;

import android.accessibilityservice.AccessibilityService;
import android.graphics.PixelFormat;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.view.Gravity;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;
import java.util.Locale;

public class LiveReaderService extends AccessibilityService {
    private TextToSpeech tts;
    private WindowManager windowManager;
    private Button floatingButton;
    private boolean reading = false;
    private String lastSpoken = "";

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(new Locale("pt", "BR"));
            }
        });
        showFloatingButton();
    }

    private void showFloatingButton() {
        if (floatingButton != null || windowManager == null) return;
        floatingButton = new Button(this);
        floatingButton.setText("LER: DESLIGADO");
        floatingButton.setOnClickListener(v -> {
            reading = !reading;
            floatingButton.setText(reading ? "LER: LIGADO" : "LER: DESLIGADO");
        });

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP | Gravity.END;
        params.x = 20;
        params.y = 180;
        windowManager.addView(floatingButton, params);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (!reading || event == null) return;

        CharSequence packageName = event.getPackageName();
        if (packageName == null) return;
        String pkg = packageName.toString();
        boolean isTikTok = pkg.equals("com.zhiliaoapp.musically")
                || pkg.equals("com.ss.android.ugc.trill");
        if (!isTikTok) return;

        CharSequence eventText = event.getText() == null || event.getText().isEmpty()
                ? null : event.getText().get(0);
        String text = eventText == null ? "" : eventText.toString().trim();

        if (text.isEmpty() && event.getSource() != null) {
            text = collectVisibleText(event.getSource());
        }

        if (!text.isEmpty() && !text.equals(lastSpoken) && tts != null) {
            lastSpoken = text;
            tts.speak(text, TextToSpeech.QUEUE_ADD, null, "live_comment");
        }
    }

    private String collectVisibleText(AccessibilityNodeInfo node) {
        if (node == null) return "";
        StringBuilder result = new StringBuilder();
        CharSequence ownText = node.getText();
        if (ownText != null) result.append(ownText).append(" ");
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                result.append(collectVisibleText(child));
                child.recycle();
            }
        }
        return result.toString().trim();
    }

    @Override
    public void onInterrupt() {
        if (tts != null) tts.stop();
    }

    @Override
    public void onDestroy() {
        if (floatingButton != null && windowManager != null) {
            windowManager.removeView(floatingButton);
            floatingButton = null;
        }
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
        }
        super.onDestroy();
    }
}
