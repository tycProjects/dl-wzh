package javax.microedition.lcdui;
import android.graphics.Bitmap; import android.graphics.BitmapFactory; import android.graphics.Canvas; import android.graphics.drawable.Drawable; import androidport.AndroidBridge; import java.io.InputStream;
public class Image {
 private final Bitmap bitmap; private Image(Bitmap b){bitmap=b;}
 public static Image createImage(String path) throws Exception { String p=path.startsWith("/")?path.substring(1):path; InputStream in=AndroidBridge.openAsset(p); Bitmap b=BitmapFactory.decodeStream(in); in.close(); if(b==null) throw new Exception("Image not found: "+path); return new Image(b); }
 public static Image createImage(int w,int h){return new Image(Bitmap.createBitmap(Math.max(1,w),Math.max(1,h),Bitmap.Config.ARGB_8888));}
 public Graphics getGraphics(){return new Graphics(new Canvas(bitmap));}
 public Bitmap bitmap(){return bitmap;} public int getWidth(){return bitmap.getWidth();} public int getHeight(){return bitmap.getHeight();}
}
