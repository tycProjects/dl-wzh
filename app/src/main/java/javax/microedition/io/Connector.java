package javax.microedition.io;
import java.net.*; import java.io.*;
public final class Connector { public static final int READ=1, WRITE=2, READ_WRITE=3; private Connector(){}
 public static Connection open(String url) throws IOException { if(url.startsWith("socket://")){String x=url.substring(9);int p=x.lastIndexOf(':');return new SocketConnectionImpl(new Socket(x.substring(0,p),Integer.parseInt(x.substring(p+1))));} throw new IOException("Unsupported connector: "+url); }
 static class SocketConnectionImpl implements StreamConnection {final Socket s;SocketConnectionImpl(Socket s){this.s=s;}public InputStream openInputStream()throws IOException{return s.getInputStream();}public OutputStream openOutputStream()throws IOException{return s.getOutputStream();}public void close()throws IOException{s.close();}}
}
