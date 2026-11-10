package com.pb.browser;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

// Robot ve bang Canvas: mat chop, anten nhap nhay, den LED o mieng. Chi ve khi dang hien tren man hinh.
final class RobotView extends View {
    final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    final RectF r = new RectF();
    final long t0 = System.currentTimeMillis();

    final boolean low;   // true: ve thua hon (~15 khung/giay) de khong tranh CPU voi trang web
    RobotView(Context c) { this(c, false); }
    RobotView(Context c, boolean lowPower) { super(c); low = lowPower; }

    @Override protected void onDraw(Canvas cv) {
        float w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;
        float t = (System.currentTimeMillis() - t0) / 1000f;
        float u = Math.min(w, h);
        float cx = w / 2f, cy = h * 0.56f;
        float hw = u * 0.78f, hh = u * 0.58f;
        int cyan = UI.CYAN;

        // anten
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(u * 0.04f); p.setColor(0xFF2A4A68);
        cv.drawLine(cx, cy - hh / 2f, cx, cy - hh / 2f - u * 0.16f, p);
        float pulse = 0.5f + 0.5f * (float) Math.sin(t * 3.2f);
        p.setStyle(Paint.Style.FILL); p.setColor(UI.alpha(cyan, 60 + (int) (120 * pulse)));
        cv.drawCircle(cx, cy - hh / 2f - u * 0.19f, u * (0.08f + 0.04f * pulse), p);
        p.setColor(cyan);
        cv.drawCircle(cx, cy - hh / 2f - u * 0.19f, u * 0.05f, p);

        // tai
        p.setColor(0xFF12304A);
        r.set(cx - hw / 2f - u * 0.07f, cy - hh * 0.16f, cx - hw / 2f + u * 0.02f, cy + hh * 0.16f);
        cv.drawRoundRect(r, u * 0.03f, u * 0.03f, p);
        r.set(cx + hw / 2f - u * 0.02f, cy - hh * 0.16f, cx + hw / 2f + u * 0.07f, cy + hh * 0.16f);
        cv.drawRoundRect(r, u * 0.03f, u * 0.03f, p);

        // dau
        r.set(cx - hw / 2f, cy - hh / 2f, cx + hw / 2f, cy + hh / 2f);
        p.setColor(0xFF0B1A2C);
        cv.drawRoundRect(r, u * 0.16f, u * 0.16f, p);
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(u * 0.03f); p.setColor(UI.alpha(cyan, 200));
        cv.drawRoundRect(r, u * 0.16f, u * 0.16f, p);

        // mat chop moi ~3.2s
        float ph = t % 3.2f;
        float open = ph > 3.0f ? Math.max(0.12f, 1f - (float) Math.sin((ph - 3.0f) / 0.2f * Math.PI)) : 1f;
        float ew = hw * 0.2f, eh = hh * 0.3f * open;
        float ey = cy - hh * 0.08f;
        float look = (float) Math.sin(t * 0.9f) * hw * 0.03f;   // mat lien lia nhe
        p.setStyle(Paint.Style.FILL); p.setColor(cyan);
        r.set(cx - hw * 0.22f - ew / 2f + look, ey - eh / 2f, cx - hw * 0.22f + ew / 2f + look, ey + eh / 2f);
        cv.drawRoundRect(r, u * 0.04f, u * 0.04f, p);
        r.set(cx + hw * 0.22f - ew / 2f + look, ey - eh / 2f, cx + hw * 0.22f + ew / 2f + look, ey + eh / 2f);
        cv.drawRoundRect(r, u * 0.04f, u * 0.04f, p);

        // mieng: 5 thanh LED nhay theo song
        float bw = hw * 0.07f, gap = hw * 0.035f, bx = cx - (5 * bw + 4 * gap) / 2f, by = cy + hh * 0.27f;
        for (int i = 0; i < 5; i++) {
            float lv = 0.35f + 0.65f * (0.5f + 0.5f * (float) Math.sin(t * 4f + i * 1.1f));
            float bh = hh * 0.16f * lv;
            p.setColor(UI.alpha(UI.GREEN, 200));
            r.set(bx + i * (bw + gap), by - bh / 2f, bx + i * (bw + gap) + bw, by + bh / 2f);
            cv.drawRoundRect(r, bw * 0.4f, bw * 0.4f, p);
        }
        if (low) postInvalidateDelayed(66); else postInvalidateOnAnimation();
    }
}
