package com.qrstudio.app.qr

import com.google.zxing.common.BitMatrix

/**
 * توليد SVG متجهي من نفس مصفوفة QR المستخدمة في الرسم النقطي.
 */
object QrSvgWriter {

    fun toHex(color: Long): String =
        "#" + (color and 0xFFFFFF).toString(16).padStart(6, '0')

    fun write(matrix: BitMatrix, style: QrStyleConfig, logoBase64: String?): String {
        val quiet = style.quietZone.coerceIn(0, 30)
        val modules = matrix.width
        val total = modules + quiet * 2
        val size = style.exportSize.coerceIn(300, 2000)
        val cell = size.toDouble() / total
        val capSpace = QrRenderer.captionSpacePx(style, size)
        val height = size + capSpace
        val finders = listOf(
            android.graphics.Rect(0, 0, 6, 6),
            android.graphics.Rect(modules - 7, 0, modules - 1, 6),
            android.graphics.Rect(0, modules - 7, 6, modules - 1),
        )
        val fill = if (style.gradient) "url(#qrGrad)" else toHex(style.primaryColor)

        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        sb.append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"$size\" height=\"$height\" viewBox=\"0 0 $size $height\">\n")
        if (style.gradient) {
            sb.append("  <defs><linearGradient id=\"qrGrad\" x1=\"0%\" y1=\"0%\" x2=\"100%\" y2=\"100%\">")
            sb.append("<stop offset=\"0%\" stop-color=\"").append(toHex(style.primaryColor)).append("\"/>")
            sb.append("<stop offset=\"100%\" stop-color=\"").append(toHex(style.secondaryColor)).append("\"/>")
            sb.append("</linearGradient></defs>\n")
        }
        if (!style.transparentBg) {
            sb.append("  <rect width=\"$size\" height=\"$height\" fill=\"").append(toHex(style.backgroundColor)).append("\"/>\n")
        }
        sb.append("  <g fill=\"$fill\">\n")
        for (y in 0 until modules) {
            for (x in 0 until modules) {
                if (!matrix.get(x, y)) continue
                if (finders.any { it.contains(x, y) }) continue
                sb.append("    ").append(moduleElement(x + quiet, y + quiet, cell, style.dotShape)).append("\n")
            }
        }
        sb.append("  </g>\n")
        sb.append("  <g fill=\"").append(toHex(style.primaryColor)).append("\">\n")
        sb.append(finderElements(matrix, quiet, cell, style))
        sb.append("  </g>\n")
        if (logoBase64 != null) {
            val pct = style.logoSizePercent.coerceIn(10, 45) / 100f
            val logoSize = size * pct
            val margin = style.logoMargin.coerceIn(0, 20)
            val box = logoSize + margin * 2
            val lx = (size - box) / 2.0
            val ly = (size - box) / 2.0
            sb.append("  <rect x=\"$lx\" y=\"$ly\" width=\"$box\" height=\"$box\" rx=\"${box * 0.18}\" fill=\"")
                .append(if (style.transparentBg) "#ffffff" else toHex(style.backgroundColor)).append("\"/>\n")
            sb.append("  <image x=\"${lx + margin}\" y=\"${ly + margin}\" width=\"$logoSize\" height=\"$logoSize\" href=\"data:image/png;base64,")
                .append(logoBase64).append("\"/>\n")
        }
        if (style.caption.isNotBlank() && capSpace > 0) {
            val textSize = style.captionSize * (size / 600f)
            sb.append("  <text x=\"${size / 2.0}\" y=\"${size + capSpace / 2.0 + textSize / 3.0}\" font-size=\"$textSize\" text-anchor=\"middle\" fill=\"")
                .append(toHex(style.captionColor)).append("\">").append(escapeXml(style.caption)).append("</text>\n")
        }
        sb.append("</svg>")
        return sb.toString()
    }

    private fun moduleElement(mx: Int, my: Int, cell: Double, shape: DotShape): String {
        val l = mx * cell
        val t = my * cell
        return when (shape) {
            DotShape.SQUARE -> "<rect x=\"$l\" y=\"$t\" width=\"$cell\" height=\"$cell\"/>"
            DotShape.ROUNDED -> "<rect x=\"$l\" y=\"$t\" width=\"$cell\" height=\"$cell\" rx=\"${cell * 0.32}\"/>"
            DotShape.DOTS -> "<circle cx=\"${l + cell / 2}\" cy=\"${t + cell / 2}\" r=\"${cell * 0.40}\"/>"
            DotShape.SMOOTH -> "<circle cx=\"${l + cell / 2}\" cy=\"${t + cell / 2}\" r=\"${cell * 0.5}\"/>"
            DotShape.CLASSY -> "<rect x=\"$l\" y=\"$t\" width=\"$cell\" height=\"$cell\" rx=\"${cell * 0.5}\"/>"
            DotShape.CLASSY_ROUND -> "<rect x=\"$l\" y=\"$t\" width=\"$cell\" height=\"$cell\" rx=\"${cell * 0.5}\"/>"
        }
    }

    private fun finderElements(matrix: BitMatrix, quiet: Int, cell: Double, style: QrStyleConfig): String {
        val modules = matrix.width
        val corners = listOf(
            android.graphics.Rect(0, 0, 7, 7),
            android.graphics.Rect(modules - 7, 0, modules, 7),
            android.graphics.Rect(0, modules - 7, 7, modules),
        )
        val sb = StringBuilder()
        for (f in corners) {
            val l = (f.left + quiet) * cell
            val t = (f.top + quiet) * cell
            val r = if (style.cornerFrame == CornerFrame.ROUND) 1.6 * cell else 0.0
            sb.append("    <path fill-rule=\"evenodd\" d=\"")
            sb.append(roundRectPath(l, t, 7 * cell, r)).append(" ")
            sb.append(roundRectPath(l + cell, t + cell, 5 * cell, (r - cell).coerceAtLeast(0.0)))
            sb.append("\"/>\n")
            val dot = 3 * cell
            val dl = l + 2 * cell
            val dt = t + 2 * cell
            if (style.cornerDot == CornerDot.DOT) {
                sb.append("    <circle cx=\"${dl + dot / 2}\" cy=\"${dt + dot / 2}\" r=\"${dot / 2}\"/>\n")
            } else {
                sb.append("    <rect x=\"$dl\" y=\"$dt\" width=\"$dot\" height=\"$dot\"/>\n")
            }
        }
        return sb.toString()
    }

    private fun roundRectPath(x: Double, y: Double, s: Double, r: Double): String =
        if (r <= 0.0) "M$x,$y h$s v$s h${-s} z"
        else "M${x + r},$y h${s - 2 * r} a$r,$r 0 0 1 $r,$r v${s - 2 * r} a$r,$r 0 0 1 ${-r},$r h${2 * r - s} a$r,$r 0 0 1 ${-r},${-r} v${2 * r - s} a$r,$r 0 0 1 $r,${-r} z"

    private fun escapeXml(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
}
