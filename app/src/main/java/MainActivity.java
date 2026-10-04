import android.app.Activity; import android.os.Bundle; import android.os.Build; import android.content.pm.PackageManager; import android.Manifest; import androidport.AndroidBridge;
public class MainActivity extends Activity {
 private MsvMidlet midlet;
 @Override protected void onCreate(Bundle b){super.onCreate(b);AndroidBridge.init(this);androidport.AssetInstaller.install();requestBtPermissions();midlet=new MsvMidlet(this);midlet.startApp();}
 private void requestBtPermissions(){if(Build.VERSION.SDK_INT>=31){String[] p={Manifest.permission.BLUETOOTH_SCAN,Manifest.permission.BLUETOOTH_CONNECT,Manifest.permission.BLUETOOTH_ADVERTISE};requestPermissions(p,50);} }
 @Override protected void onPause(){super.onPause();if(midlet!=null)midlet.pauseApp();}
 @Override protected void onDestroy(){if(midlet!=null)midlet.destroyApp(true);super.onDestroy();}
}
