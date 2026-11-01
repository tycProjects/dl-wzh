package com.lsupra.fpsmeter;

import android.app.*;
import android.content.*;
import android.content.pm.ServiceInfo;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.util.TypedValue;
import android.view.*;
import android.widget.TextView;
import org.json.JSONObject;

public class OverlayService extends Service {
    static final String[] N = {"real", "fake", "text"}, KEY = {"onR", "onF", "onT"};
    private WindowManager wm;
    private final TextView[] v = new TextView[3];
    private final WindowManager.LayoutParams[] lp = new WindowManager.LayoutParams[3];
    private final Handler h = new Handler(Looper.getMainLooper());
    private final java.util.Random rnd = new java.util.Random();
    private JSONObject cfg = new JSONObject();
    private String lastCfg = "";
    private int frames = 0;
    private float realFps = 60, fCur = 144;
    private long lastR, lastF;
    private final String[] applied = new String[3];
    private final boolean[] bot = new boolean[3], right = new boolean[3], locked = new boolean[3];

    private final Choreographer.FrameCallback cb = new Choreographer.FrameCallback() {
        @Override public void doFrame(long t) { frames++; Choreographer.getInstance().postFrameCallback(this); }
    };

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            long now = SystemClock.uptimeMillis();
            String c = getSharedPreferences("s", MODE_PRIVATE).getString("cfg", "{}");
            if (!c.equals(lastCfg)) {
                loadCfg();
                for (int i = 0; i < 3; i++) if (v[i] != null) style(i);
            }
            if (now - lastR >= 500) { realFps = frames * 1000f / (now - lastR); frames = 0; lastR = now; }
            JSONObject f = cfg.optJSONObject("fake");
            if (now - lastF >= Math.max(50, f == null ? 250 : f.optInt("speed", 250))) { lastF = now; walk(f); }
            if (v[0] != null) v[0].setText(fmt(cfg.optJSONObject("real"), realFps));
            if (v[1] != null) v[1].setText(fmt(f, fCur));
            if (v[2] != null) {
                JSONObject t = cfg.optJSONObject("text");
                String s = t == null ? "LSUPRA" : t.optString("text", "LSUPRA");
                v[2].setText(s.isEmpty() ? " " : s);
            }
            h.postDelayed(this, 100);
        }
    };

    private void loadCfg() {
        lastCfg = getSharedPreferences("s", MODE_PRIVATE).getString("cfg", "{}");
        try { cfg = new JSONObject(lastCfg); } catch (Exception e) { cfg = new JSONObject(); }
    }

    private String posOf(int i) {
        JSONObject o = cfg.optJSONObject(N[i]);
        return o == null ? (i == 2 ? "tl" : "bl") : o.optString("pos", i == 2 ? "tl" : "bl");
    }

    private void setCorner(int i, String pos) {
        bot[i] = pos.charAt(0) == 'b';
        right[i] = pos.length() > 1 && pos.charAt(1) == 'r';
        lp[i].gravity = (bot[i] ? Gravity.BOTTOM : Gravity.TOP) | (right[i] ? Gravity.END : Gravity.START);
    }

    private void walk(JSONObject f) {
        int lo = Math.max(1, f == null ? 138 : f.optInt("min", 138)), hi = f == null ? 144 : f.optInt("max", 144);
        if (hi < lo) { int x = lo; lo = hi; hi = x; }
        float span = hi - lo;
        if (span == 0) fCur = lo;
        else {
            fCur += (rnd.nextFloat() * 2 - 1) * span * 0.4f;
            if (fCur > hi) fCur = hi - rnd.nextFloat() * span * 0.3f;
            if (fCur < lo) fCur = lo + rnd.nextFloat() * span * 0.3f;
        }
    }

    private String fmt(JSONObject c, float val) {
        if (c == null) c = new JSONObject();
        int n = Math.round(val);
        String p = c.optString("prefix", "FPS"), s = c.optString("suffix", "");
        String r = (p.isEmpty() ? "" : p + " ") + n + (s.isEmpty() ? "" : " " + s);
        if (c.optInt("ms", 0) == 1 && val > 0) r += "  " + String.format(java.util.Locale.US, "%.1fms", 1000f / val);
        return r;
    }

    private int dp(int x) { return Math.round(x * getResources().getDisplayMetrics().density); }

    private int col(String s) { try { return Color.parseColor(s); } catch (Exception e) { return Color.WHITE; } }

    private void style(int i) {
        TextView tv = v[i];
        JSONObject c = cfg.optJSONObject(N[i]);
        if (c == null) c = new JSONObject();
        int tc = col(c.optString("color", "#ffffff")), glow = c.optInt("glow", 0);
        tv.setTextColor(tc);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, (float) c.optDouble("size", 13));
        tv.setTypeface(Typeface.MONOSPACE, c.optInt("bold", 1) == 1 ? Typeface.BOLD : Typeface.NORMAL);
        tv.setShadowLayer(glow > 0 ? glow : 0, 0, 0, glow > 0 ? tc : Color.TRANSPARENT);
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(dp(c.optInt("r", 0)));
        int bg = col(c.optString("bg", "#000000"));
        g.setColor(Color.argb(c.optInt("bga", 100) * 255 / 100, Color.red(bg), Color.green(bg), Color.blue(bg)));
        if (c.optInt("bw", 1) > 0) g.setStroke(dp(c.optInt("bw", 1)), col(c.optString("bc", "#555555")));
        tv.setBackground(g);
        tv.setMinWidth(dp(c.optInt("w", 0)));
        tv.setGravity(Gravity.CENTER);
        tv.setIncludeFontPadding(false);
        int py = dp(c.optInt("py", 3));
        tv.setPadding(dp(12), py, dp(12), py);
        tv.setAlpha(c.optInt("alpha", 100) / 100f);
        String pos = posOf(i), tag = pos + ":" + c.optInt("snap", 0);
        boolean lk = c.optInt("lock", 0) == 1;
        locked[i] = lk;
        SharedPreferences sp = getSharedPreferences("s", MODE_PRIVATE);
        if (!tag.equals(applied[i]) && (!lk || applied[i] == null)) {   // đang khóa thì không tự căn lại
            applied[i] = tag;
            int k = 0;
            for (int j = 0; j < i; j++) if (posOf(j).equals(pos)) k++;
            setCorner(i, pos);
            lp[i].x = 0; lp[i].y = k * dp(Math.max(18, (int) c.optDouble("size", 13) + 2 * c.optInt("py", 3) + 8));
            sp.edit().putString("qc" + i, pos).putString("qt" + i, tag)
                    .putInt("qx" + i, lp[i].x).putInt("qy" + i, lp[i].y).apply();
        }
        if (lk) sp.edit().putInt("qx" + i, lp[i].x).putInt("qy" + i, lp[i].y).apply();   // chốt đúng vị trí hiện tại
        int base = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
        lp[i].flags = base | (lk ? WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE : 0);
        wm.updateViewLayout(tv, lp[i]);
    }

    private void add(final int i) {
        final TextView tv = new TextView(this);
        final SharedPreferences sp = getSharedPreferences("s", MODE_PRIVATE);
        final WindowManager.LayoutParams p = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
                Build.VERSION.SDK_INT >= 26 ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                        : WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        if (lastCfg.isEmpty()) loadCfg();
        v[i] = tv; lp[i] = p;
        p.gravity = Gravity.TOP | Gravity.START;
        String pc = sp.getString("qc" + i, null);
        if (pc != null) {                          // khôi phục vị trí đã kéo
            setCorner(i, pc);
            p.x = sp.getInt("qx" + i, 0); p.y = sp.getInt("qy" + i, 0);
            applied[i] = sp.getString("qt" + i, null);
        } else applied[i] = null;
        tv.setOnTouchListener(new View.OnTouchListener() {
            int sx, sy; float tx, ty;
            @Override public boolean onTouch(View view, MotionEvent e) {
                if (locked[i]) return false;
                switch (e.getAction()) {
                    case MotionEvent.ACTION_DOWN: sx = p.x; sy = p.y; tx = e.getRawX(); ty = e.getRawY(); return true;
                    case MotionEvent.ACTION_MOVE:
                        p.x = sx + (right[i] ? -1 : 1) * (int) (e.getRawX() - tx); p.y = sy + (bot[i] ? -1 : 1) * (int) (e.getRawY() - ty);
                        wm.updateViewLayout(view, p); return true;
                    case MotionEvent.ACTION_UP: sp.edit().putInt("qx" + i, p.x).putInt("qy" + i, p.y).apply(); return true;
                }
                return false;
            }
        });
        wm.addView(tv, p);
        style(i);
    }

    @Override public IBinder onBind(Intent i) { return null; }

    @Override public void onCreate() {
        super.onCreate();
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
        lastR = SystemClock.uptimeMillis();
        Choreographer.getInstance().postFrameCallback(cb);
        h.post(tick);
    }

    @Override public int onStartCommand(Intent in, int f, int id) {
        if (in != null) {
            boolean any = false;
            for (int i = 0; i < 3; i++) {
                boolean want = in.getBooleanExtra(N[i], v[i] != null);
                if (want && v[i] == null) add(i);
                if (!want && v[i] != null) { wm.removeView(v[i]); v[i] = null; }
                any |= v[i] != null;
            }
            if (!any) stopSelf();
        }
        return START_NOT_STICKY;
    }

    @Override public void onDestroy() {
        h.removeCallbacksAndMessages(null);
        Choreographer.getInstance().removeFrameCallback(cb);
        for (int i = 0; i < 3; i++) if (v[i] != null) wm.removeView(v[i]);
        SharedPreferences.Editor ed = getSharedPreferences("s", MODE_PRIVATE).edit();
        for (String k : KEY) ed.putBoolean(k, false);
        ed.apply();
        super.onDestroy();
    }
}
