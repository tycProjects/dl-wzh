# editor premium release hardening
# Keep Android components and WebView JavaScript bridges discoverable.
-keep class com.smali.fox.MainActivity { *; }
-keep class com.smali.fox.DeepSeekBatchService { *; }
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# dexlib2 uses interfaces and reference implementations dynamically.
-keep class org.jf.dexlib2.** { *; }
-dontwarn org.jf.dexlib2.**
-dontwarn javax.annotation.**

# Preserve useful source metadata for crash diagnostics without retaining names.
-keepattributes RuntimeVisibleAnnotations,RuntimeInvisibleAnnotations,AnnotationDefault,InnerClasses,EnclosingMethod
