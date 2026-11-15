package com.blackmagic.ps5pro;

import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.view.*;
import android.widget.*;

public class OverlayService extends Service {
    WindowManager wm; View panel; WindowManager.LayoutParams params;
    @Override public void onCreate() {
        super.onCreate(); wm=(WindowManager)getSystemService(WINDOW_SERVICE);
        LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(8,6,8,6);
        box.setBackgroundColor(0xDD15151D);
        TextView head=new TextView(this); head.setText("BLACKMAGIC  ⠿"); head.setTextColor(Color.WHITE); head.setTextSize(12); box.addView(head);
        LinearLayout row=new LinearLayout(this);
        String[] buttons={"L1","△","○","□","×","R1","✕"};
        for(String label:buttons){ Button b=new Button(this); b.setText(label); b.setTextSize(10); b.setMinWidth(42); b.setOnClickListener(v->GameTouchAccessibilityService.tapScreenCenter()); row.addView(b); }
        box.addView(row);
        TextView hint=new TextView(this); hint.setText("Arraste pelo título • toque X para fechar"); hint.setTextColor(Color.LTGRAY); hint.setTextSize(9); box.addView(hint);
        head.setOnTouchListener(new View.OnTouchListener(){float x,y; public boolean onTouch(View v, android.view.MotionEvent e){
            if(e.getAction()==MotionEvent.ACTION_DOWN){x=e.getRawX();y=e.getRawY();return true;}
            if(e.getAction()==MotionEvent.ACTION_MOVE){params.x+=(int)(e.getRawX()-x);params.y+=(int)(e.getRawY()-y);x=e.getRawX();y=e.getRawY();wm.updateViewLayout(panel,params);return true;} return true;
        }});
        head.setOnClickListener(v->{});
        Button close=new Button(this); close.setText("FECHAR CONTROLE"); close.setTextSize(9); close.setOnClickListener(v->stopSelf()); box.addView(close);
        panel=box;
        int type= android.os.Build.VERSION.SDK_INT>=26?WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY:WindowManager.LayoutParams.TYPE_PHONE;
        params=new WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT,WindowManager.LayoutParams.WRAP_CONTENT,type,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,PixelFormat.TRANSLUCENT);
        params.gravity=Gravity.TOP|Gravity.START; params.x=20; params.y=180;
        wm.addView(panel,params);
    }
    @Override public void onDestroy(){if(panel!=null)wm.removeView(panel);super.onDestroy();}
    @Override public IBinder onBind(Intent i){return null;}
}
