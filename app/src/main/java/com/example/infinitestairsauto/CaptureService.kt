package com.example.infinitestairsauto

import android.app.*
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.*
import kotlin.math.abs

class CaptureService : Service() {
    private var projection: MediaProjection?=null
    private var reader: ImageReader?=null
    private var display: VirtualDisplay?=null
    private val handler=Handler(Looper.getMainLooper())
    private var busy=false

    override fun onBind(intent:Intent?)=null
    override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int {
        createNotification()
        val code=intent?.getIntExtra("resultCode",0) ?: 0
        val data=intent?.getParcelableExtra<Intent>("data")
        if(data!=null) {
            val mgr=getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            projection=mgr.getMediaProjection(code,data)
            val dm=resources.displayMetrics
            reader=ImageReader.newInstance(dm.widthPixels,dm.heightPixels,PixelFormat.RGBA_8888,2)
            display=projection?.createVirtualDisplay("StairsVision",dm.widthPixels,dm.heightPixels,dm.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,reader?.surface,null,handler)
            handler.post(scanLoop)
        }
        return START_NOT_STICKY
    }
    private fun createNotification() {
        val channel="stairs_vision"
        if(Build.VERSION.SDK_INT>=26) {
            val nm=getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(NotificationChannel(channel,"Stairs screen analysis",NotificationManager.IMPORTANCE_LOW))
        }
        val n=Notification.Builder(this,channel).setContentTitle("Infinite Stairs AUTO")
            .setContentText("화면을 분석 중입니다").setSmallIcon(android.R.drawable.ic_menu_search).build()
        startForeground(41,n)
    }
    private val scanLoop=object:Runnable {
        override fun run() {
            if(!getSharedPreferences("vision",MODE_PRIVATE).getBoolean("running",false)) { stopSelf(); return }
            if(!busy) {
                val image=reader?.acquireLatestImage()
                if(image!=null) {
                    busy=true
                    try {
                        val direction=analyze(image)
                        getSharedPreferences("vision",MODE_PRIVATE).edit().putString("direction",direction).putLong("updated",System.currentTimeMillis()).apply()
                    } finally { image.close(); busy=false }
                }
            }
            handler.postDelayed(this,180)
        }
    }
    // Color-based tile detector for the orange pumpkin stair skin seen in the supplied gameplay clip.
    private fun analyze(img:Image):String {
        val plane=img.planes[0]; val buf=plane.buffer
        val w=img.width; val h=img.height; val pixelStride=plane.pixelStride; val rowStride=plane.rowStride
        val cx=w/2
        // Count orange tile pixels in left/right forward regions, excluding HUD and bottom decorations.
        var left=0; var right=0
        val y0=(h*0.39).toInt(); val y1=(h*0.70).toInt()
        val xL0=(w*0.12).toInt(); val xL1=(w*0.48).toInt()
        val xR0=(w*0.52).toInt(); val xR1=(w*0.88).toInt()
        for (y in y0 until y1 step 4) {
            for (x in xL0 until xL1 step 4) {
                if (isOrange(buf, rowStride, pixelStride, x, y)) left++
            }
            for (x in xR0 until xR1 step 4) {
                if (isOrange(buf, rowStride, pixelStride, x, y)) right++
            }
        }
        // The target is the side with the stronger connected stair-color signal.
        // Low confidence is explicitly reported as HOLD to avoid blind repeated tapping.
        val total=left+right
        if(total<35 || abs(left-right)<maxOf(12,total/12)) return "HOLD"
        return if(left>right) "LEFT" else "RIGHT"
    }
    private fun isOrange(buf:java.nio.ByteBuffer,rowStride:Int,pixelStride:Int,x:Int,y:Int):Boolean {
        val i=y*rowStride+x*pixelStride
        if(i+2>=buf.limit()) return false
        val r=buf.get(i).toInt() and 255
        val g=buf.get(i+1).toInt() and 255
        val b=buf.get(i+2).toInt() and 255
        return r>145 && g in 45..175 && b<105 && r>g*1.25
    }
    override fun onDestroy() {
        handler.removeCallbacks(scanLoop); display?.release(); reader?.close(); projection?.stop()
        super.onDestroy()
    }
}
