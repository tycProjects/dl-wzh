# JUMA AI (জুমা AI) — ProGuard & R8 Optimization Rules
# Ensures WebView JavascriptInterface and Accessibility Service APIs are preserved

# Keep JavascriptInterface annotated methods in all classes
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Keep JUMA AI native package classes and methods
-keep class com.juma.ai.** { *; }

# Keep Accessibility Service
-keep class com.juma.ai.JumaAccessibilityService { *; }

# Keep Foreground Service
-keep class com.juma.ai.JumaForegroundService { *; }

# Keep Activity and Bridge
-keep class com.juma.ai.MainActivity { *; }
-keep class com.juma.ai.JumaBridge { *; }
