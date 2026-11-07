package com.example.infinitestairsauto

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent

class TapService:AccessibilityService() {
    private val handler=Handler(Looper.getMainLooper())
    private var last=0L
    override fun onServiceConnected(){super.onServiceConnected();handler.post(loop)}
    override fun onAccessibilityEvent(event:AccessibilityEvent?){}
    override fun onInterrupt(){}
    private val loop=object:Runnable {
        override fun run(){
            val p=getSharedPreferences("vision",MODE_PRIVATE)
            if(p.getBoolean("running",false)){
                val now=System.currentTimeMillis()
                val dir=p.getString("direction","HOLD") ?: "HOLD"
                val fresh=now-p.getLong("updated",0L)<900
                if(fresh && dir!="HOLD" && now-last>520){
                    // Game controls from supplied screenshot: turn button bottom-left, climb button bottom-right.
                    if(dir=="LEFT") tap(.18f,.84f)
                    handler.postDelayed({tap(.80f,.84f)},if(dir=="LEFT")180 else 0)
                    last=now
                }
            }
            handler.postDelayed(this,100)
        }
    }
    private fun tap(xp:Float,yp:Float){
        val m=resources.displayMetrics
        val path=Path().apply{moveTo(m.widthPixels*xp,m.heightPixels*yp)}
        val g=GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(path,0,55)).build()
        dispatchGesture(g,null,null)
    }
}
