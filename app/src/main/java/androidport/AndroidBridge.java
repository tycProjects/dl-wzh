package androidport;
import android.content.Context; import java.io.*; import java.net.*;
public final class AndroidBridge { private static Context context; public static void init(Context c){context=c.getApplicationContext();} public static Context context(){return context;} public static InputStream openAsset(String name)throws IOException{return context.getAssets().open(name);}
 public static File appFiles(){File f=context.getExternalFilesDir(null);if(f==null)f=context.getFilesDir();f.mkdirs();return f;} public static File fileForUrl(String url){String x=url;int i=x.indexOf("Sibubalachu/");if(i>=0)x=x.substring(i);else x=x.replaceFirst("^file:(//)?/?","");return new File(appFiles(),x);}
}
