package androidport;
import java.io.*; public final class AssetInstaller { private AssetInstaller(){} public static void install(){try{File root=new File(AndroidBridge.appFiles(),"Sibubalachu");copyTree("Sibubalachu",root); }catch(Throwable ignored){}}
 private static void copyTree(String asset,File out)throws Exception{out.mkdirs();String[] kids=AndroidBridge.context().getAssets().list(asset);if(kids==null)return;for(String k:kids){String a=asset+"/"+k;File f=new File(out,k);String[] sub=AndroidBridge.context().getAssets().list(a);if(sub!=null&&sub.length>0){copyTree(a,f);}else{InputStream in=AndroidBridge.context().getAssets().open(a);OutputStream o=new FileOutputStream(f);byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1)o.write(b,0,n);in.close();o.close();}}}
}
