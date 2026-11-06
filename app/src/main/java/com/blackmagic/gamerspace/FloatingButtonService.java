package com.blackmagic.gamerspace;

import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

public class FloatingButtonService extends Service {
    private WindowManager wm; private View button; private WindowManager.LayoutParams params;
    @Override public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(this)) { stopSelf(); return; }
        wm = (WindowManager)getSystemService(WINDOW_SERVICE);
        TextView b = new TextView(this); b.setText("BM"); b.setTextColor(Color.WHITE); b.setTextSize(16); b.setTypeface(null, Typeface.BOLD); b.setGravity(Gravity.CENTER); b.setBackground(makeBg());
        int type = Build.VERSION.SDK_INT >= 26 ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE;
        params = new WindowManager.LayoutParams(dp(56), dp(56), type, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.RIGHT; params.x = dp(14); params.y = dp(220);
        b.setOnTouchListener(new View.OnTouchListener() {
            float downX, downY; int startX, startY; boolean moved;
            public boolean onTouch(View v, MotionEvent e) {
                if (e.getAction() == MotionEvent.ACTION_DOWN) { downX=e.getRawX(); downY=e.getRawY(); startX=params.x; startY=params.y; moved=false; return true; }
                if (e.getAction() == MotionEvent.ACTION_MOVE) { float dx=e.getRawX()-downX, dy=e.getRawY()-downY; if (Math.abs(dx)+Math.abs(dy)>8) moved=true; params.x=startX-(int)dx; params.y=startY+(int)dy; wm.updateViewLayout(button,params); return true; }
                if (e.getAction() == MotionEvent.ACTION_UP) { if (!moved) { Intent i=new Intent(FloatingButtonService.this,MainActivity.class); i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_SINGLE_TOP); i.putExtra("open_panel",true); startActivity(i); } return true; }
                return true;
            }
        });
        button=b; try { wm.addView(button,params); } catch(Exception e) { stopSelf(); }
    }
    private android.graphics.drawable.GradientDrawable makeBg(){ android.graphics.drawable.GradientDrawable d=new android.graphics.drawable.GradientDrawable(); d.setColor(Color.rgb(125,55,210)); d.setShape(android.graphics.drawable.GradientDrawable.OVAL); d.setStroke(dp(2),Color.rgb(190,140,255)); return d; }
    private int dp(int v){return (int)(v*getResources().getDisplayMetrics().density+0.5f);}
    @Override public int onStartCommand(Intent i,int f,int id){return START_STICKY;}
    @Override public void onDestroy(){if(wm!=null&&button!=null)try{wm.removeView(button);}catch(Exception ignored){} super.onDestroy();}
    @Override public IBinder onBind(Intent i){return null;}
}
