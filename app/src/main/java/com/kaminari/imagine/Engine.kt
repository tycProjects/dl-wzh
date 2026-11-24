package com.kaminari.imagine

/**
 * JNI binding for the prebuilt ncnn engine (libengine_jni.so) recovered from Imagine.
 * The class name/package are fixed by the native symbols Java_com_kaminari_imagine_Engine_*.
 * Contract (recovered from the original bytecode):
 *  - loadModel(paramPath, binPath, scale, prepadding, 0)
 *  - processImage(RGBA bytes from Bitmap.copyPixelsToBuffer, w, h, sink) -> ARGB_8888 bytes of (w*s)x(h*s),
 *    scale is derived as sqrt(out.size / (w*h*4)); sink.onProgress(Float) is called from native code.
 * Do not rename members or enable obfuscation for this class.
 */
object Engine {
    @JvmField var nativeAvailable: Boolean = try {
        System.loadLibrary("engine_jni"); true
    } catch (t: Throwable) {
        android.util.Log.e("AlterEngine", "Failed to load engine_jni: $t"); false
    }

    external fun initGpu(): Boolean
    external fun gpuCount(): Int
    external fun loadModel(paramPath: String, binPath: String, scale: Int, prepadding: Int, flags: Int): Boolean
    external fun processImage(rgba: ByteArray, width: Int, height: Int, sink: Any): ByteArray?
    external fun destroy()

    /** Called reflectively from native code: name and signature must stay onProgress(F)V. */
    class ProgressSink(private val cb: (Float) -> Unit) {
        @Suppress("unused")
        fun onProgress(p: Float) = cb(p)
    }
}
