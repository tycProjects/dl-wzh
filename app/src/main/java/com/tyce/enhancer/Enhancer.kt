package com.tyce.enhancer

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.graphics.Bitmap
import java.nio.FloatBuffer
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** hiwa-hiwa para dili mag-crash */
class Enhancer(context: Context, modelAsset: String = "realesrgan.onnx", best: Boolean = false) {

    private val env: OrtEnvironment = OrtEnvironment.getEnvironment()
    private val session: OrtSession
    private val inputName: String
    private val tileSize: Int

    init {
        val bytes = if (best) bestModelFile(context).readBytes() else loadModelBytes(context, modelAsset)
        val options = OrtSession.SessionOptions().apply {
            setIntraOpNumThreads(max(2, Runtime.getRuntime().availableProcessors() - 2))
            // mag-optimize sa onnx, bahala sila
            setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
        }
        session = env.createSession(bytes, options)

        val inputInfo = session.inputInfo.entries.first()
        inputName = inputInfo.key
        val shape = (inputInfo.value.info as ai.onnxruntime.TensorInfo).shape
        // -1 kay wa ko kabalo
        val h = if (shape.size == 4) shape[2] else -1L
        val w = if (shape.size == 4) shape[3] else -1L
        tileSize = if (h > 0 && w > 0) min(h, w).toInt() else DEFAULT_TILE
    }

    /** kada tile, report sa UI */
    fun enhance(
        src: Bitmap,
        onProgress: (Int, Int) -> Unit = { _, _ -> },
        isCancelled: () -> Boolean = { false },
        onPreview: (Bitmap, Float) -> Unit = { _, _ -> },
        // kung parehas, kopyaha lang uy
        ref: Bitmap? = null,
        refOut: Bitmap? = null
    ): Bitmap? {
        val srcW = src.width
        val srcH = src.height
        val pixels = IntArray(srcW * srcH)
        src.getPixels(pixels, 0, srcW, 0, 0, srcW, srcH)

        val t = tileSize
        val pad = TILE_PAD
        val core = t - 2 * pad
        require(core > 0) { "Tile size too small" }

        val tilesX = (srcW + core - 1) / core
        val tilesY = (srcH + core - 1) / core
        val total = tilesX * tilesY

        var out: Bitmap? = null
        var scale = 0
        var done = 0

        var refPixels: IntArray? = null
        if (ref != null && refOut != null && ref.width == srcW && ref.height == srcH &&
            refOut.width % srcW == 0 && refOut.height / srcH == refOut.width / srcW
        ) {
            refPixels = IntArray(srcW * srcH)
            ref.getPixels(refPixels, 0, srcW, 0, 0, srcW, srcH)
            scale = refOut.width / srcW
            out = Bitmap.createBitmap(srcW * scale, srcH * scale, Bitmap.Config.ARGB_8888)
        }

        val tileData = FloatArray(3 * t * t)
        val plane = t * t

        for (ty in 0 until tilesY) {
            for (tx in 0 until tilesX) {
                if (isCancelled()) {
                    out?.recycle()
                    return null
                }

                val cx = tx * core
                val cy = ty * core
                val cw = min(core, srcW - cx)
                val ch = min(core, srcH - cy)

                // parehas na? skip na lang
                if (refPixels != null && refOut != null && out != null &&
                    sameAsRef(pixels, refPixels, srcW, srcH, cx - pad, cy - pad, cw + 2 * pad, ch + 2 * pad)
                ) {
                    val rw = cw * scale
                    val rh = ch * scale
                    val blk = IntArray(rw * rh)
                    refOut.getPixels(blk, 0, rw, cx * scale, cy * scale, rw, rh)
                    out.setPixels(blk, 0, rw, cx * scale, cy * scale, rw, rh)
                    done++
                    onProgress(done, total)
                    continue
                }

                // sudla ang pixels, ayaw reklamo
                for (y in 0 until t) {
                    val sy = (cy - pad + y).coerceIn(0, srcH - 1)
                    val rowBase = sy * srcW
                    for (x in 0 until t) {
                        val sx = (cx - pad + x).coerceIn(0, srcW - 1)
                        val p = pixels[rowBase + sx]
                        val i = y * t + x
                        tileData[i] = ((p shr 16) and 0xFF) / 255f
                        tileData[plane + i] = ((p shr 8) and 0xFF) / 255f
                        tileData[2 * plane + i] = (p and 0xFF) / 255f
                    }
                }

                val inputTensor = OnnxTensor.createTensor(
                    env,
                    FloatBuffer.wrap(tileData),
                    longArrayOf(1, 3, t.toLong(), t.toLong())
                )
                val result = session.run(mapOf(inputName to inputTensor))
                val outTensor = result[0] as OnnxTensor
                val shape = outTensor.info.shape // output ni, ayaw pagkalibog
                val oh = shape[2].toInt()
                val ow = shape[3].toInt()
                val buf = outTensor.floatBuffer
                val outData = FloatArray(buf.remaining())
                buf.get(outData)
                result.close()
                inputTensor.close()

                if (out == null) {
                    scale = ow / t
                    out = Bitmap.createBitmap(srcW * scale, srcH * scale, Bitmap.Config.ARGB_8888)
                }

                // tunga ra, kilid dili kuhaon
                val ocw = cw * scale
                val och = ch * scale
                val off = pad * scale
                val oPlane = oh * ow
                val block = IntArray(ocw * och)
                for (y in 0 until och) {
                    for (x in 0 until ocw) {
                        val i = (off + y) * ow + (off + x)
                        val r = (outData[i].coerceIn(0f, 1f) * 255f + 0.5f).toInt()
                        val g = (outData[oPlane + i].coerceIn(0f, 1f) * 255f + 0.5f).toInt()
                        val b = (outData[2 * oPlane + i].coerceIn(0f, 1f) * 255f + 0.5f).toInt()
                        block[y * ocw + x] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
                    }
                }
                out!!.setPixels(block, 0, ocw, cx * scale, cy * scale, ocw, och)

                done++
                onProgress(done, total)
            }

            // humana row, ipakita sa user
            val soFar = out
            if (soFar != null) {
                val long = max(soFar.width, soFar.height)
                val f = min(1f, PREVIEW_SIDE.toFloat() / long)
                val preview = Bitmap.createScaledBitmap(
                    soFar,
                    (soFar.width * f).roundToInt().coerceAtLeast(1),
                    (soFar.height * f).roundToInt().coerceAtLeast(1),
                    true
                )
                onPreview(preview, min(1f, ((ty + 1) * core).toFloat() / srcH))
            }
        }
        return out
    }

