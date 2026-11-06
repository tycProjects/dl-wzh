package com.blackmagic.gamerspace;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

public class BackgroundMonitorService extends Service {
    private static final String CHANNEL = "bm_gamer_background";
    @Override public void onCreate() {
        super.onCreate();
        NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        if(Build.VERSION.SDK_INT>=26){nm.createNotificationChannel(new NotificationChannel(CHANNEL,"BLACKMAGIC em segundo plano",NotificationManager.IMPORTANCE_LOW));}
        Intent open=new Intent(this,MainActivity.class);
        PendingIntent pi=PendingIntent.getActivity(this,0,open,PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT>=23?PendingIntent.FLAG_IMMUTABLE:0));
        Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,CHANNEL):new Notification.Builder(this);
        Notification n=b.setContentTitle("BLACKMAGIC GAMER SPACE ativo").setContentText("Serviço em segundo plano está ligado").setSmallIcon(android.R.drawable.ic_media_play).setContentIntent(pi).setOngoing(true).build();
        startForeground(62,n);
    }
    @Override public int onStartCommand(Intent i,int f,int id){return START_STICKY;}
    @Override public void onDestroy(){stopForeground(true);super.onDestroy();}
    @Override public IBinder onBind(Intent i){return null;}
}
