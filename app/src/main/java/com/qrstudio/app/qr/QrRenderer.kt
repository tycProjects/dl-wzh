package com.qrstudio.app.qr

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.Shader
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

object QrRenderer {

    private fun ecLevel(level: String): ErrorCorrectionLevel = when (level) {
        "L" -> ErrorCorrectionLevel.L
        "Q" -> ErrorCorrectionLevel.Q
        "H" -> ErrorCorrectionLevel.H
        else -> ErrorCorrectionLevel.M
    }

    fun matrixFor(content: String, style: QrStyleConfig): BitMatrix? = try {
        val hints = mapOf(
            EncodeHintType.ERROR_CORRECTION to ecLevel(style.errorCorrection),
            EncodeHintType.MARGIN to 0,
        )
        QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 0, 0, hints)
    } catch (e: WriterException) {
        null
    } catch (e: IllegalArgumentException) {
        null
    }

    private fun finderRects(modules: Int): List<Rect> = listOf(
        Rect(0, 0, 6, 6),
        Rect(modules - 7, 0, modules - 1, 6),
        Rect(0, modules - 7, 6, modules - 1),
    )

    private fun isInFinder(x: Int, y: Int, finders: List<Rect>): Boolean =
        finders.any { it.contains(x, y) }

    fun render(content: String, style: QrStyleConfig, logo: Bitmap?): Bitmap? {
        if (content.isBlank()) return null
        val matrix = matrixFor(content, style) ?: return null
        val quiet = style.quietZone.coerceIn(0, 30)
        val modules = matrix.width
        val total = modules + quiet * 2
        val size = style.exportSize.coerceIn(300, 2000)
        val cell = size.toFloat() / total
        val capSpace = captionSpacePx(style, size)
        val height = size + capSpace

        val bmp = Bitmap.createBitmap(size, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        if (!style.transparentBg) {
            paint.shader = null
            paint.color = style.backgroundColor.toInt()
            canvas.drawRect(0f, 0f, size.toFloat(), height.toFloat(), paint)
        }

        if (style.gradient) {
            paint.shader = LinearGradient(
                0f, 0f, size.toFloat(), size.toFloat(),
                style.primaryColor.toInt(), style.secondaryColor.toInt(),
                Shader.TileMode.CLAMP,
            )
        } else {
            paint.color = style.primaryColor.toInt()
        }

        val finders = finderRects(modules)
        for (y in 0 until modules) {
            for (x in 0 until modules) {
                if (!matrix.get(x, y)) continue
                if (isInFinder(x, y, finders)) continue
                drawModule(canvas, paint, (x + quiet) * cell, (y + quiet) * cell, cell, style.dotShape)
            }
        }

        paint.shader = null
        paint.color = style.primaryColor.toInt()
        drawFinders(canvas, paint, cell, quiet, finders, style)

        if (logo != null) drawLogo(canvas, paint, logo, style, size)
        if (style.caption.isNotBlank() && capSpace > 0) drawCaption(canvas, style, size, capSpace)
        return bmp
    }

    private fun drawModule(canvas: Canvas, paint: Paint, left: Float, top: Float, cell: Float, shape: DotShape) {
        val right = left + cell
        val bottom = top + cell
        when (shape) {
            DotShape.SQUARE -> canvas.drawRect(left, top, right, bottom, paint)
            DotShape.ROUNDED -> {
                val r = cell * 0.32f
                canvas.drawRoundRect(left, top, right, bottom, r, r, paint)
            }
            DotShape.DOTS -> canvas.drawCircle(left + cell / 2, top + cell / 2, cell * 0.40f, paint)
            DotShape.SMOOTH -> canvas.drawCircle(left + cell / 2, top + cell / 2, cell * 0.5f, paint)
            DotShape.CLASSY -> canvas.drawRoundRect(
                left, top, right, bottom,
                floatArrayOf(cell * 0.5f, cell * 0.5f, 0f, 0f, cell * 0.5f, cell * 0.5f, 0f, 0f),
                paint,
            )
            DotShape.CLASSY_ROUND -> {
                val r = cell * 0.5f
                canvas.drawRoundRect(left, top, right, bottom, r, r, paint)
            }
        }
    }

    private fun drawFinders(
        canvas: Canvas,
        paint: Paint,
        cell: Float,
        quiet: Int,
        finders: List<Rect>,
        style: QrStyleConfig,
    ) {
        for (f in finders) {
            val left = (f.left + quiet) * cell
            val top = (f.top + quiet) * cell
            val outerR = if (style.cornerFrame == CornerFrame.ROUND) 1.6f * cell else 0f
            val path = Path()
            path.addRoundRect(left, top, left + 7 * cell, top + 7 * cell, outerR, outerR, Path.Direction.CW)
            val innerR = if (outerR > 0f) (outerR - cell).coerceAtLeast(0f) else 0f
            path.addRoundRect(left + cell, top + cell, left + 6 * cell, top + 6 * cell, innerR, innerR, Path.Direction.CW)
            path.fillType = Path.FillType.EVEN_ODD
            canvas.drawPath(path, paint)

            val dotSize = 3f * cell
            val dotLeft = left + 2 * cell
            val dotTop = top + 2 * cell
            if (style.cornerDot == CornerDot.DOT) {
                canvas.drawCircle(dotLeft + dotSize / 2, dotTop + dotSize / 2, dotSize / 2, paint)
            } else {
                canvas.drawRect(dotLeft, dotTop, dotLeft + dotSize, dotTop + dotSize, paint)
            }
        }
    }

    private fun drawLogo(canvas: Canvas, paint: Paint, logo: Bitmap, style: QrStyleConfig, size: Int) {
        val pct = style.logoSizePercent.coerceIn(10, 45) / 100f
        val logoSize = (size * pct).toInt().coerceAtLeast(8)
        val margin = style.logoMargin.coerceIn(0, 20)
        val box = logoSize + margin * 2
        val left = (size - box) / 2f
        val top = (size - box) / 2f

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (style.transparentBg) Color.WHITE else style.backgroundColor.toInt()
        }
        canvas.drawRoundRect(left, top, left + box, top + box, box * 0.18f, box * 0.18f, bgPaint)

        val scaled = Bitmap.createScaledBitmap(logo, logoSize, logoSize, true)
        canvas.drawBitmap(scaled, left + margin, top + margin, paint)
    }

    fun captionSpacePx(style: QrStyleConfig, size: Int): Int =
        if (style.caption.isBlank()) 0
        else ((style.captionSize * (size / 600f)) + (size / 50f)).toInt()

    private fun drawCaption(canvas: Canvas, style: QrStyleConfig, size: Int, capSpace: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = style.captionColor.toInt()
            textSize = style.captionSize * (size / 600f)
            textAlign = Paint.Align.CENTER
        }
        val x = size / 2f
        val y = size + capSpace / 2f - (paint.descent() + paint.ascent()) / 2f
        canvas.drawText(style.caption, x, y, paint)
    }
}
