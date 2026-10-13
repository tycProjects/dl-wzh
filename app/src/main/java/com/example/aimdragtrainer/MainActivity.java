package com.example.aimdragtrainer;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.graphics.Typeface;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import java.util.Random;

public class MainActivity extends Activity {
    private final int BG = Color.rgb(10,14,25), PANEL = Color.rgb(20,27,44);
    private final int CYAN = Color.rgb(66,232,244), WHITE = Color.rgb(240,245,255);
    private TextView scoreView, timeView, hitView, statusView;
    private ArenaView arena;
    private Button startButton;
    private SeekBar sensitivityBar, sizeBar, speedBar;
    private int score = 0, attempts = 0, hits = 0, seconds = 30;
    private boolean running = false;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();
    private final Runnable clock = new Runnable() {
        @Override public void run() {
            if (!running) return;
            seconds--;
            timeView.setText("TIME  " + seconds + "s");
            if (seconds <= 0) {
                running = false;
                statusView.setText("Practice finished — tap Start to play again");
                startButton.setText("START PRACTICE");
                arena.invalidate();
            } else handler.postDelayed(this, 1000);
        }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        buildUi();
    }

    private int dp(float v) { return (int)(v * getResources().getDisplayMetrics().density + 0.5f); }

    private TextView label(String text, int size, int color) {
        TextView t = new TextView(this);
        t.setText(text); t.setTextSize(size); t.setTextColor(color);
        t.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        return t;
    }

