package com.mizumaruu.animaengine;
import android.app.Activity; import android.content.*; import android.net.Uri; import android.os.Bundle; import android.provider.Settings; import android.webkit.*; import android.widget.Toast;
public class MainActivity extends Activity {
 private static final int PICK=101; private WebView web;
 public void onCreate(Bundle b){super.onCreate(b);web=new WebView(this);web.setBackgroundColor(0);web.getSettings().setJavaScriptEnabled(true);web.getSettings().setDomStorageEnabled(true);web.setWebChromeClient(new WebChromeClient());web.setWebViewClient(new WebViewClient());web.addJavascriptInterface(new Bridge(),"Anima");setContentView(web);web.loadUrl("file:///android_asset/index.html");}
 class Bridge {
  @JavascriptInterface public void pickMedia(){runOnUiThread(()->{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("*/*");i.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"video/mp4","image/gif","video/*"});startActivityForResult(i,PICK);});}
  @JavascriptInterface public void startOverlay(String uri,String kind,boolean muted){runOnUiThread(()->{if(!Settings.canDrawOverlays(MainActivity.this)){startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:"+getPackageName())));Toast.makeText(MainActivity.this,"Allow Display over other apps, then tap Start again.",Toast.LENGTH_LONG).show();return;}Intent s=new Intent(MainActivity.this,OverlayService.class);s.setAction(OverlayService.START);s.putExtra(OverlayService.URI,uri);s.putExtra(OverlayService.KIND,kind);s.putExtra(OverlayService.MUTED,muted);startForegroundService(s);});}
  @JavascriptInterface public void stopOverlay(){stopService(new Intent(MainActivity.this,OverlayService.class));}
  @JavascriptInterface public void openDiscord(){runOnUiThread(()->startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("https://dsc.gg/animaengineport"))));}
 }
 protected void onActivityResult(int r,int c,Intent d){super.onActivityResult(r,c,d);if(r==PICK&&c==RESULT_OK&&d!=null&&d.getData()!=null){Uri u=d.getData();try{getContentResolver().takePersistableUriPermission(u,Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(Exception ignored){}String mime=getContentResolver().getType(u);String k=("image/gif".equals(mime)||u.toString().toLowerCase().endsWith(".gif"))?"gif":"video";web.evaluateJavascript("setPickedMedia("+org.json.JSONObject.quote(u.toString())+","+org.json.JSONObject.quote(k)+")",null);}}
}
