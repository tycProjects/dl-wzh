package com.tyce.enhancer

import android.graphics.Bitmap
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.min

/** frame sa tunga, himo-himo */
object FrameInterp {

    private const val DOWN = 4      // gamay 4x, kapoy na mangita
    private const val BLOCK = 8     // gidak-on sa block, ewan
    private const val RANGE = 6     // pangita 6 pixels, sakto ra

    fun mid(a: Bitmap, b: Bitmap): Bitmap {
        return try {
            val w = a.width
            val h = a.height
            if (b.width != w || b.height != h) return a.copy(Bitmap.Config.ARGB_8888, false)
            val pa = IntArray(w * h)
            val pb = IntArray(w * h)
            a.getPixels(pa, 0, w, 0, 0, w, h)
            b.getPixels(pb, 0, w, 0, 0, w, h)

            val sw = w / DOWN
            val sh = h / DOWN
            val ga = gray(pa, w, sw, sh)
            val gb = gray(pb, w, sw, sh)
            val bw = sw / BLOCK
            val bh = sh / BLOCK
            if (bw < 2 || bh < 2) return blend(pa, pb, w, h)

            val vx = FloatArray(bw * bh)
            val vy = FloatArray(bw * bh)
            var totalBest = 0L
            for (by in 0 until bh) {
                for (bx in 0 until bw) {
                    val x0 = bx * BLOCK
                    val y0 = by * BLOCK
                    val sad0 = sad(ga, gb, sw, sh, x0, y0, 0, 0, Int.MAX_VALUE)
                    var best = sad0
                    var bdx = 0
                    var bdy = 0
                    for (dy in -RANGE..RANGE) {
                        for (dx in -RANGE..RANGE) {
                            if (dx == 0 && dy == 0) continue
                            val s = sad(ga, gb, sw, sh, x0, y0, dx, dy, best)
                            if (s < best) { best = s; bdx = dx; bdy = dy }
                        }
                    }
                    // motion lang kung klaro, ha
                    if (best > sad0 * 0.85f) { bdx = 0; bdy = 0; best = sad0 }
                    totalBest += best
                    vx[by * bw + bx] = bdx.toFloat() * DOWN
                    vy[by * bw + bx] = bdy.toFloat() * DOWN
                }
            }
            // lahi kaayo, balika ang una
            val perPixel = totalBest.toFloat() / (bw * bh * BLOCK * BLOCK)
            if (perPixel > 28f) return a.copy(Bitmap.Config.ARGB_8888, false)

            val out = IntArray(w * h)
            val cell = (DOWN * BLOCK).toFloat()
            for (y in 0 until h) {
                val gy = ((y + 0.5f) / cell - 0.5f).coerceIn(0f, bh - 1f)
                val y0 = min(gy.toInt(), bh - 1)
                val y1 = min(y0 + 1, bh - 1)
                val fy = gy - y0
                for (x in 0 until w) {
                    val gx = ((x + 0.5f) / cell - 0.5f).coerceIn(0f, bw - 1f)
                    val x0 = min(gx.toInt(), bw - 1)
                    val x1 = min(x0 + 1, bw - 1)
                    val fx = gx - x0
                    val i00 = y0 * bw + x0; val i10 = y0 * bw + x1
                    val i01 = y1 * bw + x0; val i11 = y1 * bw + x1
                    val mx = (vx[i00] * (1 - fx) + vx[i10] * fx) * (1 - fy) + (vx[i01] * (1 - fx) + vx[i11] * fx) * fy
                    val my = (vy[i00] * (1 - fx) + vy[i10] * fx) * (1 - fy) + (vy[i01] * (1 - fx) + vy[i11] * fx) * fy
                    val ca = sample(pa, w, h, x - mx * 0.5f, y - my * 0.5f)
                    val cb = sample(pb, w, h, x + mx * 0.5f, y + my * 0.5f)
                    out[y * w + x] = avg(ca, cb)
                }
            }
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            bmp.setPixels(out, 0, w, 0, 0, w, h)
            bmp
        } catch (t: Throwable) {
            t.printStackTrace()
            a.copy(Bitmap.Config.ARGB_8888, false)
        }
    }

    private fun blend(pa: IntArray, pb: IntArray, w: Int, h: Int): Bitmap {
        val out = IntArray(w * h) { avg(pa[it], pb[it]) }
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        bmp.setPixels(out, 0, w, 0, 0, w, h)
        return bmp
    }

    private fun gray(p: IntArray, w: Int, sw: Int, sh: Int): IntArray {
        val g = IntArray(sw * sh)
        for (y in 0 until sh) for (x in 0 until sw) {
            var s = 0
            for (dy in 0 until DOWN) for (dx in 0 until DOWN) {
                val c = p[(y * DOWN + dy) * w + x * DOWN + dx]
                s += (((c shr 16) and 0xFF) * 77 + ((c shr 8) and 0xFF) * 151 + (c and 0xFF) * 28) shr 8
            }
            g[y * sw + x] = s / (DOWN * DOWN)
        }
        return g
    }

    private fun sad(a: IntArray, b: IntArray, sw: Int, sh: Int, x0: Int, y0: Int, dx: Int, dy: Int, limit: Int): Int {
        var s = 0
        for (y in 0 until BLOCK) {
            val ay = y0 + y
            val by = (ay + dy).coerceIn(0, sh - 1)
            for (x in 0 until BLOCK) {
                val ax = x0 + x
                val bx = (ax + dx).coerceIn(0, sw - 1)
                s += abs(a[ay * sw + ax] - b[by * sw + bx])
            }
            if (s >= limit) return s
        }
        return s
    }

    private fun sample(p: IntArray, w: Int, h: Int, fx: Float, fy: Float): Int {
        val x = fx.coerceIn(0f, w - 1.001f)
        val y = fy.coerceIn(0f, h - 1.001f)
        val x0 = floor(x).toInt()
        val y0 = floor(y).toInt()
        val tx = x - x0
        val ty = y - y0
        val c00 = p[y0 * w + x0]; val c10 = p[y0 * w + x0 + 1]
        val c01 = p[(y0 + 1) * w + x0]; val c11 = p[(y0 + 1) * w + x0 + 1]
        fun ch(shift: Int): Int {
            val a = ((c00 shr shift) and 0xFF) * (1 - tx) + ((c10 shr shift) and 0xFF) * tx
            val b = ((c01 shr shift) and 0xFF) * (1 - tx) + ((c11 shr shift) and 0xFF) * tx
            return (a * (1 - ty) + b * ty + 0.5f).toInt()
        }
        return (0xFF shl 24) or (ch(16) shl 16) or (ch(8) shl 8) or ch(0)
    }

    private fun avg(a: Int, b: Int): Int {
        val r = (((a shr 16) and 0xFF) + ((b shr 16) and 0xFF)) shr 1
        val g = (((a shr 8) and 0xFF) + ((b shr 8) and 0xFF)) shr 1
        val bl = ((a and 0xFF) + (b and 0xFF)) shr 1
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or bl
    }
}
