package com.tyce.enhancer

import ai.onnxruntime.OnnxJavaType
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.TensorInfo
import android.content.Context
import android.graphics.Bitmap
import java.io.File
import java.nio.FloatBuffer
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/** rife ai, pa-smooth ang video */
class RifeInterp(context: Context) {

    private val env: OrtEnvironment = OrtEnvironment.getEnvironment()
    private val session: OrtSession

    private class In(val name: String, val shape: LongArray)

    private val inputs = ArrayList<In>()
    private var fixedW = -1
    private var fixedH = -1
    private var concatChannels = 0       // 6 o 7, depende ra
    private var timeInput: In? = null    // lahi nga oras, kung naa
    private var imgA: In? = null
    private var imgB: In? = null

    init {
        val options = OrtSession.SessionOptions().apply {
            setIntraOpNumThreads(max(2, Runtime.getRuntime().availableProcessors() - 2))
            setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
        }
        session = env.createSession(loadBytes(context), options)
        for ((name, ni) in session.inputInfo) {
            val ti = ni.info as? TensorInfo ?: throw IllegalStateException("Unsupported model input")
            if (ti.type != OnnxJavaType.FLOAT) throw IllegalStateException("Model needs half-precision input; use the normal (fp32) file")
            inputs.add(In(name, ti.shape))
        }
        if (inputs.size == 1) {
            val s = inputs[0].shape
            if (s.size != 4 || (s[1] != 6L && s[1] != 7L)) throw IllegalStateException("Unsupported RIFE model layout")
            concatChannels = s[1].toInt()
            if (s[2] > 0 && s[3] > 0) { fixedH = s[2].toInt(); fixedW = s[3].toInt() }
        } else {
            val imgs = inputs.filter { it.shape.size == 4 && it.shape[1] == 3L }
            if (imgs.size < 2) throw IllegalStateException("Unsupported RIFE model layout")
            imgA = imgs[0]; imgB = imgs[1]
            timeInput = inputs.firstOrNull { it !== imgA && it !== imgB }
            val s = imgA!!.shape
            if (s[2] > 0 && s[3] > 0) { fixedH = s[2].toInt(); fixedW = s[3].toInt() }
        }
    }

    fun close() = session.close()

    /** frame sa tunga, parehas sukod */
    fun mid(a: Bitmap, b: Bitmap): Bitmap {
        val w = a.width
        val h = a.height
        if (b.width != w || b.height != h) return a.copy(Bitmap.Config.ARGB_8888, false)
        if (sceneCut(a, b)) return a.copy(Bitmap.Config.ARGB_8888, false)

        // sukod sa trabaho, gamay-i lang
        val f = min(1f, MAX_SIDE.toFloat() / max(w, h))
        var rw = (w * f).toInt().coerceAtLeast(32)
        var rh = (h * f).toInt().coerceAtLeast(32)
        val pw: Int
        val ph: Int
        if (fixedW > 0) { rw = fixedW; rh = fixedH; pw = fixedW; ph = fixedH }
        else { pw = (rw + 31) / 32 * 32; ph = (rh + 31) / 32 * 32 }

        val sa = Bitmap.createScaledBitmap(a, rw, rh, true)
        val sb = Bitmap.createScaledBitmap(b, rw, rh, true)
        val da = planes(sa, rw, rh, pw, ph)
        val db = planes(sb, rw, rh, pw, ph)
        if (sa !== a) sa.recycle()
        if (sb !== b) sb.recycle()

        val tensors = ArrayList<OnnxTensor>()
        val feed = HashMap<String, OnnxTensor>()
        try {
            if (concatChannels > 0) {
                val plane = pw * ph
                val all = FloatArray(concatChannels * plane)
                System.arraycopy(da, 0, all, 0, 3 * plane)
                System.arraycopy(db, 0, all, 3 * plane, 3 * plane)
                if (concatChannels == 7) java.util.Arrays.fill(all, 6 * plane, 7 * plane, 0.5f)
                val t = OnnxTensor.createTensor(env, FloatBuffer.wrap(all), longArrayOf(1, concatChannels.toLong(), ph.toLong(), pw.toLong()))
                tensors.add(t); feed[inputs[0].name] = t
            } else {
                val ta = OnnxTensor.createTensor(env, FloatBuffer.wrap(da), longArrayOf(1, 3, ph.toLong(), pw.toLong()))
                val tb = OnnxTensor.createTensor(env, FloatBuffer.wrap(db), longArrayOf(1, 3, ph.toLong(), pw.toLong()))
                tensors.add(ta); tensors.add(tb)
                feed[imgA!!.name] = ta; feed[imgB!!.name] = tb
                val ti = timeInput
                if (ti != null) {
                    val dims = if (ti.shape.size == 4) longArrayOf(1, 1, ph.toLong(), pw.toLong())
                    else LongArray(ti.shape.size) { 1L }
                    var n = 1
                    for (d in dims) n *= d.toInt()
                    val tt = OnnxTensor.createTensor(env, FloatBuffer.wrap(FloatArray(n) { 0.5f }), dims)
                    tensors.add(tt); feed[ti.name] = tt
                }
            }
            val result = session.run(feed)
            try {
                val out = result[0] as OnnxTensor
                val shape = out.info.shape
                if (shape.size != 4 || shape[1] != 3L) throw IllegalStateException("Unexpected RIFE output")
                val oh = shape[2].toInt()
                val ow = shape[3].toInt()
                val buf = out.floatBuffer
                val data = FloatArray(buf.remaining())
                buf.get(data)
                val plane = oh * ow
                val px = IntArray(rw * rh)
                for (y in 0 until rh) for (x in 0 until rw) {
                    val i = y * ow + x
                    val r = (data[i].coerceIn(0f, 1f) * 255f + 0.5f).toInt()
                    val g = (data[plane + i].coerceIn(0f, 1f) * 255f + 0.5f).toInt()
                    val bl = (data[2 * plane + i].coerceIn(0f, 1f) * 255f + 0.5f).toInt()
                    px[y * rw + x] = (0xFF shl 24) or (r shl 16) or (g shl 8) or bl
                }
                val small = Bitmap.createBitmap(rw, rh, Bitmap.Config.ARGB_8888)
                small.setPixels(px, 0, rw, 0, 0, rw, rh)
                if (rw == w && rh == h) return small
                val big = Bitmap.createScaledBitmap(small, w, h, true)
                if (big !== small) small.recycle()
                return big
            } finally {
                result.close()
            }
        } finally {
            for (t in tensors) t.close()
        }
    }

