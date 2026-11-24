package com.tyce.enhancer

import ai.onnxruntime.OnnxJavaType
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.TensorInfo
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PointF
import android.media.FaceDetector
import android.util.Half
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/** pa-gwapo sa nawong, GPEN */
class FaceRestorer(context: Context) {

    private val env: OrtEnvironment = OrtEnvironment.getEnvironment()
    private val session: OrtSession
    private val inputName: String
    private val size: Int
    private val fp16: Boolean

    init {
        val options = OrtSession.SessionOptions().apply {
            setIntraOpNumThreads(max(2, Runtime.getRuntime().availableProcessors() - 2))
        }
        session = env.createSession(modelFile(context).readBytes(), options)
        val entry = session.inputInfo.entries.first()
        inputName = entry.key
        val info = entry.value.info as TensorInfo
        fp16 = info.type == OnnxJavaType.FLOAT16
        val h = if (info.shape.size == 4) info.shape[2] else -1L
        size = if (h > 0) h.toInt() else 256
    }

    fun close() = session.close()

    /** gwapo na ang nawong, ayos */
    fun restore(src: Bitmap, isCancelled: () -> Boolean = { false }): Bitmap {
        return try {
            val faces = detect(src)
            if (faces.isEmpty()) return src
            val out = if (src.isMutable) src else src.copy(Bitmap.Config.ARGB_8888, true) ?: return src
            for (f in faces) {
                if (isCancelled()) break
                try { restoreOne(out, f) } catch (t: Throwable) { t.printStackTrace() }
            }
            if (out !== src) src.recycle()
            out
        } catch (t: Throwable) {
            t.printStackTrace()
            src
        }
    }

    private class Face(val cx: Float, val cy: Float, val side: Float)

    private fun detect(src: Bitmap): List<Face> {
        // gamay lang, maselan ni FaceDetector
        val maxSide = 640f
        val f = min(1f, maxSide / max(src.width, src.height))
        var w = (src.width * f).toInt().coerceAtLeast(16)
        if (w % 2 == 1) w--
        val h = (src.height * f).toInt().coerceAtLeast(16)
        val small = Bitmap.createScaledBitmap(src, w, h, true)
        val s565 = small.copy(Bitmap.Config.RGB_565, false)
        if (small !== src) small.recycle()
        val arr = arrayOfNulls<FaceDetector.Face>(6)
        val n = FaceDetector(s565.width, s565.height, arr.size).findFaces(s565, arr)
        s565.recycle()
        val sx = src.width.toFloat() / w
        val sy = src.height.toFloat() / h
        val list = ArrayList<Face>()
        for (i in 0 until n) {
            val face = arr[i] ?: continue
            if (face.confidence() < 0.4f) continue
            val p = PointF()
            face.getMidPoint(p)
            val eye = face.eyesDistance() * sx
            val side = eye * 2.8f
            // mata, gamay ra sa taas
            list.add(Face(p.x * sx, p.y * sy + side * 0.12f, side))
        }
        return list
    }

