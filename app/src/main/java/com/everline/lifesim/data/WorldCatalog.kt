package com.everline.lifesim.data

/** Entire catalog is modeled TEST CONTENT, not verified wage or census data. */
data class CountryStarter(val code: String, val name: String, val nationality: String, val currency: String,
    val city: String, val livingCostsCents: Long, val salaryMultiplier: Double)
data class CareerStarter(val id: String, val title: String, val employer: String,
    val monthlyPhpCents: Long, val minimumEducation: String)

object WorldCatalog {
    val countries = listOf(
        CountryStarter("PH", "Philippines", "Filipino", "PHP", "Quezon City", 1_200_000L, 1.00),
        CountryStarter("US", "United States", "American", "USD", "Seattle", 180_000L, 0.090),
        CountryStarter("GB", "United Kingdom", "British", "GBP", "Manchester", 130_000L, 0.065),
        CountryStarter("JP", "Japan", "Japanese", "JPY", "Osaka", 16_000_000L, 8.0),
        CountryStarter("IN", "India", "Indian", "INR", "Bengaluru", 2_200_000L, 1.25),
        CountryStarter("CA", "Canada", "Canadian", "CAD", "Toronto", 160_000L, 0.080)
    )
    val careers = listOf(
        CareerStarter("retail", "Retail Associate", "Local Retail Store", 2_200_000, "None"),
        CareerStarter("barista", "Barista", "Neighborhood Café", 2_400_000, "None"),
        CareerStarter("assistant", "Administrative Assistant", "Office Services", 3_000_000, "Secondary"),
        CareerStarter("chef", "Cook", "Local Restaurant", 3_400_000, "Secondary"),
        CareerStarter("developer", "Software Developer", "Everline Technologies (fictional)", 6_500_000, "Bachelor"),
        CareerStarter("analyst", "Business Analyst", "Civic Enterprises (fictional)", 5_000_000, "Bachelor"),
        CareerStarter("teacher", "Secondary School Teacher", "Community School (fictional)", 3_700_000, "Bachelor")
    )
    fun country(code: String): CountryStarter = countries.firstOrNull { it.code == code } ?: countries.first()
    fun salary(code: String, job: CareerStarter): Long =
        (job.monthlyPhpCents * country(code).salaryMultiplier).toLong()
    fun money(code: String, cents: Long): String {
        val symbol = when (code) { "PH" -> "₱"; "US" -> "\$"; "GB" -> "£"; "JP" -> "¥"; "IN" -> "₹"; "CA" -> "C\$"; else -> "" }
        val value = java.math.BigInteger.valueOf(cents).abs()
        val major = value.divide(java.math.BigInteger.valueOf(100)).toLong()
        val minor = value.remainder(java.math.BigInteger.valueOf(100)).toString().padStart(2, '0')
        val sign = if (cents < 0) "−" else ""
        return "$sign$symbol${"%,d".format(major)}.$minor"
    }
}