    /** i-pad ang kilid, kopya */
    private fun planes(bmp: Bitmap, rw: Int, rh: Int, pw: Int, ph: Int): FloatArray {
        val px = IntArray(rw * rh)
        bmp.getPixels(px, 0, rw, 0, 0, rw, rh)
        val plane = pw * ph
        val d = FloatArray(3 * plane)
        for (y in 0 until ph) {
            val sy = min(y, rh - 1)
            for (x in 0 until pw) {
                val p = px[sy * rw + min(x, rw - 1)]
                val i = y * pw + x
                d[i] = ((p shr 16) and 0xFF) / 255f
                d[plane + i] = ((p shr 8) and 0xFF) / 255f
                d[2 * plane + i] = (p and 0xFF) / 255f
            }
        }
        return d
    }

    /** cut ni, ayaw pag-imbento */
    private fun sceneCut(a: Bitmap, b: Bitmap): Boolean {
        var sum = 0L
        var n = 0
        val sx = max(1, a.width / 32)
        val sy = max(1, a.height / 32)
        var y = 0
        while (y < a.height) {
            var x = 0
            while (x < a.width) {
                val p = a.getPixel(x, y)
                val q = b.getPixel(x, y)
                sum += abs(((p shr 16) and 0xFF) - ((q shr 16) and 0xFF)) +
                    abs(((p shr 8) and 0xFF) - ((q shr 8) and 0xFF)) +
                    abs((p and 0xFF) - (q and 0xFF))
                n += 3
                x += sx
            }
            y += sy
        }
        return n > 0 && sum.toFloat() / n > 45f
    }

    companion object {
        /** dako sharp, gamay paspas */
        const val MAX_SIDE = 1280

        private const val ASSET = "rife.onnx"

        /** model nga imo gi-load */
        fun modelFile(context: Context): File = File(context.filesDir, "rife_model.onnx")

        fun hasUserModel(context: Context): Boolean =
            modelFile(context).let { it.exists() && it.length() > 0 }

        /** naa bay rife? tan-awa dayon */
        fun hasModel(context: Context): Boolean {
            if (hasUserModel(context)) return true
            return try { context.assets.open(ASSET).close(); true } catch (_: Throwable) { false }
        }

        private fun loadBytes(context: Context): ByteArray {
            if (hasUserModel(context)) return modelFile(context).readBytes()
            return context.assets.open(ASSET).use { it.readBytes() }
        }
    }
}
