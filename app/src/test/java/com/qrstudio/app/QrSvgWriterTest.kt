package com.qrstudio.app

import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.qrstudio.app.qr.QrRenderer
import com.qrstudio.app.qr.QrStyleConfig
import com.qrstudio.app.qr.QrSvgWriter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QrSvgWriterTest {

    private fun matrix(content: String) =
        QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 0, 0)

    @Test
    fun svgContainsRootElements() {
        val svg = QrSvgWriter.write(matrix("https://example.com"), QrStyleConfig(), null)
        assertTrue(svg.startsWith("<?xml"))
        assertTrue(svg.contains("<svg"))
        assertTrue(svg.contains("</svg>"))
        assertTrue(svg.contains("<rect"))
    }

    @Test
    fun gradientAndCaptionAreIncluded() {
        val style = QrStyleConfig(
            gradient = true,
            primaryColor = 0xFF10b981,
            secondaryColor = 0xFF2dd4bf,
            caption = "امسح للدفع",
            captionSize = 18,
        )
        val svg = QrSvgWriter.write(matrix("test"), style, null)
        assertTrue(svg.contains("linearGradient"))
        assertTrue(svg.contains("url(#qrGrad)"))
        assertTrue(svg.contains("امسح للدفع"))
    }

    @Test
    fun logoEmbeddedAsBase64Image() {
        val svg = QrSvgWriter.write(matrix("logo"), QrStyleConfig(), "QUJD")
        assertTrue(svg.contains("data:image/png;base64,QUJD"))
    }

    @Test
    fun transparentBackgroundOmitsBgRect() {
        val withBg = QrSvgWriter.write(matrix("a"), QrStyleConfig(), null)
        val withoutBg = QrSvgWriter.write(matrix("a"), QrStyleConfig(transparentBg = true), null)
        assertTrue(withBg.contains("<rect width="))
        assertTrue(!withoutBg.contains(toBgRectMarker()))
    }

    private fun toBgRectMarker(): String = "<rect width="

    @Test
    fun matrixGeneratedForAllErrorCorrectionLevels() {
        for (level in listOf("L", "M", "Q", "H")) {
            val m = QrRenderer.matrixFor("hello world", QrStyleConfig(errorCorrection = level))
            assertNotNull(m)
            assertTrue(m!!.width >= 21)
        }
    }

    @Test
    fun matrixIsNullForBlankContentOnlyThroughRenderer() {
        // المحتوى الفارغ يُرفض في render() قبل الوصول للترميز
        assertEquals(null, QrRenderer.render("   ", QrStyleConfig(), null))
    }

    @Test
    fun longContentRespectsEcCapacityDifference() {
        // المحتوى الطويل جداً قد يفشل عند مستويات حماية أعلى — نتأكد أن L يتسع لمحتوى كبير
        val longContent = "https://example.com/" + "a".repeat(800)
        val m = QrRenderer.matrixFor(longContent, QrStyleConfig(errorCorrection = "L"))
        assertNotNull(m)
    }
}
