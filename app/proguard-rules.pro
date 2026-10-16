# ─── FLUTTER SPECIFIC ──────────────────────────────────────────────────────
-keep class io.flutter.app.** { *; }
-keep class io.flutter.plugin.**  { *; }
-keep class io.flutter.util.**  { *; }
-keep class io.flutter.view.**  { *; }
-keep class io.flutter.**  { *; }
-keep class io.flutter.plugins.**  { *; }
-keep class io.flutter.embedding.** { *; }

# ─── APP SPECIFIC ─────────────────────────────────────────────────────────
-keep class com.datas.atlantis.** { *; }

# ─── PLAY CORE ─────────────────────────────────────────────────────────────
-keep class com.google.android.play.core.** { *; }
-keep interface com.google.android.play.core.** { *; }
-dontwarn com.google.android.play.core.**
-dontwarn com.google.android.play.core.splitcompat.**
-dontwarn com.google.android.play.core.splitinstall.**

# ─── KEEP NATIVE METHODS ──────────────────────────────────────────────────
-keepclasseswithmembers class * {
    native <methods>;
}

# ─── KEEP @KEEP ANNOTATION ───────────────────────────────────────────────
-keep @androidx.annotation.Keep class * {*;}
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}

# ─── SERIALIZATION ────────────────────────────────────────────────────────
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ─── WEBVIEW ──────────────────────────────────────────────────────────────
-keep class com.android.webview.** { *; }
-keep class android.webkit.** { *; }

# ─── VIDEO PLAYER ─────────────────────────────────────────────────────────
-keep class com.google.android.exoplayer2.** { *; }
-keep interface com.google.android.exoplayer2.** { *; }

# ─── HTTP / OKHTTP ───────────────────────────────────────────────────────
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# ─── REDUCE WARNINGS ─────────────────────────────────────────────────────
-dontwarn org.xmlpull.v1.**
-dontwarn org.apache.**
-dontwarn javax.**
-dontwarn java.awt.**
-dontwarn jdk.**
-dontwarn sun.**

# ─── KEEP GENERATED CLASSES ──────────────────────────────────────────────
-keep class **.GeneratedPluginRegistrant { *; }