# Jembatan JavaScript harus tetap dikenali kalau minify dinyalakan
-keepclassmembers class com.flipper.app.MainActivity$Bridge {
    @android.webkit.JavascriptInterface <methods>;
}
