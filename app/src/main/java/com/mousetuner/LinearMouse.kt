package com.mousetuner

/**
 * Lõi chuột tuyến tính: out = delta × sensitivity (× hệ số trục).
 * Không gia tốc, không đường cong. Phần lẻ (sub-pixel) được giữ lại để KHÔNG mất chuyển động nhỏ.
 */
class LinearMouse(var sens: Float = 1f, var sx: Float = 1f, var sy: Float = 1f) {
    var remX = 0.0; private set
    var remY = 0.0; private set
    var outX = 0; private set
    var outY = 0; private set

    fun reset() { remX = 0.0; remY = 0.0; outX = 0; outY = 0 }

    fun feed(dx: Float, dy: Float) {
        remX += dx.toDouble() * sens * sx
        remY += dy.toDouble() * sens * sy
        outX = remX.toInt(); outY = remY.toInt()      // cắt về 0, phần dư giữ lại
        remX -= outX; remY -= outY
    }

    companion object {
        /** Tự kiểm tra thuật toán: chậm/nhanh, ngang/dọc/chéo, trục riêng, delta rất nhỏ. */
        fun selfTest(): String {
            val sb = StringBuilder(); var all = true
            fun run(name: String, n: Int, dx: Float, dy: Float, s: Float, ax: Float, ay: Float): Pair<Long, Long> {
                val lm = LinearMouse(s, ax, ay); var ox = 0L; var oy = 0L
                repeat(n) { lm.feed(dx, dy); ox += lm.outX; oy += lm.outY }
                val ex = n * dx.toDouble() * s * ax; val ey = n * dy.toDouble() * s * ay
                val ok = Math.abs(ox + lm.remX - ex) < 0.01 && Math.abs(oy + lm.remY - ey) < 0.01
                if (!ok) all = false
                sb.append(if (ok) "PASS " else "FAIL ").append(name).append(": ra ($ox,$oy) / kỳ vọng (%.1f,%.1f)\n".format(ex, ey))
                return ox to oy
            }
            val slow = run("Kéo chậm 1px×1000", 1000, 1f, 0f, 1.5f, 1f, 1f)
            val fast = run("Kéo nhanh 50px×20", 20, 50f, 0f, 1.5f, 1f, 1f)
            val same = slow == fast; if (!same) all = false
            sb.append(if (same) "PASS " else "FAIL ").append("Chậm = nhanh (không gia tốc)\n")
            run("Ngang", 300, 7f, 0f, 0.8f, 1f, 1f)
            run("Dọc", 300, 0f, 7f, 0.8f, 1f, 1f)
            val d = run("Chéo 3:4", 500, 3f, 4f, 0.8f, 1f, 1f)
            val ratioOk = Math.abs(d.first * 4.0 - d.second * 3.0) <= 8; if (!ratioOk) all = false
            sb.append(if (ratioOk) "PASS " else "FAIL ").append("Giữ tỉ lệ X:Y của hướng chéo\n")
            run("Trục riêng X1.5 Y0.5", 400, 5f, 5f, 1f, 1.5f, 0.5f)
            run("Delta nhỏ 0.1×10000 (không mất sự kiện)", 10000, 0.1f, 0f, 1f, 1f, 1f)
            sb.append(if (all) "\n=> TẤT CẢ ĐẠT" else "\n=> CÓ LỖI")
            return sb.toString()
        }
    }
}