    /** parehas ra? mao nga skip */
    private fun sameAsRef(a: IntArray, b: IntArray, w: Int, h: Int, x0: Int, y0: Int, rw: Int, rh: Int): Boolean {
        val xs = x0.coerceAtLeast(0)
        val ys = y0.coerceAtLeast(0)
        val xe = min(w, x0 + rw)
        val ye = min(h, y0 + rh)
        var sum = 0L
        var big = 0
        var n = 0
        var y = ys
        while (y < ye) {
            var x = xs
            while (x < xe) {
                val p = a[y * w + x]
                val q = b[y * w + x]
                val d = (kotlin.math.abs(((p shr 16) and 0xFF) - ((q shr 16) and 0xFF)) +
                    kotlin.math.abs(((p shr 8) and 0xFF) - ((q shr 8) and 0xFF)) +
                    kotlin.math.abs((p and 0xFF) - (q and 0xFF))) / 3
                sum += d
                if (d > 24) big++
                n++
                x += 2
            }
            y += 2
        }
        if (n == 0) return false
        return sum.toFloat() / n < 2.5f && big * 200 < n
    }

    fun close() {
        session.close()
    }

    companion object {
        // dako tile, wa'y linya pastilan
        private const val DEFAULT_TILE = 192
        // dugang kilid, ibasura rapud
        private const val TILE_PAD = 16
        private const val PREVIEW_SIDE = 900

        /** model nga imo gi-import */
        fun userModelFile(context: Context): java.io.File =
            java.io.File(context.filesDir, "realesrgan.onnx")

        /** model nga Best, kung naa */
        fun bestModelFile(context: Context): java.io.File =
            java.io.File(context.filesDir, "realesrgan_best.onnx")

        fun hasBest(context: Context): Boolean =
            bestModelFile(context).let { it.exists() && it.length() > 0 }

        /** naa ba'y model? tan-awa dayon */
        fun hasModel(context: Context, modelAsset: String = "realesrgan.onnx"): Boolean {
            val f = userModelFile(context)
            if (f.exists() && f.length() > 0) return true
            return try {
                context.assets.open(modelAsset).close(); true
            } catch (_: Throwable) { false }
        }

        private fun loadModelBytes(context: Context, modelAsset: String): ByteArray {
            val f = userModelFile(context)
            if (f.exists() && f.length() > 0) return f.readBytes()
            try {
                return context.assets.open(modelAsset).use { it.readBytes() }
            } catch (e: java.io.IOException) {
                throw IllegalStateException(
                    "Model file not found. Tap \"Load model\" and choose your realesrgan .onnx file."
                )
            }
        }
    }
}
