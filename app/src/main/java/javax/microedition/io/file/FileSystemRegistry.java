package javax.microedition.io.file;
import java.util.*; import androidport.AndroidBridge;
public final class FileSystemRegistry { private FileSystemRegistry(){} public static Enumeration listRoots(){Vector v=new Vector();v.add("/");return v.elements();} }
