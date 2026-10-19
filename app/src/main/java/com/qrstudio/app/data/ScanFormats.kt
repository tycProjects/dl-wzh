package com.qrstudio.app.data

import com.google.mlkit.vision.barcode.Barcode

object ScanFormats {
    fun label(format: Int): String = when (format) {
        Barcode.FORMAT_QR_CODE -> "QR"
        Barcode.FORMAT_EAN_13 -> "EAN-13"
        Barcode.FORMAT_EAN_8 -> "EAN-8"
        Barcode.FORMAT_UPC_A -> "UPC-A"
        Barcode.FORMAT_UPC_E -> "UPC-E"
        Barcode.FORMAT_CODE_128 -> "Code 128"
        Barcode.FORMAT_CODE_39 -> "Code 39"
        Barcode.FORMAT_CODE_93 -> "Code 93"
        Barcode.FORMAT_CODABAR -> "Codabar"
        Barcode.FORMAT_DATA_MATRIX -> "Data Matrix"
        Barcode.FORMAT_AZTEC -> "Aztec"
        Barcode.FORMAT_PDF417 -> "PDF417"
        Barcode.FORMAT_ITF -> "ITF"
        else -> "باركود"
    }

    fun isOpenable(raw: String): Boolean = raw.startsWith("http://") ||
        raw.startsWith("https://") ||
        raw.startsWith("tel:") ||
        raw.startsWith("mailto:") ||
        raw.startsWith("sms:") ||
        raw.startsWith("geo:")
}
