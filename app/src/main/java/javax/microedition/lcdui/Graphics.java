package javax.microedition.lcdui;
import android.graphics.Canvas; import android.graphics.Paint; import android.graphics.Path;
public class Graphics {
 public static final int TOP=1, BOTTOM=2, LEFT=4, RIGHT=8, HCENTER=16, VCENTER=32;
 private final Canvas c; private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG); private Font font=Font.getFont(0,0,Font.SIZE_SMALL);
 public Graphics(Canvas c){this.c=c;} public void setColor(int rgb){p.setColor(0xFF000000|(rgb&0xFFFFFF));} public void setFont(Font f){font=f; p.setTextSize(f.size()); p.setTypeface(android.graphics.Typeface.DEFAULT);}
 public void fillRect(int x,int y,int w,int h){c.drawRect(x,y,x+w,y+h,p);} public void drawRect(int x,int y,int w,int h){c.drawRect(x,y,x+w,y+h,p);}
 public void drawLine(int x1,int y1,int x2,int y2){c.drawLine(x1,y1,x2,y2,p);} public void fillArc(int x,int y,int w,int h,int start,int sweep){c.drawArc(x,y,x+w,y+h,start,sweep,true,p);}
 public void fillTriangle(int x1,int y1,int x2,int y2,int x3,int y3){Path q=new Path();q.moveTo(x1,y1);q.lineTo(x2,y2);q.lineTo(x3,y3);q.close();c.drawPath(q,p);}
 public void drawString(String s,int x,int y,int anchor){float xx=x; float yy=y; if((anchor&HCENTER)!=0) xx-=p.measureText(s)/2f; else if((anchor&RIGHT)!=0) xx-=p.measureText(s); Paint.FontMetrics fm=p.getFontMetrics(); if((anchor&VCENTER)!=0) yy-= (fm.ascent+fm.descent)/2f; else if((anchor&TOP)!=0) yy-=fm.ascent; else if((anchor&BOTTOM)!=0) yy-=fm.descent; c.drawText(s,xx,yy,p);}
 public void drawImage(Image im,int x,int y,int anchor){float xx=x,yy=y; if((anchor&HCENTER)!=0)xx-=im.getWidth()/2f; else if((anchor&RIGHT)!=0)xx-=im.getWidth(); if((anchor&VCENTER)!=0)yy-=im.getHeight()/2f; else if((anchor&BOTTOM)!=0)yy-=im.getHeight(); else if((anchor&TOP)!=0){} c.drawBitmap(im.bitmap(),xx,yy,p);}
}
