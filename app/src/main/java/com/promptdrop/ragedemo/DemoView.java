package com.promptdrop.ragedemo;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Color;
import android.view.View;

public class DemoView extends View {
    private final Paint p = new Paint(3);
    private final SharedPreferences prefs;

    public DemoView(Context context) {
        super(context);
        prefs = context.getSharedPreferences("promptdrop_demo", Context.MODE_PRIVATE);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        float w = getWidth(), h = getHeight();
        c.drawColor(Color.rgb(18, 23, 32));

        p.setColor(Color.rgb(37, 47, 61));
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(1f);
        for (int x = 0; x < w; x += 32) c.drawLine(x, 0, x, h, p);
        for (int y = 0; y < h; y += 32) c.drawLine(0, y, w, y, p);

        p.setStyle(Paint.Style.FILL);
        p.setColor(Color.rgb(80, 255, 180));
        p.setTextSize(11);
        c.drawText("SIMULATED TRAINING VIEW", 12, 18, p);

        if (prefs.getBoolean("fov", true)) {
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(2);
            p.setColor(Color.argb(180, 80, 255, 180));
            float radius = Math.min(w, h) * prefs.getInt("fov_value", 45) / 240f;
            c.drawCircle(w * .5f, h * .56f, radius, p);
        }

        if (prefs.getBoolean("esp", true) && prefs.getBoolean("box", true)) {
            drawTarget(c, w * .28f, h * .30f, "BOT-A", "100");
            drawTarget(c, w * .68f, h * .38f, "BOT-B", "72");
        }
    }

    private void drawTarget(Canvas c, float x, float y, String name, String hp) {
        float bw = 54, bh = 112;
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(2);
        p.setColor(Color.rgb(80, 255, 180));
        c.drawRect(new RectF(x - bw/2, y, x + bw/2, y + bh), p);

        if (prefs.getBoolean("skeleton", true)) {
            p.setColor(Color.WHITE);
            p.setStrokeWidth(1.5f);
            c.drawCircle(x, y + 15, 8, p);
            c.drawLine(x, y + 23, x, y + 67, p);
            c.drawLine(x, y + 35, x - 17, y + 52, p);
            c.drawLine(x, y + 35, x + 17, y + 52, p);
            c.drawLine(x, y + 67, x - 14, y + 101, p);
            c.drawLine(x, y + 67, x + 14, y + 101, p);
        }

        p.setStyle(Paint.Style.FILL);
        p.setTextSize(10);
        p.setColor(Color.WHITE);
        c.drawText(name, x - 20, y - 7, p);

        if (prefs.getBoolean("health", true)) {
            p.setColor(Color.DKGRAY);
            c.drawRect(x - bw/2 - 7, y, x - bw/2 - 3, y + bh, p);
            p.setColor(Color.rgb(80, 255, 120));
            c.drawRect(x - bw/2 - 7, y + bh * .25f, x - bw/2 - 3, y + bh, p);
        }
        if (prefs.getBoolean("distance", true)) {
            p.setColor(Color.LTGRAY);
            c.drawText("12." + (name.endsWith("A") ? "4" : "8") + "m", x - 12, y + bh + 13, p);
        }
    }
}
