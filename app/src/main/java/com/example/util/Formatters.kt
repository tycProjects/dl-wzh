package com.example.util

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val idLocale = Locale("id", "ID")

fun formatCurrency(amount: Double, currencyCode: String = "IDR"): String {
    return when (currencyCode.uppercase()) {
        "USD" -> NumberFormat.getCurrencyInstance(Locale.US).format(amount)
        else -> {
            val nf = NumberFormat.getNumberInstance(idLocale)
            nf.maximumFractionDigits = 0
            "Rp ${nf.format(amount)}"
        }
    }
}

fun formatShortCurrency(amount: Double): String {
    return when {
        amount >= 1_000_000_000 -> String.format(idLocale, "Rp %.1fM", amount / 1_000_000_000)
        amount >= 1_000_000 -> String.format(idLocale, "Rp %.1fJt", amount / 1_000_000)
        amount >= 1_000 -> String.format(idLocale, "Rp %.0fRb", amount / 1_000)
        else -> formatCurrency(amount)
    }
}

fun formatDateTime(epochMs: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", idLocale)
    return sdf.format(Date(epochMs))
}

fun formatDateOnly(epochMs: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy", idLocale)
    return sdf.format(Date(epochMs))
}

fun formatDayShort(epochMs: Long): String {
    val sdf = SimpleDateFormat("dd/MM", idLocale)
    return sdf.format(Date(epochMs))
}

fun formatMonthShort(epochMs: Long): String {
    val sdf = SimpleDateFormat("MMM", idLocale)
    return sdf.format(Date(epochMs))
}

fun formatTimeOnly(epochMs: Long): String {
    val sdf = SimpleDateFormat("HH:mm", idLocale)
    return sdf.format(Date(epochMs))
}
