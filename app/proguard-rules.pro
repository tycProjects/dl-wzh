# Android framework-instantiated entry points are retained by AGP manifest rules.
# Keep runtime-visible annotations/signatures needed by AndroidX and reflection.
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Preserve Parcelable/Serializable creators if any model is passed through framework APIs.
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator CREATOR;
}

# Do not suppress all AndroidX warnings globally; retain only harmless missing optional annotations.
-dontwarn javax.annotation.**

# ShizukuRestart calls the private Shizuku.newProcess by reflection; R8 would otherwise drop it as unused.
-keepclassmembers class rikka.shizuku.Shizuku {
    private static *** newProcess(java.lang.String[], java.lang.String[], java.lang.String);
}
