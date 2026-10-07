package com.bubbleai.app;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.widget.LinearLayout;

/** Root jendela chat: menangkap tombol Back supaya chat bisa ditutup. */
public class ChatRootLayout extends LinearLayout {

    public interface BackListener { void onBack(); }

    private BackListener backListener;

    public ChatRootLayout(Context context) { super(context); }
    public ChatRootLayout(Context context, AttributeSet attrs) { super(context, attrs); }

    public void setBackListener(BackListener l) { this.backListener = l; }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            if (event.getAction() == KeyEvent.ACTION_UP && backListener != null) {
                backListener.onBack();
            }
            return true;
        }
        return super.dispatchKeyEvent(event);
    }
}
