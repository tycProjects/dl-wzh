# ZewMod Engine ProGuard Rules

# Keep application class
-keep class com.zewmod.engine.ZewModApp { *; }

# Keep all model classes (serializable)
-keep class com.zewmod.engine.model.** { *; }
-keepclassmembers class com.zewmod.engine.model.** { *; }

# Keep bridge/JNI classes - critical for native lib calls
-keep class com.zewmod.engine.bridge.ZewModBridge { *; }
-keep class com.zewmod.engine.bridge.ZewModBridge$* { *; }
-keepclasseswithmembernames class com.zewmod.engine.bridge.ZewModBridge {
    native <methods>;
}

# Keep RelayBridge
-keep class com.zewmod.engine.bridge.RelayBridge { *; }

# Keep all service classes
-keep class com.zewmod.engine.service.** { *; }

# Keep all UI classes
-keep class com.zewmod.engine.ui.** { *; }

# Keep util classes
-keep class com.zewmod.engine.util.** { *; }

# Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.zewmod.engine.**$$serializer { *; }
-keepclassmembers class com.zewmod.engine.** {
    *** Companion;
}
-keepclasseswithmembers class com.zewmod.engine.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# Kotlin Coroutines
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# AndroidX
-keep class androidx.** { *; }
-keep interface androidx.** { *; }

# Material Design
-keep class com.google.android.material.** { *; }

# Keep R class
-keepclassmembers class **.R$* {
    public static <fields>;
}

# Remove logging in release
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

# Keep native library loading
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep Parcelable
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}
