package com.lsupra.fpsmeter;

import android.app.*;
import android.content.*;
import android.content.pm.ServiceInfo;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.*;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;

public class OverlayService extends Service {
    static final String[] N = {"real", "fake", "text", "bubble"}, KEY = {"onR", "onF", "onT", "onB"};
    static volatile boolean up = false;
    static volatile float liveR = -1, liveF = -1;
    static volatile String liveBat = "--", liveTemp = "--", liveRam = "--";
    private volatile boolean hideFps = false;

    /** Ô nổi: TextView có thể vẽ thêm đồ thị FPS nhỏ ở dưới. */
    static class Pill extends TextView {
        float[] g = new float[0];
        boolean graph;
        int gh, padB;
        final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        Pill(Context c) { super(c); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(3f); }
        @Override protected void onDraw(Canvas cv) {
            super.onDraw(cv);
            if (!graph || g.length < 2) return;
            float mn = g[0], mx = g[0];
            for (float x : g) { mn = Math.min(mn, x); mx = Math.max(mx, x); }
            float sp = Math.max(1f, mx - mn);
            float l = getPaddingLeft(), r = getWidth() - getPaddingRight();
            float bottom = getHeight() - padB, top = bottom - gh;
            Path path = new Path();
            for (int i = 0; i < g.length; i++) {
                float x = l + (r - l) * i / (g.length - 1), y = bottom - (g[i] - mn) / sp * (bottom - top);
                if (i == 0) path.moveTo(x, y); else path.lineTo(x, y);
            }
            p.setColor(getCurrentTextColor());
            cv.drawPath(path, p);
        }
    }

