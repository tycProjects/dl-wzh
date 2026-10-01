package com.cozyhabit.app;

import android.app.*;import android.os.*;import android.Manifest;import android.content.*;import android.content.pm.PackageManager;import android.graphics.*;import android.graphics.drawable.*;import android.view.*;import android.widget.*;import java.text.*;import java.util.*;

public class MainActivity extends Activity {
 CozyView view; android.content.SharedPreferences prefs;
 final String[] names={"Bangun tidur","Solat Subuh","Mandi","Sarapan","Pergi sekolah","Balik sekolah","Mandi","Skincare","Solat Zuhur","Makan","Main game","Solat Asar","Workout","Cardio","Mandi","Solat Maghrib","Makan malam","Solat Isyak","Study","Baca novel"};
 final String[] emojis={"🌅","🕌","🚿","🍳","🎒","🏠","🚿","🧴","🕌","🍚","🎮","🕌","🏋️","🏃","🚿","🕌","🍽️","🕌","📚","📖"};
 @Override public void onCreate(Bundle b){super.onCreate(b); prefs=getSharedPreferences("habits",0); view=new CozyView(this); android.widget.ScrollView sc=new android.widget.ScrollView(this); sc.setFillViewport(true); sc.setBackgroundColor(Color.rgb(19,48,36)); sc.addView(view,new android.widget.ScrollView.LayoutParams(-1, (int)(1450*getResources().getDisplayMetrics().density))); setContentView(sc); if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED) requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},50);}
 boolean done(int i){return prefs.getBoolean("d_"+dayKey()+"_"+i,false);} void setDone(int i,boolean v){prefs.edit().putBoolean("d_"+dayKey()+"_"+i,v).apply();}
 String dayKey(){return new SimpleDateFormat("yyyyMMdd",Locale.US).format(new Date());}
 void reminder(final int i){ TimePickerDialog d=new TimePickerDialog(this,(x,h,m)->schedule(names[i],i,h,m),20,0,true); d.setTitle("Reminder • "+names[i]); d.show(); }
 void schedule(String n,int i,int h,int m){ AlarmManager am=(AlarmManager)getSystemService(ALARM_SERVICE); Intent in=new Intent(this,ReminderReceiver.class).putExtra("name",n); PendingIntent pi=PendingIntent.getBroadcast(this,1000+i,in,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE); Calendar c=Calendar.getInstance(); c.set(Calendar.HOUR_OF_DAY,h);c.set(Calendar.MINUTE,m);c.set(Calendar.SECOND,0);c.set(Calendar.MILLISECOND,0);if(c.before(Calendar.getInstance()))c.add(Calendar.DATE,1); if(Build.VERSION.SDK_INT>=23)am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,c.getTimeInMillis(),pi); else am.setExact(AlarmManager.RTC_WAKEUP,c.getTimeInMillis(),pi); Toast.makeText(this,"Reminder set for "+String.format(Locale.US,"%02d:%02d",h,m),Toast.LENGTH_SHORT).show(); }
 class CozyView extends View {
  Paint p=new Paint(3); Random rnd=new Random(4); ArrayList<Drop> drops=new ArrayList<>(); float density; RectF r=new RectF();
  int bg=Color.rgb(19,48,36), panel=Color.rgb(30,65,49), soft=Color.rgb(157,197,165), cream=Color.rgb(235,239,222), muted=Color.rgb(177,199,183);
  CozyView(Context c){super(c); density=getResources().getDisplayMetrics().density; for(int i=0;i<42;i++)drops.add(new Drop(rnd.nextFloat(),rnd.nextFloat(),1.2f+rnd.nextFloat()*2.8f)); setLayerType(View.LAYER_TYPE_SOFTWARE,null);}
  float d(float x){return x*density;} void txt(Canvas c,String s,float x,float y,float size,int col){p.setTypeface(Typeface.create("sans",Typeface.NORMAL));p.setTextSize(d(size));p.setColor(col);c.drawText(s,d(x),d(y),p);}
  @Override protected void onDraw(Canvas c){super.onDraw(c); float W=getWidth(),H=getHeight(); c.drawColor(bg);
   // soft mountain scene at top
   p.setShader(new LinearGradient(0,0,0,d(250),Color.rgb(20,54,42),Color.rgb(44,88,65),Shader.TileMode.CLAMP));c.drawRect(0,0,W,d(270),p);p.setShader(null);
   Path m=new Path();m.moveTo(0,d(205));m.lineTo(d(80),d(125));m.lineTo(d(145),d(188));m.lineTo(d(230),d(95));m.lineTo(d(330),d(190));m.lineTo(W,d(125));m.lineTo(W,d(270));m.lineTo(0,d(270));m.close();p.setColor(Color.rgb(32,70,53));c.drawPath(m,p);
   Path m2=new Path();m2.moveTo(0,d(220));m2.lineTo(d(105),d(160));m2.lineTo(d(175),d(218));m2.lineTo(d(265),d(145));m2.lineTo(d(370),d(225));m2.lineTo(W,d(170));m2.lineTo(W,d(270));m2.lineTo(0,d(270));m2.close();p.setColor(Color.rgb(24,58,44));c.drawPath(m2,p);
   // rain glass
   p.setStrokeWidth(d(1));p.setStrokeCap(Paint.Cap.ROUND);for(Drop q:drops){q.y+=q.v*.0018f;if(q.y>1.05)q.y=-.05f;float x=q.x*W,y=q.y*d(250);p.setColor(Color.argb(85,210,232,218));c.drawLine(x,y,x+d(1.5f),y+d(13*q.v),p);}
   txt(c,"COZYHABIT",20,34,13,soft);txt(c,new SimpleDateFormat("EEEE, d MMM",Locale.US).format(new Date()),20,56,12,muted);txt(c,"Today",20,92,29,cream);
   int count=0;for(int i=0;i<20;i++)if(done(i))count++; int total=21; if(prefs.getBoolean("pmo_"+dayKey(),false))count++; float pct=count/(float)total;
   txt(c,count+" / "+total+" completed",20,118,13,muted);p.setColor(Color.argb(70,235,239,222));r.set(d(20),d(132),W-d(20),d(139));c.drawRoundRect(r,d(4),d(4),p);p.setColor(soft);r.right=d(20)+(W-d(40))*pct;c.drawRoundRect(r,d(4),d(4),p);txt(c,(int)(pct*100)+"%",W/density-55,125,13,cream);
   float y=160;
   for(int i=0;i<20;i++){drawHabit(c,i,y);y+=58; if(y>H/density-100)break;}
   if(y<H/density-90)drawPmo(c,y+4);
   invalidate();
  }
  void rounded(Canvas c,float top,float bottom,int color,float rad){p.setColor(color);r.set(d(16),d(top),getWidth()-d(16),d(bottom));c.drawRoundRect(r,d(rad),d(rad),p);}
  void drawHabit(Canvas c,int i,float y){boolean v=done(i);rounded(c,y,y+49,panel,16);txt(c,emojis[i],28,y+31,20,cream);txt(c,names[i],61,y+21,14,cream);txt(c,v?"Done":"Tap to complete",61,y+39,10,v?soft:muted); // checkbox
   p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(d(2));p.setColor(v?soft:Color.argb(100,235,239,222));r.set(getWidth()-d(62),d(y+11),getWidth()-d(34),d(y+39));c.drawRoundRect(r,d(8),d(8),p);p.setStyle(Paint.Style.FILL);if(v){p.setColor(soft);c.drawRoundRect(r,d(8),d(8),p);txt(c,"✓",getWidth()/density-56,y+32,16,bg);}txt(c,"🔔",getWidth()/density-30,y+31,12,muted);
  }
  void drawPmo(Canvas c,float y){boolean v=prefs.getBoolean("pmo_"+dayKey(),false);rounded(c,y,y+72,Color.rgb(37,78,57),20);txt(c,"🚫",29,y+44,28,cream);txt(c,"NO PMO",69,y+27,17,cream);txt(c,v?"Stayed on track today":"Tap when you make it through today",69,y+49,11,muted);p.setColor(v?soft:Color.argb(80,235,239,222));r.set(getWidth()-d(76),d(y+20),getWidth()-d(30),d(y+57));c.drawRoundRect(r,d(12),d(12),p);txt(c,v?"✓":"GO",getWidth()/density-64,y+45,13,bg);}
  @Override public boolean onTouchEvent(android.view.MotionEvent e){if(e.getAction()!=MotionEvent.ACTION_UP)return true;float x=e.getX()/density,y=e.getY()/density; if(y<150)return true;float yy=160;for(int i=0;i<20;i++){if(y>=yy&&y<=yy+49){if(x>getWidth()/density-45)reminder(i);else setDone(i,!done(i));invalidate();return true;}yy+=58;}if(y>=yy+4&&y<=yy+80){boolean v=prefs.getBoolean("pmo_"+dayKey(),false);prefs.edit().putBoolean("pmo_"+dayKey(),!v).apply();invalidate();}return true;}
  class Drop{float x,y,v;Drop(float a,float b,float c){x=a;y=b;v=c;}}
 }
}