    private fun restoreOne(dst: Bitmap, face: Face) {
        val side = face.side.toInt().coerceAtLeast(32)
        val left = face.cx - face.side / 2f
        val top = face.cy - face.side / 2f

        // putla, itom ang gawas, ayos
        val crop = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val m = Matrix().apply {
            postTranslate(-left, -top)
            postScale(size / face.side, size / face.side)
        }
        Canvas(crop).drawBitmap(dst, m, Paint(Paint.FILTER_BITMAP_FLAG))

        val restored = runModel(crop)
        crop.recycle()
        val big = Bitmap.createScaledBitmap(restored, side, side, true)
        if (big !== restored) restored.recycle()

        // ilubong balik, hinay-hinay lang
        val x0 = max(0, left.toInt())
        val y0 = max(0, top.toInt())
        val x1 = min(dst.width, left.toInt() + side)
        val y1 = min(dst.height, top.toInt() + side)
        val rw = x1 - x0
        val rh = y1 - y0
        if (rw > 0 && rh > 0) {
            val orig = IntArray(rw * rh)
            dst.getPixels(orig, 0, rw, x0, y0, rw, rh)
            val fresh = IntArray(side * side)
            big.getPixels(fresh, 0, side, 0, 0, side, side)
            val half = side / 2f
            for (y in 0 until rh) {
                val fy = (y0 + y) - top.toInt()
                val ny = (fy + 0.5f - half) / half
                for (x in 0 until rw) {
                    val fx = (x0 + x) - left.toInt()
                    val nx = (fx + 0.5f - half) / half
                    val r = sqrt(nx * nx + ny * ny)
                    var wgt = when {
                        r <= 0.60f -> 1f
                        r >= 0.95f -> 0f
                        else -> { val t = (0.95f - r) / 0.35f; t * t * (3f - 2f * t) }
                    }
                    wgt *= 0.85f
                    if (wgt <= 0f) continue
                    val a = orig[y * rw + x]
                    val b = fresh[fy.coerceIn(0, side - 1) * side + fx.coerceIn(0, side - 1)]
                    val rr = (((a shr 16) and 0xFF) * (1 - wgt) + ((b shr 16) and 0xFF) * wgt).toInt()
                    val gg = (((a shr 8) and 0xFF) * (1 - wgt) + ((b shr 8) and 0xFF) * wgt).toInt()
                    val bb = ((a and 0xFF) * (1 - wgt) + (b and 0xFF) * wgt).toInt()
                    orig[y * rw + x] = (0xFF shl 24) or (rr shl 16) or (gg shl 8) or bb
                }
            }
            dst.setPixels(orig, 0, rw, x0, y0, rw, rh)
        }
        big.recycle()
    }

    private fun runModel(crop: Bitmap): Bitmap {
        val s = size
        val plane = s * s
        val px = IntArray(plane)
        crop.getPixels(px, 0, s, 0, 0, s, s)
        val data = FloatArray(3 * plane)
        for (i in 0 until plane) {
            val p = px[i]
            data[i] = (((p shr 16) and 0xFF) / 255f - 0.5f) / 0.5f
            data[plane + i] = (((p shr 8) and 0xFF) / 255f - 0.5f) / 0.5f
            data[2 * plane + i] = ((p and 0xFF) / 255f - 0.5f) / 0.5f
        }
        val shape = longArrayOf(1, 3, s.toLong(), s.toLong())
        val input: OnnxTensor = if (fp16) {
            val bb = ByteBuffer.allocateDirect(data.size * 2).order(ByteOrder.nativeOrder())
            val sb = bb.asShortBuffer()
            for (v in data) sb.put(Half.toHalf(v))
            OnnxTensor.createTensor(env, bb, shape, OnnxJavaType.FLOAT16)
        } else {
            OnnxTensor.createTensor(env, FloatBuffer.wrap(data), shape)
        }
        val result = session.run(mapOf(inputName to input))
        val outT = result[0] as OnnxTensor
        val out = FloatArray(3 * plane)
        if (outT.info.type == OnnxJavaType.FLOAT16) {
            val sb = outT.byteBuffer.order(ByteOrder.nativeOrder()).asShortBuffer()
            for (i in out.indices) out[i] = Half.toFloat(sb.get())
        } else {
            outT.floatBuffer.get(out)
        }
        result.close()
        input.close()

        val bmp = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888)
        val o = IntArray(plane)
        for (i in 0 until plane) {
            val r = ((out[i] * 0.5f + 0.5f).coerceIn(0f, 1f) * 255f + 0.5f).toInt()
            val g = ((out[plane + i] * 0.5f + 0.5f).coerceIn(0f, 1f) * 255f + 0.5f).toInt()
            val b = ((out[2 * plane + i] * 0.5f + 0.5f).coerceIn(0f, 1f) * 255f + 0.5f).toInt()
            o[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }
        bmp.setPixels(o, 0, s, 0, 0, s, s)
        return bmp
    }

    companion object {
        fun modelFile(context: Context): File = File(context.filesDir, "face_model.onnx")
        fun hasModel(context: Context): Boolean =
            modelFile(context).let { it.exists() && it.length() > 0 }
    }
}