    /** Bong bóng: logo tròn + vòng sáng quay + huy hiệu số ô đang bật / chấm đỏ khi đang quay. */
    static class Bub extends ImageView {
        final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG), tp = new Paint(Paint.ANTI_ALIAS_FLAG);
        final RectF rf = new RectF();
        int ac = 0xFFFF3030, badge = 0;
        boolean rec, open;
        float ph = 0;
        Bub(Context c) {
            super(c);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeCap(Paint.Cap.ROUND);
            tp.setTextAlign(Paint.Align.CENTER);
            tp.setTypeface(Typeface.DEFAULT_BOLD);
        }
        @Override protected void onDraw(Canvas cv) {
            super.onDraw(cv);
            float w = getWidth(), h = getHeight(), d = getResources().getDisplayMetrics().density;
            float r = Math.min(w, h) / 2f - 2.5f * d;
            rf.set(w / 2 - r, h / 2 - r, w / 2 + r, h / 2 + r);
            p.setStrokeWidth(2f * d);
            p.setColor(0x44FFFFFF);
            cv.drawArc(rf, 0, 360, false, p);
            p.setStrokeWidth(2.6f * d);
            p.setColor(ac);
            cv.drawArc(rf, ph, open ? 360 : 100, false, p);
            if (!open) cv.drawArc(rf, ph + 180, 40, false, p);
            if (rec || badge > 0) {
                float br = 8.5f * d, bx = w - br - 1.5f * d, by = br + 1.5f * d;
                p.setStyle(Paint.Style.FILL);
                p.setColor(rec ? 0xFFFF1F1F : ac);
                if (rec) {
                    float pulse = 0.5f + 0.5f * (float) Math.sin(ph / 28f);
                    p.setAlpha((int) (120 + 135 * pulse));
                }
                cv.drawCircle(bx, by, br, p);
                p.setStyle(Paint.Style.STROKE);
                if (!rec) {
                    tp.setColor(0xFF0A0A10);
                    tp.setTextSize(10.5f * d);
                    cv.drawText(String.valueOf(badge), bx, by + 3.6f * d, tp);
                }
            }
        }
    }

    private WindowManager wm;
    private final Pill[] v = new Pill[3];
    private final WindowManager.LayoutParams[] lp = new WindowManager.LayoutParams[3];
    private final Handler h = new Handler(Looper.getMainLooper());
    private final java.util.Random rnd = new java.util.Random();
    private JSONObject cfg = new JSONObject();
    private final String[] applied = new String[3], lastTxt = new String[3];
    private final boolean[] bot = new boolean[3], right = new boolean[3];
    private final float[][] hist = new float[2][];
    private final float[] cur = {-1, 144};
    private final int[] lastCol = new int[3];
    private volatile float gFps = -1, gLow = -1;
    private volatile boolean gFound = false, running = true;
    private float fCur = 144;
    private long lastR, lastF, lastT, lastBat;
    private Thread fpsThread;
    private SharedPreferences.OnSharedPreferenceChangeListener prefL;
    private Bub bubble;
    private LinearLayout panel, editor;
    private WindowManager.LayoutParams bp;
    private final LinearLayout[] tl = new LinearLayout[4];
    private final TextView[] ti = new TextView[4], ts = new TextView[4];
    private static final String[] IT = {"FPS THẬT", "FPS GIẢ", "CHỮ NỔI", "QUAY MÀN HÌNH"};
    private static final String[] IC = {"⚡", "◈", "✎", "●"};
    private final boolean[] fxOn = new boolean[3];

    private int dp(int x) { return Math.round(x * getResources().getDisplayMetrics().density); }
    private int col(String s) { try { return Color.parseColor(s); } catch (Exception e) { return Color.WHITE; } }
    private int ovType() {
        return Build.VERSION.SDK_INT >= 26 ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE;
    }
    private static final int BASE = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;

    static String[] sys(Context c) {
        String b = "--", t = "--", r = "--";
        try {
            Intent i = c.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            b = String.valueOf(i.getIntExtra("level", 0) * 100 / Math.max(1, i.getIntExtra("scale", 100)));
            t = String.format(java.util.Locale.US, "%.1f", i.getIntExtra("temperature", 0) / 10f);
        } catch (Exception ignored) {}
        try {
            ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
            ((ActivityManager) c.getSystemService(Context.ACTIVITY_SERVICE)).getMemoryInfo(mi);
            r = String.valueOf((int) (100 - mi.availMem * 100 / Math.max(1, mi.totalMem)));
        } catch (Exception ignored) {}
        return new String[]{b, t, r};
    }

    static String bat(Context c) {
        try {
            Intent b = c.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            return String.valueOf(b.getIntExtra("level", 0) * 100 / Math.max(1, b.getIntExtra("scale", 100)));
        } catch (Exception e) { return "--"; }
    }

    // ---------- vòng cập nhật nhẹ (100ms, chỉ đổi giao diện khi giá trị thật sự đổi) ----------
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            long now = SystemClock.uptimeMillis();
            JSONObject cr = cfg.optJSONObject("real"), cf = cfg.optJSONObject("fake");
            if (now - lastBat > 10000) { lastBat = now; String[] sy = sys(OverlayService.this); liveBat = sy[0]; liveTemp = sy[1]; liveRam = sy[2]; }
            JSONObject rc = cfg.optJSONObject("rec");
            boolean recOn = RecService.phase == 2;
            hideFps = recOn && rc != null && rc.optInt("hf", 0) == 1;
            if (bubble != null) {
                int cnt = 0;
                for (int k = 0; k < 3; k++) if (v[k] != null) cnt++;
                bubble.ph = (bubble.ph + 14f) % 360f;
                bubble.badge = cnt;
                bubble.rec = RecService.phase == 2;
                bubble.open = panel != null;
                bubble.ac = col(cfg.optString("ac", "#ff3030"));
                bubble.invalidate();
                int bvis = recOn && (rc == null || rc.optInt("hb", 1) == 1) ? View.INVISIBLE : View.VISIBLE;
                if (bubble.getVisibility() != bvis) bubble.setVisibility(bvis);
            }
            int want = hideFps ? View.INVISIBLE : View.VISIBLE;
            for (int k = 0; k < 3; k++)
                if (v[k] != null && v[k].getVisibility() != View.GONE && v[k].getVisibility() != want) v[k].setVisibility(want);
            if (now - lastF >= Math.max(100, cf == null ? 250 : cf.optInt("speed", 250))) { lastF = now; updateFake(cf); }
            if (now - lastR >= Math.max(250, cr == null ? 500 : cr.optInt("interval", 500))) { lastR = now; updateReal(cr); }
            if (now - lastT >= 1000) { lastT = now; updateText(cfg.optJSONObject("text")); }
            h.postDelayed(this, 100);
        }
    };

    private float[] push(int k, float x) {
        float[] o = hist[k] == null ? new float[0] : hist[k];
        int n = Math.min(40, o.length + 1);
        float[] r = new float[n];
        System.arraycopy(o, o.length + 1 > 40 ? 1 : 0, r, 0, n - 1);
        r[n - 1] = x;
        hist[k] = r;
        return r;
    }

    private void updateReal(JSONObject c) {
        boolean game = c != null && "game".equals(c.optString("src", "screen")) && Priv.level() > 0;
        float val = game ? (gFound ? gFps : -1) : display();
        liveR = val; cur[0] = val;
        if (v[0] == null) return;
        v[0].setVisibility(game && !gFound && c.optInt("hide", 0) == 1 ? View.GONE : (hideFps ? View.INVISIBLE : View.VISIBLE));
        float[] hs = val > 0 ? push(0, val) : (hist[0] == null ? new float[0] : hist[0]);
        show(0, c, val, hs, game && gFound ? gLow : -1);
    }

    private float display() {
        try { return wm.getDefaultDisplay().getRefreshRate(); } catch (Exception e) { return 60; }
    }

    private void updateFake(JSONObject f) {
        int lo = Math.max(1, f == null ? 138 : f.optInt("min", 138)), hi = f == null ? 144 : f.optInt("max", 144);
        if (hi < lo) { int x = lo; lo = hi; hi = x; }
        float span = hi - lo;
        if (span == 0) fCur = lo;
        else {
            fCur += (rnd.nextFloat() * 2 - 1) * span * 0.4f;
            if (fCur > hi) fCur = hi - rnd.nextFloat() * span * 0.3f;
            if (fCur < lo) fCur = lo + rnd.nextFloat() * span * 0.3f;
        }
        liveF = fCur; cur[1] = fCur;
        if (v[1] == null) return;
        show(1, f, fCur, push(1, fCur), -1);
    }

    private void show(int i, JSONObject c, float val, float[] hs, float lowIn) {
        if (c == null) c = new JSONObject();
        int n = Math.round(val);
        String num = val > 0 ? (c.optInt("dec", 0) == 1 ? String.format(java.util.Locale.US, "%.1f", val) : String.valueOf(n)) : "--";
        String p = c.optString("prefix", "FPS"), s = c.optString("suffix", "");
        String first = (p.isEmpty() ? "" : p + " ") + num + (s.isEmpty() ? "" : " " + s);
        StringBuilder ex = new StringBuilder();
        if (c.optInt("avg", 0) == 1 && hs.length > 0) {
            float sum = 0; for (float x : hs) sum += x;
            ex.append("AVG ").append(Math.round(sum / hs.length));
        }
        if (c.optInt("low", 0) == 1 && hs.length > 0) {
            float lo = lowIn;
            if (lo <= 0) { lo = hs[0]; for (float x : hs) lo = Math.min(lo, x); }
            if (ex.length() > 0) ex.append("  ");
            ex.append("1% ").append(Math.round(lo));
        }
        if (c.optInt("ms", 0) == 1 && val > 0) {
            if (ex.length() > 0) ex.append("  ");
            ex.append(String.format(java.util.Locale.US, "%.1fms", 1000f / val));
        }
        String txt = ex.length() == 0 ? first : first + (c.optInt("multi", 0) == 1 ? "\n" : "  ") + ex;
        if (!txt.equals(lastTxt[i])) { lastTxt[i] = txt; v[i].setText(txt); }
        int color = col(c.optString("color", "#ffffff"));
        if (c.optInt("autoc", 0) == 1)
            color = val >= c.optInt("t1", 55) ? 0xFF00FF9D : val >= c.optInt("t2", 30) ? 0xFFFFD60A : 0xFFFF3030;
        if (color != lastCol[i]) { lastCol[i] = color; applyColor(i, c, color); }
        if (c.optInt("graph", 0) == 1) { v[i].g = hs; v[i].invalidate(); }
    }

    private void reshow(int i) {
        if (i == 2) { updateText(cfg.optJSONObject("text")); return; }
        show(i, cfg.optJSONObject(N[i]), cur[i], hist[i] == null ? new float[0] : hist[i], i == 0 && gFound ? gLow : -1);
    }

    private void applyColor(int i, JSONObject c, int color) {
        int glow = c.optInt("glow", 0);
        v[i].setTextColor(color);
        v[i].setShadowLayer(glow > 0 ? glow : 0, 0, 0, glow > 0 ? color : Color.TRANSPARENT);
    }

    private String tokens(String t) {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        return t.replace("{time}", String.format(java.util.Locale.US, "%02d:%02d", cal.get(java.util.Calendar.HOUR_OF_DAY), cal.get(java.util.Calendar.MINUTE)))
                .replace("{date}", String.format(java.util.Locale.US, "%02d/%02d", cal.get(java.util.Calendar.DAY_OF_MONTH), cal.get(java.util.Calendar.MONTH) + 1))
                .replace("{bat}", liveBat + "%").replace("{temp}", liveTemp + "°").replace("{ram}", liveRam + "%").replace("{fps}", String.valueOf(Math.max(0, Math.round(liveR))));
    }

    private void updateText(JSONObject t) {
        if (v[2] == null) return;
        String s = tokens(t == null ? "LONG SUPRA" : t.optString("text", "LONG SUPRA"));
        if (s.isEmpty()) s = " ";
        if (!s.equals(lastTxt[2])) { lastTxt[2] = s; v[2].setText(s); }
    }

    // ---------- kiểu dáng + vị trí ----------
    private String posOf(int i) {
        JSONObject o = cfg.optJSONObject(N[i]);
        return o == null ? (i == 2 ? "tl" : "bl") : o.optString("pos", i == 2 ? "tl" : "bl");
    }

    private void setCorner(int i, String pos) {
        bot[i] = pos.charAt(0) == 'b';
        right[i] = pos.length() > 1 && pos.charAt(1) == 'r';
        lp[i].gravity = (bot[i] ? Gravity.BOTTOM : Gravity.TOP) | (right[i] ? Gravity.END : Gravity.START);
    }

    private void style(int i) {
        Pill tv = v[i];
        JSONObject c = cfg.optJSONObject(N[i]);
        if (c == null) c = new JSONObject();
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, (float) c.optDouble("size", 13));
        int st = (c.optInt("bold", 1) == 1 ? Typeface.BOLD : 0) | (c.optInt("italic", 0) == 1 ? Typeface.ITALIC : 0);
        String font = c.optString("font", "mono");
        Typeface tf;
        if (font.equals("sans")) tf = Typeface.create(Typeface.SANS_SERIF, st);
        else if (font.equals("serif")) tf = Typeface.create(Typeface.SERIF, st);
        else if (font.equals("cond")) tf = Typeface.create("sans-serif-condensed", st);
        else if (font.equals("black")) tf = Typeface.create("sans-serif-black", st);
        else tf = Typeface.create(Typeface.MONOSPACE, st);
        tv.setTypeface(tf);
        tv.setLetterSpacing(c.optInt("ls", 0) / 100f);
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(dp(c.optInt("r", 0)));
        int bg = col(c.optString("bg", "#000000"));
        g.setColor(Color.argb(c.optInt("bga", 100) * 255 / 100, Color.red(bg), Color.green(bg), Color.blue(bg)));
        if (c.optInt("bw", 1) > 0) g.setStroke(dp(c.optInt("bw", 1)), col(c.optString("bc", "#555555")));
        tv.setBackground(g);
        tv.setMinWidth(dp(c.optInt("w", 0)));
        String al = c.optString("align", "c");
        tv.setGravity(i == 2 ? (al.equals("l") ? Gravity.START : al.equals("r") ? Gravity.END : Gravity.CENTER_HORIZONTAL) : Gravity.CENTER);
        tv.setIncludeFontPadding(false);
        int py = dp(c.optInt("py", 3)), px = dp(c.optInt("px", 12));
        boolean gr = i < 2 && c.optInt("graph", 0) == 1;
        tv.graph = gr; tv.gh = dp(16); tv.padB = py;
        tv.setPadding(px, py, px, py + (gr ? dp(18) : 0));
        tv.setAlpha(c.optInt("alpha", 100) / 100f);
        tv.getPaint().setShader(null);
        lastCol[i] = 0;
        applyColor(i, c, col(c.optString("color", "#ffffff")));
        lastTxt[i] = null;
        String pos = posOf(i), tag = pos + ":" + c.optInt("snap", 0) + ":4";
        if (!tag.equals(applied[i])) {                 // dính sát góc, các ô cùng góc nằm cạnh nhau
            applied[i] = tag;
            int off = 0;
            for (int j = 0; j < i; j++) {
                if (v[j] != null && posOf(j).equals(pos)) {
                    v[j].measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                    off += Math.max(v[j].getWidth(), v[j].getMeasuredWidth()) + dp(2);
                }
            }
            setCorner(i, pos);
            lp[i].x = off; lp[i].y = 0;
            getSharedPreferences("s", MODE_PRIVATE).edit().putString("pc" + i, pos).putString("pt" + i, tag)
                    .putInt("px" + i, lp[i].x).putInt("py" + i, lp[i].y).apply();
        }
        lp[i].flags = BASE | (c.optInt("lock", 0) == 1 ? WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE : 0);
        wm.updateViewLayout(tv, lp[i]);
    }

    private void loadCfg() {
        try { cfg = new JSONObject(getSharedPreferences("s", MODE_PRIVATE).getString("cfg", "{}")); }
        catch (Exception e) { cfg = new JSONObject(); }
    }

    private void add(final int i) {
        final Pill tv = new Pill(this);
        final SharedPreferences sp = getSharedPreferences("s", MODE_PRIVATE);
        final WindowManager.LayoutParams p = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT, ovType(), BASE, PixelFormat.TRANSLUCENT);
        v[i] = tv; lp[i] = p;
        p.gravity = Gravity.TOP | Gravity.START;
        String pc = sp.getString("pc" + i, null);
        if (pc != null) {                              // khôi phục vị trí đã kéo
            setCorner(i, pc);
            p.x = sp.getInt("px" + i, 0); p.y = sp.getInt("py" + i, 0);
            applied[i] = sp.getString("pt" + i, null);
        } else applied[i] = null;
        lastCol[i] = 0;
        tv.setOnTouchListener(new View.OnTouchListener() {
            int sx, sy; float tx, ty; boolean moved;
            @Override public boolean onTouch(View view, MotionEvent e) {
                switch (e.getAction()) {
                    case MotionEvent.ACTION_DOWN: sx = p.x; sy = p.y; tx = e.getRawX(); ty = e.getRawY(); moved = false; return true;
                    case MotionEvent.ACTION_MOVE:
                        if (Math.abs(e.getRawX() - tx) + Math.abs(e.getRawY() - ty) > dp(8)) moved = true;
                        if (moved) {
                            p.x = sx + (right[i] ? -1 : 1) * (int) (e.getRawX() - tx);
                            p.y = sy + (bot[i] ? -1 : 1) * (int) (e.getRawY() - ty);
                            wm.updateViewLayout(view, p);
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (moved) sp.edit().putInt("px" + i, p.x).putInt("py" + i, p.y).apply();
                        else if (i == 2) openEditor();
                        return true;
                }
                return false;
            }
        });
        wm.addView(tv, p);
        style(i);
        if (i == 0) updateReal(cfg.optJSONObject("real")); else if (i == 1) updateFake(cfg.optJSONObject("fake")); else updateText(cfg.optJSONObject("text"));
    }

    // ---------- Hiệu ứng chữ (neon, cầu vồng, nhịp, nháy, chuyển màu) ----------
    private final Runnable fxr = new Runnable() {
        @Override public void run() {
            long t = SystemClock.uptimeMillis();
            boolean any = false;
            for (int i = 0; i < 3; i++) {
                Pill tv = v[i];
                if (tv == null) continue;
                JSONObject c = cfg.optJSONObject(N[i]);
                int fx = c == null ? 0 : c.optInt("fx", 0);
                if (fx == 0) { if (fxOn[i]) { fxOn[i] = false; tv.getPaint().setShader(null); lastCol[i] = 0; } continue; }
                any = true; fxOn[i] = true;
                int sp = Math.max(1, Math.min(100, c.optInt("fxs", 50))), glow = c.optInt("glow", 6);
                int base = lastCol[i] != 0 ? lastCol[i] : col(c.optString("color", "#ffffff"));
                float al = c.optInt("alpha", 100) / 100f;
                tv.getPaint().setShader(null);
                if (fx == 1) {
                    tv.setTextColor(base); tv.setShadowLayer(Math.max(glow, 12), 0, 0, base); tv.setAlpha(al);
                } else if (fx == 2) {
                    int cc = Color.HSVToColor(new float[]{(t * sp / 40L) % 360L, 1f, 1f});
                    tv.setTextColor(cc); tv.setShadowLayer(Math.max(glow, 10), 0, 0, cc); tv.setAlpha(al);
                } else if (fx == 3) {
                    float ph = (float) (0.5 + 0.5 * Math.sin(t * sp / 8000.0 * Math.PI * 2));
                    tv.setTextColor(base); tv.setShadowLayer(3 + 22 * ph, 0, 0, base); tv.setAlpha(al * (0.7f + 0.3f * ph));
                } else if (fx == 4) {
                    long per = Math.max(150, 2200 - sp * 20L);
                    tv.setTextColor(base); tv.setShadowLayer(Math.max(glow, 10), 0, 0, base);
                    tv.setAlpha(al * ((t % per) < per / 2 ? 1f : 0.15f));
                } else {
                    float w = Math.max(60, tv.getWidth());
                    float[] hv = new float[3];
                    Color.colorToHSV(base, hv);
                    int c2 = Color.HSVToColor(new float[]{(hv[0] + 140f) % 360f, 1f, 1f});
                    LinearGradient g = new LinearGradient(0, 0, w, 0, new int[]{base, c2, base}, null, Shader.TileMode.MIRROR);
                    Matrix m = new Matrix();
                    m.setTranslate((t * sp / 30L) % (w * 2), 0);
                    g.setLocalMatrix(m);
                    tv.getPaint().setShader(g);
                    tv.setShadowLayer(Math.max(glow * 0.7f, 6), 0, 0, base);
                    tv.setAlpha(al);
                }
                tv.invalidate();
            }
            h.postDelayed(this, any ? 50 : 300);
        }
    };

    // ---------- Sửa chữ nổi ngay trên màn hình ----------
    private void patchText(String key, Object val) {
        try {
            SharedPreferences sp = getSharedPreferences("s", MODE_PRIVATE);
            JSONObject c = new JSONObject(sp.getString("cfg", "{}"));
            JSONObject t = c.optJSONObject("text");
            if (t == null) { t = new JSONObject(); c.put("text", t); }
            t.put(key, val);
            t.put("_t", System.currentTimeMillis());
            sp.edit().putString("cfg", c.toString()).apply();
        } catch (Exception ignored) {}
    }

    private int tInt(String k, int d) { JSONObject t = cfg.optJSONObject("text"); return t == null ? d : t.optInt(k, d); }
    private String tStr(String k, String d) { JSONObject t = cfg.optJSONObject("text"); return t == null ? d : t.optString(k, d); }
    private int acc() { return col(cfg.optString("ac", "#ff3030")); }

    private GradientDrawable shape(int fill, int radiusDp, int stroke, int strokeDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill); g.setCornerRadius(dp(radiusDp));
        if (strokeDp > 0) g.setStroke(dp(strokeDp), stroke);
        return g;
    }

    private TextView label(String s) {
        TextView t = new TextView(this);
        t.setText(s); t.setTextColor(0xFF8D96B0); t.setTextSize(10); t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setLetterSpacing(0.18f);
        t.setPadding(0, dp(12), 0, dp(4));
        return t;
    }

    interface Pick { void on(int i); }

    /** Hàng chip chọn một mục. */
    private View chips(String[] names, int cur, final Pick pk) {
        HorizontalScrollView hs = new HorizontalScrollView(this);
        hs.setHorizontalScrollBarEnabled(false);
        LinearLayout row = new LinearLayout(this);
        final TextView[] cs = new TextView[names.length];
        final int ac = acc();
        for (int i = 0; i < names.length; i++) {
            final int k = i;
            TextView t = new TextView(this);
            t.setText(names[i]); t.setTextSize(12); t.setTypeface(Typeface.DEFAULT_BOLD); t.setGravity(Gravity.CENTER);
            t.setPadding(dp(13), dp(7), dp(13), dp(7));
            LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(-2, -2);
            lp2.setMargins(0, 0, dp(6), 0);
            row.addView(t, lp2);
            cs[i] = t;
            t.setOnClickListener(x -> {
                for (int j = 0; j < cs.length; j++) paintChip(cs[j], j == k, ac);
                pk.on(k);
            });
        }
        for (int j = 0; j < cs.length; j++) paintChip(cs[j], j == cur, ac);
        hs.addView(row);
        return hs;
    }

    private void paintChip(TextView t, boolean on, int ac) {
        t.setBackground(shape(on ? ac : 0x14FFFFFF, 16, on ? ac : 0x28FFFFFF, 1));
        t.setTextColor(on ? 0xFF0A0A10 : 0xFFE9ECF5);
    }

    private View seek(int min, int max, int cur, final String key, final TextView val, final String unit) {
        SeekBar sb = new SeekBar(this);
        sb.setMax(max - min);
        sb.setProgress(Math.max(0, Math.min(max - min, cur - min)));
        int ac = acc();
        sb.setProgressTintList(android.content.res.ColorStateList.valueOf(ac));
        sb.setThumbTintList(android.content.res.ColorStateList.valueOf(ac));
        val.setText((cur) + unit);
        final int mn = min;
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s, int p, boolean u) { val.setText((p + mn) + unit); if (u) patchText(key, p + mn); }
            public void onStartTrackingTouch(SeekBar s) {}
            public void onStopTrackingTouch(SeekBar s) {}
        });
        return sb;
    }

    private LinearLayout seekRow(String name, int min, int max, int cur, String key, String unit) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        LinearLayout head = new LinearLayout(this);
        TextView l = new TextView(this); l.setText(name); l.setTextColor(0xFF8D96B0); l.setTextSize(12);
        TextView vv = new TextView(this); vv.setTextColor(acc()); vv.setTextSize(12); vv.setTypeface(Typeface.DEFAULT_BOLD); vv.setGravity(Gravity.END);
        head.addView(l, new LinearLayout.LayoutParams(0, -2, 1f));
        head.addView(vv, new LinearLayout.LayoutParams(-2, -2));
        box.addView(head);
        box.addView(seek(min, max, cur, key, vv, unit));
        box.setPadding(0, dp(4), 0, 0);
        return box;
    }

    private TextView pillBtn(String s, boolean fill, final Runnable r) {
        TextView t = new TextView(this);
        int ac = acc();
        t.setText(s); t.setTextSize(13); t.setTypeface(Typeface.DEFAULT_BOLD); t.setGravity(Gravity.CENTER);
        t.setTextColor(fill ? 0xFF0A0A10 : 0xFFE9ECF5);
        t.setBackground(shape(fill ? ac : 0x14FFFFFF, 14, fill ? ac : 0x33FFFFFF, 1));
        t.setPadding(dp(10), dp(11), dp(10), dp(11));
        t.setOnClickListener(x -> r.run());
        return t;
    }

    private void openEditor() {
        if (editor != null) return;
        final int ac = acc();
        editor = new LinearLayout(this);
        editor.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{0xF4141826, 0xF40A0C14});
        bg.setCornerRadius(dp(22)); bg.setStroke(dp(1), (ac & 0x00FFFFFF) | 0x99000000);
        editor.setBackground(bg);

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(16), dp(14), dp(16), dp(16));

        // đầu bảng
        LinearLayout head = new LinearLayout(this);
        head.setGravity(Gravity.CENTER_VERTICAL);
        TextView ttl = new TextView(this);
        ttl.setText("✎  CHỮ NỔI"); ttl.setTextColor(ac); ttl.setTextSize(14); ttl.setTypeface(Typeface.DEFAULT_BOLD); ttl.setLetterSpacing(0.15f);
        head.addView(ttl, new LinearLayout.LayoutParams(0, -2, 1f));
        TextView x = new TextView(this);
        x.setText("✕"); x.setTextColor(0xFF8D96B0); x.setTextSize(18); x.setPadding(dp(10), 0, 0, 0);
        x.setOnClickListener(v1 -> closeEditor());
        head.addView(x);
        col.addView(head);

        // nội dung + mã nhanh
        final EditText et = new EditText(this);
        et.setText(tStr("text", ""));
        et.setHint("Gõ chữ muốn hiện"); et.setHintTextColor(0xFF6B7490); et.setTextColor(Color.WHITE); et.setTextSize(15);
        et.setMinWidth(dp(250)); et.setMaxLines(4);
        et.setBackground(shape(0x22000000, 12, 0x22FFFFFF, 1));
        et.setPadding(dp(12), dp(10), dp(12), dp(10));
        et.setSelection(et.getText().length());
        et.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            public void onTextChanged(CharSequence s, int a, int b, int c) {}
            public void afterTextChanged(Editable e) { patchText("text", e.toString()); }
        });
        LinearLayout.LayoutParams elp = new LinearLayout.LayoutParams(-1, -2);
        elp.topMargin = dp(10);
        col.addView(et, elp);
        final String[] tk = {"{time}", "{date}", "{bat}", "{temp}", "{ram}", "{fps}"};
        col.addView(chips(new String[]{"+ Giờ", "+ Ngày", "+ Pin", "+ Nhiệt", "+ RAM", "+ FPS"}, -1, i -> {
            int p = Math.max(0, et.getSelectionStart());
            et.getText().insert(p, tk[i]);
        }));

        col.addView(label("CỠ CHỮ"));
        col.addView(seekRow("Kích thước", 8, 96, tInt("size", 22), "size", ""));

        col.addView(label("MÀU CHỮ"));
        HorizontalScrollView sw = new HorizontalScrollView(this);
        sw.setHorizontalScrollBarEnabled(false);
        LinearLayout dots = new LinearLayout(this);
        for (final String cs : new String[]{"#ffffff", "#ff3030", "#ff8a00", "#ffd60a", "#00ff9d", "#00d8ff", "#3b82f6", "#a855f7", "#ff2d95", "#000000"}) {
            View d = new View(this);
            GradientDrawable g = new GradientDrawable(); g.setShape(GradientDrawable.OVAL); g.setColor(Color.parseColor(cs)); g.setStroke(dp(1), 0x88FFFFFF);
            d.setBackground(g);
            LinearLayout.LayoutParams dl = new LinearLayout.LayoutParams(dp(30), dp(30));
            dl.setMargins(0, 0, dp(9), 0);
            dots.addView(d, dl);
            d.setOnClickListener(v1 -> patchText("color", cs));
        }
        sw.addView(dots);
        col.addView(sw);
        SeekBar hue = new SeekBar(this);
        hue.setMax(360);
        GradientDrawable rb = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{0xFFFF0000, 0xFFFFFF00, 0xFF00FF00, 0xFF00FFFF, 0xFF0000FF, 0xFFFF00FF, 0xFFFF0000});
        rb.setCornerRadius(dp(4)); rb.setSize(0, dp(8));
        hue.setProgressDrawable(rb);
        hue.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s, int p, boolean u) {
                if (u) patchText("color", String.format(java.util.Locale.US, "#%06X", Color.HSVToColor(new float[]{p, 1f, 1f}) & 0xFFFFFF));
            }
            public void onStartTrackingTouch(SeekBar s) {}
            public void onStopTrackingTouch(SeekBar s) {}
        });
        LinearLayout.LayoutParams hl = new LinearLayout.LayoutParams(-1, -2);
        hl.topMargin = dp(8);
        col.addView(hue, hl);

        col.addView(label("HIỆU ỨNG"));
        col.addView(chips(new String[]{"Tắt", "Neon", "Cầu vồng", "Nhịp", "Nháy", "Chuyển màu"}, tInt("fx", 0), i -> patchText("fx", i)));
        col.addView(seekRow("Tốc độ hiệu ứng", 1, 100, tInt("fxs", 50), "fxs", ""));
        col.addView(seekRow("Phát sáng", 0, 40, tInt("glow", 6), "glow", ""));

        col.addView(label("KIỂU CHỮ"));
        col.addView(chips(new String[]{"Mono", "Sans", "Serif", "Gọn", "Đen"},
                java.util.Arrays.asList("mono", "sans", "serif", "cond", "black").indexOf(tStr("font", "mono")),
                i -> patchText("font", new String[]{"mono", "sans", "serif", "cond", "black"}[i])));
        LinearLayout.LayoutParams sp1 = new LinearLayout.LayoutParams(-1, -2);
        sp1.topMargin = dp(8);
        final String[] st = {"Đậm", "Nghiêng", "Nền đen"};
        final String[] stk = {"bold", "italic", "bga"};
        LinearLayout trow = new LinearLayout(this);
        for (int i = 0; i < 3; i++) {
            final int k = i;
            final TextView t = new TextView(this);
            t.setText(st[i]); t.setTextSize(12); t.setTypeface(Typeface.DEFAULT_BOLD); t.setGravity(Gravity.CENTER);
            t.setPadding(dp(13), dp(7), dp(13), dp(7));
            boolean on = k == 2 ? tInt("bga", 100) > 0 : tInt(stk[k], k == 0 ? 1 : 0) == 1;
            paintChip(t, on, ac);
            t.setTag(on);
            t.setOnClickListener(v1 -> {
                boolean n = !(Boolean) t.getTag();
                t.setTag(n); paintChip(t, n, ac);
                patchText(stk[k], k == 2 ? (n ? 100 : 0) : (n ? 1 : 0));
            });
            LinearLayout.LayoutParams l2 = new LinearLayout.LayoutParams(-2, -2);
            l2.setMargins(0, 0, dp(6), 0);
            trow.addView(t, l2);
        }
        col.addView(trow, sp1);
        LinearLayout.LayoutParams al = new LinearLayout.LayoutParams(-1, -2);
        al.topMargin = dp(8);
        col.addView(chips(new String[]{"Căn trái", "Giữa", "Căn phải"}, java.util.Arrays.asList("l", "c", "r").indexOf(tStr("align", "c")),
                i -> patchText("align", new String[]{"l", "c", "r"}[i])), al);

        // nút
        LinearLayout btns = new LinearLayout(this);
        TextView bl = pillBtn("🔒 KHÓA VỊ TRÍ", false, () -> { patchText("lock", 1); closeEditor(); });
        TextView bd = pillBtn("XONG", true, this::closeEditor);
        LinearLayout.LayoutParams b1 = new LinearLayout.LayoutParams(0, -2, 1f), b2 = new LinearLayout.LayoutParams(0, -2, 1f);
        b1.setMargins(0, 0, dp(8), 0);
        btns.addView(bl, b1); btns.addView(bd, b2);
        LinearLayout.LayoutParams bp2 = new LinearLayout.LayoutParams(-1, -2);
        bp2.topMargin = dp(16);
        col.addView(btns, bp2);

        ScrollView scr = new ScrollView(this);
        scr.setVerticalScrollBarEnabled(false);
        scr.addView(col);
        editor.addView(scr);

        android.util.DisplayMetrics dm = getResources().getDisplayMetrics();
        int w = Math.min((int) (dm.widthPixels * 0.94f), dp(360));
        int maxH = (int) (dm.heightPixels * 0.78f);
        editor.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(maxH, View.MeasureSpec.AT_MOST));
        WindowManager.LayoutParams ep = new WindowManager.LayoutParams(
                w, Math.min(maxH, Math.max(editor.getMeasuredHeight(), dp(300))), ovType(),
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT);
        ep.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        ep.y = dp(28);
        ep.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN;
        editor.setAlpha(0f);
        editor.setTranslationY(-dp(14));
        wm.addView(editor, ep);
        editor.animate().alpha(1f).translationY(0).setDuration(170).start();
    }

    private void closeEditor() {
        if (editor != null) {
            try { wm.removeView(editor); } catch (Exception ignored) {}
            editor = null;
        }
    }

    // ---------- Bong bóng ứng dụng ----------
    private void addBubble() {
        final SharedPreferences sp = getSharedPreferences("s", MODE_PRIVATE);
        final int sz = dp(58);
        bubble = new Bub(this);
        bubble.setImageResource(R.drawable.bubble);
        bubble.setPadding(dp(5), dp(5), dp(5), dp(5));
        bubble.setScaleType(ImageView.ScaleType.FIT_CENTER);
        bubble.setAlpha(0.96f);
        bp = new WindowManager.LayoutParams(sz, sz, ovType(), BASE, PixelFormat.TRANSLUCENT);
        bp.gravity = Gravity.TOP | Gravity.START;
        bp.x = sp.getInt("bx", 0); bp.y = sp.getInt("by", dp(160));
        bubble.setOnTouchListener(new View.OnTouchListener() {
            int sx, sy; float tx, ty; boolean moved;
            @Override public boolean onTouch(View view, MotionEvent e) {
                switch (e.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        sx = bp.x; sy = bp.y; tx = e.getRawX(); ty = e.getRawY(); moved = false;
                        view.animate().scaleX(0.88f).scaleY(0.88f).setDuration(90).start();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        float dx = e.getRawX() - tx, dy = e.getRawY() - ty;
                        if (Math.abs(dx) + Math.abs(dy) > dp(8)) moved = true;
                        if (moved) { closePanel(); bp.x = sx + (int) dx; bp.y = sy + (int) dy; wm.updateViewLayout(view, bp); }
                        return true;
                    case MotionEvent.ACTION_CANCEL:
                    case MotionEvent.ACTION_UP:
                        view.animate().scaleX(1f).scaleY(1f).setDuration(140).start();
                        if (e.getAction() == MotionEvent.ACTION_CANCEL) return true;
                        if (!moved) togglePanel();
                        else {
                            int w = getResources().getDisplayMetrics().widthPixels;
                            bp.x = bp.x + sz / 2 < w / 2 ? 0 : w - sz;
                            wm.updateViewLayout(view, bp);
                            sp.edit().putInt("bx", bp.x).putInt("by", bp.y).apply();
                        }
                        return true;
                }
                return false;
            }
        });
        wm.addView(bubble, bp);
    }

    private void removeBubble() {
        closePanel();
        try { wm.removeView(bubble); } catch (Exception ignored) {}
        bubble = null;
    }

    private void closePanel() {
        if (panel != null) {
            try { wm.removeView(panel); } catch (Exception ignored) {}
            panel = null;
        }
    }

    private LinearLayout tile(final int k) {
        LinearLayout t = new LinearLayout(this);
        t.setOrientation(LinearLayout.VERTICAL);
        t.setGravity(Gravity.CENTER_HORIZONTAL);
        t.setPadding(dp(6), dp(12), dp(6), dp(10));
        TextView ic = new TextView(this);
        ic.setText(IC[k]); ic.setTextSize(18); ic.setGravity(Gravity.CENTER); ic.setTypeface(Typeface.DEFAULT_BOLD);
        t.addView(ic, new LinearLayout.LayoutParams(dp(42), dp(42)));
        TextView nm = new TextView(this);
        nm.setText(IT[k]); nm.setTextSize(10.5f); nm.setTextColor(0xFFF4F6FC); nm.setTypeface(Typeface.DEFAULT_BOLD); nm.setGravity(Gravity.CENTER);
        nm.setLetterSpacing(0.06f);
        LinearLayout.LayoutParams nl = new LinearLayout.LayoutParams(-2, -2); nl.topMargin = dp(6);
        t.addView(nm, nl);
        TextView stt = new TextView(this);
        stt.setTextSize(9); stt.setTypeface(Typeface.DEFAULT_BOLD); stt.setLetterSpacing(0.2f); stt.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams sl = new LinearLayout.LayoutParams(-2, -2); sl.topMargin = dp(2);
        t.addView(stt, sl);
        tl[k] = t; ti[k] = ic; ts[k] = stt;
        t.setOnClickListener(x -> act(k));
        return t;
    }

    private void togglePanel() {
        if (panel != null) { closePanel(); return; }
        final int ac = acc();
        panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{0xF6161A28, 0xF60A0C14});
        g.setCornerRadius(dp(24)); g.setStroke(dp(1), (ac & 0x00FFFFFF) | 0x99000000);
        panel.setBackground(g);
        panel.setPadding(dp(12), dp(12), dp(12), dp(12));
        panel.setMinimumWidth(dp(248));

        // đầu: logo + tên + đóng
        LinearLayout head = new LinearLayout(this);
        head.setGravity(Gravity.CENTER_VERTICAL);
        ImageView lg = new ImageView(this);
        lg.setImageResource(R.drawable.bubble);
        head.addView(lg, new LinearLayout.LayoutParams(dp(34), dp(34)));
        LinearLayout tt = new LinearLayout(this);
        tt.setOrientation(LinearLayout.VERTICAL);
        tt.setPadding(dp(10), 0, 0, 0);
        TextView n1 = new TextView(this);
        n1.setText("LONG SUPRA"); n1.setTextColor(Color.WHITE); n1.setTextSize(13); n1.setTypeface(Typeface.DEFAULT_BOLD); n1.setLetterSpacing(0.14f);
        TextView n2 = new TextView(this);
        n2.setText("// MOD LAB"); n2.setTextColor(ac); n2.setTextSize(9); n2.setTypeface(Typeface.DEFAULT_BOLD); n2.setLetterSpacing(0.3f);
        tt.addView(n1); tt.addView(n2);
        head.addView(tt, new LinearLayout.LayoutParams(0, -2, 1f));
        TextView cl = new TextView(this);
        cl.setText("✕"); cl.setTextColor(0xFF8D96B0); cl.setTextSize(16); cl.setPadding(dp(10), dp(4), dp(4), dp(4));
        cl.setOnClickListener(x -> closePanel());
        head.addView(cl);
        panel.addView(head);

        // lưới 2x2
        LinearLayout r1 = new LinearLayout(this), r2 = new LinearLayout(this);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0, -2, 1f);
        tp.setMargins(dp(3), dp(3), dp(3), dp(3));
        r1.addView(tile(0), tp); r1.addView(tile(1), new LinearLayout.LayoutParams(tp));
        r2.addView(tile(2), new LinearLayout.LayoutParams(tp)); r2.addView(tile(3), new LinearLayout.LayoutParams(tp));
        LinearLayout.LayoutParams gl = new LinearLayout.LayoutParams(-1, -2);
        gl.topMargin = dp(10);
        panel.addView(r1, gl);
        panel.addView(r2, new LinearLayout.LayoutParams(-1, -2));

        // chân: mở app + ẩn bong bóng
        LinearLayout foot = new LinearLayout(this);
        TextView open = pillBtn("MỞ APP", true, () -> act(4));
        TextView hide = pillBtn("ẨN BÓNG", false, () -> act(5));
        LinearLayout.LayoutParams f1 = new LinearLayout.LayoutParams(0, -2, 1.2f), f2 = new LinearLayout.LayoutParams(0, -2, 1f);
        f1.setMargins(dp(3), 0, dp(4), 0); f2.setMargins(dp(4), 0, dp(3), 0);
        foot.addView(open, f1); foot.addView(hide, f2);
        LinearLayout.LayoutParams fl = new LinearLayout.LayoutParams(-1, -2);
        fl.topMargin = dp(8);
        panel.addView(foot, fl);
        labels();

        android.util.DisplayMetrics dm = getResources().getDisplayMetrics();
        int w = dm.widthPixels, bsz = dp(58), pw = dp(248), ph = dp(262);
        WindowManager.LayoutParams pp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT, ovType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        pp.gravity = Gravity.TOP | Gravity.START;
        pp.x = bp.x > w / 2 ? Math.max(0, bp.x - pw - dp(8)) : bp.x + bsz + dp(8);
        pp.y = Math.max(0, Math.min(bp.y, dm.heightPixels - ph - dp(24)));
        panel.setOnTouchListener((view, e) -> { if (e.getAction() == MotionEvent.ACTION_OUTSIDE) closePanel(); return false; });
        panel.setAlpha(0f); panel.setScaleX(0.9f); panel.setScaleY(0.9f);
        panel.setPivotX(bp.x > w / 2 ? pw : 0); panel.setPivotY(dp(24));
        wm.addView(panel, pp);
        panel.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(170).start();
    }

    private void labels() {
        if (panel == null) return;
        int ac = acc();
        for (int i = 0; i < 4; i++) {
            boolean on = i < 3 ? v[i] != null : RecService.phase != 0;
            tl[i].setBackground(shape(on ? ((ac & 0x00FFFFFF) | 0x30000000) : 0x12FFFFFF, 16, on ? ac : 0x22FFFFFF, 1));
            ti[i].setBackground(shape(on ? ac : 0x22FFFFFF, 21, 0, 0));
            ti[i].setTextColor(on ? 0xFF0A0A10 : 0xFFB8C0D6);
            ts[i].setText(i == 3 ? (on ? "ĐANG QUAY" : "SẴN SÀNG") : (on ? "ĐANG BẬT" : "TẮT"));
            ts[i].setTextColor(on ? ac : 0xFF6B7490);
        }
    }

    private void act(int k) {
        SharedPreferences sp = getSharedPreferences("s", MODE_PRIVATE);
        if (k < 3) {
            if (v[k] != null) { wm.removeView(v[k]); v[k] = null; } else add(k);
            sp.edit().putBoolean(KEY[k], v[k] != null).apply();
            labels();
        } else if (k == 3) {
            closePanel();
            if (RecService.phase != 0) startService(new Intent(this, RecService.class).setAction("stop"));
            else startActivity(new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP).putExtra("startRec", true));
        } else if (k == 4) {
            closePanel();
            startActivity(new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } else {
            sp.edit().putBoolean("onB", false).apply();
            removeBubble();
            if (v[0] == null && v[1] == null && v[2] == null) stopSelf();
        }
    }

    // ---------- vòng đời ----------
    @Override public IBinder onBind(Intent i) { return null; }

    @Override public void onCreate() {
        super.onCreate();
        up = true;
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        String ch = "fps";
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26)
            nm.createNotificationChannel(new NotificationChannel(ch, "FPS Meter", NotificationManager.IMPORTANCE_LOW));
        Notification.Builder nb = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(this, ch) : new Notification.Builder(this);
        Notification n = nb.setContentTitle("LONG SUPRA // MOD LAB").setContentText("Đang hiển thị overlay")
                .setSmallIcon(android.R.drawable.ic_menu_info_details).build();
        if (Build.VERSION.SDK_INT >= 34) startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        else startForeground(1, n);
        loadCfg();
        prefL = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override public void onSharedPreferenceChanged(SharedPreferences sp, String key) {
                if (!"cfg".equals(key)) return;
                loadCfg();
                for (int i = 0; i < 3; i++) if (v[i] != null) { style(i); reshow(i); }
            }
        };
        getSharedPreferences("s", MODE_PRIVATE).registerOnSharedPreferenceChangeListener(prefL);
        lastBat = 0;
        fpsThread = new Thread(new Runnable() {
            @Override public void run() {
                while (running) {
                    try {
                        JSONObject cr = cfg.optJSONObject("real");
                        boolean need = v[0] != null && cr != null && "game".equals(cr.optString("src", "screen")) && Priv.level() > 0;
                        if (!need) { gFound = false; Thread.sleep(700); continue; }
                        java.util.ArrayList<String> pk = new java.util.ArrayList<>();
                        JSONArray gs = cfg.optJSONArray("gs");
                        if (gs != null) for (int i = 0; i < gs.length(); i++) pk.add(gs.optString(i));
                        float[] s = Priv.sample(pk, cr.optInt("auto", 1) == 1, getPackageName());
                        if (s == null) gFound = false; else { gFound = true; gFps = s[0]; gLow = s[1]; }
                        Thread.sleep(Math.max(250, cr.optInt("interval", 500)));
                    } catch (InterruptedException e) { break; } catch (Exception e) { gFound = false; }
                }
            }
        });
        fpsThread.start();
        h.post(tick);
        h.post(fxr);
    }

    @Override public int onStartCommand(Intent in, int f, int id) {
        if (in != null) {
            for (int i = 0; i < 3; i++) {
                boolean want = in.getBooleanExtra(N[i], v[i] != null);
                if (want && v[i] == null) add(i);
                if (!want && v[i] != null) { wm.removeView(v[i]); v[i] = null; }
            }
            boolean bb = in.getBooleanExtra("bubble", bubble != null);
            if (bb && bubble == null) addBubble();
            if (!bb && bubble != null) removeBubble();
            if (v[0] == null && v[1] == null && v[2] == null && bubble == null) stopSelf();
        }
        return START_NOT_STICKY;
    }

    @Override public void onDestroy() {
        up = false; running = false;
        h.removeCallbacksAndMessages(null);
        if (fpsThread != null) fpsThread.interrupt();
        getSharedPreferences("s", MODE_PRIVATE).unregisterOnSharedPreferenceChangeListener(prefL);
        closeEditor();
        if (bubble != null) removeBubble();
        for (int i = 0; i < 3; i++) if (v[i] != null) wm.removeView(v[i]);
        SharedPreferences.Editor ed = getSharedPreferences("s", MODE_PRIVATE).edit();
        for (String k : KEY) ed.putBoolean(k, false);
        ed.apply();
        Priv.close();
        liveR = -1; liveF = -1;
        super.onDestroy();
    }
}
