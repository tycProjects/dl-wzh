package com.example.model

data class CalculationItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val expression: String,
    val result: String,
    val timestamp: Long = System.currentTimeMillis(),
    val category: String = "Basic" // Basic, Scientific, VAT
)
