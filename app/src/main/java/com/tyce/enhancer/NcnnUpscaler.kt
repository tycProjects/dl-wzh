package com.tyce.enhancer

import android.content.Context
import android.graphics.Bitmap
import com.kaminari.imagine.Engine
import java.io.File
import java.nio.ByteBuffer
import kotlin.math.roundToInt
import kotlin.math.sqrt

/** Runs an ncnn model through the recovered native engine. Single instance at a time (native is global). */
object NcnnUpscaler {
    private val lock = Any()
    private var gpuTried = false
    private var loadedId: String? = null

    private fun copyAsset(context: Context, asset: String, dst: File) {
        if (dst.exists() && dst.length() > 0) return
        dst.parentFile?.mkdirs()
        val tmp = File(dst.path + ".tmp")
        context.assets.open(asset).use { i -> tmp.outputStream().use { i.copyTo(it) } }
        if (!tmp.renameTo(dst)) { tmp.delete(); throw java.io.IOException("Could not extract $asset") }
    }

    /** Max input pixels accepted; larger inputs are rejected before native code runs. */
    const val MAX_INPUT_PIXELS = 4_000_000

    fun upscale(context: Context, m: EngineModel, src: Bitmap, onProgress: (Float) -> Unit, isCancelled: () -> Boolean): Bitmap {
        require(m.isNcnn) { "Not an ncnn model" }
        require(src.width.toLong() * src.height <= MAX_INPUT_PIXELS) { "Image too large for this model" }
        synchronized(lock) {
            if (!Engine.nativeAvailable) throw IllegalStateException("Native engine is not available on this device")
            if (!gpuTried) { gpuTried = true; try { Engine.initGpu() } catch (_: Throwable) {} }
            val pf: File; val bf: File
            if (m.isUser) {
                pf = File(m.userParam!!); bf = File(m.userBin!!)
            } else {
                val dir = File(context.filesDir, "models")
                pf = File(dir, File(m.assetParam!!).name)
                bf = File(dir, File(m.assetBin!!).name)
                copyAsset(context, m.assetParam!!, pf); copyAsset(context, m.assetBin!!, bf)
            }
            if (loadedId != m.id) {
                if (!Engine.loadModel(pf.absolutePath, bf.absolutePath, m.scale, m.prepadding, 0))
                    throw IllegalStateException("Could not load ${m.label}")
                loadedId = m.id
            }
            val argb = if (src.config == Bitmap.Config.ARGB_8888) src else src.copy(Bitmap.Config.ARGB_8888, false)
            val w = argb.width; val h = argb.height
            val inBytes = ByteArray(w * h * 4)
            argb.copyPixelsToBuffer(ByteBuffer.wrap(inBytes))
            if (argb !== src) argb.recycle()
            if (isCancelled()) throw CancellationException2()
            val sink = Engine.ProgressSink { p -> onProgress(p.coerceIn(0f, 1f)) }
            val out = Engine.processImage(inBytes, w, h, sink)
                ?: throw IllegalStateException("Engine returned no image")
            val s = sqrt(out.size.toDouble() / (w.toDouble() * h * 4)).roundToInt()
            if (s != m.scale) throw IllegalStateException("This model produced ${s}× output but was set to ${m.scale}×. Re-import it with the right scale.")
            if (s < 1 || out.size.toLong() != w.toLong() * s * h * s * 4)
                throw IllegalStateException("Unexpected output size ${out.size} for ${w}x$h")
            val bmp = Bitmap.createBitmap(w * s, h * s, Bitmap.Config.ARGB_8888)
            bmp.copyPixelsFromBuffer(ByteBuffer.wrap(out))
            return bmp
        }
    }

    /** Loads the model and runs a tiny image through it. Returns null if it works, else a readable error. */
    fun verify(context: Context, m: EngineModel): String? = try {
        val t = Bitmap.createBitmap(24, 24, Bitmap.Config.ARGB_8888).also { it.eraseColor(0xFF808080.toInt()) }
        val o = upscale(context, m, t, { }, { false })
        o.recycle(); t.recycle(); null
    } catch (e: Throwable) {
        loadedId = null
        e.message ?: "The engine could not run this model."
    }

    class CancellationException2 : RuntimeException("cancelled")

    fun release() = synchronized(lock) {
        if (loadedId != null || gpuTried) { try { Engine.destroy() } catch (_: Throwable) {} }
        loadedId = null; gpuTried = false
    }
}
