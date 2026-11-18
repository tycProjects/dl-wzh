package com.vexora.studio;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.ValueCallback;
import android.content.Intent;
import android.net.Uri;

public class MainActivity extends Activity {
  private WebView webView;
  private ValueCallback<Uri[]> uploadCallback;
  private static final int FILE_CHOOSER = 42;
  @Override public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    getWindow().getDecorView().setSystemUiVisibility(5894 | 1024 | 512 | 4096);
    webView = new WebView(this); setContentView(webView);
    WebSettings s = webView.getSettings(); s.setJavaScriptEnabled(true); s.setDomStorageEnabled(true); s.setAllowFileAccess(true); s.setMediaPlaybackRequiresUserGesture(false);
    webView.setWebViewClient(new WebViewClient());
    webView.setWebChromeClient(new WebChromeClient() {
      @Override public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> cb, FileChooserParams params) {
        if (uploadCallback != null) uploadCallback.onReceiveValue(null);
        uploadCallback = cb;
        Intent i = params.createIntent();
        try { startActivityForResult(i, FILE_CHOOSER); } catch (Exception e) { uploadCallback = null; return false; }
        return true;
      }
    });
    webView.loadUrl("file:///android_asset/index.html");
  }
  @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == FILE_CHOOSER && uploadCallback != null) {
      Uri[] result = WebChromeClient.FileChooserParams.parseResult(resultCode, data);
      uploadCallback.onReceiveValue(result); uploadCallback = null;
    }
  }
  @Override public void onBackPressed() { if (webView.canGoBack()) webView.goBack(); else super.onBackPressed(); }
}
