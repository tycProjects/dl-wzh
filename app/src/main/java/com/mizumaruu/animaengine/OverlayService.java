package com.mizumaruu.animaengine;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.ImageDecoder;
import android.graphics.drawable.*;
import android.graphics.BlurMaskFilter;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.widget.*;

public class OverlayService extends Service {
 static final String START="anima.start", URI="uri", KIND="kind", MUTED="muted";
 final String CHANNEL="anima_playback";
 WindowManager wm; FrameLayout root; View media; WindowManager.LayoutParams p;
 FrameLayout.LayoutParams mediaParams;
 boolean locked=false, muted=false, controlsVisible=true; float downX,downY; int startX,startY,startW,startH; int minW,minH;
 int mediaWidth, mediaHeight;
 LinearLayout controlPanel; TextView lockBtn, closeBtn;
 public void onCreate(){
  super.onCreate();
  if(Build.VERSION.SDK_INT>=26)((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(new NotificationChannel(CHANNEL,"AnimaEngine playback",NotificationManager.IMPORTANCE_LOW));
  Intent i=new Intent(this,MainActivity.class); PendingIntent pi=PendingIntent.getActivity(this,0,i,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
  Notification n=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,CHANNEL).setContentTitle("AnimaEngine").setContentText("Floating media active").setSmallIcon(android.R.drawable.ic_media_play).setContentIntent(pi).setOngoing(true).build():new Notification.Builder(this).setContentTitle("AnimaEngine").setContentText("Floating media active").setSmallIcon(android.R.drawable.ic_media_play).setContentIntent(pi).build();
  startForeground(19,n);
 }
 public int onStartCommand(Intent i,int f,int id){
  if(i!=null&&START.equals(i.getAction())){String u=i.getStringExtra(URI);if(u!=null){muted=i.getBooleanExtra(MUTED,false);show(Uri.parse(u),"gif".equals(i.getStringExtra(KIND)));}}
  return START_STICKY;
 }
 void show(Uri uri,boolean gif){
  wm=(WindowManager)getSystemService(WINDOW_SERVICE); if(root!=null)try{wm.removeView(root);}catch(Exception ignored){}
  root=new FrameLayout(this); root.setBackgroundColor(Color.TRANSPARENT);
  if(gif&&Build.VERSION.SDK_INT>=28){ImageView iv=new ImageView(this);iv.setBackgroundColor(Color.TRANSPARENT);iv.setScaleType(ImageView.ScaleType.FIT_XY);try{Drawable d=ImageDecoder.decodeDrawable(ImageDecoder.createSource(getContentResolver(),uri));iv.setImageDrawable(d);media=iv;if(d instanceof AnimatedImageDrawable)((AnimatedImageDrawable)d).start();}catch(Exception e){Toast.makeText(this,"Could not decode GIF.",Toast.LENGTH_LONG).show();stopSelf();return;}}
  else if(gif){Toast.makeText(this,"GIF overlay requires Android 9 or newer.",Toast.LENGTH_LONG).show();stopSelf();return;}
  else {VideoView v=new VideoView(this);v.setBackgroundColor(Color.TRANSPARENT);v.setZOrderMediaOverlay(true);v.setVideoURI(uri);v.setOnPreparedListener(mp->{mp.setLooping(true);mp.setVolume(muted?0f:1f,muted?0f:1f);v.start();});media=v;}
  if(media instanceof ImageView)((ImageView)media).setScaleType(ImageView.ScaleType.CENTER_INSIDE);
  mediaWidth=dp(240); mediaHeight=dp(180);
  mediaParams=new FrameLayout.LayoutParams(mediaWidth, mediaHeight);
  mediaParams.gravity=Gravity.CENTER;
  mediaParams.topMargin=dp(50);
  root.addView(media, mediaParams);

  controlPanel=new LinearLayout(this);controlPanel.setGravity(Gravity.CENTER_VERTICAL);controlPanel.setOrientation(LinearLayout.HORIZONTAL);controlPanel.setPadding(dp(10),dp(8),dp(10),dp(8));controlPanel.setBackground(createGlassmorphism(0xCC111111));

  TextView grip=control("⠿", "Drag to move"); lockBtn=control("◇", "Lock position"); closeBtn=control("×", "Close overlay");
  controlPanel.addView(grip,new LinearLayout.LayoutParams(dp(42),dp(34)));
  View gap=new View(this);gap.setLayoutParams(new LinearLayout.LayoutParams(0,dp(34),1));
  controlPanel.addView(gap);
  controlPanel.addView(lockBtn,new LinearLayout.LayoutParams(dp(42),dp(34)));
  controlPanel.addView(closeBtn,new LinearLayout.LayoutParams(dp(42),dp(34)));

  FrameLayout.LayoutParams cp=new FrameLayout.LayoutParams(-1,dp(50),Gravity.TOP);
  root.addView(controlPanel,cp);

  minW=dp(90);minH=dp(70);
  p=new WindowManager.LayoutParams(dp(340),dp(300),Build.VERSION.SDK_INT>=26?WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY:WindowManager.LayoutParams.TYPE_PHONE,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,PixelFormat.TRANSLUCENT);
  p.gravity=Gravity.TOP|Gravity.START;p.x=dp(24);p.y=dp(80);

  View.OnTouchListener moveTouch=(v,e)->{if(locked)return true;switch(e.getAction()){case MotionEvent.ACTION_DOWN:downX=e.getRawX();downY=e.getRawY();startX=p.x;startY=p.y;return true;case MotionEvent.ACTION_MOVE:p.x=startX+(int)(e.getRawX()-downX);p.y=startY+(int)(e.getRawY()-downY);clampPosition();update();return true;default:return true;}};
  grip.setOnTouchListener(moveTouch);

  View.OnTouchListener scaleTouch=(v,e)->{if(locked)return false;switch(e.getAction()){case MotionEvent.ACTION_DOWN:downX=e.getRawX();downY=e.getRawY();startW=mediaWidth;startH=mediaHeight;return true;case MotionEvent.ACTION_MOVE:mediaWidth=Math.max(minW,startW+(int)(e.getRawX()-downX));mediaHeight=Math.max(minH,startH+(int)(e.getRawY()-downY));updateMediaSize();return true;default:return false;}};
  media.setOnTouchListener(scaleTouch);

  View resizeBtn=new TextView(this);((TextView)resizeBtn).setText("◢");((TextView)resizeBtn).setTextColor(Color.WHITE);((TextView)resizeBtn).setTextSize(18);((TextView)resizeBtn).setGravity(Gravity.CENTER);resizeBtn.setContentDescription("Drag to scale");
  android.graphics.drawable.shapes.RoundRectShape rrsr=new android.graphics.drawable.shapes.RoundRectShape(new float[]{dp(12),dp(12),dp(12),dp(12),dp(12),dp(12),dp(12),dp(12)},null,null);
  android.graphics.drawable.ShapeDrawable sdr=new android.graphics.drawable.ShapeDrawable(rrsr);
  sdr.getPaint().setColor(0xFF1a1a1a);
  sdr.getPaint().setAlpha(180);
  resizeBtn.setBackground(sdr);
  FrameLayout.LayoutParams rp=new FrameLayout.LayoutParams(dp(40),dp(40),Gravity.BOTTOM|Gravity.RIGHT);
  rp.rightMargin=dp(5);rp.bottomMargin=dp(5);
  root.addView(resizeBtn,rp);
  resizeBtn.setOnTouchListener((v,e)->{if(locked)return true;switch(e.getAction()){case MotionEvent.ACTION_DOWN:downX=e.getRawX();downY=e.getRawY();startW=mediaWidth;startH=mediaHeight;return true;case MotionEvent.ACTION_MOVE:mediaWidth=Math.max(minW,startW+(int)(e.getRawX()-downX));mediaHeight=Math.max(minH,startH+(int)(e.getRawY()-downY));updateMediaSize();return true;default:return true;}});

  lockBtn.setOnClickListener(v->{locked=!locked;lockBtn.setText(locked?"◆":"◇");lockBtn.setContentDescription(locked?"Unlock position":"Lock position");controlPanel.setVisibility(locked?View.GONE:View.VISIBLE);resizeBtn.setVisibility(locked?View.GONE:View.VISIBLE);});
  closeBtn.setOnClickListener(v->stopSelf());
  try{wm.addView(root,p);clampPosition();update();}catch(Exception e){Toast.makeText(this,"Overlay permission required.",Toast.LENGTH_LONG).show();stopSelf();}
 }
 void updateMediaSize(){
  mediaParams.width=mediaWidth;
  mediaParams.height=mediaHeight;
  root.updateViewLayout(media, mediaParams);
  p.width=Math.max(mediaWidth+dp(20),dp(260));
  p.height=Math.max(mediaHeight+dp(60),dp(230));
  update();
 }

 android.graphics.drawable.ShapeDrawable createGlassmorphism(int color){
  android.graphics.drawable.shapes.RoundRectShape rrs=new android.graphics.drawable.shapes.RoundRectShape(new float[]{dp(18),dp(18),dp(18),dp(18),dp(18),dp(18),dp(18),dp(18)},null,null);
  android.graphics.drawable.ShapeDrawable sd=new android.graphics.drawable.ShapeDrawable(rrs);
  sd.getPaint().setColor(color);
  sd.getPaint().setAlpha(200);
  return sd;
 }

 TextView control(String text,String description){
  TextView t=new TextView(this);t.setText(text);t.setTextColor(Color.WHITE);t.setTextSize(20);t.setGravity(Gravity.CENTER);t.setContentDescription(description);
  android.graphics.drawable.shapes.RoundRectShape rrs=new android.graphics.drawable.shapes.RoundRectShape(new float[]{dp(12),dp(12),dp(12),dp(12),dp(12),dp(12),dp(12),dp(12)},null,null);
  android.graphics.drawable.ShapeDrawable sd=new android.graphics.drawable.ShapeDrawable(rrs);
  sd.getPaint().setColor(0xFF1a1a1a);
  sd.getPaint().setAlpha(180);
  t.setBackground(sd);
  return t;
 }
 void clampSize(){int sw=getResources().getDisplayMetrics().widthPixels,sh=getResources().getDisplayMetrics().heightPixels;p.width=Math.min(p.width,sw);p.height=Math.min(p.height,sh);}
 void clampPosition(){int sw=getResources().getDisplayMetrics().widthPixels,sh=getResources().getDisplayMetrics().heightPixels;p.x=Math.max(0,Math.min(p.x,sw-p.width));p.y=Math.max(0,Math.min(p.y,sh-p.height));}
 void update(){if(wm!=null&&root!=null)try{clampSize();clampPosition();wm.updateViewLayout(root,p);}catch(Exception ignored){}}
 int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
 public void onDestroy(){if(root!=null&&wm!=null)try{wm.removeView(root);}catch(Exception ignored){}root=null;stopForeground(STOP_FOREGROUND_REMOVE);super.onDestroy();}
 public IBinder onBind(Intent i){return null;}
}
