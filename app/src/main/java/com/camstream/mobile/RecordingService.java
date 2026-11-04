package com.camstream.mobile;

import android.app.*;
import android.content.*;
import android.hardware.display.*;
import android.media.MediaRecorder;
import android.media.projection.*;
import android.os.*;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class RecordingService extends Service {
    public static final String ACTION_START="cam.START", ACTION_STOP="cam.STOP";
    private static final int NOTIFY=91;
    private MediaProjection projection;
    private VirtualDisplay display;
    private MediaRecorder recorder;
    private String outputPath;
    @Override public void onCreate(){super.onCreate(); createChannel();}
    @Override public int onStartCommand(Intent intent,int flags,int id){
        if(intent==null)return START_NOT_STICKY;
        if(ACTION_STOP.equals(intent.getAction())){stopCapture();return START_NOT_STICKY;}
        if(ACTION_START.equals(intent.getAction())){
            startForeground(NOTIFY, notification("Preparing screen capture"));
            int code=intent.getIntExtra("resultCode",0); Intent data=intent.getParcelableExtra("data");
            try { begin(code,data); } catch(Exception e){ stopCapture(); stopSelf(); }
        }
        return START_NOT_STICKY;
    }
    private void begin(int resultCode,Intent data)throws Exception{
        if(data==null)throw new IllegalArgumentException("Missing screen-capture consent data");
        projection=((MediaProjectionManager)getSystemService(MEDIA_PROJECTION_SERVICE)).getMediaProjection(resultCode,data);
        DisplayMetrics dm=new DisplayMetrics();
        WindowManager wm=(WindowManager)getSystemService(WINDOW_SERVICE);
        if(Build.VERSION.SDK_INT>=30) dm=getResources().getDisplayMetrics(); else wm.getDefaultDisplay().getRealMetrics(dm);
        int width=dm.widthPixels,height=dm.heightPixels,dpi=dm.densityDpi;
        int bitrate=getSharedPreferences("cam",0).getInt("bitrate",8000000);
        File dir=new File(getExternalFilesDir(android.os.Environment.DIRECTORY_MOVIES),"Recordings");
        if(!dir.exists()&&!dir.mkdirs())throw new java.io.IOException("Cannot create recording folder");
        String stamp=new SimpleDateFormat("yyyyMMdd_HHmmss",Locale.US).format(new Date());
        File out=new File(dir,"CAM_"+stamp+".mp4"); outputPath=out.getAbsolutePath();
        recorder=new MediaRecorder();
        recorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        recorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        recorder.setVideoSize(width,height);
        recorder.setVideoFrameRate(30);
        recorder.setVideoEncodingBitRate(Math.max(2000000,bitrate));
        recorder.setOutputFile(outputPath);
        recorder.prepare();
        display=projection.createVirtualDisplay("CamStreamCapture",width,height,dpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,recorder.getSurface(),null,null);
        recorder.start();
        updateNotification("Recording screen");
    }
    private void stopCapture(){
        try{if(recorder!=null){try{recorder.stop();}catch(RuntimeException ignored){} recorder.reset();recorder.release();recorder=null;}}catch(Exception ignored){}
        if(display!=null){display.release();display=null;}
        if(projection!=null){projection.stop();projection=null;}
        stopForeground(true);stopSelf();
        if(outputPath!=null){ android.content.Intent scan=new android.content.Intent(android.content.Intent.ACTION_MEDIA_SCANNER_SCAN_FILE); scan.setData(android.net.Uri.fromFile(new File(outputPath))); sendBroadcast(scan); }
    }
    private Notification notification(String text){
        Intent stop=new Intent(this,RecordingService.class).setAction(ACTION_STOP);
        PendingIntent pi=PendingIntent.getService(this,2,stop,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        return new Notification.Builder(this,"capture").setContentTitle("CAM STREAM MOBILE").setContentText(text)
            .setSmallIcon(android.R.drawable.presence_video_online).setOngoing(true)
            .addAction(new Notification.Action.Builder(null,"STOP",pi).build()).build();
    }
    private void updateNotification(String s){((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).notify(NOTIFY,notification(s));}
    private void createChannel(){if(Build.VERSION.SDK_INT>=26){NotificationChannel c=new NotificationChannel("capture","Screen recording",NotificationManager.IMPORTANCE_LOW);((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(c);}}
    @Override public void onDestroy(){stopCapture();super.onDestroy();}
    @Override public android.os.IBinder onBind(Intent i){return null;}
}
