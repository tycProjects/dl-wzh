package com.example.htmltoapk;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebSettings;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WebView webView = new WebView(this);
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());

        Properties config = new Properties();
        try (InputStream in = getAssets().open("config.properties")) {
            config.load(in);
            String mode = config.getProperty("mode", "local").trim();
            if ("url".equalsIgnoreCase(mode)) {
                String url = config.getProperty("url", "").trim();
                if (url.startsWith("https://") || url.startsWith("http://")) {
                    webView.loadUrl(url);
                } else {
                    showError(webView, "Set a valid http:// or https:// URL in config.properties");
                }
            } else {
                webView.loadUrl("file:///android_asset/site/index.html");
            }
        } catch (Exception e) {
            showError(webView, "Could not read app configuration.");
        }
    }

    private void showError(WebView webView, String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        webView.loadData(
            "<html><meta name='viewport' content='width=device-width, initial-scale=1'>" +
            "<body style='font-family:sans-serif;padding:24px'><h2>HTML to APK</h2><p>" +
            message + "</p></body></html>",
            "text/html", "UTF-8"
        );
    }

    @Override
    public void onBackPressed() {
        // Keep the default Android back behavior for this minimal starter.
        super.onBackPressed();
    }
}
