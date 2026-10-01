package com.cozyhabit.app;
import android.app.*;import android.content.*;
public class ReminderReceiver extends BroadcastReceiver {
 @Override public void onReceive(Context c, Intent i){
  String name=i.getStringExtra("name"); NotificationManager nm=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);
  if(android.os.Build.VERSION.SDK_INT>=26){ NotificationChannel ch=new NotificationChannel("habit","Habit reminders",NotificationManager.IMPORTANCE_DEFAULT); nm.createNotificationChannel(ch); }
  Notification.Builder b=android.os.Build.VERSION.SDK_INT>=26?new Notification.Builder(c,"habit"):new Notification.Builder(c);
  b.setSmallIcon(android.R.drawable.ic_popup_reminder).setContentTitle("CozyHabit").setContentText("Time for: "+name).setAutoCancel(true);
  nm.notify((int)(System.currentTimeMillis()%100000),b.build());
 }
}
