package com.camstream.mobile

import android.app.*
import android.content.ContentValues
import android.content.Intent
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import java.text.SimpleDateFormat
import java.util.*

class ScreenRecordService:Service() {
    companion object {
        const val ACTION_START="cam.start", ACTION_STOP="cam.stop", ACTION_PAUSE="cam.pause", ACTION_STATE="cam.state", ACTION_FINISHED="cam.finished"
        private const val CHANNEL="cam_recording"
    }
    private var recorder:MediaRecorder?=null
    private var projection:MediaProjection?=null
    private var display:VirtualDisplay?=null
    private var outputUri:android.net.Uri?=null
    private var paused=false
    private val projectionCallback = object : MediaProjection.Callback() { override fun onStop() { /* system ended capture */ } }
    override fun onBind(intent:Intent?):IBinder?=null
    override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int {
        when(intent?.action) {
            ACTION_START -> startCapture(intent)
            ACTION_STOP -> stopCapture()
            ACTION_PAUSE -> togglePause()
        }
        return START_NOT_STICKY
    }
    private fun startCapture(i:Intent) {
        createChannel()
        startForeground(41,notification("Preparing screen recording"))
        try {
            val code=i.getIntExtra("resultCode",Activity.RESULT_CANCELED)
            val data=if(Build.VERSION.SDK_INT>=33)i.getParcelableExtra("data",Intent::class.java) else @Suppress("DEPRECATION") i.getParcelableExtra("data")
            require(data!=null){"Screen capture permission data missing"}
            val mgr=getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            projection=mgr.getMediaProjection(code,data)
            projection!!.registerCallback(projectionCallback, android.os.Handler(mainLooper))
            val wm=getSystemService(WINDOW_SERVICE) as WindowManager
            val metrics=DisplayMetrics()
            @Suppress("DEPRECATION") wm.defaultDisplay.getRealMetrics(metrics)
            val target=i.getStringExtra("resolution")?:"1080p"
            val scale=when(target){"480p"->.5f;"720p"->.67f;"1080p"->1f;"1440p"->1.33f;else->1f}
            val width=((metrics.widthPixels*scale).toInt()/2)*2
            val height=((metrics.heightPixels*scale).toInt()/2)*2
            val fps=i.getIntExtra("fps",30).coerceIn(15,60)
            val values=ContentValues().apply { put(MediaStore.Video.Media.DISPLAY_NAME,"CAM_${SimpleDateFormat("yyyyMMdd_HHmmss",Locale.US).format(Date())}.mp4");put(MediaStore.Video.Media.MIME_TYPE,"video/mp4");if(Build.VERSION.SDK_INT>=29)put(MediaStore.Video.Media.RELATIVE_PATH,Environment.DIRECTORY_MOVIES+"/CamStreamMobile/") }
            outputUri=contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,values)
            val fd=contentResolver.openFileDescriptor(outputUri!!,"w")!!.fileDescriptor
            val r=if(Build.VERSION.SDK_INT>=31)MediaRecorder(this) else @Suppress("DEPRECATION") MediaRecorder()
            recorder=r
            val sound=i.getStringExtra("sound")?:"Mic"
            if(sound=="Mic"||sound=="Both")r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setVideoSource(MediaRecorder.VideoSource.SURFACE)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            if(sound=="Mic"||sound=="Both")r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setVideoSize(width,height)
            r.setVideoFrameRate(fps)
            val bitrate=when(i.getStringExtra("quality")){"Standard"->4_000_000;"Ultra"->12_000_000;else->8_000_000}
            r.setVideoEncodingBitRate(bitrate)
            r.setOutputFile(fd)
            r.prepare()
            display=projection!!.createVirtualDisplay("CamStreamCapture",width,height,metrics.densityDpi,DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,r.surface,null,null)
            r.start()
            updateState(true,false)
            getSystemService(NotificationManager::class.java).notify(41,notification("Recording screen"))
        } catch(e:Exception) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopCapture()
            android.util.Log.e("CamStream","Recording failed",e)
            sendBroadcast(Intent(ACTION_FINISHED).setPackage(packageName))
            stopSelf()
        }
    }
    private fun togglePause() {
        val r=recorder?:return
        try {
            if(Build.VERSION.SDK_INT>=24) { if(paused)r.resume() else r.pause();paused=!paused;updateState(true,paused);getSystemService(NotificationManager::class.java).notify(41,notification(if(paused)"Recording paused" else "Recording screen")) }
        } catch(e:Exception){android.util.Log.e("CamStream","Pause/resume failed",e)}
    }
    private fun stopCapture() {
        try { recorder?.stop() } catch(_:Exception) {}
        runCatching { display?.release() };display=null
        runCatching { recorder?.reset();recorder?.release() };recorder=null
        runCatching { projection?.stop() };projection=null
        if(outputUri!=null) sendBroadcast(Intent(ACTION_FINISHED).setPackage(packageName))
        outputUri=null;paused=false
        updateState(false,false)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }
    private fun updateState(active:Boolean,p:Boolean){sendBroadcast(Intent(ACTION_STATE).setPackage(packageName).putExtra("recording",active).putExtra("paused",p))}
    private fun createChannel(){if(Build.VERSION.SDK_INT>=26){val c=NotificationChannel(CHANNEL,"Screen recording",NotificationManager.IMPORTANCE_LOW);getSystemService(NotificationManager::class.java).createNotificationChannel(c)}}
    private fun notification(msg:String):Notification {
        val stop=PendingIntent.getService(this,2,Intent(this,ScreenRecordService::class.java).setAction(ACTION_STOP),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val pause=PendingIntent.getService(this,3,Intent(this,ScreenRecordService::class.java).setAction(ACTION_PAUSE),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this,CHANNEL).setSmallIcon(android.R.drawable.presence_video_online).setContentTitle("CAM STREAM MOBILE").setContentText(msg).setOngoing(true).addAction(0,"Pause / Resume",pause).addAction(0,"Stop",stop).build()
    }
}
