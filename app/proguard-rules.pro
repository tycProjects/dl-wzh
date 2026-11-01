# ZewMod ProGuard Rules

# Keep JNI-facing class (nama harus exact untuk JNI lookup)
-keep class com.zewmod.core.NativeGate { *; }

# Shizuku wajib di-keep (binder IPC interface)
# Shizuku removed — no longer needed
-keep class moe.shizuku.** { *; }

# Keep service + activity name (referenced in manifest)
-keep class com.zewmod.core.ModService { *; }
-keep class com.zewmod.ui.MainActivity { *; }

# Keep HackState public fields
-keepclassmembers class com.zewmod.hack.HackState {
    public static *;
}
-keepclassmembers class com.zewmod.hack.AimbotConfig { *; }
-keepclassmembers class com.zewmod.hack.EspConfig { *; }
-keepclassmembers class com.zewmod.hack.ProxyConfig { *; }
-keepclassmembers class com.zewmod.hack.EnemyData { *; }

# Kotlin metadata
-keepattributes RuntimeVisibleAnnotations
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes SourceFile,LineNumberTable

# Kotlin coroutines
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# org.json
-keep class org.json.** { *; }

# Repack semua ke paket 'o' (mirror original APK obfuscation)
-repackageclasses 'o'
-allowaccessmodification
-overloadaggressively
