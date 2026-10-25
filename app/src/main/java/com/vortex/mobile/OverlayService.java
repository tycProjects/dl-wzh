package com.vortex.mobile;

import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.os.IBinder;
import android.view.Gravity;
import android.view.WindowManager;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

public class OverlayService extends Service {
  WindowManager wm; View bubble; View panel;
  @Override public IBinder onBind(Intent i){ return null; }
  @Override public void onCreate(){ super.onCreate(); wm=(WindowManager)getSystemService(WINDOW_SERVICE); showBubble(); }
  int type(){ return android.os.Build.VERSION.SDK_INT>=26 ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE; }
  WindowManager.LayoutParams params(int w,int h,int gravity){ return new WindowManager.LayoutParams(w,h,type(),WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,PixelFormat.TRANSLUCENT); }
  void showBubble(){
    TextView v=new TextView(this); v.setText("V"); v.setTextSize(22); v.setTypeface(null,Typeface.BOLD); v.setTextColor(Color.WHITE); v.setGravity(Gravity.CENTER); v.setBackgroundColor(Color.rgb(125,55,205));
    WindowManager.LayoutParams p=params(120,120,Gravity.TOP|Gravity.START); p.x=220; p.y=350; bubble=v; v.setOnClickListener(x->togglePanel());
    v.setOnTouchListener(new View.OnTouchListener(){float sx,sy; int ox,oy; public boolean onTouch(View view, android.view.MotionEvent e){ if(e.getAction()==0){sx=e.getRawX();sy=e.getRawY();ox=p.x;oy=p.y;return true;} if(e.getAction()==2){p.x=ox+(int)(e.getRawX()-sx);p.y=oy+(int)(e.getRawY()-sy);wm.updateViewLayout(v,p);return true;} if(e.getAction()==1){ if(Math.abs(e.getRawX()-sx)<10 && Math.abs(e.getRawY()-sy)<10) togglePanel(); return true;} return false; }});
    wm.addView(v,p);
  }
  void togglePanel(){ if(panel!=null){wm.removeView(panel);panel=null;return;} LinearLayout box=new LinearLayout(this); box.setOrientation(1); box.setPadding(24,20,24,24); box.setBackgroundColor(Color.rgb(16,12,28));
    LinearLayout head=new LinearLayout(this); head.setGravity(Gravity.CENTER_VERTICAL); TextView title=new TextView(this); title.setText("VORTEX MENU");title.setTextColor(Color.rgb(190,120,255));title.setTextSize(20);head.addView(title,new LinearLayout.LayoutParams(0,-2,1));TextView close=new TextView(this);close.setText("✕");close.setTextSize(22);close.setTextColor(Color.WHITE);close.setPadding(20,0,0,0);head.addView(close);close.setOnClickListener(v->{wm.removeView(panel);panel=null;});box.addView(head);
    String[] items={"Scripts — local notes only","Files — import support planned","Settings — overlay enabled","No Roblox script injection"}; for(String s:items){TextView t=new TextView(this);t.setText(s);t.setTextSize(16);t.setTextColor(Color.WHITE);t.setPadding(0,22,0,10);box.addView(t);}
    WindowManager.LayoutParams p=params(700,WindowManager.LayoutParams.WRAP_CONTENT,Gravity.CENTER);panel=box;wm.addView(panel,p);
  }
  @Override public void onDestroy(){ if(bubble!=null)wm.removeView(bubble);if(panel!=null)wm.removeView(panel);super.onDestroy(); }
}
