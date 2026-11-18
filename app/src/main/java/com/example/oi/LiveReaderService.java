package com.example.oi;

import android.accessibilityservice.AccessibilityService;
import android.content.*;
import android.speech.tts.TextToSpeech;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.Locale;

public class LiveReaderService extends AccessibilityService {
    private TextToSpeech tts;
    private boolean ttsReady = false;
    private String lastSpoken = "";
    private BroadcastReceiver stopReceiver;

    @Override public void onServiceConnected() {
        super.onServiceConnected();
        tts = new TextToSpeech(this, result -> {
            if (result == TextToSpeech.SUCCESS) {
                ttsReady = true;
                tts.setLanguage(new Locale("pt", "BR"));
                applyVoice();
            }
        });
        stopReceiver = new BroadcastReceiver() {
            @Override public void onReceive(Context c, Intent i) {
                if (tts != null) tts.stop();
                lastSpoken = "";
            }
        };
        registerReceiver(stopReceiver, new IntentFilter("com.example.oi.STOP_READING"));
    }

    private void applyVoice() {
        if (tts == null || !ttsReady) return;
        int selected = getSharedPreferences("livevoz", MODE_PRIVATE).getInt("voice", 0);
        for (android.speech.tts.Voice voice : tts.getVoices()) {
            if (voice.getLocale().getLanguage().equals("pt")) {
                String name = voice.getName().toLowerCase(Locale.ROOT);
                boolean femaleLike = name.contains("female") || name.contains("woman");
                boolean maleLike = name.contains("male") || name.contains("man");
                if ((selected == 0 && femaleLike) || (selected == 1 && maleLike)) {
                    tts.setVoice(voice);
                    return;
                }
            }
        }
        // Se o aparelho não oferecer vozes identificadas por gênero, usa a voz padrão em português.
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) return;
        boolean active = getSharedPreferences("livevoz", MODE_PRIVATE).getBoolean("enabled", false);
        if (!active) {
            if (tts != null) tts.stop();
            return;
        }

        CharSequence pkgName = event.getPackageName();
        String pkg = pkgName == null ? "" : pkgName.toString();
        boolean tikTok = pkg.equals("com.zhiliaoapp.musically") || pkg.equals("com.ss.android.ugc.trill");

        // Ao sair do TikTok, interrompe imediatamente a fala.
        if (!tikTok) {
            if (tts != null) tts.stop();
            lastSpoken = "";
            return;
        }

        if (event.getEventType() != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
                && event.getEventType() != AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) return;

        String text = "";
        if (event.getText() != null && !event.getText().isEmpty()) {
            text = event.getText().toString();
        }
        if (text.trim().isEmpty()) {
            AccessibilityNodeInfo source = event.getSource();
            if (source != null) text = collectText(source);
        }
        text = text.trim();
        if (!text.isEmpty() && !text.equals(lastSpoken) && tts != null && ttsReady) {
            lastSpoken = text;
            applyVoice();
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "live_comment");
        }
    }

    private String collectText(AccessibilityNodeInfo node) {
        if (node == null) return "";
        StringBuilder out = new StringBuilder();
        CharSequence own = node.getText();
        if (own != null) out.append(own).append(" ");
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                out.append(collectText(child));
                child.recycle();
            }
        }
        return out.toString();
    }

    @Override public void onInterrupt() {
        if (tts != null) tts.stop();
    }

    @Override public void onDestroy() {
        if (stopReceiver != null) {
            try { unregisterReceiver(stopReceiver); } catch (Exception ignored) {}
        }
        if (tts != null) { tts.stop(); tts.shutdown(); }
        super.onDestroy();
    }
}
