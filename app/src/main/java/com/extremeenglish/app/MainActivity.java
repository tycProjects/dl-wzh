package com.extremeenglish.app;

import android.app.*; import android.os.*; import android.view.*; import android.webkit.*; import android.content.*;

public class MainActivity extends Activity {
  WebView w;
  @Override public void onCreate(Bundle b){ super.onCreate(b); getWindow().setStatusBarColor(android.graphics.Color.rgb(138,91,152));
    w=new WebView(this); w.setBackgroundColor(android.graphics.Color.TRANSPARENT); w.getSettings().setJavaScriptEnabled(true); w.getSettings().setDomStorageEnabled(true); w.getSettings().setTextZoom(100); w.setOverScrollMode(View.OVER_SCROLL_NEVER);
    w.setWebViewClient(new WebViewClient()); setContentView(w); w.loadUrl("file:///android_asset/index.html"); }
  @Override public void onBackPressed(){ if(w.canGoBack()) w.goBack(); else super.onBackPressed(); }
}
