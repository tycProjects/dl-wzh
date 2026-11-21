package com.tyce.enhancer

import android.graphics.Bitmap
import kotlin.math.min

/** pa-sharp gamay, parang filter */
object Finish {

    /** ayaw sobra, mag-ugly ra */
    fun sharpen(src: Bitmap, amount: Float = 0.55f, radius: Int = 2, isCancelled: () -> Boolean = { false }): Bitmap {
        return try {
            val w = src.width
            val h = src.height
            val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val stripe = 128
            val r = radius.coerceAtLeast(1)
            var y0 = 0
            while (y0 < h) {
                if (isCancelled()) { out.recycle(); return src }
                val y1 = min(h, y0 + stripe)
                val a = (y0 - r).coerceAtLeast(0)
                val b = min(h, y1 + r)
                val rows = b - a
                val px = IntArray(w * rows)
                src.getPixels(px, 0, w, 0, a, w, rows)

                // blur sa kilid, dayon taas
                val hr = FloatArray(w * rows)
                val hg = FloatArray(w * rows)
                val hb = FloatArray(w * rows)
                val win = (2 * r + 1).toFloat()
                for (y in 0 until rows) {
                    val base = y * w
                    var sr = 0f; var sg = 0f; var sb = 0f
                    for (x in -r..r) {
                        val p = px[base + x.coerceIn(0, w - 1)]
                        sr += (p shr 16) and 0xFF; sg += (p shr 8) and 0xFF; sb += p and 0xFF
                    }
                    for (x in 0 until w) {
                        hr[base + x] = sr / win; hg[base + x] = sg / win; hb[base + x] = sb / win
                        val add = px[base + (x + r + 1).coerceAtMost(w - 1)]
                        val rem = px[base + (x - r).coerceAtLeast(0)]
                        sr += ((add shr 16) and 0xFF) - ((rem shr 16) and 0xFF)
                        sg += ((add shr 8) and 0xFF) - ((rem shr 8) and 0xFF)
                        sb += (add and 0xFF) - (rem and 0xFF)
                    }
                }

                val block = IntArray(w * (y1 - y0))
                for (y in y0 until y1) {
                    val ly = y - a
                    for (x in 0 until w) {
                        var br = 0f; var bg = 0f; var bb = 0f
                        for (k in -r..r) {
                            val yy = (ly + k).coerceIn(0, rows - 1)
                            val i = yy * w + x
                            br += hr[i]; bg += hg[i]; bb += hb[i]
                        }
                        br /= win; bg /= win; bb /= win
                        val p = px[ly * w + x]
                        val pr = (p shr 16) and 0xFF
                        val pg = (p shr 8) and 0xFF
                        val pb = p and 0xFF
                        // ayaw labti ang gagmay, noise
                        val dr = pr - br; val dg = pg - bg; val db = pb - bb
                        val nr = (pr + amount * soft(dr)).toInt().coerceIn(0, 255)
                        val ng = (pg + amount * soft(dg)).toInt().coerceIn(0, 255)
                        val nb = (pb + amount * soft(db)).toInt().coerceIn(0, 255)
                        block[(y - y0) * w + x] = (0xFF shl 24) or (nr shl 16) or (ng shl 8) or nb
                    }
                }
                out.setPixels(block, 0, w, 0, y0, w, y1 - y0)
                y0 = y1
            }
            out
        } catch (t: Throwable) {
            t.printStackTrace()
            src
        }
    }

    /** ubos 2, wa koy labot */
    private fun soft(d: Float): Float {
        val a = kotlin.math.abs(d)
        if (a < 2f) return 0f
        val c = min(a, 40f)
        return if (d < 0) -c else c
    }
}
