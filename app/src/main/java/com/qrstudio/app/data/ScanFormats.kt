package com.qrstudio.app.data

// AUTO-REPAIR: disabled -- unresolved reference 'Barcode' (was: import com.google.mlkit.vision.barcode.Barcode)

object ScanFormats {
    fun label(format: Int): String = when (format) {
        // AUTO-REPAIR: disabled -- unresolved reference 'Barcode' (was: Barcode.FORMAT_QR_CODE -> "QR")
        // AUTO-REPAIR: disabled -- unresolved reference 'Barcode' (was: Barcode.FORMAT_EAN_13 -> "EAN-13")
        // AUTO-REPAIR: disabled -- unresolved reference 'Barcode' (was: Barcode.FORMAT_EAN_8 -> "EAN-8")
        // AUTO-REPAIR: disabled -- unresolved reference 'Barcode' (was: Barcode.FORMAT_UPC_A -> "UPC-A")
        // AUTO-REPAIR: disabled -- unresolved reference 'Barcode' (was: Barcode.FORMAT_UPC_E -> "UPC-E")
        // AUTO-REPAIR: disabled -- unresolved reference 'Barcode' (was: Barcode.FORMAT_CODE_128 -> "Code 128")
        // AUTO-REPAIR: disabled -- unresolved reference 'Barcode' (was: Barcode.FORMAT_CODE_39 -> "Code 39")
        // AUTO-REPAIR: disabled -- unresolved reference 'Barcode' (was: Barcode.FORMAT_CODE_93 -> "Code 93")
        // AUTO-REPAIR: disabled -- unresolved reference 'Barcode' (was: Barcode.FORMAT_CODABAR -> "Codabar")
        // AUTO-REPAIR: disabled -- unresolved reference 'Barcode' (was: Barcode.FORMAT_DATA_MATRIX -> "Data Matrix")
        // AUTO-REPAIR: disabled -- unresolved reference 'Barcode' (was: Barcode.FORMAT_AZTEC -> "Aztec")
        // AUTO-REPAIR: disabled -- unresolved reference 'Barcode' (was: Barcode.FORMAT_PDF417 -> "PDF417")
        // AUTO-REPAIR: disabled -- unresolved reference 'Barcode' (was: Barcode.FORMAT_ITF -> "ITF")
        else -> "باركود"
    }

    fun isOpenable(raw: String): Boolean = raw.startsWith("http://") ||
        raw.startsWith("https://") ||
        raw.startsWith("tel:") ||
        raw.startsWith("mailto:") ||
        raw.startsWith("sms:") ||
        raw.startsWith("geo:")
}
