package com.example.model

enum class VatCalculationMode(val label: String, val description: String) {
    NET_TO_GROSS("Add VAT (Net → Gross)", "Calculate total price including VAT from net price"),
    GROSS_TO_NET("Extract VAT (Gross → Net)", "Calculate pre-tax net amount and VAT component"),
    CUSTOM_ANALYSIS("Rate Inspector", "Calculate effective VAT rate from Net and Gross")
}

data class VatPreset(
    val rate: Double,
    val label: String,
    val countryHint: String = ""
)

data class VatCalculationResult(
    val netAmount: Double,
    val vatRate: Double,
    val vatAmount: Double,
    val grossAmount: Double
)
