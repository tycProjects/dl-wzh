package com.example.model

enum class UnitDimension(val displayName: String) {
    LENGTH("Length"),
    MASS("Mass & Weight"),
    TEMPERATURE("Temperature"),
    AREA("Area"),
    VOLUME("Volume & Liquid"),
    SPEED("Speed"),
    STORAGE("Digital Storage")
}

data class ConversionUnit(
    val name: String,
    val symbol: String,
    val toBaseRatio: Double // relative to base unit of dimension
)

object UnitRegistry {
    val dimensions = listOf(
        UnitDimension.LENGTH,
        UnitDimension.MASS,
        UnitDimension.TEMPERATURE,
        UnitDimension.AREA,
        UnitDimension.VOLUME,
        UnitDimension.SPEED,
        UnitDimension.STORAGE
    )

    fun getUnitsForDimension(dimension: UnitDimension): List<ConversionUnit> {
        return when (dimension) {
            UnitDimension.LENGTH -> listOf(
                ConversionUnit("Meter", "m", 1.0),
                ConversionUnit("Kilometer", "km", 1000.0),
                ConversionUnit("Centimeter", "cm", 0.01),
                ConversionUnit("Millimeter", "mm", 0.001),
                ConversionUnit("Mile", "mi", 1609.344),
                ConversionUnit("Yard", "yd", 0.9144),
                ConversionUnit("Foot", "ft", 0.3048),
                ConversionUnit("Inch", "in", 0.0254)
            )
            UnitDimension.MASS -> listOf(
                ConversionUnit("Kilogram", "kg", 1.0),
                ConversionUnit("Gram", "g", 0.001),
                ConversionUnit("Milligram", "mg", 0.000001),
                ConversionUnit("Metric Ton", "t", 1000.0),
                ConversionUnit("Pound", "lb", 0.45359237),
                ConversionUnit("Ounce", "oz", 0.028349523),
                ConversionUnit("Stone", "st", 6.35029318)
            )
            UnitDimension.TEMPERATURE -> listOf(
                ConversionUnit("Celsius", "°C", 1.0),
                ConversionUnit("Fahrenheit", "°F", 1.0),
                ConversionUnit("Kelvin", "K", 1.0)
            )
            UnitDimension.AREA -> listOf(
                ConversionUnit("Square Meter", "m²", 1.0),
                ConversionUnit("Square Kilometer", "km²", 1_000_000.0),
                ConversionUnit("Square Foot", "ft²", 0.092903),
                ConversionUnit("Square Yard", "yd²", 0.836127),
                ConversionUnit("Acre", "ac", 4046.856),
                ConversionUnit("Hectare", "ha", 10000.0)
            )
            UnitDimension.VOLUME -> listOf(
                ConversionUnit("Liter", "L", 1.0),
                ConversionUnit("Milliliter", "mL", 0.001),
                ConversionUnit("Cubic Meter", "m³", 1000.0),
                ConversionUnit("US Gallon", "gal", 3.78541),
                ConversionUnit("US Quart", "qt", 0.946353),
                ConversionUnit("US Pint", "pt", 0.473176),
                ConversionUnit("US Cup", "cup", 0.236588),
                ConversionUnit("Fluid Ounce", "fl oz", 0.0295735)
            )
            UnitDimension.SPEED -> listOf(
                ConversionUnit("Meters per second", "m/s", 1.0),
                ConversionUnit("Kilometers per hour", "km/h", 0.277778),
                ConversionUnit("Miles per hour", "mph", 0.44704),
                ConversionUnit("Knot", "kn", 0.514444)
            )
            UnitDimension.STORAGE -> listOf(
                ConversionUnit("Byte", "B", 1.0),
                ConversionUnit("Kilobyte", "KB", 1024.0),
                ConversionUnit("Megabyte", "MB", 1024.0 * 1024.0),
                ConversionUnit("Gigabyte", "GB", 1024.0 * 1024.0 * 1024.0),
                ConversionUnit("Terabyte", "TB", 1024.0 * 1024.0 * 1024.0 * 1024.0)
            )
        }
    }

    fun convert(value: Double, from: ConversionUnit, to: ConversionUnit, dimension: UnitDimension): Double {
        if (dimension == UnitDimension.TEMPERATURE) {
            // Special temperature formulas
            val celsius = when (from.symbol) {
                "°C" -> value
                "°F" -> (value - 32.0) * 5.0 / 9.0
                "K" -> value - 273.15
                else -> value
            }
            return when (to.symbol) {
                "°C" -> celsius
                "°F" -> (celsius * 9.0 / 5.0) + 32.0
                "K" -> celsius + 273.15
                else -> celsius
            }
        }
        val inBase = value * from.toBaseRatio
        return inBase / to.toBaseRatio
    }
}
