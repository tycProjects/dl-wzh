package com.blackmagic.ps5pro;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;

public class GameTouchAccessibilityService extends AccessibilityService {
    private static GameTouchAccessibilityService instance;
    @Override public void onServiceConnected(){super.onServiceConnected();instance=this;}
    @Override public void onAccessibilityEvent(AccessibilityEvent event){}
    @Override public void onInterrupt(){}
    @Override public void onDestroy(){instance=null;super.onDestroy();}
    public static void tapScreenCenter(){
        if(instance==null) return;
        android.util.DisplayMetrics dm=instance.getResources().getDisplayMetrics();
        Path p=new Path(); p.moveTo(dm.widthPixels*0.5f,dm.heightPixels*0.75f);
        GestureDescription.StrokeDescription stroke=new GestureDescription.StrokeDescription(p,0,80);
        instance.dispatchGesture(new GestureDescription.Builder().addStroke(stroke).build(),null,null);
    }
}
