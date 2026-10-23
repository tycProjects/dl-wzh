# Keep all service classes intact (VPN, Cheat, Float)
-keep class com.killcheat.app.service.** { *; }
-keep class com.killcheat.app.state.** { *; }
-keep class com.killcheat.app.util.** { *; }

# Keep VpnService subclass name (system must find it by name)
-keep public class * extends android.net.VpnService

# Standard Android rules
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service
-keep public class * extends androidx.fragment.app.Fragment

# Kotlin metadata
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
