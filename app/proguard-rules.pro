# Keep the JavaScript bridge methods used by assets/index.html
-keepclassmembers class com.nomango.browser.MainActivity$Bridge {
    @android.webkit.JavascriptInterface <methods>;
}
-keepclassmembers class com.nomango.browser.MainActivity$BlobBridge {
    @android.webkit.JavascriptInterface <methods>;
}
