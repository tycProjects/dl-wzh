package javax.microedition.io.file;
import java.io.*; import java.util.*; import androidport.AndroidBridge;
public class FileConnection implements javax.microedition.io.Connection {
 private final File f; public FileConnection(String url){this.f=AndroidBridge.fileForUrl(url);} public boolean exists(){return f.exists();} public boolean isDirectory(){return f.isDirectory();}
 public Enumeration list(String filter,boolean includeHidden){Vector v=new Vector();File[] a=f.listFiles();if(a!=null)for(File x:a){if(includeHidden||!x.isHidden()){if(filter==null||filter.equals("*")||x.getName().matches(filter.replace(".","\\.").replace("*",".*")))v.add(x.getName()+(x.isDirectory()?"/":""));}}return v.elements();}
 public void create()throws IOException{File p=f.getParentFile();if(p!=null)p.mkdirs();if(!f.exists())f.createNewFile();} public void mkdir(){f.mkdirs();}
 public InputStream openInputStream()throws IOException{return new FileInputStream(f);} public OutputStream openOutputStream()throws IOException{return new FileOutputStream(f);}
 public void truncate(long size)throws IOException{RandomAccessFile r=new RandomAccessFile(f,"rw");r.setLength(size);r.close();} public void close(){}
}
