# ONNX Runtime talks to its native (JNI) code by exact class/method names/signatures.
# Keep it all so R8 can still shrink everything else safely.
-keep class ai.onnxruntime.** { *; }
-dontwarn ai.onnxruntime.**

# Kotlin coroutines + reflection metadata R8 sometimes trims too aggressively.
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
-keep class kotlin.coroutines.Continuation
-dontwarn kotlinx.coroutines.**
