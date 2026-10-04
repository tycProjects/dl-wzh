import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.provider.Settings;
import android.app.Activity;
import android.content.pm.PackageManager;
import java.io.*; import java.util.*; import javax.microedition.io.Connection; import androidport.AndroidBridge;

/** Android Bluetooth Classic SPP transport. It keeps the same UUID/protocol as the E72 J2ME build. */
public class BtLink implements Runnable {
 public static final String UUID_STR="A1B2C3D4-E5F6-0718-293A-4B5C6D7E8F90";
 public static final int S_IDLE=0,S_HOSTING=1,S_SEARCHING=2,S_LIST=3,S_CONNECTING=4,S_FAIL=5,S_DONE=6;
 public volatile int state=S_IDLE; public volatile String msg=""; public Vector devices=new Vector(); public Vector names=new Vector();
 private Net net; private int job,pick; private volatile boolean stopFlag; private BluetoothServerSocket server; private BluetoothAdapter adapter; private BroadcastReceiver receiver;
 public BtLink(Net net){this.net=net; adapter=BluetoothAdapter.getDefaultAdapter();}
 public void startHost(){job=1;stopFlag=false;new Thread(this,"BT-host").start();}
 public void startSearch(){job=2;stopFlag=false;new Thread(this,"BT-search").start();}
 public void connectTo(int i){pick=i;job=3;stopFlag=false;new Thread(this,"BT-connect").start();}
 public void backToSearch(){state=S_IDLE;msg="";devices.removeAllElements();names.removeAllElements();}
 public void stop(){stopFlag=true;try{if(server!=null)server.close();}catch(Throwable ignored){}unregister();state=S_IDLE;}
 public void run(){try{if(adapter==null)throw new IOException("May khong co Bluetooth");if(!adapter.isEnabled())throw new IOException("Hay bat Bluetooth");if(job==1)doHost();else if(job==2)doSearch();else if(job==3)doConnect();}catch(Throwable t){if(!stopFlag){state=S_FAIL;msg="Loi Bluetooth: "+t.getMessage();}}}
 private void ensureDiscoverable(){try{if(AndroidBridge.context() instanceof Activity){Intent i=new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);i.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION,300);((Activity)AndroidBridge.context()).startActivity(i);}}catch(Throwable ignored){}}
 private void doHost() throws Exception{state=S_HOSTING;msg="Dang tao phong Bluetooth...";ensureDiscoverable();server=adapter.listenUsingRfcommWithServiceRecord("MsvGame",java.util.UUID.fromString(UUID_STR));msg="Cho nguoi choi vao...";while(!stopFlag){if(net.peerCount()>=Net.MAXP-1){Thread.sleep(300);continue;}BluetoothSocket s=server.accept();if(s!=null){if(net.addPeer(s.getInputStream(),s.getOutputStream(),new BtConnection(s)))msg="Da co "+net.peerCount()+" nguoi vao";else try{s.close();}catch(Exception ignored){}}}}
 private void doSearch() throws Exception{state=S_SEARCHING;msg="Dang tim may khac...";devices.removeAllElements();names.removeAllElements();register();if(Build.VERSION.SDK_INT>=18)adapter.startDiscovery();for(BluetoothDevice d:adapter.getBondedDevices())add(d);long end=System.currentTimeMillis()+15000;while(!stopFlag&&System.currentTimeMillis()<end)Thread.sleep(200);try{adapter.cancelDiscovery();}catch(Throwable ignored){}unregister();state=S_LIST;msg="Tim thay "+devices.size()+" may";}
 private void register(){if(receiver!=null)return;receiver=new BroadcastReceiver(){public void onReceive(Context c,Intent i){String a=i.getAction();if(BluetoothDevice.ACTION_FOUND.equals(a)){BluetoothDevice d=i.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);if(d!=null)add(d);}else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(a)){}}};IntentFilter f=new IntentFilter();f.addAction(BluetoothDevice.ACTION_FOUND);f.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);if(Build.VERSION.SDK_INT>=33)AndroidBridge.context().registerReceiver(receiver,f,Context.RECEIVER_EXPORTED);else AndroidBridge.context().registerReceiver(receiver,f);}
 private void unregister(){if(receiver!=null){try{AndroidBridge.context().unregisterReceiver(receiver);}catch(Throwable ignored){}receiver=null;}}
 private synchronized void add(BluetoothDevice d){for(int i=0;i<devices.size();i++)if(((BluetoothDevice)devices.elementAt(i)).getAddress().equals(d.getAddress()))return;devices.addElement(d);String n=d.getName();if(n==null||n.length()==0)n=d.getAddress();names.addElement(n);}
 private void doConnect() throws Exception{state=S_CONNECTING;msg="Dang ket noi...";if(pick<0||pick>=devices.size())throw new IOException("May khong ton tai");BluetoothDevice d=(BluetoothDevice)devices.elementAt(pick);try{adapter.cancelDiscovery();}catch(Throwable ignored){}BluetoothSocket s=d.createRfcommSocketToServiceRecord(java.util.UUID.fromString(UUID_STR));s.connect();boolean ok=net.addPeer(s.getInputStream(),s.getOutputStream(),new BtConnection(s));state=ok?S_DONE:S_FAIL;msg=ok?"Da noi, cho host...":"Phong day hoac loi";}
 static class BtConnection implements Connection{final BluetoothSocket s;BtConnection(BluetoothSocket s){this.s=s;}public void close()throws IOException{s.close();}}
}
