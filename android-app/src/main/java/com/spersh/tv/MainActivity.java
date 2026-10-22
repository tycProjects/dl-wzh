package com.spersh.tv;
import android.annotation.SuppressLint;
import android.content.*;
import android.os.Bundle;
import android.webkit.*;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {
  WebView web;
  @SuppressLint({"SetJavaScriptEnabled","JavascriptInterface"})
  public void onCreate(Bundle b){
    super.onCreate(b); setContentView(R.layout.activity_main);
    web=findViewById(R.id.web);
    WebSettings s=web.getSettings(); s.setJavaScriptEnabled(true); s.setDomStorageEnabled(true); s.setMediaPlaybackRequiresUserGesture(false);
    web.setWebViewClient(new WebViewClient());
    web.addJavascriptInterface(new Bridge(),"AndroidMedia3");
    web.loadUrl("file:///android_asset/www/index.html");
  }
  public class Bridge {
    @JavascriptInterface public void play(String json){
      runOnUiThread(()->{
        try{
          JSONObject o=new JSONObject(json);
          Intent i=new Intent(MainActivity.this,PlayerActivity.class);
          i.putExtra("payload",json); startActivity(i);
        }catch(Exception ignored){}
      });
    }
    @JavascriptInterface public void pause(){}
    @JavascriptInterface public void resume(){}
    @JavascriptInterface public void seekTo(long ms){}
    @JavascriptInterface public void seekBy(long ms){}
    @JavascriptInterface public void setVolume(double v){}
    @JavascriptInterface public void setSpeed(double v){}
    @JavascriptInterface public void stop(){}
  }
  @Override public void onBackPressed(){
    if(web.canGoBack()) web.goBack(); else super.onBackPressed();
  }
}