    private LinearLayout column() {
        LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(18), dp(10), dp(18), dp(12)); l.setBackgroundColor(BG);
        return l;
    }

    private void buildUi() {
        LinearLayout root = column();
        TextView title = label("AIM DRAG TRAINER", 23, WHITE);
        title.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(title, new LinearLayout.LayoutParams(-1, dp(46)));
        TextView sub = label("PRACTICE MODE  •  Drag your finger to targets", 12, CYAN);
        root.addView(sub, new LinearLayout.LayoutParams(-1, dp(28)));

        LinearLayout stats = new LinearLayout(this);
        stats.setOrientation(LinearLayout.HORIZONTAL);
        scoreView = stat("SCORE  000");
        timeView = stat("TIME  30s");
        hitView = stat("HIT RATE  --%");
        stats.addView(scoreView, new LinearLayout.LayoutParams(0, dp(54), 1));
        stats.addView(timeView, new LinearLayout.LayoutParams(0, dp(54), 1));
        stats.addView(hitView, new LinearLayout.LayoutParams(0, dp(54), 1));
        root.addView(stats);

        arena = new ArenaView(this);
        LinearLayout.LayoutParams arenaParams = new LinearLayout.LayoutParams(-1, 0, 1);
        arenaParams.topMargin = dp(10); arenaParams.bottomMargin = dp(12);
        root.addView(arena, arenaParams);

        statusView = label("Tap Start, then drag onto glowing targets.", 12, WHITE);
        root.addView(statusView, new LinearLayout.LayoutParams(-1, dp(32)));

        addSlider(root, "DRAG SENSITIVITY", "50%", 0, 100, 50, 0);
        addSlider(root, "TARGET SIZE", "MEDIUM", 0, 100, 50, 1);
        addSlider(root, "TARGET SPEED", "NORMAL", 0, 100, 50, 2);

        startButton = new Button(this);
        startButton.setText("START PRACTICE");
        startButton.setTextColor(BG);
        startButton.setTextSize(15);
        startButton.setAllCaps(false);
        startButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(CYAN));
        startButton.setOnClickListener(v -> startPractice());
        root.addView(startButton, new LinearLayout.LayoutParams(-1, dp(52)));

        TextView footer = label("Standalone trainer • No game integration", 10, Color.rgb(145,158,184));
        footer.setGravity(Gravity.CENTER);
        root.addView(footer, new LinearLayout.LayoutParams(-1, dp(28)));
        setContentView(root);
    }

    private TextView stat(String text) {
        TextView t = label(text, 11, WHITE);
        t.setGravity(Gravity.CENTER);
        t.setBackgroundColor(PANEL);
        return t;
    }

    private void addSlider(LinearLayout root, String name, String value, int min, int max, int initial, int kind) {
        LinearLayout line = new LinearLayout(this);
        line.setOrientation(LinearLayout.HORIZONTAL);
        line.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = label(name, 11, WHITE);
        line.addView(title, new LinearLayout.LayoutParams(0, dp(28), 1));
        TextView val = label(value, 11, CYAN);
        val.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        line.addView(val, new LinearLayout.LayoutParams(dp(75), dp(28)));
        root.addView(line);
        SeekBar bar = new SeekBar(this);
        bar.setMax(max); bar.setProgress(initial);
        bar.setProgressTintList(android.content.res.ColorStateList.valueOf(CYAN));
        bar.setThumbTintList(android.content.res.ColorStateList.valueOf(CYAN));
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar b, int p, boolean user) {
                if (kind == 0) val.setText(p + "%");
                else if (kind == 1) val.setText(p < 34 ? "SMALL" : p < 67 ? "MEDIUM" : "LARGE");
                else val.setText(p < 34 ? "SLOW" : p < 67 ? "NORMAL" : "FAST");
                if (arena != null) arena.invalidate();
            }
            public void onStartTrackingTouch(SeekBar b) {}
            public void onStopTrackingTouch(SeekBar b) {}
        });
        if (kind == 0) sensitivityBar = bar;
        if (kind == 1) sizeBar = bar;
        if (kind == 2) speedBar = bar;
        root.addView(bar, new LinearLayout.LayoutParams(-1, dp(24)));
    }

    private void startPractice() {
        score = attempts = hits = 0; seconds = 30; running = true;
        scoreView.setText("SCORE  000"); timeView.setText("TIME  30s"); hitView.setText("HIT RATE  --%");
        statusView.setText("Drag from the crosshair onto a target!");
        startButton.setText("RESTART PRACTICE");
        arena.resetTarget();
        handler.removeCallbacks(clock);
        handler.postDelayed(clock, 1000);
        arena.invalidate();
    }

    @Override protected void onDestroy() {
        handler.removeCallbacks(clock);
        super.onDestroy();
    }

    private class ArenaView extends View {
        private final Paint p = new Paint(3);
        private float tx, ty, targetR, fingerX, fingerY;
        private boolean dragging = false;
        private long lastMove = 0;
        private float crossX, crossY;

        ArenaView(Context c) {
            super(c);
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            setBackgroundColor(Color.rgb(13,19,33));
            post(this::resetTarget);
        }

        void resetTarget() {
            float pad = dp(45);
            tx = pad + random.nextFloat() * Math.max(1, getWidth() - pad * 2);
            ty = pad + random.nextFloat() * Math.max(1, getHeight() - pad * 2);
            float size = sizeBar == null ? 50 : sizeBar.getProgress();
            targetR = dp(15) + dp(15) * size / 100f;
            crossX = getWidth() / 2f; crossY = getHeight() / 2f;
            invalidate();
        }

        @Override protected void onSizeChanged(int w, int h, int ow, int oh) {
            crossX = w / 2f; crossY = h / 2f;
            resetTarget();
        }

        @Override protected void onDraw(Canvas c) {
            super.onDraw(c);
            float w = getWidth(), h = getHeight();
            p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(dp(1)); p.setColor(Color.rgb(32,46,68));
            for (int x=dp(20); x<w; x+=dp(28)) c.drawLine(x,0,x,h,p);
            for (int y=dp(20); y<h; y+=dp(28)) c.drawLine(0,y,w,y,p);

            p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(dp(2)); p.setColor(CYAN);
            c.drawCircle(tx,ty,targetR+dp(8),p);
            p.setStyle(Paint.Style.FILL); p.setColor(Color.argb(45,66,232,244));
            c.drawCircle(tx,ty,targetR,p);
            p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(dp(3)); p.setColor(CYAN);
            c.drawCircle(tx,ty,targetR*0.58f,p);
            p.setStyle(Paint.Style.FILL); p.setColor(WHITE);
            c.drawCircle(tx,ty,dp(3),p);

            p.setColor(Color.argb(150,150,105,255)); p.setStrokeWidth(dp(2));
            c.drawLine(crossX-dp(13),crossY,crossX-dp(4),crossY,p);
            c.drawLine(crossX+dp(4),crossY,crossX+dp(13),crossY,p);
            c.drawLine(crossX,crossY-dp(13),crossX,crossY-dp(4),p);
            c.drawLine(crossX,crossY+dp(4),crossX,crossY+dp(13),p);
            if (dragging) {
                p.setColor(Color.argb(180,66,232,244)); p.setStrokeWidth(dp(2));
                c.drawLine(fingerX,fingerY,tx,ty,p);
                p.setStyle(Paint.Style.STROKE); c.drawCircle(fingerX,fingerY,dp(12),p);
                p.setStyle(Paint.Style.FILL);
            }
        }

        @Override public boolean onTouchEvent(MotionEvent e) {
            if (!running) return true;
            float x=e.getX(), y=e.getY();
            if (e.getAction()==MotionEvent.ACTION_DOWN) {
                attempts++; dragging=true; fingerX=x; fingerY=y; lastMove=System.currentTimeMillis();
                invalidate(); return true;
            } else if (e.getAction()==MotionEvent.ACTION_MOVE) {
                fingerX=x; fingerY=y;
                float sensitivity = sensitivityBar == null ? 0.5f : sensitivityBar.getProgress()/50f;
                float dx = (x-crossX)*sensitivity, dy=(y-crossY)*sensitivity;
                crossX = Math.max(0, Math.min(getWidth(), crossX+dx));
                crossY = Math.max(0, Math.min(getHeight(), crossY+dy));
                if (Math.hypot(crossX-tx,crossY-ty) <= targetR) {
                    hits++; score += 100;
                    hitView.setText("HIT RATE  " + Math.round(100f*hits/Math.max(1,attempts)) + "%");
                    scoreView.setText(String.format(java.util.Locale.US, "SCORE  %03d", score));
                    statusView.setText("Nice hit! Keep practicing.");
                    float speed = speedBar == null ? 50 : speedBar.getProgress();
                    resetTarget();
                    if (speed > 66) resetTarget();
                }
                invalidate(); return true;
            } else if (e.getAction()==MotionEvent.ACTION_UP || e.getAction()==MotionEvent.ACTION_CANCEL) {
                dragging=false; invalidate(); return true;
            }
            return true;
        }
    }
}
